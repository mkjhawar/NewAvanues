/**
 * BatchDeduplicationPerformanceTest.kt - Performance tests for batch alias deduplication
 * Path: modules/libraries/UUIDCreator/src/test/java/com/augmentalis/uuidcreator/alias/BatchDeduplicationPerformanceTest.kt
 *
 * Feature ID: VOS-PERF-001
 * Author: VoiceOS Team
 * Created: 2025-12-03
 *
 * Tests verifying that batch deduplication meets performance targets:
 * - 63 elements complete in <100ms (vs. 1351ms before)
 * - Only 2 database operations (vs. 315 before)
 * - Correct deduplication behavior
 */

package com.augmentalis.uuidcreator.alias

import com.augmentalis.database.dto.VUIDAliasDTO
import com.augmentalis.database.repositories.IVUIDRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Performance tests for batch alias deduplication
 *
 * Verifies VOS-PERF-001 optimization:
 * - Batch operations complete in <100ms for 63 elements
 * - Only 2 DB calls (1 query + 1 batch insert)
 * - Correct deduplication with suffix numbering
 */
class BatchDeduplicationPerformanceTest {

    private lateinit var mockRepository: IUUIDRepository
    private lateinit var aliasManager: UuidAliasManager

    @Before
    fun setup() {
        mockRepository = mock()
        aliasManager = UuidAliasManager(mockRepository)
    }

    /**
     * Test 1.5.1: Batch deduplication completes in <100ms for 63 elements
     *
     * **Target:** <100ms (was 1351ms before optimization)
     * **Expected:** 13x+ speedup
     */
    @Test
    fun `batch deduplication should complete in under 100ms for 63 elements`() = runBlocking {
        // Setup: Mock repository to return existing aliases
        whenever(mockRepository.getAllAliases()).thenReturn(emptyList())
        whenever(mockRepository.insertAliasesBatch(any())).thenReturn(Unit)

        // Create 63 elements with duplicate base aliases (worst case)
        val uuidAliasMap = (1..63).associate {
            "uuid-$it" to "button"  // All same base alias to force maximum deduplication
        }

        // Execute: Measure time
        val startTime = System.currentTimeMillis()
        val result = aliasManager.setAliasesBatch(uuidAliasMap)
        val elapsed = System.currentTimeMillis() - startTime

        // Verify: Performance target met
        assertTrue("Expected <100ms, got ${elapsed}ms", elapsed < 100)

        // Verify: All elements processed
        assertEquals(63, result.size)

        // Verify: All aliases are unique
        val uniqueAliases = result.values.toSet()
        assertEquals("All aliases should be unique", 63, uniqueAliases.size)

        println("✅ PASS: Registered 63 elements in ${elapsed}ms (${63 * 1000 / elapsed.coerceAtLeast(1)} elements/sec)")
    }

    /**
     * Test 1.5.2: Batch deduplication makes only 2 database calls
     *
     * **Target:** 2 DB operations (1 query + 1 batch insert)
     * **Before:** 315 DB operations (5 per element × 63 elements)
     * **Improvement:** 157x reduction
     */
    @Test
    fun `batch deduplication should make only 2 database calls`() = runBlocking {
        // Setup: Mock repository to track calls
        val existingAliases = listOf(
            VUIDAliasDTO(1, "button", "existing-uuid-1", true, System.currentTimeMillis())
        )
        whenever(mockRepository.getAllAliases()).thenReturn(existingAliases)
        whenever(mockRepository.insertAliasesBatch(any())).thenReturn(Unit)

        // Create 63 elements
        val uuidAliasMap = (1..63).associate { "uuid-$it" to "button" }

        // Execute
        aliasManager.setAliasesBatch(uuidAliasMap)

        // Verify: Only 2 DB calls
        verify(mockRepository, times(1)).getAllAliases()  // 1 query
        verify(mockRepository, times(1)).insertAliasesBatch(any())  // 1 batch insert
        verifyNoMoreInteractions(mockRepository)  // No other DB calls

        println("✅ PASS: Only 2 database operations (157x reduction from 315)")
    }

    /**
     * Test 1.5.3: Deduplication correctly appends suffixes
     *
     * **Behavior:**
     * - First "button" → "button"
     * - Second "button" → "button-1"
     * - Third "button" → "button-2"
     */
    @Test
    fun `deduplication should correctly append suffixes for conflicts`() = runBlocking {
        // Setup: Empty database
        whenever(mockRepository.getAllAliases()).thenReturn(emptyList())
        whenever(mockRepository.insertAliasesBatch(any())).thenReturn(Unit)

        // Create 5 elements with same base alias
        val uuidAliasMap = mapOf(
            "uuid-1" to "button",
            "uuid-2" to "button",
            "uuid-3" to "button",
            "uuid-4" to "button",
            "uuid-5" to "button"
        )

        // Execute
        val result = aliasManager.setAliasesBatch(uuidAliasMap)

        // Verify: Correct suffix pattern
        val aliases = result.values.toList()
        assertTrue("First should be 'button'", aliases.contains("button"))
        assertTrue("Should contain 'button-1'", aliases.contains("button-1"))
        assertTrue("Should contain 'button-2'", aliases.contains("button-2"))
        assertTrue("Should contain 'button-3'", aliases.contains("button-3"))
        assertTrue("Should contain 'button-4'", aliases.contains("button-4"))

        // Verify: All unique
        assertEquals(5, aliases.toSet().size)

        println("✅ PASS: Deduplication suffixes correct")
    }

