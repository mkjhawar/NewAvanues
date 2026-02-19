/**
 * TimePickerActions.kt - TimePicker voice command actions
 * Path: modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/actions/TimePickerActions.kt
 *
 * Created: 2026-01-27 00:00 PST
 * Module: CommandManager
 *
 * Purpose: TimePicker manipulation commands via accessibility service
 * Features: Time setting, hour/minute adjustment, AM/PM control
 *
 * Works in conjunction with TimePickerHandler for command routing.
 */

package com.augmentalis.voiceoscore.commandmanager.actions

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.*
import java.util.Calendar
import java.util.Locale

/**
 * TimePicker action implementations using BaseAction pattern.
 * Provides individual action classes for each time picker operation.
 *
 * Usage with CommandManager's action execution system.
 */
object TimePickerActions {

    private const val TAG = "TimePickerActions"

    /**
     * Set Time Action - Sets a specific time on the focused TimePicker
     *
     * Parameters:
     * - "hour" (Int): Hour value (1-12 for 12-hour, 0-23 for 24-hour)
     * - "minute" (Int): Minute value (0-59)
     * - "isAM" (Boolean): AM/PM indicator for 12-hour format
     */
    class SetTimeAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            val hour = getNumberParameter(command, "hour")?.toInt()
            val minute = getNumberParameter(command, "minute")?.toInt()
            val isAM = getBooleanParameter(command, "isAM")

            if (hour == null || minute == null) {
                return createErrorResult(
                    command,
                    ErrorCode.INVALID_PARAMETERS,
                    "Hour and minute parameters required"
                )
            }

            if (hour !in 0..23 || minute !in 0..59) {
                return createErrorResult(
                    command,
                    ErrorCode.INVALID_PARAMETERS,
                    "Invalid time values: hour=$hour, minute=$minute"
                )
            }

