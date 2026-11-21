package com.ranhroi.esp32hidremote.ui.screens.shortkey

import com.ranhroi.esp32hidremote.models.ShortKeyModel
import com.ranhroi.esp32hidremote.models.ShortKeyPresetModel

object ShortKeysRepository {

    val shortKeysDefault: List<ShortKeyModel> by lazy {
        listOf(

            // ============================================================
            // 1. FLOATING — Quick Actions
            // ============================================================
            ShortKeyModel(
                type = "floating",
                name = "Hide",
                keys = listOf(
                    listOf("WIN", "D"),
                    listOf("CMD", "H"),
                    listOf("CTRL", "ALT", "D")
                ),
                icon = "baseline_computer"
            ),
            ShortKeyModel(
                type = "floating",
                name = "Refresh",
                keys = listOf(
                    listOf("F5"),
                    listOf("CMD", "R"),
                    listOf("F5")
                ),
                icon = "baseline_refresh"
            ),
            ShortKeyModel(
                type = "floating",
                name = "Select All",
                keys = listOf(
                    listOf("CTRL", "A"),
                    listOf("CMD", "A"),
                    listOf("CTRL", "A")
                ),
                icon = "baseline_select_all"
            ),
            ShortKeyModel(
                type = "floating",
                name = "Copy",
                keys = listOf(
                    listOf("CTRL", "C"),
                    listOf("CMD", "C"),
                    listOf("CTRL", "C")
                ),
                icon = "outline_content_copy"
            ),
            ShortKeyModel(
                type = "floating",
                name = "Paste",
                keys = listOf(
                    listOf("CTRL", "V"),
                    listOf("CMD", "V"),
                    listOf("CTRL", "V")
                ),
                icon = "outline_content_paste"
            ),
            ShortKeyModel(
                type = "floating",
                name = "Keyboard",
                keys = emptyList(),   // Command đặc biệt — không gửi key
                icon = "outline_keyboard_keys"
            ),
            ShortKeyModel(
                type = "floating",
                name = "Enter",
                keys = listOf(
                    listOf("ENTER"),
                    listOf("ENTER"),
                    listOf("ENTER")
                ),
                icon = "baseline_subdirectory_arrow_left"
            ),
            ShortKeyModel(
                type = "floating",
                name = "Cut",
                keys = listOf(
                    listOf("CTRL", "X"),
                    listOf("CMD", "X"),
                    listOf("CTRL", "X")
                ),
                icon = "baseline_content_cut"
            ),
            ShortKeyModel(
                type = "floating",
                name = "Undo",
                keys = listOf(
                    listOf("CTRL", "Z"),
                    listOf("CMD", "Z"),
                    listOf("CTRL", "Z")
                ),
                icon = "baseline_undo"
            ),
            ShortKeyModel(
                type = "floating",
                name = "Redo",
                keys = listOf(
                    listOf("CTRL", "Y"),
                    listOf("CMD", "SHIFT", "Z"),
                    listOf("CTRL", "SHIFT", "Z")
                ),
                icon = "baseline_redo"
            ),

            // ============================================================
            // 2. BROWSER
            // ============================================================

            ShortKeyModel(
                type = "browser",
                name = "Back",
                keys = listOf(
                    listOf("BROWSER_BACK"),
                    listOf("BROWSER_BACK"),
                    listOf("BROWSER_BACK")
                ),
                icon = "baseline_arrow_back"
            ),
            ShortKeyModel(
                type = "browser",
                name = "Home",
                keys = listOf(
                    listOf("BROWSER_HOME"),
                    listOf("BROWSER_HOME"),
                    listOf("BROWSER_HOME")
                ),
                icon = "baseline_home"
            ),
            ShortKeyModel(
                type = "browser",
                name = "Forward",
                keys = listOf(
                    listOf("BROWSER_FORWARD"),
                    listOf("BROWSER_FORWARD"),
                    listOf("BROWSER_FORWARD")
                ),
                icon = "baseline_arrow_forward"
            ),
            ShortKeyModel(
                type = "browser",
                name = "Zoom In",
                keys = listOf(
                    listOf("CTRL", "="),
                    listOf("CMD", "="),
                    listOf("CTRL", "=")
                ),
                icon = "outline_zoom_in"
            ),
            ShortKeyModel(
                type = "browser",
                name = "Reset Zoom",
                keys = listOf(
                    listOf("CTRL", "0"),
                    listOf("CMD", "0"),
                    listOf("CTRL", "0")
                ),
                icon = "baseline_zoom_out_map"
            ),
            ShortKeyModel(
                type = "browser",
                name = "Zoom Out",
                keys = listOf(
                    listOf("CTRL", "-"),
                    listOf("CMD", "-"),
                    listOf("CTRL", "-")
                ),
                icon = "baseline_zoom_out"
            ),

            ShortKeyModel(
                type = "browser",
                name = "Print Page",
                keys = listOf(
                    listOf("CTRL", "P"),
                    listOf("CMD", "P"),
                    listOf("CTRL", "P")
                ),
                icon = "baseline_print"
            ),
            ShortKeyModel(
                type = "browser",
                name = "Search",
                keys = listOf(
                    listOf("BROWSER_SEARCH"),
                    listOf("BROWSER_SEARCH"),
                    listOf("BROWSER_SEARCH")
                ),
                icon = "baseline_search"
            ),
            ShortKeyModel(
                type = "browser",
                name = "Address Bar",
                keys = listOf(
                    listOf("CTRL", "L"),
                    listOf("CMD", "L"),
                    listOf("CTRL", "L")
                ),
                icon = "baseline_location_on"
            ),
            ShortKeyModel(
                type = "browser",
                name = "View Source",
                keys = listOf(
                    listOf("CTRL", "U"),
                    listOf("OPTION", "CMD", "U"),
                    listOf("CTRL", "U")
                ),
                icon = "baseline_code"
            ),
            ShortKeyModel(
                type = "browser",
                name = "Dev Tools",
                keys = listOf(
                    listOf("CTRL", "SHIFT", "I"),
                    listOf("OPTION", "CMD", "I"),
                    listOf("CTRL", "SHIFT", "I")
                ),
                icon = "baseline_code"
            ),

            ShortKeyModel(
                type = "browser",
                name = "Refresh",
                keys = listOf(
                    listOf("BROWSER_REFRESH"),
                    listOf("BROWSER_REFRESH"),
                    listOf("BROWSER_REFRESH")
                ),
                icon = "baseline_refresh"
            ),
            ShortKeyModel(
                type = "browser",
                name = "Browser",
                keys = listOf(
                    listOf("CTRL", "ALT", "1"),
                    emptyList(),
                    emptyList()
                ),
                icon = "baseline_launch"
            ),
            ShortKeyModel(
                type = "browser",
                name = "New Tab",
                keys = listOf(
                    listOf("CTRL", "T"),
                    listOf("CMD", "T"),
                    listOf("CTRL", "T")
                ),
                icon = "baseline_add_box"
            ),
            ShortKeyModel(
                type = "browser",
                name = "Favorites",
                keys = listOf(
                    listOf("BROWSER_FAVORITES"),
                    listOf("BROWSER_FAVORITES"),
                    listOf("BROWSER_FAVORITES")
                ),
                icon = "outline_favorite"
            ),
            ShortKeyModel(
                type = "browser",
                name = "Minimize",
                keys = listOf(
                    listOf("WIN", "DOWN"),
                    listOf("CMD", "M"),
                    listOf("CTRL", "ALT", "D")
                ),
                icon = "baseline_minimize"
            ),
            ShortKeyModel(
                type = "browser",
                name = "Maximize",
                keys = listOf(
                    listOf("WIN", "UP"),
                    listOf("CTRL", "CMD", "F"),
                    listOf("CTRL", "ALT", "UP")
                ),
                icon = "baseline_fit_screen"
            ),
            ShortKeyModel(
                type = "browser",
                name = "Close",
                keys = listOf(
                    listOf("CTRL", "W"),
                    listOf("CMD", "W"),
                    listOf("CTRL", "W")
                ),
                icon = "baseline_close"
            ),

            // ============================================================
            // 4. POWER — dùng CONSUMER CONTROL
            // ============================================================
            ShortKeyModel(
                type = "power",
                name = "Screenshot",
                keys = listOf(
                    listOf("WIN","PRTSC"),
                    emptyList(),
                    emptyList()
                ),
                icon = "baseline_screenshot"
            ),
            ShortKeyModel(
                type = "power",
                name = "Brightness Down",
                keys = listOf(
                    listOf("BRIGHTNESS_DOWN"),    // ✅ Consumer
                    listOf("BRIGHTNESS_DOWN"),
                    listOf("BRIGHTNESS_DOWN")
                ),
                icon = "baseline_brightness_low"
            ),
            ShortKeyModel(
                type = "power",
                name = "Brightness Up",
                keys = listOf(
                    listOf("BRIGHTNESS_UP"),
                    listOf("BRIGHTNESS_UP"),
                    listOf("BRIGHTNESS_UP")
                ),
                icon = "baseline_brightness_high"
            ),
            ShortKeyModel(
                type = "power",
                name = "Power Off",
                keys = listOf(
                    listOf("POWER"),
                    listOf("POWER"),
                    listOf("POWER")
                ),
                icon = "baseline_power_setting_new"
            ),
            ShortKeyModel(
                type = "power",
                name = "Restart",
                keys = listOf(
                    listOf("RESTART"),   // Windows: Restart
                    listOf("RESTART"),    // Mac: Restart
                    listOf("RESTART")    // Linux
                ),
                icon = "outline_restart_alt"
            ),
            ShortKeyModel(
                type = "power",
                name = "Sleep",
                keys = listOf(
                    listOf("SLEEP"),
                    listOf("SLEEP"),
                    listOf("SLEEP")
                ),
                icon = "baseline_hotel"
            ),
            ShortKeyModel(
                type = "power",
                name = "Log Out",
                keys = listOf(
                    listOf("WIN", "L"),           // Windows: Lock
                    listOf("CMD", "CTRL", "Q"),   // Mac: Logout
                    listOf("CTRL", "ALT", "DELETE") // Linux: Logout
                ),
                icon = "baseline_logout"
            ),
            ShortKeyModel(
                type = "power",
                name = "Lock",
                keys = listOf(
                    listOf("WIN", "L"),           // Windows: Lock screen
                    listOf("CMD", "CTRL", "Q"),   // Mac: Lock
                    listOf("CTRL", "ALT", "L")    // Linux: Lock
                ),
                icon = "baseline_lock"
            )
        ) + shortKeysMediaDefault + shortKeysYouTube + shortKeysDailymotion + shortKeysVideoPlayer + shortKeysMusicPlayer
    }

