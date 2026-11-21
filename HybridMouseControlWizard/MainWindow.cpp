// MainWindow.cpp
#include "MainWindow.h"
#include "ServerManager.h"
#include "AppSettings.h"
#include "TrayIcon.h"
#include <commctrl.h>
#include <string>
#include <sstream>
#include <cstdio>
#include <thread>

// ===== EXTERNAL =====
extern ServerManager* g_serverManager;
extern TrayIcon* g_trayIcon;

// ===== CONTROL IDs =====
#define ID_BTN_STARTSTOP     1001
#define ID_BTN_CLEARLOG      1002
#define ID_BTN_MODE_TOGGLE   1003
#define ID_BTN_PASSWORD      1004
#define ID_LBL_PORTS         1010
#define ID_LBL_PASSWORD      1011
#define ID_LBL_BLE_NAME      1012
#define ID_LBL_UUID          1013
#define ID_LBL_STATUS        1020
#define ID_EDIT_LOG          1030
#define ID_CHK_AUTORUN       1040
#define ID_CHK_AUTOSTART     1041

// ===== TIMER =====
#define TIMER_UPDATE_ID       1
#define TIMER_UPDATE_INTERVAL 500

// ===== COLORS =====
#define CLR_BG          RGB(245, 247, 250)
#define CLR_CARD        RGB(255, 255, 255)
#define CLR_TEXT        RGB(33, 37, 41)
#define CLR_TEXT_MUTED  RGB(108, 117, 125)
#define CLR_ACCENT      RGB(0, 120, 215)
#define CLR_GREEN       RGB(40, 167, 69)
#define CLR_BLUE        RGB(0, 123, 255)
#define CLR_ORANGE      RGB(255, 152, 0)
#define CLR_RED         RGB(220, 53, 69)

// ===== CONSTRUCTOR =====
MainWindow::MainWindow(HINSTANCE hInstance)
    : m_hInstance(hInstance)
{
    m_hFont = CreateFontW(-13, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE,
        DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS,
        CLEARTYPE_QUALITY, DEFAULT_PITCH | FF_DONTCARE, L"Segoe UI");

    m_hFontBold = CreateFontW(-13, 0, 0, 0, FW_SEMIBOLD, FALSE, FALSE, FALSE,
        DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS,
        CLEARTYPE_QUALITY, DEFAULT_PITCH | FF_DONTCARE, L"Segoe UI");

    m_hFontConsolas = CreateFontW(-12, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE,
        DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS,
        CLEARTYPE_QUALITY, DEFAULT_PITCH | FF_DONTCARE, L"Consolas");

    m_hFontTitle = CreateFontW(-20, 0, 0, 0, FW_BOLD, FALSE, FALSE, FALSE,
        DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS,
        CLEARTYPE_QUALITY, DEFAULT_PITCH | FF_DONTCARE, L"Segoe UI");

    m_hFontSubtitle = CreateFontW(-12, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE,
        DEFAULT_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS,
        CLEARTYPE_QUALITY, DEFAULT_PITCH | FF_DONTCARE, L"Segoe UI");

    m_hbrBackground = CreateSolidBrush(CLR_BG);
    m_hbrCard = CreateSolidBrush(CLR_CARD);
}

MainWindow::~MainWindow()
{
    if (m_hFont)         DeleteObject(m_hFont);
    if (m_hFontBold)     DeleteObject(m_hFontBold);
    if (m_hFontConsolas) DeleteObject(m_hFontConsolas);
    if (m_hFontTitle)    DeleteObject(m_hFontTitle);
    if (m_hFontSubtitle) DeleteObject(m_hFontSubtitle);
    if (m_hbrBackground) DeleteObject(m_hbrBackground);
    if (m_hbrCard)       DeleteObject(m_hbrCard);
}

// ===== CREATE WINDOW =====
bool MainWindow::Create()
{
    WNDCLASSEXW wc = { 0 };
    wc.cbSize = sizeof(WNDCLASSEXW);
    wc.style = CS_HREDRAW | CS_VREDRAW;
    wc.lpfnWndProc = WndProc;
    wc.hInstance = m_hInstance;
    wc.hCursor = LoadCursor(NULL, IDC_ARROW);
    wc.hbrBackground = m_hbrBackground;
    wc.lpszClassName = L"HybridMouseControlServer";
    wc.hIcon = LoadIcon(m_hInstance, MAKEINTRESOURCE(101));
    wc.hIconSm = wc.hIcon;

    if (!RegisterClassExW(&wc))
        return false;

    m_hWnd = CreateWindowExW(
        0, L"HybridMouseControlServer", L"Hybrid Mouse Control Server",
        WS_OVERLAPPED | WS_CAPTION | WS_SYSMENU | WS_MINIMIZEBOX,
        CW_USEDEFAULT, CW_USEDEFAULT, 560, 660,
        NULL, NULL, m_hInstance, this);

    if (!m_hWnd) return false;

    CreateControls();
    RefreshAllUI();

    SetTimer(m_hWnd, TIMER_UPDATE_ID, TIMER_UPDATE_INTERVAL, NULL);

    ShowWindow(m_hWnd, SW_SHOW);
    UpdateWindow(m_hWnd);

    return true;
}

