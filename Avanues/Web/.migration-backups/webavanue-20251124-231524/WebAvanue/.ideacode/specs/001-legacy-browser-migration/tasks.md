# Legacy Browser Migration - Task Breakdown

**Feature ID:** 001-legacy-browser-migration
**Created:** 2025-11-21
**Total Tasks:** 187 tasks across 7 phases
**Estimated Effort:** 640 hours (80 days, 2 developers)

---

## Task Categories

- 🔵 **DEV** = Development task
- 🟢 **TEST** = Testing task
- 🟡 **DOC** = Documentation task
- 🟣 **REVIEW** = Code review / QA
- 🔴 **CRITICAL** = Blocking task

---

## Phase 1: Foundation & Database (10 days, 26 tasks)

### Database Schema Updates (3 days, 8 tasks)

- [ ] 🔵 **CRITICAL** P1.1.1: Create SQLDelight migration file (v1 → v2)
- [ ] 🔵 P1.1.2: Add `scrollXPosition INTEGER DEFAULT 0` to Tab table
- [ ] 🔵 P1.1.3: Add `scrollYPosition INTEGER DEFAULT 0` to Tab table
- [ ] 🔵 P1.1.4: Add `isDesktopMode INTEGER DEFAULT 0` to Tab table
- [ ] 🔵 P1.1.5: Add `zoomLevel INTEGER DEFAULT 100` to Tab table
- [ ] 🔵 P1.1.6: Create `Favorite.sq` table schema
- [ ] 🟢 P1.1.7: Test migration on sample database
- [ ] 🟢 P1.1.8: Test rollback migration

### Data Models (1 day, 4 tasks)

- [ ] 🔵 P1.2.1: Update `Tab` data class with new fields
- [ ] 🔵 P1.2.2: Create `Favorite` data class
- [ ] 🟢 P1.2.3: Unit tests for Tab model
- [ ] 🟢 P1.2.4: Unit tests for Favorite model

### Repository Layer (3 days, 8 tasks)

- [ ] 🔵 P1.3.1: Add `updateScrollPosition()` to TabRepository
- [ ] 🔵 P1.3.2: Add `updateDesktopMode()` to TabRepository
- [ ] 🔵 P1.3.3: Add `updateZoomLevel()` to TabRepository
- [ ] 🔵 P1.3.4: Create `FavoriteRepository` interface
- [ ] 🔵 P1.3.5: Implement `FavoriteRepositoryImpl`
- [ ] 🟢 P1.3.6: Unit tests for TabRepository (new methods)
- [ ] 🟢 P1.3.7: Unit tests for FavoriteRepository (all methods)
- [ ] 🟣 P1.3.8: Code review - Repository layer

### Domain Models (2 days, 6 tasks)

- [ ] 🔵 P1.4.1: Create `ScrollCommand` sealed class
- [ ] 🔵 P1.4.2: Create `ZoomLevel` enum (L1-L5)
- [ ] 🔵 P1.4.3: Create `UserAgent` data class
- [ ] 🔵 P1.4.4: Enhance `CommandBarLevel` with new modes
- [ ] 🔵 P1.4.5: Create `TouchCommand` sealed class
- [ ] 🔵 P1.4.6: Create `CursorCommand` sealed class

---

## Phase 2: WebView Platform Implementation (10 days, 32 tasks)

### Android WebView Enhancement (4 days, 14 tasks)

- [ ] 🔵 **CRITICAL** P2.1.1: Add `scrollUp()` to WebViewContainer.android.kt
- [ ] 🔵 **CRITICAL** P2.1.2: Add `scrollDown()` to WebViewContainer.android.kt
- [ ] 🔵 P2.1.3: Add `scrollLeft()` to WebViewContainer.android.kt
- [ ] 🔵 P2.1.4: Add `scrollRight()` to WebViewContainer.android.kt
- [ ] 🔵 P2.1.5: Add `scrollToTop()` to WebViewContainer.android.kt
- [ ] 🔵 P2.1.6: Add `scrollToBottom()` to WebViewContainer.android.kt
- [ ] 🔵 **CRITICAL** P2.1.7: Add `zoomIn()` to WebViewContainer.android.kt
- [ ] 🔵 **CRITICAL** P2.1.8: Add `zoomOut()` to WebViewContainer.android.kt
- [ ] 🔵 P2.1.9: Add `setZoomLevel(level: Int)` to WebViewContainer.android.kt
- [ ] 🔵 **CRITICAL** P2.1.10: Add `setDesktopMode(enabled: Boolean)` to WebViewContainer.android.kt
- [ ] 🔵 P2.1.11: Add `clearCookies()` to WebViewContainer.android.kt
- [ ] 🔵 P2.1.12: Add `enableTouchMode()` for touch controls
- [ ] 🔵 P2.1.13: Add `simulateClick(x, y)` for cursor mode
- [ ] 🔵 P2.1.14: Add `simulateDoubleClick(x, y)` for cursor mode

