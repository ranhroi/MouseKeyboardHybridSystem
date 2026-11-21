package com.ranhroi.esp32hidremote.ui.screens.server.send

import android.content.Context
import com.ranhroi.esp32hidremote.models.HybridConnectionModel
import com.ranhroi.esp32hidremote.utils.showToast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.nio.charset.StandardCharsets
import kotlin.math.abs
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds


class HidTransmitter(private val context: Context) {
    companion object {

        const val V2_MAGIC: Byte = 0xAA.toByte()
        const val V2_VERSION: Byte = 0x01

        const val CMD_SET_MODIFIERS: Byte = 0x01
        const val CMD_KEY_DOWN: Byte = 0x02
        const val CMD_KEY_UP: Byte = 0x03
        const val CMD_KEY_TAP: Byte = 0x04
        const val CMD_KEY_UTF8: Byte = 0x05

        const val CMD_MOUSE_MOVE: Byte = 0x10
        const val CMD_MOUSE_SCROLL: Byte = 0x11
        const val CMD_MOUSE_CLICK: Byte = 0x12
        const val CMD_MOUSE_DOWN: Byte = 0x13
        const val CMD_MOUSE_UP: Byte = 0x14

        const val CMD_CONSUMER_TAP: Byte = 0x20
        const val CMD_CONSUMER_DOWN: Byte = 0x21
        const val CMD_CONSUMER_UP: Byte = 0x22

        const val CMD_SYSTEM_ACTION: Byte = 0x30

        const val CMD_REPORT_CURRENT_APP: Byte = 0xE1.toByte()

//        const val CMD_SCREENSHOT: Byte = 0xE2.toByte()
        private const val MOUSE_MOVE_INTERVAL_MS = 8L

    }

    // ============================================================
    // SCOPE
    // ============================================================
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var state: HybridConnectionModel = HybridConnectionModel()

    private var lastSentModifiersMask: Byte = 0x00

    // ============================================================
    // WRITE QUEUE — CHỈ CHO KEYBOARD/TEXT
    // ============================================================
    private var workerJob: Job? = null

    private val keyboardChannel = Channel<ByteArray>(Channel.UNLIMITED)

    // Mouse MOVE: chỉ giữ tối đa 1 packet mới nhất để tránh tích backlog.
    private val mouseMoveChannel = Channel<ByteArray>(
        capacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // Mouse control events (click/down/up/scroll) không được phép mất.
    private val mouseEventChannel = Channel<ByteArray>(Channel.UNLIMITED)

    init {
        workerJob = scope.launch {
            while (isActive) {
                // Ưu tiên control event trước (DOWN/UP/CLICK/SCROLL),
                // sau đó đến MOVE realtime, cuối cùng mới đến keyboard.
                val pendingMouseEvent = mouseEventChannel.tryReceive().getOrNull()
                if (pendingMouseEvent != null) {
                    performWrite(pendingMouseEvent)
                    continue
                }

                val pendingMouseMove = mouseMoveChannel.tryReceive().getOrNull()
                if (pendingMouseMove != null) {
                    performWrite(pendingMouseMove)
                    continue
                }

                select<Unit> {
                    mouseEventChannel.onReceiveCatching { result ->
                        result.getOrNull()?.let { performWrite(it) }
                    }
                    mouseMoveChannel.onReceiveCatching { result ->
                        result.getOrNull()?.let { performWrite(it) }
                    }
                    keyboardChannel.onReceiveCatching { result ->
                        result.getOrNull()?.let { performWrite(it) }
                    }
                }
            }
        }
    }


    // ============================================================
    // MOUSE MOVE
    // ============================================================
    @Volatile
    private var lastSendMouseTime = 0L

    @Volatile
    private var pendingMouseX = 0

    @Volatile
    private var pendingMouseY = 0

    @Volatile
    var cursorProgress: Int = 50

    @Volatile
    var cursorScroll: Int = 50

    @Volatile
    var isScrollDirection: Boolean = false


    // ==========================================
    // Protocol V2 Frames & Writing
    // ==========================================

    data class V2Frame(val cmd: Byte, val payload: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is V2Frame) return false
            return cmd == other.cmd && payload.contentEquals(other.payload)
        }

