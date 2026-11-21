/* BleGattServer.cpp
 *
 * Windows BLE GATT peripheral/server.
 * YÃªu cáº§u: Windows 10 SDK 10.0.19041+ + C++/WinRT + /std:c++17 + /await
 *
 * FIXES:
 *   1. Include thá»© tá»± Ä‘Ãºng: winrt/* trÆ°á»›c Internal.h
 *   2. NotifyValueAsync tráº£ vá» IVectorView<GattClientNotificationResult>
 *      KHÃ”NG PHáº¢I IAsyncOperation<GattCommunicationStatus>
 *   3. winsock2.h pháº£i trÆ°á»›c windows.h (Ä‘Ã£ fix trong Internal.h)
 *
 * SAFETY (giá»‘ng firmware ESP32 clearAllHardwareStates):
 *   - Client má»›i connect        â†’ reset auth + clear HID
 *   - Client thá»© 2 vÃ o          â†’ reset auth + clear HID
 *   - Client disconnect háº¿t     â†’ reset auth + clear HID
 *   - Cooldown 500ms chá»‘ng spam
 *   - Fallback detect qua WriteRequested
 */

#include "pch.h"

 // ============================================================
 //  using namespace
 // ============================================================
using namespace winrt;
using namespace Windows::Foundation;
using namespace Windows::Foundation::Collections;
using namespace Windows::Storage::Streams;
using namespace Windows::Devices::Bluetooth;
using namespace Windows::Devices::Bluetooth::GenericAttributeProfile;
using namespace Windows::Devices::Bluetooth::Advertisement;

namespace mouseKeyboard {

    // ============================================================
    //  State
    // ============================================================
    static std::mutex bleMutex;
    static GattServiceProvider provider{ nullptr };
    static GattLocalCharacteristic writeChar{ nullptr };
    static GattLocalCharacteristic notifyChar{ nullptr };
    static event_token writeToken{};
    static event_token subscribedToken{};
    static bool running = false;

    // ============================================================
    //  Client tracking â€” cÆ¡ cháº¿ an toÃ n
    // ============================================================
    static std::atomic<int>  gBleSubscribedCount{ 0 };
    static std::atomic<bool> gBleHadClient{ false };
    static std::atomic<unsigned long long> gBleLastClientChangeMs{ 0 };
    static const unsigned long long BLE_CLIENT_COOLDOWN_MS = 500;

    // ============================================================
    //  GUID parser
    // ============================================================
    static GUID parseGuid(const std::wstring& s) {
        GUID g{};
        std::wstring tmp = s;
        RPC_STATUS rs = UuidFromStringW(
            reinterpret_cast<RPC_WSTR>(tmp.data()), &g);
        if (rs != RPC_S_OK) {
            throw hresult_invalid_argument();
        }
        return g;
    }

    // ============================================================
    //  Buffer helpers
    // ============================================================
    static IBuffer toBuffer(const uint8_t* p, size_t n) {
        DataWriter w;
        w.WriteBytes(array_view<const uint8_t>(p, p + n));
        return w.DetachBuffer();
    }

    static std::vector<uint8_t> fromBuffer(IBuffer const& b) {
        std::vector<uint8_t> v(b.Length());
        DataReader r = DataReader::FromBuffer(b);
        if (!v.empty()) {
            r.ReadBytes(v);
        }
        return v;
    }

    // ============================================================
    //  Client lifecycle â€” giá»‘ng clearAllHardwareStates() firmware
    // ============================================================
    static void onBleClientConnected() {
        unsigned long long now = nowMs();
        if (now - gBleLastClientChangeMs.load() < BLE_CLIENT_COOLDOWN_MS) {
            return;
        }
        gBleLastClientChangeMs = now;

        // Dá»n sáº¡ch HID + reset auth
        resetAuthForTransport(true);

        gBleHadClient = true;

        notifyStatus(HID_STATUS_AUTHENTICATING, HID_TRANSPORT_BLE,
            L"BLE: client má»›i káº¿t ná»‘i â€” Ä‘Ã£ reset auth");
    }

