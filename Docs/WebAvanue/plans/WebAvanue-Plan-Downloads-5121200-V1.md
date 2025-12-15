# WebAvanue Download Management - Implementation Plan

**Document**: WebAvanue-Plan-Downloads-51212-V1.md
**Spec**: WebAvanue-Spec-Downloads-51212-V1.md
**Project**: WebAvanue (NewAvanues Monorepo)
**Platform**: Android (KMP shared code)
**Version**: 1.0
**Date**: 2025-12-12
**Status**: Ready for Implementation

---

## Overview

### Scope Summary
Complete the remaining ~10% of WebAvanue's download management system by implementing:
1. File picker dialog (SAF integration)
2. Storage permission handling (API 21-34+)
3. Download progress monitoring (real-time UI updates)
4. Settings integration (complete wiring)

### Platform Analysis
- **Primary**: Android (native implementation)
- **Shared**: KMP common code for Download domain models
- **Swarm**: Not recommended (single platform, 18 tasks, low complexity)

### Complexity Assessment
| Metric | Value | Rationale |
|--------|-------|-----------|
| Total Tasks | 18 | Focused feature set |
| Platforms | 1 (Android) | No cross-platform complexity |
| New Files | 6 | Modest file creation |
| Modified Files | 4 | Surgical updates to existing code |
| Swarm Recommended | No | Sequential phases more efficient |

### Time Estimates
| Mode | Duration | Notes |
|------|----------|-------|
| Sequential | 7-8 days | One developer, full testing |
| Parallel (Swarm) | N/A | Single platform, not beneficial |
| With .yolo | 6 days | Skip interactive confirmations |

---

## Implementation Phases

### Phase 1: File Picker & Permissions (2-3 days)

**Goal**: Enable users to select download locations via native Android picker with proper permission handling.

**Tasks**:

#### Task 1.1: Create DownloadFilePickerLauncher (expect/actual)
**File**: `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/webavanue/platform/DownloadFilePickerLauncher.kt`
**Priority**: P0
**Estimated**: 2 hours

**Actions**:
1. Create `expect class DownloadFilePickerLauncher`
2. Define `suspend fun pickDownloadDirectory(initialUri: String?): String?`
3. Add KDoc with usage example

**Acceptance**:
- Compiles in common source set
- Interface clear and documented

---

#### Task 1.2: Implement Android DownloadFilePickerLauncher
**File**: `Modules/WebAvanue/universal/src/androidMain/kotlin/com/augmentalis/webavanue/platform/DownloadFilePickerLauncher.kt`
**Priority**: P0
**Estimated**: 4 hours

**Actions**:
1. Implement `actual class DownloadFilePickerLauncher(context: Context)`
2. Use `Intent.ACTION_OPEN_DOCUMENT_TREE` for picker
3. Implement `suspendCancellableCoroutine` for async result
4. Add `takePersistableUriPermission()` after selection
5. Convert `content://` URI to display path via `DocumentFile`
6. Handle cancellation (return null)

**Code Pattern**:
```kotlin
actual class DownloadFilePickerLauncher(private val context: Context) {
    actual suspend fun pickDownloadDirectory(initialUri: String?): String? {
        return suspendCancellableCoroutine { continuation ->
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                initialUri?.let {
                    putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse(it))
                }
            }
            // Launch activity and await result
        }
    }

    fun takePersistablePermission(uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }
}
```

**Acceptance**:
- File picker launches successfully
- Selected URI returned to caller
- Permissions persist across app restart
- Graceful cancellation handling

**Dependencies**: None

---

#### Task 1.3: Create DownloadPermissionManager (expect/actual)
**File**: `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/webavanue/platform/DownloadPermissionManager.kt`
**Priority**: P0
**Estimated**: 1 hour

**Actions**:
1. Create `expect class DownloadPermissionManager`
2. Define methods:
   - `fun isPermissionRequired(): Boolean`
   - `fun isPermissionGranted(): Boolean`
   - `suspend fun requestPermission(): Boolean`
   - `fun openPermissionSettings()`
3. Add KDoc explaining API-level differences

**Acceptance**:
- Interface compiles in common
- Clear documentation of behavior per API level

---

#### Task 1.4: Implement Android DownloadPermissionManager
**File**: `Modules/WebAvanue/universal/src/androidMain/kotlin/com/augmentalis/webavanue/platform/DownloadPermissionManager.kt`
**Priority**: P0
**Estimated**: 4 hours

**Actions**:
1. Implement API-level detection (< 29 requires permissions)
2. Use `ActivityCompat.checkSelfPermission()` for granted check
3. Use `registerForActivityResult()` with `RequestPermission` contract
4. Implement `openPermissionSettings()` via `Intent.ACTION_APPLICATION_DETAILS_SETTINGS`
5. Handle rationale dialog (show before first request)

**API Level Logic**:
| API Level | Permission Required | Implementation |
|-----------|---------------------|----------------|
| 21-28 | WRITE_EXTERNAL_STORAGE | Request at runtime |
| 29-32 | None (scoped storage) | Return true always |
| 33+ | None (scoped storage) | Return true always |

**Acceptance**:
- Correct permission check per API level
- Permission request works on API 21-28
- Rationale dialog shown before first request
- Settings intent opens correctly

**Dependencies**: Task 1.2 (needs Activity context)

---

