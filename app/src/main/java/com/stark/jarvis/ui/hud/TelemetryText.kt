package com.stark.jarvis.ui.hud

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.stark.jarvis.ui.theme.JarvisColors

/**
 * Console-style telemetry readout.
 *
 * @param lines all telemetry strings for this run.
 * @param fullyRevealed how many lines are printed in full.
 * @param currentLineProgress 0..1 typewriter progress of the line at index
 *   [fullyRevealed] (the one currently "typing"). Ignored once every line is out.
 */
@Composable
fun TelemetryText(
    lines: List<String>,
    fullyRevealed: Int,
    currentLineProgress: Float,
    modifier: Modifier = Modifier,
) {
    val caret = rememberInfiniteTransition(label = "caret")
    val caretAlpha by caret.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(520, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "caret-alpha",
    )

    Column(modifier = modifier) {
        for (i in lines.indices) {
            when {
                i < fullyRevealed -> TelemetryLine(
                    text = lines[i],
                    isFinal = i == lines.lastIndex,
                    caretAlpha = 0f,
                    showCaret = false,
                )

                i == fullyRevealed -> {
                    val chars = (lines[i].length * currentLineProgress.coerceIn(0f, 1f)).toInt()
                    TelemetryLine(
                        text = lines[i].substring(0, chars),
                        isFinal = i == lines.lastIndex,
                        caretAlpha = caretAlpha,
                        showCaret = true,
                    )
                }
                // Lines beyond the current one are not drawn yet.
            }
        }
    }
}

@Composable
private fun TelemetryLine(
    text: String,
    isFinal: Boolean,
    caretAlpha: Float,
    showCaret: Boolean,
) {
    val color = if (isFinal) JarvisColors.IceWhite else JarvisColors.ElectricCyan
    Row {
        Text(
            text = if (isFinal) text else "> $text",
            color = color,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isFinal) FontWeight.Bold else FontWeight.Normal,
            letterSpacing = if (isFinal) 4.sp else 2.sp,
            fontSize = if (isFinal) 18.sp else 13.sp,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (showCaret) {
            Text(
                text = "▋",
                color = Color(color.red, color.green, color.blue, caretAlpha),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            )
        }
    }
}
