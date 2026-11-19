package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.ui.JibonVoiceScreen
import com.example.ui.theme.JibonVoiceTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun readStringFromContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Jibon Voice", appName)
    }

    @Test
    fun jibonVoiceScreen_rendersCoreControlsAndLanguageSwitch() {
        composeRule.setContent {
            JibonVoiceTheme(darkTheme = true) {
                JibonVoiceScreen()
            }
        }

        composeRule.onNodeWithText("জিবন ভয়েস এপস").assertIsDisplayed()
        composeRule.onNodeWithTag("voice_text_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("paste_text_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("import_txt_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("play_voice_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("download_voice_button").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("language_chip_en").performScrollTo().performClick()
        composeRule.onNodeWithTag("voice_text_input")
            .performScrollTo()
            .assertTextContains("Assalamu Alaikum, how is everyone doing today?")
    }
}
