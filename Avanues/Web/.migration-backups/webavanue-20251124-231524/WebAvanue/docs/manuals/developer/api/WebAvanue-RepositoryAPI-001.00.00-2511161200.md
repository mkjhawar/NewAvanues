# Chapter 19: Repository API Reference

**Version:** 1.0.0
**Status:** ✅ Complete
**Date:** 2025-11-16

---

## Overview

The `BrowserRepository` interface defines all data operations for WebAvanue browser functionality. It provides type-safe, coroutine-based access to 7 entity types with 60+ operations.

**Interface:** `com.augmentalis.Avanues.web.data.domain.repository.BrowserRepository`
**Implementation:** `com.augmentalis.Avanues.web.data.data.repository.BrowserRepositoryImpl`
**Total Operations:** 60+ (6 Tab + 5 Favorite + 9 Bookmark + 11 Download + 7 History + 3 Settings)

---

## Design Principles

### 1. Suspend Functions for Writes

All write operations (create, update, delete) are `suspend` functions:

```kotlin
suspend fun createTab(url: String, title: String?): Result<Tab>
suspend fun updateTab(tab: Tab): Result<Unit>
suspend fun closeTab(tabId: String): Result<Unit>
```

**Why:** Database writes are blocking I/O, must run on coroutine dispatcher.

---

### 2. Flow for Reactive Queries

All list queries return `Flow<List<T>>` for reactive updates:

```kotlin
fun getAllTabs(): Flow<List<Tab>>
fun getAllBookmarks(): Flow<List<Bookmark>>
fun getActiveDownloads(): Flow<List<Download>>
```

**Why:** UI automatically updates when database changes (insert/update/delete).

**Example:**
```kotlin
// In ViewModel
repository.getAllTabs()
    .collect { tabs ->
        _tabsState.value = tabs  // UI updates automatically
    }
```

---

### 3. Result<T> for Error Handling

All operations that can fail return `Result<T>`:

```kotlin
suspend fun createTab(url: String, title: String?): Result<Tab>  // Result<Tab>
suspend fun getTab(tabId: String): Result<Tab?>                   // Result<Tab?>
suspend fun closeTab(tabId: String): Result<Unit>                 // Result<Unit>
```

**Why:** Explicit error handling, no exceptions to catch.

**Example:**
```kotlin
val result = repository.createTab("https://example.com", "Example")

result.fold(
    onSuccess = { tab -> println("Created: ${tab.id}") },
    onFailure = { error -> println("Error: ${error.message}") }
)

// Or simpler
val tab = result.getOrThrow()  // Throws exception on failure
```

---

### 4. Nullable Returns for Optional Data

Queries that may not find data return `Result<T?>`:

```kotlin
suspend fun getTab(tabId: String): Result<Tab?>             // May not exist
suspend fun getBookmark(bookmarkId: String): Result<Bookmark?>  // May not exist
```

**Example:**
```kotlin
val tab = repository.getTab("123").getOrNull()
if (tab != null) {
    println("Found tab: ${tab.title}")
} else {
    println("Tab not found")
}
```

---

### 5. Boolean for Simple Checks

Simple existence checks return `Boolean`:

```kotlin
suspend fun isFavorite(url: String): Boolean
suspend fun isBookmarked(url: String): Boolean
```

**Why:** No error case (existence check always succeeds), simpler API.

---

## Tab Operations (6)

### createTab()

**Purpose:** Create a new browser tab

**Signature:**
```kotlin
suspend fun createTab(url: String, title: String? = null): Result<Tab>
```

**Parameters:**
- `url: String` - Initial URL (required)
- `title: String? = null` - Tab title (optional, auto-generated if null)

**Returns:** `Result<Tab>` - Created tab or error

**Example:**
```kotlin
val result = repository.createTab(
    url = "https://github.com/kotlin",
    title = "Kotlin GitHub"
)

val tab = result.getOrThrow()
println("Tab ID: ${tab.id}")
println("Created at: ${tab.createdAt}")
```

**Errors:**
- Invalid URL format
- Database write failure

---

### closeTab()

**Purpose:** Close a tab by ID

