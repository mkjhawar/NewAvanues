/**
 * DatabaseFactory.desktop.kt - Desktop (JVM) SQLite driver
 *
 * Actual implementation for Desktop/JVM platform using SQLite JDBC.
 *
 * ## Migration Strategy
 * JdbcSqliteDriver does not have an automatic onUpgrade mechanism like Android.
 * We manage upgrades manually:
 *   - New database: call Schema.create() and record version 7 in user_version
 *   - Existing database: read user_version, call DatabaseMigrations.migrate() for
 *     any missed versions, then update user_version to CURRENT_SCHEMA_VERSION
 *
 * To add a future migration (e.g., v7 → v8):
 *   1. Add migrateV7ToV8() in DatabaseMigrations.kt
 *   2. Wire it in DatabaseMigrations.migrate()
 *   3. Bump CURRENT_SCHEMA_VERSION to 8
 *   No changes needed in this file.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.augmentalis.database.migrations.CURRENT_SCHEMA_VERSION
import com.augmentalis.database.migrations.DatabaseMigrations
import java.io.File

/**
 * Desktop (JVM) implementation of DatabaseDriverFactory.
 *
 * Uses JDBC SQLite driver for desktop platforms.
 * Database file is stored in user's home directory under .voiceos/
 *
 * FIX (2026-02-22): Added incremental migration support. Previously a new install
 * always ran Schema.create() (correct), but an existing database was never upgraded
 * (DatabaseMigrations.migrate() was dead code on desktop). The driver now reads
 * SQLite's user_version pragma and applies any missed migrations before use.
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

        // Configure SQLite pragmas for better performance and concurrency
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        driver.execute(null, "PRAGMA busy_timeout = 30000", 0)
        driver.execute(null, "PRAGMA journal_mode = WAL", 0)

        if (isNewDb) {
            // Fresh database: create full schema at current version and record the version
            VoiceOSDatabase.Schema.create(driver)
            driver.execute(null, "PRAGMA user_version = $CURRENT_SCHEMA_VERSION", 0)
        } else {
            // Existing database: read stored version and apply any missed migrations
            val storedVersion = driver.executeQuery(
                identifier = null,
                sql = "PRAGMA user_version",
                mapper = { cursor ->
                    QueryResult.Value(if (cursor.next().value) cursor.getLong(0) ?: 1L else 1L)
                },
                parameters = 0,
                binders = null
            ).value

            if (storedVersion < CURRENT_SCHEMA_VERSION) {
                DatabaseMigrations.migrate(driver, storedVersion, CURRENT_SCHEMA_VERSION)
                driver.execute(null, "PRAGMA user_version = $CURRENT_SCHEMA_VERSION", 0)
            }
        }

        return driver
    }
}
