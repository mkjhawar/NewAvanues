package com.augmentalis.websocket

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Priority levels for queued operations
 */
@Serializable
enum class QueuePriority {
    LOW,      // Non-critical updates (history, analytics)
    NORMAL,   // Standard operations
    HIGH,     // User-initiated actions
    CRITICAL  // Settings, security-related
}

/**
 * Queued sync operation using AVU format
 */
@Serializable
data class QueuedOperation(
    val id: String,
    val avuMessage: String, // AVU format message string
    val entityType: String? = null,
    val entityId: String? = null,
    val priority: QueuePriority = QueuePriority.NORMAL,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val lastAttempt: Long? = null,
    val errorMessage: String? = null
) {
    val canRetry: Boolean get() = retryCount < maxRetries
}

/**
 * Queue statistics
 */
data class QueueStats(
    val totalCount: Int,
    val pendingCount: Int,
    val failedCount: Int,
    val byPriority: Map<QueuePriority, Int>,
    val oldestItemAgeMs: Long?
)

/**
 * In-memory sync queue for offline operations
 *
 * Stores operations that couldn't be sent due to network issues
 * and replays them when connectivity is restored.
 */
class SyncQueue {

    private val mutex = Mutex()
    private val queue = mutableListOf<QueuedOperation>()
    private val failedOperations = mutableListOf<QueuedOperation>()

    private val _queueSize = MutableStateFlow(0)
    val queueSize: Flow<Int> = _queueSize.asStateFlow()

    private val _status = MutableStateFlow(SyncStatus(
        lastSyncTimestamp = null,
        pendingEventCount = 0,
        syncState = SyncState.IDLE,
        errorMessage = null
    ))
    val status: Flow<SyncStatus> = _status.asStateFlow()

    /**
     * Enqueue an AVU format message
     */
    suspend fun enqueue(
        avuMessage: String,
        entityType: String? = null,
        entityId: String? = null,
        priority: QueuePriority = QueuePriority.NORMAL
    ): String {
        return mutex.withLock {
            val operation = QueuedOperation(
                id = generateOperationId(),
                avuMessage = avuMessage,
                entityType = entityType,
                entityId = entityId,
                priority = priority
            )

            // Insert based on priority (higher priority first)
            val insertIndex = queue.indexOfFirst { it.priority < priority }
            if (insertIndex >= 0) {
                queue.add(insertIndex, operation)
            } else {
                queue.add(operation)
            }

            updateState()
            operation.id
        }
    }

    /**
     * Dequeue next operation
     */
    suspend fun dequeue(): QueuedOperation? {
        return mutex.withLock {
            val operation = queue.removeFirstOrNull()
            updateState()
            operation
        }
    }

    /**
     * Peek at next operation without removing
     */
    suspend fun peek(): QueuedOperation? {
        return mutex.withLock {
            queue.firstOrNull()
        }
    }

    /**
     * Get pending operations (for batch processing)
     */
    suspend fun getPending(limit: Int = 50): List<QueuedOperation> {
        return mutex.withLock {
            queue.take(limit)
        }
    }

    /**
     * Mark operation as completed
     */
    suspend fun complete(operationId: String) {
        mutex.withLock {
            queue.removeAll { it.id == operationId }
            updateState()
        }
    }

    /**
     * Mark operation as failed
     */
    suspend fun fail(operationId: String, errorMessage: String) {
        mutex.withLock {
            val index = queue.indexOfFirst { it.id == operationId }
            if (index >= 0) {
                val operation = queue[index]
                val updated = operation.copy(
                    retryCount = operation.retryCount + 1,
                    lastAttempt = Clock.System.now().toEpochMilliseconds(),
                    errorMessage = errorMessage
                )

                if (updated.canRetry) {
                    // Re-queue at end of same priority
                    queue.removeAt(index)
                    val insertIndex = queue.indexOfFirst { it.priority < updated.priority }
                    if (insertIndex >= 0) {
                        queue.add(insertIndex, updated)
                    } else {
                        queue.add(updated)
                    }
                } else {
                    // Move to failed list
                    queue.removeAt(index)
                    failedOperations.add(updated)
                }

                updateState()
            }
        }
    }

    /**
     * Retry all failed operations
     */
    suspend fun retryFailed() {
        mutex.withLock {
            val toRetry = failedOperations.map { it.copy(retryCount = 0, errorMessage = null) }
            failedOperations.clear()

            toRetry.forEach { operation ->
                val insertIndex = queue.indexOfFirst { it.priority < operation.priority }
                if (insertIndex >= 0) {
                    queue.add(insertIndex, operation)
                } else {
                    queue.add(operation)
                }
            }

            updateState()
        }
    }

    /**
     * Clear all pending operations
     */
    suspend fun clear() {
        mutex.withLock {
            queue.clear()
            updateState()
        }
    }

    /**
     * Clear failed operations
     */
    suspend fun clearFailed() {
        mutex.withLock {
            failedOperations.clear()
        }
    }

    /**
     * Get queue statistics
     */
    suspend fun getStats(): QueueStats {
        return mutex.withLock {
            val now = Clock.System.now().toEpochMilliseconds()
            val oldestAge = queue.minOfOrNull { now - it.createdAt }

            QueueStats(
                totalCount = queue.size + failedOperations.size,
                pendingCount = queue.size,
                failedCount = failedOperations.size,
                byPriority = queue.groupingBy { it.priority }.eachCount(),
                oldestItemAgeMs = oldestAge
            )
        }
    }

    /**
     * Check if empty
     */
    suspend fun isEmpty(): Boolean = mutex.withLock { queue.isEmpty() }

    /**
     * Get operations by entity type
     */
    suspend fun getByEntityType(entityType: String): List<QueuedOperation> {
        return mutex.withLock {
            queue.filter { it.entityType == entityType }
        }
    }

    /**
     * Remove duplicate operations for same entity (keep newest)
     */
    suspend fun deduplicate() {
        mutex.withLock {
            val seen = mutableMapOf<String, Int>()
            val toRemove = mutableListOf<Int>()

            queue.forEachIndexed { index, operation ->
                val key = "${operation.entityType}:${operation.entityId}"
                if (key != "null:null") {
                    val previousIndex = seen[key]
                    if (previousIndex != null) {
                        toRemove.add(previousIndex)
                    }
                    seen[key] = index
                }
            }

            toRemove.sortedDescending().forEach { index ->
                queue.removeAt(index)
            }

            updateState()
        }
    }

    private fun updateState() {
        _queueSize.value = queue.size
        _status.value = _status.value.copy(pendingEventCount = queue.size)
    }

    private fun generateOperationId(): String {
        return "op_${Clock.System.now().toEpochMilliseconds()}_${(1000..9999).random()}"
    }
}

/**
 * Persistent sync queue interface (platform-specific storage)
 */
expect class PersistentSyncQueue() {
    suspend fun save(operations: List<QueuedOperation>)
    suspend fun load(): List<QueuedOperation>
    suspend fun clear()
}
