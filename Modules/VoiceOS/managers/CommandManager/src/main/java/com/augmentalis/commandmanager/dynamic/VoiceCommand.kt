/**
 * VoiceCommand.kt - Data model for dynamic voice commands
 *
 * Part of Week 4 implementation - Dynamic Command Registration
 * Direct implementation with zero overhead
 *
 * @since VOS4 Week 4
 * @author VOS4 Development Team
 */

package com.augmentalis.commandmanager.dynamic

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents a dynamically registered voice command
 *
 * @property id Unique identifier for the command
 * @property phrases List of voice phrases that trigger this command
 * @property priority Priority level (1-100, higher = higher priority)
 * @property namespace Namespace for module isolation (default: "default")
 * @property description Human-readable description of what the command does
 * @property category Category for grouping commands
 * @property enabled Whether the command is currently active
 * @property createdAt Timestamp when command was created
 * @property lastUsed Timestamp when command was last executed
 * @property usageCount Number of times this command has been executed
 * @property metadata Additional custom metadata
 * @property action The suspend function that executes this command's logic
 */
data class VoiceCommand(
    val id: String,
    val phrases: List<String>,
    val priority: Int = 50, // 1-100, higher = higher priority
    val namespace: String = "default",
    val description: String = "",
    val category: CommandCategory = CommandCategory.CUSTOM,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long = 0L,
    val usageCount: Long = 0L,
    val metadata: Map<String, String> = emptyMap(),
    val action: suspend (CommandExecutionContext) -> CommandResult
) {
    init {
        require(id.isNotBlank()) { "Command ID cannot be blank" }
        require(phrases.isNotEmpty()) { "Command must have at least one phrase" }
        require(phrases.all { it.isNotBlank() }) { "Phrases cannot be blank" }
        require(priority in 1..100) { "Priority must be between 1 and 100" }
        require(namespace.isNotBlank()) { "Namespace cannot be blank" }
    }

    /**
     * Get priority level category
     */
    fun getPriorityLevel(): PriorityLevel = when (priority) {
        in 1..25 -> PriorityLevel.LOW
        in 26..50 -> PriorityLevel.NORMAL
        in 51..75 -> PriorityLevel.HIGH
        in 76..100 -> PriorityLevel.CRITICAL
        else -> PriorityLevel.NORMAL
    }

    /**
     * Create a copy with updated usage statistics
     */
    fun recordUsage(): VoiceCommand = copy(
        lastUsed = System.currentTimeMillis(),
        usageCount = usageCount + 1
    )

    /**
     * Check if this command matches a given phrase
     */
    fun matches(phrase: String): Boolean {
        val normalizedPhrase = phrase.trim().lowercase()
        return phrases.any { it.trim().lowercase() == normalizedPhrase }
    }

    /**
     * Get similarity score with a given phrase (0.0 to 1.0)
     */
    fun getSimilarity(phrase: String): Float {
        val normalizedPhrase = phrase.trim().lowercase()
        return phrases.map { targetPhrase ->
            calculateSimilarity(normalizedPhrase, targetPhrase.trim().lowercase())
        }.maxOrNull() ?: 0f
    }

    /**
     * Calculate Levenshtein distance-based similarity
     */
    private fun calculateSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        if (s1.isEmpty() || s2.isEmpty()) return 0f

        val maxLen = maxOf(s1.length, s2.length)
        val distance = levenshteinDistance(s1, s2)
        return 1f - (distance.toFloat() / maxLen)
    }

    /**
     * Calculate Levenshtein distance between two strings
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[m][n]
    }
}

/**
 * Priority level categories for commands
 */
enum class PriorityLevel {
    LOW,      // 1-25: Low priority commands
    NORMAL,   // 26-50: Normal priority commands
    HIGH,     // 51-75: High priority commands
    CRITICAL  // 76-100: Critical priority commands (system-level)
}

/**
 * Command category for grouping
 */
