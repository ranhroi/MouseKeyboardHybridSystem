package com.ranhroi.esp32hidremote.models

import com.ranhroi.esp32hidremote.utils.ConstantsUtils

data class DeviceEndpoints(
    val userName: String = "",
    val mainService: String = "",  // BLE: Service UUID
    val writeChannel: String = "", // BLE: Write Char UUID
    val readChannel: String = "",   // BLE: Notify Char UUID
    val clientConfig: String = ConstantsUtils.DEFAULT_CLIENT_CONFIG_UUID,  // BLE: Client Config UUID
)
