/**
 * DatabaseFactory.kt - Platform-agnostic database factory
 *
 * Expect declaration for creating SQLDelight drivers.
 * Actual implementations in androidMain, iosMain, jvmMain.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database

import app.cash.sqldelight.db.SqlDriver

/**
 * Factory for creating platform-specific SQLite drivers.
 */
expect class DatabaseDriverFactory {
    /**
     * Create the SQLite driver for the current platform.
     */
    fun createDriver(): SqlDriver
}

/**
 * Create the VoiceOS database with all adapters configured.
 */
fun createDatabase(driverFactory: DatabaseDriverFactory): VoiceOSDatabase {
    val driver = driverFactory.createDriver()
    return VoiceOSDatabase(driver)
}
