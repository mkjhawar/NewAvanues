/**
 * ActionCoordinator.kt - Coordinates action execution across handlers
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-01-26
 */
package com.augmentalis.voiceoscore.accessibility.managers

import android.util.Log
import com.augmentalis.voiceoscore.accessibility.IVoiceOSContext
import com.augmentalis.voiceoscore.accessibility.handlers.ActionCategory
import com.augmentalis.voiceoscore.accessibility.handlers.ActionHandler
import com.augmentalis.voiceoscore.accessibility.handlers.AppHandler
import com.augmentalis.voiceoscore.accessibility.handlers.BluetoothHandler
import com.augmentalis.voiceoscore.accessibility.handlers.DeviceHandler
import com.augmentalis.voiceoscore.accessibility.handlers.DragHandler
import com.augmentalis.voiceoscore.accessibility.handlers.GestureHandler
import com.augmentalis.voiceoscore.accessibility.handlers.HelpMenuHandler
import com.augmentalis.voiceoscore.accessibility.handlers.InputHandler
import com.augmentalis.voiceoscore.accessibility.handlers.NavigationHandler
import com.augmentalis.voiceoscore.accessibility.handlers.NumberHandler
import com.augmentalis.voiceoscore.accessibility.handlers.SelectHandler
import com.augmentalis.voiceoscore.accessibility.handlers.SystemHandler
import com.augmentalis.voiceoscore.accessibility.handlers.UIHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

/**
 * Coordinates action execution across multiple handlers
 * Direct implementation except for ActionHandler interface usage
 * Now uses IVoiceOSContext for Dependency Inversion Principle compliance
 */
class ActionCoordinator(private val context: IVoiceOSContext) {

    companion object {
        private const val TAG = "ActionCoordinator"
        private const val HANDLER_TIMEOUT_MS = 5000L
    }

    // Handler registry - uses interface for polymorphic storage
    private val handlers = ConcurrentHashMap<ActionCategory, MutableList<ActionHandler>>()

    // Performance metrics
    private val metrics = ConcurrentHashMap<String, MetricData>()

    // Coroutine scope for async handler operations
    private val coordinatorScope = CoroutineScope(
        Dispatchers.Default + SupervisorJob()
    )

    data class MetricData(
        var count: Long = 0,
        var totalTimeMs: Long = 0,
        var successCount: Long = 0,
        var lastExecutionMs: Long = 0
    ) {
        val averageTimeMs: Long
            get() = if (count > 0) totalTimeMs / count else 0

        val successRate: Float
            get() = if (count > 0) successCount.toFloat() / count else 0f
    }

    /**
     * Initialize coordinator and register handlers
     */
    fun initialize() {
        Log.d(TAG, "Initializing ActionCoordinator")

        // Register handlers - lazy initialization
        registerHandler(ActionCategory.SYSTEM, SystemHandler(context))
        registerHandler(ActionCategory.APP, AppHandler(context))
        registerHandler(ActionCategory.DEVICE, DeviceHandler(context))
        registerHandler(ActionCategory.INPUT, InputHandler(context))
        registerHandler(ActionCategory.NAVIGATION, NavigationHandler(context))
        registerHandler(ActionCategory.UI, UIHandler(context))
        registerHandler(ActionCategory.GESTURE, GestureHandler(context))
        //registerHandler(ActionCategory.GAZE, GazeHandler(context))

        // Add DragHandler to GESTURE category (multiple handlers per category)
        val dragHandler = DragHandler(context)
        registerHandler(ActionCategory.GESTURE, dragHandler)

        // Register new migrated handlers from Legacy Avenue
        registerHandler(ActionCategory.DEVICE, BluetoothHandler(context))
        registerHandler(ActionCategory.UI, HelpMenuHandler(context))
        registerHandler(ActionCategory.UI, SelectHandler(context))
        registerHandler(ActionCategory.UI, NumberHandler(context))

        // Set cursor manager reference for DragHandler
        try {
//            val cursorManager = service.getCursorManager()
//            if (cursorManager != null) {
//                dragHandler.setCursorManager(cursorManager)
//            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not set cursor manager for DragHandler", e)
        }

        // Initialize all handlers
        handlers.values.flatten().forEach { handler ->
            try {
                handler.initialize()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize handler", e)
            }
        }

        Log.i(TAG, "ActionCoordinator initialized with ${handlers.size} handlers")
    }

