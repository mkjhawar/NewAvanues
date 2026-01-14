# Implementation Plan: WebAvanue Settings Fixes & Enhancements

**Project:** WebAvanue
**Platform:** Android (Kotlin + Jetpack Compose)
**Created:** 2025-12-11
**Status:** Planning
**Priority:** HIGH

---

## Overview

**Platforms:** Android only
**Swarm Recommended:** No (single platform, focused fixes)
**Estimated:** 12 tasks, 8-10 hours
**Dependencies:** None (previous fixes already committed)

---

## Context

### Completed Fixes (Commit 27ce4382)
- ✅ Voice Commands Dialog crash (IntrinsicSize.Min issue)
- ✅ CommandsView layout (LazyVerticalGrid → LazyColumn)
- ✅ Orientation changes (removed android:configChanges)

### Remaining Issues
1. **Settings state caching** - Settings require multiple clicks to apply
2. **SettingsApplicator not integrated** - Settings not applying to WebView
3. **Download path UI** - Text field instead of directory picker
4. **Custom search engine** - No way to add custom engines
5. **Settings organization** - Needs functional grouping

---

## Phase 1: Settings State & Persistence (HIGH PRIORITY)

**Goal:** Fix state caching and integrate SettingsApplicator

### Task 1.1: Investigate Settings State Flow
**File:** `SettingsViewModel.kt`
**Priority:** P0

**Steps:**
1. Read SettingsViewModel implementation
2. Trace state flow: UI → ViewModel → Repository → Storage
3. Identify where state caching occurs
4. Check for debouncing or delayed updates
5. Verify StateFlow/LiveData configuration

**Expected Finding:** Settings updates may be batched or debounced

---

### Task 1.2: Fix Settings State Updates
**File:** `SettingsViewModel.kt`
**Priority:** P0

**Changes:**
1. Remove or reduce debouncing on critical settings (desktop mode, scale)
2. Ensure immediate state propagation to StateFlow
3. Add state change logging for debugging
4. Verify settings save immediately to persistent storage

**Test:** Change desktop mode → Verify immediate application

---

### Task 1.3: Locate SettingsApplicator Integration Point
**Files:**
- `WebViewLifecycle.kt` or `WebViewController.kt`
- `SettingsApplicator.kt`
**Priority:** P0

**Steps:**
1. Find where WebView is initialized
2. Locate WebView configuration code
3. Check if SettingsApplicator is called anywhere
4. Identify correct integration point in lifecycle

**Expected:** SettingsApplicator exists but never called

---

### Task 1.4: Integrate SettingsApplicator in WebView Lifecycle
**Files:**
- `WebViewLifecycle.kt` or `WebViewController.kt`
- `BrowserViewModel.kt`
**Priority:** P0

**Implementation:**
```kotlin
// In WebView initialization
private fun applySettings(webView: WebView, settings: BrowserSettings) {
    SettingsApplicator.applyToWebView(webView, settings)
}

// On settings change
viewModel.settings.collectAsState { newSettings ->
    currentWebView?.let { webView ->
        SettingsApplicator.applyToWebView(webView, newSettings)
    }
}
```

**Integration Points:**
1. WebView creation (initial settings)
2. Settings change listener (runtime updates)
3. Orientation change (reapply settings)
4. Tab switch (apply to newly active WebView)

**Test:**
- Change desktop mode → User agent changes immediately
- Change scale → WebView zoom updates immediately
- Rotate device → Settings persist

---

### Task 1.5: Add Settings Change Observer
**File:** `BrowserScreen.kt` or `WebViewController.kt`
**Priority:** P0

**Implementation:**
```kotlin
LaunchedEffect(settings) {
    currentWebView?.let { webView ->
        SettingsApplicator.applyToWebView(webView, settings)
    }
}
```

**Test:** Change any setting → WebView updates without reload

---

### Task 1.6: Handle Orientation Changes
**File:** `MainActivity.kt` or `BrowserViewModel.kt`
**Priority:** P1

**Implementation:**
- Ensure settings reapply after configuration change
- Test: Rotate device → Settings persist (desktop mode, scale, etc.)

**Notes:** AndroidManifest already fixed (no configChanges)

---

## Phase 2: Download Path Directory Picker (HIGH PRIORITY)

**Goal:** Replace text field with proper directory picker

### Task 2.1: Create DirectoryPickerDialog Composable
**File:** `DownloadPathPicker.kt` (new)
**Priority:** P1

