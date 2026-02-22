package com.augmentalis.actions

/**
 * NOTE: CategoryCapabilityRegistry, IntentRouter, and RoutingDecision all live in
 * androidMain and import android.util.Log + javax.inject — they cannot compile in
 * commonTest. ActionResult also lives in androidMain. Only pure commonMain code is
 * testable here.
 *
 * The Actions module currently has no pure-Kotlin commonMain source files beyond its
 * coroutines dependency. Tests for the Android-specific classes exist in:
 *   Modules/Actions/src/androidUnitTest/kotlin/com/augmentalis/actions/ActionResultTest.kt
 *   Modules/Actions/src/androidUnitTest/kotlin/com/augmentalis/actions/ActionsManagerTest.kt
 *   etc.
 *
 * This file is intentionally left with a passing smoke test to confirm the commonTest
 * source set compiles, pending any future move of pure logic to commonMain.
 */
import kotlin.test.Test
import kotlin.test.assertTrue

class ActionsCommonSmokeTest {

    /**
     * Placeholder that confirms the commonTest source set compiles cleanly.
     * Real business logic tests live in androidUnitTest.
     */
    @Test
    fun commonTestSourceSetCompiles() {
        // This test verifies that the commonTest gradle source set is configured correctly.
        // If this file compiles and this test passes, the source set is wired up.
        assertTrue(true)
    }
}
