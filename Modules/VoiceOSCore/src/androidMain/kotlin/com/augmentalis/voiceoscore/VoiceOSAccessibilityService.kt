@file:Suppress("DEPRECATION") // AccessibilityEvent.obtain() deprecated in API 34+ but needed for compatibility

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
import kotlinx.coroutines.Job

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

    // NAV-500 Fix #1: Event debouncing to prevent excessive processing
    private var lastEventProcessTime = 0L
    private var pendingScreenChangeJob: Job? = null
    private var currentPackageName: String? = null

    // Scroll-triggered overlay refresh: debounced screen refresh after scroll settles
    private var pendingScrollRefreshJob: Job? = null
    private val scrollRefreshDebounceMs = 300L

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
     * Get the BoundsResolver instance for scroll offset tracking.
     * App-level service should provide this for accurate click coordinates.
     * NAV-500 Fix #2: Required for scroll offset tracking.
     */
    protected open fun getBoundsResolver(): BoundsResolver? = null

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
        try {
            // Cancel all pending coroutines to prevent task leaks
            serviceScope.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling service scope: ${e.message}")
        } finally {
            isServiceReady = false
        }
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
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // Window change = new screen, process immediately
                handleScreenChangeDebounced(safeEvent, forceImmediate = true)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // NAV-500 Fix #1: Debounce content changes to prevent excessive processing
                // on rapidly updating screens like Device Info app
                handleScreenChangeDebounced(safeEvent, forceImmediate = false)
            }
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                // NAV-500 Fix #2: Track scroll events for accurate click coordinates
                handleScrollEvent(safeEvent)
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                // Refresh after clicks for dynamic content
                handleScreenChangeDebounced(safeEvent, forceImmediate = false)
            }
        }
    }

    /**
     * NAV-500 Fix #1: Debounced screen change handler.
     *
     * For rapidly changing screens (like Device Info app), this prevents
     * excessive processing by debouncing content change events.
     *
     * @param event The accessibility event
     * @param forceImmediate If true, bypasses debouncing (for window state changes)
     */
    private fun handleScreenChangeDebounced(event: AccessibilityEvent, forceImmediate: Boolean) {
        val now = System.currentTimeMillis()
        val debounceMs = DeviceCapabilityManager.getContentDebounceMs()

        // Check for package change - notify BoundsResolver to clear stale scroll offsets
        val packageName = event.packageName?.toString()
        if (packageName != null && packageName != currentPackageName) {
            currentPackageName = packageName
            getBoundsResolver()?.onPackageChanged(packageName)

            // Force re-scan for new app by invalidating screen fingerprint.
            // DO NOT call clearDynamicCommands() here — it creates a race condition:
            //   clear (sync) → handleScreenChange (async coroutine) → voice command
            //   arrives between clear and update → sees registry size: 0.
            // CommandRegistry.update() atomically replaces all commands (never merges),
            // so explicit clearing is unnecessary. Stale commands during the brief scan
            // window (~100-500ms) are strictly better UX than "no commands available".
            lastScreenHash = ""

            // Package change = always process immediately
            handleScreenChange(event)
            lastEventProcessTime = now
            return
        }

        // For window state changes, process immediately
        if (forceImmediate) {
            pendingScreenChangeJob?.cancel()
            handleScreenChange(event)
            lastEventProcessTime = now
            return
        }

        // Debounce: skip if too recent
        if (now - lastEventProcessTime < debounceMs) {
            // Cancel any pending job and schedule a new one
            pendingScreenChangeJob?.cancel()
            pendingScreenChangeJob = serviceScope.launch {
                kotlinx.coroutines.delay(debounceMs)
                handleScreenChange(event)
                lastEventProcessTime = System.currentTimeMillis()
            }
            return
        }

        // Process immediately and update timestamp
        pendingScreenChangeJob?.cancel()
        handleScreenChange(event)
        lastEventProcessTime = now
    }

    /**
     * NAV-500 Fix #2: Handle scroll events to track scroll offsets.
     *
     * When a scrollable container scrolls, we update BoundsResolver with
     * the new scroll position so click coordinates can be adjusted.
     *
     * Also schedules a debounced screen refresh so overlay badges update
     * after scroll settles. Without this, TYPE_VIEW_SCROLLED alone never
     * triggers processScreen — new content loaded by RecyclerView would
     * only update if TYPE_WINDOW_CONTENT_CHANGED fires (which may be
     * debounced away or not fire reliably for all scroll positions).
     */
    private fun handleScrollEvent(event: AccessibilityEvent) {
        val boundsResolver = getBoundsResolver() ?: return

        // Extract scroll information from the event
        val source = event.source ?: return
        try {
            val resourceId = source.viewIdResourceName ?: ""
            val scrollX = event.scrollX
            val scrollY = event.scrollY

            // Only update if we have valid scroll position
            if (scrollX >= 0 || scrollY >= 0) {
                boundsResolver.updateScrollOffset(resourceId, scrollX, scrollY)
                Log.v(TAG, "Scroll event: $resourceId -> ($scrollX, $scrollY)")
            }
        } finally {
            @Suppress("DEPRECATION")
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                source.recycle()
            }
        }

        // Schedule debounced overlay refresh after scroll settles.
        // Cancel-and-restart pattern: only the final scroll position triggers a refresh.
        pendingScrollRefreshJob?.cancel()
        pendingScrollRefreshJob = serviceScope.launch {
            kotlinx.coroutines.delay(scrollRefreshDebounceMs)
            handleScreenChange(event)
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

                // Update ActionCoordinator — source-tagged to preserve web commands
                val coordinator = getActionCoordinator()
                coordinator.updateDynamicCommandsBySource("accessibility", commands)

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
            // NAV-500 Fix #2: Added TYPE_VIEW_SCROLLED for scroll tracking
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_SCROLLED

            // Get all windows for multi-window support
            flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS

            // Monitor all packages (can be restricted in app-level service)
            packageNames = null

            // Feedback type
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC

            // NAV-500 Fix #1: Increase notification timeout based on device speed
            // to reduce event cascade on rapidly changing screens
            notificationTimeout = DeviceCapabilityManager.getContentDebounceMs()
        }

        Log.d(TAG, "Service info configured with debounce: ${DeviceCapabilityManager.getContentDebounceMs()}ms")
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

    // =========================================================================
    // Developer settings API
    // =========================================================================

    /**
     * Set a custom debounce value for accessibility events.
     * NAV-500 Fix #1: Developer setting for tuning event processing rate.
     *
     * @param debounceMs Custom debounce in milliseconds (50-1000ms recommended)
     *                   Pass null to reset to auto-detected device-appropriate value
     */
    fun setDebounceMs(debounceMs: Long?) {
        DeviceCapabilityManager.setUserDebounceMs(debounceMs)
        Log.d(TAG, "Debounce set to: ${debounceMs ?: "auto (${DeviceCapabilityManager.getContentDebounceMs()}ms)"}")

        // Update notification timeout if debounce changed
        serviceInfo = serviceInfo?.apply {
            notificationTimeout = DeviceCapabilityManager.getContentDebounceMs()
        }
    }

    /**
     * Get current debounce value in milliseconds.
     *
     * @return Current debounce value (user override or auto-detected)
     */
    fun getDebounceMs(): Long = DeviceCapabilityManager.getContentDebounceMs()

    /**
     * Get the current device speed classification.
     */
    fun getDeviceSpeed(): DeviceCapabilityManager.DeviceSpeed = DeviceCapabilityManager.getDeviceSpeed()

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
