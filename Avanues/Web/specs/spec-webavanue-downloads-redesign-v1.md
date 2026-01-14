# WebAvanue Downloads Feature - Redesign Specification

**Version:** 1.0
**Date:** 2025-12-01
**Status:** APPROVED FOR IMPLEMENTATION
**Priority:** P0 (Critical)

---

## Problem Statement

The current downloads feature is completely non-functional:
- No database persistence (downloads lost on app restart)
- `DownloadViewModel.loadDownloads()` returns `emptyList()` (stubbed)
- No integration with Android DownloadManager for actual file downloads
- No progress tracking from WebView download events
- UI exists but displays nothing useful

---

## Requirements

### Functional Requirements

| ID | Requirement | Priority |
|----|-------------|----------|
| FR-1 | Download files from web pages | P0 |
| FR-2 | Show download progress in real-time | P0 |
| FR-3 | Persist downloads across app restarts | P0 |
| FR-4 | List all downloads with status | P0 |
| FR-5 | Cancel in-progress downloads | P1 |
| FR-6 | Retry failed downloads | P1 |
| FR-7 | Delete completed downloads | P1 |
| FR-8 | Clear all downloads | P2 |
| FR-9 | Open completed downloads | P1 |
| FR-10 | Filter by status (all/completed/in-progress/failed) | P2 |
| FR-11 | Search downloads by filename | P2 |

### Non-Functional Requirements

| ID | Requirement |
|----|-------------|
| NFR-1 | Downloads continue in background |
| NFR-2 | Progress updates at least every 500ms |
| NFR-3 | Survive app process death via Android DownloadManager |
| NFR-4 | Show notification for download progress |

---

## Architecture

### Data Flow

```
WebView Download Event
    │
    ▼
BrowserScreen.onDownloadStart()
    │
    ▼
DownloadViewModel.startDownload()
    │
    ├──► Repository.addDownload() ──► SQLDelight (persist)
    │
    └──► Android DownloadManager.enqueue() ──► System Download
                    │
                    ▼
            BroadcastReceiver (progress/completion)
                    │
                    ▼
            DownloadViewModel.updateProgress()
                    │
                    ▼
            Repository.updateDownload() ──► SQLDelight
                    │
                    ▼
            StateFlow ──► UI Update
```

### Components

| Component | Responsibility |
|-----------|---------------|
| `Download` (Model) | Data class for download entity |
| `BrowserDatabase.sq` | SQLDelight schema for downloads table |
| `BrowserRepository` | Interface for download operations |
| `BrowserRepositoryImpl` | SQLDelight implementation |
| `DownloadViewModel` | State management, business logic |
| `DownloadListScreen` | UI for downloads list |
| `DownloadItem` | UI for individual download |
| `DownloadService` | Android Service for background downloads |
| `DownloadReceiver` | BroadcastReceiver for system events |

---

## Database Schema

### Download Table

```sql
CREATE TABLE IF NOT EXISTS download (
    id TEXT PRIMARY KEY NOT NULL,
    url TEXT NOT NULL,
    filename TEXT NOT NULL,
    filepath TEXT,
    mime_type TEXT,
    file_size INTEGER NOT NULL DEFAULT 0,
    downloaded_size INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    download_manager_id INTEGER,
    created_at INTEGER NOT NULL,
    completed_at INTEGER,
    source_page_url TEXT,
    source_page_title TEXT
);

CREATE INDEX IF NOT EXISTS idx_download_status ON download(status);
CREATE INDEX IF NOT EXISTS idx_download_created ON download(created_at);
```

### Download Queries

