package com.ranhroi.esp32hidremote.ui.dialogs

import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranhroi.esp32hidremote.R
import com.ranhroi.esp32hidremote.models.DeviceServerModel
import com.ranhroi.esp32hidremote.ui.screens.server.ConnectionViewModel
import com.ranhroi.esp32hidremote.ui.screens.server.NetworkStatusViewModel
import com.ranhroi.esp32hidremote.ui.screens.server.ServerItem
import com.ranhroi.esp32hidremote.ui.theme.NeonCyan
import com.ranhroi.esp32hidremote.ui.theme.SurfaceDark
import com.ranhroi.esp32hidremote.ui.theme.TextMuted
import com.ranhroi.esp32hidremote.ui.theme.TextPrimary
import com.ranhroi.esp32hidremote.utils.playSoundClick

@Composable
fun DevicePickerDialog(
    view: View,
    networkStatus: NetworkStatusViewModel,
    connection: ConnectionViewModel,
    devices: List<DeviceServerModel>,
    isLoading: Boolean = false,
    onSelectDevice: (DeviceServerModel) -> Unit,
    onOpenServerScreen: () -> Unit,
    onDismiss: () -> Unit
) {

    val isWifiOn by networkStatus.isWifiOn.collectAsState()
    val isBluetoothOn by networkStatus.isBluetoothOn.collectAsState()
    val getSelectedDevice by connection.getSelectedDevice.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        titleContentColor = NeonCyan,
        textContentColor = TextPrimary,

        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_list_alt),
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.device_picker_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },

        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (devices.isEmpty()) {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.device_picker_empty),
                            fontSize = 14.sp,
                            color = TextMuted
                        )
                        Text(
                            text = stringResource(R.string.device_picker_empty_hint),
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                } else {
                    // Header số lượng
                    Text(
                        text = stringResource(R.string.device_picker_count, devices.size),
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Danh sách — dùng DeviceSeverItem
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(devices, key = { it.id }) { device ->
                            ServerItem(
                                networkStatus = networkStatus,
                                isWifiOn = isWifiOn,
                                isBluetoothOn = isBluetoothOn,
                                server = device,
                                isSelected = device == getSelectedDevice,
                                onSelect = {
                                    if (!isLoading && !device.selected) {
                                        onSelectDevice(device)
                                        view.playSoundClick()
                                    }
                                },
                                onLongSelect = {
                                    // Không dùng long click trong dialog
                                }
                            )
                        }
                    }
                }
            }
        },

        confirmButton = {
            Button(
                onClick = onOpenServerScreen,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_settings),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.device_picker_open_server),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },

        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.device_picker_close),
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        }
    )
}