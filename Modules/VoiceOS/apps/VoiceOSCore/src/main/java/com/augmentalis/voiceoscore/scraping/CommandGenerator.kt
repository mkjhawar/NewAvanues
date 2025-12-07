/**
 * CommandGenerator.kt - NLP-based voice command generator
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 * Modified: 2025-10-18 (Phase 3: State-aware command generation)
 */
package com.augmentalis.voiceoscore.scraping

import android.content.Context
import com.augmentalis.voiceoscore.database.VoiceOSAppDatabase
import com.augmentalis.voiceoscore.scraping.entities.GeneratedCommandEntity
import com.augmentalis.voiceoscore.scraping.entities.ScrapedElementEntity
import com.augmentalis.voiceoscore.scraping.entities.StateType
import org.json.JSONArray

/**
 * Command Generator
 *
 * Automatically generates voice commands from scraped UI elements.
 *
 * Command Generation Strategy:
 * 1. Extract meaningful text from element (text, contentDescription, viewId)
 * 2. Determine appropriate action type (click, type, scroll, etc.)
 * 3. Generate primary command phrase
 * 4. Generate synonyms using NLP rules
 * 5. Calculate confidence score based on text quality
 *
 * Example:
 * Element: Button with text "Submit"
 * Generated:
 * - Primary: "click submit"
 * - Synonyms: ["tap submit", "press submit", "send", "submit button"]
 * - Confidence: 0.95 (high - clear text label)
 */
class CommandGenerator(private val context: Context) {

    // CoT: Use unified database (migrated from AppScrapingDatabase)
    private val database: VoiceOSAppDatabase = VoiceOSAppDatabase.getInstance(context)

    companion object {
        private const val TAG = "CommandGenerator"

        // Minimum confidence threshold for command generation
        private const val MIN_CONFIDENCE = 0.2f

        // Action verbs for different element types
        private val CLICK_VERBS = listOf("click", "tap", "press", "select", "activate")
        private val INPUT_VERBS = listOf("type", "enter", "input", "write")
        private val SCROLL_VERBS = listOf("scroll", "swipe", "move")
        private val LONG_CLICK_VERBS = listOf("long press", "hold", "long click")
        private val FOCUS_VERBS = listOf("focus", "highlight", "go to")

        // Common button text synonyms
        private val BUTTON_SYNONYMS = mapOf(
            "submit" to listOf("send", "post", "confirm", "ok"),
            "cancel" to listOf("close", "dismiss", "exit", "back"),
            "next" to listOf("continue", "forward", "proceed", "advance"),
            "previous" to listOf("back", "backward", "return", "prior"),
            "save" to listOf("store", "keep", "preserve"),
            "delete" to listOf("remove", "erase", "clear"),
            "edit" to listOf("modify", "change", "update"),
            "search" to listOf("find", "look for", "locate"),
            "login" to listOf("sign in", "log in", "enter"),
            "logout" to listOf("sign out", "log out", "exit"),
            "share" to listOf("send", "forward", "distribute"),
            "refresh" to listOf("reload", "update", "renew"),
            "settings" to listOf("options", "preferences", "configuration"),
            "help" to listOf("assistance", "support", "info")
        )
    }

    /**
     * Generate commands for a scraped element
     *
     * @param element ScrapedElementEntity to generate commands for
     * @return List of generated commands (may be empty if element has no actionable text)
     */
    suspend fun generateCommands(element: ScrapedElementEntity): List<GeneratedCommandEntity> {
        val commands = mutableListOf<GeneratedCommandEntity>()

        // Extract meaningful text from element
        val elementText = extractElementText(element) ?: return emptyList()

        // Generate commands based on element capabilities
        if (element.isClickable) {
            commands.addAll(generateClickCommands(element, elementText))
        }

        if (element.isLongClickable) {
            commands.addAll(generateLongClickCommands(element, elementText))
        }

        if (element.isEditable) {
            commands.addAll(generateInputCommands(element, elementText))
        }

        if (element.isScrollable) {
            commands.addAll(generateScrollCommands(element, elementText))
        }

        if (element.isFocusable && !element.isClickable) {
            commands.addAll(generateFocusCommands(element, elementText))
        }

        // Filter out low-confidence commands
        return commands.filter { it.confidence >= MIN_CONFIDENCE }
    }

