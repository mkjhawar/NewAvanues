/**
 * PerformanceMonitor.kt - Unified performance monitoring for all speech engines
 * 
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-28
 * 
 * Consolidated performance tracking, memory monitoring, and bottleneck detection
 * Reduces ~600 lines of duplicated code across engines to ~200 lines
 */
package com.augmentalis.voiceos.speech.engines.common

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

/**
 * Centralized performance monitoring for all speech recognition engines.
 * Tracks metrics, identifies bottlenecks, and provides performance insights.
 */
class PerformanceMonitor(private val engineName: String) {
    
    companion object {
        private const val TAG = "PerformanceMonitor"
        private const val MEMORY_CHECK_INTERVAL = 5000L // 5 seconds
        private const val TREND_WINDOW_SIZE = 10
        private const val LATENCY_WINDOW_SIZE = 20
        private const val MEMORY_WARNING_THRESHOLD = 0.75 // 75% memory usage
        private const val MEMORY_CRITICAL_THRESHOLD = 0.9 // 90% memory usage
        private const val LATENCY_SPIKE_THRESHOLD = 2.0 // 2x average is a spike
    }
    
    // Session tracking
    private val sessionStartTime = AtomicLong(System.currentTimeMillis())
    private val totalSessions = AtomicInteger(0)
    private val totalRecognitions = AtomicInteger(0)
    private val successfulRecognitions = AtomicInteger(0)
    private val failedRecognitions = AtomicInteger(0)
    
    // Timing metrics
    private val totalRecognitionTime = AtomicLong(0)
    private val minRecognitionTime = AtomicLong(Long.MAX_VALUE)
    private val maxRecognitionTime = AtomicLong(0)
    private val lastRecognitionTime = AtomicLong(0)
    
    // Moving average for latency
    private val latencyWindow = ArrayDeque<Long>(LATENCY_WINDOW_SIZE)
    private var latencySum = 0L
    
    // Memory tracking
    private val memoryTrend = ArrayDeque<MemorySnapshot>(TREND_WINDOW_SIZE)
    private var lastMemoryCheck = 0L
    private var memoryWarningCount = 0
    private var memoryCriticalCount = 0
    
    // Performance states
    private val _performanceState = MutableStateFlow(PerformanceState.NORMAL)
    val performanceState = _performanceState.asStateFlow()
    
    // Bottleneck detection
    private val bottlenecks = mutableMapOf<BottleneckType, Int>()
    private var lastBottleneckCheck = 0L
    
    // Coroutines
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    data class MemorySnapshot(
        val timestamp: Long,
        val usedMemory: Long,
        val totalMemory: Long,
        val percentage: Float
    )
    
    data class PerformanceMetrics(
        val engineName: String,
        val sessionDuration: Long,
        val totalRecognitions: Int,
        val successRate: Float,
        val averageLatency: Long,
        val minLatency: Long,
        val maxLatency: Long,
        val currentMemoryUsage: Float,
        val memoryTrend: String,
        val bottlenecks: Map<BottleneckType, Int>,
        val performanceState: PerformanceState
    )
    
    enum class PerformanceState {
        NORMAL,
        DEGRADED,
        CRITICAL,
        RECOVERING
    }
    
    enum class BottleneckType {
        MEMORY_PRESSURE,
        HIGH_LATENCY,
        RECOGNITION_FAILURES,
        INITIALIZATION_SLOW,
        MODEL_LOADING_SLOW,
        AUDIO_PROCESSING_SLOW
    }
    
    init {
        startMonitoring()
    }
    
    /**
     * Start a new performance session
     */
    fun startSession() {
        sessionStartTime.set(System.currentTimeMillis())
        totalSessions.incrementAndGet()
        Log.d(TAG, "[$engineName] Performance session started #${totalSessions.get()}")
    }
    
    /**
     * Record recognition attempt with timing
     */
    fun recordRecognition(startTime: Long, success: Boolean, @Suppress("UNUSED_PARAMETER") errorMessage: String? = null) {
        val duration = System.currentTimeMillis() - startTime
        
        totalRecognitions.incrementAndGet()
        if (success) {
            successfulRecognitions.incrementAndGet()
        } else {
            failedRecognitions.incrementAndGet()
        }
        
        // Update timing metrics
        totalRecognitionTime.addAndGet(duration)
        lastRecognitionTime.set(duration)
        minRecognitionTime.updateAndGet { min(it, duration) }
        maxRecognitionTime.updateAndGet { max(it, duration) }
        
        // Update moving average
        updateLatencyWindow(duration)
        
        // Check for performance issues
        checkForBottlenecks(duration, success)
        
        Log.d(TAG, "[$engineName] Recognition: ${if (success) "SUCCESS" else "FAILED"} in ${duration}ms")
    }
    
