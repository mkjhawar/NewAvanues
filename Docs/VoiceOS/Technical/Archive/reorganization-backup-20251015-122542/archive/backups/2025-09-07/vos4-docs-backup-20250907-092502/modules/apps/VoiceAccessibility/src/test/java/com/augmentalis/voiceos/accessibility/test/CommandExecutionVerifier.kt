/**
 * CommandExecutionVerifier.kt - Verification system for command execution testing
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Test Framework
 * Created: 2025-08-28
 * 
 * Mock system for testing command execution with AccessibilityNodeInfo mocking,
 * handler invocation tracking, parameter verification, and result checking.
 */
package com.augmentalis.voiceos.accessibility.test

import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceos.accessibility.handlers.ActionCategory
import com.augmentalis.voiceos.accessibility.handlers.ActionHandler
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Mock AccessibilityNodeInfo for UI command testing
 */
class MockAccessibilityNodeInfo private constructor() : AccessibilityNodeInfo() {
    
    companion object {
        fun createMock(
            text: String = "Mock Node",
            className: String = "android.widget.TextView",
            isClickable: Boolean = true,
            isScrollable: Boolean = false,
            bounds: android.graphics.Rect = android.graphics.Rect(0, 0, 100, 50)
        ): MockAccessibilityNodeInfo {
            return MockAccessibilityNodeInfo().apply {
                this.text = text
                this.className = className
                this.isClickable = isClickable
                this.isScrollable = isScrollable
                setBoundsInScreen(bounds)
            }
        }
        
        fun createScrollableList(itemCount: Int = 10): List<MockAccessibilityNodeInfo> {
            return (1..itemCount).map { index ->
                createMock(
                    text = "Item $index",
                    isScrollable = true,
                    bounds = android.graphics.Rect(0, index * 50, 200, (index + 1) * 50)
                )
            }
        }
        
        fun createButtonGrid(rows: Int = 3, cols: Int = 3): List<MockAccessibilityNodeInfo> {
            val buttons = mutableListOf<MockAccessibilityNodeInfo>()
            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    buttons.add(
                        createMock(
                            text = "Button ${row}-${col}",
                            className = "android.widget.Button",
                            isClickable = true,
                            bounds = android.graphics.Rect(
                                col * 100, row * 50,
                                (col + 1) * 100, (row + 1) * 50
                            )
                        )
                    )
                }
            }
            return buttons
        }
    }
}

/**
 * Handler invocation tracking system
 */
class HandlerInvocationTracker {
    
    data class Invocation(
        val handlerClass: String,
        val category: ActionCategory,
        val action: String,
        val params: Map<String, Any>,
        val timestamp: Long = System.currentTimeMillis(),
        val result: Boolean? = null
    )
    
    private val invocations = mutableListOf<Invocation>()
    private val handlerCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val categoryCounts = ConcurrentHashMap<ActionCategory, AtomicInteger>()
    
    /**
     * Track a handler invocation
     */
    fun trackInvocation(
        handler: ActionHandler,
        category: ActionCategory,
        action: String,
        params: Map<String, Any>,
        result: Boolean
    ) {
        synchronized(invocations) {
            val invocation = Invocation(
                handlerClass = handler.javaClass.simpleName,
                category = category,
                action = action,
                params = params,
                result = result
            )
            invocations.add(invocation)
            
            // Update counters
            handlerCounts.getOrPut(invocation.handlerClass) { AtomicInteger(0) }.incrementAndGet()
            categoryCounts.getOrPut(category) { AtomicInteger(0) }.incrementAndGet()
        }
    }
    
    /**
     * Get all invocations
     */
    fun getAllInvocations(): List<Invocation> {
        synchronized(invocations) {
            return invocations.toList()
        }
    }
    
    /**
     * Get invocations for a specific handler
     */
    fun getInvocationsForHandler(handlerClass: String): List<Invocation> {
        synchronized(invocations) {
            return invocations.filter { it.handlerClass == handlerClass }
        }
    }
    
    /**
     * Get invocations for a specific category
     */
    fun getInvocationsForCategory(category: ActionCategory): List<Invocation> {
        synchronized(invocations) {
            return invocations.filter { it.category == category }
        }
    }
    
    /**
     * Get invocations for a specific action
     */
    fun getInvocationsForAction(action: String): List<Invocation> {
        synchronized(invocations) {
            return invocations.filter { it.action == action }
        }
    }
    
    /**
     * Check if handler was invoked
     */
    fun wasHandlerInvoked(handlerClass: String): Boolean {
        return handlerCounts.containsKey(handlerClass)
    }
    
