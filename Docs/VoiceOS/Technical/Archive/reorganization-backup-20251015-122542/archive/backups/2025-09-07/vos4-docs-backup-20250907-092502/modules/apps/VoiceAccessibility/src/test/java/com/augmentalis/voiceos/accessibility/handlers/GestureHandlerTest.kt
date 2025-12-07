/**
 * GestureHandlerTest.kt - Test suite for GestureHandler
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-09-03
 * 
 * Comprehensive test coverage for all gesture functionality
 */
package com.augmentalis.voiceos.accessibility.handlers

import android.graphics.Point
import com.augmentalis.voiceos.accessibility.handlers.ActionCategory
import com.augmentalis.voiceos.accessibility.mocks.MockVoiceAccessibilityService
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Test suite for GestureHandler functionality
 */
class GestureHandlerTest {
    
    private lateinit var mockService: MockVoiceAccessibilityService
    private lateinit var gestureHandler: GestureHandler
    
    @BeforeEach
    fun setUp() {
        mockService = MockVoiceAccessibilityService()
        gestureHandler = GestureHandler(mockService)
    }
    
    @AfterEach
    fun tearDown() {
        gestureHandler.dispose()
        // Clear any mock state if needed
    }
    
    // Basic functionality tests
    
    @Test
    fun testCanHandlePinchGestures() {
        assertTrue(gestureHandler.canHandle("pinch open"))
        assertTrue(gestureHandler.canHandle("pinch close"))
        assertTrue(gestureHandler.canHandle("zoom in"))
        assertTrue(gestureHandler.canHandle("zoom out"))
        assertTrue(gestureHandler.canHandle("pinch in"))
        assertTrue(gestureHandler.canHandle("pinch out"))
    }
    
    @Test
    fun testCanHandleDragGestures() {
        assertTrue(gestureHandler.canHandle("drag"))
        assertTrue(gestureHandler.canHandle("drag to"))
        assertTrue(gestureHandler.canHandle("drag from"))
    }
    
    @Test
    fun testCanHandleSwipeGestures() {
        assertTrue(gestureHandler.canHandle("swipe"))
        assertTrue(gestureHandler.canHandle("swipe up"))
        assertTrue(gestureHandler.canHandle("swipe down"))
        assertTrue(gestureHandler.canHandle("swipe left"))
        assertTrue(gestureHandler.canHandle("swipe right"))
    }
    
    @Test
    fun testCanHandlePathGestures() {
        assertTrue(gestureHandler.canHandle("gesture"))
        assertTrue(gestureHandler.canHandle("path gesture"))
    }
    
    @Test
    fun testCannotHandleInvalidActions() {
        assertFalse(gestureHandler.canHandle("invalid action"))
        assertFalse(gestureHandler.canHandle("click"))
        assertFalse(gestureHandler.canHandle("type text"))
    }
    
    // Pinch gesture tests
    
    @Test
    fun testPinchOpenGesture() {
        val params = mapOf("x" to 100, "y" to 200)
        val result = gestureHandler.execute(
            ActionCategory.GESTURE, 
            "pinch open", 
            params
        )
        
        assertTrue(result, "Pinch open should succeed")
        
        // Verify gesture was dispatched
        val gestures = mockService.getPerformedGestures()
        assertEquals(1, gestures.size)
        
        val gesture = gestures[0]
        assertEquals("PINCH", gesture.gestureType)
        assertNotNull(gesture.coordinates)
    }
    
    @Test
    fun testPinchCloseGesture() {
        val params = mapOf("x" to 300, "y" to 400)
        val result = gestureHandler.execute(
            ActionCategory.GESTURE, 
            "pinch close", 
            params
        )
        
        assertTrue(result, "Pinch close should succeed")
        
        val gestures = mockService.getPerformedGestures()
        assertEquals(1, gestures.size)
        
        val gesture = gestures[0]
        assertEquals("PINCH", gesture.gestureType)
    }
    
    @Test
    fun testZoomInGesture() {
        val result = gestureHandler.execute(
            ActionCategory.GESTURE, 
            "zoom in", 
            emptyMap()
        )
        
        assertTrue(result, "Zoom in should succeed")
        assertEquals(1, mockService.getPerformedGestures().size)
    }
    