#### Task 1.5: Update DownloadPathSettingItem UI
**File**: `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/ui/settings/SettingsScreen.kt` (line ~2430)
**Priority**: P0
**Estimated**: 3 hours

**Actions**:
1. Replace manual text field with read-only display + buttons
2. Add "Browse" button → launch file picker
3. Add "Use Default" button → clear custom path
4. Show available space below path (if custom path set)
5. Integrate `DownloadFilePickerLauncher` via `rememberLauncherForActivityResult`
6. Wire permission check before picker launch

**UI Changes**:
```kotlin
val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocumentTree()
) { uri: Uri? ->
    uri?.let {
        pickerLauncher.takePersistablePermission(it)
        onPathChanged(it.toString())
    }
}

ListItem(
    headlineContent = { Text("Download Location") },
    supportingContent = {
        Column {
            Text(currentPath ?: "Default system path")
            availableSpaceMB?.let { Text("$it MB available", style = caption) }
        }
    },
    trailingContent = {
        Row {
            IconButton(onClick = {
                if (permissionManager.isPermissionRequired() && !permissionManager.isPermissionGranted()) {
                    // Request permission first
                } else {
                    filePickerLauncher.launch(null)
                }
            }) {
                Icon(Icons.Default.FolderOpen, "Browse")
            }
            if (currentPath != null) {
                IconButton(onClick = { onPathChanged(null) }) {
                    Icon(Icons.Default.Delete, "Use default")
                }
            }
        }
    }
)
```

**Acceptance**:
- Browse button launches file picker
- Selected path displays correctly
- Use Default button clears path
- Permission requested before picker (API < 29)

**Dependencies**: Tasks 1.2, 1.4

---

#### Task 1.6: Add Permission Request to DownloadHelper
**File**: `android/apps/webavanue/app/src/main/kotlin/com/augmentalis/Avanues/web/app/download/DownloadHelper.kt`
**Priority**: P1
**Estimated**: 2 hours

**Actions**:
1. Inject `DownloadPermissionManager` dependency
2. Check permission before starting download
3. If not granted and required: show error + settings button
4. Fallback to default Downloads folder if permission denied

**Code Pattern**:
```kotlin
fun startDownload(...): Long {
    // Check permission before download
    if (permissionManager.isPermissionRequired() && !permissionManager.isPermissionGranted()) {
        // Show error: "Storage permission required"
        return -1
    }

    // Proceed with download...
}
```

**Acceptance**:
- Downloads blocked without permission (API < 29)
- Error message shown with "Grant Permission" action
- Fallback to default path works

**Dependencies**: Task 1.4

---

#### Task 1.7: Write Unit Tests - File Picker & Permissions
**Files**:
- `Modules/WebAvanue/universal/src/androidTest/kotlin/com/augmentalis/webavanue/platform/DownloadFilePickerLauncherTest.kt`
- `Modules/WebAvanue/universal/src/androidTest/kotlin/com/augmentalis/webavanue/platform/DownloadPermissionManagerTest.kt`
**Priority**: P1
**Estimated**: 3 hours

**Test Cases**:
```kotlin
// DownloadFilePickerLauncherTest
@Test fun `launches picker with initial URI`()
@Test fun `returns null when picker cancelled`()
@Test fun `takes persistable permission after selection`()
@Test fun `converts content URI to display path`()

// DownloadPermissionManagerTest
@Test fun `returns true for API 29+ (no permission required)`()
@Test fun `returns false for API 28 when permission not granted`()
@Test fun `requests permission and returns result`()
@Test fun `opens app settings when requested`()
```

**Acceptance**:
- All tests pass
- 90%+ coverage for permission logic
- Edge cases covered (cancellation, permission denial)

**Dependencies**: Tasks 1.2, 1.4

---

#### Task 1.8: Write UI Tests - Settings File Picker
**File**: `android/apps/webavanue/app/src/androidTest/kotlin/com/augmentalis/Avanues/web/app/ui/DownloadSettingsUITest.kt`
**Priority**: P2
**Estimated**: 2 hours

**Test Cases**:
```kotlin
@Test fun `clicking browse button launches file picker`()
@Test fun `selected path displays in settings after picker returns`()
@Test fun `delete button resets to default path`()
@Test fun `permission request shown before picker on API 28`()
```

**Acceptance**:
- UI tests pass on emulator (API 28, API 33)
- File picker interaction works
- Path persistence verified

**Dependencies**: Task 1.5

---

**Phase 1 Deliverables**:
- ✅ File picker launches from settings
- ✅ Selected path saved and displayed
- ✅ Permission requested when needed
- ✅ Graceful fallback to default on denial
- ✅ 8 tasks completed
- ✅ ~16 hours effort

---

### Phase 2: Path Validation (1 day)

**Goal**: Validate custom download paths for writability, existence, and available storage space.

**Tasks**:

#### Task 2.1: Create DownloadPathValidator (expect/actual)
**File**: `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/webavanue/platform/DownloadPathValidator.kt`
**Priority**: P1
**Estimated**: 1 hour

**Actions**:
1. Create `expect class DownloadPathValidator`
2. Define `suspend fun validate(path: String): ValidationResult`
3. Define `data class ValidationResult`

**Data Model**:
```kotlin
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val availableSpaceMB: Long = 0,
    val isLowSpace: Boolean = false // < 100MB
)
```

