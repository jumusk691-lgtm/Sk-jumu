package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.DecibelReductionGauge
import com.example.ui.components.DualWaveformOscilloscope
import com.example.ui.components.SpectrumAnalyzer32
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.RawNoiseOrange
import com.example.ui.theme.RawNoiseRed
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoiceCyan
import com.example.ui.theme.VoiceCyanGlow
import com.example.ui.theme.ZeroNoiseGreen
import com.example.ui.theme.ZeroNoiseGreenGlow
import com.example.video.VideoPreset

@Composable
fun VideoAudioScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val videoAudio = viewModel.videoAudioController

    val selectedPreset by videoAudio.selectedPreset.collectAsState()
    val customVideoUri by videoAudio.customVideoUri.collectAsState()
    val isPlaying by videoAudio.isPlaying.collectAsState()
    val isNoiseZeroActive by videoAudio.isNoiseZeroActive.collectAsState()
    val progress by videoAudio.playbackProgress.collectAsState()
    val dspStats by videoAudio.dspStats.collectAsState()
    val isExporting by videoAudio.isExporting.collectAsState()

    val pickVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            videoAudio.setCustomVideo(uri)
            viewModel.showMessage("Custom Video Loaded: Zero-Noise Audio DSP Active")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoAudio.pause()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("video_audio_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "VIDEO AUDIO STUDIO",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Video Audio Always Noise Zero Engine",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            // Pick custom video button
            Button(
                onClick = {
                    pickVideoLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = StudioCardElevated),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ZeroNoiseGreen),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("upload_video_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudUpload,
                    contentDescription = "Upload Video",
                    tint = ZeroNoiseGreenGlow,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "UPLOAD VIDEO",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Scenario Preset Selector
        Text(
            text = "CHOOSE VIDEO SCENARIO",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(videoAudio.videoPresets) { preset ->
                val isSelected = selectedPreset.id == preset.id && customVideoUri == null
                val bg by animateColorAsState(
                    targetValue = if (isSelected) VoiceCyan.copy(alpha = 0.2f) else StudioCardBg,
                    label = "bg"
                )
                val border by animateColorAsState(
                    targetValue = if (isSelected) VoiceCyan else StudioBorder,
                    label = "border"
                )

                Surface(
                    onClick = { videoAudio.loadPreset(preset) },
                    shape = RoundedCornerShape(10.dp),
                    color = bg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, border),
                    modifier = Modifier.testTag("video_preset_${preset.id}")
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Text(
                            text = preset.title,
                            color = if (isSelected) VoiceCyanGlow else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = preset.category,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Video Viewport (Cinematic Player Frame)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(StudioCardBg)
                .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
        ) {
            // Simulated Video Scene Canvas with dynamic wave and atmospheric lighting
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Atmospheric background gradient
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
                    )
                )

                // Simulated video motion lines and ambient glow
                val phaseOffset = progress * 6.28f
                for (line in 0 until 5) {
                    val y = h * 0.2f + line * 28f
                    val alpha = 0.15f + line * 0.05f
                    drawLine(
                        color = if (isNoiseZeroActive) ZeroNoiseGreen.copy(alpha = alpha) else RawNoiseOrange.copy(alpha = alpha),
                        start = Offset(0f, y + kotlin.math.sin(phaseOffset + line).toFloat() * 15f),
                        end = Offset(w, y + kotlin.math.cos(phaseOffset + line).toFloat() * 15f),
                        strokeWidth = 2f
                    )
                }
            }

            // Top Status Overlay: "ALWAYS NOISE ZERO"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isNoiseZeroActive) ZeroNoiseGreen else RawNoiseRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isNoiseZeroActive) "ALWAYS NOISE ZERO: ACTIVE" else "RAW NOISY VIDEO AUDIO",
                            color = if (isNoiseZeroActive) ZeroNoiseGreenGlow else RawNoiseRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = selectedPreset.title,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Center Play Button Overlay
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(1.5.dp, if (isNoiseZeroActive) ZeroNoiseGreen else VoiceCyan, CircleShape)
                    .clickable {
                        if (isPlaying) videoAudio.pause() else videoAudio.play()
                    }
                    .testTag("video_play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = TextPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Bottom Audio Scrub Bar
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Slider(
                    value = progress,
                    onValueChange = { videoAudio.seekTo(it) },
                    colors = SliderDefaults.colors(
                        thumbColor = if (isNoiseZeroActive) ZeroNoiseGreenGlow else VoiceCyan,
                        activeTrackColor = if (isNoiseZeroActive) ZeroNoiseGreen else VoiceCyan,
                        inactiveTrackColor = StudioBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .testTag("video_scrub_slider")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Big Master Switch: "Always Noise Zero Mode"
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (isNoiseZeroActive) ZeroNoiseGreen.copy(alpha = 0.12f) else RawNoiseOrange.copy(alpha = 0.10f)
                )
                .border(
                    1.5.dp,
                    if (isNoiseZeroActive) ZeroNoiseGreen else RawNoiseOrange.copy(alpha = 0.5f),
                    RoundedCornerShape(16.dp)
                )
                .clickable { videoAudio.toggleNoiseZero() }
                .padding(16.dp)
                .testTag("always_noise_zero_master_card")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isNoiseZeroActive) ZeroNoiseGreen else RawNoiseOrange),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isNoiseZeroActive) Icons.Default.SurroundSound else Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "ALWAYS NOISE ZERO",
                            color = if (isNoiseZeroActive) ZeroNoiseGreenGlow else TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (isNoiseZeroActive)
                                "Real-time DSP is stripping all background noise to 0.0"
                            else
                                "Noise bypass active (listening to dirty video audio)",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = isNoiseZeroActive,
                    onCheckedChange = { videoAudio.setNoiseZero(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ZeroNoiseGreenGlow,
                        checkedTrackColor = ZeroNoiseGreen.copy(alpha = 0.4f),
                        uncheckedThumbColor = RawNoiseOrange,
                        uncheckedTrackColor = StudioCardElevated
                    ),
                    modifier = Modifier.testTag("video_noise_zero_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dual Waveform Oscilloscope for Video Audio
        DualWaveformOscilloscope(
            stats = dspStats,
            isNoiseZeroActive = isNoiseZeroActive
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Spectrum Analyzer
        SpectrumAnalyzer32(bands = dspStats.spectrumBands)

        Spacer(modifier = Modifier.height(12.dp))

        // Decibel Reduction Gauge
        DecibelReductionGauge(
            stats = dspStats,
            isNoiseZeroActive = isNoiseZeroActive
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Export & Download Clean Video Audio Action
        Button(
            onClick = {
                videoAudio.exportCleanAudio { file, reductionDb ->
                    // Save to Vault
                    viewModel.saveVideoAudioToVault(
                        file = file,
                        reductionDb = reductionDb,
                        title = "${selectedPreset.title} (Cleaned Audio)"
                    )
                    // Also Download directly to device Downloads folder
                    viewModel.downloadFileToDevice(
                        file = file,
                        suggestedName = "ZeroNoise_Clean_VideoAudio_${System.currentTimeMillis()}.wav",
                        isVideo = false
                    )
                }
            },
            enabled = !isExporting,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("download_video_audio_button"),
            colors = ButtonDefaults.buttonColors(containerColor = ZeroNoiseGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download Clean Video Audio",
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isExporting) "PROCESSING ZERO NOISE..." else "DOWNLOAD ZERO NOISE VIDEO AUDIO",
                color = Color.Black,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
