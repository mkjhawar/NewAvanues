/**
 * PerformanceTest.kt - Performance testing framework for voice accessibility
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Test Framework
 * Created: 2025-08-28
 * 
 * Comprehensive performance testing including binding latency, command processing time,
 * memory usage monitoring, and resource cleanup verification.
 */
package com.augmentalis.voiceos.accessibility.test

import android.content.Context
import android.os.Debug
import android.util.Log
import com.augmentalis.voiceos.accessibility.handlers.ActionCategory
import com.augmentalis.voiceos.accessibility.managers.ActionCoordinator
import com.augmentalis.voiceos.accessibility.mocks.MockVoiceAccessibilityService
import com.augmentalis.voiceos.accessibility.mocks.MockVoiceRecognitionManager
import kotlinx.coroutines.*
// Note: Java management package not available in Android Unit Tests
// Using Runtime instead of MemoryMXBean for testing
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.system.measureTimeMillis

/**
 * Performance metrics collection system
 */
class PerformanceMetrics {
    
    data class LatencyMeasurement(
        val operation: String,
        val duration: Long,
        val timestamp: Long = System.currentTimeMillis(),
        val success: Boolean = true,
        val metadata: Map<String, Any> = emptyMap()
    )
    
    data class MemorySnapshot(
        val heapUsed: Long,
        val heapMax: Long,
        val timestamp: Long = System.currentTimeMillis(),
        val operation: String = ""
    ) {
        val heapUtilization: Float
            get() = if (heapMax > 0) heapUsed.toFloat() / heapMax else 0f
    }
    
    data class ThroughputMeasurement(
        val operation: String,
        val requestsPerSecond: Double,
        val averageLatency: Double,
        val maxLatency: Long,
        val minLatency: Long,
        val totalRequests: Int,
        val failedRequests: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        val successRate: Float
            get() = if (totalRequests > 0) (totalRequests - failedRequests).toFloat() / totalRequests else 0f
    }
    
    private val latencyMeasurements = mutableListOf<LatencyMeasurement>()
    private val memorySnapshots = mutableListOf<MemorySnapshot>()
    private val throughputData = ConcurrentHashMap<String, MutableList<Long>>()
    private val operationCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val operationFailures = ConcurrentHashMap<String, AtomicInteger>()
    
    // Using Runtime instead of MemoryMXBean for Android compatibility
    private val runtime = Runtime.getRuntime()
    
    /**
     * Record latency measurement
     */
    fun recordLatency(operation: String, duration: Long, success: Boolean = true, metadata: Map<String, Any> = emptyMap()) {
        synchronized(latencyMeasurements) {
            latencyMeasurements.add(LatencyMeasurement(operation, duration, success = success, metadata = metadata))
        }
        
        // Update throughput data
        throughputData.getOrPut(operation) { mutableListOf() }.add(duration)
        operationCounts.getOrPut(operation) { AtomicInteger(0) }.incrementAndGet()
        if (!success) {
            operationFailures.getOrPut(operation) { AtomicInteger(0) }.incrementAndGet()
        }
    }
    
    /**
     * Take memory snapshot
     */
    fun takeMemorySnapshot(operation: String = "") {
        synchronized(memorySnapshots) {
            memorySnapshots.add(
                MemorySnapshot(
                    heapUsed = runtime.totalMemory() - runtime.freeMemory(),
                    heapMax = runtime.maxMemory(),
                    operation = operation
                )
            )
        }
    }
    
