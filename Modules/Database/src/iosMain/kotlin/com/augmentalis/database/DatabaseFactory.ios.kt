/**
 * DatabaseFactory.ios.kt - iOS SQLite driver
 *
 * Actual implementation for iOS platform using Native SQLite driver.
 *
 * ## Migration Strategy
 * NativeSqliteDriver compares the stored SQLite user_version against Schema.version.
 * If they differ, it calls Schema.migrate(driver, oldVersion, newVersion) automatically.
 * By passing MigratedSchema (which reports version=CURRENT_SCHEMA_VERSION=7 and
 * delegates migrate() to DatabaseMigrations.migrate()), existing iOS databases are
 * upgraded incrementally on the next open. New installs get the full schema at v7.
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

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.augmentalis.database.migrations.MigratedSchema

/**
 * iOS implementation of DatabaseDriverFactory.
 *
 * Uses SQLDelight Native driver backed by SQLiter.
 * Database file is stored in the app's documents directory automatically.
 *
 * Configures SQLite pragmas after creation for consistency with Android:
 * - foreign_keys: ON for referential integrity
 * - busy_timeout: 30 seconds for concurrent write contention
 * - WAL mode: Write-Ahead Logging for better concurrency
 *
 * FIX (2026-02-22): Replaced VoiceOSDatabase.Schema with MigratedSchema so
 * NativeSqliteDriver detects the real schema version (7) and calls migrate()
 * on existing databases that were created at version 1.
 */
actual class DatabaseDriverFactory {

    actual fun createDriver(): SqlDriver {
        val driver = NativeSqliteDriver(
            schema = MigratedSchema(VoiceOSDatabase.Schema),
            name = "voiceos.db"
        )

        // Configure SQLite pragmas (matching Android configuration)
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        driver.execute(null, "PRAGMA busy_timeout = 30000", 0)
        driver.execute(null, "PRAGMA journal_mode = WAL", 0)

        return driver
    }
}
