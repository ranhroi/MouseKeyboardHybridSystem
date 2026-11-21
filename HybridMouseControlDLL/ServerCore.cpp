/* ServerCore.cpp
 *
 * Bao gồm:
 *   - Globals (config, status, mode)
 *   - Protocol dispatcher (handleProtocolFrame, sendProtocolResponse)
 *   - Mode switching (switchMode) — có mutex bảo vệ
 *   - Fallback watchdog — EVENT-DRIVEN (Wi-Fi loss → BLE)
 *   - BLE auto-fallback — BLE init fail → WIFI (Cách A + C)
 *   - Bluetooth fallback — BT loss → WIFI (event-driven, grace 3s)
 *   - Public C API (HID_Initialize, HID_Connect, ...)
 *
 * Logic TCP: TcpServer.cpp
 * Logic BLE: BleGattServer.cpp
 * Logic network monitor: NetworkMonitor.cpp
 * Logic bluetooth monitor: BluetoothMonitor.cpp
 *
 * FALLBACK LOGIC:
 *   ── WIFI → BLE ──
 *     • Client TCP ngắt          → Ở WIFI, chờ client mới (KHÔNG fallback)
 *     • PC mất mạng vật lý       → Fallback BLE sau 30s grace
 *     • PC có mạng lại           → Hủy grace, ở WIFI
 *
 *   ── BLE → WIFI (Cách A) ──
 *     • BleGattServer_Start fail → Retry 3 lần (cách 2s)
 *     • Vẫn fail                 → Auto-switch WIFI
 *
 *   ── BLE → WIFI (Cách C) ──
 *     • User gửi 0xFF            → Thử BLE lại
 *     • BLE fail                 → ACK + giữ WIFI
 *     • BLE OK                   → Chuyển sang BLE
 *
 *   ── BLE → WIFI (event-driven) ──
 *     • PC tắt Bluetooth         → Fallback WIFI sau 3s grace
 *     • PC bật Bluetooth lại     → Hủy grace (không auto switch lại)
 */

#include "pch.h"

namespace mouseKeyboard {

    // ============================================================
    //  Globals
    // ============================================================
    static std::mutex         gLock;
    static Config             gCfg;
    static HID_StatusCallback gCallback = nullptr;
    static void* gUser = nullptr;

    static std::atomic<HID_Status>    gStatus{ HID_STATUS_IDLE };
    static std::atomic<HID_Transport> gTransport{ HID_TRANSPORT_NONE };
    static std::atomic<HID_Mode>      gMode{ HID_MODE_BLE };
    static std::atomic<bool>          gStop{ false };
    static std::atomic<bool>          gStarted{ false };

    // Bảo vệ switchMode() — tránh 2 thread cùng switch
    static std::mutex         gSwitchMutex;

    // Auth state — 2 transport riêng biệt
    static AuthState gTcpAuthState;
    static AuthState gBleAuthState;

    // Shared instance
    InputEngine gInput;

    // ============================================================
    //  Fallback state — WIFI → BLE (mất mạng vật lý)
    // ============================================================
    static std::mutex              gFallbackMutex;
    static std::condition_variable gFallbackCv;
    static std::atomic<bool>       gNetworkLost{ false };
    static std::atomic<bool>       gFallbackStop{ false };
    static std::thread             gFallbackThread;
    static const int               NETWORK_LOST_GRACE_SEC = 30;

    // ============================================================
    //  Bluetooth fallback state — BLE → WIFI (event-driven)
    // ============================================================
    static std::atomic<bool>       gBtLost{ false };
    static std::mutex              gBtFallbackMutex;
    static std::condition_variable gBtFallbackCv;
    static std::thread             gBtFallbackThread;
    static std::atomic<bool>       gBtFallbackStop{ false };
    static const int               BT_LOST_GRACE_SEC = 3;

    // ============================================================
    //  BLE auto-fallback state (Cách A)
    // ============================================================
    static std::atomic<int>  gBleFailCount{ 0 };
    static const int         BLE_MAX_RETRY = 3;
    static const int         BLE_RETRY_DELAY_MS = 2000;

