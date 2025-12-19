/**
 * ServiceLifecycleManager.kt - Service lifecycle event management
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-17
 *
 * Responsibility: Manages AccessibilityService lifecycle events (onServiceConnected, onAccessibilityEvent, onInterrupt, onDestroy)
 */
package com.augmentalis.voiceoscore.accessibility.managers

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.augmentalis.voiceoscore.accessibility.config.ServiceConfiguration
import com.augmentalis.voiceoscore.accessibility.speech.SpeechConfigurationData
import com.augmentalis.voiceoscore.accessibility.speech.SpeechEngineManager
import com.augmentalis.voiceoscore.accessibility.utils.Const
import com.augmentalis.voiceoscore.accessibility.utils.Debouncer
import com.augmentalis.voiceoscore.accessibility.utils.EventPriorityManager
import com.augmentalis.voiceoscore.accessibility.utils.ResourceMonitor
import com.augmentalis.voiceos.constants.VoiceOSConstants
import com.augmentalis.speechrecognition.SpeechMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Manages service lifecycle, accessibility events, and foreground service state
 */
class ServiceLifecycleManager(
    private val service: AccessibilityService,
    private val context: Context,
    private val speechEngineManager: SpeechEngineManager?,
    private val onEventReceived: (AccessibilityEvent) -> Unit,
    private val onServiceReady: () -> Unit
) : DefaultLifecycleObserver {

    companion object {
        private const val TAG = "ServiceLifecycleManager"
        private const val ACTION_START_MIC = "com.augmentalis.voiceos.START_MIC"
        private const val ACTION_STOP_MIC = "com.augmentalis.voiceos.STOP_MIC"
        private const val MAX_QUEUED_EVENTS = 50
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Configuration
    private lateinit var config: ServiceConfiguration

    // Service state
    @Volatile
    var isServiceReady = false
        private set

    // Hybrid foreground service state
    private var foregroundServiceActive = false
    private var appInBackground = false
    private var voiceSessionActive = false

    // Event debouncing
    private val eventDebouncer = Debouncer(VoiceOSConstants.Timing.EVENT_DEBOUNCE_MS)

    // Event queue for initialization window
    private val pendingEvents = ConcurrentLinkedQueue<AccessibilityEvent>()

    // Resource monitoring
    private val resourceMonitor by lazy {
        ResourceMonitor(context).also {
            Log.d(TAG, "ResourceMonitor initialized (lazy)")
        }
    }

    // Event priority management
    private val eventPriorityManager by lazy {
        EventPriorityManager().also {
            Log.d(TAG, "EventPriorityManager initialized (lazy)")
        }
    }

    // Broadcast receiver for configuration updates
    private val serviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.i(TAG, "Configuration update received")
            if (intent?.action == Const.ACTION_CONFIG_UPDATE) {
                config = ServiceConfiguration.loadFromPreferences(this@ServiceLifecycleManager.context)
                Log.i(TAG, "Configuration reloaded: $config")
                speechEngineManager?.updateConfiguration(
                    SpeechConfigurationData(
                        language = config.voiceLanguage,
                        mode = SpeechMode.DYNAMIC_COMMAND,
                        enableVAD = true,
                        confidenceThreshold = 4000F,
                        maxRecordingDuration = 30000,
                        timeoutDuration = 5000,
                        enableProfanityFilter = false
                    )
                )
            }
        }
    }

    /**
     * Called when accessibility service is connected
     */
    fun onServiceConnected() {
        Log.i(TAG, "Accessibility service connected")

        // Initialize configuration
        config = ServiceConfiguration.loadFromPreferences(context)

        // Configure service capabilities
        configureServiceInfo()

        // Register for app lifecycle events
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        // Register broadcast receiver for configuration updates
        val filter = IntentFilter(Const.ACTION_CONFIG_UPDATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.i(TAG, "Registering receiver for configuration updates (API 33+)")
            context.registerReceiver(serviceReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            Log.i(TAG, "Registering receiver for configuration updates")
            context.registerReceiver(serviceReceiver, filter)
        }

        // Mark service as ready
        isServiceReady = true
        onServiceReady()

        Log.i(TAG, "Service lifecycle initialized successfully")
    }

    /**
     * Configure accessibility service info
     */
    private fun configureServiceInfo() {
        try {
            service.serviceInfo?.let { info ->
                // Configure service capabilities
                info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
                info.flags = info.flags or
                        AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                        AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE or
                        AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS

                // Add accessibility button support on Android O+
                info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON

                // Add fingerprint gesture support if enabled
                if (config.fingerprintGesturesEnabled) {
                    info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FINGERPRINT_GESTURES
                }

                // Verify FLAG_RETRIEVE_INTERACTIVE_WINDOWS is set
                val hasInteractiveWindowsFlag = (info.flags and AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS) != 0
                Log.i(TAG, "Service configured - FLAG_RETRIEVE_INTERACTIVE_WINDOWS: $hasInteractiveWindowsFlag")

                if (!hasInteractiveWindowsFlag) {
                    Log.e(TAG, "CRITICAL: FLAG_RETRIEVE_INTERACTIVE_WINDOWS not set! Windows will be unavailable!")
                }

                Log.d(TAG, "Service info configured")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure service info", e)
        }
    }

    /**
     * Handle accessibility event
     */
    fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isServiceReady || event == null) return

        // Adaptive event filtering based on memory pressure
        val isLowResource = resourceMonitor.isLowResourceMode.value
        val eventPriority = eventPriorityManager.getPriorityForEvent(event.eventType)
        val shouldProcess = !isLowResource || eventPriority >= EventPriorityManager.PRIORITY_HIGH

        if (!shouldProcess) {
            Log.v(TAG, "Event filtered due to memory pressure: type=${event.eventType}, priority=$eventPriority")
            return
        }

        // Get package name
        var packageName = event.packageName?.toString()
        val currentPackage = service.rootInActiveWindow?.packageName?.toString()

        // Handle cases where packageName might be null
        if (packageName == null && currentPackage != null) {
            val isRedundantWindowChange = isRedundantWindowChange(event)

            if (isRedundantWindowChange) {
                packageName = currentPackage
            } else {
                return
            }
        }

        if (packageName == null) return

        // Create debounce key
        val debounceKey = "$packageName-${event.className?.toString() ?: "unknown"}-${event.eventType}"

        // Apply debouncing
        if (!eventDebouncer.shouldProceed(debounceKey)) {
            Log.v(TAG, "Event debounced for: $debounceKey")
            return
        }

        // Forward event to handler
        try {
            onEventReceived(event)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling accessibility event: ${event.eventType}", e)
        }
    }

    /**
     * Check if window change event is redundant
     */
    private fun isRedundantWindowChange(event: AccessibilityEvent): Boolean {
        return event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
                event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
    }

    /**
     * Queue event for later processing
     */
    fun queueEvent(event: AccessibilityEvent) {
        if (pendingEvents.size < MAX_QUEUED_EVENTS) {
            val eventCopy = AccessibilityEvent.obtain(event)
            pendingEvents.offer(eventCopy)
            Log.d(TAG, "Queued event (type=${event.eventType}, queue size=${pendingEvents.size})")
        } else {
            Log.w(TAG, "Event queue full ($MAX_QUEUED_EVENTS), dropping event")
        }
    }

    /**
     * Process all queued events
     */
    fun processQueuedEvents() {
        val queueSize = pendingEvents.size
        if (queueSize > 0) {
            Log.i(TAG, "Processing $queueSize queued events")
            var processedCount = 0

            while (pendingEvents.isNotEmpty()) {
                val queuedEvent = pendingEvents.poll()
                if (queuedEvent != null) {
                    try {
                        onEventReceived(queuedEvent)
                        processedCount++
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing queued event", e)
                    } finally {
                        queuedEvent.recycle()
                    }
                }
            }

            Log.i(TAG, "Processed $processedCount queued events")
        }
    }

    /**
     * Handle service interrupt
     */
    fun onInterrupt() {
        Log.w(TAG, "Service interrupted")
    }

    /**
     * Lifecycle observer: App moved to foreground
     */
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        Log.d(TAG, "App moved to foreground")
        appInBackground = false
        evaluateForegroundServiceNeed()
    }

    /**
     * Lifecycle observer: App moved to background
     */
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        Log.d(TAG, "App moved to background")
        appInBackground = true
        evaluateForegroundServiceNeed()
    }

    /**
     * Evaluate whether foreground service is needed
     */
    private fun evaluateForegroundServiceNeed() {
        val needsForeground = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && appInBackground
                && voiceSessionActive
                && !foregroundServiceActive

        val shouldStopForeground = foregroundServiceActive && (!appInBackground || !voiceSessionActive)

        when {
            needsForeground -> {
                Log.d(TAG, "Starting ForegroundService (Android 12+ background requirement)")
                startForegroundService()
            }
            shouldStopForeground -> {
                Log.d(TAG, "Stopping ForegroundService (no longer needed)")
                stopForegroundService()
            }
            else -> {
                Log.v(TAG, "ForegroundService state: needed=$needsForeground, active=$foregroundServiceActive")
            }
        }
    }

    /**
     * Start foreground service
     */
    private fun startForegroundService() {
        if (foregroundServiceActive) return

        try {
            val intent = Intent(context, com.augmentalis.voiceoscore.accessibility.VoiceOnSentry::class.java).apply {
                action = ACTION_START_MIC
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            foregroundServiceActive = true
            Log.i(TAG, "ForegroundService started for background mic access")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ForegroundService", e)
            foregroundServiceActive = false
        }
    }

    /**
     * Stop foreground service
     */
    private fun stopForegroundService() {
        if (!foregroundServiceActive) return

        try {
            val intent = Intent(context, com.augmentalis.voiceoscore.accessibility.VoiceOnSentry::class.java).apply {
                action = ACTION_STOP_MIC
            }
            context.stopService(intent)

            foregroundServiceActive = false
            Log.i(TAG, "ForegroundService stopped (no longer needed)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop ForegroundService", e)
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        try {
            Log.d(TAG, "Cleaning up ServiceLifecycleManager...")

            // Unregister lifecycle observer
            ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
            Log.i(TAG, "ProcessLifecycleOwner observer unregistered")

            // Unregister broadcast receiver
            try {
                context.unregisterReceiver(serviceReceiver)
                Log.i(TAG, "Broadcast receiver unregistered")
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering receiver", e)
            }

            // Clear event debouncer
            eventDebouncer.clearAll()

            // Cancel coroutine scope
            scope.cancel()

            isServiceReady = false

            Log.i(TAG, "ServiceLifecycleManager cleaned up successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up ServiceLifecycleManager", e)
        }
    }
}
