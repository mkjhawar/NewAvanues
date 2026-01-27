/**
 * VoiceOS Command Models
 *
 * Pure Kotlin data models for command processing and execution.
 * Extracted from CommandManager for cross-platform reuse.
 *
 * Extracted: 2025-11-17
 * Original: com.augmentalis.commandmanager.models.CommandModels
 *           com.augmentalis.commandmanager.models.VOSCommand
 */
package com.augmentalis.voiceoscore

/**
 * Voice OS command structure
 *
 * Used by command parsers to represent commands from .vos and JSON files.
 *
 * @property action Unique action ID (e.g., "NAVIGATE_FORWARD", "VOLUME_UP")
 * @property cmd Primary command text (e.g., "forward", "increase volume")
 * @property syn List of synonyms (e.g., ["next", "advance", "go forward"])
 */
data class VOSCommand(
    val action: String,
    val cmd: String,
    val syn: List<String>
)

/**
 * Represents a command to be executed by the VoiceOS system.
 *
 * @property id Unique identifier for the command
 * @property text Human-readable command text
 * @property source Where the command originated from
 * @property context Additional context about the command environment
 * @property parameters Command-specific parameters
 * @property timestamp When the command was created (milliseconds since epoch)
 * @property confidence Confidence score for the command recognition (0.0-1.0)
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
 * Source of a command.
 */
enum class CommandSource {
    /** Voice recognition input */
    VOICE,

    /** Gesture-based input */
    GESTURE,

    /** Text-based input */
    TEXT,

    /** App-initiated command */
    APP,

    /** System-generated command */
    SYSTEM,

    /** External API or plugin */
    EXTERNAL
}

/**
 * Context information about the command execution environment.
 *
 * Unified context model that combines fields from both the legacy sealed class
 * (com.augmentalis.commandmanager.context.CommandContext) and the KMP data class.
 *
 * @property packageName Package name of the active application
 * @property activityName Activity name of the active screen
 * @property appCategory Category of the current app (productivity, social, etc.)
 * @property viewId View identifier of the focused element
 * @property screenContent Text content visible on screen
 * @property screenElements List of key UI elements on screen
 * @property hasEditableFields Whether the screen has editable text fields
 * @property hasScrollableContent Whether the screen has scrollable content
 * @property hasClickableElements Whether the screen has clickable elements
 * @property userLocation User's current location type (home, work, etc.)
 * @property locationConfidence Confidence in location detection (0.0-1.0)
 * @property activityType User's physical activity (walking, driving, etc.)
 * @property activityConfidence Confidence in activity detection (0.0-1.0)
 * @property timeOfDay Time of day category (morning, afternoon, etc.)
 * @property hour Current hour (0-23)
 * @property dayOfWeek Day of week (1=Sunday to 7=Saturday)
 * @property deviceState Current device state (battery, network, etc.)
 * @property focusedElement Description of the currently focused UI element
 * @property customData Additional custom context data
 */
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
    val deviceState: Map<String, Any> = emptyMap(),

    // Focused element
    val focusedElement: String? = null,

    // Custom data
    val customData: Map<String, Any> = emptyMap()
) {
    /**
     * App category constants
     */
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

    /**
     * Location type constants
     */
    object LocationTypes {
        const val HOME = "home"
        const val WORK = "work"
        const val PUBLIC = "public"
        const val VEHICLE = "vehicle"
        const val OUTDOOR = "outdoor"
        const val UNKNOWN = "unknown"
    }

    /**
     * Activity type constants
     */
    object ActivityTypes {
        const val WALKING = "walking"
        const val RUNNING = "running"
        const val DRIVING = "driving"
        const val STATIONARY = "stationary"
        const val CYCLING = "cycling"
        const val UNKNOWN = "unknown"
    }

    /**
     * Time of day constants
     */
    object TimeOfDay {
        const val EARLY_MORNING = "early_morning"  // 5-8 AM
        const val MORNING = "morning"              // 8-12 PM
        const val AFTERNOON = "afternoon"          // 12-5 PM
        const val EVENING = "evening"              // 5-9 PM
        const val NIGHT = "night"                  // 9 PM-12 AM
        const val LATE_NIGHT = "late_night"        // 12-5 AM

        fun fromHour(hour: Int): String = when (hour) {
            in 5..7 -> EARLY_MORNING
            in 8..11 -> MORNING
            in 12..16 -> AFTERNOON
            in 17..20 -> EVENING
            in 21..23 -> NIGHT
            else -> LATE_NIGHT
        }
    }

    /**
     * Check if this is a weekday
     */
    fun isWeekday(): Boolean = dayOfWeek?.let { it in 2..6 } ?: false

    /**
     * Check if this is a weekend
     */
    fun isWeekend(): Boolean = dayOfWeek?.let { it == 1 || it == 7 } ?: false

    /**
     * Check if location confidence is above threshold
     */
    fun isLocationConfident(threshold: Float = 0.7f): Boolean = locationConfidence >= threshold

    /**
     * Check if activity confidence is above threshold
     */
    fun isActivityConfident(threshold: Float = 0.7f): Boolean = activityConfidence >= threshold
}

