/**
 * VoiceCommandTestScenarios.kt - Comprehensive test scenarios for voice commands
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Test Framework
 * Created: 2025-08-28
 * 
 * Test scenarios covering all voice command handlers with natural language variations,
 * confidence thresholds, invalid commands, and partial command handling.
 */
package com.augmentalis.voiceos.accessibility.test

import com.augmentalis.voiceos.accessibility.handlers.ActionCategory

/**
 * Test scenarios for each handler type with comprehensive command variations
 */
object VoiceCommandTestScenarios {
    
    /**
     * Test data class for voice command scenarios
     */
    data class VoiceCommandScenario(
        val category: ActionCategory,
        val command: String,
        val variations: List<String>,
        val expectedAction: String,
        val expectedParams: Map<String, Any> = emptyMap(),
        val expectedResult: Boolean = true,
        val minConfidence: Float = 0.7f,
        val description: String
    )
    
    /**
     * Test data class for confidence threshold testing
     */
    data class ConfidenceTestCase(
        val command: String,
        val confidenceLevel: Float,
        val shouldSucceed: Boolean,
        val description: String
    )
    
    /**
     * Test data class for invalid commands
     */
    data class InvalidCommandCase(
        val command: String,
        val expectedResult: Boolean = false,
        val description: String
    )
    
    /**
     * Test data class for partial commands
     */
    data class PartialCommandCase(
        val command: String,
        val expectedInterpretation: String?,
        val shouldSucceed: Boolean,
        val description: String
    )

    // =================================================================================
    // APP HANDLER TEST SCENARIOS
    // =================================================================================
    
    val appHandlerScenarios = listOf(
        VoiceCommandScenario(
            category = ActionCategory.APP,
            command = "open settings",
            variations = listOf(
                "open settings",
                "launch settings",
                "start settings",
                "open the settings",
                "open up settings",
                "please open settings",
                "can you open settings",
                "hey open settings"
            ),
            expectedAction = "open settings",
            description = "Basic app launch - Settings"
        ),
        
        VoiceCommandScenario(
            category = ActionCategory.APP,
            command = "launch chrome",
            variations = listOf(
                "launch chrome",
                "open chrome",
                "start chrome",
                "open browser",
                "launch browser",
                "open the browser",
                "start web browser"
            ),
            expectedAction = "launch chrome",
            description = "Browser app with aliases"
        ),
        
        VoiceCommandScenario(
            category = ActionCategory.APP,
            command = "open camera",
            variations = listOf(
                "open camera",
                "launch camera",
                "start camera",
                "open photo",
                "take photo",
                "open the camera app"
            ),
            expectedAction = "open camera",
            description = "Camera app with common aliases"
        ),
        
        VoiceCommandScenario(
            category = ActionCategory.APP,
            command = "open phone",
            variations = listOf(
                "open phone",
                "open dialer",
                "make call",
                "call someone",
                "open phone app"
            ),
            expectedAction = "open phone",
            description = "Phone/dialer app"
        ),
        
        VoiceCommandScenario(
            category = ActionCategory.APP,
            command = "open messages",
            variations = listOf(
                "open messages",
                "open sms",
                "send message",
                "text message",
                "messaging app"
            ),
            expectedAction = "open messages",
            description = "Messages/SMS app"
        )
    )
    
    // =================================================================================
    // NAVIGATION HANDLER TEST SCENARIOS
    // =================================================================================
    
