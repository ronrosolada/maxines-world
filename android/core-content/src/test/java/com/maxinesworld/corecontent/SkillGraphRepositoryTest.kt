package com.maxinesworld.corecontent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.io.File

class SkillGraphRepositoryTest {
    private fun graphFile(): File = listOf(
        File("../app/src/main/assets/content-pack/skill-graph.json"),
        File("app/src/main/assets/content-pack/skill-graph.json"),
        File("android/app/src/main/assets/content-pack/skill-graph.json"),
    ).first { it.isFile }

    @Test
    fun `loads all nodes and supports graph lookups`() {
        var loads = 0
        val repository = SkillGraphRepository {
            loads++
            graphFile().readText()
        }

        val graph = repository.getGraph()
        assertEquals(168, graph.nodes.size)
        val node = repository.getSkill("math-g2-place-value")
        assertNotNull(node)
        node!!
        assertEquals("mathematics", node.subjectId)
        assertEquals(node.prerequisites, repository.getPrerequisiteIds(node.id))
        assertEquals(node.remediationSkillId, repository.getRemediationSkill(node.id)?.id)
        assertEquals(28, repository.getSkillsBySubject("mathematics").size)
        assertSame(graph, repository.getGraph())
        assertEquals(1, loads)
    }
}
