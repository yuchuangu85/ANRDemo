package com.codemx.anrdemo.anr.safety

import com.codemx.anrdemo.anr.catalog.AnrCatalog
import com.codemx.anrdemo.anr.catalog.ConfirmationRequirement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyPolicyTest {
    @Test
    fun dangerousScenariosNeedPhrase() {
        listOf("deadlock-classic", "memory-dangerous-oom", "service-background", "short-service").forEach { id ->
            val scenario = AnrCatalog.requireScenario(id)
            assertEquals(ConfirmationRequirement.TypePhraseConfirm, SafetyPolicy.requirementFor(scenario, scenario.defaultRequest))
            assertNotNull("$id phrase", SafetyPolicy.phraseFor(scenario, scenario.defaultRequest))
        }
    }

    @Test
    fun documentationOnlyCannotDispatch() {
        val scenario = AnrCatalog.requireScenario("no-focused-window")
        assertEquals(ConfirmationRequirement.DisabledDocumentation, SafetyPolicy.requirementFor(scenario, scenario.defaultRequest))
    }

    @Test
    fun dangerousRequestsRequireToken() {
        val scenario = AnrCatalog.requireScenario("memory-dangerous-oom")
        assertTrue(SafetyPolicy.requiresToken(scenario, scenario.defaultRequest))
    }
}
