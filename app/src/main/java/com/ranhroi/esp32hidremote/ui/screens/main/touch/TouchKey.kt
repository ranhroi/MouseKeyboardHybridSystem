package com.ranhroi.esp32hidremote.ui.screens.main.touch

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.ranhroi.esp32hidremote.data.hid.HidConstants
import com.ranhroi.esp32hidremote.models.TouchKeyType
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsLeftHand
import com.ranhroi.esp32hidremote.ui.screens.server.send.InputManager
import com.ranhroi.esp32hidremote.utils.playSoundClick

/**
 * @param type       Loại phím logic (LEFT hoặc RIGHT)
 * @param isLeftHand Nếu true → đảo vai trò: LEFT gửi chuột phải, RIGHT gửi chuột trái
 */
@Composable
fun TouchKey(
    view: View,
    type: TouchKeyType,
    inputManager: InputManager,
    modifier: Modifier = Modifier,
) {
    val isLeftHand = LocalIsLeftHand.current

    var viewAlpha by remember { mutableFloatStateOf(1f) }

    // Đảo phím 1 lần duy nhất
    val effectiveType = remember(type, isLeftHand) {
        if (isLeftHand) type.opposite() else type
    }

    fun triggerHaptic(strong: Boolean = false) {
        view.performHapticFeedback(
            if (strong) HapticFeedbackConstants.LONG_PRESS
            else HapticFeedbackConstants.KEYBOARD_TAP
        )
    }

    Surface(
        modifier = modifier
            .graphicsLayer { alpha = viewAlpha }
            .pointerInput(effectiveType) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue

                        when {
                            change.changedToDown() -> {
                                viewAlpha = 0.7f
                                triggerHaptic(false)
                                sendDown(effectiveType, inputManager)
                                change.consume()
                            }

                            change.changedToUp() -> {
                                viewAlpha = 1f
                                triggerHaptic(false)
                                view.playSoundClick()
                                sendUp(effectiveType, inputManager)
                                change.consume()
                            }
                        }
                    }
                }
            },
        tonalElevation = 3.dp,
    ) {}
}

// ============================================================
// Helpers
// ============================================================

private fun TouchKeyType.opposite(): TouchKeyType = when (this) {
    TouchKeyType.LEFT -> TouchKeyType.RIGHT
    TouchKeyType.RIGHT -> TouchKeyType.LEFT
}

private fun sendDown(
    type: TouchKeyType, inputManager: InputManager,
) {
    val button = when (type) {
        TouchKeyType.LEFT -> HidConstants.MOUSE_BUTTON_LEFT
        TouchKeyType.RIGHT -> HidConstants.MOUSE_BUTTON_RIGHT
    }
    inputManager.onMouseButtonDown(button)
}

private fun sendUp(type: TouchKeyType, inputManager: InputManager) {
    val button = when (type) {
        TouchKeyType.LEFT -> HidConstants.MOUSE_BUTTON_LEFT
        TouchKeyType.RIGHT -> HidConstants.MOUSE_BUTTON_RIGHT
    }
    inputManager.onMouseButtonUp(button)
}
