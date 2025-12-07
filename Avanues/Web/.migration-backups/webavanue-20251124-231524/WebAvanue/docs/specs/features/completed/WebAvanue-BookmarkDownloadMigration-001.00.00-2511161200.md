# Bookmark and Download Entities - Future Migration

**Status:** Pending (Phase 4)
**Source:** universal/BrowserDatabase.sq (removed in Phase 3)
**Destination:** BrowserCoreData module

---

## Overview

During Phase 3 integration, the universal module's BrowserDatabase.sq was removed and replaced with BrowserCoreData. However, two entities from the original schema need to be migrated to BrowserCoreData in a future phase:

1. **BookmarkEntity** - Full bookmark management with folders
2. **DownloadEntity** - Download tracking and management

---

## Bookmark Entity

### Schema

```sql
-- Bookmark Entity
CREATE TABLE BookmarkEntity (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    url TEXT NOT NULL UNIQUE,
    title TEXT NOT NULL,
    folder TEXT,
    position INTEGER NOT NULL DEFAULT 0,
    favicon TEXT,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL
);

-- Indexes
CREATE INDEX idx_bookmark_folder ON BookmarkEntity(folder);
CREATE INDEX idx_bookmark_position ON BookmarkEntity(position);
```

### Queries

```sql
-- Get all bookmarks ordered by position
getAllBookmarks:
SELECT * FROM BookmarkEntity
ORDER BY position ASC;

-- Get bookmarks by folder
getBookmarksByFolder:
SELECT * FROM BookmarkEntity
WHERE folder = :folder
ORDER BY position ASC;

-- Get bookmark by URL
getBookmarkByUrl:
SELECT * FROM BookmarkEntity
WHERE url = :url;

-- Search bookmarks
searchBookmarks:
SELECT * FROM BookmarkEntity
WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%'
ORDER BY position ASC;

-- Insert bookmark
insertBookmark:
INSERT INTO BookmarkEntity (url, title, folder, position, favicon, createdAt, updatedAt)
VALUES (?, ?, ?, ?, ?, ?, ?);

-- Update bookmark
updateBookmark:
UPDATE BookmarkEntity
SET url = ?, title = ?, folder = ?, position = ?, favicon = ?, updatedAt = ?
WHERE id = ?;

-- Delete bookmark
deleteBookmark:
DELETE FROM BookmarkEntity
WHERE id = :id;

-- Delete all bookmarks
deleteAllBookmarks:
DELETE FROM BookmarkEntity;

-- Get bookmark count
getBookmarkCount:
SELECT COUNT(*) FROM BookmarkEntity;
```

### Differences from Favorite Entity

**Bookmark (full-featured):**
- Has folder organization
- Has position within folder
- Has updatedAt timestamp
- More heavyweight

**Favorite (lightweight, currently in BrowserCoreData):**
- No folder organization
- Simple position
- Only createdAt timestamp
- Optimized for quick access

**Recommendation:** Keep both. Favorites for quick access (homepage tiles), Bookmarks for organized collection.

---

## Download Entity

### Schema

```sql
-- Download Entity
CREATE TABLE DownloadEntity (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    url TEXT NOT NULL,
    fileName TEXT NOT NULL,
    filePath TEXT NOT NULL,
    mimeType TEXT,
    totalBytes INTEGER NOT NULL DEFAULT 0,
    downloadedBytes INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL, -- PENDING, DOWNLOADING, COMPLETED, FAILED, CANCELLED
    createdAt INTEGER NOT NULL,
    completedAt INTEGER
);

-- Index
CREATE INDEX idx_download_status ON DownloadEntity(status);
```

### Queries

```sql
-- Get all downloads ordered by creation time
getAllDownloads:
SELECT * FROM DownloadEntity
ORDER BY createdAt DESC;

-- Get download by ID
getDownloadById:
SELECT * FROM DownloadEntity
WHERE id = :id;

-- Get downloads by status
getDownloadsByStatus:
SELECT * FROM DownloadEntity
WHERE status = :status
ORDER BY createdAt DESC;

-- Get active downloads (PENDING or DOWNLOADING)
getActiveDownloads:
SELECT * FROM DownloadEntity
WHERE status IN ('PENDING', 'DOWNLOADING')
ORDER BY createdAt DESC;

-- Insert download
insertDownload:
INSERT INTO DownloadEntity (url, fileName, filePath, mimeType, totalBytes, downloadedBytes, status, createdAt, completedAt)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);

-- Update download progress
updateDownloadProgress:
UPDATE DownloadEntity
SET downloadedBytes = :downloadedBytes
WHERE id = :id;

-- Update download status
updateDownloadStatus:
UPDATE DownloadEntity
SET status = :status, completedAt = :completedAt
WHERE id = :id;

-- Delete download
deleteDownload:
DELETE FROM DownloadEntity
WHERE id = :id;

-- Delete all downloads
deleteAllDownloads:
DELETE FROM DownloadEntity;

-- Delete completed downloads
deleteCompletedDownloads:
DELETE FROM DownloadEntity
WHERE status = 'COMPLETED';
```

### Domain Model (Proposed)

```kotlin
data class Download(
    val id: String = uuid4().toString(),
    val url: String,
    val fileName: String,
    val filePath: String,
    val mimeType: String? = null,
    val totalBytes: Long = 0L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus,
    val createdAt: Instant,
    val completedAt: Instant? = null
) {
    val progress: Float
        get() = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
}

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}
```

