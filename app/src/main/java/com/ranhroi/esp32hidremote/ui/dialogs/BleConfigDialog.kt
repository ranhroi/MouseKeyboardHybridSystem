package com.ranhroi.esp32hidremote.ui.dialogs

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranhroi.esp32hidremote.R
import com.ranhroi.esp32hidremote.models.CombineSettingModel
import com.ranhroi.esp32hidremote.utils.ConstantsUtils
import com.ranhroi.esp32hidremote.utils.isValidUUID

/**
 * Dialog cấu hình BLE — CHỈ 3 field:
 *  - Service UUID
 *  - Write Characteristic UUID
 *  - Device Name (tên ESP32 phát quảng bá)
 *
 * Các field khác (progress, scroll, language...) đã có màn hình Settings riêng.
 */
@Composable
fun BleConfigDialog(
    context: Context,
    currentConfig: CombineSettingModel,
    onSave: (CombineSettingModel) -> Unit,
    onDismiss: () -> Unit,
) {
    // ============================================================
    // LOCAL STATE — copy từ currentConfig
    // ============================================================
    var serviceUuid by rememberSaveable { mutableStateOf(currentConfig.serviceUuid) }
    var writeCharUuid by rememberSaveable { mutableStateOf(currentConfig.writeCharUuid) }
    var notifyCharUuid by rememberSaveable { mutableStateOf(currentConfig.notifyCharUuid) }
    var deviceName by rememberSaveable { mutableStateOf(currentConfig.defaultEsp32s3Name) }

    // Validation
    var serviceUuidError by rememberSaveable { mutableStateOf<String?>(null) }
    var writeCharUuidError by rememberSaveable { mutableStateOf<String?>(null) }
    var notifyCharUuidError by rememberSaveable { mutableStateOf<String?>(null) }
    var deviceNameError by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),

        title = {
            Text(
                text = stringResource(R.string.ble_config_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.material3.MaterialTheme.colorScheme.primary
            )
        },

        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // ============================================================
                // DEVICE NAME
                // ============================================================
                OutlinedTextField(
                    value = deviceName,
                    onValueChange = {
                        deviceName = it
                        deviceNameError = null
                    },
                    label = { Text(stringResource(R.string.device_name)) },
                    placeholder = { Text(ConstantsUtils.DEFAULT_ESP32S3_NAME) },
                    singleLine = true,
                    isError = deviceNameError != null,
                    supportingText = {
                        if (deviceNameError != null) {
                            Text(
                                text = deviceNameError!!,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                                fontSize = 11.sp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.device_name_hint),
                                fontSize = 11.sp,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // ============================================================
                // SERVICE UUID
                // ============================================================
                OutlinedTextField(
                    value = serviceUuid,
                    onValueChange = {
                        serviceUuid = it
                        serviceUuidError = null
                    },
                    label = { Text(stringResource(R.string.service_uuid)) },
                    placeholder = { Text(ConstantsUtils.DEFAULT_SERVICE_UUID) },
                    singleLine = true,
                    isError = serviceUuidError != null,
                    supportingText = {
                        if (serviceUuidError != null) {
                            Text(
                                text = serviceUuidError!!,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                                fontSize = 11.sp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.service_uuid_hint),
                                fontSize = 11.sp,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // ============================================================
                // WRITE CHAR UUID
                // ============================================================
                OutlinedTextField(
                    value = writeCharUuid,
                    onValueChange = {
                        writeCharUuid = it
                        writeCharUuidError = null
                    },
                    label = { Text(stringResource(R.string.write_char_uuid)) },
                    placeholder = { Text(ConstantsUtils.DEFAULT_WRITE_CHAR_UUID) },
                    singleLine = true,
                    isError = writeCharUuidError != null,
                    supportingText = {
                        if (writeCharUuidError != null) {
                            Text(
                                text = writeCharUuidError!!,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                                fontSize = 11.sp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.write_char_uuid_hint),
                                fontSize = 11.sp,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )



                // ============================================================
                // NOTIFY CHAR UUID
                // ============================================================
                OutlinedTextField(
                    value = notifyCharUuid,
                    onValueChange = {
                        notifyCharUuid = it
                        notifyCharUuidError = null
                    },
                    label = { Text(stringResource(R.string.notify_char_uuid)) },
                    placeholder = { Text(ConstantsUtils.DEFAULT_NOTIFY_CHAR_UUID) },
                    singleLine = true,
                    isError = notifyCharUuidError != null,
                    supportingText = {
                        if (notifyCharUuidError != null) {
                            Text(
                                text = notifyCharUuidError!!,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                                fontSize = 11.sp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.notify_char_uuid_hint),
                                fontSize = 11.sp,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )




                // ============================================================
                // NÚT RESET VỀ MẶC ĐỊNH
                // ============================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            serviceUuid = ConstantsUtils.DEFAULT_SERVICE_UUID
                            writeCharUuid = ConstantsUtils.DEFAULT_WRITE_CHAR_UUID
                            notifyCharUuid = ConstantsUtils.DEFAULT_NOTIFY_CHAR_UUID
                            deviceName = ConstantsUtils.DEFAULT_ESP32S3_NAME
                            serviceUuidError = null
                            writeCharUuidError = null
                            notifyCharUuidError = null
                            deviceNameError = null
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.reset_to_default),
                            fontSize = 12.sp,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },

        confirmButton = {
            Button(
                onClick = {
                    // ============================================================
                    // VALIDATE
                    // ============================================================
                    var hasError = false
                    if (deviceName.isBlank()) {
                        deviceNameError = context.getString(R.string.error_device_name_empty)
                        hasError = true
                    }

                    if (serviceUuid.isBlank()) {
                        serviceUuidError =  context.getString(R.string.error_uuid_empty)
                        hasError = true
                    } else if (!serviceUuid.isValidUUID()) {
                        serviceUuidError =  context.getString(R.string.error_uuid_invalid)
                        hasError = true
                    }

                    if (writeCharUuid.isBlank()) {
                        writeCharUuidError =  context.getString(R.string.error_uuid_empty)
                        hasError = true
                    } else if (!writeCharUuid.isValidUUID()) {
                        writeCharUuidError =  context.getString(R.string.error_uuid_invalid)
                        hasError = true
                    }

                    if (notifyCharUuid.isBlank()) {
                        notifyCharUuidError =  context.getString(R.string.error_uuid_empty)
                        hasError = true
                    } else if (!notifyCharUuid.isValidUUID()) {
                        notifyCharUuidError =  context.getString(R.string.error_uuid_invalid)
                        hasError = true
                    }

                    if (hasError) return@Button

                    // ============================================================
                    // SAVE — chỉ update 3 field
                    // ============================================================
                    val newConfig = currentConfig.copy(
                        serviceUuid = serviceUuid.trim(),
                        writeCharUuid = writeCharUuid.trim(),
                        notifyCharUuid = notifyCharUuid.trim(),
                        defaultEsp32s3Name = deviceName.trim()
                    )
                    onSave(newConfig)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.dialog_save),
                    fontWeight = FontWeight.SemiBold
                )
            }
        },

        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.dialog_cancel),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    )
}