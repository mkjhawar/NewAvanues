package com.augmentalis.ava.features.actions.handlers

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.util.Log
import com.augmentalis.ava.features.actions.ActionResult
import com.augmentalis.ava.features.actions.IntentActionHandler

/**
 * Action handler for showing the current time.
 *
 * Launches the system Clock app using AlarmClock.ACTION_SHOW_ALARMS intent.
 * This is a standard Android intent supported by all clock apps.
 *
 * Behavior:
 * - Launches Clock app (typically shows clock/alarms tab)
 * - Uses FLAG_ACTIVITY_NEW_TASK for launching from non-activity context
 * - Handles case where no clock app is installed (unlikely but possible)
 *
 * Intent classification examples:
 * - "What time is it?"
 * - "Show me the time"
 * - "Show clock"
 * - "Time?"
 *
 * @see IntentActionHandler
 */
class TimeActionHandler : IntentActionHandler {

    companion object {
        private const val TAG = "TimeActionHandler"
    }

    override val intent = "show_time"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Getting current time for utterance: '$utterance'")

            // Get current time formatted for user's locale
            val currentTime = java.text.SimpleDateFormat(
                "h:mm a",
                java.util.Locale.getDefault()
            ).format(java.util.Date())

            // Get current date
            val currentDate = java.text.SimpleDateFormat(
                "EEEE, MMMM d",
                java.util.Locale.getDefault()
            ).format(java.util.Date())

            Log.i(TAG, "Current time: $currentTime on $currentDate")

            // Return time directly in response instead of opening external app
            ActionResult.Success(
                message = "It's $currentTime on $currentDate"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current time", e)
            ActionResult.Failure(
                message = "Failed to get time: ${e.message}",
                exception = e
            )
        }
    }
}
