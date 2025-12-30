# Legacy Browser Migration to WebAvanue - Implementation Plan

**Feature ID:** 001-legacy-browser-migration
**Created:** 2025-11-21 16:00
**Profile:** multiplatform-app (Kotlin Multiplatform + Compose)
**Estimated Effort:** 80 days (16 weeks, 2 developers)
**Complexity Tier:** 3 (Full IDEACODE workflow required)

---

## Executive Summary

Migrate 29 missing features from legacy avenue-redux-browser (Android/XML) to modern WebAvanue (KMP/Compose) while simultaneously converting all XML-based UI components to Compose Multiplatform. This includes critical voice-first controls (scrolling, zoom, desktop mode), favorites system, touch/cursor controls, and authentication features. The migration will achieve 100% feature parity (52/52 features) while maintaining WebAvanue's superior cross-platform architecture, 90%+ test coverage, and modern tech stack.

---

## Architecture Overview

### Current State: WebAvanue (Modern)
```
WebAvanue Architecture
├── universal/                     # KMP shared code (95%)
│   ├── data/                      # SQLDelight database
│   ├── domain/                    # Business logic
│   ├── presentation/              # Compose UI + ViewModels
│   │   ├── ui/browser/            # BrowserScreen (Compose)
│   │   ├── ui/bookmarks/          # BookmarksScreen (Compose)
│   │   ├── ui/history/            # HistoryScreen (Compose)
│   │   └── ui/settings/           # SettingsScreen (Compose)
│   └── utils/                     # Utilities
├── android/                       # Android-specific (5%)
│   └── WebViewContainer.android.kt
├── ios/                           # iOS-specific (5%)
└── desktop/                       # Desktop-specific (5%)

Tech Stack:
- UI: Compose Multiplatform
- Database: SQLDelight 2.0.1
- Navigation: Voyager
- DI: Koin
- Async: Coroutines + Flow
```

### Legacy State: avenue-redux-browser (Old)
```
avenue-redux-browser Architecture
├── app/                           # Android-only
│   ├── WebViewFragment.kt         # XML-based UI
│   ├── WebViewModel.kt            # ViewModel
│   └── res/layout/                # XML layouts
├── voiceos-common/
│   ├── WebViewContainer.kt        # WebView wrapper
│   ├── WebViewCommands.kt         # Command definitions
│   └── BottomCommandBar (View)    # XML-based command bar
├── bottom-command-bar/
├── augmentalis_theme/             # Custom XML theme
├── voiceos-storage/
│   └── Realm database models
└── other modules/

Tech Stack:
- UI: XML Views + ViewBinding
- Database: Realm
- Navigation: Navigation Component
- DI: Hilt
- Async: RxJava + Coroutines
```

### Target State: WebAvanue with Full Parity

