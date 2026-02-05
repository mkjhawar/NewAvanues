/**
 * CursorFilter.kt - Cursor smoothing and jitter reduction (KMP)
 *
 * Provides various filtering algorithms to smooth cursor movement
 * and reduce jitter from IMU/eye tracking input.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voicecursor.filter

import com.augmentalis.voicecursor.core.CursorPosition
import com.augmentalis.voicecursor.core.FilterStrength
import kotlin.math.abs

/**
 * Cursor position filter interface
 */
interface ICursorFilter {
    fun filter(input: CursorPosition): CursorPosition
    fun reset()
}

/**
 * Exponential moving average filter for smooth cursor movement
 */
class ExponentialFilter(
    private var smoothingFactor: Float = 0.6f
) : ICursorFilter {

    private var lastPosition: CursorPosition? = null

    override fun filter(input: CursorPosition): CursorPosition {
        val last = lastPosition ?: run {
            lastPosition = input
            return input
        }

        val filtered = CursorPosition(
            x = last.x + smoothingFactor * (input.x - last.x),
            y = last.y + smoothingFactor * (input.y - last.y)
        )

        lastPosition = filtered
        return filtered
    }

    override fun reset() {
        lastPosition = null
    }

    fun setSmoothingFactor(factor: Float) {
        smoothingFactor = factor.coerceIn(0.1f, 1.0f)
    }
}

/**
 * Deadzone filter - ignores small movements
 */
class DeadzoneFilter(
    private var threshold: Float = 2f
) : ICursorFilter {

    private var lastOutput: CursorPosition? = null

    override fun filter(input: CursorPosition): CursorPosition {
        val last = lastOutput ?: run {
            lastOutput = input
            return input
        }

        val dx = abs(input.x - last.x)
        val dy = abs(input.y - last.y)

        val output = CursorPosition(
            x = if (dx > threshold) input.x else last.x,
            y = if (dy > threshold) input.y else last.y
        )

        lastOutput = output
        return output
    }

    override fun reset() {
        lastOutput = null
    }

    fun setThreshold(value: Float) {
        threshold = value.coerceIn(0f, 50f)
    }
}

/**
 * One Euro Filter - adaptive smoothing based on speed
 * Better for cursor control as it's responsive yet smooth
 */
class OneEuroFilter(
    private val minCutoff: Float = 1.0f,
    private val beta: Float = 0.007f,
    private val dCutoff: Float = 1.0f
) : ICursorFilter {

    private var xFilter = LowPassFilter(computeAlpha(minCutoff))
    private var yFilter = LowPassFilter(computeAlpha(minCutoff))
    private var dxFilter = LowPassFilter(computeAlpha(dCutoff))
    private var dyFilter = LowPassFilter(computeAlpha(dCutoff))

    private var lastTime: Long = 0
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var initialized = false

    override fun filter(input: CursorPosition): CursorPosition {
        val currentTime = currentTimeMillis()

        if (!initialized) {
            initialized = true
            lastTime = currentTime
            lastX = input.x
            lastY = input.y
            return input
        }

        val dt = ((currentTime - lastTime) / 1000.0f).coerceAtLeast(0.001f)
        lastTime = currentTime

        // Compute derivatives
        val dx = (input.x - lastX) / dt
        val dy = (input.y - lastY) / dt

        // Filter derivatives
        val filteredDx = dxFilter.filter(dx)
        val filteredDy = dyFilter.filter(dy)

        // Compute cutoff based on speed
        val cutoffX = minCutoff + beta * abs(filteredDx)
        val cutoffY = minCutoff + beta * abs(filteredDy)

        // Update filter alphas
        xFilter.setAlpha(computeAlpha(cutoffX, dt))
        yFilter.setAlpha(computeAlpha(cutoffY, dt))

        // Filter positions
        val filteredX = xFilter.filter(input.x)
        val filteredY = yFilter.filter(input.y)

        lastX = input.x
        lastY = input.y

        return CursorPosition(filteredX, filteredY)
    }

    override fun reset() {
        xFilter.reset()
        yFilter.reset()
        dxFilter.reset()
        dyFilter.reset()
        initialized = false
    }

    private fun computeAlpha(cutoff: Float, dt: Float = 1f / 60f): Float {
        val tau = 1.0f / (2 * kotlin.math.PI.toFloat() * cutoff)
        return 1.0f / (1.0f + tau / dt)
    }

    private class LowPassFilter(private var alpha: Float) {
        private var lastValue: Float? = null

        fun filter(value: Float): Float {
            val last = lastValue ?: run {
                lastValue = value
                return value
            }

            val filtered = alpha * value + (1 - alpha) * last
            lastValue = filtered
            return filtered
        }

        fun setAlpha(value: Float) {
            alpha = value.coerceIn(0f, 1f)
        }

        fun reset() {
            lastValue = null
        }
    }
}

/**
 * Composite filter combining multiple filters
 */
class CompositeCursorFilter(
    private val strength: FilterStrength = FilterStrength.Medium
) : ICursorFilter {

    private val deadzoneFilter = DeadzoneFilter(
        threshold = when (strength) {
            FilterStrength.Low -> 1f
            FilterStrength.Medium -> 2f
            FilterStrength.High -> 3f
        }
    )

    private val smoothingFilter = OneEuroFilter(
        minCutoff = when (strength) {
            FilterStrength.Low -> 2.0f
            FilterStrength.Medium -> 1.0f
            FilterStrength.High -> 0.5f
        },
        beta = when (strength) {
            FilterStrength.Low -> 0.01f
            FilterStrength.Medium -> 0.007f
            FilterStrength.High -> 0.003f
        }
    )

    override fun filter(input: CursorPosition): CursorPosition {
        val deadzoned = deadzoneFilter.filter(input)
        return smoothingFilter.filter(deadzoned)
    }

    override fun reset() {
        deadzoneFilter.reset()
        smoothingFilter.reset()
    }
}

/**
 * Platform-specific time function
 * Will be implemented via expect/actual if needed
 */
internal expect fun currentTimeMillis(): Long
