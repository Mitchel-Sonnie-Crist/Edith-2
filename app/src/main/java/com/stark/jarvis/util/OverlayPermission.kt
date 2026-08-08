package com.stark.jarvis.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Helpers for the special "display over other apps" permission
 * (SYSTEM_ALERT_WINDOW), which cannot be granted by a normal runtime prompt —
 * the user must toggle it in system Settings.
 */
object OverlayPermission {

    fun canDrawOverlays(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    /**
     * Intent that deep-links to this app's "Display over other apps" settings
     * screen. Launch it from an Activity (add FLAG_ACTIVITY_NEW_TASK if starting
     * from a non-Activity context).
     */
    fun settingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
}