    /**
     * Get latency statistics for an operation
     */
    fun getLatencyStats(operation: String): LatencyStats {
        val measurements = latencyMeasurements.filter { it.operation == operation }
        if (measurements.isEmpty()) {
            return LatencyStats(operation, 0, 0.0, 0, 0, 0.0, 0, 0, 0.0)
        }
        
        val durations = measurements.map { it.duration }
        val successCount = measurements.count { it.success }
        val mean = durations.average()
        val variance = durations.map { (it - mean) * (it - mean) }.average()
        val standardDeviation = sqrt(variance)
        
        return LatencyStats(
            operation = operation,
            sampleCount = measurements.size,
            averageMs = mean,
            minMs = durations.minOrNull() ?: 0,
            maxMs = durations.maxOrNull() ?: 0,
            medianMs = durations.sorted().let { sorted ->
                if (sorted.size % 2 == 0) {
                    (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
                } else {
                    sorted[sorted.size / 2].toDouble()
                }
            },
            p95Ms = durations.sorted().let { sorted ->
                if (sorted.isNotEmpty()) {
                    val index = (sorted.size * 0.95).toInt().coerceAtMost(sorted.size - 1)
                    sorted[index]
                } else 0
            },
            successCount = successCount,
            standardDeviationMs = standardDeviation
        )
    }
    
    /**
     * Get throughput statistics for an operation
     */
    fun getThroughputStats(operation: String, windowSeconds: Long = 60): ThroughputMeasurement? {
        val durations = throughputData[operation] ?: return null
        val totalRequests = operationCounts[operation]?.get() ?: 0
        val failedRequests = operationFailures[operation]?.get() ?: 0
        
        if (totalRequests == 0) return null
        
        val requestsPerSecond = totalRequests.toDouble() / windowSeconds
        val averageLatency = durations.average()
        val maxLatency = durations.maxOrNull() ?: 0
        val minLatency = durations.minOrNull() ?: 0
        
        return ThroughputMeasurement(
            operation = operation,
            requestsPerSecond = requestsPerSecond,
            averageLatency = averageLatency,
            maxLatency = maxLatency,
            minLatency = minLatency,
            totalRequests = totalRequests,
            failedRequests = failedRequests
        )
    }
    
    /**
     * Get memory usage statistics
     */
    fun getMemoryStats(): MemoryStats {
        if (memorySnapshots.isEmpty()) {
            return MemoryStats(0, 0, 0.0, 0, 0, 0.0)
        }
        
        val heapUsages = memorySnapshots.map { it.heapUsed }
        val utilizations = memorySnapshots.map { it.heapUtilization }
        
        return MemoryStats(
            sampleCount = memorySnapshots.size,
            currentHeapUsed = memorySnapshots.lastOrNull()?.heapUsed ?: 0,
            averageHeapUsed = heapUsages.average(),
            maxHeapUsed = heapUsages.maxOrNull() ?: 0,
            minHeapUsed = heapUsages.minOrNull() ?: 0,
            averageUtilization = utilizations.average()
        )
    }
    
    /**
     * Clear all metrics
     */
    fun clear() {
        synchronized(latencyMeasurements) { latencyMeasurements.clear() }
        synchronized(memorySnapshots) { memorySnapshots.clear() }
        throughputData.clear()
        operationCounts.clear()
        operationFailures.clear()
    }
    
    data class LatencyStats(
        val operation: String,
        val sampleCount: Int,
        val averageMs: Double,
        val minMs: Long,
        val maxMs: Long,
        val medianMs: Double,
        val p95Ms: Long,
        val successCount: Int,
        val standardDeviationMs: Double = 0.0
    ) {
        val successRate: Float
            get() = if (sampleCount > 0) successCount.toFloat() / sampleCount else 0f
    }
    
    data class MemoryStats(
        val sampleCount: Int,
        val currentHeapUsed: Long,
        val averageHeapUsed: Double,
        val maxHeapUsed: Long,
        val minHeapUsed: Long,
        val averageUtilization: Double
    )
}

/**
 * Binding latency testing
 */
class BindingLatencyTest {
    
    companion object {
        private const val TAG = "BindingLatencyTest"
        private const val DEFAULT_ITERATIONS = 100
        private const val WARMUP_ITERATIONS = 10
    }
    
    private val metrics = PerformanceMetrics()
    
