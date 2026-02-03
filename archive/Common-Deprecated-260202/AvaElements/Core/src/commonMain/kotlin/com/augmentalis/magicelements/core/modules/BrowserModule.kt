package com.augmentalis.magicelements.core.modules

import com.augmentalis.magicelements.core.mel.functions.PluginTier

/**
 * Browser module for AvaCode (@browser).
 * Wraps BrowserAvanue/BrowserConnect capabilities.
 *
 * MEL Usage:
 * ```
 * @browser.open("https://example.com")
 * @browser.search("kotlin multiplatform")
 * @browser.tab.new()
 * @browser.bookmark.add($url, "My Page")
 * ```
 *
 * @param delegate Platform-specific browser implementation (null if unsupported)
 */
class BrowserModule(
    private val delegate: BrowserModuleDelegate?
) : BaseModule("browser", "1.0.0", PluginTier.DATA) {

    init {
        // Navigation - DATA tier (basic), LOGIC tier (control)
        method("open", PluginTier.DATA, "Open URL in browser") { args, _ ->
            requireDelegate()
            val url = args.argString(0, "url")
            val options = args.argOptions(1)
            delegate!!.open(url, options)
        }

        method("back", PluginTier.LOGIC, "Navigate back") { _, _ ->
            requireDelegate()
            delegate!!.back()
        }

        method("forward", PluginTier.LOGIC, "Navigate forward") { _, _ ->
            requireDelegate()
            delegate!!.forward()
        }

        method("reload", PluginTier.LOGIC, "Reload current page") { _, _ ->
            requireDelegate()
            delegate!!.reload()
        }

        method("stop", PluginTier.LOGIC, "Stop loading") { _, _ ->
            requireDelegate()
            delegate!!.stop()
        }

        // Search
        method("search", PluginTier.DATA, "Search the web") { args, _ ->
            requireDelegate()
            val query = args.argString(0, "query")
            val options = args.argOptions(1)
            delegate!!.search(query, options)
        }

        // Tab management
        method("tab.new", PluginTier.LOGIC, "Create new tab") { args, _ ->
            requireDelegate()
            val url = args.argOrNull<String>(0)
            delegate!!.newTab(url)
        }

        method("tab.close", PluginTier.LOGIC, "Close current tab") { args, _ ->
            requireDelegate()
            val tabId = args.argOrNull<String>(0)
            delegate!!.closeTab(tabId)
        }

        method("tab.list", PluginTier.DATA, "List open tabs") { _, _ ->
            requireDelegate()
            delegate!!.listTabs()
        }

        method("tab.switch", PluginTier.LOGIC, "Switch to tab") { args, _ ->
            requireDelegate()
            val tabId = args.argString(0, "tabId")
            delegate!!.switchTab(tabId)
        }

        method("tab.current", PluginTier.DATA, "Get current tab info") { _, _ ->
            requireDelegate()
            delegate!!.getCurrentTab()
        }

        // Bookmarks
        method("bookmark.add", PluginTier.LOGIC, "Add bookmark") { args, _ ->
            requireDelegate()
            val url = args.argString(0, "url")
            val title = args.argOrNull<String>(1)
            val folder = args.argOrNull<String>(2)
            delegate!!.addBookmark(url, title, folder)
        }

        method("bookmark.remove", PluginTier.LOGIC, "Remove bookmark") { args, _ ->
            requireDelegate()
            val url = args.argString(0, "url")
            delegate!!.removeBookmark(url)
        }

        method("bookmark.list", PluginTier.DATA, "List bookmarks") { args, _ ->
            requireDelegate()
            val folder = args.argOrNull<String>(0)
            delegate!!.listBookmarks(folder)
        }

        method("bookmark.folders", PluginTier.DATA, "List bookmark folders") { _, _ ->
            requireDelegate()
            delegate!!.listBookmarkFolders()
        }

        // History
        method("history.list", PluginTier.DATA, "Get browsing history") { args, _ ->
            requireDelegate()
            val limit = args.argOrDefault(0, 50)
            delegate!!.getHistory(limit as Int)
        }

        method("history.search", PluginTier.DATA, "Search history") { args, _ ->
            requireDelegate()
            val query = args.argString(0, "query")
            delegate!!.searchHistory(query)
        }

        method("history.clear", PluginTier.LOGIC, "Clear browsing history") { _, _ ->
            requireDelegate()
            delegate!!.clearHistory()
        }

        // Page interaction (LOGIC tier - requires active page)
        method("page.title", PluginTier.DATA, "Get current page title") { _, _ ->
            requireDelegate()
            delegate!!.getPageTitle()
        }

        method("page.url", PluginTier.DATA, "Get current page URL") { _, _ ->
            requireDelegate()
            delegate!!.getPageUrl()
        }

        method("page.getText", PluginTier.LOGIC, "Get page text content") { _, _ ->
            requireDelegate()
            delegate!!.getPageText()
        }

        method("page.findText", PluginTier.LOGIC, "Find text on page") { args, _ ->
            requireDelegate()
            val query = args.argString(0, "query")
            delegate!!.findText(query)
        }

        method("page.scroll", PluginTier.LOGIC, "Scroll page") { args, _ ->
            requireDelegate()
            val direction = args.argString(0, "direction")
            val amount = args.argOrDefault(1, 100) as Int
            delegate!!.scroll(direction, amount)
        }

        method("page.screenshot", PluginTier.LOGIC, "Take page screenshot") { _, _ ->
            requireDelegate()
            delegate!!.takeScreenshot()
        }

        // Ad blocking
        method("adblock.enable", PluginTier.LOGIC, "Enable ad blocker") { _, _ ->
            requireDelegate()
            delegate!!.enableAdBlock()
        }

        method("adblock.disable", PluginTier.LOGIC, "Disable ad blocker") { _, _ ->
            requireDelegate()
            delegate!!.disableAdBlock()
        }

        method("adblock.isEnabled", PluginTier.DATA, "Check if ad blocker enabled") { _, _ ->
            requireDelegate()
            delegate!!.isAdBlockEnabled()
        }

        method("adblock.whitelist", PluginTier.LOGIC, "Add domain to whitelist") { args, _ ->
            requireDelegate()
            val domain = args.argString(0, "domain")
            delegate!!.whitelistDomain(domain)
        }

        // Incognito
        method("incognito.open", PluginTier.LOGIC, "Open in incognito mode") { args, _ ->
            requireDelegate()
            val url = args.argOrNull<String>(0)
            delegate!!.openIncognito(url)
        }
    }

    private fun requireDelegate() {
        if (delegate == null) {
            throw ModuleException(name, "", "Browser module not available on this platform")
        }
    }
}

