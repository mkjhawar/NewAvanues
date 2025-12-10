/**
 * BaseRepositoryTest.kt - Base test class for repository integration tests
 *
 * Provides common database setup and teardown for all repository tests.
 * Uses in-memory SQLite database for fast, isolated testing.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.avanues.database.repositories

import com.avanues.database.DatabaseDriverFactory
import com.avanues.database.VoiceOSDatabaseManager
import com.avanues.database.dto.ScrapedAppDTO
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.AfterTest

/**
 * Base class for all repository integration tests.
 *
 * Subclasses automatically get:
 * - Fresh in-memory database before each test
 * - Proper cleanup after each test
 * - Access to databaseManager with all repositories
 */
abstract class BaseRepositoryTest {

    protected lateinit var databaseManager: VoiceOSDatabaseManager

    @BeforeTest
    fun setup() {
        val driverFactory = DatabaseDriverFactory()
        databaseManager = VoiceOSDatabaseManager(driverFactory)
    }

    @AfterTest
    fun teardown() {
        // Database is in-memory, so it will be garbage collected
        // No explicit cleanup needed
    }

    /**
     * Helper to get current timestamp.
     */
    protected fun now(): Long = System.currentTimeMillis()

    /**
     * Helper to get a timestamp N milliseconds in the past.
     */
    protected fun past(millisAgo: Long): Long = now() - millisAgo

    /**
     * Helper to get a timestamp N milliseconds in the future.
     */
    protected fun future(millisAhead: Long): Long = now() + millisAhead

    /**
     * Helper to ensure a ScrapedApp exists for the given appId.
     * Call this before inserting ScreenContext or other entities that reference scraped_app.
     *
     * This satisfies foreign key constraints without requiring every test to manually
     * set up parent records.
     */
    protected fun ensureScrapedAppExists(appId: String = "com.example.app") {
        runBlocking {
            val existing = databaseManager.scrapedApps.getById(appId)
            if (existing == null) {
                databaseManager.scrapedApps.insert(
                    ScrapedAppDTO(
                        appId = appId,
                        packageName = appId,
                        versionCode = 1,
                        versionName = "1.0.0",
                        appHash = "test-hash-${appId}",
                        isFullyLearned = 0,
                        learnCompletedAt = null,
                        scrapingMode = "DYNAMIC",
                        scrapeCount = 0,
                        elementCount = 0,
                        commandCount = 0,
                        firstScrapedAt = now(),
                        lastScrapedAt = now()
                    )
                )
            }
        }
    }
}
