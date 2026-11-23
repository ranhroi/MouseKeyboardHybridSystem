package com.ranhroi.esp32hidremote.ui.keyboards

import androidx.compose.runtime.compositionLocalOf
import com.ranhroi.esp32hidremote.models.KeyboardLanguage

/**
 * ============================================================
 * KeyboardEnvironment — CompositionLocal cho bàn phím & D-pad
 * ============================================================
 *
 * Tất cả giá trị được cung cấp ở MainScreen và lan xuống toàn bộ
 * cây con. Mỗi Local đều có default value → không crash nếu quên
 * bọc provider.
 *
 * ============================================================
 * DANH SÁCH LOCAL (20 biến)
 * ============================================================
 *
 * ── NHÓM 1: SERVER & KẾT NỐI (3) ──
 * 1.  LocalIsHID              — true: bo mạch ESP32, false: app PC
 * 2.  LocalIsConnected        — trạng thái kết nối
 * 3.  LocalIsLoading          — đang kết nối/xử lý
 *
 * ── NHÓM 2: LED STATUS (3) ──
 * 4.  LocalIsCapsLockOn       — đèn Caps Lock
 * 5.  LocalIsNumLockOn        — đèn Num Lock
 * 6.  LocalIsScrollLockOn     — đèn Scroll Lock
 *
 * ── NHÓM 3: CẤU HÌNH BÀN PHÍM (8) ──
 * 7.  LocalIsRotation         — bàn phím xoay ngang
 * 8.  LocalIsLeftHand         — chế độ tay trái
 * 9.  LocalOsIndex            — OS index (0=Windows, 1=macOS, ...)
 * 10. LocalIsFloating         — floating hotkeys
 * 11. LocalIsAutoPing         — auto ping
 * 12. LocalIsScrollDirection  — hướng scroll
 * 13. LocalIsButtonText       — hiển thị text trên nút
 * 14. LocalLanguageCode       — mã ngôn ngữ bàn phím
 *
 * ── NHÓM 4: DIALOG (2) ──
 * 15. LocalShowPassDeviceDialog — dialog nhập mật khẩu thiết bị
 * 16. LocalIsPasswordError      — mật khẩu sai
 *
 * ── NHÓM 5: TỐC ĐỘ & DELAY (2) ──
 * 17. LocalSpeedPressed       — tốc độ auto-repeat (ms)
 * 18. LocalDelayText          — delay khi gõ text (ms)
 *
 * ── NHÓM 6: CẤU HÌNH ĐÃ LƯU (2) ──
 * 19. LocalProgress           — progress text input (chỉ từ DataStore)
 * 20. LocalScroll             — scroll speed (chỉ từ DataStore)
 *
 * ============================================================
 * QUY TẮC THÊM BIẾN MỚI
 * ============================================================
 * Chỉ thêm khi thỏa CẢ 4 điều kiện:
 *   1. Kiểu đơn giản (Boolean, Int, String ngắn, enum)
 *   2. Ít thay đổi (vài giây, phút, giờ — không phải mỗi frame)
 *   3. Nhiều nơi dùng (>= 2-3 Composable)
 *   4. Là val (không phải var)
 *
 * KHÔNG thêm:
 *   - statusMessage (đổi mỗi ping)
 *   - errorMessage (String?)
 *   - state (object lớn)
 *   - servers (List)
 *   - password (var)
 *   - serviceUuid, writeCharUuid, notifyCharUuid (String dài)
 *   - defaultEsp32s3Name (không cần cho UI)
 */

// ============================================================
// NHÓM 1: SERVER & KẾT NỐI
// ============================================================

/**
 * true  : server là bo mạch ESP32 (isHID = true)
 * false : server là app PC (isHID = false)
 *
 * Dùng để phân nhánh cơ chế gửi phím Lock:
 *   - Bo mạch  : DOWN/UP (ESP32 tự gửi LED qua usbEventCallback)
 *   - App PC   : TAP (server PC chỉ nhận diện KEY_TAP 0x04)
 */

val LocalIsWifiOn = compositionLocalOf { false }

val LocalIsBluetoothOn = compositionLocalOf { false }

val LocalIsHID = compositionLocalOf { false }

val LocalIsBluetooth = compositionLocalOf { true }

// ============================================================
// NHÓM 2: LED STATUS
// ============================================================

/**
 * Đèn Caps Lock đang bật.
 * Tần suất đổi: mỗi lần user bấm Caps Lock.
 */
val LocalIsCapsLockOn = compositionLocalOf { false }

/**
 * Đèn Num Lock đang bật.
 */
val LocalIsNumLockOn = compositionLocalOf { false }

/**
 * Đèn Scroll Lock đang bật.
 */
val LocalIsScrollLockOn = compositionLocalOf { false }

// ============================================================
// NHÓM 3: CẤU HÌNH BÀN PHÍM
// ============================================================

/**
 * Bàn phím đang ở chế độ xoay ngang.
 * Tần suất đổi: khi user xoay màn hình.
 */
val LocalIsRotation = compositionLocalOf { true }

/**
 * Chế độ tay trái (đảo trái/phải).
 * Tần suất đổi: khi user đổi trong cài đặt.
 */
val LocalIsLeftHand = compositionLocalOf { false }

/**
 * OS index (0 = Windows, 1 = macOS, 2 = Linux, ...).
 * Tần suất đổi: khi user đổi OS.
 */
val LocalOsIndex = compositionLocalOf { 0 }

/**
 * Floating hotkeys đang bật.
 * Tần suất đổi: khi user đổi trong cài đặt.
 */
val LocalIsFloating = compositionLocalOf { true }

/**
 * Auto ping đang bật.
 * Tần suất đổi: khi user đổi trong cài đặt.
 */
val LocalIsAutoPing = compositionLocalOf { false }

/**
 * Hướng scroll (true = ngược, false = xuôi).
 * Tần suất đổi: khi user đổi trong cài đặt.
 */
val LocalIsScrollDirection = compositionLocalOf { false }

/**
 * Hiển thị text trên nút.
 * Tần suất đổi: khi user đổi trong cài đặt.
 */
val LocalIsButtonText = compositionLocalOf { true }

/**
 * Mã ngôn ngữ bàn phím ("en", "vi", ...).
 * Tần suất đổi: khi user đổi ngôn ngữ.
 */
val LocalKeyboardLanguage = compositionLocalOf { KeyboardLanguage() }

// ============================================================
// NHÓM 5: TỐC ĐỘ & DELAY
// ============================================================

/**
 * Tốc độ auto-repeat khi giữ phím (ms).
 * Mặc định 25ms.
 */
val LocalSpeedPressed = compositionLocalOf { 25L }

/**
 * Delay khi gõ text (ms).
 * Mặc định 5ms.
 */
val LocalDelayText = compositionLocalOf { 5L }