**Implementation:**
```kotlin
@Composable
fun DirectoryPickerDialog(
    currentPath: String?,
    onPathSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    // Use DocumentsContract.ACTION_OPEN_DOCUMENT_TREE
    // Or implement custom directory browser
}
```

**Options:**
1. Use Android Storage Access Framework (SAF)
2. Custom file browser with directory listing
3. Preset paths + custom option

**Recommendation:** Use SAF for Android 10+ compatibility

---

### Task 2.2: Implement ActivityResultLauncher for Directory Picker
**File:** `SettingsScreen.kt`
**Priority:** P1

**Implementation:**
```kotlin
val directoryPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocumentTree()
) { uri ->
    uri?.let {
        viewModel.updateSettings(settings.copy(downloadPath = it.path))
    }
}
```

**Test:** Click browse → Directory picker opens → Select folder → Path updates

---

### Task 2.3: Update DownloadPathSettingItem
**File:** `SettingsScreen.kt` (line 2373-2421)
**Priority:** P1

**Changes:**
```kotlin
@Composable
fun DownloadPathSettingItem(
    currentPath: String?,
    onPathChanged: (String?) -> Unit,
    onBrowseClicked: () -> Unit,  // NEW
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = { Text("Download Location") },
        supportingContent = { Text(currentPath ?: "Default") },
        trailingContent = {
            Row {
                // Browse button
                IconButton(onClick = onBrowseClicked) {
                    Icon(Icons.Default.FolderOpen, "Browse")
                }
                // Edit button (manual path)
                IconButton(onClick = { showDialog = true }) {
                    Icon(Icons.Default.Edit, "Edit")
                }
            }
        }
    )
}
```

**Test:** Both browse and manual entry work

---

### Task 2.4: Add Storage Permissions Check
**File:** `SettingsScreen.kt` or `PermissionHandler.kt`
**Priority:** P1

**Implementation:**
```kotlin
val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
) { granted ->
    if (granted) {
        directoryPickerLauncher.launch(null)
    }
}

// Check permission before browse
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    // Android 11+ - no permission needed for SAF
    directoryPickerLauncher.launch(null)
} else {
    // Android 10 and below
    permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
}
```

**Test:** Permission flow works correctly

---

## Phase 3: Custom Search Engine Support (MEDIUM PRIORITY)

**Goal:** Allow users to add custom search engines

### Task 3.1: Update BrowserSettings Model
**File:** Search for data class with SearchEngine enum
**Priority:** P2

**Changes:**
```kotlin
enum class SearchEngine {
    GOOGLE,
    DUCKDUCKGO,
    BING,
    BRAVE,
    ECOSIA,
    CUSTOM  // NEW
}

data class CustomSearchEngine(
    val name: String,
    val searchUrl: String,  // e.g., "https://example.com/search?q={searchTerms}"
    val suggestUrl: String? = null
)

data class BrowserSettings(
    // ...
    val defaultSearchEngine: SearchEngine = SearchEngine.GOOGLE,
    val customSearchEngines: List<CustomSearchEngine> = emptyList(),  // NEW
    val selectedCustomEngine: CustomSearchEngine? = null,  // NEW
    // ...
)
```

---

### Task 3.2: Create AddCustomSearchEngineDialog
**File:** `CustomSearchEngineDialog.kt` (new)
**Priority:** P2

