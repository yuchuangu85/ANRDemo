package com.codemx.anrdemo.anr.safety

data class ResolvedMemoryBudget(
    val maxMb: Int,
    val chunkMb: Int,
)

object MemoryBudget {
    private const val MB = 1024 * 1024L

    fun resolve(
        requestedMaxMb: Int,
        chunkMb: Int,
        allowDangerousOom: Boolean,
        runtime: Runtime = Runtime.getRuntime(),
    ): ResolvedMemoryBudget {
        require(chunkMb > 0) { "chunkMb must be positive" }
        require(requestedMaxMb > 0) { "requestedMaxMb must be positive" }
        if (allowDangerousOom) {
            return ResolvedMemoryBudget(requestedMaxMb, chunkMb.coerceAtMost(requestedMaxMb))
        }
        val maxHeapMb = (runtime.maxMemory() / MB).toInt().coerceAtLeast(32)
        val safeMaxMb = minOf(requestedMaxMb, maxHeapMb / 4, 256).coerceAtLeast(1)
        return ResolvedMemoryBudget(
            maxMb = safeMaxMb,
            chunkMb = chunkMb.coerceAtMost(safeMaxMb).coerceAtLeast(1),
        )
    }
}