    static void onBleClientDisconnected() {
        unsigned long long now = nowMs();
        if (now - gBleLastClientChangeMs.load() < BLE_CLIENT_COOLDOWN_MS) {
            return;
        }
        gBleLastClientChangeMs = now;

        // Dá»n sáº¡ch HID + reset auth
        resetAuthForTransport(true);

        gBleHadClient = false;
        gBleSubscribedCount = 0;

        notifyStatus(HID_STATUS_IDLE, HID_TRANSPORT_BLE,
            L"BLE: client Ä‘Ã£ ngáº¯t â€” Ä‘Ã£ dá»n state");
    }

    // ============================================================
    //  Start
    // ============================================================
    bool BleGattServer_Start(const Config& cfg) {
        std::lock_guard<std::mutex> lock(bleMutex);
        if (running) return true;

        try {
            static bool apartmentInit = false;
            if (!apartmentInit) {
                init_apartment(apartment_type::multi_threaded);
                apartmentInit = true;
            }

            gBleSubscribedCount = 0;
            gBleHadClient = false;
            gBleLastClientChangeMs = 0;

            // ---- Bước 1: GUID parse ----
            GUID svcGuid, writeGuid, notifyGuid;
            try {
                svcGuid = parseGuid(cfg.service);
                writeGuid = parseGuid(cfg.writeUuid);
                notifyGuid = parseGuid(cfg.notifyUuid);
            }
            catch (...) {
                OutputDebugStringW(L"[BLE] parseGuid FAIL\n");
                return false;
            }

            // ---- Bước 2: Create service provider ----
            auto created = GattServiceProvider::CreateAsync(svcGuid).get();
            auto err = created.Error();
            if (err != BluetoothError::Success) {
                wchar_t buf[128];
                swprintf_s(buf, L"[BLE] CreateAsync FAIL: error=%d\n", (int)err);
                OutputDebugStringW(buf);
                return false;
            }
            provider = created.ServiceProvider();

            // ---- Bước 3: Write char ----
            GattLocalCharacteristicParameters wp;
            wp.CharacteristicProperties(
                GattCharacteristicProperties::Write |
                GattCharacteristicProperties::WriteWithoutResponse);
            wp.WriteProtectionLevel(GattProtectionLevel::Plain);

            auto wc = provider.Service().CreateCharacteristicAsync(
                writeGuid, wp).get();
            err = wc.Error();
            if (err != BluetoothError::Success) {
                wchar_t buf[128];
                swprintf_s(buf, L"[BLE] WriteChar FAIL: error=%d\n", (int)err);
                OutputDebugStringW(buf);
                return false;
            }
            writeChar = wc.Characteristic();

            // ---- Bước 4: Notify char ----
            GattLocalCharacteristicParameters np;
            np.CharacteristicProperties(GattCharacteristicProperties::Notify);
            np.ReadProtectionLevel(GattProtectionLevel::Plain);

            auto nc = provider.Service().CreateCharacteristicAsync(
                notifyGuid, np).get();
            err = nc.Error();
            if (err != BluetoothError::Success) {
                wchar_t buf[128];
                swprintf_s(buf, L"[BLE] NotifyChar FAIL: error=%d\n", (int)err);
                OutputDebugStringW(buf);
                return false;
            }
            notifyChar = nc.Characteristic();

            // ---- Bước 5: Callbacks ----
            writeToken = writeChar.WriteRequested(
                [](GattLocalCharacteristic const&,
                    GattWriteRequestedEventArgs const& args) {
                        try {
                            auto req = args.GetRequestAsync().get();
                            auto data = fromBuffer(req.Value());
                            if (!gBleHadClient.load()) {
                                onBleClientConnected();
                            }
                            if (data.size() <= 64) {
                                req.Respond();
                                BleGattDispatch(data.data(), data.size());
                            }
                            else {
                                req.Respond();
                            }
                        }
                        catch (...) {}
                });

            subscribedToken = notifyChar.SubscribedClientsChanged(
                [](GattLocalCharacteristic const& sender,
                    IInspectable const&) {
                        try {
                            auto clients = sender.SubscribedClients();
                            int count = (int)clients.Size();
                            int prev = gBleSubscribedCount.exchange(count);

                            if (prev == 0 && count > 0) {
                                onBleClientConnected();
                            }
                            else if (prev > 0 && count == 0) {
                                onBleClientDisconnected();
                            }
                            else if (prev > 0 && count > prev) {
                                onBleClientConnected();
                            }
                        }
                        catch (...) {}
                });

            // ---- Bước 6: Start advertising ----
            GattServiceProviderAdvertisingParameters adv;
            adv.IsConnectable(true);
            adv.IsDiscoverable(true);
            provider.StartAdvertising(adv);

            running = true;
            OutputDebugStringW(L"[BLE] Start OK\n");
            return true;
        }
        catch (winrt::hresult_error const& ex) {
            wchar_t buf[256];
            swprintf_s(buf, L"[BLE] hresult_error: 0x%08X - %s\n",
                (unsigned)ex.code(), ex.message().c_str());
            OutputDebugStringW(buf);
        }
        catch (...) {
            OutputDebugStringW(L"[BLE] Unknown exception\n");
        }

        // Cleanup
        try {
            if (provider) provider.StopAdvertising();
        }
        catch (...) {}
        provider = nullptr;
        writeChar = nullptr;
        notifyChar = nullptr;
        running = false;
        return false;
    }

