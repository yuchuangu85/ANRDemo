package com.codemx.anrdemo.anr.diagnostics

import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsRepositoryTest {
    @Test
    fun memorySnapshotModelCarriesValues() {
        val snapshot = DiagnosticsSnapshot(memory = MemoryPressureState(retainedMemoryMb = 1, heapUsedMb = 2, heapMaxMb = 3))
        assertTrue(snapshot.memory.heapMaxMb >= snapshot.memory.heapUsedMb)
    }
}
