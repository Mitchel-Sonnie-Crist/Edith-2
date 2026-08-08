package com.stark.jarvis.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Monospace-forward typography. Telemetry text reads like a system console, so we
 * lean on [FontFamily.Monospace] with wide letter spacing.
 *
 * Drop a custom techno font into res/font and swap [FontFamily.Monospace] for it
 * if you want the full Stark look (e.g. a "Nasalization" / "Orbitron"-style face).
 */
val JarvisTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        letterSpacing = 2.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        letterSpacing = 3.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        letterSpacing = 4.sp,
    ),
)
