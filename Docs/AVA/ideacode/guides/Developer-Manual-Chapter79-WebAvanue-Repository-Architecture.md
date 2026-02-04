# Developer Manual - Chapter 79: WebAvanue Repository Architecture

**Module:** `Modules/WebAvanue`
**Package:** `com.augmentalis.webavanue.repository`

---

## Overview

The WebAvanue repository layer has been refactored from a monolithic 1,264-line `BrowserRepositoryImpl` into 7 focused domain-specific repositories. This follows the Single Responsibility Principle and improves testability, maintainability, and code clarity.

## Architecture

```
BrowserRepositoryImpl (Facade)
        │
        ├── TabRepository (~230 lines)
        ├── FavoriteRepository (~220 lines)
        ├── HistoryRepository (~175 lines)
        ├── DownloadRepository (~195 lines)
        ├── SettingsRepository (~145 lines)
        ├── SessionRepository (~135 lines)
        └── SitePermissionRepository (~80 lines)
```

## Repository Responsibilities

### TabRepository

Handles browser tab operations:
- Tab CRUD (create, read, update, delete)
- Tab state management (active, pinned)
- Tab reordering
- Reactive observation via `Flow<List<Tab>>`

```kotlin
interface TabRepository {
    suspend fun createTab(tab: Tab): Result<Tab>
    suspend fun getTab(tabId: String): Result<Tab?>
    suspend fun getAllTabs(): Result<List<Tab>>
    suspend fun getRecentTabs(limit: Int): Result<List<Tab>>
    fun observeTabs(): Flow<List<Tab>>
    suspend fun updateTab(tab: Tab): Result<Unit>
    suspend fun closeTab(tabId: String): Result<Unit>
    suspend fun closeTabs(tabIds: List<String>): Result<Unit>
    suspend fun closeAllTabs(): Result<Unit>
    suspend fun setActiveTab(tabId: String): Result<Unit>
    suspend fun setPinned(tabId: String, isPinned: Boolean): Result<Unit>
    suspend fun reorderTabs(tabIds: List<String>): Result<Unit>
}
```

### FavoriteRepository

Handles bookmark/favorite operations:
- Favorite CRUD
- Folder management
- Search functionality
- Reactive observation

```kotlin
interface FavoriteRepository {
    suspend fun addFavorite(favorite: Favorite): Result<Favorite>
    suspend fun getFavorite(favoriteId: String): Result<Favorite?>
    suspend fun getAllFavorites(): Result<List<Favorite>>
    suspend fun getFavoritesInFolder(folderId: String?): Result<List<Favorite>>
    fun observeFavorites(): Flow<List<Favorite>>
    suspend fun updateFavorite(favorite: Favorite): Result<Unit>
    suspend fun removeFavorite(favoriteId: String): Result<Unit>
    suspend fun removeFavorites(favoriteIds: List<String>): Result<Unit>
    suspend fun isFavorite(url: String): Result<Boolean>
    suspend fun searchFavorites(query: String): Result<List<Favorite>>
    suspend fun createFolder(folder: FavoriteFolder): Result<FavoriteFolder>
    suspend fun getAllFolders(): Result<List<FavoriteFolder>>
    suspend fun deleteFolder(folderId: String, deleteContents: Boolean): Result<Unit>
}
```

### HistoryRepository

Handles browsing history:
- History entry CRUD
- Date range queries
- Most visited sites
- Search functionality
- Private browsing support (skips incognito entries)

```kotlin
interface HistoryRepository {
    suspend fun addHistoryEntry(entry: HistoryEntry): Result<HistoryEntry>
    suspend fun getHistory(limit: Int, offset: Int): Result<List<HistoryEntry>>
    suspend fun getHistoryByDateRange(startDate: Instant, endDate: Instant): Result<List<HistoryEntry>>
    suspend fun searchHistory(query: String, limit: Int): Result<List<HistoryEntry>>
    suspend fun getMostVisited(limit: Int): Result<List<HistoryEntry>>
    fun observeHistory(): Flow<List<HistoryEntry>>
    suspend fun deleteHistoryEntry(entryId: String): Result<Unit>
    suspend fun deleteHistoryForUrl(url: String): Result<Unit>
    suspend fun clearHistoryByDateRange(startDate: Instant, endDate: Instant): Result<Unit>
    suspend fun clearAllHistory(): Result<Unit>
}
```

### DownloadRepository

Handles download management:
- Download CRUD
- Progress tracking
- Status management (pending, downloading, completed, failed)
- Android DownloadManager integration

```kotlin
interface DownloadRepository {
    fun observeDownloads(): Flow<List<Download>>
    suspend fun addDownload(download: Download): Result<Download>
    suspend fun getDownload(downloadId: String): Result<Download?>
    suspend fun getDownloadByManagerId(managerId: Long): Download?
    suspend fun getAllDownloads(limit: Int, offset: Int): Result<List<Download>>
    suspend fun getDownloadsByStatus(status: DownloadStatus): Result<List<Download>>
    suspend fun updateDownload(download: Download): Result<Unit>
    suspend fun updateDownloadProgress(downloadId: String, downloadedSize: Long, status: DownloadStatus): Result<Unit>
    suspend fun completeDownload(downloadId: String, filepath: String, downloadedSize: Long): Result<Unit>
    suspend fun failDownload(downloadId: String, errorMessage: String): Result<Unit>
    suspend fun deleteDownload(downloadId: String): Result<Unit>
    suspend fun clearAllDownloads(): Result<Unit>
}
```

