package com.ranhroi.esp32hidremote.ui.screens.server

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import com.ranhroi.esp32hidremote.models.DeviceServerModel
import com.ranhroi.esp32hidremote.models.HybridConnectionModel
import com.ranhroi.esp32hidremote.ui.screens.server.send.BleHidOutput
import com.ranhroi.esp32hidremote.ui.screens.server.send.HidOutput
import com.ranhroi.esp32hidremote.ui.screens.server.send.TcpHidOutput
import com.ranhroi.esp32hidremote.utils.BluetoothPermissionHandler
import com.ranhroi.esp32hidremote.utils.ConstantsUtils.DEFAULT_CLIENT_CONFIG_UUID
import com.ranhroi.esp32hidremote.utils.ConstantsUtils.DEFAULT_ESP32S3_NAME
import com.ranhroi.esp32hidremote.utils.ConstantsUtils.DEFAULT_NOTIFY_CHAR_UUID
import com.ranhroi.esp32hidremote.utils.ConstantsUtils.DEFAULT_SERVICE_UUID
import com.ranhroi.esp32hidremote.utils.ConstantsUtils.DEFAULT_WRITE_CHAR_UUID
import com.ranhroi.esp32hidremote.utils.sendChannel
import com.ranhroi.esp32hidremote.utils.toBluetoothDevice
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

