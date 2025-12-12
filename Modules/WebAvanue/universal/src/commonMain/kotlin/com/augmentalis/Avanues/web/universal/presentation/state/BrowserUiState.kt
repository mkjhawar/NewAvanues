package com.augmentalis.Avanues.web.universal.presentation.state

import com.augmentalis.Avanues.web.universal.presentation.viewmodel.TabUiState
import com.augmentalis.webavanue.domain.model.BrowserSettings
import com.augmentalis.webavanue.domain.model.Favorite

/**
 * PERFORMANCE OPTIMIZATION Phase 2: UI State Batching
 *
 * Combines all browser UI state into a single data class to reduce recompositions.
 * Instead of collecting 4+ separate StateFlows (tabs, activeTab, settings, favorites),
 * we collect one combined state, triggering recomposition only when the composite state changes.
 *
 * Benefits:
 * - Reduces recompositions from 4+ to 1 per state change
 * - More predictable state updates
 * - Easier to debug state changes
 * - Better performance with derivedStateOf
 */
data class BrowserUiState(
    val tabs: List<TabUiState> = emptyList(),
    val activeTab: TabUiState? = null,
    val settings: BrowserSettings? = null,
    val favorites: List<Favorite> = emptyList(),
    val error: String? = null,
    val isLoading: Boolean = false
) {
    companion object {
        /**
         * Default/initial state
         */
        fun initial() = BrowserUiState()
    }

    /**
     * Helper to check if browser has data loaded
     */
    val isInitialized: Boolean
        get() = settings != null

    /**
     * Helper to check if there are any tabs
     */
    val hasTabs: Boolean
        get() = tabs.isNotEmpty()

    /**
     * Helper to get active tab index
     */
    val activeTabIndex: Int
        get() = activeTab?.let { active ->
            tabs.indexOfFirst { it.tab.id == active.tab.id }
        } ?: -1
}
