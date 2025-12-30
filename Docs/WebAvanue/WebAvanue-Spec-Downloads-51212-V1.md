# WebAvanue Download Management Features - Specification

**Document**: WebAvanue-Spec-Downloads-51212-V1.md
**Project**: WebAvanue (NewAvanues Monorepo)
**Platform**: Android
**Version**: 1.0
**Date**: 2025-12-12
**Author**: AI Assistant (Claude Code)
**Status**: Draft

---

## Executive Summary

This specification defines four critical enhancements to WebAvanue's download management system to bring it to production-ready state. Currently, basic download functionality exists but lacks user-facing controls for location selection, permission handling, real-time progress monitoring, and full settings integration. This spec addresses the remaining **~10% of implementation** to complete the download feature set.

### Scope
1. **File Picker Dialog**: Replace manual text entry with Android Storage Access Framework (SAF) picker
2. **Permission Handling**: Implement Android storage permissions with validation for custom paths
3. **Progress Observer**: Wire DownloadManager progress to UI via reactive Flow
4. **Settings Integration**: Complete wiring between settings UI and actual download behavior

### Success Criteria
- Users can select download locations via native file picker
- Custom paths validated for writability and permissions
- Download progress updates in real-time in DownloadListScreen
- Settings changes immediately affect download behavior
- Zero regressions in existing download functionality

---

## Problem Statement

### Current State Analysis

**Existing Implementation** (`WebAvanue-WebAvanue-51212-V1`):
- ✅ `BrowserSettings.downloadPath` exists (line 51)
- ✅ `BrowserSettings.askDownloadLocation` exists (line 52)
- ✅ Settings UI displays download options (SettingsScreen.kt:626-660)
- ✅ DownloadViewModel validates and saves downloads
- ✅ DownloadHelper uses Android DownloadManager
- ✅ DownloadListScreen displays download list

**Critical Gaps**:
1. **No File Picker**: `DownloadPathSettingItem` (SettingsScreen.kt:2428-2478) uses manual text entry instead of SAF picker
2. **No Permission Handling**: No runtime permission requests or validation for custom paths
3. **No Progress Updates**: DownloadHelper.queryProgress() exists but not wired to UI observers
4. **Incomplete Integration**: Settings exist but don't trigger file picker or validate paths

### User Impact

**Without These Features**:
- ❌ Users must manually type paths (error-prone, poor UX)
- ❌ No feedback if path is invalid or unwritable
- ❌ Downloads start but users can't see progress
- ❌ Settings appear broken (toggle has no visible effect)

**With These Features**:
- ✅ Native file picker (familiar Android UX)
- ✅ Automatic permission requests
- ✅ Real-time progress bars and notifications
- ✅ Settings work as expected

---

## Functional Requirements

### FR-001: File Picker Dialog Integration

**Priority**: P0 (Blocking)
**Platform**: Android (API 21+)

**Description**:
Replace manual text entry in `DownloadPathSettingItem` with Android Storage Access Framework's directory picker.

**Acceptance Criteria**:
- AC-001.1: Clicking "Download Location" in settings opens system file picker
- AC-001.2: User can navigate and select any accessible directory
- AC-001.3: Selected path is persisted to `BrowserSettings.downloadPath`
- AC-001.4: Path is displayed in settings UI after selection
- AC-001.5: "Use Default" button clears custom path (sets to null)
- AC-001.6: Persisted URI permissions survive app restart

**Technical Requirements**:
- Use `Intent.ACTION_OPEN_DOCUMENT_TREE` for directory selection
- Call `takePersistableUriPermission()` to retain access
- Convert `content://` URI to display-friendly path for UI
- Store both URI and display path in settings

**Dependencies**:
- Android API 21+ (Lollipop)
- `androidx.activity:activity-compose` for result contracts

**Test Coverage**:
- Unit test: ViewModel handles URI permission grants
- UI test: File picker launches and returns valid URI
- Integration test: Selected path persists across app restart

---

### FR-002: Storage Permission Handling

**Priority**: P0 (Blocking)
**Platform**: Android (API 21-33+)

**Description**:
Implement runtime permission requests for storage access with graceful degradation across Android versions.

**Acceptance Criteria**:
- AC-002.1: App requests appropriate permissions based on API level
- AC-002.2: Permission rationale shown before first request
- AC-002.3: User can proceed to system settings if permission denied
- AC-002.4: Downloads fail gracefully with error message if permission denied
- AC-002.5: No permissions required for default Download folder (scoped storage)

**API-Level Requirements**:

| API Level | Permissions Required | Behavior |
|-----------|---------------------|----------|
| 21-28 | `WRITE_EXTERNAL_STORAGE` | Request at download or settings change |
| 29-32 | None (scoped storage) | Direct access to app-specific and Downloads |
| 33+ | None (scoped storage) | Use MediaStore or SAF for custom locations |

**Permission Request Flow**:
1. User selects custom download location
2. Check if permission required for API level
3. If required: Show rationale dialog → Request permission
4. If granted: Proceed with selection
5. If denied: Show error + "Go to Settings" button
6. Fallback: Use default Downloads folder

