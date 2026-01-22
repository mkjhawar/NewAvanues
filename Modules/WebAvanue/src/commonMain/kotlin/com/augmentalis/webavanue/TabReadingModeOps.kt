package com.augmentalis.webavanue

import com.augmentalis.webavanue.Logger
import com.augmentalis.webavanue.ReadingModeArticle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Reading mode operations handler
 *
 * Encapsulates all reading mode functionality including:
 * - Toggle reading mode
 * - Set article content
 * - Article availability detection
 */
class TabReadingModeOps(
    private val viewModelScope: CoroutineScope,
    private val stateMutex: Mutex,
    private val activeTab: MutableStateFlow<TabUiState?>,
    private val tabs: MutableStateFlow<List<TabUiState>>
) {
    /**
     * Toggle reading mode for the active tab
     *
     * When enabled:
     * - Extracts article content using JavaScript injection
     * - Shows ReadingModeView overlay with cleaned content
     * - Hides web content and browser UI
     *
     * When disabled:
     * - Returns to normal browsing view
     * - Preserves scroll position and state
     */
    fun toggleReadingMode() {
        viewModelScope.launch {
            stateMutex.withLock {
                activeTab.value?.let { state ->
                    val newReadingMode = !state.isReadingMode
                    val updatedState = state.copy(isReadingMode = newReadingMode)
                    activeTab.value = updatedState

                    // Update in tabs list
                    tabs.value = tabs.value.map {
                        if (it.tab.id == state.tab.id) updatedState else it
                    }

                    Logger.info("TabReadingModeOps", "Reading mode ${if (newReadingMode) "enabled" else "disabled"} for tab ${state.tab.id}")
                }
            }
        }
    }

    /**
     * Set reading mode article content for the active tab
     *
     * Called after JavaScript extraction completes
     *
     * @param article Extracted article content
     */
    fun setReadingModeArticle(article: ReadingModeArticle?) {
        viewModelScope.launch {
            stateMutex.withLock {
                activeTab.value?.let { state ->
                    val updatedState = state.copy(
                        readingModeArticle = article,
                        isArticleAvailable = article != null
                    )
                    activeTab.value = updatedState

                    // Update in tabs list
                    tabs.value = tabs.value.map {
                        if (it.tab.id == state.tab.id) updatedState else it
                    }
                }
            }
        }
    }

    /**
     * Set article availability indicator for the active tab
     *
     * Called after article detection script runs
     *
     * @param isAvailable Whether an article was detected on the page
     */
    fun setArticleAvailable(isAvailable: Boolean) {
        viewModelScope.launch {
            stateMutex.withLock {
                activeTab.value?.let { state ->
                    val updatedState = state.copy(isArticleAvailable = isAvailable)
                    activeTab.value = updatedState

                    // Update in tabs list
                    tabs.value = tabs.value.map {
                        if (it.tab.id == state.tab.id) updatedState else it
                    }
                }
            }
        }
    }
}
