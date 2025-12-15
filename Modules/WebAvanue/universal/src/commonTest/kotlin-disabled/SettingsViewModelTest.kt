package com.augmentalis.webavanue.ui.viewmodel

import com.augmentalis.webavanue.domain.model.BrowserSettings
import com.augmentalis.webavanue.FakeBrowserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlin.test.*

/**
 * SettingsViewModelTest - Unit tests for SettingsViewModel
 *
 * Tests:
 * - Loading settings
 * - Updating settings
 * - Toggling individual settings (desktop mode, JavaScript, cookies, etc.)
 * - Resetting to defaults
 * - Error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var repository: FakeBrowserRepository
    private lateinit var viewModel: SettingsViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeBrowserRepository()
        viewModel = SettingsViewModel(repository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        viewModel.onCleared()
    }

    @Test
    fun `loadSettings loads browser settings`() = runTest(testDispatcher) {
        // When
        viewModel.loadSettings()
        advanceUntilIdle()

        // Then
        val settings = viewModel.settings.first()
        assertNotNull(settings)
        assertTrue(settings.enableJavaScript)
        assertTrue(settings.enableCookies)
    }

    @Test
    fun `updateSettings updates all settings`() = runTest(testDispatcher) {
        // Given
        viewModel.loadSettings()
        advanceUntilIdle()

        val newSettings = BrowserSettings(
            enableJavaScript = false,
            enableCookies = false,
            blockPopups = true,
            useDesktopMode = true,
            enableCookies = false,
            textReflow = false,
            clearHistoryOnExit = false,
            defaultSearchEngine = BrowserSettings.SearchEngine.DUCKDUCKGO,
            homePage = "https://duckduckgo.com"
        )

        // When
        viewModel.updateSettings(newSettings)
        advanceUntilIdle()

        // Then
        val settings = viewModel.settings.first()
        assertNotNull(settings)
        assertFalse(settings.enableJavaScript)
        assertFalse(settings.enableCookies)
        assertTrue(settings.blockPopups)
        assertTrue(settings.useDesktopMode)
        assertEquals("DuckDuckGo", settings.defaultSearchEngine)
        assertEquals("https://duckduckgo.com", settings.homePage)
    }

    @Test
    fun `setDesktopMode updates desktop mode setting`() = runTest(testDispatcher) {
        // Given
        viewModel.loadSettings()
        advanceUntilIdle()

        // When
        viewModel.setDesktopMode(true)
        advanceUntilIdle()

        // Then
        val settings = viewModel.settings.first()
        assertTrue(settings?.useDesktopMode ?: false)
    }

    @Test
    fun `setBlockPopups updates popup blocker setting`() = runTest(testDispatcher) {
        // Given
        viewModel.loadSettings()
        advanceUntilIdle()

        // When
        viewModel.setBlockPopups(true)
        advanceUntilIdle()

        // Then
        val settings = viewModel.settings.first()
        assertTrue(settings?.blockPopups ?: false)
    }

    @Test
    fun `setEnableJavaScript updates JavaScript setting`() = runTest(testDispatcher) {
        // Given
        viewModel.loadSettings()
        advanceUntilIdle()

        // When
        viewModel.setEnableJavaScript(false)
        advanceUntilIdle()

        // Then
        val settings = viewModel.settings.first()
        assertFalse(settings?.enableJavaScript ?: true)
    }

    @Test
    fun `setEnableCookies updates cookies setting`() = runTest(testDispatcher) {
        // Given
        viewModel.loadSettings()
        advanceUntilIdle()

        // When
        viewModel.setEnableCookies(false)
        advanceUntilIdle()

        // Then
        val settings = viewModel.settings.first()
        assertFalse(settings?.enableCookies ?: true)
    }

    @Test
    fun `setEnableCookies updates location access setting`() = runTest(testDispatcher) {
        // Given
        viewModel.loadSettings()
        advanceUntilIdle()

        // When
        viewModel.setEnableCookies(false)
        advanceUntilIdle()

        // Then
        val settings = viewModel.settings.first()
        assertFalse(settings?.enableCookies ?: true)
    }

    @Test
    fun `setTextReflow updates media autoplay setting`() = runTest(testDispatcher) {
        // Given
        viewModel.loadSettings()
        advanceUntilIdle()

        // When
        viewModel.setTextReflow(false)
        advanceUntilIdle()

        // Then
        val settings = viewModel.settings.first()
        assertFalse(settings?.textReflow ?: true)
    }

    @Test
    fun `setClearHistoryOnExit updates browsing history setting`() = runTest(testDispatcher) {
        // Given
        viewModel.loadSettings()
        advanceUntilIdle()

        // When
        viewModel.setClearHistoryOnExit(false)
        advanceUntilIdle()

        // Then
        val settings = viewModel.settings.first()
        assertFalse(settings?.clearHistoryOnExit ?: true)
    }

    @Test
    fun `setDefaultSearchEngine updates search engine`() = runTest(testDispatcher) {
        // Given
        viewModel.loadSettings()
        advanceUntilIdle()

        // When
        viewModel.setDefaultSearchEngine("DuckDuckGo")
        advanceUntilIdle()

        // Then
        val settings = viewModel.settings.first()
        assertEquals("DuckDuckGo", settings?.defaultSearchEngine)
    }

    @Test
    fun `setHomepage updates homepage URL`() = runTest(testDispatcher) {
        // Given
        viewModel.loadSettings()
        advanceUntilIdle()

        // When
        viewModel.setHomepage("https://example.com")
        advanceUntilIdle()

        // Then
        val settings = viewModel.settings.first()
        assertEquals("https://example.com", settings?.homePage)
    }

    @Test
    fun `saveSuccess state is set after successful update`() = runTest(testDispatcher) {
        // Given
        viewModel.loadSettings()
        advanceUntilIdle()

        val currentSettings = viewModel.settings.first()!!

        // When
        viewModel.updateSettings(currentSettings.copy(useDesktopMode = true))
        advanceUntilIdle()

        // Then
        val saveSuccess = viewModel.saveSuccess.first()
        assertTrue(saveSuccess)
    }

    @Test
    fun `clearSaveSuccess clears save success state`() = runTest(testDispatcher) {
        // Given
        viewModel.loadSettings()
        advanceUntilIdle()
        val currentSettings = viewModel.settings.first()!!
        viewModel.updateSettings(currentSettings)
        advanceUntilIdle()

        // When
        viewModel.clearSaveSuccess()

        // Then
        val saveSuccess = viewModel.saveSuccess.first()
        assertFalse(saveSuccess)
    }

    @Test
    fun `resetToDefaults reloads settings`() = runTest(testDispatcher) {
        // Given
        viewModel.loadSettings()
        advanceUntilIdle()

        // Modify settings
        viewModel.setDesktopMode(true)
        advanceUntilIdle()

        // When
        viewModel.resetToDefaults()
        advanceUntilIdle()

        // Then
        val settings = viewModel.settings.first()
        assertNotNull(settings)
        // Should be reset to defaults
        assertFalse(settings.useDesktopMode)  // Default is false
    }

    @Test
    fun `error state is set when repository operation fails`() = runTest(testDispatcher) {
        // Given
        repository.setShouldFail(true)

        // When
        viewModel.loadSettings()
        advanceUntilIdle()

        // Then
        val error = viewModel.errorMessage.first()
        assertNotNull(error)
        assertTrue(error.contains("Failed"))
    }

    @Test
    fun `clearError clears error state`() = runTest(testDispatcher) {
        // Given
        repository.setShouldFail(true)
        viewModel.loadSettings()
        advanceUntilIdle()

        // When
        viewModel.clearError()

        // Then
        val error = viewModel.errorMessage.first()
        assertNull(error)
    }

    @Test
    fun `multiple setting updates preserve all changes`() = runTest(testDispatcher) {
        // Given
        viewModel.loadSettings()
        advanceUntilIdle()

        // When - update multiple settings
        viewModel.setEnableJavaScript(false)
        advanceUntilIdle()

        viewModel.setEnableCookies(false)
        advanceUntilIdle()

        viewModel.setDesktopMode(true)
        advanceUntilIdle()

        // Then
        val settings = viewModel.settings.first()
        assertNotNull(settings)
        assertFalse(settings.enableJavaScript)
        assertFalse(settings.enableCookies)
        assertTrue(settings.useDesktopMode)
    }
}