    // ============================================================
    // 1. MEDIA DEFAULT — 14 items (10 consumer + 2 window + 2 group)
    // ============================================================
    val shortKeysMediaDefault: List<ShortKeyModel> by lazy {
        listOf(
            // 1. Lùi về
            ShortKeyModel(
                type = "media",
                name = "Back",
                keys = listOf(listOf("MEDIA_PREV"), listOf("MEDIA_PREV"), listOf("MEDIA_PREV")),
                icon = "baseline_arrow_back"
            ),
            // 2. Phát / Tạm dừng
            ShortKeyModel(
                type = "media",
                name = "Play / Pause",
                keys = listOf(listOf("PLAY_PAUSE"), listOf("PLAY_PAUSE"), listOf("PLAY_PAUSE")),
                icon = "baseline_play_arrow"
            ),
            // 3. Bài / Video tiếp
            ShortKeyModel(
                type = "media",
                name = "Next",
                keys = listOf(listOf("NEXT_TRACK"), listOf("NEXT_TRACK"), listOf("NEXT_TRACK")),
                icon = "baseline_arrow_forward"
            ),
            // 4. Tăng âm lượng
            ShortKeyModel(
                type = "media",
                name = "Volume Up",
                keys = listOf(listOf("VOLUME_UP"), listOf("VOLUME_UP"), listOf("VOLUME_UP")),
                icon = "baseline_volume_up"
            ),
            // 5. Tắt tiếng
            ShortKeyModel(
                type = "media",
                name = "Mute",
                keys = listOf(listOf("MUTE"), listOf("MUTE"), listOf("MUTE")),
                icon = "baseline_volume_off"
            ),
            // 6. Giảm âm lượng
            ShortKeyModel(
                type = "media",
                name = "Volume Down",
                keys = listOf(listOf("VOLUME_DOWN"), listOf("VOLUME_DOWN"), listOf("VOLUME_DOWN")),
                icon = "baseline_volume_down"
            ),
            // 7. Phụ đề
            ShortKeyModel(
                type = "media",
                name = "Captions",
                keys = listOf(listOf("C"), listOf("C"), listOf("C")),
                icon = "baseline_caption"
            ),
            // 8. Tua lại (REWIND, tách khỏi LEFT)
            ShortKeyModel(
                type = "media",
                name = "Rewind",
                keys = listOf(listOf("REWIND"), listOf("REWIND"), listOf("REWIND")),
                icon = "baseline_fast_rewind"
            ),
            // 9. Dừng (S thay vì C)
            ShortKeyModel(
                type = "media",
                name = "Stop",
                keys = listOf(listOf("MEDIA_STOP"), listOf("MEDIA_STOP"), listOf("MEDIA_STOP")),
                icon = "baseline_stop"
            ),
            // 10. Tua tới
            ShortKeyModel(
                type = "media",
                name = "Fast Forward",
                keys = listOf(
                    listOf("FAST_FORWARD"),
                    listOf("FAST_FORWARD"),
                    listOf("FAST_FORWARD")
                ),
                icon = "baseline_fast_forward"
            ),
            // 11. Phát lại / Bài trước
            ShortKeyModel(
                type = "media",
                name = "Replay",
                keys = listOf(listOf("PREV_TRACK"), listOf("PREV_TRACK"), listOf("PREV_TRACK")),
                icon = "baseline_replay"
            ),
            // 12. Toàn màn hình
            ShortKeyModel(
                type = "media",
                name = "Maximize",
                keys = listOf(
                    listOf("WIN", "UP"),
                    listOf("CTRL", "CMD", "F"),
                    listOf("CTRL", "ALT", "UP")
                ),
                icon = "baseline_fit_screen"
            ),
            // 13. Đóng cửa sổ
            ShortKeyModel(
                type = "media",
                name = "Close Window",
                keys = listOf(
                    listOf("ALT", "F4"),
                    listOf("CMD", "W"),
                    listOf("ALT", "F4")
                ),
                icon = "baseline_close"
            )
        )
    }

