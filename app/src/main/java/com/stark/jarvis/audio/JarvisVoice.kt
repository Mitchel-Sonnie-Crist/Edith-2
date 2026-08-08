package com.stark.jarvis.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * The J.A.R.V.I.S. voice.
 *
 * Wraps Android's built-in [TextToSpeech] engine and tunes it for a calm British
 * "AI butler" delivery — UK English, a slightly lowered pitch, and an unhurried
 * rate. Because it uses the device's on-board TTS, **no audio files are bundled**;
 * the voice is part of the single APK and works offline once the system has an
 * English voice installed.
 *
 * Init is asynchronous, so lines requested before the engine is ready are queued
 * and flushed on init — important because the boot sequence starts speaking almost
 * immediately.
 */
class JarvisVoice(context: Context) {

    private var tts: TextToSpeech? = null
    private var ready = false
    private val pending = ArrayDeque<String>()

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                configure()
            } else {
                Log.w(TAG, "TextToSpeech init failed (status=$status); voice disabled.")
            }
        }
    }

    private fun configure() {
        val engine = tts ?: return
        // Refined British-butler delivery.
        val langResult = engine.setLanguage(Locale.UK)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fall back to the default locale rather than staying silent.
            engine.setLanguage(Locale.getDefault())
        }
        engine.setPitch(PITCH)
        engine.setSpeechRate(RATE)
        selectPreferredVoice(engine)

        ready = true
        while (pending.isNotEmpty()) speakNow(pending.removeFirst())
    }

    /** Prefer an offline British male voice when the engine exposes one. */
    private fun selectPreferredVoice(engine: TextToSpeech) {
        val voices = runCatching { engine.voices }.getOrNull().orEmpty()
        val british = voices.filter {
            it.locale?.language.equals("en", true) &&
                it.locale?.country.equals("GB", true)
        }
        val chosen = british.firstOrNull {
            it.name.contains("male", true) && !it.name.contains("female", true) &&
                !it.isNetworkConnectionRequired
        }
            ?: british.firstOrNull { !it.name.contains("female", true) && !it.isNetworkConnectionRequired }
            ?: british.firstOrNull { !it.name.contains("female", true) }
            ?: british.firstOrNull()
        chosen?.let { runCatching { engine.setVoice(it) } }
    }

    /** Speak a line, queued after any currently-playing line. */
    fun speak(text: String) {
        if (text.isBlank()) return
        if (ready) speakNow(text) else pending.addLast(text)
    }

    private fun speakNow(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "jarvis-${text.hashCode()}")
    }

    /** Stop any in-progress speech without tearing down the engine. */
    fun stop() {
        runCatching { tts?.stop() }
    }

    /** Release the engine. Call from the owner's teardown. */
    fun release() {
        ready = false
        pending.clear()
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
    }

    private companion object {
        const val TAG = "JarvisVoice"
        const val PITCH = 0.9f   // slightly deeper than default → composed, masculine
        const val RATE = 0.98f   // measured, unhurried
    }
}
