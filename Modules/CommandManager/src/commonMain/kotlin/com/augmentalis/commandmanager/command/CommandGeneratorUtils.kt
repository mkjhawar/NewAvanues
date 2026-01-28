/*
 * Copyright (c) 2026 Augmentalis. All rights reserved.
 *
 * CommandGeneratorUtils.kt - Utility functions for command generation
 *
 * Provides synonym generation, label synonyms, and action verb mappings
 * for voice command generation and AVU export.
 *
 * Author: Manoj Jhawar
 * Created: 2026-01-16
 * Migrated from: LearnAppCore/export/CommandGenerator.kt
 *
 * ## Command Generation Rules:
 * 1. Use element label as base trigger
 * 2. Generate synonyms for common actions
 * 3. Prefix with action verb (click, open, tap)
 * 4. Confidence based on element stability
 *
 * @since 2.0.0 (VoiceOSCoreNG)
 */

package com.augmentalis.commandmanager

import com.augmentalis.commandmanager.CommandActionType
import com.augmentalis.commandmanager.ElementType
import com.augmentalis.commandmanager.QuantizedCommand
import com.augmentalis.commandmanager.QuantizedElement
import com.augmentalis.commandmanager.currentTimeMillis

/**
 * Element category for command generation.
 */
enum class ElementCategory {
    NAVIGATION,
    ACTION,
    INPUT,
    MENU,
    CONTACT,
    DISPLAY,
    OTHER
}

/**
 * Command Generator Utilities - Helper functions for command generation.
 *
 * Provides:
 * - Action verb prefixes for different element types
 * - Common label synonyms for UI elements
 * - Synonym generation for commands
 * - Trigger phrase generation
 */
object CommandGeneratorUtils {

    /**
     * Action verb prefixes for different element categories.
     */
    val ACTION_VERBS: Map<ElementCategory, List<String>> = mapOf(
        ElementCategory.NAVIGATION to listOf("go to", "open", "show", "navigate to"),
        ElementCategory.ACTION to listOf("click", "tap", "press", "select"),
        ElementCategory.INPUT to listOf("type in", "enter", "fill", "search"),
        ElementCategory.MENU to listOf("open", "show", "expand"),
        ElementCategory.CONTACT to listOf("call", "message", "contact")
    )

    /**
     * Common synonyms for UI labels.
     */
    val LABEL_SYNONYMS: Map<String, List<String>> = mapOf(
        "home" to listOf("main", "start", "beginning"),
        "back" to listOf("previous", "return", "go back"),
        "settings" to listOf("preferences", "options", "config"),
        "search" to listOf("find", "look for", "search for"),
        "profile" to listOf("account", "my profile", "user"),
        "menu" to listOf("options", "more"),
        "share" to listOf("send to"),
        "save" to listOf("keep", "store"),
        "delete" to listOf("remove", "trash"),
        "edit" to listOf("modify", "change"),
        "add" to listOf("create", "new"),
        "close" to listOf("dismiss", "exit"),
        "next" to listOf("continue", "forward"),
        "submit" to listOf("send", "confirm", "done"),
        "cancel" to listOf("abort", "stop"),
        "refresh" to listOf("reload", "update")
    )

    /**
     * Infer element category from ElementType.
     */
    fun inferCategory(type: ElementType): ElementCategory {
        return when (type) {
            ElementType.BUTTON -> ElementCategory.ACTION
            ElementType.TEXT_FIELD -> ElementCategory.INPUT
            ElementType.IMAGE_BUTTON -> ElementCategory.ACTION
            ElementType.CHECKBOX -> ElementCategory.ACTION
            ElementType.SWITCH -> ElementCategory.ACTION
            ElementType.TAB -> ElementCategory.NAVIGATION
            ElementType.LIST_ITEM -> ElementCategory.ACTION
            ElementType.MENU -> ElementCategory.MENU
            ElementType.TEXT -> ElementCategory.DISPLAY
            ElementType.IMAGE -> ElementCategory.DISPLAY
            ElementType.CONTAINER -> ElementCategory.OTHER
            else -> ElementCategory.OTHER
        }
    }

    /**
     * Generate trigger phrase for a label and category.
     *
     * @param label Element label
     * @param category Element category
     * @param action Action type
     * @return Generated trigger phrase
     */
    fun generateTrigger(label: String, category: ElementCategory, action: String = "click"): String {
        // Clean label
        val cleanLabel = label.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .trim()

        // Get action verbs for category
        val verbs = ACTION_VERBS[category] ?: listOf("click")
        val primaryVerb = verbs.firstOrNull() ?: "click"

        // Simple trigger: verb + label
        return when (category) {
            ElementCategory.NAVIGATION -> "$primaryVerb $cleanLabel"
            ElementCategory.INPUT -> if (action == "edit" || action == "type") "type in $cleanLabel" else "$primaryVerb $cleanLabel"
            ElementCategory.CONTACT -> "contact $cleanLabel"
            else -> "$primaryVerb $cleanLabel"
        }
    }

    /**
     * Generate synonyms for a command trigger phrase.
     *
     * @param trigger Original trigger phrase
     * @return SynonymSet with alternative trigger phrases
     */
    fun generateSynonyms(trigger: String): SynonymSet {
        val words = trigger.lowercase().split(" ")
        val allSynonyms = mutableSetOf<String>()

        for (word in words) {
            val synonyms = LABEL_SYNONYMS[word]
            if (synonyms != null) {
                for (syn in synonyms) {
                    // Replace word with synonym in trigger
                    val altTrigger = trigger.lowercase().replace(word, syn)
                    allSynonyms.add(altTrigger)
                }
            }
        }

        return SynonymSet(trigger.lowercase(), allSynonyms.toList())
    }

