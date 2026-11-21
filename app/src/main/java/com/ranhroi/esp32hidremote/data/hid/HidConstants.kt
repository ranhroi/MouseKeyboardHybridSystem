package com.ranhroi.esp32hidremote.data.hid

/**
 * Standard USB HID Usage IDs — ĐỒNG BỘ 100% VỚI FIRMWARE ESP32-S3
 * Mọi hằng số ở đây đều được firmware xử lý.
 * Không thêm hằng số nào mà firmware không hỗ trợ.
 */
object HidConstants {

    // ============================================================
    // MODIFIER MASKS — KHỚP 100% VỚI ESP32
    // ============================================================
    const val MOD_NONE: Byte = 0x00
    const val MOD_LEFT_CTRL: Byte = 0x01
    const val MOD_LEFT_SHIFT: Byte = 0x02
    const val MOD_LEFT_ALT: Byte = 0x04
    const val MOD_LEFT_GUI: Byte = 0x08
    const val MOD_RIGHT_CTRL: Byte = 0x10
    const val MOD_RIGHT_SHIFT: Byte = 0x20
    const val MOD_RIGHT_ALT: Byte = 0x40
    const val MOD_RIGHT_GUI: Byte = 0x80.toByte()

    // ============================================================
    // MOUSE BUTTONS — KHỚP 100% VỚI ESP32 (chỉ 3 nút)
    // ============================================================
    const val MOUSE_BUTTON_LEFT: Byte = 0x01
    const val MOUSE_BUTTON_RIGHT: Byte = 0x02
    const val MOUSE_BUTTON_MIDDLE: Byte = 0x04
    // LƯU Ý: BACK (0x08) và FORWARD (0x10) KHÔNG được ESP32 hỗ trợ.
    // Không thêm vào đây để tránh gửi lệnh không xử lý được.

    // ============================================================
    // KEYBOARD KEYCODES (USB HID Usage Page 0x07)
    // ESP32 dùng pressRaw(keycode) trực tiếp — không cần định nghĩa hết.
    // Android có thể gửi bất kỳ keycode 0x00–0xFF hợp lệ.
    // ============================================================
    const val KEY_NONE: Byte = 0x00

    // A → Z
    const val KEY_A: Byte = 0x04
    const val KEY_B: Byte = 0x05
    const val KEY_C: Byte = 0x06
    const val KEY_D: Byte = 0x07
    const val KEY_E: Byte = 0x08
    const val KEY_F: Byte = 0x09
    const val KEY_G: Byte = 0x0A
    const val KEY_H: Byte = 0x0B
    const val KEY_I: Byte = 0x0C
    const val KEY_J: Byte = 0x0D
    const val KEY_K: Byte = 0x0E
    const val KEY_L: Byte = 0x0F
    const val KEY_M: Byte = 0x10
    const val KEY_N: Byte = 0x11
    const val KEY_O: Byte = 0x12
    const val KEY_P: Byte = 0x13
    const val KEY_Q: Byte = 0x14
    const val KEY_R: Byte = 0x15
    const val KEY_S: Byte = 0x16
    const val KEY_T: Byte = 0x17
    const val KEY_U: Byte = 0x18
    const val KEY_V: Byte = 0x19
    const val KEY_W: Byte = 0x1A
    const val KEY_X: Byte = 0x1B
    const val KEY_Y: Byte = 0x1C
    const val KEY_Z: Byte = 0x1D

    // 1 → 0
    const val KEY_1: Byte = 0x1E
    const val KEY_2: Byte = 0x1F
    const val KEY_3: Byte = 0x20
    const val KEY_4: Byte = 0x21
    const val KEY_5: Byte = 0x22
    const val KEY_6: Byte = 0x23
    const val KEY_7: Byte = 0x24
    const val KEY_8: Byte = 0x25
    const val KEY_9: Byte = 0x26
    const val KEY_0: Byte = 0x27

