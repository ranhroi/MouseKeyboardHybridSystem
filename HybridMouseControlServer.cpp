/* main.cpp — Test DLL MouseKeyboardHybrid
 *
 * Cấu hình project:
 *   - C/C++ → General → Additional Include Directories:
 *       $(SolutionDir)MouseKeyboardHybrid\include
 *   - Linker → General → Additional Library Directories:
 *       $(SolutionDir)$(Configuration)
 *   - Linker → Input → Additional Dependencies:
 *       MouseKeyboardHybrid.lib
 *   - References → Add Reference → MouseKeyboardHybrid
 */

#include <windows.h>
#include <iostream>
#include <conio.h>

#include "HybridMouseControlDLL.h"

 // ============================================================
 //  Helper: wstring -> string (UTF-8)
 // ============================================================
static std::string ws2s(const std::wstring& w) {
    if (w.empty()) return {};
    int n = WideCharToMultiByte(CP_UTF8, 0, w.c_str(), (int)w.size(),
        nullptr, 0, nullptr, nullptr);
    std::string s(n, 0);
    WideCharToMultiByte(CP_UTF8, 0, w.c_str(), (int)w.size(),
        &s[0], n, nullptr, nullptr);
    return s;
}

// ============================================================
//  Status callback
// ============================================================
void OnHidStatus(HID_Status status, HID_Transport transport,
    const wchar_t* message, void* /*user*/)
{
    const char* st = "?";
    switch (status) {
    case HID_STATUS_IDLE:           st = "IDLE";           break;
    case HID_STATUS_CONNECTING:     st = "CONNECTING";     break;
    case HID_STATUS_AUTHENTICATING: st = "AUTHENTICATING"; break;
    case HID_STATUS_CONNECTED_WIFI: st = "CONNECTED_WIFI"; break;
    case HID_STATUS_CONNECTED_BLE:  st = "CONNECTED_BLE";  break;
    case HID_STATUS_SWITCHING:      st = "SWITCHING";      break;
    case HID_STATUS_ERROR:          st = "ERROR";          break;
    case HID_STATUS_STOPPING:       st = "STOPPING";       break;
    }

    const char* tp = "NONE";
    switch (transport) {
    case HID_TRANSPORT_WIFI: tp = "WIFI"; break;
    case HID_TRANSPORT_BLE:  tp = "BLE";  break;
    default: break;
    }

    std::cout << "[STATUS] " << st
        << " | " << tp
        << " | " << (message ? ws2s(message) : "")
        << std::endl;
}

// ============================================================
//  Print current status
// ============================================================
void PrintStatus()
{
    HID_Status    st = HID_GetStatus();
    HID_Transport tp = HID_GetTransport();
    HID_Mode      md = HID_GetMode();

    const char* statusStr = "?";
    switch (st) {
    case HID_STATUS_IDLE:           statusStr = "IDLE";           break;
    case HID_STATUS_CONNECTING:     statusStr = "CONNECTING";     break;
    case HID_STATUS_AUTHENTICATING: statusStr = "AUTHENTICATING"; break;
    case HID_STATUS_CONNECTED_WIFI: statusStr = "CONNECTED_WIFI"; break;
    case HID_STATUS_CONNECTED_BLE:  statusStr = "CONNECTED_BLE";  break;
    case HID_STATUS_SWITCHING:      statusStr = "SWITCHING";      break;
    case HID_STATUS_ERROR:          statusStr = "ERROR";          break;
    case HID_STATUS_STOPPING:       statusStr = "STOPPING";       break;
    }

    const char* transportStr = "NONE";
    switch (tp) {
    case HID_TRANSPORT_WIFI: transportStr = "WIFI"; break;
    case HID_TRANSPORT_BLE:  transportStr = "BLE";  break;
    default: break;
    }

    const char* modeStr = "?";
    switch (md) {
    case HID_MODE_BLE:   modeStr = "BLE";   break;
    case HID_MODE_WIFI:  modeStr = "WIFI";  break;
    }

    std::cout << "\n+---- STATUS -----------------------+\n"
        << "|  Status    : " << statusStr << "\n"
        << "|  Transport : " << transportStr << "\n"
        << "|  Mode      : " << modeStr << "\n"
        << "+-----------------------------------+\n"
        << std::endl;
}

