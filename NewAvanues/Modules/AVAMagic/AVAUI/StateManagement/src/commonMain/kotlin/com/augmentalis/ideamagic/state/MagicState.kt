package com.augmentalis.avamagic.state

import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * MagicState - Reactive state management for IDEAMagic
 *
 * Provides reactive state that automatically triggers recomposition
 * when values change. Supports both Compose State and Kotlin Flow.
 *
 * Features:
 * - Automatic recomposition on state changes
 * - Two-way binding support
 * - Flow integration for async updates
 * - Computed/derived states
 * - State persistence (optional)
 *
 * Usage:
 * ```kotlin
 * // Simple state
 * val counter = magicStateOf(0)
 * counter.value++ // Triggers recomposition
 *
 * // Derived state
 * val isEven = magicDerivedStateOf { counter.value % 2 == 0 }
 *
 * // Two-way binding
 * TextField(
 *     value = text.value,
 *     onValueChange = { text.value = it }
 * )
 * ```
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */

// ===== CORE STATE =====

/**
 * Mutable state wrapper that triggers recomposition
 */
interface MagicState<T> {
    var value: T
    fun asState(): State<T>
    fun asFlow(): StateFlow<T>
}

/**
 * Read-only state wrapper
 */
interface ReadOnlyMagicState<T> {
    val value: T
    fun asState(): State<T>
    fun asFlow(): StateFlow<T>
}

/**
 * Internal implementation of MagicState
 */
private class MagicStateImpl<T>(initialValue: T) : MagicState<T> {
    private val _state = mutableStateOf(initialValue)
    private val _flow = MutableStateFlow(initialValue)

    override var value: T
        get() = _state.value
        set(newValue) {
            _state.value = newValue
            _flow.value = newValue
        }

    override fun asState(): State<T> = _state

    override fun asFlow(): StateFlow<T> = _flow.asStateFlow()
}

/**
 * Create a new MagicState with initial value
 *
 * @param initialValue The initial value of the state
 * @return A new MagicState instance
 */
fun <T> magicStateOf(initialValue: T): MagicState<T> {
    return MagicStateImpl(initialValue)
}

/**
 * Remember a MagicState across recompositions
 *
 * @param initialValue The initial value of the state
 * @return A remembered MagicState instance
 */
@Composable
fun <T> rememberMagicState(initialValue: T): MagicState<T> {
    return remember { magicStateOf(initialValue) }
}

/**
 * Remember a MagicState with calculation key
 *
 * @param key Key to trigger recalculation
 * @param calculation Lambda that produces the initial value
 * @return A remembered MagicState instance
 */
@Composable
fun <T> rememberMagicState(key: Any? = null, calculation: () -> T): MagicState<T> {
    return remember(key) { magicStateOf(calculation()) }
}

// ===== DERIVED STATE =====

/**
 * Derived state that recomputes when dependencies change
 */
interface DerivedMagicState<T> : ReadOnlyMagicState<T>

/**
 * Internal implementation of DerivedMagicState
 */
private class DerivedMagicStateImpl<T>(
    private val calculation: () -> T
) : DerivedMagicState<T> {
    private var _cachedValue: T? = null
    private var _isValid = false

    @Suppress("UNCHECKED_CAST")
    override val value: T
        get() {
            if (!_isValid) {
                _cachedValue = calculation()
                _isValid = true
            }
            return _cachedValue as T
        }

    override fun asState(): State<T> {
        return derivedStateOf { value }
    }

    override fun asFlow(): StateFlow<T> {
        return MutableStateFlow(value).asStateFlow()
    }

    fun invalidate() {
        _isValid = false
    }
}

/**
 * Create a derived state that recomputes when dependencies change
 *
 * @param calculation Lambda that computes the derived value
 * @return A new DerivedMagicState instance
 */
@Composable
fun <T> magicDerivedStateOf(calculation: () -> T): ReadOnlyMagicState<T> {
    return remember { derivedStateOf(calculation) }.value.let { value ->
        object : ReadOnlyMagicState<T> {
            override val value: T = value
            override fun asState(): State<T> = derivedStateOf { this.value }
            override fun asFlow(): StateFlow<T> = MutableStateFlow(value).asStateFlow()
        }
    }
}

// ===== STATE LIST =====

/**
 * Observable list that triggers recomposition on changes
 */
interface MagicStateList<T> : List<T> {
    fun add(element: T)
    fun addAll(elements: Collection<T>)
    fun remove(element: T): Boolean
    fun removeAt(index: Int): T
    fun clear()
    fun set(index: Int, element: T): T
    fun asState(): State<List<T>>
}

/**
 * Internal implementation of MagicStateList
 */
