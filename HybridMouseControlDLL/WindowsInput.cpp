#include "pch.h"

namespace mouseKeyboard {

    // ============================================================
    //  Usage → VK mapping
    // ============================================================
    static WORD usageToVk(uint8_t u) {
        if (u >= 0x04 && u <= 0x1D) return (WORD)('A' + u - 0x04);
        if (u >= 0x1E && u <= 0x26) return (WORD)('1' + u - 0x1E);
        if (u == 0x27)              return '0';
        if (u >= 0x3A && u <= 0x45) return (WORD)(VK_F1 + u - 0x3A);
        if (u >= 0x68 && u <= 0x73) return (WORD)(VK_F13 + u - 0x68);
        switch (u) {
        case 0x28: return VK_RETURN;
        case 0x29: return VK_ESCAPE;
        case 0x2A: return VK_BACK;
        case 0x2B: return VK_TAB;
        case 0x2C: return VK_SPACE;
        case 0x2D: return VK_OEM_MINUS;
        case 0x2E: return VK_OEM_PLUS;
        case 0x2F: return VK_OEM_4;
        case 0x30: return VK_OEM_6;
        case 0x31: return VK_OEM_5;
        case 0x33: return VK_OEM_1;
        case 0x34: return VK_OEM_7;
        case 0x35: return VK_OEM_3;
        case 0x36: return VK_OEM_COMMA;
        case 0x37: return VK_OEM_PERIOD;
        case 0x38: return VK_OEM_2;
        case 0x39: return VK_CAPITAL;
        case 0x46: return VK_SNAPSHOT;
        case 0x47: return VK_SCROLL;
        case 0x48: return VK_PAUSE;
        case 0x49: return VK_INSERT;
        case 0x4A: return VK_HOME;
        case 0x4B: return VK_PRIOR;
        case 0x4C: return VK_DELETE;
        case 0x4D: return VK_END;
        case 0x4E: return VK_NEXT;
        case 0x4F: return VK_RIGHT;
        case 0x50: return VK_LEFT;
        case 0x51: return VK_DOWN;
        case 0x52: return VK_UP;
        case 0x53: return VK_NUMLOCK;
        case 0x54: return VK_DIVIDE;
        case 0x55: return VK_MULTIPLY;
        case 0x56: return VK_SUBTRACT;
        case 0x57: return VK_ADD;
        case 0x58: return VK_RETURN;
        case 0x59: return VK_NUMPAD1;
        case 0x5A: return VK_NUMPAD2;
        case 0x5B: return VK_NUMPAD3;
        case 0x5C: return VK_NUMPAD4;
        case 0x5D: return VK_NUMPAD5;
        case 0x5E: return VK_NUMPAD6;
        case 0x5F: return VK_NUMPAD7;
        case 0x60: return VK_NUMPAD8;
        case 0x61: return VK_NUMPAD9;
        case 0x62: return VK_NUMPAD0;
        case 0x63: return VK_DECIMAL;
        case 0xE0: return VK_LCONTROL;
        case 0xE1: return VK_LSHIFT;
        case 0xE2: return VK_LMENU;
        case 0xE3: return VK_LWIN;
        case 0xE4: return VK_RCONTROL;
        case 0xE5: return VK_RSHIFT;
        case 0xE6: return VK_RMENU;
        case 0xE7: return VK_RWIN;
        default:   return 0;
        }
    }

    static void sendKey(WORD vk, bool down, bool extended = false) {
        if (!vk) return;
        INPUT in{};
        in.type = INPUT_KEYBOARD;
        in.ki.wVk = vk;
        in.ki.dwFlags = (down ? 0 : KEYEVENTF_KEYUP) |
            (extended ? KEYEVENTF_EXTENDEDKEY : 0);
        SendInput(1, &in, sizeof(in));
    }

    // ============================================================
    //  Ctor / Dtor
    // ============================================================
    InputEngine::InputEngine() { startWorker(); }
    InputEngine::~InputEngine() { stopWorker(); }

    void InputEngine::startWorker() {
        kbStop_ = false;
        kbWorker_ = std::thread(&InputEngine::workerLoop, this);
    }

    void InputEngine::stopWorker() {
        kbStop_ = true;
        kbQueueCv_.notify_all();
        if (kbWorker_.joinable()) kbWorker_.join();
    }

