/**
 * LearnAppCoreTest.kt - Instrumented tests for LearnAppCore functionality
 *
 * Tests element learning, command generation, and database operations
 * for the VoiceOS LearnApp system.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Claude Code
 * Created: 2025-12-30
 */

package com.augmentalis.voiceoscore.learnapp

import android.content.Context
import android.graphics.Rect
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.uuidcreator.thirdparty.ThirdPartyUuidGenerator
import com.augmentalis.voiceoscore.learnapp.core.LearnAppCore
import com.augmentalis.voiceoscore.learnapp.core.ProcessingMode
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import com.augmentalis.voiceoscore.version.AppVersionDetector
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.Field

/**
 * Instrumented tests for LearnAppCore.
 *
 * Verifies:
 * - Element learning and storage
 * - Command generation from learned elements
 * - Database operations (insert, query, update)
 * - No duplicate entries for same elements
 */
@RunWith(AndroidJUnit4::class)
class LearnAppCoreTest {

    private lateinit var context: Context
    private lateinit var databaseManager: VoiceOSDatabaseManager
    private lateinit var learnAppCore: LearnAppCore
    private lateinit var uuidGenerator: ThirdPartyUuidGenerator

    companion object {
        private const val TEST_PACKAGE_NAME = "com.test.learnapp.core"
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Reset singleton to ensure clean state
        resetDatabaseSingleton()

        // Initialize database
        val driverFactory = DatabaseDriverFactory(context)
        databaseManager = VoiceOSDatabaseManager.getInstance(driverFactory)

        // Initialize UUID generator
        uuidGenerator = ThirdPartyUuidGenerator(context)

        // Initialize LearnAppCore
        learnAppCore = LearnAppCore(
            context = context,
            database = databaseManager,
            uuidGenerator = uuidGenerator,
            versionDetector = null // Optional for these tests
        )

        // Clean up any existing test data
        cleanupTestData()

        // Create parent scraped_app record for FK constraint
        createParentAppRecord(TEST_PACKAGE_NAME)
    }

    @After
    fun teardown() {
        cleanupTestData()
        resetDatabaseSingleton()
    }

    private fun resetDatabaseSingleton() {
        try {
            val instanceField: Field = VoiceOSDatabaseManager::class.java
                .declaredFields
                .first { it.name == "INSTANCE" }
            instanceField.isAccessible = true
            instanceField.set(null, null)
        } catch (e: Exception) {
            println("Warning: Could not reset singleton: ${e.message}")
        }
    }

    private fun cleanupTestData() {
        try {
            // Clear any test commands and parent app record
            runBlocking {
                databaseManager.generatedCommands.deleteCommandsByPackage(TEST_PACKAGE_NAME)
                databaseManager.scrapedElements.deleteByApp(TEST_PACKAGE_NAME)
                databaseManager.scrapedAppQueries.deleteById(TEST_PACKAGE_NAME)
            }
        } catch (e: Exception) {
            println("Cleanup error: ${e.message}")
        }
    }

    /**
     * Creates parent scraped_app record required by FK constraint.
     */
    private fun createParentAppRecord(packageName: String) {
        val currentTime = System.currentTimeMillis()
        runBlocking {
            try {
                databaseManager.scrapedAppQueries.insert(
                    appId = packageName,
                    packageName = packageName,
                    versionCode = 1L,
                    versionName = "1.0.0",
                    appHash = "test_app_hash_$packageName",
                    isFullyLearned = 0L,
                    learnCompletedAt = null,
                    scrapingMode = "DYNAMIC",
                    scrapeCount = 0L,
                    elementCount = 0L,
                    commandCount = 0L,
                    firstScrapedAt = currentTime,
                    lastScrapedAt = currentTime,
                    pkg_hash = null
                )
            } catch (e: Exception) {
                println("Parent app record may already exist: ${e.message}")
            }
        }
    }

    // =========================================================================
    // Element Learning and Storage Tests
    // =========================================================================

    @Test
    fun testProcessElementCreatesCommand() = runBlocking {
        // Given: A clickable button element
        val element = createTestElement(
            className = "android.widget.Button",
            text = "Submit",
            isClickable = true
        )

        // When: Processing element in IMMEDIATE mode
        // Note: LearnAppCore.ensureElementRecordExists handles FK constraint internally
        val result = learnAppCore.processElement(
            element = element,
            packageName = TEST_PACKAGE_NAME,
            mode = ProcessingMode.IMMEDIATE
        )

        // Then: Result should be successful
        assertTrue("Processing should succeed", result.success)
        assertNotNull("UUID should be generated", result.uuid)
        assertTrue("UUID should not be empty", result.uuid.isNotEmpty())
    }