        override fun hashCode(): Int {
            var result = cmd.toInt()
            result = 31 * result + payload.contentHashCode()
            return result
        }
    }

    // ============================================================
    // SET STATE
    // ============================================================
    fun setState(newState: HybridConnectionModel) {
        this.state = newState
        this.lastSentModifiersMask = 0x00
    }


    private fun isMouseFrame(frame: V2Frame): Boolean {
        return when (frame.cmd) {
            CMD_MOUSE_MOVE,
            CMD_MOUSE_SCROLL,
            CMD_MOUSE_CLICK,
            CMD_MOUSE_DOWN,
            CMD_MOUSE_UP -> true
            else -> false
        }
    }

    private fun encodeV2Frame(frame: V2Frame): ByteArray? {
        val len = frame.payload.size
        if (len > 0xFF) return null

        val bytes = ByteArray(4 + len)
        bytes[0] = V2_MAGIC
        bytes[1] = V2_VERSION
        bytes[2] = frame.cmd
        bytes[3] = len.toByte()
        if (len > 0) {
            frame.payload.copyInto(bytes, destinationOffset = 4)
        }
        return bytes
    }

    private fun enqueueMouseRealtime(frames: List<V2Frame>) {
        for (frame in frames) {
            val bytes = encodeV2Frame(frame) ?: continue

            if (frame.cmd == CMD_MOUSE_MOVE) {
                // MOVE là dữ liệu realtime: packet cũ có thể bỏ nếu packet mới đã đến.
                mouseMoveChannel.trySend(bytes)
            } else {
                // Click/Down/Up/Scroll phải giữ đầy đủ.
                mouseEventChannel.trySend(bytes)
            }
        }
    }

    /**
     * V2 WRITE ROUTER
     *
     * Keyboard/Text/Consumer/System:
     *   -> writeChannel UNLIMITED
     *
     * Mouse:
     *   -> mouseChannel capacity=1 + DROP_OLDEST
     *   -> writer ưu tiên mouse để tránh tích đống packet.
     *
     * BLE keyboard vẫn batching theo MTU như cũ.
     * TCP keyboard vẫn gộp tất cả frame như cũ.
     */
    private fun writeV2(frames: List<V2Frame>) {
        val s = state
        if (!s.isConnected || !s.isVerified || frames.isEmpty()) return

        // Các API mouse hiện tại truyền frame mouse riêng lẻ. Nếu một caller
        // vô tình trộn mouse + keyboard trong cùng một list, giữ nguyên thứ tự
        // bằng cách dùng đường cũ thay vì tách hai channel.
        val hasMouse = frames.any(::isMouseFrame)
        val hasNonMouse = frames.any { !isMouseFrame(it) }

        if (hasMouse && !hasNonMouse) {
            enqueueMouseRealtime(frames)
            return
        }

        if (s.isBluetooth) {
            // ============================================================
            // BLE KEYBOARD/TEXT — Gộp frame theo MTU như cũ
            // ============================================================
            val maxLen = (s.currentMtu - 3).coerceIn(20, 250)
            var i = 0

            while (i < frames.size) {
                val bytes = mutableListOf<Byte>(V2_MAGIC, V2_VERSION)

                while (i < frames.size) {
                    val frame = frames[i]
                    val len = frame.payload.size
                    if (len > 0xFF) break

                    val frameLen = 2 + len
                    if (bytes.size + frameLen > maxLen) break

                    bytes.add(frame.cmd)
                    bytes.add(len.toByte())
                    for (b in frame.payload) {
                        bytes.add(b)
                    }
                    i++
                }

                if (bytes.size > 2) {
                    keyboardChannel.trySend(bytes.toByteArray())
                }
            }

        } else {
            // ============================================================
            // TCP KEYBOARD/TEXT — Gộp tất cả frame như cũ
            // ============================================================
            val bytes = mutableListOf<Byte>(V2_MAGIC, V2_VERSION)
            for (frame in frames) {
                bytes.add(frame.cmd)
                bytes.add(frame.payload.size.toByte())
                for (b in frame.payload) {
                    bytes.add(b)
                }
            }
            keyboardChannel.trySend(bytes.toByteArray())
        }
    }


    private fun writeMouseV2(frame: V2Frame) {
        val s = state
        if (!s.isConnected || !s.isVerified) return

        val len = frame.payload.size
        if (len > 0xFF) return

        val bytes = ByteArray(4 + len)

        bytes[0] = V2_MAGIC
        bytes[1] = V2_VERSION
        bytes[2] = frame.cmd
        bytes[3] = len.toByte()

        if (len > 0) {
            frame.payload.copyInto(
                bytes,
                destinationOffset = 4
            )
        }

        mouseMoveChannel.trySend(bytes)
    }


    private fun performWrite(data: ByteArray) {
        try {
            state.hidOutput?.write(data)
        } catch (e: Exception) {
            if (e !is CancellationException) {
            }
        }
    }

    // ==========================================
    // High-Level Mouse Commands
    // ==========================================
    fun sendMouseMove(dx: Int, dy: Int): Boolean {
        val now = System.nanoTime()

        if (now - lastSendMouseTime < MOUSE_MOVE_INTERVAL_MS * 1_000_000) {
            pendingMouseX += dx
            pendingMouseY += dy
            return false
        }

        val x = dx + pendingMouseX
        val y = dy + pendingMouseY

        pendingMouseX = 0
        pendingMouseY = 0
        lastSendMouseTime = now

        if (x == 0 && y == 0) return true

        val maxDelta = max(abs(x), abs(y))

        val sx: Int
        val sy: Int

        when {
            maxDelta <= 10 -> {
                sx = x
                sy = y
            }

            maxDelta <= 20 -> {
                sx = ((x * 1.1f) - 1f).toInt()
                sy = ((y * 1.1f) - 1f).toInt()
            }

            else -> {
                sx = ((x * 1.3f) - 5f).toInt()
                sy = ((y * 1.3f) - 5f).toInt()
            }
        }

        val mx = (sx * (cursorProgress + 50)) / 100
        val my = (sy * (cursorProgress + 50)) / 100

        val clampedX = mx.coerceIn(-127, 127).toByte()
        val clampedY = my.coerceIn(-127, 127).toByte()

        writeMouseV2(V2Frame(CMD_MOUSE_MOVE, byteArrayOf(clampedX, clampedY)))
        return true
    }

    // ============================================================
    // SCROLL
    // ============================================================
    fun sendMouseHScroll(z: Boolean, i: Int = 0) {
        var dir = z
        if (isScrollDirection) dir = !dir

        val step = (cursorScroll + 50) / 100
        if (step == 0) return

        val value = if (dir) step else -step
        val clampedValue = value.coerceIn(-127, 127).toByte()

        writeMouseV2(V2Frame(CMD_MOUSE_SCROLL, byteArrayOf(clampedValue, 0)))
    }

    fun sendMouseMiddleScroll(z: Boolean, i: Int) {
        var dir = z
        var step = i

        if (isScrollDirection) dir = !dir

        if (step > 4) {
            step = when {
                step <= 8 -> (step * 2) - 4
                step <= 12 -> (step * 4) - 20
                step <= 16 -> (step * 6) - 44
                else -> (step * 8) - 76
            }
        }

        val value = ((cursorScroll + 50) * step) / 100
        val v = if (dir) value else -value
        val clampedValue = v.coerceIn(-127, 127).toByte()

        writeMouseV2(V2Frame(CMD_MOUSE_SCROLL, byteArrayOf(0, clampedValue)))
    }

    /**
     * Send mouse click event (1=left, 2=right, 4=middle).
     */
    fun sendMouseClick(button: Byte) {
        writeV2(listOf(V2Frame(CMD_MOUSE_CLICK, byteArrayOf(button))))
    }

    /**
     * Mouse button down (for drag and drop, selection).
     */
    fun sendMouseButtonDown(button: Byte) {
        writeV2(listOf(V2Frame(CMD_MOUSE_DOWN, byteArrayOf(button))))
    }

    /**
     * Mouse button up (release drag).
     */
    fun sendMouseButtonUp(button: Byte) {
        writeV2(listOf(V2Frame(CMD_MOUSE_UP, byteArrayOf(button))))
    }

    // ==========================================
    // High-Level Keyboard Commands
    // ==========================================

    private fun setModifiersFrameIfNeeded(mask: Byte): List<V2Frame> {
        if (mask == lastSentModifiersMask) return emptyList()
        lastSentModifiersMask = mask
        return listOf(V2Frame(CMD_SET_MODIFIERS, byteArrayOf(mask)))
    }

    /**
     * Sets the active modifiers (Ctrl, Alt, Shift, Win).
     */
    fun setModifiers(mask: Byte) {
        val frames = setModifiersFrameIfNeeded(mask)
        if (frames.isNotEmpty()) {
            writeV2(frames)
        }
    }

    /**
     * Sends Key Down event with specified modifiers.
     */
    fun sendKeyDown(modifiersMask: Byte, keycode: Byte) {
        val frames = mutableListOf<V2Frame>()
        frames.addAll(setModifiersFrameIfNeeded(modifiersMask))
        frames.add(V2Frame(CMD_KEY_DOWN, byteArrayOf(keycode)))
        writeV2(frames)
    }

    /**
     * Sends Key Up event.
     */
    fun sendKeyUp(keycode: Byte) {
        writeV2(listOf(V2Frame(CMD_KEY_UP, byteArrayOf(keycode))))
    }

    /**
     * Sends non-disruptive key tap (press + release) with temporary modifier.
     */
    fun sendKeyTap(modifiers: Byte, keycode: Byte) {
        writeV2(listOf(V2Frame(CMD_KEY_TAP, byteArrayOf(modifiers, keycode))))
    }

    /**
     * Batch send key taps (useful for typing whole words or text diffs).
     */
    fun sendKeyTaps(taps: List<Pair<Byte, Byte>>) {
        val frames = taps.map { V2Frame(CMD_KEY_TAP, byteArrayOf(it.first, it.second)) }
        writeV2(frames)
    }

    /**
     * Gửi từng key tap với delay — dùng cho gõ text tiếng Việt (Unikey cần thời gian).
     */
    fun sendKeyTapsWithDelay(
        taps: List<Pair<Byte, Byte>>,
        delayMs: Long = 15L
    ) {
        scope.launch {
            try {
                for ((mod, key) in taps) {
                    sendKeyTap(mod, key)
                    delay(delayMs.coerceAtLeast(0L).milliseconds)
                }
            } finally {
                setModifiers(0x00)
                lastSentModifiersMask = 0x00
            }
        }
    }


    fun sendTextUtf8(str: String) {
        if (str.isEmpty()) return

        val s = state
        if (!s.isConnected || !s.isVerified) return

        val bytes = str.toByteArray(StandardCharsets.UTF_8)
        val maxChunkSize = 240
        var offset = 0

        while (offset < bytes.size) {
            // Dừng nếu kết nối hoặc xác thực không còn hợp lệ.
            if (!state.isConnected || !state.isVerified) {
                break
            }

            var end = minOf(offset + maxChunkSize, bytes.size)

            // Không cắt giữa một ký tự UTF-8.
            if (end < bytes.size) {
                while (
                    end > offset &&
                    (bytes[end].toInt() and 0xC0) == 0x80
                ) {
                    end--
                }
            }

            if (end <= offset) {
                break
            }

            val payload = bytes.copyOfRange(offset, end)
            val frame = V2Frame(
                cmd = CMD_KEY_UTF8,
                payload = payload
            )

            val encoded = encodeV2Frame(frame) ?: break

            // Đưa từng frame vào hàng đợi theo đúng thứ tự.
            if (keyboardChannel.trySend(encoded).isFailure) {
                break
            }

            offset = end
        }
    }

    // ============================================================
    // Consumer Control (media keys)
    // ============================================================

    fun sendConsumerTap(usage: Int) {
        val payload = byteArrayOf(
            ((usage shr 8) and 0xFF).toByte(),
            (usage and 0xFF).toByte()
        )
        writeV2(listOf(V2Frame(CMD_CONSUMER_TAP, payload)))
    }

    fun sendConsumerDown(usage: Int) {
        val payload = byteArrayOf(
            ((usage shr 8) and 0xFF).toByte(),
            (usage and 0xFF).toByte()
        )
        writeV2(listOf(V2Frame(CMD_CONSUMER_DOWN, payload)))
    }

    fun sendConsumerUp(usage: Int) {
        val payload = byteArrayOf(
            ((usage shr 8) and 0xFF).toByte(),
            (usage and 0xFF).toByte()
        )
        writeV2(listOf(V2Frame(CMD_CONSUMER_UP, payload)))
    }


    fun sendSystemAction(action: Byte) {
        writeV2(listOf(V2Frame(CMD_SYSTEM_ACTION, byteArrayOf(action))))
    }

    /**
     * Gửi legacy frame 1 byte (server xử lý ở p[0] check).
     * KHÔNG wrap trong V2 batch.
     */
    fun requestCurrentApp() {
        val s = state
        if (!s.isConnected || !s.isVerified) return
        keyboardChannel.trySend(byteArrayOf(CMD_REPORT_CURRENT_APP))
    }

    /**
     * Reset khẩn cấp: nhả toàn bộ modifier và phím đang giữ.
     * Dùng khi người dùng nghi ngờ phím bị treo (stuck key).
     */
    fun sendResetAll() {
        val s = state
        if (!s.isConnected || !s.isVerified) return

        // Reset modifier mask về 0
        lastSentModifiersMask = 0x00

        // Gửi frame reset: modifier = 0, không có phím nào
        // Gửi tất cả các key up cho các phím có thể đang giữ (an toàn)
        val frames = mutableListOf<V2Frame>()

        // 1. Reset modifiers
        frames.add(V2Frame(CMD_SET_MODIFIERS, byteArrayOf(0x00)))

        // 2. Nhả tất cả các phím modifier riêng lẻ (Ctrl, Shift, Alt, GUI)
        //    để chắc chắn firmware nhận được lệnh key up
        val modifierKeys = listOf(
            0xE0.toByte(), // Left Ctrl
            0xE1.toByte(), // Left Shift
            0xE2.toByte(), // Left Alt
            0xE3.toByte(), // Left GUI
            0xE4.toByte(), // Right Ctrl
            0xE5.toByte(), // Right Shift
            0xE6.toByte(), // Right Alt
            0xE7.toByte()  // Right GUI
        )
        modifierKeys.forEach { key ->
            frames.add(V2Frame(CMD_KEY_UP, byteArrayOf(key)))
        }

        writeV2(frames)
    }

    // ============================================================
    // CLEANUP
    // ============================================================
    fun close() {
        state = HybridConnectionModel()
        lastSentModifiersMask = 0x00
        pendingMouseX = 0
        pendingMouseY = 0
        lastSendMouseTime = 0L
    }

    fun destroy() {
        workerJob?.cancel()
        workerJob = null
        keyboardChannel.close()
        mouseMoveChannel.close()
        mouseEventChannel.close()
        scope.cancel()
    }
}