    // ============================================================
    //  Config accessor
    // ============================================================
    const Config& getConfig() { return gCfg; }
    void setConfig(const Config& cfg) { gCfg = cfg; }

    // ============================================================
    //  nowMs
    // ============================================================
    unsigned long long nowMs() {
        using namespace std::chrono;
        return duration_cast<milliseconds>(
            steady_clock::now().time_since_epoch()).count();
    }

    // ============================================================
    //  notifyStatus
    // ============================================================
    void notifyStatus(HID_Status s, HID_Transport t, const wchar_t* m) {
        gStatus = s;
        gTransport = t;
        HID_StatusCallback cb;
        void* u;
        { std::lock_guard<std::mutex> l(gLock); cb = gCallback; u = gUser; }
        if (cb) cb(s, t, m, u);
    }

    // ============================================================
    //  Auth reset helper
    // ============================================================
    void resetAuthForTransport(bool ble) {
        if (ble) gBleAuthState.reset();
        else     gTcpAuthState.reset();
        gInput.releaseAll();
    }

    // ============================================================
    //  Password check
    // ============================================================
    bool passwordMatches(const uint8_t* p, size_t n) {
        std::wstring w(gCfg.password);
        if (w.empty()) return n == 0;
        int len = WideCharToMultiByte(CP_UTF8, 0, w.data(), (int)w.size(),
            nullptr, 0, nullptr, nullptr);
        if (len <= 0) return n == 0;
        std::vector<uint8_t> pass(len);
        WideCharToMultiByte(CP_UTF8, 0, w.data(), (int)w.size(),
            (char*)pass.data(), len, nullptr, nullptr);
        return pass.size() == n && std::equal(pass.begin(), pass.end(), p);
    }

    // ============================================================
    //  Legacy frame size Loại 1: 1 byte (legacy) — PING, Current App, Screenshot
    // ============================================================
    size_t legacyFrameSize(const std::vector<uint8_t>& b) {
        if (b.empty()) return 0;
        switch (b[0]) {
        case 0xAA:
            if (b.size() < 4) return 0;
            if (b[1] != 0x01) return 1;
            { size_t n = 4 + b[3]; return n <= 64 && b.size() >= n ? n : 0; }
        case 0x01: case 0x02: case 0x03: case 0x04: case 0x05:
            return b.size() >= 3 ? 3 : 0;
        case 0xF1: return b.size() >= 2 ? 2 : 0;
        case 0xF3: return b.size() >= 2 && b.size() >= 2 + b[1] ? 2 + b[1] : 0;
        case 0xF7: case 0xFD: case 0xFE: case 0xFF: case 0xE0: 
        case 0xE1:
            return 1;
        default: return 1;
        }
    }

    // Loại 2: Nhiều byte (V2 batch) — HID frames
    bool isV2Command(uint8_t c) {
        switch (c) {
        case 0x01: case 0x02: case 0x03: case 0x04: case 0x05:
        case 0x10: case 0x11: case 0x12: case 0x13: case 0x14:
        case 0x20: case 0x21: case 0x22:
        case 0x30: return true;
        default:   return false;
        }
    }

    // ============================================================
    //  sendProtocolResponse
    // ============================================================
    bool sendProtocolResponse(bool ble, const uint8_t* p, size_t n) {
        if (ble) return BleGattServer_Send(p, n);
        return TcpServer_Send(p, n);
    }

    // Đọc trực tiếp bảng trạng thái phím toàn cục của Windows (tránh lỗi rác bit trên Worker Thread)
    void sendLedStatusResponse(bool ble) {
        // Đọc trực tiếp trạng thái Bật/Tắt (Toggle bit) từ hệ thống
        uint8_t num = (GetKeyState(VK_NUMLOCK) & 0x0001) ? 1 : 0;
        uint8_t caps = (GetKeyState(VK_CAPITAL) & 0x0001) ? 1 : 0;
        uint8_t scroll = (GetKeyState(VK_SCROLL) & 0x0001) ? 1 : 0;

        uint8_t packet[4] = { 0xBB, num, caps, scroll };
        sendProtocolResponse(ble, packet, 4);
    }


