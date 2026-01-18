package com.augmentalis.magicdata

import android.content.Context
import android.content.SharedPreferences

/**
 * Android implementation of Database using SharedPreferences for key-value storage.
 *
 * Collection operations are currently stub implementations. Full Room Database
 * integration will be added in a future update.
 *
 * @property name The name of the database
 * @property version The database version
 * @property context Android application context
 * @since 1.0.0
 */
actual class Database(
    private val context: Context,
    actual var name: String,
    actual var version: Int
) {
    private val preferences: SharedPreferences by lazy {
        context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    private val collections = mutableMapOf<String, Collection>()

    /**
     * Stores a value associated with a key using SharedPreferences.
     *
     * @param key The key to store the value under
     * @param value The value to store (String, Int, Long, Float, Boolean supported)
     */
    actual fun put(key: String, value: Any?) {
        val editor = preferences.edit()
        when (value) {
            null -> editor.remove(key)
            is String -> editor.putString(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Float -> editor.putFloat(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Double -> editor.putFloat(key, value.toFloat())
            else -> editor.putString(key, value.toString())
        }
        editor.apply()
    }

    /**
     * Retrieves a value associated with a key from SharedPreferences.
     *
     * @param key The key to retrieve
     * @return The stored value, or null if not found
     */
    actual fun get(key: String): Any? {
        return preferences.all[key]
    }

    /**
     * Retrieves a typed value associated with a key.
     *
     * @param T The expected type
     * @param key The key to retrieve
     * @return The stored value cast to type T, or null if not found
     */
    @Suppress("UNCHECKED_CAST")
    actual fun <T> getTyped(key: String): T? {
        return preferences.all[key] as? T
    }

    /**
     * Removes a key-value pair from SharedPreferences.
     *
     * @param key The key to remove
     */
    actual fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }

    /**
     * Removes all key-value pairs from SharedPreferences.
     */
    actual fun clear() {
        preferences.edit().clear().apply()
    }

    /**
     * Checks if a key exists in SharedPreferences.
     *
     * @param key The key to check
     * @return true if the key exists, false otherwise
     */
    actual fun containsKey(key: String): Boolean {
        return preferences.contains(key)
    }

    /**
     * Returns all keys in SharedPreferences.
     *
     * @return Set of all keys
     */
    actual fun keys(): Set<String> {
        return preferences.all.keys
    }

    /**
     * Creates a new collection.
     *
     * NOTE: This is a stub implementation. Room Database integration coming soon.
     *
     * @param name The name of the collection
     * @param schema Optional schema defining field types
     */
    actual fun createCollection(name: String, schema: CollectionSchema?) {
        if (!collections.containsKey(name)) {
            collections[name] = Collection(context, name, this.name)
        }
    }

    /**
     * Retrieves a collection by name.
     *
     * @param name The name of the collection
     * @return The collection if it exists, null otherwise
     */
    actual fun getCollection(name: String): Collection? {
        return collections[name]
    }

    /**
     * Deletes a collection and all its documents.
     *
     * NOTE: This is a stub implementation. Room Database integration coming soon.
     *
     * @param name The name of the collection to drop
     */
    actual fun dropCollection(name: String) {
        collections.remove(name)
    }

    /**
     * Lists all collection names in the database.
     *
     * @return List of collection names
     */
    actual fun listCollections(): List<String> {
        return collections.keys.toList()
    }

    /**
     * Opens the database connection.
     */
    actual fun open() {
        // SharedPreferences doesn't require explicit opening
    }

    /**
     * Closes the database connection.
     */
    actual fun close() {
        // SharedPreferences doesn't require explicit closing
    }

    /**
     * Forces all pending writes to disk.
     */
    actual fun flush() {
        // SharedPreferences.apply() already handles async writes
    }
}
