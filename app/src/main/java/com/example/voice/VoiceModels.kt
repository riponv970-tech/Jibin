package com.example.voice

import java.io.File
import java.util.Locale

enum class VoiceLanguage(
    val code: String,
    val bcp47Tag: String,
    val locale: Locale,
    val nativeLabel: String,
    val englishLabel: String,
    val regionBadge: String,
    val quickPhrases: List<String>
) {
    BENGALI(
        code = "bn",
        bcp47Tag = "bn-BD",
        locale = Locale("bn", "BD"),
        nativeLabel = "বাংলা",
        englishLabel = "Bangla",
        regionBadge = "BD",
        quickPhrases = listOf(
            "আসসালামু আলাইকুম সবাই কেমন আছেন",
            "জিবন ভয়েস অ্যাপে আপনাকে স্বাগতম!",
            "আজকের দিনটি অনেক সুন্দর কাটুক।",
            "ধন্যবাদ সবাইকে আমার সাথে থাকার জন্য।"
        )
    ),
    ENGLISH(
        code = "en",
        bcp47Tag = "en-US",
        locale = Locale.US,
        nativeLabel = "English",
        englishLabel = "English",
        regionBadge = "US",
        quickPhrases = listOf(
            "Assalamu Alaikum, how is everyone doing today?",
            "Welcome to Jibon Voice Studio. Write text and hear it live!",
            "Thank you for listening and supporting our channel."
        )
    ),
    HINDI(
        code = "hi",
        bcp47Tag = "hi-IN",
        locale = Locale("hi", "IN"),
        nativeLabel = "हिन्दी",
        englishLabel = "Hindi",
        regionBadge = "IN",
        quickPhrases = listOf(
            "नमस्ते दोस्तों, आप सब कैसे हैं?",
            "जीवन वॉइस ऐप में आपका स्वागत है!",
            "इस वीडियो को देखने के लिए बहुत-बहुत धन्यवाद।"
        )
    );

    companion object {
        fun fromCode(code: String?): VoiceLanguage =
            entries.firstOrNull { it.code == code } ?: BENGALI
    }
}

enum class VoiceEngineMode(
    val key: String,
    val titleBn: String,
    val subtitleEn: String
) {
    AUTO("auto", "অটো (স্মার্ট)", "Offline first, Cloud MP3 fallback"),
    ON_DEVICE("offline", "অফলাইন ভয়েস", "Device TextToSpeech engine"),
    CLOUD_MP3("cloud", "অনলাইন HD MP3", "Natural MP3 voice download");

    companion object {
        fun fromKey(key: String?): VoiceEngineMode =
            entries.firstOrNull { it.key == key } ?: AUTO
    }
}

sealed interface PlaybackState {
    data object Idle : PlaybackState
    data class Preparing(val label: String) : PlaybackState
    data class Speaking(val sourceLabel: String) : PlaybackState
    data class Error(val message: String) : PlaybackState
}

data class GeneratedAudioClip(
    val id: String,
    val file: File,
    val mimeType: String,
    val extension: String,
    val language: VoiceLanguage,
    val textPreview: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val createdAtLabel: String,
    val sourceLabel: String
)