private class MagicStateListImpl<T>(
    initialList: List<T> = emptyList()
) : MagicStateList<T> {
    private val _state = mutableStateListOf<T>().apply { addAll(initialList) }

    override val size: Int get() = _state.size
    override fun contains(element: T): Boolean = _state.contains(element)
    override fun containsAll(elements: Collection<T>): Boolean = _state.containsAll(elements)
    override fun get(index: Int): T = _state[index]
    override fun indexOf(element: T): Int = _state.indexOf(element)
    override fun isEmpty(): Boolean = _state.isEmpty()
    override fun iterator(): Iterator<T> = _state.iterator()
    override fun lastIndexOf(element: T): Int = _state.lastIndexOf(element)
    override fun listIterator(): ListIterator<T> = _state.listIterator()
    override fun listIterator(index: Int): ListIterator<T> = _state.listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int): List<T> = _state.subList(fromIndex, toIndex)

    override fun add(element: T) {
        _state.add(element)
    }

    override fun addAll(elements: Collection<T>) {
        _state.addAll(elements)
    }

    override fun remove(element: T): Boolean {
        return _state.remove(element)
    }

    override fun removeAt(index: Int): T {
        return _state.removeAt(index)
    }

    override fun clear() {
        _state.clear()
    }

    override fun set(index: Int, element: T): T {
        val old = _state[index]
        _state[index] = element
        return old
    }

    override fun asState(): State<List<T>> {
        return derivedStateOf { _state.toList() }
    }
}

/**
 * Create a new MagicStateList
 *
 * @param initialList The initial list contents
 * @return A new MagicStateList instance
 */
fun <T> magicStateListOf(vararg elements: T): MagicStateList<T> {
    return MagicStateListImpl(elements.toList())
}

/**
 * Remember a MagicStateList across recompositions
 *
 * @param initialList The initial list contents
 * @return A remembered MagicStateList instance
 */
@Composable
fun <T> rememberMagicStateList(vararg elements: T): MagicStateList<T> {
    return remember { magicStateListOf(*elements) }
}

// ===== STATE MAP =====

/**
 * Observable map that triggers recomposition on changes
 */
interface MagicStateMap<K, V> : Map<K, V> {
    fun put(key: K, value: V): V?
    fun putAll(from: Map<out K, V>)
    fun remove(key: K): V?
    fun clear()
    fun asState(): State<Map<K, V>>
}

/**
 * Internal implementation of MagicStateMap
 */
private class MagicStateMapImpl<K, V>(
    initialMap: Map<K, V> = emptyMap()
) : MagicStateMap<K, V> {
    private val _state = mutableStateMapOf<K, V>().apply { putAll(initialMap) }

    override val entries: Set<Map.Entry<K, V>> get() = _state.entries
    override val keys: Set<K> get() = _state.keys
    override val size: Int get() = _state.size
    override val values: Collection<V> get() = _state.values

    override fun containsKey(key: K): Boolean = _state.containsKey(key)
    override fun containsValue(value: V): Boolean = _state.containsValue(value)
    override fun get(key: K): V? = _state[key]
    override fun isEmpty(): Boolean = _state.isEmpty()

    override fun put(key: K, value: V): V? {
        return _state.put(key, value)
    }

    override fun putAll(from: Map<out K, V>) {
        _state.putAll(from)
    }

    override fun remove(key: K): V? {
        return _state.remove(key)
    }

    override fun clear() {
        _state.clear()
    }

    override fun asState(): State<Map<K, V>> {
        return derivedStateOf { _state.toMap() }
    }
}

/**
 * Create a new MagicStateMap
 *
 * @param pairs The initial key-value pairs
 * @return A new MagicStateMap instance
 */
fun <K, V> magicStateMapOf(vararg pairs: Pair<K, V>): MagicStateMap<K, V> {
    return MagicStateMapImpl(pairs.toMap())
}

/**
 * Remember a MagicStateMap across recompositions
 *
 * @param pairs The initial key-value pairs
 * @return A remembered MagicStateMap instance
 */
@Composable
fun <K, V> rememberMagicStateMap(vararg pairs: Pair<K, V>): MagicStateMap<K, V> {
    return remember { magicStateMapOf(*pairs) }
}

// ===== TWO-WAY BINDING =====

/**
 * Create a two-way binding for TextField or other input components
 *
 * Usage:
 * ```kotlin
 * val text = rememberMagicState("")
 * TextField(
 *     value = text.value,
 *     onValueChange = text::set
 * )
 * ```
 */
fun <T> MagicState<T>.set(newValue: T) {
    value = newValue
}

/**
 * Binding helper for common cases
 */
@Composable
fun <T> MagicState<T>.binding(): Pair<T, (T) -> Unit> {
    return value to { newValue -> value = newValue }
}
