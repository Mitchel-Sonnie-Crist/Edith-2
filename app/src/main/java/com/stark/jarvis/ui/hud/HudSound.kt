package com.stark.jarvis.ui.hud

/**
 * Sound cues the HUD emits at specific animation beats. The composable stays
 * decoupled from the audio engine: it just calls back with one of these, and the
 * host (the overlay service) maps them onto [com.stark.jarvis.audio.SoundManager].
 */
enum class HudSound {
    /** Sustained arc-reactor power-up hum, fired as the HUD fades in. */
    POWER_UP,

    /** Short metallic chime as each diagnostic ring completes. */
    RING_CHIME,

    /** UI beep as each telemetry line prints. */
    TELEMETRY_BEEP,

    /** Confirmation tone on "SYSTEMS ONLINE". */
    ONLINE,

    /** Soft system-diagnostic click, layered under continuous HUD activity. */
    DIAG_CLICK,

    /** Sharp glitch/spark tick, fired on random HUD spark flashes. */
    SPARK,
}
