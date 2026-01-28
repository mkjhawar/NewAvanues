/**
 * IBatchManager.kt - Batch processing management interface for JIT processing
 *
 * Copyright (C) 2026 Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-16
 *
 * Defines the contract for managing batch processing of voice commands.
 * Supports configurable queue sizes, automatic flushing, and transaction support.
 */
package com.augmentalis.commandmanager

/**
 * Configuration for batch processing.
 *
 * @property maxBatchSize Maximum commands to queue before auto-flush (default: 100)
 * @property flushThresholdPercent Percentage of capacity that triggers warning (default: 80)
 * @property autoFlushEnabled Whether to auto-flush when queue is full (default: true)
 * @property retryOnFailure Whether to retry failed batch inserts (default: true)
 * @property maxRetries Maximum retry attempts for failed operations (default: 3)
 * @property retryDelayMs Delay between retries in milliseconds (default: 100)
 */
data class BatchConfig(
    val maxBatchSize: Int = 100,
    val flushThresholdPercent: Int = 80,
    val autoFlushEnabled: Boolean = true,
    val retryOnFailure: Boolean = true,
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 100L
) {
    /**
     * Calculate the flush threshold count.
     */
    val flushThresholdCount: Int
        get() = (maxBatchSize * flushThresholdPercent) / 100

    /**
     * Validate configuration values.
     */
    fun isValid(): Boolean {
        return maxBatchSize > 0 &&
               flushThresholdPercent in 1..100 &&
               maxRetries >= 0 &&
               retryDelayMs >= 0
    }

    companion object {
        /**
         * Default configuration for JIT mode (small batches, fast flush).
         */
        val JIT_DEFAULT = BatchConfig(
            maxBatchSize = 10,
            flushThresholdPercent = 50,
            autoFlushEnabled = true,
            retryOnFailure = true,
            maxRetries = 2,
            retryDelayMs = 50L
        )

        /**
         * Default configuration for exploration mode (large batches).
         */
        val EXPLORATION_DEFAULT = BatchConfig(
            maxBatchSize = 100,
            flushThresholdPercent = 80,
            autoFlushEnabled = true,
            retryOnFailure = true,
            maxRetries = 3,
            retryDelayMs = 100L
        )

        /**
         * High-throughput configuration for bulk operations.
         */
        val BULK_DEFAULT = BatchConfig(
            maxBatchSize = 500,
            flushThresholdPercent = 90,
            autoFlushEnabled = true,
            retryOnFailure = true,
            maxRetries = 5,
            retryDelayMs = 200L
        )
    }
}

/**
 * Statistics about batch processing performance.
 *
 * @property totalQueued Total commands queued since start
 * @property totalFlushed Total commands successfully flushed
 * @property totalFailed Total commands that failed to insert
 * @property totalRetries Total retry attempts made
 * @property currentQueueSize Current number of commands in queue
 * @property lastFlushTimeMs Time of last flush in milliseconds
 * @property lastFlushDurationMs Duration of last flush operation
 * @property avgFlushDurationMs Average flush duration
 * @property autoFlushCount Number of auto-flushes triggered
 */
data class BatchStats(
    val totalQueued: Long = 0L,
    val totalFlushed: Long = 0L,
    val totalFailed: Long = 0L,
    val totalRetries: Long = 0L,
    val currentQueueSize: Int = 0,
    val lastFlushTimeMs: Long = 0L,
    val lastFlushDurationMs: Long = 0L,
    val avgFlushDurationMs: Long = 0L,
    val autoFlushCount: Long = 0L
) {
    /**
     * Calculate success rate.
     */
    val successRate: Double
        get() = if (totalQueued > 0) {
            totalFlushed.toDouble() / totalQueued.toDouble()
        } else 1.0

    /**
     * Calculate commands per second based on average flush duration.
     */
    val commandsPerSecond: Double
        get() = if (avgFlushDurationMs > 0) {
            1000.0 / avgFlushDurationMs
        } else 0.0

    /**
     * Check if there are pending commands.
     */
    val hasPending: Boolean
        get() = currentQueueSize > 0
}

/**
 * Result of a batch flush operation.
 *
 * @property success Whether the flush completed successfully
 * @property commandsInserted Number of commands successfully inserted
 * @property commandsFailed Number of commands that failed
 * @property durationMs Time taken for the flush operation
 * @property error Error message if flush failed
 * @property wasAutoFlush Whether this was an automatic flush
 */
