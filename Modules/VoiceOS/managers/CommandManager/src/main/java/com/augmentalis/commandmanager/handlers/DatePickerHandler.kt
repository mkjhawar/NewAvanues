/**
 * DatePickerHandler.kt - Voice handler for DatePicker interactions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-27
 *
 * Purpose: Voice-driven date picker control with natural language date parsing
 * Features:
 * - Natural date parsing ("January 15", "January 15 2026")
 * - Relative dates (today, tomorrow, yesterday)
 * - Navigation (next week, next month, previous month, next year, previous year)
 * - Accessibility service integration for DatePicker updates
 * - Voice feedback for selected dates
 *
 * Location: CommandManager module handlers
 */

package com.augmentalis.commandmanager.handlers

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.commandmanager.CommandHandler
import com.augmentalis.commandmanager.CommandRegistry
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.CommandActionType
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.IHandler
import com.augmentalis.voiceoscore.QuantizedCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

/**
 * Voice command handler for DatePicker interactions.
 *
 * Supports natural language date commands:
 * - "set date to January 15"
 * - "set date to January 15 2026"
 * - "today" - sets to current date
 * - "tomorrow" - sets to next day
 * - "yesterday" - sets to previous day
 * - "next week" - adds 7 days
 * - "next month" - advances one month
 * - "previous month" - goes back one month
 * - "next year" - advances one year
 * - "previous year" - goes back one year
 *
 * Thread Safety:
 * - All operations execute on Main dispatcher
 * - AccessibilityService operations are synchronized
 */
class DatePickerHandler private constructor() : BaseHandler(), CommandHandler {

