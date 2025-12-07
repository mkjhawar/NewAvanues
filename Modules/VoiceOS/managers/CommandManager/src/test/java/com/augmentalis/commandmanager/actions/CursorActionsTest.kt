/**
 * CursorActionsTest.kt - Unit tests for CursorActions voice command handlers
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-10-14
 *
 * Test Coverage:
 * - Cursor movement (up, down, left, right)
 * - Click actions (single, double, long press)
 * - Cursor visibility (show, hide, toggle, center)
 * - Cursor configuration (coordinates, cursor type)
 * - System commands (menu, settings, calibrate)
 * - Scrolling (up, down)
 *
 * Architecture: Tests CursorActions object delegation to VoiceCursorAPI
 */
package com.augmentalis.commandmanager.actions

import android.content.Context
import com.augmentalis.voiceos.cursor.VoiceCursorAPI
import com.augmentalis.voiceos.cursor.core.CursorConfig
import com.augmentalis.voiceos.cursor.core.CursorOffset
import com.augmentalis.voiceos.cursor.core.CursorType
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*

class CursorActionsTest {

    private lateinit var mockContext: Context

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)

        // Mock VoiceCursorAPI static methods
        mockkObject(VoiceCursorAPI)
    }

    @After
    fun teardown() {
        unmockkObject(VoiceCursorAPI)
        clearAllMocks()
    }

    // ========== Movement Tests ==========

    @Test
    fun `test move cursor up`() = runTest {
        // Arrange
        val startPosition = CursorOffset(500f, 500f)
        val expectedPosition = CursorOffset(500f, 450f)

        every { VoiceCursorAPI.getCurrentPosition() } returns startPosition
        every { VoiceCursorAPI.moveTo(expectedPosition, animate = true) } returns true

        // Act
        val result = CursorActions.moveCursor(CursorDirection.UP, distance = 50f)

        // Assert
        assertTrue("Move up should succeed", result)
        verify { VoiceCursorAPI.getCurrentPosition() }
        verify { VoiceCursorAPI.moveTo(expectedPosition, animate = true) }
    }

    @Test
    fun `test move cursor down`() = runTest {
        // Arrange
        val startPosition = CursorOffset(500f, 500f)
        val expectedPosition = CursorOffset(500f, 550f)

        every { VoiceCursorAPI.getCurrentPosition() } returns startPosition
        every { VoiceCursorAPI.moveTo(expectedPosition, animate = true) } returns true

        // Act
        val result = CursorActions.moveCursor(CursorDirection.DOWN, distance = 50f)

        // Assert
        assertTrue("Move down should succeed", result)
        verify { VoiceCursorAPI.moveTo(expectedPosition, animate = true) }
    }

    @Test
    fun `test move cursor left`() = runTest {
        // Arrange
        val startPosition = CursorOffset(500f, 500f)
        val expectedPosition = CursorOffset(450f, 500f)

        every { VoiceCursorAPI.getCurrentPosition() } returns startPosition
        every { VoiceCursorAPI.moveTo(expectedPosition, animate = true) } returns true

        // Act
        val result = CursorActions.moveCursor(CursorDirection.LEFT, distance = 50f)

        // Assert
        assertTrue("Move left should succeed", result)
        verify { VoiceCursorAPI.moveTo(expectedPosition, animate = true) }
    }

    @Test
    fun `test move cursor right`() = runTest {
        // Arrange
        val startPosition = CursorOffset(500f, 500f)
        val expectedPosition = CursorOffset(550f, 500f)

        every { VoiceCursorAPI.getCurrentPosition() } returns startPosition
        every { VoiceCursorAPI.moveTo(expectedPosition, animate = true) } returns true

        // Act
        val result = CursorActions.moveCursor(CursorDirection.RIGHT, distance = 50f)

        // Assert
        assertTrue("Move right should succeed", result)
        verify { VoiceCursorAPI.moveTo(expectedPosition, animate = true) }
    }

    @Test
    fun `test move cursor with custom distance`() = runTest {
        // Arrange
        val startPosition = CursorOffset(500f, 500f)
        val expectedPosition = CursorOffset(500f, 400f)

        every { VoiceCursorAPI.getCurrentPosition() } returns startPosition
        every { VoiceCursorAPI.moveTo(expectedPosition, animate = true) } returns true

        // Act
        val result = CursorActions.moveCursor(CursorDirection.UP, distance = 100f)

        // Assert
        assertTrue("Move with custom distance should succeed", result)
        verify { VoiceCursorAPI.moveTo(expectedPosition, animate = true) }
    }

    @Test
    fun `test move cursor when not initialized`() = runTest {
        // Arrange
        every { VoiceCursorAPI.getCurrentPosition() } returns null

        // Act
        val result = CursorActions.moveCursor(CursorDirection.UP)

        // Assert
        assertFalse("Move should fail when cursor not initialized", result)
        verify(exactly = 0) { VoiceCursorAPI.moveTo(any(), any()) }
    }

    // ========== Click Actions Tests ==========

    @Test
    fun `test single click success`() = runTest {
        // Arrange
        every { VoiceCursorAPI.click() } returns true

        // Act
        val result = CursorActions.click()

        // Assert
        assertTrue("Single click should succeed", result)
        verify { VoiceCursorAPI.click() }
    }

    @Test
    fun `test single click failure`() = runTest {
        // Arrange
        every { VoiceCursorAPI.click() } returns false

        // Act
        val result = CursorActions.click()

        // Assert
        assertFalse("Single click should fail", result)
        verify { VoiceCursorAPI.click() }
    }

    @Test
    fun `test double click success`() = runTest {
        // Arrange
        every { VoiceCursorAPI.doubleClick() } returns true

        // Act
        val result = CursorActions.doubleClick()

        // Assert
        assertTrue("Double click should succeed", result)
        verify { VoiceCursorAPI.doubleClick() }
    }

    @Test
    fun `test long press success`() = runTest {
        // Arrange
        every { VoiceCursorAPI.longPress() } returns true

        // Act
        val result = CursorActions.longPress()

        // Assert
        assertTrue("Long press should succeed", result)
        verify { VoiceCursorAPI.longPress() }
    }

    @Test
    fun `test click exception handling`() = runTest {
        // Arrange
        every { VoiceCursorAPI.click() } throws RuntimeException("Test exception")

        // Act
        val result = CursorActions.click()

        // Assert
        assertFalse("Click should return false on exception", result)
    }

    // ========== Visibility Tests ==========

    @Test
    fun `test show cursor with default config`() = runTest {
        // Arrange
        every { VoiceCursorAPI.showCursor(any()) } returns true

        // Act
        val result = CursorActions.showCursor()

        // Assert
        assertTrue("Show cursor should succeed", result)
        verify { VoiceCursorAPI.showCursor(any()) }
    }

    @Test
    fun `test show cursor with custom config`() = runTest {
        // Arrange
        val customConfig = CursorConfig(type = CursorType.Hand, showCoordinates = true)
        every { VoiceCursorAPI.showCursor(customConfig) } returns true

        // Act
        val result = CursorActions.showCursor(customConfig)

        // Assert
        assertTrue("Show cursor with custom config should succeed", result)
        verify { VoiceCursorAPI.showCursor(customConfig) }
    }

    @Test
    fun `test hide cursor success`() = runTest {
        // Arrange
        every { VoiceCursorAPI.hideCursor() } returns true

        // Act
        val result = CursorActions.hideCursor()

        // Assert
        assertTrue("Hide cursor should succeed", result)
        verify { VoiceCursorAPI.hideCursor() }
    }

    @Test
    fun `test center cursor success`() = runTest {
        // Arrange
        every { VoiceCursorAPI.centerCursor() } returns true

        // Act
        val result = CursorActions.centerCursor()

        // Assert
        assertTrue("Center cursor should succeed", result)
        verify { VoiceCursorAPI.centerCursor() }
    }

    @Test
    fun `test toggle cursor success`() = runTest {
        // Arrange
        every { VoiceCursorAPI.toggleCursor() } returns true

        // Act
        val result = CursorActions.toggleCursor()

        // Assert
        assertTrue("Toggle cursor should succeed", result)
        verify { VoiceCursorAPI.toggleCursor() }
    }

    // ========== Coordinates Tests ==========

    @Test
    fun `test show coordinates`() = runTest {
        // Arrange
        every { VoiceCursorAPI.updateConfiguration(any()) } returns true

        // Act
        val result = CursorActions.showCoordinates()

        // Assert
        assertTrue("Show coordinates should succeed", result)
        verify { VoiceCursorAPI.updateConfiguration(match { it.showCoordinates == true }) }
    }

    @Test
    fun `test hide coordinates`() = runTest {
        // Arrange
        every { VoiceCursorAPI.updateConfiguration(any()) } returns true

        // Act
        val result = CursorActions.hideCoordinates()

        // Assert
        assertTrue("Hide coordinates should succeed", result)
        verify { VoiceCursorAPI.updateConfiguration(match { it.showCoordinates == false }) }
    }

    @Test
    fun `test toggle coordinates`() = runTest {
        // Arrange
        every { VoiceCursorAPI.updateConfiguration(any()) } returns true

        // Act
        val result = CursorActions.toggleCoordinates()

        // Assert
        assertTrue("Toggle coordinates should succeed", result)
        verify { VoiceCursorAPI.updateConfiguration(any()) }
    }

    // ========== Cursor Type Tests ==========

    @Test
    fun `test set cursor type to Hand`() = runTest {
        // Arrange
        every { VoiceCursorAPI.updateConfiguration(any()) } returns true

        // Act
        val result = CursorActions.setCursorType(CursorType.Hand)

        // Assert
        assertTrue("Set cursor type to Hand should succeed", result)
        verify { VoiceCursorAPI.updateConfiguration(match { it.type == CursorType.Hand }) }
    }

    @Test
    fun `test set cursor type to Normal`() = runTest {
        // Arrange
        every { VoiceCursorAPI.updateConfiguration(any()) } returns true

        // Act
        val result = CursorActions.setCursorType(CursorType.Normal)

        // Assert
        assertTrue("Set cursor type to Normal should succeed", result)
        verify { VoiceCursorAPI.updateConfiguration(match { it.type == CursorType.Normal }) }
    }

    // ========== System Commands Tests ==========

    @Test
    fun `test show menu`() = runTest {
        // Arrange
        every { VoiceCursorAPI.showCursor() } returns true

        // Act
        val result = CursorActions.showMenu()

        // Assert
        assertTrue("Show menu should succeed", result)
        verify { VoiceCursorAPI.showCursor() }
    }

    @Test
    fun `test open settings success`() = runTest {
        // Arrange
        val mockContext = mockk<Context>(relaxed = true)

        // Act
        val result = CursorActions.openSettings(mockContext)

        // Assert
        assertTrue("Open settings should succeed", result)
        verify { mockContext.startActivity(any()) }
    }

    @Test
    fun `test open settings exception handling`() = runTest {
        // Arrange
        val mockContext = mockk<Context>(relaxed = true)
        every { mockContext.startActivity(any()) } throws RuntimeException("Test exception")

        // Act
        val result = CursorActions.openSettings(mockContext)

        // Assert
        assertFalse("Open settings should return false on exception", result)
    }

    @Test
    fun `test calibrate returns false`() = runTest {
        // Act
        val result = CursorActions.calibrate()

        // Assert
        assertFalse("Calibrate should return false (not implemented yet)", result)
    }

    // ========== Scrolling Tests ==========

    @Test
    fun `test scroll up success`() = runTest {
        // Arrange
        every { VoiceCursorAPI.scrollUp() } returns true

        // Act
        val result = CursorActions.scrollUp()

        // Assert
        assertTrue("Scroll up should succeed", result)
        verify { VoiceCursorAPI.scrollUp() }
    }

    @Test
    fun `test scroll down success`() = runTest {
        // Arrange
        every { VoiceCursorAPI.scrollDown() } returns true

        // Act
        val result = CursorActions.scrollDown()

        // Assert
        assertTrue("Scroll down should succeed", result)
        verify { VoiceCursorAPI.scrollDown() }
    }

    @Test
    fun `test scroll up failure`() = runTest {
        // Arrange
        every { VoiceCursorAPI.scrollUp() } returns false

        // Act
        val result = CursorActions.scrollUp()

        // Assert
        assertFalse("Scroll up should fail", result)
        verify { VoiceCursorAPI.scrollUp() }
    }

    // ========== Edge Cases ==========

    @Test
    fun `test move with zero distance`() = runTest {
        // Arrange
        val startPosition = CursorOffset(500f, 500f)
        every { VoiceCursorAPI.getCurrentPosition() } returns startPosition
        every { VoiceCursorAPI.moveTo(startPosition, animate = true) } returns true

        // Act
        val result = CursorActions.moveCursor(CursorDirection.UP, distance = 0f)

        // Assert
        assertTrue("Move with zero distance should succeed", result)
        verify { VoiceCursorAPI.moveTo(startPosition, animate = true) }
    }

    @Test
    fun `test move with negative distance`() = runTest {
        // Arrange
        val startPosition = CursorOffset(500f, 500f)
        val expectedPosition = CursorOffset(500f, 550f) // Negative distance = opposite direction
        every { VoiceCursorAPI.getCurrentPosition() } returns startPosition
        every { VoiceCursorAPI.moveTo(expectedPosition, animate = true) } returns true

        // Act
        val result = CursorActions.moveCursor(CursorDirection.UP, distance = -50f)

        // Assert
        assertTrue("Move with negative distance should succeed", result)
        verify { VoiceCursorAPI.moveTo(expectedPosition, animate = true) }
    }

    @Test
    fun `test concurrent calls`() = runTest {
        // Arrange
        every { VoiceCursorAPI.click() } returns true
        every { VoiceCursorAPI.doubleClick() } returns true

        // Act
        val result1 = CursorActions.click()
        val result2 = CursorActions.doubleClick()

        // Assert
        assertTrue(result1)
        assertTrue(result2)
        verify(exactly = 1) { VoiceCursorAPI.click() }
        verify(exactly = 1) { VoiceCursorAPI.doubleClick() }
    }
}
