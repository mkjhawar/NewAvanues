# Feature Specification: WebAvanue UI/UX and Functional Improvements

**Version:** 1.3
**Date:** 2025-12-03
**Project:** WebAvanue Browser (v1.8.1)
**Platform:** Android (Kotlin Multiplatform with Jetpack Compose)
**Status:** Draft - Awaiting Approval

**Modifications:**
- **v1.3:** Added landscape orientation support for FR-009 arc layout (vertical arc, adjusted parameters)
- **v1.2:** Added FR-022 to FR-029 (7 developer APIs + graceful degradation)
- **v1.1:** Added FR-019, FR-020, FR-021 (Intent APIs + SDK)
- **v1.0:** Initial specification with 21 FRs

---

## Executive Summary

This specification addresses 28 UI/UX and functional issues identified in the WebAvanue v1.8.1 test report, plus comprehensive developer API requirements. Issues span visual styling, layout consistency, state management, missing features, and UX improvements. Primary goals: eliminate redundancy, improve visual consistency across themes/orientations, implement headless browser mode with external app integration, complete missing features (tab groups, file downloads, history), and provide rich developer APIs for AR/XR integration.

**Key Changes:**
- Headless browser mode (true fullscreen)
- **External app integration via Intents and APIs**
- **Deep linking support for programmatic control**
- **7 advanced developer APIs: JavaScript execution, page callbacks, screenshot capture, WebXR detection, zoom control, cookie management, find in page**
- **Graceful degradation with themed error dialogs**
- Tab groups implementation
- File download functionality
- Command bar state sync and layout fixes
- Voice dialog improvements
- Navigation and settings consolidation

---

## Chain of Thought Reasoning

### 1. Platform Detection
- **Input:** "WebAvanue UI/UX improvements for Android browser + external app APIs"
- **Detection:** Single platform (Android), existing project, new integration layer
- **Confidence:** 100%
- **Tech Stack:** Kotlin Multiplatform + Jetpack Compose + Material3 + WebView + Android Intent System
- **Reasoning:** All issues are Android UI/UX specific, plus Android-specific IPC mechanisms (Intents, Broadcast Receivers)

### 2. Issue Categorization

**Visual/Styling (Priority: Medium, Complexity: Low):**
- Floating action button lacks glass effect (FR-001)
- Light theme dropdown visibility (FR-012)
- Landscape search engine popup layout (FR-013)
- VoiceCommandDialog button sizing inconsistent (FR-003)

**Layout/Alignment (Priority: High, Complexity: Low):**
- Portrait command bar not centered, scrollable (FR-011)
- VoiceCommandDialog single-column in landscape (FR-002)

**State Management (Priority: Critical, Complexity: Medium):**
- Command bar toggle state incorrect (FR-010)
- Command bar auto-hides unexpectedly (FR-011b)

**Missing Features (Priority: Critical, Complexity: High):**
- Headless browser mode (FR-005)
- **External app integration APIs (FR-020, FR-021) - NEW**
- Tab groups (FR-015)
- File downloads (FR-016)

**Navigation/UX (Priority: High, Complexity: Low-Medium):**
- Tab management back button missing (FR-006)
- Network status alerts (FR-008)
- Star icon should be History not Favorites (FR-009)
- Default homepage not loading (FR-018)

**Settings (Priority: Medium, Complexity: Low):**
- Voice dialog auto-timeout toggle (FR-004)
- WebXR settings separate (FR-014)

**Redundancy (Priority: High, Complexity: Low):**
- Remove Prev/Next/Reload buttons from command bar (FR-019)

### 3. Priority Ranking Logic

**Critical (Implement First):**
- FR-010, FR-011: State management - blocks user expectations
- FR-005: Headless mode - AR/XR use case enabler
- **FR-019, FR-020, FR-021: External APIs - enables third-party AR/XR integrations**
- **FR-022, FR-023: JavaScript execution + page callbacks - essential for AR integration**
- **FR-029: Graceful degradation - prevents crashes on all devices**
- FR-016: File downloads - core browser functionality missing

**High (Implement Second):**
- **FR-024: Screenshot API - unique AR/XR value proposition**
- **FR-025: WebXR detection - AR coordination**
- FR-006, FR-009, FR-018: Navigation improvements
- FR-002, FR-011: Layout fixes affecting usability

**Medium (Implement Third):**
- **FR-026: Zoom control - accessibility + AR gestures**
- FR-001, FR-012, FR-013: Visual polish
- FR-004, FR-014: Settings consolidation
- FR-015: Tab groups (nice-to-have)
- **FR-027: Cookie management - SSO enabler**

**Low (Implement Fourth):**
- **FR-028: Find in page - can be done via JS API**
- FR-008: Network alerts (edge case)
- FR-007, FR-017: Minor UX improvements

### 4. Technical Constraints

**WebView Limitations:**
- File downloads require DownloadListener implementation
- Network detection requires ConnectivityManager
- WebXR already supported via DOM API

**Compose Limitations:**
- GridLayout vs FlowRow for voice commands (API level consideration)
- BoxWithConstraints required for orientation detection
- State hoisting needed for command bar visibility

**AOSP Compatibility:**
- Must test on HMT-1 (Android 10)
- Avoid APIs above API 29
- Handler.postDelayed for UI updates (ANR prevention)

**Android IPC Security:**
- Intent validation to prevent malicious apps
- Signature-based permissions for control APIs
- Content provider with read/write permissions

---

## Problem Statement

### Current State
WebAvanue v1.8.1 has successfully resolved ANR issues but has accumulated UI/UX debt:
- Command bar visibility state doesn't reflect actual UI state
- VoiceCommandDialog uses single column in landscape (wastes space)
- Light theme has white-on-white dropdown text (invisible)
- Headless mode flag exists in BrowserSettings but not fully implemented
- **No external app integration - AR/XR apps cannot embed WebAvanue**
- File downloads UI exists but functionality is broken (stuck in "pending")
- Tab groups UI exists but no functionality
- Redundant buttons in command bar (duplicates of address bar)

### Pain Points
1. **AR/XR Developers:** Cannot launch WebAvanue in headless mode from their apps
2. **AR/XR Users:** Cannot enter true fullscreen (headless mode incomplete)
3. **Tablet Users:** Voice commands cramped in landscape mode
4. **Light Theme Users:** Cannot read dropdown text
5. **All Users:** Confusing toggle state, unexpected auto-hide, redundant buttons
6. **Power Users:** Cannot organize tabs into groups
7. **Mobile Users:** Cannot download files

### Desired State
- **External apps can launch WebAvanue in headless mode via Intents**
- **External apps can control navigation programmatically**
- Command bar state accurately reflects visibility
- Voice dialog uses 2-3 columns in landscape
- Dropdowns readable in all themes
- Headless mode fully functional (true fullscreen)
- File downloads working
- Tab groups functional
- Redundant UI removed

---

## Functional Requirements

### FR-001: Floating Action Button Glass Effect
**Priority:** Medium | **Complexity:** Low | **Effort:** 1 hour

**Description:** Floating help button (?) needs glassmorphic styling and higher z-index.

**Current Behavior:**
- Solid blue button, flat appearance
- Sometimes appears behind command bar

**Expected Behavior:**
- Glassmorphic effect matching command bar (semi-transparent, blur)
- Z-index higher than command bar
- Consistent with OceanTheme design language

**Implementation:**
- File: `common/webavanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/ui/browser/BrowserScreen.kt`
- Apply `glassBar` modifier to help FAB
- Set `zIndex(10f)` (command bar is `zIndex(5f)`)

**Acceptance Criteria:**
- [ ] Help button has semi-transparent background with blur
- [ ] Button always visible above command bar
- [ ] Matches Ocean Blue theme palette

---

### FR-002: VoiceCommandDialog Multi-Column Layout
**Priority:** High | **Complexity:** Low | **Effort:** 2 hours

**Description:** Voice command dialog should use 2-3 columns in landscape/horizontal mode.

**Current Behavior:**
- Single column regardless of orientation
- Wastes horizontal space on tablets/landscape

**Expected Behavior:**
- Portrait: Single column (current)
- Landscape (width > height): 2 columns
- Tablet landscape (width > 800dp): 3 columns

**Implementation:**
- File: `common/webavanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/ui/dialogs/VoiceCommandDialog.kt`
- Use `BoxWithConstraints` to detect orientation
- Replace `Column` with `FlowRow` (or LazyVerticalGrid if minSdk allows)
- Calculate columns: `if (maxWidth > 800.dp) 3 else if (maxWidth > maxHeight) 2 else 1`

**Acceptance Criteria:**
- [ ] Portrait: 1 column
- [ ] Landscape phone: 2 columns
- [ ] Landscape tablet: 3 columns
- [ ] Buttons maintain 48dp touch targets
- [ ] Tested on HMT-1 (Android 10)

---

### FR-003: VoiceCommandDialog Consistent Button Sizing
**Priority:** Medium | **Complexity:** Low | **Effort:** 1 hour

**Description:** Voice command buttons have inconsistent widths.

**Current Behavior:**
- Buttons have varying widths based on text content

**Expected Behavior:**
- All buttons same width (uniform grid)
- Minimum 48dp height (touch target)

**Implementation:**
- File: `VoiceCommandDialog.kt`
- Add `Modifier.fillMaxWidth()` to each button in grid layout
- Set `minHeight = 48.dp`

**Acceptance Criteria:**
- [ ] All buttons same width within their column
- [ ] 48dp minimum height
- [ ] Text centered or aligned consistently

---

### FR-004: Voice Dialog Auto-Timeout Setting
**Priority:** Medium | **Complexity:** Low | **Effort:** 1 hour

**Description:** Add toggle in Settings to disable VoiceCommandDialog auto-timeout.

**Current Behavior:**
- Dialog auto-closes after inactivity (hardcoded)
- No user control

**Expected Behavior:**
- Settings > Advanced > "Voice Dialog Auto-Close" toggle (default: ON)
- When OFF, dialog stays open until user closes it

**Implementation:**
1. **BrowserSettings.kt:** Add `voiceDialogAutoClose: Boolean = true`
2. **SettingsScreen.kt:** Add toggle under Advanced section
3. **VoiceCommandDialog.kt:** Conditionally register LaunchedEffect timeout based on setting

**Acceptance Criteria:**
- [ ] Toggle appears in Settings > Advanced
- [ ] When ON: Dialog auto-closes after 10 seconds
- [ ] When OFF: Dialog stays open indefinitely
- [ ] Default: ON (current behavior)

---

### FR-005: Headless Browser Mode (True Fullscreen)
**Priority:** Critical | **Complexity:** High | **Effort:** 8 hours

**Description:** Implement true fullscreen mode hiding address bar, only showing command bar.

**Current State:**
- `headlessMode` field exists in BrowserSettings (added v1.8.0)
- Partial implementation in BottomCommandBar (conditional button rendering)
- AddressBar always visible
- BrowserScreen doesn't read headlessMode setting

**Expected Behavior:**
- When enabled: Address bar hidden, only command bar + WebView
- Desktop mode toggle moves to command bar PAGE level
- Favorite/History buttons move to command bar MENU level
- Command bar becomes primary navigation
- **Controllable via external apps (see FR-020, FR-021)**

**Implementation:**

**Phase 1: BrowserScreen Integration (3 hours)**
- File: `BrowserScreen.kt`
- Read `settings.headlessMode` from ViewModel
- Conditionally render AddressBar: `if (!isHeadlessMode) { AddressBar(...) }`
- Pass `isHeadlessMode` to BottomCommandBar
- Adjust WebView layout to use full height when headless

**Phase 2: Command Bar Layout Updates (3 hours)**
- File: `BottomCommandBar.kt`
- PAGE level buttons when headless:
  - Desktop Mode toggle (currently in AddressBar)
  - Add to Favorites (currently in AddressBar star icon)
- MENU level buttons when headless:
  - History (repurpose star icon functionality)
  - Bookmarks, Downloads, Settings (existing)
- Remove conditional zoom controls (redundant)

**Phase 3: Settings UI (2 hours)**
- File: `SettingsScreen.kt`
- Add "Headless Mode" toggle under Advanced section
- Description: "Hide address bar for fullscreen browsing. Navigation via command bar."
- Warning: "Experimental: May affect usability"

**Acceptance Criteria:**
- [ ] Settings toggle appears in Advanced section
- [ ] When enabled: AddressBar hidden, command bar visible
- [ ] When disabled: AddressBar visible (current behavior)
- [ ] Desktop mode toggle accessible in headless mode
- [ ] All navigation functions accessible via command bar
- [ ] WebView uses full screen height (minus command bar)
- [ ] Tested on HMT-1
- [ ] **Can be controlled via external Intents (FR-020)**

---

### FR-006: Tab Management Screen Navigation
**Priority:** High | **Complexity:** Low | **Effort:** 1 hour

**Description:** Add back button to tab management screen, remove close button.

**Current Behavior:**
- Top bar shows: Grid/List toggle, New Tab (+), Close (X)
- Close button redundant (can use device back button)
- No explicit back button

**Expected Behavior:**
- Top bar: Back (<), "6 tabs", Grid/List toggle, New Tab (+)
- Remove Close (X) button
- Back button navigates to browser screen

**Implementation:**
- File: `common/webavanue/universal/src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/presentation/ui/tab/TabSwitcherView.kt`
- Replace Close IconButton with Back IconButton
- Connect to `onBack` callback (likely already exists)

**Acceptance Criteria:**
- [ ] Back button appears at left
- [ ] Close button removed
- [ ] Back button navigates to browser
- [ ] Tab count shown in title

---

### FR-007: Tab Management List/Grid Icons
**Priority:** Low | **Complexity:** Low | **Effort:** 0.5 hours

**Description:** Current icons unclear for list vs grid view.

**Current Behavior:**
- Icons don't clearly indicate grid vs list mode

**Expected Behavior:**
- Grid mode icon: `Icons.Default.GridView` (3x3 grid)
- List mode icon: `Icons.Default.ViewList` (horizontal lines)
- Or toggle button showing current state

**Implementation:**
- File: `TabSwitcherView.kt`
- Replace existing icons with `GridView` and `ViewList`

**Acceptance Criteria:**
- [ ] Icons clearly represent grid/list
- [ ] Icon changes based on current view mode

---

### FR-008: Network Status Alerts
**Priority:** Low | **Complexity:** Medium | **Effort:** 3 hours

**Description:** Show alert when user tries to navigate without network connection.

**Implementation:**

**Phase 1: Network Detection (2 hours)**
- Create `NetworkManager.kt` in `domain/` layer
- Use `ConnectivityManager` to monitor network state
- Expose `StateFlow<Boolean>` for isConnected
- Platform-specific implementation (Android only)

**Phase 2: UI Integration (1 hour)**
- File: `BrowserScreen.kt`
- Collect network state in ViewModel
- Show Snackbar when navigation attempted offline: "No internet connection. Check your network settings."
- Intercept `onGo()` and `onUrlChange()` calls

**Acceptance Criteria:**
- [ ] Alert shown when navigating offline
- [ ] Alert dismissible
- [ ] Alert only shown for user-initiated navigation
- [ ] Does not block cached page viewing

