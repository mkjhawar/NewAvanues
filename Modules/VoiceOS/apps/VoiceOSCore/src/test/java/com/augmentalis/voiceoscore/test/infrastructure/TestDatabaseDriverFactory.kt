/**
 * TestDatabaseDriverFactory.kt - Test implementation of DatabaseDriverFactory
 *
 * Provides in-memory SQLite driver for testing.
 * Uses JdbcSqliteDriver for JVM-based unit tests.
 *
 * Note: This is a test-only implementation and does not implement
 * the KMP DatabaseDriverFactory interface (which expects platform-specific actuals).
 * Instead, it provides a simple factory for creating test databases.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-11-26
 */

package com.augmentalis.voiceoscore.test.infrastructure

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.augmentalis.database.VoiceOSDatabase

/**
 * Test-only factory for creating in-memory SQLite databases.
 *
 * Creates in-memory databases for fast, isolated testing.
 * Not a platform-specific implementation - JVM only.
 */
class TestDatabaseDriverFactory {

    fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VoiceOSDatabase.Schema.create(driver)
        return driver
    }

    /**
     * Create driver with specific connection URL.
     * Useful for custom test scenarios.
     */
    fun createDriver(url: String): SqlDriver {
        val driver = JdbcSqliteDriver(url)
        VoiceOSDatabase.Schema.create(driver)
        return driver
    }
}
