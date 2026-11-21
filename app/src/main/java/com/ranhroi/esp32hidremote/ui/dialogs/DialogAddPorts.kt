package com.ranhroi.esp32hidremote.ui.dialogs

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranhroi.esp32hidremote.R
import com.ranhroi.esp32hidremote.utils.ConstantsUtils
import com.ranhroi.esp32hidremote.utils.playSoundClick
import com.ranhroi.esp32hidremote.utils.showToast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogAddPorts(
    context: Context,
    port: Int,
    onUpdatePort: (Int) -> Unit,
    onDismiss: () -> Unit = {},
) {
    val view = LocalView.current

    // ============================================================
    // STATE — Đồng bộ với portsResult
    // ============================================================
    var state by remember { mutableIntStateOf(port) }
    var portConnectionText by remember { mutableStateOf(port.toString()) }

    LaunchedEffect(port) {
        state = port
        portConnectionText = port.toString()
    }

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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painterResource(R.drawable.baseline_settings_input_svideo),
                    contentDescription = null,
                )
                Text(
                    text = stringResource(R.string.dialog_ports_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = portConnectionText,
                    onValueChange = {
                        portConnectionText = it
                        state = it.toIntOrNull() ?: 0
                    },
                    label = { Text(stringResource(R.string.dialog_ports_connection_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = portConnectionText.isNotBlank() && portConnectionText.toIntOrNull() == null
                )

                // ============================================================
                // NÚT RESET VỀ MẶC ĐỊNH — ĐÃ SỬA
                // ============================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            val defaultConnection = ConstantsUtils.PORT_CONNECTION
                            portConnectionText = defaultConnection.toString()
                            state = defaultConnection

                            view.playSoundClick()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.reset_to_default),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val valid = listOf(
                        portConnectionText.toIntOrNull()
                    ).all { it != null }

                    if (valid) {
                        onUpdatePort(state)
                        onDismiss()
                    } else {
                        context.showToast(context.getString(R.string.dialog_ports_invalid))
                    }
                    view.playSoundClick()
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(stringResource(R.string.dialog_ports_save))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                view.playSoundClick()
                onDismiss()
            }) {
                Text(stringResource(R.string.dialog_ports_cancel))
            }
        }
    )
}