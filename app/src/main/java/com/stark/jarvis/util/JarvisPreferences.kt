package com.stark.jarvis.util

import android.content.Context

/**
 * Tiny SharedPreferences-backed settings store for the control panel toggles.
 */
class JarvisPreferences(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Play the HUD when the device finishes booting. */
    var playOnBoot: Boolean
        get() = prefs.getBoolean(KEY_ON_BOOT, true)
        set(value) = prefs.edit().putBoolean(KEY_ON_BOOT, value).apply()

    /** Play the HUD each time the device is unlocked (ACTION_USER_PRESENT). */
    var playOnUnlock: Boolean
        get() = prefs.getBoolean(KEY_ON_UNLOCK, false)
        set(value) = prefs.edit().putBoolean(KEY_ON_UNLOCK, value).apply()

    private companion object {
        const val FILE = "jarvis_prefs"
        const val KEY_ON_BOOT = "play_on_boot"
        const val KEY_ON_UNLOCK = "play_on_unlock"
    }
}
