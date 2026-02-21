/**
 * PIIRedactionHelper.kt - PII (Personally Identifiable Information) redaction utility
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-02 (consolidated from voiceos-logging)
 *
 * Detects and redacts PII from text before logging to protect user privacy.
 * Ensures logs do not contain sensitive information like email addresses,
 * phone numbers, credit cards, SSNs, names, or addresses.
 */
package com.avanues.logging

/**
 * PII Redaction Helper
 *
 * Provides comprehensive PII detection and redaction for logging safety.
 * All methods are null-safe and exception-safe.
 *
 * Performance: Regex patterns are pre-compiled for optimal performance.
 * Average redaction time: <1ms per call.
 */
object PIIRedactionHelper {

    // Compiled Regex Patterns

    /** Email: user@example.com, test.user+tag@domain.co.uk */
    private val EMAIL_PATTERN: Regex = Regex(
        "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
        RegexOption.IGNORE_CASE
    )

    /** Phone: (555) 123-4567, 555-123-4567, +1-555-123-4567 */
    private val PHONE_PATTERN: Regex = Regex(
        "(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}",
        RegexOption.IGNORE_CASE
    )

    /** Credit card: 4111-1111-1111-1111 (13-19 digits) */
    private val CREDIT_CARD_PATTERN: Regex = Regex(
        "\\b(?:\\d{4}[-.\\s]?){3}\\d{4}\\b|\\b\\d{13,19}\\b",
        RegexOption.IGNORE_CASE
    )

    /** SSN: 123-45-6789, 123 45 6789, 123456789 */
    private val SSN_PATTERN: Regex = Regex(
        "\\b\\d{3}[-.\\s]?\\d{2}[-.\\s]?\\d{4}\\b",
        RegexOption.IGNORE_CASE
    )

    /**
     * Name (heuristic): John Smith, Mary-Jane O'Connor
     * Requires both words to start with an uppercase letter followed by at least two lowercase
     * letters, so generic two-word phrases and single-case log tokens are not falsely redacted.
     * IGNORE_CASE is intentionally omitted — lowercase phrases must not match.
     */
    private val NAME_PATTERN: Regex = Regex(
        "\\b[A-Z][a-z]{2,}(?:['-][A-Z][a-z]+)?\\s+[A-Z][a-z]{2,}(?:['-][A-Z][a-z]+)?\\b"
    )

    /** Address (heuristic): 123 Main St, 456 Oak Avenue */
    private val ADDRESS_PATTERN: Regex = Regex(
        "\\b\\d+\\s+[A-Za-z]+\\s+(?:Street|St|Avenue|Ave|Road|Rd|Drive|Dr|Lane|Ln|Boulevard|Blvd|Court|Ct|Circle|Cir|Way)\\b",
        RegexOption.IGNORE_CASE
    )

    /**
     * ZIP code: 12345, 12345-6789
     * Negative lookbehind (?<!:) prevents matching port numbers like :50051.
     * IGNORE_CASE is irrelevant for digit-only patterns but removed for clarity.
     */
    private val ZIP_CODE_PATTERN: Regex = Regex(
        "(?<!:)\\b\\d{5}(?:-\\d{4})?\\b"
    )

    /**
     * Redact all PII types from text
     *
     * @param text Input text (may contain PII)
     * @return Text with all PII redacted
     */
    fun redactPII(text: String?): String {
        if (text.isNullOrBlank()) return ""

        return try {
            var result = text
            result = redactEmail(result)
            result = redactPhone(result)
            result = redactCreditCard(result)
            result = redactSSN(result)
            result = redactZipCode(result)
            result = redactAddress(result)
            result = redactName(result)
            result
        } catch (e: Exception) {
            "[REDACTED-ERROR]"
        }
    }

    /** Redact email addresses */
    fun redactEmail(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return try { EMAIL_PATTERN.replace(text, "[REDACTED-EMAIL]") } catch (e: Exception) { text }
    }

