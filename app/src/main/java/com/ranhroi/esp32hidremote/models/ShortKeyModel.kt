package com.ranhroi.esp32hidremote.models

/**
 * Model phím tắt.
 *
 * @param type  Loại shortkey: "floating", "browser", "media", "power", "shortkey"
 * @param name  Resource string ID cho tên hiển thị
 * @param keys  Danh sách tổ hợp phím theo OS: [Windows, Mac, Linux]
 * @param icon  Tên icon (Material icon name dạng snake_case)
 */
data class ShortKeyModel(
    val id: Int = 0,
    val type: String = "",
    val name: String = "",
    val keys: List<List<String>> = listOf(emptyList(), emptyList(), emptyList()),
    val icon: String = "baseline_help"
)