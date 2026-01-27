/**
 * VivokaPerformance.kt - Performance monitoring for Vivoka VSDK engine
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-28
 * Updated: 2026-01-27 - Migrated to KMP androidMain
 *
 * Extracted from monolithic VivokaEngine.kt as part of SOLID refactoring
 * Wraps shared PerformanceMonitor with Vivoka-specific monitoring and metrics
 */
package com.augmentalis.speechrecognition.vivoka

import android.util.Log
import com.augmentalis.speechrecognition.PerformanceMonitor
import kotlinx.coroutines.*
import java.util.*
import kotlin.math.max

/**
 * Vivoka-specific performance monitoring that extends shared PerformanceMonitor
 */
class VivokaPerformance(
    private val coroutineScope: CoroutineScope
) {

    companion object {
        private const val TAG = "VivokaPerformance"
        private const val MEMORY_CHECK_INTERVAL = 10000L // 10 seconds
        private const val PERFORMANCE_LOG_INTERVAL = 60000L // 1 minute
        private const val VSDK_SPECIFIC_METRICS_INTERVAL = 30000L // 30 seconds
    }

    // Shared performance monitor
    private val performanceMonitor = PerformanceMonitor("Vivoka")

    // Vivoka-specific metrics
    @Volatile private var vsdkInitializationTime = 0L
    @Volatile private var modelCompilationTime = 0L
    @Volatile private var assetExtractionTime = 0L
    @Volatile private var pipelineRecoveryCount = 0
    @Volatile private var modelSwitchCount = 0
    @Volatile private var assetValidationCount = 0

    // Timing tracking
    private val operationTimings = ArrayDeque<OperationTiming>(100)

    // Background monitoring
    private var memoryMonitoringJob: Job? = null
    private var performanceLoggingJob: Job? = null
    private var vsdkMetricsJob: Job? = null

    // Performance thresholds for Vivoka
    private val vsdkThresholds = VivokaPerfThresholds()

    data class OperationTiming(
        val operation: String,
        val startTime: Long,
        val endTime: Long,
        val duration: Long,
        val success: Boolean,
        val details: String? = null
    )

    data class VivokaPerfThresholds(
        val initializationWarning: Long = 5000L, // 5 seconds
        val initializationCritical: Long = 10000L, // 10 seconds
        val modelCompilationWarning: Long = 3000L, // 3 seconds
        val modelCompilationCritical: Long = 8000L, // 8 seconds
        val assetExtractionWarning: Long = 10000L, // 10 seconds
        val assetExtractionCritical: Long = 30000L, // 30 seconds
        val recognitionLatencyWarning: Long = 1000L, // 1 second
        val recognitionLatencyCritical: Long = 3000L, // 3 seconds
    )

    /**
     * Initialize performance monitoring
     */
    fun initialize() {
        Log.d(TAG, "Initializing Vivoka performance monitoring")

        // Start the shared performance monitor
        performanceMonitor.startSession()

        // Start background monitoring jobs
        startBackgroundMonitoring()

        Log.i(TAG, "Performance monitoring initialized")
    }

    /**
     * Record VSDK initialization timing
     */
    fun recordVSDKInitialization(startTime: Long, success: Boolean, details: String? = null) {
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        vsdkInitializationTime = duration

        // Record with shared monitor
        performanceMonitor.recordSlowOperation("initialization", duration, vsdkThresholds.initializationWarning)

        // Record timing
        val timing = OperationTiming("vsdk_initialization", startTime, endTime, duration, success, details)
        recordOperationTiming(timing)

        // Check thresholds
        checkVSDKInitThreshold(duration, success)

        Log.d(TAG, "VSDK initialization: ${duration}ms, success: $success")
    }

    /**
     * Record model compilation timing
     */
    fun recordModelCompilation(startTime: Long, commandCount: Int, success: Boolean) {
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        modelCompilationTime = duration

        // Record with shared monitor
        performanceMonitor.recordSlowOperation("model_compilation", duration, vsdkThresholds.modelCompilationWarning)

        // Record timing
        val details = "commands: $commandCount"
        val timing = OperationTiming("model_compilation", startTime, endTime, duration, success, details)
        recordOperationTiming(timing)

        // Check thresholds
        checkModelCompilationThreshold(duration, success, commandCount)

        Log.d(TAG, "Model compilation: ${duration}ms, commands: $commandCount, success: $success")
    }

    /**
     * Record asset extraction/validation timing
     */
    fun recordAssetOperation(operation: String, startTime: Long, success: Boolean, details: String? = null) {
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        when (operation) {
            "asset_extraction" -> {
                assetExtractionTime = duration
                performanceMonitor.recordSlowOperation("asset_extraction", duration, vsdkThresholds.assetExtractionWarning)
            }
            "asset_validation" -> {
                assetValidationCount++
                performanceMonitor.recordSlowOperation("asset_validation", duration, 2000L)
            }
        }

        // Record timing
        val timing = OperationTiming(operation, startTime, endTime, duration, success, details)
        recordOperationTiming(timing)

        Log.d(TAG, "$operation: ${duration}ms, success: $success")
    }

    /**
     * Record recognition session
     */
    fun recordRecognition(startTime: Long, recognized: String?, confidence: Float, success: Boolean) {
        // Use shared performance monitor
        performanceMonitor.recordRecognition(startTime, success, recognized)

        // Record timing
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        val details = "text: ${recognized?.take(20) ?: "null"}, confidence: $confidence"
        val timing = OperationTiming("recognition", startTime, endTime, duration, success, details)
        recordOperationTiming(timing)

        // Check recognition latency threshold
        if (duration > vsdkThresholds.recognitionLatencyWarning) {
            Log.w(TAG, "High recognition latency: ${duration}ms for '$recognized'")
        }
    }

    /**
     * Record pipeline recovery
     */
    fun recordPipelineRecovery(success: Boolean) {
        pipelineRecoveryCount++
        Log.d(TAG, "Pipeline recovery #$pipelineRecoveryCount: success=$success")

        if (!success) {
            // Record as failed recognition instead
            performanceMonitor.recordRecognition(System.currentTimeMillis(), false, "pipeline_recovery_failed")
        }
    }

    /**
     * Record model switch operation
     */
    fun recordModelSwitch(fromModel: String?, toModel: String?, duration: Long, success: Boolean) {
        modelSwitchCount++

        val details = "from: ${fromModel ?: "none"} to: ${toModel ?: "none"}"
        val timing = OperationTiming("model_switch", System.currentTimeMillis() - duration,
                                   System.currentTimeMillis(), duration, success, details)
        recordOperationTiming(timing)

        Log.d(TAG, "Model switch: ${duration}ms, $details, success: $success")
    }

    /**
     * Get comprehensive Vivoka performance metrics
     */
    fun getVivokaMetrics(): Map<String, Any> {
        val baseMetrics = performanceMonitor.getMetrics()
        val recentOperations = getRecentOperationMetrics()

        // Convert PerformanceMetrics to Map and add Vivoka-specific metrics
        return mutableMapOf<String, Any>().apply {
            // Base metrics from shared monitor
            this["engineName"] = baseMetrics.engineName
            this["totalRecognitions"] = baseMetrics.totalRecognitions
            this["successfulRecognitions"] = baseMetrics.successfulRecognitions
            this["failedRecognitions"] = baseMetrics.failedRecognitions
            this["successRate"] = baseMetrics.successRate
            this["averageLatency"] = baseMetrics.averageLatency
            this["slowOperationCount"] = baseMetrics.slowOperationCount
            this["performanceState"] = baseMetrics.performanceState

            // Vivoka-specific metrics
            this["vsdkInitializationTimeMs"] = vsdkInitializationTime
            this["modelCompilationTimeMs"] = modelCompilationTime
            this["assetExtractionTimeMs"] = assetExtractionTime
            this["pipelineRecoveryCount"] = pipelineRecoveryCount
            this["modelSwitchCount"] = modelSwitchCount
            this["assetValidationCount"] = assetValidationCount

            // Recent operation analysis
            this["recentOperations"] = recentOperations

            // Performance health assessment
            this["vsdkPerformanceHealth"] = assessVSDKPerformanceHealth()

            // Bottleneck analysis
            this["vsdkBottlenecks"] = identifyVSDKBottlenecks()
        }
    }

    /**
     * Get recent operation metrics
     */
    private fun getRecentOperationMetrics(): Map<String, Any> {
        val recentOps = synchronized(operationTimings) {
            operationTimings.toList().takeLast(20)
        }

        val operationStats = recentOps.groupBy { timing -> timing.operation }.mapValues { (_, timings) ->
            mapOf(
                "count" to timings.size,
                "averageDuration" to if (timings.isNotEmpty()) timings.map { timing -> timing.duration }.average().toLong() else 0L,
                "successRate" to if (timings.isNotEmpty()) (timings.count { timing -> timing.success }.toFloat() / timings.size) * 100 else 0f,
                "lastOperation" to timings.maxByOrNull { timing -> timing.endTime }?.endTime
            )
        }

        return mapOf(
            "totalRecentOperations" to recentOps.size,
            "operationBreakdown" to operationStats,
            "averageOperationTime" to if (recentOps.isNotEmpty()) recentOps.map { timing -> timing.duration }.average().toLong() else 0L
        )
    }

    /**
     * Assess VSDK-specific performance health
     */
    private fun assessVSDKPerformanceHealth(): String {
        var score = 100
        val issues = mutableListOf<String>()

        // Check initialization time
        if (vsdkInitializationTime > vsdkThresholds.initializationCritical) {
            score -= 30
            issues.add("Critical initialization delay")
        } else if (vsdkInitializationTime > vsdkThresholds.initializationWarning) {
            score -= 15
            issues.add("Slow initialization")
        }

        // Check model compilation time
        if (modelCompilationTime > vsdkThresholds.modelCompilationCritical) {
            score -= 25
            issues.add("Critical model compilation delay")
        } else if (modelCompilationTime > vsdkThresholds.modelCompilationWarning) {
            score -= 10
            issues.add("Slow model compilation")
        }

        // Check asset extraction time
        if (assetExtractionTime > vsdkThresholds.assetExtractionCritical) {
            score -= 20
            issues.add("Critical asset extraction delay")
        } else if (assetExtractionTime > vsdkThresholds.assetExtractionWarning) {
            score -= 10
            issues.add("Slow asset extraction")
        }

        // Check recovery frequency
        if (pipelineRecoveryCount > 5) {
            score -= 20
            issues.add("Frequent pipeline recoveries")
        } else if (pipelineRecoveryCount > 2) {
            score -= 10
            issues.add("Some pipeline recoveries")
        }

        return when {
            score >= 90 -> "excellent"
            score >= 75 -> "good"
            score >= 60 -> "fair"
            score >= 40 -> "poor"
            else -> "critical"
        }
    }

    /**
     * Identify VSDK-specific bottlenecks
     */
    private fun identifyVSDKBottlenecks(): List<String> {
        val bottlenecks = mutableListOf<String>()

        // Initialization bottleneck
        if (vsdkInitializationTime > vsdkThresholds.initializationWarning) {
            bottlenecks.add("VSDK initialization taking ${vsdkInitializationTime}ms")
        }

        // Model compilation bottleneck
        if (modelCompilationTime > vsdkThresholds.modelCompilationWarning) {
            bottlenecks.add("Model compilation taking ${modelCompilationTime}ms")
        }

        // Asset extraction bottleneck
        if (assetExtractionTime > vsdkThresholds.assetExtractionWarning) {
            bottlenecks.add("Asset extraction taking ${assetExtractionTime}ms")
        }

        // Frequent recoveries
        if (pipelineRecoveryCount > 2) {
            bottlenecks.add("Pipeline recovered $pipelineRecoveryCount times")
        }

        // Frequent model switches
        if (modelSwitchCount > 20) {
            bottlenecks.add("High model switch frequency ($modelSwitchCount)")
        }

        return bottlenecks
    }

    /**
     * Record operation timing
     */
    private fun recordOperationTiming(timing: OperationTiming) {
        synchronized(operationTimings) {
            if (operationTimings.size >= 100) {
                operationTimings.removeFirst()
            }
            operationTimings.addLast(timing)
        }
    }

    /**
     * Check VSDK initialization threshold
     */
    private fun checkVSDKInitThreshold(duration: Long, success: Boolean) {
        when {
            duration > vsdkThresholds.initializationCritical -> {
                Log.e(TAG, "CRITICAL: VSDK initialization took ${duration}ms (threshold: ${vsdkThresholds.initializationCritical}ms)")
            }
            duration > vsdkThresholds.initializationWarning -> {
                Log.w(TAG, "WARNING: VSDK initialization took ${duration}ms (threshold: ${vsdkThresholds.initializationWarning}ms)")
            }
        }

        if (!success) {
            Log.e(TAG, "VSDK initialization FAILED after ${duration}ms")
        }
    }

    /**
     * Check model compilation threshold
     */
    private fun checkModelCompilationThreshold(duration: Long, success: Boolean, commandCount: Int) {
        val threshold = vsdkThresholds.modelCompilationWarning + (commandCount * 5L) // Adjust for command count

        when {
            duration > vsdkThresholds.modelCompilationCritical -> {
                Log.e(TAG, "CRITICAL: Model compilation took ${duration}ms for $commandCount commands")
            }
            duration > threshold -> {
                Log.w(TAG, "WARNING: Model compilation took ${duration}ms for $commandCount commands")
            }
        }

        if (!success) {
            Log.e(TAG, "Model compilation FAILED after ${duration}ms with $commandCount commands")
        }
    }

    /**
     * Start background monitoring jobs
     */
    private fun startBackgroundMonitoring() {
        // Memory monitoring
        memoryMonitoringJob = coroutineScope.launch {
            while (isActive) {
                delay(MEMORY_CHECK_INTERVAL)
                try {
                    // Memory monitoring is handled internally by PerformanceMonitor
                    // Check memory usage and log if concerning
                    val runtime = Runtime.getRuntime()
                    val usedMemory = runtime.totalMemory() - runtime.freeMemory()
                    val maxMemory = runtime.maxMemory()
                    val percentage = (usedMemory.toFloat() / maxMemory.toFloat()) * 100

                    if (percentage > 75) {
                        Log.w(TAG, "High memory usage detected: ${percentage.toInt()}%")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Memory monitoring failed", e)
                }
            }
        }

        // Performance logging
        performanceLoggingJob = coroutineScope.launch {
            while (isActive) {
                delay(PERFORMANCE_LOG_INTERVAL)
                try {
                    // Performance metrics logged via getMetrics()
                    logVivokaSpecificMetrics()
                } catch (e: Exception) {
                    Log.e(TAG, "Performance logging failed", e)
                }
            }
        }

        // VSDK-specific metrics
        vsdkMetricsJob = coroutineScope.launch {
            while (isActive) {
                delay(VSDK_SPECIFIC_METRICS_INTERVAL)
                try {
                    logVSDKMetrics()
                } catch (e: Exception) {
                    Log.e(TAG, "VSDK metrics logging failed", e)
                }
            }
        }
    }

    /**
     * Log Vivoka-specific metrics
     */
    private fun logVivokaSpecificMetrics() {
        val health = assessVSDKPerformanceHealth()
        val bottlenecks = identifyVSDKBottlenecks()

        Log.i(TAG, """
            Vivoka Performance Summary:
            ├── Health: $health
            ├── VSDK Init: ${vsdkInitializationTime}ms
            ├── Model Compilation: ${modelCompilationTime}ms
            ├── Asset Extraction: ${assetExtractionTime}ms
            ├── Pipeline Recoveries: $pipelineRecoveryCount
            ├── Model Switches: $modelSwitchCount
            └── Bottlenecks: ${bottlenecks.joinToString(", ").ifEmpty { "none" }}
        """.trimIndent())
    }

    /**
     * Log VSDK-specific metrics
     */
    private fun logVSDKMetrics() {
        val recentOps = synchronized(operationTimings) {
            operationTimings.toList().takeLast(10)
        }
        val recentSuccess = recentOps.count { timing -> timing.success }
        val recentTotal = recentOps.size

        Log.d(TAG, "Recent VSDK operations: $recentSuccess/$recentTotal successful")
    }

    /**
     * Reset performance monitoring
     */
    fun reset() {
        Log.d(TAG, "Resetting performance monitoring")

        // Reset shared monitor
        performanceMonitor.reset()

        // Reset Vivoka-specific metrics
        vsdkInitializationTime = 0L
        modelCompilationTime = 0L
        assetExtractionTime = 0L
        pipelineRecoveryCount = 0
        modelSwitchCount = 0
        assetValidationCount = 0

        // Clear operation timings
        synchronized(operationTimings) {
            operationTimings.clear()
        }
    }

    /**
     * Destroy performance monitoring
     */
    fun destroy() {
        Log.i(TAG, "Destroying performance monitoring")

        try {
            // Cancel background jobs
            memoryMonitoringJob?.cancel()
            performanceLoggingJob?.cancel()
            vsdkMetricsJob?.cancel()

            // Final metrics log
            // Performance metrics logged via getMetrics()
            logVivokaSpecificMetrics()

            // Destroy shared monitor
            performanceMonitor.destroy()

        } catch (e: Exception) {
            Log.e(TAG, "Error during performance monitoring destruction", e)
        }
    }

    /**
     * Get performance state for external monitoring
     */
    fun getPerformanceState(): PerformanceMonitor.PerformanceState {
        return performanceMonitor.getMetrics().performanceState
    }

    /**
     * Force performance metrics update
     */
    fun updateMetrics() {
        // The PerformanceMonitor handles metrics internally through its monitoring loop
        // We can trigger a log report to ensure metrics are current
        // Performance metrics logged via getMetrics()
    }
}
