package com.ranhroi.esp32hidremote.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ranhroi.esp32hidremote.R
import com.ranhroi.esp32hidremote.models.BottomNavModel
import com.ranhroi.esp32hidremote.models.LanguageCodeModel
import com.ranhroi.esp32hidremote.models.OsOptionModel

object ConstantsUtils {
    val bottomNavRepository: List<BottomNavModel> by lazy {
        listOf(
            BottomNavModel(
                nameRes = R.string.bottom_nav_menu,
                icon = "baseline_menu"
            ),
            BottomNavModel(
                nameRes = R.string.bottom_nav_shortcut,
                icon = "baseline_shortcut"
            ),
            BottomNavModel(
                nameRes = R.string.bottom_nav_keyboard,
                icon = "baseline_keyboard_hide"
            ),
            BottomNavModel(
                nameRes = R.string.bottom_nav_media,
                icon = "baseline_music_note"
            ),
            BottomNavModel(
                nameRes = R.string.bottom_nav_browser,
                icon = "outline_browse"
            ),
            BottomNavModel(
                nameRes = R.string.bottom_nav_power,
                icon = "baseline_power_setting_new"
            ),
        )
    }

    val osOptionsList = listOf(
        OsOptionModel(OS_INDEX_WINDOWS, R.string.os_windows, "🪟"),
        OsOptionModel(OS_INDEX_MAC, R.string.os_mac, "🍎"),
        OsOptionModel(OS_INDEX_LINUX, R.string.os_linux, "🐧"),
        OsOptionModel(OS_INDEX_UBUNTU, R.string.os_ubuntu, "🟠")
    )

    /**
     * Lấy tên hiển thị của OS theo index.
     */
    @Composable
    fun getOsLabel(index: Int): String {
        val option = osOptionsList.find { it.index == index }
        return if (option != null) {
            "${option.flag} ${stringResource(option.labelRes)}"
        } else {
            "❓ Unknown"
        }
    }

    // DataStore key
    val OS_INDEX = intPreferencesKey("os_index")

    // ==================== LANGUAGE ====================
    val languageOptionsList = listOf(
        LanguageCodeModel("en", R.string.language_english, "🇬🇧"),
        LanguageCodeModel("vi", R.string.language_vietnamese, "🇻🇳"),
    )

    @Composable
    fun getDisplayLabel(code: String): String = when {
        code == "auto" -> "🌐 ${stringResource(R.string.language_auto)}"
        else -> languageOptionsList.find { it.code == code }
            ?.let { "${it.flag} ${stringResource(it.labelRes)}" }
            ?: "🌐 ${stringResource(R.string.language_auto)}"
    }


    // Server
    const val OS_INDEX_UNKNOW = -1
    const val OS_INDEX_WINDOWS = 0
    const val OS_INDEX_MAC = 1
    const val OS_INDEX_LINUX = 2
    const val OS_INDEX_UBUNTU = 3


    const val PORT_CONNECTION = 1989

    val DEVICE_LIST_KEY = stringPreferencesKey("device_list")
    val KEY_PORT_CONNECTION = intPreferencesKey("PORT_CONNECTION")

    // Language
    val KEY_LANGUAGE = stringPreferencesKey("selected_language_display")
    val KEY_LANGUAGE_MODE = stringPreferencesKey("selected_language_mode")
    const val DEFAULT_LANGUAGE = "vi"

    // Bluetooth ESP32S3    // ============================================================
    // UUID & DEVICE NAME — Preferences Keys
    // ============================================================
    val SERVICE_UUID = stringPreferencesKey("service_uuid")
    val WRITE_CHAR_UUID = stringPreferencesKey("write_char_uuid")
    val NOTIFY_CHAR_UUID = stringPreferencesKey("notify_char_uuid")   // 🆕
    val ESP32S3_NAME = stringPreferencesKey("default_esp32s3_name")

    // ============================================================
    // UUID & DEVICE NAME — Default Values
    // Đồng bộ với firmware v3.0:
    //   Service UUID:       2D2A0001-8A5A-4E76-A2E3-1E57D9A1B001
    //   Write Char UUID:    2D2A0002-8A5A-4E76-A2E3-1E57D9A1B001
    //   Notify Char UUID:   2D2A0003-8A5A-4E76-A2E3-1E57D9A1B001
    //   Client Config UUID: 00002902-0000-1000-8000-00805f9b34fb
    // ============================================================
    const val DEFAULT_SERVICE_UUID = "2D2A0001-8A5A-4E76-A2E3-1E57D9A1B001"
    const val DEFAULT_WRITE_CHAR_UUID = "2D2A0002-8A5A-4E76-A2E3-1E57D9A1B001"
    const val DEFAULT_NOTIFY_CHAR_UUID = "2D2A0003-8A5A-4E76-A2E3-1E57D9A1B001"
    const val DEFAULT_CLIENT_CONFIG_UUID = "00002902-0000-1000-8000-00805f9b34fb"
    const val DEFAULT_ESP32S3_NAME = "ESP32S3_HID_REMOTE"

    // Cursor
    val CURSOR_PROGRESS = intPreferencesKey("CURSOR_PROGRESS")
    val CURSOR_SCROLL = intPreferencesKey("CURSOR_SCROLL")
    val IS_SCROLL_DIRECTION = booleanPreferencesKey("IS_SCROLL_DIRECTION")
    val SPEED_PRESSED = longPreferencesKey("SPEED_PRESSED")
    val DELAY_TEXT = longPreferencesKey("DELAY_TEXT")
    val LEFT_HAND = booleanPreferencesKey("LEFT_HAND")

    val AUTO_PING = booleanPreferencesKey("AUTO_PING")

    val SHOW_ROTATION = booleanPreferencesKey("SHOW_ROTATION")
    val SHOW_FLOATING = booleanPreferencesKey("SHOW_FLOATING")
    val SHOW_BUTTON_TEXT = booleanPreferencesKey("SHOW_BUTTON_TEXT")

    val SHORTKEY_LIST_KEY = stringPreferencesKey("shortcut_list")
    val QUICK_KEY_LIST_KEY = stringPreferencesKey("quickkey_list")

    val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
    val TYPE_MEDIA = stringPreferencesKey("type_media")
    val SELECTED_PRESET_ID = stringPreferencesKey("selected_preset_id")

    val IMPORTED_PRESETS = stringPreferencesKey("imported_presets")

    // ==================== MEDIA TYPES ====================

    // Native Apps (1-99)
    const val MEDIA_TYPE_VLC = 2
    const val MEDIA_TYPE_SPOTIFY = 3
    const val MEDIA_TYPE_MPLAYERX = 4
    const val MEDIA_TYPE_WINDOWSMEDIAPLAYER = 5
    const val MEDIA_TYPE_ITUNES = 6
    const val MEDIA_TYPE_POTPLAYER = 7
    const val MEDIA_TYPE_YOUTUBE_WEB = 100
    const val MEDIA_TYPE_YOUTUBE_MUSIC_WEB = 119

    // ==================== CUSTOM MAP ====================
    val customMap: MutableMap<String, Int> = mutableMapOf()
}