    /**
     * Register a handler for a category
     */
    private fun registerHandler(category: ActionCategory, handler: ActionHandler) {
        handlers.computeIfAbsent(category) { mutableListOf() }.add(handler)
        Log.d(TAG, "Registered handler for category: $category")
    }

    /**
     * Check if any handler can handle the action
     */
    fun canHandle(action: String): Boolean {
        return handlers.values.flatten().any { it.canHandle(action) }
    }

    /**
     * Process voice command text and route to appropriate handler
     * This method is called by SpeechRecognitionIntegration -> UnifiedCommandProcessor
     */
    suspend fun processCommand(commandText: String): Boolean {
        if (commandText.isBlank()) {
            Log.w(TAG, "Received empty command text")
            return false
        }

        val cleanCommand = commandText.trim().lowercase()
        Log.d(TAG, "Processing voice command: '$cleanCommand'")

        // Try to execute the command directly first
        if (executeAction(cleanCommand)) {
            Log.d(TAG, "Direct command execution successful")
            return true
        }

        // If direct execution fails, try command interpretation
        val interpretedAction = interpretVoiceCommand(cleanCommand)
        if (interpretedAction != null) {
            Log.d(TAG, "Interpreted command: '$cleanCommand' -> '$interpretedAction'")
            return executeAction(interpretedAction)
        }

        Log.w(TAG, "Could not process voice command: '$cleanCommand'")
        return false
    }

