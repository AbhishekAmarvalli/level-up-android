package com.levelup.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.levelup.app.R

val LvBg = Color(0xFF0F1115)
val LvSurface = Color(0xFF171A21)
val LvBorder = Color(0xFF2A2F3A)
val LvBorderStrong = Color(0xFF3A3F4B)
val LvText = Color(0xFFE8EAED)
val LvMuted = Color(0xFF9AA0A6)
val LvAccent = Color(0xFFFF3B3B)
val LvBarInactive = Color(0xFF5F6673)

/** JetBrains Mono is bundled, with the platform monospace as a fallback family. */
val Mono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
)

private val LevelUpColors = darkColorScheme(
    primary = LvText,
    onPrimary = LvBg,
    secondary = LvMuted,
    onSecondary = LvBg,
    background = LvBg,
    onBackground = LvText,
    surface = LvSurface,
    onSurface = LvText,
    surfaceVariant = LvSurface,
    onSurfaceVariant = LvMuted,
    outline = LvBorder,
    error = LvAccent,
    onError = LvText,
)

@Composable
fun LevelUpTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LevelUpColors, content = content)
}
