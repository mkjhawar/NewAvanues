package com.augmentalis.webavanue.ui.viewmodel

import com.augmentalis.webavanue.FakeBrowserRepository
import com.augmentalis.webavanue.domain.model.BrowserSettings
import com.augmentalis.webavanue.domain.model.Tab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlin.test.*

/**
 * TabViewModelTest - Comprehensive unit tests for TabViewModel
 *
 * Tests cover:
 * - Tab creation with various URL formats
 * - Tab deletion and switching
 * - NewTabPage setting behavior
 * - URL navigation and normalization
 * - Error handling
 * - Settings integration
 *
 * Coverage Target: 80%
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TabViewModelTest {

    private lateinit var repository: FakeBrowserRepository
    private lateinit var viewModel: TabViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)
        repository = FakeBrowserRepository()
        viewModel = TabViewModel(repository)
        // Allow init block to complete
        advanceUntilIdle()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        viewModel.onCleared()
    }

    // ========== Test 1: Create tab with valid URL ==========
    @Test
    fun `createTab with valid URL succeeds`() = runTest(testDispatcher) {
        // When
        viewModel.createTab(url = "https://example.com", title = "Example")
        advanceUntilIdle()

        // Then
        val tabs = viewModel.tabs.first()
        val activeTab = viewModel.activeTab.first()

        assertEquals(1, tabs.size)
        assertNotNull(activeTab)
        assertEquals("https://example.com", activeTab?.tab?.url)
        assertEquals("Example", activeTab?.tab?.title)
    }

    // ========== Test 2: Create tab with blank URL uses newTabPage setting - BLANK ==========
    @Test
    fun `createTab with blank URL uses BLANK newTabPage setting`() = runTest(testDispatcher) {
        // Given - Set newTabPage to BLANK
        val settingsResult = repository.getSettings()
        assertTrue(settingsResult.isSuccess)
        val settings = settingsResult.getOrNull()!!
        repository.setSettings(settings.copy(newTabPage = BrowserSettings.NewTabPage.BLANK))
        advanceUntilIdle()

        // When
        viewModel.createTab(url = "")
        advanceUntilIdle()

        // Then
        val activeTab = viewModel.activeTab.first()
        assertEquals("about:blank", activeTab?.tab?.url)
    }

    // ========== Test 3: Create tab respects TOP_SITES newTabPage setting ==========
    @Test
    fun `createTab respects TOP_SITES newTabPage setting`() = runTest(testDispatcher) {
        // Given - Set newTabPage to TOP_SITES
        val settingsResult = repository.getSettings()
        assertTrue(settingsResult.isSuccess)
        val settings = settingsResult.getOrNull()!!
        repository.setSettings(settings.copy(newTabPage = BrowserSettings.NewTabPage.TOP_SITES))
        advanceUntilIdle()

        // When
        viewModel.createTab(url = "")
        advanceUntilIdle()

        // Then
        val activeTab = viewModel.activeTab.first()
        assertEquals(NewTabUrls.TOP_SITES, activeTab?.tab?.url)
    }

    // ========== Test 4: Create tab respects HOME_PAGE newTabPage setting ==========
    @Test
    fun `createTab respects HOME_PAGE newTabPage setting`() = runTest(testDispatcher) {
        // Given - Set newTabPage to HOME_PAGE with custom homepage
        val settingsResult = repository.getSettings()
        assertTrue(settingsResult.isSuccess)
        val settings = settingsResult.getOrNull()!!
        repository.setSettings(settings.copy(
            newTabPage = BrowserSettings.NewTabPage.HOME_PAGE,
            homePage = "https://custom-homepage.com"
        ))
        advanceUntilIdle()

        // When
        viewModel.createTab(url = "")
        advanceUntilIdle()

        // Then
        val activeTab = viewModel.activeTab.first()
        assertEquals("https://custom-homepage.com", activeTab?.tab?.url)
    }

    // ========== Test 5: Create tab respects MOST_VISITED newTabPage setting ==========
    @Test
    fun `createTab respects MOST_VISITED newTabPage setting`() = runTest(testDispatcher) {
        // Given - Set newTabPage to MOST_VISITED
        val settingsResult = repository.getSettings()
        assertTrue(settingsResult.isSuccess)
        val settings = settingsResult.getOrNull()!!
        repository.setSettings(settings.copy(newTabPage = BrowserSettings.NewTabPage.MOST_VISITED))
        advanceUntilIdle()

        // When
        viewModel.createTab(url = "")
        advanceUntilIdle()

        // Then
        val activeTab = viewModel.activeTab.first()
        assertEquals(NewTabUrls.MOST_VISITED, activeTab?.tab?.url)
    }

    // ========== Test 6: Create tab respects SPEED_DIAL newTabPage setting ==========
    @Test
    fun `createTab respects SPEED_DIAL newTabPage setting`() = runTest(testDispatcher) {
        // Given - Set newTabPage to SPEED_DIAL
        val settingsResult = repository.getSettings()
        assertTrue(settingsResult.isSuccess)
        val settings = settingsResult.getOrNull()!!
        repository.setSettings(settings.copy(newTabPage = BrowserSettings.NewTabPage.SPEED_DIAL))
        advanceUntilIdle()

        // When
        viewModel.createTab(url = "")
        advanceUntilIdle()

        // Then
        val activeTab = viewModel.activeTab.first()
        assertEquals(NewTabUrls.SPEED_DIAL, activeTab?.tab?.url)
    }

    // ========== Test 7: Create tab respects NEWS_FEED newTabPage setting ==========
    @Test
    fun `createTab respects NEWS_FEED newTabPage setting`() = runTest(testDispatcher) {
        // Given - Set newTabPage to NEWS_FEED
        val settingsResult = repository.getSettings()
        assertTrue(settingsResult.isSuccess)
        val settings = settingsResult.getOrNull()!!
        repository.setSettings(settings.copy(newTabPage = BrowserSettings.NewTabPage.NEWS_FEED))
        advanceUntilIdle()

        // When
        viewModel.createTab(url = "")
        advanceUntilIdle()

        // Then
        val activeTab = viewModel.activeTab.first()
        assertEquals(NewTabUrls.NEWS_FEED, activeTab?.tab?.url)
    }

    // ========== Test 8: Close tab removes tab from list ==========
    @Test
    fun `closeTab removes tab from list`() = runTest(testDispatcher) {
        // Given
        viewModel.createTab(url = "https://example.com", title = "Example")
        advanceUntilIdle()
        val tabId = viewModel.activeTab.first()?.tab?.id!!

        // When
        viewModel.closeTab(tabId)
        advanceUntilIdle()

        // Then
        val tabs = viewModel.tabs.first()
        assertEquals(0, tabs.size)
    }

    // ========== Test 9: Close tab with invalid ID does nothing ==========
    @Test
    fun `closeTab with invalid ID does nothing`() = runTest(testDispatcher) {
        // Given
        viewModel.createTab(url = "https://example.com", title = "Example")
        advanceUntilIdle()
        val initialTabCount = viewModel.tabs.first().size

        // When
        viewModel.closeTab("non-existent-id")
        advanceUntilIdle()

        // Then
        val tabs = viewModel.tabs.first()
        assertEquals(initialTabCount, tabs.size)
    }

    // ========== Test 10: Settings flow updates when repository changes ==========
    @Test
    fun `settings flow updates when repository changes`() = runTest(testDispatcher) {
        // Given
        val settingsResult = repository.getSettings()
        assertTrue(settingsResult.isSuccess)
        val settings = settingsResult.getOrNull()!!

        // When
        val newSettings = settings.copy(enableJavaScript = false)
        repository.setSettings(newSettings)
        advanceUntilIdle()

        // Then
        val updatedSettings = viewModel.settings.first()
        assertNotNull(updatedSettings)
        assertFalse(updatedSettings.enableJavaScript)
    }

    // ========== Test 11: Switch tab updates active tab ==========
    @Test
    fun `switchTab updates active tab`() = runTest(testDispatcher) {
        // Given
        viewModel.createTab(url = "https://first.com", title = "First")
        advanceUntilIdle()
        val firstTabId = viewModel.activeTab.first()?.tab?.id!!

        viewModel.createTab(url = "https://second.com", title = "Second")
        advanceUntilIdle()

        // When
        viewModel.switchTab(firstTabId)
        advanceUntilIdle()

        // Then
        val activeTab = viewModel.activeTab.first()
        assertEquals(firstTabId, activeTab?.tab?.id)
        assertEquals("https://first.com", activeTab?.tab?.url)
    }

    // ========== Test 12: Update tab URL updates existing tab ==========
    @Test
    fun `updateTabUrl updates existing tab`() = runTest(testDispatcher) {
        // Given
        viewModel.createTab(url = "https://example.com", title = "Example")
        advanceUntilIdle()
        val tabId = viewModel.activeTab.first()?.tab?.id!!

        // When
        viewModel.updateTabUrl(tabId, "https://updated.com")
        advanceUntilIdle()

        // Then
        val activeTab = viewModel.activeTab.first()
        assertEquals("https://updated.com", activeTab?.tab?.url)
    }

    // ========== Test 13: Multiple tabs maintain independent state ==========
    @Test
    fun `multiple tabs maintain independent state`() = runTest(testDispatcher) {
        // Given
        viewModel.createTab(url = "https://first.com", title = "First")
        advanceUntilIdle()
        val firstTabId = viewModel.activeTab.first()?.tab?.id!!

        viewModel.createTab(url = "https://second.com", title = "Second")
        advanceUntilIdle()
        val secondTabId = viewModel.activeTab.first()?.tab?.id!!

        viewModel.createTab(url = "https://third.com", title = "Third")
        advanceUntilIdle()

        // When - Update different properties on different tabs
        viewModel.updateTabUrl(firstTabId, "https://first-updated.com")
        viewModel.updateTabTitle(secondTabId, "Second Updated")
        advanceUntilIdle()

        // Then - Verify each tab has independent state
        val tabs = viewModel.tabs.first()
        assertEquals(3, tabs.size)

        val firstTab = tabs.find { it.tab.id == firstTabId }
        assertEquals("https://first-updated.com", firstTab?.tab?.url)

        val secondTab = tabs.find { it.tab.id == secondTabId }
        assertEquals("Second Updated", secondTab?.tab?.title)

        val thirdTab = tabs.find { it.tab.id != firstTabId && it.tab.id != secondTabId }
        assertEquals("https://third.com", thirdTab?.tab?.url)
        assertEquals("Third", thirdTab?.tab?.title)
    }

    // ========== Test 14: Tab loading state updates correctly ==========
    @Test
    fun `updateTabLoading updates tab loading state`() = runTest(testDispatcher) {
        // Given
        viewModel.createTab(url = "https://example.com", title = "Example")
        advanceUntilIdle()
        val tabId = viewModel.activeTab.first()?.tab?.id!!

        // When
        viewModel.updateTabLoading(tabId, true)
        advanceUntilIdle()

        // Then
        val activeTab = viewModel.activeTab.first()
        assertTrue(activeTab?.isLoading ?: false)

        // When - Stop loading
        viewModel.updateTabLoading(tabId, false)
        advanceUntilIdle()

        // Then
        val activeTab2 = viewModel.activeTab.first()
        assertFalse(activeTab2?.isLoading ?: true)
    }

    // ========== Test 15: Tab navigation state updates correctly ==========
    @Test
    fun `updateTabNavigation updates navigation state`() = runTest(testDispatcher) {
        // Given
        viewModel.createTab(url = "https://example.com", title = "Example")
        advanceUntilIdle()
        val tabId = viewModel.activeTab.first()?.tab?.id!!

        // When
        viewModel.updateTabNavigation(tabId, canGoBack = true, canGoForward = false)
        advanceUntilIdle()

        // Then
        val activeTab = viewModel.activeTab.first()
        assertTrue(activeTab?.canGoBack ?: false)
        assertFalse(activeTab?.canGoForward ?: true)

        // When - Update navigation again
        viewModel.updateTabNavigation(tabId, canGoBack = false, canGoForward = true)
        advanceUntilIdle()

        // Then
        val activeTab2 = viewModel.activeTab.first()
        assertFalse(activeTab2?.canGoBack ?: true)
        assertTrue(activeTab2?.canGoForward ?: false)
    }
}
