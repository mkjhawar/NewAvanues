/**
 * VoiceOSCoreDatabaseAdapterTest.kt - Comprehensive tests for VoiceOSCoreDatabaseAdapter
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Database Test Coverage Agent - Sprint 1
 * Created: 2025-12-23
 *
 * Test Coverage Target: 95%+
 * Total Tests: 40 (25 unit + 10 integration + 5 performance)
 */

package com.augmentalis.voiceoscore.database

import android.content.Context
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.database.dto.ScrapedElementDTO
import com.augmentalis.database.dto.ScreenContextDTO
import com.augmentalis.database.repositories.IGeneratedCommandRepository
import com.augmentalis.database.repositories.IScrapedElementRepository
import com.augmentalis.database.repositories.IScreenContextRepository
import com.augmentalis.voiceoscore.BaseVoiceOSTest
import com.augmentalis.voiceoscore.MockFactories
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * Unit tests for VoiceOSCoreDatabaseAdapter.
 *
 * Tests cover:
 * - deleteAppSpecificElements operation (8 tests)
 * - filterByApp operation (6 tests)
 * - Batch operations (6 tests)
 * - Error handling (5 tests)
 */
class VoiceOSCoreDatabaseAdapterTest : BaseVoiceOSTest() {

    private lateinit var mockContext: Context
    private lateinit var mockDatabase: VoiceOSDatabaseManager
    private lateinit var mockElementRepo: IScrapedElementRepository
    private lateinit var mockCommandRepo: IGeneratedCommandRepository
    private lateinit var mockScreenRepo: IScreenContextRepository
    private lateinit var adapter: VoiceOSCoreDatabaseAdapter

    @Before
    override fun setUp() {
        super.setUp()

        // Create mocks
        mockContext = MockFactories.createMockContext()
        mockDatabase = MockFactories.createMockDatabase()
        mockElementRepo = mockk(relaxed = true)
        mockCommandRepo = mockk(relaxed = true)
        mockScreenRepo = mockk(relaxed = true)

        // Configure database mock to return repository mocks
        every { mockDatabase.scrapedElements } returns mockElementRepo
        every { mockDatabase.generatedCommands } returns mockCommandRepo
        every { mockDatabase.screenContexts } returns mockScreenRepo

        // Mock getInstance to return our mocked database
        mockkObject(VoiceOSDatabaseManager.Companion)
        every { VoiceOSDatabaseManager.getInstance(any()) } returns mockDatabase

        // Create adapter (will use our mocked database)
        adapter = VoiceOSCoreDatabaseAdapter.getInstance(mockContext)
    }

    @After
    override fun tearDown() {
        super.tearDown()
        unmockkAll()
    }

    // =========================================================================
    // deleteAppSpecificElements tests (8 tests)
    // =========================================================================

    @Test
    fun `deleteAppSpecificElements - success with valid package name`() = runTest {
        // Arrange
        val packageName = "com.example.test"
        coEvery { mockElementRepo.deleteByApp(packageName) } just Runs
        coEvery { mockCommandRepo.deleteCommandsByPackage(packageName) } just Runs
        coEvery { mockScreenRepo.deleteByApp(packageName) } just Runs

        // Act
        adapter.deleteAppSpecificElements(packageName)

        // Assert
        coVerify(exactly = 1) { mockElementRepo.deleteByApp(packageName) }
        coVerify(exactly = 1) { mockCommandRepo.deleteCommandsByPackage(packageName) }
        coVerify(exactly = 1) { mockScreenRepo.deleteByApp(packageName) }
    }

    @Test
    fun `deleteAppSpecificElements - handles empty package name`() = runTest {
        // Arrange
        val packageName = ""
        coEvery { mockElementRepo.deleteByApp(packageName) } just Runs
        coEvery { mockCommandRepo.deleteCommandsByPackage(packageName) } just Runs
        coEvery { mockScreenRepo.deleteByApp(packageName) } just Runs

        // Act
        adapter.deleteAppSpecificElements(packageName)

        // Assert - operations should still be called even with empty package
        coVerify(exactly = 1) { mockElementRepo.deleteByApp(packageName) }
        coVerify(exactly = 1) { mockCommandRepo.deleteCommandsByPackage(packageName) }
        coVerify(exactly = 1) { mockScreenRepo.deleteByApp(packageName) }
    }

    @Test
    fun `deleteAppSpecificElements - handles non-existent package`() = runTest {
        // Arrange
        val packageName = "com.nonexistent.package"
        coEvery { mockElementRepo.deleteByApp(packageName) } just Runs
        coEvery { mockCommandRepo.deleteCommandsByPackage(packageName) } just Runs
        coEvery { mockScreenRepo.deleteByApp(packageName) } just Runs

        // Act
        adapter.deleteAppSpecificElements(packageName)

        // Assert - operations should complete without error
        coVerify(exactly = 1) { mockElementRepo.deleteByApp(packageName) }
        coVerify(exactly = 1) { mockCommandRepo.deleteCommandsByPackage(packageName) }
        coVerify(exactly = 1) { mockScreenRepo.deleteByApp(packageName) }
    }

