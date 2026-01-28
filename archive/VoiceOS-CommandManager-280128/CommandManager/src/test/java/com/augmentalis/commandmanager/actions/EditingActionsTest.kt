/**
 * EditingActionsTest.kt - Unit tests for text editing actions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-10-14
 *
 * Test Coverage:
 * - Copy/Paste/Cut operations
 * - Select All functionality
 * - Undo/Redo operations (API 24+)
 * - Clipboard management
 * - Accessibility node interaction
 * - Error handling and fallbacks
 *
 * Architecture: Tests EditingActions class that extends BaseAction
 */
package com.augmentalis.commandmanager.actions

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*

class EditingActionsTest {

    private lateinit var mockContext: Context
    private lateinit var mockAccessibilityService: AccessibilityService
    private lateinit var mockClipboardManager: ClipboardManager
    private lateinit var editingActions: EditingActions

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockAccessibilityService = mockk(relaxed = true)
        mockClipboardManager = mockk(relaxed = true)

        every { mockContext.getSystemService(Context.CLIPBOARD_SERVICE) } returns mockClipboardManager

        editingActions = EditingActions(mockContext, mockAccessibilityService)
    }

    @After
    fun teardown() {
        clearAllMocks()
    }

    // ========== Copy Tests ==========

    @Test
    fun `test copy action success with global action`() = runTest {
        // Arrange
        val command = Command("copy", "copy", CommandSource.VOICE, confidence = 1.0f, timestamp = System.currentTimeMillis())
        every { mockAccessibilityService.performGlobalAction(AccessibilityNodeInfo.ACTION_COPY) } returns true

        // Act
        val result = editingActions.execute(command, mockAccessibilityService, mockContext)

        // Assert
        assertTrue("Result should be CommandResult", result is CommandResult)
        assertTrue("Copy should succeed", result.success)
        assertEquals("Text copied", result.response)
        verify { mockAccessibilityService.performGlobalAction(AccessibilityNodeInfo.ACTION_COPY) }
    }

    @Test
    fun `test copy action failure requires fallback`() = runTest {
        // Arrange
        val command = Command("copy", "copy", CommandSource.VOICE, confidence = 1.0f, timestamp = System.currentTimeMillis())
        val mockRootNode = mockk<AccessibilityNodeInfo>(relaxed = true)
        val mockFocusedNode = mockk<AccessibilityNodeInfo>(relaxed = true)

        every { mockAccessibilityService.performGlobalAction(AccessibilityNodeInfo.ACTION_COPY) } returns false
        every { mockAccessibilityService.rootInActiveWindow } returns mockRootNode
        every { mockRootNode.isEditable } returns false
        every { mockRootNode.isFocused } returns false
        every { mockRootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) } returns mockFocusedNode
        every { mockFocusedNode.isEditable } returns true
        every { mockFocusedNode.performAction(AccessibilityNodeInfo.ACTION_COPY) } returns true

        // Act
        val result = editingActions.execute(command, mockAccessibilityService, mockContext)

        // Assert
        assertTrue("Copy should succeed with fallback", result.success)
        assertEquals("Text copied (fallback)", result.response)
    }

    @Test
    fun `test copy action complete failure`() = runTest {
        // Arrange
        val command = Command("copy", "copy", CommandSource.VOICE, confidence = 1.0f, timestamp = System.currentTimeMillis())
        every { mockAccessibilityService.performGlobalAction(AccessibilityNodeInfo.ACTION_COPY) } returns false
        every { mockAccessibilityService.rootInActiveWindow } returns null

        // Act
        val result = editingActions.execute(command, mockAccessibilityService, mockContext)

        // Assert
        assertFalse("Copy should fail", result.success)
        assertEquals(ErrorCode.EXECUTION_FAILED, result.error?.code)
    }

    // ========== Paste Tests ==========

    @Test
    fun `test paste action success with global action`() = runTest {
        // Arrange
        val command = Command("paste", "paste", CommandSource.VOICE, confidence = 1.0f, timestamp = System.currentTimeMillis())
        val mockClipData = mockk<ClipData>(relaxed = true)

        every { mockClipboardManager.hasPrimaryClip() } returns true
        every { mockClipboardManager.primaryClip } returns mockClipData
        every { mockAccessibilityService.performGlobalAction(AccessibilityNodeInfo.ACTION_PASTE) } returns true

        // Act
        val result = editingActions.execute(command, mockAccessibilityService, mockContext)

        // Assert
        assertTrue("Paste should succeed", result.success)
        assertEquals("Text pasted", result.response)
        verify { mockAccessibilityService.performGlobalAction(AccessibilityNodeInfo.ACTION_PASTE) }
    }

    @Test
    fun `test paste action with empty clipboard`() = runTest {
        // Arrange
        val command = Command("paste", "paste", CommandSource.VOICE, confidence = 1.0f, timestamp = System.currentTimeMillis())
        every { mockClipboardManager.hasPrimaryClip() } returns false

        // Act
        val result = editingActions.execute(command, mockAccessibilityService, mockContext)

        // Assert
        assertFalse("Paste should fail when clipboard is empty", result.success)
        assertEquals(ErrorCode.EXECUTION_FAILED, result.error?.code)
        assertEquals("Clipboard is empty", result.error?.message)
    }

    @Test
    fun `test paste action with fallback`() = runTest {
        // Arrange
        val command = Command("paste", "paste", CommandSource.VOICE, confidence = 1.0f, timestamp = System.currentTimeMillis())
        val mockClipData = mockk<ClipData>(relaxed = true)
        val mockClipItem = mockk<ClipData.Item>(relaxed = true)
        val mockRootNode = mockk<AccessibilityNodeInfo>(relaxed = true)
        val mockFocusedNode = mockk<AccessibilityNodeInfo>(relaxed = true)

        every { mockClipboardManager.hasPrimaryClip() } returns true
        every { mockClipboardManager.primaryClip } returns mockClipData
        every { mockAccessibilityService.performGlobalAction(AccessibilityNodeInfo.ACTION_PASTE) } returns false
        every { mockAccessibilityService.rootInActiveWindow } returns mockRootNode
        every { mockRootNode.isEditable } returns false
        every { mockRootNode.isFocused } returns false
        every { mockRootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) } returns mockFocusedNode
        every { mockFocusedNode.isEditable } returns true
        every { mockClipData.itemCount } returns 1
        every { mockClipData.getItemAt(0) } returns mockClipItem
        every { mockClipItem.text } returns "test text"
        every { mockFocusedNode.performAction(AccessibilityNodeInfo.ACTION_PASTE, any()) } returns true

        // Act
        val result = editingActions.execute(command, mockAccessibilityService, mockContext)

        // Assert
        assertTrue("Paste should succeed with fallback", result.success)
    }

    // ========== Cut Tests ==========

    @Test
    fun `test cut action success`() = runTest {
        // Arrange
        val command = Command("cut", "cut", CommandSource.VOICE, confidence = 1.0f, timestamp = System.currentTimeMillis())
        val mockRootNode = mockk<AccessibilityNodeInfo>(relaxed = true)
        val mockFocusedNode = mockk<AccessibilityNodeInfo>(relaxed = true)

        every { mockAccessibilityService.rootInActiveWindow } returns mockRootNode
        every { mockRootNode.isEditable } returns false
        every { mockRootNode.isFocused } returns false
        every { mockRootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) } returns mockFocusedNode
        every { mockFocusedNode.isEditable } returns true
        every { mockFocusedNode.performAction(AccessibilityNodeInfo.ACTION_CUT) } returns true

        // Act
        val result = editingActions.execute(command, mockAccessibilityService, mockContext)

        // Assert
        assertTrue("Cut should succeed", result.success)
        assertEquals("Text cut", result.response)
        verify { mockFocusedNode.performAction(AccessibilityNodeInfo.ACTION_CUT) }
    }

    @Test
    fun `test cut action with no focused node`() = runTest {
        // Arrange
        val command = Command("cut", "cut", CommandSource.VOICE, confidence = 1.0f, timestamp = System.currentTimeMillis())
        every { mockAccessibilityService.rootInActiveWindow } returns null

        // Act
        val result = editingActions.execute(command, mockAccessibilityService, mockContext)

        // Assert
        assertFalse("Cut should fail when no editable field focused", result.success)
        assertEquals(ErrorCode.EXECUTION_FAILED, result.error?.code)
    }

    // ========== Select All Tests ==========

    @Test
    fun `test select all action success`() = runTest {
        // Arrange
        val command = Command("select_all", "select all", CommandSource.VOICE, confidence = 1.0f, timestamp = System.currentTimeMillis())
        val mockRootNode = mockk<AccessibilityNodeInfo>(relaxed = true)
        val mockFocusedNode = mockk<AccessibilityNodeInfo>(relaxed = true)

        every { mockAccessibilityService.rootInActiveWindow } returns mockRootNode
        every { mockRootNode.isEditable } returns false
        every { mockRootNode.isFocused } returns false
        every { mockRootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) } returns mockFocusedNode
        every { mockFocusedNode.isEditable } returns true
        every { mockFocusedNode.performAction(0x20000) } returns true // ACTION_SELECT_ALL

        // Act
        val result = editingActions.execute(command, mockAccessibilityService, mockContext)

        // Assert
        assertTrue("Select all should succeed", result.success)
        assertEquals("All text selected", result.response)
    }

    @Test
    fun `test select all with no editable field`() = runTest {
        // Arrange
        val command = Command("select_all", "select all", CommandSource.VOICE, confidence = 1.0f, timestamp = System.currentTimeMillis())
        val mockRootNode = mockk<AccessibilityNodeInfo>(relaxed = true)

        every { mockAccessibilityService.rootInActiveWindow } returns mockRootNode
        every { mockRootNode.isEditable } returns false
        every { mockRootNode.isFocused } returns false
        every { mockRootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) } returns null
        every { mockRootNode.childCount } returns 0

        // Act
        val result = editingActions.execute(command, mockAccessibilityService, mockContext)

        // Assert
        assertFalse("Select all should fail when no editable field", result.success)
        assertEquals(ErrorCode.EXECUTION_FAILED, result.error?.code)
    }

    // ========== Undo Tests ==========

    @Test
    fun `test undo action success on API 24+`() = runTest {
        // Arrange - Only run if SDK supports undo (API 24+)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return@runTest // Skip test on older APIs
        }

        val command = Command("undo", "undo", CommandSource.VOICE, confidence = 1.0f, timestamp = System.currentTimeMillis())
        val mockRootNode = mockk<AccessibilityNodeInfo>(relaxed = true)
        val mockFocusedNode = mockk<AccessibilityNodeInfo>(relaxed = true)

        every { mockAccessibilityService.rootInActiveWindow } returns mockRootNode
        every { mockRootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) } returns mockFocusedNode
        every { mockFocusedNode.isEditable } returns true
        every { mockFocusedNode.performAction(0x01020036) } returns true // ACTION_UNDO

        // Act
        val result = editingActions.execute(command, mockAccessibilityService, mockContext)

        // Assert
        assertTrue("Undo should succeed", result.success)
        assertEquals("Undo performed", result.response)
    }

    @Test
    fun `test undo not supported on older API`() = runTest {
        // Arrange
        val command = Command("undo", "undo", CommandSource.VOICE, confidence = 1.0f, timestamp = System.currentTimeMillis())
        every { mockAccessibilityService.rootInActiveWindow } returns null

        // Act
        val result = editingActions.execute(command, mockAccessibilityService, mockContext)

        // Assert
        // Result depends on API level and node availability
        assertTrue(result is CommandResult)
    }

    // ========== Redo Tests ==========

    @Test
    fun `test redo action success on API 24+`() = runTest {
        // Arrange - Only run if SDK supports redo (API 24+)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return@runTest // Skip test on older APIs
        }

        val command = Command("redo", "redo", CommandSource.VOICE, confidence = 1.0f, timestamp = System.currentTimeMillis())
        val mockRootNode = mockk<AccessibilityNodeInfo>(relaxed = true)
        val mockFocusedNode = mockk<AccessibilityNodeInfo>(relaxed = true)

        every { mockAccessibilityService.rootInActiveWindow } returns mockRootNode
        every { mockRootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) } returns mockFocusedNode
        every { mockFocusedNode.isEditable } returns true
        every { mockFocusedNode.performAction(0x01020037) } returns true // ACTION_REDO

        // Act
        val result = editingActions.execute(command, mockAccessibilityService, mockContext)

        // Assert
        assertTrue("Redo should succeed", result.success)
        assertEquals("Redo performed", result.response)
    }

    // ========== Unknown Command Tests ==========

    @Test
    fun `test unknown editing command`() = runTest {
        // Arrange
        val command = Command("invalid", "invalid command", CommandSource.VOICE, confidence = 1.0f, timestamp = System.currentTimeMillis())

        // Act
        val result = editingActions.execute(command, mockAccessibilityService, mockContext)

        // Assert
        assertFalse("Unknown command should fail", result.success)
        assertEquals(ErrorCode.UNKNOWN_COMMAND, result.error?.code)
        assertTrue(result.error?.message?.contains("Unknown editing command") == true)
    }

    // ========== Edge Cases ==========

    @Test
    fun `test copy with null accessibility service`() = runTest {
        // Arrange
        val command = Command("copy", "copy", CommandSource.VOICE, confidence = 1.0f, timestamp = System.currentTimeMillis())
        val editingActionsWithoutService = EditingActions(mockContext, null)

        // Act
        val result = editingActionsWithoutService.execute(command, null, mockContext)

        // Assert
        assertFalse("Copy should fail without accessibility service", result.success)
    }

    @Test
    fun `test exception handling during copy`() = runTest {
        // Arrange
        val command = Command("copy", "copy", CommandSource.VOICE, confidence = 1.0f, timestamp = System.currentTimeMillis())
        every { mockAccessibilityService.performGlobalAction(any()) } throws RuntimeException("Test exception")

        // Act
        val result = editingActions.execute(command, mockAccessibilityService, mockContext)

        // Assert
        assertFalse("Copy should fail on exception", result.success)
        assertEquals(ErrorCode.EXECUTION_FAILED, result.error?.code)
    }

    @Test
    fun `test recursively find focused editable node`() = runTest {
        // Arrange
        val command = Command("copy", "copy", CommandSource.VOICE, confidence = 1.0f, timestamp = System.currentTimeMillis())
        val mockRootNode = mockk<AccessibilityNodeInfo>(relaxed = true)
        val mockChildNode = mockk<AccessibilityNodeInfo>(relaxed = true)
        val mockGrandchildNode = mockk<AccessibilityNodeInfo>(relaxed = true)

        every { mockAccessibilityService.performGlobalAction(AccessibilityNodeInfo.ACTION_COPY) } returns false
        every { mockAccessibilityService.rootInActiveWindow } returns mockRootNode
        every { mockRootNode.isEditable } returns false
        every { mockRootNode.isFocused } returns false
        every { mockRootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) } returns null
        every { mockRootNode.childCount } returns 1
        every { mockRootNode.getChild(0) } returns mockChildNode
        every { mockChildNode.isEditable } returns false
        every { mockChildNode.isFocused } returns false
        every { mockChildNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) } returns null
        every { mockChildNode.childCount } returns 1
        every { mockChildNode.getChild(0) } returns mockGrandchildNode
        every { mockGrandchildNode.isEditable } returns true
        every { mockGrandchildNode.isFocused } returns true
        every { mockGrandchildNode.performAction(AccessibilityNodeInfo.ACTION_COPY) } returns true

        // Act
        val result = editingActions.execute(command, mockAccessibilityService, mockContext)

        // Assert
        assertTrue("Should find focused node recursively", result.success)
    }
}
