package com.ranhroi.esp32hidremote.data.scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import com.ranhroi.esp32hidremote.models.BleDeviceModel
import com.ranhroi.esp32hidremote.utils.ConstantsUtils.DEFAULT_ESP32S3_NAME
import com.ranhroi.esp32hidremote.utils.ConstantsUtils.DEFAULT_SERVICE_UUID
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "BleScanner"

/**
 * Quản lý quét thiết bị BLE — GOM TỪ BleKeyboardBridge.
 *
 * Đặc điểm (giống hệt file cũ):
 *  - Scan filter RỖNG → nhận TẤT CẢ device BLE
 *  - Timeout 15s dùng Handler (main thread)
 *  - LUÔN thêm device vào danh sách (không lọc cứng)
 *  - Match UUID hoặc TÊN để auto-connect
 *  - Log chi tiết từng bước
 *  - Deduplicate theo address
 */
class BleScanner(private val context: Context) {

    companion object {
        private const val DEFAULT_SCAN_TIMEOUT_MS = 15_000L
    }

    // ============================================================
    // CONFIG — Service UUID mặc định (có thể override)
    // ============================================================
    var serviceUuid: UUID = UUID.fromString(DEFAULT_SERVICE_UUID)
    var deviceName: String = DEFAULT_ESP32S3_NAME

    // ============================================================
    // BLUETOOTH
    // ============================================================
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter: BluetoothAdapter? =
        bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()

    private var bleScanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null

    // Handler chạy timeout trên main thread (giống file cũ)
    private val mainHandler = Handler(Looper.getMainLooper())

    // ============================================================
    // STATE
    // ============================================================
    @Volatile
    var isScanning: Boolean = false
        private set

    /** Map address → BleDeviceModel để deduplicate */
    private val discoveredMap = ConcurrentHashMap<String, BleDeviceModel>()

    /** Snapshot danh sách thiết bị đã tìm thấy */
    val discoveredDevices: List<BleDeviceModel>
        get() = discoveredMap.values.toList()

    // ============================================================
    // CALLBACK
    // ============================================================
    interface ScanListener {
        fun onDeviceFound(device: BleDeviceModel)
        fun onDeviceUpdated(device: BleDeviceModel)
        fun onScanStarted()
        fun onScanStopped()
        fun onScanFailed(errorCode: Int)
    }

    private var listener: ScanListener? = null

    fun setListener(listener: ScanListener?) {
        this.listener = listener
    }

    fun updateConfig(serviceUuid: UUID?, deviceName: String?) {
        serviceUuid?.let { this.serviceUuid = it }
        deviceName?.let { this.deviceName = it }
        Log.d(TAG, "Config updated: serviceUuid=$serviceUuid, deviceName=$deviceName")
    }

    // ============================================================
    // TIMEOUT RUNNABLE (giống file cũ)
    // ============================================================
    private val scanTimeoutRunnable = Runnable {
        if (isScanning) {
            Log.d(TAG, "⏱️ Scan timeout reached — stopping")
            stopScan()
        }
    }

    // ============================================================
    // KIỂM TRA
    // ============================================================
    fun isBluetoothEnabled(): Boolean = adapter?.isEnabled == true

