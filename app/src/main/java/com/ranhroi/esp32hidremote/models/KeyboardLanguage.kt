package com.ranhroi.esp32hidremote.models

import android.content.Context
import android.os.Build
import android.view.inputmethod.InputMethodManager
import java.util.Locale

data class KeyboardLanguage(
    val languageTag: String = "",
    val languageCode: String = "en",
    val countryCode: String = ""
)
