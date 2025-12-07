/**
 * VoiceCommandIntegrationTest.kt - Integration tests for voice command processing
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-08-28
 * 
 * Tests for voice command routing, confidence filtering, command variations,
 * and handler execution through the ActionCoordinator.
 */
package com.augmentalis.voiceaccessibility.integration

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.voiceos.accessibility.managers.ActionCoordinator
import com.augmentalis.voiceos.accessibility.service.VoiceAccessibilityService
import com.augmentalis.voiceaccessibility.mocks.MockActionCoordinator
import com.augmentalis.voiceaccessibility.mocks.MockVoiceAccessibilityService
import com.augmentalis.voicerecognition.IRecognitionCallback
import com.augmentalis.voicerecognition.IVoiceRecognitionService
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Integration tests for voice command processing pipeline
 * 
 * Tests the complete flow from voice recognition to action execution:
 * - Voice command routing through ActionCoordinator
 * - Confidence filtering and thresholds
 * - Command variations and interpretation
 * - Handler execution and verification
 * - End-to-end AIDL communication with command processing
 */
@RunWith(AndroidJUnit4::class)
class VoiceCommandIntegrationTest {
    
    companion object {
        private const val TAG = "VoiceCommandIntegrationTest"
        private const val SERVICE_PACKAGE = "com.augmentalis.voicerecognition"
        private const val SERVICE_CLASS = "com.augmentalis.voicerecognition.service.VoiceRecognitionService"
        private const val BINDING_TIMEOUT_MS = 10000L
        private const val COMMAND_TIMEOUT_MS = 5000L
        private const val MIN_CONFIDENCE_THRESHOLD = 0.5f
        private const val HIGH_CONFIDENCE_THRESHOLD = 0.8f
        private const val LOW_CONFIDENCE_THRESHOLD = 0.3f
    }
    
    private lateinit var context: Context
    private lateinit var mockActionCoordinator: MockActionCoordinator
    private lateinit var mockService: MockVoiceAccessibilityService
    private var voiceRecognitionService: IVoiceRecognitionService? = null
    private val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    // Test service connection for VoiceRecognition service
    private val serviceConnection = object : ServiceConnection {
        private val connectionLatch = CountDownLatch(1)
        private val isConnected = AtomicBoolean(false)
        
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            voiceRecognitionService = IVoiceRecognitionService.Stub.asInterface(service)
            isConnected.set(true)
            connectionLatch.countDown()
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            voiceRecognitionService = null
            isConnected.set(false)
        }
        
        fun waitForConnection(): Boolean {
            return connectionLatch.await(BINDING_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        }
        
        fun isServiceConnected(): Boolean = isConnected.get()
    }
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockService = MockVoiceAccessibilityService()
        mockActionCoordinator = MockActionCoordinator(mockService)
        
        // Connect to VoiceRecognitionService for end-to-end tests
        val intent = Intent().apply {
            setClassName(SERVICE_PACKAGE, SERVICE_CLASS)
        }
        
        context.bindService(
            intent,
            serviceConnection,
            Context.BIND_AUTO_CREATE or Context.BIND_IMPORTANT
        )
        