// ===== CREATE CONTROLS =====
void MainWindow::CreateControls()
{
    // ===== HEADER =====
    m_lblTitle = CreateWindowW(L"STATIC", L"Hybrid Mouse Control Server",
        WS_CHILD | WS_VISIBLE | SS_LEFT,
        0, 0, 0, 0, m_hWnd, NULL, m_hInstance, NULL);

    m_lblSubtitle = CreateWindowW(L"STATIC",
        L"Hybrid mouse control server for mobile devices",
        WS_CHILD | WS_VISIBLE | SS_LEFT,
        0, 0, 0, 0, m_hWnd, NULL, m_hInstance, NULL);

    // ===== ROW 1: START/STOP + CLEAR LOG =====
    m_btnStartStop = CreateWindowW(L"BUTTON", L"\x25B6  Start Server",
        WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
        0, 0, 0, 0, m_hWnd, (HMENU)ID_BTN_STARTSTOP, m_hInstance, NULL);

    m_btnClearLog = CreateWindowW(L"BUTTON", L"Clear Log",
        WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
        0, 0, 0, 0, m_hWnd, (HMENU)ID_BTN_CLEARLOG, m_hInstance, NULL);

    // ===== ROW 2: MODE (BLE ↔ WIFI) =====
    m_lblModeTitle = CreateWindowW(L"STATIC", L"Mode",
        WS_CHILD | WS_VISIBLE | SS_LEFT,
        0, 0, 0, 0, m_hWnd, NULL, m_hInstance, NULL);

    m_lblModeValue = CreateWindowW(L"STATIC", L"Bluetooth (BLE)",
        WS_CHILD | WS_VISIBLE | SS_LEFT,
        0, 0, 0, 0, m_hWnd, NULL, m_hInstance, NULL);

    m_btnModeToggle = CreateWindowW(L"BUTTON", L"Switch to WiFi",
        WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
        0, 0, 0, 0, m_hWnd, (HMENU)ID_BTN_MODE_TOGGLE, m_hInstance, NULL);

    // ===== ROW 3: TCP PORT =====
    m_lblPortsTitle = CreateWindowW(L"STATIC", L"TCP Port",
        WS_CHILD | WS_VISIBLE | SS_LEFT,
        0, 0, 0, 0, m_hWnd, NULL, m_hInstance, NULL);

    m_lblPortsValue = CreateWindowExW(WS_EX_STATICEDGE, L"STATIC", L"",
        WS_CHILD | WS_VISIBLE | SS_CENTER | SS_NOTIFY | SS_CENTERIMAGE,
        0, 0, 0, 0, m_hWnd, (HMENU)ID_LBL_PORTS, m_hInstance, NULL);

    // ===== ROW 4: PASSWORD (label + value + button) =====
    m_lblPasswordTitle = CreateWindowW(L"STATIC", L"Password",
        WS_CHILD | WS_VISIBLE | SS_LEFT,
        0, 0, 0, 0, m_hWnd, NULL, m_hInstance, NULL);

    m_lblPasswordValue = CreateWindowExW(WS_EX_STATICEDGE, L"STATIC", L"",
        WS_CHILD | WS_VISIBLE | SS_CENTER | SS_NOTIFY | SS_CENTERIMAGE,
        0, 0, 0, 0, m_hWnd, (HMENU)ID_LBL_PASSWORD, m_hInstance, NULL);

    m_btnPassword = CreateWindowW(L"BUTTON", L"Change",
        WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
        0, 0, 0, 0, m_hWnd, (HMENU)ID_BTN_PASSWORD, m_hInstance, NULL);

    // ===== ROW 5: BLE NAME =====
    m_lblBleNameTitle = CreateWindowW(L"STATIC", L"BLE Device Name",
        WS_CHILD | WS_VISIBLE | SS_LEFT,
        0, 0, 0, 0, m_hWnd, NULL, m_hInstance, NULL);

    m_lblBleNameValue = CreateWindowExW(WS_EX_STATICEDGE, L"STATIC", L"",
        WS_CHILD | WS_VISIBLE | SS_CENTER | SS_NOTIFY | SS_CENTERIMAGE,
        0, 0, 0, 0, m_hWnd, (HMENU)ID_LBL_BLE_NAME, m_hInstance, NULL);

    // ===== ROW 6: UUID =====
    m_lblUuidTitle = CreateWindowW(L"STATIC", L"BLE Service UUID",
        WS_CHILD | WS_VISIBLE | SS_LEFT,
        0, 0, 0, 0, m_hWnd, NULL, m_hInstance, NULL);

    m_lblUuidValue = CreateWindowExW(WS_EX_STATICEDGE, L"STATIC", L"",
        WS_CHILD | WS_VISIBLE | SS_CENTER | SS_NOTIFY | SS_CENTERIMAGE,
        0, 0, 0, 0, m_hWnd, (HMENU)ID_LBL_UUID, m_hInstance, NULL);

    // ===== ROW 7: STATE =====
    m_lblStatusTitle = CreateWindowW(L"STATIC", L"State:",
        WS_CHILD | WS_VISIBLE | SS_LEFT,
        0, 0, 0, 0, m_hWnd, NULL, m_hInstance, NULL);

    m_lblStatusValue = CreateWindowW(L"STATIC", L"Stopped",
        WS_CHILD | WS_VISIBLE | SS_LEFT,
        0, 0, 0, 0, m_hWnd, (HMENU)ID_LBL_STATUS, m_hInstance, NULL);

    // ===== ROW 8: CHECKBOXES =====
    m_chkAutoRun = CreateWindowW(L"BUTTON", L"Auto run with Windows",
        WS_CHILD | WS_VISIBLE | BS_AUTOCHECKBOX,
        0, 0, 0, 0, m_hWnd, (HMENU)ID_CHK_AUTORUN, m_hInstance, NULL);

    m_chkAutoStart = CreateWindowW(L"BUTTON", L"Auto start server",
        WS_CHILD | WS_VISIBLE | BS_AUTOCHECKBOX,
        0, 0, 0, 0, m_hWnd, (HMENU)ID_CHK_AUTOSTART, m_hInstance, NULL);

    // ===== LOG =====
    m_editLog = CreateWindowExW(WS_EX_CLIENTEDGE, L"EDIT", L"",
        WS_CHILD | WS_VISIBLE | WS_VSCROLL |
        ES_MULTILINE | ES_READONLY | ES_AUTOVSCROLL,
        0, 0, 0, 0, m_hWnd, (HMENU)ID_EDIT_LOG, m_hInstance, NULL);

    // ===== VERSION =====
    m_lblVersion = CreateWindowW(L"STATIC",
        L"v1.0  |  Hybrid Mouse Control Server",
        WS_CHILD | WS_VISIBLE | SS_CENTER,
        0, 0, 0, 0, m_hWnd, NULL, m_hInstance, NULL);

    ApplyFonts();
    LayoutControls();
}

// ===== APPLY FONTS =====
void MainWindow::ApplyFonts()
{
    SendMessageW(m_lblTitle, WM_SETFONT, (WPARAM)m_hFontTitle, TRUE);
    SendMessageW(m_lblSubtitle, WM_SETFONT, (WPARAM)m_hFontSubtitle, TRUE);
    SendMessageW(m_lblVersion, WM_SETFONT, (WPARAM)m_hFontSubtitle, TRUE);

    SendMessageW(m_btnStartStop, WM_SETFONT, (WPARAM)m_hFontBold, TRUE);
    SendMessageW(m_btnClearLog, WM_SETFONT, (WPARAM)m_hFont, TRUE);
    SendMessageW(m_btnModeToggle, WM_SETFONT, (WPARAM)m_hFontBold, TRUE);
    SendMessageW(m_btnPassword, WM_SETFONT, (WPARAM)m_hFont, TRUE);

    SendMessageW(m_lblModeTitle, WM_SETFONT, (WPARAM)m_hFont, TRUE);
    SendMessageW(m_lblModeValue, WM_SETFONT, (WPARAM)m_hFontBold, TRUE);
    SendMessageW(m_lblPortsTitle, WM_SETFONT, (WPARAM)m_hFont, TRUE);
    SendMessageW(m_lblPasswordTitle, WM_SETFONT, (WPARAM)m_hFont, TRUE);
    SendMessageW(m_lblBleNameTitle, WM_SETFONT, (WPARAM)m_hFont, TRUE);
    SendMessageW(m_lblUuidTitle, WM_SETFONT, (WPARAM)m_hFont, TRUE);

    SendMessageW(m_lblPortsValue, WM_SETFONT, (WPARAM)m_hFontBold, TRUE);
    SendMessageW(m_lblPasswordValue, WM_SETFONT, (WPARAM)m_hFontBold, TRUE);
    SendMessageW(m_lblBleNameValue, WM_SETFONT, (WPARAM)m_hFontBold, TRUE);
    SendMessageW(m_lblUuidValue, WM_SETFONT, (WPARAM)m_hFontBold, TRUE);

    SendMessageW(m_lblStatusTitle, WM_SETFONT, (WPARAM)m_hFont, TRUE);
    SendMessageW(m_lblStatusValue, WM_SETFONT, (WPARAM)m_hFontBold, TRUE);

    SendMessageW(m_chkAutoRun, WM_SETFONT, (WPARAM)m_hFont, TRUE);
    SendMessageW(m_chkAutoStart, WM_SETFONT, (WPARAM)m_hFont, TRUE);
    SendMessageW(m_editLog, WM_SETFONT, (WPARAM)m_hFontConsolas, TRUE);
}

