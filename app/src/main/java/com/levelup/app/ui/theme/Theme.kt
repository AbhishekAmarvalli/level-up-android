package com.levelup.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/** No Material default tones are allowed to leak through. */
private val LevelUpColors = darkColorScheme(
    primary = LvAccent,
    onPrimary = LvBg,
    primaryContainer = LvSurface,
    onPrimaryContainer = LvText,
    secondary = LvMuted,
    onSecondary = LvBg,
    secondaryContainer = LvSurface,
    onSecondaryContainer = LvText,
    tertiary = LvText,
    onTertiary = LvBg,
    background = LvBg,
    onBackground = LvText,
    surface = LvSurface,
    onSurface = LvText,
    surfaceVariant = LvSurface,
    onSurfaceVariant = LvMuted,
    surfaceTint = Color.Transparent,
    outline = LvBorder,
    outlineVariant = LvBorder,
    error = LvAccent,
    onError = LvText,
)

private val LevelUpTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 56.sp),
    displayMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 30.sp),
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 28.sp),
    titleLarge = TextStyle(fontFamily = Mono, fontSize = 16.sp, fontWeight = FontWeight.Normal),
    titleMedium = TextStyle(fontFamily = Mono, fontSize = 14.sp, fontWeight = FontWeight.Normal),
    titleSmall = TextStyle(fontFamily = Mono, fontSize = 13.sp, fontWeight = FontWeight.Normal),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 13.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 11.sp),
)

private val LevelUpShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

@Composable
fun LevelUpTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LevelUpColors,
        typography = LevelUpTypography,
        shapes = LevelUpShapes,
        content = content,
    )
}