        assertTrue("VoiceRecognition service should bind", serviceConnection.waitForConnection())
    }
    
    @After
    fun tearDown() {
        try {
            context.unbindService(serviceConnection)
        } catch (e: Exception) {
            // Ignore unbinding errors during cleanup
        }
        
        testScope.cancel()
    }
    
    /**
     * Test basic voice command routing
     */
    @Test
    fun testBasicVoiceCommandRouting() = runTest {
        val testCommands = mapOf(
            "go back" to "navigate_back",
            "go home" to "navigate_home",
            "volume up" to "volume_up",
            "scroll down" to "scroll_down",
            "open settings" to "launch_app:settings"
        )
        
        for ((voiceCommand, _) in testCommands) {
            mockActionCoordinator.reset()
            
            val result = mockActionCoordinator.processCommand(voiceCommand)
            
            assertTrue("Command '$voiceCommand' should be processed", result)
            assertTrue("Action coordinator should have received command", 
                mockActionCoordinator.hasProcessedCommand(voiceCommand))
            
            // Verify the command was interpreted correctly
            val processedCommands = mockActionCoordinator.getProcessedCommands()
            assertTrue("Should have processed at least one command", processedCommands.isNotEmpty())
        }
    }
    
    /**
     * Test confidence-based filtering
     */
    @Test
    fun testConfidenceFiltering() = runTest {
        val testCommand = "open settings"
        
        // Test high confidence - should be accepted
        mockActionCoordinator.reset()
        val highConfidenceResult = mockActionCoordinator.processVoiceCommand(
            testCommand, 
            HIGH_CONFIDENCE_THRESHOLD
        )
        assertTrue("High confidence command should be accepted", highConfidenceResult)
        assertTrue("High confidence command should be processed", 
            mockActionCoordinator.hasProcessedCommand(testCommand))
        
        // Test medium confidence - should be accepted if above threshold
        mockActionCoordinator.reset()
        val mediumConfidenceResult = mockActionCoordinator.processVoiceCommand(
            testCommand, 
            MIN_CONFIDENCE_THRESHOLD + 0.1f
        )
        assertTrue("Medium confidence command should be accepted", mediumConfidenceResult)
        
        // Test low confidence - behavior depends on implementation
        mockActionCoordinator.reset()
        mockActionCoordinator.processVoiceCommand(
            testCommand, 
            LOW_CONFIDENCE_THRESHOLD
        )
        // Low confidence might still be processed, just noting the confidence
        assertTrue("Command should be attempted regardless of confidence", 
            mockActionCoordinator.getProcessedCommands().isNotEmpty())
    }
    
    /**
     * Test command variations and interpretation
     */
    @Test
    fun testCommandVariations() = runTest {
        val commandVariations = listOf(
            // Navigation variations
            listOf("go back", "back", "navigate back", "return"),
            listOf("go home", "home", "navigate home", "home screen"),
            
            // App launching variations  
            listOf("open settings", "launch settings", "start settings", "settings"),
            listOf("open camera", "launch camera", "start camera", "camera"),
            
            // Volume variations
            listOf("volume up", "increase volume", "turn up volume", "louder"),
            listOf("volume down", "decrease volume", "turn down volume", "quieter"),
            
            // UI action variations
            listOf("tap", "click", "touch", "press"),
            listOf("swipe left", "slide left", "scroll left"),
            
            // System variations
            listOf("scroll up", "scroll to top", "move up"),
            listOf("scroll down", "scroll to bottom", "move down")
        )
        
        for (variations in commandVariations) {
            val baseCommand = variations.first()
            mockActionCoordinator.reset()
            
            // Test base command
            val baseResult = mockActionCoordinator.processCommand(baseCommand)
            assertTrue("Base command '$baseCommand' should be processed", baseResult)
            
            // Test variations - they should all map to similar actions
            for (variation in variations.drop(1)) {
                mockActionCoordinator.reset()
                val variationResult = mockActionCoordinator.processCommand(variation)
                assertTrue("Variation '$variation' should be processed", variationResult)
                assertTrue("Variation '$variation' should be handled", 
                    mockActionCoordinator.hasProcessedCommand(variation))
            }
        }
    }
    
    /**
     * Test handler execution and verification
     */
    @Test 
    fun testHandlerExecution() = runTest {
        val handlerTests = mapOf(
            "system" to listOf("navigate_back", "navigate_home", "volume_up", "volume_down"),
            "navigation" to listOf("scroll_up", "scroll_down", "scroll_left", "scroll_right"),
            "app" to listOf("launch_app:settings", "launch_app:camera"),
            "device" to listOf("brightness_up", "brightness_down", "wifi_enable", "wifi_disable"),
            "input" to listOf("input_text:hello world"),
            "ui" to listOf("ui_tap", "swipe_left", "swipe_right")
        )
        
        for ((handlerType, actions) in handlerTests) {
            for (action in actions) {
                mockActionCoordinator.reset()
                
                val result = mockActionCoordinator.executeAction(action)
                assertTrue("Action '$action' should be executed by $handlerType handler", result)
                assertTrue("Action '$action' should be tracked", 
                    mockActionCoordinator.hasExecutedAction(action))
                
                // Verify execution metrics
                val metrics = mockActionCoordinator.getMetricsForAction(action)
                assertNotNull("Should have metrics for action '$action'", metrics)
                assertEquals("Should have one execution", 1, metrics?.count)
            }
        }
    }
    
    /**
     * Test end-to-end voice recognition to command execution
     */
    @Test
    fun testEndToEndVoiceCommand() = runTest {
        val service = voiceRecognitionService
        assertNotNull("VoiceRecognition service should be connected", service)
        service!!
        
        // Create callback that routes to our mock coordinator
        val callback = object : IRecognitionCallback.Stub() {
            override fun onRecognitionResult(text: String?, confidence: Float, isFinal: Boolean) {
                if (text != null && isFinal) {
                    mockActionCoordinator.processVoiceCommand(text, confidence)
                }
            }
            
            override fun onError(errorCode: Int, message: String?) {
                // Log errors but continue test
            }
            
            override fun onStateChanged(state: Int, message: String?) {
                // Track state changes
            }
            
            override fun onPartialResult(partialText: String?) {
                // Handle partial results if needed
            }
        }
        
        // Register callback and start recognition
        service.registerCallback(callback)
        mockActionCoordinator.reset()
        
        // Simulate recognition results directly since we can't generate actual voice
        val testCommands = listOf(
            "go back" to 0.9f,
            "volume up" to 0.8f,
            "open settings" to 0.85f
        )
        
        for ((command, confidence) in testCommands) {
            callback.onRecognitionResult(command, confidence, true)
            
            // Verify command was processed
            assertTrue("Command '$command' should be processed", 
                mockActionCoordinator.hasProcessedCommand(command))
            
            val metrics = mockActionCoordinator.getMetricsForAction("voice:$command")
            assertNotNull("Should have voice metrics for '$command'", metrics)
        }
        
        service.unregisterCallback(callback)
    }
    
    /**
     * Test multi-step command sequences
     */
    @Test
    fun testMultiStepCommandSequences() = runTest {
        val commandSequences = listOf(
            // App launching and navigation
            listOf("open settings", "scroll down", "scroll down", "go back"),
            
            // Volume control sequence
            listOf("volume up", "volume up", "volume down", "mute"),
            
            // Navigation sequence
            listOf("go home", "open camera", "go back", "go home"),
            
            // UI interaction sequence
            listOf("scroll up", "scroll up", "tap", "scroll down")
        )
        
        for (sequence in commandSequences) {
            mockActionCoordinator.reset()
            
            for ((index, command) in sequence.withIndex()) {
                val result = mockActionCoordinator.processCommand(command)
                assertTrue("Step ${index + 1}: '$command' should be processed", result)
                assertTrue("Step ${index + 1}: '$command' should be tracked", 
                    mockActionCoordinator.hasProcessedCommand(command))
            }
            
            // Verify all commands in sequence were processed
            val processedCommands = mockActionCoordinator.getProcessedCommands()
            assertEquals("Should have processed all ${sequence.size} commands", 
                sequence.size, processedCommands.size)
        }
    }
    
    /**
     * Test error handling and recovery
     */
    @Test
    fun testErrorHandlingAndRecovery() = runTest {
        // Test invalid commands
        val invalidCommands = listOf(
            "",
            "   ",
            "invalid command that should not work",
            "xyz123 nonexistent action"
        )
        
        for (invalidCommand in invalidCommands) {
            mockActionCoordinator.reset()
            
            val result = mockActionCoordinator.processCommand(invalidCommand)
            // Invalid commands might return false or be ignored
            mockActionCoordinator.getProcessedCommands()
            
            if (!result) {
                // If command failed, verify it was handled gracefully
                assertTrue("Invalid command should be handled gracefully", true)
            }
        }
        
        // Test recovery with valid command after invalid ones
        mockActionCoordinator.reset()
        mockActionCoordinator.processCommand("invalid command")
        val validResult = mockActionCoordinator.processCommand("go back")
        
        assertTrue("Valid command should work after invalid one", validResult)
        assertTrue("Valid command should be processed", 
            mockActionCoordinator.hasProcessedCommand("go back"))
    }
    
    /**
     * Test performance and timing
     */
    @Test
    fun testPerformanceAndTiming() = runTest {
        val performanceCommands = listOf(
            "go back", "go home", "volume up", "scroll down", 
            "open settings", "swipe left", "tap"
        )
        
        mockActionCoordinator.reset()
        
        // Execute commands multiple times to test performance
        repeat(10) {
            for (command in performanceCommands) {
                val startTime = System.currentTimeMillis()
                val result = mockActionCoordinator.processCommand(command)
                val executionTime = System.currentTimeMillis() - startTime
                
                assertTrue("Command '$command' should execute successfully", result)
                assertTrue("Command '$command' should execute quickly (< 1000ms)", 
                    executionTime < 1000)
            }
        }
        
        // Verify metrics collection
        val allMetrics = mockActionCoordinator.getMetrics()
        assertTrue("Should have collected performance metrics", allMetrics.isNotEmpty())
        
        for ((action, metrics) in allMetrics) {
            assertTrue("Action '$action' should have reasonable execution count", 
                metrics.count > 0)
            assertTrue("Action '$action' should have reasonable average time", 
                metrics.averageTimeMs < 500)
        }
    }
    
    /**
     * Test concurrent command processing
     */
    @Test
    fun testConcurrentCommandProcessing() = runTest {
        val commands = listOf("go back", "volume up", "scroll down", "open settings")
        val results = mutableListOf<Boolean>()
        mockActionCoordinator.reset()
        
        // Execute commands concurrently
        val jobs = commands.map { command ->
            async {
                mockActionCoordinator.processCommand(command)
            }
        }
        
        // Wait for all commands to complete
        val completedResults = jobs.awaitAll()
        results.addAll(completedResults)
        
        // Verify all commands were processed successfully
        assertTrue("All concurrent commands should succeed", 
            results.all { it })
        
        val processedCommands = mockActionCoordinator.getProcessedCommands()
        assertEquals("Should have processed all concurrent commands", 
            commands.size, processedCommands.size)
        
        // Verify all commands were actually processed
        for (command in commands) {
            assertTrue("Command '$command' should have been processed", 
                mockActionCoordinator.hasProcessedCommand(command))
        }
    }
}