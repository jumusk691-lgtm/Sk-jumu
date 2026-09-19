package com.example.ui.screens

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.SyntheticAudioGenerator
import com.example.dsp.NoiseCancellationMode
import com.example.ui.MainViewModel
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun AudioCleanerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    val selectedScenario by viewModel.selectedCleanScenario.collectAsState()
    val isCleaning by viewModel.isCleaningFile.collectAsState()
    val progress by viewModel.cleanProgress.collectAsState()
    val denoiseResult by viewModel.lastDenoiseResult.collectAsState()
    val isCleanMode by viewModel.fileAudioCompareMode.collectAsState()

    var isPreviewPlaying by remember { mutableStateOf(false) }
    var audioTrack by remember { mutableStateOf<AudioTrack?>(null) }
    var playbackJob by remember { mutableStateOf<Job?>(null) }

    fun stopAudio() {
        isPreviewPlaying = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioTrack = null
    }

    fun playPreview(samples: ShortArray, sampleRate: Int) {
        stopAudio()
        isPreviewPlaying = true
        try {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            track.play()
            audioTrack = track

            playbackJob = coroutineScope.launch(Dispatchers.Default) {
                var offset = 0
                val chunkSize = 256
                while (isActive && isPreviewPlaying) {
                    if (offset >= samples.size) offset = 0
                    val count = minOf(chunkSize, samples.size - offset)
                    track.write(samples, offset, count)
                    offset += count
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isPreviewPlaying = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopAudio()
        }
    }

    val openAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.runCleanOnUri(uri, isVideo = false, mode = NoiseCancellationMode.SAFELY_VOICE_ZERO)
        }
    }

    val openVideoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.runCleanOnUri(uri, isVideo = true, mode = NoiseCancellationMode.SAFELY_VOICE_ZERO)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("audio_cleaner_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AUDIO CANCELLATION",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Upload & Download Zero-Noise Media",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { openAudioLauncher.launch("audio/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioCardElevated),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VoiceCyan),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("upload_audio_header_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = VoiceCyanGlow,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "UPLOAD",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Big Quick Upload Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(StudioCardBg)
                .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { openAudioLauncher.launch("audio/*") },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("upload_audio_quick_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = StudioCardElevated),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VoiceCyan.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = null,
                        tint = VoiceCyanGlow,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "UPLOAD AUDIO",
                        color = VoiceCyanGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Button(
                    onClick = { openVideoLauncher.launch("video/*") },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("upload_video_quick_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = StudioCardElevated),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ZeroNoiseGreen.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = ZeroNoiseGreenGlow,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "UPLOAD VIDEO",
                        color = ZeroNoiseGreenGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Scenario Selector Cards
        Text(
            text = "SELECT NOISY RECORDING TO ZERO",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SyntheticAudioGenerator.SCENARIOS.forEach { sc ->
                val isSelected = selectedScenario.id == sc.id
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) VoiceCyan.copy(alpha = 0.15f) else StudioCardBg)
                        .border(
                            1.dp,
                            if (isSelected) VoiceCyan else StudioBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.selectCleanScenario(sc.id) }
                        .padding(12.dp)
                        .testTag("scenario_card_${sc.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sc.name,
                                color = if (isSelected) VoiceCyanGlow else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = sc.noiseTypeDescription,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = VoiceCyanGlow,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Process Action Button
        Button(
            onClick = {
                stopAudio()
                viewModel.runCleanOnScenario(NoiseCancellationMode.SAFELY_VOICE_ZERO)
            },
            enabled = !isCleaning,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("run_clean_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = ZeroNoiseGreen
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isCleaning) "RUNNING ZERO-NOISE DSP..." else "CANCEL ALL NOISE (APPLY ZERO NOISE)",
                color = Color.Black,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
        }

        if (isCleaning) {
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = ZeroNoiseGreenGlow,
                trackColor = StudioCardElevated
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Denoise Result Section
        if (denoiseResult != null) {
            val res = denoiseResult!!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(StudioCardBg)
                    .border(1.5.dp, ZeroNoiseGreen.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CLEANING COMPLETE",
                            color = ZeroNoiseGreenGlow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "-${"%.1f".format(res.averageNoiseReductionDb)} dB Noise Cut",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // A/B Comparison Toggle Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudioCardElevated)
                            .padding(4.dp)
                    ) {
                        // Raw Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!isCleanMode) RawNoiseOrange.copy(alpha = 0.3f) else Color.Transparent)
                                .clickable {
                                    if (isCleanMode) {
                                        viewModel.toggleFileCompareMode()
                                        if (isPreviewPlaying) {
                                            playPreview(res.originalSamples, res.sampleRate)
                                        }
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "RAW (NOISY)",
                                color = if (!isCleanMode) RawNoiseOrange else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Clean Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCleanMode) ZeroNoiseGreen.copy(alpha = 0.3f) else Color.Transparent)
                                .clickable {
                                    if (!isCleanMode) {
                                        viewModel.toggleFileCompareMode()
                                        if (isPreviewPlaying) {
                                            playPreview(res.cleanedSamples, res.sampleRate)
                                        }
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ZERO NOISE (CLEAN)",
                                color = if (isCleanMode) ZeroNoiseGreenGlow else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Play/Pause button
                    Button(
                        onClick = {
                            if (isPreviewPlaying) {
                                stopAudio()
                            } else {
                                val samples = if (isCleanMode) res.cleanedSamples else res.originalSamples
                                playPreview(samples, res.sampleRate)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("preview_audio_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCleanMode) VoiceCyan else RawNoiseOrange
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPreviewPlaying) "PAUSE PLAYBACK" else if (isCleanMode) "LISTEN TO CLEAN AUDIO" else "LISTEN TO NOISY AUDIO",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // DOWNLOAD ZERO NOISE AUDIO (Requested by user)
                    Button(
                        onClick = {
                            viewModel.downloadFileToDevice(
                                file = res.cleanFile,
                                suggestedName = "ZeroNoise_Clean_Audio_${System.currentTimeMillis()}.wav",
                                isVideo = false
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("download_zero_noise_audio_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZeroNoiseGreen
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Download Zero Noise Audio",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DOWNLOAD ZERO NOISE AUDIO",
                            color = Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Info
                    Text(
                        text = "Saved to Vault automatically as pristine 16-bit uncompressed WAV. Tap Download to save directly to phone Downloads folder.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
