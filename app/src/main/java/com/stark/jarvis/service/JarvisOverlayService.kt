package com.stark.jarvis.service

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.stark.jarvis.JarvisApplication
import com.stark.jarvis.R
import com.stark.jarvis.audio.JarvisVoice
import com.stark.jarvis.audio.SoundManager
import com.stark.jarvis.ui.hud.JarvisHud
import com.stark.jarvis.ui.overlay.ComposeOverlay
import com.stark.jarvis.ui.theme.JarvisTheme
import com.stark.jarvis.util.JarvisPreferences
import com.stark.jarvis.util.OverlayPermission

/**
 * Foreground service that owns the HUD overlay window and its audio.
 *
 * Two triggers:
 *  - **Boot** — [com.stark.jarvis.boot.BootCompletedReceiver] starts us with
 *    [ACTION_PLAY], and we play the sequence once.
 *  - **Unlock** — if the user enabled "play on unlock", we register a *dynamic*
 *    receiver for [Intent.ACTION_USER_PRESENT] (this action cannot be declared in
 *    the manifest) and replay the sequence on each unlock, staying resident.
 *
 * When nothing needs monitoring, the service stops itself as soon as the boot
 * animation completes, so it isn't a persistent battery cost.
 */
class JarvisOverlayService : LifecycleService() {

    private lateinit var prefs: JarvisPreferences
    private var soundManager: SoundManager? = null
    private var voice: JarvisVoice? = null
    private var overlay: ComposeOverlay? = null
    private var isPlaying = false
    private var monitoringUnlock = false
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Fires on each device unlock while we are resident. */
    private val userPresentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_USER_PRESENT) {
                Log.d(TAG, "USER_PRESENT — replaying boot sequence")
                playSequence()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = JarvisPreferences(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startAsForeground()

        // Stay resident to catch unlocks if requested.
        if (prefs.playOnUnlock && !monitoringUnlock) {
            registerReceiver(userPresentReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
            monitoringUnlock = true
        }

        when (intent?.action) {
            ACTION_PLAY, null -> playSequence()
            ACTION_STOP -> stopEverything()
        }

        return if (monitoringUnlock) START_STICKY else START_NOT_STICKY
    }

    private fun playSequence() {
        if (isPlaying) {
            Log.d(TAG, "Sequence already playing; ignoring trigger")
            return
        }
        if (!OverlayPermission.canDrawOverlays(this)) {
            Log.w(TAG, "Overlay permission not granted; cannot show HUD")
            finishOrIdle()
            return
        }

        isPlaying = true
        val sound = soundManager ?: SoundManager(this).also { soundManager = it }
        val jarvisVoice = voice ?: JarvisVoice(this).also { voice = it }

        val hud = ComposeOverlay(this) {
            JarvisTheme {
                JarvisHud(
                    onSound = { cue -> sound.play(cue) },
                    onSpeakLine = { line -> jarvisVoice.speak(line) },
                    onFinished = { onSequenceFinished() },
                )
            }
        }
        overlay = hud
        hud.show()
    }

    private fun onSequenceFinished() {
        // The visuals are done — remove the overlay immediately.
        overlay?.remove()
        overlay = null
        isPlaying = false

        if (monitoringUnlock) return // stay resident; the voice finishes naturally

        // The final spoken line ("…welcome back, sir.") outlasts the animation, so
        // hold the (silent) service open briefly before teardown to let it finish.
        mainHandler.postDelayed({ stopEverything() }, VOICE_TAIL_MS)
    }

    /** Stop immediately when there is nothing to play (e.g. missing permission). */
    private fun finishOrIdle() {
        if (!monitoringUnlock) {
            stopEverything()
        }
    }

    private fun stopEverything() {
        mainHandler.removeCallbacksAndMessages(null)
        overlay?.remove()
        overlay = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun startAsForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, JarvisApplication.CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(getString(R.string.notif_text))
            .setSmallIcon(R.drawable.ic_arc_reactor_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        if (monitoringUnlock) {
            runCatching { unregisterReceiver(userPresentReceiver) }
            monitoringUnlock = false
        }
        overlay?.remove()
        overlay = null
        soundManager?.release()
        soundManager = null
        voice?.release()
        voice = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "JarvisOverlayService"
        private const val NOTIFICATION_ID = 0xA5C

        // Grace period after the animation so the closing spoken line can finish.
        private const val VOICE_TAIL_MS = 3500L

        const val ACTION_PLAY = "com.stark.jarvis.action.PLAY"
        const val ACTION_STOP = "com.stark.jarvis.action.STOP"

        /** Convenience launcher used by the boot receiver and the control panel. */
        fun play(context: Context) {
            val intent = Intent(context, JarvisOverlayService::class.java).apply {
                action = ACTION_PLAY
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
