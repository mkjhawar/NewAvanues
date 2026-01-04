/**
 * SpeedController.kt - Cursor speed management and acceleration control
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.accessibility.cursor

import android.util.Log
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/**
 * Speed presets for cursor movement
 */
enum class CursorSpeed(
    val pixelsPerSecond: Float,
    val accelerationTime: Long,
    val description: String
) {
    PRECISION(50f, 0L, "Precision mode - 5x slower for fine control"),
    SLOW(250f, 500L, "Slow speed - 25% of normal"),
    MEDIUM(1000f, 300L, "Medium speed - normal cursor movement"),
    FAST(2000f, 200L, "Fast speed - 2x normal speed"),
    VERY_FAST(4000f, 100L, "Very fast - 4x normal speed");

    companion object {
        fun fromString(speed: String): CursorSpeed? {
            return when (speed.lowercase().trim()) {
                "precision", "precise", "fine" -> PRECISION
                "slow", "slower" -> SLOW
                "medium", "normal", "default" -> MEDIUM
                "fast", "faster" -> FAST
                "very fast", "fastest", "max" -> VERY_FAST
                else -> null
            }
        }
    }
}

/**
 * Easing functions for smooth acceleration
 */
enum class EasingFunction {
    LINEAR,
    EASE_IN,
    EASE_OUT,
    EASE_IN_OUT,
    EXPONENTIAL,
    CIRCULAR,
    BACK
}

/**
 * Controller for cursor speed and acceleration
 *
 * Features:
 * - Speed preset management
 * - Acceleration curve application
 * - Velocity calculations
 * - Precision mode toggle
 * - Smooth easing functions
 *
 * Usage:
 * ```
 * val controller = SpeedController()
 * controller.setSpeed(CursorSpeed.FAST)
 * val velocity = controller.calculateVelocity(deltaTime)
 * val easedProgress = controller.applyEasing(progress, EasingFunction.EASE_OUT)
 * ```
 */
class SpeedController {

    companion object {
        private const val TAG = "SpeedController"

        // Precision mode multiplier
        private const val PRECISION_MULTIPLIER = 0.2f  // 5x slower

        // Maximum velocity cap (prevent excessive speed)
        private const val MAX_VELOCITY = 5000f  // pixels per second

        // Minimum movement threshold (prevent jitter)
        private const val MIN_MOVEMENT_THRESHOLD = 1f  // pixels
    }

    // Current speed setting
    private var currentSpeed: CursorSpeed = CursorSpeed.MEDIUM

    // Precision mode flag
    private var precisionModeEnabled: Boolean = false

    // Current velocity (pixels per second)
    private var currentVelocity: Float = 0f

    // Target velocity (for acceleration)
    private var targetVelocity: Float = currentSpeed.pixelsPerSecond

    // Acceleration start time
    private var accelerationStartTime: Long = 0L

    // Last update time
    private var lastUpdateTime: Long = System.currentTimeMillis()

    /**
     * Set cursor speed preset
     *
     * @param speed The speed preset to apply
     */
    fun setSpeed(speed: CursorSpeed) {
        Log.d(TAG, "Setting cursor speed: ${speed.name} (${speed.pixelsPerSecond}px/s)")
        currentSpeed = speed
        targetVelocity = calculateTargetVelocity()
        accelerationStartTime = System.currentTimeMillis()
    }

    /**
     * Get current speed preset
     */
    fun getSpeed(): CursorSpeed = currentSpeed

    /**
     * Enable or disable precision mode
     *
     * @param enabled true to enable precision mode (5x slower)
     */
    fun setPrecisionMode(enabled: Boolean) {
        Log.d(TAG, "Precision mode: ${if (enabled) "ENABLED" else "DISABLED"}")
        precisionModeEnabled = enabled
        targetVelocity = calculateTargetVelocity()
        accelerationStartTime = System.currentTimeMillis()
    }

    /**
     * Check if precision mode is enabled
     */
    fun isPrecisionMode(): Boolean = precisionModeEnabled

    /**
     * Toggle precision mode
     */
    fun togglePrecisionMode() {
        setPrecisionMode(!precisionModeEnabled)
    }

    /**
     * Calculate target velocity based on speed and precision mode
     */
    private fun calculateTargetVelocity(): Float {
        val baseVelocity = currentSpeed.pixelsPerSecond
        val multiplier = if (precisionModeEnabled) PRECISION_MULTIPLIER else 1f
        return (baseVelocity * multiplier).coerceAtMost(MAX_VELOCITY)
    }

