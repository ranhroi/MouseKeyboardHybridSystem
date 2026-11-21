// AppSettings.cpp
#include "AppSettings.h"
#include <Windows.h>
#include <shlobj.h>
#include <fstream>
#include <sstream>
#include <string>
#include <cstdlib>
#include <ctime>

#pragma comment(lib, "shell32.lib")
#pragma comment(lib, "advapi32.lib")

// ===== STATIC MEMBERS =====
int         AppSettings::TCPPort = 2000;
bool        AppSettings::NeedPassword = true;      // LUÔN BẬT
std::string AppSettings::Password = "";        // Sẽ auto-generate khi Load
bool        AppSettings::BluetoothEnabled = true;
std::string AppSettings::BleDeviceName = "BLE_COMPUTER_REMOTE";
std::string AppSettings::BleServiceUuid = "2D2A0001-8A5A-4E76-A2E3-1E57D9A1B001";
std::string AppSettings::BleWriteUuid = "2D2A0002-8A5A-4E76-A2E3-1E57D9A1B001";
std::string AppSettings::BleNotifyUuid = "2D2A0003-8A5A-4E76-A2E3-1E57D9A1B001";
bool        AppSettings::AutoRunWithWindows = false;
bool        AppSettings::AutoStartServer = false;
int         AppSettings::StartMode = 0;

// ===== HELPER: PATH =====
static std::string GetConfigDir()
{
    char path[MAX_PATH] = { 0 };
    if (SUCCEEDED(SHGetFolderPathA(NULL, CSIDL_LOCAL_APPDATA, NULL, 0, path)))
    {
        std::string dir = std::string(path) + "\\HybridMouseControl";
        CreateDirectoryA(dir.c_str(), NULL);
        return dir;
    }
    return ".";
}

static std::string GetConfigPath()
{
    return GetConfigDir() + "\\config.dat";
}

// ===== HELPER: PARSE =====
static std::string GetKey(const std::string& line)
{
    size_t pos = line.find('=');
    if (pos == std::string::npos) return line;
    return line.substr(0, pos);
}

static std::string GetValue(const std::string& line)
{
    size_t pos = line.find('=');
    if (pos == std::string::npos) return "";
    return line.substr(pos + 1);
}

// ===== ENCRYPT / DECRYPT =====
static std::string EncryptPassword(const std::string& password)
{
    if (password.empty()) return "";

    const char key = 'K';
    std::string enc = password;
    for (size_t i = 0; i < enc.length(); i++)
        enc[i] = enc[i] ^ key;

    std::stringstream ss;
    for (size_t i = 0; i < enc.length(); i++)
    {
        ss << std::hex << (int)(unsigned char)enc[i];
        if (i < enc.length() - 1) ss << ",";
    }
    return ss.str();
}

static std::string DecryptPassword(const std::string& encrypted)
{
    if (encrypted.empty()) return "";

    std::string dec;
    std::stringstream ss(encrypted);
    std::string item;

    while (std::getline(ss, item, ','))
    {
        if (item.empty()) continue;
        try
        {
            int val = std::stoi(item, nullptr, 16);
            dec += (char)(val ^ 'K');
        }
        catch (...) {}
    }
    return dec;
}

// ===== ANSI → WIDE =====
static std::wstring AnsiToWide(const std::string& ansi)
{
    if (ansi.empty()) return L"";
    int len = MultiByteToWideChar(CP_ACP, 0, ansi.c_str(),
        (int)ansi.size(), nullptr, 0);
    if (len <= 0) return L"";
    std::wstring w(len, 0);
    MultiByteToWideChar(CP_ACP, 0, ansi.c_str(),
        (int)ansi.size(), &w[0], len);
    return w;
}