**Technical Requirements**:
- Use `ActivityCompat.requestPermissions()` for API < 29
- Use `registerForActivityResult()` with `RequestPermission` contract
- Store permission state in ViewModel
- Show Snackbar with clear error messages

**Test Coverage**:
- Unit test: Permission check logic for each API level
- UI test: Permission rationale dialog displays
- Integration test: Permission denial handled gracefully

---

### FR-003: Custom Path Validation

**Priority**: P1 (High)
**Platform**: Android

**Description**:
Validate custom download paths for writability, existence, and available storage space before accepting.

**Acceptance Criteria**:
- AC-003.1: Path validated immediately after selection
- AC-003.2: Error shown if path doesn't exist or isn't writable
- AC-003.3: Warning shown if available space < 100MB
- AC-003.4: Validation runs on app startup for saved path
- AC-003.5: Reverted to default if saved path becomes invalid

**Validation Checks**:

| Check | Error Message | Action |
|-------|---------------|--------|
| Path doesn't exist | "Selected folder no longer exists" | Revert to default |
| Not writable | "Cannot write to this location" | Show permission prompt or revert |
| < 100MB free | "Low storage space (X MB free)" | Warning only, allow |
| Permission revoked | "Storage permission revoked" | Request permission again |

**Technical Requirements**:
- Use `DocumentFile.canWrite()` for URI-based paths
- Use `StatFs` for free space calculation
- Validate on path selection, app startup, and download start
- Store validation results in ViewModel state

**Test Coverage**:
- Unit test: Validation logic for each failure case
- Integration test: Path auto-reverts when external SD card removed

---

### FR-004: Download Progress Observer

**Priority**: P0 (Blocking)
**Platform**: Android

**Description**:
Wire DownloadManager progress to UI using reactive Flows, updating DownloadListScreen and system notifications in real-time.

**Acceptance Criteria**:
- AC-004.1: Download progress updates every 500ms in UI
- AC-004.2: Progress bar shows percentage (0-100%)
- AC-004.3: Download speed displayed (KB/s, MB/s)
- AC-004.4: ETA displayed (e.g., "2 minutes remaining")
- AC-004.5: Completion triggers UI update immediately
- AC-004.6: Progress persists during configuration changes

**Architecture**:

```
DownloadCompletionReceiver (BroadcastReceiver)
  └─> observes DownloadManager.ACTION_DOWNLOAD_COMPLETE

DownloadProgressMonitor (Background Service/Worker)
  └─> polls DownloadHelper.queryProgress() every 500ms
  └─> emits to StateFlow<Map<DownloadId, DownloadProgress>>

DownloadViewModel
  └─> collects progress flow
  └─> updates _downloads StateFlow

DownloadListScreen
  └─> collectAsState() from ViewModel
  └─> renders progress bars
```

**Technical Requirements**:
- Create `DownloadProgressMonitor` class with coroutine-based polling
- Register `DownloadCompletionReceiver` in AndroidManifest
- Emit progress to `MutableStateFlow<Map<String, DownloadProgress>>`
- Calculate speed: `(currentBytes - lastBytes) / timeDelta`
- Calculate ETA: `remainingBytes / averageSpeed`

**Progress States**:

| State | UI Display |
|-------|-----------|
| PENDING | "Waiting..." (indeterminate) |
| RUNNING | Progress bar + speed + ETA |
| PAUSED | "Paused" + resume button |
| SUCCESSFUL | "Complete" + open button |
| FAILED | "Failed: [reason]" + retry button |

**Test Coverage**:
- Unit test: Progress calculation logic (speed, ETA)
- UI test: Progress bar updates during mock download
- Integration test: Real download shows progress → completion

---

### FR-005: Ask Download Location Flow

**Priority**: P1 (High)
**Platform**: Android

**Description**:
When `askDownloadLocation` setting is enabled, show file picker before each download starts.

