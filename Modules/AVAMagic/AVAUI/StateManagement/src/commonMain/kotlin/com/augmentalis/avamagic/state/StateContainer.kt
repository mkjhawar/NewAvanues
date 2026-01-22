package com.augmentalis.avamagic.state

/**
 * Container for managing stateful values.
 *
 * StateContainer provides a simple key-value store for MagicState instances,
 * similar to how Compose remembers state across recompositions.
 *
 * Usage:
 * ```kotlin
 * val container = StateContainer()
 * val counter = container.remember("counter", 0)
 * counter.value = 10
 * ```
 */
class StateContainer {
    private val states = mutableMapOf<String, MagicState<*>>()

    /**
     * Remember or create a state with the given key and initial value.
     *
     * If a state with this key already exists, returns the existing state.
     * Otherwise, creates a new state with the initial value.
     *
     * @param key Unique identifier for this state
     * @param initialValue Initial value if state doesn't exist
     * @return The MagicState instance for this key
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> remember(key: String, initialValue: T): MagicState<T> {
        return states.getOrPut(key) {
            magicStateOf(initialValue)
        } as MagicState<T>
    }

    /**
     * Get an existing state by key, or null if not found.
     *
     * @param key The key to look up
     * @return The MagicState if found, null otherwise
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): MagicState<T>? {
        return states[key] as? MagicState<T>
    }

    /**
     * Check if a state exists for the given key.
     */
    fun contains(key: String): Boolean {
        return states.containsKey(key)
    }

    /**
     * Remove a state by key.
     *
     * @return The removed state, or null if not found
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> remove(key: String): MagicState<T>? {
        return states.remove(key) as? MagicState<T>
    }

    /**
     * Get all state keys.
     */
    fun keys(): Set<String> {
        return states.keys.toSet()
    }

    /**
     * Get the number of states in this container.
     */
    fun size(): Int {
        return states.size
    }

    /**
     * Clear all states.
     */
    fun clearAll() {
        states.clear()
    }

    /**
     * Create a snapshot of all current values.
     */
    fun snapshot(): Map<String, Any?> {
        return states.mapValues { it.value.value }
    }

    /**
     * Restore values from a snapshot.
     * Only restores values for keys that exist in the container.
     */
    @Suppress("UNCHECKED_CAST")
    fun restore(snapshot: Map<String, Any?>) {
        snapshot.forEach { (key, value) ->
            (states[key] as? MagicState<Any?>)?.value = value
        }
    }
}

/**
 * Type alias for mutable magic state (for backward compatibility with StateBuilder)
 */
typealias MutableMagicState<T> = MagicState<T>
