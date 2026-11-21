// main.cpp
#include <Windows.h>
#include <commctrl.h>
#include <mmsystem.h>
#include "MainWindow.h"
#include "ServerManager.h"
#include "TrayIcon.h"
#include "AppSettings.h"

#pragma comment(lib, "comctl32.lib")
#pragma comment(lib, "user32.lib")
#pragma comment(lib, "winmm.lib")

// ===== GLOBAL =====
HINSTANCE      g_hInstance = NULL;
ServerManager* g_serverManager = NULL;
TrayIcon* g_trayIcon = NULL;
MainWindow* g_mainWindow = NULL;
HANDLE         g_hMutex = NULL;

// ===== ENTRY POINT =====
int WINAPI wWinMain(
    _In_     HINSTANCE hInstance,
    _In_opt_ HINSTANCE hPrevInstance,
    _In_     LPWSTR    lpCmdLine,
    _In_     int       nCmdShow)
{
    UNREFERENCED_PARAMETER(hPrevInstance);
    UNREFERENCED_PARAMETER(lpCmdLine);
    UNREFERENCED_PARAMETER(nCmdShow);

    // 1. Tăng độ phân giải timer
    timeBeginPeriod(1);

    // 2. Single instance
    g_hMutex = CreateMutexW(NULL, TRUE,
        L"HybridMouseControlServer_SingleInstance");

    if (g_hMutex == NULL)
    {
        MessageBoxW(NULL, L"Cannot create mutex!",
            L"Error", MB_OK | MB_ICONERROR);
        timeEndPeriod(1);
        return 1;
    }

    if (GetLastError() == ERROR_ALREADY_EXISTS)
    {
        HWND hWnd = FindWindowW(L"HybridMouseControlClass",
            L"Hybrid Mouse Control");

        if (hWnd != NULL)
        {
            SetForegroundWindow(hWnd);
            ShowWindow(hWnd, SW_RESTORE);
            FlashWindow(hWnd, TRUE);
        }
        else
        {
            MessageBoxW(NULL,
                L"Application is already running!\n"
                L"Please check the system tray.",
                L"Hybrid Mouse Control",
                MB_OK | MB_ICONINFORMATION);
        }

        CloseHandle(g_hMutex);
        timeEndPeriod(1);
        return 0;
    }

    // 3. Init Common Controls
    INITCOMMONCONTROLSEX icc;
    icc.dwSize = sizeof(icc);
    icc.dwICC = ICC_WIN95_CLASSES | ICC_STANDARD_CLASSES;
    InitCommonControlsEx(&icc);

    g_hInstance = hInstance;

    // 4. Load Settings
    AppSettings::Load();

    // 5. ServerManager
    g_serverManager = new ServerManager();

    // 6. Main Window
    g_mainWindow = new MainWindow(g_hInstance);
    if (!g_mainWindow->Create())
    {
        MessageBoxW(NULL, L"Failed to create window!",
            L"Error", MB_OK | MB_ICONERROR);
        delete g_mainWindow;
        g_mainWindow = NULL;
        timeEndPeriod(1);
        return 1;
    }

    g_mainWindow->RefreshAllUI();

    // 7. Tray Icon
    g_trayIcon = new TrayIcon(g_mainWindow->GetHandle(), g_hInstance);
    g_trayIcon->Create();

    g_trayIcon->UpdateAutoStartServer(AppSettings::AutoStartServer);
    g_trayIcon->UpdateAutoRunWithWindows(AppSettings::AutoRunWithWindows);

    // 8. Auto Start Server
    if (AppSettings::AutoStartServer)
    {
        if (g_serverManager->Start())
        {
            g_trayIcon->UpdateServerStatus(true);
            g_mainWindow->UpdateServerStatus(true);
        }
    }

    // 9. Message Loop
    MSG msg = { 0 };
    while (GetMessageW(&msg, NULL, 0, 0))
    {
        TranslateMessage(&msg);
        DispatchMessageW(&msg);
    }

    // 10. Cleanup
    if (g_serverManager != NULL)
    {
        if (g_serverManager->IsRunning())
            g_serverManager->Stop();
        delete g_serverManager;
        g_serverManager = NULL;
    }

    if (g_trayIcon != NULL)
    {
        delete g_trayIcon;
        g_trayIcon = NULL;
    }

    if (g_mainWindow != NULL)
    {
        delete g_mainWindow;
        g_mainWindow = NULL;
    }

    if (g_hMutex != NULL)
    {
        ReleaseMutex(g_hMutex);
        CloseHandle(g_hMutex);
        g_hMutex = NULL;
    }

    // 11. Trả timer
    timeEndPeriod(1);

    return (int)msg.wParam;
}