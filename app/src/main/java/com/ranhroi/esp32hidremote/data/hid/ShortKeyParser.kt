package com.ranhroi.esp32hidremote.data.hid

import com.ranhroi.esp32hidremote.models.ShortKeyModel

/**
 * Phân tích ShortKeyModel → keycode HID.
 *
 *  - CHỈ dùng keycode CÓ trong HidConstants
 *  - Media keys (MUTE, VOLUME_UP...) dùng Consumer Control
 *  - KHÔNG dùng keycode không tồn tại (KEY_F13+, KEY_KP_*, KEY_NUM_LOCK, ...)
 */
object ShortKeyParser {
    // MODIFIERS — Khớp 100% với HidConstants.MOD_*
    private val modifiers: Map<String, Byte> = mapOf(
        "CTRL" to HidConstants.MOD_LEFT_CTRL,
        "CONTROL" to HidConstants.MOD_LEFT_CTRL,
        "LCTRL" to HidConstants.MOD_LEFT_CTRL,
        "RCTRL" to HidConstants.MOD_RIGHT_CTRL,

        "SHIFT" to HidConstants.MOD_LEFT_SHIFT,
        "LSHIFT" to HidConstants.MOD_LEFT_SHIFT,
        "RSHIFT" to HidConstants.MOD_RIGHT_SHIFT,

        "ALT" to HidConstants.MOD_LEFT_ALT,
        "LALT" to HidConstants.MOD_LEFT_ALT,
        "RALT" to HidConstants.MOD_RIGHT_ALT,
        "OPTION" to HidConstants.MOD_LEFT_ALT,

        "CMD" to HidConstants.MOD_LEFT_GUI,
        "COMMAND" to HidConstants.MOD_LEFT_GUI,
        "WIN" to HidConstants.MOD_LEFT_GUI,
        "SUPER" to HidConstants.MOD_LEFT_GUI,
        "META" to HidConstants.MOD_LEFT_GUI,
        "GUI" to HidConstants.MOD_LEFT_GUI
    )

    // Power / System shortcuts
    private val systemActions: Map<String, Byte> = mapOf(
        "POWER" to HidConstants.SystemAction.POWER,
        "RESTART" to HidConstants.SystemAction.RESTART,
        "SLEEP" to HidConstants.SystemAction.SLEEP
    )

    // Browser shortcuts
    val browserShortKeys: Map<String, Pair<Byte, Byte>> = mapOf(
        "BROWSER_HOME" to HidConstants.BrowserShortKeys.BROWSER_HOME,
        "BROWSER_BACK" to HidConstants.BrowserShortKeys.BROWSER_BACK,
        "BROWSER_FORWARD" to HidConstants.BrowserShortKeys.BROWSER_FORWARD,
        "BROWSER_REFRESH" to HidConstants.BrowserShortKeys.BROWSER_REFRESH,
        "BROWSER_SEARCH" to HidConstants.BrowserShortKeys.BROWSER_SEARCH,
        "BROWSER_FAVORITES" to HidConstants.BrowserShortKeys.BROWSER_FAVORITES
    )

    // Modifier keys không cần keycode — chỉ set mask
    val modifierOnlyKeys: Set<String> = modifiers.keys

    // ============================================================
    // CONSUMER KEYS — Media keys (Page 0x0C)
    // Dùng HidConstants.Consumer.* (Int usage code)
    // ============================================================
    private val consumerKeys: Map<String, Int> = mapOf(
        // Audio
        "MUTE" to HidConstants.Consumer.MUTE,
        "MEDIA_MUTE" to HidConstants.Consumer.MUTE,
        "VOLUME_UP" to HidConstants.Consumer.VOLUME_UP,
        "VOLUMEUP" to HidConstants.Consumer.VOLUME_UP,
        "MEDIA_VOLUME_UP" to HidConstants.Consumer.VOLUME_UP,
        "VOLUME_DOWN" to HidConstants.Consumer.VOLUME_DOWN,
        "VOLUMEDN" to HidConstants.Consumer.VOLUME_DOWN,
        "VOLUMEDOWN" to HidConstants.Consumer.VOLUME_DOWN,
        "MEDIA_VOLUME_DOWN" to HidConstants.Consumer.VOLUME_DOWN,

        // Media playback
        "PLAY_PAUSE" to HidConstants.Consumer.PLAY_PAUSE,
        "MEDIA_PLAY" to HidConstants.Consumer.PLAY_PAUSE,
        "MEDIA_PLAY_PAUSE" to HidConstants.Consumer.PLAY_PAUSE,
        "NEXT_TRACK" to HidConstants.Consumer.NEXT_TRACK,
        "MEDIA_NEXT" to HidConstants.Consumer.NEXT_TRACK,
        "PREV_TRACK" to HidConstants.Consumer.PREV_TRACK,
        "MEDIA_PREV" to HidConstants.Consumer.PREV_TRACK,
        "MEDIA_STOP" to HidConstants.Consumer.STOP,
        "FAST_FORWARD" to HidConstants.Consumer.FAST_FORWARD,
        "REWIND" to HidConstants.Consumer.REWIND,

        // Brightness
        "BRIGHTNESS_UP" to HidConstants.Consumer.BRIGHTNESS_UP,
        "BRIGHTNESS_DOWN" to HidConstants.Consumer.BRIGHTNESS_DOWN,

        // Menu
        "MENU_PICK" to HidConstants.Consumer.MENU_PICK,
        "MENU_ESCAPE" to HidConstants.Consumer.MENU_ESCAPE
    )