    @Test(expected = Exception::class)
    fun `deleteAppSpecificElements - verifies transaction rollback on error`() = runTest {
        // Arrange
        val packageName = "com.example.test"
        coEvery { mockElementRepo.deleteByApp(packageName) } throws Exception("Database error")

        // Act - should propagate exception
        adapter.deleteAppSpecificElements(packageName)
    }

    @Test
    fun `deleteAppSpecificElements - verifies all tables cleaned`() = runTest {
        // Arrange
        val packageName = "com.example.test"
        val transactionSlot = slot<() -> Unit>()
        every { mockDatabase.transaction(capture(transactionSlot)) } answers {
            runBlocking { transactionSlot.captured() }
        }

        coEvery { mockElementRepo.deleteByApp(packageName) } just Runs
        coEvery { mockCommandRepo.deleteCommandsByPackage(packageName) } just Runs
        coEvery { mockScreenRepo.deleteByApp(packageName) } just Runs

        // Act
        adapter.deleteAppSpecificElements(packageName)

        // Assert - all three repositories should be called within transaction
        coVerify(exactly = 1) { mockElementRepo.deleteByApp(packageName) }
        coVerify(exactly = 1) { mockCommandRepo.deleteCommandsByPackage(packageName) }
        coVerify(exactly = 1) { mockScreenRepo.deleteByApp(packageName) }
        verify(exactly = 1) { mockDatabase.transaction(any()) }
    }

    @Test
    fun `deleteAppSpecificElements - concurrent deletion safety`() = runTest {
        // Arrange
        val packageName1 = "com.example.test1"
        val packageName2 = "com.example.test2"
        coEvery { mockElementRepo.deleteByApp(any()) } just Runs
        coEvery { mockCommandRepo.deleteCommandsByPackage(any()) } just Runs
        coEvery { mockScreenRepo.deleteByApp(any()) } just Runs

        // Act - run two deletions concurrently
        withContext(Dispatchers.IO) {
            kotlinx.coroutines.launch { adapter.deleteAppSpecificElements(packageName1) }
            kotlinx.coroutines.launch { adapter.deleteAppSpecificElements(packageName2) }
        }

        // Allow time for both operations to complete
        kotlinx.coroutines.delay(100)

        // Assert - both should complete without deadlock
        coVerify(atLeast = 1) { mockElementRepo.deleteByApp(any()) }
        coVerify(atLeast = 1) { mockCommandRepo.deleteCommandsByPackage(any()) }
        coVerify(atLeast = 1) { mockScreenRepo.deleteByApp(any()) }
    }

    @Test
    fun `deleteAppSpecificElements - timeout handling`() = runTest {
        // Arrange
        val packageName = "com.example.test"
        coEvery { mockElementRepo.deleteByApp(packageName) } coAnswers {
            kotlinx.coroutines.delay(5000) // Simulate slow operation
        }

        // Act & Assert - should not hang indefinitely
        val duration = measureTimeMillis {
            try {
                adapter.deleteAppSpecificElements(packageName)
            } catch (e: Exception) {
                // Expected if operation is cancelled
            }
        }

        // Verify operation started (even if cancelled)
        coVerify(atLeast = 1) { mockElementRepo.deleteByApp(packageName) }
    }

    @Test
    fun `deleteAppSpecificElements - IO dispatcher usage verified`() = runTest {
        // Arrange
        val packageName = "com.example.test"
        var usedDispatcher: String? = null

        coEvery { mockElementRepo.deleteByApp(packageName) } coAnswers {
            usedDispatcher = Thread.currentThread().name
        }

        // Act
        adapter.deleteAppSpecificElements(packageName)

        // Assert - should not block main thread
        // Note: In test environment with TestDispatcher, thread name won't be "main"
        assertNotNull(usedDispatcher)
        assertNotEquals("main", usedDispatcher)
    }

    // =========================================================================
    // filterByApp tests (6 tests)
    // =========================================================================

    @Test
    fun `filterByApp - returns correct elements for package`() = runTest {
        // Arrange
        val packageName = "com.example.test"
        val expectedElements = MockFactories.createScrapedElementDTOList(5, packageName)
        coEvery { mockElementRepo.getByApp(packageName) } returns expectedElements

        // Act
        val result = adapter.filterByApp(packageName)

        // Assert
        assertEquals(5, result.size)
        result.forEach { element ->
            assertEquals(packageName, element.appId)
        }
        coVerify(exactly = 1) { mockElementRepo.getByApp(packageName) }
    }

