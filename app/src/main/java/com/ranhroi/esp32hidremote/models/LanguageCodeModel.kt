package com.ranhroi.esp32hidremote.models

data class LanguageCodeModel(
    val code: String,
    val labelRes: Int, // Lưu ID từ R.string
    val flag: String   // Lưu emoji cờ
)