    // ============================================================
    // 2. YOUTUBE — 14 items (10 keyboard + 2 window + 2 group)
    // ============================================================
    val shortKeysYouTube: List<ShortKeyModel> by lazy {
        listOf(
            // 1. Lùi về
            ShortKeyModel(
                type = "youtube",
                name = "Back",
                keys = listOf(listOf("SHIFT", "P"), listOf("SHIFT", "P"), listOf("SHIFT", "P")),
                icon = "baseline_arrow_back"
            ),
            // 2. Phát / Tạm dừng
            ShortKeyModel(
                type = "youtube",
                name = "Play / Pause",
                keys = listOf(listOf("K"), listOf("K"), listOf("K")),
                icon = "baseline_play_arrow"
            ),
            // 3. Video tiếp
            ShortKeyModel(
                type = "youtube",
                name = "Next Video",
                keys = listOf(listOf("SHIFT", "N"), listOf("SHIFT", "N"), listOf("SHIFT", "N")),
                icon = "baseline_arrow_forward"
            ),
            // 4. Tăng âm lượng
            ShortKeyModel(
                type = "youtube",
                name = "Volume Up",
                keys = listOf(listOf("UP"), listOf("UP"), listOf("UP")),
                icon = "baseline_volume_up"
            ),
            // 5. Tắt tiếng
            ShortKeyModel(
                type = "youtube",
                name = "Mute",
                keys = listOf(listOf("M"), listOf("M"), listOf("M")),
                icon = "baseline_volume_off"
            ),
            // 6. Giảm âm lượng
            ShortKeyModel(
                type = "youtube",
                name = "Volume Down",
                keys = listOf(listOf("DOWN"), listOf("DOWN"), listOf("DOWN")),
                icon = "baseline_volume_down"
            ),
            // 7. Phụ đề
            ShortKeyModel(
                type = "youtube",
                name = "Captions",
                keys = listOf(listOf("C"), listOf("C"), listOf("C")),
                icon = "baseline_caption"
            ),
            // 8. Tua lại
            ShortKeyModel(
                type = "youtube",
                name = "Rewind",
                keys = listOf(listOf("J"), listOf("J"), listOf("J")),
                icon = "baseline_fast_rewind"
            ),
            // 9. Dừng (YouTube không có phím chữ, dùng media key STOP)
            ShortKeyModel(
                type = "youtube",
                name = "Stop",
                keys = listOf(listOf("MEDIA_STOP"), listOf("MEDIA_STOP"), listOf("MEDIA_STOP")),
                icon = "baseline_stop"
            ),
            // 10. Tua tới
            ShortKeyModel(
                type = "youtube",
                name = "Fast Forward",
                keys = listOf(listOf("L"), listOf("L"), listOf("L")),
                icon = "baseline_fast_forward"
            ),
            // 11. Video trước / Phát lại
            ShortKeyModel(
                type = "youtube",
                name = "Replay",
                keys = listOf(listOf("0"), listOf("0"), listOf("0")),
                icon = "baseline_replay"
            ),
            // 12. Toàn màn hình
            ShortKeyModel(
                type = "youtube",
                name = "Fullscreen",
                keys = listOf(listOf("F"), listOf("F"), listOf("F")),
                icon = "baseline_fit_screen"
            ),
            // 13. Đóng cửa sổ
            ShortKeyModel(
                type = "youtube",
                name = "Close Window",
                keys = listOf(
                    listOf("ALT", "F4"),
                    listOf("CMD", "W"),
                    listOf("ALT", "F4")
                ),
                icon = "baseline_close"
            )
        )
    }