    // ============================================================
    //  Forward declarations
    // ============================================================
    static void switchMode(HID_Mode newMode);
    static bool startBleWithFallback();
    static void switchToWifi();

    // ============================================================
    //  switchToWifi — helper chuyển sang WIFI (dùng chung)
    // ============================================================
    static void switchToWifi() {
        // Reset network-lost state (notify sau khi release lock)
        {
            std::lock_guard<std::mutex> lk(gFallbackMutex);
            gNetworkLost = false;
        }
        gFallbackCv.notify_all();

        // Reset BT-lost state
        {
            std::lock_guard<std::mutex> lk(gBtFallbackMutex);
            gBtLost = false;
        }
        gBtFallbackCv.notify_all();

        gMode = HID_MODE_WIFI;

        // Stop BLE nếu đang chạy
        BleGattServer_Stop();
        Sleep(100);

        // Start TCP
        if (TcpServer_Start()) {
            notifyStatus(HID_STATUS_IDLE, HID_TRANSPORT_WIFI,
                L"TCP listening; waiting for client");
        }
        else {
            notifyStatus(HID_STATUS_ERROR, HID_TRANSPORT_NONE,
                L"Cannot start TCP server");
        }
    }

    // ============================================================
    //  startBleWithFallback — Cách A
    //
    //  Thử start BLE tối đa BLE_MAX_RETRY lần.
    //  Nếu vẫn fail → auto-switch sang WIFI.
    //
    //  Trả về:
    //    true  → BLE đã start OK
    //    false → BLE fail, đã fallback WIFI
    // ============================================================
    static bool startBleWithFallback() {
        for (int attempt = 1; attempt <= BLE_MAX_RETRY; ++attempt) {
            wchar_t buf[64];
            swprintf_s(buf, L"[BLE] Attempt %d/%d...\n",
                attempt, BLE_MAX_RETRY);
            OutputDebugStringW(buf);

            if (BleGattServer_Start(gCfg)) {
                gBleFailCount = 0;
                notifyStatus(HID_STATUS_IDLE, HID_TRANSPORT_BLE,
                    L"BLE advertising; waiting for client");
                return true;
            }

            // Fail → đợi rồi retry (trừ lần cuối)
            if (attempt < BLE_MAX_RETRY) {
                Sleep(BLE_RETRY_DELAY_MS);
            }
        }

        // Fail hết N lần → fallback WIFI
        gBleFailCount++;
        OutputDebugStringW(L"[BLE] Failed all retries -> fallback WIFI\n");

        notifyStatus(HID_STATUS_SWITCHING, HID_TRANSPORT_NONE,
            L"BLE unavailable - switching to WIFI");

        switchToWifi();
        return false;
    }

