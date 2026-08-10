package com.stark.jarvis.ui.immersion

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.stark.jarvis.ui.home.AppInfo
import com.stark.jarvis.ui.home.HexagonShape
import com.stark.jarvis.ui.hud.HudSound
import com.stark.jarvis.ui.theme.JarvisColors
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * A high-density, "living" J.A.R.V.I.S. HUD. Complexity is pushed to the top and
 * the two side columns so a minimal launcher (Niagara) stays readable down the
 * vertical centre.
 *
 * Layers (back to front): rotating city-wireframe + grid matrix → live telemetry
 * graphs in the side voids → scrolling diagnostic logs → the six hexagonal app
 * tiles with per-tile micro-animations → the breathing arc-reactor cluster with
 * orbiting telemetry → random spark/glitch flashes.
 */
@Composable
fun JarvisImmersionOverlay(
    apps: List<AppInfo>,
    onLaunch: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
    onSound: (HudSound) -> Unit = {},
) {
    val metrics = rememberSystemMetrics()

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind { drawRect(JarvisColors.VoidBlack) },
    ) {
        BackgroundMatrix(Modifier.fillMaxSize())

        // Live telemetry graphs drifting in the side voids.
        LiveGraph(
            metrics.netHistory, JarvisColors.ElectricCyan,
            Modifier
                .align(Alignment.CenterStart)
                .padding(start = 6.dp)
                .size(120.dp, 54.dp),
        )
        LiveGraph(
            metrics.battHistory, JarvisColors.StarkAmber,
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 6.dp)
                .size(120.dp, 54.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 6.dp, vertical = 8.dp),
        ) {
            // ---- Top: reactor cluster ----
            ReactorCluster(
                metrics = metrics,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
            )

            Spacer(Modifier.height(8.dp))

            // ---- Middle: left + right tile columns, centre left for Niagara ----
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                TileColumn(
                    apps = apps,
                    range = 0..2,
                    onLaunch = onLaunch,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(96.dp),
                )
                Spacer(Modifier.weight(1f)) // Niagara's zone
                TileColumn(
                    apps = apps,
                    range = 3..5,
                    onLaunch = onLaunch,
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(96.dp),
                )
            }

            // ---- Bottom: dual diagnostic log streams ----
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LogStream(Modifier.weight(1f).fillMaxHeight(), seed = 1)
                LogStream(Modifier.weight(1f).fillMaxHeight(), seed = 2)
            }
        }

        SparkOverlay(Modifier.fillMaxSize(), onSpark = { onSound(HudSound.SPARK) })
    }
}

/* ----------------------------------------------------------------------------
 * Background: grid + rotating wireframe "city" + sweep line.
 * ------------------------------------------------------------------------- */
@Composable
private fun BackgroundMatrix(modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "bg")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(60000, easing = LinearEasing)), label = "spin")
    val sweep by t.animateFloat(0f, 360f, infiniteRepeatable(tween(5200, easing = LinearEasing)), label = "sweep")
    val drift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "drift")

    Canvas(modifier) {
        val grid = JarvisColors.ElectricCyan.copy(alpha = 0.05f)
        val step = 46f
        val off = drift * step
        var x = -step + off
        while (x < size.width) {
            drawLine(grid, Offset(x, 0f), Offset(x, size.height), 1f); x += step
        }
        var y = -step + off
        while (y < size.height) {
            drawLine(grid, Offset(0f, y), Offset(size.width, y), 1f); y += step
        }

        val c = Offset(size.width / 2f, size.height * 0.28f)
        val r = min(size.width, size.height) * 0.34f
        rotate(spin, c) { drawCityWireframe(c, r) }

        // Radar sweep from the reactor centre.
        rotate(sweep, c) {
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(JarvisColors.ElectricCyan.copy(alpha = 0f), JarvisColors.ElectricCyan.copy(alpha = 0.22f)),
                    center = c,
                ),
                startAngle = 0f, sweepAngle = 40f, useCenter = true,
                topLeft = Offset(c.x - r, c.y - r), size = Size(r * 2, r * 2),
            )
        }
    }
}