// ===== LAYOUT CONTROLS =====
void MainWindow::LayoutControls()
{
    RECT rc;
    GetClientRect(m_hWnd, &rc);
    int W = rc.right;
    int M = 24;
    int cardX = M;
    int cardW = W - M * 2;
    int labelW = 130;
    int valueX = cardX + labelW + 12;
    int valueW = cardW - labelW - 24;
    int rowH = 32;

    int y = 20;

    // ===== Header =====
    MoveWindow(m_lblTitle, M, y, W - M * 2, 30, TRUE); y += 30;
    MoveWindow(m_lblSubtitle, M, y, W - M * 2, 20, TRUE); y += 32;

    // ===== Row 1: Start/Stop + Clear Log =====
    int btnW = (cardW - 12) / 2;
    MoveWindow(m_btnStartStop, M, y, btnW, 40, TRUE);
    MoveWindow(m_btnClearLog, M + btnW + 12, y, btnW, 40, TRUE);
    y += 52;

    // ===== Row 2: Mode =====
    MoveWindow(m_lblModeTitle, cardX, y + 6, labelW, 22, TRUE);
    MoveWindow(m_lblModeValue, valueX, y + 6, valueW, 22, TRUE);
    y += rowH;

    MoveWindow(m_btnModeToggle, M, y, cardW, 36, TRUE);
    y += 48;

    // ===== Row 3: TCP Port =====
    MoveWindow(m_lblPortsTitle, cardX, y + 6, labelW, 22, TRUE);
    MoveWindow(m_lblPortsValue, valueX, y, valueW, 30, TRUE);
    y += rowH + 8;

    // ===== Row 4: Password =====
    int passBtnW = 100;
    int passValueW = valueW - passBtnW - 12;

    MoveWindow(m_lblPasswordTitle, cardX, y + 6, labelW, 22, TRUE);
    MoveWindow(m_lblPasswordValue, valueX, y, passValueW, 30, TRUE);
    MoveWindow(m_btnPassword, valueX + passValueW + 12, y, passBtnW, 30, TRUE);
    y += rowH + 8;

    // ===== Row 5: BLE Name =====
    MoveWindow(m_lblBleNameTitle, cardX, y + 6, labelW, 22, TRUE);
    MoveWindow(m_lblBleNameValue, valueX, y, valueW, 30, TRUE);
    y += rowH + 8;

    // ===== Row 6: UUID =====
    MoveWindow(m_lblUuidTitle, cardX, y + 6, labelW, 22, TRUE);
    MoveWindow(m_lblUuidValue, valueX, y, valueW, 30, TRUE);
    y += rowH + 12;

    // ===== Row 7: STATE =====
    MoveWindow(m_lblStatusTitle, cardX, y + 4, labelW, 22, TRUE);
    MoveWindow(m_lblStatusValue, valueX, y + 4, valueW, 22, TRUE);
    y += rowH + 12;

    // ===== Row 8: Checkboxes =====
    MoveWindow(m_chkAutoRun, M, y, 200, 24, TRUE);
    MoveWindow(m_chkAutoStart, M + cardW / 2, y, 200, 24, TRUE);
    y += 32;

    // ===== Log =====
    int logH = 110;
    MoveWindow(m_editLog, M, y, cardW, logH, TRUE);
    y += logH + 8;

    // ===== Version =====
    MoveWindow(m_lblVersion, M, y, cardW, 20, TRUE);
}

// ===== WINDOW PROC =====
LRESULT CALLBACK MainWindow::WndProc(HWND hWnd, UINT msg, WPARAM wParam, LPARAM lParam)
{
    MainWindow* pThis = NULL;

    if (msg == WM_NCCREATE)
    {
        CREATESTRUCT* pCreate = (CREATESTRUCT*)lParam;
        pThis = (MainWindow*)pCreate->lpCreateParams;
        SetWindowLongPtrW(hWnd, GWLP_USERDATA, (LONG_PTR)pThis);
        pThis->m_hWnd = hWnd;
    }
    else
    {
        pThis = (MainWindow*)GetWindowLongPtrW(hWnd, GWLP_USERDATA);
    }

    if (pThis)
        return pThis->HandleMessage(msg, wParam, lParam);

    return DefWindowProcW(hWnd, msg, wParam, lParam);
}