**Acceptance**:
- Interface compiles
- Clear contract for validation

---

#### Task 2.2: Implement Android DownloadPathValidator
**File**: `Modules/WebAvanue/universal/src/androidMain/kotlin/com/augmentalis/webavanue/platform/DownloadPathValidator.kt`
**Priority**: P1
**Estimated**: 4 hours

**Actions**:
1. Implement `actual class DownloadPathValidator(context: Context)`
2. Use `DocumentFile.canWrite()` for URI-based paths
3. Use `StatFs` to calculate free space
4. Check if path exists (`DocumentFile.exists()`)
5. Detect permission revocation (re-check permissions)

**Validation Checks**:
```kotlin
suspend fun validate(path: String): ValidationResult = withContext(Dispatchers.IO) {
    val uri = Uri.parse(path)
    val docFile = DocumentFile.fromTreeUri(context, uri) ?: return@withContext ValidationResult(
        isValid = false,
        errorMessage = "Invalid path"
    )

    if (!docFile.exists()) {
        return@withContext ValidationResult(false, "Path no longer exists")
    }

    if (!docFile.canWrite()) {
        return@withContext ValidationResult(false, "Cannot write to this location")
    }

    val freeSpaceMB = calculateFreeSpace(uri)

    ValidationResult(
        isValid = true,
        availableSpaceMB = freeSpaceMB,
        isLowSpace = freeSpaceMB < 100
    )
}
```

**Acceptance**:
- Detects non-existent paths
- Detects non-writable paths
- Calculates free space correctly
- Low space warning at < 100MB

**Dependencies**: Task 2.1

---

#### Task 2.3: Wire Validation to SettingsViewModel
**File**: `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/viewmodel/SettingsViewModel.kt`
**Priority**: P1
**Estimated**: 2 hours

**Actions**:
1. Inject `DownloadPathValidator` dependency
2. Add `validateDownloadPath(path: String)` method
3. Store validation result in StateFlow
4. Run validation on path selection
5. Run validation on app startup (init block)
6. Auto-revert to default if validation fails on startup

**Code Pattern**:
```kotlin
private val _pathValidation = MutableStateFlow<ValidationResult?>(null)
val pathValidation: StateFlow<ValidationResult?> = _pathValidation.asStateFlow()

init {
    // Validate saved path on startup
    viewModelScope.launch {
        settings.value?.downloadPath?.let { path ->
            val result = pathValidator.validate(path)
            if (!result.isValid) {
                // Revert to default
                updateSettings(settings.value!!.copy(downloadPath = null))
            }
            _pathValidation.value = result
        }
    }
}

fun validateDownloadPath(path: String) {
    viewModelScope.launch {
        _pathValidation.value = pathValidator.validate(path)
    }
}
```

**Acceptance**:
- Validation runs on path selection
- Validation runs on app startup
- Invalid path auto-reverts to default
- Validation result exposed to UI

**Dependencies**: Task 2.2

---

#### Task 2.4: Show Validation Errors in UI
**File**: `SettingsScreen.kt` (DownloadPathSettingItem)
**Priority**: P1
**Estimated**: 2 hours

**Actions**:
1. Collect `pathValidation` flow from ViewModel
2. Show error Snackbar when `isValid == false`
3. Show warning if `isLowSpace == true`
4. Display available space in settings item

**UI Updates**:
```kotlin
val pathValidation by viewModel.pathValidation.collectAsState()

// Show error Snackbar
LaunchedEffect(pathValidation) {
    pathValidation?.errorMessage?.let { error ->
        snackbarHostState.showSnackbar(
            message = error,
            actionLabel = "Use Default"
        )
    }
}

// Show warning for low space
if (pathValidation?.isLowSpace == true) {
    Text(
        "Low storage space (${pathValidation.availableSpaceMB} MB free)",
        color = MaterialTheme.colorScheme.warning
    )
}
```

**Acceptance**:
- Error shown when path invalid
- Warning shown when space < 100MB
- Available space displayed
- "Use Default" action works

**Dependencies**: Task 2.3

---

#### Task 2.5: Write Unit Tests - Path Validation
**File**: `Modules/WebAvanue/universal/src/androidTest/kotlin/com/augmentalis/webavanue/platform/DownloadPathValidatorTest.kt`
**Priority**: P1
**Estimated**: 2 hours

**Test Cases**:
```kotlin
@Test fun `validate returns error when path not writable`()
@Test fun `validate returns warning when space less than 100MB`()
@Test fun `validate succeeds for valid path with sufficient space`()
@Test fun `validate returns error when path does not exist`()
@Test fun `validate returns error when permission revoked`()
```

**Acceptance**:
- All validation logic tested
- Edge cases covered
- 90%+ coverage

**Dependencies**: Task 2.2

---

**Phase 2 Deliverables**:
- ✅ Invalid paths detected and reported
- ✅ Low space warnings shown
- ✅ Auto-revert to default on startup if path invalid
- ✅ 5 tasks completed
- ✅ ~11 hours effort

---

### Phase 3: Download Progress Monitoring (2 days)

**Goal**: Wire DownloadManager progress to UI using reactive Flows for real-time updates.

**Tasks**:

#### Task 3.1: Create DownloadProgressMonitor Class
**File**: `Modules/WebAvanue/universal/src/androidMain/kotlin/com/augmentalis/Avanues/web/universal/download/DownloadProgressMonitor.kt`
**Priority**: P0
**Estimated**: 4 hours