**Implementation:**
```kotlin
@Composable
fun AddCustomSearchEngineDialog(
    onDismiss: () -> Unit,
    onAdd: (CustomSearchEngine) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var searchUrl by remember { mutableStateOf("") }
    var suggestUrl by remember { mutableStateOf("") }

    AlertDialog(
        title = { Text("Add Custom Search Engine") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("My Search Engine") }
                )
                OutlinedTextField(
                    value = searchUrl,
                    onValueChange = { searchUrl = it },
                    label = { Text("Search URL") },
                    placeholder = { Text("https://example.com/search?q=%s") },
                    supportingText = { Text("Use %s for search term") }
                )
                OutlinedTextField(
                    value = suggestUrl,
                    onValueChange = { suggestUrl = it },
                    label = { Text("Suggestions URL (optional)") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onAdd(CustomSearchEngine(name, searchUrl, suggestUrl.ifBlank { null }))
                onDismiss()
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

---

### Task 3.3: Update SearchEngineSettingItem
**File:** `SettingsScreen.kt` (line 1950-2002)
**Priority:** P2

**Changes:**
```kotlin
@Composable
fun SearchEngineSettingItem(
    currentEngine: BrowserSettings.SearchEngine,
    customEngines: List<CustomSearchEngine>,
    selectedCustomEngine: CustomSearchEngine?,
    onEngineSelected: (BrowserSettings.SearchEngine) -> Unit,
    onCustomEngineSelected: (CustomSearchEngine) -> Unit,
    onAddCustomEngine: () -> Unit,  // NEW
    onEditCustomEngine: (CustomSearchEngine) -> Unit,  // NEW
    onDeleteCustomEngine: (CustomSearchEngine) -> Unit,  // NEW
    modifier: Modifier = Modifier
) {
    // Show predefined + custom engines in dropdown
    // Add "Add custom..." option at bottom
    // Long-press custom engine to edit/delete
}
```

---

### Task 3.4: Implement Search URL Replacement
**File:** `SearchUrlBuilder.kt` or in ViewModel
**Priority:** P2

**Implementation:**
```kotlin
fun buildSearchUrl(engine: SearchEngine, customEngine: CustomSearchEngine?, query: String): String {
    return when (engine) {
        SearchEngine.GOOGLE -> "https://www.google.com/search?q=${Uri.encode(query)}"
        SearchEngine.DUCKDUCKGO -> "https://duckduckgo.com/?q=${Uri.encode(query)}"
        // ... other engines
        SearchEngine.CUSTOM -> {
            customEngine?.searchUrl?.replace("%s", Uri.encode(query))
                ?: buildSearchUrl(SearchEngine.GOOGLE, null, query)  // Fallback
        }
    }
}
```

**Test:** Custom engine performs searches correctly

---

## Phase 4: Settings Organization & UI Improvements (MEDIUM PRIORITY)

**Goal:** Improve settings grouping and usability

### Task 4.1: Reorganize Settings into Functional Groups
**File:** `SettingsScreen.kt`
**Priority:** P2

**Current Structure:**
- General (8 items)
- Appearance (5 items)
- Privacy & Security (10 items)
- Advanced (everything else - too broad)
- Downloads (3 items)
- Performance (4 items)
- Sync (5 items)
- Voice & AI (3 items)
- Command Bar (2 items)
- WebXR (7 items)

**Proposed Structure:**
```
1. General (7 items)
   - Search engine
   - Homepage
   - New tab page
   - Search suggestions
   - Voice search
   - Restore tabs
   - Link behavior

2. Appearance (6 items)
   - Theme
   - Font size
   - Show images
   - Force zoom
   - Initial scale
   - Text reflow

3. Privacy & Security (10 items)
   - JavaScript, Cookies, etc.
   - (No changes - well organized)

4. Desktop Mode (5 items)  ← NEW GROUP
   - Enable desktop mode
   - Default zoom
   - Window size
   - Auto-fit zoom
   - User agent (NEW)

5. Downloads (3 items)
   - Download path (with picker)
   - Ask location
   - Wi-Fi only

6. Voice & Commands (5 items)  ← MERGED
   - Voice commands
   - Voice dialog auto-close
   - Voice dialog delay
   - Command bar auto-hide
   - Command bar delay

7. Performance (4 items)
   - (No changes)

8. Sync (5 items)
   - (No changes)

9. AI Features (3 items)
   - AI summaries
   - AI translation
   - Read aloud

10. WebXR (7 items)
    - (No changes)

11. Advanced (5 items)  ← REDUCED
    - Hardware acceleration
    - Preload pages
    - Data saver
    - Auto-play
    - WebRTC
```

**Benefits:**
- Desktop Mode is now its own section (clearer)
- Voice settings consolidated
- Advanced section reduced to truly advanced items

---

### Task 4.2: Update Landscape Two-Pane Layout
**File:** `SettingsScreen.kt` (SettingsCategory enum)
**Priority:** P3

**Changes:**
```kotlin
enum class SettingsCategory(val title: String, val icon: ImageVector) {
    GENERAL("General", Icons.Default.Settings),
    APPEARANCE("Appearance", Icons.Default.Palette),
    PRIVACY("Privacy & Security", Icons.Default.Shield),
    DESKTOP("Desktop Mode", Icons.Default.DesktopWindows),  // NEW
    DOWNLOADS("Downloads", Icons.Default.Download),  // NEW
    VOICE("Voice & Commands", Icons.Default.Mic),  // NEW
    PERFORMANCE("Performance", Icons.Default.Speed),  // NEW
    AI("AI Features", Icons.Default.AutoAwesome),  // NEW
    SYNC("Sync", Icons.Default.Sync),  // NEW
    XR("AR/XR", Icons.Default.ViewInAr),
    ADVANCED("Advanced", Icons.Default.Build)
}
```

**Test:** Landscape layout shows new categories correctly

---

## Phase 5: Testing & Validation

### Task 5.1: Unit Tests for SettingsApplicator
**File:** `SettingsApplicatorTest.kt`
**Priority:** P1

**Tests:**
```kotlin
@Test
fun testDesktopModeApplies()

