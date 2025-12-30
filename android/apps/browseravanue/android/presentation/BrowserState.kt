package com.augmentalis.browseravanue.presentation

import com.augmentalis.browseravanue.domain.model.BrowserSettings
import com.augmentalis.browseravanue.domain.model.Favorite
import com.augmentalis.browseravanue.domain.model.Tab

/**
 * Browser UI state
 *
 * Architecture:
 * - Immutable state container
 * - Single source of truth for UI
 * - Compose-friendly (data class)
 * - Supports all browser features
 *
 * State Management:
 * - StateFlow in ViewModel
 * - UI observes and renders
 * - Events trigger state changes
 * - Type-safe sealed class for loading/error states
 *
 * Features Tracked:
 * - All tabs (active + background)
 * - Current tab state
 * - Favorites list
 * - Settings
 * - Loading states
 * - Error states
 * - UI states (dialogs, menus)
 *
 * Usage:
 * ```
 * val state by viewModel.state.collectAsState()
 * when (state) {
 *     is BrowserState.Loading -> LoadingScreen()
 *     is BrowserState.Success -> BrowserScreen(state.data)
 *     is BrowserState.Error -> ErrorScreen(state.message)
 * }
 * ```
 */
sealed class BrowserState {

    /**
     * Loading state (initial load, data refresh)
     */
    data object Loading : BrowserState()

    /**
     * Success state with all browser data
     */
    data class Success(
        // Tabs
        val tabs: List<Tab> = emptyList(),
        val currentTab: Tab? = null,
        val currentTabIndex: Int = 0,

        // Favorites
        val favorites: List<Favorite> = emptyList(),
        val favoriteFolders: List<String> = emptyList(),

        // Settings
        val settings: BrowserSettings = BrowserSettings(),

        // Page state
        val pageLoading: Boolean = false,
        val pageProgress: Int = 0,
        val pageTitle: String? = null,
        val pageUrl: String? = null,
        val canGoBack: Boolean = false,
        val canGoForward: Boolean = false,

        // Find in page
        val findInPageQuery: String = "",
        val findInPageActive: Boolean = false,
        val findInPageCurrentMatch: Int = 0,
        val findInPageTotalMatches: Int = 0,

        // UI state
        val showTabSwitcher: Boolean = false,
        val showFavorites: Boolean = false,
        val showSettings: Boolean = false,
        val showHistory: Boolean = false,
        val showDownloads: Boolean = false,

        // Dialogs
        val showSslErrorDialog: Boolean = false,
        val sslErrorMessage: String? = null,
        val showAuthDialog: Boolean = false,
        val authHost: String? = null,
        val authRealm: String? = null,

        // Privacy
        val isIncognitoMode: Boolean = false,
        val adBlockingEnabled: Boolean = false,
        val doNotTrackEnabled: Boolean = false,

        // Downloads
        val pendingDownloads: List<DownloadInfo> = emptyList(),

        // Voice commands
        val lastVoiceCommand: String? = null,
        val voiceCommandResult: String? = null
    ) : BrowserState()

    /**
     * Error state
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : BrowserState()
}

/**
 * Download information
 */
data class DownloadInfo(
    val url: String,
    val filename: String,
    val mimeType: String,
    val contentLength: Long,
    val progress: Int = 0,
    val isComplete: Boolean = false
)