### iOS WebView Enhancement (3 days, 10 tasks)

- [ ] 🔵 P2.2.1: Add `scrollUp()` to WebViewContainer.ios.kt
- [ ] 🔵 P2.2.2: Add `scrollDown()` to WebViewContainer.ios.kt
- [ ] 🔵 P2.2.3: Add `scrollToTop()` to WebViewContainer.ios.kt
- [ ] 🔵 P2.2.4: Add `scrollToBottom()` to WebViewContainer.ios.kt
- [ ] 🔵 P2.2.5: Add `zoomIn()` to WebViewContainer.ios.kt
- [ ] 🔵 P2.2.6: Add `zoomOut()` to WebViewContainer.ios.kt
- [ ] 🔵 P2.2.7: Add `setZoomLevel(level: Int)` to WebViewContainer.ios.kt
- [ ] 🔵 P2.2.8: Add `setDesktopMode(enabled: Boolean)` to WebViewContainer.ios.kt
- [ ] 🔵 P2.2.9: Add `clearCookies()` to WebViewContainer.ios.kt
- [ ] 🟢 P2.2.10: iOS platform tests

### Desktop WebView Enhancement (2 days, 6 tasks)

- [ ] 🔵 P2.3.1: Add scroll methods to WebViewContainer.desktop.kt
- [ ] 🔵 P2.3.2: Add zoom methods to WebViewContainer.desktop.kt
- [ ] 🔵 P2.3.3: Add `setDesktopMode()` to WebViewContainer.desktop.kt
- [ ] 🔵 P2.3.4: Add `clearCookies()` to WebViewContainer.desktop.kt
- [ ] 🟢 P2.3.5: Desktop platform tests
- [ ] 🟣 P2.3.6: Code review - All platform implementations

### Testing (1 day, 2 tasks)

- [ ] 🟢 P2.4.1: Integration tests (ViewModel → WebViewContainer)
- [ ] 🟢 P2.4.2: Cross-platform tests (ensure all platforms compile)

---

## Phase 3: UI Migration (XML → Compose) (20 days, 51 tasks)

### Favorites System (5 days, 15 tasks)

- [ ] 🔵 **CRITICAL** P3.1.1: Create `FavoritesBar.kt` composable
- [ ] 🔵 P3.1.2: Create `FavoriteItem.kt` composable
- [ ] 🔵 P3.1.3: Add favicon loading to FavoriteItem
- [ ] 🔵 P3.1.4: Add long-press handler for favorite editing
- [ ] 🔵 P3.1.5: Create `AddToFavoritesDialog.kt`
- [ ] 🔵 P3.1.6: Create `FavoritesScreen.kt` (full-screen manager)
- [ ] 🔵 P3.1.7: Add reordering logic (drag & drop)
- [ ] 🔵 P3.1.8: Add star icon to `AddressBar.kt`
- [ ] 🔵 P3.1.9: Wire star icon to AddToFavoritesDialog
- [ ] 🔵 P3.1.10: Add favorites state to BrowserScreen
- [ ] 🟢 P3.1.11: UI test for FavoritesBar
- [ ] 🟢 P3.1.12: UI test for FavoriteItem
- [ ] 🟢 P3.1.13: UI test for AddToFavoritesDialog
- [ ] 🟢 P3.1.14: UI test for star icon in AddressBar
- [ ] 🟣 P3.1.15: Code review - Favorites system

### Desktop Mode Indicator (2 days, 5 tasks)

- [ ] 🔵 P3.2.1: Create `DesktopModeIndicator.kt` composable
- [ ] 🔵 P3.2.2: Add indicator to AddressBar
- [ ] 🔵 P3.2.3: Wire indicator to tab state
- [ ] 🟢 P3.2.4: UI test for DesktopModeIndicator
- [ ] 🟣 P3.2.5: Code review - Desktop mode indicator

### Command Bar Enhancement (5 days, 18 tasks)

