/* CurrentApp.cpp
 *
 * Xử lý lệnh "Report Current App" — lấy tên app đang foreground trên PC
 * và gửi về client (điện thoại).
 *
 * Protocol:
 *   Client gửi: [0xE1]           (1 byte opcode)
 *   DLL trả về: "app <name>.exe\n"  hoặc  "app unknown\n"
 *
 * Hỗ trợ cả TCP và BLE.
 */

#include "pch.h"

namespace mouseKeyboard {

    // ============================================================
    //  Helper: Wide → UTF-8 (dùng cho response)
    // ============================================================
    static std::string WStringToUtf8(const std::wstring& wstr) {
        if (wstr.empty()) return "";
        int size_needed = WideCharToMultiByte(CP_UTF8, 0,
            wstr.c_str(), (int)wstr.length(),
            NULL, 0, NULL, NULL);
        if (size_needed <= 0) return "";
        std::string str(size_needed, 0);
        WideCharToMultiByte(CP_UTF8, 0,
            wstr.c_str(), (int)wstr.length(),
            &str[0], size_needed, NULL, NULL);
        return str;
    }

    // ============================================================
    //  Lấy tên exe của app đang foreground
    //  Trả về "" nếu không lấy được
    // ============================================================
    static std::string getForegroundAppExeName() {
        HWND hWnd = GetForegroundWindow();
        if (hWnd == NULL) return "";

        DWORD processId = 0;
        GetWindowThreadProcessId(hWnd, &processId);
        if (processId == 0) return "";

        std::string exeName;

        // Cách 1: OpenProcess + QueryFullProcessImageNameW (chính xác nhất)
        HANDLE hProcess = OpenProcess(
            PROCESS_QUERY_LIMITED_INFORMATION, FALSE, processId);
        if (hProcess) {
            wchar_t path[MAX_PATH] = { 0 };
            DWORD size = MAX_PATH;

            if (QueryFullProcessImageNameW(hProcess, 0, path, &size)) {
                std::string fullPath = WStringToUtf8(path);
                size_t lastSlash = fullPath.find_last_of("\\/");
                if (lastSlash != std::string::npos) {
                    exeName = fullPath.substr(lastSlash + 1);
                }
                else {
                    exeName = fullPath;
                }
            }
            CloseHandle(hProcess);
        }

        // Cách 2 (fallback): GetWindowModuleFileNameA
        // Chỉ dùng nếu cách 1 thất bại
        if (exeName.empty()) {
            char moduleName[MAX_PATH] = { 0 };
            if (GetWindowModuleFileNameA(hWnd, moduleName, MAX_PATH) > 0) {
                std::string fullPath(moduleName);
                size_t lastSlash = fullPath.find_last_of("\\/");
                if (lastSlash != std::string::npos) {
                    exeName = fullPath.substr(lastSlash + 1);
                }
                else {
                    exeName = fullPath;
                }
            }
        }

        return exeName;
    }

    // ============================================================
    //  Xử lý lệnh 0xE1 — Report Current App
    //  Gửi response về client qua đúng transport (TCP/BLE)
    // ============================================================
    void currentAppRequest(bool ble) {
        std::string exeName = getForegroundAppExeName();

        // Tạo response
        std::string response;
        if (!exeName.empty()) {
            response = "app " + exeName + "\n";
        }
        else {
            response = "app unknown\n";
        }

        // Gửi về client
        sendProtocolResponse(ble,
            reinterpret_cast<const uint8_t*>(response.data()),
            response.size());

        // Log
        {
            wchar_t buf[256];
            std::wstring wresp(response.begin(), response.end());
            // Bỏ ký tự \n cuối để log gọn
            if (!wresp.empty() && wresp.back() == L'\n')
                wresp.pop_back();
            swprintf_s(buf, L"[CurrentApp] %s", wresp.c_str());
            OutputDebugStringW(buf);
        }
    }

} // namespace mouseKeyboard