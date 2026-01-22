package com.augmentalis.avamagic.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*

/**
 * Computed state that derives its value from other states.
 * Automatically recomputes when any dependency changes.
 *
 * Usage:
 * ```kotlin
 * val firstName = mutableStateOf("John")
 * val lastName = mutableStateOf("Doe")
 * val fullName = computed(firstName, lastName) {
 *     "${firstName.value} ${lastName.value}"
 * }
 * ```
 */
class ComputedState<T>(
    private val dependencies: List<State<*>>,
    private val compute: () -> T
) : State<T> {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _value: StateFlow<T>

    init {
        val initialValue = compute()

        if (dependencies.isEmpty()) {
            // No dependencies, create constant flow
            _value = MutableStateFlow(initialValue).asStateFlow()
        } else {
            // Combine all dependency flows
            _value = combine(dependencies.map { it.observe() }) {
                compute()
            }.stateIn(
                scope = scope,
                started = SharingStarted.Eagerly,
                initialValue = initialValue
            )
        }
    }

    override val value: T
        get() = _value.value

    override fun update(newValue: T) {
        throw UnsupportedOperationException("Cannot update computed state directly. Update its dependencies instead.")
    }

    override fun observe(): StateFlow<T> = _value
}

/**
 * Factory function to create a computed state from multiple dependencies
 */
fun <T> computed(
    vararg dependencies: State<*>,
    compute: () -> T
): ComputedState<T> {
    return ComputedState(dependencies.toList(), compute)
}

/**
 * Create a computed state from a single dependency
 */
fun <T, R> computed(
    dependency: State<T>,
    compute: (T) -> R
): ComputedState<R> {
    return ComputedState(listOf(dependency)) {
        compute(dependency.value)
    }
}

/**
 * Create a computed state from two dependencies
 */
fun <T1, T2, R> computed(
    dep1: State<T1>,
    dep2: State<T2>,
    compute: (T1, T2) -> R
): ComputedState<R> {
    return ComputedState(listOf(dep1, dep2)) {
        compute(dep1.value, dep2.value)
    }
}

/**
 * Create a computed state from three dependencies
 */
fun <T1, T2, T3, R> computed(
    dep1: State<T1>,
    dep2: State<T2>,
    dep3: State<T3>,
    compute: (T1, T2, T3) -> R
): ComputedState<R> {
    return ComputedState(listOf(dep1, dep2, dep3)) {
        compute(dep1.value, dep2.value, dep3.value)
    }
}

/**
 * Create a computed state from four dependencies
 */
fun <T1, T2, T3, T4, R> computed(
    dep1: State<T1>,
    dep2: State<T2>,
    dep3: State<T3>,
    dep4: State<T4>,
    compute: (T1, T2, T3, T4) -> R
): ComputedState<R> {
    return ComputedState(listOf(dep1, dep2, dep3, dep4)) {
        compute(dep1.value, dep2.value, dep3.value, dep4.value)
    }
}

/**
 * Extension function to create computed state from MutableState
 */
fun <T, R> MutableState<T>.computed(
    transform: (T) -> R
): ComputedState<R> {
    return computed(this, transform)
}

/**
 * Combine two states into a computed state
 */
infix fun <T1, T2> State<T1>.with(other: State<T2>): Pair<State<T1>, State<T2>> {
    return this to other
}

/**
 * Create computed state from a pair of states
 */
fun <T1, T2, R> Pair<State<T1>, State<T2>>.compute(
    transform: (T1, T2) -> R
): ComputedState<R> {
    return computed(first, second, transform)
}

/**
 * Computed list operations
 */
object ComputedOps {

    /**
     * Map a list state to a new computed list state
     */
    fun <T, R> State<List<T>>.mapList(
        transform: (T) -> R
    ): ComputedState<List<R>> {
        return computed(this) { list ->
            list.map(transform)
        }
    }

    /**
     * Filter a list state
     */
    fun <T> State<List<T>>.filterList(
        predicate: (T) -> Boolean
    ): ComputedState<List<T>> {
        return computed(this) { list ->
            list.filter(predicate)
        }
    }

    /**
     * Sort a list state
     */
    fun <T : Comparable<T>> State<List<T>>.sortedList(): ComputedState<List<T>> {
        return computed(this) { list ->
            list.sorted()
        }
    }

    /**
     * Sort a list state by a selector
     */
    fun <T, R : Comparable<R>> State<List<T>>.sortedByList(
        selector: (T) -> R
    ): ComputedState<List<R>> {
        return computed(this) { list ->
            list.sortedBy(selector)
        }
    }

    /**
     * Count items in a list state
     */
    fun <T> State<List<T>>.countList(): ComputedState<Int> {
        return computed(this) { it.size }
    }

    /**
     * Check if list is empty
     */
    fun <T> State<List<T>>.isEmptyList(): ComputedState<Boolean> {
        return computed(this) { it.isEmpty() }
    }

    /**
     * Get first item or null
     */
    fun <T> State<List<T>>.firstOrNullList(): ComputedState<T?> {
        return computed(this) { it.firstOrNull() }
    }

