/**
 * IResourceMonitor.kt - Resource monitoring interface
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-19
 *
 * Platform-agnostic interface for monitoring system resources.
 * Implementations provide platform-specific memory/CPU tracking.
 */
package com.augmentalis.voiceoscore

import kotlinx.coroutines.flow.StateFlow

/**
 * Resource usage level for adaptive behavior.
 */
enum class ResourceLevel {
    /** Resources normal, full processing */
    NORMAL,
    /** Approaching limits, reduce non-essential work */
    WARNING,
    /** Critical pressure, minimum processing only */
    CRITICAL
}

/**
 * Throttle level recommendation for adaptive processing.
 * Higher levels indicate more aggressive resource conservation.
 */
enum class ThrottleLevel {
    /** No throttling needed */
    NONE,
    /** Light throttling - skip low priority work */
    LOW,
    /** Moderate throttling - significant reduction */
    MEDIUM,
    /** Heavy throttling - critical operations only */
    HIGH
}

/**
 * Snapshot of current resource status.
 */
data class ResourceStatus(
    val timestamp: Long = currentTimeMillis(),
    val memoryUsedPercent: Int = 0,
    val cpuUsagePercent: Int = 0,
    val level: ResourceLevel = ResourceLevel.NORMAL,
    val throttleRecommendation: ThrottleLevel = ThrottleLevel.NONE,
    val warnings: List<String> = emptyList()
) {
    val hasIssues: Boolean get() = level != ResourceLevel.NORMAL
}

/**
 * Resource monitor interface for platform-agnostic resource tracking.
 *
 * Usage:
 * ```kotlin
 * val monitor: IResourceMonitor = platformResourceMonitor()
 *
 * // Check current status
 * val status = monitor.currentStatus.value
 * if (status.throttleRecommendation >= ThrottleLevel.MEDIUM) {
 *     skipNonEssentialWork()
 * }
 *
 * // Or observe changes
 * monitor.currentStatus.collect { status ->
 *     adaptProcessing(status.throttleRecommendation)
 * }
 * ```
 */
interface IResourceMonitor {
    /**
     * Current resource status as StateFlow for reactive observation.
     */
    val currentStatus: StateFlow<ResourceStatus>

    /**
     * Start periodic resource monitoring.
     * @param intervalMs Monitoring interval in milliseconds
     */
    fun start(intervalMs: Long = 5000L)

    /**
     * Stop resource monitoring.
     */
    fun stop()

    /**
     * Get current throttle recommendation based on resource pressure.
     */
    fun getThrottleRecommendation(): ThrottleLevel

    /**
     * Check if monitoring is currently active.
     */
    fun isMonitoring(): Boolean

    /**
     * Force immediate status update (for on-demand checks).
     */
    suspend fun refreshStatus(): ResourceStatus
}

/**
 * No-op resource monitor for platforms without resource tracking.
 * Always reports NORMAL status with no throttling.
 */
class StubResourceMonitor : IResourceMonitor {
    private val _status = kotlinx.coroutines.flow.MutableStateFlow(ResourceStatus())
    override val currentStatus: StateFlow<ResourceStatus> = _status

    override fun start(intervalMs: Long) {}
    override fun stop() {}
    override fun getThrottleRecommendation(): ThrottleLevel = ThrottleLevel.NONE
    override fun isMonitoring(): Boolean = false
    override suspend fun refreshStatus(): ResourceStatus = ResourceStatus()
}
