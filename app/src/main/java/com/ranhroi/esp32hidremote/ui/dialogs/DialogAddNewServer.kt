package com.ranhroi.esp32hidremote.ui.dialogs

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.sp
import com.ranhroi.esp32hidremote.R
import com.ranhroi.esp32hidremote.models.CombineSettingModel
import com.ranhroi.esp32hidremote.models.DeviceEndpoints
import com.ranhroi.esp32hidremote.models.DeviceServerModel
import com.ranhroi.esp32hidremote.utils.ConstantsUtils
import com.ranhroi.esp32hidremote.utils.formatUuid
import com.ranhroi.esp32hidremote.utils.isValidUUID
import com.ranhroi.esp32hidremote.utils.playSoundClick
import com.ranhroi.esp32hidremote.utils.showToast

@Composable
fun DialogAddNewServer(
    context: Context,
    config: CombineSettingModel,
    port: Int,
    onAddNewServer: (DeviceServerModel) -> Unit,
    onDismiss: () -> Unit = {},
) {
    val view = LocalView.current

    // States
    var portState by remember { mutableStateOf(port.toString()) }
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // ---- LẤY CẤU HÌNH MẶC ĐỊNH TỪ CONFIG TRUYỀN VÀO ----
    var serviceUuid by remember { mutableStateOf(config.serviceUuid.ifBlank { ConstantsUtils.DEFAULT_SERVICE_UUID }) }
    var writeUuid by remember { mutableStateOf(config.writeCharUuid.ifBlank { ConstantsUtils.DEFAULT_WRITE_CHAR_UUID }) }
    var notifyUuid by remember { mutableStateOf(config.notifyCharUuid.ifBlank { ConstantsUtils.DEFAULT_NOTIFY_CHAR_UUID }) }

    var rememberPass by remember { mutableStateOf(false) }
    var isBluetooth by remember { mutableStateOf(false) }
    var isHID by remember { mutableStateOf(true) }

    // Validation States để hiển thị lỗi trực quan (Red Border)
    val isPortError = !isBluetooth && portState.toIntOrNull() == null
    val isServiceUuidError = isBluetooth && serviceUuid.isNotEmpty() && !serviceUuid.isValidUUID()
    val isWriteUuidError = isBluetooth && writeUuid.isNotEmpty() && !writeUuid.isValidUUID()
    val isNotifyUuidError = isBluetooth && notifyUuid.isNotEmpty() && !notifyUuid.isValidUUID()


    AlertDialog(
        modifier = Modifier
            .wrapContentWidth()
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(if (isBluetooth) R.drawable.baseline_bluetooth else R.drawable.baseline_wifi),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = stringResource(R.string.dialog_add_server_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        modifier = Modifier.scale(0.7f),
                        checked = isHID,
                        onCheckedChange = { isHID = it }
                    )
                    Text(
                        text = if (isHID) "ESP" else "PC",
                        fontSize = 10.sp,
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        modifier = Modifier.scale(0.7f),
                        checked = isBluetooth,
                        onCheckedChange = { isBluetooth = it }
                    )
                    Text(
                        text = if (isBluetooth) stringResource(R.string.dialog_add_server_bt) else stringResource(R.string.dialog_add_server_wifi),
                        fontSize = 10.sp,
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()), // Thêm cuộn vì form BLE sẽ dài hơn
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Chỉ hiện ô Port nếu kết nối qua Wi-Fi
                if (!isBluetooth) {
                    OutlinedTextField(
                        value = portState,
                        onValueChange = { if (it.all { c -> c.isDigit() }) portState = it },
                        label = { Text(stringResource(R.string.dialog_port_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = isPortError,
                        supportingText = { if (isPortError) Text(stringResource(R.string.dialog_port_error)) }
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.dialog_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(if (isBluetooth) stringResource(R.string.dialog_mac_label) else stringResource(R.string.dialog_address_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Hiển thị dải cấu hình 3 UUID nếu là Bluetooth
                if (isBluetooth) {
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

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.dialog_password_label)) },
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
                    Text(text = stringResource(R.string.dialog_remember_password), fontSize = 14.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Kích hoạt hiệu ứng âm thanh click đặc trưng của hệ thống
                    view.playSoundClick()

                    val portInt = portState.toIntOrNull()

                    // ---- HỆ THỐNG KIỂM TRA ĐIỀU KIỆN (VALIDATION) ----
                    when {
                        // 1. Nếu là Wi-Fi mà Port trống hoặc không đúng định dạng số
                        !isBluetooth && portInt == null -> {
                            context.showToast(context.getString(R.string.dialog_port_invalid))
                        }
                        // 2. Tên Server không được bỏ trống
                        name.isBlank() -> {
                            context.showToast(context.getString(R.string.dialog_name_empty))
                        }
                        // 3. Địa chỉ (IP/MAC) không được bỏ trống
                        address.isBlank() -> {
                            context.showToast(context.getString(R.string.dialog_address_empty))
                        }
                        // 4. Nếu là Bluetooth: Kiểm tra tính hợp lệ cấu trúc của cả 3 chuỗi UUID
                        isBluetooth && (!serviceUuid.isValidUUID() || !writeUuid.isValidUUID() || !notifyUuid.isValidUUID()) -> {
                            context.showToast(context.getString(R.string.dialog_uuid_invalid))
                        }
                        // ---- TẤT CẢ DỮ LIỆU HỢP LỆ -> TIẾN HÀNH ĐÓNG GÓI ----
                        else -> {
                            onAddNewServer(
                                DeviceServerModel(
                                    os = config.osIndex,
                                    port = if (isBluetooth) 0 else (portInt ?: 0),
                                    name = name.trim(),
                                    address = address.trim(),
                                    isHID = isHID,
                                    password = password,
                                    rememberPass = rememberPass,
                                    bluetooth = isBluetooth,
                                    endpoints = DeviceEndpoints(
                                        userName = if (isBluetooth) address.trim() else "",
                                        mainService = if (isBluetooth) serviceUuid.trim() else "",
                                        writeChannel = if (isBluetooth) writeUuid.trim() else "",
                                        readChannel = if (isBluetooth) notifyUuid.trim() else "",
                                    ),
                                    selected = false,
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                            // Đóng dialog sau khi thêm thành công
                            onDismiss()
                        }
                    }
                }
            ) {
                Text(stringResource(R.string.dialog_add_server_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                view.playSoundClick()
                onDismiss()
            }) {
                Text(stringResource(R.string.dialog_button_cancel))
            }
        }
    )
}