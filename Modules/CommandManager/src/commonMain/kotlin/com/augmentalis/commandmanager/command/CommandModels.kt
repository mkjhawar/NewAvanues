/**
 * VoiceOS Command Models
 *
 * Core command types for the CommandManager module.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Migrated: 2026-01-28
 */
package com.augmentalis.commandmanager

/**
 * Source of a voice command.
 */
enum class CommandSource {
    VOICE,
    TEXT,
    GESTURE,
    BUTTON,
    KEYBOARD,
    SYSTEM,
    EXTERNAL,
    UNKNOWN
}

/**
 * Types of command parameters.
 */
enum class ParameterType {
    STRING,
    NUMBER,
    BOOLEAN,
    DATE,
    TIME,
    DURATION,
    CONTACT,
    LOCATION,
    APP_NAME,
    FILE_PATH,
    URL,
    CUSTOM
}

/**
 * Types of command lifecycle events.
 */
enum class EventType {
    COMMAND_RECEIVED,
    COMMAND_RECOGNIZED,
    COMMAND_MATCHED,
    COMMAND_EXECUTING,
    COMMAND_COMPLETED,
    COMMAND_FAILED,
    COMMAND_CANCELLED,
    FEEDBACK_PROVIDED
}

/**
 * Error codes for command execution failures.
 */
enum class ErrorCode {
    NONE,
    UNKNOWN,
    INVALID_COMMAND,
    COMMAND_NOT_FOUND,
    PERMISSION_DENIED,
    NETWORK_ERROR,
    TIMEOUT,
    PARSE_ERROR,
    EXECUTION_FAILED,
    CANCELLED,
    NOT_SUPPORTED,
    RESOURCE_NOT_FOUND,
    INVALID_PARAMETER,
    SERVICE_UNAVAILABLE
}

/**
 * Command with Any-typed parameters for runtime flexibility.
 */
data class Command(
    val id: String,
    val text: String,
    val source: CommandSource,
    val context: CommandContext? = null,
    val parameters: Map<String, Any> = emptyMap(),
    val timestamp: Long,
    val confidence: Float = 1.0f
)

/**
 * VoiceOSCore CommandContext with Any-typed maps for runtime flexibility.
 */
data class CommandContext(
    val packageName: String? = null,
    val activityName: String? = null,
    val appCategory: String? = null,
    val viewId: String? = null,
    val screenContent: String? = null,
    val screenElements: List<String> = emptyList(),
    val hasEditableFields: Boolean = false,
    val hasScrollableContent: Boolean = false,
    val hasClickableElements: Boolean = false,
    val userLocation: String? = null,
    val locationConfidence: Float = 1.0f,
    val activityType: String? = null,
    val activityConfidence: Float = 1.0f,
    val timeOfDay: String? = null,
    val hour: Int? = null,
    val dayOfWeek: Int? = null,
    val deviceState: Map<String, Any> = emptyMap(),
    val focusedElement: String? = null,
    val customData: Map<String, Any> = emptyMap()
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
 * VoiceOSCore CommandResult with full Command object.
 */
data class CommandResult(
    val success: Boolean,
    val command: Command,
    val response: String? = null,
    val data: Any? = null,
    val error: CommandError? = null,
    val executionTime: Long = 0
)

/**
 * Error information for a failed command.
 */
data class CommandError(
    val code: ErrorCode,
    val message: String,
    val details: String? = null
)

/**
 * VoiceOSCore CommandDefinition with Any-typed default values.
 */
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
 * VoiceOSCore CommandParameter with Any-typed default value.
 */
data class CommandParameter(
    val name: String,
    val type: ParameterType,
    val required: Boolean = false,
    val defaultValue: Any? = null,
    val description: String? = null
)

/**
 * Entry in command execution history.
 */
data class CommandHistoryEntry(
    val command: Command,
    val result: CommandResult,
    val timestamp: Long
)

/**
 * Event emitted during command lifecycle.
 */
data class CommandEvent(
    val type: EventType,
    val command: Command? = null,
    val result: CommandResult? = null,
    val message: String? = null,
    val timestamp: Long
)

/**
 * Handler function for processing commands.
 */
typealias CommandHandler = suspend (Command) -> CommandResult
