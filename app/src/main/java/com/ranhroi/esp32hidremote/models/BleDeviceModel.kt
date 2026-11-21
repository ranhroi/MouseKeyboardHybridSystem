package com.ranhroi.esp32hidremote.models

import android.bluetooth.BluetoothDevice
import java.util.UUID

// ============================================================
// DATA MODEL
// ============================================================
data class BleDeviceModel(
    val name: String,
    val address: String,
    val rssi: Int,
    val device: BluetoothDevice,
    val serviceUuids: List<UUID> = emptyList(),
    val lastSeenMs: Long = System.currentTimeMillis()
)