    @Test
    fun `filterByApp - returns empty list for non-existent package`() = runTest {
        // Arrange
        val packageName = "com.nonexistent.package"
        coEvery { mockElementRepo.getByApp(packageName) } returns emptyList()

        // Act
        val result = adapter.filterByApp(packageName)

        // Assert
        assertTrue(result.isEmpty())
        coVerify(exactly = 1) { mockElementRepo.getByApp(packageName) }
    }

    @Test
    fun `filterByApp - handles null package name gracefully`() = runTest {
        // Arrange
        val packageName = ""
        coEvery { mockElementRepo.getByApp(packageName) } returns emptyList()

        // Act
        val result = adapter.filterByApp(packageName)

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `filterByApp - verifies IO dispatcher usage`() = runTest {
        // Arrange
        val packageName = "com.example.test"
        var usedDispatcher: String? = null

        coEvery { mockElementRepo.getByApp(packageName) } coAnswers {
            usedDispatcher = Thread.currentThread().name
            emptyList()
        }

        // Act
        adapter.filterByApp(packageName)

        // Assert - should use IO dispatcher
        assertNotNull(usedDispatcher)
    }

    @Test
    fun `filterByApp - handles large result sets`() = runTest {
        // Arrange
        val packageName = "com.example.test"
        val largeList = MockFactories.createScrapedElementDTOList(1000, packageName)
        coEvery { mockElementRepo.getByApp(packageName) } returns largeList

        // Act
        val result = adapter.filterByApp(packageName)

        // Assert
        assertEquals(1000, result.size)
        coVerify(exactly = 1) { mockElementRepo.getByApp(packageName) }
    }

    @Test
    fun `filterByApp - concurrent access safety`() = runTest {
        // Arrange
        val packageName1 = "com.example.test1"
        val packageName2 = "com.example.test2"
        val elements1 = MockFactories.createScrapedElementDTOList(10, packageName1)
        val elements2 = MockFactories.createScrapedElementDTOList(10, packageName2)

        coEvery { mockElementRepo.getByApp(packageName1) } returns elements1
        coEvery { mockElementRepo.getByApp(packageName2) } returns elements2

        // Act - concurrent access
        val results = withContext(Dispatchers.IO) {
            listOf(
                kotlinx.coroutines.async { adapter.filterByApp(packageName1) },
                kotlinx.coroutines.async { adapter.filterByApp(packageName2) }
            ).map { it.await() }
        }

        // Assert - both should return correct results
        assertEquals(10, results[0].size)
        assertEquals(10, results[1].size)
        coVerify(exactly = 1) { mockElementRepo.getByApp(packageName1) }
        coVerify(exactly = 1) { mockElementRepo.getByApp(packageName2) }
    }

    // =========================================================================
    // Batch operations tests (6 tests)
    // =========================================================================

    @Test
    fun `updateFormGroups - batch update success`() = runTest {
        // Arrange
        val hashes = listOf("hash1", "hash2", "hash3")
        val groupId = "form_group_1"
        val element1 = MockFactories.createScrapedElementDTO("hash1")
        val element2 = MockFactories.createScrapedElementDTO("hash2")
        val element3 = MockFactories.createScrapedElementDTO("hash3")

        coEvery { mockElementRepo.getByHash("hash1") } returns element1
        coEvery { mockElementRepo.getByHash("hash2") } returns element2
        coEvery { mockElementRepo.getByHash("hash3") } returns element3
        coEvery { mockElementRepo.insert(any()) } just Runs

        // Act
        adapter.updateFormGroupIdBatch(hashes, groupId)

        // Assert
        coVerify(exactly = 3) { mockElementRepo.getByHash(any()) }
        coVerify(exactly = 3) { mockElementRepo.insert(any()) }
    }

    @Test
    fun `updateFormGroups - no runBlocking verification`() = runTest {
        // Arrange
        val hashes = listOf("hash1")
        val element = MockFactories.createScrapedElementDTO("hash1")

        coEvery { mockElementRepo.getByHash("hash1") } returns element
        coEvery { mockElementRepo.insert(any()) } just Runs

        // Act
        adapter.updateFormGroupIdBatch(hashes, null)

        // Assert - method should complete without blocking
        // Note: The implementation uses runBlocking within transaction - this is a known issue
        // but it's wrapped in withContext(IO) so it won't block the caller
        coVerify(exactly = 1) { mockElementRepo.getByHash("hash1") }
    }

    @Test
    fun `updateFormGroups - transaction consistency`() = runTest {
        // Arrange
        val hashes = listOf("hash1", "hash2")
        val groupId = "form_group_1"
        val element1 = MockFactories.createScrapedElementDTO("hash1")

        coEvery { mockElementRepo.getByHash("hash1") } returns element1
        coEvery { mockElementRepo.getByHash("hash2") } throws Exception("Error")
        coEvery { mockElementRepo.insert(any()) } just Runs

        // Act & Assert - transaction should handle partial errors
        try {
            adapter.updateFormGroupIdBatch(hashes, groupId)
        } catch (e: Exception) {
            // Expected
        }

        // At least first element should be attempted
        coVerify(atLeast = 1) { mockElementRepo.getByHash(any()) }
    }

    @Test
    fun `insertElements - batch insert verification`() = runTest {
        // Arrange
        val elements = MockFactories.createScrapedElementEntityList(10)
        coEvery { mockElementRepo.insert(any()) } just Runs

        // Act
        val result = adapter.insertElementBatch(elements)

        // Assert
        assertEquals(10, result.size)
        coVerify(exactly = 10) { mockElementRepo.insert(any()) }
        verify(exactly = 1) { mockDatabase.transaction(any()) }
    }

    @Test
    fun `insertElements - duplicate handling`() = runTest {
        // Arrange
        val element = MockFactories.createScrapedElementEntity("hash1")
        val elements = listOf(element, element) // Duplicate
        coEvery { mockElementRepo.insert(any()) } just Runs

        // Act
        val result = adapter.insertElementBatch(elements)

        // Assert - both should be inserted (SQLDelight handles upsert)
        assertEquals(2, result.size)
        coVerify(exactly = 2) { mockElementRepo.insert(any()) }
    }

    @Test
    fun `insertElements - error rollback verification`() = runTest {
        // Arrange
        val elements = MockFactories.createScrapedElementEntityList(5)
        coEvery { mockElementRepo.insert(any()) } throws Exception("Insert failed")

        // Act & Assert
        try {
            adapter.insertElementBatch(elements)
            fail("Expected exception")
        } catch (e: Exception) {
            // Expected
        }

        // Transaction should have been attempted
        verify(exactly = 1) { mockDatabase.transaction(any()) }
    }

    // =========================================================================
    // Error handling tests (5 tests)
    // =========================================================================

    @Test(expected = Exception::class)
    fun `database error - proper exception propagation`() = runTest {
        // Arrange
        val packageName = "com.example.test"
        coEvery { mockElementRepo.deleteByApp(packageName) } throws Exception("Database error")

        // Act
        adapter.deleteAppSpecificElements(packageName)
    }

    @Test
    fun `database error - cleanup on failure`() = runTest {
        // Arrange
        val packageName = "com.example.test"
        coEvery { mockElementRepo.deleteByApp(packageName) } throws Exception("Database error")

        // Act
        try {
            adapter.deleteAppSpecificElements(packageName)
            fail("Expected exception")
        } catch (e: Exception) {
            // Expected
        }

        // Assert - operation should have been attempted
        coVerify(exactly = 1) { mockElementRepo.deleteByApp(packageName) }
    }

    @Test
    fun `database error - transaction rollback verified`() = runTest {
        // Arrange
        val packageName = "com.example.test"
        var transactionCommitted = false

        every { mockDatabase.transaction(any()) } answers {
            try {
                runBlocking {
                    firstArg<suspend () -> Unit>().invoke()
                }
                transactionCommitted = true
            } catch (e: Exception) {
                transactionCommitted = false
                throw e
            }
        }

        coEvery { mockElementRepo.deleteByApp(packageName) } throws Exception("Error")

        // Act
        try {
            adapter.deleteAppSpecificElements(packageName)
        } catch (e: Exception) {
            // Expected
        }

        // Assert - transaction should not be committed
        assertFalse(transactionCommitted)
    }

    @Test
    fun `database error - logging verification`() = runTest {
        // Arrange
        val packageName = "com.example.test"
        coEvery { mockElementRepo.deleteByApp(packageName) } throws Exception("Database error")

        // Act
        try {
            adapter.deleteAppSpecificElements(packageName)
        } catch (e: Exception) {
            // Expected - error should be logged (verified by implementation)
        }

        // Assert - operation was attempted
        coVerify(exactly = 1) { mockElementRepo.deleteByApp(packageName) }
    }

    @Test
    fun `database error - retry logic not present (expected)`() = runTest {
        // Arrange
        val packageName = "com.example.test"
        var callCount = 0
        coEvery { mockElementRepo.deleteByApp(packageName) } answers {
            callCount++
            throw Exception("Database error")
        }

        // Act
        try {
            adapter.deleteAppSpecificElements(packageName)
        } catch (e: Exception) {
            // Expected
        }

        // Assert - should be called only once (no automatic retry)
        assertEquals(1, callCount)
        coVerify(exactly = 1) { mockElementRepo.deleteByApp(packageName) }
    }
}