    val navigationHandlerScenarios = listOf(
        VoiceCommandScenario(
            category = ActionCategory.NAVIGATION,
            command = "go back",
            variations = listOf(
                "go back",
                "back",
                "navigate back",
                "back button",
                "press back",
                "go to previous",
                "previous screen"
            ),
            expectedAction = "navigate_back",
            description = "Back navigation command"
        ),
        
        VoiceCommandScenario(
            category = ActionCategory.NAVIGATION,
            command = "go home",
            variations = listOf(
                "go home",
                "home",
                "home screen",
                "home button",
                "go to home",
                "main screen",
                "launcher"
            ),
            expectedAction = "navigate_home",
            description = "Home navigation command"
        ),
        
        VoiceCommandScenario(
            category = ActionCategory.NAVIGATION,
            command = "scroll up",
            variations = listOf(
                "scroll up",
                "swipe up",
                "move up",
                "scroll to top",
                "page up"
            ),
            expectedAction = "scroll_up",
            description = "Scroll up command"
        ),
        
        VoiceCommandScenario(
            category = ActionCategory.NAVIGATION,
            command = "scroll down",
            variations = listOf(
                "scroll down",
                "swipe down",
                "move down",
                "scroll to bottom",
                "page down"
            ),
            expectedAction = "scroll_down",
            description = "Scroll down command"
        ),
        
        VoiceCommandScenario(
            category = ActionCategory.NAVIGATION,
            command = "scroll left",
            variations = listOf(
                "scroll left",
                "swipe left",
                "move left",
                "previous page",
                "left swipe"
            ),
            expectedAction = "scroll_left",
            description = "Scroll left command"
        ),
        
        VoiceCommandScenario(
            category = ActionCategory.NAVIGATION,
            command = "scroll right",
            variations = listOf(
                "scroll right",
                "swipe right",
                "move right",
                "next page",
                "right swipe"
            ),
            expectedAction = "scroll_right",
            description = "Scroll right command"
        )
    )
    
    // =================================================================================
    // SYSTEM HANDLER TEST SCENARIOS
    // =================================================================================
    
    val systemHandlerScenarios = listOf(
        VoiceCommandScenario(
            category = ActionCategory.SYSTEM,
            command = "volume up",
            variations = listOf(
                "volume up",
                "increase volume",
                "turn volume up",
                "make it louder",
                "louder",
                "raise volume"
            ),
            expectedAction = "volume_up",
            description = "Volume up command"
        ),
        
        VoiceCommandScenario(
            category = ActionCategory.SYSTEM,
            command = "volume down",
            variations = listOf(
                "volume down",
                "decrease volume",
                "turn volume down",
                "make it quieter",
                "quieter",
                "lower volume"
            ),
            expectedAction = "volume_down",
            description = "Volume down command"
        ),
        
        VoiceCommandScenario(
            category = ActionCategory.SYSTEM,
            command = "mute",
            variations = listOf(
                "mute",
                "silence",
                "turn off sound",
                "mute volume",
                "silent mode"
            ),
            expectedAction = "volume_mute",
            description = "Mute command"
        ),
        
        VoiceCommandScenario(
            category = ActionCategory.SYSTEM,
            command = "recent apps",
            variations = listOf(
                "recent apps",
                "app switcher",
                "multitasking",
                "switch apps",
                "recent applications"
            ),
            expectedAction = "recent_apps",
            description = "Recent apps command"
        ),
        
        VoiceCommandScenario(
            category = ActionCategory.SYSTEM,
            command = "notifications",
            variations = listOf(
                "notifications",
                "notification panel",
                "pull down notifications",
                "show notifications"
            ),
            expectedAction = "notifications",
            description = "Notifications command"
        )
    )
    
    // =================================================================================
    // UI HANDLER TEST SCENARIOS
    // =================================================================================
    