### SettingsRepository

Handles browser settings:
- Settings CRUD
- Preset configurations (Default, Privacy, Performance, Accessibility)
- Reactive observation

```kotlin
interface SettingsRepository {
    suspend fun getSettings(): Result<BrowserSettings>
    fun observeSettings(): Flow<BrowserSettings>
    suspend fun updateSettings(settings: BrowserSettings): Result<Unit>
    suspend fun <T> updateSetting(key: String, value: T): Result<Unit>
    suspend fun resetSettings(): Result<Unit>
    suspend fun applyPreset(preset: SettingsPreset): Result<Unit>
}
```

### SessionRepository

Handles session save/restore and crash recovery:
- Session saving with tabs
- Latest session retrieval
- Crash recovery sessions
- Session history

```kotlin
interface SessionRepository {
    suspend fun saveSession(session: Session, tabs: List<SessionTab>): Result<Unit>
    suspend fun getSession(sessionId: String): Result<Session?>
    suspend fun getLatestSession(): Result<Session?>
    suspend fun getLatestCrashSession(): Result<Session?>
    suspend fun getAllSessions(limit: Int, offset: Int): Result<List<Session>>
    suspend fun getSessionTabs(sessionId: String): Result<List<SessionTab>>
    suspend fun getActiveSessionTab(sessionId: String): Result<SessionTab?>
    suspend fun deleteSession(sessionId: String): Result<Unit>
    suspend fun deleteAllSessions(): Result<Unit>
    suspend fun deleteOldSessions(timestamp: Instant): Result<Unit>
}
```

### SitePermissionRepository

Handles per-site permissions:
- Permission CRUD per domain
- Permission types (location, camera, microphone, etc.)

```kotlin
interface SitePermissionRepository {
    suspend fun getSitePermission(domain: String, permissionType: String): Result<SitePermission?>
    suspend fun insertSitePermission(domain: String, permissionType: String, granted: Boolean): Result<Unit>
    suspend fun deleteSitePermission(domain: String, permissionType: String): Result<Unit>
    suspend fun deleteAllSitePermissions(domain: String): Result<Unit>
    suspend fun getAllSitePermissions(): Result<List<SitePermission>>
}
```

---

## BrowserRepositoryImpl Facade

The `BrowserRepositoryImpl` now acts as a facade that:
1. Implements the `BrowserRepository` interface (unchanged external API)
2. Delegates all operations to domain-specific repositories
3. Handles initialization and cross-cutting concerns
4. Manages reactive state coordination

```kotlin
class BrowserRepositoryImpl(
    private val database: BrowserDatabase
) : BrowserRepository {

    // Domain-specific repositories
    private val tabRepository: TabRepository = TabRepositoryImpl(database)
    private val favoriteRepository: FavoriteRepository = FavoriteRepositoryImpl(database)
    private val historyRepository: HistoryRepository = HistoryRepositoryImpl(database)
    private val downloadRepository: DownloadRepository = DownloadRepositoryImpl(database)
    private val settingsRepository: SettingsRepository = SettingsRepositoryImpl(database)
    private val sessionRepository: SessionRepository = SessionRepositoryImpl(database)
    private val sitePermissionRepository: SitePermissionRepository = SitePermissionRepositoryImpl(database)

    // Delegation
    override suspend fun createTab(tab: Tab) = tabRepository.createTab(tab)
    override fun observeTabs() = tabRepository.observeTabs()
    // ... more delegations
}
```

---

## Benefits

### 1. Single Responsibility
Each repository handles exactly one domain, making the code easier to understand and modify.

### 2. Testability
Individual repositories can be tested in isolation with mock databases.

### 3. Maintainability
- Smaller files (~150-200 lines each vs 1,264)
- Reduced cognitive load
- Clear ownership boundaries

### 4. Extensibility
New features can be added to specific domains without touching unrelated code.

### 5. Backward Compatibility
External API (`BrowserRepository` interface) remains unchanged.

---

## File Structure

```
Modules/WebAvanue/src/commonMain/kotlin/com/augmentalis/webavanue/
├── BrowserRepository.kt          # Interface (unchanged)
├── BrowserRepositoryImpl.kt      # Facade implementation
└── repository/
    ├── TabRepository.kt          # Tab operations
    ├── FavoriteRepository.kt     # Bookmark operations
    ├── HistoryRepository.kt      # History operations
    ├── DownloadRepository.kt     # Download management
    ├── SettingsRepository.kt     # Settings management
    ├── SessionRepository.kt      # Session save/restore
    └── SitePermissionRepository.kt # Site permissions
```

---

## Threading Model

All repositories follow the same threading model:
- **Database Operations**: `Dispatchers.IO`
- **StateFlow Updates**: Thread-safe via `MutableStateFlow`
- **Initialization**: Background via `CoroutineScope(SupervisorJob() + Dispatchers.IO)`

---

## Migration Notes

### For Existing Code
No changes required. The `BrowserRepository` interface is unchanged, so all existing code continues to work.

### For New Features
Consider which domain a feature belongs to and add it to the appropriate repository. If it spans multiple domains, add coordination logic to `BrowserRepositoryImpl`.

---

## Related Documentation

- [Chapter 77: Logging Module Architecture](Developer-Manual-Chapter77-Logging-Module-Architecture.md)
- [Chapter 78: Handler Utilities](Developer-Manual-Chapter78-Handler-Utilities.md)
