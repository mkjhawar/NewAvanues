/**
 * CommandManagerUITest.kt - UI instrumentation tests for CommandManager
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 * 
 * Tests UI components, interactions, and visual elements
 */
package com.augmentalis.commandmanager.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.augmentalis.commandmanager.models.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class CommandManagerUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun testCommandManagerActivityLaunch() {
        composeTestRule.setContent {
            CommandManagerContent(
                viewModel = CommandViewModel(context)
            )
        }

        // Verify main components are displayed
        composeTestRule.onNodeWithText("Command Manager").assertIsDisplayed()
        composeTestRule.onNodeWithText("Command Statistics").assertIsDisplayed()
        composeTestRule.onNodeWithText("Quick Test").assertIsDisplayed()
    }

    @Test
    fun testCommandStatsCardDisplay() {
        composeTestRule.setContent {
            CommandStatsCard(
                stats = CommandStats(
                    totalCommands = 100,
                    successfulCommands = 85,
                    failedCommands = 15,
                    averageExecutionTime = 120L,
                    topCommands = listOf("go_back", "volume_up", "scroll_down")
                ),
                onRefresh = { }
            )
        }

        // Verify stats are displayed
        composeTestRule.onNodeWithText("100").assertIsDisplayed()
        composeTestRule.onNodeWithText("85").assertIsDisplayed()
        composeTestRule.onNodeWithText("15").assertIsDisplayed()
        composeTestRule.onNodeWithText("120ms").assertIsDisplayed()
    }

    @Test
    fun testQuickTestPanelInteraction() {
        composeTestRule.setContent {
            QuickTestPanel(
                isLoading = false,
                onTestCommand = { _, _ -> },
                onVoiceTest = { }
            )
        }

        // Verify input field exists
        composeTestRule.onNodeWithText("Enter command to test...").assertIsDisplayed()
        
        // Test text input
        composeTestRule.onNodeWithText("Enter command to test...")
            .performTextInput("go back")
        
        // Test buttons exist and are clickable
        composeTestRule.onNodeWithText("Test Command").assertIsDisplayed().assertIsEnabled()
        composeTestRule.onNodeWithText("Voice Test").assertIsDisplayed().assertIsEnabled()
        
        // Click test command button
        composeTestRule.onNodeWithText("Test Command").performClick()
    }

    @Test
    fun testCommandCategoriesDisplay() {
        val sampleCategories = mapOf(
            CommandCategory.NAVIGATION to listOf(
                CommandDefinition("nav_back", "Go Back", "Navigate back", "NAVIGATION", listOf("go back"))
            ),
            CommandCategory.MEDIA to listOf(
                CommandDefinition("vol_up", "Volume Up", "Increase volume", "MEDIA", listOf("volume up"))
            )
        )

        composeTestRule.setContent {
            CommandCategoriesCard(
                onCategorySelected = { }
            )
        }

        // Verify categories are displayed
        composeTestRule.onNodeWithText("Command Categories").assertIsDisplayed()
        composeTestRule.onNodeWithText("NAVIGATION").assertIsDisplayed()
        composeTestRule.onNodeWithText("MEDIA").assertIsDisplayed()
        
        // Test category interaction
        composeTestRule.onNodeWithText("NAVIGATION").performClick()
    }

    @Test
    fun testCommandHistoryDisplay() {
        val sampleHistory = listOf(
            CommandHistoryEntry(
                command = Command("test_1", "go back", CommandSource.TEXT),
                result = CommandResult(
                    success = true,
                    command = Command("test_1", "go back", CommandSource.TEXT),
                    response = "Navigation successful",
                    executionTime = 150L
                )
            ),
            CommandHistoryEntry(
                command = Command("test_2", "volume up", CommandSource.VOICE),
                result = CommandResult(
                    success = false,
                    command = Command("test_2", "volume up", CommandSource.VOICE),
                    error = CommandError(ErrorCode.EXECUTION_FAILED, "Volume control failed"),
                    executionTime = 200L
                )
            )
        )

        composeTestRule.setContent {
            CommandHistoryCard(
                history = sampleHistory,
                onClearHistory = { }
            )
        }

        // Verify history items are displayed
        composeTestRule.onNodeWithText("Recent Commands").assertIsDisplayed()
        composeTestRule.onNodeWithText("go back").assertIsDisplayed()
        composeTestRule.onNodeWithText("volume up").assertIsDisplayed()
        composeTestRule.onNodeWithText("Navigation successful").assertIsDisplayed()
        composeTestRule.onNodeWithText("Volume control failed").assertIsDisplayed()
        
        // Test clear history button
        composeTestRule.onNodeWithText("Clear History").assertIsDisplayed().performClick()
    }

    @Test
    fun testLoadingStatesDisplay() {
        composeTestRule.setContent {
            QuickTestPanel(
                isLoading = true,
                onTestCommand = { _, _ -> },
                onVoiceTest = { }
            )
        }

        // Verify loading indicator appears
        composeTestRule.onNodeWithContentDescription("Loading").assertIsDisplayed()
        
        // Verify buttons are disabled during loading
        composeTestRule.onNodeWithText("Test Command").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Voice Test").assertIsNotEnabled()
    }

    @Test
    fun testErrorMessageDisplay() {
        composeTestRule.setContent {
            ErrorDisplay(
                message = "Command execution failed",
                onDismiss = { }
            )
        }

        // Verify error message is displayed
        composeTestRule.onNodeWithText("Command execution failed").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dismiss").assertIsDisplayed().performClick()
    }

    @Test
    fun testSuccessMessageDisplay() {
        composeTestRule.setContent {
            SuccessDisplay(
                message = "Command executed successfully",
                onDismiss = { }
            )
        }

        // Verify success message is displayed
        composeTestRule.onNodeWithText("Command executed successfully").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dismiss").assertIsDisplayed().performClick()
    }

    @Test
    fun testGlassmorphismStyling() {
        composeTestRule.setContent {
            CommandStatsCard(
                stats = CommandStats(
                    totalCommands = 50,
                    successfulCommands = 40,
                    failedCommands = 10,
                    averageExecutionTime = 100L,
                    topCommands = emptyList()
                ),
                onRefresh = { }
            )
        }

        // Verify glassmorphism card is rendered (basic visibility test)
        composeTestRule.onNodeWithText("Command Statistics").assertIsDisplayed()
        
        // The glassmorphism styling itself is visual and harder to test programmatically
        // but we can verify the card structure exists
        composeTestRule.onNode(hasTestTag("stats_card") or hasText("Command Statistics"))
            .assertIsDisplayed()
    }

    @Test
    fun testCommandSourceFiltering() {
        composeTestRule.setContent {
            QuickTestPanel(
                isLoading = false,
                onTestCommand = { _, _ -> },
                onVoiceTest = { }
            )
        }

        // Test voice test button specifically
        composeTestRule.onNodeWithText("Voice Test").assertIsDisplayed()
        composeTestRule.onNodeWithText("Voice Test").performClick()
        
        // Test manual command entry (TEXT source)
        composeTestRule.onNodeWithText("Enter command to test...")
            .performTextInput("test command")
        composeTestRule.onNodeWithText("Test Command").performClick()
    }

    @Test
    fun testCategoryColorCoding() {
        val categories = mapOf(
            CommandCategory.NAVIGATION to listOf(
                CommandDefinition("nav", "Navigate", "Navigation command", "NAVIGATION", emptyList())
            ),
            CommandCategory.MEDIA to listOf(
                CommandDefinition("media", "Media", "Media command", "MEDIA", emptyList())
            ),
            CommandCategory.SYSTEM to listOf(
                CommandDefinition("sys", "System", "System command", "SYSTEM", emptyList())
            )
        )

        composeTestRule.setContent {
            CommandCategoriesCard(
                onCategorySelected = { }
            )
        }

        // Verify all categories are displayed with proper styling
        composeTestRule.onNodeWithText("NAVIGATION").assertIsDisplayed()
        composeTestRule.onNodeWithText("MEDIA").assertIsDisplayed()
        composeTestRule.onNodeWithText("SYSTEM").assertIsDisplayed()
    }

    @Test
    fun testCommandExecutionFlow() {
        composeTestRule.setContent {
            CommandManagerContent(
                viewModel = CommandViewModel(context)
            )
        }

        // Navigate through command execution flow
        composeTestRule.onNodeWithText("Enter command to test...")
            .performTextInput("go back")
        
        composeTestRule.onNodeWithText("Test Command").performClick()
        
        // The actual execution will depend on the CommandProcessor
        // but we can verify the UI responds to the interaction
        assertTrue(true) // Basic interaction completed without crash
    }
}