    // ============================================================
    //  Mouse — direct
    // ============================================================
    void InputEngine::mouseMove(int8_t dx, int8_t dy) {
        INPUT in{};
        in.type = INPUT_MOUSE;
        in.mi.dx = dx;
        in.mi.dy = dy;
        in.mi.dwFlags = MOUSEEVENTF_MOVE;
        SendInput(1, &in, sizeof(in));
    }

    void InputEngine::mouseScroll(int8_t v, int8_t h) {
        if (v) {
            INPUT in{};
            in.type = INPUT_MOUSE;
            in.mi.dwFlags = MOUSEEVENTF_HWHEEL ;
            in.mi.mouseData = (DWORD)(SHORT)(v * WHEEL_DELTA);
            SendInput(1, &in, sizeof(in));
        }
        if (h) {
            INPUT in{};
            in.type = INPUT_MOUSE;
            in.mi.dwFlags = MOUSEEVENTF_WHEEL;
            in.mi.mouseData = (DWORD)(SHORT)(h * WHEEL_DELTA);
            SendInput(1, &in, sizeof(in));
        }
    }

    // ============================================================
    //  Mouse button mapping — KHỚP HidConstants.kt
    //    LEFT    = 0x01
    //    RIGHT   = 0x02
    //    MIDDLE  = 0x04   ← FIX (trước là 3)
    //    BACK    = 0x08   ← FIX (trước là 4)
    //    FORWARD = 0x10   ← THÊM
    // ============================================================
    static DWORD mouseDownFlag(uint8_t b) {
        switch (b) {
        case 0x01: return MOUSEEVENTF_LEFTDOWN;
        case 0x02: return MOUSEEVENTF_RIGHTDOWN;
        case 0x04: return MOUSEEVENTF_MIDDLEDOWN;   // ← FIX
        case 0x08: return MOUSEEVENTF_XDOWN;        // BACK
        case 0x10: return MOUSEEVENTF_XDOWN;        // FORWARD
        default: return 0;
        }
    }

    static DWORD mouseUpFlag(uint8_t b) {
        switch (b) {
        case 0x01: return MOUSEEVENTF_LEFTUP;
        case 0x02: return MOUSEEVENTF_RIGHTUP;
        case 0x04: return MOUSEEVENTF_MIDDLEUP;     // ← FIX
        case 0x08: return MOUSEEVENTF_XUP;          // BACK
        case 0x10: return MOUSEEVENTF_XUP;          // FORWARD
        default: return 0;
        }
    }

    // XBUTTON data cho BACK/FORWARD
    static DWORD mouseXButtonData(uint8_t b) {
        switch (b) {
        case 0x08: return XBUTTON1;   // BACK
        case 0x10: return XBUTTON2;   // FORWARD
        default:   return 0;
        }
    }

    // ============================================================
    //  Mouse down — dùng HID bitmask
    // ============================================================
    void InputEngine::mouseDown(uint8_t b) {
        std::lock_guard<std::mutex> lk(stateMutex_);

        // Chỉ chấp nhận LEFT/RIGHT/MIDDLE/BACK/FORWARD
        if (b != 0x01 && b != 0x02 && b != 0x04 &&
            b != 0x08 && b != 0x10) {
            return;
        }

        if (buttons_ & b) return;   // đã down rồi

        INPUT in{};
        in.type = INPUT_MOUSE;
        in.mi.dwFlags = mouseDownFlag(b);
        in.mi.mouseData = mouseXButtonData(b);
        SendInput(1, &in, sizeof(in));
        buttons_ |= b;
    }

    void InputEngine::mouseUp(uint8_t b) {
        std::lock_guard<std::mutex> lk(stateMutex_);

        if (b != 0x01 && b != 0x02 && b != 0x04 &&
            b != 0x08 && b != 0x10) {
            return;
        }

        if (!(buttons_ & b)) return;   // chưa down

        INPUT in{};
        in.type = INPUT_MOUSE;
        in.mi.dwFlags = mouseUpFlag(b);
        in.mi.mouseData = mouseXButtonData(b);
        SendInput(1, &in, sizeof(in));
        buttons_ &= ~b;
    }

    void InputEngine::mouseClick(uint8_t b) {
        mouseDown(b);
        mouseUp(b);
    }

    // ============================================================
    //  Keyboard — worker thread only
    // ============================================================
    void InputEngine::setModifiers(uint8_t m) {
        static const WORD vks[8] = {
            VK_LCONTROL, VK_LSHIFT, VK_LMENU, VK_LWIN,
            VK_RCONTROL, VK_RSHIFT, VK_RMENU, VK_RWIN
        };
        for (int i = 0; i < 8; ++i) {
            bool was = (modifiers_ >> i) & 1;
            bool now = (m >> i) & 1;
            if (was != now) sendKey(vks[i], now, i >= 3);
        }
        modifiers_ = m;
    }

