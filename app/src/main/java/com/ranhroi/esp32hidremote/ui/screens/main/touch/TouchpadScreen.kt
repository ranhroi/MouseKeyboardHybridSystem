package com.ranhroi.esp32hidremote.ui.screens.main.touch

import android.annotation.SuppressLint
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ranhroi.esp32hidremote.R
import com.ranhroi.esp32hidremote.data.hid.HidConstants
import com.ranhroi.esp32hidremote.models.HeaderMenuModel
import com.ranhroi.esp32hidremote.models.ShortKeyModel
import com.ranhroi.esp32hidremote.models.TouchKeyType
import com.ranhroi.esp32hidremote.ui.components.ConfirmDialog
import com.ranhroi.esp32hidremote.ui.components.DpadDirection
import com.ranhroi.esp32hidremote.ui.components.HeaderHorizontal
import com.ranhroi.esp32hidremote.ui.components.MatchboxDpad
import com.ranhroi.esp32hidremote.ui.components.SelectAppState
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsBluetooth
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsButtonText
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsFloating
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsHID
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsRotation
import com.ranhroi.esp32hidremote.ui.keyboards.LocalKeyboardLanguage
import com.ranhroi.esp32hidremote.ui.keyboards.LocalSpeedPressed
import com.ranhroi.esp32hidremote.ui.keyboards.VoicePreviewBadge
import com.ranhroi.esp32hidremote.ui.keyboards.rememberVoiceState
import com.ranhroi.esp32hidremote.ui.screens.server.ConnectionViewModel
import com.ranhroi.esp32hidremote.ui.screens.server.send.InputManager
import com.ranhroi.esp32hidremote.ui.weight.WeightShortKeysFloating
import com.ranhroi.esp32hidremote.utils.playSoundClick

