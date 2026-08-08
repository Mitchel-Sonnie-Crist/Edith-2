package com.stark.jarvis.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** A launchable application shown in the J.A.R.V.I.S. home screen. */
data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: ImageBitmap,
)

object AppsRepository {

    /** Query every launchable app on the device (blocking — call off the main thread). */
    fun loadLaunchableApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities = pm.queryIntentActivities(intent, 0)
        return activities.asSequence()
            .mapNotNull { ri ->
                val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
                val label = ri.loadLabel(pm)?.toString().orEmpty().ifBlank { pkg }
                val icon = runCatching { ri.loadIcon(pm).toImageBitmap() }.getOrNull()
                    ?: return@mapNotNull null
                AppInfo(label, pkg, icon)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
            .toList()
    }

    /** Launch an app in a new task. */
    fun launch(context: Context, app: AppInfo) {
        val launch = context.packageManager.getLaunchIntentForPackage(app.packageName) ?: return
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(launch) }
    }
}

private fun Drawable.toImageBitmap(size: Int = 144): ImageBitmap {
    val w = if (intrinsicWidth > 0) intrinsicWidth else size
    val h = if (intrinsicHeight > 0) intrinsicHeight else size
    return toBitmap(width = w, height = h).asImageBitmap()
}

data class BatteryInfo(val percent: Int, val charging: Boolean)

/** Live battery percentage + charging state, backed by a registered receiver. */
@Composable
fun rememberBattery(): BatteryInfo {
    val context = LocalContext.current
    var info by remember { mutableStateOf(BatteryInfo(0, false)) }
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                i ?: return
                val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val pct = if (level >= 0 && scale > 0) level * 100 / scale else 0
                val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
                info = BatteryInfo(pct, charging)
            }
        }
        // NOT_EXPORTED keeps this compliant on Android 13+/14.
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return info
}

data class ClockState(val time: String, val date: String, val hour: Int)

/** Ticking clock state, updated once per second. */
@Composable
fun rememberClock(): ClockState {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000L)
        }
    }
    val timeFmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("EEEE, dd MMM", Locale.getDefault()) }
    val date = Date(now)
    val hour = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.HOUR_OF_DAY)
    return ClockState(
        time = timeFmt.format(date),
        date = dateFmt.format(date).uppercase(Locale.getDefault()),
        hour = hour,
    )
}

/** Time-of-day greeting in the J.A.R.V.I.S. register. */
fun greetingForHour(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning, sir."
    in 12..17 -> "Good afternoon, sir."
    else -> "Good evening, sir."
}