```
WebAvanue (Enhanced)
├── universal/
│   ├── data/
│   │   ├── sqldelight/
│   │   │   ├── Tab.sq (enhanced)  # + scrollX, scrollY, isDesktopMode, zoomLevel
│   │   │   └── Favorite.sq (NEW)  # Favorites system
│   │   ├── repository/
│   │   │   ├── TabRepository.kt   # Enhanced with scroll/zoom/desktop
│   │   │   └── FavoriteRepository.kt (NEW)
│   │   └── models/
│   │       └── Tab.kt (enhanced)
│   ├── domain/
│   │   ├── usecases/
│   │   │   ├── ScrollUseCase.kt (NEW)
│   │   │   ├── ZoomUseCase.kt (NEW)
│   │   │   ├── DesktopModeUseCase.kt (NEW)
│   │   │   └── FavoriteUseCase.kt (NEW)
│   │   └── models/
│   │       ├── ScrollCommand.kt (NEW)
│   │       ├── ZoomLevel.kt (NEW)
│   │       └── CommandBarLevel.kt (enhanced)
│   ├── presentation/
│   │   ├── viewmodels/
│   │   │   ├── TabViewModel.kt (enhanced)
│   │   │   └── FavoriteViewModel.kt (NEW)
│   │   └── ui/
│   │       ├── browser/
│   │       │   ├── BrowserScreen.kt (enhanced)
│   │       │   ├── AddressBar.kt (enhanced with star icon)
│   │       │   ├── BottomCommandBar.kt (enhanced with new modes)
│   │       │   ├── FavoritesBar.kt (NEW - Compose)
│   │       │   ├── DesktopModeIndicator.kt (NEW - Compose)
│   │       │   └── AuthDialog.kt (NEW - Compose)
│   │       ├── favorites/ (NEW)
│   │       │   └── FavoritesScreen.kt
│   │       └── theme/ (enhanced)
│   │           └── WebAvanueTheme.kt
│   └── utils/
│       └── WebViewCommands.kt (NEW - ported from legacy)
├── android/
│   └── WebViewContainer.android.kt (ENHANCED)
│       # Add: scroll, zoom, desktop mode, touch, cursor, auth
├── ios/
│   └── WebViewContainer.ios.kt (ENHANCED)
└── desktop/
    └── WebViewContainer.desktop.kt (ENHANCED)

All UI: 100% Compose Multiplatform (NO XML)
Database: 100% SQLDelight (NO Realm)
```

### Components to Port/Migrate

#### 1. WebView Controls (Legacy → WebAvanue)
```kotlin
// Legacy: WebViewContainer.kt (Android-specific)
fun scrollUp() { webView?.evaluateJavascript("window.scrollBy(0, -100);", null) }
fun zoomIn() { webView?.zoomIn() }
fun setDesktopMode(enabled: Boolean) { webView?.settings?.userAgentString = ... }

// WebAvanue: WebViewContainer.android.kt (expect/actual pattern)
expect class WebViewContainer {
    actual fun scrollUp(pixels: Int)
    actual fun zoomIn()
    actual fun setDesktopMode(enabled: Boolean)
}

// Implementations for android, ios, desktop
```

#### 2. UI Components (XML → Compose)
```kotlin
// Legacy: res/layout/favorites_bar.xml
<HorizontalScrollView>
    <LinearLayout id="@+id/linear_favorites" />
</HorizontalScrollView>

// WebAvanue: FavoritesBar.kt (Compose)
@Composable
fun FavoritesBar(
    favorites: List<Favorite>,
    onFavoriteClick: (Favorite) -> Unit
) {
    LazyRow { ... }
}
```

#### 3. Command System (Legacy → Enhanced)
```kotlin
// Legacy: 8 modes, 52 commands (hardcoded in Views)
enum class CommandMode { INITIAL, NAVIGATION, SCROLL, CURSOR, ZOOM, ... }

// WebAvanue: Enhanced CommandBarLevel (Compose state)
enum class CommandBarLevel {
    MAIN, NAVIGATION, ACTIONS, MENU,
    SCROLL,        // NEW
    ZOOM,          // NEW
    ZOOM_LEVELS,   // NEW
    CURSOR,        // NEW
    TOUCH,         // NEW
    WEB_CONTROLS   // NEW
}
```

### Data Flow

```
User Input (Voice/Touch)
    ↓
CommandBarLevel State Change
    ↓
TabViewModel.executeCommand()
    ↓
Use Case (ScrollUseCase, ZoomUseCase, etc.)
    ↓
WebViewContainer.platformSpecificAction()
    ↓
WebView API (JavaScript, Settings, etc.)
    ↓
State Update (scroll position, zoom level saved to DB)
    ↓
UI Update (Compose recomposition)
```

### Integration Points

1. **BrowserScreen.kt**
   - Integrate FavoritesBar composable
   - Add DesktopModeIndicator
   - Connect new CommandBarLevel modes

2. **TabViewModel.kt**
   - Add scroll commands handler
   - Add zoom commands handler
   - Add desktop mode toggle
   - Add favorites management

