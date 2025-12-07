package com.augmentalis.voiceos.speech.engines.common

import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Centralized command processor that handles common speech commands
 * and eliminates duplicate command processing logic across engines.
 */
class CommandProcessor {
    
    private val commandHistory = CopyOnWriteArrayList<CommandHistoryEntry>()
    private var callbacks: EngineCallbacks? = null
    private var confidenceThreshold: Float = 0.7f
    
    /**
     * Data class representing the result of command processing
     */
    data class CommandResult(
        val isHandled: Boolean,
        val command: String,
        val normalizedCommand: String,
        val confidence: Float,
        val timestamp: Long = System.currentTimeMillis(),
        val success: Boolean = false,
        val errorMessage: String? = null
    )
    
    /**
     * Data class for command history tracking
     */
    data class CommandHistoryEntry(
        val originalCommand: String,
        val normalizedCommand: String,
        val confidence: Float,
        val timestamp: Long,
        val wasHandled: Boolean,
        val success: Boolean
    )
    
    /**
     * Interface for engine-specific command callbacks
     */
    interface EngineCallbacks {
        /**
         * Called for commands that are not handled by common processor
         */
        fun onCustomCommand(command: String, confidence: Float): CommandResult
        
        /**
         * Called when a command execution fails
         */
        fun onCommandError(command: String, error: String)
        
        /**
         * Called when the engine state should be updated
         */
        fun onEngineStateChange(newState: String)
    }
    
    /**
     * Common commands that are handled by this processor
     */
    enum class CommonCommand(val patterns: List<String>) {
        MUTE(listOf("mute", "silence", "quiet", "stop listening")),
        UNMUTE(listOf("unmute", "listen", "wake up", "start listening")),
        START_DICTATION(listOf("start dictation", "begin dictation", "dictate", "start typing")),
        STOP_DICTATION(listOf("stop dictation", "end dictation", "stop typing", "finish dictation")),
        PAUSE_RECOGNITION(listOf("pause", "pause recognition", "hold")),
        RESUME_RECOGNITION(listOf("resume", "continue", "resume recognition")),
        CLEAR_BUFFER(listOf("clear", "reset", "clear buffer", "start over"));
        
        fun matches(command: String): Boolean {
            val normalizedInput = command.lowercase().trim()
            return patterns.any { pattern ->
                normalizedInput.contains(pattern.lowercase()) || 
                normalizedInput == pattern.lowercase()
            }
        }
    }
    
    /**
     * Set the engine callbacks for handling custom commands
     */
    fun setEngineCallbacks(callbacks: EngineCallbacks) {
        this.callbacks = callbacks
    }
    
    /**
     * Set the confidence threshold for command processing
     */
    fun setConfidenceThreshold(threshold: Float) {
        this.confidenceThreshold = threshold.coerceIn(0.0f, 1.0f)
    }
    
    /**
     * Process a speech command with confidence score
     */
    fun processCommand(command: String, confidence: Float): CommandResult {
        val normalizedCommand = normalizeCommand(command)
        val timestamp = System.currentTimeMillis()
        
        // Check confidence threshold
        if (confidence < confidenceThreshold) {
            val result = CommandResult(
                isHandled = false,
                command = command,
                normalizedCommand = normalizedCommand,
                confidence = confidence,
                timestamp = timestamp,
                success = false,
                errorMessage = "Confidence below threshold ($confidence < $confidenceThreshold)"
            )
            addToHistory(command, normalizedCommand, confidence, timestamp, false, false)
            return result
        }
        
        // Validate command
        if (!isValidCommand(normalizedCommand)) {
            val result = CommandResult(
                isHandled = false,
                command = command,
                normalizedCommand = normalizedCommand,
                confidence = confidence,
                timestamp = timestamp,
                success = false,
                errorMessage = "Invalid command format"
            )
            addToHistory(command, normalizedCommand, confidence, timestamp, false, false)
            return result
        }
        
        // Try to handle as common command
        val commonCommandResult = handleCommonCommand(normalizedCommand, confidence, timestamp)
        if (commonCommandResult.isHandled) {
            addToHistory(command, normalizedCommand, confidence, timestamp, true, commonCommandResult.success)
            return commonCommandResult.copy(command = command)
        }
        
        // Delegate to engine-specific handler
        val customResult = callbacks?.onCustomCommand(normalizedCommand, confidence) ?: CommandResult(
            isHandled = false,
            command = command,
            normalizedCommand = normalizedCommand,
            confidence = confidence,
            timestamp = timestamp,
            success = false,
            errorMessage = "No engine callbacks registered"
        )
        
        addToHistory(command, normalizedCommand, confidence, timestamp, customResult.isHandled, customResult.success)
        return customResult.copy(command = command)
    }
    
