package com.maxinesworld.gamefireflycatch

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Asset-free cue player built on ToneGenerator so the module compiles with no
 * bundled audio. Swap for a SoundPool + R.raw.* player once final sound art lands.
 * All calls are guarded; audio must never crash a reward break.
 */
class ToneCuePlayer : AutoCloseable {
    private val gen: ToneGenerator? = runCatching {
        ToneGenerator(AudioManager.STREAM_MUSIC, 70)
    }.getOrNull()

    private fun cue(tone: Int, ms: Int) { runCatching { gen?.startTone(tone, ms) } }

    fun tap() = cue(ToneGenerator.TONE_PROP_BEEP, 90)
    fun good() = cue(ToneGenerator.TONE_PROP_ACK, 150)
    fun oops() = cue(ToneGenerator.TONE_PROP_NACK, 150)
    fun celebrate() = cue(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 250)

    override fun close() { runCatching { gen?.release() } }
}
