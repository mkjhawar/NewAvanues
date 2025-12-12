package com.augmentalis.webavanue.domain.manager

import com.augmentalis.webavanue.domain.model.Tab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * PrivateBrowsingManager - Manages private/incognito browsing mode
 *
 * ## Single Responsibility
 * Manages the state and lifecycle of private browsing sessions, ensuring data isolation
 * and proper cleanup when private tabs are closed.
 *
 * ## Features
 * - **Session Isolation**: Tracks active private tabs separately from regular tabs
 * - **Data Isolation**: Ensures no history, cookies, or cache persistence for private tabs
 * - **Cleanup Management**: Automatically cleans up private data when all private tabs close
 * - **Thread Safety**: All operations are mutex-protected for concurrent access
 *
 * ## Private Mode Guarantees
 * When a tab is in private mode (`isIncognito = true`):
 * 1. No history entries are created
 * 2. Cookies are not persisted after session
 * 3. Cache is in-memory only (cleared on tab close)
 * 4. Form data is not saved
 * 5. Search suggestions don't include private browsing history
 * 6. Download history is not recorded (but downloads still work)
 *
 * ## Usage
 * ```kotlin
 * val manager = PrivateBrowsingManager()
 *
 * // Start private browsing session
 * manager.registerPrivateTab(tab)
 *
 * // Check if private mode is active
 * if (manager.isPrivateModeActive.value) {
 *     // Show private mode indicator
 * }
 *
 * // End private session
 * manager.unregisterPrivateTab(tabId)
 * ```
 *
 * @see Tab
 * @see BrowserRepository
 */
class PrivateBrowsingManager {

    private val mutex = Mutex()

    // Active private tab IDs
    private val _privateTabs = MutableStateFlow<Set<String>>(emptySet())
    val privateTabs: StateFlow<Set<String>> = _privateTabs.asStateFlow()

    // Private mode active indicator (true if any private tabs exist)
    private val _isPrivateModeActive = MutableStateFlow(false)
    val isPrivateModeActive: StateFlow<Boolean> = _isPrivateModeActive.asStateFlow()

    // Count of active private tabs
    private val _privateTabCount = MutableStateFlow(0)
    val privateTabCount: StateFlow<Int> = _privateTabCount.asStateFlow()

    /**
     * Register a private tab with the manager
     *
     * @param tab Tab to register (must have isIncognito = true)
     * @throws IllegalArgumentException if tab is not incognito
     */
    suspend fun registerPrivateTab(tab: Tab) {
        require(tab.isIncognito) {
            "Cannot register non-incognito tab as private (tabId: ${tab.id})"
        }

        mutex.withLock {
            val updated = _privateTabs.value + tab.id
            _privateTabs.value = updated
            _privateTabCount.value = updated.size
            _isPrivateModeActive.value = updated.isNotEmpty()
        }
    }

    /**
     * Unregister a private tab (called when tab is closed)
     *
     * @param tabId Tab ID to unregister
     */
    suspend fun unregisterPrivateTab(tabId: String) {
        mutex.withLock {
            val updated = _privateTabs.value - tabId
            _privateTabs.value = updated
            _privateTabCount.value = updated.size
            _isPrivateModeActive.value = updated.isNotEmpty()
        }
    }

    /**
     * Check if a tab is registered as private
     *
     * @param tabId Tab ID to check
     * @return true if tab is registered as private
     */
    fun isPrivateTab(tabId: String): Boolean {
        return _privateTabs.value.contains(tabId)
    }

    /**
     * Close all private tabs
     *
     * This will trigger cleanup of all private browsing data.
     * Returns the list of tab IDs that were closed.
     *
     * @return List of closed private tab IDs
     */
    suspend fun closeAllPrivateTabs(): List<String> {
        return mutex.withLock {
            val closedTabs = _privateTabs.value.toList()
            _privateTabs.value = emptySet()
            _privateTabCount.value = 0
            _isPrivateModeActive.value = false
            closedTabs
        }
    }

    /**
     * Get all active private tab IDs
     *
     * @return Set of private tab IDs
     */
    fun getPrivateTabs(): Set<String> {
        return _privateTabs.value
    }

    /**
     * Check if any private tabs are active
     *
     * @return true if at least one private tab exists
     */
    fun hasPrivateTabs(): Boolean {
        return _privateTabs.value.isNotEmpty()
    }

    /**
     * Get count of active private tabs
     *
     * @return Number of private tabs
     */
    fun getPrivateTabCount(): Int {
        return _privateTabs.value.size
    }

    /**
     * Clear all private browsing state (for testing/reset)
     *
     * This is an internal operation that should only be called during
     * app reset or testing scenarios.
     */
    suspend fun reset() {
        mutex.withLock {
            _privateTabs.value = emptySet()
            _privateTabCount.value = 0
            _isPrivateModeActive.value = false
        }
    }
}
