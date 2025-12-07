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
package com.augmentalis.voiceos.command

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

    /** System-generated command */
    SYSTEM,

    /** External API or plugin */
    EXTERNAL
}

/**
 * Context information about the command execution environment.
 *
 * @property packageName Package name of the active application
 * @property activityName Activity name of the active screen
 * @property viewId View identifier of the focused element
 * @property screenContent Text content visible on screen
 * @property userLocation User's current location
 * @property deviceState Current device state (battery, network, etc.)
 * @property focusedElement Description of the currently focused UI element
 * @property customData Additional custom context data
 */
data class CommandContext(
    val packageName: String? = null,
    val activityName: String? = null,
    val viewId: String? = null,
    val screenContent: String? = null,
    val userLocation: String? = null,
    val deviceState: Map<String, Any> = emptyMap(),
    val focusedElement: String? = null,
    val customData: Map<String, Any> = emptyMap()
)

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

/**
 * Categories for organizing commands.
 */
enum class CommandCategory {
    /** Navigation commands (go, back, forward) */
    NAVIGATION,

    /** Text input commands (type, delete) */
    TEXT,

    /** Media control commands (play, pause) */
    MEDIA,

    /** System commands (volume, brightness) */
    SYSTEM,

    /** Application control commands */
    APP,

    /** Accessibility-specific commands */
    ACCESSIBILITY,

    /** Voice-specific commands */
    VOICE,

    /** Gesture-based commands */
    GESTURE,

    /** Custom user commands */
    CUSTOM,

    /** Input method commands */
    INPUT,

    /** App control commands */
    APP_CONTROL
}

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