@Test
fun testScaleApplies()

@Test
fun testJavaScriptToggle()

@Test
fun testSettingsPersistAcrossOrientationChange()
```

---

### Task 5.2: Integration Tests
**File:** `SettingsIntegrationTest.kt`
**Priority:** P2

**Tests:**
- Settings UI → ViewModel → Repository → WebView
- Custom search engine CRUD
- Directory picker flow

---

### Task 5.3: Manual Testing Checklist
**Reference:** `WebAvanue-Test-Plan-51211-V1.md`
**Priority:** P0

**Critical Tests:**
- TC-201 to TC-205: Settings state persistence
- TC-801 to TC-805: Downloads with directory picker
- TC-701 to TC-707: Desktop mode with SettingsApplicator

---

## Time Estimates

### Sequential Execution
| Phase | Tasks | Hours | Priority |
|-------|-------|-------|----------|
| Phase 1: Settings State | 6 tasks | 4-5 hours | P0 |
| Phase 2: Directory Picker | 4 tasks | 2-3 hours | P1 |
| Phase 3: Custom Search | 4 tasks | 1-2 hours | P2 |
| Phase 4: UI Reorganization | 2 tasks | 1 hour | P2 |
| Phase 5: Testing | 3 tasks | 1-2 hours | P1 |
| **Total** | **19 tasks** | **9-13 hours** | |

### Recommended Execution Order
1. Phase 1 (P0) - **MUST FIX FIRST**
2. Phase 2 (P1) - High user impact
3. Phase 5.3 (P0) - Validate Phase 1 & 2
4. Phase 3 (P2) - Nice to have
5. Phase 4 (P2) - Polish
6. Phase 5.1-5.2 (P1) - Automated tests

---

## Dependencies

### External Dependencies
- None (all Android SDK features)

### Internal Dependencies
- Phase 1 must complete before meaningful testing
- Phase 2 requires Android permissions
- Phase 3 requires BrowserSettings model update

---

## Risks & Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| SettingsApplicator location unclear | High | Search entire codebase, may need to create integration point |
| Directory picker permissions complex | Medium | Use SAF (no permissions needed on Android 11+) |
| Settings state caching deeply rooted | High | May need ViewModel refactor |
| Custom search engine URL parsing errors | Low | Validate URL format, provide fallback |

---

## Success Criteria

### Must Complete (Release Blocker)
- ✅ Settings apply immediately (no caching delay)
- ✅ Desktop mode scale works
- ✅ Orientation changes preserve settings
- ✅ All TC-201 to TC-205 tests pass

### Should Complete (High Priority)
- ✅ Directory picker for downloads
- ✅ All TC-801 to TC-805 tests pass
- ✅ Settings persist across app restart

### Nice to Have (Medium Priority)
- ✅ Custom search engine support
- ✅ Settings reorganization
- ✅ Automated tests

---

## Implementation Workflow

### Recommended Workflow
```bash
# Phase 1: Fix settings state
1. Read SettingsViewModel.kt
2. Locate SettingsApplicator integration point
3. Implement integration
4. Test manually (TC-201 to TC-205)

# Phase 2: Directory picker
5. Implement DirectoryPickerDialog
6. Update DownloadPathSettingItem
7. Test manually (TC-801 to TC-805)

# Phase 3: Custom search (optional)
8. Update BrowserSettings model
9. Create AddCustomSearchEngineDialog
10. Update SearchEngineSettingItem

# Phase 4: Reorganize (polish)
11. Update SettingsCategory enum
12. Reorganize settings items

# Phase 5: Test
13. Run manual test plan
14. Write unit tests
15. Document changes
```

---

## Next Steps

After plan approval:
1. Execute Phase 1 (Settings State) - **START HERE**
2. Manual test with TC-201 to TC-205
3. Execute Phase 2 (Directory Picker)
4. Manual test with TC-801 to TC-805
5. Decide on Phase 3 & 4 based on time/priority
6. Final validation against test plan

---

**Plan Version:** 1.0
**Created:** 2025-12-11
**Status:** Ready for implementation
**Estimated Completion:** Phase 1+2 = 6-8 hours
