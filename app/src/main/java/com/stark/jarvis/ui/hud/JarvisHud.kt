package com.stark.jarvis.ui.hud

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stark.jarvis.R
import com.stark.jarvis.ui.theme.JarvisColors
import com.stark.jarvis.ui.theme.JarvisTheme
import kotlin.math.floor

/**
 * The complete J.A.R.V.I.S. boot HUD.
 *
 * Self-contained: mount it full-screen (in an Activity or a WindowManager overlay)
 * and it plays a single ~[durationMillis] sequence — fade-in, diagnostic sweep,
 * telemetry print-out, "SYSTEMS ONLINE" flash, crisp fade-out — then calls
 * [onFinished]. Sound beats are emitted through [onSound].
 *
 * The visual is anchored to [alignment] (default center-right) so Niagara
 * Launcher's left-aligned app list stays completely unobstructed.
 */
@Composable
fun JarvisHud(
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.CenterEnd,
    durationMillis: Int = 3600,
    lines: List<String> = defaultTelemetry(),
    onSound: (HudSound) -> Unit = {},
    onFinished: () -> Unit = {},
) {
    // Keep the latest callbacks without restarting the animation.
    val currentOnSound by rememberUpdatedState(onSound)
    val currentOnFinished by rememberUpdatedState(onFinished)

    val master = remember { Animatable(0f) }

    // Drive the master timeline exactly once.
    LaunchedEffect(Unit) {
        currentOnSound(HudSound.POWER_UP)
        master.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis, easing = LinearEasing),
        )
        currentOnFinished()
    }

    // Emit discrete sound beats as the timeline crosses thresholds, without
    // coupling side effects to recomposition.
    LaunchedEffect(lines) {
        var ringChimed = false
        var revealed = 0
        var onlineFired = false
        snapshotFlow { master.value }.collect { m ->
            val content = contentProgress(m)
            if (!ringChimed && content > 0.02f) {
                ringChimed = true
                currentOnSound(HudSound.RING_CHIME)
            }
            val nowRevealed = floor(content * lines.size).toInt().coerceIn(0, lines.size)
            while (revealed < nowRevealed) {
                revealed++
                val isLast = revealed == lines.size
                if (isLast && !onlineFired) {
                    onlineFired = true
                    currentOnSound(HudSound.ONLINE)
                } else if (!isLast) {
                    currentOnSound(HudSound.TELEMETRY_BEEP)
                }
            }
        }
    }

    val m = master.value
    val alpha = hudAlpha(m)
    val content = contentProgress(m)
    val intensity = (m / FADE_IN).coerceIn(0f, 1f)

    val lineFloat = content * lines.size
    val fullyRevealed = floor(lineFloat).toInt().coerceIn(0, lines.size)
    val currentLineProgress = (lineFloat - fullyRevealed).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            // Radial scrim behind the HUD only — darkens the right side for
            // contrast while leaving the far left (Niagara list) barely touched.
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            JarvisColors.VoidBlack.copy(alpha = 0.55f),
                            JarvisColors.VoidBlack.copy(alpha = 0f),
                        ),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.72f, size.height / 2f),
                        radius = size.width * 0.6f,
                    ),
                )
            },
        contentAlignment = alignment,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                DiagnosticRings(
                    progress = content,
                    modifier = Modifier.size(280.dp),
                )
                ArcReactor(
                    intensity = intensity,
                    modifier = Modifier.size(150.dp),
                )
            }

            TelemetryText(
                lines = lines,
                fullyRevealed = fullyRevealed,
                currentLineProgress = currentLineProgress,
                modifier = Modifier.widthIn(max = 320.dp),
            )
        }
    }
}

/** Default Stark-diagnostic telemetry, sourced from string resources. */
@Composable
private fun defaultTelemetry(): List<String> = listOf(
    stringResource(R.string.telemetry_1),
    stringResource(R.string.telemetry_2),
    stringResource(R.string.telemetry_3),
    stringResource(R.string.telemetry_4),
    stringResource(R.string.telemetry_5),
)

// ---- Timeline shape (fractions of the master 0..1) ----
private const val FADE_IN = 0.12f
private const val CONTENT_END = 0.86f
private const val FADE_OUT_START = 0.90f

private fun contentProgress(m: Float): Float =
    ((m - FADE_IN) / (CONTENT_END - FADE_IN)).coerceIn(0f, 1f)

private fun hudAlpha(m: Float): Float = when {
    m < FADE_IN -> m / FADE_IN
    m > FADE_OUT_START -> 1f - ((m - FADE_OUT_START) / (1f - FADE_OUT_START))
    else -> 1f
}

@Preview(showBackground = true, backgroundColor = 0xFF05080D, widthDp = 400, heightDp = 800)
@Composable
private fun JarvisHudPreview() {
    JarvisTheme {
        JarvisHud(durationMillis = 3600)
    }
}
