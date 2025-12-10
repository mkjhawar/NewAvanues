/**
 * CommandModels.kt - Data models for command system
 * Direct implementation without interfaces for zero overhead
 */

package com.augmentalis.commandmanager.models

import android.content.Context

// Core command data classes

data class Command(
    val id: String,
    val text: String,
    val source: CommandSource,
    val context: CommandContext? = null,
    val parameters: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
    val confidence: Float = 1.0f
)

enum class CommandSource {
    VOICE,
    GESTURE,
    TEXT,
    SYSTEM,
    EXTERNAL
}

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

data class CommandResult(
    val success: Boolean,
    val command: Command,
    val response: String? = null,
    val data: Any? = null,
    val error: CommandError? = null,
    val executionTime: Long = 0
)

data class CommandError(
    val code: ErrorCode,
    val message: String,
    val details: String? = null
)

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

data class CommandParameter(
    val name: String,
    val type: ParameterType,
    val required: Boolean = false,
    val defaultValue: Any? = null,
    val description: String? = null
)

enum class ParameterType {
    STRING,
    NUMBER,
    BOOLEAN,
    LIST,
    MAP,
    CUSTOM
}

data class CommandHistoryEntry(
    val command: Command,
    val result: CommandResult,
    val timestamp: Long = System.currentTimeMillis()
)

data class CommandEvent(
    val type: EventType,
    val command: Command? = null,
    val result: CommandResult? = null,
    val message: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

enum class EventType {
    COMMAND_RECEIVED,
    COMMAND_EXECUTING,
    COMMAND_COMPLETED,
    COMMAND_FAILED,
    COMMAND_REGISTERED,
    COMMAND_UNREGISTERED
}

data class CommandInfo(
    val id: String,
    val name: String,
    val category: String,
    val isCustom: Boolean = false,
    val usageCount: Int = 0
)

// Handler interface - simple functional interface
typealias CommandHandler = suspend (Command) -> CommandResult

// Stats data class
data class CommandStats(
    val totalCommands: Int,
    val successfulCommands: Int,
    val failedCommands: Int,
    val averageExecutionTime: Long,
    val topCommands: List<String>
)

// Command categories
enum class CommandCategory {
    NAVIGATION,
    TEXT,
    MEDIA,
    SYSTEM,
    APP,
    ACCESSIBILITY,
    VOICE,
    GESTURE,
    CUSTOM,
    INPUT,
    APP_CONTROL
}

// Accessibility action constants
object AccessibilityActions {
    const val ACTION_SELECT_ALL = 0x10000
    const val ACTION_BACKUP_AND_RESET_SETTINGS = 0x20000
}