    // ============================================================
    // KEYS — CHỈ keycode CÓ trong HidConstants
    // ============================================================
    private val keys: Map<String, Byte> = buildMap {

        // A → Z
        for (i in 0 until 26) {
            put(('A' + i).toString(), (0x04 + i).toByte())
        }

        // 0 → 9
        put("1", HidConstants.KEY_1); put("2", HidConstants.KEY_2)
        put("3", HidConstants.KEY_3); put("4", HidConstants.KEY_4)
        put("5", HidConstants.KEY_5); put("6", HidConstants.KEY_6)
        put("7", HidConstants.KEY_7); put("8", HidConstants.KEY_8)
        put("9", HidConstants.KEY_9); put("0", HidConstants.KEY_0)

        // F1 → F12 (KHÔNG có F13+)
        put("F1", HidConstants.KEY_F1); put("F2", HidConstants.KEY_F2)
        put("F3", HidConstants.KEY_F3); put("F4", HidConstants.KEY_F4)
        put("F5", HidConstants.KEY_F5); put("F6", HidConstants.KEY_F6)
        put("F7", HidConstants.KEY_F7); put("F8", HidConstants.KEY_F8)
        put("F9", HidConstants.KEY_F9); put("F10", HidConstants.KEY_F10)
        put("F11", HidConstants.KEY_F11); put("F12", HidConstants.KEY_F12)

        // Special
        put("ENTER", HidConstants.KEY_ENTER)
        put("RETURN", HidConstants.KEY_ENTER)
        put("ESC", HidConstants.KEY_ESCAPE)
        put("ESCAPE", HidConstants.KEY_ESCAPE)
        put("TAB", HidConstants.KEY_TAB)
        put("SPACE", HidConstants.KEY_SPACE)
        put("BACKSPACE", HidConstants.KEY_BACKSPACE)
        put("DELETE", HidConstants.KEY_DELETE)
        put("DEL", HidConstants.KEY_DELETE)
        put("INSERT", HidConstants.KEY_INSERT)
        put("INS", HidConstants.KEY_INSERT)
        put("HOME", HidConstants.KEY_HOME)
        put("END", HidConstants.KEY_END)
        put("PAGEUP", HidConstants.KEY_PAGE_UP)
        put("PGUP", HidConstants.KEY_PAGE_UP)
        put("PAGEDOWN", HidConstants.KEY_PAGE_DOWN)
        put("PGDN", HidConstants.KEY_PAGE_DOWN)

        // Arrows
        put("UP", HidConstants.KEY_UP_ARROW)
        put("DOWN", HidConstants.KEY_DOWN_ARROW)
        put("LEFT", HidConstants.KEY_LEFT_ARROW)
        put("RIGHT", HidConstants.KEY_RIGHT_ARROW)

        // Symbol
        put("-", HidConstants.KEY_MINUS)
        put("=", HidConstants.KEY_EQUAL)
        put("[", HidConstants.KEY_LEFT_BRACKET)
        put("]", HidConstants.KEY_RIGHT_BRACKET)
        put("\\", HidConstants.KEY_BACKSLASH)
        put(";", HidConstants.KEY_SEMICOLON)
        put("'", HidConstants.KEY_QUOTE)
        put("`", HidConstants.KEY_GRAVE)
        put(",", HidConstants.KEY_COMMA)
        put(".", HidConstants.KEY_PERIOD)
        put("/", HidConstants.KEY_SLASH)

        // Special keyboard
        put("PRINTSCREEN", HidConstants.KEY_PRINT_SCREEN)
        put("PRTSC", HidConstants.KEY_PRINT_SCREEN)
        put("SCROLLLOCK", HidConstants.KEY_SCROLL_LOCK)
        put("PAUSE", HidConstants.KEY_PAUSE)
        put("CAPSLOCK", HidConstants.KEY_CAPS_LOCK)
        put("MOD_NONE", HidConstants.MOD_NONE)

        // NOTE: KHÔNG có NumPad (KEY_KP_*), KHÔNG có KEY_NUM_LOCK,
        // KHÔNG có KEY_F13+, KHÔNG có KEY_MENU/APPLICATION/POWER...
        // vì HidConstants KHÔNG định nghĩa chúng.
    }

