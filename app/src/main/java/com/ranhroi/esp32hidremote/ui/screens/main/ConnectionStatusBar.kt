package com.ranhroi.esp32hidremote.ui.screens.main

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ranhroi.esp32hidremote.R
import com.ranhroi.esp32hidremote.models.CombineSettingModel
import com.ranhroi.esp32hidremote.models.ConnectionStatus
import com.ranhroi.esp32hidremote.models.ConnectionStatusModel
import com.ranhroi.esp32hidremote.models.DeviceServerModel
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsBluetoothOn
import com.ranhroi.esp32hidremote.ui.keyboards.LocalIsWifiOn
import com.ranhroi.esp32hidremote.ui.screens.server.NetworkStatusViewModel
import com.ranhroi.esp32hidremote.ui.theme.DangerRed
import com.ranhroi.esp32hidremote.ui.theme.NeonCyan
import com.ranhroi.esp32hidremote.ui.theme.SuccessGreen
import com.ranhroi.esp32hidremote.ui.theme.SurfaceCard
import com.ranhroi.esp32hidremote.ui.theme.TextMuted
import com.ranhroi.esp32hidremote.ui.theme.TextSecondary
import com.ranhroi.esp32hidremote.ui.theme.WarningAmber
import com.ranhroi.esp32hidremote.utils.BluetoothPermissionHandler
import com.ranhroi.esp32hidremote.utils.playSoundClick

