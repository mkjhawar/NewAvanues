/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * This file is part of AVA AI and is proprietary software.
 * See LICENSE file in the project root for license information.
 */

package com.augmentalis.ava.core.data.db

import app.cash.sqldelight.db.SqlDriver

/**
 * Factory for creating SQLDelight database drivers
 *
 * Platform-specific implementations provide the appropriate driver:
 * - Android: AndroidSqliteDriver
 * - iOS: NativeSqliteDriver
 * - Desktop: JdbcSqliteDriver
 */
expect class DatabaseDriverFactory {
    /**
     * Create platform-specific SQLite driver
     *
     * @return Platform-appropriate SQLDelight driver for AVADatabase
     */
    fun createDriver(): SqlDriver
}

/**
 * Create AVADatabase instance from driver
 *
 * Extension function for convenient database creation.
 * SQLDelight handles kotlin.Boolean ↔ INTEGER conversion automatically.
 */
fun SqlDriver.createDatabase(): AVADatabase {
    return AVADatabase(this)
}