**Actions**:
1. Create class with `StateFlow<Map<String, DownloadProgress>>`
2. Implement coroutine-based polling (500ms interval)
3. Use `DownloadHelper.queryProgress()` to get current status
4. Calculate download speed: `(currentBytes - lastBytes) / timeDelta`
5. Calculate ETA: `remainingBytes / averageSpeed`
6. Emit updates to StateFlow
7. Handle download completion (stop polling)

**Architecture**:
```kotlin
class DownloadProgressMonitor(
    private val downloadManager: DownloadManager,
    private val scope: CoroutineScope
) {
    private val _progressFlow = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val progressFlow: StateFlow<Map<String, DownloadProgress>> = _progressFlow.asStateFlow()

    private val activeDownloads = mutableMapOf<String, Job>()

    fun startMonitoring(downloadId: String) {
        val job = scope.launch {
            var lastBytes = 0L
            var lastTime = System.currentTimeMillis()

            while (isActive) {
                val progress = DownloadHelper.queryProgress(context, downloadId.toLong())
                progress?.let {
                    val currentTime = System.currentTimeMillis()
                    val timeDelta = (currentTime - lastTime) / 1000.0 // seconds
                    val bytesDelta = it.bytesDownloaded - lastBytes

                    val speed = if (timeDelta > 0) (bytesDelta / timeDelta).toLong() else 0L
                    val remainingBytes = it.bytesTotal - it.bytesDownloaded
                    val eta = if (speed > 0) (remainingBytes / speed).toLong() else 0L

                    _progressFlow.value = _progressFlow.value + (downloadId to DownloadProgress(
                        downloadId = downloadId,
                        bytesDownloaded = it.bytesDownloaded,
                        bytesTotal = it.bytesTotal,
                        downloadSpeed = speed,
                        estimatedTimeRemaining = eta,
                        status = it.status
                    ))

                    lastBytes = it.bytesDownloaded
                    lastTime = currentTime

                    if (it.isComplete || it.isFailed) {
                        stopMonitoring(downloadId)
                    }
                }

                delay(500)
            }
        }

        activeDownloads[downloadId] = job
    }

    fun stopMonitoring(downloadId: String) {
        activeDownloads[downloadId]?.cancel()
        activeDownloads.remove(downloadId)
    }

    fun stopAll() {
        activeDownloads.values.forEach { it.cancel() }
        activeDownloads.clear()
    }
}
```

**Acceptance**:
- Polls every 500ms
- Speed calculated correctly
- ETA calculated correctly
- Stops when download completes
- Multiple downloads monitored simultaneously

**Dependencies**: None (uses existing DownloadHelper)

---

#### Task 3.2: Enhance Download Model with Progress Fields
**File**: `Modules/WebAvanue/coredata/src/commonMain/kotlin/com/augmentalis/webavanue/domain/model/Download.kt`
**Priority**: P0
**Estimated**: 1 hour

**Actions**:
1. Add `downloadManagerId: Long?` field
2. Add `downloadSpeed: Long` field (bytes/sec)
3. Add `estimatedTimeRemaining: Long` field (seconds)
4. Add `lastProgressUpdate: Long` field (timestamp ms)
5. Add computed property `progress: Float` (0.0-1.0)

**Model Updates**:
```kotlin
data class Download(
    // ... existing fields ...

    // New fields for progress tracking
    val downloadManagerId: Long? = null,  // Android DownloadManager ID
    val downloadSpeed: Long = 0,           // Bytes per second
    val estimatedTimeRemaining: Long = 0,  // Seconds
    val lastProgressUpdate: Long = 0       // Timestamp (ms)
) {
    val progress: Float
        get() = if (fileSize > 0) downloadedSize.toFloat() / fileSize else 0f
}
```

**Acceptance**:
- Fields added to data class
- Backwards compatible (default values)
- Progress calculation correct

**Dependencies**: None

---

#### Task 3.3: Wire DownloadProgressMonitor to DownloadViewModel
**File**: `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/viewmodel/DownloadViewModel.kt`
**Priority**: P0
**Estimated**: 3 hours

**Actions**:
1. Inject `DownloadProgressMonitor` dependency
2. Collect `progressFlow` in init
3. Update `_downloads` StateFlow with progress data
4. Start monitoring when download added
5. Stop monitoring when download completed/cancelled
6. Clean up on ViewModel cleared

**Code Pattern**:
```kotlin
init {
    // Collect progress updates
    viewModelScope.launch {
        progressMonitor.progressFlow.collect { progressMap ->
            val updatedDownloads = _downloads.value.map { download ->
                progressMap[download.id]?.let { progress ->
                    download.copy(
                        downloadedSize = progress.bytesDownloaded,
                        fileSize = progress.bytesTotal,
                        downloadSpeed = progress.downloadSpeed,
                        estimatedTimeRemaining = progress.estimatedTimeRemaining,
                        lastProgressUpdate = System.currentTimeMillis()
                    )
                } ?: download
            }
            _downloads.value = updatedDownloads
        }
    }
}

fun startDownload(...): String? {
    // ... existing code ...
    val downloadId = download.id

    // Start progress monitoring
    download.downloadManagerId?.let { dmId ->
        progressMonitor.startMonitoring(dmId.toString())
    }

    return downloadId
}

fun cancelDownload(downloadId: String) {
    progressMonitor.stopMonitoring(downloadId)
    // ... existing cancel logic ...
}
```

