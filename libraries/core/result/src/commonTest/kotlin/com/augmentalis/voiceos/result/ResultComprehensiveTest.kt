/**
 * ResultComprehensiveTest.kt - Comprehensive tests for Result type
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-17
 */
package com.augmentalis.voiceos.result

import kotlin.test.*

/**
 * Comprehensive tests for VoiceOSResult sealed class
 */
class VoiceOSResultTest {

    @Test
    fun testSuccessCreation() {
        val result = VoiceOSResult.Success("value")
        assertTrue(result is VoiceOSResult.Success)
        assertEquals("value", result.data)
    }

    @Test
    fun testSuccessWithMessage() {
        val result = VoiceOSResult.Success("value", "Success message")
        assertEquals("value", result.data)
        assertEquals("Success message", result.message)
    }

    @Test
    fun testSuccessWithDifferentTypes() {
        val stringResult = VoiceOSResult.Success("string")
        val intResult = VoiceOSResult.Success(42)
        val listResult = VoiceOSResult.Success(listOf(1, 2, 3))

        assertEquals("string", stringResult.data)
        assertEquals(42, intResult.data)
        assertEquals(3, listResult.data.size)
    }

    @Test
    fun testSuccessWithNullData() {
        val result = VoiceOSResult.Success<String?>(null)
        assertTrue(result is VoiceOSResult.Success)
        assertNull(result.data)
    }

    @Test
    fun testFailureCreation() {
        val error = VoiceOSError("ERROR_CODE", "Error message")
        val result = VoiceOSResult.Failure(error)
        assertTrue(result is VoiceOSResult.Failure)
        assertEquals("Error message", result.error.message)
        assertEquals("ERROR_CODE", result.error.code)
    }

    @Test
    fun testFailureWithCause() {
        val cause = Exception("Cause")
        val error = VoiceOSError("ERROR", "Error", cause)
        val result = VoiceOSResult.Failure(error)

        assertEquals("Error", result.error.message)
        assertNotNull(result.error.cause)
        assertEquals("Cause", result.error.cause?.message)
    }

    @Test
    fun testNotFoundCreation() {
        val result = VoiceOSResult.NotFound("element-123", "Element")
        assertTrue(result is VoiceOSResult.NotFound)
        assertEquals("element-123", result.identifier)
        assertEquals("Element", result.resourceType)
    }

    @Test
    fun testPermissionDeniedCreation() {
        val result = VoiceOSResult.PermissionDenied("WRITE_EXTERNAL_STORAGE", "Need to save files")
        assertTrue(result is VoiceOSResult.PermissionDenied)
        assertEquals("WRITE_EXTERNAL_STORAGE", result.permission)
        assertEquals("Need to save files", result.reason)
    }

    @Test
    fun testTimeoutCreation() {
        val result = VoiceOSResult.Timeout("database_query", 5000L, 3000L)
        assertTrue(result is VoiceOSResult.Timeout)
        assertEquals("database_query", result.operation)
        assertEquals(5000L, result.durationMs)
        assertEquals(3000L, result.timeoutMs)
    }

    @Test
    fun testIsSuccess() {
        val success = VoiceOSResult.Success("value")
        val failure = VoiceOSResult.Failure(VoiceOSError("ERR", "error"))
        val notFound = VoiceOSResult.NotFound("id", "Resource")

        assertTrue(success.isSuccess())
        assertFalse(failure.isSuccess())
        assertFalse(notFound.isSuccess())
    }

    @Test
    fun testIsFailure() {
        val success = VoiceOSResult.Success("value")
        val failure = VoiceOSResult.Failure(VoiceOSError("ERR", "error"))
        val notFound = VoiceOSResult.NotFound("id", "Resource")
        val timeout = VoiceOSResult.Timeout("op", 1000L, 500L)

        assertFalse(success.isFailure())
        assertTrue(failure.isFailure())
        assertTrue(notFound.isFailure())
        assertTrue(timeout.isFailure())
    }

