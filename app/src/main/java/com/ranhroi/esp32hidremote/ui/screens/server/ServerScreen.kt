package com.ranhroi.esp32hidremote.ui.screens.server

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.ranhroi.esp32hidremote.R
import com.ranhroi.esp32hidremote.models.BleDeviceModel
import com.ranhroi.esp32hidremote.models.ConnectionStatus
import com.ranhroi.esp32hidremote.models.DeviceServerModel
import com.ranhroi.esp32hidremote.models.DropMenuModel
import com.ranhroi.esp32hidremote.ui.components.ConfirmDialog
import com.ranhroi.esp32hidremote.ui.components.DotsLoadingIndicator
import com.ranhroi.esp32hidremote.ui.components.MoreVertDropdown
import com.ranhroi.esp32hidremote.ui.dialogs.BleConfigDialog
import com.ranhroi.esp32hidremote.ui.dialogs.ConnectionDialogsHost
import com.ranhroi.esp32hidremote.ui.dialogs.DialogAddNewServer
import com.ranhroi.esp32hidremote.ui.dialogs.DialogAddPorts
import com.ranhroi.esp32hidremote.ui.dialogs.DialogEditServer
import com.ranhroi.esp32hidremote.ui.dialogs.SmartRescueGuideDialog
import com.ranhroi.esp32hidremote.ui.screens.main.ConnectionStatusBar
import com.ranhroi.esp32hidremote.utils.BluetoothPermissionHandler
import com.ranhroi.esp32hidremote.utils.playSoundClick
import com.ranhroi.esp32hidremote.utils.toEntity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