/**
 * Platform-specific browser operations.
 * Implemented by BrowserAvanue on Android, Safari on iOS, etc.
 */
interface BrowserModuleDelegate {
    // Navigation
    fun open(url: String, options: Map<String, Any?>?)
    fun back()
    fun forward()
    fun reload()
    fun stop()

    // Search
    fun search(query: String, options: Map<String, Any?>?)

    // Tabs
    fun newTab(url: String?): Map<String, Any?>
    fun closeTab(tabId: String?)
    fun listTabs(): List<Map<String, Any?>>
    fun switchTab(tabId: String)
    fun getCurrentTab(): Map<String, Any?>?

    // Bookmarks
    fun addBookmark(url: String, title: String?, folder: String?): String
    fun removeBookmark(url: String): Boolean
    fun listBookmarks(folder: String?): List<Map<String, Any?>>
    fun listBookmarkFolders(): List<String>

    // History
    fun getHistory(limit: Int): List<Map<String, Any?>>
    fun searchHistory(query: String): List<Map<String, Any?>>
    fun clearHistory()

    // Page
    fun getPageTitle(): String?
    fun getPageUrl(): String?
    suspend fun getPageText(): String?
    fun findText(query: String): Int
    fun scroll(direction: String, amount: Int)
    suspend fun takeScreenshot(): String? // Base64 encoded

    // Ad blocking
    fun enableAdBlock()
    fun disableAdBlock()
    fun isAdBlockEnabled(): Boolean
    fun whitelistDomain(domain: String)

    // Incognito
    fun openIncognito(url: String?)
}