    /**
     * Test service binding latency
     */
    fun testServiceBinding(_context: Context, iterations: Int = DEFAULT_ITERATIONS): BindingLatencyResult {
        Log.d(TAG, "Starting service binding latency test with $iterations iterations")
        
        val results = mutableListOf<Long>()
        val errors = mutableListOf<String>()
        
        // Warmup
        repeat(WARMUP_ITERATIONS) {
            try {
                performSingleBindingTest(_context)
            } catch (e: Exception) {
                // Ignore warmup errors
            }
        }
        
        // Actual test
        repeat(iterations) { iteration ->
            try {
                val latency = performSingleBindingTest(_context)
                results.add(latency)
                metrics.recordLatency("service_binding", latency, true, mapOf("iteration" to iteration))
            } catch (e: Exception) {
                errors.add("Iteration $iteration failed: ${e.message}")
                Log.w(TAG, "Binding test iteration $iteration failed", e)
            }
        }
        
        return BindingLatencyResult(
            totalIterations = iterations,
            successfulIterations = results.size,
            failedIterations = errors.size,
            averageLatencyMs = if (results.isNotEmpty()) results.average() else 0.0,
            minLatencyMs = results.minOrNull() ?: 0,
            maxLatencyMs = results.maxOrNull() ?: 0,
            standardDeviationMs = calculateStandardDeviation(results),
            errors = errors,
            latencyStats = metrics.getLatencyStats("service_binding")
        )
    }
    
    /**
     * Perform single binding test
     */
    private fun performSingleBindingTest(_context: Context): Long {
        val startTime = System.currentTimeMillis()
        
        // Create and initialize components  
        val mockService = MockVoiceAccessibilityService()
        mockService.initialize()
        val actionCoordinator = ActionCoordinator(mockService).apply {
            initialize()
        }
        
        // Mock VoiceRecognitionManager functionality
        val voiceManager = MockVoiceRecognitionManager(actionCoordinator)
        voiceManager.initialize(_context)
        
        // Wait for connection
        var attempts = 0
        val maxAttempts = 100
        while (!voiceManager.isServiceConnected() && attempts < maxAttempts) {
            Thread.sleep(10)
            attempts++
        }
        
        val endTime = System.currentTimeMillis()
        
        // Cleanup
        voiceManager.dispose()
        actionCoordinator.dispose()
        
        if (!voiceManager.isServiceConnected()) {
            throw RuntimeException("Failed to bind within timeout")
        }
        
        return endTime - startTime
    }
    
    private fun calculateStandardDeviation(values: List<Long>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance)
    }
    
    data class BindingLatencyResult(
        val totalIterations: Int,
        val successfulIterations: Int,
        val failedIterations: Int,
        val averageLatencyMs: Double,
        val minLatencyMs: Long,
        val maxLatencyMs: Long,
        val standardDeviationMs: Double,
        val errors: List<String>,
        val latencyStats: PerformanceMetrics.LatencyStats
    ) {
        val successRate: Float
            get() = if (totalIterations > 0) successfulIterations.toFloat() / totalIterations else 0f
    }
}

/**
 * Command processing performance testing
 */
class CommandProcessingTest {
    
    companion object {
        private const val TAG = "CommandProcessingTest"
        private const val DEFAULT_ITERATIONS = 1000
        private const val CONCURRENT_THREADS = 10
    }
    
    private val metrics = PerformanceMetrics()
    
