# Cockpit Window Controls & State Management - Feature Specification

**Version:** 1.0
**Date:** 2025-12-10
**Status:** Draft
**Platform:** Android (Jetpack Compose)
**Priority:** HIGH

---

## Executive Summary

Implement missing window management features from legacy Task_Cockpit to bring our new Cockpit MVP to feature parity. This includes window control buttons (minimize, maximize), selection states, content state persistence, and enhanced WebView capabilities. Implementation will use Compose best practices while maintaining the modern Ocean Theme design.

**Key Features:**
1. Window control buttons (minimize, maximize, close)
2. Window selection and focus states
3. Content state persistence (scroll positions, playback states)
4. Enhanced WebView features (desktop mode, dynamic titles, auth)

**Success Criteria:**
- ✅ All window controls functional with haptic feedback
- ✅ Selection state visually distinct from focus state
- ✅ PDF/WebView scroll positions persist across resets
- ✅ WebView titles update dynamically from page content

---

## Problem Statement

**Current State:**
Our Cockpit MVP has basic window management but lacks critical features present in the legacy Task_Cockpit implementation:
- Windows can only be closed, not minimized or maximized
- No visual distinction between selected and unselected windows
- Content state (scroll positions, zoom levels) is lost on window recreation
- WebView features are basic (no desktop mode, static titles, no auth handling)

**Pain Points:**
1. Users cannot temporarily hide windows without closing them
2. Cannot enlarge important windows while keeping others small
3. Lose their place in PDFs/web pages when switching windows
4. Web pages don't display properly without desktop mode
5. No way to know which window is actively selected vs just focused

**Desired State:**
Full-featured window management matching legacy functionality with modern Compose UI and improved UX through haptic feedback and smooth animations.

---

## Functional Requirements

### FR-1: Window Control Buttons (CRITICAL)

**FR-1.1: Minimize Button**
- Icon: Dash/Minimize icon (Material Icons)
- Action: Sets `window.isHidden = true`
- Callback: `onMinimize(windowId: String)`
- Visual: Hidden windows show as collapsed bar (title only, 48dp height)
- Haptic: Medium tap on click
- Location: Title bar, before maximize button

**FR-1.2: Maximize Button**
- Icon: Maximize/Restore icon (Material Icons, changes based on state)
- Action: Toggles `window.isLarge` boolean
- Callback: `onToggleSize(windowId: String)`
- Visual:
  - Normal: 300x400dp (current)
  - Large: 600x800dp (2x size)
- Haptic: Medium tap on click
- Location: Title bar, before close button

**FR-1.3: Close Button** ✅ (Already Implemented)
- Icon: Close X icon
- Action: Removes window from workspace
- Callback: `onClose()`
- Haptic: Light tap on click
- Location: Title bar, rightmost

### FR-2: Window Selection State (CRITICAL)

**FR-2.1: Selection Model**
- Add `selectedWindowId: StateFlow<String?>` to WorkspaceViewModel
- Method: `selectWindow(windowId: String)`
- Only ONE window can be selected at a time
- Selection persists until another window selected or window closed

**FR-2.2: Visual Indicators**
```
State           | Border    | Title Bar         | Elevation
----------------|-----------|-------------------|----------
Unselected      | 1dp gray  | Normal gradient   | 4dp
Selected        | 2dp blue  | Blue gradient     | 8dp
Hidden          | 0dp       | Collapsed         | 0dp
```

**FR-2.3: Selection Interaction**
- Click anywhere on window title bar → selects window
- Click on window content area → selects window (NEW)
- Keyboard shortcut: Number keys 1-6 select windows by position (FUTURE)

### FR-3: Content State Persistence (HIGH)

**FR-3.1: WebView Scroll Position**
- Extend `WindowContent.WebContent` with:
  ```kotlin
  data class WebContent(
      val url: String,
      // ... existing fields
      val scrollX: Int = 0,
      val scrollY: Int = 0
  )
  ```
- Save scroll position on window deselection
- Restore scroll position on WebView load complete

