package com.stark.jarvis.ui.hud

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.stark.jarvis.ui.theme.JarvisColors
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Concentric diagnostic rings surrounding the reactor. Each ring "sweeps in" as
 * [progress] advances (0..1), tick marks fill, and a scanner arc rotates
 * continuously to sell the "system check" feel.
 */
@Composable
fun DiagnosticRings(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "diag-rings")
    val scanner by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "scanner",
    )
    val slowSpin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing)),
        label = "slow-spin",
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxR = min(size.width, size.height) / 2f
        val p = progress.coerceIn(0f, 1f)

        // Ring 1 — outermost sweep arc, tracks progress directly.
        drawSweepRing(
            center = center,
            radius = maxR * 0.96f,
            sweep = 360f * p,
            strokeWidth = maxR * 0.012f,
            color = JarvisColors.ElectricCyan,
        )

        // Ring 2 — tick ring, ticks illuminate with progress.
        rotate(slowSpin, pivot = center) {
            drawTickRing(
                center = center,
                radius = maxR * 0.82f,
                ticks = 60,
                litFraction = p,
                length = maxR * 0.05f,
            )
        }

        // Ring 3 — dashed ring appears in the second half.
        val ring3 = ((p - 0.35f) / 0.65f).coerceIn(0f, 1f)
        drawSweepRing(
            center = center,
            radius = maxR * 0.68f,
            sweep = 360f * ring3,
            strokeWidth = maxR * 0.008f,
            color = JarvisColors.ArcBlue,
        )

        // Continuous scanner wedge for liveliness.
        drawScannerWedge(
            center = center,
            radius = maxR * 0.9f,
            angle = scanner,
            widthDeg = 26f,
        )

        // Faint crosshair gridlines (very subtle, keeps HUD grounded).
        drawCrosshair(center, maxR, alpha = 0.10f + 0.10f * p)
    }
}

private fun DrawScope.drawSweepRing(
    center: Offset,
    radius: Float,
    sweep: Float,
    strokeWidth: Float,
    color: androidx.compose.ui.graphics.Color,
) {
    val topLeft = Offset(center.x - radius, center.y - radius)
    val arcSize = Size(radius * 2f, radius * 2f)
    // Faint full track underneath.
    drawArc(
        color = color.copy(alpha = 0.12f),
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth),
    )
    // Bright progress arc.
    drawArc(
        color = color,
        startAngle = -90f,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth * 1.6f),
    )
}

private fun DrawScope.drawTickRing(
    center: Offset,
    radius: Float,
    ticks: Int,
    litFraction: Float,
    length: Float,
) {
    val lit = (ticks * litFraction).toInt()
    for (i in 0 until ticks) {
        val angle = Math.toRadians((i * 360.0 / ticks) - 90.0)
        val cosA = cos(angle).toFloat()
        val sinA = sin(angle).toFloat()
        val inner = radius - length
        val start = Offset(center.x + inner * cosA, center.y + inner * sinA)
        val end = Offset(center.x + radius * cosA, center.y + radius * sinA)
        val isMajor = i % 5 == 0
        val color = if (i < lit) JarvisColors.ElectricCyan else JarvisColors.CyanFaint
        drawLine(
            color = if (isMajor) color else color.copy(alpha = color.alpha * 0.6f),
            start = start,
            end = end,
            strokeWidth = if (isMajor) 3.5f else 1.8f,
        )
    }
}

private fun DrawScope.drawScannerWedge(
    center: Offset,
    radius: Float,
    angle: Float,
    widthDeg: Float,
) {
    val topLeft = Offset(center.x - radius, center.y - radius)
    val arcSize = Size(radius * 2f, radius * 2f)
    drawArc(
        brush = androidx.compose.ui.graphics.Brush.sweepGradient(
            colors = listOf(
                JarvisColors.ElectricCyan.copy(alpha = 0f),
                JarvisColors.ElectricCyan.copy(alpha = 0.45f),
            ),
            center = center,
        ),
        startAngle = angle,
        sweepAngle = widthDeg,
        useCenter = true,
        topLeft = topLeft,
        size = arcSize,
    )
}

private fun DrawScope.drawCrosshair(center: Offset, radius: Float, alpha: Float) {
    val c = JarvisColors.ElectricCyan.copy(alpha = alpha)
    drawLine(c, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), 1f)
    drawLine(c, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), 1f)
}