            return try {
                val success = setTimePicker(accessibilityService, hour, minute, isAM ?: true)
                if (success) {
                    val amPmStr = if (isAM == null) "" else if (isAM) " AM" else " PM"
                    createSuccessResult(
                        command,
                        "Time set to $hour:${String.format(Locale.US, "%02d", minute)}$amPmStr"
                    )
                } else {
                    createErrorResult(
                        command,
                        ErrorCode.EXECUTION_FAILED,
                        "Failed to set time - no TimePicker found"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "SetTimeAction failed", e)
                createErrorResult(
                    command,
                    ErrorCode.EXECUTION_FAILED,
                    "Failed to set time: ${e.message}"
                )
            }
        }
    }

    /**
     * Set Hour Action - Sets only the hour value
     *
     * Parameters:
     * - "hour" (Int): Hour value to set
     */
    class SetHourAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            val hour = getNumberParameter(command, "hour")?.toInt()

            if (hour == null || hour !in 1..12) {
                return createErrorResult(
                    command,
                    ErrorCode.INVALID_PARAMETERS,
                    "Hour must be between 1 and 12"
                )
            }

            return try {
                val success = setHourValue(accessibilityService, hour)
                if (success) {
                    createSuccessResult(command, "Hour set to $hour")
                } else {
                    createErrorResult(
                        command,
                        ErrorCode.EXECUTION_FAILED,
                        "Failed to set hour - no TimePicker found"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "SetHourAction failed", e)
                createErrorResult(
                    command,
                    ErrorCode.EXECUTION_FAILED,
                    "Failed to set hour: ${e.message}"
                )
            }
        }
    }

    /**
     * Set Minute Action - Sets only the minute value
     *
     * Parameters:
     * - "minute" (Int): Minute value to set
     */
    class SetMinuteAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            val minute = getNumberParameter(command, "minute")?.toInt()

            if (minute == null || minute !in 0..59) {
                return createErrorResult(
                    command,
                    ErrorCode.INVALID_PARAMETERS,
                    "Minute must be between 0 and 59"
                )
            }

            return try {
                val success = setMinuteValue(accessibilityService, minute)
                if (success) {
                    createSuccessResult(
                        command,
                        "Minute set to ${String.format(Locale.US, "%02d", minute)}"
                    )
                } else {
                    createErrorResult(
                        command,
                        ErrorCode.EXECUTION_FAILED,
                        "Failed to set minute - no TimePicker found"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "SetMinuteAction failed", e)
                createErrorResult(
                    command,
                    ErrorCode.EXECUTION_FAILED,
                    "Failed to set minute: ${e.message}"
                )
            }
        }
    }

    /**
     * Increase Hour Action - Increments hour by 1
     */
    class IncreaseHourAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            return try {
                val success = incrementHour(accessibilityService)
                if (success) {
                    createSuccessResult(command, "Hour increased")
                } else {
                    createErrorResult(
                        command,
                        ErrorCode.EXECUTION_FAILED,
                        "Failed to increase hour"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "IncreaseHourAction failed", e)
                createErrorResult(
                    command,
                    ErrorCode.EXECUTION_FAILED,
                    "Failed to increase hour: ${e.message}"
                )
            }
        }
    }

    /**
     * Decrease Hour Action - Decrements hour by 1
     */
    class DecreaseHourAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            return try {
                val success = decrementHour(accessibilityService)
                if (success) {
                    createSuccessResult(command, "Hour decreased")
                } else {
                    createErrorResult(
                        command,
                        ErrorCode.EXECUTION_FAILED,
                        "Failed to decrease hour"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "DecreaseHourAction failed", e)
                createErrorResult(
                    command,
                    ErrorCode.EXECUTION_FAILED,
                    "Failed to decrease hour: ${e.message}"
                )
            }
        }
    }

    /**
     * Increase Minute Action - Increments minute by 1
     */
    class IncreaseMinuteAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            return try {
                val success = incrementMinute(accessibilityService)
                if (success) {
                    createSuccessResult(command, "Minute increased")
                } else {
                    createErrorResult(
                        command,
                        ErrorCode.EXECUTION_FAILED,
                        "Failed to increase minute"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "IncreaseMinuteAction failed", e)
                createErrorResult(
                    command,
                    ErrorCode.EXECUTION_FAILED,
                    "Failed to increase minute: ${e.message}"
                )
            }
        }
    }

    /**
     * Decrease Minute Action - Decrements minute by 1
     */
    class DecreaseMinuteAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            return try {
                val success = decrementMinute(accessibilityService)
                if (success) {
                    createSuccessResult(command, "Minute decreased")
                } else {
                    createErrorResult(
                        command,
                        ErrorCode.EXECUTION_FAILED,
                        "Failed to decrease minute"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "DecreaseMinuteAction failed", e)
                createErrorResult(
                    command,
                    ErrorCode.EXECUTION_FAILED,
                    "Failed to decrease minute: ${e.message}"
                )
            }
        }
    }

    /**
     * Set AM Action - Switches to AM
     */
    class SetAMAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            return try {
                val success = setAMPM(accessibilityService, isAM = true)
                if (success) {
                    createSuccessResult(command, "Set to AM")
                } else {
                    createErrorResult(
                        command,
                        ErrorCode.EXECUTION_FAILED,
                        "Failed to set AM"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "SetAMAction failed", e)
                createErrorResult(
                    command,
                    ErrorCode.EXECUTION_FAILED,
                    "Failed to set AM: ${e.message}"
                )
            }
        }
    }

    /**
     * Set PM Action - Switches to PM
     */
    class SetPMAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            return try {
                val success = setAMPM(accessibilityService, isAM = false)
                if (success) {
                    createSuccessResult(command, "Set to PM")
                } else {
                    createErrorResult(
                        command,
                        ErrorCode.EXECUTION_FAILED,
                        "Failed to set PM"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "SetPMAction failed", e)
                createErrorResult(
                    command,
                    ErrorCode.EXECUTION_FAILED,
                    "Failed to set PM: ${e.message}"
                )
            }
        }
    }

    /**
     * Toggle AM/PM Action - Toggles between AM and PM
     */
    class ToggleAMPMAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            return try {
                val success = toggleAMPM(accessibilityService)
                if (success) {
                    createSuccessResult(command, "AM/PM toggled")
                } else {
                    createErrorResult(
                        command,
                        ErrorCode.EXECUTION_FAILED,
                        "Failed to toggle AM/PM"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "ToggleAMPMAction failed", e)
                createErrorResult(
                    command,
                    ErrorCode.EXECUTION_FAILED,
                    "Failed to toggle AM/PM: ${e.message}"
                )
            }
        }
    }

    /**
     * Set Current Time Action - Sets to system current time
     */
    class SetCurrentTimeAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            return try {
                val calendar = Calendar.getInstance()
                var hour = calendar.get(Calendar.HOUR)
                if (hour == 0) hour = 12
                val minute = calendar.get(Calendar.MINUTE)
                val isAM = calendar.get(Calendar.AM_PM) == Calendar.AM

                val success = setTimePicker(accessibilityService, hour, minute, isAM)
                if (success) {
                    createSuccessResult(
                        command,
                        "Time set to current: $hour:${String.format(Locale.US, "%02d", minute)} ${if (isAM) "AM" else "PM"}"
                    )
                } else {
                    createErrorResult(
                        command,
                        ErrorCode.EXECUTION_FAILED,
                        "Failed to set current time"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "SetCurrentTimeAction failed", e)
                createErrorResult(
                    command,
                    ErrorCode.EXECUTION_FAILED,
                    "Failed to set current time: ${e.message}"
                )
            }
        }
    }

    /**
     * Set Noon Action - Sets to 12:00 PM
     */
    class SetNoonAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            return try {
                val success = setTimePicker(accessibilityService, 12, 0, false)
                if (success) {
                    createSuccessResult(command, "Time set to noon (12:00 PM)")
                } else {
                    createErrorResult(
                        command,
                        ErrorCode.EXECUTION_FAILED,
                        "Failed to set noon"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "SetNoonAction failed", e)
                createErrorResult(
                    command,
                    ErrorCode.EXECUTION_FAILED,
                    "Failed to set noon: ${e.message}"
                )
            }
        }
    }

    /**
     * Set Midnight Action - Sets to 12:00 AM
     */
    class SetMidnightAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            return try {
                val success = setTimePicker(accessibilityService, 12, 0, true)
                if (success) {
                    createSuccessResult(command, "Time set to midnight (12:00 AM)")
                } else {
                    createErrorResult(
                        command,
                        ErrorCode.EXECUTION_FAILED,
                        "Failed to set midnight"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "SetMidnightAction failed", e)
                createErrorResult(
                    command,
                    ErrorCode.EXECUTION_FAILED,
                    "Failed to set midnight: ${e.message}"
                )
            }
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Set TimePicker to specified time via accessibility
     */
    private fun setTimePicker(
        service: AccessibilityService?,
        hour: Int,
        minute: Int,
        isAM: Boolean
    ): Boolean {
        val rootNode = service?.rootInActiveWindow ?: return false

        // Strategy 1: Find TimePicker widget
        val timePickers = findNodesByClassName(rootNode, "android.widget.TimePicker")
        if (timePickers.isNotEmpty()) {
            return updateTimePickerWidget(timePickers.first(), hour, minute, isAM)
        }

        // Strategy 2: Find individual NumberPickers
        val numberPickers = findNodesByClassName(rootNode, "android.widget.NumberPicker")
        if (numberPickers.size >= 2) {
            return updateNumberPickers(numberPickers, hour, minute, isAM)
        }

        // Strategy 3: Find Material TimePicker
        val materialPickers = findNodesByClassName(
            rootNode,
            "com.google.android.material.timepicker.TimePickerView"
        )
        if (materialPickers.isNotEmpty()) {
            return updateMaterialTimePicker(materialPickers.first(), hour, minute, isAM)
        }

        return false
    }

    /**
     * Update standard Android TimePicker widget
     */
    private fun updateTimePickerWidget(
        picker: AccessibilityNodeInfo,
        hour: Int,
        minute: Int,
        isAM: Boolean
    ): Boolean {
        val numberPickers = findNodesByClassName(picker, "android.widget.NumberPicker")

        if (numberPickers.size >= 2) {
            setPickerValue(numberPickers[0], hour)
            setPickerValue(numberPickers[1], minute)

            if (numberPickers.size >= 3) {
                setPickerValue(numberPickers[2], if (isAM) 0 else 1)
            }
            return true
        }
        return false
    }

    /**
     * Update separate NumberPicker widgets
     */
    private fun updateNumberPickers(
        pickers: List<AccessibilityNodeInfo>,
        hour: Int,
        minute: Int,
        isAM: Boolean
    ): Boolean {
        setPickerValue(pickers[0], hour)
        setPickerValue(pickers[1], minute)

        if (pickers.size >= 3) {
            setPickerValue(pickers[2], if (isAM) 0 else 1)
        }
        return true
    }

    /**
     * Update Material Design TimePicker
     */
    private fun updateMaterialTimePicker(
        picker: AccessibilityNodeInfo,
        hour: Int,
        minute: Int,
        isAM: Boolean
    ): Boolean {
        // Material TimePicker uses text views and buttons
        val textNodes = findNodesByClassName(picker, "android.widget.TextView")
        val buttonNodes = findNodesByClassName(picker, "android.widget.Button")

        for (node in textNodes) {
            val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
            when {
                contentDesc.contains("hour") -> setNodeText(node, hour.toString())
                contentDesc.contains("minute") -> setNodeText(
                    node,
                    String.format(Locale.US, "%02d", minute)
                )
            }
        }

        // Handle AM/PM buttons
        for (node in buttonNodes) {
            val text = node.text?.toString()?.lowercase() ?: continue
            val shouldClick = (text == "am" && !isAM) || (text == "pm" && isAM)
            if (shouldClick) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }

        return true
    }

    /**
     * Set hour value only
     */
    private fun setHourValue(service: AccessibilityService?, hour: Int): Boolean {
        val rootNode = service?.rootInActiveWindow ?: return false

        val numberPickers = findNodesByClassName(rootNode, "android.widget.NumberPicker")
        if (numberPickers.isNotEmpty()) {
            return setPickerValue(numberPickers[0], hour)
        }
        return false
    }

    /**
     * Set minute value only
     */
    private fun setMinuteValue(service: AccessibilityService?, minute: Int): Boolean {
        val rootNode = service?.rootInActiveWindow ?: return false

        val numberPickers = findNodesByClassName(rootNode, "android.widget.NumberPicker")
        if (numberPickers.size >= 2) {
            return setPickerValue(numberPickers[1], minute)
        }
        return false
    }

    /**
     * Increment hour via scroll action
     */
    private fun incrementHour(service: AccessibilityService?): Boolean {
        val rootNode = service?.rootInActiveWindow ?: return false

        val numberPickers = findNodesByClassName(rootNode, "android.widget.NumberPicker")
        if (numberPickers.isNotEmpty()) {
            return numberPickers[0].performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
        }
        return false
    }

    /**
     * Decrement hour via scroll action
     */
    private fun decrementHour(service: AccessibilityService?): Boolean {
        val rootNode = service?.rootInActiveWindow ?: return false

        val numberPickers = findNodesByClassName(rootNode, "android.widget.NumberPicker")
        if (numberPickers.isNotEmpty()) {
            return numberPickers[0].performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
        }
        return false
    }

    /**
     * Increment minute via scroll action
     */
    private fun incrementMinute(service: AccessibilityService?): Boolean {
        val rootNode = service?.rootInActiveWindow ?: return false

        val numberPickers = findNodesByClassName(rootNode, "android.widget.NumberPicker")
        if (numberPickers.size >= 2) {
            return numberPickers[1].performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
        }
        return false
    }

    /**
     * Decrement minute via scroll action
     */
    private fun decrementMinute(service: AccessibilityService?): Boolean {
        val rootNode = service?.rootInActiveWindow ?: return false

        val numberPickers = findNodesByClassName(rootNode, "android.widget.NumberPicker")
        if (numberPickers.size >= 2) {
            return numberPickers[1].performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
        }
        return false
    }

    /**
     * Set AM/PM value
     */
    private fun setAMPM(service: AccessibilityService?, isAM: Boolean): Boolean {
        val rootNode = service?.rootInActiveWindow ?: return false

        // Try NumberPicker for AM/PM
        val numberPickers = findNodesByClassName(rootNode, "android.widget.NumberPicker")
        if (numberPickers.size >= 3) {
            return setPickerValue(numberPickers[2], if (isAM) 0 else 1)
        }

        // Try buttons
        val buttons = findNodesByClassName(rootNode, "android.widget.Button")
        for (button in buttons) {
            val text = button.text?.toString()?.lowercase() ?: continue
            if ((text == "am" && isAM) || (text == "pm" && !isAM)) {
                // Already in desired state
                return true
            }
            if ((text == "am" && !isAM) || (text == "pm" && isAM)) {
                // Need to click to toggle
                return button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }

        return false
    }

    /**
     * Toggle AM/PM value
     */
    private fun toggleAMPM(service: AccessibilityService?): Boolean {
        val rootNode = service?.rootInActiveWindow ?: return false

        // Try NumberPicker for AM/PM
        val numberPickers = findNodesByClassName(rootNode, "android.widget.NumberPicker")
        if (numberPickers.size >= 3) {
            // Toggle by scrolling
            return numberPickers[2].performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
        }

        // Try buttons
        val buttons = findNodesByClassName(rootNode, "android.widget.Button")
        for (button in buttons) {
            val text = button.text?.toString()?.lowercase() ?: continue
            if (text == "am" || text == "pm") {
                return button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }

        return false
    }

    /**
     * Set value on a NumberPicker
     */
    private fun setPickerValue(picker: AccessibilityNodeInfo, value: Int): Boolean {
        // Try direct text setting first
        val bundle = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                value.toString()
            )
        }

        if (picker.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)) {
            return true
        }

        // Fallback: look for editable child
        for (i in 0 until picker.childCount) {
            val child = picker.getChild(i)
            if (child?.isEditable == true) {
                val success = setNodeText(child, value.toString())
                if (success) return true
            }
        }

        return false
    }

    /**
     * Set text on a node
     */
    private fun setNodeText(node: AccessibilityNodeInfo, text: String): Boolean {
        val bundle = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text
            )
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
    }

    /**
     * Find nodes by class name recursively
     */
    private fun findNodesByClassName(
        rootNode: AccessibilityNodeInfo,
        className: String
    ): List<AccessibilityNodeInfo> {
        val results = mutableListOf<AccessibilityNodeInfo>()
        findNodesByClassNameRecursive(rootNode, className, results)
        return results
    }

    private fun findNodesByClassNameRecursive(
        node: AccessibilityNodeInfo,
        className: String,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        if (node.className?.toString() == className) {
            results.add(node)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findNodesByClassNameRecursive(child, className, results)
        }
    }
}
