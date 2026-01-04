/**
 * LearnAppMergeTest.kt - Test scenarios for LearnApp mode merge logic
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-10
 */
package com.augmentalis.voiceoscore.scraping

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.voiceoscore.scraping.database.AppScrapingDatabase
import com.augmentalis.voiceoscore.scraping.entities.ScrapedAppEntity
import com.augmentalis.voiceoscore.scraping.entities.ScrapedElementEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * Test suite for LearnApp mode merge functionality
 *
 * These tests verify that:
 * 1. Dynamic mode elements are preserved when LearnApp runs
 * 2. LearnApp mode elements are updated when Dynamic mode revisits
 * 3. Hash-based deduplication prevents duplicate elements
 * 4. Element counts remain consistent after merges
 */
@RunWith(AndroidJUnit4::class)
class LearnAppMergeTest {

    private lateinit var database: AppScrapingDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Use in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppScrapingDatabase::class.java
        ).build()
    }

    @After
    fun teardown() {
        database.close()
    }

    /**
     * Test Scenario 1: Dynamic first, then LearnApp
     *
     * Flow:
     * 1. Dynamic mode scrapes 3 elements (partial UI)
     * 2. LearnApp mode discovers 5 elements (complete UI)
     * 3. Result: 5 total elements (3 updated, 2 new)
     *
     * Validates:
     * - Existing elements are updated, not duplicated
     * - New elements are added
     * - Element count reflects actual unique elements
     */
    @Test
    fun testDynamicFirstThenLearnApp() = runBlocking {
        // Create test app
        val appId = UUID.randomUUID().toString()
        val app = createTestApp(appId, isFullyLearned = false)
        database.scrapedAppDao().insert(app)

        // Phase 1: Dynamic mode scrapes 3 elements
        val dynamicElements = listOf(
            createTestElement(appId, "hash_button_1", "Button", "Submit"),
            createTestElement(appId, "hash_input_1", "EditText", "Username"),
            createTestElement(appId, "hash_text_1", "TextView", "Welcome")
        )

        for (element in dynamicElements) {
            database.scrapedElementDao().insert(element)
        }

        // Verify initial state
        var elementCount = database.scrapedElementDao().getElementCountForApp(appId)
        assertEquals("Initial dynamic scrape should have 3 elements", 3, elementCount)

        // Phase 2: LearnApp mode discovers 5 elements (3 overlap + 2 new)
        val learnAppElements = listOf(
            createTestElement(appId, "hash_button_1", "Button", "Submit - Updated"),
            createTestElement(appId, "hash_input_1", "EditText", "Username - Updated"),
            createTestElement(appId, "hash_text_1", "TextView", "Welcome - Updated"),
            createTestElement(appId, "hash_button_2", "Button", "Cancel"), // New
            createTestElement(appId, "hash_link_1", "TextView", "Forgot Password?") // New
        )

        var newCount = 0
        var updatedCount = 0

        for (element in learnAppElements) {
            val existing = database.scrapedElementDao().getElementByHash(element.elementHash)
            if (existing != null) {
                updatedCount++
            } else {
                newCount++
            }
            database.scrapedElementDao().upsertElement(element)
        }

        // Verify merge results
        assertEquals("Should have 3 updated elements", 3, updatedCount)
        assertEquals("Should have 2 new elements", 2, newCount)

        elementCount = database.scrapedElementDao().getElementCountForApp(appId)
        assertEquals("Total should be 5 unique elements", 5, elementCount)

        // Verify updated content
        val updatedButton = database.scrapedElementDao().getElementByHash("hash_button_1")
        assertNotNull("Updated button should exist", updatedButton)
        assertEquals("Button text should be updated", "Submit - Updated", updatedButton?.text)

        // Mark app as fully learned
        database.scrapedAppDao().markAsFullyLearned(appId)
        val finalApp = database.scrapedAppDao().getAppById(appId)
        assertTrue("App should be marked as fully learned", finalApp?.isFullyLearned == true)
    }

    /**
     * Test Scenario 2: LearnApp first, then Dynamic
     *
     * Flow:
     * 1. LearnApp mode discovers 5 elements (complete UI)
     * 2. Dynamic mode revisits 2 elements (partial UI)
     * 3. Result: 5 total elements (2 updated timestamps, 3 unchanged)
     *
     * Validates:
     * - LearnApp elements are preserved
     * - Dynamic mode updates existing elements
     * - No duplicate elements created
     */
    @Test
    fun testLearnAppFirstThenDynamic() = runBlocking {
        // Create test app
        val appId = UUID.randomUUID().toString()
        val app = createTestApp(appId, isFullyLearned = false)
        database.scrapedAppDao().insert(app)

        // Phase 1: LearnApp mode discovers 5 elements
        val learnAppElements = listOf(
            createTestElement(appId, "hash_button_1", "Button", "Submit"),
            createTestElement(appId, "hash_input_1", "EditText", "Username"),
            createTestElement(appId, "hash_text_1", "TextView", "Welcome"),
            createTestElement(appId, "hash_button_2", "Button", "Cancel"),
            createTestElement(appId, "hash_link_1", "TextView", "Forgot Password?")
        )

        for (element in learnAppElements) {
            database.scrapedElementDao().insert(element)
        }

        database.scrapedAppDao().markAsFullyLearned(appId)

        // Verify initial state
        var elementCount = database.scrapedElementDao().getElementCountForApp(appId)
        assertEquals("LearnApp should discover 5 elements", 5, elementCount)

        // Phase 2: Dynamic mode revisits 2 elements (simulating user navigation)
        val dynamicElements = listOf(
            createTestElement(
                appId,
                "hash_button_1",
                "Button",
                "Submit",
                scrapedAt = System.currentTimeMillis() + 1000 // Later timestamp
            ),
            createTestElement(
                appId,
                "hash_input_1",
                "EditText",
                "Username",
                scrapedAt = System.currentTimeMillis() + 1000
            )
        )

        var newCount = 0
        var updatedCount = 0

        for (element in dynamicElements) {
            val existing = database.scrapedElementDao().getElementByHash(element.elementHash)
            if (existing != null) {
                updatedCount++
            } else {
                newCount++
            }
            database.scrapedElementDao().upsertElement(element)
        }

        // Verify merge results
        assertEquals("Should have 2 updated elements", 2, updatedCount)
        assertEquals("Should have 0 new elements", 0, newCount)

        elementCount = database.scrapedElementDao().getElementCountForApp(appId)
        assertEquals("Total should remain 5 unique elements", 5, elementCount)

        // Verify timestamps updated
        val updatedButton = database.scrapedElementDao().getElementByHash("hash_button_1")
        assertNotNull("Updated button should exist", updatedButton)
        assertTrue(
            "Updated button should have newer timestamp",
            updatedButton!!.scrapedAt > learnAppElements[0].scrapedAt
        )
    }

    /**
     * Test Scenario 3: Duplicate detection
     *
     * Flow:
     * 1. Insert element with hash_A
     * 2. Attempt to insert same element again (same hash)
     * 3. Result: 1 element (update, not duplicate)
     *
     * Validates:
     * - Hash-based deduplication works correctly
     * - Database ID is preserved during update
     * - Element count remains 1
     */
    @Test
    fun testDuplicateDetection() = runBlocking {
        // Create test app
        val appId = UUID.randomUUID().toString()
        val app = createTestApp(appId, isFullyLearned = false)
        database.scrapedAppDao().insert(app)

        // Insert initial element
        val element1 = createTestElement(appId, "hash_duplicate_test", "Button", "Click Me")
        database.scrapedElementDao().insert(element1)

        val initialElement = database.scrapedElementDao().getElementByHash("hash_duplicate_test")
        assertNotNull("Initial element should exist", initialElement)
        val initialDbId = initialElement!!.id

        // Attempt to insert duplicate (same hash, different text)
        val element2 = createTestElement(appId, "hash_duplicate_test", "Button", "Click Me - Updated")
        database.scrapedElementDao().upsertElement(element2)

        // Verify results
        val elementCount = database.scrapedElementDao().getElementCountForApp(appId)
        assertEquals("Should have only 1 element (not 2)", 1, elementCount)

        val updatedElement = database.scrapedElementDao().getElementByHash("hash_duplicate_test")
        assertNotNull("Element should still exist", updatedElement)
        assertEquals("Database ID should be preserved", initialDbId, updatedElement!!.id)
        assertEquals("Text should be updated", "Click Me - Updated", updatedElement.text)
    }

    /**
     * Test Scenario 4: Element count validation after multiple merges
     *
     * Flow:
     * 1. Dynamic scrapes 2 elements
     * 2. LearnApp discovers 4 elements (2 overlap, 2 new)
     * 3. Dynamic revisits 3 elements (3 overlap, 0 new)
     * 4. Result: 4 total elements (no duplicates)
     *
     * Validates:
     * - Element count remains consistent across multiple merge operations
     * - Hash-based deduplication prevents count inflation
     * - Database queries return correct counts
     */
    @Test
    fun testElementCountValidation() = runBlocking {
        // Create test app
        val appId = UUID.randomUUID().toString()
        val app = createTestApp(appId, isFullyLearned = false)
        database.scrapedAppDao().insert(app)

        // Phase 1: Dynamic scrapes 2 elements
        val dynamicPhase1 = listOf(
            createTestElement(appId, "hash_A", "Button", "A"),
            createTestElement(appId, "hash_B", "Button", "B")
        )

        for (element in dynamicPhase1) {
            database.scrapedElementDao().insert(element)
        }

        var elementCount = database.scrapedElementDao().getElementCountForApp(appId)
        assertEquals("Phase 1 should have 2 elements", 2, elementCount)

        // Phase 2: LearnApp discovers 4 elements (A, B, C, D)
        val learnAppPhase = listOf(
            createTestElement(appId, "hash_A", "Button", "A-Updated"),
            createTestElement(appId, "hash_B", "Button", "B-Updated"),
            createTestElement(appId, "hash_C", "Button", "C"), // New
            createTestElement(appId, "hash_D", "Button", "D")  // New
        )

        for (element in learnAppPhase) {
            database.scrapedElementDao().upsertElement(element)
        }

        elementCount = database.scrapedElementDao().getElementCountForApp(appId)
        assertEquals("Phase 2 should have 4 elements", 4, elementCount)

        database.scrapedAppDao().markAsFullyLearned(appId)

        // Phase 3: Dynamic revisits 3 elements (A, B, C)
        val dynamicPhase2 = listOf(
            createTestElement(appId, "hash_A", "Button", "A-ReUpdated"),
            createTestElement(appId, "hash_B", "Button", "B-ReUpdated"),
            createTestElement(appId, "hash_C", "Button", "C-ReUpdated")
        )

        for (element in dynamicPhase2) {
            database.scrapedElementDao().upsertElement(element)
        }

        elementCount = database.scrapedElementDao().getElementCountForApp(appId)
        assertEquals("Phase 3 should still have 4 elements", 4, elementCount)

        // Verify all 4 unique elements exist
        val allElements = database.scrapedElementDao().getElementsByAppId(appId)
        assertEquals("Should retrieve 4 unique elements", 4, allElements.size)

        val hashes = allElements.map { it.elementHash }.toSet()
        assertEquals("Should have 4 unique hashes", 4, hashes.size)
        assertTrue("Should contain hash_A", hashes.contains("hash_A"))
        assertTrue("Should contain hash_B", hashes.contains("hash_B"))
        assertTrue("Should contain hash_C", hashes.contains("hash_C"))
        assertTrue("Should contain hash_D", hashes.contains("hash_D"))
    }

    /**
     * Test Scenario 5: Scraping mode transitions
     *
     * Flow:
     * 1. App starts in DYNAMIC mode
     * 2. LearnApp mode triggered (mode = LEARN_APP)
     * 3. Learning completes (mode = DYNAMIC, isFullyLearned = true)
     *
     * Validates:
     * - Scraping mode transitions correctly
     * - isFullyLearned flag set after LearnApp completion
     * - learnCompletedAt timestamp recorded
     */
    @Test
    fun testScrapingModeTransitions() = runBlocking {
        // Create test app in DYNAMIC mode
        val appId = UUID.randomUUID().toString()
        val app = createTestApp(appId, isFullyLearned = false)
        database.scrapedAppDao().insert(app)

        // Verify initial mode
        var currentApp = database.scrapedAppDao().getAppById(appId)
        assertEquals("Initial mode should be DYNAMIC", "DYNAMIC", currentApp?.scrapingMode)
        assertFalse("Should not be fully learned initially", currentApp?.isFullyLearned == true)

        // Simulate LearnApp start
        database.scrapedAppDao().updateScrapingMode(appId, ScrapingMode.LEARN_APP.name)

        currentApp = database.scrapedAppDao().getAppById(appId)
        assertEquals("Mode should be LEARN_APP", ScrapingMode.LEARN_APP.name, currentApp?.scrapingMode)

        // Simulate LearnApp completion
        val completionTime = System.currentTimeMillis()
        database.scrapedAppDao().markAsFullyLearned(appId, completionTime)
        database.scrapedAppDao().updateScrapingMode(appId, ScrapingMode.DYNAMIC.name)

        currentApp = database.scrapedAppDao().getAppById(appId)
        assertEquals("Mode should be back to DYNAMIC", "DYNAMIC", currentApp?.scrapingMode)
        assertTrue("Should be marked as fully learned", currentApp?.isFullyLearned == true)
        assertEquals("Learn completion timestamp should match", completionTime, currentApp?.learnCompletedAt)
    }

    // Helper functions

    private fun createTestApp(
        appId: String,
        isFullyLearned: Boolean = false
    ): ScrapedAppEntity {
        return ScrapedAppEntity(
            appId = appId,
            packageName = "com.test.app",
            appName = "Test App",
            versionCode = 1,
            versionName = "1.0",
            appHash = "test_hash_${appId.take(8)}",
            firstScraped = System.currentTimeMillis(),
            lastScraped = System.currentTimeMillis(),
            isFullyLearned = isFullyLearned,
            learnCompletedAt = if (isFullyLearned) System.currentTimeMillis() else null,
            scrapingMode = if (isFullyLearned) "DYNAMIC" else "DYNAMIC"
        )
    }

    private fun createTestElement(
        appId: String,
        hash: String,
        className: String,
        text: String,
        scrapedAt: Long = System.currentTimeMillis()
    ): ScrapedElementEntity {
        return ScrapedElementEntity(
            id = 0, // Auto-generate
            elementHash = hash,
            appId = appId,
            className = className,
            viewIdResourceName = null,
            text = text,
            contentDescription = null,
            bounds = "{\"left\":0,\"top\":0,\"right\":100,\"bottom\":50}",
            isClickable = className == "Button",
            isLongClickable = false,
            isEditable = className == "EditText",
            isScrollable = false,
            isCheckable = false,
            isFocusable = true,
            isEnabled = true,
            depth = 2,
            indexInParent = 0,
            scrapedAt = scrapedAt
        )
    }
}