private fun DrawScope.drawCityWireframe(c: Offset, r: Float) {
    val col = JarvisColors.ElectricCyan.copy(alpha = 0.12f)
    // Concentric polygons.
    for (ring in 1..3) {
        val rr = r * ring / 3f
        val p = Path()
        for (i in 0..8) {
            val a = Math.toRadians(i * 45.0)
            val x = c.x + rr * cos(a).toFloat()
            val y = c.y + rr * sin(a).toFloat()
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        drawPath(p, col, style = Stroke(1f))
    }
    // Radial spokes + "buildings".
    for (i in 0 until 24) {
        val a = Math.toRadians(i * 15.0)
        val ca = cos(a).toFloat(); val sa = sin(a).toFloat()
        drawLine(col, c, Offset(c.x + r * ca, c.y + r * sa), 1f)
        val bh = r * (0.12f + (i % 5) * 0.05f)
        val bx = c.x + (r * 0.7f) * ca; val by = c.y + (r * 0.7f) * sa
        drawRect(
            JarvisColors.ArcBlue.copy(alpha = 0.10f),
            topLeft = Offset(bx - 3f, by - bh / 2f), size = Size(6f, bh),
        )
    }
}

/* ----------------------------------------------------------------------------
 * Reactor cluster: breathing core + orbiting telemetry rings + live labels.
 * ------------------------------------------------------------------------- */
@Composable
private fun ReactorCluster(metrics: SystemMetrics, modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "reactor")
    val breathe by t.animateFloat(
        0.55f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse), label = "breathe",
    )
    val cw by t.animateFloat(0f, 360f, infiniteRepeatable(tween(11000, easing = LinearEasing)), label = "cw")
    val ccw by t.animateFloat(360f, 0f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "ccw")

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val r = min(size.width, size.height) / 2f

            drawCircle(JarvisColors.CyanFaint, radius = r * 0.95f, center = c, style = Stroke(1f))
            rotate(cw, c) { ringOfTicks(c, r * 0.86f, 48, JarvisColors.ElectricCyan.copy(alpha = 0.7f)) }
            rotate(ccw, c) { ringOfTicks(c, r * 0.66f, 24, JarvisColors.ArcBlue.copy(alpha = 0.7f)) }
            rotate(cw * 0.6f, c) {
                drawArc(
                    color = JarvisColors.StarkAmber,
                    startAngle = 0f, sweepAngle = 60f, useCenter = false,
                    topLeft = Offset(c.x - r * 0.86f, c.y - r * 0.86f),
                    size = Size(r * 1.72f, r * 1.72f), style = Stroke(r * 0.02f),
                )
            }

            // Breathing glow + triangular core.
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        JarvisColors.IceWhite.copy(alpha = breathe),
                        JarvisColors.ElectricCyan.copy(alpha = 0.6f * breathe),
                        JarvisColors.ElectricCyan.copy(alpha = 0f),
                    ),
                    center = c, radius = r * 0.4f,
                ),
                radius = r * 0.4f, center = c,
            )
            val core = Path()
            for (i in 0..3) {
                val a = Math.toRadians(90.0 + i * 120.0)
                val x = c.x + r * 0.2f * cos(a).toFloat()
                val y = c.y - r * 0.2f * sin(a).toFloat()
                if (i == 0) core.moveTo(x, y) else core.lineTo(x, y)
            }
            core.close()
            drawPath(core, JarvisColors.IceWhite.copy(alpha = breathe))
        }

        // Live telemetry labels around the reactor.
        TelemetryLabel("CPU  ${metrics.cpuTemp.toInt()}°C", Alignment.TopCenter)
        TelemetryLabel("MEM  ${metrics.memPercent}%", Alignment.CenterStart)
        TelemetryLabel("NET  ${metrics.netKbps.toInt()} KB/s", Alignment.CenterEnd)
        TelemetryLabel("PWR  ${metrics.batteryPct}%", Alignment.BottomCenter)
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.TelemetryLabel(text: String, align: Alignment) {
    Text(
        text = text,
        color = JarvisColors.ElectricCyan,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 1.sp,
        modifier = Modifier.align(align).padding(4.dp),
    )
}

