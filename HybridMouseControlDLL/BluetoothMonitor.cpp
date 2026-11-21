/* BluetoothMonitor.cpp
 *
 * Theo dõi trạng thái Bluetooth radio của PC — HOÀN TOÀN EVENT-DRIVEN.
 * Dùng CM_Register_Notification() — Windows gọi callback khi radio đổi.
 * KHÔNG polling, KHÔNG loop, KHÔNG thread riêng.
 *
 * ⚠️ cfgmgr32.h, devpkey.h, bluetoothapis.h đã được include trong framework.h
 *    KHÔNG include lại ở đây.
 *
 * LIMITATION:
 *   CM_NOTIFY_FILTER_TYPE_DEVICEINTERFACE với Bluetooth Radio GUID chỉ bắt
 *   được khi interface được BẬT/TẮT qua driver. Nếu user disable radio qua
 *   Device Manager (interface vẫn tồn tại), callback có thể KHÔNG được gọi.
 *   → Mitigation: luôn re-check BluetoothMonitor_IsAvailable() trước khi
 *     quyết định fallback (đã làm trong ServerCore.cpp watchdog).
 */

#include "pch.h"

namespace mouseKeyboard {

    // ============================================================
    //  Bluetooth Radio Interface Class GUID
    //  {e0cbf06c-cd8b-4647-bb8a-263b43f0f974}
    // ============================================================
    static const GUID GUID_BLUETOOTH_RADIO_INTERFACE = {
        0xe0cbf06c, 0xcd8b, 0x4647,
        { 0xbb, 0x8a, 0x26, 0x3b, 0x43, 0xf0, 0xf9, 0x74 }
    };

    // ============================================================
    //  State
    // ============================================================
    static std::atomic<bool> gBtAvailable{ false };
    static HCMNOTIFICATION   gBtNotifyHandle = nullptr;
    static std::mutex        gBtLock;

    static std::function<void()> gOnBtLost;
    static std::function<void()> gOnBtRestored;

    // ============================================================
    //  Kiểm tra PC có Bluetooth radio không
    // ============================================================
    static bool hasBluetoothRadio() {
        BLUETOOTH_FIND_RADIO_PARAMS params = {};
        params.dwSize = sizeof(params);

        HANDLE hRadio = nullptr;
        HBLUETOOTH_RADIO_FIND hFind =
            BluetoothFindFirstRadio(&params, &hRadio);  

        if (hFind == nullptr) return false;

        if (hRadio) {
            CloseHandle(hRadio);   // ← PHẢI đóng radio handle
        }

        BluetoothFindRadioClose(hFind);
        return true;
    }

    // ============================================================
    //  Callback — Windows gọi khi BT device thay đổi
    //  ⚠️ Chạy trên thread riêng của Windows
    // ============================================================
    static DWORD CALLBACK onBtDeviceChanged(
        HCMNOTIFICATION /*hNotify*/,
        PVOID /*context*/,
        CM_NOTIFY_ACTION action,
        PCM_NOTIFY_EVENT_DATA /*eventData*/,
        DWORD /*eventDataSize*/)
    {
        // Chỉ quan tâm arrival/removal
        if (action != CM_NOTIFY_ACTION_DEVICEINTERFACEARRIVAL &&
            action != CM_NOTIFY_ACTION_DEVICEINTERFACEREMOVAL) {
            return ERROR_SUCCESS;
        }

        // Kiểm tra BT NGOÀI lock
        bool nowAvail = hasBluetoothRadio();
        bool prevAvail = gBtAvailable.exchange(nowAvail);

        if (prevAvail == nowAvail) return ERROR_SUCCESS;  // Không đổi

        // Đọc callback TRONG lock — tránh race với BluetoothMonitor_Stop()
        std::function<void()> cb;
        {
            std::lock_guard<std::mutex> l(gBtLock);
            cb = nowAvail ? gOnBtRestored : gOnBtLost;
        }

        if (cb) {
            std::thread([cb]() { cb(); }).detach();
        }

        return ERROR_SUCCESS;
    }

    // ============================================================
    //  Start / Stop
    // ============================================================
    void BluetoothMonitor_Start(
        std::function<void()> onLost,
        std::function<void()> onRestored)
    {
        std::lock_guard<std::mutex> l(gBtLock);

        if (gBtNotifyHandle) return;

        gOnBtLost = onLost;
        gOnBtRestored = onRestored;
        gBtAvailable.store(hasBluetoothRadio());

        CM_NOTIFY_FILTER filter = {};
        filter.cbSize = sizeof(filter);
        filter.FilterType = CM_NOTIFY_FILTER_TYPE_DEVICEINTERFACE;
        filter.u.DeviceInterface.ClassGuid = GUID_BLUETOOTH_RADIO_INTERFACE;

        CONFIGRET cr = CM_Register_Notification(
            &filter,
            nullptr,
            onBtDeviceChanged,
            &gBtNotifyHandle);

        if (cr != CR_SUCCESS) {
            gBtNotifyHandle = nullptr;
        }
    }

    void BluetoothMonitor_Stop()
    {
        std::lock_guard<std::mutex> l(gBtLock);
        if (gBtNotifyHandle) {
            CM_Unregister_Notification(gBtNotifyHandle);
            gBtNotifyHandle = nullptr;
        }
        gOnBtLost = nullptr;
        gOnBtRestored = nullptr;
    }

    bool BluetoothMonitor_IsAvailable() {
        return gBtAvailable.load();
    }

} // namespace mouseKeyboard