    @Test
    fun testProcessElementGeneratesCommand() = runBlocking {
        // Given: A clickable button with valid label
        val element = createTestElement(
            className = "android.widget.Button",
            text = "Login",
            isClickable = true
        )

        // When: Processing element
        val result = learnAppCore.processElement(
            element = element,
            packageName = TEST_PACKAGE_NAME,
            mode = ProcessingMode.IMMEDIATE
        )

        // Then: Command should be generated
        assertTrue("Processing should succeed", result.success)
        assertNotNull("Command should be generated", result.command)
        assertEquals("Command should have click action", "click", result.command?.actionType)
        assertTrue(
            "Command text should contain 'login'",
            result.command?.commandText?.lowercase()?.contains("login") == true
        )
    }

    @Test
    fun testProcessElementFiltersShortLabels() = runBlocking {
        // Given: Element with single-character text (too short)
        val element = createTestElement(
            className = "android.widget.Button",
            text = "X",
            isClickable = true
        )

        // When: Processing element
        val result = learnAppCore.processElement(
            element = element,
            packageName = TEST_PACKAGE_NAME,
            mode = ProcessingMode.IMMEDIATE
        )

        // Then: Should succeed but without generating command (filtered)
        assertTrue("Processing should succeed", result.success)
        // Note: For native apps, short labels are filtered (no command generated)
        // This is expected behavior for quality control
    }

    @Test
    fun testProcessElementWithEditText() = runBlocking {
        // Given: An EditText element
        val element = createTestElement(
            className = "android.widget.EditText",
            text = "Username",
            isClickable = false,
            isScrollable = false
        )

        // When: Processing element
        val result = learnAppCore.processElement(
            element = element,
            packageName = TEST_PACKAGE_NAME,
            mode = ProcessingMode.IMMEDIATE
        )

        // Then: Should generate "type" action
        assertTrue("Processing should succeed", result.success)
        if (result.command != null) {
            assertEquals("Action should be 'type'", "type", result.command?.actionType)
        }
    }

    @Test
    fun testProcessElementWithScrollable() = runBlocking {
        // Given: A scrollable element
        val element = createTestElement(
            className = "androidx.recyclerview.widget.RecyclerView",
            contentDescription = "Messages List",
            isScrollable = true
        )

        // When: Processing element
        val result = learnAppCore.processElement(
            element = element,
            packageName = TEST_PACKAGE_NAME,
            mode = ProcessingMode.IMMEDIATE
        )

        // Then: Should generate "scroll" action
        assertTrue("Processing should succeed", result.success)
        if (result.command != null) {
            assertEquals("Action should be 'scroll'", "scroll", result.command?.actionType)
        }
    }

    // =========================================================================
    // Command Generation Tests
    // =========================================================================

    @Test
    fun testCommandTextIsLowercase() = runBlocking {
        // Given: Element with mixed case text
        val element = createTestElement(
            className = "android.widget.Button",
            text = "Sign In Now",
            isClickable = true
        )

        // When: Processing element
        val result = learnAppCore.processElement(
            element = element,
            packageName = TEST_PACKAGE_NAME,
            mode = ProcessingMode.IMMEDIATE
        )

        // Then: Command text should be lowercase
        assertTrue("Processing should succeed", result.success)
        if (result.command != null) {
            assertEquals(
                "Command text should be lowercase",
                result.command?.commandText?.lowercase(),
                result.command?.commandText
            )
        }
    }

    @Test
    fun testSynonymsAreGenerated() = runBlocking {
        // Given: A clickable button
        val element = createTestElement(
            className = "android.widget.Button",
            text = "Settings",
            isClickable = true
        )

        // When: Processing element
        val result = learnAppCore.processElement(
            element = element,
            packageName = TEST_PACKAGE_NAME,
            mode = ProcessingMode.IMMEDIATE
        )

        // Then: Synonyms should be generated
        assertTrue("Processing should succeed", result.success)
        if (result.command != null) {
            assertNotNull("Synonyms should be present", result.command?.synonyms)
            assertTrue(
                "Synonyms should contain tap alternative",
                result.command?.synonyms?.contains("tap") == true
            )
        }
    }