    /**
     * Extract meaningful text from element for command generation
     *
     * Priority: text > contentDescription > viewId
     */
    private fun extractElementText(element: ScrapedElementEntity): String? {
        return when {
            !element.text.isNullOrBlank() -> element.text.trim()
            !element.contentDescription.isNullOrBlank() -> element.contentDescription.trim()
            !element.viewIdResourceName.isNullOrBlank() -> {
                // Extract readable part from view ID (e.g., "submit_button" from "com.example:id/submit_button")
                element.viewIdResourceName
                    .substringAfterLast('/')
                    .replace('_', ' ')
                    .trim()
            }
            else -> null
        }
    }

    /**
     * Generate click commands
     */
    private fun generateClickCommands(
        element: ScrapedElementEntity,
        text: String
    ): List<GeneratedCommandEntity> {
        val commands = mutableListOf<GeneratedCommandEntity>()
        val normalizedText = text.lowercase().trim()

        // Calculate confidence based on text quality
        val confidence = calculateConfidence(text, element)

        // Generate primary command: "click [text]"
        val primaryCommand = "click $normalizedText"
        val synonyms = generateClickSynonyms(normalizedText)

        commands.add(
            GeneratedCommandEntity(
                elementHash = element.elementHash,
                commandText = primaryCommand,
                actionType = "click",
                confidence = confidence,
                synonyms = JSONArray(synonyms).toString()
            )
        )

        return commands
    }

    /**
     * Generate long click commands
     */
    private fun generateLongClickCommands(
        element: ScrapedElementEntity,
        text: String
    ): List<GeneratedCommandEntity> {
        val commands = mutableListOf<GeneratedCommandEntity>()
        val normalizedText = text.lowercase().trim()
        val confidence = calculateConfidence(text, element) * 0.9f // Slightly lower confidence

        val primaryCommand = "long press $normalizedText"
        val synonyms = LONG_CLICK_VERBS.map { "$it $normalizedText" }

        commands.add(
            GeneratedCommandEntity(
                elementHash = element.elementHash,
                commandText = primaryCommand,
                actionType = "long_click",
                confidence = confidence,
                synonyms = JSONArray(synonyms).toString()
            )
        )

        return commands
    }

    /**
     * Generate input commands for editable fields
     */
    private fun generateInputCommands(
        element: ScrapedElementEntity,
        text: String
    ): List<GeneratedCommandEntity> {
        val commands = mutableListOf<GeneratedCommandEntity>()
        val normalizedText = text.lowercase().trim()
        val confidence = calculateConfidence(text, element)

        val primaryCommand = "type in $normalizedText"
        val synonyms = INPUT_VERBS.map { "$it $normalizedText" }

        commands.add(
            GeneratedCommandEntity(
                elementHash = element.elementHash,
                commandText = primaryCommand,
                actionType = "type",
                confidence = confidence,
                synonyms = JSONArray(synonyms).toString()
            )
        )

        return commands
    }

    /**
     * Generate scroll commands
     */
    private fun generateScrollCommands(
        element: ScrapedElementEntity,
        text: String
    ): List<GeneratedCommandEntity> {
        val commands = mutableListOf<GeneratedCommandEntity>()
        val normalizedText = text.lowercase().trim()
        val confidence = calculateConfidence(text, element) * 0.8f

        // Generate "scroll [text]" command
        val primaryCommand = "scroll $normalizedText"
        val synonyms = SCROLL_VERBS.map { "$it $normalizedText" }

        commands.add(
            GeneratedCommandEntity(
                elementHash = element.elementHash,
                commandText = primaryCommand,
                actionType = "scroll",
                confidence = confidence,
                synonyms = JSONArray(synonyms).toString()
            )
        )

        return commands
    }