3. **WebViewContainer (expect/actual)**
   - Android: JavaScript injection, WebView settings
   - iOS: WKWebView equivalents
   - Desktop: JavaFX WebView equivalents

4. **Database (SQLDelight)**
   - Migration: Add columns (scrollX, scrollY, isDesktopMode, zoomLevel)
   - New table: Favorite

5. **BottomCommandBar.kt**
   - Add 3 new command levels (SCROLL, ZOOM, CURSOR, TOUCH)
   - Port 37 new command definitions

---

## Implementation Phases

### Phase 1: Foundation & Database (Week 1-2, 10 days)

**Duration:** 10 days (80 hours)
**Complexity:** Tier 2 (Subagents for schema design)

#### Tasks

**Database Schema Updates (3 days)**
1. Update Tab.sq schema:
   ```sql
   ALTER TABLE Tab ADD COLUMN scrollXPosition INTEGER DEFAULT 0;
   ALTER TABLE Tab ADD COLUMN scrollYPosition INTEGER DEFAULT 0;
   ALTER TABLE Tab ADD COLUMN isDesktopMode INTEGER DEFAULT 0;
   ALTER TABLE Tab ADD COLUMN zoomLevel INTEGER DEFAULT 100;
   ```

2. Create Favorite.sq:
   ```sql
   CREATE TABLE Favorite (
       id TEXT PRIMARY KEY NOT NULL,
       title TEXT NOT NULL,
       url TEXT NOT NULL,
       favicon TEXT,
       position INTEGER NOT NULL,
       createdAt INTEGER NOT NULL,
       updatedAt INTEGER NOT NULL
   );
   ```

3. Create migration scripts (SQLDelight migrations)
4. Update Tab data class with new fields
5. Create Favorite data class

**Repository Layer (3 days)**
6. Enhance TabRepository:
   - `updateScrollPosition(tabId, x, y)`
   - `updateDesktopMode(tabId, enabled)`
   - `updateZoomLevel(tabId, level)`

7. Create FavoriteRepository:
   - `getAllFavorites(): Flow<List<Favorite>>`
   - `addFavorite(favorite: Favorite)`
   - `removeFavorite(id: String)`
   - `updateFavoritePosition(id, newPosition)`

**Domain Models (2 days)**
8. Create ScrollCommand.kt (sealed class)
9. Create ZoomLevel.kt (enum: L1-L5 + percentage)
10. Create UserAgent.kt (Desktop vs Mobile)
11. Enhance CommandBarLevel enum with new modes
12. Create TouchCommand.kt, CursorCommand.kt

**Use Cases (2 days)**
13. Create ScrollUseCase.kt
14. Create ZoomUseCase.kt
15. Create DesktopModeUseCase.kt
16. Create FavoriteUseCase.kt

#### Agents Required
- `@database-expert` - SQLDelight schema design
- `@kotlin-expert` - Domain modeling
- `@test-specialist` - Repository tests

#### Quality Gates
- [ ] All migrations run without errors
- [ ] Database schema validated
- [ ] Repository methods have unit tests (≥90% coverage)
- [ ] Domain models have documentation
- [ ] Use cases have unit tests

#### Risks
- **Risk:** SQLDelight migration fails on existing data
  - **Mitigation:** Test migrations on copy of production DB
  - **Contingency:** Provide rollback migration script

- **Risk:** Performance regression on large tab counts
  - **Mitigation:** Profile database queries, add indexes
  - **Contingency:** Implement pagination for favorites

---

### Phase 2: WebView Platform Implementation (Week 3-4, 10 days)

**Duration:** 10 days (80 hours)
**Complexity:** Tier 2 (Platform-specific code)

#### Tasks

