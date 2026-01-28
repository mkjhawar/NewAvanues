/**
 * CommandManager - Complete Command Models
 *
 * Full-featured data models for voice command processing and execution.
 * This is the source of truth - all apps should import from here.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-28
 */
package com.augmentalis.commandmanager.models

import kotlinx.serialization.Serializable

/**
 * Voice OS command structure.
 * Used by command parsers to represent commands from .vos and JSON files.
 */
@Serializable
data class VOSCommand(
    val action: String,
    val cmd: String,
    val syn: List<String> = emptyList()
)

/**
 * Represents a command to be executed by the voice system.
 */
@Serializable
data class Command(
    val id: String,
    val text: String,
    val source: CommandSource,
    val context: CommandContext? = null,
    val parameters: Map<String, String> = emptyMap(),
    val timestamp: Long,
    val confidence: Float = 1.0f
)

/**
 * Source of a command.
 */
@Serializable
enum class CommandSource {
    VOICE, GESTURE, TEXT, APP, SYSTEM, EXTERNAL
}

/**
 * Context information about the command execution environment.
 * Full-featured context with location, activity, time, and device awareness.
 */
@Serializable
data class CommandContext(
    // App context
    val packageName: String? = null,
    val activityName: String? = null,
    val appCategory: String? = null,

    // Screen context
    val viewId: String? = null,
    val screenContent: String? = null,
    val screenElements: List<String> = emptyList(),
    val hasEditableFields: Boolean = false,
    val hasScrollableContent: Boolean = false,
    val hasClickableElements: Boolean = false,

    // Location context
    val userLocation: String? = null,
    val locationConfidence: Float = 1.0f,

    // Activity context
    val activityType: String? = null,
    val activityConfidence: Float = 1.0f,

    // Time context
    val timeOfDay: String? = null,
    val hour: Int? = null,
    val dayOfWeek: Int? = null,

    // Device state
    val deviceState: Map<String, String> = emptyMap(),

    // Focused element
    val focusedElement: String? = null,

    // Custom data
    val customData: Map<String, String> = emptyMap()
) {
    object AppCategories {
        const val PRODUCTIVITY = "productivity"
        const val SOCIAL = "social"
        const val MEDIA = "media"
        const val COMMUNICATION = "communication"
        const val BROWSER = "browser"
        const val SHOPPING = "shopping"
        const val NAVIGATION = "navigation"
        const val GAMES = "games"
        const val SYSTEM = "system"
        const val UNKNOWN = "unknown"
    }

    object LocationTypes {
        const val HOME = "home"
        const val WORK = "work"
        const val PUBLIC = "public"
        const val VEHICLE = "vehicle"
        const val OUTDOOR = "outdoor"
        const val UNKNOWN = "unknown"
    }

    object ActivityTypes {
        const val WALKING = "walking"
        const val RUNNING = "running"
        const val DRIVING = "driving"
        const val STATIONARY = "stationary"
        const val CYCLING = "cycling"
        const val UNKNOWN = "unknown"
    }

    object TimeOfDay {
        const val EARLY_MORNING = "early_morning"
        const val MORNING = "morning"
        const val AFTERNOON = "afternoon"
        const val EVENING = "evening"
        const val NIGHT = "night"
        const val LATE_NIGHT = "late_night"

        fun fromHour(hour: Int): String = when (hour) {
            in 5..7 -> EARLY_MORNING
            in 8..11 -> MORNING
            in 12..16 -> AFTERNOON
            in 17..20 -> EVENING
            in 21..23 -> NIGHT
            else -> LATE_NIGHT
        }
    }

    fun isWeekday(): Boolean = dayOfWeek?.let { it in 2..6 } ?: false
    fun isWeekend(): Boolean = dayOfWeek?.let { it == 1 || it == 7 } ?: false
    fun isLocationConfident(threshold: Float = 0.7f): Boolean = locationConfidence >= threshold
    fun isActivityConfident(threshold: Float = 0.7f): Boolean = activityConfidence >= threshold
}

/**
 * Result of a command execution.
 */
@Serializable
data class CommandResult(
    val success: Boolean,
    val commandId: String,
    val response: String? = null,
    val errorCode: ErrorCode? = null,
    val errorMessage: String? = null,
    val errorDetails: String? = null,
    val executionTimeMs: Long = 0
)

/**
 * Standardized error codes for command execution failures.
 */
@Serializable
enum class ErrorCode {
    MODULE_NOT_AVAILABLE,
    COMMAND_NOT_FOUND,
    INVALID_PARAMETERS,
    PERMISSION_DENIED,
    EXECUTION_FAILED,
    TIMEOUT,
    NETWORK_ERROR,
    UNKNOWN,
    UNKNOWN_COMMAND,
    MISSING_CONTEXT,
    CANCELLED,
    NO_ACCESSIBILITY_SERVICE,
    ACTION_FAILED
}

/**
 * Definition of a registered command.
 */
@Serializable
data class CommandDefinition(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val patterns: List<String>,
    val parameters: List<CommandParameter> = emptyList(),
    val requiredPermissions: List<String> = emptyList(),
    val supportedLanguages: List<String> = listOf("en"),
    val requiredContext: Set<String> = emptySet()
)

/**
 * Definition of a command parameter.
 */
@Serializable
data class CommandParameter(
    val name: String,
    val type: ParameterType,
    val required: Boolean = false,
    val defaultValue: String? = null,
    val description: String? = null
)

/**
 * Supported parameter types.
 */
@Serializable
enum class ParameterType {
    STRING, NUMBER, BOOLEAN, LIST, MAP, CUSTOM
}

/**
 * Command categories for organization.
 */
@Serializable
enum class CommandCategory {
    NAVIGATION,
    SYSTEM,
    APP_CONTROL,
    TEXT_EDITING,
    UI_INTERACTION,
    MEDIA,
    COMMUNICATION,
    CUSTOM
}

/**
 * Entry in command execution history.
 */
@Serializable
data class CommandHistoryEntry(
    val commandId: String,
    val commandText: String,
    val success: Boolean,
    val timestamp: Long
)

/**
 * Event emitted during command lifecycle.
 */
@Serializable
data class CommandEvent(
    val type: EventType,
    val commandId: String? = null,
    val commandText: String? = null,
    val message: String? = null,
    val timestamp: Long
)

/**
 * Types of command lifecycle events.
 */
@Serializable
enum class EventType {
    COMMAND_RECEIVED,
    COMMAND_EXECUTING,
    COMMAND_COMPLETED,
    COMMAND_FAILED,
    COMMAND_REGISTERED,
    COMMAND_UNREGISTERED
}

/**
 * Basic information about a command.
 */
@Serializable
data class CommandInfo(
    val id: String,
    val name: String,
    val category: String,
    val isCustom: Boolean = false,
    val usageCount: Int = 0
)

/**
 * Statistics about command usage.
 */
@Serializable
data class CommandStats(
    val totalCommands: Int,
    val successfulCommands: Int,
    val failedCommands: Int,
    val averageExecutionTime: Long,
    val topCommands: List<String>
)

/**
 * Accessibility action constants.
 */
object AccessibilityActions {
    const val ACTION_SELECT_ALL = 0x10000
    const val ACTION_BACKUP_AND_RESET_SETTINGS = 0x20000
}
