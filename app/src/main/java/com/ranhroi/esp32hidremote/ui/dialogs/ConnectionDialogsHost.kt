package com.ranhroi.esp32hidremote.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ranhroi.esp32hidremote.R
import com.ranhroi.esp32hidremote.models.DeviceServerModel
import com.ranhroi.esp32hidremote.ui.screens.server.ConnectionViewModel
import com.ranhroi.esp32hidremote.ui.theme.DangerRed
import com.ranhroi.esp32hidremote.ui.theme.NeonCyan
import com.ranhroi.esp32hidremote.ui.theme.TextMuted
import com.ranhroi.esp32hidremote.ui.theme.TextPrimary
import com.ranhroi.esp32hidremote.ui.theme.TextSecondary

/**
 * Host component — tự động hiển thị dialog đúng khi uiState bật cờ.
 * Đặt component này 1 lần duy nhất trong MainScreen.
 *
 * LƯU Ý: Cứu hộ Wi-Fi đã chuyển sang Web Server (firmware).
 * Host này chỉ còn xử lý auth mật khẩu thiết bị (TCP).
 */
@Composable
fun ConnectionDialogsHost(
    viewModel: ConnectionViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ============================================================
    // DIALOG DUY NHẤT: Nhập mật khẩu thiết bị (TCP auth)
    // ============================================================
    if (uiState.showPassDeviceDialog) {
        DevicePasswordDialog(
            server = uiState.state.server,
            errorMessage = uiState.errorMessage,
            onSubmit = { password, remember ->
                viewModel.submitPasswordDevice(
                    currentServer = uiState.state.server,
                    password = password,
                    rememberPass = remember
                )
            },
            onDismiss = { viewModel.dismissPasswordDialog() }
        )
    }

}


// ============================================================
// DIALOG DUY NHẤT: NHẬP MẬT KHẨU THIẾT BỊ (TCP AUTH)
// Kích hoạt khi uiState.showPassDeviceDialog = true
// ============================================================
@Composable
fun DevicePasswordDialog(
    server: DeviceServerModel,
    errorMessage: String?,
    onSubmit: (password: String, remember: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var password by rememberSaveable { mutableStateOf(server.password) }
    var remember by rememberSaveable { mutableStateOf(false) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E),
        titleContentColor = NeonCyan,
        textContentColor = TextPrimary,

        title = {
            Text(
                text = stringResource(R.string.dialog_pass_device_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },

        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.dialog_pass_device_message),
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        localError = null
                    },
                    label = { Text(stringResource(R.string.dialog_pass_device_hint)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    isError = localError != null || errorMessage != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = TextMuted,
                        focusedLabelColor = NeonCyan,
                        cursorColor = NeonCyan,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                val displayError = localError ?: errorMessage
                if (!displayError.isNullOrBlank()) {
                    Text(
                        text = displayError,
                        fontSize = 12.sp,
                        color = DangerRed
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = remember,
                        onCheckedChange = { remember = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.dialog_pass_device_remember),
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        },

        confirmButton = {
            Button(
                onClick = {
                    if (password.isBlank()) {
                        localError = "Mật khẩu không được để trống"
                        return@Button
                    }
                    onSubmit(password, remember)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    text = stringResource(R.string.dialog_pass_device_submit),
                    fontWeight = FontWeight.SemiBold
                )
            }
        },

        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.dialog_pass_device_cancel),
                    color = TextMuted
                )
            }
        }
    )
}