    /**
     * Get invocation count for handler
     */
    fun getHandlerInvocationCount(handlerClass: String): Int {
        return handlerCounts[handlerClass]?.get() ?: 0
    }
    
    /**
     * Get invocation count for category
     */
    fun getCategoryInvocationCount(category: ActionCategory): Int {
        return categoryCounts[category]?.get() ?: 0
    }
    
    /**
     * Get successful invocations count
     */
    fun getSuccessfulInvocations(): Int {
        synchronized(invocations) {
            return invocations.count { it.result == true }
        }
    }
    
    /**
     * Get failed invocations count
     */
    fun getFailedInvocations(): Int {
        synchronized(invocations) {
            return invocations.count { it.result == false }
        }
    }
    
    /**
     * Get last invocation for handler
     */
    fun getLastInvocationForHandler(handlerClass: String): Invocation? {
        synchronized(invocations) {
            return invocations.filter { it.handlerClass == handlerClass }.lastOrNull()
        }
    }
    
    /**
     * Clear all tracking data
     */
    fun clear() {
        synchronized(invocations) {
            invocations.clear()
            handlerCounts.clear()
            categoryCounts.clear()
        }
    }
    
    /**
     * Get tracking statistics
     */
    fun getStatistics(): TrackingStatistics {
        synchronized(invocations) {
            return TrackingStatistics(
                totalInvocations = invocations.size,
                successfulInvocations = getSuccessfulInvocations(),
                failedInvocations = getFailedInvocations(),
                uniqueHandlers = handlerCounts.size,
                uniqueCategories = categoryCounts.size,
                uniqueActions = invocations.map { it.action }.distinct().size,
                handlerBreakdown = handlerCounts.mapValues { it.value.get() },
                categoryBreakdown = categoryCounts.mapValues { it.value.get() }
            )
        }
    }
    
    data class TrackingStatistics(
        val totalInvocations: Int,
        val successfulInvocations: Int,
        val failedInvocations: Int,
        val uniqueHandlers: Int,
        val uniqueCategories: Int,
        val uniqueActions: Int,
        val handlerBreakdown: Map<String, Int>,
        val categoryBreakdown: Map<ActionCategory, Int>
    ) {
        val successRate: Float
            get() = if (totalInvocations > 0) successfulInvocations.toFloat() / totalInvocations else 0f
    }
}

/**
 * Parameter verification system
 */
class ParameterVerifier {
    
    data class ParameterExpectation(
        val key: String,
        val expectedValue: Any?,
        val expectedType: Class<*>? = null,
        val required: Boolean = true
    )
    
    data class VerificationResult(
        val passed: Boolean,
        val errors: List<String> = emptyList(),
        val actualParams: Map<String, Any> = emptyMap()
    )
    
    /**
     * Verify parameters against expectations
     */
    fun verifyParameters(
        actualParams: Map<String, Any>,
        expectations: List<ParameterExpectation>
    ): VerificationResult {
        val errors = mutableListOf<String>()
        
        // Check required parameters
        expectations.filter { it.required }.forEach { expectation ->
            if (!actualParams.containsKey(expectation.key)) {
                errors.add("Missing required parameter: ${expectation.key}")
            }
        }
        
        // Check parameter values and types
        expectations.forEach { expectation ->
            val actualValue = actualParams[expectation.key]
            
            if (actualValue != null) {
                // Type check
                expectation.expectedType?.let { expectedType ->
                    if (!expectedType.isInstance(actualValue)) {
                        errors.add("Parameter ${expectation.key} expected type ${expectedType.simpleName}, got ${actualValue.javaClass.simpleName}")
                    }
                }
                
                // Value check
                expectation.expectedValue?.let { expectedValue ->
                    if (actualValue != expectedValue) {
                        errors.add("Parameter ${expectation.key} expected value '$expectedValue', got '$actualValue'")
                    }
                }
            }
        }
        
        return VerificationResult(
            passed = errors.isEmpty(),
            errors = errors,
            actualParams = actualParams
        )
    }
    
    /**
     * Create parameter expectations for common scenarios
     */
    object ExpectationBuilder {
        
        fun textInput(expectedText: String) = listOf(
            ParameterExpectation("text", expectedText, String::class.java)
        )
        
        fun appLaunch(expectedPackage: String) = listOf(
            ParameterExpectation("packageName", expectedPackage, String::class.java)
        )
        
        fun uiCoordinates(x: Int, y: Int) = listOf(
            ParameterExpectation("x", x, Int::class.java),
            ParameterExpectation("y", y, Int::class.java)
        )
        
        fun voiceCommand(confidence: Float, originalText: String) = listOf(
            ParameterExpectation("confidence", confidence, Float::class.java),
            ParameterExpectation("originalText", originalText, String::class.java),
            ParameterExpectation("source", "voice", String::class.java)
        )
        
