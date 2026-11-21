package com.ranhroi.esp32hidremote.di

import android.content.Context
import com.ranhroi.esp32hidremote.data.scanner.BleScanner
import com.ranhroi.esp32hidremote.ui.screens.server.ConnectionManager
import com.ranhroi.esp32hidremote.ui.screens.server.send.HidTransmitter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideBluetoothScanner(
        @ApplicationContext context: Context
    ): BleScanner {
        return BleScanner(context)
    }


    @Provides
    @Singleton
    fun provideConnectionManager(
        @ApplicationContext context: Context
    ): ConnectionManager {
        return ConnectionManager(context)
    }

    @Provides
    @Singleton
    fun provideHidTransmitter(
        @ApplicationContext context: Context
    ): HidTransmitter {
        return HidTransmitter(context)
    }

}
