/**
 * UUIDManagerActivityTest.kt - UI integration tests
 * 
 * Tests UI interactions, element registration, and navigation
 * 
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 */
package com.augmentalis.uuidmanager.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UUIDManagerActivityTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Before
    fun setup() {
        // Set up the UI for testing
        composeTestRule.setContent {
            UUIDManagerTheme {
                UUIDManagerScreen()
            }
        }
    }
    
    @Test
    fun testHeaderDisplayed() {
        // Verify header elements
        composeTestRule
            .onNodeWithText("UUID Manager")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Universal Unique Identifier System")
            .assertIsDisplayed()
    }
    
    @Test
    fun testStatisticsCardVisible() {
        // Verify statistics card
        composeTestRule
            .onNodeWithTag("statistics_card")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Registry Statistics")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Total")
            .assertIsDisplayed()
    }
    
    @Test
    fun testQuickActionsAvailable() {
        // Verify quick actions
        composeTestRule
            .onNodeWithText("Quick Actions")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Generate")
            .assertIsDisplayed()
            .assertHasClickAction()
        
        composeTestRule
            .onNodeWithText("Test Nav")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
    
    @Test
    fun testSearchBarFunctionality() {
        // Find and interact with search bar
        composeTestRule
            .onNodeWithText("Search elements...")
            .assertIsDisplayed()
            .performClick()
            .performTextInput("button")
        
        // Verify search query is entered
        composeTestRule
            .onNodeWithText("button")
            .assertIsDisplayed()
    }
    
    @Test
    fun testFilterChipsDisplayed() {
        // Verify filter chips
        composeTestRule
            .onNodeWithText("All")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Button")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Text")
            .assertIsDisplayed()
    }
    
    @Test
    fun testElementCardsDisplayed() {
        // Verify element cards are shown
        composeTestRule
            .onNodeWithText("Registered Elements")
            .assertIsDisplayed()
        
        // Check for at least one element card
        composeTestRule
            .onAllNodesWithTag("element_card")
            .onFirst()
            .assertIsDisplayed()
    }
    
    @Test
    fun testRegisterButtonFunctionality() {
        // Click the register FAB
        composeTestRule
            .onNodeWithText("Register")
            .assertIsDisplayed()
            .performClick()
        
        // Verify dialog appears
        composeTestRule
            .onNodeWithText("Register New Element")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Element Name")
            .assertIsDisplayed()
    }
    
    @Test
    fun testVoiceCommandButtonFunctionality() {
        // Click the voice command FAB
        composeTestRule
            .onNodeWithContentDescription("Voice Command")
            .assertIsDisplayed()
            .performClick()
        
        // Verify dialog appears
        composeTestRule
            .onNodeWithText("Voice Command Test")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Enter voice command")
            .assertIsDisplayed()
    }
    
    @Test
    fun testElementSelection() {
        // Click on an element card
        composeTestRule
            .onAllNodesWithTag("element_card")
            .onFirst()
            .performClick()
        
        // Verify selected element card appears
        composeTestRule
            .onNodeWithText("Selected Element")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Spatial Navigation")
            .assertIsDisplayed()
    }
    
    @Test
    fun testNavigationPadButtons() {
        // Select an element first
        composeTestRule
            .onAllNodesWithTag("element_card")
            .onFirst()
            .performClick()
        
        // Verify navigation buttons
        composeTestRule
            .onNodeWithContentDescription("Up")
            .assertIsDisplayed()
            .assertHasClickAction()
        
        composeTestRule
            .onNodeWithContentDescription("Down")
            .assertIsDisplayed()
            .assertHasClickAction()
        
        composeTestRule
            .onNodeWithContentDescription("Left")
            .assertIsDisplayed()
            .assertHasClickAction()
        
        composeTestRule
            .onNodeWithContentDescription("Right")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
    
    @Test
    fun testCommandHistoryDisplay() {
        // Process a command first
        composeTestRule
            .onNodeWithContentDescription("Voice Command")
            .performClick()
        
        composeTestRule
            .onNodeWithText("Enter voice command")
            .performTextInput("test command")
        
        composeTestRule
            .onNodeWithText("Execute")
            .performClick()
        
        // Scroll to command history if needed
        composeTestRule
            .onNodeWithTag("main_content")
            .performScrollToNode(hasText("Command History"))
        
        // Verify command history section
        composeTestRule
            .onNodeWithText("Command History")
            .assertIsDisplayed()
    }
    
    @Test
    fun testExportFunctionality() {
        // Click export button
        composeTestRule
            .onNodeWithText("Export")
            .assertIsDisplayed()
            .performClick()
        
        // Verify export dialog
        composeTestRule
            .onNodeWithText("Export Registry")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("UUID Registry Export")
            .assertIsDisplayed()
    }
}