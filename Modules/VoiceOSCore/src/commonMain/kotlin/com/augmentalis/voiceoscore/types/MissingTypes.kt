/**
 * MissingTypes.kt - Stub definitions for missing types during migration
 */
package com.augmentalis.voiceoscore

/**
 * Error codes for command execution.
 */
enum class ErrorCode {
    NONE,
    ELEMENT_NOT_FOUND,
    PERMISSION_DENIED,
    TIMEOUT,
    INVALID_STATE,
    NOT_SUPPORTED,
    UNKNOWN,
    EXECUTION_FAILED,
    MODULE_NOT_AVAILABLE,
    INVALID_PARAMETERS
}

/**
 * Source of a command.
 */
enum class CommandSource {
    VOICE,
    GESTURE,
    KEYBOARD,
    PROGRAMMATIC,
    UNKNOWN
}

/**
 * Command error details.
 */
data class CommandError(
    val code: ErrorCode,
    val message: String,
    val details: String? = null
)

/**
 * Basic command representation.
 */
data class Command(
    val id: String? = null,
    val text: String? = null,
    val action: String? = null,
    val target: String? = null,
    val parameters: Map<String, Any> = emptyMap(),
    val source: CommandSource = CommandSource.UNKNOWN,
    val timestamp: Long = 0L,
    val confidence: Float = 1.0f
)

/**
 * Result of a command execution.
 */
data class CommandResult(
    val success: Boolean,
    val command: Command? = null,
    val response: String? = null,
    val error: CommandError? = null,
    val executionTime: Long = 0L,
    val data: Any? = null
) {
    val isSuccess: Boolean get() = success
    val message: String get() = response ?: error?.message ?: ""
}
