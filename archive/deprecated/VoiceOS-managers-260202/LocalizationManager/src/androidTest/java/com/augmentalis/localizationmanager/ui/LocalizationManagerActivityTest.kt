/**
 * LocalizationManagerActivityTest.kt - UI integration tests
 * 
 * Tests UI interactions, language selection, and visual components
 * 
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 */
package com.augmentalis.localizationmanager.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalizationManagerActivityTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Before
    fun setup() {
        // Set up the UI for testing
        composeTestRule.setContent {
            LocalizationManagerScreen()
        }
    }
    
    @Test
    fun testHeaderSectionDisplayed() {
        // Verify header elements
        composeTestRule
            .onNodeWithText("Localization Manager")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("42+ Languages")
            .assertIsDisplayed()
    }
    
    @Test
    fun testCurrentLanguageCardDisplayed() {
        // Verify current language card
        composeTestRule
            .onNodeWithText("Current Language")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("English")
            .assertIsDisplayed()
        
        // Verify change button
        composeTestRule
            .onNodeWithText("Change Language")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
    
    @Test
    fun testLanguageStatisticsVisible() {
        // Verify statistics section
        composeTestRule
            .onNodeWithText("Language Statistics")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Total Languages")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Downloaded")
            .assertIsDisplayed()
    }
    
    @Test
    fun testDownloadedLanguagesSection() {
        // Verify downloaded languages section
        composeTestRule
            .onNodeWithText("Downloaded Languages")
            .assertIsDisplayed()
        
        // Check for language items
        composeTestRule
            .onAllNodesWithTag("language_item")
            .onFirst()
            .assertIsDisplayed()
    }
    
    @Test
    fun testAvailableLanguagesSection() {
        // Scroll to available languages
        composeTestRule
            .onNodeWithTag("main_content")
            .performScrollToNode(hasText("Available Languages"))
        
        composeTestRule
            .onNodeWithText("Available Languages")
            .assertIsDisplayed()
        
        // Verify search field
        composeTestRule
            .onNodeWithText("Search languages...")
            .assertIsDisplayed()
    }
    
    @Test
    fun testLanguageSearchFunctionality() {
        // Scroll to search field
        composeTestRule
            .onNodeWithTag("main_content")
            .performScrollToNode(hasText("Search languages..."))
        
        // Type in search field
        composeTestRule
            .onNodeWithText("Search languages...")
            .performTextInput("Spanish")
        
        // Verify filtered results
        composeTestRule
            .onNodeWithText("Spanish")
            .assertIsDisplayed()
    }
    
    @Test
    fun testLanguageSelectionDialog() {
        // Click change language button
        composeTestRule
            .onNodeWithText("Change Language")
            .performClick()
        
        // Verify dialog appears
        composeTestRule
            .onNodeWithText("Select Language")
            .assertIsDisplayed()
        
        // Verify language options
        composeTestRule
            .onNodeWithText("Spanish")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("French")
            .assertIsDisplayed()
    }
    
    @Test
    fun testDownloadLanguageButton() {
        // Scroll to available languages
        composeTestRule
            .onNodeWithTag("main_content")
            .performScrollToNode(hasText("Available Languages"))
        
        // Find download button
        composeTestRule
            .onAllNodesWithContentDescription("Download language")
            .onFirst()
            .assertIsDisplayed()
            .assertHasClickAction()
    }
    
    @Test
    fun testTranslationTestDialog() {
        // Open translation dialog
        composeTestRule
            .onNodeWithText("Test Translation")
            .performClick()
        
        // Verify dialog components
        composeTestRule
            .onNodeWithText("Translation Test")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Enter text to translate")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Translate")
            .assertIsDisplayed()
    }
    
    @Test
    fun testQuickActionsButtons() {
        // Verify quick action buttons
        composeTestRule
            .onNodeWithText("Test Translation")
            .assertIsDisplayed()
            .assertHasClickAction()
        
        composeTestRule
            .onNodeWithContentDescription("Refresh languages")
            .assertIsDisplayed()
            .assertHasClickAction()
    }
    
    @Test
    fun testGlassmorphismEffects() {
        // Verify glassmorphism cards have proper styling
        composeTestRule
            .onAllNodesWithTag("glass_card")
            .onFirst()
            .assertIsDisplayed()
    }
    
    @Test
    fun testLanguageRegionGrouping() {
        // Scroll to available languages
        composeTestRule
            .onNodeWithTag("main_content")
            .performScrollToNode(hasText("Available Languages"))
        
        // Verify region chips
        composeTestRule
            .onNodeWithText("All")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Europe")
            .assertIsDisplayed()
        
        composeTestRule
            .onNodeWithText("Asia")
            .assertIsDisplayed()
    }
}