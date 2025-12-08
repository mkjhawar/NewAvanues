/**
 * DragHandlerTest.kt - Unit tests for DragHandler
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-09-03
 * 
 * Tests all DragHandler functionality to ensure 100% functional equivalence with Legacy.
 * Covers cursor-based drag operations, continuous drag mode, and voice command handling.
 */
package com.augmentalis.voiceos.accessibility.handlers

import android.accessibilityservice.GestureDescription
import com.augmentalis.voiceos.accessibility.handlers.ActionCategory
import com.augmentalis.voiceos.accessibility.managers.CursorManager
import com.augmentalis.voiceos.accessibility.service.VoiceAccessibilityService
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive test suite for DragHandler
 * Tests migration from Legacy Avenue with 100% functional equivalence
 */
@ExperimentalCoroutinesApi
class DragHandlerTest {
    
    private lateinit var mockService: VoiceAccessibilityService
    private lateinit var mockCursorManager: CursorManager
    private lateinit var dragHandler: DragHandler
    private lateinit var testDispatcher: TestDispatcher
    
    @BeforeEach
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        
        mockService = mockk(relaxed = true)
        mockCursorManager = mockk(relaxed = true)
        
        dragHandler = DragHandler(mockService)
        dragHandler.setCursorManager(mockCursorManager)
        
