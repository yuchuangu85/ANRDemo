package com.codemx.anrdemo.anr.safety

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryBudgetTest {
    @Test
    fun safeBudgetIsCappedToQuarterHeapAnd256Mb() {
        val budget = MemoryBudget.resolve(
            requestedMaxMb = 1024,
            chunkMb = 64,
            allowDangerousOom = false,
            runtime = Runtime.getRuntime(),
        )
        assertTrue(budget.maxMb <= 256)
        assertTrue(budget.chunkMb <= budget.maxMb)
    }

    @Test
    fun dangerousOomKeepsRequestedBudget() {
        val budget = MemoryBudget.resolve(
            requestedMaxMb = 512,
            chunkMb = 32,
            allowDangerousOom = true,
            runtime = Runtime.getRuntime(),
        )
        assertEquals(512, budget.maxMb)
        assertEquals(32, budget.chunkMb)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidChunk() {
        MemoryBudget.resolve(128, 0, false)
    }
}
