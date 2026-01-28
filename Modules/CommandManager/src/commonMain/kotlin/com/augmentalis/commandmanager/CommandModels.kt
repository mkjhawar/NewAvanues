/**
 * CommandManager - Core Command Models
 *
 * Pure Kotlin data models for voice command processing.
 * This module provides the API contract - implementations are in VoiceOSCore.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-28
 */
package com.augmentalis.commandmanager

import kotlinx.serialization.Serializable

/**
 * Voice OS command structure.
 *
 * Used by command parsers to represent commands from .vos and JSON files.
 *
 * @property action Unique action ID (e.g., "NAVIGATE_FORWARD", "VOLUME_UP")
 * @property cmd Primary command text (e.g., "forward", "increase volume")
 * @property syn List of synonyms (e.g., ["next", "advance", "go forward"])
 */
@Serializable
data class VOSCommand(
    val action: String,
    val cmd: String,
    val syn: List<String> = emptyList()
)

/**
 * Represents a command to be executed by the voice system.
 *
 * @property id Unique identifier for the command
 * @property text Human-readable command text
 * @property source Where the command originated from
 * @property context Additional context about the command environment
 * @property parameters Command-specific parameters
 * @property timestamp When the command was created (milliseconds since epoch)
 * @property confidence Confidence score for the command recognition (0.0-1.0)
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
 * @property packageName Package name of the active application
 * @property activityName Activity name of the active screen
 * @property appCategory Category of the current app
 * @property viewId View identifier of the focused element
 * @property screenContent Text content visible on screen
 * @property hasEditableFields Whether the screen has editable text fields
 * @property hasScrollableContent Whether the screen has scrollable content
 * @property focusedElement Description of the currently focused UI element
 */
@Serializable
data class CommandContext(
    val packageName: String? = null,
    val activityName: String? = null,
    val appCategory: String? = null,
    val viewId: String? = null,
    val screenContent: String? = null,
    val hasEditableFields: Boolean = false,
    val hasScrollableContent: Boolean = false,
    val focusedElement: String? = null
)

/**
 * Result of a command execution.
 *
 * @property success Whether the command executed successfully
 * @property commandId The command ID that was executed
 * @property response Human-readable response message
 * @property errorCode Error code if execution failed
 * @property executionTimeMs Time taken to execute (milliseconds)
 */
@Serializable
data class CommandResult(
    val success: Boolean,
    val commandId: String,
    val response: String? = null,
    val errorCode: ErrorCode? = null,
    val executionTimeMs: Long = 0
)

/**
 * Standardized error codes for command execution failures.
 */
@Serializable
enum class ErrorCode {
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

    /** Command not recognized */
    UNKNOWN_COMMAND,

    /** Required context missing */
    MISSING_CONTEXT,

    /** Operation was cancelled */
    CANCELLED,

    /** Accessibility service not available */
    NO_ACCESSIBILITY_SERVICE,

    /** Action execution failed */
    ACTION_FAILED,

    /** Unknown error */
    UNKNOWN
}

/**
 * Definition of a registered command.
 *
 * @property id Unique command identifier
 * @property name Display name of the command
 * @property description What the command does
 * @property category Command category for organization
 * @property patterns Recognition patterns for this command
 * @property supportedLanguages Supported language codes
 */
@Serializable
data class CommandDefinition(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val patterns: List<String>,
    val supportedLanguages: List<String> = listOf("en")
)

/**
 * Command categories for organization.
 */
@Serializable
enum class CommandCategory {
    /** Navigation commands (back, forward, scroll) */
    NAVIGATION,

    /** System commands (volume, brightness) */
    SYSTEM,

    /** App control commands (open, close) */
    APP_CONTROL,

    /** Text editing commands (select, copy, paste) */
    TEXT_EDITING,

    /** UI interaction commands (click, tap) */
    UI_INTERACTION,

    /** Media control commands (play, pause) */
    MEDIA,

    /** Communication commands (call, message) */
    COMMUNICATION,

    /** Custom user-defined commands */
    CUSTOM
}
