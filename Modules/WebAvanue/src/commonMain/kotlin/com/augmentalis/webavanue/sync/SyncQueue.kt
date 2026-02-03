package com.augmentalis.webavanue.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Priority levels for sync operations
 */
@Serializable
enum class SyncPriority {
    LOW,      // History, non-critical updates
    NORMAL,   // Standard operations
    HIGH,     // User-initiated actions
    CRITICAL  // Settings, security-related
}

/**
 * Represents a queued sync operation
 */
@Serializable
data class QueuedSyncOperation(
    val id: String,
    val event: RemoteUpdateEvent,
    val priority: SyncPriority,
    val createdAt: Instant,
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val lastAttempt: Instant? = null,
    val errorMessage: String? = null
) {
    val canRetry: Boolean get() = retryCount < maxRetries
}

/**
 * Queue statistics for monitoring
 */
data class QueueStats(
    val totalCount: Int,
    val pendingCount: Int,
    val failedCount: Int,
    val byPriority: Map<SyncPriority, Int>,
    val oldestItemAge: Long? // milliseconds
)

/**
 * Offline-capable sync queue for storing pending operations
 *
 * This queue persists operations that couldn't be sent due to network issues
 * and replays them when connectivity is restored.
 */
class SyncQueue {

    private val mutex = Mutex()
    private val queue = mutableListOf<QueuedSyncOperation>()
    private val failedOperations = mutableListOf<QueuedSyncOperation>()