**Acceptance**:
- Progress updates reflected in `downloads` StateFlow
- Monitoring starts automatically when download added
- Monitoring stops when download completed/cancelled
- No memory leaks (cleanup in onCleared)

**Dependencies**: Tasks 3.1, 3.2

---

#### Task 3.4: Update DownloadItem UI with Progress Display
**File**: `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/ui/download/DownloadItem.kt`
**Priority**: P0
**Estimated**: 3 hours

**Actions**:
1. Add progress bar for `DOWNLOADING` status
2. Display download speed (formatted: KB/s, MB/s)
3. Display ETA (formatted: "X minutes remaining")
4. Add helper functions: `formatSpeed()`, `formatETA()`, `formatFileSize()`
5. Animate progress bar smoothly

**UI Updates**:
```kotlin
@Composable
fun DownloadItem(download: Download, ...) {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            // File name and size
            Text(download.filename, style = MaterialTheme.typography.bodyLarge)
            Text(formatFileSize(download.fileSize), style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(8.dp))

            // Progress bar (if downloading)
            if (download.status == DownloadStatus.DOWNLOADING) {
                LinearProgressIndicator(
                    progress = download.progress,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(4.dp))

                // Speed and ETA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatSpeed(download.downloadSpeed),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${formatETA(download.estimatedTimeRemaining)} remaining",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Action buttons...
        }
    }
}

fun formatSpeed(bytesPerSec: Long): String {
    return when {
        bytesPerSec < 1024 -> "$bytesPerSec B/s"
        bytesPerSec < 1024 * 1024 -> "${bytesPerSec / 1024} KB/s"
        else -> String.format("%.1f MB/s", bytesPerSec / (1024.0 * 1024.0))
    }
}

fun formatETA(seconds: Long): String {
    return when {
        seconds < 60 -> "$seconds seconds"
        seconds < 3600 -> "${seconds / 60} minutes"
        else -> "${seconds / 3600} hours ${(seconds % 3600) / 60} minutes"
    }
}
```

**Acceptance**:
- Progress bar updates smoothly
- Speed displayed in appropriate units
- ETA displayed in human-readable format
- UI doesn't lag during updates

**Dependencies**: Task 3.3

---

#### Task 3.5: Create DownloadCompletionReceiver
**File**: `android/apps/webavanue/app/src/main/kotlin/com/augmentalis/Avanues/web/app/download/DownloadCompletionReceiver.kt`
**Priority**: P1
**Estimated**: 2 hours

**Actions**:
1. Create `BroadcastReceiver` subclass
2. Listen for `DownloadManager.ACTION_DOWNLOAD_COMPLETE`
3. Update download status in database via ViewModel
4. Stop progress monitoring
5. Register in `AndroidManifest.xml`

**Implementation**:
```kotlin
class DownloadCompletionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadId != -1L) {
                // Update download status
                // viewModel.updateDownloadStatus(downloadId.toString(), DownloadStatus.COMPLETED)
                // progressMonitor.stopMonitoring(downloadId.toString())
            }
        }
    }
}
```

**AndroidManifest.xml**:
```xml
<receiver android:name=".download.DownloadCompletionReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE" />
    </intent-filter>
</receiver>
```

**Acceptance**:
- Receiver registered in manifest
- Completion events trigger UI update
- Progress monitoring stops on completion

**Dependencies**: Task 3.3

---

#### Task 3.6: Write Unit Tests - Progress Calculations
**File**: `Modules/WebAvanue/universal/src/androidTest/kotlin/com/augmentalis/webavanue/download/DownloadProgressMonitorTest.kt`
**Priority**: P1
**Estimated**: 3 hours

**Test Cases**:
```kotlin
@Test fun `calculates download speed correctly`()
@Test fun `calculates ETA based on average speed`()
@Test fun `emits progress updates every 500ms`()
@Test fun `stops monitoring when download completes`()
@Test fun `handles multiple concurrent downloads`()
@Test fun `formatSpeed returns correct units`()
@Test fun `formatETA returns human-readable time`()
```

**Acceptance**:
- All calculation logic tested
- Edge cases covered (0 speed, 0 bytes, etc.)
- 90%+ coverage

**Dependencies**: Task 3.1

---

#### Task 3.7: Write UI Tests - Progress Display
**File**: `android/apps/webavanue/app/src/androidTest/kotlin/com/augmentalis/Avanues/web/app/ui/DownloadProgressUITest.kt`
**Priority**: P2
**Estimated**: 2 hours

**Test Cases**:
```kotlin
@Test fun `progress bar updates when download progress changes`()
@Test fun `speed and ETA displayed during download`()
@Test fun `completed downloads show open button`()
@Test fun `failed downloads show retry button`()
```

**Acceptance**:
- UI tests pass
- Progress updates verified visually

**Dependencies**: Task 3.4

---

**Phase 3 Deliverables**:
- ✅ Real-time progress bars in DownloadListScreen
- ✅ Speed and ETA displayed
- ✅ Completion triggers UI update immediately
- ✅ 7 tasks completed
- ✅ ~18 hours effort

---

### Phase 4: Settings Integration (1 day)

**Goal**: Complete the wiring between settings UI and actual download behavior.