**Android WebView Enhancement (4 days)**
1. Update `WebViewContainer.android.kt`:
   ```kotlin
   actual fun scrollUp(pixels: Int) {
       webView?.evaluateJavascript("window.scrollBy(0, -$pixels);", null)
   }

   actual fun scrollDown(pixels: Int) { ... }
   actual fun scrollLeft(pixels: Int) { ... }
   actual fun scrollRight(pixels: Int) { ... }
   actual fun scrollToTop() { ... }
   actual fun scrollToBottom() { ... }

   actual fun zoomIn() { webView?.zoomIn() }
   actual fun zoomOut() { webView?.zoomOut() }
   actual fun setZoomLevel(level: Int) {
       val textZoom = when(level) {
           1 -> 75; 2 -> 100; 3 -> 125; 4 -> 150; 5 -> 200
           else -> 100
       }
       webView?.settings?.textZoom = textZoom
   }

   actual fun setDesktopMode(enabled: Boolean) {
       val ua = if (enabled) DESKTOP_USER_AGENT else defaultUserAgent
       webView?.settings?.userAgentString = ua
       webView?.reload()
   }

   actual fun clearCookies() {
       CookieManager.getInstance().removeAllCookies(null)
   }

   actual fun enableTouchMode() { ... }
   actual fun simulateClick(x: Float, y: Float) { ... }
   actual fun simulateDoubleClick(x: Float, y: Float) { ... }
   ```

**iOS WebView Enhancement (3 days)**
2. Implement `WebViewContainer.ios.kt`:
   ```kotlin
   actual fun scrollUp(pixels: Int) {
       wkWebView?.evaluateJavaScript("window.scrollBy(0, -$pixels)", null)
   }
   // ... similar implementations for iOS WKWebView
   ```

**Desktop WebView Enhancement (2 days)**
3. Implement `WebViewContainer.desktop.kt`:
   ```kotlin
   actual fun scrollUp(pixels: Int) {
       // JavaFX WebView implementation
   }
   ```

**Testing (1 day)**
4. Platform-specific tests for each implementation

#### Agents Required
- `@android-expert` - Android WebView specifics
- `@ios-expert` - WKWebView implementation
- `@desktop-expert` - JavaFX WebView
- `@test-specialist` - Platform testing

#### Quality Gates
- [ ] All scroll commands work on Android
- [ ] All zoom commands work on Android
- [ ] Desktop mode toggles correctly
- [ ] iOS implementation compiles and runs
- [ ] Desktop implementation compiles and runs
- [ ] Platform-specific tests pass (≥85% coverage)

#### Risks
- **Risk:** iOS WKWebView has different JavaScript API
  - **Mitigation:** Research WKWebView docs early
  - **Contingency:** Implement Android-first, iOS later

- **Risk:** Desktop JavaFX WebView limitations
  - **Mitigation:** Test on actual JavaFX WebView early
  - **Contingency:** Feature flag for platform-specific features

---

### Phase 3: UI Migration (XML → Compose) (Week 5-8, 20 days)

**Duration:** 20 days (160 hours)
**Complexity:** Tier 3 (Full UI redesign)

#### Tasks

**Favorites System (5 days)**
1. Create `FavoritesBar.kt` (Compose):
   ```kotlin
   @Composable
   fun FavoritesBar(
       favorites: List<Favorite>,
       onFavoriteClick: (Favorite) -> Unit,
       onLongPress: (Favorite) -> Unit,
       modifier: Modifier = Modifier
   ) {
       LazyRow(
           modifier = modifier.fillMaxWidth(),
           horizontalArrangement = Arrangement.spacedBy(8.dp)
       ) {
           items(favorites, key = { it.id }) { favorite ->
               FavoriteItem(
                   title = favorite.title,
                   url = favorite.url,
                   favicon = favorite.favicon,
                   onClick = { onFavoriteClick(favorite) },
                   onLongPress = { onLongPress(favorite) }
               )
           }
       }
   }
   ```

2. Create `FavoriteItem.kt` composable
3. Create `AddToFavoritesDialog.kt`
4. Create `FavoritesScreen.kt` (full-screen favorites manager)
5. Add star icon to AddressBar.kt (Compose)