data class FlushResult(
    val success: Boolean,
    val commandsInserted: Int,
    val commandsFailed: Int,
    val durationMs: Long,
    val error: String? = null,
    val wasAutoFlush: Boolean = false
) {
    companion object {
        /**
         * Create a successful flush result.
         */
        fun success(
            inserted: Int,
            durationMs: Long,
            wasAutoFlush: Boolean = false
        ): FlushResult {
            return FlushResult(
                success = true,
                commandsInserted = inserted,
                commandsFailed = 0,
                durationMs = durationMs,
                wasAutoFlush = wasAutoFlush
            )
        }

        /**
         * Create a partial success result.
         */
        fun partial(
            inserted: Int,
            failed: Int,
            durationMs: Long,
            wasAutoFlush: Boolean = false
        ): FlushResult {
            return FlushResult(
                success = inserted > 0,
                commandsInserted = inserted,
                commandsFailed = failed,
                durationMs = durationMs,
                wasAutoFlush = wasAutoFlush
            )
        }

        /**
         * Create a failure result.
         */
        fun failure(
            error: String,
            commandCount: Int,
            durationMs: Long,
            wasAutoFlush: Boolean = false
        ): FlushResult {
            return FlushResult(
                success = false,
                commandsInserted = 0,
                commandsFailed = commandCount,
                durationMs = durationMs,
                error = error,
                wasAutoFlush = wasAutoFlush
            )
        }

        /**
         * Empty result (nothing to flush).
         */
        val EMPTY = FlushResult(
            success = true,
            commandsInserted = 0,
            commandsFailed = 0,
            durationMs = 0
        )
    }
}

/**
 * Listener for batch manager events.
 */
interface BatchManagerListener {
    /**
     * Called when a command is added to the queue.
     *
     * @param command The added command
     * @param queueSize Current queue size after adding
     */
    fun onCommandQueued(command: GeneratedCommand, queueSize: Int) {}

    /**
     * Called when the queue reaches the warning threshold.
     *
     * @param currentSize Current queue size
     * @param maxSize Maximum queue size
     */
    fun onQueueThresholdReached(currentSize: Int, maxSize: Int) {}

    /**
     * Called before a flush operation starts.
     *
     * @param commandCount Number of commands to be flushed
     * @param isAutoFlush Whether this is an automatic flush
     */
    fun onFlushStarted(commandCount: Int, isAutoFlush: Boolean) {}

    /**
     * Called after a flush operation completes.
     *
     * @param result The flush operation result
     */
    fun onFlushCompleted(result: FlushResult) {}

    /**
     * Called when a flush operation fails and will be retried.
     *
     * @param attempt Current retry attempt number
     * @param maxAttempts Maximum retry attempts
     * @param error The error that occurred
     */
    fun onFlushRetry(attempt: Int, maxAttempts: Int, error: String) {}
}

/**
 * Interface for managing batch processing of voice commands.
 *
 * Provides queue management, automatic flushing, and transaction support
 * for efficient database operations during element processing.
 *
 * ## Usage
 * ```kotlin
 * val batchManager: IBatchManager = AndroidBatchManager(repository, config)
 *
 * // Add commands to queue
 * batchManager.addCommand(command1)
 * batchManager.addCommand(command2)
 *
 * // Flush when ready (or let auto-flush handle it)
 * val result = batchManager.flush()
 * if (result.success) {
 *     println("Inserted ${result.commandsInserted} commands")
 * }
 * ```
 *
 * ## Performance
 * - IMMEDIATE mode: ~10ms per element (1 DB insert per element)
 * - BATCH mode: ~50ms for 100 elements (1 DB transaction for all)
 */
interface IBatchManager {

    /**
     * Get the current batch configuration.
     */
    val config: BatchConfig

    /**
     * Get current batch statistics.
     */
    val stats: BatchStats

    /**
     * Get current queue size.
     */
    val queueSize: Int

    /**
     * Check if queue is empty.
     */
    val isEmpty: Boolean

    /**
     * Check if queue is at or above flush threshold.
     */
    val isAtThreshold: Boolean

    /**
     * Check if queue is full.
     */
    val isFull: Boolean

    /**
     * Add a command to the batch queue.
     *
     * If the queue is full and auto-flush is enabled, triggers a flush.
     *
     * @param command The command to add
     * @return True if command was added (or auto-flushed), false if queue full and auto-flush disabled
     */
    fun addCommand(command: GeneratedCommand): Boolean

    /**
     * Add multiple commands to the batch queue.
     *
     * @param commands List of commands to add
     * @return Number of commands successfully added
     */
    fun addCommands(commands: List<GeneratedCommand>): Int {
        return commands.count { addCommand(it) }
    }

    /**
     * Flush all queued commands to the database.
     *
     * Uses a single database transaction for efficiency.
     *
     * @return FlushResult with operation details
     */
    suspend fun flush(): FlushResult

    /**
     * Clear the queue without flushing to database.
     *
     * Use with caution - commands will be lost.
     *
     * @return Number of commands cleared
     */
    fun clear(): Int

    /**
     * Get a copy of currently queued commands.
     *
     * Does not modify the queue.
     *
     * @return List of queued commands
     */
    fun getQueuedCommands(): List<GeneratedCommand>

    /**
     * Update batch configuration.
     *
     * @param newConfig The new configuration to use
     */
    fun updateConfig(newConfig: BatchConfig)

    /**
     * Reset statistics.
     */
    fun resetStats()

    /**
     * Add a listener for batch events.
     *
     * @param listener The listener to add
     */
    fun addListener(listener: BatchManagerListener)

    /**
     * Remove a listener.
     *
     * @param listener The listener to remove
     */
    fun removeListener(listener: BatchManagerListener)
}
