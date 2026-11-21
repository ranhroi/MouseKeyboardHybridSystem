package com.ranhroi.esp32hidremote.models


data class QuickKeyModel(
    val id: Int = 0,
    val name: String = "",
    val openApp: Boolean = false,
    val keys: List<List<String>> = listOf(emptyList(), emptyList(), emptyList()),
    val icon: String = "",
    val preset: String = ""
)