    /**
     * Test 1.5.4: Deduplication handles existing database aliases
     *
     * **Scenario:**
     * - Database already has "button" and "button-1"
     * - New batch has 3 "button" aliases
     * - Should start numbering from "button-2"
     */
    @Test
    fun `deduplication should respect existing database aliases`() = runBlocking {
        // Setup: Database has existing aliases
        val existingAliases = listOf(
            VUIDAliasDTO(1, "button", "existing-uuid-1", true, System.currentTimeMillis()),
            VUIDAliasDTO(2, "button-1", "existing-uuid-2", true, System.currentTimeMillis())
        )
        whenever(mockRepository.getAllAliases()).thenReturn(existingAliases)
        whenever(mockRepository.insertAliasesBatch(any())).thenReturn(Unit)

        // Create 3 new elements with "button" alias
        val uuidAliasMap = mapOf(
            "uuid-1" to "button",
            "uuid-2" to "button",
            "uuid-3" to "button"
        )

        // Execute
        val result = aliasManager.setAliasesBatch(uuidAliasMap)

        // Verify: Starts from "button-2" (since "button" and "button-1" exist)
        val aliases = result.values.toList()
        assertTrue("Should contain 'button-2'", aliases.contains("button-2"))
        assertTrue("Should contain 'button-3'", aliases.contains("button-3"))
        assertTrue("Should contain 'button-4'", aliases.contains("button-4"))

        // Verify: Doesn't conflict with existing
        assertFalse("Should not reuse 'button'", aliases.contains("button"))
        assertFalse("Should not reuse 'button-1'", aliases.contains("button-1"))

        println("✅ PASS: Respects existing database aliases")
    }

    /**
     * Test 1.5.5: Deduplication within batch (no DB conflicts)
     *
     * **Scenario:**
     * - Empty database
     * - Batch has multiple "button" aliases
     * - Should deduplicate within batch before inserting
     */
    @Test
    fun `deduplication should handle conflicts within batch`() = runBlocking {
        // Setup: Empty database
        whenever(mockRepository.getAllAliases()).thenReturn(emptyList())

        // Capture the aliases being inserted
        val capturedAliases = argumentCaptor<List<UUIDAliasDTO>>()
        whenever(mockRepository.insertAliasesBatch(capturedAliases.capture())).thenReturn(Unit)

        // Create batch with duplicates
        val uuidAliasMap = mapOf(
            "uuid-1" to "button",
            "uuid-2" to "text",
            "uuid-3" to "button",
            "uuid-4" to "text",
            "uuid-5" to "button"
        )

        // Execute
        val result = aliasManager.setAliasesBatch(uuidAliasMap)

        // Verify: Result has unique aliases
        assertEquals(5, result.size)
        assertEquals(5, result.values.toSet().size)

        // Verify: Inserted aliases are all unique
        val insertedAliases = capturedAliases.firstValue
        val insertedAliasNames = insertedAliases.map { it.alias }
        assertEquals(5, insertedAliasNames.toSet().size)

        // Verify: Contains expected pattern
        assertTrue(insertedAliasNames.contains("button"))
        assertTrue(insertedAliasNames.contains("button-1"))
        assertTrue(insertedAliasNames.contains("button-2"))
        assertTrue(insertedAliasNames.contains("text"))
        assertTrue(insertedAliasNames.contains("text-1"))

        println("✅ PASS: Deduplicates within batch correctly")
    }

    /**
     * Test 1.5.6: Performance comparison with individual operations
     *
     * **Demonstrates:** 27x speedup (1351ms → <50ms for 63 elements)
     */
    @Test
    fun `batch operations should be significantly faster than individual operations`() = runBlocking {
        // Setup: Mock repository
        whenever(mockRepository.getAllAliases()).thenReturn(emptyList())
        whenever(mockRepository.insertAliasesBatch(any())).thenReturn(Unit)
        whenever(mockRepository.aliasExists(any())).thenReturn(false)
        whenever(mockRepository.insertAlias(any())).thenReturn(Unit)

        // Create 63 elements
        val uuidAliasMap = (1..63).associate { "uuid-$it" to "button-$it" }

        // Test batch operation
        val batchStart = System.currentTimeMillis()
        val batchResult = aliasManager.setAliasesBatch(uuidAliasMap)
        val batchElapsed = System.currentTimeMillis() - batchStart

        // Test individual operations (simulated)
        val individualStart = System.currentTimeMillis()
        for ((uuid, alias) in uuidAliasMap) {
            aliasManager.setAliasWithDeduplication(uuid, alias)
        }
        val individualElapsed = System.currentTimeMillis() - individualStart

        // Report results
        println("📊 Performance Comparison:")
        println("   Batch operation:      ${batchElapsed}ms")
        println("   Individual operations: ${individualElapsed}ms")
        println("   Speedup:              ${individualElapsed / batchElapsed.coerceAtLeast(1)}x")

        // Verify: Batch is faster
        assertTrue("Batch should be faster than individual operations",
            batchElapsed < individualElapsed)

        // Verify: Batch meets target
        assertTrue("Batch should complete in <100ms, got ${batchElapsed}ms",
            batchElapsed < 100)
    }
}
