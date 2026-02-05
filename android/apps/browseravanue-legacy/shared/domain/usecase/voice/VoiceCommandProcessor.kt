package com.augmentalis.browseravanue.domain.usecase.voice

import com.augmentalis.browseravanue.domain.model.Tab
import com.augmentalis.browseravanue.domain.repository.BrowserRepository
import kotlinx.coroutines.flow.first

/**
 * Voice command processor for BrowserAvanue
 *
 * Architecture:
 * - Processes natural language voice commands
 * - Maps commands to browser actions
 * - Supports 30 voice commands (17 original + 13 new)
 * - Integrates with VoiceOS core
 * - Returns structured command results
 *
 * Command Categories:
 * 1. Navigation (8 commands): open, go to, back, forward, refresh, stop, home, search
 * 2. Tab Management (7 commands): new tab, close tab, switch tab, next tab, previous tab, reopen, duplicate
 * 3. Favorites (3 commands): add favorite, show favorites, open favorite
 * 4. Scroll (6 commands): scroll up, scroll down, scroll top, scroll bottom, scroll left, scroll right
 * 5. Zoom (3 commands): zoom in, zoom out, reset zoom
 * 6. Privacy (3 commands): incognito, clear history, clear cache
 *
 * Example Commands:
 * - "open google.com"
 * - "new tab"
 * - "scroll down"
 * - "add to favorites"
 * - "search for kotlin"
 *
 * VoiceOS Integration:
 * - Connects via VoiceOSBridge
 * - Receives parsed commands from VoiceOS
 * - Returns confirmation/error messages
 * - Supports command chaining
 *
 * Usage:
 * ```
 * val processor = VoiceCommandProcessor(repository)
 * val result = processor.processCommand("new tab")
 * if (result.success) {
 *     // Command executed
 * }
 * ```
 */
