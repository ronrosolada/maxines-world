package com.maxinesworld.featurelessonplayer

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C

/** Shared Media3 policy: request focus and pause automatically on focus loss. */
internal fun mediaAudioAttributes(): AudioAttributes = AudioAttributes.Builder()
    .setUsage(C.USAGE_MEDIA)
    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
    .build()