    @Test
    fun testZoomOutGesture() {
        val result = gestureHandler.execute(
            ActionCategory.GESTURE, 
            "zoom out", 
            emptyMap()
        )
        
        assertTrue(result, "Zoom out should succeed")
        assertEquals(1, mockService.getPerformedGestures().size)
    }
    
    // Drag gesture tests
    
    @Test
    fun testDragGesture() {
        val params = mapOf(
            "startX" to 100,
            "startY" to 200,
            "endX" to 300,
            "endY" to 400,
            "duration" to 500L
        )
        
        val result = gestureHandler.execute(
            ActionCategory.GESTURE, 
            "drag", 
            params
        )
        
        assertTrue(result, "Drag should succeed")
        
        val gestures = mockService.getPerformedGestures()
        assertEquals(1, gestures.size)
        
        val gesture = gestures[0]
        assertEquals("DRAG", gesture.gestureType)
        assertEquals(500L, gesture.duration)
    }
    
    @Test
    fun testDragGestureMissingParameters() {
        val params = mapOf(
            "startX" to 100,
            "startY" to 200
            // Missing endX, endY
        )
        
        val result = gestureHandler.execute(
            ActionCategory.GESTURE, 
            "drag", 
            params
        )
        
        assertFalse(result, "Drag should fail without required parameters")
        assertEquals(0, mockService.getPerformedGestures().size)
    }
    
    // Swipe gesture tests
    
    @Test
    fun testSwipeUpGesture() {
        val params = mapOf(
            "x" to 200,
            "y" to 300,
            "distance" to 400
        )
        
        val result = gestureHandler.execute(
            ActionCategory.GESTURE, 
            "swipe up", 
            params
        )
        
        assertTrue(result, "Swipe up should succeed")
        
        val gestures = mockService.getPerformedGestures()
        assertEquals(1, gestures.size)
        assertEquals("SWIPE", gestures[0].gestureType)
    }
    
    @Test
    fun testSwipeDownGesture() {
        val result = gestureHandler.execute(
            ActionCategory.GESTURE, 
            "swipe down", 
            emptyMap()
        )
        
        assertTrue(result, "Swipe down should succeed")
        assertEquals(1, mockService.getPerformedGestures().size)
    }
    
    @Test
    fun testSwipeLeftGesture() {
        val result = gestureHandler.execute(
            ActionCategory.GESTURE, 
            "swipe left", 
            emptyMap()
        )
        
        assertTrue(result, "Swipe left should succeed")
        assertEquals(1, mockService.getPerformedGestures().size)
    }
    
    @Test
    fun testSwipeRightGesture() {
        val result = gestureHandler.execute(
            ActionCategory.GESTURE, 
            "swipe right", 
            emptyMap()
        )
        
        assertTrue(result, "Swipe right should succeed")
        assertEquals(1, mockService.getPerformedGestures().size)
    }
    
    @Test
    fun testSwipeDefaultDirection() {
        val result = gestureHandler.execute(
            ActionCategory.GESTURE, 
            "swipe", 
            emptyMap()
        )
        
        assertTrue(result, "Default swipe should succeed")
        assertEquals(1, mockService.getPerformedGestures().size)
    }
    
    @Test
    fun testSwipeInvalidDirection() {
        val result = gestureHandler.execute(
            ActionCategory.GESTURE, 
            "swipe diagonal", 
            emptyMap()
        )
        
        assertFalse(result, "Invalid swipe direction should fail")
        assertEquals(0, mockService.getPerformedGestures().size)
    }
    
    // Path gesture tests
    
    @Test
    fun testPathGesture() {
        val pathPoints = listOf(
            Point(100, 100),
            Point(200, 150),
            Point(300, 200),
            Point(400, 250)
        )
        
        val params = mapOf(
            "path" to pathPoints,
            "duration" to 800L
        )
        
        val result = gestureHandler.execute(
            ActionCategory.GESTURE, 
            "gesture", 
            params
        )
        
        assertTrue(result, "Path gesture should succeed")
        
        val gestures = mockService.getPerformedGestures()
        assertEquals(1, gestures.size)
        assertEquals("PATH", gestures[0].gestureType)
    }
    