    // ============================================================
    // START SCAN — GIỐNG FILE CŨ
    // ============================================================
    @SuppressLint("MissingPermission")
    fun startScan(
        filterServiceUuid: UUID? = null,
        targetName: String? = null,
        scanTimeoutMs: Long = DEFAULT_SCAN_TIMEOUT_MS
    ) {
        Log.d(TAG, "═══════════════════════════════════════════")
        Log.d(TAG, "🚀 startScan() called")
        Log.d(TAG, "   filterServiceUuid = $filterServiceUuid")
        Log.d(TAG, "   targetName = $targetName")
        Log.d(TAG, "   scanTimeoutMs = $scanTimeoutMs")

        // 1. Kiểm tra Bluetooth
        if (!isBluetoothEnabled()) {
            Log.e(TAG, "❌ Bluetooth is OFF")
            listener?.onScanFailed(-1)
            return
        }
        Log.d(TAG, "✅ Bluetooth is ON")

        // 2. Nếu đang scan → dừng trước
        if (isScanning) {
            Log.d(TAG, "⚠️ Already scanning — stopping first")
            stopScan()
        }

        // 3. Lấy scanner
        bleScanner = adapter?.bluetoothLeScanner
        if (bleScanner == null) {
            Log.e(TAG, "❌ BluetoothLeScanner not available")
            listener?.onScanFailed(-3)
            return
        }
        Log.d(TAG, "✅ Got BluetoothLeScanner")

        // 4. Reset danh sách
        discoveredMap.clear()
        Log.d(TAG, "🗑️ Cleared discovered list")

        // 5. Cấu hình scan — GIỐNG FILE CŨ
        val scanFilter = ScanFilter.Builder().build()  // ← Filter RỖNG
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        Log.d(TAG, "⚙️ Scan config: filter=empty, mode=LOW_LATENCY")

        // 6. Callback
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                handleScanResult(result, filterServiceUuid, targetName)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                Log.d(TAG, "📦 Batch results: ${results.size}")
                for (result in results) {
                    handleScanResult(result, filterServiceUuid, targetName)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "❌ onScanFailed: $errorCode")
                isScanning = false
                mainHandler.removeCallbacks(scanTimeoutRunnable)
                listener?.onScanFailed(errorCode)
            }
        }

        // 7. Bắt đầu scan
        try {
            bleScanner?.startScan(listOf(scanFilter), settings, scanCallback)
            isScanning = true
            Log.d(TAG, "✅ bleScanner.startScan() OK")

            listener?.onScanStarted()
            Log.d(TAG, "✅ listener.onScanStarted() called")

            // 8. Timeout
            mainHandler.removeCallbacks(scanTimeoutRunnable)
            mainHandler.postDelayed(scanTimeoutRunnable, scanTimeoutMs)
            Log.d(TAG, "⏱️ Timeout scheduled: ${scanTimeoutMs}ms")

        } catch (e: Exception) {
            Log.e(TAG, "❌ startScan error: ${e.message}", e)
            isScanning = false
            listener?.onScanFailed(-4)
        }
        Log.d(TAG, "═══════════════════════════════════════════")
    }

    // ============================================================
    // XỬ LÝ KẾT QUẢ — GIỐNG FILE CŨ
    // ============================================================
    @SuppressLint("MissingPermission")
    private fun handleScanResult(
        result: ScanResult,
        filterServiceUuid: UUID?,
        targetName: String?
    ) {
        val device = result.device ?: return
        val address = device.address
        val name = try {
            device.name
                ?: result.scanRecord?.deviceName
                ?: "Unknown"
        } catch (e: SecurityException) {
            "Unknown"
        }
        val rssi = result.rssi
        val serviceUuids = result.scanRecord?.serviceUuids?.map { it.uuid } ?: emptyList()

        // ============================================================
        // ✅ LUÔN thêm device — KHÔNG lọc cứng (giống file cũ)
        // ============================================================
        val bleDeviceModel = BleDeviceModel(
            name = name,
            address = address,
            rssi = rssi,
            device = device,
            serviceUuids = serviceUuids,
            lastSeenMs = System.currentTimeMillis()
        )

        val existing = discoveredMap[address]
        if (existing == null) {
            discoveredMap[address] = bleDeviceModel
            Log.d(TAG, "🔵 NEW: $name ($address) RSSI=$rssi")
            listener?.onDeviceFound(bleDeviceModel)
        } else {
            discoveredMap[address] = bleDeviceModel
            Log.d(TAG, "🔄 UPDATE: $name ($address) RSSI=$rssi")
            listener?.onDeviceUpdated(bleDeviceModel)
        }

        // ============================================================
        // Match UUID hoặc TÊN — CHỈ LOG (không return sớm)
        // ============================================================
        val matchUuid = filterServiceUuid != null &&
                serviceUuids.any { it == filterServiceUuid }
        val matchName = targetName != null &&
                name.contains(targetName, ignoreCase = true)

        if (matchUuid || matchName) {
            Log.d(TAG, "🎯 MATCH: $name (uuid=$matchUuid, name=$matchName)")
        }
    }

    // ============================================================
    // STOP SCAN
    // ============================================================
    @SuppressLint("MissingPermission")
    fun stopScan() {
        Log.d(TAG, "🛑 stopScan() called")
        mainHandler.removeCallbacks(scanTimeoutRunnable)

        try {
            scanCallback?.let {
                bleScanner?.stopScan(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "stopScan error: ${e.message}")
        }

        scanCallback = null

        if (isScanning) {
            isScanning = false
            listener?.onScanStopped()
            Log.d(TAG, "✅ listener.onScanStopped() called")
        }
    }

    // ============================================================
    // CLEAR
    // ============================================================
    fun clearResults() {
        discoveredMap.clear()
    }

    // ============================================================
    // LẤY DANH SÁCH
    // ============================================================
    fun getDevicesSortedByRssi(): List<BleDeviceModel> =
        discoveredMap.values.sortedByDescending { it.rssi }

    fun getDeviceByAddress(address: String): BleDeviceModel? =
        discoveredMap[address]

    // ============================================================
    // CLOSE
    // ============================================================
    fun close() {
        stopScan()
        discoveredMap.clear()
        listener = null
        mainHandler.removeCallbacks(scanTimeoutRunnable)
    }
}