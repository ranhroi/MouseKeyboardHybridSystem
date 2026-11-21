/* TcpServer.cpp
 *
 * Toàn bộ logic TCP socket.
 * Bao gồm:
 *   - Listen socket, accept loop
 *   - Client management (cooldown, đá client cũ)
 *   - TCP keep-alive
 *   - Frame parser (V2 batch + legacy)
 *   - Gọi handleProtocolFrame() để xử lý lệnh
 *
 * KHÔNG có fallback logic.
 * Fallback do NetworkMonitor + ServerCore quản lý.
 *
 * "Tắt Wi-Fi" = TcpServer_Stop() = đóng socket, KHÔNG tắt adapter Windows.
 */

#include "pch.h"

namespace mouseKeyboard {

    // ============================================================
    //  TCP-specific state
    // ============================================================
    static std::mutex         gTcpLock;
    static SOCKET             gListen = INVALID_SOCKET;
    static SOCKET             gClient = INVALID_SOCKET;
    static std::thread        gTcpThread;
    static std::atomic<bool>  gTcpRunning{ false };
    static std::atomic<bool>  gTcpStop{ false };

    static constexpr uint8_t CMD_KEY_UTF8 = 0x05;
    static unsigned long long gLastClientChangeMs = 0;
    static const unsigned long long CLIENT_CHANGE_COOLDOWN_MS = 500;

    // ============================================================
    //  Helpers
    // ============================================================
    static bool sendAll(SOCKET s, const uint8_t* p, size_t n) {
        size_t off = 0;
        while (off < n && !gTcpStop) {
            int z = send(s, (const char*)p + off, (int)(n - off), 0);
            if (z <= 0) return false;
            off += z;
        }
        return off == n;
    }

    // ============================================================
    //  TCP loop
    // ============================================================
    static void tcpLoop() {
        while (!gTcpStop) {
            sockaddr_storage addr{};
            int alen = sizeof(addr);
            SOCKET c = accept(gListen, (sockaddr*)&addr, &alen);
            if (c == INVALID_SOCKET) {
                if (gTcpStop) break;
                Sleep(100);
                continue;
            }

            // Cooldown check
            unsigned long long now = nowMs();
            if (now - gLastClientChangeMs < CLIENT_CHANGE_COOLDOWN_MS) {
                shutdown(c, SD_BOTH);
                closesocket(c);
                continue;
            }

            // Đá client cũ
            {
                std::lock_guard<std::mutex> l(gTcpLock);
                if (gClient != INVALID_SOCKET) {
                    shutdown(gClient, SD_BOTH);
                    closesocket(gClient);
                }
                gClient = c;
            }
            gLastClientChangeMs = now;

            // Socket options
            BOOL nd = TRUE;
            setsockopt(c, IPPROTO_TCP, TCP_NODELAY, (char*)&nd, sizeof(nd));

            // TCP Keep-Alive
            {
                int ka = 1, idle = 10, intvl = 5, cnt = 3;
                setsockopt(c, SOL_SOCKET, SO_KEEPALIVE, (char*)&ka, sizeof(ka));
                setsockopt(c, IPPROTO_TCP, TCP_KEEPIDLE, (char*)&idle, sizeof(idle));
                setsockopt(c, IPPROTO_TCP, TCP_KEEPINTVL, (char*)&intvl, sizeof(intvl));
                setsockopt(c, IPPROTO_TCP, TCP_KEEPCNT, (char*)&cnt, sizeof(cnt));
            }

            // Reset auth cho client mới
            resetAuthForTransport(false);
            notifyStatus(HID_STATUS_AUTHENTICATING, HID_TRANSPORT_WIFI,
                L"Wi-Fi client connected; waiting for auth");

            // RX loop
            std::vector<uint8_t> b;
            b.reserve(4096);
            uint8_t tmp[1024];
            bool v2Batch = false;

            while (!gTcpStop) {
                fd_set f;
                FD_ZERO(&f);
                FD_SET(c, &f);
                timeval tv{ 0, 500000 };   // 500ms
                int q = select(0, &f, nullptr, nullptr, &tv);
                if (q == 0) continue;
                if (q < 0) break;

                int n = recv(c, (char*)tmp, sizeof(tmp), 0);
                if (n <= 0) break;

                b.insert(b.end(), tmp, tmp + n);

                while (!b.empty()) {
                    // ---- V2 batch header ----
                    if (!v2Batch && b.size() >= 2 &&
                        b[0] == 0xAA && b[1] == 0x01) {
                        b.erase(b.begin(), b.begin() + 2);
                        v2Batch = true;
                        continue;
                    }

                    // ---- V2 batch frame ----
                    if (v2Batch) {
                        // Header V2 khác → bỏ
                        if (b.size() >= 2 && b[0] == 0xAA && b[1] == 0x01) {
                            b.erase(b.begin(), b.begin() + 2);
                            continue;
                        }
                        if (b.size() < 2) break;

                        uint8_t cmd = b[0];
                        uint8_t len = b[1];

                        if (!isV2Command(cmd)) {
                            v2Batch = false;
                            continue;
                        }

                        size_t total = 2u + static_cast<size_t>(len);

                        // Giới hạn payload theo từng loại lệnh
                        if (cmd == CMD_KEY_UTF8) {
                            // UTF-8 được phép dài hơn (tối đa 240 byte)
                            if (len > 240) {
                                b.clear();
                                v2Batch = false;
                                continue;
                            }
                        }
                        else {
                            // Các lệnh khác giữ giới hạn 62 byte
                            if (total > 62) {
                                b.clear();
                                v2Batch = false;
                                continue;
                            }
                        }

                        if (b.size() < total) break;

                        std::vector<uint8_t> frame{ 0xAA, 0x01 };
                        frame.insert(frame.end(), b.begin(), b.begin() + total);
                        handleProtocolFrame(frame.data(), frame.size(), false);
                        b.erase(b.begin(), b.begin() + total);
                        continue;
                    }

                    // ---- Legacy/control frame ----
                    size_t sz = legacyFrameSize(b);
                    if (!sz) break;
                    if (sz > b.size()) {
                        b.erase(b.begin());
                        continue;
                    }
                    handleProtocolFrame(b.data(), sz, false);
                    b.erase(b.begin(), b.begin() + sz);
                }

                if (b.size() > 4096) b.clear();
            }

            // Client ngắt — chỉ dọn state, KHÔNG fallback
            {
                std::lock_guard<std::mutex> l(gTcpLock);
                if (gClient == c) gClient = INVALID_SOCKET;
            }
            shutdown(c, SD_BOTH);
            closesocket(c);
            resetAuthForTransport(false);

            if (!gTcpStop) {
                notifyStatus(HID_STATUS_IDLE, HID_TRANSPORT_WIFI,
                    L"Wi-Fi client disconnected; waiting for new client");
                // KHÔNG fallback. Tiếp tục accept() chờ client mới.
            }
        }
    }

