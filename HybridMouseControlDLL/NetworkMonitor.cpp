/* NetworkMonitor.cpp
 *
 * Theo dõi trạng thái mạng vật lý của PC — HOÀN TOÀN EVENT-DRIVEN.
 * Dùng NotifyIpInterfaceChange() — Windows gọi callback khi có thay đổi.
 * KHÔNG polling, KHÔNG loop, KHÔNG thread riêng.
 *
 * ⚠️ iphlpapi.h và netioapi.h đã được include trong framework.h
 *    KHÔNG include lại ở đây.
 */

#include "pch.h"

namespace mouseKeyboard {

    // ============================================================
    //  State
    // ============================================================
    static std::atomic<bool> gNetworkUp{ false };
    static HANDLE            gNotifyHandle = nullptr;
    static std::mutex        gNetLock;

    // Callbacks do ServerCore set
    static std::function<void()> gOnNetworkLost;
    static std::function<void()> gOnNetworkRestored;

    // ============================================================
    //  Kiểm tra PC có mạng không
    // ============================================================
    static bool hasActiveNetwork() {
        ULONG bufLen = 15000;
        std::vector<uint8_t> buffer(bufLen);

        ULONG flags = GAA_FLAG_SKIP_ANYCAST |
            GAA_FLAG_SKIP_MULTICAST |
            GAA_FLAG_SKIP_DNS_SERVER;

        PIP_ADAPTER_ADDRESSES addrs =
            reinterpret_cast<PIP_ADAPTER_ADDRESSES>(buffer.data());

        ULONG result = GetAdaptersAddresses(
            AF_UNSPEC, flags, nullptr, addrs, &bufLen);

        if (result == ERROR_BUFFER_OVERFLOW) {
            buffer.resize(bufLen);
            addrs = reinterpret_cast<PIP_ADAPTER_ADDRESSES>(buffer.data());
            result = GetAdaptersAddresses(
                AF_UNSPEC, flags, nullptr, addrs, &bufLen);
        }

        if (result != NO_ERROR) return false;

        for (PIP_ADAPTER_ADDRESSES a = addrs; a != nullptr; a = a->Next) {
            if (a->OperStatus != IfOperStatusUp) continue;
            if (a->IfType == IF_TYPE_SOFTWARE_LOOPBACK) continue;

            for (PIP_ADAPTER_UNICAST_ADDRESS ua = a->FirstUnicastAddress;
                ua != nullptr; ua = ua->Next) {

                SOCKADDR* sa = ua->Address.lpSockaddr;
                if (sa->sa_family == AF_INET) {
                    sockaddr_in* sin = reinterpret_cast<sockaddr_in*>(sa);
                    if (sin->sin_addr.s_addr != 0 &&
                        sin->sin_addr.s_addr != htonl(INADDR_LOOPBACK)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // ============================================================
    //  Kiểm tra PC có adapter WiFi đang UP không
    //  (khác với hasActiveNetwork — chỉ cần có mạng bất kỳ)
    // ============================================================
    static bool hasWifiAdapter() {
        ULONG bufLen = 15000;
        std::vector<uint8_t> buffer(bufLen);

        ULONG flags = GAA_FLAG_SKIP_ANYCAST |
            GAA_FLAG_SKIP_MULTICAST |
            GAA_FLAG_SKIP_DNS_SERVER;

        PIP_ADAPTER_ADDRESSES addrs =
            reinterpret_cast<PIP_ADAPTER_ADDRESSES>(buffer.data());

        ULONG result = GetAdaptersAddresses(
            AF_UNSPEC, flags, nullptr, addrs, &bufLen);

        if (result == ERROR_BUFFER_OVERFLOW) {
            buffer.resize(bufLen);
            addrs = reinterpret_cast<PIP_ADAPTER_ADDRESSES>(buffer.data());
            result = GetAdaptersAddresses(
                AF_UNSPEC, flags, nullptr, addrs, &bufLen);
        }

        if (result != NO_ERROR) return false;

        for (PIP_ADAPTER_ADDRESSES a = addrs; a != nullptr; a = a->Next) {
            // IF_TYPE_IEEE80211 = WiFi
            if (a->IfType == IF_TYPE_IEEE80211 &&
                a->OperStatus == IfOperStatusUp) {
                return true;
            }
        }
        return false;
    }

    // ============================================================
    //  Callback — Windows gọi khi interface thay đổi
    //  ⚠️ Chạy trên thread riêng của Windows
    //  ⚠️ KHÔNG gọi Win32 API nặng trong lock
    // ============================================================
    static void CALLBACK onInterfaceChanged(
        PVOID /*context*/,
        PMIB_IPINTERFACE_ROW /*row*/,
        MIB_NOTIFICATION_TYPE type)
    {
        if (type != MibParameterNotification &&
            type != MibAddInstance &&
            type != MibDeleteInstance) {
            return;
        }

        // Kiểm tra mạng NGOÀI lock (Win32 API có thể chậm)
        bool nowUp = hasActiveNetwork();
        bool prevUp = gNetworkUp.exchange(nowUp);

        if (prevUp == nowUp) return;  // Không có thay đổi

        // Đọc callback TRONG lock — tránh race với NetworkMonitor_Stop()
        std::function<void()> cb;
        {
            std::lock_guard<std::mutex> l(gNetLock);
            cb = nowUp ? gOnNetworkRestored : gOnNetworkLost;
        }

        if (cb) {
            std::thread([cb]() { cb(); }).detach();
        }
    }

    // ============================================================
    //  Start / Stop
    // ============================================================
    void NetworkMonitor_Start(
        std::function<void()> onLost,
        std::function<void()> onRestored)
    {
        std::lock_guard<std::mutex> l(gNetLock);

        if (gNotifyHandle) return;

        gOnNetworkLost = onLost;
        gOnNetworkRestored = onRestored;
        gNetworkUp.store(hasActiveNetwork());

        DWORD err = NotifyIpInterfaceChange(
            AF_UNSPEC,
            onInterfaceChanged,
            nullptr,
            FALSE,
            &gNotifyHandle);

        if (err != NO_ERROR) {
            gNotifyHandle = nullptr;
        }
    }

    void NetworkMonitor_Stop()
    {
        std::lock_guard<std::mutex> l(gNetLock);
        if (gNotifyHandle) {
            CancelMibChangeNotify2(gNotifyHandle);
            gNotifyHandle = nullptr;
        }
        gOnNetworkLost = nullptr;
        gOnNetworkRestored = nullptr;
    }

    bool NetworkMonitor_HasWifiAdapter() {
        return hasWifiAdapter();
    }

    bool NetworkMonitor_IsUp() {
        return gNetworkUp.load();
    }

    bool NetworkMonitor_HasActive() {
        return hasActiveNetwork();
    }

} // namespace mouseKeyboard