// ============================================================
//  Menu
// ============================================================
void PrintHelp()
{
    std::cout << "\n+====================================+\n"
        << "|        MENU - CONTROL DLL          |\n"
        << "+====================================+\n"
        << "|  1 - Connect (start transport)     |\n"
        << "|  2 - Switch to BLE mode            |\n"
        << "|  3 - Switch to WIFI mode           |\n"
        << "|  4 - Show status                   |\n"
        << "|  5 - Disconnect                    |\n"
        << "|  h - Help                          |\n"
        << "|  q - Quit                          |\n"
        << "+====================================+\n"
        << std::endl;
}

// ============================================================
//  main
// ============================================================
int main()
{
    std::cout << "=== MouseKeyboardHybrid Test ===" << std::endl;

    // ---- Config ----
    HID_Config cfg{};
    cfg.wifi_host = L"0.0.0.0";
    cfg.wifi_port = 2000;
    cfg.wifi_password = L"123456";
    cfg.ble_device_name = L"BLE_COMPUTER_REMOTE";
    cfg.ble_service_uuid = L"2D2A0001-8A5A-4E76-A2E3-1E57D9A1B001";
    cfg.ble_write_uuid = L"2D2A0002-8A5A-4E76-A2E3-1E57D9A1B001";
    cfg.ble_notify_uuid = L"2D2A0003-8A5A-4E76-A2E3-1E57D9A1B001";
    cfg.connect_timeout_ms = 5000;
    cfg.retry_interval_ms = 3000;
    cfg.auto_failover_to_ble = 0;
    cfg.auto_enable_bluetooth = 1;
    cfg.start_mode = HID_MODE_BLE;

    // ---- Initialize ----
    std::cout << "[INFO] HID_Initialize()..." << std::endl;
    if (!HID_Initialize(&cfg, OnHidStatus, nullptr)) {
        std::cerr << "[ERROR] HID_Initialize failed" << std::endl;
        std::cout << "Press Enter to exit...";
        std::cin.get();
        return 1;
    }

    // ---- Connect ----
    std::cout << "[INFO] HID_Connect()..." << std::endl;
    if (!HID_Connect()) {
        std::cerr << "[ERROR] HID_Connect failed" << std::endl;
        HID_Shutdown();
        std::cout << "Press Enter to exit...";
        std::cin.get();
        return 1;
    }

    std::cout << "[OK] DLL is running. Waiting for client..." << std::endl;

    // ---- Menu loop ----
    PrintHelp();
    bool running = true;

    while (running) {
        std::cout << "\n> ";
        int ch = _getch();
        std::cout << (char)ch << std::endl;

        switch (ch) {
        case '1':
            std::cout << "[CMD] Connect" << std::endl;
            HID_Connect();
            break;

        case '2':
            std::cout << "[CMD] Switch to BLE" << std::endl;
            HID_SwitchMode(HID_MODE_BLE);
            break;

        case '3':
            std::cout << "[CMD] Switch to WIFI" << std::endl;
            HID_SwitchMode(HID_MODE_WIFI);
            break;

        case '4':
            PrintStatus();
            break;

        case '5':
            std::cout << "[CMD] Disconnect" << std::endl;
            HID_Disconnect();
            break;

        case 'h':
        case 'H':
            PrintHelp();
            break;

        case 'q':
        case 'Q':
        case 27:  // ESC
            std::cout << "[CMD] Quit" << std::endl;
            running = false;
            break;

        default:
            std::cout << "[INFO] Invalid key. Press 'h' for help."
                << std::endl;
            break;
        }
    }

    // ---- Cleanup ----
    std::cout << "\n[INFO] Shutdown..." << std::endl;
    HID_Shutdown();
    std::cout << "[OK] Exited." << std::endl;

    return 0;
}