package com.codemx.anrdemo.anr.safety

import com.codemx.anrdemo.anr.catalog.ConfirmationRequirement

data class ConfirmationToken(
    val scenarioId: String,
    val issuedAtElapsedMs: Long,
    val requirement: ConfirmationRequirement,
)