**Tasks**:

#### Task 4.1: Create AskDownloadLocationDialog Composable
**File**: `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/ui/download/AskDownloadLocationDialog.kt`
**Priority**: P1
**Estimated**: 3 hours

**Actions**:
1. Create dialog composable with file picker integration
2. Add "Remember" checkbox (saves path to settings)
3. Show current selected path
4. Add "Change" button to launch file picker
5. Add "Download" and "Cancel" buttons

**Implementation**:
```kotlin
@Composable
fun AskDownloadLocationDialog(
    filename: String,
    defaultPath: String?,
    onPathSelected: (String, rememberChoice: Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var selectedPath by remember { mutableStateOf<String?>(null) }
    var rememberChoice by remember { mutableStateOf(false) }
    val filePickerLauncher = rememberLauncherForActivityResult(...)

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Save Download") },
        text = {
            Column {
                Text("Save \"$filename\" to:")

                // Path display with change button
                Surface(...) {
                    Row(...) {
                        Icon(Icons.Default.Folder, null)
                        Text(selectedPath ?: defaultPath ?: "Downloads")
                        IconButton(onClick = { filePickerLauncher.launch(null) }) {
                            Icon(Icons.Default.FolderOpen, "Change")
                        }
                    }
                }

                // Remember checkbox
                Row {
                    Checkbox(rememberChoice, { rememberChoice = it })
                    Text("Always use this location")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onPathSelected(selectedPath ?: defaultPath ?: "", rememberChoice)
            }) {
                Text("Download")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    )
}
```

**Acceptance**:
- Dialog displays correctly
- File picker integration works
- Remember checkbox saves path
- Cancel cancels download

**Dependencies**: Phase 1 (file picker)

---

#### Task 4.2: Wire "Ask Download Location" to Download Start
**File**: `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/viewmodel/DownloadViewModel.kt`
**Priority**: P1
**Estimated**: 2 hours

**Actions**:
1. Check `askDownloadLocation` setting before download
2. If enabled: emit event to show dialog
3. Suspend `startDownload()` until path selected
4. Use `CompletableDeferred<String>` for async path selection
5. If remember checked: update `downloadPath` setting

**Code Pattern**:
```kotlin
private val _showAskLocationDialog = MutableStateFlow<AskLocationDialogState?>(null)
val showAskLocationDialog: StateFlow<AskLocationDialogState?> = _showAskLocationDialog.asStateFlow()

suspend fun startDownload(...): String? {
    // Check if we should ask for location
    val settings = _settings.value ?: return null

    val downloadPath = if (settings.askDownloadLocation) {
        // Show dialog and wait for user selection
        val deferred = CompletableDeferred<String>()
        _showAskLocationDialog.value = AskLocationDialogState(
            filename = filename,
            onPathSelected = { path, remember ->
                if (remember) {
                    updateSettings(settings.copy(downloadPath = path))
                }
                deferred.complete(path)
                _showAskLocationDialog.value = null
            },
            onCancel = {
                deferred.cancel()
                _showAskLocationDialog.value = null
            }
        )
        deferred.await()
    } else {
        settings.downloadPath ?: getDefaultDownloadPath()
    }

    // Proceed with download using selected path...
}
```

**Acceptance**:
- Dialog shown when `askDownloadLocation == true`
- Download waits for user selection
- Remember option saves path to settings
- Cancel cancels download

**Dependencies**: Task 4.1

---

#### Task 4.3: Implement WiFi-Only Download Check
**File**: `android/apps/webavanue/app/src/main/kotlin/com/augmentalis/Avanues/web/app/download/DownloadHelper.kt`
**Priority**: P1
**Estimated**: 2 hours

**Actions**:
1. Add `isWiFiConnected()` helper method
2. Use `ConnectivityManager.getActiveNetworkInfo()`
3. Check before download start
4. If cellular and `downloadOverWiFiOnly == true`: show error
5. Add "Download Anyway" override option

**Implementation**:
```kotlin
fun isWiFiConnected(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkInfo = cm.activeNetworkInfo
    return networkInfo?.type == ConnectivityManager.TYPE_WIFI
}

fun startDownload(..., downloadOverWiFiOnly: Boolean): Long {
    // Check WiFi requirement
    if (downloadOverWiFiOnly && !isWiFiConnected(context)) {
        // Show error: "WiFi required for downloads"
        return -1
    }

    // Proceed with download...
}
```

**Acceptance**:
- WiFi check works correctly
- Downloads blocked on cellular when setting enabled
- Error message shown with "Change Settings" action
- Override option available

**Dependencies**: None

---

#### Task 4.4: Add Network Change Listener for Queued Downloads
**File**: `android/apps/webavanue/app/src/main/kotlin/com/augmentalis/Avanues/web/app/download/NetworkChangeReceiver.kt`
**Priority**: P2
**Estimated**: 2 hours

**Actions**:
1. Create `BroadcastReceiver` for network changes
2. Listen for WiFi connection
3. Resume queued downloads automatically
4. Register in manifest

**Implementation**:
```kotlin
class NetworkChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION == intent.action) {
            if (DownloadHelper.isWiFiConnected(context)) {
                // Resume queued downloads
                // viewModel.resumeQueuedDownloads()
            }
        }
    }
}
```

**Acceptance**:
- Queued downloads resume on WiFi connection
- No unnecessary battery drain
- Graceful handling

