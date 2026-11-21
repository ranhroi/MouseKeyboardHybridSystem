# Mô hình Kết nối Hệ thống (Hybrid Mouse & Keyboard Control)

Sơ đồ dưới đây mô tả luồng giao tiếp hai chiều giữa **Ứng dụng Android** với **Bo mạch ESP32-S3** và **App Server trên Máy tính** qua hai kênh **Bluetooth (BLE)** và **Wi-Fi (TCP)**.

```text
                                 +-----------------------+

                                 |   ỨNG DỤNG ANDROID    |
                                 |    (Hybrid Remote)    |
                                 +-----------+-----------+
                                             |
                      +----------------------+----------------------+

                      | (Gửi lệnh điều khiển & Nhận trạng thái LED) |
                      v                                             v
        +---------------------------+                 +---------------------------+

        |       BLUETOOTH BLE       |                 |       WI-FI NỘI BỘ        |
        |   (GATT Write / Notify)   |                 |       (TCP Socket)        |
        +-------------+-------------+                 +-------------+-------------+

                      |                                             |
          +-----------+-----------+                     +-----------+-----------+

          | (Không dây)           | (Không dây)         | (Qua Router)          | (Qua Router)
          v                       v                     v                       v
+-------------------+   +-------------------+ +-------------------+   +-------------------+

| BO MẠCH ESP32-S3  |   |  APP PC SERVER    | | BO MẠCH ESP32-S3  |   |  APP PC SERVER    |
|  (Cắm cổng USB)   |   | (Win / Mac / Lin) | |  (Cắm cổng USB)   |   | (Win / Mac / Lin) |
+---------+---------+   +---------+---------+ +---------+---------+   +---------+---------+

          |                       |                     |                       |
          | USB HID               | System API          | USB HID               | System API
          | (Phần cứng)           | (SendInput)         | (Phần cứng)           | (SendInput)
          +-----------+-----------+                     +-----------+-----------+

                      |                                             |
                      v                                             v
              +---------------+                             +---------------+

              |   MÁY TÍNH    |                             |   MÁY TÍNH    |
              | (PC / Laptop) |                             | (PC / Laptop) |
              +---------------+                             +---------------+
```

## Tóm tắt luồng hoạt động

* **Chế độ Phần cứng (qua Bo mạch ESP32-S3):** 
  * Ứng dụng Android gửi gói tin qua **Bluetooth BLE** hoặc **Wi-Fi TCP** tới ESP32-S3.
  * Bo mạch xuất tín hiệu **USB HID vật lý** trực tiếp vào máy tính (hoạt động cả trong BIOS, màn hình khóa `Win + L`) và đẩy ngược trạng thái 3 đèn LED (`Caps Lock`, `Num Lock`, `Scroll Lock`) về điện thoại.
* **Chế độ Phần mềm (qua App PC Server):** 
  * Ứng dụng Android kết nối trực tiếp tới phần mềm chạy nền trên máy tính qua **Bluetooth BLE** hoặc **Wi-Fi TCP**.
  * Phần mềm dùng API hệ điều hành (`SendInput` trên Windows) để điều khiển chuột, bàn phím, gõ ký tự Unicode/Emoji và đồng bộ đèn LED theo thời gian thực.
