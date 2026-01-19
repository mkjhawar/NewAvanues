/**
 * AndroidResourceMonitor.kt - Android resource monitoring implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-19
 *
 * Android-specific implementation of IResourceMonitor.
 * Monitors heap memory, native memory, and CPU usage.
 */
package com.augmentalis.voiceoscore

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.Process
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileReader
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Android implementation of resource monitoring.
 *
 * Monitors:
 * - Heap memory usage (Java heap)
 * - Native memory usage (NDK allocations)
 * - CPU usage (process level)
 *
 * Provides throttle recommendations based on resource pressure.
 */
class AndroidResourceMonitor(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : IResourceMonitor {

    companion object {
        private const val TAG = "AndroidResourceMonitor"
        private const val BYTES_IN_MB = 1024 * 1024

        // Thresholds
        private const val MEMORY_WARNING_PERCENT = 75
        private const val MEMORY_CRITICAL_PERCENT = 90
        private const val CPU_WARNING_PERCENT = 70
        private const val CPU_CRITICAL_PERCENT = 85
    }

    private val _currentStatus = MutableStateFlow(ResourceStatus())
    override val currentStatus: StateFlow<ResourceStatus> = _currentStatus.asStateFlow()

    private val isMonitoringFlag = AtomicBoolean(false)
    private var monitoringJob: Job? = null

    // CPU tracking state
    private val lastCpuTime = AtomicLong(0)
    private val lastAppTime = AtomicLong(0)

    private val activityManager: ActivityManager? =
        context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

    override fun start(intervalMs: Long) {
        if (isMonitoringFlag.getAndSet(true)) {
            Log.w(TAG, "Resource monitoring already running")
            return
        }

        Log.i(TAG, "Starting resource monitoring (interval: ${intervalMs}ms)")

        monitoringJob = scope.launch {
            while (isActive && isMonitoringFlag.get()) {
                try {
                    val status = collectStatus()
                    _currentStatus.value = status

                    when (status.level) {
                        ResourceLevel.WARNING -> Log.w(TAG, "Resource warning: ${status.warnings}")
                        ResourceLevel.CRITICAL -> Log.e(TAG, "Resource critical: ${status.warnings}")
                        else -> Log.v(TAG, "Resources normal (mem: ${status.memoryUsedPercent}%, cpu: ${status.cpuUsagePercent}%)")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error collecting resource status", e)
                }

                delay(intervalMs)
            }
        }
    }

    override fun stop() {
        if (!isMonitoringFlag.getAndSet(false)) return

        Log.i(TAG, "Stopping resource monitoring")
        monitoringJob?.cancel()
        monitoringJob = null
    }

    override fun getThrottleRecommendation(): ThrottleLevel {
        val status = currentStatus.value
        return status.throttleRecommendation
    }

    override fun isMonitoring(): Boolean = isMonitoringFlag.get()

    override suspend fun refreshStatus(): ResourceStatus = withContext(Dispatchers.IO) {
        collectStatus().also { _currentStatus.value = it }
    }

    private fun collectStatus(): ResourceStatus {
        val memoryPercent = getMemoryUsagePercent()
        val cpuPercent = getCpuUsagePercent()

        val warnings = mutableListOf<String>()
        var level = ResourceLevel.NORMAL

        // Check memory thresholds
        when {
            memoryPercent >= MEMORY_CRITICAL_PERCENT -> {
                level = ResourceLevel.CRITICAL
                warnings.add("Memory critical: $memoryPercent%")
            }
            memoryPercent >= MEMORY_WARNING_PERCENT -> {
                level = ResourceLevel.WARNING
                warnings.add("Memory high: $memoryPercent%")
            }
        }

        // Check CPU thresholds
        when {
            cpuPercent >= CPU_CRITICAL_PERCENT -> {
                if (level != ResourceLevel.CRITICAL) level = ResourceLevel.CRITICAL
                warnings.add("CPU critical: $cpuPercent%")
            }
            cpuPercent >= CPU_WARNING_PERCENT -> {
                if (level == ResourceLevel.NORMAL) level = ResourceLevel.WARNING
                warnings.add("CPU high: $cpuPercent%")
            }
        }

        // Calculate throttle recommendation
        val throttle = when (level) {
            ResourceLevel.CRITICAL -> ThrottleLevel.HIGH
            ResourceLevel.WARNING -> ThrottleLevel.MEDIUM
            ResourceLevel.NORMAL -> {
                if (memoryPercent >= 60 || cpuPercent >= 60) ThrottleLevel.LOW
                else ThrottleLevel.NONE
            }
        }

        return ResourceStatus(
            timestamp = System.currentTimeMillis(),
            memoryUsedPercent = memoryPercent,
            cpuUsagePercent = cpuPercent,
            level = level,
            throttleRecommendation = throttle,
            warnings = warnings
        )
    }

    private fun getMemoryUsagePercent(): Int {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        return if (maxMemory > 0) {
            ((usedMemory.toDouble() / maxMemory) * 100).toInt()
        } else 0
    }

    private fun getCpuUsagePercent(): Int {
        return try {
            val pid = Process.myPid()
            val (totalCpu, appCpu) = readProcessCpuInfo(pid)

            val lastCpu = lastCpuTime.get()
            val lastApp = lastAppTime.get()

            if (lastCpu > 0 && lastApp > 0) {
                val cpuDiff = totalCpu - lastCpu
                val appDiff = appCpu - lastApp

                lastCpuTime.set(totalCpu)
                lastAppTime.set(appCpu)

                if (cpuDiff > 0) {
                    ((appDiff.toDouble() / cpuDiff) * 100).toInt().coerceIn(0, 100)
                } else 0
            } else {
                lastCpuTime.set(totalCpu)
                lastAppTime.set(appCpu)
                0
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get CPU usage", e)
            0
        }
    }

    private fun readProcessCpuInfo(pid: Int): Pair<Long, Long> {
        var totalCpuTime = 0L
        var appCpuTime = 0L

        try {
            // Read total CPU time from /proc/stat
            BufferedReader(FileReader("/proc/stat")).use { reader ->
                val line = reader.readLine()
                val tokens = line.split("\\s+".toRegex())
                if (tokens.isNotEmpty() && tokens[0] == "cpu") {
                    for (i in 1 until minOf(tokens.size, 9)) {
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
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read CPU info from /proc", e)
        }

        return Pair(totalCpuTime, appCpuTime)
    }
}

/**
 * Factory function to create Android resource monitor.
 */
fun createResourceMonitor(context: Context): IResourceMonitor {
    return AndroidResourceMonitor(context)
}
