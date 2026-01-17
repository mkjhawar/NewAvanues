/**
 * SynonymEntry.kt - Data class for synonym entries
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-08
 *
 * Represents a canonical action with its associated synonyms.
 */
package com.augmentalis.voiceoscore

/**
 * A synonym entry mapping synonyms to a canonical action.
 *
 * Example:
 * ```
 * SynonymEntry(
 *     canonical = "click",
 *     synonyms = listOf("tap", "press", "push", "hit", "select")
 * )
 * ```
 *
 * @property canonical The canonical action name (e.g., "click", "scroll_up")
 * @property synonyms List of synonym words that map to this canonical action
 */
data class SynonymEntry(
    val canonical: String,
    val synonyms: List<String>
) {
    /**
     * All words associated with this entry (canonical + synonyms).
     */
    val allWords: List<String> by lazy {
        listOf(canonical) + synonyms
    }

    /**
     * Check if a word matches this entry (canonical or any synonym).
     */
    fun matches(word: String): Boolean {
        val normalized = word.lowercase().trim()
        return canonical.lowercase() == normalized ||
               synonyms.any { it.lowercase() == normalized }
    }

    companion object {
        /**
         * Parse a synonym entry from .syn format line.
         *
         * Format: `canonical | synonym1, synonym2, synonym3`
         *
         * @param line The line to parse
         * @return SynonymEntry or null if line is invalid/comment
         */
        fun parse(line: String): SynonymEntry? {
            val trimmed = line.trim()

            // Skip empty lines and comments
            if (trimmed.isBlank() || trimmed.startsWith("#") || trimmed.startsWith("@")) {
                return null
            }

            // Split by pipe
            val parts = trimmed.split("|")
            if (parts.size != 2) {
                return null
            }

            val canonical = parts[0].trim().lowercase()
            if (canonical.isBlank()) {
                return null
            }

            val synonyms = parts[1]
                .split(",")
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() }

            return SynonymEntry(canonical, synonyms)
        }
    }
}
