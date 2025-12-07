# Implementation Tasks: WebAvanue UI/UX Improvements v1.9.0

**Source Spec:** spec-webavanue-ui-ux-improvements-20251203.md v1.3
**Target Version:** WebAvanue v1.9.0
**Platform:** Android (Kotlin Multiplatform + Jetpack Compose)
**Execution Mode:** Swarm (Parallel)
**Date:** 2025-12-03

---

## Swarm Organization

### Agent Allocation

| Agent | Focus Areas | Task IDs |
|-------|-------------|----------|
| UI-Agent | Composables, layouts, theming | T001-T013, T025-T027 |
| State-Agent | ViewModels, state management | T014-T016, T028-T030 |
| Feature-Agent | Core features, downloads, groups | T017-T023 |
| API-Agent | External APIs, Intents, SDK | T031-T040 |
| Integration-Agent | Testing, validation, integration | T041-T045 |

### Execution Phases

**Phase 1 (Critical):** T001-T016 (State + Core UI fixes)
**Phase 2 (High):** T017-T030 (Features + Navigation)
**Phase 3 (Medium):** T031-T040 (External APIs)
**Phase 4 (Validation):** T041-T045 (Testing + Integration)

---

## Task List (45 Tasks)

### Phase 1: Critical Fixes (T001-T016)

#### T001: Fix Command Bar Toggle State Sync (FR-010)
- **Priority:** Critical
- **Effort:** 1.5h
- **Files:** `BrowserViewModel.kt`, `BrowserScreen.kt`
- **Agent:** State-Agent
- **Dependencies:** None
- **Description:** Sync command bar toggle state between ViewModel and UI
- **Steps:**
  1. Add `commandBarVisible: Boolean` to BrowserViewModel state
  2. Update toggle button onClick to update ViewModel state
  3. Observe state in BrowserScreen composable
  4. Test toggle persists through recomposition
- **Tests:** Unit test state updates, UI test toggle persistence

#### T002: Fix Command Bar Auto-Hide Logic (FR-011)
- **Priority:** Critical
- **Effort:** 2h
- **Files:** `BrowserViewModel.kt`, `BrowserScreen.kt`
- **Agent:** State-Agent
- **Dependencies:** T001
- **Description:** Fix auto-hide timer logic and cancellation
- **Steps:**
  1. Implement proper timer cancellation on user interaction
  2. Add `commandBarAutoHide: Boolean` setting
  3. Only auto-hide if setting enabled
  4. Cancel timer on manual toggle
- **Tests:** Timer cancellation test, setting toggle test

#### T003: Center Command Bar in Portrait (FR-011)
- **Priority:** High
- **Effort:** 1h
- **Files:** `BottomCommandBar.kt`
- **Agent:** UI-Agent
- **Dependencies:** None
- **Description:** Center command bar buttons, remove scrolling
- **Steps:**
  1. Change Row to use `Arrangement.Center`
  2. Remove horizontal scroll modifier
  3. Test on multiple screen sizes
- **Tests:** Screenshot test for centering

#### T004: Add Glassmorphism to FAB (FR-001)
- **Priority:** Medium
- **Effort:** 0.5h
- **Files:** `BrowserScreen.kt`
- **Agent:** UI-Agent
- **Dependencies:** None
- **Description:** Apply glass effect to floating action button
- **Steps:**
  1. Add blur modifier (12.dp)
  2. Set containerColor with 15% opacity
  3. Add 1px border with 30% opacity
- **Tests:** Visual regression test

#### T005: Fix VoiceCommandDialog Landscape Layout (FR-002)
- **Priority:** High
- **Effort:** 1.5h
- **Files:** `VoiceCommandDialog.kt`
- **Agent:** UI-Agent
- **Dependencies:** None
- **Description:** Use 2-column grid in landscape
- **Steps:**
  1. Add BoxWithConstraints for orientation detection
  2. Use LazyVerticalGrid with 2 columns if landscape
  3. Keep single column for portrait
  4. Test rotation
- **Tests:** Landscape orientation test

#### T006: Fix VoiceCommandDialog Button Sizing (FR-003)
- **Priority:** Medium
- **Effort:** 0.5h
- **Files:** `VoiceCommandDialog.kt`
- **Agent:** UI-Agent
- **Dependencies:** T005
- **Description:** Consistent 48.dp button height
- **Steps:**
  1. Set all buttons to `Modifier.height(48.dp)`
  2. Verify touch targets meet Material3 spec
