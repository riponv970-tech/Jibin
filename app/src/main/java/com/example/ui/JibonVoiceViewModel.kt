package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voice.AudioPlayerStatus
import com.example.voice.GeneratedAudioClip
import com.example.voice.PlaybackState
import com.example.voice.VoiceEngineMode
import com.example.voice.VoiceLanguage
import com.example.voice.VoicePreferencesRepository
import com.example.voice.VoiceSynthesisEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class JibonVoiceUiState(
    val text: String = "আসসালামু আলাইকুম সবাই কেমন আছেন",
    val selectedLanguage: VoiceLanguage = VoiceLanguage.BENGALI,
    val speechRate: Float = 0.9f,
    val speechPitch: Float = 1.0f,
    val engineMode: VoiceEngineMode = VoiceEngineMode.AUTO,
    val isTtsReady: Boolean = false,
    val deviceSupportMap: Map<VoiceLanguage, Boolean> = emptyMap(),
    val playbackState: PlaybackState = PlaybackState.Idle,
    val isGeneratingFile: Boolean = false,
    val activeClip: GeneratedAudioClip? = null,
    val recentClips: List<GeneratedAudioClip> = emptyList(),
    val playerStatus: AudioPlayerStatus = AudioPlayerStatus(),
    val statusBannerMessage: String? = null,
    val isErrorBanner: Boolean = false,
    val isTuningExpanded: Boolean = false
)

class JibonVoiceViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = VoicePreferencesRepository(application)
    private val engine = VoiceSynthesisEngine(application)

    private val _uiState = MutableStateFlow(JibonVoiceUiState())
    val uiState: StateFlow<JibonVoiceUiState> = _uiState.asStateFlow()

    private var activeJob: Job? = null
    private var prefsInitialized = false

    init {
        viewModelScope.launch {
            prefsRepo.preferencesFlow.collect { prefs ->
                _uiState.update { current ->
                    val shouldUpdateSample = !prefsInitialized &&
                        current.text == VoiceLanguage.BENGALI.quickPhrases.first() &&
                        prefs.language != VoiceLanguage.BENGALI
                    prefsInitialized = true
                    current.copy(
                        selectedLanguage = prefs.language,
                        speechRate = prefs.speechRate,
                        speechPitch = prefs.speechPitch,
                        engineMode = prefs.engineMode,
                        text = if (shouldUpdateSample) prefs.language.quickPhrases.first() else current.text
                    )
                }
            }
        }

        viewModelScope.launch {
            engine.isTtsReady.collect { ready ->
                _uiState.update { it.copy(isTtsReady = ready) }
            }
        }

        viewModelScope.launch {
            engine.deviceSupportMap.collect { map ->
                _uiState.update { it.copy(deviceSupportMap = map) }
            }
        }

        viewModelScope.launch {
            engine.playerStatus.collect { status ->
                _uiState.update { state ->
                    val updatedPlayback = if (
                        !status.isPlaying &&
                        state.playbackState is PlaybackState.Speaking &&
                        status.activeClipId != null &&
                        status.activeClipId == state.activeClip?.id
                    ) {
                        PlaybackState.Idle
                    } else {
                        state.playbackState
                    }
                    state.copy(
                        playerStatus = status,
                        playbackState = updatedPlayback
                    )
                }
            }
        }
    }

    fun refreshTtsSupport() {
        engine.refreshLanguageSupport()
    }

    fun updateText(newText: String) {
        _uiState.update {
            it.copy(
                text = newText,
                statusBannerMessage = if (it.isErrorBanner) null else it.statusBannerMessage,
                isErrorBanner = false
            )
        }
    }

    fun clearText() {
        _uiState.update {
            it.copy(
                text = "",
                statusBannerMessage = null,
                isErrorBanner = false
            )
        }
    }

    fun applyQuickPhrase(phrase: String) {
        _uiState.update {
            it.copy(
                text = phrase,
                statusBannerMessage = null,
                isErrorBanner = false
            )
        }
    }

    fun selectLanguage(language: VoiceLanguage) {
        val currentState = _uiState.value
        val previousDefaults = VoiceLanguage.entries.flatMap { it.quickPhrases }
        val shouldSwitchSample = currentState.text.isBlank() ||
            previousDefaults.any { it == currentState.text.trim() }

        _uiState.update {
            it.copy(
                selectedLanguage = language,
                text = if (shouldSwitchSample) language.quickPhrases.first() else it.text,
                statusBannerMessage = null,
                isErrorBanner = false
            )
        }
        viewModelScope.launch {
            prefsRepo.saveLanguage(language)
        }
    }

    fun updateSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.5f, 2.0f)
        _uiState.update { it.copy(speechRate = clamped) }
        viewModelScope.launch { prefsRepo.saveSpeechRate(clamped) }
    }

    fun updateSpeechPitch(pitch: Float) {
        val clamped = pitch.coerceIn(0.6f, 1.5f)
        _uiState.update { it.copy(speechPitch = clamped) }
        viewModelScope.launch { prefsRepo.saveSpeechPitch(clamped) }
    }

    fun resetVoiceTuning() {
        updateSpeechRate(0.9f)
        updateSpeechPitch(1.0f)
    }

    fun updateEngineMode(mode: VoiceEngineMode) {
        _uiState.update { it.copy(engineMode = mode, statusBannerMessage = null, isErrorBanner = false) }
        viewModelScope.launch { prefsRepo.saveEngineMode(mode) }
    }

    fun toggleTuningExpanded() {
        _uiState.update { it.copy(isTuningExpanded = !it.isTuningExpanded) }
    }

    fun showStatusMessage(message: String, isError: Boolean = false) {
        _uiState.update {
            it.copy(
                statusBannerMessage = message,
                isErrorBanner = isError
            )
        }
    }

    fun clearStatusBanner() {
        _uiState.update { it.copy(statusBannerMessage = null, isErrorBanner = false) }
    }

    fun playVoice() {
        val state = _uiState.value
        val cleanText = state.text.trim()
        if (cleanText.isEmpty()) {
            showStatusMessage("আগে কিছু লেখো ভাই!", isError = true)
            return
        }

        activeJob?.cancel()
        engine.stopAllPlayback()

        when (state.engineMode) {
            VoiceEngineMode.ON_DEVICE -> {
                val started = startOnDeviceSpeech(cleanText, state.selectedLanguage, state.speechRate, state.speechPitch)
                if (!started) {
                    showStatusMessage(
                        "এই ডিভাইসে ${state.selectedLanguage.nativeLabel} অফলাইন ভয়েস ইনস্টল নেই। অটো বা অনলাইন মোড ব্যবহার করুন।",
                        isError = true
                    )
                    _uiState.update { it.copy(playbackState = PlaybackState.Idle) }
                }
            }

            VoiceEngineMode.CLOUD_MP3 -> {
                playViaCloudMp3(cleanText, state.selectedLanguage, state.speechRate, state.speechPitch)
            }

            VoiceEngineMode.AUTO -> {
                val deviceSupported = engine.isDeviceVoiceSupported(state.selectedLanguage)
                if (deviceSupported) {
                    val started = startOnDeviceSpeech(
                        cleanText,
                        state.selectedLanguage,
                        state.speechRate,
                        state.speechPitch
                    )
                    if (!started) {
                        playViaCloudMp3(cleanText, state.selectedLanguage, state.speechRate, state.speechPitch)
                    }
                } else {
                    playViaCloudMp3(cleanText, state.selectedLanguage, state.speechRate, state.speechPitch)
                }
            }
        }
    }

    private fun startOnDeviceSpeech(
        text: String,
        language: VoiceLanguage,
        rate: Float,
        pitch: Float
    ): Boolean {
        _uiState.update {
            it.copy(
                playbackState = PlaybackState.Preparing("অফলাইন ভয়েস চালু হচ্ছে..."),
                statusBannerMessage = "✅ ভয়েস চলতেছে ভাই...",
                isErrorBanner = false
            )
        }
        return engine.speakOnDevice(
            text = text,
            language = language,
            rate = rate,
            pitch = pitch,
            onStarted = {
                _uiState.update {
                    it.copy(
                        playbackState = PlaybackState.Speaking("অফলাইন ভয়েস (${language.nativeLabel})"),
                        statusBannerMessage = "✅ ভয়েস চলতেছে ভাই...",
                        isErrorBanner = false
                    )
                }
            },
            onCompleted = {
                _uiState.update { it.copy(playbackState = PlaybackState.Idle) }
            },
            onError = { err ->
                _uiState.update {
                    it.copy(
                        playbackState = PlaybackState.Idle,
                        statusBannerMessage = err,
                        isErrorBanner = true
                    )
                }
            }
        )
    }

    private fun playViaCloudMp3(
        text: String,
        language: VoiceLanguage,
        speed: Float,
        pitch: Float
    ) {
        activeJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    playbackState = PlaybackState.Preparing("ভয়েস তৈরি হচ্ছে..."),
                    statusBannerMessage = "⏳ ভয়েস প্রস্তুত হচ্ছে...",
                    isErrorBanner = false
                )
            }

            val result = engine.synthesizeCloudMp3(text, language)
            result.onSuccess { file ->
                val clipId = "preview_${UUID.randomUUID()}"
                engine.playAudioFile(
                    clipId = clipId,
                    file = file,
                    playbackSpeed = speed,
                    playbackPitch = pitch,
                    onCompletion = {
                        _uiState.update { it.copy(playbackState = PlaybackState.Idle) }
                    },
                    onError = { err ->
                        _uiState.update {
                            it.copy(
                                playbackState = PlaybackState.Idle,
                                statusBannerMessage = err,
                                isErrorBanner = true
                            )
                        }
                    }
                )
                _uiState.update {
                    it.copy(
                        playbackState = PlaybackState.Speaking("অনলাইন HD (${language.nativeLabel})"),
                        statusBannerMessage = "✅ ভয়েস চলতেছে ভাই...",
                        isErrorBanner = false
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        playbackState = PlaybackState.Idle,
                        statusBannerMessage = err.localizedMessage
                            ?: "ইন্টারনেট সংযোগ না থাকায় ভয়েস চালানো যায়নি।",
                        isErrorBanner = true
                    )
                }
            }
        }
    }

    fun stopVoice() {
        activeJob?.cancel()
        engine.stopAllPlayback()
        _uiState.update {
            it.copy(
                playbackState = PlaybackState.Idle,
                statusBannerMessage = "ভয়েস থামানো হয়েছে।",
                isErrorBanner = false
            )
        }
    }

    fun generateDownloadableVoice() {
        val state = _uiState.value
        val cleanText = state.text.trim()
        if (cleanText.isEmpty()) {
            showStatusMessage("আগে কিছু লেখো ভাই!", isError = true)
            return
        }

        activeJob?.cancel()
        engine.stopAllPlayback()

        activeJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGeneratingFile = true,
                    playbackState = PlaybackState.Preparing("ডাউনলোড অডিও তৈরি হচ্ছে..."),
                    statusBannerMessage = "⏳ অডিও ফাইল তৈরি হচ্ছে...",
                    isErrorBanner = false
                )
            }

            val (fileResult, isMp3, sourceLabel) = when (state.engineMode) {
                VoiceEngineMode.ON_DEVICE -> {
                    val wav = engine.synthesizeOnDeviceToWav(
                        cleanText,
                        state.selectedLanguage,
                        state.speechRate,
                        state.speechPitch
                    )
                    Triple(wav, false, "অফলাইন WAV")
                }

                VoiceEngineMode.CLOUD_MP3 -> {
                    val mp3 = engine.synthesizeCloudMp3(cleanText, state.selectedLanguage)
                    Triple(mp3, true, "অনলাইন MP3")
                }

                VoiceEngineMode.AUTO -> {
                    val cloud = engine.synthesizeCloudMp3(cleanText, state.selectedLanguage)
                    if (cloud.isSuccess) {
                        Triple(cloud, true, "HD MP3")
                    } else if (engine.isDeviceVoiceSupported(state.selectedLanguage)) {
                        val wav = engine.synthesizeOnDeviceToWav(
                            cleanText,
                            state.selectedLanguage,
                            state.speechRate,
                            state.speechPitch
                        )
                        Triple(wav, false, "অফলাইন WAV")
                    } else {
                        Triple(cloud, true, "অনলাইন MP3")
                    }
                }
            }

            fileResult.onSuccess { file ->
                val durationMs = engine.readAudioDurationMs(file)
                val timeLabel = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
                val preview = if (cleanText.length > 48) cleanText.take(48) + "…" else cleanText
                val clip = GeneratedAudioClip(
                    id = UUID.randomUUID().toString(),
                    file = file,
                    mimeType = if (isMp3) "audio/mpeg" else "audio/wav",
                    extension = if (isMp3) "mp3" else "wav",
                    language = state.selectedLanguage,
                    textPreview = preview,
                    durationMs = durationMs,
                    sizeBytes = file.length(),
                    createdAtLabel = timeLabel,
                    sourceLabel = sourceLabel
                )

                _uiState.update { current ->
                    current.copy(
                        isGeneratingFile = false,
                        activeClip = clip,
                        recentClips = (listOf(clip) + current.recentClips).take(8),
                        playbackState = PlaybackState.Speaking("${clip.language.nativeLabel} · $sourceLabel"),
                        statusBannerMessage = "⬇️ ভয়েস আইছে ভাই! এখন নিচের প্লেয়ার থেকে শুনুন অথবা ফোনে সেভ করুন।",
                        isErrorBanner = false
                    )
                }

                engine.playAudioFile(
                    clipId = clip.id,
                    file = clip.file,
                    playbackSpeed = if (isMp3) state.speechRate else 1.0f,
                    playbackPitch = if (isMp3) state.speechPitch else 1.0f,
                    onCompletion = {
                        _uiState.update { it.copy(playbackState = PlaybackState.Idle) }
                    }
                )
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isGeneratingFile = false,
                        playbackState = PlaybackState.Idle,
                        statusBannerMessage = err.localizedMessage
                            ?: "অডিও তৈরি করতে সমস্যা হয়েছে। ইন্টারনেট বা অফলাইন ভয়েস চেক করুন।",
                        isErrorBanner = true
                    )
                }
            }
        }
    }

    fun toggleClipPlayback(clip: GeneratedAudioClip) {
        val state = _uiState.value
        _uiState.update { it.copy(activeClip = clip) }
        val speed = if (clip.extension == "mp3") state.speechRate else 1.0f
        val pitch = if (clip.extension == "mp3") state.speechPitch else 1.0f
        engine.togglePlayPause(clip, playbackSpeed = speed, playbackPitch = pitch)
    }

    fun seekAudio(positionMs: Long) {
        engine.seekTo(positionMs)
    }

    override fun onCleared() {
        super.onCleared()
        engine.release()
    }
}
