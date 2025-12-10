/**
 * UserInteractionRepositoryTest.kt - Tests for UserInteraction repository
 *
 * Tests new methods added to IUserInteractionRepository:
 * - getInteractionCount
 * - getSuccessFailureRatio
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-11-27
 */

package com.avanues.database.repositories

import com.avanues.database.dto.UserInteractionDTO
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class UserInteractionRepositoryTest : BaseRepositoryTest() {

    // ==================== Helper Functions ====================

    /**
     * Create test UserInteractionDTO
     */
    private fun createInteraction(
        elementHash: String,
        screenHash: String = "screen-hash",
        interactionType: String = "click",
        timestamp: Long = now()
    ): UserInteractionDTO {
        return UserInteractionDTO(
            id = 0, // Auto-generated
            elementHash = elementHash,
            screenHash = screenHash,
            interactionType = interactionType,
            interactionTime = timestamp,
            visibilityStart = past(5000),
            visibilityDuration = 5000
        )
    }

    // ==================== getInteractionCount Tests ====================

    @Test
    fun testGetInteractionCountEmpty() = runTest {
        val repo = databaseManager.userInteractions

        val count = repo.getInteractionCount("element-1")
        assertEquals(0, count)
    }

    @Test
    fun testGetInteractionCountSingle() = runTest {
        val repo = databaseManager.userInteractions

        repo.insert(createInteraction("element-1"))

        val count = repo.getInteractionCount("element-1")
        assertEquals(1, count)
    }

    @Test
    fun testGetInteractionCountMultiple() = runTest {
        val repo = databaseManager.userInteractions

        // Insert 5 interactions for element-1
        repeat(5) {
            repo.insert(createInteraction("element-1"))
        }

        // Insert 3 interactions for element-2
        repeat(3) {
            repo.insert(createInteraction("element-2"))
        }

        assertEquals(5, repo.getInteractionCount("element-1"))
        assertEquals(3, repo.getInteractionCount("element-2"))
    }

    @Test
    fun testGetInteractionCountDifferentTypes() = runTest {
        val repo = databaseManager.userInteractions

        repo.insert(createInteraction("element-1", interactionType = "click"))
        repo.insert(createInteraction("element-1", interactionType = "long_press"))
        repo.insert(createInteraction("element-1", interactionType = "double_tap"))

        // Should count all interaction types
        val count = repo.getInteractionCount("element-1")
        assertEquals(3, count)
    }

    // ==================== getSuccessFailureRatio Tests ====================

    @Test
    fun testGetSuccessFailureRatioNull() = runTest {
        val repo = databaseManager.userInteractions

        val ratio = repo.getSuccessFailureRatio("element-1")
        assertNull(ratio)
    }

    @Test
    fun testGetSuccessFailureRatioAllSuccessful() = runTest {
        val repo = databaseManager.userInteractions

        // Insert 10 interactions
        repeat(10) {
            repo.insert(createInteraction("element-1"))
        }

        val ratio = repo.getSuccessFailureRatio("element-1")
        assertNotNull(ratio)
        assertEquals(10, ratio.successful)
        assertEquals(0, ratio.failed)
    }

    @Test
    fun testGetSuccessFailureRatioIsolation() = runTest {
        val repo = databaseManager.userInteractions

        // Element 1: 5 interactions
        repeat(5) {
            repo.insert(createInteraction("element-1"))
        }

        // Element 2: 3 interactions
        repeat(3) {
            repo.insert(createInteraction("element-2"))
        }

        // Should only count element-1
        val ratio1 = repo.getSuccessFailureRatio("element-1")
        assertNotNull(ratio1)
        assertEquals(5, ratio1.successful)

        // Should only count element-2
        val ratio2 = repo.getSuccessFailureRatio("element-2")
        assertNotNull(ratio2)
        assertEquals(3, ratio2.successful)
    }

    // ==================== Integration Tests ====================

    @Test
    fun testInteractionCountAfterDelete() = runTest {
        val repo = databaseManager.userInteractions

        val interaction1 = repo.insert(createInteraction("element-1"))
        repo.insert(createInteraction("element-1"))

        assertEquals(2, repo.getInteractionCount("element-1"))

        repo.deleteById(interaction1)

        assertEquals(1, repo.getInteractionCount("element-1"))
    }

    @Test
    fun testInteractionCountAfterDeleteByElement() = runTest {
        val repo = databaseManager.userInteractions

        repeat(5) {
            repo.insert(createInteraction("element-1"))
        }

        assertEquals(5, repo.getInteractionCount("element-1"))

        repo.deleteByElement("element-1")

        assertEquals(0, repo.getInteractionCount("element-1"))
    }

    @Test
    fun testInteractionCountWithTimeRange() = runTest {
        val repo = databaseManager.userInteractions

        // Insert interactions at different times
        repo.insert(createInteraction("element-1", timestamp = past(10000)))
        repo.insert(createInteraction("element-1", timestamp = past(5000)))
        repo.insert(createInteraction("element-1", timestamp = now()))

        // Total count should be 3
        assertEquals(3, repo.getInteractionCount("element-1"))

        // Query within time range should only find recent ones
        val recentInteractions = repo.getByTimeRange(past(6000), now() + 1000)
        assertEquals(2, recentInteractions.size)
    }

    // ==================== Edge Cases ====================

    @Test
    fun testGetInteractionCountEmptyHash() = runTest {
        val repo = databaseManager.userInteractions

        val count = repo.getInteractionCount("")
        assertEquals(0, count)
    }

    @Test
    fun testSuccessFailureRatioAfterClear() = runTest {
        val repo = databaseManager.userInteractions

        repeat(5) {
            repo.insert(createInteraction("element-1"))
        }

        assertNotNull(repo.getSuccessFailureRatio("element-1"))

        repo.deleteAll()

        assertNull(repo.getSuccessFailureRatio("element-1"))
    }
}
