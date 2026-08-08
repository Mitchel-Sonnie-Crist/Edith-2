package com.stark.jarvis.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Stark Industries / J.A.R.V.I.S. palette.
 *
 * Everything is built on a near-black base so the HUD reads as glowing neon over
 * whatever is behind it, and so it sits cleanly on top of Niagara's dark theme.
 */
object JarvisColors {
    // Base / background
    val VoidBlack = Color(0xFF05080D)
    val PanelBlack = Color(0xFF0A0F16)

    // Primary neon accents
    val ElectricCyan = Color(0xFF00E5FF)
    val ArcBlue = Color(0xFF1E88E5)
    val IceWhite = Color(0xFFB3F5FF)

    // Secondary / warning accents (Stark HUD amber)
    val StarkAmber = Color(0xFFFFB300)
    val AlertRed = Color(0xFFFF3D5A)

    // Dimmed variants for rings / gridlines
    val CyanDim = Color(0x5900E5FF)
    val CyanFaint = Color(0x2600E5FF)
    val BlueDim = Color(0x401E88E5)
}