**Acceptance Criteria**:
- AC-005.1: File picker shown immediately when download initiated
- AC-005.2: Download waits for user selection
- AC-005.3: Download uses selected path (one-time, doesn't save to settings)
- AC-005.4: Cancel picker cancels download
- AC-005.5: Remember choice checkbox: "Always use this location"
- AC-005.6: Works for both manual downloads and WebView-initiated downloads

**User Flow**:
1. User clicks download link in WebView
2. WebView calls `DownloadViewModel.startDownload()`
3. If `askDownloadLocation == true`:
   - Show file picker dialog
   - Wait for user selection
   - Use selected path for this download only
4. If checkbox checked: Update `BrowserSettings.downloadPath`
5. Enqueue download to DownloadManager

**Technical Requirements**:
- Suspend `startDownload()` coroutine until path selected
- Use `CompletableDeferred<Uri>` for async path selection
- Pass selected URI to DownloadManager request
- Show Material3 dialog with picker + checkbox

**Test Coverage**:
- UI test: Picker shows before download starts
- Integration test: Download uses selected path
- Unit test: Checkbox saves path to settings

---

### FR-006: Settings Integration Completion

**Priority**: P1 (High)
**Platform**: Android

**Description**:
Complete the wiring between settings UI and actual download behavior, ensuring all settings have immediate effect.

**Acceptance Criteria**:
- AC-006.1: Changing `downloadPath` validates and applies immediately
- AC-006.2: Toggling `askDownloadLocation` affects next download
- AC-006.3: `downloadOverWiFiOnly` blocks cellular downloads
- AC-006.4: Settings persist across app restarts
- AC-006.5: Invalid settings revert to defaults automatically

**Settings Wiring**:

| Setting | Current State | Required Action |
|---------|--------------|-----------------|
| `downloadPath` | Text field only | Add "Browse" button → launch picker |
| `askDownloadLocation` | Switch works | Wire to download start flow (FR-005) |
| `downloadOverWiFiOnly` | Switch works | Check network type in DownloadHelper |

**Enhanced DownloadPathSettingItem**:
```kotlin
Row {
  Text(currentPath ?: "Default")
  IconButton(onClick = { launchFilePicker() }) {
    Icon(Icons.Default.FolderOpen, "Browse")
  }
  IconButton(onClick = { onPathChanged(null) }) {
    Icon(Icons.Default.Delete, "Use Default")
  }
}
```

**WiFi-Only Implementation**:
- Check `ConnectivityManager.getActiveNetworkInfo()` before download
- If cellular and `downloadOverWiFiOnly == true`: Show error dialog
- Allow user to override: "Download Anyway" button
- Queue downloads automatically when WiFi reconnects

**Technical Requirements**:
- Add `isWiFiConnected()` helper in DownloadHelper
- Show Snackbar: "WiFi required for downloads" when blocked
- Store queued downloads in database, retry on network change

**Test Coverage**:
- UI test: Browse button launches picker
- Unit test: WiFi check logic for each network type
- Integration test: Download blocked on cellular, proceeds on WiFi

---

## Non-Functional Requirements

### NFR-001: Performance

| Metric | Target | Measurement |
|--------|--------|-------------|
| File picker launch time | < 500ms | Time from click to picker visible |
| Progress update frequency | 500ms ± 100ms | Time between UI updates |
| Permission check overhead | < 50ms | Additional time per download |
| Settings save latency | < 100ms | Time from toggle to database write |

**Optimization Strategies**:
- Cache permission state in ViewModel (avoid repeated checks)
- Batch progress updates (collect 500ms of changes, emit once)
- Use `remember` for expensive Composables (file picker launcher)
- Debounce path validation (wait 300ms after input stops)

---

### NFR-002: Accessibility

**Requirements**:
- File picker: Native Android picker (inherently accessible)
- Progress bars: Content description with percentage ("45% complete")
- Buttons: Minimum 48dp touch target
- Dialogs: Focus management (auto-focus first input)
- Snackbars: Persistent until dismissed (no auto-hide for errors)

**Testing**:
- TalkBack: All interactive elements announced correctly
- Switch Access: Tab order follows visual flow
- Large text: UI scales up to 200% without clipping

---

### NFR-003: Security

**Threat Model**:

| Threat | Mitigation | Priority |
|--------|------------|----------|
| Path traversal attacks | Sanitize filenames (already done in DownloadViewModel:82-87) | ✅ Implemented |
| Malicious file types | Block dangerous extensions (already done:95-102) | ✅ Implemented |
| Permission escalation | Use scoped storage on API 29+ | P0 |
| URI injection | Validate SAF URIs (check scheme == "content://") | P1 |
| Storage overflow | Check free space before download (FR-003) | P1 |

**Additional Mitigations**:
- Never execute downloaded files automatically
- Scan downloads with Play Protect APIs (future enhancement)
- Revoke URI permissions when download deleted
- Log all permission grants for audit trail

---

### NFR-004: Compatibility

**API Level Support**:

| API Level | Support Level | Notes |
|-----------|--------------|-------|
| 21-22 (Lollipop) | Full | Requires storage permissions |
| 23-28 (Marshmallow-Pie) | Full | Runtime permission requests |
| 29-32 (Q-S) | Full | Scoped storage, no permissions |
| 33-34 (Tiramisu-U) | Full | Enhanced scoped storage |
| 35+ (Future) | Full | Design for forward compatibility |

**Device Support**:
- Phones: Full support (primary target)
- Tablets: Full support (benefits from large screen)
- Foldables: Adaptive layout (two-pane when unfolded)
- Chrome OS: Full support (uses Android file picker)
- AR Glasses: Limited support (no file picker, use voice commands)

---

## Platform-Specific Implementation

### Android

**Core Components**:

| Component | File | Purpose |
|-----------|------|---------|
| `DownloadFilePickerLauncher` | `universal/src/androidMain/.../platform/` | Wraps SAF intents |
| `DownloadPermissionManager` | `universal/src/androidMain/.../platform/` | Permission requests |
| `DownloadPathValidator` | `universal/src/androidMain/.../platform/` | Path validation |
| `DownloadProgressMonitor` | `universal/src/androidMain/.../download/` | Progress polling |
| `DownloadCompletionReceiver` | `app/src/main/.../download/` | Completion events |

**Compose Integration**:
```kotlin
// In DownloadPathSettingItem
val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocumentTree()
) { uri: Uri? ->
    uri?.let { onPathChanged(it.toString()) }
}

Button(onClick = { filePickerLauncher.launch(null) }) {
    Text("Browse")
}
```

**Permissions (AndroidManifest.xml)**:
```xml
<!-- Required for API < 29 -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />

<!-- Required for DownloadManager notifications -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

**BroadcastReceiver (AndroidManifest.xml)**:
```xml
<receiver android:name=".download.DownloadCompletionReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE" />
    </intent-filter>
</receiver>
```

---

## API Contracts

### DownloadFilePickerLauncher API

**Package**: `com.augmentalis.Avanues.web.universal.platform`

```kotlin
/**
 * Launches Android Storage Access Framework file picker
 *
 * @param initialUri Optional URI to start navigation at
 * @return Selected directory URI or null if cancelled
 */
expect class DownloadFilePickerLauncher {
    suspend fun pickDownloadDirectory(initialUri: String? = null): String?
}
```

**Android Implementation**:
```kotlin
actual class DownloadFilePickerLauncher(
    private val context: Context
) {
    actual suspend fun pickDownloadDirectory(initialUri: String?): String? {
        return suspendCancellableCoroutine { continuation ->
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                initialUri?.let { putExtra(DocumentsContract.EXTRA_INITIAL_URI, Uri.parse(it)) }
            }

            // Launch picker and await result...
            // (implementation details omitted for brevity)
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

---

### DownloadPermissionManager API

**Package**: `com.augmentalis.Avanues.web.universal.platform`

```kotlin
/**
 * Manages storage permissions across Android versions
 */
expect class DownloadPermissionManager {
    /**
     * Check if storage permission required for current API level
     */
    fun isPermissionRequired(): Boolean

    /**
     * Check if permission currently granted
     */
    fun isPermissionGranted(): Boolean

    /**
     * Request storage permission (suspends until result)
     */
    suspend fun requestPermission(): Boolean

    /**
     * Open app settings for manual permission grant
     */
    fun openPermissionSettings()
}
```

---

### DownloadPathValidator API

**Package**: `com.augmentalis.Avanues.web.universal.platform`

```kotlin
/**
 * Validates download paths for writability and space
 */
expect class DownloadPathValidator {
    /**
     * Validate path and return result
     */
    suspend fun validate(path: String): ValidationResult
}

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val availableSpaceMB: Long = 0,
    val isLowSpace: Boolean = false // < 100MB
)
```

---

### DownloadProgressMonitor API

**Package**: `com.augmentalis.Avanues.web.universal.download`

```kotlin
/**
 * Monitors download progress and emits updates
 */
class DownloadProgressMonitor(
    private val downloadManager: DownloadManager,
    private val scope: CoroutineScope
) {
    /**
     * Flow of download progress updates
     * Key: Download ID, Value: Progress info
     */
    val progressFlow: StateFlow<Map<String, DownloadProgress>>

    /**
     * Start monitoring download
     */
    fun startMonitoring(downloadId: String)

    /**
     * Stop monitoring download
     */
    fun stopMonitoring(downloadId: String)

    /**
     * Stop all monitoring
     */
    fun stopAll()
}
```

---

## Data Models

### Enhanced Download Model

**File**: `Modules/WebAvanue/coredata/src/commonMain/kotlin/com/augmentalis/webavanue/domain/model/Download.kt`

```kotlin
data class Download(
    // ... existing fields ...

    // New fields for progress tracking
    val downloadManagerId: Long? = null,  // Android DownloadManager ID
    val downloadSpeed: Long = 0,           // Bytes per second
    val estimatedTimeRemaining: Long = 0,  // Seconds
    val lastProgressUpdate: Long = 0       // Timestamp (ms)
)
```

---

## User Interface

### Modified DownloadPathSettingItem

**File**: `SettingsScreen.kt` (line ~2430)

**Before**:
```kotlin
ListItem(
    headlineContent = { Text("Download Location") },
    supportingContent = { Text(currentPath ?: "Default") },
    modifier = modifier.clickable { showDialog = true }
)
// Dialog with text field...
```

**After**:
```kotlin
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
            IconButton(onClick = { launchFilePicker() }) {
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

---

### Download Progress UI (DownloadItem.kt)

**File**: `Modules/WebAvanue/universal/src/commonMain/kotlin/.../ui/download/DownloadItem.kt`

**Enhanced Progress Display**:
```kotlin
@Composable
fun DownloadItem(download: Download, ...) {
    Card {
        Column {
            // File name and size
            Text(download.filename, style = MaterialTheme.typography.bodyLarge)
            Text(formatFileSize(download.fileSize), style = caption)

            // Progress bar (if downloading)
            if (download.status == DownloadStatus.DOWNLOADING) {
                LinearProgressIndicator(
                    progress = download.progress,
                    modifier = Modifier.fillMaxWidth()
                )

                // Speed and ETA
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${formatSpeed(download.downloadSpeed)}")
                    Text("${formatETA(download.estimatedTimeRemaining)} remaining")
                }
            }

            // Action buttons
            Row {
                when (download.status) {
                    DOWNLOADING -> IconButton(onClick = onCancel) { Icon(Icons.Default.Close, "Cancel") }
                    FAILED -> IconButton(onClick = onRetry) { Icon(Icons.Default.Refresh, "Retry") }
                    COMPLETED -> IconButton(onClick = onClick) { Icon(Icons.Default.Launch, "Open") }
                }
            }
        }
    }
}
```

---

### Ask Location Dialog

**New Composable**: `AskDownloadLocationDialog.kt`

```kotlin
@Composable
fun AskDownloadLocationDialog(
    filename: String,
    defaultPath: String?,
    onPathSelected: (String, rememberChoice: Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var rememberChoice by remember { mutableStateOf(false) }
    val filePickerLauncher = rememberLauncherForActivityResult(...)

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Save Download") },
        text = {
            Column {
                Text("Save \"$filename\" to:")

                // Current selection
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, null)
                        Spacer(Modifier.width(8.dp))
                        Text(selectedPath ?: defaultPath ?: "Downloads", modifier = Modifier.weight(1f))
                        IconButton(onClick = { filePickerLauncher.launch(null) }) {
                            Icon(Icons.Default.FolderOpen, "Change")
                        }
                    }
                }

                // Remember choice checkbox
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = rememberChoice, onCheckedChange = { rememberChoice = it })
                    Text("Always use this location")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onPathSelected(selectedPath ?: defaultPath ?: "", rememberChoice) }) {
                Text("Download")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
    )
}
```

---

## Error Handling

### Error Scenarios

| Error | User Message | Recovery |
|-------|--------------|----------|
| Permission denied | "Storage permission required to download files" | "Grant Permission" button → settings |
| Path not writable | "Cannot write to selected location" | Revert to default + notify user |
| Insufficient space | "Not enough storage space (X MB required, Y MB available)" | Cancel download or choose different location |
| Network unavailable | "No internet connection" | Retry when connected |
| WiFi required | "WiFi required for downloads. Change settings or connect to WiFi." | "Download Anyway" / "Change Settings" |
| File already exists | "File already exists: [filename]" | "Overwrite" / "Keep Both" / "Cancel" |
| Download failed | "Download failed: [reason]" | "Retry" button |

---

## Testing Strategy

### Unit Tests (90% coverage target)

**DownloadPathValidator Tests**:
```kotlin
@Test fun `validate returns error when path not writable`()
@Test fun `validate returns warning when space less than 100MB`()
@Test fun `validate succeeds for valid path with sufficient space`()
```

**DownloadProgressMonitor Tests**:
```kotlin
@Test fun `calculates download speed correctly`()
@Test fun `calculates ETA based on average speed`()
@Test fun `emits progress updates every 500ms`()
@Test fun `stops monitoring when download completes`()
```

**DownloadPermissionManager Tests**:
```kotlin
@Test fun `returns true for API 29+ (no permission required)`()
@Test fun `returns false for API 28 when permission not granted`()
@Test fun `requestPermission returns result`()
```

---

### UI Tests

**File Picker Tests**:
```kotlin
@Test fun `clicking browse button launches file picker`()
@Test fun `selected path displays in settings after picker returns`()
@Test fun `delete button resets to default path`()
```

**Download Progress Tests**:
```kotlin
@Test fun `progress bar updates when download progress changes`()
@Test fun `speed and ETA displayed during download`()
@Test fun `completed downloads show open button`()
```

**Ask Location Dialog Tests**:
```kotlin
@Test fun `dialog shows when askDownloadLocation enabled`()
@Test fun `remember checkbox saves path to settings`()
@Test fun `cancel button cancels download`()
```

---

### Integration Tests

**End-to-End Scenarios**:
```kotlin
@Test fun `complete download flow with custom path`() {
    // 1. Open settings
    // 2. Click "Download Location"
    // 3. Select custom folder via picker
    // 4. Navigate to web page
    // 5. Click download link
    // 6. Verify download goes to custom path
    // 7. Verify progress updates in UI
    // 8. Verify completion notification
}

