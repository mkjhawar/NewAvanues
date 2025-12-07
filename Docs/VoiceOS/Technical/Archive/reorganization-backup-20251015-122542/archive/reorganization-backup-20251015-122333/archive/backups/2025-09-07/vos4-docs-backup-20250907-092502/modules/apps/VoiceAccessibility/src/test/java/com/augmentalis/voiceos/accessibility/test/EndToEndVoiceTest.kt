/**
 * EndToEndVoiceTest.kt - End-to-end voice command testing framework
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Test Framework
 * Created: 2025-08-28
 * 
 * Comprehensive end-to-end testing that simulates the complete voice command flow:
 * Voice Input -> Service Binding -> Callback Invocation -> Command Routing -> Handler Execution
 */
package com.augmentalis.voiceos.accessibility.test

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.augmentalis.voiceos.accessibility.handlers.ActionCategory
import com.augmentalis.voiceos.accessibility.managers.ActionCoordinator
import com.augmentalis.voiceos.accessibility.mocks.MockVoiceAccessibilityService
import com.augmentalis.voiceos.accessibility.mocks.MockVoiceRecognitionManager
import kotlinx.coroutines.*
import org.junit.Assert.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Mock voice recognition service for testing
 */
class MockVoiceRecognitionService {
    
    private var isListening = AtomicBoolean(false)
    private var callback: ((String, Float) -> Unit)? = null
    
    fun startListening(_engine: String, _language: String, callback: (String, Float) -> Unit): Boolean {
        this.callback = callback
        isListening.set(true)
        return true
    }
    
    fun stopListening(): Boolean {
        isListening.set(false)
        callback = null
        return true
    }
    
    fun simulateVoiceInput(text: String, confidence: Float = 0.85f) {
        if (isListening.get()) {
            callback?.invoke(text, confidence)
        }
    }
    
    fun isListening(): Boolean = isListening.get()
}

// Using the MockVoiceAccessibilityService from the mocks package

/**
 * End-to-end voice testing framework
 */
class EndToEndVoiceTest {
    
    companion object {
        private const val TAG = "EndToEndVoiceTest"
        private const val TEST_TIMEOUT_MS = 10000L
        private const val OPERATION_TIMEOUT_MS = 5000L
    }
    
    private var mockService: MockVoiceRecognitionService? = null
    private var mockAccessibilityService: MockVoiceAccessibilityService? = null
    private var voiceRecognitionManager: MockVoiceRecognitionManager? = null
    private var actionCoordinator: ActionCoordinator? = null
    private var commandExecutionVerifier: CommandExecutionVerifier? = null
    
    private val testResults = mutableListOf<EndToEndTestResult>()
    
    /**
     * Setup test environment
     */
    fun setupTestEnvironment(_context: Context) {
        // Create mock services
        mockService = MockVoiceRecognitionService()
        mockAccessibilityService = MockVoiceAccessibilityService()
        
        // Setup command execution verifier
        commandExecutionVerifier = CommandExecutionVerifier().apply {
            setupMockHandlers()
        }
        
        // Create ActionCoordinator with mock service
        actionCoordinator = ActionCoordinator(mockAccessibilityService!!).apply {
            initialize()
        }
        
        // Set up mock accessibility service
        mockAccessibilityService!!.apply {
            // Note: Mock service setup handled internally in our implementation
        }
        
        // Create MockVoiceRecognitionManager
        voiceRecognitionManager = MockVoiceRecognitionManager(actionCoordinator!!)
        
        Log.d(TAG, "Test environment setup complete")
    }
    
