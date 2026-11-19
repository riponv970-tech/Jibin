package com.example

import com.example.voice.VoiceEngineMode
import com.example.voice.VoiceLanguage
import com.example.voice.VoiceSynthesisEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun voiceLanguage_fromCode_resolvesExpectedLanguages() {
        assertEquals(VoiceLanguage.BENGALI, VoiceLanguage.fromCode("bn"))
        assertEquals(VoiceLanguage.ENGLISH, VoiceLanguage.fromCode("en"))
        assertEquals(VoiceLanguage.HINDI, VoiceLanguage.fromCode("hi"))
        assertEquals(VoiceLanguage.BENGALI, VoiceLanguage.fromCode(null))
    }

    @Test
    fun voiceEngineMode_fromKey_resolvesExpectedModes() {
        assertEquals(VoiceEngineMode.AUTO, VoiceEngineMode.fromKey("auto"))
        assertEquals(VoiceEngineMode.ON_DEVICE, VoiceEngineMode.fromKey("offline"))
        assertEquals(VoiceEngineMode.CLOUD_MP3, VoiceEngineMode.fromKey("cloud"))
        assertEquals(VoiceEngineMode.AUTO, VoiceEngineMode.fromKey("unknown"))
    }

    @Test
    fun splitTextForTts_handlesEmptyAndLongBengaliText() {
        assertTrue(VoiceSynthesisEngine.splitTextForTts("   ").isEmpty())

        val shortText = "আসসালামু আলাইকুম সবাই কেমন আছেন"
        assertEquals(listOf(shortText), VoiceSynthesisEngine.splitTextForTts(shortText, 180))

        val longText = List(10) { "আসসালামু আলাইকুম সবাই কেমন আছেন।" }.joinToString(" ")
        val chunks = VoiceSynthesisEngine.splitTextForTts(longText, maxChunkLength = 100)
        assertTrue(chunks.size > 1)
        assertTrue(chunks.all { it.length <= 100 })
    }
}