    /**
     * Interpret natural language voice commands into action strings
     */
    private fun interpretVoiceCommand(command: String): String? {
        // Basic command interpretation patterns
        return when {
            // Navigation commands
            command.contains("go back") || command.contains("back") -> "navigate_back"
            command.contains("go home") || command.contains("home") -> "navigate_home"
            command.contains("scroll up") -> "scroll_up"
            command.contains("scroll down") -> "scroll_down"
            command.contains("scroll left") -> "scroll_left"
            command.contains("scroll right") -> "scroll_right"

            // System commands
            command.contains("volume up") -> "volume_up"
            command.contains("volume down") -> "volume_down"
            command.contains("mute") -> "volume_mute"

            // App commands
            command.startsWith("open ") -> {
                val appName = command.removePrefix("open ").trim()
                "launch_app:$appName"
            }

            command.startsWith("launch ") -> {
                val appName = command.removePrefix("launch ").trim()
                "launch_app:$appName"
            }

            // UI commands
            command.contains("tap") || command.contains("click") -> {
                // Extract coordinates or element if present
                "ui_tap"
            }

            command.contains("swipe left") -> "swipe left"
            command.contains("swipe right") -> "swipe right"
            command.contains("swipe up") -> "swipe up"
            command.contains("swipe down") -> "swipe down"
            command.contains("pinch open") || command.contains("zoom in") -> "pinch open"
            command.contains("pinch close") || command.contains("zoom out") -> "pinch close"
            command.contains("pinch in") -> "pinch open"
            command.contains("pinch out") -> "pinch close"

            // Input commands
            command.startsWith("type ") -> {
                val text = command.removePrefix("type ").trim()
                "input_text:$text"
            }

            command.startsWith("say ") -> {
                val text = command.removePrefix("say ").trim()
                "input_text:$text"
            }

            // Device commands
            command.contains("brightness up") -> "brightness_up"
            command.contains("brightness down") -> "brightness_down"
            command.contains("wifi on") -> "wifi_enable"
            command.contains("wifi off") -> "wifi_disable"
            command.contains("bluetooth on") -> "bluetooth_enable"
            command.contains("bluetooth off") -> "bluetooth_disable"

            // Legacy Avenue migrated commands - Bluetooth
            command.contains("turn on bluetooth") -> "bluetooth_enable"
            command.contains("turn off bluetooth") -> "bluetooth_disable"
            command.contains("bluetooth settings") -> "bluetooth_settings"

            // Legacy Avenue migrated commands - Help system
            command.contains("show help") -> "show_help"
            command.contains("hide help") -> "hide_help"
            command.contains("help menu") -> "help_menu"
            command.contains("what can i say") -> "show_commands"
            command.contains("show commands") -> "show_commands"
            command.contains("voice commands") -> "show_commands"

            // Legacy Avenue migrated commands - Selection
            command.contains("select mode") -> "select_mode"
            command.contains("selection mode") -> "selection_mode"
            command.contains("select all") -> "select_all"
            command.equals("select", ignoreCase = true) -> "select"
            command.equals("menu", ignoreCase = true) -> "menu"

            // Legacy Avenue migrated commands - Number overlay
            command.contains("show numbers") -> "show_numbers"
            command.contains("hide numbers") -> "hide_numbers"
            command.contains("numbers on") -> "show_numbers"
            command.contains("numbers off") -> "hide_numbers"
            command.contains("label elements") -> "show_numbers"

            // Number commands (e.g., "tap 5", "click 3")
            Regex("(tap|click|select)\\s+(\\d+)").find(command) != null -> {
                val match = Regex("(tap|click|select)\\s+(\\d+)").find(command)!!
                val number = match.groupValues[2]
                "click_number:$number"
            }

            // Gaze commands - Legacy Avenue compatibility
            command.contains("gaze on") || command.contains("enable gaze") -> "gaze_on"
            command.contains("gaze off") || command.contains("disable gaze") -> "gaze_off"
            command.contains("look and click") || command.contains("gaze tap") -> "look_and_click"
            command.contains("gaze click") || command.contains("dwell click") -> "gaze_click"
            command.contains("calibrate gaze") || command.contains("gaze calibrate") -> "gaze_calibrate"
            command.contains("center gaze") || command.contains("gaze center") -> "gaze_center"
            command.contains("toggle dwell") || command.contains("dwell toggle") -> "toggle_dwell"
            command.contains("gaze status") || command.contains("where am i looking") -> "gaze_status"
            command.contains("reset gaze") || command.contains("gaze reset") -> "gaze_reset"
            command.contains("gaze help") -> "gaze_help"

            else -> {
                // If no interpretation found, try as-is
                Log.d(TAG, "No interpretation found for: '$command'")
                null
            }
        }
    }

    /**
     * Execute an action by routing to appropriate handler
     */
    suspend fun executeAction(
        action: String,
        params: Map<String, Any> = emptyMap()
    ): Boolean = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        // Find handler that can handle this action
        val handler = findHandler(action)
        if (handler == null) {
            Log.w(TAG, "No handler found for action: $action")
            recordMetric(action, System.currentTimeMillis() - startTime, false)
            return@withContext false
        }

        // Determine category
        val category = handlers.entries.find { it.value.contains(handler) }?.key
            ?: ActionCategory.CUSTOM

