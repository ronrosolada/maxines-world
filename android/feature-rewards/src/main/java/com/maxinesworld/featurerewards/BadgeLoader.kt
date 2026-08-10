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
    val title: String, val fun_fact: String
)

@Serializable
private data class BadgePhotoJson(
    val badge_id: String,
    val asset: String,
    val provider: String? = null,
    val source_url: String? = null,
    val source_title: String? = null,
    val credit: String? = null,
    val license: String? = null,
    val kind: String? = null,
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
            val photosByBadgeId = loadPhotoMetadata()
            badges.map { b ->
                val photo = photosByBadgeId[b.id]
                CollectibleBadge(
                    id = b.id, biome = b.biome, name = b.name,
                    title = b.title, funFact = b.fun_fact,
                    photoAsset = photo?.asset,
                    photoCredit = photo?.credit,
                    photoSourceUrl = photo?.source_url,
                    photoLicense = photo?.license,
                    photoKind = photo?.kind,
                )
            }.also { cache = it }
        }
    }

    private fun loadPhotoMetadata(): Map<String, BadgePhotoJson> = try {
        val raw = context.assets.open("badge_photos.json").bufferedReader().use { it.readText() }
        json.decodeFromString<List<BadgePhotoJson>>(raw).associateBy { it.badge_id }
    } catch (_: Exception) {
        // Keep catalog loading backward-compatible for tests, old installs, and
        // milestone-only builds that do not ship the optional photo manifest.
        emptyMap()
    }

    fun getByBiome(all: List<CollectibleBadge>, biomeId: String): List<CollectibleBadge> =
        all.filter { it.biome == biomeId }
}
