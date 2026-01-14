/**
 * DatabaseMaintenanceTest.kt - Tests for database maintenance operations
 *
 * Tests VACUUM, integrity checks, and database info methods.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database

import kotlinx.coroutines.runBlocking
import kotlin.test.*

class DatabaseMaintenanceTest {

    private lateinit var databaseManager: VoiceOSDatabaseManager

    @BeforeTest
    fun setup() {
        val factory = DatabaseDriverFactory()
        databaseManager = VoiceOSDatabaseManager.getInstance(factory)
    }

    @Test
    fun testVacuum() = runBlocking {
        // VACUUM should complete without errors
        databaseManager.vacuum()
        // If we reach here, VACUUM succeeded
        assertTrue(true)
    }

    @Test
    fun testIntegrityCheck() = runBlocking {
        // Fresh database should be healthy
        val isHealthy = databaseManager.checkIntegrity()
        assertTrue(isHealthy, "Database integrity check should return true for healthy database")
    }

    @Test
    fun testIntegrityReport() = runBlocking {
        // Get detailed integrity report
        val report = databaseManager.getIntegrityReport()

        // Report should not be empty
        assertTrue(report.isNotEmpty(), "Integrity report should contain at least one result")

        // For a healthy database, first result should be "ok"
        assertEquals("ok", report[0], "Healthy database should report 'ok'")
    }

    @Test
    fun testDatabaseInfo() = runBlocking {
        // Get database statistics
        val info = databaseManager.getDatabaseInfo()

        // Verify all fields are populated with reasonable values
        assertTrue(info.totalPages > 0, "Database should have at least 1 page")
        assertTrue(info.pageSize > 0, "Page size should be greater than 0")
        assertTrue(info.totalSize > 0, "Total size should be greater than 0")
        assertTrue(info.unusedPages >= 0, "Unused pages should be non-negative")
        assertTrue(info.unusedSize >= 0, "Unused size should be non-negative")

        // Verify calculation is correct
        assertEquals(info.totalPages * info.pageSize, info.totalSize,
            "Total size should equal totalPages * pageSize")
        assertEquals(info.unusedPages * info.pageSize, info.unusedSize,
            "Unused size should equal unusedPages * pageSize")
    }

    @Test
    fun testVacuumReducesUnusedSpace() = runBlocking {
        // Insert and delete some data to create unused space
        val queries = databaseManager.commandHistoryQueries
        val now = System.currentTimeMillis()

        // Insert 100 records
        repeat(100) { i ->
            queries.insert("cmd$i", null, 0.9, now, "en", "Vosk", 1, 100, 1, "VOICE")
        }

        // Delete them all
        queries.deleteAll()

        // Get unused space before VACUUM
        val beforeVacuum = databaseManager.getDatabaseInfo()

        // Run VACUUM
        databaseManager.vacuum()

        // Get unused space after VACUUM
        val afterVacuum = databaseManager.getDatabaseInfo()

        // After VACUUM, unused space should be reduced or remain the same
        assertTrue(afterVacuum.unusedSize <= beforeVacuum.unusedSize,
            "VACUUM should reduce or maintain unused space")
    }

    @Test
    fun testIntegrityCheckAfterVacuum() = runBlocking {
        // VACUUM should not corrupt the database
        databaseManager.vacuum()

        val isHealthy = databaseManager.checkIntegrity()
        assertTrue(isHealthy, "Database should remain healthy after VACUUM")
    }
}
