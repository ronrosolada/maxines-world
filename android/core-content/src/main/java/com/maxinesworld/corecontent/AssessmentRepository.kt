package com.maxinesworld.corecontent

import android.content.Context
import android.util.Log
import com.maxinesworld.coremodel.AssessmentCatalog
import com.maxinesworld.coremodel.AssessmentPack
import com.maxinesworld.coremodel.AssessmentPackMetadata
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssessmentRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val json: Json = Json { 
        ignoreUnknownKeys = true 
        isLenient = true
    }
    private var cachedCatalog: AssessmentCatalog? = null

    suspend fun getCatalog(): AssessmentCatalog = withContext(Dispatchers.IO) {
        cachedCatalog?.let { return@withContext it }
        try {
            val raw = context.assets.open("assessment-packs/catalog.json").bufferedReader().use { it.readText() }
            Log.d("AssessmentRepo", "Loaded catalog JSON raw length: ${raw.length}")
            val catalog = json.decodeFromString<AssessmentCatalog>(raw)
            Log.d("AssessmentRepo", "Decoded packs count: ${catalog.packs.size}")
            cachedCatalog = catalog
            catalog
        } catch (e: Exception) {
            Log.e("AssessmentRepo", "Error decoding assessment catalog: ${e.message}", e)
            AssessmentCatalog()
        }
    }

    suspend fun getPack(packId: String): AssessmentPack? = withContext(Dispatchers.IO) {
        try {
            val catalog = getCatalog()
            val meta = catalog.packs.firstOrNull { it.id == packId } ?: return@withContext null
            val raw = context.assets.open(meta.file).bufferedReader().use { it.readText() }
            json.decodeFromString<AssessmentPack>(raw)
        } catch (e: Exception) {
            Log.e("AssessmentRepo", "Error decoding pack $packId: ${e.message}", e)
            null
        }
    }

    suspend fun getPacksBySubject(subjectId: String): List<AssessmentPackMetadata> {
        return getCatalog().packs.filter { it.subjectId.equals(subjectId, ignoreCase = true) }
    }
}
