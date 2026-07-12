package com.maxinesworld.featurelessonplayer

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class LessonTtsPlayer(context: Context) {
    private var tts: TextToSpeech? = null
    private var isSpeaking = false
    private var onDone: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
    }

    fun speak(text: String, language: String = "english", onComplete: (() -> Unit)? = null) {
        tts?.let { engine ->
            if (isSpeaking) return
            onDone = onComplete
            isSpeaking = true

            // Set locale based on language
            when (language) {
                "filipino" -> {
                    val filLocale = Locale.Builder()
                        .setLanguage("fil")
                        .setRegion("PH")
                        .build()
                    val result = engine.setLanguage(filLocale)
                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        engine.language = Locale.US
                    }
                }
                else -> engine.language = Locale.US
            }

            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                    onDone?.invoke()
                }
                @Deprecated("")
                @Suppress("DEPRECATION")
                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                    onDone?.invoke()
                }
            })

            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lesson_narration")
        }
    }

    fun stop() {
        tts?.stop()
        isSpeaking = false
    }

    fun isSpeaking(): Boolean = isSpeaking

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
