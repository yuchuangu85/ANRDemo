package com.codemx.anrdemo.anr.dispatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepLinkParserTest {
    @Test
    fun parsesDeadlockDeepLink() {
        val request = DeepLinkParser.parse("anrdemo://scenario/deadlock?mode=contention&blockMs=8000")!!
        assertEquals("deadlock", request.scenarioId)
        assertEquals("contention", request.mode)
        assertEquals(8000L, request.blockMs)
    }

    @Test
    fun parsesMemoryPressureDeepLink() {
        val request = DeepLinkParser.parse("anrdemo://scenario/memory-pressure?maxMb=128&chunkMb=8&blockMs=8000&allowDangerousOom=false")!!
        assertEquals("memory-pressure", request.scenarioId)
        assertEquals(128, request.maxMb)
        assertEquals(8, request.chunkMb)
        assertEquals(8000L, request.blockMs)
        assertFalse(request.allowDangerousOom)
    }

    @Test
    fun parsesBooleanForeground() {
        val request = DeepLinkParser.parse("anrdemo://scenario/broadcast-foreground?foreground=true&adbConfirmed=true")!!
        assertTrue(request.foreground == true)
        assertTrue(request.adbConfirmed)
    }

    @Test
    fun deepLinksRequireExplicitAdbConfirmationFlag() {
        val unconfirmed = DeepLinkParser.parse("anrdemo://scenario/input-dispatch?blockMs=8000")!!
        val confirmed = DeepLinkParser.parse("anrdemo://scenario/input-dispatch?blockMs=8000&adbConfirmed=true")!!

        assertFalse(unconfirmed.adbConfirmed)
        assertTrue(confirmed.adbConfirmed)
    }

    @Test
    fun specializedScenarioIdsArePreservedForCorpusProvenance() {
        val classic = DeepLinkParser.parse("anrdemo://scenario/deadlock-classic?adbConfirmed=true")!!
        val dangerousOom = DeepLinkParser.parse("anrdemo://scenario/memory-dangerous-oom?adbConfirmed=true")!!
        val jobStop = DeepLinkParser.parse("anrdemo://scenario/job-service-stop?adbConfirmed=true")!!

        assertEquals("deadlock-classic", classic.scenarioId)
        assertEquals("classic", classic.mode)
        assertEquals("memory-dangerous-oom", dangerousOom.scenarioId)
        assertTrue(dangerousOom.allowDangerousOom)
        assertEquals("job-service-stop", jobStop.scenarioId)
        assertEquals("onStopJob", jobStop.mode)
    }

    @Test
    fun invalidDeepLinkReturnsNull() {
        assertNull(DeepLinkParser.parse("https://example.com/scenario/deadlock"))
        assertNull(DeepLinkParser.parse("anrdemo://scenario"))
    }
}