        try {
            // Execute with timeout
            val result = withTimeoutOrNull(HANDLER_TIMEOUT_MS) {
                handler.execute(category, action, params)
            } ?: false

            val executionTime = System.currentTimeMillis() - startTime
            recordMetric(action, executionTime, result)

            if (executionTime > 100) {
                Log.w(TAG, "Slow action execution: $action took ${executionTime}ms")
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "Error executing action: $action", e)
            recordMetric(action, System.currentTimeMillis() - startTime, false)
            false
        }
    }

    /**
     * Execute action asynchronously
     */
    fun executeActionAsync(
        action: String,
        params: Map<String, Any> = emptyMap(),
        callback: (Boolean) -> Unit = {}
    ) {
        coordinatorScope.launch {
            val result = executeAction(action, params)
            withContext(Dispatchers.Main) {
                callback(result)
            }
        }
    }

    /**
     * Execute action synchronously (blocking version for compatibility)
     * WARNING: Only use from background threads!
     */
    @Deprecated("Use suspend executeAction instead", ReplaceWith("executeAction(action, params)"))
    fun executeActionBlocking(
        action: String,
        params: Map<String, Any> = emptyMap()
    ): Boolean {
        return runBlocking {
            executeAction(action, params)
        }
    }

    /**
     * Process voice command with confidence scoring
     */
    suspend fun processVoiceCommand(text: String, confidence: Float): Boolean {
        val startTime = System.currentTimeMillis()

        Log.d(TAG, "Processing voice command: '$text' (confidence: $confidence)")

        // Normalize and preprocess the command
        val normalizedCommand = text.lowercase().trim()

        // Create enhanced parameters with voice metadata
        val voiceParams = mapOf(
            "source" to "voice",
            "confidence" to confidence,
            "originalText" to text,
            "timestamp" to startTime
        )

        // Try to execute the command
        val result = try {
            // First try as direct action
            if (canHandle(normalizedCommand)) {
                executeAction(normalizedCommand, voiceParams)
            }
            // Try with voice-specific preprocessing
            else {
                processVoiceCommandWithContext(normalizedCommand, voiceParams)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing voice command: $text", e)
            false
        }

        val executionTime = System.currentTimeMillis() - startTime
        recordMetric("voice:$normalizedCommand", executionTime, result)

        Log.d(TAG, "Voice command '$text' processed in ${executionTime}ms: $result")
        return result
    }

    /**
     * Process voice command with additional context and variations
     */
    private suspend fun processVoiceCommandWithContext(
        command: String,
        params: Map<String, Any>
    ): Boolean {
        // Try common voice command variations
        val variations = generateVoiceCommandVariations(command)

        for (variation in variations) {
            if (canHandle(variation)) {
                Log.d(TAG, "Matched voice variation: '$command' -> '$variation'")
                return executeAction(variation, params)
            }
        }

        // Try handler-specific voice processing
        for ((category, handlerList) in handlers) {
            for (handler in handlerList) {
                if (handler.canHandle(command)) {
                    Log.d(TAG, "Handler $category processing voice command: $command")
                    return try {
                        handler.execute(category, command, params)
                    } catch (e: Exception) {
                        Log.e(TAG, "Handler $category failed to process voice command", e)
                        false
                    }
                }
            }
        }

        Log.w(TAG, "No handler found for voice command: $command")
        return false
    }

    /**
     * Generate common variations of voice commands
     */
    private fun generateVoiceCommandVariations(command: String): List<String> {
        val variations = mutableListOf<String>()

        // Add the original
        variations.add(command)

        // Common voice command prefixes/suffixes to try removing
        val prefixesToRemove = listOf("please ", "can you ", "could you ", "will you ", "hey ", "ok ")
        val suffixesToRemove = listOf(" please", " now", " for me")

        var current = command

        // Remove common prefixes
        for (prefix in prefixesToRemove) {
            if (current.startsWith(prefix)) {
                current = current.removePrefix(prefix).trim()
                variations.add(current)
                break
            }
        }

        // Remove common suffixes
        for (suffix in suffixesToRemove) {
            if (current.endsWith(suffix)) {
                current = current.removeSuffix(suffix).trim()
                variations.add(current)
                break
            }
        }

        // Try common verb transformations
        val verbMappings = mapOf(
            "open up" to "open",
            "launch" to "open",
            "start" to "open",
            "close" to "back",
            "exit" to "back",
            "navigate to" to "open",
            "go to" to "open",
            "switch to" to "open",
            "press" to "click",
            "tap" to "click",
            "touch" to "click",
            "select" to "click"
        )

        for ((from, to) in verbMappings) {
            if (current.contains(from)) {
                variations.add(current.replace(from, to))
            }
        }

        return variations.distinct()
    }

    /**
     * Find handler that can process the action
     */
    private fun findHandler(action: String): ActionHandler? {
        // First, check handlers by category priority
        val priorityOrder = listOf(
            ActionCategory.SYSTEM,      // System commands have highest priority
            ActionCategory.NAVIGATION,  // Navigation next
            ActionCategory.APP,         // App launching
            ActionCategory.GAZE,        // Gaze interactions have high priority
            ActionCategory.GESTURE,     // Gesture interactions
            ActionCategory.UI,          // UI interaction
            ActionCategory.DEVICE,      // Device control
            ActionCategory.INPUT,       // Text input
            ActionCategory.CUSTOM       // Custom last
        )

        for (category in priorityOrder) {
            handlers[category]?.let { handlerList ->
                for (handler in handlerList) {
                    if (handler.canHandle(action)) {
                        return handler
                    }
                }
            }
        }

        // If no prioritized handler found, check all
        return handlers.values.flatten().find { it.canHandle(action) }
    }

    /**
     * Get all supported actions across all handlers
     */
    fun getAllSupportedActions(): List<String> {
        return handlers.flatMap { (category, handlerList) ->
            handlerList.flatMap { handler ->
                handler.getSupportedActions().map { action ->
                    "${category.name.lowercase()}: $action"
                }
            }
        }
    }

    /**
     * Get supported actions for a specific category
     */
    fun getSupportedActions(category: ActionCategory): List<String> {
        return handlers[category]?.flatMap { it.getSupportedActions() } ?: emptyList()
    }

    /**
     * Record performance metric
     */
    private fun recordMetric(action: String, timeMs: Long, success: Boolean) {
        metrics.getOrPut(action) { MetricData() }.apply {
            count++
            totalTimeMs += timeMs
            lastExecutionMs = timeMs
            if (success) successCount++
        }
    }

    /**
     * Get performance metrics
     */
    fun getMetrics(): Map<String, MetricData> {
        return metrics.toMap()
    }

    /**
     * Get metrics for a specific action
     */
    fun getMetricsForAction(action: String): MetricData? {
        return metrics[action]
    }

    /**
     * Clear all metrics
     */
    fun clearMetrics() {
        metrics.clear()
    }

    /**
     * Dispose coordinator and all handlers
     */
    fun dispose() {
        Log.d(TAG, "Disposing ActionCoordinator")

        // Dispose all handlers
        handlers.values.flatten().forEach { handler ->
            try {
                handler.dispose()
            } catch (e: Exception) {
                Log.e(TAG, "Error disposing handler", e)
            }
        }

        // Clear handlers
        handlers.clear()

        // Cancel coroutines
        coordinatorScope.cancel()

        // Clear metrics
        metrics.clear()

        Log.d(TAG, "ActionCoordinator disposed")
    }

    /**
     * Get debug information
     */
    fun getDebugInfo(): String {
        return buildString {
            appendLine("ActionCoordinator Debug Info")
            appendLine("Handlers: ${handlers.size}")
            handlers.forEach { (category, handlerList) ->
                handlerList.forEach { handler ->
                    appendLine("  - $category: ${handler.javaClass.simpleName}")
                }
            }
            appendLine("Metrics: ${metrics.size} actions tracked")
            metrics.entries.take(5).forEach { (action, data) ->
                appendLine("  - $action: ${data.count} calls, ${data.averageTimeMs}ms avg, ${(data.successRate * 100).toInt()}% success")
            }
        }
    }

    fun getAllActions(): List<String> {
        return BluetoothHandler.SUPPORTED_ACTIONS +
                DeviceHandler.SUPPORTED_ACTIONS +
                DragHandler.SUPPORTED_ACTIONS +
                GestureHandler.SUPPORTED_ACTIONS +
                HelpMenuHandler.SUPPORTED_ACTIONS +
                InputHandler.SUPPORTED_ACTIONS +
                NavigationHandler.SUPPORTED_ACTIONS +
                NumberHandler.SUPPORTED_ACTIONS +
                SelectHandler.SUPPORTED_ACTIONS +
                SystemHandler.SUPPORTED_ACTIONS +
                UIHandler.SUPPORTED_ACTIONS +
                arrayListOf("mute voice", "wake up voice", "dictation", "end dictation")
    }
}