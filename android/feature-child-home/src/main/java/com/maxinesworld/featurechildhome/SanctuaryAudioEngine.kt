package com.maxinesworld.featurechildhome

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

/** Small offline sensory layer; callers gate haptics and reduced motion. */
class SanctuaryAudioEngine(context: Context) : AutoCloseable {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val tones = ToneGenerator(AudioManager.STREAM_MUSIC, 32)

    val soundAllowed: Boolean get() = audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL

    fun purr() { if (soundAllowed) tones.startTone(ToneGenerator.TONE_PROP_BEEP2, 160) }
    fun discovery() { if (soundAllowed) tones.startTone(ToneGenerator.TONE_PROP_ACK, 140) }
    fun ambientCue() { if (soundAllowed) tones.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 70) }
    override fun close() = tones.release()
}
