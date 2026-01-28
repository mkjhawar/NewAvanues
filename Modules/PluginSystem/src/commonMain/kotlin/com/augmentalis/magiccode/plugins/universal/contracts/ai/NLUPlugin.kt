/**
 * NLUPlugin.kt - Natural Language Understanding plugin interface
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Defines the contract for NLU plugins that provide intent classification,
 * entity extraction, and command suggestion capabilities. Essential for
 * voice-controlled accessibility applications.
 */
package com.augmentalis.magiccode.plugins.universal.contracts.ai

import com.augmentalis.magiccode.plugins.universal.UniversalPlugin
import com.augmentalis.voiceoscore.QuantizedElement
import kotlinx.serialization.Serializable

/**
 * Plugin interface for Natural Language Understanding.
 *
 * NLUPlugin extends [UniversalPlugin] to provide natural language understanding
 * capabilities including intent classification, entity extraction, and
 * command suggestion. This is essential for voice-controlled accessibility
 * applications that need to interpret user speech.
 *
 * ## Capabilities
 * Implementations should advertise one or more of:
 * - [PluginCapability.NLU_INTENT] - Intent classification
 * - [PluginCapability.NLU_ENTITY] - Entity extraction
 * - [PluginCapability.NLU_SLOT_FILLING] - Slot filling for commands
 *
 * ## Use Cases
 * - **Voice Commands**: "Click the submit button" -> CLICK intent, entity: submit button
 * - **Navigation**: "Go back" -> NAVIGATE intent, entity: back
 * - **Dictation**: "Type hello world" -> DICTATE intent, entity: "hello world"
 * - **Query**: "What apps are open?" -> QUERY intent
 *
 * ## Implementation Example
 * ```kotlin
 * class SnipsNLUPlugin : NLUPlugin {
 *     override val pluginId = "com.augmentalis.nlu.snips"
 *     override val supportedIntents = setOf("click", "scroll", "type", "navigate")
 *     override val supportedEntities = setOf("element", "direction", "text")
 *
 *     override suspend fun classifyIntent(utterance: String, context: NLUContext): IntentResult {
 *         // Classify using Snips NLU engine
 *     }
 * }
 * ```
 *
 * @since 1.0.0
 * @see UniversalPlugin
 * @see IntentResult
 * @see Entity
 */
interface NLUPlugin : UniversalPlugin {

    // =========================================================================
    // Capability Properties
    // =========================================================================

    /**
     * Set of intents this plugin can classify.
     *
     * Common accessibility intents:
     * - "click", "tap" - Activate an element
     * - "scroll" - Scroll in a direction
     * - "type", "dictate" - Input text
     * - "navigate", "go" - Navigate to screen/location
     * - "back", "forward" - Browser/navigation history
     * - "select", "choose" - Selection actions
     * - "open", "close" - App/window management
     * - "help", "describe" - Accessibility assistance
     */
    val supportedIntents: Set<String>

    /**
     * Set of entity types this plugin can extract.
     *
     * Common accessibility entities:
     * - "element" - UI element reference (button, field, link)
     * - "direction" - Scroll/swipe direction (up, down, left, right)
     * - "text" - Free-form text for dictation
     * - "number" - Numeric values
     * - "app" - Application name
     * - "action" - Action type (double-tap, long-press)
     */
    val supportedEntities: Set<String>

    // =========================================================================
    // NLU Methods
    // =========================================================================

    /**
     * Classify the intent of an utterance.
     *
     * Analyzes the user's spoken utterance to determine their intent and
     * extract relevant entities. Uses context to improve accuracy.
     *
     * ## Context Usage
     * The [NLUContext] provides:
     * - Current screen elements for disambiguation
     * - Previous utterances for multi-turn understanding
     * - User preferences for personalization
     * - Available commands for constraint-based parsing
     *
     * @param utterance The spoken/typed text to classify
     * @param context Current application context
     * @return IntentResult with classification and entities
     * @see IntentResult
     * @see NLUContext
     */
    suspend fun classifyIntent(utterance: String, context: NLUContext): IntentResult

    /**
     * Extract entities from an utterance.
     *
     * Identifies and extracts named entities from text without full intent
     * classification. Useful for:
     * - Pre-processing before intent classification
     * - Entity-only use cases (search, filtering)
     * - Multi-entity extraction
     *
     * @param utterance The text to extract entities from
     * @return List of extracted entities with positions and confidence
     * @see Entity
     */
    suspend fun extractEntities(utterance: String): List<Entity>

    /**
     * Suggest voice commands for UI elements.
     *
     * Analyzes the current screen elements and generates natural voice
     * command suggestions. This is core to the learning/exploration flow.
     *
     * ## Use Cases
     * - Initial app learning (generate commands for all elements)
     * - Context-aware suggestions based on user behavior
     * - Synonym generation for existing commands
     *
     * @param elements List of UI elements on the current screen
     * @param screenContext Current screen context
     * @return List of command suggestions with confidence scores
     * @see CommandSuggestion
     * @see ScreenContext
     */
    suspend fun suggestCommands(
        elements: List<QuantizedElement>,
        screenContext: ScreenContext
    ): List<CommandSuggestion>
}

