package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateDeep
import com.example.ui.theme.SlateMidnight
import com.example.ui.theme.SlateOutline
import com.example.voice.AudioPlayerStatus
import com.example.voice.GeneratedAudioClip
import com.example.voice.PlaybackState
import com.example.voice.VoiceEngineMode
import com.example.voice.VoiceLanguage
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun HeroStudioHeader(
    playbackState: PlaybackState,
    selectedLanguage: VoiceLanguage,
    isDeviceVoiceInstalled: Boolean,
    modifier: Modifier = Modifier
) {
    val isLive = playbackState is PlaybackState.Speaking

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isLive) EmeraldPrimary.copy(alpha = 0.65f) else SlateOutline.copy(alpha = 0.7f)
        ),
        colors = CardDefaults.cardColors(containerColor = SlateCard)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_voice_studio_banner),
                contentDescription = stringResource(R.string.cd_hero_banner),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Dark translucent gradient overlay for crisp legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                SlateMidnight.copy(alpha = 0.94f),
                                SlateDeep.copy(alpha = 0.80f),
                                SlateMidnight.copy(alpha = 0.68f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldPrimary.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.45f))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Mic,
                                contentDescription = null,
                                tint = EmeraldBright,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.studio_title),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.studio_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = EmeraldBright
                            )
                        }
                    }

                    // Voice availability badge
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isLive) {
                            EmeraldPrimary.copy(alpha = 0.22f)
                        } else {
                            SlateDeep.copy(alpha = 0.82f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isLive) EmeraldBright else SlateOutline
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isLive -> EmeraldBright
                                            isDeviceVoiceInstalled -> EmeraldPrimary
                                            else -> ElectricBlue
                                        }
                                    )
                            )
                            Text(
                                text = when {
                                    isLive -> "LIVE"
                                    isDeviceVoiceInstalled -> "${selectedLanguage.regionBadge} · Ready"
                                    else -> "${selectedLanguage.regionBadge} · HD Cloud"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Live animated equalizer strip
                StudioWaveformVisualizer(
                    isActive = isLive,
                    barCount = 28,
                    activeColor = EmeraldBright,
                    secondaryColor = ElectricBlue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                )
            }
        }
    }
}

