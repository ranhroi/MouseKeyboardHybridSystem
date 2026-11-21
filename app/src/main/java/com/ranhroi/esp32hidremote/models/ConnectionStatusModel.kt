package com.ranhroi.esp32hidremote.models

/**
 * UI state cho màn hình kết nối.
 * Phản ánh chính xác 2 luồng dialog khác nhau:
 *  - showPassDeviceDialog: dialog nhập mật khẩu THIẾT BỊ (ESP32 gửi 0xF2)
 *  - showWifiConfigDialog: dialog nhập SSID + pass ROUTER (user chủ động)
 */
data class ConnectionStatusModel(
    // ============================================================
    // TRẠNG THÁI CHUNG
    // ============================================================
    val isConnected: Boolean = false,
    val isLoading: Boolean = false,
    var password: String = "",

    // ============================================================
    // DIALOG 1: Nhập mật khẩu THIẾT BỊ (ESP32 gửi 0xF2)
    // ============================================================
    val showPassDeviceDialog: Boolean = false,

    // ============================================================
    // DIALOG 2: Nhập SSID + mật khẩu ROUTER WI-FI (user chủ động)
    // ============================================================
    val showWifiConfigDialog: Boolean = false,
    val isPasswordError: Boolean = false,

    // ============================================================
    // DỮ LIỆU
    // ============================================================
    val servers: List<DeviceServerModel> = emptyList(),
    val state: HybridConnectionModel = HybridConnectionModel(),
    val statusMessage: String = "",
    val errorMessage: String? = null,

    // ============================================================
    // TRẠNG THÁI ĐÈN BÁO BÀN PHÍM (ĐỒNG BỘ 2 CHIỀU TỪ PC)
    // ============================================================
    val isNumLockOn: Boolean = false,
    val isCapsLockOn: Boolean = false,
    val isScrollLockOn: Boolean = false
)