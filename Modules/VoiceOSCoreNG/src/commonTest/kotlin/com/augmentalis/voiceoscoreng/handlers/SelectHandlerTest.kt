/**
 * SelectHandlerTest.kt - Tests for SelectHandler
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * TDD tests for selection and clipboard handling.
 */
package com.augmentalis.voiceoscoreng.handlers

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SelectHandlerTest {

    private lateinit var handler: SelectHandler
    private lateinit var mockClipboard: MockClipboardProvider

    @BeforeTest
    fun setup() {
        mockClipboard = MockClipboardProvider()
        handler = SelectHandler(clipboardProvider = mockClipboard)
    }

    // ==================== handleCommand Tests ====================

    @Test
    fun `handleCommand select all returns SELECT_ALL action`() {
        val result = handler.handleCommand("select all")

        assertTrue(result.success)
        assertEquals(SelectionAction.SELECT_ALL, result.action)
        assertNull(result.error)
    }

    @Test
    fun `handleCommand select all case insensitive`() {
        val result = handler.handleCommand("SELECT ALL")

        assertTrue(result.success)
        assertEquals(SelectionAction.SELECT_ALL, result.action)
    }

    @Test
    fun `handleCommand select all with whitespace`() {
        val result = handler.handleCommand("  select all  ")

        assertTrue(result.success)
        assertEquals(SelectionAction.SELECT_ALL, result.action)
    }

    @Test
    fun `handleCommand copy with selection succeeds`() {
        // First select some text
        handler.selectText("Hello World")

        val result = handler.handleCommand("copy")

        assertTrue(result.success)
        assertEquals(SelectionAction.COPY, result.action)
        assertEquals("Hello World", result.selectedText)
        assertEquals("Hello World", mockClipboard.lastCopiedText)
    }

    @Test
    fun `handleCommand copy without selection fails`() {
        val result = handler.handleCommand("copy")

        assertFalse(result.success)
        assertEquals(SelectionAction.COPY, result.action)
        assertEquals("Nothing selected", result.error)
    }

    @Test
    fun `handleCommand cut removes selection`() {
        // First select some text
        handler.selectText("Cut this text")

        val result = handler.handleCommand("cut")

        assertTrue(result.success)
        assertEquals(SelectionAction.CUT, result.action)
        assertEquals("Cut this text", result.selectedText)
        assertEquals("Cut this text", mockClipboard.lastCopiedText)

        // Selection should be cleared after cut
        assertNull(handler.getSelectedText())
    }

    @Test
    fun `handleCommand cut without selection fails`() {
        val result = handler.handleCommand("cut")

        assertFalse(result.success)
        assertEquals(SelectionAction.CUT, result.action)
        assertEquals("Nothing selected", result.error)
    }

    @Test
    fun `handleCommand paste with clipboard succeeds`() {
        mockClipboard.initClipboardContent("Pasted content")

        val result = handler.handleCommand("paste")

        assertTrue(result.success)
        assertEquals(SelectionAction.PASTE, result.action)
        assertEquals("Pasted content", result.selectedText)
    }

    @Test
    fun `handleCommand paste without clipboard fails`() {
        // Clipboard is empty by default

        val result = handler.handleCommand("paste")

        assertFalse(result.success)
        assertEquals(SelectionAction.PASTE, result.action)
        assertEquals("Clipboard empty or unavailable", result.error)
    }

    @Test
    fun `handleCommand clear clears selection`() {
        // First select some text
        handler.selectText("Some text")
        assertTrue(handler.hasSelection())

        val result = handler.handleCommand("clear")

        assertTrue(result.success)
        assertEquals(SelectionAction.CLEAR, result.action)
        assertFalse(handler.hasSelection())
    }

    @Test
    fun `handleCommand clear selection clears selection`() {
        handler.selectText("Some text")

        val result = handler.handleCommand("clear selection")

        assertTrue(result.success)
        assertEquals(SelectionAction.CLEAR, result.action)
        assertFalse(handler.hasSelection())
    }

    @Test
    fun `handleCommand select X stores selected text`() {
        val result = handler.handleCommand("select important phrase")

        assertTrue(result.success)
        assertEquals(SelectionAction.SELECT, result.action)
        assertEquals("important phrase", result.selectedText)
        assertEquals("important phrase", handler.getSelectedText())
    }

    @Test
    fun `handleCommand select preserves original case`() {
        val result = handler.handleCommand("select Hello World")

        assertTrue(result.success)
        assertEquals("Hello World", result.selectedText)
    }

    @Test
    fun `handleCommand unknown returns error`() {
        val result = handler.handleCommand("unknown command")

        assertFalse(result.success)
        assertEquals(SelectionAction.SELECT, result.action)
        assertTrue(result.error?.contains("Unknown command") == true)
    }

    // ==================== Direct Method Tests ====================

    @Test
    fun `selectAll sets selection range`() {
        val result = handler.selectAll()

        assertTrue(result.success)
        assertEquals(SelectionAction.SELECT_ALL, result.action)
        assertTrue(handler.hasSelection())
    }

    @Test
    fun `selectText stores text`() {
        val result = handler.selectText("Test selection")

        assertTrue(result.success)
        assertEquals(SelectionAction.SELECT, result.action)
        assertEquals("Test selection", result.selectedText)
        assertEquals("Test selection", handler.getSelectedText())
    }

    @Test
    fun `copy without clipboard provider still succeeds`() {
        // Create handler without clipboard provider
        val handlerWithoutClipboard = SelectHandler(clipboardProvider = null)
        handlerWithoutClipboard.selectText("Some text")

        val result = handlerWithoutClipboard.copy()

        assertTrue(result.success)
        assertEquals(SelectionAction.COPY, result.action)
        assertEquals("Some text", result.selectedText)
    }

    @Test
    fun `paste without clipboard provider fails`() {
        val handlerWithoutClipboard = SelectHandler(clipboardProvider = null)

        val result = handlerWithoutClipboard.paste()

        assertFalse(result.success)
        assertEquals(SelectionAction.PASTE, result.action)
    }

    @Test
    fun `clearSelection resets state`() {
        handler.selectText("Text")
        handler.selectAll()
        assertTrue(handler.hasSelection())

        val result = handler.clearSelection()

        assertTrue(result.success)
        assertEquals(SelectionAction.CLEAR, result.action)
        assertFalse(handler.hasSelection())
        assertNull(handler.getSelectedText())
    }

    // ==================== hasSelection Tests ====================

    @Test
    fun `hasSelection returns false initially`() {
        val freshHandler = SelectHandler()
        assertFalse(freshHandler.hasSelection())
    }

    @Test
    fun `hasSelection returns true after selectText`() {
        handler.selectText("Selected")
        assertTrue(handler.hasSelection())
    }

    @Test
    fun `hasSelection returns true after selectAll`() {
        handler.selectAll()
        assertTrue(handler.hasSelection())
    }

    @Test
    fun `hasSelection returns false after clearSelection`() {
        handler.selectText("Selected")
        handler.clearSelection()
        assertFalse(handler.hasSelection())
    }

    // ==================== getSelectedText Tests ====================

    @Test
    fun `getSelectedText returns null initially`() {
        val freshHandler = SelectHandler()
        assertNull(freshHandler.getSelectedText())
    }

    @Test
    fun `getSelectedText returns selected text`() {
        handler.selectText("My selected text")
        assertEquals("My selected text", handler.getSelectedText())
    }

    @Test
    fun `getSelectedText returns null after cut`() {
        handler.selectText("To be cut")
        handler.cut()
        assertNull(handler.getSelectedText())
    }

    // ==================== Edge Cases ====================

    @Test
    fun `select empty string`() {
        val result = handler.handleCommand("select ")

        assertTrue(result.success)
        assertEquals("", result.selectedText)
    }

    @Test
    fun `multiple selections replace previous`() {
        handler.selectText("First")
        handler.selectText("Second")

        assertEquals("Second", handler.getSelectedText())
    }

    @Test
    fun `copy same text multiple times`() {
        handler.selectText("Repeated")
        handler.copy()
        handler.copy()
        handler.copy()

        assertEquals(3, mockClipboard.copyCount)
    }
}

/**
 * Mock clipboard provider for testing.
 */
class MockClipboardProvider : IClipboardProvider {
    var lastCopiedText: String? = null
    var clipboardContent: String? = null
    var copyCount: Int = 0
    var clearCount: Int = 0

    override fun copy(text: String): Boolean {
        lastCopiedText = text
        clipboardContent = text
        copyCount++
        return true
    }

    override fun paste(): String? {
        return clipboardContent
    }

    override fun clear(): Boolean {
        clipboardContent = null
        clearCount++
        return true
    }

    fun initClipboardContent(content: String) {
        clipboardContent = content
    }
}