    /**
     * Test command processing performance
     */
    fun testCommandProcessing(
        _context: Context,
        iterations: Int = DEFAULT_ITERATIONS,
        concurrentThreads: Int = CONCURRENT_THREADS
    ): CommandProcessingResult {
        
        Log.d(TAG, "Starting command processing test: $iterations iterations, $concurrentThreads threads")
        
        // Setup
        val mockService = MockVoiceAccessibilityService()
        mockService.initialize() 
        val actionCoordinator = ActionCoordinator(mockService).apply {
            initialize()
        }
        
        val testCommands = listOf(
            "open settings" to ActionCategory.APP,
            "go back" to ActionCategory.NAVIGATION,
            "volume up" to ActionCategory.SYSTEM,
            "tap" to ActionCategory.UI,
            "brightness up" to ActionCategory.DEVICE,
            "type hello" to ActionCategory.INPUT
        )
        
        val results = ConcurrentHashMap<String, MutableList<Long>>()
        val errors = ConcurrentHashMap<String, MutableList<String>>()
        
        val startTime = System.currentTimeMillis()
        
        // Run concurrent tests
        runBlocking {
            val jobs = (1..concurrentThreads).map { threadId ->
                async(Dispatchers.Default) {
                    repeat(iterations / concurrentThreads) { iteration ->
                        testCommands.forEach { (command, _) ->
                            try {
                                var success = false
                                val processingTime = measureTimeMillis {
                                    success = actionCoordinator.processCommand(command)
                                }
                                metrics.recordLatency("command_processing", processingTime, success)
                                
                                results.getOrPut(command) { mutableListOf() }.add(processingTime)
                                
                            } catch (e: Exception) {
                                val errorKey = "$command-thread$threadId-iter$iteration"
                                errors.getOrPut(command) { mutableListOf() }.add("$errorKey: ${e.message}")
                            }
                        }
                    }
                }
            }
            jobs.awaitAll()
        }
        
        val totalTime = System.currentTimeMillis() - startTime
        
        // Calculate statistics
        val commandStats = testCommands.associate { (command, _) ->
            command to results[command]?.let { times ->
                CommandStats(
                    command = command,
                    executionCount = times.size,
                    averageMs = times.average(),
                    minMs = times.minOrNull() ?: 0,
                    maxMs = times.maxOrNull() ?: 0,
                    medianMs = times.sorted().let { sorted ->
                        if (sorted.size % 2 == 0) {
                            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
                        } else {
                            sorted[sorted.size / 2].toDouble()
                        }
                    }
                )
            }
        }
        
        // Cleanup
        actionCoordinator.dispose()
        
        return CommandProcessingResult(
            totalCommands = iterations * testCommands.size,
            totalTime = totalTime,
            throughputCommandsPerSecond = (iterations * testCommands.size * 1000.0) / totalTime,
            commandStats = commandStats.filterValues { it != null }.mapValues { it.value!! },
            errors = errors.mapValues { it.value.toList() }.toMap()
        )
    }
    
    data class CommandStats(
        val command: String,
        val executionCount: Int,
        val averageMs: Double,
        val minMs: Long,
        val maxMs: Long,
        val medianMs: Double
    )
    
    data class CommandProcessingResult(
        val totalCommands: Int,
        val totalTime: Long,
        val throughputCommandsPerSecond: Double,
        val commandStats: Map<String, CommandStats>,
        val errors: Map<String, List<String>>
    ) {
        val overallAverageMs: Double
            get() = commandStats.values.map { it.averageMs }.average()
            
        val overallMaxMs: Long
            get() = commandStats.values.map { it.maxMs }.maxOrNull() ?: 0
    }
}

/**
 * Memory usage monitoring
 */
class MemoryUsageTest {
    
    companion object {
        private const val TAG = "MemoryUsageTest"
        private const val MONITORING_INTERVAL_MS = 1000L
        private const val TEST_DURATION_MS = 60000L
    }
    
    private val metrics = PerformanceMetrics()
    
