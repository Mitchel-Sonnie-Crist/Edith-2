package com.stark.jarvis.ui.immersion

import android.app.ActivityManager
import android.content.Context
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.max
import kotlin.random.Random

/**
 * Live device telemetry powering the HUD. Memory load, network throughput and
 * battery are **real**; CPU temperature is read from the thermal sysfs node when
 * available and otherwise falls back to a plausible simulated feed (most devices
 * don't expose it without privileges).
 *
 * Updated ~8×/second so graphs animate smoothly.
 */
class SystemMetrics {
    var cpuTemp by mutableFloatStateOf(42f)
    var memPercent by mutableIntStateOf(0)
    var netKbps by mutableFloatStateOf(0f)
    var batteryPct by mutableIntStateOf(0)

    /** Rolling histories for the live graphs (normalised 0..1). */
    val netHistory: SnapshotStateList<Float> = mutableStateListOf()
    val battHistory: SnapshotStateList<Float> = mutableStateListOf()
    val cpuHistory: SnapshotStateList<Float> = mutableStateListOf()

    internal fun pushHistory(list: SnapshotStateList<Float>, value: Float, cap: Int = 64) {
        list.add(value.coerceIn(0f, 1f))
        while (list.size > cap) list.removeAt(0)
    }
}

@Composable
fun rememberSystemMetrics(): SystemMetrics {
    val context = LocalContext.current
    val metrics = remember { SystemMetrics() }

    LaunchedEffect(Unit) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        var lastRx = TrafficStats.getTotalRxBytes().let { if (it < 0) 0L else it }
        var lastTx = TrafficStats.getTotalTxBytes().let { if (it < 0) 0L else it }
        var lastAt = SystemClock.elapsedRealtime()
        var peakKbps = 256f

        while (true) {
            // ---- Memory (real) ----
            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            val total = max(mi.totalMem, 1L)
            val used = total - mi.availMem
            metrics.memPercent = (used * 100 / total).toInt()

            // ---- Network throughput (real) ----
            val now = SystemClock.elapsedRealtime()
            val rx = TrafficStats.getTotalRxBytes().let { if (it < 0) lastRx else it }
            val tx = TrafficStats.getTotalTxBytes().let { if (it < 0) lastTx else it }
            val dt = max(now - lastAt, 1L) / 1000f
            val kbps = ((rx - lastRx) + (tx - lastTx)) / 1024f / dt
            lastRx = rx; lastTx = tx; lastAt = now
            metrics.netKbps = kbps
            peakKbps = max(peakKbps, kbps)
            metrics.pushHistory(metrics.netHistory, (kbps / peakKbps))

            // ---- Battery (real) ----
            val batt = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            if (batt in 0..100) {
                metrics.batteryPct = batt
                metrics.pushHistory(metrics.battHistory, batt / 100f)
            }

            // ---- CPU temp (thermal sysfs, else simulated) ----
            val temp = readCpuTemp() ?: simulateTemp(metrics.cpuTemp)
            metrics.cpuTemp = temp
            metrics.pushHistory(metrics.cpuHistory, ((temp - 30f) / 50f))

            delay(120L)
        }
    }
    return metrics
}

private fun readCpuTemp(): Float? = runCatching {
    val f = File("/sys/class/thermal/thermal_zone0/temp")
    if (!f.canRead()) return null
    val raw = f.readText().trim().toFloatOrNull() ?: return null
    // Values are usually milli-°C.
    val c = if (raw > 1000f) raw / 1000f else raw
    if (c in 10f..120f) c else null
}.getOrNull()

private fun simulateTemp(prev: Float): Float {
    val drift = (Random.nextFloat() - 0.5f) * 1.4f
    return (prev + drift).coerceIn(38f, 61f)
}
