package com.ranhroi.esp32hidremote.models

import androidx.annotation.StringRes

data class KeyGroup(
    @StringRes val titleRes: Int,
    val items: List<KeyboardModel>
)