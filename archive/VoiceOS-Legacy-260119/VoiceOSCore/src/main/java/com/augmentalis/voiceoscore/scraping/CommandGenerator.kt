/**
 * CommandGenerator.kt - NLP-based voice command generator
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 * Modified: 2025-10-18 (Phase 3: State-aware command generation)
 * Modified: 2025-12-18 (Room → SQLDelight migration)
 */
package com.augmentalis.voiceoscore.scraping

import android.content.Context
import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.database.dto.ScrapedElementDTO
import com.augmentalis.database.ElementStateHistoryQueries
import com.augmentalis.database.stats.UserInteractionQueries
import org.json.JSONArray

/**
 * State type constants for element state tracking
 */
object StateType {
    const val ENABLED = "ENABLED"
    const val DISABLED = "DISABLED"
    const val CHECKED = "CHECKED"
    const val UNCHECKED = "UNCHECKED"
    const val SELECTED = "SELECTED"
    const val UNSELECTED = "UNSELECTED"
    const val EXPANDED = "EXPANDED"
    const val COLLAPSED = "COLLAPSED"
}

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
class CommandGenerator(
    private val context: Context,
    private val elementStateHistoryQueries: ElementStateHistoryQueries,
    private val userInteractionQueries: UserInteractionQueries
) {

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
     * @param element ScrapedElementDTO to generate commands for
     * @return List of generated commands (may be empty if element has no actionable text)
     */
    suspend fun generateCommands(element: ScrapedElementDTO): List<GeneratedCommandDTO> {
        val commands = mutableListOf<GeneratedCommandDTO>()

        // Extract meaningful text from element
        val elementText = extractElementText(element) ?: return emptyList()

        // Generate commands based on element capabilities
        if (element.isClickable != 0L) {
            commands.addAll(generateClickCommands(element, elementText))
        }

        if (element.isLongClickable != 0L) {
            commands.addAll(generateLongClickCommands(element, elementText))
        }

        if (element.isEditable != 0L) {
            commands.addAll(generateInputCommands(element, elementText))
        }

        if (element.isScrollable != 0L) {
            commands.addAll(generateScrollCommands(element, elementText))
        }

        if (element.isFocusable != 0L && element.isClickable == 0L) {
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
    private fun extractElementText(element: ScrapedElementDTO): String? {
        val text = element.text
        if (!text.isNullOrBlank()) {
            return text.trim()
        }

        val contentDesc = element.contentDescription
        if (!contentDesc.isNullOrBlank()) {
            return contentDesc.trim()
        }

        val viewId = element.viewIdResourceName
        if (!viewId.isNullOrBlank()) {
            // Extract readable part from view ID (e.g., "submit_button" from "com.example:id/submit_button")
            return viewId
                .substringAfterLast('/')
                .replace('_', ' ')
                .trim()
        }

        return null
    }

    /**
     * Generate click commands
     */
    private fun generateClickCommands(
        element: ScrapedElementDTO,
        text: String
    ): List<GeneratedCommandDTO> {
        val commands = mutableListOf<GeneratedCommandDTO>()
        val normalizedText = text.lowercase().trim()

        // Calculate confidence based on text quality
        val confidence = calculateConfidence(text, element)

        // Generate primary command: "click [text]"
        val primaryCommand = "click $normalizedText"
        val synonyms = generateClickSynonyms(normalizedText)

        commands.add(
            GeneratedCommandDTO(
                id = 0, // Will be assigned by database
                elementHash = element.elementHash,
                commandText = primaryCommand,
                actionType = "click",
                confidence = confidence.toDouble(),
                synonyms = JSONArray(synonyms).toString(),
                isUserApproved = 0,
                usageCount = 0,
                lastUsed = null,
                createdAt = System.currentTimeMillis(),
                appId = element.appId
            )
        )

        return commands
    }

    /**
     * Generate long click commands
     */
    private fun generateLongClickCommands(
        element: ScrapedElementDTO,
        text: String
    ): List<GeneratedCommandDTO> {
        val commands = mutableListOf<GeneratedCommandDTO>()
        val normalizedText = text.lowercase().trim()
        val confidence = calculateConfidence(text, element) * 0.9f // Slightly lower confidence

        val primaryCommand = "long press $normalizedText"
        val synonyms = LONG_CLICK_VERBS.map { "$it $normalizedText" }

        commands.add(
            GeneratedCommandDTO(
                id = 0,
                elementHash = element.elementHash,
                commandText = primaryCommand,
                actionType = "long_click",
                confidence = confidence.toDouble(),
                synonyms = JSONArray(synonyms).toString(),
                isUserApproved = 0,
                usageCount = 0,
                lastUsed = null,
                createdAt = System.currentTimeMillis(),
                appId = element.appId
            )
        )

        return commands
    }

    /**
     * Generate input commands for editable fields
     */
    private fun generateInputCommands(
        element: ScrapedElementDTO,
        text: String
    ): List<GeneratedCommandDTO> {
        val commands = mutableListOf<GeneratedCommandDTO>()
        val normalizedText = text.lowercase().trim()
        val confidence = calculateConfidence(text, element)

        val primaryCommand = "type in $normalizedText"
        val synonyms = INPUT_VERBS.map { "$it $normalizedText" }

        commands.add(
            GeneratedCommandDTO(
                id = 0,
                elementHash = element.elementHash,
                commandText = primaryCommand,
                actionType = "type",
                confidence = confidence.toDouble(),
                synonyms = JSONArray(synonyms).toString(),
                isUserApproved = 0,
                usageCount = 0,
                lastUsed = null,
                createdAt = System.currentTimeMillis(),
                appId = element.appId
            )
        )

        return commands
    }

    /**
     * Generate scroll commands
     */
    private fun generateScrollCommands(
        element: ScrapedElementDTO,
        text: String
    ): List<GeneratedCommandDTO> {
        val commands = mutableListOf<GeneratedCommandDTO>()
        val normalizedText = text.lowercase().trim()
        val confidence = calculateConfidence(text, element) * 0.8f

        // Generate "scroll [text]" command
        val primaryCommand = "scroll $normalizedText"
        val synonyms = SCROLL_VERBS.map { "$it $normalizedText" }

        commands.add(
            GeneratedCommandDTO(
                id = 0,
                elementHash = element.elementHash,
                commandText = primaryCommand,
                actionType = "scroll",
                confidence = confidence.toDouble(),
                synonyms = JSONArray(synonyms).toString(),
                isUserApproved = 0,
                usageCount = 0,
                lastUsed = null,
                createdAt = System.currentTimeMillis(),
                appId = element.appId
            )
        )

        return commands
    }

    /**
     * Generate focus commands
     */
    private fun generateFocusCommands(
        element: ScrapedElementDTO,
        text: String
    ): List<GeneratedCommandDTO> {
        val commands = mutableListOf<GeneratedCommandDTO>()
        val normalizedText = text.lowercase().trim()
        val confidence = calculateConfidence(text, element) * 0.7f

        val primaryCommand = "focus $normalizedText"
        val synonyms = FOCUS_VERBS.map { "$it $normalizedText" }

        commands.add(
            GeneratedCommandDTO(
                id = 0,
                elementHash = element.elementHash,
                commandText = primaryCommand,
                actionType = "focus",
                confidence = confidence.toDouble(),
                synonyms = JSONArray(synonyms).toString(),
                isUserApproved = 0,
                usageCount = 0,
                lastUsed = null,
                createdAt = System.currentTimeMillis(),
                appId = element.appId
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
    private fun calculateConfidence(text: String, element: ScrapedElementDTO): Float {
        var confidence = 0.5f // Base confidence

        // Text source bonus
        val elementText = element.text
        val contentDesc = element.contentDescription
        val viewId = element.viewIdResourceName
        when {
            !elementText.isNullOrBlank() -> confidence += 0.3f // Direct text label
            !contentDesc.isNullOrBlank() -> confidence += 0.2f // Content description
            !viewId.isNullOrBlank() -> confidence += 0.1f // View ID fallback
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
            className.contains("textview") && element.isClickable != 0L -> 0.1f
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
    suspend fun generateCommandsForElements(elements: List<ScrapedElementDTO>): List<GeneratedCommandDTO> {
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
     * @param element ScrapedElementDTO to generate commands for
     * @return List of state-aware generated commands
     */
    suspend fun generateStateAwareCommands(element: ScrapedElementDTO): List<GeneratedCommandDTO> {
        val commands = mutableListOf<GeneratedCommandDTO>()

        // Extract meaningful text from element
        val elementText = extractElementText(element) ?: return emptyList()
        val normalizedText = elementText.lowercase().trim()

        // Check for checkable elements (checkboxes, radio buttons, toggle switches)
        if (element.isCheckable != 0L) {
            val currentState = elementStateHistoryQueries
                .getCurrentState(element.elementHash, StateType.CHECKED)
                .executeAsOneOrNull()

            val isChecked = currentState?.newValue?.toBoolean() ?: false

            commands.addAll(
                generateCheckableCommands(element, normalizedText, isChecked)
            )
        }

        // Check for expandable elements (expandable list items, accordions)
        if (element.className.contains("ExpandableListView") ||
            element.className.contains("Expandable")) {

            val currentState = elementStateHistoryQueries
                .getCurrentState(element.elementHash, StateType.EXPANDED)
                .executeAsOneOrNull()

            val isExpanded = currentState?.newValue?.toBoolean() ?: false

            commands.addAll(
                generateExpandableCommands(element, normalizedText, isExpanded)
            )
        }

        // Check for selectable elements
        val selectableState = elementStateHistoryQueries
            .getCurrentState(element.elementHash, StateType.SELECTED)
            .executeAsOneOrNull()

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
        element: ScrapedElementDTO,
        text: String,
        isChecked: Boolean
    ): List<GeneratedCommandDTO> {
        val commands = mutableListOf<GeneratedCommandDTO>()
        val confidence = calculateConfidence(text, element)

        if (isChecked) {
            // Element is checked → generate "uncheck" commands
            commands.add(
                GeneratedCommandDTO(
                    id = 0,
                    elementHash = element.elementHash,
                    commandText = "uncheck $text",
                    actionType = "click",
                    confidence = confidence.toDouble(),
                    synonyms = JSONArray(listOf(
                        "untick $text",
                        "deselect $text",
                        "turn off $text",
                        "disable $text"
                    )).toString(),
                    isUserApproved = 0,
                    usageCount = 0,
                    lastUsed = null,
                    createdAt = System.currentTimeMillis(),
                    appId = element.appId
                )
            )
        } else {
            // Element is unchecked → generate "check" commands
            commands.add(
                GeneratedCommandDTO(
                    id = 0,
                    elementHash = element.elementHash,
                    commandText = "check $text",
                    actionType = "click",
                    confidence = confidence.toDouble(),
                    synonyms = JSONArray(listOf(
                        "tick $text",
                        "select $text",
                        "turn on $text",
                        "enable $text"
                    )).toString(),
                    isUserApproved = 0,
                    usageCount = 0,
                    lastUsed = null,
                    createdAt = System.currentTimeMillis(),
                    appId = element.appId
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
        element: ScrapedElementDTO,
        text: String,
        isExpanded: Boolean
    ): List<GeneratedCommandDTO> {
        val commands = mutableListOf<GeneratedCommandDTO>()
        val confidence = calculateConfidence(text, element)

        if (isExpanded) {
            // Element is expanded → generate "collapse" commands
            commands.add(
                GeneratedCommandDTO(
                    id = 0,
                    elementHash = element.elementHash,
                    commandText = "collapse $text",
                    actionType = "click",
                    confidence = confidence.toDouble(),
                    synonyms = JSONArray(listOf(
                        "close $text",
                        "fold $text",
                        "minimize $text",
                        "hide $text"
                    )).toString(),
                    isUserApproved = 0,
                    usageCount = 0,
                    lastUsed = null,
                    createdAt = System.currentTimeMillis(),
                    appId = element.appId
                )
            )
        } else {
            // Element is collapsed → generate "expand" commands
            commands.add(
                GeneratedCommandDTO(
                    id = 0,
                    elementHash = element.elementHash,
                    commandText = "expand $text",
                    actionType = "click",
                    confidence = confidence.toDouble(),
                    synonyms = JSONArray(listOf(
                        "open $text",
                        "unfold $text",
                        "show $text",
                        "reveal $text"
                    )).toString(),
                    isUserApproved = 0,
                    usageCount = 0,
                    lastUsed = null,
                    createdAt = System.currentTimeMillis(),
                    appId = element.appId
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
        element: ScrapedElementDTO,
        text: String,
        isSelected: Boolean
    ): List<GeneratedCommandDTO> {
        val commands = mutableListOf<GeneratedCommandDTO>()
        val confidence = calculateConfidence(text, element)

        if (isSelected) {
            // Element is selected → generate "deselect" commands
            commands.add(
                GeneratedCommandDTO(
                    id = 0,
                    elementHash = element.elementHash,
                    commandText = "deselect $text",
                    actionType = "click",
                    confidence = confidence.toDouble(),
                    synonyms = JSONArray(listOf(
                        "unselect $text",
                        "clear selection $text"
                    )).toString(),
                    isUserApproved = 0,
                    usageCount = 0,
                    lastUsed = null,
                    createdAt = System.currentTimeMillis(),
                    appId = element.appId
                )
            )
        } else {
            // Element is not selected → generate "select" commands
            commands.add(
                GeneratedCommandDTO(
                    id = 0,
                    elementHash = element.elementHash,
                    commandText = "select $text",
                    actionType = "click",
                    confidence = confidence.toDouble(),
                    synonyms = JSONArray(listOf(
                        "choose $text",
                        "pick $text"
                    )).toString(),
                    isUserApproved = 0,
                    usageCount = 0,
                    lastUsed = null,
                    createdAt = System.currentTimeMillis(),
                    appId = element.appId
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
     * @param element ScrapedElementDTO to generate commands for
     * @return List of generated commands with adjusted confidence scores
     */
    suspend fun generateInteractionWeightedCommands(element: ScrapedElementDTO): List<GeneratedCommandDTO> {
        // Generate base commands
        val baseCommands = generateCommands(element)

        // Get interaction count for this element
        val interactionCount = userInteractionQueries
            .getInteractionCount(element.elementHash)
            .executeAsOne()
            .toInt()

        // Get success/failure ratio (currently returns 1.0 as default since we don't track success/failure)
        val successRatio = userInteractionQueries
            .getSuccessFailureRatio(element.elementHash)
            .executeAsOneOrNull()

        // Calculate confidence boost based on interaction frequency
        val frequencyBoost = when {
            interactionCount > 100 -> 0.15f  // Very frequently used
            interactionCount > 50 -> 0.10f   // Frequently used
            interactionCount > 20 -> 0.05f   // Moderately used
            interactionCount > 5 -> 0.02f    // Occasionally used
            else -> 0.0f                     // Rarely/never used
        }

        // For now, we don't track success/failure, so assume 100% success rate
        // TODO: Add success/failure tracking to UserInteraction table
        val successRate = successRatio?.toFloat() ?: 1.0f

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
                confidence = (command.confidence + totalBoost).coerceIn(0.0, 1.0)
            )
        }
    }
}
