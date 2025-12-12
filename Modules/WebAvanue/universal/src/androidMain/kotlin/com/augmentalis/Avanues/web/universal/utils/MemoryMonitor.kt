package com.augmentalis.Avanues.web.universal.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * PERFORMANCE OPTIMIZATION Phase 2: Memory Monitoring
 *
 * Monitors app memory usage and detects memory pressure conditions.
 * Helps ensure the app runs smoothly on low-memory devices (2GB RAM).
 *
 * Features:
 * - Real-time memory usage tracking
 * - Memory pressure detection (low, moderate, critical)
 * - Memory leak detection hints
 * - Automatic GC suggestions
 *
 * Usage:
 * ```kotlin
 * // In your ViewModel or Component with lifecycle
 * val memoryMonitor = MemoryMonitor(context, viewModelScope)
 * memoryMonitor.start()
 *
 * // Observe memory state
 * val memoryState by memoryMonitor.memoryState.collectAsState()
 * if (memoryState.pressureLevel == MemoryPressureLevel.CRITICAL) {
 *     // Take action: clear caches, close tabs, etc.
 * }
 *
 * // Monitoring automatically stops when the provided scope is cancelled
 * ```
 *
 * @param context Android context for accessing system services
 * @param scope CoroutineScope that controls the lifecycle of monitoring.
 *              When this scope is cancelled, monitoring stops automatically.
 *              Use viewModelScope, lifecycleScope, or another appropriate lifecycle-aware scope.
 */
class MemoryMonitor(
    private val context: Context,
    private val scope: CoroutineScope
) {

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private var monitoringJob: Job? = null

    private val _memoryState = MutableStateFlow(MemoryState())
    val memoryState: StateFlow<MemoryState> = _memoryState.asStateFlow()

    /**
     * Start monitoring memory usage
     * @param intervalMs Monitoring interval in milliseconds (default 5000ms = 5 seconds)
     */
    fun start(intervalMs: Long = 5000) {
        stop() // Stop any existing monitoring

        monitoringJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                updateMemoryState()
                delay(intervalMs)
            }
        }
    }

    /**
     * Stop monitoring memory usage
     */
    fun stop() {
        monitoringJob?.cancel()
        monitoringJob = null
    }

    /**
     * Get current memory state (one-time snapshot)
     */
    fun getMemorySnapshot(): MemoryState {
        updateMemoryState()
        return _memoryState.value
    }

    /**
     * Update memory state
     */
    private fun updateMemoryState() {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val runtime = Runtime.getRuntime()
        val usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMemoryMB = runtime.maxMemory() / (1024 * 1024)
        val availableMemoryMB = memInfo.availMem / (1024 * 1024)
        val totalMemoryMB = memInfo.totalMem / (1024 * 1024)

        val usagePercent = (usedMemoryMB.toFloat() / maxMemoryMB.toFloat() * 100).toInt()

        // Determine memory pressure level
        val pressureLevel = when {
            memInfo.lowMemory -> MemoryPressureLevel.CRITICAL
            usagePercent > 85 -> MemoryPressureLevel.HIGH
            usagePercent > 70 -> MemoryPressureLevel.MODERATE
            else -> MemoryPressureLevel.NORMAL
        }

        _memoryState.value = MemoryState(
            usedMemoryMB = usedMemoryMB,
            maxMemoryMB = maxMemoryMB,
            availableMemoryMB = availableMemoryMB,
            totalMemoryMB = totalMemoryMB,
            usagePercent = usagePercent,
            pressureLevel = pressureLevel,
            isLowMemory = memInfo.lowMemory,
            timestamp = System.currentTimeMillis()
        )

        // Log memory warnings
        when (pressureLevel) {
            MemoryPressureLevel.HIGH -> {
                Logger.warn("MemoryMonitor", "HIGH memory pressure: ${usedMemoryMB}MB / ${maxMemoryMB}MB ($usagePercent%)")
            }
            MemoryPressureLevel.CRITICAL -> {
                Logger.error("MemoryMonitor", "CRITICAL memory pressure: ${usedMemoryMB}MB / ${maxMemoryMB}MB ($usagePercent%) - System low memory flag set")
            }
            else -> {
                // Normal/Moderate - no logging to reduce noise
            }
        }
    }

    companion object {
        /**
         * Get device memory class (low-end, mid-range, high-end)
         */
        fun getDeviceMemoryClass(context: Context): DeviceMemoryClass {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)

            val totalMemoryGB = memInfo.totalMem / (1024 * 1024 * 1024)

            return when {
                totalMemoryGB < 3 -> DeviceMemoryClass.LOW_END      // <3GB
                totalMemoryGB < 6 -> DeviceMemoryClass.MID_RANGE    // 3-6GB
                else -> DeviceMemoryClass.HIGH_END                   // >6GB
            }
        }

        /**
         * Check if device is low-memory (for adaptive optimization)
         */
        fun isLowMemoryDevice(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.isLowRamDevice
            } else {
                getDeviceMemoryClass(context) == DeviceMemoryClass.LOW_END
            }
        }
    }
}

/**
 * Memory state snapshot
 */
data class MemoryState(
    val usedMemoryMB: Long = 0,
    val maxMemoryMB: Long = 0,
    val availableMemoryMB: Long = 0,
    val totalMemoryMB: Long = 0,
    val usagePercent: Int = 0,
    val pressureLevel: MemoryPressureLevel = MemoryPressureLevel.NORMAL,
    val isLowMemory: Boolean = false,
    val timestamp: Long = 0
) {
    /**
     * Human-readable memory usage string
     */
    fun toDisplayString(): String {
        return "${usedMemoryMB}MB / ${maxMemoryMB}MB ($usagePercent%)"
    }

    /**
     * Should trigger cleanup actions
     */
    fun shouldCleanup(): Boolean {
        return pressureLevel == MemoryPressureLevel.HIGH || pressureLevel == MemoryPressureLevel.CRITICAL
    }
}

/**
 * Memory pressure levels
 */
enum class MemoryPressureLevel {
    NORMAL,      // <70% usage
    MODERATE,    // 70-85% usage
    HIGH,        // 85%+ usage
    CRITICAL     // System low memory flag set
}

/**
 * Device memory classification
 */
enum class DeviceMemoryClass {
    LOW_END,     // <3GB RAM
    MID_RANGE,   // 3-6GB RAM
    HIGH_END     // >6GB RAM
}
