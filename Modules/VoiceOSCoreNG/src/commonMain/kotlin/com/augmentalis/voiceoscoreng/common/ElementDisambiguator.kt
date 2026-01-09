/**
 * ElementDisambiguator.kt - Handles disambiguation of duplicate UI elements
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-08
 *
 * When a user says "click Submit" and multiple elements match, this class:
 * 1. Finds all matching elements
 * 2. Generates context labels for each (from resourceId, position, parent, etc.)
 * 3. Creates numbered items ONLY for the matching elements
 * 4. Returns disambiguation result for overlay display
 *
 * Numbers are assigned only to duplicate matches, not all screen elements.
 */
package com.augmentalis.voiceoscoreng.common

/**
 * Result of element matching against a voice command.
 *
 * @property query The original voice command text (e.g., "click Submit")
 * @property matches List of elements that matched the query
 * @property needsDisambiguation True if multiple elements matched
 * @property numberedItems Numbered items for overlay display (only populated if disambiguation needed)
 */
data class DisambiguationResult(
    val query: String,
    val matches: List<ElementInfo>,
    val needsDisambiguation: Boolean,
    val numberedItems: List<NumberedMatch>
) {
    /**
     * Single match - can execute directly without user selection.
     */
    val singleMatch: ElementInfo?
        get() = if (matches.size == 1) matches[0] else null

    /**
     * No matches found.
     */
    val noMatches: Boolean
        get() = matches.isEmpty()

    /**
     * Number of matches found.
     */
    val matchCount: Int
        get() = matches.size

    /**
     * Accessibility announcement for the result.
     */
    fun getAccessibilityAnnouncement(): String = when {
        noMatches -> "No elements found matching '$query'"
        !needsDisambiguation -> "Found ${matches[0].voiceLabel}. Executing."
        else -> "$matchCount items match '$query'. Say a number 1 through $matchCount to select."
    }
}

/**
 * A matched element with its assigned number and context label.
 *
 * @property number The number assigned for voice selection (1-based)
 * @property element The matching element
 * @property contextLabel Additional context to help user distinguish (e.g., "iPhone", "top")
 * @property displayLabel Full label for display (e.g., "Submit (iPhone)")
 */
data class NumberedMatch(
    val number: Int,
    val element: ElementInfo,
    val contextLabel: String?,
    val displayLabel: String
) {
    /**
     * Whether this match has a context label for disambiguation.
     */
    val hasContext: Boolean
        get() = !contextLabel.isNullOrBlank()

    /**
     * Bounds of the element for badge positioning.
     */
    val bounds: Bounds
        get() = element.bounds
}

/**
 * Handles disambiguation of duplicate UI elements for voice commands.
 *
 * When a voice command matches multiple elements (e.g., "click Submit" with 3 Submit buttons),
 * this class generates numbered matches with context labels so users can select the correct one.
 *
 * ## Usage
 *
 * ```kotlin
 * val disambiguator = ElementDisambiguator()
 *
 * // User says "click Submit"
 * val result = disambiguator.findMatches(
 *     query = "Submit",
 *     elements = screenElements,
 *     matchMode = MatchMode.CONTAINS
 * )
 *
 * if (result.needsDisambiguation) {
 *     // Show numbered badges only on matching elements
 *     overlay.showDisambiguation(result.numberedItems)
 *
 *     // User says "two"
 *     val selected = result.numberedItems.find { it.number == 2 }
 *     performClick(selected?.element)
 * } else if (result.singleMatch != null) {
 *     // Execute directly
 *     performClick(result.singleMatch)
 * }
 * ```
 *
 * ## Context Label Generation
 *
 * Context labels are derived from (in priority order):
 * 1. resourceId suffix (e.g., "btn_iphone_submit" → "iPhone")
 * 2. contentDescription differences
 * 3. Parent/sibling context
 * 4. Screen position (top, middle, bottom)
 *
 * @property contextStrategy Strategy for generating context labels
 */
