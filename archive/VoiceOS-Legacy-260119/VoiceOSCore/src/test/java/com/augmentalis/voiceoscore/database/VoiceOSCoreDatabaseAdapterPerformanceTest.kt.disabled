/**
 * VoiceOSCoreDatabaseAdapterPerformanceTest.kt - Performance tests for VoiceOSCoreDatabaseAdapter
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Database Test Coverage Agent - Sprint 1
 * Created: 2025-12-23
 *
 * Performance Tests: 5 tests
 * Verifies operations meet performance requirements
 */

package com.augmentalis.voiceoscore.database

import android.content.Context
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.voiceoscore.BaseVoiceOSTest
import com.augmentalis.voiceoscore.MockFactories
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * Performance tests for VoiceOSCoreDatabaseAdapter.
 *
 * Tests verify operations meet performance targets:
 * - Delete 1000 elements: < 500ms
 * - Filter 10000 elements: < 200ms
 * - Batch insert 1000 elements: < 1s
 * - No deadlocks under concurrent load
 * - Memory usage < 50MB for large operations
 */
class VoiceOSCoreDatabaseAdapterPerformanceTest : BaseVoiceOSTest() {

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
        // Force garbage collection to clean up test data
        System.gc()
    }

    @Test
    fun `performance - delete 1000 elements under 500ms`() = runTest {
        // Arrange
        val packageName = "com.example.test"
        coEvery { mockDatabase.scrapedElements.deleteByApp(packageName) } coAnswers {
            // Simulate realistic database delete time
            kotlinx.coroutines.delay(100)
        }
        coEvery { mockDatabase.generatedCommands.deleteCommandsByPackage(packageName) } coAnswers {
            kotlinx.coroutines.delay(50)
        }
        coEvery { mockDatabase.screenContexts.deleteByApp(packageName) } coAnswers {
            kotlinx.coroutines.delay(50)
        }

        // Act
        val duration = measureTimeMillis {
            adapter.deleteAppSpecificElements(packageName)
        }

        // Assert
        assertTrue(
            "Delete took ${duration}ms, expected < 500ms",
            duration < 500
        )
        coVerify(exactly = 1) { mockDatabase.scrapedElements.deleteByApp(packageName) }
    }

    @Test
    fun `performance - filter 10000 elements under 200ms`() = runTest {
        // Arrange
        val packageName = "com.example.test"
        val largeDataset = MockFactories.createScrapedElementDTOList(10000, packageName)

        coEvery { mockDatabase.scrapedElements.getByApp(packageName) } returns largeDataset

        // Act
        val duration = measureTimeMillis {
            val result = adapter.filterByApp(packageName)
            assertEquals(10000, result.size)
        }

        // Assert
        assertTrue(
            "Filter took ${duration}ms, expected < 200ms",
            duration < 200
        )
    }

    @Test
    fun `performance - batch insert 1000 elements under 1s`() = runTest {
        // Arrange
        val elements = MockFactories.createScrapedElementEntityList(1000)
        coEvery { mockDatabase.scrapedElements.insert(any()) } just Runs

        // Act
        val duration = measureTimeMillis {
            val result = adapter.insertElementBatch(elements)
            assertEquals(1000, result.size)
        }

        // Assert
        assertTrue(
            "Batch insert took ${duration}ms, expected < 1000ms",
            duration < 1000
        )
        coVerify(exactly = 1000) { mockDatabase.scrapedElements.insert(any()) }
    }

    @Test
    fun `performance - concurrent operations no deadlock`() = runTest {
        // Arrange
        val packages = (1..100).map { "com.example.test$it" }
        packages.forEach { packageName ->
            val elements = MockFactories.createScrapedElementDTOList(10, packageName)
            coEvery { mockDatabase.scrapedElements.getByApp(packageName) } returns elements
        }

        // Act - Run 100 concurrent operations
        val duration = measureTimeMillis {
            val results = withContext(Dispatchers.IO) {
                packages.map { packageName ->
                    kotlinx.coroutines.async {
                        adapter.filterByApp(packageName)
                    }
                }.map { it.await() }
            }

            // Assert - All operations should complete
            assertEquals(100, results.size)
            results.forEach { result ->
                assertEquals(10, result.size)
            }
        }

        // Assert - Should complete without deadlock (< 5 seconds for 100 operations)
        assertTrue(
            "Concurrent operations took ${duration}ms, expected < 5000ms",
            duration < 5000
        )
    }

    @Test
    fun `performance - memory usage under 50MB for large ops`() = runTest {
        // Arrange
        val runtime = Runtime.getRuntime()
        System.gc() // Clean up before measurement
        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()

        val packageName = "com.example.test"
        val largeDataset = MockFactories.createScrapedElementDTOList(5000, packageName)
        coEvery { mockDatabase.scrapedElements.getByApp(packageName) } returns largeDataset

        // Act - Perform large operation
        val result = adapter.filterByApp(packageName)

        // Measure memory after operation
        System.gc() // Force garbage collection
        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
        val memoryUsed = (memoryAfter - memoryBefore) / (1024 * 1024) // Convert to MB

        // Assert
        assertEquals(5000, result.size)
        assertTrue(
            "Memory used: ${memoryUsed}MB, expected < 50MB",
            memoryUsed < 50
        )
    }
}
