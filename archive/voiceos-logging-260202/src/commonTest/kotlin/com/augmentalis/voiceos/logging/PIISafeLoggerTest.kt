/**
 * PIISafeLoggerTest.kt - Tests for PII-safe logging wrapper
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-11-17
 */
package com.augmentalis.voiceos.logging

import kotlin.test.*

/**
 * Test logger implementation for testing
 *
 * Captures logged messages for verification
 */
class TestLogger : Logger {
    val messages = mutableListOf<Pair<LogLevel, String>>()
    var throwable: Throwable? = null

    override fun v(message: () -> String) {
        messages.add(LogLevel.VERBOSE to message())
    }

    override fun d(message: () -> String) {
        messages.add(LogLevel.DEBUG to message())
    }

    override fun i(message: () -> String) {
        messages.add(LogLevel.INFO to message())
    }

    override fun w(message: () -> String) {
        messages.add(LogLevel.WARN to message())
    }

    override fun e(message: () -> String) {
        messages.add(LogLevel.ERROR to message())
    }

    override fun e(message: () -> String, throwable: Throwable) {
        messages.add(LogLevel.ERROR to message())
        this.throwable = throwable
    }

    override fun wtf(message: () -> String) {
        messages.add(LogLevel.ASSERT to message())
    }

    override fun isLoggable(level: LogLevel): Boolean = true

    fun clear() {
        messages.clear()
        throwable = null
    }

    fun getLastMessage(): String? = messages.lastOrNull()?.second
    fun getLastLevel(): LogLevel? = messages.lastOrNull()?.first
}

/**
 * Tests for PIISafeLogger
 *
 * Verifies automatic PII redaction across all log levels
 */
class PIISafeLoggerTest {

    private lateinit var testLogger: TestLogger
    private lateinit var piiLogger: PIISafeLogger

    @BeforeTest
    fun setup() {
        testLogger = TestLogger()
        piiLogger = PIISafeLogger(testLogger)
    }

    @AfterTest
    fun teardown() {
        testLogger.clear()
    }

    // ===== Basic Logging Tests =====

    @Test
    fun `verbose logging should redact PII automatically`() {
        piiLogger.v { "User email: user@example.com" }

        assertEquals(LogLevel.VERBOSE, testLogger.getLastLevel())
        assertEquals("User email: [REDACTED-EMAIL]", testLogger.getLastMessage())
    }

    @Test
    fun `debug logging should redact PII automatically`() {
        piiLogger.d { "Phone: 555-123-4567" }

        assertEquals(LogLevel.DEBUG, testLogger.getLastLevel())
        assertEquals("Phone: [REDACTED-PHONE]", testLogger.getLastMessage())
    }

    @Test
    fun `info logging should redact PII automatically`() {
        piiLogger.i { "Card: 4111-1111-1111-1111" }

        assertEquals(LogLevel.INFO, testLogger.getLastLevel())
        assertEquals("Card: [REDACTED-CC]", testLogger.getLastMessage())
    }

    @Test
    fun `warn logging should redact PII automatically`() {
        piiLogger.w { "SSN: 123-45-6789" }

        assertEquals(LogLevel.WARN, testLogger.getLastLevel())
        assertEquals("SSN: [REDACTED-SSN]", testLogger.getLastMessage())
    }

    @Test
    fun `error logging should redact PII automatically`() {
        piiLogger.e { "Error for user@example.com" }

        assertEquals(LogLevel.ERROR, testLogger.getLastLevel())
        assertEquals("Error for [REDACTED-EMAIL]", testLogger.getLastMessage())
    }

    @Test
    fun `error with exception should redact PII and preserve exception`() {
        val exception = RuntimeException("Test error")
        piiLogger.e({ "Failed for user@example.com" }, exception)

        assertEquals(LogLevel.ERROR, testLogger.getLastLevel())
        assertEquals("Failed for [REDACTED-EMAIL]", testLogger.getLastMessage())
        assertSame(exception, testLogger.throwable)
    }

    @Test
    fun `wtf logging should redact PII automatically`() {
        piiLogger.wtf { "Critical: user@example.com" }

        assertEquals(LogLevel.ASSERT, testLogger.getLastLevel())
        assertEquals("Critical: [REDACTED-EMAIL]", testLogger.getLastMessage())
    }

    // ===== Lazy Evaluation Tests =====

    @Test
    fun `should use lazy evaluation (message lambda)`() {
        var callCount = 0

        piiLogger.d {
            callCount++
            "Debug message"
        }

        assertEquals(1, callCount, "Lambda should be called exactly once")
        assertEquals(1, testLogger.messages.size)
    }

    // ===== Multiple PII Types Tests =====

    @Test
    fun `should redact multiple PII types in single message`() {
        piiLogger.d {
            "User user@example.com called from 555-123-4567 with card 4111-1111-1111-1111"
        }

        val message = testLogger.getLastMessage()!!
        assertTrue(message.contains("[REDACTED-EMAIL]"))
        assertTrue(message.contains("[REDACTED-PHONE]"))
        assertTrue(message.contains("[REDACTED-CC]"))
    }

    // ===== Safe Identifiers Preservation Tests =====

    @Test
    fun `should preserve resource IDs`() {
        piiLogger.d { "Element: com.example:id/button" }

        val message = testLogger.getLastMessage()!!
        assertTrue(message.contains("com.example:id/button"))
    }

    @Test
    fun `should preserve class names`() {
        piiLogger.d { "Class: android.widget.Button" }

        val message = testLogger.getLastMessage()!!
        assertTrue(message.contains("android.widget.Button"))
    }

    @Test
    fun `should preserve package names`() {
        piiLogger.d { "Package: com.augmentalis.voiceos" }

        val message = testLogger.getLastMessage()!!
        assertTrue(message.contains("com.augmentalis.voiceos"))
    }

    // ===== Edge Cases =====

    @Test
    fun `should handle empty message`() {
        piiLogger.d { "" }

        assertEquals("", testLogger.getLastMessage())
    }

    @Test
    fun `should handle message with no PII`() {
        val message = "Processing request 12345"
        piiLogger.d { message }

        assertEquals(message, testLogger.getLastMessage())
    }

    // ===== PIILoggingWrapper Convenience Methods Tests =====

    @Test
    fun `PIILoggingWrapper d should redact PII`() {
        PIILoggingWrapper.d("TAG", "Email: user@example.com")

        // No assertions here since we can't test LoggerFactory in commonTest
        // This is tested in androidTest
    }

    @Test
    fun `PIILoggingWrapper should handle null message`() {
        PIILoggingWrapper.d("TAG", null)

        // No assertions here since we can't test LoggerFactory in commonTest
        // This is tested in androidTest
    }
}