@Test fun `ask location flow with remember choice`() {
    // 1. Enable "Ask Download Location"
    // 2. Initiate download
    // 3. Select path in dialog
    // 4. Check "Remember" checkbox
    // 5. Start another download
    // 6. Verify no dialog shown (uses remembered path)
}

@Test fun `wifi only setting blocks cellular downloads`() {
    // 1. Enable "WiFi Only"
    // 2. Switch to cellular network
    // 3. Initiate download
    // 4. Verify error shown
    // 5. Switch to WiFi
    // 6. Verify download proceeds
}
```

---

## Implementation Plan

### Phase 1: File Picker & Permissions (2-3 days)

**Tasks**:
1. Create `DownloadFilePickerLauncher` (expect/actual)
2. Create `DownloadPermissionManager` (expect/actual)
3. Update `DownloadPathSettingItem` with Browse button
4. Implement permission request flow
5. Add permission check to `DownloadHelper.startDownload()`
6. **Tests**: Unit tests for permission logic, UI tests for picker

**Deliverables**:
- ✅ File picker launches from settings
- ✅ Selected path saved and displayed
- ✅ Permission requested when needed
- ✅ Graceful fallback to default on denial

---

### Phase 2: Path Validation (1 day)

**Tasks**:
1. Create `DownloadPathValidator` (expect/actual)
2. Implement validation checks (writable, space, exists)
3. Add validation on path selection
4. Add validation on app startup
5. Show Snackbar for validation errors
6. **Tests**: Unit tests for all validation cases

**Deliverables**:
- ✅ Invalid paths detected and reported
- ✅ Low space warnings shown
- ✅ Auto-revert to default on startup if path invalid

---

### Phase 3: Progress Monitoring (2 days)

**Tasks**:
1. Create `DownloadProgressMonitor` class
2. Implement progress polling (500ms interval)
3. Calculate speed and ETA
4. Emit progress to StateFlow
5. Update `DownloadViewModel` to collect progress
6. Update `DownloadItem` UI with progress display
7. Create `DownloadCompletionReceiver`
8. **Tests**: Unit tests for calculations, UI tests for progress updates

**Deliverables**:
- ✅ Real-time progress bars in DownloadListScreen
- ✅ Speed and ETA displayed
- ✅ Completion triggers UI update immediately

---

### Phase 4: Settings Integration (1 day)

**Tasks**:
1. Wire "Ask Download Location" to download start
2. Create `AskDownloadLocationDialog` composable
3. Implement WiFi-only check in `DownloadHelper`
4. Add network state listener for queued downloads
5. Update settings to trigger validation on change
6. **Tests**: Integration tests for complete flows

**Deliverables**:
- ✅ All settings have immediate effect
- ✅ Ask location dialog works correctly
- ✅ WiFi-only blocks cellular downloads
- ✅ Settings persist across restarts

---

### Phase 5: Testing & Polish (1 day)

**Tasks**:
1. Run full test suite (unit + UI + integration)
2. Fix any discovered bugs
3. Accessibility testing with TalkBack
4. Performance profiling (progress update overhead)
5. Update documentation
6. Code review

**Deliverables**:
- ✅ 90%+ test coverage
- ✅ All acceptance criteria met
- ✅ Zero regressions
- ✅ Accessible to screen reader users

---

## Dependencies

### Internal Dependencies

| Component | Dependency | Reason |
|-----------|------------|--------|
| File Picker | `DownloadPermissionManager` | Check permission before launch |
| Progress Monitor | `DownloadHelper` | Query download status |
| Ask Location Dialog | `DownloadFilePickerLauncher` | Picker integration |
| Settings UI | `DownloadPathValidator` | Real-time validation |

**Dependency Graph**:
```
DownloadHelper
  ├─> DownloadPermissionManager (check permission)
  └─> DownloadPathValidator (validate path)
      └─> DownloadFilePickerLauncher (picker for custom path)

