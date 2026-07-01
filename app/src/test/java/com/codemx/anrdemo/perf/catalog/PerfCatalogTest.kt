package com.codemx.anrdemo.perf.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PerfCatalogTest {
    @Test
    fun scenarioIdsAreUnique() {
        val ids = PerfCatalog.scenarios.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun allScenariosHaveRequiredMetadata() {
        PerfCatalog.scenarios.forEach { scenario ->
            assertTrue("${scenario.id} title", scenario.title.isNotBlank())
            assertTrue("${scenario.id} description", scenario.description.isNotBlank())
            assertTrue("${scenario.id} howToObserve", scenario.howToObserve.isNotBlank())
            assertTrue("${scenario.id} expectedSymptom", scenario.expectedSymptom.isNotBlank())
            assertEquals("${scenario.id} defaultRequest id", scenario.id, scenario.defaultRequest.scenarioId)
        }
    }

    @Test
    fun loadScenariosHaveAutoStopGuard() {
        PerfCatalog.scenarios.filter { it.isLoad }.forEach { scenario ->
            assertNotNull("${scenario.id} 需要 autoStopMs 护栏", scenario.autoStopMs)
            assertTrue("${scenario.id} autoStopMs > 0", (scenario.autoStopMs ?: 0) > 0)
        }
    }

    @Test
    fun coreThreeCategoriesArePresent() {
        val categories = PerfCatalog.scenarios.map { it.category }.toSet()
        assertTrue(categories.contains(PerfCategory.Overdraw))
        assertTrue(categories.contains(PerfCategory.Jank))
        assertTrue(categories.contains(PerfCategory.FrameDrop))
    }

    @Test
    fun requestBuilderClampsOutOfRangeValues() {
        val scenario = PerfCatalog.requireScenario("jank-periodic-block")
        val request = PerfRequestBuilder.build(scenario, mapOf("blockMs" to "999999"))
        // blockMs spec max = 700
        assertEquals(700L, request.blockMs)
    }

    @Test
    fun requestBuilderUsesDefaultsWhenNoOverride() {
        val scenario = PerfCatalog.requireScenario("overdraw-layers")
        val request = PerfRequestBuilder.build(scenario, emptyMap())
        assertEquals(scenario.defaultRequest.layerCount, request.layerCount)
    }
}
