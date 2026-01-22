/**
 * VisualRegressionTest.kt - Visual regression testing for UI components
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team  
 * Created: 2025-01-28
 * 
 * Screenshot-based testing to ensure UI consistency across changes
 */
package com.augmentalis.voiceuielements.visual

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.voiceuielements.components.*
import com.augmentalis.voiceuielements.models.*
import com.augmentalis.voiceuielements.theme.VoiceUITheme
import com.augmentalis.voiceuielements.utils.assertAgainstGolden
import com.augmentalis.voiceuielements.utils.captureToImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VisualRegressionTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun visualTest_voiceCommandButton_lightTheme() {
        composeTestRule.setContent {
            VoiceUITheme(darkTheme = false) {
                VoiceCommandButton(
                    text = "Test Command",
                    onClick = { },
                    enabled = true
                )
            }
        }
        
        composeTestRule
            .onNodeWithText("Test Command")
            .captureToImage()
            .assertAgainstGolden("voice_command_button_light")
    }
    
    @Test
    fun visualTest_voiceCommandButton_darkTheme() {
        composeTestRule.setContent {
            VoiceUITheme(darkTheme = true) {
                VoiceCommandButton(
                    text = "Test Command", 
                    onClick = { },
                    enabled = true
                )
            }
        }
        
        composeTestRule
            .onNodeWithText("Test Command")
            .captureToImage()
            .assertAgainstGolden("voice_command_button_dark")
    }
    
    @Test
    fun visualTest_voiceStatusCard_listeningState() {
        val status = VoiceStatus(
            isListening = true,
            recognitionEngine = "Vosk",
            currentLanguage = "English",
            confidence = 0.85f,
            lastCommand = "go back"
        )
        
        composeTestRule.setContent {
            VoiceUITheme {
                VoiceStatusCard(status = status)
            }
        }
        
        composeTestRule
            .onNodeWithText("Listening")
            .captureToImage() 
            .assertAgainstGolden("voice_status_card_listening")
    }
    
    @Test
    fun visualTest_glassmorphismCard_effects() {
        composeTestRule.setContent {
            VoiceUITheme {
                GlassmorphismCard(
                    glassmorphismConfig = GlassmorphismConfig(
                        blurRadius = 20f,
                        alpha = 0.8f,
                        cornerRadius = 12f
                    )
                ) {
                    androidx.compose.material3.Text("Glassmorphism Content")
                }
            }
        }
        
        composeTestRule
            .onNodeWithText("Glassmorphism Content")
            .captureToImage()
            .assertAgainstGolden("glassmorphism_card")
    }
    
    @Test
    fun visualTest_voiceWaveform_animating() {
        composeTestRule.setContent {
            VoiceUITheme {
                VoiceWaveform(
                    isAnimating = true,
                    amplitude = 0.7f,
                    color = Color.Blue
                )
            }
        }
        
        // Wait for animation to settle
        composeTestRule.waitForIdle()
        
        composeTestRule
            .onNodeWithTag("voice_waveform")
            .captureToImage()
            .assertAgainstGolden("voice_waveform_animating")
    }
}