# WebAvanue UI/UX Fixes Specification

**Version:** 1.0
**Date:** 2025-12-05
**Status:** Ready for Implementation
**Platform:** Android (KMP)

---

## Executive Summary

Fix 8 outstanding UI/UX issues identified in WebAvanue browser testing. Issues span visual styling (FAB), layout adaptations (VoiceCommandDialog), missing features (Headless Mode, Network Status), incorrect behaviors (Star icon, Command Bar toggle), and minor design updates (Tab icons).

---

## Problem Statement

| Current State | Pain Point | Desired State |
|---------------|------------|---------------|
| FAB uses solid color | Doesn't match glassmorphic theme | Glass-like appearance |
| VoiceCommandDialog single column | Commands don't fit in landscape | Multi-column adaptive layout |
| No auto-timeout setting exposed | Users can't configure | Toggle in Settings UI |
| Headless mode not triggerable | Can't view fullscreen web | UI toggle for true fullscreen |
| Network status not monitored | No offline warning | Alert when disconnected |
| Star icon = Favorites | Duplicates bookmark | Should open History |
| Command bar toggle out of sync | Icon shows wrong state | Accurate state reflection |
| Tab icons generic | Need visual refresh | Custom redesigned icons |

---

## Functional Requirements

### FR-001: FAB Glass-Like Style
| ID | Requirement |
|----|-------------|
| FR-001.1 | Replace solid `Surface` with `GlassCard` component |
| FR-001.2 | Use `GlassLevel.MEDIUM` for visibility with blur effect |
| FR-001.3 | Maintain `zIndex(10f)` for proper layering |
| FR-001.4 | Keep 48dp touch target for accessibility |

**File:** `BrowserScreen.kt:577-607`

### FR-002: VoiceCommandDialog Horizontal Columns
| ID | Requirement |
|----|-------------|
| FR-002.1 | Add `BoxWithConstraints` to detect landscape orientation |
| FR-002.2 | In landscape: Display commands in 2-3 columns using `LazyVerticalGrid` |
| FR-002.3 | All command badges must have fixed width (120.dp minimum) |
| FR-002.4 | Category buttons must have equal height (64.dp) |

**File:** `VoiceCommandsDialog.kt:120-337`

### FR-003: VoiceCommandDialog Auto-Timeout Setting
| ID | Requirement |
|----|-------------|
| FR-003.1 | Add setting item in SettingsScreen under "Voice & AI" section |
| FR-003.2 | Toggle for `voiceDialogAutoClose` (existing in BrowserSettings) |
| FR-003.3 | Slider for `voiceDialogAutoCloseDelayMs` (500-5000ms range) |
| FR-003.4 | Settings must persist via SettingsViewModel |

**Files:** `SettingsScreen.kt`, `BrowserSettings.kt` (already has fields)

### FR-004: Headless Browser Mode
| ID | Requirement |
|----|-------------|
| FR-004.1 | Add `isHeadlessMode` state to `BrowserScreen` |
| FR-004.2 | In headless mode: Hide AddressBar, hide FAB, show only BottomCommandBar |
| FR-004.3 | Add toggle button in MENU command bar level |
| FR-004.4 | Add gesture: Double-tap status bar area to toggle headless mode |
| FR-004.5 | Show brief toast/indicator when entering/exiting headless mode |

**Files:** `BrowserScreen.kt`, `BottomCommandBar.kt`

### FR-005: Tab Switcher Grid/List Icons
| ID | Requirement |
|----|-------------|
| FR-005.1 | Replace `Icons.Default.GridView` with custom 2x2 grid icon |
| FR-005.2 | Replace `Icons.Default.ViewList` with custom 3-line list icon |
| FR-005.3 | Icons should be 16dp with clear visual distinction |

**File:** `TabSwitcherView.kt:222-258`

### FR-006: Network Status Alert
| ID | Requirement |
|----|-------------|
| FR-006.1 | Create `NetworkStatusMonitor` expect/actual for platform-specific monitoring |
| FR-006.2 | Android: Use `ConnectivityManager` + `NetworkCallback` |
| FR-006.3 | Wire `NetworkStatusIndicator` to `BrowserScreen` at top overlay position |
| FR-006.4 | Show DISCONNECTED state immediately on network loss |
| FR-006.5 | Show RECONNECTING with spinner during recovery attempts |