    /**
     * Generate focus commands
     */
    private fun generateFocusCommands(
        element: ScrapedElementEntity,
        text: String
    ): List<GeneratedCommandEntity> {
        val commands = mutableListOf<GeneratedCommandEntity>()
        val normalizedText = text.lowercase().trim()
        val confidence = calculateConfidence(text, element) * 0.7f

        val primaryCommand = "focus $normalizedText"
        val synonyms = FOCUS_VERBS.map { "$it $normalizedText" }

        commands.add(
            GeneratedCommandEntity(
                elementHash = element.elementHash,
                commandText = primaryCommand,
                actionType = "focus",
                confidence = confidence,
                synonyms = JSONArray(synonyms).toString()
            )
        )

        return commands
    }

    /**
     * Generate synonyms for click commands using NLP rules
     */
    private fun generateClickSynonyms(text: String): List<String> {
        val synonyms = mutableListOf<String>()

        // Add verb variations
        CLICK_VERBS.forEach { verb ->
            synonyms.add("$verb $text")
        }

        // Add semantic synonyms for common button text
        BUTTON_SYNONYMS.entries.forEach { (key, values) ->
            if (text.contains(key, ignoreCase = true)) {
                values.forEach { synonym ->
                    synonyms.add(synonym)
                    synonyms.add("click $synonym")
                }
            }
        }

        // Add simplified version (just the text without verb)
        synonyms.add(text)

        // Remove duplicates and return
        return synonyms.distinct()
    }

    /**
     * Calculate confidence score for command quality
     *
     * Factors:
     * - Text length (longer text = more specific = higher confidence)
     * - Text source (text > contentDescription > viewId)
     * - Element type (buttons = high confidence)
     * - Special characters (fewer = better)
     *
     * @return Confidence score between 0.0 and 1.0
     */
    private fun calculateConfidence(text: String, element: ScrapedElementEntity): Float {
        var confidence = 0.5f // Base confidence

        // Text source bonus
        when {
            !element.text.isNullOrBlank() -> confidence += 0.3f // Direct text label
            !element.contentDescription.isNullOrBlank() -> confidence += 0.2f // Content description
            !element.viewIdResourceName.isNullOrBlank() -> confidence += 0.1f // View ID fallback
        }

        // Text length bonus (normalized)
        val textLength = text.length
        val lengthBonus = when {
            textLength in 5..20 -> 0.2f // Ideal length
            textLength in 3..4 -> 0.1f // Short but acceptable
            textLength > 20 -> -0.1f // Too long
            else -> -0.2f // Too short
        }
        confidence += lengthBonus

        // Element type bonus
        val className = element.className.lowercase()
        val typeBonus = when {
            className.contains("button") -> 0.2f
            className.contains("imagebutton") -> 0.15f
            className.contains("edittext") -> 0.15f
            className.contains("textview") && element.isClickable -> 0.1f
            else -> 0.0f
        }
        confidence += typeBonus

        // Penalize special characters and numbers
        val specialCharCount = text.count { !it.isLetterOrDigit() && !it.isWhitespace() }
        val numberCount = text.count { it.isDigit() }
        confidence -= (specialCharCount * 0.05f)
        confidence -= (numberCount * 0.02f)

        // Clamp to [0.0, 1.0]
        return confidence.coerceIn(0.0f, 1.0f)
    }

    /**
     * Batch generate commands for multiple elements
     */
    suspend fun generateCommandsForElements(elements: List<ScrapedElementEntity>): List<GeneratedCommandEntity> {
        return elements.flatMap { element ->
            generateCommands(element)
        }
    }

    // ========== Phase 3: State-Aware Command Generation ==========

