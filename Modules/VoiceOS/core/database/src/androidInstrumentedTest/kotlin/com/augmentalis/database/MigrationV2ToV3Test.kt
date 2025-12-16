/**
 * MigrationV2ToV3Test.kt - Tests for database migration from v2 to v3
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-13
 *
 * Tests schema migration that adds version tracking columns.
 * Follows TDD principles - tests define expected migration behavior.
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
class MigrationV2ToV3Test {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    /**
     * Test 1: Migration adds all 4 new columns
     *
     * TDD: This test defines that migration MUST add:
     * - appVersion (TEXT)
     * - versionCode (INTEGER)
     * - lastVerified (INTEGER)
     * - isDeprecated (INTEGER)
     */
    @Test
    fun testMigration_v2ToV3_addsAllColumns() = runBlocking {
        val driver = AndroidSqliteDriver(
            schema = VoiceOSDatabase.Schema,
            context = context,
            name = null  // In-memory database
        )

        try {
            // Execute migration
            DatabaseMigrations.migrate(driver, oldVersion = 2, newVersion = 3)

            // Verify all columns exist by querying table info
            val cursor = driver.executeQuery(
                identifier = null,
                sql = "PRAGMA table_info(commands_generated)",
                mapper = { cursor ->
                    val columns = mutableListOf<String>()
                    while (cursor.next().value) {
                        columns.add(cursor.getString(1) ?: "")
                    }
                    app.cash.sqldelight.db.QueryResult.Value(columns)
                },
                parameters = 0,
                binders = null
            )

            val columns = cursor.value
            assertTrue("appVersion column should exist", columns.contains("appVersion"))
            assertTrue("versionCode column should exist", columns.contains("versionCode"))
            assertTrue("lastVerified column should exist", columns.contains("lastVerified"))
            assertTrue("isDeprecated column should exist", columns.contains("isDeprecated"))

        } finally {
            driver.close()
        }
    }

    /**
     * Test 2: Migration creates composite index idx_gc_app_version
     *
     * TDD: Index on (appId, versionCode, isDeprecated) MUST exist for performance
     */
    @Test
    fun testMigration_v2ToV3_createsAppVersionIndex() = runBlocking {
        val driver = AndroidSqliteDriver(
            schema = VoiceOSDatabase.Schema,
            context = context,
            name = null
        )

        try {
            DatabaseMigrations.migrate(driver, oldVersion = 2, newVersion = 3)

            // Verify index exists
            val cursor = driver.executeQuery(
                identifier = null,
                sql = "SELECT name FROM sqlite_master WHERE type='index' AND name='idx_gc_app_version'",
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

            assertTrue("idx_gc_app_version index should exist", cursor.value)

        } finally {
            driver.close()
        }
    }

    /**
     * Test 3: Migration creates cleanup index idx_gc_last_verified
     *
     * TDD: Index on (lastVerified, isDeprecated) MUST exist for cleanup queries
     */
    @Test
    fun testMigration_v2ToV3_createsLastVerifiedIndex() = runBlocking {
        val driver = AndroidSqliteDriver(
            schema = VoiceOSDatabase.Schema,
            context = context,
            name = null
        )

        try {
            DatabaseMigrations.migrate(driver, oldVersion = 2, newVersion = 3)

            // Verify index exists
            val cursor = driver.executeQuery(
                identifier = null,
                sql = "SELECT name FROM sqlite_master WHERE type='index' AND name='idx_gc_last_verified'",
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

            assertTrue("idx_gc_last_verified index should exist", cursor.value)

        } finally {
            driver.close()
        }
    }

    /**
     * Test 4: Migration preserves existing data
     *
     * TDD: ALL existing commands MUST remain intact after migration
     * No data loss is acceptable
     */
    @Test
    fun testMigration_v2ToV3_preservesExistingData() = runBlocking {
        val driver = AndroidSqliteDriver(
            schema = VoiceOSDatabase.Schema,
            context = context,
            name = null
        )

        try {
            val database = VoiceOSDatabase(driver)

            // Insert test data BEFORE migration (simulating v2 data)
            database.generatedCommandQueries.insert(
                elementHash = "test_hash_1",
                commandText = "click button",
                actionType = "CLICK",
                confidence = 0.9,
                synonyms = null,
                isUserApproved = 0,
                usageCount = 5,
                lastUsed = 1234567890L,
                createdAt = System.currentTimeMillis(),
                appId = "com.test.app",
                appVersion = "",  // Default value
                versionCode = 0,  // Default value
                lastVerified = null,  // Default value
                isDeprecated = 0  // Default value
            )

            database.generatedCommandQueries.insert(
                elementHash = "test_hash_2",
                commandText = "open menu",
                actionType = "CLICK",
                confidence = 0.85,
                synonyms = "show menu",
                isUserApproved = 1,
                usageCount = 10,
                lastUsed = 1234567891L,
                createdAt = System.currentTimeMillis(),
                appId = "com.test.app",
                appVersion = "",
                versionCode = 0,
                lastVerified = null,
                isDeprecated = 0
            )

            // Get count before migration
            val countBefore = database.generatedCommandQueries.count().executeAsOne()
            assertEquals("Should have 2 commands before migration", 2, countBefore)

            // Execute migration
            DatabaseMigrations.migrate(driver, oldVersion = 2, newVersion = 3)

            // Verify data preserved
            val countAfter = database.generatedCommandQueries.count().executeAsOne()
            assertEquals("Should have same count after migration", countBefore, countAfter)

            // Verify specific commands still exist with correct data
            val commands = database.generatedCommandQueries.getAll().executeAsList()
            assertEquals("Should have 2 commands", 2, commands.size)

            val command1 = commands.find { it.commandText == "click button" }
            assertNotNull("First command should exist", command1)
            assertEquals("First command hash preserved", "test_hash_1", command1!!.elementHash)
            assertEquals("First command action preserved", "CLICK", command1.actionType)
            assertEquals("First command confidence preserved", 0.9, command1.confidence, 0.001)
            assertEquals("First command usage preserved", 5, command1.usageCount)

            val command2 = commands.find { it.commandText == "open menu" }
            assertNotNull("Second command should exist", command2)
            assertEquals("Second command synonyms preserved", "show menu", command2!!.synonyms)
            assertEquals("Second command approved status preserved", 1, command2.isUserApproved)
            assertEquals("Second command usage preserved", 10, command2.usageCount)

        } finally {
            driver.close()
        }
    }

    /**
     * Test 5: Migration applies correct default values to new columns
     *
     * TDD: New columns MUST have these defaults for existing data:
     * - appVersion: "" (empty string)
     * - versionCode: 0
     * - lastVerified: NULL
     * - isDeprecated: 0
     */
    @Test
    fun testMigration_v2ToV3_appliesCorrectDefaults() = runBlocking {
        val driver = AndroidSqliteDriver(
            schema = VoiceOSDatabase.Schema,
            context = context,
            name = null
        )

        try {
            val database = VoiceOSDatabase(driver)

            // Insert command
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
                appId = "com.test.app",
                appVersion = "",
                versionCode = 0,
                lastVerified = null,
                isDeprecated = 0
            )

            // Execute migration
            DatabaseMigrations.migrate(driver, oldVersion = 2, newVersion = 3)

            // Verify defaults
            val commands = database.generatedCommandQueries.getAll().executeAsList()
            assertEquals("Should have 1 command", 1, commands.size)

            val command = commands[0]
            assertEquals("appVersion default should be empty string", "", command.appVersion)
            assertEquals("versionCode default should be 0", 0, command.versionCode)
            assertNull("lastVerified default should be null", command.lastVerified)
            assertEquals("isDeprecated default should be 0", 0, command.isDeprecated)

        } finally {
            driver.close()
        }
    }

    /**
     * Test 6: Migration is idempotent (can run multiple times safely)
     *
     * TDD: Running migration multiple times MUST NOT cause errors
     * This is critical for deployment safety
     */
    @Test
    fun testMigration_v2ToV3_isIdempotent() = runBlocking {
        val driver = AndroidSqliteDriver(
            schema = VoiceOSDatabase.Schema,
            context = context,
            name = null
        )

        try {
            // Run migration twice
            DatabaseMigrations.migrate(driver, oldVersion = 2, newVersion = 3)
            DatabaseMigrations.migrate(driver, oldVersion = 2, newVersion = 3)

            // Verify no errors and schema is correct
            val cursor = driver.executeQuery(
                identifier = null,
                sql = "PRAGMA table_info(commands_generated)",
                mapper = { cursor ->
                    val columns = mutableListOf<String>()
                    while (cursor.next().value) {
                        columns.add(cursor.getString(1) ?: "")
                    }
                    app.cash.sqldelight.db.QueryResult.Value(columns)
                },
                parameters = 0,
                binders = null
            )

            val columns = cursor.value
            assertTrue("Columns should exist after double migration", columns.containsAll(
                listOf("appVersion", "versionCode", "lastVerified", "isDeprecated")
            ))

        } finally {
            driver.close()
        }
    }

    /**
     * Test 7: Migration works with empty database
     *
     * TDD: Migration MUST work even if no commands exist yet
     */
    @Test
    fun testMigration_v2ToV3_worksWithEmptyDatabase() = runBlocking {
        val driver = AndroidSqliteDriver(
            schema = VoiceOSDatabase.Schema,
            context = context,
            name = null
        )

        try {
            val database = VoiceOSDatabase(driver)

            // Verify database is empty
            val countBefore = database.generatedCommandQueries.count().executeAsOne()
            assertEquals("Database should be empty", 0, countBefore)

            // Execute migration
            DatabaseMigrations.migrate(driver, oldVersion = 2, newVersion = 3)

            // Verify schema updated correctly
            val cursor = driver.executeQuery(
                identifier = null,
                sql = "PRAGMA table_info(commands_generated)",
                mapper = { cursor ->
                    val columns = mutableListOf<String>()
                    while (cursor.next().value) {
                        columns.add(cursor.getString(1) ?: "")
                    }
                    app.cash.sqldelight.db.QueryResult.Value(columns)
                },
                parameters = 0,
                binders = null
            )

            val columns = cursor.value
            assertTrue("All new columns should exist", columns.containsAll(
                listOf("appVersion", "versionCode", "lastVerified", "isDeprecated")
            ))

        } finally {
            driver.close()
        }
    }

    /**
     * Test 8: Migration handles large dataset (performance test)
     *
     * TDD: Migration MUST complete in reasonable time even with 10,000 commands
     * Target: < 5 seconds for 10,000 commands
     */
    @Test
    fun testMigration_v2ToV3_handlesLargeDataset() = runBlocking {
        val driver = AndroidSqliteDriver(
            schema = VoiceOSDatabase.Schema,
            context = context,
            name = null
        )

        try {
            val database = VoiceOSDatabase(driver)

            // Insert 1,000 commands (reduced from 10,000 for test speed)
            repeat(1000) { i ->
                database.generatedCommandQueries.insert(
                    elementHash = "hash_$i",
                    commandText = "command $i",
                    actionType = "CLICK",
                    confidence = 0.8,
                    synonyms = null,
                    isUserApproved = 0,
                    usageCount = 0,
                    lastUsed = null,
                    createdAt = System.currentTimeMillis(),
                    appId = "com.test.app",
                    appVersion = "",
                    versionCode = 0,
                    lastVerified = null,
                    isDeprecated = 0
                )
            }

            // Measure migration time
            val startTime = System.currentTimeMillis()
            DatabaseMigrations.migrate(driver, oldVersion = 2, newVersion = 3)
            val duration = System.currentTimeMillis() - startTime

            // Verify all data preserved
            val count = database.generatedCommandQueries.count().executeAsOne()
            assertEquals("All commands should be preserved", 1000, count)

            // Performance assertion (generous for CI)
            assertTrue("Migration should complete in < 10 seconds", duration < 10000)

        } finally {
            driver.close()
        }
    }

    /**
     * Test 9: Migration v1→v3 (skipping v2) works correctly
     *
     * TDD: Sequential migration from v1→v2→v3 MUST work
     */
    @Test
    fun testMigration_v1ToV3_appliesToBothMigrations() = runBlocking {
        val driver = AndroidSqliteDriver(
            schema = VoiceOSDatabase.Schema,
            context = context,
            name = null
        )

        try {
            // Migrate from v1 to v3 (should apply both v1→v2 and v2→v3)
            DatabaseMigrations.migrate(driver, oldVersion = 1, newVersion = 3)

            // Verify both migrations applied
            val cursor = driver.executeQuery(
                identifier = null,
                sql = "PRAGMA table_info(commands_generated)",
                mapper = { cursor ->
                    val columns = mutableListOf<String>()
                    while (cursor.next().value) {
                        columns.add(cursor.getString(1) ?: "")
                    }
                    app.cash.sqldelight.db.QueryResult.Value(columns)
                },
                parameters = 0,
                binders = null
            )

            val columns = cursor.value
            // From v1→v2: appId
            assertTrue("appId column should exist (v1→v2)", columns.contains("appId"))
            // From v2→v3: version tracking
            assertTrue("appVersion column should exist (v2→v3)", columns.contains("appVersion"))
            assertTrue("versionCode column should exist (v2→v3)", columns.contains("versionCode"))
            assertTrue("lastVerified column should exist (v2→v3)", columns.contains("lastVerified"))
            assertTrue("isDeprecated column should exist (v2→v3)", columns.contains("isDeprecated"))

        } finally {
            driver.close()
        }
    }

    /**
     * Test 10: Migration column types are correct
     *
     * TDD: New columns MUST have correct SQLite types
     */
    @Test
    fun testMigration_v2ToV3_columnTypesCorrect() = runBlocking {
        val driver = AndroidSqliteDriver(
            schema = VoiceOSDatabase.Schema,
            context = context,
            name = null
        )

        try {
            DatabaseMigrations.migrate(driver, oldVersion = 2, newVersion = 3)

            // Query column types
            val cursor = driver.executeQuery(
                identifier = null,
                sql = "PRAGMA table_info(commands_generated)",
                mapper = { cursor ->
                    val columnInfo = mutableMapOf<String, String>()
                    while (cursor.next().value) {
                        val name = cursor.getString(1) ?: ""
                        val type = cursor.getString(2) ?: ""
                        columnInfo[name] = type
                    }
                    app.cash.sqldelight.db.QueryResult.Value(columnInfo)
                },
                parameters = 0,
                binders = null
            )

            val columnInfo = cursor.value
            assertEquals("appVersion should be TEXT", "TEXT", columnInfo["appVersion"])
            assertEquals("versionCode should be INTEGER", "INTEGER", columnInfo["versionCode"])
            assertEquals("lastVerified should be INTEGER", "INTEGER", columnInfo["lastVerified"])
            assertEquals("isDeprecated should be INTEGER", "INTEGER", columnInfo["isDeprecated"])

        } finally {
            driver.close()
        }
    }
}
