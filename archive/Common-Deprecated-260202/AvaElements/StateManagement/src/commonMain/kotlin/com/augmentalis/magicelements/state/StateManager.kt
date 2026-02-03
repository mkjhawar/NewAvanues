package com.augmentalis.avaelements.state

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Base interface for reactive state management.
 * Represents a single piece of state that can be observed and updated.
 */
interface State<T> {
    /**
     * Current value of the state
     */
    val value: T

    /**
     * Update the state with a new value
     */
    fun update(newValue: T)

    /**
     * Observe changes to the state as a Flow
     */
    fun observe(): Flow<T>
}

/**
 * Mutable state implementation using Kotlin Flow's StateFlow.
 * Thread-safe and supports multiple observers.
 */
class MutableState<T>(initialValue: T) : State<T> {
    private val _state = MutableStateFlow(initialValue)

    override val value: T
        get() = _state.value

    override fun update(newValue: T) {
        _state.value = newValue
    }

    override fun observe(): StateFlow<T> = _state.asStateFlow()

    /**
     * Update state based on current value
     */
    fun updateWith(transform: (T) -> T) {
        _state.value = transform(_state.value)
    }

    /**
     * Compare and set - only update if current value matches expected
     */
    fun compareAndSet(expected: T, newValue: T): Boolean {
        return _state.compareAndSet(expected, newValue)
    }
}

/**
 * Read-only state implementation.
 * Wraps a MutableState but doesn't expose update capabilities.
 */
class ReadOnlyState<T>(private val backingState: MutableState<T>) : State<T> {
    override val value: T
        get() = backingState.value

    override fun update(newValue: T) {
        throw UnsupportedOperationException("Cannot update read-only state")
    }

    override fun observe(): Flow<T> = backingState.observe()
}

// Factory functions

/**
 * Create a mutable state with an initial value
 */
fun <T> mutableStateOf(initialValue: T): MutableState<T> {
    return MutableState(initialValue)
}

/**
 * Create a read-only state with an initial value
 */
fun <T> stateOf(initialValue: T): State<T> {
    return ReadOnlyState(MutableState(initialValue))
}

/**
 * Create a state from an existing MutableState as read-only
 */
fun <T> MutableState<T>.asReadOnly(): State<T> {
    return ReadOnlyState(this)
}

/**
 * State snapshot for capturing current values
 */
data class StateSnapshot<T>(
    val value: T,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Extension to create snapshots
 */
fun <T> State<T>.snapshot(): StateSnapshot<T> {
    return StateSnapshot(value)
}
