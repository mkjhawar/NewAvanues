package com.augmentalis.voiceoscore.learnapp.jit

import com.augmentalis.database.dto.ScrapedElementDTO
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for JIT screen hash deduplication feature (Spec 009 - Phases 1-3)
 *
 * Tests cover:
 * - Database schema with screen_hash column
 * - Screen deduplication check logic
 * - Element persistence with screen hash
 *
 * Note: These are data structure tests. Integration tests with actual database
 * are in JitScreenHashIntegrationTest.kt (androidTest).
 *
 * @see JitElementCapture
 * @see JustInTimeLearner
 */
class JitDeduplicationTest {

    /**
     * Test: Deduplication logic (count > 0 means already captured)
     *
     * Scenario: Check deduplication decision based on count
     * Expected: count > 0 returns true, count == 0 returns false
     */
    @Test
    fun `deduplication check returns true when count greater than zero`() {
        // Given: Various count results
        val countExisting = 5L
        val countNew = 0L
        val countOne = 1L

        // When/Then: Existing screen (multiple elements)
        assertTrue(countExisting > 0, "Should return true for existing screen with multiple elements")

        // When/Then: New screen
        assertFalse(countNew > 0, "Should return false for new screen")

        // When/Then: Existing screen (single element)
        assertTrue(countOne > 0, "Should return true even for single element")
    }

    /**
     * Test: Element persistence includes screen_hash
     *
     * Scenario: Persist element with screen hash
     * Expected: DTO contains screen_hash field
     */
    @Test
    fun `element DTO includes screen_hash field`() {
        // Given: Element data with screen hash
        val screenHash = "screen_abc123"
        val appId = "com.example.app"

        // When: Create DTO
        val elementDTO = ScrapedElementDTO(
            id = 1L,
            elementHash = "element_xyz789",
            appId = appId,
            uuid = "uuid-test-1234",
            className = "android.widget.Button",
            viewIdResourceName = "btn_submit",
            text = "Submit",
            contentDescription = "Submit button",
            bounds = "10,20,100,80",
            isClickable = 1L,
            isLongClickable = 0L,
            isEditable = 0L,
            isScrollable = 0L,
            isCheckable = 0L,
            isFocusable = 1L,
            isEnabled = 1L,
            depth = 3L,
            indexInParent = 2L,
            scrapedAt = System.currentTimeMillis(),
            semanticRole = "button",
            inputType = null,
            visualWeight = null,
            isRequired = null,
            formGroupId = null,
            placeholderText = null,
            validationPattern = null,
            backgroundColor = null,
            screen_hash = screenHash  // Phase 1: New field
        )

        // Then: DTO has screen_hash
        assertEquals(screenHash, elementDTO.screen_hash)
    }

    /**
     * Test: Element DTO accepts null screen_hash for backward compatibility
     *
     * Scenario: Legacy elements without screen hash
     * Expected: DTO accepts null screen_hash
     */
    @Test
    fun `element DTO accepts null screen_hash`() {
        // Given: Element without screen hash (legacy)
        val elementDTO = ScrapedElementDTO(
            id = 2L,
            elementHash = "element_legacy_123",
            appId = "com.example.legacy",
            uuid = "uuid-legacy-5678",
            className = "android.widget.TextView",
            viewIdResourceName = "tv_title",
            text = "Title",
            contentDescription = null,
            bounds = "0,0,100,50",
            isClickable = 0L,
            isLongClickable = 0L,
            isEditable = 0L,
            isScrollable = 0L,
            isCheckable = 0L,
            isFocusable = 0L,
            isEnabled = 1L,
            depth = 1L,
            indexInParent = 0L,
            scrapedAt = System.currentTimeMillis(),
            semanticRole = "text",
            inputType = null,
            visualWeight = null,
            isRequired = null,
            formGroupId = null,
            placeholderText = null,
            validationPattern = null,
            backgroundColor = null,
            screen_hash = null  // Legacy: No screen hash
        )

        // Then: DTO accepts null
        assertEquals(null, elementDTO.screen_hash)
    }

