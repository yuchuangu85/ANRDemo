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

    @Test
    fun adbCommandsUseConfirmedDeepLinksInsteadOfExportedBroadcasts() {
        AnrCatalog.scenarios.mapNotNull { it.adbCommand }.forEach { command ->
            assertFalse(command, command.contains("am broadcast"))
            assertTrue(command, command.contains("adbConfirmed=true"))
        }
    }

    @Test
    fun jobServiceStopKeepsDistinctScenarioIdentityAndMode() {
        val scenario = AnrCatalog.requireScenario("job-service-stop")
        assertEquals("job-service-stop", scenario.defaultRequest.scenarioId)
        assertEquals("onStopJob", scenario.defaultRequest.mode)
        assertTrue(scenario.adbCommand.orEmpty().contains("job-service-stop"))
        assertTrue(scenario.adbCommand.orEmpty().contains("mode=onStopJob"))
    }

    @Test
    fun binderPeerScenarioDocumentsRemoteSynchronousWait() {
        val scenario = AnrCatalog.requireScenario("binder-peer-stall")
        assertEquals(AnrTriggerKind.BinderPeer, scenario.triggerKind)
        assertEquals(AnrDefaults.BINDER_PEER_BLOCK_MS, scenario.defaultRequest.blockMs)
        assertTrue(scenario.explanation.contains("独立进程"))
        assertTrue(scenario.expectedReason.contains("BinderProxy.transactNative"))
        assertTrue(scenario.adbCommand.orEmpty().contains("binder-peer-stall"))
    }
}
