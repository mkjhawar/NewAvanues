package com.augmentalis.Avanues.web.data.data.repository

import com.augmentalis.Avanues.web.data.db.BrowserDatabase
import com.augmentalis.Avanues.web.data.domain.model.SearchEngine
import app.cash.sqldelight.sqlite.driver.JdbcSqliteDriver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration test for BrowserRepositoryImpl using in-memory SQLDelight database
 *
 * Platform-specific test (Desktop/JVM) using JdbcSqliteDriver for in-memory SQLite
 */
class BrowserRepositoryImplTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var database: BrowserDatabase
    private lateinit var repository: BrowserRepositoryImpl

    @BeforeTest
    fun setup() {
        // Create in-memory SQLite database
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        // Create schema
        BrowserDatabase.Schema.create(driver)

        // Create database instance
        database = BrowserDatabase(driver)

        // Create repository
        repository = BrowserRepositoryImpl(database)
    }

    @AfterTest
    fun teardown() {
        driver.close()
    }

    // ============ TAB OPERATIONS ============

    @Test
    fun `createTab should insert tab into database`() = runTest {
        // Given
        val url = "https://example.com"
        val title = "Example Site"

        // When
        val result = repository.createTab(url, title)

        // Then
        assertTrue(result.isSuccess)
        val tab = result.getOrNull()
        assertEquals(url, tab?.url)
        assertEquals(title, tab?.title)
    }

    @Test
    fun `closeTab should remove tab from database`() = runTest {
        // Given
        val createResult = repository.createTab("https://example.com", "Example")
        val tabId = createResult.getOrNull()?.id ?: ""

        // When
        val result = repository.closeTab(tabId)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `getAllTabs should return Flow of all tabs`() = runTest {
        // Given
        repository.createTab("https://example1.com", "Site 1")
        repository.createTab("https://example2.com", "Site 2")

        // When
        val flow = repository.getAllTabs()
        val tabs = flow.first()

        // Then
        assertEquals(2, tabs.size)
    }

    @Test
    fun `updateTab should update tab properties`() = runTest {
        // Given
        val createResult = repository.createTab("https://example.com", "Example")
        val originalTab = createResult.getOrNull()!!

        // When - Update tab title
        val updatedTab = originalTab.copy(title = "Updated Title")
        val result = repository.updateTab(updatedTab)

        // Then
        assertTrue(result.isSuccess)
        val retrievedTab = repository.getTab(originalTab.id).getOrNull()
        assertEquals("Updated Title", retrievedTab?.title)
    }

    // ============ FAVORITE OPERATIONS ============

    @Test
    fun `addFavorite should insert favorite into database`() = runTest {
        // Given
        val url = "https://example.com"
        val title = "Example Site"

        // When
        val result = repository.addFavorite(url, title)

        // Then
        assertTrue(result.isSuccess)
        val favorite = result.getOrNull()
        assertEquals(url, favorite?.url)
        assertEquals(title, favorite?.title)
    }

    @Test
    fun `removeFavorite should delete favorite from database`() = runTest {
        // Given
        val addResult = repository.addFavorite("https://example.com", "Example")
        val favoriteId = addResult.getOrNull()?.id ?: ""

        // When
        val result = repository.removeFavorite(favoriteId)

        // Then
        assertTrue(result.isSuccess)
    }

    @Test
    fun `isFavorite should return true for favorited URL`() = runTest {
        // Given
        val url = "https://example.com"
        repository.addFavorite(url, "Example")

        // When
        val result = repository.isFavorite(url)

        // Then
        assertTrue(result)
    }

    @Test
    fun `isFavorite should return false for non-favorited URL`() = runTest {
        // When
        val result = repository.isFavorite("https://unknown.com")

        // Then
        assertFalse(result)
    }

    @Test
    fun `getAllFavorites should return Flow of all favorites`() = runTest {
        // Given
        repository.addFavorite("https://example1.com", "Site 1")
        repository.addFavorite("https://example2.com", "Site 2")

        // When
        val flow = repository.getAllFavorites()
        val favorites = flow.first()

        // Then
        assertEquals(2, favorites.size)
    }

    // ============ HISTORY OPERATIONS ============

    @Test
    fun `addHistoryEntry should insert history entry`() = runTest {
        // Given
        val url = "https://example.com"
        val title = "Example Site"

        // When
        val result = repository.addHistoryEntry(url, title)

        // Then
        assertTrue(result.isSuccess)
        val entry = result.getOrNull()
        assertEquals(url, entry?.url)
        assertEquals(title, entry?.title)
        assertEquals(1, entry?.visitCount)
    }

    @Test
    fun `addHistoryEntry with existing URL should increment visit count`() = runTest {
        // Given
        val url = "https://example.com"
        repository.addHistoryEntry(url, "Example")

        // When
        val result = repository.addHistoryEntry(url, "Example")

        // Then
        assertTrue(result.isSuccess)
        val entry = result.getOrNull()
        assertEquals(2, entry?.visitCount)
    }

    @Test
    fun `clearHistory should remove all history entries`() = runTest {
        // Given
        repository.addHistoryEntry("https://example1.com", "Site 1")
        repository.addHistoryEntry("https://example2.com", "Site 2")

        // When
        val result = repository.clearHistory()

        // Then
        assertTrue(result.isSuccess)
        val history = repository.getAllHistory().first()
        assertTrue(history.isEmpty())
    }

    @Test
    fun `searchHistory should find entries by query`() = runTest {
        // Given
        repository.addHistoryEntry("https://example.com", "Example Site")
        repository.addHistoryEntry("https://google.com", "Google")

        // When
        val flow = repository.searchHistory("example")
        val results = flow.first()

        // Then
        assertEquals(1, results.size)
        assertEquals("https://example.com", results.first().url)
    }

    @Test
    fun `getHistoryByDate should return entries from specific date`() = runTest {
        // Given
        val now = Clock.System.now()
        repository.addHistoryEntry("https://example.com", "Example")

        // When
        val flow = repository.getHistoryByDate(now)
        val results = flow.first()

        // Then
        assertTrue(results.isNotEmpty())
    }

    // ============ SETTINGS OPERATIONS ============

    @Test
    fun `getSettings should return default settings initially`() = runTest {
        // When
        val result = repository.getSettings()

        // Then
        assertTrue(result.isSuccess)
        val settings = result.getOrNull()!!
        assertFalse(settings.desktopMode)
        assertTrue(settings.blockPopups)
        assertFalse(settings.clearCacheOnExit)
        assertEquals(SearchEngine.GOOGLE, settings.defaultSearchEngine)
    }

    @Test
    fun `updateSettings should persist changes`() = runTest {
        // Given
        val currentSettings = repository.getSettings().getOrNull()!!
        val newSettings = currentSettings.copy(
            desktopMode = true,
            defaultSearchEngine = SearchEngine.DUCKDUCKGO
        )

        // When
        val result = repository.updateSettings(newSettings)

        // Then
        assertTrue(result.isSuccess)
        val updatedSettings = repository.getSettings().getOrNull()!!
        assertTrue(updatedSettings.desktopMode)
        assertEquals(SearchEngine.DUCKDUCKGO, updatedSettings.defaultSearchEngine)
    }

    @Test
    fun `toggleDesktopMode should toggle setting`() = runTest {
        // Given
        val initial = repository.getSettings().getOrNull()!!
        val initialDesktopMode = initial.desktopMode

        // When
        val result = repository.toggleDesktopMode()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(!initialDesktopMode, result.getOrNull())
    }

    @Test
    fun `closeAllTabs should remove all tabs from database`() = runTest {
        // Given
        repository.createTab("https://example1.com", "Site 1")
        repository.createTab("https://example2.com", "Site 2")

        // When
        val result = repository.closeAllTabs()

        // Then
        assertTrue(result.isSuccess)
        val tabs = repository.getAllTabs().first()
        assertTrue(tabs.isEmpty())
    }

    @Test
    fun `getTab should return tab by ID`() = runTest {
        // Given
        val createResult = repository.createTab("https://example.com", "Example")
        val tabId = createResult.getOrNull()?.id ?: ""

        // When
        val result = repository.getTab(tabId)

        // Then
        assertTrue(result.isSuccess)
        val tab = result.getOrNull()
        assertEquals("https://example.com", tab?.url)
    }

    @Test
    fun `getFavorite should return favorite by ID`() = runTest {
        // Given
        val addResult = repository.addFavorite("https://example.com", "Example")
        val favoriteId = addResult.getOrNull()?.id ?: ""

        // When
        val result = repository.getFavorite(favoriteId)

        // Then
        assertTrue(result.isSuccess)
        val favorite = result.getOrNull()
        assertEquals("https://example.com", favorite?.url)
    }

    @Test
    fun `getHistoryEntry should return history entry by ID`() = runTest {
        // Given
        val addResult = repository.addHistoryEntry("https://example.com", "Example")
        val entryId = addResult.getOrNull()?.id ?: ""

        // When
        val result = repository.getHistoryEntry(entryId)

        // Then
        assertTrue(result.isSuccess)
        val entry = result.getOrNull()
        assertEquals("https://example.com", entry?.url)
    }
}