    @Test
    fun testGetOrNull() {
        val success = VoiceOSResult.Success("value")
        val failure = VoiceOSResult.Failure(VoiceOSError("ERR", "error"))
        val notFound = VoiceOSResult.NotFound("id", "Resource")

        assertEquals("value", success.getOrNull())
        assertNull(failure.getOrNull())
        assertNull(notFound.getOrNull())
    }

    @Test
    fun testGetOrDefault() {
        val success = VoiceOSResult.Success("value")
        val failure: VoiceOSResult<String> = VoiceOSResult.Failure(VoiceOSError("ERR", "error"))

        assertEquals("value", success.getOrDefault("default"))
        assertEquals("default", failure.getOrDefault("default"))
    }

    @Test
    fun testGetOrThrow() {
        val success = VoiceOSResult.Success("value")
        assertEquals("value", success.getOrThrow())

        val failure = VoiceOSResult.Failure(VoiceOSError("ERR", "error"))
        assertFailsWith<VoiceOSException> {
            failure.getOrThrow()
        }
    }

    @Test
    fun testGetOrThrowForNotFound() {
        val notFound = VoiceOSResult.NotFound("element-123", "Element")
        val exception = assertFailsWith<VoiceOSException> {
            notFound.getOrThrow()
        }
        assertTrue(exception.message?.contains("not found") == true)
    }

    @Test
    fun testGetOrThrowForPermissionDenied() {
        val denied = VoiceOSResult.PermissionDenied("CAMERA", "Need camera access")
        val exception = assertFailsWith<VoiceOSException> {
            denied.getOrThrow()
        }
        assertEquals("PERMISSION_DENIED", exception.getCode())
    }

    @Test
    fun testGetOrThrowForTimeout() {
        val timeout = VoiceOSResult.Timeout("query", 5000L, 3000L)
        val exception = assertFailsWith<VoiceOSException> {
            timeout.getOrThrow()
        }
        assertEquals("TIMEOUT", exception.getCode())
    }

    @Test
    fun testMap() {
        val result = VoiceOSResult.Success(5)
        val mapped = result.map { it * 2 }

        assertTrue(mapped is VoiceOSResult.Success)
        assertEquals(10, (mapped as VoiceOSResult.Success).data)
    }

    @Test
    fun testMapOnFailure() {
        val error = VoiceOSError("ERR", "error")
        val result: VoiceOSResult<Int> = VoiceOSResult.Failure(error)
        val mapped = result.map { it * 2 }

        assertTrue(mapped is VoiceOSResult.Failure)
        assertEquals("error", (mapped as VoiceOSResult.Failure).error.message)
    }

    @Test
    fun testMapOnNotFound() {
        val result: VoiceOSResult<Int> = VoiceOSResult.NotFound("id", "Item")
        val mapped = result.map { it * 2 }

        assertTrue(mapped is VoiceOSResult.NotFound)
    }

    @Test
    fun testFlatMap() {
        val result = VoiceOSResult.Success(5)
        val flatMapped = result.flatMap { VoiceOSResult.Success(it * 2) }

        assertTrue(flatMapped is VoiceOSResult.Success)
        assertEquals(10, (flatMapped as VoiceOSResult.Success).data)
    }

    @Test
    fun testFlatMapToFailure() {
        val result = VoiceOSResult.Success(5)
        val flatMapped = result.flatMap<Int> {
            VoiceOSResult.Failure(VoiceOSError("FAIL", "Failed"))
        }

        assertTrue(flatMapped is VoiceOSResult.Failure)
    }

    @Test
    fun testOnSuccess() {
        var captured = ""
        val result = VoiceOSResult.Success("value")

        result.onSuccess { captured = it }

        assertEquals("value", captured)
    }

