/**
 * Debouncer.kt - Event debouncing utility to prevent excessive processing
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-09-17
 */
package com.augmentalis.voiceoscore.accessibility.utils

import android.os.SystemClock
import android.util.Log
import com.augmentalis.voiceos.constants.VoiceOSConstants
import java.util.concurrent.ConcurrentHashMap

/**
 * Debouncer utility to prevent excessive processing of rapid successive events
 *
 * @param cooldownMillis The minimum time interval between processing events with the same key
 */
class Debouncer(private val cooldownMillis: Long = VoiceOSConstants.Timing.THROTTLE_DELAY_MS) {

    companion object {
        private const val TAG = "Debouncer"
    }

    // Use ConcurrentHashMap for thread safety in coroutine environment
    private val lastExecutionMap: ConcurrentHashMap<String, Long> = ConcurrentHashMap()

    /**
     * Check if enough time has passed since the last execution for the given key
     *
     * @param key Unique identifier for the event/operation to debounce
     * @return true if the operation should proceed, false if it should be skipped
     */
    fun shouldProceed(key: String): Boolean {
        val now = SystemClock.uptimeMillis()
        val last = lastExecutionMap[key] ?: 0L

        return if (now - last >= cooldownMillis) {
            Log.d(TAG, "Event proceeding for key: $key (cooldown: ${cooldownMillis}ms)")
            lastExecutionMap[key] = now
            true
        } else {
            Log.v(TAG, "Event debounced for key: $key (${now - last}ms < ${cooldownMillis}ms)")
            false
        }
    }

    /**
     * Reset the debounce state for a specific key
     *
     * @param key The key to reset
     */
    fun reset(key: String) {
        lastExecutionMap.remove(key)
        Log.d(TAG, "Reset debounce state for key: $key")
    }

    /**
     * Clear all debounce states
     */
    fun clearAll() {
        lastExecutionMap.clear()
        Log.d(TAG, "Cleared all debounce states")
    }

    /**
     * Get the current size of the debounce map (for monitoring)
     */
    fun getMapSize(): Int = lastExecutionMap.size

    /**
     * Get performance metrics for monitoring
     */
    fun getMetrics(): Map<String, Any> {
        return mapOf(
            "cooldownMillis" to cooldownMillis,
            "activeKeys" to lastExecutionMap.size,
            "keys" to lastExecutionMap.keys.toList()
        )
    }
}