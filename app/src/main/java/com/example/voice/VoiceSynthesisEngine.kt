package com.example.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

data class AudioPlayerStatus(
    val activeClipId: String? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L
)

class VoiceSynthesisEngine(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val audioCacheDir: File by lazy {
        File(appContext.cacheDir, "tts_audio").apply { mkdirs() }
    }

    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    private val _deviceSupportMap = MutableStateFlow<Map<VoiceLanguage, Boolean>>(emptyMap())
    val deviceSupportMap: StateFlow<Map<VoiceLanguage, Boolean>> = _deviceSupportMap.asStateFlow()

    private val _playerStatus = MutableStateFlow(AudioPlayerStatus())
    val playerStatus: StateFlow<AudioPlayerStatus> = _playerStatus.asStateFlow()

    private var onTtsStartedCallback: (() -> Unit)? = null
    private var onTtsCompletedCallback: (() -> Unit)? = null
    private var onTtsErrorCallback: ((String) -> Unit)? = null
    private val synthesisWaiters = mutableMapOf<String, (Boolean) -> Unit>()

    init {
        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                _isTtsReady.value = true
                refreshLanguageSupport()
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        scope.launch {
                            if (utteranceId?.startsWith("speak_") == true) {
                                onTtsStartedCallback?.invoke()
                            }
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        scope.launch {
                            if (utteranceId != null && synthesisWaiters.containsKey(utteranceId)) {
                                synthesisWaiters.remove(utteranceId)?.invoke(true)
                            } else if (utteranceId?.startsWith("speak_") == true) {
                                onTtsCompletedCallback?.invoke()
                            }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        handleTtsError(utteranceId)
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        handleTtsError(utteranceId)
                    }
                })
            } else {
                _isTtsReady.value = false
            }
        }
    }

    private fun handleTtsError(utteranceId: String?) {
        scope.launch {
            if (utteranceId != null && synthesisWaiters.containsKey(utteranceId)) {
                synthesisWaiters.remove(utteranceId)?.invoke(false)
            } else if (utteranceId?.startsWith("speak_") == true) {
                onTtsErrorCallback?.invoke("অফলাইন ভয়েস ইঞ্জিনে সমস্যা হয়েছে।")
            }
        }
    }

    fun refreshLanguageSupport() {
        val engine = tts ?: return
        if (!_isTtsReady.value) return
        val map = VoiceLanguage.entries.associateWith { lang ->
            val availability = engine.isLanguageAvailable(lang.locale)
            availability == TextToSpeech.LANG_AVAILABLE ||
                availability == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                availability == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
        }
        _deviceSupportMap.value = map
    }

    fun isDeviceVoiceSupported(language: VoiceLanguage): Boolean =
        _deviceSupportMap.value[language] == true

    fun speakOnDevice(
        text: String,
        language: VoiceLanguage,
        rate: Float,
        pitch: Float,
        onStarted: () -> Unit,
        onCompleted: () -> Unit,
        onError: (String) -> Unit
    ): Boolean {
        val engine = tts ?: return false
        if (!_isTtsReady.value) return false
        val langResult = engine.setLanguage(language.locale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            return false
        }

        stopAllPlayback()
        onTtsStartedCallback = onStarted
        onTtsCompletedCallback = onCompleted
        onTtsErrorCallback = onError

        engine.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
        engine.setPitch(pitch.coerceIn(0.6f, 1.5f))

        val utteranceId = "speak_${UUID.randomUUID()}"
        val params = Bundle()
        val queued = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        return queued == TextToSpeech.SUCCESS
    }

    suspend fun synthesizeOnDeviceToWav(
        text: String,
        language: VoiceLanguage,
        rate: Float,
        pitch: Float
    ): Result<File> = withContext(Dispatchers.Main.immediate) {
        val engine = tts ?: return@withContext Result.failure(IllegalStateException("TTS not ready"))
        if (!_isTtsReady.value) {
            return@withContext Result.failure(IllegalStateException("TTS initializing"))
        }

        val langResult = engine.setLanguage(language.locale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            return@withContext Result.failure(IllegalStateException("Offline voice not installed"))
        }

        engine.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
        engine.setPitch(pitch.coerceIn(0.6f, 1.5f))

        val outFile = File(audioCacheDir, "voice_${language.code}_${System.currentTimeMillis()}.wav")
        val utteranceId = "synth_${UUID.randomUUID()}"

        val success = withTimeoutOrNull(30_000L) {
            suspendCancellableCoroutine { cont ->
                synthesisWaiters[utteranceId] = { ok ->
                    if (cont.isActive) cont.resume(ok)
                }
                cont.invokeOnCancellation {
                    synthesisWaiters.remove(utteranceId)
                }
                val result = engine.synthesizeToFile(text, Bundle(), outFile, utteranceId)
                if (result != TextToSpeech.SUCCESS) {
                    synthesisWaiters.remove(utteranceId)
                    if (cont.isActive) cont.resume(false)
                }
            }
        } ?: false

        if (success && outFile.exists() && outFile.length() > 44L) {
            Result.success(outFile)
        } else {
            outFile.delete()
            Result.failure(IllegalStateException("অফলাইন অডিও তৈরি ব্যর্থ হয়েছে।"))
        }
    }

    suspend fun synthesizeCloudMp3(
        text: String,
        language: VoiceLanguage
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val chunks = splitTextForTts(text, maxChunkLength = 180)
            if (chunks.isEmpty()) error("কোনো লেখা পাওয়া যায়নি।")

            val combined = ByteArrayOutputStream()
            for (chunk in chunks) {
                val encoded = URLEncoder.encode(chunk, "UTF-8")
                val url =
                    "https://translate.google.com/translate_tts?ie=UTF-8&q=$encoded&tl=${language.code}&client=tw-ob"
                val request = Request.Builder()
                    .url(url)
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                    )
                    .header("Referer", "https://translate.google.com/")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        error("অনলাইন ভয়েস সার্ভার সাড়া দেয়নি (${response.code})")
                    }
                    val bytes = response.body?.bytes()
                    if (bytes == null || bytes.isEmpty()) {
                        error("খালি অডিও ডাটা পাওয়া গেছে।")
                    }
                    combined.write(bytes)
                }
            }

            val finalBytes = combined.toByteArray()
            if (finalBytes.size < 128) {
                error("সঠিক অডিও তৈরি হয়নি।")
            }

            val outFile = File(audioCacheDir, "voice_${language.code}_${System.currentTimeMillis()}.mp3")
            outFile.writeBytes(finalBytes)
            outFile
        }
    }

    fun readAudioDurationMs(file: File): Long {
        if (!file.exists()) return 0L
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    fun playAudioFile(
        clipId: String,
        file: File,
        playbackSpeed: Float = 1.0f,
        playbackPitch: Float = 1.0f,
        onCompletion: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        stopAllPlayback()
        if (!file.exists()) {
            onError?.invoke("অডিও ফাইল পাওয়া যায়নি।")
            return
        }

        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    stopProgressTicker()
                    val total = duration.toLong().coerceAtLeast(0L)
                    _playerStatus.value = AudioPlayerStatus(
                        activeClipId = clipId,
                        isPlaying = false,
                        currentPositionMs = 0L,
                        durationMs = total
                    )
                    onCompletion?.invoke()
                }
                setOnErrorListener { _, _, _ ->
                    stopProgressTicker()
                    _playerStatus.value = AudioPlayerStatus()
                    onError?.invoke("অডিও চালাতে সমস্যা হয়েছে।")
                    true
                }
                start()
                applyPlaybackParams(this, playbackSpeed, playbackPitch)
            }
            mediaPlayer = mp
            val totalDuration = mp.duration.toLong().coerceAtLeast(0L)
            _playerStatus.value = AudioPlayerStatus(
                activeClipId = clipId,
                isPlaying = true,
                currentPositionMs = 0L,
                durationMs = totalDuration
            )
            startProgressTicker(clipId, totalDuration)
        } catch (e: Exception) {
            _playerStatus.value = AudioPlayerStatus()
            onError?.invoke(e.localizedMessage ?: "অডিও চালু করা যায়নি।")
        }
    }

    private fun applyPlaybackParams(mp: MediaPlayer, speed: Float, pitch: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && (speed != 1.0f || pitch != 1.0f)) {
            try {
                mp.playbackParams = PlaybackParams()
                    .setSpeed(speed.coerceIn(0.5f, 2.0f))
                    .setPitch(pitch.coerceIn(0.6f, 1.5f))
            } catch (_: Exception) {
            }
        }
    }

    fun togglePlayPause(
        clip: GeneratedAudioClip,
        playbackSpeed: Float = 1.0f,
        playbackPitch: Float = 1.0f
    ) {
        val current = _playerStatus.value
        val mp = mediaPlayer
        if (current.activeClipId == clip.id && mp != null) {
            if (mp.isPlaying) {
                mp.pause()
                stopProgressTicker()
                _playerStatus.value = current.copy(
                    isPlaying = false,
                    currentPositionMs = mp.currentPosition.toLong()
                )
            } else {
                mp.start()
                applyPlaybackParams(mp, playbackSpeed, playbackPitch)
                _playerStatus.value = current.copy(isPlaying = true)
                startProgressTicker(clip.id, current.durationMs)
            }
        } else {
            playAudioFile(
                clipId = clip.id,
                file = clip.file,
                playbackSpeed = playbackSpeed,
                playbackPitch = playbackPitch
            )
        }
    }

    fun seekTo(positionMs: Long) {
        val mp = mediaPlayer ?: return
        val clamped = positionMs.coerceIn(0L, mp.duration.toLong().coerceAtLeast(0L))
        mp.seekTo(clamped.toInt())
        _playerStatus.value = _playerStatus.value.copy(currentPositionMs = clamped)
    }

    fun stopAllPlayback() {
        tts?.stop()
        stopProgressTicker()
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) mp.stop()
                mp.release()
            } catch (_: Exception) {
            }
        }
        mediaPlayer = null
        _playerStatus.value = _playerStatus.value.copy(isPlaying = false, currentPositionMs = 0L)
    }

    private fun startProgressTicker(clipId: String, totalDurationMs: Long) {
        stopProgressTicker()
        progressJob = scope.launch {
            while (isActive) {
                val mp = mediaPlayer ?: break
                val playing = try {
                    mp.isPlaying
                } catch (_: Exception) {
                    false
                }
                if (!playing) break
                val pos = try {
                    mp.currentPosition.toLong()
                } catch (_: Exception) {
                    0L
                }
                _playerStatus.value = AudioPlayerStatus(
                    activeClipId = clipId,
                    isPlaying = true,
                    currentPositionMs = pos,
                    durationMs = totalDurationMs
                )
                delay(60L)
            }
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopAllPlayback()
        tts?.shutdown()
        tts = null
        scope.cancel()
    }

    companion object {
        internal fun splitTextForTts(input: String, maxChunkLength: Int = 180): List<String> {
            val cleaned = input.trim().replace(Regex("\\s+"), " ")
            if (cleaned.isEmpty()) return emptyList()
            if (cleaned.length <= maxChunkLength) return listOf(cleaned)

            val chunks = mutableListOf<String>()
            var remaining = cleaned
            val delimiters = charArrayOf('।', '.', '!', '?', '\n', ',', ';', ' ')

            while (remaining.length > maxChunkLength) {
                val slice = remaining.substring(0, maxChunkLength)
                var splitIndex = -1
                for (delim in delimiters) {
                    val idx = slice.lastIndexOf(delim)
                    if (idx > maxChunkLength / 3) {
                        splitIndex = idx + 1
                        break
                    }
                }
                if (splitIndex <= 0) splitIndex = maxChunkLength
                val part = remaining.substring(0, splitIndex).trim()
                if (part.isNotEmpty()) chunks.add(part)
                remaining = remaining.substring(splitIndex).trimStart()
            }
            if (remaining.isNotEmpty()) {
                chunks.add(remaining)
            }
            return chunks
        }
    }
}
