/**
 * VoiceOSService.kt - VoiceOS accessibility service implementation
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-09-02
 * Updated: 2025-09-03 - Streamlined naming, added hybrid foreground service
 * 
 * High-performance accessibility service with lazy loading, caching, and optimizations
 * Uses hybrid approach: ForegroundService only when needed (Android 12+ in background)
 */
package com.augmentalis.voiceos.accessibility

import android.accessibilityservice.AccessibilityService
import android.util.ArrayMap
import java.util.concurrent.atomic.AtomicLong
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceos.accessibility.config.ServiceConfiguration
import com.augmentalis.voiceos.accessibility.extractors.UIScrapingEngine
import com.augmentalis.voiceos.accessibility.handlers.*
import com.augmentalis.voiceos.accessibility.managers.*
import com.augmentalis.voiceos.accessibility.service.VoiceAccessibilityService
// UI components will be implemented later
import com.augmentalis.voiceos.speech.api.SpeechListenerManager
import com.augmentalis.voiceos.speech.api.RecognitionResult
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import android.os.Build
import android.content.Intent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * VoiceOS Service - Main accessibility service implementation
 * High-performance service with efficient data structures, lazy loading, and caching
 * Now with hybrid ForegroundService approach for optimal battery and memory usage
 */
class VoiceOSService : VoiceAccessibilityService(), DefaultLifecycleObserver {
    
    companion object {
        private const val TAG = "VoiceOSService"
        private const val CACHE_SIZE = 100
        private const val INIT_DELAY_MS = 200L
        
        // Mic service actions (foreground service for background mic access)
        private const val ACTION_START_MIC = "com.augmentalis.voiceos.START_MIC"
        private const val ACTION_STOP_MIC = "com.augmentalis.voiceos.STOP_MIC"
    }
    
    // Service state
    private var isServiceReady = false
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Hybrid foreground service state
    private var foregroundServiceActive = false
    private var appInBackground = false
    private var voiceSessionActive = false
    
    // Configuration
    private lateinit var config: ServiceConfiguration
    
    // TODO: UI components to be implemented later
    // private val floatingMenu by lazy { FloatingMenu(this) }
    // private val cursorOverlay by lazy { CursorOverlay(this) }
    
    // Event type tracking for performance monitoring
    private val eventCounts = ArrayMap<Int, AtomicLong>().apply {
        put(AccessibilityEvent.TYPE_VIEW_CLICKED, AtomicLong(0))
        put(AccessibilityEvent.TYPE_VIEW_FOCUSED, AtomicLong(0))
        put(AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED, AtomicLong(0))
        put(AccessibilityEvent.TYPE_VIEW_SCROLLED, AtomicLong(0))
        put(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED, AtomicLong(0))
        put(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED, AtomicLong(0))
    }
    
    // Optimized managers
    private val actionCoordinator by lazy {
        ActionCoordinator(this).also {
            Log.d(TAG, "ActionCoordinator initialized (lazy)")
        }
    }
    
    private val cursorManagerInstance by lazy {
        CursorManager(this).also {
            Log.d(TAG, "CursorManager initialized (lazy)")
        }
    }
    
    private val dynamicCommandGenerator by lazy {
        DynamicCommandGenerator(this).also {
            Log.d(TAG, "DynamicCommandGenerator initialized (lazy)")
        }
    }
    
    // Use optimized AppCommandManager
    private val appCommandManager by lazy {
        AppCommandManagerV2(this).also {
            Log.d(TAG, "AppCommandManagerV2 initialized (lazy)")
        }
    }
    
    // Use optimized UIScrapingEngine
    private val uiScrapingEngine by lazy {
        UIScrapingEngine(this).also {
            Log.d(TAG, "UIScrapingEngine initialized (lazy)")
        }
    }
    
    // Voice Recognition Integration
    private var speechListenerManager: SpeechListenerManager? = null
    private var isVoiceRecognitionActive = false
    private var voiceRecognitionEnabled = true
    
