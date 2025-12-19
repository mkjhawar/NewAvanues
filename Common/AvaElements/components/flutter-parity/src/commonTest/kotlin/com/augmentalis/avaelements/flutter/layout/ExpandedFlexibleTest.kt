package com.augmentalis.avaelements.flutter.layout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * Tests for ExpandedComponent and FlexibleComponent
 *
 * Covers:
 * - Flex factor validation
 * - Default values
 * - FlexFit (Tight vs Loose)
 * - Edge cases (zero flex, large flex)
 */
class ExpandedFlexibleTest {

    // ======= ExpandedComponent Tests =======

    @Test
    fun `test default expanded component`() {
        val child = "TestChild"
        val expanded = ExpandedComponent(child = child)

        assertEquals(1, expanded.flex)
        assertEquals(child, expanded.child)
    }

    @Test
    fun `test expanded with custom flex factor`() {
        val expanded = ExpandedComponent(flex = 2, child = "Test")
        assertEquals(2, expanded.flex)
    }

    @Test
    fun `test expanded with zero flex is valid`() {
        val expanded = ExpandedComponent(flex = 0, child = "Test")
        assertEquals(0, expanded.flex)
    }

    @Test
    fun `test expanded with large flex factor`() {
        val expanded = ExpandedComponent(flex = 100, child = "Test")
        assertEquals(100, expanded.flex)
    }

    @Test
    fun `test expanded with negative flex throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            ExpandedComponent(flex = -1, child = "Test")
        }
    }

    @Test
    fun `test expanded serialization preserves properties`() {
        val expanded = ExpandedComponent(flex = 3, child = "TestChild")
        assertNotNull(expanded)
        assertEquals(3, expanded.flex)
        assertEquals("TestChild", expanded.child)
    }

    // ======= FlexibleComponent Tests =======

    @Test
    fun `test default flexible component`() {
        val child = "TestChild"
        val flexible = FlexibleComponent(child = child)

        assertEquals(1, flexible.flex)
        assertEquals(FlexFit.Loose, flexible.fit)
        assertEquals(child, flexible.child)
    }

    @Test
    fun `test flexible with tight fit`() {
        val flexible = FlexibleComponent(fit = FlexFit.Tight, child = "Test")
        assertEquals(FlexFit.Tight, flexible.fit)
    }

    @Test
    fun `test flexible with loose fit`() {
        val flexible = FlexibleComponent(fit = FlexFit.Loose, child = "Test")
        assertEquals(FlexFit.Loose, flexible.fit)
    }

    @Test
    fun `test flexible with custom flex factor`() {
        val flexible = FlexibleComponent(flex = 2, child = "Test")
        assertEquals(2, flexible.flex)
    }

    @Test
    fun `test flexible with zero flex is valid`() {
        val flexible = FlexibleComponent(flex = 0, child = "Test")
        assertEquals(0, flexible.flex)
    }

    @Test
    fun `test flexible with negative flex throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            FlexibleComponent(flex = -1, child = "Test")
        }
    }

    @Test
    fun `test flexible tight fit behaves like expanded`() {
        val flexible = FlexibleComponent(flex = 1, fit = FlexFit.Tight, child = "Test")
        val expanded = ExpandedComponent(flex = 1, child = "Test")

        assertEquals(flexible.flex, expanded.flex)
        assertEquals(FlexFit.Tight, flexible.fit)
    }

    @Test
    fun `test flexible loose fit allows smaller size`() {
        val flexible = FlexibleComponent(flex = 1, fit = FlexFit.Loose, child = "Test")
        assertEquals(FlexFit.Loose, flexible.fit)
    }

    @Test
    fun `test flexible serialization preserves properties`() {
        val flexible = FlexibleComponent(flex = 3, fit = FlexFit.Tight, child = "TestChild")
        assertNotNull(flexible)
        assertEquals(3, flexible.flex)
        assertEquals(FlexFit.Tight, flexible.fit)
        assertEquals("TestChild", flexible.child)
    }

    // ======= FlexFit Enum Tests =======

    @Test
    fun `test flex fit enum values`() {
        assertEquals(2, FlexFit.values().size)
        assertNotNull(FlexFit.Tight)
        assertNotNull(FlexFit.Loose)
    }

    @Test
    fun `test flex fit tight and loose are different`() {
        assert(FlexFit.Tight != FlexFit.Loose)
    }
}
