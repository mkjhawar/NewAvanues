/**
 * TextSanitizers.kt - Text sanitization utilities for security
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-11-17
 */
package com.augmentalis.commandmanager

/**
 * Text sanitization utilities for preventing XSS and injection attacks
 */
object TextSanitizers {

    /**
     * Sanitize XPath expression by removing dangerous content
     *
     * Removes script tags and event handlers to prevent XSS attacks
     * when XPath is used in web contexts.
     *
     * @param xpath The XPath expression to sanitize
     * @return Sanitized XPath safe for use
     */
    fun sanitizeXPath(xpath: String): String {
        var sanitized = xpath

        // Remove script tags
        sanitized = sanitized.replace(
            Regex("<script[^>]*>.*?</script>", RegexOption.IGNORE_CASE),
            ""
        )

        // Remove event handlers
        sanitized = sanitized.replace(
            Regex("on\\w+\\s*=", RegexOption.IGNORE_CASE),
            ""
        )

        return sanitized
    }

    /**
     * Check if a string is safe for use in JavaScript context
     *
     * Detects dangerous patterns that could lead to XSS attacks.
     *
     * @param value The string to check
     * @return true if the string is safe, false if it contains dangerous patterns
     */
    fun isJavaScriptSafe(value: String): Boolean {
        val dangerousPatterns = listOf(
            "<script",
            "javascript:",
            "onerror=",
            "onload=",
            "onclick=",
            "eval(",
            "Function("
        )

        return dangerousPatterns.none { pattern ->
            value.contains(pattern, ignoreCase = true)
        }
    }

    /**
     * Escape a string for safe use in JavaScript
     *
     * Escapes special characters to prevent code injection when
     * strings are embedded in JavaScript code.
     *
     * @param value The string to escape
     * @return Escaped string safe for JavaScript
     */
    fun escapeForJavaScript(value: String): String {
        return value
            .replace("\\", "\\\\")  // Escape backslashes
            .replace("'", "\\'")    // Escape single quotes
            .replace("\"", "\\\"")  // Escape double quotes
            .replace("\n", "\\n")   // Escape newlines
            .replace("\r", "\\r")   // Escape carriage returns
            .replace("\t", "\\t")   // Escape tabs
    }

    /**
     * Escape HTML special characters
     *
     * Prevents HTML injection by escaping special characters.
     *
     * @param value The string to escape
     * @return HTML-escaped string
     */
    fun escapeHtml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    /**
     * Remove all HTML tags from a string
     *
     * Strips HTML tags while preserving text content.
     *
     * @param html The HTML string
     * @return Plain text without HTML tags
     */
    fun stripHtmlTags(html: String): String {
        return html.replace(Regex("<[^>]*>"), "")
    }
}