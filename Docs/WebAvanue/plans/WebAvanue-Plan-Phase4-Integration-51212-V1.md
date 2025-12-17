# Implementation Plan: Phase 4 Integration & Settings UI
**Date:** 2025-12-12
**Project:** WebAvanue Browser
**Platforms:** Android (KMP Universal Module)
**Mode:** .yolo .swarm .cot

---

## Overview

**Objective:** Integrate completed backend implementations and finish Phase 4 P1 features

**Scope:**
1. Compile and test Session Restore + Settings UI changes
2. Complete UI integration for 3 P1 features
3. Wire completed composables into main UI

**Swarm Recommended:** NO (single platform, focused integration work)
**Estimated Tasks:** 8 tasks
**Sequential Time:** 5-6 hours
**Parallel Time:** N/A (tasks are sequential dependencies)

---

## Chain-of-Thought Reasoning

### Why This Order?

**Phase 1 (Build/Test):** Must verify backend changes compile before adding UI
- Session Restore database schema changes
- Repository method implementations
- Settings ViewModel state additions

**Phase 2 (Session UI):** Easiest P1 feature, tests compilation of domain models
- SessionRestoreDialog is standalone
- No complex integration points
- Validates Session/SessionTab classes work

**Phase 3 (Find in Page):** Quick win, builds confidence
- Feature already 95% complete
- Just adding keyboard shortcut
- Tests Activity-level integration

**Phase 4 (Settings Integration):** Most complex, benefits from working build
- Refactoring existing 2,541 line file
- Multiple integration points
- Needs careful testing of collapsible sections

**Phase 5 (Reading Mode):** Final feature, depends on all previous work
- Requires AddressBar changes (affects browser chrome)
- Article detection adds runtime logic
- Should be tested with working build

### KMP Considerations

**No KMP Setup Needed:**
- Work is in existing `universal` module (already KMP)
- Android-only testing (no iOS implementation yet)
- Database changes are commonMain (already cross-platform)

### Swarm Assessment

**NOT Recommended Because:**
- Single platform (Android)
- 8 sequential tasks (many dependencies)
- Integration work (not parallel development)
- Small scope (~5-6 hours)

**Would Recommend Swarm If:**
- Adding iOS implementations simultaneously
- Multiple independent features (could parallelize)
- Large refactoring (>10 hours sequential work)

---

## Phases

### Phase 1: Build & Validation (1 hour)

**Objective:** Ensure all backend changes compile and tests pass

**Tasks:**

**Task 1.1: Build coredata module**
```bash
./gradlew :Modules:WebAvanue:coredata:build --console=plain
```
- **Expected Issues:**
  - SQLDelight may need schema migration version bump
  - Mapper functions might have type mismatches
- **Fix Strategy:**
  - Check SQLDelight version in build.gradle.kts
  - Verify Session/SessionTab imports in BrowserRepositoryImpl
  - Run `./gradlew :Modules:WebAvanue:coredata:generateCommonMainBrowserDatabaseInterface` if needed

**Task 1.2: Build universal module**
```bash
./gradlew :Modules:WebAvanue:universal:build --console=plain
```
- **Expected Issues:**
  - SettingsScreen.kt missing imports for new composables
  - SettingsViewModel missing imports
- **Fix Strategy:**
  - Add missing imports
  - Fix any Compose API changes

**Task 1.3: Run existing tests**
```bash
./gradlew :Modules:WebAvanue:coredata:test
./gradlew :Modules:WebAvanue:universal:testDebugUnitTest
```
- **Acceptance Criteria:**
  - All existing tests pass
  - No new compilation errors
  - Database schema generates correctly

**Time Estimate:** 1 hour (including fixing any errors)

---

### Phase 2: Session Restore UI (1.5 hours)

**Objective:** Add crash recovery dialog and lifecycle integration

**Task 2.1: Create SessionRestoreDialog composable**

**File:** `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/ui/dialogs/SessionRestoreDialog.kt`