    @Test
    fun testPathGestureSinglePoint() {
        val pathPoints = listOf(Point(150, 150))
        
        val params = mapOf("path" to pathPoints)
        
        val result = gestureHandler.execute(
            ActionCategory.GESTURE, 
            "path gesture", 
            params
        )
        
        assertTrue(result, "Single point path should succeed")
        assertEquals(1, mockService.getPerformedGestures().size)
    }
    
    @Test
    fun testPathGestureEmptyPath() {
        val params = mapOf("path" to emptyList<Point>())
        
        val result = gestureHandler.execute(
            ActionCategory.GESTURE, 
            "gesture", 
            params
        )
        
        assertFalse(result, "Empty path should fail")
        assertEquals(0, mockService.getPerformedGestures().size)
    }
    
    @Test
    fun testPathGestureMissingPath() {
        val result = gestureHandler.execute(
            ActionCategory.GESTURE, 
            "path gesture", 
            emptyMap()
        )
        
        assertFalse(result, "Missing path parameter should fail")
        assertEquals(0, mockService.getPerformedGestures().size)
    }
    
    // Coordinate-based click tests
    
    @Test
    fun testPerformClickAt() {
        val result = gestureHandler.performClickAt(150f, 250f)
        
        assertTrue(result, "Click at coordinates should succeed")
        
        val gestures = mockService.getPerformedGestures()
        assertEquals(1, gestures.size)
        assertEquals("TAP", gestures[0].gestureType)
    }
    
    @Test
    fun testPerformLongPressAt() {
        val result = gestureHandler.performLongPressAt(200f, 300f)
        
        assertTrue(result, "Long press at coordinates should succeed")
        
        val gestures = mockService.getPerformedGestures()
        assertEquals(1, gestures.size)
        assertEquals("LONG_PRESS", gestures[0].gestureType)
    }
    
    @Test
    fun testPerformDoubleClickAt() = runBlocking {
        val result = gestureHandler.performDoubleClickAt(250f, 350f)
        
        assertTrue(result, "Double click at coordinates should succeed")
        
        // Wait for async double click to complete
        delay(200)
        
        val gestures = mockService.getPerformedGestures()
        assertEquals(2, gestures.size) // Two taps for double click
        assertTrue(gestures.all { it.gestureType == "TAP" })
    }
    
    // Queue management tests
    
    @Test
    fun testMultipleGesturesQueued() = runBlocking {
        // Execute multiple pinch gestures rapidly
        val params1 = mapOf("x" to 100, "y" to 100)
        val params2 = mapOf("x" to 200, "y" to 200)
        val params3 = mapOf("x" to 300, "y" to 300)
        
        gestureHandler.execute(ActionCategory.GESTURE, "pinch open", params1)
        gestureHandler.execute(ActionCategory.GESTURE, "pinch close", params2)
        gestureHandler.execute(ActionCategory.GESTURE, "pinch open", params3)
        
        // Allow some time for gesture processing
        delay(1500) // 3 gestures * 400ms each + buffer
        
        val gestures = mockService.getPerformedGestures()
        assertEquals(3, gestures.size)
        assertTrue(gestures.all { it.gestureType == "PINCH" })
    }
    
    // Error handling tests
    
    @Test
    fun testInvalidActionHandling() {
        val result = gestureHandler.execute(
            ActionCategory.GESTURE, 
            "invalid_gesture", 
            emptyMap()
        )
        
        assertFalse(result, "Invalid action should fail")
        assertEquals(0, mockService.getPerformedGestures().size)
    }
    
    // Supported actions tests
    
    @Test
    fun testGetSupportedActions() {
        val supportedActions = gestureHandler.getSupportedActions()
        
        assertFalse(supportedActions.isEmpty(), "Should have supported actions")
        assertTrue(supportedActions.any { it.contains("pinch open") }, 
            "Should support pinch open")
        assertTrue(supportedActions.any { it.contains("pinch close") }, 
            "Should support pinch close")
        assertTrue(supportedActions.any { it.contains("drag") }, 
            "Should support drag")
        assertTrue(supportedActions.any { it.contains("swipe") }, 
            "Should support swipe")
        assertTrue(supportedActions.any { it.contains("gesture") }, 
            "Should support gesture")
    }
    
