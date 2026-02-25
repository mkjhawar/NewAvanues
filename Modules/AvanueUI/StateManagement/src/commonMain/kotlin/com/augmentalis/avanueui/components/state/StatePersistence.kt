package com.augmentalis.avanueui.state

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*

/**
 * Interface for persisting and restoring state.
 *
 * StatePersistence allows state to survive app restarts, process death,
 * and navigation changes. Implementations handle platform-specific storage
 * (SharedPreferences on Android, UserDefaults on iOS, localStorage on Web).
 *
 * Usage:
 * ```kotlin
 * val persistence = LocalStatePersistence()
 * persistence.save("user_settings", settings)
 * val restored = persistence.restore<Settings>("user_settings")
 * ```
 */
interface StatePersistence {
    /**
     * Save state with a given key
     */
    suspend fun save(key: String, state: Any)

    /**
     * Restore state by key
     */
    suspend fun <T> restore(key: String): T?

    /**
     * Remove saved state
     */
    suspend fun remove(key: String)

    /**
     * Clear all saved state
     */
    suspend fun clear()

    /**
     * Check if a key exists
     */
    suspend fun contains(key: String): Boolean

    /**
     * Get all keys
     */
    suspend fun keys(): Set<String>
}

/**
 * In-memory state persistence implementation.
 * Useful for testing and as a fallback when platform storage is not available.
 */
class InMemoryStatePersistence : StatePersistence {
    private val storage = mutableMapOf<String, Any>()

    override suspend fun save(key: String, state: Any) {
        storage[key] = state
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> restore(key: String): T? {
        return storage[key] as? T
    }

    override suspend fun remove(key: String) {
        storage.remove(key)
    }

    override suspend fun clear() {
        storage.clear()
    }

    override suspend fun contains(key: String): Boolean {
        return storage.containsKey(key)
    }

    override suspend fun keys(): Set<String> {
        return storage.keys.toSet()
    }
}

/**
 * JSON-based state persistence that can be implemented per platform.
 */
abstract class JsonStatePersistence : StatePersistence {
    protected val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Save a JSON string to platform storage
     */
    protected abstract suspend fun saveString(key: String, value: String)

    /**
     * Load a JSON string from platform storage
     */
    protected abstract suspend fun loadString(key: String): String?

    override suspend fun save(key: String, state: Any) {
        val jsonString = when (state) {
            is String -> json.encodeToString(state)
            is Int -> json.encodeToString(state)
            is Long -> json.encodeToString(state)
            is Float -> json.encodeToString(state)
            is Double -> json.encodeToString(state)
            is Boolean -> json.encodeToString(state)
            is List<*> -> json.encodeToString(ListSerializer(JsonElement.serializer()),
                state.map { JsonPrimitive(it.toString()) })
            is Map<*, *> -> json.encodeToString(state as Map<String, JsonElement>)
            else -> state.toString()
        }
        saveString(key, jsonString)
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun <T> restore(key: String): T? {
        val jsonString = loadString(key) ?: return null
        return try {
            // Parse as JsonElement and convert to primitive types
            val element = json.parseToJsonElement(jsonString)
            when (element) {
                is JsonPrimitive -> {
                    when {
                        element.isString -> element.content as? T
                        element.content == "true" || element.content == "false" -> element.content.toBoolean() as? T
                        element.content.toLongOrNull() != null -> element.content.toLong() as? T
                        element.content.toDoubleOrNull() != null -> element.content.toDouble() as? T
                        else -> element.content as? T
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * State manager that combines state containers with persistence.
 */
class StateManager(
    private val persistence: StatePersistence = InMemoryStatePersistence()
) {
    /**
     * Save a state container
     */
    suspend fun saveContainer(container: StateContainer, key: String) {
        val snapshot = container.snapshot()
        persistence.save(key, snapshot)
    }

    /**
     * Restore a state container
     */
    suspend fun restoreContainer(key: String): StateContainer? {
        val snapshot = persistence.restore<Map<String, Any?>>(key) ?: return null
        return StateContainer().apply {
            restore(snapshot)
        }
    }

    /**
     * Save a single state
     */
    suspend fun <T> saveState(key: String, state: MagicState<T>) {
        persistence.save(key, state.current() as Any)
    }

    /**
     * Restore a single state
     */
    suspend fun <T> restoreState(key: String, defaultValue: T): MutableMagicState<T> {
        val value = persistence.restore<T>(key) ?: defaultValue
        return MutableMagicState(value)
    }

    /**
     * Save a ViewModel's state
     */
    suspend fun saveViewModel(viewModel: MagicViewModel, key: String) {
        // Access the protected stateContainer through reflection or a public getter
        // This is a simplified version - in production, you'd want a better approach
    }

    /**
     * Auto-save state on changes
     */
    fun <T> autoSave(
        key: String,
        state: MutableMagicState<T>,
        debounceMillis: Long = 500
    ) {
        kotlinx.coroutines.GlobalScope.launch {
            state.value
                .debounce(debounceMillis)
                .collect { value ->
                    persistence.save(key, value as Any)
                }
        }
    }

    /**
     * Remove saved state
     */
    suspend fun remove(key: String) {
        persistence.remove(key)
    }

    /**
     * Clear all saved state
     */
    suspend fun clear() {
        persistence.clear()
    }
}

/**
 * Persistent state that automatically saves changes
 */
class PersistentState<T>(
    key: String,
    initialValue: T,
    private val manager: StateManager,
    private val debounceMillis: Long = 500
) {
    private val state = MutableMagicState(initialValue)
    val value: StateFlow<T> = state.value

    init {
        // Auto-save on changes
        manager.autoSave(key, state, debounceMillis)
    }

    /**
     * Update the state value
     */
    fun setValue(newValue: T) {
        state.setValue(newValue)
    }

    /**
     * Update using a transform function
     */
    fun update(transform: (T) -> T) {
        state.update(transform)
    }

    /**
     * Get current value
     */
    fun current(): T = state.current()

    companion object {
        /**
         * Create a persistent state with automatic restoration
         */
        suspend fun <T> create(
            key: String,
            defaultValue: T,
            manager: StateManager,
            debounceMillis: Long = 500
        ): PersistentState<T> {
            val initialValue = manager.restoreState(key, defaultValue).current()
            return PersistentState(key, initialValue, manager, debounceMillis)
        }
    }
}

/**
 * Create a persistent state
 */
fun <T> persistentStateOf(
    key: String,
    initialValue: T,
    manager: StateManager,
    debounceMillis: Long = 500
): PersistentState<T> {
    return PersistentState(key, initialValue, manager, debounceMillis)
}

/**
 * Extension function to make a state persistent
 */
fun <T> MutableMagicState<T>.persist(
    key: String,
    manager: StateManager,
    debounceMillis: Long = 500
): MutableMagicState<T> {
    manager.autoSave(key, this, debounceMillis)
    return this
}