    // ============================================================
    //  Protocol dispatcher
    // ============================================================
    void handleProtocolFrame(const uint8_t* p, size_t n, bool ble) {
        if (!p || !n) return;

        AuthState& st = ble ? gBleAuthState : gTcpAuthState;

        // ---- PING ----
        if (p[0] == 0xF7) {
            uint8_t r = 0xF8;
            sendProtocolResponse(ble, &r, 1);
            return;
        }



        // ---- GET CONFIG ----
        if (p[0] == 0xFD) {
            // BLE name
            {
                const char* name = "BLE_COMPUTER_REMOTE";
                size_t nameLen = strlen(name);
                uint8_t buf[64];
                buf[0] = 0xFB;
                buf[1] = (uint8_t)nameLen;
                memcpy(&buf[2], name, nameLen);
                sendProtocolResponse(ble, buf, 2 + nameLen);
            }
            // TCP port
            {
                uint8_t buf[3];
                buf[0] = 0xFC;
                buf[1] = (uint8_t)(gCfg.port >> 8);
                buf[2] = (uint8_t)(gCfg.port & 0xFF);
                sendProtocolResponse(ble, buf, 3);
            }
            // IP
            {
                uint8_t buf[5] = { 0xFA, 127, 0, 0, 1 };
                sendProtocolResponse(ble, buf, 5);
            }
            return;
        }

        // ---- SWITCH MODE ----
        if (p[0] == 0xFE || p[0] == 0xFF || p[0] == 0xE0) {
            // 0xE0 (SWITCH_TO_SETUP) — bỏ qua trên PC, chỉ ACK
            if (p[0] == 0xE0) {
                uint8_t r = 0xF9;
                sendProtocolResponse(ble, &r, 1);
                return;
            }

            HID_Mode target = (p[0] == 0xFE) ? HID_MODE_WIFI
                : HID_MODE_BLE;

            std::thread([target]() {
                Sleep(50);
                switchMode(target);
                }).detach();
            return;
        }

        // ---- HANDSHAKE ----
        if (!st.identified) {
            if (p[0] == 0xF1 && n >= 2) {
                st.identified = true;
                if (p[1] == 0x02) {  // BLE role → auto-verify
                    st.verified = true;
                    uint8_t r = 0xF4;
                    sendProtocolResponse(ble, &r, 1);
                    Sleep(30);
                    sendLedStatusResponse(ble);


                    // BLE auto-verify → báo CONNECTED
                    if (ble) {
                        notifyStatus(HID_STATUS_CONNECTED_BLE, HID_TRANSPORT_BLE,
                            L"BLE client connected (auto-verified)");
                    }
                }
                else {
                    uint8_t r = 0xF2;
                    sendProtocolResponse(ble, &r, 1);
                }
            }
            return;
        }

        // ---- AUTH ----
        if (!st.verified) {
            if (p[0] == 0xF3 && n >= 2 && n >= 2 + p[1]) {
                if (passwordMatches(p + 2, p[1])) {
                    st.verified = true;
                    uint8_t r = 0xF4;
                    sendProtocolResponse(ble, &r, 1);
                    Sleep(30);
                    sendLedStatusResponse(ble);


                    //  auth OK → báo CONNECTED
                    if (ble) {
                        notifyStatus(HID_STATUS_CONNECTED_BLE, HID_TRANSPORT_BLE,
                            L"BLE client authenticated");
                    }
                    else {
                        notifyStatus(HID_STATUS_CONNECTED_WIFI, HID_TRANSPORT_WIFI,
                            L"Wi-Fi client authenticated");
                    }
                }
                else {
                    uint8_t r = 0xF5;
                    sendProtocolResponse(ble, &r, 1);

                    //  auth fail → quay về AUTHENTICATING
                    if (ble) {
                        notifyStatus(HID_STATUS_AUTHENTICATING, HID_TRANSPORT_BLE,
                            L"BLE auth failed - waiting for retry");
                    }
                    else {
                        notifyStatus(HID_STATUS_AUTHENTICATING, HID_TRANSPORT_WIFI,
                            L"Wi-Fi auth failed - waiting for retry");
                    }
                }
            }
            return;
        }


        // ---- REPORT CURRENT APP (0xE1) ---- 
        if (p[0] == 0xE1) {
            currentAppRequest(ble);
            return;
        }

        // ---- HID frames ----
        // 1. Thực thi lệnh chuột/bàn phím ngay lập tức (0ms delay cho chuột)
        gInput.process(p, n);

        // 2. Kiểm tra nếu gói tin V2 vừa xử lý là lệnh Tap (0x04) của 1 trong 3 phím Lock
        // Cấu trúc gói V2 Tap: [0xAA, 0x01, 0x04, 0x02, Modifiers, KeyCode]
        if (n >= 6 && p[0] == 0xAA && p[1] == 0x01 && p[2] == 0x04) {
            uint8_t key = p[5];
            if (key == 0x39 || key == 0x53 || key == 0x47) { // Caps (0x39), Num (0x53), Scroll (0x47)
                Sleep(10); // Đợi 10ms duy nhất cho 3 phím này để Windows lật bit đèn
                sendLedStatusResponse(ble);
            }
        }

    }

