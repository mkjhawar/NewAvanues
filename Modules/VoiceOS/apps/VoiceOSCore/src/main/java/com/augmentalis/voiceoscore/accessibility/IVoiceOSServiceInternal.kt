/**
 * IVoiceOSServiceInternal.kt - Internal service interface for component coordination
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-16
 * Updated: 2025-12-22
 *
 * Purpose: Internal API for service components (managers, handlers) to coordinate with VoiceOSService
 * Provides access to internal service functionality not exposed in public API
 *
 * VOS4 Exception: Interface justified for internal component coordination
 * - Separates internal coordination contract from public API
 * - Enables testing of internal components with mock service
 * - Maintains clear boundaries between public and internal APIs
 */
package com.augmentalis.voiceoscore.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.commandmanager.CommandManager
import com.augmentalis.voiceoscore.accessibility.extractors.UIScrapingEngine
import com.augmentalis.voiceoscore.accessibility.managers.ActionCoordinator
import com.augmentalis.voiceoscore.accessibility.managers.DatabaseManager
import com.augmentalis.voiceoscore.accessibility.managers.IPCManager
import com.augmentalis.voiceoscore.accessibility.overlays.OverlayManager
import com.augmentalis.voiceoscore.accessibility.recognition.VoiceRecognitionManager
import com.augmentalis.voiceoscore.accessibility.speech.SpeechEngineManager
import kotlinx.coroutines.CoroutineScope

/**
 * Internal interface for VoiceOS service component coordination
 *
 * Used by internal service components:
 * - Managers (DatabaseManager, IPCManager, etc.)
 * - Handlers (ActionHandler implementations)
 * - Recognition and speech engines
 * - Overlay components
 * - Integration components (LearnAppIntegration, JustInTimeLearner)
 *
 * NOT intended for external use - see IVoiceOSService for public API
 *
 * Extends IVoiceOSContext to inherit context access methods
 *
 * Implementation: VoiceOSService
 */
interface IVoiceOSServiceInternal : IVoiceOSContext {

    /**
     * Service context and resources
     */

    /**
     * Get application context
     * @return Application context
     */
    fun getApplicationContext(): Context

    // Note: getAccessibilityService() and getWindowManager() are inherited from IVoiceOSContext as properties
    // The property getters satisfy these method requirements

    /**
     * Get service coroutine scope
     * Primary scope for service operations (Dispatchers.Default)
     * @return Service-level CoroutineScope
     */
    fun getServiceScope(): CoroutineScope

    /**
     * Get command processing scope
     * Dedicated scope for I/O-heavy command operations (Dispatchers.IO)
     * @return Command processing CoroutineScope
     */
    fun getCommandScope(): CoroutineScope

    /**
     * Core service components
     */

    /**
     * Get command manager
     * Manages command registration and lookup
     * @return CommandManager instance
     */
    fun getCommandManager(): CommandManager

    /**
     * Get database manager
     * Handles all database operations
     * @return DatabaseManager instance
     *
     * Overrides IDatabaseContext.getDatabaseManager()
     */
    override fun getDatabaseManager(): DatabaseManager

    /**
     * Get IPC manager
     * Handles inter-process communication
     * @return IPCManager instance
     */
    fun getIPCManager(): IPCManager

    /**
     * Speech engine manager
     * Manages TTS and speech synthesis
     */
    val speechEngineManager: SpeechEngineManager

    /**
     * Voice recognition manager
     * Manages voice input and recognition (may be null)
     */
    val voiceRecognitionManager: VoiceRecognitionManager?

    /**
     * UI scraping engine
     * Extracts UI elements from accessibility tree
     */
    val uiScrapingEngine: UIScrapingEngine

    /**
     * Overlay manager
     * Manages all service overlays
     */
    val overlayManager: OverlayManager

    /**
     * Action coordinator
     * Coordinates complex multi-step actions
     */
    val actionCoordinator: ActionCoordinator

    /**
     * Service state and lifecycle
     */

    /**
     * Check if service is fully initialized
     * @return true if ready for operations
     */
    fun isInitialized(): Boolean

    /**
     * Set service initialization state
     * @param initialized true when initialization complete
     */
    fun setInitialized(initialized: Boolean)

    /**
     * Check if voice recognition is initialized
     * @return true if voice recognition ready
     */
    fun isVoiceInitialized(): Boolean

    /**
     * Set voice initialization state
     * @param initialized true when voice recognition ready
     */
    fun setVoiceInitialized(initialized: Boolean)

