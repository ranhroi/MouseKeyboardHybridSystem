// ServerManager.h
#pragma once
#include <Windows.h>
#include <string>
#include <vector>
#include <mutex>
#include "HybridMouseControlDLL.h"

class ServerManager
{
public:
    ServerManager();
    ~ServerManager();

    bool Start();
    void Stop();
    bool IsRunning() const;

    bool SwitchMode(HID_Mode mode);

    // Đọc transport live từ DLL (mỗi lần gọi)
    HID_Transport GetLiveTransport() const;

    // Đồng bộ mode từ DLL → AppSettings::StartMode + Save config
    void SyncModeFromDLL();

    HID_Status    GetStatus() const;
    HID_Transport GetTransport() const;

    int  GetConnectionCount() const;
    void GetConnectionStats(int* tcp, int* bt) const;

    std::string PopLog();

    static void StatusCallback(
        HID_Status status,
        HID_Transport transport,
        const wchar_t* message,
        void* user);

private:
    bool m_running;

    mutable std::mutex       m_logMutex;
    std::vector<std::string> m_logQueue;

    mutable int m_connTcp;
    mutable int m_connBt;

    void PushLog(const std::string& msg);
};