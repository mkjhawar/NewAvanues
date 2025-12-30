/**
 * KV Cache Memory Manager
 *
 * Single Responsibility: Manage KV cache and memory budgets
 *
 * Tracks memory usage, enforces budgets, and manages KV cache lifecycle
 * for attention mechanisms in transformer models.
 *
 * Created: 2025-10-31
 */

package com.augmentalis.llm.alc.memory

import com.augmentalis.llm.alc.interfaces.IMemoryManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.atomic.AtomicLong

/**
 * Memory manager for KV cache
 *
 * @param memoryBudgetBytes Maximum allowed memory usage in bytes (default 512 MB)
 */
class KVCacheMemoryManager(
    private val memoryBudgetBytes: Long = 512 * 1024 * 1024 // 512 MB default
) : IMemoryManager {

    private val currentUsage = AtomicLong(0)
    private val mutex = Mutex()
    private var kvCache: Any? = null
    private var cacheHits = 0L
    private var cacheMisses = 0L

    override fun checkMemoryAvailable(requiredBytes: Long): Boolean {
        val available = memoryBudgetBytes - currentUsage.get()
        return available >= requiredBytes
    }

    override fun getCurrentMemoryUsage(): Long {
        return currentUsage.get()
    }

    override fun getMemoryBudget(): Long {
        return memoryBudgetBytes
    }

    override suspend fun resetCache(): Unit = mutex.withLock {
        Timber.d("Resetting KV cache")
        kvCache = null
        cacheMisses++
        // Note: Don't reset currentUsage here - that's tracked separately
    }

    override suspend fun optimizeMemory(): Long = mutex.withLock {
        val before = currentUsage.get()

        // Clear cache if it exists
        if (kvCache != null) {
            kvCache = null
            Timber.d("Cleared KV cache during optimization")
        }

        // Suggest garbage collection (not guaranteed)
        System.gc()

        val after = currentUsage.get()
        val freed = before - after

        Timber.i("Memory optimization freed $freed bytes")
        return@withLock freed
    }

    override fun getCacheStats(): Map<String, Any> {
        return mapOf(
            "cache_hits" to cacheHits,
            "cache_misses" to cacheMisses,
            "cache_hit_rate" to if (cacheHits + cacheMisses > 0) {
                cacheHits.toFloat() / (cacheHits + cacheMisses)
            } else 0f,
            "current_usage_bytes" to currentUsage.get(),
            "budget_bytes" to memoryBudgetBytes,
            "utilization_pct" to (currentUsage.get().toFloat() / memoryBudgetBytes * 100)
        )
    }

    /**
     * Update memory usage tracking
     *
     * @param deltaBytes Change in memory usage (positive = allocated, negative = freed)
     */
    fun updateMemoryUsage(deltaBytes: Long) {
        currentUsage.addAndGet(deltaBytes)
    }

    /**
     * Get the current KV cache
     */
    suspend fun getCache(): Any? = mutex.withLock {
        if (kvCache != null) {
            cacheHits++
        }
        return@withLock kvCache
    }

    /**
     * Set the KV cache
     */
    suspend fun setCache(cache: Any?) = mutex.withLock {
        kvCache = cache
    }
}