    @Test
    fun testOnSuccessNotCalledForFailure() {
        var called = false
        val result: VoiceOSResult<String> = VoiceOSResult.Failure(VoiceOSError("ERR", "error"))

        result.onSuccess { called = true }

        assertFalse(called)
    }

    @Test
    fun testOnFailure() {
        var captured = ""
        val result: VoiceOSResult<String> = VoiceOSResult.Failure(VoiceOSError("ERR", "error message"))

        result.onFailure { captured = it.message }

        assertEquals("error message", captured)
    }

    @Test
    fun testOnFailureNotCalledForSuccess() {
        var called = false
        val result = VoiceOSResult.Success("value")

        result.onFailure { called = true }

        assertFalse(called)
    }

    @Test
    fun testOnFailureForNotFound() {
        var captured = ""
        val result: VoiceOSResult<String> = VoiceOSResult.NotFound("item-123", "Item")

        result.onFailure { captured = it.code }

        assertEquals("NOT_FOUND", captured)
    }

    @Test
    fun testOnFailureForTimeout() {
        var captured = ""
        val result: VoiceOSResult<String> = VoiceOSResult.Timeout("query", 5000L, 3000L)

        result.onFailure { captured = it.code }

        assertEquals("TIMEOUT", captured)
    }

    @Test
    fun testOnFailureForPermissionDenied() {
        var captured = ""
        val result: VoiceOSResult<String> = VoiceOSResult.PermissionDenied("CAMERA", "Need camera")

        result.onFailure { captured = it.code }

        assertEquals("PERMISSION_DENIED", captured)
    }

    @Test
    fun testWhenExpression() {
        val result: VoiceOSResult<String> = VoiceOSResult.Success("value")

        val message = when (result) {
            is VoiceOSResult.Success -> "Got: ${result.data}"
            is VoiceOSResult.Failure -> "Error: ${result.error.message}"
            is VoiceOSResult.NotFound -> "Not found: ${result.identifier}"
            is VoiceOSResult.PermissionDenied -> "Denied: ${result.permission}"
            is VoiceOSResult.Timeout -> "Timeout: ${result.operation}"
        }

        assertEquals("Got: value", message)
    }

    @Test
    fun testChaining() {
        val result = VoiceOSResult.Success(5)
            .map { it * 2 }
            .map { it + 1 }
            .map { it.toString() }

        assertEquals("11", (result as VoiceOSResult.Success).data)
    }

    @Test
    fun testChainingWithFailure() {
        val result = VoiceOSResult.Success(5)
            .map { it * 2 }
            .flatMap<Int> { VoiceOSResult.Failure(VoiceOSError("FAIL", "Failed at 10")) }
            .map { it + 1 }

        assertTrue(result is VoiceOSResult.Failure)
        assertEquals("Failed at 10", (result as VoiceOSResult.Failure).error.message)
    }

    @Test
    fun testChainingPreservesMessage() {
        val result = VoiceOSResult.Success("data", "Original message")
            .map { it.uppercase() }

        assertTrue(result is VoiceOSResult.Success)
        assertEquals("DATA", (result as VoiceOSResult.Success).data)
        assertEquals("Original message", result.message)
    }
}

/**
 * Tests for VoiceOSError
 */
class VoiceOSErrorTest {

    @Test
    fun testErrorCreation() {
        val error = VoiceOSError("ERR_001", "Error message")
        assertEquals("ERR_001", error.code)
        assertEquals("Error message", error.message)
        assertNull(error.cause)
        assertTrue(error.context.isEmpty())
    }

    @Test
    fun testErrorWithCause() {
        val cause = Exception("Original error")
        val error = VoiceOSError("ERR", "Wrapped error", cause)

        assertNotNull(error.cause)
        assertEquals("Original error", error.cause?.message)
    }

    @Test
    fun testErrorWithContext() {
        val error = VoiceOSError(
            "ERR",
            "Error",
            context = mapOf("userId" to "123", "action" to "save")
        )

        assertEquals(2, error.context.size)
        assertEquals("123", error.context["userId"])
        assertEquals("save", error.context["action"])
    }