    // ============================================================
    //  Stop
    // ============================================================
    void BleGattServer_Stop() {
        std::lock_guard<std::mutex> lock(bleMutex);
        if (!running) return;

        try {
            if (provider) {
                provider.StopAdvertising();
            }
            if (writeChar) {
                writeChar.WriteRequested(writeToken);
            }
            if (notifyChar) {
                notifyChar.SubscribedClientsChanged(subscribedToken);
            }
        }
        catch (...) {}

        provider = nullptr;
        writeChar = nullptr;
        notifyChar = nullptr;
        running = false;

        // Reset tracking + dá»n state
        gBleSubscribedCount = 0;
        gBleHadClient = false;
        resetAuthForTransport(true);
    }

    // ============================================================
    //  Send notify â€” FIX API ÄÃšNG
    //
    //  GattLocalCharacteristic::NotifyValueAsync() tráº£ vá»:
    //      IAsyncOperation<IVectorView<GattClientNotificationResult>>
    //
    //  KHÃ”NG PHáº¢I:
    //      IAsyncOperation<GattCommunicationStatus>
    //
    //  â†’ Pháº£i .get() Ä‘á»ƒ láº¥y IVectorView, rá»“i iterate Ä‘á»ƒ check status
    // ============================================================
    bool BleGattServer_Send(const uint8_t* p, size_t n) {
        std::lock_guard<std::mutex> lock(bleMutex);
        if (!running || !notifyChar || !p || !n) return false;

        try {
            // BÆ°á»›c 1: táº¡o IBuffer
            IBuffer buffer = toBuffer(p, n);

            // BÆ°á»›c 2: gá»i NotifyValueAsync
            auto op = notifyChar.NotifyValueAsync(buffer);

            // BÆ°á»›c 3: chá» vÃ  láº¥y IVectorView<GattClientNotificationResult>
            IVectorView<GattClientNotificationResult> results = op.get();

            // BÆ°á»›c 4: kiá»ƒm tra káº¿t quáº£
            if (results.Size() == 0) {
                return false;
            }

            bool anySuccess = false;
            for (uint32_t i = 0; i < results.Size(); ++i) {
                GattClientNotificationResult r = results.GetAt(i);
                if (r.Status() == GattCommunicationStatus::Success) {
                    anySuccess = true;
                    break;
                }
            }
            return anySuccess;
        }
        catch (...) {
            return false;
        }
    }

} // namespace mouseKeyboard