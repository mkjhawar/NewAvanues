/**
 * CommandGenerator.kt - Generates voice commands from captured elements
 *
 * Creates voice command triggers from UI elements discovered during exploration.
 * Commands are validated before saving to ensure they work correctly.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md Section 7
 *
 * ## Command Generation Rules:
 * 1. Use element label as base trigger
 * 2. Generate synonyms for common actions
 * 3. Prefix with action verb (click, open, tap)
 * 4. Confidence based on element stability
 *
 * @since 2.0.0 (LearnApp Dual-Edition)
 */

package com.augmentalis.learnappcore.export

import com.augmentalis.learnappcore.models.ElementCategory
import com.augmentalis.learnappcore.models.ElementInfo
import java.util.UUID

/**
 * Command Generator - Creates voice commands from UI elements
 */
object CommandGenerator {

    // Action verb prefixes for different element types
    private val ACTION_VERBS = mapOf(
        ElementCategory.NAVIGATION to listOf("go to", "open", "show", "navigate to"),
        ElementCategory.ACTION to listOf("click", "tap", "press", "select"),
        ElementCategory.INPUT to listOf("type in", "enter", "fill", "search"),
        ElementCategory.MENU to listOf("open", "show", "expand"),
        ElementCategory.CONTACT to listOf("call", "message", "contact")
    )

    // Common synonyms for UI labels
    private val LABEL_SYNONYMS = mapOf(
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
     * Generate commands for a list of elements.
     *
     * @param elements Elements to generate commands for
     * @param packageName Package name for context
     * @return List of generated commands
     */
    fun generateCommands(
        elements: List<ElementInfo>,
        packageName: String
    ): List<GeneratedCommand> {
        val commands = mutableListOf<GeneratedCommand>()

        for (element in elements) {
            // Skip non-interactive elements
            if (!element.isClickable && !element.isLongClickable && !element.isEditable) {
                continue
            }

            // Skip elements with no meaningful label
            val label = element.getDisplayName()
            if (label == "Unknown" || label.isBlank()) {
                continue
            }

            // Generate command
            val cmd = generateCommand(element, label)
            if (cmd != null) {
                commands.add(cmd)
            }
        }

        return commands
    }

    /**
     * Generate a single command for an element.
     */
    private fun generateCommand(element: ElementInfo, label: String): GeneratedCommand? {
        val category = element.inferCategory()
        val uuid = element.uuid ?: generateUuid(element)

        // Determine action based on category
        val action = when {
            element.isEditable -> "edit"
            element.isLongClickable -> "longClick"
            element.isClickable -> "click"
            else -> "click"
        }

        // Generate trigger phrase
        val trigger = generateTrigger(label, category, action)

        // Calculate confidence based on element properties
        val confidence = calculateConfidence(element)

        return GeneratedCommand(
            uuid = uuid,
            trigger = trigger,
            action = action,
            elementUuid = uuid,
            confidence = confidence
        )
    }

    /**
     * Generate trigger phrase for command.
     */
    private fun generateTrigger(label: String, category: ElementCategory, action: String): String {
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
            ElementCategory.INPUT -> if (action == "edit") "type in $cleanLabel" else "$primaryVerb $cleanLabel"
            ElementCategory.CONTACT -> "contact $cleanLabel"
            else -> "$primaryVerb $cleanLabel"
        }
    }

    /**
     * Calculate command confidence based on element stability.
     */
    private fun calculateConfidence(element: ElementInfo): Float {
        var confidence = 0.5f

        // Boost for resource ID (most stable)
        if (element.resourceId.isNotEmpty()) {
            confidence += 0.2f
        }

        // Boost for text content
        if (element.text.isNotEmpty()) {
            confidence += 0.15f
        }

        // Boost for content description
        if (element.contentDescription.isNotEmpty()) {
            confidence += 0.1f
        }

        // Boost for enabled state
        if (element.isEnabled) {
            confidence += 0.05f
        }

        return confidence.coerceAtMost(1.0f)
    }

    /**
     * Generate synonyms for a command.
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
     * Generate all synonym sets for commands.
     */
    fun generateAllSynonyms(commands: List<GeneratedCommand>): List<SynonymSet> {
        return commands.mapNotNull { cmd ->
            val synSet = generateSynonyms(cmd.trigger)
            if (synSet.synonyms.isNotEmpty()) synSet else null
        }
    }

    /**
     * Generate UUID for element.
     */
    private fun generateUuid(element: ElementInfo): String {
        val stableId = element.stableId()
        return "cmd-${stableId.hashCode().toString(16).takeLast(8)}"
    }

    /**
     * Validate generated commands (basic validation).
     */
    fun validateCommands(commands: List<GeneratedCommand>): List<GeneratedCommand> {
        return commands.filter { cmd ->
            // Trigger must have at least 2 characters
            cmd.trigger.length >= 2 &&
            // Confidence must be reasonable
            cmd.confidence >= 0.3f &&
            // Action must be valid
            cmd.action in listOf("click", "longClick", "edit", "scroll", "focus")
        }
    }

    /**
     * Deduplicate commands by trigger.
     */
    fun deduplicateCommands(commands: List<GeneratedCommand>): List<GeneratedCommand> {
        val seen = mutableSetOf<String>()
        return commands.filter { cmd ->
            val normalizedTrigger = cmd.trigger.lowercase().trim()
            if (normalizedTrigger in seen) {
                false
            } else {
                seen.add(normalizedTrigger)
                true
            }
        }
    }

    /**
     * Convert to database-compatible format.
     *
     * Maps to commands_generated schema:
     * - uuid: STRING
     * - trigger_phrase: STRING
     * - action_type: STRING
     * - target_element_uuid: STRING
     * - confidence: REAL
     * - package_name: STRING
     * - created_at: INTEGER
     */
    fun toDbFormat(cmd: GeneratedCommand, packageName: String): Map<String, Any> {
        return mapOf(
            "uuid" to cmd.uuid,
            "trigger_phrase" to cmd.trigger,
            "action_type" to cmd.action,
            "target_element_uuid" to cmd.elementUuid,
            "confidence" to cmd.confidence,
            "package_name" to packageName,
            "created_at" to System.currentTimeMillis()
        )
    }
}
