package com.augmentalis.voiceos.preferences

/**
 * Platform-agnostic preference storage interface.
 */
expect class PreferenceStore {
    /**
     * Store a string preference.
     */
    fun putString(key: String, value: String): PreferenceResult<Unit>

    /**
     * Get a string preference.
     */
    fun getString(key: String, defaultValue: String): PreferenceResult<String>

    /**
     * Store an integer preference.
     */
    fun putInt(key: String, value: Int): PreferenceResult<Unit>

    /**
     * Get an integer preference.
     */
    fun getInt(key: String, defaultValue: Int): PreferenceResult<Int>

    /**
     * Store a long preference.
     */
    fun putLong(key: String, value: Long): PreferenceResult<Unit>

    /**
     * Get a long preference.
     */
    fun getLong(key: String, defaultValue: Long): PreferenceResult<Long>

    /**
     * Store a float preference.
     */
    fun putFloat(key: String, value: Float): PreferenceResult<Unit>

    /**
     * Get a float preference.
     */
    fun getFloat(key: String, defaultValue: Float): PreferenceResult<Float>

    /**
     * Store a boolean preference.
     */
    fun putBoolean(key: String, value: Boolean): PreferenceResult<Unit>

    /**
     * Get a boolean preference.
     */
    fun getBoolean(key: String, defaultValue: Boolean): PreferenceResult<Boolean>

    /**
     * Store a string set preference.
     */
    fun putStringSet(key: String, value: Set<String>): PreferenceResult<Unit>

    /**
     * Get a string set preference.
     */
    fun getStringSet(key: String, defaultValue: Set<String>): PreferenceResult<Set<String>>

    /**
     * Remove a preference.
     */
    fun remove(key: String): PreferenceResult<Unit>

    /**
     * Clear all preferences.
     */
    fun clear(): PreferenceResult<Unit>

    /**
     * Check if a key exists.
     */
    fun contains(key: String): Boolean

    /**
     * Get all preference keys.
     */
    fun getAllKeys(): Set<String>

    /**
     * Register a change listener.
     */
    fun registerListener(listener: PreferenceChangeListener)

    /**
     * Unregister a change listener.
     */
    fun unregisterListener(listener: PreferenceChangeListener)
}

/**
 * Factory for creating preference store instances.
 */
expect object PreferenceStoreFactory {
    /**
     * Create a new PreferenceStore instance.
     *
     * @param config Store configuration
     * @return Platform-specific preference store
     */
    fun create(config: PreferenceConfig = PreferenceConfig()): PreferenceStore
}

/**
 * Extension functions for type-safe preference access.
 */
fun <T : Any> PreferenceStore.put(key: PreferenceKey<T>, value: T): PreferenceResult<Unit> {
    return when (value) {
        is String -> putString(key.key, value)
        is Int -> putInt(key.key, value)
        is Long -> putLong(key.key, value)
        is Float -> putFloat(key.key, value)
        is Boolean -> putBoolean(key.key, value)
        is Set<*> -> putStringSet(key.key, value as Set<String>)
        else -> PreferenceResult.Error("Unsupported preference type: ${value!!::class}")
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> PreferenceStore.get(key: PreferenceKey<T>): PreferenceResult<T> {
    return when (key.defaultValue) {
        is String -> getString(key.key, key.defaultValue as String) as PreferenceResult<T>
        is Int -> getInt(key.key, key.defaultValue as Int) as PreferenceResult<T>
        is Long -> getLong(key.key, key.defaultValue as Long) as PreferenceResult<T>
        is Float -> getFloat(key.key, key.defaultValue as Float) as PreferenceResult<T>
        is Boolean -> getBoolean(key.key, key.defaultValue as Boolean) as PreferenceResult<T>
        is Set<*> -> getStringSet(key.key, key.defaultValue as Set<String>) as PreferenceResult<T>
        else -> PreferenceResult.Error("Unsupported preference type: ${key.defaultValue!!::class}")
    }
}
