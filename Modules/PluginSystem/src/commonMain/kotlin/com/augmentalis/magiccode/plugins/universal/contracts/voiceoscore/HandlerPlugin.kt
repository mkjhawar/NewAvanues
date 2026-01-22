/**
 * HandlerPlugin.kt - Handler Plugin contract for VoiceOSCore
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Defines the contract for handler plugins that execute voice/gaze commands.
 * Handlers are the workhorses of VoiceOSCore, processing user commands
 * and executing actions on UI elements.
 */
package com.augmentalis.magiccode.plugins.universal.contracts.voiceoscore

import com.augmentalis.magiccode.plugins.universal.UniversalPlugin
import com.augmentalis.voiceoscore.ActionResult
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.QuantizedElement

/**
 * Handler Plugin contract for executing voice/gaze commands.
 *
 * Handler plugins are responsible for processing user commands and executing
 * the appropriate actions on UI elements. Each handler specializes in a
 * particular type of interaction (navigation, text input, system commands, etc.).
 *
 * ## Design Principles
 * - **Single Responsibility**: Each handler focuses on one type of interaction
 * - **Confidence-Based Selection**: Handlers report confidence scores for routing
 * - **Pattern Matching**: Command patterns enable flexible matching
 * - **Context-Aware**: Handlers receive full screen context for decisions
 *
 * ## Implementation Example
 * ```kotlin
 * class NavigationHandlerPlugin : HandlerPlugin {
 *     override val handlerType = HandlerType.NAVIGATION
 *     override val patterns = listOf(
 *         CommandPattern(
 *             regex = Regex("^(go|navigate|open)\\s+(.+)$", RegexOption.IGNORE_CASE),
 *             intent = "NAVIGATE",
 *             requiredEntities = setOf("destination"),
 *             examples = listOf("go home", "navigate to settings", "open menu")
 *         )
 *     )
 *
 *     override fun canHandle(command: QuantizedCommand, context: HandlerContext): Boolean {
 *         return patterns.any { it.regex.matches(command.phrase) }
 *     }
 *
 *     override suspend fun handle(command: QuantizedCommand, context: HandlerContext): ActionResult {
 *         // Execute navigation action
 *         return ActionResult.Success("Navigated to ${command.targetAvid}")
 *     }
 *
 *     override fun getConfidence(command: QuantizedCommand, context: HandlerContext): Float {
 *         // Return confidence score based on pattern match quality
 *         return if (canHandle(command, context)) 0.9f else 0.0f
 *     }
 * }
 * ```
 *
 * @since 1.0.0
 * @see HandlerType
 * @see CommandPattern
 * @see HandlerContext
 */
interface HandlerPlugin : UniversalPlugin {

    /**
     * Type of handler, used for categorization and routing.
     *
     * The handler type helps the command dispatcher route commands
     * to appropriate handlers based on the command's nature.
     */
    val handlerType: HandlerType

    /**
     * List of command patterns this handler can process.
     *
     * Patterns are used for command matching and intent extraction.
     * A handler may support multiple patterns for different variations
     * of related commands.
     */
    val patterns: List<CommandPattern>

    /**
     * Check if this handler can process the given command.
     *
     * This is a fast check to filter out irrelevant commands before
     * the more expensive [handle] operation. Implementations should
     * be lightweight and avoid I/O operations.
     *
     * @param command The command to check
     * @param context Current handler context with screen state
     * @return true if this handler can process the command
     */
    fun canHandle(command: QuantizedCommand, context: HandlerContext): Boolean

    /**
     * Execute the command and return the result.
     *
     * This is the main entry point for command execution. The handler
     * should perform the necessary actions and return an appropriate
     * [ActionResult] indicating success or failure.
     *
     * ## Threading
     * This method is called from a coroutine context. Long-running
     * operations should use appropriate dispatchers.
     *
     * ## Error Handling
     * Handlers should catch exceptions and return appropriate
     * [ActionResult] variants rather than throwing exceptions.
     *
     * @param command The command to execute
     * @param context Current handler context with screen state
     * @return ActionResult indicating the outcome
     */
    suspend fun handle(command: QuantizedCommand, context: HandlerContext): ActionResult

    /**
     * Get the confidence score for handling this command.
     *
     * The confidence score is used when multiple handlers can process
     * a command. The handler with the highest confidence is selected.
     *
     * @param command The command to evaluate
     * @param context Current handler context with screen state
     * @return Confidence score between 0.0 (cannot handle) and 1.0 (perfect match)
     */
    fun getConfidence(command: QuantizedCommand, context: HandlerContext): Float
}

/**
 * Type of handler, categorizing the kind of interactions it handles.
 *
 * Handler types enable efficient routing and help users understand
 * what kind of commands a handler can process.
 */
enum class HandlerType {
    /** Navigation commands (go back, open app, switch screen) */
    NAVIGATION,

    /** UI element interactions (click, tap, select, toggle) */
    UI_INTERACTION,

    /** Text input commands (type, enter, dictate) */
    TEXT_INPUT,

    /** System commands (volume, brightness, settings) */
    SYSTEM,

