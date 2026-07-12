package com.maxinesworld.featurerewards

import android.content.Context
import com.maxinesworld.coremodel.CollectibleBadge
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import kotlinx.serialization.json.*

@Serializable
private data class BadgeJson(
    val id: String, val biome: String, val name: String,
    val title: String, val fun_fact: String, val emoji: String
)

class BadgeLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var cache: List<CollectibleBadge>? = null

    suspend fun loadAll(): List<CollectibleBadge> {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            val raw = context.assets.open("badge_catalog.json").bufferedReader().use { it.readText() }
            val badges = json.decodeFromString<List<BadgeJson>>(raw)
            badges.map { b ->
                CollectibleBadge(
                    id = b.id, biome = b.biome, name = b.name,
                    title = b.title, funFact = b.fun_fact, emoji = b.emoji
                )
            }.also { cache = it }
        }
    }

    fun getByBiome(all: List<CollectibleBadge>, biomeId: String): List<CollectibleBadge> =
        all.filter { it.biome == biomeId }
}