    /**
     * Generate state-aware commands for an element
     *
     * Checks the current state of the element (checked, selected, enabled, etc.)
     * and generates appropriate commands based on that state.
     *
     * Examples:
     * - Checkbox currently checked → generates "uncheck [text]"
     * - Checkbox currently unchecked → generates "check [text]"
     * - Expandable item collapsed → generates "expand [text]"
     * - Expandable item expanded → generates "collapse [text]"
     *
     * @param element ScrapedElementEntity to generate commands for
     * @return List of state-aware generated commands
     */
    suspend fun generateStateAwareCommands(element: ScrapedElementEntity): List<GeneratedCommandEntity> {
        val commands = mutableListOf<GeneratedCommandEntity>()

        // Extract meaningful text from element
        val elementText = extractElementText(element) ?: return emptyList()
        val normalizedText = elementText.lowercase().trim()

        // Check for checkable elements (checkboxes, radio buttons, toggle switches)
        if (element.isCheckable) {
            val currentState = database.databaseManager.elementStateHistory
                .getCurrentState(element.elementHash, StateType.CHECKED)
            val isChecked = currentState?.newValue?.toBoolean() ?: false

            commands.addAll(
                generateCheckableCommands(element, normalizedText, isChecked)
            )
        }

        // Check for expandable elements (expandable list items, accordions)
        if (element.className.contains("ExpandableListView") ||
            element.className.contains("Expandable")) {

            val currentState = database.databaseManager.elementStateHistory
                .getCurrentState(element.elementHash, StateType.EXPANDED)
            val isExpanded = currentState?.newValue?.toBoolean() ?: false

            commands.addAll(
                generateExpandableCommands(element, normalizedText, isExpanded)
            )
        }

        // Check for selectable elements
        val selectableState = database.databaseManager.elementStateHistory
            .getCurrentState(element.elementHash, StateType.SELECTED)
        if (selectableState != null) {
            val isSelected = selectableState.newValue?.toBoolean() ?: false
            commands.addAll(
                generateSelectableCommands(element, normalizedText, isSelected)
            )
        }

        // Filter out low-confidence commands
        return commands.filter { it.confidence >= MIN_CONFIDENCE }
    }

    /**
     * Generate commands for checkable elements based on current state
     *
     * @param element The checkable element
     * @param text Normalized element text
     * @param isChecked Current checked state
     * @return List of generated commands
     */
    private fun generateCheckableCommands(
        element: ScrapedElementEntity,
        text: String,
        isChecked: Boolean
    ): List<GeneratedCommandEntity> {
        val commands = mutableListOf<GeneratedCommandEntity>()
        val confidence = calculateConfidence(text, element)

        if (isChecked) {
            // Element is checked → generate "uncheck" commands
            commands.add(
                GeneratedCommandEntity(
                    elementHash = element.elementHash,
                    commandText = "uncheck $text",
                    actionType = "click",
                    confidence = confidence,
                    synonyms = JSONArray(listOf(
                        "untick $text",
                        "deselect $text",
                        "turn off $text",
                        "disable $text"
                    )).toString()
                )
            )
        } else {
            // Element is unchecked → generate "check" commands
            commands.add(
                GeneratedCommandEntity(
                    elementHash = element.elementHash,
                    commandText = "check $text",
                    actionType = "click",
                    confidence = confidence,
                    synonyms = JSONArray(listOf(
                        "tick $text",
                        "select $text",
                        "turn on $text",
                        "enable $text"
                    )).toString()
                )
            )
        }

        return commands
    }

