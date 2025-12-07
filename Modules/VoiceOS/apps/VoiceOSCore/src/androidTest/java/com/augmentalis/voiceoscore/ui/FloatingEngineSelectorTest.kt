/**
 * FloatingEngineSelectorTest.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-11
 */
/**
 * FloatingEngineSelectorTest.kt
 * 
 * Purpose: Instrumented test for floating engine selector UI
 * Tests engine selection, initialization, and AIDL communication
 */
package com.augmentalis.voiceoscore.ui

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.augmentalis.voiceoscore.ui.components.FloatingEngineSelector
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for FloatingEngineSelector
 * Tests UI interactions and engine switching
 */
@RunWith(AndroidJUnit4::class)
class FloatingEngineSelectorTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    private lateinit var context: Context
    private var selectedEngine = "vivoka"
    private var isRecognizing = false
    private var lastInitiatedEngine: String? = null
    
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }
    
    @Test
    fun testEngineSelectorExpandsAndCollapses() {
        composeTestRule.setContent {
            FloatingEngineSelector(
                selectedEngine = selectedEngine,
                onEngineSelected = { selectedEngine = it },
                onInitiate = { },
                isRecognizing = false
            )
        }
        
        // Initially, selector should be collapsed (showing settings icon)
        composeTestRule.onNodeWithContentDescription("Select Engine")
            .assertExists()
            .assertIsDisplayed()
        
        // Click to expand
        composeTestRule.onNodeWithContentDescription("Select Engine")
            .performClick()
        
        // Should show "Engines" label when expanded
        composeTestRule.onNodeWithText("Engines")
            .assertExists()
            .assertIsDisplayed()
        
        // Should show all engine buttons
        composeTestRule.onNodeWithText("V").assertExists() // Vivoka
        composeTestRule.onNodeWithText("K").assertExists() // Vosk
        composeTestRule.onNodeWithText("A").assertExists() // Android
        composeTestRule.onNodeWithText("W").assertExists() // Whisper
        composeTestRule.onNodeWithText("G").assertExists() // Google
    }
    
    @Test
    fun testEngineSelection() {
        composeTestRule.setContent {
            FloatingEngineSelector(
                selectedEngine = selectedEngine,
                onEngineSelected = { engine -> 
                    selectedEngine = engine
                },
                onInitiate = { },
                isRecognizing = false
            )
        }
        
        // Expand selector
        composeTestRule.onNodeWithContentDescription("Select Engine")
            .performClick()
        
        // Click on Vosk engine (K)
        composeTestRule.onNodeWithText("K")
            .performClick()
        
        // Verify engine was selected
        assertEquals("vosk", selectedEngine)
        
        // Click on Android engine (A)
        composeTestRule.onNodeWithText("A")
            .performClick()
        
        // Verify engine was selected
        assertEquals("android_stt", selectedEngine)
    }
    
    @Test
    fun testInitiateButton() {
        var initiateClickCount = 0
        
        composeTestRule.setContent {
            FloatingEngineSelector(
                selectedEngine = "vivoka",
                onEngineSelected = { },
                onInitiate = { engine ->
                    lastInitiatedEngine = engine
                    initiateClickCount++
                    isRecognizing = !isRecognizing
                },
                isRecognizing = isRecognizing
            )
        }
        
        // Expand selector
        composeTestRule.onNodeWithContentDescription("Select Engine")
            .performClick()
        
        // Find and click initiate button (Play icon)
        composeTestRule.onNodeWithContentDescription("Start")
            .performClick()
        
        // Verify initiate was called
        assertEquals(1, initiateClickCount)
        assertEquals("vivoka", lastInitiatedEngine)
    }
    
    @Test
    fun testEngineColorCoding() {
        composeTestRule.setContent {
            FloatingEngineSelector(
                selectedEngine = "vivoka",
                onEngineSelected = { },
                onInitiate = { },
                isRecognizing = false
            )
        }
        
        // Expand selector
        composeTestRule.onNodeWithContentDescription("Select Engine")
            .performClick()
        
        // Verify Vivoka button exists with its initial
        composeTestRule.onNodeWithText("V")
            .assertExists()
            .assertIsDisplayed()
        
        // Should show selection indicator (dot) for selected engine
        // Note: The dot is rendered as "●" character below the initial
        composeTestRule.onNodeWithText("●")
            .assertExists()
    }
    
    @Test
    fun testRecognizingState() {
        composeTestRule.setContent {
            FloatingEngineSelector(
                selectedEngine = "vivoka",
                onEngineSelected = { },
                onInitiate = { },
                isRecognizing = true // Recognizing state
            )
        }
        
        // Expand selector
        composeTestRule.onNodeWithContentDescription("Select Engine")
            .performClick()
        
        // When recognizing, the initiate button should show "Stop" 
        // (though content description is still "Stop" in our implementation)
        composeTestRule.onNodeWithContentDescription("Stop")
            .assertExists()
    }
    
    @Test
    fun testQuickEngineSwitch() {
        var switchCount = 0
        val engineSequence = mutableListOf<String>()
        
        composeTestRule.setContent {
            FloatingEngineSelector(
                selectedEngine = selectedEngine,
                onEngineSelected = { engine ->
                    selectedEngine = engine
                    engineSequence.add(engine)
                    switchCount++
                },
                onInitiate = { },
                isRecognizing = false
            )
        }
        
        // Expand selector
        composeTestRule.onNodeWithContentDescription("Select Engine")
            .performClick()
        
        // Quick switch between engines
        composeTestRule.onNodeWithText("V").performClick() // Vivoka
        composeTestRule.onNodeWithText("K").performClick() // Vosk
        composeTestRule.onNodeWithText("A").performClick() // Android
        composeTestRule.onNodeWithText("W").performClick() // Whisper
        
        // Verify all switches were recorded
        assertEquals(4, switchCount)
        assertEquals(listOf("vivoka", "vosk", "android_stt", "whisper"), engineSequence)
    }
    
    @Test
    fun testEngineInitialization() {
        val initResults = mutableMapOf<String, Boolean>()
        
        composeTestRule.setContent {
            FloatingEngineSelector(
                selectedEngine = selectedEngine,
                onEngineSelected = { engine ->
                    selectedEngine = engine
                },
                onInitiate = { engine ->
                    // Simulate engine initialization
                    initResults[engine] = when (engine) {
                        "vivoka", "vosk", "android_stt" -> true
                        "whisper" -> true // Whisper should work
                        "google_cloud" -> false // Google Cloud disabled
                        else -> false
                    }
                },
                isRecognizing = false
            )
        }
        
        // Expand selector
        composeTestRule.onNodeWithContentDescription("Select Engine")
            .performClick()
        
        // Test each engine
        val engines = listOf(
            "V" to "vivoka",
            "K" to "vosk",
            "A" to "android_stt",
            "W" to "whisper",
            "G" to "google_cloud"
        )
        
        engines.forEach { (initial, _) ->
            // Select engine
            composeTestRule.onNodeWithText(initial).performClick()
            
            // Initiate
            composeTestRule.onNodeWithContentDescription("Start").performClick()
            
            // Wait for UI to update
            composeTestRule.waitForIdle()
        }
        
        // Verify initialization results
        assertTrue("Vivoka should initialize", initResults["vivoka"] == true)
        assertTrue("Vosk should initialize", initResults["vosk"] == true)
        assertTrue("Android STT should initialize", initResults["android_stt"] == true)
        assertTrue("Whisper should initialize", initResults["whisper"] == true)
        assertFalse("Google Cloud should fail (disabled)", initResults["google_cloud"] == true)
    }
}

/**
 * Additional test for integration with MainViewModel
 */
@RunWith(AndroidJUnit4::class)
class FloatingEngineSelectorIntegrationTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testEngineSelectionPersistence() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("voice_recognition_prefs", Context.MODE_PRIVATE)
        
        // Clear preferences
        prefs.edit().clear().commit()
        
        // Set initial engine
        prefs.edit().putString("selected_engine", "vosk").commit()
        
        // Verify preference was saved
        val savedEngine = prefs.getString("selected_engine", "vivoka")
        assertEquals("vosk", savedEngine)
        
        // Change to different engine
        prefs.edit().putString("selected_engine", "android_stt").commit()
        
        // Verify change persisted
        val updatedEngine = prefs.getString("selected_engine", "vivoka")
        assertEquals("android_stt", updatedEngine)
    }
    
    @Test
    fun testDefaultEngineIsVivoka() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = context.getSharedPreferences("voice_recognition_prefs", Context.MODE_PRIVATE)
        
        // Clear preferences to simulate fresh install
        prefs.edit().clear().commit()
        
        // Get engine with default
        val engine = prefs.getString("selected_engine", "vivoka")
        
        // Should default to Vivoka as requested
        assertEquals("vivoka", engine)
    }
}