package com.augmentalis.intentactions

/**
 * Central URI sanitization for IntentActions.
 * Prevents URI injection attacks from NLU-extracted strings.
 */
object UriSanitizer {
    private val ALLOWED_WEB_SCHEMES = setOf("https", "http")
    private val FORBIDDEN_SCHEMES = setOf("javascript", "intent", "data", "file", "content", "vnd")

    private val EMAIL_REGEX = Regex("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-z]{2,}$", RegexOption.IGNORE_CASE)
    private val PHONE_REGEX = Regex("^[+\\d()\\-\\s]+$")

    /**
     * Sanitize a URL for navigation. Enforces HTTPS, blocks dangerous schemes.
     * @return sanitized HTTPS URL or null if invalid/dangerous
     */
    fun sanitizeWebUrl(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return null

        // Block forbidden schemes
        val lowerUrl = trimmed.lowercase()
        if (FORBIDDEN_SCHEMES.any { lowerUrl.startsWith("$it:") }) return null

        // Upgrade http to https
        return when {
            lowerUrl.startsWith("https://") -> trimmed
            lowerUrl.startsWith("http://") -> "https://${trimmed.substring("http://".length)}"
            trimmed.contains(".") -> "https://$trimmed"  // bare domain
            else -> null
        }
    }

    /**
     * Validate an email address. Rejects addresses with URI manipulation characters.
     * @return true if email is safe for mailto: URI construction
     */
    fun isValidEmail(email: String): Boolean {
        return EMAIL_REGEX.matches(email.trim()) &&
                !email.contains("?") &&
                !email.contains("&") &&
                !email.contains("#")
    }

    /**
     * Sanitize a phone number for tel: URI. Strips non-phone characters.
     * Blocks USSD codes (* and # patterns).
     * @return sanitized phone string or null if invalid
     */
    fun sanitizePhoneNumber(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return null

        // Block USSD codes (patterns like *#, *21*, ##002#, etc.) before stripping
        if (trimmed.contains('*') || trimmed.contains('#')) return null

        // Strip everything except digits, +, -, (, ), spaces
        val cleaned = trimmed.filter { it.isDigit() || it == '+' || it == '-' || it == '(' || it == ')' || it == ' ' }
        if (cleaned.isBlank()) return null

        // Must have at least 3 digits
        val digitCount = cleaned.count { it.isDigit() }
        if (digitCount < 3) return null

        return cleaned
    }

    /**
     * Sanitize an SMS address. Only allows phone number characters.
     * @return sanitized address or null if invalid
     */
    fun sanitizeSmsAddress(input: String): String? = sanitizePhoneNumber(input)
}
