/**
 * DatabaseAutomatedTest.kt - Comprehensive automated database tests
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-11-07
 * Purpose: Automated tests for database operations that can run on emulator
 */
package com.augmentalis.voiceoscore.automated

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.augmentalis.voiceoscore.database.VoiceOSAppDatabase
import com.augmentalis.voiceoscore.database.dao.AppDao
import com.augmentalis.voiceoscore.database.entities.AppEntity
import com.augmentalis.voiceoscore.scraping.dao.ScrapedElementDao
import com.augmentalis.voiceoscore.scraping.entities.ScrapedElementEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

/**
 * Automated database tests
 *
 * Can run as:
 * - Unit tests: ./gradlew :tests:automated-tests:test
 * - Instrumented tests: ./gradlew :tests:automated-tests:connectedAndroidTest
 */
@RunWith(RobolectricTestRunner::class)
class DatabaseAutomatedTest {

    private lateinit var context: Context
    private lateinit var database: VoiceOSAppDatabase
    private lateinit var appDao: AppDao
    private lateinit var scrapedElementDao: ScrapedElementDao

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            VoiceOSAppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        appDao = database.appDao()
        scrapedElementDao = database.scrapedElementDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    // ========== BASIC CRUD TESTS ==========

    @Test
    fun test001_insertApp_shouldPersistData() = runBlocking {
        // Given: An app entity
        val app = createTestApp("com.test.app", "Test App")

        // When: Insert app
        appDao.insert(app)

        // Then: Should be retrievable
        val retrieved = appDao.getApp("com.test.app")
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.appName).isEqualTo("Test App")
    }

    @Test
    fun test002_updateApp_shouldPersistChanges() = runBlocking {
        // Given: An inserted app
        val app = createTestApp("com.test.app", "Original Name")
        appDao.insert(app)

        // When: Update app
        val updated = app.copy(appName = "Updated Name")
        appDao.update(updated)

        // Then: Changes should persist
        val retrieved = appDao.getApp("com.test.app")
        assertThat(retrieved?.appName).isEqualTo("Updated Name")
    }

    @Test
    fun test003_deleteApp_shouldRemoveFromDatabase() = runBlocking {
        // Given: An inserted app
        val app = createTestApp("com.test.app")
        appDao.insert(app)

        // When: Delete app
        appDao.deleteApp("com.test.app")

        // Then: Should not be retrievable
        val retrieved = appDao.getApp("com.test.app")
        assertThat(retrieved).isNull()
    }

    @Test
    fun test004_insertMultipleApps_shouldAllBeRetrievable() = runBlocking {
        // Given: Multiple apps
        val apps = listOf(
            createTestApp("com.app1", "App 1"),
            createTestApp("com.app2", "App 2"),
            createTestApp("com.app3", "App 3")
        )

        // When: Insert batch
        appDao.insertBatch(apps)

        // Then: All should be retrievable
        val allApps = appDao.getAllApps()
        assertThat(allApps).hasSize(3)
        assertThat(allApps.map { it.appName }).containsExactly("App 1", "App 2", "App 3")
    }

    // ========== QUERY TESTS ==========

    @Test
    fun test005_getAppCount_shouldReturnCorrectCount() = runBlocking {
        // Given: 5 apps
        repeat(5) { i ->
            appDao.insert(createTestApp("com.app$i", "App $i"))
        }

        // When: Get count
        val count = appDao.getAppCount()

        // Then: Should return 5
        assertThat(count).isEqualTo(5)
    }

    @Test
    fun test006_getAppByName_shouldFindPartialMatch() = runBlocking {
        // Given: Apps with different names
        appDao.insert(createTestApp("com.instagram", "Instagram"))
        appDao.insert(createTestApp("com.facebook", "Facebook"))
        appDao.insert(createTestApp("com.twitter", "Twitter"))

        // When: Search by partial name (case-insensitive)
        val result = appDao.getAppByName("insta")

        // Then: Should find Instagram
        assertThat(result).isNotNull()
        assertThat(result?.appName).isEqualTo("Instagram")
    }

    @Test
    fun test007_getFullyLearnedAppCount_shouldReturnCorrectCount() = runBlocking {
        // Given: Mix of fully learned and partial apps
        appDao.insert(createTestApp("com.app1", isFullyLearned = true))
        appDao.insert(createTestApp("com.app2", isFullyLearned = true))
        appDao.insert(createTestApp("com.app3", isFullyLearned = false))
        appDao.insert(createTestApp("com.app4", isFullyLearned = false))

        // When: Get fully learned count
        val count = appDao.getFullyLearnedAppCount()

        // Then: Should return 2
        assertThat(count).isEqualTo(2)
    }

    @Test
    fun test008_getAppsByExplorationStatus_shouldFilterCorrectly() = runBlocking {
        // Given: Apps with different statuses
        appDao.insert(createTestApp("com.app1", explorationStatus = "COMPLETE"))
        appDao.insert(createTestApp("com.app2", explorationStatus = "IN_PROGRESS"))
        appDao.insert(createTestApp("com.app3", explorationStatus = "COMPLETE"))
        appDao.insert(createTestApp("com.app4", explorationStatus = "IN_PROGRESS"))

        // When: Query by status
        val completed = appDao.getAppsByExplorationStatus("COMPLETE")

        // Then: Should return 2 apps
        assertThat(completed).hasSize(2)
        assertThat(completed.map { it.packageName }).containsExactly("com.app1", "com.app3")
    }

    // ========== SCRAPED ELEMENTS TESTS ==========

    @Test
    fun test009_insertScrapedElement_shouldPersist() = runBlocking {
        // Given: An app and element
        val app = createTestApp("com.test.app")
        appDao.insert(app)

        val element = createTestElement(app.appId, "element1", "Button")

        // When: Insert element
        scrapedElementDao.insert(element)

        // Then: Should be retrievable by hash
        val retrieved = scrapedElementDao.getElementByHash(element.elementHash)
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.text).isEqualTo("Button")
    }

    @Test
    fun test010_getTotalCount_shouldReturnCorrectCount() = runBlocking {
        // Given: An app with 10 elements
        val app = createTestApp("com.test.app")
        appDao.insert(app)

        repeat(10) { i ->
            val element = createTestElement(app.appId, "element$i", "Element $i")
            scrapedElementDao.insert(element)
        }

        // When: Get total count
        val count = scrapedElementDao.getTotalCount()

        // Then: Should return 10
        assertThat(count).isEqualTo(10)
    }

    @Test
    fun test011_getElementCountForApp_shouldReturnCorrectCount() = runBlocking {
        // Given: Two apps with different element counts
        val app1 = createTestApp("com.app1")
        val app2 = createTestApp("com.app2")
        appDao.insert(app1)
        appDao.insert(app2)

        // App1: 5 elements
        repeat(5) { i ->
            scrapedElementDao.insert(createTestElement(app1.appId, "app1_elem$i", "Element $i"))
        }

        // App2: 3 elements
        repeat(3) { i ->
            scrapedElementDao.insert(createTestElement(app2.appId, "app2_elem$i", "Element $i"))
        }

        // When: Get count for app1
        val count = scrapedElementDao.getElementCountForApp(app1.appId)

        // Then: Should return 5
        assertThat(count).isEqualTo(5)
    }

    // ========== FOREIGN KEY CASCADE TESTS ==========

    @Test
    fun test012_deleteApp_shouldCascadeDeleteElements() = runBlocking {
        // Given: An app with elements
        val app = createTestApp("com.test.app")
        appDao.insert(app)

        repeat(5) { i ->
            scrapedElementDao.insert(createTestElement(app.appId, "elem$i", "Element $i"))
        }

        val elementsBeforeDelete = scrapedElementDao.getElementCountForApp(app.appId)
        assertThat(elementsBeforeDelete).isEqualTo(5)

        // When: Delete app
        appDao.deleteApp("com.test.app")

        // Then: All elements should be cascade deleted
        val elementsAfterDelete = scrapedElementDao.getElementCountForApp(app.appId)
        assertThat(elementsAfterDelete).isEqualTo(0)
    }

    // ========== PERFORMANCE TESTS ==========

    @Test
    fun test013_batchInsert100Apps_shouldCompleteFast() = runBlocking {
        // Given: 100 apps
        val apps = (1..100).map { i ->
            createTestApp("com.test.app$i", "App $i")
        }

        // When: Insert batch and measure time
        val startTime = System.currentTimeMillis()
        appDao.insertBatch(apps)
        val endTime = System.currentTimeMillis()

        // Then: Should complete in <1 second
        val duration = endTime - startTime
        assertThat(duration).isLessThan(1000L)

        // Verify all inserted
        val count = appDao.getAppCount()
        assertThat(count).isEqualTo(100)
    }

    @Test
    fun test014_queryAllApps_shouldCompleteFast() = runBlocking {
        // Given: 50 apps
        val apps = (1..50).map { i ->
            createTestApp("com.test.app$i", "App $i")
        }
        appDao.insertBatch(apps)

        // When: Query all and measure time
        val startTime = System.currentTimeMillis()
        val results = appDao.getAllApps()
        val endTime = System.currentTimeMillis()

        // Then: Should complete in <100ms
        val duration = endTime - startTime
        assertThat(duration).isLessThan(100L)
        assertThat(results).hasSize(50)
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    fun test015_insertDuplicatePackageName_shouldReplace() = runBlocking {
        // Given: An app
        val app1 = createTestApp("com.test.app", "Original")
        appDao.insert(app1)

        // When: Insert same packageName with different data
        val app2 = createTestApp("com.test.app", "Replaced")
        appDao.insert(app2)

        // Then: Should replace (REPLACE conflict strategy)
        val result = appDao.getApp("com.test.app")
        assertThat(result?.appName).isEqualTo("Replaced")

        // And: Should have only 1 app
        val count = appDao.getAppCount()
        assertThat(count).isEqualTo(1)
    }

    @Test
    fun test016_getAppByName_withNonExistentName_shouldReturnNull() = runBlocking {
        // When: Search for non-existent app
        val result = appDao.getAppByName("NonExistentApp")

        // Then: Should return null
        assertThat(result).isNull()
    }

    @Test
    fun test017_getAllApps_onEmptyDatabase_shouldReturnEmptyList() = runBlocking {
        // When: Query empty database
        val apps = appDao.getAllApps()

        // Then: Should return empty list
        assertThat(apps).isEmpty()
    }

    @Test
    fun test018_deleteNonExistentApp_shouldNotThrow() = runBlocking {
        // When: Delete non-existent app
        // Then: Should not throw exception
        try {
            appDao.deleteApp("com.nonexistent.app")
            // Success - no exception thrown
            assertThat(true).isTrue()
        } catch (e: Exception) {
            // Fail if exception thrown
            assertThat(false).isTrue()
        }
    }

    // ========== HELPER FUNCTIONS ==========

    private fun createTestApp(
        packageName: String,
        appName: String = "Test App",
        isFullyLearned: Boolean = false,
        explorationStatus: String? = null,
        exploredElementCount: Int? = null,
        scrapedElementCount: Int? = null
    ): AppEntity {
        return AppEntity(
            packageName = packageName,
            appId = UUID.randomUUID().toString(),
            appName = appName,
            versionCode = 1L,
            versionName = "1.0",
            appHash = "hash_$packageName",
            isFullyLearned = isFullyLearned,
            explorationStatus = explorationStatus,
            exploredElementCount = exploredElementCount,
            scrapedElementCount = scrapedElementCount,
            firstScraped = System.currentTimeMillis(),
            lastScraped = System.currentTimeMillis()
        )
    }

    private fun createTestElement(
        appId: String,
        hashSuffix: String,
        text: String
    ): ScrapedElementEntity {
        return ScrapedElementEntity(
            id = 0, // Auto-generated
            appId = appId,
            elementHash = "hash_$hashSuffix",
            uuid = UUID.randomUUID().toString(),
            className = "android.widget.TextView",
            text = text,
            contentDescription = null,
            viewIdResourceName = null,
            bounds = "{\"left\":0,\"top\":0,\"right\":100,\"bottom\":50}",
            isClickable = true,
            isLongClickable = false,
            isEditable = false,
            isScrollable = false,
            isCheckable = false,
            isFocusable = true,
            isEnabled = true,
            depth = 1,
            indexInParent = 0,
            scrapedAt = System.currentTimeMillis()
        )
    }
}
