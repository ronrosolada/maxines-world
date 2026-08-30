package com.maxinesworld.featurelessonplayer

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.provider.Settings

/** Asset-free, best-effort quiz cues. Every platform call is guarded. */
internal class ArenaSoundEffectPlayer(context: Context) : AutoCloseable {
    private val appContext = context.applicationContext
    private val audioManager = runCatching {
        appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }.getOrNull()
    private val tone = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 55) }.getOrNull()

    private fun isEnabled(): Boolean = runCatching {
        val audioAllowed = audioManager?.ringerMode != AudioManager.RINGER_MODE_SILENT &&
            (audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0) > 0
        val reducedMotion = Settings.Global.getFloat(
            appContext.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
        audioAllowed && !reducedMotion
    }.getOrDefault(false)

    fun play(effect: ArenaSoundEffect) {
        if (!isEnabled()) return
        runCatching {
            when (effect) {
                ArenaSoundEffect.CORRECT -> tone?.startTone(ToneGenerator.TONE_PROP_ACK, 180)
                ArenaSoundEffect.ENCOURAGEMENT -> tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 120)
                ArenaSoundEffect.CELEBRATION -> tone?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 350)
            }
        }
    }

    override fun close() { runCatching { tone?.release() } }
}