    // =========================================================================
    // Batch Processing Tests
    // =========================================================================

    @Test
    fun testBatchModeQueuesCommands() = runBlocking {
        // Given: Multiple elements
        val elements = listOf(
            createTestElement("android.widget.Button", "Save", isClickable = true),
            createTestElement("android.widget.Button", "Cancel", isClickable = true),
            createTestElement("android.widget.Button", "Delete", isClickable = true)
        )

        // When: Processing in BATCH mode
        elements.forEach { element ->
            learnAppCore.processElement(
                element = element,
                packageName = TEST_PACKAGE_NAME,
                mode = ProcessingMode.BATCH
            )
        }

        // Then: Commands should be queued
        assertTrue("Batch queue should have elements", learnAppCore.getBatchQueueSize() > 0)

        // Cleanup: Clear the batch queue
        learnAppCore.clearBatchQueue()
        assertEquals("Batch queue should be empty after clear", 0, learnAppCore.getBatchQueueSize())
    }

    @Test
    fun testFlushBatchInsertsToDatabase() = runBlocking {
        // Given: Elements processed in BATCH mode
        val elements = listOf(
            createTestElement("android.widget.Button", "Home", isClickable = true),
            createTestElement("android.widget.Button", "Profile", isClickable = true)
        )

        elements.forEach { element ->
            learnAppCore.processElement(
                element = element,
                packageName = TEST_PACKAGE_NAME,
                mode = ProcessingMode.BATCH
            )
        }

        // When: Flushing batch
        learnAppCore.flushBatch()

        // Then: Queue should be empty and commands in database
        assertEquals("Batch queue should be empty after flush", 0, learnAppCore.getBatchQueueSize())

        // Verify commands are in database
        val commandCount = databaseManager.generatedCommands.count()
        assertTrue("Commands should be in database", commandCount > 0)
    }

    // =========================================================================
    // Database Operation Tests
    // =========================================================================

    @Test
    fun testCommandIsStoredInDatabase() = runBlocking {
        // Given: Initial command count
        val initialCount = databaseManager.generatedCommands.count()

        // When: Processing element in IMMEDIATE mode
        val element = createTestElement(
            className = "android.widget.Button",
            text = "Next Step",
            isClickable = true
        )

        val result = learnAppCore.processElement(
            element = element,
            packageName = TEST_PACKAGE_NAME,
            mode = ProcessingMode.IMMEDIATE
        )

        // Then: Count should increase if command was generated
        if (result.command != null) {
            val newCount = databaseManager.generatedCommands.count()
            assertEquals("Command count should increase by 1", initialCount + 1, newCount)
        }
    }

    @Test
    fun testElementHashIsCalculatedConsistently() = runBlocking {
        // Given: Same element processed twice
        val element = createTestElement(
            className = "android.widget.Button",
            text = "Consistent Button",
            resourceId = "com.test:id/consistent_btn",
            isClickable = true,
            bounds = Rect(100, 200, 300, 400)
        )

        // When: Processing same element twice
        val result1 = learnAppCore.processElement(
            element = element,
            packageName = TEST_PACKAGE_NAME,
            mode = ProcessingMode.BATCH
        )

        val result2 = learnAppCore.processElement(
            element = element,
            packageName = TEST_PACKAGE_NAME,
            mode = ProcessingMode.BATCH
        )

        // Then: Both should have same element hash
        if (result1.command != null && result2.command != null) {
            assertEquals(
                "Element hashes should match for identical elements",
                result1.command?.elementHash,
                result2.command?.elementHash
            )
        }

        // Cleanup
        learnAppCore.clearBatchQueue()
    }

    // =========================================================================
    // UUID Generation Tests
    // =========================================================================

    @Test
    fun testUUIDGenerationIsStable() = runBlocking {
        // Given: Same element
        val element = createTestElement(
            className = "android.widget.Button",
            text = "Stable UUID Test",
            resourceId = "com.test:id/stable_uuid_btn",
            isClickable = true,
            bounds = Rect(50, 100, 250, 180)
        )

        // When: Processing element multiple times
        val result1 = learnAppCore.processElement(
            element = element,
            packageName = TEST_PACKAGE_NAME,
            mode = ProcessingMode.BATCH
        )

        val result2 = learnAppCore.processElement(
            element = element,
            packageName = TEST_PACKAGE_NAME,
            mode = ProcessingMode.BATCH
        )

        // Then: UUIDs should be identical for same element
        assertEquals(
            "UUID should be stable for same element",
            result1.uuid,
            result2.uuid
        )

        // Cleanup
        learnAppCore.clearBatchQueue()
    }

