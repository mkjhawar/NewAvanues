package com.augmentalis.avamagic.state

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * DSL builder for declarative state management.
 *
 * StateBuilder provides a clean, type-safe API for creating and managing state
 * within MagicElements UI components.
 *
 * Usage:
 * ```kotlin
 * MagicUI {
 *     val state = stateBuilder {
 *         val text = state("")
 *         val count = state(0)
 *         val isValid = derivedState { text.value.value.length > 3 }
 *     }
 * }
 * ```
 */
class StateBuilder(private val container: StateContainer = StateContainer()) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private val stateCounter = mutableMapOf<String, Int>()

    /**
     * Create a mutable state with an initial value
     */
    fun <T> state(initialValue: T): MutableMagicState<T> {
        val key = generateKey("state")
        return container.remember(key, initialValue)
    }

    /**
     * Create a mutable state with a custom key
     */
    fun <T> state(key: String, initialValue: T): MutableMagicState<T> {
        return container.remember(key, initialValue)
    }

    /**
     * Create a derived state that automatically recomputes when dependencies change
     */
    fun <T> derivedState(calculation: StateBuilder.() -> T): StateFlow<T> {
        val initialValue = calculation()
        return flow {
            emit(initialValue)
            // Note: In a real implementation, you'd want to track dependencies
            // and recompute when they change. For now, this is a simplified version.
        }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = initialValue
        )
    }

    /**
     * Create a derived state from a single source state
     */
    fun <T, R> derivedStateFrom(
        source: MagicState<T>,
        transform: (T) -> R
    ): StateFlow<R> {
        return source.value.map(transform).stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = transform(source.current())
        )
    }

    /**
     * Create a derived state from two source states
     */
    fun <T1, T2, R> derivedStateFrom(
        source1: MagicState<T1>,
        source2: MagicState<T2>,
        transform: (T1, T2) -> R
    ): StateFlow<R> {
        return combine(source1.value, source2.value) { v1, v2 ->
            transform(v1, v2)
        }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = transform(source1.current(), source2.current())
        )
    }

    /**
     * Create a derived state from three source states
     */
    fun <T1, T2, T3, R> derivedStateFrom(
        source1: MagicState<T1>,
        source2: MagicState<T2>,
        source3: MagicState<T3>,
        transform: (T1, T2, T3) -> R
    ): StateFlow<R> {
        return combine(source1.value, source2.value, source3.value) { v1, v2, v3 ->
            transform(v1, v2, v3)
        }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = transform(source1.current(), source2.current(), source3.current())
        )
    }

    /**
     * Get the state container
     */
    fun getContainer(): StateContainer = container

    /**
     * Generate a unique key for auto-keyed states
     */
    private fun generateKey(prefix: String): String {
        val count = stateCounter.getOrDefault(prefix, 0)
        stateCounter[prefix] = count + 1
        return "${prefix}_$count"
    }

    /**
     * Clear all states
     */
    fun clear() {
        container.clearAll()
        stateCounter.clear()
    }
}

/**
 * Create a state builder scope
 */
fun stateBuilder(
    container: StateContainer = StateContainer(),
    block: StateBuilder.() -> Unit
): StateBuilder {
    return StateBuilder(container).apply(block)
}

/**
 * Helper class for building reactive computed states
 */
class ComputedStateBuilder<T>(
    private val scope: CoroutineScope,
    private val compute: () -> T
) {
    private val dependencies = mutableListOf<StateFlow<*>>()

    /**
     * Track a dependency for this computed state
     */
    fun <R> StateFlow<R>.track(): R {
        dependencies.add(this)
        return this.value
    }

    /**
     * Build the computed state flow
     */
    fun build(): StateFlow<T> {
        return if (dependencies.isEmpty()) {
            // No dependencies, just compute once
            MutableStateFlow(compute()).asStateFlow()
        } else {
            // Combine all dependencies and recompute
            combine(dependencies) { compute() }.stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = compute()
            )
        }
    }
}

/**
 * Create a computed state with tracked dependencies
 */
fun <T> StateBuilder.computedState(
    compute: ComputedStateBuilder<T>.() -> T
): StateFlow<T> {
    val scope = CoroutineScope(Dispatchers.Main)
    val builder = ComputedStateBuilder(scope) {
        builder.compute()
    }
    return builder.build()
}