**FR-3.2: PDF State**
- Extend `WindowContent.DocumentContent` for PDF with:
  ```kotlin
  // For PDF documents only
  val currentPage: Int = 0
  val zoomLevel: Float = 1.0f
  val scrollX: Float = 0f
  val scrollY: Float = 0f
  ```
- Save on page change, zoom change, scroll
- Restore on PDF reload

**FR-3.3: Video Playback Position**
- Add to DocumentContent for VIDEO type:
  ```kotlin
  val playbackPosition: Long = 0L  // milliseconds
  ```
- Save position every 5 seconds during playback
- Restore on video reload, auto-resume if was playing

**FR-3.4: Timestamps**
- Add to AppWindow:
  ```kotlin
  val createdAt: Long = System.currentTimeMillis()
  val updatedAt: Long = System.currentTimeMillis()
  ```
- Update `updatedAt` on any content state change
- Display relative time in window metadata (optional)

### FR-4: Enhanced WebView Features (MEDIUM)

**FR-4.1: Desktop Mode Toggle**
- Add `isDesktopMode: Boolean` to WebContent
- Default: `true` (better rendering on AR glasses)
- User agent override: Desktop Chrome UA
- UI: Toggle in window controls or settings (FUTURE)

**FR-4.2: Dynamic Title Updates**
- WebView title change listener
- Update window title when page title changes
- Fallback: Use URL if title empty
- Max length: 30 characters with ellipsis

**FR-4.3: HTTP Basic Auth**
- Detect basic auth challenge
- Show AlertDialog with username/password fields
- Store credentials per domain (optional, FUTURE)
- Handle auth failure/retry

**FR-4.4: New Tab Handling**
- Intercept `window.open()` calls
- Create new window with target URL
- Add to workspace automatically
- Animate new window slide-in (FUTURE)

---

## Non-Functional Requirements

### NFR-1: Performance
- Window state updates: <16ms (60 FPS)
- Minimize/maximize animation: 300ms duration
- Scroll position save: Debounced 500ms
- WebView title update: Immediate (no debounce)

### NFR-2: Accessibility
- All buttons: 48dp minimum touch target
- Button labels: Clear content descriptions
- High contrast: 4.5:1 minimum ratio
- Haptic feedback: Consistent patterns (light=12ms, medium=25ms)

### NFR-3: State Management
- All state in ViewModel (single source of truth)
- State persistence: Survive process death (FUTURE)
- Undo/redo: Support window size/hide changes (FUTURE)

### NFR-4: Visual Design
- Ocean Theme compliance: All colors from theme palette
- Animations: Material Design 3 motion specs
- Spacing: 8dp grid system
- Icons: Material Icons filled variant

---

## Platform-Specific Details

### Android (Jetpack Compose)

**Tech Stack:**
- UI: Jetpack Compose (already in use)
- State: StateFlow + ViewModel (already in use)
- Animation: `animateDpAsState`, `AnimatedVisibility`
- Haptics: `HapticFeedbackManager` (already in use)

**Components:**
1. **WindowControlBar.kt** (NEW)
   - Composable with minimize/maximize/close buttons
   - Material Icon buttons with haptic feedback
   - 48dp height, horizontal arrangement

2. **SelectableWindowCard.kt** (UPDATE WindowCard.kt)
   - Add `isSelected` parameter
   - Border modifier based on selection state
   - Click listener for selection

3. **ContentStateSaver.kt** (NEW)
   - Utility class for content state persistence
   - Methods: `saveWebViewState()`, `savePdfState()`, `saveVideoState()`

4. **EnhancedWebViewContent.kt** (UPDATE)
   - WebChromeClient override for title updates
   - HttpAuthHandler integration
   - Desktop mode UA override
   - Scroll position listener

**Dependencies:**
- No new external dependencies required
- Use existing: Compose Material3, AndroidX ViewModel, Kotlin Coroutines

**Testing Strategy:**
- Unit tests: ViewModel state transitions (minimize, maximize, select)
- UI tests: Button clicks, visual state changes (Espresso/Compose Testing)
- Integration tests: State persistence across rotations
- Manual tests: Haptic feedback, animations smoothness

---

## User Stories

