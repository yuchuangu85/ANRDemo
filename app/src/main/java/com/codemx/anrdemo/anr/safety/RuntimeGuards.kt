package com.codemx.anrdemo.anr.safety

object RuntimeGuards {
    fun requirePositiveDuration(blockMs: Long?) {
        require((blockMs ?: 1L) > 0L) { "blockMs must be positive" }
    }
}
