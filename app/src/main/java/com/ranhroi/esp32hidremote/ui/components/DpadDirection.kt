package com.ranhroi.esp32hidremote.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranhroi.esp32hidremote.R
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsHID
import com.ranhroi.esp32hidremote.ui.keyboards.LocalSpeedPressed
import com.ranhroi.esp32hidremote.ui.theme.ButtonDark
import com.ranhroi.esp32hidremote.ui.theme.NeonCyan
import com.ranhroi.esp32hidremote.ui.theme.SurfaceCardBorder
import com.ranhroi.esp32hidremote.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * Hướng của D-pad.
 */
enum class DpadDirection { UP, DOWN, LEFT, RIGHT, ENTER }

/**
 * MatchboxDpad — hộp diêm 5 phím (4 hướng + Enter giữa).
 *
 * Chỉ hỗ trợ 2 góc xoay:
 *  - [rotated] = false → 0°   (portrait, xuôi)
 *  - [rotated] = true  → 90°  (màn hình vẫn dọc, hộp xoay ngang)
 *
 * Khi xoay 90°:
 *  - Icon mũi tên được bù trừ để giữ đúng hướng theo cảm nhận người dùng.
 *  - Label chữ luôn nằm ngang.
 *  - Enter vẫn ở giữa, không bị nghiêng.
 *
 * Callback [onPressDown] / [onReleaseUp] cho phép nối vào InputManager
 * để gửi keyDown/keyUp.
 */

@Composable
fun MatchboxDpad(
    isRotation: Boolean = false,
    modifier: Modifier = Modifier,
    onPressDown: (DpadDirection) -> Unit = {},
    onReleaseUp: (DpadDirection) -> Unit = {},
) {
    // Góc bù trừ cho icon và label khi hộp xoay 90°
    val rotation = if (isRotation) 90f else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ============================================================
        // MATCHBOX — hộp diêm
        // ============================================================
        Box(
            modifier = Modifier.graphicsLayer {
                // Chỉ xoay 2 góc: 0 hoặc 90
                rotationZ = rotation
                transformOrigin = TransformOrigin.Center
            },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(240.dp)
                    .aspectRatio(1f)
//                    .border(
//                        2.dp,
//                        NeonCyan.copy(alpha = 0.6f),
//                        RoundedCornerShape(16.dp)
//                    )
//                    .background(
//                        ButtonDark.copy(alpha = 0.5f),
//                        RoundedCornerShape(16.dp)
//                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ---------- HÀNG 1: UP ----------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    DpadKey(
                        direction = DpadDirection.UP,
                        icon = painterResource(R.drawable.baseline_arrow_upward),
                        label = "UP",
                        rotation = rotation,
                        onPressDown = onPressDown,
                        onReleaseUp = onReleaseUp
                    )
                }

                // ---------- HÀNG 2: LEFT + ENTER + RIGHT ----------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DpadKey(
                        direction = DpadDirection.LEFT,
                        icon = painterResource(R.drawable.baseline_arrow_back),
                        label = "LEFT",
                        rotation = rotation,
                        onPressDown = onPressDown,
                        onReleaseUp = onReleaseUp
                    )

                    DpadKey(
                        direction = DpadDirection.ENTER,
                        icon = painterResource(R.drawable.outline_enter),
                        label = "OK",
                        size = 72.dp,
                        isAccent = true,
                        rotation = rotation,
                        onPressDown = onPressDown,
                        onReleaseUp = onReleaseUp
                    )

                    DpadKey(
                        direction = DpadDirection.RIGHT,
                        icon = painterResource(R.drawable.baseline_arrow_forward),
                        label = "RIGHT",
                        rotation = rotation,
                        onPressDown = onPressDown,
                        onReleaseUp = onReleaseUp
                    )
                }

                // ---------- HÀNG 3: DOWN ----------
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    DpadKey(
                        direction = DpadDirection.DOWN,
                        icon = painterResource(R.drawable.baseline_arrow_downward),
                        label = "DOWN",
                        rotation = rotation,
                        onPressDown = onPressDown,
                        onReleaseUp = onReleaseUp
                    )
                }
            }
        }
    }
}

// ============================================================
// DPAD KEY — phím đơn
// ============================================================
@Composable
private fun DpadKey(
    direction: DpadDirection,
    icon: Painter,
    label: String,
    size: Dp = 56.dp,
    isAccent: Boolean = false,
    rotation: Float = 0f,
    onPressDown: (DpadDirection) -> Unit,
    onReleaseUp: (DpadDirection) -> Unit,
) {
    val isHID = LocalIsHID.current
    val speedPressed = LocalSpeedPressed.current

    var isPressed by remember { mutableStateOf(false) }
    var isRepeating by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = tween(100),
        label = "dpadScale"
    )

    val bgColor by animateColorAsState(
        targetValue = when {
            isPressed -> NeonCyan.copy(alpha = 0.45f)
            isAccent -> NeonCyan.copy(alpha = 0.15f)
            else -> ButtonDark
        },
        animationSpec = tween(120),
        label = "dpadBg"
    )

    val borderColor = when {
        isPressed -> NeonCyan
        isAccent -> NeonCyan.copy(alpha = 0.7f)
        else -> SurfaceCardBorder
    }

    // ============================================================
    // AUTO-REPEAT: CHỈ áp dụng cho app PC (isHID = false)
    // Bo mạch (isHID = true): KHÔNG repeat, giữ nguyên hành vi cũ
    // ============================================================
    LaunchedEffect(isPressed, direction, isHID) {
        // Bo mạch: không làm gì cả — hành vi cũ hoàn toàn
        if (isHID) {
            isRepeating = false
            return@LaunchedEffect
        }

        if (!isPressed) {
            isRepeating = false
            return@LaunchedEffect
        }

        // App PC: chờ initial delay
        delay(INITIAL_DELAY_MS.milliseconds)
        if (!isPressed) return@LaunchedEffect

        isRepeating = true
        while (isPressed && direction != DpadDirection.ENTER) {
            onPressDown(direction)
            onReleaseUp(direction)
            delay(speedPressed.milliseconds)
        }
        isRepeating = false
    }

    Surface(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .pointerInput(direction, isHID) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue

                        when {
                            change.changedToDown() -> {
                                isPressed = true

                                if (isHID) {
                                    // ── Bo mạch: giữ nguyên y hệt file cũ ──
                                    onPressDown(direction)
                                } else {
                                    // ── App PC: gửi tap lần đầu ──
                                    if (!isRepeating) {
                                        onPressDown(direction)
                                        onReleaseUp(direction)
                                    }
                                }
                                change.consume()
                            }

                            change.changedToUp() -> {
                                isPressed = false

                                if (isHID) {
                                    // ── Bo mạch: giữ nguyên y hệt file cũ ──
                                    onReleaseUp(direction)
                                }
                                // App PC: không cần UP (đã tap)
                                change.consume()
                            }
                        }
                    }
                }
            },
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(
            width = if (isPressed) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = when {
                        isPressed -> NeonCyan
                        isAccent -> NeonCyan
                        else -> TextPrimary
                    },
                    modifier = Modifier
                        .size(if (size > 60.dp) 28.dp else 22.dp)
                )
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isPressed -> NeonCyan
                        isAccent -> NeonCyan
                        else -> TextPrimary
                    },
                )
            }
        }
    }
}

// ============================================================
// CONSTANTS
// ============================================================
private const val INITIAL_DELAY_MS = 500L
