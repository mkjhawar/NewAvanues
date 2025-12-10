/**
 * ScrapedAppRepositoryIntegrationTest.kt - Comprehensive ScrapedApp repository tests
 *
 * Tests all scraped app metadata operations and app lifecycle.
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.avanues.database.repositories

import com.avanues.database.dto.ScrapedAppDTO
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class ScrapedAppRepositoryIntegrationTest : BaseRepositoryTest() {

    // ==================== CRUD Tests ====================

    @Test
    fun testAppInsertAndGetById() = runTest {
        val repo = databaseManager.scrapedApps
        val app = createApp("app-001", "com.example.test")

        repo.insert(app)

        val retrieved = repo.getById("app-001")
        assertNotNull(retrieved)
        assertEquals("com.example.test", retrieved.packageName)
        assertEquals("1.0.0", retrieved.versionName)
    }

    @Test
    fun testGetByPackage() = runTest {
        val repo = databaseManager.scrapedApps

        repo.insert(createApp("app-001", "com.example.test"))
        repo.insert(createApp("app-002", "com.example.other"))

        val retrieved = repo.getByPackage("com.example.test")
        assertEquals("app-001", retrieved?.appId)
    }

    @Test
    fun testGetAll() = runTest {
        val repo = databaseManager.scrapedApps

        repo.insert(createApp("app-001", "com.test.one"))
        repo.insert(createApp("app-002", "com.test.two"))
        repo.insert(createApp("app-003", "com.test.three"))

        assertEquals(3, repo.getAll().size)
    }

    // ==================== Update & Lifecycle Tests ====================

    @Test
    fun testUpdateStats() = runTest {
        val repo = databaseManager.scrapedApps
        val app = createApp("app-001", "com.example.test")

        repo.insert(app)

        repo.updateStats("app-001", 5, 120, 35, now())

        val updated = repo.getById("app-001")
        assertEquals(5, updated?.scrapeCount)
        assertEquals(120, updated?.elementCount)
        assertEquals(35, updated?.commandCount)
    }

    @Test
    fun testMarkFullyLearned() = runTest {
        val repo = databaseManager.scrapedApps
        val app = createApp("app-001", "com.example.test")

        repo.insert(app)

        val completionTime = now()
        repo.markFullyLearned("app-001", completionTime)

        val learned = repo.getById("app-001")
        assertEquals(1, learned?.isFullyLearned)
        assertEquals(completionTime, learned?.learnCompletedAt)
    }

    @Test
    fun testGetFullyLearned() = runTest {
        val repo = databaseManager.scrapedApps

        repo.insert(createApp("app-001", "com.learned.one").copy(isFullyLearned = 1, learnCompletedAt = now()))
        repo.insert(createApp("app-002", "com.learning.two"))
        repo.insert(createApp("app-003", "com.learned.three").copy(isFullyLearned = 1, learnCompletedAt = now()))

        val fullyLearned = repo.getFullyLearned()
        assertEquals(2, fullyLearned.size)
        assertTrue(fullyLearned.all { it.isFullyLearned == 1L })
    }

    @Test
    fun testAppVersionUpdate() = runTest {
        val repo = databaseManager.scrapedApps

        // Insert v1
        repo.insert(createApp("app-001", "com.example.test").copy(versionCode = 1, versionName = "1.0.0"))

        // Delete old version
        repo.deleteById("app-001")

        // Insert v2
        repo.insert(createApp("app-001", "com.example.test").copy(versionCode = 2, versionName = "2.0.0"))

        val current = repo.getById("app-001")
        assertEquals(2, current?.versionCode)
        assertEquals("2.0.0", current?.versionName)
    }

    // ==================== Delete Tests ====================

    @Test
    fun testAppDelete() = runTest {
        val repo = databaseManager.scrapedApps

        repo.insert(createApp("app-001", "com.test.one"))
        repo.insert(createApp("app-002", "com.test.two"))

        assertEquals(2, repo.count())

        repo.deleteById("app-001")

        assertEquals(1, repo.count())
        assertNull(repo.getById("app-001"))
    }

    @Test
    fun testDeleteAll() = runTest {
        val repo = databaseManager.scrapedApps

        repo.insert(createApp("app-001", "com.test.one"))
        repo.insert(createApp("app-002", "com.test.two"))
        repo.insert(createApp("app-003", "com.test.three"))

        assertEquals(3, repo.count())

        repo.deleteAll()

        assertEquals(0, repo.count())
    }

    // ==================== Helpers ====================

    private fun createApp(appId: String, packageName: String): ScrapedAppDTO {
        val timestamp = now()
        return ScrapedAppDTO(
            appId = appId,
            packageName = packageName,
            versionCode = 1,
            versionName = "1.0.0",
            appHash = "hash-$appId",
            isFullyLearned = 0,
            learnCompletedAt = null,
            scrapingMode = "DYNAMIC",
            scrapeCount = 0,
            elementCount = 0,
            commandCount = 0,
            firstScrapedAt = timestamp,
            lastScrapedAt = timestamp
        )
    }
}
