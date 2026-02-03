package com.augmentalis.avucodec.core

/**
 * AVU Escape Utilities - Canonical Implementation
 *
 * This is the SINGLE SOURCE OF TRUTH for AVU format escape/unescape operations.
 * All modules in the Avanues ecosystem should use this implementation.
 *
 * The AVU format uses colon (:) as the field delimiter. Special characters
 * must be percent-encoded to prevent parsing ambiguity:
 *
 * | Character | Encoded | Purpose              |
 * |-----------|---------|----------------------|
 * | `%`       | `%25`   | Escape character     |
 * | `:`       | `%3A`   | Field delimiter      |
 * | `\n`      | `%0A`   | Line feed            |
 * | `\r`      | `%0D`   | Carriage return      |
 *
 * IMPORTANT: Order matters!
 * - When escaping: `%` MUST be escaped FIRST (otherwise `%3A` becomes `%253A`)
 * - When unescaping: `%` MUST be unescaped LAST (to correctly decode `%25`)
 *
 * @author Augmentalis Engineering
 * @since AVU 2.2
 */
object AvuEscape {

    /** Escape sequence for percent sign */
    const val PERCENT_ENCODED = "%25"

    /** Escape sequence for colon (delimiter) */
    const val COLON_ENCODED = "%3A"

    /** Escape sequence for line feed */
    const val LF_ENCODED = "%0A"

    /** Escape sequence for carriage return */
    const val CR_ENCODED = "%0D"

    /**
     * Escape special characters in a value for safe AVU transmission.
     *
     * Use this when encoding field values that may contain reserved characters.
     *
     * Example:
     * ```kotlin
     * val url = "https://example.com:8080/path"
     * val escaped = AvuEscape.escape(url)
     * // Result: "https%3A//example.com%3A8080/path"
     * ```
     *
     * @param value The raw string value to escape
     * @return The escaped string safe for AVU format
     */
    fun escape(value: String): String {
        return value
            .replace("%", PERCENT_ENCODED)  // Must be first
            .replace(":", COLON_ENCODED)
            .replace("\n", LF_ENCODED)
            .replace("\r", CR_ENCODED)
    }

    /**
     * Unescape a percent-encoded AVU value back to its original form.
     *
     * Use this when parsing field values from AVU messages.
     *
     * Example:
     * ```kotlin
     * val escaped = "https%3A//example.com%3A8080/path"
     * val original = AvuEscape.unescape(escaped)
     * // Result: "https://example.com:8080/path"
     * ```
     *
     * @param value The escaped string from AVU format
     * @return The original unescaped string
     */
    fun unescape(value: String): String {
        return value
            .replace(CR_ENCODED, "\r")
            .replace(LF_ENCODED, "\n")
            .replace(COLON_ENCODED, ":")
            .replace(PERCENT_ENCODED, "%")  // Must be last
    }

    /**
     * Check if a string contains characters that need escaping.
     *
     * Useful for optimization when you want to avoid unnecessary
     * string allocations for values that don't need escaping.
     *
     * @param value The string to check
     * @return true if the string contains reserved characters
     */
    fun needsEscaping(value: String): Boolean {
        return value.any { it == '%' || it == ':' || it == '\n' || it == '\r' }
    }

    /**
     * Check if a string appears to be already escaped.
     *
     * This is a heuristic check - it looks for common escape sequences.
     * Not 100% reliable for edge cases.
     *
     * @param value The string to check
     * @return true if the string appears to contain escape sequences
     */
    fun isEscaped(value: String): Boolean {
        return value.contains(PERCENT_ENCODED) ||
               value.contains(COLON_ENCODED) ||
               value.contains(LF_ENCODED) ||
               value.contains(CR_ENCODED)
    }

    /**
     * Escape a value only if it needs escaping.
     *
     * Optimization for cases where most values don't need escaping.
     *
     * @param value The raw string value
     * @return The escaped string, or the original if no escaping needed
     */
    fun escapeIfNeeded(value: String): String {
        return if (needsEscaping(value)) escape(value) else value
    }
}
