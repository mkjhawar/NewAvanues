package com.augmentalis.magicdata

/**
 * Platform-agnostic database with key-value and collection storage.
 *
 * Provides two storage paradigms:
 * 1. Key-Value: Simple get/put operations for storing primitive values
 * 2. Collections: Structured document storage with CRUD operations and queries
 *
 * Platform implementations:
 * - Android: SharedPreferences (key-value) + Room Database (collections)
 * - iOS: UserDefaults (key-value) + Core Data (collections)
 * - JVM: Properties file (key-value) + SQLite (collections)
 *
 * Example:
 * ```kotlin
 * // Create or get default database
 * val db = DatabaseFactory.default()
 *
 * // Key-value operations
 * db.put("username", "john_doe")
 * db.put("theme", "dark")
 * val username = db.getTyped<String>("username")
 *
 * // Collection operations
 * db.createCollection("tasks")
 * val tasks = db.getCollection("tasks")
 * tasks?.insert(Document(
 *     id = "",
 *     data = mapOf("title" to "Buy milk", "completed" to "false")
 * ))
 * ```
 *
 * @property name The name of the database
 * @property version The database version (for migrations)
 * @since 1.0.0
 */
expect class Database {
    var name: String
    var version: Int

    /**
     * Stores a value associated with a key.
     *
     * Supported types: String, Int, Long, Float, Double, Boolean
     *
     * @param key The key to store the value under
     * @param value The value to store
     */
    fun put(key: String, value: Any?)

    /**
     * Retrieves a value associated with a key.
     *
     * @param key The key to retrieve
     * @return The stored value, or null if not found
     */
    fun get(key: String): Any?

    /**
     * Retrieves a typed value associated with a key.
     *
     * @param T The expected type
     * @param key The key to retrieve
     * @return The stored value cast to type T, or null if not found
     */
    fun <T> getTyped(key: String): T?

    /**
     * Removes a key-value pair from the database.
     *
     * @param key The key to remove
     */
    fun remove(key: String)

    /**
     * Removes all key-value pairs from the database.
     *
     * Note: This does not affect collections.
     */
    fun clear()

    /**
     * Checks if a key exists in the database.
     *
     * @param key The key to check
     * @return true if the key exists, false otherwise
     */
    fun containsKey(key: String): Boolean

    /**
     * Returns all keys in the database.
     *
     * @return Set of all keys
     */
    fun keys(): Set<String>

    /**
     * Creates a new collection with an optional schema.
     *
     * @param name The name of the collection
     * @param schema Optional schema defining field types
     */
    fun createCollection(name: String, schema: CollectionSchema? = null)

    /**
     * Retrieves a collection by name.
     *
     * @param name The name of the collection
     * @return The collection if it exists, null otherwise
     */
    fun getCollection(name: String): Collection?

    /**
     * Deletes a collection and all its documents.
     *
     * @param name The name of the collection to drop
     */
    fun dropCollection(name: String)

    /**
     * Lists all collection names in the database.
     *
     * @return List of collection names
     */
    fun listCollections(): List<String>

    /**
     * Opens the database connection.
     *
     * This is typically called automatically when accessing the database,
     * but can be called explicitly for initialization.
     */
    fun open()

    /**
     * Closes the database connection.
     *
     * After closing, the database should not be used until opened again.
     */
    fun close()

    /**
     * Forces all pending writes to disk.
     *
     * Ensures data persistence even in case of unexpected shutdown.
     */
    fun flush()
}