    /**
     * Generate all synonym sets for a list of commands.
     *
     * @param commands List of GeneratedCommand
     * @return List of SynonymSet (only non-empty ones)
     */
    fun generateAllSynonyms(commands: List<GeneratedCommand>): List<SynonymSet> {
        return commands.mapNotNull { cmd ->
            val synSet = generateSynonyms(cmd.commandText)
            if (synSet.synonyms.isNotEmpty()) synSet else null
        }
    }

    /**
     * Generate all synonym sets for QuantizedCommand list.
     *
     * @param commands List of QuantizedCommand
     * @return List of SynonymSet (only non-empty ones)
     */
    fun generateAllSynonymsFromQuantized(commands: List<QuantizedCommand>): List<SynonymSet> {
        return commands.mapNotNull { cmd ->
            val synSet = generateSynonyms(cmd.phrase)
            if (synSet.synonyms.isNotEmpty()) synSet else null
        }
    }

    /**
     * Generate a GeneratedCommand from a QuantizedElement.
     *
     * @param element Source element
     * @param packageName Host application package
     * @return GeneratedCommand with trigger phrase including action verb
     */
    fun fromElement(element: QuantizedElement, packageName: String): GeneratedCommand? {
        // Skip elements without meaningful label
        if (element.label.isBlank() || element.label == "unlabeled") {
            return null
        }

        // Skip non-actionable elements
        if (element.actions.isBlank()) {
            return null
        }

        val category = inferCategory(element.type)
        val action = deriveAction(element)
        val commandText = generateTrigger(element.label, category, action)
        val confidence = calculateConfidence(element)
        val synonymSet = generateSynonyms(commandText)
        val synonymsJson = if (synonymSet.synonyms.isNotEmpty()) {
            "[${synonymSet.synonyms.joinToString(",") { "\"$it\"" }}]"
        } else {
            "[]"
        }

        return GeneratedCommand(
            elementHash = element.avid,
            commandText = commandText,
            actionType = action,
            confidence = confidence.toDouble(),
            synonyms = synonymsJson,
            appId = packageName,
            createdAt = currentTimeMillis()
        )
    }

    /**
     * Generate commands for a list of elements.
     *
     * @param elements List of QuantizedElement
     * @param packageName Host application package
     * @return List of GeneratedCommand
     */
    fun generateCommands(elements: List<QuantizedElement>, packageName: String): List<GeneratedCommand> {
        return elements.mapNotNull { fromElement(it, packageName) }
    }

    /**
     * Validate generated commands.
     *
     * @param commands List of GeneratedCommand
     * @return Filtered list with only valid commands
     */
    fun validateCommands(commands: List<GeneratedCommand>): List<GeneratedCommand> {
        return commands.filter { cmd ->
            // Command text must have at least 2 characters
            cmd.commandText.length >= 2 &&
            // Confidence must be reasonable
            cmd.confidence >= 0.3 &&
            // Action must be valid
            cmd.actionType.lowercase() in listOf("click", "longclick", "long_click", "edit", "type", "scroll", "focus")
        }
    }

    /**
     * Deduplicate commands by command text.
     *
     * @param commands List of GeneratedCommand
     * @return Deduplicated list
     */
    fun deduplicateCommands(commands: List<GeneratedCommand>): List<GeneratedCommand> {
        val seen = mutableSetOf<String>()
        return commands.filter { cmd ->
            val normalizedCommand = cmd.commandText.lowercase().trim()
            if (normalizedCommand in seen) {
                false
            } else {
                seen.add(normalizedCommand)
                true
            }
        }
    }

    /**
     * Convert GeneratedCommand to database-compatible format.
     *
     * @param cmd Command to convert
     * @param packageName Package name
     * @return Map suitable for database insertion
     */
    fun toDbFormat(cmd: GeneratedCommand, packageName: String): Map<String, Any> {
        return mapOf(
            "element_hash" to cmd.elementHash,
            "command_text" to cmd.commandText,
            "action_type" to cmd.actionType,
            "confidence" to cmd.confidence,
            "synonyms" to cmd.synonyms,
            "package_name" to packageName,
            "created_at" to currentTimeMillis()
        )
    }

    // ==================== Private Helpers ====================

    private fun deriveAction(element: QuantizedElement): String {
        return when {
            element.type == ElementType.TEXT_FIELD -> "type"
            element.actions.contains("scroll") -> "scroll"
            element.actions.contains("click") -> "click"
            else -> "click"
        }
    }

    private fun calculateConfidence(element: QuantizedElement): Float {
        var confidence = 0.5f

        // Boost for having AVID (unique identifier)
        if (element.avid.isNotBlank()) {
            confidence += 0.2f
        }

        // Boost for reasonable label length
        if (element.label.length in 2..20) {
            confidence += 0.15f
        }

        // Boost for having aliases
        if (element.aliases.isNotEmpty()) {
            confidence += 0.1f
        }

        // Boost for being actionable
        if (element.actions.isNotBlank()) {
            confidence += 0.05f
        }

        return confidence.coerceIn(0f, 1f)
    }
}