    /**
     * Generate commands for expandable elements based on current state
     *
     * @param element The expandable element
     * @param text Normalized element text
     * @param isExpanded Current expanded state
     * @return List of generated commands
     */
    private fun generateExpandableCommands(
        element: ScrapedElementEntity,
        text: String,
        isExpanded: Boolean
    ): List<GeneratedCommandEntity> {
        val commands = mutableListOf<GeneratedCommandEntity>()
        val confidence = calculateConfidence(text, element)

        if (isExpanded) {
            // Element is expanded → generate "collapse" commands
            commands.add(
                GeneratedCommandEntity(
                    elementHash = element.elementHash,
                    commandText = "collapse $text",
                    actionType = "click",
                    confidence = confidence,
                    synonyms = JSONArray(listOf(
                        "close $text",
                        "fold $text",
                        "minimize $text",
                        "hide $text"
                    )).toString()
                )
            )
        } else {
            // Element is collapsed → generate "expand" commands
            commands.add(
                GeneratedCommandEntity(
                    elementHash = element.elementHash,
                    commandText = "expand $text",
                    actionType = "click",
                    confidence = confidence,
                    synonyms = JSONArray(listOf(
                        "open $text",
                        "unfold $text",
                        "show $text",
                        "reveal $text"
                    )).toString()
                )
            )
        }

        return commands
    }

    /**
     * Generate commands for selectable elements based on current state
     *
     * @param element The selectable element
     * @param text Normalized element text
     * @param isSelected Current selected state
     * @return List of generated commands
     */
    private fun generateSelectableCommands(
        element: ScrapedElementEntity,
        text: String,
        isSelected: Boolean
    ): List<GeneratedCommandEntity> {
        val commands = mutableListOf<GeneratedCommandEntity>()
        val confidence = calculateConfidence(text, element)

        if (isSelected) {
            // Element is selected → generate "deselect" commands
            commands.add(
                GeneratedCommandEntity(
                    elementHash = element.elementHash,
                    commandText = "deselect $text",
                    actionType = "click",
                    confidence = confidence,
                    synonyms = JSONArray(listOf(
                        "unselect $text",
                        "clear selection $text"
                    )).toString()
                )
            )
        } else {
            // Element is not selected → generate "select" commands
            commands.add(
                GeneratedCommandEntity(
                    elementHash = element.elementHash,
                    commandText = "select $text",
                    actionType = "click",
                    confidence = confidence,
                    synonyms = JSONArray(listOf(
                        "choose $text",
                        "pick $text"
                    )).toString()
                )
            )
        }

        return commands
    }

    /**
     * Generate interaction-weighted commands
     *
     * Boosts confidence for frequently interacted elements and adjusts based on
     * success/failure ratio from interaction history.
     *
     * @param element ScrapedElementEntity to generate commands for
     * @return List of generated commands with adjusted confidence scores
     */
    suspend fun generateInteractionWeightedCommands(element: ScrapedElementEntity): List<GeneratedCommandEntity> {
        // Generate base commands
        val baseCommands = generateCommands(element)

        // Get interaction count for this element
        val interactionCount = database.databaseManager.userInteractions
            .getInteractionCount(element.elementHash)

        // Get success/failure ratio
        val ratio = database.databaseManager.userInteractions
            .getSuccessFailureRatio(element.elementHash)

        // Calculate confidence boost based on interaction frequency
        val frequencyBoost = when {
            interactionCount > 100 -> 0.15f  // Very frequently used
            interactionCount > 50 -> 0.10f   // Frequently used
            interactionCount > 20 -> 0.05f   // Moderately used
            interactionCount > 5 -> 0.02f    // Occasionally used
            else -> 0.0f                     // Rarely/never used
        }

        // Calculate success rate penalty/boost
        val successRate = if (ratio != null && (ratio.successful + ratio.failed) > 0) {
            ratio.successful.toFloat() / (ratio.successful + ratio.failed)
        } else {
            1.0f  // No data = assume 100% success
        }

        val successBoost = when {
            successRate >= 0.9f -> 0.05f     // Very reliable
            successRate >= 0.7f -> 0.0f      // Normally reliable
            successRate >= 0.5f -> -0.05f    // Somewhat unreliable
            else -> -0.10f                   // Very unreliable
        }

        val totalBoost = frequencyBoost + successBoost

        // Apply confidence boost to all commands
        return baseCommands.map { command ->
            command.copy(
                confidence = (command.confidence + totalBoost).coerceIn(0.0f, 1.0f)
            )
        }
    }
}
