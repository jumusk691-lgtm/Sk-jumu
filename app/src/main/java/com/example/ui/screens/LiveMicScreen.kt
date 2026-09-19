package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.dsp.NoiseCancellationMode
import com.example.ui.MainViewModel
import com.example.ui.components.AncGlowRing
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

@Composable
fun LiveMicScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val liveMic = viewModel.liveMicController

    val isProcessing by liveMic.isProcessing.collectAsState()
    val isRecording by liveMic.isRecording.collectAsState()
    val recordSec by liveMic.recordingDurationSec.collectAsState()
    val isMonitoring by liveMic.isMonitoring.collectAsState()
    val hardwareActive by liveMic.hardwareSuppressorActive.collectAsState()
    val dspStats by liveMic.dspStats.collectAsState()

    var isNoiseZeroActive by remember { mutableStateOf(true) }
    var activeMode by remember { mutableStateOf(NoiseCancellationMode.SAFELY_VOICE_ZERO) }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            liveMic.startCapture()
        } else {
            viewModel.showMessage("Microphone permission required for live hardware capture")
            liveMic.startCapture() // Still starts fallback simulated acoustic stream
        }
    }

    // Auto-start capture on first launch
    LaunchedEffect(Unit) {
        if (!hasMicPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            liveMic.startCapture()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("live_mic_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "ZERONOISE",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isNoiseZeroActive) ZeroNoiseGreen.copy(alpha = 0.2f) else RawNoiseOrange.copy(alpha = 0.2f))
                            .border(1.dp, if (isNoiseZeroActive) ZeroNoiseGreen else RawNoiseOrange, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isNoiseZeroActive) "ZERO NOISE" else "RAW BYPASS",
                            color = if (isNoiseZeroActive) ZeroNoiseGreenGlow else RawNoiseOrange,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Text(
                    text = "Acoustic Voice Isolation & Ambient Silence",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = { viewModel.openSettings(true) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(StudioCardElevated)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Acoustic Tuning Settings",
                    tint = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Center ANC Glow Ring with Toggle
        Box(
            modifier = Modifier
                .clickable {
                    isNoiseZeroActive = !isNoiseZeroActive
                    liveMic.setNoiseCancellationEnabled(isNoiseZeroActive)
                }
                .testTag("anc_toggle_button"),
            contentAlignment = Alignment.Center
        ) {
            AncGlowRing(isNoiseZeroActive = isNoiseZeroActive)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Mode Selector Chips
        Text(
            text = "NOISE CANCELLATION PRESET",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(NoiseCancellationMode.values()) { mode ->
                val isSelected = activeMode == mode
                val chipBg by animateColorAsState(
                    targetValue = if (isSelected) VoiceCyan.copy(alpha = 0.2f) else StudioCardBg,
                    label = "chip_bg"
                )
                val chipBorder by animateColorAsState(
                    targetValue = if (isSelected) VoiceCyan else StudioBorder,
                    label = "chip_border"
                )

                Surface(
                    onClick = {
                        activeMode = mode
                        liveMic.setMode(mode)
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = chipBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, chipBorder),
                    modifier = Modifier.testTag("mode_chip_${mode.name}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(VoiceCyanGlow)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = mode.displayName,
                            color = if (isSelected) VoiceCyanGlow else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dual Waveform Oscilloscope
        DualWaveformOscilloscope(
            stats = dspStats,
            isNoiseZeroActive = isNoiseZeroActive
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 32-Band FFT Frequency Spectrum
        SpectrumAnalyzer32(bands = dspStats.spectrumBands)

        Spacer(modifier = Modifier.height(12.dp))

        // Decibel & Voice Isolation Gauge
        DecibelReductionGauge(
            stats = dspStats,
            isNoiseZeroActive = isNoiseZeroActive
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Monitor & Hardware Engine Info Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(StudioCardBg)
                .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Headphone Monitoring Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Headphones,
                            contentDescription = "Headphone Monitor",
                            tint = if (isMonitoring) VoiceCyanGlow else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Headphone Live Monitor",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isMonitoring) "Playing zero-noise voice output" else "Muted (prevents feedback)",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Switch(
                        checked = isMonitoring,
                        onCheckedChange = { liveMic.setMonitoringEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = VoiceCyanGlow,
                            checkedTrackColor = VoiceCyan.copy(alpha = 0.4f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = StudioCardElevated
                        ),
                        modifier = Modifier.testTag("monitor_switch")
                    )
                }

                // Hardware DSP Suppressor info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "DSP Mode",
                            tint = if (hardwareActive) ZeroNoiseGreenGlow else VoiceCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (hardwareActive) "Hardware NoiseSuppressor Active" else "Turbo Software DSP (FFT Subtraction)",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Record Clean Audio Button
        if (!isRecording) {
            Button(
                onClick = {
                    val file = liveMic.startRecording()
                    if (file == null) {
                        viewModel.showMessage("Could not initialize recording.")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("record_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioCardElevated
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(RawNoiseRed)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "RECORD CLEAN AUDIO (WAV)",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        } else {
            // Recording Active Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(RawNoiseRed.copy(alpha = 0.15f))
                    .border(1.dp, RawNoiseRed, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FiberManualRecord,
                        contentDescription = "Recording indicator",
                        tint = RawNoiseRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RECORDING: ${String.format("%02d:%02d", recordSec / 60, recordSec % 60)}",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Button(
                    onClick = {
                        val file = liveMic.stopRecording()
                        if (file != null) {
                            viewModel.saveRecordingToVault(file, recordSec, activeMode.displayName)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RawNoiseRed),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("stop_record_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SAVE CLEAN",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
