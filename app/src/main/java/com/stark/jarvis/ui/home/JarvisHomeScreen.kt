package com.stark.jarvis.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stark.jarvis.audio.JarvisVoice
import com.stark.jarvis.ui.theme.JarvisColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val HudBackground = Brush.verticalGradient(
    listOf(Color(0xFF061319), Color(0xFF0A2531), Color(0xFF061319)),
)

private val FavoriteSubtitles =
    listOf("MARK VII", "A.I.", "SYSTEM", "HOLOMAP", "AUDIO LINK", "MODULE")

/**
 * The full J.A.R.V.I.S. launcher home screen, styled after the Stark HUD:
 * hexagon app modules, a central multi-ring arc reactor, orbital + map readouts,
 * WEATHER / POWER / HEALTH stat panels, and a bottom command bar.
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
    var drawerOpen by remember { mutableStateOf(false) }

    val apps by produceState(initialValue = emptyList<AppInfo>()) {
        value = withContext(Dispatchers.IO) { AppsRepository.loadLaunchableApps(context) }
    }

    LaunchedEffect(Unit) {
        voice.speak("${greetingForHour(clock.hour)} All systems online.")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HudBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            TopStrip(
                time = clock.time,
                batteryPct = battery.percent,
                onApps = { drawerOpen = true },
            )

            Spacer(Modifier.height(4.dp))

            // ---- Apps (left) + reactor (right) ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val favorites = apps.take(5)
                    if (favorites.isEmpty()) {
                        Text(
                            "SCANNING MODULES…",
                            color = JarvisColors.ElectricCyan,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                        )
                    }
                    favorites.forEachIndexed { i, app ->
                        HexAppRow(
                            app = app,
                            subtitle = FavoriteSubtitles[i % FavoriteSubtitles.size],
                            onClick = { AppsRepository.launch(context, app) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                HudReactor(
                    topLabel = "JARVIS HUD: ONLINE",
                    bottomLabel = "SYSTEM STATUS: 100% ONLINE",
                    modifier = Modifier
                        .size(220.dp)
                        .clickable { voice.speak(statusReport(battery, clock)) },
                )
            }

            Spacer(Modifier.height(6.dp))
            Divider()
            Spacer(Modifier.height(8.dp))

            // ---- Readouts (left) + stat panels (right) ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    HudOrbital(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp),
                    )
                    Spacer(Modifier.height(6.dp))
                    HudWireframe(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    StatCard("☀", JarvisColors.ElectricCyan, "WEATHER", "21°C", "LOCAL · SUNNY")
                    StatCard(
                        "⚡", JarvisColors.StarkAmber, "POWER", "${battery.percent}%",
                        if (battery.charging) "ARC REACTOR · CHARGING" else "ARC REACTOR · ONLINE",
                    )
                    StatCard("♥", JarvisColors.StarkAmber, "HEALTH", "72 BPM", "BP 120/80 · OPTIMAL")
                }
            }

            Spacer(Modifier.height(10.dp))

            // ---- Command bar ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ActionButton("SYSTEM\nDIAGNOSTICS", { voice.speak(statusReport(battery, clock)) }, Modifier.weight(1f))
                ActionButton("FLIGHT\nMODE", { voice.speak("Flight systems on standby, sir.") }, Modifier.weight(1f))
                ActionButton("COMMS\nHUB", { drawerOpen = true }, Modifier.weight(1f))
                ActionButton("JARVIS A.I.\nCOMMAND", { voice.speak(statusReport(battery, clock)) }, Modifier.weight(1f))
            }

            Spacer(Modifier.height(8.dp))

            // ---- Footer ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "STARK INDUSTRIES",
                    color = JarvisColors.ElectricCyan.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    letterSpacing = 2.sp,
                )
                Text(
                    "⚙ SETTINGS",
                    color = JarvisColors.ElectricCyan.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    letterSpacing = 2.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onOpenSettings() }
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }

        // ---- Full app drawer overlay ----
        AnimatedVisibility(visible = drawerOpen, enter = fadeIn(), exit = fadeOut()) {
            AppDrawer(
                apps = apps,
                onLaunch = { app ->
                    AppsRepository.launch(context, app)
                    drawerOpen = false
                },
                onClose = { drawerOpen = false },
            )
        }
    }
}

@Composable
private fun TopStrip(time: String, batteryPct: Int, onApps: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            "JARVIS OS",
            color = JarvisColors.ElectricCyan,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 3.sp,
        )
        Text(
            "[ SYSTEM STATUS: 100% ONLINE ]",
            color = JarvisColors.IceWhite,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            letterSpacing = 1.sp,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$batteryPct%  $time",
                color = JarvisColors.ElectricCyan,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "▦ APPS",
                color = JarvisColors.StarkAmber,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.dp, JarvisColors.StarkAmber, RoundedCornerShape(4.dp))
                    .clickable { onApps() }
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
private fun AppDrawer(
    apps: List<AppInfo>,
    onLaunch: (AppInfo) -> Unit,
    onClose: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF2061319)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "APPLICATIONS  ·  ${apps.size}",
                    color = JarvisColors.ElectricCyan,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 3.sp,
                )
                Text(
                    "✕ CLOSE",
                    color = JarvisColors.StarkAmber,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onClose() }
                        .padding(8.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(apps, key = { it.packageName }) { app ->
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onLaunch(app) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .border(1.5.dp, JarvisColors.ElectricCyan, HexagonShape)
                                .padding(8.dp),
                        ) {
                            Image(
                                bitmap = app.icon,
                                contentDescription = app.label,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(HexagonShape),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            app.label,
                            color = JarvisColors.IceWhite,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
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

private fun statusReport(battery: BatteryInfo, clock: ClockState): String = buildString {
    append("All systems nominal, sir. ")
    append("Power reserves at ${battery.percent} percent")
    append(if (battery.charging) ", and charging. " else ". ")
    append("The time is ${clock.time.substringBeforeLast(':')}.")
}
