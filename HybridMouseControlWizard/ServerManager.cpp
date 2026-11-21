#include "ServerManager.h"
#include "AppSettings.h"
#include <sstream>
#include <cstdio>
#include <string>

// ===== CONSTRUCTOR =====
ServerManager::ServerManager()
    : m_running(false)
    , m_connTcp(0)
    , m_connBt(0)
{
}

ServerManager::~ServerManager()
{
    Stop();
}

// ===== START =====
bool ServerManager::Start()
{
    if (m_running) return true;

    HID_Config config = AppSettings::ToHidConfig();

    {
        char buf[1024];
        sprintf_s(buf, sizeof(buf),
            "[ServerManager] Starting: TCP=%u, BLE=%s, Mode=%s",
            (unsigned)config.wifi_port,
            AppSettings::BleDeviceName.c_str(),
            AppSettings::StartMode == 0 ? "BLE" : "WIFI");
        PushLog(buf);
    }

    if (!HID_Initialize(&config, &ServerManager::StatusCallback, this))
    {
        PushLog("[ServerManager] HID_Initialize FAILED");
        return false;
    }

    if (!HID_Connect())
    {
        PushLog("[ServerManager] HID_Connect FAILED");
        HID_Shutdown();
        return false;
    }

    m_running = true;

    // Đồng bộ mode ngay sau khi connect
    // (vì HID_Connect có thể đã tự fallback BLE → WiFi)
    SyncModeFromDLL();

    // Log transport thực tế
    {
        HID_Transport tp = HID_GetTransport();
        const char* tpStr = "NONE";
        if (tp == HID_TRANSPORT_WIFI) tpStr = "WIFI";
        else if (tp == HID_TRANSPORT_BLE) tpStr = "BLE";

        char buf[128];
        sprintf_s(buf, "[ServerManager] Started OK (transport=%s)", tpStr);
        PushLog(buf);
    }

    return true;
}

// ===== STOP =====
void ServerManager::Stop()
{
    if (!m_running) return;

    PushLog("[ServerManager] Stopping...");

    HID_Disconnect();
    HID_Shutdown();

    m_running = false;
    m_connTcp = 0;
    m_connBt = 0;

    PushLog("[ServerManager] Stopped");
}

// ===== SWITCH MODE =====
bool ServerManager::SwitchMode(HID_Mode mode)
{
    if (!m_running)
    {
        AppSettings::StartMode = (int)mode;
        AppSettings::Save();
        return true;
    }

    if (HID_SwitchMode(mode))
    {
        AppSettings::StartMode = (int)mode;
        AppSettings::Save();

        char buf[64];
        sprintf_s(buf, "[ServerManager] Switched to %s",
            mode == HID_MODE_BLE ? "BLE" : "WIFI");
        PushLog(buf);
        return true;
    }

    PushLog("[ServerManager] Switch FAILED");
    return false;
}

// ===== GET LIVE TRANSPORT =====
HID_Transport ServerManager::GetLiveTransport() const
{
    if (m_running)
        return HID_GetTransport();
    return HID_TRANSPORT_NONE;
}

// ===== SYNC MODE TỪ DLL → AppSettings =====
void ServerManager::SyncModeFromDLL()
{
    if (!m_running) return;

    HID_Transport tp = HID_GetTransport();

    int newMode = AppSettings::StartMode;
    if (tp == HID_TRANSPORT_BLE)  newMode = 0;   // HID_MODE_BLE
    else if (tp == HID_TRANSPORT_WIFI) newMode = 1;   // HID_MODE_WIFI

    // Chỉ ghi đè khi mode thực sự thay đổi
    if (newMode != AppSettings::StartMode)
    {
        AppSettings::StartMode = newMode;
        AppSettings::Save();

        char buf[128];
        sprintf_s(buf, "[ServerManager] Auto-saved mode: %s",
            newMode == 0 ? "BLE" : "WIFI");
        PushLog(buf);
    }
}

// ===== STATUS =====
HID_Status ServerManager::GetStatus() const
{
    if (m_running) return HID_GetStatus();
    return HID_STATUS_IDLE;
}

HID_Transport ServerManager::GetTransport() const
{
    if (m_running) return HID_GetTransport();
    return HID_TRANSPORT_NONE;
}

bool ServerManager::IsRunning() const
{
    return m_running;
}

// ===== CONNECTION STATS =====
int ServerManager::GetConnectionCount() const
{
    return m_connTcp + m_connBt;
}

void ServerManager::GetConnectionStats(int* tcp, int* bt) const
{
    if (tcp) *tcp = m_connTcp;
    if (bt)  *bt = m_connBt;
}

// ===== LOG =====
void ServerManager::PushLog(const std::string& msg)
{
    std::lock_guard<std::mutex> lk(m_logMutex);
    m_logQueue.push_back(msg);

    if (m_logQueue.size() > 100)
        m_logQueue.erase(m_logQueue.begin());
}

std::string ServerManager::PopLog()
{
    std::lock_guard<std::mutex> lk(m_logMutex);
    if (m_logQueue.empty()) return std::string();

    std::string msg = m_logQueue.front();
    m_logQueue.erase(m_logQueue.begin());
    return msg;
}

// ===== STATUS CALLBACK =====
void ServerManager::StatusCallback(
    HID_Status status,
    HID_Transport transport,
    const wchar_t* message,
    void* user)
{
    ServerManager* self = static_cast<ServerManager*>(user);
    if (!self) return;

    // Đếm connection
    if (status == HID_STATUS_CONNECTED_WIFI)      self->m_connTcp = 1;
    else if (status == HID_STATUS_CONNECTED_BLE)  self->m_connBt = 1;
    else if (status == HID_STATUS_IDLE)
    {
        self->m_connTcp = 0;
        self->m_connBt = 0;
    }

    // Log
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

    std::string msgStr;
    if (message)
    {
        int len = WideCharToMultiByte(CP_ACP, 0, message, -1,
            nullptr, 0, nullptr, nullptr);
        if (len > 0)
        {
            msgStr.resize(len - 1);
            WideCharToMultiByte(CP_ACP, 0, message, -1,
                &msgStr[0], len, nullptr, nullptr);
        }
    }

    char buf[512];
    sprintf_s(buf, sizeof(buf), "[%s | %s] %s", st, tp, msgStr.c_str());
    self->PushLog(buf);
}