- **Tests:** Accessibility test for touch targets

#### T007: Add Voice Dialog Auto-Timeout Setting (FR-004)
- **Priority:** Medium
- **Effort:** 1h
- **Files:** `BrowserSettings.kt`, `GeneralSettingsScreen.kt`, `VoiceCommandDialog.kt`
- **Agent:** State-Agent
- **Dependencies:** None
- **Description:** Toggle for auto-closing voice dialog
- **Steps:**
  1. Add `voiceDialogAutoClose: Boolean` to BrowserSettings
  2. Add switch to General Settings screen
  3. Implement auto-close logic in dialog
  4. Persist setting
- **Tests:** Setting persistence test, auto-close behavior test

#### T008: Implement Headless Browser Mode (FR-005)
- **Priority:** Critical
- **Effort:** 3h
- **Files:** `BrowserViewModel.kt`, `BrowserScreen.kt`, `BrowserSettings.kt`
- **Agent:** Feature-Agent
- **Dependencies:** None
- **Description:** True fullscreen mode with no UI elements
- **Steps:**
  1. Add `headlessModeActive: Boolean` to ViewModel state
  2. Hide all UI (address bar, command bar, tabs) when active
  3. Add gesture to exit (3-finger tap or long-press)
  4. Persist state across sessions
  5. Add voice command "headless mode on/off"
- **Tests:** UI visibility test, gesture recognition test, voice command test

#### T009: Add Tab Management Back Button (FR-006)
- **Priority:** High
- **Effort:** 0.5h
- **Files:** `TabSwitcherView.kt`
- **Agent:** UI-Agent
- **Dependencies:** None
- **Description:** Add back button to close tab switcher
- **Steps:**
  1. Add IconButton with back arrow to top bar
  2. Call onDismiss callback
- **Tests:** Navigation test

#### T010: Update Tab Management Icons (FR-007)
- **Priority:** Low
- **Effort:** 0.5h
- **Files:** `TabSwitcherView.kt`
- **Agent:** UI-Agent
- **Dependencies:** T009
- **Description:** Change icons: List → ViewList, Grid → GridView
- **Steps:**
  1. Replace icon imports
  2. Update contentDescription
- **Tests:** Visual test

#### T011: Implement Network Status Alerts (FR-008)
- **Priority:** High
- **Effort:** 2h
- **Files:** `NetworkMonitor.kt`, `BrowserViewModel.kt`, `BrowserScreen.kt`
- **Agent:** Feature-Agent
- **Dependencies:** None
- **Description:** Alert user when offline, retry on reconnect
- **Steps:**
  1. Create NetworkMonitor with ConnectivityManager
  2. Show Snackbar when offline
  3. Add "Retry" action
  4. Auto-reload when reconnected
- **Tests:** Offline scenario test, reconnection test

#### T012: Fix Light Theme Dropdown Visibility (FR-012)
- **Priority:** Medium
- **Effort:** 0.5h
- **Files:** `SearchEnginePopup.kt`
- **Agent:** UI-Agent
- **Dependencies:** None
- **Description:** Darker border/background for light theme
- **Steps:**
  1. Check if theme is Light
  2. Apply darker surface color (12% opacity)
  3. Add visible border
- **Tests:** Light theme screenshot test

#### T013: Fix Landscape Search Engine Popup (FR-013)
- **Priority:** Medium
- **Effort:** 1h
- **Files:** `SearchEnginePopup.kt`
- **Agent:** UI-Agent
- **Dependencies:** T012
- **Description:** Use 3-column grid in landscape
- **Steps:**
  1. Add BoxWithConstraints
  2. Use LazyVerticalGrid with 3 columns if landscape
  3. Test rotation
- **Tests:** Landscape orientation test

#### T014: Move WebXR Settings to Advanced (FR-014)
- **Priority:** Medium
- **Effort:** 0.5h
- **Files:** `SettingsScreen.kt`
- **Agent:** UI-Agent
- **Dependencies:** None
- **Description:** Remove WebXR section, move to Advanced
- **Steps:**
  1. Remove AR/XR navigation item
  2. Add WebXR toggle to Advanced Settings screen
- **Tests:** Settings navigation test