    void InputEngine::keyDown(uint8_t u) {
        std::lock_guard<std::mutex> lk(stateMutex_);
        if (!u || keysDown_[u]) return;
        WORD vk = usageToVk(u);
        if (vk) {
            bool ext = (u >= 0x4F && u <= 0x52);
            sendKey(vk, true, ext);
            keysDown_[u] = true;
        }
    }

    void InputEngine::keyUp(uint8_t u) {
        std::lock_guard<std::mutex> lk(stateMutex_);
        if (!u || !keysDown_[u]) return;
        WORD vk = usageToVk(u);
        if (vk) {
            bool ext = (u >= 0x4F && u <= 0x52);
            sendKey(vk, false, ext);
            keysDown_[u] = false;
        }
    }

    void InputEngine::keyTap(uint8_t modifiers, uint8_t u) {
        uint8_t saved = modifiers_;
        setModifiers(modifiers);
        WORD vk = usageToVk(u);
        if (vk) {
            bool ext = (u >= 0x4F && u <= 0x52);
            sendKey(vk, true, ext);
            sendKey(vk, false, ext);
        }
        setModifiers(saved);
    }

    // ============================================================
    //  Gửi ký tự UTF-8 trực tiếp qua SendInput + KEYEVENTF_UNICODE
    // ============================================================
    void InputEngine::textUtf8(const uint8_t* data, size_t len) {
        if (!data || !len) return;

        int wideLen = MultiByteToWideChar(
            CP_UTF8, 0,
            reinterpret_cast<const char*>(data), (int)len,
            nullptr, 0);
        if (wideLen <= 0) return;

        std::wstring wide(wideLen, 0);
        MultiByteToWideChar(
            CP_UTF8, 0,
            reinterpret_cast<const char*>(data), (int)len,
            &wide[0], wideLen);

        for (wchar_t wc : wide) {
            if (wc == 0) continue;

            INPUT in[2]{};

            in[0].type = INPUT_KEYBOARD;
            in[0].ki.wVk = 0;
            in[0].ki.wScan = wc;
            in[0].ki.dwFlags = KEYEVENTF_UNICODE;

            in[1].type = INPUT_KEYBOARD;
            in[1].ki.wVk = 0;
            in[1].ki.wScan = wc;
            in[1].ki.dwFlags = KEYEVENTF_UNICODE | KEYEVENTF_KEYUP;

            SendInput(2, in, sizeof(INPUT));
        }
    }

    // ============================================================
    //  Consumer — KHỚP HidConstants.Consumer
    // ============================================================
    void InputEngine::consumerAction(uint8_t cmd, uint16_t usage) {
        WORD vk = 0;
        switch (usage) {
        case 0x00E2: vk = VK_VOLUME_MUTE;      break;   // MUTE
        case 0x00E9: vk = VK_VOLUME_UP;        break;   // VOLUME_UP
        case 0x00EA: vk = VK_VOLUME_DOWN;      break;   // VOLUME_DOWN
        case 0x00CD: vk = VK_MEDIA_PLAY_PAUSE; break;   // PLAY_PAUSE
        case 0x00B5: vk = VK_MEDIA_NEXT_TRACK; break;   // NEXT_TRACK
        case 0x00B6: vk = VK_MEDIA_PREV_TRACK; break;   // PREV_TRACK
        case 0x00B7: vk = VK_MEDIA_STOP;       break;   // STOP

            // Brightness — gọi hàm riêng, không map sang VK
        case 0x006F:  // BRIGHTNESS_UP
            if (cmd == 0x20) changeBrightness(+10);
            return;

        case 0x0070:  // BRIGHTNESS_DOWN
            if (cmd == 0x20) changeBrightness(-10);
            return;

            // MENU_PICK → Enter
        case 0x009E: vk = VK_RETURN;           break;   // ← FIX

            // MENU_ESCAPE → Escape
        case 0x0076: vk = VK_ESCAPE;           break;   // ← FIX

            // FAST_FORWARD / REWIND — best effort
        case 0x00B3: vk = VK_MEDIA_NEXT_TRACK; break;
        case 0x00B4: vk = VK_MEDIA_PREV_TRACK; break;

        default: return;
        }

        bool isTap = (cmd == 0x20);
        bool isDown = (cmd == 0x21);
        bool isUp = (cmd == 0x22);
        if (isTap || isDown) sendKey(vk, true, true);
        if (isTap || isUp)   sendKey(vk, false, true);
    }

