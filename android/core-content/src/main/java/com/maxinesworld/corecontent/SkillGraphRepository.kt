package com.maxinesworld.corecontent

import android.content.Context
import com.maxinesworld.coremodel.SkillGraph
import com.maxinesworld.coremodel.SkillNode
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Singleton
class SkillGraphRepository internal constructor(
    private val source: () -> String,
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this({
        context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
    })

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    @Volatile
    private var cachedGraph: SkillGraph? = null

    fun getGraph(): SkillGraph = cachedGraph ?: synchronized(this) {
        cachedGraph ?: json.decodeFromString<SkillGraph>(source()).also { graph ->
            require(graph.nodes.map { it.id }.distinct().size == graph.nodes.size) {
                "Skill graph contains duplicate skill IDs"
            }
            cachedGraph = graph
        }
    }

    fun getSkill(skillId: String): SkillNode? = getGraph().nodes.firstOrNull { it.id == skillId }

    fun getSkillsBySubject(subjectId: String): List<SkillNode> =
        getGraph().nodes.filter { it.subjectId.equals(subjectId, ignoreCase = true) }

    fun getPrerequisiteIds(skillId: String): List<String> = getSkill(skillId)?.prerequisites.orEmpty()

    fun getPrerequisites(skillId: String): List<SkillNode> =
        getPrerequisiteIds(skillId).mapNotNull(::getSkill)

    fun getRemediationSkill(skillId: String): SkillNode? =
        getSkill(skillId)?.remediationSkillId?.let(::getSkill)

    private companion object {
        const val ASSET_PATH = "content-pack/skill-graph.json"
    }
}
