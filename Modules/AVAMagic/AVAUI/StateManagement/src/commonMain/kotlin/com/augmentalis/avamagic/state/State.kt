package com.augmentalis.avamagic.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Read-only state interface.
 *
 * Provides observable state that can be used with Flow.
 * This is a custom state abstraction (not Compose State) designed for
 * cross-platform state management with Flow-based observation.
 *
 * Named FlowState to avoid conflicts with androidx.compose.runtime.State
 *
 * @param T The type of value held by this state
 */
interface FlowState<T> {
    /**
     * The current value of this state
     */
    val value: T

    /**
     * Update the state value (throws for read-only states like ComputedState)
     */
    fun update(newValue: T)

    /**
     * Observe changes to this state as a StateFlow
     */
    fun observe(): StateFlow<T>
}

/**
 * Mutable state interface that allows updating the value.
 *
 * @param T The type of value held by this state
 */
interface MutableFlowState<T> : FlowState<T> {
    /**
     * The current value of this state (readable and writable)
     */
    override var value: T
}

/**
 * Default implementation of MutableFlowState backed by MutableStateFlow.
 */
private class MutableFlowStateImpl<T>(initialValue: T) : MutableFlowState<T> {
    private val _flow = MutableStateFlow(initialValue)

    override var value: T
        get() = _flow.value
        set(newValue) {
            _flow.value = newValue
        }

    override fun update(newValue: T) {
        value = newValue
    }

    override fun observe(): StateFlow<T> = _flow.asStateFlow()
}

/**
 * Create a new mutable flow state with the given initial value.
 *
 * Usage:
 * ```kotlin
 * val counter = mutableFlowStateOf(0)
 * counter.value = 10  // Direct assignment
 * counter.update(20)  // Method update
 * counter.updateWith { it + 1 }  // Transform update
 *
 * // Observe changes
 * counter.observe().collect { value ->
 *     println("Counter changed to: $value")
 * }
 * ```
 *
 * @param initialValue The initial value for the state
 * @return A new MutableFlowState instance
 */
fun <T> mutableFlowStateOf(initialValue: T): MutableFlowState<T> {
    return MutableFlowStateImpl(initialValue)
}

/**
 * Alias for backward compatibility with code using mutableStateOf
 */
@Suppress("FunctionName")
fun <T> mutableStateOf(initialValue: T): MutableFlowState<T> {
    return mutableFlowStateOf(initialValue)
}

/**
 * Type aliases for backward compatibility
 */
typealias State<T> = FlowState<T>
typealias MutableState<T> = MutableFlowState<T>

/**
 * Update state with a transformation function.
 *
 * Usage:
 * ```kotlin
 * val counter = mutableFlowStateOf(0)
 * counter.updateWith { it + 1 }  // Increment
 * ```
 */
inline fun <T> MutableFlowState<T>.updateWith(transform: (T) -> T) {
    value = transform(value)
}

/**
 * Create a read-only view of a mutable state.
 */
fun <T> MutableFlowState<T>.asReadOnly(): FlowState<T> = this

/**
 * Map a state to a new state with a transform function.
 * Note: This creates a snapshot, not a reactive computed state.
 * For reactive computed values, use ComputedState.
 */
fun <T, R> FlowState<T>.map(transform: (T) -> R): R = transform(value)

/**
 * Combine two states into a pair.
 */
fun <T1, T2> FlowState<T1>.combinedWith(other: FlowState<T2>): Pair<T1, T2> {
    return Pair(value, other.value)
}