#### T015: Implement Tab Groups (FR-015)
- **Priority:** Medium
- **Effort:** 6h
- **Files:** `TabGroup.kt`, `TabViewModel.kt`, `TabSwitcherView.kt`, `TabDatabase.kt`
- **Agent:** Feature-Agent
- **Dependencies:** None
- **Description:** Group tabs with labels and colors
- **Steps:**
  1. Create TabGroup data model
  2. Add group CRUD operations to ViewModel
  3. Update UI for group creation/assignment
  4. Implement drag-and-drop grouping
  5. Add collapse/expand groups
  6. Persist groups in database
- **Tests:** Group creation test, assignment test, persistence test

#### T016: Implement File Downloads (FR-016)
- **Priority:** Critical
- **Effort:** 5h
- **Files:** `DownloadManager.kt`, `WebViewClient.kt`, `DownloadScreen.kt`, `BrowserViewModel.kt`
- **Agent:** Feature-Agent
- **Dependencies:** None
- **Description:** Handle file downloads with progress and management
- **Steps:**
  1. Override onDownloadStart in WebViewClient
  2. Create DownloadManager with WorkManager
  3. Show download notification with progress
  4. Create Downloads screen (list with status)
  5. Add "Open" and "Delete" actions
  6. Persist download history
- **Tests:** Download initiation test, progress tracking test, file access test

---

### Phase 2: High Priority Features (T017-T030)

#### T017: Fix Default Homepage Loading (FR-017)
- **Priority:** High
- **Effort:** 0.5h
- **Files:** `BrowserViewModel.kt`
- **Agent:** State-Agent
- **Dependencies:** None
- **Description:** Load homepage on app start
- **Steps:**
  1. Check if current URL is empty on init
  2. Load settings.defaultHomepage
  3. Update WebView
- **Tests:** App launch test

#### T018: Remove Redundant Command Bar Buttons (FR-018)
- **Priority:** High
- **Effort:** 0.5h
- **Files:** `BottomCommandBar.kt`
- **Agent:** UI-Agent
- **Dependencies:** T003
- **Description:** Remove Prev/Next/Reload buttons
- **Steps:**
  1. Delete button composables
  2. Test remaining layout
- **Tests:** UI snapshot test

#### T019: Implement Favorites Screen with History (FR-009)
- **Priority:** High
- **Effort:** 3h
- **Files:** `FavoritesScreen.kt`, `FavoritesList.kt`, `FavoritesARView.kt`, `AddressBar.kt`
- **Agent:** UI-Agent
- **Dependencies:** None
- **Description:** Dual tab screen with List and AR views
- **Steps:**
  1. Create FavoritesScreen with TabRow (Favorites/History)
  2. Implement List view (LazyColumn)
  3. Implement AR view with arc layout (BoxWithConstraints for orientation)
  4. Add view mode toggle button
  5. Update AddressBar star icon to open Favorites
  6. Add voice commands
- **Tests:** Tab switching test, view mode toggle test, orientation test

#### T020: Add Favorites/History Voice Commands (FR-009)
- **Priority:** High
- **Effort:** 1h
- **Files:** `VoiceCommandProcessor.kt`
- **Agent:** State-Agent
- **Dependencies:** T019
- **Description:** Voice commands for Favorites/History navigation
- **Steps:**
  1. Add command patterns: "show history", "show favorites", "AR view", "list view"
  2. Implement navigation logic
  3. Test command recognition
- **Tests:** Voice command recognition test

#### T021: Create ArcLayout Composable (FR-009)
- **Priority:** High
- **Effort:** 4h
- **Files:** `ArcLayout.kt`
- **Agent:** UI-Agent
- **Dependencies:** None
- **Description:** Reusable arc/carousel layout with orientation support
- **Steps:**
  1. Create custom Layout with arc calculations
  2. Support HORIZONTAL and VERTICAL orientations
  3. Implement swipe gesture handling
  4. Add scaling (center 1.0x, adjacent 0.6x)
  5. Smooth animations (300ms)
  6. Test portrait and landscape
- **Tests:** Layout measurement test, gesture test, orientation test

#### T022: Implement Tab Groups UI (FR-015)
- **Priority:** Medium
- **Effort:** 4h
- **Files:** `TabGroupSheet.kt`, `TabSwitcherView.kt`
- **Agent:** UI-Agent
- **Dependencies:** T015
- **Description:** UI for creating and managing tab groups
- **Steps:**
  1. Create group creation dialog
  2. Add group assignment bottom sheet
  3. Implement drag-and-drop
  4. Add group header with collapse/expand
  5. Color picker for groups
- **Tests:** UI interaction test, drag-and-drop test

