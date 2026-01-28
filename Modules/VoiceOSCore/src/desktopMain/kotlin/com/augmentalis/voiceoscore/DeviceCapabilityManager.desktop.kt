/**
 * DeviceCapabilityManager.desktop.kt - Desktop actual implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-22
 *
 * Desktop implementation of DeviceCapabilityManager.
 * Assumes desktop machines are generally fast devices.
 */
package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.platform.TimingConfig
import com.augmentalis.voiceoscore.platform.TimingOperation

/**
 * Desktop implementation of DeviceCapabilityManager.
 *
 * Desktop devices are typically well-resourced, so we default to FAST
 * configuration with option for user override.
 */
actual object DeviceCapabilityManager {

    actual enum class DeviceSpeed {
        FAST,
        MEDIUM,
        SLOW
    }

    private var cachedSpeed: DeviceSpeed? = null
    private var userOverrideDebounce: Long? = null

    // Default timing configurations - Desktop uses FAST timings
    private val fastTimings = mapOf(
        TimingOperation.CONTENT_CHANGE to TimingConfig(debounceMs = 100, minIntervalMs = 50, canSkip = false),
        TimingOperation.SCROLL to TimingConfig(debounceMs = 50, minIntervalMs = 30, canSkip = true),
        TimingOperation.FULL_SCRAPE to TimingConfig(debounceMs = 200, minIntervalMs = 100, canSkip = false),
        TimingOperation.INCREMENTAL_UPDATE to TimingConfig(debounceMs = 75, minIntervalMs = 50, canSkip = true),
        TimingOperation.OVERLAY_REFRESH to TimingConfig(debounceMs = 50, minIntervalMs = 16, canSkip = true),
        TimingOperation.SPEECH_ENGINE_UPDATE to TimingConfig(debounceMs = 150, minIntervalMs = 100, canSkip = false)
    )

    private val mediumTimings = mapOf(
        TimingOperation.CONTENT_CHANGE to TimingConfig(debounceMs = 200, minIntervalMs = 100, canSkip = false),
        TimingOperation.SCROLL to TimingConfig(debounceMs = 100, minIntervalMs = 50, canSkip = true),
        TimingOperation.FULL_SCRAPE to TimingConfig(debounceMs = 300, minIntervalMs = 200, canSkip = false),
        TimingOperation.INCREMENTAL_UPDATE to TimingConfig(debounceMs = 150, minIntervalMs = 100, canSkip = true),
        TimingOperation.OVERLAY_REFRESH to TimingConfig(debounceMs = 100, minIntervalMs = 33, canSkip = true),
        TimingOperation.SPEECH_ENGINE_UPDATE to TimingConfig(debounceMs = 250, minIntervalMs = 150, canSkip = false)
    )

    private val slowTimings = mapOf(
        TimingOperation.CONTENT_CHANGE to TimingConfig(debounceMs = 300, minIntervalMs = 150, canSkip = false),
        TimingOperation.SCROLL to TimingConfig(debounceMs = 150, minIntervalMs = 75, canSkip = true),
        TimingOperation.FULL_SCRAPE to TimingConfig(debounceMs = 500, minIntervalMs = 300, canSkip = false),
        TimingOperation.INCREMENTAL_UPDATE to TimingConfig(debounceMs = 250, minIntervalMs = 150, canSkip = true),
        TimingOperation.OVERLAY_REFRESH to TimingConfig(debounceMs = 150, minIntervalMs = 50, canSkip = true),
        TimingOperation.SPEECH_ENGINE_UPDATE to TimingConfig(debounceMs = 400, minIntervalMs = 250, canSkip = false)
    )

    actual fun getContentDebounceMs(): Long {
        userOverrideDebounce?.let { return it }
        return getTimingConfig(TimingOperation.CONTENT_CHANGE).debounceMs
    }

    actual fun getScrollDebounceMs(): Long {
        userOverrideDebounce?.let { return it / 2 }
        return getTimingConfig(TimingOperation.SCROLL).debounceMs
    }

    actual fun setUserDebounceMs(ms: Long?) {
        userOverrideDebounce = ms
    }

    actual fun getUserDebounceMs(): Long? {
        return userOverrideDebounce
    }

    actual fun getDeviceSpeed(): DeviceSpeed {
        cachedSpeed?.let { return it }

        val speed = detectDeviceSpeed()
        cachedSpeed = speed
        return speed
    }

    actual fun getMaxConcurrentOperations(): Int {
        return when (getDeviceSpeed()) {
            DeviceSpeed.FAST -> 4
            DeviceSpeed.MEDIUM -> 2
            DeviceSpeed.SLOW -> 1
        }
    }

    actual fun supportsAggressiveScanning(): Boolean {
        return getDeviceSpeed() == DeviceSpeed.FAST
    }

    actual fun resetCache() {
        cachedSpeed = null
    }

    actual fun getTimingConfig(operation: TimingOperation): TimingConfig {
        val timings = when (getDeviceSpeed()) {
            DeviceSpeed.FAST -> fastTimings
            DeviceSpeed.MEDIUM -> mediumTimings
            DeviceSpeed.SLOW -> slowTimings
        }
        return timings[operation] ?: TimingConfig(debounceMs = 200, minIntervalMs = 100)
    }

    /**
     * Detect device speed using JVM runtime properties.
     * Desktop machines are generally fast, but we check memory and CPU.
     */
    private fun detectDeviceSpeed(): DeviceSpeed {
        val memoryScore = getMemoryScore()
        val cpuScore = getCpuScore()

        // Weighted average (memory is most important)
        val totalScore = (memoryScore * 0.6f) + (cpuScore * 0.4f)

        return when {
            totalScore >= 0.7f -> DeviceSpeed.FAST
            totalScore >= 0.4f -> DeviceSpeed.MEDIUM
            else -> DeviceSpeed.SLOW
        }
    }

    /**
     * Score based on JVM max memory.
     */
    private fun getMemoryScore(): Float {
        val runtime = Runtime.getRuntime()
        val maxMemoryGB = runtime.maxMemory() / (1024.0 * 1024.0 * 1024.0)

        return when {
            maxMemoryGB >= 4.0 -> 1.0f   // 4GB+ heap = high-end
            maxMemoryGB >= 2.0 -> 0.8f   // 2GB+ heap = good
            maxMemoryGB >= 1.0 -> 0.6f   // 1GB+ heap = decent
            maxMemoryGB >= 0.5 -> 0.4f   // 512MB+ heap = limited
            else -> 0.2f                  // <512MB = very constrained
        }
    }

    /**
     * Score based on CPU cores.
     */
    private fun getCpuScore(): Float {
        val cores = Runtime.getRuntime().availableProcessors()
        return when {
            cores >= 8 -> 1.0f
            cores >= 6 -> 0.8f
            cores >= 4 -> 0.6f
            cores >= 2 -> 0.4f
            else -> 0.2f
        }
    }
}
