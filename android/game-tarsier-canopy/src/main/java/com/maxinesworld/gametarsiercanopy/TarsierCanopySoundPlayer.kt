package com.maxinesworld.gametarsiercanopy

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/** Lightweight SoundPool wrapper for Tarsier Canopy SFX. */
class TarsierCanopySoundPlayer(context: Context) : AutoCloseable {
    private val pool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val hopSound = pool.load(context, R.raw.tarsier_hop, 1)
    private val fireflySound = pool.load(context, R.raw.tarsier_firefly, 1)
    private val bumpSound = pool.load(context, R.raw.tarsier_bump, 1)
    private val finishSound = pool.load(context, R.raw.tarsier_finish, 1)

    fun hop() = play(hopSound, 0.45f)
    fun firefly() = play(fireflySound, 0.5f)
    fun bump() = play(bumpSound, 0.35f)
    fun finish() = play(finishSound, 0.55f)

    private fun play(soundId: Int, volume: Float) {
        pool.play(soundId, volume, volume, 1, 0, 1f)
    }

    override fun close() = pool.release()
}