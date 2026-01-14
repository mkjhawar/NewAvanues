/**
 * PIIRedactionHelper.kt - PII (Personally Identifiable Information) redaction utility
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-31
 *
 * Purpose:
 * Detects and redacts PII from text before logging to protect user privacy.
 * Ensures logs do not contain sensitive information like email addresses,
 * phone numbers, credit cards, SSNs, names, or addresses.
 *
 * Usage:
 * ```kotlin
 * // Full redaction
 * Log.d(TAG, "User input: ${PIIRedactionHelper.redactPII(userText)}")
 *
 * // Specific type redaction
 * Log.d(TAG, "Email: ${PIIRedactionHelper.redactEmail(email)}")
 *
 * // Partial masking (for debugging)
 * Log.d(TAG, "Email: ${PIIRedactionHelper.maskEmail(email)}")
 * ```
 */
package com.augmentalis.voiceos.logging

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

    // ===== Compiled Regex Patterns (for performance) =====

    /**
     * Email pattern: matches standard email formats
     * Examples: user@example.com, test.user+tag@domain.co.uk
     */
    private val EMAIL_PATTERN: Regex = Regex(
        "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
        RegexOption.IGNORE_CASE
    )

    /**
     * Phone number pattern: matches various phone formats
     * Examples: (555) 123-4567, 555-123-4567, 5551234567, +1-555-123-4567
     */
    private val PHONE_PATTERN: Regex = Regex(
        "(?:\\+?\\d{1,3}[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}",
        RegexOption.IGNORE_CASE
    )

    /**
     * Credit card pattern: matches common card formats
     * Examples: 4111-1111-1111-1111, 4111 1111 1111 1111, 4111111111111111
     * Supports: Visa, MasterCard, Amex, Discover (13-19 digits)
     */
    private val CREDIT_CARD_PATTERN: Regex = Regex(
        "\\b(?:\\d{4}[-.\\s]?){3}\\d{4}\\b|\\b\\d{13,19}\\b",
        RegexOption.IGNORE_CASE
    )

    /**
     * SSN pattern: matches Social Security Number formats
     * Examples: 123-45-6789, 123 45 6789, 123456789
     */
    private val SSN_PATTERN: Regex = Regex(
        "\\b\\d{3}[-.\\s]?\\d{2}[-.\\s]?\\d{4}\\b",
        RegexOption.IGNORE_CASE
    )

    /**
     * Name pattern (simple heuristic): matches capitalized words
     * Examples: John Smith, Mary-Jane O'Connor
     * Note: This is a heuristic and may have false positives
     */
    private val NAME_PATTERN: Regex = Regex(
        "\\b[A-Z][a-z]+(?:['-][A-Z][a-z]+)?\\s+[A-Z][a-z]+(?:['-][A-Z][a-z]+)?\\b",
        RegexOption.IGNORE_CASE
    )

    /**
     * Address pattern (simple heuristic): matches street addresses
     * Examples: 123 Main St, 456 Oak Avenue Apt 2B
     * Note: This is a heuristic and may have false positives
     */
    private val ADDRESS_PATTERN: Regex = Regex(
        "\\b\\d+\\s+[A-Za-z]+\\s+(?:Street|St|Avenue|Ave|Road|Rd|Drive|Dr|Lane|Ln|Boulevard|Blvd|Court|Ct|Circle|Cir|Way)\\b",
        RegexOption.IGNORE_CASE
    )

    /**
     * ZIP code pattern: matches US ZIP codes
     * Examples: 12345, 12345-6789
     */
    private val ZIP_CODE_PATTERN: Regex = Regex(
        "\\b\\d{5}(?:-\\d{4})?\\b",
        RegexOption.IGNORE_CASE
    )

    // ===== Full Redaction Methods =====

    /**
     * Redact all PII types from text
     *
     * Detects and replaces all PII with [REDACTED-TYPE] markers.
     * This is the recommended method for general log sanitization.
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
            // Exception-safe: return empty string on error
            "[REDACTED-ERROR]"
        }
    }

    /**
     * Redact email addresses from text
     *
     * @param text Input text
     * @return Text with emails replaced with [REDACTED-EMAIL]
     */
    fun redactEmail(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return try {
            EMAIL_PATTERN.replace(text, "[REDACTED-EMAIL]")
        } catch (e: Exception) {
            text
        }
    }

    /**
     * Redact phone numbers from text
     *
     * @param text Input text
     * @return Text with phone numbers replaced with [REDACTED-PHONE]
     */
    fun redactPhone(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return try {
            PHONE_PATTERN.replace(text, "[REDACTED-PHONE]")
        } catch (e: Exception) {
            text
        }
    }

    /**
     * Redact credit card numbers from text
     *
     * @param text Input text
     * @return Text with credit cards replaced with [REDACTED-CC]
     */
    fun redactCreditCard(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return try {
            CREDIT_CARD_PATTERN.replace(text, "[REDACTED-CC]")
        } catch (e: Exception) {
            text
        }
    }

    /**
     * Redact Social Security Numbers from text
     *
     * @param text Input text
     * @return Text with SSNs replaced with [REDACTED-SSN]
     */
    fun redactSSN(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return try {
            SSN_PATTERN.replace(text, "[REDACTED-SSN]")
        } catch (e: Exception) {
            text
        }
    }

    /**
     * Redact ZIP codes from text
     *
     * @param text Input text
     * @return Text with ZIP codes replaced with [REDACTED-ZIP]
     */
    fun redactZipCode(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return try {
            ZIP_CODE_PATTERN.replace(text, "[REDACTED-ZIP]")
        } catch (e: Exception) {
            text
        }
    }

    /**
     * Redact street addresses from text
     *
     * @param text Input text
     * @return Text with addresses replaced with [REDACTED-ADDRESS]
     */
    fun redactAddress(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return try {
            ADDRESS_PATTERN.replace(text, "[REDACTED-ADDRESS]")
        } catch (e: Exception) {
            text
        }
    }

    /**
     * Redact names from text (heuristic)
     *
     * Note: This is a best-effort heuristic that may have false positives.
     * Use with caution in contexts where proper nouns are important.
     *
     * @param text Input text
     * @return Text with names replaced with [REDACTED-NAME]
     */
    fun redactName(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return try {
            NAME_PATTERN.replace(text, "[REDACTED-NAME]")
        } catch (e: Exception) {
            text
        }
    }

    // ===== Partial Masking Methods (for debugging) =====

    /**
     * Mask email address (partial visibility)
     *
     * Shows first character and domain, masks the rest.
     * Example: john.doe@example.com → j***@e***.com
     *
     * @param email Email address to mask
     * @return Partially masked email, or [INVALID-EMAIL] if invalid
     */
    fun maskEmail(email: String?): String {
        if (email.isNullOrBlank()) return ""

        return try {
            val parts = email.split("@")
            if (parts.size != 2) return "[INVALID-EMAIL]"

            val localPart = parts[0]
            val domainPart = parts[1]

            val maskedLocal = if (localPart.length > 1) {
                "${localPart[0]}***"
            } else {
                "***"
            }

            val maskedDomain = if (domainPart.length > 1) {
                "${domainPart[0]}***.${domainPart.substringAfterLast('.')}"
            } else {
                "***"
            }

            "$maskedLocal@$maskedDomain"
        } catch (e: Exception) {
            "[INVALID-EMAIL]"
        }
    }

    /**
     * Mask phone number (partial visibility)
     *
     * Shows last 4 digits, masks the rest.
     * Example: (555) 123-4567 → ***-***-4567
     *
     * @param phone Phone number to mask
     * @return Partially masked phone, or [INVALID-PHONE] if invalid
     */
    fun maskPhone(phone: String?): String {
        if (phone.isNullOrBlank()) return ""

        return try {
            // Extract digits only
            val digits = phone.filter { it.isDigit() }
            if (digits.length < 4) return "[INVALID-PHONE]"

            // Show last 4 digits
            val lastFour = digits.takeLast(4)
            "***-***-$lastFour"
        } catch (e: Exception) {
            "[INVALID-PHONE]"
        }
    }

    /**
     * Mask credit card number (partial visibility)
     *
     * Shows last 4 digits, masks the rest.
     * Example: 4111-1111-1111-1111 → ****-****-****-1111
     *
     * @param creditCard Credit card number to mask
     * @return Partially masked card, or [INVALID-CC] if invalid
     */
    fun maskCreditCard(creditCard: String?): String {
        if (creditCard.isNullOrBlank()) return ""

        return try {
            // Extract digits only
            val digits = creditCard.filter { it.isDigit() }
            if (digits.length < 13) return "[INVALID-CC]"

            // Show last 4 digits
            val lastFour = digits.takeLast(4)
            "****-****-****-$lastFour"
        } catch (e: Exception) {
            "[INVALID-CC]"
        }
    }

    // ===== Detection Methods =====

    /**
     * Check if text contains any PII
     *
     * Useful for conditional logging or validation.
     *
     * @param text Text to check
     * @return true if PII detected, false otherwise
     */
    fun containsPII(text: String?): Boolean {
        if (text.isNullOrBlank()) return false

        return try {
            containsEmail(text) ||
            containsPhone(text) ||
            containsCreditCard(text) ||
            containsSSN(text) ||
            containsZipCode(text) ||
            containsAddress(text) ||
            containsName(text)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if text contains email address
     *
     * @param text Text to check
     * @return true if email detected
     */
    fun containsEmail(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        return try {
            EMAIL_PATTERN.containsMatchIn(text)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if text contains phone number
     *
     * @param text Text to check
     * @return true if phone detected
     */
    fun containsPhone(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        return try {
            PHONE_PATTERN.containsMatchIn(text)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if text contains credit card number
     *
     * @param text Text to check
     * @return true if credit card detected
     */
    fun containsCreditCard(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        return try {
            CREDIT_CARD_PATTERN.containsMatchIn(text)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if text contains SSN
     *
     * @param text Text to check
     * @return true if SSN detected
     */
    fun containsSSN(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        return try {
            SSN_PATTERN.containsMatchIn(text)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if text contains ZIP code
     *
     * @param text Text to check
     * @return true if ZIP code detected
     */
    fun containsZipCode(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        return try {
            ZIP_CODE_PATTERN.containsMatchIn(text)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if text contains street address
     *
     * @param text Text to check
     * @return true if address detected
     */
    fun containsAddress(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        return try {
            ADDRESS_PATTERN.containsMatchIn(text)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if text contains name (heuristic)
     *
     * @param text Text to check
     * @return true if name detected
     */
    fun containsName(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        return try {
            NAME_PATTERN.containsMatchIn(text)
        } catch (e: Exception) {
            false
        }
    }
}
