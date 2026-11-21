package com.ranhroi.esp32hidremote.utils

import com.ranhroi.esp32hidremote.models.CombineSettingModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreConfig @Inject constructor(
    private val dataStore: DataStoreManager
) {

    val combine: Flow<CombineSettingModel> =
        dataStore.data.map { pref ->
            CombineSettingModel(
                progress = pref[ConstantsUtils.CURSOR_PROGRESS] ?: 50,
                scroll = pref[ConstantsUtils.CURSOR_SCROLL] ?: 50,
                isScrollDirection = pref[ConstantsUtils.IS_SCROLL_DIRECTION] ?: false,
                speedPressed = pref[ConstantsUtils.SPEED_PRESSED] ?: 25L,
                delayText = pref[ConstantsUtils.DELAY_TEXT] ?: 5L,
                osIndex = pref[ConstantsUtils.OS_INDEX] ?: ConstantsUtils.OS_INDEX_WINDOWS,
                isAutoPing = pref[ConstantsUtils.AUTO_PING] ?: false,
                isLeftHand = pref[ConstantsUtils.LEFT_HAND] ?: false,
                isRotation = pref[ConstantsUtils.SHOW_ROTATION] ?: true,
                isFloating = pref[ConstantsUtils.SHOW_FLOATING] ?: true,
                isButtonText = pref[ConstantsUtils.SHOW_BUTTON_TEXT] ?: true,
                languageCode = pref[ConstantsUtils.KEY_LANGUAGE] ?: ConstantsUtils.DEFAULT_LANGUAGE,
                serviceUuid = pref[ConstantsUtils.SERVICE_UUID]
                    ?: ConstantsUtils.DEFAULT_SERVICE_UUID,
                writeCharUuid = pref[ConstantsUtils.WRITE_CHAR_UUID]
                    ?: ConstantsUtils.DEFAULT_WRITE_CHAR_UUID,
                notifyCharUuid = pref[ConstantsUtils.NOTIFY_CHAR_UUID]
                    ?: ConstantsUtils.DEFAULT_NOTIFY_CHAR_UUID,
                defaultEsp32s3Name = pref[ConstantsUtils.ESP32S3_NAME]
                    ?: ConstantsUtils.DEFAULT_ESP32S3_NAME,
            )
        }

    suspend fun saveConfigBle(config: CombineSettingModel) {
        runCatching {
            dataStore.put(ConstantsUtils.SERVICE_UUID, config.serviceUuid)
            dataStore.put(ConstantsUtils.WRITE_CHAR_UUID, config.writeCharUuid)
            dataStore.put(ConstantsUtils.NOTIFY_CHAR_UUID, config.notifyCharUuid)
            dataStore.put(ConstantsUtils.ESP32S3_NAME, config.defaultEsp32s3Name)
        }.onSuccess {
        }
    }

    // Thêm function để update language riêng
    suspend fun updateLanguage(languageCode: String) {
        dataStore.put(ConstantsUtils.KEY_LANGUAGE, languageCode)
    }
}
