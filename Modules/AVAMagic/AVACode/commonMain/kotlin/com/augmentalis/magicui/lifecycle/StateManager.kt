package com.augmentalis.avamagic.lifecycle

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Manages app state persistence and restoration.
 */
class StateManager(
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
) {
    internal val state = mutableMapOf<String, Any?>()

    /**
     * Save state value.
     */
    fun put(key: String, value: Any?) {
        state[key] = value
    }

    /**
     * Get state value.
     */
    fun get(key: String): Any? {
        return state[key]
    }

    /**
     * Get typed state value.
     */
    @Suppress("NOTHING_TO_INLINE")
    inline fun <reified T> getTyped(key: String): T? {
        return get(key) as? T
    }

    /**
     * Remove state value.
     */
    fun remove(key: String): Any? {
        return state.remove(key)
    }

    /**
     * Clear all state.
     */
    fun clear() {
        state.clear()
    }

    /**
     * Serialize state to JSON.
     */
    fun serialize(): String {
        val serializable = state.mapValues { (_, value) ->
            when (value) {
                is String -> value
                is Number -> value
                is Boolean -> value
                else -> value.toString()
            }
        }
        return json.encodeToString(serializable)
    }

    /**
     * Deserialize state from JSON.
     */
    fun deserialize(jsonString: String) {
        try {
            val deserialized: Map<String, Any?> = json.decodeFromString(jsonString)
            state.clear()
            state.putAll(deserialized)
        } catch (e: Exception) {
            throw StateException("Failed to deserialize state: ${e.message}", e)
        }
    }

    /**
     * Get all state keys.
     */
    fun keys(): Set<String> = state.keys.toSet()

    /**
     * Get state size.
     */
    fun size(): Int = state.size

    /**
     * Check if key exists.
     */
    fun contains(key: String): Boolean = state.containsKey(key)
}

class StateException(message: String, cause: Throwable? = null) : Exception(message, cause)
