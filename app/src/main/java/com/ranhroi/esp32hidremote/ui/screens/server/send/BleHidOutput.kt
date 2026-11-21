package com.ranhroi.esp32hidremote.ui.screens.server.send

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothStatusCodes
import android.os.Build
import android.util.Log

private const val TAG = "BleHidOutput"

@SuppressLint("MissingPermission")
class BleHidOutput(
    private val gatt: BluetoothGatt,
    private val writeChar: BluetoothGattCharacteristic
) : HidOutput {

    @Volatile
    private var closed = false

    override val isConnected: Boolean
        get() = !closed

    override fun write(bytes: ByteArray): Boolean {
        if (closed) return false
        return try {
            writeChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(
                    writeChar,
                    bytes,
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                ) == BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                writeChar.value = bytes
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(writeChar)
            }
        } catch (e: Exception) {
            Log.e(TAG, "BLE write error: ${e.message}")
            closed = true
            false
        }
    }

    override fun close() {
        closed = true
        try { gatt.disconnect() } catch (_: Exception) {}
        try { gatt.close() } catch (_: Exception) {}
    }
}