// ===== LOAD =====
void AppSettings::Load()
{
    std::string configPath = GetConfigPath();

    std::ifstream file(configPath.c_str(), std::ios::binary);
    if (!file.is_open())
    {
        // File chưa tồn tại → dùng default
        NeedPassword = true;
        if (Password.empty())
            Password = GenerateRandomPassword(6);

        SetAutoRunRegistry(AutoRunWithWindows);
        return;
    }

    std::string content((std::istreambuf_iterator<char>(file)),
        std::istreambuf_iterator<char>());
    file.close();

    std::stringstream ss(content);
    std::string line;

    while (std::getline(ss, line))
    {
        if (!line.empty() && line.back() == '\r') line.pop_back();
        if (line.empty() || line[0] == '#') continue;

        std::string key = GetKey(line);
        std::string value = GetValue(line);

        if (key == "TCPPort")
        {
            try { TCPPort = std::stoi(value); }
            catch (...) { TCPPort = 2000; }
            if (TCPPort < 1 || TCPPort > 65535) TCPPort = 2000;
        }
        else if (key == "NeedPassword")
        {
            // Bỏ qua — luôn bật
        }
        else if (key == "Password")
        {
            Password = DecryptPassword(value);
        }
        else if (key == "BluetoothEnabled")
        {
            BluetoothEnabled = (value == "1" || value == "true");
        }
        else if (key == "BleDeviceName")
        {
            BleDeviceName = value;
        }
        else if (key == "BleServiceUuid")
        {
            BleServiceUuid = value;
        }
        else if (key == "BleWriteUuid")
        {
            BleWriteUuid = value;
        }
        else if (key == "BleNotifyUuid")
        {
            BleNotifyUuid = value;
        }
        else if (key == "AutoRunWithWindows")
        {
            AutoRunWithWindows = (value == "1" || value == "true");
        }
        else if (key == "AutoStartServer")
        {
            AutoStartServer = (value == "1" || value == "true");
        }
        else if (key == "StartMode")
        {
            try { StartMode = std::stoi(value); }
            catch (...) { StartMode = 0; }
            if (StartMode < 0 || StartMode > 1) StartMode = 0;
        }
    }

    // ⚠️ LUÔN BẬT PASSWORD
    NeedPassword = true;

    // Nếu chưa có password → tạo random
    if (Password.empty())
        Password = GenerateRandomPassword(6);

    SetAutoRunRegistry(AutoRunWithWindows);
}

// ===== SAVE =====
void AppSettings::Save()
{
    std::string configPath = GetConfigPath();

    std::stringstream ss;
    ss << "TCPPort=" << TCPPort << "\n";
    ss << "NeedPassword=" << "1" << "\n";  // Luôn 1
    ss << "Password=" << EncryptPassword(Password) << "\n";
    ss << "BluetoothEnabled=" << (BluetoothEnabled ? "1" : "0") << "\n";
    ss << "BleDeviceName=" << BleDeviceName << "\n";
    ss << "BleServiceUuid=" << BleServiceUuid << "\n";
    ss << "BleWriteUuid=" << BleWriteUuid << "\n";
    ss << "BleNotifyUuid=" << BleNotifyUuid << "\n";
    ss << "AutoRunWithWindows=" << (AutoRunWithWindows ? "1" : "0") << "\n";
    ss << "AutoStartServer=" << (AutoStartServer ? "1" : "0") << "\n";
    ss << "StartMode=" << StartMode << "\n";

    std::ofstream file(configPath.c_str(), std::ios::binary | std::ios::trunc);
    if (!file.is_open())
    {
        MessageBoxA(NULL, "Cannot save config file!", "Error", MB_OK | MB_ICONERROR);
        return;
    }

    std::string content = ss.str();
    file.write(content.c_str(), content.size());
    file.close();

    SetAutoRunRegistry(AutoRunWithWindows);
}

