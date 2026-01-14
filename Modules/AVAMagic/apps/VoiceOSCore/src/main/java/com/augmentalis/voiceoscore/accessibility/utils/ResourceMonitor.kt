/**
 * ResourceMonitor.kt - Resource monitoring utility
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-22
 *
 * Purpose: Monitors service health metrics (memory, CPU) to prevent resource exhaustion
 * Provides early warning for resource constraints
 */
package com.augmentalis.voiceoscore.accessibility.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.Process
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Resource monitoring utility for service health tracking
 *
 * Monitors:
 * - Memory usage (heap, native, total)
 * - CPU usage (process level)
 * - Resource thresholds with warnings
 *
 * Thread-safe and coroutine-based periodic monitoring
 *
 * Usage:
 * ```
 * val monitor = ResourceMonitor(context, scope)
 * monitor.start { status ->
 *     when (status.level) {
 *         ResourceLevel.CRITICAL -> handleCritical()
 *         ResourceLevel.WARNING -> handleWarning()
 *         else -> {}
 *     }
 * }
 * ```
 */
class ResourceMonitor(
    private val context: Context,
    private val scope: CoroutineScope,
    private val intervalMs: Long = Const.RESOURCE_MONITOR_INTERVAL_MS
) {

    companion object {
        private const val TAG = "ResourceMonitor"
        private const val BYTES_IN_MB = 1024 * 1024
    }

    /**
     * Resource usage level
     */
    enum class ResourceLevel {
        NORMAL,     // Everything OK
        WARNING,    // Approaching limits
        CRITICAL    // Immediate action needed
    }

    /**
     * Throttle level for adaptive resource management
     * Used to dynamically adjust processing intensity
     */
    enum class ThrottleLevel {
        NONE,       // No throttling needed - full processing
        LOW,        // Light throttling - reduce non-essential work
        MEDIUM,     // Moderate throttling - significant reduction
        HIGH        // Heavy throttling - minimum processing only
    }

    /**
     * Resource status snapshot
     */
    data class ResourceStatus(
        val timestamp: Long = System.currentTimeMillis(),
        val memoryUsedMb: Long = 0,
        val memoryMaxMb: Long = 0,
        val memoryUsagePercent: Int = 0,
        val nativeMemoryMb: Long = 0,
        val cpuUsagePercent: Double = 0.0,
        val level: ResourceLevel = ResourceLevel.NORMAL,
        val warnings: List<String> = emptyList()
    ) {
        /**
         * Check if status indicates problems
         */
        fun hasIssues(): Boolean = level != ResourceLevel.NORMAL

        /**
         * Get human-readable summary
         */
        fun getSummary(): String {
            return buildString {
                append("Memory: $memoryUsedMb/$memoryMaxMb MB (${memoryUsagePercent}%)")
                if (nativeMemoryMb > 0) {
                    append(", Native: $nativeMemoryMb MB")
                }
                append(", CPU: ${"%.1f".format(cpuUsagePercent)}%")
                append(", Level: $level")
                if (warnings.isNotEmpty()) {
                    append(", Warnings: ${warnings.joinToString(", ")}")
                }
            }
        }
    }

    /**
     * Callback for resource status updates
     */
    fun interface StatusCallback {
        fun onStatusUpdate(status: ResourceStatus)
    }

    private val activityManager: ActivityManager? =
        context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

    private val isMonitoring = AtomicBoolean(false)
    private var monitoringJob: Job? = null
    private var statusCallback: StatusCallback? = null

    // CPU monitoring state
    private val lastCpuTime = AtomicLong(0)
    private val lastAppTime = AtomicLong(0)

    /**
     * Latest resource status
     */
    @Volatile
    private var latestStatus: ResourceStatus = ResourceStatus()

    /**
     * Start resource monitoring
     *
     * @param callback Callback invoked on each status update
     */
    fun start(callback: StatusCallback? = null) {
        if (isMonitoring.getAndSet(true)) {
            Log.w(TAG, "Resource monitoring already running")
            return
        }

        statusCallback = callback
        Log.i(TAG, "Starting resource monitoring (interval: ${intervalMs}ms)")

        monitoringJob = scope.launch {
            while (isActive && isMonitoring.get()) {
                try {
                    val status = collectResourceStatus()
                    latestStatus = status

                    // Log warnings and critical issues
                    when (status.level) {
                        ResourceLevel.WARNING -> {
                            Log.w(TAG, "Resource warning: ${status.getSummary()}")
                        }
                        ResourceLevel.CRITICAL -> {
                            Log.e(TAG, "Resource critical: ${status.getSummary()}")
                        }
                        else -> {
                            Log.d(TAG, "Resources normal: ${status.getSummary()}")
                        }
                    }

                    // Notify callback
                    statusCallback?.onStatusUpdate(status)

                } catch (e: Exception) {
                    Log.e(TAG, "Error collecting resource status", e)
                }

                delay(intervalMs)
            }
        }
    }

    /**
     * Stop resource monitoring
     */
    fun stop() {
        if (!isMonitoring.getAndSet(false)) {
            return
        }

        Log.i(TAG, "Stopping resource monitoring")
        monitoringJob?.cancel()
        monitoringJob = null
        statusCallback = null
    }

    /**
     * Get current resource status
     * Returns cached status if monitoring is active
     *
     * @param forceUpdate Force fresh collection even if monitoring
     * @return Current resource status
     */
    fun getStatus(forceUpdate: Boolean = false): ResourceStatus {
        return if (forceUpdate || !isMonitoring.get()) {
            collectResourceStatus()
        } else {
            latestStatus
        }
    }

    /**
     * Collect current resource status
     *
     * @return Fresh resource status snapshot
     */
    private fun collectResourceStatus(): ResourceStatus {
        val memoryInfo = getMemoryInfo()
        val cpuUsage = getCpuUsage()

        val warnings = mutableListOf<String>()
        var level = ResourceLevel.NORMAL

        // Check memory thresholds
        if (memoryInfo.memoryUsagePercent >= Const.MEMORY_CRITICAL_THRESHOLD) {
            level = ResourceLevel.CRITICAL
            warnings.add("Memory critical: ${memoryInfo.memoryUsagePercent}%")
        } else if (memoryInfo.memoryUsagePercent >= Const.MEMORY_WARNING_THRESHOLD) {
            if (level == ResourceLevel.NORMAL) level = ResourceLevel.WARNING
            warnings.add("Memory high: ${memoryInfo.memoryUsagePercent}%")
        }

        // Check CPU thresholds
        if (cpuUsage >= Const.CPU_CRITICAL_THRESHOLD) {
            level = ResourceLevel.CRITICAL
            warnings.add("CPU critical: ${"%.1f".format(cpuUsage)}%")
        } else if (cpuUsage >= Const.CPU_WARNING_THRESHOLD) {
            if (level == ResourceLevel.NORMAL) level = ResourceLevel.WARNING
            warnings.add("CPU high: ${"%.1f".format(cpuUsage)}%")
        }

        return ResourceStatus(
            memoryUsedMb = memoryInfo.memoryUsedMb,
            memoryMaxMb = memoryInfo.memoryMaxMb,
            memoryUsagePercent = memoryInfo.memoryUsagePercent,
            nativeMemoryMb = memoryInfo.nativeMemoryMb,
            cpuUsagePercent = cpuUsage,
            level = level,
            warnings = warnings
        )
    }

    /**
     * Memory information data class
     */
    private data class MemoryInfo(
        val memoryUsedMb: Long,
        val memoryMaxMb: Long,
        val memoryUsagePercent: Int,
        val nativeMemoryMb: Long
    )

    /**
     * Get memory usage information
     */
    private fun getMemoryInfo(): MemoryInfo {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / BYTES_IN_MB
        val maxMemory = runtime.maxMemory() / BYTES_IN_MB
        val usagePercent = if (maxMemory > 0) {
            ((usedMemory.toDouble() / maxMemory) * 100).toInt()
        } else 0

        // Get native memory if available
        val nativeMemory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Debug.getNativeHeapAllocatedSize() / BYTES_IN_MB
        } else {
            0L
        }

        return MemoryInfo(
            memoryUsedMb = usedMemory,
            memoryMaxMb = maxMemory,
            memoryUsagePercent = usagePercent,
            nativeMemoryMb = nativeMemory
        )
    }

    /**
     * Get CPU usage percentage
     * Returns process-level CPU usage
     */
    private fun getCpuUsage(): Double {
        return try {
            val pid = Process.myPid()
            val cpuInfo = readProcessCpuInfo(pid)

            val currentCpuTime = cpuInfo.first
            val currentAppTime = cpuInfo.second

            val lastCpu = lastCpuTime.get()
            val lastApp = lastAppTime.get()

            if (lastCpu > 0 && lastApp > 0) {
                val cpuDiff = currentCpuTime - lastCpu
                val appDiff = currentAppTime - lastApp

                lastCpuTime.set(currentCpuTime)
                lastAppTime.set(currentAppTime)

                if (cpuDiff > 0) {
                    (appDiff.toDouble() / cpuDiff) * 100.0
                } else {
                    0.0
                }
            } else {
                // First measurement, store baseline
                lastCpuTime.set(currentCpuTime)
                lastAppTime.set(currentAppTime)
                0.0
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get CPU usage", e)
            0.0
        }
    }

    /**
     * Read CPU time from /proc filesystem
     * Returns pair of (total CPU time, app CPU time)
     */
    private fun readProcessCpuInfo(pid: Int): Pair<Long, Long> {
        var totalCpuTime = 0L
        var appCpuTime = 0L

        try {
            // Read total CPU time from /proc/stat
            BufferedReader(FileReader("/proc/stat")).use { reader ->
                val line = reader.readLine()
                val tokens = line.split("\\s+".toRegex())
                if (tokens.isNotEmpty() && tokens[0] == "cpu") {
                    for (i in 1 until tokens.size.coerceAtMost(9)) {
                        totalCpuTime += tokens[i].toLongOrNull() ?: 0L
                    }
                }
            }

            // Read app CPU time from /proc/[pid]/stat
            BufferedReader(FileReader("/proc/$pid/stat")).use { reader ->
                val line = reader.readLine()
                val tokens = line.split("\\s+".toRegex())
                if (tokens.size >= 15) {
                    val utime = tokens[13].toLongOrNull() ?: 0L
                    val stime = tokens[14].toLongOrNull() ?: 0L
                    appCpuTime = utime + stime
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Failed to read CPU info from /proc", e)
        } catch (e: NumberFormatException) {
            Log.w(TAG, "Failed to parse CPU info", e)
        }

        return Pair(totalCpuTime, appCpuTime)
    }

    /**
     * Check if monitoring is active
     */
    fun isMonitoring(): Boolean = isMonitoring.get()

    /**
     * Trigger garbage collection
     * Use sparingly, only in critical memory situations
     */
    fun requestGarbageCollection() {
        Log.i(TAG, "Requesting garbage collection")
        System.gc()
    }

    /**
     * Get memory pressure level from ActivityManager
     * Requires ActivityManager.MemoryInfo
     */
    fun getMemoryPressure(): String {
        return try {
            val memInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memInfo)

            when {
                memInfo.lowMemory -> "LOW"
                memInfo.availMem < memInfo.threshold * 1.5 -> "MODERATE"
                else -> "NORMAL"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get memory pressure", e)
            "UNKNOWN"
        }
    }

    /**
     * Get recommended throttle level based on current resource status
     *
     * Maps resource levels to throttle recommendations:
     * - CRITICAL: HIGH throttle (minimum processing)
     * - WARNING: MEDIUM throttle (significant reduction)
     * - NORMAL with high memory: LOW throttle (light reduction)
     * - NORMAL: NONE (full processing)
     *
     * @return Recommended ThrottleLevel for current conditions
     */
    fun getThrottleRecommendation(): ThrottleLevel {
        val status = getStatus()
        return when (status.level) {
            ResourceLevel.CRITICAL -> ThrottleLevel.HIGH
            ResourceLevel.WARNING -> ThrottleLevel.MEDIUM
            ResourceLevel.NORMAL -> {
                // Check if we're approaching limits
                if (status.memoryUsagePercent >= 70 || status.cpuUsagePercent >= 70) {
                    ThrottleLevel.LOW
                } else {
                    ThrottleLevel.NONE
                }
            }
        }
    }
}