```sql
-- Insert
insertDownload:
INSERT OR REPLACE INTO download VALUES ?;

-- Select all (paginated)
selectAllDownloads:
SELECT * FROM download ORDER BY created_at DESC LIMIT ? OFFSET ?;

-- Select by ID
selectDownloadById:
SELECT * FROM download WHERE id = ?;

-- Select by status
selectDownloadsByStatus:
SELECT * FROM download WHERE status = ? ORDER BY created_at DESC;

-- Select by DownloadManager ID (for receiver)
selectDownloadByManagerId:
SELECT * FROM download WHERE download_manager_id = ? LIMIT 1;

-- Update progress
updateDownloadProgress:
UPDATE download SET
    downloaded_size = ?,
    status = ?
WHERE id = ?;

-- Update completion
updateDownloadComplete:
UPDATE download SET
    filepath = ?,
    downloaded_size = ?,
    status = 'COMPLETED',
    completed_at = ?
WHERE id = ?;

-- Update failure
updateDownloadFailed:
UPDATE download SET
    status = 'FAILED',
    error_message = ?
WHERE id = ?;

-- Delete
deleteDownload:
DELETE FROM download WHERE id = ?;

-- Delete all
deleteAllDownloads:
DELETE FROM download;

-- Search
searchDownloads:
SELECT * FROM download
WHERE filename LIKE '%' || ? || '%'
ORDER BY created_at DESC;
```

---

## Domain Model

### Download.kt

```kotlin
package com.augmentalis.webavanue.domain.model

import kotlinx.datetime.Instant
import kotlinx.datetime.Clock

data class Download(
    val id: String,
    val url: String,
    val filename: String,
    val filepath: String? = null,
    val mimeType: String? = null,
    val fileSize: Long = 0,
    val downloadedSize: Long = 0,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val errorMessage: String? = null,
    val downloadManagerId: Long? = null,
    val createdAt: Instant = Clock.System.now(),
    val completedAt: Instant? = null,
    val sourcePageUrl: String? = null,
    val sourcePageTitle: String? = null
) {
    val progress: Float
        get() = if (fileSize > 0) downloadedSize.toFloat() / fileSize else 0f

    val progressPercent: Int
        get() = (progress * 100).toInt()

    val isComplete: Boolean
        get() = status == DownloadStatus.COMPLETED

    val isInProgress: Boolean
        get() = status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PENDING

    val canRetry: Boolean
        get() = status == DownloadStatus.FAILED || status == DownloadStatus.CANCELLED

    companion object {
        fun create(
            url: String,
            filename: String,
            mimeType: String? = null,
            fileSize: Long = 0,
            sourcePageUrl: String? = null,
            sourcePageTitle: String? = null
        ): Download = Download(
            id = java.util.UUID.randomUUID().toString(),
            url = url,
            filename = filename,
            mimeType = mimeType,
            fileSize = fileSize,
            sourcePageUrl = sourcePageUrl,
            sourcePageTitle = sourcePageTitle
        )
    }
}

enum class DownloadStatus {
    PENDING,      // Queued, not started
    DOWNLOADING,  // In progress
    PAUSED,       // User paused
    COMPLETED,    // Successfully finished
    FAILED,       // Error occurred
    CANCELLED     // User cancelled
}
```

---

## Repository Interface

### BrowserRepository.kt (additions)

```kotlin
// Download operations
fun observeDownloads(): Flow<List<Download>>
suspend fun addDownload(download: Download): Result<Download>
suspend fun updateDownload(download: Download): Result<Unit>
suspend fun updateDownloadProgress(id: String, downloadedSize: Long, status: DownloadStatus): Result<Unit>
suspend fun completeDownload(id: String, filepath: String, downloadedSize: Long): Result<Unit>
suspend fun failDownload(id: String, errorMessage: String): Result<Unit>
suspend fun deleteDownload(id: String): Result<Unit>
suspend fun clearAllDownloads(): Result<Unit>
suspend fun getDownloadByManagerId(managerId: Long): Download?
suspend fun searchDownloads(query: String): List<Download>
```

---

## ViewModel

### DownloadViewModel.kt (redesigned)

