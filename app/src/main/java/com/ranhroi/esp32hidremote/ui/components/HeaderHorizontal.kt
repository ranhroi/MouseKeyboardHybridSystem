package com.ranhroi.esp32hidremote.ui.components

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranhroi.esp32hidremote.models.HeaderMenuModel
import com.ranhroi.esp32hidremote.utils.IconsMap
import com.ranhroi.esp32hidremote.utils.playSoundClick

/**
 * Header dạng hàng ngang với menu items tùy biến
 * 
 * Cách dùng:
 * HeaderHorizontalCustom(
 *     view = view,
 *     leftItems = listOf(
 *         HeaderMenuModel(icon = "stop", onClick = { ... }),
 *         HeaderMenuModel(text = "START", showOnlyText = true, onClick = { ... })
 *     ),
 *     rightItems = listOf(
 *         HeaderMenuModel(icon = "settings", onClick = { ... })
 *     )
 * )
 */
@Composable
fun HeaderHorizontal(
    view: View,
    height: Int = 50,
    itemSpacing: Int = 8,
    rotationAngle: Float = 0f,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    // 🔥 LEFT ITEMS (Hiển thị bên trái)
    leftItems: List<HeaderMenuModel> = emptyList(),
    // 🔥 RIGHT ITEMS (Hiển thị bên phải)
    rightItems: List<HeaderMenuModel> = emptyList(),
    // 🔥 CENTER CONTENT (Nội dung ở giữa)
    centerContent: @Composable (() -> Unit)? = null,
    // 🔥 LEFT CONTENT (Nội dung bên trái, trước leftItems)
    leftContent: @Composable (() -> Unit)? = null,
    // 🔥 RIGHT CONTENT (Nội dung bên phải, sau rightItems)
    rightContent: @Composable (() -> Unit)? = null,
    // 🔥 Extra menu items
    extraLeftItems: @Composable (() -> Unit)? = null,
    extraRightItems: @Composable (() -> Unit)? = null
) {
    val visibleLeftItems = leftItems.filter { it.isVisible() }
    val visibleRightItems = rightItems.filter { it.isVisible() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp),
        color = color,
        tonalElevation = 3.dp,
    ) {
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height.dp)
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ===== LEFT SECTION =====
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .height(height.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(itemSpacing.dp)
            ) {
                leftContent?.invoke()

                visibleLeftItems.forEach { item ->
                    HeaderMenuItemView(
                        item = item,
                        view = view,
                        rotationAngle = rotationAngle
                    )
                }
                extraLeftItems?.invoke()
            }

            // ===== CENTER SECTION =====
            centerContent?.invoke()

            // ===== RIGHT SECTION =====
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .height(height.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((6 + itemSpacing).dp)
            ) {
                visibleRightItems.forEach { item ->
                    HeaderMenuItemView(
                        item = item,
                        view = view,
                        rotationAngle = rotationAngle
                    )
                }
                extraRightItems?.invoke()
                rightContent?.invoke()
            }
        }
    }
}

// ===== ITEM VIEW =====

@Composable
private fun HeaderMenuItemView(
    item: HeaderMenuModel,
    view: View,
    rotationAngle: Float
) {
    // 🔥 Lấy icon từ IconsMap
    val icon = remember(item.icon) {
        if (!item.icon.isNullOrEmpty()) IconsMap.getIconOrDefault(item.icon) else null
    }

    // 🔥 Xác định hiển thị
    val hasIcon = icon != null
    val hasText = !item.text.isNullOrEmpty()

    // 🔥 Nếu không có icon và không có text → KHÔNG HIỂN THỊ
    if (!hasIcon && !hasText && !item.isStatusDot) return

    // 🔥 Màu sắc
    val defaultColor = if (item.isDestructive) {
        Color.Red
    } else {
        MaterialTheme.colorScheme.primary
    }
    val textColor = item.textColor?.copy(alpha = 0.6f) ?: defaultColor.copy(alpha = 0.6f)
    val iconTint = item.iconTint?.copy(alpha = 0.6f) ?: defaultColor.copy(alpha = 0.6f)

    // ============================================================
    // 1. STATUS DOT (Ưu tiên cao nhất)
    // ============================================================
    if (item.isStatusDot) {
        Box(
            modifier = item.modifier
                .size(12.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(
                    item.iconTint
                        ?: if (item.isConnected) Color(0xFF4CAF50)
                        else Color(0xFFE57373)
                )
        )
        return
    }

    // ============================================================
    // 2. CHỈ HIỂN THỊ ICON (Không có text)
    // ============================================================
    if (hasIcon && !hasText) {
        IconButton(
            enabled = item.enabled,
            onClick = {
                try {
                    item.onClick()
                    view.playSoundClick()
                } catch (_: Exception) {
                }
            },
            modifier = Modifier
                .size(33.dp)
                .padding(2.dp)
        ) {
            IconAny(
                icon = icon,
                text = item.text,
                color = iconTint,
                modifier = item.modifier
                    .size(18.dp)
                    .graphicsLayer { rotationZ = rotationAngle }
            )
        }
        return
    }

    // ============================================================
    // 3. CHỈ HIỂN THỊ TEXT (Không có icon)
    // ============================================================
    if (!hasIcon && hasText) {
        TextButton(
            modifier = item.modifier
                .graphicsLayer { rotationZ = rotationAngle },
            enabled = item.enabled,
            onClick = {
                try {
                    item.onClick()
                    view.playSoundClick()
                } catch (_: Exception) {
                }
            },
        ) {
            Text(
                text = item.text,
                fontSize = item.textFontSize.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
        return
    }

    // ============================================================
    // 4. HIỂN THỊ CẢ ICON VÀ TEXT
    // ============================================================
    if (hasIcon && hasText) {
        // ===== TRƯỜNG HỢP XOAY 90° (HEADER DỌC) =====
        if (rotationAngle == 90f) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = item.modifier
            ) {
                IconButton(
                    enabled = item.enabled,
                    onClick = {
                        try {
                            item.onClick()
                            view.playSoundClick()
                        } catch (_: Exception) {
                        }
                    },
                    modifier = Modifier
                        .size(33.dp)
                        .padding(2.dp)
                ) {
                    IconAny(
                        icon = icon,
                        text = item.text,
                        color = iconTint,
                        modifier = item.modifier
                            .size(18.dp)
                            .graphicsLayer { rotationZ = rotationAngle }
                    )
                }
                Text(
                    text = item.text,
                    fontSize = item.textFontSize.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier
                        .graphicsLayer { rotationZ = rotationAngle }
                )
            }
            return
        }

        // ===== TRƯỜNG HỢP KHÔNG XOAY (HEADER NGANG) =====
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                enabled = item.enabled,
                onClick = {
                    try {
                        item.onClick()
                        view.playSoundClick()
                    } catch (_: Exception) {
                    }
                },
                modifier = Modifier
                    .size(33.dp)
                    .padding(2.dp)
            ) {
                IconAny(
                    icon = icon,
                    text = item.text,
                    color = iconTint,
                    modifier = item.modifier
                        .size(18.dp)
                        .graphicsLayer { rotationZ = rotationAngle }
                )
            }
            Text(
                text = item.text,
                fontSize = item.textFontSize.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                maxLines = 1,
                modifier = Modifier
                    .graphicsLayer { rotationZ = rotationAngle }
            )
        }
        return
    }
}
