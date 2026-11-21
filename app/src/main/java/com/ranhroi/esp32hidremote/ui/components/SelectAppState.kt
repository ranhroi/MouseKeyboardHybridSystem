package com.ranhroi.esp32hidremote.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class SelectAppState {
    var bottomIndex by mutableIntStateOf(-1)
    var isCommandKey by mutableStateOf(false)
    var isControlCircle by mutableStateOf(false)
    fun resetState() {
        bottomIndex = -1
        isCommandKey = false
        isControlCircle = false
    }
}