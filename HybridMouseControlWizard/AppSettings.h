// AppSettings.h
#pragma once
#include <string>
#include "HybridMouseControlDLL.h"

class AppSettings
{
public:
    // ===== NETWORK =====
    static int  TCPPort;                 // default 2000

    // ===== SECURITY =====
    static bool         NeedPassword;    // LUÔN = true
    static std::string  Password;        // ANSI — luôn có giá trị

    // ===== BLUETOOTH =====
    static bool         BluetoothEnabled;
    static std::string  BleDeviceName;
    static std::string  BleServiceUuid;
    static std::string  BleWriteUuid;
    static std::string  BleNotifyUuid;

    // ===== STARTUP =====
    static bool AutoRunWithWindows;
    static bool AutoStartServer;

    // ===== MODE =====
    static int  StartMode;               // 0 = BLE, 1 = WIFI

    // ===== METHODS =====
    static void Load();
    static void Save();
    static void ResetToDefault();

    // ===== CONVERT TO DLL CONFIG =====
    static HID_Config ToHidConfig();

    // ===== AUTO RUN =====
    static void SetAutoRunRegistry(bool enable);
    static bool IsAutoRunEnabled();

    // ===== PASSWORD =====
    static std::string GenerateRandomPassword(int length = 6);
};