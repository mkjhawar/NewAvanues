/**
 * ScreenHashCalculatorTest.kt - Unit tests for screen hash calculation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-15
 *
 * P3 Task 3.2: Direct unit tests for hash calculation edge cases
 */

package com.augmentalis.voiceoscore.version

import com.augmentalis.database.dto.ScrapedElementDTO
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ScreenHashCalculator.
 *
 * Tests hash stability, collision resistance, and structural change detection.
 * Validates that:
 * - Same elements produce same hash
 * - Element order doesn't matter
 * - Structural changes (bounds, add/remove) are detected
 * - Non-structural changes (text) are ignored
 * - No collisions occur in large datasets
 */
class ScreenHashCalculatorTest {

    @Test
    fun calculateScreenHash_emptyList_returnsEmptyString() {
        val hash = ScreenHashCalculator.calculateScreenHash(emptyList())
        assertEquals("Empty list should produce empty hash", "", hash)
    }

    @Test
    fun calculateScreenHash_sameElements_produceSameHash() {
        val elements1 = createTestElements(5)
        val elements2 = createTestElements(5)

        val hash1 = ScreenHashCalculator.calculateScreenHash(elements1)
        val hash2 = ScreenHashCalculator.calculateScreenHash(elements2)

        assertEquals("Identical element sets should produce identical hashes", hash1, hash2)
    }

    @Test
    fun calculateScreenHash_sameElementsDifferentOrder_produceSameHash() {
        val elements1 = createTestElements(5)
        val elements2 = elements1.reversed()

        val hash1 = ScreenHashCalculator.calculateScreenHash(elements1)
        val hash2 = ScreenHashCalculator.calculateScreenHash(elements2)

        assertEquals("Hash should be order-independent", hash1, hash2)
    }

    @Test
    fun calculateScreenHash_differentBounds_producesDifferentHash() {
        val element1 = createTestElement(id = "btn1", bounds = "0,0,100,50")
        val element2 = createTestElement(id = "btn1", bounds = "0,0,100,51") // 1px different

        val hash1 = ScreenHashCalculator.calculateScreenHash(listOf(element1))
        val hash2 = ScreenHashCalculator.calculateScreenHash(listOf(element2))

        assertNotEquals("Different bounds should produce different hash", hash1, hash2)
    }

    @Test
    fun calculateScreenHash_differentText_producesSameHash() {
        val element1 = createTestElement(id = "btn1", text = "Submit")
        val element2 = createTestElement(id = "btn1", text = "Done")

        val hash1 = ScreenHashCalculator.calculateScreenHash(listOf(element1))
        val hash2 = ScreenHashCalculator.calculateScreenHash(listOf(element2))

        assertEquals("Text differences should not affect hash (structural only)", hash1, hash2)
    }

    @Test
    fun calculateScreenHash_addElement_producesDifferentHash() {
        val elements1 = createTestElements(5)
        val elements2 = elements1 + createTestElement(id = "newBtn", bounds = "200,200,300,250")

        val hash1 = ScreenHashCalculator.calculateScreenHash(elements1)
        val hash2 = ScreenHashCalculator.calculateScreenHash(elements2)

        assertNotEquals("Adding element should change hash", hash1, hash2)
    }

    @Test
    fun calculateScreenHash_removeElement_producesDifferentHash() {
        val elements1 = createTestElements(5)
        val elements2 = elements1.dropLast(1)

        val hash1 = ScreenHashCalculator.calculateScreenHash(elements1)
        val hash2 = ScreenHashCalculator.calculateScreenHash(elements2)

        assertNotEquals("Removing element should change hash", hash1, hash2)
    }

    @Test
    fun calculateScreenHash_returns64CharHex() {
        val elements = createTestElements(10)
        val hash = ScreenHashCalculator.calculateScreenHash(elements)

        assertEquals("SHA-256 hash should be 64 hex characters", 64, hash.length)
        assertTrue(
            "Hash should contain only hex characters (0-9, a-f)",
            hash.matches(Regex("[0-9a-f]{64}"))
        )
    }

    @Test
    fun calculateScreenHash_collisionProbability_isNegligible() {
        // Generate 1,000 different screen configurations
        // Each screen has unique element positions based on iteration index
        val uniqueHashes = (1..1000).map { iteration ->
            val elementCount = (iteration % 20) + 1  // 1-20 elements
            val elements = (1..elementCount).map { i ->
                // Use iteration in bounds to ensure truly unique screens
                createTestElement(
                    id = "element${iteration}_$i",
                    bounds = "${iteration + i * 10},${iteration + i * 5},${iteration + i * 10 + 100},${iteration + i * 5 + 50}"
                )
            }
            ScreenHashCalculator.calculateScreenHash(elements)
        }.toSet()

        // All hashes should be unique (no collisions)
        // Note: With 1000 screens, collision probability with SHA-256 is ~10^-71
        assertEquals(
            "No collisions expected for 1000 screens (SHA-256 collision probability ~0)",
            1000,
            uniqueHashes.size
        )
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Create test elements with unique IDs and bounds.
     *
     * @param count Number of elements to create
     * @return List of test elements
     */
    private fun createTestElements(count: Int): List<ScrapedElementDTO> {
        return (1..count).map { i ->
            createTestElement(
                id = "element$i",
                className = "Button",
                bounds = "${i * 10},${i * 10},${i * 10 + 100},${i * 10 + 50}"
            )
        }
    }

    /**
     * Create a single test element with specified properties.
     *
     * @param id Element hash / identifier
     * @param className Widget class name
     * @param bounds Bounding box as "left,top,right,bottom"
     * @param text Element text (not included in hash)
     * @return Scrap element DTO
     */
    private fun createTestElement(
        id: String = "testElement",
        className: String = "Button",
        bounds: String = "0,0,100,50",
        text: String = "Test"
    ): ScrapedElementDTO {
        return ScrapedElementDTO(
            id = 0L,
            elementHash = id,
            appId = "com.test.app",
            uuid = "uuid-$id",
            className = className,
            viewIdResourceName = id,
            text = text,
            contentDescription = null,
            bounds = bounds,
            isClickable = 1L,
            isLongClickable = 0L,
            isEditable = 0L,
            isScrollable = 0L,
            isCheckable = 0L,
            isFocusable = 1L,
            isEnabled = 1L,
            depth = 2L,
            indexInParent = 0L,
            scrapedAt = System.currentTimeMillis(),
            semanticRole = null,
            inputType = null,
            visualWeight = null,
            isRequired = null,
            formGroupId = null,
            placeholderText = null,
            validationPattern = null,
            backgroundColor = null,
            screen_hash = null
        )
    }
}
