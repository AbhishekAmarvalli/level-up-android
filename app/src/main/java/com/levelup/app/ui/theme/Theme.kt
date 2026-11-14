package com.levelup.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val Colors = darkColorScheme(
    primary = C.Red,
    onPrimary = C.Text,
    background = C.Bg,
    onBackground = C.Text,
    surface = C.Surface,
    onSurface = C.Text,
    surfaceVariant = C.Surface,
    onSurfaceVariant = C.Muted,
    outline = C.Border,
    error = C.Red,
)

/** MaterialTheme is used only as the app wrapper; components never read its values. */
@Composable
fun LevelUpTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