    /**
     * Request service restart
     * Triggers graceful restart of service
     */
    fun requestRestart()

    /**
     * Command lifecycle notifications
     */

    /**
     * Called when new commands have been generated or updated
     * VoiceOSService should refresh the speech recognition engine
     * with the updated command list
     *
     * Used by LearnAppIntegration and JustInTimeLearner
     */
    fun onNewCommandsGenerated()

    /**
     * Accessibility operations
     */

    /**
     * Get root node with retry logic
     * Retries if initial attempt fails
     *
     * @param maxRetries Maximum retry attempts
     * @param delayMs Delay between retries
     * @return Root node or null
     */
    fun getRootNodeWithRetry(maxRetries: Int = 3, delayMs: Long = 100): AccessibilityNodeInfo?

    /**
     * Find nodes matching criteria
     * Uses UIScrapingEngine for efficient searching
     *
     * @param predicate Filter predicate
     * @return List of matching nodes
     */
    fun findNodes(predicate: (UIScrapingEngine.UIElement) -> Boolean): List<UIScrapingEngine.UIElement>

    /**
     * Refresh UI element cache
     * Forces re-scraping of accessibility tree
     */
    fun refreshUICache()

    /**
     * Command cache management
     */

    /**
     * Get all cached commands
     * Includes static and dynamic commands
     * @return List of command strings
     */
    fun getCachedCommands(): List<String>

    /**
     * Get static commands only
     * System commands that don't change
     * @return List of static command strings
     */
    fun getStaticCommands(): List<String>

    /**
     * Get dynamic commands only
     * Context-specific commands that change
     * @return List of dynamic command strings
     */
    fun getDynamicCommands(): List<String>

    /**
     * Add dynamic command to cache
     * @param command Command string to add
     */
    fun addDynamicCommand(command: String)

    /**
     * Remove dynamic command from cache
     * @param command Command string to remove
     */
    fun removeDynamicCommand(command: String)

    /**
     * Clear all dynamic commands
     * Preserves static commands
     */
    fun clearDynamicCommands()

    /**
     * Reload command cache
     * Refreshes from database and managers
     */
    fun reloadCommands()

    /**
     * Event handling
     */

    /**
     * Queue accessibility event for processing
     * Uses priority queue for event ordering
     *
     * @param event AccessibilityEvent to process
     * @param priority Event priority
     */
    fun queueAccessibilityEvent(
        event: android.view.accessibility.AccessibilityEvent,
        priority: Int = 2
    )

    /**
     * Process queued events
     * Drains event queue in priority order
     */
    fun processQueuedEvents()

    /**
     * Notifications and feedback
     */

    /**
     * Update service notification
     * Updates foreground service notification
     *
     * @param title Notification title
     * @param message Notification message
     */
    fun updateNotification(title: String, message: String)

    /**
     * Show error notification
     * Displays error in notification area
     *
     * @param error Error message
     */
    fun showErrorNotification(error: String)

    /**
     * Send feedback to user
     * Uses appropriate feedback method (speech, vibration, toast)
     *
     * @param message Feedback message
     * @param type Feedback type (speech, vibration, toast, all)
     */
    fun sendFeedback(message: String, type: FeedbackType = FeedbackType.SPEECH)

    /**
     * Feedback type enum
     */
    enum class FeedbackType {
        SPEECH,     // TTS only
        VIBRATION,  // Haptic only
        TOAST,      // Toast message only
        ALL         // All feedback methods
    }

    /**
     * Configuration and settings
     */

    /**
     * Get configuration value
     * @param key Configuration key
     * @return Configuration value or null
     */
    fun getConfigValue(key: String): Any?

    /**
     * Set configuration value
     * @param key Configuration key
     * @param value Configuration value
     */
    fun setConfigValue(key: String, value: Any)

    /**
     * Apply configuration changes
     * Triggers reconfiguration of affected components
     */
    fun applyConfigChanges()

    /**
     * Monitoring and diagnostics
     */

    /**
     * Log service metric
     * Records metric for monitoring
     *
     * @param metric Metric name
     * @param value Metric value
     */
    fun logMetric(metric: String, value: Any)

    /**
     * Get service metrics
     * @return Map of metric names to values
     */
    fun getMetrics(): Map<String, Any>

    /**
     * Check resource health
     * @return true if resources within safe limits
     */
    fun checkResourceHealth(): Boolean

    /**
     * Request garbage collection
     * Use only when necessary (low memory situation)
     */
    fun requestGC()
}
