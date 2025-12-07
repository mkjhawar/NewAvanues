/**
 * HUDManagerTest.kt - Comprehensive unit tests for HUDManager
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-28
 * 
 * Tests AR HUD system, spatial tracking, and rendering performance
 */
package com.augmentalis.hudmanager

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.augmentalis.hudmanager.models.*
import com.augmentalis.hudmanager.spatial.SpatialPosition
import com.augmentalis.hudmanager.models.UIContext
import com.augmentalis.hudmanager.models.VoiceCommand
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.argThat
import kotlin.test.*

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class HUDManagerTest {
    
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var hudStateObserver: Observer<HUDState>
    
    @Mock
    private lateinit var renderingStatsObserver: Observer<RenderingStats>
    
    @Mock
    private lateinit var spatialDataObserver: Observer<SpatialData>
    
    @Mock
    private lateinit var errorObserver: Observer<String?>
    
    private lateinit var hudManager: HUDManager
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        
        `when`(mockContext.applicationContext).thenReturn(mockContext)
        
        hudManager = HUDManager(mockContext)
        
        // Setup observers
        hudManager.hudState.observeForever(hudStateObserver)
        hudManager.renderingStats.observeForever(renderingStatsObserver)
        hudManager.spatialData.observeForever(spatialDataObserver)
        hudManager.errorMessage.observeForever(errorObserver)
    }
    
    @After
    fun tearDown() {
        hudManager.hudState.removeObserver(hudStateObserver)
        hudManager.renderingStats.removeObserver(renderingStatsObserver)
        hudManager.spatialData.removeObserver(spatialDataObserver)
        hudManager.errorMessage.removeObserver(errorObserver)
        Dispatchers.resetMain()
    }
    
    @Test
    fun `test initial HUD state`() {
        val state = hudManager.hudState.value
        assertNotNull(state)
        assertEquals(HUDMode.DISABLED, state.mode)
        assertFalse(state.isTracking)
        assertTrue(state.activeElements.isEmpty())
        assertEquals(1.0f, state.opacity)
    }
    
    @Test
    fun `test initialize HUD system`() = runTest {
        val config = HUDConfig(
            mode = HUDMode.STANDARD,
            targetFPS = 90,
            maxElements = 50,
            trackingEnabled = true
        )
        
        val result = hudManager.initialize(config)
        
        assertTrue(result)
        verify(hudStateObserver).onChanged(argThat<HUDState> { state -> state.mode == HUDMode.STANDARD })
    }
    
    @Test
    fun `test enable HUD tracking`() = runTest {
        hudManager.initialize(HUDConfig.default())
        
        val result = hudManager.enableTracking()
        
        assertTrue(result)
        verify(hudStateObserver).onChanged(argThat<HUDState> { state -> state.isTracking })
    }
    
    @Test
    fun `test disable HUD tracking`() = runTest {
        hudManager.initialize(HUDConfig.default())
        hudManager.enableTracking()
        
        val result = hudManager.disableTracking()
        
        assertTrue(result)
        verify(hudStateObserver).onChanged(argThat<HUDState> { state -> !state.isTracking })
    }
    
    @Test
    fun `test add HUD element`() = runTest {
        hudManager.initialize(HUDConfig.default())
        
        val element = HUDElement(
            id = "test-element-1",
            type = HUDElementType.TEXT_OVERLAY,
            position = Vector3D(0.0f, 0.0f, 1.0f),
            content = "Test Content",
            priority = 1
        )
        
        val result = hudManager.addElement(element)
        
        assertTrue(result)
        verify(hudStateObserver).onChanged(argThat<HUDState> { state -> state.activeElements.contains(element) })
    }
    
    @Test
    fun `test remove HUD element`() = runTest {
        hudManager.initialize(HUDConfig.default())
        
        val element = HUDElement(
            id = "test-element-2",
            type = HUDElementType.ICON,
            position = Vector3D(1.0f, 1.0f, 1.0f),
            content = "Icon",
            priority = 2
        )
        
        hudManager.addElement(element)
        val result = hudManager.removeElement(element.id)
        
        assertTrue(result)
        verify(hudStateObserver).onChanged(argThat<HUDState> { state -> !state.activeElements.any { el -> el.id == element.id } })
    }
    
    @Test
    fun `test update element position`() = runTest {
        hudManager.initialize(HUDConfig.default())
        
        val element = HUDElement(
            id = "movable-element",
            type = HUDElementType.BUTTON,
            position = Vector3D(0.0f, 0.0f, 1.0f),
            content = "Movable Button",
            priority = 1
        )
        
        hudManager.addElement(element)
        
        val newPosition = Vector3D(2.0f, 2.0f, 1.5f)
        val result = hudManager.updateElementPosition(element.id, newPosition)
        
        assertTrue(result)
        verify(hudStateObserver).onChanged(
            argThat<HUDState> { state -> 
                state.activeElements.find { el -> el.id == element.id }?.position == newPosition 
            }
        )
    }
    
    @Test
    fun `test spatial calibration`() = runTest {
        hudManager.initialize(HUDConfig.default())
        hudManager.enableTracking()
        
        val calibrationPoints = listOf(
            CalibrationPoint(Vector3D(0.0f, 0.0f, 1.0f), "center"),
            CalibrationPoint(Vector3D(-1.0f, 0.0f, 1.0f), "left"),
            CalibrationPoint(Vector3D(1.0f, 0.0f, 1.0f), "right"),
            CalibrationPoint(Vector3D(0.0f, 1.0f, 1.0f), "top"),
            CalibrationPoint(Vector3D(0.0f, -1.0f, 1.0f), "bottom")
        )
        
        val result = hudManager.calibrateSpatialMapping(calibrationPoints)
        
        assertTrue(result)
        verify(spatialDataObserver).onChanged(argThat<SpatialData> { data -> data.isCalibrated })
    }
    
    @Test
    fun `test rendering performance tracking`() = runTest {
        hudManager.initialize(HUDConfig.default().copy(targetFPS = 60))
        hudManager.enableTracking()
        
        // Add multiple elements to test performance
        repeat(10) { index ->
            val element = HUDElement(
                id = "perf-test-$index",
                type = HUDElementType.TEXT_OVERLAY,
                position = Vector3D(index * 0.1f, 0.0f, 1.0f),
                content = "Element $index",
                priority = index
            )
            hudManager.addElement(element)
        }
        
        // Simulate rendering cycles
        repeat(60) {
            hudManager.onRenderFrame()
            advanceTimeBy(16L) // ~60 FPS
        }
        
        val stats = hudManager.renderingStats.value
        assertNotNull(stats)
        assertTrue(stats.averageFPS > 0)
        assertTrue(stats.frameTime > 0)
        assertTrue(stats.renderedElements >= 10)
    }
    
    @Test
    fun `test FPS optimization`() = runTest {
        val highPerformanceConfig = HUDConfig.default().copy(
            targetFPS = 120,
            maxElements = 20,
            optimizationEnabled = true
        )
        
        hudManager.initialize(highPerformanceConfig)
        
        // Add elements beyond optimization threshold
        repeat(25) { index ->
            val element = HUDElement(
                id = "opt-test-$index",
                type = HUDElementType.TEXT_OVERLAY,
                position = Vector3D(index * 0.1f, 0.0f, 1.0f),
                content = "Element $index",
                priority = index
            )
            hudManager.addElement(element)
        }
        
        val state = hudManager.hudState.value
        assertNotNull(state)
        assertTrue(state.activeElements.size <= 20, "Should limit elements for optimization")
        
        // Verify high-priority elements are retained
        val highPriorityElement = state.activeElements.find { it.id == "opt-test-24" }
        assertNotNull(highPriorityElement, "Highest priority element should be retained")
    }
    
    @Test
    fun `test element collision detection`() = runTest {
        hudManager.initialize(HUDConfig.default())
        
        val element1 = HUDElement(
            id = "collision-test-1",
            type = HUDElementType.BUTTON,
            position = Vector3D(0.0f, 0.0f, 1.0f),
            content = "Button 1",
            priority = 1,
            bounds = ElementBounds(0.2f, 0.1f)
        )
        
        val element2 = HUDElement(
            id = "collision-test-2",
            type = HUDElementType.BUTTON,
            position = Vector3D(0.1f, 0.05f, 1.0f), // Overlapping position
            content = "Button 2",
            priority = 2,
            bounds = ElementBounds(0.2f, 0.1f)
        )
        
        hudManager.addElement(element1)
        val collisionResult = hudManager.addElement(element2)
        
        assertTrue(collisionResult)
        
        // Check if collision was detected and handled
        val collisions = hudManager.detectCollisions()
        assertTrue(collisions.isNotEmpty(), "Should detect collision between overlapping elements")
        
        val collision = collisions.first()
        assertTrue(
            (collision.element1.id == element1.id && collision.element2.id == element2.id) ||
            (collision.element1.id == element2.id && collision.element2.id == element1.id)
        )
    }
    
    @Test
    fun `test depth sorting`() = runTest {
        hudManager.initialize(HUDConfig.default())
        
        val elements = listOf(
            HUDElement(
                id = "far",
                type = HUDElementType.TEXT_OVERLAY,
                position = Vector3D(0.0f, 0.0f, 5.0f), // Far
                content = "Far Element",
                priority = 1
            ),
            HUDElement(
                id = "near",
                type = HUDElementType.TEXT_OVERLAY,
                position = Vector3D(0.0f, 0.0f, 1.0f), // Near
                content = "Near Element",
                priority = 2
            ),
            HUDElement(
                id = "middle",
                type = HUDElementType.TEXT_OVERLAY,
                position = Vector3D(0.0f, 0.0f, 3.0f), // Middle
                content = "Middle Element",
                priority = 3
            )
        )
        
        elements.forEach { hudManager.addElement(it) }
        
        val sortedElements = hudManager.getElementsSortedByDepth()
        assertEquals(3, sortedElements.size)
        
        // Should be sorted near to far
        assertEquals("near", sortedElements[0].id)
        assertEquals("middle", sortedElements[1].id)
        assertEquals("far", sortedElements[2].id)
    }
    
    @Test
    fun `test HUD mode switching`() = runTest {
        hudManager.initialize(HUDConfig.default().copy(mode = HUDMode.STANDARD))
        
        // Switch to different mode
        val result = hudManager.switchMode(HUDMode.MEETING)
        
        assertTrue(result)
        verify(hudStateObserver).onChanged(argThat<HUDState> { state -> state.mode == HUDMode.MEETING })
    }
    
    @Test
    fun `test opacity adjustment`() = runTest {
        hudManager.initialize(HUDConfig.default())
        
        val newOpacity = 0.7f
        hudManager.setOpacity(newOpacity)
        
        verify(hudStateObserver).onChanged(argThat<HUDState> { state -> state.opacity == newOpacity })
    }
    
    @Test
    fun `test error handling for invalid elements`() = runTest {
        hudManager.initialize(HUDConfig.default())
        
        val invalidElement = HUDElement(
            id = "", // Invalid empty ID
            type = HUDElementType.TEXT_OVERLAY,
            position = Vector3D(0.0f, 0.0f, 1.0f),
            content = "Invalid Element",
            priority = 1
        )
        
        val result = hudManager.addElement(invalidElement)
        
        assertFalse(result)
        verify(errorObserver).onChanged(argThat<String?> { it?.contains("Invalid element") == true })
    }
    
    @Test
    fun `test cleanup and resource management`() = runTest {
        hudManager.initialize(HUDConfig.default())
        hudManager.enableTracking()
        
        // Add some elements
        repeat(5) { index ->
            val element = HUDElement(
                id = "cleanup-test-$index",
                type = HUDElementType.TEXT_OVERLAY,
                position = Vector3D(index * 0.2f, 0.0f, 1.0f),
                content = "Element $index",
                priority = index
            )
            hudManager.addElement(element)
        }
        
        hudManager.cleanup()
        
        val state = hudManager.hudState.value
        assertNotNull(state)
        assertEquals(HUDMode.DISABLED, state.mode)
        assertFalse(state.isTracking)
        assertTrue(state.activeElements.isEmpty())
    }
    
    @Test
    fun `test multi-language content support`() = runTest {
        hudManager.initialize(HUDConfig.default())
        
        val multiLanguageElements = listOf(
            HUDElement(
                id = "english",
                type = HUDElementType.TEXT_OVERLAY,
                position = Vector3D(0.0f, 0.0f, 1.0f),
                content = "Hello World",
                priority = 1
            ),
            HUDElement(
                id = "spanish",
                type = HUDElementType.TEXT_OVERLAY,
                position = Vector3D(0.0f, 0.5f, 1.0f),
                content = "Hola Mundo",
                priority = 2
            ),
            HUDElement(
                id = "chinese",
                type = HUDElementType.TEXT_OVERLAY,
                position = Vector3D(0.0f, 1.0f, 1.0f),
                content = "你好世界",
                priority = 3
            )
        )
        
        multiLanguageElements.forEach { element ->
            val result = hudManager.addElement(element)
            assertTrue(result, "Should handle multi-language content: ${element.content}")
        }
        
        val state = hudManager.hudState.value
        assertEquals(3, state?.activeElements?.size)
    }
}