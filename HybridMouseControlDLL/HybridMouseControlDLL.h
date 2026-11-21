#pragma once

#ifdef HYBRID_MOUSE_CONTROL_DLL_EXPORTS
#  define HYBRID_MOUSE_CONTROL_DLL_API __declspec(dllexport)
#else
#  define HYBRID_MOUSE_CONTROL_DLL_API __declspec(dllimport)
#endif

#ifdef __cplusplus
extern "C" {
#endif

    typedef enum HID_Transport {
        HID_TRANSPORT_NONE = 0,
        HID_TRANSPORT_WIFI = 1,
        HID_TRANSPORT_BLE = 2
    } HID_Transport;

    typedef enum HID_Status {
        HID_STATUS_IDLE = 0,
        HID_STATUS_CONNECTING = 1,
        HID_STATUS_AUTHENTICATING = 2,
        HID_STATUS_CONNECTED_WIFI = 3,
        HID_STATUS_CONNECTED_BLE = 4,
        HID_STATUS_SWITCHING = 5,
        HID_STATUS_ERROR = 6,
        HID_STATUS_STOPPING = 7
    } HID_Status;

    /* Chỉ còn 2 mode: BLE và WIFI */
    typedef enum HID_Mode {
        HID_MODE_BLE = 0,
        HID_MODE_WIFI = 1
    } HID_Mode;

    typedef struct HID_Config {
        const wchar_t* wifi_host;
        uint16_t       wifi_port;
        const wchar_t* wifi_password;
        const wchar_t* ble_device_name;
        const wchar_t* ble_service_uuid;
        const wchar_t* ble_write_uuid;
        const wchar_t* ble_notify_uuid;
        uint32_t connect_timeout_ms;
        uint32_t retry_interval_ms;
        int      auto_failover_to_ble;
        int      auto_enable_bluetooth;
        HID_Mode start_mode;
    } HID_Config;

    typedef void (*HID_StatusCallback)(HID_Status, HID_Transport, const wchar_t* message, void* user);

    HYBRID_MOUSE_CONTROL_DLL_API int           HID_Initialize(const HID_Config* config, HID_StatusCallback cb, void* user);
    HYBRID_MOUSE_CONTROL_DLL_API int           HID_Connect(void);
    HYBRID_MOUSE_CONTROL_DLL_API int           HID_SwitchMode(HID_Mode mode);
    HYBRID_MOUSE_CONTROL_DLL_API HID_Mode      HID_GetMode(void);
    HYBRID_MOUSE_CONTROL_DLL_API void          HID_Disconnect(void);
    HYBRID_MOUSE_CONTROL_DLL_API void          HID_Shutdown(void);
    HYBRID_MOUSE_CONTROL_DLL_API HID_Status    HID_GetStatus(void);
    HYBRID_MOUSE_CONTROL_DLL_API HID_Transport HID_GetTransport(void);

    HYBRID_MOUSE_CONTROL_DLL_API int           HID_IsNetworkUp(void);
    HYBRID_MOUSE_CONTROL_DLL_API int           HID_HasWifiAdapter(void);
    HYBRID_MOUSE_CONTROL_DLL_API int           HID_IsBluetoothAvailable(void);
#ifdef __cplusplus
}
#endif