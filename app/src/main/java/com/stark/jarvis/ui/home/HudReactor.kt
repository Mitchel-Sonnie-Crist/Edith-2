package com.stark.jarvis.ui.home

import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.stark.jarvis.ui.theme.JarvisColors
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The central J.A.R.V.I.S. reactor HUD: concentric segmented rings, a rotating
 * amber accent arc, left/right chevrons, curved "JARVIS HUD: INITIALIZING…" text
 * top and bottom, a mid band, and the glowing triangular arc-reactor core.
 */
@Composable
fun HudReactor(
    modifier: Modifier = Modifier,
    topLabel: String = "JARVIS HUD: INITIALIZING…",
    midLabel: String = "COMMS // FLIGHT // POWER // BIO",
    bottomLabel: String = "JARVIS HUD: INITIALIZING…",
) {
    val t = rememberInfiniteTransition(label = "reactor")
    val cw by t.animateFloat(
        0f, 360f, infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "cw",
    )
    val ccw by t.animateFloat(
        360f, 0f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "ccw",
    )
    val accent by t.animateFloat(
        0f, 360f, infiniteRepeatable(tween(4200, easing = LinearEasing)), label = "accent",
    )
    val pulse by t.animateFloat(
        0.85f, 1f,
        infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val r = min(size.width, size.height) / 2f

        // Outer thin ring.
        drawCircle(JarvisColors.CyanDim, radius = r * 0.98f, center = center, style = Stroke(1.5f))

        // Segmented outer ring (rotating clockwise).
        rotate(cw, center) {
            drawSegmentedRing(center, r * 0.88f, 24, 0.4f, r * 0.02f, JarvisColors.ElectricCyan, 0.9f)
        }
        // Rotating amber accent arc over the segmented ring.
        drawArcStroke(center, r * 0.88f, accent, 70f, r * 0.03f, JarvisColors.StarkAmber)

        // Curved top + bottom labels.
        drawArcText(topLabel, center, r * 0.74f, 200f, 140f, JarvisColors.IceWhite, r * 0.075f)
        drawArcText(bottomLabel, center, r * 0.74f, 160f, -140f, JarvisColors.ElectricCyan, r * 0.07f)

        // Mid ring + straight mid band label.
        drawCircle(JarvisColors.CyanFaint, radius = r * 0.6f, center = center, style = Stroke(1f))
        drawArcText(midLabel, center, r * 0.48f, 200f, 140f, JarvisColors.ElectricCyan, r * 0.06f)

        // Inner ring (counter-rotating, segmented).
        rotate(ccw, center) {
            drawSegmentedRing(center, r * 0.4f, 8, 0.55f, r * 0.025f, JarvisColors.ArcBlue, 0.85f)
        }

        // Left/right chevrons.
        drawChevron(center + Offset(-r * 0.66f, 0f), r * 0.06f, pointLeft = true)
        drawChevron(center + Offset(r * 0.66f, 0f), r * 0.06f, pointLeft = false)

        // Glow behind the core.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    JarvisColors.ElectricCyan.copy(alpha = 0.45f),
                    JarvisColors.ElectricCyan.copy(alpha = 0f),
                ),
                center = center,
                radius = r * 0.34f,
            ),
            radius = r * 0.34f,
            center = center,
        )

        // Triangular arc-reactor core.
        drawReactorCore(center, r * 0.22f * pulse)
    }
}

private fun DrawScope.drawSegmentedRing(
    center: Offset, radius: Float, segments: Int, gap: Float,
    stroke: Float, color: androidx.compose.ui.graphics.Color, alpha: Float,
) {
    val per = 360f / segments
    val draw = per * (1f - gap)
    val topLeft = Offset(center.x - radius, center.y - radius)
    val arc = Size(radius * 2f, radius * 2f)
    for (i in 0 until segments) {
        drawArc(
            color = color.copy(alpha = alpha),
            startAngle = i * per,
            sweepAngle = draw,
            useCenter = false,
            topLeft = topLeft,
            size = arc,
            style = Stroke(stroke),
        )
    }
}

private fun DrawScope.drawArcStroke(
    center: Offset, radius: Float, start: Float, sweep: Float,
    stroke: Float, color: androidx.compose.ui.graphics.Color,
) {
    drawArc(
        color = color,
        startAngle = start,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(stroke),
    )
}

private fun DrawScope.drawChevron(at: Offset, size: Float, pointLeft: Boolean) {
    val dx = if (pointLeft) size else -size
    val path = Path().apply {
        moveTo(at.x + dx, at.y - size)
        lineTo(at.x - dx, at.y)
        lineTo(at.x + dx, at.y + size)
    }
    drawPath(path, JarvisColors.ElectricCyan, style = Stroke(2.5f))
}

private fun DrawScope.drawReactorCore(center: Offset, radius: Float) {
    // Outer housing triangle.
    trianglePath(center, radius).let { drawPath(it, JarvisColors.ElectricCyan, style = Stroke(radius * 0.12f)) }
    // Bright inner triangle.
    trianglePath(center, radius * 0.62f).let {
        drawPath(
            it,
            brush = Brush.radialGradient(
                colors = listOf(JarvisColors.IceWhite, JarvisColors.ElectricCyan),
                center = center,
                radius = radius,
            ),
        )
    }
}

private fun trianglePath(center: Offset, radius: Float): Path = Path().apply {
    for (i in 0..3) {
        val a = Math.toRadians(90.0 + i * 120.0)
        val x = center.x + radius * cos(a).toFloat()
        val y = center.y - radius * sin(a).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

/** Draw [text] along a circular arc using the native canvas' drawTextOnPath. */
private fun DrawScope.drawArcText(
    text: String, center: Offset, radius: Float,
    startAngle: Float, sweepAngle: Float,
    color: androidx.compose.ui.graphics.Color, textSizePx: Float,
) {
    val paint = Paint().apply {
        isAntiAlias = true
        this.color = color.toArgb()
        textSize = textSizePx
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        letterSpacing = 0.12f
        textAlign = Paint.Align.CENTER
    }
    val path = android.graphics.Path().apply {
        addArc(
            center.x - radius, center.y - radius,
            center.x + radius, center.y + radius,
            startAngle, sweepAngle,
        )
    }
    drawContext.canvas.nativeCanvas.drawTextOnPath(text, path, 0f, 0f, paint)
}