    /** Accessibility-specific commands (read screen, describe element) */
    ACCESSIBILITY,

    /** Custom/application-specific commands */
    CUSTOM
}

/**
 * Pattern for matching voice commands.
 *
 * Command patterns define how voice input is matched to handler capabilities.
 * They include regular expressions for matching, intent classification,
 * and example phrases for documentation and training.
 *
 * ## Pattern Design
 * - Use named groups in regex for entity extraction
 * - Provide multiple examples covering edge cases
 * - Keep patterns focused on a single intent
 *
 * @property regex Regular expression for matching command phrases
 * @property intent Intent identifier for this pattern (e.g., "CLICK", "NAVIGATE")
 * @property requiredEntities Set of entity names that must be extracted
 * @property examples Example phrases that match this pattern (for documentation/training)
 */
data class CommandPattern(
    val regex: Regex,
    val intent: String,
    val requiredEntities: Set<String> = emptySet(),
    val examples: List<String> = emptyList()
) {
    /**
     * Check if a phrase matches this pattern.
     *
     * @param phrase The phrase to check
     * @return true if the phrase matches the pattern
     */
    fun matches(phrase: String): Boolean = regex.matches(phrase)

    /**
     * Extract match result with groups from a phrase.
     *
     * @param phrase The phrase to match
     * @return MatchResult if the phrase matches, null otherwise
     */
    fun matchResult(phrase: String): MatchResult? = regex.matchEntire(phrase)

    /**
     * Extract named groups from a matching phrase.
     *
     * @param phrase The phrase to extract from
     * @return Map of group names to values, empty if no match
     */
    fun extractEntities(phrase: String): Map<String, String> {
        val result = regex.matchEntire(phrase) ?: return emptyMap()
        return result.groups
            .filterNotNull()
            .drop(1) // Skip the full match
            .mapIndexedNotNull { index, group ->
                // Try to get named group, fall back to index
                group.value.takeIf { it.isNotBlank() }?.let { "entity$index" to it }
            }
            .toMap()
    }
}

/**
 * Context provided to handlers for command processing.
 *
 * Contains all the information a handler needs to make decisions
 * and execute commands, including current screen state, available
 * UI elements, and user preferences.
 *
 * @property currentScreen Current screen context
 * @property elements List of quantized UI elements on screen
 * @property previousCommand Previous command executed (for context chaining)
 * @property userPreferences User preferences map (accessibility settings, etc.)
 */
data class HandlerContext(
    val currentScreen: ScreenContext,
    val elements: List<QuantizedElement>,
    val previousCommand: QuantizedCommand?,
    val userPreferences: Map<String, Any>
) {
    /**
     * Find an element by its AVID.
     *
     * @param avid The AVID to search for
     * @return The matching element or null
     */
    fun findElementByAvid(avid: String): QuantizedElement? {
        return elements.find { it.avid == avid }
    }

    /**
     * Find elements by label (case-insensitive partial match).
     *
     * @param label The label to search for
     * @return List of matching elements
     */
    fun findElementsByLabel(label: String): List<QuantizedElement> {
        val lowerLabel = label.lowercase()
        return elements.filter {
            it.label.lowercase().contains(lowerLabel) ||
                    it.aliases.any { alias -> alias.lowercase().contains(lowerLabel) }
        }
    }

    /**
     * Get a user preference value.
     *
     * @param key Preference key
     * @param default Default value if not found
     * @return Preference value or default
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getPreference(key: String, default: T): T {
        return (userPreferences[key] as? T) ?: default
    }

    /**
     * Check if screen has actionable elements.
     *
     * @return true if there are clickable/actionable elements
     */
    fun hasActionableElements(): Boolean {
        return elements.any { it.actions.contains("click") }
    }
}

/**
 * Context describing the current screen state.
 *
 * Provides metadata about the currently visible screen to help
 * handlers make context-aware decisions.
 *
 * @property packageName Application package name
 * @property activityName Current activity/view controller name
 * @property screenTitle Screen title if available
 * @property elementCount Total number of UI elements on screen
 * @property primaryAction Primary action available on this screen (if any)
 */
data class ScreenContext(
    val packageName: String,
    val activityName: String,
    val screenTitle: String?,
    val elementCount: Int,
    val primaryAction: String?
) {
    /**
     * Check if this is the home/launcher screen.
     *
     * @return true if on launcher/home screen
     */
    fun isHomeScreen(): Boolean {
        return activityName.contains("Launcher", ignoreCase = true) ||
                activityName.contains("Home", ignoreCase = true)
    }

    /**
     * Get a simple screen identifier.
     *
     * @return Screen ID in format "packageName/activityName"
     */
    fun screenId(): String = "$packageName/$activityName"

    companion object {
        /**
         * Create an empty/unknown screen context.
         */
        val UNKNOWN = ScreenContext(
            packageName = "unknown",
            activityName = "unknown",
            screenTitle = null,
            elementCount = 0,
            primaryAction = null
        )

        /**
         * Alias for UNKNOWN for code compatibility.
         */
        val EMPTY = UNKNOWN
    }
}