    /**
     * Calculate current velocity with acceleration curve
     *
     * @param deltaTime Time elapsed since last update (milliseconds)
     * @return Current velocity in pixels per second
     */
    fun calculateVelocity(deltaTime: Long = 0L): Float {
        val currentTime = System.currentTimeMillis()
        val actualDeltaTime = if (deltaTime > 0) deltaTime else (currentTime - lastUpdateTime)
        lastUpdateTime = currentTime

        // If no acceleration time, jump to target immediately
        if (currentSpeed.accelerationTime == 0L) {
            currentVelocity = targetVelocity
            return currentVelocity
        }

        // Calculate acceleration progress (0.0 to 1.0)
        val elapsedTime = currentTime - accelerationStartTime
        val progress = (elapsedTime.toFloat() / currentSpeed.accelerationTime).coerceIn(0f, 1f)

        // Apply easing function for smooth acceleration
        val easedProgress = applyEasing(progress, EasingFunction.EASE_OUT)

        // Interpolate velocity
        currentVelocity = currentVelocity + (targetVelocity - currentVelocity) * easedProgress

        Log.v(TAG, "Velocity: ${currentVelocity.toInt()}px/s (progress: ${(easedProgress * 100).toInt()}%)")

        return currentVelocity
    }

    /**
     * Calculate distance to move based on velocity and time
     *
     * @param deltaTime Time elapsed (milliseconds)
     * @param direction Direction vector (normalized)
     * @return Distance to move in pixels
     */
    fun calculateDistance(deltaTime: Long, direction: Pair<Float, Float> = Pair(1f, 0f)): Pair<Float, Float> {
        val velocity = calculateVelocity(deltaTime)
        val deltaSeconds = deltaTime / 1000f
        val distance = velocity * deltaSeconds

        // Apply direction vector
        val dx = distance * direction.first
        val dy = distance * direction.second

        // Apply minimum threshold
        if (kotlin.math.abs(dx) < MIN_MOVEMENT_THRESHOLD && kotlin.math.abs(dy) < MIN_MOVEMENT_THRESHOLD) {
            return Pair(0f, 0f)
        }

        return Pair(dx, dy)
    }

    /**
     * Apply easing function to progress value (0.0 to 1.0)
     *
     * @param progress Input progress (0.0 to 1.0)
     * @param easing Easing function to apply
     * @return Eased progress (0.0 to 1.0)
     */
    fun applyEasing(progress: Float, easing: EasingFunction): Float {
        val t = progress.coerceIn(0f, 1f)

        return when (easing) {
            EasingFunction.LINEAR -> t

            EasingFunction.EASE_IN -> t * t

            EasingFunction.EASE_OUT -> t * (2f - t)

            EasingFunction.EASE_IN_OUT -> {
                if (t < 0.5f) {
                    2f * t * t
                } else {
                    -1f + (4f - 2f * t) * t
                }
            }

            EasingFunction.EXPONENTIAL -> {
                if (t == 0f) 0f
                else if (t == 1f) 1f
                else 2f.pow(10f * (t - 1f))
            }

            EasingFunction.CIRCULAR -> {
                1f - kotlin.math.sqrt(1f - t * t)
            }

            EasingFunction.BACK -> {
                val c1 = 1.70158f
                val c3 = c1 + 1f
                c3 * t * t * t - c1 * t * t
            }
        }
    }

    /**
     * Apply sine wave easing (smooth start and end)
     *
     * @param progress Input progress (0.0 to 1.0)
     * @return Sine-eased progress (0.0 to 1.0)
     */
    fun applySineEasing(progress: Float): Float {
        val t = progress.coerceIn(0f, 1f)
        return (sin((t * PI - PI / 2).toFloat()) + 1f) / 2f
    }

    /**
     * Calculate time to reach distance at current speed
     *
     * @param distance Distance in pixels
     * @return Time in milliseconds
     */
    fun calculateTimeForDistance(distance: Float): Long {
        val velocity = calculateVelocity()
        if (velocity == 0f) return 0L

        val seconds = distance / velocity
        return (seconds * 1000).toLong()
    }

    /**
     * Calculate pixels moved per frame at given frame rate
     *
     * @param frameRate Target frame rate (default 60fps)
     * @return Pixels per frame
     */
    fun calculatePixelsPerFrame(frameRate: Int = 60): Float {
        val velocity = calculateVelocity()
        return velocity / frameRate
    }

    /**
     * Reset velocity and acceleration state
     */
    fun reset() {
        Log.d(TAG, "Resetting speed controller")
        currentVelocity = 0f
        accelerationStartTime = System.currentTimeMillis()
        lastUpdateTime = System.currentTimeMillis()
    }

    /**
     * Get current velocity
     */
    fun getCurrentVelocity(): Float = currentVelocity

    /**
     * Get target velocity
     */
    fun getTargetVelocity(): Float = targetVelocity

    /**
     * Get current speed info as string
     */
    fun getSpeedInfo(): String {
        val mode = if (precisionModeEnabled) "PRECISION" else currentSpeed.name
        return "$mode (${currentVelocity.toInt()}px/s target: ${targetVelocity.toInt()}px/s)"
    }
}
