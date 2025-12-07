/**
 * TestUtils.kt - Comprehensive Test Utilities for VoiceAccessibility
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-08-28
 * 
 * Provides comprehensive testing utilities for service binding, callbacks,
 * performance measurement, and test data generation for VoiceAccessibility app.
 * 
 * VOS4 Standards Compliance:
 * - Direct implementation pattern
 * - Performance optimized utilities
 * - Android testing best practices
 * - Comprehensive error handling
 */

package com.augmentalis.voiceos.accessibility.test

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import com.augmentalis.voicerecognition.IRecognitionCallback
import com.augmentalis.voicerecognition.IVoiceRecognitionService
import com.augmentalis.voicerecognition.RecognitionData
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

/**
 * Comprehensive test utilities for VoiceAccessibility integration testing
 */
object TestUtils {
    
    private const val TAG = "VoiceAccessibility_TestUtils"
    
    // Test timeouts (in milliseconds)
    object Timeouts {
        const val SERVICE_BIND = 5000L
        const val SERVICE_UNBIND = 3000L
        const val CALLBACK_RESPONSE = 2000L
        const val RECOGNITION_COMPLETE = 10000L
        const val PERFORMANCE_TEST = 15000L
        const val ASYNC_OPERATION = 5000L
        const val BACKGROUND_OPERATION = 30000L
    }
    
    // Service Binding Helpers
    
