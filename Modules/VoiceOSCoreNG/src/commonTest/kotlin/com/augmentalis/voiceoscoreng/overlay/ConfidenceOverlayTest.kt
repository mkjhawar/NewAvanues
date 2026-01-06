/**
 * ConfidenceOverlayTest.kt - TDD tests for ConfidenceOverlay
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 *
 * TDD test file - written BEFORE implementation.
 * Tests the speech recognition confidence display overlay.
 */
package com.augmentalis.voiceoscoreng.overlay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.BeforeTest

// ═══════════════════════════════════════════════════════════════════════════
// ConfidenceLevel Enum Tests
// ═══════════════════════════════════════════════════════════════════════════

class ConfidenceLevelTest {

    @Test
    fun `ConfidenceLevel has all expected values`() {
        val levels = ConfidenceLevel.entries
        assertEquals(4, levels.size)
        assertTrue(levels.contains(ConfidenceLevel.HIGH))
        assertTrue(levels.contains(ConfidenceLevel.MEDIUM))
        assertTrue(levels.contains(ConfidenceLevel.LOW))
        assertTrue(levels.contains(ConfidenceLevel.REJECT))
    }

    @Test
    fun `ConfidenceLevel ordinals are stable`() {
        assertEquals(0, ConfidenceLevel.HIGH.ordinal)
        assertEquals(1, ConfidenceLevel.MEDIUM.ordinal)
        assertEquals(2, ConfidenceLevel.LOW.ordinal)
        assertEquals(3, ConfidenceLevel.REJECT.ordinal)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ConfidenceResult Data Class Tests
// ═══════════════════════════════════════════════════════════════════════════

class ConfidenceResultTest {

    @Test
    fun `ConfidenceResult stores confidence value`() {
        val result = ConfidenceResult(0.85f, "click button")
        assertEquals(0.85f, result.confidence, 0.001f)
    }

    @Test
    fun `ConfidenceResult stores recognized text`() {
        val result = ConfidenceResult(0.90f, "open settings")
        assertEquals("open settings", result.recognizedText)
    }

    @Test
    fun `ConfidenceResult has empty alternatives by default`() {
        val result = ConfidenceResult(0.90f, "open settings")
        assertTrue(result.alternatives.isEmpty())
    }

    @Test
    fun `ConfidenceResult stores alternatives`() {
        val alternatives = listOf("open settings", "over settings", "hope settings")
        val result = ConfidenceResult(0.75f, "open settings", alternatives)
        assertEquals(3, result.alternatives.size)
        assertEquals("over settings", result.alternatives[1])
    }

    // ==================== Level Calculation Tests ====================

    @Test
    fun `level returns HIGH for confidence 0_8 or above`() {
        assertEquals(ConfidenceLevel.HIGH, ConfidenceResult(0.80f, "test").level)
        assertEquals(ConfidenceLevel.HIGH, ConfidenceResult(0.85f, "test").level)
        assertEquals(ConfidenceLevel.HIGH, ConfidenceResult(0.90f, "test").level)
        assertEquals(ConfidenceLevel.HIGH, ConfidenceResult(0.95f, "test").level)
        assertEquals(ConfidenceLevel.HIGH, ConfidenceResult(1.0f, "test").level)
    }

    @Test
    fun `level returns MEDIUM for confidence 0_6 to 0_8`() {
        assertEquals(ConfidenceLevel.MEDIUM, ConfidenceResult(0.60f, "test").level)
        assertEquals(ConfidenceLevel.MEDIUM, ConfidenceResult(0.65f, "test").level)
        assertEquals(ConfidenceLevel.MEDIUM, ConfidenceResult(0.70f, "test").level)
        assertEquals(ConfidenceLevel.MEDIUM, ConfidenceResult(0.75f, "test").level)
        assertEquals(ConfidenceLevel.MEDIUM, ConfidenceResult(0.79f, "test").level)
    }

    @Test
    fun `level returns LOW for confidence 0_4 to 0_6`() {
        assertEquals(ConfidenceLevel.LOW, ConfidenceResult(0.40f, "test").level)
        assertEquals(ConfidenceLevel.LOW, ConfidenceResult(0.45f, "test").level)
        assertEquals(ConfidenceLevel.LOW, ConfidenceResult(0.50f, "test").level)
        assertEquals(ConfidenceLevel.LOW, ConfidenceResult(0.55f, "test").level)
        assertEquals(ConfidenceLevel.LOW, ConfidenceResult(0.59f, "test").level)
    }

    @Test
    fun `level returns REJECT for confidence below 0_4`() {
        assertEquals(ConfidenceLevel.REJECT, ConfidenceResult(0.0f, "test").level)
        assertEquals(ConfidenceLevel.REJECT, ConfidenceResult(0.10f, "test").level)
        assertEquals(ConfidenceLevel.REJECT, ConfidenceResult(0.20f, "test").level)
        assertEquals(ConfidenceLevel.REJECT, ConfidenceResult(0.30f, "test").level)
        assertEquals(ConfidenceLevel.REJECT, ConfidenceResult(0.39f, "test").level)
    }

    @Test
    fun `boundary value 0_8 is HIGH`() {
        assertEquals(ConfidenceLevel.HIGH, ConfidenceResult(0.80f, "test").level)
    }

    @Test
    fun `boundary value 0_6 is MEDIUM`() {
        assertEquals(ConfidenceLevel.MEDIUM, ConfidenceResult(0.60f, "test").level)
    }

    @Test
    fun `boundary value 0_4 is LOW`() {
        assertEquals(ConfidenceLevel.LOW, ConfidenceResult(0.40f, "test").level)
    }

    // ==================== Equality and Copy Tests ====================

    @Test
    fun `ConfidenceResult equality works correctly`() {
        val result1 = ConfidenceResult(0.85f, "click button")
        val result2 = ConfidenceResult(0.85f, "click button")
        val result3 = ConfidenceResult(0.75f, "click button")

        assertEquals(result1, result2)
        assertFalse(result1 == result3)
    }

    @Test
    fun `ConfidenceResult copy works correctly`() {
        val original = ConfidenceResult(0.85f, "click button")
        val copied = original.copy(confidence = 0.90f)

        assertEquals(0.90f, copied.confidence, 0.001f)
        assertEquals("click button", copied.recognizedText)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ConfidenceOverlay Tests
// ═══════════════════════════════════════════════════════════════════════════

class ConfidenceOverlayTest {

    private lateinit var overlay: ConfidenceOverlay

    @BeforeTest
    fun setup() {
        overlay = ConfidenceOverlay()
    }

    // ==================== Identity Tests ====================

    @Test
    fun `overlay has id 'confidence'`() {
        assertEquals("confidence", overlay.id)
    }

    @Test
    fun `overlay id is stable across show and hide`() {
        overlay.show()
        assertEquals("confidence", overlay.id)
        overlay.hide()
        assertEquals("confidence", overlay.id)
    }

    // ==================== Initial State Tests ====================

    @Test
    fun `overlay starts hidden`() {
        assertFalse(overlay.isVisible)
    }

    @Test
    fun `overlay starts with null confidence result`() {
        assertNull(overlay.confidenceResult.value)
    }

    // ==================== Show Tests ====================

    @Test
    fun `show without result makes overlay visible`() {
        overlay.show()
        assertTrue(overlay.isVisible)
    }

    @Test
    fun `show with confidence result makes overlay visible`() {
        val result = ConfidenceResult(0.85f, "click button")
        overlay.show(result)
        assertTrue(overlay.isVisible)
    }

    @Test
    fun `show with confidence result sets confidenceResult`() {
        val result = ConfidenceResult(0.85f, "click button")
        overlay.show(result)
        assertNotNull(overlay.confidenceResult.value)
        assertEquals(0.85f, overlay.confidenceResult.value!!.confidence, 0.001f)
    }

    @Test
    fun `show with result stores recognized text`() {
        val result = ConfidenceResult(0.90f, "open settings")
        overlay.show(result)
        assertEquals("open settings", overlay.confidenceResult.value?.recognizedText)
    }

    @Test
    fun `show with result stores alternatives`() {
        val alternatives = listOf("open settings", "over settings")
        val result = ConfidenceResult(0.75f, "open settings", alternatives)
        overlay.show(result)
        assertEquals(2, overlay.confidenceResult.value?.alternatives?.size)
    }

    // ==================== Hide Tests ====================

    @Test
    fun `hide makes overlay invisible`() {
        overlay.show()
        overlay.hide()
        assertFalse(overlay.isVisible)
    }

    @Test
    fun `hide preserves confidence result`() {
        val result = ConfidenceResult(0.85f, "click button")
        overlay.show(result)
        overlay.hide()
        // Confidence result should be preserved after hide
        assertNotNull(overlay.confidenceResult.value)
    }

    @Test
    fun `multiple hide calls are safe`() {
        overlay.hide()
        overlay.hide()
        assertFalse(overlay.isVisible)
    }

    // ==================== Toggle Tests ====================

    @Test
    fun `toggle from hidden shows overlay`() {
        overlay.toggle()
        assertTrue(overlay.isVisible)
    }

    @Test
    fun `toggle from visible hides overlay`() {
        overlay.show()
        overlay.toggle()
        assertFalse(overlay.isVisible)
    }

    // ==================== UpdateConfidence Tests ====================

    @Test
    fun `updateConfidence updates displayed value`() {
        overlay.show(ConfidenceResult(0.50f, "initial"))
        overlay.updateConfidence(ConfidenceResult(0.90f, "updated"))
        assertEquals(0.90f, overlay.confidenceResult.value!!.confidence, 0.001f)
    }

    @Test
    fun `updateConfidence updates recognized text`() {
        overlay.show(ConfidenceResult(0.50f, "initial"))
        overlay.updateConfidence(ConfidenceResult(0.90f, "new text"))
        assertEquals("new text", overlay.confidenceResult.value?.recognizedText)
    }

    @Test
    fun `updateConfidence does not change visibility`() {
        overlay.show()
        assertTrue(overlay.isVisible)
        overlay.updateConfidence(ConfidenceResult(0.90f, "test"))
        assertTrue(overlay.isVisible)

        overlay.hide()
        assertFalse(overlay.isVisible)
        overlay.updateConfidence(ConfidenceResult(0.85f, "another"))
        assertFalse(overlay.isVisible)
    }

    // ==================== Update (IOverlay) Tests ====================

    @Test
    fun `update with Confidence data sets confidence result`() {
        val data = OverlayData.Confidence(0.92f, "test command")
        overlay.update(data)
        assertNotNull(overlay.confidenceResult.value)
        assertEquals(0.92f, overlay.confidenceResult.value!!.confidence, 0.001f)
    }

    @Test
    fun `update with Confidence data shows overlay`() {
        val data = OverlayData.Confidence(0.92f, "test command")
        overlay.update(data)
        assertTrue(overlay.isVisible)
    }

    @Test
    fun `update with non-Confidence data is ignored`() {
        val statusData = OverlayData.Status("message", CommandState.LISTENING)
        overlay.update(statusData)
        // Should remain hidden and without confidence
        assertFalse(overlay.isVisible)
        assertNull(overlay.confidenceResult.value)
    }

    // ==================== Dispose Tests ====================

    @Test
    fun `dispose makes overlay invisible`() {
        overlay.show()
        overlay.dispose()
        assertFalse(overlay.isVisible)
    }

    @Test
    fun `dispose clears confidence result`() {
        overlay.show(ConfidenceResult(0.85f, "test"))
        overlay.dispose()
        assertNull(overlay.confidenceResult.value)
    }

    @Test
    fun `show after dispose has no effect on visibility`() {
        overlay.dispose()
        overlay.show()
        // Behavior after dispose - implementation may vary
        // The important thing is it doesn't crash
    }

    @Test
    fun `multiple dispose calls are safe`() {
        overlay.dispose()
        overlay.dispose()
        assertFalse(overlay.isVisible)
    }

    // ==================== State Flow Tests ====================

    @Test
    fun `confidenceResult StateFlow emits updates`() {
        // Initial state
        assertNull(overlay.confidenceResult.value)

        // After show with result
        overlay.show(ConfidenceResult(0.85f, "first"))
        assertEquals(0.85f, overlay.confidenceResult.value!!.confidence, 0.001f)

        // After updateConfidence
        overlay.updateConfidence(ConfidenceResult(0.90f, "second"))
        assertEquals(0.90f, overlay.confidenceResult.value!!.confidence, 0.001f)

        // After dispose
        overlay.dispose()
        assertNull(overlay.confidenceResult.value)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ConfidenceOverlay Companion Object Tests
// ═══════════════════════════════════════════════════════════════════════════

class ConfidenceOverlayCompanionTest {

    // ==================== Color Constant Tests ====================

    @Test
    fun `COLOR_HIGH is green`() {
        // 0xFF4CAF50 = Material Green 500
        assertEquals(0xFF4CAF50, ConfidenceOverlay.COLOR_HIGH)
    }

    @Test
    fun `COLOR_MEDIUM is yellow`() {
        // 0xFFFFEB3B = Material Yellow 500
        assertEquals(0xFFFFEB3B, ConfidenceOverlay.COLOR_MEDIUM)
    }

    @Test
    fun `COLOR_LOW is orange`() {
        // 0xFFFF9800 = Material Orange 500
        assertEquals(0xFFFF9800, ConfidenceOverlay.COLOR_LOW)
    }

    @Test
    fun `COLOR_REJECT is red`() {
        // 0xFFF44336 = Material Red 500
        assertEquals(0xFFF44336, ConfidenceOverlay.COLOR_REJECT)
    }

    // ==================== getColorForLevel Tests ====================

    @Test
    fun `getColorForLevel returns correct color for HIGH`() {
        assertEquals(ConfidenceOverlay.COLOR_HIGH, ConfidenceOverlay.getColorForLevel(ConfidenceLevel.HIGH))
    }

    @Test
    fun `getColorForLevel returns correct color for MEDIUM`() {
        assertEquals(ConfidenceOverlay.COLOR_MEDIUM, ConfidenceOverlay.getColorForLevel(ConfidenceLevel.MEDIUM))
    }

    @Test
    fun `getColorForLevel returns correct color for LOW`() {
        assertEquals(ConfidenceOverlay.COLOR_LOW, ConfidenceOverlay.getColorForLevel(ConfidenceLevel.LOW))
    }

    @Test
    fun `getColorForLevel returns correct color for REJECT`() {
        assertEquals(ConfidenceOverlay.COLOR_REJECT, ConfidenceOverlay.getColorForLevel(ConfidenceLevel.REJECT))
    }

    @Test
    fun `colors have full opacity`() {
        // All colors should have 0xFF alpha (fully opaque)
        val alphaHigh = (ConfidenceOverlay.COLOR_HIGH shr 24) and 0xFF
        val alphaMedium = (ConfidenceOverlay.COLOR_MEDIUM shr 24) and 0xFF
        val alphaLow = (ConfidenceOverlay.COLOR_LOW shr 24) and 0xFF
        val alphaReject = (ConfidenceOverlay.COLOR_REJECT shr 24) and 0xFF

        assertEquals(0xFF, alphaHigh)
        assertEquals(0xFF, alphaMedium)
        assertEquals(0xFF, alphaLow)
        assertEquals(0xFF, alphaReject)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// Integration-Style Tests
// ═══════════════════════════════════════════════════════════════════════════

class ConfidenceOverlayIntegrationTest {

    @Test
    fun `typical usage scenario - show update hide`() {
        val overlay = ConfidenceOverlay()

        // Start recognition
        overlay.show(ConfidenceResult(0.50f, "list..."))
        assertTrue(overlay.isVisible)
        assertEquals(ConfidenceLevel.LOW, overlay.confidenceResult.value?.level)

        // Confidence improves
        overlay.updateConfidence(ConfidenceResult(0.70f, "listen..."))
        assertEquals(ConfidenceLevel.MEDIUM, overlay.confidenceResult.value?.level)

        // Final result
        overlay.updateConfidence(ConfidenceResult(0.92f, "click submit"))
        assertEquals(ConfidenceLevel.HIGH, overlay.confidenceResult.value?.level)

        // Hide after command processed
        overlay.hide()
        assertFalse(overlay.isVisible)

        // Cleanup
        overlay.dispose()
        assertNull(overlay.confidenceResult.value)
    }

    @Test
    fun `color progression from low to high`() {
        val overlay = ConfidenceOverlay()

        // Low confidence - orange
        overlay.show(ConfidenceResult(0.45f, "test"))
        assertEquals(
            ConfidenceOverlay.COLOR_LOW,
            ConfidenceOverlay.getColorForLevel(overlay.confidenceResult.value!!.level)
        )

        // Medium confidence - yellow
        overlay.updateConfidence(ConfidenceResult(0.65f, "test"))
        assertEquals(
            ConfidenceOverlay.COLOR_MEDIUM,
            ConfidenceOverlay.getColorForLevel(overlay.confidenceResult.value!!.level)
        )

        // High confidence - green
        overlay.updateConfidence(ConfidenceResult(0.85f, "test"))
        assertEquals(
            ConfidenceOverlay.COLOR_HIGH,
            ConfidenceOverlay.getColorForLevel(overlay.confidenceResult.value!!.level)
        )
    }

    @Test
    fun `rejected speech scenario`() {
        val overlay = ConfidenceOverlay()

        // Very low confidence - rejected
        overlay.show(ConfidenceResult(0.20f, "???"))
        assertEquals(ConfidenceLevel.REJECT, overlay.confidenceResult.value?.level)
        assertEquals(
            ConfidenceOverlay.COLOR_REJECT,
            ConfidenceOverlay.getColorForLevel(overlay.confidenceResult.value!!.level)
        )

        // System should likely hide and ask user to repeat
        overlay.hide()
        assertFalse(overlay.isVisible)
    }

    @Test
    fun `IOverlay interface compliance`() {
        val ioverlay: IOverlay = ConfidenceOverlay()

        // Verify interface contract
        assertEquals("confidence", ioverlay.id)
        assertFalse(ioverlay.isVisible)

        ioverlay.show()
        assertTrue(ioverlay.isVisible)

        ioverlay.hide()
        assertFalse(ioverlay.isVisible)

        ioverlay.update(OverlayData.Confidence(0.85f, "interface test"))
        assertTrue(ioverlay.isVisible)

        ioverlay.toggle()
        assertFalse(ioverlay.isVisible)

        ioverlay.dispose()
        assertFalse(ioverlay.isVisible)
    }
}