**Desktop Mode Indicator (2 days)**
6. Create `DesktopModeIndicator.kt`:
   ```kotlin
   @Composable
   fun DesktopModeIndicator(
       isDesktopMode: Boolean,
       onClick: () -> Unit,
       modifier: Modifier = Modifier
   ) {
       if (isDesktopMode) {
           Icon(
               imageVector = Icons.Default.Computer,
               contentDescription = "Desktop Mode Active",
               modifier = modifier.clickable(onClick = onClick),
               tint = MaterialTheme.colorScheme.primary
           )
       }
   }
   ```

**Command Bar Enhancement (5 days)**
7. Enhance `BottomCommandBar.kt`:
   - Add SCROLL level with 7 commands
   - Add ZOOM level with 2 commands
   - Add ZOOM_LEVELS level with 5 commands
   - Add CURSOR level with 4 commands
   - Add TOUCH level with 6 commands

8. Create `CommandBarButton.kt` reusable component
9. Create `CommandBarMode.kt` state management
10. Port all 37 new command definitions from legacy

**Authentication Dialog (3 days)**
11. Create `BasicAuthDialog.kt`:
    ```kotlin
    @Composable
    fun BasicAuthDialog(
        host: String,
        onAuthenticate: (username: String, password: String) -> Unit,
        onDismiss: () -> Unit
    ) {
        // Compose dialog with username/password fields
    }
    ```

12. Create `AuthCredentialCache.kt` (encrypted storage)

**BrowserScreen Integration (3 days)**
13. Integrate FavoritesBar into `BrowserScreen.kt`:
    ```kotlin
    @Composable
    fun BrowserScreen(viewModel: TabViewModel) {
        Column {
            TabBar(...)
            FavoritesBar(         // NEW
                favorites = viewModel.favorites.collectAsState().value,
                onFavoriteClick = { viewModel.loadUrl(it.url) }
            )
            AddressBar(...)       // Enhanced with star icon
            WebViewContainer(...) // Enhanced with all new features
            BottomCommandBar(...) // Enhanced with new command levels
        }
    }
    ```

14. Wire up all ViewModels
15. Add state management for new features

**Theme Updates (2 days)**
16. Update `WebAvanueTheme.kt` for new components
17. Add Dark 3D theme support (if not already present)
18. Ensure Material 3 theming consistency

#### Agents Required
- `@compose-expert` - Compose UI implementation
- `@ux-expert` - UI/UX design consistency
- `@kotlin-expert` - State management
- `@test-specialist` - UI tests

#### Quality Gates
- [ ] All new UI is 100% Compose (NO XML)
- [ ] FavoritesBar displays and scrolls smoothly
- [ ] Command bar all modes work correctly
- [ ] Desktop mode indicator shows/hides correctly
- [ ] Auth dialog captures credentials
- [ ] UI tests for all new components (≥80% coverage)
- [ ] Accessibility labels on all interactive elements
- [ ] Dark theme support verified

#### Risks
- **Risk:** Compose performance issues with large favorites list
  - **Mitigation:** Use LazyRow, profile with 100+ favorites
  - **Contingency:** Implement pagination or limit favorites count

- **Risk:** Command bar state management complexity
  - **Mitigation:** Use sealed class for command state
  - **Contingency:** Simplify to fewer command levels initially

---

### Phase 4: ViewModel & Business Logic (Week 9-10, 10 days)

**Duration:** 10 days (80 hours)
**Complexity:** Tier 2

#### Tasks

**TabViewModel Enhancement (4 days)**
1. Add scroll command handlers:
   ```kotlin
   fun scrollUp(pixels: Int = 100) {
       currentTab.value?.let { tab ->
           webViewContainer.scrollUp(pixels)
           viewModelScope.launch {
               tabRepository.updateScrollPosition(
                   tab.id,
                   tab.scrollXPosition,
                   tab.scrollYPosition - pixels
               )
           }
       }
   }
   ```