// ===== HANDLE MESSAGE =====
LRESULT MainWindow::HandleMessage(UINT msg, WPARAM wParam, LPARAM lParam)
{
    switch (msg)
    {
    case WM_COMMAND:
    {
        int id = LOWORD(wParam);
        int code = HIWORD(wParam);

        switch (id)
        {
        case ID_LBL_STATUS:
            if (code == STN_CLICKED) OnStartStopServer();
            break;
        case ID_BTN_STARTSTOP:
            if (code == BN_CLICKED) OnStartStopServer();
            break;

        case ID_BTN_CLEARLOG:
            if (code == BN_CLICKED) OnClearLog();
            break;

        case ID_BTN_MODE_TOGGLE:
            if (code == BN_CLICKED) OnToggleMode();
            break;

        case ID_BTN_PASSWORD:
            if (code == BN_CLICKED) OnEditPassword();
            break;

        case ID_LBL_PORTS:
            if (code == STN_CLICKED) OnEditPorts();
            break;

        case ID_LBL_PASSWORD:
            if (code == STN_CLICKED) OnEditPassword();
            break;

        case ID_LBL_BLE_NAME:
            if (code == STN_CLICKED) OnEditBleName();
            break;

        case ID_LBL_UUID:
            if (code == STN_CLICKED) OnEditUuid();
            break;

        case ID_CHK_AUTORUN:
            if (code == BN_CLICKED) OnAutoRunChanged();
            break;

        case ID_CHK_AUTOSTART:
            if (code == BN_CLICKED) OnAutoStartChanged();
            break;
        }
    }
    break;

    case WM_TIMER:
        if (wParam == TIMER_UPDATE_ID) OnTimerUpdate();
        break;

    case WM_TRAYICON:
    {
        UINT trayMsg = (UINT)lParam;

        if (trayMsg == WM_RBUTTONUP)
        {
            POINT pt;
            GetCursorPos(&pt);
            SetForegroundWindow(m_hWnd);

            if (g_trayIcon != nullptr)
            {
                UINT cmd = TrackPopupMenu(
                    g_trayIcon->GetMenu(),
                    TPM_RETURNCMD | TPM_RIGHTBUTTON,
                    pt.x, pt.y, 0, m_hWnd, NULL);

                switch (cmd)
                {
                case TRAY_MENU_START_SERVER: OnTrayStartServer(); break;
                case TRAY_MENU_STOP_SERVER:  OnTrayStopServer();  break;
                case TRAY_MENU_AUTO_START:
                    OnTrayAutoStartChanged(!g_trayIcon->IsAutoStartServerChecked());
                    break;
                case TRAY_MENU_AUTO_RUN:
                    OnTrayAutoRunChanged(!g_trayIcon->IsAutoRunWithWindowsChecked());
                    break;
                case TRAY_MENU_SHOW_WINDOW:  OnTrayShowWindow();  break;
                case TRAY_MENU_EXIT:         OnTrayExit();        break;
                }
            }
        }
        else if (trayMsg == WM_LBUTTONDBLCLK)
        {
            OnTrayDoubleClick();
        }
    }
    break;

    case WM_CTLCOLORSTATIC:
    {
        HDC hdc = (HDC)wParam;
        HWND hCtrl = (HWND)lParam;

        SetBkMode(hdc, TRANSPARENT);

        if (hCtrl == m_lblModeTitle ||
            hCtrl == m_lblPortsTitle ||
            hCtrl == m_lblPasswordTitle ||
            hCtrl == m_lblBleNameTitle ||
            hCtrl == m_lblUuidTitle ||
            hCtrl == m_lblStatusTitle ||
            hCtrl == m_lblSubtitle ||
            hCtrl == m_lblVersion)
        {
            SetTextColor(hdc, CLR_TEXT_MUTED);
            return (LRESULT)m_hbrBackground;
        }

        if (hCtrl == m_lblStatusValue)
        {
            // Dùng màu theo status động
            SetTextColor(hdc, m_statusColor);
            return (LRESULT)m_hbrBackground;
        }
        if (hCtrl == m_lblModeValue)
        {
            SetTextColor(hdc, CLR_ORANGE);
            return (LRESULT)m_hbrBackground;
        }

        SetTextColor(hdc, CLR_TEXT);
        return (LRESULT)m_hbrBackground;
    }

    case WM_CTLCOLOREDIT:
    {
        HDC hdc = (HDC)wParam;
        SetTextColor(hdc, CLR_TEXT);
        SetBkColor(hdc, CLR_CARD);
        return (LRESULT)m_hbrCard;
    }

    case WM_ENDSESSION:
        if (g_serverManager && g_serverManager->IsRunning())
            g_serverManager->Stop();
        AppSettings::Save();
        break;

    case WM_QUERYENDSESSION:
        return TRUE;

    case WM_CLOSE:
        if (m_isExiting) DestroyWindow(m_hWnd);
        else             ShowWindow(m_hWnd, SW_HIDE);
        return 0;

    case WM_DESTROY:
        KillTimer(m_hWnd, TIMER_UPDATE_ID);
        if (g_trayIcon != nullptr) g_trayIcon->Destroy();
        PostQuitMessage(0);
        return 0;

    case WM_SIZE:
        LayoutControls();
        break;
    }

    return DefWindowProcW(m_hWnd, msg, wParam, lParam);
}

// ===== TIMER =====
void MainWindow::OnTimerUpdate()
{
    if (m_isExiting) return;
    if (g_serverManager == nullptr) return;

    // ============================================================
    //  1. SERVER RUNNING STATUS + LIVE STATUS
    // ============================================================
    bool running = g_serverManager->IsRunning();

    if (running != m_serverRunning) {
        m_serverRunning = running;
        if (g_trayIcon != nullptr)
            g_trayIcon->UpdateServerStatus(running);

        // Cập nhật nút Start/Stop
        SetWindowTextW(m_btnStartStop,
            running ? L"\x25A0  Stop Server" : L"\x25B6  Start Server");
    }

    // Hiển thị status chi tiết từ DLL
    if (m_serverRunning) {
        HID_Status status = g_serverManager->GetStatus();
        HID_Transport transport = g_serverManager->GetTransport();

        LPCWSTR statusText = L"Running";
        COLORREF statusColor = CLR_GREEN;

        switch (status) {
        case HID_STATUS_IDLE:
            if (transport == HID_TRANSPORT_NONE) {
                statusText = L"Waiting for network...";
                statusColor = CLR_ORANGE;
            }
            else if (transport == HID_TRANSPORT_WIFI) {
                statusText = L"Waiting for Wi-Fi client...";
                statusColor = CLR_BLUE;
            }
            else if (transport == HID_TRANSPORT_BLE) {
                statusText = L"Waiting for BLE client...";
                statusColor = CLR_BLUE;
            }
            break;
        case HID_STATUS_CONNECTING:
            statusText = L"Connecting...";
            statusColor = CLR_BLUE;
            break;
        case HID_STATUS_AUTHENTICATING:
            statusText = L"Authenticating...";
            statusColor = CLR_BLUE;
            break;
        case HID_STATUS_CONNECTED_WIFI:
            statusText = L"Connected (Wi-Fi)";
            statusColor = CLR_GREEN;
            break;
        case HID_STATUS_CONNECTED_BLE:
            statusText = L"Connected (BLE)";
            statusColor = CLR_GREEN;
            break;
        case HID_STATUS_SWITCHING:
            statusText = L"Switching mode...";
            statusColor = CLR_ORANGE;
            break;
        case HID_STATUS_ERROR:
            statusText = L"Error";
            statusColor = CLR_RED;
            break;
        case HID_STATUS_STOPPING:
            statusText = L"Stopping...";
            statusColor = CLR_ORANGE;
            break;
        }

        wchar_t currentText[128] = { 0 };
        GetWindowTextW(m_lblStatusValue, currentText, 128);
        if (wcscmp(currentText, statusText) != 0 ||
            m_statusColor != statusColor) {
            SetWindowTextW(m_lblStatusValue, statusText);
            m_statusColor = statusColor;
            InvalidateRect(m_lblStatusValue, NULL, TRUE);
        }
    }
    else {
        // Đã stop → hiển thị "Stopped"
        wchar_t currentText[128] = { 0 };
        GetWindowTextW(m_lblStatusValue, currentText, 128);
        if (wcscmp(currentText, L"Stopped") != 0) {
            SetWindowTextW(m_lblStatusValue, L"Stopped");
            m_statusColor = CLR_TEXT_MUTED;
            InvalidateRect(m_lblStatusValue, NULL, TRUE);
        }
    }

    // ============================================================
    //  2. MODE — đọc transport từ DLL, cập nhật UI + config
    // ============================================================
    HID_Transport tp = g_serverManager->GetLiveTransport();

    HID_Mode liveMode = m_currentMode;
    if (tp == HID_TRANSPORT_BLE)       liveMode = HID_MODE_BLE;
    else if (tp == HID_TRANSPORT_WIFI) liveMode = HID_MODE_WIFI;

    if (liveMode != m_currentMode)
    {
        m_currentMode = liveMode;
        UpdateModeDisplay();
        InvalidateRect(m_lblModeValue, NULL, TRUE);

        g_serverManager->SyncModeFromDLL();
    }

    // ============================================================
    //  3. LOG — pop hết queue mỗi tick
    // ============================================================
    std::string log;
    while (!(log = g_serverManager->PopLog()).empty()) {
        AppendLog(log);
    }
}

