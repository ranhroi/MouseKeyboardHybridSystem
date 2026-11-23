package com.ranhroi.esp32hidremote.ui.keyboards

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ranhroi.esp32hidremote.models.KeyboardLanguage
import com.ranhroi.esp32hidremote.utils.showToastLong

private const val SILENCE_TIMEOUT_MS = 5000L
private const val FALLBACK_LANGUAGE_TAG = "vi-VN"

class VoiceTextState(
    private val context: Context,
    private val onCommitText: (String) -> Unit,
    private val requestPermission: () -> Unit,
    private val isOfflineProvider: () -> Boolean,
    private val languageTagProvider: () -> String
) {
    var isListening by mutableStateOf(false); private set
    var liveText by mutableStateOf(""); private set
    var soundLevel by mutableFloatStateOf(0f); private set

    /** null = chưa kiểm tra được, true = đã cài gói ngoại tuyến cho ngôn ngữ hiện tại, false = chưa cài */
    var offlineLanguageAvailable by mutableStateOf<Boolean?>(null); private set

    private var recognizer: SpeechRecognizer? = null
    private var isStoppingByUser = false
    private val handler = Handler(Looper.getMainLooper())

    private val silenceTimeoutRunnable = Runnable {
        // Hết 5 giây không có âm thanh: hủy toàn bộ dữ liệu và tắt mic
        forceClose()
    }

    init {
        refreshOfflineAvailability()
    }

    /**
     * Kiểm tra gói ngoại tuyến cho ngôn ngữ hiện tại.
     * So sánh theo mã ngôn ngữ ngắn (vd "vi") thay vì tag đầy đủ (vd "vi-VN"),
     * để tránh trường hợp bàn phím trả về "vi" nhưng gói cài là "vi-VN" (hoặc ngược lại).
     */
    fun refreshOfflineAvailability() {
        if (Build.VERSION.SDK_INT < 33) {
            offlineLanguageAvailable = null
            return
        }
        val currentTag = languageTagProvider().ifBlank { FALLBACK_LANGUAGE_TAG }

        runCatching {
            val probe = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentTag)
            }
            probe.checkRecognitionSupport(
                intent,
                context.mainExecutor,
                object : RecognitionSupportCallback {
                    override fun onSupportResult(recognitionSupport: RecognitionSupport) {
                        val installed = recognitionSupport.installedOnDeviceLanguages
                        val normalizedTarget = currentTag.replace('_', '-').substringBefore('-').lowercase()
                        offlineLanguageAvailable = installed.any { tag ->
                            tag.replace('_', '-').substringBefore('-').lowercase() == normalizedTarget
                        }
                        runCatching { probe.destroy() }
                    }

                    override fun onError(error: Int) {
                        offlineLanguageAvailable = null
                        runCatching { probe.destroy() }
                    }
                }
            )
        }.onFailure {
            offlineLanguageAvailable = null
        }
    }

    fun toggle() {
        if (isListening) {
            stop(graceful = true)
        } else {
            if (isOfflineProvider() && offlineLanguageAvailable == false) {
                showDownloadPrompt(context)
                return
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
            ) start() else requestPermission()
        }
    }

    fun start() {
        ensureRecognizer()
        isListening = true
        isStoppingByUser = false
        listenOnce()
        resetSilenceTimer()
    }

    fun stop(graceful: Boolean = false) {
        if (graceful && liveText.isNotEmpty()) {
            isStoppingByUser = true
            isListening = false
            soundLevel = 0f
            cancelSilenceTimer()
            try {
                recognizer?.stopListening()
            } catch (_: Exception) {
                forceClose()
            }
        } else {
            forceClose()
        }
    }

    private fun forceClose() {
        isListening = false
        isStoppingByUser = false
        liveText = ""
        soundLevel = 0f
        cancelSilenceTimer()
        try {
            recognizer?.cancel()
        } catch (_: Exception) {
        }
    }

    fun destroy() {
        forceClose()
        try {
            recognizer?.destroy()
        } catch (_: Exception) {
        }
        recognizer = null
    }

    private fun resetSilenceTimer() {
        handler.removeCallbacks(silenceTimeoutRunnable)
        handler.postDelayed(silenceTimeoutRunnable, SILENCE_TIMEOUT_MS)
    }

    private fun cancelSilenceTimer() {
        handler.removeCallbacks(silenceTimeoutRunnable)
    }

    private fun ensureRecognizer() {
        if (recognizer != null) return
        recognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        ) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }

        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}

            override fun onBeginningOfSpeech() {
                resetSilenceTimer()
            }

            override fun onRmsChanged(rmsdB: Float) {
                soundLevel = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                soundLevel = 0f
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.trim() ?: ""
                if (partial.isNotEmpty()) {
                    liveText = partial
                    resetSilenceTimer()
                }
            }

            override fun onResults(results: Bundle?) {
                val raw = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.trim() ?: ""
                if (raw.isNotEmpty()) {
                    liveText = ""
                    onCommitText("$raw ")
                }
                forceClose()
            }

            override fun onError(error: Int) {
                if (isStoppingByUser || !isListening) {
                    forceClose()
                    return
                }
                if (error == 12 || error == 13) {
                    showDownloadPrompt(context)
                }
                forceClose()
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun listenOnce() {
        val currentTag = languageTagProvider().ifBlank { FALLBACK_LANGUAGE_TAG }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

            if (isOfflineProvider()) {
                @Suppress("DEPRECATION")
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }

            if (Build.VERSION.SDK_INT >= 33) {
                putExtra(
                    RecognizerIntent.EXTRA_ENABLE_FORMATTING,
                    RecognizerIntent.FORMATTING_OPTIMIZE_QUALITY
                )
                // Không dùng EXTRA_SEGMENTED_SESSION và EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS
                // vì chúng gây loạn chữ và chặn logic tắt sau 5 giây.
            }
        }
        try {
            recognizer?.startListening(intent)
        } catch (_: Exception) {
            forceClose()
        }
    }
}

