package com.augmentalis.magicdata

import android.content.Context

/**
 * Android implementation of DatabaseFactory.
 *
 * Requires Android Context to create SharedPreferences-backed databases.
 *
 * @since 1.0.0
 */
actual object DatabaseFactory {
    private var applicationContext: Context? = null
    private val defaultDatabase by lazy {
        create("voiceos_default", 1)
    }

    /**
     * Initializes the factory with application context.
     *
     * This must be called before using create() or default().
     *
     * @param context Android application context
     */
    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    /**
     * Creates a new database instance with the specified name and version.
     *
     * @param name The name of the database
     * @param version The database version (default: 1)
     * @return A new Database instance
     * @throws IllegalStateException if initialize() was not called
     */
    actual fun create(name: String, version: Int): Database {
        val context = applicationContext
            ?: throw IllegalStateException("DatabaseFactory not initialized. Call initialize(context) first.")
        return Database(context, name, version)
    }

    /**
     * Returns the default application database.
     *
     * @return The default Database instance
     * @throws IllegalStateException if initialize() was not called
     */
    actual fun default(): Database {
        return defaultDatabase
    }
}
