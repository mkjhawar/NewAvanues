package com.augmentalis.webavanue

import com.augmentalis.webavanue.BrowserSettings
import com.augmentalis.webavanue.Session
import com.augmentalis.webavanue.SessionTab
import com.augmentalis.webavanue.BrowserRepository
import com.augmentalis.webavanue.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Session management operations handler
 *
 * Encapsulates all session-related functionality including:
 * - Save/restore sessions
 * - Crash recovery
 * - App lifecycle handling
 */
class TabSessionManager(
    private val repository: BrowserRepository,
    private val viewModelScope: CoroutineScope,
    private val tabs: MutableStateFlow<List<TabUiState>>,
    private val activeTab: MutableStateFlow<TabUiState?>,
    private val settings: StateFlow<BrowserSettings?>
) {
    /**
     * Save current browsing session.
     * Called automatically on app pause/background.
     *
     * Saves all open tabs (except private tabs) to database for restoration later.
     */
    fun saveSession() {
        viewModelScope.launch {
            try {
                val tabsList = tabs.value.map { it.tab }
                val activeTabId = activeTab.value?.tab?.id

                // Filter out private tabs
                val tabsToSave = tabsList.filter { !it.isIncognito }

                if (tabsToSave.isEmpty()) {
                    Logger.info("TabSessionManager", "No tabs to save (all private)")
                    return@launch
                }

                // Save through repository
                val session = Session.create(
                    activeTabId = activeTabId,
                    tabCount = tabsToSave.size,
                    isCrashRecovery = false
                )

                val sessionTabs = tabsToSave.map { tab ->
                    SessionTab.fromTab(
                        sessionId = session.id,
                        tab = tab,
                        isActive = tab.id == activeTabId
                    )
                }

                repository.saveSession(session, sessionTabs)
                    .onSuccess {
                        Logger.info("TabSessionManager", "Session saved: ${session.id} with ${sessionTabs.size} tabs")
                    }
                    .onFailure { e ->
                        Logger.error("TabSessionManager", "Failed to save session: ${e.message}", e)
                    }
            } catch (e: Exception) {
                Logger.error("TabSessionManager", "Error saving session: ${e.message}", e)
            }
        }
    }

    /**
     * Save current session for crash recovery
     *
     * @param isCrashRecovery Whether this is a crash recovery save
     */
    fun saveCurrentSession(isCrashRecovery: Boolean) {
        viewModelScope.launch {
            try {
                val tabsList = tabs.value.map { it.tab }
                val activeTabId = activeTab.value?.tab?.id

                val tabsToSave = tabsList.filter { !it.isIncognito }
                if (tabsToSave.isEmpty()) return@launch

                val session = Session.create(
                    activeTabId = activeTabId,
                    tabCount = tabsToSave.size,
                    isCrashRecovery = isCrashRecovery
                )

                val sessionTabs = tabsToSave.map { tab ->
                    SessionTab.fromTab(
                        sessionId = session.id,
                        tab = tab,
                        isActive = tab.id == activeTabId
                    )
                }

                repository.saveSession(session, sessionTabs)
            } catch (e: Exception) {
                Logger.error("TabSessionManager", "Error saving crash recovery session: ${e.message}", e)
            }
        }
    }

    /**
     * Get latest crash recovery session info
     *
     * @return Session info or null if none exists
     */
    suspend fun getLatestCrashSession(): Session? {
        return try {
            repository.getLatestCrashSession().getOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Restore the most recent browsing session.
     *
     * @return true if session was restored, false if no session available
     */
    suspend fun restoreSession(): Boolean {
        return try {
            val settingsValue = settings.value
            if (settingsValue?.restoreTabsOnStartup == false) {
                Logger.info("TabSessionManager", "Session restore disabled in settings")
                return false
            }

            val sessionResult = repository.getLatestSession()
            if (sessionResult.isFailure || sessionResult.getOrNull() == null) {
                Logger.info("TabSessionManager", "No session to restore")
                return false
            }

            val session = sessionResult.getOrNull()!!

            if (session.isCrashRecovery) {
                Logger.info("TabSessionManager", "Skipping crash recovery session")
                return false
            }

            val sessionTabsResult = repository.getSessionTabs(session.id)
            if (sessionTabsResult.isFailure || sessionTabsResult.getOrNull()?.isEmpty() != false) {
                Logger.info("TabSessionManager", "Session has no tabs to restore")
                return false
            }

            val sessionTabs = sessionTabsResult.getOrNull()!!

            repository.closeAllTabs()

            val restoredTabs = sessionTabs.map { it.toTab() }
            restoredTabs.forEach { tab ->
                repository.createTab(tab)
            }

            val activeTabToSet = restoredTabs.find { it.isActive } ?: restoredTabs.firstOrNull()
            if (activeTabToSet != null) {
                repository.setActiveTab(activeTabToSet.id)
            }

            Logger.info("TabSessionManager", "Session restored: ${session.id} with ${restoredTabs.size} tabs")
            true
        } catch (e: Exception) {
            Logger.error("TabSessionManager", "Failed to restore session: ${e.message}", e)
            false
        }
    }

    /**
     * Check if there's a crash recovery session available.
     */
    suspend fun hasCrashRecoverySession(): Boolean {
        return try {
            val result = repository.getLatestCrashSession()
            result.isSuccess && result.getOrNull() != null
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Restore crash recovery session.
     *
     * @return true if restored successfully
     */
    suspend fun restoreCrashRecoverySession(): Boolean {
        return try {
            val sessionResult = repository.getLatestCrashSession()
            if (sessionResult.isFailure || sessionResult.getOrNull() == null) {
                return false
            }

            val session = sessionResult.getOrNull()!!

            val sessionTabsResult = repository.getSessionTabs(session.id)
            if (sessionTabsResult.isFailure || sessionTabsResult.getOrNull()?.isEmpty() != false) {
                return false
            }

            val sessionTabs = sessionTabsResult.getOrNull()!!

            repository.closeAllTabs()

            val restoredTabs = sessionTabs.map { it.toTab() }
            restoredTabs.forEach { tab ->
                repository.createTab(tab)
            }

            val activeTabToSet = restoredTabs.find { it.isActive } ?: restoredTabs.firstOrNull()
            if (activeTabToSet != null) {
                repository.setActiveTab(activeTabToSet.id)
            }

            Logger.info("TabSessionManager", "Crash recovery session restored: ${session.id}")
            true
        } catch (e: Exception) {
            Logger.error("TabSessionManager", "Failed to restore crash recovery session: ${e.message}", e)
            false
        }
    }

    /**
     * Dismiss crash recovery session (user chose not to restore).
     */
    suspend fun dismissCrashRecovery() {
        try {
            val crashSessionResult = repository.getLatestCrashSession()
            if (crashSessionResult.isSuccess) {
                val crashSession = crashSessionResult.getOrNull()
                if (crashSession != null) {
                    repository.deleteSession(crashSession.id)
                    Logger.info("TabSessionManager", "Crash recovery session dismissed")
                }
            }
        } catch (e: Exception) {
            Logger.error("TabSessionManager", "Failed to dismiss crash recovery: ${e.message}", e)
        }
    }

    /**
     * Handle app pause - save current session.
     */
    fun onAppPause() {
        saveSession()
    }

    /**
     * Handle app resume - check for crash recovery.
     *
     * @return true if crash recovery session is available
     */
    suspend fun onAppResume(): Boolean {
        return hasCrashRecoverySession()
    }
}