**Dependencies**: Task 4.3

---

#### Task 4.5: Write Integration Tests - Complete Download Flow
**File**: `android/apps/webavanue/app/src/androidTest/kotlin/com/augmentalis/Avanues/web/app/integration/DownloadFlowIntegrationTest.kt`
**Priority**: P1
**Estimated**: 3 hours

**Test Scenarios**:
```kotlin
@Test fun `complete download flow with custom path`()
@Test fun `ask location flow with remember choice`()
@Test fun `wifi only setting blocks cellular downloads`()
@Test fun `queued downloads resume on WiFi connection`()
@Test fun `permission denial reverts to default path`()
```

**Acceptance**:
- End-to-end scenarios work
- All settings have effect
- Edge cases handled

**Dependencies**: All Phase 4 tasks

---

**Phase 4 Deliverables**:
- ✅ All settings have immediate effect
- ✅ Ask location dialog works correctly
- ✅ WiFi-only blocks cellular downloads
- ✅ Settings persist across restarts
- ✅ 5 tasks completed
- ✅ ~12 hours effort

---

### Phase 5: Testing & Polish (1 day)

**Goal**: Ensure quality, accessibility, and zero regressions.

**Tasks**:

#### Task 5.1: Run Full Test Suite
**Priority**: P0
**Estimated**: 2 hours

**Actions**:
1. Run all unit tests (`./gradlew test`)
2. Run all instrumented tests (`./gradlew connectedAndroidTest`)
3. Generate coverage report (JaCoCo)
4. Fix failing tests
5. Ensure 90%+ coverage

**Acceptance**:
- All tests pass
- Coverage ≥ 90%
- No flaky tests

**Dependencies**: All previous phases

---

#### Task 5.2: Accessibility Testing with TalkBack
**Priority**: P1
**Estimated**: 2 hours

**Actions**:
1. Enable TalkBack on test device
2. Navigate through settings → downloads
3. Test file picker with screen reader
4. Test download progress screen
5. Verify all buttons have content descriptions
6. Fix accessibility issues

**Checklist**:
- [ ] File picker launches via TalkBack
- [ ] Browse/Delete buttons announced correctly
- [ ] Progress bars have descriptive labels
- [ ] Error messages read aloud
- [ ] Dialogs have proper focus management

**Acceptance**:
- All interactions work with TalkBack
- Content descriptions present
- Focus order logical

**Dependencies**: All UI components implemented

---

#### Task 5.3: Performance Profiling
**Priority**: P2
**Estimated**: 2 hours

**Actions**:
1. Profile progress update overhead (Android Profiler)
2. Check for memory leaks (LeakCanary)
3. Measure frame drops during downloads
4. Optimize if needed (reduce polling frequency, batch updates)

**Metrics**:
| Metric | Target | Actual |
|--------|--------|--------|
| Progress update lag | < 1 second | __ |
| Frame rate during download | > 55 FPS | __ |
| Memory overhead | < 10MB | __ |
| Battery drain (1 hour) | < 5% | __ |

**Acceptance**:
- All metrics meet targets
- No performance regressions

**Dependencies**: Phase 3 (progress monitoring)

---

#### Task 5.4: Code Review & Documentation
**Priority**: P1
**Estimated**: 2 hours

**Actions**:
1. Self-review all new code
2. Check for TODO/FIXME comments (remove or address)
3. Verify all public APIs have KDoc
4. Update README.md with download architecture
5. Add troubleshooting guide to docs

**Documentation Checklist**:
- [ ] All classes have KDoc headers
- [ ] All public methods documented
- [ ] README updated with component diagram
- [ ] Troubleshooting guide added
- [ ] No TODO/FIXME in production code

**Acceptance**:
- Code review complete
- Documentation comprehensive
- No outstanding issues

**Dependencies**: All code complete

---

#### Task 5.5: Manual Regression Testing
**Priority**: P0
**Estimated**: 2 hours

**Actions**:
1. Run through all acceptance test scripts (from spec)
2. Test on API 28, API 33 emulators
3. Test on physical device (if available)
4. Verify existing features still work
5. Fix any discovered bugs

**Test Coverage**:
- [ ] File picker integration (Test Case 1)
- [ ] Permission handling (Test Case 2)
- [ ] Path validation (Test Case 3)
- [ ] Progress monitoring (Test Case 4)
- [ ] Ask location dialog (Test Case 5)
- [ ] WiFi-only downloads (Test Case 6)

**Acceptance**:
- All manual tests pass
- Zero regressions
- Bugs fixed

**Dependencies**: All features implemented

---

**Phase 5 Deliverables**:
- ✅ 90%+ test coverage
- ✅ All acceptance criteria met
- ✅ Zero regressions
- ✅ Accessible to screen reader users
- ✅ Performance targets met
- ✅ 5 tasks completed
- ✅ ~10 hours effort

---

## File Structure

### New Files to Create (6)

