package com.augmentalis.foundation.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModelState<T> - Eliminates repetitive StateFlow boilerplate
 *
 * BEFORE (3 lines per state):
 * ```
 * private val _isLoading = MutableStateFlow(false)
 * val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
 * ```
 *
 * AFTER (1 line per state):
 * ```
 * val isLoading = ViewModelState(false)
 * ```
 *
 * Usage:
 * - Read value: `state.value`
 * - Update value: `state.value = newValue`
 * - Expose to UI: `state.flow` (read-only StateFlow)
 * - Lambda update: `state.update { it + 1 }`
 *
 * @param T Type of the state value
 * @param initialValue Initial value for the state
 */
class ViewModelState<T>(initialValue: T) {
    private val _state = MutableStateFlow(initialValue)

    /**
     * Read-only StateFlow for UI observation (equivalent to `asStateFlow()`)
     */
    val flow: StateFlow<T> = _state.asStateFlow()

    /**
     * Current value - can be read or written
     */
    var value: T
        get() = _state.value
        set(value) { _state.value = value }

    /**
     * Atomic update using transform function
     */
    fun update(transform: (T) -> T) {
        _state.value = transform(_state.value)
    }

    /**
     * Collect the underlying flow (for suspend functions)
     */
    suspend fun collect(collector: (T) -> Unit) {
        _state.collect { collector(it) }
    }
}

/**
 * NullableState<T> - ViewModelState specifically for nullable types
 *
 * Adds convenience methods for working with nullable dialog/error states.
 */
class NullableState<T : Any>(initialValue: T? = null) {
    private val _state = MutableStateFlow<T?>(initialValue)

    val flow: StateFlow<T?> = _state.asStateFlow()

    var value: T?
        get() = _state.value
        set(value) { _state.value = value }

    /**
     * Clear the state (set to null)
     */
    fun clear() {
        _state.value = null
    }

    /**
     * Check if state has a value
     */
    fun hasValue(): Boolean = _state.value != null

    /**
     * Execute block only if value is not null
     */
    fun ifPresent(block: (T) -> Unit) {
        _state.value?.let(block)
    }

    /**
     * Set value and return it (for chaining)
     */
    fun set(value: T): T {
        _state.value = value
        return value
    }
}
