/**
 * PIIRedactionHelperTest.kt - Tests for PII redaction utility
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-11-17
 */
package com.augmentalis.voiceos.logging

import kotlin.test.*

/**
 * Comprehensive tests for PIIRedactionHelper
 *
 * Tests all PII detection and redaction patterns across platforms
 */
class PIIRedactionHelperTest {

    // ===== Email Redaction Tests =====

    @Test
    fun `redactEmail should redact simple email`() {
        val result = PIIRedactionHelper.redactEmail("Contact: user@example.com")
        assertEquals("Contact: [REDACTED-EMAIL]", result)
    }

    @Test
    fun `redactEmail should redact complex email with tags`() {
        val result = PIIRedactionHelper.redactEmail("Email: test.user+tag@domain.co.uk")
        assertEquals("Email: [REDACTED-EMAIL]", result)
    }

    @Test
    fun `redactEmail should handle multiple emails`() {
        val result = PIIRedactionHelper.redactEmail("user@example.com and admin@test.com")
        assertEquals("[REDACTED-EMAIL] and [REDACTED-EMAIL]", result)
    }

    @Test
    fun `containsEmail should detect email presence`() {
        assertTrue(PIIRedactionHelper.containsEmail("Contact: user@example.com"))
        assertFalse(PIIRedactionHelper.containsEmail("No email here"))
    }

    @Test
    fun `maskEmail should partially mask email`() {
        val result = PIIRedactionHelper.maskEmail("john.doe@example.com")
        assertTrue(result.startsWith("j"))
        assertTrue(result.contains("@"))
        assertTrue(result.contains("***"))
    }

    // ===== Phone Number Redaction Tests =====

    @Test
    fun `redactPhone should redact US phone formats`() {
        val tests = listOf(
            "555-123-4567" to "[REDACTED-PHONE]",
            "(555) 123-4567" to "[REDACTED-PHONE]",
            "555.123.4567" to "[REDACTED-PHONE]",
            "+1-555-123-4567" to "[REDACTED-PHONE]"
        )

        tests.forEach { (input, expected) ->
            val result = PIIRedactionHelper.redactPhone(input)
            assertEquals(expected, result, "Failed for input: $input")
        }
    }

    @Test
    fun `containsPhone should detect phone numbers`() {
        assertTrue(PIIRedactionHelper.containsPhone("Call: 555-123-4567"))
        assertFalse(PIIRedactionHelper.containsPhone("No phone here"))
    }

    @Test
    fun `maskPhone should show last 4 digits`() {
        val result = PIIRedactionHelper.maskPhone("555-123-4567")
        assertEquals("***-***-4567", result)
    }

    // ===== Credit Card Redaction Tests =====

    @Test
    fun `redactCreditCard should redact various formats`() {
        val tests = listOf(
            "4111-1111-1111-1111" to "[REDACTED-CC]",
            "4111 1111 1111 1111" to "[REDACTED-CC]",
            "4111111111111111" to "[REDACTED-CC]"
        )

        tests.forEach { (input, expected) ->
            val result = PIIRedactionHelper.redactCreditCard(input)
            assertEquals(expected, result, "Failed for input: $input")
        }
    }

    @Test
    fun `containsCreditCard should detect credit cards`() {
        assertTrue(PIIRedactionHelper.containsCreditCard("Card: 4111-1111-1111-1111"))
        assertFalse(PIIRedactionHelper.containsCreditCard("No card here"))
    }

    @Test
    fun `maskCreditCard should show last 4 digits`() {
        val result = PIIRedactionHelper.maskCreditCard("4111-1111-1111-1111")
        assertEquals("****-****-****-1111", result)
    }

    // ===== SSN Redaction Tests =====

    @Test
    fun `redactSSN should redact various formats`() {
        val tests = listOf(
            "123-45-6789" to "[REDACTED-SSN]",
            "123 45 6789" to "[REDACTED-SSN]",
            "123456789" to "[REDACTED-SSN]"
        )

        tests.forEach { (input, expected) ->
            val result = PIIRedactionHelper.redactSSN(input)
            assertEquals(expected, result, "Failed for input: $input")
        }
    }

    @Test
    fun `containsSSN should detect SSN patterns`() {
        assertTrue(PIIRedactionHelper.containsSSN("SSN: 123-45-6789"))
        assertFalse(PIIRedactionHelper.containsSSN("No SSN here"))
    }

    // ===== ZIP Code Redaction Tests =====

    @Test
    fun `redactZipCode should redact ZIP codes`() {
        val tests = listOf(
            "12345" to "[REDACTED-ZIP]",
            "12345-6789" to "[REDACTED-ZIP]"
        )

        tests.forEach { (input, expected) ->
            val result = PIIRedactionHelper.redactZipCode(input)
            assertEquals(expected, result, "Failed for input: $input")
        }
    }

    @Test
    fun `containsZipCode should detect ZIP codes`() {
        assertTrue(PIIRedactionHelper.containsZipCode("ZIP: 12345"))
        assertFalse(PIIRedactionHelper.containsZipCode("No ZIP here"))
    }

