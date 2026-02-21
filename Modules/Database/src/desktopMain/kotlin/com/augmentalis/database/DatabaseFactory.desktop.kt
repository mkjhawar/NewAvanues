/**
 * DatabaseFactory.desktop.kt - Desktop (JVM) SQLite driver
 *
 * Actual implementation for Desktop/JVM platform using SQLite JDBC.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

/**
 * Desktop (JVM) implementation of DatabaseDriverFactory.
 *
 * Uses JDBC SQLite driver for desktop platforms.
 * Database file is stored in user's home directory under .voiceos/
 */
actual class DatabaseDriverFactory {

    actual fun createDriver(): SqlDriver {
        // Create database directory in user home
        val dbDir = File(System.getProperty("user.home"), ".voiceos")
        if (!dbDir.exists()) {
            dbDir.mkdirs()
        }

        val dbFile = File(dbDir, "voiceos.db")
        val isNewDb = !dbFile.exists()
        val driver: SqlDriver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")

        if (isNewDb) {
            VoiceOSDatabase.Schema.create(driver)
        }

        // Configure SQLite pragmas for better performance and concurrency
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        driver.execute(null, "PRAGMA busy_timeout = 30000", 0)
        driver.execute(null, "PRAGMA journal_mode = WAL", 0)

        return driver
    }
}