    /**
     * Normalize command text for consistent processing
     */
    private fun normalizeCommand(command: String): String {
        return command.trim()
            .lowercase()
            .replace(Regex("\\s+"), " ") // Replace multiple spaces with single space
            .replace(Regex("[^a-z0-9\\s]"), "") // Remove special characters except spaces
    }
    
    /**
     * Validate command format
     */
    private fun isValidCommand(command: String): Boolean {
        return command.isNotBlank() && 
               command.length <= 100 && 
               command.matches(Regex("[a-z0-9\\s]+"))
    }
    
    /**
     * Handle common commands
     */
    private fun handleCommonCommand(normalizedCommand: String, confidence: Float, timestamp: Long): CommandResult {
        return when {
            CommonCommand.MUTE.matches(normalizedCommand) -> {
                handleMute(normalizedCommand, confidence, timestamp)
            }
            CommonCommand.UNMUTE.matches(normalizedCommand) -> {
                handleUnmute(normalizedCommand, confidence, timestamp)
            }
            CommonCommand.START_DICTATION.matches(normalizedCommand) -> {
                handleStartDictation(normalizedCommand, confidence, timestamp)
            }
            CommonCommand.STOP_DICTATION.matches(normalizedCommand) -> {
                handleStopDictation(normalizedCommand, confidence, timestamp)
            }
            CommonCommand.PAUSE_RECOGNITION.matches(normalizedCommand) -> {
                handlePauseRecognition(normalizedCommand, confidence, timestamp)
            }
            CommonCommand.RESUME_RECOGNITION.matches(normalizedCommand) -> {
                handleResumeRecognition(normalizedCommand, confidence, timestamp)
            }
            CommonCommand.CLEAR_BUFFER.matches(normalizedCommand) -> {
                handleClearBuffer(normalizedCommand, confidence, timestamp)
            }
            else -> CommandResult(
                isHandled = false,
                command = "",
                normalizedCommand = normalizedCommand,
                confidence = confidence,
                timestamp = timestamp
            )
        }
    }
    
    /**
     * Handle mute command
     */
    private fun handleMute(command: String, confidence: Float, timestamp: Long): CommandResult {
        return try {
            callbacks?.onEngineStateChange("MUTED")
            CommandResult(
                isHandled = true,
                command = "",
                normalizedCommand = command,
                confidence = confidence,
                timestamp = timestamp,
                success = true
            )
        } catch (e: Exception) {
            callbacks?.onCommandError(command, e.message ?: "Unknown error during mute")
            CommandResult(
                isHandled = true,
                command = "",
                normalizedCommand = command,
                confidence = confidence,
                timestamp = timestamp,
                success = false,
                errorMessage = e.message
            )
        }
    }
    
    /**
     * Handle unmute command
     */
    private fun handleUnmute(command: String, confidence: Float, timestamp: Long): CommandResult {
        return try {
            callbacks?.onEngineStateChange("LISTENING")
            CommandResult(
                isHandled = true,
                command = "",
                normalizedCommand = command,
                confidence = confidence,
                timestamp = timestamp,
                success = true
            )
        } catch (e: Exception) {
            callbacks?.onCommandError(command, e.message ?: "Unknown error during unmute")
            CommandResult(
                isHandled = true,
                command = "",
                normalizedCommand = command,
                confidence = confidence,
                timestamp = timestamp,
                success = false,
                errorMessage = e.message
            )
        }
    }
    
    /**
     * Handle start dictation command
     */
    private fun handleStartDictation(command: String, confidence: Float, timestamp: Long): CommandResult {
        return try {
            callbacks?.onEngineStateChange("DICTATING")
            CommandResult(
                isHandled = true,
                command = "",
                normalizedCommand = command,
                confidence = confidence,
                timestamp = timestamp,
                success = true
            )
        } catch (e: Exception) {
            callbacks?.onCommandError(command, e.message ?: "Unknown error starting dictation")
            CommandResult(
                isHandled = true,
                command = "",
                normalizedCommand = command,
                confidence = confidence,
                timestamp = timestamp,
                success = false,
                errorMessage = e.message
            )
        }
    }
    
    /**
     * Handle stop dictation command
     */
    private fun handleStopDictation(command: String, confidence: Float, timestamp: Long): CommandResult {
        return try {
            callbacks?.onEngineStateChange("LISTENING")
            CommandResult(
                isHandled = true,
                command = "",
                normalizedCommand = command,
                confidence = confidence,
                timestamp = timestamp,
                success = true
            )
        } catch (e: Exception) {
            callbacks?.onCommandError(command, e.message ?: "Unknown error stopping dictation")
            CommandResult(
                isHandled = true,
                command = "",
                normalizedCommand = command,
                confidence = confidence,
                timestamp = timestamp,
                success = false,
                errorMessage = e.message
            )
        }
    }
    