    // Integration tests
    
    @Test
    fun testGestureHandlerIntegration() {
        // Test that the handler properly integrates with ActionCategory.GESTURE
        val category = ActionCategory.GESTURE
        
        val result = gestureHandler.execute(
            category, 
            "zoom in", 
            emptyMap()
        )
        
        assertTrue(result, "Integration should work")
        assertEquals(1, mockService.getPerformedGestures().size)
    }
}

/**
 * Test scenarios for comprehensive gesture testing
 */
object GestureTestScenarios {
    
    /**
     * Basic gesture test scenarios
     */
    val basicGestureScenarios = listOf(
        TestScenario(
            name = "Pinch to zoom in",
            action = "pinch open",
            params = mapOf("x" to 400, "y" to 600),
            expectedResult = true,
            expectedGestureType = "PINCH"
        ),
        TestScenario(
            name = "Pinch to zoom out",
            action = "pinch close",
            params = mapOf("x" to 400, "y" to 600),
            expectedResult = true,
            expectedGestureType = "PINCH"
        ),
        TestScenario(
            name = "Drag gesture",
            action = "drag",
            params = mapOf(
                "startX" to 100, "startY" to 200,
                "endX" to 300, "endY" to 400,
                "duration" to 600L
            ),
            expectedResult = true,
            expectedGestureType = "DRAG"
        ),
        TestScenario(
            name = "Swipe up gesture",
            action = "swipe up",
            params = mapOf("x" to 300, "y" to 500, "distance" to 200),
            expectedResult = true,
            expectedGestureType = "SWIPE"
        ),
        TestScenario(
            name = "Complex path gesture",
            action = "gesture",
            params = mapOf(
                "path" to listOf(
                    Point(100, 100),
                    Point(200, 150),
                    Point(300, 200),
                    Point(200, 250),
                    Point(100, 200)
                ),
                "duration" to 1000L
            ),
            expectedResult = true,
            expectedGestureType = "PATH"
        )
    )
    
    /**
     * Voice command integration scenarios
     */
    val voiceCommandScenarios = listOf(
        VoiceCommandScenario(
            command = "zoom in",
            expectedAction = "pinch open",
            shouldSucceed = true
        ),
        VoiceCommandScenario(
            command = "zoom out",
            expectedAction = "pinch close",
            shouldSucceed = true
        ),
        VoiceCommandScenario(
            command = "pinch open",
            expectedAction = "pinch open",
            shouldSucceed = true
        ),
        VoiceCommandScenario(
            command = "swipe left",
            expectedAction = "swipe left",
            shouldSucceed = true
        ),
        VoiceCommandScenario(
            command = "swipe right",
            expectedAction = "swipe right",
            shouldSucceed = true
        )
    )
    
    /**
     * Performance test scenarios
     */
    val performanceScenarios = listOf(
        PerformanceScenario(
            name = "Rapid pinch gestures",
            gestureCount = 10,
            action = "pinch open",
            maxExpectedTimeMs = 5000L
        ),
        PerformanceScenario(
            name = "Sequential swipes",
            gestureCount = 5,
            action = "swipe up",
            maxExpectedTimeMs = 2000L
        ),
        PerformanceScenario(
            name = "Mixed gesture types",
            gestureCount = 8,
            action = "mixed",
            maxExpectedTimeMs = 4000L
        )
    )
    
    data class TestScenario(
        val name: String,
        val action: String,
        val params: Map<String, Any>,
        val expectedResult: Boolean,
        val expectedGestureType: String
    )
    
    data class VoiceCommandScenario(
        val command: String,
        val expectedAction: String,
        val shouldSucceed: Boolean
    )
    
    data class PerformanceScenario(
        val name: String,
        val gestureCount: Int,
        val action: String,
        val maxExpectedTimeMs: Long
    )
}