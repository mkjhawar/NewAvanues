/**
 * TextUtils.kt - General text manipulation utilities
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-11-17
 */
package com.augmentalis.voiceos.text

/**
 * General text manipulation utilities
 */
object TextUtils {

    /**
     * Truncate text to specified length with ellipsis
     *
     * @param text The text to truncate
     * @param maxLength Maximum length
     * @param ellipsis The ellipsis string (default "...")
     * @return Truncated text with ellipsis if needed
     */
    fun truncate(text: String, maxLength: Int, ellipsis: String = "..."): String {
        return if (text.length <= maxLength) {
            text
        } else {
            text.take(maxLength - ellipsis.length) + ellipsis
        }
    }

    /**
     * Capitalize first letter of each word
     *
     * @param text The text to capitalize
     * @return Text with each word capitalized
     */
    fun capitalizeWords(text: String): String {
        return text.split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    /**
     * Remove extra whitespace and trim
     *
     * Collapses multiple spaces into single spaces and trims.
     *
     * @param text The text to normalize
     * @return Normalized text
     */
    fun normalizeWhitespace(text: String): String {
        return text.replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Check if string contains only alphanumeric characters
     *
     * @param text The text to check
     * @return true if only contains letters and digits
     */
    fun isAlphanumeric(text: String): Boolean {
        return text.all { it.isLetterOrDigit() }
    }

    /**
     * Extract numbers from text
     *
     * @param text The text containing numbers
     * @return List of numbers found in the text
     */
    fun extractNumbers(text: String): List<Int> {
        return Regex("\\d+").findAll(text)
            .map { it.value.toIntOrNull() }
            .filterNotNull()
            .toList()
    }

    /**
     * Count words in text
     *
     * @param text The text to count words in
     * @return Number of words
     */
    fun wordCount(text: String): Int {
        return if (text.isBlank()) {
            0
        } else {
            normalizeWhitespace(text).split(" ").size
        }
    }
}

/**
 * Extension function for find and replace with map
 *
 * Replaces multiple strings according to a replacement map.
 *
 * @param replacements Map of strings to replace
 * @return Text with replacements applied
 */
fun String.replaceAll(replacements: Map<String, String>): String {
    var result = this
    for ((key, value) in replacements) {
        result = result.replace(key, value)
    }
    return result
}

/**
 * Extension function to check if string is a valid email
 *
 * Basic email validation using regex pattern.
 *
 * @return true if string appears to be a valid email
 */
fun String.isValidEmail(): Boolean {
    val emailRegex = Regex(
        "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
    )
    return emailRegex.matches(this)
}

/**
 * Extension function to check if string is a valid phone number
 *
 * Basic phone validation for common formats.
 *
 * @return true if string appears to be a valid phone number
 */
fun String.isValidPhone(): Boolean {
    val phoneRegex = Regex(
        "^[+]?[(]?[0-9]{1,3}[)]?[-\\s.]?[(]?[0-9]{1,3}[)]?[-\\s.]?[0-9]{3,4}[-\\s.]?[0-9]{4}$"
    )
    return phoneRegex.matches(this)
}