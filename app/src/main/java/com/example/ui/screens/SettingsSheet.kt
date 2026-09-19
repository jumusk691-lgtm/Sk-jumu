package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val liveMic = viewModel.liveMicController

    var gateThreshold by remember { mutableFloatStateOf(liveMic.dspEngine.gateThresholdDb) }
    var noiseDepth by remember { mutableFloatStateOf(liveMic.dspEngine.noiseSuppressionDepthDb) }
    var voiceSafety by remember { mutableFloatStateOf(liveMic.dspEngine.voiceFormantSafety * 100f) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = StudioCardBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(StudioBorder)
            )
        },
        modifier = Modifier.testTag("settings_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = VoiceCyanGlow,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ACOUSTIC DSP TUNING",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Button(
                    onClick = {
                        gateThreshold = -45f
                        noiseDepth = 48f
                        voiceSafety = 85f
                        liveMic.dspEngine.gateThresholdDb = -45f
                        liveMic.dspEngine.noiseSuppressionDepthDb = 48f
                        liveMic.dspEngine.voiceFormantSafety = 0.85f
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioCardElevated),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "RESET",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Zero-Noise Gate Threshold
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "ZERO-NOISE GATE THRESHOLD",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${"%.0f".format(gateThreshold)} dB",
                        color = ZeroNoiseGreenGlow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = gateThreshold,
                    onValueChange = {
                        gateThreshold = it
                        liveMic.dspEngine.gateThresholdDb = it
                    },
                    valueRange = -65f..-20f,
                    colors = SliderDefaults.colors(
                        thumbColor = ZeroNoiseGreenGlow,
                        activeTrackColor = ZeroNoiseGreen,
                        inactiveTrackColor = StudioCardElevated
                    )
                )
                Text(
                    text = "Signals below this decibel level will be instantly zeroed to dead silence.",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            // Noise Subtraction Depth
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "NOISE SUBTRACTION DEPTH",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "-${"%.0f".format(noiseDepth)} dB",
                        color = VoiceCyanGlow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = noiseDepth,
                    onValueChange = {
                        noiseDepth = it
                        liveMic.dspEngine.noiseSuppressionDepthDb = it
                    },
                    valueRange = 20f..60f,
                    colors = SliderDefaults.colors(
                        thumbColor = VoiceCyanGlow,
                        activeTrackColor = VoiceCyan,
                        inactiveTrackColor = StudioCardElevated
                    )
                )
                Text(
                    text = "Intensity of continuous spectral subtraction applied to stationary background noise.",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            // Voice Safety Protection
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SAFELY VOICE FORMANT PROTECTION",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${"%.0f".format(voiceSafety)}%",
                        color = ZeroNoiseGreenGlow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = voiceSafety,
                    onValueChange = {
                        voiceSafety = it
                        liveMic.dspEngine.voiceFormantSafety = it / 100f
                    },
                    valueRange = 40f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = ZeroNoiseGreenGlow,
                        activeTrackColor = ZeroNoiseGreen,
                        inactiveTrackColor = StudioCardElevated
                    )
                )
                Text(
                    text = "Protects human vocal harmonics (85Hz-3400Hz) from clipping or watery artifacts.",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