// ===== COMMAND HANDLERS =====
void MainWindow::OnStartStopServer()
{
    if (m_serverRunning)
    {
        if (g_serverManager != nullptr) g_serverManager->Stop();
        m_serverRunning = false;
        SetWindowTextW(m_btnStartStop, L"\x25B6  Start Server");
        SetWindowTextW(m_lblStatusValue, L"Stopped");
        m_statusColor = CLR_TEXT_MUTED;
    }
    else
    {
        if (g_serverManager != nullptr && g_serverManager->Start())
        {
            m_serverRunning = true;
            SetWindowTextW(m_btnStartStop, L"\x25A0  Stop Server");
            // Status text sẽ được OnTimerUpdate cập nhật
            SetWindowTextW(m_lblStatusValue, L"Starting...");
            m_statusColor = CLR_BLUE;
        }
        else
        {
            MessageBoxW(m_hWnd,
                L"Failed to start server!\n\nPlease check port and password.",
                L"Error", MB_OK | MB_ICONERROR);
        }
    }

    if (g_trayIcon != nullptr) g_trayIcon->UpdateServerStatus(m_serverRunning);

    InvalidateRect(m_lblStatusValue, NULL, TRUE);
    RedrawWindow(m_hWnd, NULL, NULL, RDW_INVALIDATE | RDW_ALLCHILDREN);
}

void MainWindow::OnClearLog()
{
    SetWindowTextW(m_editLog, L"");
}

void MainWindow::OnToggleMode()
{
    HID_Mode newMode = (m_currentMode == HID_MODE_BLE)
        ? HID_MODE_WIFI
        : HID_MODE_BLE;

    bool ok = false;
    if (g_serverManager != nullptr)
        ok = g_serverManager->SwitchMode(newMode);

    if (ok)
    {
        m_currentMode = newMode;
        UpdateModeDisplay();
        InvalidateRect(m_lblModeValue, NULL, TRUE);
        RedrawWindow(m_hWnd, NULL, NULL, RDW_INVALIDATE | RDW_ALLCHILDREN);
    }
    else
    {
        MessageBoxW(m_hWnd, L"Failed to switch mode!", L"Error", MB_OK | MB_ICONERROR);
    }
}

void MainWindow::OnEditPorts() { ShowEditPortsDialog(); }
void MainWindow::OnEditPassword() { ShowEditPasswordDialog(); }
void MainWindow::OnEditUuid() { ShowEditUuidDialog(); }
void MainWindow::OnEditBleName() { ShowEditBleNameDialog(); }


void MainWindow::OnAutoRunChanged()
{
    BOOL checked = (BOOL)SendMessageW(m_chkAutoRun, BM_GETCHECK, 0, 0);
    AppSettings::AutoRunWithWindows = (checked == BST_CHECKED);
    AppSettings::Save();
    if (g_trayIcon != nullptr)
        g_trayIcon->UpdateAutoRunWithWindows(AppSettings::AutoRunWithWindows);
}

void MainWindow::OnAutoStartChanged()
{
    BOOL checked = (BOOL)SendMessageW(m_chkAutoStart, BM_GETCHECK, 0, 0);
    AppSettings::AutoStartServer = (checked == BST_CHECKED);
    AppSettings::Save();
    if (g_trayIcon != nullptr)
        g_trayIcon->UpdateAutoStartServer(AppSettings::AutoStartServer);

    if (AppSettings::AutoStartServer && !m_serverRunning)
        OnStartStopServer();
}

// ===== UPDATE UI =====
void MainWindow::UpdateServerStatus(bool running)
{
    m_serverRunning = running;
    SetWindowTextW(m_btnStartStop,
        running ? L"\x25A0  Stop Server" : L"\x25B6  Start Server");

    if (running) {
        m_statusColor = CLR_GREEN;
        // Text sẽ được OnTimerUpdate cập nhật chi tiết
    }
    else {
        SetWindowTextW(m_lblStatusValue, L"Stopped");
        m_statusColor = CLR_TEXT_MUTED;
    }

    InvalidateRect(m_lblStatusValue, NULL, TRUE);
}


void MainWindow::UpdatePasswordDisplay()
{
    if (!AppSettings::Password.empty())
    {
        wchar_t wpass[64] = { 0 };
        MultiByteToWideChar(CP_ACP, 0, AppSettings::Password.c_str(), -1, wpass, 64);
        SetWindowTextW(m_lblPasswordValue, wpass);
    }
    else
    {
        SetWindowTextW(m_lblPasswordValue, L"(not set)");
    }
}

void MainWindow::UpdateBluetoothDisplay()
{
    if (!AppSettings::BleDeviceName.empty())
    {
        std::wstring name(AppSettings::BleDeviceName.begin(),
            AppSettings::BleDeviceName.end());
        SetWindowTextW(m_lblBleNameValue, name.c_str());
    }
    else
    {
        SetWindowTextW(m_lblBleNameValue, L"(not set)");
    }

    if (!AppSettings::BleServiceUuid.empty())
    {
        std::wstring uuid(AppSettings::BleServiceUuid.begin(),
            AppSettings::BleServiceUuid.end());
        SetWindowTextW(m_lblUuidValue, uuid.c_str());
    }
    else
    {
        SetWindowTextW(m_lblUuidValue, L"(not set)");
    }
}

void MainWindow::UpdatePortsDisplay()
{
    wchar_t buf[32];
    swprintf_s(buf, L"%d", AppSettings::TCPPort);
    SetWindowTextW(m_lblPortsValue, buf);
}

void MainWindow::UpdateModeDisplay()
{
    if (m_currentMode == HID_MODE_BLE)
    {
        SetWindowTextW(m_lblModeValue, L"Bluetooth (BLE)");
        SetWindowTextW(m_btnModeToggle, L"Switch to WiFi");
    }
    else
    {
        SetWindowTextW(m_lblModeValue, L"WiFi / TCP");
        SetWindowTextW(m_btnModeToggle, L"Switch to Bluetooth");
    }
}

void MainWindow::RefreshAllUI()
{
    m_currentMode = (HID_Mode)AppSettings::StartMode;

    UpdatePortsDisplay();
    UpdatePasswordDisplay();
    UpdateBluetoothDisplay();
    UpdateModeDisplay();

    SendMessageW(m_chkAutoRun, BM_SETCHECK,
        AppSettings::AutoRunWithWindows ? BST_CHECKED : BST_UNCHECKED, 0);
    SendMessageW(m_chkAutoStart, BM_SETCHECK,
        AppSettings::AutoStartServer ? BST_CHECKED : BST_UNCHECKED, 0);

    UpdateServerStatus(m_serverRunning);
}