#### T023: Implement Download Notifications (FR-016)
- **Priority:** High
- **Effort:** 2h
- **Files:** `DownloadNotification.kt`
- **Agent:** Feature-Agent
- **Dependencies:** T016
- **Description:** System notifications for downloads
- **Steps:**
  1. Create notification channel
  2. Show progress notification
  3. Update on completion/failure
  4. Add open/cancel actions
- **Tests:** Notification display test

#### T024: Create Downloads Screen (FR-016)
- **Priority:** High
- **Effort:** 2h
- **Files:** `DownloadsScreen.kt`
- **Agent:** UI-Agent
- **Dependencies:** T016
- **Description:** List view of all downloads with actions
- **Steps:**
  1. Create screen with LazyColumn
  2. Show download status (in progress, completed, failed)
  3. Add open file action
  4. Add delete action
  5. Filter by status
- **Tests:** List display test, action test

#### T025: Implement Favorite Thumbnail Cards (FR-009)
- **Priority:** Medium
- **Effort:** 2h
- **Files:** `FavoriteThumbnailCard.kt`
- **Agent:** UI-Agent
- **Dependencies:** T019
- **Description:** Card for favorites in AR view
- **Steps:**
  1. Create Card with AsyncImage
  2. Show thumbnail or favicon
  3. Add glassmorphic title overlay
  4. Scale based on position (center/adjacent)
  5. Handle long-press for delete
- **Tests:** Card rendering test, gesture test

#### T026: Implement History List View (FR-009)
- **Priority:** Medium
- **Effort:** 1h
- **Files:** `HistoryList.kt`
- **Agent:** UI-Agent
- **Dependencies:** T019
- **Description:** Standard list view for browsing history
- **Steps:**
  1. LazyColumn with history items
  2. Group by date
  3. Show title, URL, timestamp
  4. Swipe to delete
- **Tests:** List rendering test, delete gesture test

#### T027: Implement History AR View (FR-009)
- **Priority:** Medium
- **Effort:** 1h
- **Files:** `HistoryARView.kt`
- **Agent:** UI-Agent
- **Dependencies:** T021
- **Description:** Arc layout for history items
- **Steps:**
  1. Reuse ArcLayout composable
  2. Use same orientation logic as Favorites
  3. Show history thumbnails
- **Tests:** Arc rendering test

#### T028: Add View Mode Preference Persistence (FR-009)
- **Priority:** Medium
- **Effort:** 0.5h
- **Files:** `BrowserSettings.kt`, `FavoritesViewModel.kt`
- **Agent:** State-Agent
- **Dependencies:** T019
- **Description:** Remember user's preferred view mode
- **Steps:**
  1. Add `favoritesViewMode: ViewMode` to settings
  2. Save on change
  3. Load on screen open
- **Tests:** Persistence test

#### T029: Create NetworkMonitor Service (FR-008)
- **Priority:** High
- **Effort:** 2h
- **Files:** `NetworkMonitor.kt`
- **Agent:** Feature-Agent
- **Dependencies:** None
- **Description:** Background service for connectivity monitoring
- **Steps:**
  1. Register ConnectivityManager callback
  2. Emit connectivity state via Flow
  3. Detect online/offline transitions
  4. Clean up on destroy
- **Tests:** Connectivity state test

#### T030: Implement Auto-Reload on Reconnect (FR-008)
- **Priority:** High
- **Effort:** 1h
- **Files:** `BrowserViewModel.kt`
- **Agent:** State-Agent
- **Dependencies:** T029, T011
- **Description:** Auto-reload failed pages when network returns
- **Steps:**
  1. Track failed loads due to network
  2. Listen to NetworkMonitor
  3. Reload on reconnection
  4. Show success message
- **Tests:** Reconnection reload test

---

### Phase 3: External APIs (T031-T040)

#### T031: Implement Headless Mode Intent API (FR-019)
- **Priority:** Critical
- **Effort:** 2h
- **Files:** `HeadlessModeReceiver.kt`, `AndroidManifest.xml`
- **Agent:** API-Agent
- **Dependencies:** T008
- **Description:** Intent-based API for external apps to toggle headless mode
- **Steps:**
  1. Create BroadcastReceiver for headless mode Intents
  2. Register in manifest with permissions
  3. Validate caller signature
  4. Update BrowserViewModel state
  5. Document Intent format
- **Tests:** Intent handling test, permission test