    // ===== Address Redaction Tests =====

    @Test
    fun `redactAddress should redact street addresses`() {
        val tests = listOf(
            "123 Main St" to "[REDACTED-ADDRESS]",
            "456 Oak Avenue" to "[REDACTED-ADDRESS]",
            "789 Elm Drive" to "[REDACTED-ADDRESS]"
        )

        tests.forEach { (input, expected) ->
            val result = PIIRedactionHelper.redactAddress(input)
            assertEquals(expected, result, "Failed for input: $input")
        }
    }

    @Test
    fun `containsAddress should detect addresses`() {
        assertTrue(PIIRedactionHelper.containsAddress("123 Main St"))
        assertFalse(PIIRedactionHelper.containsAddress("No address here"))
    }

    // ===== Name Redaction Tests =====

    @Test
    fun `redactName should redact names (heuristic)`() {
        val tests = listOf(
            "John Smith" to "[REDACTED-NAME]",
            "Mary-Jane O'Connor" to "[REDACTED-NAME]"
        )

        tests.forEach { (input, expected) ->
            val result = PIIRedactionHelper.redactName(input)
            assertEquals(expected, result, "Failed for input: $input")
        }
    }

    @Test
    fun `containsName should detect names`() {
        assertTrue(PIIRedactionHelper.containsName("Contact John Smith"))
        assertFalse(PIIRedactionHelper.containsName("contact person"))
    }

    // ===== Full PII Redaction Tests =====

    @Test
    fun `redactPII should redact all PII types`() {
        val input = "Contact John Smith at john@example.com or 555-123-4567. " +
                "Card: 4111-1111-1111-1111, SSN: 123-45-6789, ZIP: 12345"

        val result = PIIRedactionHelper.redactPII(input)

        // Verify all PII types are redacted
        assertTrue(result.contains("[REDACTED-EMAIL]"))
        assertTrue(result.contains("[REDACTED-PHONE]"))
        assertTrue(result.contains("[REDACTED-CC]"))
        assertTrue(result.contains("[REDACTED-SSN]"))
        assertTrue(result.contains("[REDACTED-ZIP]"))
        assertTrue(result.contains("[REDACTED-NAME]"))
    }

    @Test
    fun `containsPII should detect any PII type`() {
        assertTrue(PIIRedactionHelper.containsPII("Email: user@example.com"))
        assertTrue(PIIRedactionHelper.containsPII("Phone: 555-123-4567"))
        assertTrue(PIIRedactionHelper.containsPII("Card: 4111-1111-1111-1111"))
        assertFalse(PIIRedactionHelper.containsPII("No PII here"))
    }

    // ===== Edge Cases =====

    @Test
    fun `should handle null input gracefully`() {
        assertEquals("", PIIRedactionHelper.redactPII(null))
        assertEquals("", PIIRedactionHelper.redactEmail(null))
        assertEquals("", PIIRedactionHelper.redactPhone(null))
    }

    @Test
    fun `should handle empty input gracefully`() {
        assertEquals("", PIIRedactionHelper.redactPII(""))
        assertEquals("", PIIRedactionHelper.redactEmail(""))
        assertEquals("", PIIRedactionHelper.redactPhone(""))
    }

    @Test
    fun `should preserve non-PII content`() {
        val input = "Processing request 12345 for app com.example.app"
        val result = PIIRedactionHelper.redactPII(input)
        assertEquals(input, result)
    }

    @Test
    fun `should handle mixed PII and safe identifiers`() {
        val input = "Element com.example:id/email has text: user@example.com in package com.example.app"
        val result = PIIRedactionHelper.redactPII(input)

        // Email should be redacted
        assertTrue(result.contains("[REDACTED-EMAIL]"))

        // Safe identifiers should be preserved
        assertTrue(result.contains("com.example:id/email"))
        assertTrue(result.contains("com.example.app"))
    }

    // ===== Mask Method Edge Cases =====

    @Test
    fun `maskEmail should handle invalid email`() {
        val result = PIIRedactionHelper.maskEmail("not-an-email")
        assertEquals("[INVALID-EMAIL]", result)
    }

    @Test
    fun `maskPhone should handle invalid phone`() {
        val result = PIIRedactionHelper.maskPhone("123")
        assertEquals("[INVALID-PHONE]", result)
    }

    @Test
    fun `maskCreditCard should handle invalid card`() {
        val result = PIIRedactionHelper.maskCreditCard("123")
        assertEquals("[INVALID-CC]", result)
    }

    // ===== Performance Tests (Smoke tests) =====

    @Test
    fun `should handle large text efficiently`() {
        val largeText = buildString {
            repeat(1000) {
                append("Line $it: user@example.com and 555-123-4567\n")
            }
        }

        val result = PIIRedactionHelper.redactPII(largeText)

        // Verify redaction happened
        assertTrue(result.contains("[REDACTED-EMAIL]"))
        assertTrue(result.contains("[REDACTED-PHONE]"))
    }
}