// =============================================================================
// Context Data Classes
// =============================================================================

/**
 * Context for NLU processing.
 *
 * Provides contextual information to improve intent classification accuracy.
 * The more context provided, the better the disambiguation.
 *
 * ## Example
 * ```kotlin
 * val context = NLUContext(
 *     screenContext = ScreenContext(
 *         packageName = "com.example.app",
 *         screenId = "main_screen",
 *         availableElements = elements
 *     ),
 *     previousUtterances = listOf("open settings", "scroll down"),
 *     availableCommands = listOf("click submit", "go back"),
 *     userPreferences = mapOf("preferredVerb" to "tap")
 * )
 * ```
 *
 * @property screenContext Current screen and UI element context
 * @property previousUtterances Recent utterances for multi-turn understanding
 * @property availableCommands Known commands that can be matched
 * @property userPreferences User-specific preferences and history
 *
 * @since 1.0.0
 * @see NLUPlugin.classifyIntent
 */
@Serializable
data class NLUContext(
    val screenContext: ScreenContext,
    val previousUtterances: List<String> = emptyList(),
    val availableCommands: List<String> = emptyList(),
    val userPreferences: Map<String, String> = emptyMap()
) {
    /**
     * Check if this context has screen information.
     */
    fun hasScreenContext(): Boolean = screenContext.packageName.isNotBlank()

    /**
     * Check if this is a multi-turn conversation.
     */
    fun isMultiTurn(): Boolean = previousUtterances.isNotEmpty()

    companion object {
        /**
         * Create an empty context.
         */
        val EMPTY = NLUContext(screenContext = ScreenContext.EMPTY)

        /**
         * Create context with just screen information.
         */
        fun fromScreen(screenContext: ScreenContext): NLUContext =
            NLUContext(screenContext = screenContext)
    }
}

/**
 * Context describing the current screen state.
 *
 * Provides information about the active screen for NLU disambiguation
 * and command suggestion.
 *
 * @property packageName App package name
 * @property screenId Screen identifier/hash
 * @property screenTitle Human-readable screen title
 * @property availableElements UI elements on the screen
 * @property focusedElementAvid AVID of currently focused element
 * @property metadata Additional screen metadata
 *
 * @since 1.0.0
 */
@Serializable
data class ScreenContext(
    val packageName: String,
    val screenId: String = "",
    val screenTitle: String = "",
    val availableElements: List<String> = emptyList(),
    val focusedElementAvid: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Check if screen has elements.
     */
    fun hasElements(): Boolean = availableElements.isNotEmpty()

    /**
     * Get element count.
     */
    val elementCount: Int get() = availableElements.size

    companion object {
        /**
         * Empty screen context.
         */
        val EMPTY = ScreenContext(packageName = "")

        /**
         * Create minimal screen context.
         */
        fun minimal(packageName: String, screenId: String = ""): ScreenContext =
            ScreenContext(packageName = packageName, screenId = screenId)
    }
}

// =============================================================================
// Result Data Classes
// =============================================================================

/**
 * Result of intent classification.
 *
 * Contains the primary intent, confidence score, extracted entities,
 * and alternative interpretations for disambiguation.
 *
 * ## Usage
 * ```kotlin
 * val result = nluPlugin.classifyIntent("tap the blue button", context)
 * if (result.confidence > 0.8) {
 *     executeIntent(result.intent, result.entities)
 * } else if (result.alternatives.isNotEmpty()) {
 *     askForClarification(result.alternatives)
 * }
 * ```
 *
 * @property intent Primary classified intent
 * @property confidence Confidence score (0.0-1.0)
 * @property entities Extracted entities
 * @property alternatives Alternative intent interpretations
 *
 * @since 1.0.0
 * @see NLUPlugin.classifyIntent
 */
@Serializable
data class IntentResult(
    val intent: String,
    val confidence: Float,
    val entities: List<Entity>,
    val alternatives: List<IntentAlternative> = emptyList()
) {
    /**
     * Check if classification is confident (> 0.7).
     */
    fun isConfident(): Boolean = confidence > 0.7f

    /**
     * Check if classification is ambiguous.
     */
    fun isAmbiguous(): Boolean = confidence < 0.5f || alternatives.any { it.confidence > 0.3f }

    /**
     * Get entity by type.
     */
    fun getEntity(type: String): Entity? = entities.find { it.type == type }

    /**
     * Get all entities of a type.
     */
    fun getEntities(type: String): List<Entity> = entities.filter { it.type == type }

    /**
     * Check if a specific entity type was extracted.
     */
    fun hasEntity(type: String): Boolean = entities.any { it.type == type }

    companion object {
        /**
         * Create an unknown/unclassified result.
         */
        fun unknown(): IntentResult = IntentResult(
            intent = "unknown",
            confidence = 0f,
            entities = emptyList()
        )

        /**
         * Create a high-confidence result.
         */
        fun confident(intent: String, entities: List<Entity> = emptyList()): IntentResult =
            IntentResult(
                intent = intent,
                confidence = 1.0f,
                entities = entities
            )
    }
}