**Implementation:**
```kotlin
package com.augmentalis.Avanues.web.universal.presentation.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.Avanues.web.universal.presentation.ui.theme.OceanTheme

/**
 * Dialog shown on app startup if crash recovery session is detected
 *
 * Features:
 * - Shows tab count from crashed session
 * - Restore button (primary action)
 * - Dismiss button (starts fresh)
 * - Cannot be dismissed by outside click (critical decision)
 */
@Composable
fun SessionRestoreDialog(
    visible: Boolean,
    tabCount: Int,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = { /* Prevent dismiss by outside click */ },
        icon = {
            Icon(
                imageVector = Icons.Default.Restore,
                contentDescription = "Restore session",
                tint = OceanTheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Restore Previous Session?",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "WebAvanue didn't close properly last time.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Restore $tabCount ${if (tabCount == 1) "tab" else "tabs"}?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = OceanTheme.primary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onRestore,
                colors = ButtonDefaults.buttonColors(
                    containerColor = OceanTheme.primary
                )
            ) {
                Text("Restore Tabs")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Start Fresh")
            }
        },
        modifier = modifier
    )
}
```

**Acceptance Criteria:**
- Dialog shows tab count correctly
- Cannot be dismissed by tapping outside
- Restore button triggers tab restoration
- Dismiss button starts fresh session

**Task 2.2: Integrate dialog into BrowserScreen**

**File:** `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/ui/browser/BrowserScreen.kt`

**Changes:**

1. **Add state for dialog (around line 180):**
```kotlin
// Session restore dialog state
var showSessionRestoreDialog by remember { mutableStateOf(false) }
var sessionRestoreTabCount by remember { mutableStateOf(0) }
```

2. **Check for crash session on startup (after line 200):**
```kotlin
// Check for crash recovery session on startup
LaunchedEffect(Unit) {
    val crashSession = tabViewModel.getLatestCrashSession()
    if (crashSession != null && crashSession.tabCount > 0) {
        showSessionRestoreDialog = true
        sessionRestoreTabCount = crashSession.tabCount
    }
}
```

3. **Add dialog before closing Box (around line 1035):**
```kotlin
        // Session Restore Dialog
        SessionRestoreDialog(
            visible = showSessionRestoreDialog,
            tabCount = sessionRestoreTabCount,
            onRestore = {
                scope.launch {
                    val restored = tabViewModel.restoreCrashRecoverySession()
                    if (!restored) {
                        snackbarHostState.showSnackbar(
                            message = "Failed to restore session",
                            duration = SnackbarDuration.Short
                        )
                    }
                    showSessionRestoreDialog = false
                }
            },
            onDismiss = {
                scope.launch {
                    tabViewModel.dismissCrashRecovery()
                    showSessionRestoreDialog = false
                }
            }
        )
    } // End Box
```

4. **Add import:**
```kotlin
import com.augmentalis.Avanues.web.universal.presentation.ui.dialogs.SessionRestoreDialog
```

**Acceptance Criteria:**
- Dialog appears on startup if crash detected
- Restore button loads all tabs from crashed session
- Dismiss button clears crash session
- Dialog disappears after action taken

**Task 2.3: Add lifecycle session saving**

**File:** `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/ui/browser/BrowserScreen.kt`

**Add DisposableEffect for lifecycle (around line 220):**
```kotlin
// Save session on app pause/background
DisposableEffect(Unit) {
    onDispose {
        // Save current session when BrowserScreen is disposed
        scope.launch {
            tabViewModel.saveCurrentSession(isCrashRecovery = false)
        }
    }
}
```

**Acceptance Criteria:**
- Session saved when app goes to background
- Session saved when app is closed normally
- Crash sessions marked with isCrashRecovery = true

**Time Estimate:** 1.5 hours

---

### Phase 3: Find in Page Keyboard Shortcut (30 minutes)

**Objective:** Add Ctrl+F / Cmd+F keyboard shortcut

**Task 3.1: Add keyboard event handling to MainActivity**

**File:** `android/apps/webavanue/app/src/main/kotlin/com/augmentalis/Avanues/web/app/MainActivity.kt`

**Changes:**

1. **Add import:**
```kotlin
import android.view.KeyEvent
```

2. **Override dispatchKeyEvent (before onCreate):**
```kotlin
/**
 * Handle global keyboard shortcuts
 *
 * Shortcuts:
 * - Ctrl+F / Cmd+F: Open Find in Page
 */
override fun dispatchKeyEvent(event: KeyEvent): Boolean {
    // Only handle key down events
    if (event.action == KeyEvent.ACTION_DOWN) {
        // Ctrl+F or Cmd+F (Ctrl on Android, Meta on ChromeOS)
        if ((event.isCtrlPressed || event.isMetaPressed) &&
            event.keyCode == KeyEvent.KEYCODE_F) {

            // TODO: Need to pass event to BrowserScreen/TabViewModel
            // For now, log it
            android.util.Log.d("MainActivity", "Ctrl+F detected")
            return true
        }
    }

    return super.dispatchKeyEvent(event)
}
```

