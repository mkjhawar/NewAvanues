/**
 * Memory Manager Interface
 *
 * Single Responsibility: Manage memory and KV cache
 *
 * Implementations:
 * - KVCacheMemoryManager: Manage KV cache for attention
 * - BudgetMemoryManager: Enforce memory budgets
 * - MockMemoryManager: For testing
 *
 * Created: 2025-10-31
 */

package com.augmentalis.llm.alc.interfaces

/**
 * Interface for managing memory and KV cache
 */
interface IMemoryManager {
    /**
     * Check if enough memory is available
     *
     * @param requiredBytes Bytes required for operation
     * @return true if memory is available
     */
    fun checkMemoryAvailable(requiredBytes: Long): Boolean

    /**
     * Get current memory usage
     *
     * @return Current memory usage in bytes
     */
    fun getCurrentMemoryUsage(): Long

    /**
     * Get memory budget limit
     *
     * @return Maximum allowed memory in bytes
     */
    fun getMemoryBudget(): Long

    /**
     * Reset KV cache (clear conversation context)
     */
    suspend fun resetCache()

    /**
     * Optimize memory (e.g., trim cache, garbage collect)
     *
     * @return Bytes freed
     */
    suspend fun optimizeMemory(): Long

    /**
     * Get cache statistics
     *
     * @return Map of cache metrics (size, hits, misses, etc.)
     */
    fun getCacheStats(): Map<String, Any>
}
