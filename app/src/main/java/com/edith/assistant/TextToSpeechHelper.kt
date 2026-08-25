package com.edith.assistant

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Wraps Android's TextToSpeech engine.
 * Two personas: EDITH (female) and VYRO (male) — both tuned to sound
 * calm and smooth rather than robotic/cartoonish, per the design notes.
 */
class TextToSpeechHelper(context: Context, private val onReady: () -> Unit = {}) {

    private var tts: TextToSpeech? = null
    var isMale: Boolean = false
        private set
    private var ready = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                applyPersona()
                ready = true
                onReady()
            }
        }
    }

    /** Switch persona. male=true -> "Vyro", male=false -> "Edith" */
    fun setPersona(male: Boolean) {
        isMale = male
        applyPersona()
    }

    private fun applyPersona() {
        val engine = tts ?: return
        // Calm, smooth delivery — not fast/cartoonish (per notes: "smooth + calm")
        engine.setPitch(if (isMale) 0.85f else 1.05f)
        engine.setSpeechRate(0.95f)

        val voices = engine.voices ?: return
        val target = voices.firstOrNull { v ->
            v.locale == Locale.US &&
                (if (isMale) v.name.contains("male", true) && !v.name.contains("female", true)
                 else v.name.contains("female", true))
        }
        target?.let { engine.voice = it }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        val engine = tts ?: return
        if (onDone != null) {
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) { onDone() }
                @Deprecated("") override fun onError(utteranceId: String?) { onDone() }
            })
        }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "edith_utterance")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
