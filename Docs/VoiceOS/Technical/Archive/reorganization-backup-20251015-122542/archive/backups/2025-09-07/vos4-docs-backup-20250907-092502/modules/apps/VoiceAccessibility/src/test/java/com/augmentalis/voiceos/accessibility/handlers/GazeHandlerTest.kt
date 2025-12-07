/**
 * GazeHandlerTest.kt - Comprehensive tests for GazeHandler
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-09-03
 * 
 * Purpose: Unit tests for GazeHandler functionality
 * Covers Legacy Avenue compatibility and new VOS4 features
 */
package com.augmentalis.voiceos.accessibility.handlers

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import com.augmentalis.voiceos.accessibility.handlers.ActionCategory
import com.augmentalis.voiceos.accessibility.service.VoiceAccessibilityService
import com.augmentalis.voiceos.accessibility.managers.CursorManager
import com.augmentalis.hudmanager.HUDManager
import com.augmentalis.hudmanager.spatial.GazeTracker
import com.augmentalis.hudmanager.models.GazeTarget
import com.augmentalis.hudmanager.models.Vector3D
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalCoroutinesApi::class)
class GazeHandlerTest {
    
    // Mock dependencies
    private lateinit var mockService: VoiceAccessibilityService
    private lateinit var mockContext: Context
    private lateinit var mockResources: Resources
    private lateinit var mockDisplayMetrics: DisplayMetrics
    private lateinit var mockHUDManager: HUDManager
    private lateinit var mockGazeTracker: GazeTracker
    
    // Test subject
    private lateinit var gazeHandler: GazeHandler
    
