package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = VoiceCyan,
    onPrimary = Color.Black,
    primaryContainer = StudioCardElevated,
    onPrimaryContainer = VoiceCyanGlow,
    secondary = ZeroNoiseGreen,
    onSecondary = Color.Black,
    secondaryContainer = StudioCardElevated,
    onSecondaryContainer = ZeroNoiseGreenGlow,
    tertiary = RawNoiseOrange,
    onTertiary = Color.Black,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = StudioCardBg,
    onSurface = TextPrimary,
    surfaceVariant = StudioCardElevated,
    onSurfaceVariant = TextSecondary,
    outline = StudioBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to studio dark
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