@Composable
fun StudioWaveformVisualizer(
    isActive: Boolean,
    barCount: Int = 24,
    activeColor: Color = EmeraldPrimary,
    secondaryColor: Color = ElectricBlue,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )
    val energy by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.18f,
        animationSpec = spring(stiffness = 220f),
        label = "wave_energy"
    )

    Canvas(modifier = modifier) {
        val totalWidth = size.width
        val maxHeight = size.height
        if (totalWidth <= 0f || maxHeight <= 0f) return@Canvas

        val slotWidth = totalWidth / barCount
        val barWidth = (slotWidth * 0.56f).coerceAtLeast(3.dp.toPx())

        for (i in 0 until barCount) {
            val normalizedIndex = i.toFloat() / barCount.toFloat()
            val wave1 = sin(phase + i * 0.55f)
            val wave2 = sin(phase * 1.7f - i * 0.35f)
            val combined = abs(wave1 * 0.65f + wave2 * 0.35f)

            val idleWave = 0.25f + 0.35f * abs(sin(i * 0.7f))
            val fraction = ((idleWave * (1f - energy)) + (0.22f + combined * 0.78f) * energy)
                .coerceIn(0.14f, 1f)

            val barHeight = maxHeight * fraction
            val left = i * slotWidth + (slotWidth - barWidth) / 2f
            val top = (maxHeight - barHeight) / 2f

            val color = if (normalizedIndex < 0.55f) {
                activeColor.copy(alpha = if (isActive) 0.95f else 0.42f)
            } else {
                secondaryColor.copy(alpha = if (isActive) 0.92f else 0.38f)
            }

            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
fun LanguageSelectorRow(
    selectedLanguage: VoiceLanguage,
    deviceSupportMap: Map<VoiceLanguage, Boolean>,
    onSelectLanguage: (VoiceLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.label_language),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            VoiceLanguage.entries.forEach { language ->
                val isSelected = language == selectedLanguage
                val containerColor by animateColorAsState(
                    targetValue = if (isSelected) EmeraldPrimary.copy(alpha = 0.16f) else SlateCard,
                    animationSpec = spring(),
                    label = "lang_bg"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) EmeraldPrimary else SlateOutline,
                    animationSpec = spring(),
                    label = "lang_border"
                )
                val offlineReady = deviceSupportMap[language] == true

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelectLanguage(language) }
                        .testTag("language_chip_${language.code}"),
                    shape = RoundedCornerShape(16.dp),
                    color = containerColor,
                    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = language.regionBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = EmeraldBright,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Text(
                            text = language.nativeLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = if (offlineReady) "Offline + HD" else "Cloud HD",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) EmeraldBright else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VoiceTuningCard(
    speechRate: Float,
    speechPitch: Float,
    engineMode: VoiceEngineMode,
    isExpanded: Boolean,
    isDeviceVoiceInstalled: Boolean,
    onToggleExpand: () -> Unit,
    onRateChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onResetTuning: () -> Unit,
    onEngineModeSelected: (VoiceEngineMode) -> Unit,
    onInstallOfflineVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, SlateOutline.copy(alpha = 0.75f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .testTag("toggle_voice_tuning_button"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.label_voice_tuning),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Speed ${String.format("%.1fx", speechRate)} · Pitch ${String.format("%.1fx", speechPitch)} · ${engineMode.titleBn}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (isExpanded) "বন্ধ করুন" else "সেটিংস",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanAccent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(spring()) + fadeIn(),
                exit = shrinkVertically(spring()) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Speed Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.label_speed),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format("%.1fx", speechRate),
                                style = MaterialTheme.typography.titleSmall,
                                color = EmeraldBright
                            )
                        }
                        Slider(
                            value = speechRate,
                            onValueChange = onRateChange,
                            valueRange = 0.5f..2.0f,
                            steps = 14,
                            modifier = Modifier.testTag("speed_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = EmeraldPrimary,
                                activeTrackColor = EmeraldPrimary
                            )
                        )
                    }

                    // Pitch Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.label_pitch),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format("%.1fx", speechPitch),
                                style = MaterialTheme.typography.titleSmall,
                                color = CyanAccent
                            )
                        }
                        Slider(
                            value = speechPitch,
                            onValueChange = onPitchChange,
                            valueRange = 0.6f..1.5f,
                            steps = 8,
                            modifier = Modifier.testTag("pitch_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = CyanAccent,
                                activeTrackColor = CyanAccent
                            )
                        )
                    }

                    // Engine Mode chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VoiceEngineMode.entries.forEach { mode ->
                            val selected = mode == engineMode
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onEngineModeSelected(mode) }
                                    .testTag("engine_mode_${mode.key}"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) ElectricBlue.copy(alpha = 0.2f) else SlateDeep,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) ElectricBlue else SlateOutline
                                )
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = mode.titleBn,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isDeviceVoiceInstalled) {
                            OutlinedButton(
                                onClick = onInstallOfflineVoice,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("install_offline_voice_button"),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                border = BorderStroke(1.dp, SlateOutline)
                            ) {
                                Text(
                                    text = "অফলাইন ভয়েস ইনস্টল",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanAccent
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        FilledTonalButton(
                            onClick = onResetTuning,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("reset_tuning_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ডিফল্ট (0.9x)",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GeneratedAudioPlayerCard(
    clip: GeneratedAudioClip,
    playerStatus: AudioPlayerStatus,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSaveToDownloads: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isThisClipActive = playerStatus.activeClipId == clip.id
    val isPlaying = isThisClipActive && playerStatus.isPlaying
    val totalMs = if (isThisClipActive && playerStatus.durationMs > 0L) {
        playerStatus.durationMs
    } else {
        clip.durationMs.coerceAtLeast(1L)
    }
    val currentMs = if (isThisClipActive) {
        playerStatus.currentPositionMs.coerceIn(0L, totalMs)
    } else {
        0L
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("generated_audio_player_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.5.dp, ElectricBlue.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.label_generated_audio),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = ElectricBlue.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "${clip.extension.uppercase()} · ${formatFileSize(clip.sizeBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanAccent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = clip.textPreview,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Player row with Play/Pause + Waveform + Seekbar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("player_play_pause_button"),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play_pause_clip),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Slider(
                        value = if (totalMs > 0) currentMs.toFloat() / totalMs.toFloat() else 0f,
                        onValueChange = { fraction ->
                            onSeek((fraction * totalMs).toLong())
                        },
                        modifier = Modifier.testTag("player_seek_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricBlue,
                            activeTrackColor = ElectricBlue,
                            inactiveTrackColor = SlateOutline
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDuration(currentMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDuration(totalMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Export Actions: Save to Downloads + Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSaveToDownloads,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("save_to_downloads_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = stringResource(R.string.cd_save_clip),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.btn_save_downloads),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("share_audio_button"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SlateOutline),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = stringResource(R.string.cd_share_clip),
                        tint = CyanAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.btn_share_audio),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun RecentClipRow(
    clip: GeneratedAudioClip,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onPlayClick() },
        shape = RoundedCornerShape(14.dp),
        color = SlateCard,
        border = BorderStroke(
            1.dp,
            if (isPlaying) EmeraldPrimary else SlateOutline.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isPlaying) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = null,
                        tint = if (isPlaying) MaterialTheme.colorScheme.onPrimary else CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = clip.textPreview,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${clip.language.nativeLabel} · ${clip.extension.uppercase()} · ${clip.createdAtLabel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onSaveClick,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = stringResource(R.string.cd_save_clip),
                    tint = ElectricBlue
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format("%02d:%02d", minutes, seconds)
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
        else -> String.format("%.1f MB", bytes / (1024f * 1024f))
    }
}
