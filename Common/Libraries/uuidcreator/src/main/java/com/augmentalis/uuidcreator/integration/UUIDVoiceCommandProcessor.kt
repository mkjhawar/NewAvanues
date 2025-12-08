/**
 * UUIDVoiceCommandProcessor.kt - Voice command processor with alias support
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/integration/UUIDVoiceCommandProcessor.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Processes voice commands with UUID and alias resolution
 *
 * NOTE: This file is NOT wired into VOS4. It provides integration interfaces only.
 */

package com.augmentalis.uuidcreator.integration

import com.augmentalis.uuidcreator.UUIDCreator
import com.augmentalis.uuidcreator.alias.UuidAliasManager
import com.augmentalis.uuidcreator.analytics.UuidAnalytics

/**
 * UUID Voice Command Processor
 *
 * Processes voice commands with alias resolution and analytics tracking.
 *
 * ## Command Formats Supported
 *
 * ### Alias Commands
 * ```
 * "click instagram_like_btn"
 * "tap submit_btn"
 * "focus main_menu"
 * ```
 *
 * ### UUID Commands
 * ```
 * "click uuid 550e8400-e29b-41d4-a716-446655440000"
 * "tap btn-550e8400-e29b-41d4-a716-446655440000"
 * "focus com.instagram.android.v12.0.0.button-abc123"
 * ```
 *
 * ### Name Commands
 * ```
 * "click submit button"
 * "tap like button"
 * "focus menu"
 * ```
 *
 * ## Usage Example
 *
 * ```kotlin
 * val processor = UUIDVoiceCommandProcessor(
 *     uuidCreator = uuidCreator,
 *     aliasManager = aliasManager,
 *     analytics = analytics
 * )
 *
 * // Process command
 * val success = processor.processCommand("click instagram_like_btn")
 * if (success) {
 *     println("Command executed")
 * } else {
 *     println("Command failed")
 * }
 *
 * // Get command stats
 * val stats = processor.getStats()
 * ```
 *
 * @property uuidCreator UUIDCreator instance
 * @property aliasManager Alias manager for resolution
 * @property analytics Analytics tracker
 *
 * @since 1.0.0
 */
