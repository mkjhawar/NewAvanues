/**
 * VoiceAccessibilityService.kt - Unified Voice Accessibility Service
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-26
 * 
 * This implementation combines:
 * - Handler architecture (validated via COT/ROT/TOT analysis)
 * - Direct command execution (VOS4 compliance)
 * - SR6-HYBRID patterns (configuration, eventbus where needed)
 * - Performance optimizations (lazy loading, caching)
 * 
 * Performance Targets:
 * - Startup: < 1 second
 * - Command Response: < 100ms 
 * - Memory: < 15MB idle
 * - CPU: < 2% idle
 */
package com.augmentalis.voiceos.accessibility.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceos.accessibility.managers.*
import com.augmentalis.voiceos.accessibility.handlers.*
import com.augmentalis.voiceos.accessibility.config.ServiceConfiguration
import com.augmentalis.voiceos.accessibility.extractors.UIScrapingEngine
import com.augmentalis.voiceos.speech.api.SpeechListenerManager
import com.augmentalis.voiceos.speech.api.RecognitionResult
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import kotlin.system.measureTimeMillis

/**
 * VoiceOS Accessibility Service
 * 
 * Merges best features from multiple implementations:
 * - VOSAccessibilitySvc handler architecture
 * - AccessibilityService direct execution
 * - SR6-HYBRID configuration patterns
 */
open class VoiceAccessibilityService : android.accessibilityservice.AccessibilityService() {
    
    companion object {
        private const val TAG = "VoiceAccessibilityService"
        private const val SERVICE_NAME = "VoiceOS Accessibility"
        
        // Performance constants (VOS4 standards)
        private const val INIT_TIMEOUT_MS = 1000L
        private const val COMMAND_TIMEOUT_MS = 100L
        
        // Singleton with weak reference (prevents memory leaks)
        @Volatile
        private var instanceRef: WeakReference<VoiceAccessibilityService>? = null
        
        @JvmStatic
        fun getInstance(): VoiceAccessibilityService? = instanceRef?.get()
        
        @JvmStatic
        fun isServiceRunning(): Boolean = instanceRef?.get() != null
        
        /**
         * Direct command execution - VOS4 pattern with handler support
         * Fast path for common commands, handler routing for complex ones
         */
        @JvmStatic
        fun executeCommand(commandText: String): Boolean {
            val service = instanceRef?.get() ?: return false
            val command = commandText.lowercase().trim()
            
            // Measure performance
            val startTime = System.nanoTime()
            
            val result = when {
                // Navigation commands - direct execution (fastest path)
                command == "back" || command == "go back" -> 
                    service.performGlobalAction(GLOBAL_ACTION_BACK)
                    
                command == "home" || command == "go home" -> 
                    service.performGlobalAction(GLOBAL_ACTION_HOME)
                    
                command == "recent" || command == "recent apps" -> 
                    service.performGlobalAction(GLOBAL_ACTION_RECENTS)
                    
                command == "notifications" -> 
                    service.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                    
                command == "settings" || command == "quick settings" -> 
                    service.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
                    
                command == "power" || command == "power menu" ->
                    service.performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
                    
                // Screenshot (Android P+)
                command == "screenshot" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        service.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                    } else {
                        Log.w(TAG, "Screenshot not available on API < 28")
                        false
                    }
                }
                
                // Voice recognition controls
                command == "voice on" || command == "start voice" || command == "listen" ->
                    service.startVoiceRecognition().let { true }
                    
                command == "voice off" || command == "stop voice" || command == "stop listening" ->
                    service.stopVoiceRecognition().let { true }
                    
                command == "voice toggle" || command == "toggle voice" ->
                    service.toggleVoiceRecognition()
                    
                command == "voice status" ->
                    service.getVoiceRecognitionStatus().let { status ->
                        Log.i(TAG, "Voice status: $status")
                        true
                    }
                    
                // For complex commands, use handler architecture
                else -> service.processComplexCommand(command)
            }
            