@SuppressLint("MissingPermission")
@Singleton
class ConnectionManager @Inject constructor(
    private val context: Context,
) {
    companion object {
        private const val TAG = "ConnectionManager"

        // ---- ROLE ----
        const val ROLE_WIFI: Byte = 0x01
        const val ROLE_BLUETOOTH: Byte = 0x02

        // ---- COMMANDS ----
        const val CMD_IDENTIFY_ROLE: Byte = 0xF1.toByte()
        const val CMD_AUTH_PASSWORD: Byte = 0xF3.toByte()
        const val CMD_PING: Byte = 0xF7.toByte()
        const val CMD_SWITCH_TO_WIFI: Byte = 0xFE.toByte()
        const val CMD_SWITCH_TO_BLE: Byte = 0xFF.toByte()
        const val CMD_SWITCH_TO_SETUP: Byte = 0xE0.toByte()

        // ---- RESPONSES ----
        const val REP_REQUIRE_AUTH: Byte = 0xF2.toByte()
        const val REP_AUTH_SUCCESS: Byte = 0xF4.toByte()
        const val REP_AUTH_FAILED: Byte = 0xF5.toByte()
        const val REP_PONG: Byte = 0xF8.toByte()
        const val REP_SWITCH_OK: Byte = 0xF9.toByte()
        const val REP_LED_STATUS: Byte = 0xBB.toByte()

        // ---- TIMEOUTS ----
        private const val BLE_CONNECT_TIMEOUT_MS = 12_000L
        private const val TCP_CONNECT_TIMEOUT_MS = 6_000L
        private const val HANDSHAKE_TIMEOUT_MS = 3_000L
        private const val AUTH_TIMEOUT_MS = 3_000L
        private const val PING_TIMEOUT_MS = 2_000L
    }

    // ============================================================
    // CONFIG
    // ============================================================
    private var serviceUuid: UUID = UUID.fromString(DEFAULT_SERVICE_UUID)
    private var writeCharUuid: UUID = UUID.fromString(DEFAULT_WRITE_CHAR_UUID)
    private var notifyCharUuid: UUID = UUID.fromString(DEFAULT_NOTIFY_CHAR_UUID)
    private var clientConfigUuid: UUID = UUID.fromString(DEFAULT_CLIENT_CONFIG_UUID)
    private var deviceName: String = DEFAULT_ESP32S3_NAME

    private var scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter: BluetoothAdapter? =
        bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
    private var listener: Listener? = null

    private var tcpCollectJob: Job? = null

    @Volatile
    private var state: HybridConnectionModel = HybridConnectionModel()

    // ============================================================
    // BLE STATE
    // ============================================================
    private var bluetoothGatt: BluetoothGatt? = null
    private var writeChar: BluetoothGattCharacteristic? = null
    private var notifyChar: BluetoothGattCharacteristic? = null

    private var bleConnectDeferred = CompletableDeferred<Boolean>()
    private var bleNotifyDeferred = CompletableDeferred<Boolean>()
    private var bleAuthDeferred = CompletableDeferred<Boolean>()
    private var blePongDeferred = CompletableDeferred<Boolean>()
    private var bleSwitchDeferred = CompletableDeferred<Boolean>()

    private var currentMtu: Int = 23

    @Volatile
    private var currentBleDevice: BluetoothDevice? = null

    // ============================================================
    // LISTENER
    // ============================================================
    interface Listener {
        fun onConnectResult(state: HybridConnectionModel)
        fun onVerifyResult(success: Boolean)
        fun onBytesReceived(bytes: ByteArray)
        fun onSwitchResult(success: Boolean) {}
    }

    // ============================================================
    // SET MANAGER
    // ============================================================
    fun setManager(scope: CoroutineScope) {
        this.scope = scope
    }

    fun registerListener(listener: Listener) {
        this.listener = listener
    }

    fun unregisterListener() {
        this.listener = null
    }

    fun getConnectionState(): HybridConnectionModel = state

    // ============================================================
    // CONNECT
    // ============================================================
    fun connect(server: DeviceServerModel) {
        scope.launch {
            close()
            delay(300.milliseconds)

            // ----  TÁCH BIỆT DỮ LIỆU ĐẦU VÀO GIỮA BLE VÀ WIFI (PC/MẠCH) ----
            if (server.bluetooth) {
                try {
                    serviceUuid =
                        UUID.fromString(server.endpoints.mainService.ifBlank { DEFAULT_SERVICE_UUID })
                    writeCharUuid =
                        UUID.fromString(server.endpoints.writeChannel.ifBlank { DEFAULT_WRITE_CHAR_UUID })
                    notifyCharUuid =
                        UUID.fromString(server.endpoints.readChannel.ifBlank { DEFAULT_NOTIFY_CHAR_UUID })
                    clientConfigUuid =
                        UUID.fromString(server.endpoints.clientConfig.ifBlank { DEFAULT_CLIENT_CONFIG_UUID })
                    deviceName = server.name.ifBlank { DEFAULT_ESP32S3_NAME }

                    Log.d(TAG, "==== CONNECTING TO BLE SERVER (isHID=${server.isHID}) ====")
                    Log.d(TAG, "Service: $serviceUuid | Write: $writeCharUuid")
                } catch (e: Exception) {
                    Log.e(TAG, "Lỗi phân tích cú pháp UUID, dùng mặc định: ${e.message}")
                    serviceUuid = UUID.fromString(DEFAULT_SERVICE_UUID)
                    writeCharUuid = UUID.fromString(DEFAULT_WRITE_CHAR_UUID)
                    notifyCharUuid = UUID.fromString(DEFAULT_NOTIFY_CHAR_UUID)
                    clientConfigUuid = UUID.fromString(DEFAULT_CLIENT_CONFIG_UUID)
                }
            } else {
                Log.d(TAG, "==== CONNECTING TO WIFI SERVER (isHID=${server.isHID}) ====")
                Log.d(TAG, "IP Address: ${server.address}, Port: ${server.port}")
            }

            val success = configConnect(server)
            if (!success) {
                listener?.onConnectResult(HybridConnectionModel())
                return@launch
            }

            // ---- ĐỒNG BỘ LOGIC XÁC THỰC SAU KHI KẾT NỐI VẬT LÝ THÀNH CÔNG ----
            when {
                state.isBluetooth -> {
                    Log.d(TAG, "connect: BLE handshake sent, waiting for REP_AUTH_SUCCESS...")
                    waitBleHandshakeAndVerify(server)
                }

                else -> {
                    if (state.isNeedDevicePassword && server.password.isNotEmpty()) {
                        val verifyOk = doVerifyTcp(state, server.password)
                        listener?.onVerifyResult(verifyOk)
                    }
                }
            }

            listener?.onConnectResult(state)
        }
    }


    private suspend fun waitBleHandshakeAndVerify(server: DeviceServerModel) {
        bleAuthDeferred = CompletableDeferred()
        val ok = withTimeoutOrNull(AUTH_TIMEOUT_MS.milliseconds) {
            bleAuthDeferred.await()
        } ?: false

        if (ok) {
            state.isVerified = true
            state.isNeedDevicePassword = false
        } else {
            if (server.password.isNotEmpty()) {
                verifyPassword(server.password)
            }
        }
    }

    // ============================================================
    // configConnect
    // ============================================================
    private suspend fun configConnect(server: DeviceServerModel): Boolean =
        withContext(Dispatchers.IO) {
            try {
                if (server.bluetooth) {
                    val device: BluetoothDevice =
                        server.toBluetoothDevice() ?: return@withContext false
                    connectBle(device)
                } else {
                    connectTcp(server)
                }
            } catch (e: Exception) {
                Log.e(TAG, "configConnect error: ${e.message}")
                false
            }
        }

    // ============================================================
    // GATT CALLBACK
    // ============================================================
    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.requestMtu(512)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                if (!bleConnectDeferred.isCompleted) bleConnectDeferred.complete(false)
                if (!bleNotifyDeferred.isCompleted) bleNotifyDeferred.complete(false)
                if (!bleAuthDeferred.isCompleted) bleAuthDeferred.complete(false)
                if (!blePongDeferred.isCompleted) blePongDeferred.complete(false)
                if (!bleSwitchDeferred.isCompleted) bleSwitchDeferred.complete(false)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            currentMtu = mtu
            gatt.discoverServices()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                bleConnectDeferred.complete(false)
                return
            }

            val service = gatt.getService(serviceUuid)
            if (service == null) {
                Log.e(TAG, "Service not found: $serviceUuid")
                bleConnectDeferred.complete(false)
                return
            }

            writeChar = service.getCharacteristic(writeCharUuid)
            notifyChar = service.getCharacteristic(notifyCharUuid)

            if (writeChar == null) {
                Log.e(TAG, "Write char not found: $writeCharUuid")
                bleConnectDeferred.complete(false)
                return
            }

            if (notifyChar != null) {
                gatt.setCharacteristicNotification(notifyChar, true)
                val desc = notifyChar!!.getDescriptor(clientConfigUuid)
                if (desc != null) {
                    desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(desc)
                } else {
                    bleConnectDeferred.complete(true)
                }
            } else {
                bleConnectDeferred.complete(true)
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            if (descriptor.uuid == clientConfigUuid) {
                bleNotifyDeferred.complete(status == BluetoothGatt.GATT_SUCCESS)
                bleConnectDeferred.complete(true)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.value ?: return
            handleBleNotification(data)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleBleNotification(value)
        }
    }

    // ============================================================
    // XỬ LÝ NOTIFY TỪ BLE — CHỈ GIỮ RESPONSE CẦN THIẾT
    // ============================================================
    private fun handleBleNotification(data: ByteArray) {
        if (data.isEmpty()) return

        val firstByte = data[0]

        when (firstByte) {
            REP_AUTH_SUCCESS -> {
                Log.d(TAG, "→ REP_AUTH_SUCCESS")
                state.isVerified = true
                state.isNeedDevicePassword = false
                if (!bleAuthDeferred.isCompleted) bleAuthDeferred.complete(true)
            }

            REP_AUTH_FAILED -> {
                Log.d(TAG, "→ REP_AUTH_FAILED")
                state.isVerified = false
                state.isNeedDevicePassword = true
                if (!bleAuthDeferred.isCompleted) bleAuthDeferred.complete(false)
            }

            REP_REQUIRE_AUTH -> {
                Log.d(TAG, "→ REP_REQUIRE_AUTH")
                state.isNeedDevicePassword = true
                state.isVerified = false
            }

            REP_PONG -> {
                Log.d(TAG, "→ REP_PONG")
                if (!blePongDeferred.isCompleted) blePongDeferred.complete(true)
            }

            REP_SWITCH_OK -> {
                Log.d(TAG, "→ REP_SWITCH_OK")
                if (!bleSwitchDeferred.isCompleted) bleSwitchDeferred.complete(true)
                listener?.onSwitchResult(true)
            }

            REP_LED_STATUS -> {
                if (data.size >= 4) {
                    listener?.onBytesReceived(data)
                }
            }

            else -> {
                scope.sendChannel(data)
                listener?.onBytesReceived(data)
            }
        }
    }

    // ============================================================
    // KẾT NỐI BLE
    // ============================================================
    private suspend fun connectBle(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        try {
            closeBle()

            currentBleDevice = device
            bleConnectDeferred = CompletableDeferred()
            bleNotifyDeferred = CompletableDeferred()
            bleAuthDeferred = CompletableDeferred()

            if (BluetoothPermissionHandler.checkSelfPermission(context)) {
                adapter?.cancelDiscovery()
            }

            bluetoothGatt = device.connectGatt(
                context, false, gattCallback, BluetoothDevice.TRANSPORT_LE
            )

            val success = withTimeoutOrNull(BLE_CONNECT_TIMEOUT_MS.milliseconds) {
                bleConnectDeferred.await()
            } ?: false

            if (!success || writeChar == null) {
                Log.e(TAG, "BLE connect failed")
                closeBle()
                return@withContext false
            }
            val hidOutput = BleHidOutput(bluetoothGatt!!, writeChar!!)

            state = HybridConnectionModel(
                isConnected = true,
                hidOutput = hidOutput,
                isBluetooth = true,
                currentMtu = currentMtu,
                server = DeviceServerModel(
                    bluetooth = true,
                    address = device.address,
                    name = device.name ?: "Unknown"
                )
            )

            val handshake = byteArrayOf(CMD_IDENTIFY_ROLE, ROLE_BLUETOOTH)
            hidOutput.write(handshake)

            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "connectBle error: ${e.message}")
            closeBle()
            return@withContext false
        }
    }

    // ============================================================
    // KẾT NỐI TCP
    // ============================================================
    private suspend fun connectTcp(server: DeviceServerModel): Boolean =
        withContext(Dispatchers.IO) {
            try {
                closeTcp()

                // ============================================================
                // BƯỚC 1: TẠO SOCKET + KẾT NỐI TCP
                // ============================================================
                val socket = Socket()
                socket.connect(
                    InetSocketAddress(server.address, server.port),
                    TCP_CONNECT_TIMEOUT_MS.toInt()
                )
                socket.keepAlive = true
                socket.tcpNoDelay = true
                socket.soTimeout = 5000   // Read timeout 5s

                val output = socket.getOutputStream()
                val ins = socket.getInputStream()

                // ============================================================
                // BƯỚC 2: GỬI HANDSHAKE — IDENTIFY ROLE
                // ============================================================
                // Gửi: [0xF1, 0x01] — IDENTIFY_ROLE với ROLE_WIFI
                val handshake = byteArrayOf(CMD_IDENTIFY_ROLE, ROLE_WIFI)
                output.write(handshake)
                output.flush()

                // ============================================================
                // BƯỚC 3: KHỞI TẠO STATE + HID OUTPUT
                // ============================================================
                val hidOutput = TcpHidOutput(socket, output)

                state = HybridConnectionModel(
                    isConnected = true,
                    socket = socket,
                    input = ins,
                    hidOutput = hidOutput,
                    isBluetooth = false,
                    server = server
                )

                // ============================================================
                // BƯỚC 4: ĐỌC RESPONSE TỪ SERVER
                // ============================================================
                when (readHandshakeResponse(ins, HANDSHAKE_TIMEOUT_MS)) {
                    REP_REQUIRE_AUTH -> {
                        state.isNeedDevicePassword = true
                        state.isVerified = false
                        Log.d(TAG, "Socket requires password")
                    }

                    REP_AUTH_SUCCESS -> {
                        state.isNeedDevicePassword = false
                        state.isVerified = true
                        subscribeToTcpUpdates(ins)
                        Log.d(TAG, "Socket ready")
                    }
                    REP_PONG -> {
                        listener?.onBytesReceived(byteArrayOf(REP_PONG))
                    }
                    null -> {
                        state.isNeedDevicePassword = false
                        state.isVerified = false
                        Log.w(TAG, "No handshake response")
                        closeTcp()
                        return@withContext false
                    }

                    else -> {
                        state.isNeedDevicePassword = false
                        state.isVerified = false
                        closeTcp()
                        return@withContext false
                    }
                }

                return@withContext true

            } catch (e: Exception) {
                Log.e(TAG, "connectTcp error: ${e.message}")
                closeTcp()
                return@withContext false
            }
        }


    // ============================================================
    // READ RESPONSE (dùng chung cho TCP)
    // ============================================================
    private suspend fun readHandshakeResponse(
        input: InputStream,
        timeoutMs: Long
    ): Byte? = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (input.available() > 0) return@withContext input.read().toByte()
            delay(20.milliseconds)
        }
        return@withContext null
    }

    // ============================================================
    // VERIFY PASSWORD
    // ============================================================
    suspend fun verifyPassword(password: String): Boolean = withContext(Dispatchers.IO) {
        val currentState = state

        if (currentState.isBluetooth) {
            bleAuthDeferred = CompletableDeferred()
            sendAuthPasswordBle(password)

            val ok = withTimeoutOrNull(AUTH_TIMEOUT_MS.milliseconds) {
                bleAuthDeferred.await()
            } ?: false

            listener?.onVerifyResult(ok)
            return@withContext ok
        } else {
            val ins = currentState.input ?: return@withContext false
            val output = currentState.hidOutput ?: return@withContext false
            val ok = doVerifyPassword(ins, output, password)
            listener?.onVerifyResult(ok)
            return@withContext ok
        }
    }


    private suspend fun doVerifyPassword(
        input: InputStream,
        output: HidOutput,
        password: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Bước 1: Encode mật khẩu thành UTF-8
            val passBytes = password.toByteArray(Charsets.UTF_8)

            // Firmware cfg_sys_password[32]: tối đa 31 byte
            if (passBytes.isEmpty() || passBytes.size > 31) {
                Log.w(TAG, "Invalid password length: ${passBytes.size} bytes")
                state.isVerified = false
                state.isNeedDevicePassword = true
                return@withContext false
            }

            // Bước 2: Tạo gói F3 + độ dài + mật khẩu
            val packet = ByteArray(2 + passBytes.size)
            packet[0] = CMD_AUTH_PASSWORD
            packet[1] = passBytes.size.toByte()
            System.arraycopy(passBytes, 0, packet, 2, passBytes.size)

            // Bước 3: Gửi yêu cầu xác thực
            output.write(packet)

            // Bước 4: Chờ phản hồi từ máy chủ
            when (val response = readHandshakeResponse(input, AUTH_TIMEOUT_MS)) {
                REP_AUTH_SUCCESS -> {
                    state.isNeedDevicePassword = false
                    state.isVerified = true
                    subscribeToTcpUpdates(input)
                    Log.d(TAG, "Password verification successful")
                    true
                }

                REP_AUTH_FAILED -> {
                    state.isNeedDevicePassword = true
                    state.isVerified = false
                    Log.w(TAG, "Password verification failed")
                    false
                }

                else -> {
                    state.isNeedDevicePassword = true
                    state.isVerified = false
                    Log.w(
                        TAG,
                        "Invalid or missing auth response: $response"
                    )
                    false
                }
            }
        } catch (e: Exception) {
            state.isNeedDevicePassword = true
            state.isVerified = false
            Log.e(TAG, "doVerifyPassword error", e)
            false
        }
    }


    private suspend fun doVerifyTcp(
        state: HybridConnectionModel,
        password: String
    ): Boolean = withContext(Dispatchers.IO) {
        val ins = state.input ?: return@withContext false
        val output = state.hidOutput ?: return@withContext false
        doVerifyPassword(ins, output, password)
    }

    private fun sendAuthPasswordBle(password: String) {
        try {
            val output = state.hidOutput ?: return
            val passBytes = password.toByteArray(Charsets.UTF_8)
            val packet = ByteArray(2 + passBytes.size)
            packet[0] = CMD_AUTH_PASSWORD
            packet[1] = passBytes.size.toByte()
            System.arraycopy(passBytes, 0, packet, 2, passBytes.size)
            output.write(packet)
            Log.d(TAG, "BLE auth password sent")
        } catch (e: Exception) {
            Log.e(TAG, "sendAuthPasswordBle error: ${e.message}")
        }
    }

    // ============================================================
    // SWITCH MODE
    // ============================================================
    suspend fun requestSwitchToWifi(): Boolean = withContext(Dispatchers.IO) {
        requestSwitchInternal(CMD_SWITCH_TO_WIFI)
    }

    suspend fun requestSwitchToBle(): Boolean = withContext(Dispatchers.IO) {
        requestSwitchInternal(CMD_SWITCH_TO_BLE)
    }

    suspend fun requestSwitchToSetup(): Boolean = withContext(Dispatchers.IO) {
        requestSwitchInternal(CMD_SWITCH_TO_SETUP)
    }

    private suspend fun requestSwitchInternal(cmd: Byte): Boolean = withContext(Dispatchers.IO) {
        try {
            val output = state.hidOutput ?: return@withContext false

            // Gửi lệnh
            output.write(byteArrayOf(cmd))
            Log.d(TAG, "Switch command sent: 0x${"%02X".format(cmd)}")

            // KHÔNG đợi response — vì ESP32 sẽ tắt kênh cũ
            // Chỉ cần gửi thành công là đủ
            delay(200.milliseconds)

            return@withContext true   // Luôn trả về true nếu gửi được

        } catch (e: Exception) {
            Log.e(TAG, "requestSwitch error: ${e.message}")
            return@withContext false
        }
    }

    // ============================================================
    // PING
    // ============================================================
    suspend fun ping(state: HybridConnectionModel): Boolean = withContext(Dispatchers.IO) {
        try {
            val output = state.hidOutput ?: return@withContext false

            if (state.isBluetooth) {
                blePongDeferred = CompletableDeferred()
                output.write(byteArrayOf(CMD_PING))

                val ok = withTimeoutOrNull(PING_TIMEOUT_MS.milliseconds) {
                    blePongDeferred.await()
                } ?: false

                return@withContext ok
            } else {
                return@withContext output.write(byteArrayOf(CMD_PING))
            }
        } catch (e: Exception) {
            return@withContext false
        }
    }

    // Gọi hàm này ngay sau khi xác thực thành công (REP_AUTH_SUCCESS)
    private fun subscribeToTcpUpdates(ins: InputStream) {
        tcpCollectJob?.cancel()
        tcpCollectJob = scope.launch {
            // Bắt buộc đưa timeout về 0 để không bị văng lỗi sau 5 giây im lặng
            (state.socket as? Socket)?.soTimeout = 0

            readTcpStream(ins).collect { data ->
                listener?.onBytesReceived(data)
            }
        }
    }

    fun readTcpStream(ins: InputStream): Flow<ByteArray> = flow {
        try {
            var firstByteInt = ins.read()
            // Hàm read() của Java sẽ BLOCK luồng (chờ dữ liệu ảo) chứ KHÔNG chạy liên tục đốt pin
            while (firstByteInt != -1) {
                val firstByte = firstByteInt.toByte()
                if (firstByte == REP_LED_STATUS) {
                    val remaining = ByteArray(3)
                    // Đọc nốt 3 byte còn lại
                    var bytesRead = 0
                    while (bytesRead < 3) {
                        val read = ins.read(remaining, bytesRead, 3 - bytesRead)
                        if (read == -1) break
                        bytesRead += read
                    }
                    emit(byteArrayOf(REP_LED_STATUS) + remaining)
                }
                firstByteInt = ins.read() // Chờ byte tiếp theo từ PC gửi xuống
            }
        } catch (e: Exception) {
            Log.e(TAG, "TCP Stream error: ${e.message}")
        }
    }.flowOn(Dispatchers.IO) // Ép buộc chạy hoàn toàn ở luồng chạy ngầm IO

    // ============================================================
    // CLOSE
    // ============================================================
    fun closeTcp() {
        try {
            state.input?.close()
            state.input = null
        } catch (e: Exception) {
        }
        try {
            state.hidOutput?.close()
            state.hidOutput = null
        } catch (e: Exception) {
        }
        try {
            (state.socket as? Socket)?.close()
            state.socket = null
        } catch (e: Exception) {
        }
        Log.d(TAG, "closeTcp: Đã giải phóng tài nguyên Wi-Fi Socket.")
    }

    fun closeBle() {
        try {
            bluetoothGatt?.disconnect(); bluetoothGatt?.close()
        } catch (e: Exception) {
        }
        bluetoothGatt = null
        writeChar = null
        notifyChar = null
        currentBleDevice = null

        if (!bleConnectDeferred.isCompleted) bleConnectDeferred.complete(false)
        if (!bleNotifyDeferred.isCompleted) bleNotifyDeferred.complete(false)
        if (!bleAuthDeferred.isCompleted) bleAuthDeferred.complete(false)
        if (!blePongDeferred.isCompleted) blePongDeferred.complete(false)
        if (!bleSwitchDeferred.isCompleted) bleSwitchDeferred.complete(false)

        bleConnectDeferred = CompletableDeferred()
        bleNotifyDeferred = CompletableDeferred()
        bleAuthDeferred = CompletableDeferred()
        blePongDeferred = CompletableDeferred()
        bleSwitchDeferred = CompletableDeferred()
        Log.d(TAG, "closeBle: Đã giải phóng và reset cờ BLE GATT.")
    }

    fun close() {
        closeTcp() // Gọi socket.close() để cắt đứt luồng dây mạng Wi-Fi
        closeBle() // Gọi gatt.disconnect() để cắt đứt sóng Bluetooth và gán biến chức năng về null

        tcpCollectJob?.cancel()
        tcpCollectJob = null

        // Bước 2: Reset toàn bộ hồ sơ biến trạng thái về trắng
        state = HybridConnectionModel()

        // Bước 3: Phát tín hiệu lên ViewModel để cập nhật giao diện người dùng (UI)
        listener?.onConnectResult(state)
        Log.d(TAG, "Hệ thống đã dọn sạch từ phần cứng cho đến bộ nhớ App.")
    }
}