        fun scrollDirection(direction: String, amount: Int = 1) = listOf(
            ParameterExpectation("direction", direction, String::class.java),
            ParameterExpectation("amount", amount, Int::class.java)
        )
        
        fun volumeLevel(level: Int) = listOf(
            ParameterExpectation("level", level, Int::class.java)
        )
    }
}

/**
 * Execution result verification system
 */
class ExecutionResultVerifier {
    
    data class ExecutionExpectation(
        val shouldSucceed: Boolean = true,
        val expectedDuration: LongRange? = null,
        val expectedSideEffects: List<String> = emptyList(),
        val expectedStateChanges: Map<String, Any> = emptyMap()
    )
    
    data class ExecutionResult(
        val success: Boolean,
        val duration: Long,
        val sideEffects: List<String> = emptyList(),
        val stateChanges: Map<String, Any> = emptyMap(),
        val error: Exception? = null
    )
    
    data class ResultVerification(
        val passed: Boolean,
        val errors: List<String> = emptyList(),
        val actualResult: ExecutionResult
    )
    
    /**
     * Verify execution result against expectations
     */
    fun verifyResult(
        actualResult: ExecutionResult,
        expectation: ExecutionExpectation
    ): ResultVerification {
        val errors = mutableListOf<String>()
        
        // Check success/failure expectation
        if (actualResult.success != expectation.shouldSucceed) {
            errors.add("Expected success: ${expectation.shouldSucceed}, got: ${actualResult.success}")
        }
        
        // Check duration if specified
        expectation.expectedDuration?.let { expectedRange ->
            if (actualResult.duration !in expectedRange) {
                errors.add("Duration ${actualResult.duration}ms not in expected range ${expectedRange}")
            }
        }
        
        // Check side effects
        expectation.expectedSideEffects.forEach { expectedEffect ->
            if (!actualResult.sideEffects.contains(expectedEffect)) {
                errors.add("Missing expected side effect: $expectedEffect")
            }
        }
        
        // Check state changes
        expectation.expectedStateChanges.forEach { (key, expectedValue) ->
            val actualValue = actualResult.stateChanges[key]
            if (actualValue != expectedValue) {
                errors.add("State change '$key' expected '$expectedValue', got '$actualValue'")
            }
        }
        
        return ResultVerification(
            passed = errors.isEmpty(),
            errors = errors,
            actualResult = actualResult
        )
    }
}

/**
 * Mock handler for testing
 */
class MockActionHandler(
    private val handlerName: String,
    private val supportedActions: List<String>,
    private val defaultResult: Boolean = true,
    private val simulatedDelay: Long = 0L
) : ActionHandler {
    
    private val tracker = HandlerInvocationTracker()
    private val executionTimes = mutableListOf<Long>()
    
    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean {
        val startTime = System.currentTimeMillis()
        
        // Simulate processing delay
        if (simulatedDelay > 0) {
            Thread.sleep(simulatedDelay)
        }
        
        val result = when {
            !canHandle(action) -> false
            action.contains("fail") -> false
            action.contains("error") -> throw RuntimeException("Simulated error")
            else -> defaultResult
        }
        
        val executionTime = System.currentTimeMillis() - startTime
        executionTimes.add(executionTime)
        
        tracker.trackInvocation(this, category, action, params, result)
        
        return result
    }
    
    override fun canHandle(action: String): Boolean {
        return supportedActions.any { action.contains(it, ignoreCase = true) }
    }
    
    override fun getSupportedActions(): List<String> {
        return supportedActions
    }
    
    fun getTracker(): HandlerInvocationTracker = tracker
    
    fun getAverageExecutionTime(): Double {
        return if (executionTimes.isNotEmpty()) {
            executionTimes.average()
        } else 0.0
    }
    
    fun getMaxExecutionTime(): Long = executionTimes.maxOrNull() ?: 0L
    
    fun getMinExecutionTime(): Long = executionTimes.minOrNull() ?: 0L
    
    fun clearStats() {
        tracker.clear()
        executionTimes.clear()
    }
}

/**
 * Main command execution verifier
 */
class CommandExecutionVerifier {
    
    private val invocationTracker = HandlerInvocationTracker()
    private val parameterVerifier = ParameterVerifier()
    private val resultVerifier = ExecutionResultVerifier()
    private val mockHandlers = mutableMapOf<ActionCategory, MockActionHandler>()
    
