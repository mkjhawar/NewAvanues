/**
 * VoiceCommandsAutomatedTest.kt - Comprehensive automated voice command tests
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-11-07
 * Purpose: Automated tests for 20 voice commands - can run on emulator
 */
package com.augmentalis.voiceoscore.automated

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.augmentalis.voiceoscore.commands.DatabaseCommandHandler
import com.augmentalis.voiceoscore.database.VoiceOSAppDatabase
import com.augmentalis.voiceoscore.database.entities.AppEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

/**
 * Automated voice command tests
 *
 * Tests all 20 voice commands for database interaction
 *
 * Can run as:
 * - Unit tests: ./gradlew :tests:automated-tests:test
 * - Instrumented tests: ./gradlew :tests:automated-tests:connectedAndroidTest
 */
@RunWith(RobolectricTestRunner::class)
class VoiceCommandsAutomatedTest {

    private lateinit var context: Context
    private lateinit var database: VoiceOSAppDatabase
    private lateinit var handler: DatabaseCommandHandler

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Create in-memory database
        database = Room.inMemoryDatabaseBuilder(
            context,
            VoiceOSAppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        handler = DatabaseCommandHandler(context, database)

        // Seed realistic test data
        seedTestData()
    }

    @After
    fun teardown() {
        database.close()
    }

