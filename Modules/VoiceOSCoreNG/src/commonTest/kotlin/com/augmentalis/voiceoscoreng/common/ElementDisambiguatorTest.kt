/**
 * ElementDisambiguatorTest.kt - Tests for element disambiguation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-08
 */
package com.augmentalis.voiceoscoreng.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ElementDisambiguatorTest {

    private val disambiguator = ElementDisambiguator()

    // ═══════════════════════════════════════════════════════════════════════════
    // Test Data
    // ═══════════════════════════════════════════════════════════════════════════

    private fun createTestElements(): List<ElementInfo> = listOf(
        // 3 Submit buttons (duplicates)
        ElementInfo(
            resourceId = "btn_iphone_submit",
            className = "android.widget.Button",
            text = "Submit",
            contentDescription = "Submit iPhone order",
            bounds = Bounds(100, 100, 200, 150),
            isClickable = true,
            isEnabled = true
        ),
        ElementInfo(
            resourceId = "btn_airpods_submit",
            className = "android.widget.Button",
            text = "Submit",
            contentDescription = "Submit AirPods order",
            bounds = Bounds(100, 200, 200, 250),
            isClickable = true,
            isEnabled = true
        ),
        ElementInfo(
            resourceId = "btn_macbook_submit",
            className = "android.widget.Button",
            text = "Submit",
            contentDescription = "Submit MacBook order",
            bounds = Bounds(100, 300, 200, 350),
            isClickable = true,
            isEnabled = true
        ),
        // 2 Cancel buttons (duplicates)
        ElementInfo(
            resourceId = "btn_cancel_1",
            className = "android.widget.Button",
            text = "Cancel",
            contentDescription = "",
            bounds = Bounds(220, 100, 300, 150),
            isClickable = true,
            isEnabled = true
        ),
        ElementInfo(
            resourceId = "btn_cancel_2",
            className = "android.widget.Button",
            text = "Cancel",
            contentDescription = "",
            bounds = Bounds(220, 200, 300, 250),
            isClickable = true,
            isEnabled = true
        ),
        // 1 Checkout button (unique)
        ElementInfo(
            resourceId = "btn_checkout",
            className = "android.widget.Button",
            text = "Checkout",
            contentDescription = "Proceed to checkout",
            bounds = Bounds(100, 400, 300, 450),
            isClickable = true,
            isEnabled = true
        ),
        // 1 Disabled Submit button
        ElementInfo(
            resourceId = "btn_disabled_submit",
            className = "android.widget.Button",
            text = "Submit",
            contentDescription = "",
            bounds = Bounds(100, 500, 200, 550),
            isClickable = true,
            isEnabled = false
        )
    )

    // ═══════════════════════════════════════════════════════════════════════════
    // Single Match Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `single match executes without disambiguation`() {
        val elements = createTestElements()
        val result = disambiguator.findMatches("Checkout", elements)

        assertFalse(result.needsDisambiguation)
        assertNotNull(result.singleMatch)
        assertEquals("Checkout", result.singleMatch?.text)
        assertTrue(result.numberedItems.isEmpty())
    }

    @Test
    fun `single match accessibility announcement`() {
        val elements = createTestElements()
        val result = disambiguator.findMatches("Checkout", elements)

        assertTrue(result.getAccessibilityAnnouncement().contains("Executing"))
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Multiple Match Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `multiple matches trigger disambiguation`() {
        val elements = createTestElements()
        val result = disambiguator.findMatches("Submit", elements)

        assertTrue(result.needsDisambiguation)
        assertNull(result.singleMatch)
        assertEquals(3, result.matchCount) // 3 enabled Submit buttons
    }

    @Test
    fun `numbered items are created for duplicates`() {
        val elements = createTestElements()
        val result = disambiguator.findMatches("Submit", elements)

        assertEquals(3, result.numberedItems.size)
        assertEquals(1, result.numberedItems[0].number)
        assertEquals(2, result.numberedItems[1].number)
        assertEquals(3, result.numberedItems[2].number)
    }

    @Test
    fun `disabled elements are excluded from matches`() {
        val elements = createTestElements()
        val result = disambiguator.findMatches("Submit", elements)

        // Should only match 3 enabled Submit buttons, not the disabled one
        assertEquals(3, result.matchCount)
        assertTrue(result.matches.all { it.isEnabled })
    }

    @Test
    fun `cancel buttons get disambiguated`() {
        val elements = createTestElements()
        val result = disambiguator.findMatches("Cancel", elements)

        assertTrue(result.needsDisambiguation)
        assertEquals(2, result.matchCount)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // No Match Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `no matches returns empty result`() {
        val elements = createTestElements()
        val result = disambiguator.findMatches("Delete", elements)

        assertTrue(result.noMatches)
        assertNull(result.singleMatch)
        assertEquals(0, result.matchCount)
    }

    @Test
    fun `no matches accessibility announcement`() {
        val elements = createTestElements()
        val result = disambiguator.findMatches("Delete", elements)

        assertTrue(result.getAccessibilityAnnouncement().contains("No elements found"))
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Context Label Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `context labels extracted from resourceId`() {
        val elements = createTestElements()
        val result = disambiguator.findMatches("Submit", elements)

        // Should extract context from resourceId
        val contexts = result.numberedItems.mapNotNull { it.contextLabel }
        assertTrue(contexts.isNotEmpty())
    }

    @Test
    fun `display labels include context`() {
        val elements = createTestElements()
        val result = disambiguator.findMatches("Submit", elements)

        // Display labels should include context in parentheses
        val displayLabels = result.numberedItems.map { it.displayLabel }
        assertTrue(displayLabels.any { it.contains("(") && it.contains(")") })
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Selection Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `select by number returns correct element`() {
        val elements = createTestElements()
        val result = disambiguator.findMatches("Submit", elements)

        val selected = disambiguator.selectByNumber(result, 2)

        assertNotNull(selected)
        assertEquals("Submit", selected.text)
        assertEquals("btn_airpods_submit", selected.resourceId)
    }

    @Test
    fun `select invalid number returns null`() {
        val elements = createTestElements()
        val result = disambiguator.findMatches("Submit", elements)

        val selected = disambiguator.selectByNumber(result, 99)

        assertNull(selected)
    }

    @Test
    fun `select from non-disambiguation result returns null`() {
        val elements = createTestElements()
        val result = disambiguator.findMatches("Checkout", elements)

        // Single match - no disambiguation items
        val selected = disambiguator.selectByNumber(result, 1)

        assertNull(selected)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Match Mode Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `exact match mode only matches exact text`() {
        val elements = createTestElements()

        val exactResult = disambiguator.findMatches(
            "Submit",
            elements,
            ElementDisambiguator.MatchMode.EXACT
        )

        assertTrue(exactResult.matchCount > 0)
    }

    @Test
    fun `contains match mode matches partial text`() {
        val elements = createTestElements()

        val containsResult = disambiguator.findMatches(
            "sub",
            elements,
            ElementDisambiguator.MatchMode.CONTAINS
        )

        // Should match "Submit" buttons
        assertTrue(containsResult.matchCount > 0)
    }

    @Test
    fun `starts_with mode matches prefix`() {
        val elements = createTestElements()

        val startsWithResult = disambiguator.findMatches(
            "Sub",
            elements,
            ElementDisambiguator.MatchMode.STARTS_WITH
        )

        assertTrue(startsWithResult.matchCount > 0)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Context Strategy Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `position context strategy uses screen position`() {
        val positionDisambiguator = ElementDisambiguator(
            contextStrategy = ElementDisambiguator.ContextStrategy.POSITION
        )

        val elements = createTestElements()
        val result = positionDisambiguator.findMatches("Submit", elements)

        // Should have position-based context (top, middle, bottom)
        val contexts = result.numberedItems.mapNotNull { it.contextLabel }
        assertTrue(contexts.any { it == "top" || it == "middle" || it == "bottom" })
    }

    @Test
    fun `none context strategy produces no labels`() {
        val noContextDisambiguator = ElementDisambiguator(
            contextStrategy = ElementDisambiguator.ContextStrategy.NONE
        )

        val elements = createTestElements()
        val result = noContextDisambiguator.findMatches("Submit", elements)

        // All context labels should be null
        assertTrue(result.numberedItems.all { it.contextLabel == null })
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Bounds Tests
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `numbered matches include bounds for badge positioning`() {
        val elements = createTestElements()
        val result = disambiguator.findMatches("Submit", elements)

        result.numberedItems.forEach { item ->
            assertNotNull(item.bounds)
            assertTrue(item.bounds.left >= 0)
            assertTrue(item.bounds.top >= 0)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Edge Cases
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `empty element list returns no matches`() {
        val result = disambiguator.findMatches("Submit", emptyList())

        assertTrue(result.noMatches)
    }

    @Test
    fun `case insensitive matching`() {
        val elements = createTestElements()

        val lowerResult = disambiguator.findMatches("submit", elements)
        val upperResult = disambiguator.findMatches("SUBMIT", elements)
        val mixedResult = disambiguator.findMatches("SuBmIt", elements)

        assertEquals(lowerResult.matchCount, upperResult.matchCount)
        assertEquals(upperResult.matchCount, mixedResult.matchCount)
    }

    @Test
    fun `whitespace in query is trimmed`() {
        val elements = createTestElements()

        val result = disambiguator.findMatches("  Submit  ", elements)

        assertEquals(3, result.matchCount)
    }
}