**Alternative Approach (Compose-level):**

**File:** `BrowserScreen.kt`

**Add to main Box modifier (around line 280):**
```kotlin
Box(
    modifier = modifier
        .fillMaxSize()
        .onPreviewKeyEvent { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown) {
                when {
                    // Ctrl+F / Cmd+F
                    (keyEvent.isCtrlPressed || keyEvent.isMetaPressed) &&
                    keyEvent.key == Key.F -> {
                        tabViewModel.showFindInPage()
                        true
                    }
                    // Escape - close find bar
                    keyEvent.key == Key.Escape && findInPageState.isVisible -> {
                        scope.launch {
                            webViewController.clearFindMatches()
                        }
                        tabViewModel.hideFindInPage()
                        true
                    }
                    else -> false
                }
            } else {
                false
            }
        }
) {
    // Existing content
}
```

**Add imports:**
```kotlin
import androidx.compose.ui.input.key.*
```

**Acceptance Criteria:**
- Ctrl+F opens Find in Page bar
- Cmd+F works on ChromeOS/desktop
- Escape closes Find bar
- Shortcuts don't interfere with WebView input

**Time Estimate:** 30 minutes

---

### Phase 4: Settings UI Integration (2-3 hours)

**Objective:** Wire SearchBar and CollapsibleHeader into SettingsScreen

**Task 4.1: Add search state to PortraitSettingsLayout**

**File:** `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/ui/settings/SettingsScreen.kt`

**Changes:**

1. **Collect search state (around line 180 in PortraitSettingsLayout):**
```kotlin
@Composable
private fun PortraitSettingsLayout(
    settings: BrowserSettings?,
    isLoading: Boolean,
    error: String?,
    onNavigateBack: () -> Unit,
    onNavigateToXRSettings: () -> Unit,
    onNavigateToSitePermissions: () -> Unit,
    onNavigateToARPreview: () -> Unit,
    onImportBookmarks: () -> Unit,
    onExportBookmarks: () -> Unit,
    viewModel: SettingsViewModel
) {
    // Collect search state
    val searchQuery by viewModel.searchQuery.collectAsState()
    val expandedSections by viewModel.expandedSections.collectAsState()

    // ... rest of function
```

2. **Add SearchBar to settings content Column (around line 250):**

Find this section:
```kotlin
settings != null -> {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
```

Replace with:
```kotlin
settings != null -> {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Search bar (sticky at top)
        SettingsSearchBar(
            searchQuery = searchQuery,
            onSearchQueryChange = { viewModel.setSearchQuery(it) },
            onExpandAll = { viewModel.expandAllSections() },
            onCollapseAll = { viewModel.collapseAllSections() }
        )

        // Scrollable settings content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
```

**Task 4.2: Wrap General section with collapsible header**

**Find General section (around line 260):**
```kotlin
// General Section
Text(
    text = "General",
    style = MaterialTheme.typography.titleLarge,
    color = MaterialTheme.colorScheme.primary
)
```

**Replace with:**
```kotlin
// General Section
val generalExpanded = expandedSections.contains("General")
val generalMatchesSearch = searchQuery.isNotBlank() &&
    (matchesSearchQuery("General", searchQuery) ||
     matchesSearchQuery("Home Page", searchQuery) ||
     matchesSearchQuery("Search Engine", searchQuery) ||
     matchesSearchQuery("New Tab Page", searchQuery))

CollapsibleSectionHeader(
    title = "General",
    isExpanded = generalExpanded,
    onToggle = { viewModel.toggleSection("General") },
    matchesSearch = generalMatchesSearch
)

AnimatedVisibility(visible = generalExpanded) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Existing General section settings
```

**Close the AnimatedVisibility after General section settings:**
```kotlin
        // ... last setting in General section ...
    } // Column
} // AnimatedVisibility
```

**Task 4.3: Add search filtering helper function**

**Add before PortraitSettingsLayout:**
```kotlin
/**
 * Check if text matches search query (case-insensitive)
 */
private fun matchesSearchQuery(text: String, query: String): Boolean {
    return text.contains(query, ignoreCase = true)
}
```

**Task 4.4: Repeat for remaining 10 sections**

Wrap each section with:
1. CollapsibleSectionHeader
2. AnimatedVisibility
3. Search matching logic

