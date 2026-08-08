package com.stark.jarvis.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.stark.jarvis.MainActivity
import com.stark.jarvis.audio.JarvisVoice
import com.stark.jarvis.ui.theme.JarvisTheme

/**
 * The launcher / Home activity. Set this app as the device Home app and the whole
 * home screen becomes the J.A.R.V.I.S. HUD ([JarvisHomeScreen]).
 *
 * Declared with the HOME + DEFAULT categories in the manifest so it appears in
 * Settings → Default apps → Home app.
 */
class HomeActivity : ComponentActivity() {

    private var voice: JarvisVoice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val jarvisVoice = JarvisVoice(this).also { voice = it }

        setContent {
            JarvisTheme {
                JarvisHomeScreen(
                    voice = jarvisVoice,
                    onOpenSettings = {
                        startActivity(Intent(this, MainActivity::class.java))
                    },
                )
            }
        }
    }

    override fun onDestroy() {
        voice?.release()
        voice = null
        super.onDestroy()
    }

    /** On the Home screen, Back should stay put rather than exit to nothing. */
    @Deprecated("Intentional no-op for a launcher Home activity")
    @Suppress("MissingSuperCall")
    override fun onBackPressed() {
        // no-op: we are the home screen
    }
}