    /** Redact phone numbers */
    fun redactPhone(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return try { PHONE_PATTERN.replace(text, "[REDACTED-PHONE]") } catch (e: Exception) { text }
    }

    /** Redact credit card numbers */
    fun redactCreditCard(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return try { CREDIT_CARD_PATTERN.replace(text, "[REDACTED-CC]") } catch (e: Exception) { text }
    }

    /** Redact Social Security Numbers */
    fun redactSSN(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return try { SSN_PATTERN.replace(text, "[REDACTED-SSN]") } catch (e: Exception) { text }
    }

    /** Redact ZIP codes */
    fun redactZipCode(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return try { ZIP_CODE_PATTERN.replace(text, "[REDACTED-ZIP]") } catch (e: Exception) { text }
    }

    /** Redact street addresses */
    fun redactAddress(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return try { ADDRESS_PATTERN.replace(text, "[REDACTED-ADDRESS]") } catch (e: Exception) { text }
    }

    /** Redact names (heuristic) */
    fun redactName(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return try { NAME_PATTERN.replace(text, "[REDACTED-NAME]") } catch (e: Exception) { text }
    }

    // Partial Masking Methods

    /** Mask email: john.doe@example.com → j***@e***.com */
    fun maskEmail(email: String?): String {
        if (email.isNullOrBlank()) return ""
        return try {
            val parts = email.split("@")
            if (parts.size != 2) return "[INVALID-EMAIL]"
            val maskedLocal = if (parts[0].length > 1) "${parts[0][0]}***" else "***"
            val maskedDomain = if (parts[1].length > 1) "${parts[1][0]}***.${parts[1].substringAfterLast('.')}" else "***"
            "$maskedLocal@$maskedDomain"
        } catch (e: Exception) { "[INVALID-EMAIL]" }
    }

    /** Mask phone: (555) 123-4567 → ***-***-4567 */
    fun maskPhone(phone: String?): String {
        if (phone.isNullOrBlank()) return ""
        return try {
            val digits = phone.filter { it.isDigit() }
            if (digits.length < 4) return "[INVALID-PHONE]"
            "***-***-${digits.takeLast(4)}"
        } catch (e: Exception) { "[INVALID-PHONE]" }
    }

    /** Mask credit card: 4111-1111-1111-1111 → ****-****-****-1111 */
    fun maskCreditCard(creditCard: String?): String {
        if (creditCard.isNullOrBlank()) return ""
        return try {
            val digits = creditCard.filter { it.isDigit() }
            if (digits.length < 13) return "[INVALID-CC]"
            "****-****-****-${digits.takeLast(4)}"
        } catch (e: Exception) { "[INVALID-CC]" }
    }

    // Detection Methods

    /** Check if text contains any PII */
    fun containsPII(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        return try {
            containsEmail(text) || containsPhone(text) || containsCreditCard(text) ||
            containsSSN(text) || containsZipCode(text) || containsAddress(text) || containsName(text)
        } catch (e: Exception) { false }
    }

    fun containsEmail(text: String?): Boolean = text?.let { EMAIL_PATTERN.containsMatchIn(it) } ?: false
    fun containsPhone(text: String?): Boolean = text?.let { PHONE_PATTERN.containsMatchIn(it) } ?: false
    fun containsCreditCard(text: String?): Boolean = text?.let { CREDIT_CARD_PATTERN.containsMatchIn(it) } ?: false
    fun containsSSN(text: String?): Boolean = text?.let { SSN_PATTERN.containsMatchIn(it) } ?: false
    fun containsZipCode(text: String?): Boolean = text?.let { ZIP_CODE_PATTERN.containsMatchIn(it) } ?: false
    fun containsAddress(text: String?): Boolean = text?.let { ADDRESS_PATTERN.containsMatchIn(it) } ?: false
    fun containsName(text: String?): Boolean = text?.let { NAME_PATTERN.containsMatchIn(it) } ?: false
}
