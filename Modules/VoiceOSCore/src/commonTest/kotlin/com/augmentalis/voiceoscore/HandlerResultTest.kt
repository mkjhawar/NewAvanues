/**
 * HandlerResultTest.kt - Unit tests for HandlerResult sealed class
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-17
 */
package com.augmentalis.voiceoscore

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HandlerResultTest {

    // ==================== Success Tests ====================

    @Test
    fun success_withMessage() {
        val result = HandlerResult.Success("Operation completed")

        assertTrue(result.isSuccess)
        assertFalse(result.isFailure)
        assertEquals("Operation completed", result.message)
    }

    @Test
    fun success_withData() {
        val data = mapOf("key" to "value", "count" to 42)
        val result = HandlerResult.Success(data = data)

        assertTrue(result.isSuccess)
        assertEquals("value", result.data["key"])
        assertEquals(42, result.data["count"])
    }

    @Test
    fun success_factoryMethod() {
        val result = HandlerResult.success("Done")

        assertIs<HandlerResult.Success>(result)
        assertTrue(result.isSuccess)
    }

    // ==================== Failure Tests ====================

    @Test
    fun failure_withReason() {
        val result = HandlerResult.Failure("Element not found")

        assertTrue(result.isFailure)
        assertFalse(result.isSuccess)
        assertEquals("Element not found", result.reason)
    }

    @Test
    fun failure_recoverable() {
        val result = HandlerResult.Failure("Network error", recoverable = true)

        assertTrue(result.recoverable)
    }

    @Test
    fun failure_nonRecoverable() {
        val result = HandlerResult.Failure("Fatal error", recoverable = false)

        assertFalse(result.recoverable)
    }

    @Test
    fun failure_withSuggestedAction() {
        val result = HandlerResult.Failure(
            reason = "Permission denied",
            suggestedAction = "Request accessibility permission"
        )

        assertEquals("Request accessibility permission", result.suggestedAction)
    }

    @Test
    fun failure_factoryMethod() {
        val result = HandlerResult.failure("Error occurred")

        assertIs<HandlerResult.Failure>(result)
        assertTrue(result.isFailure)
    }

    // ==================== NotHandled Tests ====================

    @Test
    fun notHandled_properties() {
        val result = HandlerResult.NotHandled

        assertFalse(result.isSuccess)
        assertFalse(result.isFailure)
    }

    @Test
    fun notHandled_factoryMethod() {
        val result = HandlerResult.notHandled()

        assertIs<HandlerResult.NotHandled>(result)
    }

    // ==================== RequiresInput Tests ====================

    @Test
    fun requiresInput_textInput() {
        val result = HandlerResult.RequiresInput(
            prompt = "Enter the text to type",
            inputType = InputType.TEXT
        )

        assertEquals("Enter the text to type", result.prompt)
        assertEquals(InputType.TEXT, result.inputType)
    }

    @Test
    fun requiresInput_numberInput() {
        val result = HandlerResult.RequiresInput(
            prompt = "Enter page number",
            inputType = InputType.NUMBER
        )

        assertEquals(InputType.NUMBER, result.inputType)
    }

    @Test
    fun requiresInput_confirmationInput() {
        val result = HandlerResult.RequiresInput(
            prompt = "Are you sure you want to delete?",
            inputType = InputType.CONFIRMATION
        )

        assertEquals(InputType.CONFIRMATION, result.inputType)
    }

    // ==================== InProgress Tests ====================

    @Test
    fun inProgress_withProgress() {
        val result = HandlerResult.InProgress(progress = 50, statusMessage = "Downloading...")

        assertEquals(50, result.progress)
        assertEquals("Downloading...", result.statusMessage)
    }

    @Test
    fun inProgress_defaultValues() {
        val result = HandlerResult.InProgress()

        assertEquals(0, result.progress)
        assertEquals("", result.statusMessage)
    }

    // ==================== AwaitingSelection Tests ====================

    @Test
    fun awaitingSelection_multipleMatches() {
        val result = HandlerResult.AwaitingSelection(
            message = "Multiple items found",
            matchCount = 3,
            accessibilityAnnouncement = "3 items found. Say a number to select."
        )

        assertTrue(result.isAwaitingSelection)
        assertEquals(3, result.matchCount)
        assertEquals("Multiple items found", result.message)
    }

    @Test
    fun awaitingSelection_factoryMethod() {
        val result = HandlerResult.awaitingSelection(
            message = "Select one",
            matchCount = 2,
            accessibilityAnnouncement = "2 options available"
        )

        assertIs<HandlerResult.AwaitingSelection>(result)
        assertEquals(2, result.matchCount)
    }

    // ==================== Pattern Matching Tests ====================

    @Test
    fun whenExpression_handlesAllCases() {
        val results = listOf(
            HandlerResult.Success("ok"),
            HandlerResult.Failure("error"),
            HandlerResult.NotHandled,
            HandlerResult.RequiresInput("prompt"),
            HandlerResult.InProgress(50),
            HandlerResult.AwaitingSelection("msg", 2, "announcement")
        )

        results.forEach { result ->
            val handled = when (result) {
                is HandlerResult.Success -> "success"
                is HandlerResult.Failure -> "failure"
                is HandlerResult.NotHandled -> "not_handled"
                is HandlerResult.RequiresInput -> "requires_input"
                is HandlerResult.InProgress -> "in_progress"
                is HandlerResult.AwaitingSelection -> "awaiting_selection"
            }
            assertTrue(handled.isNotEmpty())
        }
    }
}
