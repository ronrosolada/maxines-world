package com.maxinesworld.gamepawbeats

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

    fun pad(index: Int) = cue(
        when (index) {
            0 -> ToneGenerator.TONE_DTMF_1
            1 -> ToneGenerator.TONE_DTMF_4
            2 -> ToneGenerator.TONE_DTMF_7
            else -> ToneGenerator.TONE_DTMF_9
        }, 220)
    fun good() = cue(ToneGenerator.TONE_PROP_ACK, 150)
    fun oops() = cue(ToneGenerator.TONE_PROP_NACK, 150)
    fun celebrate() = cue(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 250)

    override fun close() { runCatching { gen?.release() } }
}
