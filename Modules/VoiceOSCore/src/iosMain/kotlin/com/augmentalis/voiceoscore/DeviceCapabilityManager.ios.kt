/**
 * DeviceCapabilityManager.ios.kt - iOS actual implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-28
 *
 * iOS implementation of DeviceCapabilityManager.
 * iOS devices are generally well-optimized, so we default to FAST.
 */
package com.augmentalis.voiceoscore

/**
 * iOS implementation of DeviceCapabilityManager.
 *
 * iOS devices are typically well-optimized, so we default to FAST
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

    /**
     * No-op on iOS - no initialization needed.
     */
    actual fun init(context: Any) {
        // No-op on iOS
    }

    // Default timing configurations - iOS uses FAST timings
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

        // iOS devices are generally fast
        val speed = DeviceSpeed.FAST
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
}