    /**
     * Update latency moving average window
     */
    private fun updateLatencyWindow(latency: Long) {
        synchronized(latencyWindow) {
            if (latencyWindow.size >= LATENCY_WINDOW_SIZE) {
                val removed = latencyWindow.removeFirst()
                latencySum -= removed
            }
            latencyWindow.addLast(latency)
            latencySum += latency
        }
    }
    
    /**
     * Get current average latency
     */
    fun getAverageLatency(): Long {
        synchronized(latencyWindow) {
            return if (latencyWindow.isNotEmpty()) {
                latencySum / latencyWindow.size
            } else {
                0L
            }
        }
    }
    
    /**
     * Check memory usage and update trends
     */
    private fun checkMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val percentage = usedMemory.toFloat() / maxMemory.toFloat()
        
        val snapshot = MemorySnapshot(
            System.currentTimeMillis(),
            usedMemory,
            maxMemory,
            percentage
        )
        
        synchronized(memoryTrend) {
            if (memoryTrend.size >= TREND_WINDOW_SIZE) {
                memoryTrend.removeFirst()
            }
            memoryTrend.addLast(snapshot)
        }
        
        // Check thresholds
        when {
            percentage >= MEMORY_CRITICAL_THRESHOLD -> {
                memoryCriticalCount++
                bottlenecks[BottleneckType.MEMORY_PRESSURE] = 
                    (bottlenecks[BottleneckType.MEMORY_PRESSURE] ?: 0) + 1
                _performanceState.value = PerformanceState.CRITICAL
                Log.w(TAG, "[$engineName] CRITICAL memory usage: ${(percentage * 100).toInt()}%")
            }
            percentage >= MEMORY_WARNING_THRESHOLD -> {
                memoryWarningCount++
                _performanceState.value = PerformanceState.DEGRADED
                Log.w(TAG, "[$engineName] High memory usage: ${(percentage * 100).toInt()}%")
            }
            _performanceState.value == PerformanceState.CRITICAL && percentage < MEMORY_WARNING_THRESHOLD -> {
                _performanceState.value = PerformanceState.RECOVERING
                Log.i(TAG, "[$engineName] Memory recovering: ${(percentage * 100).toInt()}%")
            }
            _performanceState.value == PerformanceState.RECOVERING && percentage < 0.5 -> {
                _performanceState.value = PerformanceState.NORMAL
                Log.i(TAG, "[$engineName] Memory normalized: ${(percentage * 100).toInt()}%")
            }
        }
        