**Sections to wrap:**
- Appearance (theme, font size, force zoom, show images)
- Privacy & Security (desktop mode, blockers, cookies, JavaScript)
- Downloads (path, WiFi only, ask location)
- Performance (hardware accel, preload, data saver)
- Sync (enabled, bookmarks, history, passwords, settings)
- Bookmarks (import/export buttons)
- Voice & AI (voice commands, summaries, translation, read aloud)
- Command Bar (settings if any)
- WebXR (performance mode, FOV, IPD, tracking)
- Advanced (WebRTC, text reflow, clear on exit)

**Acceptance Criteria:**
- All 11 sections are collapsible
- Search filters settings by title/description
- Sections with matches auto-expand
- Matched sections highlighted
- Search bar sticky at top
- Scrolling only affects settings content

**Time Estimate:** 2-3 hours (repetitive work for 11 sections)

---

### Phase 5: Reading Mode UI (1 hour)

**Objective:** Add book icon button and article detection

**Task 5.1: Add Reading Mode button to AddressBar**

**File:** `Modules/WebAvanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/ui/browser/AddressBar.kt`

**Changes:**

1. **Find function signature (around line 50):**
```kotlin
@Composable
fun AddressBar(
    url: String,
    isLoading: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isDesktopMode: Boolean,
    onUrlChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onTabSwitcher: () -> Unit,
    onMenu: () -> Unit,
    onDesktopModeToggle: () -> Unit,
    modifier: Modifier = Modifier
)
```

**Add parameters:**
```kotlin
@Composable
fun AddressBar(
    url: String,
    isLoading: Boolean,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isDesktopMode: Boolean,
    isReadingMode: Boolean = false,             // NEW
    isArticleAvailable: Boolean = false,        // NEW
    onUrlChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onTabSwitcher: () -> Unit,
    onMenu: () -> Unit,
    onDesktopModeToggle: () -> Unit,
    onReadingModeToggle: () -> Unit = {},       // NEW
    modifier: Modifier = Modifier
)
```

2. **Add Reading Mode button (find button row, around line 120):**
```kotlin
// Reading Mode button (only show if article detected)
if (isArticleAvailable) {
    OceanComponents.IconButton(
        onClick = onReadingModeToggle,
        modifier = Modifier.size(40.dp)
    ) {
        OceanComponents.Icon(
            imageVector = Icons.Default.MenuBook,
            contentDescription = "Reading Mode",
            variant = if (isReadingMode)
                IconVariant.Primary
            else
                IconVariant.Secondary
        )
    }
}
```

3. **Add import:**
```kotlin
import androidx.compose.material.icons.filled.MenuBook
```

**Task 5.2: Add article detection to BrowserScreen**

**File:** `BrowserScreen.kt`

**Add after URL change handling (around line 400):**
```kotlin
// Detect articles for Reading Mode
LaunchedEffect(activeTab?.tab?.url, activeTab?.isLoading) {
    val tab = activeTab
    if (tab != null && tab.isLoading == false && tab.tab.url.isNotBlank()) {
        // Only check for articles on actual webpages (not about:blank)
        if (!tab.tab.url.startsWith("about:")) {
            delay(500) // Wait for page to stabilize

            val detectionScript = ReadingModeExtractor.getDetectionScript()
            val result = webViewController.evaluateJavaScript(detectionScript)
            val isArticle = result?.toBoolean() ?: false

            tabViewModel.setArticleAvailable(isArticle)
        } else {
            tabViewModel.setArticleAvailable(false)
        }
    }
}
```

**Task 5.3: Wire Reading Mode state to AddressBar**

**Find AddressBar call in BrowserScreen (around line 450):**
```kotlin
AddressBar(
    url = activeTab?.tab?.url ?: "",
    isLoading = activeTab?.isLoading ?: false,
    canGoBack = canGoBack,
    canGoForward = canGoForward,
    isDesktopMode = activeTab?.tab?.isDesktopMode ?: false,
    // ... rest of params
)
```

