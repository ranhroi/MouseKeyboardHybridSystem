/* framework.h
 *
 * Precompiled header — gom TẤT CẢ include của dự án.
 *
 * ⚠️ THỨ TỰ INCLUDE RẤT QUAN TRỌNG — KHÔNG ĐƯỢC ĐỔI:
 *
 *   BƯỚC 1: Define macros
 *   BƯỚC 2: Winsock2 — PHẢI TRƯỚC windows.h
 *   BƯỚC 3: Windows.h + COM
 *   BƯỚC 4: Network APIs (netioapi.h, iphlpapi.h) — SAU windows.h
 *   BƯỚC 5: Bluetooth + Device APIs — SAU windows.h
 *   BƯỚC 6: C++ standard library
 *   BƯỚC 7: WinRT headers — SAU CÙNG (trước project headers)
 *   BƯỚC 8: Project headers
 */

#pragma once

 // ============================================================
 //  BƯỚC 1: Macros — PHẢI TRƯỚC MỌI INCLUDE
 // ============================================================
#ifndef WIN32_LEAN_AND_MEAN
#  define WIN32_LEAN_AND_MEAN
#endif

#ifndef NOMINMAX
#  define NOMINMAX
#endif

// ⚠️ KHÔNG define _WINSOCKAPI_ — vì netioapi.h cần một số định nghĩa
// #ifndef _WINSOCKAPI_
// #  define _WINSOCKAPI_
// #endif

// ============================================================
//  BƯỚC 2: Winsock2 — PHẢI TRƯỚC windows.h
// ============================================================
#include <winsock2.h>
#include <ws2tcpip.h>
#include <mstcpip.h>        // TCP_KEEPIDLE, TCP_KEEPINTVL, TCP_KEEPCNT

// ============================================================
//  BƯỚC 3: Windows.h + COM
// ============================================================
#include <windows.h>
#include <objbase.h>        // CLSIDFromString
#include <rpc.h>            // UuidFromStringW

// ============================================================
//  BƯỚC 4: Network APIs
//  PHẢI SAU windows.h, TRƯỚC WinRT
// ============================================================
#include <iphlpapi.h>       // GetAdaptersAddresses
#include <netioapi.h>       // NotifyIpInterfaceChange

// ============================================================
//  BƯỚC 5: Bluetooth + Device APIs
//  PHẢI SAU windows.h
// ============================================================
#include <cfgmgr32.h>       // CM_Register_Notification, CM_Unregister_Notification
#include <devpkey.h>        // DEVPKEY_*
#include <devpropdef.h>     // DEVPROPKEY
#include <bluetoothapis.h>  // BluetoothFindFirstRadio, BluetoothFindRadioClose

// Power profile functions (SetSuspendState)
#include <PowrProf.h>

// ============================================================
//  BƯỚC 6: C++ standard library
// ============================================================
#include <stdint.h>
#include <cstddef>
#include <cstring>
#include <string>
#include <vector>
#include <deque>
#include <mutex>
#include <atomic>
#include <thread>
#include <condition_variable>
#include <memory>
#include <functional>
#include <algorithm>
#include <chrono>
#include <shlobj.h> 
// ============================================================
//  BƯỚC 7: WinRT headers (C++/WinRT)
// ============================================================
#include <winrt/base.h>
#include <winrt/Windows.Foundation.h>
#include <winrt/Windows.Foundation.Collections.h>
#include <winrt/Windows.Storage.Streams.h>
#include <winrt/Windows.Devices.Bluetooth.h>
#include <winrt/Windows.Devices.Bluetooth.GenericAttributeProfile.h>
#include <winrt/Windows.Devices.Bluetooth.Advertisement.h>

// ============================================================
//  BƯỚC 8: Project headers
// ============================================================
#include "HybridMouseControlDLL.h"
#include "Internal.h"

// ============================================================
//  Auto-link libraries
// ============================================================
#pragma comment(lib, "Ws2_32.lib")         // Winsock2: socket, TCP server, select, keep-alive
#pragma comment(lib, "Rpcrt4.lib")         // UuidFromStringW (parse BLE UUID)
#pragma comment(lib, "WindowsApp.lib")     // WinRT: GattServiceProvider, Bluetooth APIs
#pragma comment(lib, "Ole32.lib")          // COM: CoInitialize, CLSIDFromString
#pragma comment(lib, "Iphlpapi.lib")       // NotifyIpInterfaceChange, GetAdaptersAddresses
#pragma comment(lib, "PowrProf.lib")       // SetSuspendState (system actions)
#pragma comment(lib, "Bthprops.lib")       // BluetoothFindFirstRadio, BluetoothFindRadioClose
#pragma comment(lib, "Cfgmgr32.lib")       // CM_Register_Notification, CM_Unregister_Notification
#pragma comment(lib, "User32.lib")         // GetForegroundWindow, GetWindowTextW, OpenProcess
#pragma comment(lib, "Shell32.lib")