    const val KEY_ENTER: Byte = 0x28
    const val KEY_ESCAPE: Byte = 0x29
    const val KEY_BACKSPACE: Byte = 0x2A
    const val KEY_TAB: Byte = 0x2B
    const val KEY_SPACE: Byte = 0x2C
    const val KEY_MINUS: Byte = 0x2D
    const val KEY_EQUAL: Byte = 0x2E
    const val KEY_LEFT_BRACKET: Byte = 0x2F
    const val KEY_RIGHT_BRACKET: Byte = 0x30
    const val KEY_BACKSLASH: Byte = 0x31
    const val KEY_SEMICOLON: Byte = 0x33
    const val KEY_QUOTE: Byte = 0x34
    const val KEY_GRAVE: Byte = 0x35
    const val KEY_COMMA: Byte = 0x36
    const val KEY_PERIOD: Byte = 0x37
    const val KEY_SLASH: Byte = 0x38

    const val KEY_CAPS_LOCK: Byte = 0x39

    // F1 → F12
    const val KEY_F1: Byte = 0x3A
    const val KEY_F2: Byte = 0x3B
    const val KEY_F3: Byte = 0x3C
    const val KEY_F4: Byte = 0x3D
    const val KEY_F5: Byte = 0x3E
    const val KEY_F6: Byte = 0x3F
    const val KEY_F7: Byte = 0x40
    const val KEY_F8: Byte = 0x41
    const val KEY_F9: Byte = 0x42
    const val KEY_F10: Byte = 0x43
    const val KEY_F11: Byte = 0x44
    const val KEY_F12: Byte = 0x45

    const val KEY_PRINT_SCREEN: Byte = 0x46
    const val KEY_SCROLL_LOCK: Byte = 0x47
    const val KEY_PAUSE: Byte = 0x48
    const val KEY_INSERT: Byte = 0x49
    const val KEY_HOME: Byte = 0x4A
    const val KEY_PAGE_UP: Byte = 0x4B
    const val KEY_DELETE: Byte = 0x4C
    const val KEY_END: Byte = 0x4D
    const val KEY_PAGE_DOWN: Byte = 0x4E
    const val KEY_RIGHT_ARROW: Byte = 0x4F
    const val KEY_LEFT_ARROW: Byte = 0x50
    const val KEY_DOWN_ARROW: Byte = 0x51
    const val KEY_UP_ARROW: Byte = 0x52
    const val KEY_NUM_LOCK: Byte = 0x53

    // ============================================================
    // CONSUMER CONTROL (Usage Page 0x0C)
    // CHỈ NHỮNG USAGE MÀ ESP32 THỰC SỰ HỖ TRỢ
    // ============================================================
    object Consumer {
        const val MUTE: Int = 0xE2
        const val VOLUME_UP: Int = 0xE9
        const val VOLUME_DOWN: Int = 0xEA
        const val PLAY_PAUSE: Int = 0xCD
        const val NEXT_TRACK: Int = 0xB5
        const val PREV_TRACK: Int = 0xB6
        const val STOP: Int = 0xB7
        const val FAST_FORWARD: Int = 0xB3
        const val REWIND: Int = 0xB4
        const val BRIGHTNESS_UP: Int = 0x6F
        const val BRIGHTNESS_DOWN: Int = 0x70
        const val MENU_PICK: Int = 0x9E
        const val MENU_ESCAPE: Int = 0x76
    }

    object SystemAction {
        const val POWER: Byte = 0x01.toByte()
        const val RESTART: Byte = 0x02.toByte()
        const val SLEEP: Byte = 0x03.toByte()
    }

    // ============================================================
    // BROWSER SHORTCUTS (dùng keyboard thay vì consumer)
    // Vì ESP32 không hỗ trợ consumer browser keys
    // ============================================================
    object BrowserShortKeys {
        // Alt + Home = Browser Home
        val BROWSER_HOME: Pair<Byte, Byte> = Pair(MOD_LEFT_ALT, KEY_HOME)

        // Alt + Left Arrow = Browser Back
        val BROWSER_BACK: Pair<Byte, Byte> = Pair(MOD_LEFT_ALT, KEY_LEFT_ARROW)

        // Alt + Right Arrow = Browser Forward
        val BROWSER_FORWARD: Pair<Byte, Byte> = Pair(MOD_LEFT_ALT, KEY_RIGHT_ARROW)

        // Ctrl + R = Browser Refresh
        val BROWSER_REFRESH: Pair<Byte, Byte> = Pair(MOD_LEFT_CTRL, KEY_R)

