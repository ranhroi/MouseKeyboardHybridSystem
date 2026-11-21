package com.ranhroi.esp32hidremote.ui.dialogs

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ranhroi.esp32hidremote.R
import com.ranhroi.esp32hidremote.models.DeviceEndpoints
import com.ranhroi.esp32hidremote.models.DeviceServerModel
import com.ranhroi.esp32hidremote.ui.screens.server.ServersControlViewModel
import com.ranhroi.esp32hidremote.utils.formatUuid
import com.ranhroi.esp32hidremote.utils.isValidUUID
import com.ranhroi.esp32hidremote.utils.playSoundClick
import com.ranhroi.esp32hidremote.utils.showToast

@Composable
fun DialogEditServer(
    context: Context,
    serverControl: ServersControlViewModel,
    server: DeviceServerModel,
    onDismiss: () -> Unit = {},
) {
    val view = LocalView.current

    // States
    var rememberPass by remember { mutableStateOf(server.rememberPass) }
    var portState by remember { mutableStateOf(server.port.toString()) }
    var name by remember { mutableStateOf(server.name) }
    var address by remember { mutableStateOf(server.address) }
    var password by remember { mutableStateOf(server.password) }
    var isHID by remember { mutableStateOf(server.isHID) }

    // ---- ĐỌC DỮ LIỆU ĐANG CÓ TỪ TRONG ENDPOINTS CỦA SERVER RA ----
    var serviceUuid by remember { mutableStateOf(server.endpoints.mainService) }
    var writeUuid by remember { mutableStateOf(server.endpoints.writeChannel) }
    var notifyUuid by remember { mutableStateOf(server.endpoints.readChannel) }

    // Logic kiểm tra nhanh để hiển thị lỗi trên từng TextField (Red Border)
    val isPortError = !server.bluetooth && portState.toIntOrNull() == null
    val isServiceUuidError =
        server.bluetooth && serviceUuid.isNotEmpty() && !serviceUuid.isValidUUID()
    val isWriteUuidError = server.bluetooth && writeUuid.isNotEmpty() && !writeUuid.isValidUUID()
    val isNotifyUuidError = server.bluetooth && notifyUuid.isNotEmpty() && !notifyUuid.isValidUUID()

    AlertDialog(
        modifier = Modifier
            .defaultMinSize()
            .padding(horizontal = 10.dp),
        shape = RoundedCornerShape(8.dp),
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.wrapContentWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(if (server.bluetooth) R.drawable.baseline_bluetooth else R.drawable.baseline_wifi),
                        contentDescription = null,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                    Text(
                        text = stringResource(R.string.dialog_edit_server_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                Row(
                    modifier = Modifier.wrapContentWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        modifier = Modifier.scale(0.7f),
                        checked = isHID,
                        onCheckedChange = { isHID = it }
                    )

                    Icon(
                        painter = painterResource(if (isHID) R.drawable.outline_usb else R.drawable.baseline_computer),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()), // Kích hoạt cuộn khi form dài ra
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Chỉ hiện ô Port nếu đây là Server Wi-Fi
                if (!server.bluetooth) {
                    OutlinedTextField(
                        value = portState,
                        onValueChange = { if (it.all { char -> char.isDigit() }) portState = it },
                        label = { Text(stringResource(R.string.dialog_edit_port_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = isPortError,
                        supportingText = { if (isPortError) Text(stringResource(R.string.dialog_edit_port_error)) }
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.dialog_edit_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = {
                        Text(
                            if (server.bluetooth) stringResource(R.string.dialog_mac_label) else stringResource(
                                R.string.dialog_edit_address_label
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // 2. Hiển thị 3 trường UUID nếu là Server Bluetooth
                if (server.bluetooth) {
                    OutlinedTextField(
                        value = serviceUuid,
                        onValueChange = { serviceUuid = it.formatUuid() },
                        label = { Text("Service UUID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = isServiceUuidError,
                        supportingText = { if (isServiceUuidError) Text(stringResource(R.string.dialog_uuid_error)) }
                    )

                    OutlinedTextField(
                        value = writeUuid,
                        onValueChange = { writeUuid = it.formatUuid() },
                        label = { Text("Write Characteristic UUID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = isWriteUuidError,
                        supportingText = { if (isWriteUuidError) Text(stringResource(R.string.dialog_uuid_error)) }
                    )

                    OutlinedTextField(
                        value = notifyUuid,
                        onValueChange = { notifyUuid = it.formatUuid() },
                        label = { Text("Notify Characteristic UUID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = isNotifyUuidError,
                        supportingText = { if (isNotifyUuidError) Text(stringResource(R.string.dialog_uuid_error)) }
                    )
                }

                if (!server.bluetooth) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.dialog_edit_password_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = rememberPass,
                            onCheckedChange = { rememberPass = it }
                        )
                        Text(text = stringResource(R.string.dialog_edit_remember_password))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    view.playSoundClick()
                    val portInt = portState.toIntOrNull()

                    // ---- HỆ THỐNG KIỂM TRA ĐIỀU KIỆN KHI CHỈNH SỬA ----
                    when {
                        !server.bluetooth && portInt == null -> {
                            context.showToast(context.getString(R.string.dialog_edit_port_invalid))
                        }

                        name.isBlank() -> {
                            context.showToast(context.getString(R.string.dialog_edit_name_empty))
                        }

                        address.isBlank() -> {
                            context.showToast(context.getString(R.string.dialog_edit_address_empty))
                        }

                        server.bluetooth && (!serviceUuid.isValidUUID() || !writeUuid.isValidUUID() || !notifyUuid.isValidUUID()) -> {
                            context.showToast(context.getString(R.string.dialog_edit_uuid_invalid))
                        }

                        // ---- HỢP LỆ -> TIẾN HÀNH SAO CHÉP VÀ CẬP NHẬT ----
                        else -> {
                            val editServer = server.copy(
                                port = if (server.bluetooth) 0 else (portInt ?: 0),
                                name = name.trim(),
                                address = address.trim(),
                                isHID = isHID,
                                password = password,
                                rememberPass = rememberPass && password.isNotEmpty(),
                                endpoints = DeviceEndpoints(
                                    mainService = if (server.bluetooth) serviceUuid.trim() else "",
                                    writeChannel = if (server.bluetooth) writeUuid.trim() else "",
                                    readChannel = if (server.bluetooth) notifyUuid.trim() else ""
                                )
                            )
                            serverControl.setServer(editServer)
                            onDismiss()
                        }
                    }
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.dialog_edit_save))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                view.playSoundClick()
                onDismiss()
            }) {
                Text(stringResource(R.string.dialog_edit_cancel))
            }
        }
    )
}