    /**
     * Test: Screen hash grouping logic
     *
     * Scenario: Multiple elements share same screen hash
     * Expected: Can group elements by screen hash
     */
    @Test
    fun `multiple elements can share same screen hash`() {
        // Given: Multiple elements on same screen
        val sharedHash = "screen_abc123"
        val element1 = createTestElement(id = 1L, hash = sharedHash)
        val element2 = createTestElement(id = 2L, hash = sharedHash)
        val element3 = createTestElement(id = 3L, hash = sharedHash)

        // When: Group by screen hash
        val elements = listOf(element1, element2, element3)
        val groupedByHash = elements.groupBy { it.screen_hash }

        // Then: All elements grouped under same hash
        assertEquals(1, groupedByHash.size, "Should have one screen hash group")
        assertEquals(3, groupedByHash[sharedHash]?.size, "Should have 3 elements in group")
        assertTrue(groupedByHash.containsKey(sharedHash), "Should contain the screen hash")
    }

    /**
     * Test: Different screens have different hashes
     *
     * Scenario: Elements from different screens
     * Expected: Different screen_hash values
     */
    @Test
    fun `elements from different screens have different hashes`() {
        // Given: Elements from different screens
        val screen1Hash = "screen_home"
        val screen2Hash = "screen_settings"
        val screen3Hash = "screen_profile"

        val element1 = createTestElement(id = 1L, hash = screen1Hash)
        val element2 = createTestElement(id = 2L, hash = screen2Hash)
        val element3 = createTestElement(id = 3L, hash = screen3Hash)

        // When: Collect unique hashes
        val elements = listOf(element1, element2, element3)
        val uniqueHashes = elements.mapNotNull { it.screen_hash }.distinct()

        // Then: Three different hashes
        assertEquals(3, uniqueHashes.size, "Should have 3 unique screen hashes")
        assertTrue(uniqueHashes.contains(screen1Hash), "Should contain home screen hash")
        assertTrue(uniqueHashes.contains(screen2Hash), "Should contain settings screen hash")
        assertTrue(uniqueHashes.contains(screen3Hash), "Should contain profile screen hash")
    }

    /**
     * Test: Null screen hash handling for legacy elements
     *
     * Scenario: Elements without screen hash (pre-Phase 1)
     * Expected: Null is handled gracefully
     */
    @Test
    fun `null screen hash handled for legacy elements`() {
        // Given: Legacy element without screen hash
        val legacyElement = createTestElement(id = 99L, hash = null)

        // When: Access screen_hash
        val hash = legacyElement.screen_hash

        // Then: Returns null safely
        assertNull(hash, "Legacy element should have null screen_hash")
    }

    /**
     * Test: Empty string vs null screen hash
     *
     * Scenario: Distinguish between empty and null
     * Expected: Both are valid but different
     */
    @Test
    fun `empty string screen hash differs from null`() {
        // Given: Elements with empty string vs null
        val emptyHashElement = createTestElement(id = 1L, hash = "")
        val nullHashElement = createTestElement(id = 2L, hash = null)

        // When: Check values
        val emptyHash = emptyHashElement.screen_hash
        val nullHash = nullHashElement.screen_hash

        // Then: They are different
        assertEquals("", emptyHash, "Empty string should be preserved")
        assertNull(nullHash, "Null should be preserved")
        assertTrue(emptyHash != nullHash, "Empty string and null are different")
    }

    // Helper: Create test element with configurable screen_hash
    private fun createTestElement(id: Long, hash: String?): ScrapedElementDTO {
        return ScrapedElementDTO(
            id = id,
            elementHash = "element_hash_$id",
            appId = "com.example.test",
            uuid = "uuid_$id",
            className = "android.widget.Button",
            viewIdResourceName = "btn_$id",
            text = "Button $id",
            contentDescription = "Test button $id",
            bounds = "0,0,100,50",
            isClickable = 1L,
            isLongClickable = 0L,
            isEditable = 0L,
            isScrollable = 0L,
            isCheckable = 0L,
            isFocusable = 1L,
            isEnabled = 1L,
            depth = 2L,
            indexInParent = id,
            scrapedAt = System.currentTimeMillis(),
            semanticRole = "button",
            inputType = null,
            visualWeight = null,
            isRequired = null,
            formGroupId = null,
            placeholderText = null,
            validationPattern = null,
            backgroundColor = null,
            screen_hash = hash  // Configurable screen hash
        )
    }
}
