package com.augmentalis.Avanues.web.universal.xr

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * CommonPerformanceMonitor - Platform-agnostic XR performance monitoring
 *
 * Contains shared logic for:
 * - FPS tracking and statistics
 * - Warning generation based on thresholds
 * - Metrics aggregation
 *
 * Platform implementations provide:
 * - Battery level reading
 * - Temperature reading
 * - Thermal status reading
 */
abstract class CommonPerformanceMonitor(
    protected val coroutineScope: CoroutineScope
) {
    // State flows
    protected val _metrics = MutableStateFlow(PerformanceMetrics())
    val metrics: StateFlow<PerformanceMetrics> = _metrics.asStateFlow()

    protected val _warnings = MutableStateFlow<List<PerformanceWarning>>(emptyList())
    val warnings: StateFlow<List<PerformanceWarning>> = _warnings.asStateFlow()

    protected val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    // Internal state
    private var monitoringJob: Job? = null
    private var sessionStartTime: Long = 0L
    private val fpsHistory = mutableListOf<Float>()
    private var lastBatteryLevel: Int = 100
    private var batteryCheckTime: Long = 0L

    /**
     * Start monitoring performance during XR session
     */
    fun startMonitoring() {
        if (_isMonitoring.value) return

        sessionStartTime = currentTimeMillis()
        lastBatteryLevel = getBatteryLevel()
        batteryCheckTime = sessionStartTime
        fpsHistory.clear()

        _isMonitoring.value = true

        monitoringJob = coroutineScope.launch {
            while (isActive) {
                updateMetrics()
                checkWarnings()
                delay(MONITORING_INTERVAL_MS)
            }
        }
    }

    /**
     * Stop monitoring performance
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        _isMonitoring.value = false
        fpsHistory.clear()
        _warnings.value = emptyList()
    }

    /**
     * Update current frame rate
     *
     * @param fps Current frames per second
     */
    fun updateFrameRate(fps: Float) {
        fpsHistory.add(fps)
        if (fpsHistory.size > FPS_HISTORY_SIZE) {
            fpsHistory.removeAt(0)
        }

        val avgFps = if (fpsHistory.isNotEmpty()) fpsHistory.average().toFloat() else fps
        val minFps = fpsHistory.minOrNull() ?: fps
        val maxFps = fpsHistory.maxOrNull() ?: fps
        val frameDrops = fpsHistory.count { it < FPS_TARGET * 0.9f }

        _metrics.value = _metrics.value.copy(
            fps = fps,
            averageFps = avgFps,
            minFps = minFps,
            maxFps = maxFps,
            frameDrops = frameDrops
        )
    }

    /**
     * Update all performance metrics
     */
    private fun updateMetrics() {
        val uptime = currentTimeMillis() - sessionStartTime
        val battery = getBatteryLevel()
        val temp = getBatteryTemperature()
        val thermal = getThermalStatus()
        val drainRate = calculateBatteryDrainRate(battery)

        _metrics.value = _metrics.value.copy(
            batteryLevel = battery,
            batteryTemperature = temp,
            thermalStatus = thermal,
            isDraining = drainRate > 0,
            drainRatePerHour = drainRate,
            uptime = uptime
        )
    }

    /**
     * Check for performance warnings
     */
    private fun checkWarnings() {
        val newWarnings = mutableListOf<PerformanceWarning>()
        val m = _metrics.value
        val now = currentTimeMillis()

        // FPS warnings
        when {
            m.averageFps < FPS_CRITICAL_THRESHOLD -> {
                newWarnings.add(
                    PerformanceWarning(
                        type = WarningType.LOW_FPS,
                        severity = WarningSeverity.CRITICAL,
                        message = "Frame rate critically low: ${m.averageFps.toInt()}fps",
                        recommendation = "Exit XR session to prevent motion sickness",
                        timestamp = now
                    )
                )
            }
            m.averageFps < FPS_WARNING_THRESHOLD -> {
                newWarnings.add(
                    PerformanceWarning(
                        type = WarningType.LOW_FPS,
                        severity = WarningSeverity.MEDIUM,
                        message = "Frame rate below target: ${m.averageFps.toInt()}fps",
                        recommendation = "Lower XR performance mode or close background apps",
                        timestamp = now
                    )
                )
            }
        }

        // Battery warnings
        when {
            m.batteryLevel <= BATTERY_CRITICAL_LEVEL -> {
                newWarnings.add(
                    PerformanceWarning(
                        type = WarningType.BATTERY_CRITICAL,
                        severity = WarningSeverity.CRITICAL,
                        message = "Battery critically low: ${m.batteryLevel}%",
                        recommendation = "Exit XR session and charge device",
                        timestamp = now
                    )
                )
            }
            m.batteryLevel <= BATTERY_WARNING_LEVEL -> {
                newWarnings.add(
                    PerformanceWarning(
                        type = WarningType.BATTERY_LOW,
                        severity = WarningSeverity.MEDIUM,
                        message = "Battery low: ${m.batteryLevel}%",
                        recommendation = "Consider charging device soon",
                        timestamp = now
                    )
                )
            }
        }

        // Battery drain warnings
        if (m.drainRatePerHour > HIGH_DRAIN_THRESHOLD) {
            newWarnings.add(
                PerformanceWarning(
                    type = WarningType.HIGH_DRAIN,
                    severity = WarningSeverity.MEDIUM,
                    message = "High battery drain: ${m.drainRatePerHour.toInt()}%/hour",
                    recommendation = "Lower XR performance mode to extend battery life",
                    timestamp = now
                )
            )
        }

        // Thermal warnings
        when {
            m.thermalStatus >= THERMAL_CRITICAL || m.batteryTemperature >= TEMP_CRITICAL_CELSIUS -> {
                newWarnings.add(
                    PerformanceWarning(
                        type = WarningType.THERMAL_CRITICAL,
                        severity = WarningSeverity.CRITICAL,
                        message = "Device overheating: ${m.batteryTemperature.toInt()}°C",
                        recommendation = "Exit XR session immediately and let device cool down",
                        timestamp = now
                    )
                )
            }
            m.thermalStatus >= THERMAL_WARNING || m.batteryTemperature >= TEMP_WARNING_CELSIUS -> {
                newWarnings.add(
                    PerformanceWarning(
                        type = WarningType.THERMAL_WARNING,
                        severity = WarningSeverity.HIGH,
                        message = "Device getting hot: ${m.batteryTemperature.toInt()}°C",
                        recommendation = "Take a break or lower XR performance mode",
                        timestamp = now
                    )
                )
            }
        }

        _warnings.value = newWarnings
    }

    /**
     * Calculate battery drain rate in %/hour
     */
    private fun calculateBatteryDrainRate(currentLevel: Int): Float {
        val now = currentTimeMillis()
        val timeDelta = now - batteryCheckTime

        if (timeDelta < 30_000) return 0f

        val levelDelta = lastBatteryLevel - currentLevel
        val hoursElapsed = timeDelta / 3600000f

        lastBatteryLevel = currentLevel
        batteryCheckTime = now

        return if (hoursElapsed > 0 && levelDelta > 0) {
            levelDelta / hoursElapsed
        } else {
            0f
        }
    }

    /**
     * Check if session should be auto-paused due to critical conditions
     */
    fun shouldAutoPause(): Boolean {
        return _warnings.value.any { it.severity == WarningSeverity.CRITICAL }
    }

    /**
     * Get highest severity warning currently active
     */
    fun getHighestSeverity(): WarningSeverity {
        return _warnings.value.maxOfOrNull { it.severity } ?: WarningSeverity.NONE
    }

    /**
     * Get user-friendly performance summary
     */
    fun getPerformanceSummary(): String {
        val m = _metrics.value
        return buildString {
            append("FPS: ${m.fps.toInt()} (avg ${m.averageFps.toInt()})")
            append(" | Battery: ${m.batteryLevel}%")
            if (m.isDraining) {
                append(" (-${m.drainRatePerHour.toInt()}%/h)")
            }
            if (m.batteryTemperature > 0) {
                append(" | Temp: ${m.batteryTemperature.toInt()}°C")
            }
            if (m.frameDrops > 0) {
                append(" | Drops: ${m.frameDrops}")
            }
        }
    }

    /**
     * Get formatted session uptime
     */
    fun getFormattedUptime(): String {
        val uptime = _metrics.value.uptime / 1000
        val minutes = uptime / 60
        val seconds = uptime % 60
        return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }

    // ========== Abstract Methods (Platform-Specific) ==========

    /**
     * Get current battery level (0-100)
     */
    protected abstract fun getBatteryLevel(): Int

    /**
     * Get battery temperature in Celsius
     */
    protected abstract fun getBatteryTemperature(): Float

    /**
     * Get thermal throttling status (0=None to 4=Critical)
     */
    protected abstract fun getThermalStatus(): Int

    /**
     * Get current time in milliseconds
     */
    protected abstract fun currentTimeMillis(): Long

    companion object {
        // Thresholds (shared across platforms)
        const val FPS_TARGET = 60f
        const val FPS_WARNING_THRESHOLD = 45f
        const val FPS_CRITICAL_THRESHOLD = 30f
        const val BATTERY_WARNING_LEVEL = 20
        const val BATTERY_CRITICAL_LEVEL = 10
        const val HIGH_DRAIN_THRESHOLD = 15f
        const val MONITORING_INTERVAL_MS = 1000L
        const val FPS_HISTORY_SIZE = 30
        const val TEMP_WARNING_CELSIUS = 42f
        const val TEMP_CRITICAL_CELSIUS = 45f
        const val THERMAL_WARNING = 2
        const val THERMAL_CRITICAL = 4
    }
}
