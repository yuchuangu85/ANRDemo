package com.codemx.anrdemo.anr.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenarioRequestBuilderTest {
    @Test
    fun mergesDefaultsAndOverrides() {
        val scenario = AnrCatalog.requireScenario("memory-pressure")
        val request = ScenarioRequestBuilder.build(scenario, mapOf("maxMb" to "64", "chunkMb" to "4"))
        assertEquals("memory-pressure", request.scenarioId)
        assertEquals(64, request.maxMb)
        assertEquals(4, request.chunkMb)
        assertEquals(8_000L, request.blockMs)
    }

    @Test
    fun clampsDurationToSpecRange() {
        val scenario = AnrCatalog.requireScenario("input-dispatch")
        val request = ScenarioRequestBuilder.build(scenario, mapOf("blockMs" to "999999"))
        assertEquals(20_000L, request.blockMs)
    }

    @Test
    fun dangerousOomKeepsDangerousFlag() {
        val scenario = AnrCatalog.requireScenario("memory-dangerous-oom")
        val request = ScenarioRequestBuilder.build(scenario, emptyMap())
        assertTrue(request.allowDangerousOom)
    }
}
