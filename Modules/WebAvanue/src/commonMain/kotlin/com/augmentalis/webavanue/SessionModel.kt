package com.augmentalis.webavanue

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Represents a browsing session that can be saved and restored.
 *
 * A session contains all open tabs at a specific point in time,
 * including their state (URL, scroll position, zoom level, etc.).
 *
 * Sessions are used for:
 * - Restoring tabs after app restart
 * - Crash recovery
 * - Session history/management
 *
 * @property id Unique session identifier
 * @property timestamp When this session was saved
 * @property activeTabId ID of the tab that was active when session was saved
 * @property tabCount Number of tabs in this session
 * @property isCrashRecovery Whether this session is from a crash (vs normal save)
 */
@Serializable
data class Session(
    val id: String,
    val timestamp: Instant,
    val activeTabId: String?,
    val tabCount: Int,
    val isCrashRecovery: Boolean = false
) {
    companion object {
        /**
         * Creates a new session
         *
         * @param activeTabId ID of currently active tab
         * @param tabCount Number of tabs to save
         * @param isCrashRecovery Whether this is a crash recovery session
         */
        fun create(
            activeTabId: String?,
            tabCount: Int,
            isCrashRecovery: Boolean = false
        ): Session {
            val now = kotlinx.datetime.Clock.System.now()
            return Session(
                id = generateSessionId(),
                timestamp = now,
                activeTabId = activeTabId,
                tabCount = tabCount,
                isCrashRecovery = isCrashRecovery
            )
        }

        private fun generateSessionId(): String {
            return "session_${kotlinx.datetime.Clock.System.now().toEpochMilliseconds()}_${(0..9999).random()}"
        }
    }
}

/**
 * Represents a tab's state within a saved session.
 *
 * This stores all information needed to restore a tab to its exact state,
 * including URL, scroll position, zoom level, and visual state.
 *
 * @property sessionId ID of the session this tab belongs to
 * @property tabId Original tab ID (for matching on restore)
 * @property url Current URL
 * @property title Page title
 * @property favicon Favicon URL
 * @property position Tab position in tab bar
 * @property isPinned Whether tab is pinned
 * @property isActive Whether this was the active tab
 * @property scrollX Horizontal scroll position (pixels)
 * @property scrollY Vertical scroll position (pixels)
 * @property zoomLevel Zoom level (1-5, where 3 = 100%)
 * @property isDesktopMode Whether desktop mode is enabled
 * @property isLoaded Whether tab content was loaded (for lazy loading)
 */
@Serializable
data class SessionTab(
    val sessionId: String,
    val tabId: String,
    val url: String,
    val title: String,
    val favicon: String?,
    val position: Int,
    val isPinned: Boolean,
    val isActive: Boolean,
    val scrollX: Int,
    val scrollY: Int,
    val zoomLevel: Int,
    val isDesktopMode: Boolean,
    val isLoaded: Boolean = false
) {
    companion object {
        /**
         * Creates a SessionTab from a regular Tab
         *
         * @param sessionId Session this tab belongs to
         * @param tab Tab to convert
         * @param isActive Whether this is the active tab
         */
        fun fromTab(
            sessionId: String,
            tab: Tab,
            isActive: Boolean
        ): SessionTab {
            return SessionTab(
                sessionId = sessionId,
                tabId = tab.id,
                url = tab.url,
                title = tab.title,
                favicon = tab.favicon,
                position = tab.position,
                isPinned = tab.isPinned,
                isActive = isActive,
                scrollX = tab.scrollXPosition,
                scrollY = tab.scrollYPosition,
                zoomLevel = tab.zoomLevel,
                isDesktopMode = tab.isDesktopMode,
                isLoaded = false
            )
        }
    }

    /**
     * Converts this SessionTab back to a regular Tab
     *
     * @param groupId Optional tab group ID
     */
    fun toTab(groupId: String? = null): Tab {
        val now = kotlinx.datetime.Clock.System.now()
        return Tab(
            id = tabId,
            url = url,
            title = title,
            favicon = favicon,
            isActive = isActive,
            isPinned = isPinned,
            isIncognito = false, // Private tabs are never persisted
            createdAt = now, // Use current time for restored tabs
            lastAccessedAt = now,
            position = position,
            parentTabId = null,
            groupId = groupId,
            sessionData = null,
            scrollXPosition = scrollX,
            scrollYPosition = scrollY,
            zoomLevel = zoomLevel,
            isDesktopMode = isDesktopMode
        )
    }
}