    // ============================================================
    //  Fallback watchdog — WIFI → BLE (PC mất mạng vật lý)
    // ============================================================
    static void fallbackWatchdogLoop() {
        std::unique_lock<std::mutex> lk(gFallbackMutex);

        while (!gFallbackStop.load()) {
            auto status = gFallbackCv.wait_for(
                lk,
                std::chrono::seconds(NETWORK_LOST_GRACE_SEC));

            if (gFallbackStop.load()) break;

            // Timeout hết 30s
            if (status == std::cv_status::timeout) {
                if (gMode.load() == HID_MODE_WIFI &&
                    gNetworkLost.load()) {

                    // Kiểm tra lại lần cuối
                    if (!NetworkMonitor_HasActive()) {
                        gNetworkLost = false;

                        // Detach để không block watchdog
                        std::thread([]() {
                            notifyStatus(HID_STATUS_SWITCHING,
                                HID_TRANSPORT_NONE,
                                L"PC network lost > 30s - auto switch to BLE");
                            switchMode(HID_MODE_BLE);
                            }).detach();
                    }
                    else {
                        // Mạng có lại giữa chừng
                        gNetworkLost = false;
                    }
                }
            }
        }
    }

    // ============================================================
    //  Bluetooth fallback watchdog — BLE → WIFI (grace 3s)
    // ============================================================
    static void btFallbackWatchdogLoop() {
        std::unique_lock<std::mutex> lk(gBtFallbackMutex);

        while (!gBtFallbackStop.load()) {
            auto status = gBtFallbackCv.wait_for(
                lk,
                std::chrono::seconds(BT_LOST_GRACE_SEC));

            if (gBtFallbackStop.load()) break;

            if (status == std::cv_status::timeout) {
                if (gMode.load() == HID_MODE_BLE &&
                    gBtLost.load()) {

                    // Kiểm tra lại lần cuối
                    if (!BluetoothMonitor_IsAvailable()) {
                        gBtLost = false;

                        std::thread([]() {
                            notifyStatus(HID_STATUS_SWITCHING,
                                HID_TRANSPORT_NONE,
                                L"BT lost > 3s - switching to WIFI");
                            switchMode(HID_MODE_WIFI);
                            }).detach();
                    }
                    else {
                        // BT có lại giữa chừng
                        gBtLost = false;
                    }
                }
            }
        }
    }

    // ============================================================
    //  Callbacks từ NetworkMonitor
    //  ⚠️ Notify SAU khi release lock để tránh giữ lock lâu
    // ============================================================
    static void onNetworkLost() {
        {
            std::lock_guard<std::mutex> lk(gFallbackMutex);
            if (gMode.load() != HID_MODE_WIFI) {
                return;
            }
            gNetworkLost = true;
        }

        notifyStatus(HID_STATUS_IDLE, HID_TRANSPORT_WIFI,
            L"PC lost network - grace 30s");

        gFallbackCv.notify_one();
    }

    static void onNetworkRestored() {
        {
            std::lock_guard<std::mutex> lk(gFallbackMutex);
            gNetworkLost = false;
        }

        notifyStatus(HID_STATUS_IDLE, HID_TRANSPORT_WIFI,
            L"PC network restored");

        gFallbackCv.notify_one();
    }

    // ============================================================
    //  Callbacks từ BluetoothMonitor
    //  ⚠️ Notify SAU khi release lock
    // ============================================================
    static void onBtLost() {
        {
            std::lock_guard<std::mutex> lk(gBtFallbackMutex);
            if (gMode.load() != HID_MODE_BLE) {
                return;
            }
            gBtLost = true;
        }

        notifyStatus(HID_STATUS_IDLE, HID_TRANSPORT_BLE,
            L"PC Bluetooth lost - grace 3s");

        gBtFallbackCv.notify_one();
    }

    static void onBtRestored() {
        {
            std::lock_guard<std::mutex> lk(gBtFallbackMutex);
            gBtLost = false;
        }

        notifyStatus(HID_STATUS_IDLE, HID_TRANSPORT_BLE,
            L"PC Bluetooth restored");

        gBtFallbackCv.notify_one();
    }