DownloadViewModel
  ├─> DownloadProgressMonitor (observe progress)
  └─> DownloadHelper (start downloads)

DownloadListScreen
  └─> DownloadViewModel (download list + progress)
```

---

### External Dependencies

**Gradle Dependencies**:
```kotlin
// Already included (no new dependencies required)
implementation("androidx.activity:activity-compose:1.8.2")
implementation("androidx.compose.material3:material3:1.2.0")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```

**Android APIs**:
- `android.app.DownloadManager` (API 9+)
- `android.content.Intent.ACTION_OPEN_DOCUMENT_TREE` (API 21+)
- `android.provider.DocumentsContract` (API 19+)
- `android.os.StatFs` (API 1+)
- `androidx.core.app.ActivityCompat` (permissions)

---

## Out of Scope

### Future Enhancements (Not in V1)

1. **Pause/Resume Downloads**: Requires DownloadManager session management
2. **Download Queue Priority**: User reorders downloads
3. **Bandwidth Throttling**: Limit download speed
4. **Auto-Resume on WiFi**: Pause on cellular, resume on WiFi
5. **Scan with Play Protect**: Malware scanning
6. **Download History Search**: Full-text search of filenames
7. **Batch Download**: Select multiple files
8. **Cloud Integration**: Download to Google Drive/OneDrive
9. **Smart Folder Categorization**: Auto-sort by file type
10. **Download Notifications Settings**: Granular notification controls

### Known Limitations

1. **Storage Access Framework Limitations**:
   - Cannot pre-select folders outside app directories on some OEMs
   - Path display may show `content://` URI instead of friendly path
   - Some cloud storage providers return non-standard URIs

