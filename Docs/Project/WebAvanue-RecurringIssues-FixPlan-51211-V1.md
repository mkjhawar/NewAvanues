# WebAvanue Recurring Issues - Root Cause Analysis & Fix Plan

**Version:** 1.0
**Date:** 2025-12-11
**Status:** Active
**Priority:** Critical

---

## Executive Summary

This document analyzes 7 recurring UI/UX issues in WebAvanue and provides a comprehensive fix plan with root cause resolution. These issues have persisted across multiple fix attempts, indicating systemic problems in architecture and implementation patterns.

**Key Finding:** Issues stem from **inconsistent UI framework usage** and **incomplete state management wiring**, not individual bugs.

---

## Table of Contents

1. [Root Cause Analysis](#root-cause-analysis)
2. [Current Architecture](#current-architecture)
3. [Issue Breakdown](#issue-breakdown)
4. [Comprehensive Fix Plan](#comprehensive-fix-plan)
5. [Future Architecture](#future-architecture)
6. [Migration Strategy](#migration-strategy)

---

## Root Cause Analysis

### Why These Issues Keep Recurring

| Root Cause | Manifestation | Impact |
|------------|---------------|--------|
| **Inconsistent UI Patterns** | Mix of direct Material3 + Ocean components | Code difficult to refactor uniformly |
| **State Management Gaps** | ViewModels exist but not fully wired to UI | Settings changes don't propagate |
| **No Centralized Component Library** | Each screen implements layouts differently | Grid layouts inconsistent |
| **Platform-Specific Code in Universal Module** | Android-specific WebView code in `commonMain` | Desktop/iOS implementations incomplete |
| **Settings Not Applied** | SettingsApplicator exists but not called on changes | User changes ignored |

### Chain of Thought (CoT) Analysis

```
Issue: Address bar doesn't update URL
↓
Symptom: Shows google.com when on apple.com
↓
Investigation: Focus state prevents URL update
↓
Root Cause: State flow doesn't reset focus when tab changes
↓
Pattern: State management not handling edge cases
↓
Systemic Issue: Need comprehensive state machine for address bar
```

```
Issue: Bookmark folder not saving
↓
Symptom: Folder selection resets to "No folder"
↓
Investigation: Dialog shows correct folder, then resets
↓
Root Cause: Folder ID passed but not persisted to database
↓
Pattern: ViewModel method incomplete (marked with FIX comment)
↓
Systemic Issue: Async operations need proper coroutine handling
```

```
Issue: Settings not working
↓
Symptom: 70+ settings exist, most do nothing
↓
Investigation: SettingsViewModel saves, but WebView ignores
↓
Root Cause: SettingsApplicator not called on setting changes
↓
Pattern: Repository → ViewModel → UI complete, but UI → WebView missing
↓
Systemic Issue: Need reactive settings application pipeline
```

---

## Current Architecture

### Component Structure

```
WebAvanue
├── universal/
│   ├── commonMain/            # KMP shared code
│   │   ├── domain/            # Business logic (KMP)
│   │   ├── data/              # Repositories (KMP)
│   │   ├── viewmodel/         # ViewModels (KMP)
│   │   ├── presentation/
│   │   │   ├── design/        # Ocean Design System
│   │   │   │   ├── ComponentProvider.kt     # Interface
│   │   │   │   ├── OceanComponents.kt       # Material3 impl
│   │   │   │   └── OceanDesignTokens.kt     # Design tokens
│   │   │   └── ui/            # UI Components (Compose)
│   │   │       ├── browser/   # Browser screens
│   │   │       ├── bookmark/  # Bookmark screens
│   │   │       ├── settings/  # Settings screens
│   │   │       └── components/ # Shared components
│   ├── androidMain/           # Android platform
│   │   └── platform/
│   │       ├── SettingsApplicator.kt  # WebView settings
│   │       └── webview/
│   │           └── WebViewLifecycle.kt
│   ├── iosMain/               # iOS platform
│   └── desktopMain/           # Desktop platform
└── coredata/                  # SQLDelight database
```

### Current UI Framework: Ocean Design System

**Status:** Partially implemented in WebAvanue only
**Technology:** Jetpack Compose + Material3
**Location:** `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/design/`

**Components:**
- `ComponentProvider` - Interface for UI components
- `OceanComponents` - Material3 implementation
- `OceanDesignTokens` - Design tokens (colors, spacing, typography)

**Problem:** Only WebAvanue has this. Other apps (VoiceOS, AVA, Avanues) use inconsistent patterns.

---

## Issue Breakdown

### Issue 1: Voice Commands Dialog - Grid Layout

**Reported Issue:**
- Dialog displays items in list format, requires scrolling
- All sub-views (Navigation, Scrolling, Tabs, Zoom, Modes, Features) need grid layout
- Must fit on screen without scrolling (portrait + landscape)

**File:** `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/voice/VoiceCommandsDialog.kt:1`

**Root Cause:**
```kotlin
// Current: Uses LazyColumn (list)
LazyColumn { items(commands) { CommandItemCard(...) } }

// Needed: LazyVerticalGrid (grid)
LazyVerticalGrid(columns = GridCells.Adaptive(minSize = 150.dp)) {
    items(commands) { CommandItemCard(...) }
}
```

**Fix Complexity:** Low
**Estimated Lines Changed:** ~30 lines
**Test Impact:** Visual regression test needed

---

### Issue 2: Portrait Mode - Bottom Command Bar Scrolling

**Reported Issue:**
- Bottom command bar scrolls horizontally
- Not center-aligned
- Reload button (Page Commands) and Back button (Home) should be removed

**Files:**
- `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/ui/browser/BottomCommandBar.kt:1`
- `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/ui/browser/BrowserScreen.kt:1` (calls BottomCommandBar)

**Root Cause:**
```kotlin
// Current: Row allows scrolling, no justification
Row(modifier = Modifier.horizontalScroll(...)) {
    buttons.forEach { ... }
}

// Needed: Row with center alignment, no scroll, max 6 buttons enforced
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceEvenly  // Center-aligned
) {
    buttons.take(6).forEach { ... }  // Enforce max 6
}
```

**Fix Complexity:** Low
**Estimated Lines Changed:** ~20 lines
**Test Impact:** Layout test for portrait orientation

---

### Issue 3: Landscape Mode - Bottom Command Bar Shadow

**Reported Issue:**
- Unwanted background + white bold line appears behind command bar
- Looks like incorrect shadow implementation
- Visual inconsistency

**File:** `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/ui/browser/BottomCommandBar.kt:1`

**Root Cause:**
```kotlin
// Current: Elevation creates shadow on transparent background
Surface(
    modifier = Modifier.glass(...),  // Transparent glass effect
    tonalElevation = 8.dp,           // Creates white shadow artifact
    shadowElevation = 12.dp
)

// Needed: Remove elevation, use border instead
Surface(
    modifier = Modifier
        .glass(...)
        .border(1.dp, Color.White.copy(alpha = 0.2f)),  // Subtle border
    tonalElevation = 0.dp,   // No tonal elevation
    shadowElevation = 0.dp   // No shadow
)
```

**Fix Complexity:** Low
**Estimated Lines Changed:** ~10 lines
**Test Impact:** Visual regression test landscape mode

---

### Issue 4: Address Bar - Incorrect Link Display

**Reported Issue:**
- After adding new tab and navigating to apple.com, address bar still shows google.com
- Only occurs when address bar is focused before adding new tab
- Focus state not reset, prevents URL update

**File:** `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/ui/browser/AddressBar.kt:1`

**Root Cause:**
```kotlin
// Current: URL state doesn't react to tab changes when focused
var url by remember { mutableStateOf(currentTab.url) }
val focusRequester = remember { FocusRequester() }

TextField(
    value = url,
    onValueChange = { url = it },
    modifier = Modifier.focusRequester(focusRequester)
)

// Problem: When focused, url state is stale
// LaunchedEffect observes currentTab.url but doesn't override focused state

// Needed: Clear focus and update URL on tab change
LaunchedEffect(currentTab.id) {  // Trigger on tab ID change
    focusRequester.freeFocus()   // Clear focus
    url = currentTab.url         // Update URL
}
```

**Fix Complexity:** Medium
**Estimated Lines Changed:** ~15 lines
**Test Impact:** Instrumented test for tab switching with focused address bar

---

### Issue 5: Bookmarks - Folder Not Saving

**Reported Issue:**
- Change folder from "No folder" → "Create new folder"
- Popup appears, folder name entered, shown correctly in Edit dialog
- Click Save → folder selection NOT saved
- Reopen Edit → shows "No folder"

**Files:**
- `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/ui/bookmark/AddBookmarkDialog.kt:1`
- `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/viewmodel/FavoriteViewModel.kt:1`

**Root Cause:**
```kotlin
// Current: FavoriteViewModel.kt has incomplete implementation
fun updateFavorite(id: Long, url: String, title: String, folderId: Long?) {
    viewModelScope.launch {
        repository.updateFavorite(
            id = id,
            url = url,
            title = title
            // Missing: folderId parameter!
        )
    }
}

// Needed: Pass folderId to repository
fun updateFavorite(id: Long, url: String, title: String, folderId: Long?) {
    viewModelScope.launch {
        repository.updateFavorite(
            id = id,
            url = url,
            title = title,
            folderId = folderId  // Add this parameter
        )
    }
}
```

**Fix Complexity:** Low
**Estimated Lines Changed:** ~5 lines
**Test Impact:** Unit test for FavoriteViewModel, integration test for folder persistence

---

### Issue 6: File Downloads - Stuck in Pending

**Reported Issue:**
- File downloads don't start
- Items stuck in "Pending" state in Downloads screen
- Never progress to downloading

**Files:**
- `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/ui/download/DownloadListScreen.kt:1`
- `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/viewmodel/DownloadViewModel.kt:1`
- `Modules/WebAvanue/coredata/src/commonMain/kotlin/com/augmentalis/webavanue/data/repository/BrowserRepositoryImpl.kt:1`

**Root Cause:**
```kotlin
// Current: DownloadViewModel has stub implementation
class DownloadViewModel(private val repository: BrowserRepository) : ViewModel() {
    val downloads = repository.getAllDownloads()  // Reads from DB

    fun startDownload(url: String, filename: String) {
        // TODO: Implement actual download logic
        // Currently just adds to DB with status = PENDING
        viewModelScope.launch {
            repository.addDownload(url, filename, status = DownloadStatus.PENDING)
        }
    }
}

// Needed: Integrate platform download manager
class DownloadViewModel(
    private val repository: BrowserRepository,
    private val downloadManager: PlatformDownloadManager  // New dependency
) : ViewModel() {
    fun startDownload(url: String, filename: String) {
        viewModelScope.launch {
            val downloadId = repository.addDownload(url, filename, DownloadStatus.PENDING)

            // Start actual download via platform manager
            downloadManager.download(
                url = url,
                destination = filename,
                onProgress = { progress ->
                    repository.updateDownloadProgress(downloadId, progress)
                },
                onComplete = {
                    repository.updateDownloadStatus(downloadId, DownloadStatus.COMPLETED)
                },
                onError = { error ->
                    repository.updateDownloadStatus(downloadId, DownloadStatus.FAILED)
                }
            )
        }
    }
}
```

**Fix Complexity:** High
**Estimated Lines Changed:** ~200 lines (new PlatformDownloadManager interface + implementations)
**Test Impact:** Platform-specific tests for Android/iOS/Desktop download managers

---

### Issue 7: Settings Not Applied to Browser

**Reported Issue:**
- 70+ settings exist in SettingsScreen
- Most settings save to database but don't affect browser behavior
- Examples:
  - JavaScript enable/disable: Always enabled (hardcoded)
  - Cookies enable/disable: Always disabled
  - Pop-ups block: Always blocked regardless of setting
  - Desktop mode: Partially working (only on new tabs)
  - Search suggestions: Not integrated
  - New tab page: Always opens google.com

**Files:**
- `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/ui/settings/SettingsScreen.kt:1`
- `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/viewmodel/SettingsViewModel.kt:1`
- `Modules/WebAvanue/universal/src/androidMain/kotlin/com/augmentalis/Avanues/web/universal/platform/SettingsApplicator.kt:1`
- `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/ui/browser/BrowserScreen.kt:1`

**Root Cause:**
```kotlin
// Current: Settings flow stops at repository
SettingsScreen → SettingsViewModel → Repository (saves to DB)
                                          ↓
                                     (STOPS HERE)

// SettingsApplicator exists but is NEVER CALLED

// Needed: Complete the flow
SettingsScreen → SettingsViewModel → Repository → Settings StateFlow
                                                          ↓
                                                   BrowserScreen observes
                                                          ↓
                                                   Calls SettingsApplicator
                                                          ↓
                                                   Updates WebView

// In BrowserScreen.kt, need to add:
val settings by settingsViewModel.settings.collectAsState()

LaunchedEffect(settings) {
    // Apply settings to WebView whenever they change
    webView?.let { view ->
        SettingsApplicator.applySettings(view, settings)
    }
}
```

**Fix Complexity:** High
**Estimated Lines Changed:** ~100 lines
**Test Impact:** Comprehensive integration tests for all 70 settings

---

## Comprehensive Fix Plan

### Phase 1: Quick Wins (Days 1-2)

**Goal:** Fix low-complexity issues to provide immediate user value

| Issue | Priority | Complexity | Files | LOC |
|-------|----------|------------|-------|-----|
| 1. Voice Commands Grid | P1 | Low | VoiceCommandsDialog.kt | ~30 |
| 2. Command Bar Portrait | P1 | Low | BottomCommandBar.kt | ~20 |
| 3. Command Bar Landscape Shadow | P2 | Low | BottomCommandBar.kt | ~10 |
| 5. Bookmark Folder Save | P1 | Low | FavoriteViewModel.kt | ~5 |

**Total:** ~65 lines of code
**Estimated Time:** 4-6 hours
**Testing:** ~2 hours

#### Implementation Steps

**Step 1.1: Fix Voice Commands Grid Layout**

```kotlin
// File: VoiceCommandsDialog.kt
// Line: ~150 (CommandsView composable)

// BEFORE:
LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(OceanDesignTokens.Spacing.md)
) {
    items(commands) { command ->
        CommandItemCard(command, onClick = { onCommandSelected(command) })
    }
}

// AFTER:
LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 160.dp),  // Auto-fit columns
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(OceanDesignTokens.Spacing.md),
    horizontalArrangement = Arrangement.spacedBy(OceanDesignTokens.Spacing.sm),
    verticalArrangement = Arrangement.spacedBy(OceanDesignTokens.Spacing.sm)
) {
    items(commands) { command ->
        CommandItemCard(
            command = command,
            onClick = { onCommandSelected(command) },
            modifier = Modifier.aspectRatio(1.2f)  // Consistent card size
        )
    }
}
```

**Step 1.2: Fix Bottom Command Bar Portrait**

```kotlin
// File: BottomCommandBar.kt
// Line: ~200 (HorizontalCommandBarLayout)

// BEFORE:
Row(
    modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),  // REMOVE scrolling
    horizontalArrangement = Arrangement.Start
) {
    buttons.forEach { button ->  // May exceed 6 buttons
        CommandButton(button)
    }
}

// AFTER:
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceEvenly  // Center-aligned
) {
    buttons.take(6).forEach { button ->  // Max 6 buttons enforced
        CommandButton(
            button = button,
            modifier = Modifier.weight(1f)  // Equal width
        )
    }
}

// Also remove Reload from PAGE_COMMANDS and Back from MAIN level
```

**Step 1.3: Fix Landscape Shadow**

```kotlin
// File: BottomCommandBar.kt
// Line: ~150 (VerticalCommandBarLayout surface)

// BEFORE:
Surface(
    modifier = glassModifier,
    shape = RoundedCornerShape(OceanDesignTokens.CornerRadius.lg),
    color = Color.Transparent,
    tonalElevation = 8.dp,  // REMOVE - causes white shadow
    shadowElevation = 12.dp  // REMOVE
) {
    content()
}

// AFTER:
Surface(
    modifier = glassModifier.border(
        width = 1.dp,
        color = Color.White.copy(alpha = 0.15f),
        shape = RoundedCornerShape(OceanDesignTokens.CornerRadius.lg)
    ),
    shape = RoundedCornerShape(OceanDesignTokens.CornerRadius.lg),
    color = Color.Transparent,
    tonalElevation = 0.dp,  // No tonal elevation
    shadowElevation = 0.dp   // No shadow (glass effect is enough)
) {
    content()
}
```

**Step 1.4: Fix Bookmark Folder Save**

```kotlin
// File: FavoriteViewModel.kt
// Line: ~80 (updateFavorite function)

// BEFORE:
fun updateFavorite(id: Long, url: String, title: String, folderId: Long?) {
    viewModelScope.launch {
        repository.updateFavorite(
            id = id,
            url = url,
            title = title
            // Missing folderId!
        )
        loadFavorites()  // Refresh list
    }
}

// AFTER:
fun updateFavorite(id: Long, url: String, title: String, folderId: Long?) {
    viewModelScope.launch {
        repository.updateFavorite(
            id = id,
            url = url,
            title = title,
            folderId = folderId  // Pass folderId to repository
        )
        loadFavorites()  // Refresh list
    }
}

// Also verify repository method signature includes folderId parameter
```

---

### Phase 2: Medium Complexity (Days 3-4)

**Goal:** Fix state management issues

| Issue | Priority | Complexity | Files | LOC |
|-------|----------|------------|-------|-----|
| 4. Address Bar URL Update | P1 | Medium | AddressBar.kt | ~15 |

**Estimated Time:** 4-6 hours
**Testing:** ~3 hours

#### Implementation Steps

**Step 2.1: Fix Address Bar Focus State**

```kotlin
// File: AddressBar.kt
// Line: ~100 (AddressBar composable)

@Composable
fun AddressBar(
    currentTab: Tab,
    onNavigate: (String) -> Unit,
    ...
) {
    var url by remember { mutableStateOf(currentTab.url) }
    var isEditing by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // KEY FIX: Reset URL and focus when tab changes
    LaunchedEffect(currentTab.id) {
        if (!isEditing) {  // Only update if not actively editing
            url = currentTab.url
        }
        // Clear focus when tab changes
        focusManager.clearFocus()
    }

    // Update URL when tab URL changes (e.g., navigation)
    LaunchedEffect(currentTab.url) {
        if (!isEditing) {
            url = currentTab.url
        }
    }

    TextField(
        value = url,
        onValueChange = { newUrl ->
            url = newUrl
        },
        modifier = Modifier
            .weight(1f)
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                isEditing = focusState.isFocused
                if (!focusState.isFocused && url != currentTab.url) {
                    // User finished editing without navigating, reset
                    url = currentTab.url
                }
            },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Go
        ),
        keyboardActions = KeyboardActions(
            onGo = {
                onNavigate(url)
                focusManager.clearFocus()
                isEditing = false
            }
        ),
        ...
    )
}
```

---

### Phase 3: High Complexity (Days 5-10)

**Goal:** Implement missing platform integrations

| Issue | Priority | Complexity | Files | LOC |
|-------|----------|------------|-------|-----|
| 6. Download Manager | P2 | High | DownloadViewModel.kt, PlatformDownloadManager.kt (new), AndroidDownloadManager.kt (new), IOSDownloadManager.kt (new), DesktopDownloadManager.kt (new) | ~200 |
| 7. Settings Application | P0 | High | BrowserScreen.kt, SettingsApplicator.kt, DesktopSettingsApplicator.kt (new), IOSSettingsApplicator.kt (new) | ~100 |

**Estimated Time:** 12-16 hours
**Testing:** ~8 hours

#### Implementation Steps

**Step 3.1: Implement Download Manager**

```kotlin
// NEW FILE: Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/platform/PlatformDownloadManager.kt

interface PlatformDownloadManager {
    suspend fun download(
        url: String,
        destination: String,
        onProgress: (Int) -> Unit,
        onComplete: () -> Unit,
        onError: (Throwable) -> Unit
    ): Long  // Returns download ID

    suspend fun cancelDownload(downloadId: Long)

    suspend fun pauseDownload(downloadId: Long)

    suspend fun resumeDownload(downloadId: Long)
}

expect fun createDownloadManager(): PlatformDownloadManager
```

```kotlin
// NEW FILE: Modules/WebAvanue/universal/src/androidMain/kotlin/com/augmentalis/Avanues/web/universal/platform/AndroidDownloadManager.kt

actual fun createDownloadManager(): PlatformDownloadManager = AndroidDownloadManager()

class AndroidDownloadManager : PlatformDownloadManager {
    private val downloadManager by lazy {
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }

    override suspend fun download(
        url: String,
        destination: String,
        onProgress: (Int) -> Unit,
        onComplete: () -> Unit,
        onError: (Throwable) -> Unit
    ): Long {
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, destination)
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        }

        val downloadId = downloadManager.enqueue(request)

        // Monitor progress
        monitorDownload(downloadId, onProgress, onComplete, onError)

        return downloadId
    }

    private suspend fun monitorDownload(
        downloadId: Long,
        onProgress: (Int) -> Unit,
        onComplete: () -> Unit,
        onError: (Throwable) -> Unit
    ) = withContext(Dispatchers.IO) {
        val query = DownloadManager.Query().setFilterById(downloadId)

        while (true) {
            val cursor = downloadManager.query(query)
            if (cursor.moveToFirst()) {
                val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                val bytesDownloaded = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val bytesTotal = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        cursor.close()
                        withContext(Dispatchers.Main) { onComplete() }
                        break
                    }
                    DownloadManager.STATUS_FAILED -> {
                        cursor.close()
                        withContext(Dispatchers.Main) {
                            onError(Exception("Download failed"))
                        }
                        break
                    }
                    DownloadManager.STATUS_RUNNING -> {
                        val progress = ((bytesDownloaded * 100) / bytesTotal).toInt()
                        withContext(Dispatchers.Main) { onProgress(progress) }
                    }
                }
            }
            cursor.close()
            delay(500)  // Poll every 500ms
        }
    }

    override suspend fun cancelDownload(downloadId: Long) {
        downloadManager.remove(downloadId)
    }

    override suspend fun pauseDownload(downloadId: Long) {
        // Android DownloadManager doesn't support pause/resume
        // Would need custom implementation
    }

    override suspend fun resumeDownload(downloadId: Long) {
        // Not supported by Android DownloadManager
    }
}
```

```kotlin
// UPDATE: Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/viewmodel/DownloadViewModel.kt

class DownloadViewModel(
    private val repository: BrowserRepository,
    private val downloadManager: PlatformDownloadManager = createDownloadManager()
) : ViewModel() {

    val downloads: StateFlow<List<Download>> = repository.getAllDownloads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun startDownload(url: String, filename: String) {
        viewModelScope.launch {
            try {
                // Create pending download in DB
                val downloadId = repository.addDownload(
                    url = url,
                    filename = filename,
                    status = DownloadStatus.PENDING,
                    progress = 0
                )

                // Start actual download
                val platformDownloadId = downloadManager.download(
                    url = url,
                    destination = filename,
                    onProgress = { progress ->
                        viewModelScope.launch {
                            repository.updateDownloadProgress(downloadId, progress)
                        }
                    },
                    onComplete = {
                        viewModelScope.launch {
                            repository.updateDownloadStatus(downloadId, DownloadStatus.COMPLETED)
                        }
                    },
                    onError = { error ->
                        viewModelScope.launch {
                            repository.updateDownloadStatus(downloadId, DownloadStatus.FAILED)
                        }
                    }
                )

                // Store platform download ID for cancellation
                repository.updatePlatformDownloadId(downloadId, platformDownloadId)

            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun cancelDownload(downloadId: Long) {
        viewModelScope.launch {
            val download = repository.getDownload(downloadId)
            download?.platformDownloadId?.let { platformId ->
                downloadManager.cancelDownload(platformId)
                repository.updateDownloadStatus(downloadId, DownloadStatus.CANCELLED)
            }
        }
    }
}
```

**Step 3.2: Wire Settings to WebView**

```kotlin
// UPDATE: Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/ui/browser/BrowserScreen.kt

@Composable
fun BrowserScreen(
    tabViewModel: TabViewModel,
    settingsViewModel: SettingsViewModel,
    ...
) {
    val currentTab by tabViewModel.currentTab.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()

    // Store WebView reference
    var webView: Any? by remember { mutableStateOf(null) }

    // KEY FIX: Apply settings whenever they change
    LaunchedEffect(settings, webView) {
        webView?.let { view ->
            when (view) {
                is android.webkit.WebView -> {
                    SettingsApplicator.applySettings(view, settings)
                }
                is WKWebView -> {
                    IOSSettingsApplicator.applySettings(view, settings)
                }
                is JavaFXWebView -> {
                    DesktopSettingsApplicator.applySettings(view, settings)
                }
            }
        }
    }

    // Apply settings when WebView is created
    AndroidView(
        factory = { context ->
            val view = WebView(context)
            webView = view  // Store reference

            // Apply initial settings
            SettingsApplicator.applySettings(view, settings)

            view
        },
        update = { view ->
            // Settings already applied via LaunchedEffect
        }
    )
}
```

```kotlin
// UPDATE: Modules/WebAvanue/universal/src/androidMain/kotlin/com/augmentalis/Avanues/web/universal/platform/SettingsApplicator.kt

object SettingsApplicator {

    fun applySettings(webView: WebView, settings: BrowserSettings) {
        // Apply all settings categories
        applyPrivacySettings(webView, settings)
        applyDisplaySettings(webView, settings)
        applyPerformanceSettings(webView, settings)
        applyAdvancedSettings(webView, settings)
    }

    private fun applyPrivacySettings(webView: WebView, settings: BrowserSettings) {
        webView.settings.apply {
            // JavaScript
            javaScriptEnabled = settings.enableJavaScript

            // Cookies
            if (settings.enableCookies) {
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
            } else {
                CookieManager.getInstance().setAcceptCookie(false)
                CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false)
            }

            // Pop-ups
            javaScriptCanOpenWindowsAutomatically = !settings.blockPopups
            setSupportMultipleWindows(!settings.blockPopups)

            // Mixed content
            mixedContentMode = if (settings.blockMixedContent) {
                WebSettings.MIXED_CONTENT_NEVER_ALLOW
            } else {
                WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            // Do Not Track
            // Note: DNT header must be set via WebViewClient
        }
    }

    private fun applyDisplaySettings(webView: WebView, settings: BrowserSettings) {
        webView.settings.apply {
            // Font size
            textZoom = when (settings.fontSize) {
                FontSize.TINY -> 75
                FontSize.SMALL -> 90
                FontSize.MEDIUM -> 100
                FontSize.LARGE -> 115
                FontSize.HUGE -> 130
            }

            // Images
            loadsImagesAutomatically = settings.showImages
            blockNetworkImage = !settings.showImages

            // Zoom
            builtInZoomControls = settings.forceZoom
            displayZoomControls = false  // Hide zoom controls UI
            setSupportZoom(settings.forceZoom)

            // Desktop mode
            userAgentString = if (settings.useDesktopMode) {
                DESKTOP_USER_AGENT  // Constant defined in companion object
            } else {
                null  // Use default mobile UA
            }

            // Desktop mode zoom and viewport
            if (settings.useDesktopMode) {
                useWideViewPort = true
                loadWithOverviewMode = settings.desktopModeAutoFitZoom
                initialScale = settings.desktopModeDefaultZoom
                // Viewport size would need JavaScript injection
            }
        }
    }

    private fun applyPerformanceSettings(webView: WebView, settings: BrowserSettings) {
        webView.settings.apply {
            // Hardware acceleration (set at WebView level, not settings)
            webView.setLayerType(
                if (settings.hardwareAcceleration) View.LAYER_TYPE_HARDWARE
                else View.LAYER_TYPE_SOFTWARE,
                null
            )

            // Cache mode
            cacheMode = if (settings.dataSaver) {
                WebSettings.LOAD_CACHE_ELSE_NETWORK
            } else {
                WebSettings.LOAD_DEFAULT
            }

            // DOM storage
            domStorageEnabled = true
            databaseEnabled = true

            // Text reflow
            layoutAlgorithm = if (settings.textReflow) {
                WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
            } else {
                WebSettings.LayoutAlgorithm.NORMAL
            }
        }
    }

    private fun applyAdvancedSettings(webView: WebView, settings: BrowserSettings) {
        webView.settings.apply {
            // Safe browsing
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                safeBrowsingEnabled = settings.blockTrackers
            }

            // Geolocation
            setGeolocationEnabled(settings.enableGeolocation)

            // File access
            allowFileAccess = settings.allowFileAccess
            allowContentAccess = true

            // Media autoplay
            mediaPlaybackRequiresUserGesture = when (settings.autoPlay) {
                AutoPlay.ALWAYS -> false
                AutoPlay.WIFI_ONLY, AutoPlay.NEVER, AutoPlay.ASK -> true
            }
        }
    }

    companion object {
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/121.0.0.0 Safari/537.36"
    }
}
```

---

### Testing Strategy

**Phase 1 Tests (Quick Wins):**
```kotlin
// VoiceCommandsDialogTest.kt
@Test
fun voiceCommandsDialog_showsGridLayout() {
    composeTestRule.setContent {
        VoiceCommandsDialog(
            showDialog = true,
            onDismiss = {}
        )
    }

    // Verify grid layout (multiple items visible in row)
    composeTestRule.onNodeWithText("Navigation").assertIsDisplayed()
    composeTestRule.onNodeWithText("Scrolling").assertIsDisplayed()
    // Both should be visible without scrolling
}

// BottomCommandBarTest.kt
@Test
fun bottomCommandBar_portrait_centerAligned() {
    composeTestRule.setContent {
        BottomCommandBar(
            level = CommandBarLevel.MAIN,
            onBack = {},
            onHome = {},
            ...
        )
    }

    // Verify no horizontal scroll
    composeTestRule.onNode(hasScrollAction()).assertDoesNotExist()

    // Verify max 6 buttons
    composeTestRule.onAllNodesWithContentDescription("Command button")
        .assertCountEquals(6)
}
```

**Phase 2 Tests (Address Bar):**
```kotlin
// AddressBarTest.kt
@Test
fun addressBar_updatesURL_whenTabChanges() = runTest {
    val tab1 = Tab(id = 1, url = "https://google.com")
    val tab2 = Tab(id = 2, url = "https://apple.com")
    val currentTab = mutableStateOf(tab1)

    composeTestRule.setContent {
        AddressBar(
            currentTab = currentTab.value,
            onNavigate = {}
        )
    }

    // Initially shows google.com
    composeTestRule.onNodeWithText("https://google.com").assertIsDisplayed()

    // Focus address bar
    composeTestRule.onNodeWithContentDescription("Address bar").performClick()

    // Change tab while focused
    currentTab.value = tab2

    // Should update to apple.com (focus cleared)
    composeTestRule.onNodeWithText("https://apple.com").assertIsDisplayed()
}
```

**Phase 3 Tests (Downloads & Settings):**
```kotlin
// DownloadViewModelTest.kt
@Test
fun startDownload_createsDownload_andStartsPlatformManager() = runTest {
    val repository = FakeBrowserRepository()
    val downloadManager = FakePlatformDownloadManager()
    val viewModel = DownloadViewModel(repository, downloadManager)

    viewModel.startDownload("https://example.com/file.pdf", "file.pdf")

    // Verify download created in repository
    val downloads = repository.getAllDownloads().first()
    assertEquals(1, downloads.size)
    assertEquals(DownloadStatus.PENDING, downloads[0].status)

    // Verify platform download manager called
    assertEquals(1, downloadManager.downloads.size)
    assertEquals("https://example.com/file.pdf", downloadManager.downloads[0].url)
}

// SettingsApplicationTest.kt (Instrumented)
@Test
fun settingsChange_appliedToWebView() = runInstrumentedTest {
    val scenario = launchActivity<MainActivity>()

    // Open settings
    onView(withId(R.id.menu_button)).perform(click())
    onView(withText("Settings")).perform(click())

    // Disable JavaScript
    onView(withText("Privacy & Security")).perform(click())
    onView(withText("Enable JavaScript")).perform(click())

    // Navigate back to browser
    pressBack()
    pressBack()

    // Verify JavaScript disabled in WebView
    val webView = getWebViewFromActivity(scenario)
    assertFalse(webView.settings.javaScriptEnabled)
}
```

---

## Future Architecture

### Goal: Unified Design System Across All Apps

**Problem:** Currently each app implements UI differently:
- WebAvanue: Ocean Design System (partial)
- VoiceOS: Custom Compose
- AVA: Mix of Material3 + custom
- Avanues: Inconsistent patterns

**Solution:** Create shared UI component libraries in `Common/UI/` per platform

### Proposed Structure

```
Common/UI/
├── ComposeUI/                    # Android Compose components
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/           # KMP shared (design tokens)
│       │   └── kotlin/
│       │       └── design/
│       │           ├── DesignTokens.kt      # Colors, spacing, typography
│       │           └── ComponentInterface.kt # Component contracts
│       └── androidMain/          # Android Compose implementation
│           └── kotlin/
│               ├── ocean/        # Ocean Design System (Material3)
│               │   ├── OceanButton.kt
│               │   ├── OceanTextField.kt
│               │   ├── OceanCard.kt
│               │   └── OceanTheme.kt
│               └── magic/        # MagicUI/AvaCode (future)
│                   ├── MagicButton.kt
│                   └── MagicTheme.kt
├── SwiftUI/                      # iOS SwiftUI components
│   ├── Package.swift
│   └── Sources/
│       ├── Design/               # Design tokens in Swift
│       │   └── DesignTokens.swift
│       ├── Ocean/                # Ocean Design System (SwiftUI)
│       │   ├── OceanButton.swift
│       │   ├── OceanTextField.swift
│       │   └── OceanTheme.swift
│       └── Magic/                # MagicUI/AvaCode (future)
│           └── MagicButton.swift
├── DesktopUI/                    # Desktop JavaFX components
│   ├── build.gradle.kts
│   └── src/
│       └── main/kotlin/
│           ├── design/
│           │   └── DesignTokens.kt
│           └── ocean/
│               ├── OceanButton.kt
│               └── OceanTheme.kt
└── WebUI/                        # Web React components
    ├── package.json
    └── src/
        ├── design/
        │   └── tokens.ts
        └── ocean/
            ├── OceanButton.tsx
            └── OceanTheme.tsx
```

### Design Token Sharing (KMP)

```kotlin
// Common/UI/ComposeUI/src/commonMain/kotlin/design/DesignTokens.kt

package com.augmentalis.common.ui.design

/**
 * Platform-agnostic design tokens
 * Used by all UI frameworks (Compose, SwiftUI, JavaFX, React)
 */
object DesignTokens {

    object Colors {
        // Primary palette (hex strings for cross-platform)
        const val PRIMARY = "#3B82F6"          // Blue
        const val PRIMARY_DARK = "#2563EB"
        const val PRIMARY_LIGHT = "#60A5FA"

        // Surface colors
        const val SURFACE = "#1E293B"          // Dark slate
        const val SURFACE_ELEVATED = "#334155"
        const val SURFACE_INPUT = "#475569"

        // Text colors
        const val TEXT_PRIMARY = "#E2E8F0"
        const val TEXT_SECONDARY = "#CBD5E1"
        const val TEXT_DISABLED = "#64748B"

        // State colors
        const val SUCCESS = "#10B981"          // Green
        const val WARNING = "#F59E0B"          // Amber
        const val ERROR = "#EF4444"            // Red
    }

    object Spacing {
        const val XS = 4   // 4dp/pt
        const val SM = 8   // 8dp/pt
        const val MD = 12  // 12dp/pt
        const val LG = 16  // 16dp/pt
        const val XL = 24  // 24dp/pt
        const val XXL = 32 // 32dp/pt
        const val TOUCH_TARGET = 48  // Minimum touch target
    }

    object Typography {
        // Font sizes
        const val FONT_SIZE_XS = 12
        const val FONT_SIZE_SM = 14
        const val FONT_SIZE_MD = 16
        const val FONT_SIZE_LG = 18
        const val FONT_SIZE_XL = 24
        const val FONT_SIZE_XXL = 32

        // Font weights
        const val FONT_WEIGHT_REGULAR = 400
        const val FONT_WEIGHT_MEDIUM = 500
        const val FONT_WEIGHT_BOLD = 700
    }

    object CornerRadius {
        const val SM = 4
        const val MD = 8
        const val LG = 12
        const val XL = 16
        const val XXL = 24
        const val FULL = 9999
    }

    object Elevation {
        const val NONE = 0
        const val SM = 2
        const val MD = 4
        const val LG = 8
        const val XL = 12
        const val XXL = 16
    }
}
```

### Platform-Specific Implementations

**Android Compose:**
```kotlin
// Common/UI/ComposeUI/src/androidMain/kotlin/ocean/OceanButton.kt

@Composable
fun OceanButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    enabled: Boolean = true
) {
    val backgroundColor = when (variant) {
        ButtonVariant.Primary -> Color(parseColor(DesignTokens.Colors.PRIMARY))
        ButtonVariant.Secondary -> Color.Transparent
        ButtonVariant.Tertiary -> Color.Transparent
        ButtonVariant.Ghost -> Color.Transparent
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = Color(parseColor(DesignTokens.Colors.TEXT_PRIMARY))
        ),
        shape = RoundedCornerShape(DesignTokens.CornerRadius.LG.dp)
    ) {
        Text(text)
    }
}
```

**iOS SwiftUI:**
```swift
// Common/UI/SwiftUI/Sources/Ocean/OceanButton.swift

import SwiftUI

struct OceanButton: View {
    let text: String
    let onClick: () -> Void
    var variant: ButtonVariant = .primary
    var enabled: Bool = true

    var body: some View {
        Button(action: onClick) {
            Text(text)
                .font(.system(size: CGFloat(DesignTokens.Typography.fontSizeMD)))
                .foregroundColor(Color(hex: DesignTokens.Colors.textPrimary))
                .padding(.horizontal, CGFloat(DesignTokens.Spacing.LG))
                .padding(.vertical, CGFloat(DesignTokens.Spacing.MD))
                .background(backgroundColor)
                .cornerRadius(CGFloat(DesignTokens.CornerRadius.LG))
        }
        .disabled(!enabled)
    }

    private var backgroundColor: Color {
        switch variant {
        case .primary:
            return Color(hex: DesignTokens.Colors.primary)
        case .secondary, .tertiary, .ghost:
            return Color.clear
        }
    }
}
```

### Migration Path: Ocean → MagicUI/AvaCode

**Phase 1: Today**
- Use Ocean Design System (Material3 on Android, SwiftUI patterns on iOS)
- Shared design tokens in KMP

**Phase 2: Parallel Development**
- Implement MagicUI/AvaCode components alongside Ocean
- Feature flag to switch between implementations
```kotlin
val uiProvider = if (settings.useMagicUI) {
    MagicUIComponentProvider
} else {
    OceanComponentProvider
}
```

**Phase 3: Full Migration**
- All apps use MagicUI/AvaCode
- Ocean kept for compatibility/fallback

---

## Migration Strategy

### Step 1: Move Existing Ocean Components to Common/UI/ComposeUI

**From:**
```
Modules/WebAvanue/universal/src/commonMain/kotlin/presentation/design/
├── ComponentProvider.kt
├── OceanComponents.kt
└── OceanDesignTokens.kt
```

**To:**
```
Common/UI/ComposeUI/src/
├── commonMain/kotlin/design/
│   ├── DesignTokens.kt           # Platform-agnostic
│   └── ComponentInterface.kt     # Component contracts
└── androidMain/kotlin/ocean/
    ├── OceanComponents.kt        # Android Compose impl
    └── OceanTheme.kt
```

### Step 2: Update WebAvanue to Import from Common

```kotlin
// Before:
import com.augmentalis.Avanues.web.universal.presentation.design.OceanComponents
import com.augmentalis.Avanues.web.universal.presentation.design.OceanDesignTokens

// After:
import com.augmentalis.common.ui.ocean.OceanComponents
import com.augmentalis.common.ui.design.DesignTokens
```

### Step 3: Migrate Other Apps to Use Common/UI

**VoiceOS:**
```kotlin
// Replace custom Compose components with Ocean components
dependencies {
    implementation(project(":Common:UI:ComposeUI"))
}

// In VoiceOS screens:
@Composable
fun VoiceOSMainScreen() {
    OceanButton(
        text = "Start Voice Recognition",
        onClick = { startVoiceRecognition() }
    )
}
```

**AVA:**
```kotlin
// Standardize on Ocean Design System
dependencies {
    implementation(project(":Common:UI:ComposeUI"))  // Android
    implementation(project(":Common:UI:SwiftUI"))    // iOS
    implementation(project(":Common:UI:DesktopUI"))  // Desktop
}
```

### Step 4: Create MagicUI/AvaCode Variants

```kotlin
// Common/UI/ComposeUI/src/androidMain/kotlin/magic/MagicButton.kt

@Composable
fun MagicButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // MagicUI implementation (custom animations, effects)
    Box(
        modifier = modifier
            .magicGlow()  // Custom MagicUI effect
            .magicPulse() // Custom MagicUI animation
            .clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            style = MagicUITheme.typography.button
        )
    }
}
```

---

## Summary

### Total Effort Estimate

| Phase | Issues | Complexity | Time | LOC |
|-------|--------|------------|------|-----|
| Phase 1: Quick Wins | 1, 2, 3, 5 | Low | 6-8h | ~65 |
| Phase 2: Medium | 4 | Medium | 4-6h | ~15 |
| Phase 3: High | 6, 7 | High | 16-20h | ~300 |
| **Total** | **7 issues** | **Mixed** | **26-34h** | **~380** |

### Success Criteria

**Phase 1 Complete:**
- ✅ Voice commands dialog uses grid layout
- ✅ Bottom command bar doesn't scroll in portrait
- ✅ No visual artifacts in landscape mode
- ✅ Bookmark folders save correctly

**Phase 2 Complete:**
- ✅ Address bar updates URL on tab change even when focused

**Phase 3 Complete:**
- ✅ Downloads start and show progress
- ✅ All 70 settings applied to WebView when changed

### Long-Term Goals

**Architecture:**
- ✅ All apps use shared UI components from `Common/UI/`
- ✅ Design tokens shared across platforms
- ✅ Easy migration path Ocean → MagicUI/AvaCode

**Quality:**
- ✅ No recurring UI issues
- ✅ Comprehensive test coverage (90%+)
- ✅ Consistent UX across all apps

---

**Next Steps:**
1. Review and approve this plan
2. Begin Phase 1 implementation
3. Create `Common/UI/` module structure
4. Document component usage guidelines

**Maintainer:** WebAvanue Team
**Last Updated:** 2025-12-11