class UUIDVoiceCommandProcessor(
    private val uuidCreator: UUIDCreator,
    private val aliasManager: UuidAliasManager,
    private val analytics: UuidAnalytics
) {

    /**
     * Command statistics
     */
    private val stats = CommandStats()

    /**
     * Command history (last 100 commands)
     */
    private val commandHistory = mutableListOf<CommandHistoryEntry>()

    /**
     * Process voice command
     *
     * Main entry point for command processing.
     *
     * ## Processing Flow
     *
     * 1. Parse command text
     * 2. Resolve target (alias → UUID or direct UUID)
     * 3. Find element
     * 4. Execute action
     * 5. Track analytics
     * 6. Return result
     *
     * @param command Voice command text
     * @return true if command executed successfully
     */
    suspend fun processCommand(command: String): Boolean {
        val startTime = System.currentTimeMillis()
        stats.totalCommands++

        try {
            // Parse command
            val parsed = parseCommand(command)
            if (parsed == null) {
                stats.failedCommands++
                addToHistory(command, false, "Failed to parse command")
                return false
            }

            // Resolve target (alias or UUID)
            val uuid = resolveTarget(parsed.target)
            if (uuid == null) {
                stats.failedCommands++
                addToHistory(command, false, "Target not found: ${parsed.target}")
                return false
            }

            // Find element
            val element = uuidCreator.findByUUID(uuid)
            if (element == null) {
                stats.failedCommands++
                addToHistory(command, false, "Element not found: $uuid")
                return false
            }

            // Execute action
            val executionStart = System.currentTimeMillis()
            val success = uuidCreator.executeAction(
                uuid = uuid,
                action = parsed.action,
                parameters = emptyMap()
            )
            val executionTime = System.currentTimeMillis() - executionStart

            // Track analytics
            analytics.trackExecution(
                uuid = uuid,
                action = parsed.action,
                executionTimeMs = executionTime,
                success = success
            )

            // Update stats
            if (success) {
                stats.successfulCommands++
            } else {
                stats.failedCommands++
            }

            // Add to history
            addToHistory(command, success, if (success) "Success" else "Execution failed")

            return success

        } catch (e: Exception) {
            stats.failedCommands++
            addToHistory(command, false, "Exception: ${e.message}")
            return false
        } finally {
            stats.totalExecutionTime += System.currentTimeMillis() - startTime
        }
    }

    /**
     * Parse command text
     *
     * Extracts action and target from command.
     *
     * @param command Command text
     * @return Parsed command or null if invalid
     */
    private fun parseCommand(command: String): ParsedCommand? {
        val normalized = command.lowercase().trim()

        // Pattern: "action target"
        // Examples:
        // - "click instagram_like_btn"
        // - "tap submit_btn"
        // - "focus main_menu"
        val pattern = Regex("^(click|tap|focus|select|open)\\s+(.+)$")
        val match = pattern.find(normalized) ?: return null

        val action = match.groupValues[1]
        val target = match.groupValues[2]

        return ParsedCommand(action, target)
    }

    /**
     * Resolve target to UUID
     *
     * Resolves alias or direct UUID to actual UUID.
     *
     * @param target Target string (alias or UUID)
     * @return Resolved UUID or null if not found
     */
    private suspend fun resolveTarget(target: String): String? {
        // Try alias resolution first
        val aliasUuid = aliasManager.resolveAlias(target)
        if (aliasUuid != null) {
            return aliasUuid
        }

        // Try direct UUID (if target is already a UUID)
        val element = uuidCreator.findByUUID(target)
        if (element != null) {
            return target
        }

        // Try finding by name
        val byName = uuidCreator.findByName(target)
        if (byName.isNotEmpty()) {
            return byName.first().uuid
        }

        return null
    }

    /**
     * Add command to history
     *
     * @param command Command text
     * @param success Whether command succeeded
     * @param message Result message
     */
    private fun addToHistory(command: String, success: Boolean, message: String) {
        commandHistory.add(
            CommandHistoryEntry(
                command = command,
                success = success,
                message = message,
                timestamp = System.currentTimeMillis()
            )
        )

        // Keep only last 100 commands
        if (commandHistory.size > 100) {
            commandHistory.removeAt(0)
        }
    }

    /**
     * Get command statistics
     *
     * @return Command stats
     */
    fun getStats(): CommandStats {
        return stats.copy(
            successRate = if (stats.totalCommands > 0) {
                stats.successfulCommands.toFloat() / stats.totalCommands
            } else {
                0f
            },
            averageExecutionTime = if (stats.totalCommands > 0) {
                stats.totalExecutionTime / stats.totalCommands
            } else {
                0L
            }
        )
    }

    /**
     * Get command history
     *
     * @param limit Maximum number of entries to return
     * @return List of command history entries
     */
    fun getHistory(limit: Int = 20): List<CommandHistoryEntry> {
        return commandHistory.takeLast(limit)
    }

    /**
     * Clear command history
     */
    fun clearHistory() {
        commandHistory.clear()
    }

    /**
     * Clear statistics
     */
    fun clearStats() {
        stats.totalCommands = 0
        stats.successfulCommands = 0
        stats.failedCommands = 0
        stats.totalExecutionTime = 0L
    }
}

/**
 * Parsed Command
 *
 * @property action Action to execute (click, tap, focus, etc.)
 * @property target Target identifier (alias or UUID)
 */
private data class ParsedCommand(
    val action: String,
    val target: String
)

/**
 * Command Statistics
 *
 * @property totalCommands Total commands processed
 * @property successfulCommands Successful commands
 * @property failedCommands Failed commands
 * @property totalExecutionTime Total execution time (ms)
 * @property successRate Success rate (0.0-1.0)
 * @property averageExecutionTime Average execution time (ms)
 */
data class CommandStats(
    var totalCommands: Int = 0,
    var successfulCommands: Int = 0,
    var failedCommands: Int = 0,
    var totalExecutionTime: Long = 0L,
    var successRate: Float = 0f,
    var averageExecutionTime: Long = 0L
) {
    override fun toString(): String {
        return """
            Voice Command Statistics:
            - Total Commands: $totalCommands
            - Successful: $successfulCommands
            - Failed: $failedCommands
            - Success Rate: ${"%.1f".format(successRate * 100)}%
            - Avg Execution Time: ${averageExecutionTime}ms
        """.trimIndent()
    }
}

/**
 * Command History Entry
 *
 * @property command Command text
 * @property success Whether command succeeded
 * @property message Result message
 * @property timestamp Timestamp
 */
data class CommandHistoryEntry(
    val command: String,
    val success: Boolean,
    val message: String,
    val timestamp: Long
)