class ElementDisambiguator(
    private val contextStrategy: ContextStrategy = ContextStrategy.AUTO
) {
    /**
     * Strategy for generating context labels.
     */
    enum class ContextStrategy {
        /** Automatically choose best context source */
        AUTO,
        /** Use resourceId-based context only */
        RESOURCE_ID,
        /** Use position-based context only */
        POSITION,
        /** Use parent element context */
        PARENT,
        /** No context labels */
        NONE
    }

    /**
     * How to match elements against the query.
     */
    enum class MatchMode {
        /** Element text/label exactly equals query */
        EXACT,
        /** Element text/label contains query (case-insensitive) */
        CONTAINS,
        /** Element text/label starts with query */
        STARTS_WITH,
        /** Fuzzy matching with tolerance for typos */
        FUZZY
    }

    /**
     * Find all elements matching the voice command query.
     *
     * @param query The text to match (e.g., "Submit", "Cancel")
     * @param elements All elements on the current screen
     * @param matchMode How to match elements against the query
     * @return DisambiguationResult with matches and numbered items if disambiguation needed
     */
    fun findMatches(
        query: String,
        elements: List<ElementInfo>,
        matchMode: MatchMode = MatchMode.CONTAINS
    ): DisambiguationResult {
        val queryLower = query.lowercase().trim()

        // Find all matching elements
        val matches = elements.filter { element ->
            element.isEnabled && matchesQuery(element, queryLower, matchMode)
        }

        // If 0 or 1 match, no disambiguation needed
        if (matches.size <= 1) {
            return DisambiguationResult(
                query = query,
                matches = matches,
                needsDisambiguation = false,
                numberedItems = emptyList()
            )
        }

        // Multiple matches - generate numbered items with context
        val numberedItems = matches.mapIndexed { index, element ->
            val contextLabel = generateContextLabel(element, matches, index)
            val displayLabel = if (contextLabel != null) {
                "${element.voiceLabel} ($contextLabel)"
            } else {
                element.voiceLabel
            }

            NumberedMatch(
                number = index + 1,
                element = element,
                contextLabel = contextLabel,
                displayLabel = displayLabel
            )
        }

        return DisambiguationResult(
            query = query,
            matches = matches,
            needsDisambiguation = true,
            numberedItems = numberedItems
        )
    }

    /**
     * Select a match by number.
     *
     * @param result The disambiguation result
     * @param number The number spoken by user (1-based)
     * @return The selected element, or null if invalid number
     */
    fun selectByNumber(result: DisambiguationResult, number: Int): ElementInfo? {
        return result.numberedItems.find { it.number == number }?.element
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Private Implementation
    // ═══════════════════════════════════════════════════════════════════════════

    private fun matchesQuery(element: ElementInfo, queryLower: String, mode: MatchMode): Boolean {
        val label = element.voiceLabel.lowercase()
        val text = element.text.lowercase()
        val description = element.contentDescription.lowercase()

        return when (mode) {
            MatchMode.EXACT -> label == queryLower || text == queryLower
            MatchMode.CONTAINS -> label.contains(queryLower) || text.contains(queryLower) || description.contains(queryLower)
            MatchMode.STARTS_WITH -> label.startsWith(queryLower) || text.startsWith(queryLower)
            MatchMode.FUZZY -> fuzzyMatch(label, queryLower) || fuzzyMatch(text, queryLower)
        }
    }

    private fun fuzzyMatch(text: String, query: String): Boolean {
        // Simple fuzzy matching - allow 1 character difference for short strings
        if (text == query) return true
        if (text.contains(query)) return true

        // Levenshtein distance check for typo tolerance
        val maxDistance = when {
            query.length <= 3 -> 1
            query.length <= 6 -> 2
            else -> 3
        }

        return levenshteinDistance(text, query) <= maxDistance
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[s1.length][s2.length]
    }

    private fun generateContextLabel(
        element: ElementInfo,
        allMatches: List<ElementInfo>,
        index: Int
    ): String? {
        return when (contextStrategy) {
            ContextStrategy.NONE -> null
            ContextStrategy.RESOURCE_ID -> extractResourceIdContext(element)
            ContextStrategy.POSITION -> getPositionContext(element, allMatches, index)
            ContextStrategy.PARENT -> extractParentContext(element)
            ContextStrategy.AUTO -> {
                // Try each strategy in order until we get a useful context
                extractResourceIdContext(element)
                    ?: extractParentContext(element)
                    ?: getPositionContext(element, allMatches, index)
            }
        }
    }

    /**
     * Extract parent context from resourceId or contentDescription.
     * Since ElementInfo doesn't track parent hierarchy, we infer from naming patterns.
     */
    private fun extractParentContext(element: ElementInfo): String? {
        // Try to extract parent context from resourceId naming convention
        // e.g., "form_submit_button" -> "Form" or "dialog_cancel" -> "Dialog"
        val resourceId = element.resourceId
        if (resourceId.isNotBlank()) {
            val idPart = resourceId.substringAfterLast("/").substringAfterLast(":")
            val parts = idPart.split("_", "-").filter { it.isNotBlank() }

            // Common parent indicators at the start
            val parentIndicators = setOf("form", "dialog", "header", "footer", "toolbar", "nav", "menu", "card", "list", "item")
            val parentWord = parts.firstOrNull { it.lowercase() in parentIndicators }

            if (parentWord != null) {
                return parentWord.replaceFirstChar { char -> char.uppercase() }
            }
        }
        return null
    }

    /**
     * Extract context from resourceId.
     *
     * Examples:
     * - "btn_iphone_submit" → "iPhone"
     * - "payment_submit_button" → "Payment"
     * - "com.app:id/form_submit" → "Form"
     */
    private fun extractResourceIdContext(element: ElementInfo): String? {
        val resourceId = element.resourceId
        if (resourceId.isBlank()) return null

        // Extract the ID part after the last / or :
        val idPart = resourceId.substringAfterLast("/").substringAfterLast(":")

        // Split by underscores and find context words
        val parts = idPart.split("_", "-")
            .filter { it.isNotBlank() }
            .map { it.lowercase() }

        // Skip common suffixes/prefixes
        val skipWords = setOf("btn", "button", "submit", "cancel", "ok", "click", "text", "view", "layout", "container")

        // Find a meaningful context word
        val contextWord = parts.firstOrNull { it !in skipWords && it.length > 2 }

        return contextWord?.replaceFirstChar { it.uppercase() }
    }

    /**
     * Get position-based context (top, middle, bottom).
     */
    private fun getPositionContext(
        element: ElementInfo,
        allMatches: List<ElementInfo>,
        index: Int
    ): String? {
        if (allMatches.size < 2) return null

        // Sort by Y position
        val sortedByY = allMatches.sortedBy { it.bounds.top }
        val positionIndex = sortedByY.indexOf(element)

        return when {
            allMatches.size == 2 -> if (positionIndex == 0) "top" else "bottom"
            allMatches.size == 3 -> when (positionIndex) {
                0 -> "top"
                1 -> "middle"
                else -> "bottom"
            }
            else -> {
                // For more items, use ordinal position
                when (positionIndex) {
                    0 -> "first"
                    allMatches.lastIndex -> "last"
                    else -> "item ${positionIndex + 1}"
                }
            }
        }
    }

    companion object {
        /**
         * Shared instance with default configuration.
         */
        val default = ElementDisambiguator()
    }
}