---

### FR-009: Favorites Screen with History Access (Dual View Mode)
**Priority:** High | **Complexity:** Medium | **Effort:** 3 hours

**Description:** Star icon in AddressBar should open Favorites screen with History option, voice command support, and **dual viewing modes** (List + Pseudo AR).

**Current Behavior:**
- Star icon adds current page to favorites (single action)
- Yellow when page is favorited, gray otherwise
- Pseudo AR view already exists for spatial browsing
- No dedicated Favorites viewing screen with tabs

**Expected Behavior:**
- Star icon opens Favorites screen (shows all favorites)
- Favorites screen has "History" button/tab to switch to History view
- **Two viewing modes:** List View (default) and Pseudo AR View (spatial)
- **View toggle button** to switch between List ↔ AR view
- Voice commands: "show history", "open history", "go to history" navigate to History
- Voice commands: "AR view", "list view" switch viewing modes
- Star icon retains favorite state indication (yellow = favorited, gray = not favorited)

**Rationale:**
- Users need quick access to both Favorites and History
- List view for quick scanning, AR view for spatial/immersive browsing
- Leverage existing pseudo AR view implementation
- Voice command for hands-free History access (AR/XR use case)
- Consistent with WebAvanue's AR/XR design philosophy

**Implementation:**

**Phase 1: Favorites Screen UI with Dual View Mode (1.5 hours)**
- File: Create `FavoritesScreen.kt` in `presentation/ui/favorites/`

```kotlin
@Composable
fun FavoritesScreen(
    favorites: List<Favorite>,
    history: List<HistoryItem>,
    selectedTab: FavoritesTab = FavoritesTab.FAVORITES,
    viewMode: ViewMode = ViewMode.LIST,
    onTabChange: (FavoritesTab) -> Unit,
    onViewModeChange: (ViewMode) -> Unit,
    onFavoriteClick: (Favorite) -> Unit,
    onHistoryClick: (HistoryItem) -> Unit,
    onDeleteFavorite: (Favorite) -> Unit,
    onDeleteHistory: (HistoryItem) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar with tabs and view mode toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                modifier = Modifier.weight(1f)
            ) {
                Tab(
                    selected = selectedTab == FavoritesTab.FAVORITES,
                    onClick = { onTabChange(FavoritesTab.FAVORITES) },
                    text = { Text("Favorites") }
                )
                Tab(
                    selected = selectedTab == FavoritesTab.HISTORY,
                    onClick = { onTabChange(FavoritesTab.HISTORY) },
                    text = { Text("History") }
                )
            }

            // View mode toggle button
            IconButton(onClick = {
                onViewModeChange(
                    if (viewMode == ViewMode.LIST) ViewMode.AR else ViewMode.LIST
                )
            }) {
                Icon(
                    imageVector = if (viewMode == ViewMode.LIST) {
                        Icons.Default.ViewInAr // Switch to AR view
                    } else {
                        Icons.Default.List // Switch to List view
                    },
                    contentDescription = "Toggle view mode"
                )
            }
        }

        // Content based on selected tab and view mode
        when (selectedTab) {
            FavoritesTab.FAVORITES -> {
                when (viewMode) {
                    ViewMode.LIST -> FavoritesList(favorites, onFavoriteClick, onDeleteFavorite)
                    ViewMode.AR -> FavoritesARView(favorites, onFavoriteClick, onDeleteFavorite)
                }
            }
            FavoritesTab.HISTORY -> {
                when (viewMode) {
                    ViewMode.LIST -> HistoryList(history, onHistoryClick, onDeleteHistory)
                    ViewMode.AR -> HistoryARView(history, onHistoryClick, onDeleteHistory)
                }
            }
        }
    }
}

enum class FavoritesTab {
    FAVORITES,
    HISTORY
}

enum class ViewMode {
    LIST,  // Traditional list view
    AR     // Pseudo AR spatial view
}
```

**List View (Standard):**
```kotlin
@Composable
fun FavoritesList(
    favorites: List<Favorite>,
    onFavoriteClick: (Favorite) -> Unit,
    onDeleteFavorite: (Favorite) -> Unit
) {
    LazyColumn {
        items(favorites) { favorite ->
            ListItem(
                headlineContent = { Text(favorite.title) },
                supportingContent = { Text(favorite.url) },
                leadingContent = {
                    Icon(Icons.Default.Star, contentDescription = null)
                },
                trailingContent = {
                    IconButton(onClick = { onDeleteFavorite(favorite) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                },
                modifier = Modifier.clickable { onFavoriteClick(favorite) }
            )
        }
    }
}
```

**AR View (Arc Layout - Leverage Existing):**
```kotlin
@Composable
fun FavoritesARView(
    favorites: List<Favorite>,
    onFavoriteClick: (Favorite) -> Unit,
    onDeleteFavorite: (Favorite) -> Unit
) {
    // Use existing arc layout implementation
    // Display thumbnails curved across screen in 3D arc
    var currentIndex by remember { mutableStateOf(0) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
    ) {
        val isLandscape = maxWidth > maxHeight

        // Arc orientation based on device orientation
        // Portrait: Horizontal arc (items spread left-right)
        // Landscape: Vertical arc (items spread top-bottom, rotated 90°)
        val arcOrientation = if (isLandscape) ArcOrientation.VERTICAL else ArcOrientation.HORIZONTAL
        val arcRadius = if (isLandscape) 500.dp else 400.dp
        val itemSpacing = if (isLandscape) 35f else 45f // tighter spacing in landscape

        // Reuse existing ArcLayout or CarouselArcView composable
        ArcLayout(
            items = favorites,
            currentIndex = currentIndex,
            onIndexChange = { currentIndex = it },
            onItemClick = onFavoriteClick,
            onItemLongPress = onDeleteFavorite,
            orientation = arcOrientation, // NEW: pass orientation
            arcRadius = arcRadius,
            itemSpacing = itemSpacing, // degrees between items
            centerScale = 1.0f,
            sideScale = 0.6f
        ) { favorite, index ->
            // Thumbnail card for each favorite
            FavoriteThumbnailCard(
                favorite = favorite,
                isCenterItem = index == currentIndex,
                modifier = Modifier
                    .size(
                        width = if (index == currentIndex) 200.dp else 120.dp,
                        height = if (index == currentIndex) 150.dp else 90.dp
                    )
            )
        }

        // Instruction hint at bottom (portrait) or end (landscape)
        Text(
            text = if (isLandscape) "↑ Swipe ↓" else "← Swipe to rotate arc →",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier
                .align(if (isLandscape) Alignment.CenterEnd else Alignment.BottomCenter)
                .padding(
                    end = if (isLandscape) 24.dp else 0.dp,
                    bottom = if (isLandscape) 0.dp else 24.dp
                )
        )
    }
}

enum class ArcOrientation {
    HORIZONTAL, // Arc curves left-right (portrait mode)
    VERTICAL    // Arc curves top-bottom (landscape mode, rotated 90°)
}

@Composable
fun FavoriteThumbnailCard(
    favorite: Favorite,
    isCenterItem: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(
                alpha = if (isCenterItem) 1f else 0.7f
            )
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isCenterItem) 8.dp else 4.dp
        )
    ) {
        Column {
            // Page thumbnail/screenshot
            AsyncImage(
                model = favorite.thumbnailUrl ?: favorite.faviconUrl,
                contentDescription = favorite.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentScale = ContentScale.Crop
            )

            // Title overlay (glassmorphic)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                    .padding(8.dp)
            ) {
                Text(
                    text = favorite.title,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
```

**Arc Layout Details:**
- **Center item:** Largest, fully opaque, main focus
- **Adjacent items:** 60% size, slightly faded
- **Far items:** 36% size (0.6²), more transparent
- **Swipe gesture:** Rotates arc to bring items to center
- **Tap center:** Navigate to that favorite
- **Long-press:** Delete menu appears
- **Smooth animations:** 300ms transitions between rotations
- **Portrait orientation:** Horizontal arc (items curve left-right), swipe left/right to rotate
- **Landscape orientation:** Vertical arc (items curve top-bottom, rotated 90°), swipe up/down to rotate
  - Increased arc radius (500.dp vs 400.dp) for wider screens
  - Tighter item spacing (35° vs 45°) to fit more items in view
  - Instruction hint positioned at right edge instead of bottom

**Visual Representation:**

```
PORTRAIT MODE (Horizontal Arc):
┌──────────────────┐
│                  │
│   [▢]            │  ▢ = Thumbnail (smaller)
│      [▢]         │  ▣ = Center thumbnail (largest)
│         [▣]      │  Arc curves left to right
│      [▢]         │
│   [▢]            │
│                  │
│ ← Swipe left/right →
└──────────────────┘

LANDSCAPE MODE (Vertical Arc):
┌────────────────────────────┐
│        [▢]                 │  ▢ = Thumbnail (smaller)
│     [▢]                    │  ▣ = Center thumbnail (largest)
│  [▣]                    ↑  │  Arc curves top to bottom (90° rotated)
│     [▢]            Swipe   │  Larger radius, tighter spacing
│        [▢]            ↓  │
└────────────────────────────┘
```

**Phase 2: AddressBar Integration (0.5 hours)**
- File: `AddressBar.kt`
- Change star icon click handler from `onAddFavorite` to `onOpenFavorites`
- Keep favorite state indication (yellow/gray)

```kotlin
// Star icon (opens Favorites screen)
IconButton(
    onClick = onOpenFavorites, // Changed from onAddFavorite
    modifier = Modifier.size(48.dp)
) {
    Icon(
        imageVector = Icons.Default.Star,
        contentDescription = "Favorites",
        tint = if (isFavorite) {
            Color(0xFFFFC107) // Yellow (favorited)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) // Gray
        },
        modifier = Modifier.size(24.dp)
    )
}
```

**Phase 3: Voice Command Support (1 hour)**
- File: `VoiceCommandDialog.kt` or `BrowserViewModel.kt`

Add voice commands:
- "show history" → Navigate to Favorites screen with History tab selected
- "open history" → Same as above
- "go to history" → Same as above
- "show favorites" → Navigate to Favorites screen with Favorites tab selected
- **"AR view"** → Switch to AR/spatial view mode
- **"list view"** → Switch to list view mode
- **"switch view"** → Toggle between current view modes

```kotlin
// In voice command processor
when {
    command.contains("show history") ||
    command.contains("open history") ||
    command.contains("go to history") -> {
        navigateToFavorites(
            selectedTab = FavoritesTab.HISTORY,
            viewMode = ViewMode.LIST // Default to list
        )
    }
    command.contains("show favorites") -> {
        navigateToFavorites(
            selectedTab = FavoritesTab.FAVORITES,
            viewMode = ViewMode.LIST
        )
    }
    command.contains("ar view") ||
    command.contains("spatial view") -> {
        setViewMode(ViewMode.AR)
    }
    command.contains("list view") ||
    command.contains("normal view") -> {
        setViewMode(ViewMode.LIST)
    }
    command.contains("switch view") ||
    command.contains("toggle view") -> {
        toggleViewMode()
    }
}
```

**Acceptance Criteria:**
- [ ] Star icon opens Favorites screen
- [ ] Favorites screen has two tabs: Favorites and History
- [ ] **View mode toggle button switches between List ↔ AR view**
- [ ] Star icon remains yellow when page is favorited, gray otherwise
- [ ] Tapping Favorites tab shows list/AR view of all favorites
- [ ] Tapping History tab shows list/AR view of browsing history
- [ ] **List view displays standard scrollable list with delete buttons**
- [ ] **AR view displays items in arc/carousel layout (reuses existing ArcLayout composable)**
- [ ] **Arc layout displays horizontal arc in portrait mode (items curve left-right)**
- [ ] **Arc layout displays vertical arc in landscape mode (items curve top-bottom, rotated 90°)**
- [ ] **Arc parameters adjust for landscape: larger radius (500.dp), tighter spacing (35°)**
- [ ] Voice command "show history" opens History tab (list view)
- [ ] Voice command "show favorites" opens Favorites tab (list view)
- [ ] **Voice command "AR view" switches to spatial view mode**
- [ ] **Voice command "list view" switches to standard list mode**
- [ ] **Voice command "switch view" toggles between modes**
- [ ] Long-press on favorite/history item shows delete option (both views)
- [ ] Tapping favorite/history item navigates to that URL (both views)
- [ ] Back button closes Favorites screen and returns to browser
- [ ] View mode preference persists across sessions

---

### FR-010: Command Bar Toggle State Sync
**Priority:** Critical | **Complexity:** Medium | **Effort:** 2 hours

**Description:** Command bar visibility toggle button doesn't reflect actual visibility state.

**Current Behavior:**
- Toggle button state independent of actual command bar visibility
- Clicking toggle doesn't always match expected behavior

**Root Cause:**
- State management issue in `CommandBarWrapper` or parent
- `isVisible` state not properly hoisted to parent

**Expected Behavior:**
- Toggle button reflects actual command bar visibility
- Clicking toggle immediately updates state and UI

**Implementation:**
- File: `BrowserScreen.kt` and `BottomCommandBar.kt`
- Ensure `isCommandBarVisible` state hoisted to ViewModel
- Pass state down to `CommandBarWrapper`
- Verify `onToggleVisibility` updates ViewModel state
- Add logging to debug state transitions

**Acceptance Criteria:**
- [ ] Toggle button shows correct state (eye icon changes)
- [ ] Clicking toggle immediately shows/hides command bar
- [ ] State persists across orientation changes
- [ ] State synced with actual UI visibility

---

### FR-011: Command Bar Layout and Auto-Hide Fixes
**Priority:** High | **Complexity:** Medium | **Effort:** 3 hours

**Description:** Portrait mode command bar not centered and scrollable; auto-hides without user action.

**Current Behavior:**
- Portrait: Command bar left-aligned and horizontally scrollable
- Auto-hide triggers unexpectedly

**Expected Behavior:**
- Portrait: Command bar centered, no scrolling (all buttons visible)
- Auto-hide only on user scroll gesture (or disabled)

**Implementation:**

**Phase 1: Center Alignment (1 hour)**
- File: `BottomCommandBar.kt` (HorizontalCommandBarLayout function)
- Remove `horizontalScroll` modifier
- Replace `Row` arrangement with `Arrangement.Center`
- Ensure max 6 buttons per level (current design)

**Phase 2: Auto-Hide Logic (2 hours)**
- File: `BrowserScreen.kt`
- Review auto-hide trigger (LaunchedEffect?)
- Add setting: `commandBarAutoHide: Boolean = false` to BrowserSettings
- Conditionally enable auto-hide based on setting
- If enabled, trigger only on WebView scroll down gesture

**Acceptance Criteria:**
- [ ] Portrait: Buttons centered, no scrolling needed
- [ ] Auto-hide disabled by default
- [ ] Optional: Settings toggle for auto-hide
- [ ] Auto-hide only on scroll down (if enabled)

---

### FR-012: Light Theme Dropdown Visibility
**Priority:** Medium | **Complexity:** Low | **Effort:** 1 hour

**Description:** Light theme search engine dropdown has white text on white background.

