package com.stark.jarvis.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.stark.jarvis.service.JarvisOverlayService
import com.stark.jarvis.util.JarvisPreferences
import com.stark.jarvis.util.OverlayPermission

/**
 * Launches the J.A.R.V.I.S. boot sequence when the device finishes booting.
 *
 * A [BroadcastReceiver] gets only a few seconds of CPU and cannot draw UI itself,
 * so it does the minimum: verify the trigger and hand off to
 * [JarvisOverlayService], which is allowed to be started from BOOT_COMPLETED and
 * owns the actual overlay + audio.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!isBootAction(intent.action)) return

        val prefs = JarvisPreferences(context)
        if (!prefs.playOnBoot) {
            Log.i(TAG, "Play-on-boot disabled; skipping.")
            return
        }
        if (!OverlayPermission.canDrawOverlays(context)) {
            // Nothing we can do from here — the user must grant it in the app.
            Log.w(TAG, "Overlay permission not granted; boot HUD skipped.")
            return
        }

        Log.i(TAG, "Boot completed (${intent.action}) — starting HUD service.")
        JarvisOverlayService.play(context)
    }

    private fun isBootAction(action: String?): Boolean = action in BOOT_ACTIONS

    private companion object {
        const val TAG = "JarvisBootReceiver"
        val BOOT_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
        )
    }
}
