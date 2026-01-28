package com.augmentalis.webavanue

import io.github.aakira.napier.Napier
import com.augmentalis.webavanue.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Database type mapping extensions for BrowserRepository
 *
 * Extracted from BrowserRepositoryImpl for Single Responsibility compliance.
 * These extension functions convert between domain models and database models.
 */

// ==================== Tab Mappers ====================

internal fun Tab.toDbModel() = com.augmentalis.webavanue.data.Tab(
    id = id, url = url, title = title, favicon = favicon,
    is_active = if (isActive) 1L else 0L,
    is_pinned = if (isPinned) 1L else 0L,
    is_incognito = if (isIncognito) 1L else 0L,
    created_at = createdAt.toEpochMilliseconds(),
    last_accessed_at = lastAccessedAt.toEpochMilliseconds(),
    position = position.toLong(),
    parent_tab_id = parentTabId,
    group_id = groupId,
    session_data = sessionData,
    scroll_x_position = scrollXPosition.toLong(),
    scroll_y_position = scrollYPosition.toLong(),
    zoom_level = zoomLevel.toLong(),
    is_desktop_mode = if (isDesktopMode) 1L else 0L
)

internal fun com.augmentalis.webavanue.data.Tab.toDomainModel() = Tab(
    id = id, url = url, title = title, favicon = favicon,
    isActive = is_active != 0L, isPinned = is_pinned != 0L, isIncognito = is_incognito != 0L,
    createdAt = Instant.fromEpochMilliseconds(created_at),
    lastAccessedAt = Instant.fromEpochMilliseconds(last_accessed_at),
    position = position.toInt(), parentTabId = parent_tab_id, groupId = group_id, sessionData = session_data,
    scrollXPosition = scroll_x_position.toInt(),
    scrollYPosition = scroll_y_position.toInt(),
    zoomLevel = zoom_level.toInt(),
    isDesktopMode = is_desktop_mode != 0L
)

// ==================== Favorite Mappers ====================

internal fun Favorite.toDbModel() = com.augmentalis.webavanue.data.Favorite(
    id = id, url = url, title = title, favicon = favicon, folder_id = folderId,
    description = description,
    created_at = createdAt.toEpochMilliseconds(),
    last_modified_at = lastModifiedAt.toEpochMilliseconds(),
    visit_count = visitCount.toLong(), position = position.toLong()
)

internal fun com.augmentalis.webavanue.data.Favorite.toDomainModel() = Favorite(
    id = id, url = url, title = title, favicon = favicon, folderId = folder_id,
    description = description,
    createdAt = Instant.fromEpochMilliseconds(created_at),
    lastModifiedAt = Instant.fromEpochMilliseconds(last_modified_at),
    visitCount = visit_count.toInt(), position = position.toInt(), tags = emptyList()
)

// ==================== Favorite Folder Mappers ====================

internal fun FavoriteFolder.toDbModel() = com.augmentalis.webavanue.data.Favorite_folder(
    id = id,
    name = name,
    parent_id = parentId,
    icon = icon,
    color = color,
    created_at = createdAt.toEpochMilliseconds(),
    position = position.toLong()
)

internal fun com.augmentalis.webavanue.data.Favorite_folder.toDomainModel() = FavoriteFolder(
    id = id,
    name = name,
    parentId = parent_id,
    icon = icon,
    color = color,
    createdAt = Instant.fromEpochMilliseconds(created_at),
    position = position.toInt()
)

// ==================== History Mappers ====================

internal fun HistoryEntry.toDbModel() = com.augmentalis.webavanue.data.History_entry(
    id = id, url = url, title = title, favicon = favicon,
    visited_at = visitedAt.toEpochMilliseconds(),
    visit_count = visitCount.toLong(), visit_duration = visitDuration.toLong(),
    referrer = referrer, search_terms = searchTerms,
    is_incognito = if (isIncognito) 1L else 0L, device_id = deviceId
)

internal fun com.augmentalis.webavanue.data.History_entry.toDomainModel() = HistoryEntry(
    id = id, url = url, title = title, favicon = favicon,
    visitedAt = Instant.fromEpochMilliseconds(visited_at),
    visitCount = visit_count.toInt(), visitDuration = visit_duration,
    referrer = referrer, searchTerms = search_terms,
    isIncognito = is_incognito != 0L, deviceId = device_id
)

// ==================== Download Mappers ====================

internal fun Download.toDbModel() = com.augmentalis.webavanue.data.Download(
    id = id,
    url = url,
    filename = filename,
    filepath = filepath,
    mime_type = mimeType,
    file_size = fileSize,
    downloaded_size = downloadedSize,
    status = status.name,
    error_message = errorMessage,
    download_manager_id = downloadManagerId,
    created_at = createdAt.toEpochMilliseconds(),
    completed_at = completedAt?.toEpochMilliseconds(),
    source_page_url = sourcePageUrl,
    source_page_title = sourcePageTitle
)

internal fun com.augmentalis.webavanue.data.Download.toDomainModel() = Download(
    id = id,
    url = url,
    filename = filename,
    filepath = filepath,
    mimeType = mime_type,
    fileSize = file_size,
    downloadedSize = downloaded_size,
    status = DownloadStatus.fromString(status),
    errorMessage = error_message,
    downloadManagerId = download_manager_id,
    createdAt = Instant.fromEpochMilliseconds(created_at),
    completedAt = completed_at?.let { Instant.fromEpochMilliseconds(it) },
    sourcePageUrl = source_page_url,
    sourcePageTitle = source_page_title
)

