/**
 * DatabaseFactory.jvm.kt - JVM SQLite driver
 *
 * Actual implementation for JVM/Desktop platform.
 * Used for testing and desktop applications.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

/**
 * JVM implementation of DatabaseDriverFactory.
 */
actual class DatabaseDriverFactory {

    actual fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VoiceOSDatabase.Schema.create(driver)
        return driver
    }

    /**
     * Create driver with persistent database file.
     */
    fun createPersistentDriver(path: String): SqlDriver {
        val driver = JdbcSqliteDriver("jdbc:sqlite:$path")
        VoiceOSDatabase.Schema.create(driver)
        return driver
    }
}
