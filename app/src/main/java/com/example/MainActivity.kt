package com.example

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.JibonVoiceScreen
import com.example.ui.JibonVoiceViewModel
import com.example.ui.theme.JibonVoiceTheme

class MainActivity : ComponentActivity() {
    private val viewModel: JibonVoiceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        handleSharedTextIntent(intent)
        setContent {
            JibonVoiceTheme(darkTheme = true) {
                JibonVoiceScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSharedTextIntent(intent)
    }

    private fun handleSharedTextIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                viewModel.updateText(sharedText)
                viewModel.showStatusMessage("✅ শেয়ার করা লেখা অ্যাপে চলে এসেছে!", isError = false)
            }
        }
    }
}