    // Test scheduler
    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)
    
    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock service and dependencies
        mockService = mockk<VoiceAccessibilityService>(relaxed = true)
        mockContext = mockk<Context>(relaxed = true)
        mockResources = mockk<Resources>(relaxed = true)
        mockDisplayMetrics = mockk<DisplayMetrics>(relaxed = true)
        mockHUDManager = mockk<HUDManager>(relaxed = true)
        mockGazeTracker = mockk<GazeTracker>(relaxed = true)
        
        // Configure mock behavior
        every { mockService.applicationContext } returns mockContext
        every { mockService.resources } returns mockResources
        every { mockResources.displayMetrics } returns mockDisplayMetrics
        every { mockDisplayMetrics.widthPixels } returns 1080
        every { mockDisplayMetrics.heightPixels } returns 1920
        
        // Mock HUDManager singleton
        mockkObject(HUDManager.Companion)
        every { HUDManager.getInstance(any()) } returns mockHUDManager
        every { mockHUDManager.gazeTracker } returns mockGazeTracker
        
        // Mock gaze tracker initialization
        every { mockGazeTracker.initialize() } returns true
        coEvery { mockGazeTracker.startTracking() } returns Unit
        every { mockGazeTracker.stopTracking() } returns Unit
        every { mockGazeTracker.dispose() } returns Unit
        coEvery { mockGazeTracker.getCurrentTarget() } returns null
        
        // Mock cursor manager
        val mockCursorManager = mockk<CursorManager>(relaxed = true)
        every { mockService.getCursorManager() } returns mockCursorManager
        every { mockCursorManager.isCursorVisible() } returns true
        
        // Create test subject
        gazeHandler = GazeHandler(mockService)
    }
    
    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }
    
    @Nested
    @DisplayName("Initialization Tests")
    inner class InitializationTests {
        
        @Test
        fun `initialize should set up gaze tracking successfully`() {
            // When
            gazeHandler.initialize()
            
            // Then
            verify { HUDManager.getInstance(mockService) }
            verify { mockHUDManager.gazeTracker }
            verify { mockGazeTracker.initialize() }
        }
        
        @Test
        fun `initialize should handle HUDManager failure gracefully`() {
            // Given
            every { HUDManager.getInstance(any()) } throws RuntimeException("HUDManager failed")
            
            // When/Then - should not throw
            assertDoesNotThrow {
                gazeHandler.initialize()
            }
        }
        
        @Test
        fun `initialize should handle GazeTracker initialization failure`() {
            // Given
            every { mockGazeTracker.initialize() } returns false
            
            // When
            gazeHandler.initialize()
            
            // Then - should still complete initialization
            verify { mockGazeTracker.initialize() }
        }
    }
    
    @Nested
    @DisplayName("Action Handling Tests")
    inner class ActionHandlingTests {
        
        @BeforeEach
        fun setUpHandler() {
            gazeHandler.initialize()
        }
        
        @Test
        fun `canHandle should return true for supported gaze actions`() {
            // Test Legacy Avenue compatibility
            assertTrue(gazeHandler.canHandle("gaze_on"))
            assertTrue(gazeHandler.canHandle("gaze_off"))
            
            // Test enhanced commands
            assertTrue(gazeHandler.canHandle("enable_gaze"))
            assertTrue(gazeHandler.canHandle("disable_gaze"))
            assertTrue(gazeHandler.canHandle("gaze_click"))
            assertTrue(gazeHandler.canHandle("dwell_click"))
            assertTrue(gazeHandler.canHandle("gaze_calibrate"))
            assertTrue(gazeHandler.canHandle("look_and_click"))
            assertTrue(gazeHandler.canHandle("gaze_status"))
            assertTrue(gazeHandler.canHandle("gaze_help"))
        }
        
        @Test
        fun `canHandle should return false for unsupported actions`() {
            assertFalse(gazeHandler.canHandle("unknown_action"))
            assertFalse(gazeHandler.canHandle(""))
            assertFalse(gazeHandler.canHandle("tap"))
            assertFalse(gazeHandler.canHandle("swipe"))
        }
        
        @Test
        fun `getSupportedActions should return all supported commands`() {
            // When
            val actions = gazeHandler.getSupportedActions()
            
            // Then
            assertTrue(actions.contains("gaze_on"))
            assertTrue(actions.contains("gaze_off"))
            assertTrue(actions.contains("enable_gaze"))
            assertTrue(actions.contains("disable_gaze"))
            assertTrue(actions.contains("gaze_click"))
            assertTrue(actions.contains("look_and_click"))
            assertTrue(actions.contains("gaze_calibrate"))
            assertTrue(actions.contains("gaze_status"))
            assertTrue(actions.contains("gaze_help"))
            assertTrue(actions.size >= 14) // Should have at least all defined actions
        }
    }
    
    @Nested
    @DisplayName("Legacy Avenue Compatibility Tests")
    inner class LegacyCompatibilityTests {
        
        @BeforeEach
        fun setUpHandler() {
            gazeHandler.initialize()
        }
        
        @Test
        fun `gaze_on should enable gaze when cursor is visible`() = runTest {
            // Given
            val mockCursorManager = mockk<CursorManager>(relaxed = true)
            every { mockService.getCursorManager() } returns mockCursorManager
            every { mockCursorManager.isCursorVisible() } returns true
            coEvery { mockGazeTracker.startTracking() } returns Unit
            
            // When
            val result = gazeHandler.execute(ActionCategory.GAZE, "gaze_on", emptyMap())
            testScheduler.advanceUntilIdle()
            
            // Then
            assertTrue(result)
            coVerify { mockGazeTracker.startTracking() }
        }
        
        @Test
        fun `gaze_on should fail when cursor is not visible - Legacy behavior`() {
            // Given
            val mockCursorManager = mockk<CursorManager>(relaxed = true)
            every { mockService.getCursorManager() } returns mockCursorManager
            every { mockCursorManager.isCursorVisible() } returns false
            
            // When
            val result = gazeHandler.execute(ActionCategory.GAZE, "gaze_on", emptyMap())
            
            // Then
            assertFalse(result) // Legacy Avenue behavior: fail if cursor not visible
            coVerify(exactly = 0) { mockGazeTracker.startTracking() }
        }
        
        @Test
        fun `gaze_off should disable gaze when cursor is visible`() {
            // Given
            val mockCursorManager = mockk<CursorManager>(relaxed = true)
            every { mockService.getCursorManager() } returns mockCursorManager
            every { mockCursorManager.isCursorVisible() } returns true
            
            // When
            val result = gazeHandler.execute(ActionCategory.GAZE, "gaze_off", emptyMap())
            
            // Then
            assertTrue(result)
            verify { mockGazeTracker.stopTracking() }
        }
        
        @Test
        fun `gaze_off should fail when cursor is not visible - Legacy behavior`() {
            // Given
            val mockCursorManager = mockk<CursorManager>(relaxed = true)
            every { mockService.getCursorManager() } returns mockCursorManager
            every { mockCursorManager.isCursorVisible() } returns false
            
            // When
            val result = gazeHandler.execute(ActionCategory.GAZE, "gaze_off", emptyMap())
            
            // Then
            assertFalse(result) // Legacy Avenue behavior: fail if cursor not visible
        }
    }
    
    @Nested
    @DisplayName("Gaze Click Tests")
    inner class GazeClickTests {
        
        @BeforeEach
        fun setUpHandler() {
            gazeHandler.initialize()
            // Enable gaze first
            every { mockService.getCursorManager()?.javaClass?.getMethod("isCursorVisible")?.invoke(any()) } returns true
            gazeHandler.execute(ActionCategory.GAZE, "gaze_on", emptyMap())
        }
        
        @Test
        fun `gaze_click should perform click at current gaze position`() = runTest {
            // Given
            val mockTarget = GazeTarget("test_target", Vector3D(0.5f, 0.5f, 0f), 0.9f, 0L)
            coEvery { mockGazeTracker.getCurrentTarget() } returns mockTarget
            every { mockService.performClick(any(), any()) } returns true
            
            // When
            val result = gazeHandler.execute(ActionCategory.GAZE, "gaze_click", emptyMap())
            testScheduler.advanceUntilIdle()
            
            // Then
            assertTrue(result)
            // Screen coordinates should be calculated from normalized coordinates
            verify { mockService.performClick(810f, 1440f) } // (0.5+1)*0.5*1080, (0.5+1)*0.5*1920
        }
        
        @Test
        fun `gaze_click should fail when gaze not enabled`() = runTest {
            // Given - disable gaze first
            gazeHandler.execute(ActionCategory.GAZE, "gaze_off", emptyMap())
            
            // When
            val result = gazeHandler.execute(ActionCategory.GAZE, "gaze_click", emptyMap())
            testScheduler.advanceUntilIdle()
            
            // Then
            assertFalse(result)
            verify(exactly = 0) { mockService.performClick(any(), any()) }
        }
        
        @Test
        fun `gaze_click should fail when no gaze target available`() = runTest {
            // Given
            coEvery { mockGazeTracker.getCurrentTarget() } returns null
            
            // When
            val result = gazeHandler.execute(ActionCategory.GAZE, "gaze_click", emptyMap())
            testScheduler.advanceUntilIdle()
            
            // Then
            assertFalse(result)
            verify(exactly = 0) { mockService.performClick(any(), any()) }
        }
        
        @Test
        fun `gaze_click should succeed with force parameter even with low confidence`() = runTest {
            // Given
            val lowConfidenceTarget = GazeTarget("low_confidence", Vector3D(0.5f, 0.5f, 0f), 0.5f, 0L)
            coEvery { mockGazeTracker.getCurrentTarget() } returns lowConfidenceTarget
            every { mockService.performClick(any(), any()) } returns true
            
            // When
            val result = gazeHandler.execute(ActionCategory.GAZE, "gaze_click", mapOf("force" to true))
            testScheduler.advanceUntilIdle()
            
            // Then
            assertTrue(result)
            verify { mockService.performClick(any(), any()) }
        }
        
        @Test
        fun `look_and_click should perform immediate gaze click`() = runTest {
            // Given
            val mockTarget = GazeTarget("look_click_target", Vector3D(0.2f, 0.8f, 0f), 0.9f, 0L)
            coEvery { mockGazeTracker.getCurrentTarget() } returns mockTarget
            every { mockService.performClick(any(), any()) } returns true
            
            // When
            val result = gazeHandler.execute(ActionCategory.GAZE, "look_and_click", emptyMap())
            testScheduler.advanceUntilIdle()
            
            // Then
            assertTrue(result)
            verify { mockService.performClick(any(), any()) }
        }
    }
    
    @Nested
    @DisplayName("Calibration Tests")
    inner class CalibrationTests {
        
        @BeforeEach
        fun setUpHandler() {
            gazeHandler.initialize()
        }
        
        @Test
        fun `gaze_calibrate should perform calibration with target coordinates`() {
            // Given
            val params = mapOf("targetX" to 0.5f, "targetY" to 0.5f)
            every { mockGazeTracker.calibrateGaze(any(), any()) } returns Unit
            
            // When
            val result = gazeHandler.execute(ActionCategory.GAZE, "gaze_calibrate", params)
            
            // Then
            assertTrue(result)
            verify { mockGazeTracker.calibrateGaze(any(), any()) }
        }
        
        @Test
        fun `gaze_calibrate should start interactive calibration without parameters`() {
            // When
            val result = gazeHandler.execute(ActionCategory.GAZE, "gaze_calibrate", emptyMap())
            
            // Then
            assertTrue(result)
            // Should calibrate to center as default
            verify { mockGazeTracker.calibrateGaze(any(), any()) }
        }
        
        @Test
        fun `gaze_center should reset gaze to screen center`() {
            // When
            val result = gazeHandler.execute(ActionCategory.GAZE, "gaze_center", emptyMap())
            
            // Then
            assertTrue(result)
            verify { mockGazeTracker.calibrateGaze(any(), any()) }
        }
    }
    
    @Nested
    @DisplayName("Utility Commands Tests")
    inner class UtilityCommandsTests {
        
        @BeforeEach
        fun setUpHandler() {
            gazeHandler.initialize()
        }
        
        @Test
        fun `toggle_dwell should toggle dwell click functionality`() {
            // When - First toggle (should enable)
            val result1 = gazeHandler.execute(ActionCategory.GAZE, "toggle_dwell", emptyMap())
            
            // Then
            assertTrue(result1)
            
            // When - Second toggle (should disable)  
            val result2 = gazeHandler.execute(ActionCategory.GAZE, "toggle_dwell", emptyMap())
            
            // Then
            assertTrue(result2)
        }
        
        @Test
        fun `gaze_reset should reset tracking system`() {
            // When
            val result = gazeHandler.execute(ActionCategory.GAZE, "gaze_reset", emptyMap())
            
            // Then
            assertTrue(result)
            verify { mockGazeTracker.stopTracking() }
        }
        
        @Test
        fun `gaze_status should return current gaze information`() {
            // When
            val result = gazeHandler.execute(ActionCategory.GAZE, "gaze_status", emptyMap())
            
            // Then
            assertTrue(result)
        }
        
        @Test
        fun `gaze_help should return help information`() {
            // When
            val result = gazeHandler.execute(ActionCategory.GAZE, "gaze_help", emptyMap())
            
            // Then
            assertTrue(result)
        }
    }
    
    @Nested
    @DisplayName("Error Handling Tests")
    inner class ErrorHandlingTests {
        
        @BeforeEach
        fun setUpHandler() {
            gazeHandler.initialize()
        }
        
        @Test
        fun `execute should handle unknown actions gracefully`() {
            // When
            val result = gazeHandler.execute(ActionCategory.GAZE, "unknown_action", emptyMap())
            
            // Then
            assertFalse(result)
        }
        
        @Test
        fun `execute should handle exceptions gracefully`() {
            // Given
            every { mockGazeTracker.stopTracking() } throws RuntimeException("Tracker error")
            
            // When
            val result = gazeHandler.execute(ActionCategory.GAZE, "gaze_off", emptyMap())
            
            // Then
            assertFalse(result) // Should return false on exception
        }
        
        @Test
        fun `performClick should handle screen coordinate calculation errors`() = runTest {
            // Given
            every { mockService.resources } throws RuntimeException("Resources error")
            val mockTarget = GazeTarget("test_target", Vector3D(0.5f, 0.5f, 0f), 0.9f, 0L)
            coEvery { mockGazeTracker.getCurrentTarget() } returns mockTarget
            every { mockService.performClick(any(), any()) } returns true
            
            // Enable gaze first
            every { mockService.getCursorManager()?.javaClass?.getMethod("isCursorVisible")?.invoke(any()) } returns true
            gazeHandler.execute(ActionCategory.GAZE, "gaze_on", emptyMap())
            
            // When
            val result = gazeHandler.execute(ActionCategory.GAZE, "gaze_click", emptyMap())
            testScheduler.advanceUntilIdle()
            
            // Then
            assertTrue(result) // Should still work with fallback defaults
            verify { mockService.performClick(any(), any()) }
        }
    }
    
    @Nested
    @DisplayName("Integration Tests")
    inner class IntegrationTests {
        
        @BeforeEach
        fun setUpHandler() {
            gazeHandler.initialize()
        }
        
        @Test
        fun `cursor integration should enable and disable gaze properly`() {
            // Given
            val mockCursorManager = mockk<CursorManager>(relaxed = true)
            every { mockService.getCursorManager() } returns mockCursorManager
            every { mockCursorManager.getCursorPosition() } returns Pair(100, 200)
            every { mockCursorManager.isCursorVisible() } returns true
            
            // When - Enable gaze
            val enableResult = gazeHandler.execute(ActionCategory.GAZE, "gaze_on", emptyMap())
            
            // Then
            assertTrue(enableResult)
            
            // When - Disable gaze
            val disableResult = gazeHandler.execute(ActionCategory.GAZE, "gaze_off", emptyMap())
            
            // Then
            assertTrue(disableResult)
        }
        
        @Test
        fun `full gaze workflow should work end-to-end`() = runTest {
            // Given
            every { mockService.getCursorManager()?.javaClass?.getMethod("isCursorVisible")?.invoke(any()) } returns true
            val mockTarget = GazeTarget("cursor_target", Vector3D(0.3f, 0.7f, 0f), 0.95f, 0L)
            coEvery { mockGazeTracker.getCurrentTarget() } returns mockTarget
            every { mockService.performClick(any(), any()) } returns true
            
            // When - Enable gaze
            val enableResult = gazeHandler.execute(ActionCategory.GAZE, "gaze_on", emptyMap())
            testScheduler.advanceUntilIdle()
            
            // Then
            assertTrue(enableResult)
            
            // When - Calibrate
            val calibrateResult = gazeHandler.execute(ActionCategory.GAZE, "gaze_calibrate", emptyMap())
            
            // Then  
            assertTrue(calibrateResult)
            
            // When - Perform gaze click
            val clickResult = gazeHandler.execute(ActionCategory.GAZE, "gaze_click", emptyMap())
            testScheduler.advanceUntilIdle()
            
            // Then
            assertTrue(clickResult)
            verify { mockService.performClick(any(), any()) }
            
            // When - Disable gaze
            val disableResult = gazeHandler.execute(ActionCategory.GAZE, "gaze_off", emptyMap())
            
            // Then
            assertTrue(disableResult)
        }
    }
    
    @Nested
    @DisplayName("Disposal Tests")
    inner class DisposalTests {
        
        @Test
        fun `dispose should clean up all resources`() {
            // Given
            gazeHandler.initialize()
            
            // When
            gazeHandler.dispose()
            
            // Then
            verify { mockGazeTracker.stopTracking() }
            verify { mockGazeTracker.dispose() }
        }
        
        @Test
        fun `dispose should handle errors gracefully`() {
            // Given
            gazeHandler.initialize()
            every { mockGazeTracker.dispose() } throws RuntimeException("Disposal error")
            
            // When/Then - should not throw
            assertDoesNotThrow {
                gazeHandler.dispose()
            }
        }
    }
}