// ==================== Settings Mappers ====================

/**
 * Safe enum parsing helper to prevent crashes from invalid DB values
 */
internal inline fun <reified T : Enum<T>> safeEnumValueOf(value: String, default: T): T {
    return try {
        enumValueOf<T>(value)
    } catch (e: IllegalArgumentException) {
        Napier.w("Invalid enum value '$value' for ${T::class.simpleName}, using default: $default", tag = "DbMappers")
        default
    }
}

internal fun com.augmentalis.webavanue.data.Browser_settings.toDomainModel() = BrowserSettings(
    theme = safeEnumValueOf(theme, BrowserSettings.Theme.SYSTEM),
    fontSize = safeEnumValueOf(font_size, BrowserSettings.FontSize.MEDIUM),
    forceZoom = force_zoom != 0L, showImages = show_images != 0L,
    useDesktopMode = use_desktop_mode != 0L, blockPopups = block_popups != 0L,
    blockAds = block_ads != 0L, blockTrackers = block_trackers != 0L,
    doNotTrack = do_not_track != 0L, clearCacheOnExit = clear_cache_on_exit != 0L,
    clearHistoryOnExit = clear_history_on_exit != 0L, clearCookiesOnExit = clear_cookies_on_exit != 0L,
    enableCookies = enable_cookies != 0L, enableJavaScript = enable_javascript != 0L,
    enableWebRTC = enable_webrtc != 0L,
    defaultSearchEngine = safeEnumValueOf(default_search_engine, BrowserSettings.SearchEngine.GOOGLE),
    customSearchEngineName = custom_search_engine_name,
    customSearchEngineUrl = custom_search_engine_url,
    searchSuggestions = search_suggestions != 0L, voiceSearch = voice_search != 0L,
    homePage = home_page, newTabPage = safeEnumValueOf(new_tab_page, BrowserSettings.NewTabPage.BLANK),
    restoreTabsOnStartup = restore_tabs_on_startup != 0L,
    openLinksInBackground = open_links_in_background != 0L,
    openLinksInNewTab = open_links_in_new_tab != 0L,
    downloadPath = download_path, askDownloadLocation = ask_download_location != 0L,
    downloadOverWiFiOnly = download_over_wifi_only != 0L, syncEnabled = sync_enabled != 0L,
    syncBookmarks = sync_bookmarks != 0L, syncHistory = sync_history != 0L,
    syncPasswords = sync_passwords != 0L, syncSettings = sync_settings != 0L,
    hardwareAcceleration = hardware_acceleration != 0L, preloadPages = preload_pages != 0L,
    dataSaver = data_saver != 0L, autoPlay = safeEnumValueOf(auto_play, BrowserSettings.AutoPlay.NEVER),
    textReflow = text_reflow != 0L, enableVoiceCommands = enable_voice_commands != 0L,
    aiSummaries = ai_summaries != 0L, aiTranslation = ai_translation != 0L,
    readAloud = read_aloud != 0L
)

// ==================== Site Permission Mappers ====================

internal fun com.augmentalis.webavanue.data.Site_permission.toDomainModel() = SitePermission(
    domain = domain,
    permissionType = permission_type,
    granted = granted != 0L,
    timestamp = Instant.fromEpochMilliseconds(timestamp)
)

// ==================== Session Mappers ====================

internal fun Session.toDbModel() = com.augmentalis.webavanue.data.Session(
    id = id,
    timestamp = timestamp.toEpochMilliseconds(),
    active_tab_id = activeTabId,
    tab_count = tabCount.toLong(),
    is_crash_recovery = if (isCrashRecovery) 1L else 0L
)

internal fun com.augmentalis.webavanue.data.Session.toDomainModel() = Session(
    id = id,
    timestamp = Instant.fromEpochMilliseconds(timestamp),
    activeTabId = active_tab_id,
    tabCount = tab_count.toInt(),
    isCrashRecovery = is_crash_recovery != 0L
)

internal fun SessionTab.toDbModel() = com.augmentalis.webavanue.data.Session_tab(
    session_id = sessionId,
    tab_id = tabId,
    url = url,
    title = title,
    favicon = favicon,
    position = position.toLong(),
    is_pinned = if (isPinned) 1L else 0L,
    is_active = if (isActive) 1L else 0L,
    scroll_x = scrollX.toLong(),
    scroll_y = scrollY.toLong(),
    zoom_level = zoomLevel.toLong(),
    is_desktop_mode = if (isDesktopMode) 1L else 0L,
    is_loaded = if (isLoaded) 1L else 0L
)

internal fun com.augmentalis.webavanue.data.Session_tab.toDomainModel() = SessionTab(
    sessionId = session_id,
    tabId = tab_id,
    url = url,
    title = title,
    favicon = favicon,
    position = position.toInt(),
    isPinned = is_pinned != 0L,
    isActive = is_active != 0L,
    scrollX = scroll_x.toInt(),
    scrollY = scroll_y.toInt(),
    zoomLevel = zoom_level.toInt(),
    isDesktopMode = is_desktop_mode != 0L,
    isLoaded = is_loaded != 0L
)

// ==================== Most Visited Helper ====================

internal fun com.augmentalis.webavanue.data.SelectMostVisited.toHistoryEntry() = HistoryEntry(
    id = "",
    url = url,
    title = title,
    favicon = favicon,
    visitedAt = Clock.System.now(),
    visitCount = max_visits?.toInt() ?: 0,
    visitDuration = 0,
    referrer = null,
    searchTerms = null,
    isIncognito = false,
    deviceId = null
)