**Signature:**
```kotlin
suspend fun closeTab(tabId: String): Result<Unit>
```

**Parameters:**
- `tabId: String` - Tab ID to close

**Returns:** `Result<Unit>` - Success or error

**Example:**
```kotlin
repository.closeTab("abc-123")
    .onSuccess { println("Tab closed") }
    .onFailure { println("Error: ${it.message}") }
```

**Errors:**
- Tab not found
- Database delete failure

---

### getTab()

**Purpose:** Get tab by ID

**Signature:**
```kotlin
suspend fun getTab(tabId: String): Result<Tab?>
```

**Parameters:**
- `tabId: String` - Tab ID

**Returns:** `Result<Tab?>` - Tab or null if not found

**Example:**
```kotlin
val tab = repository.getTab("abc-123").getOrNull()
if (tab != null) {
    println("Title: ${tab.title}")
    println("URL: ${tab.url}")
} else {
    println("Tab not found")
}
```

---

### getAllTabs()

**Purpose:** Get all tabs (reactive)

**Signature:**
```kotlin
fun getAllTabs(): Flow<List<Tab>>
```

**Returns:** `Flow<List<Tab>>` - Flow of tab list (updates on changes)

**Example:**
```kotlin
// In ViewModel
val tabs: StateFlow<List<Tab>> = repository.getAllTabs()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

// In UI
tabs.collect { tabList ->
    tabList.forEach { tab ->
        println("${tab.title} - ${tab.url}")
    }
}
```

---

### updateTab()

**Purpose:** Update existing tab

**Signature:**
```kotlin
suspend fun updateTab(tab: Tab): Result<Unit>
```

**Parameters:**
- `tab: Tab` - Updated tab (full object)

**Returns:** `Result<Unit>` - Success or error

**Example:**
```kotlin
val tab = repository.getTab("abc-123").getOrThrow()
val updated = tab.copy(title = "New Title", url = "https://new-url.com")
repository.updateTab(updated)
```

**Errors:**
- Tab not found
- Invalid tab data
- Database update failure

---

### closeAllTabs()

**Purpose:** Close all tabs

**Signature:**
```kotlin
suspend fun closeAllTabs(): Result<Unit>
```

**Returns:** `Result<Unit>` - Success or error

**Example:**
```kotlin
repository.closeAllTabs()
    .onSuccess { println("All tabs closed") }
```

---

## Favorite Operations (5)

### addFavorite()

**Purpose:** Add URL to favorites

**Signature:**
```kotlin
suspend fun addFavorite(url: String, title: String? = null): Result<Favorite>
```

**Parameters:**
- `url: String` - URL to favorite
- `title: String? = null` - Optional title (extracted from page if null)

**Returns:** `Result<Favorite>` - Created favorite

**Example:**
```kotlin
val favorite = repository.addFavorite(
    url = "https://kotlinlang.org",
    title = "Kotlin Official"
).getOrThrow()
```

**Errors:**
- URL already favorited (UNIQUE constraint)
- Invalid URL

---

### removeFavorite()

**Purpose:** Remove favorite by ID

**Signature:**
```kotlin
suspend fun removeFavorite(favoriteId: String): Result<Unit>
```

**Example:**
```kotlin
repository.removeFavorite("fav-123")
```

---

### getFavorite()

**Purpose:** Get favorite by ID

**Signature:**
```kotlin
suspend fun getFavorite(favoriteId: String): Result<Favorite?>
```

**Example:**
```kotlin
val favorite = repository.getFavorite("fav-123").getOrNull()
```

---

### getAllFavorites()

**Purpose:** Get all favorites (reactive)

**Signature:**
```kotlin
fun getAllFavorites(): Flow<List<Favorite>>
```

**Example:**
```kotlin
repository.getAllFavorites().collect { favorites ->
    favorites.forEach { println(it.title) }
}
```

---

### isFavorite()

**Purpose:** Check if URL is favorited

**Signature:**
```kotlin
suspend fun isFavorite(url: String): Boolean
```

**Returns:** `Boolean` - true if favorited, false otherwise