- [ ] 🔵 **CRITICAL** P3.3.1: Add `SCROLL` level to `CommandBarLevel` enum
- [ ] 🔵 **CRITICAL** P3.3.2: Add `ZOOM` level to `CommandBarLevel` enum
- [ ] 🔵 P3.3.3: Add `ZOOM_LEVELS` level to `CommandBarLevel` enum
- [ ] 🔵 P3.3.4: Add `CURSOR` level to `CommandBarLevel` enum
- [ ] 🔵 P3.3.5: Add `TOUCH` level to `CommandBarLevel` enum
- [ ] 🔵 P3.3.6: Create `CommandBarButton.kt` reusable component
- [ ] 🔵 P3.3.7: Define SCROLL commands (up, down, left, right, top, bottom, freeze)
- [ ] 🔵 P3.3.8: Define ZOOM commands (in, out)
- [ ] 🔵 P3.3.9: Define ZOOM_LEVELS commands (L1-L5)
- [ ] 🔵 P3.3.10: Define CURSOR commands (click, double click)
- [ ] 🔵 P3.3.11: Define TOUCH commands (drag, pinch open, pinch close, rotate)
- [ ] 🔵 P3.3.12: Update `BottomCommandBar.kt` with new levels
- [ ] 🔵 P3.3.13: Add level transition animations
- [ ] 🔵 P3.3.14: Add voice command mapping for new levels
- [ ] 🟢 P3.3.15: UI test for SCROLL level
- [ ] 🟢 P3.3.16: UI test for ZOOM level
- [ ] 🟢 P3.3.17: UI test for command button
- [ ] 🟣 P3.3.18: Code review - Command bar enhancement

### Authentication Dialog (3 days, 7 tasks)

- [ ] 🔵 P3.4.1: Create `BasicAuthDialog.kt` composable
- [ ] 🔵 P3.4.2: Add username/password TextField
- [ ] 🔵 P3.4.3: Add "Remember credentials" checkbox
- [ ] 🔵 P3.4.4: Create `AuthCredentialCache.kt` (encrypted storage)
- [ ] 🟢 P3.4.5: UI test for BasicAuthDialog
- [ ] 🟢 P3.4.6: Unit test for AuthCredentialCache
- [ ] 🟣 P3.4.7: Code review - Authentication

### BrowserScreen Integration (3 days, 4 tasks)

- [ ] 🔵 **CRITICAL** P3.5.1: Integrate FavoritesBar into BrowserScreen.kt
- [ ] 🔵 P3.5.2: Update BrowserScreen layout (Column structure)
- [ ] 🔵 P3.5.3: Wire all new components to ViewModels
- [ ] 🟣 P3.5.4: Code review - BrowserScreen integration

### Theme Updates (2 days, 2 tasks)

- [ ] 🔵 P3.6.1: Update `WebAvanueTheme.kt` for new components
- [ ] 🔵 P3.6.2: Verify Dark theme support for all new UI

---

## Phase 4: ViewModel & Business Logic (10 days, 24 tasks)

### TabViewModel Enhancement (4 days, 12 tasks)

- [ ] 🔵 **CRITICAL** P4.1.1: Add `scrollUp(pixels: Int)` to TabViewModel
- [ ] 🔵 **CRITICAL** P4.1.2: Add `scrollDown(pixels: Int)` to TabViewModel
- [ ] 🔵 P4.1.3: Add `scrollLeft(pixels: Int)` to TabViewModel
- [ ] 🔵 P4.1.4: Add `scrollRight(pixels: Int)` to TabViewModel
- [ ] 🔵 P4.1.5: Add `scrollToTop()` to TabViewModel
- [ ] 🔵 P4.1.6: Add `scrollToBottom()` to TabViewModel
- [ ] 🔵 **CRITICAL** P4.1.7: Add `zoomIn()` to TabViewModel
- [ ] 🔵 **CRITICAL** P4.1.8: Add `zoomOut()` to TabViewModel
- [ ] 🔵 P4.1.9: Add `setZoomLevel(level: Int)` to TabViewModel
- [ ] 🔵 **CRITICAL** P4.1.10: Add `toggleDesktopMode()` to TabViewModel
- [ ] 🔵 P4.1.11: Add `freezePage()` to TabViewModel
- [ ] 🔵 P4.1.12: Add state persistence for scroll/zoom/desktop mode

### FavoriteViewModel Creation (3 days, 7 tasks)

