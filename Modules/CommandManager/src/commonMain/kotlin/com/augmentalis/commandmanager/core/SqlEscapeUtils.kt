/**
 * SqlEscapeUtils.kt - SQL escaping utilities for safe LIKE query patterns
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-09
 * Extracted to KMP: 2025-11-16
 *
 * ## Problem
 * User-controlled text in SQL LIKE queries can contain wildcards (%, _) that cause
 * unintended matches. For example, searching for "50% discount" would match "50X discount",
 * "50 discount", etc.
 *
 * ## Solution
 * Escape SQL LIKE wildcards before using in queries:
 * - `%` (match any characters) → `\%` (literal percent)
 * - `_` (match single character) → `\_` (literal underscore)
 * - `\` (escape character) → `\\` (literal backslash)
 *
 * ## Usage
 * ```kotlin
 * // In DAO, before executing LIKE query:
 * val safeText = SqlEscapeUtils.escapeLikePattern(userInput)
 * val query = "SELECT * FROM table WHERE text LIKE '%' || :safeText || '%' ESCAPE '\\'"
 * ```
 *
 * ## Important
 * Queries using escaped patterns MUST include `ESCAPE '\'` clause in SQL.
 */
package com.augmentalis.commandmanager

/**
 * SQL escaping utilities
 *
 * Provides safe escaping of user input for SQL LIKE queries to prevent
 * unintended wildcard matches.
 */
object SqlEscapeUtils {

    /**
     * Escape SQL LIKE wildcards in user input
     *
     * Escapes the following characters:
     * - `\` → `\\` (must be first to avoid double-escaping)
     * - `%` → `\%`
     * - `_` → `\_`
     *
     * @param pattern The user input to escape
     * @return The escaped pattern safe for use in LIKE queries
     *
     * Example:
     * ```kotlin
     * escapeLikePattern("50% off") // Returns "50\% off"
     * escapeLikePattern("user_name") // Returns "user\_name"
     * escapeLikePattern("C:\\path") // Returns "C:\\\\path"
     * ```
     */
    fun escapeLikePattern(pattern: String): String {
        if (pattern.isEmpty()) {
            return pattern
        }

        return pattern
            .replace("\\", "\\\\") // Backslash must be first
            .replace("%", "\\%")
            .replace("_", "\\_")
    }

    /**
     * Check if a pattern contains SQL wildcards
     *
     * @param pattern Pattern to check
     * @return true if pattern contains %, _, or \ characters
     */
    fun containsWildcards(pattern: String): Boolean {
        return pattern.contains('%') || pattern.contains('_') || pattern.contains('\\')
    }

    /**
     * Escape and wrap pattern with wildcards for partial matching
     *
     * Escapes the pattern and wraps it with % wildcards for LIKE queries.
     *
     * @param pattern User input to escape and wrap
     * @return Escaped pattern wrapped with % wildcards
     *
     * Example:
     * ```kotlin
     * wrapWithWildcards("john") // Returns "%john%"
     * wrapWithWildcards("50% off") // Returns "%50\% off%"
     * ```
     */
    fun wrapWithWildcards(pattern: String): String {
        val escaped = escapeLikePattern(pattern)
        return "%$escaped%"
    }

    /**
     * Escape and prefix pattern with wildcard for suffix matching
     *
     * @param pattern User input to escape and prefix
     * @return Escaped pattern prefixed with % wildcard
     *
     * Example:
     * ```kotlin
     * prefixWithWildcard("@example.com") // Returns "%@example.com"
     * ```
     */
    fun prefixWithWildcard(pattern: String): String {
        val escaped = escapeLikePattern(pattern)
        return "%$escaped"
    }

    /**
     * Escape and suffix pattern with wildcard for prefix matching
     *
     * @param pattern User input to escape and suffix
     * @return Escaped pattern suffixed with % wildcard
     *
     * Example:
     * ```kotlin
     * suffixWithWildcard("prefix_") // Returns "prefix\_%"
     * ```
     */
    fun suffixWithWildcard(pattern: String): String {
        val escaped = escapeLikePattern(pattern)
        return "$escaped%"
    }
}