**Example:**
```kotlin
val isFav = repository.isFavorite("https://github.com")
if (isFav) {
    println("Already favorited!")
} else {
    repository.addFavorite("https://github.com", "GitHub")
}
```

---

## Bookmark Operations (9)

### addBookmark()

**Purpose:** Add bookmark with optional folder

**Signature:**
```kotlin
suspend fun addBookmark(url: String, title: String, folder: String? = null): Result<Bookmark>
```

**Parameters:**
- `url: String` - Bookmark URL (UNIQUE constraint)
- `title: String` - Bookmark title
- `folder: String? = null` - Optional folder name (null = "Unsorted")

**Returns:** `Result<Bookmark>` - Created bookmark

**Example:**
```kotlin
val bookmark = repository.addBookmark(
    url = "https://github.com/kotlin",
    title = "Kotlin GitHub",
    folder = "Development"
).getOrThrow()
```

---

### removeBookmark()

**Purpose:** Remove bookmark by ID

**Signature:**
```kotlin
suspend fun removeBookmark(bookmarkId: String): Result<Unit>
```

---

### getBookmark()

**Purpose:** Get bookmark by ID

**Signature:**
```kotlin
suspend fun getBookmark(bookmarkId: String): Result<Bookmark?>
```

---

### getAllBookmarks()

**Purpose:** Get all bookmarks (ordered by position)

**Signature:**
```kotlin
fun getAllBookmarks(): Flow<List<Bookmark>>
```

**Example:**
```kotlin
repository.getAllBookmarks().collect { bookmarks ->
    bookmarks.forEach { bookmark ->
        println("${bookmark.title} - ${bookmark.folder ?: "Unsorted"}")
    }
}
```

---

### getBookmarksByFolder()

**Purpose:** Get bookmarks in specific folder

**Signature:**
```kotlin
fun getBookmarksByFolder(folder: String): Flow<List<Bookmark>>
```

**Example:**
```kotlin
repository.getBookmarksByFolder("Development").collect { bookmarks ->
    println("Development bookmarks: ${bookmarks.size}")
}
```

---

### searchBookmarks()

**Purpose:** Full-text search bookmarks (title + URL)

**Signature:**
```kotlin
fun searchBookmarks(query: String): Flow<List<Bookmark>>
```

**Example:**
```kotlin
repository.searchBookmarks("kotlin").collect { results ->
    println("Found ${results.size} bookmarks matching 'kotlin'")
}
```

**Search Behavior:**
- Searches both title and URL
- Case-sensitive by default
- Uses SQL LIKE with wildcards: `%query%`

---

### isBookmarked()

**Purpose:** Check if URL is bookmarked

**Signature:**
```kotlin
suspend fun isBookmarked(url: String): Boolean
```

**Example:**
```kotlin
if (repository.isBookmarked("https://github.com/kotlin")) {
    println("Already bookmarked!")
}
```

---

### updateBookmark()

**Purpose:** Update existing bookmark

**Signature:**
```kotlin
suspend fun updateBookmark(bookmark: Bookmark): Result<Unit>
```

**Example:**
```kotlin
val bookmark = repository.getBookmark("bm-123").getOrThrow()
val updated = bookmark.copy(title = "New Title")
repository.updateBookmark(updated)
```

---

### moveBookmarkToFolder()

**Purpose:** Move bookmark to different folder

**Signature:**
```kotlin
suspend fun moveBookmarkToFolder(bookmarkId: String, folder: String?): Result<Unit>
```

**Parameters:**
- `bookmarkId: String` - Bookmark ID
- `folder: String?` - Target folder (null = "Unsorted")

**Example:**
```kotlin
repository.moveBookmarkToFolder("bm-123", "Programming")
```

---

### getAllBookmarkFolders()

**Purpose:** Get list of all bookmark folders

**Signature:**
```kotlin
fun getAllBookmarkFolders(): Flow<List<String>>
```

**Example:**
```kotlin
repository.getAllBookmarkFolders().collect { folders ->
    println("Folders: ${folders.joinToString(", ")}")
    // Output: "Development, News, Programming, Unsorted"
}
```

---

## Download Operations (11)

### addDownload()

**Purpose:** Add new download

