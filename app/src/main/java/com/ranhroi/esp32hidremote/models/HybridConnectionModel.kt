package com.ranhroi.esp32hidremote.models

import com.ranhroi.esp32hidremote.ui.screens.server.send.HidOutput
import java.io.InputStream

data class HybridConnectionModel(
    var socket: Any? = null,
    var input: InputStream? = null,
    var hidOutput: HidOutput? = null,
    var currentMtu: Int = 23,
    val isConnected: Boolean = false,
    val isBluetooth: Boolean = false,
    val server: DeviceServerModel = DeviceServerModel(),
    var isNeedDevicePassword: Boolean = false,
    var isVerified: Boolean = false,
)