    // ============================================================
    //  switchMode
    //
    //  ⚠️ CÓ MUTEX BẢO VỆ — tránh 2 thread cùng switch
    //     (UI + watchdog + protocol frame)
    //
    //  - Chuyển sang BLE: dùng startBleWithFallback (Cách A)
    //    → Nếu BLE fail, tự fallback WIFI
    //  - Chuyển sang WIFI: dùng switchToWifi
    // ============================================================
    static void switchMode(HID_Mode newMode) {
        // Lock toàn bộ quá trình switch
        std::lock_guard<std::mutex> switchLock(gSwitchMutex);

        // Check lại SAU lock — nếu thread khác đã switch xong thì return
        if (gMode.load() == newMode) return;

        notifyStatus(HID_STATUS_SWITCHING, HID_TRANSPORT_NONE,
            L"Switching mode...");

        gInput.releaseAll();
        Sleep(50);

        HID_Mode oldMode = gMode.load();
        gMode = newMode;

        // Reset network-lost state (notify SAU khi release lock)
        {
            std::lock_guard<std::mutex> lk(gFallbackMutex);
            gNetworkLost = false;
        }
        gFallbackCv.notify_all();

        // Reset BT-lost state
        {
            std::lock_guard<std::mutex> lk(gBtFallbackMutex);
            gBtLost = false;
        }
        gBtFallbackCv.notify_all();

        // Stop tất cả transport cũ
        BleGattServer_Stop();
        if (oldMode == HID_MODE_WIFI) {
            TcpServer_Stop();
        }
        Sleep(300);

        // Start mode mới
        switch (newMode) {
        case HID_MODE_BLE:
            startBleWithFallback();
            break;

        case HID_MODE_WIFI:
            switchToWifi();
            break;
        }
    }

    // ============================================================
    //  BLE dispatch
    // ============================================================
    void BleGattDispatch(const uint8_t* data, size_t len) {
        handleProtocolFrame(data, len, true);
    }

    void BleGattDisconnected() {
        resetAuthForTransport(true);
    }

} // namespace mouseKeyboard