    // ============================================================
    //  Public API
    // ============================================================
    bool TcpServer_Start() {
        if (gTcpRunning.load()) return true;

        const Config& cfg = getConfig();

        gListen = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
        if (gListen == INVALID_SOCKET) {
            notifyStatus(HID_STATUS_ERROR, HID_TRANSPORT_NONE,
                L"Cannot create TCP socket");
            return false;
        }
        BOOL yes = TRUE;
        setsockopt(gListen, SOL_SOCKET, SO_REUSEADDR, (char*)&yes, sizeof(yes));

        sockaddr_in a{};
        a.sin_family = AF_INET;
        a.sin_port = htons(cfg.port);
        a.sin_addr.s_addr = htonl(INADDR_ANY);

        if (bind(gListen, (sockaddr*)&a, sizeof(a)) == SOCKET_ERROR ||
            listen(gListen, 4) == SOCKET_ERROR) {
            closesocket(gListen);
            gListen = INVALID_SOCKET;
            notifyStatus(HID_STATUS_ERROR, HID_TRANSPORT_NONE,
                L"Cannot bind TCP port; check firewall");
            return false;
        }

        gTcpStop = false;
        gTcpRunning = true;
        gTcpThread = std::thread(tcpLoop);
        return true;
    }

    void TcpServer_Stop() {
        if (!gTcpRunning.load()) return;

        gTcpStop = true;

        if (gListen != INVALID_SOCKET) {
            shutdown(gListen, SD_BOTH);
            closesocket(gListen);
            gListen = INVALID_SOCKET;
        }
        if (gClient != INVALID_SOCKET) {
            shutdown(gClient, SD_BOTH);
            closesocket(gClient);
            gClient = INVALID_SOCKET;
        }
        if (gTcpThread.joinable() &&
            gTcpThread.get_id() != std::this_thread::get_id()) {
            gTcpThread.join();
        }

        resetAuthForTransport(false);
        gTcpRunning = false;
    }

    bool TcpServer_IsRunning() {
        return gTcpRunning.load();
    }

    bool TcpServer_Send(const uint8_t* data, size_t len) {
        std::lock_guard<std::mutex> l(gTcpLock);
        return gClient != INVALID_SOCKET && sendAll(gClient, data, len);
    }

    bool TcpServer_HasClient() {
        std::lock_guard<std::mutex> l(gTcpLock);
        return gClient != INVALID_SOCKET;
    }

} // namespace mouseKeyboard