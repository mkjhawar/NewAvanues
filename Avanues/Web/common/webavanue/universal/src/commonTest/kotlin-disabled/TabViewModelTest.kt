package com.augmentalis.Avanues.web.universal.presentation.viewmodel

import com.augmentalis.webavanue.domain.model.Tab
import com.augmentalis.Avanues.web.universal.FakeBrowserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlin.test.*

/**
 * TabViewModelTest - Unit tests for TabViewModel
 *
 * Tests:
 * - Tab creation and deletion
 * - Tab switching
 * - URL navigation
 * - Loading state management
 * - Title updates
 * - Navigation state updates
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TabViewModelTest {

    private lateinit var repository: FakeBrowserRepository
    private lateinit var viewModel: TabViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeBrowserRepository()
        viewModel = TabViewModel(repository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        viewModel.onCleared()
    }

    @Test
    fun `createTab creates new tab and sets it as active`() = runTest(testDispatcher) {
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

    @Test
    fun `createTab with setActive=false does not change active tab`() = runTest(testDispatcher) {
        // Given
        viewModel.createTab(url = "https://first.com", title = "First")
        advanceUntilIdle()
        val firstActiveTab = viewModel.activeTab.first()

        // When
        viewModel.createTab(url = "https://second.com", title = "Second", setActive = false)
        advanceUntilIdle()

        // Then
        val tabs = viewModel.tabs.first()
        val activeTab = viewModel.activeTab.first()

        assertEquals(2, tabs.size)
        assertEquals(firstActiveTab?.tab?.id, activeTab?.tab?.id)
        assertEquals("https://first.com", activeTab?.tab?.url)
    }

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

    @Test
    fun `closeTab switches to previous tab when closing active tab`() = runTest(testDispatcher) {
        // Given
        viewModel.createTab(url = "https://first.com", title = "First")
        advanceUntilIdle()
        val firstTabId = viewModel.activeTab.first()?.tab?.id!!

        viewModel.createTab(url = "https://second.com", title = "Second")
        advanceUntilIdle()
        val secondTabId = viewModel.activeTab.first()?.tab?.id!!

        // When
        viewModel.closeTab(secondTabId)
        advanceUntilIdle()

        // Then
        val activeTab = viewModel.activeTab.first()
        assertEquals(firstTabId, activeTab?.tab?.id)
        assertEquals("https://first.com", activeTab?.tab?.url)
    }

    @Test
    fun `switchTab changes active tab`() = runTest(testDispatcher) {
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

    @Test
    fun `navigateToUrl updates active tab URL`() = runTest(testDispatcher) {
        // Given
        viewModel.createTab(url = "https://example.com", title = "Example")
        advanceUntilIdle()

        // When
        viewModel.navigateToUrl("https://newurl.com")
        advanceUntilIdle()

        // Then
        val activeTab = viewModel.activeTab.first()
        assertEquals("https://newurl.com", activeTab?.tab?.url)
    }

    @Test
    fun `updateTabUrl updates specific tab URL`() = runTest(testDispatcher) {
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

    @Test
    fun `updateTabLoading updates loading state`() = runTest(testDispatcher) {
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
    }

    @Test
    fun `updateTabTitle updates tab title`() = runTest(testDispatcher) {
        // Given
        viewModel.createTab(url = "https://example.com", title = "Example")
        advanceUntilIdle()
        val tabId = viewModel.activeTab.first()?.tab?.id!!

        // When
        viewModel.updateTabTitle(tabId, "New Title")
        advanceUntilIdle()

        // Then
        val activeTab = viewModel.activeTab.first()
        assertEquals("New Title", activeTab?.tab?.title)
    }

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
    }

    @Test
    fun `loadTabs loads tabs from repository`() = runTest(testDispatcher) {
        // Given
        val now = kotlinx.datetime.Clock.System.now()
        val tab1 = Tab(id = "1", url = "https://first.com", title = "First", createdAt = now, lastAccessedAt = now)
        val tab2 = Tab(id = "2", url = "https://second.com", title = "Second", createdAt = now, lastAccessedAt = now)
        repository.setTabs(listOf(tab1, tab2))

        // When
        viewModel.loadTabs()
        advanceUntilIdle()

        // Then
        val tabs = viewModel.tabs.first()
        assertEquals(2, tabs.size)
        assertEquals("https://first.com", tabs[0].tab.url)
        assertEquals("https://second.com", tabs[1].tab.url)
    }

    @Test
    fun `error state is set when repository operation fails`() = runTest(testDispatcher) {
        // Given
        repository.setShouldFail(true)

        // When
        viewModel.loadTabs()
        advanceUntilIdle()

        // Then
        val error = viewModel.errorMessage.first()
        assertNotNull(error)
        assertTrue(error.contains("Failed"))
    }

    // ========== Phase 4: Zoom Control Tests ==========

    @Test
    fun `zoomIn increases zoom level and persists to repository`() = runTest(testDispatcher) {
        // Given
        viewModel.createTab(url = "https://example.com", title = "Example")
        advanceUntilIdle()
        val initialTab = viewModel.activeTab.first()
        assertEquals(3, initialTab?.tab?.zoomLevel) // Default is 3 (100%)

        // When
        viewModel.zoomIn()
        advanceUntilIdle()

        // Then
        val activeTab = viewModel.activeTab.first()
        assertEquals(4, activeTab?.tab?.zoomLevel)
    }

    @Test
    fun `zoomIn does not exceed maximum zoom level 5`() = runTest(testDispatcher) {
        // Given
        viewModel.createTab(url = "https://example.com", title = "Example")
        advanceUntilIdle()

        // When - Zoom in 5 times (should cap at level 5)
        viewModel.zoomIn()
        viewModel.zoomIn()
        viewModel.zoomIn()
        viewModel.zoomIn()
        viewModel.zoomIn()
        advanceUntilIdle()

        // Then
        val activeTab = viewModel.activeTab.first()
        assertEquals(5, activeTab?.tab?.zoomLevel)
    }

    @Test
    fun `zoomOut decreases zoom level and persists to repository`() = runTest(testDispatcher) {
        // Given
        viewModel.createTab(url = "https://example.com", title = "Example")
        advanceUntilIdle()

        // When
        viewModel.zoomOut()
        advanceUntilIdle()

        // Then
        val activeTab = viewModel.activeTab.first()
        assertEquals(2, activeTab?.tab?.zoomLevel)
    }

    @Test
    fun `zoomOut does not go below minimum zoom level 1`() = runTest(testDispatcher) {
        // Given
        viewModel.createTab(url = "https://example.com", title = "Example")
        advanceUntilIdle()

        // When - Zoom out 5 times (should cap at level 1)
        viewModel.zoomOut()
        viewModel.zoomOut()
        viewModel.zoomOut()
        viewModel.zoomOut()
        viewModel.zoomOut()
        advanceUntilIdle()

        // Then
        val activeTab = viewModel.activeTab.first()
        assertEquals(1, activeTab?.tab?.zoomLevel)
    }

    @Test
    fun `setZoomLevel sets specific zoom level and persists`() = runTest(testDispatcher) {
        // Given
        viewModel.createTab(url = "https://example.com", title = "Example")
        advanceUntilIdle()

        // When
        viewModel.setZoomLevel(5)
        advanceUntilIdle()

        // Then
        val activeTab = viewModel.activeTab.first()
        assertEquals(5, activeTab?.tab?.zoomLevel)
    }

    @Test
    fun `setZoomLevel validates level range`() = runTest(testDispatcher) {
        // Given
        viewModel.createTab(url = "https://example.com", title = "Example")
        advanceUntilIdle()

        // When - Try to set invalid level (should clamp to 1-5)
        viewModel.setZoomLevel(10)
        advanceUntilIdle()

        // Then
        val activeTab = viewModel.activeTab.first()
        assertEquals(5, activeTab?.tab?.zoomLevel) // Clamped to max

        // When - Try negative level
        viewModel.setZoomLevel(-5)
        advanceUntilIdle()

        // Then
        val activeTab2 = viewModel.activeTab.first()
        assertEquals(1, activeTab2?.tab?.zoomLevel) // Clamped to min
    }

    // ========== Phase 4: Desktop Mode Tests ==========

    @Test
    fun `toggleDesktopMode toggles desktop mode state and persists`() = runTest(testDispatcher) {
        // Given
        viewModel.createTab(url = "https://example.com", title = "Example")
        advanceUntilIdle()
        val initialTab = viewModel.activeTab.first()
        assertFalse(initialTab?.tab?.isDesktopMode ?: true) // Default is false

        // When
        viewModel.toggleDesktopMode()
        advanceUntilIdle()

        // Then
        val activeTab = viewModel.activeTab.first()
        assertTrue(activeTab?.tab?.isDesktopMode ?: false)

        // When - Toggle again
        viewModel.toggleDesktopMode()
        advanceUntilIdle()

        // Then
        val activeTab2 = viewModel.activeTab.first()
        assertFalse(activeTab2?.tab?.isDesktopMode ?: true)
    }

    @Test
    fun `setDesktopMode sets desktop mode explicitly`() = runTest(testDispatcher) {
        // Given
        viewModel.createTab(url = "https://example.com", title = "Example")
        advanceUntilIdle()

        // When
        viewModel.setDesktopMode(true)
        advanceUntilIdle()

        // Then
        val activeTab = viewModel.activeTab.first()
        assertTrue(activeTab?.tab?.isDesktopMode ?: false)

        // When
        viewModel.setDesktopMode(false)
        advanceUntilIdle()

        // Then
        val activeTab2 = viewModel.activeTab.first()
        assertFalse(activeTab2?.tab?.isDesktopMode ?: true)
    }

    @Test
    fun `desktop mode persists when switching tabs`() = runTest(testDispatcher) {
        // Given
        viewModel.createTab(url = "https://first.com", title = "First")
        advanceUntilIdle()
        val firstTabId = viewModel.activeTab.first()?.tab?.id!!
        viewModel.setDesktopMode(true)
        advanceUntilIdle()

        viewModel.createTab(url = "https://second.com", title = "Second")
        advanceUntilIdle()
        val secondTabId = viewModel.activeTab.first()?.tab?.id!!

        // Second tab should have desktop mode false (default)
        assertFalse(viewModel.activeTab.first()?.tab?.isDesktopMode ?: true)

        // When - Switch back to first tab
        viewModel.switchTab(firstTabId)
        advanceUntilIdle()

        // Then - First tab should still have desktop mode true
        val activeTab = viewModel.activeTab.first()
        assertTrue(activeTab?.tab?.isDesktopMode ?: false)
    }

    // ========== Phase 4: Scroll Position Tests ==========

    @Test
    fun `updateScrollPosition persists scroll coordinates`() = runTest(testDispatcher) {
        // Given
        viewModel.createTab(url = "https://example.com", title = "Example")
        advanceUntilIdle()

        // When
        viewModel.updateScrollPosition(x = 100, y = 500)
        advanceUntilIdle()

        // Then
        val activeTab = viewModel.activeTab.first()
        assertEquals(100, activeTab?.tab?.scrollXPosition)
        assertEquals(500, activeTab?.tab?.scrollYPosition)
    }

    @Test
    fun `scroll position persists when switching tabs`() = runTest(testDispatcher) {
        // Given
        viewModel.createTab(url = "https://first.com", title = "First")
        advanceUntilIdle()
        val firstTabId = viewModel.activeTab.first()?.tab?.id!!
        viewModel.updateScrollPosition(x = 100, y = 500)
        advanceUntilIdle()

        viewModel.createTab(url = "https://second.com", title = "Second")
        advanceUntilIdle()
        viewModel.updateScrollPosition(x = 200, y = 1000)
        advanceUntilIdle()

        // When - Switch back to first tab
        viewModel.switchTab(firstTabId)
        advanceUntilIdle()

        // Then - First tab should have original scroll position
        val activeTab = viewModel.activeTab.first()
        assertEquals(100, activeTab?.tab?.scrollXPosition)
        assertEquals(500, activeTab?.tab?.scrollYPosition)
    }

    @Test
    fun `zoom level persists when switching tabs`() = runTest(testDispatcher) {
        // Given
        viewModel.createTab(url = "https://first.com", title = "First")
        advanceUntilIdle()
        val firstTabId = viewModel.activeTab.first()?.tab?.id!!
        viewModel.setZoomLevel(5)
        advanceUntilIdle()

        viewModel.createTab(url = "https://second.com", title = "Second")
        advanceUntilIdle()
        viewModel.setZoomLevel(1)
        advanceUntilIdle()

        // When - Switch back to first tab
        viewModel.switchTab(firstTabId)
        advanceUntilIdle()

        // Then - First tab should have zoom level 5
        val activeTab = viewModel.activeTab.first()
        assertEquals(5, activeTab?.tab?.zoomLevel)
    }

    // ========== TabUiState Wrapper Tests ==========

    @Test
    fun `activeTab provides TabUiState wrapper properties`() = runTest(testDispatcher) {
        // Given
        viewModel.createTab(url = "https://example.com", title = "Example")
        advanceUntilIdle()

        // Then
        val activeTab = viewModel.activeTab.first()

        // Test TabUiState wrapper properties
        assertNotNull(activeTab)
        assertFalse(activeTab?.isLoading ?: true)
        assertFalse(activeTab?.canGoBack ?: true)
        assertFalse(activeTab?.canGoForward ?: true)

        // Test wrapped Tab properties
        assertEquals("https://example.com", activeTab?.tab?.url)
        assertEquals("Example", activeTab?.tab?.title)
    }

    @Test
    fun `TabUiState wrapper reflects loading state changes`() = runTest(testDispatcher) {
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

    @Test
    fun `TabUiState wrapper reflects navigation state changes`() = runTest(testDispatcher) {
        // Given
        viewModel.createTab(url = "https://example.com", title = "Example")
        advanceUntilIdle()
        val tabId = viewModel.activeTab.first()?.tab?.id!!

        // When
        viewModel.updateTabNavigation(tabId, canGoBack = true, canGoForward = true)
        advanceUntilIdle()

        // Then
        val activeTab = viewModel.activeTab.first()
        assertTrue(activeTab?.canGoBack ?: false)
        assertTrue(activeTab?.canGoForward ?: false)

        // When - Update navigation again
        viewModel.updateTabNavigation(tabId, canGoBack = false, canGoForward = false)
        advanceUntilIdle()

        // Then
        val activeTab2 = viewModel.activeTab.first()
        assertFalse(activeTab2?.canGoBack ?: true)
        assertFalse(activeTab2?.canGoForward ?: true)
    }
}
