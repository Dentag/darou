package dev.dentag.darou.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Palette.LightPrimary,
    onPrimary = Palette.LightOnPrimary,
    background = Palette.LightBackground,
    surface = Palette.LightBackground,
)

private val DarkColors = darkColorScheme(
    primary = Palette.DarkPrimary,
    onPrimary = Palette.DarkOnPrimary,
    background = Palette.DarkBackground,
    surface = Palette.DarkBackground,
)

@Composable
fun DarouTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
