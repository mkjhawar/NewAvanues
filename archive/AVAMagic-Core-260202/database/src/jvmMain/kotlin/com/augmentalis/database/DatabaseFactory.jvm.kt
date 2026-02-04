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

        // CRITICAL: Enable foreign key constraints
        // FIX (2025-12-19): Added to enforce referential integrity
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)

        return driver
    }

    /**
     * Create driver with persistent database file.
     */
    fun createPersistentDriver(path: String): SqlDriver {
        val driver = JdbcSqliteDriver("jdbc:sqlite:$path")
        VoiceOSDatabase.Schema.create(driver)

        // CRITICAL: Enable foreign key constraints
        // FIX (2025-12-19): Added to enforce referential integrity
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)

        return driver
    }
}