    val uiHandlerScenarios = listOf(
        VoiceCommandScenario(
            category = ActionCategory.UI,
            command = "tap",
            variations = listOf(
                "tap",
                "click",
                "press",
                "touch",
                "select",
                "tap here",
                "click this"
            ),
            expectedAction = "ui_tap",
            description = "Basic tap/click command"
        ),
        
        VoiceCommandScenario(
            category = ActionCategory.UI,
            command = "swipe left",
            variations = listOf(
                "swipe left",
                "left swipe",
                "swipe to left",
                "move left",
                "slide left"
            ),
            expectedAction = "swipe_left",
            description = "Swipe left gesture"
        ),
        
        VoiceCommandScenario(
            category = ActionCategory.UI,
            command = "swipe right",
            variations = listOf(
                "swipe right",
                "right swipe",
                "swipe to right",
                "move right",
                "slide right"
            ),
            expectedAction = "swipe_right",
            description = "Swipe right gesture"
        ),
        
        VoiceCommandScenario(
            category = ActionCategory.UI,
            command = "swipe up",
            variations = listOf(
                "swipe up",
                "up swipe",
                "swipe upward",
                "slide up"
            ),
            expectedAction = "swipe_up",
            description = "Swipe up gesture"
        ),
        
        VoiceCommandScenario(
            category = ActionCategory.UI,
            command = "swipe down",
            variations = listOf(
                "swipe down",
                "down swipe",
                "swipe downward",
                "slide down"
            ),
            expectedAction = "swipe_down",
            description = "Swipe down gesture"
        )
    )
    
    // =================================================================================
    // DEVICE HANDLER TEST SCENARIOS
    // =================================================================================
    
    val deviceHandlerScenarios = listOf(
        VoiceCommandScenario(
            category = ActionCategory.DEVICE,
            command = "brightness up",
            variations = listOf(
                "brightness up",
                "increase brightness",
                "brighter",
                "turn brightness up",
                "make screen brighter"
            ),
            expectedAction = "brightness_up",
            description = "Brightness up command"
        ),
        
        VoiceCommandScenario(
            category = ActionCategory.DEVICE,
            command = "brightness down",
            variations = listOf(
                "brightness down",
                "decrease brightness",
                "dimmer",
                "turn brightness down",
                "make screen dimmer"
            ),
            expectedAction = "brightness_down",
            description = "Brightness down command"
        ),
        
        VoiceCommandScenario(
            category = ActionCategory.DEVICE,
            command = "wifi on",
            variations = listOf(
                "wifi on",
                "enable wifi",
                "turn wifi on",
                "wifi enable",
                "connect wifi"
            ),
            expectedAction = "wifi_enable",
            description = "WiFi enable command"
        ),
        
        VoiceCommandScenario(
            category = ActionCategory.DEVICE,
            command = "wifi off",
            variations = listOf(
                "wifi off",
                "disable wifi",
                "turn wifi off",
                "wifi disable",
                "disconnect wifi"
            ),
            expectedAction = "wifi_disable",
            description = "WiFi disable command"
        ),
        
        VoiceCommandScenario(
            category = ActionCategory.DEVICE,
            command = "bluetooth on",
            variations = listOf(
                "bluetooth on",
                "enable bluetooth",
                "turn bluetooth on",
                "bluetooth enable"
            ),
            expectedAction = "bluetooth_enable",
            description = "Bluetooth enable command"
        ),
        
        VoiceCommandScenario(
            category = ActionCategory.DEVICE,
            command = "bluetooth off",
            variations = listOf(
                "bluetooth off",
                "disable bluetooth",
                "turn bluetooth off",
                "bluetooth disable"
            ),
            expectedAction = "bluetooth_disable",
            description = "Bluetooth disable command"
        )
    )
    
    // =================================================================================
    // INPUT HANDLER TEST SCENARIOS
    // =================================================================================
    
    val inputHandlerScenarios = listOf(
        VoiceCommandScenario(
            category = ActionCategory.INPUT,
            command = "type hello world",
            variations = listOf(
                "type hello world",
                "say hello world",
                "write hello world",
                "input hello world",
                "enter hello world"
            ),
            expectedAction = "input_text:hello world",
            expectedParams = mapOf("text" to "hello world"),
            description = "Text input command"
        ),
        
        VoiceCommandScenario(
            category = ActionCategory.INPUT,
            command = "type my email",
            variations = listOf(
                "type my email",
                "enter my email",
                "input my email address",
                "write my email"
            ),
            expectedAction = "input_text:my email",
            expectedParams = mapOf("text" to "my email"),
            description = "Email input command"
        ),
        
        VoiceCommandScenario(
            category = ActionCategory.INPUT,
            command = "delete",
            variations = listOf(
                "delete",
                "backspace",
                "erase",
                "remove last character"
            ),
            expectedAction = "input_delete",
            description = "Delete/backspace command"
        ),
        
        VoiceCommandScenario(
            category = ActionCategory.INPUT,
            command = "enter",
            variations = listOf(
                "enter",
                "return",
                "submit",
                "press enter"
            ),
            expectedAction = "input_enter",
            description = "Enter/return command"
        )
    )
    