        lastMemoryCheck = System.currentTimeMillis()
    }
    
    /**
     * Analyze memory trend
     */
    fun getMemoryTrend(): String {
        synchronized(memoryTrend) {
            if (memoryTrend.size < 3) return "STABLE"
            
            val recent = memoryTrend.takeLast(5)
            val older = memoryTrend.take(5)
            
            val recentAvg = recent.map { it.percentage }.average()
            val olderAvg = older.map { it.percentage }.average()
            
            return when {
                recentAvg > olderAvg * 1.2 -> "INCREASING"
                recentAvg < olderAvg * 0.8 -> "DECREASING"
                else -> "STABLE"
            }
        }
    }
    
    /**
     * Check for performance bottlenecks
     */
    private fun checkForBottlenecks(latency: Long, @Suppress("UNUSED_PARAMETER") isCached: Boolean) {
        val avgLatency = getAverageLatency()
        
        // Latency spike detection
        if (avgLatency > 0 && latency > avgLatency * LATENCY_SPIKE_THRESHOLD) {
            bottlenecks[BottleneckType.HIGH_LATENCY] = 
                (bottlenecks[BottleneckType.HIGH_LATENCY] ?: 0) + 1
            Log.w(TAG, "[$engineName] Latency spike detected: ${latency}ms (avg: ${avgLatency}ms)")
        }
        
        // Failure rate detection
        val failureRate = if (totalRecognitions.get() > 0) {
            failedRecognitions.get().toFloat() / totalRecognitions.get().toFloat()
        } else 0f
        
        if (failureRate > 0.3 && totalRecognitions.get() > 10) { // 30% failure rate
            bottlenecks[BottleneckType.RECOGNITION_FAILURES] = 
                (bottlenecks[BottleneckType.RECOGNITION_FAILURES] ?: 0) + 1
            
            if (_performanceState.value == PerformanceState.NORMAL) {
                _performanceState.value = PerformanceState.DEGRADED
            }
        }
    }
    
    /**
     * Record slow operation (for initialization, model loading, etc.)
     */
    fun recordSlowOperation(operation: String, duration: Long, threshold: Long) {
        if (duration > threshold) {
            val type = when (operation) {
                "initialization" -> BottleneckType.INITIALIZATION_SLOW
                "model_loading" -> BottleneckType.MODEL_LOADING_SLOW
                "audio_processing" -> BottleneckType.AUDIO_PROCESSING_SLOW
                else -> return
            }
            
            bottlenecks[type] = (bottlenecks[type] ?: 0) + 1
            Log.w(TAG, "[$engineName] Slow $operation: ${duration}ms (threshold: ${threshold}ms)")
        }
    }
    
    /**
     * Get comprehensive performance metrics
     */
    fun getMetrics(): PerformanceMetrics {
        val sessionDuration = System.currentTimeMillis() - sessionStartTime.get()
        val successRate = if (totalRecognitions.get() > 0) {
            successfulRecognitions.get().toFloat() / totalRecognitions.get().toFloat()
        } else 0f
        
        val currentMemory = synchronized(memoryTrend) {
            memoryTrend.lastOrNull()?.percentage ?: 0f
        }
        
        return PerformanceMetrics(
            engineName = engineName,
            sessionDuration = sessionDuration,
            totalRecognitions = totalRecognitions.get(),
            successRate = successRate,
            averageLatency = getAverageLatency(),
            minLatency = if (minRecognitionTime.get() == Long.MAX_VALUE) 0 else minRecognitionTime.get(),
            maxLatency = maxRecognitionTime.get(),
            currentMemoryUsage = currentMemory,
            memoryTrend = getMemoryTrend(),
            bottlenecks = bottlenecks.toMap(),
            performanceState = _performanceState.value
        )
    }
    
    /**
     * Start background monitoring
     */
    private fun startMonitoring() {
        scope.launch {
            while (isActive) {
                checkMemoryUsage()
                delay(MEMORY_CHECK_INTERVAL)
            }
        }
    }
    
    /**
     * Reset metrics for new session
     */
    fun reset() {
        totalRecognitions.set(0)
        successfulRecognitions.set(0)
        failedRecognitions.set(0)
        totalRecognitionTime.set(0)
        minRecognitionTime.set(Long.MAX_VALUE)
        maxRecognitionTime.set(0)
        lastRecognitionTime.set(0)
        
        synchronized(latencyWindow) {
            latencyWindow.clear()
            latencySum = 0L
        }
        
        synchronized(memoryTrend) {
            memoryTrend.clear()
        }
        
        bottlenecks.clear()
        _performanceState.value = PerformanceState.NORMAL
        
        startSession()
    }
    
    /**
     * Clean up resources
     */
    fun destroy() {
        scope.cancel()
        Log.i(TAG, "[$engineName] Performance monitoring stopped. Final metrics: ${getMetrics()}")
    }
    
    /**
     * Log detailed performance report
     */
    fun logDetailedReport() {
        val metrics = getMetrics()
        Log.i(TAG, """
            [$engineName] Performance Report:
            ├── Session: ${metrics.sessionDuration / 1000}s
            ├── Recognitions: ${metrics.totalRecognitions} (${(metrics.successRate * 100).toInt()}% success)
            ├── Latency: avg=${metrics.averageLatency}ms, min=${metrics.minLatency}ms, max=${metrics.maxLatency}ms
            ├── Memory: ${(metrics.currentMemoryUsage * 100).toInt()}% (${metrics.memoryTrend})
            ├── State: ${metrics.performanceState}
            └── Bottlenecks: ${metrics.bottlenecks.entries.joinToString { "${it.key}=${it.value}" }}
        """.trimIndent())
    }
}

private fun min(a: Long, b: Long): Long = if (a < b) a else b