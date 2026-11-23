package com.ranhroi.esp32hidremote.ui.screens.server.send

import android.content.Context
import com.ranhroi.esp32hidremote.data.hid.HidConstants
import com.ranhroi.esp32hidremote.data.hid.ShortKeyParser
import com.ranhroi.esp32hidremote.data.hid.VietnameseUtils
import com.ranhroi.esp32hidremote.models.CombineSettingModel
import com.ranhroi.esp32hidremote.models.KeyboardLanguage
import com.ranhroi.esp32hidremote.models.QuickKeyModel
import com.ranhroi.esp32hidremote.models.ShortKeyModel
import com.ranhroi.esp32hidremote.utils.getCurrentKeyboardLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow


class InputManager(
    private val context: Context,
    private val hidTransmitter: HidTransmitter,
    private val configProvider: () -> CombineSettingModel
) {

    private val config: CombineSettingModel
        get() = configProvider()

    // ============================================================
    // MODIFIERS STATE
    // ============================================================
    private val _latchedModifiers = MutableStateFlow<Byte>(0x00)
    val latchedModifiers: StateFlow<Byte> = _latchedModifiers.asStateFlow()

    private val _momentaryModifiers = MutableStateFlow<Byte>(0x00)
    val momentaryModifiers: StateFlow<Byte> = _momentaryModifiers.asStateFlow()

    private val _isHid = MutableStateFlow(false)
    val isHid: StateFlow<Boolean> = _isHid.asStateFlow()

    private val _keyboardLanguage = MutableStateFlow(KeyboardLanguage())
    val keyboardLanguage: StateFlow<KeyboardLanguage> = _keyboardLanguage.asStateFlow()

    @Volatile
    private var isVietnamese = false

    fun updateKeyboardLanguage() {
        val language = context.getCurrentKeyboardLanguage()
        if (language != null) {
            _keyboardLanguage.value = language
            isVietnamese = _keyboardLanguage.value.languageCode == "vi"
        }
    }

    fun setHid(hid: Boolean) {
        _isHid.value = hid
    }

    // ============================================================
    // MOUSE CONTROLS
    // ============================================================
    fun onMouseMove(dx: Int, dy: Int) {
        hidTransmitter.cursorProgress = config.progress
        hidTransmitter.sendMouseMove(dx, dy)
    }

    // ============================================================
    // SCROLL
    // ============================================================
    fun onMouseScrollVertical(direction: Boolean, steps: Int) {
        hidTransmitter.cursorScroll = config.scroll
        hidTransmitter.isScrollDirection = config.isScrollDirection

        hidTransmitter.sendMouseMiddleScroll(direction, steps)
    }

    fun onMouseScrollHorizontal(direction: Boolean, steps: Int = 0) {
        hidTransmitter.cursorScroll = config.scroll
        hidTransmitter.isScrollDirection = config.isScrollDirection

        hidTransmitter.sendMouseHScroll(direction, steps)
    }

    fun onLeftClick() {
        hidTransmitter.sendMouseClick(HidConstants.MOUSE_BUTTON_LEFT)
    }

    fun onRightClick() {
        hidTransmitter.sendMouseClick(HidConstants.MOUSE_BUTTON_RIGHT)
    }

    fun onMiddleClick() {
        hidTransmitter.sendMouseClick(HidConstants.MOUSE_BUTTON_MIDDLE)
    }

    fun onMouseButtonDown(button: Byte) {
        hidTransmitter.sendMouseButtonDown(button)
    }

    fun onMouseButtonUp(button: Byte) {
        hidTransmitter.sendMouseButtonUp(button)
    }

    // ==========================================
    // Modifiers & Keys
    // ==========================================

    fun toggleModifier(mask: Byte) {
        val current = _latchedModifiers.value
        val next = (current.toInt() xor mask.toInt()).toByte()
        _latchedModifiers.value = next
        hidTransmitter.setModifiers((next.toInt() or _momentaryModifiers.value.toInt()).toByte())
    }

    fun setMomentaryModifier(mask: Byte, isDown: Boolean) {
        val current = _momentaryModifiers.value
        val next = if (isDown) {
            (current.toInt() or mask.toInt()).toByte()
        } else {
            (current.toInt() and mask.toInt().inv()).toByte()
        }
        _momentaryModifiers.value = next
        hidTransmitter.setModifiers((_latchedModifiers.value.toInt() or next.toInt()).toByte())
    }

    fun currentActiveMods(): Byte {
        return (_latchedModifiers.value.toInt() or _momentaryModifiers.value.toInt()).toByte()
    }

    fun sendKeyDown(keycode: Byte) {
        hidTransmitter.sendKeyDown(currentActiveMods(), keycode)
    }

    fun sendKeyUp(keycode: Byte) {
        hidTransmitter.sendKeyUp(keycode)
    }

    fun sendKeyTap(keycode: Byte, extraModifiers: Byte = HidConstants.MOD_NONE) {
        val mods = (currentActiveMods().toInt() or extraModifiers.toInt()).toByte()
        hidTransmitter.sendKeyTap(mods, keycode)
    }

    /**
     * Gửi backspace.
     */
    fun sendBackspace() {
        hidTransmitter.sendKeyTap(HidConstants.MOD_NONE, HidConstants.KEY_BACKSPACE)
    }

    /**
     * Gửi Enter.
     */
    fun sendEnter() {
        hidTransmitter.sendKeyTap(HidConstants.MOD_NONE, HidConstants.KEY_ENTER)
    }


    fun sendCapsLock() {
        hidTransmitter.sendKeyTap(HidConstants.MOD_NONE, HidConstants.KEY_CAPS_LOCK)
    }

    fun sendNumLock() {
        hidTransmitter.sendKeyTap(HidConstants.MOD_NONE, HidConstants.KEY_NUM_LOCK)
    }

    fun sendScrollLock() {
        hidTransmitter.sendKeyTap(HidConstants.MOD_NONE, HidConstants.KEY_SCROLL_LOCK)
    }

    fun onSendText(deltaText: String) {
        if (_isHid.value) {
            val outputText = if (isVietnamese) {
                VietnameseUtils.autoConvert(deltaText)
            } else {
                deltaText
            }

            val taps = mutableListOf<Pair<Byte, Byte>>()
            for (ch in outputText) {
                val mapping = HidConstants.mapCharacterToHid(ch)
                if (mapping != null) {
                    taps.add(mapping)
                }
            }

            if (isVietnamese) {
                hidTransmitter.sendKeyTapsWithDelay(taps, config.delayText)
            } else {
                hidTransmitter.sendKeyTaps(taps)
            }
        } else {
            hidTransmitter.sendTextUtf8(deltaText)
        }
    }


    fun sendConsumerTap(usage: Int) = hidTransmitter.sendConsumerTap(usage)
    fun sendSystemAction(action: Byte) = hidTransmitter.sendSystemAction(action)

    /**
     * Gửi 1 shortkey.
     * Tự động dùng OS index từ config.
     */
    fun executeShortKey(shortKey: ShortKeyModel): Boolean {
        val osIndex = config.osIndex
        val combo = shortKey.keys.getOrNull(osIndex)
            ?: shortKey.keys.getOrNull(0)
            ?: return false

        if (combo.isEmpty()) return false

        // ============================================================
        // TRƯỜNG HỢP 1: SINGLE ELEMENT (chỉ 1 phím)
        // ============================================================
        if (ShortKeyParser.isSingleElement(combo)) {
            val result = ShortKeyParser.parseSingleElement(combo) ?: return false
            when (result) {
                is ShortKeyParser.ParseResult.Keyboard -> {
                    hidTransmitter.sendKeyTap(result.modifierMask, result.keycode)
                    return true
                }

                is ShortKeyParser.ParseResult.Consumer -> {
                    hidTransmitter.sendConsumerTap(result.usageCode)
                    return true
                }

                is ShortKeyParser.ParseResult.SystemAction -> {
                    hidTransmitter.sendSystemAction(result.actionCode)
                    return true
                }
            }
        }

        // ============================================================
        // TRƯỜNG HỢP 2: MULTI ELEMENT (có modifier)
        // ============================================================
        val result = ShortKeyParser.parseAuto(combo) ?: return false
        when (result) {
            is ShortKeyParser.ParseResult.Keyboard -> {
                hidTransmitter.sendKeyTap(result.modifierMask, result.keycode)
                return true
            }

            is ShortKeyParser.ParseResult.Consumer -> {
                if (result.modifierMask != 0.toByte()) {
                    hidTransmitter.setModifiers(result.modifierMask)
                }
                hidTransmitter.sendConsumerTap(result.usageCode)
                if (result.modifierMask != 0.toByte()) {
                    hidTransmitter.setModifiers(0x00)
                }
                return true
            }

            is ShortKeyParser.ParseResult.SystemAction -> {
                hidTransmitter.sendSystemAction(result.actionCode)
                return true
            }
        }
    }

    fun executeQuickKeys(quickKeys: QuickKeyModel): Boolean {
        val osIndex = config.osIndex
        val combo = quickKeys.keys.getOrNull(osIndex)
            ?: quickKeys.keys.getOrNull(0)
            ?: return false

        if (combo.isEmpty()) return false

        // ============================================================
        // TRƯỜNG HỢP 1: SINGLE ELEMENT (chỉ 1 phím)
        // ============================================================
        if (ShortKeyParser.isSingleElement(combo)) {
            val result = ShortKeyParser.parseSingleElement(combo) ?: return false
            when (result) {
                is ShortKeyParser.ParseResult.Keyboard -> {
                    hidTransmitter.sendKeyTap(result.modifierMask, result.keycode)
                    hidTransmitter.setModifiers(0x00)
                    return true
                }

                is ShortKeyParser.ParseResult.Consumer -> {
                    hidTransmitter.sendConsumerTap(result.usageCode)
                    return true
                }

                is ShortKeyParser.ParseResult.SystemAction -> {
                    hidTransmitter.sendSystemAction(result.actionCode)
                    return true
                }
            }
        }

        // ============================================================
        // TRƯỜNG HỢP 2: MULTI ELEMENT (có modifier)
        // ============================================================
        val result = ShortKeyParser.parseAuto(combo) ?: return false
        when (result) {
            is ShortKeyParser.ParseResult.Keyboard -> {
                hidTransmitter.sendKeyTap(result.modifierMask, result.keycode)
                return true
            }

            is ShortKeyParser.ParseResult.Consumer -> {
                if (result.modifierMask != 0.toByte()) {
                    hidTransmitter.setModifiers(result.modifierMask)
                }
                hidTransmitter.sendConsumerTap(result.usageCode)
                if (result.modifierMask != 0.toByte()) {
                    hidTransmitter.setModifiers(0x00)
                }
                return true
            }

            is ShortKeyParser.ParseResult.SystemAction -> {
                hidTransmitter.sendSystemAction(result.actionCode)
                return true
            }
        }
    }

    /**
     * Gửi 1 phím từ code (VD: "CTRL", "A", "F5").
     * Hỗ trợ cả modifier lẫn key thường.
     */
    fun sendKeyByCode(code: String): Boolean {
        val parsed = ShortKeyParser.parse(listOf(code)) ?: return false
        hidTransmitter.sendKeyTap(
            modifiers = (currentActiveMods().toInt() or parsed.modifierMask.toInt()).toByte(),
            keycode = parsed.keycode
        )
        return true
    }

    /**
     * Gửi tổ hợp phím: ["CTRL", "SHIFT", "S"]
     */
    fun sendKeyCombo(codes: List<String>): Boolean {
        val parsed = ShortKeyParser.parse(codes) ?: return false
        hidTransmitter.sendKeyTap(
            modifiers = (currentActiveMods().toInt() or parsed.modifierMask.toInt()).toByte(),
            keycode = parsed.keycode
        )
        return true
    }


    /** Gửi lệnh hỏi app hiện tại. */
    fun requestCurrentApp() {
        hidTransmitter.requestCurrentApp()
    }

    fun resetAllKeys() {
        _latchedModifiers.value = 0x00
        _momentaryModifiers.value = 0x00
        toggleModifier(0x00)
        hidTransmitter.sendResetAll()
    }
}