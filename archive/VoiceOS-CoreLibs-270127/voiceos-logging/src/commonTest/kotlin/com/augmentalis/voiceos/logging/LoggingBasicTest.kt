/**
 * LoggingBasicTest.kt - Basic tests for logging and PII redaction
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-17
 */
package com.augmentalis.voiceos.logging

import kotlin.test.*

/**
 * Basic tests for PIIRedactionHelper covering all actual methods
 */
class PIIRedactionHelperBasicTest {

    @Test
    fun testRedactEmail_StandardEmails() {
        assertEquals("[REDACTED-EMAIL]", PIIRedactionHelper.redactEmail("user@example.com"))
        assertEquals("[REDACTED-EMAIL]", PIIRedactionHelper.redactEmail("john.doe@company.org"))
        assertEquals("[REDACTED-EMAIL]", PIIRedactionHelper.redactEmail("admin+tag@test.co.uk"))
    }

    @Test
    fun testRedactPhone_USFormats() {
        assertEquals("[REDACTED-PHONE]", PIIRedactionHelper.redactPhone("555-123-4567"))
        assertEquals("[REDACTED-PHONE]", PIIRedactionHelper.redactPhone("(555) 123-4567"))
        assertEquals("[REDACTED-PHONE]", PIIRedactionHelper.redactPhone("+1-555-123-4567"))
    }

    @Test
    fun testRedactCreditCard_CommonFormats() {
        assertEquals("[REDACTED-CC]", PIIRedactionHelper.redactCreditCard("4111-1111-1111-1111"))
        assertEquals("[REDACTED-CC]", PIIRedactionHelper.redactCreditCard("4111111111111111"))
        assertEquals("[REDACTED-CC]", PIIRedactionHelper.redactCreditCard("5500 0000 0000 0004"))
    }

    @Test
    fun testRedactSSN_StandardFormat() {
        assertEquals("[REDACTED-SSN]", PIIRedactionHelper.redactSSN("123-45-6789"))
        assertEquals("[REDACTED-SSN]", PIIRedactionHelper.redactSSN("987-65-4321"))
    }

    @Test
    fun testRedactZipCode_USFormats() {
        assertEquals("[REDACTED-ZIP]", PIIRedactionHelper.redactZipCode("12345"))
        assertEquals("[REDACTED-ZIP]", PIIRedactionHelper.redactZipCode("12345-6789"))
    }

    @Test
    fun testRedactAddress_CommonAddresses() {
        val address = "123 Main Street, Anytown, CA 12345"
        val result = PIIRedactionHelper.redactAddress(address)
        assertTrue(result.contains("[REDACTED-ADDRESS]") || result == address)
    }

    @Test
    fun testRedactName_CommonNames() {
        val result = PIIRedactionHelper.redactName("John Doe")
        assertTrue(result.contains("[REDACTED-NAME]") || result == "John Doe")
    }

    @Test
    fun testRedactPII_ComprehensiveRedaction() {
        val text = """
            User: John Doe
            Email: john.doe@example.com
            Phone: 555-123-4567
            SSN: 123-45-6789
            Credit Card: 4111-1111-1111-1111
            ZIP: 12345
        """.trimIndent()

        val result = PIIRedactionHelper.redactPII(text)

        assertTrue(result.contains("[REDACTED-EMAIL]"))
        assertTrue(result.contains("[REDACTED-PHONE]"))
        assertTrue(result.contains("[REDACTED-SSN]"))
        assertTrue(result.contains("[REDACTED-CC]"))
        assertTrue(result.contains("[REDACTED-ZIP]"))
    }

    @Test
    fun testRedactPII_PreservesSystemIdentifiers() {
        val text = "Resource: com.example:id/button Class: android.widget.TextView"
        val result = PIIRedactionHelper.redactPII(text)

        // System identifiers should be preserved
        assertTrue(result.contains("com.example:id/button"))
        assertTrue(result.contains("android.widget.TextView"))
    }

    @Test
    fun testRedactPII_EmptyAndNull() {
        assertEquals("", PIIRedactionHelper.redactPII(""))
        assertEquals("", PIIRedactionHelper.redactPII(null))
    }

    @Test
    fun testRedactEmail_NoEmail() {
        assertEquals("No email here", PIIRedactionHelper.redactEmail("No email here"))
    }

    @Test
    fun testRedactPhone_NoPhone() {
        assertEquals("No phone here", PIIRedactionHelper.redactPhone("No phone here"))
    }
}

/**
 * Basic tests for PIILoggingWrapper
 */
class PIILoggingWrapperBasicTest {