2. Add zoom command handlers
3. Add desktop mode toggle
4. Add freeze page functionality
5. Add touch/cursor mode handlers
6. Wire up state persistence (scroll position, zoom, desktop mode)

**FavoriteViewModel Creation (3 days)**
7. Create `FavoriteViewModel.kt`:
   ```kotlin
   class FavoriteViewModel(
       private val favoriteRepository: FavoriteRepository
   ) : ViewModel() {
       val favorites: StateFlow<List<Favorite>>
       fun addFavorite(title: String, url: String, favicon: String?)
       fun removeFavorite(id: String)
       fun reorderFavorite(fromIndex: Int, toIndex: Int)
   }
   ```

8. Implement CRUD operations
9. Add favorites reordering logic

**Command Router (2 days)**
10. Create `WebViewCommandRouter.kt`:
    - Map voice commands → ViewModel actions
    - Handle command bar level changes
    - State machine for command modes

11. Integrate with existing voice command system

**Testing (1 day)**
12. ViewModel unit tests (all new methods)
13. Integration tests (ViewModel ↔ Repository)

#### Agents Required
- `@kotlin-expert` - ViewModel architecture
- `@coroutines-expert` - Flow and state management
- `@test-specialist` - ViewModel testing

#### Quality Gates
- [ ] All ViewModel methods have unit tests (≥90%)
- [ ] State flows emit correctly
- [ ] Commands execute without crashes
- [ ] State persists to database correctly
- [ ] Memory leaks checked (LeakCanary)

#### Risks
- **Risk:** State synchronization issues between ViewModel and DB
  - **Mitigation:** Use single source of truth pattern
  - **Contingency:** Add manual sync method

---

### Phase 5: Testing & Quality (Week 11-12, 10 days)

**Duration:** 10 days (80 hours)
**Complexity:** Tier 2

#### Tasks

**Unit Tests (4 days)**
1. Repository tests (Tab, Favorite)
2. Use case tests (Scroll, Zoom, DesktopMode, Favorite)
3. ViewModel tests (Tab, Favorite)
4. Domain model tests

**UI Tests (3 days)**
5. FavoritesBar UI test
6. DesktopModeIndicator UI test
7. CommandBar modes UI test
8. BrowserScreen integration test

**Platform Tests (2 days)**
9. Android WebView tests
10. iOS WebView tests (if applicable)
11. Desktop WebView tests (if applicable)

**Performance Tests (1 day)**
12. Scroll performance (60fps)
13. Favorites bar scroll performance
14. Database query performance
15. Memory usage profiling

#### Agents Required
- `@test-specialist` - Test architecture
- `@performance-expert` - Profiling and optimization

#### Quality Gates
- [ ] Overall test coverage ≥90%
- [ ] All critical paths covered
- [ ] No memory leaks detected
- [ ] Performance benchmarks pass:
  - Scroll: 60fps
  - Zoom: <100ms
  - Command execution: <50ms
- [ ] Crash-free rate: 99.9%

#### Risks
- **Risk:** Test coverage drops below 90%
  - **Mitigation:** Write tests alongside implementation
  - **Contingency:** Dedicate extra sprint to testing

---

### Phase 6: Documentation & Polish (Week 13-14, 10 days)

**Duration:** 10 days (80 hours)
**Complexity:** Tier 1

#### Tasks

**Code Documentation (3 days)**
1. KDoc for all public APIs
2. README updates for new features
3. Architecture decision records (ADRs)

**User Documentation (3 days)**
4. User manual updates (voice commands)
5. Feature guide (favorites, desktop mode, zoom)
6. Screenshot updates

**Developer Documentation (2 days)**
7. Migration guide (XML → Compose)
8. Architecture diagrams
9. API reference updates

**Polish (2 days)**
10. Code cleanup and refactoring
11. Remove dead code
12. Optimize imports
13. Final accessibility review

#### Agents Required
- `@documentation-specialist` - Technical writing
- `@ux-expert` - User-facing docs