            val elapsed = (System.nanoTime() - startTime) / 1_000_000 // Convert to ms
            if (elapsed > COMMAND_TIMEOUT_MS) {
                Log.w(TAG, "⚠️ Slow command: $command took ${elapsed}ms")
            } else {
                Log.d(TAG, "✅ Command: $command executed in ${elapsed}ms")
            }
            
            return result
        }
    }
    
    // Coroutine scope for async operations
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // Configuration (SR6-HYBRID pattern)
    private lateinit var configuration: ServiceConfiguration
    
    // Event bus for internal communication (SR6-HYBRID pattern where needed)
    // Note: Only use if handlers need to communicate
    // private val eventBus = SharedFlow<ServiceEvent>()
    
    // Handler architecture (VOS4 Interface Exception - documented and justified)
    private val actionCoordinator by lazy {
        ActionCoordinator(this).also {
            Log.d(TAG, "ActionCoordinator initialized (lazy)")
        }
    }
    
    // Core managers - lazy loaded for performance
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
    
    private val appCommandManager by lazy {
        AppCommandManager(this).also {
            Log.d(TAG, "AppCommandManager initialized (lazy)")
        }
    }
    
    // Advanced features - only load if needed
    private val uiScrapingEngine by lazy {
        UIScrapingEngine(this).also {
            Log.d(TAG, "UIScrapingEngine initialized (lazy)")
        }
    }
    
    // Voice Recognition Integration
    private var speechListenerManager: SpeechListenerManager? = null
    private var isVoiceRecognitionActive = false
    private var voiceRecognitionEnabled = true // Can be controlled via settings
    
    // Cache for performance
    private val commandCache = mutableMapOf<String, Boolean>()
    private val nodeCache = mutableMapOf<String, WeakReference<AccessibilityNodeInfo>>()
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "$SERVICE_NAME onCreate")
        
        // Set instance reference
        instanceRef = WeakReference(this)
        
        // Load configuration (SR6-HYBRID pattern)
        configuration = ServiceConfiguration.loadFromPreferences(this)
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "$SERVICE_NAME connected successfully!")
        
        // Configure service info
        configureServiceInfo()
        
        // Initialize in coroutine for non-blocking startup
        serviceScope.launch {
            val initTime = measureTimeMillis {
                initializeService()
            }
            
            if (initTime < INIT_TIMEOUT_MS) {
                Log.i(TAG, "✅ Service initialized in ${initTime}ms (target: <${INIT_TIMEOUT_MS}ms)")
            } else {
                Log.w(TAG, "⚠️ Slow initialization: ${initTime}ms (target: <${INIT_TIMEOUT_MS}ms)")
            }
            
            // Initialize voice recognition if enabled
            if (configuration.voiceRecognitionEnabled) {
                initializeVoiceRecognition()
            }
        }
        
        // Show confirmation toast
        if (configuration.showToasts) {
            try {
                android.widget.Toast.makeText(
                    applicationContext,
                    "$SERVICE_NAME Started",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Log.e(TAG, "Could not show toast", e)
            }
        }
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        
        // Skip if service is not enabled
        if (!configuration.isEnabled) return
        
        // Log important events for debugging (if verbose mode)
        if (configuration.verboseLogging) {
            when (event.eventType) {
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    Log.d(TAG, "Window changed: ${event.packageName}")
                    // Update dynamic commands for new window
                    if (configuration.dynamicCommandsEnabled) {
                        updateDynamicCommands()
                    }
                }
                AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                    Log.d(TAG, "View clicked: ${event.text}")
                }
            }
        }
    }
    
    override fun onInterrupt() {
        Log.w(TAG, "Service interrupted")
    }
    
    override fun onDestroy() {
        Log.i(TAG, "Service destroying")
        
        // Clean up voice recognition first
        cleanupVoiceRecognition()
        
        // Clean up resources
        cleanupResources()
        
        // Clean up instance reference
        instanceRef = null
        
        // Cancel coroutines
        serviceScope.cancel()
        
        super.onDestroy()
    }
    
    /**
     * Initialize service components
     */
    private suspend fun initializeService() = withContext(Dispatchers.IO) {
        try {
            // Initialize handlers if enabled
            if (configuration.handlersEnabled) {
                actionCoordinator.initialize()
            }
            
            // Preload app commands for faster execution
            if (configuration.appLaunchingEnabled) {
                appCommandManager.initialize()
            }
            
            // Initialize cursor if enabled
            if (configuration.cursorEnabled) {
                cursorManagerInstance.initialize()
            }
            
            Log.i(TAG, "Service initialization complete")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize service", e)
        }
    }
    
    /**
     * Configure service capabilities
     */
    private fun configureServiceInfo() {
        try {
            serviceInfo?.let { info ->
                // Configure service capabilities
                info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
                info.flags = info.flags or 
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
                
                // Add accessibility button support on Android O+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON
                }
                
                // Add fingerprint gesture support on Android O+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && configuration.fingerprintGesturesEnabled) {
                    info.flags = info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FINGERPRINT_GESTURES
                }
                
                Log.d(TAG, "Service info configured")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to configure service info", e)
        }
    }
    
    /**
     * Process complex commands using handler architecture
     */
    private fun processComplexCommand(command: String): Boolean {
        // Check cache first
        commandCache[command]?.let { 
            Log.d(TAG, "Command cache hit: $command")
            return it 
        }
        
        val result = try {
            when {
                // Try action coordinator first (uses handlers)
                configuration.handlersEnabled && actionCoordinator.canHandle(command) -> {
                    actionCoordinator.executeAction(command)
                }
                
                // Try app launching
                configuration.appLaunchingEnabled && 
                    (command.startsWith("open ") || command.startsWith("launch ")) -> {
                    val appName = command.removePrefix("open ").removePrefix("launch ").trim()
                    runBlocking { appCommandManager.launchAppByCommand(appName) }
                }
                
                // Try dynamic commands
                configuration.dynamicCommandsEnabled && 
                    dynamicCommandGenerator.hasCommand(command) -> {
                    dynamicCommandGenerator.executeCommand(command)
                }
                
                // Try cursor commands
                configuration.cursorEnabled && command.startsWith("cursor ") -> {
                    cursorManagerInstance.handleCursorCommand(command)
                }
                
                // Try click by text
                command.startsWith("click ") || command.startsWith("tap ") -> {
                    val target = command.removePrefix("click ").removePrefix("tap ").trim()
                    clickNodeByText(target)
                }
                
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing command: $command", e)
            false
        }
        
        // Cache result
        if (configuration.commandCachingEnabled) {
            commandCache[command] = result
        }
        
        return result
    }
    
    /**
     * Update dynamic commands based on current UI
     */
    private fun updateDynamicCommands() {
        serviceScope.launch {
            try {
                dynamicCommandGenerator.generateCommands()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update dynamic commands", e)
            }
        }
    }
    
    /**
     * Click node by text - direct implementation
     */
    private fun clickNodeByText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        
        // Check node cache first
        nodeCache[text]?.get()?.let { node ->
            if (node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                Log.d(TAG, "Clicked cached node: $text")
                return true
            }
        }
        
        val nodes = root.findAccessibilityNodeInfosByText(text)
        if (!nodes.isNullOrEmpty()) {
            val node = nodes[0]
            // Cache node
            nodeCache[text] = WeakReference(node)
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        
        return clickNodeByDescription(root, text)
    }
    
    /**
     * Click by content description
     */
    private fun clickNodeByDescription(
        node: AccessibilityNodeInfo, 
        text: String
    ): Boolean {
        if (node.contentDescription?.contains(text, ignoreCase = true) == true) {
            // Cache node
            nodeCache[text] = WeakReference(node)
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                if (clickNodeByDescription(child, text)) {
                    // Recycle node for API < 34
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        @Suppress("DEPRECATION")
                        child.recycle()
                    }
                    return true
                }
                // Recycle node for API < 34
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    @Suppress("DEPRECATION")
                    child.recycle()
                }
            }
        }
        return false
    }
    
    /**
     * Check if voice recognition can be initialized
     */
    private fun checkVoiceRecognitionPrerequisites(): Boolean {
        // Check if VoiceRecognition app is installed
        if (!isVoiceRecognitionAppInstalled()) {
            Log.w(TAG, "VoiceRecognition app not installed")
            if (configuration.showToasts) {
                showToast("Voice recognition requires VoiceRecognition app to be installed")
            }
            return false
        }
        
        // Check RECORD_AUDIO permission (although VoiceRecognition app should handle this)
        if (!hasRecordAudioPermission()) {
            Log.w(TAG, "RECORD_AUDIO permission not granted")
            // Don't block initialization since VoiceRecognition app handles its own permissions
            // Just log for awareness
        }
        
        return true
    }
    
    /**
     * Check if VoiceRecognition app is installed
     */
    private fun isVoiceRecognitionAppInstalled(): Boolean {
        return try {
            packageManager.getApplicationInfo("com.augmentalis.voicerecognition", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    /**
     * Check if RECORD_AUDIO permission is granted
     */
    private fun hasRecordAudioPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Pre-M, permission is granted at install time
        }
    }
    
    /**
     * Initialize voice recognition client
     */
    private suspend fun initializeVoiceRecognition() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing voice recognition client")
            
            // Check prerequisites first
            if (!checkVoiceRecognitionPrerequisites()) {
                Log.w(TAG, "Voice recognition prerequisites not met, skipping initialization")
                return@withContext
            }
            
            speechListenerManager = SpeechListenerManager().apply {
                onResult = { result: RecognitionResult ->
                    if (result.isFinal && result.text.isNotBlank()) {
                        Log.d(TAG, "Voice command received: '${result.text}' (confidence: ${result.confidence})")
                        processVoiceCommand(result.text, result.confidence)
                    }
                }
                
                onError = { error: String, code: Int ->
                    Log.e(TAG, "Voice recognition error: $code - $error")
                    showVoiceError(error)
                }
                
                onStateChange = { state: String, message: String? ->
                    Log.d(TAG, "Voice recognition state: $state ${message?.let { "- $it" } ?: ""}")
                    isVoiceRecognitionActive = state == "LISTENING" || state == "RECOGNIZING"
                }
            }
            
            isVoiceRecognitionActive = true
            Log.i(TAG, "Voice recognition initialized with SpeechListenerManager")
            
            // Start recognition if configured to auto-start
            if (configuration.voiceAutoStart) {
                startVoiceRecognition()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize voice recognition", e)
            speechListenerManager = null
        }
    }
    
    /**
     * Start voice recognition
     */
    private fun startVoiceRecognition() {
        speechListenerManager?.let { manager ->
            if (!isVoiceRecognitionActive) {
                isVoiceRecognitionActive = true
                manager.notifyStateChange("LISTENING", "Voice recognition started")
                Log.i(TAG, "Voice recognition started")
                
                if (configuration.showToasts) {
                    showToast("Voice recognition activated")
                } else {
                    // No toast to show
                }
            } else {
                Log.w(TAG, "Voice recognition already active")
            }
        }
    }
    
    /**
     * Stop voice recognition
     */
    private fun stopVoiceRecognition() {
        speechListenerManager?.let { manager ->
            if (isVoiceRecognitionActive) {
                isVoiceRecognitionActive = false
                manager.notifyStateChange("STOPPED", "Voice recognition stopped by user")
                Log.i(TAG, "Voice recognition stopped")
            }
        }
    }
    
    /**
     * Process voice command
     */
    private fun processVoiceCommand(text: String, confidence: Float) {
        serviceScope.launch {
            try {
                // Minimum confidence check
                if (confidence < configuration.voiceMinConfidence) {
                    Log.d(TAG, "Command ignored due to low confidence: $confidence < ${configuration.voiceMinConfidence}")
                    return@launch
                }
                
                // Route to ActionCoordinator for processing
                val result = if (configuration.handlersEnabled) {
                    actionCoordinator.processVoiceCommand(text, confidence)
                } else {
                    // Fallback to static command execution
                    executeCommand(text)
                }
                
                // Provide feedback
                if (result) {
                    Log.i(TAG, "Voice command executed successfully: '$text'")
                    if (configuration.voiceCommandFeedback) {
                        showToast("Command executed: $text")
                    }
                } else {
                    Log.w(TAG, "Voice command failed or unrecognized: '$text'")
                    if (configuration.voiceCommandFeedback) {
                        showToast("Command not recognized: $text")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing voice command: $text", e)
            }
        }
    }
    
    /**
     * Clean up voice recognition
     */
    private fun cleanupVoiceRecognition() {
        try {
            stopVoiceRecognition()
            speechListenerManager?.clear()
            speechListenerManager = null
            isVoiceRecognitionActive = false
            Log.d(TAG, "Voice recognition cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up voice recognition", e)
        }
    }
    
    /**
     * Show voice error
     */
    private fun showVoiceError(message: String) {
        if (configuration.showToasts) {
            serviceScope.launch(Dispatchers.Main) {
                showToast("Voice Error: $message")
            }
        }
    }
    
    /**
     * Show toast message
     */
    private fun showToast(message: String) {
        try {
            android.widget.Toast.makeText(
                applicationContext,
                message,
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Log.e(TAG, "Could not show toast: $message", e)
        }
    }
    
    /**
     * Get voice recognition status
     */
    fun getVoiceRecognitionStatus(): String {
        return when {
            speechListenerManager == null -> "Not initialized"
            isVoiceRecognitionActive -> "Active - Listening"
            else -> "Idle - Ready"
        }
    }
    
    /**
     * Get cursor manager instance
     */
    open fun getCursorManager(): CursorManager? {
        return try {
            cursorManagerInstance
        } catch (e: Exception) {
            Log.w(TAG, "CursorManager not available", e)
            null
        }
    }
    
    /**
     * Get voice recognition debug info
     */
    fun getVoiceRecognitionDebugInfo(): String {
        return buildString {
            appendLine("Voice Recognition Status:")
            appendLine("- Enabled: ${configuration.voiceRecognitionEnabled}")
            appendLine("- Auto-start: ${configuration.voiceAutoStart}")
            appendLine("- Engine: ${configuration.voiceEngine}")
            appendLine("- Language: ${configuration.voiceLanguage}")
            appendLine("- Min Confidence: ${configuration.voiceMinConfidence}")
            appendLine("- Status: ${getVoiceRecognitionStatus()}")
            appendLine("- Manager Ready: ${speechListenerManager != null}")
            appendLine("- Prerequisites: ${checkVoiceRecognitionPrerequisites()}")
            appendLine("- Active: $isVoiceRecognitionActive")
        }
    }
    
    /**
     * Toggle voice recognition on/off
     */
    fun toggleVoiceRecognition(): Boolean {
        return if (isVoiceRecognitionActive) {
            stopVoiceRecognition()
            if (configuration.showToasts) {
                showToast("Voice recognition stopped")
            }
            false
        } else if (speechListenerManager != null) {
            startVoiceRecognition()
            if (configuration.showToasts) {
                showToast("Voice recognition started")
            }
            true
        } else {
            if (configuration.showToasts) {
                showToast("Voice recognition not available")
            }
            false
        }
    }
    
    /**
     * Perform click at screen coordinates
     * Used by GazeHandler for gaze-based clicking
     */
    open fun performClick(x: Float, y: Float): Boolean {
        return try {
            val path = android.graphics.Path().apply { moveTo(x, y) }
            val gesture = android.accessibilityservice.GestureDescription.Builder()
                .addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(path, 0, 100))
                .build()
            
            dispatchGesture(gesture, null, null)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to perform click at ($x, $y)", e)
            false
        }
    }
    
    /**
     * Clean up resources
     */
    private fun cleanupResources() {
        try {
            // Clear caches
            commandCache.clear()
            nodeCache.clear()
            
            // Dispose managers
            if (::configuration.isInitialized && configuration.handlersEnabled) {
                actionCoordinator.dispose()
            }
            
            Log.d(TAG, "Resources cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up resources", e)
        }
    }
}