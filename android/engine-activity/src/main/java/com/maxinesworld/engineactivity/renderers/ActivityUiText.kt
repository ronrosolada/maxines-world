package com.maxinesworld.engineactivity.renderers

/** Chrome language for Filipino, Makabansa, and GMRC lessons. */
internal fun activityUiText(language: String?, english: String, filipino: String): String =
    if (language?.trim()?.lowercase() in setOf("fil", "fil-ph", "filipino", "makabansa", "gmrc")) filipino else english
