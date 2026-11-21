package com.ranhroi.esp32hidremote.ui.dialogs


import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ranhroi.esp32hidremote.R
import com.ranhroi.esp32hidremote.data.hid.HidConstants
import com.ranhroi.esp32hidremote.data.hid.ShortKeyParser
import com.ranhroi.esp32hidremote.models.KeyboardLayer
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsCapsLockOn
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsHID
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsNumLockOn
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsScrollLockOn
import com.ranhroi.esp32hidremote.ui.screens.server.send.InputManager
import com.ranhroi.esp32hidremote.ui.theme.KeyColorModifier
import com.ranhroi.esp32hidremote.ui.theme.KeyColorNormal
import com.ranhroi.esp32hidremote.ui.theme.KeyColorSpecial
import com.ranhroi.esp32hidremote.ui.theme.KeyboardBg
import com.ranhroi.esp32hidremote.ui.theme.NeonCyan
import com.ranhroi.esp32hidremote.ui.theme.SurfaceCardBorder
import com.ranhroi.esp32hidremote.ui.theme.TextColorDark
import com.ranhroi.esp32hidremote.ui.theme.TextSecondary
import com.ranhroi.esp32hidremote.utils.playSoundClick

@Composable
fun HorizontalVirtualKeyboardDialog(
    modifier: Modifier = Modifier,
    inputManager: InputManager,
    onDismiss: (() -> Unit)? = null,
) {
    val compensate = 90f // Xoay chữ 90 độ
    val view = LocalView.current
    val isCapsLockOn = LocalIsCapsLockOn.current
    val isNumLockOn = LocalIsNumLockOn.current
    val isScrollLockOn = LocalIsScrollLockOn.current

    var isInstantMode by remember { mutableStateOf(true) }
    var layer by remember { mutableStateOf(KeyboardLayer.LETTERS) }
    val selectedKeys = remember { mutableStateListOf<String>() }
    val latched by inputManager.latchedModifiers.collectAsState()

    fun clearAllKeys() {
        selectedKeys.clear()
        clearLatched(inputManager, latched)
        inputManager.resetAllKeys()
    }
    if (onDismiss == null) return

    DisposableEffect(Unit) {
        onDispose { clearAllKeys() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false, // Bắt buộc để full màn hình
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(KeyboardBg)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(
                4.dp,
                Alignment.End
            ), // Xếp các cột từ Phải sang Trái
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (layer == KeyboardLayer.LETTERS) {
                // ============================================================
                // HÀNG 5: Ctrl + Win + Alt + Space + Alt + Fn + Ctrl + ← ↓ →
                // ============================================================
                Column(
                    modifier = Modifier
                        .width(50.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Key(
                        label = "Ctrl",
                        weight = 1.3f,
                        bgColor = KeyColorModifier,
                        isModifier = true,
                        modifierMask = HidConstants.MOD_LEFT_CTRL,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        label = "Win",
                        weight = 1.3f,
                        bgColor = KeyColorModifier,
                        isModifier = true,
                        modifierMask = HidConstants.MOD_LEFT_GUI,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        label =
                            "Alt",
                        weight = 1.3f,
                        bgColor = KeyColorModifier,
                        isModifier = true,
                        modifierMask = HidConstants.MOD_LEFT_ALT,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        label = "Space",
                        weight = 6f,
                        bgColor = KeyColorNormal,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )

                    Key(
                        label = "Alt",
                        weight = 1.3f,
                        bgColor = KeyColorSpecial,
                        isModifier = true,
                        modifierMask = HidConstants.MOD_RIGHT_ALT,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        label = "Home",
                        weight = 1f,
                        bgColor = KeyColorSpecial,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        label = "Ctrl",
                        weight = 1.3f,
                        bgColor = KeyColorSpecial,
                        isModifier = true,
                        modifierMask = HidConstants.MOD_RIGHT_CTRL,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )

                    Key(
                        label = "←",
                        weight = 1f,
                        bgColor = KeyColorSpecial,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        label = "↓",
                        weight = 1f,
                        bgColor = KeyColorSpecial,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        label = "→",
                        weight = 1f,
                        bgColor = KeyColorSpecial,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                }


                // ============================================================
                // HÀNG 4: Shift + ZXCVBNM + , . / + Shift + ↑ + PgDn
                // ============================================================
                Column(
                    modifier = Modifier
                        .width(50.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Key(
                        label = "↑Shift",
                        weight = 2.3f,
                        bgColor = KeyColorModifier,
                        isModifier = true,
                        modifierMask = HidConstants.MOD_LEFT_SHIFT,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    for (c in "ZXCVBNM") Key(
                        label = if (isCapsLockOn) c.toString() else c.toString()
                            .lowercase(),
                        weight = 1f,
                        bgColor = KeyColorNormal,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        ",",
                        shift = "<",
                        weight = 1f,
                        bgColor = KeyColorNormal,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        ".",
                        shift = ">",
                        weight = 1f,
                        bgColor = KeyColorNormal,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        "/",
                        shift = "?",
                        weight = 1f,
                        bgColor = KeyColorNormal,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        label = "↑Shift",
                        weight = 1.8f,
                        bgColor = KeyColorModifier,
                        isModifier = true,
                        modifierMask = HidConstants.MOD_RIGHT_SHIFT,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        label = "↑",
                        weight = 1f,
                        bgColor = KeyColorSpecial,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        label = "PgDn",
                        weight = 1f,
                        bgColor = KeyColorSpecial,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                }

                // ============================================================
                // HÀNG 3: Caps + ASDFGHJKL + ; ' + Enter + PgUp
                // ============================================================
                Column(
                    modifier = Modifier
                        .width(50.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Key(
                        label = "Caps",
                        weight = 1.8f,
                        isAccent = isCapsLockOn,
                        bgColor = KeyColorModifier,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    for (c in "ASDFGHJKL") Key(
                        label = if (isCapsLockOn) c.toString() else c.toString()
                            .lowercase(),
                        weight = 1f,
                        bgColor = KeyColorNormal,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        label = ";",
                        shift = ":",
                        weight = 1f,
                        bgColor = KeyColorNormal,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        label = "'",
                        shift = "\"",
                        weight = 1f,
                        bgColor = KeyColorNormal,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        label = "↵Enter",
                        weight = 2.3f,
                        bgColor = KeyColorModifier,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        label = "PgUp",
                        weight = 1f,
                        bgColor = KeyColorSpecial,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                }

                // ============================================================
                // HÀNG 2: Tab + QWERTYUIOP + [ ] \ + End
                // ============================================================
                Column(
                    modifier = Modifier
                        .width(50.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Key(
                        label = "↹Tab",
                        weight = 1.5f,
                        bgColor = KeyColorModifier,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    for (c in "QWERTYUIOP") Key(
                        label = if (isCapsLockOn) c.toString() else c.toString()
                            .lowercase(),
                        weight = 1f,
                        bgColor = KeyColorNormal,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        label = "[",
                        shift = "{",
                        weight = 1f,
                        bgColor = KeyColorNormal,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        label = "]",
                        shift = "}",
                        weight = 1f,
                        bgColor = KeyColorNormal,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        label = "\\",
                        shift = "|",
                        weight = 1.5f,
                        bgColor = KeyColorSpecial,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        label = "End",
                        weight = 1f,
                        bgColor = KeyColorSpecial,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                }

                // ============================================================
                // HÀNG 1: ` + 1-0 + - = + Backspace + Ins
                // ============================================================
                Column(
                    modifier = Modifier
                        .width(50.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Key(
                        label = "`",
                        shift = "~",
                        weight = 1f,
                        bgColor = KeyColorNormal,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    for ((i, sh) in listOf(
                        "1" to "!",
                        "2" to "@",
                        "3" to "#",
                        "4" to "$",
                        "5" to "%",
                        "6" to "^",
                        "7" to "&",
                        "8" to "*",
                        "9" to "(",
                        "0" to ")"
                    )) {
                        Key(
                            label = i,
                            shift = sh,
                            weight = 1f,
                            bgColor = KeyColorNormal,
                            inputManager = inputManager,
                            isInstantMode = isInstantMode,
                            selectedKeys = selectedKeys
                        )
                    }
                    Key(
                        label = "-",
                        shift = "_",
                        weight = 1f,
                        bgColor = KeyColorNormal,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        label = "=",
                        shift = "+",
                        weight = 1f,
                        bgColor = KeyColorNormal,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        label = "←Backspace",
                        weight = 2.2f,
                        bgColor = KeyColorModifier,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        label = "Ins",
                        weight = 1f,
                        bgColor = KeyColorSpecial,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                }

                // ============================================================
                // HÀNG 0: Esc + F1-F12 + PrtSc + Pause + Del
                // ============================================================
                Column(
                    modifier = Modifier
                        .width(50.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Key(
                        "Esc",
                        weight = 1.2f,
                        bgColor = KeyColorModifier,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    for (i in 1..4) Key(
                        "F$i",
                        weight = 1f,
                        bgColor = KeyColorNormal,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Spacer(Modifier.width(4.dp)) // Khoảng trống nhỏ giữa F4 và F5
                    for (i in 5..8) Key(
                        "F$i",
                        weight = 1f,
                        bgColor = KeyColorNormal,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Spacer(Modifier.width(4.dp))
                    for (i in 9..12) Key(
                        "F$i",
                        weight = 1f,
                        bgColor = KeyColorNormal,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        label = "PrtSc",
                        weight = 1f,
                        bgColor = KeyColorSpecial,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        label = "Pause",
                        weight = 1f,
                        bgColor = KeyColorSpecial,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        label = "Del",
                        weight = 1f,
                        bgColor = KeyColorSpecial,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                }
            } else {
                // ============================================================
                // LAYER SYMBOLS
                // ============================================================
                Column(
                    modifier = Modifier
                        .width(50.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (s in listOf(
                        "!",
                        "@",
                        "#",
                        "$",
                        "%",
                        "^",
                        "&",
                        "*",
                        "(",
                        ")",
                        "-",
                        "_",
                        "=",
                        "+"
                    )) {
                        Key(
                            label = s,
                            weight = 1f,
                            bgColor = KeyColorNormal,
                            inputManager = inputManager,
                            isInstantMode = isInstantMode,
                            selectedKeys = selectedKeys
                        )
                    }
                    Key(
                        label = "←Backspace",
                        weight = 2f,
                        bgColor = KeyColorModifier,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                }
                Column(
                    modifier = Modifier
                        .width(50.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (s in listOf(
                        "`",
                        "~",
                        "[",
                        "]",
                        "{",
                        "}",
                        "\\",
                        "|",
                        ";",
                        ":",
                        "'",
                        "\"",
                        ",",
                        ".",
                        "/",
                        "?",
                        "<",
                        ">"
                    )) {
                        Key(
                            label = s,
                            weight = 1f,
                            bgColor = KeyColorNormal,
                            inputManager = inputManager,
                            isInstantMode = isInstantMode,
                            selectedKeys = selectedKeys
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .width(50.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Key(
                        label = "↑Shift",
                        weight = 2f,
                        bgColor = KeyColorModifier,
                        isModifier = true,
                        modifierMask = HidConstants.MOD_LEFT_SHIFT,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        label = "Ctrl",
                        weight = 1.5f,
                        bgColor = KeyColorModifier,
                        isModifier = true,
                        modifierMask = HidConstants.MOD_LEFT_CTRL,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    Key(
                        label = "Alt",
                        weight = 1.5f,
                        bgColor = KeyColorModifier,
                        isModifier = true,
                        modifierMask = HidConstants.MOD_LEFT_ALT,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    ActionKey(
                        label = "ABC",
                        weight = 2f,
                        bgColor = KeyColorSpecial,
                        onClick = { layer = KeyboardLayer.LETTERS })
                    Key(
                        "Space",
                        weight = 4f,
                        bgColor = KeyColorNormal,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                    ActionKey(
                        label = "?123",
                        weight = 2f,
                        bgColor = KeyColorSpecial,
                        onClick = { layer = KeyboardLayer.SYMBOLS })
                    Key(
                        label = "↵Enter",
                        weight = 2f,
                        bgColor = KeyColorModifier,
                        inputManager = inputManager,
                        isInstantMode = isInstantMode,
                        selectedKeys = selectedKeys
                    )
                }
            }

            // ============================================================
            // CỘT 8: COMBO + INSTANT + DONE (Ngoài cùng bên phải)
            // ============================================================
            Column(
                modifier = Modifier
                    .width(35.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ModeChip(
                    label = "Combo",
                    selected = !isInstantMode,
                    compensate = compensate,
                    onClick = {
                        if (isInstantMode) {
                            isInstantMode = false; clearAllKeys()
                        }
                    },
                    modifier = Modifier.weight(0.2f)
                )

                ModeChip(
                    label = "Instant",
                    selected = isInstantMode,
                    compensate = compensate,
                    onClick = {
                        if (!isInstantMode) {
                            isInstantMode = true
                            clearAllKeys()
                        }
                    },
                    modifier = Modifier.weight(0.2f)
                )

                IconButton(onClick = {
                    inputManager.sendCapsLock()
                    view.playSoundClick()
                }) {
                    Icon(
                        painter = painterResource(R.drawable.outline_keyboard_capslock_badge),
                        contentDescription = null,
                        tint = if (isCapsLockOn) Color.White else Color.Gray,
                        modifier = Modifier
                            .graphicsLayer { rotationZ = compensate }
                            .size(18.dp)

                    )
                }
                IconButton(onClick = {
                    inputManager.sendNumLock()
                    view.playSoundClick()
                }) {
                    Icon(
                        painter = painterResource(R.drawable.outline_numbers),
                        contentDescription = null,
                        tint = if (isNumLockOn) Color.White else Color.Gray,
                        modifier = Modifier
                            .graphicsLayer { rotationZ = compensate }
                            .size(18.dp)

                    )
                }
                IconButton(onClick = {
                    inputManager.sendScrollLock()
                    view.playSoundClick()
                }) {
                    Icon(
                        painter = painterResource(R.drawable.outline_scrollable_header),
                        contentDescription = null,
                        tint = if (isScrollLockOn) Color.White else Color.Gray,
                        modifier = Modifier
                            .graphicsLayer { rotationZ = compensate }
                            .size(18.dp)

                    )
                }

                IconButton(onClick = {
                    selectedKeys.clear()
                    clearLatched(inputManager, latched)
                    inputManager.resetAllKeys()
                    view.playSoundClick()
                }) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_cleaning_services),
                        contentDescription = null,
                        modifier = Modifier
                            .graphicsLayer { rotationZ = compensate }
                            .size(15.dp)

                    )
                }

                if (!isInstantMode && selectedKeys.isNotEmpty()) {
                    ComboPreview(
                        modifier = Modifier.weight(1f),
                        selectedKeys = selectedKeys,
                        compensate = compensate,
                        onRemove = { selectedKeys.remove(it) },
                        onClear = { selectedKeys.clear() },
                        onSend = {
                            val parsed = ShortKeyParser.parse(selectedKeys.toList())
                            if (parsed != null) {
                                inputManager.sendKeyTap(
                                    parsed.keycode,
                                    parsed.modifierMask
                                ); clearAllKeys()
                            }
                        }
                    )
                } else Spacer(Modifier.weight(1f))

                IconButton(
                    onClick = {
                        clearAllKeys()
                        onDismiss()
                        view.playSoundClick()
                    }) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_close),
                        contentDescription = null
                    )
                }
            }
        }
    }
}

// ============================================================
// KEY
// ============================================================
@Composable
private fun ColumnScope.Key(
    label: String,
    weight: Float,
    bgColor: Color,
    inputManager: InputManager,
    isInstantMode: Boolean,
    selectedKeys: MutableList<String>,
    shift: String? = null,
    isModifier: Boolean = false,
    modifierMask: Byte = 0,
    isAccent: Boolean = false,
) {
    val compensate = 90f // Xoay chữ 90 độ
    val view = LocalView.current
    val isHID = LocalIsHID.current
    val code = labelToCode(label)

    val latched by inputManager.latchedModifiers.collectAsState()

    // Trạng thái Shift đang bật (dùng để hiển thị ký tự thứ 2)
    val isShiftActive = (latched.toInt() and (HidConstants.MOD_LEFT_SHIFT.toInt() or HidConstants.MOD_RIGHT_SHIFT.toInt())) != 0
    val currentCode = if (isShiftActive && shift != null) shift else label
    val codeToSend = labelToCode(currentCode)

    val isSelectedInCombo = !isInstantMode && selectedKeys.contains(codeToSend)
    val isLatched = isModifier && (latched.toInt() and modifierMask.toInt()) != 0

    var isPressed by remember { mutableStateOf(false) }

    // 1. HIỆU ỨNG SCALE KHI NHẤN
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(50),
        label = "keyScale"
    )

    // 2. XỬ LÝ MÀU NỀN (BACKGROUND)
    // - Phím Modifier (Shift/Ctrl/Alt): Nhấn hoặc Latch đều đổi màu NeonCyan như cũ.
    // - Phím thường: Nhấn hoặc chọn Combo thì đổi màu. Nếu chỉ Shift đang bật thì KHÔNG đổi màu nền.
    val actualBg by animateColorAsState(
        targetValue = when {
            isPressed || isSelectedInCombo || isLatched || isAccent -> NeonCyan.copy(alpha = 0.3f)
            else -> bgColor
        },
        animationSpec = tween(100),
        label = "keyBg"
    )

    // 3. XỬ LÝ MÀU VIỀN (BORDER)
    val borderColor = when {
        isPressed || isSelectedInCombo || isLatched || isAccent -> NeonCyan
        else -> Color.Black.copy(alpha = 0.2f)
    }

    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxWidth()
            .padding(1.dp)
            .scale(scale)
            .clip(RoundedCornerShape(4.dp))
            .background(actualBg)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .pointerInput(label, isInstantMode, isHID) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        when {
                            change.changedToDown() -> {
                                isPressed = true
                                if (isInstantMode) {
                                    val resolved = resolveKeycode(code)
                                    val useTapForLock = !isHID && (
                                            resolved == HidConstants.KEY_CAPS_LOCK ||
                                                    resolved == HidConstants.KEY_NUM_LOCK ||
                                                    resolved == HidConstants.KEY_SCROLL_LOCK
                                            )
                                    if (!useTapForLock) {
                                        handlePressDown(
                                            code,
                                            isModifier,
                                            modifierMask,
                                            inputManager
                                        )
                                    }
                                }
                                change.consume()
                            }

                            change.changedToUp() -> {
                                isPressed = false
                                if (isInstantMode) {
                                    if (!isModifier) {
                                        val resolved = resolveKeycode(code)
                                        if (resolved != null) {
                                            val useTapForLock = !isHID && (
                                                    resolved == HidConstants.KEY_CAPS_LOCK ||
                                                            resolved == HidConstants.KEY_NUM_LOCK ||
                                                            resolved == HidConstants.KEY_SCROLL_LOCK
                                                    )
                                            if (useTapForLock) {
                                                inputManager.sendKeyTap(resolved)
                                            } else {
                                                inputManager.sendKeyUp(resolved)
                                            }
                                        }
                                    }
                                } else {
                                    if (selectedKeys.contains(code)) selectedKeys.remove(code)
                                    else selectedKeys.add(code)
                                }
                                view.playSoundClick()
                                change.consume()
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // 4. XỬ LÝ MÀU CHỮ (TEXT COLOR)
        val textColor = when {
            // Ưu tiên 1: Đang nhấn, đang chọn Combo, hoặc phím Modifier đang bật -> Chữ trắng
            isPressed || isSelectedInCombo || isLatched -> Color.White

            // Ưu tiên 3: Phím Accent (nếu có)
            isAccent -> NeonCyan.copy(alpha = 0.8f)

            // Mặc định: Chữ đen
            else -> TextColorDark
        }

        // Độ đậm của chữ: Nếu Shift đang bật và phím có ký tự shift, cho in đậm để rõ hơn
        val mainFontWeight =
            if (isShiftActive && shift != null) FontWeight.Bold else FontWeight.SemiBold
        val shiftFontWeight =
            if (isShiftActive && shift != null) FontWeight.Bold else FontWeight.Normal

        if (shift != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = if (isShiftActive) TextColorDark.copy(alpha = 0.4f) else textColor,
                    fontWeight = mainFontWeight,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.graphicsLayer { rotationZ = compensate }
                )
                Text(
                    text = shift,
                    fontSize = 10.sp,
                    color = if (isShiftActive) textColor else TextColorDark.copy(alpha = 0.4f),
                    fontWeight = shiftFontWeight, // Đậm hơn khi Shift bật
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.graphicsLayer { rotationZ = compensate }
                )
            }
        } else {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = mainFontWeight,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.graphicsLayer { rotationZ = compensate }
            )
        }
    }
}

// ============================================================
// MODE CHIP
// ============================================================
@Composable
private fun ModeChip(
    label: String,
    selected: Boolean? = null,
    compensate: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val localView = LocalView.current
    Box(
        modifier = modifier
            .width(25.dp)
            .height(60.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected == true) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
            .border(
                1.dp,
                if (selected == true) NeonCyan else SurfaceCardBorder,
                RoundedCornerShape(6.dp)
            )
            .clickable { onClick(); localView.playSoundClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 9.sp, // Giảm nhẹ font size
            fontWeight = if (selected == true) FontWeight.Bold else FontWeight.Normal,
            color = if (selected == true) NeonCyan else TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,          // Bắt buộc chỉ 1 dòng
            softWrap = false,      // Không cho phép tự động xuống dòng
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .graphicsLayer { rotationZ = compensate }
        )
    }
}


// ============================================================
// ACTION KEY (Nút Fn / Chuyển Layer)
// ============================================================
@Composable
private fun ColumnScope.ActionKey(
    label: String,
    weight: Float = 1f,
    bgColor: Color,
    onClick: () -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        label = "actScale"
    )

    Box(
        modifier = Modifier
            .weight(weight)
            .fillMaxWidth()
            .padding(1.dp)
            .scale(scale)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isPressed) NeonCyan.copy(alpha = 0.6f) else bgColor)
            .border(1.dp, Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPressed) Color.White else TextColorDark,
            maxLines = 1,
            softWrap = false, // Bắt buộc không xuống dòng
            modifier = Modifier.graphicsLayer { rotationZ = 90f }
        )
    }
}

// COMBO PREVIEW
// ============================================================
@Composable
private fun ComboPreview(
    selectedKeys: List<String>,
    compensate: Float,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1F1F1F), RoundedCornerShape(6.dp))
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(selectedKeys, key = { it }) { code ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(NeonCyan.copy(alpha = 0.2f))
                        .border(1.dp, NeonCyan, RoundedCornerShape(4.dp))
                        .clickable { onRemove(code); view.playSoundClick() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = code,
                        fontSize = 8.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false, // Bắt buộc không xuống dòng
                        modifier = Modifier.graphicsLayer { rotationZ = compensate }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Red.copy(alpha = 0.15f))
                .clickable(onClick = { onClear(); view.playSoundClick() }),
            contentAlignment = Alignment.Center
        ) { Text("✕", fontSize = 12.sp, color = Color.Red, fontWeight = FontWeight.Bold) }

        Box(
            modifier = Modifier
                .width(40.dp)
                .height(80.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(NeonCyan)
                .clickable(onClick = { onSend(); view.playSoundClick() })
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Send", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false, // Bắt buộc không xuống dòng
                modifier = Modifier
                    .graphicsLayer { rotationZ = compensate })
        }

    }
}




// ============================================================
// HELPERS
// ============================================================
fun handlePressDown(
    code: String,
    isModifier: Boolean,
    modifierMask: Byte,
    inputManager: InputManager,
) {
    if (isModifier) {
        inputManager.toggleModifier(modifierMask)
        return
    }
    val parsed = ShortKeyParser.parse(listOf(code))
    if (parsed != null) {
        inputManager.sendKeyDown(parsed.keycode)
        return
    }
    ShortKeyParser.parseSingleElement(listOf(code))?.let { single ->
        when (single) {
            is ShortKeyParser.ParseResult.Consumer -> inputManager.sendConsumerTap(single.usageCode)
            is ShortKeyParser.ParseResult.SystemAction -> inputManager.sendSystemAction(
                single.actionCode
            )

            is ShortKeyParser.ParseResult.Keyboard -> inputManager.sendKeyDown(single.keycode)
        }
    }
}

fun resolveKeycode(code: String): Byte? {
    ShortKeyParser.parse(listOf(code))?.let { return it.keycode }
    val single = ShortKeyParser.parseSingleElement(listOf(code))
    if (single is ShortKeyParser.ParseResult.Keyboard) return single.keycode
    return null
}

fun clearLatched(inputManager: InputManager, latched: Byte) {
    val m = latched.toInt()
    if (m and HidConstants.MOD_LEFT_CTRL.toInt() != 0)
        inputManager.toggleModifier(HidConstants.MOD_LEFT_CTRL)
    if (m and HidConstants.MOD_RIGHT_CTRL.toInt() != 0)
        inputManager.toggleModifier(HidConstants.MOD_RIGHT_CTRL)
    if (m and HidConstants.MOD_LEFT_SHIFT.toInt() != 0)
        inputManager.toggleModifier(HidConstants.MOD_LEFT_SHIFT)
    if (m and HidConstants.MOD_RIGHT_SHIFT.toInt() != 0)
        inputManager.toggleModifier(HidConstants.MOD_RIGHT_SHIFT)
    if (m and HidConstants.MOD_LEFT_ALT.toInt() != 0)
        inputManager.toggleModifier(HidConstants.MOD_LEFT_ALT)
    if (m and HidConstants.MOD_RIGHT_ALT.toInt() != 0)
        inputManager.toggleModifier(HidConstants.MOD_RIGHT_ALT)
    if (m and HidConstants.MOD_LEFT_GUI.toInt() != 0)
        inputManager.toggleModifier(HidConstants.MOD_LEFT_GUI)
    if (m and HidConstants.MOD_RIGHT_GUI.toInt() != 0)
        inputManager.toggleModifier(HidConstants.MOD_RIGHT_GUI)
}


fun labelToCode(label: String): String = when (label) {
    "←" -> "LEFT"; "↑" -> "UP"; "↓" -> "DOWN"; "→" -> "RIGHT"
    "Esc" -> "ESC"; "←Backspace" -> "BACKSPACE"; "Caps" -> "CAPSLOCK"
    "PrtSc" -> "PRINTSCREEN"; "ScrLk" -> "SCROLLLOCK"; "PgUp" -> "PAGEUP"; "PgDn" -> "PAGEDOWN"
    "Ins" -> "INSERT"; "Del" -> "DELETE"; "↵Enter" -> "ENTER"; "Space" -> "SPACE"; "↹Tab" -> "TAB"
    "Home" -> "HOME"
    "!" -> "1"; "@" -> "2"; "#" -> "3"; "$" -> "4"; "%" -> "5"
    "^" -> "6"; "&" -> "7"; "*" -> "8"; "(" -> "9"; ")" -> "0"
    "_" -> "-"; "+" -> "="; "{" -> "["; "}" -> "]"; "|" -> "\\"
    ":" -> ";"; "\"" -> "'"; "~" -> "`"; "<" -> ","; ">" -> "."
    "?" -> "/"
    else -> label
}
