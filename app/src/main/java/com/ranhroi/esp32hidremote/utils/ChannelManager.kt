package com.ranhroi.esp32hidremote.utils

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

object ChannelManager {
    private val _channel = Channel<ByteArray>(Channel.UNLIMITED)
    val channel = _channel.receiveAsFlow()

    suspend fun send(event: ByteArray) {
        _channel.send(event)
    }

}


