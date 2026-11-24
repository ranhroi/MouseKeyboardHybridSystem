package com.ranhroi.esp32hidremote.utils

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.view.SoundEffectConstants
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.annotation.StringRes
import com.ranhroi.esp32hidremote.models.BleDeviceModel
import com.ranhroi.esp32hidremote.models.DeviceEndpoints
import com.ranhroi.esp32hidremote.models.DeviceServerModel
import com.ranhroi.esp32hidremote.models.KeyboardLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.Locale

// ============================================================
// TOAST EXTENSIONS
// ============================================================
fun Context.showToast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

fun Context.showToastLong(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}

fun Context.showToastSafe(@StringRes resId: Int, vararg formatArgs: Any?) {
    try {
        val message = getString(resId, *formatArgs)
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        // Bỏ qua
    }
}

fun Context.showToastSafe(message: String?) {
    if (!message.isNullOrEmpty()) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

fun CoroutineScope.sendChannel(event: ByteArray) {
    launch {
        ChannelManager.send(event)
    }
}


// ============================================================
// STRING EXTENSIONS
// ============================================================
fun Context.getStringSafe(resId: Int): String {
    return try {
        getString(resId)
    } catch (e: Exception) {
        ""
    }
}

fun Context.getStringSafe(@StringRes id: Int, vararg formatArgs: Any): String {
    return try {
        getString(id, *formatArgs)
    } catch (e: Exception) {
        ""
    }
}

// ============================================================
// VIEW EXTENSIONS
// ============================================================
fun View.playSoundClick() {
    this.playSoundEffect(SoundEffectConstants.CLICK)
}

// ============================================================
// UUID VALIDATION
// ============================================================
private val UUID_REGEX = Regex(
    "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
    RegexOption.IGNORE_CASE
)

fun String?.isValidUUID(): Boolean {
    if (this == null) return false
    val cleanInput = this.trim()
    return UUID_REGEX.matches(cleanInput)
}

// ==================== FORMAT ====================
fun String?.formatUuid(): String {
    if (this == null) return ""
    val cleaned = this.replace(Regex("[^0-9a-fA-F]"), "").take(32)
    val sb = StringBuilder()
    for (i in cleaned.indices) {
        if (i == 8 || i == 12 || i == 16 || i == 20) sb.append("-")
        sb.append(cleaned[i])
    }
    return sb.toString()
}

// ============================================================
// BLE DEVICE → DEVICE SERVER MODEL
// Dùng cho BLE scan results (từ BleScanner)
// ============================================================
@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun BleDeviceModel.toEntity(
    userName: String,
    userServiceUuid: String,
    userWriteUuid: String,
    userNotifyUuid: String,
    userClientUuid: String = ConstantsUtils.DEFAULT_CLIENT_CONFIG_UUID,
): DeviceServerModel {
    return DeviceServerModel(
        name = this.name.ifBlank { "ESP32 Device" },
        address = this.address,
        port = 0,
        bluetooth = true,
        endpoints = DeviceEndpoints(
            userName = userName.ifBlank { ConstantsUtils.DEFAULT_ESP32S3_NAME },
            mainService = userServiceUuid.ifBlank { ConstantsUtils.DEFAULT_SERVICE_UUID },
            writeChannel = userWriteUuid.ifBlank { ConstantsUtils.DEFAULT_WRITE_CHAR_UUID },
            readChannel = userNotifyUuid.ifBlank { ConstantsUtils.DEFAULT_NOTIFY_CHAR_UUID },
            clientConfig = userClientUuid.ifBlank { ConstantsUtils.DEFAULT_CLIENT_CONFIG_UUID },
        )
    )
}

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun List<BleDeviceModel>.toEntity(
    userName: String,
    userServiceUuid: String,
    userWriteUuid: String,
    userNotifyUuid: String,
    userClientUuid: String = ConstantsUtils.DEFAULT_CLIENT_CONFIG_UUID,
): List<DeviceServerModel> {
    return map {
        it.toEntity(userName, userServiceUuid, userWriteUuid, userNotifyUuid, userClientUuid)
    }
}

// ============================================================
// DEVICE SERVER MODEL → BLUETOOTH DEVICE
// ============================================================
@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun DeviceServerModel.toBluetoothDevice(): BluetoothDevice? {
    if (!bluetooth) return null
    return try {
        BluetoothAdapter.getDefaultAdapter()?.getRemoteDevice(address)
    } catch (e: Exception) {
        null
    }
}


/**
 * Kiểm tra thiết bị có đang kết nối AP của ESP32 không.
 *
 * Cách kiểm tra:
 *  1. Kết nối phải là Wi-Fi
 *  2. Wi-Fi phải KHÔNG có Internet (AP ESP32 không có Internet)
 *  3. SSID phải bắt đầu bằng "ESP32"
 */

@SuppressLint("MissingPermission", "ServiceCast")
fun Context.isConnectedToEsp32Ap(
    apSsidPrefix: String = "ESP32"
): Boolean {
    return try {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false

        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false

        if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return false

        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val hasValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        if (hasInternet && hasValidated) return false

        val ssid = getCurrentWifiSsid() ?: return false
        ssid.startsWith(apSsidPrefix, ignoreCase = true)
    } catch (e: Exception) {
        false
    }
}

@SuppressLint("MissingPermission", "ServiceCast")
fun Context.getCurrentWifiSsid(): String? {
    return try {
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return null
        val info = wm.connectionInfo ?: return null
        val ssid = info.ssid?.replace("\"", "")

        if (ssid.isNullOrBlank() || ssid == "<unknown ssid>") return null
        ssid
    } catch (e: Exception) {
        null
    }
}


// Giữ nguyên hàm mở rộng lấy ngôn ngữ chuẩn xác của bạn ở đây
@SuppressLint("ServiceCast")
fun Context.getCurrentKeyboardLanguage(): KeyboardLanguage? {
    val imm = getSystemService(
        Context.INPUT_METHOD_SERVICE
    ) as? InputMethodManager ?: return null

    val subtype = imm.currentInputMethodSubtype
        ?: return null

    val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        subtype.languageTag
    } else {
        @Suppress("DEPRECATION")
        subtype.locale.replace('_', '-')
    }

    if (tag.isBlank()) return null

    val locale = Locale.forLanguageTag(tag)

    if (locale.language.isBlank()) return null

    return KeyboardLanguage(
        languageTag = locale.toLanguageTag(),
        languageCode = locale.language,
        countryCode = locale.country
    )
}


fun Context.hasValidInternet(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}