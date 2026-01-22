/**
 * GestureHandlerTest.kt - Test suite for GestureHandler
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-09-03
 */
package com.augmentalis.voiceoscore.accessibility.handlers

import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Point
import android.os.Handler
import android.util.DisplayMetrics
import com.augmentalis.voiceoscore.accessibility.VoiceOSService
import com.augmentalis.voiceoscore.accessibility.handlers.ActionCategory
import com.augmentalis.voiceoscore.accessibility.ui.utils.DisplayUtils
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Test suite for GestureHandler functionality
 * Pure unit tests using mockk - no Robolectric needed
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GestureHandlerTest {

    private lateinit var mockService: VoiceOSService
    private lateinit var mockPathFactory: GesturePathFactory
    private lateinit var gestureHandler: GestureHandler
    private val capturedGestures = mutableListOf<GestureDescription>()
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        mockService = mockk(relaxed = true)
        mockPathFactory = mockk(relaxed = true)
        capturedGestures.clear()

        // Mock DisplayUtils to avoid Android framework dependencies
        mockkObject(DisplayUtils)
        every { DisplayUtils.getRealScreenSize(any<Context>()) } answers { Point(1080, 1920) }
        every { DisplayUtils.getRealDisplayMetrics(any<Context>()) } answers {
            DisplayMetrics().apply {
                widthPixels = 1080
                heightPixels = 1920
                density = 2.0f
            }
        }

        // Mock GestureResultCallback to avoid Android framework instantiation issues
        mockkConstructor(GestureResultCallback::class)
        every { anyConstructed<GestureResultCallback>().onCompleted(any()) } just Runs
        every { anyConstructed<GestureResultCallback>().onCancelled(any()) } just Runs

        // Mock Path creation - return mocked Path that supports moveTo/lineTo
        every { mockPathFactory.createPath() } answers {
            mockk<android.graphics.Path>(relaxed = true) {
                every { moveTo(any(), any()) } just Runs
                every { lineTo(any(), any()) } just Runs
            }
        }

        // Mock StrokeDescription creation
        every { mockPathFactory.createStroke(any(), any(), any(), any()) } returns mockk(relaxed = true)

        // Mock GestureDescription creation - return mock
        every { mockPathFactory.createGesture(any()) } answers {
            mockk<GestureDescription>(relaxed = true)
        }

        // Capture all dispatched gestures AND invoke the callback to simulate success
        every { mockService.dispatchGesture(any(), any(), any()) } answers {
            val gesture = firstArg<GestureDescription>()
            val callback = secondArg<GestureResultCallback?>()

            // Track all dispatched gestures
            capturedGestures.add(gesture)

            // Simulate successful gesture completion by invoking callback
            // Wrap in try-catch to handle potential Android SDK stub exceptions from super calls
            try {
                callback?.onCompleted(gesture)
            } catch (_: Exception) {
                // Ignore exceptions from Android SDK stubs - the gesture was still dispatched
            }

            true  // Return success
        }

        // Create GestureHandler with test dispatcher's scope
        val testScope = CoroutineScope(testDispatcher)
        gestureHandler = GestureHandler(mockService, mockPathFactory, testScope)
    }

    @After
    fun tearDown() {
        gestureHandler.dispose()
        unmockkObject(DisplayUtils)
        unmockkConstructor(GestureResultCallback::class)
        clearAllMocks()
        Dispatchers.resetMain()
    }
    
    // Basic functionality tests
    
    @Test
    fun testCanHandlePinchGestures() {
        assertTrue("Should handle pinch open", gestureHandler.canHandle("pinch open"))
        assertTrue("Should handle pinch close", gestureHandler.canHandle("pinch close"))
        assertTrue("Should handle zoom in", gestureHandler.canHandle("zoom in"))
        assertTrue("Should handle zoom out", gestureHandler.canHandle("zoom out"))
        assertTrue("Should handle pinch in", gestureHandler.canHandle("pinch in"))
        assertTrue("Should handle pinch out", gestureHandler.canHandle("pinch out"))
    }

    @Test
    fun testCanHandleDragGestures() {
        assertTrue("Should handle drag", gestureHandler.canHandle("drag"))
        assertTrue("Should handle drag to", gestureHandler.canHandle("drag to"))
        assertTrue("Should handle drag from", gestureHandler.canHandle("drag from"))
    }

    @Test
    fun testCanHandleSwipeGestures() {
        assertTrue("Should handle swipe", gestureHandler.canHandle("swipe"))
        assertTrue("Should handle swipe up", gestureHandler.canHandle("swipe up"))
        assertTrue("Should handle swipe down", gestureHandler.canHandle("swipe down"))
        assertTrue("Should handle swipe left", gestureHandler.canHandle("swipe left"))
        assertTrue("Should handle swipe right", gestureHandler.canHandle("swipe right"))
    }

    @Test
    fun testCanHandlePathGestures() {
        assertTrue("Should handle gesture", gestureHandler.canHandle("gesture"))
        assertTrue("Should handle path gesture", gestureHandler.canHandle("path gesture"))
    }

    @Test
    fun testCannotHandleInvalidActions() {
        assertFalse("Should not handle invalid action", gestureHandler.canHandle("invalid action"))
        assertFalse("Should not handle click", gestureHandler.canHandle("click"))
        assertFalse("Should not handle type text", gestureHandler.canHandle("type text"))
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

        // Advance dispatcher to process queued gestures
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("Pinch open should succeed", result)

        // Verify gesture was dispatched
        assertEquals("One gesture should be dispatched", 1, capturedGestures.size)
        verify(exactly = 1) { mockService.dispatchGesture(any(), any(), any()) }
    }
    
    @Test
    fun testPinchCloseGesture() {
        val params = mapOf("x" to 300, "y" to 400)
        val result = gestureHandler.execute(
            ActionCategory.GESTURE,
            "pinch close",
            params
        )

        // Advance dispatcher to process queued gestures
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("Pinch close should succeed", result)

        assertEquals("One gesture should be dispatched", 1, capturedGestures.size)
        verify(exactly = 1) { mockService.dispatchGesture(any(), any(), any()) }
    }
    
    @Test
    fun testZoomInGesture() {
        val result = gestureHandler.execute(
            ActionCategory.GESTURE,
            "zoom in",
            emptyMap()
        )

        // Advance dispatcher to execute launched coroutines
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("Zoom in should succeed", result)
        assertEquals("One gesture should be dispatched", 1, capturedGestures.size)
    }
    
    @Test
    fun testZoomOutGesture() {
        val result = gestureHandler.execute(
            ActionCategory.GESTURE,
            "zoom out",
            emptyMap()
        )

        // Advance dispatcher to process queued gestures
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("Zoom out should succeed", result)
        assertEquals("One gesture should be dispatched", 1, capturedGestures.size)
    }
    
    // Drag gesture tests
    
    @Test
    fun testDragGesture() {
        // Explicitly create params without duration to avoid type inference issues with mixed Int/Long
        // The implementation uses a default of 500L for duration when not provided
        val params = mapOf(
            "startX" to 100,
            "startY" to 200,
            "endX" to 300,
            "endY" to 400
        )

        val result = gestureHandler.execute(
            ActionCategory.GESTURE,
            "drag",
            params
        )

        // Advance dispatcher to process gesture dispatch
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("Drag should succeed", result)

        assertEquals("One gesture should be dispatched", 1, capturedGestures.size)
        verify(exactly = 1) { mockService.dispatchGesture(any(), any(), any()) }
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
        
        assertFalse("Drag should fail without required parameters", result)
        assertEquals("No gestures should be dispatched", 0, capturedGestures.size)
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
        
        assertTrue("Swipe up should succeed", result)
        
        val gestures = capturedGestures
        assertEquals(1, gestures.size)
        // Note: gestureType not available on GestureDescription - verified by dispatch success
    }
    
    @Test
    fun testSwipeDownGesture() {
        val result = gestureHandler.execute(
            ActionCategory.GESTURE, 
            "swipe down", 
            emptyMap()
        )
        
        assertTrue("Swipe down should succeed", result)
        assertEquals("One gesture should be dispatched", 1, capturedGestures.size)
    }
    
    @Test
    fun testSwipeLeftGesture() {
        val result = gestureHandler.execute(
            ActionCategory.GESTURE, 
            "swipe left", 
            emptyMap()
        )
        
        assertTrue("Swipe left should succeed", result)
        assertEquals("One gesture should be dispatched", 1, capturedGestures.size)
    }
    
    @Test
    fun testSwipeRightGesture() {
        val result = gestureHandler.execute(
            ActionCategory.GESTURE, 
            "swipe right", 
            emptyMap()
        )
        
        assertTrue("Swipe right should succeed", result)
        assertEquals("One gesture should be dispatched", 1, capturedGestures.size)
    }
    
    @Test
    fun testSwipeDefaultDirection() {
        val result = gestureHandler.execute(
            ActionCategory.GESTURE, 
            "swipe", 
            emptyMap()
        )
        
        assertTrue("Default swipe should succeed", result)
        assertEquals("One gesture should be dispatched", 1, capturedGestures.size)
    }
    
    @Test
    fun testSwipeInvalidDirection() {
        val result = gestureHandler.execute(
            ActionCategory.GESTURE, 
            "swipe diagonal", 
            emptyMap()
        )
        
        assertFalse("Invalid swipe direction should fail", result)
        assertEquals("No gestures should be dispatched", 0, capturedGestures.size)
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
        
        assertTrue("Path gesture should succeed", result)
        
        val gestures = capturedGestures
        assertEquals(1, gestures.size)
        // Note: gestureType not available on GestureDescription - verified by dispatch success
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
        
        assertTrue("Single point path should succeed", result)
        assertEquals("One gesture should be dispatched", 1, capturedGestures.size)
    }
    
    @Test
    fun testPathGestureEmptyPath() {
        val params = mapOf("path" to emptyList<Point>())
        
        val result = gestureHandler.execute(
            ActionCategory.GESTURE, 
            "gesture", 
            params
        )
        
        assertFalse("Empty path should fail", result)
        assertEquals("No gestures should be dispatched", 0, capturedGestures.size)
    }
    
    @Test
    fun testPathGestureMissingPath() {
        val result = gestureHandler.execute(
            ActionCategory.GESTURE, 
            "path gesture", 
            emptyMap()
        )
        
        assertFalse("Missing path parameter should fail", result)
        assertEquals("No gestures should be dispatched", 0, capturedGestures.size)
    }
    
    // Coordinate-based click tests
    
    @Test
    fun testPerformClickAt() {
        val result = gestureHandler.performClickAt(150f, 250f)
        
        assertTrue("Click at coordinates should succeed", result)
        
        val gestures = capturedGestures
        assertEquals(1, gestures.size)
        // Note: gestureType not available on GestureDescription - verified by dispatch success
    }
    
    @Test
    fun testPerformLongPressAt() {
        val result = gestureHandler.performLongPressAt(200f, 300f)
        
        assertTrue("Long press at coordinates should succeed", result)
        
        val gestures = capturedGestures
        assertEquals(1, gestures.size)
        // Note: gestureType not available on GestureDescription - verified by dispatch success
    }
    
    @Test
    fun testPerformDoubleClickAt() {
        val result = gestureHandler.performDoubleClickAt(250f, 350f)

        assertTrue("Double click at coordinates should succeed", result)

        // Advance dispatcher to process async double click coroutine
        testDispatcher.scheduler.advanceUntilIdle()

        val gestures = capturedGestures
        assertEquals("Two gestures should be dispatched", 2, gestures.size) // Two taps for double click
        // Note: gestureType not available on GestureDescription - verified by dispatch count
    }

    // Queue management tests

    @Test
    fun testMultipleGesturesQueued() {
        // Execute multiple pinch gestures rapidly
        val params1 = mapOf("x" to 100, "y" to 100)
        val params2 = mapOf("x" to 200, "y" to 200)
        val params3 = mapOf("x" to 300, "y" to 300)

        gestureHandler.execute(ActionCategory.GESTURE, "pinch open", params1)
        gestureHandler.execute(ActionCategory.GESTURE, "pinch close", params2)
        gestureHandler.execute(ActionCategory.GESTURE, "pinch open", params3)

        // Advance dispatcher to process all queued gestures
        testDispatcher.scheduler.advanceUntilIdle()

        val gestures = capturedGestures
        assertEquals("Three gestures should be dispatched", 3, gestures.size)
        // Note: gestureType not available on GestureDescription - verified by dispatch count
    }
    
    // Error handling tests
    
    @Test
    fun testInvalidActionHandling() {
        val result = gestureHandler.execute(
            ActionCategory.GESTURE, 
            "invalid_gesture", 
            emptyMap()
        )
        
        assertFalse("Invalid action should fail", result)
        assertEquals("No gestures should be dispatched", 0, capturedGestures.size)
    }
    
    // Supported actions tests
    
    @Test
    fun testGetSupportedActions() {
        val supportedActions = gestureHandler.getSupportedActions()
        
        assertFalse("Should have supported actions", supportedActions.isEmpty())
        assertTrue("Should support pinch open", supportedActions.any { it.contains("pinch open") })
        assertTrue("Should support pinch close", supportedActions.any { it.contains("pinch close") })
        assertTrue("Should support drag", supportedActions.any { it.contains("drag") })
        assertTrue("Should support swipe", supportedActions.any { it.contains("swipe") })
        assertTrue("Should support gesture", supportedActions.any { it.contains("gesture") })
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

        // Advance dispatcher to process queued gestures
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("Integration should work", result)
        assertEquals("One gesture should be dispatched", 1, capturedGestures.size)
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