#### Quality Gates
- [ ] All public APIs documented
- [ ] User manual complete
- [ ] Developer guide complete
- [ ] No TODO/FIXME comments in production code
- [ ] Accessibility audit passed

---

### Phase 7: Advanced Features (Week 15-16, 10 days)

**Duration:** 10 days (80 hours)
**Complexity:** Tier 2

#### Tasks

**Frame Navigation (2 days)**
1. Previous/Next frame buttons
2. Frame detection logic
3. UI integration

**Touch Controls (3 days)**
4. Drag mode implementation
5. Pinch open/close gestures
6. Rotate view

**Login Tracking (2 days)**
7. Google login detection
8. Office login detection
9. VidCall login detection

**QR Scanner (2 days)**
10. Integrate ZXing or ML Kit
11. Camera permission handling
12. QR code parsing

**Dropbox Integration (1 day)**
13. Dropbox SDK setup
14. OAuth flow
15. File download integration

#### Agents Required
- `@android-expert` - Platform-specific features
- `@integration-expert` - Third-party SDK integration

#### Quality Gates
- [ ] All P3 features working
- [ ] 100% feature parity achieved
- [ ] Tests for all new features
- [ ] Performance verified

---

## Technical Decisions

### Decision 1: Database Migration Strategy

**Options Considered:**
1. **In-place migration** - Alter existing tables
   - ✅ Preserves existing data
   - ✅ Simpler for users
   - ❌ Requires careful migration testing

2. **New tables + data copy** - Create new schema, copy data
   - ✅ Safer (old data preserved)
   - ❌ More complex
   - ❌ Higher disk usage during migration

**Selected:** Option 1 (In-place migration)

**Rationale:** SQLDelight migrations are well-tested, and we can provide rollback scripts. User data preservation is critical.

---

### Decision 2: UI Framework

**Options Considered:**
1. **XML Views** (keep legacy)
   - ✅ Known quantity
   - ❌ Not cross-platform
   - ❌ Outdated

2. **Jetpack Compose** (Android only)
   - ✅ Modern
   - ❌ Android-only
   - ❌ Doesn't align with KMP strategy

3. **Compose Multiplatform** (current choice)
   - ✅ Cross-platform (Android + iOS + Desktop)
   - ✅ Modern declarative UI
   - ✅ Single codebase
   - ❌ Slightly less mature than Jetpack Compose

**Selected:** Option 3 (Compose Multiplatform)

**Rationale:** Aligns with WebAvanue's KMP architecture, enables iOS/Desktop support, and provides modern UI framework.

---

### Decision 3: Command System Architecture

**Options Considered:**
1. **Hardcoded commands in UI** (legacy approach)
   - ✅ Simple
   - ❌ Not maintainable
   - ❌ Hard to test

2. **Sealed class hierarchy**
   - ✅ Type-safe
   - ✅ Testable
   - ✅ Extensible
   - ❌ More boilerplate

**Selected:** Option 2 (Sealed class hierarchy)

**Rationale:** Better architecture, testability, and maintainability. Worth the extra boilerplate.

---

### Decision 4: Platform-Specific Implementation

**Options Considered:**
1. **Shared expect/actual interfaces**
   - ✅ Type-safe
   - ✅ Compiler-enforced
   - ✅ KMP best practice

2. **Runtime platform checks**
   - ❌ Error-prone
   - ❌ Not KMP-idiomatic

**Selected:** Option 1 (expect/actual)

**Rationale:** Standard KMP pattern, compile-time safety, platform-specific optimizations possible.

---

## Dependencies

### Internal
- `universal/data` - Database access
- `universal/domain` - Business logic
- `universal/presentation` - UI and ViewModels
- `android/` - Platform-specific Android code
- `ios/` - Platform-specific iOS code (future)
- `desktop/` - Platform-specific Desktop code (future)