    // ============================================================
    // 3. DAILYMOTION — 14 items (10 keyboard + 2 window + 2 group)
    // ============================================================
    val shortKeysDailymotion: List<ShortKeyModel> by lazy {
        listOf(
            // 1. Lùi về (dùng LEFT快退)
            ShortKeyModel(
                type = "dailymotion",
                name = "Back",
                keys = listOf(listOf("SHIFT", "P"), listOf("SHIFT", "P"), listOf("SHIFT", "P")),
                icon = "baseline_arrow_back"
            ),
            // 2. Phát / Tạm dừng — Space
            ShortKeyModel(
                type = "dailymotion",
                name = "Play / Pause",
                keys = listOf(listOf("SPACE"), listOf("SPACE"), listOf("SPACE")),
                icon = "baseline_play_arrow"
            ),
            // 3. Tiếp — RIGHT (快进)
            ShortKeyModel(
                type = "dailymotion",
                name = "Next",
                keys = listOf(listOf("SHIFT", "N"), listOf("SHIFT", "N"), listOf("SHIFT", "N")),
                icon = "baseline_arrow_forward"
            ),
            // 4. Tăng âm lượng — UP
            ShortKeyModel(
                type = "dailymotion",
                name = "Volume Up",
                keys = listOf(listOf("UP"), listOf("UP"), listOf("UP")),
                icon = "baseline_volume_up"
            ),
            // 5. Tắt tiếng — M
            ShortKeyModel(
                type = "dailymotion",
                name = "Mute",
                keys = listOf(listOf("M"), listOf("M"), listOf("M")),
                icon = "baseline_volume_off"
            ),
            // 6. Giảm âm lượng — DOWN
            ShortKeyModel(
                type = "dailymotion",
                name = "Volume Down",
                keys = listOf(listOf("DOWN"), listOf("DOWN"), listOf("DOWN")),
                icon = "baseline_volume_down"
            ),
            // 7. Phụ đề (Dailymotion无官方C键，保留占位)
            ShortKeyModel(
                type = "dailymotion",
                name = "Captions",
                keys = listOf(listOf("C"), listOf("C"), listOf("C")),
                icon = "baseline_caption"
            ),
            // 8. Tua lại — LEFT (快退 10s)
            ShortKeyModel(
                type = "dailymotion",
                name = "Rewind",
                keys = listOf(listOf("LEFT"), listOf("LEFT"), listOf("LEFT")),
                icon = "baseline_fast_rewind"
            ),
            // 9. Dừng (无官方停止键，保留 SPACE 占位)
            ShortKeyModel(
                type = "dailymotion",
                name = "Stop",
                keys = listOf(listOf("SPACE"), listOf("SPACE"), listOf("SPACE")),
                icon = "baseline_stop"
            ),
            // 10. Tua tới — RIGHT (快进 10s)
            ShortKeyModel(
                type = "dailymotion",
                name = "Fast Forward",
                keys = listOf(listOf("RIGHT"), listOf("RIGHT"), listOf("RIGHT")),
                icon = "baseline_fast_forward"
            ),
            // 11. Phát lại — 数字 0 跳回开头
            ShortKeyModel(
                type = "dailymotion",
                name = "Replay",
                keys = listOf(listOf("0"), listOf("0"), listOf("0")),
                icon = "baseline_replay"
            ),
            // 12. Toàn màn hình — F
            ShortKeyModel(
                type = "dailymotion",
                name = "Fullscreen",
                keys = listOf(listOf("F"), listOf("F"), listOf("F")),
                icon = "baseline_fit_screen"
            ),
            // 13. Đóng cửa sổ
            ShortKeyModel(
                type = "dailymotion",
                name = "Close Window",
                keys = listOf(
                    listOf("ALT", "F4"),
                    listOf("CMD", "W"),
                    listOf("ALT", "F4")
                ),
                icon = "baseline_close"
            )
        )
    }

