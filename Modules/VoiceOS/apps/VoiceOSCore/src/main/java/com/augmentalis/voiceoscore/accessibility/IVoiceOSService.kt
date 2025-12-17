/**
 * IVoiceOSService.kt - Interface for VoiceOSService implementations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-15
 *
 * This interface defines the contract that all VoiceOSService implementations must follow.
 * Used by the wrapper pattern to support both legacy and refactored implementations.
 */
package com.augmentalis.voiceoscore.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.augmentalis.voiceos.cursor.core.CursorOffset

/**
 * Interface for VoiceOSService implementations
 *
 * Defines the public API that must be supported by both legacy and refactored
 * implementations. This enables the wrapper to transparently switch between
 * implementations while maintaining functional equivalence.
 *
 * Method Categories:
 * 1. Service Lifecycle: onCreate, onServiceConnected, onDestroy
 * 2. Accessibility Events: onAccessibilityEvent, onInterrupt
 * 3. Cursor Control: showCursor, hideCursor, toggleCursor, centerCursor, clickCursor
 * 4. State Queries: isServiceRunning, getCursorPosition, isCursorVisible
 * 5. Command Handling: executeCommand (static), onNewCommandsGenerated
 * 6. Fallback Management: enableFallbackMode
 */
interface IVoiceOSService {

    /**
     * Called when the service is first created
     * Must initialize all core components
     */
    fun onCreate()

    /**
     * Called when the service is connected to the accessibility framework
     * Must configure service info and start component initialization
     */
    fun onServiceConnected()

    /**
     * Called when an accessibility event occurs
     * @param event The accessibility event to process
     */
    fun onAccessibilityEvent(event: AccessibilityEvent?)

    /**
     * Called when the service is interrupted
     */
    fun onInterrupt()

    /**
     * Called when the service is destroyed
     * Must cleanup all resources and cancel coroutines
     */
    fun onDestroy()

    /**
     * Show the voice cursor overlay
     * @return true if successful, false otherwise
     */
    fun showCursor(): Boolean

    /**
     * Hide the voice cursor overlay
     * @return true if successful, false otherwise
     */
    fun hideCursor(): Boolean

    /**
     * Toggle cursor visibility
     * @return true if successful, false otherwise
     */
    fun toggleCursor(): Boolean

    /**
     * Center cursor on screen
     * @return true if successful, false otherwise
     */
    fun centerCursor(): Boolean

    /**
     * Perform click at current cursor position
     * @return true if successful, false otherwise
     */
    fun clickCursor(): Boolean

    /**
     * Get current cursor position
     * @return CursorOffset with current X,Y coordinates
     */
    fun getCursorPosition(): CursorOffset

    /**
     * Check if cursor is currently visible
     * @return true if cursor is visible, false otherwise
     */
    fun isCursorVisible(): Boolean

    /**
     * Called when new commands are generated (e.g., after app scraping)
     * Triggers re-registration of database commands with speech engine
     */
    fun onNewCommandsGenerated()

    /**
     * Enable fallback mode when CommandManager is unavailable
     * Called by ServiceMonitor during graceful degradation
     */
    fun enableFallbackMode()

    /**
     * Get installed app commands
     * @return Map of app command names to package names
     */
    fun getAppCommands(): Map<String, String>

    companion object {
        /**
         * Check if service is currently running
         * @return true if service instance exists, false otherwise
         */
        @JvmStatic
        fun isServiceRunning(): Boolean {
            // Stub implementation - will be provided by VoiceOSService
            return getInstance() != null
        }

        /**
         * Execute a voice command via the service
         * @param commandText The command text to execute
         * @return true if command was executed, false otherwise
         */
        @JvmStatic
        fun executeCommand(commandText: String): Boolean {
            // Stub implementation - will be provided by VoiceOSService
            return false
        }

        /**
         * Get the current service instance
         * @return Current service instance or null if not available
         */
        @JvmStatic
        fun getInstance(): AccessibilityService? {
            // Stub implementation - will be injected by VoiceOSService
            return null
        }
    }
}
