/**
 * PaginationTest.kt - Tests for database pagination
 *
 * Verifies pagination works correctly for large datasets.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-12
 * Related: VoiceOS-Plan-JITLearnApp-Fixes-51212-V1.md (Phase 5)
 */

package com.augmentalis.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.database.repositories.IGeneratedCommandRepository
import com.augmentalis.database.repositories.impl.SQLDelightGeneratedCommandRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

/**
 * Tests for database pagination
 *
 * Covers:
 * - getAllPaginated with various limits/offsets
 * - getByActionTypePaginated
 * - Edge cases (empty results, offset > count, etc.)
 * - Large datasets (1000+ records)
 */
@RunWith(AndroidJUnit4::class)
class PaginationTest {

    private lateinit var database: VoiceOSDatabase
    private lateinit var repository: IGeneratedCommandRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Create in-memory database for testing
        val driver = app.cash.sqldelight.driver.android.AndroidSqliteDriver(
            schema = VoiceOSDatabase.Schema,
            context = context,
            name = null // null = in-memory database
        )
        database = VoiceOSDatabase(driver)
        repository = SQLDelightGeneratedCommandRepository(database)
    }

    @After
    fun teardown() {
        // Database will be cleaned up when test ends
    }

    @Test
    fun testGetAllPaginatedReturnsCorrectPageSize() = runTest {
        // Insert 50 test commands
        repeat(50) { i ->
            repository.insert(createTestCommand("element_$i", "click_$i"))
        }

        // Get first page (limit 10)
        val page1 = repository.getAllPaginated(limit = 10, offset = 0)
        assertEquals(10, page1.size)

        // Get second page
        val page2 = repository.getAllPaginated(limit = 10, offset = 10)
        assertEquals(10, page2.size)

        // Verify no overlap
        val page1Ids = page1.map { it.id }.toSet()
        val page2Ids = page2.map { it.id }.toSet()
        assertTrue("Pages should not overlap", page1Ids.intersect(page2Ids).isEmpty())
    }

    @Test
    fun testGetAllPaginatedWithOffsetBeyondDatasetReturnsEmpty() = runTest {
        // Insert 10 commands
        repeat(10) { i ->
            repository.insert(createTestCommand("element_$i", "click_$i"))
        }

        // Get page beyond dataset
        val page = repository.getAllPaginated(limit = 10, offset = 100)
        assertTrue("Page should be empty", page.isEmpty())
    }

    @Test
    fun testGetAllPaginatedHandlesPartialLastPage() = runTest {
        // Insert 25 commands
        repeat(25) { i ->
            repository.insert(createTestCommand("element_$i", "click_$i"))
        }

        // Get last page (limit 10, offset 20)
        val lastPage = repository.getAllPaginated(limit = 10, offset = 20)
        assertEquals(5, lastPage.size) // Should only return 5 remaining
    }

    @Test
    fun testGetAllPaginatedWithLimit0ThrowsException() = runTest {
        repeat(10) { i ->
            repository.insert(createTestCommand("element_$i", "click_$i"))
        }

        // Limit must be between 1 and 1000, so 0 should throw IllegalArgumentException
        try {
            repository.getAllPaginated(limit = 0, offset = 0)
            fail("Should throw IllegalArgumentException for limit = 0")
        } catch (e: IllegalArgumentException) {
            assertTrue("Exception message should mention limit",
                e.message?.contains("Limit") == true)
        }
    }

    @Test
    fun testGetByActionTypePaginatedFiltersCorrectly() = runTest {
        // Insert 30 CLICK commands and 20 SCROLL commands
        repeat(30) { i ->
            repository.insert(createTestCommand("element_$i", "click_$i", actionType = "CLICK"))
        }
        repeat(20) { i ->
            repository.insert(createTestCommand("scroll_element_$i", "scroll_$i", actionType = "SCROLL"))
        }

        // Get first page of CLICK commands
        val clickPage1 = repository.getByActionTypePaginated("CLICK", limit = 10, offset = 0)
        assertEquals(10, clickPage1.size)
        assertTrue("All should be CLICK", clickPage1.all { it.actionType == "CLICK" })

        // Get second page of CLICK commands
        val clickPage2 = repository.getByActionTypePaginated("CLICK", limit = 10, offset = 10)
        assertEquals(10, clickPage2.size)
        assertTrue("All should be CLICK", clickPage2.all { it.actionType == "CLICK" })

        // Get SCROLL commands
        val scrollPage = repository.getByActionTypePaginated("SCROLL", limit = 10, offset = 0)
        assertEquals(10, scrollPage.size)
        assertTrue("All should be SCROLL", scrollPage.all { it.actionType == "SCROLL" })
    }

    @Test
    fun testPaginationHandlesLargeDatasetEfficiently() = runTest {
        // Insert 1000 commands
        val insertStart = System.currentTimeMillis()
        val commands = (0 until 1000).map { i ->
            createTestCommand("element_$i", "command_$i")
        }
        repository.insertBatch(commands)
        val insertTime = System.currentTimeMillis() - insertStart

        // Paginated retrieval should be fast
        val queryStart = System.currentTimeMillis()
        val page = repository.getAllPaginated(limit = 50, offset = 500)
        val queryTime = System.currentTimeMillis() - queryStart

        assertEquals(50, page.size)
        assertTrue("Query should be < 50ms", queryTime < 50)
        assertTrue("Batch insert should be < 500ms", insertTime < 500)
    }

    @Test
    fun testPaginationPreservesSortOrder() = runTest {
        // Insert commands with varying usage counts
        repeat(50) { i ->
            val command = createTestCommand("element_$i", "command_$i")
                .copy(usageCount = i.toLong()) // Usage count = index
            repository.insert(command)
        }

        // Get pages (sorted by usageCount DESC)
        val page1 = repository.getAllPaginated(limit = 10, offset = 0)
        val page2 = repository.getAllPaginated(limit = 10, offset = 10)

        // Verify descending order within pages
        for (i in 0 until page1.size - 1) {
            assertTrue(
                "Page 1 should be descending: ${page1[i].usageCount} >= ${page1[i + 1].usageCount}",
                page1[i].usageCount >= page1[i + 1].usageCount
            )
        }

        // Verify page 1 elements have higher usage than page 2
        assertTrue(
            "Page 1 max should be > Page 2 max",
            page1.maxOf { it.usageCount } > page2.maxOf { it.usageCount }
        )
    }

    @Test
    fun testPaginationWithNegativeOffsetThrowsException() = runTest {
        repeat(10) { i ->
            repository.insert(createTestCommand("element_$i", "command_$i"))
        }

        // Offset must be non-negative, so negative offset should throw IllegalArgumentException
        try {
            repository.getAllPaginated(limit = 5, offset = -10)
            fail("Should throw IllegalArgumentException for negative offset")
        } catch (e: IllegalArgumentException) {
            assertTrue("Exception message should mention offset",
                e.message?.contains("Offset") == true)
        }
    }

    @Test
    fun testPaginationIteratesThroughEntireDataset() = runTest {
        val totalCount = 37 // Non-multiple of page size
        repeat(totalCount) { i ->
            repository.insert(createTestCommand("element_$i", "command_$i"))
        }

        val pageSize = 10
        val allIds = mutableSetOf<Long>()
        var offset = 0

        // Iterate through all pages
        while (true) {
            val page = repository.getAllPaginated(limit = pageSize, offset = offset)
            if (page.isEmpty()) break

            page.forEach { allIds.add(it.id) }
            offset += pageSize
        }

        assertEquals("Should retrieve all commands", totalCount, allIds.size)
    }

    // Helper function to create test commands
    private fun createTestCommand(
        elementHash: String,
        commandText: String,
        actionType: String = "CLICK"
    ): GeneratedCommandDTO {
        return GeneratedCommandDTO(
            id = 0, // Auto-generated
            elementHash = elementHash,
            commandText = commandText,
            actionType = actionType,
            confidence = 0.8,
            synonyms = null,
            isUserApproved = 0L, // SQLite boolean: 0 = false, 1 = true
            usageCount = 0,
            lastUsed = null,
            createdAt = System.currentTimeMillis(),
            appId = ""  // Default for tests
        )
    }
}