**Signature:**
```kotlin
suspend fun addDownload(download: Download): Result<Download>
```

**Parameters:**
- `download: Download` - Download object (use `Download.create()`)

**Example:**
```kotlin
val download = Download.create(
    id = uuid4().toString(),
    url = "https://example.com/file.pdf",
    fileName = "document.pdf",
    filePath = "/downloads/document.pdf",
    mimeType = "application/pdf",
    now = Clock.System.now()
)

repository.addDownload(download)
```

---

### updateDownloadProgress()

**Purpose:** Update download progress (bytes downloaded)

**Signature:**
```kotlin
suspend fun updateDownloadProgress(
    downloadId: String,
    downloadedBytes: Long,
    totalBytes: Long? = null
): Result<Unit>
```

**Parameters:**
- `downloadId: String` - Download ID
- `downloadedBytes: Long` - Bytes downloaded so far
- `totalBytes: Long? = null` - Optional total bytes (if known)

**Example:**
```kotlin
// Update progress incrementally
repository.updateDownloadProgress(
    downloadId = download.id,
    downloadedBytes = 500_000,
    totalBytes = 1_000_000
)

// Later: update just downloaded bytes
repository.updateDownloadProgress(
    downloadId = download.id,
    downloadedBytes = 750_000
)
```

---

### updateDownloadStatus()

**Purpose:** Update download status (state transition)

**Signature:**
```kotlin
suspend fun updateDownloadStatus(
    downloadId: String,
    status: DownloadStatus,
    completedAt: Instant? = null
): Result<Unit>
```

**Parameters:**
- `downloadId: String` - Download ID
- `status: DownloadStatus` - New status (PENDING, DOWNLOADING, COMPLETED, FAILED, CANCELLED)
- `completedAt: Instant? = null` - Completion timestamp (for terminal states)

**Example:**
```kotlin
// Start download
repository.updateDownloadStatus(
    downloadId = download.id,
    status = DownloadStatus.DOWNLOADING
)

// Mark completed
repository.updateDownloadStatus(
    downloadId = download.id,
    status = DownloadStatus.COMPLETED,
    completedAt = Clock.System.now()
)

// Mark failed
repository.updateDownloadStatus(
    downloadId = download.id,
    status = DownloadStatus.FAILED,
    completedAt = Clock.System.now()
)
```

---

### getDownload()

**Purpose:** Get download by ID

**Signature:**
```kotlin
suspend fun getDownload(downloadId: String): Result<Download?>
```

**Example:**
```kotlin
val download = repository.getDownload("dl-123").getOrNull()
if (download != null) {
    println("Progress: ${download.progressPercentage}%")
    println("Status: ${download.status}")
}
```

---

### getAllDownloads()

**Purpose:** Get all downloads (newest first)

**Signature:**
```kotlin
fun getAllDownloads(): Flow<List<Download>>
```

**Example:**
```kotlin
repository.getAllDownloads().collect { downloads ->
    downloads.forEach { dl ->
        println("${dl.fileName}: ${dl.status}")
    }
}
```

---

### getActiveDownloads()

**Purpose:** Get active downloads (PENDING or DOWNLOADING)

**Signature:**
```kotlin
fun getActiveDownloads(): Flow<List<Download>>
```

**Example:**
```kotlin
repository.getActiveDownloads().collect { activeDownloads ->
    println("${activeDownloads.size} downloads in progress")
    activeDownloads.forEach { dl ->
        println("${dl.fileName}: ${dl.progressPercentage}%")
    }
}
```

---

### getDownloadsByStatus()

**Purpose:** Get downloads filtered by status

**Signature:**
```kotlin
fun getDownloadsByStatus(status: DownloadStatus): Flow<List<Download>>
```

**Example:**
```kotlin
// Get completed downloads
repository.getDownloadsByStatus(DownloadStatus.COMPLETED)
    .collect { completedDownloads ->
        println("${completedDownloads.size} completed")
    }

// Get failed downloads
repository.getDownloadsByStatus(DownloadStatus.FAILED)
    .collect { failedDownloads ->
        failedDownloads.forEach { dl ->
            println("Failed: ${dl.fileName}")
        }
    }
```

---