// ===== TO HID CONFIG =====
HID_Config AppSettings::ToHidConfig()
{
    HID_Config config = {};

    // Giữ chuỗi sống trong suốt thời gian DLL chạy
    static std::wstring s_wifiHost;
    static std::wstring s_wifiPassword;
    static std::wstring s_bleName;
    static std::wstring s_serviceUuid;
    static std::wstring s_writeUuid;
    static std::wstring s_notifyUuid;

    s_wifiHost = L"0.0.0.0";
    s_wifiPassword = AnsiToWide(Password);
    s_bleName = AnsiToWide(BleDeviceName);
    s_serviceUuid = AnsiToWide(BleServiceUuid);
    s_writeUuid = AnsiToWide(BleWriteUuid);
    s_notifyUuid = AnsiToWide(BleNotifyUuid);

    config.wifi_host = s_wifiHost.c_str();
    config.wifi_port = (uint16_t)TCPPort;
    config.wifi_password = s_wifiPassword.c_str();
    config.ble_device_name = s_bleName.c_str();
    config.ble_service_uuid = s_serviceUuid.c_str();
    config.ble_write_uuid = s_writeUuid.c_str();
    config.ble_notify_uuid = s_notifyUuid.c_str();
    config.connect_timeout_ms = 5000;
    config.retry_interval_ms = 3000;
    config.auto_failover_to_ble = 0;
    config.auto_enable_bluetooth = BluetoothEnabled ? 1 : 0;
    config.start_mode = (HID_Mode)StartMode;

    return config;
}

// ===== AUTO RUN =====
void AppSettings::SetAutoRunRegistry(bool enable)
{
    HKEY hKey = NULL;
    LONG result = RegOpenKeyExA(
        HKEY_CURRENT_USER,
        "Software\\Microsoft\\Windows\\CurrentVersion\\Run",
        0, KEY_SET_VALUE | KEY_QUERY_VALUE, &hKey);

    if (result != ERROR_SUCCESS) return;

    if (enable)
    {
        char exePath[MAX_PATH] = { 0 };
        GetModuleFileNameA(NULL, exePath, MAX_PATH);
        RegSetValueExA(hKey, "HybridMouseControlServer", 0, REG_SZ,
            (const BYTE*)exePath, (DWORD)(strlen(exePath) + 1));
    }
    else
    {
        RegDeleteValueA(hKey, "HybridMouseControlServer");
    }

    RegCloseKey(hKey);
}

bool AppSettings::IsAutoRunEnabled()
{
    HKEY hKey = NULL;
    LONG result = RegOpenKeyExA(
        HKEY_CURRENT_USER,
        "Software\\Microsoft\\Windows\\CurrentVersion\\Run",
        0, KEY_QUERY_VALUE, &hKey);

    if (result != ERROR_SUCCESS) return false;

    char value[MAX_PATH] = { 0 };
    DWORD size = sizeof(value);
    DWORD type = 0;

    result = RegQueryValueExA(hKey, "HybridMouseControlServer",
        NULL, &type, (LPBYTE)value, &size);

    RegCloseKey(hKey);
    return (result == ERROR_SUCCESS && type == REG_SZ);
}

// ===== RESET =====
void AppSettings::ResetToDefault()
{
    TCPPort = 2000;
    NeedPassword = true;
    Password = GenerateRandomPassword(6);
    BluetoothEnabled = true;
    BleDeviceName = "BLE_COMPUTER_REMOTE";
    BleServiceUuid = "2D2A0001-8A5A-4E76-A2E3-1E57D9A1B001";
    BleWriteUuid = "2D2A0002-8A5A-4E76-A2E3-1E57D9A1B001";
    BleNotifyUuid = "2D2A0003-8A5A-4E76-A2E3-1E57D9A1B001";
    AutoRunWithWindows = false;
    AutoStartServer = false;
    StartMode = 0;

    SetAutoRunRegistry(false);
    Save();
}

// ===== PASSWORD =====
std::string AppSettings::GenerateRandomPassword(int length)
{
    if (length < 1) length = 6;
    if (length > 20) length = 20;

    const char chars[] = "0123456";
    int charCount = (sizeof(chars) / sizeof(char)) - 1;

    std::string result;
    result.reserve(length);

    // Seed 1 lần duy nhất
    static bool seeded = false;
    if (!seeded)
    {
        srand((unsigned int)GetTickCount64());
        seeded = true;
    }

    for (int i = 0; i < length; i++)
        result += chars[rand() % charCount];

    return result;
}