    /**
     * Service binding helper with comprehensive lifecycle management
     */
    class ServiceBindingHelper(
        private val context: Context,
        private val packageName: String = "com.augmentalis.voicerecognition"
    ) {
        
        private var serviceConnection: ServiceConnection? = null
        private var recognitionService: IVoiceRecognitionService? = null
        private val bindLatch = CountDownLatch(1)
        private val unbindLatch = CountDownLatch(1)
        private val isConnected = AtomicBoolean(false)
        private val connectionError = AtomicReference<String?>(null)
        
        /**
         * Bind to VoiceRecognition service with timeout
         */
        suspend fun bindService(timeoutMs: Long = Timeouts.SERVICE_BIND): IVoiceRecognitionService? {
            return withTimeoutOrNull(timeoutMs) {
                withContext(Dispatchers.Main) {
                    val intent = Intent().apply {
                        setClassName(packageName, "$packageName.service.VoiceRecognitionService")
                        action = "com.augmentalis.voicerecognition.SERVICE"
                    }
                    
                    serviceConnection = object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                            try {
                                recognitionService = IVoiceRecognitionService.Stub.asInterface(service)
                                isConnected.set(true)
                                bindLatch.countDown()
                                Log.d(TAG, "Service connected successfully")
                            } catch (e: Exception) {
                                connectionError.set("Service connection error: ${e.message}")
                                bindLatch.countDown()
                            }
                        }
                        
                        override fun onServiceDisconnected(name: ComponentName?) {
                            recognitionService = null
                            isConnected.set(false)
                            Log.d(TAG, "Service disconnected")
                        }
                        
                        override fun onBindingDied(name: ComponentName?) {
                            connectionError.set("Service binding died")
                            // Note: unbindService is a suspend function, handle cleanup directly
                            try {
                                context.unbindService(this)
                            } catch (e: Exception) {
                                Log.w(TAG, "Error during binding died cleanup: ${e.message}")
                            }
                            Log.w(TAG, "Service binding died")
                        }
                        
                        override fun onNullBinding(name: ComponentName?) {
                            connectionError.set("Service returned null binding")
                            Log.e(TAG, "Service returned null binding")
                        }
                    }
                    
                    val bindResult = context.bindService(intent, serviceConnection!!, Context.BIND_AUTO_CREATE)
                    if (!bindResult) {
                        connectionError.set("Failed to bind to service")
                        bindLatch.countDown()
                    }
                }
                
                // Wait for binding with timeout
                if (bindLatch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                    connectionError.get()?.let { error ->
                        throw RuntimeException("Service binding failed: $error")
                    }
                    recognitionService
                } else {
                    throw RuntimeException("Service binding timed out after ${timeoutMs}ms")
                }
            }
        }
        
        /**
         * Unbind from service
         */
        suspend fun unbindService(timeoutMs: Long = Timeouts.SERVICE_UNBIND) {
            withTimeoutOrNull(timeoutMs) {
                withContext(Dispatchers.Main) {
                    serviceConnection?.let { connection ->
                        try {
                            context.unbindService(connection)
                            isConnected.set(false)
                            recognitionService = null
                            Log.d(TAG, "Service unbound successfully")
                        } catch (e: Exception) {
                            Log.w(TAG, "Error unbinding service: ${e.message}")
                        }
                    }
                }
            }
        }
        
        /**
         * Check if service is currently connected
         */
        fun isServiceConnected(): Boolean = isConnected.get()
        
        /**
         * Get current service connection error if any
         */
        fun getConnectionError(): String? = connectionError.get()
        
        /**
         * Reset binding state for reuse
         */
        fun reset() {
            // Note: unbindService is a suspend function, so we handle cleanup synchronously here
            serviceConnection?.let { 
                try {
                    context.unbindService(it)
                } catch (e: Exception) {
                    Log.w(TAG, "Error unbinding service during reset: ${e.message}")
                }
            }
            serviceConnection = null
            recognitionService = null
            isConnected.set(false)
            connectionError.set(null)
        }
    }
    
    // Callback Verification Utilities
    
    /**
     * Callback verification helper for testing recognition callbacks
     */
    class CallbackVerificationHelper {
        
        private val callbackResults = Channel<CallbackResult>(capacity = Channel.UNLIMITED)
        private val callbackCount = AtomicInteger(0)
        private val errorCount = AtomicInteger(0)
        private val successCount = AtomicInteger(0)
        
        data class CallbackResult(
            val type: CallbackType,
            val data: RecognitionData? = null,
            val error: String? = null,
            val timestamp: Long = System.currentTimeMillis()
        )
        
        enum class CallbackType {
            RECOGNITION_START,
            RECOGNITION_PARTIAL,
            RECOGNITION_COMPLETE,
            RECOGNITION_ERROR,
            RECOGNITION_TIMEOUT
        }
        
        /**
         * Create test callback that captures all results
         */
        fun createTestCallback(): IRecognitionCallback {
            return object : IRecognitionCallback.Stub() {
                override fun onRecognitionResult(text: String?, confidence: Float, isFinal: Boolean) {
                    callbackCount.incrementAndGet()
                    if (isFinal) {
                        successCount.incrementAndGet()
                        callbackResults.trySend(CallbackResult(CallbackType.RECOGNITION_COMPLETE, 
                            RecognitionData(
                                text = text ?: "",
                                confidence = confidence,
                                timestamp = System.currentTimeMillis(),
                                engineUsed = "test",
                                isFinal = true
                            )))
                        Log.d(TAG, "Callback: Recognition complete - $text (confidence: $confidence)")
                    } else {
                        callbackResults.trySend(CallbackResult(CallbackType.RECOGNITION_PARTIAL,
                            RecognitionData(
                                text = text ?: "",
                                confidence = confidence,
                                timestamp = System.currentTimeMillis(),
                                engineUsed = "test",
                                isFinal = false
                            )))
                        Log.d(TAG, "Callback: Partial result - $text (confidence: $confidence)")
                    }
                }
                
                override fun onError(errorCode: Int, message: String?) {
                    callbackCount.incrementAndGet()
                    errorCount.incrementAndGet()
                    callbackResults.trySend(CallbackResult(CallbackType.RECOGNITION_ERROR, 
                        error = "Code: $errorCode, Message: $message"))
                    Log.e(TAG, "Callback: Recognition error - $errorCode: $message")
                }
                
                override fun onStateChanged(state: Int, message: String?) {
                    callbackCount.incrementAndGet()
                    if (state == 1) { // listening state
                        callbackResults.trySend(CallbackResult(CallbackType.RECOGNITION_START))
                        Log.d(TAG, "Callback: Recognition started - $message")
                    }
                }
                
                override fun onPartialResult(partialText: String?) {
                    callbackCount.incrementAndGet()
                    callbackResults.trySend(CallbackResult(CallbackType.RECOGNITION_PARTIAL,
                        RecognitionData(
                            text = partialText ?: "",
                            confidence = 0.0f,
                            timestamp = System.currentTimeMillis(),
                            engineUsed = "test",
                            isFinal = false
                        )))
                    Log.d(TAG, "Callback: Partial result - $partialText")
                }
            }
        }
        
        /**
         * Wait for specific callback with timeout
         */
        suspend fun waitForCallback(
            expectedType: CallbackType,
            timeoutMs: Long = Timeouts.CALLBACK_RESPONSE
        ): CallbackResult? {
            return try {
                withTimeoutOrNull(timeoutMs) {
                    while (true) {
                        val result = callbackResults.receive()
                        if (result.type == expectedType) {
                            return@withTimeoutOrNull result
                        }
                    }
                    @Suppress("UNREACHABLE_CODE")
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
        
        /**
         * Wait for any callback within timeout
         */
        suspend fun waitForAnyCallback(timeoutMs: Long = Timeouts.CALLBACK_RESPONSE): CallbackResult? {
            return withTimeoutOrNull(timeoutMs) {
                callbackResults.receive()
            }
        }
        
        /**
         * Verify callback sequence matches expected pattern
         */
        suspend fun verifyCallbackSequence(
            expectedSequence: List<CallbackType>,
            timeoutMs: Long = Timeouts.RECOGNITION_COMPLETE
        ): Boolean {
            return try {
                withTimeoutOrNull(timeoutMs) {
                    val receivedSequence = mutableListOf<CallbackType>()
                    
                    repeat(expectedSequence.size) {
                        val result = callbackResults.receive()
                        receivedSequence.add(result.type)
                    }
                    
                    receivedSequence == expectedSequence
                } ?: false
            } catch (e: Exception) {
                false
            }
        }
        
        /**
         * Get callback statistics
         */
        fun getStatistics(): CallbackStatistics {
            return CallbackStatistics(
                totalCallbacks = callbackCount.get(),
                successCallbacks = successCount.get(),
                errorCallbacks = errorCount.get()
            )
        }
        
        data class CallbackStatistics(
            val totalCallbacks: Int,
            val successCallbacks: Int,
            val errorCallbacks: Int
        ) {
            val successRate: Double = if (totalCallbacks > 0) successCallbacks.toDouble() / totalCallbacks else 0.0
            val errorRate: Double = if (totalCallbacks > 0) errorCallbacks.toDouble() / totalCallbacks else 0.0
        }
        
        /**
         * Reset callback state for new test
         */
        fun reset() {
            callbackCount.set(0)
            errorCount.set(0)
            successCount.set(0)
            // Drain existing results
            while (!callbackResults.isEmpty) {
                callbackResults.tryReceive()
            }
        }
    }
    
    // Timeout Handlers
    
    /**
     * Advanced timeout handler for async operations
     */
    class TimeoutHandler {
        
        /**
         * Execute operation with timeout and retry logic
         */
        suspend fun <T> executeWithTimeout(
            timeoutMs: Long,
            retries: Int = 3,
            operation: suspend () -> T
        ): Result<T> {
            var lastException: Exception? = null
            
            repeat(retries) { attempt ->
                try {
                    val result = withTimeout(timeoutMs) {
                        operation()
                    }
                    return Result.success(result)
                } catch (e: TimeoutCancellationException) {
                    lastException = e
                    Log.w(TAG, "Operation timed out on attempt ${attempt + 1}/$retries")
                } catch (e: Exception) {
                    lastException = e
                    Log.w(TAG, "Operation failed on attempt ${attempt + 1}/$retries: ${e.message}")
                }
                
                // Wait before retry (exponential backoff)
                if (attempt < retries - 1) {
                    delay(100L * (1L shl attempt)) // 100ms, 200ms, 400ms, etc.
                }
            }
            
            return Result.failure(lastException ?: RuntimeException("Operation failed after $retries retries"))
        }
        
        /**
         * Execute multiple operations concurrently with individual timeouts
         */
        suspend fun <T> executeConcurrentWithTimeouts(
            operations: List<suspend () -> T>,
            timeoutMs: Long
        ): List<Result<T>> = coroutineScope {
            operations.map { operation ->
                async {
                    try {
                        val result = withTimeout(timeoutMs) {
                            operation()
                        }
                        Result.success(result)
                    } catch (e: Exception) {
                        Result.failure(e)
                    }
                }
            }.awaitAll()
        }
    }
    
    // Test Data Generators
    
    /**
     * Comprehensive test data generator
     */
    object TestDataGenerator {
        
        private val sampleCommands = listOf(
            "open settings", "close app", "scroll down", "go back", "click button",
            "navigate home", "open keyboard", "select all", "copy text", "paste text",
            "increase volume", "decrease brightness", "open camera", "take screenshot",
            "enable wifi", "disable bluetooth", "open notifications", "clear cache"
        )
        
        private val sampleResults = listOf(
            "Command executed successfully",
            "Action completed",
            "Navigation successful",
            "Settings updated",
            "Operation finished"
        )
        
        /**
         * Generate random RecognitionData for testing
         */
        fun generateRecognitionData(
            includePartialResults: Boolean = true,
            confidence: Float? = null
        ): RecognitionData {
            return RecognitionData(
                text = sampleCommands.random(),
                confidence = confidence ?: Random.nextFloat(),
                timestamp = System.currentTimeMillis(),
                engineUsed = listOf("vosk", "vivoka", "google_stt", "google_cloud").random(),
                isFinal = !(includePartialResults && Random.nextBoolean())
            )
        }
        
        /**
         * Generate series of recognition data for sequence testing
         */
        fun generateRecognitionSequence(count: Int): List<RecognitionData> {
            return (1..count).map { index ->
                RecognitionData(
                    text = if (index < count) sampleCommands.random() else sampleResults.random(),
                    confidence = Random.nextFloat() * 0.5f + 0.5f, // 0.5-1.0 range
                    timestamp = System.currentTimeMillis() + (index * 100L),
                    engineUsed = listOf("vosk", "vivoka", "google_stt").random(),
                    isFinal = index >= count - 1
                )
            }
        }
        
        /**
         * Generate error scenarios for testing
         */
        fun generateErrorScenarios(): List<Pair<Int, String>> {
            return listOf(
                1001 to "Network connection error",
                1002 to "Audio recording permission denied",
                1003 to "Recognition engine not available",
                1004 to "Timeout waiting for response",
                1005 to "Invalid audio format",
                1006 to "Engine initialization failed",
                1007 to "Service temporarily unavailable"
            )
        }
        
        /**
         * Generate Bundle for service extras
         */
        fun generateServiceBundle(): Bundle {
            return Bundle().apply {
                putString("test_id", "test_${System.currentTimeMillis()}")
                putBoolean("enable_logging", true)
                putInt("timeout_ms", 5000)
                putFloat("confidence_threshold", 0.7f)
                putStringArray("engines", arrayOf("vosk", "vivoka"))
            }
        }
        
        /**
         * Generate performance test parameters
         */
        fun generatePerformanceTestParams(): PerformanceTestParams {
            return PerformanceTestParams(
                commandCount = Random.nextInt(10, 100),
                concurrentConnections = Random.nextInt(1, 5),
                testDurationMs = Random.nextLong(5000, 30000),
                targetLatencyMs = Random.nextLong(50, 200),
                memoryLimitMB = Random.nextInt(10, 50)
            )
        }
        
        data class PerformanceTestParams(
            val commandCount: Int,
            val concurrentConnections: Int,
            val testDurationMs: Long,
            val targetLatencyMs: Long,
            val memoryLimitMB: Int
        )
    }
    
    // Performance Measurement Tools
    
    /**
     * Comprehensive performance measurement utilities
     */
    class PerformanceMeasurement {
        
        private val measurements = mutableMapOf<String, MutableList<Long>>()
        private val startTimes = mutableMapOf<String, Long>()
        private val memoryUsage = AtomicLong(0L)
        private val operationCounts = mutableMapOf<String, AtomicInteger>()
        
        /**
         * Start timing an operation
         */
        fun startTiming(operationName: String) {
            startTimes[operationName] = System.nanoTime()
            operationCounts.getOrPut(operationName) { AtomicInteger(0) }.incrementAndGet()
        }
        
        /**
         * Stop timing and record measurement
         */
        fun stopTiming(operationName: String): Long {
            val endTime = System.nanoTime()
            val startTime = startTimes[operationName] ?: return -1L
            val duration = (endTime - startTime) / 1_000_000L // Convert to milliseconds
            
            measurements.getOrPut(operationName) { mutableListOf() }.add(duration)
            startTimes.remove(operationName)
            
            return duration
        }
        
        /**
         * Measure operation execution time
         */
        suspend fun <T> measureOperation(
            operationName: String,
            operation: suspend () -> T
        ): Pair<T, Long> {
            startTiming(operationName)
            val result = operation()
            val duration = stopTiming(operationName)
            return Pair(result, duration)
        }
        
        /**
         * Record memory usage
         */
        fun recordMemoryUsage() {
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            memoryUsage.set(usedMemory / (1024 * 1024)) // Convert to MB
        }
        
        /**
         * Get performance statistics
         */
        fun getPerformanceStats(operationName: String): PerformanceStats? {
            val times = measurements[operationName] ?: return null
            if (times.isEmpty()) return null
            
            return PerformanceStats(
                operationName = operationName,
                count = times.size,
                minTime = times.minOrNull() ?: 0L,
                maxTime = times.maxOrNull() ?: 0L,
                averageTime = times.average(),
                totalTime = times.sum(),
                memoryUsageMB = memoryUsage.get()
            )
        }
        
        /**
         * Get comprehensive performance report
         */
        fun getComprehensiveReport(): PerformanceReport {
            val stats = measurements.keys.mapNotNull { getPerformanceStats(it) }
            
            return PerformanceReport(
                operationStats = stats,
                totalOperations = operationCounts.values.sumOf { it.get() },
                totalMemoryUsageMB = memoryUsage.get(),
                testDuration = stats.maxOfOrNull { it.totalTime } ?: 0L
            )
        }
        
        /**
         * Reset all measurements
         */
        fun reset() {
            measurements.clear()
            startTimes.clear()
            operationCounts.clear()
            memoryUsage.set(0L)
        }
        
        data class PerformanceStats(
            val operationName: String,
            val count: Int,
            val minTime: Long,
            val maxTime: Long,
            val averageTime: Double,
            val totalTime: Long,
            val memoryUsageMB: Long
        )
        
        data class PerformanceReport(
            val operationStats: List<PerformanceStats>,
            val totalOperations: Int,
            val totalMemoryUsageMB: Long,
            val testDuration: Long
        ) {
            fun meetsBenchmarks(
                maxLatencyMs: Long = 100L,
                maxMemoryMB: Long = 15L,
                minSuccessRate: Double = 0.95
            ): Boolean {
                val avgLatency = operationStats.map { it.averageTime }.average()
                return avgLatency <= maxLatencyMs && 
                       totalMemoryUsageMB <= maxMemoryMB
            }
        }
    }
    
    // Helper Methods
    
    /**
     * Wait for condition with timeout
     */
    suspend fun waitForCondition(
        timeoutMs: Long,
        intervalMs: Long = 100L,
        condition: () -> Boolean
    ): Boolean {
        return withTimeoutOrNull(timeoutMs) {
            while (!condition()) {
                delay(intervalMs)
            }
            true
        } ?: false
    }
    
    /**
     * Get test context
     */
    fun getTestContext(): Context = InstrumentationRegistry.getInstrumentation().targetContext
    
    /**
     * Get instrumentation context
     */
    fun getInstrumentationContext(): Context = InstrumentationRegistry.getInstrumentation().context
    
    /**
     * Log test result with timestamp
     */
    fun logTestResult(testName: String, success: Boolean, details: String = "") {
        val status = if (success) "PASSED" else "FAILED"
        val timestamp = System.currentTimeMillis()
        Log.i(TAG, "TEST [$testName] $status at $timestamp - $details")
    }
    
    /**
     * Create test intent for VoiceRecognition service
     */
    fun createTestServiceIntent(packageName: String = "com.augmentalis.voicerecognition"): Intent {
        return Intent().apply {
            setClassName(packageName, "$packageName.service.VoiceRecognitionService")
            action = "com.augmentalis.voicerecognition.SERVICE"
        }
    }
}