@Composable
fun ConnectionStatusBar(
    networkStatus: NetworkStatusViewModel,
    uiState: ConnectionStatusModel? = null,
    status: ConnectionStatus,
    statusText: String,
    rssi: Int?,
    @DrawableRes iconNav: Int = R.drawable.baseline_list_alt,
    onToggleScan: () -> Unit,
    onOpenDeviceList: () -> Unit,
    onOpenPassDialog: () -> Unit = {},
    onRequestEnableNetwork: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val currentServer by rememberUpdatedState(uiState?.state?.server)

    val isWifiOn = LocalIsWifiOn.current
    val isBluetoothOn = LocalIsBluetoothOn.current

    // Kiểm tra cả 2 mạng đều tắt
    val effectiveStatus = if (!isWifiOn && !isBluetoothOn) {
        ConnectionStatus.NO_NETWORK
    } else {
        status
    }

    val displayText = if (effectiveStatus == ConnectionStatus.NO_NETWORK) {
        stringResource(R.string.connection_enable_network)
    } else {
        statusText
    }

    val statusColor by animateColorAsState(
        targetValue = when (effectiveStatus) {
            ConnectionStatus.CONNECTED -> SuccessGreen
            ConnectionStatus.CONNECTING,
            ConnectionStatus.SCANNING,
            ConnectionStatus.VERIFYING -> WarningAmber

            ConnectionStatus.DISCONNECTED -> TextMuted
            ConnectionStatus.ERROR -> DangerRed
            ConnectionStatus.NEED_DEVICE_PASSWORD -> NeonCyan
            ConnectionStatus.NO_NETWORK -> DangerRed
        },
        label = "statusColor"
    )


    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // ============================================================
    // BLUETOOTH ENABLE LAUNCHER
    // ============================================================
    val enableBtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { }


    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
    }

    LaunchedEffect(Unit) {
        networkStatus.requestAction.collect { action ->
            when (action) {
                NetworkStatusViewModel.BluetoothAction.REQUEST_PERMISSION -> {
                    permissionLauncher.launch(
                        BluetoothPermissionHandler.getRequiredPermissions()
                    )
                }

                NetworkStatusViewModel.BluetoothAction.REQUEST_ENABLE -> {
                    enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                }

                else -> {}
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .testTag("connection_status_bar"),
    ) {
        Column(
            modifier = Modifier.padding(5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // ============================================================
                // TRÁI: STATUS DOT + LABEL + MESSAGE
                // ============================================================
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(32.dp)
                    ) {
                        if (status == ConnectionStatus.SCANNING ||
                            status == ConnectionStatus.CONNECTING ||
                            status == ConnectionStatus.VERIFYING
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(statusColor.copy(alpha = 0.3f))
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                                .border(2.dp, SurfaceCard, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(
                                    when (effectiveStatus) {
                                        ConnectionStatus.CONNECTED -> R.string.connection_status_label_connected
                                        ConnectionStatus.CONNECTING -> R.string.connection_status_label_connecting
                                        ConnectionStatus.SCANNING -> R.string.connection_status_label_scanning
                                        ConnectionStatus.DISCONNECTED -> R.string.connection_status_label_disconnected
                                        ConnectionStatus.ERROR -> R.string.connection_status_label_error
                                        ConnectionStatus.NEED_DEVICE_PASSWORD -> R.string.connection_status_label_need_password
                                        ConnectionStatus.VERIFYING -> R.string.connection_status_label_verifying
                                        ConnectionStatus.NO_NETWORK -> R.string.connection_status_label_no_network
                                    }
                                ),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    fontSize = 11.sp
                                ),
                                color = statusColor,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                modifier = Modifier
                                    .basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        initialDelayMillis = 2000,
                                        repeatDelayMillis = 2000,
                                        velocity = 25.dp
                                    )
                            )

                            if (rssi != null && status == ConnectionStatus.CONNECTED) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.connection_status_rssi, rssi),
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier
                                .fillMaxWidth()              // ← Thêm để có không gian chạy
                                .basicMarquee(               // ← Chạy ngang khi tràn
                                    iterations = Int.MAX_VALUE,      // Lặp vô hạn
                                    initialDelayMillis = 1500,       // Chờ 1.5s trước khi chạy
                                    repeatDelayMillis = 1500,        // Chờ 1.5s trước khi lặp lại
                                    velocity = 30.dp                 // Tốc độ chạy
                                )
                        )
                    }
                }

                // ============================================================
                // PHẢI: NETWORK ICONS + ACTION BUTTONS
                // ============================================================
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.wrapContentWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            3.dp,
                            Alignment.CenterHorizontally
                        ), verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(if (currentServer?.bluetooth == true) R.drawable.baseline_bluetooth else R.drawable.baseline_wifi),
                            contentDescription = stringResource(
                                if (currentServer?.bluetooth == true) R.string.cd_bluetooth_on
                                else R.string.cd_bluetooth_off
                            ),
                            modifier = Modifier.size(13.dp),
                            tint = Color.Gray
                        )
                        Icon(
                            painter = painterResource(if (currentServer?.isHID == true) R.drawable.outline_usb else R.drawable.baseline_computer),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color.Gray
                        )
                    }

                    // --------------------------------------------------------
                    // DEVICE LIST BUTTON
                    // --------------------------------------------------------
                    IconButton(
                        onClick = {
                            onOpenDeviceList()
                            view.playSoundClick()
                        },
                        modifier = Modifier
                            .size(35.dp)
                            .testTag("devices_button")
                    ) {
                        Icon(
                            painter = painterResource(iconNav),
                            contentDescription = stringResource(R.string.connection_status_open_device_list),
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // --------------------------------------------------------
                    // ACTION BUTTON theo status
                    // --------------------------------------------------------
                    when (effectiveStatus) {

                        ConnectionStatus.CONNECTED -> {
                            OutlinedButton(
                                onClick = {
                                    onToggleScan()
                                    view.playSoundClick()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("disconnect_button"),
                                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = stringResource(R.string.connection_status_disconnect),
                                    color = DangerRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }

                        ConnectionStatus.SCANNING -> {
                            OutlinedButton(
                                onClick = {
                                    onToggleScan()
                                    view.playSoundClick()
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(35.dp)
                                    .testTag("stop_scan_button")
                            ) {
                                Text(
                                    text = stringResource(R.string.connection_status_stop),
                                    color = WarningAmber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }

                        ConnectionStatus.NEED_DEVICE_PASSWORD -> {
                            FilledTonalButton(
                                onClick = {
                                    onOpenPassDialog()
                                    view.playSoundClick()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = NeonCyan.copy(alpha = 0.15f),
                                    contentColor = NeonCyan
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(35.dp)
                                    .testTag("open_pass_dialog_button")
                            ) {
                                Text(
                                    text = stringResource(R.string.connection_status_enter_password),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }

                        ConnectionStatus.VERIFYING -> {
                            // Không hiển thị nút — đang xử lý
                        }

                        ConnectionStatus.NO_NETWORK -> {
                            FilledTonalButton(
                                onClick = {
                                    onRequestEnableNetwork()
                                    view.playSoundClick()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = DangerRed.copy(alpha = 0.15f),
                                    contentColor = DangerRed
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(35.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.connection_status_enable_network),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        else -> {
                            FilledTonalButton(
                                onClick = {
                                    onToggleScan()
                                    view.playSoundClick()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = NeonCyan.copy(alpha = 0.15f),
                                    contentColor = NeonCyan
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(35.dp)
                                    .testTag("connect_scan_button")
                            ) {
                                Text(
                                    text = stringResource(R.string.connection_status_rescan),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}