/**
 * DatabaseFactory.ios.kt - iOS SQLite driver
 *
 * Actual implementation for iOS platform using Native SQLite driver.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

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
 */
actual class DatabaseDriverFactory {

    actual fun createDriver(): SqlDriver {
        val driver = NativeSqliteDriver(
            schema = VoiceOSDatabase.Schema,
            name = "voiceos.db"
        )

        // Configure SQLite pragmas (matching Android configuration)
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        driver.execute(null, "PRAGMA busy_timeout = 30000", 0)
        driver.execute(null, "PRAGMA journal_mode = WAL", 0)

        return driver
    }
}
