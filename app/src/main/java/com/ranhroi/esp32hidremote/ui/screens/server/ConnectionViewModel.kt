package com.ranhroi.esp32hidremote.ui.screens.server

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ranhroi.esp32hidremote.R
import com.ranhroi.esp32hidremote.models.CombineSettingModel
import com.ranhroi.esp32hidremote.models.ConnectionStatus
import com.ranhroi.esp32hidremote.models.ConnectionStatusModel
import com.ranhroi.esp32hidremote.models.DeviceServerModel
import com.ranhroi.esp32hidremote.models.HybridConnectionModel
import com.ranhroi.esp32hidremote.ui.screens.server.send.HidTransmitter
import com.ranhroi.esp32hidremote.ui.screens.server.send.InputManager
import com.ranhroi.esp32hidremote.utils.ConstantsUtils
import com.ranhroi.esp32hidremote.utils.DataStoreConfig
import com.ranhroi.esp32hidremote.utils.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "ConnectionVM"

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStoreManager,
    private val dataStoreConfig: DataStoreConfig,
    private val connection: ConnectionManager,
    private val hidTransmitter: HidTransmitter,
) : ViewModel() {

    // ============================================================
    // CONFIG
    // ============================================================
    val config = dataStoreConfig.combine.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(500),
        CombineSettingModel()
    )

    // ============================================================
    // STATE
    // ============================================================
    private val _hasServers = MutableStateFlow(false)
    val hasServers: StateFlow<Boolean> = _hasServers.asStateFlow()

    private val _isLoadingServers = MutableStateFlow(false)
    val isLoadingServers: StateFlow<Boolean> = _isLoadingServers.asStateFlow()

    private val _uiState = MutableStateFlow(ConnectionStatusModel())
    val uiState: StateFlow<ConnectionStatusModel> = _uiState.asStateFlow()

    private var commandListener: ConnectionManager.Listener? = null

    val getSelectedDevice: StateFlow<DeviceServerModel> =
        dataStore.getSelectedDevice()
            .filterNotNull()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(300),
                initialValue = DeviceServerModel()
            )

    private val _inputState = MutableStateFlow(
        InputManager(context, hidTransmitter) { config.value }
    )
    val inputState: StateFlow<InputManager> = _inputState.asStateFlow()

    // ============================================================
    // CONNECTION STATUS
    // ============================================================
    val connectionStatus: StateFlow<ConnectionStatus> = uiState
        .map { ui ->
            when {
                ui.showPassDeviceDialog -> ConnectionStatus.NEED_DEVICE_PASSWORD
                ui.isLoading -> ConnectionStatus.CONNECTING
                ui.isConnected -> ConnectionStatus.CONNECTED
                !ui.errorMessage.isNullOrBlank() -> ConnectionStatus.ERROR
                else -> ConnectionStatus.DISCONNECTED
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(500),
            initialValue = ConnectionStatus.DISCONNECTED
        )

    // ============================================================
    // INIT
    // ============================================================
    init {
        loadServers()
        startWithConfig()
        setupConnectionListener()
    }

    // ============================================================
    // LOAD SERVERS
    // ============================================================
    private fun loadServers() {
        viewModelScope.launch {
            _isLoadingServers.value = true
            try {
                dataStore.getAllDevices().collect { servers ->
                    _uiState.update { it.copy(servers = servers) }
                    _hasServers.value = servers.isNotEmpty()
                    _isLoadingServers.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadServers error: ${e.message}")
                _hasServers.value = false
                _isLoadingServers.value = false
            }
        }
    }

    // ============================================================
    // CONFIG & STARTUP
    // ============================================================
    private fun startWithConfig() {
        viewModelScope.launch {
            try {
                // Chờ DataStore load xong (tối đa 2s)
                val selectedDevice = withTimeoutOrNull(2000.milliseconds) {
                    getSelectedDevice.first { it.id != 0 }
                }

                if (selectedDevice == null) {
                    Log.d(TAG, "startWithConfig: no device found")
                    showNoServerMessage()
                    return@launch
                }

                if (selectedDevice.selected) {
                    connectToServer(selectedDevice)
                } else {
                    _uiState.update {
                        it.copy(
                            statusMessage = context.getString(R.string.connection_auto_failed)
                        )
                    }
                    showNoServerMessage()
                }
            } catch (e: Exception) {
                Log.e(TAG, "startWithConfig error: ${e.message}")
            }
        }
    }

    // ============================================================
    // CONNECTION LISTENER
    // ============================================================
    private fun setupConnectionListener() {
        hidTransmitter.close()
        commandListener?.let { connection.unregisterListener() }
        commandListener = null

        connection.setManager(viewModelScope)

        commandListener = object : ConnectionManager.Listener {
            override fun onConnectResult(state: HybridConnectionModel) {
                viewModelScope.launch {
                    if (state.isConnected) {
                        handleConnectSuccess(state)
                    } else {
                        handleConnectFailure(state.server.name)
                    }
                }
            }

            override fun onVerifyResult(success: Boolean) {
                viewModelScope.launch {
                    if (success) {
                        handleVerifySuccess()
                    } else {
                        // Mật khẩu sai hoặc xác thực thất bại:
                        // dừng tự động thử lại và giữ hộp thoại mở.
                        _uiState.update { ui ->
                            ui.copy(
                                isLoading = false,
                                isConnected = ui.state.isConnected,
                                showPassDeviceDialog = true,
                                isPasswordError = true,
                                errorMessage = null,
                                statusMessage = context.getString(
                                    R.string.dialog_verify_invalid_password
                                )
                            )
                        }
                    }
                }
            }

            override fun onBytesReceived(bytes: ByteArray) {
                viewModelScope.launch {
                    // Trường hợp nhận chuỗi byte rời rạc hoặc dạng mảng trực tiếp
                    if (bytes.isNotEmpty() && bytes[0] == 0xBB.toByte() && bytes.size >= 4) {
                        val numLock = bytes[1] == 1.toByte()
                        val capsLock = bytes[2] == 1.toByte()
                        val scrollLock = bytes[3] == 1.toByte()

                        // Đẩy trực tiếp trạng thái mới vào uiState để ép Compose vẽ lại đèn
                        _uiState.update { ui ->
                            ui.copy(
                                isNumLockOn = numLock,
                                isCapsLockOn = capsLock,
                                isScrollLockOn = scrollLock
                            )
                        }
                        return@launch // Thoát sớm vì đây là gói tin trạng thái hệ thống
                    }

                    for (byte in bytes) {
                        when (byte) {
                            ConnectionManager.REP_REQUIRE_AUTH -> {
                                _uiState.update {
                                    it.copy(
                                        isConnected = true,
                                        showPassDeviceDialog = true,
                                        statusMessage = context.getString(
                                            R.string.connection_device_requires_password
                                        )
                                    )
                                }
                            }

                            ConnectionManager.REP_AUTH_SUCCESS -> {
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        isConnected = true,
                                        showPassDeviceDialog = false,
                                        isPasswordError = false,
                                        statusMessage = context.getString(
                                            R.string.connection_auth_success
                                        )
                                    )
                                }
                            }

                            ConnectionManager.REP_AUTH_FAILED -> {
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        showPassDeviceDialog = true,
                                        isPasswordError = true,
                                        statusMessage = context.getString(
                                            R.string.dialog_verify_invalid_password
                                        )
                                    )
                                }
                            }

                            ConnectionManager.REP_PONG -> {
                                Log.d(TAG, "Pong received")
                            }
                        }
                    }
                }
            }

            override fun onSwitchResult(success: Boolean) {
                viewModelScope.launch {
                    if (success) {
                        _uiState.update {
                            it.copy(
                                statusMessage = context.getString(
                                    R.string.connection_switch_sent
                                )
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                statusMessage = context.getString(
                                    R.string.connection_switch_failed
                                )
                            )
                        }
                    }
                }
            }
        }
        commandListener?.let { connection.registerListener(it) }
    }

    // ============================================================
    // XỬ LÝ KẾT NỐI THÀNH CÔNG
    // ============================================================
    private fun handleConnectSuccess(state: HybridConnectionModel) {
        _uiState.update {
            it.copy(
                state = state,
                password = state.server.password
            )
        }

        when {
            state.isNeedDevicePassword -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isConnected = true,
                        showPassDeviceDialog = true,
                        statusMessage = context.getString(
                            R.string.connection_device_requires_password
                        )
                    )
                }
            }

            state.isVerified || state.isBluetooth -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isConnected = true,
                        showPassDeviceDialog = false,
                        statusMessage = context.getString(R.string.connection_success)
                    )
                }
            }

            else -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isConnected = true,
                        statusMessage = context.getString(
                            R.string.connection_waiting_auth
                        )
                    )
                }
            }
        }

        hidTransmitter.setState(state)
    }

    private fun handleConnectFailure(serverName: String) {
        _uiState.update {
            it.copy(
                isLoading = false,
                isConnected = false,
                showPassDeviceDialog = false,
                errorMessage = context.getString(R.string.connection_failed, serverName)
            )
        }
    }

    // ============================================================
    // XỬ LÝ XÁC THỰC
    // ============================================================
    private fun handleVerifySuccess() {
        _uiState.update { ui ->
            ui.copy(
                state = ui.state.copy(
                    isVerified = true,
                    isNeedDevicePassword = false
                ),
                isLoading = false,
                isConnected = true,
                showPassDeviceDialog = false,
                isPasswordError = false,
                statusMessage = context.getString(R.string.connection_auth_success)
            )
        }
    }

    // ============================================================
    // CONNECT / DISCONNECT
    // ============================================================
    fun connectToServer(server: DeviceServerModel) {
        viewModelScope.launch {
            val currentState = _uiState.value.state
            val isCurrentlyConnected = currentState.isConnected

            // ============================================================
            // BƯỚC 1: KIỂM TRA CÓ CẦN SWITCH MODE KHÔNG
            // ============================================================
            if (isCurrentlyConnected) {
                val currentIsBle = currentState.isBluetooth
                val targetIsBle = server.bluetooth

                // KIỂM TRA ĐỐI TƯỢNG QUA CỜ ISHID CỦA BẠN
                // Nếu cả Server cũ và Server mới đều là bo mạch ESP32 (isHID = true)
                // Hoặc cả hai đều là PC nhưng bạn thiết kế PC có khả năng tự lật cánh Radio
                if (currentState.server.isHID && server.isHID) {

                    // CHỈ CHẠY LOGIC SWITCH GỐC KHI THAY ĐỔI PHƯƠNG THỨC KẾT NỐI (WIFI <-> BLE)
                    if (currentIsBle != targetIsBle) {
                        _uiState.update {
                            it.copy(
                                isLoading = true,
                                statusMessage = context.getString(R.string.connection_switching_mode)
                            )
                        }

                        // Gửi lệnh switch qua kênh hiện tại 
                        val switchOk = if (targetIsBle) {
                            connection.requestSwitchToBle()
                        } else {
                            connection.requestSwitchToWifi()
                        }

                        if (!switchOk) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = context.getString(
                                        if (targetIsBle) R.string.connection_switch_to_ble_failed
                                        else R.string.connection_switch_to_wifi_failed
                                    )
                                )
                            }
                            return@launch
                        }

                        // Đợi thiết bị mất kết nối cũ - tối đa 5s 
                        val startTime = System.currentTimeMillis()
                        while (System.currentTimeMillis() - startTime < 5000) {
                            if (!_uiState.value.state.isConnected) break
                            delay(200.milliseconds)
                        }

                        // Đợi thiết bị ổn định mode mới 
                        delay(1000.milliseconds)

                        // Ngắt kết nối cũ phía Client 
                        disconnect()
                        delay(300.milliseconds)

                        _uiState.update {
                            it.copy(statusMessage = context.getString(R.string.connection_switch_then_connect))
                        }
                    }
                } else {
                    // TRƯỜNG HỢP LIÊN QUAN ĐẾN PC (isHID = false) HOẶC ĐỔI SANG THIẾT BỊ KHÁC:
                    // Không chạy luồng gối đầu lệnh switch của mạch, lập tức ngắt kết nối cũ để sang mục tiêu mới
                    disconnect()
                    delay(100.milliseconds)
                }
            }

            // ============================================================
            // BƯỚC 2: KẾT NỐI SERVER MỚI 
            // ============================================================
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isConnected = false,
                    showPassDeviceDialog = false,
                    isPasswordError = false,
                    errorMessage = null,
                    statusMessage = context.getString(R.string.connection_connecting, server.name)
                )
            }

            connection.connect(server)
            dataStore.setSelectedDevice(server.id)
        }
    }

    fun disconnect() {
        connection.close()
        val emptyState = HybridConnectionModel()
        hidTransmitter.setState(emptyState)
        _uiState.update {
            it.copy(
                isConnected = false,
                isLoading = false,
                showPassDeviceDialog = false,
                isPasswordError = false,
                statusMessage = context.getString(R.string.connection_disconnected),
                state = emptyState,

                // TẮT TẤT CẢ ĐÈN KHI NGẮT KẾT NỐI
                isNumLockOn = false,
                isCapsLockOn = false,
                isScrollLockOn = false
            )
        }
    }


    // ============================================================
    // PASSWORD DEVICE HANDLER
    // ============================================================

    fun submitPasswordDevice(
        currentServer: DeviceServerModel,
        password: String,
        rememberPass: Boolean
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    password = password,
                    isPasswordError = false,
                    errorMessage = null,
                    statusMessage = context.getString(
                        R.string.connection_checking_password
                    )
                )
            }

            try {
                val verified = connection.verifyPassword(password)

                if (!verified) {
                    // Không lưu mật khẩu sai.
                    // Hiển thị thông báo lỗi và giữ dialog mở.
                    _uiState.update { ui ->
                        ui.copy(
                            isLoading = false,
                            showPassDeviceDialog = true,
                            isPasswordError = true,
                            errorMessage = context.getString(
                                R.string.dialog_verify_invalid_password
                            ),
                            statusMessage = context.getString(
                                R.string.dialog_verify_invalid_password
                            )
                        )
                    }
                    return@launch
                }

                if (rememberPass) {
                    dataStore.saveDevice(
                        currentServer.copy(
                            password = password,
                            rememberPass = true
                        )
                    )
                } else {
                    dataStore.saveDevice(
                        currentServer.copy(
                            password = "",
                            rememberPass = false
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "submitPasswordDevice error", e)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showPassDeviceDialog = true,
                        isPasswordError = true,
                        errorMessage = context.getString(
                            R.string.dialog_verify_invalid_password
                        )
                    )
                }
            }
        }
    }


    fun dismissPasswordDialog() {
        _uiState.update {
            it.copy(
                showPassDeviceDialog = false,
                isPasswordError = false,
                statusMessage = context.getString(R.string.connection_cancelled)
            )
        }
    }

    // ============================================================
    // SWITCH MODE (Wi-Fi ↔ BLE ↔ SETUP)
    // ============================================================
    fun requestSwitchToWifi() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    statusMessage = context.getString(
                        R.string.connection_switch_to_wifi_requesting
                    )
                )
            }

            val ok = connection.requestSwitchToWifi()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    statusMessage = if (ok) {
                        context.getString(R.string.connection_switch_to_wifi_sent)
                    } else {
                        context.getString(R.string.connection_switch_to_wifi_failed)
                    }
                )
            }
        }
    }

    fun requestSwitchToBle() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    statusMessage = context.getString(
                        R.string.connection_switch_to_ble_requesting
                    )
                )
            }

            val ok = connection.requestSwitchToBle()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    statusMessage = if (ok) {
                        context.getString(R.string.connection_switch_to_ble_sent)
                    } else {
                        context.getString(R.string.connection_switch_to_ble_failed)
                    }
                )
            }
        }
    }

    fun requestSwitchToSetup() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    statusMessage = context.getString(
                        R.string.connection_switch_to_setup_requesting
                    )
                )
            }

            val ok = connection.requestSwitchToSetup()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    statusMessage = if (ok) {
                        context.getString(R.string.connection_switch_to_setup_sent)
                    } else {
                        context.getString(R.string.connection_switch_to_setup_failed)
                    }
                )
            }
        }
    }

    // ============================================================
    // PING
    // ============================================================
    fun pingConnection() {
        val currentState = _uiState.value.state

        viewModelScope.launch {
            if (!currentState.isBluetooth && config.value.isAutoPing && !currentState.isNeedDevicePassword) {
                _uiState.update {
                    it.copy(
                        statusMessage = context.getString(R.string.connection_ping),
                        isLoading = true
                    )
                }


                if (!currentState.isConnected || currentState.hidOutput == null) {
                    _uiState.update {
                        it.copy(
                            statusMessage = context.getString(R.string.connection_not_connected),
                            isLoading = false,
                            isConnected = false
                        )
                    }
                    attemptReconnection(context.getString(R.string.connection_reconnecting))
                    return@launch
                }

                val ok = connection.ping(currentState)

                if (ok) {
                    _uiState.update {
                        it.copy(
                            statusMessage = context.getString(R.string.connection_stable),
                            errorMessage = null,
                            isConnected = true,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            statusMessage = context.getString(R.string.connection_ping_failed),
                            isLoading = false,
                            isConnected = false
                        )
                    }
                    attemptReconnection(context.getString(R.string.connection_reconnecting))
                }
            }
        }
    }

    private fun attemptReconnection(message: String) {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        statusMessage = message,
                        isLoading = true,
                        isConnected = false,
                        errorMessage = null
                    )
                }

                disconnect()
                delay(500.milliseconds)

                val device = dataStore.getSelectedDevice().filterNotNull().first()

                _uiState.update {
                    it.copy(
                        statusMessage = context.getString(
                            R.string.connection_reconnecting_role
                        ),
                        isLoading = true
                    )
                }

                connectToServer(device)

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isConnected = false,
                        statusMessage = context.getString(
                            R.string.connection_reconnect_failed
                        ),
                        errorMessage = e.message
                    )
                }
            }
        }
    }

    // ============================================================
    // UI HELPERS
    // ============================================================
    private fun showNoServerMessage() {
        _uiState.update {
            it.copy(
                statusMessage = context.getString(R.string.connection_no_server),
                errorMessage = context.getString(R.string.connection_select_server)
            )
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            dataStore.put(ConstantsUtils.FIRST_LAUNCH, true)
        }
    }

    fun updateRotation(isRotation: Boolean) {
        viewModelScope.launch {
            dataStore.put(ConstantsUtils.SHOW_ROTATION, isRotation)
        }
    }

    // ============================================================
    // CLEANUP
    // ============================================================
    override fun onCleared() {
        if (!getSelectedDevice.value.bluetooth) {
            _uiState.update { it.copy(isConnected = false) }
        }
        connection.unregisterListener()
        commandListener = null
        connection.close()
        hidTransmitter.close()
        super.onCleared()
    }
}