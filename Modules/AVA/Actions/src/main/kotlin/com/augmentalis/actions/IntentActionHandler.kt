package com.augmentalis.actions

import android.content.Context

/**
 * Interface for intent action handlers.
 *
 * Each handler is responsible for executing a specific intent action
 * (e.g., launching Clock app, setting alarm, opening weather).
 *
 * Design principles:
 * - One handler per intent (Single Responsibility)
 * - Handlers are stateless and reusable
 * - Returns ActionResult for uniform error handling
 * - Receives full utterance for parameter extraction (future)
 *
 * Implementation guidelines:
 * - Use Android standard intents (AlarmClock, Intent.ACTION_VIEW)
 * - Handle missing apps gracefully (fallback to web or error message)
 * - Log all actions for debugging
 * - Keep execution fast (<100ms typical, <500ms max)
 *
 * Example:
 * ```
 * class TimeActionHandler : IntentActionHandler {
 *     override val intent = "show_time"
 *
 *     override suspend fun execute(context: Context, utterance: String): ActionResult {
 *         // Launch Clock app using AlarmClock.ACTION_SHOW_ALARMS
 *         return ActionResult.Success("Opening clock app")
 *     }
 * }
 * ```
 *
 * @see ActionResult
 * @see IntentActionHandlerRegistry
 */
interface IntentActionHandler {

    /**
     * Intent name this handler supports (e.g., "show_time", "set_alarm").
     * Must match intent from BuiltInIntents or user-taught intents.
     */
    val intent: String

    /**
     * Execute the action for this intent.
     *
     * @param context Android context for launching intents
     * @param utterance Original user utterance (for parameter extraction in future)
     * @return ActionResult indicating success or failure
     */
    suspend fun execute(context: Context, utterance: String): ActionResult
}
