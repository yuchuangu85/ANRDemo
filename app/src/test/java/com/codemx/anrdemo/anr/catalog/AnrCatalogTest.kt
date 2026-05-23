package com.codemx.anrdemo.anr.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnrCatalogTest {
    @Test
    fun scenarioIdsAreUnique() {
        val ids = AnrCatalog.scenarios.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun allScenariosHaveRequiredMetadata() {
        AnrCatalog.scenarios.forEach { scenario ->
            assertTrue("${scenario.id} title", scenario.title.isNotBlank())
            assertTrue("${scenario.id} timeout", scenario.timeoutDescription.isNotBlank())
            assertTrue("${scenario.id} expectedReason", scenario.expectedReason.isNotBlank())
            assertTrue("${scenario.id} explanation", scenario.explanation.isNotBlank())
            assertNotNull("${scenario.id} risk", scenario.riskLevel)
        }
    }

    @Test
    fun deadlockScenarioDocumentsRootCause() {
        val scenario = AnrCatalog.requireScenario("deadlock")
        assertEquals(AnrTriggerKind.Deadlock, scenario.triggerKind)
        assertTrue(scenario.explanation.contains("根因"))
        assertTrue(scenario.expectedReason.contains("Input dispatching timed out"))
    }

    @Test
    fun memoryPressureScenarioIsGuardedByDefault() {
        val scenario = AnrCatalog.requireScenario("memory-pressure")
        assertEquals(AnrTriggerKind.MemoryPressure, scenario.triggerKind)
        assertFalse(scenario.defaultRequest.allowDangerousOom)
        assertNotNull(scenario.defaultRequest.maxMb)
        assertNotNull(scenario.defaultRequest.chunkMb)
    }

    @Test
    fun enabledUiScenariosHaveRecoveryCommand() {
        AnrCatalog.scenarios.filter { it.enabledByDefault }.forEach { scenario ->
            assertTrue("${scenario.id} recovery", scenario.recoveryCommand.contains("force-stop"))
        }
    }
}
