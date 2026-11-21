package com.ranhroi.esp32hidremote.models

import com.ranhroi.esp32hidremote.utils.ConstantsUtils

data class CombineSettingModel(
    val progress: Int = 50,
    val scroll: Int = 50,
    val isScrollDirection: Boolean = false,
    val speedPressed: Long = 25L,
    val delayText: Long = 5L,
    val osIndex: Int = 0,
    val isAutoPing: Boolean = false,
    val isLeftHand: Boolean = false,
    val isRotation: Boolean = true,
    val isFloating: Boolean = true,
    val isButtonText: Boolean = true,
    val languageCode: String = "en",
    val serviceUuid: String = ConstantsUtils.DEFAULT_SERVICE_UUID,
    val writeCharUuid: String = ConstantsUtils.DEFAULT_WRITE_CHAR_UUID,
    val notifyCharUuid: String = ConstantsUtils.DEFAULT_NOTIFY_CHAR_UUID,
    val defaultEsp32s3Name: String = ConstantsUtils.DEFAULT_ESP32S3_NAME,
)
