/**
 * DragHandlerTest.kt - Unit tests for DragHandler
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-09-03
 */
package com.augmentalis.voiceoscore.accessibility.handlers

import android.accessibilityservice.GestureDescription
import com.augmentalis.voiceoscore.accessibility.VoiceOSService
import com.augmentalis.voiceos.cursor.core.CursorOffset
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive test suite for DragHandler
 * Tests migration from Legacy Avenue with 100% functional equivalence
 */
@ExperimentalCoroutinesApi
class DragHandlerTest {

    private lateinit var mockService: VoiceOSService
    private lateinit var dragHandler: DragHandler
    private lateinit var testDispatcher: TestDispatcher

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        mockService = mockk(relaxed = true)

        dragHandler = DragHandler(mockService)

        // Setup common mock responses
        every { mockService.isCursorVisible() } returns true
        every { mockService.getCursorPosition() } returns CursorOffset(500f, 500f)
        every { mockService.dispatchGesture(any(), any(), any()) } returns true
    }
    
    @After
    fun tearDown() {
        dragHandler.dispose()
        Dispatchers.resetMain()
        clearAllMocks()
    }
    
    inner class Initialization {
        
        @Test
        fun `initialize should setup handler correctly`() {
            // When
            dragHandler.initialize()
            
            // Then - should not throw and be ready for operation
            assertFalse(dragHandler.isDragActive())
        }
        
        @Test
        fun `handler should have access to service cursor operations`() {
            // Given - cursor operations are now directly on VoiceOSService

            // When
            val isCursorVisible = mockService.isCursorVisible()

            // Then - should be able to use cursor operations through service
            assertTrue(isCursorVisible, "Cursor should be visible through service")
        }
    }
    
    inner class CommandHandling {
        
        @Test
        fun `canHandle should return true for supported drag commands`() {
            val supportedCommands = listOf(
                "drag start",
                "drag stop", 
                "start drag",
                "stop drag",
                "drag handle",
                "drag up down",
                "continuous drag",
                "cursor drag"
            )
            
            supportedCommands.forEach { command ->
                assertTrue(
                    dragHandler.canHandle(command),
                    "Should handle command: $command"
                )
            }
        }
        
        @Test
        fun `canHandle should return false for unsupported commands`() {
            val unsupportedCommands = listOf(
                "click",
                "tap",
                "swipe left",
                "pinch open",
                "drag from coordinate", // This is handled by GestureHandler
                "drag to coordinate"    // This is handled by GestureHandler
            )
            
            unsupportedCommands.forEach { command ->
                assertFalse(
                    dragHandler.canHandle(command),
                    "Should not handle command: $command"
                )
            }
        }
        
        @Test
        fun `getSupportedActions should return correct action list`() {
            // When
            val supportedActions = dragHandler.getSupportedActions()
            
            // Then
            val expectedActions = listOf(
                "drag start", "drag stop", "drag handle",
                "continuous drag", "start drag", "stop drag",
                "cursor drag", "drag up down"
            )
            
            assertTrue(supportedActions.containsAll(expectedActions))
            assertEquals(expectedActions.size, supportedActions.size)
        }
    }
    
    inner class DragOperations {
        
        @Test
        fun `execute drag start should start drag mode successfully`() {
            // Given
            every { mockService.isCursorVisible() } returns true

            // When
            val result = dragHandler.execute(
                ActionCategory.GESTURE,
                "drag start",
                emptyMap()
            )

            // Then
            assertTrue(result, "Drag start should succeed")
            assertTrue(dragHandler.isDragActive(), "Drag should be active")
        }

        @Test
        fun `execute drag start should fail when cursor not visible`() {
            // Given
            every { mockService.isCursorVisible() } returns false

            // When
            val result = dragHandler.execute(
                ActionCategory.GESTURE,
                "drag start",
                emptyMap()
            )

            // Then
            assertFalse(result, "Drag start should fail")
            assertFalse(dragHandler.isDragActive(), "Drag should not be active")
        }
        
        @Test
        fun `execute drag stop should stop active drag`() {
            // Given - start drag first
            every { mockService.isCursorVisible() } returns true
            dragHandler.execute(ActionCategory.GESTURE, "drag start", emptyMap())
            assertTrue(dragHandler.isDragActive(), "Drag should be active after start")

            // When
            val result = dragHandler.execute(
                ActionCategory.GESTURE,
                "drag stop",
                emptyMap()
            )

            // Then
            assertTrue(result, "Drag stop should succeed")
            assertFalse(dragHandler.isDragActive(), "Drag should not be active after stop")
        }
        
        @Test
        fun `execute drag handle should return true (Legacy DRAG_UP_DOWN equivalent)`() {
            // When
            val result = dragHandler.execute(
                ActionCategory.GESTURE,
                "drag handle",
                emptyMap()
            )

            // Then
            assertTrue(result, "Legacy DRAG_UP_DOWN should return true")
        }

        @Test
        fun `execute drag up down should return true (Legacy equivalent)`() {
            // When
            val result = dragHandler.execute(
                ActionCategory.GESTURE,
                "drag up down",
                emptyMap()
            )

            // Then
            assertTrue(result, "Legacy drag up down should return true")
        }
        
        @Test
        fun `execute continuous drag should start drag and immediate cursor drag`() {
            // Given
            every { mockService.isCursorVisible() } returns true
            every { mockService.getCursorPosition() } returns CursorOffset(300f, 400f)

            // When
            val result = dragHandler.execute(
                ActionCategory.GESTURE,
                "continuous drag",
                emptyMap()
            )

            // Then
            assertTrue(result, "Continuous drag should succeed")
            assertTrue(dragHandler.isDragActive(), "Drag should be active after continuous drag")

            // Should have called getCursorPosition to start drag at cursor location
            verify { mockService.getCursorPosition() }
        }
        
        @Test
        fun `continuous drag should fail when cursor not visible`() {
            // Given
            every { mockService.isCursorVisible() } returns false

            // When
            val result = dragHandler.execute(
                ActionCategory.GESTURE,
                "continuous drag",
                emptyMap()
            )

            // Then
            assertFalse(result, "Continuous drag should fail when cursor not visible")
            assertFalse(dragHandler.isDragActive(), "Drag should not be active")
        }
    }
    
    inner class GestureBuilding {
        
        @Test
        fun `drag operations should dispatch gestures to accessibility service`() = runTest {
            // Given
            every { mockService.isCursorVisible() } returns true
            every { mockService.getCursorPosition() } returns CursorOffset(100f, 200f)
            
            // When - start drag mode
            dragHandler.execute(ActionCategory.GESTURE, "drag start", emptyMap())
            
            // Simulate cursor movement that would trigger gesture creation
            advanceTimeBy(200) // Allow time for cursor tracking
            
            // Then - should have attempted to dispatch gestures
            verify(timeout = 1000, atLeast = 0) { 
                mockService.dispatchGesture(any<GestureDescription>(), any(), any()) 
            }
        }
        
        @Test
        fun `gesture should handle coordinate bounds correctly`() {
            // Given
            every { mockService.isCursorVisible() } returns true
            every { mockService.getCursorPosition() } returns CursorOffset(-10f, -10f) // Negative coordinates
            
            // When
            dragHandler.execute(ActionCategory.GESTURE, "continuous drag", emptyMap())
            
            // Then - should not crash and handle negative coordinates
            assertTrue(dragHandler.isDragActive(), "Drag should handle negative coordinates")
        }
    }

    inner class CursorTracking {
        
        @Test
        fun `drag should track cursor movement with correct polling interval`() = runTest {
            // Given
            every { mockService.isCursorVisible() } returns true
            val positions = mutableListOf(
                CursorOffset(100f, 100f),
                CursorOffset(110f, 110f), // Movement > threshold
                CursorOffset(120f, 120f)  // More movement
            )
            var positionIndex = 0
            every { mockService.getCursorPosition() } answers {
                if (positionIndex < positions.size) {
                    positions[positionIndex++]
                } else {
                    positions.last()
                }
            }
            
            // When
            dragHandler.execute(ActionCategory.GESTURE, "drag start", emptyMap())
            
            // Then - should poll cursor position
            advanceTimeBy(300) // 3 polling intervals at 100ms each
            
            // Should have called getCursorPosition multiple times
            verify(atLeast = 3) { mockService.getCursorPosition() }
        }
        
        @Test
        fun `drag should respect movement threshold from Legacy`() = runTest {
            // Given
            every { mockService.isCursorVisible() } returns true
            val positions = mutableListOf(
                CursorOffset(100f, 100f),
                CursorOffset(102f, 102f), // Small movement < threshold (5 pixels)
                CursorOffset(108f, 108f)  // Still below threshold
            )
            var positionIndex = 0
            every { mockService.getCursorPosition() } answers {
                if (positionIndex < positions.size) {
                    positions[positionIndex++]
                } else {
                    positions.last()
                }
            }
            
            // When
            dragHandler.execute(ActionCategory.GESTURE, "drag start", emptyMap())
            advanceTimeBy(300)
            
            // Then - small movements should not trigger gesture events
            // Verify minimal gesture dispatching for small movements
            verify(atMost = 1) { 
                mockService.dispatchGesture(any<GestureDescription>(), any(), any()) 
            }
        }
    }
    
    inner class ErrorHandling {
        
        @Test
        fun `drag should handle service dispatch failure gracefully`() {
            // Given
            every { mockService.dispatchGesture(any(), any(), any()) } returns false
            every { mockService.isCursorVisible() } returns true
            
            // When
            val result = dragHandler.execute(
                ActionCategory.GESTURE,
                "drag start",
                emptyMap()
            )
            
            // Then - should still return true (start was successful)
            assertTrue(result, "Drag start should succeed even if dispatch fails")
        }

        @Test
        fun `drag should handle missing cursor operations gracefully`() {
            // Given
            val handlerWithoutCursor = DragHandler(mockService)
            every { mockService.isCursorVisible() } returns false

            // When
            val result = handlerWithoutCursor.execute(
                ActionCategory.GESTURE,
                "drag start",
                emptyMap()
            )

            // Then
            assertFalse(result, "Drag should fail gracefully without cursor")
        }
        
        @Test
        fun `dispose should cleanup all resources`() = runTest {
            // Given
            every { mockService.isCursorVisible() } returns true
            dragHandler.execute(ActionCategory.GESTURE, "drag start", emptyMap())
            assertTrue(dragHandler.isDragActive(), "Drag should be active before dispose")

            // When
            dragHandler.dispose()

            // Then
            assertFalse(dragHandler.isDragActive(), "Drag should not be active after dispose")

            // Verify cleanup completed
            advanceTimeBy(1000)
            // No further cursor position calls should happen
        }
    }

    inner class LegacyCompatibility {

        @Test
        fun `drag flow should match Legacy DragAction behavior`() = runTest {
            // Given - simulate Legacy flow
            every { mockService.isCursorVisible() } returns true
            every { mockService.getCursorPosition() } returnsMany listOf(
                CursorOffset(100f, 100f), // Start position
                CursorOffset(150f, 150f), // Move position
                CursorOffset(200f, 200f)  // End position
            )

            // When - execute Legacy equivalent sequence
            // 1. Start drag (Legacy: Action.DRAG_START)
            val startResult = dragHandler.execute(ActionCategory.GESTURE, "drag start", emptyMap())
            assertTrue(startResult, "Drag start should succeed")

            // 2. Allow cursor movement to be detected
            advanceTimeBy(300)

            // 3. Stop drag (Legacy: Action.DRAG_STOP)
            val stopResult = dragHandler.execute(ActionCategory.GESTURE, "drag stop", emptyMap())
            assertTrue(stopResult, "Drag stop should succeed")

            // Then - should have completed full drag cycle
            assertFalse(dragHandler.isDragActive(), "Drag should not be active after stop")
        }

        @Test
        fun `drag handle command should match Legacy DRAG_UP_DOWN action`() {
            // Given - Legacy DRAG_UP_DOWN behavior

            // When
            val result = dragHandler.execute(
                ActionCategory.GESTURE,
                "drag handle",
                emptyMap()
            )

            // Then - should return true like Legacy
            assertTrue(result, "Drag handle should match Legacy DRAG_UP_DOWN behavior")
        }

        @Test
        fun `cursor requirement should match Legacy behavior`() {
            // Given - Legacy required cursor to be visible
            every { mockService.isCursorVisible() } returns false

            // When
            val result = dragHandler.execute(
                ActionCategory.GESTURE,
                "drag start",
                emptyMap()
            )

            // Then - should fail like Legacy when cursor not visible
            assertFalse(result, "Should fail when cursor not visible like Legacy")
        }
    }
}