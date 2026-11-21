package com.ranhroi.esp32hidremote.models

import androidx.annotation.StringRes

data class BottomNavModel(
    @StringRes val nameRes: Int,
    val icon: String
)