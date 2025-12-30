package com.augmentalis.magicdata

/**
 * Factory object for creating Database instances.
 *
 * Provides platform-specific database implementations with convenient factory methods.
 *
 * Example:
 * ```kotlin
 * // Get the default database
 * val db = DatabaseFactory.default()
 *
 * // Create a named database with versioning
 * val customDb = DatabaseFactory.create("my_app_db", version = 2)
 * ```
 *
 * @since 1.0.0
 */
expect object DatabaseFactory {
    /**
     * Creates a new database instance with the specified name and version.
     *
     * @param name The name of the database
     * @param version The database version (default: 1)
     * @return A new Database instance
     */
    fun create(name: String, version: Int = 1): Database

    /**
     * Returns the default application database.
     *
     * This is a singleton instance named "voiceos_default" with version 1.
     *
     * @return The default Database instance
     */
    fun default(): Database
}

/**
 * Builder class for constructing Database instances with a fluent API.
 *
 * Example:
 * ```kotlin
 * val db = DatabaseBuilder()
 *     .withName("tasks_db")
 *     .withVersion(3)
 *     .build()
 * ```
 *
 * @since 1.0.0
 */
class DatabaseBuilder {
    private var name: String = "default"
    private var version: Int = 1

    /**
     * Sets the database name.
     *
     * @param name The database name
     * @return This builder for chaining
     */
    fun withName(name: String): DatabaseBuilder {
        this.name = name
        return this
    }

    /**
     * Sets the database version.
     *
     * @param version The database version
     * @return This builder for chaining
     */
    fun withVersion(version: Int): DatabaseBuilder {
        this.version = version
        return this
    }

    /**
     * Builds the final Database object.
     *
     * @return A new Database instance
     */
    fun build(): Database {
        return DatabaseFactory.create(name, version)
    }
}
