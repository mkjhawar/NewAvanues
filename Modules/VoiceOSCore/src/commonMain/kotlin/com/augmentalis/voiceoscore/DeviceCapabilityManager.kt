package com.augmentalis.voiceoscore

/**
 * Device Capability Manager - Manages device-specific performance settings.
 *
 * Adjusts debounce and other timing parameters based on device capability.
 * Supports user override for fine-grained control.
 *
 * ## Device Speed Classification:
 * - FAST: High-end devices (flagship phones, tablets)
 * - MEDIUM: Mid-range devices (typical consumer devices)
 * - SLOW: Low-end or older devices
 *
 * ## Usage:
 * ```kotlin
 * // Get optimal debounce for content changes
 * val debounce = DeviceCapabilityManager.getContentDebounceMs()
 *
 * // User override
 * DeviceCapabilityManager.setUserDebounceMs(150L)
 *
 * // Check device classification
 * val speed = DeviceCapabilityManager.getDeviceSpeed()
 * ```
 */
expect object DeviceCapabilityManager {

    /**
     * Device speed classification.
     */
    enum class DeviceSpeed {
        FAST,
        MEDIUM,
        SLOW
    }

    /**
     * Get optimal debounce delay for content changes.
     * Priority: User override > Device capability > Default
     *
     * @return Debounce delay in milliseconds
     */
    fun getContentDebounceMs(): Long

    /**
     * Get optimal debounce delay for scroll events.
     * Typically shorter than content changes to feel more responsive.
     *
     * @return Scroll debounce delay in milliseconds
     */
    fun getScrollDebounceMs(): Long

    /**
     * Allow user to override debounce setting.
     * Set to null to revert to automatic detection.
     *
     * @param ms Custom debounce in milliseconds, or null to auto-detect
     */
    fun setUserDebounceMs(ms: Long?)

    /**
     * Get the user's custom debounce setting, if set.
     *
     * @return User-specified debounce in ms, or null if using auto-detection
     */
    fun getUserDebounceMs(): Long?

    /**
     * Detect device speed classification.
     * Result is cached after first call.
     *
     * @return DeviceSpeed classification
     */
    fun getDeviceSpeed(): DeviceSpeed

    /**
     * Get the maximum number of concurrent operations recommended.
     * Based on device capability.
     *
     * @return Recommended max concurrent operations
     */
    fun getMaxConcurrentOperations(): Int

    /**
     * Check if device supports aggressive scanning.
     * Fast devices can handle more frequent re-scrapes.
     *
     * @return True if device can handle aggressive scanning
     */
    fun supportsAggressiveScanning(): Boolean

    /**
     * Reset cached device capability detection.
     * Call this if device state changes significantly.
     */
    fun resetCache()

    /**
     * Get timing configuration for a specific operation.
     * Provides fine-grained control over operation-specific delays.
     *
     * @param operation Operation identifier
     * @return Timing configuration for the operation
     */
    fun getTimingConfig(operation: TimingOperation): TimingConfig
}

/**
 * Operation types for timing configuration.
 */
enum class TimingOperation {
    /** Content change detection (scroll, list updates) */
    CONTENT_CHANGE,

    /** Scroll event processing */
    SCROLL,

    /** Full screen scrape */
    FULL_SCRAPE,

    /** Incremental update */
    INCREMENTAL_UPDATE,

    /** Overlay refresh */
    OVERLAY_REFRESH,

    /** Speech engine update */
    SPEECH_ENGINE_UPDATE
}

/**
 * Timing configuration for an operation.
 */
data class TimingConfig(
    /** Debounce delay in milliseconds */
    val debounceMs: Long,

    /** Minimum interval between operations in milliseconds */
    val minIntervalMs: Long,

    /** Maximum queue depth for batching */
    val maxQueueDepth: Int = 1,

    /** Whether operation can be skipped under load */
    val canSkip: Boolean = false
)
