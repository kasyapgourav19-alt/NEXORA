package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NexoraColorScheme = darkColorScheme(
    primary = NexoraElectricCyan,
    onPrimary = NexoraAmoledBlack,
    primaryContainer = NexoraSecondaryPanel,
    onPrimaryContainer = NexoraSoftWhite,
    secondary = NexoraIceBlue,
    onSecondary = NexoraAmoledBlack,
    secondaryContainer = NexoraPrimaryPanel,
    onSecondaryContainer = NexoraSoftWhite,
    tertiary = NexoraVioletAccent,
    onTertiary = NexoraSoftWhite,
    background = NexoraAmoledBlack,
    onBackground = NexoraSoftWhite,
    surface = NexoraPrimaryPanel,
    onSurface = NexoraSoftWhite,
    surfaceVariant = NexoraSecondaryPanel,
    onSurfaceVariant = NexoraMutedBlueGray,
    outline = NexoraElectricCyanBorder,
    error = NexoraCoralError,
    onError = NexoraSoftWhite
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NexoraColorScheme,
        typography = Typography,
        content = content
    )
}