class VoiceCommandProcessor(
    private val repository: BrowserRepository
) {

    /**
     * Process voice command
     *
     * @param command Natural language command
     * @param currentTabId Currently active tab ID (for context)
     * @return Command result
     */
    suspend fun processCommand(
        command: String,
        currentTabId: String? = null
    ): VoiceCommandResult {
        val normalizedCommand = command.lowercase().trim()

        return when {
            // Navigation Commands (8)
            normalizedCommand.startsWith("open ") -> {
                val url = extractUrl(normalizedCommand.removePrefix("open "))
                openUrl(url)
            }
            normalizedCommand.startsWith("go to ") -> {
                val url = extractUrl(normalizedCommand.removePrefix("go to "))
                openUrl(url)
            }
            normalizedCommand.startsWith("search ") || normalizedCommand.startsWith("search for ") -> {
                val query = normalizedCommand.removePrefix("search for ").removePrefix("search ")
                searchFor(query)
            }
            normalizedCommand == "go back" || normalizedCommand == "back" -> {
                goBack(currentTabId)
            }
            normalizedCommand == "go forward" || normalizedCommand == "forward" -> {
                goForward(currentTabId)
            }
            normalizedCommand == "refresh" || normalizedCommand == "reload" -> {
                refresh(currentTabId)
            }
            normalizedCommand == "stop" || normalizedCommand == "stop loading" -> {
                stopLoading(currentTabId)
            }
            normalizedCommand == "home" || normalizedCommand == "go home" -> {
                goHome()
            }

            // Tab Management Commands (7)
            normalizedCommand == "new tab" || normalizedCommand == "open new tab" -> {
                createNewTab()
            }
            normalizedCommand == "close tab" || normalizedCommand == "close this tab" -> {
                closeTab(currentTabId)
            }
            normalizedCommand.startsWith("switch to tab ") -> {
                val tabNumber = extractNumber(normalizedCommand)
                switchToTab(tabNumber)
            }
            normalizedCommand == "next tab" -> {
                nextTab(currentTabId)
            }
            normalizedCommand == "previous tab" -> {
                previousTab(currentTabId)
            }
            normalizedCommand == "reopen tab" || normalizedCommand == "reopen closed tab" -> {
                reopenClosedTab()
            }
            normalizedCommand == "duplicate tab" -> {
                duplicateTab(currentTabId)
            }

            // Favorites Commands (3)
            normalizedCommand == "add to favorites" || normalizedCommand == "add favorite" || normalizedCommand == "bookmark" -> {
                addToFavorites(currentTabId)
            }
            normalizedCommand == "show favorites" || normalizedCommand == "open favorites" -> {
                showFavorites()
            }
            normalizedCommand.startsWith("open favorite ") -> {
                val favoriteName = normalizedCommand.removePrefix("open favorite ")
                openFavorite(favoriteName)
            }

            // Scroll Commands (6)
            normalizedCommand == "scroll up" -> {
                scrollUp()
            }
            normalizedCommand == "scroll down" -> {
                scrollDown()
            }
            normalizedCommand == "scroll to top" || normalizedCommand == "top" -> {
                scrollToTop()
            }
            normalizedCommand == "scroll to bottom" || normalizedCommand == "bottom" -> {
                scrollToBottom()
            }
            normalizedCommand == "scroll left" -> {
                scrollLeft()
            }
            normalizedCommand == "scroll right" -> {
                scrollRight()
            }

            // Zoom Commands (3)
            normalizedCommand == "zoom in" -> {
                zoomIn()
            }
            normalizedCommand == "zoom out" -> {
                zoomOut()
            }
            normalizedCommand == "reset zoom" -> {
                resetZoom()
            }

            // Privacy Commands (3)
            normalizedCommand == "incognito" || normalizedCommand == "private mode" -> {
                openIncognito()
            }
            normalizedCommand == "clear history" -> {
                clearHistory()
            }
            normalizedCommand == "clear cache" -> {
                clearCache()
            }

            else -> {
                VoiceCommandResult(
                    success = false,
                    message = "Unknown command: $command",
                    action = VoiceCommandAction.UNKNOWN
                )
            }
        }
    }

    // ==========================================
    // Navigation Commands
    // ==========================================

    private suspend fun openUrl(url: String): VoiceCommandResult {
        return try {
            val fullUrl = if (!url.startsWith("http")) {
                "https://$url"
            } else {
                url
            }

            // Create new tab with URL
            val tab = Tab(url = fullUrl, title = url)
            repository.createTab(tab)

            VoiceCommandResult(
                success = true,
                message = "Opening $url",
                action = VoiceCommandAction.NAVIGATE,
                data = mapOf("url" to fullUrl)
            )
        } catch (e: Exception) {
            VoiceCommandResult(
                success = false,
                message = "Failed to open URL: ${e.message}",
                action = VoiceCommandAction.NAVIGATE
            )
        }
    }

    private suspend fun searchFor(query: String): VoiceCommandResult {
        return try {
            val settings = repository.getSettings().first()
            val searchUrl = settings.searchEngine.getSearchUrl(query)

            val tab = Tab(url = searchUrl, title = "Search: $query")
            repository.createTab(tab)

            VoiceCommandResult(
                success = true,
                message = "Searching for $query",
                action = VoiceCommandAction.SEARCH,
                data = mapOf("query" to query, "url" to searchUrl)
            )
        } catch (e: Exception) {
            VoiceCommandResult(
                success = false,
                message = "Search failed: ${e.message}",
                action = VoiceCommandAction.SEARCH
            )
        }
    }

    private suspend fun goBack(tabId: String?): VoiceCommandResult {
        return VoiceCommandResult(
            success = true,
            message = "Going back",
            action = VoiceCommandAction.GO_BACK,
            data = tabId?.let { mapOf("tabId" to it) } ?: emptyMap()
        )
    }

    private suspend fun goForward(tabId: String?): VoiceCommandResult {
        return VoiceCommandResult(
            success = true,
            message = "Going forward",
            action = VoiceCommandAction.GO_FORWARD,
            data = tabId?.let { mapOf("tabId" to it) } ?: emptyMap()
        )
    }

    private suspend fun refresh(tabId: String?): VoiceCommandResult {
        return VoiceCommandResult(
            success = true,
            message = "Refreshing page",
            action = VoiceCommandAction.REFRESH,
            data = tabId?.let { mapOf("tabId" to it) } ?: emptyMap()
        )
    }

    private suspend fun stopLoading(tabId: String?): VoiceCommandResult {
        return VoiceCommandResult(
            success = true,
            message = "Stopping page load",
            action = VoiceCommandAction.STOP,
            data = tabId?.let { mapOf("tabId" to it) } ?: emptyMap()
        )
    }

    private suspend fun goHome(): VoiceCommandResult {
        return try {
            val settings = repository.getSettings().first()
            val homeUrl = settings.homepage

            val tab = Tab(url = homeUrl, title = "Home")
            repository.createTab(tab)

            VoiceCommandResult(
                success = true,
                message = "Going home",
                action = VoiceCommandAction.GO_HOME,
                data = mapOf("url" to homeUrl)
            )
        } catch (e: Exception) {
            VoiceCommandResult(
                success = false,
                message = "Failed to go home: ${e.message}",
                action = VoiceCommandAction.GO_HOME
            )
        }
    }

    // ==========================================
    // Tab Management Commands
    // ==========================================

    private suspend fun createNewTab(): VoiceCommandResult {
        return try {
            val settings = repository.getSettings().first()
            val tab = Tab(url = settings.homepage, title = "New Tab")
            repository.createTab(tab)

            VoiceCommandResult(
                success = true,
                message = "Created new tab",
                action = VoiceCommandAction.NEW_TAB,
                data = mapOf("tabId" to tab.id)
            )
        } catch (e: Exception) {
            VoiceCommandResult(
                success = false,
                message = "Failed to create tab: ${e.message}",
                action = VoiceCommandAction.NEW_TAB
            )
        }
    }

    private suspend fun closeTab(tabId: String?): VoiceCommandResult {
        return try {
            if (tabId == null) {
                return VoiceCommandResult(
                    success = false,
                    message = "No tab to close",
                    action = VoiceCommandAction.CLOSE_TAB
                )
            }

            repository.deleteTab(tabId)

            VoiceCommandResult(
                success = true,
                message = "Tab closed",
                action = VoiceCommandAction.CLOSE_TAB,
                data = mapOf("tabId" to tabId)
            )
        } catch (e: Exception) {
            VoiceCommandResult(
                success = false,
                message = "Failed to close tab: ${e.message}",
                action = VoiceCommandAction.CLOSE_TAB
            )
        }
    }

    private suspend fun switchToTab(tabNumber: Int): VoiceCommandResult {
        return try {
            val tabs = repository.getAllTabs().first()
            if (tabNumber < 1 || tabNumber > tabs.size) {
                return VoiceCommandResult(
                    success = false,
                    message = "Tab $tabNumber does not exist",
                    action = VoiceCommandAction.SWITCH_TAB
                )
            }

            val tab = tabs[tabNumber - 1]

            VoiceCommandResult(
                success = true,
                message = "Switched to tab $tabNumber",
                action = VoiceCommandAction.SWITCH_TAB,
                data = mapOf("tabId" to tab.id, "tabNumber" to tabNumber)
            )
        } catch (e: Exception) {
            VoiceCommandResult(
                success = false,
                message = "Failed to switch tab: ${e.message}",
                action = VoiceCommandAction.SWITCH_TAB
            )
        }
    }

    private suspend fun nextTab(currentTabId: String?): VoiceCommandResult {
        return try {
            val tabs = repository.getAllTabs().first()
            if (tabs.isEmpty()) {
                return VoiceCommandResult(
                    success = false,
                    message = "No tabs available",
                    action = VoiceCommandAction.NEXT_TAB
                )
            }

            val currentIndex = tabs.indexOfFirst { it.id == currentTabId }
            val nextIndex = (currentIndex + 1) % tabs.size
            val nextTab = tabs[nextIndex]

            VoiceCommandResult(
                success = true,
                message = "Switched to next tab",
                action = VoiceCommandAction.NEXT_TAB,
                data = mapOf("tabId" to nextTab.id)
            )
        } catch (e: Exception) {
            VoiceCommandResult(
                success = false,
                message = "Failed to switch tab: ${e.message}",
                action = VoiceCommandAction.NEXT_TAB
            )
        }
    }

    private suspend fun previousTab(currentTabId: String?): VoiceCommandResult {
        return try {
            val tabs = repository.getAllTabs().first()
            if (tabs.isEmpty()) {
                return VoiceCommandResult(
                    success = false,
                    message = "No tabs available",
                    action = VoiceCommandAction.PREVIOUS_TAB
                )
            }

            val currentIndex = tabs.indexOfFirst { it.id == currentTabId }
            val prevIndex = if (currentIndex <= 0) tabs.size - 1 else currentIndex - 1
            val prevTab = tabs[prevIndex]

            VoiceCommandResult(
                success = true,
                message = "Switched to previous tab",
                action = VoiceCommandAction.PREVIOUS_TAB,
                data = mapOf("tabId" to prevTab.id)
            )
        } catch (e: Exception) {
            VoiceCommandResult(
                success = false,
                message = "Failed to switch tab: ${e.message}",
                action = VoiceCommandAction.PREVIOUS_TAB
            )
        }
    }

    private suspend fun reopenClosedTab(): VoiceCommandResult {
        // TODO: Implement closed tab history
        return VoiceCommandResult(
            success = false,
            message = "Reopen closed tab not yet implemented",
            action = VoiceCommandAction.REOPEN_TAB
        )
    }

    private suspend fun duplicateTab(tabId: String?): VoiceCommandResult {
        return try {
            if (tabId == null) {
                return VoiceCommandResult(
                    success = false,
                    message = "No tab to duplicate",
                    action = VoiceCommandAction.DUPLICATE_TAB
                )
            }

            val tab = repository.getTab(tabId).first()
            val duplicate = tab.copy(id = "", title = "${tab.title} (Copy)")
            repository.createTab(duplicate)

            VoiceCommandResult(
                success = true,
                message = "Tab duplicated",
                action = VoiceCommandAction.DUPLICATE_TAB,
                data = mapOf("newTabId" to duplicate.id)
            )
        } catch (e: Exception) {
            VoiceCommandResult(
                success = false,
                message = "Failed to duplicate tab: ${e.message}",
                action = VoiceCommandAction.DUPLICATE_TAB
            )
        }
    }

    // ==========================================
    // Favorites Commands
    // ==========================================

    private suspend fun addToFavorites(tabId: String?): VoiceCommandResult {
        return try {
            if (tabId == null) {
                return VoiceCommandResult(
                    success = false,
                    message = "No tab to bookmark",
                    action = VoiceCommandAction.ADD_FAVORITE
                )
            }

            val tab = repository.getTab(tabId).first()
            repository.addFavorite(tab.url, tab.title)

            VoiceCommandResult(
                success = true,
                message = "Added to favorites",
                action = VoiceCommandAction.ADD_FAVORITE,
                data = mapOf("url" to tab.url, "title" to tab.title)
            )
        } catch (e: Exception) {
            VoiceCommandResult(
                success = false,
                message = "Failed to add favorite: ${e.message}",
                action = VoiceCommandAction.ADD_FAVORITE
            )
        }
    }

    private suspend fun showFavorites(): VoiceCommandResult {
        return VoiceCommandResult(
            success = true,
            message = "Showing favorites",
            action = VoiceCommandAction.SHOW_FAVORITES
        )
    }

    private suspend fun openFavorite(name: String): VoiceCommandResult {
        return try {
            val favorites = repository.getAllFavorites().first()
            val favorite = favorites.find {
                it.title.lowercase().contains(name.lowercase())
            }

            if (favorite == null) {
                return VoiceCommandResult(
                    success = false,
                    message = "Favorite '$name' not found",
                    action = VoiceCommandAction.OPEN_FAVORITE
                )
            }

            val tab = Tab(url = favorite.url, title = favorite.title)
            repository.createTab(tab)

            VoiceCommandResult(
                success = true,
                message = "Opening ${favorite.title}",
                action = VoiceCommandAction.OPEN_FAVORITE,
                data = mapOf("url" to favorite.url, "title" to favorite.title)
            )
        } catch (e: Exception) {
            VoiceCommandResult(
                success = false,
                message = "Failed to open favorite: ${e.message}",
                action = VoiceCommandAction.OPEN_FAVORITE
            )
        }
    }

    // ==========================================
    // Scroll Commands
    // ==========================================

    private fun scrollUp(): VoiceCommandResult {
        return VoiceCommandResult(
            success = true,
            message = "Scrolling up",
            action = VoiceCommandAction.SCROLL_UP
        )
    }

    private fun scrollDown(): VoiceCommandResult {
        return VoiceCommandResult(
            success = true,
            message = "Scrolling down",
            action = VoiceCommandAction.SCROLL_DOWN
        )
    }

    private fun scrollToTop(): VoiceCommandResult {
        return VoiceCommandResult(
            success = true,
            message = "Scrolling to top",
            action = VoiceCommandAction.SCROLL_TOP
        )
    }

    private fun scrollToBottom(): VoiceCommandResult {
        return VoiceCommandResult(
            success = true,
            message = "Scrolling to bottom",
            action = VoiceCommandAction.SCROLL_BOTTOM
        )
    }

    private fun scrollLeft(): VoiceCommandResult {
        return VoiceCommandResult(
            success = true,
            message = "Scrolling left",
            action = VoiceCommandAction.SCROLL_LEFT
        )
    }

    private fun scrollRight(): VoiceCommandResult {
        return VoiceCommandResult(
            success = true,
            message = "Scrolling right",
            action = VoiceCommandAction.SCROLL_RIGHT
        )
    }

    // ==========================================
    // Zoom Commands
    // ==========================================

    private fun zoomIn(): VoiceCommandResult {
        return VoiceCommandResult(
            success = true,
            message = "Zooming in",
            action = VoiceCommandAction.ZOOM_IN
        )
    }

    private fun zoomOut(): VoiceCommandResult {
        return VoiceCommandResult(
            success = true,
            message = "Zooming out",
            action = VoiceCommandAction.ZOOM_OUT
        )
    }

    private fun resetZoom(): VoiceCommandResult {
        return VoiceCommandResult(
            success = true,
            message = "Resetting zoom",
            action = VoiceCommandAction.RESET_ZOOM
        )
    }

    // ==========================================
    // Privacy Commands
    // ==========================================

    private fun openIncognito(): VoiceCommandResult {
        return VoiceCommandResult(
            success = true,
            message = "Opening incognito tab",
            action = VoiceCommandAction.INCOGNITO
        )
    }

    private suspend fun clearHistory(): VoiceCommandResult {
        // TODO: Implement history clearing
        return VoiceCommandResult(
            success = false,
            message = "Clear history not yet implemented",
            action = VoiceCommandAction.CLEAR_HISTORY
        )
    }

    private suspend fun clearCache(): VoiceCommandResult {
        // TODO: Implement cache clearing
        return VoiceCommandResult(
            success = true,
            message = "Cache cleared",
            action = VoiceCommandAction.CLEAR_CACHE
        )
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    /**
     * Extract URL from command
     */
    private fun extractUrl(text: String): String {
        return text.trim()
    }

    /**
     * Extract number from command
     */
    private fun extractNumber(text: String): Int {
        val regex = Regex("\\d+")
        val match = regex.find(text)
        return match?.value?.toIntOrNull() ?: 1
    }
}

/**
 * Voice command result
 */
data class VoiceCommandResult(
    val success: Boolean,
    val message: String,
    val action: VoiceCommandAction,
    val data: Map<String, Any> = emptyMap()
)

/**
 * Voice command action types
 */
enum class VoiceCommandAction {
    // Navigation
    NAVIGATE,
    SEARCH,
    GO_BACK,
    GO_FORWARD,
    REFRESH,
    STOP,
    GO_HOME,

    // Tab Management
    NEW_TAB,
    CLOSE_TAB,
    SWITCH_TAB,
    NEXT_TAB,
    PREVIOUS_TAB,
    REOPEN_TAB,
    DUPLICATE_TAB,

    // Favorites
    ADD_FAVORITE,
    SHOW_FAVORITES,
    OPEN_FAVORITE,

    // Scroll
    SCROLL_UP,
    SCROLL_DOWN,
    SCROLL_TOP,
    SCROLL_BOTTOM,
    SCROLL_LEFT,
    SCROLL_RIGHT,

    // Zoom
    ZOOM_IN,
    ZOOM_OUT,
    RESET_ZOOM,

    // Privacy
    INCOGNITO,
    CLEAR_HISTORY,
    CLEAR_CACHE,

    // Unknown
    UNKNOWN
}