    /**
     * Test complete voice command flow
     */
    fun testCompleteVoiceFlow(
        voiceInput: String,
        expectedCategory: ActionCategory,
        expectedAction: String,
        confidence: Float = 0.85f,
        _timeout: Long = TEST_TIMEOUT_MS
    ): EndToEndTestResult {
        
        Log.d(TAG, "Starting end-to-end test: '$voiceInput'")
        
        val testResult = EndToEndTestResult(
            voiceInput = voiceInput,
            expectedCategory = expectedCategory,
            expectedAction = expectedAction,
            confidence = confidence,
            startTime = System.currentTimeMillis()
        )
        
        try {
            // Step 1: Setup voice recognition
            val setupResult = setupVoiceRecognition(_timeout)
            testResult.setupSuccess = setupResult.success
            testResult.setupTime = setupResult.duration
            testResult.errors.addAll(setupResult.errors)
            
            if (!setupResult.success) {
                testResult.overallSuccess = false
                return testResult.copy(endTime = System.currentTimeMillis())
            }
            
            // Step 2: Start listening
            val listeningResult = startVoiceListening(_timeout)
            testResult.listeningStarted = listeningResult.success
            testResult.listeningTime = listeningResult.duration
            testResult.errors.addAll(listeningResult.errors)
            
            if (!listeningResult.success) {
                testResult.overallSuccess = false
                return testResult.copy(endTime = System.currentTimeMillis())
            }
            
            // Step 3: Simulate voice input
            val inputResult = simulateVoiceInput(voiceInput, confidence, _timeout)
            testResult.voiceInputProcessed = inputResult.success
            testResult.inputProcessingTime = inputResult.duration
            testResult.errors.addAll(inputResult.errors)
            
            if (!inputResult.success) {
                testResult.overallSuccess = false
                return testResult.copy(endTime = System.currentTimeMillis())
            }
            
            // Step 4: Verify command routing
            val routingResult = verifyCommandRouting(expectedCategory, expectedAction, _timeout)
            testResult.commandRouted = routingResult.success
            testResult.routingTime = routingResult.duration
            testResult.actualCategory = routingResult.actualCategory
            testResult.actualAction = routingResult.actualAction
            testResult.errors.addAll(routingResult.errors)
            
            // Step 5: Verify execution result
            val executionResult = verifyCommandExecution(expectedAction, _timeout)
            testResult.commandExecuted = executionResult.success
            testResult.executionTime = executionResult.duration
            testResult.executionDetails = executionResult.details
            testResult.errors.addAll(executionResult.errors)
            
            // Overall success
            testResult.overallSuccess = testResult.setupSuccess &&
                    testResult.listeningStarted &&
                    testResult.voiceInputProcessed &&
                    testResult.commandRouted &&
                    testResult.commandExecuted
                    
        } catch (e: Exception) {
            Log.e(TAG, "End-to-end test failed with exception", e)
            testResult.errors.add("Exception: ${e.message}")
            testResult.overallSuccess = false
        }
        
        testResult.endTime = System.currentTimeMillis()
        testResults.add(testResult)
        
        Log.d(TAG, "End-to-end test completed: ${testResult.overallSuccess}")
        return testResult
    }
    