    // ============================================================
    //  System action — KHỚP HidConstants.SystemAction
    // ============================================================
    void InputEngine::systemAction(uint8_t action) {
        switch (action) {
        case 0x01: case 0x02: case 0x03: break;
        default: return;
        }

        switch (action) {
        case 0x01: {  // WIN_POWER
            system("shutdown /s /t 0 /f");
            break;
        }
        case 0x02: {  // WIN_RESTART
            system("shutdown /r /t 0 /f");
            break;
        }
        case 0x03: {  // WIN_SLEEP
            SetSuspendState(FALSE, TRUE, FALSE);
            break;
        }
        }

        setModifiers(0x00);
        releaseAll();
    }

    // ============================================================
    //  Enqueue API
    // ============================================================
    void InputEngine::enqueueSetModifiers(uint8_t mask) {
        KbEvent e{ KBEV_SET_MODIFIERS, mask, 0, 0 };
        std::lock_guard<std::mutex> lk(kbQueueMutex_);
        kbQueue_.push_back(e);
        kbQueueCv_.notify_one();
    }
    void InputEngine::enqueueKeyDown(uint8_t u) {
        KbEvent e{ KBEV_KEY_DOWN, u, 0, 0 };
        std::lock_guard<std::mutex> lk(kbQueueMutex_);
        kbQueue_.push_back(e);
        kbQueueCv_.notify_one();
    }
    void InputEngine::enqueueKeyUp(uint8_t u) {
        KbEvent e{ KBEV_KEY_UP, u, 0, 0 };
        std::lock_guard<std::mutex> lk(kbQueueMutex_);
        kbQueue_.push_back(e);
        kbQueueCv_.notify_one();
    }
    void InputEngine::enqueueKeyTap(uint8_t modifiers, uint8_t u) {
        KbEvent e{ KBEV_KEY_TAP, modifiers, u, 0 };
        std::lock_guard<std::mutex> lk(kbQueueMutex_);
        kbQueue_.push_back(e);
        kbQueueCv_.notify_one();
    }
    void InputEngine::enqueueConsumer(uint8_t cmd, uint16_t usage) {
        uint8_t t = (cmd == 0x20) ? KBEV_CONSUMER_TAP
            : (cmd == 0x21) ? KBEV_CONSUMER_DOWN
            : KBEV_CONSUMER_UP;
        KbEvent e{ t, 0, 0, usage };
        std::lock_guard<std::mutex> lk(kbQueueMutex_);
        kbQueue_.push_back(e);
        kbQueueCv_.notify_one();
    }
    void InputEngine::enqueueSystemAction(uint8_t action) {
        KbEvent e{ KBEV_SYSTEM_ACTION, action, 0, 0 };
        std::lock_guard<std::mutex> lk(kbQueueMutex_);
        kbQueue_.push_back(e);
        kbQueueCv_.notify_one();
    }

    // ============================================================
    //  Worker loop
    // ============================================================
    void InputEngine::workerLoop() {
        while (!kbStop_) {
            KbEvent e{};
            {
                std::unique_lock<std::mutex> lk(kbQueueMutex_);
                kbQueueCv_.wait(lk, [this] { return kbStop_ || !kbQueue_.empty(); });
                if (kbStop_ && kbQueue_.empty()) return;
                e = kbQueue_.front();
                kbQueue_.pop_front();
            }
            switch (e.type) {
            case KBEV_SET_MODIFIERS: setModifiers(e.a); break;
            case KBEV_KEY_DOWN:      keyDown(e.a);      break;
            case KBEV_KEY_UP:        keyUp(e.a);        break;
            case KBEV_KEY_TAP:       keyTap(e.a, e.b);  break;
            case KBEV_CONSUMER_TAP:
            case KBEV_CONSUMER_DOWN:
            case KBEV_CONSUMER_UP:
                consumerAction(
                    e.type == KBEV_CONSUMER_TAP ? 0x20 :
                    e.type == KBEV_CONSUMER_DOWN ? 0x21 : 0x22,
                    e.usage);
                break;
            case KBEV_SYSTEM_ACTION: systemAction(e.a); break;
            default: break;
            }
        }
    }