    // ============================================================
    // 4. VIDEO PLAYER (WIN 11) — 14 items (10 keyboard + 2 window + 2 group)
    // ============================================================
    val shortKeysVideoPlayer: List<ShortKeyModel> by lazy {
        listOf(
            // 1. Lùi về
            ShortKeyModel(
                type = "video_player",
                name = "Back",
                keys = listOf(listOf("LEFT"), listOf("LEFT"), listOf("LEFT")),
                icon = "baseline_arrow_back"
            ),
            // 2. Phát / Tạm dừng — Space
            ShortKeyModel(
                type = "video_player",
                name = "Play / Pause",
                keys = listOf(listOf("SPACE"), listOf("SPACE"), listOf("SPACE")),
                icon = "baseline_play_arrow"
            ),
            // 3. Tiếp — RIGHT
            ShortKeyModel(
                type = "video_player",
                name = "Next",
                keys = listOf(listOf("RIGHT"), listOf("RIGHT"), listOf("RIGHT")),
                icon = "baseline_arrow_forward"
            ),
            // 4. Tăng âm lượng — UP
            ShortKeyModel(
                type = "video_player",
                name = "Volume Up",
                keys = listOf(listOf("UP"), listOf("UP"), listOf("UP")),
                icon = "baseline_volume_up"
            ),
            // 5. Tắt tiếng — M
            ShortKeyModel(
                type = "video_player",
                name = "Mute",
                keys = listOf(listOf("M"), listOf("M"), listOf("M")),
                icon = "baseline_volume_off"
            ),
            // 6. Giảm âm lượng — DOWN
            ShortKeyModel(
                type = "video_player",
                name = "Volume Down",
                keys = listOf(listOf("DOWN"), listOf("DOWN"), listOf("DOWN")),
                icon = "baseline_volume_down"
            ),
            // 7. Phụ đề (无官方V键，保留占位)
            ShortKeyModel(
                type = "video_player",
                name = "Captions",
                keys = listOf(listOf("V"), listOf("V"), listOf("V")),
                icon = "baseline_caption"
            ),
            // 8. Tua lại — LEFT
            ShortKeyModel(
                type = "video_player",
                name = "Rewind",
                keys = listOf(listOf("LEFT"), listOf("LEFT"), listOf("LEFT")),
                icon = "baseline_fast_rewind"
            ),
            // 9. Dừng (无官方S键，保留占位)
            ShortKeyModel(
                type = "video_player",
                name = "Stop",
                keys = listOf(listOf("S"), listOf("S"), listOf("S")),
                icon = "baseline_stop"
            ),
            // 10. Tua tới — RIGHT
            ShortKeyModel(
                type = "video_player",
                name = "Fast Forward",
                keys = listOf(listOf("RIGHT"), listOf("RIGHT"), listOf("RIGHT")),
                icon = "baseline_fast_forward"
            ),
            // 11. Phát lại (无官方P键，保留占位)
            ShortKeyModel(
                type = "video_player",
                name = "Replay",
                keys = listOf(listOf("P"), listOf("P"), listOf("P")),
                icon = "baseline_replay"
            ),
            // 12. Toàn màn hình — F
            ShortKeyModel(
                type = "video_player",
                name = "Fullscreen",
                keys = listOf(listOf("F"), listOf("F"), listOf("F")),
                icon = "baseline_fit_screen"
            ),
            // 13. Đóng cửa sổ
            ShortKeyModel(
                type = "video_player",
                name = "Close Window",
                keys = listOf(
                    listOf("ALT", "F4"),
                    listOf("CMD", "W"),
                    listOf("ALT", "F4")
                ),
                icon = "baseline_close"
            )
        )
    }