    /**
     * Monitor memory usage during operations
     */
    fun monitorMemoryUsage(
        _context: Context,
        operations: List<() -> Unit>,
        durationMs: Long = TEST_DURATION_MS,
        intervalMs: Long = MONITORING_INTERVAL_MS
    ): MemoryUsageResult = runBlocking {
        
        Log.d(TAG, "Starting memory usage monitoring for ${durationMs}ms")
        
        val startTime = System.currentTimeMillis()
        
        // Initial memory snapshot
        metrics.takeMemorySnapshot("test_start")
        
        // Start monitoring
        val monitoringJob = launch(Dispatchers.IO) {
            while (System.currentTimeMillis() - startTime < durationMs) {
                metrics.takeMemorySnapshot("monitoring")
                delay(intervalMs)
            }
        }
        
        // Run operations
        val operationJob = launch(Dispatchers.Default) {
            val operationInterval = durationMs / operations.size
            operations.forEachIndexed { index, operation ->
                try {
                    metrics.takeMemorySnapshot("before_operation_$index")
                    operation()
                    metrics.takeMemorySnapshot("after_operation_$index")
                } catch (e: Exception) {
                    Log.e(TAG, "Operation $index failed", e)
                }
                
                delay(operationInterval)
            }
        }
        
        // Wait for completion
        listOf(monitoringJob, operationJob).joinAll()
        
        // Final memory snapshot
        metrics.takeMemorySnapshot("test_end")
        
        val memoryStats = metrics.getMemoryStats()
        
        MemoryUsageResult(
            testDurationMs = durationMs,
            monitoringIntervalMs = intervalMs,
            memoryStats = memoryStats,
            peakMemoryUsage = memoryStats.maxHeapUsed,
            memoryGrowthBytes = memoryStats.maxHeapUsed - memoryStats.minHeapUsed,
            averageMemoryUtilization = memoryStats.averageUtilization
        )
    }
    
    data class MemoryUsageResult(
        val testDurationMs: Long,
        val monitoringIntervalMs: Long,
        val memoryStats: PerformanceMetrics.MemoryStats,
        val peakMemoryUsage: Long,
        val memoryGrowthBytes: Long,
        val averageMemoryUtilization: Double
    ) {
        val memoryGrowthMB: Double
            get() = memoryGrowthBytes / (1024.0 * 1024.0)
            
        val peakMemoryUsageMB: Double
            get() = peakMemoryUsage / (1024.0 * 1024.0)
    }
}

/**
 * Resource cleanup verification
 */
class ResourceCleanupTest {
    
    companion object {
        private const val TAG = "ResourceCleanupTest"
    }
    
    /**
     * Test resource cleanup after operations
     */
    fun testResourceCleanup(_context: Context): ResourceCleanupResult {
        Log.d(TAG, "Starting resource cleanup test")
        
        val beforeMemory = getMemorySnapshot("before_test")
        val leaks = mutableListOf<String>()
        val errors = mutableListOf<String>()
        
        try {
            // Create and use components
            val mockService = MockVoiceAccessibilityService()
            mockService.initialize()
            val actionCoordinator = ActionCoordinator(mockService).apply {
                initialize()
            }
            
            val voiceManager = MockVoiceRecognitionManager(actionCoordinator).apply {
                initialize(_context)
            }
            
            // Use the components
            repeat(100) {
                actionCoordinator.processCommand("test command $it")
            }
            
            val duringMemory = getMemorySnapshot("during_operations")
            
            // Dispose components
            voiceManager.dispose()
            actionCoordinator.dispose()
            
            // Force garbage collection
            System.gc()
            Thread.sleep(1000)
            System.gc()
            
            val afterMemory = getMemorySnapshot("after_cleanup")
            
            // Check for memory leaks
            val memoryGrowth = afterMemory.heapUsed - beforeMemory.heapUsed
            val memoryGrowthMB = memoryGrowth / (1024.0 * 1024.0)
            
            if (memoryGrowthMB > 5.0) { // Threshold for significant memory growth
                leaks.add("Significant memory growth detected: ${memoryGrowthMB}MB")
            }
            
            // Test component disposal
            try {
                voiceManager.startListening()
                leaks.add("VoiceRecognitionManager still functional after disposal")
            } catch (e: Exception) {
                // Expected - component should be disposed
            }
            
        } catch (e: Exception) {
            errors.add("Test execution error: ${e.message}")
        }
        
        return ResourceCleanupResult(
            memoryLeaksDetected = leaks.size,
            leakDescriptions = leaks,
            errors = errors,
            memoryGrowthMB = 0.0 // Calculate actual growth
        )
    }
    