/**
 * Result of a command execution.
 *
 * @property success Whether the command executed successfully
 * @property command The command that was executed
 * @property response Human-readable response message
 * @property data Additional data returned by the command
 * @property error Error details if execution failed
 * @property executionTime Time taken to execute (milliseconds)
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
 *
 * @property code Error code for programmatic handling
 * @property message Human-readable error message
 * @property details Additional error details
 */
data class CommandError(
    val code: ErrorCode,
    val message: String,
    val details: String? = null
)

/**
 * Standardized error codes for command execution failures.
 */
enum class ErrorCode {
    /** Required module is not available */
    MODULE_NOT_AVAILABLE,

    /** Command not found in registry */
    COMMAND_NOT_FOUND,

    /** Invalid parameters provided */
    INVALID_PARAMETERS,

    /** Permission denied for this operation */
    PERMISSION_DENIED,

    /** Command execution failed */
    EXECUTION_FAILED,

    /** Operation timed out */
    TIMEOUT,

    /** Network error occurred */
    NETWORK_ERROR,

    /** Unknown error */
    UNKNOWN,

    /** Command not recognized */
    UNKNOWN_COMMAND,

    /** Required context missing */
    MISSING_CONTEXT,

    /** Operation was cancelled */
    CANCELLED,

    /** Accessibility service not available */
    NO_ACCESSIBILITY_SERVICE,

    /** Action execution failed */
    ACTION_FAILED
}

/**
 * Definition of a registered command.
 *
 * @property id Unique command identifier
 * @property name Display name of the command
 * @property description What the command does
 * @property category Command category for organization
 * @property patterns Recognition patterns for this command
 * @property parameters Parameters this command accepts
 * @property requiredPermissions Android permissions required
 * @property supportedLanguages Supported language codes
 * @property requiredContext Context fields required for execution
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
 * Definition of a command parameter.
 *
 * @property name Parameter name
 * @property type Data type of the parameter
 * @property required Whether this parameter is required
 * @property defaultValue Default value if not provided
 * @property description What this parameter does
 */
data class CommandParameter(
    val name: String,
    val type: ParameterType,
    val required: Boolean = false,
    val defaultValue: Any? = null,
    val description: String? = null
)

/**
 * Supported parameter types.
 */
enum class ParameterType {
    /** String value */
    STRING,

    /** Numeric value */
    NUMBER,

    /** Boolean value */
    BOOLEAN,

    /** List of values */
    LIST,

    /** Map/dictionary of key-value pairs */
    MAP,

    /** Custom type */
    CUSTOM
}

/**
 * Entry in command execution history.
 *
 * @property command The command that was executed
 * @property result The result of execution
 * @property timestamp When the command was executed (milliseconds since epoch)
 */
data class CommandHistoryEntry(
    val command: Command,
    val result: CommandResult,
    val timestamp: Long
)

/**
 * Event emitted during command lifecycle.
 *
 * @property type Type of event
 * @property command Associated command (if applicable)
 * @property result Command result (if applicable)
 * @property message Event message
 * @property timestamp When the event occurred (milliseconds since epoch)
 */
data class CommandEvent(
    val type: EventType,
    val command: Command? = null,
    val result: CommandResult? = null,
    val message: String? = null,
    val timestamp: Long
)

/**
 * Types of command lifecycle events.
 */
enum class EventType {
    /** Command received by the system */
    COMMAND_RECEIVED,

    /** Command is being executed */
    COMMAND_EXECUTING,

    /** Command completed successfully */
    COMMAND_COMPLETED,

    /** Command execution failed */
    COMMAND_FAILED,

    /** New command registered */
    COMMAND_REGISTERED,

    /** Command unregistered from system */
    COMMAND_UNREGISTERED
}

/**
 * Basic information about a command.
 *
 * @property id Command identifier
 * @property name Command name
 * @property category Command category
 * @property isCustom Whether this is a custom user command
 * @property usageCount Number of times this command has been used
 */
data class CommandInfo(
    val id: String,
    val name: String,
    val category: String,
    val isCustom: Boolean = false,
    val usageCount: Int = 0
)

/**
 * Handler function for processing commands.
 *
 * Suspending function that takes a Command and returns a CommandResult.
 */
typealias CommandHandler = suspend (Command) -> CommandResult

/**
 * Statistics about command usage.
 *
 * @property totalCommands Total number of commands executed
 * @property successfulCommands Number of successful executions
 * @property failedCommands Number of failed executions
 * @property averageExecutionTime Average execution time in milliseconds
 * @property topCommands Most frequently used commands
 */
data class CommandStats(
    val totalCommands: Int,
    val successfulCommands: Int,
    val failedCommands: Int,
    val averageExecutionTime: Long,
    val topCommands: List<String>
)

// CommandCategory is defined in StaticCommandRegistry.kt with more comprehensive values

/**
 * Accessibility action constants.
 *
 * These values match Android's AccessibilityNodeInfo action constants.
 */
object AccessibilityActions {
    /** Select all text in an editable field */
    const val ACTION_SELECT_ALL = 0x10000

    /** Backup and reset accessibility settings */
    const val ACTION_BACKUP_AND_RESET_SETTINGS = 0x20000
}