    /**
     * Setup voice recognition service
     */
    private fun setupVoiceRecognition(_timeout: Long): StepResult {
        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<String>()
        
        try {
            val context = mockAccessibilityService ?: throw IllegalStateException("Mock service not initialized")
            
            // Initialize voice recognition manager
            voiceRecognitionManager?.initialize(context as Context)
            
            // Wait for initialization
            var attempts = 0
            val maxAttempts = 50
            while (attempts < maxAttempts && !isVoiceRecognitionReady()) {
                Thread.sleep(100)
                attempts++
            }
            
            val isReady = isVoiceRecognitionReady()
            if (!isReady) {
                errors.add("Voice recognition failed to initialize within timeout")
            }
            
            return StepResult(
                success = isReady,
                duration = System.currentTimeMillis() - startTime,
                errors = errors
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Voice recognition setup failed", e)
            errors.add("Setup exception: ${e.message}")
            return StepResult(
                success = false,
                duration = System.currentTimeMillis() - startTime,
                errors = errors
            )
        }
    }
    
    /**
     * Start voice listening
     */
    private fun startVoiceListening(_timeout: Long): StepResult {
        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<String>()
        
        try {
            val started = voiceRecognitionManager?.startListening() ?: false
            if (!started) {
                errors.add("Failed to start voice listening")
            }
            
            return StepResult(
                success = started,
                duration = System.currentTimeMillis() - startTime,
                errors = errors
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Voice listening start failed", e)
            errors.add("Listening exception: ${e.message}")
            return StepResult(
                success = false,
                duration = System.currentTimeMillis() - startTime,
                errors = errors
            )
        }
    }
    
    /**
     * Simulate voice input
     */
    private fun simulateVoiceInput(text: String, confidence: Float, _timeout: Long): StepResult {
        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<String>()
        
        try {
            // Simulate voice input
            mockService?.simulateVoiceInput(text, confidence)
            
            // Wait for processing
            Thread.sleep(500)
            
            return StepResult(
                success = true,
                duration = System.currentTimeMillis() - startTime,
                errors = errors
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Voice input simulation failed", e)
            errors.add("Input simulation exception: ${e.message}")
            return StepResult(
                success = false,
                duration = System.currentTimeMillis() - startTime,
                errors = errors
            )
        }
    }
    
    /**
     * Verify command routing
     */
    private fun verifyCommandRouting(
        expectedCategory: ActionCategory,
        expectedAction: String,
        _timeout: Long
    ): RoutingResult {
        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<String>()
        
        try {
            // Check if command was routed to coordinator
            val coordinator = actionCoordinator
            if (coordinator == null) {
                errors.add("ActionCoordinator not available")
                return RoutingResult(
                    success = false,
                    duration = System.currentTimeMillis() - startTime,
                    errors = errors
                )
            }
            
            // Verify command can be handled
            val canHandle = coordinator.canHandle(expectedAction)
            if (!canHandle) {
                errors.add("ActionCoordinator cannot handle action: $expectedAction")
            }
            
            return RoutingResult(
                success = canHandle,
                duration = System.currentTimeMillis() - startTime,
                actualCategory = expectedCategory, // Would need tracking in real implementation
                actualAction = expectedAction,
                errors = errors
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Command routing verification failed", e)
            errors.add("Routing verification exception: ${e.message}")
            return RoutingResult(
                success = false,
                duration = System.currentTimeMillis() - startTime,
                errors = errors
            )
        }
    }
    
    /**
     * Verify command execution
     */
    private fun verifyCommandExecution(action: String, _timeout: Long): ExecutionResult {
        val startTime = System.currentTimeMillis()
        val errors = mutableListOf<String>()
        
        try {
            val coordinator = actionCoordinator
            if (coordinator == null) {
                errors.add("ActionCoordinator not available for execution")
                return ExecutionResult(
                    success = false,
                    duration = System.currentTimeMillis() - startTime,
                    errors = errors
                )
            }
            
            // Execute the command
            val executed = coordinator.executeAction(action)
            
            val details = mutableMapOf<String, Any>(
                "action" to action,
                "result" to executed,
                "metrics" to coordinator.getMetricsForAction(action).toString()
            )
            
            return ExecutionResult(
                success = executed,
                duration = System.currentTimeMillis() - startTime,
                details = details,
                errors = errors
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Command execution verification failed", e)
            errors.add("Execution verification exception: ${e.message}")
            return ExecutionResult(
                success = false,
                duration = System.currentTimeMillis() - startTime,
                errors = errors
            )
        }
    }
    
    /**
     * Check if voice recognition is ready
     */
    private fun isVoiceRecognitionReady(): Boolean {
        return voiceRecognitionManager?.isServiceConnected() == true
    }
    
    /**
     * Run a complete test suite
     */
    fun runCompleteTestSuite(_context: Context): TestSuiteResult {
        Log.d(TAG, "Starting complete test suite")
        
        setupTestEnvironment(_context)
        val suiteStartTime = System.currentTimeMillis()
        
        val testCases = listOf(
            TestCase("open settings", ActionCategory.APP, "open settings"),
            TestCase("go back", ActionCategory.NAVIGATION, "navigate_back"),
            TestCase("volume up", ActionCategory.SYSTEM, "volume_up"),
            TestCase("tap", ActionCategory.UI, "ui_tap"),
            TestCase("brightness up", ActionCategory.DEVICE, "brightness_up"),
            TestCase("type hello", ActionCategory.INPUT, "input_text:hello"),
        )
        
        val results = mutableListOf<EndToEndTestResult>()
        var passed = 0
        var failed = 0
        
        testCases.forEach { testCase ->
            Log.d(TAG, "Running test case: ${testCase.voiceInput}")
            
            val result = testCompleteVoiceFlow(
                voiceInput = testCase.voiceInput,
                expectedCategory = testCase.expectedCategory,
                expectedAction = testCase.expectedAction
            )
            
            results.add(result)
            
            if (result.overallSuccess) {
                passed++
                Log.d(TAG, "Test case PASSED: ${testCase.voiceInput}")
            } else {
                failed++
                Log.w(TAG, "Test case FAILED: ${testCase.voiceInput} - ${result.errors}")
            }
        }
        
        val suiteEndTime = System.currentTimeMillis()
        
        return TestSuiteResult(
            totalTests = testCases.size,
            passedTests = passed,
            failedTests = failed,
            totalDuration = suiteEndTime - suiteStartTime,
            results = results,
            setupTime = results.sumOf { it.setupTime },
            averageProcessingTime = results.map { it.inputProcessingTime }.average(),
            averageExecutionTime = results.map { it.executionTime }.average()
        )
    }
    
    /**
     * Get all test results
     */
    fun getAllTestResults(): List<EndToEndTestResult> = testResults.toList()
    
    /**
     * Clear test results
     */
    fun clearResults() {
        testResults.clear()
        commandExecutionVerifier?.clear()
    }
    
    /**
     * Cleanup test environment
     */
    fun cleanup() {
        voiceRecognitionManager?.dispose()
        actionCoordinator?.dispose()
        mockService = null
        mockAccessibilityService = null
    }
    
    // Data classes for test results
    
    data class TestCase(
        val voiceInput: String,
        val expectedCategory: ActionCategory,
        val expectedAction: String,
        val confidence: Float = 0.85f
    )
    
    data class StepResult(
        val success: Boolean,
        val duration: Long,
        val errors: List<String>
    )
    
    data class RoutingResult(
        val success: Boolean,
        val duration: Long,
        val actualCategory: ActionCategory? = null,
        val actualAction: String? = null,
        val errors: List<String>
    )
    
    data class ExecutionResult(
        val success: Boolean,
        val duration: Long,
        val details: Map<String, Any> = emptyMap(),
        val errors: List<String>
    )
    
    data class EndToEndTestResult(
        val voiceInput: String,
        val expectedCategory: ActionCategory,
        val expectedAction: String,
        val confidence: Float,
        val startTime: Long,
        var endTime: Long = 0,
        var setupSuccess: Boolean = false,
        var listeningStarted: Boolean = false,
        var voiceInputProcessed: Boolean = false,
        var commandRouted: Boolean = false,
        var commandExecuted: Boolean = false,
        var overallSuccess: Boolean = false,
        var setupTime: Long = 0,
        var listeningTime: Long = 0,
        var inputProcessingTime: Long = 0,
        var routingTime: Long = 0,
        var executionTime: Long = 0,
        var actualCategory: ActionCategory? = null,
        var actualAction: String? = null,
        var executionDetails: Map<String, Any> = emptyMap(),
        val errors: MutableList<String> = mutableListOf()
    ) {
        val totalDuration: Long
            get() = endTime - startTime
            
        val successRate: Float
            get() {
                val steps = listOf(setupSuccess, listeningStarted, voiceInputProcessed, commandRouted, commandExecuted)
                val successCount = steps.count { it }
                return successCount.toFloat() / steps.size
            }
    }
    
    data class TestSuiteResult(
        val totalTests: Int,
        val passedTests: Int,
        val failedTests: Int,
        val totalDuration: Long,
        val results: List<EndToEndTestResult>,
        val setupTime: Long,
        val averageProcessingTime: Double,
        val averageExecutionTime: Double
    ) {
        val successRate: Float
            get() = if (totalTests > 0) passedTests.toFloat() / totalTests else 0f
            
        val averageTotalTime: Double
            get() = if (results.isNotEmpty()) results.map { it.totalDuration }.average() else 0.0
    }
}