---

## Migration Plan (Phase 4)

### Step 1: Add Bookmark.sq to BrowserCoreData
```bash
# Create new schema file
touch BrowserCoreData/src/commonMain/sqldelight/com/augmentalis/Avanues/web/data/db/Bookmark.sq

# Copy schema and queries from above
```

### Step 2: Add Download.sq to BrowserCoreData
```bash
# Create new schema file
touch BrowserCoreData/src/commonMain/sqldelight/com/augmentalis/Avanues/web/data/db/Download.sq

# Copy schema and queries from above
```

### Step 3: Create Domain Models
```bash
# Create domain models
touch BrowserCoreData/src/commonMain/kotlin/com/augmentalis/Avanues/web/data/domain/model/Bookmark.kt
touch BrowserCoreData/src/commonMain/kotlin/com/augmentalis/Avanues/web/data/domain/model/Download.kt
```

### Step 4: Create Mappers
```bash
# Create mappers
touch BrowserCoreData/src/commonMain/kotlin/com/augmentalis/Avanues/web/data/data/mapper/BookmarkMapper.kt
touch BrowserCoreData/src/commonMain/kotlin/com/augmentalis/Avanues/web/data/data/mapper/DownloadMapper.kt
```

### Step 5: Extend Repository
```kotlin
// Add to BrowserRepository interface
interface BrowserRepository {
    // ... existing methods ...

    // Bookmark operations
    suspend fun addBookmark(url: String, title: String, folder: String?): Result<Bookmark>
    suspend fun removeBookmark(bookmarkId: String): Result<Unit>
    suspend fun getBookmark(bookmarkId: String): Result<Bookmark?>
    fun getAllBookmarks(): Flow<List<Bookmark>>
    fun getBookmarksByFolder(folder: String): Flow<List<Bookmark>>
    fun searchBookmarks(query: String): Flow<List<Bookmark>>

    // Download operations
    suspend fun addDownload(download: Download): Result<Download>
    suspend fun updateDownloadProgress(downloadId: String, downloadedBytes: Long): Result<Unit>
    suspend fun updateDownloadStatus(downloadId: String, status: DownloadStatus): Result<Unit>
    suspend fun getDownload(downloadId: String): Result<Download?>
    fun getAllDownloads(): Flow<List<Download>>
    fun getActiveDownloads(): Flow<List<Download>>
}
```

### Step 6: Create Managers (Optional)
```kotlin
// BookmarkManager.kt - with folder organization
class BookmarkManager(
    private val repository: BrowserRepository,
    private val scope: CoroutineScope
) {
    fun getBookmarksByFolder(folder: String): Flow<List<Bookmark>>
    suspend fun addBookmark(url: String, title: String, folder: String?): Result<Bookmark>
    suspend fun organizeBookmarks(bookmarks: List<Bookmark>, folder: String): Result<Unit>
}

// DownloadManager.kt - with progress tracking
class DownloadManager(
    private val repository: BrowserRepository,
    private val scope: CoroutineScope
) {
    fun getActiveDownloads(): Flow<List<Download>>
    suspend fun startDownload(url: String, fileName: String, filePath: String): Result<Download>
    suspend fun updateProgress(downloadId: String, progress: Long): Result<Unit>
    suspend fun cancelDownload(downloadId: String): Result<Unit>
}
```

### Step 7: Write Tests
```bash
# Create test files
touch BrowserCoreData/src/commonTest/kotlin/com/augmentalis/Avanues/web/data/BookmarkManagerTest.kt
touch BrowserCoreData/src/commonTest/kotlin/com/augmentalis/Avanues/web/data/DownloadManagerTest.kt
touch BrowserCoreData/src/commonTest/kotlin/com/augmentalis/Avanues/web/data/BookmarkRepositoryTest.kt
touch BrowserCoreData/src/commonTest/kotlin/com/augmentalis/Avanues/web/data/DownloadRepositoryTest.kt
```

### Step 8: Integration Test
```kotlin
// Verify all entities work together
@Test
fun testCompleteWorkflow() {
    // Create tab
    // Visit URLs (history)
    // Add favorite
    // Add bookmark to folder
    // Download file
    // Verify all data persisted
}
```

---

## Timeline Estimate

**Phase 4: Bookmark & Download Integration**
- Schema creation: 30 minutes
- Domain models: 30 minutes
- Mappers: 30 minutes
- Repository extension: 1 hour
- Managers (optional): 1 hour
- Tests: 2 hours
- Integration testing: 1 hour

**Total: ~6-7 hours**

---

## Benefits After Phase 4

**Complete browser data management:**
- ✅ Tabs (Phase 2)
- ✅ History (Phase 2)
- ✅ Favorites (Phase 2)
- ✅ Browser Settings (Phase 2)
- ✅ Auth Credentials (Phase 2)
- ⏳ Bookmarks (Phase 4)
- ⏳ Downloads (Phase 4)

**Feature parity with browser-plugin:**
- All original features preserved
- Enhanced with LRU caching
- Cross-platform ready
- 407+ tests (will grow to ~500+ with Bookmark/Download tests)

---

**Created:** 2025-11-16
**Author:** Manoj Jhawar <manoj@ideahq.net>
**Status:** Documentation Complete - Ready for Phase 4 Implementation
