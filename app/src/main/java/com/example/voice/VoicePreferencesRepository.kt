package com.example.voice

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.voiceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "jibon_voice_preferences"
)

data class VoicePreferences(
    val language: VoiceLanguage = VoiceLanguage.BENGALI,
    val speechRate: Float = 0.9f,
    val speechPitch: Float = 1.0f,
    val engineMode: VoiceEngineMode = VoiceEngineMode.AUTO
)

class VoicePreferencesRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val SPEECH_PITCH = floatPreferencesKey("speech_pitch")
        val ENGINE_MODE = stringPreferencesKey("engine_mode")
    }

    val preferencesFlow: Flow<VoicePreferences> = context.voiceDataStore.data.map { prefs ->
        VoicePreferences(
            language = VoiceLanguage.fromCode(prefs[Keys.LANGUAGE_CODE]),
            speechRate = (prefs[Keys.SPEECH_RATE] ?: 0.9f).coerceIn(0.5f, 2.0f),
            speechPitch = (prefs[Keys.SPEECH_PITCH] ?: 1.0f).coerceIn(0.6f, 1.5f),
            engineMode = VoiceEngineMode.fromKey(prefs[Keys.ENGINE_MODE])
        )
    }

    suspend fun saveLanguage(language: VoiceLanguage) {
        context.voiceDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE_CODE] = language.code
        }
    }

    suspend fun saveSpeechRate(rate: Float) {
        context.voiceDataStore.edit { prefs ->
            prefs[Keys.SPEECH_RATE] = rate.coerceIn(0.5f, 2.0f)
        }
    }

    suspend fun saveSpeechPitch(pitch: Float) {
        context.voiceDataStore.edit { prefs ->
            prefs[Keys.SPEECH_PITCH] = pitch.coerceIn(0.6f, 1.5f)
        }
    }

    suspend fun saveEngineMode(mode: VoiceEngineMode) {
        context.voiceDataStore.edit { prefs ->
            prefs[Keys.ENGINE_MODE] = mode.key
        }
    }
}
