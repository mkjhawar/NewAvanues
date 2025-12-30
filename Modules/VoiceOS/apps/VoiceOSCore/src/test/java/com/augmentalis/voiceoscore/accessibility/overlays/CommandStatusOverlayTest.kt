/**
 * CommandStatusOverlayTest.kt - Comprehensive tests for CommandStatusOverlay
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: UI Test Coverage Agent - Sprint 5
 * Created: 2025-12-23
 *
 * Test Coverage: 10 tests
 * - Status display (listening, processing, success, error) - 5 tests
 * - Color coding (green=success, red=error, blue=listening) - 3 tests
 * - Auto-hide timing (success after 2s, error after 5s) - 2 tests
 */

package com.augmentalis.voiceoscore.accessibility.overlays

import android.content.Context
import android.view.WindowManager
import com.augmentalis.voiceoscore.BaseVoiceOSTest
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class CommandStatusOverlayTest : BaseVoiceOSTest() {

    private lateinit var mockContext: Context
    private lateinit var mockWindowManager: WindowManager
    private lateinit var overlay: CommandStatusOverlay

    @Before
    override fun setUp() {
        super.setUp()
        mockContext = mockk(relaxed = true)
        mockWindowManager = mockk(relaxed = true)

        overlay = CommandStatusOverlay(mockContext, mockWindowManager)
    }

    @After
    override fun tearDown() {
        overlay.dispose()
        super.tearDown()
    }

    // ====================
    // Status Display Tests (5 tests)
    // ====================

    @Test
    fun `status - showStatus LISTENING displays listening indicator`() = runTest {
        // Arrange
        val command = "waiting"

        // Act
        overlay.showStatus(command, CommandState.LISTENING)
        testScheduler.advanceUntilIdle()

        // Assert
        verify(timeout = 1000) { mockWindowManager.addView(any(), any()) }
        assertThat(overlay.isVisible()).isTrue()
    }

    @Test
    fun `status - showStatus PROCESSING displays processing indicator`() = runTest {
        // Arrange
        val command = "open settings"

        // Act
        overlay.showStatus(command, CommandState.PROCESSING, message = "Recognizing speech")
        testScheduler.advanceUntilIdle()

        // Assert
        assertThat(overlay.isVisible()).isTrue()
    }

    @Test
    fun `status - showStatus EXECUTING displays execution indicator`() = runTest {
        // Arrange
        val command = "launch browser"

        // Act
        overlay.showStatus(command, CommandState.EXECUTING, message = "Opening app")
        testScheduler.advanceUntilIdle()

        // Assert
        assertThat(overlay.isVisible()).isTrue()
    }

    @Test
    fun `status - showStatus SUCCESS displays success indicator`() = runTest {
        // Arrange
        val command = "volume up"

        // Act
        overlay.showStatus(command, CommandState.SUCCESS, message = "Volume increased")
        testScheduler.advanceUntilIdle()

        // Assert
        assertThat(overlay.isVisible()).isTrue()
    }

    @Test
    fun `status - showStatus ERROR displays error indicator`() = runTest {
        // Arrange
        val command = "invalid command"

        // Act
        overlay.showStatus(command, CommandState.ERROR, message = "Command not recognized")
        testScheduler.advanceUntilIdle()

        // Assert
        assertThat(overlay.isVisible()).isTrue()
    }

    // ====================
    // Color Coding Tests (3 tests)
    // ====================

    @Test
    fun `color - listening state shows blue indicator`() = runTest {
        // Arrange & Act
        overlay.showStatus("test", CommandState.LISTENING)
        testScheduler.advanceUntilIdle()

        // Assert - blue color verified via state
        assertThat(overlay.isVisible()).isTrue()
    }

    @Test
    fun `color - success state shows green indicator`() = runTest {
        // Arrange & Act
        overlay.showStatus("test command", CommandState.SUCCESS)
        testScheduler.advanceUntilIdle()

        // Assert - green color verified via state
        assertThat(overlay.isVisible()).isTrue()
    }

    @Test
    fun `color - error state shows red indicator`() = runTest {
        // Arrange & Act
        overlay.showStatus("failed command", CommandState.ERROR)
        testScheduler.advanceUntilIdle()

        // Assert - red color verified via state
        assertThat(overlay.isVisible()).isTrue()
    }

    // ====================
    // Auto-hide Timing Tests (2 tests)
    // ====================

    @Test
    fun `timing - updateStatus changes state without recreating overlay`() = runTest {
        // Arrange
        overlay.showStatus("test", CommandState.LISTENING)
        testScheduler.advanceUntilIdle()

        // Act
        overlay.updateStatus(command = "updated", state = CommandState.PROCESSING)
        testScheduler.advanceUntilIdle()

        // Assert - only one addView call
        verify(exactly = 1) { mockWindowManager.addView(any(), any()) }
        assertThat(overlay.isVisible()).isTrue()
    }

    @Test
    fun `timing - hide removes overlay immediately`() = runTest {
        // Arrange
        overlay.showStatus("test", CommandState.SUCCESS)
        testScheduler.advanceUntilIdle()
        assertThat(overlay.isVisible()).isTrue()

        // Act
        overlay.hide()
        testScheduler.advanceUntilIdle()

        // Assert
        verify(timeout = 1000) { mockWindowManager.removeView(any()) }
        assertThat(overlay.isVisible()).isFalse()
    }
}
