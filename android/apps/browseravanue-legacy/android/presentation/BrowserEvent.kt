package com.augmentalis.browseravanue.presentation

import android.net.http.SslError
import android.webkit.HttpAuthHandler
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler

/**
 * Browser user events
 *
 * Architecture:
 * - Sealed class for type-safe events
 * - One-way data flow (Event → ViewModel → State)
 * - Covers all user interactions
 * - Maps to UseCases
 *
 * Event Flow:
 * ```
 * User Action (UI)
 *     ↓
 * BrowserEvent emitted
 *     ↓
 * ViewModel.onEvent(event)
 *     ↓
 * UseCase executed
 *     ↓
 * State updated
 *     ↓
 * UI recomposes
 * ```
 *
 * Event Categories:
 * 1. Navigation Events (8)
 * 2. Tab Events (7)
 * 3. Favorite Events (3)
 * 4. Settings Events (2)
 * 5. Page Events (6)
 * 6. Privacy Events (3)
 * 7. Voice Events (1)
 * 8. Dialog Events (4)
 *
 * Usage:
 * ```
 * Button(onClick = { viewModel.onEvent(BrowserEvent.NewTab) }) {
 *     Text("New Tab")
 * }
 * ```
 */
sealed class BrowserEvent {

    // ==========================================
    // Navigation Events
    // ==========================================

    /**
     * Navigate to URL
     */
    data class NavigateToUrl(val url: String) : BrowserEvent()

    /**
     * Search with query
     */
    data class Search(val query: String) : BrowserEvent()

    /**
     * Go back in history
     */
    data object GoBack : BrowserEvent()

    /**
     * Go forward in history
     */
    data object GoForward : BrowserEvent()

    /**
     * Refresh current page
     */
    data object Refresh : BrowserEvent()

    /**
     * Stop page loading
     */
    data object StopLoading : BrowserEvent()

    /**
     * Go to home page
     */
    data object GoHome : BrowserEvent()

    /**
     * Navigate to internal browser page (settings, favorites, etc.)
     */
    data class NavigateToInternalPage(val page: InternalPage) : BrowserEvent()

    // ==========================================
    // Tab Events
    // ==========================================

    /**
     * Create new tab
     */
    data class NewTab(val url: String? = null, val isIncognito: Boolean = false) : BrowserEvent()

    /**
     * Close tab
     */
    data class CloseTab(val tabId: String) : BrowserEvent()

    /**
     * Switch to tab
     */
    data class SwitchTab(val tabId: String) : BrowserEvent()

    /**
     * Switch to next tab
     */
    data object NextTab : BrowserEvent()

    /**
     * Switch to previous tab
     */
    data object PreviousTab : BrowserEvent()

    /**
     * Duplicate current tab
     */
    data object DuplicateTab : BrowserEvent()

    /**
     * Reorder tabs
     */
    data class ReorderTabs(val fromIndex: Int, val toIndex: Int) : BrowserEvent()

    // ==========================================
    // Favorite Events
    // ==========================================

    /**
     * Add current page to favorites
     */
    data class AddToFavorites(
        val url: String,
        val title: String,
        val folder: String? = null,
        val tags: List<String> = emptyList()
    ) : BrowserEvent()

    /**
     * Remove from favorites
     */
    data class RemoveFavorite(val favoriteId: String) : BrowserEvent()

    /**
     * Open favorite
     */
    data class OpenFavorite(val favoriteId: String) : BrowserEvent()

    // ==========================================
    // Settings Events
    // ==========================================

    /**
     * Update browser settings
     */
    data class UpdateSettings(val settings: com.augmentalis.browseravanue.domain.model.BrowserSettings) : BrowserEvent()

    /**
     * Toggle setting
     */
    data class ToggleSetting(val setting: SettingType) : BrowserEvent()

    // ==========================================
    // Page Events (WebView callbacks)
    // ==========================================

    /**
     * Page started loading
     */
    data class PageStarted(val url: String) : BrowserEvent()

    /**
     * Page finished loading
     */
    data class PageFinished(val url: String, val title: String?) : BrowserEvent()

    /**
     * Page loading progress
     */
    data class ProgressChanged(val progress: Int) : BrowserEvent()

    /**
     * Page title received
     */
    data class TitleReceived(val title: String) : BrowserEvent()

