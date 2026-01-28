/**
 * ICommandGenerator.kt - Command generation interface for JIT processing
 *
 * Copyright (C) 2026 Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-16
 *
 * Defines the contract for generating voice commands from UI elements.
 * Implementations can use different strategies (rule-based, AI-powered, etc.)
 */
package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.FrameworkType

/**
 * Generated voice command data.
 *
 * Contains all information needed to register and execute a voice command
 * for a UI element.
 *
 * @property id Database ID (0 for new commands, auto-generated on insert)
 * @property elementHash Deterministic hash of element properties for deduplication
 * @property commandText Primary voice command (e.g., "click submit")
 * @property actionType The action to perform (click, type, scroll, etc.)
 * @property confidence Confidence score (0.0-1.0) for the generated command
 * @property synonyms JSON array of alternative phrasings
 * @property isUserApproved Whether user has verified this command (0 = no, 1 = yes)
 * @property usageCount Number of times this command has been used
 * @property lastUsed Timestamp of last usage (null if never used)
 * @property createdAt Timestamp when command was created
 * @property appId Package name of the app this command belongs to
 */
data class GeneratedCommand(
    val id: Long = 0L,
    val elementHash: String,
    val commandText: String,
    val actionType: String,
    val confidence: Double = 0.85,
    val synonyms: String = "[]",
    val isUserApproved: Long = 0L,
    val usageCount: Long = 0L,
    val lastUsed: Long? = null,
    val createdAt: Long = 0L,
    val appId: String = ""
) {
    /**
     * Check if this command has high confidence.
     */
    fun isHighConfidence(): Boolean = confidence >= 0.8

    /**
     * Check if this is a user-verified command.
     */
    fun isVerified(): Boolean = isUserApproved == 1L

    /**
     * Check if this command has been used.
     */
    fun hasBeenUsed(): Boolean = usageCount > 0

    /**
     * Get synonyms as a list.
     */
    fun getSynonymList(): List<String> {
        return try {
            // Simple JSON array parsing without external dependencies
            if (synonyms.isBlank() || synonyms == "[]") {
                emptyList()
            } else {
                synonyms
                    .removePrefix("[")
                    .removeSuffix("]")
                    .split(",")
                    .map { it.trim().removeSurrounding("\"") }
                    .filter { it.isNotBlank() }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

/**
 * Result of command generation for an element.
 *
 * @property avid The generated AVID (Avanues Voice ID) for the element
 * @property command Generated voice command (null if no command could be generated)
 * @property success Whether command generation was successful
 * @property error Error message if generation failed
 */
data class CommandGenerationResult(
    val avid: String,
    val command: GeneratedCommand?,
    val success: Boolean,
    val error: String? = null
) {
    companion object {
        /**
         * Create a successful result.
         */
        fun success(avid: String, command: GeneratedCommand): CommandGenerationResult {
            return CommandGenerationResult(
                avid = avid,
                command = command,
                success = true
            )
        }

        /**
         * Create a failure result.
         */
        fun failure(avid: String, error: String): CommandGenerationResult {
            return CommandGenerationResult(
                avid = avid,
                command = null,
                success = false,
                error = error
            )
        }

        /**
         * Create a result for elements that don't need commands.
         */
        fun skipped(avid: String, reason: String): CommandGenerationResult {
            return CommandGenerationResult(
                avid = avid,
                command = null,
                success = true, // Not a failure, just skipped
                error = reason
            )
        }
    }
}

/**
 * Interface for generating voice commands from UI elements.
 *
 * Implementations provide different strategies for command generation:
 * - Rule-based: Uses predefined patterns and heuristics
 * - AI-powered: Uses machine learning for semantic understanding
 * - Hybrid: Combines both approaches
 *
 * ## Usage
 * ```kotlin
 * val generator: ICommandGenerator = AndroidCommandGenerator(context)
 *
 * // Generate command for single element
 * val result = generator.generateCommand(element, packageName)
 * if (result.success && result.command != null) {
 *     database.insert(result.command)
 * }
 *
 * // Generate commands in batch
 * val results = generator.generateCommands(elements, packageName)
 * ```
 */
interface ICommandGenerator {

    /**
     * Generate a voice command for a UI element.
     *
     * @param element The UI element to generate a command for
     * @param packageName The package name of the host application
     * @param frameworkType Optional framework type (if already detected)
     * @return CommandGenerationResult with generated command or error
     */
    fun generateCommand(
        element: ElementInfo,
        packageName: String,
        frameworkType: FrameworkType? = null
    ): CommandGenerationResult

    /**
     * Generate voice commands for multiple elements.
     *
     * Default implementation processes sequentially; implementations may
     * override for parallel processing.
     *
     * @param elements List of UI elements to process
     * @param packageName The package name of the host application
     * @param frameworkType Optional framework type (if already detected)
     * @return List of generation results
     */
    fun generateCommands(
        elements: List<ElementInfo>,
        packageName: String,
        frameworkType: FrameworkType? = null
    ): List<CommandGenerationResult> {
        return elements.map { generateCommand(it, packageName, frameworkType) }
    }

    /**
     * Generate synonyms for a command.
     *
     * Creates alternative phrasings that users might say for the same action.
     *
     * @param actionType The action type (click, type, scroll, etc.)
     * @param label The element label
     * @return JSON array string of synonyms
     */
    fun generateSynonyms(actionType: String, label: String): String

    /**
     * Determine the action type for an element.
     *
     * @param element The element to analyze
     * @return Action type string (click, type, scroll, long_click, etc.)
     */
    fun determineActionType(element: ElementInfo): String

    /**
     * Generate a fallback label for unlabeled elements.
     *
     * Called when an element has no text, content description, or resource ID.
     *
     * @param element The element to generate a label for
     * @param frameworkType The detected framework type
     * @return Generated label string
     */
    fun generateFallbackLabel(element: ElementInfo, frameworkType: FrameworkType): String
}