- [ ] 🔵 P4.2.1: Create `FavoriteViewModel.kt`
- [ ] 🔵 P4.2.2: Add `favorites: StateFlow<List<Favorite>>`
- [ ] 🔵 P4.2.3: Add `addFavorite(title, url, favicon)`
- [ ] 🔵 P4.2.4: Add `removeFavorite(id)`
- [ ] 🔵 P4.2.5: Add `reorderFavorite(fromIndex, toIndex)`
- [ ] 🟢 P4.2.6: Unit tests for FavoriteViewModel
- [ ] 🟣 P4.2.7: Code review - FavoriteViewModel

### Command Router (2 days, 3 tasks)

- [ ] 🔵 P4.3.1: Create `WebViewCommandRouter.kt`
- [ ] 🔵 P4.3.2: Map voice commands to ViewModel actions
- [ ] 🔵 P4.3.3: Integrate with existing voice command system

### Testing (1 day, 2 tasks)

- [ ] 🟢 P4.4.1: ViewModel unit tests (all new methods)
- [ ] 🟢 P4.4.2: Integration tests (ViewModel ↔ Repository)

---

## Phase 5: Testing & Quality (10 days, 24 tasks)

### Unit Tests (4 days, 10 tasks)

- [ ] 🟢 P5.1.1: TabRepository tests (scroll/zoom/desktop methods)
- [ ] 🟢 P5.1.2: FavoriteRepository tests (CRUD operations)
- [ ] 🟢 P5.1.3: ScrollUseCase tests
- [ ] 🟢 P5.1.4: ZoomUseCase tests
- [ ] 🟢 P5.1.5: DesktopModeUseCase tests
- [ ] 🟢 P5.1.6: FavoriteUseCase tests
- [ ] 🟢 P5.1.7: TabViewModel tests (all new methods)
- [ ] 🟢 P5.1.8: FavoriteViewModel tests
- [ ] 🟢 P5.1.9: Domain model tests
- [ ] 🟣 P5.1.10: Review test coverage (target: ≥90%)

### UI Tests (3 days, 8 tasks)

- [ ] 🟢 P5.2.1: FavoritesBar UI test
- [ ] 🟢 P5.2.2: FavoriteItem UI test
- [ ] 🟢 P5.2.3: DesktopModeIndicator UI test
- [ ] 🟢 P5.2.4: CommandBar modes UI test
- [ ] 🟢 P5.2.5: BasicAuthDialog UI test
- [ ] 🟢 P5.2.6: BrowserScreen integration UI test
- [ ] 🟢 P5.2.7: AddressBar star icon UI test
- [ ] 🟣 P5.2.8: Review UI test coverage

### Platform Tests (2 days, 4 tasks)

- [ ] 🟢 P5.3.1: Android WebView tests (all new methods)
- [ ] 🟢 P5.3.2: iOS WebView tests (if implemented)
- [ ] 🟢 P5.3.3: Desktop WebView tests (if implemented)
- [ ] 🟣 P5.3.4: Cross-platform test verification

### Performance Tests (1 day, 2 tasks)

- [ ] 🟢 P5.4.1: Scroll performance test (60fps target)
- [ ] 🟢 P5.4.2: Zoom performance test (<100ms target)

---

## Phase 6: Documentation & Polish (10 days, 18 tasks)

### Code Documentation (3 days, 6 tasks)

- [ ] 🟡 P6.1.1: KDoc for all new public APIs
- [ ] 🟡 P6.1.2: README updates (new features section)
- [ ] 🟡 P6.1.3: Architecture Decision Records (ADRs)
- [ ] 🟡 P6.1.4: Migration guide (XML → Compose)
- [ ] 🟡 P6.1.5: API reference updates
- [ ] 🟣 P6.1.6: Documentation review

### User Documentation (3 days, 6 tasks)

- [ ] 🟡 P6.2.1: User manual - Voice commands section
- [ ] 🟡 P6.2.2: User manual - Favorites feature guide
- [ ] 🟡 P6.2.3: User manual - Desktop mode guide
- [ ] 🟡 P6.2.4: User manual - Zoom controls guide
- [ ] 🟡 P6.2.5: Screenshots for all new features
- [ ] 🟣 P6.2.6: User documentation review

### Developer Documentation (2 days, 4 tasks)

- [ ] 🟡 P6.3.1: Architecture diagrams (updated)
- [ ] 🟡 P6.3.2: Component hierarchy diagrams
- [ ] 🟡 P6.3.3: Data flow diagrams
- [ ] 🟡 P6.3.4: Platform-specific implementation guide

### Polish (2 days, 2 tasks)

