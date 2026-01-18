/**
 * MovingAverage.kt
 * Path: /libraries/DeviceManager/src/main/java/com/augmentalis/devicemanager/sensors/imu/MovingAverage.kt
 *
 * Created: 2025-09-19 14:35 IST
 * Author: VOS4 Development Team
 * Version: 1.0.0
 *
 * Purpose: Moving average implementation for sensor data smoothing
 * Based on proven legacy algorithm for cursor movement filtering
 */

package com.augmentalis.devicemanager.sensors.imu

/**
 * MovingAverage class calculates a smoothed average of the input data using the
 * Smoothed Moving Average (SMMA) algorithm over a specified sample size or time period.
 * This moving average class considers both the sampling period and the time passed limit.
 *
 * Smoothed moving average:
 * SUM1 = SUM(CLOSE, N)
 * SMMA1 = SUM1/N
 *
 * The second and succeeding moving averages are calculated according to this formula:
 *
 * PREVSUM = SMMA(i-1) *N
 * SMMA(i) = (PREVSUM-SMMA(i-1)+CLOSE(i))/N = SMMA(i-1) + (CLOSE(i)-SMMA(i-1))/N
 * @param maxSamples The maximum number of samples to keep in the window.
 * @param maxTimeWindowNs The time window (in nanoseconds) for which samples are kept.
 */
class MovingAverage(
    private val maxSamples: Int,  // Maximum number of samples to retain
    private val maxTimeWindowNs: Long    // Maximum time (in nanoseconds) over which samples are retained
) {
    // Pre-allocated circular buffer
    private val values = FloatArray(maxSamples)
    private val timestamps = LongArray(maxSamples)
    private var head = 0
    private var size = 0
    private var runningSum = 0f

    /**
     * Adds a new data point and calculates the smoothed moving average (SMMA).
     *
     * @param data The new data point to add.
     * @param ts The timestamp of the new data point (in nanoseconds).
     * @return The current moving average.
     */
    fun getAvg(data: Float, ts: Long): Float {
        add(data, ts)
        return if (size == 0) 0f else runningSum / size
    }

    /**
     * Adds a new data point to the moving average and removes old data points based on
     * the sample number and sample time.
     *
     * @param data The new data point.
     * @param ts The timestamp of the new data point (in nanoseconds).
     */
    private fun add(data: Float, ts: Long) {
        // Remove expired samples first
        purgeOldSamples(ts)

        // If buffer is full, overwrite oldest entry
        if (size == maxSamples) {
            runningSum -= values[head]
            head = (head + 1) % maxSamples
            size--
        }

        // Add new sample
        val insertPos = (head + size) % maxSamples
        values[insertPos] = data
        timestamps[insertPos] = ts
        runningSum += data
        size++
    }

    private fun purgeOldSamples(currentTs: Long) {
        var i = 0
        while (i < size) {
            val idx = (head + i) % maxSamples
            val age = currentTs - timestamps[idx]

            if (age > maxTimeWindowNs) {
                // Remove this sample
                runningSum -= values[idx]
                head = (head + 1) % maxSamples
                size--
            } else {
                // If this sample is not expired, newer samples won't be either
                break
            }
        }
    }

    /**
     * Recursively calculates the Smoothed Moving Average (SMMA).
     *
     * @return The calculated SMMA value.
     */
    fun getSMMA(): Float {
        if (size == 0) return 0f

        var result = values[head]
        for (i in 1 until size) {
            val idx = (head + i) % maxSamples
            result += (values[idx] - result) / (i + 1)
        }

        return result
    }

    /**
     * Clear all samples from the moving average
     */
    fun clear() {
        head = 0
        size = 0
        runningSum = 0f
    }

    /**
     * Get current sample count
     */
    fun getSize(): Int = size

    /**
     * Check if the moving average has any samples
     */
    fun isEmpty(): Boolean = size == 0
}