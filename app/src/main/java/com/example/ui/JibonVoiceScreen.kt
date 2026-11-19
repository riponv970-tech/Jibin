package com.example.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.components.GeneratedAudioPlayerCard
import com.example.ui.components.HeroStudioHeader
import com.example.ui.components.LanguageSelectorRow
import com.example.ui.components.RecentClipRow
import com.example.ui.components.VoiceTuningCard
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.CoralError
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.OnCoral
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateDeep
import com.example.ui.theme.SlateMidnight
import com.example.ui.theme.SlateOutline
import com.example.voice.AudioExportHelper
import com.example.voice.GeneratedAudioClip
import com.example.voice.PlaybackState
import com.example.voice.SaveDownloadResult
import kotlinx.coroutines.launch

@Composable
fun JibonVoiceScreen(
    viewModel: JibonVoiceViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val composeClipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    fun handlePasteFromClipboard() {
        val systemClipboard =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val systemText = systemClipboard?.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
        val pasted = systemText?.takeIf { it.isNotBlank() }
            ?: composeClipboard.getText()?.text?.takeIf { it.isNotBlank() }

        if (pasted != null) {
            viewModel.updateText(pasted)
            viewModel.showStatusMessage("✅ লেখা পেস্ট করা হয়েছে!", isError = false)
        } else {
            val msg = "ক্লিপবোর্ডে কোনো লেখা পাওয়া যায়নি। বক্সের ভেতরে চেপে ধরে Paste চাপুন।"
            viewModel.showStatusMessage(msg, isError = true)
            coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
        }
    }

    var pendingClipForSaf by remember { mutableStateOf<GeneratedAudioClip?>(null) }

    val safCreateDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { targetUri ->
        val clip = pendingClipForSaf
        pendingClipForSaf = null
        if (targetUri != null && clip != null) {
            coroutineScope.launch {
                val res = AudioExportHelper.copyFileToUri(context, clip.file, targetUri)
                if (res.isSuccess) {
                    val msg = context.getString(R.string.msg_saved_downloads)
                    viewModel.showStatusMessage(msg, isError = false)
                    snackbarHostState.showSnackbar(msg)
                } else {
                    val errMsg = "ফাইল সেভ করা যায়নি।"
                    viewModel.showStatusMessage(errMsg, isError = true)
                    snackbarHostState.showSnackbar(errMsg)
                }
            }
        }
    }

    val ttsInstallLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshTtsSupport()
    }

    val openTextFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val readResult = runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bytes = stream.readBytes()
                        if (bytes.size > 512 * 1024) {
                            error("ফাইলটি অনেক বড় (সর্বোচ্চ ৫০০ KB টেক্সট ফাইল সাপোর্টেড)।")
                        }
                        if (bytes.any { it == 0.toByte() }) {
                            error("অনুগ্রহ করে শুধু টেক্সট (.txt) ফাইল নির্বাচন করুন।")
                        }
                        bytes.toString(Charsets.UTF_8)
                    }
                }
                val content = readResult.getOrNull()
                val failureMsg = readResult.exceptionOrNull()?.localizedMessage
                when {
                    failureMsg != null -> {
                        viewModel.showStatusMessage(failureMsg, isError = true)
                        snackbarHostState.showSnackbar(failureMsg)
                    }
                    !content.isNullOrBlank() -> {
                        viewModel.updateText(content)
                        viewModel.showStatusMessage("✅ ফাইল থেকে স্টোরি লোড হয়েছে!", isError = false)
                    }
                    else -> {
                        val errMsg = "ফাইল থেকে কোনো লেখা পড়া যায়নি।"
                        viewModel.showStatusMessage(errMsg, isError = true)
                        snackbarHostState.showSnackbar(errMsg)
                    }
                }
            }
        }
    }

    fun handleSaveClip(clip: GeneratedAudioClip) {
        coroutineScope.launch {
            when (val res = AudioExportHelper.saveToDownloads(context, clip)) {
                is SaveDownloadResult.Saved -> {
                    val msg = "✅ Downloads/JibonVoice/${res.displayName} এ সেভ হয়েছে!"
                    viewModel.showStatusMessage(msg, isError = false)
                    snackbarHostState.showSnackbar(msg)
                }

                is SaveDownloadResult.NeedsDocumentPicker -> {
                    pendingClipForSaf = clip
                    safCreateDocumentLauncher.launch(res.suggestedName)
                }

                is SaveDownloadResult.Failed -> {
                    viewModel.showStatusMessage(res.errorMessage, isError = true)
                    snackbarHostState.showSnackbar(res.errorMessage)
                }
            }
        }
    }

    fun handleShareClip(clip: GeneratedAudioClip) {
        try {
            val intent = AudioExportHelper.buildShareIntent(context, clip)
            context.startActivity(Intent.createChooser(intent, "ভয়েস শেয়ার করো"))
        } catch (_: Exception) {
            viewModel.showStatusMessage("শেয়ার অ্যাপ পাওয়া যায়নি।", isError = true)
        }
    }

    fun handleInstallOfflineVoice() {
        try {
            val intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
            ttsInstallLauncher.launch(intent)
        } catch (_: Exception) {
            viewModel.showStatusMessage("ডিভাইসে TTS সেটিংস খোলা যায়নি।", isError = true)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SlateMidnight, SlateDeep)
                )
            ),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth >= 680.dp

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                if (isWideScreen) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = 1120.dp)
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1.05f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            HeroStudioHeader(
                                playbackState = state.playbackState,
                                selectedLanguage = state.selectedLanguage,
                                isDeviceVoiceInstalled = state.deviceSupportMap[state.selectedLanguage] == true
                            )

                            LanguageSelectorRow(
                                selectedLanguage = state.selectedLanguage,
                                deviceSupportMap = state.deviceSupportMap,
                                onSelectLanguage = viewModel::selectLanguage
                            )

                            ScriptEditorCard(
                                text = state.text,
                                quickPhrases = state.selectedLanguage.quickPhrases,
                                onTextChange = viewModel::updateText,
                                onClear = viewModel::clearText,
                                onPaste = ::handlePasteFromClipboard,
                                onImportFile = {
                                    openTextFileLauncher.launch(
                                        arrayOf("text/plain", "text/*", "application/octet-stream")
                                    )
                                },
                                onSelectPhrase = viewModel::applyQuickPhrase
                            )

                            PrimaryActionButtons(
                                playbackState = state.playbackState,
                                isGeneratingFile = state.isGeneratingFile,
                                onPlayClick = viewModel::playVoice,
                                onStopClick = viewModel::stopVoice,
                                onDownloadClick = viewModel::generateDownloadableVoice
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(0.95f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            StatusBanner(
                                message = state.statusBannerMessage,
                                isError = state.isErrorBanner
                            )

                            VoiceTuningCard(
                                speechRate = state.speechRate,
                                speechPitch = state.speechPitch,
                                engineMode = state.engineMode,
                                isExpanded = state.isTuningExpanded,
                                isDeviceVoiceInstalled = state.deviceSupportMap[state.selectedLanguage] == true,
                                onToggleExpand = viewModel::toggleTuningExpanded,
                                onRateChange = viewModel::updateSpeechRate,
                                onPitchChange = viewModel::updateSpeechPitch,
                                onResetTuning = viewModel::resetVoiceTuning,
                                onEngineModeSelected = viewModel::updateEngineMode,
                                onInstallOfflineVoice = ::handleInstallOfflineVoice
                            )

                            OutputSection(
                                state = state,
                                onTogglePlayPause = viewModel::toggleClipPlayback,
                                onSeek = viewModel::seekAudio,
                                onSave = ::handleSaveClip,
                                onShare = ::handleShareClip
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        HeroStudioHeader(
                            playbackState = state.playbackState,
                            selectedLanguage = state.selectedLanguage,
                            isDeviceVoiceInstalled = state.deviceSupportMap[state.selectedLanguage] == true
                        )

                        LanguageSelectorRow(
                            selectedLanguage = state.selectedLanguage,
                            deviceSupportMap = state.deviceSupportMap,
                            onSelectLanguage = viewModel::selectLanguage
                        )

                        ScriptEditorCard(
                            text = state.text,
                            quickPhrases = state.selectedLanguage.quickPhrases,
                            onTextChange = viewModel::updateText,
                            onClear = viewModel::clearText,
                            onPaste = ::handlePasteFromClipboard,
                            onImportFile = {
                                openTextFileLauncher.launch(
                                    arrayOf("text/plain", "text/*", "application/octet-stream")
                                )
                            },
                            onSelectPhrase = viewModel::applyQuickPhrase
                        )

                        VoiceTuningCard(
                            speechRate = state.speechRate,
                            speechPitch = state.speechPitch,
                            engineMode = state.engineMode,
                            isExpanded = state.isTuningExpanded,
                            isDeviceVoiceInstalled = state.deviceSupportMap[state.selectedLanguage] == true,
                            onToggleExpand = viewModel::toggleTuningExpanded,
                            onRateChange = viewModel::updateSpeechRate,
                            onPitchChange = viewModel::updateSpeechPitch,
                            onResetTuning = viewModel::resetVoiceTuning,
                            onEngineModeSelected = viewModel::updateEngineMode,
                            onInstallOfflineVoice = ::handleInstallOfflineVoice
                        )

                        PrimaryActionButtons(
                            playbackState = state.playbackState,
                            isGeneratingFile = state.isGeneratingFile,
                            onPlayClick = viewModel::playVoice,
                            onStopClick = viewModel::stopVoice,
                            onDownloadClick = viewModel::generateDownloadableVoice
                        )

                        StatusBanner(
                            message = state.statusBannerMessage,
                            isError = state.isErrorBanner
                        )

                        OutputSection(
                            state = state,
                            onTogglePlayPause = viewModel::toggleClipPlayback,
                            onSeek = viewModel::seekAudio,
                            onSave = ::handleSaveClip,
                            onShare = ::handleShareClip
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ScriptEditorCard(
    text: String,
    quickPhrases: List<String>,
    onTextChange: (String) -> Unit,
    onClear: () -> Unit,
    onPaste: () -> Unit,
    onImportFile: () -> Unit,
    onSelectPhrase: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, SlateOutline.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.text_input_label),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "${text.length} chars",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = onPaste,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("paste_text_button"),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.ContentPaste,
                            contentDescription = stringResource(R.string.cd_paste_text),
                            tint = EmeraldBright,
                            modifier = Modifier.size(17.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "পেস্ট করুন",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    shape = RoundedCornerShape(50),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = EmeraldContainer.copy(alpha = 0.7f)
                    ),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.55f))
                )

                AssistChip(
                    onClick = onImportFile,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("import_txt_button"),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.UploadFile,
                            contentDescription = "Import text file",
                            tint = ElectricBlue,
                            modifier = Modifier.size(17.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "ফাইল (.txt)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    shape = RoundedCornerShape(50),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = SlateDeep
                    ),
                    border = BorderStroke(1.dp, SlateOutline)
                )

                if (text.isNotEmpty()) {
                    AssistChip(
                        onClick = onClear,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("clear_text_button"),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = stringResource(R.string.cd_clear_text),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        },
                        label = {
                            Text(
                                text = "মুছুন",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        shape = RoundedCornerShape(50),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = SlateDeep
                        ),
                        border = BorderStroke(1.dp, SlateOutline)
                    )
                }
            }

            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                singleLine = false,
                maxLines = Int.MAX_VALUE,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 148.dp, max = 240.dp)
                    .testTag("voice_text_input"),
                placeholder = {
                    Text(
                        text = stringResource(R.string.text_input_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SlateDeep,
                    unfocusedContainerColor = SlateDeep,
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = SlateOutline,
                    cursorColor = EmeraldBright
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(R.string.label_quick_phrases),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickPhrases.forEachIndexed { idx, phrase ->
                        AssistChip(
                            onClick = { onSelectPhrase(phrase) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("quick_phrase_chip_$idx"),
                            label = {
                                Text(
                                    text = phrase,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 190.dp)
                                )
                            },
                            shape = RoundedCornerShape(50),
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = SlateDeep,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = BorderStroke(1.dp, SlateOutline)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrimaryActionButtons(
    playbackState: PlaybackState,
    isGeneratingFile: Boolean,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    val isSpeaking = playbackState is PlaybackState.Speaking
    val isPreparingPlay = playbackState is PlaybackState.Preparing && !isGeneratingFile

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = { if (isSpeaking || isPreparingPlay) onStopClick() else onPlayClick() },
            enabled = !isGeneratingFile,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("play_voice_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isSpeaking || isPreparingPlay) CoralError else EmeraldPrimary,
                contentColor = if (isSpeaking || isPreparingPlay) OnCoral else Color(0xFF042211)
            ),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            if (isPreparingPlay) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = OnCoral,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.btn_stop_voice),
                    style = MaterialTheme.typography.labelLarge
                )
            } else {
                Icon(
                    imageVector = if (isSpeaking) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSpeaking) {
                        stringResource(R.string.btn_stop_voice)
                    } else {
                        stringResource(R.string.btn_play_voice)
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Button(
            onClick = onDownloadClick,
            enabled = !isGeneratingFile,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("download_voice_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricBlue,
                contentColor = Color.White,
                disabledContainerColor = ElectricBlue.copy(alpha = 0.5f),
                disabledContentColor = Color.White.copy(alpha = 0.8f)
            ),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            if (isGeneratingFile) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.btn_generating),
                    style = MaterialTheme.typography.titleMedium
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.btn_download_voice),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun StatusBanner(
    message: String?,
    isError: Boolean
) {
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = expandVertically(spring()) + fadeIn(),
        exit = shrinkVertically(spring()) + fadeOut()
    ) {
        if (!message.isNullOrBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("status_banner"),
                shape = RoundedCornerShape(14.dp),
                color = if (isError) CoralContainer else EmeraldContainer,
                border = BorderStroke(
                    1.dp,
                    if (isError) CoralError.copy(alpha = 0.6f) else EmeraldPrimary.copy(alpha = 0.6f)
                )
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isError) OnCoral else EmeraldBright,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun OutputSection(
    state: JibonVoiceUiState,
    onTogglePlayPause: (GeneratedAudioClip) -> Unit,
    onSeek: (Long) -> Unit,
    onSave: (GeneratedAudioClip) -> Unit,
    onShare: (GeneratedAudioClip) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        AnimatedVisibility(
            visible = state.activeClip != null,
            enter = expandVertically(spring()) + fadeIn(),
            exit = shrinkVertically(spring()) + fadeOut()
        ) {
            state.activeClip?.let { clip ->
                GeneratedAudioPlayerCard(
                    clip = clip,
                    playerStatus = state.playerStatus,
                    onTogglePlayPause = { onTogglePlayPause(clip) },
                    onSeek = onSeek,
                    onSaveToDownloads = { onSave(clip) },
                    onShare = { onShare(clip) }
                )
            }
        }

        if (state.recentClips.size > 1) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.label_recent_clips),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                state.recentClips.drop(1).forEach { clip ->
                    val isPlaying = state.playerStatus.activeClipId == clip.id &&
                        state.playerStatus.isPlaying
                    RecentClipRow(
                        clip = clip,
                        isPlaying = isPlaying,
                        onPlayClick = { onTogglePlayPause(clip) },
                        onSaveClick = { onSave(clip) }
                    )
                }
            }
        }
    }
}
