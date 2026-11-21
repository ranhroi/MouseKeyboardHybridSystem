package com.ranhroi.esp32hidremote.ui.screens.quickkey

import com.ranhroi.esp32hidremote.models.QuickKeyModel


object QuickKeysRepository {
    val quickKeysDefault: List<QuickKeyModel> by lazy {
        listOf(
            QuickKeyModel(
                openApp = true,
                name = "Cốc Cốc", preset = "Software", icon = "baseline_launch",
                keys = listOf(
                    listOf("CTRL", "ALT", "1"),
                    emptyList(),
                    emptyList()
                )
            ),
            QuickKeyModel(
                openApp = true,
                name = " Android Studio", preset = "Software", icon = "baseline_launch",
                keys = listOf(
                    listOf("CTRL", "ALT", "2"),
                    emptyList(),
                    emptyList()
                )
            ),
            // ============================================================
            // DEFAULT (Phím tắt cơ bản & phổ biến trên máy tính)
            // ============================================================
            QuickKeyModel(
                name = "Save", preset = "Default", icon = "baseline_save",
                keys = listOf(
                    listOf("CTRL", "S"),
                    listOf("CMD", "S"),
                    listOf("CTRL", "S")
                )
            ),
            QuickKeyModel(
                name = "Find", preset = "Default", icon = "baseline_search",
                keys = listOf(
                    listOf("CTRL", "F"),
                    listOf("CMD", "F"),
                    listOf("CTRL", "F")
                )
            ),
            QuickKeyModel(
                name = "Task View", preset = "Default", icon = "baseline_lock",
                keys = listOf(
                    listOf("WIN", "TAB"),            // Windows
                    listOf("CTRL", "CMD", "TAB"),    // macOS
                    listOf("WIN", "TAB")
                )
            ),
            QuickKeyModel(
                name = "Task Manager / Force Quit", preset = "Default", icon = "baseline_monitor",
                keys = listOf(
                    listOf("CTRL", "SHIFT", "ESC"), // Windows
                    listOf("CMD", "OPTION", "ESC"), // macOS
                    listOf("CTRL", "SHIFT", "ESC")
                )
            ),
            QuickKeyModel(
                name = "Switch App", preset = "Default", icon = "baseline_swap_horiz",
                keys = listOf(
                    listOf("ALT", "TAB"),
                    listOf("CMD", "TAB"),
                    listOf("ALT", "TAB")
                )
            ),
            QuickKeyModel(
                name = "Screenshot", preset = "Default", icon = "baseline_screenshot",
                keys = listOf(
                    listOf("WIN", "SHIFT", "S"),   // Windows
                    listOf("CMD", "SHIFT", "4"),   // macOS
                    listOf("WIN", "SHIFT", "S")
                )
            ),
            QuickKeyModel(
                name = "File Explorer", preset = "Default", icon = "baseline_folder",
                keys = listOf(
                    listOf("WIN", "E"),            // Windows
                    listOf("CMD", "N"),            // macOS
                    listOf("WIN", "E")
                )
            ),

            // ============================================================
            // DEFAULT — Bổ sung: File/Folder & Settings
            // ============================================================
            QuickKeyModel(
                name = "Open File Manager", preset = "Default", icon = "baseline_file_open",
                keys = listOf(
                    listOf("WIN", "E"),            // Windows: File Explorer
                    listOf("CMD", "N"),            // macOS: Finder
                    listOf("WIN", "E")
                )
            ),
            QuickKeyModel(
                name = "New Folder", preset = "Default", icon = "baseline_create_new_folder",
                keys = listOf(
                    listOf("CTRL", "SHIFT", "N"),  // Windows
                    listOf("SHIFT", "CMD", "N"),   // macOS
                    listOf("CTRL", "SHIFT", "N")
                )
            ),
            QuickKeyModel(
                name = "Open Settings", preset = "Default", icon = "baseline_settings",
                keys = listOf(
                    listOf("WIN", "I"),            // Windows 11
                    listOf("CMD", "SPACE"),        // macOS: Spotlight → gõ "System Settings"
                    listOf("WIN", "I")
                )
            ),
            QuickKeyModel(
                name = "Quick Settings", preset = "Default", icon = "baseline_tune",
                keys = listOf(
                    listOf("WIN", "A"),            // Windows 11
                    listOf("FN", "C"),             // macOS: Control Center
                    listOf("WIN", "A")
                )
            ),
            QuickKeyModel(
                name = "Run / Spotlight", preset = "Default", icon = "baseline_terminal",
                keys = listOf(
                    listOf("WIN", "R"),            // Windows: Run dialog
                    listOf("CMD", "SPACE"),        // macOS: Spotlight
                    listOf("WIN", "R")
                )
            ),
            QuickKeyModel(
                name = "Force Quit", preset = "Default", icon = "baseline_cancel",
                keys = listOf(
                    listOf("CTRL", "ALT", "DELETE"), // Windows: Security Screen
                    listOf("CMD", "OPTION", "ESC"),  // macOS: Force Quit
                    listOf("CTRL", "ALT", "DELETE")
                )
            ),

            QuickKeyModel(
                name = "Close Window", preset = "Default", icon = "baseline_close",
                keys = listOf(
                    listOf("ALT", "F4"),           // Windows
                    listOf("CMD", "W"),            // macOS
                    listOf("ALT", "F4")
                )
            ),
            // ============================================================
            // ANDROID STUDIO
            // ============================================================
            QuickKeyModel(
                name = "Save All", preset = "Android Studio", icon = "baseline_save_alt",
                keys = listOf(
                    listOf("CTRL", "S"),        // Windows / Linux
                    listOf("CMD", "S"),         // macOS
                    listOf("CTRL", "S")         // Linux
                )
            ),
            QuickKeyModel(
                name = "Find", preset = "Android Studio", icon = "baseline_search",
                keys = listOf(
                    listOf("CTRL", "F"),
                    listOf("CMD", "F"),
                    listOf("CTRL", "F")
                )
            ),
            QuickKeyModel(
                name = "Replace", preset = "Android Studio", icon = "baseline_find_replace",
                keys = listOf(
                    listOf("CTRL", "R"),
                    listOf("CMD", "R"),
                    listOf("CTRL", "R")
                )
            ),
            QuickKeyModel(
                name = "Col", preset = "Android Studio", icon = "outline_content_paste",
                keys = listOf(
                    listOf("ALT", "SHIFT", "INSERT"),
                    listOf("ALT", "SHIFT", "INSERT"),
                    listOf("ALT", "SHIFT", "INSERT")
                )
            ),
            QuickKeyModel(
                name = "Go to Class", preset = "Android Studio", icon = "baseline_code",
                keys = listOf(
                    listOf("CTRL", "N"),
                    listOf("CMD", "O"),
                    listOf("CTRL", "N")
                )
            ),
            QuickKeyModel(
                name = "Go to File", preset = "Android Studio", icon = "baseline_folder",
                keys = listOf(
                    listOf("CTRL", "SHIFT", "N"),
                    listOf("CMD", "SHIFT", "O"),
                    listOf("CTRL", "SHIFT", "N")
                )
            ),
            QuickKeyModel(
                name = "Recent Files", preset = "Android Studio", icon = "baseline_history",
                keys = listOf(
                    listOf("CTRL", "E"),
                    listOf("CMD", "E"),
                    listOf("CTRL", "E")
                )
            ),
            QuickKeyModel(
                name = "Clear Import",
                preset = "Android Studio",
                keys = listOf(
                    listOf("CTRL", "ALT", "O"),
                    listOf("CMD", "OPTION", "O"),
                    listOf("CTRL", "ALT", "O")
                ),
                icon = "baseline_cleaning_services"
            ),
            QuickKeyModel(
                name = "Reformat Code",
                preset = "Android Studio",
                icon = "baseline_format_line_spacing",
                keys = listOf(
                    listOf("CTRL", "ALT", "L"),
                    listOf("CMD", "OPTION", "L"),
                    listOf("CTRL", "ALT", "L")
                )
            ),
            QuickKeyModel(
                name = "Comment", preset = "Android Studio", icon = "baseline_comment",
                keys = listOf(
                    listOf("CTRL", "/"),
                    listOf("CMD", "/"),
                    listOf("CTRL", "/")
                )
            ),
            QuickKeyModel(
                preset = "Android Studio",
                name = "Undo",
                keys = listOf(
                    listOf("CTRL", "Z"),
                    listOf("CMD", "Z"),
                    listOf("CTRL", "Z")
                ),
                icon = "baseline_undo"
            ),
            QuickKeyModel(
                preset = "Android Studio",
                name = "Redo",
                keys = listOf(
                    listOf("CTRL", "SHIFT", "Z"),
                    listOf("CMD", "SHIFT", "Z"),
                    listOf("CTRL", "SHIFT", "Z")
                ),
                icon = "baseline_redo"
            ),
            QuickKeyModel(
                name = "Close Tab", preset = "Android Studio", icon = "baseline_close",
                keys = listOf(
                    listOf("CTRL", "F4"),
                    listOf("CMD", "W"),
                    listOf("CTRL", "F4")
                )
            ),

            // ============================================================
            // VISUAL STUDIO (IDE đầy đủ, chủ yếu Windows)
            // ============================================================
            QuickKeyModel(
                name = "Save All", preset = "Visual Studio", icon = "baseline_save_alt",
                keys = listOf(
                    listOf("CTRL", "SHIFT", "S"),   // Windows
                    emptyList(),                     // macOS không hỗ trợ chính thức
                    emptyList()
                )
            ),
            QuickKeyModel(
                name = "Find", preset = "Visual Studio", icon = "baseline_search",
                keys = listOf(
                    listOf("CTRL", "F"),
                    emptyList(),
                    emptyList()
                )
            ),
            QuickKeyModel(
                name = "Replace", preset = "Visual Studio", icon = "baseline_find_replace",
                keys = listOf(
                    listOf("CTRL", "H"),
                    emptyList(),
                    emptyList()
                )
            ),
            QuickKeyModel(
                name = "Tab", preset = "Visual Studio", icon = "baseline_tab",
                keys = listOf(
                    listOf("TAB"),
                    emptyList(),
                    emptyList()
                )
            ),
            QuickKeyModel(
                name = "Go to Class", preset = "Visual Studio", icon = "baseline_code",
                keys = listOf(
                    listOf("CTRL", "F12"),          // Go to Declaration
                    emptyList(),
                    emptyList()
                )
            ),
            QuickKeyModel(
                name = "Go to File", preset = "Visual Studio", icon = "baseline_folder",
                keys = listOf(
                    listOf(
                        "CTRL",
                        "SHIFT",
                        "T"
                    ),   // Go to File (VS không có mặc định, dùng Go to All)
                    emptyList(),
                    emptyList()
                )
            ),
            QuickKeyModel(
                name = "Recent Files", preset = "Visual Studio", icon = "baseline_history",
                keys = listOf(
                    listOf("CTRL", "TAB"),          // Window navigation
                    emptyList(),
                    emptyList()
                )
            ),
            QuickKeyModel(
                name = "Reformat Code",
                preset = "Visual Studio",
                icon = "baseline_format_line_spacing",
                keys = listOf(
                    listOf("CTRL", "K", "CTRL", "D"),  // tuần tự
                    emptyList(),
                    emptyList()
                )
            ),
            QuickKeyModel(
                name = "Comment", preset = "Visual Studio", icon = "baseline_comment",
                keys = listOf(
                    listOf("CTRL", "K", "CTRL", "C"),  // tuần tự
                    emptyList(),
                    emptyList()
                )
            ),
            QuickKeyModel(
                name = "Undo", preset = "Visual Studio", icon = "baseline_undo",
                keys = listOf(
                    listOf("CTRL", "Z"),
                    emptyList(),
                    emptyList()
                )
            ),
            QuickKeyModel(
                name = "Redo", preset = "Visual Studio", icon = "baseline_redo",
                keys = listOf(
                    listOf("CTRL", "Y"),            // VS mặc định dùng Ctrl+Y
                    emptyList(),
                    emptyList()
                )
            ),
            QuickKeyModel(
                name = "Close Tab", preset = "Visual Studio", icon = "baseline_close",
                keys = listOf(
                    listOf("CTRL", "F4"),
                    emptyList(),
                    emptyList()
                )
            ),

            // ============================================================
            // VS CODE (Trình soạn thảo nhẹ, đa nền tảng)
            // ============================================================
            QuickKeyModel(
                name = "Save All", preset = "VS Code", icon = "baseline_save_alt",
                keys = listOf(
                    listOf("CTRL", "K", "S"),       // tuần tự
                    listOf("CMD", "K", "S"),
                    listOf("CTRL", "K", "S")
                )
            ),
            QuickKeyModel(
                name = "Find", preset = "VS Code", icon = "baseline_search",
                keys = listOf(
                    listOf("CTRL", "F"),
                    listOf("CMD", "F"),
                    listOf("CTRL", "F")
                )
            ),
            QuickKeyModel(
                name = "Replace", preset = "VS Code", icon = "baseline_find_replace",
                keys = listOf(
                    listOf("CTRL", "H"),
                    listOf("CMD", "OPTION", "F"),
                    listOf("CTRL", "H")
                )
            ),
            QuickKeyModel(
                name = "Col", preset = "VS Code", icon = "outline_content_paste",
                keys = listOf(
                    listOf("CTRL", "SHIFT", "V"),   // Paste without Formatting
                    listOf("CMD", "SHIFT", "V"),
                    listOf("CTRL", "SHIFT", "V")
                )
            ),
            QuickKeyModel(
                name = "Go to Class", preset = "VS Code", icon = "baseline_code",
                keys = listOf(
                    listOf("CTRL", "T"),            // Go to Symbol in Workspace
                    listOf("CMD", "T"),
                    listOf("CTRL", "T")
                )
            ),
            QuickKeyModel(
                name = "Go to File", preset = "VS Code", icon = "baseline_folder",
                keys = listOf(
                    listOf("CTRL", "P"),            // Quick Open
                    listOf("CMD", "P"),
                    listOf("CTRL", "P")
                )
            ),
            QuickKeyModel(
                name = "Recent Files", preset = "VS Code", icon = "baseline_history",
                keys = listOf(
                    listOf("CTRL", "R"),            // Recent Workspaces
                    listOf("CMD", "R"),
                    listOf("CTRL", "R")
                )
            ),
            QuickKeyModel(
                name = "Clear Import", preset = "VS Code", icon = "baseline_cleaning_services",
                keys = listOf(
                    listOf("CTRL", "SHIFT", "O"),   // Go to Symbol in File
                    listOf("CMD", "SHIFT", "O"),
                    listOf("CTRL", "SHIFT", "O")
                )
            ),
            QuickKeyModel(
                name = "Reformat Code", preset = "VS Code", icon = "baseline_format_line_spacing",
                keys = listOf(
                    listOf("SHIFT", "ALT", "F"),
                    listOf("SHIFT", "OPTION", "F"),
                    listOf("CTRL", "SHIFT", "I")    // Linux khác Windows
                )
            ),
            QuickKeyModel(
                name = "Comment", preset = "VS Code", icon = "baseline_comment",
                keys = listOf(
                    listOf("CTRL", "/"),
                    listOf("CMD", "/"),
                    listOf("CTRL", "/")
                )
            ),
            QuickKeyModel(
                name = "Undo", preset = "VS Code", icon = "baseline_undo",
                keys = listOf(
                    listOf("CTRL", "Z"),
                    listOf("CMD", "Z"),
                    listOf("CTRL", "Z")
                )
            ),
            QuickKeyModel(
                name = "Redo", preset = "VS Code", icon = "baseline_redo",
                keys = listOf(
                    listOf("CTRL", "SHIFT", "Z"),
                    listOf("CMD", "SHIFT", "Z"),
                    listOf("CTRL", "SHIFT", "Z")
                )
            ),
            QuickKeyModel(
                name = "Close Tab", preset = "VS Code", icon = "baseline_close",
                keys = listOf(
                    listOf("CTRL", "W"),
                    listOf("CMD", "W"),
                    listOf("CTRL", "W")
                )
            )
        )
    }

}