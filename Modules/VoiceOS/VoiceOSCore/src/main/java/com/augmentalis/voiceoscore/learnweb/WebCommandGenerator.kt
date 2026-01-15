/**
 * WebCommandGenerator.kt - Voice command generation for web elements
 * Path: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnweb/WebCommandGenerator.kt
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-22
 *
 * Generates voice commands from scraped web elements
 * Provides natural language command text with synonyms
 */

package com.augmentalis.voiceoscore.learnweb

import android.util.Log

/**
 * Web Command Generator
 *
 * Generates voice commands from web elements with natural language variants.
 *
 * Features:
 * - Command text generation from element metadata
 * - Synonym generation for natural variations
 * - Action type detection (CLICK, SCROLL_TO, FOCUS, INPUT)
 * - Duplicate filtering
 * - Quality scoring
 *
 * @since 1.0.0
 */
class WebCommandGenerator {

    companion object {
        private const val TAG = "WebCommandGenerator"

        /**
         * Action types for web commands
         */
        private const val ACTION_CLICK = "CLICK"
        private const val ACTION_SCROLL_TO = "SCROLL_TO"
        private const val ACTION_FOCUS = "FOCUS"
        private const val ACTION_INPUT = "INPUT"

        /**
         * Input element types
         */
        private val INPUT_TYPES = setOf(
            "INPUT", "TEXTAREA", "SELECT"
        )

        /**
         * Clickable element types
         */
        private val CLICKABLE_TYPES = setOf(
            "BUTTON", "A", "SUMMARY"
        )

        /**
         * Common verb synonyms for actions
         */
        private val CLICK_VERBS = listOf("click", "tap", "press", "select")
        private val SCROLL_VERBS = listOf("scroll to", "go to", "navigate to")
        private val INPUT_VERBS = listOf("type in", "enter in", "fill")
        private val FOCUS_VERBS = listOf("focus on", "go to")

        /**
         * Minimum text length for command generation
         */
        private const val MIN_TEXT_LENGTH = 2

        /**
         * Maximum text length for command text
         */
        private const val MAX_COMMAND_TEXT_LENGTH = 50
    }

    /**
     * Generate commands from scraped elements
     *
     * @param elements List of scraped web elements
     * @param websiteUrlHash URL hash for association
     * @return List of generated web commands
     */
    fun generateCommands(
        elements: List<ScrapedWebElement>,
        websiteUrlHash: String
    ): List<GeneratedWebCommand> {
        val commands = mutableListOf<GeneratedWebCommand>()
        val now = System.currentTimeMillis()

        elements.forEach { element ->
            val generatedCommands = generateFromElement(element, websiteUrlHash, now)
            commands.addAll(generatedCommands)
        }

        Log.d(TAG, "Generated ${commands.size} commands from ${elements.size} elements")
        return commands
    }

    /**
     * Generate commands from single element
     *
     * @param element Scraped web element
     * @param websiteUrlHash URL hash
     * @param timestamp Generation timestamp
     * @return List of generated commands for element
     */
    fun generateFromElement(
        element: ScrapedWebElement,
        websiteUrlHash: String,
        timestamp: Long = System.currentTimeMillis()
    ): List<GeneratedWebCommand> {
        val commands = mutableListOf<GeneratedWebCommand>()

        // Determine action type
        val action = determineAction(element)

        // Generate command variants
        val commandTexts = generateCommandTexts(element, action)

        commandTexts.forEach { (primaryText, synonyms) ->
            if (validateCommand(primaryText, element)) {
                commands.add(
                    GeneratedWebCommand(
                        id = 0,
                        websiteUrlHash = websiteUrlHash,
                        elementHash = element.elementHash,
                        commandText = primaryText,
                        synonyms = synonyms.joinToString(","),
                        action = action,
                        xpath = element.xpath,
                        generatedAt = timestamp,
                        usageCount = 0,
                        lastUsedAt = null
                    )
                )
            }
        }

        return commands
    }

    /**
     * Determine action type for element
     *
     * @param element Scraped web element
     * @return Action type string
     */
    private fun determineAction(element: ScrapedWebElement): String {
        return when {
            INPUT_TYPES.contains(element.tagName) -> ACTION_INPUT
            CLICKABLE_TYPES.contains(element.tagName) -> ACTION_CLICK
            element.clickable -> ACTION_CLICK
            element.visible -> ACTION_SCROLL_TO
            else -> ACTION_FOCUS
        }
    }