    /**
     * Download started
     */
    data class DownloadStarted(
        val url: String,
        val userAgent: String,
        val contentDisposition: String,
        val mimeType: String,
        val contentLength: Long
    ) : BrowserEvent()

    /**
     * New tab requested (popup, target="_blank")
     */
    data class NewTabRequested(val url: String) : BrowserEvent()

    // ==========================================
    // Privacy Events
    // ==========================================

    /**
     * Toggle ad blocking
     */
    data class ToggleAdBlocking(val enabled: Boolean) : BrowserEvent()

    /**
     * Toggle Do Not Track
     */
    data class ToggleDoNotTrack(val enabled: Boolean) : BrowserEvent()

    /**
     * Clear browsing data
     */
    data class ClearBrowsingData(
        val clearHistory: Boolean = true,
        val clearCache: Boolean = true,
        val clearCookies: Boolean = true
    ) : BrowserEvent()

    // ==========================================
    // Find in Page Events
    // ==========================================

    /**
     * Find in page
     */
    data class FindInPage(val query: String) : BrowserEvent()

    /**
     * Find next match
     */
    data object FindNext : BrowserEvent()

    /**
     * Find previous match
     */
    data object FindPrevious : BrowserEvent()

    /**
     * Close find in page
     */
    data object CloseFindInPage : BrowserEvent()

    // ==========================================
    // Scroll & Zoom Events
    // ==========================================

    /**
     * Scroll page
     */
    data class Scroll(val direction: ScrollDirection) : BrowserEvent()

    /**
     * Zoom page
     */
    data class Zoom(val direction: ZoomDirection) : BrowserEvent()

    // ==========================================
    // Voice Command Events
    // ==========================================

    /**
     * Process voice command
     */
    data class VoiceCommand(val command: String) : BrowserEvent()

    // ==========================================
    // Dialog Events
    // ==========================================

    /**
     * SSL error occurred
     */
    data class SslErrorOccurred(
        val error: SslError,
        val handler: SslErrorHandler
    ) : BrowserEvent()

    /**
     * SSL error decision (proceed or cancel)
     */
    data class SslErrorDecision(val proceed: Boolean) : BrowserEvent()

    /**
     * Authentication required
     */
    data class AuthenticationRequired(
        val host: String,
        val realm: String?,
        val handler: HttpAuthHandler
    ) : BrowserEvent()

    /**
     * Authentication credentials provided
     */
    data class AuthenticationCredentials(
        val username: String,
        val password: String
    ) : BrowserEvent()

    /**
     * Permission request
     */
    data class PermissionRequested(val request: PermissionRequest) : BrowserEvent()

    /**
     * Permission decision
     */
    data class PermissionDecision(val granted: Boolean) : BrowserEvent()

    // ==========================================
    // UI State Events
    // ==========================================

    /**
     * Toggle tab switcher
     */
    data object ToggleTabSwitcher : BrowserEvent()

    /**
     * Toggle favorites view
     */
    data object ToggleFavorites : BrowserEvent()

    /**
     * Toggle settings view
     */
    data object ToggleSettings : BrowserEvent()

    /**
     * Show history
     */
    data object ShowHistory : BrowserEvent()

    /**
     * Show downloads
     */
    data object ShowDownloads : BrowserEvent()

    /**
     * Dismiss dialog
     */
    data object DismissDialog : BrowserEvent()

    // ==========================================
    // Export/Import Events
    // ==========================================

    /**
     * Export browser data
     */
    data class ExportData(
        val includeHistory: Boolean = true,
        val includeCookies: Boolean = false
    ) : BrowserEvent()

    /**
     * Import browser data
     */
    data class ImportData(
        val jsonData: String,
        val conflictStrategy: com.augmentalis.browseravanue.data.export.ConflictStrategy
    ) : BrowserEvent()
}

/**
 * Internal browser pages
 */
enum class InternalPage {
    FAVORITES,
    HISTORY,
    SETTINGS,
    DOWNLOADS,
    ABOUT
}

/**
 * Setting types
 */
enum class SettingType {
    DESKTOP_MODE,
    DARK_MODE,
    AD_BLOCKING,
    DO_NOT_TRACK,
    COOKIES,
    JAVASCRIPT
}

/**
 * Scroll directions
 */
enum class ScrollDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    TOP,
    BOTTOM
}

/**
 * Zoom directions
 */
enum class ZoomDirection {
    IN,
    OUT,
    RESET
}
