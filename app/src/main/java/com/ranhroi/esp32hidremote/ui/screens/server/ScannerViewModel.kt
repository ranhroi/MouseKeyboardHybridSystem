package com.ranhroi.esp32hidremote.ui.screens.server

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ranhroi.esp32hidremote.data.scanner.BleScanner
import com.ranhroi.esp32hidremote.data.scanner.TcpScanner
import com.ranhroi.esp32hidremote.models.BleDeviceModel
import com.ranhroi.esp32hidremote.models.CombineSettingModel
import com.ranhroi.esp32hidremote.models.DeviceServerModel
import com.ranhroi.esp32hidremote.utils.DataStoreConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ScannerViewModel"

@HiltViewModel
class ScannerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStoreConfig: DataStoreConfig,
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
    // BLE SCANNER
    // ============================================================
    private val bleScanner = BleScanner(context)

    private val _devicesBlt = MutableStateFlow<List<BleDeviceModel>>(emptyList())
    val devicesBlt = _devicesBlt.asStateFlow()

    private val _isLoadingBlt = MutableStateFlow(false)
    val isLoadingBlt = _isLoadingBlt.asStateFlow()

    // ============================================================
    // TCP SCANNER
    // ============================================================
    private var tcpScanner: TcpScanner? = null
    private var tcpScanJob: Job? = null

    private val _devicesTcp = MutableStateFlow<List<DeviceServerModel>>(emptyList())
    val devicesTcp = _devicesTcp.asStateFlow()

    private val _isLoadingTcp = MutableStateFlow(false)
    val isLoadingTcp = _isLoadingTcp.asStateFlow()

    // ============================================================
    // INIT
    // ============================================================
    init {
        observeConfig()
        setupBleScannerListener()
    }


    /**
     * Cập nhật serviceUuid + deviceName từ config khi thay đổi.
     */
    private fun observeConfig() {
        viewModelScope.launch {
            dataStoreConfig.combine.collect { config ->
                bleScanner.updateConfig(
                    serviceUuid = try {
                        java.util.UUID.fromString(config.serviceUuid)
                    } catch (e: Exception) {
                        null
                    },
                    deviceName = config.defaultEsp32s3Name
                )
            }
        }
    }


    // ============================================================
    // SAVE CONFIG — Gọi từ BleConfigDialog
    // ============================================================
    fun saveConfigBle(newConfig: CombineSettingModel) {
        viewModelScope.launch {
            dataStoreConfig.saveConfigBle(newConfig)
        }
    }


    private fun setupBleScannerListener() {
        bleScanner.setListener(object : BleScanner.ScanListener {

            override fun onDeviceFound(device: BleDeviceModel) {
                _devicesBlt.update { list ->
                    val newList = if (list.none { it.address == device.address }) {
                        list + device
                    } else {
                        list.map { if (it.address == device.address) device else it }
                    }
                    // ✅ Sort theo RSSI giảm dần
                    newList.sortedByDescending { it.rssi }
                }
            }

            override fun onDeviceUpdated(device: BleDeviceModel) {
                _devicesBlt.update { list ->
                    list.map { if (it.address == device.address) device else it }
                        .sortedByDescending { it.rssi }
                }
            }


            override fun onScanStarted() {
                Log.d(TAG, "✅ onScanStarted")
                _devicesBlt.value = emptyList()
                _isLoadingBlt.value = true
            }

            override fun onScanStopped() {
                Log.d(TAG, "🛑 onScanStopped")
                _isLoadingBlt.value = false
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "❌ onScanFailed: $errorCode")
                _isLoadingBlt.value = false
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun startScanBlt(
        filterServiceUuid: java.util.UUID? = null,
        targetName: String? = null,
        timeoutMs: Long = 15_000L
    ) {
        Log.d(TAG, "🚀 startScanBlt called")

        _devicesBlt.value = emptyList()
        _isLoadingBlt.value = true

        bleScanner.startScan(
            filterServiceUuid = filterServiceUuid,
            targetName = targetName,
            scanTimeoutMs = timeoutMs
        )
    }

    fun stopScanBlt() {
        bleScanner.stopScan()
        _isLoadingBlt.value = false
    }

    // ============================================================
    // TCP SCAN
    // ============================================================
    fun startScanTcp(port: Int) {
        stopScanTcp()
        _devicesTcp.value = emptyList()
        _isLoadingTcp.value = true

        tcpScanJob = viewModelScope.launch {
            tcpScanner = TcpScanner(context, port)
            tcpScanner?.scan()?.collect { device ->
                _devicesTcp.update { list ->
                    if (list.none { it.address == device.address && it.port == device.port }) {
                        list + device
                    } else {
                        list
                    }
                }
            }
            _isLoadingTcp.value = false
        }
    }

    fun stopScanTcp() {
        tcpScanJob?.cancel()
        tcpScanJob = null
        tcpScanner = null
        _isLoadingTcp.value = false
    }

    // ============================================================
    // CLEANUP
    // ============================================================
    override fun onCleared() {
        super.onCleared()
        stopScanTcp()
        stopScanBlt()
        bleScanner.close()
    }
}