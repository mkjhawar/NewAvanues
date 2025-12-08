package com.augmentalis.avanues.avamagic.components.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * StateScope manages a collection of states for a component tree.
 * Provides state lifecycle management and persistence capabilities.
 */
open class StateScope(
    private val persistence: StatePersistence? = null
) {
    private val states = mutableMapOf<String, State<*>>()
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val savedStates = mutableMapOf<String, Any?>()

    /**
     * Remember a state value that survives recomposition.
     * Creates a new state if the key doesn't exist, otherwise returns existing state.
     */
    fun <T> remember(key: String, initialValue: T): MutableState<T> {
        @Suppress("UNCHECKED_CAST")
        return states.getOrPut(key) {
            MutableState(initialValue)
        } as MutableState<T>
    }

    /**
     * Remember a state value that survives configuration changes.
     * Attempts to restore from savedStates or persistent storage.
     */
    suspend fun <T> rememberSaveable(key: String, initialValue: T): MutableState<T> {
        @Suppress("UNCHECKED_CAST")
        return states.getOrPut(key) {
            // Try to restore from saved state first
            val restoredValue = savedStates[key] as? T
                ?: persistence?.restore<T>(key)
                ?: initialValue

            val state = MutableState(restoredValue)

            // Auto-save to persistence if available
            persistence?.let { persist ->
                coroutineScope.launch {
                    state.observe().collect { value ->
                        persist.save(key, value as Any)
                    }
                }
            }

            state
        } as MutableState<T>
    }

    /**
     * Save current state for restoration (e.g., during configuration changes)
     */
    fun saveState(): Map<String, Any?> {
        return states.mapValues { (_, state) -> state.value }
    }

    /**
     * Restore state from a previously saved state map
     */
    fun restoreState(savedState: Map<String, Any?>) {
        savedStates.clear()
        savedStates.putAll(savedState)

        // Update existing states
        savedState.forEach { (key, value) ->
            @Suppress("UNCHECKED_CAST")
            val state = states[key] as? MutableState<Any?>
            if (state != null && value != null) {
                state.update(value)
            }
        }
    }

    /**
     * Clear all states and cancel coroutines
     */
    fun clear() {
        states.clear()
        savedStates.clear()
        coroutineScope.cancel()
    }

    /**
     * Remove a specific state by key
     */
    fun remove(key: String) {
        states.remove(key)
        savedStates.remove(key)
    }

    /**
     * Check if a state exists
     */
    fun contains(key: String): Boolean {
        return states.containsKey(key)
    }

    /**
     * Get all state keys
     */
    fun keys(): Set<String> {
        return states.keys.toSet()
    }

    /**
     * Clear only transient states (not saveable)
     */
    fun clearTransient() {
        // Could implement marking states as transient vs persistent
        // For now, just a placeholder for future enhancement
    }
}

/**
 * Global state scope for application-wide state
 */
object GlobalStateScope : StateScope()

/**
 * Extension to create a child scope
 */
fun StateScope.createChildScope(persistence: StatePersistence? = null): StateScope {
    return StateScope(persistence)
}