### External
- SQLDelight 2.0.1 - Database
- Kotlin Coroutines 1.7+ - Async
- Compose Multiplatform 1.5+ - UI
- Voyager 1.0+ - Navigation
- Koin 3.5+ - DI
- ZXing or ML Kit - QR scanning (Phase 7)
- Dropbox SDK - File integration (Phase 7)

---

## Quality Gates (Profile: multiplatform-app)

- **Test Coverage:** ≥ 90% (enforced by CI)
- **Build Time:** ≤ 600 seconds
- **Documentation:** All public APIs
- **Review:** All code changes require PR
- **Performance:** No regressions (60fps scrolling, <100ms zoom)
- **Accessibility:** All interactive elements have labels
- **Cross-platform:** Android + iOS + Desktop compile and run

---

## Success Criteria

From analysis and specifications:

- [ ] **100% feature parity** (52/52 features working)
- [ ] **29 missing features implemented**:
  - [ ] All scrolling controls (7 features)
  - [ ] All zoom controls (7 features)
  - [ ] Desktop mode (4 features)
  - [ ] Favorites system (2 features)
  - [ ] Touch/cursor controls (6 features)
  - [ ] Authentication (5 features)
  - [ ] Advanced features (4 features)
- [ ] **100% UI in Compose** (NO XML remaining)
- [ ] **90%+ test coverage** maintained
- [ ] **Cross-platform builds** working (Android, iOS, Desktop)
- [ ] **Performance metrics** met:
  - Scroll: 60fps
  - Zoom: <100ms
  - Command execution: <50ms
- [ ] **All documentation** complete
- [ ] **Production-ready** v1.0

---

## Risk Summary

### Critical Risks

1. **Platform API differences** (iOS/Desktop WebView)
   - Probability: High
   - Impact: High
   - Mitigation: Research early, feature flags

2. **Performance regression** with complex UI
   - Probability: Medium
   - Impact: High
   - Mitigation: Profile early, optimize iteratively

3. **Database migration failures**
   - Probability: Low
   - Impact: Critical
   - Mitigation: Test on production data copy, provide rollback

### Medium Risks

4. **Test coverage drop**
   - Probability: Medium
   - Impact: Medium
   - Mitigation: Enforce 90% minimum in CI

5. **Compose Multiplatform bugs**
   - Probability: Medium
   - Impact: Medium
   - Mitigation: Use stable versions, file bugs upstream

---

## Timeline

```
Week 1-2:   Phase 1 (Foundation & Database)           ████░░░░░░░░░░░░ 12.5%
Week 3-4:   Phase 2 (WebView Platform)                ████░░░░░░░░░░░░ 25%
Week 5-8:   Phase 3 (UI Migration to Compose)         ████████░░░░░░░░ 50%
Week 9-10:  Phase 4 (ViewModel & Business Logic)      ██░░░░░░░░░░░░░░ 62.5%
Week 11-12: Phase 5 (Testing & Quality)                ██░░░░░░░░░░░░░░ 75%
Week 13-14: Phase 6 (Documentation & Polish)           ██░░░░░░░░░░░░░░ 87.5%
Week 15-16: Phase 7 (Advanced Features)                ██░░░░░░░░░░░░░░ 100%

Total: 16 weeks, 80 days, 640 hours (2 developers)
```

---

## Next Steps

1. **Review this plan** for completeness and feasibility
2. **Generate detailed tasks** using `/ideacode.tasks` or create `tasks.md`
3. **Setup development environment**:
   - Clone WebAvanue
   - Build and run
   - Verify tests pass
4. **Create feature branch**: `git checkout -b feature/legacy-migration`
5. **Start Phase 1**: Database schema updates
6. **Daily standups** to track progress
7. **Weekly demos** to stakeholders

---

**Approval Required:** Yes
**Estimated Start Date:** TBD
**Estimated Completion:** Start + 16 weeks
**Team Size:** 2 developers + 1 QA

---

**Last Updated:** 2025-11-21 16:00
**Status:** ✅ Ready for Review
**Next:** Create `tasks.md` with detailed task breakdown
