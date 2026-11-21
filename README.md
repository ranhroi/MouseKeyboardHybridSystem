
## 🏗️ Kiến trúc hệ thống

**App Android (`MousseKeyboardHybridSystem`)** đóng vai trò là trung tâm điều khiển chính. Người dùng có thể tùy chọn kết nối trực tiếp tới **Bo mạch ESP32-S3** (hoạt động như thiết bị phần cứng cắm ngoài qua USB) hoặc kết nối thẳng tới **Phần mềm Server trên PC** (không cần phần cứng) với định dạng gói tin điều khiển tương đương nhau:

```text
                                 ┌──(Wi-Fi / BLE)──► [ 🔌 ESP32-S3 HID ] ──(USB HID)──► [ 💻 PC (Không cần cài app) ]
                                 │
[ 📱 App Android (Trung tâm) ] ──┤  (Người dùng tùy chọn 1 trong 2 chế độ kết nối)
                                 │
                                 └──(Wi-Fi / LAN)──► [ 🖥️ HybridMouseControlServer ] ──► [ 💻 PC (Cài phần mềm) ]
```

### 🔄 Hai chế độ hoạt động linh hoạt
1. **Chế độ Phần cứng (Qua ESP32-S3):** App Android gửi lệnh qua Wi-Fi/BLE tới bo mạch ESP32-S3. Bo mạch giả lập chuột/bàn phím vật lý (USB HID) cắm vào máy tính (hoạt động cả trong BIOS, màn hình khóa hoặc máy tính không cho cài phần mềm lạ).
2. **Chế độ Phần mềm (Qua Windows Server):** App Android gửi trực tiếp gói tin tương tự qua mạng LAN/Wi-Fi tới phần mềm `HybridMouseControlServer` cài sẵn trên PC để điều khiển chuột/bàn phím (không cần mang theo bo mạch).
