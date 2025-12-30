package com.augmentalis.webavanue.ui.viewmodel.tab

import com.augmentalis.webavanue.domain.model.Tab
import com.augmentalis.webavanue.util.ReadingModeArticle

/**
 * UI state wrapper for Tab with additional transient UI properties
 * These are not persisted to database - only for runtime UI state
 */
data class TabUiState(
    val tab: Tab,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isReadingMode: Boolean = false,
    val readingModeArticle: ReadingModeArticle? = null,
    val isArticleAvailable: Boolean = false
)

/**
 * Find in page state for text search functionality
 *
 * @property isVisible Whether the find bar is visible
 * @property query Current search query
 * @property currentMatch Index of current match (0-based)
 * @property totalMatches Total number of matches found
 * @property caseSensitive Whether search is case-sensitive
 * @property highlightAll Whether to highlight all matches (always true for better UX)
 */
data class FindInPageState(
    val isVisible: Boolean = false,
    val query: String = "",
    val currentMatch: Int = 0,
    val totalMatches: Int = 0,
    val caseSensitive: Boolean = false,
    val highlightAll: Boolean = true
)

/**
 * Combined UI state for efficient recomposition
 * Reduces recompositions by batching tab state updates
 */
data class CombinedTabUiState(
    val tabs: List<TabUiState> = emptyList(),
    val activeTab: TabUiState? = null,
    val error: String? = null,
    val isLoading: Boolean = false
)

/**
 * Constants for new tab page URLs
 */
object NewTabUrls {
    const val TOP_SITES = "webavanue://top-sites"
    const val MOST_VISITED = "webavanue://most-visited"
    const val SPEED_DIAL = "webavanue://speed-dial"
    const val NEWS_FEED = "webavanue://news-feed"
}
