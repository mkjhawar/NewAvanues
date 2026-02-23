package com.augmentalis.intentactions.actions

import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.util.Log
import com.augmentalis.intentactions.EntityType
import com.augmentalis.intentactions.ExtractedEntities
import com.augmentalis.intentactions.IIntentAction
import com.augmentalis.intentactions.IntentCategory
import com.augmentalis.intentactions.IntentResult
import com.augmentalis.intentactions.PlatformContext

/**
 * Sets an alarm using the system clock app.
 *
 * Opens the alarm creation screen via AlarmClock.ACTION_SET_ALARM.
 * If time entity is provided, it could pre-fill the alarm time (future enhancement).
 */
object SetAlarmAction : IIntentAction {
    private const val TAG = "SetAlarmAction"

    override val intentId = "set_alarm"
    override val category = IntentCategory.PRODUCTIVITY
    override val requiredEntities = emptyList<EntityType>()

    override suspend fun execute(platformCtx: PlatformContext, entities: ExtractedEntities): IntentResult {
        val context = platformCtx.android
        return try {
            Log.d(TAG, "Launching alarm creation")

            val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val resolveInfo = context.packageManager.resolveActivity(alarmIntent, 0)

            if (resolveInfo != null) {
                context.startActivity(alarmIntent)
                Log.i(TAG, "Successfully launched alarm creation")
                IntentResult.Success(message = "Opening alarm setup")
            } else {
                Log.w(TAG, "No clock app found on device")
                IntentResult.Failed(reason = "No clock app installed on this device")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch alarm creation", e)
            IntentResult.Failed(
                reason = "Failed to set alarm: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Sets a countdown timer using AlarmClock.ACTION_SET_TIMER.
 *
 * Parses duration from the entities.duration field or falls back to pattern matching
 * on entities.query.
 */
object SetTimerAction : IIntentAction {
    private const val TAG = "SetTimerAction"

    override val intentId = "set_timer"
    override val category = IntentCategory.PRODUCTIVITY
    override val requiredEntities = listOf(EntityType.DURATION)

    private val durationPatterns = listOf(
        Regex("(?:timer|countdown) (?:for )?([0-9]+) ?(?:minute|min)s?", RegexOption.IGNORE_CASE),
        Regex("set (?:timer|countdown) (?:for )?([0-9]+) ?(?:minute|min)s?", RegexOption.IGNORE_CASE),
        Regex("(?:timer|countdown) (?:for )?([0-9]+) ?(?:second|sec)s?", RegexOption.IGNORE_CASE),
        Regex("set (?:timer|countdown) (?:for )?([0-9]+) ?(?:second|sec)s?", RegexOption.IGNORE_CASE),
        Regex("(?:timer|countdown) (?:for )?([0-9]+) ?hours?", RegexOption.IGNORE_CASE),
        Regex("set (?:timer|countdown) (?:for )?([0-9]+) ?hours?", RegexOption.IGNORE_CASE)
    )

    override suspend fun execute(platformCtx: PlatformContext, entities: ExtractedEntities): IntentResult {
        val context = platformCtx.android
        return try {
            Log.d(TAG, "Setting timer ${entities.toSafeString()}")

            // Try to parse duration from entities or query
            val durationText = entities.duration ?: entities.query ?: ""
            val parsed = parseDuration(durationText)

            if (parsed == null) {
                return IntentResult.NeedsMoreInfo(
                    missingEntity = EntityType.DURATION,
                    prompt = "How long should the timer be? Try: '10 minutes' or '30 seconds'"
                )
            }

            val (durationSeconds, unit) = parsed

            val timerIntent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, durationSeconds)
                putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            val resolveInfo = context.packageManager.resolveActivity(timerIntent, 0)

            if (resolveInfo != null) {
                context.startActivity(timerIntent)

                val friendlyDuration = when (unit) {
                    "seconds" -> "$durationSeconds second${if (durationSeconds == 1) "" else "s"}"
                    "hours" -> "${durationSeconds / 3600} hour${if (durationSeconds / 3600 == 1) "" else "s"}"
                    else -> "${durationSeconds / 60} minute${if (durationSeconds / 60 == 1) "" else "s"}"
                }

                Log.i(TAG, "Started timer for $friendlyDuration")
                IntentResult.Success(message = "Timer set for $friendlyDuration")
            } else {
                Log.w(TAG, "No clock app found on device")
                IntentResult.Failed(reason = "No clock app installed on this device")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set timer", e)
            IntentResult.Failed(
                reason = "Failed to set timer: ${e.message}",
                exception = e
            )
        }
    }

    private fun parseDuration(text: String): Pair<Int, String>? {
        var durationSeconds: Int? = null
        var unit = "minutes"

        durationPatterns.forEach { pattern ->
            pattern.find(text)?.let { matchResult ->
                val value = matchResult.groupValues.getOrNull(1)?.toIntOrNull()
                if (value != null) {
                    unit = when {
                        matchResult.value.contains("second", ignoreCase = true) ||
                        matchResult.value.contains("sec", ignoreCase = true) -> "seconds"
                        matchResult.value.contains("hour", ignoreCase = true) -> "hours"
                        else -> "minutes"
                    }
                    durationSeconds = when (unit) {
                        "seconds" -> value
                        "hours" -> value * 3600
                        else -> value * 60
                    }
                    return@forEach
                }
            }
        }

        // Fallback: try to extract a plain number and assume minutes
        if (durationSeconds == null) {
            val numberMatch = Regex("(\\d+)").find(text)
            numberMatch?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { value ->
                if (value > 0) {
                    durationSeconds = value * 60
                    unit = "minutes"
                }
            }
        }

        return durationSeconds?.takeIf { it > 0 }?.let { it to unit }
    }
}

/**
 * Creates a reminder using Google Tasks or Google Keep.
 */
object CreateReminderAction : IIntentAction {
    private const val TAG = "CreateReminderAction"

    override val intentId = "create_reminder"
    override val category = IntentCategory.PRODUCTIVITY
    override val requiredEntities = listOf(EntityType.QUERY)

    override suspend fun execute(platformCtx: PlatformContext, entities: ExtractedEntities): IntentResult {
        val context = platformCtx.android
        return try {
            Log.d(TAG, "Creating reminder ${entities.toSafeString()}")

            val task = entities.query ?: entities.message
            if (task.isNullOrBlank()) {
                return IntentResult.NeedsMoreInfo(
                    missingEntity = EntityType.QUERY,
                    prompt = "What should I remind you about?"
                )
            }

            // Try Google Tasks intent first
            val tasksIntent = Intent(Intent.ACTION_INSERT).apply {
                data = Uri.parse("content://com.google.android.apps.tasks/tasks")
                putExtra("title", task)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(tasksIntent)
                Log.i(TAG, "Opened Google Tasks for reminder: $task")
                IntentResult.Success(message = "Creating reminder: $task")
            } catch (e: Exception) {
                // Fallback to Google Keep
                Log.d(TAG, "Google Tasks not available, trying Google Keep")
                val keepIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, task)
                    setPackage("com.google.android.keep")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                try {
                    context.startActivity(keepIntent)
                    Log.i(TAG, "Opened Google Keep for reminder: $task")
                    IntentResult.Success(message = "Creating reminder in Google Keep: $task")
                } catch (keepError: Exception) {
                    Log.w(TAG, "Neither Google Tasks nor Keep available")
                    IntentResult.Failed(
                        reason = "Please install Google Tasks or Google Keep to create reminders",
                        exception = keepError
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create reminder", e)
            IntentResult.Failed(
                reason = "Failed to create reminder: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Creates a calendar event using CalendarContract.
 */
object CreateCalendarEventAction : IIntentAction {
    private const val TAG = "CreateCalendarEventAction"

    override val intentId = "create_calendar_event"
    override val category = IntentCategory.PRODUCTIVITY
    override val requiredEntities = listOf(EntityType.QUERY)

    override suspend fun execute(platformCtx: PlatformContext, entities: ExtractedEntities): IntentResult {
        val context = platformCtx.android
        return try {
            Log.d(TAG, "Creating calendar event ${entities.toSafeString()}")

            val title = entities.query
            if (title.isNullOrBlank()) {
                return IntentResult.NeedsMoreInfo(
                    missingEntity = EntityType.QUERY,
                    prompt = "What is the event about? Say something like 'meeting with John'."
                )
            }

            val calendarIntent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                entities.location?.takeIf { it.isNotBlank() }?.let {
                    putExtra(CalendarContract.Events.EVENT_LOCATION, it)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(calendarIntent)

            Log.i(TAG, "Opened calendar for event: $title")
            IntentResult.Success(message = "Creating calendar event: $title")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create calendar event", e)
            IntentResult.Failed(
                reason = "Failed to open calendar: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Adds a todo item using Google Tasks or a generic task app.
 */
object AddTodoAction : IIntentAction {
    private const val TAG = "AddTodoAction"

    override val intentId = "add_todo"
    override val category = IntentCategory.PRODUCTIVITY
    override val requiredEntities = listOf(EntityType.QUERY)

    override suspend fun execute(platformCtx: PlatformContext, entities: ExtractedEntities): IntentResult {
        val context = platformCtx.android
        return try {
            Log.d(TAG, "Adding todo ${entities.toSafeString()}")

            val task = entities.query ?: entities.message
            if (task.isNullOrBlank()) {
                return IntentResult.NeedsMoreInfo(
                    missingEntity = EntityType.QUERY,
                    prompt = "What task would you like to add?"
                )
            }

            val tasksIntent = Intent(Intent.ACTION_INSERT).apply {
                data = Uri.parse("content://com.google.android.apps.tasks/tasks")
                putExtra("title", task)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(tasksIntent)
                Log.i(TAG, "Opened Google Tasks for todo: $task")
                IntentResult.Success(message = "Adding task: $task")
            } catch (e: Exception) {
                Log.d(TAG, "Google Tasks not available, using generic intent")
                val genericIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, task)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                try {
                    context.startActivity(Intent.createChooser(genericIntent, "Add task to..."))
                    IntentResult.Success(message = "Adding task: $task")
                } catch (chooserError: Exception) {
                    IntentResult.Failed(
                        reason = "Please install a task management app",
                        exception = chooserError
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add todo", e)
            IntentResult.Failed(
                reason = "Failed to add task: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Creates a note using Google Keep or a generic notes app.
 */
object CreateNoteAction : IIntentAction {
    private const val TAG = "CreateNoteAction"

    override val intentId = "create_note"
    override val category = IntentCategory.PRODUCTIVITY
    override val requiredEntities = listOf(EntityType.QUERY)

    override suspend fun execute(platformCtx: PlatformContext, entities: ExtractedEntities): IntentResult {
        val context = platformCtx.android
        return try {
            Log.d(TAG, "Creating note ${entities.toSafeString()}")

            val content = entities.query ?: entities.message
            if (content.isNullOrBlank()) {
                return IntentResult.NeedsMoreInfo(
                    missingEntity = EntityType.QUERY,
                    prompt = "What would you like to note down?"
                )
            }

            val keepIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, content)
                setPackage("com.google.android.keep")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(keepIntent)
                Log.i(TAG, "Opened Google Keep for note: $content")
                val preview = if (content.length > 50) "${content.take(50)}..." else content
                IntentResult.Success(message = "Creating note: $preview")
            } catch (e: Exception) {
                Log.d(TAG, "Google Keep not available, using generic intent")
                val genericIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, content)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                try {
                    context.startActivity(Intent.createChooser(genericIntent, "Create note in..."))
                    val preview = if (content.length > 50) "${content.take(50)}..." else content
                    IntentResult.Success(message = "Creating note: $preview")
                } catch (chooserError: Exception) {
                    IntentResult.Failed(
                        reason = "Please install a notes app like Google Keep",
                        exception = chooserError
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create note", e)
            IntentResult.Failed(
                reason = "Failed to create note: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Opens the calendar app to view upcoming events.
 */
object CheckCalendarAction : IIntentAction {
    private const val TAG = "CheckCalendarAction"

    override val intentId = "check_calendar"
    override val category = IntentCategory.PRODUCTIVITY
    override val requiredEntities = emptyList<EntityType>()

    override suspend fun execute(platformCtx: PlatformContext, entities: ExtractedEntities): IntentResult {
        val context = platformCtx.android
        return try {
            Log.d(TAG, "Opening calendar")

            val calendarIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("content://com.android.calendar/time")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(calendarIntent)
                Log.i(TAG, "Opened calendar view")
                IntentResult.Success(message = "Opening calendar")
            } catch (e: Exception) {
                Log.d(TAG, "Calendar content URI not available, trying package launch")
                val fallbackIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_CALENDAR)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                try {
                    context.startActivity(fallbackIntent)
                    IntentResult.Success(message = "Opening calendar")
                } catch (fallbackError: Exception) {
                    IntentResult.Failed(
                        reason = "Please install a calendar app",
                        exception = fallbackError
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open calendar", e)
            IntentResult.Failed(
                reason = "Failed to open calendar: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Returns the current time and date without launching an external app.
 */
object GetTimeAction : IIntentAction {
    private const val TAG = "GetTimeAction"

    override val intentId = "show_time"
    override val category = IntentCategory.PRODUCTIVITY
    override val requiredEntities = emptyList<EntityType>()

    override suspend fun execute(platformCtx: PlatformContext, entities: ExtractedEntities): IntentResult {
        return try {
            Log.d(TAG, "Getting current time")

            val currentTime = java.text.SimpleDateFormat(
                "h:mm a",
                java.util.Locale.getDefault()
            ).format(java.util.Date())

            val currentDate = java.text.SimpleDateFormat(
                "EEEE, MMMM d",
                java.util.Locale.getDefault()
            ).format(java.util.Date())

            Log.i(TAG, "Current time: $currentTime on $currentDate")
            IntentResult.Success(message = "It's $currentTime on $currentDate")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current time", e)
            IntentResult.Failed(
                reason = "Failed to get time: ${e.message}",
                exception = e
            )
        }
    }
}