2. **DownloadManager Limitations**:
   - Progress polling required (no push notifications)
   - Cannot download to arbitrary URIs on API < 29
   - Notification cannot be fully customized

3. **Platform Limitations**:
   - Android TV: No touch picker UI (voice input required)
   - AR Glasses: Limited file picker support (use voice commands)
   - Web: Browser's native download UI (no custom implementation)

---

## Acceptance Testing

### Manual Test Script

**Test Case 1: File Picker Integration**
1. Open Settings → Downloads
2. Tap "Download Location"
3. Verify native file picker opens
4. Navigate to Documents folder
5. Select folder
6. Verify path displays in settings: "Documents"
7. Close and reopen app
8. Verify path still shows "Documents"
9. **Expected**: Path persists, picker works smoothly

---

**Test Case 2: Permission Handling (API 28 Device)**
1. Uninstall app (clear permissions)
2. Install app
3. Go to Settings → Downloads
4. Tap "Download Location"
5. Verify permission rationale dialog shown
6. Tap "Allow"
7. Verify system permission dialog shown
8. Tap "Allow"
9. Verify file picker opens
10. **Expected**: Permission flow smooth, no crashes

---

**Test Case 3: Custom Path Validation**
1. Connect USB drive or SD card
2. Select SD card as download location
3. Start a download
4. While downloading, eject SD card
5. Verify error shown: "Download failed: storage not available"
6. Reconnect SD card
7. Tap "Retry"
8. Verify download resumes
9. **Expected**: Graceful handling of removed storage