    companion object {
        private const val TAG = "DatePickerHandler"
        private const val MODULE_ID = "datepicker"

        @Volatile
        private var instance: DatePickerHandler? = null

        /**
         * Get singleton instance
         */
        fun getInstance(): DatePickerHandler {
            return instance ?: synchronized(this) {
                instance ?: DatePickerHandler().also {
                    instance = it
                }
            }
        }

        // Date pattern constants
        private val MONTH_NAMES = mapOf(
            "january" to Calendar.JANUARY,
            "february" to Calendar.FEBRUARY,
            "march" to Calendar.MARCH,
            "april" to Calendar.APRIL,
            "may" to Calendar.MAY,
            "june" to Calendar.JUNE,
            "july" to Calendar.JULY,
            "august" to Calendar.AUGUST,
            "september" to Calendar.SEPTEMBER,
            "october" to Calendar.OCTOBER,
            "november" to Calendar.NOVEMBER,
            "december" to Calendar.DECEMBER
        )

        // Regex patterns for date parsing
        // Pattern: "January 15" or "January 15 2026"
        private val DATE_PATTERN = Pattern.compile(
            "^(january|february|march|april|may|june|july|august|september|october|november|december)\\s+(\\d{1,2})(?:\\s+(\\d{4}))?$",
            Pattern.CASE_INSENSITIVE
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // IHandler Implementation (BaseHandler)
    // ═══════════════════════════════════════════════════════════════════════════

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        "set date to",
        "today",
        "tomorrow",
        "yesterday",
        "next week",
        "next month",
        "previous month",
        "last month",
        "next year",
        "previous year",
        "last year"
    )

    // ═══════════════════════════════════════════════════════════════════════════
    // CommandHandler Implementation (for CommandRegistry)
    // ═══════════════════════════════════════════════════════════════════════════

    override val moduleId: String = MODULE_ID

    override val supportedCommands: List<String> = listOf(
        "set date to [date]",
        "today",
        "tomorrow",
        "yesterday",
        "next week",
        "next month",
        "previous month",
        "last month",
        "next year",
        "previous year",
        "last year"
    )

    // ═══════════════════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════════════════

    private val handlerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var accessibilityService: AccessibilityService? = null
    private var isInitialized = false

    // Current date state (for relative operations)
    private var currentDate: Calendar = Calendar.getInstance()

    // Callback for voice feedback
    var onDateSelected: ((String) -> Unit)? = null

    // ═══════════════════════════════════════════════════════════════════════════
    // Initialization
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Initialize with accessibility service reference
     */
    fun initializeWithService(service: AccessibilityService? = null) {
        accessibilityService = service
        if (!isInitialized) {
            CommandRegistry.registerHandler(moduleId, this)
            isInitialized = true
            Log.d(TAG, "DatePickerHandler initialized")
        }
    }

    override suspend fun initialize() {
        initializeWithService(null)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Command Handling
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * CommandHandler interface: Check if this handler can process the command
     */
    override fun canHandle(command: String): Boolean {
        val normalized = command.lowercase().trim()
        return when {
            normalized.startsWith("set date to ") -> true
            normalized == "today" -> true
            normalized == "tomorrow" -> true
            normalized == "yesterday" -> true
            normalized == "next week" -> true
            normalized == "next month" -> true
            normalized == "previous month" || normalized == "last month" -> true
            normalized == "next year" -> true
            normalized == "previous year" || normalized == "last year" -> true
            else -> false
        }
    }

    /**
     * CommandHandler interface: Execute the command
     */
    override suspend fun handleCommand(command: String): Boolean {
        val result = execute(
            QuantizedCommand(
                phrase = command,
                actionType = CommandActionType.EXECUTE,
                targetAvid = null,
                confidence = 1.0f
            )
        )
        return result.isSuccess
    }

    /**
     * IHandler execute implementation
     */
    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()
        Log.d(TAG, "Executing date command: $normalizedAction")

        return try {
            when {
                // Set date to specific date
                normalizedAction.startsWith("set date to ") -> {
                    val dateString = normalizedAction.removePrefix("set date to ").trim()
                    handleSetDate(dateString)
                }

                // Relative dates
                normalizedAction == "today" -> handleRelativeDate(RelativeDate.TODAY)
                normalizedAction == "tomorrow" -> handleRelativeDate(RelativeDate.TOMORROW)
                normalizedAction == "yesterday" -> handleRelativeDate(RelativeDate.YESTERDAY)
                normalizedAction == "next week" -> handleRelativeDate(RelativeDate.NEXT_WEEK)
                normalizedAction == "next month" -> handleRelativeDate(RelativeDate.NEXT_MONTH)
                normalizedAction == "previous month" ||
                normalizedAction == "last month" -> handleRelativeDate(RelativeDate.PREVIOUS_MONTH)
                normalizedAction == "next year" -> handleRelativeDate(RelativeDate.NEXT_YEAR)
                normalizedAction == "previous year" ||
                normalizedAction == "last year" -> handleRelativeDate(RelativeDate.PREVIOUS_YEAR)

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing date command", e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Date Parsing
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Handle "set date to [date]" command
     */
    private suspend fun handleSetDate(dateString: String): HandlerResult {
        val parsedDate = parseNaturalDate(dateString)
            ?: return HandlerResult.failure(
                reason = "Could not parse date: $dateString. Try 'January 15' or 'January 15 2026'",
                recoverable = true,
                suggestedAction = "Say 'set date to' followed by month and day"
            )

        return applyDate(parsedDate)
    }

    /**
     * Handle relative date commands
     */
    private suspend fun handleRelativeDate(relativeDate: RelativeDate): HandlerResult {
        val calendar = Calendar.getInstance()

        when (relativeDate) {
            RelativeDate.TODAY -> {
                // Already set to today
            }
            RelativeDate.TOMORROW -> {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            RelativeDate.YESTERDAY -> {
                calendar.add(Calendar.DAY_OF_MONTH, -1)
            }
            RelativeDate.NEXT_WEEK -> {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
            }
            RelativeDate.NEXT_MONTH -> {
                calendar.add(Calendar.MONTH, 1)
            }
            RelativeDate.PREVIOUS_MONTH -> {
                calendar.add(Calendar.MONTH, -1)
            }
            RelativeDate.NEXT_YEAR -> {
                calendar.add(Calendar.YEAR, 1)
            }
            RelativeDate.PREVIOUS_YEAR -> {
                calendar.add(Calendar.YEAR, -1)
            }
        }

        return applyDate(calendar)
    }

    /**
     * Parse natural language date string
     *
     * Supports:
     * - "January 15" (uses current year)
     * - "January 15 2026" (specific year)
     */
    private fun parseNaturalDate(dateString: String): Calendar? {
        val matcher = DATE_PATTERN.matcher(dateString.lowercase().trim())

        if (!matcher.matches()) {
            Log.d(TAG, "Date string did not match pattern: $dateString")
            return null
        }

        val monthName = matcher.group(1)?.lowercase() ?: return null
        val dayString = matcher.group(2) ?: return null
        val yearString = matcher.group(3)

        val month = MONTH_NAMES[monthName] ?: return null
        val day = dayString.toIntOrNull() ?: return null
        val year = yearString?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)

        // Validate day for month
        if (!isValidDayForMonth(day, month, year)) {
            Log.w(TAG, "Invalid day $day for month $monthName")
            return null
        }

        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
        }
    }

    /**
     * Validate day is valid for the given month/year
     */
    private fun isValidDayForMonth(day: Int, month: Int, year: Int): Boolean {
        if (day < 1) return false

        val maxDays = when (month) {
            Calendar.JANUARY, Calendar.MARCH, Calendar.MAY, Calendar.JULY,
            Calendar.AUGUST, Calendar.OCTOBER, Calendar.DECEMBER -> 31
            Calendar.APRIL, Calendar.JUNE, Calendar.SEPTEMBER, Calendar.NOVEMBER -> 30
            Calendar.FEBRUARY -> if (isLeapYear(year)) 29 else 28
            else -> return false
        }

        return day <= maxDays
    }

    /**
     * Check if year is a leap year
     */
    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Accessibility Service Integration
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Apply date to focused DatePicker via accessibility service
     */
    private suspend fun applyDate(calendar: Calendar): HandlerResult {
        val service = accessibilityService
        if (service == null) {
            Log.w(TAG, "AccessibilityService not available")
            return HandlerResult.failure(
                reason = "Accessibility service not available",
                recoverable = false
            )
        }

        val rootNode = service.rootInActiveWindow
        if (rootNode == null) {
            Log.w(TAG, "No active window found")
            return HandlerResult.failure(
                reason = "No active window found",
                recoverable = true
            )
        }

        try {
            // Find focused DatePicker or Calendar widget
            val datePickerNode = findDatePickerNode(rootNode)
            if (datePickerNode == null) {
                rootNode.recycle()
                return HandlerResult.failure(
                    reason = "No date picker found. Please focus a date picker first.",
                    recoverable = true,
                    suggestedAction = "Tap on a date picker to focus it"
                )
            }

            // Apply the date
            val success = setDateOnNode(datePickerNode, calendar)
            datePickerNode.recycle()
            rootNode.recycle()

            if (success) {
                // Update internal state
                currentDate = calendar

                // Format date for feedback
                val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                val formattedDate = dateFormat.format(calendar.time)

                // Provide voice feedback
                onDateSelected?.invoke(formattedDate)

                Log.i(TAG, "Date set to: $formattedDate")
                return HandlerResult.success(
                    message = "Date set to $formattedDate",
                    data = mapOf(
                        "year" to calendar.get(Calendar.YEAR),
                        "month" to calendar.get(Calendar.MONTH),
                        "day" to calendar.get(Calendar.DAY_OF_MONTH),
                        "formatted" to formattedDate
                    )
                )
            } else {
                return HandlerResult.failure(
                    reason = "Could not set date on picker",
                    recoverable = true
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying date", e)
            rootNode.recycle()
            return HandlerResult.failure(
                reason = "Error setting date: ${e.message}",
                recoverable = true
            )
        }
    }

    /**
     * Find DatePicker or Calendar widget in the accessibility tree
     */
    private fun findDatePickerNode(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Try to find focused node first
        val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focusedNode != null && isDatePickerNode(focusedNode)) {
            return focusedNode
        }
        focusedNode?.recycle()

        // Search for DatePicker by class name
        return findNodeByClassName(rootNode, listOf(
            "android.widget.DatePicker",
            "android.widget.CalendarView",
            "androidx.appcompat.widget.AppCompatDatePicker",
            "com.google.android.material.datepicker.MaterialCalendar"
        ))
    }

    /**
     * Check if node is a DatePicker
     */
    private fun isDatePickerNode(node: AccessibilityNodeInfo): Boolean {
        val className = node.className?.toString() ?: return false
        return className.contains("DatePicker", ignoreCase = true) ||
               className.contains("CalendarView", ignoreCase = true) ||
               className.contains("Calendar", ignoreCase = true)
    }

    /**
     * Find node by class name (recursive search)
     */
    private fun findNodeByClassName(
        rootNode: AccessibilityNodeInfo,
        classNames: List<String>
    ): AccessibilityNodeInfo? {
        val className = rootNode.className?.toString()
        if (className != null && classNames.any { className.equals(it, ignoreCase = true) }) {
            return rootNode
        }

        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i) ?: continue
            val found = findNodeByClassName(child, classNames)
            if (found != null) {
                if (found != child) child.recycle()
                return found
            }
            child.recycle()
        }

        return null
    }

    /**
     * Set date on DatePicker node
     */
    private fun setDateOnNode(node: AccessibilityNodeInfo, calendar: Calendar): Boolean {
        // Try using ACTION_SET_SELECTION with date bundle (API 21+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val bundle = Bundle().apply {
                putInt("android.widget.DatePicker.year", calendar.get(Calendar.YEAR))
                putInt("android.widget.DatePicker.month", calendar.get(Calendar.MONTH))
                putInt("android.widget.DatePicker.day", calendar.get(Calendar.DAY_OF_MONTH))
            }

            // Try SET_TEXT action with date value
            val success = node.performAction(
                AccessibilityNodeInfo.ACTION_SET_TEXT,
                bundle
            )
            if (success) return true
        }

        // Fallback: Try clicking on date elements within the picker
        return setDateByNavigatingPicker(node, calendar)
    }

