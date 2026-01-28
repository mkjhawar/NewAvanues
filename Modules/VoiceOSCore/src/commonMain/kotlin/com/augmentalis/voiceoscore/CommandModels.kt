/**
 * VoiceOS Command Models
 *
 * Re-exports from CommandManager and provides VoiceOSCore-specific extensions
 * with richer type support (Map<String, Any> vs Map<String, String>).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Migrated: 2026-01-28
 */
package com.augmentalis.voiceoscore

// Re-export types from CommandManager that don't need extension
// These are directly usable by VoiceOSCore
typealias VOSCommand = com.augmentalis.commandmanager.VOSCommand
typealias CommandSource = com.augmentalis.commandmanager.CommandSource
typealias ErrorCode = com.augmentalis.commandmanager.ErrorCode
typealias ParameterType = com.augmentalis.commandmanager.ParameterType
typealias CommandCategory = com.augmentalis.commandmanager.CommandCategory
typealias EventType = com.augmentalis.commandmanager.EventType
typealias CommandInfo = com.augmentalis.commandmanager.CommandInfo
typealias CommandStats = com.augmentalis.commandmanager.CommandStats
typealias AccessibilityActions = com.augmentalis.commandmanager.AccessibilityActions

// VoiceOSCore-specific versions with Map<String, Any> support
// These extend CommandManager types for richer runtime capabilities

/**
 * VoiceOSCore Command with Any-typed parameters for runtime flexibility.
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
    // Re-use constants from CommandManager
    object AppCategories {
        const val PRODUCTIVITY = com.augmentalis.commandmanager.CommandContext.AppCategories.PRODUCTIVITY
        const val SOCIAL = com.augmentalis.commandmanager.CommandContext.AppCategories.SOCIAL
        const val MEDIA = com.augmentalis.commandmanager.CommandContext.AppCategories.MEDIA
        const val COMMUNICATION = com.augmentalis.commandmanager.CommandContext.AppCategories.COMMUNICATION
        const val BROWSER = com.augmentalis.commandmanager.CommandContext.AppCategories.BROWSER
        const val SHOPPING = com.augmentalis.commandmanager.CommandContext.AppCategories.SHOPPING
        const val NAVIGATION = com.augmentalis.commandmanager.CommandContext.AppCategories.NAVIGATION
        const val GAMES = com.augmentalis.commandmanager.CommandContext.AppCategories.GAMES
        const val SYSTEM = com.augmentalis.commandmanager.CommandContext.AppCategories.SYSTEM
        const val UNKNOWN = com.augmentalis.commandmanager.CommandContext.AppCategories.UNKNOWN
    }

    object LocationTypes {
        const val HOME = com.augmentalis.commandmanager.CommandContext.LocationTypes.HOME
        const val WORK = com.augmentalis.commandmanager.CommandContext.LocationTypes.WORK
        const val PUBLIC = com.augmentalis.commandmanager.CommandContext.LocationTypes.PUBLIC
        const val VEHICLE = com.augmentalis.commandmanager.CommandContext.LocationTypes.VEHICLE
        const val OUTDOOR = com.augmentalis.commandmanager.CommandContext.LocationTypes.OUTDOOR
        const val UNKNOWN = com.augmentalis.commandmanager.CommandContext.LocationTypes.UNKNOWN
    }

    object ActivityTypes {
        const val WALKING = com.augmentalis.commandmanager.CommandContext.ActivityTypes.WALKING
        const val RUNNING = com.augmentalis.commandmanager.CommandContext.ActivityTypes.RUNNING
        const val DRIVING = com.augmentalis.commandmanager.CommandContext.ActivityTypes.DRIVING
        const val STATIONARY = com.augmentalis.commandmanager.CommandContext.ActivityTypes.STATIONARY
        const val CYCLING = com.augmentalis.commandmanager.CommandContext.ActivityTypes.CYCLING
        const val UNKNOWN = com.augmentalis.commandmanager.CommandContext.ActivityTypes.UNKNOWN
    }

    object TimeOfDay {
        const val EARLY_MORNING = com.augmentalis.commandmanager.CommandContext.TimeOfDay.EARLY_MORNING
        const val MORNING = com.augmentalis.commandmanager.CommandContext.TimeOfDay.MORNING
        const val AFTERNOON = com.augmentalis.commandmanager.CommandContext.TimeOfDay.AFTERNOON
        const val EVENING = com.augmentalis.commandmanager.CommandContext.TimeOfDay.EVENING
        const val NIGHT = com.augmentalis.commandmanager.CommandContext.TimeOfDay.NIGHT
        const val LATE_NIGHT = com.augmentalis.commandmanager.CommandContext.TimeOfDay.LATE_NIGHT

        fun fromHour(hour: Int): String = com.augmentalis.commandmanager.CommandContext.TimeOfDay.fromHour(hour)
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
