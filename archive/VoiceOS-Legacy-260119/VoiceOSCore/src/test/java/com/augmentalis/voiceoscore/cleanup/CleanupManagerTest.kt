/**
 * CleanupManagerTest.kt - Comprehensive unit tests for CleanupManager
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 *
 * Tests all CleanupManager business logic including:
 * - Preview calculation accuracy
 * - Safety limit enforcement (>90% rejection)
 * - Grace period validation (1-365 days)
 * - User-approved preservation
 * - Batch deletion progress
 * - VACUUM threshold trigger (>10%)
 *
 * Target: 90%+ code coverage
 */

package com.augmentalis.voiceoscore.cleanup

import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.database.repositories.IGeneratedCommandRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFailsWith

/**
 * Unit tests for CleanupManager.
 *
 * Uses MockK to mock IGeneratedCommandRepository and kotlinx-coroutines-test
 * for testing suspend functions.
 */
class CleanupManagerTest {

    private lateinit var mockRepo: IGeneratedCommandRepository
    private lateinit var cleanupManager: CleanupManager

    @Before
    fun setup() {
        mockRepo = mockk()
        cleanupManager = CleanupManager(mockRepo)
    }

    // ========== Helper Functions ==========

    /**
     * Create a test command with configurable properties.
     */
    private fun createTestCommand(
        id: Long,
        appId: String = "com.example.app",
        elementHash: String = "hash_$id",
        commandText: String = "Command $id",
        createdAt: Long = System.currentTimeMillis(),
        lastVerified: Long? = System.currentTimeMillis(),
        isDeprecated: Boolean = false,
        isUserApproved: Boolean = false
    ): GeneratedCommandDTO {
        return GeneratedCommandDTO(
            id = id,
            appId = appId,
            elementHash = elementHash,
            commandText = commandText,
            actionType = "CLICK",
            confidence = 0.95,
            usageCount = 0L,
            synonyms = "",
            versionCode = 1L,
            appVersion = "1.0.0",
            createdAt = createdAt,
            lastUsed = null,
            isUserApproved = if (isUserApproved) 1L else 0L,
            lastVerified = lastVerified,
            isDeprecated = if (isDeprecated) 1L else 0L
        )
    }

    // ========== Test 1: Preview Calculation Accuracy ==========

