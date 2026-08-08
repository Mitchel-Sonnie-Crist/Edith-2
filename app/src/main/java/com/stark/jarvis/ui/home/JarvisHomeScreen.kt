package com.stark.jarvis.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stark.jarvis.audio.JarvisVoice
import com.stark.jarvis.ui.hud.ArcReactor
import com.stark.jarvis.ui.theme.JarvisColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The J.A.R.V.I.S. home screen — a full launcher surface. When this app is set as
 * the device Home app, this replaces the desktop: a persistent HUD with a live
 * clock, real battery telemetry, an arc reactor, and the full app list.
 */
@Composable
fun JarvisHomeScreen(
    voice: JarvisVoice,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clock = rememberClock()
    val battery = rememberBattery()

    // Load the app list off the main thread.
    val apps by produceState(initialValue = emptyList<AppInfo>()) {
        value = withContext(Dispatchers.IO) { AppsRepository.loadLaunchableApps(context) }
    }

    // Greet once per launch.
    LaunchedEffect(Unit) {
        voice.speak("${greetingForHour(clock.hour)} All systems online.")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisColors.VoidBlack)
            .drawBehind { drawHudGrid() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            // ---- Header: clock + reactor ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = greetingForHour(clock.hour),
                        color = JarvisColors.IceWhite,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = clock.time,
                        color = JarvisColors.ElectricCyan,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 44.sp,
                        letterSpacing = 2.sp,
                    )
                    Text(
                        text = clock.date,
                        color = JarvisColors.ElectricCyan.copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        letterSpacing = 3.sp,
                    )
                }

                // Tap the reactor for a spoken status report.
                ArcReactor(
                    intensity = 1f,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .clickable { voice.speak(statusReport(battery, clock)) },
                )
            }

            Spacer(Modifier.height(10.dp))

            // ---- Telemetry strip ----
            Text(
                text = buildString {
                    append("PWR ")
                    append(battery.percent)
                    append("%  •  ")
                    append(if (battery.charging) "CHARGING" else "ON BATTERY")
                    append("  •  ")
                    append(if (apps.isEmpty()) "SCANNING…" else "${apps.size} MODULES")
                },
                color = JarvisColors.ElectricCyan.copy(alpha = 0.85f),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
            )

            Spacer(Modifier.height(10.dp))
            Divider()
            Spacer(Modifier.height(6.dp))

            Text(
                text = "APPLICATIONS",
                color = JarvisColors.ArcBlue,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 4.sp,
            )
            Spacer(Modifier.height(4.dp))

            // ---- App list (left-aligned, HUD styled) ----
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(apps, key = { it.packageName }) { app ->
                    AppRow(app = app, onClick = { AppsRepository.launch(context, app) })
                }
            }

            // ---- Footer: settings ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = "◧ J.A.R.V.I.S. SETTINGS",
                    color = JarvisColors.ElectricCyan.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onOpenSettings() }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun AppRow(app: AppInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = app.label,
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(9.dp)),
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = app.label,
            color = JarvisColors.IceWhite,
            fontFamily = FontFamily.Monospace,
            fontSize = 15.sp,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(JarvisColors.CyanDim),
    )
}

/** Faint HUD grid drawn behind the whole home screen. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHudGrid() {
    val step = 64.dp.toPx()
    val color = JarvisColors.ElectricCyan.copy(alpha = 0.05f)
    var x = 0f
    while (x < size.width) {
        drawLine(color, Offset(x, 0f), Offset(x, size.height), 1f)
        x += step
    }
    var y = 0f
    while (y < size.height) {
        drawLine(color, Offset(0f, y), Offset(size.width, y), 1f)
        y += step
    }
}

private fun statusReport(battery: BatteryInfo, clock: ClockState): String = buildString {
    append("All systems nominal, sir. ")
    append("Power reserves at ${battery.percent} percent")
    append(if (battery.charging) ", and charging. " else ". ")
    append("The time is ${clock.time.substringBeforeLast(':')}.")
}
