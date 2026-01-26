/**
 * VoiceOSAccessibilityService.kt - Main Android Accessibility Service
 *
 * Thin wrapper that receives accessibility events and delegates to
 * KMP commonMain components (CommandGenerator, ActionCoordinator).
 *
 * This replaces the 3077-line God class VoiceOSService.kt from LEGACY
 * by delegating most logic to existing MASTER commonMain code.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-19
 */
package com.augmentalis.voiceoscore

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * VoiceOS Accessibility Service - Entry point for accessibility events on Android.
 *
 * Responsibilities (thin wrapper only):
 * 1. Receive accessibility events from Android
 * 2. Extract UI elements using AndroidScreenExtractor
 * 3. Generate commands using CommandGenerator (commonMain)
 * 4. Update ActionCoordinator (commonMain) with commands
 * 5. Execute gestures using AndroidGestureDispatcher
 *
 * All business logic is delegated to KMP commonMain components.
 */
abstract class VoiceOSAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "VoiceOSAccessibility"
    }

    // Coroutine scope for async operations
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Core components
    private lateinit var screenExtractor: AndroidScreenExtractor
    private lateinit var gestureDispatcher: AndroidGestureDispatcher

    // Fingerprinter for screen deduplication
    private val screenFingerprinter = ScreenFingerprinter()
    private var lastScreenHash: String = ""

    // Service state
    private var isServiceReady = false

    // =========================================================================
    // Abstract methods - to be implemented by app-level service
    // =========================================================================

    /**
     * Get the ActionCoordinator instance.
     * App-level service should provide this with proper NLU/LLM processors.
     */
    protected abstract fun getActionCoordinator(): ActionCoordinator

    /**
     * Called when service is ready to receive events.
     * App-level service can initialize additional components here.
     */
    protected open fun onServiceReady() {}

    /**
     * Called when commands are updated.
     * App-level service can update UI, speech engine, etc.
     */
    protected open fun onCommandsUpdated(commands: List<QuantizedCommand>) {}

    /**
     * Called when a command is executed.
     * App-level service can provide feedback (TTS, haptic, etc.)
     */
    protected open fun onCommandExecuted(command: QuantizedCommand, success: Boolean) {}

    // =========================================================================
    // AccessibilityService lifecycle
    // =========================================================================

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Service connected")

        // Configure service info
        configureServiceInfo()

        // Initialize components
        screenExtractor = AndroidScreenExtractor()
        gestureDispatcher = AndroidGestureDispatcher(this)

        isServiceReady = true
        Log.i(TAG, "Service ready")

        onServiceReady()
    }

    override fun onDestroy() {
        Log.i(TAG, "Service destroying")
        serviceScope.cancel()
        isServiceReady = false
        super.onDestroy()
    }

    override fun onInterrupt() {
        Log.w(TAG, "Service interrupted")
    }

    // =========================================================================
    // Accessibility event handling
    // =========================================================================

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // BUG FIX: Use safe reference to avoid potential NPE in edge cases
        val safeEvent = event ?: return
        if (!isServiceReady) return

        when (safeEvent.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handleScreenChange(safeEvent)
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                // Optionally refresh after clicks for dynamic content
                handleScreenChange(safeEvent)
            }
        }
    }

    /**
     * Handle screen changes by extracting elements and updating commands.
     */
    private fun handleScreenChange(event: AccessibilityEvent) {
        serviceScope.launch(Dispatchers.Default) {
            try {
                val root = rootInActiveWindow ?: return@launch
                val packageName = event.packageName?.toString() ?: root.packageName?.toString() ?: return@launch

                // Extract elements
                val elements = screenExtractor.extract(root)

                if (elements.isEmpty()) {
                    Log.d(TAG, "No elements extracted for $packageName")
                    return@launch
                }

                // Check if screen changed using fingerprint
                val screenHash = screenFingerprinter.calculateFingerprint(elements)
                if (screenHash == lastScreenHash) {
                    Log.v(TAG, "Screen unchanged, skipping command update")
                    return@launch
                }
                lastScreenHash = screenHash

                // Generate commands from elements
                val commands = generateCommands(elements, packageName)

                Log.d(TAG, "Generated ${commands.size} commands for $packageName")

                // Update ActionCoordinator
                val coordinator = getActionCoordinator()
                coordinator.updateDynamicCommands(commands)

                // Notify app-level service
                onCommandsUpdated(commands)

            } catch (e: Exception) {
                Log.e(TAG, "Error handling screen change: ${e.message}", e)
            }
        }
    }

    /**
     * Generate commands from extracted elements.
     */
    private fun generateCommands(elements: List<ElementInfo>, packageName: String): List<QuantizedCommand> {
        val commands = mutableListOf<QuantizedCommand>()

        // Generate element-based commands
        elements.forEach { element ->
            CommandGenerator.fromElement(element, packageName)?.let { cmd ->
                commands.add(cmd)
            }
        }

        // Generate list index commands (for lists)
        val listElements = elements.filter { it.listIndex >= 0 }
        if (listElements.isNotEmpty()) {
            commands.addAll(CommandGenerator.generateListIndexCommands(listElements, packageName))
        }

        return commands
    }

    // =========================================================================
    // Service configuration
    // =========================================================================

    private fun configureServiceInfo() {
        serviceInfo = serviceInfo?.apply {
            // Monitor all event types we care about
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED

            // Get all windows for multi-window support
            flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS

            // Monitor all packages (can be restricted in app-level service)
            packageNames = null

            // Feedback type
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC

            // Reasonable notification timeout
            notificationTimeout = 100
        }

        Log.d(TAG, "Service info configured")
    }

    // =========================================================================
    // Public API for app-level service
    // =========================================================================

    /**
     * Get the gesture dispatcher for executing actions.
     */
    fun getGestureDispatcher(): AndroidGestureDispatcher = gestureDispatcher

    /**
     * Get the screen extractor for manual extraction.
     */
    fun getScreenExtractor(): AndroidScreenExtractor = screenExtractor

    /**
     * Force a screen refresh.
     */
    fun refreshScreen() {
        lastScreenHash = "" // Clear cache to force update
        rootInActiveWindow?.let { root ->
            handleScreenChange(AccessibilityEvent.obtain().apply {
                eventType = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                packageName = root.packageName
            })
        }
    }

    /**
     * Process a voice command.
     * Delegates to ActionCoordinator which handles the full priority chain.
     *
     * @param utterance The voice input text
     * @param confidence Speech recognition confidence (0.0-1.0)
     */
    fun processVoiceCommand(utterance: String, confidence: Float = 1.0f) {
        if (confidence < 0.5f) {
            Log.d(TAG, "Command rejected: confidence too low ($confidence)")
            return
        }

        serviceScope.launch {
            val coordinator = getActionCoordinator()
            val result = coordinator.processVoiceCommand(utterance, confidence)

            // Handle the result
            when (result) {
                is HandlerResult.Success -> {
                    Log.d(TAG, "Command executed: ${result.message}")
                    // Create a placeholder command for the callback
                    val cmd = QuantizedCommand(
                        phrase = utterance,
                        actionType = CommandActionType.EXECUTE,
                        targetAvid = null,
                        confidence = confidence
                    )
                    onCommandExecuted(cmd, true)
                }
                is HandlerResult.Failure -> {
                    Log.w(TAG, "Command failed: ${result.reason}")
                    val cmd = QuantizedCommand(
                        phrase = utterance,
                        actionType = CommandActionType.EXECUTE,
                        targetAvid = null,
                        confidence = confidence
                    )
                    onCommandExecuted(cmd, false)
                }
                is HandlerResult.NotHandled -> {
                    Log.w(TAG, "No matching command for: $utterance")
                }
                is HandlerResult.AwaitingSelection -> {
                    Log.d(TAG, "Awaiting selection: ${result.message}")
                    // UI layer should handle disambiguation
                }
                else -> {
                    Log.d(TAG, "Command result: $result")
                }
            }
        }
    }
}