#### T032: Implement Browser Control Intent API (FR-020)
- **Priority:** Critical
- **Effort:** 4h
- **Files:** `BrowserControlReceiver.kt`, `AndroidManifest.xml`, `IntentActions.kt`
- **Agent:** API-Agent
- **Dependencies:** T031
- **Description:** Comprehensive Intent API for browser control
- **Steps:**
  1. Create receiver for actions: NAVIGATE, BACK, FORWARD, RELOAD, NEW_TAB, CLOSE_TAB, SWITCH_TAB
  2. Implement action handlers
  3. Add signature validation
  4. Return result broadcasts
  5. Document all actions with examples
- **Tests:** All action tests, validation test

#### T033: Create Developer SDK Library (FR-021)
- **Priority:** Medium
- **Effort:** 6h
- **Files:** `webavanue-sdk/` (new module)
- **Agent:** API-Agent
- **Dependencies:** T032
- **Description:** Android library for easy integration
- **Steps:**
  1. Create new Gradle module
  2. Implement WebAvanueController class
  3. Wrap all Intent APIs with methods
  4. Add callback interfaces
  5. Write documentation and sample app
  6. Publish to Maven Local
- **Tests:** SDK method tests, integration test with sample app

#### T034: Implement JavaScript Execution API (FR-022)
- **Priority:** Critical
- **Effort:** 3h
- **Files:** `JavaScriptExecutor.kt`, `BrowserControlReceiver.kt`
- **Agent:** API-Agent
- **Dependencies:** T032
- **Description:** Execute JavaScript via Intent and return result
- **Steps:**
  1. Add EXECUTE_JAVASCRIPT action to receiver
  2. Validate script length and content
  3. Execute via WebView.evaluateJavascript()
  4. Return result via broadcast
  5. Handle errors and timeouts
- **Tests:** Execution test, result test, error handling test

#### T035: Implement Page Lifecycle Callbacks (FR-023)
- **Priority:** Critical
- **Effort:** 2h
- **Files:** `PageLifecycleManager.kt`, `WebViewClient.kt`
- **Agent:** API-Agent
- **Dependencies:** T032
- **Description:** Broadcast page load events to subscribed apps
- **Steps:**
  1. Override WebViewClient callbacks
  2. Broadcast: PAGE_STARTED, PAGE_FINISHED, PAGE_ERROR, TITLE_CHANGED
  3. Include URL, title, error details
  4. Subscribe/unsubscribe mechanism
- **Tests:** Callback test for all events

#### T036: Implement Screenshot Capture API (FR-024)
- **Priority:** High
- **Effort:** 3h
- **Files:** `ScreenshotManager.kt`, `BrowserControlReceiver.kt`
- **Agent:** API-Agent
- **Dependencies:** T032
- **Description:** Capture page screenshots via Intent
- **Steps:**
  1. Add CAPTURE_SCREENSHOT action
  2. Implement WebView bitmap capture
  3. Save to cache directory
  4. Return file URI via broadcast
  5. Add cleanup after 24h
  6. Support visible area and full page options
- **Tests:** Capture test, file access test, cleanup test

#### T037: Implement WebXR Detection API (FR-025)
- **Priority:** High
- **Effort:** 2h
- **Files:** `WebXRDetector.kt`, `BrowserControlReceiver.kt`
- **Agent:** API-Agent
- **Dependencies:** T032
- **Description:** Detect WebXR session start/end
- **Steps:**
  1. Inject JavaScript to monitor navigator.xr
  2. Listen for session requests
  3. Broadcast WEBXR_SESSION_STARTED/ENDED
  4. Include session type (immersive-vr, immersive-ar)
- **Tests:** Detection test, broadcast test

#### T038: Implement Zoom Control API (FR-026)
- **Priority:** Medium
- **Effort:** 1.5h
- **Files:** `ZoomController.kt`, `BrowserControlReceiver.kt`
- **Agent:** API-Agent
- **Dependencies:** T032
- **Description:** Control page zoom via Intent
- **Steps:**
  1. Add ZOOM_IN, ZOOM_OUT, SET_ZOOM actions
  2. Clamp zoom levels (50%-300%)
  3. Update WebView settings
  4. Return current zoom level
- **Tests:** Zoom test, bounds test