| File | Purpose | Lines | Priority |
|------|---------|-------|----------|
| `universal/src/commonMain/.../platform/DownloadFilePickerLauncher.kt` | expect class for file picker | 15 | P0 |
| `universal/src/androidMain/.../platform/DownloadFilePickerLauncher.kt` | Android file picker impl | 80 | P0 |
| `universal/src/commonMain/.../platform/DownloadPermissionManager.kt` | expect class for permissions | 20 | P0 |
| `universal/src/androidMain/.../platform/DownloadPermissionManager.kt` | Android permission impl | 120 | P0 |
| `universal/src/commonMain/.../platform/DownloadPathValidator.kt` | expect class for validation | 15 | P1 |
| `universal/src/androidMain/.../platform/DownloadPathValidator.kt` | Android validation impl | 80 | P1 |
| `universal/src/androidMain/.../download/DownloadProgressMonitor.kt` | Progress monitoring | 120 | P0 |
| `universal/src/commonMain/.../ui/download/AskDownloadLocationDialog.kt` | Ask location UI | 100 | P1 |
| `app/src/main/.../download/DownloadCompletionReceiver.kt` | Completion broadcast | 40 | P1 |
| `app/src/main/.../download/NetworkChangeReceiver.kt` | Network change listener | 40 | P2 |

**Total**: ~630 lines of new code

---

### Files to Modify (4)

| File | Changes | Lines Modified | Priority |
|------|---------|---------------|----------|
| `SettingsScreen.kt` | Update DownloadPathSettingItem (line ~2430) | ~50 | P0 |
| `DownloadViewModel.kt` | Wire progress monitor + ask location | ~80 | P0 |
| `DownloadHelper.kt` | Add permission check + WiFi check | ~30 | P1 |
| `Download.kt` (model) | Add progress fields | ~10 | P0 |
| `DownloadItem.kt` | Add progress UI | ~60 | P0 |

**Total**: ~230 lines modified

---

### Test Files (10)

| File | Tests | Lines | Priority |
|------|-------|-------|----------|
| `DownloadFilePickerLauncherTest.kt` | 4 tests | 120 | P1 |
| `DownloadPermissionManagerTest.kt` | 4 tests | 120 | P1 |
| `DownloadPathValidatorTest.kt` | 5 tests | 150 | P1 |
| `DownloadProgressMonitorTest.kt` | 7 tests | 200 | P1 |
| `DownloadSettingsUITest.kt` | 4 tests | 150 | P2 |
| `DownloadProgressUITest.kt` | 4 tests | 150 | P2 |
| `DownloadFlowIntegrationTest.kt` | 5 tests | 250 | P1 |

**Total**: ~1,140 lines of test code

---

## Dependencies & Requirements

### Android Dependencies (No New)
All required dependencies already in project:
- ✅ `androidx.activity:activity-compose:1.8.2`
- ✅ `androidx.compose.material3:material3:1.2.0`
- ✅ `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3`

### Android Manifest Updates
Add receivers:
```xml
<receiver android:name=".download.DownloadCompletionReceiver" android:exported="true">
    <intent-filter>
        <action android:name="android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE" />
    </intent-filter>
</receiver>

<receiver android:name=".download.NetworkChangeReceiver" android:exported="true">
    <intent-filter>
        <action android:name="android.net.conn.CONNECTIVITY_CHANGE" />
    </intent-filter>
</receiver>

<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

---

## Risk Mitigation

| Risk | Impact | Mitigation | Contingency |
|------|--------|------------|-------------|
| SAF picker crashes on some devices | High | Test on top 10 devices | Fallback to text entry |
| Permission denial breaks downloads | High | Always fallback to default Downloads | Show clear error message |
| Progress polling drains battery | Medium | Use 500ms interval, stop when backgrounded | Add setting to disable |
| File path changes after selection | Medium | Revalidate on app startup | Auto-revert to default |

---

## Success Metrics

### Quantitative
| Metric | Target | Tracking |
|--------|--------|----------|
| Test coverage | > 90% | JaCoCo report |
| Feature adoption | > 30% | Analytics: askDownloadLocation enabled |
| Custom path usage | > 15% | Analytics: non-null downloadPath |
| Permission grant rate | > 85% | Analytics: granted/(granted+denied) |
| Error rate | < 5% | Analytics: failed/total downloads |

### Qualitative
- User feedback: 4.5+/5.0 for "Downloading files is easy"
- Support tickets: < 5 tickets/month related to downloads
- Zero high-severity bugs in production

---

## Next Steps

### After Plan Approval

**Option 1: Generate Tasks**
```
/i.tasks WebAvanue-Plan-Downloads-51212-V1.md
```
Creates detailed task list in TodoWrite for tracking.

**Option 2: Start Implementation**
```
/i.implement WebAvanue-Plan-Downloads-51212-V1.md
```
Begins Phase 1 implementation immediately.

**Option 3: Full Workflow (.yolo)**
```
/i.develop .yolo WebAvanue-Spec-Downloads-51212-V1.md
```
Runs: Plan → Tasks → Implement → Test → Commit (autonomous).

---

## Timeline Summary

| Phase | Duration | Tasks | Deliverables |
|-------|----------|-------|-------------|
| Phase 1: File Picker & Permissions | 2-3 days | 8 | ✅ File picker + permission handling |
| Phase 2: Path Validation | 1 day | 5 | ✅ Validation + error handling |
| Phase 3: Progress Monitoring | 2 days | 7 | ✅ Real-time progress UI |
| Phase 4: Settings Integration | 1 day | 5 | ✅ Complete settings wiring |
| Phase 5: Testing & Polish | 1 day | 5 | ✅ Quality assurance |
| **Total** | **7-8 days** | **30** | **Production-ready downloads** |

---

**Ready to proceed?**