@SuppressLint("NonObservableLocale")
@Composable
fun TouchpadScreen(
    selectState: SelectAppState,
    inputManager: InputManager,
    floatingShortKeys: List<ShortKeyModel>,
    connection: ConnectionViewModel
) {
    val width = 52
    val view = LocalView.current
    val isHID = LocalIsHID.current
    val isRotation = LocalIsRotation.current
    val isBluetooth = LocalIsBluetooth.current
    val isButtonText = LocalIsButtonText.current
    val speedPressed = LocalSpeedPressed.current
    val isFloating = LocalIsFloating.current
    val keyboardLanguage = LocalKeyboardLanguage.current

    var showResetConfirm by remember { mutableStateOf(false) }

    fun triggerHaptic(strong: Boolean = false) {
        view.performHapticFeedback(
            if (strong) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.KEYBOARD_TAP
        )
    }

    val color = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)

    val voiceState = rememberVoiceState(keyboardLanguage) { text ->
        inputManager.onSendText(text)
    }

    val rightMenuItems = listOfNotNull(
        HeaderMenuModel(
            icon = if (voiceState.isListening) "baseline_mic" else "baseline_mic_off",
            iconTint = if (voiceState.isListening) Color.Red else Color.White,
            onClick = {
                triggerHaptic(false)
                voiceState.toggle()
            }
        ),
        HeaderMenuModel(
            icon = if (isRotation) "outline_rotate_90_degrees_ccw" else "outline_rotate_90_degrees_cw",
            iconTint = if (isRotation) Color.White else Color.LightGray,
            onClick = { connection.updateRotation(!isRotation) },
            isVisible = { selectState.isControlCircle }
        ),
        HeaderMenuModel(
            icon = if (selectState.isControlCircle) "baseline_games" else "outline_gamepad",
            iconTint = if (selectState.isControlCircle) Color.White else null,
            onClick = { selectState.isControlCircle = !selectState.isControlCircle },
            modifier = Modifier.size(17.dp)
        ),
        HeaderMenuModel(
            text = keyboardLanguage.languageCode.uppercase(LocalLocale.current.platformLocale),
            modifier = Modifier.size(43.dp),
            onClick = { inputManager.updateKeyboardLanguage() },
            isVisible = { isHID }
        ),
        HeaderMenuModel(
            icon = "baseline_cleaning_services",
            onClick = { showResetConfirm = true },
            modifier = Modifier.size(17.dp),
        ),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // ===== FLOATING HOTKEYS =====
        if (isFloating) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .zIndex(1f)
                    .fillMaxHeight()
                    .padding(bottom = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                WeightShortKeysFloating(
                    view,
                    width,
                    color,
                    isRotation,
                    isButtonText,
                    isBluetooth,
                    speedPressed,
                    selectState,
                    inputManager,
                    floatingShortKeys,
                )
            }
        }

        // ===== HEADER =====
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = if (isFloating) width.dp else 0.dp)
                .zIndex(1f)
        ) {
            HeaderHorizontal(
                view = view,
                modifier = Modifier.fillMaxWidth(),
//                leftItems = leftMenuItems,
                rightItems = rightMenuItems,
                color = color,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.2f))
        ) {
            Touchpad(
                view = view,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                inputManager = inputManager,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                TouchKey(
                    view = view,
                    type = TouchKeyType.LEFT,
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topEnd = 5.dp)),
                    inputManager = inputManager,
                )
                // ===== NÚT GIỮA =====
                TouchMiddleKey(
                    view = view,
                    modifier = Modifier
                        .weight(1.8f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)),
                    inputManager = inputManager,
                )
                TouchKey(
                    view = view,
                    type = TouchKeyType.RIGHT,
                    modifier = Modifier
                        .weight(2f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(topStart = 5.dp)),
                    inputManager = inputManager,
                )
            }

        }

        // ===== UNIVERSAL CONTROL CIRCLE =====
        AnimatedVisibility(
            visible = selectState.isControlCircle,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp)
                .zIndex(2f)
        ) {
            Box {
                MatchboxDpad(
                    isRotation = isRotation,
                    onPressDown = { dir ->
                        when (dir) {
                            DpadDirection.UP -> inputManager.sendKeyDown(HidConstants.KEY_UP_ARROW)
                            DpadDirection.DOWN -> inputManager.sendKeyDown(HidConstants.KEY_DOWN_ARROW)
                            DpadDirection.LEFT -> inputManager.sendKeyDown(HidConstants.KEY_LEFT_ARROW)
                            DpadDirection.RIGHT -> inputManager.sendKeyDown(HidConstants.KEY_RIGHT_ARROW)
                            DpadDirection.ENTER -> inputManager.sendKeyDown(HidConstants.KEY_ENTER)
                        }
                    },
                    onReleaseUp = { dir ->
                        when (dir) {
                            DpadDirection.UP -> inputManager.sendKeyUp(HidConstants.KEY_UP_ARROW)
                            DpadDirection.DOWN -> inputManager.sendKeyUp(HidConstants.KEY_DOWN_ARROW)
                            DpadDirection.LEFT -> inputManager.sendKeyUp(HidConstants.KEY_LEFT_ARROW)
                            DpadDirection.RIGHT -> inputManager.sendKeyUp(HidConstants.KEY_RIGHT_ARROW)
                            DpadDirection.ENTER -> inputManager.sendKeyUp(HidConstants.KEY_ENTER)
                        }
                        view.playSoundClick()
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = voiceState.isListening,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
                .zIndex(2f)
        ) {
            VoicePreviewBadge(voiceState = voiceState)
        }

    }

    if (showResetConfirm) {
        ConfirmDialog(
            show = showResetConfirm,
            title = stringResource(R.string.reset_key_title),
            message = stringResource(R.string.reset_key_message),
            onConfirm = {
                inputManager.resetAllKeys()
                view.playSoundClick()
            },
            confirmText = stringResource(R.string.reset_key_confirm),
            dismissText = stringResource(R.string.reset_key_cancel),
            onDismiss = {
                showResetConfirm = false
            }
        )
    }
}
