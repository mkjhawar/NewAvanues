package com.augmentalis.avaelements.state

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Two-way data binding for reactive UI updates.
 *
 * DataBinding provides bidirectional synchronization between UI state
 * and business logic, automatically propagating changes in both directions.
 *
 * Usage:
 * ```kotlin
 * val nameBinding = DataBinding("") { newName ->
 *     viewModel.updateUserName(newName)
 * }
 *
 * TextField(value = nameBinding.value.value) {
 *     onValueChange = { nameBinding.update(it) }
 * }
 * ```
 */
class DataBinding<T>(
    initialValue: T,
    private val scope: CoroutineScope = kotlinx.coroutines.GlobalScope,
    private val onUpdate: (T) -> Unit = {}
) {
    private val _value = MutableStateFlow(initialValue)
    val value: StateFlow<T> = _value.asStateFlow()

    /**
     * Update the value and notify listeners
     */
    fun update(newValue: T) {
        if (_value.value != newValue) {
            _value.value = newValue
            scope.launch {
                onUpdate(newValue)
            }
        }
    }

    /**
     * Set value without triggering the update callback
     */
    fun setValue(newValue: T) {
        _value.value = newValue
    }

    /**
     * Get current value
     */
    fun get(): T = _value.value

    /**
     * Transform the binding with a mapper
     */
    fun <R> map(
        toR: (T) -> R,
        fromR: (R) -> T
    ): DataBinding<R> {
        return DataBinding(toR(_value.value), scope) { newR ->
            update(fromR(newR))
        }.also { newBinding ->
            // Keep the bindings synchronized
            scope.launch {
                _value.collect { newBinding.setValue(toR(it)) }
            }
        }
    }
}

/**
 * Bidirectional binding that synchronizes two states
 */
class BidirectionalBinding<T>(
    private val source: MutableMagicState<T>,
    private val target: MutableMagicState<T>,
    private val scope: CoroutineScope = kotlinx.coroutines.GlobalScope,
    private val sourceToTarget: (T) -> T = { it },
    private val targetToSource: (T) -> T = { it }
) {
    private var isUpdating = false

    init {
        // Sync source to target
        scope.launch {
            source.value.collect { value ->
                if (!isUpdating) {
                    isUpdating = true
                    target.setValue(sourceToTarget(value))
                    isUpdating = false
                }
            }
        }

        // Sync target to source
        scope.launch {
            target.value.collect { value ->
                if (!isUpdating) {
                    isUpdating = true
                    source.setValue(targetToSource(value))
                    isUpdating = false
                }
            }
        }
    }

    /**
     * Unbind the synchronization
     */
    fun unbind() {
        // In a production implementation, you'd want to cancel the collection jobs
    }
}

/**
 * Property binding for object properties
 */
class PropertyBinding<T, P>(
    private val state: MutableMagicState<T>,
    private val getter: (T) -> P,
    private val setter: (T, P) -> T
) {
    /**
     * Get the property value
     */
    fun get(): P = getter(state.current())

    /**
     * Set the property value
     */
    fun set(value: P) {
        state.update { currentValue ->
            setter(currentValue, value)
        }
    }

    /**
     * Get a StateFlow for the property
     */
    fun asFlow(): StateFlow<P> {
        return state.value.map(getter).stateIn(
            scope = kotlinx.coroutines.GlobalScope,
            started = SharingStarted.Eagerly,
            initialValue = get()
        )
    }
}

/**
 * Collection binding for list-based state
 */
class CollectionBinding<T>(
    initialValue: List<T>,
    private val scope: CoroutineScope = kotlinx.coroutines.GlobalScope,
    private val onUpdate: (List<T>) -> Unit = {}
) {
    private val _items = MutableStateFlow(initialValue)
    val items: StateFlow<List<T>> = _items.asStateFlow()

    /**
     * Add an item to the collection
     */
    fun add(item: T) {
        update(_items.value + item)
    }

    /**
     * Add multiple items to the collection
     */
    fun addAll(items: List<T>) {
        update(_items.value + items)
    }

    /**
     * Remove an item from the collection
     */
    fun remove(item: T) {
        update(_items.value - item)
    }

    /**
     * Remove item at index
     */
    fun removeAt(index: Int) {
        update(_items.value.filterIndexed { i, _ -> i != index })
    }

    /**
     * Update item at index
     */
    fun updateAt(index: Int, item: T) {
        update(_items.value.mapIndexed { i, existing ->
            if (i == index) item else existing
        })
    }

    /**
     * Clear all items
     */
    fun clear() {
        update(emptyList())
    }

    /**
     * Update the entire collection
     */
    fun update(newItems: List<T>) {
        if (_items.value != newItems) {
            _items.value = newItems
            scope.launch {
                onUpdate(newItems)
            }
        }
    }

    /**
     * Get current items
     */
    fun get(): List<T> = _items.value

    /**
     * Get item count
     */
    fun size(): Int = _items.value.size

    /**
     * Check if empty
     */
    fun isEmpty(): Boolean = _items.value.isEmpty()
}

/**
 * Create a data binding
 */
fun <T> dataBindingOf(
    initialValue: T,
    scope: CoroutineScope = kotlinx.coroutines.GlobalScope,
    onUpdate: (T) -> Unit = {}
): DataBinding<T> {
    return DataBinding(initialValue, scope, onUpdate)
}

/**
 * Create a bidirectional binding between two states
 */
fun <T> MutableMagicState<T>.bindTo(
    target: MutableMagicState<T>,
    scope: CoroutineScope = kotlinx.coroutines.GlobalScope
): BidirectionalBinding<T> {
    return BidirectionalBinding(this, target, scope)
}

/**
 * Bind to a property of the state object
 */
fun <T, P> MutableMagicState<T>.bindProperty(
    getter: (T) -> P,
    setter: (T, P) -> T
): PropertyBinding<T, P> {
    return PropertyBinding(this, getter, setter)
}

/**
 * Create a collection binding
 */
fun <T> collectionBindingOf(
    initialValue: List<T> = emptyList(),
    scope: CoroutineScope = kotlinx.coroutines.GlobalScope,
    onUpdate: (List<T>) -> Unit = {}
): CollectionBinding<T> {
    return CollectionBinding(initialValue, scope, onUpdate)
}
