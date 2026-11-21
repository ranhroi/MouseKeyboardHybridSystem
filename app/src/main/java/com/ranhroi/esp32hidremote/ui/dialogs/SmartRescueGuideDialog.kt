package com.ranhroi.esp32hidremote.ui.dialogs

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.ranhroi.esp32hidremote.R
import com.ranhroi.esp32hidremote.ui.screens.server.ConnectionViewModel
import com.ranhroi.esp32hidremote.utils.getCurrentWifiSsid
import com.ranhroi.esp32hidremote.utils.isConnectedToEsp32Ap
import com.ranhroi.esp32hidremote.utils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SmartRescueGuideDialog(
    context: Context = LocalContext.current,
    connection: ConnectionViewModel,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var isConnectedAp by remember {
        mutableStateOf(context.isConnectedToEsp32Ap())
    }

    // ============================================================
    // POLLING: Kiểm tra lại mỗi 2 giây
    // ============================================================
    LaunchedEffect(Unit) {
        while (true) {
            context.getCurrentWifiSsid()
            val isAp = context.isConnectedToEsp32Ap()
            isConnectedAp = isAp
            delay(2000.milliseconds)
        }
    }

    // ============================================================
    // STRINGS ĐA NGÔN NGỮ
    // ============================================================
    val titleText = stringResource(R.string.rescue_dialog_title)
    val notConnectedText = stringResource(R.string.rescue_not_connected)
    val stepGuideText = stringResource(R.string.rescue_step_guide)
    val connectedText = stringResource(R.string.rescue_connected)
    val openWebHintText = stringResource(R.string.rescue_open_web_hint)
    val btnConnectWifiText = stringResource(R.string.rescue_btn_connect_wifi)
    val btnOpenWebText = stringResource(R.string.rescue_btn_open_web)
    val btnCloseText = stringResource(R.string.rescue_btn_close)

    // ============================================================
    // DIALOG
    // ============================================================
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(titleText)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!isConnectedAp) {
                    // ---- Chưa kết nối AP ----
                    Text(
                        text = notConnectedText,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp
                    )
                    Text(
                        text = stepGuideText,
                        fontSize = 13.sp
                    )
                } else {
                    // ---- Đã kết nối AP ----
                    Text(
                        text = connectedText,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = openWebHintText,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isConnectedAp) {
                        // ============================================================
                        // BƯỚC 1: Gửi lệnh switch sang SETUP + mở Wi-Fi Settings
                        // ============================================================
                        scope.launch {
                            // Gửi lệnh switch
                            val ok = connection.requestSwitchToSetup()
                            context.showToast(
                                "SmartRescue : requestSwitchToSetup result: $ok"
                            )

                            // Đợi ESP32 switch mode + bật SoftAP
                            // (BLE tắt ~200ms, delay 300ms, SoftAP bật ~1-2s)
                            delay(2000.milliseconds)

                            // Mở Wi-Fi Settings trên main thread
                            withContext(Dispatchers.Main) {
                                val intent = Intent(
                                    android.provider.Settings.ACTION_WIFI_SETTINGS
                                )
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                context.startActivity(intent)
                            }
                        }
                    } else {
                        // ============================================================
                        // BƯỚC 2: Mở web config
                        // ============================================================
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            "http://192.168.4.1".toUri()
                        )
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    }
                }
            ) {
                Text(
                    text = if (isConnectedAp) btnOpenWebText else btnConnectWifiText
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(btnCloseText)
            }
        }
    )
}