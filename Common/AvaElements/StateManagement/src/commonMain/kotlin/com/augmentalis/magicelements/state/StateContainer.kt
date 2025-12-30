package com.augmentalis.avaelements.state

import kotlinx.coroutines.flow.StateFlow

/**
 * Container for managing component-scoped state.
 *
 * StateContainer allows you to store and retrieve state values by key,
 * similar to how Compose's remember works but in a platform-agnostic way.
 *
 * Usage:
 * ```kotlin
 * val container = StateContainer()
 * val counter = container.remember("counter", 0)
 * counter.setValue(42)
 * ```
 */
class StateContainer {
    private val states = mutableMapOf<String, MagicState<*>>()
    private val listeners = mutableMapOf<String, MutableList<(Any?) -> Unit>>()

    /**
     * Remember a state value with the given key.
     * If the key already exists, returns the existing state.
     * Otherwise, creates a new state with the initial value.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> remember(key: String, initialValue: T): MutableMagicState<T> {
        return states.getOrPut(key) {
            MutableMagicState(initialValue).also { state ->
                notifyListeners(key, initialValue)
            }
        } as MutableMagicState<T>
    }

    /**
     * Get an existing state by key, or null if not found
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): MagicState<T>? {
        return states[key] as? MagicState<T>
    }

    /**
     * Check if a state with the given key exists
     */
    fun contains(key: String): Boolean {
        return states.containsKey(key)
    }

    /**
     * Clear a specific state by key
     */
    fun clear(key: String) {
        states.remove(key)
        listeners.remove(key)
    }

    /**
     * Clear all states
     */
    fun clearAll() {
        states.clear()
        listeners.clear()
    }

    /**
     * Get all state keys
     */
    fun keys(): Set<String> {
        return states.keys.toSet()
    }

    /**
     * Add a listener for state changes
     */
    fun addListener(key: String, listener: (Any?) -> Unit) {
        listeners.getOrPut(key) { mutableListOf() }.add(listener)
    }

    /**
     * Remove a listener
     */
    fun removeListener(key: String, listener: (Any?) -> Unit) {
        listeners[key]?.remove(listener)
    }

    /**
     * Notify all listeners for a key
     */
    private fun notifyListeners(key: String, value: Any?) {
        listeners[key]?.forEach { listener ->
            listener(value)
        }
    }

    /**
     * Create a snapshot of current state values
     */
    fun snapshot(): Map<String, Any?> {
        return states.mapValues { (_, state) -> state.current() }
    }

    /**
     * Restore state from a snapshot
     */
    @Suppress("UNCHECKED_CAST")
    fun restore(snapshot: Map<String, Any?>) {
        snapshot.forEach { (key, value) ->
            val state = states[key]
            if (state is MutableMagicState<*>) {
                (state as MutableMagicState<Any?>).setValue(value)
            } else if (value != null) {
                states[key] = MutableMagicState(value)
            }
        }
    }
}

/**
 * Global state container for application-wide state
 */
object GlobalStateContainer : StateContainer()

/**
 * Extension function to remember state in a container
 */
inline fun <reified T> StateContainer.rememberState(
    key: String,
    initialValue: T
): MutableMagicState<T> {
    return remember(key, initialValue)
}

/**
 * Extension function to get or create state
 */
inline fun <reified T> StateContainer.getOrCreate(
    key: String,
    defaultValue: () -> T
): MutableMagicState<T> {
    return get<T>(key) as? MutableMagicState<T> ?: remember(key, defaultValue())
}
