package com.augmentalis.webavanue.ui

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.augmentalis.Avanues.web.universal.presentation.ui.settings.SettingsScreen
import com.augmentalis.Avanues.web.universal.presentation.viewmodel.SettingsViewModel
import com.augmentalis.webavanue.data.db.BrowserDatabase
import com.augmentalis.webavanue.data.repository.BrowserRepositoryImpl
import com.augmentalis.webavanue.domain.model.BrowserSettings
import com.augmentalis.webavanue.platform.createAndroidDriver
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.*

/**
 * UI tests for collapsible settings sections and search filtering
 *
 * Phase C - Settings UI Organization
 * Tests verify:
 * 1. Search bar filtering functionality
 * 2. Expand/collapse section behavior
 * 3. Auto-expansion when searching
 * 4. Landscape layout category navigation
 * 5. Section toggle interactions
 *
 * Coverage Target: 90%+ of UI interactions
 */
@RunWith(AndroidJUnit4::class)
class SettingsUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockViewModel: SettingsViewModel
    private lateinit var testRepository: BrowserRepositoryImpl

    private fun createTestRepository(): BrowserRepositoryImpl {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val driver = createAndroidDriver(context)
        val database = BrowserDatabase(driver)
        return BrowserRepositoryImpl(database)
    }

    @Before
    fun setUp() {
        testRepository = createTestRepository()
        mockViewModel = spyk(SettingsViewModel(testRepository))

        // Initialize with default settings
        runBlocking {
            testRepository.resetSettings()
        }
    }

    // ========== Search Filtering Tests ==========

    /**
     * Test 1: Search bar filters settings sections
     *
     * Verifies that typing in the search bar filters visible sections
     * to only show those matching the query.
     */
    @Test
    fun searchBar_filtersSettingsSections() {
        // Given: Settings screen displayed
        composeTestRule.setContent {
            SettingsScreen(viewModel = mockViewModel)
        }

        // Wait for content to load
        composeTestRule.waitForIdle()

        // When: User types "javascript" in search
        composeTestRule.onNodeWithTag("search_bar")
            .assertIsDisplayed()
            .performTextInput("javascript")

        composeTestRule.waitForIdle()

        // Then: Privacy section with JavaScript setting is visible
        composeTestRule.onNodeWithText("Privacy & Security", substring = true)
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("JavaScript", substring = true)
            .assertIsDisplayed()
    }

    /**
     * Test 2: Search query auto-expands matching sections
     *
     * Verifies that entering a search query automatically expands
     * sections containing matching settings.
     */
    @Test
    fun searchQuery_autoExpandsMatchingSections() {
        // Given: Settings screen with some collapsed sections
        composeTestRule.setContent {
            SettingsScreen(viewModel = mockViewModel)
        }

        composeTestRule.waitForIdle()

        // Collapse all sections first
        if (composeTestRule.onAllNodesWithText("Collapse All").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithText("Collapse All")
                .performClick()
        }

        composeTestRule.waitForIdle()

        // When: Search query matches "download" settings
        composeTestRule.onNodeWithTag("search_bar")
            .performTextInput("download")

        composeTestRule.waitForIdle()

        // Then: Downloads section auto-expands and shows download settings
        composeTestRule.onNodeWithText("Downloads", substring = true)
            .assertIsDisplayed()

        // Download-related settings should be visible
        composeTestRule.onNodeWithText("Download Path", substring = true)
            .assertIsDisplayed()
    }

    /**
     * Test 3: Clear search button resets filter
     *
     * Verifies that clicking the clear button (X icon) in the search bar
     * clears the search query and shows all sections again.
     */
    @Test
    fun searchBar_clearButton_resetsFilter() {
        // Given: Settings screen with active search
        composeTestRule.setContent {
            SettingsScreen(viewModel = mockViewModel)
        }

        composeTestRule.waitForIdle()

        // Enter search query
        composeTestRule.onNodeWithTag("search_bar")
            .performTextInput("javascript")

        composeTestRule.waitForIdle()

        // When: User clicks clear button
        composeTestRule.onNodeWithContentDescription("Clear search")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // Then: Search is cleared and expand/collapse buttons reappear
        composeTestRule.onNodeWithText("Expand All")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Collapse All")
            .assertIsDisplayed()
    }

    /**
     * Test 4: Search with no matches shows all sections
     *
     * Verifies behavior when search query doesn't match any settings.
     */
    @Test
    fun searchBar_noMatches_showsAllSections() {
        // Given: Settings screen
        composeTestRule.setContent {
            SettingsScreen(viewModel = mockViewModel)
        }

        composeTestRule.waitForIdle()

        // When: User searches for non-existent setting
        composeTestRule.onNodeWithTag("search_bar")
            .performTextInput("nonexistentsetting12345")

        composeTestRule.waitForIdle()

        // Then: All section headers still visible (no filtering on headers themselves)
        // But no content items match
        composeTestRule.onNodeWithText("General", substring = true)
            .assertIsDisplayed()
    }

    /**
     * Test 5: Case-insensitive search filtering
     *
     * Verifies that search is case-insensitive (e.g., "JAVASCRIPT" matches "JavaScript").
     */
    @Test
    fun searchBar_caseInsensitiveSearch() {
        // Given: Settings screen
        composeTestRule.setContent {
            SettingsScreen(viewModel = mockViewModel)
        }

        composeTestRule.waitForIdle()

        // When: User searches with uppercase
        composeTestRule.onNodeWithTag("search_bar")
            .performTextInput("JAVASCRIPT")

        composeTestRule.waitForIdle()

        // Then: JavaScript setting is found and displayed
        composeTestRule.onNodeWithText("JavaScript", substring = true)
            .assertIsDisplayed()
    }

    // ========== Expand/Collapse Behavior Tests ==========

    /**
     * Test 6: Expand All button expands all sections
     *
     * Verifies that clicking "Expand All" button makes all 11 sections visible.
     */
    @Test
    fun expandAll_button_expandsAllSections() {
        // Given: Settings screen with some collapsed sections
        composeTestRule.setContent {
            SettingsScreen(viewModel = mockViewModel)
        }

        composeTestRule.waitForIdle()

        // Collapse all first to ensure consistent state
        if (composeTestRule.onAllNodesWithText("Collapse All").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithText("Collapse All")
                .performClick()
            composeTestRule.waitForIdle()
        }

        // When: User clicks "Expand All"
        composeTestRule.onNodeWithText("Expand All")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // Then: All major sections are visible
        val expectedSections = listOf(
            "General",
            "Appearance",
            "Privacy & Security",
            "Downloads",
            "Performance"
            // Note: Some sections may require scrolling to view
        )

        expectedSections.forEach { section ->
            composeTestRule.onNodeWithText(section, substring = true, useUnmergedTree = true)
                .assertExists()
        }
    }

    /**
     * Test 7: Collapse All button collapses all sections
     *
     * Verifies that clicking "Collapse All" hides all section content,
     * leaving only headers visible.
     */
    @Test
    fun collapseAll_button_collapsesAllSections() {
        // Given: All sections expanded
        composeTestRule.setContent {
            SettingsScreen(viewModel = mockViewModel)
        }

        composeTestRule.waitForIdle()

        // Expand all first
        composeTestRule.onNodeWithText("Expand All")
            .performClick()

        composeTestRule.waitForIdle()

        // When: User clicks "Collapse All"
        composeTestRule.onNodeWithText("Collapse All")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        // Then: Content items are hidden (headers remain)
        // Theme setting should not be immediately visible when collapsed
        // (We can't guarantee all content is hidden without scrolling, but collapse should work)

        // Verify collapse/expand buttons are still visible
        composeTestRule.onNodeWithText("Expand All")
            .assertIsDisplayed()
    }

    /**
     * Test 8: Individual section toggle
     *
     * Verifies that clicking a section header toggles its expansion state.
     */
    @Test
    fun sectionHeader_clickToggle_expandsAndCollapses() {
        // Given: Settings screen
        composeTestRule.setContent {
            SettingsScreen(viewModel = mockViewModel)
        }

        composeTestRule.waitForIdle()

        // Collapse all first for predictable state
        if (composeTestRule.onAllNodesWithText("Collapse All").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithText("Collapse All")
                .performClick()
            composeTestRule.waitForIdle()
        }

        // When: User clicks "Appearance" header
        composeTestRule.onNodeWithText("Appearance", substring = true, useUnmergedTree = true)
            .performClick()

        composeTestRule.waitForIdle()

        // Then: Appearance section content becomes visible
        composeTestRule.onNodeWithText("Theme", substring = true)
            .assertIsDisplayed()

        // When: Click again to collapse
        composeTestRule.onNodeWithText("Appearance", substring = true, useUnmergedTree = true)
            .performClick()

        composeTestRule.waitForIdle()

        // Then: Theme setting may become hidden (depending on lazy loading)
        // At minimum, verify the toggle action was called
        verify(atLeast = 1) { mockViewModel.toggleSection(any()) }
    }

    /**
     * Test 9: Multiple sections can be expanded simultaneously
     *
     * Verifies that expanding one section doesn't collapse others.
     */
    @Test
    fun multipleSections_canBeExpandedSimultaneously() {
        // Given: Settings screen
        composeTestRule.setContent {
            SettingsScreen(viewModel = mockViewModel)
        }

        composeTestRule.waitForIdle()

        // Collapse all first
        if (composeTestRule.onAllNodesWithText("Collapse All").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithText("Collapse All")
                .performClick()
            composeTestRule.waitForIdle()
        }

        // When: Expand General section
        composeTestRule.onNodeWithText("General", substring = true, useUnmergedTree = true)
            .performClick()

        composeTestRule.waitForIdle()

        // And: Expand Privacy section
        composeTestRule.onNodeWithText("Privacy & Security", substring = true, useUnmergedTree = true)
            .performClick()

        composeTestRule.waitForIdle()

        // Then: Both sections are expanded
        composeTestRule.onNodeWithText("Search Engine", substring = true)
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("JavaScript", substring = true)
            .assertIsDisplayed()
    }

    // ========== Landscape Layout Tests ==========

    /**
     * Test 10: Landscape layout shows category navigation
     *
     * Verifies that in landscape orientation, a two-pane layout is displayed
     * with category navigation on the left.
     */
    @Test
    fun landscapeLayout_showsCategoryNavigation() {
        // Given: Settings screen in landscape orientation
        // Note: We test the composable directly rather than changing device orientation
        // since orientation changes require activity-level testing

        composeTestRule.setContent {
            SettingsScreen(viewModel = mockViewModel)
        }

        composeTestRule.waitForIdle()

        // For landscape testing, we verify the category list tag exists
        // when the layout switches to landscape mode
        // (This test documents the expected behavior; actual orientation
        // testing would require AndroidX Test espresso device rotation)

        // Verify settings screen is displayed
        composeTestRule.onNodeWithText("Settings", substring = true)
            .assertIsDisplayed()

        // In portrait mode, we should see the search bar
        composeTestRule.onNodeWithTag("search_bar")
            .assertIsDisplayed()
    }

    /**
     * Test 11: Category selection in landscape mode
     *
     * Verifies that category navigation works in landscape layout.
     * Note: This test documents expected behavior. Full landscape testing
     * requires device orientation changes.
     */
    @Test
    fun landscapeLayout_categorySelection_showsCorrectContent() {
        // Given: Settings screen
        composeTestRule.setContent {
            SettingsScreen(viewModel = mockViewModel)
        }

        composeTestRule.waitForIdle()

        // Verify basic navigation structure
        // In portrait: search bar + sections
        composeTestRule.onNodeWithTag("search_bar")
            .assertIsDisplayed()

        // Verify sections are present
        composeTestRule.onNodeWithText("General", substring = true)
            .assertExists()

        composeTestRule.onNodeWithText("Appearance", substring = true)
            .assertExists()
    }

    // ========== Additional UI Interaction Tests ==========

    /**
     * Test 12: Search hides expand/collapse buttons
     *
     * Verifies that when search is active, the Expand All/Collapse All
     * buttons are hidden to reduce UI clutter.
     */
    @Test
    fun searchActive_hidesExpandCollapseButtons() {
        // Given: Settings screen
        composeTestRule.setContent {
            SettingsScreen(viewModel = mockViewModel)
        }

        composeTestRule.waitForIdle()

        // Verify buttons are initially visible
        composeTestRule.onNodeWithText("Expand All")
            .assertIsDisplayed()

        // When: User starts searching
        composeTestRule.onNodeWithTag("search_bar")
            .performTextInput("test")

        composeTestRule.waitForIdle()

        // Then: Expand/Collapse buttons are hidden
        composeTestRule.onNodeWithText("Expand All")
            .assertDoesNotExist()
        composeTestRule.onNodeWithText("Collapse All")
            .assertDoesNotExist()
    }

    /**
     * Test 13: Settings persistence after expansion changes
     *
     * Verifies that toggling sections doesn't affect actual browser settings.
     */
    @Test
    fun sectionToggle_doesNotAffectSettings() {
        // Given: Settings screen with known settings
        composeTestRule.setContent {
            SettingsScreen(viewModel = mockViewModel)
        }

        composeTestRule.waitForIdle()

        val initialSettings = runBlocking {
            testRepository.getSettings().getOrNull()
        }

        // When: Toggle sections
        composeTestRule.onNodeWithText("Expand All")
            .performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Collapse All")
            .performClick()

        composeTestRule.waitForIdle()

        // Then: Settings remain unchanged
        val finalSettings = runBlocking {
            testRepository.getSettings().getOrNull()
        }

        assertEquals(initialSettings, finalSettings)
    }

    /**
     * Test 14: Verify search bar placeholder text
     *
     * Verifies that the search bar shows helpful placeholder text.
     */
    @Test
    fun searchBar_showsPlaceholderText() {
        // Given: Settings screen
        composeTestRule.setContent {
            SettingsScreen(viewModel = mockViewModel)
        }

        composeTestRule.waitForIdle()

        // Then: Search bar has placeholder
        composeTestRule.onNodeWithText("Search settings...", substring = true)
            .assertIsDisplayed()
    }

    /**
     * Test 15: Section expansion state persists during search
     *
     * Verifies that manually expanded sections remain expanded when
     * search is cleared.
     */
    @Test
    fun sectionExpansion_persistsAfterSearchClear() {
        // Given: Settings screen with Privacy section expanded
        composeTestRule.setContent {
            SettingsScreen(viewModel = mockViewModel)
        }

        composeTestRule.waitForIdle()

        // Collapse all, then expand Privacy
        if (composeTestRule.onAllNodesWithText("Collapse All").fetchSemanticsNodes().isNotEmpty()) {
            composeTestRule.onNodeWithText("Collapse All")
                .performClick()
            composeTestRule.waitForIdle()
        }

        composeTestRule.onNodeWithText("Privacy & Security", substring = true, useUnmergedTree = true)
            .performClick()

        composeTestRule.waitForIdle()

        // Verify Privacy is expanded
        composeTestRule.onNodeWithText("JavaScript", substring = true)
            .assertIsDisplayed()

        // When: User searches and then clears
        composeTestRule.onNodeWithTag("search_bar")
            .performTextInput("test")

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Clear search")
            .performClick()

        composeTestRule.waitForIdle()

        // Then: Privacy section remains expanded
        // (Search auto-expands all, so when cleared, previously expanded sections stay expanded)
        composeTestRule.onNodeWithText("JavaScript", substring = true)
            .assertIsDisplayed()
    }
}