### deleteDownload()

**Purpose:** Delete download by ID

**Signature:**
```kotlin
suspend fun deleteDownload(downloadId: String): Result<Unit>
```

**Example:**
```kotlin
repository.deleteDownload("dl-123")
```

---

### deleteAllDownloads()

**Purpose:** Delete all downloads

**Signature:**
```kotlin
suspend fun deleteAllDownloads(): Result<Unit>
```

**Example:**
```kotlin
repository.deleteAllDownloads()
    .onSuccess { println("All downloads deleted") }
```

---

### cancelAllActiveDownloads()

**Purpose:** Cancel all active downloads

**Signature:**
```kotlin
suspend fun cancelAllActiveDownloads(): Result<Unit>
```

**Example:**
```kotlin
repository.cancelAllActiveDownloads()
    .onSuccess { println("All downloads cancelled") }
```

**Behavior:**
- Sets status to CANCELLED
- Sets completedAt to current time
- Only affects PENDING or DOWNLOADING downloads

---

### retryFailedDownload()

**Purpose:** Retry a failed download

**Signature:**
```kotlin
suspend fun retryFailedDownload(downloadId: String): Result<Unit>
```

**Example:**
```kotlin
repository.retryFailedDownload("dl-123")
    .onSuccess { println("Download restarted") }
```

**Behavior:**
- Resets status to PENDING
- Resets downloadedBytes to 0
- Clears completedAt
- Only works for FAILED downloads

---

## History Operations (7)

### addHistoryEntry()

**Purpose:** Add history entry (URL visited)

**Signature:**
```kotlin
suspend fun addHistoryEntry(url: String, title: String? = null): Result<HistoryEntry>
```

**Example:**
```kotlin
repository.addHistoryEntry(
    url = "https://kotlinlang.org",
    title = "Kotlin Programming Language"
)
```

---

### getHistoryEntry()

**Purpose:** Get history entry by ID

**Signature:**
```kotlin
suspend fun getHistoryEntry(entryId: String): Result<HistoryEntry?>
```

---

### getAllHistory()

**Purpose:** Get all history entries (newest first)

**Signature:**
```kotlin
fun getAllHistory(): Flow<List<HistoryEntry>>
```

**Example:**
```kotlin
repository.getAllHistory().collect { history ->
    history.forEach { entry ->
        println("${entry.title} - ${entry.visitedAt}")
    }
}
```

---

### getHistoryByDate()

**Purpose:** Get history entries for specific date

**Signature:**
```kotlin
fun getHistoryByDate(date: Instant): Flow<List<HistoryEntry>>
```

**Parameters:**
- `date: Instant` - Date to query (start of day)

**Example:**
```kotlin
val today = Clock.System.now()
repository.getHistoryByDate(today).collect { todayHistory ->
    println("${todayHistory.size} entries today")
}
```

**Behavior:**
- Queries from start of day (00:00:00) to end of day (23:59:59)

---

### searchHistory()

**Purpose:** Full-text search history (title + URL)

**Signature:**
```kotlin
fun searchHistory(query: String): Flow<List<HistoryEntry>>
```

**Example:**
```kotlin
repository.searchHistory("kotlin").collect { results ->
    results.forEach { entry ->
        println("${entry.title} - ${entry.url}")
    }
}
```

---

### clearHistory()

**Purpose:** Clear all history

**Signature:**
```kotlin
suspend fun clearHistory(): Result<Unit>
```

**Example:**
```kotlin
repository.clearHistory()
    .onSuccess { println("History cleared") }
```

---

### clearHistoryByTimeRange()

**Purpose:** Clear history within time range

**Signature:**
```kotlin
suspend fun clearHistoryByTimeRange(startTime: Instant, endTime: Instant): Result<Unit>
```

**Example:**
```kotlin
// Clear last 24 hours
val now = Clock.System.now()
val yesterday = now.minus(24.hours)
repository.clearHistoryByTimeRange(yesterday, now)
    .onSuccess { println("Last 24 hours cleared") }
```

---

## Settings Operations (3)

### getSettings()

**Purpose:** Get browser settings