#### T039: Implement Cookie Management API (FR-027)
- **Priority:** Medium
- **Effort:** 2h
- **Files:** `CookieController.kt`, `BrowserControlReceiver.kt`
- **Agent:** API-Agent
- **Dependencies:** T032
- **Description:** Read/write cookies via Intent
- **Steps:**
  1. Add GET_COOKIES, SET_COOKIE, CLEAR_COOKIES actions
  2. Access CookieManager
  3. Validate cookie format
  4. Return cookie data via broadcast
- **Tests:** Cookie CRUD tests

#### T040: Implement Find in Page API (FR-028)
- **Priority:** Medium
- **Effort:** 1.5h
- **Files:** `FindController.kt`, `BrowserControlReceiver.kt`
- **Agent:** API-Agent
- **Dependencies:** T032
- **Description:** Search page content via Intent
- **Steps:**
  1. Add FIND_IN_PAGE, FIND_NEXT, FIND_PREVIOUS, CLEAR_FIND actions
  2. Use WebView.findAllAsync()
  3. Return match count and current index
  4. Highlight matches
- **Tests:** Find test, navigation test

---

### Phase 4: Validation & Integration (T041-T045)

#### T041: Implement Feature Compatibility System (FR-029)
- **Priority:** Critical
- **Effort:** 2h
- **Files:** `FeatureCompatibility.kt`
- **Agent:** Integration-Agent
- **Dependencies:** T031-T040
- **Description:** Feature detection and graceful degradation
- **Steps:**
  1. Create feature detection utility
  2. Check API level, WebView version
  3. Return availability status
  4. Generate error messages
- **Tests:** Compatibility detection tests

#### T042: Create Feature Unavailable Dialog (FR-029)
- **Priority:** Critical
- **Effort:** 1h
- **Files:** `FeatureUnavailableDialog.kt`
- **Agent:** UI-Agent
- **Dependencies:** T041
- **Description:** Themed error dialog for unsupported features
- **Steps:**
  1. Create AlertDialog with Material3 theming
  2. Show feature name and reason
  3. Include upgrade suggestion
  4. Use theme colors
- **Tests:** Dialog display test, theming test

#### T043: Integration Testing (All FRs)
- **Priority:** Critical
- **Effort:** 6h
- **Files:** `androidTest/` directory
- **Agent:** Integration-Agent
- **Dependencies:** T001-T042
- **Description:** End-to-end tests for all features
- **Steps:**
  1. Test UI flows (headless mode, favorites, downloads)
  2. Test API integrations (Intents, callbacks)
  3. Test orientation changes
  4. Test error scenarios
  5. Test accessibility
- **Tests:** 50+ integration tests

#### T044: Update Documentation
- **Priority:** High
- **Effort:** 3h
- **Files:** `docs/webavanue/`
- **Agent:** Integration-Agent
- **Dependencies:** T043
- **Description:** Update all documentation for v1.9.0
- **Steps:**
  1. Update user manual with new features
  2. Write API documentation for developers
  3. Create Intent API reference
  4. Update changelog
  5. Create migration guide from v1.8.1
- **Tests:** Documentation review

#### T045: Final Validation and Build
- **Priority:** Critical
- **Effort:** 2h
- **Files:** All
- **Agent:** Integration-Agent
- **Dependencies:** T001-T044
- **Description:** Final checks and release build
- **Steps:**
  1. Run full test suite (unit + integration)
  2. Check code coverage (target 90%+)
  3. Run linters and static analysis
  4. Build release APK
  5. Test on physical devices (HMT-1, standard Android)
  6. Version bump to 1.9.0
- **Tests:** All tests passing, 0 blockers, 0 warnings

---

## Summary

**Total Tasks:** 45
**Total Effort:** ~90 hours sequential, ~50 hours with swarm (45% time savings)
**Swarm Agents:** 5 (UI, State, Feature, API, Integration)
**Test Coverage Target:** 90%+
**Quality Gates:** 0 blockers, 0 warnings, all tests passing

**Execution Order:**
1. Phase 1 (Critical): T001-T016 (parallel where possible)
2. Phase 2 (High): T017-T030 (parallel where possible)
3. Phase 3 (APIs): T031-T040 (parallel where possible)
4. Phase 4 (Validation): T041-T045 (mostly sequential)

**Key Dependencies:**
- T001 → T002 (state foundation)
- T008 → T031 (headless mode for API)
- T019 → T020, T021, T025-T028 (Favorites screen dependencies)
- T031 → T032-T040 (Intent API foundation)
- T041 → T042 (error handling)
- All → T043-T045 (final validation)
