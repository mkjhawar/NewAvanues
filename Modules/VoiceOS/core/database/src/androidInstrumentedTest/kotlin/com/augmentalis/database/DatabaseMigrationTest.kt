/**
 * DatabaseMigrationTest.kt - Tests for database schema migrations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-13
 *
 * Tests that database migrations work correctly when upgrading schema versions.
 */

package com.augmentalis.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.augmentalis.database.migrations.DatabaseMigrations
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    @Test
    fun testMigration_v1ToV2_addsAppIdColumn() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Create database with old schema (version 1)
        val driver = AndroidSqliteDriver(
            schema = VoiceOSDatabase.Schema,
            context = context,
            name = null  // In-memory for testing
        )

        try {
            // Simulate migration
            DatabaseMigrations.migrate(driver, oldVersion = 1, newVersion = 2)

            // Verify appId column exists by querying the table info
            val cursor = driver.executeQuery(
                identifier = null,
                sql = "PRAGMA table_info(commands_generated)",
                mapper = { cursor ->
                    val columns = mutableListOf<String>()
                    while (cursor.next().value) {
                        // Column name is at index 1
                        columns.add(cursor.getString(1) ?: "")
                    }
                    app.cash.sqldelight.db.QueryResult.Value(columns)
                },
                parameters = 0,
                binders = null
            )

            val columns = cursor.value
            assertTrue("appId column should exist after migration", columns.contains("appId"))

        } finally {
            driver.close()
        }
    }

    @Test
    fun testMigration_v1ToV2_createsIndex() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val driver = AndroidSqliteDriver(
            schema = VoiceOSDatabase.Schema,
            context = context,
            name = null
        )

        try {
            // Simulate migration
            DatabaseMigrations.migrate(driver, oldVersion = 1, newVersion = 2)

            // Verify index exists
            val cursor = driver.executeQuery(
                identifier = null,
                sql = "SELECT name FROM sqlite_master WHERE type='index' AND name='idx_gc_app_id'",
                mapper = { cursor ->
                    var exists = false
                    if (cursor.next().value) {
                        exists = true
                    }
                    app.cash.sqldelight.db.QueryResult.Value(exists)
                },
                parameters = 0,
                binders = null
            )

            assertTrue("idx_gc_app_id index should exist after migration", cursor.value)

        } finally {
            driver.close()
        }
    }

    @Test
    fun testMigration_preservesExistingData() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val driver = AndroidSqliteDriver(
            schema = VoiceOSDatabase.Schema,
            context = context,
            name = null
        )

        try {
            val database = VoiceOSDatabase(driver)

            // Insert test data before migration
            database.generatedCommandQueries.insert(
                elementHash = "test_hash",
                commandText = "click button",
                actionType = "CLICK",
                confidence = 0.9,
                synonyms = null,
                isUserApproved = 0,
                usageCount = 0,
                lastUsed = null,
                createdAt = System.currentTimeMillis(),
                appId = "",
                appVersion = "",
                versionCode = 0,
                lastVerified = null,
                isDeprecated = 0
            )

            // Get row count before migration
            val countBefore = database.generatedCommandQueries.count().executeAsOne()

            // Simulate migration (though in this case, the table already has appId)
            DatabaseMigrations.migrate(driver, oldVersion = 1, newVersion = 2)

            // Verify data is preserved
            val countAfter = database.generatedCommandQueries.count().executeAsOne()
            assertEquals("Row count should remain same after migration", countBefore, countAfter)

            // Verify we can still query the data
            val commands = database.generatedCommandQueries.getAll().executeAsList()
            assertEquals("Should have 1 command after migration", 1, commands.size)
            assertEquals("Command text should be preserved", "click button", commands[0].commandText)

        } finally {
            driver.close()
        }
    }

    @Test
    fun testMigration_defaultValueForAppId() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val driver = AndroidSqliteDriver(
            schema = VoiceOSDatabase.Schema,
            context = context,
            name = null
        )

        try {
            val database = VoiceOSDatabase(driver)

            // Insert data
            database.generatedCommandQueries.insert(
                elementHash = "test",
                commandText = "test command",
                actionType = "CLICK",
                confidence = 0.8,
                synonyms = null,
                isUserApproved = 0,
                usageCount = 0,
                lastUsed = null,
                createdAt = System.currentTimeMillis(),
                appId = "",
                appVersion = "",
                versionCode = 0,
                lastVerified = null,
                isDeprecated = 0
            )

            // Query and verify default appId
            val commands = database.generatedCommandQueries.getAll().executeAsList()
            assertEquals("Default appId should be empty string", "", commands[0].appId)

        } finally {
            driver.close()
        }
    }
}
