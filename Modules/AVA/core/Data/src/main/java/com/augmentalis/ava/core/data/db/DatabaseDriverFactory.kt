/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * This file is part of AVA AI and is proprietary software.
 * See LICENSE file in the project root for license information.
 */

package com.augmentalis.ava.core.data.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

/**
 * Factory for creating SQLDelight database drivers
 *
 * Provides Android-specific SQLite driver implementation.
 * Future: Can be converted to expect/actual for KMP support (iOS, Desktop)
 */
class DatabaseDriverFactory(private val context: Context) {

    /**
     * Create Android SQLite driver
     *
     * @return Android SQLDelight driver for AVADatabase
     */
    fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = AVADatabase.Schema,
            context = context,
            name = "ava_database.db"
        )
    }
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