// ===== LOG =====
void MainWindow::AppendLog(const std::string& message)
{
    if (message.empty()) return;

    // Giới hạn: chỉ giữ 30 dòng gần nhất
    static const int MAX_LOG_LINES = 30;

    // Đếm số dòng hiện tại
    int lineCount = (int)SendMessageW(m_editLog, EM_GETLINECOUNT, 0, 0);

    // Tính số dòng cần xóa từ đầu để sau khi append còn đúng MAX_LOG_LINES
    int linesToRemove = lineCount - MAX_LOG_LINES + 1;
    if (linesToRemove > 0) {
        // Lấy vị trí ký tự bắt đầu của dòng cần giữ đầu tiên
        int charPos = (int)SendMessageW(m_editLog, EM_LINEINDEX, linesToRemove, 0);
        if (charPos > 0) {
            // Chọn từ đầu đến vị trí đó và xóa
            SendMessageW(m_editLog, EM_SETSEL, 0, charPos);
            SendMessageW(m_editLog, EM_REPLACESEL, FALSE, (LPARAM)L"");
        }
    }

    // Append dòng mới
    int len = GetWindowTextLengthW(m_editLog);
    SendMessageW(m_editLog, EM_SETSEL, len, len);

    std::wstring wmsg(message.begin(), message.end());
    std::wstring text = wmsg + L"\r\n";

    SendMessageW(m_editLog, EM_REPLACESEL, FALSE, (LPARAM)text.c_str());
    SendMessageW(m_editLog, EM_SCROLLCARET, 0, 0);
}

void MainWindow::ClearLog() { SetWindowTextW(m_editLog, L""); }

// ===== TRAY HANDLERS =====
void MainWindow::OnTrayStartServer() { if (!m_serverRunning) OnStartStopServer(); }
void MainWindow::OnTrayStopServer() { if (m_serverRunning)  OnStartStopServer(); }

void MainWindow::OnTrayShowWindow()
{
    ShowWindow(m_hWnd, SW_SHOW);
    ShowWindow(m_hWnd, SW_RESTORE);
    SetForegroundWindow(m_hWnd);
}

void MainWindow::OnTrayDoubleClick() { OnTrayShowWindow(); }

void MainWindow::OnTrayExit()
{
    if (MessageBoxW(m_hWnd,
        L"Are you sure you want to exit?\n\nThe server will be stopped automatically.",
        L"Confirm Exit",
        MB_YESNO | MB_ICONQUESTION | MB_DEFBUTTON2) == IDYES)
    {
        m_isExiting = true;

        SetWindowTextW(m_lblStatusValue, L"Exiting...");
        SetWindowTextW(m_btnStartStop, L"Exiting...");

        EnableWindow(m_btnStartStop, FALSE);
        EnableWindow(m_btnClearLog, FALSE);
        EnableWindow(m_btnModeToggle, FALSE);
        EnableWindow(m_btnPassword, FALSE);
        EnableWindow(m_chkAutoRun, FALSE);
        EnableWindow(m_chkAutoStart, FALSE);

        UpdateWindow(m_hWnd);
        RedrawWindow(m_hWnd, NULL, NULL, RDW_UPDATENOW | RDW_ALLCHILDREN);

        KillTimer(m_hWnd, TIMER_UPDATE_ID);
        if (g_trayIcon != nullptr) g_trayIcon->Destroy();

        HWND hWndCopy = m_hWnd;
        std::thread([hWndCopy]() {
            if (g_serverManager != nullptr && g_serverManager->IsRunning())
                g_serverManager->Stop();
            AppSettings::Save();
            PostMessageW(hWndCopy, WM_CLOSE, 0, 0);
            }).detach();
    }
}

void MainWindow::OnTrayAutoStartChanged(bool checked)
{
    SendMessageW(m_chkAutoStart, BM_SETCHECK,
        checked ? BST_CHECKED : BST_UNCHECKED, 0);
    AppSettings::AutoStartServer = checked;
    AppSettings::Save();
    if (g_trayIcon != nullptr) g_trayIcon->UpdateAutoStartServer(checked);
}

void MainWindow::OnTrayAutoRunChanged(bool checked)
{
    SendMessageW(m_chkAutoRun, BM_SETCHECK,
        checked ? BST_CHECKED : BST_UNCHECKED, 0);
    AppSettings::AutoRunWithWindows = checked;
    AppSettings::Save();
    if (g_trayIcon != nullptr) g_trayIcon->UpdateAutoRunWithWindows(checked);
}

// ===== DIALOG: EDIT PORTS =====
void MainWindow::ShowEditPortsDialog()
{
    HWND hDlg = CreateWindowExW(
        WS_EX_DLGMODALFRAME, L"#32770", L"Edit TCP Port",
        WS_POPUP | WS_CAPTION | WS_SYSMENU | DS_MODALFRAME,
        CW_USEDEFAULT, CW_USEDEFAULT, 340, 170,
        m_hWnd, NULL, m_hInstance, NULL);
    if (!hDlg) return;

    RECT rcP, rcD;
    GetWindowRect(m_hWnd, &rcP);
    GetWindowRect(hDlg, &rcD);
    int x = rcP.left + ((rcP.right - rcP.left) - (rcD.right - rcD.left)) / 2;
    int y = rcP.top + ((rcP.bottom - rcP.top) - (rcD.bottom - rcD.top)) / 2;
    SetWindowPos(hDlg, NULL, x, y, 0, 0, SWP_NOSIZE | SWP_NOZORDER);

    CreateWindowW(L"STATIC", L"TCP Port:",
        WS_CHILD | WS_VISIBLE | SS_RIGHT,
        15, 30, 90, 22, hDlg, NULL, m_hInstance, NULL);

    HWND hTcp = CreateWindowExW(WS_EX_CLIENTEDGE, L"EDIT", L"",
        WS_CHILD | WS_VISIBLE | ES_NUMBER | ES_AUTOHSCROLL,
        115, 28, 190, 24, hDlg, (HMENU)100, m_hInstance, NULL);

    wchar_t buf[16];
    swprintf_s(buf, L"%d", AppSettings::TCPPort);
    SetWindowTextW(hTcp, buf);

    CreateWindowW(L"STATIC", L"Range: 1 - 65535",
        WS_CHILD | WS_VISIBLE | SS_LEFT,
        115, 58, 190, 18, hDlg, NULL, m_hInstance, NULL);

    CreateWindowW(L"BUTTON", L"OK",
        WS_CHILD | WS_VISIBLE | BS_DEFPUSHBUTTON,
        130, 90, 80, 28, hDlg, (HMENU)IDOK, m_hInstance, NULL);
    CreateWindowW(L"BUTTON", L"Cancel",
        WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
        225, 90, 80, 28, hDlg, (HMENU)IDCANCEL, m_hInstance, NULL);

    SendMessageW(hTcp, WM_SETFONT, (WPARAM)m_hFont, TRUE);
    ShowWindow(hDlg, SW_SHOW);
    SetFocus(hTcp);

    MSG msg; bool done = false, ok = false;
    while (!done && GetMessageW(&msg, NULL, 0, 0))
    {
        if (msg.message == WM_KEYDOWN && msg.wParam == VK_RETURN) { ok = true; done = true; }
        else if (msg.message == WM_KEYDOWN && msg.wParam == VK_ESCAPE) { done = true; }
        else if (msg.message == WM_COMMAND)
        {
            if (LOWORD(msg.wParam) == IDOK) { ok = true; done = true; }
            else if (LOWORD(msg.wParam) == IDCANCEL) { done = true; }
        }
        else if (msg.message == WM_CLOSE) { done = true; }
        if (IsDialogMessageW(hDlg, &msg)) continue;
        TranslateMessage(&msg);
        DispatchMessageW(&msg);
    }

    if (ok)
    {
        wchar_t tcpBuf[16] = { 0 };
        GetWindowTextW(hTcp, tcpBuf, 16);
        int tcpPort = _wtoi(tcpBuf);

        if (tcpPort >= 1 && tcpPort <= 65535)
        {
            AppSettings::TCPPort = tcpPort;
            AppSettings::Save();
            UpdatePortsDisplay();
        }
        else
        {
            MessageBoxW(m_hWnd, L"Invalid port! Range: 1-65535.",
                L"Error", MB_OK | MB_ICONERROR);
        }
    }

    DestroyWindow(hDlg);
}