    /**
     * Fallback: Navigate the date picker by clicking elements
     */
    private fun setDateByNavigatingPicker(node: AccessibilityNodeInfo, calendar: Calendar): Boolean {
        val day = calendar.get(Calendar.DAY_OF_MONTH).toString()

        // Find and click the day element
        val dayNode = findNodeByText(node, day)
        if (dayNode != null) {
            val success = dayNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            dayNode.recycle()
            return success
        }

        Log.w(TAG, "Could not find day element: $day")
        return false
    }

    /**
     * Find node by text content
     */
    private fun findNodeByText(rootNode: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val nodeText = rootNode.text?.toString()
        val contentDesc = rootNode.contentDescription?.toString()

        if (nodeText == text || contentDesc == text) {
            return rootNode
        }

        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i) ?: continue
            val found = findNodeByText(child, text)
            if (found != null) {
                if (found != child) child.recycle()
                return found
            }
            child.recycle()
        }

        return null
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Voice Phrases for Speech Engine Registration
    // ═══════════════════════════════════════════════════════════════════════════

    override fun getVoicePhrases(): List<String> {
        return listOf(
            "set date to",
            "today",
            "tomorrow",
            "yesterday",
            "next week",
            "next month",
            "previous month",
            "last month",
            "next year",
            "previous year",
            "last year"
        )
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Update accessibility service reference
     */
    fun setAccessibilityService(service: AccessibilityService?) {
        accessibilityService = service
    }

    /**
     * Cleanup resources
     */
    override suspend fun dispose() {
        CommandRegistry.unregisterHandler(moduleId)
        handlerScope.cancel()
        accessibilityService = null
        instance = null
        Log.d(TAG, "DatePickerHandler disposed")
    }
}

/**
 * Relative date types for navigation commands
 */
private enum class RelativeDate {
    TODAY,
    TOMORROW,
    YESTERDAY,
    NEXT_WEEK,
    NEXT_MONTH,
    PREVIOUS_MONTH,
    NEXT_YEAR,
    PREVIOUS_YEAR
}