private fun DrawScope.ringOfTicks(c: Offset, radius: Float, count: Int, color: Color) {
    for (i in 0 until count) {
        val a = Math.toRadians(i * 360.0 / count)
        val ca = cos(a).toFloat(); val sa = sin(a).toFloat()
        val inner = radius - (if (i % 4 == 0) radius * 0.08f else radius * 0.04f)
        drawLine(color, Offset(c.x + inner * ca, c.y + inner * sa), Offset(c.x + radius * ca, c.y + radius * sa), 2f)
    }
}

/* ----------------------------------------------------------------------------
 * Side tile columns with per-tile micro animations.
 * ------------------------------------------------------------------------- */
private enum class MicroAnim { SCROLL_TEXT, SIGNAL_BARS, WAVEFORM, PULSE_RING, HEX_FILL, SPARKLINE }

@Composable
private fun TileColumn(
    apps: List<AppInfo>,
    range: IntRange,
    onLaunch: (AppInfo) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        for (i in range) {
            val app = apps.getOrNull(i)
            HexTile(app = app, micro = MicroAnim.entries[i % MicroAnim.entries.size], onClick = { app?.let(onLaunch) })
        }
    }
}

@Composable
private fun HexTile(app: AppInfo?, micro: MicroAnim, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .border(1.5.dp, JarvisColors.ElectricCyan, HexagonShape)
                .clip(HexagonShape)
                .clickable { onClick() }
                .drawBehind { drawRect(JarvisColors.ElectricCyan.copy(alpha = 0.06f)) }
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (app != null) {
                Image(bitmap = app.icon, contentDescription = app.label, modifier = Modifier.fillMaxSize().clip(HexagonShape))
            }
        }
        MicroAnimation(micro, Modifier.width(72.dp).height(16.dp))
        Text(
            text = app?.label?.uppercase() ?: "MODULE",
            color = JarvisColors.IceWhite,
            fontFamily = FontFamily.Monospace,
            fontSize = 8.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun MicroAnimation(micro: MicroAnim, modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "micro")
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "phase")

    when (micro) {
        MicroAnim.SCROLL_TEXT -> {
            val msg = "…INCOMING // SYNC // ACK…INCOMING // SYNC // ACK"
            Box(modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))) {
                Text(
                    msg,
                    color = JarvisColors.ElectricCyan.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace, fontSize = 7.sp, maxLines = 1,
                    modifier = Modifier.graphicsLayer { translationX = -phase * 120f },
                )
            }
        }
        else -> Canvas(modifier) {
            when (micro) {
                MicroAnim.SIGNAL_BARS -> {
                    val bars = 6
                    val bw = size.width / (bars * 1.6f)
                    for (i in 0 until bars) {
                        val h = size.height * (0.3f + 0.7f * sinAbs(phase + i * 0.15f))
                        drawRect(JarvisColors.ElectricCyan, Offset(i * bw * 1.6f, size.height - h), Size(bw, h))
                    }
                }
                MicroAnim.WAVEFORM -> {
                    val p = Path()
                    val n = 24
                    for (i in 0..n) {
                        val x = size.width * i / n
                        val y = size.height / 2f + sin((phase * 6.28f) + i * 0.6f) * size.height * 0.4f
                        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                    }
                    drawPath(p, JarvisColors.ElectricCyan, style = Stroke(1.5f))
                }
                MicroAnim.PULSE_RING -> {
                    val rr = size.height / 2f * (0.4f + 0.6f * sinAbs(phase))
                    drawCircle(JarvisColors.StarkAmber.copy(alpha = 1f - sinAbs(phase)), radius = rr, center = Offset(size.width / 2f, size.height / 2f), style = Stroke(1.5f))
                }
                MicroAnim.HEX_FILL -> {
                    val w = size.width * sinAbs(phase)
                    drawRect(JarvisColors.ElectricCyan.copy(alpha = 0.6f), Offset(0f, size.height * 0.3f), Size(w, size.height * 0.4f))
                }
                MicroAnim.SPARKLINE -> {
                    val p = Path()
                    val n = 16
                    for (i in 0..n) {
                        val x = size.width * i / n
                        val y = size.height * (0.5f + (Random(i + (phase * 10).toInt()).nextFloat() - 0.5f) * 0.8f)
                        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
                    }
                    drawPath(p, JarvisColors.ArcBlue, style = Stroke(1f))
                }
                else -> {}
            }
        }
    }
}

