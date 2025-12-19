package com.augmentalis.magicelements.core.mel

import kotlinx.serialization.json.JsonElement
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Represents a state change event.
 *
 * @property path The path that changed (e.g., "count", "config.theme")
 * @property oldValue The previous value at this path
 * @property newValue The new value at this path
 * @property timestamp When the change occurred (milliseconds since epoch)
 */
data class StateChange(
    val path: String,
    val oldValue: JsonElement,
    val newValue: JsonElement,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Checks if this change actually modified the value.
     */
    fun isActualChange(): Boolean = oldValue != newValue
}

/**
 * Batch of multiple state changes applied atomically.
 *
 * @property changes List of individual changes
 * @property source Optional identifier for the source of these changes
 */
data class StateChangeBatch(
    val changes: List<StateChange>,
    val source: String? = null
) {
    /**
     * Gets only the changes that actually modified values.
     */
    fun actualChanges(): List<StateChange> = changes.filter { it.isActualChange() }

    /**
     * Gets all unique paths that were affected.
     */
    fun affectedPaths(): Set<String> = changes.map { it.path }.toSet()
}

/**
 * Observer pattern implementation for state changes.
 *
 * Provides both callback-based and Flow-based observation of state changes,
 * with support for path-specific subscriptions and batch updates.
 *
 * Thread-safe and supports multiple concurrent observers.
 */
class StateObserver {
    /**
     * Internal state for managing observers.
     */
    private val lock = Any()

    /**
     * Map of observer IDs to their callbacks and subscribed paths.
     */
    private val observers = mutableMapOf<String, ObserverEntry>()

    /**
     * Shared flow for reactive state change observation.
     */
    private val _changeFlow = MutableSharedFlow<StateChangeBatch>(
        replay = 0,
        extraBufferCapacity = 100
    )

    /**
     * Public flow for observing state changes reactively.
     */
    val changeFlow: SharedFlow<StateChangeBatch> = _changeFlow.asSharedFlow()

    /**
     * Counter for generating unique observer IDs.
     */
    private var nextObserverId = 0

    /**
     * Flag to enable/disable batch mode.
     */
    private var batchMode = false

    /**
     * Pending changes in current batch.
     */
    private val pendingBatch = mutableListOf<StateChange>()

    /**
     * Internal representation of an observer.
     */
    private data class ObserverEntry(
        val id: String,
        val paths: Set<String>?,  // null means observe all paths
        val callback: (StateChangeBatch) -> Unit
    )

    /**
     * Subscribes to state changes with a callback.
     *
     * @param paths Optional list of specific paths to observe. If null, observes all changes.
     * @param callback Function to call when observed paths change
     * @return Observer ID that can be used to unsubscribe
     */
    fun subscribe(
        paths: List<String>? = null,
        callback: (StateChangeBatch) -> Unit
    ): String = synchronized(lock) {
        val observerId = "observer_${nextObserverId++}"
        val entry = ObserverEntry(
            id = observerId,
            paths = paths?.toSet(),
            callback = callback
        )
        observers[observerId] = entry
        return observerId
    }

    /**
     * Subscribes to changes on a single path (convenience method).
     *
     * @param path The path to observe
     * @param callback Function to call when this path changes
     * @return Observer ID that can be used to unsubscribe
     */
    fun subscribe(
        path: String,
        callback: (StateChange) -> Unit
    ): String {
        return subscribe(listOf(path)) { batch ->
            batch.changes
                .filter { it.path == path }
                .forEach(callback)
        }
    }

    /**
     * Unsubscribes an observer.
     *
     * @param observerId The ID returned from subscribe()
     * @return true if observer was found and removed, false otherwise
     */
    fun unsubscribe(observerId: String): Boolean = synchronized(lock) {
        observers.remove(observerId) != null
    }

    /**
     * Unsubscribes all observers.
     */
    fun unsubscribeAll() = synchronized(lock) {
        observers.clear()
    }

    /**
     * Gets the number of active observers.
     */
    fun observerCount(): Int = synchronized(lock) {
        observers.size
    }

    /**
     * Notifies observers of state changes.
     *
     * @param oldState The previous state
     * @param newState The new state
     * @param source Optional identifier for the source of changes
     */
    fun notify(
        oldState: PluginState,
        newState: PluginState,
        source: String? = null
    ) {
        val changes = computeChanges(oldState, newState)
        if (changes.isEmpty()) return

        val batch = StateChangeBatch(changes, source)
        notifyBatch(batch)
    }