    @Test
    fun testGetFullMessage() {
        val cause = Exception("Cause message")
        val error = VoiceOSError("ERR", "Error message", cause)

        val fullMessage = error.getFullMessage()
        assertTrue(fullMessage.contains("Error message"))
        assertTrue(fullMessage.contains("Cause message"))
    }

    @Test
    fun testGetFullMessageWithoutCause() {
        val error = VoiceOSError("ERR", "Error message")
        assertEquals("Error message", error.getFullMessage())
    }

    @Test
    fun testWithContext() {
        val error = VoiceOSError("ERR", "Error")
        val withContext = error.withContext(mapOf("key" to "value"))

        assertEquals(1, withContext.context.size)
        assertEquals("value", withContext.context["key"])
    }

    @Test
    fun testWithContextMerges() {
        val error = VoiceOSError("ERR", "Error", context = mapOf("key1" to "value1"))
        val withContext = error.withContext(mapOf("key2" to "value2"))

        assertEquals(2, withContext.context.size)
        assertEquals("value1", withContext.context["key1"])
        assertEquals("value2", withContext.context["key2"])
    }
}

/**
 * Tests for VoiceOSException
 */
class VoiceOSExceptionTest {

    @Test
    fun testExceptionCreation() {
        val error = VoiceOSError("ERR_001", "Error message")
        val exception = VoiceOSException(error)

        assertEquals("ERR_001", exception.getCode())
        assertEquals("Error message", exception.message)
    }

    @Test
    fun testExceptionPreservesCause() {
        val cause = Exception("Original")
        val error = VoiceOSError("ERR", "Wrapped", cause)
        val exception = VoiceOSException(error)

        assertEquals(cause, exception.cause)
    }

    @Test
    fun testGetFullMessage() {
        val cause = Exception("Cause")
        val error = VoiceOSError("ERR", "Error", cause)
        val exception = VoiceOSException(error)

        val fullMessage = exception.getFullMessage()
        assertTrue(fullMessage.contains("Error"))
        assertTrue(fullMessage.contains("Cause"))
    }
}

/**
 * Tests for runCatchingResult function
 */
class RunCatchingResultTest {

    @Test
    fun testRunCatchingResultSuccess() {
        val result = runCatchingResult {
            "value"
        }
        assertTrue(result is VoiceOSResult.Success)
        assertEquals("value", (result as VoiceOSResult.Success).data)
    }

    @Test
    fun testRunCatchingResultFailure() {
        val result = runCatchingResult<String> {
            throw Exception("error")
        }
        assertTrue(result is VoiceOSResult.Failure)
        assertEquals("EXCEPTION", (result as VoiceOSResult.Failure).error.code)
    }

    @Test
    fun testRunCatchingResultWithNullMessage() {
        val result = runCatchingResult<String> {
            throw Exception()
        }
        assertTrue(result is VoiceOSResult.Failure)
        assertEquals("Unknown error", (result as VoiceOSResult.Failure).error.message)
    }

    @Test
    fun testRunCatchingResultPreservesCause() {
        val original = Exception("Original error")
        val result = runCatchingResult<String> {
            throw original
        }

        assertTrue(result is VoiceOSResult.Failure)
        assertEquals(original, (result as VoiceOSResult.Failure).error.cause)
    }
}

/**
 * Integration tests for Result usage patterns
 */
class ResultIntegrationTest {

    @Test
    fun testRepositoryPattern() {
        // Simulate repository returning Result
        fun fetchData(id: Int): VoiceOSResult<String> {
            return if (id > 0) {
                VoiceOSResult.Success("Data for $id")
            } else {
                VoiceOSResult.Failure(VoiceOSError("INVALID_ID", "Invalid ID"))
            }
        }

        val success = fetchData(1)
        val failure = fetchData(-1)

        assertEquals("Data for 1", success.getOrNull())
        assertNull(failure.getOrNull())
    }

