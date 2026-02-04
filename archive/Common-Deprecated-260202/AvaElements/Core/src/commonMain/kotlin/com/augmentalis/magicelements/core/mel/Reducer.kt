package com.augmentalis.magicelements.core.mel

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Reducer data model for state transitions.
 *
 * Reducers define how state changes in response to actions/events.
 * They are declarative in Tier 1 and support effects in Tier 2.
 *
 * Example:
 * ```yaml
 * appendDigit:
 *   params: [digit]
 *   next_state:
 *     display: $string.concat($state.display, $digit)
 * ```
 */
@Serializable
data class Reducer(
    /**
     * List of parameter names for this reducer.
     * When the reducer is dispatched, values for these parameters must be provided.
     * Example: ["digit"] for appendDigit(7)
     */
    val params: List<String> = emptyList(),

    /**
     * Map of state paths to raw expression strings.
     * These expressions compute the new values for state variables.
     * Keys are state variable names (e.g., "display", "count").
     * Values are raw expression strings that will be parsed and evaluated.
     *
     * Example:
     * ```
     * next_state = mapOf(
     *   "count" to "$math.add($state.count, 1)",
     *   "display" to "$string.concat($state.display, $digit)"
     * )
     * ```
     */
    val next_state: Map<String, String>,

    /**
     * Optional side effects to execute after state update (Tier 2 only).
     * Effects are ignored on Tier 1 platforms (Apple).
     *
     * Example effects:
     * - navigation: { type: "navigate", config: { screen: "home" } }
     * - haptic feedback: { type: "haptic", config: { intensity: "medium" } }
     * - storage: { type: "storage", config: { action: "save", key: "data" } }
     */
    val effects: List<Effect>? = null
)

/**
 * Side effect definition (Tier 2 only).
 *
 * Effects allow plugins to perform actions beyond state updates:
 * - Navigate to different screens
 * - Trigger haptic feedback
 * - Save data to storage
 * - Show notifications
 *
 * These are only executed on non-Apple platforms (Tier 2).
 */
@Serializable
data class Effect(
    /**
     * Type of effect to execute.
     *
     * Standard types:
     * - "navigate": Navigate to a different screen/view
     * - "haptic": Trigger haptic feedback
     * - "storage": Interact with persistent storage
     * - "notification": Show a notification
     * - "clipboard": Copy to clipboard
     * - "audio": Play a sound
     */
    val type: String,

    /**
     * Configuration for the effect.
     * Structure depends on the effect type.
     *
     * Examples:
     * - navigate: { "screen": "home", "params": { "id": 123 } }
     * - haptic: { "intensity": "medium" }
     * - storage: { "action": "save", "key": "history", "value": "$state.history" }
     * - notification: { "title": "Saved", "message": "Data saved successfully" }
     */
    val config: Map<String, JsonElement>
)

/**
 * Result of a reducer execution.
 *
 * Contains the computed state updates and any effects to execute.
 */
data class ReducerResult(
    /**
     * Map of state paths to their new values.
     * These values are ready to be applied to the state.
     */
    val stateUpdates: Map<String, Any?>,

    /**
     * List of effects to execute (Tier 2 only).
     * Empty if reducer has no effects or running on Tier 1 platform.
     */
    val effects: List<Effect> = emptyList()
)

/**
 * Exception thrown when a reducer execution fails.
 */
class ReducerException(
    message: String,
    val reducerName: String,
    cause: Throwable? = null
) : Exception("Reducer '$reducerName': $message", cause)

/**
 * Exception thrown when required reducer parameters are missing.
 */
class MissingParameterException(
    val parameterName: String,
    val reducerName: String
) : ReducerException(
    "Missing required parameter '$parameterName'",
    reducerName
)

/**
 * Exception thrown when a reducer expression evaluation fails.
 */
class ExpressionEvaluationException(
    message: String,
    val expression: String,
    val reducerName: String,
    cause: Throwable? = null
) : ReducerException(
    "Expression evaluation failed: $message\nExpression: $expression",
    reducerName,
    cause
)