    /**
     * Notifies observers of a single change.
     *
     * @param path The path that changed
     * @param oldValue The previous value
     * @param newValue The new value
     * @param source Optional identifier for the source
     */
    fun notify(
        path: String,
        oldValue: JsonElement,
        newValue: JsonElement,
        source: String? = null
    ) {
        if (oldValue == newValue) return

        val change = StateChange(path, oldValue, newValue)
        val batch = StateChangeBatch(listOf(change), source)
        notifyBatch(batch)
    }

    /**
     * Notifies observers of a batch of changes.
     *
     * @param batch The batch of changes to notify
     */
    private fun notifyBatch(batch: StateChangeBatch) {
        if (batchMode) {
            // Accumulate changes for later notification
            synchronized(lock) {
                pendingBatch.addAll(batch.changes)
            }
            return
        }

        // Get snapshot of observers to avoid holding lock during callbacks
        val observerSnapshot = synchronized(lock) {
            observers.values.toList()
        }

        // Notify each observer based on their path subscriptions
        observerSnapshot.forEach { entry ->
            val relevantChanges = if (entry.paths == null) {
                // Observer wants all changes
                batch.changes
            } else {
                // Filter to only changes matching observer's paths
                batch.changes.filter { change ->
                    entry.paths.any { observedPath ->
                        change.path == observedPath ||
                        change.path.startsWith("$observedPath.") ||
                        observedPath.startsWith("${change.path}.")
                    }
                }
            }

            if (relevantChanges.isNotEmpty()) {
                val filteredBatch = StateChangeBatch(relevantChanges, batch.source)
                try {
                    entry.callback(filteredBatch)
                } catch (e: Exception) {
                    // Log error but don't let one observer break others
                    println("Error in state observer ${entry.id}: ${e.message}")
                }
            }
        }

        // Emit to Flow subscribers
        _changeFlow.tryEmit(batch)
    }

    /**
     * Computes the differences between two states.
     *
     * @param oldState Previous state
     * @param newState New state
     * @return List of StateChange objects representing the differences
     */
    private fun computeChanges(
        oldState: PluginState,
        newState: PluginState
    ): List<StateChange> {
        val changes = mutableListOf<StateChange>()
        val oldData = oldState.toMap()
        val newData = newState.toMap()

        // Find all changed or added keys
        newData.forEach { (path, newValue) ->
            val oldValue = oldData[path]
            if (oldValue != newValue) {
                changes.add(StateChange(path, oldValue ?: JsonElement.serializer().descriptor.toString().let { kotlinx.serialization.json.JsonNull }, newValue))
            }
        }

        // Find removed keys
        oldData.keys.forEach { path ->
            if (path !in newData) {
                changes.add(StateChange(path, oldData[path]!!, kotlinx.serialization.json.JsonNull))
            }
        }

        return changes
    }

    /**
     * Starts batch mode - changes are accumulated and notified together.
     * Call endBatch() to flush accumulated changes.
     */
    fun startBatch() {
        synchronized(lock) {
            batchMode = true
            pendingBatch.clear()
        }
    }

    /**
     * Ends batch mode and notifies observers of all accumulated changes.
     *
     * @param source Optional identifier for the source of this batch
     */
    fun endBatch(source: String? = null) {
        val batch = synchronized(lock) {
            batchMode = false
            val changes = pendingBatch.toList()
            pendingBatch.clear()
            StateChangeBatch(changes, source)
        }

        if (batch.changes.isNotEmpty()) {
            notifyBatch(batch)
        }
    }

    /**
     * Executes a block with batched change notifications.
     *
     * @param source Optional identifier for the source
     * @param block Code to execute with batching enabled
     */
    inline fun batched(source: String? = null, block: () -> Unit) {
        startBatch()
        try {
            block()
        } finally {
            endBatch(source)
        }
    }

    /**
     * Checks if a specific path is being observed.
     *
     * @param path The path to check
     * @return true if any observer is watching this path
     */
    fun isPathObserved(path: String): Boolean = synchronized(lock) {
        observers.values.any { entry ->
            entry.paths == null || path in entry.paths
        }
    }

    /**
     * Gets all currently observed paths.
     */
    fun getObservedPaths(): Set<String> = synchronized(lock) {
        observers.values
            .flatMap { it.paths ?: emptySet() }
            .toSet()
    }

    /**
     * Clears all state and observers (useful for testing).
     */
    fun clear() {
        synchronized(lock) {
            observers.clear()
            pendingBatch.clear()
            batchMode = false
        }
    }
}
