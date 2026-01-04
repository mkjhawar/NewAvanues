package com.augmentalis.magicui.components.state

import kotlinx.coroutines.flow.*

/**
 * Base class for reactive state management in AvaElements.
 *
 * MagicState provides a foundation for building reactive UI components
 * that automatically update when the underlying state changes.
 *
 * Usage:
 * ```kotlin
 * val state = MutableMagicState(0)
 * state.setValue(42)
 * state.value.collect { value -> println("New value: $value") }
 * ```
 */
abstract class MagicState<T> {
    /**
     * Observe state changes as a Flow
     */
    abstract val value: StateFlow<T>

    /**
     * Get current state value
     */
    fun current(): T = value.value

    /**
     * Transform this state into a derived state
     */
    fun <R> map(transform: (T) -> R): StateFlow<R> {
        return value.map(transform).stateIn(
            scope = kotlinx.coroutines.GlobalScope,
            started = SharingStarted.Eagerly,
            initialValue = transform(current())
        )
    }

    /**
     * Combine this state with another state
     */
    fun <T2, R> combine(
        other: MagicState<T2>,
        transform: (T, T2) -> R
    ): StateFlow<R> {
        return value.combine(other.value, transform).stateIn(
            scope = kotlinx.coroutines.GlobalScope,
            started = SharingStarted.Eagerly,
            initialValue = transform(current(), other.current())
        )
    }
}

/**
 * Mutable state that can be updated
 *
 * Example:
 * ```kotlin
 * val counter = MutableMagicState(0)
 * counter.setValue(counter.value.value + 1)
 * ```
 */
class MutableMagicState<T>(initialValue: T) : MagicState<T>() {
    private val _value = MutableStateFlow(initialValue)
    override val value: StateFlow<T> = _value.asStateFlow()

    /**
     * Update the state value
     */
    fun setValue(newValue: T) {
        _value.value = newValue
    }

    /**
     * Update the state using a transformation function
     */
    fun update(transform: (T) -> T) {
        _value.value = transform(_value.value)
    }

    /**
     * Reset to initial value
     */
    fun reset(initialValue: T) {
        _value.value = initialValue
    }
}

/**
 * Read-only state wrapper
 */
class ImmutableMagicState<T>(
    override val value: StateFlow<T>
) : MagicState<T>()

/**
 * Create a mutable state
 */
fun <T> mutableStateOf(initialValue: T): MutableMagicState<T> {
    return MutableMagicState(initialValue)
}

/**
 * Create an immutable state from a StateFlow
 */
fun <T> stateOf(flow: StateFlow<T>): ImmutableMagicState<T> {
    return ImmutableMagicState(flow)
}

/**
 * Create a derived state that computes its value from other states
 */
fun <T> derivedStateOf(computation: () -> T): StateFlow<T> {
    return flow {
        emit(computation())
    }.stateIn(
        scope = kotlinx.coroutines.GlobalScope,
        started = SharingStarted.Eagerly,
        initialValue = computation()
    )
}
