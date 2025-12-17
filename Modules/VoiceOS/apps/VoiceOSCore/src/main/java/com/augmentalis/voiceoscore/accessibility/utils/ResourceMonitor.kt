/**
 * ResourceMonitor.kt - Monitors system resources for VoiceOS
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-16
 *
 * Monitors memory, CPU, and battery to enable resource-aware operation.
 * Used to dynamically adjust VoiceOS behavior based on available resources.
 *
 * STUB: This class was referenced but not implemented. Added as stub to allow build.
 */
package com.augmentalis.voiceoscore.accessibility.utils

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Debug
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Resource Monitor
 *
 * Monitors system resources and provides state flows for resource-aware
 * decision making throughout VoiceOS.
 *
 * @param context Application context
 */
class ResourceMonitor(
    private val context: Context
) {
    companion object {
        private const val TAG = "ResourceMonitor"

        // Monitoring intervals
        private const val MONITOR_INTERVAL_MS = 30000L  // 30 seconds

        // Memory thresholds
        private const val MEMORY_LOW_THRESHOLD_MB = 100
        private const val MEMORY_CRITICAL_THRESHOLD_MB = 50

        // Battery thresholds
        private const val BATTERY_LOW_THRESHOLD = 20
        private const val BATTERY_CRITICAL_THRESHOLD = 10
    }

    /**
     * Resource state data class.
     */
    data class ResourceState(
        val availableMemoryMb: Long = 0,
        val totalMemoryMb: Long = 0,
        val usedMemoryPercent: Int = 0,
        val batteryLevel: Int = 100,
        val isCharging: Boolean = false,
        val isBatteryLow: Boolean = false,
        val isMemoryLow: Boolean = false
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null

    private val _resourceState = MutableStateFlow(ResourceState())
    val resourceState: StateFlow<ResourceState> = _resourceState.asStateFlow()

    private val _isLowResourceMode = MutableStateFlow(false)
    val isLowResourceMode: StateFlow<Boolean> = _isLowResourceMode.asStateFlow()

    private val activityManager: ActivityManager by lazy {
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }

    /**
     * Start resource monitoring.
     */
    fun start() {
        if (monitorJob?.isActive == true) {
            Log.d(TAG, "Resource monitor already running")
            return
        }

        monitorJob = scope.launch {
            while (isActive) {
                updateResourceState()
                delay(MONITOR_INTERVAL_MS)
            }
        }
        Log.d(TAG, "Resource monitoring started")
    }

    /**
     * Stop resource monitoring.
     */
    fun stop() {
        monitorJob?.cancel()
        monitorJob = null
        Log.d(TAG, "Resource monitoring stopped")
    }

    /**
     * Update the current resource state.
     */
    private fun updateResourceState() {
        val memoryInfo = getMemoryInfo()
        val batteryInfo = getBatteryInfo()

        val state = ResourceState(
            availableMemoryMb = memoryInfo.first,
            totalMemoryMb = memoryInfo.second,
            usedMemoryPercent = if (memoryInfo.second > 0) {
                ((memoryInfo.second - memoryInfo.first) * 100 / memoryInfo.second).toInt()
            } else 0,
            batteryLevel = batteryInfo.first,
            isCharging = batteryInfo.second,
            isBatteryLow = batteryInfo.first <= BATTERY_LOW_THRESHOLD,
            isMemoryLow = memoryInfo.first <= MEMORY_LOW_THRESHOLD_MB
        )

        _resourceState.value = state
        _isLowResourceMode.value = state.isMemoryLow || state.isBatteryLow

        if (_isLowResourceMode.value) {
            Log.w(TAG, "Low resource mode active - Memory: ${state.availableMemoryMb}MB, Battery: ${state.batteryLevel}%")
        }
    }

    /**
     * Get current memory info.
     * @return Pair of (available MB, total MB)
     */
    private fun getMemoryInfo(): Pair<Long, Long> {
        return try {
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            val availableMb = memInfo.availMem / (1024 * 1024)
            val totalMb = memInfo.totalMem / (1024 * 1024)
            Pair(availableMb, totalMb)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting memory info", e)
            Pair(0L, 0L)
        }
    }

    /**
     * Get current battery info.
     * @return Pair of (battery level, isCharging)
     */
    private fun getBatteryInfo(): Pair<Int, Boolean> {
        return try {
            val batteryIntent = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

            val batteryLevel = if (level >= 0 && scale > 0) (level * 100 / scale) else 100
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            Pair(batteryLevel, isCharging)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting battery info", e)
            Pair(100, false)
        }
    }

    /**
     * Check if we should enable battery-saving features.
     */
    fun shouldSaveBattery(): Boolean {
        val state = _resourceState.value
        return state.isBatteryLow && !state.isCharging
    }

    /**
     * Check if we should reduce memory usage.
     */
    fun shouldReduceMemory(): Boolean {
        return _resourceState.value.isMemoryLow
    }

    /**
     * Get recommended operation mode based on current resources.
     */
    fun getRecommendedMode(): OperationMode {
        val state = _resourceState.value
        return when {
            state.availableMemoryMb <= MEMORY_CRITICAL_THRESHOLD_MB -> OperationMode.MINIMAL
            state.batteryLevel <= BATTERY_CRITICAL_THRESHOLD && !state.isCharging -> OperationMode.MINIMAL
            state.isMemoryLow || state.isBatteryLow -> OperationMode.REDUCED
            else -> OperationMode.FULL
        }
    }

    /**
     * Clean up resources.
     */
    fun cleanup() {
        stop()
        scope.cancel()
        Log.d(TAG, "Resource monitor cleaned up")
    }

    /**
     * Operation modes based on resource availability.
     */
    enum class OperationMode {
        /** Full functionality enabled */
        FULL,
        /** Reduced functionality (disable non-essential features) */
        REDUCED,
        /** Minimal functionality (only essential operations) */
        MINIMAL
    }
}
