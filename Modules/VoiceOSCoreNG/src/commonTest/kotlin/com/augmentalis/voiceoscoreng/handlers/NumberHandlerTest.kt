package com.augmentalis.voiceoscoreng.handlers

import com.augmentalis.voiceoscoreng.common.Bounds
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NumberHandlerTest {

    private lateinit var handler: NumberHandler

    @BeforeTest
    fun setup() {
        handler = NumberHandler()
    }

    @AfterTest
    fun teardown() {
        handler.clear()
    }

    // ==================== assignNumbers Tests ====================

    @Test
    fun `assignNumbers assigns sequential numbers starting at 1`() {
        val elements = listOf(
            "vuid_btn_1" to "Submit",
            "vuid_btn_2" to "Cancel",
            "vuid_btn_3" to "Help"
        )

        val result = handler.assignNumbers(elements)

        assertEquals(3, result.size)
        assertEquals(1, result[1]?.number)
        assertEquals("vuid_btn_1", result[1]?.vuid)
        assertEquals("Submit", result[1]?.label)

        assertEquals(2, result[2]?.number)
        assertEquals("vuid_btn_2", result[2]?.vuid)
        assertEquals("Cancel", result[2]?.label)

        assertEquals(3, result[3]?.number)
        assertEquals("vuid_btn_3", result[3]?.vuid)
        assertEquals("Help", result[3]?.label)
    }

    @Test
    fun `assignNumbers clears previous assignments`() {
        // First assignment
        val elements1 = listOf("vuid_1" to "First", "vuid_2" to "Second")
        handler.assignNumbers(elements1)
        assertEquals(2, handler.getCount())

        // Second assignment should clear and restart from 1
        val elements2 = listOf("vuid_a" to "Alpha")
        val result = handler.assignNumbers(elements2)

        assertEquals(1, result.size)
        assertEquals(1, handler.getCount())
        assertEquals("vuid_a", result[1]?.vuid)
        assertEquals("Alpha", result[1]?.label)
        assertNull(handler.getElement(2)) // Previous element should be gone
    }

    @Test
    fun `assignNumbers with empty list returns empty map`() {
        val result = handler.assignNumbers(emptyList())

        assertTrue(result.isEmpty())
        assertEquals(0, handler.getCount())
    }

    // ==================== handleCommand Tests ====================

    @Test
    fun `handleCommand tap N selects number N`() {
        handler.assignNumbers(listOf(
            "vuid_1" to "First",
            "vuid_2" to "Second",
            "vuid_3" to "Third"
        ))

        val result = handler.handleCommand("tap 3")

        assertTrue(result.success)
        assertEquals(3, result.selectedNumber)
        assertEquals("vuid_3", result.element?.vuid)
        assertEquals("Third", result.element?.label)
        assertNull(result.error)
    }

    @Test
    fun `handleCommand click N selects number N`() {
        handler.assignNumbers(listOf(
            "vuid_1" to "First",
            "vuid_2" to "Second",
            "vuid_3" to "Third",
            "vuid_4" to "Fourth",
            "vuid_5" to "Fifth"
        ))

        val result = handler.handleCommand("click 5")

        assertTrue(result.success)
        assertEquals(5, result.selectedNumber)
        assertEquals("vuid_5", result.element?.vuid)
        assertEquals("Fifth", result.element?.label)
    }

    @Test
    fun `handleCommand select N selects number N`() {
        handler.assignNumbers(listOf("vuid_1" to "Option"))

        val result = handler.handleCommand("select 1")

        assertTrue(result.success)
        assertEquals(1, result.selectedNumber)
        assertEquals("vuid_1", result.element?.vuid)
    }

    @Test
    fun `handleCommand number N selects number N`() {
        handler.assignNumbers(listOf("vuid_1" to "First", "vuid_2" to "Second"))

        val result = handler.handleCommand("number 2")

        assertTrue(result.success)
        assertEquals(2, result.selectedNumber)
        assertEquals("vuid_2", result.element?.vuid)
    }

    @Test
    fun `handleCommand just number selects number`() {
        handler.assignNumbers(listOf(
            "vuid_1" to "First",
            "vuid_2" to "Second",
            "vuid_3" to "Third"
        ))

        val result = handler.handleCommand("3")

        assertTrue(result.success)
        assertEquals(3, result.selectedNumber)
        assertEquals("vuid_3", result.element?.vuid)
        assertEquals("Third", result.element?.label)
    }

    @Test
    fun `handleCommand word number three selects number 3`() {
        handler.assignNumbers(listOf(
            "vuid_1" to "First",
            "vuid_2" to "Second",
            "vuid_3" to "Third"
        ))

        val result = handler.handleCommand("three")

        assertTrue(result.success)
        assertEquals(3, result.selectedNumber)
        assertEquals("vuid_3", result.element?.vuid)
    }

    @Test
    fun `handleCommand word number with tap prefix`() {
        handler.assignNumbers(listOf(
            "vuid_1" to "First",
            "vuid_2" to "Second",
            "vuid_3" to "Third",
            "vuid_4" to "Fourth",
            "vuid_5" to "Fifth",
            "vuid_6" to "Sixth",
            "vuid_7" to "Seventh"
        ))

        val result = handler.handleCommand("tap seven")

        assertTrue(result.success)
        assertEquals(7, result.selectedNumber)
        assertEquals("vuid_7", result.element?.vuid)
    }

    @Test
    fun `handleCommand all word numbers one to ten`() {
        val elements = (1..10).map { "vuid_$it" to "Element $it" }
        handler.assignNumbers(elements)

        val wordNumbers = listOf("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten")

        wordNumbers.forEachIndexed { index, word ->
            val expectedNumber = index + 1
            val result = handler.handleCommand(word)

            assertTrue(result.success, "Failed for word: $word")
            assertEquals(expectedNumber, result.selectedNumber, "Number mismatch for word: $word")
            assertEquals("vuid_$expectedNumber", result.element?.vuid, "VUID mismatch for word: $word")
        }
    }

    @Test
    fun `handleCommand with invalid number returns error`() {
        handler.assignNumbers(listOf("vuid_1" to "First"))

        val result = handler.handleCommand("tap 99")

        assertFalse(result.success)
        assertEquals(99, result.selectedNumber)
        assertNull(result.element)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("No element with number 99"))
    }

    @Test
    fun `handleCommand with unparseable command returns error`() {
        handler.assignNumbers(listOf("vuid_1" to "First"))

        val result = handler.handleCommand("do something")

        assertFalse(result.success)
        assertNull(result.selectedNumber)
        assertNull(result.element)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Could not parse number"))
    }

    @Test
    fun `handleCommand is case insensitive`() {
        handler.assignNumbers(listOf("vuid_1" to "First", "vuid_2" to "Second"))

        val resultUpper = handler.handleCommand("TAP 2")
        val resultMixed = handler.handleCommand("Tap 2")
        val resultLower = handler.handleCommand("tap 2")

        assertTrue(resultUpper.success)
        assertTrue(resultMixed.success)
        assertTrue(resultLower.success)
    }

    @Test
    fun `handleCommand trims whitespace`() {
        handler.assignNumbers(listOf("vuid_1" to "First", "vuid_2" to "Second"))

        val result = handler.handleCommand("  tap 2  ")

        assertTrue(result.success)
        assertEquals(2, result.selectedNumber)
    }

    // ==================== selectNumber Tests ====================

    @Test
    fun `selectNumber returns correct element`() {
        handler.assignNumbers(listOf(
            "vuid_a" to "Alpha",
            "vuid_b" to "Beta",
            "vuid_c" to "Gamma"
        ))

        val result = handler.selectNumber(2)

        assertTrue(result.success)
        assertEquals(2, result.selectedNumber)
        assertEquals("vuid_b", result.element?.vuid)
        assertEquals("Beta", result.element?.label)
        assertNull(result.error)
    }

    @Test
    fun `selectNumber with non-existent number returns error`() {
        handler.assignNumbers(listOf("vuid_1" to "Only"))

        val result = handler.selectNumber(5)

        assertFalse(result.success)
        assertEquals(5, result.selectedNumber)
        assertNull(result.element)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("No element with number 5"))
    }

    @Test
    fun `selectNumber on empty handler returns error`() {
        val result = handler.selectNumber(1)

        assertFalse(result.success)
        assertEquals(1, result.selectedNumber)
        assertNull(result.element)
        assertNotNull(result.error)
    }

    // ==================== getElement Tests ====================

    @Test
    fun `getElement returns correct element`() {
        handler.assignNumbers(listOf("vuid_x" to "X"))

        val element = handler.getElement(1)

        assertNotNull(element)
        assertEquals(1, element.number)
        assertEquals("vuid_x", element.vuid)
        assertEquals("X", element.label)
    }

    @Test
    fun `getElement returns null for non-existent number`() {
        handler.assignNumbers(listOf("vuid_1" to "One"))

        val element = handler.getElement(99)

        assertNull(element)
    }

    @Test
    fun `getElement returns null on empty handler`() {
        val element = handler.getElement(1)

        assertNull(element)
    }

    // ==================== getAllNumberedElements Tests ====================

    @Test
    fun `getAllNumberedElements returns copy of all elements`() {
        handler.assignNumbers(listOf(
            "vuid_1" to "First",
            "vuid_2" to "Second"
        ))

        val all = handler.getAllNumberedElements()

        assertEquals(2, all.size)
        assertTrue(all.containsKey(1))
        assertTrue(all.containsKey(2))
    }

    @Test
    fun `getAllNumberedElements returns empty map when empty`() {
        val all = handler.getAllNumberedElements()

        assertTrue(all.isEmpty())
    }

    // ==================== clear Tests ====================

    @Test
    fun `clear resets state and numbering`() {
        handler.assignNumbers(listOf(
            "vuid_1" to "First",
            "vuid_2" to "Second"
        ))
        assertEquals(2, handler.getCount())

        handler.clear()

        assertEquals(0, handler.getCount())
        assertNull(handler.getElement(1))
        assertNull(handler.getElement(2))
    }

    @Test
    fun `clear resets numbering to 1`() {
        handler.assignNumbers(listOf("vuid_1" to "First", "vuid_2" to "Second"))
        handler.clear()

        // After clear, new assignment should start from 1 again
        handler.assignNumbers(listOf("vuid_new" to "New"))

        assertEquals(1, handler.getCount())
        val element = handler.getElement(1)
        assertNotNull(element)
        assertEquals(1, element.number)
        assertEquals("vuid_new", element.vuid)
    }

    // ==================== getCount Tests ====================

    @Test
    fun `getCount returns correct count`() {
        assertEquals(0, handler.getCount())

        handler.assignNumbers(listOf("a" to "A"))
        assertEquals(1, handler.getCount())

        handler.assignNumbers(listOf("a" to "A", "b" to "B", "c" to "C"))
        assertEquals(3, handler.getCount())
    }

    // ==================== NumberedElement Tests ====================

    @Test
    fun `NumberedElement with bounds stores bounds correctly`() {
        val bounds = Bounds(10, 20, 110, 70)
        val element = NumberedElement(
            number = 1,
            vuid = "test_vuid",
            label = "Test Label",
            bounds = bounds
        )

        assertEquals(1, element.number)
        assertEquals("test_vuid", element.vuid)
        assertEquals("Test Label", element.label)
        assertNotNull(element.bounds)
        assertEquals(10, element.bounds?.left)
        assertEquals(20, element.bounds?.top)
        assertEquals(110, element.bounds?.right)
        assertEquals(70, element.bounds?.bottom)
    }

    @Test
    fun `NumberedElement default bounds is null`() {
        val element = NumberedElement(
            number = 1,
            vuid = "test_vuid",
            label = "Test Label"
        )

        assertNull(element.bounds)
    }

    // ==================== NumberSelectionResult Tests ====================

    @Test
    fun `NumberSelectionResult success case`() {
        val element = NumberedElement(1, "vuid", "label")
        val result = NumberSelectionResult(
            success = true,
            selectedNumber = 1,
            element = element
        )

        assertTrue(result.success)
        assertEquals(1, result.selectedNumber)
        assertEquals(element, result.element)
        assertNull(result.error)
    }

    @Test
    fun `NumberSelectionResult failure case`() {
        val result = NumberSelectionResult(
            success = false,
            selectedNumber = 99,
            error = "No element with number 99"
        )

        assertFalse(result.success)
        assertEquals(99, result.selectedNumber)
        assertNull(result.element)
        assertEquals("No element with number 99", result.error)
    }
}
