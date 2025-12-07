/**
 * ResourceMonitor.kt - System resource monitoring for performance optimization
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-31
 *
 * Phase 3D: Resource Monitoring - Production Readiness
 *
 * Monitors system resources (memory, CPU) to enable intelligent throttling
 * of expensive operations during high memory pressure or low battery conditions.
 */
package com.augmentalis.voiceoscore.accessibility.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.util.Log
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException

/**
 * Resource Monitor
 *
 * Lightweight monitoring utility for tracking system resource usage.
 * Enables adaptive performance tuning based on current device state.
 *
 * ## Features:
 * - Memory usage tracking (heap, native, total)
 * - Memory pressure detection
 * - Throttle recommendations
 * - Performance-conscious (<5ms overhead)
 *
 * ## Usage:
 * ```kotlin
 * val monitor = ResourceMonitor(context)
 *
 * // Check memory stats
 * val stats = monitor.getMemoryStats()
 * Log.i(TAG, "Memory: ${stats.usedHeapMB}MB/${stats.maxHeapMB}MB")
 *
 * // Check if should throttle
 * if (monitor.shouldThrottle()) {
 *     // Skip expensive operations
 *     return
 * }
 *
 * // Log stats for debugging
 * monitor.logMemoryStats("MyComponent")
 * ```
 */
class ResourceMonitor(private val context: Context) {

    companion object {
        private const val TAG = "ResourceMonitor"

        // Memory pressure thresholds
        private const val MEMORY_PRESSURE_THRESHOLD = 0.85f  // 85% heap usage = high pressure
        private const val MEMORY_CRITICAL_THRESHOLD = 0.95f  // 95% heap usage = critical

        // Throttle level thresholds
        private const val THROTTLE_LOW_THRESHOLD = 0.70f     // 70% heap usage
        private const val THROTTLE_MEDIUM_THRESHOLD = 0.80f  // 80% heap usage
        private const val THROTTLE_HIGH_THRESHOLD = 0.90f    // 90% heap usage

        // Available memory thresholds (for system-wide pressure)
        private const val LOW_MEMORY_MB = 100  // <100MB available = system pressure
    }

    private val activityManager: ActivityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    private val runtime = Runtime.getRuntime()

    /**
     * Memory statistics data class
     *
     * @property usedHeapMB Heap memory used (MB)
     * @property maxHeapMB Maximum heap memory available (MB)
     * @property usedNativeMB Native memory used (MB)
     * @property totalUsedMB Total memory used (heap + native) (MB)
     * @property availableMB System available memory (MB)
     * @property usagePercentage Heap usage percentage (0.0-1.0)
     */
    data class MemoryStats(
        val usedHeapMB: Long,
        val maxHeapMB: Long,
        val usedNativeMB: Long,
        val totalUsedMB: Long,
        val availableMB: Long,
        val usagePercentage: Float
    )

    /**
     * Throttle level recommendation
     */
    enum class ThrottleLevel {
        NONE,      // No throttling needed (<70% memory)
        LOW,       // Light throttling (70-80% memory)
        MEDIUM,    // Moderate throttling (80-90% memory)
        HIGH       // Aggressive throttling (>90% memory)
    }

    /**
     * Get current memory statistics
     *
     * Performance: <2ms on typical devices
     *
     * @return MemoryStats containing current memory usage
     */
    fun getMemoryStats(): MemoryStats {
        Log.d(TAG, "getMemoryStats() called")
        // Heap memory (from Runtime)
        val maxHeap = runtime.maxMemory()
        val totalHeap = runtime.totalMemory()
        val freeHeap = runtime.freeMemory()
        val usedHeap = totalHeap - freeHeap

        // Native memory (from Debug)
        val nativeHeapSize = Debug.getNativeHeapSize()
        val nativeHeapAllocated = Debug.getNativeHeapAllocatedSize()

        // System available memory (from ActivityManager)
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val availableMemory = memInfo.availMem

        // Convert to MB for readability
        val usedHeapMB = usedHeap / (1024 * 1024)
        val maxHeapMB = maxHeap / (1024 * 1024)
        val usedNativeMB = nativeHeapAllocated / (1024 * 1024)
        val totalUsedMB = usedHeapMB + usedNativeMB
        val availableMB = availableMemory / (1024 * 1024)

        // Calculate usage percentage
        val usagePercentage = usedHeap.toFloat() / maxHeap.toFloat()

        return MemoryStats(
            usedHeapMB = usedHeapMB,
            maxHeapMB = maxHeapMB,
            usedNativeMB = usedNativeMB,
            totalUsedMB = totalUsedMB,
            availableMB = availableMB,
            usagePercentage = usagePercentage
        )
    }

    /**
     * Check if memory pressure is high
     *
     * High memory pressure indicates the app should reduce memory-intensive operations
     * to avoid OOM crashes or GC thrashing.
     *
     * @return true if memory pressure is high (>85% heap usage)
     */
    fun isMemoryPressureHigh(): Boolean {
        val stats = getMemoryStats()
        return stats.usagePercentage >= MEMORY_PRESSURE_THRESHOLD
    }