**Current Behavior:**
- Settings > General > Search Engine dropdown
- Light theme renders white text on white card
- Text invisible

**Expected Behavior:**
- Light theme: Dark text on light background
- Proper contrast for WCAG AA compliance

**Implementation:**
- File: `SettingsScreen.kt` (SearchEngineDropdown composable)
- Use `MaterialTheme.colorScheme.onSurface` for text color
- Use `MaterialTheme.colorScheme.surface` for background
- Avoid hardcoded colors

**Acceptance Criteria:**
- [ ] Light theme: Dark text visible
- [ ] Dark theme: Light text visible (don't regress)
- [ ] Contrast ratio ≥ 4.5:1 (WCAG AA)
- [ ] Tested on both themes

---

### FR-013: Landscape Search Engine Popup Layout
**Priority:** Medium | **Complexity:** Low | **Effort:** 1 hour

**Description:** Search engine popup breaks layout in landscape mode.

**Current Behavior:**
- Dropdown extends off-screen or behind UI elements

**Expected Behavior:**
- Dropdown positioned above trigger button if space available
- Or scrollable if content exceeds screen height

**Implementation:**
- File: `SettingsScreen.kt`
- Use `DropdownMenu` with explicit `offset` parameter
- Calculate available space and position accordingly
- Add `Modifier.heightIn(max = screenHeight * 0.6f)`

**Acceptance Criteria:**
- [ ] Dropdown fully visible in landscape
- [ ] Dropdown scrollable if needed
- [ ] Dropdown doesn't overlap other UI

---

### FR-014: WebXR Settings Integration
**Priority:** Medium | **Complexity:** Low | **Effort:** 2 hours

**Description:** WebXR settings should be in main Settings, not separate AR/XR section.

**Current Behavior:**
- Separate "AR/XR" menu item in Settings sidebar
- WebXR settings isolated from related settings

**Expected Behavior:**
- WebXR settings moved to Settings > Advanced
- Remove separate AR/XR menu item
- Group with hardware acceleration and performance settings

**Implementation:**

**Phase 1: Settings Screen Refactor (1.5 hours)**
- File: `SettingsScreen.kt`
- Remove `AR/XR` navigation item from sidebar
- Add WebXR section to Advanced settings:
  - "WebXR Support" toggle
  - "Enable AR" toggle (indented, disabled if WebXR off)
  - "Enable VR" toggle (indented, disabled if WebXR off)
  - "XR Performance Mode" dropdown (High/Balanced/Battery)

**Phase 2: Navigation Cleanup (0.5 hours)**
- Remove AR/XR route from navigation graph
- Update Settings navigation destinations

**Acceptance Criteria:**
- [ ] AR/XR menu item removed from sidebar
- [ ] WebXR settings appear in Advanced section
- [ ] All WebXR toggles functional
- [ ] Performance mode dropdown works
- [ ] No navigation errors

---

### FR-015: Tab Groups Implementation
**Priority:** Medium | **Complexity:** High | **Effort:** 12 hours

**Description:** Implement tab groups feature (UI exists, no functionality).

**Current State:**
- "Groups" tab in TabSwitcherView shows "No tab groups" placeholder
- No domain model or data persistence

**Expected Behavior:**
- Users can create named tab groups
- Drag tabs into groups
- Collapse/expand groups
- Persist groups in database

**Implementation:**

**Phase 1: Domain Model (2 hours)**
- File: `common/webavanue/coredata/src/commonMain/kotlin/com/augmentalis/webavanue/domain/model/TabGroup.kt`
```kotlin
@Serializable
data class TabGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: GroupColor = GroupColor.BLUE,
    val tabIds: List<String> = emptyList(),
    val isCollapsed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class GroupColor {
    BLUE, GREEN, ORANGE, PURPLE, RED, YELLOW
}
```

**Phase 2: Database Schema (2 hours)**
- File: `BrowserDatabase.sq`
- Add `TabGroups` table
- Add `group_id` column to `Tabs` table
- Write migration

**Phase 3: Repository Layer (3 hours)**
- File: `TabGroupRepository.kt`
- CRUD operations: create, read, update, delete groups
- Assign tab to group
- Remove tab from group
- Get tabs by group

**Phase 4: ViewModel Integration (2 hours)**
- File: `TabViewModel.kt`
- Add `tabGroups: StateFlow<List<TabGroup>>`
- Methods: createGroup, renameGroup, deleteGroup, moveTabToGroup

**Phase 5: UI Implementation (3 hours)**
- File: `TabSwitcherView.kt`
- Groups tab: Show list of groups with tab counts
- Long-press menu: Create group, Rename, Delete
- Drag-and-drop: Assign tabs to groups
- Group list item: Collapse/expand, show tabs

**Acceptance Criteria:**
- [ ] User can create named groups
- [ ] User can rename groups
- [ ] User can delete groups
- [ ] User can drag tabs into groups
- [ ] Groups persist across app restarts
- [ ] Collapsed groups hide tabs
- [ ] Group colors customizable

**Out of Scope:**
- Cross-device sync (future: v2.0)
- Nested groups (future: v2.0)

---

### FR-016: File Downloads Implementation
**Priority:** Critical | **Complexity:** High | **Effort:** 10 hours

**Description:** File downloads stuck in "pending" state, no actual download occurs.

**Current State:**
- Downloads screen exists with UI
- Downloads show "0 B / 50.45 MB - Pending"
- No DownloadListener registered on WebView

**Expected Behavior:**
- Downloads start automatically when triggered
- Progress updates in real-time
- Files saved to Downloads folder
- Notifications for completed downloads
- Download manager integration

**Implementation:**

**Phase 1: DownloadListener (3 hours)**
- File: `WebViewContainer.android.kt`
- Register `DownloadListener` on WebView
- Extract filename, MIME type, content length
- Trigger system DownloadManager or custom download

**Phase 2: Download Repository (3 hours)**
- File: `DownloadRepository.kt`
- Track active downloads (in-memory + database)
- Update progress via StateFlow
- Handle pause/resume/cancel
- Save metadata to database

**Phase 3: Download Service (2 hours)**
- File: `DownloadService.kt` (Android foreground service)
- Download files in background
- Show notification with progress
- Handle network interruptions
- Resume interrupted downloads

**Phase 4: UI Integration (2 hours)**
- File: `DownloadScreen.kt`
- Real-time progress updates
- Pause/Resume/Cancel buttons
- Open downloaded file
- Delete downloaded file

**Acceptance Criteria:**
- [ ] Clicking download link starts download
- [ ] Progress updates every second
- [ ] Download continues in background
- [ ] Notification shown during download
- [ ] Downloaded files accessible from Downloads screen
- [ ] User can pause/resume/cancel downloads
- [ ] User can open downloaded files
- [ ] Downloads survive app restart (resume)
- [ ] Tested with 50MB file on HMT-1

**Technical Decisions:**
- Use Android DownloadManager for simplicity (no custom network code)
- Store download metadata in database (track history)
- Use WorkManager for reliability (retries, doze mode)

---

### FR-017: Default Homepage Loading Fix
**Priority:** Low | **Complexity:** Low | **Effort:** 1 hour

**Description:** Changing default homepage in Settings doesn't load new homepage on restart.

**Current Behavior:**
- User changes homepage in Settings > General
- Restart app: Google.com loads instead of new homepage

**Root Cause:**
- BrowserViewModel hardcodes initial URL
- Doesn't read `settings.homePage` on startup

**Expected Behavior:**
- App reads `settings.homePage` on startup
- First tab loads configured homepage

**Implementation:**
- File: `BrowserViewModel.kt`
- In `init {}` block, read settings and set initial URL
```kotlin
viewModelScope.launch {
    val settings = settingsRepository.getSettings().first()
    loadUrl(settings.homePage)
}
```

**Acceptance Criteria:**
- [ ] Changing homepage in Settings persists
- [ ] App restart loads new homepage
- [ ] Default homepage is Google.com (if not changed)

---

### FR-018: Remove Redundant Command Bar Buttons
**Priority:** High | **Complexity:** Low | **Effort:** 1 hour

**Description:** Command bar has Prev/Next/Reload buttons duplicating AddressBar buttons.

**Current Behavior:**
- Command bar MAIN level: Back, Home, Add, Scroll, Page, Menu
- Command bar PAGE level: Close, Prev, Next, Reload, Desktop, Fav
- AddressBar also has: Back, Forward, Reload buttons

**Expected Behavior:**
- Remove Prev/Next/Reload from command bar PAGE level
- Keep only: Close, Desktop Mode, Add to Favorites (if not headless)
- Justification: Reduces clutter, AddressBar is primary navigation

**Implementation:**
- File: `BottomCommandBar.kt` (PAGE level layout functions)
- Remove `CommandButton(icon = Icons.Default.ArrowBack, ...)`
- Remove `CommandButton(icon = Icons.Default.ArrowForward, ...)`
- Remove `CommandButton(icon = Icons.Default.Refresh, ...)`
- Keep Close, Desktop, Favorite buttons

**Acceptance Criteria:**
- [ ] PAGE level has 3 buttons (not 6)
- [ ] Back/Forward/Reload only in AddressBar
- [ ] Desktop mode toggle still accessible
- [ ] Favorite/History still accessible

---

### FR-019: Headless Mode Intent API (External App Integration)
**Priority:** Critical | **Complexity:** High | **Effort:** 6 hours

**Description:** Enable external apps to launch WebAvanue in headless mode with programmatic control.

**Use Case:**
AR/XR applications need to embed a web browser without UI chrome, controlling navigation from their own UI or voice commands.

**Expected Behavior:**
External apps can:
1. Launch WebAvanue in headless mode via Intent
2. Specify target URL
3. Control command bar visibility
4. Enable/disable user navigation
5. Send navigation commands (back, forward, reload, close)
6. Query browser status (URL, loading state)

**Implementation:**

**Phase 1: Intent Extras Definition (1 hour)**
- File: `android/apps/webavanue/src/main/kotlin/com/augmentalis/Avanues/web/BrowserActivity.kt`

**Intent Extras:**
```kotlin
companion object {
    const val EXTRA_HEADLESS_MODE = "com.augmentalis.webavanue.HEADLESS_MODE"
    const val EXTRA_URL = "com.augmentalis.webavanue.URL"
    const val EXTRA_SHOW_COMMAND_BAR = "com.augmentalis.webavanue.SHOW_COMMAND_BAR"
    const val EXTRA_ALLOW_USER_NAVIGATION = "com.augmentalis.webavanue.ALLOW_USER_NAVIGATION"
    const val EXTRA_CALLER_PACKAGE = "com.augmentalis.webavanue.CALLER_PACKAGE"
}
```

**Example Intent (from external app):**
```kotlin
val intent = Intent().apply {
    setClassName("com.augmentalis.Avanues.web",
                 "com.augmentalis.Avanues.web.BrowserActivity")
    putExtra(EXTRA_HEADLESS_MODE, true)
    putExtra(EXTRA_URL, "https://example.com")
    putExtra(EXTRA_SHOW_COMMAND_BAR, false)
    putExtra(EXTRA_ALLOW_USER_NAVIGATION, false)
    putExtra(EXTRA_CALLER_PACKAGE, packageName)
}
startActivity(intent)
```

**Phase 2: Deep Link Support (1 hour)**
- File: `AndroidManifest.xml`

**Deep Link Scheme:**
```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />

    <data android:scheme="webavanue" android:host="browser" />
</intent-filter>
```

**Example Deep Link:**
```
webavanue://browser?url=https://example.com&headless=true&commandBar=false&navigation=false
```

**Phase 3: Intent Processing (2 hours)**
- File: `BrowserActivity.kt` and `BrowserViewModel.kt`

```kotlin
// BrowserActivity.onCreate()
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Process launch intent
    val headlessMode = intent.getBooleanExtra(EXTRA_HEADLESS_MODE, false)
    val url = intent.getStringExtra(EXTRA_URL)
    val showCommandBar = intent.getBooleanExtra(EXTRA_SHOW_COMMAND_BAR, true)
    val allowNavigation = intent.getBooleanExtra(EXTRA_ALLOW_USER_NAVIGATION, true)
    val callerPackage = intent.getStringExtra(EXTRA_CALLER_PACKAGE)

    // Validate caller (security check)
    if (headlessMode && !isAuthorizedCaller(callerPackage)) {
        finish()
        return
    }

    // Pass to ViewModel
    viewModel.setHeadlessMode(headlessMode, showCommandBar, allowNavigation)
    url?.let { viewModel.loadUrl(it) }
}

private fun isAuthorizedCaller(packageName: String?): Boolean {
    // Option 1: Check signature (same developer)
    // Option 2: Whitelist of authorized packages
    // Option 3: User approval dialog (first time)
    return packageName?.let {
        // Check if signature matches or is whitelisted
        packageManager.getPackageInfo(it, PackageManager.GET_SIGNATURES)
        // Validate signature...
        true
    } ?: false
}
```

**Phase 4: ViewModel State Management (2 hours)**
- File: `BrowserViewModel.kt`

```kotlin
data class HeadlessConfig(
    val enabled: Boolean = false,
    val showCommandBar: Boolean = true,
    val allowUserNavigation: Boolean = true,
    val callerPackage: String? = null
)

private val _headlessConfig = MutableStateFlow(HeadlessConfig())
val headlessConfig: StateFlow<HeadlessConfig> = _headlessConfig.asStateFlow()

fun setHeadlessMode(
    enabled: Boolean,
    showCommandBar: Boolean = true,
    allowUserNavigation: Boolean = true,
    callerPackage: String? = null
) {
    _headlessConfig.value = HeadlessConfig(
        enabled = enabled,
        showCommandBar = showCommandBar,
        allowUserNavigation = allowUserNavigation,
        callerPackage = callerPackage
    )
}
```

**Acceptance Criteria:**
- [ ] External app can launch WebAvanue in headless mode via explicit Intent
- [ ] External app can launch via deep link `webavanue://browser?...`
- [ ] URL parameter loads specified website
- [ ] `showCommandBar=false` hides command bar entirely
- [ ] `allowUserNavigation=false` disables address bar and navigation buttons
- [ ] Caller validation prevents unauthorized access
- [ ] BrowserScreen respects headless configuration
- [ ] Tested with sample AR/XR app on HMT-1

**Security Considerations:**
- Signature validation for sensitive operations
- User approval dialog on first headless launch (per app)
- Persistent whitelist of authorized packages
- Prevent phishing: Show small "Powered by WebAvanue" badge in headless mode

---

### FR-020: Browser Control API (Broadcast Intents)
**Priority:** Critical | **Complexity:** Medium | **Effort:** 4 hours

**Description:** Enable external apps to control WebAvanue navigation and query status via broadcast intents.

**Use Case:**
AR/XR app launches WebAvanue in headless mode, then sends navigation commands from its own UI (e.g., hand gestures, voice commands).

**Expected Behavior:**
External apps can send control commands:
- Navigate to URL
- Go back
- Go forward
- Reload page
- Close browser
- Query current URL and loading state

**Implementation:**

**Phase 1: Broadcast Receiver (2 hours)**
- File: `android/apps/webavanue/src/main/kotlin/com/augmentalis/Avanues/web/BrowserControlReceiver.kt`

```kotlin
class BrowserControlReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_NAVIGATE = "com.augmentalis.webavanue.action.NAVIGATE"
        const val ACTION_GO_BACK = "com.augmentalis.webavanue.action.GO_BACK"
        const val ACTION_GO_FORWARD = "com.augmentalis.webavanue.action.GO_FORWARD"
        const val ACTION_RELOAD = "com.augmentalis.webavanue.action.RELOAD"
        const val ACTION_CLOSE = "com.augmentalis.webavanue.action.CLOSE"
        const val ACTION_QUERY_STATUS = "com.augmentalis.webavanue.action.QUERY_STATUS"

        const val EXTRA_URL = "url"
        const val EXTRA_CALLER_PACKAGE = "caller_package"

        // Response extras (for query status)
        const val EXTRA_CURRENT_URL = "current_url"
        const val EXTRA_IS_LOADING = "is_loading"
        const val EXTRA_CAN_GO_BACK = "can_go_back"
        const val EXTRA_CAN_GO_FORWARD = "can_go_forward"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val callerPackage = intent.getStringExtra(EXTRA_CALLER_PACKAGE)

        // Validate caller
        if (!isAuthorizedCaller(context, callerPackage)) {
            return
        }

        when (intent.action) {
            ACTION_NAVIGATE -> {
                val url = intent.getStringExtra(EXTRA_URL) ?: return
                BrowserControlService.navigate(context, url)
            }
            ACTION_GO_BACK -> BrowserControlService.goBack(context)
            ACTION_GO_FORWARD -> BrowserControlService.goForward(context)
            ACTION_RELOAD -> BrowserControlService.reload(context)
            ACTION_CLOSE -> BrowserControlService.close(context)
            ACTION_QUERY_STATUS -> {
                val status = BrowserControlService.getStatus(context)
                sendStatusResponse(context, callerPackage, status)
            }
        }
    }

    private fun sendStatusResponse(
        context: Context,
        callerPackage: String?,
        status: BrowserStatus
    ) {
        val responseIntent = Intent("com.augmentalis.webavanue.response.STATUS").apply {
            setPackage(callerPackage)
            putExtra(EXTRA_CURRENT_URL, status.currentUrl)
            putExtra(EXTRA_IS_LOADING, status.isLoading)
            putExtra(EXTRA_CAN_GO_BACK, status.canGoBack)
            putExtra(EXTRA_CAN_GO_FORWARD, status.canGoForward)
        }
        context.sendBroadcast(responseIntent)
    }
}

data class BrowserStatus(
    val currentUrl: String,
    val isLoading: Boolean,
    val canGoBack: Boolean,
    val canGoForward: Boolean
)
```

**Phase 2: Control Service (1.5 hours)**
- File: `BrowserControlService.kt`

```kotlin
object BrowserControlService {
    private var viewModel: BrowserViewModel? = null

    fun register(vm: BrowserViewModel) {
        viewModel = vm
    }

    fun unregister() {
        viewModel = null
    }

    fun navigate(context: Context, url: String) {
        viewModel?.loadUrl(url)
    }

    fun goBack(context: Context) {
        viewModel?.goBack()
    }

    fun goForward(context: Context) {
        viewModel?.goForward()
    }

    fun reload(context: Context) {
        viewModel?.reload()
    }

    fun close(context: Context) {
        (context as? Activity)?.finish()
    }

    fun getStatus(context: Context): BrowserStatus {
        return viewModel?.let { vm ->
            BrowserStatus(
                currentUrl = vm.currentUrl.value,
                isLoading = vm.isLoading.value,
                canGoBack = vm.canGoBack.value,
                canGoForward = vm.canGoForward.value
            )
        } ?: BrowserStatus("", false, false, false)
    }
}
```

**Phase 3: Manifest Registration (0.5 hours)**
- File: `AndroidManifest.xml`

```xml
<receiver
    android:name=".BrowserControlReceiver"
    android:exported="true"
    android:permission="android.permission.INTERNET">
    <intent-filter>
        <action android:name="com.augmentalis.webavanue.action.NAVIGATE" />
        <action android:name="com.augmentalis.webavanue.action.GO_BACK" />
        <action android:name="com.augmentalis.webavanue.action.GO_FORWARD" />
        <action android:name="com.augmentalis.webavanue.action.RELOAD" />
        <action android:name="com.augmentalis.webavanue.action.CLOSE" />
        <action android:name="com.augmentalis.webavanue.action.QUERY_STATUS" />
    </intent-filter>
</receiver>
```

**Example Usage (from external app):**
```kotlin
// Navigate to URL
val intent = Intent("com.augmentalis.webavanue.action.NAVIGATE").apply {
    setPackage("com.augmentalis.Avanues.web")
    putExtra("url", "https://example.com")
    putExtra("caller_package", packageName)
}
sendBroadcast(intent)

// Go back
sendBroadcast(Intent("com.augmentalis.webavanue.action.GO_BACK").apply {
    setPackage("com.augmentalis.Avanues.web")
    putExtra("caller_package", packageName)
})

// Query status
registerReceiver(object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val url = intent.getStringExtra("current_url")
        val isLoading = intent.getBooleanExtra("is_loading", false)
        // Handle status...
    }
}, IntentFilter("com.augmentalis.webavanue.response.STATUS"))

sendBroadcast(Intent("com.augmentalis.webavanue.action.QUERY_STATUS").apply {
    setPackage("com.augmentalis.Avanues.web")
    putExtra("caller_package", packageName)
})
```

**Acceptance Criteria:**
- [ ] External app can send NAVIGATE broadcast, WebAvanue loads URL
- [ ] GO_BACK, GO_FORWARD, RELOAD broadcasts work
- [ ] CLOSE broadcast closes WebAvanue activity
- [ ] QUERY_STATUS returns current browser state
- [ ] Unauthorized apps cannot send commands (signature check)
- [ ] Commands only work when WebAvanue is in foreground
- [ ] Tested with sample AR/XR app

**Security Considerations:**
- Signature validation in `isAuthorizedCaller()`
- Require same signature as WebAvanue or user approval
- Log all control commands for audit
- Rate limiting to prevent abuse (max 10 commands/second)

---

### FR-021: Developer SDK Library (Optional)
**Priority:** Medium | **Complexity:** Low | **Effort:** 3 hours

**Description:** Create a helper library to simplify WebAvanue integration for external developers.

**Use Case:**
AR/XR developers shouldn't need to construct raw Intents. Provide a clean Kotlin API.

**Implementation:**

**Phase 1: SDK Module (2 hours)**
- Create: `android/libs/webavanue-sdk/`
- File: `WebAvanueController.kt`

```kotlin
class WebAvanueController(private val context: Context) {

    /**
     * Launch WebAvanue in headless mode
     * @param url Initial URL to load
     * @param showCommandBar Show floating command bar (default: false)
     * @param allowNavigation Allow user to navigate (default: false)
     */
    fun launchHeadless(
        url: String,
        showCommandBar: Boolean = false,
        allowNavigation: Boolean = false
    ) {
        val intent = Intent().apply {
            setClassName(PACKAGE_NAME, ACTIVITY_NAME)
            putExtra(EXTRA_HEADLESS_MODE, true)
            putExtra(EXTRA_URL, url)
            putExtra(EXTRA_SHOW_COMMAND_BAR, showCommandBar)
            putExtra(EXTRA_ALLOW_USER_NAVIGATION, allowNavigation)
            putExtra(EXTRA_CALLER_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Navigate to a new URL
     */
    fun navigate(url: String) {
        sendCommand(ACTION_NAVIGATE) {
            putExtra(EXTRA_URL, url)
        }
    }

    /**
     * Go back in history
     */
    fun goBack() {
        sendCommand(ACTION_GO_BACK)
    }

    /**
     * Go forward in history
     */
    fun goForward() {
        sendCommand(ACTION_GO_FORWARD)
    }

    /**
     * Reload current page
     */
    fun reload() {
        sendCommand(ACTION_RELOAD)
    }

    /**
     * Close WebAvanue
     */
    fun close() {
        sendCommand(ACTION_CLOSE)
    }

    /**
     * Query current browser status
     * @param callback Receives browser status asynchronously
     */
    fun queryStatus(callback: (BrowserStatus) -> Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val status = BrowserStatus(
                    currentUrl = intent.getStringExtra(EXTRA_CURRENT_URL) ?: "",
                    isLoading = intent.getBooleanExtra(EXTRA_IS_LOADING, false),
                    canGoBack = intent.getBooleanExtra(EXTRA_CAN_GO_BACK, false),
                    canGoForward = intent.getBooleanExtra(EXTRA_CAN_GO_FORWARD, false)
                )
                callback(status)
                context.unregisterReceiver(this)
            }
        }

        context.registerReceiver(
            receiver,
            IntentFilter("com.augmentalis.webavanue.response.STATUS")
        )

        sendCommand(ACTION_QUERY_STATUS)
    }

    private fun sendCommand(action: String, extras: Intent.() -> Unit = {}) {
        val intent = Intent(action).apply {
            setPackage(PACKAGE_NAME)
            putExtra(EXTRA_CALLER_PACKAGE, context.packageName)
            extras()
        }
        context.sendBroadcast(intent)
    }

    companion object {
        private const val PACKAGE_NAME = "com.augmentalis.Avanues.web"
        private const val ACTIVITY_NAME = "com.augmentalis.Avanues.web.BrowserActivity"

        private const val EXTRA_HEADLESS_MODE = "com.augmentalis.webavanue.HEADLESS_MODE"
        private const val EXTRA_URL = "com.augmentalis.webavanue.URL"
        private const val EXTRA_SHOW_COMMAND_BAR = "com.augmentalis.webavanue.SHOW_COMMAND_BAR"
        private const val EXTRA_ALLOW_USER_NAVIGATION = "com.augmentalis.webavanue.ALLOW_USER_NAVIGATION"
        private const val EXTRA_CALLER_PACKAGE = "com.augmentalis.webavanue.CALLER_PACKAGE"

        private const val ACTION_NAVIGATE = "com.augmentalis.webavanue.action.NAVIGATE"
        private const val ACTION_GO_BACK = "com.augmentalis.webavanue.action.GO_BACK"
        private const val ACTION_GO_FORWARD = "com.augmentalis.webavanue.action.GO_FORWARD"
        private const val ACTION_RELOAD = "com.augmentalis.webavanue.action.RELOAD"
        private const val ACTION_CLOSE = "com.augmentalis.webavanue.action.CLOSE"
        private const val ACTION_QUERY_STATUS = "com.augmentalis.webavanue.action.QUERY_STATUS"

        private const val EXTRA_CURRENT_URL = "current_url"
        private const val EXTRA_IS_LOADING = "is_loading"
        private const val EXTRA_CAN_GO_BACK = "can_go_back"
        private const val EXTRA_CAN_GO_FORWARD = "can_go_forward"
    }
}

data class BrowserStatus(
    val currentUrl: String,
    val isLoading: Boolean,
    val canGoBack: Boolean,
    val canGoForward: Boolean
)
```

**Phase 2: Example App (1 hour)**
- Create sample AR app demonstrating SDK usage

```kotlin
class MyARActivity : AppCompatActivity() {
    private val browser = WebAvanueController(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Launch WebAvanue in headless mode
        browser.launchHeadless(
            url = "https://threejs.org/examples/webxr_vr_ballshooter.html",
            showCommandBar = false,
            allowNavigation = false
        )

        // Control from AR gestures
        onSwipeLeft = { browser.goBack() }
        onSwipeRight = { browser.goForward() }
        onDoubleTap = { browser.reload() }

        // Query status
        browser.queryStatus { status ->
            println("Current URL: ${status.currentUrl}")
        }
    }
}
```

**Acceptance Criteria:**
- [ ] SDK library compiles and packages as AAR
- [ ] Developers can add dependency: `implementation("com.augmentalis:webavanue-sdk:1.0.0")`
- [ ] Clean Kotlin API, no raw Intent manipulation
- [ ] Example app demonstrates all SDK methods
- [ ] Published to Maven Central or GitHub Packages

**Out of Scope:**
- iOS SDK (future)
- JavaScript bridge for web-to-native communication (future: v2.0)

---

### FR-022: JavaScript Execution API
**Priority:** Critical | **Complexity:** High | **Effort:** 4 hours

**Description:** Enable external apps to inject and execute JavaScript in the WebView, with results returned asynchronously.

**Use Cases:**
- DOM manipulation for AR overlays (highlight elements, inject markers)
- Data extraction (scrape product details, extract coordinates)
- Form auto-fill from AR voice input
- WebXR anchor synchronization (read DOM positions)

**Implementation:**

**Phase 1: JavaScript Bridge (2 hours)**
- File: `BrowserControlService.kt`

```kotlin
fun executeJavaScript(
    script: String,
    callback: ((String) -> Unit)? = null
) {
    viewModel?.executeJavaScript(script, callback)
}
```

- File: `BrowserViewModel.kt`

```kotlin
fun executeJavaScript(script: String, callback: ((String) -> Unit)?) {
    webViewContainer?.evaluateJavascript(script) { result ->
        callback?.invoke(result)
    }
}
```

**Phase 2: Broadcast API Integration (1.5 hours)**
- File: `BrowserControlReceiver.kt`

```kotlin
companion object {
    const val ACTION_EXECUTE_JS = "com.augmentalis.webavanue.action.EXECUTE_JS"
    const val EXTRA_SCRIPT = "script"
    const val EXTRA_REQUEST_ID = "request_id"

    const val ACTION_JS_RESULT = "com.augmentalis.webavanue.response.JS_RESULT"
    const val EXTRA_RESULT = "result"
}

override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
        ACTION_EXECUTE_JS -> {
            val script = intent.getStringExtra(EXTRA_SCRIPT) ?: return
            val requestId = intent.getStringExtra(EXTRA_REQUEST_ID)

            BrowserControlService.executeJavaScript(script) { result ->
                sendJsResult(context, callerPackage, requestId, result)
            }
        }
    }
}
```

**Phase 3: SDK Method (0.5 hours)**
- File: `WebAvanueController.kt`

```kotlin
/**
 * Execute JavaScript in the current page
 * @param script JavaScript code to execute
 * @param callback Receives execution result (or null if no return value)
 */
fun executeJavaScript(script: String, callback: (String?) -> Unit) {
    val requestId = UUID.randomUUID().toString()

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val result = intent.getStringExtra(EXTRA_RESULT)
            callback(result)
            context.unregisterReceiver(this)
        }
    }

    context.registerReceiver(
        receiver,
        IntentFilter("com.augmentalis.webavanue.response.JS_RESULT")
    )

    sendCommand(ACTION_EXECUTE_JS) {
        putExtra(EXTRA_SCRIPT, script)
        putExtra(EXTRA_REQUEST_ID, requestId)
    }
}
```

**Example Usage:**
```kotlin
// Count images on page
browser.executeJavaScript("document.querySelectorAll('img').length") { result ->
    println("Found $result images")
}

// Extract product price
browser.executeJavaScript("""
    document.querySelector('.price')?.textContent
""") { price ->
    displayInAR(price)
}

// Inject AR marker
browser.executeJavaScript("""
    const marker = document.createElement('div');
    marker.id = 'ar-anchor';
    marker.style.position = 'absolute';
    marker.style.top = '100px';
    marker.style.left = '100px';
    document.body.appendChild(marker);
    marker.getBoundingClientRect().toJSON();
""") { rect ->
    // Parse rect and create AR anchor
}
```

**Acceptance Criteria:**
- [ ] External app can execute arbitrary JavaScript
- [ ] Results returned within 500ms (timeout)
- [ ] Null result returned if script has no return value
- [ ] Sandboxing prevents script from closing WebAvanue or navigating away
- [ ] Scripts execute in page context (can access DOM)
- [ ] Multiple concurrent requests supported (via request IDs)
- [ ] Tested with 100+ line scripts
- [ ] Error handling: Script errors return error message, not crash

**Security Considerations:**
- Caller validation required (same as FR-020)
- Scripts cannot call Android APIs directly
- No access to WebAvanue internal state
- Rate limiting: max 20 JS executions/second per app

**Fallback:** If WebView doesn't support `evaluateJavascript` (API < 19), show error dialog (see FR-029)

---

### FR-023: Page Lifecycle Callbacks
**Priority:** Critical | **Complexity:** Low | **Effort:** 2 hours

**Description:** Enable external apps to receive callbacks for page load events: started, finished, progress, errors.

**Use Cases:**
- Show loading indicators in AR UI
- Track navigation for analytics
- Trigger AR actions when page fully loads
- Handle network errors gracefully

**Implementation:**

**Phase 1: Listener Interface (1 hour)**
- File: `BrowserControlService.kt`

```kotlin
interface PageLifecycleListener {
    fun onPageStarted(url: String)
    fun onPageFinished(url: String)
    fun onProgressChanged(progress: Int) // 0-100
    fun onPageError(errorCode: Int, description: String, failingUrl: String)
}

private val listeners = mutableMapOf<String, PageLifecycleListener>()

fun registerPageListener(packageName: String, listener: PageLifecycleListener) {
    listeners[packageName] = listener
}

fun unregisterPageListener(packageName: String) {
    listeners.remove(packageName)
}

// Called from WebViewClient
fun notifyPageStarted(url: String) {
    listeners.values.forEach { it.onPageStarted(url) }
}
```

**Phase 2: Broadcast Integration (0.5 hours)**
- File: `BrowserControlReceiver.kt`

```kotlin
companion object {
    const val ACTION_REGISTER_LISTENER = "com.augmentalis.webavanue.action.REGISTER_LISTENER"
    const val ACTION_UNREGISTER_LISTENER = "com.augmentalis.webavanue.action.UNREGISTER_LISTENER"

    // Broadcast to external apps
    const val EVENT_PAGE_STARTED = "com.augmentalis.webavanue.event.PAGE_STARTED"
    const val EVENT_PAGE_FINISHED = "com.augmentalis.webavanue.event.PAGE_FINISHED"
    const val EVENT_PROGRESS_CHANGED = "com.augmentalis.webavanue.event.PROGRESS_CHANGED"
    const val EVENT_PAGE_ERROR = "com.augmentalis.webavanue.event.PAGE_ERROR"

    const val EXTRA_URL = "url"
    const val EXTRA_PROGRESS = "progress"
    const val EXTRA_ERROR_CODE = "error_code"
    const val EXTRA_ERROR_DESC = "error_description"
}
```

**Phase 3: SDK Methods (0.5 hours)**
- File: `WebAvanueController.kt`

```kotlin
interface PageListener {
    fun onPageStarted(url: String) {}
    fun onPageFinished(url: String) {}
    fun onProgressChanged(progress: Int) {}
    fun onPageError(errorCode: Int, description: String, failingUrl: String) {}
}

private var pageListener: PageListener? = null
private var eventReceiver: BroadcastReceiver? = null

fun setPageListener(listener: PageListener) {
    pageListener = listener

    // Register broadcast receiver
    eventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                EVENT_PAGE_STARTED -> {
                    listener.onPageStarted(intent.getStringExtra(EXTRA_URL) ?: "")
                }
                EVENT_PAGE_FINISHED -> {
                    listener.onPageFinished(intent.getStringExtra(EXTRA_URL) ?: "")
                }
                EVENT_PROGRESS_CHANGED -> {
                    listener.onProgressChanged(intent.getIntExtra(EXTRA_PROGRESS, 0))
                }
                EVENT_PAGE_ERROR -> {
                    listener.onPageError(
                        intent.getIntExtra(EXTRA_ERROR_CODE, -1),
                        intent.getStringExtra(EXTRA_ERROR_DESC) ?: "",
                        intent.getStringExtra(EXTRA_URL) ?: ""
                    )
                }
            }
        }
    }

    context.registerReceiver(eventReceiver, IntentFilter().apply {
        addAction(EVENT_PAGE_STARTED)
        addAction(EVENT_PAGE_FINISHED)
        addAction(EVENT_PROGRESS_CHANGED)
        addAction(EVENT_PAGE_ERROR)
    })

    // Tell WebAvanue to start broadcasting
    sendCommand(ACTION_REGISTER_LISTENER)
}

fun removePageListener() {
    sendCommand(ACTION_UNREGISTER_LISTENER)
    eventReceiver?.let { context.unregisterReceiver(it) }
    eventReceiver = null
    pageListener = null
}
```

**Example Usage:**
```kotlin
browser.setPageListener(object : PageListener {
    override fun onPageStarted(url: String) {
        showARLoadingIndicator()
        log("Loading: $url")
    }

    override fun onPageFinished(url: String) {
        hideARLoadingIndicator()
        enableARInteractions()
    }

    override fun onProgressChanged(progress: Int) {
        updateARProgressBar(progress)
    }

    override fun onPageError(errorCode: Int, description: String, failingUrl: String) {
        showARErrorOverlay("Failed to load: $description")
    }
})
```

**Acceptance Criteria:**
- [ ] onPageStarted called when navigation begins
- [ ] onPageFinished called when page fully loaded
- [ ] onProgressChanged called every 5% progress increment
- [ ] onPageError called on network/HTTP errors
- [ ] Multiple apps can register listeners simultaneously
- [ ] Listeners auto-unregister when app closes
- [ ] No memory leaks (WeakReference if needed)

---

### FR-024: Screenshot/Capture API
**Priority:** High | **Complexity:** Medium | **Effort:** 3 hours

**Description:** Enable external apps to capture the current webpage as a Bitmap for AR overlays and thumbnails.

**Use Cases:**
- Pin webpage as texture on AR plane
- Generate thumbnails for tab switcher
- Create spatial bookmarks in AR space
- Record browsing session as AR video overlay

**Implementation:**

**Phase 1: WebView Capture (1.5 hours)**
- File: `BrowserControlService.kt`

```kotlin
fun captureScreenshot(callback: (Bitmap?) -> Unit) {
    viewModel?.captureScreenshot(callback)
}
```

- File: `BrowserViewModel.kt`

```kotlin
fun captureScreenshot(callback: (Bitmap?) -> Unit) {
    webViewContainer?.let { webView ->
        try {
            // Capture entire WebView (not just visible area)
            val bitmap = Bitmap.createBitmap(
                webView.width,
                webView.height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            webView.draw(canvas)
            callback(bitmap)
        } catch (e: Exception) {
            Log.e("WebAvanue", "Screenshot failed", e)
            callback(null)
        }
    } ?: callback(null)
}
```

**Phase 2: File Sharing (1 hour)**
- Store bitmap in cache, share via FileProvider
- File: `BrowserControlReceiver.kt`

```kotlin
companion object {
    const val ACTION_CAPTURE_SCREENSHOT = "com.augmentalis.webavanue.action.CAPTURE_SCREENSHOT"
    const val ACTION_SCREENSHOT_READY = "com.augmentalis.webavanue.response.SCREENSHOT_READY"
    const val EXTRA_SCREENSHOT_URI = "screenshot_uri"
    const val EXTRA_WIDTH = "width"
    const val EXTRA_HEIGHT = "height"
}

override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
        ACTION_CAPTURE_SCREENSHOT -> {
            BrowserControlService.captureScreenshot { bitmap ->
                bitmap?.let {
                    // Save to cache
                    val file = File(context.cacheDir, "screenshots/${UUID.randomUUID()}.png")
                    file.parentFile?.mkdirs()
                    file.outputStream().use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }

                    // Share via FileProvider
                    val uri = FileProvider.getUriForFile(
                        context,
                        "com.augmentalis.webavanue.fileprovider",
                        file
                    )

                    sendScreenshotReady(context, callerPackage, uri, bitmap.width, bitmap.height)
                }
            }
        }
    }
}
```

**Phase 3: SDK Method (0.5 hours)**
- File: `WebAvanueController.kt`

```kotlin
/**
 * Capture current webpage as bitmap
 * @param callback Receives bitmap (or null on error)
 */
fun captureScreenshot(callback: (Bitmap?) -> Unit) {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val uriString = intent.getStringExtra(EXTRA_SCREENSHOT_URI)
            if (uriString != null) {
                val uri = Uri.parse(uriString)
                try {
                    val bitmap = context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it)
                    }
                    callback(bitmap)
                } catch (e: Exception) {
                    callback(null)
                }
            } else {
                callback(null)
            }
            context.unregisterReceiver(this)
        }
    }

    context.registerReceiver(
        receiver,
        IntentFilter(ACTION_SCREENSHOT_READY)
    )

    sendCommand(ACTION_CAPTURE_SCREENSHOT)
}
```

**Example Usage:**
```kotlin
browser.captureScreenshot { bitmap ->
    bitmap?.let {
        // Apply to AR plane
        arPlane.setTexture(it)

        // Or save to gallery
        MediaStore.Images.Media.insertImage(
            contentResolver,
            it,
            "WebAvanue Screenshot",
            "Captured from AR browser"
        )
    }
}
```

**Acceptance Criteria:**
- [ ] Captures full WebView content (not just visible area)
- [ ] Returns null if capture fails (memory issues, etc.)
- [ ] Bitmap dimensions match WebView size
- [ ] PNG format with transparency support
- [ ] Files auto-deleted after 1 hour (cache cleanup)
- [ ] Works with hardware acceleration enabled
- [ ] Tested with 4K resolution pages

**Performance:**
- Capture should complete within 1 second
- Bitmap compressed to reduce memory (max 4096x4096)
- FileProvider prevents direct file access (security)

**Fallback:** If WebView cannot be captured, show error dialog (see FR-029)

---

### FR-025: WebXR Session Detection
**Priority:** High | **Complexity:** Medium | **Effort:** 2 hours

**Description:** Detect when WebXR immersive sessions start/end, allowing coordination between native AR and web-based AR.

**Use Cases:**
- Pause native AR when web AR session starts
- Show notification: "WebXR session active"
- Switch camera feed to WebView when in immersive mode
- Log WebXR usage for analytics

**Implementation:**

**Phase 1: JavaScript Injection (1 hour)**
- File: `WebViewContainer.android.kt`

```kotlin
private fun injectWebXRDetector() {
    webView?.evaluateJavascript("""
        (function() {
            // Intercept XRSession.requestAnimationFrame
            if ('xr' in navigator) {
                navigator.xr.requestSession = new Proxy(navigator.xr.requestSession, {
                    apply: function(target, thisArg, args) {
                        // Notify native
                        window.AndroidBridge.onWebXRSessionStart(args[0]);

                        return target.apply(thisArg, args).then(session => {
                            session.addEventListener('end', () => {
                                window.AndroidBridge.onWebXRSessionEnd();
                            });
                            return session;
                        });
                    }
                });
            }
        })();
    """, null)
}

@JavascriptInterface
class AndroidBridge {
    @JavascriptInterface
    fun onWebXRSessionStart(mode: String) {
        // mode: "immersive-ar", "immersive-vr", or "inline"
        handler.post {
            onWebXRStateChange?.invoke(true, mode)
        }
    }

    @JavascriptInterface
    fun onWebXRSessionEnd() {
        handler.post {
            onWebXRStateChange?.invoke(false, "")
        }
    }
}
```

**Phase 2: Broadcast Integration (0.5 hours)**
- File: `BrowserControlReceiver.kt`

```kotlin
companion object {
    const val EVENT_WEBXR_SESSION_START = "com.augmentalis.webavanue.event.WEBXR_START"
    const val EVENT_WEBXR_SESSION_END = "com.augmentalis.webavanue.event.WEBXR_END"
    const val EXTRA_XR_MODE = "xr_mode" // "immersive-ar", "immersive-vr", "inline"
}
```

**Phase 3: SDK Method (0.5 hours)**
- File: `WebAvanueController.kt`

```kotlin
interface WebXRListener {
    fun onSessionStarted(mode: String) // "immersive-ar", "immersive-vr", "inline"
    fun onSessionEnded()
}

fun setWebXRListener(listener: WebXRListener) {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                EVENT_WEBXR_SESSION_START -> {
                    val mode = intent.getStringExtra(EXTRA_XR_MODE) ?: "inline"
                    listener.onSessionStarted(mode)
                }
                EVENT_WEBXR_SESSION_END -> {
                    listener.onSessionEnded()
                }
            }
        }
    }

    context.registerReceiver(receiver, IntentFilter().apply {
        addAction(EVENT_WEBXR_SESSION_START)
        addAction(EVENT_WEBXR_SESSION_END)
    })
}
```

**Example Usage:**
```kotlin
browser.setWebXRListener(object : WebXRListener {
    override fun onSessionStarted(mode: String) {
        when (mode) {
            "immersive-ar" -> {
                pauseNativeAR()
                showNotification("Web AR active")
            }
            "immersive-vr" -> {
                hideUI()
                enterFullscreen()
            }
        }
    }

    override fun onSessionEnded() {
        resumeNativeAR()
        showUI()
    }
})
```

**Acceptance Criteria:**
- [ ] Detects immersive-ar session start
- [ ] Detects immersive-vr session start
- [ ] Detects session end
- [ ] Works with Three.js WebXR examples
- [ ] Works with Babylon.js WebXR examples
- [ ] No false positives (inline mode ignored by default)
- [ ] Listener unregisters automatically on app close

**Fallback:** If WebXR API not available (old WebView), never trigger events (graceful degradation)

---

### FR-026: Zoom Control API
**Priority:** Medium | **Complexity:** Low | **Effort:** 1 hour

**Description:** Enable external apps to control WebView zoom programmatically.

**Use Cases:**
- Pinch gesture in AR space controls zoom
- Voice command: "Zoom in 150%"
- Accessibility: Large text for AR headsets
- Reset zoom after page load

**Implementation:**

**Phase 1: ViewModel Methods (0.5 hours)**
- File: `BrowserViewModel.kt`

```kotlin
fun setZoomLevel(percent: Int) {
    val scale = percent / 100f
    webViewContainer?.setInitialScale((scale * 100).toInt())
}

fun zoomIn() {
    webViewContainer?.zoomIn()
}

fun zoomOut() {
    webViewContainer?.zoomOut()
}

fun getZoomLevel(): Int {
    return (webViewContainer?.scale ?: 1f * 100).toInt()
}
```

**Phase 2: Broadcast API (0.3 hours)**
- File: `BrowserControlReceiver.kt`

```kotlin
companion object {
    const val ACTION_SET_ZOOM = "com.augmentalis.webavanue.action.SET_ZOOM"
    const val ACTION_ZOOM_IN = "com.augmentalis.webavanue.action.ZOOM_IN"
    const val ACTION_ZOOM_OUT = "com.augmentalis.webavanue.action.ZOOM_OUT"
    const val EXTRA_ZOOM_PERCENT = "zoom_percent" // 50-200
}
```

**Phase 3: SDK Methods (0.2 hours)**
- File: `WebAvanueController.kt`

```kotlin
fun setZoomLevel(percent: Int) {
    require(percent in 50..200) { "Zoom must be 50-200%" }
    sendCommand(ACTION_SET_ZOOM) {
        putExtra(EXTRA_ZOOM_PERCENT, percent)
    }
}

fun zoomIn() = sendCommand(ACTION_ZOOM_IN)
fun zoomOut() = sendCommand(ACTION_ZOOM_OUT)
```

**Example Usage:**
```kotlin
// Set zoom to 150%
browser.setZoomLevel(150)

// Zoom in/out
browser.zoomIn()
browser.zoomOut()

// Reset to default
browser.setZoomLevel(100)
```

**Acceptance Criteria:**
- [ ] setZoomLevel accepts 50-200%
- [ ] zoomIn increases by 25%
- [ ] zoomOut decreases by 25%
- [ ] Zoom persists across page loads
- [ ] Zoom resets when navigating to different domain (optional)

---

### FR-027: Cookie Management API
**Priority:** Medium | **Complexity:** Medium | **Effort:** 3 hours

**Description:** Enable external apps to set/get/clear cookies for SSO and session management.

**Use Cases:**
- SSO: AR app logs in, sets auth cookie for WebAvanue
- Session persistence across AR sessions
- Clear cookies for privacy
- Debug: Inspect cookie values

**Implementation:**

**Phase 1: CookieManager Wrapper (1.5 hours)**
- File: `BrowserControlService.kt`

```kotlin
fun setCookie(url: String, cookieName: String, cookieValue: String, callback: (Boolean) -> Unit) {
    val cookie = "$cookieName=$cookieValue; path=/; max-age=31536000" // 1 year
    CookieManager.getInstance().setCookie(url, cookie) {
        callback(it)
    }
}

fun getCookie(url: String, callback: (String?) -> Unit) {
    val cookies = CookieManager.getInstance().getCookie(url)
    callback(cookies)
}

fun clearCookies(callback: (Boolean) -> Unit) {
    CookieManager.getInstance().removeAllCookies { success ->
        callback(success)
    }
}

fun clearCookiesForDomain(domain: String, callback: (Boolean) -> Unit) {
    // Remove cookies for specific domain
    val cookieManager = CookieManager.getInstance()
    val cookies = cookieManager.getCookie(domain) ?: ""
    cookies.split(";").forEach { cookie ->
        val name = cookie.substringBefore("=").trim()
        cookieManager.setCookie(domain, "$name=; max-age=0")
    }
    callback(true)
}
```

**Phase 2: Broadcast API (1 hour)**
- File: `BrowserControlReceiver.kt`

```kotlin
companion object {
    const val ACTION_SET_COOKIE = "com.augmentalis.webavanue.action.SET_COOKIE"
    const val ACTION_GET_COOKIE = "com.augmentalis.webavanue.action.GET_COOKIE"
    const val ACTION_CLEAR_COOKIES = "com.augmentalis.webavanue.action.CLEAR_COOKIES"

    const val EXTRA_COOKIE_URL = "cookie_url"
    const val EXTRA_COOKIE_NAME = "cookie_name"
    const val EXTRA_COOKIE_VALUE = "cookie_value"

    const val ACTION_COOKIE_RESULT = "com.augmentalis.webavanue.response.COOKIE"
    const val EXTRA_COOKIES = "cookies"
}
```

**Phase 3: SDK Methods (0.5 hours)**
- File: `WebAvanueController.kt`

```kotlin
fun setCookie(url: String, name: String, value: String, callback: (Boolean) -> Unit) {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val success = intent.getBooleanExtra("success", false)
            callback(success)
            context.unregisterReceiver(this)
        }
    }

    context.registerReceiver(receiver, IntentFilter("com.augmentalis.webavanue.response.COOKIE_SET"))

    sendCommand(ACTION_SET_COOKIE) {
        putExtra(EXTRA_COOKIE_URL, url)
        putExtra(EXTRA_COOKIE_NAME, name)
        putExtra(EXTRA_COOKIE_VALUE, value)
    }
}

fun getCookies(url: String, callback: (String?) -> Unit) {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val cookies = intent.getStringExtra(EXTRA_COOKIES)
            callback(cookies)
            context.unregisterReceiver(this)
        }
    }

    context.registerReceiver(receiver, IntentFilter(ACTION_COOKIE_RESULT))
    sendCommand(ACTION_GET_COOKIE) {
        putExtra(EXTRA_COOKIE_URL, url)
    }
}

fun clearAllCookies(callback: (Boolean) -> Unit) {
    sendCommand(ACTION_CLEAR_COOKIES)
    // Assume success (no response needed)
    callback(true)
}
```

**Example Usage:**
```kotlin
// SSO: Set auth token
browser.setCookie("https://example.com", "auth_token", "xyz123") { success ->
    if (success) {
        browser.navigate("https://example.com/dashboard")
    }
}

// Get all cookies
browser.getCookies("https://example.com") { cookies ->
    println("Cookies: $cookies")
}

// Clear all cookies
browser.clearAllCookies { success ->
    println("Cookies cleared: $success")
}
```

**Acceptance Criteria:**
- [ ] setCookie sets cookie for specified domain
- [ ] getCookies returns all cookies for domain
- [ ] clearAllCookies removes all cookies
- [ ] Cookies persist across app restarts (if not session cookies)
- [ ] Secure cookies (HTTPS only) respected
- [ ] HttpOnly cookies inaccessible from JavaScript

**Security:**
- Caller validation required (sensitive operation)
- Cannot set cookies for domains user hasn't visited (prevents CSRF)
- Audit log all cookie operations

---

### FR-028: Find in Page API
**Priority:** Low | **Complexity:** Low | **Effort:** 2 hours

**Description:** Enable external apps to search for text on the current page and navigate results.

**Use Cases:**
- Voice command: "Find product specifications"
- Highlight search results in AR overlay
- Auto-scroll to search result
- Count occurrences for analytics

**Implementation:**

**Phase 1: WebView FindListener (1 hour)**
- File: `BrowserViewModel.kt`

```kotlin
fun findInPage(query: String, callback: (Int) -> Unit) {
    webViewContainer?.findAllAsync(query)
    webViewContainer?.setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
        if (isDoneCounting) {
            callback(numberOfMatches)
        }
    }
}

fun findNext() {
    webViewContainer?.findNext(true)
}

fun findPrevious() {
    webViewContainer?.findNext(false)
}

fun clearFindMatches() {
    webViewContainer?.clearMatches()
}
```

**Phase 2: Broadcast API (0.5 hours)**
- File: `BrowserControlReceiver.kt`

```kotlin
companion object {
    const val ACTION_FIND_IN_PAGE = "com.augmentalis.webavanue.action.FIND_IN_PAGE"
    const val ACTION_FIND_NEXT = "com.augmentalis.webavanue.action.FIND_NEXT"
    const val ACTION_FIND_PREVIOUS = "com.augmentalis.webavanue.action.FIND_PREVIOUS"
    const val ACTION_CLEAR_FIND = "com.augmentalis.webavanue.action.CLEAR_FIND"

    const val EXTRA_QUERY = "query"

    const val ACTION_FIND_RESULT = "com.augmentalis.webavanue.response.FIND_RESULT"
    const val EXTRA_MATCH_COUNT = "match_count"
}
```

**Phase 3: SDK Methods (0.5 hours)**
- File: `WebAvanueController.kt`

```kotlin
fun findInPage(query: String, callback: (Int) -> Unit) {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val count = intent.getIntExtra(EXTRA_MATCH_COUNT, 0)
            callback(count)
            context.unregisterReceiver(this)
        }
    }

    context.registerReceiver(receiver, IntentFilter(ACTION_FIND_RESULT))
    sendCommand(ACTION_FIND_IN_PAGE) {
        putExtra(EXTRA_QUERY, query)
    }
}

fun findNext() = sendCommand(ACTION_FIND_NEXT)
fun findPrevious() = sendCommand(ACTION_FIND_PREVIOUS)
fun clearFind() = sendCommand(ACTION_CLEAR_FIND)
```

**Example Usage:**
```kotlin
// Find text
browser.findInPage("product") { count ->
    println("Found $count matches")
    speakARFeedback("Found $count results")
}

// Navigate results
browser.findNext()
browser.findPrevious()

// Clear highlights
browser.clearFind()
```

**Acceptance Criteria:**
- [ ] findInPage searches current page
- [ ] Returns match count
- [ ] findNext navigates to next match
- [ ] findPrevious navigates to previous match
- [ ] Search wraps around (after last match, goes to first)
- [ ] clearFind removes highlights
- [ ] Case-insensitive by default

---

### FR-029: Graceful Feature Degradation
**Priority:** Critical | **Complexity:** Low | **Effort:** 2 hours

**Description:** When a feature is not available on the device/WebView version, show a themed error dialog instead of crashing.

**Use Cases:**
- Old Android devices don't support WebXR
- AOSP builds lack certain WebView features
- File download requires storage permissions
- JavaScript execution requires API 19+

**Implementation:**

**Phase 1: Feature Detection Utility (0.5 hours)**
- File: `FeatureCompatibility.kt`

```kotlin
object FeatureCompatibility {
    fun isJavaScriptExecutionSupported(): Boolean {
        return Build.VERSION.SDK_INT >= 19 // KitKat+
    }

    fun isScreenshotSupported(): Boolean {
        return true // Always supported
    }

    fun isWebXRSupported(webView: WebView): Boolean {
        // Check if WebView supports WebXR API
        var supported = false
        webView.evaluateJavascript("'xr' in navigator") { result ->
            supported = result == "true"
        }
        return supported
    }

    fun isDownloadSupported(): Boolean {
        return Build.VERSION.SDK_INT >= 29 // Scoped storage
    }

    fun isCookieManagementSupported(): Boolean {
        return Build.VERSION.SDK_INT >= 21 // Lollipop+
    }

    fun getUnsupportedReason(feature: String): String {
        return when (feature) {
            "javascript_execution" -> {
                if (Build.VERSION.SDK_INT < 19) {
                    "JavaScript execution requires Android 4.4 (KitKat) or newer"
                } else "Feature unavailable"
            }
            "webxr" -> "WebXR requires updated WebView (version 79+)"
            "downloads" -> "File downloads require Android 10 or newer"
            "cookies" -> "Cookie management requires Android 5.0 or newer"
            else -> "Feature not available on this device"
        }
    }
}
```

**Phase 2: Error Dialog Component (1 hour)**
- File: `FeatureUnavailableDialog.kt`

```kotlin
@Composable
fun FeatureUnavailableDialog(
    featureName: String,
    onDismiss: () -> Unit
) {
    val reason = FeatureCompatibility.getUnsupportedReason(featureName)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Feature Not Available",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Feature: $featureName",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Consider upgrading the WebView or Device Operating System.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("OK", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    )
}
```

**Phase 3: Integration (0.5 hours)**
- Wrap all API methods with feature checks

Example in `WebAvanueController.kt`:
```kotlin
fun executeJavaScript(script: String, callback: (String?) -> Unit) {
    if (!FeatureCompatibility.isJavaScriptExecutionSupported()) {
        showFeatureUnavailableDialog("JavaScript Execution")
        callback(null)
        return
    }

    // Normal implementation...
}

fun captureScreenshot(callback: (Bitmap?) -> Unit) {
    if (!FeatureCompatibility.isScreenshotSupported()) {
        showFeatureUnavailableDialog("Screenshot Capture")
        callback(null)
        return
    }

    // Normal implementation...
}
```

**Acceptance Criteria:**
- [ ] Dialog uses Material3 theme colors (light/dark adaptive)
- [ ] Dialog shows feature name prominently
- [ ] Dialog explains why feature is unavailable
- [ ] Dialog suggests upgrading WebView/OS
- [ ] Dialog dismissible with "OK" button
- [ ] All 7 API features protected with checks
- [ ] No crashes on old devices
- [ ] Tested on Android 10 (API 29)

**Error Message Format:**
```
Title: Feature Not Available

Feature: [Feature Name]

[Specific reason based on device capabilities]

Consider upgrading the WebView or Device Operating System.

[OK Button]
```

**Examples:**
- Feature: JavaScript Execution
  Reason: JavaScript execution requires Android 4.4 (KitKat) or newer

- Feature: WebXR Detection
  Reason: WebXR requires updated WebView (version 79+)

- Feature: File Downloads
  Reason: File downloads require Android 10 or newer

---

## Non-Functional Requirements

### NFR-001: Performance
- Command bar state changes must render within 16ms (60fps)
- VoiceCommandDialog layout calculation < 50ms
- File download progress updates max 1fps (avoid UI jank)
- Headless mode transition < 100ms
- **Intent processing < 50ms (launch time critical)**

### NFR-002: Accessibility
- All interactive elements minimum 48dp touch target
- Contrast ratios ≥ 4.5:1 (WCAG AA)
- Voice dialog buttons have content descriptions
- Screen reader support for all new UI

### NFR-003: Compatibility
- Minimum API 29 (Android 10) for HMT-1
- Test on AOSP Android 10 device
- Avoid APIs introduced after Android 10

### NFR-004: UX Consistency
- Follow OceanTheme design language (glassmorphism, 12dp radius)
- Match existing animation durations (300ms standard, 150ms fast)
- Use Material3 components where possible

### NFR-005: Code Quality
- Follow SOLID principles
- 90%+ test coverage on business logic
- Zero compiler warnings
- Kotlin coding conventions

### NFR-006: Security (NEW)
- **Signature validation for external app control**
- **User approval dialog on first headless launch (per app)**
- **Whitelist persistence across app restarts**
- **Rate limiting: max 10 control commands/second per app**
- **Audit log of all external control commands**

### NFR-007: Developer Experience (NEW)
- **SDK API intuitive, no Android IPC knowledge required**
- **Example code in documentation**
- **Error messages clear (e.g., "WebAvanue not installed")**
- **Gradle dependency setup < 5 minutes**

---

## User Stories

### US-001: Headless Browser Mode
**As an** AR/XR user
**I want** to hide the address bar
**So that** I have maximum screen space for immersive experiences

**Acceptance Criteria:**
- [ ] I can toggle headless mode in Settings > Advanced
- [ ] When enabled, address bar disappears
- [ ] I can still navigate using command bar
- [ ] I can toggle desktop mode from command bar

---

### US-002: File Downloads
**As a** mobile user
**I want** to download files from websites
**So that** I can access them offline later

**Acceptance Criteria:**
- [ ] I can click a download link and see progress
- [ ] I receive a notification when download completes
- [ ] I can pause/resume downloads
- [ ] I can open downloaded files from Downloads screen

---

### US-003: Tab Groups
**As a** power user
**I want** to organize tabs into named groups
**So that** I can separate work/personal/research tabs

**Acceptance Criteria:**
- [ ] I can create a new tab group with a name
- [ ] I can drag tabs into groups
- [ ] I can collapse groups to hide tabs
- [ ] Groups persist when I close the app

---

### US-004: Voice Dialog Landscape
**As a** tablet user
**I want** voice commands in multiple columns
**So that** I can see all commands at once without scrolling

**Acceptance Criteria:**
- [ ] Landscape mode shows 2-3 columns
- [ ] All commands visible without scrolling
- [ ] Buttons maintain 48dp touch targets

---

### US-005: Command Bar State
**As a** user
**I want** the command bar toggle to reflect actual visibility
**So that** I know whether the bar is hidden or shown

**Acceptance Criteria:**
- [ ] Toggle button icon matches actual state
- [ ] Clicking toggle immediately shows/hides bar
- [ ] State persists across orientation changes

---

### US-006: AR/XR Developer Integration (NEW)
**As an** AR/XR app developer
**I want** to embed WebAvanue in my app without UI chrome
**So that** my users can view web content in immersive mode

**Acceptance Criteria:**
- [ ] I can launch WebAvanue in headless mode from my app
- [ ] I can specify the URL to load
- [ ] I can control navigation from my app's UI
- [ ] I can query the current URL and loading state
- [ ] My app's signature is validated for security

---

### US-007: Gesture-Based Browser Control (NEW)
**As an** AR headset user
**I want** to control WebAvanue with hand gestures
**So that** I can browse hands-free

**Acceptance Criteria:**
- [ ] My AR app can send navigation commands to WebAvanue
- [ ] Swipe left goes back, swipe right goes forward
- [ ] Pinch gesture reloads page
- [ ] WebAvanue responds instantly to gestures

---

## Platform-Specific Details

### Android (Kotlin Multiplatform + Jetpack Compose)

**Tech Stack:**
- **UI:** Jetpack Compose 1.5+ with Material3
- **Architecture:** MVVM with ViewModel + Repository
- **Database:** SQLDelight (for tab groups, downloads)
- **IPC:** Android Intent System, Broadcast Receivers
- **Dependency Injection:** Manual (existing pattern)
- **Concurrency:** Kotlin Coroutines + Flow
- **WebView:** Android WebView with custom WebViewClient

**Key Components:**

| Component | File | Responsibility |
|-----------|------|----------------|
| BrowserScreen | `BrowserScreen.kt` | Main UI orchestration |
| BrowserViewModel | `BrowserViewModel.kt` | State management + headless config |
| BrowserActivity | `BrowserActivity.kt` | Intent processing, lifecycle |
| BrowserControlReceiver | `BrowserControlReceiver.kt` | Broadcast intent handler (NEW) |
| BrowserControlService | `BrowserControlService.kt` | Command execution service (NEW) |
| AddressBar | `AddressBar.kt` | URL input, navigation |
| BottomCommandBar | `BottomCommandBar.kt` | Floating command menu |
| VoiceCommandDialog | `VoiceCommandDialog.kt` | Voice commands UI |
| WebViewContainer | `WebViewContainer.android.kt` | WebView wrapper + DownloadListener |
| TabSwitcherView | `TabSwitcherView.kt` | Tab management |
| SettingsScreen | `SettingsScreen.kt` | Settings UI |
| BrowserSettings | `BrowserSettings.kt` | Settings domain model |
| WebAvanueController | `WebAvanueController.kt` | Developer SDK (NEW) |

**Database Schema (SQLDelight):**
```sql
-- Tab Groups
CREATE TABLE TabGroups (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    color TEXT NOT NULL,
    is_collapsed INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL
);

-- Add group_id to Tabs table
ALTER TABLE Tabs ADD COLUMN group_id TEXT;

-- Downloads
CREATE TABLE Downloads (
    id TEXT PRIMARY KEY NOT NULL,
    url TEXT NOT NULL,
    filename TEXT NOT NULL,
    mime_type TEXT NOT NULL,
    total_bytes INTEGER NOT NULL,
    downloaded_bytes INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL, -- PENDING, DOWNLOADING, PAUSED, COMPLETED, FAILED
    created_at INTEGER NOT NULL,
    completed_at INTEGER
);

-- External App Whitelist (NEW)
CREATE TABLE AuthorizedApps (
    package_name TEXT PRIMARY KEY NOT NULL,
    app_name TEXT NOT NULL,
    signature_hash TEXT NOT NULL,
    authorized_at INTEGER NOT NULL,
    last_used INTEGER NOT NULL
);
```

**New Settings Fields:**
```kotlin
data class BrowserSettings(
    // ... existing fields ...

    // UI Mode Settings
    val headlessMode: Boolean = false, // EXISTING (v1.8.0)

    // Voice Settings
    val voiceDialogAutoClose: Boolean = true, // NEW (FR-004)

    // Command Bar Settings
    val commandBarAutoHide: Boolean = false, // NEW (FR-011)

    // External API Settings (NEW)
    val allowExternalControl: Boolean = true, // Allow external apps to control browser
    val requireApprovalPerApp: Boolean = true, // Show approval dialog first time
)
```

**Dependencies:**
- No new external dependencies required
- Uses existing: Compose, Material3, Coroutines, SQLDelight, WebView

**Testing:**
- **Unit Tests:** ViewModels, Repositories (JUnit 5)
- **Integration Tests:** Intent processing, broadcast receiver (Robolectric)
- **UI Tests:** Compose Testing, Espresso (critical flows)
- **Manual Tests:** HMT-1 (AOSP Android 10)
- **SDK Tests:** Sample AR app integration test

---

## Technical Constraints

### Platform Constraints
- **Minimum Android API:** 29 (Android 10) for HMT-1 compatibility
- **WebView Limitations:** Download requires DownloadListener, no direct file access
- **Compose Version:** 1.5+ (avoid newer APIs)
- **Material3:** Already in use, no version upgrade needed

### Performance Constraints
- **ANR Prevention:** No main thread blocking > 150ms (learned from v1.8.1)
- **Memory:** Tab groups and downloads must handle 100+ items without OOM
- **Storage:** Downloads limited to available disk space (check before download)
- **Intent Processing:** < 50ms from external intent to UI update

### Design Constraints
- **OceanTheme:** Glassmorphism (semi-transparent blur), 12dp corner radius, 48dp touch targets
- **Accessibility:** WCAG AA contrast ratios (4.5:1)
- **Animation:** 300ms standard, 150ms fast (consistency)

### Business Constraints
- **AOSP Compatibility:** Must work on HMT-1 without Google Play Services
- **Offline First:** Core browser functions work without network
- **Privacy:** No telemetry, no cloud sync (local only)

### Security Constraints (NEW)
- **Signature Validation:** External apps must have same signature OR user approval
- **Rate Limiting:** Max 10 control commands/second per app
- **Audit Logging:** All external commands logged with timestamp and caller
- **Sandboxing:** External apps cannot access browser data (cookies, history, etc.)

---

## Dependencies

### Implementation Order

**Phase 1: Critical Fixes (Week 1)**
1. FR-010: Command bar toggle state sync
2. FR-011: Command bar layout and auto-hide
3. FR-018: Remove redundant buttons
4. FR-016: File downloads implementation (start)

**Phase 2: Headless Mode + APIs (Week 2-3)**
5. FR-005: Headless browser mode (full implementation)
6. **FR-019: Headless mode Intent API**
7. **FR-020: Browser control API (Broadcast intents)**
8. FR-009: Star icon behavior change
9. FR-006: Tab management navigation

**Phase 3: Visual Polish (Week 4)**
10. FR-001: Floating action button glass effect
11. FR-012: Light theme dropdown visibility
12. FR-013: Landscape search engine popup
13. FR-002: Voice dialog multi-column layout
14. FR-003: Voice dialog button sizing

**Phase 4: Settings & Features (Week 5)**
15. FR-014: WebXR settings integration
16. FR-004: Voice dialog auto-timeout setting
17. FR-015: Tab groups implementation
18. FR-016: File downloads (complete)

**Phase 5: Edge Cases & SDK (Week 6)**
19. FR-008: Network status alerts
20. FR-017: Default homepage fix
21. FR-007: Tab management icons
22. **FR-021: Developer SDK library**

### External Dependencies
- None (all functionality uses existing libraries)

### Cross-Component Dependencies
- FR-005 (Headless mode) depends on FR-018 (Remove redundant buttons) - reduces command bar clutter
- **FR-019, FR-020 depend on FR-005** - headless mode must work first
- FR-016 (File downloads) depends on database migration - TabGroups schema change
- FR-014 (WebXR settings) should complete before FR-004 (Voice timeout) - both modify Settings UI
- **FR-021 (SDK) depends on FR-019, FR-020** - APIs must be stable before SDK

---

## Swarm Assessment

### Multi-Agent Activation: **HIGHLY RECOMMENDED**

**Reasoning:**
- **28 functional requirements** across 4 complexity tiers
- **7-8 week timeline** with parallel work opportunities
- **Multiple domains:** UI, Settings, Dialogs, Tab management, **Android IPC, SDK development, advanced developer APIs**
- **Database changes:** TabGroups, Downloads, AuthorizedApps (separate from UI work)
- **Expected time savings:** 45-50% via parallel implementation
- **17 additional hours** for 7 new API features (FR-022 to FR-029)

### Recommended Swarm Agents

| Agent | Responsibilities | FRs | Duration |
|-------|------------------|-----|----------|
| **UI-Stylist** | Visual fixes, layout adjustments | FR-001, FR-002, FR-003, FR-012, FR-013 | 6 hours |
| **State-Manager** | State sync, command bar fixes | FR-010, FR-011, FR-018 | 6 hours |
| **Feature-Builder-1** | Headless mode implementation | FR-005, FR-009 | 9 hours |
| **Feature-Builder-2** | File downloads | FR-016 | 10 hours |
| **Feature-Builder-3** | Tab groups | FR-015 | 12 hours |
| **API-Developer-1** | **Intent APIs, broadcast receivers** | **FR-019, FR-020** | **10 hours** |
| **API-Developer-2** | **Advanced developer APIs** | **FR-022, FR-023, FR-024, FR-025** | **11 hours** |
| **API-Developer-3** | **Additional developer APIs** | **FR-026, FR-027, FR-028, FR-029** | **8 hours** |
| **SDK-Developer** | **Developer SDK library** | **FR-021 + SDK updates for new APIs** | **5 hours** |
| **Settings-Specialist** | Settings UI refactoring | FR-004, FR-014, FR-017 | 4 hours |
| **UX-Polisher** | Navigation improvements | FR-006, FR-007, FR-008 | 4.5 hours |
| **Architect** | Database schema, API security, feature detection | Schema design, security review, compatibility | 6 hours |
| **Test-Engineer** | Test coverage, HMT-1 testing, SDK integration tests, API tests | All FRs | 14 hours |

**Parallelization Strategy:**

**Week 1 (Parallel):**
- **State-Manager:** FR-010, FR-011, FR-018 (critical path)
- **Feature-Builder-2:** FR-016 Phase 1-2 (DownloadListener, Repository)
- **Architect:** Database schema design + security model + feature detection utility

**Week 2 (Parallel):**
- **Feature-Builder-1:** FR-005 (Headless mode)
- **Feature-Builder-2:** FR-016 Phase 3-4 (Download service, UI)
- **Settings-Specialist:** FR-014 (WebXR integration)
- **API-Developer-3:** FR-029 (Graceful degradation - blocks all APIs)

**Week 3 (Parallel):**
- **API-Developer-1:** FR-019 (Intent API)
- **API-Developer-1:** FR-020 (Broadcast API)
- **API-Developer-2:** FR-022 (JavaScript execution)
- **API-Developer-2:** FR-023 (Page callbacks)
- **Architect:** Security review, signature validation
- **Feature-Builder-3:** FR-015 Phase 1-2 (Tab groups domain)

**Week 4 (Parallel):**
- **UI-Stylist:** FR-001, FR-002, FR-003, FR-012, FR-013
- **Feature-Builder-3:** FR-015 Phase 3-5 (Tab groups UI)
- **UX-Polisher:** FR-006, FR-009
- **API-Developer-2:** FR-024 (Screenshot API)
- **API-Developer-2:** FR-025 (WebXR detection)
- **SDK-Developer:** FR-021 Phase 1 (SDK library base)

**Week 5 (Parallel):**
- **Settings-Specialist:** FR-004, FR-017
- **UX-Polisher:** FR-007, FR-008
- **API-Developer-3:** FR-026 (Zoom control)
- **API-Developer-3:** FR-027 (Cookie management)
- **SDK-Developer:** FR-021 Phase 2 (SDK methods for FR-022 to FR-025)

**Week 6 (Parallel):**
- **API-Developer-3:** FR-028 (Find in page)
- **SDK-Developer:** FR-021 Phase 3 (SDK methods for FR-026 to FR-028)
- **Test-Engineer:** Unit tests for APIs
- **Test-Engineer:** Integration tests

**Week 7 (Parallel):**
- **SDK-Developer:** Example AR app demonstrating all APIs
- **Test-Engineer:** HMT-1 validation, all features
- **Test-Engineer:** SDK integration tests
- **Architect:** Documentation + API reference

**Week 8 (Sequential - If Needed):**
- **Test-Engineer:** Regression testing, edge cases
- **Architect:** Final code review, security audit
- **SDK-Developer:** SDK documentation + publishing

### Swarm Communication Protocol

**Daily Sync (Async):**
- Each agent reports: completed FRs, blockers, dependencies
- Architect reviews PRs, identifies conflicts

**Shared Resources:**
- `BrowserSettings.kt` - Settings-Specialist owns, others request changes
- `BrowserScreen.kt` - Feature-Builder-1 owns (Headless mode)
- `BrowserActivity.kt` - **API-Developer owns (Intent processing)**
- `BottomCommandBar.kt` - State-Manager owns
- Database schema - Architect owns
- **Security model** - Architect owns, API-Developer implements

**Conflict Resolution:**
- Merge conflicts → Architect resolves
- Design disagreements → Lead developer decides
- Security concerns → Architect escalates
- Blocker escalation → Switch to sequential for affected FRs

---

## Success Criteria

### Functional Success
- [ ] All 28 FRs implemented and tested
- [ ] Zero regression bugs in existing features
- [ ] Headless mode works on HMT-1 without ANR
- [ ] **External app can launch WebAvanue in headless mode**
- [ ] **External app can control navigation via broadcasts**
- [ ] **SDK library published and documented**
- [ ] **All 7 developer APIs functional:** JavaScript, Callbacks, Screenshot, WebXR, Zoom, Cookies, Find
- [ ] **Graceful degradation dialogs shown on unsupported devices**
- [ ] File downloads complete successfully (50MB test file)
- [ ] Tab groups persist across app restarts
- [ ] All light theme text readable
- [ ] Command bar toggle state accurate

### Performance Success
- [ ] No ANRs on HMT-1 (Android 10 AOSP)
- [ ] Command bar state changes < 16ms (60fps)
- [ ] VoiceCommandDialog layout < 50ms
- [ ] File download progress updates 1Hz (no jank)
- [ ] **Intent processing < 50ms**
- [ ] **JavaScript execution < 500ms**
- [ ] **Screenshot capture < 1 second**

### UX Success
- [ ] User testing: 90%+ task completion rate
- [ ] No user confusion on command bar toggle
- [ ] Voice dialog readable in landscape (tablet)
- [ ] Headless mode demo successful (AR/XR use case)
- [ ] **Feature unavailable dialogs use correct theme colors**
- [ ] **No crashes on API 29 devices**

### Code Quality Success
- [ ] Zero compiler warnings
- [ ] 90%+ test coverage on new code
- [ ] All code follows Kotlin conventions
- [ ] No SOLID violations
- [ ] Code review approved by architect

### Accessibility Success
- [ ] All touch targets ≥ 48dp
- [ ] Contrast ratios ≥ 4.5:1 (WCAG AA)
- [ ] Screen reader announces all new UI
- [ ] Voice dialog buttons have content descriptions
- [ ] **Error dialogs accessible via screen reader**

### Security Success
- [ ] Signature validation prevents unauthorized access
- [ ] User approval dialog shown on first external launch
- [ ] Rate limiting prevents command spam (10 cmds/sec navigation, 20/sec JavaScript)
- [ ] Audit log records all external commands
- [ ] Security review passed with zero high/critical issues
- [ ] **JavaScript sandboxing prevents malicious code execution**
- [ ] **Cookie management requires domain validation**

### Developer Experience Success
- [ ] SDK dependency setup < 5 minutes
- [ ] Example AR app runs without modifications
- [ ] Documentation complete with code examples
- [ ] **API reference published (Appendices B, C, D)**
- [ ] **All 7 developer APIs documented in SDK**
- [ ] **Example app demonstrates JavaScript execution, callbacks, screenshot, WebXR detection**
- [ ] Developer feedback: 8/10 or higher satisfaction

---

## Out of Scope

**Explicitly excluded from this spec:**
- Cross-device sync for tab groups (future: v2.0)
- Nested tab groups (future: v2.0)
- Custom download folder selection (use system default)
- P2P file sharing (future: v2.1)
- Advanced network diagnostics (beyond simple online/offline)
- Redesign of entire Settings screen (incremental changes only)
- **iOS version** (Android only for now)
- Tablet-optimized layouts (works on tablet but not optimized)
- **JavaScript bridge for web-to-native communication** (future: v2.0)
- **Content Provider for read-only browser data access** (future: v1.10)
- **OAuth-based authorization for external apps** (future: v2.0)

---

## Risks and Mitigations

### Risk 1: File Downloads Complexity
**Risk:** Android download manager API complexity, permission handling
**Likelihood:** Medium | **Impact:** High
**Mitigation:**
- Use WorkManager instead of raw DownloadManager (simpler API)
- Request WRITE_EXTERNAL_STORAGE at runtime (API 29 requires it)
- Fallback: Use app-private storage if permission denied

### Risk 2: Tab Groups Database Migration
**Risk:** Migration failure on existing users' databases
**Likelihood:** Low | **Impact:** High
**Mitigation:**
- Test migration on production database copies
- Add rollback logic
- Fallback: Wipe tab groups only (preserve tabs)

### Risk 3: Headless Mode Usability
**Risk:** Users get stuck in headless mode, can't access settings
**Likelihood:** Medium | **Impact:** Medium
**Mitigation:**
- Command bar MENU level always shows Settings button
- Add "Exit Headless Mode" button in Settings
- Show tooltip on first enable: "Tap ? button for help"

### Risk 4: AOSP Android 10 Compatibility
**Risk:** APIs used are unavailable on AOSP HMT-1
**Likelihood:** Low | **Impact:** High
**Mitigation:**
- Test every FR on HMT-1 before marking complete
- Avoid Google Play Services dependencies
- Use @RequiresApi checks for safety

### Risk 5: Swarm Coordination Overhead
**Risk:** Merge conflicts, communication delays between agents
**Likelihood:** Medium | **Impact:** Medium
**Mitigation:**
- Architect reviews all PRs before merge
- Daily async syncs (written reports)
- Clear ownership of shared files
- Feature flags for incomplete work (no broken main branch)

### Risk 6: External App Security Vulnerabilities (NEW)
**Risk:** Malicious apps hijack WebAvanue, phishing attacks, data theft
**Likelihood:** Medium | **Impact:** Critical
**Mitigation:**
- **Signature validation mandatory** (not optional)
- User approval dialog with app name, icon, signature hash
- Persistent whitelist, user can revoke at any time
- Audit log of all external commands (for forensics)
- "Powered by WebAvanue" badge always visible in headless mode
- Rate limiting prevents DoS attacks
- Security review by external pentester before release

### Risk 7: SDK API Instability (NEW)
**Risk:** API changes break external apps, poor developer adoption
**Likelihood:** Low | **Impact:** Medium
**Mitigation:**
- Semantic versioning (1.0.0 → 1.0.x for patches, 1.x.0 for features)
- Deprecation warnings for 6 months before breaking changes
- Beta testing with 3-5 AR/XR developers before GA
- Comprehensive documentation and migration guides

---

## Appendix A: Screenshot Analysis

*(Same as before - FR-001 through FR-018)*

---

## Appendix B: Intent API Reference

### B1: Launch Intent Extras

| Extra Key | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `HEADLESS_MODE` | Boolean | No | false | Enable headless mode (hide address bar) |
| `URL` | String | No | homepage | Initial URL to load |
| `SHOW_COMMAND_BAR` | Boolean | No | true | Show floating command bar in headless mode |
| `ALLOW_USER_NAVIGATION` | Boolean | No | true | Allow user to navigate (address bar, gestures) |
| `CALLER_PACKAGE` | String | Yes* | null | Calling app's package name (*required for headless) |

**Example (Kotlin):**
```kotlin
val intent = Intent().apply {
    setClassName("com.augmentalis.Avanues.web",
                 "com.augmentalis.Avanues.web.BrowserActivity")
    putExtra("com.augmentalis.webavanue.HEADLESS_MODE", true)
    putExtra("com.augmentalis.webavanue.URL", "https://example.com")
    putExtra("com.augmentalis.webavanue.SHOW_COMMAND_BAR", false)
    putExtra("com.augmentalis.webavanue.ALLOW_USER_NAVIGATION", false)
    putExtra("com.augmentalis.webavanue.CALLER_PACKAGE", packageName)
}
startActivity(intent)
```

### B2: Deep Link Format

**Scheme:** `webavanue://browser`

**Query Parameters:**
- `url` (required): URL-encoded website URL
- `headless` (optional): `true` or `false`
- `commandBar` (optional): `true` or `false`
- `navigation` (optional): `true` or `false`

**Example:**
```
webavanue://browser?url=https%3A%2F%2Fexample.com&headless=true&commandBar=false&navigation=false
```

**HTML Link (for web apps):**
```html
<a href="webavanue://browser?url=https://example.com&headless=true">
    Open in WebAvanue Headless Mode
</a>
```

### B3: Control Broadcast Actions

| Action | Extras | Description |
|--------|--------|-------------|
| `com.augmentalis.webavanue.action.NAVIGATE` | `url`, `caller_package` | Navigate to URL |
| `com.augmentalis.webavanue.action.GO_BACK` | `caller_package` | Go back in history |
| `com.augmentalis.webavanue.action.GO_FORWARD` | `caller_package` | Go forward in history |
| `com.augmentalis.webavanue.action.RELOAD` | `caller_package` | Reload current page |
| `com.augmentalis.webavanue.action.CLOSE` | `caller_package` | Close browser activity |
| `com.augmentalis.webavanue.action.QUERY_STATUS` | `caller_package` | Query current status |

**Example (Kotlin):**
```kotlin
// Navigate
val intent = Intent("com.augmentalis.webavanue.action.NAVIGATE").apply {
    setPackage("com.augmentalis.Avanues.web")
    putExtra("url", "https://example.com")
    putExtra("caller_package", packageName)
}
sendBroadcast(intent)
```

### B4: Status Response

**Action:** `com.augmentalis.webavanue.response.STATUS`

**Extras:**
- `current_url`: String - Current page URL
- `is_loading`: Boolean - Is page loading
- `can_go_back`: Boolean - Can navigate back
- `can_go_forward`: Boolean - Can navigate forward

**Example (Kotlin):**
```kotlin
// Register receiver
val receiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val url = intent.getStringExtra("current_url")
        val loading = intent.getBooleanExtra("is_loading", false)
        println("Current URL: $url, Loading: $loading")
    }
}
registerReceiver(receiver, IntentFilter("com.augmentalis.webavanue.response.STATUS"))

// Query status
sendBroadcast(Intent("com.augmentalis.webavanue.action.QUERY_STATUS").apply {
    setPackage("com.augmentalis.Avanues.web")
    putExtra("caller_package", packageName)
})
```

---

## Appendix C: SDK Quick Start

### C1: Add Dependency

**build.gradle.kts:**
```kotlin
dependencies {
    implementation("com.augmentalis:webavanue-sdk:1.0.0")
}
```

### C2: Initialize Controller

```kotlin
class MyActivity : AppCompatActivity() {
    private val browser = WebAvanueController(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Launch in headless mode
        browser.launchHeadless(
            url = "https://example.com",
            showCommandBar = false,
            allowNavigation = false
        )
    }
}
```

### C3: Control Navigation

```kotlin
// Navigate to new URL
browser.navigate("https://example.com")

// Go back
browser.goBack()

// Go forward
browser.goForward()

// Reload
browser.reload()

// Close
browser.close()

// Query status
browser.queryStatus { status ->
    println("URL: ${status.currentUrl}")
    println("Loading: ${status.isLoading}")
}
```

### C4: Permissions

**AndroidManifest.xml:**
```xml
<!-- Required for sending broadcasts to WebAvanue -->
<uses-permission android:name="android.permission.INTERNET" />
```

**No runtime permissions needed** (queries package manager automatically).

---

## Appendix D: Glossary

| Term | Definition |
|------|------------|
| **ANR** | Application Not Responding - Android system kills app after 5s UI freeze |
| **AOSP** | Android Open Source Project - Android without Google services |
| **Broadcast Receiver** | Android IPC mechanism for sending/receiving asynchronous messages |
| **Deep Link** | URL scheme that launches an app with specific data |
| **Glassmorphism** | UI design style with semi-transparent backgrounds and blur effects |
| **Headless Mode** | Browser mode with no address bar, only command bar and web content |
| **HMT-1** | RealWear Head-Mounted Tablet running AOSP Android 10 |
| **Intent** | Android IPC mechanism for launching activities and passing data |
| **IPC** | Inter-Process Communication - mechanism for apps to communicate |
| **OceanTheme** | WebAvanue's design system (blue palette, glassmorphism, rounded corners) |
| **SDK** | Software Development Kit - library for developers to integrate features |
| **Signature Validation** | Verifying an app's cryptographic signature for security |
| **SQLDelight** | Kotlin Multiplatform SQL library |
| **WebXR** | Web API for AR/VR experiences in browser |
| **WCAG AA** | Web Content Accessibility Guidelines Level AA (4.5:1 contrast) |

---

## Appendix E: File Structure Reference

```
common/webavanue/
├── coredata/
│   └── src/commonMain/kotlin/com/augmentalis/webavanue/domain/model/
│       ├── BrowserSettings.kt          [FR-004, FR-005, FR-011, FR-014]
│       ├── TabGroup.kt                 [FR-015] NEW
│       └── Download.kt                 [FR-016] NEW
│
├── universal/
│   └── src/commonMain/kotlin/com/augmentalis/Avanues/web/universal/
│       ├── presentation/ui/browser/
│       │   ├── BrowserScreen.kt        [FR-001, FR-005, FR-008, FR-010, FR-011]
│       │   ├── AddressBar.kt           [FR-009]
│       │   └── BottomCommandBar.kt     [FR-005, FR-010, FR-011, FR-018]
│       │
│       ├── presentation/ui/dialogs/
│       │   └── VoiceCommandDialog.kt   [FR-002, FR-003, FR-004]
│       │
│       ├── presentation/ui/settings/
│       │   └── SettingsScreen.kt       [FR-004, FR-012, FR-013, FR-014]
│       │
│       ├── presentation/ui/tab/
│       │   └── TabSwitcherView.kt      [FR-006, FR-007, FR-015]
│       │
│       ├── presentation/ui/downloads/
│       │   └── DownloadScreen.kt       [FR-016]
│       │
│       └── presentation/viewmodel/
│           ├── BrowserViewModel.kt     [FR-005, FR-010, FR-017, FR-019]
│           ├── TabViewModel.kt         [FR-015]
│           └── DownloadViewModel.kt    [FR-016] NEW
│
└── platform/
    └── android/
        ├── apps/webavanue/src/main/kotlin/com/augmentalis/Avanues/web/
        │   ├── BrowserActivity.kt          [FR-019] MODIFIED
        │   ├── BrowserControlReceiver.kt   [FR-020] NEW
        │   └── BrowserControlService.kt    [FR-020] NEW
        │
        ├── libs/webavanue-sdk/src/main/kotlin/com/augmentalis/webavanue/sdk/
        │   ├── WebAvanueController.kt      [FR-021] NEW
        │   └── BrowserStatus.kt            [FR-021] NEW
        │
        └── platform/src/main/kotlin/com/augmentalis/webavanue/platform/
            ├── WebViewContainer.android.kt  [FR-016]
            ├── DownloadService.kt          [FR-016] NEW
            └── NetworkManager.kt           [FR-008] NEW
```

---

## Document Metadata

**Author:** Claude (Anthropic)
**Specification Template:** IDEACODE v10.2
**Review Status:** Draft
**Approval Required:** Yes
**Target Version:** WebAvanue v1.9.0
**Total Functional Requirements:** 28 (19 UI/UX + 9 Developer APIs)
**Total Effort:** ~105 hours (sequential) / ~55 hours (with swarm - 48% savings)
**Estimated Timeline:** 7-8 weeks (with swarm) / 13+ weeks (sequential)

**Key Additions from v1.1:**
- FR-022: JavaScript Execution API (4h)
- FR-023: Page Lifecycle Callbacks (2h)
- FR-024: Screenshot/Capture API (3h)
- FR-025: WebXR Session Detection (2h)
- FR-026: Zoom Control API (1h)
- FR-027: Cookie Management API (3h)
- FR-028: Find in Page API (2h)
- FR-029: Graceful Feature Degradation (2h)

**Next Steps:**
1. Review specification with team
2. Approve swarm activation (13 agents recommended)
3. Assign agents to FRs based on parallelization strategy
4. Create feature branch: `feature/webavanue-v1.9.0-ui-improvements`
5. **Create sample AR app for SDK testing**
6. **Security review of external API design (signature validation, rate limiting)**
7. **Feature compatibility matrix for AOSP devices**
8. Begin Phase 1 implementation (Week 1)

---

**END OF SPECIFICATION**
