package com.ranhroi.esp32hidremote.ui.screens.server

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranhroi.esp32hidremote.R
import com.ranhroi.esp32hidremote.models.DeviceServerModel
import com.ranhroi.esp32hidremote.utils.ConstantsUtils

@Composable
fun ServerItem(
    modifier: Modifier = Modifier,
    networkStatus: NetworkStatusViewModel? = null,
    isWifiOn: Boolean? = null,
    isBluetoothOn: Boolean? = null,
    server: DeviceServerModel,
    isSelected: Boolean = false,
    showOsLabel: Boolean = true,
    showCheckIcon: Boolean = true,
    onSelect: () -> Unit = {},
    onLongSelect: () -> Unit = {},
    content: @Composable (() -> Unit) = {},
) {
    val isBle = server.bluetooth
    val isNetworkOn = if (isBle) isBluetoothOn else isWifiOn
    val schemeColor = if (isBle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    val onTextColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .combinedClickable(
                onClick = {
                    if (isNetworkOn == null || isNetworkOn) {
                        onSelect()
                    } else {
                        if (isBle) networkStatus?.checkBleAndRequest() else networkStatus?.checkWifiAndRequest()
                    }
                },
                onLongClick = onLongSelect
            ),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = if (isSelected && showCheckIcon) {
            BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ============================================================
            // ICON — Phân biệt BLE / TCP
            // ============================================================
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color = schemeColor.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                val iconRes = when {
                    isNetworkOn == null -> if (isBle) R.drawable.baseline_bluetooth else R.drawable.baseline_wifi
                    isBle -> if (isNetworkOn) R.drawable.baseline_bluetooth else R.drawable.baseline_bluetooth_disabled
                    else -> if (isNetworkOn) R.drawable.baseline_wifi else R.drawable.baseline_wifi_off
                }

                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = schemeColor
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // ============================================================
            // INFO — Tên + Badge + Địa chỉ + OS
            // ============================================================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Dòng 1: Tên + Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = server.name.ifBlank { stringResource(R.string.server_unknown_name) },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = onTextColor,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Badge BLE / WiFi
                    Box(
                        modifier = Modifier
                            .background(color = schemeColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = stringResource(if (isBle) R.string.server_badge_ble else R.string.server_badge_wifi),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = schemeColor
                            )

                            Icon(
                                painter = painterResource(if (server.isHID) R.drawable.outline_usb else R.drawable.baseline_computer),
                                contentDescription = null,
                                tint = schemeColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))

                // Dòng 2: Địa chỉ + OS
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val subTextColor = onTextColor.copy(alpha = if (isSelected) 0.7f else 0.6f)

                    Text(
                        text = if (isBle) server.address else "${server.address}: ${server.port}",
                        fontSize = 11.sp,
                        color = subTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (showOsLabel) {
                        Text(
                            text = "•",
                            fontSize = 11.sp,
                            color = onTextColor.copy(alpha = if (isSelected) 0.5f else 0.4f)
                        )

                        Text(
                            text = ConstantsUtils.getOsLabel(server.os),
                            fontSize = 11.sp,
                            color = subTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ============================================================
            // INDICATOR — Check (selected) / Arrow (không)
            // ============================================================
            val (indicatorRes, indicatorTint, indicatorSize) = if (isSelected && showCheckIcon) {
                Triple(R.drawable.baseline_check_circle, MaterialTheme.colorScheme.primary, 20.dp)
            } else {
                Triple(R.drawable.baseline_arrow_forward, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), 18.dp)
            }

            Icon(
                painter = painterResource(indicatorRes),
                contentDescription = if (isSelected && showCheckIcon) "Selected" else null,
                modifier = Modifier.size(indicatorSize),
                tint = indicatorTint
            )

            content()
        }
    }
}
