package com.ranhroi.esp32hidremote.ui.screens.main.touch

import android.view.View
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.ranhroi.esp32hidremote.ui.keyboards.LocalOsIndex
import com.ranhroi.esp32hidremote.ui.screens.server.send.InputManager
import com.ranhroi.esp32hidremote.utils.ConstantsUtils.OS_INDEX_MAC
import com.ranhroi.esp32hidremote.utils.playSoundClick
import kotlin.math.abs

/**
 * Phím chuột giữa — hỗ trợ 3 chức năng:
 *  1. Click (nhấn nhả nhanh < 150ms, không di chuyển) → middle click
 *  2. Kéo ngang → scroll ngang
 *  3. Kéo dọc  → scroll dọc
 *
 * @param osIndex              Chỉ số OS: 0=Windows, 1=Mac, 2=Linux
 * @param macScrollMultiplier  Hệ số nhân scroll trên Mac (mặc định 4)
 */
@Composable
fun TouchMiddleKey(
    view: View,
    inputManager: InputManager,
    osIndex: Int = 0,
    modifier: Modifier = Modifier,
    macScrollMultiplier: Int = 4,
) {
    val osIndex = LocalOsIndex.current
    var lastDownTime by remember { mutableLongStateOf(0L) }
    var lastPosX by remember { mutableFloatStateOf(0f) }
    var lastPosY by remember { mutableFloatStateOf(0f) }
    var downX by remember { mutableFloatStateOf(0f) }
    var downY by remember { mutableFloatStateOf(0f) }
    var viewAlpha by remember { mutableFloatStateOf(1f) }

    Surface(
        modifier = modifier
            .graphicsLayer { alpha = viewAlpha }
            .clip(RoundedCornerShape(topStart = 5.dp))
            .pointerInput(inputManager, osIndex) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue

                        when {
                            // ===== NHẤN XUỐNG =====
                            change.changedToDown() -> {
                                viewAlpha = 0.7f
                                downX = change.position.x
                                downY = change.position.y
                                lastPosX = downX
                                lastPosY = downY
                                lastDownTime = System.currentTimeMillis()
                                change.consume()
                            }

                            // ===== NHẢ LÊN =====
                            change.changedToUp() -> {
                                viewAlpha = 1f

                                val currentTime = System.currentTimeMillis()
                                val deltaX = abs(change.position.x - downX)
                                val deltaY = abs(change.position.y - downY)

                                // Tap nhanh, không di chuyển → middle click
                                if ((currentTime - lastDownTime) < 150 &&
                                    deltaX + deltaY < 2f
                                ) {
                                    view.playSoundClick()
                                    inputManager.onMiddleClick()
                                }
                                change.consume()
                            }

                            // ===== ĐANG GIỮ VÀ DI CHUYỂN =====
                            change.pressed -> {
                                val deltaX = change.position.x - lastPosX
                                val deltaY = change.position.y - lastPosY

                                fun adjust(value: Float): Int {
                                    var adjusted = (abs(value) / 3).toInt()
                                    if (osIndex == OS_INDEX_MAC) adjusted *= macScrollMultiplier
                                    return adjusted
                                }

                                if (deltaX < -3f) {
                                    inputManager.onMouseScrollHorizontal(false, adjust(deltaX))
                                } else if (deltaX > 3f) {
                                    inputManager.onMouseScrollHorizontal(true, adjust(deltaX))
                                }

                                if (deltaY < -3f) {
                                    inputManager.onMouseScrollVertical(false, adjust(deltaY))
                                } else if (deltaY > 3f) {
                                    inputManager.onMouseScrollVertical(true, adjust(deltaY))
                                }

                                if (abs(deltaX) >= 3f) lastPosX = change.position.x
                                if (abs(deltaY) >= 3f) lastPosY = change.position.y

                                change.consume()
                            }
                        }
                    }
                }
            },
        tonalElevation = 3.dp,
    ) {}
}