    // ============================================================
    // SHIFT SYMBOLS — ký tự cần giữ SHIFT
    // Bản chất: Shift + phím cơ bản. Khi parse sẽ tự cộng MOD_LEFT_SHIFT.
    // ============================================================
    private val shiftSymbols: Map<String, Byte> = mapOf(
        "!" to HidConstants.KEY_1,
        "@" to HidConstants.KEY_2,
        "#" to HidConstants.KEY_3,
        "$" to HidConstants.KEY_4,
        "%" to HidConstants.KEY_5,
        "^" to HidConstants.KEY_6,
        "&" to HidConstants.KEY_7,
        "*" to HidConstants.KEY_8,
        "(" to HidConstants.KEY_9,
        ")" to HidConstants.KEY_0,
        "_" to HidConstants.KEY_MINUS,
        "+" to HidConstants.KEY_EQUAL,
        "{" to HidConstants.KEY_LEFT_BRACKET,
        "}" to HidConstants.KEY_RIGHT_BRACKET,
        "|" to HidConstants.KEY_BACKSLASH,
        ":" to HidConstants.KEY_SEMICOLON,
        "\"" to HidConstants.KEY_QUOTE,
        "~" to HidConstants.KEY_GRAVE,
        "<" to HidConstants.KEY_COMMA,
        ">" to HidConstants.KEY_PERIOD,
        "?" to HidConstants.KEY_SLASH,
    )

    // ============================================================
    // PARSED RESULT
    // ============================================================
    data class Parsed(
        val modifierMask: Byte,
        val keycode: Byte
    )

    /**
     * Kết quả khi parse ra Consumer key (media).
     */
    data class ConsumerParsed(
        val modifierMask: Byte,
        val usageCode: Int
    )

    /**
     * Kết quả tự động — phân biệt keyboard vs consumer.
     */
    sealed class ParseResult {
        data class Keyboard(val modifierMask: Byte, val keycode: Byte) : ParseResult()
        data class Consumer(val modifierMask: Byte, val usageCode: Int) : ParseResult()
        data class SystemAction(val actionCode: Byte) : ParseResult()
    }

    // ============================================================
    // PARSE
    // ============================================================
    /**
     * Parse 1 tổ hợp keyboard: ["CTRL", "V"] → Parsed(MOD_LEFT_CTRL, KEY_V)
     * Hỗ trợ cả ký tự Shift-symbol: ["CTRL", "?"] → Parsed(MOD_LEFT_CTRL | MOD_LEFT_SHIFT, KEY_SLASH)
     */
    fun parse(combo: List<String>): Parsed? {
        var mask: Byte = 0
        var keycode: Byte? = null

        for (raw in combo) {
            val name = raw.trim().uppercase()

            modifiers[name]?.let {
                mask = (mask.toInt() or it.toInt()).toByte()
            } ?: run {
                // 1. Ưu tiên shiftSymbols trước (tự cộng SHIFT)
                val shiftKc = shiftSymbols[name]
                if (shiftKc != null) {
                    mask = (mask.toInt() or HidConstants.MOD_LEFT_SHIFT.toInt()).toByte()
                    keycode = shiftKc
                    return@run
                }

                // 2. Fallback keys thường
                val kc = keys[name] ?: return null
                keycode = kc
            }
        }
        return keycode?.let { Parsed(mask, it) }
    }

    /**
     * Parse 1 media key (Consumer Control).
     * VD: ["MUTE"] → ConsumerParsed(0, Consumer.MUTE)
     * VD: ["CTRL", "VOLUME_UP"] → ConsumerParsed(MOD_LEFT_CTRL, Consumer.VOLUME_UP)
     */
    fun parseConsumer(combo: List<String>): ConsumerParsed? {
        var mask: Byte = 0
        var usageCode: Int? = null

        for (raw in combo) {
            val name = raw.trim().uppercase()

            modifiers[name]?.let {
                mask = (mask.toInt() or it.toInt()).toByte()
            } ?: run {
                val uc = consumerKeys[name] ?: return null
                usageCode = uc
            }
        }
        return usageCode?.let { ConsumerParsed(mask, it) }
    }

