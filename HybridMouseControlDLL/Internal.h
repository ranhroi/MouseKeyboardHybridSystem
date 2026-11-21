#pragma once

namespace mouseKeyboard {

    // ============================================================
    //  Config
    // ============================================================
    struct Config {
        std::wstring host = L"0.0.0.0";
        uint16_t     port = 2000;
        std::wstring password = L"123456";
        std::wstring bleName = L"MOUSE_KEYBOARD_HYBRID";
        std::wstring service = L"2D2A0001-8A5A-4E76-A2E3-1E57D9A1B001";
        std::wstring writeUuid = L"2D2A0002-8A5A-4E76-A2E3-1E57D9A1B001";
        std::wstring notifyUuid = L"2D2A0003-8A5A-4E76-A2E3-1E57D9A1B001";
        uint32_t     timeoutMs = 5000;
        uint32_t     retryMs = 3000;
        bool         failover = false;
        bool         autoEnableBt = false;
        HID_Mode     startMode = HID_MODE_BLE;
    };

    // ============================================================
    //  Keyboard event queue
    // ============================================================
    enum KbEventType : uint8_t {
        KBEV_SET_MODIFIERS = 1,
        KBEV_KEY_DOWN = 2,
        KBEV_KEY_UP = 3,
        KBEV_KEY_TAP = 4,

        KBEV_CONSUMER_TAP = 5,
        KBEV_CONSUMER_DOWN = 6,
        KBEV_CONSUMER_UP = 7,
        KBEV_SYSTEM_ACTION = 8
    };

    struct KbEvent {
        uint8_t  type;
        uint8_t  a;
        uint8_t  b;
        uint16_t usage;
    };

    // ============================================================
    //  InputEngine — SendInput-based (thay USB HID)
    // ============================================================
    class InputEngine {
    public:
        InputEngine();
        ~InputEngine();

        void mouseMove(int8_t dx, int8_t dy);
        void mouseScroll(int8_t v, int8_t h);
        void mouseClick(uint8_t button);
        void mouseDown(uint8_t button);
        void mouseUp(uint8_t button);

        void enqueueSetModifiers(uint8_t mask);
        void enqueueKeyDown(uint8_t usage);
        void enqueueKeyUp(uint8_t usage);
        void enqueueKeyTap(uint8_t modifiers, uint8_t usage);
        void enqueueConsumer(uint8_t cmd, uint16_t usage);
        void enqueueSystemAction(uint8_t action);

        void releaseAll();
        void process(const uint8_t* data, size_t len);
        void startWorker();
        void stopWorker();

    private:
        void setModifiers(uint8_t m);
        void keyDown(uint8_t usage);
        void keyUp(uint8_t usage);
        void keyTap(uint8_t modifiers, uint8_t usage);
        void textUtf8(const uint8_t* data, size_t len);
        void changeBrightness(int delta);

        void consumerAction(uint8_t cmd, uint16_t usage);
        void systemAction(uint8_t action);
        void legacy(const uint8_t* d, size_t n);
        void v2(const uint8_t* d, size_t n);
        void workerLoop();

        bool     keysDown_[256]{};
        uint8_t  modifiers_ = 0;
        uint8_t  buttons_ = 0;
        std::mutex stateMutex_;

        std::deque<KbEvent>     kbQueue_;
        std::mutex              kbQueueMutex_;
        std::condition_variable kbQueueCv_;
        std::thread             kbWorker_;
        std::atomic<bool>       kbStop_{ false };
    };

    // ============================================================
    //  Auth state — dùng chung cho TCP + BLE
    // ============================================================
    struct AuthState {
        bool identified = false;
        bool verified = false;
        void reset() { identified = false; verified = false; }
    };

    // ============================================================
    //  Shared instance — định nghĩa trong ServerCore.cpp
    // ============================================================
    extern InputEngine gInput;

    // ============================================================
    //  Config accessor
    // ============================================================
    const Config& getConfig();
    void          setConfig(const Config& cfg);

    // ============================================================
    //  Protocol helpers
    // ============================================================
    size_t legacyFrameSize(const std::vector<uint8_t>& b);
    bool   isV2Command(uint8_t c);

    void handleProtocolFrame(const uint8_t* p, size_t n, bool ble);
    bool sendProtocolResponse(bool ble, const uint8_t* p, size_t n);
    bool passwordMatches(const uint8_t* p, size_t n);
    void sendLedStatusResponse(bool ble);
    void currentAppRequest(bool ble);
    void screenshotRequest(bool ble);

    // ============================================================
    //  Status notifier
    // ============================================================
    void notifyStatus(HID_Status s, HID_Transport t, const wchar_t* m);

    // ============================================================
    //  Auth reset helper
    // ============================================================
    void resetAuthForTransport(bool ble);

    // ============================================================
    //  TCP Server (TcpServer.cpp)
    // ============================================================
    bool TcpServer_Start();
    void TcpServer_Stop();
    bool TcpServer_IsRunning();
    bool TcpServer_Send(const uint8_t* data, size_t len);
    bool TcpServer_HasClient();

    // ============================================================
    //  NetworkMonitor (NetworkMonitor.cpp)
    // ============================================================
    void NetworkMonitor_Start(std::function<void()> onLost,
        std::function<void()> onRestored);
    void NetworkMonitor_Stop();
    bool NetworkMonitor_IsUp();
    bool NetworkMonitor_HasActive();
    bool NetworkMonitor_HasWifiAdapter();

    // ============================================================
    //  BluetoothMonitor (BluetoothMonitor.cpp)
    // ============================================================
    void BluetoothMonitor_Start(std::function<void()> onLost,
        std::function<void()> onRestored);
    void BluetoothMonitor_Stop();
    bool BluetoothMonitor_IsAvailable();

    // ============================================================
    //  BLE GATT Server (BleGattServer.cpp)
    // ============================================================
    bool BleGattServer_Start(const Config& cfg);
    void BleGattServer_Stop();
    bool BleGattServer_Send(const uint8_t* data, size_t len);
    void BleGattDisconnected();
    void BleGattDispatch(const uint8_t* data, size_t len);

    // ============================================================
    //  Utilities
    // ============================================================
    unsigned long long nowMs();

} // namespace mouseKeyboard