    private val _queueSize = MutableStateFlow(0)
    val queueSize: Flow<Int> = _queueSize.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus(
        lastSyncTimestamp = null,
        pendingEventCount = 0,
        syncState = SyncState.IDLE,
        errorMessage = null
    ))
    val syncStatus: Flow<SyncStatus> = _syncStatus.asStateFlow()

    /**
     * Enqueue a sync operation
     */
    suspend fun enqueue(
        event: RemoteUpdateEvent,
        priority: SyncPriority = SyncPriority.NORMAL
    ): String {
        return mutex.withLock {
            val operation = QueuedSyncOperation(
                id = generateOperationId(),
                event = event,
                priority = priority,
                createdAt = Clock.System.now()
            )

            // Insert based on priority
            val insertIndex = queue.indexOfFirst { it.priority < priority }
            if (insertIndex >= 0) {
                queue.add(insertIndex, operation)
            } else {
                queue.add(operation)
            }

            updateQueueSize()
            operation.id
        }
    }

    /**
     * Dequeue the next operation to process
     */
    suspend fun dequeue(): QueuedSyncOperation? {
        return mutex.withLock {
            val operation = queue.removeFirstOrNull()
            updateQueueSize()
            operation
        }
    }

    /**
     * Peek at the next operation without removing it
     */
    suspend fun peek(): QueuedSyncOperation? {
        return mutex.withLock {
            queue.firstOrNull()
        }
    }

    /**
     * Get all pending operations (for batch processing)
     */
    suspend fun getPending(limit: Int = 50): List<QueuedSyncOperation> {
        return mutex.withLock {
            queue.take(limit)
        }
    }

    /**
     * Mark an operation as completed and remove it
     */
    suspend fun complete(operationId: String) {
        mutex.withLock {
            queue.removeAll { it.id == operationId }
            updateQueueSize()
            updateSyncStatus(SyncState.IDLE)
        }
    }

    /**
     * Mark an operation as failed
     */
    suspend fun fail(operationId: String, errorMessage: String) {
        mutex.withLock {
            val index = queue.indexOfFirst { it.id == operationId }
            if (index >= 0) {
                val operation = queue[index]
                val updated = operation.copy(
                    retryCount = operation.retryCount + 1,
                    lastAttempt = Clock.System.now(),
                    errorMessage = errorMessage
                )

                if (updated.canRetry) {
                    // Move to end of same-priority section for retry
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

                updateQueueSize()
            }
        }
    }

    /**
     * Retry all failed operations
     */
    suspend fun retryFailed() {
        mutex.withLock {
            val toRetry = failedOperations.map { it.copy(retryCount = 0) }
            failedOperations.clear()

            toRetry.forEach { operation ->
                val insertIndex = queue.indexOfFirst { it.priority < operation.priority }
                if (insertIndex >= 0) {
                    queue.add(insertIndex, operation)
                } else {
                    queue.add(operation)
                }
            }

            updateQueueSize()
        }
    }

    /**
     * Clear the queue
     */
    suspend fun clear() {
        mutex.withLock {
            queue.clear()
            updateQueueSize()
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
            val now = Clock.System.now()
            val oldestAge = queue.minOfOrNull {
                (now - it.createdAt).inWholeMilliseconds
            }

            QueueStats(
                totalCount = queue.size + failedOperations.size,
                pendingCount = queue.size,
                failedCount = failedOperations.size,
                byPriority = queue.groupingBy { it.priority }.eachCount(),
                oldestItemAge = oldestAge
            )
        }
    }

    /**
     * Check if queue is empty
     */
    suspend fun isEmpty(): Boolean {
        return mutex.withLock {
            queue.isEmpty()
        }
    }

    /**
     * Get operations by entity type
     */
    suspend fun getByEntityType(entityType: SyncEntityType): List<QueuedSyncOperation> {
        return mutex.withLock {
            queue.filter { operation ->
                when (operation.event) {
                    is RemoteTabUpdate -> entityType == SyncEntityType.TAB
                    is RemoteFavoriteUpdate -> entityType == SyncEntityType.FAVORITE
                    is RemoteHistoryUpdate -> entityType == SyncEntityType.HISTORY
                    is RemoteDownloadUpdate -> entityType == SyncEntityType.DOWNLOAD
                    is RemoteSettingsUpdate -> entityType == SyncEntityType.SETTINGS
                    is RemoteSessionUpdate -> entityType == SyncEntityType.SESSION
                }
            }
        }
    }

    /**
     * Remove duplicate operations for the same entity
     * Keeps only the most recent operation
     */
    suspend fun deduplicate() {
        mutex.withLock {
            val seen = mutableMapOf<String, Int>()
            val toRemove = mutableListOf<Int>()

            queue.forEachIndexed { index, operation ->
                val entityKey = getEntityKey(operation.event)
                if (entityKey != null) {
                    val previousIndex = seen[entityKey]
                    if (previousIndex != null) {
                        // Keep the newer one (current), mark older for removal
                        toRemove.add(previousIndex)
                    }
                    seen[entityKey] = index
                }
            }

            // Remove in reverse order to maintain indices
            toRemove.sortedDescending().forEach { index ->
                queue.removeAt(index)
            }

            updateQueueSize()
        }
    }

    // ==================== Private Helpers ====================

    private fun updateQueueSize() {
        _queueSize.value = queue.size
    }

    private fun updateSyncStatus(state: SyncState, error: String? = null) {
        _syncStatus.value = SyncStatus(
            lastSyncTimestamp = if (state == SyncState.IDLE) Clock.System.now() else _syncStatus.value.lastSyncTimestamp,
            pendingEventCount = queue.size,
            syncState = state,
            errorMessage = error
        )
    }

    private fun generateOperationId(): String {
        return "op_${Clock.System.now().toEpochMilliseconds()}_${(1000..9999).random()}"
    }

    private fun getEntityKey(event: RemoteUpdateEvent): String? {
        return when (event) {
            is RemoteTabUpdate -> event.tabId ?: event.tab?.id
            is RemoteFavoriteUpdate -> event.favoriteId ?: event.favorite?.id
            is RemoteHistoryUpdate -> event.entryId ?: event.historyEntry?.id
            is RemoteDownloadUpdate -> event.downloadId ?: event.download?.id
            is RemoteSettingsUpdate -> "settings" // Singleton
            is RemoteSessionUpdate -> event.sessionId ?: event.session?.id
        }
    }
}

/**
 * Persistent sync queue that survives app restarts
 * Uses platform-specific storage (SharedPreferences on Android, UserDefaults on iOS)
 */
expect class PersistentSyncQueue() {
    suspend fun save(queue: List<QueuedSyncOperation>)
    suspend fun load(): List<QueuedSyncOperation>
    suspend fun clear()
}