- [ ] 🔵 P6.4.1: Code cleanup and refactoring
- [ ] 🔵 P6.4.2: Final accessibility review

---

## Phase 7: Advanced Features (10 days, 12 tasks)

### Frame Navigation (2 days, 2 tasks)

- [ ] 🔵 P7.1.1: Implement Previous/Next frame buttons
- [ ] 🟢 P7.1.2: Frame navigation tests

### Touch Controls (3 days, 4 tasks)

- [ ] 🔵 P7.2.1: Drag mode implementation
- [ ] 🔵 P7.2.2: Pinch open/close gestures
- [ ] 🔵 P7.2.3: Rotate view
- [ ] 🟢 P7.2.4: Touch controls tests

### Login Tracking (2 days, 3 tasks)

- [ ] 🔵 P7.3.1: Google/Office/VidCall login detection
- [ ] 🔵 P7.3.2: Login state persistence
- [ ] 🟢 P7.3.3: Login tracking tests

### QR Scanner & Integrations (3 days, 3 tasks)

- [ ] 🔵 P7.4.1: QR scanner integration (ZXing/ML Kit)
- [ ] 🔵 P7.4.2: Dropbox SDK integration
- [ ] 🟢 P7.4.3: Integration tests

---

## Summary

| Phase | Tasks | Critical | Test | Doc | Review | Days |
|-------|-------|----------|------|-----|--------|------|
| **Phase 1** | 26 | 1 | 6 | 0 | 1 | 10 |
| **Phase 2** | 32 | 5 | 4 | 0 | 1 | 10 |
| **Phase 3** | 51 | 5 | 10 | 0 | 5 | 20 |
| **Phase 4** | 24 | 5 | 4 | 0 | 2 | 10 |
| **Phase 5** | 24 | 0 | 20 | 0 | 4 | 10 |
| **Phase 6** | 18 | 0 | 0 | 14 | 3 | 10 |
| **Phase 7** | 12 | 0 | 4 | 0 | 0 | 10 |
| **TOTAL** | **187** | **16** | **48** | **14** | **16** | **80** |

---

## Critical Path

1. ✅ Database schema (P1.1.1 - P1.1.6) - **MUST complete first**
2. ✅ Android WebView core methods (P2.1.1, P2.1.2, P2.1.7, P2.1.8, P2.1.10) - **Blocking for testing**
3. ✅ FavoritesBar (P3.1.1) - **High user value**
4. ✅ Command Bar levels (P3.3.1, P3.3.2) - **Blocking for voice commands**
5. ✅ BrowserScreen integration (P3.5.1) - **Blocking for UI**
6. ✅ TabViewModel scroll/zoom (P4.1.1, P4.1.2, P4.1.7, P4.1.8, P4.1.10) - **Blocking for functionality**

---

## Dependencies

### Blockers

- P2.1.x requires P1.1.x (database must exist)
- P3.5.x requires P3.1.x, P3.2.x, P3.3.x (all UI components ready)
- P4.1.x requires P2.1.x (platform methods implemented)
- P5.x requires all previous phases (can't test what doesn't exist)

### External Dependencies

- SQLDelight 2.0.1 (already integrated)
- Compose Multiplatform 1.5+ (already integrated)
- ZXing or ML Kit (Phase 7 only)
- Dropbox SDK (Phase 7 only)

---

## Progress Tracking

**Completion Formula:** `(Completed Tasks / Total Tasks) × 100`

**Current Progress:** 0/187 = 0%

**Milestones:**
- [ ] Phase 1 Complete: 26/187 = 14%
- [ ] Phase 2 Complete: 58/187 = 31%
- [ ] Phase 3 Complete: 109/187 = 58%
- [ ] Phase 4 Complete: 133/187 = 71%
- [ ] Phase 5 Complete: 157/187 = 84%
- [ ] Phase 6 Complete: 175/187 = 94%
- [ ] Phase 7 Complete: 187/187 = 100% ✅

---

## How to Use This File

1. **Mark tasks complete** by changing `- [ ]` to `- [x]`
2. **Track blockers** in comments: `<!-- BLOCKED: waiting for P1.1.1 -->`
3. **Update progress** weekly in standup
4. **Link to PRs** for completed tasks: `<!-- PR: #123 -->`
5. **Note issues** inline: `<!-- ISSUE: Performance regression -->`

---

**Last Updated:** 2025-11-21
**Status:** Ready for Implementation
**Next:** Start Phase 1, Task P1.1.1
