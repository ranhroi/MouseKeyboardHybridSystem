package com.ranhroi.esp32hidremote.ui.keyboards

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.ranhroi.esp32hidremote.ui.screens.server.send.InputManager

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VirtualKeyboard(
    view: View,
    context: Context,
    inputManager: InputManager? = null
) {
    val isHID = LocalIsHID.current

    fun triggerHaptic(strong: Boolean = false) {
        view.performHapticFeedback(
            if (strong) HapticFeedbackConstants.LONG_PRESS else HapticFeedbackConstants.KEYBOARD_TAP
        )
    }

    val editText = remember {
        object : AppCompatEditText(context) {
            override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
                outAttrs.actionLabel = null
                outAttrs.inputType = EditorInfo.TYPE_CLASS_TEXT
                outAttrs.imeOptions = EditorInfo.IME_ACTION_DONE

                return object : BaseInputConnection(this, true) {
                    private val editable = SpannableStringBuilder()
                    override fun getEditable() = editable

                    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                        text.toString().let {
                            if (isHID) {
                                inputManager?.onTextHid(it)
                            } else {
                                inputManager?.onTextInput(it)
                            }
                        }
                        return super.commitText(text, newCursorPosition)
                    }

                    override fun deleteSurroundingText(
                        beforeLength: Int,
                        afterLength: Int
                    ): Boolean {
                        if (editable.isNotEmpty()) editable.delete(
                            editable.length - 1,
                            editable.length
                        )
                        return super.deleteSurroundingText(beforeLength, afterLength)
                    }

                    override fun sendKeyEvent(event: KeyEvent?): Boolean {
                        if (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                            triggerHaptic(true)
                            inputManager?.sendEnter()
                            return true
                        }
                        if (event?.keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                            triggerHaptic(false)
                            inputManager?.sendBackspace()
                            return true
                        }
                        return super.sendKeyEvent(event)
                    }
                }
            }
        }
    }

    AndroidView(
        modifier = Modifier.size(0.dp),
        factory = {
            editText.apply {
                isFocusable = true
                isFocusableInTouchMode = true
                setBackgroundColor(Color.TRANSPARENT)
                requestFocus()
                post {
                    val imm =
                        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                }
            }
        }
    )

    LaunchedEffect(Unit) { inputManager?.updateKeyboardLanguage() }

    DisposableEffect(context) {
        // 1. Theo dõi sự kiện đổi ngôn ngữ nội bộ (bấm nút quả địa cầu trên Gboard/Laban Key)
        val subtypeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                inputManager?.updateKeyboardLanguage()
            }
        }

        // Đăng ký lắng nghe khóa cấu hình Subtype của hệ thống Android
        val subtypeUri = Settings.Secure.getUriFor(Settings.Secure.SELECTED_INPUT_METHOD_SUBTYPE)
        context.contentResolver.registerContentObserver(subtypeUri, false, subtypeObserver)

        // 2. Theo dõi sự kiện đổi sang ứng dụng bàn phím khác (Gboard <-> Laban Key)
        val imeReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_INPUT_METHOD_CHANGED) {
                    inputManager?.updateKeyboardLanguage()
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_INPUT_METHOD_CHANGED)
        ContextCompat.registerReceiver(
            context,
            imeReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        // Dọn dẹp tài nguyên khi đóng bàn phím ảo
        onDispose {
            context.contentResolver.unregisterContentObserver(subtypeObserver)
            context.unregisterReceiver(imeReceiver)

            // 3. Các lệnh dọn dẹp gốc của bạn
            inputManager?.updateKeyboardLanguage()
            editText.clearFocus()
        }
    }
}