        // Ctrl + L = Browser Address Bar / Search
        val BROWSER_SEARCH: Pair<Byte, Byte> = Pair(MOD_LEFT_CTRL, KEY_L)

        // Ctrl + D = Bookmark / Favorites
        val BROWSER_FAVORITES: Pair<Byte, Byte> = Pair(MOD_LEFT_CTRL, KEY_D)
    }

    // ============================================================
    // CHAR → HID MAPPING (giữ nguyên — đã khớp hoàn hảo)
    // ============================================================
    private val charMap: Map<Char, Pair<Byte, Byte>> = buildMap {
        for (i in 0 until 26) {
            put(('a'.code + i).toChar(), Pair(MOD_NONE, (0x04 + i).toByte()))
        }
        for (i in 0 until 26) {
            put(('A'.code + i).toChar(), Pair(MOD_LEFT_SHIFT, (0x04 + i).toByte()))
        }
        put('1', Pair(MOD_NONE, KEY_1))
        put('2', Pair(MOD_NONE, KEY_2))
        put('3', Pair(MOD_NONE, KEY_3))
        put('4', Pair(MOD_NONE, KEY_4))
        put('5', Pair(MOD_NONE, KEY_5))
        put('6', Pair(MOD_NONE, KEY_6))
        put('7', Pair(MOD_NONE, KEY_7))
        put('8', Pair(MOD_NONE, KEY_8))
        put('9', Pair(MOD_NONE, KEY_9))
        put('0', Pair(MOD_NONE, KEY_0))

        put('!', Pair(MOD_LEFT_SHIFT, KEY_1))
        put('@', Pair(MOD_LEFT_SHIFT, KEY_2))
        put('#', Pair(MOD_LEFT_SHIFT, KEY_3))
        put('$', Pair(MOD_LEFT_SHIFT, KEY_4))
        put('%', Pair(MOD_LEFT_SHIFT, KEY_5))
        put('^', Pair(MOD_LEFT_SHIFT, KEY_6))
        put('&', Pair(MOD_LEFT_SHIFT, KEY_7))
        put('*', Pair(MOD_LEFT_SHIFT, KEY_8))
        put('(', Pair(MOD_LEFT_SHIFT, KEY_9))
        put(')', Pair(MOD_LEFT_SHIFT, KEY_0))

        put('-', Pair(MOD_NONE, KEY_MINUS))
        put('=', Pair(MOD_NONE, KEY_EQUAL))
        put('[', Pair(MOD_NONE, KEY_LEFT_BRACKET))
        put(']', Pair(MOD_NONE, KEY_RIGHT_BRACKET))
        put('\\', Pair(MOD_NONE, KEY_BACKSLASH))
        put(';', Pair(MOD_NONE, KEY_SEMICOLON))
        put('\'', Pair(MOD_NONE, KEY_QUOTE))
        put('`', Pair(MOD_NONE, KEY_GRAVE))
        put(',', Pair(MOD_NONE, KEY_COMMA))
        put('.', Pair(MOD_NONE, KEY_PERIOD))
        put('/', Pair(MOD_NONE, KEY_SLASH))

        put('_', Pair(MOD_LEFT_SHIFT, KEY_MINUS))
        put('+', Pair(MOD_LEFT_SHIFT, KEY_EQUAL))
        put('{', Pair(MOD_LEFT_SHIFT, KEY_LEFT_BRACKET))
        put('}', Pair(MOD_LEFT_SHIFT, KEY_RIGHT_BRACKET))
        put('|', Pair(MOD_LEFT_SHIFT, KEY_BACKSLASH))
        put(':', Pair(MOD_LEFT_SHIFT, KEY_SEMICOLON))
        put('"', Pair(MOD_LEFT_SHIFT, KEY_QUOTE))
        put('~', Pair(MOD_LEFT_SHIFT, KEY_GRAVE))
        put('<', Pair(MOD_LEFT_SHIFT, KEY_COMMA))
        put('>', Pair(MOD_LEFT_SHIFT, KEY_PERIOD))
        put('?', Pair(MOD_LEFT_SHIFT, KEY_SLASH))

        put(' ', Pair(MOD_NONE, KEY_SPACE))
        put('\t', Pair(MOD_NONE, KEY_TAB))
    }

    fun mapCharacterToHid(ch: Char): Pair<Byte, Byte>? = charMap[ch]
}