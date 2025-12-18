package com.augmentalis.actions

import kotlin.test.*

/**
 * Tests for ActionResult sealed class.
 *
 * Validates:
 * - Success result creation with message and data
 * - Failure result creation with message and exception
 * - Sealed class exhaustiveness in when() expressions
 * - Data extraction from results
 */
class ActionResultTest {

    // ========== SUCCESS TESTS ==========

    @Test
    fun `test Success with message only`() {
        val result = ActionResult.Success(message = "Action completed")

        assertEquals("Action completed", result.message)
        assertNull(result.data)
    }

    @Test
    fun `test Success with message and data`() {
        val data = mapOf(
            "intent" to "show_time",
            "timestamp" to 123456789L,
            "confidence" to 0.95f
        )
        val result = ActionResult.Success(
            message = "Opened clock app",
            data = data
        )

        assertEquals("Opened clock app", result.message)
        assertEquals(data, result.data)
        assertEquals("show_time", result.data?.get("intent"))
        assertEquals(123456789L, result.data?.get("timestamp"))
        assertEquals(0.95f, result.data?.get("confidence"))
    }

    @Test
    fun `test Success with no message or data`() {
        val result = ActionResult.Success()

        assertNull(result.message)
        assertNull(result.data)
    }

    @Test
    fun `test Success with empty data map`() {
        val result = ActionResult.Success(
            message = "Done",
            data = emptyMap()
        )

        assertEquals("Done", result.message)
        assertNotNull(result.data)
        assertTrue(result.data!!.isEmpty())
    }

    // ========== FAILURE TESTS ==========

    @Test
    fun `test Failure with message only`() {
        val result = ActionResult.Failure(message = "Action failed")

        assertEquals("Action failed", result.message)
        assertNull(result.exception)
    }

    @Test
    fun `test Failure with message and exception`() {
        val exception = IllegalStateException("App not installed")
        val result = ActionResult.Failure(
            message = "Clock app not found",
            exception = exception
        )

        assertEquals("Clock app not found", result.message)
        assertEquals(exception, result.exception)
        assertEquals("App not installed", result.exception?.message)
    }

    @Test
    fun `test Failure preserves exception stack trace`() {
        val exception = RuntimeException("Test error")
        val result = ActionResult.Failure(
            message = "Failed",
            exception = exception
        )

        assertNotNull(result.exception?.stackTrace)
        assertTrue(result.exception?.stackTrace?.isNotEmpty() == true)
    }

    // ========== SEALED CLASS TESTS ==========

    @Test
    fun `test sealed class exhaustiveness in when expression`() {
        val successResult: ActionResult = ActionResult.Success("OK")
        val failureResult: ActionResult = ActionResult.Failure("Error")

        // Verify when() expression is exhaustive
        val successMessage = when (successResult) {
            is ActionResult.Success -> "success"
            is ActionResult.Failure -> "failure"
        }

        val failureMessage = when (failureResult) {
            is ActionResult.Success -> "success"
            is ActionResult.Failure -> "failure"
        }

        assertEquals("success", successMessage)
        assertEquals("failure", failureMessage)
    }

    @Test
    fun `test type checking with is operator`() {
        val success: ActionResult = ActionResult.Success("OK")
        val failure: ActionResult = ActionResult.Failure("Error")

        assertTrue(success is ActionResult.Success)
        assertFalse(success is ActionResult.Failure)

        assertTrue(failure is ActionResult.Failure)
        assertFalse(failure is ActionResult.Success)
    }

    // ========== DATA EXTRACTION TESTS ==========

    @Test
    fun `test extracting message from polymorphic ActionResult`() {
        val results: List<ActionResult> = listOf(
            ActionResult.Success("Success message"),
            ActionResult.Failure("Failure message")
        )

        val messages = results.map { result ->
            when (result) {
                is ActionResult.Success -> result.message
                is ActionResult.Failure -> result.message
            }
        }

        assertEquals(listOf("Success message", "Failure message"), messages)
    }

    @Test
    fun `test safe data extraction from Success`() {
        val result: ActionResult = ActionResult.Success(
            message = "Done",
            data = mapOf("key" to "value")
        )

        val data = (result as? ActionResult.Success)?.data
        assertNotNull(data)
        assertEquals("value", data["key"])
    }

    @Test
    fun `test safe exception extraction from Failure`() {
        val exception = IllegalArgumentException("Bad input")
        val result: ActionResult = ActionResult.Failure(
            message = "Invalid",
            exception = exception
        )

        val extractedException = (result as? ActionResult.Failure)?.exception
        assertNotNull(extractedException)
        assertEquals("Bad input", extractedException.message)
    }

    // ========== EDGE CASES ==========

    @Test
    fun `test Success with large data map`() {
        val largeData = (1..100).associate { "key$it" to "value$it" }
        val result = ActionResult.Success(
            message = "Large data",
            data = largeData
        )

        assertEquals(100, result.data?.size)
        assertEquals("value1", result.data?.get("key1"))
        assertEquals("value100", result.data?.get("key100"))
    }

    @Test
    fun `test Success with complex nested data`() {
        val nestedData = mapOf(
            "user" to mapOf(
                "name" to "Test User",
                "id" to 123
            ),
            "settings" to listOf("dark_mode", "notifications"),
            "count" to 42
        )

        val result = ActionResult.Success(data = nestedData)

        @Suppress("UNCHECKED_CAST")
        val user = result.data?.get("user") as? Map<String, Any>
        assertEquals("Test User", user?.get("name"))
        assertEquals(123, user?.get("id"))

        @Suppress("UNCHECKED_CAST")
        val settings = result.data?.get("settings") as? List<String>
        assertEquals(2, settings?.size)
        assertTrue(settings?.contains("dark_mode") == true)
    }

    @Test
    fun `test Failure with different exception types`() {
        val exceptions = listOf(
            IllegalStateException("State error"),
            IllegalArgumentException("Arg error"),
            RuntimeException("Runtime error"),
            Exception("Generic error")
        )

        exceptions.forEach { exception ->
            val result = ActionResult.Failure(
                message = "Failed",
                exception = exception
            )

            assertEquals(exception, result.exception)
            assertTrue(result.exception is Exception)
        }
    }

    // ========== EQUALITY TESTS ==========

    @Test
    fun `test Success equality`() {
        val result1 = ActionResult.Success(message = "OK", data = mapOf("a" to 1))
        val result2 = ActionResult.Success(message = "OK", data = mapOf("a" to 1))
        val result3 = ActionResult.Success(message = "Different")

        assertEquals(result1, result2)
        assertNotEquals(result1, result3)
    }

    @Test
    fun `test Failure equality`() {
        val exception = RuntimeException("Test")
        val result1 = ActionResult.Failure(message = "Error", exception = exception)
        val result2 = ActionResult.Failure(message = "Error", exception = exception)
        val result3 = ActionResult.Failure(message = "Different error")

        assertEquals(result1, result2)
        assertNotEquals(result1, result3)
    }
}
