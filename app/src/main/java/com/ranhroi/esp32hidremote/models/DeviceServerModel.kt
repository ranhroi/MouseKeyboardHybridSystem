package com.ranhroi.esp32hidremote.models

import com.ranhroi.esp32hidremote.utils.ConstantsUtils

data class DeviceServerModel(
    val id: Int = 0,
    val os: Int = ConstantsUtils.OS_INDEX_UNKNOW,
    val port: Int = 0,
    val name: String = "",
    val address: String = "",
    var password: String = "",
    val isHID: Boolean = true,
    val selected: Boolean = false,
    val bluetooth: Boolean = false,
    val rememberPass: Boolean = false,
    val endpoints: DeviceEndpoints = DeviceEndpoints(),
    val createdAt: Long = System.currentTimeMillis(),
)
