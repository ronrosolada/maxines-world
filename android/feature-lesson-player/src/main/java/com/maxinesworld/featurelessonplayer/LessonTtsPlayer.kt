package com.maxinesworld.featurelessonplayer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class LessonTtsPlayer(context: Context) {
    private val audioManager = context.applicationContext
        .getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        .setOnAudioFocusChangeListener { change ->
            if (change == AudioManager.AUDIOFOCUS_LOSS) stop()
        }
        .build()
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
        if (audioManager.requestAudioFocus(audioFocusRequest) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            onUnavailable?.invoke()
            return
        }
        onDone = onComplete
        isSpeaking = true

        when {
            language.startsWith("fil") -> {
                val filLocale = Locale.Builder().setLanguage("fil").setRegion("PH").build()
                val result = engine.setLanguage(filLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    failSpeech(onUnavailable)
                    return
                }
            }
            language.startsWith("en") -> {
                val enPh = Locale.Builder().setLanguage("en").setRegion("PH").build()
                engine.language =
                    if (engine.isLanguageAvailable(enPh) >= TextToSpeech.LANG_AVAILABLE) enPh
                    else Locale.US
            }
            else -> {
                failSpeech(onUnavailable)
                return
            }
        }

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                val callback = onDone
                onDone = null
                abandonAudioFocus()
                callback?.invoke()
            }

            @Deprecated("")
            @Suppress("DEPRECATION")
            override fun onError(utteranceId: String?) {
                failSpeech(onUnavailable)
            }
        })

        if (engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lesson_narration") == TextToSpeech.ERROR) {
            failSpeech(onUnavailable)
        }
    }

    fun stop() {
        pendingRequest = null
        tts?.stop()
        isSpeaking = false
        onDone = null
        abandonAudioFocus()
    }

    fun isSpeaking(): Boolean = isSpeaking

    fun shutdown() {
        pendingRequest = null
        initialized = false
        tts?.stop()
        tts?.shutdown()
        tts = null
        isSpeaking = false
        onDone = null
        abandonAudioFocus()
    }

    private fun failSpeech(onUnavailable: (() -> Unit)?) {
        isSpeaking = false
        onDone = null
        abandonAudioFocus()
        onUnavailable?.invoke()
    }

    private fun abandonAudioFocus() {
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
    }
}