---

**Test Case 4: Download Progress Monitoring**
1. Open WebAvanue
2. Navigate to https://speed.hetzner.de/ (test files)
3. Download 100MB test file
4. Switch to Downloads screen
5. Verify progress bar updates smoothly
6. Verify speed shown (e.g., "5.2 MB/s")
7. Verify ETA shown (e.g., "15 seconds remaining")
8. Wait for completion
9. Verify "Complete" status appears within 1 second
10. **Expected**: Real-time updates, accurate calculations

---

**Test Case 5: Ask Download Location**
1. Enable "Ask Download Location" in settings
2. Navigate to any file download link
3. Tap download
4. Verify "Save Download" dialog appears
5. Tap "Change" to select different folder
6. Select "Documents"
7. Check "Always use this location"
8. Tap "Download"
9. Initiate another download
10. Verify no dialog shown (uses saved path)
11. **Expected**: Dialog works, remember choice persists

---

**Test Case 6: WiFi-Only Downloads**
1. Enable "Download Over WiFi Only"
2. Disable WiFi (use cellular)
3. Attempt download
4. Verify error shown: "WiFi required for downloads"
5. Tap "Change Settings" → disable WiFi-only
6. Retry download
7. Verify download proceeds on cellular
8. **Expected**: Cellular blocked when enabled, allowed when disabled

---

## Success Metrics

### Quantitative Metrics

| Metric | Target | Measurement Method |
|--------|--------|-------------------|
| Feature adoption | > 30% of users | Analytics: % enabling `askDownloadLocation` |
| Custom path usage | > 15% of users | Analytics: % with non-null `downloadPath` |
| Permission grant rate | > 85% | Analytics: granted / (granted + denied) |
| Error rate | < 5% of downloads | Analytics: failed downloads / total |
| Progress update lag | < 1 second | Profiling: time between actual and displayed progress |
| Test coverage | > 90% | JaCoCo report |

---

### Qualitative Metrics

1. **User Feedback**:
   - Survey: "Downloading files is easy" → 4.5+ / 5.0 average
   - Support tickets: < 5 tickets/month related to downloads

2. **Code Quality**:
   - No `TODO` or `FIXME` comments in production code
   - All public APIs documented with KDoc
   - Zero high-severity SonarQube issues

3. **Accessibility**:
   - TalkBack users can complete download flow
   - All actions achievable without vision

---

## Rollout Plan

### Phased Rollout

| Phase | Audience | Duration | Success Criteria |
|-------|----------|----------|------------------|
| Alpha | Internal testers (10 users) | 1 week | No critical bugs, positive feedback |
| Beta | Early adopters (100 users) | 2 weeks | < 1% crash rate, error rate < 5% |
| Staged | 10% → 50% → 100% users | 3 weeks | Monitor metrics, rollback if errors spike |
| Full | All users | - | Stable for 1 week |

### Rollback Triggers

**Immediate Rollback If**:
- Crash rate > 2% (compared to < 0.5% baseline)
- Download failure rate > 10%
- Permission denial causes app to become unusable
- Data loss reported (downloads deleted/corrupted)

### Monitoring

**Key Metrics to Watch**:
- Firebase Crashlytics: Crash-free rate
- Custom analytics: Download success/failure rates
- Performance: Frame drops during progress updates
- Network: Download bandwidth usage

---

## Documentation Updates

### User-Facing Documentation

**Update**: `Docs/WebAvanue/UserGuide.md`

**New Sections**:
1. "Downloading Files"
   - How to download files from web pages
   - Changing download location
   - Managing downloads
   - Troubleshooting (permission denied, insufficient space)