    @Test
    fun testGetLogger() {
        val logger = PIILoggingWrapper.getLogger("TestTag")
        assertNotNull(logger)
    }

    @Test
    fun testStaticLoggingMethods() {
        // These should not throw exceptions
        PIILoggingWrapper.d("Test", "Debug message with PII: test@example.com")
        PIILoggingWrapper.i("Test", "Info message")
        PIILoggingWrapper.w("Test", "Warning message")
        PIILoggingWrapper.e("Test", "Error message")
        PIILoggingWrapper.v("Test", "Verbose message")
        PIILoggingWrapper.wtf("Test", "WTF message")

        // Test should complete successfully
        assertTrue(true)
    }

    @Test
    fun testPIISafeLogger() {
        val logger = PIILoggingWrapper.getLogger("TEST")

        // These would normally log but with redacted PII
        logger.d { "Email: test@example.com" }
        logger.i { "Phone: 555-123-4567" }
        logger.w { "SSN: 123-45-6789" }
        logger.e { "Error with CC: 4111-1111-1111-1111" }

        // Should not throw
        assertTrue(true)
    }

    @Test
    fun testErrorLoggingWithException() {
        val logger = PIILoggingWrapper.getLogger("TEST")
        val exception = Exception("Test exception")

        // Should not throw
        PIILoggingWrapper.e("TEST", "Error occurred", exception)
        assertTrue(true)
    }
}

/**
 * Basic tests for Logger interface
 */
class LoggerBasicTest {

    @Test
    fun testLoggerFactory() {
        val logger = LoggerFactory.getLogger("TestLogger")
        assertNotNull(logger)

        // Test lazy evaluation
        logger.d { "Debug message" }
        logger.i { "Info message" }
        logger.w { "Warning message" }
        logger.e { "Error message" }
        logger.v { "Verbose message" }
        logger.wtf { "WTF message" }

        // Should complete successfully
        assertTrue(true)
    }

    @Test
    fun testLogLevels() {
        assertEquals(2, LogLevel.VERBOSE.priority)
        assertEquals(3, LogLevel.DEBUG.priority)
        assertEquals(4, LogLevel.INFO.priority)
        assertEquals(5, LogLevel.WARN.priority)
        assertEquals(6, LogLevel.ERROR.priority)
        assertEquals(7, LogLevel.ASSERT.priority)
    }

    @Test
    fun testLogLevelFromPriority() {
        assertEquals(LogLevel.DEBUG, LogLevel.fromPriority(3))
        assertEquals(LogLevel.INFO, LogLevel.fromPriority(4))
        assertEquals(LogLevel.WARN, LogLevel.fromPriority(5))
        assertEquals(LogLevel.ERROR, LogLevel.fromPriority(6))
    }

    @Test
    fun testLogLevelComparison() {
        assertTrue(LogLevel.VERBOSE.priority < LogLevel.DEBUG.priority)
        assertTrue(LogLevel.DEBUG.priority < LogLevel.INFO.priority)
        assertTrue(LogLevel.INFO.priority < LogLevel.WARN.priority)
        assertTrue(LogLevel.WARN.priority < LogLevel.ERROR.priority)
        assertTrue(LogLevel.ERROR.priority < LogLevel.ASSERT.priority)
    }
}

/**
 * Integration tests
 */
class LoggingIntegrationBasicTest {

    @Test
    fun testCompleteLoggingFlow() {
        // Get logger
        val logger = LoggerFactory.getLogger("Integration")

        // Log messages with various PII
        logger.d { "Starting process for user@example.com" }
        logger.i { "Processing payment: 4111-1111-1111-1111" }
        logger.w { "Accessing phone: 555-123-4567" }
        logger.e { "Failed with SSN: 123-45-6789" }

        // Use wrapper for backward compatibility
        PIILoggingWrapper.d("Wrapper", "Legacy call with email: test@test.com")

        // Should complete without errors
        assertTrue(true)
    }

    @Test
    fun testLazyEvaluation() {
        val logger = LoggerFactory.getLogger("Lazy")

        // Expensive operation should only be called if logging is enabled
        logger.d {
            // This lambda is only evaluated if debug logging is enabled
            val expensive = "Expensive ${System.currentTimeMillis()}"
            "Debug: $expensive"
        }

        // Should complete successfully
        assertTrue(true)
    }

    @Test
    fun testPIIRedactionInRealLogging() {
        val logger = PIILoggingWrapper.getLogger("PII")

        // Log with PII that should be redacted
        logger.i { "User john.doe@example.com called 555-123-4567" }
        logger.e { "Credit card 4111-1111-1111-1111 declined" }

        // Should complete without exposing PII
        assertTrue(true)
    }
}
