package com.augmentalis.magicelements.core.mel

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * Immutable state container for plugin state management.
 *
 * Provides thread-safe access to state with path-based navigation,
 * copy-on-write updates, and snapshot/restore capabilities.
 *
 * All state updates return a new PluginState instance, preserving immutability.
 *
 * @property schema The state schema defining variable types and defaults
 * @property data The current state data
 */
class PluginState(
    val schema: StateSchema,
    private val data: Map<String, JsonElement>
) {
    /**
     * Thread-safe state access lock.
     */
    private val lock = Any()

    /**
     * History of state snapshots for undo/redo functionality.
     */
    private val history = mutableListOf<Map<String, JsonElement>>()

    /**
     * Maximum history size to prevent memory bloat.
     */
    private val maxHistorySize = 50

    init {
        // Validate initial state against schema
        val errors = schema.validate(data)
        require(errors.isEmpty()) {
            "Initial state validation failed: ${errors.joinToString(", ")}"
        }
    }

    /**
     * Secondary constructor that initializes state from schema defaults.
     */
    constructor(schema: StateSchema) : this(schema, schema.createInitialState())

    /**
     * Gets a value by path. Supports nested access like "config.theme".
     *
     * @param path Dot-separated path to the value
     * @return The JsonElement at the path, or JsonNull if not found
     */
    fun get(path: String): JsonElement = synchronized(lock) {
        val parts = path.split(".")
        var current: JsonElement = data[parts[0]] ?: return JsonNull

        for (i in 1 until parts.size) {
            current = when (current) {
                is JsonObject -> current.jsonObject[parts[i]] ?: return JsonNull
                is JsonArray -> {
                    val index = parts[i].toIntOrNull()
                    if (index != null && index >= 0 && index < current.jsonArray.size) {
                        current.jsonArray[index]
                    } else {
                        return JsonNull
                    }
                }
                else -> return JsonNull
            }
        }

        return current
    }

    /**
     * Gets multiple values by their paths.
     *
     * @param paths List of paths to retrieve
     * @return Map of paths to their values
     */
    fun getAll(paths: List<String>): Map<String, JsonElement> {
        return paths.associateWith { get(it) }
    }

    /**
     * Updates state with new values (immutable - returns new instance).
     *
     * @param updates Map of paths to new values
     * @return New PluginState instance with updated values
     */
    fun update(updates: Map<String, JsonElement>): PluginState = synchronized(lock) {
        // Save current state to history before updating
        if (history.size >= maxHistorySize) {
            history.removeAt(0)
        }
        history.add(data.toMap())

        // Apply updates to create new state
        val newData = data.toMutableMap()

        updates.forEach { (path, value) ->
            val parts = path.split(".")

            if (parts.size == 1) {
                // Simple top-level update
                newData[path] = value
            } else {
                // Nested update - need to reconstruct the parent objects
                newData[parts[0]] = updateNested(data[parts[0]] ?: JsonNull, parts.drop(1), value)
            }
        }

        // Validate the new state
        val errors = schema.validate(newData)
        require(errors.isEmpty()) {
            "State update validation failed: ${errors.joinToString(", ")}"
        }

        return PluginState(schema, newData)
    }

    /**
     * Updates a single value by path (convenience method).
     *
     * @param path Path to the value
     * @param value New value
     * @return New PluginState instance
     */
    fun update(path: String, value: JsonElement): PluginState {
        return update(mapOf(path to value))
    }

    /**
     * Helper function to update nested values immutably.
     */
    private fun updateNested(
        current: JsonElement,
        pathParts: List<String>,
        value: JsonElement
    ): JsonElement {
        if (pathParts.isEmpty()) return value

        val key = pathParts[0]
        val rest = pathParts.drop(1)

        return when (current) {
            is JsonObject -> {
                val obj = current.jsonObject.toMutableMap()
                obj[key] = updateNested(obj[key] ?: JsonNull, rest, value)
                JsonObject(obj)
            }
            is JsonArray -> {
                val index = key.toIntOrNull() ?: throw IllegalArgumentException("Invalid array index: $key")
                val arr = current.jsonArray.toMutableList()
                if (index >= 0 && index < arr.size) {
                    arr[index] = updateNested(arr[index], rest, value)
                    JsonArray(arr)
                } else {
                    throw IndexOutOfBoundsException("Array index $index out of bounds")
                }
            }
            else -> {
                // Can't navigate further into primitive values
                throw IllegalArgumentException("Cannot navigate into primitive value at path $key")
            }
        }
    }

    /**
     * Creates a complete snapshot of the current state.
     *
     * @return Immutable copy of current state data
     */
    fun snapshot(): Map<String, JsonElement> = synchronized(lock) {
        data.toMap()
    }

    /**
     * Restores state from a snapshot (immutable - returns new instance).
     *
     * @param snapshot Previously captured state snapshot
     * @return New PluginState instance with restored state
     */
    fun restore(snapshot: Map<String, JsonElement>): PluginState {
        val errors = schema.validate(snapshot)
        require(errors.isEmpty()) {
            "Snapshot validation failed: ${errors.joinToString(", ")}"
        }

        return PluginState(schema, snapshot)
    }

    /**
     * Restores the previous state from history (undo).
     *
     * @return New PluginState instance with previous state, or current state if history is empty
     */
    fun undo(): PluginState = synchronized(lock) {
        if (history.isEmpty()) {
            return this
        }

        val previous = history.removeAt(history.size - 1)
        return PluginState(schema, previous)
    }

    /**
     * Checks if undo is possible.
     */
    fun canUndo(): Boolean = synchronized(lock) {
        history.isNotEmpty()
    }

    /**
     * Gets only the persistent state variables (for saving to storage).
     *
     * @return Map of persistent variable names to their current values
     */
    fun getPersistentState(): Map<String, JsonElement> = synchronized(lock) {
        val persistentVars = schema.getPersistentVariables()
        data.filterKeys { it in persistentVars }
    }

    /**
     * Resets state to schema defaults (immutable - returns new instance).
     *
     * @return New PluginState instance with default values
     */
    fun reset(): PluginState {
        return PluginState(schema)
    }

    /**
     * Resets specific variables to their defaults.
     *
     * @param paths List of variable paths to reset
     * @return New PluginState instance with reset values
     */
    fun reset(paths: List<String>): PluginState {
        val updates = paths.mapNotNull { path ->
            val parts = path.split(".")
            val variable = schema.variables[parts[0]]
            variable?.let { path to it.default }
        }.toMap()

        return update(updates)
    }

    /**
     * Gets all state as a flat map (useful for debugging).
     */
    fun toMap(): Map<String, JsonElement> = synchronized(lock) {
        data.toMap()
    }

    /**
     * Merges another state into this one (immutable - returns new instance).
     * Other state's values take precedence on conflicts.
     *
     * @param other State to merge in
     * @return New PluginState instance with merged state
     */
    fun merge(other: PluginState): PluginState {
        require(schema == other.schema) {
            "Cannot merge states with different schemas"
        }

        val merged = data.toMutableMap()
        merged.putAll(other.data)

        return PluginState(schema, merged)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PluginState) return false
        return data == other.data && schema == other.schema
    }

    override fun hashCode(): Int {
        var result = schema.hashCode()
        result = 31 * result + data.hashCode()
        return result
    }

    override fun toString(): String {
        return "PluginState(data=$data)"
    }

    companion object {
        /**
         * Creates a state from a map of defaults (convenience method).
         */
        fun fromDefaults(defaults: Map<String, JsonElement>): PluginState {
            val schema = StateSchema.fromDefaults(defaults)
            return PluginState(schema, defaults)
        }

        /**
         * Creates an empty state (useful for testing).
         */
        fun empty(): PluginState {
            return PluginState(StateSchema.empty(), emptyMap())
        }
    }
}