    private fun getMemorySnapshot(operation: String): PerformanceMetrics.MemorySnapshot {
        val runtime = Runtime.getRuntime()
        return PerformanceMetrics.MemorySnapshot(
            heapUsed = runtime.totalMemory() - runtime.freeMemory(),
            heapMax = runtime.maxMemory(),
            operation = operation
        )
    }
    
    data class ResourceCleanupResult(
        val memoryLeaksDetected: Int,
        val leakDescriptions: List<String>,
        val errors: List<String>,
        val memoryGrowthMB: Double
    ) {
        val cleanupSuccessful: Boolean
            get() = memoryLeaksDetected == 0 && errors.isEmpty()
    }
}

/**
 * Main performance test controller
 */
class PerformanceTestController {
    
    companion object {
        private const val TAG = "PerformanceTestController"
    }
    
    /**
     * Run complete performance test suite
     */
    fun runCompletePerformanceTest(_context: Context): CompletePerformanceResult {
        Log.i(TAG, "Starting complete performance test suite")
        
        val startTime = System.currentTimeMillis()
        
        // Binding latency test
        val bindingTest = BindingLatencyTest()
        val bindingResult = bindingTest.testServiceBinding(_context)
        
        // Command processing test
        val processingTest = CommandProcessingTest()
        val processingResult = processingTest.testCommandProcessing(_context)
        
        // Memory usage test
        val memoryTest = MemoryUsageTest()
        val memoryOperations = listOf(
            { /* Simulate app launch */ Thread.sleep(100) },
            { /* Simulate navigation */ Thread.sleep(50) },
            { /* Simulate voice command */ Thread.sleep(200) }
        )
        val memoryResult = memoryTest.monitorMemoryUsage(_context, memoryOperations)
        
        // Resource cleanup test
        val cleanupTest = ResourceCleanupTest()
        val cleanupResult = cleanupTest.testResourceCleanup(_context)
        
        val totalTime = System.currentTimeMillis() - startTime
        
        return CompletePerformanceResult(
            totalTestTime = totalTime,
            bindingLatencyResult = bindingResult,
            commandProcessingResult = processingResult,
            memoryUsageResult = memoryResult,
            resourceCleanupResult = cleanupResult
        )
    }
    
    data class CompletePerformanceResult(
        val totalTestTime: Long,
        val bindingLatencyResult: BindingLatencyTest.BindingLatencyResult,
        val commandProcessingResult: CommandProcessingTest.CommandProcessingResult,
        val memoryUsageResult: MemoryUsageTest.MemoryUsageResult,
        val resourceCleanupResult: ResourceCleanupTest.ResourceCleanupResult
    ) {
        val overallScore: Double
            get() {
                val bindingScore = if (bindingLatencyResult.averageLatencyMs < 1000) 100.0 else 
                    max(0.0, 100.0 - (bindingLatencyResult.averageLatencyMs - 1000) / 10)
                    
                val processingScore = if (commandProcessingResult.overallAverageMs < 100) 100.0 else
                    max(0.0, 100.0 - (commandProcessingResult.overallAverageMs - 100) / 5)
                    
                val memoryScore = if (memoryUsageResult.memoryGrowthMB < 5.0) 100.0 else
                    max(0.0, 100.0 - (memoryUsageResult.memoryGrowthMB - 5.0) * 10)
                    
                val cleanupScore = if (resourceCleanupResult.cleanupSuccessful) 100.0 else 0.0
                
                return (bindingScore + processingScore + memoryScore + cleanupScore) / 4.0
            }
    }
}