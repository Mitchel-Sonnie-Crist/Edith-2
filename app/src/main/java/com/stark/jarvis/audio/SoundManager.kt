package com.stark.jarvis.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import androidx.annotation.RawRes
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.stark.jarvis.ui.hud.HudSound
import java.util.concurrent.ConcurrentHashMap

/**
 * Audio engine for the boot sequence.
 *
 * - **SoundPool** drives the short, latency-sensitive SFX (ring chime, UI beep,
 *   confirmation tone). SoundPool decodes to memory up-front, so triggering is
 *   effectively instant — exactly what synchronized HUD beeps need.
 * - **ExoPlayer (Media3)** drives the longer, sustained arc-reactor power-up hum,
 *   where a slightly higher trigger latency is irrelevant.
 *
 * Audio files are resolved from `res/raw` **by name at runtime**, so the project
 * compiles cleanly with no assets present and simply stays silent until you drop
 * the files in. Expected filenames (any of .ogg/.mp3/.wav):
 *
 * | HudSound          | res/raw file          |
 * |-------------------|-----------------------|
 * | POWER_UP          | `jarvis_power_up`     |
 * | RING_CHIME        | `jarvis_ring_chime`   |
 * | TELEMETRY_BEEP    | `jarvis_beep`         |
 * | ONLINE            | `jarvis_online`       |
 *
 * Not thread-safe across arbitrary threads; call from a single (main) thread.
 */
class SoundManager(private val context: Context) {

    private val appContext = context.applicationContext

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    // sampleId -> loaded?  Guards against playing a sample before it is decoded.
    private val loaded = ConcurrentHashMap<Int, Boolean>()

    // HudSound -> SoundPool sampleId (short SFX only).
    private val sampleIds = HashMap<HudSound, Int>()

    private var exoPlayer: ExoPlayer? = null
    private var released = false

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            loaded[sampleId] = (status == 0)
            if (status != 0) Log.w(TAG, "SoundPool failed to load sample $sampleId (status=$status)")
        }
        preloadSfx()
    }

    private fun preloadSfx() {
        mapOf(
            HudSound.RING_CHIME to "jarvis_ring_chime",
            HudSound.TELEMETRY_BEEP to "jarvis_beep",
            HudSound.ONLINE to "jarvis_online",
            HudSound.DIAG_CLICK to "jarvis_click",
            HudSound.SPARK to "jarvis_spark",
        ).forEach { (cue, name) ->
            val resId = rawResId(name)
            if (resId != 0) {
                val id = soundPool.load(appContext, resId, 1)
                sampleIds[cue] = id
            } else {
                Log.i(TAG, "No res/raw/$name found — '$cue' will be silent.")
            }
        }
    }

    /** Route a HUD cue to the right engine. Safe to call rapidly. */
    fun play(cue: HudSound) {
        if (released) return
        when (cue) {
            HudSound.POWER_UP -> playHum()
            else -> playSfx(cue)
        }
    }

    private fun playSfx(cue: HudSound) {
        val id = sampleIds[cue] ?: return
        if (loaded[id] == true) {
            // Per-cue mix + a touch of pitch jitter so layered clicks/sparks never
            // sound like the same sample retriggering — richer, more "alive".
            val vol = when (cue) {
                HudSound.DIAG_CLICK -> 0.35f
                HudSound.SPARK -> 0.5f
                else -> VOLUME
            }
            val rate = when (cue) {
                HudSound.DIAG_CLICK, HudSound.SPARK -> 0.9f + Math.random().toFloat() * 0.3f
                else -> 1f
            }
            soundPool.play(id, vol, vol, /* priority = */ 1, /* loop = */ 0, rate)
        } else {
            // Decode may still be in flight for the very first cue; skip rather
            // than block. (Assets are tiny, so this is rare in practice.)
            Log.d(TAG, "Sample for $cue not ready yet; skipping.")
        }
    }

    private fun playHum() {
        val resId = rawResId("jarvis_power_up")
        if (resId == 0) {
            Log.i(TAG, "No res/raw/jarvis_power_up found — power-up hum will be silent.")
            return
        }
        // Reuse a single player instance.
        val player = exoPlayer ?: ExoPlayer.Builder(appContext).build().also { p ->
            p.setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_SONIFICATION)
                    .build(),
                /* handleAudioFocus = */ false,
            )
            p.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    Log.w(TAG, "ExoPlayer error on power-up hum", error)
                }
            })
            exoPlayer = p
        }
        val uri = "android.resource://${appContext.packageName}/$resId"
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        player.playWhenReady = true
    }

    @RawRes
    private fun rawResId(name: String): Int =
        appContext.resources.getIdentifier(name, "raw", appContext.packageName)

    /** Release all native audio resources. Call from the owner's teardown. */
    fun release() {
        if (released) return
        released = true
        soundPool.release()
        exoPlayer?.release()
        exoPlayer = null
        loaded.clear()
        sampleIds.clear()
    }

    private companion object {
        const val TAG = "JarvisSoundManager"
        const val MAX_STREAMS = 4
        const val VOLUME = 0.9f
    }
}