    @Test
    fun testServiceCallChain() {
        fun step1(): VoiceOSResult<Int> = VoiceOSResult.Success(10)
        fun step2(input: Int): VoiceOSResult<Int> = VoiceOSResult.Success(input * 2)
        fun step3(input: Int): VoiceOSResult<String> = VoiceOSResult.Success("Result: $input")

        val result = step1()
            .flatMap { step2(it) }
            .flatMap { step3(it) }

        assertEquals("Result: 20", (result as VoiceOSResult.Success).data)
    }

    @Test
    fun testNotFoundPattern() {
        fun findItem(id: String): VoiceOSResult<String> {
            val items = mapOf("1" to "Item 1", "2" to "Item 2")
            return items[id]?.let { VoiceOSResult.Success(it) }
                ?: VoiceOSResult.NotFound(id, "Item")
        }

        val found = findItem("1")
        val notFound = findItem("999")

        assertTrue(found is VoiceOSResult.Success)
        assertTrue(notFound is VoiceOSResult.NotFound)
        assertEquals("999", (notFound as VoiceOSResult.NotFound).identifier)
    }

    @Test
    fun testTimeoutPattern() {
        fun slowOperation(): VoiceOSResult<String> {
            // Simulate timeout
            return VoiceOSResult.Timeout("database_query", 5000L, 3000L)
        }

        val result = slowOperation()
        assertTrue(result is VoiceOSResult.Timeout)
        assertEquals("database_query", (result as VoiceOSResult.Timeout).operation)
    }

    @Test
    fun testPermissionPattern() {
        fun requiresPermission(hasPermission: Boolean): VoiceOSResult<String> {
            return if (hasPermission) {
                VoiceOSResult.Success("Access granted")
            } else {
                VoiceOSResult.PermissionDenied("WRITE_EXTERNAL_STORAGE", "Need to save file")
            }
        }

        val granted = requiresPermission(true)
        val denied = requiresPermission(false)

        assertTrue(granted is VoiceOSResult.Success)
        assertTrue(denied is VoiceOSResult.PermissionDenied)
    }

    @Test
    fun testErrorRecoveryPattern() {
        fun primaryOperation(): VoiceOSResult<String> {
            return VoiceOSResult.Failure(VoiceOSError("PRIMARY_FAILED", "Primary failed"))
        }

        fun fallbackOperation(): VoiceOSResult<String> {
            return VoiceOSResult.Success("Fallback value")
        }

        val result = primaryOperation()
        val finalResult = if (result.isFailure()) {
            fallbackOperation()
        } else {
            result
        }

        assertTrue(finalResult is VoiceOSResult.Success)
        assertEquals("Fallback value", (finalResult as VoiceOSResult.Success).data)
    }

    @Test
    fun testCompleteErrorHandlingFlow() {
        val results = listOf(
            VoiceOSResult.Success("data"),
            VoiceOSResult.Failure(VoiceOSError("ERR", "error")),
            VoiceOSResult.NotFound("id", "Item"),
            VoiceOSResult.PermissionDenied("CAMERA", "reason"),
            VoiceOSResult.Timeout("op", 1000L, 500L)
        )

        results.forEach { result ->
            when (result) {
                is VoiceOSResult.Success -> {
                    assertNotNull(result.data)
                }
                is VoiceOSResult.Failure -> {
                    assertNotNull(result.error.code)
                    assertNotNull(result.error.message)
                }
                is VoiceOSResult.NotFound -> {
                    assertNotNull(result.identifier)
                    assertNotNull(result.resourceType)
                }
                is VoiceOSResult.PermissionDenied -> {
                    assertNotNull(result.permission)
                }
                is VoiceOSResult.Timeout -> {
                    assertNotNull(result.operation)
                    assertTrue(result.durationMs > 0)
                }
            }
        }
    }
}
