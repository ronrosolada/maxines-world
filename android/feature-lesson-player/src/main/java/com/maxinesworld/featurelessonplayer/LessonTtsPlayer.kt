package com.maxinesworld.featurelessonplayer

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class LessonTtsPlayer(context: Context) {
    private var tts: TextToSpeech? = null
    private var initialized = false
    private var isSpeaking = false
    private var onDone: (() -> Unit)? = null
    private var pendingRequest: SpeakRequest? = null

    private data class SpeakRequest(
        val text: String,
        val language: String,
        val onComplete: (() -> Unit)?,
        val onUnavailable: (() -> Unit)?,
    )

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                initialized = true
                tts?.language = Locale.US
                pendingRequest?.let { request ->
                    pendingRequest = null
                    speak(request.text, request.language, request.onComplete, request.onUnavailable)
                }
            } else {
                initialized = false
                pendingRequest?.onUnavailable?.invoke()
                pendingRequest = null
            }
        }
    }

    fun speak(
        text: String,
        language: String = "english",
        onComplete: (() -> Unit)? = null,
        onUnavailable: (() -> Unit)? = null
    ) {
        val engine = tts ?: run {
            onUnavailable?.invoke()
            return
        }
        if (!initialized) {
            pendingRequest = SpeakRequest(text, language, onComplete, onUnavailable)
            return
        }
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
                    isSpeaking = false
                    onDone = null
                    onUnavailable?.invoke()
                    return
                }
            }
            else -> engine.language = Locale.US
        }

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                val callback = onDone
                onDone = null
                callback?.invoke()
            }
            @Deprecated("")
            @Suppress("DEPRECATION")
            override fun onError(utteranceId: String?) {
                isSpeaking = false
                onDone = null
                onUnavailable?.invoke()
            }
        })

        if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lesson_narration") == TextToSpeech.ERROR) {
            isSpeaking = false
            onDone = null
            onUnavailable?.invoke()
        }
    }

    fun stop() {
        pendingRequest = null
        tts?.stop()
        isSpeaking = false
    }

    fun isSpeaking(): Boolean = isSpeaking

    fun shutdown() {
        pendingRequest = null
        initialized = false
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