    /**
     * Parse tự động — thử consumer trước, fallback keyboard.
     */
    fun parseAuto(combo: List<String>): ParseResult? {
        // Thử consumer trước (vì media key thường đơn)
        parseConsumer(combo)?.let {
            return ParseResult.Consumer(it.modifierMask, it.usageCode)
        }
        // Fallback keyboard
        parse(combo)?.let {
            return ParseResult.Keyboard(it.modifierMask, it.keycode)
        }
        return null
    }

    /**
     * Lấy tổ hợp keyboard cho OS cụ thể.
     * @param osIndex 0 = Windows, 1 = Mac, 2 = Linux
     */
    fun parseForOs(item: ShortKeyModel, osIndex: Int): Parsed? {
        val combo = item.keys.getOrNull(osIndex)
            ?: item.keys.getOrNull(0)
            ?: return null
        if (combo.isEmpty()) return null
        return parse(combo)
    }

    /**
     * Lấy tổ hợp cho OS — hỗ trợ cả keyboard và consumer.
     */
    fun parseForOsAuto(item: ShortKeyModel, osIndex: Int): ParseResult? {
        val combo = item.keys.getOrNull(osIndex)
            ?: item.keys.getOrNull(0)
            ?: return null
        if (combo.isEmpty()) return null
        return parseAuto(combo)
    }

    // ============================================================
    // VALIDATION
    // ============================================================

    /**
     * Kiểm tra combo có phải là "media key" không.
     * Media keys cần Consumer Control — không gửi qua keyboard HID.
     */
    fun isMediaKey(combo: List<String>): Boolean {
        if (combo.isEmpty()) return false
        // Bỏ modifier ra, còn lại là key chính
        val nonModifiers = combo.filter { it.trim().uppercase() !in modifiers.keys }
        if (nonModifiers.isEmpty()) return false
        return nonModifiers.any { it.trim().uppercase() in consumerKeys.keys }
    }

    /**
     * Kiểm tra combo có hợp lệ không (parse được keyboard hoặc consumer).
     */
    fun isValid(combo: List<String>): Boolean {
        return parse(combo) != null || parseConsumer(combo) != null
    }

    /**
     * Kiểm tra combo có hợp lệ cho keyboard HID không.
     */
    fun isValidKeyboard(combo: List<String>): Boolean = parse(combo) != null

    /**
     * Kiểm tra combo có hợp lệ cho consumer control không.
     */
    fun isValidConsumer(combo: List<String>): Boolean = parseConsumer(combo) != null

    // ============================================================
    // SINGLE ELEMENT PARSING
    // Xử lý các phím tắt có MỘT phần tử (chỉ 1 key, không modifier)
    // VD: ["F5"], ["ENTER"], ["VOLUME_UP"], ["PRINTSCREEN"]
    // ============================================================

    /**
     * Kiểm tra combo có phải SINGLE ELEMENT không.
     * VD: ["F5"] → true, ["CTRL", "V"] → false
     */
    fun isSingleElement(combo: List<String>): Boolean {
        return combo.size == 1 && combo[0].isNotBlank()
    }

    /**
     * Parse combo SINGLE ELEMENT.
     *
     * Trả về ParseResult (Keyboard hoặc Consumer) hoặc null.
     * Chỉ xử lý khi combo có ĐÚNG 1 phần tử.
     */
    fun parseSingleElement(combo: List<String>): ParseResult? {
        if (!isSingleElement(combo)) return null
        val name = combo[0].trim().uppercase()

        systemActions[name]?.let { action ->
            return ParseResult.SystemAction(action)
        }

        // 1. Browser shortkey (6 phím)
        browserShortKeys[name]?.let { (mod, key) ->
            return ParseResult.Keyboard(mod, key)
        }

        // 2. Consumer key (16 phím media)
        consumerKeys[name]?.let { usage ->
            return ParseResult.Consumer(0x00, usage)
        }

        // 3. Shift-symbol (tự cộng SHIFT) — VD: "?" → Shift + /
        shiftSymbols[name]?.let { kc ->
            return ParseResult.Keyboard(HidConstants.MOD_LEFT_SHIFT, kc)
        }

        // 4. Keyboard key (A-Z, F1-F12, ENTER...)
        keys[name]?.let { kc ->
            return ParseResult.Keyboard(0x00, kc)
        }

        return null
    }
    /**
     * Parse combo SINGLE ELEMENT từ ShortKeyModel.
     * Tự chọn theo osIndex.
     */
    fun parseSingleElementForOs(item: ShortKeyModel, osIndex: Int): ParseResult? {
        val combo = item.keys.getOrNull(osIndex)
            ?: item.keys.getOrNull(0)
            ?: return null
        return parseSingleElement(combo)
    }

    /**
     * Kiểm tra combo có phải single element VÀ có thể gửi được không.
     */
    fun isValidSingleElement(combo: List<String>): Boolean {
        return parseSingleElement(combo) != null
    }
}