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
        val request = DeepLinkParser.parse("anrdemo://scenario/broadcast-foreground?foreground=true")!!
        assertTrue(request.foreground == true)
    }

    @Test
    fun invalidDeepLinkReturnsNull() {
        assertNull(DeepLinkParser.parse("https://example.com/scenario/deadlock"))
        assertNull(DeepLinkParser.parse("anrdemo://scenario"))
    }
}
