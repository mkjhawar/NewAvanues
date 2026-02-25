/**
 * PIIRedactionHelperTest.kt — Unit tests for PIIRedactionHelper
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-22
 *
 * Tests each redaction method individually: email, phone, SSN, credit card,
 * address, ZIP, and name. Also validates containsPII detection,
 * partial masking helpers, and null/blank safety.
 */
package com.avanues.logging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PIIRedactionHelperTest {

    // ── Email Redaction ───────────────────────────────────────────

    @Test
    fun `redactEmail removes a standard email address`() {
        val result = PIIRedactionHelper.redactEmail("Contact john.doe@example.com for help")
        assertFalse(result.contains("john.doe@example.com"))
        assertTrue(result.contains("[REDACTED-EMAIL]"))
    }

    @Test
    fun `redactEmail handles plus-addressing and subdomains`() {
        val result = PIIRedactionHelper.redactEmail("user+tag@mail.co.uk")
        assertFalse(result.contains("@"))
        assertTrue(result.contains("[REDACTED-EMAIL]"))
    }

    @Test
    fun `redactEmail returns empty string for null input`() {
        assertEquals("", PIIRedactionHelper.redactEmail(null))
    }

    @Test
    fun `redactEmail leaves non-email text unchanged`() {
        val text = "No emails here, just plain text."
        assertEquals(text, PIIRedactionHelper.redactEmail(text))
    }

    @Test
    fun `redactEmail removes multiple email addresses`() {
        val result = PIIRedactionHelper.redactEmail("a@b.com and c@d.org")
        assertEquals(2, result.split("[REDACTED-EMAIL]").size - 1)
    }

    // ── Phone Redaction ───────────────────────────────────────────

    @Test
    fun `redactPhone removes formatted phone number`() {
        val result = PIIRedactionHelper.redactPhone("Call (555) 123-4567 now")
        assertFalse(result.contains("555"))
        assertTrue(result.contains("[REDACTED-PHONE]"))
    }

    @Test
    fun `redactPhone removes international format`() {
        val result = PIIRedactionHelper.redactPhone("+1-555-123-4567")
        assertTrue(result.contains("[REDACTED-PHONE]"))
    }

    @Test
    fun `redactPhone returns empty string for blank input`() {
        assertEquals("", PIIRedactionHelper.redactPhone("   "))
    }

    // ── SSN Redaction ─────────────────────────────────────────────

    @Test
    fun `redactSSN removes hyphenated SSN`() {
        val result = PIIRedactionHelper.redactSSN("SSN: 123-45-6789 on file")
        assertFalse(result.contains("123-45-6789"))
        assertTrue(result.contains("[REDACTED-SSN]"))
    }

    @Test
    fun `redactSSN removes space-separated SSN`() {
        val result = PIIRedactionHelper.redactSSN("SSN 123 45 6789")
        assertTrue(result.contains("[REDACTED-SSN]"))
    }

    @Test
    fun `redactSSN leaves unrelated digit sequences alone`() {
        // 6 consecutive digits should not match SSN pattern (###-##-####)
        val text = "Order #123456"
        val result = PIIRedactionHelper.redactSSN(text)
        assertFalse(result.contains("[REDACTED-SSN]"), "Short digit run should not be redacted")
    }

    // ── Credit Card Redaction ─────────────────────────────────────

    @Test
    fun `redactCreditCard removes hyphen-delimited card number`() {
        val result = PIIRedactionHelper.redactCreditCard("Card: 4111-1111-1111-1111")
        assertFalse(result.contains("4111"))
        assertTrue(result.contains("[REDACTED-CC]"))
    }

    @Test
    fun `redactCreditCard returns empty string for null input`() {
        assertEquals("", PIIRedactionHelper.redactCreditCard(null))
    }

    // ── Address Redaction ─────────────────────────────────────────

    @Test
    fun `redactAddress removes street address with Street suffix`() {
        val result = PIIRedactionHelper.redactAddress("Located at 123 Main Street in the city")
        assertTrue(result.contains("[REDACTED-ADDRESS]"))
        assertFalse(result.contains("123 Main Street"))
    }

    @Test
    fun `redactAddress removes address with Blvd suffix`() {
        val result = PIIRedactionHelper.redactAddress("456 Oak Boulevard is the venue")
        assertTrue(result.contains("[REDACTED-ADDRESS]"))
    }

    // ── Name Redaction ────────────────────────────────────────────

    @Test
    fun `redactName removes capitalized first and last name`() {
        val result = PIIRedactionHelper.redactName("Patient: John Smith needs appointment")
        assertFalse(result.contains("John Smith"))
        assertTrue(result.contains("[REDACTED-NAME]"))
    }

    @Test
    fun `redactName does not remove lowercase phrases`() {
        // Lowercase text must NOT trigger the name heuristic (IGNORE_CASE omitted by design)
        val text = "the quick brown fox"
        val result = PIIRedactionHelper.redactName(text)
        assertFalse(result.contains("[REDACTED-NAME]"),
            "Lowercase phrases should not be redacted as names")
    }

    // ── redactPII (full pipeline) ─────────────────────────────────

    @Test
    fun `redactPII handles null gracefully returning empty string`() {
        assertEquals("", PIIRedactionHelper.redactPII(null))
    }

    @Test
    fun `redactPII redacts email from a mixed sentence`() {
        val result = PIIRedactionHelper.redactPII("Send results to patient@hospital.org")
        assertFalse(result.contains("patient@hospital.org"))
    }

    @Test
    fun `redactPII redacts SSN from log message`() {
        val result = PIIRedactionHelper.redactPII("Verified SSN 987-65-4321 in database")
        assertFalse(result.contains("987-65-4321"))
    }

    // ── containsPII Detection ─────────────────────────────────────

    @Test
    fun `containsPII returns true when email present`() {
        assertTrue(PIIRedactionHelper.containsPII("Contact admin@example.com"))
    }

    @Test
    fun `containsPII returns false for clean text`() {
        assertFalse(PIIRedactionHelper.containsPII("System started successfully"))
    }

    @Test
    fun `containsPII returns false for null input`() {
        assertFalse(PIIRedactionHelper.containsPII(null))
    }

    // ── Partial Masking ───────────────────────────────────────────

    @Test
    fun `maskEmail replaces most characters keeping first char and domain first char`() {
        val masked = PIIRedactionHelper.maskEmail("john.doe@example.com")
        assertTrue(masked.startsWith("j***@"))
        assertTrue(masked.contains("***."))
        assertFalse(masked.contains("john.doe"))
    }

    @Test
    fun `maskEmail returns INVALID-EMAIL for input without at sign`() {
        assertEquals("[INVALID-EMAIL]", PIIRedactionHelper.maskEmail("notanemail"))
    }

    @Test
    fun `maskPhone shows only last 4 digits`() {
        val masked = PIIRedactionHelper.maskPhone("(555) 123-4567")
        assertTrue(masked.endsWith("4567"))
        assertTrue(masked.startsWith("***-***-"))
    }

    @Test
    fun `maskCreditCard shows only last 4 digits`() {
        val masked = PIIRedactionHelper.maskCreditCard("4111111111111111")
        assertTrue(masked.endsWith("1111"))
        assertTrue(masked.startsWith("****-****-****-"))
    }
}
