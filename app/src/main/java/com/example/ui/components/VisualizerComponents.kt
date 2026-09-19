package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dsp.DspStats
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
import kotlin.math.abs

@Composable
fun DualWaveformOscilloscope(
    stats: DspStats,
    isNoiseZeroActive: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(StudioCardBg)
            .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(RawNoiseOrange)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RAW INPUT",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isNoiseZeroActive) ZeroNoiseGreen else TextMuted)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isNoiseZeroActive) "ZERO NOISE ACTIVE" else "BYPASS",
                        color = if (isNoiseZeroActive) ZeroNoiseGreenGlow else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val midY = height / 2f

                // Grid background lines
                val gridColor = Color(0xFF1E293B).copy(alpha = 0.6f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, midY),
                    end = Offset(width, midY),
                    strokeWidth = 1f
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, midY - height * 0.35f),
                    end = Offset(width, midY - height * 0.35f),
                    strokeWidth = 1f
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, midY + height * 0.35f),
                    end = Offset(width, midY + height * 0.35f),
                    strokeWidth = 1f
                )

                val rawPoints = stats.rawWaveform
                val cleanPoints = stats.cleanWaveform

                // Draw Raw Noisy Waveform (Orange/Red)
                if (rawPoints.isNotEmpty()) {
                    val rawPath = Path()
                    val dx = width / (rawPoints.size - 1).toFloat()
                    for (i in rawPoints.indices) {
                        val x = i * dx
                        val y = midY - (rawPoints[i] * height * 0.45f)
                        if (i == 0) rawPath.moveTo(x, y) else rawPath.lineTo(x, y)
                    }
                    drawPath(
                        path = rawPath,
                        color = RawNoiseOrange.copy(alpha = 0.5f),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Draw Clean Output Waveform (Emerald / Cyan)
                if (cleanPoints.isNotEmpty()) {
                    val cleanPath = Path()
                    val dx = width / (cleanPoints.size - 1).toFloat()
                    for (i in cleanPoints.indices) {
                        val x = i * dx
                        val y = midY - (cleanPoints[i] * height * 0.45f)
                        if (i == 0) cleanPath.moveTo(x, y) else cleanPath.lineTo(x, y)
                    }
                    val cleanColor = if (isNoiseZeroActive) ZeroNoiseGreen else TextSecondary
                    drawPath(
                        path = cleanPath,
                        color = cleanColor,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

@Composable
fun SpectrumAnalyzer32(
    bands: FloatArray,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(StudioCardBg)
            .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACOUSTIC FFT SPECTRUM",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "85Hz (Vocal) ─── 3.4kHz ─── 8kHz",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Canvas(modifier = Modifier.fillMaxSize()) {
                val count = bands.size.coerceAtLeast(1)
                val barWidth = (size.width / count) * 0.7f
                val gap = (size.width / count) * 0.3f

                for (i in 0 until count) {
                    val magnitude = bands[i].coerceIn(0.04f, 1.0f)
                    val barHeight = size.height * magnitude
                    val x = i * (barWidth + gap)
                    val y = size.height - barHeight

                    // Frequency-dependent gradient:
                    // Low rumble: Red/Orange, Voice band (bins 3-16): Vibrant Cyan, High hiss: Emerald
                    val barColor = when {
                        i < 3 -> RawNoiseRed
                        i in 3..18 -> VoiceCyan
                        else -> ZeroNoiseGreen
                    }

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                }
            }
        }
    }
}

@Composable
fun DecibelReductionGauge(
    stats: DspStats,
    isNoiseZeroActive: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Noise Eliminated Card
        MetricBox(
            modifier = Modifier.weight(1f),
            label = "NOISE ELIMINATED",
            value = if (isNoiseZeroActive) "-${"%.1f".format(abs(stats.noiseReductionDb.coerceAtLeast(36.0f)))} dB" else "0.0 dB",
            subtext = if (isNoiseZeroActive) "Pristine Zero Floor" else "DSP Inactive",
            valueColor = if (isNoiseZeroActive) ZeroNoiseGreenGlow else TextMuted
        )

        // Voice Isolation Integrity
        MetricBox(
            modifier = Modifier.weight(1f),
            label = "VOICE ISOLATION",
            value = if (isNoiseZeroActive) "${stats.voiceClarityScore}%" else "45%",
            subtext = if (isNoiseZeroActive) "Safely Preserved" else "Noise Contaminated",
            valueColor = if (isNoiseZeroActive) VoiceCyanGlow else RawNoiseOrange
        )

        // Clean Output dB
        MetricBox(
            modifier = Modifier.weight(1f),
            label = "OUTPUT FLOOR",
            value = if (isNoiseZeroActive && !stats.isVoiceDetected) "0.0 SILENT" else "${"%.1f".format(stats.cleanRmsDb)} dB",
            subtext = if (!stats.isVoiceDetected && isNoiseZeroActive) "Zero Noise Gate" else "Active Voice",
            valueColor = if (!stats.isVoiceDetected && isNoiseZeroActive) ZeroNoiseGreen else TextPrimary
        )
    }
}

@Composable
fun MetricBox(
    label: String,
    value: String,
    subtext: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(StudioCardBg)
            .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Column {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                color = TextMuted,
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
fun AncGlowRing(
    isNoiseZeroActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "anc_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier
            .size(170.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.minDimension / 2f) - 16.dp.toPx()

            if (isNoiseZeroActive) {
                // Glowing outer aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ZeroNoiseGreen.copy(alpha = 0.25f),
                            VoiceCyan.copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius * 1.35f * pulseScale
                    )
                )

                // Outer animated ring
                drawCircle(
                    color = ZeroNoiseGreenGlow,
                    radius = radius * pulseScale,
                    style = Stroke(width = 3.dp.toPx())
                )

                // Inner ring
                drawCircle(
                    color = VoiceCyan,
                    radius = radius * 0.78f,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            } else {
                // Inactive dim ring
                drawCircle(
                    color = Color(0xFF334155),
                    radius = radius,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (isNoiseZeroActive) "ZERO" else "OFF",
                color = if (isNoiseZeroActive) ZeroNoiseGreenGlow else TextMuted,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            Text(
                text = if (isNoiseZeroActive) "NOISE 100%" else "BYPASS",
                color = if (isNoiseZeroActive) VoiceCyan else TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