    /**
     * Get last item or null
     */
    fun <T> State<List<T>>.lastOrNullList(): ComputedState<T?> {
        return computed(this) { it.lastOrNull() }
    }
}

/**
 * Computed math operations for numeric states
 */
object ComputedMath {

    /**
     * Sum of two numeric states
     */
    fun State<Int>.plus(other: State<Int>): ComputedState<Int> {
        return computed(this, other) { a, b -> a + b }
    }

    /**
     * Difference of two numeric states
     */
    fun State<Int>.minus(other: State<Int>): ComputedState<Int> {
        return computed(this, other) { a, b -> a - b }
    }

    /**
     * Product of two numeric states
     */
    fun State<Int>.times(other: State<Int>): ComputedState<Int> {
        return computed(this, other) { a, b -> a * b }
    }

    /**
     * Division of two numeric states
     */
    fun State<Int>.div(other: State<Int>): ComputedState<Int> {
        return computed(this, other) { a, b ->
            if (b == 0) 0 else a / b
        }
    }

    /**
     * Sum of double states
     */
    fun State<Double>.plus(other: State<Double>): ComputedState<Double> {
        return computed(this, other) { a, b -> a + b }
    }

    /**
     * Difference of double states
     */
    fun State<Double>.minus(other: State<Double>): ComputedState<Double> {
        return computed(this, other) { a, b -> a - b }
    }

    /**
     * Product of double states
     */
    fun State<Double>.times(other: State<Double>): ComputedState<Double> {
        return computed(this, other) { a, b -> a * b }
    }

    /**
     * Division of double states
     */
    fun State<Double>.div(other: State<Double>): ComputedState<Double> {
        return computed(this, other) { a, b ->
            if (b == 0.0) 0.0 else a / b
        }
    }
}

/**
 * Computed boolean operations
 */
object ComputedBool {

    /**
     * Logical AND of two boolean states
     */
    infix fun State<Boolean>.and(other: State<Boolean>): ComputedState<Boolean> {
        return computed(this, other) { a, b -> a && b }
    }

    /**
     * Logical OR of two boolean states
     */
    infix fun State<Boolean>.or(other: State<Boolean>): ComputedState<Boolean> {
        return computed(this, other) { a, b -> a || b }
    }

    /**
     * Logical NOT of a boolean state
     */
    fun State<Boolean>.not(): ComputedState<Boolean> {
        return computed(this) { !it }
    }

    /**
     * Logical XOR of two boolean states
     */
    infix fun State<Boolean>.xor(other: State<Boolean>): ComputedState<Boolean> {
        return computed(this, other) { a, b -> a xor b }
    }
}

/**
 * Computed string operations
 */
object ComputedString {

    /**
     * Concatenate two string states
     */
    operator fun State<String>.plus(other: State<String>): ComputedState<String> {
        return computed(this, other) { a, b -> a + b }
    }

    /**
     * Check if string is blank
     */
    fun State<String>.isBlank(): ComputedState<Boolean> {
        return computed(this) { it.isBlank() }
    }

    /**
     * Check if string is empty
     */
    fun State<String>.isEmpty(): ComputedState<Boolean> {
        return computed(this) { it.isEmpty() }
    }

    /**
     * Get string length
     */
    fun State<String>.length(): ComputedState<Int> {
        return computed(this) { it.length }
    }

    /**
     * Uppercase string
     */
    fun State<String>.uppercase(): ComputedState<String> {
        return computed(this) { it.uppercase() }
    }

    /**
     * Lowercase string
     */
    fun State<String>.lowercase(): ComputedState<String> {
        return computed(this) { it.lowercase() }
    }

    /**
     * Trim string
     */
    fun State<String>.trim(): ComputedState<String> {
        return computed(this) { it.trim() }
    }
}

/**
 * Memoized computed state that only recomputes when inputs change
 */
class MemoizedComputedState<T>(
    private val dependencies: List<State<*>>,
    private val compute: () -> T,
    private val areEqual: (T, T) -> Boolean = { a, b -> a == b }
) : State<T> {
    private var cachedValue: T? = null
    private var lastDependencyValues: List<Any?>? = null
    private val _value = MutableStateFlow(compute())

    override val value: T
        get() {
            val currentDependencyValues = dependencies.map { it.value }

            if (lastDependencyValues == null || currentDependencyValues != lastDependencyValues) {
                val newValue = compute()

                if (cachedValue == null || !areEqual(cachedValue!!, newValue)) {
                    cachedValue = newValue
                    _value.value = newValue
                }

                lastDependencyValues = currentDependencyValues
            }

            return _value.value
        }

    override fun update(newValue: T) {
        throw UnsupportedOperationException("Cannot update memoized computed state")
    }

    override fun observe(): StateFlow<T> = _value.asStateFlow()
}

/**
 * Create a memoized computed state
 */
fun <T> memoizedComputed(
    vararg dependencies: State<*>,
    areEqual: (T, T) -> Boolean = { a, b -> a == b },
    compute: () -> T
): MemoizedComputedState<T> {
    return MemoizedComputedState(dependencies.toList(), compute, areEqual)
}