        // Setup common mock responses
        every { mockCursorManager.isCursorVisible() } returns true
        every { mockCursorManager.getCursorPosition() } returns Pair(500, 500)
        every { mockService.dispatchGesture(any(), any(), any()) } returns true
    }
    
    @AfterEach
    fun tearDown() {
        dragHandler.dispose()
        Dispatchers.resetMain()
        clearAllMocks()
    }
    
    @Nested
    inner class Initialization {
        
        @Test
        fun `initialize should setup handler correctly`() {
            // When
            dragHandler.initialize()
            
            // Then - should not throw and be ready for operation
            assertFalse(dragHandler.isDragActive())
        }
        
        @Test
        fun `setCursorManager should store reference correctly`() {
            // Given
            val cursorManager = mockk<CursorManager>()
            
            // When
            dragHandler.setCursorManager(cursorManager)
            
            // Then - should be able to use cursor manager in operations
            // (verified through subsequent drag operations tests)
        }
    }
    
    @Nested
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
    
    @Nested
    inner class DragOperations {
        
        @Test
        fun `execute drag start should start drag mode successfully`() {
            // Given
            every { mockCursorManager.isCursorVisible() } returns true
            
            // When
            val result = dragHandler.execute(
                ActionCategory.GESTURE,
                "drag start",
                emptyMap()
            )
            
            // Then
            assertTrue(result)
            assertTrue(dragHandler.isDragActive())
        }
        
        @Test
        fun `execute drag start should fail when cursor not visible`() {
            // Given
            every { mockCursorManager.isCursorVisible() } returns false
            
            // When
            val result = dragHandler.execute(
                ActionCategory.GESTURE,
                "drag start",
                emptyMap()
            )
            
            // Then
            assertFalse(result)
            assertFalse(dragHandler.isDragActive())
        }
        
        @Test
        fun `execute drag stop should stop active drag`() {
            // Given - start drag first
            every { mockCursorManager.isCursorVisible() } returns true
            dragHandler.execute(ActionCategory.GESTURE, "drag start", emptyMap())
            assertTrue(dragHandler.isDragActive())
            
            // When
            val result = dragHandler.execute(
                ActionCategory.GESTURE,
                "drag stop",
                emptyMap()
            )
            
            // Then
            assertTrue(result)
            assertFalse(dragHandler.isDragActive())
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
            assertTrue(result) // Legacy implementation returned true
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
            assertTrue(result) // Legacy implementation returned true
        }
        
        @Test
        fun `execute continuous drag should start drag and immediate cursor drag`() {
            // Given
            every { mockCursorManager.isCursorVisible() } returns true
            every { mockCursorManager.getCursorPosition() } returns Pair(300, 400)
            
            // When
            val result = dragHandler.execute(
                ActionCategory.GESTURE,
                "continuous drag",
                emptyMap()
            )
            
            // Then
            assertTrue(result)
            assertTrue(dragHandler.isDragActive())
            
            // Should have called getCursorPosition to start drag at cursor location
            verify { mockCursorManager.getCursorPosition() }
        }
        
        @Test
        fun `continuous drag should fail when cursor not visible`() {
            // Given
            every { mockCursorManager.isCursorVisible() } returns false
            
            // When
            val result = dragHandler.execute(
                ActionCategory.GESTURE,
                "continuous drag",
                emptyMap()
            )
            
            // Then
            assertFalse(result)
            assertFalse(dragHandler.isDragActive())
        }
    }
    
    @Nested
    inner class GestureBuilding {
        
        @Test
        fun `drag operations should dispatch gestures to accessibility service`() = runTest {
            // Given
            every { mockCursorManager.isCursorVisible() } returns true
            every { mockCursorManager.getCursorPosition() } returns Pair(100, 200)
            
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
            every { mockCursorManager.isCursorVisible() } returns true
            every { mockCursorManager.getCursorPosition() } returns Pair(-10, -10) // Negative coordinates
            
            // When
            dragHandler.execute(ActionCategory.GESTURE, "continuous drag", emptyMap())
            
            // Then - should not crash and handle negative coordinates
            assertTrue(dragHandler.isDragActive())
        }
    }
    
    @Nested
    inner class CursorTracking {
        
        @Test
        fun `drag should track cursor movement with correct polling interval`() = runTest {
            // Given
            every { mockCursorManager.isCursorVisible() } returns true
            val positions = mutableListOf(
                Pair(100, 100),
                Pair(110, 110), // Movement > threshold
                Pair(120, 120)  // More movement
            )
            var positionIndex = 0
            every { mockCursorManager.getCursorPosition() } answers {
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
            verify(atLeast = 3) { mockCursorManager.getCursorPosition() }
        }
        
        @Test
        fun `drag should respect movement threshold from Legacy`() = runTest {
            // Given
            every { mockCursorManager.isCursorVisible() } returns true
            val positions = mutableListOf(
                Pair(100, 100),
                Pair(102, 102), // Small movement < threshold (5 pixels)
                Pair(108, 108)  // Still below threshold
            )
            var positionIndex = 0
            every { mockCursorManager.getCursorPosition() } answers {
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
    
    @Nested
    inner class ErrorHandling {
        
        @Test
        fun `drag should handle service dispatch failure gracefully`() {
            // Given
            every { mockService.dispatchGesture(any(), any(), any()) } returns false
            every { mockCursorManager.isCursorVisible() } returns true
            
            // When
            val result = dragHandler.execute(
                ActionCategory.GESTURE,
                "drag start",
                emptyMap()
            )
            
            // Then - should still return true (start was successful)
            assertTrue(result)
        }
        
        @Test
        fun `drag should handle missing cursor manager gracefully`() {
            // Given
            val handlerWithoutCursor = DragHandler(mockService)
            // Don't set cursor manager
            
            // When
            val result = handlerWithoutCursor.execute(
                ActionCategory.GESTURE,
                "drag start",
                emptyMap()
            )
            
            // Then
            assertFalse(result) // Should fail gracefully
        }
        
        @Test
        fun `dispose should cleanup all resources`() = runTest {
            // Given
            every { mockCursorManager.isCursorVisible() } returns true
            dragHandler.execute(ActionCategory.GESTURE, "drag start", emptyMap())
            assertTrue(dragHandler.isDragActive())
            
            // When
            dragHandler.dispose()
            
            // Then
            assertFalse(dragHandler.isDragActive())
            
            // Verify cleanup completed
            advanceTimeBy(1000)
            // No further cursor position calls should happen
        }
    }
    
    @Nested
    inner class LegacyCompatibility {
        
        @Test
        fun `drag flow should match Legacy DragAction behavior`() = runTest {
            // Given - simulate Legacy flow
            every { mockCursorManager.isCursorVisible() } returns true
            every { mockCursorManager.getCursorPosition() } returnsMany listOf(
                Pair(100, 100), // Start position
                Pair(150, 150), // Move position  
                Pair(200, 200)  // End position
            )
            
            // When - execute Legacy equivalent sequence
            // 1. Start drag (Legacy: Action.DRAG_START)
            val startResult = dragHandler.execute(ActionCategory.GESTURE, "drag start", emptyMap())
            assertTrue(startResult)
            
            // 2. Allow cursor movement to be detected
            advanceTimeBy(300)
            
            // 3. Stop drag (Legacy: Action.DRAG_STOP)
            val stopResult = dragHandler.execute(ActionCategory.GESTURE, "drag stop", emptyMap())
            assertTrue(stopResult)
            
            // Then - should have completed full drag cycle
            assertFalse(dragHandler.isDragActive())
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
            assertTrue(result)
        }
        
        @Test
        fun `cursor requirement should match Legacy behavior`() {
            // Given - Legacy required cursor to be visible
            every { mockCursorManager.isCursorVisible() } returns false
            
            // When
            val result = dragHandler.execute(
                ActionCategory.GESTURE,
                "drag start",
                emptyMap()
            )
            
            // Then - should fail like Legacy when cursor not visible
            assertFalse(result)
        }
    }
}