2. "Download Settings"
   - Ask Download Location explanation
   - WiFi-only downloads
   - Custom download paths

---

### Developer Documentation

**Update**: `Modules/WebAvanue/README.md`

**New Sections**:
1. "Download Architecture"
   - Component diagram
   - Flow diagrams (file picker, permissions, progress)
   - API documentation links

2. "Testing Downloads"
   - Running download tests
   - Mocking DownloadManager
   - Testing on different API levels

---

## Risks & Mitigations

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| SAF picker crashes on some devices | High | Low | Test on top 10 devices, add try-catch + fallback to text entry |
| Permission denial breaks downloads | High | Medium | Always fallback to default Downloads folder |
| Progress polling drains battery | Medium | Medium | Use 500ms interval (not 100ms), stop when app backgrounded |
| File path changes after selection | Medium | Low | Revalidate path on app startup, auto-revert if invalid |
| DownloadManager API changes | Low | Very Low | Wrap in abstraction layer for future replacement |

---

## Appendix

### A. Related Documents

- `PHASE1_COMPLETE.md` - WebAvanue Phase 1 completion summary
- `ENCRYPTION.md` - Download security and encryption
- `SECURITY_IMPLEMENTATION.md` - Security threat model
- `WebAvanue-Plan-Phase4-Integration-51212-V1.md` - Phase 4 implementation plan

---

### B. Android Storage Access Framework (SAF) Reference

**Key Intents**:
- `ACTION_OPEN_DOCUMENT_TREE`: Pick directory (API 21+)
- `ACTION_OPEN_DOCUMENT`: Pick single file (API 19+)
- `ACTION_CREATE_DOCUMENT`: Save file with picker (API 19+)

**Persistent Permissions**:
```kotlin
// Grant persistent access
context.contentResolver.takePersistableUriPermission(
    uri,
    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
)

// Check if permission still valid
val persistedUris = context.contentResolver.persistedUriPermissions
val hasPermission = persistedUris.any { it.uri == selectedUri }
```

**Content URI Handling**:
```kotlin
// Convert content:// URI to file path (display only)
fun getDisplayPath(uri: Uri): String {
    val docUri = DocumentsContract.buildDocumentUriUsingTree(
        uri,
        DocumentsContract.getTreeDocumentId(uri)
    )
    return DocumentFile.fromTreeUri(context, docUri)?.name ?: uri.toString()
}
```

---

### C. DownloadManager API Reference

**Enqueue Download**:
```kotlin
val request = DownloadManager.Request(Uri.parse(url)).apply {
    setTitle(filename)
    setDescription("Downloading...")
    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
    setNotificationVisibility(VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
    setAllowedOverMetered(true) // Allow cellular
    setAllowedOverRoaming(false)
}

val downloadId = downloadManager.enqueue(request)
```

**Query Progress**:
```kotlin
val query = DownloadManager.Query().setFilterById(downloadId)
val cursor = downloadManager.query(query)
cursor.use {
    if (it.moveToFirst()) {
        val bytesDownloaded = it.getLong(it.getColumnIndex(COLUMN_BYTES_DOWNLOADED_SO_FAR))
        val bytesTotal = it.getLong(it.getColumnIndex(COLUMN_TOTAL_SIZE_BYTES))
        val status = it.getInt(it.getColumnIndex(COLUMN_STATUS))
        // ...
    }
}
```

**Listen for Completion**:
```kotlin
class DownloadCompletionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
        if (downloadId != -1L) {
            // Download completed, update UI
        }
    }
}
```

---

### D. Glossary

| Term | Definition |
|------|------------|
| **SAF** | Storage Access Framework - Android's file picker API (API 19+) |
| **URI** | Uniform Resource Identifier - `content://` or `file://` path |
| **Scoped Storage** | Android 10+ storage isolation (app-specific + MediaStore) |
| **Persistent Permission** | Long-term URI access grant (survives app restart) |
| **DownloadManager** | Android system service for background downloads |
| **Document Provider** | App that exposes files via SAF (e.g., Google Drive) |

---

## Conclusion

This specification addresses the **critical gaps** in WebAvanue's download management system, completing the remaining ~10% of functionality required for production readiness. By implementing these four features—file picker integration, permission handling, progress monitoring, and settings completion—we deliver a **polished, production-grade download experience** that meets user expectations and Android best practices.

**Key Deliverables**:
- ✅ Native file picker (no more manual text entry)
- ✅ Automatic permission requests (graceful degradation)
- ✅ Real-time progress updates (smooth 500ms polling)
- ✅ Fully functional settings (all toggles have effect)

**Success Metrics**:
- 90%+ test coverage
- < 5% download error rate
- 85%+ permission grant rate
- Positive user feedback (4.5+/5.0)

**Timeline**: 7-8 days for full implementation, testing, and polish.

---

**Document Version**: 1.0
**Last Updated**: 2025-12-12
**Next Review**: After Phase 5 implementation completion
