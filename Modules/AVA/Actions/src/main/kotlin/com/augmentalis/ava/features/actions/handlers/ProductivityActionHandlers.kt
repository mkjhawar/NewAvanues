package com.augmentalis.ava.features.actions.handlers

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import com.augmentalis.ava.features.actions.ActionResult
import com.augmentalis.ava.features.actions.IntentActionHandler
import com.augmentalis.ava.features.actions.entities.QueryEntityExtractor
import com.augmentalis.ava.features.actions.entities.URLEntityExtractor
import com.augmentalis.ava.features.actions.web.DuckDuckGoSearchService
import com.augmentalis.ava.features.actions.web.SearchResult

/**
 * Action handler for checking calendar.
 *
 * Opens the calendar app to view upcoming events.
 *
 * Intent: check_calendar (productivity.aot)
 * Utterances: "check calendar", "what's on my calendar", "show my schedule"
 * Entities: date (optional), time_range (optional)
 *
 * Examples:
 * - "check calendar" → Opens calendar to today
 * - "what's on my calendar" → Shows calendar view
 * - "show my schedule" → Opens calendar app
 *
 * Priority: P3 (Week 4)
 * Effort: 3 hours
 */
class CheckCalendarActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "CheckCalendarHandler"
    }

    override val intent = "check_calendar"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Opening calendar for utterance: '$utterance'")

            // Open calendar app to view events
            val calendarIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("content://com.android.calendar/time")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(calendarIntent)
                Log.i(TAG, "Opened calendar view")
                ActionResult.Success(message = "Opening calendar")
            } catch (e: Exception) {
                // Fallback to generic calendar app launch
                Log.d(TAG, "Calendar content URI not available, trying package launch")
                val fallbackIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_CALENDAR)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                try {
                    context.startActivity(fallbackIntent)
                    ActionResult.Success(message = "Opening calendar")
                } catch (fallbackError: Exception) {
                    ActionResult.Failure(
                        message = "Please install a calendar app",
                        exception = fallbackError
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open calendar", e)
            ActionResult.Failure(
                message = "Failed to open calendar: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for web search.
 *
 * Provides two modes:
 * 1. IN_APP (default): Uses DuckDuckGo API to fetch results and display answer in chat
 * 2. BROWSER: Opens browser with search results
 *
 * Intent: search_web (information.ava)
 * Utterances: "search for X", "google Y", "look up Z", "what is A"
 * Entities: query (required)
 *
 * Examples:
 * - "search for cats" → DuckDuckGo search, shows answer in app
 * - "google kotlin tutorials" → Shows tutorial info in app
 * - "what is quantum computing" → Shows definition in app
 *
 * Updated: 2025-12-01 - Added DuckDuckGo API for in-app answers
 */
class SearchWebActionHandler(
    private val searchService: DuckDuckGoSearchService = DuckDuckGoSearchService(),
    private val mode: SearchMode = SearchMode.IN_APP
) : IntentActionHandler {

    companion object {
        private const val TAG = "SearchWebHandler"
    }

    enum class SearchMode {
        IN_APP,   // Fetch results via API, show in chat
        BROWSER   // Open browser with search
    }

    override val intent = "search_web"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Searching web for utterance: '$utterance' (mode=$mode)")

            // Extract query using entity extractor
            val query = QueryEntityExtractor.extract(utterance)

            if (query.isNullOrEmpty()) {
                Log.w(TAG, "Could not extract search query from: $utterance")
                return ActionResult.Failure("I couldn't understand what to search for. Try: 'search for cats'")
            }

            when (mode) {
                SearchMode.IN_APP -> executeInAppSearch(query)
                SearchMode.BROWSER -> executeBrowserSearch(context, query)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to search web", e)
            ActionResult.Failure(
                message = "Failed to search: ${e.message}",
                exception = e
            )
        }
    }

    /**
     * Execute search via DuckDuckGo API and return results in-app.
     */
    private suspend fun executeInAppSearch(query: String): ActionResult {
        Log.d(TAG, "Executing in-app search for: $query")

        val searchResult = searchService.search(query)

        return when (searchResult) {
            is SearchResult.Success -> {
                val response = formatResultsForDisplay(searchResult)
                Log.i(TAG, "In-app search successful for '$query'")
                ActionResult.Success(
                    message = response,
                    data = mapOf(
                        "query" to query,
                        "resultCount" to searchResult.snippets.size,
                        "hasInstantAnswer" to (searchResult.instantAnswer != null),
                        "searchContext" to searchService.formatForLLM(searchResult)
                    )
                )
            }

            is SearchResult.NoResults -> {
                Log.w(TAG, "No results for '$query'")
                ActionResult.Success(
                    message = "I couldn't find specific information about \"$query\". Would you like me to open a web browser to search?",
                    data = mapOf("query" to query, "resultCount" to 0)
                )
            }

            is SearchResult.Error -> {
                Log.e(TAG, "Search error: ${searchResult.message}")
                ActionResult.Failure(
                    message = "I had trouble searching the web. Please check your internet connection and try again."
                )
            }
        }
    }

    /**
     * Execute search by opening browser.
     */
    private fun executeBrowserSearch(context: Context, query: String): ActionResult {
        Log.d(TAG, "Opening browser search for: $query")

        val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(searchIntent)

        Log.i(TAG, "Launched web search for: $query")
        return ActionResult.Success(message = "Searching for $query")
    }

    /**
     * Format search results for display in chat.
     */
    private fun formatResultsForDisplay(result: SearchResult.Success): String {
        return buildString {
            if (result.instantAnswer != null) {
                append(result.instantAnswer)
                if (result.snippets.size > 1) {
                    append("\n\n")
                    append("Related: ")
                    append(result.snippets.drop(1).take(2).joinToString(", ") {
                        it.title.take(30)
                    })
                }
            } else if (result.snippets.isNotEmpty()) {
                append("Here's what I found about \"${result.query}\":\n\n")
                result.snippets.take(3).forEach { snippet ->
                    append("• ${snippet.content.take(200)}")
                    if (snippet.content.length > 200) append("...")
                    append("\n\n")
                }
            }
        }.trim()
    }
}

/**
 * Action handler for URL navigation.
 *
 * Extracts URL from utterance and opens it in the default browser.
 *
 * Intent: navigate_url (NEW - not in AON 3.0, but commonly requested)
 * Utterances: "go to website.com", "open youtube.com", "navigate to example.org"
 * Entities: url (required)
 *
 * Examples:
 * - "go to youtube.com" → Opens https://youtube.com
 * - "open google.com" → Opens https://google.com
 * - "navigate to https://github.com" → Opens GitHub
 *
 * Priority: P0 (Week 1)
 * Effort: 1 hour
 */
class NavigateURLActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "NavigateURLHandler"
    }

    override val intent = "navigate_url"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Navigating to URL for utterance: '$utterance'")

            // Extract URL using entity extractor
            val url = URLEntityExtractor.extract(utterance)

            if (url.isNullOrEmpty()) {
                Log.w(TAG, "Could not extract URL from: $utterance")
                return ActionResult.Failure("I couldn't find a website address. Try: 'go to youtube.com'")
            }

            // Open URL in browser
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)

            Log.i(TAG, "Opened URL: $url")
            ActionResult.Success(message = "Opening $url")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to navigate to URL", e)
            ActionResult.Failure(
                message = "Failed to open website: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for creating reminders.
 *
 * Extracts task/reminder text from utterance and opens Google Tasks or Google Keep.
 * Android doesn't have a universal reminder API, so we use app-specific intents.
 *
 * Intent: create_reminder (productivity.aot)
 * Utterances: "remind me to X", "reminder to buy milk", "don't forget to call John"
 * Entities: task (required), time (optional)
 *
 * Examples:
 * - "remind me to take medicine" → Creates reminder in Google Tasks
 * - "reminder to buy milk at 5pm" → Reminder with time (time extraction TBD)
 * - "don't let me forget to call John" → Creates reminder
 *
 * Priority: P2 (Week 3)
 * Effort: 4 hours
 */
class CreateReminderActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "CreateReminderHandler"
    }

    override val intent = "create_reminder"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Creating reminder for utterance: '$utterance'")

            // Extract reminder task using pattern matching
            val taskPatterns = listOf(
                Regex("remind me to (.+?)(?:at|on|in|$)", RegexOption.IGNORE_CASE),
                Regex("reminder to (.+?)(?:at|on|in|$)", RegexOption.IGNORE_CASE),
                Regex("don't (?:let me )?forget to (.+?)(?:at|on|in|$)", RegexOption.IGNORE_CASE),
                Regex("remember to (.+?)(?:at|on|in|$)", RegexOption.IGNORE_CASE),
                Regex("create reminder (.+?)(?:at|on|in|$)", RegexOption.IGNORE_CASE)
            )

            var task: String? = null
            taskPatterns.forEach { pattern ->
                pattern.find(utterance)?.groupValues?.getOrNull(1)?.let { match ->
                    task = match.trim()
                    return@forEach
                }
            }

            if (task.isNullOrEmpty()) {
                Log.w(TAG, "Could not extract reminder task from: $utterance")
                return ActionResult.Failure("I couldn't understand what to remind you about. Try: 'remind me to buy milk'")
            }

            // Try Google Tasks intent first (most common)
            val tasksIntent = Intent(Intent.ACTION_INSERT).apply {
                data = Uri.parse("content://com.google.android.apps.tasks/tasks")
                putExtra("title", task)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(tasksIntent)
                Log.i(TAG, "Opened Google Tasks for reminder: $task")
                ActionResult.Success(message = "Creating reminder: $task")
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
                    ActionResult.Success(message = "Creating reminder in Google Keep: $task")
                } catch (keepError: Exception) {
                    Log.w(TAG, "Neither Google Tasks nor Keep available")
                    ActionResult.Failure(
                        message = "Please install Google Tasks or Google Keep to create reminders",
                        exception = keepError
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create reminder", e)
            ActionResult.Failure(
                message = "Failed to create reminder: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for creating calendar events.
 *
 * Extracts event title from utterance and opens Calendar app with pre-filled data.
 *
 * Intent: create_calendar_event (productivity.aot)
 * Utterances: "schedule meeting with John", "add to calendar dinner", "create event"
 * Entities: title (required), date (optional), time (optional), location (optional)
 *
 * Examples:
 * - "schedule meeting with John" → Calendar event
 * - "add to calendar dentist appointment" → Opens calendar
 * - "create event team lunch at the cafe" → Event with location
 *
 * Priority: P2 (Week 3)
 * Effort: 5 hours
 */
class CreateCalendarEventActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "CreateCalendarEventHandler"
    }

    override val intent = "create_calendar_event"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Creating calendar event for utterance: '$utterance'")

            // Extract event title using pattern matching
            val titlePatterns = listOf(
                Regex("schedule (.+?)(?:tomorrow|today|on|at|in|$)", RegexOption.IGNORE_CASE),
                Regex("add (?:to calendar|event) (.+?)(?:tomorrow|today|on|at|in|$)", RegexOption.IGNORE_CASE),
                Regex("create event (.+?)(?:tomorrow|today|on|at|in|$)", RegexOption.IGNORE_CASE),
                Regex("(?:schedule|book) (?:meeting|appointment) (?:with )?(.+?)(?:tomorrow|today|on|at|in|$)", RegexOption.IGNORE_CASE),
                Regex("put on calendar (.+?)(?:tomorrow|today|on|at|in|$)", RegexOption.IGNORE_CASE)
            )

            var title: String? = null
            titlePatterns.forEach { pattern ->
                pattern.find(utterance)?.groupValues?.getOrNull(1)?.let { match ->
                    title = match.trim()
                    return@forEach
                }
            }

            if (title.isNullOrEmpty()) {
                Log.w(TAG, "Could not extract event title from: $utterance")
                return ActionResult.Failure("I couldn't understand the event title. Try: 'schedule meeting with John'")
            }

            // Build calendar intent using CalendarContract
            val calendarIntent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)

                // Extract optional location (simple pattern: "at the X")
                val locationPattern = Regex("at the (.+?)(?:tomorrow|today|on|$)", RegexOption.IGNORE_CASE)
                locationPattern.find(utterance)?.groupValues?.getOrNull(1)?.let { location ->
                    putExtra(CalendarContract.Events.EVENT_LOCATION, location.trim())
                }

                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(calendarIntent)

            Log.i(TAG, "Opened calendar for event: $title")
            ActionResult.Success(message = "Creating calendar event: $title")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create calendar event", e)
            ActionResult.Failure(
                message = "Failed to open calendar: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for adding todo items.
 *
 * Extracts task description from utterance and opens Google Tasks.
 *
 * Intent: add_todo (productivity.aot)
 * Utterances: "add to do X", "add task buy groceries", "new task finish report"
 * Entities: task (required), due_date (optional), priority (optional)
 *
 * Examples:
 * - "add to my todo list buy groceries" → Task in Google Tasks
 * - "add task finish report" → New todo
 * - "I need to call the dentist" → Todo from "I need to" pattern
 *
 * Priority: P2 (Week 3)
 * Effort: 3 hours
 */
class AddTodoActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "AddTodoHandler"
    }

    override val intent = "add_todo"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Adding todo for utterance: '$utterance'")

            // Extract task description using pattern matching
            val taskPatterns = listOf(
                Regex("add (?:to|to my|a) (?:todo|to do|task|list) (.+?)(?:by|due|$)", RegexOption.IGNORE_CASE),
                Regex("add task (.+?)(?:by|due|$)", RegexOption.IGNORE_CASE),
                Regex("(?:new|create) task (.+?)(?:by|due|$)", RegexOption.IGNORE_CASE),
                Regex("todo (.+?)(?:by|due|$)", RegexOption.IGNORE_CASE),
                Regex("I need to (.+?)(?:by|due|$)", RegexOption.IGNORE_CASE)
            )

            var task: String? = null
            taskPatterns.forEach { pattern ->
                pattern.find(utterance)?.groupValues?.getOrNull(1)?.let { match ->
                    task = match.trim()
                    return@forEach
                }
            }

            if (task.isNullOrEmpty()) {
                Log.w(TAG, "Could not extract task from: $utterance")
                return ActionResult.Failure("I couldn't understand the task. Try: 'add to do buy milk'")
            }

            // Build Google Tasks intent
            val tasksIntent = Intent(Intent.ACTION_INSERT).apply {
                data = Uri.parse("content://com.google.android.apps.tasks/tasks")
                putExtra("title", task)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(tasksIntent)
                Log.i(TAG, "Opened Google Tasks for todo: $task")
                ActionResult.Success(message = "Adding task: $task")
            } catch (e: Exception) {
                // Fallback to generic todo/note app
                Log.d(TAG, "Google Tasks not available, using generic intent")
                val genericIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, task)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                try {
                    context.startActivity(Intent.createChooser(genericIntent, "Add task to..."))
                    ActionResult.Success(message = "Adding task: $task")
                } catch (chooserError: Exception) {
                    ActionResult.Failure(
                        message = "Please install a task management app",
                        exception = chooserError
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add todo", e)
            ActionResult.Failure(
                message = "Failed to add task: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Action handler for creating notes.
 *
 * Extracts note content from utterance and opens Google Keep or notes app.
 *
 * Intent: create_note (productivity.aot)
 * Utterances: "take a note X", "create note buy milk", "note this meeting notes"
 * Entities: content (required), title (optional)
 *
 * Examples:
 * - "take a note meeting summary from today" → Note in Google Keep
 * - "create note buy milk eggs bread" → Quick note
 * - "note this important reminder" → Creates note
 *
 * Priority: P2 (Week 3)
 * Effort: 3 hours
 */
class CreateNoteActionHandler : IntentActionHandler {
    companion object {
        private const val TAG = "CreateNoteHandler"
    }

    override val intent = "create_note"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Creating note for utterance: '$utterance'")

            // Extract note content using pattern matching
            val contentPatterns = listOf(
                Regex("take (?:a )?note (.+?)(?:titled|called|$)", RegexOption.IGNORE_CASE),
                Regex("create note (.+?)(?:titled|called|$)", RegexOption.IGNORE_CASE),
                Regex("make (?:a )?note (.+?)(?:titled|called|$)", RegexOption.IGNORE_CASE),
                Regex("note this (.+?)(?:titled|called|$)", RegexOption.IGNORE_CASE),
                Regex("jot down (.+?)(?:titled|called|$)", RegexOption.IGNORE_CASE),
                Regex("write down (.+?)(?:titled|called|$)", RegexOption.IGNORE_CASE),
                Regex("save note (.+?)(?:titled|called|$)", RegexOption.IGNORE_CASE)
            )

            var content: String? = null
            contentPatterns.forEach { pattern ->
                pattern.find(utterance)?.groupValues?.getOrNull(1)?.let { match ->
                    content = match.trim()
                    return@forEach
                }
            }

            // Defensive null check - should not happen after extraction
            val finalContent: String = content ?: run {
                Log.w(TAG, "Could not extract note content from: $utterance")
                return ActionResult.Failure("I couldn't understand what to note. Try: 'take a note buy milk'")
            }

            // Try Google Keep first (most common notes app on Android)
            val keepIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, finalContent)
                setPackage("com.google.android.keep")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(keepIntent)
                Log.i(TAG, "Opened Google Keep for note: $finalContent")
                val preview = if (finalContent.length > 50) "${finalContent.take(50)}..." else finalContent
                ActionResult.Success(message = "Creating note: $preview")
            } catch (e: Exception) {
                // Fallback to generic notes app
                Log.d(TAG, "Google Keep not available, using generic intent")
                val genericIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, finalContent)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                try {
                    context.startActivity(Intent.createChooser(genericIntent, "Create note in..."))
                    val preview = if (finalContent.length > 50) "${finalContent.take(50)}..." else finalContent
                    ActionResult.Success(message = "Creating note: $preview")
                } catch (chooserError: Exception) {
                    ActionResult.Failure(
                        message = "Please install a notes app like Google Keep",
                        exception = chooserError
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create note", e)
            ActionResult.Failure(
                message = "Failed to create note: ${e.message}",
                exception = e
            )
        }
    }
}