// ===== DIALOG: EDIT PASSWORD =====
void MainWindow::ShowEditPasswordDialog()
{
    HWND hDlg = CreateWindowExW(
        WS_EX_DLGMODALFRAME, L"#32770", L"Change Password",
        WS_POPUP | WS_CAPTION | WS_SYSMENU | DS_MODALFRAME,
        CW_USEDEFAULT, CW_USEDEFAULT, 400, 220,
        m_hWnd, NULL, m_hInstance, NULL);
    if (!hDlg) return;

    RECT rcP, rcD;
    GetWindowRect(m_hWnd, &rcP);
    GetWindowRect(hDlg, &rcD);
    int x = rcP.left + ((rcP.right - rcP.left) - (rcD.right - rcD.left)) / 2;
    int y = rcP.top + ((rcP.bottom - rcP.top) - (rcD.bottom - rcD.top)) / 2;
    SetWindowPos(hDlg, NULL, x, y, 0, 0, SWP_NOSIZE | SWP_NOZORDER);

    CreateWindowW(L"STATIC", L"New Password:",
        WS_CHILD | WS_VISIBLE | SS_RIGHT,
        15, 25, 110, 22, hDlg, NULL, m_hInstance, NULL);

    HWND hEdit = CreateWindowExW(WS_EX_CLIENTEDGE, L"EDIT", L"",
        WS_CHILD | WS_VISIBLE | ES_AUTOHSCROLL,
        135, 23, 230, 24, hDlg, (HMENU)100, m_hInstance, NULL);

    wchar_t wpass[64] = { 0 };
    MultiByteToWideChar(CP_ACP, 0, AppSettings::Password.c_str(), -1, wpass, 64);
    SetWindowTextW(hEdit, wpass);

    CreateWindowW(L"STATIC", L"Enter 4-20 characters",
        WS_CHILD | WS_VISIBLE | SS_LEFT,
        135, 52, 230, 18, hDlg, NULL, m_hInstance, NULL);

    // Nút "Random"
    CreateWindowW(L"BUTTON", L"Random",
        WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
        135, 80, 100, 28, hDlg, (HMENU)200, m_hInstance, NULL);

    CreateWindowW(L"BUTTON", L"OK",
        WS_CHILD | WS_VISIBLE | BS_DEFPUSHBUTTON,
        150, 130, 100, 30, hDlg, (HMENU)IDOK, m_hInstance, NULL);
    CreateWindowW(L"BUTTON", L"Cancel",
        WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
        265, 130, 100, 30, hDlg, (HMENU)IDCANCEL, m_hInstance, NULL);

    SendMessageW(hEdit, WM_SETFONT, (WPARAM)m_hFont, TRUE);
    ShowWindow(hDlg, SW_SHOW);
    SetFocus(hEdit);

    MSG msg; bool done = false, ok = false;
    while (!done && GetMessageW(&msg, NULL, 0, 0))
    {
        if (msg.message == WM_KEYDOWN && msg.wParam == VK_RETURN) { ok = true; done = true; }
        else if (msg.message == WM_KEYDOWN && msg.wParam == VK_ESCAPE) { done = true; }
        else if (msg.message == WM_COMMAND)
        {
            int cid = LOWORD(msg.wParam);
            if (cid == IDOK) { ok = true; done = true; }
            else if (cid == IDCANCEL) { done = true; }
            else if (cid == 200)
            {
                // Random password
                std::string rnd = AppSettings::GenerateRandomPassword(6);
                std::wstring wrnd(rnd.begin(), rnd.end());
                SetWindowTextW(hEdit, wrnd.c_str());
            }
        }
        else if (msg.message == WM_CLOSE) { done = true; }
        if (IsDialogMessageW(hDlg, &msg)) continue;
        TranslateMessage(&msg);
        DispatchMessageW(&msg);
    }

    if (ok)
    {
        wchar_t newPass[64] = { 0 };
        GetWindowTextW(hEdit, newPass, 64);
        size_t len = wcslen(newPass);

        if (len < 4 || len > 20)
        {
            MessageBoxW(m_hWnd, L"Password must be 4-20 characters!",
                L"Error", MB_OK | MB_ICONERROR);
        }
        else
        {
            char ansi[64] = { 0 };
            WideCharToMultiByte(CP_ACP, 0, newPass, -1, ansi, 64, NULL, NULL);
            AppSettings::Password = std::string(ansi);
            AppSettings::Save();
            UpdatePasswordDisplay();
        }
    }

    DestroyWindow(hDlg);
}

