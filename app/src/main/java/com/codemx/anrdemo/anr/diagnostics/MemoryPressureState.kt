package com.codemx.anrdemo.anr.diagnostics

data class MemoryPressureState(
    val retainedMemoryMb: Int,
    val heapUsedMb: Int,
    val heapMaxMb: Int,
)
