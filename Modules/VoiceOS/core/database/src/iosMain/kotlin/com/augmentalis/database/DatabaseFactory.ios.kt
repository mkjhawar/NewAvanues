/**
 * DatabaseFactory.ios.kt - iOS SQLite driver
 *
 * Actual implementation for iOS platform.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/**
 * iOS implementation of DatabaseDriverFactory.
 */
actual class DatabaseDriverFactory {

    actual fun createDriver(): SqlDriver {
        val driver = NativeSqliteDriver(
            schema = VoiceOSDatabase.Schema,
            name = "voiceos.db"
        )

        // CRITICAL: Enable foreign key constraints
        // FIX (2025-12-19): Added to enforce referential integrity
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)

        return driver
    }
}