    // =================================================================================
    // ACTION HANDLER TEST SCENARIOS
    // =================================================================================
    
    val actionHandlerScenarios = listOf(
        VoiceCommandScenario(
            category = ActionCategory.CUSTOM,
            command = "take screenshot",
            variations = listOf(
                "take screenshot",
                "screenshot",
                "capture screen",
                "screen capture"
            ),
            expectedAction = "take_screenshot",
            description = "Screenshot action command"
        ),
        
        VoiceCommandScenario(
            category = ActionCategory.CUSTOM,
            command = "open accessibility",
            variations = listOf(
                "open accessibility",
                "accessibility settings",
                "voice accessibility"
            ),
            expectedAction = "open_accessibility",
            description = "Accessibility action command"
        )
    )
    
    // =================================================================================
    // CONFIDENCE THRESHOLD TEST CASES
    // =================================================================================
    
    val confidenceTestCases = listOf(
        ConfidenceTestCase(
            command = "open settings",
            confidenceLevel = 0.95f,
            shouldSucceed = true,
            description = "High confidence - should succeed"
        ),
        
        ConfidenceTestCase(
            command = "open settings",
            confidenceLevel = 0.85f,
            shouldSucceed = true,
            description = "Good confidence - should succeed"
        ),
        
        ConfidenceTestCase(
            command = "open settings",
            confidenceLevel = 0.70f,
            shouldSucceed = true,
            description = "Minimum confidence - should succeed"
        ),
        
        ConfidenceTestCase(
            command = "open settings",
            confidenceLevel = 0.60f,
            shouldSucceed = false,
            description = "Below threshold - should fail"
        ),
        
        ConfidenceTestCase(
            command = "open settings",
            confidenceLevel = 0.40f,
            shouldSucceed = false,
            description = "Low confidence - should fail"
        ),
        
        ConfidenceTestCase(
            command = "mumbled unclear words",
            confidenceLevel = 0.30f,
            shouldSucceed = false,
            description = "Very low confidence unclear command - should fail"
        )
    )
    
    // =================================================================================
    // INVALID COMMAND TEST CASES
    // =================================================================================
    
    val invalidCommandCases = listOf(
        InvalidCommandCase(
            command = "",
            description = "Empty command"
        ),
        
        InvalidCommandCase(
            command = "   ",
            description = "Whitespace only command"
        ),
        
        InvalidCommandCase(
            command = "xyz123 invalid command",
            description = "Completely invalid command"
        ),
        
        InvalidCommandCase(
            command = "open nonexistent app that definitely does not exist",
            description = "Open non-existent app"
        ),
        
        InvalidCommandCase(
            command = "perform impossible action",
            description = "Impossible action request"
        ),
        
        InvalidCommandCase(
            command = "random gibberish text here",
            description = "Random gibberish"
        ),
        
        InvalidCommandCase(
            command = "123456789",
            description = "Numbers only"
        ),
        
        InvalidCommandCase(
            command = "!@#$%^&*()",
            description = "Special characters only"
        )
    )
    
    // =================================================================================
    // PARTIAL COMMAND TEST CASES
    // =================================================================================
    
