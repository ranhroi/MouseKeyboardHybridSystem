package com.ranhroi.esp32hidremote.ui.screens.shortkey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ranhroi.esp32hidremote.models.HybridConnectionModel
import com.ranhroi.esp32hidremote.models.ShortKeyModel
import com.ranhroi.esp32hidremote.utils.ChannelManager
import com.ranhroi.esp32hidremote.utils.ConstantsUtils
import com.ranhroi.esp32hidremote.utils.DataStoreManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class ShortKeySetupViewModel @Inject constructor(
    private val dataStore: DataStoreManager
) : ViewModel() {

    // ============================================================
    // STATE
    // ============================================================
    val allShortKeys: StateFlow<List<ShortKeyModel>> = dataStore.getAllShortKeys()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedKey = MutableStateFlow<String?>(null)
    val selectedKey: StateFlow<String?> = _selectedKey.asStateFlow()

    private val _isManualSelected = MutableStateFlow(false)
    val isManualSelected: StateFlow<Boolean> = _isManualSelected.asStateFlow()

    private val _detectedAppType = MutableStateFlow<String?>(null)
    val detectedAppType: StateFlow<String?> = _detectedAppType.asStateFlow()

    // ============================================================
    // DETECTION STATE
    // ============================================================
    private val mediaAppManager = MediaAppManager()
    private var tcpReadJob: Job? = null
    private var bleReadJob: Job? = null

    @Volatile
    private var running = false

    // ============================================================
    // SHORTKEY MANAGEMENT
    // ============================================================
    fun getShortKeysByType(type: String): List<ShortKeyModel> =
        allShortKeys.value.filter { it.type == type }

    fun addShortKey(shortKey: ShortKeyModel) {
        viewModelScope.launch {
            try {
                dataStore.saveShortKey(shortKey)
            } catch (_: Exception) {
            }
        }
    }

    fun removeShortKey(shortKey: ShortKeyModel) {
        viewModelScope.launch {
            try {
                dataStore.deleteShortKey(shortKey.id)
            } catch (_: Exception) {
            }
        }
    }

    fun updateShortKey(shortKey: ShortKeyModel) {
        addShortKey(shortKey)
    }

    // ============================================================
    // SELECTED KEY
    // ============================================================
    fun loadSelectedKey() {
        viewModelScope.launch {
            val saved = dataStore.get(ConstantsUtils.TYPE_MEDIA, "media").first()
            _selectedKey.value = saved.ifBlank { "media" }
        }
    }

    /** Người dùng chọn thủ công → đánh dấu manual, không bị auto ghi đè. */
    fun selectedKey(type: String?) {
        val value = type ?: "media"
        _selectedKey.value = value
        _isManualSelected.value = true
        viewModelScope.launch {
            dataStore.put(ConstantsUtils.TYPE_MEDIA, value)
        }
    }

    /** Auto-detect: chỉ cập nhật nếu người dùng chưa chọn thủ công. */
    fun onAppDetected(type: String) {
        _detectedAppType.value = type
        if (!_isManualSelected.value) {
            _selectedKey.value = type
        }
    }

    /** Reset về auto (nếu cần). */
    fun resetToAuto() {
        _isManualSelected.value = false
        _detectedAppType.value?.let { _selectedKey.value = it }
    }


    // ============================================================
    // START / STOP APP DETECTION
    // ============================================================
    /**
     * Bắt đầu đọc dữ liệu — gọi khi mở màn hình.
     *
     * Đọc tức thì từ 2 nguồn:
     *  - TCP: đọc `input: InputStream` trực tiếp
     *  - BLE: đọc từ ` ChannelManager.channel`
     *
     * Sau đó gửi lệnh `0xE1` để hỏi app hiện tại.
     */
    fun startAppDetection(state: HybridConnectionModel) {
        running = true

        val input = state.input
        if (input != null) {
            tcpReadJob = viewModelScope.launch(Dispatchers.IO) {
                val buffer = ByteArray(1024)
                while (isActive) {
                    if (!running) {
                        delay(100.milliseconds)
                        continue
                    }

                    val n = try {
                        input.read(buffer)
                    } catch (e: java.net.SocketTimeoutException) {
                        continue   // ← KHÔNG break, tiếp tục đọc
                    } catch (_: Exception) {
                        break
                    }
                    if (n <= 0) break
                    feedData(buffer, n)
                }
            }
        }

        // ---- BLE: đọc từ Channel ----
        if (state.isBluetooth) {
            bleReadJob = viewModelScope.launch(Dispatchers.IO) {
                try {
                    ChannelManager.channel.collect { data ->
                        feedData(data, data.size)
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    // ============================================================
    // DATA PARSER — dùng chung cho TCP + BLE
    // ============================================================
    /**
     * Parse dữ liệu thô — tìm `"app <name>\n"`.
     *
     * Tối ưu:
     *  - Quét 1 lần O(n)
     *  - Chỉ tạo substring khi chắc chắn là "app "
     *  - Giới hạn buffer 8KB tránh tràn RAM
     */
    private fun feedData(buffer: ByteArray, n: Int) {
        if (n <= 0) return
        val text = String(buffer, 0, n, Charsets.UTF_8)
        parseAppId(text)?.let {
            if (it.isNotEmpty()) {
                val type = mediaAppManager.detectAppType(
                    it,
                    ConstantsUtils.MEDIA_TYPE_YOUTUBE_WEB
                )
                onAppDetected(type)
            }
        }
    }


    private fun parseAppId(data: String): String? {
        val idx = data.indexOf(" ")
        if (idx == -1) return null
        return data.substring(idx + 1).trim()
    }


    /**
     * Dừng đọc — gọi khi rời màn hình.
     */
    fun stopAppDetection() {
        running = false
        tcpReadJob?.cancel()
        tcpReadJob = null
        bleReadJob?.cancel()
        bleReadJob = null
    }


    // ============================================================
    // CLEANUP
    // ============================================================
    override fun onCleared() {
        super.onCleared()
        stopAppDetection()
    }
}