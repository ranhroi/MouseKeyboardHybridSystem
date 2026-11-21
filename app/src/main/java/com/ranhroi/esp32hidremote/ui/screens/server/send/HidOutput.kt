package com.ranhroi.esp32hidremote.ui.screens.server.send

interface HidOutput {
    fun write(bytes: ByteArray): Boolean
    fun close()
    val isConnected: Boolean
}