    val partialCommandCases = listOf(
        PartialCommandCase(
            command = "open",
            expectedInterpretation = null,
            shouldSucceed = false,
            description = "Incomplete app command - missing app name"
        ),
        
        PartialCommandCase(
            command = "type",
            expectedInterpretation = null,
            shouldSucceed = false,
            description = "Incomplete text input - missing text"
        ),
        
        PartialCommandCase(
            command = "swipe",
            expectedInterpretation = null,
            shouldSucceed = false,
            description = "Incomplete swipe - missing direction"
        ),
        
        PartialCommandCase(
            command = "volume",
            expectedInterpretation = null,
            shouldSucceed = false,
            description = "Incomplete volume - missing up/down"
        ),
        
        PartialCommandCase(
            command = "brightness",
            expectedInterpretation = null,
            shouldSucceed = false,
            description = "Incomplete brightness - missing up/down"
        ),
        
        PartialCommandCase(
            command = "wifi",
            expectedInterpretation = null,
            shouldSucceed = false,
            description = "Incomplete wifi - missing on/off"
        ),
        
        PartialCommandCase(
            command = "bluetooth",
            expectedInterpretation = null,
            shouldSucceed = false,
            description = "Incomplete bluetooth - missing on/off"
        ),
        
        PartialCommandCase(
            command = "scroll",
            expectedInterpretation = null,
            shouldSucceed = false,
            description = "Incomplete scroll - missing direction"
        )
    )
    
    // =================================================================================
    // NATURAL LANGUAGE VARIATIONS
    // =================================================================================
    
    val naturalLanguageVariations = listOf(
        VoiceCommandScenario(
            category = ActionCategory.APP,
            command = "please open settings for me",
            variations = listOf(
                "please open settings for me",
                "can you open settings please",
                "would you mind opening settings",
                "could you open the settings app",
                "hey, open settings now"
            ),
            expectedAction = "open settings",
            description = "Polite natural language variations"
        ),
        
        VoiceCommandScenario(
            category = ActionCategory.NAVIGATION,
            command = "I want to go back to the previous screen",
            variations = listOf(
                "I want to go back to the previous screen",
                "please take me back to the last page",
                "can we go back to where we were",
                "navigate back to the previous screen"
            ),
            expectedAction = "navigate_back",
            description = "Natural language back navigation"
        ),
        
        VoiceCommandScenario(
            category = ActionCategory.INPUT,
            command = "please type the word hello for me",
            variations = listOf(
                "please type the word hello for me",
                "can you enter the text hello",
                "I need you to type hello",
                "would you write hello please"
            ),
            expectedAction = "input_text:hello",
            expectedParams = mapOf("text" to "hello"),
            description = "Natural language text input"
        )
    )
    
    // =================================================================================
    // ALL SCENARIOS COMBINED
    // =================================================================================
    
    val allScenarios = listOf(
        appHandlerScenarios,
        navigationHandlerScenarios,
        systemHandlerScenarios,
        uiHandlerScenarios,
        deviceHandlerScenarios,
        inputHandlerScenarios,
        actionHandlerScenarios,
        naturalLanguageVariations
    ).flatten()
    
    /**
     * Get scenarios for a specific handler category
     */
    fun getScenariosForCategory(category: ActionCategory): List<VoiceCommandScenario> {
        return allScenarios.filter { it.category == category }
    }
    
    /**
     * Get all test commands for a category
     */
    fun getTestCommandsForCategory(category: ActionCategory): List<String> {
        return getScenariosForCategory(category).flatMap { scenario ->
            listOf(scenario.command) + scenario.variations
        }
    }
    
    /**
     * Get expected results for test commands
     */
    fun getExpectedResults(): Map<String, ExpectedResult> {
        val results = mutableMapOf<String, ExpectedResult>()
        
        allScenarios.forEach { scenario ->
            val commands = listOf(scenario.command) + scenario.variations
            commands.forEach { command ->
                results[command] = ExpectedResult(
                    action = scenario.expectedAction,
                    category = scenario.category,
                    params = scenario.expectedParams,
                    shouldSucceed = scenario.expectedResult,
                    minConfidence = scenario.minConfidence
                )
            }
        }
        
        return results
    }
    
    /**
     * Expected result data class
     */
    data class ExpectedResult(
        val action: String,
        val category: ActionCategory,
        val params: Map<String, Any>,
        val shouldSucceed: Boolean,
        val minConfidence: Float
    )
}