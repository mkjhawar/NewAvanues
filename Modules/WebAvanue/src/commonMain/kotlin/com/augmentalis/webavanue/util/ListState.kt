package com.augmentalis.webavanue.util

import kotlinx.coroutines.flow.StateFlow

/**
 * ListState<T> - Simplifies common list manipulation patterns in ViewModels
 *
 * BEFORE (5+ lines per operation):
 * ```
 * val currentDownloads = _downloads.value.toMutableList()
 * val index = currentDownloads.indexOfFirst { it.id == downloadId }
 * if (index >= 0) {
 *     currentDownloads[index] = currentDownloads[index].copy(status = DownloadStatus.CANCELLED)
 *     _downloads.value = currentDownloads
 * }
 * ```
 *
 * AFTER (1 line):
 * ```
 * downloads.updateItem({ it.id == downloadId }) { it.copy(status = DownloadStatus.CANCELLED) }
 * ```
 *
 * Usage:
 * ```
 * val downloads = ListState<Download>()
 *
 * // Add item
 * downloads.add(newDownload)
 * downloads.addFirst(newDownload)  // Add at beginning
 *
 * // Update specific item
 * downloads.updateItem({ it.id == id }) { it.copy(progress = 50) }
 *
 * // Remove items
 * downloads.removeItem { it.id == id }
 * downloads.removeAll { it.status == DownloadStatus.COMPLETED }
 *
 * // Bulk operations
 * downloads.replaceAll(newList)
 * downloads.clear()
 * ```
 */
class ListState<T>(initialValue: List<T> = emptyList()) {
    private val state = ViewModelState(initialValue)

    /**
     * Read-only StateFlow for UI observation
     */
    val flow: StateFlow<List<T>> = state.flow

    /**
     * Current list value
     */
    var value: List<T>
        get() = state.value
        set(value) { state.value = value }

    /**
     * Add item to end of list
     */
    fun add(item: T) {
        state.update { it + item }
    }

    /**
     * Add item at the beginning of list
     */
    fun addFirst(item: T) {
        state.update { listOf(item) + it }
    }

    /**
     * Add item at specific index
     */
    fun addAt(index: Int, item: T) {
        state.update { list ->
            list.toMutableList().apply { add(index.coerceIn(0, size), item) }
        }
    }

    /**
     * Add all items to end of list
     */
    fun addAll(items: List<T>) {
        state.update { it + items }
    }

    /**
     * Update item matching predicate
     *
     * @param predicate Function to find the item to update
     * @param transform Function to transform the found item
     * @return true if an item was updated, false otherwise
     */
    fun updateItem(predicate: (T) -> Boolean, transform: (T) -> T): Boolean {
        var updated = false
        state.update { list ->
            list.map { item ->
                if (predicate(item)) {
                    updated = true
                    transform(item)
                } else {
                    item
                }
            }
        }
        return updated
    }

    /**
     * Update all items matching predicate
     */
    fun updateAll(predicate: (T) -> Boolean, transform: (T) -> T) {
        state.update { list ->
            list.map { item ->
                if (predicate(item)) transform(item) else item
            }
        }
    }

    /**
     * Remove first item matching predicate
     *
     * @return true if an item was removed, false otherwise
     */
    fun removeItem(predicate: (T) -> Boolean): Boolean {
        var removed = false
        state.update { list ->
            val mutable = list.toMutableList()
            val index = mutable.indexOfFirst(predicate)
            if (index >= 0) {
                mutable.removeAt(index)
                removed = true
            }
            mutable
        }
        return removed
    }

    /**
     * Remove all items matching predicate
     *
     * @return number of items removed
     */
    fun removeAll(predicate: (T) -> Boolean): Int {
        var count = 0
        state.update { list ->
            val result = list.filter { item ->
                val shouldRemove = predicate(item)
                if (shouldRemove) count++
                !shouldRemove
            }
            result
        }
        return count
    }

    /**
     * Replace entire list
     */
    fun replaceAll(newList: List<T>) {
        state.value = newList
    }

    /**
     * Clear all items
     */
    fun clear() {
        state.value = emptyList()
    }

    /**
     * Get item by predicate
     */
    fun find(predicate: (T) -> Boolean): T? {
        return state.value.find(predicate)
    }

    /**
     * Get index of item by predicate
     */
    fun indexOf(predicate: (T) -> Boolean): Int {
        return state.value.indexOfFirst(predicate)
    }

    /**
     * Check if list contains item matching predicate
     */
    fun contains(predicate: (T) -> Boolean): Boolean {
        return state.value.any(predicate)
    }

    /**
     * Get list size
     */
    val size: Int get() = state.value.size

    /**
     * Check if list is empty
     */
    val isEmpty: Boolean get() = state.value.isEmpty()

    /**
     * Filter and return new list (non-mutating)
     */
    fun filter(predicate: (T) -> Boolean): List<T> {
        return state.value.filter(predicate)
    }

    /**
     * Map and return new list (non-mutating)
     */
    fun <R> map(transform: (T) -> R): List<R> {
        return state.value.map(transform)
    }
}
