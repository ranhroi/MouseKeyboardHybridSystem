package com.ranhroi.esp32hidremote.models

data class ShortKeyPresetModel(
        val type: String,       // key dùng để filter: "media", "youtube", ...
        val label: String,      // tên hiển thị: "Media", "YouTube", ...
        val icon: String
    )