```kotlin
class DownloadViewModel(
    private val repository: BrowserRepository
) {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // State
    private val _downloads = MutableStateFlow<List<Download>>(emptyList())
    val downloads: StateFlow<List<Download>> = _downloads.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _filter = MutableStateFlow<DownloadFilter>(DownloadFilter.ALL)
    val filter: StateFlow<DownloadFilter> = _filter.asStateFlow()

    // Filtered downloads
    val filteredDownloads: StateFlow<List<Download>> = combine(
        _downloads, _filter
    ) { downloads, filter ->
        when (filter) {
            DownloadFilter.ALL -> downloads
            DownloadFilter.COMPLETED -> downloads.filter { it.isComplete }
            DownloadFilter.IN_PROGRESS -> downloads.filter { it.isInProgress }
            DownloadFilter.FAILED -> downloads.filter { it.status == DownloadStatus.FAILED }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        observeDownloads()
    }

    private fun observeDownloads() {
        viewModelScope.launch {
            repository.observeDownloads()
                .catch { e -> _error.value = "Failed to load downloads: ${e.message}" }
                .collect { downloadList ->
                    _downloads.value = downloadList
                    _isLoading.value = false
                }
        }
    }

    fun startDownload(url: String, filename: String, mimeType: String? = null, fileSize: Long = 0) {
        viewModelScope.launch {
            val download = Download.create(url, filename, mimeType, fileSize)
            repository.addDownload(download)
                .onSuccess {
                    // Actual download is triggered by platform-specific code
                    // Android: DownloadManager via BroadcastReceiver
                }
                .onFailure { e ->
                    _error.value = "Failed to start download: ${e.message}"
                }
        }
    }

    fun cancelDownload(downloadId: String) {
        viewModelScope.launch {
            _downloads.value.find { it.id == downloadId }?.let { download ->
                // Cancel via DownloadManager if has ID
                download.downloadManagerId?.let { managerId ->
                    // Platform-specific cancellation
                }
                repository.updateDownload(download.copy(status = DownloadStatus.CANCELLED))
            }
        }
    }

    fun retryDownload(downloadId: String) {
        viewModelScope.launch {
            _downloads.value.find { it.id == downloadId }?.let { download ->
                if (download.canRetry) {
                    val retryDownload = download.copy(
                        status = DownloadStatus.PENDING,
                        downloadedSize = 0,
                        errorMessage = null
                    )
                    repository.updateDownload(retryDownload)
                    // Trigger platform download again
                }
            }
        }
    }

    fun deleteDownload(downloadId: String) {
        viewModelScope.launch {
            repository.deleteDownload(downloadId)
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            repository.clearAllDownloads()
        }
    }

    fun setFilter(filter: DownloadFilter) {
        _filter.value = filter
    }

    fun clearError() {
        _error.value = null
    }
}

enum class DownloadFilter {
    ALL, COMPLETED, IN_PROGRESS, FAILED
}
```

---

## UI Components

### DownloadListScreen.kt (updated)

- Show list of downloads grouped by date
- Filter chips (All / Completed / In Progress / Failed)
- Empty state when no downloads
- Swipe to delete (with undo)
- Clear all button in toolbar

### DownloadItem.kt (updated)

- Show filename, file size, status
- Show progress bar for in-progress downloads
- Show download/uploaded size (e.g., "5.2 MB / 10.5 MB")
- Show estimated time remaining
- Actions: Open (completed), Cancel (in progress), Retry (failed), Delete

---

## Platform Integration (Android)

### AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />

<receiver android:name=".download.DownloadCompletionReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.DOWNLOAD_COMPLETE" />
    </intent-filter>
</receiver>
```

### DownloadHelper.kt (Android-specific)

```kotlin
object DownloadHelper {
    fun startDownload(
        context: Context,
        url: String,
        filename: String,
        mimeType: String?
    ): Long {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(filename)
            .setDescription("Downloading file...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        mimeType?.let { request.setMimeType(it) }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return downloadManager.enqueue(request)
    }

    fun queryProgress(context: Context, downloadId: Long): DownloadProgress? {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query) ?: return null

        return cursor.use {
            if (it.moveToFirst()) {
                val bytesDownloaded = it.getLong(
                    it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                )
                val bytesTotal = it.getLong(
                    it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                )
                val status = it.getInt(
                    it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
                )
                DownloadProgress(bytesDownloaded, bytesTotal, status)
            } else null
        }
    }