enum class CommandCategory {
    NAVIGATION,      // Navigation commands (go to, back, home)
    TEXT_EDITING,    // Text manipulation (select, copy, paste)
    SYSTEM,          // System controls (volume, brightness)
    APP_CONTROL,     // App-specific commands
    ACCESSIBILITY,   // Accessibility features
    MEDIA,           // Media playback controls
    VOICE,           // Voice-specific commands
    GESTURE,         // Gesture-based commands
    CUSTOM           // User-defined custom commands
}

/**
 * Context provided during command execution
 */
data class CommandExecutionContext(
    val recognizedPhrase: String,
    val confidence: Float = 1.0f,
    val parameters: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String? = null,
    val deviceInfo: Map<String, String> = emptyMap()
)

/**
 * Result of command execution
 */
sealed class CommandResult {
    /**
     * Command executed successfully
     */
    object Success : CommandResult()

    /**
     * Command execution failed with an error
     *
     * @property message Error message describing what went wrong
     * @property code Optional error code for categorization
     * @property cause Optional underlying exception
     */
    data class Error(
        val message: String,
        val code: ErrorCode = ErrorCode.UNKNOWN,
        val cause: Throwable? = null
    ) : CommandResult()

    /**
     * Command partially executed, requires additional steps
     *
     * @property message Description of current state
     * @property nextSteps List of possible next commands to complete the task
     * @property completionPercentage How much of the command was completed (0-100)
     */
    data class Partial(
        val message: String,
        val nextSteps: List<VoiceCommand> = emptyList(),
        val completionPercentage: Int = 50
    ) : CommandResult() {
        init {
            require(completionPercentage in 0..100) {
                "Completion percentage must be between 0 and 100"
            }
        }
    }

    /**
     * Command requires user confirmation before proceeding
     *
     * @property message Confirmation prompt message
     * @property confirmAction Action to execute if confirmed
     * @property cancelAction Action to execute if cancelled
     */
    data class RequiresConfirmation(
        val message: String,
        val confirmAction: suspend () -> CommandResult,
        val cancelAction: suspend () -> CommandResult = { Success }
    ) : CommandResult()

    /**
     * Command execution was cancelled by user
     */
    object Cancelled : CommandResult()
}

/**
 * Error codes for command execution failures
 */
enum class ErrorCode {
    UNKNOWN,                 // Unknown error
    PERMISSION_DENIED,       // Missing required permissions
    INVALID_PARAMETERS,      // Invalid command parameters
    EXECUTION_FAILED,        // Command execution failed
    TIMEOUT,                 // Command execution timed out
    NETWORK_ERROR,           // Network-related error
    NOT_AVAILABLE,           // Feature not available on this device
    DISABLED,                // Command is disabled
    CONFLICT,                // Command conflicts with another command
    RESOURCE_UNAVAILABLE     // Required resource is unavailable
}

/**
 * Parcelable version of VoiceCommand for persistence and IPC
 *
 * Note: Cannot include suspend functions in Parcelable, so action is excluded
 */
@Parcelize
data class VoiceCommandData(
    val id: String,
    val phrases: List<String>,
    val priority: Int,
    val namespace: String,
    val description: String,
    val category: CommandCategory,
    val enabled: Boolean,
    val createdAt: Long,
    val lastUsed: Long,
    val usageCount: Long,
    val metadata: Map<String, String>
) : Parcelable {
    companion object {
        /**
         * Create VoiceCommandData from VoiceCommand
         */
        fun from(command: VoiceCommand): VoiceCommandData = VoiceCommandData(
            id = command.id,
            phrases = command.phrases,
            priority = command.priority,
            namespace = command.namespace,
            description = command.description,
            category = command.category,
            enabled = command.enabled,
            createdAt = command.createdAt,
            lastUsed = command.lastUsed,
            usageCount = command.usageCount,
            metadata = command.metadata
        )
    }
}