    // Optimized cache with ArrayMap
    private val commandCache = ArrayMap<String, Boolean>()
    private val nodeCache = ArrayMap<String, WeakReference<AccessibilityNodeInfo>>()
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "VoiceOS Service connected")
        
        // Initialize configuration
        config = ServiceConfiguration.loadFromPreferences(this)
        
        // Register for app lifecycle events for hybrid foreground service
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        
        // Start delayed initialization
        serviceScope.launch {
            delay(INIT_DELAY_MS) // Small delay to not block service startup
            initializeComponents()
        }
    }
    
    // Lifecycle observer methods for hybrid foreground service management
    override fun onStart(owner: LifecycleOwner) {
        super<DefaultLifecycleObserver>.onStart(owner)
        Log.d(TAG, "App moved to foreground")
        appInBackground = false
        evaluateForegroundServiceNeed()
    }
    
    override fun onStop(owner: LifecycleOwner) {
        super<DefaultLifecycleObserver>.onStop(owner)
        Log.d(TAG, "App moved to background")
        appInBackground = true
        evaluateForegroundServiceNeed()
    }
    
    /**
     * Initialize components with staggered loading
     */
    private suspend fun initializeComponents() = withContext(Dispatchers.Main) {
        try {
            // Initialize core components first
            actionCoordinator.initialize()
            delay(50)
            
            // Initialize managers
            appCommandManager.initialize()
            delay(50)
            
            dynamicCommandGenerator.initialize()
            delay(50)
            
            // TODO: Initialize UI components when implemented
            // if (config.isFloatingMenuEnabled()) {
            //     floatingMenu.show()
            // }
            // if (config.isCursorEnabled()) {
            //     cursorManagerInstance.initialize()
            // }
            
            // Initialize voice recognition if available
            initializeVoiceRecognition()
            
            isServiceReady = true
            Log.i(TAG, "All components initialized with optimization")
            
            // Log performance metrics
            logPerformanceMetrics()
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing components", e)
        }
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isServiceReady) return
        
        try {
            // Track event counts for performance monitoring
            event?.eventType?.let { eventCounts[it]?.incrementAndGet() }
            
            // Call parent implementation for standard handling
            event?.let { super.onAccessibilityEvent(it) }
            
            // Additional optimized processing
            event?.let { evt ->
                when (evt.eventType) {
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                    // Update UI scraping cache asynchronously
                    serviceScope.launch {
                        uiScrapingEngine.extractUIElementsAsync()
                    }
                }
                AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                    // Log click events for analytics
                    if (config.verboseLogging) {
                        Log.d(TAG, "Click event: ${evt.className}")
                    } else {
                        // No logging needed when verbose logging is disabled
                    }
                }
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    // Update app context
                    val packageName = evt.packageName?.toString()
                    if (packageName != null) {
                        serviceScope.launch {
                            appCommandManager.getAppCommands(packageName)
                        }
                    } else {
                        // Package name is null, cannot get app commands
                    }
                }
                else -> {
                    // Handle other event types or do nothing
                }
            }
            
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling event: ${event?.eventType}", e)
        }
    }
    
    /**
     * Initialize voice recognition with optimization
     */
    private fun initializeVoiceRecognition() {
        if (!voiceRecognitionEnabled) return
        
        try {
            speechListenerManager = SpeechListenerManager().apply {
                // TODO: Implement proper speech recognition callback
                // setOnSpeechResultListener { result ->
                //     handleVoiceCommand(result.text, result.confidence)
                // }
            }
            
            Log.d(TAG, "Voice recognition initialized")
            voiceSessionActive = true
            evaluateForegroundServiceNeed()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize voice recognition", e)
        }
    }
    
    /**
     * Evaluate whether ForegroundService is needed (hybrid approach)
     * Only starts ForegroundService on Android 12+ when app is in background with active voice
     */
    private fun evaluateForegroundServiceNeed() {
        val needsForeground = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            && appInBackground 
            && voiceSessionActive
            && !foregroundServiceActive
            
        val shouldStopForeground = foregroundServiceActive && 
            (!appInBackground || !voiceSessionActive)
            
        when {
            needsForeground -> {
                Log.d(TAG, "Starting ForegroundService (Android 12+ background requirement)")
                startForegroundServiceHelper()
            }
            shouldStopForeground -> {
                Log.d(TAG, "Stopping ForegroundService (no longer needed)")
                stopForegroundServiceHelper()
            }
            else -> {
                Log.v(TAG, "ForegroundService state: needed=$needsForeground, active=$foregroundServiceActive")
            }
        }
    }
    
    /**
     * Start the foreground service when needed
     */
    private fun startForegroundServiceHelper() {
        if (foregroundServiceActive) return
        
        try {
            val intent = Intent(this, VoiceOnSentry::class.java).apply {
                action = ACTION_START_MIC
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            
            foregroundServiceActive = true
            Log.i(TAG, "ForegroundService started for background mic access")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ForegroundService", e)
            foregroundServiceActive = false
        }
    }
    
    /**
     * Stop the foreground service when no longer needed
     */
    private fun stopForegroundServiceHelper() {
        if (!foregroundServiceActive) return
        
        try {
            val intent = Intent(this, VoiceOnSentry::class.java).apply {
                action = ACTION_STOP_MIC
            }
            stopService(intent)
            
            foregroundServiceActive = false
            Log.i(TAG, "ForegroundService stopped (no longer needed)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop ForegroundService", e)
        }
    }
    
    /**
     * Handle voice command with caching
     */
    private fun handleVoiceCommand(command: String, confidence: Float) {
        if (confidence < 0.5f) return
        
        val normalizedCommand = command.lowercase().trim()
        
        // Check cache first
        commandCache[normalizedCommand]?.let { cached ->
            if (cached) {
                executeCommand(normalizedCommand)
            }
            return
        }
        
        // Process new command
        serviceScope.launch {
            val isValid = appCommandManager.processCommand(normalizedCommand)
            commandCache[normalizedCommand] = isValid
            
            if (isValid) {
                executeCommand(normalizedCommand)
            }
            
            // Limit cache size
            if (commandCache.size > CACHE_SIZE) {
                val toRemove = commandCache.size - CACHE_SIZE
                commandCache.keys.take(toRemove).forEach { commandCache.remove(it) }
            }
        }
    }
    
    /**
     * Execute command through action coordinator
     */
    private fun executeCommand(command: String) {
        serviceScope.launch {
            actionCoordinator.executeAction(command)
        }
    }
    
    /**
     * Log performance metrics
     */
    private fun logPerformanceMetrics() {
        serviceScope.launch {
            val metrics = mutableMapOf<String, Any>()
            
            // Get metrics from optimized components
            metrics.putAll(uiScrapingEngine.getPerformanceMetrics())
            metrics.putAll(appCommandManager.getPerformanceMetrics())
            
            // Add service metrics
            metrics["commandCacheSize"] = commandCache.size
            metrics["nodeCacheSize"] = nodeCache.size
            metrics["isServiceReady"] = isServiceReady
            
            // Add event count metrics
            eventCounts.forEach { (eventType, count) ->
                val eventName = when (eventType) {
                    AccessibilityEvent.TYPE_VIEW_CLICKED -> "clicks"
                    AccessibilityEvent.TYPE_VIEW_FOCUSED -> "focuses"
                    AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> "textChanges"
                    AccessibilityEvent.TYPE_VIEW_SCROLLED -> "scrolls"
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "windowChanges"
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "contentChanges"
                    else -> "event_$eventType"
                }
                metrics["event_$eventName"] = count.get()
            }
            
            Log.i(TAG, "Performance Metrics: $metrics")
        }
    }
    
    override fun onInterrupt() {
        Log.w(TAG, "Service interrupted")
    }
    
    override fun onDestroy() {
        super<VoiceAccessibilityService>.onDestroy()
        
        // Cleanup optimized components
        uiScrapingEngine.destroy()
        appCommandManager.destroy()
        
        // Cancel coroutines
        serviceScope.cancel()
        
        // Clear caches
        commandCache.clear()
        nodeCache.clear()
        
        Log.i(TAG, "VoiceOSAccessibility destroyed")
    }
}