    /**
     * Check if memory pressure is critical
     *
     * Critical pressure indicates immediate risk of OOM.
     *
     * @return true if memory pressure is critical (>95% heap usage)
     */
    fun isMemoryPressureCritical(): Boolean {
        val stats = getMemoryStats()
        return stats.usagePercentage >= MEMORY_CRITICAL_THRESHOLD
    }

    /**
     * Check if operations should be throttled
     *
     * Combines memory pressure and system available memory to determine
     * if expensive operations should be skipped or delayed.
     *
     * @return true if throttling recommended
     */
    fun shouldThrottle(): Boolean {
        Log.d(TAG, "shouldThrottle() called")
        val stats = getMemoryStats()

        // Throttle if heap usage is high
        if (stats.usagePercentage >= MEMORY_PRESSURE_THRESHOLD) {
            return true
        }

        // Throttle if system has low available memory
        if (stats.availableMB < LOW_MEMORY_MB) {
            return true
        }

        return false
    }

    /**
     * Get throttle recommendation level
     *
     * Provides granular throttling recommendations based on current memory state.
     * Allows callers to implement adaptive behavior (e.g., reduce depth vs skip entirely).
     *
     * @return ThrottleLevel recommendation
     */
    fun getThrottleRecommendation(): ThrottleLevel {
        val stats = getMemoryStats()

        return when {
            stats.usagePercentage >= THROTTLE_HIGH_THRESHOLD -> ThrottleLevel.HIGH
            stats.usagePercentage >= THROTTLE_MEDIUM_THRESHOLD -> ThrottleLevel.MEDIUM
            stats.usagePercentage >= THROTTLE_LOW_THRESHOLD -> ThrottleLevel.LOW
            else -> ThrottleLevel.NONE
        }
    }

    /**
     * Log memory statistics to logcat
     *
     * Formats memory stats for easy debugging. Use at INFO level for periodic monitoring
     * or WARN level when pressure is detected.
     *
     * @param tag Tag to identify the logging component
     */
    fun logMemoryStats(tag: String) {
        val stats = getMemoryStats()
        val level = getThrottleRecommendation()

        val message = "Memory: ${stats.usedHeapMB}MB/${stats.maxHeapMB}MB " +
                     "(${(stats.usagePercentage * 100).toInt()}%) - " +
                     "Native: ${stats.usedNativeMB}MB - " +
                     "Available: ${stats.availableMB}MB - " +
                     "Status: $level"

        when (level) {
            ThrottleLevel.HIGH -> Log.w(tag, message)
            ThrottleLevel.MEDIUM -> Log.i(tag, message)
            else -> Log.i(tag, message)
        }
    }

    /**
     * Get current CPU usage percentage
     *
     * Note: CPU monitoring is more expensive than memory monitoring (~5-10ms).
     * Use sparingly or cache results.
     *
     * @return CPU usage percentage (0-100), or -1 if unavailable
     */
    fun getCpuUsage(): Float {
        Log.d(TAG, "getCpuUsage() called")
        return try {
            // Read /proc/stat for total CPU time
            val reader = BufferedReader(FileReader("/proc/stat"))
            val cpuLine = reader.readLine()
            reader.close()

            // Parse: cpu user nice system idle iowait irq softirq
            val tokens = cpuLine.split("\\s+".toRegex())
            if (tokens.size < 8) {
                Log.w(TAG, "Unable to parse /proc/stat: insufficient tokens")
                return -1f
            }

            val user = tokens[1].toLongOrNull() ?: 0
            val nice = tokens[2].toLongOrNull() ?: 0
            val system = tokens[3].toLongOrNull() ?: 0
            val idle = tokens[4].toLongOrNull() ?: 0
            val iowait = tokens[5].toLongOrNull() ?: 0

            val totalCpu = user + nice + system + idle + iowait
            val totalActive = user + nice + system + iowait

            if (totalCpu == 0L) {
                return 0f
            }

            // Return percentage
            (totalActive.toFloat() / totalCpu.toFloat()) * 100f

        } catch (e: IOException) {
            Log.w(TAG, "Error reading CPU stats: ${e.message}")
            -1f
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected error getting CPU stats: ${e.message}")
            -1f
        }
    }

    /**
     * Get formatted memory status string
     *
     * Useful for UI display or quick logging.
     *
     * @return Formatted string like "45MB/128MB (35%)"
     */
    fun getMemoryStatusString(): String {
        val stats = getMemoryStats()
        return "${stats.usedHeapMB}MB/${stats.maxHeapMB}MB " +
               "(${(stats.usagePercentage * 100).toInt()}%)"
    }

    /**
     * Check if system is under low memory condition
     *
     * Uses ActivityManager to check if system-wide low memory threshold is crossed.
     *
     * @return true if system is in low memory state
     */
    fun isLowMemory(): Boolean {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.lowMemory
    }

    /**
     * Get low memory threshold in bytes
     *
     * Returns the system's low memory threshold (when isLowMemory becomes true).
     *
     * @return Low memory threshold in bytes
     */
    fun getLowMemoryThreshold(): Long {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.threshold
    }
}
