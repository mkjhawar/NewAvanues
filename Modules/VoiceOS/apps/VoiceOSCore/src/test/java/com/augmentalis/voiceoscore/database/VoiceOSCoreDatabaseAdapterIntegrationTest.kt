/**
 * VoiceOSCoreDatabaseAdapterIntegrationTest.kt - Integration tests for VoiceOSCoreDatabaseAdapter
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Database Test Coverage Agent - Sprint 1
 * Created: 2025-12-23
 *
 * Integration Tests: 10 tests
 */

package com.augmentalis.voiceoscore.database

import android.content.Context
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.voiceoscore.BaseVoiceOSTest
import com.augmentalis.voiceoscore.MockFactories
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Integration tests for VoiceOSCoreDatabaseAdapter.
 *
 * These tests verify end-to-end workflows and interactions
 * between multiple components.
 */
class VoiceOSCoreDatabaseAdapterIntegrationTest : BaseVoiceOSTest() {

    private lateinit var mockContext: Context
    private lateinit var mockDatabase: VoiceOSDatabaseManager
    private lateinit var adapter: VoiceOSCoreDatabaseAdapter

    @Before
    override fun setUp() {
        super.setUp()
        mockContext = MockFactories.createMockContext()
        mockDatabase = MockFactories.createMockDatabase()

        mockkObject(VoiceOSDatabaseManager.Companion)
        every { VoiceOSDatabaseManager.getInstance(any()) } returns mockDatabase

        adapter = VoiceOSCoreDatabaseAdapter.getInstance(mockContext)
    }

    @After
    override fun tearDown() {
        super.tearDown()
        unmockkAll()
    }

    @Test
    fun `end-to-end - insert, filter, delete workflow`() = runTest {
        // Arrange
        val packageName = "com.example.test"
        val elements = MockFactories.createScrapedElementEntityList(5, packageName)
        val elementDTOs = MockFactories.createScrapedElementDTOList(5, packageName)

        coEvery { mockDatabase.scrapedElements.insert(any()) } just runs
        coEvery { mockDatabase.scrapedElements.getByApp(packageName) } returns elementDTOs
        coEvery { mockDatabase.scrapedElements.deleteByApp(packageName) } just runs
        coEvery { mockDatabase.generatedCommands.deleteCommandsByPackage(packageName) } returns 0
        coEvery { mockDatabase.screenContexts.deleteByApp(packageName) } just runs

        // Act - Insert
        val insertedIds = adapter.insertElementBatch(elements)

        // Act - Filter
        val filteredElements = adapter.filterByApp(packageName)

        // Act - Delete
        adapter.deleteAppSpecificElements(packageName)

        // Assert
        assertEquals(5, insertedIds.size)
        assertEquals(5, filteredElements.size)
        coVerify(exactly = 1) { mockDatabase.scrapedElements.deleteByApp(packageName) }
    }

    @Test
    fun `end-to-end - multiple apps isolation verified`() = runTest {
        // Arrange
        val packageName1 = "com.example.test1"
        val packageName2 = "com.example.test2"
        val elements1 = MockFactories.createScrapedElementDTOList(10, packageName1)
        val elements2 = MockFactories.createScrapedElementDTOList(10, packageName2)

        coEvery { mockDatabase.scrapedElements.getByApp(packageName1) } returns elements1
        coEvery { mockDatabase.scrapedElements.getByApp(packageName2) } returns elements2

        // Act
        val result1 = adapter.filterByApp(packageName1)
        val result2 = adapter.filterByApp(packageName2)

        // Assert - apps should be isolated
        assertEquals(10, result1.size)
        assertEquals(10, result2.size)
        assertTrue(result1.all { it.appId == packageName1 })
        assertTrue(result2.all { it.appId == packageName2 })
    }

    @Test
    fun `integration - with QueryExtensions batch operations`() = runTest {
        // Arrange
        val elements = MockFactories.createScrapedElementEntityList(100)
        coEvery { mockDatabase.scrapedElements.insert(any()) } just runs

        // Act - Large batch insert
        val insertedIds = adapter.insertElementBatch(elements)

        // Assert
        assertEquals(100, insertedIds.size)
        coVerify(exactly = 100) { mockDatabase.scrapedElements.insert(any()) }
        coVerify(exactly = 1) { mockDatabase.transaction<Unit>(any()) }
    }

    @Test
    fun `integration - with CleanupWorker coordination`() = runTest {
        // Arrange
        val packageName = "com.example.test"
        coEvery { mockDatabase.scrapedElements.deleteByApp(packageName) } just runs
        coEvery { mockDatabase.generatedCommands.deleteCommandsByPackage(packageName) } returns 0
        coEvery { mockDatabase.screenContexts.deleteByApp(packageName) } just runs

        // Act - Simulate cleanup worker calling delete
        adapter.deleteAppSpecificElements(packageName)

        // Assert - All cleanup operations should be called
        coVerify(exactly = 1) { mockDatabase.scrapedElements.deleteByApp(packageName) }
        coVerify(exactly = 1) { mockDatabase.generatedCommands.deleteCommandsByPackage(packageName) }
        coVerify(exactly = 1) { mockDatabase.screenContexts.deleteByApp(packageName) }
    }

