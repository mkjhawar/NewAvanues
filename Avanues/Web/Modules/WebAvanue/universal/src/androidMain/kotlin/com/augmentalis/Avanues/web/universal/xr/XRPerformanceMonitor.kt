package com.augmentalis.Avanues.web.universal.xr

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors performance metrics during XR sessions.
 * Tracks FPS, battery, thermal state, and provides warnings.
 *
 * Requirements:
 * - REQ-XR-007: Performance Optimization for XR
 *
 * Features:
 * - Real-time FPS monitoring (60fps target)
 * - Battery level and drain rate tracking
 * - Thermal throttling detection
 * - Performance warnings
 * - Auto-pause on critical conditions
 *
 * @see <a href="/.ideacode-v2/features/012-add-webxr-support-to-webavanue-browser-to-enable-immersive-ar-vr-web-experiences/spec.md">WebXR Specification</a>
 */
class XRPerformanceMonitor(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {

    /**
     * Performance warning severity levels.
     */
    enum class WarningSeverity {
        NONE,       // No warnings
        LOW,        // Minor performance degradation
        MEDIUM,     // Noticeable performance issues
        HIGH,       // Significant issues, consider pausing
        CRITICAL    // Critical issues, must pause
    }

    /**
     * Performance warning types.
     */
    enum class WarningType {
        LOW_FPS,            // Frame rate below 45fps
        BATTERY_LOW,        // Battery below 20%
        BATTERY_CRITICAL,   // Battery below 10%
        THERMAL_WARNING,    // Device getting hot
        THERMAL_CRITICAL,   // Device overheating
        HIGH_DRAIN          // Battery draining too fast
    }

    /**
     * Performance metrics snapshot.
     */
    data class PerformanceMetrics(
        val fps: Float = 0f,
        val averageFps: Float = 0f,
        val minFps: Float = 0f,
        val maxFps: Float = 0f,
        val frameDrops: Int = 0,
        val batteryLevel: Int = 100,
        val batteryTemperature: Float = 0f,
        val thermalStatus: Int = 0, // 0=None, 1=Light, 2=Moderate, 3=Severe, 4=Critical
        val isDraining: Boolean = false,
        val drainRatePerHour: Float = 0f,
        val uptime: Long = 0L // Session uptime in milliseconds
    )

    /**
     * Performance warning with severity and recommendations.
     */
    data class PerformanceWarning(
        val type: WarningType,
        val severity: WarningSeverity,
        val message: String,
        val recommendation: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    // State flows
    private val _metrics = MutableStateFlow(PerformanceMetrics())
    val metrics: StateFlow<PerformanceMetrics> = _metrics.asStateFlow()

    private val _warnings = MutableStateFlow<List<PerformanceWarning>>(emptyList())
    val warnings: StateFlow<List<PerformanceWarning>> = _warnings.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    // Internal state
    private var monitoringJob: Job? = null
    private var sessionStartTime: Long = 0L
    private val fpsHistory = mutableListOf<Float>()
    private var lastBatteryLevel: Int = 100
    private var batteryCheckTime: Long = 0L

    companion object {
        private const val FPS_TARGET = 60f
        private const val FPS_WARNING_THRESHOLD = 45f
        private const val FPS_CRITICAL_THRESHOLD = 30f
        private const val BATTERY_WARNING_LEVEL = 20
        private const val BATTERY_CRITICAL_LEVEL = 10
        private const val HIGH_DRAIN_THRESHOLD = 15f // %/hour
        private const val MONITORING_INTERVAL_MS = 1000L // 1 second
        private const val FPS_HISTORY_SIZE = 30 // 30 seconds of history
        private const val TEMP_WARNING_CELSIUS = 42f
        private const val TEMP_CRITICAL_CELSIUS = 45f
    }

    /**
     * Start monitoring performance during XR session.
     * Should be called when XR session becomes active.
     */
    fun startMonitoring() {
        if (_isMonitoring.value) return

        sessionStartTime = System.currentTimeMillis()
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
     * Stop monitoring performance.
     * Should be called when XR session ends.
     */
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        _isMonitoring.value = false
        fpsHistory.clear()
        _warnings.value = emptyList()
    }

    /**
     * Update current frame rate.
     * Should be called from XR render loop or animation frame callback.
     *
     * @param fps Current frames per second
     */
    fun updateFrameRate(fps: Float) {
        // Add to history
        fpsHistory.add(fps)
        if (fpsHistory.size > FPS_HISTORY_SIZE) {
            fpsHistory.removeAt(0)
        }

        // Calculate statistics
        val avgFps = if (fpsHistory.isNotEmpty()) fpsHistory.average().toFloat() else fps
        val minFps = fpsHistory.minOrNull() ?: fps
        val maxFps = fpsHistory.maxOrNull() ?: fps
        val frameDrops = fpsHistory.count { it < FPS_TARGET * 0.9f }

        // Update metrics
        _metrics.value = _metrics.value.copy(
            fps = fps,
            averageFps = avgFps,
            minFps = minFps,
            maxFps = maxFps,
            frameDrops = frameDrops
        )
    }

    /**
     * Update all performance metrics.
     */
    private fun updateMetrics() {
        val uptime = System.currentTimeMillis() - sessionStartTime
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
     * Check for performance warnings.
     */
    private fun checkWarnings() {
        val newWarnings = mutableListOf<PerformanceWarning>()
        val m = _metrics.value

        // FPS warnings
        when {
            m.averageFps < FPS_CRITICAL_THRESHOLD -> {
                newWarnings.add(
                    PerformanceWarning(
                        type = WarningType.LOW_FPS,
                        severity = WarningSeverity.CRITICAL,
                        message = "Frame rate critically low: ${m.averageFps.toInt()}fps",
                        recommendation = "Exit XR session to prevent motion sickness"
                    )
                )
            }
            m.averageFps < FPS_WARNING_THRESHOLD -> {
                newWarnings.add(
                    PerformanceWarning(
                        type = WarningType.LOW_FPS,
                        severity = WarningSeverity.MEDIUM,
                        message = "Frame rate below target: ${m.averageFps.toInt()}fps",
                        recommendation = "Lower XR performance mode or close background apps"
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
                        recommendation = "Exit XR session and charge device"
                    )
                )
            }
            m.batteryLevel <= BATTERY_WARNING_LEVEL -> {
                newWarnings.add(
                    PerformanceWarning(
                        type = WarningType.BATTERY_LOW,
                        severity = WarningSeverity.MEDIUM,
                        message = "Battery low: ${m.batteryLevel}%",
                        recommendation = "Consider charging device soon"
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
                    recommendation = "Lower XR performance mode to extend battery life"
                )
            )
        }

        // Thermal warnings
        when {
            m.thermalStatus >= 4 || m.batteryTemperature >= TEMP_CRITICAL_CELSIUS -> {
                newWarnings.add(
                    PerformanceWarning(
                        type = WarningType.THERMAL_CRITICAL,
                        severity = WarningSeverity.CRITICAL,
                        message = "Device overheating: ${m.batteryTemperature.toInt()}°C",
                        recommendation = "Exit XR session immediately and let device cool down"
                    )
                )
            }
            m.thermalStatus >= 2 || m.batteryTemperature >= TEMP_WARNING_CELSIUS -> {
                newWarnings.add(
                    PerformanceWarning(
                        type = WarningType.THERMAL_WARNING,
                        severity = WarningSeverity.HIGH,
                        message = "Device getting hot: ${m.batteryTemperature.toInt()}°C",
                        recommendation = "Take a break or lower XR performance mode"
                    )
                )
            }
        }

        _warnings.value = newWarnings
    }

    /**
     * Get current battery level.
     *
     * @return Battery level percentage (0-100)
     */
    private fun getBatteryLevel(): Int {
        val batteryStatus = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        return if (level >= 0 && scale > 0) {
            (level * 100 / scale)
        } else {
            100
        }
    }

    /**
     * Get battery temperature in Celsius.
     *
     * @return Temperature in °C
     */
    private fun getBatteryTemperature(): Float {
        val batteryStatus = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val temp = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1

        return if (temp >= 0) {
            temp / 10f // Temperature is in tenths of degree Celsius
        } else {
            0f
        }
    }

    /**
     * Get thermal throttling status.
     *
     * @return Thermal status (0=None, 1=Light, 2=Moderate, 3=Severe, 4=Critical)
     */
    private fun getThermalStatus(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            return powerManager?.currentThermalStatus ?: 0
        }
        return 0
    }

    /**
     * Calculate battery drain rate in %/hour.
     *
     * @param currentLevel Current battery level
     * @return Drain rate in percent per hour
     */
    private fun calculateBatteryDrainRate(currentLevel: Int): Float {
        val now = System.currentTimeMillis()
        val timeDelta = now - batteryCheckTime

        // Need at least 30 seconds of data
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
     * Check if session should be auto-paused due to critical conditions.
     *
     * @return true if session should pause
     */
    fun shouldAutoPause(): Boolean {
        return _warnings.value.any { it.severity == WarningSeverity.CRITICAL }
    }

    /**
     * Get highest severity warning currently active.
     *
     * @return Highest warning severity, or NONE if no warnings
     */
    fun getHighestSeverity(): WarningSeverity {
        return _warnings.value.maxOfOrNull { it.severity } ?: WarningSeverity.NONE
    }

    /**
     * Get user-friendly performance summary.
     *
     * @return Human-readable performance status
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
     * Get formatted session uptime.
     *
     * @return Uptime as "MM:SS" string
     */
    fun getFormattedUptime(): String {
        val uptime = _metrics.value.uptime / 1000 // Convert to seconds
        val minutes = uptime / 60
        val seconds = uptime % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    /**
     * Clean up resources.
     */
    fun cleanup() {
        stopMonitoring()
        coroutineScope.cancel()
    }
}
