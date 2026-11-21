## Sơ đồ Luồng Xử lý trên Máy Tính (PC Server)

Sơ đồ dưới đây mô tả quá trình **App Server trên Máy tính** nhận dữ liệu điều khiển từ điện thoại qua **Bluetooth (BLE)** hoặc **Wi-Fi (TCP)**, thực thi vào hệ điều hành và phản hồi trạng thái ngược lại.

```text
                                 +-----------------------+

                                 |   MÁY KHÁCH ANDROID   |
                                 |    (Hybrid Remote)    |
                                 +-----------+-----------+
                                             |
                      +----------------------+----------------------+

                      | (Gửi gói lệnh V2/UTF-8 & Nhận trạng thái)   |
                      v                                             v
        +---------------------------+                 +---------------------------+

        |       BLUETOOTH BLE       |                 |       WI-FI NỘI BỘ        |
        |   (Windows GATT Server)   |                 |    (Winsock TCP Server)   |
        +-------------+-------------+                 +-------------+-------------+

                      |                                             |
                      +----------------------+----------------------+
                                             |
                                             v
                                 +-----------------------+

                                 |   BỘ ĐIỀU PHỐI LÕI    |
                                 |   (ServerCore.cpp)    |
                                 +-----------+-----------+
                                             |
          +----------------------------------+----------------------------------+

          | (Phân tích & Thực thi)                                              | (Đọc trạng thái đèn)
          v                                                                     v
+-------------------+                                                 +-------------------+

|   XỬ LÝ NHẬP LIỆU |                                                 |  ĐỒNG BỘ ĐÈN LED  |
| (WindowsInput.cpp)|                                                 |   (GetKeyState)   |
+---------+---------+                                                 +---------+---------+

          |                                                                     |
          | Gọi Windows API                                                     | Đóng gói 0xBB
          | (SendInput / LockWorkStation)                                       | [Num, Caps, Scroll]
          v                                                                     |
+-------------------+                                                           |

| HỆ ĐIỀU HÀNH PC   | ----------------------------------------------------------+
| (Windows Desktop) |      (Tự động báo trạng thái đèn sau khi bấm phím)
+-------------------+
```

### Tóm tắt luồng hoạt động trên PC Server
1. **Tiếp nhận không dây:** Máy tính mở song song hoặc chuyển đổi linh hoạt giữa **Bluetooth GATT Server** và **TCP Socket Server** (cổng `2000`) để đón gói tin từ điện thoại.
2. **Điều phối trung tâm (`ServerCore.cpp`):** Xác thực mật khẩu (`0xF3`), kiểm tra trạng thái mạng/Bluetooth tự động dự phòng (Fallback) và phân loại khung dữ liệu.
3. **Thực thi hệ thống (`WindowsInput.cpp`):** Chuyển đổi mã HID thành sự kiện chuột/bàn phím (`SendInput`), gõ văn bản Unicode/Emoji (`KEYEVENTF_UNICODE`) hoặc khóa màn hình (`LockWorkStation` cho `Win + L`).
4. **Phản hồi thời gian thực:** Ngay khi kết nối xong hoặc ngay sau khi thực thi các phím khóa (`Caps Lock`, `Num Lock`, `Scroll Lock`), hệ thống đọc trạng thái thật từ Windows và bắn gói `0xBB` ngược về điện thoại.
