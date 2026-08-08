package com.stark.jarvis.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * The app is deliberately dark-only — a light J.A.R.V.I.S. HUD would defeat the
 * purpose — but we still route through a proper [MaterialTheme] so standard
 * components (text, ripples) pick up the palette.
 */
private val JarvisColorScheme = darkColorScheme(
    primary = JarvisColors.ElectricCyan,
    onPrimary = JarvisColors.VoidBlack,
    secondary = JarvisColors.ArcBlue,
    onSecondary = JarvisColors.IceWhite,
    background = JarvisColors.VoidBlack,
    onBackground = JarvisColors.IceWhite,
    surface = JarvisColors.PanelBlack,
    onSurface = JarvisColors.IceWhite,
    error = JarvisColors.AlertRed,
)

@Composable
fun JarvisTheme(
    // Accepted for API symmetry; the scheme is dark regardless.
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = JarvisTypography,
        content = content,
    )
}
