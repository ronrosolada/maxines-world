package com.maxinesworld.gamecatcafe

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class CafeSoundPlayer(context: Context) : AutoCloseable {
    private val pool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build())
        .build()
    private val tapId = pool.load(context, R.raw.cafe_tap, 1)
    private val correctId = pool.load(context, R.raw.cafe_correct, 1)
    private val retryId = pool.load(context, R.raw.cafe_retry, 1)
    private val endId = pool.load(context, R.raw.cafe_end, 1)
    fun tap() = play(tapId, .45f)
    fun correct() = play(correctId, .65f)
    fun retry() = play(retryId, .50f)
    fun end() = play(endId, .60f)
    private fun play(id: Int, volume: Float) { pool.play(id, volume, volume, 1, 0, 1f) }
    override fun close() = pool.release()
}
