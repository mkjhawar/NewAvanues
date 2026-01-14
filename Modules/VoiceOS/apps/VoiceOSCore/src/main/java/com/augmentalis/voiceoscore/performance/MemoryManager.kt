/**
 * MemoryManager.kt - Memory profiling and leak detection
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Sprint 4 Performance Infrastructure
 * Created: 2025-12-23
 *
 * Provides memory profiling, leak detection, cache management, and GC monitoring.
 */

package com.augmentalis.voiceoscore.performance

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

/**
 * Manages memory monitoring, leak detection, and cache eviction.
 *
 * Features:
 * - Heap size and allocation rate tracking
 * - Memory leak detection via weak references
 * - LRU cache management
 * - GC pause time monitoring
 */
class MemoryManager {

    companion object {
        private const val TAG = "MemoryManager"
        private const val MAX_CACHE_SIZE_MB = 50
        private const val LEAK_CHECK_INTERVAL_MS = 30000L // 30 seconds
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Memory metrics
    data class MemoryMetrics(
        val heapSizeMB: Long,
        val heapUsedMB: Long,
        val heapFreeMB: Long,
        val allocationRateMBPerSec: Double,
        val gcCount: Int,
        val gcPauseTimeMs: Long
    )

    // Tracked objects for leak detection
    private val trackedObjects = ConcurrentHashMap<String, WeakReference<Any>>()

    // LRU cache simulation
    private val cache = object : LinkedHashMap<String, ByteArray>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ByteArray>?): Boolean {
            val currentSizeMB = values.sumOf { it.size } / (1024 * 1024)
            return currentSizeMB > MAX_CACHE_SIZE_MB
        }
    }

    // GC metrics
    private var lastGcCount = 0
    private var lastGcTime = System.currentTimeMillis()

    /**
     * Start memory profiling
     */
    fun startProfiling() {
        Log.d(TAG, "Starting memory profiling")

        scope.launch {
            while (true) {
                try {
                    kotlinx.coroutines.delay(LEAK_CHECK_INTERVAL_MS)
                    performLeakCheck()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in leak check", e)
                }
            }
        }
    }

    /**
     * Stop memory profiling
     */
    fun stopProfiling() {
        Log.d(TAG, "Stopping memory profiling")
        scope.cancel()
    }

    /**
     * Get current memory metrics
     */
    fun getMemoryMetrics(): MemoryMetrics {
        val runtime = Runtime.getRuntime()
        val heapSize = runtime.totalMemory() / (1024 * 1024)
        val heapFree = runtime.freeMemory() / (1024 * 1024)
        val heapUsed = heapSize - heapFree

        // Calculate allocation rate (simplified)
        val currentTime = System.currentTimeMillis()
        val timeDeltaSec = max(1, (currentTime - lastGcTime)) / 1000.0
        val allocationRate = heapUsed / timeDeltaSec

        return MemoryMetrics(
            heapSizeMB = heapSize,
            heapUsedMB = heapUsed,
            heapFreeMB = heapFree,
            allocationRateMBPerSec = allocationRate,
            gcCount = getGcCount(),
            gcPauseTimeMs = estimateGcPauseTime()
        )
    }

    /**
     * Track object for leak detection
     */
    fun track(weakRef: WeakReference<Any>, tag: String = "unknown") {
        trackedObjects[tag] = weakRef
        Log.d(TAG, "Tracking object: $tag (total tracked: ${trackedObjects.size})")
    }

    /**
     * Track object by creating weak reference
     */
    fun trackObject(obj: Any, tag: String = "unknown") {
        track(WeakReference(obj), tag)
    }

    /**
     * Get list of tracked objects (for testing)
     */
    fun getTrackedObjects(): List<String> {
        return trackedObjects.keys.toList()
    }

    /**
     * Perform leak check on tracked objects
     */
    private fun performLeakCheck() {
        Log.d(TAG, "Performing leak check on ${trackedObjects.size} tracked objects")

        val leaks = mutableListOf<String>()
        val iterator = trackedObjects.iterator()

        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.get() == null) {
                // Object has been collected (no leak)
                iterator.remove()
                Log.d(TAG, "Object collected (no leak): ${entry.key}")
            } else {
                // Object still referenced (potential leak)
                leaks.add(entry.key)
            }
        }

        if (leaks.isNotEmpty()) {
            Log.w(TAG, "Potential memory leaks detected: $leaks")
        }
    }

    /**
     * Add item to cache
     */
    fun cacheItem(key: String, data: ByteArray) {
        synchronized(cache) {
            cache[key] = data
        }
    }

    /**
     * Get item from cache
     */
    fun getCachedItem(key: String): ByteArray? {
        synchronized(cache) {
            return cache[key]
        }
    }

    /**
     * Get cache size in MB
     */
    fun getCacheSizeMB(): Long {
        synchronized(cache) {
            return cache.values.sumOf { it.size }.toLong() / (1024 * 1024)
        }
    }

    /**
     * Clear cache
     */
    fun clearCache() {
        synchronized(cache) {
            cache.clear()
        }
        Log.d(TAG, "Cache cleared")
    }

    /**
     * Get GC count (approximation)
     */
    private fun getGcCount(): Int {
        // In production, would use Debug.getRuntimeStats() or similar
        // For now, increment on each check
        return ++lastGcCount
    }

    /**
     * Estimate GC pause time
     */
    private fun estimateGcPauseTime(): Long {
        // In production, would measure actual pause time
        // For now, return estimate based on heap usage
        val metrics = Runtime.getRuntime()
        val usagePercent = (metrics.totalMemory() - metrics.freeMemory()) * 100 / metrics.totalMemory()

        return when {
            usagePercent > 90 -> 100 // High usage = longer pause
            usagePercent > 70 -> 50
            else -> 10
        }
    }

    /**
     * Force garbage collection (for testing only)
     */
    fun forceGc() {
        System.gc()
        System.runFinalization()
        Log.d(TAG, "Forced GC")
    }

    /**
     * Cleanup and release resources
     */
    fun cleanup() {
        stopProfiling()
        trackedObjects.clear()
        clearCache()
        Log.d(TAG, "MemoryManager cleaned up")
    }
}