    private fun seedTestData() = runBlocking {
        // Create realistic test apps
        val apps = listOf(
            AppEntity(
                packageName = "com.instagram.android",
                appId = "app1",
                appName = "Instagram",
                versionCode = 12345L,
                versionName = "200.0.0",
                appHash = "hash1",
                exploredElementCount = 312,
                scrapedElementCount = 47,
                totalScreens = 47,
                isFullyLearned = true,
                explorationStatus = "COMPLETE",
                firstExplored = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000), // 3 days ago
                lastExplored = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000),
                firstScraped = System.currentTimeMillis(),
                lastScraped = System.currentTimeMillis()
            ),
            AppEntity(
                packageName = "com.twitter.android",
                appId = "app2",
                appName = "Twitter",
                versionCode = 9999L,
                versionName = "9.50.0",
                appHash = "hash2",
                exploredElementCount = 75,
                scrapedElementCount = 25,
                totalScreens = 20,
                isFullyLearned = false,
                explorationStatus = "IN_PROGRESS",
                firstExplored = System.currentTimeMillis() - (1 * 24 * 60 * 60 * 1000), // 1 day ago
                lastExplored = System.currentTimeMillis() - (1 * 24 * 60 * 60 * 1000),
                firstScraped = System.currentTimeMillis(),
                lastScraped = System.currentTimeMillis()
            ),
            AppEntity(
                packageName = "com.facebook.katana",
                appId = "app3",
                appName = "Facebook",
                versionCode = 5000L,
                versionName = "350.0.0",
                appHash = "hash3",
                exploredElementCount = 50,
                scrapedElementCount = 30,
                totalScreens = 15,
                isFullyLearned = false,
                explorationStatus = "IN_PROGRESS",
                firstScraped = System.currentTimeMillis(),
                lastScraped = System.currentTimeMillis()
            )
        )

        database.appDao().insertBatch(apps)
    }

    // ========== CATEGORY 1: STATISTICS COMMANDS ==========

    @Test
    fun test101_showDatabaseStats_shouldReturnFullSummary() = runBlocking {
        // When: Execute "show database stats" command
        val result = handler.handleCommand("show database stats")

        // Then: Should return statistics with app count
        assertThat(result).isNotNull()
        assertThat(result).contains("3 apps")
        assertThat(result).contains("fully explored")
        assertThat(result).contains("Database is")
        assertThat(result).contains("elements")
    }

    @Test
    fun test102_howManyApps_shouldReturnAppCount() = runBlocking {
        // When: Execute "how many apps" command
        val result = handler.handleCommand("how many apps")

        // Then: Should return count breakdown
        assertThat(result).isNotNull()
        assertThat(result).contains("3 apps")
        assertThat(result).contains("fully learned")
        assertThat(result).contains("partially learned")
    }

    @Test
    fun test103_databaseSize_shouldReturnReadableSize() = runBlocking {
        // When: Execute "database size" command
        val result = handler.handleCommand("database size")

        // Then: Should return size with unit (KB, MB, GB)
        assertThat(result).isNotNull()
        assertThat(result).containsMatch("\\d+\\.\\d+ (KB|MB|GB)")
    }

    @Test
    fun test104_elementCount_shouldReturnTotalElements() = runBlocking {
        // When: Execute "element count" command
        val result = handler.handleCommand("element count")

        // Then: Should contain element count
        assertThat(result).isNotNull()
        assertThat(result).containsMatch("\\d+ elements")
    }

    // ========== CATEGORY 2: MIGRATION COMMANDS ==========

    @Test
    fun test201_migrationStatus_shouldReturnStatus() = runBlocking {
        // When: Execute "migration status" command
        val result = handler.handleCommand("migration status")

        // Then: Should return migration state
        assertThat(result).isNotNull()
        // Should contain either "complete" or "pending"
        assertThat(result).containsMatch("(complete|pending)")
    }

    // ========== CATEGORY 3: APP QUERY COMMANDS ==========

    @Test
    fun test301_listLearnedApps_shouldListAppsWithCompletion() = runBlocking {
        // When: Execute "list learned apps" command
        val result = handler.handleCommand("list learned apps")

        // Then: Should list apps with percentages
        assertThat(result).isNotNull()
        assertThat(result).contains("Instagram")
        assertThat(result).contains("Twitter")
        assertThat(result).contains("%") // Completion percentage
    }

    @Test
    fun test302_showAppDetailsForInstagram_shouldReturnDetailedInfo() = runBlocking {
        // When: Execute "show app details for Instagram" command
        val result = handler.handleCommand("show app details for Instagram")

        // Then: Should return detailed app info
        assertThat(result).isNotNull()
        assertThat(result).contains("Instagram")
        assertThat(result).contains("47 screens")
        assertThat(result).contains("elements")
        assertThat(result).contains("explored")
    }

    @Test
    fun test303_showAppDetailsForTwitter_shouldReturnTwitterInfo() = runBlocking {
        // When: Execute "show app details for Twitter" command
        val result = handler.handleCommand("show app details for Twitter")

        // Then: Should return Twitter details
        assertThat(result).isNotNull()
        assertThat(result).contains("Twitter")
        assertThat(result).contains("20 screens")
    }

    @Test
    fun test304_showAppDetailsForNonExistent_shouldReturnNotFound() = runBlocking {
        // When: Execute command for non-existent app
        val result = handler.handleCommand("show app details for NonExistentApp")

        // Then: Should return not found message
        assertThat(result).isNotNull()
        assertThat(result).contains("not found")
    }

    @Test
    fun test305_whichAppsNeedLearning_shouldListIncompleteApps() = runBlocking {
        // When: Execute "which apps need learning" command
        val result = handler.handleCommand("which apps need learning")

        // Then: Should list incomplete apps
        assertThat(result).isNotNull()
        // Should NOT include Instagram (100% complete)
        assertThat(result).doesNotContain("Instagram")
        // Should include Twitter or Facebook (incomplete)
        assertThat(result).containsMatch("(Twitter|Facebook)")
    }

    @Test
    fun test306_mostLearnedApp_shouldReturnInstagram() = runBlocking {
        // When: Execute "most learned app" command
        val result = handler.handleCommand("most learned app")

        // Then: Should return Instagram (most screens)
        assertThat(result).isNotNull()
        assertThat(result).contains("Instagram")
        assertThat(result).contains("47 screens")
    }

    @Test
    fun test307_recentlyLearnedApps_shouldListByLastExplored() = runBlocking {
        // When: Execute "recently learned apps" command
        val result = handler.handleCommand("recently learned apps")

        // Then: Should list apps with time references
        assertThat(result).isNotNull()
        assertThat(result).containsMatch("(today|yesterday|\\d+ days ago)")
    }

    // ========== CATEGORY 4: MANAGEMENT COMMANDS ==========

    @Test
    fun test401_clearAppDataForTwitter_shouldDeleteApp() = runBlocking {
        // When: Execute "clear app data for Twitter" command
        val result = handler.handleCommand("clear app data for Twitter")

        // Then: Should confirm deletion
        assertThat(result).isNotNull()
        assertThat(result).contains("Cleared")
        assertThat(result).contains("Twitter")

        // And: App should be deleted
        val app = database.appDao().getAppByName("Twitter")
        assertThat(app).isNull()
    }

    @Test
    fun test402_optimizeDatabase_shouldRunVacuum() = runBlocking {
        // When: Execute "optimize database" command
        val result = handler.handleCommand("optimize database")

        // Then: Should confirm optimization
        assertThat(result).isNotNull()
        assertThat(result).containsMatch("(Optimized|Reduced size)")
    }

    @Test
    fun test403_databaseIntegrityCheck_shouldVerifyDatabase() = runBlocking {
        // When: Execute "database integrity check" command
        val result = handler.handleCommand("database integrity check")

        // Then: Should confirm integrity
        assertThat(result).isNotNull()
        assertThat(result).containsMatch("(integrity OK|No errors)")
    }

    // ========== PATTERN MATCHING TESTS ==========

    @Test
    fun test501_commands_shouldBeCaseInsensitive() = runBlocking {
        // When: Execute commands with different cases
        val lower = handler.handleCommand("show database stats")
        val upper = handler.handleCommand("SHOW DATABASE STATS")
        val mixed = handler.handleCommand("Show Database Stats")

        // Then: All should match and return results
        assertThat(lower).isNotNull()
        assertThat(upper).isNotNull()
        assertThat(mixed).isNotNull()

        // All should contain "apps"
        assertThat(lower).contains("apps")
        assertThat(upper).contains("apps")
        assertThat(mixed).contains("apps")
    }

    @Test
    fun test502_commands_shouldHandleExtraWords() = runBlocking {
        // When: Execute with extra words
        val result = handler.handleCommand("please show me the database stats right now")

        // Then: Should still match pattern
        assertThat(result).isNotNull()
        assertThat(result).contains("apps")
    }

    @Test
    fun test503_unrecognizedCommand_shouldReturnNull() = runBlocking {
        // When: Execute unrecognized command
        val result = handler.handleCommand("this is not a valid command at all")

        // Then: Should return null (allows fallback to other handlers)
        assertThat(result).isNull()
    }

    // ========== PERFORMANCE TESTS ==========

    @Test
    fun test601_statisticsCommand_shouldExecuteUnder500ms() = runBlocking {
        // When: Measure execution time
        val startTime = System.currentTimeMillis()
        handler.handleCommand("show database stats")
        val endTime = System.currentTimeMillis()

        val executionTime = endTime - startTime

        // Then: Should complete quickly
        assertThat(executionTime).isLessThan(500L)
    }

    @Test
    fun test602_allCommands_shouldExecuteUnder500ms() = runBlocking {
        // Given: List of commands to test
        val commands = listOf(
            "show database stats",
            "how many apps",
            "database size",
            "list learned apps",
            "migration status"
        )

        // When: Execute each command and measure time
        commands.forEach { command ->
            val startTime = System.currentTimeMillis()
            handler.handleCommand(command)
            val endTime = System.currentTimeMillis()

            val duration = endTime - startTime

            // Then: Each should complete in <500ms
            assertThat(duration).isLessThan(500L)
        }
    }

    // ========== INTEGRATION TESTS ==========

    @Test
    fun test701_multipleCommandSequence_shouldWorkCorrectly() = runBlocking {
        // Execute sequence of commands
        val result1 = handler.handleCommand("show database stats")
        assertThat(result1).isNotNull()

        val result2 = handler.handleCommand("list learned apps")
        assertThat(result2).isNotNull()

        val result3 = handler.handleCommand("most learned app")
        assertThat(result3).isNotNull()

        // All should return valid results
        assertThat(result1).contains("apps")
        assertThat(result2).contains("%")
        assertThat(result3).contains("Instagram")
    }

    @Test
    fun test702_commandAfterDataModification_shouldReflectChanges() = runBlocking {
        // Given: Initial app count
        val initialResult = handler.handleCommand("how many apps")
        assertThat(initialResult).contains("3 apps")

        // When: Add a new app
        database.appDao().insert(
            AppEntity(
                packageName = "com.whatsapp",
                appId = UUID.randomUUID().toString(),
                appName = "WhatsApp",
                versionCode = 1L,
                versionName = "1.0",
                appHash = "hash_whatsapp",
                firstScraped = System.currentTimeMillis(),
                lastScraped = System.currentTimeMillis()
            )
        )

        // Then: Command should reflect new count
        val updatedResult = handler.handleCommand("how many apps")
        assertThat(updatedResult).contains("4 apps")
    }
}
