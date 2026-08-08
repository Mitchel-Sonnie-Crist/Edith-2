package com.stark.jarvis.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stark.jarvis.ui.theme.JarvisColors
import kotlin.math.min

/** A pointy-top/bottom hexagon. */
val HexagonShape: GenericShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(w * 0.5f, 0f)
    lineTo(w, h * 0.25f)
    lineTo(w, h * 0.75f)
    lineTo(w * 0.5f, h)
    lineTo(0f, h * 0.75f)
    lineTo(0f, h * 0.25f)
    close()
}

/** A launchable app rendered as a hexagon module with label + subtitle. */
@Composable
fun HexAppRow(
    app: AppInfo,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .border(1.5.dp, JarvisColors.ElectricCyan, HexagonShape)
                .padding(7.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = app.icon,
                contentDescription = app.label,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(HexagonShape),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = app.label.uppercase(),
                color = JarvisColors.IceWhite,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 1.sp,
                maxLines = 1,
            )
            Text(
                text = subtitle,
                color = JarvisColors.ElectricCyan.copy(alpha = 0.75f),
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 2.sp,
                maxLines = 1,
            )
        }
    }
}

/** A right-side stat panel: hex glyph + title + big value + sub-line. */
@Composable
fun StatCard(
    glyph: String,
    accent: Color,
    title: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .border(1.5.dp, accent, HexagonShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(glyph, fontSize = 18.sp, color = accent)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                title,
                color = accent,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
            )
            Text(
                value,
                color = JarvisColors.IceWhite,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            )
            Text(
                sub,
                color = JarvisColors.ElectricCyan.copy(alpha = 0.7f),
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
            )
        }
    }
}

/** A bottom command button. */
@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, JarvisColors.CyanDim, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = JarvisColors.ElectricCyan,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
            maxLines = 2,
        )
    }
}

/** A little rotating atom / orbital diagram, like the reference's nav readout. */
@Composable
fun HudOrbital(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "orbital")
    val spin by t.animateFloat(
        0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "spin",
    )
    Canvas(modifier = modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val rr = min(size.width, size.height) / 2f
        // Three orbits at different tilts.
        for (i in 0 until 3) {
            rotate(60f * i + spin * (if (i % 2 == 0) 1f else -1f), c) {
                drawOval(
                    color = JarvisColors.ElectricCyan.copy(alpha = 0.5f),
                    topLeft = Offset(c.x - rr * 0.9f, c.y - rr * 0.32f),
                    size = androidx.compose.ui.geometry.Size(rr * 1.8f, rr * 0.64f),
                    style = Stroke(1.5f),
                )
                // Electron.
                drawCircle(
                    JarvisColors.IceWhite,
                    radius = rr * 0.06f,
                    center = Offset(c.x + rr * 0.9f, c.y),
                )
            }
        }
        drawCircle(JarvisColors.StarkAmber, radius = rr * 0.12f, center = c)
    }
}

/** A faint wireframe "map trace" panel. */
@Composable
fun HudWireframe(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val faint = JarvisColors.ElectricCyan.copy(alpha = 0.18f)
        val step = size.width / 8f
        var x = 0f
        while (x <= size.width) {
            drawLine(faint, Offset(x, 0f), Offset(x, size.height), 1f)
            x += step
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(faint, Offset(0f, y), Offset(size.width, y), 1f)
            y += step
        }
        // A couple of "route" traces + nodes.
        val bright = JarvisColors.ElectricCyan.copy(alpha = 0.6f)
        drawLine(bright, Offset(size.width * 0.1f, size.height * 0.8f), Offset(size.width * 0.5f, size.height * 0.3f), 1.5f)
        drawLine(bright, Offset(size.width * 0.5f, size.height * 0.3f), Offset(size.width * 0.85f, size.height * 0.55f), 1.5f)
        drawCircle(JarvisColors.StarkAmber, radius = 4f, center = Offset(size.width * 0.5f, size.height * 0.3f))
        drawCircle(bright, radius = 3f, center = Offset(size.width * 0.85f, size.height * 0.55f))
        // Concentric radar rings bottom-left.
        val rc = Offset(size.width * 0.25f, size.height * 0.65f)
        for (k in 1..3) {
            drawCircle(faint, radius = size.width * 0.08f * k, center = rc, style = Stroke(1f))
        }
    }
}
