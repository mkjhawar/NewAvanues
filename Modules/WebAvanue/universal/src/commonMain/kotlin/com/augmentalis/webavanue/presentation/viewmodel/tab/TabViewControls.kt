package com.augmentalis.webavanue.ui.viewmodel.tab

import com.augmentalis.webavanue.domain.model.Tab
import com.augmentalis.webavanue.domain.repository.BrowserRepository
import com.augmentalis.webavanue.ui.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Tab view controls handler
 *
 * Encapsulates scroll, zoom, and desktop mode operations
 */
class TabViewControls(
    private val repository: BrowserRepository,
    private val viewModelScope: CoroutineScope,
    private val activeTab: MutableStateFlow<TabUiState?>,
    private val error: MutableStateFlow<String?>
) {
    // ========== Scroll Controls ==========

    /**
     * Scroll up in the active tab's WebView
     */
    fun scrollUp() {
        // Will be implemented with WebViewController reference
    }

    /**
     * Scroll down in the active tab's WebView
     */
    fun scrollDown() {
        // Will be implemented with WebViewController reference
    }

    /**
     * Scroll left in the active tab's WebView
     */
    fun scrollLeft() {
        // Will be implemented with WebViewController reference
    }

    /**
     * Scroll right in the active tab's WebView
     */
    fun scrollRight() {
        // Will be implemented with WebViewController reference
    }

    /**
     * Scroll to top of page in the active tab
     */
    fun scrollToTop() {
        // Will be implemented with WebViewController reference
    }

    /**
     * Scroll to bottom of page in the active tab
     */
    fun scrollToBottom() {
        // Will be implemented with WebViewController reference
    }

    /**
     * Update scroll position in database for the active tab
     *
     * @param x Horizontal scroll position
     * @param y Vertical scroll position
     */
    fun updateScrollPosition(x: Int, y: Int) {
        activeTab.value?.let { state ->
            viewModelScope.launch {
                val updatedTab = state.tab.copy(scrollXPosition = x, scrollYPosition = y)
                repository.updateTab(updatedTab)
                    .onSuccess {
                        activeTab.value = state.copy(tab = updatedTab)
                    }
                    .onFailure { e ->
                        error.value = "Failed to save scroll position: ${e.message}"
                    }
            }
        }
    }

    // ========== Zoom Controls ==========

    /**
     * Zoom in on the active tab's WebView
     */
    fun zoomIn() {
        activeTab.value?.let { state ->
            val newZoomLevel = (state.tab.zoomLevel + 1).coerceIn(1, 5)
            setZoomLevel(newZoomLevel)
        }
    }

    /**
     * Zoom out on the active tab's WebView
     */
    fun zoomOut() {
        activeTab.value?.let { state ->
            val newZoomLevel = (state.tab.zoomLevel - 1).coerceIn(1, 5)
            setZoomLevel(newZoomLevel)
        }
    }

    /**
     * Set zoom level for the active tab
     *
     * @param level Zoom level (1-5, where 3 is 100%)
     */
    fun setZoomLevel(level: Int) {
        activeTab.value?.let { state ->
            viewModelScope.launch {
                val validatedLevel = level.coerceIn(1, 5)
                val updatedTab = state.tab.copy(zoomLevel = validatedLevel)
                repository.updateTab(updatedTab)
                    .onSuccess {
                        activeTab.value = state.copy(tab = updatedTab)
                    }
                    .onFailure { e ->
                        error.value = "Failed to set zoom level: ${e.message}"
                    }
            }
        }
    }

    // ========== Desktop Mode ==========

    /**
     * Toggle desktop mode for the active tab
     */
    fun toggleDesktopMode() {
        activeTab.value?.let { state ->
            setDesktopMode(!state.tab.isDesktopMode)
        }
    }

    /**
     * Set desktop mode for the active tab
     *
     * @param enabled Whether to enable desktop mode
     */
    fun setDesktopMode(enabled: Boolean) {
        activeTab.value?.let { state ->
            viewModelScope.launch {
                val updatedTab = state.tab.copy(isDesktopMode = enabled)
                repository.updateTab(updatedTab)
                    .onSuccess {
                        activeTab.value = state.copy(tab = updatedTab)
                    }
                    .onFailure { e ->
                        error.value = "Failed to set desktop mode: ${e.message}"
                    }
            }
        }
    }

    // ========== Page Freeze ==========

    /**
     * Toggle freeze/unfreeze page scrolling
     */
    fun freezePage() {
        // Will be implemented with WebViewController reference
    }
}