**Add new parameters:**
```kotlin
AddressBar(
    url = activeTab?.tab?.url ?: "",
    isLoading = activeTab?.isLoading ?: false,
    canGoBack = canGoBack,
    canGoForward = canGoForward,
    isDesktopMode = activeTab?.tab?.isDesktopMode ?: false,
    isReadingMode = activeTab?.isReadingMode ?: false,        // NEW
    isArticleAvailable = activeTab?.isArticleAvailable ?: false,  // NEW
    onUrlChange = { viewModel.updateUrl(it) },
    onSearch = { viewModel.loadUrl(it) },
    onBack = { webViewController.navigateBack() },
    onForward = { webViewController.navigateForward() },
    onRefresh = { webViewController.reloadPage() },
    onTabSwitcher = { showTabSwitcher = true },
    onMenu = { showMenu = true },
    onDesktopModeToggle = {
        webViewController.setDesktopMode(!(activeTab?.tab?.isDesktopMode ?: false))
    },
    onReadingModeToggle = {                                   // NEW
        if (activeTab?.isReadingMode == true) {
            tabViewModel.exitReadingMode()
        } else {
            scope.launch {
                val article = ReadingModeExtractor.extractArticle(
                    webViewController,
                    activeTab?.tab?.url ?: ""
                )
                if (article != null) {
                    tabViewModel.enterReadingMode(article)
                } else {
                    snackbarHostState.showSnackbar("Article extraction failed")
                }
            }
        }
    }
)
```

**Add import:**
```kotlin
import com.augmentalis.Avanues.web.universal.readingmode.ReadingModeExtractor
```

**Acceptance Criteria:**
- Book icon appears when article detected
- Book icon highlighted when in Reading Mode
- Click toggles Reading Mode
- Article detection runs on page load
- No article detection on about:blank or data:// URLs

**Time Estimate:** 1 hour

---

## Time Estimates

| Phase | Tasks | Sequential Time |
|-------|-------|-----------------|
| 1. Build & Validation | 3 | 1 hour |
| 2. Session Restore UI | 3 | 1.5 hours |
| 3. Find in Page Shortcut | 1 | 30 minutes |
| 4. Settings Integration | 4 | 2-3 hours |
| 5. Reading Mode UI | 3 | 1 hour |
| **TOTAL** | **14** | **5.5-6.5 hours** |

**No Parallel Option:** Tasks have dependencies (must build before UI work)

---

## Testing Checklist

### Session Restore
- [ ] Build succeeds with new database schema
- [ ] Repository methods compile
- [ ] Dialog appears on crash recovery
- [ ] Restore button loads all tabs
- [ ] Dismiss button starts fresh
- [ ] Session saved on app background
- [ ] Session saved on normal exit

### Find in Page
- [ ] Ctrl+F opens find bar
- [ ] Cmd+F works on ChromeOS
- [ ] Escape closes find bar
- [ ] Shortcuts work when WebView has focus

### Settings UI
- [ ] Search bar appears at top
- [ ] Search filters settings
- [ ] Sections expand/collapse
- [ ] Auto-expand on search
- [ ] Matched sections highlighted
- [ ] Expand All / Collapse All work
- [ ] Scrolling smooth (no jank)

### Reading Mode
- [ ] Book icon appears on articles
- [ ] No icon on non-article pages
- [ ] Icon highlighted when active
- [ ] Click enters Reading Mode
- [ ] Article extraction works
- [ ] Exit Reading Mode works

---

## Success Metrics

**Completion Criteria:**
- ✅ All 14 tasks completed
- ✅ All tests pass
- ✅ 3 P1 features fully integrated
- ✅ Settings UI reduces scrolling by 90%
- ✅ Session Restore enables crash recovery

**Quality Gates:**
- Zero compilation errors
- Zero runtime crashes
- SOLID principles maintained
- No performance regressions

---

## Risk Assessment

| Risk | Probability | Mitigation |
|------|-------------|------------|
| SQLDelight schema migration issues | Medium | Have rollback plan, test with clean install |
| Settings refactor breaks existing UI | Low | Wrap incrementally, test after each section |
| Keyboard shortcuts conflict | Low | Use standard shortcuts (Ctrl+F), test on multiple devices |
| Article detection false positives | Medium | Use conservative detection, allow manual toggle |

---

## Rollback Plan

If critical issues arise:

1. **Database:** Revert BrowserDatabase.sq, remove session methods
2. **Settings:** Remove SearchBar, keep original layout
3. **Find/Reading:** Remove features, mark as Phase 4.1

All changes are additive (no breaking changes to existing code).

---

## Next Steps After Completion

**Phase 4 Remaining Work:**
- Private Browsing UI (P2) - 1-2 hours
- Screenshots UI (P2) - 2-3 hours
- Bookmarks Import/Export (P3) - 2-3 hours

**Testing:**
- Unit tests for Session Restore
- UI tests for Settings collapsible sections
- Integration tests for full workflows

**Documentation:**
- Update README with new features
- Add CHANGELOG entries
- Document keyboard shortcuts

---

**Plan Created By:** Claude (Autonomous Mode .yolo .swarm .cot)
**Ready for:** Immediate implementation
**Estimated Completion:** 1 working day
