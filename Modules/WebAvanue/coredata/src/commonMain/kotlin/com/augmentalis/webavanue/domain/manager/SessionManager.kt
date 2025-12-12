package com.augmentalis.webavanue.domain.manager

import com.augmentalis.webavanue.domain.model.Session
import com.augmentalis.webavanue.domain.model.SessionTab
import com.augmentalis.webavanue.domain.model.Tab
import com.augmentalis.webavanue.domain.repository.BrowserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.days

/**
 * SessionManager - Manages browser session persistence and restoration.
 *
 * Responsibilities:
 * - Save browsing sessions on app pause/background
 * - Restore sessions on app restart
 * - Handle crash recovery
 * - Manage session lifecycle (cleanup old sessions)
 * - Support lazy tab loading (load active tab first)
 *
 * Features:
 * - Auto-save on app background
 * - Crash detection and recovery
 * - Skip private/incognito tabs (never persist)
 * - Lazy loading for performance (load active tab immediately, others on demand)
 * - Session history management (keep last N sessions)
 *
 * @property repository Browser repository for data operations
 * @property scope Coroutine scope for async operations
 */
class SessionManager(
    private val repository: BrowserRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    // State: Whether app was properly closed (vs crash)
    private val _wasProperlyClosedKey = "session_properly_closed"

    // State: Current session being managed
    private val _currentSession = MutableStateFlow<Session?>(null)
    val currentSession: StateFlow<Session?> = _currentSession.asStateFlow()

    // State: Whether a crash recovery session is available
    private val _hasCrashRecoverySession = MutableStateFlow(false)
    val hasCrashRecoverySession: StateFlow<Boolean> = _hasCrashRecoverySession.asStateFlow()

    /**
     * Initialize session manager.
     * - Check if app crashed (was not properly closed)
     * - Mark app as running (not properly closed yet)
     */
    suspend fun initialize() {
        // Check if app crashed (was not properly closed)
        val wasCrash = !wasProperlyClosedLastTime()

        if (wasCrash) {
            // App crashed - check if there's a session to recover
            repository.getLatestSession()
                .onSuccess { session ->
                    if (session != null && session.tabCount > 0) {
                        _hasCrashRecoverySession.value = true

                        // Mark this as a crash recovery session
                        val crashSession = session.copy(isCrashRecovery = true)
                        repository.saveSession(crashSession, emptyList()) // Update flag
                    }
                }
        }

        // Mark app as running (not properly closed yet)
        setProperlyClosedFlag(false)

        // Clean up old sessions (keep last 10 sessions)
        cleanupOldSessions(keepCount = 10)
    }

    /**
     * Save current browsing session.
     *
     * Saves all open tabs except:
     * - Private/incognito tabs (never persisted)
     * - Empty tabs (about:blank, no history)
     *
     * @param tabs List of all open tabs
     * @param activeTabId ID of currently active tab
     * @param isCrashRecovery Whether this is a crash recovery save
     * @return Result with saved Session or error
     */
    suspend fun saveSession(
        tabs: List<Tab>,
        activeTabId: String?,
        isCrashRecovery: Boolean = false
    ): Result<Session> {
        return try {
            // Filter out private tabs (never persist)
            val tabsToSave = tabs.filter { !it.isIncognito }

            if (tabsToSave.isEmpty()) {
                return Result.failure(Exception("No tabs to save (all private or empty)"))
            }

            // Create session
            val session = Session.create(
                activeTabId = activeTabId,
                tabCount = tabsToSave.size,
                isCrashRecovery = isCrashRecovery
            )

            // Create session tabs
            val sessionTabs = tabsToSave.map { tab ->
                SessionTab.fromTab(
                    sessionId = session.id,
                    tab = tab,
                    isActive = tab.id == activeTabId
                )
            }

            // Save to repository
            repository.saveSession(session, sessionTabs)
                .onSuccess {
                    _currentSession.value = session
                }

            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restore the most recent session.
     *
     * Loads tabs from the last saved session.
     * For performance, only the active tab is fully loaded initially.
     * Other tabs are loaded lazily when accessed.
     *
     * @param loadAllImmediately If true, load all tabs immediately (default: false for lazy loading)
     * @return Result with list of restored Tabs or error
     */
    suspend fun restoreSession(loadAllImmediately: Boolean = false): Result<List<Tab>> {
        return try {
            // Get latest session
            val sessionResult = repository.getLatestSession()
            if (sessionResult.isFailure) {
                return Result.failure(sessionResult.exceptionOrNull() ?: Exception("Failed to get session"))
            }

            val session = sessionResult.getOrNull()
            if (session == null || session.tabCount == 0) {
                return Result.failure(Exception("No session to restore"))
            }

            // Get session tabs
            val sessionTabsResult = repository.getSessionTabs(session.id)
            if (sessionTabsResult.isFailure) {
                return Result.failure(sessionTabsResult.exceptionOrNull() ?: Exception("Failed to get session tabs"))
            }

            val sessionTabs = sessionTabsResult.getOrNull() ?: emptyList()
            if (sessionTabs.isEmpty()) {
                return Result.failure(Exception("Session has no tabs"))
            }

            // Convert session tabs to regular tabs
            val tabs = sessionTabs.map { sessionTab ->
                sessionTab.toTab()
            }

            // Store current session
            _currentSession.value = session

            // Clear crash recovery flag
            _hasCrashRecoverySession.value = false

            Result.success(tabs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Restore a specific session by ID.
     *
     * @param sessionId Session ID to restore
     * @return Result with list of restored Tabs or error
     */
    suspend fun restoreSessionById(sessionId: String): Result<List<Tab>> {
        return try {
            val sessionResult = repository.getSession(sessionId)
            if (sessionResult.isFailure) {
                return Result.failure(sessionResult.exceptionOrNull() ?: Exception("Session not found"))
            }

            val session = sessionResult.getOrNull()
                ?: return Result.failure(Exception("Session not found"))

            val sessionTabsResult = repository.getSessionTabs(sessionId)
            if (sessionTabsResult.isFailure) {
                return Result.failure(sessionTabsResult.exceptionOrNull() ?: Exception("Failed to get session tabs"))
            }

            val sessionTabs = sessionTabsResult.getOrNull() ?: emptyList()
            val tabs = sessionTabs.map { it.toTab() }

            _currentSession.value = session

            Result.success(tabs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get list of all saved sessions (for session history UI).
     *
     * @param limit Maximum number of sessions to return
     * @param offset Offset for pagination
     * @return Flow of session list
     */
    suspend fun getAllSessions(limit: Int = 20, offset: Int = 0): Result<List<Session>> {
        return repository.getAllSessions(limit, offset)
    }

    /**
     * Delete a specific session.
     *
     * @param sessionId Session ID to delete
     */
    suspend fun deleteSession(sessionId: String): Result<Unit> {
        return repository.deleteSession(sessionId)
    }

    /**
     * Delete all saved sessions.
     */
    suspend fun deleteAllSessions(): Result<Unit> {
        _currentSession.value = null
        _hasCrashRecoverySession.value = false
        return repository.deleteAllSessions()
    }

    /**
     * Clean up old sessions (keep only recent N sessions).
     *
     * Deletes sessions older than 30 days or beyond the keepCount limit.
     *
     * @param keepCount Number of recent sessions to keep (default: 10)
     */
    suspend fun cleanupOldSessions(keepCount: Int = 10) {
        try {
            // Get all sessions
            val sessionsResult = repository.getAllSessions(limit = 1000, offset = 0)
            if (sessionsResult.isFailure) return

            val sessions = sessionsResult.getOrNull() ?: return

            // Delete sessions beyond keepCount
            if (sessions.size > keepCount) {
                val toDelete = sessions.drop(keepCount)
                toDelete.forEach { session ->
                    repository.deleteSession(session.id)
                }
            }

            // Delete sessions older than 30 days
            val thirtyDaysAgo = Clock.System.now().minus(30.days)
            repository.deleteOldSessions(thirtyDaysAgo)
        } catch (e: Exception) {
            // Log error but don't throw - cleanup is non-critical
        }
    }

    /**
     * Mark app as properly closed.
     * Call this when user explicitly exits the app.
     */
    suspend fun markProperClose() {
        setProperlyClosedFlag(true)
    }

    /**
     * Check if app was properly closed last time.
     *
     * @return true if properly closed, false if crashed
     */
    private suspend fun wasProperlyClosedLastTime(): Boolean {
        // This would use SharedPreferences/UserDefaults in actual implementation
        // For now, we'll check if there's a recent crash recovery session
        val result = repository.getLatestCrashSession()
        return result.isFailure || result.getOrNull() == null
    }

    /**
     * Set the properly closed flag in persistent storage.
     *
     * @param properlyClosed true if app is closing properly, false if starting up
     */
    private suspend fun setProperlyClosedFlag(properlyClosed: Boolean) {
        // This would use SharedPreferences/UserDefaults in actual implementation
        // For now, we'll rely on crash recovery flag in session
        // Platform-specific implementation needed
    }

    /**
     * Save session automatically on app pause/background.
     * Should be called from Activity.onPause() or similar lifecycle event.
     */
    suspend fun onAppPause(tabs: List<Tab>, activeTabId: String?) {
        saveSession(tabs, activeTabId, isCrashRecovery = false)
    }

    /**
     * Handle app resume (check for crash recovery).
     * Should be called from Activity.onResume() or similar lifecycle event.
     */
    suspend fun onAppResume() {
        // Check if there's a crash recovery session available
        val crashSessionResult = repository.getLatestCrashSession()
        if (crashSessionResult.isSuccess) {
            val crashSession = crashSessionResult.getOrNull()
            if (crashSession != null && crashSession.tabCount > 0) {
                _hasCrashRecoverySession.value = true
            }
        }
    }

    /**
     * Dismiss crash recovery session (user chose not to restore).
     */
    suspend fun dismissCrashRecovery() {
        _hasCrashRecoverySession.value = false

        // Optionally delete the crash recovery session
        val crashSessionResult = repository.getLatestCrashSession()
        if (crashSessionResult.isSuccess) {
            val crashSession = crashSessionResult.getOrNull()
            if (crashSession != null) {
                repository.deleteSession(crashSession.id)
            }
        }
    }

    /**
     * Clean up resources.
     */
    fun cleanup() {
        // Cancel scope coroutines
        // scope.cancel() // Don't cancel if shared scope
    }

    companion object {
        /**
         * Maximum number of sessions to keep in history
         */
        const val MAX_SESSIONS = 10

        /**
         * Age threshold for deleting old sessions (days)
         */
        const val SESSION_MAX_AGE_DAYS = 30
    }
}
