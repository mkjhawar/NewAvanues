/**
 * IVoiceOSServiceLocal.kt - Local public interface for VoiceOS accessibility service
 *
 * Note: Renamed from IVoiceOSService to avoid conflict with AIDL-generated IVoiceOSService
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-22
 *
 * Purpose: Public API for external components to interact with VoiceOSService
 * Provides controlled access to service functionality while maintaining encapsulation
 *
 * VOS4 Exception: Interface justified for public API boundary
 * - Defines contract for external service interactions
 * - Enables testing with mock implementations
 * - Maintains service encapsulation and SOLID principles
 */
package com.augmentalis.voiceoscore.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceos.command.Command
import kotlinx.coroutines.flow.StateFlow

/**
 * Public interface for VoiceOS accessibility service
 *
 * Provides controlled access to service functionality for:
 * - External applications via IPC
 * - UI components and overlays
 * - Testing and mocking
 *
 * Implementation: VoiceOSService
 */
interface IVoiceOSServiceLocal {

    /**
     * Service lifecycle and state
     */

    /**
     * Check if service is ready for command processing
     * @return true if fully initialized and ready
     */
    fun isServiceReady(): Boolean

    /**
     * Check if service is currently running
     * @return true if service instance exists
     */
    fun isServiceRunning(): Boolean

    /**
     * Get current service status
     * @return Service status string (initializing, ready, listening, processing, error, stopped)
     */
    fun getStatus(): String

    /**
     * Observe service status changes
     * @return StateFlow of service status
     */
    fun observeStatus(): StateFlow<String>

    /**
     * Voice recognition control
     */

    /**
     * Start voice listening
     * Begins continuous voice recognition
     * @return true if started successfully
     */
    fun startListening(): Boolean

    /**
     * Stop voice listening
     * Stops continuous voice recognition
     * @return true if stopped successfully
     */
    fun stopListening(): Boolean

    /**
     * Check if currently listening for voice input
     * @return true if actively listening
     */
    fun isListening(): Boolean

    /**
     * Command execution
     */

    /**
     * Execute voice command
     *
     * @param commandText Command text to execute
     * @return true if command was executed successfully
     */
    fun executeCommand(commandText: String): Boolean

    /**
     * Execute command with context
     *
     * @param command Command object with metadata
     * @return true if command was executed successfully
     */
    fun executeCommand(command: Command): Boolean

    /**
     * Check if command is currently being processed
     * @return true if command processing is in progress
     */
    fun isCommandProcessing(): Boolean

    /**
     * Accessibility node access
     */

    /**
     * Get root accessibility node of active window
     * @return Root node or null if no active window
     */
    fun getRootNodeInActiveWindow(): AccessibilityNodeInfo?

    /**
     * Perform global accessibility action
     * @param action Global action constant from AccessibilityService
     * @return true if action was performed
     */
    fun performGlobalAction(action: Int): Boolean

    /**
     * Command and app information
     */

    /**
     * Get all available voice commands
     * @return List of command strings
     */
    fun getAvailableCommands(): List<String>

    /**
     * Get app launch commands
     * @return Map of command strings to package names
     */
    fun getAppCommands(): Map<String, String>

    /**
     * Get command for launching specific app
     * @param packageName Package name of app
     * @return Command string or null if not found
     */
    fun getCommandForApp(packageName: String): String?

    /**
     * Configuration
     */

    /**
     * Update service configuration
     * @param config Configuration data
     */
    fun updateConfiguration(config: Map<String, Any>)

    /**
     * Get current configuration
     * @return Configuration data
     */
    fun getConfiguration(): Map<String, Any>

    /**
     * Feedback and notifications
     */

    /**
     * Speak text using TTS
     * @param text Text to speak
     * @param priority Speech priority (0=immediate, 1=high, 2=normal, 3=low)
     */
    fun speak(text: String, priority: Int = 2)

    /**
     * Show toast message
     * @param message Message to display
     */
    fun showToast(message: String)

    /**
     * Vibrate device
     * @param duration Duration in milliseconds
     */
    fun vibrate(duration: Long)

    /**
     * Statistics and monitoring
     */

    /**
     * Get service statistics
     * @return Map of statistic keys to values
     */
    fun getStatistics(): Map<String, Any>

    /**
     * Reset service statistics
     */
    fun resetStatistics()

    /**
     * Get resource usage information
     * @return Map of resource metrics
     */
    fun getResourceUsage(): Map<String, Any>

    /**
     * Health check
     */

    /**
     * Perform service health check
     * @return Health status (healthy, degraded, unhealthy)
     */
    fun checkHealth(): HealthStatus

    /**
     * Service health status
     */
    data class HealthStatus(
        val status: Status,
        val message: String,
        val details: Map<String, Any> = emptyMap()
    ) {
        enum class Status {
            HEALTHY,    // All systems operational
            DEGRADED,   // Some issues but functional
            UNHEALTHY   // Critical issues
        }

        fun isHealthy(): Boolean = status == Status.HEALTHY
    }

    /**
     * Service state constants
     */
    companion object {
        const val STATE_INITIALIZING = "initializing"
        const val STATE_READY = "ready"
        const val STATE_LISTENING = "listening"
        const val STATE_PROCESSING = "processing"
        const val STATE_ERROR = "error"
        const val STATE_STOPPED = "stopped"
    }
}