/**
 * Alternative intent interpretation.
 *
 * Represents a secondary possible interpretation of the utterance
 * when the primary classification is not highly confident.
 *
 * @property intent Alternative intent
 * @property confidence Confidence score for this alternative
 *
 * @since 1.0.0
 */
@Serializable
data class IntentAlternative(
    val intent: String,
    val confidence: Float
)

/**
 * Extracted entity from utterance.
 *
 * Represents a named entity extracted from text with its position,
 * normalized value, and confidence score.
 *
 * ## Example
 * For utterance "click the blue submit button":
 * ```kotlin
 * Entity(
 *     type = "element",
 *     value = "blue submit button",
 *     normalizedValue = "submit_button",
 *     start = 10,
 *     end = 28,
 *     confidence = 0.95f
 * )
 * ```
 *
 * @property type Entity type (element, direction, text, etc.)
 * @property value Raw extracted value from utterance
 * @property normalizedValue Normalized/canonical form of the value
 * @property start Start position in the utterance (character index)
 * @property end End position in the utterance (exclusive)
 * @property confidence Confidence score for extraction (0.0-1.0)
 *
 * @since 1.0.0
 * @see NLUPlugin.extractEntities
 */
@Serializable
data class Entity(
    val type: String,
    val value: String,
    val normalizedValue: String = value,
    val start: Int,
    val end: Int,
    val confidence: Float
) {
    /**
     * Check if this entity overlaps with another.
     */
    fun overlaps(other: Entity): Boolean =
        start < other.end && end > other.start

    /**
     * Get the span length.
     */
    val length: Int get() = end - start

    /**
     * Check if normalization changed the value.
     */
    fun isNormalized(): Boolean = normalizedValue != value

    companion object {
        /**
         * Create an element entity.
         */
        fun element(value: String, start: Int, end: Int, confidence: Float = 1f): Entity =
            Entity(
                type = "element",
                value = value,
                normalizedValue = value.lowercase().replace(" ", "_"),
                start = start,
                end = end,
                confidence = confidence
            )

        /**
         * Create a direction entity.
         */
        fun direction(value: String, start: Int, end: Int): Entity =
            Entity(
                type = "direction",
                value = value,
                normalizedValue = normalizeDirection(value),
                start = start,
                end = end,
                confidence = 1f
            )

        /**
         * Create a text entity (for dictation).
         */
        fun text(value: String, start: Int, end: Int): Entity =
            Entity(
                type = "text",
                value = value,
                normalizedValue = value,
                start = start,
                end = end,
                confidence = 1f
            )

        private fun normalizeDirection(value: String): String {
            return when (value.lowercase()) {
                "up", "top", "upward", "upwards" -> "up"
                "down", "bottom", "downward", "downwards" -> "down"
                "left", "leftward", "leftwards" -> "left"
                "right", "rightward", "rightwards" -> "right"
                else -> value.lowercase()
            }
        }
    }
}

/**
 * Suggested voice command for a UI element.
 *
 * Generated by NLU plugins during app learning/exploration to provide
 * natural voice command phrases for UI elements.
 *
 * ## Example
 * For a submit button:
 * ```kotlin
 * CommandSuggestion(
 *     phrase = "submit",
 *     targetAvid = "btn_submit_abc123",
 *     confidence = 0.95f,
 *     synonyms = listOf("send", "confirm", "done")
 * )
 * ```
 *
 * @property phrase Primary voice phrase for the command
 * @property targetAvid AVID of the target element
 * @property confidence Confidence in this suggestion (0.0-1.0)
 * @property synonyms Alternative phrases that could trigger this command
 *
 * @since 1.0.0
 * @see NLUPlugin.suggestCommands
 */
@Serializable
data class CommandSuggestion(
    val phrase: String,
    val targetAvid: String,
    val confidence: Float,
    val synonyms: List<String> = emptyList()
) {
    /**
     * Get all possible phrases (primary + synonyms).
     */
    fun allPhrases(): List<String> = listOf(phrase) + synonyms

    /**
     * Check if this is a high-confidence suggestion.
     */
    fun isHighConfidence(): Boolean = confidence > 0.8f

    companion object {
        /**
         * Create a simple suggestion without synonyms.
         */
        fun simple(phrase: String, targetAvid: String, confidence: Float = 1f): CommandSuggestion =
            CommandSuggestion(
                phrase = phrase,
                targetAvid = targetAvid,
                confidence = confidence
            )
    }
}

// =============================================================================
// Extension Functions
// =============================================================================

/**
 * Check if this plugin supports a specific intent.
 */
fun NLUPlugin.supportsIntent(intent: String): Boolean =
    intent in supportedIntents

/**
 * Check if this plugin supports a specific entity type.
 */
fun NLUPlugin.supportsEntity(entityType: String): Boolean =
    entityType in supportedEntities

/**
 * Classify with minimal context.
 */
suspend fun NLUPlugin.classifySimple(utterance: String): IntentResult =
    classifyIntent(utterance, NLUContext.EMPTY)
