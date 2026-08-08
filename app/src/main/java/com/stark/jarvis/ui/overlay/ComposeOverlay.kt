package com.stark.jarvis.ui.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

/**
 * Presents arbitrary Compose [content] as a full-screen, **non-interactive**
 * system overlay via [WindowManager].
 *
 * The window is `FLAG_NOT_TOUCHABLE`, so every touch passes straight through to
 * the launcher underneath — the HUD is purely decorative and never blocks the
 * user from swiping Niagara's app list while it plays.
 *
 * Requires the "display over other apps" permission (SYSTEM_ALERT_WINDOW); the
 * caller is responsible for checking it (see [com.stark.jarvis.util.OverlayPermission]).
 */
class ComposeOverlay(
    private val context: Context,
    private val content: @Composable () -> Unit,
) {
    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val lifecycleOwner = OverlayLifecycleOwner()
    private var composeView: ComposeView? = null
    private var shown = false

    fun show() {
        if (shown) return
        shown = true

        lifecycleOwner.onCreate()

        val view = ComposeView(context).apply {
            // Wire up the ViewTree owners so Compose can run without an Activity.
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent(content)
        }
        composeView = view

        windowManager.addView(view, buildLayoutParams())
        lifecycleOwner.onResume()
    }

    fun remove() {
        if (!shown) return
        shown = false
        composeView?.let { v ->
            runCatching { windowManager.removeViewImmediate(v) }
        }
        composeView = null
        lifecycleOwner.onDestroy()
    }

    private fun buildLayoutParams(): WindowManager.LayoutParams {
        @Suppress("DEPRECATION")
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            // Non-focusable + non-touchable => fully transparent to input.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            // Draw under notches/cutouts too, so the HUD truly covers the screen.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
    }
}
