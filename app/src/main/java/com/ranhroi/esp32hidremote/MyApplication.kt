package com.ranhroi.esp32hidremote

import android.app.Application
import com.ranhroi.esp32hidremote.utils.IconsMap
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        IconsMap.initialize()
    }
}