    @Test
    fun `previewCleanup calculates correct statistics for empty database`() = runTest {
        // Given: No deprecated commands
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), any())
        } returns emptyList()

        // When: Preview cleanup
        val preview = cleanupManager.previewCleanup(gracePeriodDays = 30)

        // Then: All statistics are zero
        assertEquals(0, preview.commandsToDelete)
        assertEquals(0, preview.appsAffected.size)
        assertEquals(0L, preview.databaseSizeReduction)
        assertEquals(0L, preview.oldestCommandDate)
        assertEquals(0L, preview.newestCommandDate)
    }

    @Test
    fun `previewCleanup calculates correct statistics for single app`() = runTest {
        // Given: 5 deprecated commands from same app
        val deprecatedCommands = listOf(
            createTestCommand(1, appId = "com.app.test", lastVerified = 1000L, isDeprecated = true),
            createTestCommand(2, appId = "com.app.test", lastVerified = 2000L, isDeprecated = true),
            createTestCommand(3, appId = "com.app.test", lastVerified = 3000L, isDeprecated = true),
            createTestCommand(4, appId = "com.app.test", lastVerified = 4000L, isDeprecated = true),
            createTestCommand(5, appId = "com.app.test", lastVerified = 5000L, isDeprecated = true)
        )

        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), any())
        } returns deprecatedCommands

        // When: Preview cleanup
        val preview = cleanupManager.previewCleanup(gracePeriodDays = 30)

        // Then: Statistics are accurate
        assertEquals(5, preview.commandsToDelete)
        assertEquals(1, preview.appsAffected.size)
        assertEquals("com.app.test", preview.appsAffected[0])
        assertEquals(1000L, preview.databaseSizeReduction) // 5 commands × 200 bytes
        assertEquals(1000L, preview.oldestCommandDate)
        assertEquals(5000L, preview.newestCommandDate)
    }

    @Test
    fun `previewCleanup calculates correct statistics for multiple apps`() = runTest {
        // Given: Deprecated commands from 3 different apps
        val deprecatedCommands = listOf(
            createTestCommand(1, appId = "com.app.one", lastVerified = 1000L, isDeprecated = true),
            createTestCommand(2, appId = "com.app.two", lastVerified = 2000L, isDeprecated = true),
            createTestCommand(3, appId = "com.app.three", lastVerified = 3000L, isDeprecated = true),
            createTestCommand(4, appId = "com.app.one", lastVerified = 4000L, isDeprecated = true),
            createTestCommand(5, appId = "com.app.two", lastVerified = 5000L, isDeprecated = true)
        )

        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), any())
        } returns deprecatedCommands

        // When: Preview cleanup
        val preview = cleanupManager.previewCleanup(gracePeriodDays = 30)

        // Then: Apps are distinct and sorted
        assertEquals(5, preview.commandsToDelete)
        assertEquals(3, preview.appsAffected.size)
        assertEquals(listOf("com.app.one", "com.app.three", "com.app.two"), preview.appsAffected)
    }

    @Test
    fun `previewCleanup handles commands with missing lastVerified timestamps`() = runTest {
        // Given: Commands with mix of lastVerified and createdAt
        val deprecatedCommands = listOf(
            createTestCommand(1, createdAt = 1000L, lastVerified = null, isDeprecated = true),
            createTestCommand(2, createdAt = 2000L, lastVerified = 3000L, isDeprecated = true),
            createTestCommand(3, createdAt = 4000L, lastVerified = null, isDeprecated = true)
        )

        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), any())
        } returns deprecatedCommands

        // When: Preview cleanup
        val preview = cleanupManager.previewCleanup(gracePeriodDays = 30)

        // Then: Falls back to createdAt for date range
        assertEquals(3, preview.commandsToDelete)
        assertEquals(1000L, preview.oldestCommandDate) // From createdAt of command 1
        assertEquals(4000L, preview.newestCommandDate) // From createdAt of command 3
    }

    // ========== Test 2: Safety Limit Enforcement (>90% rejection) ==========

    @Test
    fun `executeCleanup rejects deletion when exceeding 90 percent safety limit`() = runTest {
        // Given: Total 100 commands, 95 deprecated (95%)
        coEvery { mockRepo.count() } returns 100L

        val deprecatedCommands = (1..95).map {
            createTestCommand(it.toLong(), isDeprecated = true)
        }
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), any())
        } returns deprecatedCommands

        // When/Then: Throws IllegalStateException
        val exception = assertFailsWith<IllegalStateException> {
            cleanupManager.executeCleanup(gracePeriodDays = 30, dryRun = false)
        }

        assertTrue(exception.message!!.contains("Safety limit exceeded"))
        assertTrue(exception.message!!.contains("95% > 90%"))

        // Verify no deletion was attempted
        coVerify(exactly = 0) { mockRepo.deleteDeprecatedCommands(any(), any()) }
    }

    @Test
    fun `executeCleanup allows deletion at exactly 90 percent`() = runTest {
        // Given: Total 100 commands, 90 deprecated (exactly 90%)
        coEvery { mockRepo.count() } returns 100L

        val deprecatedCommands = (1..90).map {
            createTestCommand(it.toLong(), isDeprecated = true)
        }
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), any())
        } returns deprecatedCommands

        coEvery {
            mockRepo.deleteDeprecatedCommands(any(), any())
        } returns 90

        // When: Execute cleanup
        val result = cleanupManager.executeCleanup(gracePeriodDays = 30, dryRun = false)

        // Then: Deletion proceeds successfully
        assertEquals(90, result.deletedCount)
        assertEquals(10, result.preservedCount)
        assertEquals(0, result.errors.size)

        coVerify(exactly = 1) { mockRepo.deleteDeprecatedCommands(any(), any()) }
    }

    @Test
    fun `executeCleanup allows deletion below 90 percent`() = runTest {
        // Given: Total 100 commands, 50 deprecated (50%)
        coEvery { mockRepo.count() } returns 100L

        val deprecatedCommands = (1..50).map {
            createTestCommand(it.toLong(), isDeprecated = true)
        }
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), any())
        } returns deprecatedCommands

        coEvery {
            mockRepo.deleteDeprecatedCommands(any(), any())
        } returns 50

        // When: Execute cleanup
        val result = cleanupManager.executeCleanup(gracePeriodDays = 30, dryRun = false)

        // Then: Deletion proceeds successfully
        assertEquals(50, result.deletedCount)
        assertEquals(50, result.preservedCount)
        assertEquals(0, result.errors.size)
    }

    @Test
    fun `executeCleanup handles empty database safely`() = runTest {
        // Given: Empty database
        coEvery { mockRepo.count() } returns 0L
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), any())
        } returns emptyList()
        coEvery { mockRepo.deleteDeprecatedCommands(any(), any()) } returns 0

        // When: Execute cleanup
        val result = cleanupManager.executeCleanup(gracePeriodDays = 30, dryRun = false)

        // Then: No errors, no deletions
        assertEquals(0, result.deletedCount)
        assertEquals(0, result.preservedCount)
        assertEquals(0, result.errors.size)
    }

    // ========== Test 3: Grace Period Validation (1-365 days) ==========

    @Test
    fun `previewCleanup rejects grace period less than 1 day`() = runTest {
        // When/Then: Throws IllegalArgumentException
        val exception = assertFailsWith<IllegalArgumentException> {
            cleanupManager.previewCleanup(gracePeriodDays = 0)
        }

        assertTrue(exception.message!!.contains("gracePeriodDays must be between 1 and 365"))
    }

    @Test
    fun `previewCleanup rejects grace period greater than 365 days`() = runTest {
        // When/Then: Throws IllegalArgumentException
        val exception = assertFailsWith<IllegalArgumentException> {
            cleanupManager.previewCleanup(gracePeriodDays = 366)
        }

        assertTrue(exception.message!!.contains("gracePeriodDays must be between 1 and 365"))
    }

    @Test
    fun `previewCleanup accepts grace period of 1 day`() = runTest {
        // Given: Mock empty result
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), any())
        } returns emptyList()

        // When: Preview with 1 day
        val preview = cleanupManager.previewCleanup(gracePeriodDays = 1)

        // Then: No exception thrown
        assertNotNull(preview)
    }

    @Test
    fun `previewCleanup accepts grace period of 365 days`() = runTest {
        // Given: Mock empty result
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), any())
        } returns emptyList()

        // When: Preview with 365 days
        val preview = cleanupManager.previewCleanup(gracePeriodDays = 365)

        // Then: No exception thrown
        assertNotNull(preview)
    }

    @Test
    fun `executeCleanup validates grace period correctly`() = runTest {
        // When/Then: Invalid grace period throws exception
        assertFailsWith<IllegalArgumentException> {
            cleanupManager.executeCleanup(gracePeriodDays = -1)
        }

        assertFailsWith<IllegalArgumentException> {
            cleanupManager.executeCleanup(gracePeriodDays = 400)
        }
    }

    @Test
    fun `previewCleanup calculates correct cutoff timestamp for grace period`() = runTest {
        // Given: Mock repository
        val olderThanSlot = slot<Long>()
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(
                packageName = "",
                olderThan = capture(olderThanSlot),
                keepUserApproved = any(),
                limit = any()
            )
        } returns emptyList()

        // When: Preview with 30 days grace period
        val beforeCall = System.currentTimeMillis()
        cleanupManager.previewCleanup(gracePeriodDays = 30)
        val afterCall = System.currentTimeMillis()

        // Then: Cutoff timestamp is approximately 30 days ago
        val expectedCutoff = beforeCall - (30 * 86400000L)
        val actualCutoff = olderThanSlot.captured

        // Allow 1 second tolerance for test execution time
        assertTrue(actualCutoff >= expectedCutoff - 1000)
        assertTrue(actualCutoff <= afterCall - (30 * 86400000L) + 1000)
    }

    // ========== Test 4: User-Approved Preservation ==========

    @Test
    fun `previewCleanup passes keepUserApproved flag to repository`() = runTest {
        // Given: Mock repository
        val keepUserApprovedSlot = slot<Boolean>()
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(
                packageName = any(),
                olderThan = any(),
                keepUserApproved = capture(keepUserApprovedSlot),
                limit = any()
            )
        } returns emptyList()

        // When: Preview with keepUserApproved = true
        cleanupManager.previewCleanup(gracePeriodDays = 30, keepUserApproved = true)

        // Then: Repository receives correct flag
        assertTrue(keepUserApprovedSlot.captured)

        // When: Preview with keepUserApproved = false
        cleanupManager.previewCleanup(gracePeriodDays = 30, keepUserApproved = false)

        // Then: Repository receives correct flag
        assertFalse(keepUserApprovedSlot.captured)
    }

    @Test
    fun `executeCleanup passes keepUserApproved flag to repository`() = runTest {
        // Given: Mock repository
        coEvery { mockRepo.count() } returns 100L
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), any())
        } returns (1..10).map { createTestCommand(it.toLong()) }

        val keepUserApprovedSlot = slot<Boolean>()
        coEvery {
            mockRepo.deleteDeprecatedCommands(any(), capture(keepUserApprovedSlot))
        } returns 10

        // When: Execute with keepUserApproved = true
        cleanupManager.executeCleanup(gracePeriodDays = 30, keepUserApproved = true, dryRun = false)

        // Then: Repository receives correct flag
        assertTrue(keepUserApprovedSlot.captured)

        // When: Execute with keepUserApproved = false
        cleanupManager.executeCleanup(gracePeriodDays = 30, keepUserApproved = false, dryRun = false)

        // Then: Repository receives correct flag
        assertFalse(keepUserApprovedSlot.captured)
    }

    @Test
    fun `executeCleanup result contains keepUserApproved flag`() = runTest {
        // Given: Mock repository
        coEvery { mockRepo.count() } returns 100L
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), any())
        } returns (1..10).map { createTestCommand(it.toLong()) }
        coEvery { mockRepo.deleteDeprecatedCommands(any(), any()) } returns 10

        // When: Execute with keepUserApproved = true
        val resultTrue = cleanupManager.executeCleanup(gracePeriodDays = 30, keepUserApproved = true)

        // Then: Result reflects flag
        assertTrue(resultTrue.keepUserApproved)

        // When: Execute with keepUserApproved = false
        val resultFalse = cleanupManager.executeCleanup(gracePeriodDays = 30, keepUserApproved = false)

        // Then: Result reflects flag
        assertFalse(resultFalse.keepUserApproved)
    }

    // ========== Test 5: Batch Deletion Progress ==========

    @Test
    fun `executeCleanupWithProgress calls progress callback for each batch`() = runTest {
        // Given: 2500 commands to delete (3 batches with size 1000)
        coEvery { mockRepo.count() } returns 10000L

        val deprecatedCommands = (1..2500).map {
            createTestCommand(it.toLong(), isDeprecated = true)
        }

        // previewCleanup uses limit = 10000, returns all 2500
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), limit = 10000)
        } returns deprecatedCommands

        // Batch deletion uses limit = 1000
        // First call returns batch 1 (1000 commands)
        // Second call returns batch 2 (1000 commands)
        // Third call returns batch 3 (500 commands)
        // Fourth call returns empty (done)
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), limit = 1000)
        } returnsMany listOf(
            deprecatedCommands.subList(0, 1000),
            deprecatedCommands.subList(1000, 2000),
            deprecatedCommands.subList(2000, 2500),
            emptyList()
        )

        coEvery { mockRepo.deleteById(any()) } returns Unit

        // Track progress calls
        val progressCalls = mutableListOf<Triple<Int, Int, Int>>() // deleted, total, batch

        // When: Execute with progress callback
        val result = cleanupManager.executeCleanupWithProgress(
            gracePeriodDays = 30,
            batchSize = 1000,
            autoVacuum = false,
            progressCallback = { deleted, total, batch, totalBatches ->
                progressCalls.add(Triple(deleted, total, batch))
            }
        )

        // Then: Progress callback called after each batch
        assertEquals(3, progressCalls.size)

        // Batch 1: 1000 deleted out of 2500
        assertEquals(1000, progressCalls[0].first)
        assertEquals(2500, progressCalls[0].second)
        assertEquals(1, progressCalls[0].third)

        // Batch 2: 2000 deleted out of 2500
        assertEquals(2000, progressCalls[1].first)
        assertEquals(2500, progressCalls[1].second)
        assertEquals(2, progressCalls[1].third)

        // Batch 3: 2500 deleted out of 2500
        assertEquals(2500, progressCalls[2].first)
        assertEquals(2500, progressCalls[2].second)
        assertEquals(3, progressCalls[2].third)

        // Final result
        assertEquals(2500, result.deletedCount)
    }

    @Test
    fun `executeCleanupWithProgress validates batch size range`() = runTest {
        // When/Then: Batch size below minimum throws exception
        assertFailsWith<IllegalArgumentException> {
            cleanupManager.executeCleanupWithProgress(batchSize = 99)
        }

        // When/Then: Batch size above maximum throws exception
        assertFailsWith<IllegalArgumentException> {
            cleanupManager.executeCleanupWithProgress(batchSize = 10001)
        }
    }

    @Test
    fun `executeCleanupWithProgress accepts valid batch sizes`() = runTest {
        // Given: Mock repository
        coEvery { mockRepo.count() } returns 100L
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), any())
        } returns emptyList()

        // When: Execute with minimum batch size
        val result1 = cleanupManager.executeCleanupWithProgress(batchSize = 100)
        assertNotNull(result1)

        // When: Execute with maximum batch size
        val result2 = cleanupManager.executeCleanupWithProgress(batchSize = 10000)
        assertNotNull(result2)
    }

    @Test
    fun `executeCleanupWithProgress handles partial batch at end`() = runTest {
        // Given: 1250 commands (1 full batch + 1 partial batch of 250)
        coEvery { mockRepo.count() } returns 10000L

        val deprecatedCommands = (1..1250).map {
            createTestCommand(it.toLong(), isDeprecated = true)
        }

        // previewCleanup uses limit = 10000
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), limit = 10000)
        } returns deprecatedCommands

        // Batch deletion uses limit = 1000
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), limit = 1000)
        } returnsMany listOf(
            deprecatedCommands.subList(0, 1000),
            deprecatedCommands.subList(1000, 1250),
            emptyList()
        )

        coEvery { mockRepo.deleteById(any()) } returns Unit

        var lastBatchSize = 0

        // When: Execute with progress callback
        cleanupManager.executeCleanupWithProgress(
            batchSize = 1000,
            autoVacuum = false,
            progressCallback = { deleted, _, _, _ ->
                lastBatchSize = deleted
            }
        )

        // Then: Final progress shows 1250 (full + partial)
        assertEquals(1250, lastBatchSize)
    }

    // ========== Test 6: VACUUM Threshold Trigger (>10%) ==========

    @Test
    fun `executeCleanupWithProgress triggers VACUUM when deleting more than 10 percent`() = runTest {
        // Given: Total 1000 commands, deleting 150 (15%)
        coEvery { mockRepo.count() } returns 1000L

        val deprecatedCommands = (1..150).map {
            createTestCommand(it.toLong(), isDeprecated = true)
        }

        // previewCleanup uses limit = 10000
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), limit = 10000)
        } returns deprecatedCommands

        // Batch deletion uses default limit = 1000
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), limit = 1000)
        } returnsMany listOf(deprecatedCommands, emptyList())

        coEvery { mockRepo.deleteById(any()) } returns Unit
        coEvery { mockRepo.vacuumDatabase() } returns Unit

        // When: Execute with autoVacuum = true
        val result = cleanupManager.executeCleanupWithProgress(
            gracePeriodDays = 30,
            autoVacuum = true
        )

        // Then: VACUUM was executed
        assertTrue(result.vacuumExecuted)
        assertTrue(result.vacuumDurationMs >= 0)

        coVerify(exactly = 1) { mockRepo.vacuumDatabase() }
    }

    @Test
    fun `executeCleanupWithProgress does not trigger VACUUM when deleting less than 10 percent`() = runTest {
        // Given: Total 1000 commands, deleting 50 (5%)
        coEvery { mockRepo.count() } returns 1000L

        val deprecatedCommands = (1..50).map {
            createTestCommand(it.toLong(), isDeprecated = true)
        }

        // previewCleanup uses limit = 10000
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), limit = 10000)
        } returns deprecatedCommands

        // Batch deletion uses default limit = 1000
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), limit = 1000)
        } returnsMany listOf(deprecatedCommands, emptyList())

        coEvery { mockRepo.deleteById(any()) } returns Unit

        // When: Execute with autoVacuum = true
        val result = cleanupManager.executeCleanupWithProgress(
            gracePeriodDays = 30,
            autoVacuum = true
        )

        // Then: VACUUM was NOT executed
        assertFalse(result.vacuumExecuted)
        assertEquals(0L, result.vacuumDurationMs)

        coVerify(exactly = 0) { mockRepo.vacuumDatabase() }
    }

    @Test
    fun `executeCleanupWithProgress triggers VACUUM at exactly 10 percent threshold`() = runTest {
        // Given: Total 1000 commands, deleting exactly 100 (10%)
        coEvery { mockRepo.count() } returns 1000L

        val deprecatedCommands = (1..100).map {
            createTestCommand(it.toLong(), isDeprecated = true)
        }

        // previewCleanup uses limit = 10000
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), limit = 10000)
        } returns deprecatedCommands

        // Batch deletion uses default limit = 1000
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), limit = 1000)
        } returnsMany listOf(deprecatedCommands, emptyList())

        coEvery { mockRepo.deleteById(any()) } returns Unit
        coEvery { mockRepo.vacuumDatabase() } returns Unit

        // When: Execute with autoVacuum = true
        val result = cleanupManager.executeCleanupWithProgress(
            gracePeriodDays = 30,
            autoVacuum = true
        )

        // Then: VACUUM was executed (>= threshold)
        assertTrue(result.vacuumExecuted)

        coVerify(exactly = 1) { mockRepo.vacuumDatabase() }
    }

    @Test
    fun `executeCleanupWithProgress skips VACUUM when autoVacuum is false`() = runTest {
        // Given: Total 1000 commands, deleting 500 (50%)
        coEvery { mockRepo.count() } returns 1000L

        val deprecatedCommands = (1..500).map {
            createTestCommand(it.toLong(), isDeprecated = true)
        }

        // previewCleanup uses limit = 10000
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), limit = 10000)
        } returns deprecatedCommands

        // Batch deletion uses default limit = 1000
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), limit = 1000)
        } returnsMany listOf(deprecatedCommands, emptyList())

        coEvery { mockRepo.deleteById(any()) } returns Unit

        // When: Execute with autoVacuum = false
        val result = cleanupManager.executeCleanupWithProgress(
            gracePeriodDays = 30,
            autoVacuum = false
        )

        // Then: VACUUM was NOT executed
        assertFalse(result.vacuumExecuted)

        coVerify(exactly = 0) { mockRepo.vacuumDatabase() }
    }

    @Test
    fun `executeCleanupWithProgress handles VACUUM failure gracefully`() = runTest {
        // Given: VACUUM will fail
        coEvery { mockRepo.count() } returns 1000L

        val deprecatedCommands = (1..150).map {
            createTestCommand(it.toLong(), isDeprecated = true)
        }

        // previewCleanup uses limit = 10000
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), limit = 10000)
        } returns deprecatedCommands

        // Batch deletion uses default limit = 1000
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), limit = 1000)
        } returnsMany listOf(deprecatedCommands, emptyList())

        coEvery { mockRepo.deleteById(any()) } returns Unit
        coEvery { mockRepo.vacuumDatabase() } throws Exception("VACUUM failed: disk full")

        // When: Execute with autoVacuum = true
        val result = cleanupManager.executeCleanupWithProgress(
            gracePeriodDays = 30,
            autoVacuum = true
        )

        // Then: VACUUM failure recorded in errors
        assertFalse(result.vacuumExecuted)
        assertEquals(150, result.deletedCount) // Deletion still succeeded
        assertTrue(result.errors.any { it.contains("VACUUM failed") })
    }

    // ========== Test 7: Dry Run Mode ==========

    @Test
    fun `executeCleanup in dry run mode does not delete commands`() = runTest {
        // Given: 50 commands to delete
        coEvery { mockRepo.count() } returns 100L

        val deprecatedCommands = (1..50).map {
            createTestCommand(it.toLong(), isDeprecated = true)
        }
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), any())
        } returns deprecatedCommands

        // When: Execute in dry run mode
        val result = cleanupManager.executeCleanup(gracePeriodDays = 30, dryRun = true)

        // Then: Reports what would be deleted but doesn't actually delete
        assertEquals(50, result.deletedCount)
        assertEquals(50, result.preservedCount)

        // Verify no actual deletion
        coVerify(exactly = 0) { mockRepo.deleteDeprecatedCommands(any(), any()) }
    }

    // ========== Test 8: Error Handling ==========

    @Test
    fun `executeCleanup handles deletion count mismatch`() = runTest {
        // Given: Preview shows 50, but actual deletion returns 65 (mismatch > 10)
        coEvery { mockRepo.count() } returns 100L

        val deprecatedCommands = (1..50).map {
            createTestCommand(it.toLong(), isDeprecated = true)
        }
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), any())
        } returns deprecatedCommands

        coEvery {
            mockRepo.deleteDeprecatedCommands(any(), any())
        } returns 65 // Mismatch

        // When: Execute cleanup
        val result = cleanupManager.executeCleanup(gracePeriodDays = 30, dryRun = false)

        // Then: Warning added to errors
        assertEquals(65, result.deletedCount)
        assertTrue(result.errors.any { it.contains("differs from preview") })
    }

    @Test
    fun `executeCleanupWithProgress handles individual deletion failures`() = runTest {
        // Given: Some deletions will fail
        coEvery { mockRepo.count() } returns 1000L

        val deprecatedCommands = (1..10).map {
            createTestCommand(it.toLong(), isDeprecated = true)
        }

        // previewCleanup uses limit = 10000
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), limit = 10000)
        } returns deprecatedCommands

        // Batch deletion uses default limit = 1000
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), limit = 1000)
        } returnsMany listOf(deprecatedCommands, emptyList())

        // Commands 3, 5, 7 will fail to delete
        coEvery { mockRepo.deleteById(match { it in listOf(3L, 5L, 7L) }) } throws Exception("Delete failed")
        coEvery { mockRepo.deleteById(match { it !in listOf(3L, 5L, 7L) }) } returns Unit

        // When: Execute cleanup
        val result = cleanupManager.executeCleanupWithProgress(
            gracePeriodDays = 30,
            autoVacuum = false
        )

        // Then: Partial success with errors
        assertEquals(7, result.deletedCount) // 10 - 3 failures
        assertEquals(3, result.errors.size)
        assertTrue(result.errors.all { it.contains("Failed to delete command") })
    }

    // ========== Test 9: Manual VACUUM ==========

    @Test
    fun `manualVacuum executes and returns duration`() = runTest {
        // Given: Mock VACUUM
        coEvery { mockRepo.vacuumDatabase() } returns Unit

        // When: Execute manual VACUUM
        val duration = cleanupManager.manualVacuum()

        // Then: Duration is non-negative
        assertTrue(duration >= 0)

        coVerify(exactly = 1) { mockRepo.vacuumDatabase() }
    }

    @Test
    fun `manualVacuum propagates exceptions`() = runTest {
        // Given: VACUUM will fail
        coEvery { mockRepo.vacuumDatabase() } throws Exception("VACUUM error")

        // When/Then: Exception propagates
        assertFailsWith<Exception> {
            cleanupManager.manualVacuum()
        }
    }

    // ========== Test 10: Result Metadata ==========

    @Test
    fun `executeCleanup result contains grace period and duration`() = runTest {
        // Given: Mock repository
        coEvery { mockRepo.count() } returns 100L
        coEvery {
            mockRepo.getDeprecatedCommandsForCleanup(any(), any(), any(), any())
        } returns (1..10).map { createTestCommand(it.toLong()) }
        coEvery { mockRepo.deleteDeprecatedCommands(any(), any()) } returns 10

        // When: Execute cleanup
        val result = cleanupManager.executeCleanup(gracePeriodDays = 45, dryRun = false)

        // Then: Metadata is correct
        assertEquals(45, result.gracePeriodDays)
        assertTrue(result.durationMs >= 0)
    }
}
