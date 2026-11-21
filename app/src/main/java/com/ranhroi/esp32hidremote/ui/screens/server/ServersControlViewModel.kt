package com.ranhroi.esp32hidremote.ui.screens.server

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ranhroi.esp32hidremote.models.DeviceServerModel
import com.ranhroi.esp32hidremote.utils.ConstantsUtils
import com.ranhroi.esp32hidremote.utils.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ServersControlViewModel @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : ViewModel() {

    // Danh sách Server đã lưu
    private val _savedServers = MutableStateFlow<List<DeviceServerModel>>(emptyList())
    val savedServers: StateFlow<List<DeviceServerModel>> = _savedServers.asStateFlow()

    // Danh sách Server tìm thấy (quét qua mạng)
    private val _foundServers = MutableStateFlow<List<DeviceServerModel>>(emptyList())
    val foundServers: StateFlow<List<DeviceServerModel>> = _foundServers.asStateFlow()

    private val _currentSelectedServer = MutableStateFlow<DeviceServerModel?>(null)
    val currentSelectedServer: StateFlow<DeviceServerModel?> = _currentSelectedServer.asStateFlow()

    val portConnection: StateFlow<Int> = dataStoreManager
        .get(ConstantsUtils.KEY_PORT_CONNECTION, ConstantsUtils.PORT_CONNECTION)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            initialValue = ConstantsUtils.PORT_CONNECTION
        )

    init {
        loadDeviceServers()
    }

    fun loadDeviceServers() {
        // Lắng nghe danh sách Server: Khi DataStore thay đổi, StateFlow sẽ tự cập nhật
        viewModelScope.launch {
            dataStoreManager.getAllDevices().collect { servers ->
                _savedServers.value = servers
                // Tự động cập nhật server đang được chọn dựa trên field 'selected'
                _currentSelectedServer.value = servers.find { it.selected }
            }
        }

        // Bạn có thể giữ thêm hàm này nếu DataStoreManager có hàm getSelectedDevice() riêng biệt
        viewModelScope.launch {
            dataStoreManager.getSelectedDevice().collect { selected ->
                _currentSelectedServer.value = selected
            }
        }
    }

    // ============= Server Management (THÊM, SỬA, XÓA) =============

    /**
     * Xóa Server khỏi danh sách đã lưu
     */
    fun removeSavedServer(server: DeviceServerModel) {
        viewModelScope.launch {
            // ✅ Xóa theo ID
            dataStoreManager.deleteDevice(server.id)
            loadDeviceServers()

            // Nếu xóa server đang được chọn, reset selected
            if (_currentSelectedServer.value?.id == server.id) {
                _currentSelectedServer.value = null
            }
        }
    }

    fun removeAllSavedServers() {
        viewModelScope.launch {
            dataStoreManager.deleteAllDevices()
            loadDeviceServers()

            _currentSelectedServer.value = null
        }
    }

    /**
     * Thêm Server mới vào danh sách lưu trữ
     */
    fun addSavedServer(server: DeviceServerModel) {
        viewModelScope.launch {
            val currentList = _savedServers.value
            // ✅ Kiểm tra trùng lặp address + port
            val exists = currentList.any { it.address == server.address && it.port == server.port }
            if (!exists) {
                // ✅ KHÔNG set selected ở đây, để DataStoreManager tự xử lý ID
                val serverToSave =
                    if (currentList.isEmpty()) server.copy(selected = true) else server
                dataStoreManager.saveDevice(serverToSave)
                loadDeviceServers() // ✅ Refresh danh sách
            }
        }
    }

    /**
     * Cập nhật thông tin Server (Sửa)
     */
    fun setServer(server: DeviceServerModel) {
        viewModelScope.launch {
            // ✅ GIỮ NGUYÊN ID, chỉ cập nhật thông tin
            dataStoreManager.saveDevice(server)
            loadDeviceServers()
        }
    }

    /**
     * Đánh dấu Server này là Server đang được chọn kết nối
     */
    fun setCurrentSelectedServer(server: DeviceServerModel) {
        viewModelScope.launch {
            // Hàm này trong DataStoreManager sẽ set true cho server này và false cho các cái khác
            dataStoreManager.setSelectedDevice(server.id)
        }
    }

    // ============= Found Servers (Quét mạng) =============

    fun setFoundServers(servers: List<DeviceServerModel>) {
        _foundServers.value = servers.distinctBy { it.address to it.port }
    }

    fun removeFoundServer(server: DeviceServerModel) {
        _foundServers.value = _foundServers.value.filter { it != server }
    }

    fun clearFoundServers() {
        _foundServers.value = emptyList()
    }

    fun updatePort(port: Int) {
        viewModelScope.launch {
            runCatching {
                dataStoreManager.put(ConstantsUtils.KEY_PORT_CONNECTION, port)
            }.onFailure {
            }
        }
    }
}