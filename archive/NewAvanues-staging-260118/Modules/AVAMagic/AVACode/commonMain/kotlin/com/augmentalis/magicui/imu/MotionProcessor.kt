package com.augmentalis.avamagic.imu

/**
 * Interface for composable motion processing in the IMU pipeline.
 *
 * Motion processors transform raw sensor data through a chain of operations:
 * ```
 * Raw IMU → AxisLocker → RateLimiter → Smoother → Cursor Movement
 * ```
 *
 * Each processor operates independently and can be enabled/disabled at runtime.
 *
 * ## Example Implementations
 *
 * - **AxisLockerProcessor**: Locks pitch, roll, or yaw independently
 * - **RateLimiterProcessor**: Applies acceleration/deceleration curves
 * - **MotionSmootherProcessor**: Smoothing algorithms (One Euro, Exponential, Moving Average)
 *
 * ## Performance Requirements
 *
 * Processors MUST maintain 60 FPS (16.67ms budget per frame):
 * - Process() should complete in <2ms
 * - Avoid allocations in hot path
 * - Use pre-allocated buffers where possible
 *
 * @since 3.1.0
 */
interface MotionProcessor {
    /**
     * Unique processor identifier (e.g., "axis_locker", "one_euro_filter").
     */
    val processorId: String

    /**
     * Human-readable processor name (e.g., "Axis Locker", "One Euro Filter").
     */
    val displayName: String

    /**
     * Whether this processor is currently enabled.
     */
    var isEnabled: Boolean

    /**
     * Processes IMU orientation data and returns transformed data.
     *
     * This method is called at 60 FPS, so performance is critical.
     *
     * @param input Raw or partially processed orientation data
     * @return Transformed orientation data
     */
    fun process(input: IMUOrientationData): IMUOrientationData

    /**
     * Resets the processor's internal state.
     *
     * Called when:
     * - User recalibrates IMU
     * - Preset changes (AR → VR → Accessibility)
     * - Processor is re-enabled after being disabled
     */
    fun reset()

    /**
     * Returns processor-specific configuration as a map.
     *
     * Used for persistence and UI display.
     *
     * Example:
     * ```kotlin
     * mapOf(
     *     "lock_pitch" to true,
     *     "lock_roll" to false,
     *     "lock_yaw" to false
     * )
     * ```
     */
    fun getConfiguration(): Map<String, Any>

    /**
     * Applies processor-specific configuration from a map.
     *
     * @param config Configuration map (from getConfiguration() or user settings)
     */
    fun applyConfiguration(config: Map<String, Any>)
}