    /**
     * Setup mock handlers for testing
     */
    fun setupMockHandlers() {
        mockHandlers[ActionCategory.APP] = MockActionHandler(
            "MockAppHandler",
            listOf("open", "launch", "start", "close", "switch")
        )
        
        mockHandlers[ActionCategory.NAVIGATION] = MockActionHandler(
            "MockNavigationHandler",
            listOf("back", "home", "scroll", "swipe")
        )
        
        mockHandlers[ActionCategory.SYSTEM] = MockActionHandler(
            "MockSystemHandler",
            listOf("volume", "mute", "recent", "notifications")
        )
        
        mockHandlers[ActionCategory.UI] = MockActionHandler(
            "MockUIHandler",
            listOf("tap", "click", "swipe", "drag")
        )
        
        mockHandlers[ActionCategory.DEVICE] = MockActionHandler(
            "MockDeviceHandler",
            listOf("brightness", "wifi", "bluetooth")
        )
        
        mockHandlers[ActionCategory.INPUT] = MockActionHandler(
            "MockInputHandler",
            listOf("type", "enter", "delete", "clear")
        )
    }
    
    /**
     * Verify command execution
     */
    fun verifyCommandExecution(
        category: ActionCategory,
        action: String,
        params: Map<String, Any> = emptyMap(),
        parameterExpectations: List<ParameterVerifier.ParameterExpectation> = emptyList(),
        executionExpectation: ExecutionResultVerifier.ExecutionExpectation = ExecutionResultVerifier.ExecutionExpectation()
    ): CommandVerificationResult {
        
        val handler = mockHandlers[category]
            ?: return CommandVerificationResult(
                success = false,
                errors = listOf("No handler found for category: $category")
            )
        
        val startTime = System.currentTimeMillis()
        var executionResult: ExecutionResultVerifier.ExecutionResult
        
        try {
            val result = handler.execute(category, action, params)
            val duration = System.currentTimeMillis() - startTime
            
            executionResult = ExecutionResultVerifier.ExecutionResult(
                success = result,
                duration = duration
            )
            
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            executionResult = ExecutionResultVerifier.ExecutionResult(
                success = false,
                duration = duration,
                error = e
            )
        }
        
        // Verify parameters
        val paramVerification = parameterVerifier.verifyParameters(params, parameterExpectations)
        
        // Verify execution result
        val resultVerification = resultVerifier.verifyResult(executionResult, executionExpectation)
        
        val allErrors = mutableListOf<String>()
        allErrors.addAll(paramVerification.errors)
        allErrors.addAll(resultVerification.errors)
        
        return CommandVerificationResult(
            success = paramVerification.passed && resultVerification.passed,
            errors = allErrors,
            executionResult = executionResult,
            parameterVerification = paramVerification,
            resultVerification = resultVerification,
            handlerInvocations = handler.getTracker().getAllInvocations()
        )
    }
    
    /**
     * Get comprehensive verification stats
     */
    fun getVerificationStats(): VerificationStats {
        val allInvocations = mockHandlers.values.flatMap { it.getTracker().getAllInvocations() }
        val totalExecutions = allInvocations.size
        val successfulExecutions = allInvocations.count { it.result == true }
        val failedExecutions = allInvocations.count { it.result == false }
        
        return VerificationStats(
            totalExecutions = totalExecutions,
            successfulExecutions = successfulExecutions,
            failedExecutions = failedExecutions,
            handlerStats = mockHandlers.mapValues { (_, handler) ->
                HandlerStats(
                    invocations = handler.getTracker().getAllInvocations().size,
                    averageExecutionTime = handler.getAverageExecutionTime(),
                    maxExecutionTime = handler.getMaxExecutionTime(),
                    minExecutionTime = handler.getMinExecutionTime()
                )
            }
        )
    }
    
    /**
     * Clear all verification data
     */
    fun clear() {
        mockHandlers.values.forEach { it.clearStats() }
        invocationTracker.clear()
    }
    
    data class CommandVerificationResult(
        val success: Boolean,
        val errors: List<String> = emptyList(),
        val executionResult: ExecutionResultVerifier.ExecutionResult? = null,
        val parameterVerification: ParameterVerifier.VerificationResult? = null,
        val resultVerification: ExecutionResultVerifier.ResultVerification? = null,
        val handlerInvocations: List<HandlerInvocationTracker.Invocation> = emptyList()
    )
    
    data class VerificationStats(
        val totalExecutions: Int,
        val successfulExecutions: Int,
        val failedExecutions: Int,
        val handlerStats: Map<ActionCategory, HandlerStats>
    ) {
        val successRate: Float
            get() = if (totalExecutions > 0) successfulExecutions.toFloat() / totalExecutions else 0f
    }
    
    data class HandlerStats(
        val invocations: Int,
        val averageExecutionTime: Double,
        val maxExecutionTime: Long,
        val minExecutionTime: Long
    )
}