// ===== DIALOG: EDIT BLE NAME =====
void MainWindow::ShowEditBleNameDialog()
{
    HWND hDlg = CreateWindowExW(
        WS_EX_DLGMODALFRAME, L"#32770", L"Edit BLE Device Name",
        WS_POPUP | WS_CAPTION | WS_SYSMENU | DS_MODALFRAME,
        CW_USEDEFAULT, CW_USEDEFAULT, 420, 170,
        m_hWnd, NULL, m_hInstance, NULL);
    if (!hDlg) return;

    RECT rcP, rcD;
    GetWindowRect(m_hWnd, &rcP);
    GetWindowRect(hDlg, &rcD);
    int x = rcP.left + ((rcP.right - rcP.left) - (rcD.right - rcD.left)) / 2;
    int y = rcP.top + ((rcP.bottom - rcP.top) - (rcD.bottom - rcD.top)) / 2;
    SetWindowPos(hDlg, NULL, x, y, 0, 0, SWP_NOSIZE | SWP_NOZORDER);

    CreateWindowW(L"STATIC", L"Name:",
        WS_CHILD | WS_VISIBLE | SS_RIGHT,
        15, 30, 70, 22, hDlg, NULL, m_hInstance, NULL);

    HWND hEdit = CreateWindowExW(WS_EX_CLIENTEDGE, L"EDIT", L"",
        WS_CHILD | WS_VISIBLE | ES_AUTOHSCROLL,
        95, 28, 300, 24, hDlg, (HMENU)100, m_hInstance, NULL);

    std::wstring name(AppSettings::BleDeviceName.begin(), AppSettings::BleDeviceName.end());
    SetWindowTextW(hEdit, name.c_str());

    CreateWindowW(L"STATIC", L"1-31 characters",
        WS_CHILD | WS_VISIBLE | SS_LEFT,
        95, 58, 300, 18, hDlg, NULL, m_hInstance, NULL);

    CreateWindowW(L"BUTTON", L"OK",
        WS_CHILD | WS_VISIBLE | BS_DEFPUSHBUTTON,
        180, 90, 80, 28, hDlg, (HMENU)IDOK, m_hInstance, NULL);
    CreateWindowW(L"BUTTON", L"Cancel",
        WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
        275, 90, 80, 28, hDlg, (HMENU)IDCANCEL, m_hInstance, NULL);

    SendMessageW(hEdit, WM_SETFONT, (WPARAM)m_hFont, TRUE);
    ShowWindow(hDlg, SW_SHOW);
    SetFocus(hEdit);

    MSG msg; bool done = false, ok = false;
    while (!done && GetMessageW(&msg, NULL, 0, 0))
    {
        if (msg.message == WM_KEYDOWN && msg.wParam == VK_RETURN) { ok = true; done = true; }
        else if (msg.message == WM_KEYDOWN && msg.wParam == VK_ESCAPE) { done = true; }
        else if (msg.message == WM_COMMAND)
        {
            if (LOWORD(msg.wParam) == IDOK) { ok = true; done = true; }
            else if (LOWORD(msg.wParam) == IDCANCEL) { done = true; }
        }
        else if (msg.message == WM_CLOSE) { done = true; }
        if (IsDialogMessageW(hDlg, &msg)) continue;
        TranslateMessage(&msg);
        DispatchMessageW(&msg);
    }

    if (ok)
    {
        wchar_t newName[64] = { 0 };
        GetWindowTextW(hEdit, newName, 64);
        size_t len = wcslen(newName);

        if (len < 1 || len > 31)
        {
            MessageBoxW(m_hWnd, L"Name must be 1-31 characters!",
                L"Error", MB_OK | MB_ICONERROR);
        }
        else
        {
            char ansi[64] = { 0 };
            WideCharToMultiByte(CP_ACP, 0, newName, -1, ansi, 64, NULL, NULL);
            AppSettings::BleDeviceName = std::string(ansi);
            AppSettings::Save();
            UpdateBluetoothDisplay();
        }
    }

    DestroyWindow(hDlg);
}

// ===== DIALOG: EDIT UUID =====
void MainWindow::ShowEditUuidDialog()
{
    HWND hDlg = CreateWindowExW(
        WS_EX_DLGMODALFRAME, L"#32770", L"Edit BLE UUIDs",
        WS_POPUP | WS_CAPTION | WS_SYSMENU | DS_MODALFRAME,
        CW_USEDEFAULT, CW_USEDEFAULT, 540, 280,
        m_hWnd, NULL, m_hInstance, NULL);
    if (!hDlg) return;

    RECT rcP, rcD;
    GetWindowRect(m_hWnd, &rcP);
    GetWindowRect(hDlg, &rcD);
    int x = rcP.left + ((rcP.right - rcP.left) - (rcD.right - rcD.left)) / 2;
    int y = rcP.top + ((rcP.bottom - rcP.top) - (rcD.bottom - rcD.top)) / 2;
    SetWindowPos(hDlg, NULL, x, y, 0, 0, SWP_NOSIZE | SWP_NOZORDER);

    auto makeEdit = [&](int yy, const wchar_t* label,
        const std::string& val, int id) -> HWND {
            CreateWindowW(L"STATIC", label,
                WS_CHILD | WS_VISIBLE | SS_RIGHT,
                15, yy + 4, 110, 22, hDlg, NULL, m_hInstance, NULL);
            HWND h = CreateWindowExW(WS_EX_CLIENTEDGE, L"EDIT", L"",
                WS_CHILD | WS_VISIBLE | ES_AUTOHSCROLL,
                135, yy, 385, 24, hDlg, (HMENU)(INT_PTR)id,
                m_hInstance, NULL);
            std::wstring w(val.begin(), val.end());
            SetWindowTextW(h, w.c_str());
            SendMessageW(h, WM_SETFONT, (WPARAM)m_hFont, TRUE);
            return h;
        };

    HWND hService = makeEdit(20, L"Service UUID:", AppSettings::BleServiceUuid, 100);
    HWND hWrite = makeEdit(55, L"Write UUID:", AppSettings::BleWriteUuid, 101);
    HWND hNotify = makeEdit(90, L"Notify UUID:", AppSettings::BleNotifyUuid, 102);

    CreateWindowW(L"STATIC",
        L"Format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
        WS_CHILD | WS_VISIBLE | SS_LEFT,
        135, 125, 385, 18, hDlg, NULL, m_hInstance, NULL);

    CreateWindowW(L"BUTTON", L"OK",
        WS_CHILD | WS_VISIBLE | BS_DEFPUSHBUTTON,
        290, 180, 100, 30, hDlg, (HMENU)IDOK, m_hInstance, NULL);
    CreateWindowW(L"BUTTON", L"Cancel",
        WS_CHILD | WS_VISIBLE | BS_PUSHBUTTON,
        410, 180, 100, 30, hDlg, (HMENU)IDCANCEL, m_hInstance, NULL);

    ShowWindow(hDlg, SW_SHOW);
    SetFocus(hService);

    MSG msg; bool done = false, ok = false;
    while (!done && GetMessageW(&msg, NULL, 0, 0))
    {
        if (msg.message == WM_KEYDOWN && msg.wParam == VK_RETURN) { ok = true; done = true; }
        else if (msg.message == WM_KEYDOWN && msg.wParam == VK_ESCAPE) { done = true; }
        else if (msg.message == WM_COMMAND)
        {
            if (LOWORD(msg.wParam) == IDOK) { ok = true; done = true; }
            else if (LOWORD(msg.wParam) == IDCANCEL) { done = true; }
        }
        else if (msg.message == WM_CLOSE) { done = true; }
        if (IsDialogMessageW(hDlg, &msg)) continue;
        TranslateMessage(&msg);
        DispatchMessageW(&msg);
    }

    if (ok)
    {
        wchar_t buf[128];
        char ansi[128];

        GetWindowTextW(hService, buf, 128);
        if (wcslen(buf) >= 32) {
            ZeroMemory(ansi, sizeof(ansi));
            WideCharToMultiByte(CP_ACP, 0, buf, -1, ansi, 128, NULL, NULL);
            AppSettings::BleServiceUuid = std::string(ansi);
        }

        GetWindowTextW(hWrite, buf, 128);
        if (wcslen(buf) >= 32) {
            ZeroMemory(ansi, sizeof(ansi));
            WideCharToMultiByte(CP_ACP, 0, buf, -1, ansi, 128, NULL, NULL);
            AppSettings::BleWriteUuid = std::string(ansi);
        }

        GetWindowTextW(hNotify, buf, 128);
        if (wcslen(buf) >= 32) {
            ZeroMemory(ansi, sizeof(ansi));
            WideCharToMultiByte(CP_ACP, 0, buf, -1, ansi, 128, NULL, NULL);
            AppSettings::BleNotifyUuid = std::string(ansi);
        }

        AppSettings::Save();
        UpdateBluetoothDisplay();
    }

    DestroyWindow(hDlg);
}