### US-1: Minimize Window
**As a** user
**I want to** minimize windows I'm not currently using
**So that** I can reduce clutter while keeping them accessible

**Acceptance Criteria:**
- ✅ Click minimize button hides window content
- ✅ Hidden window shows as collapsed title bar
- ✅ Click on collapsed window restores it
- ✅ Haptic feedback on minimize action

### US-2: Maximize Window
**As a** user
**I want to** enlarge important windows
**So that** I can see more content detail

**Acceptance Criteria:**
- ✅ Click maximize toggles between normal (300x400dp) and large (600x800dp)
- ✅ Icon changes to restore icon when maximized
- ✅ Other windows remain at current size
- ✅ Smooth size transition animation (300ms)

### US-3: Select Window
**As a** user
**I want to** explicitly select a window
**So that** voice commands and keyboard shortcuts target the correct window

**Acceptance Criteria:**
- ✅ Click window title bar selects window
- ✅ Selected window has blue border (2dp) and elevated appearance
- ✅ Previously selected window returns to normal state
- ✅ Only one window selected at a time

### US-4: Persistent PDF State
**As a** user reading a PDF
**I want to** keep my place when switching windows
**So that** I don't lose my reading position

**Acceptance Criteria:**
- ✅ Switching away from PDF window saves page number, zoom, scroll position
- ✅ Returning to PDF window restores exact previous state
- ✅ State persists across workspace resets
- ✅ Multiple PDF windows maintain independent states

### US-5: Dynamic WebView Titles
**As a** user browsing web pages
**I want to** see the actual page title in the window
**So that** I can identify windows by content

**Acceptance Criteria:**
- ✅ Window title updates when page title loads
- ✅ Title truncates with ellipsis after 30 characters
- ✅ Falls back to URL if page has no title
- ✅ Update happens within 500ms of page load

### US-6: HTTP Basic Auth
**As a** user accessing authenticated web resources
**I want to** enter credentials when prompted
**So that** I can access protected content

**Acceptance Criteria:**
- ✅ Basic auth challenge shows dialog with username/password fields
- ✅ Credentials submitted to WebView auth handler
- ✅ Failed auth shows error and allows retry
- ✅ Successful auth loads protected content

---

## Technical Constraints

### Minimum Requirements
- Android API Level: 28 (already set)
- Kotlin Version: 1.9.25 (already set)
- Compose BOM: 2024.02.00+ (check current version)