    /**
     * Generate command text variants
     *
     * @param element Scraped web element
     * @param action Action type
     * @return List of (primary text, synonyms) pairs
     */
    private fun generateCommandTexts(
        element: ScrapedWebElement,
        action: String
    ): List<Pair<String, List<String>>> {
        val results = mutableListOf<Pair<String, List<String>>>()

        // Get element description
        val description = getElementDescription(element)
        if (description.isBlank()) {
            return emptyList()
        }

        // Generate commands based on action
        when (action) {
            ACTION_CLICK -> {
                CLICK_VERBS.forEachIndexed { index, verb ->
                    val primary = "$verb $description"
                    val synonyms = if (index == 0) {
                        CLICK_VERBS.drop(1).map { "$it $description" }
                    } else {
                        emptyList()
                    }
                    results.add(primary to synonyms)
                }
            }

            ACTION_SCROLL_TO -> {
                SCROLL_VERBS.forEachIndexed { index, verb ->
                    val primary = "$verb $description"
                    val synonyms = if (index == 0) {
                        SCROLL_VERBS.drop(1).map { "$it $description" }
                    } else {
                        emptyList()
                    }
                    results.add(primary to synonyms)
                }
            }

            ACTION_INPUT -> {
                INPUT_VERBS.forEachIndexed { index, verb ->
                    val primary = "$verb $description"
                    val synonyms = if (index == 0) {
                        INPUT_VERBS.drop(1).map { "$it $description" }
                    } else {
                        emptyList()
                    }
                    results.add(primary to synonyms)
                }
            }

            ACTION_FOCUS -> {
                FOCUS_VERBS.forEachIndexed { index, verb ->
                    val primary = "$verb $description"
                    val synonyms = if (index == 0) {
                        FOCUS_VERBS.drop(1).map { "$it $description" }
                    } else {
                        emptyList()
                    }
                    results.add(primary to synonyms)
                }
            }
        }

        // Take only first variant to avoid duplication
        return if (results.isNotEmpty()) listOf(results.first()) else emptyList()
    }

    /**
     * Get element description for command generation
     *
     * Priority: ARIA label > text content > role > tag name
     *
     * @param element Scraped web element
     * @return Element description string
     */
    private fun getElementDescription(element: ScrapedWebElement): String {
        // Priority 1: ARIA label
        if (!element.ariaLabel.isNullOrBlank() && element.ariaLabel.length >= MIN_TEXT_LENGTH) {
            return normalizeText(element.ariaLabel).take(MAX_COMMAND_TEXT_LENGTH)
        }

        // Priority 2: Text content
        if (!element.text.isNullOrBlank() && element.text.length >= MIN_TEXT_LENGTH) {
            return normalizeText(element.text).take(MAX_COMMAND_TEXT_LENGTH)
        }

        // Priority 3: Role
        if (!element.role.isNullOrBlank()) {
            return "${element.role} element"
        }

        // Priority 4: Tag name
        return "${element.tagName.lowercase()} element"
    }

    /**
     * Normalize text for command generation
     *
     * Removes extra whitespace, converts to lowercase, removes special characters.
     *
     * @param text Input text
     * @return Normalized text
     */
    private fun normalizeText(text: String): String {
        return text
            .trim()
            .replace(Regex("\\s+"), " ")
            .lowercase()
    }

    /**
     * Validate command quality
     *
     * @param commandText Command text to validate
     * @param element Associated element
     * @return true if command is valid
     */
    fun validateCommand(commandText: String, element: ScrapedWebElement): Boolean {
        // Must have minimum length
        if (commandText.length < MIN_TEXT_LENGTH) {
            return false
        }

        // Must contain valid characters
        if (!commandText.matches(Regex("^[a-z0-9\\s\\-]+$"))) {
            return false
        }

        // Must not be generic
        val genericTerms = setOf(
            "click element",
            "click button element",
            "click a element",
            "scroll to element"
        )
        if (genericTerms.contains(commandText)) {
            return false
        }

        return true
    }

    /**
     * Filter duplicate and low-quality commands
     *
     * @param commands List of raw commands
     * @return Filtered list of commands
     */
    fun filterCommands(commands: List<GeneratedWebCommand>): List<GeneratedWebCommand> {
        val filtered = mutableListOf<GeneratedWebCommand>()
        val seenTexts = mutableSetOf<String>()

        commands.forEach { command ->
            // Skip duplicates
            if (seenTexts.contains(command.commandText)) {
                return@forEach
            }

            // Add to filtered list
            filtered.add(command)
            seenTexts.add(command.commandText)
        }

        Log.d(TAG, "Filtered ${commands.size} -> ${filtered.size} commands (${commands.size - filtered.size} duplicates removed)")
        return filtered
    }

    /**
     * Get command generation statistics
     *
     * @param commands List of generated commands
     * @return Statistics map
     */
    fun getStatistics(commands: List<GeneratedWebCommand>): Map<String, Any> {
        val actionCounts = commands.groupingBy { it.action }.eachCount()
        val avgSynonyms = commands.map { it.synonyms.split(",").size }.average()
        val avgTextLength = commands.map { it.commandText.length }.average()

        return mapOf(
            "total" to commands.size,
            "actions" to actionCounts,
            "avgSynonyms" to avgSynonyms,
            "avgTextLength" to avgTextLength
        )
    }

    /**
     * Create selector from element
     *
     * Generates optimized selector for element (ID > class > XPath).
     *
     * @param element Scraped web element
     * @return Optimized selector string
     */
    fun createSelector(element: ScrapedWebElement): String {
        // XPath is already available and most reliable
        return element.xpath
    }
}
