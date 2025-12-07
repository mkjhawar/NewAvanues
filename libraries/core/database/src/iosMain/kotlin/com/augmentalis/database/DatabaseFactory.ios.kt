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
        return NativeSqliteDriver(
            schema = VoiceOSDatabase.Schema,
            name = "voiceos.db"
        )
    }
}
