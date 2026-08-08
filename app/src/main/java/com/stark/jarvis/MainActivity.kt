package com.stark.jarvis

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.stark.jarvis.audio.SoundManager
import com.stark.jarvis.service.JarvisOverlayService
import com.stark.jarvis.ui.hud.JarvisHud
import com.stark.jarvis.ui.theme.JarvisColors
import com.stark.jarvis.ui.theme.JarvisTheme
import com.stark.jarvis.util.JarvisPreferences
import com.stark.jarvis.util.OverlayPermission

/**
 * Control panel: grant the overlay permission, toggle boot/unlock playback, and
 * preview the HUD (either in-app, which needs no permission, or as the real
 * system overlay).
 */
class MainActivity : ComponentActivity() {

    private var overlayGranted by mutableStateOf(false)

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            overlayGranted = OverlayPermission.canDrawOverlays(this)
        }

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* best-effort */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overlayGranted = OverlayPermission.canDrawOverlays(this)
        maybeRequestNotificationPermission()

        setContent {
            JarvisTheme {
                ControlPanel(
                    overlayGranted = overlayGranted,
                    onGrantOverlay = {
                        overlayPermissionLauncher.launch(OverlayPermission.settingsIntent(this))
                    },
                    onTriggerOverlay = { JarvisOverlayService.play(this) },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        overlayGranted = OverlayPermission.canDrawOverlays(this)
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@Composable
private fun ControlPanel(
    overlayGranted: Boolean,
    onGrantOverlay: () -> Unit,
    onTriggerOverlay: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { JarvisPreferences(context) }

    var playOnBoot by remember { mutableStateOf(prefs.playOnBoot) }
    var playOnUnlock by remember { mutableStateOf(prefs.playOnUnlock) }
    var previewing by remember { mutableStateOf(false) }

    // Own a SoundManager for the in-app preview; release it with the composition.
    val soundManager = remember { SoundManager(context) }
    DisposableEffect(Unit) {
        onDispose { soundManager.release() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "J.A.R.V.I.S. OS",
            color = JarvisColors.ElectricCyan,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 6.sp,
            fontSize = 24.sp,
        )
        Text(
            text = "Boot HUD overlay for Niagara Launcher",
            color = JarvisColors.IceWhite,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
        )

        Spacer(Modifier.height(6.dp))

        // --- Permission card ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = JarvisColors.PanelBlack),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (overlayGranted) {
                        context.getString(R.string.cta_overlay_granted)
                    } else {
                        "Overlay permission required"
                    },
                    color = if (overlayGranted) JarvisColors.ElectricCyan else JarvisColors.StarkAmber,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                )
                if (!overlayGranted) {
                    Button(onClick = onGrantOverlay, modifier = Modifier.fillMaxWidth()) {
                        Text(context.getString(R.string.cta_grant_overlay))
                    }
                }
            }
        }

        // --- Trigger toggles ---
        SettingSwitch(
            label = context.getString(R.string.switch_boot),
            checked = playOnBoot,
            onChecked = {
                playOnBoot = it
                prefs.playOnBoot = it
            },
        )
        SettingSwitch(
            label = context.getString(R.string.switch_unlock),
            checked = playOnUnlock,
            onChecked = {
                playOnUnlock = it
                prefs.playOnUnlock = it
            },
        )

        Spacer(Modifier.height(6.dp))

        // In-app preview (no overlay permission needed).
        Button(
            onClick = { previewing = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(context.getString(R.string.cta_preview))
        }

        // Real system-overlay trigger (uses the foreground service).
        OutlinedButton(
            onClick = onTriggerOverlay,
            enabled = overlayGranted,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Trigger as system overlay")
        }
    }

    // In-app preview layer, drawn above the panel.
    AnimatedVisibility(
        visible = previewing,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        JarvisHud(
            alignment = Alignment.Center,
            onSound = { cue -> soundManager.play(cue) },
            onFinished = { previewing = false },
        )
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = JarvisColors.PanelBlack),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    color = JarvisColors.IceWhite,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(checked = checked, onCheckedChange = onChecked)
            }
        }
    }
}
