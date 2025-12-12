/**
 * ScreenContextRepositoryTest.kt - Integration tests for ScreenContextRepository
 *
 * Phase 2 Integration Tests - Task 2.2
 * Tests database persistence using in-memory SQLDelight database.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: VoiceOS-LearnApp-Phase2-Tests-51211-V1.md
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.augmentalis.database.dto.ScreenContextDTO
import com.augmentalis.database.repositories.impl.SQLDelightScreenContextRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for ScreenContextRepository
 *
 * Tests database operations using in-memory SQLDelight database
 * to ensure data persistence, querying, and transaction handling.
 *
 * @since 2.1.0 (Phase 2 Integration Tests)
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
class ScreenContextRepositoryTest {

    private lateinit var driver: SqlDriver
    private lateinit var database: VoiceOSDatabase
    private lateinit var repository: SQLDelightScreenContextRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Create in-memory database for testing
        driver = AndroidSqliteDriver(
            schema = VoiceOSDatabase.Schema,
            context = context,
            name = null // null = in-memory database
        )

        database = VoiceOSDatabase(driver)
        repository = SQLDelightScreenContextRepository(database)
    }

    @After
    fun teardown() {
        driver.close()
    }

    /**
     * Test: insert_NewScreen_PersistsCorrectly
     *
     * Verifies that inserting a new screen context persists
     * correctly and can be retrieved by hash.
     */
    @Test
    fun insert_NewScreen_PersistsCorrectly() = runBlocking {
        // Arrange
        val screenContext = createTestScreenContext(
            screenHash = "screen_hash_001",
            packageName = "com.example.app1",
            appId = "app1",
            activityName = "MainActivity"
        )

        // Act
        repository.insert(screenContext)

        // Assert
        val retrieved = repository.getByHash("screen_hash_001")
        assertNotNull("Retrieved screen should not be null", retrieved)
        assertEquals("Screen hash should match", "screen_hash_001", retrieved?.screenHash)
        assertEquals("Package name should match", "com.example.app1", retrieved?.packageName)
        assertEquals("App ID should match", "app1", retrieved?.appId)
        assertEquals("Activity name should match", "MainActivity", retrieved?.activityName)
        assertEquals("Element count should match", 15L, retrieved?.elementCount)
    }

    /**
     * Test: getByHash_ExistingScreen_ReturnsScreen
     *
     * Verifies that retrieving a screen by hash returns the
     * correct screen context when it exists.
     */
    @Test
    fun getByHash_ExistingScreen_ReturnsScreen() = runBlocking {
        // Arrange
        val screen1 = createTestScreenContext(
            screenHash = "hash_001",
            packageName = "com.example.app1",
            appId = "app1"
        )
        val screen2 = createTestScreenContext(
            screenHash = "hash_002",
            packageName = "com.example.app1",
            appId = "app1"
        )

        repository.insert(screen1)
        repository.insert(screen2)

        // Act
        val retrieved = repository.getByHash("hash_002")

        // Assert
        assertNotNull("Retrieved screen should not be null", retrieved)
        assertEquals("Should retrieve correct screen", "hash_002", retrieved?.screenHash)
    }

    /**
     * Test: getByHash_NonExistentScreen_ReturnsNull
     *
     * Verifies that retrieving a screen by non-existent hash
     * returns null.
     */
    @Test
    fun getByHash_NonExistentScreen_ReturnsNull() = runBlocking {
        // Arrange - empty database

        // Act
        val retrieved = repository.getByHash("non_existent_hash")

        // Assert
        assertNull("Should return null for non-existent hash", retrieved)
    }

    /**
     * Test: getByPackage_MultipleScreens_ReturnsAll
     *
     * Verifies that retrieving screens by package name returns
     * all screens for that package.
     */
    @Test
    fun getByPackage_MultipleScreens_ReturnsAll() = runBlocking {
        // Arrange
        val screen1 = createTestScreenContext(
            screenHash = "hash_001",
            packageName = "com.example.app1",
            appId = "app1",
            activityName = "MainActivity"
        )
        val screen2 = createTestScreenContext(
            screenHash = "hash_002",
            packageName = "com.example.app1",
            appId = "app1",
            activityName = "SettingsActivity"
        )
        val screen3 = createTestScreenContext(
            screenHash = "hash_003",
            packageName = "com.example.app2",
            appId = "app2",
            activityName = "MainActivity"
        )

        repository.insert(screen1)
        repository.insert(screen2)
        repository.insert(screen3)

        // Act
        val app1Screens = repository.getByPackage("com.example.app1")
        val app2Screens = repository.getByPackage("com.example.app2")

        // Assert
        assertEquals("Should return 2 screens for app1", 2, app1Screens.size)
        assertEquals("Should return 1 screen for app2", 1, app2Screens.size)

        val app1Hashes = app1Screens.map { it.screenHash }
        assertTrue("Should contain hash_001", app1Hashes.contains("hash_001"))
        assertTrue("Should contain hash_002", app1Hashes.contains("hash_002"))

        val app2Hashes = app2Screens.map { it.screenHash }
        assertTrue("Should contain hash_003", app2Hashes.contains("hash_003"))
    }

    /**
     * Test: getLearnedHashes_Package_ReturnsHashes
     *
     * Verifies that we can retrieve all learned screen hashes
     * for a specific package (custom query).
     */
    @Test
    fun getLearnedHashes_Package_ReturnsHashes() = runBlocking {
        // Arrange
        val screen1 = createTestScreenContext(
            screenHash = "hash_A",
            packageName = "com.example.testapp",
            appId = "testapp"
        )
        val screen2 = createTestScreenContext(
            screenHash = "hash_B",
            packageName = "com.example.testapp",
            appId = "testapp"
        )
        val screen3 = createTestScreenContext(
            screenHash = "hash_C",
            packageName = "com.example.otherapp",
            appId = "otherapp"
        )

        repository.insert(screen1)
        repository.insert(screen2)
        repository.insert(screen3)

        // Act
        val testappScreens = repository.getByPackage("com.example.testapp")
        val hashes = testappScreens.map { it.screenHash }

        // Assert
        assertEquals("Should return 2 hashes for testapp", 2, hashes.size)
        assertTrue("Should contain hash_A", hashes.contains("hash_A"))
        assertTrue("Should contain hash_B", hashes.contains("hash_B"))
        assertFalse("Should not contain hash_C", hashes.contains("hash_C"))
    }

    /**
     * Test: batchInsert_MultipleScreens_SingleTransaction
     *
     * Verifies that multiple screens can be inserted efficiently
     * and all are persisted correctly.
     */
    @Test
    fun batchInsert_MultipleScreens_SingleTransaction() = runBlocking {
        // Arrange
        val screens = (1..10).map { i ->
            createTestScreenContext(
                screenHash = "batch_hash_$i",
                packageName = "com.example.batchapp",
                appId = "batchapp",
                activityName = "Activity$i"
            )
        }

        // Act
        screens.forEach { screen ->
            repository.insert(screen)
        }

        // Assert
        val allScreens = repository.getByPackage("com.example.batchapp")
        assertEquals("Should have inserted all 10 screens", 10, allScreens.size)

        val hashes = allScreens.map { it.screenHash }
        for (i in 1..10) {
            assertTrue("Should contain batch_hash_$i", hashes.contains("batch_hash_$i"))
        }

        // Verify count
        val count = repository.countByApp("batchapp")
        assertEquals("Count should match inserted screens", 10L, count)
    }

    /**
     * Test: insert_UpdateExisting_ReplacesData
     *
     * Verifies that inserting a screen with the same hash
     * replaces the existing data (upsert behavior).
     */
    @Test
    fun insert_UpdateExisting_ReplacesData() = runBlocking {
        // Arrange
        val originalScreen = createTestScreenContext(
            screenHash = "update_hash",
            packageName = "com.example.updateapp",
            appId = "updateapp",
            activityName = "OriginalActivity",
            elementCount = 10L,
            visitCount = 1L
        )

        repository.insert(originalScreen)

        // Act - Update with same hash but different data
        val updatedScreen = originalScreen.copy(
            activityName = "UpdatedActivity",
            elementCount = 25L,
            visitCount = 5L
        )
        repository.insert(updatedScreen)

        // Assert
        val retrieved = repository.getByHash("update_hash")
        assertNotNull("Updated screen should exist", retrieved)
        assertEquals("Activity name should be updated", "UpdatedActivity", retrieved?.activityName)
        assertEquals("Element count should be updated", 25L, retrieved?.elementCount)
        assertEquals("Visit count should be updated", 5L, retrieved?.visitCount)

        // Verify only one record exists
        val allScreens = repository.getByPackage("com.example.updateapp")
        assertEquals("Should have only one screen", 1, allScreens.size)
    }

    // ================================================================
    // HELPER METHODS
    // ================================================================

    /**
     * Create a test ScreenContextDTO with sensible defaults
     */
    private fun createTestScreenContext(
        screenHash: String,
        packageName: String,
        appId: String,
        activityName: String = "TestActivity",
        elementCount: Long = 15L,
        visitCount: Long = 1L
    ): ScreenContextDTO {
        val currentTime = System.currentTimeMillis()

        return ScreenContextDTO(
            id = 0L, // Auto-generated by database
            screenHash = screenHash,
            appId = appId,
            packageName = packageName,
            activityName = activityName,
            windowTitle = "Test Window",
            screenType = "normal",
            formContext = null,
            navigationLevel = 1L,
            primaryAction = "view",
            elementCount = elementCount,
            hasBackButton = 1L,
            firstScraped = currentTime,
            lastScraped = currentTime,
            visitCount = visitCount
        )
    }
}
