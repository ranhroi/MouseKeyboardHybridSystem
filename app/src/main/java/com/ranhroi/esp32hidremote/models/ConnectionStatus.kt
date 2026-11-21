package com.ranhroi.esp32hidremote.models

enum class ConnectionStatus {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
    ERROR,

    // ============================================================
    // TRẠNG THÁI MỚI — Phản ánh dialog đang chờ user
    // ============================================================
    NEED_DEVICE_PASSWORD,   // ESP32 gửi 0xF2 → chờ nhập pass thiết bị
    VERIFYING,             // Đang verify mật khẩu
    NO_NETWORK
}