    /**
     * Handle pause recognition command
     */
    private fun handlePauseRecognition(command: String, confidence: Float, timestamp: Long): CommandResult {
        return try {
            callbacks?.onEngineStateChange("PAUSED")
            CommandResult(
                isHandled = true,
                command = "",
                normalizedCommand = command,
                confidence = confidence,
                timestamp = timestamp,
                success = true
            )
        } catch (e: Exception) {
            callbacks?.onCommandError(command, e.message ?: "Unknown error pausing recognition")
            CommandResult(
                isHandled = true,
                command = "",
                normalizedCommand = command,
                confidence = confidence,
                timestamp = timestamp,
                success = false,
                errorMessage = e.message
            )
        }
    }
    
    /**
     * Handle resume recognition command
     */
    private fun handleResumeRecognition(command: String, confidence: Float, timestamp: Long): CommandResult {
        return try {
            callbacks?.onEngineStateChange("LISTENING")
            CommandResult(
                isHandled = true,
                command = "",
                normalizedCommand = command,
                confidence = confidence,
                timestamp = timestamp,
                success = true
            )
        } catch (e: Exception) {
            callbacks?.onCommandError(command, e.message ?: "Unknown error resuming recognition")
            CommandResult(
                isHandled = true,
                command = "",
                normalizedCommand = command,
                confidence = confidence,
                timestamp = timestamp,
                success = false,
                errorMessage = e.message
            )
        }
    }
    
    /**
     * Handle clear buffer command
     */
    private fun handleClearBuffer(command: String, confidence: Float, timestamp: Long): CommandResult {
        return try {
            callbacks?.onEngineStateChange("BUFFER_CLEARED")
            CommandResult(
                isHandled = true,
                command = "",
                normalizedCommand = command,
                confidence = confidence,
                timestamp = timestamp,
                success = true
            )
        } catch (e: Exception) {
            callbacks?.onCommandError(command, e.message ?: "Unknown error clearing buffer")
            CommandResult(
                isHandled = true,
                command = "",
                normalizedCommand = command,
                confidence = confidence,
                timestamp = timestamp,
                success = false,
                errorMessage = e.message
            )
        }
    }
    
    /**
     * Add command to history
     */
    private fun addToHistory(
        originalCommand: String,
        normalizedCommand: String,
        confidence: Float,
        timestamp: Long,
        wasHandled: Boolean,
        success: Boolean
    ) {
        val entry = CommandHistoryEntry(
            originalCommand = originalCommand,
            normalizedCommand = normalizedCommand,
            confidence = confidence,
            timestamp = timestamp,
            wasHandled = wasHandled,
            success = success
        )
        
        commandHistory.add(entry)
        
        // Keep history size manageable (last 100 commands)
        if (commandHistory.size > 100) {
            commandHistory.removeAt(0)
        }
    }
    
    /**
     * Get command history
     */
    fun getCommandHistory(): List<CommandHistoryEntry> {
        return commandHistory.toList()
    }
    
    /**
     * Get recent command history (last N commands)
     */
    fun getRecentHistory(count: Int): List<CommandHistoryEntry> {
        val size = commandHistory.size
        val startIndex = maxOf(0, size - count)
        return commandHistory.subList(startIndex, size).toList()
    }
    
    /**
     * Clear command history
     */
    fun clearHistory() {
        commandHistory.clear()
    }
    
    /**
     * Get statistics about command processing
     */
    fun getStatistics(): CommandStatistics {
        val totalCommands = commandHistory.size
        val handledCommands = commandHistory.count { it.wasHandled }
        val successfulCommands = commandHistory.count { it.success }
        val averageConfidence = if (commandHistory.isNotEmpty()) {
            commandHistory.map { it.confidence }.average().toFloat()
        } else 0.0f
        
        return CommandStatistics(
            totalCommands = totalCommands,
            handledCommands = handledCommands,
            successfulCommands = successfulCommands,
            handlingRate = if (totalCommands > 0) handledCommands.toFloat() / totalCommands else 0.0f,
            successRate = if (handledCommands > 0) successfulCommands.toFloat() / handledCommands else 0.0f,
            averageConfidence = averageConfidence
        )
    }
    
    /**
     * Data class for command processing statistics
     */
    data class CommandStatistics(
        val totalCommands: Int,
        val handledCommands: Int,
        val successfulCommands: Int,
        val handlingRate: Float,
        val successRate: Float,
        val averageConfidence: Float
    )
}