### Framework Constraints
- Must use existing WorkspaceViewModel (don't create new ViewModels)
- Must maintain Ocean Theme color palette
- Must use existing HapticFeedbackManager
- Must not break existing spatial mode rendering

### Performance Constraints
- Max memory per window: 50MB (WebView can exceed, acceptable)
- State update latency: <16ms
- Animation frame rate: 60 FPS minimum

---

## Dependencies

### Implementation Order
1. **Phase 1** - Window Controls (FIRST)
   - Update AppWindow data class
   - Create WindowControlBar composable
   - Add ViewModel methods (minimize, maximize)
   - Update WindowCard integration

2. **Phase 2** - Selection State (SECOND)
   - Add selectedWindowId to ViewModel
   - Update WindowCard visual states
   - Implement selection logic

3. **Phase 3** - State Persistence (THIRD)
   - Extend WindowContent types
   - Create state saver utilities
   - Update content renderers (WebView, PDF, Video)

4. **Phase 4** - WebView Enhancements (FOURTH)
   - Desktop mode implementation
   - Dynamic title updates
   - Basic auth dialog
   - New tab handling

### External Dependencies
- None (all features use existing libraries)

### Cross-Component Dependencies
```
AppWindow → WindowContent → Content Renderers
     ↓
WindowCard → WindowControlBar
     ↓
WorkspaceViewModel (manages all state)
```

---

## Implementation Plan Summary

### Phase 1: Window Controls (2-3 hours)
- [ ] Update AppWindow: Add `isHidden`, `isLarge`, `createdAt`, `updatedAt`
- [ ] Create WindowControlBar composable (minimize, maximize, close buttons)
- [ ] Update WorkspaceViewModel: Add `minimizeWindow()`, `toggleWindowSize()`, `selectWindow()`
- [ ] Update WindowCard: Integrate control bar, size animation
- [ ] Test: All buttons functional with haptic feedback

### Phase 2: Selection State (1-2 hours)
- [ ] Add `selectedWindowId` StateFlow to ViewModel
- [ ] Update WindowCard: Selection visual states (border, elevation, gradient)
- [ ] Add click handlers for selection
- [ ] Test: Visual states correct, only one window selected

### Phase 3: State Persistence (3-4 hours)
- [ ] Extend WindowContent with state fields (scroll, page, zoom)
- [ ] Create ContentStateSaver utility class
- [ ] Update WebViewContent: Save/restore scroll position
- [ ] Update DocumentViewerContent: Save/restore PDF/video state
- [ ] Test: State persists across window switches

### Phase 4: WebView Enhancements (2-3 hours)
- [ ] Add desktop mode toggle (user agent override)
- [ ] Implement WebChromeClient for title updates
- [ ] Create BasicAuthDialog composable
- [ ] Add new tab interception logic
- [ ] Test: All WebView features working

### Total Estimated Time: 8-12 hours

---

## Success Criteria

### Functional Completeness
- ✅ All 3 window control buttons (minimize, maximize, close) implemented and functional
- ✅ Window selection state visually distinct with proper keyboard/click interactions
- ✅ PDF scroll position, page number, and zoom level persist across window switches
- ✅ WebView scroll position and playback position persist across resets
- ✅ WebView titles update dynamically from page content within 500ms

### Quality Metrics
- ✅ All animations run at 60 FPS (verified with GPU profiling)
- ✅ Touch targets ≥48dp (accessibility requirement)
- ✅ Haptic feedback consistent across all button interactions
- ✅ No regressions in existing spatial mode or 2D mode functionality
- ✅ Zero critical bugs, zero high-priority bugs before deployment

### User Experience
- ✅ Window management feels smooth and responsive
- ✅ State persistence is transparent (users don't notice it)
- ✅ Visual feedback is immediate and clear
- ✅ Features work consistently in both 2D and spatial modes

---

## Open Questions

1. **State Persistence Scope:** Should state persist across app restarts (requires serialization)?
   - **Recommendation:** Phase 2 feature, start with in-memory only

2. **Window Numbering:** Should we show frame numbers like legacy (Frame 1, Frame 2)?
   - **Recommendation:** Yes, helpful for voice commands ("close frame 2")

3. **Maximum Window Size:** Should large windows be fullscreen or just 2x?
   - **Recommendation:** 2x initially (600x800dp), fullscreen as advanced option

4. **Hidden Window Interaction:** Can hidden windows be voice-commanded?
   - **Recommendation:** Yes, voice should work on all windows regardless of visibility

5. **Basic Auth Credentials:** Store per-domain or prompt every time?
   - **Recommendation:** Prompt every time initially (security), store as Phase 2 feature

---

## Risks & Mitigation

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| WebView scroll save lag | High | Medium | Debounce save to 500ms, async operation |
| State bloat (too much data) | Medium | Low | Limit history, only save on deselection |
| Animation jank on low-end devices | High | Medium | Use hardware acceleration, test on min-spec device |
| Basic auth dialog UX issues | Low | Low | Use Material Design AlertDialog pattern |
| Backward compatibility with existing windows | High | Low | Default all new fields, migration not needed |

---

## Future Enhancements (Out of Scope)

1. Window drag & drop repositioning
2. Custom window sizes (user-configurable)
3. Window snapshots (visual previews)
4. Window grouping (tabs/stacks)
5. Keyboard shortcuts for all actions
6. Window state serialization (persist across app restarts)
7. Advanced WebView features (downloads, file uploads, permissions)
8. Window history (undo/redo size/position changes)

---

## Approval & Sign-off

**Prepared By:** AI Assistant
**Date:** 2025-12-10
**Status:** Awaiting User Approval

**Next Steps:**
1. Review specification
2. Approve or request changes
3. Proceed to implementation plan (`/i.plan`)
4. Begin Phase 1 implementation

---

**End of Specification**
