// MainWindow.h
#pragma once
#include <Windows.h>
#include <string>
#include "TrayIcon.h"
#include "HybridMouseControlDLL.h"

class MainWindow
{
public:
    MainWindow(HINSTANCE hInstance);
    ~MainWindow();

    bool Create();
    HWND GetHandle() const { return m_hWnd; }

    void UpdateServerStatus(bool running);
    void UpdatePasswordDisplay();
    void UpdateBluetoothDisplay();
    void UpdatePortsDisplay();
    void UpdateModeDisplay();
    void RefreshAllUI();

    void AppendLog(const std::string& message);
    void ClearLog();

    bool IsServerRunning() const { return m_serverRunning; }

    void OnTrayStartServer();
    void OnTrayStopServer();
    void OnTrayShowWindow();
    void OnTrayExit();
    void OnTrayAutoStartChanged(bool checked);
    void OnTrayAutoRunChanged(bool checked);
    void OnTrayDoubleClick();

private:
    static LRESULT CALLBACK WndProc(HWND, UINT, WPARAM, LPARAM);
    LRESULT HandleMessage(UINT, WPARAM, LPARAM);

    void CreateControls();
    void LayoutControls();
    void ApplyFonts();

    // Command handlers
    void OnStartStopServer();
    void OnToggleMode();
    void OnEditPorts();
    void OnEditPassword();
    void OnEditUuid();
    void OnEditBleName();
    void OnClearLog();
    void OnAutoRunChanged();
    void OnAutoStartChanged();
    void OnTimerUpdate();

    // Dialogs
    void ShowEditPortsDialog();
    void ShowEditPasswordDialog();
    void ShowEditUuidDialog();
    void ShowEditBleNameDialog();

private:
    HINSTANCE m_hInstance{ NULL };
    HWND      m_hWnd{ NULL };

    // Header
    HWND m_lblTitle{ NULL };
    HWND m_lblSubtitle{ NULL };
    HWND m_lblVersion{ NULL };

    // Row 1
    HWND m_btnStartStop{ NULL };
    HWND m_btnClearLog{ NULL };

    // Row 2: Mode
    HWND m_lblModeTitle{ NULL };
    HWND m_lblModeValue{ NULL };
    HWND m_btnModeToggle{ NULL };

    // Row 3: TCP Port
    HWND m_lblPortsTitle{ NULL };
    HWND m_lblPortsValue{ NULL };

    // Row 4: Password
    HWND m_lblPasswordTitle{ NULL };
    HWND m_lblPasswordValue{ NULL };
    HWND m_btnPassword{ NULL };

    // Row 5: BLE Name
    HWND m_lblBleNameTitle{ NULL };
    HWND m_lblBleNameValue{ NULL };

    // Row 6: UUID
    HWND m_lblUuidTitle{ NULL };
    HWND m_lblUuidValue{ NULL };

    // Row 7: STATE (chỉ State)
    HWND m_lblStatusTitle{ NULL };
    HWND m_lblStatusValue{ NULL };

    // Row 8: Checkboxes
    HWND m_chkAutoRun{ NULL };
    HWND m_chkAutoStart{ NULL };

    // Log
    HWND m_editLog{ NULL };

    // Fonts
    HFONT m_hFont{ NULL };
    HFONT m_hFontBold{ NULL };
    HFONT m_hFontConsolas{ NULL };
    HFONT m_hFontTitle{ NULL };
    HFONT m_hFontSubtitle{ NULL };

    // Brushes
    HBRUSH m_hbrBackground{ NULL };
    HBRUSH m_hbrCard{ NULL };

    // State
    bool       m_serverRunning{ false };
    bool       m_isExiting{ false };
    HID_Mode   m_currentMode{ HID_MODE_BLE };
    COLORREF   m_statusColor{ RGB(108, 117, 125) };
};