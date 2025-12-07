/**
 * AsyncQueryManager.kt - Non-blocking Database Query Manager with Caching
 *
 * YOLO Phase 1 - Critical Issue #1: runBlocking on UI Thread - SOLUTION
 *
 * Problem Solved:
 * - Eliminates runBlocking calls on UI thread
 * - Prevents ANR (Application Not Responding) during database queries
 * - Provides proper async/await pattern for accessibility event handlers
 *
 * Features:
 * - All queries run on background thread (Dispatchers.IO)
 * - LRU cache to reduce database load
 * - Concurrent query deduplication
 * - Exception-safe with proper cleanup
 * - Lifecycle-aware (AutoCloseable)
 * - Thread-safe operations
 *
 * Usage:
 * ```kotlin
 * AsyncQueryManager().use { manager ->
 *     val element = manager.query("element:$hash") {
 *         database.scrapedElementDao().getElementByHash(hash)
 *     }
 *     // Use element (query ran on background thread, result cached)
 * }
 * ```
 *
 * @property maxCacheSize Maximum number of cached query results (default 200)
 */
package com.augmentalis.voiceoscore.lifecycle

import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * AsyncQueryManager - Non-blocking query execution with LRU caching
 *
 * Replaces dangerous runBlocking pattern with proper coroutine suspension.
 * All queries execute on IO dispatcher to prevent UI thread blocking.
 */
class AsyncQueryManager(
    private val maxCacheSize: Int = DEFAULT_MAX_CACHE_SIZE
) : AutoCloseable {

    companion object {
        private const val TAG = "AsyncQueryManager"
        private const val DEFAULT_MAX_CACHE_SIZE = 200

        /**
         * Sentinel value to distinguish "null result" from "no cache entry"
         */
        private val NULL_RESULT = Any()
    }

    /**
     * Coroutine scope for query execution
     * Uses IO dispatcher for background execution
     */
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * LRU cache for query results
     * Thread-safe with synchronized access
     */
    private val cache = object : LinkedHashMap<String, Any?>(
        maxCacheSize,
        0.75f,
        true  // Access-order (LRU)
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Any?>?): Boolean {
            return size > maxCacheSize
        }
    }

    /**
     * In-flight query deferred results
     * Prevents duplicate execution of concurrent queries with same key
     */
    private val inFlightQueries = ConcurrentHashMap<String, Deferred<Any?>>()

    /**
     * Track if manager is closed
     */
    @Volatile
    private var closed = false

    /**
     * Execute a query with caching
     *
     * @param key Unique cache key for this query
     * @param queryFunction Suspend function that executes the query
     * @return Query result (from cache or fresh execution)
     * @throws IllegalStateException if manager is closed
     */
    suspend fun <T> query(key: String, queryFunction: suspend () -> T): T {
        if (closed) {
            throw IllegalStateException(
                "Cannot execute query on closed AsyncQueryManager. " +
                "The manager has been shut down or the associated lifecycle owner has reached DESTROYED state. " +
                "Ensure queries are executed before close() is called or create a new AsyncQueryManager instance."
            )
        }

        // Check cache first (synchronized for thread safety)
        val cachedResult = synchronized(cache) {
            cache[key]
        }

        if (cachedResult != null) {
            // Cache hit
            @Suppress("UNCHECKED_CAST")
            return if (cachedResult === NULL_RESULT) {
                null as T
            } else {
                cachedResult as T
            }
        }

        // Cache miss - execute query or join in-flight query
        return executeQuery(key, queryFunction)
    }

    /**
     * Execute query on background thread with deduplication
     *
     * If another coroutine is already executing the same query,
     * this will wait for that result instead of duplicating work.
     */
    private suspend fun <T> executeQuery(key: String, queryFunction: suspend () -> T): T {
        // Check if query already in-flight
        val existingDeferred = inFlightQueries[key]
        if (existingDeferred != null && existingDeferred.isActive) {
            // Wait for existing query to complete
            @Suppress("UNCHECKED_CAST")
            val result = existingDeferred.await()
            return if (result === NULL_RESULT) null as T else result as T
        }

        // Create new deferred for this query
        val deferred = scope.async {
            try {
                val result = queryFunction()

                // Cache the result (use sentinel for null)
                val cacheValue = result ?: NULL_RESULT
                synchronized(cache) {
                    cache[key] = cacheValue
                }

                cacheValue
            } catch (e: Exception) {
                // Don't cache failures - let exception propagate
                throw e
            } finally {
                // Remove from in-flight map
                inFlightQueries.remove(key)
            }
        }

        // Store in-flight query
        inFlightQueries[key] = deferred

        // Wait for result
        @Suppress("UNCHECKED_CAST")
        val result = deferred.await()
        return if (result === NULL_RESULT) null as T else result as T
    }

    /**
     * Invalidate a specific cache entry
     *
     * Next query with this key will re-execute the query function.
     *
     * @param key Cache key to invalidate
     */
    fun invalidate(key: String) {
        synchronized(cache) {
            cache.remove(key)
        }
    }

    /**
     * Clear all cached entries
     *
     * All subsequent queries will re-execute their query functions.
     */
    fun clear() {
        synchronized(cache) {
            cache.clear()
        }
    }

    /**
     * Get current cache size
     *
     * Useful for testing and monitoring.
     */
    fun getCacheSize(): Int {
        return synchronized(cache) {
            cache.size
        }
    }

    /**
     * Close the manager and cancel all in-flight queries
     *
     * After close(), all query() calls will throw IllegalStateException.
     * Safe to call multiple times (idempotent).
     */
    override fun close() {
        if (closed) return
        closed = true

        // Cancel all in-flight queries
        inFlightQueries.values.forEach { deferred ->
            deferred.cancel()
        }
        inFlightQueries.clear()

        // Cancel scope
        scope.cancel()

        // Clear cache
        synchronized(cache) {
            cache.clear()
        }

        Log.d(TAG, "AsyncQueryManager closed")
    }
}

/**
 * Extension function for convenient query execution
 *
 * Automatically creates and closes AsyncQueryManager for single query.
 *
 * Usage:
 * ```kotlin
 * val element = asyncQuery("element:$hash") {
 *     database.scrapedElementDao().getElementByHash(hash)
 * }
 * ```
 */
suspend fun <T> asyncQuery(key: String, queryFunction: suspend () -> T): T {
    return AsyncQueryManager().use { manager ->
        manager.query(key, queryFunction)
    }
}

/**
 * Extension function to use AsyncQueryManager with automatic cleanup
 *
 * Mimics the standard use{} pattern but properly handles suspend functions.
 */
suspend inline fun <T> AsyncQueryManager.use(block: (AsyncQueryManager) -> T): T {
    return try {
        block(this)
    } finally {
        close()
    }
}