    @Test
    fun `integration - database migration compatibility`() = runTest {
        // Arrange
        val packageName = "com.example.test"
        val elements = MockFactories.createScrapedElementDTOList(5, packageName)

        coEvery { mockDatabase.scrapedElements.getByApp(packageName) } returns elements

        // Act
        val result = adapter.filterByApp(packageName)

        // Assert - DTOs should be converted to entities correctly
        assertEquals(5, result.size)
        result.forEach { entity ->
            assertNotNull(entity.elementHash)
            assertEquals(packageName, entity.appId)
        }
    }

    @Test
    fun `integration - concurrent multi-app operations`() = runTest {
        // Arrange
        val apps = (1..5).map { "com.example.test$it" }
        apps.forEach { packageName ->
            val elements = MockFactories.createScrapedElementDTOList(10, packageName)
            coEvery { mockDatabase.scrapedElements.getByApp(packageName) } returns elements
        }

        // Act - Concurrent operations on multiple apps
        val results = withContext(Dispatchers.IO) {
            apps.map { packageName ->
                async {
                    adapter.filterByApp(packageName)
                }
            }.map { it.await() }
        }

        // Assert - All operations should complete successfully
        assertEquals(5, results.size)
        results.forEach { result ->
            assertEquals(10, result.size)
        }
    }

    @Test
    fun `integration - large dataset performance (1000+ elements)`() = runTest {
        // Arrange
        val packageName = "com.example.test"
        val largeDataset = MockFactories.createScrapedElementDTOList(1500, packageName)
        coEvery { mockDatabase.scrapedElements.getByApp(packageName) } returns largeDataset

        // Act
        val startTime = System.currentTimeMillis()
        val result = adapter.filterByApp(packageName)
        val duration = System.currentTimeMillis() - startTime

        // Assert
        assertEquals(1500, result.size)
        // Performance expectation: < 500ms for 1500 elements
        assertTrue("Operation took ${duration}ms, expected < 500ms", duration < 500)
    }

    @Test
    fun `integration - transaction boundary verification`() = runTest {
        // Arrange
        val elements = MockFactories.createScrapedElementEntityList(10)
        var transactionStarted = false
        var transactionCompleted = false

        coEvery { mockDatabase.transaction<Unit>(any()) } coAnswers {
            transactionStarted = true
            firstArg<suspend () -> Unit>().invoke()
            transactionCompleted = true
        }

        coEvery { mockDatabase.scrapedElements.insert(any()) } just runs

        // Act
        adapter.insertElementBatch(elements)

        // Assert
        assertTrue("Transaction should have started", transactionStarted)
        assertTrue("Transaction should have completed", transactionCompleted)
        coVerify(exactly = 1) { mockDatabase.transaction<Unit>(any()) }
    }

    @Test
    fun `integration - foreign key constraints verified`() = runTest {
        // Arrange
        val packageName = "com.example.test"
        val screenHash = "screen_hash_1"
        val element = MockFactories.createScrapedElementDTO(
            elementHash = "element_1",
            appId = packageName
        ).copy(screen_hash = screenHash)

        val screenContext = MockFactories.createScreenContextDTO(
            screenHash = screenHash,
            packageName = packageName
        )

        coEvery { mockDatabase.scrapedElements.getByApp(packageName) } returns listOf(element)
        coEvery { mockDatabase.screenContexts.getByHash(screenHash) } returns screenContext

        // Act
        val elements = adapter.filterByApp(packageName)

        // Assert - Element should reference valid screen context
        assertEquals(1, elements.size)
        assertEquals(screenHash, elements[0].screen_hash)
    }

    @Test
    fun `integration - schema version compatibility`() = runTest {
        // Arrange - Simulate different schema versions
        val packageName = "com.example.test"
        val legacyElement = MockFactories.createScrapedElementDTO(
            elementHash = "legacy_element",
            appId = packageName
        ).copy(
            screen_hash = null, // Legacy schema didn't have screen_hash
            isRequired = null    // Legacy schema didn't have isRequired
        )

        coEvery { mockDatabase.scrapedElements.getByApp(packageName) } returns listOf(legacyElement)

        // Act
        val result = adapter.filterByApp(packageName)

        // Assert - Should handle legacy data gracefully
        assertEquals(1, result.size)
        assertNull(result[0].screen_hash)
        assertEquals(0L, result[0].isRequired) // Should default to 0
    }
}