private fun sinAbs(x: Float): Float = (sin(x * 6.2831855f) * 0.5f + 0.5f)

/* ----------------------------------------------------------------------------
 * Live telemetry graph.
 * ------------------------------------------------------------------------- */
@Composable
private fun LiveGraph(history: List<Float>, color: Color, modifier: Modifier) {
    Canvas(modifier.drawBehind { drawRect(JarvisColors.PanelBlack.copy(alpha = 0.35f)) }) {
        if (history.size < 2) return@Canvas
        val stepX = size.width / (history.size - 1)
        val fill = Path()
        val line = Path()
        history.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height - v.coerceIn(0f, 1f) * size.height
            if (i == 0) { line.moveTo(x, y); fill.moveTo(x, size.height); fill.lineTo(x, y) }
            else { line.lineTo(x, y); fill.lineTo(x, y) }
        }
        fill.lineTo(size.width, size.height)
        fill.close()
        drawPath(fill, brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.3f), color.copy(alpha = 0f))))
        drawPath(line, color, style = Stroke(1.5f))
    }
}

/* ----------------------------------------------------------------------------
 * Scrolling diagnostic log stream.
 * ------------------------------------------------------------------------- */
private val LOG_POOL = listOf(
    "CALIBRATING MARK 85", "NEURAL LINK ESTABLISHED", "REFRESHING TELEMETRY",
    "ROUTING ARC POWER", "SCAN VECTOR LOCKED", "REPULSOR CHARGE 100%",
    "SATELLITE UPLINK OK", "DECRYPTING PACKET", "THERMAL NOMINAL",
    "GYRO STABILIZED", "COMMS HANDSHAKE", "BIOMETRIC MATCH",
)

@Composable
private fun LogStream(modifier: Modifier, seed: Int) {
    val lines = remember { mutableStateListOf<String>() }
    LaunchedEffect(seed) {
        val rnd = Random(seed * 977 + 13)
        while (true) {
            val tag = LOG_POOL[rnd.nextInt(LOG_POOL.size)]
            val hex = (rnd.nextInt(0xFFFF)).toString(16).uppercase().padStart(4, '0')
            lines.add("0x$hex  $tag…")
            while (lines.size > 8) lines.removeAt(0)
            delay(140L)
        }
    }
    Column(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
            .drawBehind { drawRect(JarvisColors.PanelBlack.copy(alpha = 0.4f)) }
            .border(1.dp, JarvisColors.CyanFaint, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.Bottom,
    ) {
        lines.forEachIndexed { i, l ->
            Text(
                l,
                color = JarvisColors.ElectricCyan.copy(alpha = 0.35f + 0.08f * i),
                fontFamily = FontFamily.Monospace,
                fontSize = 7.sp,
                maxLines = 1,
            )
        }
    }
}

/* ----------------------------------------------------------------------------
 * Random spark / glitch flashes over the HUD.
 * ------------------------------------------------------------------------- */
@Composable
private fun SparkOverlay(modifier: Modifier, onSpark: () -> Unit) {
    var sparks by remember { mutableStateOf(listOf<Spark>()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(500, 1600))
            sparks = List(Random.nextInt(1, 4)) {
                Spark(Random.nextFloat(), Random.nextFloat(), Random.nextFloat() * 40f + 10f)
            }
            onSpark()
            delay(90L)
            sparks = emptyList()
        }
    }
    Canvas(modifier) {
        sparks.forEach { s ->
            val x = s.x * size.width
            val y = s.y * size.height
            drawLine(JarvisColors.IceWhite, Offset(x, y), Offset(x + s.len, y), 2f)
            drawLine(JarvisColors.ElectricCyan, Offset(x, y - 3f), Offset(x + s.len * 0.6f, y - 3f), 1f)
        }
    }
}

private data class Spark(val x: Float, val y: Float, val len: Float)