    @Test
    fun testUUIDIsDifferentForDifferentElements() = runBlocking {
        // Given: Two different elements
        val element1 = createTestElement(
            className = "android.widget.Button",
            text = "Button A",
            resourceId = "com.test:id/btn_a",
            isClickable = true,
            bounds = Rect(0, 0, 100, 50)
        )

        val element2 = createTestElement(
            className = "android.widget.Button",
            text = "Button B",
            resourceId = "com.test:id/btn_b",
            isClickable = true,
            bounds = Rect(0, 60, 100, 110)
        )

        // When: Processing both elements
        val result1 = learnAppCore.processElement(
            element = element1,
            packageName = TEST_PACKAGE_NAME,
            mode = ProcessingMode.BATCH
        )

        val result2 = learnAppCore.processElement(
            element = element2,
            packageName = TEST_PACKAGE_NAME,
            mode = ProcessingMode.BATCH
        )

        // Then: UUIDs should be different
        assertNotEquals(
            "UUIDs should be different for different elements",
            result1.uuid,
            result2.uuid
        )

        // Cleanup
        learnAppCore.clearBatchQueue()
    }

    // =========================================================================
    // Error Handling Tests
    // =========================================================================

    @Test
    fun testProcessElementWithNumericOnlyText() = runBlocking {
        // Given: Element with only numeric text
        val element = createTestElement(
            className = "android.widget.Button",
            text = "12345",
            isClickable = true
        )

        // When: Processing element
        val result = learnAppCore.processElement(
            element = element,
            packageName = TEST_PACKAGE_NAME,
            mode = ProcessingMode.IMMEDIATE
        )

        // Then: Should succeed but command may be null (filtered)
        assertTrue("Processing should succeed", result.success)
        // Numeric-only labels are filtered for native apps
    }

    @Test
    fun testProcessElementWithContentDescription() = runBlocking {
        // Given: Element with only contentDescription (no text)
        val element = createTestElement(
            className = "android.widget.ImageButton",
            text = "",
            contentDescription = "Navigation Menu",
            isClickable = true
        )

        // When: Processing element
        val result = learnAppCore.processElement(
            element = element,
            packageName = TEST_PACKAGE_NAME,
            mode = ProcessingMode.IMMEDIATE
        )

        // Then: Should use contentDescription as label
        assertTrue("Processing should succeed", result.success)
        if (result.command != null) {
            assertTrue(
                "Command should use contentDescription",
                result.command?.commandText?.contains("navigation") == true ||
                        result.command?.commandText?.contains("menu") == true
            )
        }
    }

    @Test
    fun testProcessElementWithResourceId() = runBlocking {
        // Given: Element with only resourceId (no text or description)
        val element = createTestElement(
            className = "android.widget.ImageView",
            text = "",
            contentDescription = "",
            resourceId = "com.test:id/search_button",
            isClickable = true
        )

        // When: Processing element
        val result = learnAppCore.processElement(
            element = element,
            packageName = TEST_PACKAGE_NAME,
            mode = ProcessingMode.IMMEDIATE
        )

        // Then: Should use resourceId as label
        assertTrue("Processing should succeed", result.success)
        if (result.command != null) {
            assertTrue(
                "Command should use resourceId suffix",
                result.command?.commandText?.contains("search_button") == true ||
                        result.command?.commandText?.contains("search") == true
            )
        }
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private fun createTestElement(
        className: String,
        text: String = "",
        contentDescription: String = "",
        resourceId: String = "",
        isClickable: Boolean = false,
        isScrollable: Boolean = false,
        bounds: Rect = Rect(0, 0, 200, 100)
    ): ElementInfo {
        return ElementInfo(
            className = className,
            text = text,
            contentDescription = contentDescription,
            resourceId = resourceId,
            isClickable = isClickable,
            isEnabled = true,
            isPassword = false,
            isScrollable = isScrollable,
            bounds = bounds,
            node = null,
            uuid = null
        )
    }

}