// ============================================================
//  Public C API
// ============================================================
extern "C" {

    using namespace mouseKeyboard;

    static std::wstring safeStr(const wchar_t* p, const wchar_t* d) {
        return p && *p ? p : d;
    }

    int HID_Initialize(const HID_Config* c, HID_StatusCallback cb, void* u) {
        if (gStarted) return 1;

        gCfg = Config{};
        if (c) {
            gCfg.host = safeStr(c->wifi_host, L"0.0.0.0");
            gCfg.port = c->wifi_port ? c->wifi_port : 2000;
            gCfg.password = safeStr(c->wifi_password, L"123456");
            gCfg.bleName = safeStr(c->ble_device_name, L"BLE_COMPUTER_REMOTE");
            gCfg.service = safeStr(c->ble_service_uuid, L"2D2A0001-8A5A-4E76-A2E3-1E57D9A1B001");
            gCfg.writeUuid = safeStr(c->ble_write_uuid, L"2D2A0002-8A5A-4E76-A2E3-1E57D9A1B001");
            gCfg.notifyUuid = safeStr(c->ble_notify_uuid, L"2D2A0003-8A5A-4E76-A2E3-1E57D9A1B001");
            gCfg.timeoutMs = c->connect_timeout_ms ? c->connect_timeout_ms : 5000;
            gCfg.retryMs = c->retry_interval_ms ? c->retry_interval_ms : 3000;
            gCfg.autoEnableBt = c->auto_enable_bluetooth != 0;
            gCfg.startMode = c->start_mode;
        }

        gCallback = cb;
        gUser = u;

        WSADATA w{};
        if (WSAStartup(MAKEWORD(2, 2), &w) != 0) return 0;

        gStop = false;
        gStarted = true;
        gMode = gCfg.startMode;

        return 1;
    }

    int HID_Connect(void) {
        if (!gStarted) return 0;

        // ---- Start NetworkMonitor (chỉ 1 lần) ----
        static bool netMonitorStarted = false;
        if (!netMonitorStarted) {
            NetworkMonitor_Start(onNetworkLost, onNetworkRestored);
            netMonitorStarted = true;
        }

        // ---- Start BluetoothMonitor (chỉ 1 lần) ----
        static bool btMonitorStarted = false;
        if (!btMonitorStarted) {
            BluetoothMonitor_Start(onBtLost, onBtRestored);
            btMonitorStarted = true;
        }

        // ---- Start fallback watchdog network (1 lần) ----
        if (!gFallbackThread.joinable()) {
            gFallbackStop = false;
            gFallbackThread = std::thread(fallbackWatchdogLoop);
        }

        // ---- Start fallback watchdog BLE (1 lần) ----
        if (!gBtFallbackThread.joinable()) {
            gBtFallbackStop = false;
            gBtFallbackThread = std::thread(btFallbackWatchdogLoop);
        }

        // ---- Start transport theo mode hiện tại ----
        switch (gMode.load()) {
        case HID_MODE_BLE:
            // Kiểm tra BT có sẵn không trước khi thử
            if (!BluetoothMonitor_IsAvailable()) {
                notifyStatus(HID_STATUS_SWITCHING, HID_TRANSPORT_NONE,
                    L"BT not available - switching to WIFI");
                switchToWifi();
                break;
            }
            startBleWithFallback();
            break;

        case HID_MODE_WIFI:
            // Kiểm tra WiFi có sẵn không
            if (!NetworkMonitor_HasWifiAdapter()) {
                notifyStatus(HID_STATUS_IDLE, HID_TRANSPORT_NONE,
                    L"WiFi adapter not available - waiting...");
                // Vẫn start TCP để bind port (sẽ không có client nào kết nối)
                // Khi adapter lên, onNetworkRestored() sẽ báo
            }
            if (TcpServer_Start()) {
                notifyStatus(HID_STATUS_IDLE, HID_TRANSPORT_WIFI,
                    L"TCP listening; waiting for client");
            }
            break;
        }


        return 1;
    }

    int HID_SwitchMode(HID_Mode mode) {
        if (!gStarted) return 0;
        switchMode(mode);
        return 1;
    }

    HID_Mode HID_GetMode(void) {
        return gMode.load();
    }

    void HID_Disconnect(void) {
        if (!gStarted) return;
        gStop = true;

        // ---- Stop network watchdog ----
        {
            std::lock_guard<std::mutex> lk(gFallbackMutex);
            gFallbackStop = true;
        }
        gFallbackCv.notify_all();

        if (gFallbackThread.joinable() &&
            gFallbackThread.get_id() != std::this_thread::get_id()) {
            gFallbackThread.join();
        }

        // ---- Stop BT watchdog ----
        {
            std::lock_guard<std::mutex> lk(gBtFallbackMutex);
            gBtFallbackStop = true;
        }
        gBtFallbackCv.notify_all();

        if (gBtFallbackThread.joinable() &&
            gBtFallbackThread.get_id() != std::this_thread::get_id()) {
            gBtFallbackThread.join();
        }

        // ---- Stop monitors ----
        NetworkMonitor_Stop();
        BluetoothMonitor_Stop();

        TcpServer_Stop();
        BleGattServer_Stop();
        gInput.releaseAll();

        notifyStatus(HID_STATUS_IDLE, HID_TRANSPORT_NONE, L"Disconnected");
    }

    void HID_Shutdown(void) {
        HID_Disconnect();
        WSACleanup();
        gStarted = false;
    }

    HID_Status HID_GetStatus(void) {
        return gStatus.load();
    }

    HID_Transport HID_GetTransport(void) {
        return gTransport.load();
    }

    int HID_IsNetworkUp(void) {
        return NetworkMonitor_IsUp() ? 1 : 0;
    }

    int HID_HasWifiAdapter(void) {
        return NetworkMonitor_HasWifiAdapter() ? 1 : 0;
    }

    int HID_IsBluetoothAvailable(void) {
        return BluetoothMonitor_IsAvailable() ? 1 : 0;
    }

} // extern "C"