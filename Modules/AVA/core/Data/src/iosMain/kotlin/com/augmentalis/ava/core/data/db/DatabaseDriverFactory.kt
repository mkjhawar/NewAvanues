/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * This file is part of AVA AI and is proprietary software.
 * See LICENSE file in the project root for license information.
 */

package com.augmentalis.ava.core.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/**
 * iOS implementation of DatabaseDriverFactory
 *
 * Provides iOS-specific SQLite driver implementation using NativeSqliteDriver.
 */
actual class DatabaseDriverFactory {

    /**
     * Create iOS SQLite driver
     *
     * @return iOS SQLDelight driver for AVADatabase
     */
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = AVADatabase.Schema,
            name = "ava_database.db"
        )
    }
}
