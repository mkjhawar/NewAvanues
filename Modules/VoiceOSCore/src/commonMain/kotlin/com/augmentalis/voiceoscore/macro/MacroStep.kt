package com.augmentalis.voiceoscore.macro

import kotlinx.serialization.Serializable

/**
 * Represents a single step in a macro (automated command sequence).
 *
 * Macros compose steps from two systems:
 * - VoiceAction: Dispatches through VoiceOSCore's ActionCoordinator (gestures, system controls, module commands)
 * - IntentStep: Dispatches through IntentActionRegistry (launch apps, communicate, navigate)
 * - Delay: Pauses between steps
 * - Conditional: Branches based on runtime conditions
 *
 * Example macro "Start Field Inspection":
 * ```
 * listOf(
 *     MacroStep.IntentStep("open_app", mapOf("appName" to "com.augmentalis.avanues")),
 *     MacroStep.Delay(1000),
 *     MacroStep.VoiceAction("open cockpit"),
 *     MacroStep.VoiceAction("add camera frame"),
 *     MacroStep.VoiceAction("add note frame"),
 *     MacroStep.VoiceAction("layout split left"),
 *     MacroStep.IntentStep("get_directions", mapOf("location" to "job site"))
 * )
 * ```
 *
 * @see MacroDefinition
 */
@Serializable
sealed class MacroStep {
    /**
     * Execute a voice command through VoiceOSCore's ActionCoordinator.
     * Handles: gestures, system controls, media, text manipulation, module commands.
     *
     * @param command The voice command phrase (e.g., "scroll down", "bold", "add camera frame")
     * @param confidence Confidence level for command matching (default 1.0 for macros)
     */
    @Serializable
    data class VoiceAction(
        val command: String,
        val confidence: Float = 1.0f
    ) : MacroStep()

    /**
     * Execute an intent action through IntentActionRegistry.
     * Handles: app launches, communication, navigation, productivity, search.
     *
     * @param intentId The intent identifier (e.g., "send_email", "navigate_map")
     * @param entities Pre-defined entity values (bypasses NLU extraction since macro knows the values)
     */
    @Serializable
    data class IntentStep(
        val intentId: String,
        val entities: Map<String, String> = emptyMap()
    ) : MacroStep()

    /**
     * Pause execution for a specified duration.
     * Useful for waiting for UI transitions, app launches, or animations.
     *
     * @param ms Duration in milliseconds
     */
    @Serializable
    data class Delay(val ms: Long) : MacroStep()

    /**
     * Conditional branching based on a runtime condition.
     *
     * @param condition Condition expression to evaluate (e.g., "screen.hasElement('Submit')")
     * @param thenSteps Steps to execute if condition is true
     * @param elseSteps Steps to execute if condition is false (default empty)
     */
    @Serializable
    data class Conditional(
        val condition: String,
        val thenSteps: List<MacroStep>,
        val elseSteps: List<MacroStep> = emptyList()
    ) : MacroStep()
}

/**
 * A named, reusable macro definition.
 *
 * @param id Unique identifier
 * @param name User-visible name (e.g., "Start Field Inspection")
 * @param description Brief description of what the macro does
 * @param steps Ordered list of macro steps to execute
 * @param voicePhrase Optional voice trigger phrase (e.g., "start inspection")
 * @param icon Optional icon identifier for UI display
 */
@Serializable
data class MacroDefinition(
    val id: String,
    val name: String,
    val description: String,
    val steps: List<MacroStep>,
    val voicePhrase: String? = null,
    val icon: String? = null
)

/**
 * Result of executing a single macro step.
 */
data class MacroStepResult(
    val step: MacroStep,
    val success: Boolean,
    val message: String? = null,
    val durationMs: Long = 0
)