**Files:** `NetworkStatusIndicator.kt`, `NetworkStatusMonitor.kt` (new), `BrowserScreen.kt`

### FR-007: Star Icon = History
| ID | Requirement |
|----|-------------|
| FR-007.1 | Change Star icon to `Icons.Default.History` |
| FR-007.2 | Tap: Navigate to History screen (`onHistoryClick`) |
| FR-007.3 | Long-press: Show recent history dropdown (last 5 items) |
| FR-007.4 | Remove `onAddFavorite` binding from star button |
| FR-007.5 | Update content description to "History" |

**Files:** `AddressBar.kt:263-280` (portrait), `AddressBar.kt:537-555` (landscape)

### FR-008: Command Bar Toggle State Sync
| ID | Requirement |
|----|-------------|
| FR-008.1 | Toggle icon must reflect `isCommandBarVisible`, not `isVoiceMode` |
| FR-008.2 | When command bar is manually hidden via `onHide`, toggle should show "show" icon |
| FR-008.3 | Add new icon pair: `KeyboardArrowUp` (show bar) / `KeyboardArrowDown` (hide bar) |
| FR-008.4 | Keep separate Mic button for voice activation |
| FR-008.5 | Prevent auto-hide from triggering while user is interacting |

**Files:** `BrowserScreen.kt:116-125`, `AddressBar.kt:369-397`

---

## Non-Functional Requirements

| ID | Requirement | Target |
|----|-------------|--------|
| NFR-001 | All UI changes must not increase frame drop | <16ms render |
| NFR-002 | Settings changes must persist immediately | No data loss |
| NFR-003 | Network status must update within 2 seconds | Real-time feel |
| NFR-004 | Touch targets must meet accessibility minimum | 48dp |
| NFR-005 | Changes must work in both portrait and landscape | 100% coverage |

---

## Technical Constraints

| Constraint | Value |
|------------|-------|
| Min Android API | 24 |
| Compose Version | 1.5+ |
| Material3 | Required |
| OceanTheme | Mandatory for all components |

---

## Implementation Order (Dependencies)

| Order | Issue | Depends On |
|-------|-------|------------|
| 1 | FR-008 Command Bar Toggle | None |
| 2 | FR-001 FAB Glass Style | None |
| 3 | FR-005 Tab Icons | None |
| 4 | FR-002 VoiceCommandDialog Columns | None |
| 5 | FR-003 Auto-Timeout Setting | FR-002 (same component) |
| 6 | FR-007 Star → History | None |
| 7 | FR-006 Network Status | Requires expect/actual |
| 8 | FR-004 Headless Mode | FR-008 (command bar state) |

---

## Success Criteria

- [ ] FAB has glassmorphic appearance matching OceanTheme
- [ ] VoiceCommandDialog shows 2-3 columns in landscape with no scrolling needed
- [ ] Settings screen has Voice Dialog auto-timeout toggle
- [ ] Headless mode can be activated and shows full-screen web content
- [ ] Tab switcher has visually distinct grid/list toggle icons
- [ ] Network disconnection shows immediate alert overlay
- [ ] Star icon opens History, not Favorites
- [ ] Command bar toggle icon accurately reflects visibility state
- [ ] All changes work in portrait and landscape orientations

---

## Files to Modify

| File | Changes |
|------|---------|
| `BrowserScreen.kt` | FR-001, FR-004, FR-006, FR-008 |
| `VoiceCommandsDialog.kt` | FR-002, FR-003 |
| `TabSwitcherView.kt` | FR-005 |
| `AddressBar.kt` | FR-007, FR-008 |
| `BottomCommandBar.kt` | FR-004 |
| `NetworkStatusIndicator.kt` | FR-006 |
| `SettingsScreen.kt` | FR-003 |
| *NEW* `NetworkStatusMonitor.kt` | FR-006 (expect/actual) |

---

## Swarm Assessment

| Metric | Value | Trigger |
|--------|-------|---------|
| Platforms | 1 (Android) | NO |
| Tasks | 8 | NO |
| Complexity | Medium | NO |

**Recommendation:** Sequential implementation, no swarm needed.
