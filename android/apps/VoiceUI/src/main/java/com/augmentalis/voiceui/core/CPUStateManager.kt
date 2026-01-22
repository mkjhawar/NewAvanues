/**
 * CPUStateManager.kt - CPU-optimized state management fallback
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-27
 *
 * Provides CPU-based state caching and diffing for devices without
 * RenderEffect support (API < 31). Optimized for performance with
 * hash-based diffing and LRU cache eviction.
 */
package com.augmentalis.voiceui.core

import androidx.compose.runtime.Stable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * CPU-optimized state manager for API < 31 devices
 *
 * Features:
 * - Hash-based fast diffing
 * - LRU cache with configurable size
 * - Thread-safe operations
 * - Background thread optimization
 */
@Stable
class CPUStateManager(
    private val maxCacheSize: Int = DEFAULT_CACHE_SIZE
) {

    private val stateCache = ConcurrentHashMap<String, StateEntry>()
    private val accessOrder = LinkedHashMap<String, Long>(maxCacheSize, 0.75f, true)
    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * State entry optimized for CPU operations
     */
    data class StateEntry(
        val value: Any,
        val hash: Int,
        val timestamp: Long,
        val accessCount: Int = 0
    )

    /**
     * Cache state with LRU eviction
     */
    suspend fun cacheState(key: String, value: Any): Boolean {
        val newHash = value.hashCode()
        val existing = stateCache[key]

        // Fast path: no change detected
        if (existing?.hash == newHash) {
            updateAccessOrder(key)
            return false
        }

        // Evict if at capacity
        if (stateCache.size >= maxCacheSize && !stateCache.containsKey(key)) {
            evictLRU()
        }

        // Update cache
        stateCache[key] = StateEntry(
            value = value,
            hash = newHash,
            timestamp = System.currentTimeMillis(),
            accessCount = (existing?.accessCount ?: 0) + 1
        )

        updateAccessOrder(key)
        return true
    }

    /**
     * Synchronous cache state (for non-suspend contexts)
     */
    fun cacheStateSync(key: String, value: Any): Boolean {
        val newHash = value.hashCode()
        val existing = stateCache[key]

        if (existing?.hash == newHash) {
            return false
        }

        // Simple eviction without coroutine
        if (stateCache.size >= maxCacheSize && !stateCache.containsKey(key)) {
            evictOldest()
        }

        stateCache[key] = StateEntry(
            value = value,
            hash = newHash,
            timestamp = System.currentTimeMillis(),
            accessCount = (existing?.accessCount ?: 0) + 1
        )

        return true
    }

    /**
     * Perform CPU-optimized state diff
     */
    suspend fun diffState(key: String, newValue: Any): StateDiffResult {
        val oldEntry = stateCache[key]
        val newHash = newValue.hashCode()

        return when {
            oldEntry == null -> {
                cacheState(key, newValue)
                StateDiffResult(
                    changed = true,
                    isNew = true,
                    diffTimeNanos = 0
                )
            }
            oldEntry.hash != newHash -> {
                val startTime = System.nanoTime()
                cacheState(key, newValue)
                StateDiffResult(
                    changed = true,
                    isNew = false,
                    previousHash = oldEntry.hash,
                    diffTimeNanos = System.nanoTime() - startTime
                )
            }
            else -> {
                updateAccessOrder(key)
                StateDiffResult(
                    changed = false,
                    isNew = false,
                    diffTimeNanos = 0
                )
            }
        }
    }

    /**
     * Synchronous diff for non-suspend contexts
     */
    fun diffStateSync(key: String, newValue: Any): StateDiffResult {
        val oldEntry = stateCache[key]
        val newHash = newValue.hashCode()

        return when {
            oldEntry == null -> {
                cacheStateSync(key, newValue)
                StateDiffResult(changed = true, isNew = true, diffTimeNanos = 0)
            }
            oldEntry.hash != newHash -> {
                val startTime = System.nanoTime()
                cacheStateSync(key, newValue)
                StateDiffResult(
                    changed = true,
                    isNew = false,
                    previousHash = oldEntry.hash,
                    diffTimeNanos = System.nanoTime() - startTime
                )
            }
            else -> {
                StateDiffResult(changed = false, isNew = false, diffTimeNanos = 0)
            }
        }
    }

    /**
     * Update access order for LRU tracking
     */
    private suspend fun updateAccessOrder(key: String) {
        mutex.withLock {
            accessOrder[key] = System.currentTimeMillis()
        }
    }

    /**
     * Evict least recently used entry
     */
    private suspend fun evictLRU() {
        mutex.withLock {
            val oldest = accessOrder.entries.firstOrNull()?.key
            if (oldest != null) {
                stateCache.remove(oldest)
                accessOrder.remove(oldest)
            }
        }
    }

    /**
     * Simple eviction for sync context
     */
    private fun evictOldest() {
        val oldest = stateCache.entries.minByOrNull { it.value.timestamp }?.key
        if (oldest != null) {
            stateCache.remove(oldest)
        }
    }

    /**
     * Get cached state by key
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getCachedState(key: String): T? {
        return stateCache[key]?.value as? T
    }

    /**
     * Clear state cache
     */
    fun clearCache() {
        stateCache.clear()
        scope.launch {
            mutex.withLock {
                accessOrder.clear()
            }
        }
    }

    /**
     * Get cache statistics
     */
    fun getCacheStats(): CacheStats {
        val entries = stateCache.values.toList()
        return CacheStats(
            size = entries.size,
            maxSize = maxCacheSize,
            hitRate = calculateHitRate(entries),
            avgAccessCount = entries.map { it.accessCount }.average().takeIf { !it.isNaN() } ?: 0.0,
            oldestEntry = entries.minOfOrNull { it.timestamp } ?: 0L,
            newestEntry = entries.maxOfOrNull { it.timestamp } ?: 0L
        )
    }

    /**
     * Calculate cache hit rate based on access counts
     */
    private fun calculateHitRate(entries: List<StateEntry>): Float {
        if (entries.isEmpty()) return 0f
        val totalAccess = entries.sumOf { it.accessCount }
        val uniqueEntries = entries.size
        return if (totalAccess > uniqueEntries) {
            (totalAccess - uniqueEntries).toFloat() / totalAccess
        } else {
            0f
        }
    }

    /**
     * Async state update with callback
     */
    fun updateStateAsync(
        key: String,
        value: Any,
        onComplete: (StateDiffResult) -> Unit
    ) {
        scope.launch {
            val result = diffState(key, value)
            onComplete(result)
        }
    }

    /**
     * Result of state diff operation
     */
    data class StateDiffResult(
        val changed: Boolean,
        val isNew: Boolean,
        val previousHash: Int? = null,
        val diffTimeNanos: Long = 0
    )

    /**
     * Cache statistics for monitoring
     */
    data class CacheStats(
        val size: Int,
        val maxSize: Int,
        val hitRate: Float,
        val avgAccessCount: Double,
        val oldestEntry: Long,
        val newestEntry: Long
    )

    companion object {
        const val DEFAULT_CACHE_SIZE = 500
        const val SMALL_CACHE_SIZE = 100
        const val LARGE_CACHE_SIZE = 1000
    }
}