    // ============================================================
    // 5. MUSIC PLAYER (WIN 11) — 14 items (10 keyboard + 2 window + 2 group)
    // ============================================================
    val shortKeysMusicPlayer: List<ShortKeyModel> by lazy {
        listOf(
            // 1. Lùi về
            ShortKeyModel(
                type = "music_player",
                name = "Back",
                keys = listOf(
                    listOf("CTRL", "LEFT"),
                    listOf("CMD", "LEFT"),
                    listOf("CTRL", "LEFT")
                ),
                icon = "baseline_arrow_back"
            ),
            // 2. Phát / Tạm dừng — Space
            ShortKeyModel(
                type = "music_player",
                name = "Play / Pause",
                keys = listOf(listOf("SPACE"), listOf("SPACE"), listOf("SPACE")),
                icon = "baseline_play_arrow"
            ),
            // 3. Bài tiếp
            ShortKeyModel(
                type = "music_player",
                name = "Next Track",
                keys = listOf(
                    listOf("CTRL", "RIGHT"),
                    listOf("CMD", "RIGHT"),
                    listOf("CTRL", "RIGHT")
                ),
                icon = "baseline_arrow_forward"
            ),
            // 4. Tăng âm lượng
            ShortKeyModel(
                type = "music_player",
                name = "Volume Up",
                keys = listOf(listOf("VOLUME_UP"), listOf("VOLUME_UP"), listOf("VOLUME_UP")),
                icon = "baseline_volume_up"
            ),
            // 5. Tắt tiếng
            ShortKeyModel(
                type = "music_player",
                name = "Mute",
                keys = listOf(listOf("MUTE"), listOf("MUTE"), listOf("MUTE")),
                icon = "baseline_volume_off"
            ),
            // 6. Giảm âm lượng
            ShortKeyModel(
                type = "music_player",
                name = "Volume Down",
                keys = listOf(listOf("VOLUME_DOWN"), listOf("VOLUME_DOWN"), listOf("VOLUME_DOWN")),
                icon = "baseline_volume_down"
            ),
            // 7. Yêu thích (无官方快捷键，保留占位)
            ShortKeyModel(
                type = "music_player",
                name = "Like",
                keys = listOf(
                    listOf("ALT", "SHIFT", "B"),
                    listOf("CMD", "SHIFT", "B"),
                    listOf("ALT", "SHIFT", "B")
                ),
                icon = "outline_favorite"
            ),
            // 8. Tua lại
            ShortKeyModel(
                type = "music_player",
                name = "Rewind",
                keys = listOf(
                    listOf("CTRL", "LEFT"),
                    listOf("CMD", "LEFT"),
                    listOf("CTRL", "LEFT")
                ),
                icon = "baseline_fast_rewind"
            ),
            // 9. Dừng — Space
            ShortKeyModel(
                type = "music_player",
                name = "Stop",
                keys = listOf(listOf("SPACE"), listOf("SPACE"), listOf("SPACE")),
                icon = "baseline_stop"
            ),
            // 10. Tua tới
            ShortKeyModel(
                type = "music_player",
                name = "Fast Forward",
                keys = listOf(
                    listOf("CTRL", "RIGHT"),
                    listOf("CMD", "RIGHT"),
                    listOf("CTRL", "RIGHT")
                ),
                icon = "baseline_fast_forward"
            ),
            // 11. Phát lại / Bài trước
            ShortKeyModel(
                type = "music_player",
                name = "Previous Track",
                keys = listOf(
                    listOf("CTRL", "LEFT"),
                    listOf("CMD", "LEFT"),
                    listOf("CTRL", "LEFT")
                ),
                icon = "baseline_replay"
            ),
            // 12. Toàn màn hình
            ShortKeyModel(
                type = "music_player",
                name = "Fullscreen",
                keys = listOf(listOf("F"), listOf("F"), listOf("F")),
                icon = "baseline_fit_screen"
            ),
            // 13. Đóng cửa sổ
            ShortKeyModel(
                type = "music_player",
                name = "Close Window",
                keys = listOf(
                    listOf("ALT", "F4"),
                    listOf("CMD", "W"),
                    listOf("ALT", "F4")
                ),
                icon = "baseline_close"
            )
        )
    }

    val shortKeyPreset: List<ShortKeyPresetModel> = listOf(
        ShortKeyPresetModel("media", "Media", "ic_media"),
        ShortKeyPresetModel("youtube", "YouTube", "ic_youtube"),
        ShortKeyPresetModel("dailymotion", "Dailymotion", "ic_dailymotion"),
        ShortKeyPresetModel("video_player", "Video Player", "ic_videoplay"),
        ShortKeyPresetModel("music_player", "Music Player", "ic_musicplay"),
    )
}