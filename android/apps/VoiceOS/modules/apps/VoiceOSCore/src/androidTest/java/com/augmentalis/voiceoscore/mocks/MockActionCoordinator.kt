/**
 * MockActionCoordinator.kt - Mock implementation of ActionCoordinator for testing
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-08-28
 */
package com.augmentalis.voiceoscore.mocks

import android.util.Log
import com.augmentalis.voiceoscore.accessibility.handlers.ActionCategory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * Mock implementation of ActionCoordinator for testing
 * 
 * Tracks all command processing and action execution calls with timing
 * and success metrics for verification in tests.
 */
class MockActionCoordinator(
    private val mockService: MockVoiceAccessibilityService
) {
    
    companion object {
        private const val TAG = "MockActionCoordinator"
    }
    
    // Tracking collections
    private val processedCommands = ConcurrentLinkedQueue<ProcessedCommand>()
    private val executedActions = ConcurrentLinkedQueue<ExecutedAction>()
    private val metrics = ConcurrentHashMap<String, MetricData>()
    
    // Counters
    private val commandCount = AtomicInteger(0)
    private val actionCount = AtomicInteger(0)
    private val successCount = AtomicInteger(0)
    private val errorCount = AtomicInteger(0)
    
    // Data classes for tracking
    data class ProcessedCommand(
        val command: String,
        val confidence: Float? = null,
        val success: Boolean,
        val timestamp: Long = System.currentTimeMillis(),
        val executionTimeMs: Long = 0
    )
    
    data class ExecutedAction(
        val action: String,
        val category: ActionCategory? = null,
        val params: Map<String, Any> = emptyMap(),
        val success: Boolean,
        val timestamp: Long = System.currentTimeMillis(),
        val executionTimeMs: Long = 0
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
     * Mock implementation of processCommand
     */
    fun processCommand(commandText: String): Boolean {
        val startTime = System.currentTimeMillis()
        commandCount.incrementAndGet()
        
        Log.d(TAG, "Mock processing command: '$commandText'")
        
        if (commandText.isBlank()) {
            val command = ProcessedCommand(commandText, null, false, startTime, 0)
            processedCommands.offer(command)
            recordMetric(commandText, 0, false)
            errorCount.incrementAndGet()
            return false
        }
        
        val cleanCommand = commandText.trim().lowercase()
        
        // Simulate command processing logic
        val success = simulateCommandProcessing(cleanCommand)
        val executionTime = System.currentTimeMillis() - startTime
        
        val command = ProcessedCommand(cleanCommand, null, success, startTime, executionTime)
        processedCommands.offer(command)
        recordMetric(commandText, executionTime, success)
        
        if (success) {
            successCount.incrementAndGet()
        } else {
            errorCount.incrementAndGet()
        }
        
        return success
    }
    
    /**
     * Mock implementation of processVoiceCommand with confidence
     */
    fun processVoiceCommand(text: String, confidence: Float): Boolean {
        val startTime = System.currentTimeMillis()
        commandCount.incrementAndGet()
        
        Log.d(TAG, "Mock processing voice command: '$text' (confidence: $confidence)")
        
        if (text.isBlank()) {
            val command = ProcessedCommand(text, confidence, false, startTime, 0)
            processedCommands.offer(command)
            recordMetric("voice:$text", 0, false)
            errorCount.incrementAndGet()
            return false
        }
        
        val normalizedCommand = text.lowercase().trim()
        
        // Simulate voice-specific processing
        val success = simulateVoiceCommandProcessing(normalizedCommand, confidence)
        val executionTime = System.currentTimeMillis() - startTime
        
        val command = ProcessedCommand(normalizedCommand, confidence, success, startTime, executionTime)
        processedCommands.offer(command)
        recordMetric("voice:$normalizedCommand", executionTime, success)
        
        if (success) {
            successCount.incrementAndGet()
        } else {
            errorCount.incrementAndGet()
        }
        
        return success
    }
    
    /**
     * Mock implementation of executeAction
     */
    fun executeAction(action: String, params: Map<String, Any> = emptyMap()): Boolean {
        val startTime = System.currentTimeMillis()
        actionCount.incrementAndGet()
        
        Log.d(TAG, "Mock executing action: '$action' with params: $params")
        
        if (action.isBlank()) {
            val executedAction = ExecutedAction(action, null, params, false, startTime, 0)
            executedActions.offer(executedAction)
            recordMetric(action, 0, false)
            errorCount.incrementAndGet()
            return false
        }
        
        // Simulate action execution
        val (success, category) = simulateActionExecution(action, params)
        val executionTime = System.currentTimeMillis() - startTime
        
        val executedAction = ExecutedAction(action, category, params, success, startTime, executionTime)
        executedActions.offer(executedAction)
        recordMetric(action, executionTime, success)
        
        if (success) {
            successCount.incrementAndGet()
        } else {
            errorCount.incrementAndGet()
        }
        
        return success
    }
    
    // Simulation methods
    
    private fun simulateCommandProcessing(command: String): Boolean {
        // Simulate processing time
        Thread.sleep((10..50).random().toLong())
        
        // Known command patterns that should succeed
        val knownPatterns = listOf(
            "go back", "back", "navigate back", "return",
            "go home", "home", "navigate home", "home screen",
            "volume up", "volume down", "mute", "louder", "quieter",
            "scroll up", "scroll down", "scroll left", "scroll right",
            "open", "launch", "start", "close", "exit",
            "tap", "click", "touch", "press",
            "swipe left", "swipe right", "swipe up", "swipe down",
            "settings", "camera", "phone", "messages",
            "brightness up", "brightness down",
            "wifi", "bluetooth"
        )
        
        // Check if command contains any known patterns
        return knownPatterns.any { pattern -> 
            command.contains(pattern) || pattern.contains(command)
        } || command.startsWith("open ") || command.startsWith("launch ") || 
           command.startsWith("input_text:") || command.startsWith("launch_app:")
    }
    
    private fun simulateVoiceCommandProcessing(command: String, confidence: Float): Boolean {
        // Lower confidence commands have higher chance of failure
        val confidenceModifier = when {
            confidence >= 0.8f -> 1.0f
            confidence >= 0.5f -> 0.9f
            confidence >= 0.3f -> 0.7f
            else -> 0.5f
        }
        
        val baseSuccess = simulateCommandProcessing(command)
        return baseSuccess && (Math.random() < confidenceModifier)
    }
    
    private fun simulateActionExecution(action: String, params: Map<String, Any>): Pair<Boolean, ActionCategory?> {
        // Simulate execution time
        Thread.sleep((5..30).random().toLong())
        
        val category = when {
            action.contains("navigate") || action.contains("back") || action.contains("home") ||
            action.contains("volume") || action.contains("mute") -> ActionCategory.SYSTEM
            
            action.contains("scroll") || action.contains("swipe") -> ActionCategory.NAVIGATION
            
            action.startsWith("launch_app") || action.contains("open") || 
            action.contains("launch") || action.contains("start") -> ActionCategory.APP
            
            action.contains("brightness") || action.contains("wifi") || 
            action.contains("bluetooth") -> ActionCategory.DEVICE
            
            action.startsWith("input_text") || action.contains("type") || 
            action.contains("say") -> ActionCategory.INPUT
            
            action.contains("tap") || action.contains("click") || action.contains("touch") ||
            action.contains("ui_") -> ActionCategory.UI
            
            action.contains("pinch") || action.contains("drag") || action.contains("gesture") ||
            action.contains("zoom") -> ActionCategory.GESTURE
            
            action.contains("gaze") || action.contains("eye") || action.contains("look") -> ActionCategory.GAZE
            
            else -> ActionCategory.CUSTOM
        }
        
        // Simulate success based on action type
        val success = when (category) {
            ActionCategory.SYSTEM -> true // System actions usually succeed
            ActionCategory.NAVIGATION -> true // Navigation actions usually succeed
            ActionCategory.APP -> action.contains("settings") || action.contains("camera") // Limited apps
            ActionCategory.DEVICE -> true // Device actions usually succeed
            ActionCategory.INPUT -> params.isNotEmpty() || action.contains(":") // Need parameters
            ActionCategory.UI -> true // UI actions usually succeed
            ActionCategory.GESTURE -> true // Gesture actions usually succeed
            ActionCategory.GAZE -> Math.random() < 0.9 // 90% success rate for gaze tracking
            ActionCategory.CUSTOM -> Math.random() < 0.8 // 80% success rate for custom
        }
        
        return Pair(success, category)
    }
    
    // Query methods for testing
    
    fun hasProcessedCommand(command: String): Boolean {
        return processedCommands.any { it.command == command || it.command == command.lowercase().trim() }
    }
    
    fun hasExecutedAction(action: String): Boolean {
        return executedActions.any { it.action == action }
    }
    
    fun getProcessedCommands(): List<ProcessedCommand> {
        return processedCommands.toList()
    }
    
    fun getExecutedActions(): List<ExecutedAction> {
        return executedActions.toList()
    }
    
    fun getCommandCount(): Int = commandCount.get()
    
    fun getActionCount(): Int = actionCount.get()
    
    fun getSuccessCount(): Int = successCount.get()
    
    fun getErrorCount(): Int = errorCount.get()
    
    fun getSuccessRate(): Float {
        val total = commandCount.get() + actionCount.get()
        return if (total > 0) successCount.get().toFloat() / total else 0f
    }
    
    // Metrics methods
    
    fun getMetrics(): Map<String, MetricData> {
        return metrics.toMap()
    }
    
    fun getMetricsForAction(action: String): MetricData? {
        return metrics[action]
    }
    
    private fun recordMetric(action: String, timeMs: Long, success: Boolean) {
        metrics.getOrPut(action) { MetricData() }.apply {
            count++
            totalTimeMs += timeMs
            lastExecutionMs = timeMs
            if (success) successCount++
        }
    }
    
    // Test utilities
    
    fun reset() {
        processedCommands.clear()
        executedActions.clear()
        metrics.clear()
        commandCount.set(0)
        actionCount.set(0)
        successCount.set(0)
        errorCount.set(0)
    }
    
    fun getDebugInfo(): String {
        return buildString {
            appendLine("MockActionCoordinator Debug Info")
            appendLine("Commands Processed: ${commandCount.get()}")
            appendLine("Actions Executed: ${actionCount.get()}")
            appendLine("Successes: ${successCount.get()}")
            appendLine("Errors: ${errorCount.get()}")
            appendLine("Success Rate: ${(getSuccessRate() * 100).toInt()}%")
            appendLine("Metrics Tracked: ${metrics.size}")
            
            if (processedCommands.isNotEmpty()) {
                appendLine("Recent Commands:")
                processedCommands.toList().takeLast(5).forEach { cmd ->
                    appendLine("  - '${cmd.command}' (${if (cmd.success) "SUCCESS" else "FAIL"}, ${cmd.executionTimeMs}ms)")
                }
            }
            
            if (executedActions.isNotEmpty()) {
                appendLine("Recent Actions:")
                executedActions.toList().takeLast(5).forEach { action ->
                    appendLine("  - '${action.action}' [${action.category}] (${if (action.success) "SUCCESS" else "FAIL"}, ${action.executionTimeMs}ms)")
                }
            }
        }
    }
}