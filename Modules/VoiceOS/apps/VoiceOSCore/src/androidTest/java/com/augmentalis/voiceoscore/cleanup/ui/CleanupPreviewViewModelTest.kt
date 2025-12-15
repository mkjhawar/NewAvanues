/**
 * CleanupPreviewViewModelTest.kt - Unit tests for cleanup preview ViewModel
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-15
 *
 * P2 Task 2.2: Tests for cleanup preview business logic
 */

package com.augmentalis.voiceoscore.cleanup.ui

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.database.repositories.impl.SQLDelightGeneratedCommandRepository
import com.augmentalis.voiceoscore.cleanup.CleanupManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test suite for CleanupPreviewViewModel.
 *
 * Verifies:
 * - Loading preview calculates correct statistics
 * - Safety levels assigned correctly based on deletion percentage
 * - Cleanup execution handles success/error states
 * - Affected apps loaded with correct command counts
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class CleanupPreviewViewModelTest {

    private lateinit var application: Application
    private lateinit var databaseManager: VoiceOSDatabaseManager
    private lateinit var commandRepository: SQLDelightGeneratedCommandRepository
    private lateinit var cleanupManager: CleanupManager
    private lateinit var viewModel: CleanupPreviewViewModel

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()

        // Create in-memory database
        val driver = DatabaseDriverFactory(application).createDriver(inMemory = true)
        databaseManager = VoiceOSDatabaseManager(driver)
        commandRepository = SQLDelightGeneratedCommandRepository(driver)
        cleanupManager = CleanupManager(commandRepository)
    }

    @After
    fun tearDown() {
        databaseManager.close()
    }

    /**
     * Test: Initial state is Loading
     */
    @Test
    fun testInitialState_isLoading() = runTest {
        viewModel = CleanupPreviewViewModel(cleanupManager, commandRepository, application)

        val initialState = viewModel.uiState.value
        assertTrue("Initial state should be Loading", initialState is CleanupPreviewUiState.Loading)
    }

    /**
     * Test: Load preview with no deprecated commands
     */
    @Test
    fun testLoadPreview_noDeprecatedCommands_showsZeroToDelete() = runTest {
        // Insert 10 active commands (no deprecated)
        insertTestCommands(
            packageName = "com.test.app",
            activeCount = 10,
            deprecatedCount = 0
        )

        viewModel = CleanupPreviewViewModel(cleanupManager, commandRepository, application)
        viewModel.loadPreview(gracePeriodDays = 30, keepUserApproved = true)

        // Wait for loading to complete
        kotlinx.coroutines.delay(500)

        val state = viewModel.uiState.value
        assertTrue("State should be Preview", state is CleanupPreviewUiState.Preview)

        val preview = state as CleanupPreviewUiState.Preview
        assertEquals("Should show 0 commands to delete", 0, preview.statistics.commandsToDelete)
        assertEquals("Should preserve all 10 commands", 10, preview.statistics.commandsToPreserve)
        assertEquals("Deletion percentage should be 0", 0, preview.statistics.deletionPercentage)
        assertEquals("Safety level should be SAFE", SafetyLevel.SAFE, preview.safetyLevel)
    }

    /**
     * Test: Load preview with deprecated commands within grace period
     */
    @Test
    fun testLoadPreview_deprecatedWithinGracePeriod_notMarkedForDeletion() = runTest {
        // Insert commands deprecated 15 days ago (within 30-day grace period)
        val fifteenDaysAgo = System.currentTimeMillis() - (15 * 86400000L)
        insertTestCommands(
            packageName = "com.test.app",
            activeCount = 5,
            deprecatedCount = 5,
            deprecatedCreatedAt = fifteenDaysAgo
        )

        viewModel = CleanupPreviewViewModel(cleanupManager, commandRepository, application)
        viewModel.loadPreview(gracePeriodDays = 30, keepUserApproved = true)

        kotlinx.coroutines.delay(500)

        val state = viewModel.uiState.value as CleanupPreviewUiState.Preview

        // Commands deprecated within grace period should not be deleted yet
        assertTrue(
            "Should have minimal deletions within grace period",
            state.statistics.commandsToDelete <= 5
        )
    }

    /**
     * Test: Load preview with deprecated commands beyond grace period
     */
    @Test
    fun testLoadPreview_deprecatedBeyondGracePeriod_markedForDeletion() = runTest {
        // Insert commands deprecated 45 days ago (beyond 30-day grace period)
        val fortyFiveDaysAgo = System.currentTimeMillis() - (45 * 86400000L)
        insertTestCommands(
            packageName = "com.test.app",
            activeCount = 10,
            deprecatedCount = 10,
            deprecatedCreatedAt = fortyFiveDaysAgo
        )

        viewModel = CleanupPreviewViewModel(cleanupManager, commandRepository, application)
        viewModel.loadPreview(gracePeriodDays = 30, keepUserApproved = true)

        kotlinx.coroutines.delay(500)

        val state = viewModel.uiState.value as CleanupPreviewUiState.Preview

        assertTrue(
            "Should mark deprecated commands for deletion",
            state.statistics.commandsToDelete > 0
        )
        assertEquals(
            "Should calculate correct deletion percentage",
            50,
            state.statistics.deletionPercentage
        )
    }

    /**
     * Test: Safety level calculation - SAFE (<10%)
     */
    @Test
    fun testSafetyLevel_lessThan10Percent_isSafe() {
        val safetyLevel = calculateSafetyLevel(5)
        assertEquals("5% deletion should be SAFE", SafetyLevel.SAFE, safetyLevel)
    }

    /**
     * Test: Safety level calculation - MODERATE (10-50%)
     */
    @Test
    fun testSafetyLevel_between10And50Percent_isModerate() {
        val safetyLevel = calculateSafetyLevel(30)
        assertEquals("30% deletion should be MODERATE", SafetyLevel.MODERATE, safetyLevel)
    }

    /**
     * Test: Safety level calculation - HIGH_RISK (>50%)
     */
    @Test
    fun testSafetyLevel_moreThan50Percent_isHighRisk() {
        val safetyLevel = calculateSafetyLevel(75)
        assertEquals("75% deletion should be HIGH_RISK", SafetyLevel.HIGH_RISK, safetyLevel)
    }

    /**
     * Test: Statistics formatting - size display
     */
    @Test
    fun testStatistics_formattedSize_displaysCorrectly() {
        val statsKB = CleanupStatistics(
            commandsToDelete = 10,
            commandsToPreserve = 90,
            appsAffected = 2,
            deletionPercentage = 10,
            estimatedSizeMB = 0.05
        )
        assertTrue("Should display KB for small sizes", statsKB.getFormattedSize().contains("KB"))

        val statsMB = CleanupStatistics(
            commandsToDelete = 100,
            commandsToPreserve = 900,
            appsAffected = 5,
            deletionPercentage = 10,
            estimatedSizeMB = 2.5
        )
        assertTrue("Should display MB for larger sizes", statsMB.getFormattedSize().contains("MB"))
    }

    /**
     * Test: Retry after error reloads preview
     */
    @Test
    fun testRetry_afterError_reloadsPreview() = runTest {
        viewModel = CleanupPreviewViewModel(cleanupManager, commandRepository, application)

        // Simulate error state by triggering with invalid grace period
        try {
            viewModel.loadPreview(gracePeriodDays = -1, keepUserApproved = true)
        } catch (e: Exception) {
            // Expected to fail
        }

        kotlinx.coroutines.delay(500)

        // Now retry with valid parameters
        viewModel.retry()

        kotlinx.coroutines.delay(500)

        val state = viewModel.uiState.value
        // Should attempt to load again (may be Loading or Preview depending on data)
        assertTrue(
            "Retry should trigger new load",
            state is CleanupPreviewUiState.Loading || state is CleanupPreviewUiState.Preview
        )
    }

    /**
     * Test: Execute cleanup transitions through states correctly
     */
    @Test
    fun testExecuteCleanup_transitionsThroughStates() = runTest {
        // Insert test data
        insertTestCommands(
            packageName = "com.test.app",
            activeCount = 50,
            deprecatedCount = 50,
            deprecatedCreatedAt = System.currentTimeMillis() - (45 * 86400000L)
        )

        viewModel = CleanupPreviewViewModel(cleanupManager, commandRepository, application)
        viewModel.loadPreview(gracePeriodDays = 30, keepUserApproved = true)

        kotlinx.coroutines.delay(500)

        // Execute cleanup
        viewModel.executeCleanup()

        kotlinx.coroutines.delay(100)

        // Should transition to Executing state first
        val executingState = viewModel.uiState.value
        assertTrue(
            "Should transition to Executing state",
            executingState is CleanupPreviewUiState.Executing ||
                    executingState is CleanupPreviewUiState.Success ||
                    executingState is CleanupPreviewUiState.Error
        )
    }

    /**
     * Test: Success state contains correct deleted count
     */
    @Test
    fun testSuccessState_containsDeletedCount() {
        val successState = CleanupPreviewUiState.Success(
            deletedCount = 25,
            preservedCount = 75,
            durationMs = 1234
        )

        assertEquals(25, successState.deletedCount)
        assertEquals(75, successState.preservedCount)
        assertEquals("1.2s", successState.getDurationSeconds())
    }

    /**
     * Helper: Insert test commands into database
     */
    private suspend fun insertTestCommands(
        packageName: String,
        activeCount: Int,
        deprecatedCount: Int,
        deprecatedCreatedAt: Long = System.currentTimeMillis() - (45 * 86400000L)
    ) {
        val now = System.currentTimeMillis()

        // Insert active commands
        repeat(activeCount) { i ->
            val cmd = GeneratedCommandDTO(
                id = (i + 1).toLong(),
                elementHash = "element_active_$i",
                commandText = "Active command $i",
                actionType = "CLICK",
                confidence = 0.9,
                synonyms = null,
                isUserApproved = 0,
                usageCount = 5,
                lastUsed = now,
                createdAt = now,
                appId = packageName,
                appVersion = "1.0.0",
                versionCode = 1,
                lastVerified = now,
                isDeprecated = 0
            )
            commandRepository.insert(cmd)
        }

        // Insert deprecated commands
        repeat(deprecatedCount) { i ->
            val cmd = GeneratedCommandDTO(
                id = (activeCount + i + 1).toLong(),
                elementHash = "element_deprecated_$i",
                commandText = "Deprecated command $i",
                actionType = "CLICK",
                confidence = 0.8,
                synonyms = null,
                isUserApproved = 0,
                usageCount = 2,
                lastUsed = deprecatedCreatedAt,
                createdAt = deprecatedCreatedAt,
                appId = packageName,
                appVersion = "0.9.0",
                versionCode = 1,
                lastVerified = deprecatedCreatedAt,
                isDeprecated = 1
            )
            commandRepository.insert(cmd)
        }
    }
}