    // ============================================================
    //  Parser
    // ============================================================
    void InputEngine::process(const uint8_t* d, size_t n) {
        if (!d || !n) return;
        if (n >= 2 && d[0] == 0xAA && d[1] == 0x01) v2(d, n);
        else if (d[0] >= 0x01 && d[0] <= 0x05)      legacy(d, n);
    }

    void InputEngine::legacy(const uint8_t* d, size_t n) {
        if (n < 3) return;
        switch (d[0]) {
        case 0x01: enqueueKeyTap(d[1], d[2]);               break;
        case 0x02: mouseMove((int8_t)d[1], (int8_t)d[2]);   break;
        case 0x03: mouseClick(d[1]);                        break;
        case 0x04: mouseScroll((int8_t)d[1], (int8_t)d[2]); break;
        case 0x05: enqueueSystemAction(d[1]);               break;
        default: break;
        }
    }

    void InputEngine::v2(const uint8_t* d, size_t n) {
        size_t i = 2;
        while (i + 1 < n) {
            uint8_t c = d[i++];
            uint8_t l = d[i++];
            if (i + l > n) break;
            const uint8_t* p = d + i;
            switch (c) {
            case 0x01: if (l == 1) enqueueSetModifiers(p[0]);             break;
            case 0x02: if (l == 1) enqueueKeyDown(p[0]);                  break;
            case 0x03: if (l == 1) enqueueKeyUp(p[0]);                    break;
            case 0x04: if (l == 2) enqueueKeyTap(p[0], p[1]);             break;
            case 0x05: if (l > 0)  textUtf8(p, l);                        break;
            case 0x10: if (l == 2) mouseMove((int8_t)p[0], (int8_t)p[1]); break;
            case 0x11: if (l == 2) mouseScroll((int8_t)p[0], (int8_t)p[1]); break;
            case 0x12: if (l == 1) mouseClick(p[0]);                      break;
            case 0x13: if (l == 1) mouseDown(p[0]);                       break;
            case 0x14: if (l == 1) mouseUp(p[0]);                         break;
            case 0x20:
            case 0x21:
            case 0x22:
                if (l == 2) {
                    uint16_t usage = (uint16_t)((p[0] << 8) | p[1]);
                    enqueueConsumer(c, usage);
                }
                break;
            case 0x30:
                if (l >= 1) enqueueSystemAction(p[0]);
                break;
            default: break;
            }
            i += l;
        }
    }

    // ============================================================
    //  Release all — dùng HID bitmask
    // ============================================================
    void InputEngine::releaseAll() {
        std::lock_guard<std::mutex> lk(stateMutex_);

        for (int i = 0; i < 256; ++i) {
            if (keysDown_[i]) {
                WORD vk = usageToVk((uint8_t)i);
                if (vk) sendKey(vk, false, i >= 0x4F && i <= 0x52);
                keysDown_[i] = false;
            }
        }
        setModifiers(0x00);

        // Release tất cả mouse buttons theo HID bitmask
        const uint8_t btnList[] = { 0x01, 0x02, 0x04, 0x08, 0x10 };
        for (uint8_t b : btnList) {
            if (buttons_ & b) {
                INPUT in{};
                in.type = INPUT_MOUSE;
                in.mi.dwFlags = mouseUpFlag(b);
                in.mi.mouseData = mouseXButtonData(b);
                SendInput(1, &in, sizeof(in));
                buttons_ &= ~b;
            }
        }
    }

    // ============================================================
    //  Brightness control — dùng PowerShell WMI
    // ============================================================
    void InputEngine::changeBrightness(int delta) {
        if (delta == 0) return;

        static int brightness = 50;

        brightness += delta;
        if (brightness > 100) brightness = 100;
        if (brightness < 0)   brightness = 0;

        char cmd[512];
        sprintf_s(cmd, sizeof(cmd),
            "powershell -windowstyle hidden -command "
            "\"(Get-WmiObject -Namespace root/wmi "
            "-Class WmiMonitorBrightnessMethods)"
            ".WmiSetBrightness(0,%d)\"",
            brightness);

        STARTUPINFOA si = { sizeof(si) };
        PROCESS_INFORMATION pi{};
        si.dwFlags = STARTF_USESHOWWINDOW;
        si.wShowWindow = SW_HIDE;

        if (CreateProcessA(NULL, cmd, NULL, NULL, FALSE,
            CREATE_NO_WINDOW, NULL, NULL, &si, &pi)) {
            CloseHandle(pi.hProcess);
            CloseHandle(pi.hThread);
        }
    }

} // namespace mouseKeyboard