@SuppressLint("UnrememberedGetBackStackEntry", "MissingPermission", "AutoboxingStateCreation")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerScreen(
    navController: NavHostController,
    connection: ConnectionViewModel = hiltViewModel(),
    serversControl: ServersControlViewModel = hiltViewModel(),
    networkStatus: NetworkStatusViewModel = hiltViewModel(),
    scanner: ScannerViewModel = hiltViewModel(),
) {
    val view = LocalView.current
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    // ============================================================
    // STATE — Network
    // ============================================================
    val isWifiOn by networkStatus.isWifiOn.collectAsState()
    val isBluetoothOn by networkStatus.isBluetoothOn.collectAsState()

    // ============================================================
    // STATE — Scanner
    // ============================================================
    val isLoadingBlt by scanner.isLoadingBlt.collectAsState()
    val isLoadingTcp by scanner.isLoadingTcp.collectAsState()

    // ============================================================
    // STATE — Servers
    // ============================================================
    val foundServers by serversControl.foundServers.collectAsState()
    val savedServers by serversControl.savedServers.collectAsState()
    val currentSelectedServer by serversControl.currentSelectedServer.collectAsState()
    val portsResult by serversControl.portConnection.collectAsState()

    val getSelectedDevice by connection.getSelectedDevice.collectAsState()
    val connectionStatus by connection.connectionStatus.collectAsStateWithLifecycle()
    val uiState by connection.uiState.collectAsStateWithLifecycle()

    val config by scanner.config.collectAsStateWithLifecycle()

    // ============================================================
    // LOCAL UI STATE
    // ============================================================
    var isVisibleIp by remember { mutableStateOf(false) }
    var isVisiblePorts by remember { mutableStateOf(false) }
    var isVisibleEditPort by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRescueConfirm by remember { mutableStateOf(false) }
    var selectedServer by remember { mutableStateOf<DeviceServerModel?>(null) }
    var showBleConfigDialog by remember { mutableStateOf(false) }
    var showOptionsSettings by remember { mutableStateOf(false) }
    var showOptionsMode by remember { mutableStateOf(false) }
    var isDeleteAllServers by remember { mutableStateOf(false) }

    // ============================================================
    // ĐIỀU KIỆN HIỂN THỊ "SERVER CỨU HỘ"
    // - Bluetooth ON
    // - Wi-Fi ON
    // - Không có server Wi-Fi nào trong saved
    // ============================================================
    val showRescueServer = isBluetoothOn
            && savedServers.none { !it.bluetooth }
            && !isLoadingBlt                        // ← Chưa quét BLE xong
            && !isLoadingTcp                        // ← Chưa quét Wi-Fi xong
            && foundServers.isEmpty()               // ← Chưa có device nào


    ConnectionDialogsHost(viewModel = connection)

    BackHandler(enabled = true) { scope.launch { navController.popBackStack() } }

    // ============================================================
    // COLLECT SCAN RESULTS — MERGE BLE + TCP
    // ============================================================
    LaunchedEffect(Unit) {
        // Collect BLE results
        launch {
            scanner.devicesBlt.collectLatest { bleDevices: List<BleDeviceModel> ->
                val currentTcp = scanner.devicesTcp.value

                val filteredBle = bleDevices
                    .filterNot { device ->
                        savedServers.any { saved -> saved.address == device.address }
                    }
                    .toEntity(
                        userName = config.defaultEsp32s3Name,
                        userServiceUuid = config.serviceUuid,
                        userWriteUuid = config.writeCharUuid,
                        userNotifyUuid = config.notifyCharUuid,
                    )
                    .map { it.copy(os = config.osIndex) }

                val filteredTcp = currentTcp
                    .filterNot { device ->
                        savedServers.any { saved ->
                            saved.address == device.address && saved.port == device.port
                        }
                    }
                    .map { it.copy(os = config.osIndex) }

                val merged = filteredBle + filteredTcp

                if (merged.isEmpty()) {
                    serversControl.clearFoundServers()
                } else {
                    serversControl.setFoundServers(merged)
                }
            }
        }

        // Collect TCP results
        launch {
            scanner.devicesTcp.collectLatest { tcpDevices ->
                val currentBle = scanner.devicesBlt.value

                val filteredTcp = tcpDevices
                    .filterNot { device ->
                        savedServers.any { saved ->
                            saved.address == device.address && saved.port == device.port
                        }
                    }
                    .map { it.copy(os = config.osIndex) }

                val filteredBle = currentBle
                    .filterNot { device ->
                        savedServers.any { saved -> saved.address == device.address }
                    }
                    .toEntity(
                        userName = config.defaultEsp32s3Name,
                        userServiceUuid = config.serviceUuid,
                        userWriteUuid = config.writeCharUuid,
                        userNotifyUuid = config.notifyCharUuid,
                    )
                    .map { it.copy(os = config.osIndex) }

                val merged = filteredBle + filteredTcp

                if (merged.isEmpty()) {
                    serversControl.clearFoundServers()
                } else {
                    serversControl.setFoundServers(merged)
                }
            }
        }
    }

    // ============================================================
    // STOP SCAN ON DISPOSE
    // ============================================================
    DisposableEffect(Unit) {
        onDispose {
            scanner.stopScanTcp()
            scanner.stopScanBlt()
            serversControl.clearFoundServers()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.server_title)) },
                actions = {
                    ConnectionStatusBar(
                        networkStatus = networkStatus,
                        uiState = uiState,
                        status = connectionStatus,
                        statusText = uiState.statusMessage.ifBlank { stringResource(R.string.connection_not_connected) },
                        rssi = null,
                        iconNav = R.drawable.baseline_home,
                        onToggleScan = {
                            when (connectionStatus) {
                                ConnectionStatus.CONNECTED -> connection.disconnect()
                                else -> {
                                    if (!uiState.state.isNeedDevicePassword) connection.pingConnection()
                                }
                            }
                        },
                        onOpenDeviceList = {
                            if (navController.previousBackStackEntry != null) {
                                navController.popBackStack()
                            }
                        },
                        onRequestEnableNetwork = {
                            networkStatus.checkWifiAndRequest()
                        }
                    )
                }
            )
        }) { paddingValues ->
        Box(
            modifier = Modifier.padding(paddingValues)
        ) {

            // ============================================================
            // UI
            // ============================================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                // ============================================================
                // SCROLLABLE CONTENT
                // ============================================================
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ===== SAVED SERVERS =====
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    ) {
                        SectionHeader(
                            title = stringResource(R.string.server_saved_title, savedServers.size),
                            savedServers = savedServers,
                            onClick = {
                                isDeleteAllServers = !isDeleteAllServers
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (savedServers.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(70.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.server_click_to_add),
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 100.dp, max = 280.dp)
                            ) {
                                itemsIndexed(savedServers) { _, server ->
                                    var isVisibleDrop by remember { mutableStateOf(false) }

                                    ServerItem(
                                        networkStatus = networkStatus,
                                        isWifiOn = isWifiOn,
                                        isBluetoothOn = isBluetoothOn,
                                        server = server,
                                        isSelected = server == currentSelectedServer,
                                        onSelect = {
                                            if (server != currentSelectedServer) {
                                                connection.disconnect()
                                                serversControl.setCurrentSelectedServer(server)
                                            }
                                            view.playSoundClick()
                                        },
                                        onLongSelect = {
                                            selectedServer = server
                                            isVisibleDrop = true
                                        }
                                    ) {
                                        MoreVertDropdown(
                                            view = view,
                                            expanded = isVisibleDrop,
                                            onDismiss = { isVisibleDrop = false },
                                            menuItems = listOf(
                                                DropMenuModel(
                                                    text = stringResource(R.string.server_edit_port),
                                                    onClick = {
                                                        selectedServer = server
                                                        isVisibleDrop = false
                                                        isVisibleEditPort = true
                                                    },
                                                    icon = "baseline_edit"
                                                ),
                                                DropMenuModel(
                                                    text = stringResource(R.string.server_remove),
                                                    onClick = {
                                                        selectedServer = server
                                                        isVisibleDrop = false
                                                        showDeleteConfirm = true
                                                    },
                                                    icon = "baseline_delete",
                                                    isDestructive = true
                                                ),
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // ===== FOUND SERVERS =====
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    ) {
                        SectionHeader(
                            title = stringResource(R.string.server_found_title)
                        )

                        // ============================================================
                        // 2 NÚT SCAN RIÊNG
                        // ============================================================
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Nút QUÉT BLUETOOTH
                            Button(
                                onClick = {
                                    if (!BluetoothPermissionHandler.hasBluetoothPermissions(context)) {
                                        activity?.let {
                                            BluetoothPermissionHandler.requestBluetoothPermissions(
                                                it
                                            )
                                        }
                                    } else if (isBluetoothOn) {
                                        if (isLoadingBlt) {
                                            scanner.stopScanBlt()
                                        } else {
                                            // ============================================================
                                            // ✅ Đọc tên + UUID từ config
                                            // ============================================================
                                            val targetName =
                                                config.defaultEsp32s3Name.ifBlank { null }
                                            val targetUuid = try {
                                                UUID.fromString(config.serviceUuid)
                                            } catch (e: Exception) {
                                                null
                                            }

                                            scanner.startScanBlt(
                                                filterServiceUuid = targetUuid,   // ← Từ config
                                                targetName = targetName,           // ← Từ config
                                                timeoutMs = 15_000L
                                            )
                                        }
                                    } else {
                                        networkStatus.checkBleAndRequest()
                                    }
                                    view.playSoundClick()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isLoadingBlt)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (isLoadingBlt) R.drawable.baseline_stop
                                        else R.drawable.baseline_bluetooth
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isLoadingBlt)
                                        stringResource(R.string.server_scan_ble_stop)
                                    else
                                        stringResource(R.string.server_scan_ble),
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }

                            // Nút QUÉT WI-FI
                            Button(
                                onClick = {
                                    if (isWifiOn) {
                                        if (isLoadingTcp) {
                                            scanner.stopScanTcp()
                                        } else {
                                            scanner.startScanTcp(portsResult)
                                        }
                                    } else {
                                        val intent =
                                            Intent(Settings.ACTION_WIFI_SETTINGS)
                                        context.startActivity(intent)
                                    }
                                    view.playSoundClick()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isLoadingTcp)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.tertiary
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (isLoadingTcp) R.drawable.baseline_stop
                                        else R.drawable.baseline_wifi
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isLoadingTcp)
                                        stringResource(R.string.server_scan_wifi_stop)
                                    else
                                        stringResource(R.string.server_scan_wifi),
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        // ============================================================
                        // DANH SÁCH KẾT QUẢ
                        // ============================================================
                        when {
                            // ============================================================
                            // Cả 2 tắt
                            // ============================================================
                            !isBluetoothOn && !isWifiOn -> {
                                EmptyState(
                                    icon = Icons.Default.Warning,
                                    text = stringResource(R.string.server_enable_wifi_bluetooth)
                                )
                            }

                            // ============================================================
                            // ✅ Đang quét → hiển thị loading (ưu tiên cao)
                            // ============================================================
                            (isLoadingBlt || isLoadingTcp) && foundServers.isEmpty() -> {
                                LoadingState(
                                    text = when {
                                        isLoadingBlt && isLoadingTcp -> stringResource(R.string.server_scanning_both)
                                        isLoadingBlt -> stringResource(R.string.server_scanning_ble)
                                        else -> stringResource(R.string.server_scanning_wifi)
                                    }
                                )
                            }

                            // ============================================================
                            // ✅ Không quét + Không có device + Chưa có Wi-Fi server → Rescue
                            // ============================================================
                            !isLoadingBlt && !isLoadingTcp && foundServers.isEmpty() && showRescueServer -> {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 80.dp, max = 280.dp)
                                ) {
                                    item {
                                        RescueServerItem(
                                            onClick = {
                                                showRescueConfirm = true
                                                view.playSoundClick()
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                }
                            }

                            // ============================================================
                            // ✅ Không quét + Không có device + Không cần cứu hộ → Empty
                            // ============================================================
                            !isLoadingBlt && !isLoadingTcp && foundServers.isEmpty() && !showRescueServer -> {
                                EmptyState(
                                    icon = null,
                                    text = stringResource(R.string.server_found_empty_hint)
                                )
                            }

                            // ============================================================
                            // ✅ Có device → hiển thị danh sách
                            // ============================================================
                            foundServers.isNotEmpty() -> {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 80.dp, max = 280.dp)
                                ) {
                                    // Rescue server lên đầu (nếu cần)
                                    if (showRescueServer) {
                                        item {
                                            RescueServerItem(
                                                onClick = {
                                                    showRescueConfirm = true
                                                    view.playSoundClick()
                                                }
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }
                                    }

                                    // Danh sách device
                                    itemsIndexed(foundServers) { _, server ->
                                        ServerItem(
                                            server = server,
                                            isSelected = server == currentSelectedServer,
                                            onSelect = {
                                                serversControl.addSavedServer(server)
                                                serversControl.removeFoundServer(server)
                                                view.playSoundClick()
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                }
                            }
                        }

                    }
                }

                // ============================================================
                // BOTTOM BAR
                // ============================================================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(vertical = 12.dp)
                ) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "Set Switch mode",
                            fontStyle = FontStyle.Italic,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FloatingActionButton(
                            onClick = { showOptionsMode = !showOptionsMode; view.playSoundClick() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painterResource(R.drawable.baseline_import_export),
                                contentDescription = null
                            )
                            MoreVertDropdown(
                                view = view,
                                expanded = showOptionsMode,
                                onDismiss = { showOptionsMode = false },
                                menuItems = listOf(
                                    DropMenuModel(
                                        text = stringResource(R.string.server_request_switch_mode_ble),
                                        onClick = {
                                            if (isBluetoothOn) {
                                                connection.requestSwitchToBle()
                                            } else {
                                                networkStatus.checkBleAndRequest()
                                            }
                                            showOptionsMode = false
                                        },
                                        icon = "baseline_bluetooth",
                                        isVisible = { (connectionStatus == ConnectionStatus.CONNECTED) && !getSelectedDevice.bluetooth && isWifiOn }
                                    ),
                                    DropMenuModel(
                                        text = stringResource(R.string.server_request_switch_mode_wifi),
                                        onClick = {
                                            if (isWifiOn) {
                                                connection.requestSwitchToWifi()
                                            } else {
                                                networkStatus.checkWifiAndRequest()
                                            }
                                            showOptionsMode = false
                                        },
                                        icon = "baseline_wifi",
                                        isVisible = { (connectionStatus == ConnectionStatus.CONNECTED) && getSelectedDevice.bluetooth && isBluetoothOn }
                                    ),

                                    DropMenuModel(
                                        text = stringResource(R.string.server_request_switch_mode_rescue),
                                        onClick = {
                                            showRescueConfirm = true
                                            showOptionsMode = false
                                        },
                                        icon = "baseline_launch",
                                        isVisible = { connectionStatus == ConnectionStatus.CONNECTED && (isBluetoothOn || isWifiOn) && getSelectedDevice.isHID }
                                    ),

                                    DropMenuModel(
                                        text = "http://192.168.4.1",
                                        onClick = {
                                            val intent = Intent(
                                                Intent.ACTION_VIEW,
                                                "http://192.168.4.1".toUri()
                                            )
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            context.startActivity(intent)
                                        },
                                        isVisible = { isWifiOn && getSelectedDevice.isHID },
                                        icon = "baseline_link",
                                    )
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = stringResource(R.string.settings_title),
                            fontStyle = FontStyle.Italic,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FloatingActionButton(
                            onClick = {
                                showOptionsSettings = !showOptionsSettings; view.playSoundClick()
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painterResource(R.drawable.baseline_settings),
                                contentDescription = null
                            )
                            MoreVertDropdown(
                                view = view,
                                expanded = showOptionsSettings,
                                onDismiss = { showOptionsSettings = false },
                                menuItems = listOf(
                                    DropMenuModel(
                                        text = stringResource(R.string.server_set_ports_scanner),
                                        onClick = {
                                            isVisiblePorts = true
                                            showOptionsSettings = false
                                        },
                                        icon = "outline_electrical_services"
                                    ),
                                    DropMenuModel(
                                        text = stringResource(R.string.ble_config_title),
                                        onClick = {
                                            showBleConfigDialog = true
                                            showOptionsSettings = false
                                        },
                                        icon = "baseline_settings_input_composite"
                                    ),
                                    DropMenuModel(
                                        text = stringResource(R.string.server_manual_add),
                                        onClick = {
                                            isVisibleIp = true
                                            showOptionsSettings = false
                                        },
                                        icon = "baseline_add"
                                    ),
                                )
                            )
                        }
                    }
                }
            }

            // ============================================================
            // DIALOGS
            // ============================================================
            if (isVisibleIp) {
                DialogAddNewServer(
                    context = context,
                    config = config,
                    port = portsResult,
                    onAddNewServer = { server -> serversControl.setFoundServers(listOf(server)) },
                    onDismiss = { isVisibleIp = false }
                )
            }

            if (isVisiblePorts) {
                DialogAddPorts(
                    context = context,
                    port = portsResult,
                    onUpdatePort = { serversControl.updatePort(it) },
                    onDismiss = { isVisiblePorts = false }
                )
            }

            if (isVisibleEditPort && selectedServer != null) {
                DialogEditServer(
                    context = context,
                    serverControl = serversControl,
                    server = selectedServer!!,
                    onDismiss = {
                        isVisibleEditPort = false
                        selectedServer = null
                    }
                )
            }

            if (showDeleteConfirm && selectedServer != null) {
                ConfirmDialog(
                    show = showDeleteConfirm,
                    title = stringResource(R.string.server_confirm_delete_title),
                    message = stringResource(
                        R.string.server_confirm_delete_message,
                        selectedServer?.name ?: ""
                    ),
                    onConfirm = {
                        serversControl.removeSavedServer(selectedServer!!)
                        connection.disconnect()
                        showDeleteConfirm = false
                        selectedServer = null
                    },
                    onDismiss = {
                        showDeleteConfirm = false
                        selectedServer = null
                    }
                )
            }


            if (isDeleteAllServers) {
                ConfirmDialog(
                    show = isDeleteAllServers,
                    title = stringResource(R.string.server_confirm_delete_title),
                    message = stringResource(
                        R.string.server_confirm_delete_message,
                        savedServers.size
                    ),
                    onConfirm = {
                        serversControl.removeAllSavedServers()
                        connection.disconnect()
                        isDeleteAllServers = false
                    },
                    onDismiss = {
                        isDeleteAllServers = false
                    }
                )
            }

            // ============================================================
            // RESCUE CONFIRM DIALOG
            // ============================================================
            if (showRescueConfirm) {
                SmartRescueGuideDialog(
                    context = context,
                    connection = connection,
                    onDismiss = { showRescueConfirm = false }
                )
            }

            if (showBleConfigDialog) {
                BleConfigDialog(
                    context = context,
                    currentConfig = config,
                    onSave = { newConfig ->
                        scanner.saveConfigBle(newConfig)
                        showBleConfigDialog = false
                    },
                    onDismiss = { showBleConfigDialog = false }
                )
            }
        }

    }
}


// ============================================================
// SECTION HEADER
// ============================================================
@Composable
private fun SectionHeader(
    title: String,
    savedServers: List<DeviceServerModel>? = null,
    onClick: (() -> Unit)? = null
) {

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            if ((savedServers?.isNotEmpty() == true) && onClick != null) {
                IconButton(onClick = onClick) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null
                    )
                }
            }
        }
        Divider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    }

}

// ============================================================
// LOADING STATE
// ============================================================
@Composable
private fun LoadingState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DotsLoadingIndicator()
            Text(
                text = text,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

// ============================================================
// EMPTY STATE
// ============================================================
@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    text: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

// ============================================================
// RESCUE SERVER ITEM — Nút cứu hộ chuyển sang Wi-Fi
// ============================================================
@Composable
private fun RescueServerItem(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {}
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.baseline_wifi),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Cấu hình Wi-Fi qua Web",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "WEB",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Kết nối Wi-Fi 'ESP32S3_HID_Setup' → mở http://192.168.4.1",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                painter = painterResource(R.drawable.baseline_arrow_forward),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
            )
        }
    }
}