@Composable
fun rememberVoiceState(
    isOffline: Boolean,
    keyboardLanguage: KeyboardLanguage,
    onCommitText: (String) -> Unit
): VoiceTextState {
    val context = LocalContext.current
    var stateRef by remember { mutableStateOf<VoiceTextState?>(null) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) stateRef?.start() }

    val offlineState = rememberUpdatedState(isOffline)
    val languageState = rememberUpdatedState(keyboardLanguage)

    val voiceState = remember {
        VoiceTextState(
            context = context,
            onCommitText = onCommitText,
            requestPermission = { launcher.launch(Manifest.permission.RECORD_AUDIO) },
            isOfflineProvider = { offlineState.value },
            languageTagProvider = { languageState.value.languageTag.ifBlank { FALLBACK_LANGUAGE_TAG } }
        ).also { stateRef = it }
    }

    // Khi ngôn ngữ bàn phím thay đổi, kiểm tra lại gói ngoại tuyến cho ngôn ngữ mới
    LaunchedEffect(languageState.value.languageTag) {
        voiceState.refreshOfflineAvailability()
    }

    DisposableEffect(Unit) { onDispose { voiceState.destroy() } }
    return voiceState
}

@Composable
fun VoicePreviewBadge(voiceState: VoiceTextState) {
    val animatedScale by animateFloatAsState(
        targetValue = 1f + (voiceState.soundLevel * 0.6f),
        animationSpec = tween(80),
        label = "mic_wave"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .background(Color(0xFF1E1E1E).copy(alpha = 0.9f), RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(18.dp)) {
            Box(
                Modifier
                    .size(12.dp)
                    .scale(animatedScale)
                    .background(Color(0xFF4285F4).copy(alpha = 0.35f), CircleShape)
            )
            Box(
                Modifier
                    .size(8.dp)
                    .background(Color(0xFF4285F4), CircleShape)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = voiceState.liveText.ifEmpty { "Hãy nói gì đó..." },
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun showDownloadPrompt(context: Context) {
    context.showToastLong(
        "Chưa cài đặt gói ngoại tuyến cho ngôn ngữ hiện tại.\n" +
                "Vui lòng vào Cài đặt → Hệ thống → Nhận dạng giọng nói → Nhận dạng ngoại tuyến, " +
                "tải xuống ngôn ngữ tương ứng, sau đó thử lại."
    )
}