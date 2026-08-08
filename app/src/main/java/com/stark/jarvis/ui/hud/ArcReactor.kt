package com.stark.jarvis.ui.hud

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.stark.jarvis.ui.theme.JarvisColors
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A glowing Arc Reactor core.
 *
 * @param intensity 0..1 overall brightness/energy of the reactor, driven by the
 *   boot timeline so it "charges up" as the sequence plays.
 */
@Composable
fun ArcReactor(
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
) {
    val transition = rememberInfiniteTransition(label = "arc-reactor")

    // Slow counter-rotating rings.
    val cwRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "cw",
    )
    val ccwRotation by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "ccw",
    )

    // Core breathing pulse.
    val pulse by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = min(size.width, size.height) / 2f
        val energy = intensity.coerceIn(0f, 1f)

        // Outer bloom / glow halo.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    JarvisColors.ElectricCyan.copy(alpha = 0.35f * energy),
                    JarvisColors.ArcBlue.copy(alpha = 0.10f * energy),
                    JarvisColors.ElectricCyan.copy(alpha = 0f),
                ),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )

        // Rotating outer ring with gaps (segmented).
        rotate(cwRotation, pivot = center) {
            drawSegmentedRing(
                center = center,
                radius = radius * 0.72f,
                segments = 12,
                gapFraction = 0.35f,
                strokeWidth = radius * 0.02f,
                alpha = 0.9f * energy,
            )
        }

        // Rotating inner ring, opposite direction.
        rotate(ccwRotation, pivot = center) {
            drawSegmentedRing(
                center = center,
                radius = radius * 0.55f,
                segments = 6,
                gapFraction = 0.5f,
                strokeWidth = radius * 0.03f,
                alpha = 0.8f * energy,
            )
        }

        // Reactor housing triangle (the classic Iron Man core silhouette).
        rotate(cwRotation * 0.5f, pivot = center) {
            drawReactorTriangle(
                center = center,
                radius = radius * 0.42f,
                strokeWidth = radius * 0.02f,
                alpha = 0.9f * energy,
            )
        }

        // Bright pulsing core.
        val coreRadius = radius * 0.22f * pulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    JarvisColors.IceWhite.copy(alpha = energy),
                    JarvisColors.ElectricCyan.copy(alpha = 0.9f * energy),
                    JarvisColors.ArcBlue.copy(alpha = 0f),
                ),
                center = center,
                radius = coreRadius * 1.6f,
            ),
            radius = coreRadius * 1.6f,
            center = center,
        )
    }
}

/** Draws [segments] arc chunks around a circle, each separated by a gap. */
private fun DrawScope.drawSegmentedRing(
    center: Offset,
    radius: Float,
    segments: Int,
    gapFraction: Float,
    strokeWidth: Float,
    alpha: Float,
) {
    val sweepPer = 360f / segments
    val drawSweep = sweepPer * (1f - gapFraction)
    val topLeft = Offset(center.x - radius, center.y - radius)
    val arcSize = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f)

    for (i in 0 until segments) {
        val start = i * sweepPer
        drawArc(
            color = JarvisColors.ElectricCyan.copy(alpha = alpha),
            startAngle = start,
            sweepAngle = drawSweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth),
        )
    }
}

/** The three-pointed reactor core outline. */
private fun DrawScope.drawReactorTriangle(
    center: Offset,
    radius: Float,
    strokeWidth: Float,
    alpha: Float,
) {
    val path = androidx.compose.ui.graphics.Path()
    for (i in 0..3) {
        val angle = Math.toRadians((90.0 + i * 120.0))
        val x = center.x + radius * cos(angle).toFloat()
        val y = center.y - radius * sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(
        path = path,
        color = JarvisColors.IceWhite.copy(alpha = alpha),
        style = Stroke(width = strokeWidth),
    )
}