**Signature:**
```kotlin
suspend fun getSettings(): Result<BrowserSettings>
```

**Example:**
```kotlin
val settings = repository.getSettings().getOrThrow()
println("Desktop mode: ${settings.desktopMode}")
println("Block popups: ${settings.blockPopups}")
```

---

### updateSettings()

**Purpose:** Update browser settings

**Signature:**
```kotlin
suspend fun updateSettings(settings: BrowserSettings): Result<Unit>
```

**Example:**
```kotlin
val settings = repository.getSettings().getOrThrow()
val updated = settings.copy(
    desktopMode = true,
    blockPopups = true,
    enableJavaScript = true
)
repository.updateSettings(updated)
```

---

### toggleDesktopMode()

**Purpose:** Toggle desktop mode setting

**Signature:**
```kotlin
suspend fun toggleDesktopMode(): Result<Boolean>
```

**Returns:** `Result<Boolean>` - New desktop mode state

**Example:**
```kotlin
val newState = repository.toggleDesktopMode().getOrThrow()
println("Desktop mode now: $newState")
```

---

## Error Handling Patterns

### Pattern 1: Simple Success/Failure

```kotlin
repository.createTab("https://example.com", "Example")
    .onSuccess { tab -> println("Created: ${tab.id}") }
    .onFailure { error -> println("Error: ${error.message}") }
```

---

### Pattern 2: Get-Or-Null

```kotlin
val tab = repository.getTab("123").getOrNull()
if (tab != null) {
    // Handle tab
} else {
    // Handle not found
}
```

---

### Pattern 3: Get-Or-Throw

```kotlin
try {
    val tab = repository.getTab("123").getOrThrow()
    // Handle tab
} catch (e: Exception) {
    // Handle error
}
```

---

### Pattern 4: Flow Error Handling

```kotlin
repository.getAllTabs()
    .catch { error -> println("Error: ${error.message}") }
    .collect { tabs ->
        // Handle tabs
    }
```

---

## Usage Examples

### Example 1: Tab Management

```kotlin
// Create tab
val tab = repository.createTab(
    url = "https://github.com",
    title = "GitHub"
).getOrThrow()

// Observe all tabs
repository.getAllTabs().collect { tabs ->
    println("Open tabs: ${tabs.size}")
}

// Close tab
repository.closeTab(tab.id)
```

---

### Example 2: Bookmark Workflow

```kotlin
// Add bookmark to folder
val bookmark = repository.addBookmark(
    url = "https://kotlinlang.org",
    title = "Kotlin",
    folder = "Development"
).getOrThrow()

// Search bookmarks
repository.searchBookmarks("kotlin").collect { results ->
    println("Found ${results.size} bookmarks")
}

// Move to different folder
repository.moveBookmarkToFolder(bookmark.id, "Programming")

// Get all folders
repository.getAllBookmarkFolders().collect { folders ->
    println("Folders: ${folders.joinToString(", ")}")
}
```

---

### Example 3: Download Tracking

```kotlin
// Create download
val download = Download.create(
    id = uuid4().toString(),
    url = "https://example.com/file.pdf",
    fileName = "document.pdf",
    filePath = "/downloads/document.pdf"
)

repository.addDownload(download)

// Simulate download progress
for (i in 1..10) {
    repository.updateDownloadProgress(
        downloadId = download.id,
        downloadedBytes = i * 100_000L,
        totalBytes = 1_000_000L
    )
    delay(100)
}

// Mark completed
repository.updateDownloadStatus(
    downloadId = download.id,
    status = DownloadStatus.COMPLETED,
    completedAt = Clock.System.now()
)
```

---

## References

- **Repository Interface:** `BrowserRepository.kt`
- **Repository Implementation:** `BrowserRepositoryImpl.kt`
- **Domain Models:** [Chapter 20: Domain Models](20-Domain-Models.md)
- **BrowserCoreData Module:** [Chapter 07: BrowserCoreData Module](07-BrowserCoreData-Module.md)

---

**Version History:**
- 1.0.0 (2025-11-16) - Initial documentation

**Author:** Manoj Jhawar <manoj@ideahq.net>
**License:** Proprietary - Augmentalis Inc.
