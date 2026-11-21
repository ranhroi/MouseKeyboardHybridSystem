package com.ranhroi.esp32hidremote.ui.screens.server.send

import android.util.Log
import java.io.OutputStream
import java.net.Socket

private const val TAG = "TcpHidOutput"

class TcpHidOutput(
    private val socket: Socket,
    private val output: OutputStream
) : HidOutput {

    @Volatile
    private var closed = false

    override val isConnected: Boolean
        get() = !closed && socket.isConnected && !socket.isClosed

    override fun write(bytes: ByteArray): Boolean {
        if (closed) return false
        return try {
            output.write(bytes)
            output.flush()
            true
        } catch (e: Exception) {
            Log.e(TAG, "TCP write error: ${e.message}")
            closed = true
            false
        }
    }

    override fun close() {
        closed = true
        try { output.close() } catch (_: Exception) {}
        try { socket.close() } catch (_: Exception) {}
    }
}