    fun cancelDownload(context: Context, downloadId: Long) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.remove(downloadId)
    }
}

data class DownloadProgress(
    val bytesDownloaded: Long,
    val bytesTotal: Long,
    val status: Int
)
```

### DownloadCompletionReceiver.kt

```kotlin
class DownloadCompletionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (downloadId == -1L) return

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        cursor?.use {
            if (it.moveToFirst()) {
                val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val localUri = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                val bytesDownloaded = it.getLong(
                    it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                )

                // Update repository via Application class or WorkManager
                (context.applicationContext as? WebAvanueApp)?.let { app ->
                    val repository = app.provideRepository()
                    CoroutineScope(Dispatchers.IO).launch {
                        val download = repository.getDownloadByManagerId(downloadId)
                        download?.let { dl ->
                            when (status) {
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    repository.completeDownload(
                                        dl.id,
                                        localUri ?: "",
                                        bytesDownloaded
                                    )
                                }
                                DownloadManager.STATUS_FAILED -> {
                                    repository.failDownload(dl.id, "Download failed")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
```

---

## Implementation Plan

### Phase 1: Database & Model (Day 1)
1. Add Download table to BrowserDatabase.sq
2. Create Download.kt domain model
3. Add repository interface methods
4. Implement repository methods in BrowserRepositoryImpl
5. Run SQLDelight generation

### Phase 2: ViewModel (Day 1)
1. Redesign DownloadViewModel with proper state management
2. Implement all download operations
3. Add filtering logic
4. Test with mock data

### Phase 3: Android Integration (Day 2)
1. Add DownloadHelper.kt for Android DownloadManager
2. Create DownloadCompletionReceiver
3. Register receiver in AndroidManifest
4. Wire WebView download listener to DownloadViewModel
5. Implement progress polling

### Phase 4: UI (Day 2)
1. Update DownloadListScreen with proper list display
2. Update DownloadItem with progress bar and actions
3. Add filter chips
4. Add empty state
5. Test full flow

### Phase 5: Polish (Day 3)
1. Add error handling
2. Add retry logic
3. Add notification support
4. Performance testing
5. Edge case handling

---

## Testing Checklist

| Test Case | Expected Result |
|-----------|-----------------|
| Start download from web page | Download appears in list as "Pending" |
| Download progress updates | Progress bar shows % and size |
| Download completes | Status changes to "Completed" |
| Cancel in-progress download | Status changes to "Cancelled" |
| Retry failed download | Download restarts from beginning |
| Delete download | Removed from list |
| Clear all downloads | List is empty |
| Filter by status | Only matching downloads shown |
| App restart | Downloads persist and resume |
| Open completed download | File opens in appropriate app |

---

## Files to Create/Modify

| File | Action | Priority |
|------|--------|----------|
| `BrowserDatabase.sq` | ADD download table + queries | P0 |
| `Download.kt` | CREATE domain model | P0 |
| `BrowserRepository.kt` | ADD download interface methods | P0 |
| `BrowserRepositoryImpl.kt` | IMPLEMENT download methods | P0 |
| `DownloadViewModel.kt` | REWRITE completely | P0 |
| `DownloadListScreen.kt` | UPDATE with real data | P0 |
| `DownloadItem.kt` | UPDATE with progress/actions | P0 |
| `DownloadHelper.kt` | CREATE Android-specific | P0 |
| `DownloadCompletionReceiver.kt` | CREATE | P0 |
| `AndroidManifest.xml` | ADD receiver | P0 |
| `BrowserScreen.kt` | WIRE download event | P0 |

---

## Risks & Mitigations

| Risk | Mitigation |
|------|------------|
| DownloadManager varies by Android version | Test on API 24, 28, 30, 33 |
| Background downloads killed by OS | Use WorkManager for critical downloads |
| Large file downloads fail | Implement chunked download resume |
| Permission denied on newer Android | Use MediaStore API for Android 10+ |

---

## Approval

- [ ] Technical Review
- [ ] UX Review
- [ ] Implementation Start

**Estimated Effort:** 3 days
**Assignee:** TBD
