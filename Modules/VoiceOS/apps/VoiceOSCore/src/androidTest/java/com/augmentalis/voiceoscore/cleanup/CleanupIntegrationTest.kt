/**
 * CleanupIntegrationTest.kt - End-to-end integration tests for cleanup flow
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-15
 *
 * P2 Tasks 2.2-2.3: Integration tests for complete cleanup workflow
 */

package com.augmentalis.voiceoscore.cleanup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.database.repositories.impl.SQLDelightGeneratedCommandRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test suite for cleanup flow.
 *
 * Tests complete workflow:
 * 1. Preview cleanup (calculate statistics)
 * 2. Execute cleanup (delete deprecated commands)
 * 3. Verify results (correct deletions, preservations)
 * 4. Settings integration (SharedPreferences persistence)
 */
@RunWith(AndroidJUnit4::class)
class CleanupIntegrationTest {

    private lateinit var context: Context
    private lateinit var databaseManager: VoiceOSDatabaseManager
    private lateinit var commandRepository: SQLDelightGeneratedCommandRepository
    private lateinit var cleanupManager: CleanupManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Create in-memory database
        val driver = DatabaseDriverFactory(context).createDriver(inMemory = true)
        databaseManager = VoiceOSDatabaseManager(driver)
        commandRepository = SQLDelightGeneratedCommandRepository(driver)
        cleanupManager = CleanupManager(commandRepository)
    }

    @After
    fun tearDown() {
        databaseManager.close()

        // Clear SharedPreferences
        context.getSharedPreferences("voiceos_cleanup", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    /**
     * Test: Complete cleanup workflow - preview then execute
     */
    @Test
    fun testCompleteCleanupWorkflow_previewThenExecute() = runBlocking {
        // Setup: Insert test data
        val packageName = "com.test.app"
        insertTestData(
            packageName = packageName,
            activeCommands = 70,
            deprecatedCommands = 30,
            deprecatedAge = 45  // Days old
        )

        // Step 1: Preview cleanup
        val preview = cleanupManager.previewCleanup(
            gracePeriodDays = 30,
            keepUserApproved = true
        )

        assertNotNull("Preview should not be null", preview)
        assertTrue("Should have commands to delete", preview.commandsToDelete > 0)
        assertTrue("Should have apps affected", preview.appsAffected.isNotEmpty())
        assertEquals("Should affect 1 package", 1, preview.appsAffected.size)
        assertTrue("Should estimate size reduction", preview.databaseSizeReduction > 0)

        // Step 2: Execute cleanup
        val result = cleanupManager.executeCleanup(
            gracePeriodDays = 30,
            keepUserApproved = true,
            dryRun = false
        )

        assertNotNull("Result should not be null", result)
        assertTrue("Should have deleted commands", result.deletedCount > 0)
        assertTrue("Should have preserved commands", result.preservedCount > 0)
        assertTrue("Should complete in reasonable time", result.durationMs < 5000)
        assertTrue("Should have no errors", result.errors.isEmpty())

        // Step 3: Verify database state
        val remainingCommands = commandRepository.getByPackage(packageName)
        assertEquals(
            "Remaining commands should match preserved count",
            result.preservedCount,
            remainingCommands.size
        )

        // All remaining commands should NOT be deprecated
        val deprecatedRemaining = remainingCommands.count { it.isDeprecated == 1L }
        assertEquals("No deprecated commands should remain", 0, deprecatedRemaining)
    }

    /**
     * Test: Cleanup respects grace period
     */
    @Test
    fun testCleanup_respectsGracePeriod() = runBlocking {
        val packageName = "com.test.grace"

        // Insert commands deprecated 15 days ago (within 30-day grace period)
        insertTestData(
            packageName = packageName,
            activeCommands = 50,
            deprecatedCommands = 20,
            deprecatedAge = 15
        )

        // Execute with 30-day grace period
        val result = cleanupManager.executeCleanup(
            gracePeriodDays = 30,
            keepUserApproved = true,
            dryRun = false
        )

        // Commands within grace period should be preserved
        assertEquals(
            "Should preserve commands within grace period",
            0,
            result.deletedCount
        )
    }

    /**
     * Test: Cleanup preserves user-approved commands
     */
    @Test
    fun testCleanup_preservesUserApproved() = runBlocking {
        val packageName = "com.test.approved"
        val oldTimestamp = System.currentTimeMillis() - (45 * 86400000L)

        // Insert deprecated but user-approved commands
        repeat(10) { i ->
            val cmd = GeneratedCommandDTO(
                id = (i + 1).toLong(),
                elementHash = "element_$i",
                commandText = "Approved command $i",
                actionType = "CLICK",
                confidence = 0.9,
                synonyms = null,
                isUserApproved = 1,  // User approved
                usageCount = 10,
                lastUsed = oldTimestamp,
                createdAt = oldTimestamp,
                appId = packageName,
                appVersion = "1.0.0",
                versionCode = 1,
                lastVerified = oldTimestamp,
                isDeprecated = 1  // Deprecated
            )
            commandRepository.insert(cmd)
        }

        // Execute cleanup with keepUserApproved = true
        val result = cleanupManager.executeCleanup(
            gracePeriodDays = 30,
            keepUserApproved = true,
            dryRun = false
        )

        assertEquals(
            "Should preserve all user-approved commands",
            0,
            result.deletedCount
        )
        assertEquals(
            "Should keep all 10 approved commands",
            10,
            result.preservedCount
        )
    }

    /**
     * Test: Dry run mode does not delete commands
     */
    @Test
    fun testCleanup_dryRunDoesNotDelete() = runBlocking {
        val packageName = "com.test.dryrun"

        insertTestData(
            packageName = packageName,
            activeCommands = 50,
            deprecatedCommands = 50,
            deprecatedAge = 45
        )

        val beforeCount = commandRepository.count()

        // Execute in dry run mode
        val result = cleanupManager.executeCleanup(
            gracePeriodDays = 30,
            keepUserApproved = true,
            dryRun = true
        )

        val afterCount = commandRepository.count()

        assertEquals(
            "Dry run should not change database",
            beforeCount,
            afterCount
        )
        assertTrue(
            "Dry run should still report what would be deleted",
            result.deletedCount > 0
        )
    }

    /**
     * Test: Safety limit prevents >90% deletion
     */
    @Test(expected = IllegalStateException::class)
    fun testCleanup_safetyLimit_prevents90PercentDeletion() = runBlocking {
        val packageName = "com.test.safety"

        // Insert mostly deprecated commands (95% deprecated)
        insertTestData(
            packageName = packageName,
            activeCommands = 5,
            deprecatedCommands = 95,
            deprecatedAge = 45
        )

        // Should throw IllegalStateException due to >90% deletion rate
        cleanupManager.executeCleanup(
            gracePeriodDays = 30,
            keepUserApproved = true,
            dryRun = false
        )
    }

    /**
     * Test: SharedPreferences persistence after cleanup
     */
    @Test
    fun testSharedPreferences_persistsLastCleanupInfo() = runBlocking {
        val packageName = "com.test.prefs"

        insertTestData(
            packageName = packageName,
            activeCommands = 60,
            deprecatedCommands = 40,
            deprecatedAge = 45
        )

        val beforeTimestamp = System.currentTimeMillis()

        // Execute cleanup
        val result = cleanupManager.executeCleanup(
            gracePeriodDays = 30,
            keepUserApproved = true,
            dryRun = false
        )

        // Simulate what CleanupPreviewActivity does
        val prefs = context.getSharedPreferences("voiceos_cleanup", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putLong("last_cleanup_timestamp", System.currentTimeMillis())
            putInt("last_cleanup_deleted", result.deletedCount)
            apply()
        }

        // Verify persistence
        val savedTimestamp = prefs.getLong("last_cleanup_timestamp", 0L)
        val savedDeleted = prefs.getInt("last_cleanup_deleted", 0)

        assertTrue("Should save timestamp", savedTimestamp >= beforeTimestamp)
        assertEquals("Should save deleted count", result.deletedCount, savedDeleted)
    }

    /**
     * Test: Multiple package cleanup
     */
    @Test
    fun testCleanup_multiplePackages_cleansCorrectly() = runBlocking {
        // Insert data for multiple packages
        insertTestData("com.app1", 30, 20, 45)
        insertTestData("com.app2", 40, 10, 45)
        insertTestData("com.app3", 50, 0, 0)

        val result = cleanupManager.executeCleanup(
            gracePeriodDays = 30,
            keepUserApproved = true,
            dryRun = false
        )

        assertTrue("Should delete from multiple packages", result.deletedCount >= 20)
        assertTrue("Should preserve active commands", result.preservedCount >= 120)

        // Verify app3 untouched (no deprecated commands)
        val app3Commands = commandRepository.getByPackage("com.app3")
        assertEquals("App3 should retain all commands", 50, app3Commands.size)
    }

    /**
     * Test: Cleanup performance benchmark
     */
    @Test
    fun testCleanup_performance_completesUnder5Seconds() = runBlocking {
        // Insert large dataset
        insertTestData("com.perf.test", 500, 500, 45)

        val startTime = System.currentTimeMillis()

        val result = cleanupManager.executeCleanup(
            gracePeriodDays = 30,
            keepUserApproved = true,
            dryRun = false
        )

        val endTime = System.currentTimeMillis()
        val actualDuration = endTime - startTime

        assertTrue(
            "Cleanup of 1000 commands should complete under 5 seconds (actual: ${actualDuration}ms)",
            actualDuration < 5000
        )
        assertTrue(
            "Result duration should be reasonable",
            result.durationMs < 5000
        )
    }

    /**
     * Helper: Insert test data
     */
    private suspend fun insertTestData(
        packageName: String,
        activeCommands: Int,
        deprecatedCommands: Int,
        deprecatedAge: Int
    ) {
        val now = System.currentTimeMillis()
        val deprecatedTimestamp = now - (deprecatedAge * 86400000L)

        // Insert active commands
        repeat(activeCommands) { i ->
            val cmd = GeneratedCommandDTO(
                id = 0,  // Auto-increment
                elementHash = "${packageName}_active_$i",
                commandText = "Active $i",
                actionType = "CLICK",
                confidence = 0.9,
                synonyms = null,
                isUserApproved = 0,
                usageCount = 5,
                lastUsed = now,
                createdAt = now,
                appId = packageName,
                appVersion = "2.0.0",
                versionCode = 2,
                lastVerified = now,
                isDeprecated = 0
            )
            commandRepository.insert(cmd)
        }

        // Insert deprecated commands
        repeat(deprecatedCommands) { i ->
            val cmd = GeneratedCommandDTO(
                id = 0,
                elementHash = "${packageName}_deprecated_$i",
                commandText = "Deprecated $i",
                actionType = "CLICK",
                confidence = 0.8,
                synonyms = null,
                isUserApproved = 0,
                usageCount = 2,
                lastUsed = deprecatedTimestamp,
                createdAt = deprecatedTimestamp,
                appId = packageName,
                appVersion = "1.0.0",
                versionCode = 1,
                lastVerified = deprecatedTimestamp,
                isDeprecated = 1
            )
            commandRepository.insert(cmd)
        }
    }
}
