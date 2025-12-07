/**
 * TestDatabaseFactory.kt - Test database factory for SQLDelight
 *
 * Provides in-memory database instances for unit testing.
 * Uses JdbcSqliteDriver for JVM-based tests.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-11-26
 */

package com.augmentalis.voiceoscore.test.infrastructure

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.augmentalis.database.VoiceOSDatabase

/**
 * Factory for creating test database instances.
 *
 * All databases are in-memory for fast test execution.
 */
object TestDatabaseFactory {

    /**
     * Create a new in-memory database with schema initialized.
     */
    fun createInMemoryDatabase(): VoiceOSDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        VoiceOSDatabase.Schema.create(driver)
        return VoiceOSDatabase(driver)
    }

    /**
     * Create a clean database with all tables empty.
     * Useful for tests that need a fresh state.
     *
     * Note: Since we're using in-memory databases, each new instance
     * is already clean. This method exists for API consistency.
     */
    fun createCleanDatabase(): VoiceOSDatabase {
        return createInMemoryDatabase()
    }
}
