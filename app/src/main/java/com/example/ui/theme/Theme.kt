package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = MehfilSecondary,
    onPrimary = MehfilYellowCream,
    primaryContainer = MehfilSecondary,
    onPrimaryContainer = MehfilYellowCream,
    secondary = MehfilSecondary,
    onSecondary = MehfilYellowCream,
    secondaryContainer = MehfilInputField,
    onSecondaryContainer = MehfilYellowCream,
    tertiary = MehfilCreamGold,
    onTertiary = MehfilSecondary,
    background = MehfilMainBackground,
    onBackground = MehfilYellowCream,
    surface = MehfilSecondary,
    onSurface = MehfilYellowCream,
    surfaceVariant = MehfilSecondary,
    onSurfaceVariant = MehfilYellowCream,
    outline = MehfilMutedGold,
    outlineVariant = MehfilMutedGold,
    error = MehfilSecondary,
    onError = MehfilYellowCream
)

private val LightColorScheme = DarkColorScheme

@Composable
fun MehfilPlayTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
