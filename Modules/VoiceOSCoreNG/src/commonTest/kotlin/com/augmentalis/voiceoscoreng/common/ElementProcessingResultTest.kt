package com.augmentalis.voiceoscoreng.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ElementProcessingResultTest {

    // ==================== Success Tests ====================

    @Test
    fun `Success can be created with all fields`() {
        val result = ElementProcessingResult.Success(
            vuid = "a3f2e1-b917cc9dc",
            commandsGenerated = 3,
            processingTimeMs = 50L
        )

        assertEquals("a3f2e1-b917cc9dc", result.vuid)
        assertEquals(3, result.commandsGenerated)
        assertEquals(50L, result.processingTimeMs)
    }

    @Test
    fun `Success isSuccess returns true`() {
        val result = ElementProcessingResult.success("test-vuid")
        assertTrue(result.isSuccess())
    }

    @Test
    fun `Success isFailure returns false`() {
        val result = ElementProcessingResult.success("test-vuid")
        assertFalse(result.isFailure())
    }

    @Test
    fun `Success isSkipped returns false`() {
        val result = ElementProcessingResult.success("test-vuid")
        assertFalse(result.isSkipped())
    }

    @Test
    fun `Success getVuidOrNull returns vuid`() {
        val result = ElementProcessingResult.success("a3f2e1-b917cc9dc")
        assertEquals("a3f2e1-b917cc9dc", result.getVuidOrNull())
    }

    // ==================== Failure Tests ====================

    @Test
    fun `Failure can be created with error message`() {
        val result = ElementProcessingResult.Failure(
            error = "Element not found"
        )

        assertEquals("Element not found", result.error)
        assertNull(result.elementInfo)
    }

    @Test
    fun `Failure can be created with element info`() {
        val element = ElementInfo(className = "Button", text = "Submit")
        val result = ElementProcessingResult.Failure(
            error = "Processing failed",
            elementInfo = element
        )

        assertEquals("Processing failed", result.error)
        assertNotNull(result.elementInfo)
        assertEquals("Button", result.elementInfo?.className)
    }

    @Test
    fun `Failure isFailure returns true`() {
        val result = ElementProcessingResult.failure("error")
        assertTrue(result.isFailure())
    }

    @Test
    fun `Failure isSuccess returns false`() {
        val result = ElementProcessingResult.failure("error")
        assertFalse(result.isSuccess())
    }

    @Test
    fun `Failure getVuidOrNull returns null`() {
        val result = ElementProcessingResult.failure("error")
        assertNull(result.getVuidOrNull())
    }

    @Test
    fun `Failure getErrorOrNull returns error`() {
        val result = ElementProcessingResult.failure("Something went wrong")
        assertEquals("Something went wrong", result.getErrorOrNull())
    }

    // ==================== Skipped Tests ====================

    @Test
    fun `Skipped can be created with reason`() {
        val result = ElementProcessingResult.Skipped(
            reason = "Element not actionable"
        )

        assertEquals("Element not actionable", result.reason)
    }

    @Test
    fun `Skipped isSkipped returns true`() {
        val result = ElementProcessingResult.skipped("filtered")
        assertTrue(result.isSkipped())
    }

    @Test
    fun `Skipped isSuccess returns false`() {
        val result = ElementProcessingResult.skipped("filtered")
        assertFalse(result.isSuccess())
    }

    @Test
    fun `Skipped isFailure returns false`() {
        val result = ElementProcessingResult.skipped("filtered")
        assertFalse(result.isFailure())
    }

    @Test
    fun `Skipped getVuidOrNull returns null`() {
        val result = ElementProcessingResult.skipped("filtered")
        assertNull(result.getVuidOrNull())
    }

    @Test
    fun `Skipped getSkipReasonOrNull returns reason`() {
        val result = ElementProcessingResult.skipped("Not visible")
        assertEquals("Not visible", result.getSkipReasonOrNull())
    }

    // ==================== Fold Tests ====================

    @Test
    fun `fold calls onSuccess for Success result`() {
        val result = ElementProcessingResult.success("vuid-123", 2, 100L)

        val message = result.fold(
            onSuccess = { "Success: ${it.vuid}" },
            onFailure = { "Failure: ${it.error}" },
            onSkipped = { "Skipped: ${it.reason}" }
        )

        assertEquals("Success: vuid-123", message)
    }

    @Test
    fun `fold calls onFailure for Failure result`() {
        val result = ElementProcessingResult.failure("Network error")

        val message = result.fold(
            onSuccess = { "Success: ${it.vuid}" },
            onFailure = { "Failure: ${it.error}" },
            onSkipped = { "Skipped: ${it.reason}" }
        )

        assertEquals("Failure: Network error", message)
    }

    @Test
    fun `fold calls onSkipped for Skipped result`() {
        val result = ElementProcessingResult.skipped("Duplicate element")

        val message = result.fold(
            onSuccess = { "Success: ${it.vuid}" },
            onFailure = { "Failure: ${it.error}" },
            onSkipped = { "Skipped: ${it.reason}" }
        )

        assertEquals("Skipped: Duplicate element", message)
    }

    // ==================== Callback Tests ====================

    @Test
    fun `onSuccess executes action for Success`() {
        var captured = ""
        val result = ElementProcessingResult.success("test-vuid")

        result.onSuccess { captured = it.vuid }

        assertEquals("test-vuid", captured)
    }

    @Test
    fun `onSuccess does not execute for Failure`() {
        var captured = "unchanged"
        val result = ElementProcessingResult.failure("error")

        result.onSuccess { captured = it.vuid }

        assertEquals("unchanged", captured)
    }

    @Test
    fun `onFailure executes action for Failure`() {
        var captured = ""
        val result = ElementProcessingResult.failure("test error")

        result.onFailure { captured = it.error }

        assertEquals("test error", captured)
    }

    @Test
    fun `onSkipped executes action for Skipped`() {
        var captured = ""
        val result = ElementProcessingResult.skipped("test reason")

        result.onSkipped { captured = it.reason }

        assertEquals("test reason", captured)
    }

    @Test
    fun `callback chain returns original result`() {
        val original = ElementProcessingResult.success("vuid")

        val returned = original
            .onSuccess { /* do something */ }
            .onFailure { /* do something */ }
            .onSkipped { /* do something */ }

        assertEquals(original, returned)
    }

    // ==================== Companion Factory Tests ====================

    @Test
    fun `success factory creates Success with defaults`() {
        val result = ElementProcessingResult.success("vuid-abc")

        assertTrue(result is ElementProcessingResult.Success)
        assertEquals("vuid-abc", result.vuid)
        assertEquals(1, result.commandsGenerated)
        assertEquals(0L, result.processingTimeMs)
    }

    @Test
    fun `failure factory creates Failure`() {
        val element = ElementInfo.button("OK")
        val result = ElementProcessingResult.failure("Test error", element)

        assertTrue(result is ElementProcessingResult.Failure)
        assertEquals("Test error", result.error)
        assertNotNull(result.elementInfo)
    }

    @Test
    fun `skipped factory creates Skipped`() {
        val result = ElementProcessingResult.skipped("Not needed")

        assertTrue(result is ElementProcessingResult.Skipped)
        assertEquals("Not needed", result.reason)
    }
}
