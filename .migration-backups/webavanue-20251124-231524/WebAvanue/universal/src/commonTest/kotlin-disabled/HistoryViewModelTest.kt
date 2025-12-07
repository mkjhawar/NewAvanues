package com.augmentalis.Avanues.web.universal.presentation.viewmodel

import com.augmentalis.webavanue.domain.model.HistoryEntry
import com.augmentalis.Avanues.web.universal.FakeBrowserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlinx.datetime.Instant
import kotlin.test.*

/**
 * HistoryViewModelTest - Unit tests for HistoryViewModel
 *
 * Tests:
 * - Loading history (all and by date)
 * - Searching history
 * - Adding history entries
 * - Clearing history (all and by time range)
 * - Error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private lateinit var repository: FakeBrowserRepository
    private lateinit var viewModel: HistoryViewModel
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeBrowserRepository()
        viewModel = HistoryViewModel(repository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        viewModel.onCleared()
    }

    @Test
    fun `loadHistory loads all history entries`() = runTest(testDispatcher) {
        // Given
        val entry1 = HistoryEntry(
            id = "1",
            url = "https://first.com",
            title = "First Site",
            visitedAt = kotlinx.datetime.Clock.System.now()
        )
        val entry2 = HistoryEntry(
            id = "2",
            url = "https://second.com",
            title = "Second Site",
            visitedAt = kotlinx.datetime.Clock.System.now()
        )
        repository.setHistory(listOf(entry1, entry2))

        // When
        viewModel.loadHistory()
        advanceUntilIdle()

        // Then
        val history = viewModel.history.first()
        assertEquals(2, history.size)
        assertEquals("https://first.com", history[0].url)
        assertEquals("https://second.com", history[1].url)
    }

    @Test
    fun `loadHistoryByDateRange filters history by date`() = runTest(testDispatcher) {
        // Given
        val today = Instant.fromEpochMilliseconds(System.currentTimeMillis())
        val yesterday = Instant.fromEpochMilliseconds(System.currentTimeMillis() - 24 * 60 * 60 * 1000)

        val entry1 = HistoryEntry(
            id = "1",
            url = "https://today.com",
            title = "Today",
            visitedAt = today.toEpochMilliseconds()
        )
        val entry2 = HistoryEntry(
            id = "2",
            url = "https://yesterday.com",
            title = "Yesterday",
            visitedAt = yesterday.toEpochMilliseconds()
        )
        repository.setHistory(listOf(entry1, entry2))

        // When
        viewModel.loadHistoryByDateRange(today)
        advanceUntilIdle()

        // Then
        val history = viewModel.history.first()
        assertEquals(1, history.size)
        assertEquals("https://today.com", history[0].url)
    }

    @Test
    fun `searchHistory filters history by query`() = runTest(testDispatcher) {
        // Given
        val entry1 = HistoryEntry(
            id = "1",
            url = "https://github.com",
            title = "GitHub",
            visitedAt = kotlinx.datetime.Clock.System.now()
        )
        val entry2 = HistoryEntry(
            id = "2",
            url = "https://stackoverflow.com",
            title = "Stack Overflow",
            visitedAt = kotlinx.datetime.Clock.System.now()
        )
        repository.setHistory(listOf(entry1, entry2))

        // When
        viewModel.searchHistory("GitHub")
        advanceUntilIdle()

        // Then
        val history = viewModel.history.first()
        val searchQuery = viewModel.searchQuery.first()

        assertEquals(1, history.size)
        assertEquals("https://github.com", history[0].url)
        assertEquals("GitHub", searchQuery)
    }

    @Test
    fun `searchHistory with empty query reloads all history`() = runTest(testDispatcher) {
        // Given
        val entry1 = HistoryEntry(
            id = "1",
            url = "https://first.com",
            title = "First",
            visitedAt = kotlinx.datetime.Clock.System.now()
        )
        val entry2 = HistoryEntry(
            id = "2",
            url = "https://second.com",
            title = "Second",
            visitedAt = kotlinx.datetime.Clock.System.now()
        )
        repository.setHistory(listOf(entry1, entry2))

        // When
        viewModel.searchHistory("")
        advanceUntilIdle()

        // Then
        val history = viewModel.history.first()
        assertEquals(2, history.size)
    }

    @Test
    fun `addHistoryEntry creates new history entry`() = runTest(testDispatcher) {
        // When
        viewModel.addHistoryEntry(url = "https://example.com", title = "Example")
        advanceUntilIdle()

        // Then
        val history = viewModel.history.first()
        assertEquals(1, history.size)
        assertEquals("https://example.com", history[0].url)
        assertEquals("Example", history[0].title)
    }

    @Test
    fun `addHistoryEntry with null title uses URL as title`() = runTest(testDispatcher) {
        // When
        viewModel.addHistoryEntry(url = "https://example.com")
        advanceUntilIdle()

        // Then
        val history = viewModel.history.first()
        assertEquals(1, history.size)
        assertEquals("https://example.com", history[0].url)
        assertEquals("https://example.com", history[0].title)
    }

    @Test
    fun `clearHistory removes all history entries`() = runTest(testDispatcher) {
        // Given
        val entry1 = HistoryEntry(
            id = "1",
            url = "https://first.com",
            title = "First",
            visitedAt = kotlinx.datetime.Clock.System.now()
        )
        val entry2 = HistoryEntry(
            id = "2",
            url = "https://second.com",
            title = "Second",
            visitedAt = kotlinx.datetime.Clock.System.now()
        )
        repository.setHistory(listOf(entry1, entry2))
        viewModel.loadHistory()
        advanceUntilIdle()

        // When
        viewModel.clearHistory()
        advanceUntilIdle()

        // Then
        val history = viewModel.history.first()
        assertEquals(0, history.size)
    }

    @Test
    fun `clearHistoryByTimeRange removes entries in time range`() = runTest(testDispatcher) {
        // Given
        val now = System.currentTimeMillis()
        val oneHourAgo = now - (60 * 60 * 1000)
        val twoDaysAgo = now - (2 * 24 * 60 * 60 * 1000)

        val entry1 = HistoryEntry(
            id = "1",
            url = "https://recent.com",
            title = "Recent",
            visitedAt = oneHourAgo
        )
        val entry2 = HistoryEntry(
            id = "2",
            url = "https://old.com",
            title = "Old",
            visitedAt = twoDaysAgo
        )
        repository.setHistory(listOf(entry1, entry2))
        viewModel.loadHistory()
        advanceUntilIdle()

        // When - clear last 24 hours
        val startTime = Instant.fromEpochMilliseconds(now - (24 * 60 * 60 * 1000))
        val endTime = Instant.fromEpochMilliseconds(now)
        viewModel.clearHistoryByTimeRange(startTime, endTime)
        advanceUntilIdle()

        // Then - only old entry remains
        val history = viewModel.history.first()
        assertEquals(1, history.size)
        assertEquals("https://old.com", history[0].url)
    }

    @Test
    fun `clearSearch resets search query and reloads history`() = runTest(testDispatcher) {
        // Given
        viewModel.searchHistory("test query")
        advanceUntilIdle()

        // When
        viewModel.clearSearch()
        advanceUntilIdle()

        // Then
        val searchQuery = viewModel.searchQuery.first()
        assertEquals("", searchQuery)
    }

    @Test
    fun `error state is set when repository operation fails`() = runTest(testDispatcher) {
        // Given
        repository.setShouldFail(true)

        // When
        viewModel.loadHistory()
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
        viewModel.loadHistory()
        advanceUntilIdle()

        // When
        viewModel.clearError()

        // Then
        val error = viewModel.errorMessage.first()
        assertNull(error)
    }
}
