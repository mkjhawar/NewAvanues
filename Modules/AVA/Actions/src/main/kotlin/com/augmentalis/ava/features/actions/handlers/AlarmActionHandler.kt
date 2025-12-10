package com.augmentalis.ava.features.actions.handlers

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.util.Log
import com.augmentalis.ava.features.actions.ActionResult
import com.augmentalis.ava.features.actions.IntentActionHandler

/**
 * Action handler for setting alarms.
 *
 * Launches the system Clock app with AlarmClock.ACTION_SET_ALARM intent.
 * This opens the alarm creation screen where user can configure the alarm.
 *
 * Current implementation:
 * - Opens alarm creation screen (user sets time manually)
 * - Uses standard Android AlarmClock intent
 * - Handles case where no clock app is installed
 *
 * Future enhancements (Phase 2):
 * - Parse time from utterance ("set alarm for 7am")
 * - Pass EXTRA_HOUR and EXTRA_MINUTES to pre-fill time
 * - Handle relative times ("alarm in 30 minutes")
 * - Add alarm message/label support (EXTRA_MESSAGE)
 *
 * Intent classification examples:
 * - "Set an alarm"
 * - "Set alarm for 7am"
 * - "Wake me up at 6:30"
 * - "Create alarm"
 *
 * @see IntentActionHandler
 */
class AlarmActionHandler : IntentActionHandler {

    companion object {
        private const val TAG = "AlarmActionHandler"
    }

    override val intent = "set_alarm"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Launching alarm creation for utterance: '$utterance'")

            // Create intent to set alarm
            // Note: Without EXTRA_HOUR/EXTRA_MINUTES, this opens the alarm creation UI
            // where user can manually set the time
            val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK

                // Future: Parse time from utterance and add:
                // putExtra(AlarmClock.EXTRA_HOUR, hour)
                // putExtra(AlarmClock.EXTRA_MINUTES, minutes)
                // putExtra(AlarmClock.EXTRA_MESSAGE, "AVA Alarm")
                // putExtra(AlarmClock.EXTRA_SKIP_UI, false) // Show UI for confirmation
            }

            // Check if there's an app that can handle this intent
            val packageManager = context.packageManager
            val resolveInfo = packageManager.resolveActivity(
                alarmIntent,
                0
            )

            if (resolveInfo != null) {
                // Launch the Clock app alarm creation screen
                context.startActivity(alarmIntent)
                Log.i(TAG, "Successfully launched alarm creation")
                ActionResult.Success(message = "Opening alarm setup")
            } else {
                // No clock app installed (very rare)
                Log.w(TAG, "No clock app found on device")
                ActionResult.Failure(
                    message = "No clock app installed on this device"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch alarm creation", e)
            ActionResult.Failure(
                message = "Failed to set alarm: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for setting timers.
 *
 * Extracts duration from utterance and sets a countdown timer using AlarmClock.ACTION_SET_TIMER.
 *
 * Intent: set_timer (device_control.aot)
 * Utterances: "set timer for 10 minutes", "timer for 5 minutes", "start countdown 30 seconds"
 * Entities: duration (required), label (optional)
 *
 * Examples:
 * - "set timer for 10 minutes" → 10 minute countdown timer
 * - "timer for 5 minutes" → 5 minute timer
 * - "start countdown 30 seconds" → 30 second timer
 *
 * Priority: P2 (Week 3)
 * Effort: 3 hours
 */
class SetTimerActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "SetTimerHandler"
    }

    override val intent = "set_timer"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Setting timer for utterance: '$utterance'")

            // Extract duration in seconds using pattern matching
            val durationPatterns = listOf(
                // Minutes patterns
                Regex("(?:timer|countdown) (?:for )?([0-9]+) ?(?:minute|min)s?", RegexOption.IGNORE_CASE),
                Regex("set (?:timer|countdown) (?:for )?([0-9]+) ?(?:minute|min)s?", RegexOption.IGNORE_CASE),
                // Seconds patterns
                Regex("(?:timer|countdown) (?:for )?([0-9]+) ?(?:second|sec)s?", RegexOption.IGNORE_CASE),
                Regex("set (?:timer|countdown) (?:for )?([0-9]+) ?(?:second|sec)s?", RegexOption.IGNORE_CASE),
                // Hours patterns
                Regex("(?:timer|countdown) (?:for )?([0-9]+) ?hours?", RegexOption.IGNORE_CASE),
                Regex("set (?:timer|countdown) (?:for )?([0-9]+) ?hours?", RegexOption.IGNORE_CASE)
            )

            var durationSeconds: Int? = null
            var unit = "minutes" // default unit

            durationPatterns.forEach { pattern ->
                pattern.find(utterance)?.let { matchResult ->
                    val value = matchResult.groupValues.getOrNull(1)?.toIntOrNull()
                    if (value != null) {
                        // Determine unit from the matched pattern
                        unit = when {
                            matchResult.value.contains("second", ignoreCase = true) ||
                            matchResult.value.contains("sec", ignoreCase = true) -> "seconds"
                            matchResult.value.contains("hour", ignoreCase = true) -> "hours"
                            else -> "minutes"
                        }

                        durationSeconds = when (unit) {
                            "seconds" -> value
                            "hours" -> value * 3600
                            else -> value * 60 // minutes
                        }
                        return@forEach
                    }
                }
            }

            // Defensive null check - ensure we have valid duration
            val finalDurationSeconds: Int = durationSeconds?.takeIf { it > 0 } ?: run {
                Log.w(TAG, "Could not extract valid duration from: $utterance")
                return ActionResult.Failure("I couldn't understand the timer duration. Try: 'set timer for 10 minutes'")
            }

            // Create timer intent
            val timerIntent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, finalDurationSeconds)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true) // Don't show UI, just start timer
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            // Check if there's an app that can handle this intent
            val packageManager = context.packageManager
            val resolveInfo = packageManager.resolveActivity(timerIntent, 0)

            if (resolveInfo != null) {
                context.startActivity(timerIntent)

                // Format friendly duration message
                val friendlyDuration = when (unit) {
                    "seconds" -> "$finalDurationSeconds second${if (finalDurationSeconds == 1) "" else "s"}"
                    "hours" -> "${finalDurationSeconds / 3600} hour${if (finalDurationSeconds / 3600 == 1) "" else "s"}"
                    else -> "${finalDurationSeconds / 60} minute${if (finalDurationSeconds / 60 == 1) "" else "s"}"
                }

                Log.i(TAG, "Started timer for $friendlyDuration")
                ActionResult.Success(message = "Timer set for $friendlyDuration")
            } else {
                Log.w(TAG, "No clock app found on device")
                ActionResult.Failure(message = "No clock app installed on this device")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set timer", e)
            ActionResult.Failure(
                message = "Failed to set timer: ${e.message}",
                exception = e
            )
        }
    }
}
