# WebAvanue Implementation Plan - Prioritized Excellence (Excluding CI/CD & Tests)

**Document ID:** WebAvanue-Implementation-Plan-51211-V2
**Status:** Active - Ready for Implementation
**Mode:** .tasks (Task tracking enabled)
**Scope:** Security, Performance, Documentation, Advanced Features (CI/CD & Test Coverage excluded per request)

---

## Executive Summary

This plan focuses on **immediate production-readiness improvements** excluding CI/CD pipeline and test coverage expansion. Prioritizes security hardening, performance optimization, documentation, and advanced features.

**Total Tasks:** 35
**Estimated Duration:** 6-8 weeks
**Priority Distribution:** 15 P0 tasks, 12 P1 tasks, 8 P2 tasks

---

## Phase 1: Critical Security & Infrastructure (Week 1) 🔴 P0

**Goal:** Fix critical security vulnerabilities
**Duration:** 5 days
**Tasks:** 7

### Task 1.1: Implement Database Encryption with SQLCipher
**Priority:** P0 - CRITICAL
**Effort:** 2 days
**Risk:** High - Plaintext sensitive data

**Subtasks:**
1. Add SQLCipher dependency (4.5.4)
2. Create EncryptionManager for key generation (Android Keystore)
3. Update DatabaseModule to use SupportFactory
4. Implement database migration script
5. Test encrypted database operations
6. Add key rotation mechanism
7. Document encryption setup

**Files:**
- `build.gradle.kts` (add dependency)
- `EncryptionManager.kt` (NEW)
- `DatabaseModule.kt` (NEW)
- `BrowserRepositoryImpl.kt` (update initialization)

**Acceptance Criteria:**
- [ ] Database encrypted with AES-256
- [ ] Encryption key stored in Android Keystore
- [ ] Existing data migrated successfully
- [ ] All database operations functional
- [ ] Key rotation documented

---

### Task 1.2: Implement Secure Credential Storage
**Priority:** P0 - CRITICAL
**Effort:** 1 day
**Risk:** High - Credential exposure

**Subtasks:**
1. Add EncryptedSharedPreferences dependency
2. Create SecureStorage wrapper class
3. Implement HTTP auth credential storage
4. Migrate existing credentials (if any)
5. Add biometric authentication option
6. Test secure storage operations

**Files:**
- `SecureStorage.kt` (NEW)
- `SecurityViewModel.kt` (integrate secure storage)
- `WebViewConfigurator.kt` (use secure storage for HTTP auth)

**Acceptance Criteria:**
- [ ] Credentials encrypted with EncryptedSharedPreferences
- [ ] Master key in Android Keystore
- [ ] Biometric auth for credential access
- [ ] No plaintext credentials in logs

---

### Task 1.3: Fix ProGuard Configuration
**Priority:** P0 - CRITICAL
**Effort:** 1 day
**Risk:** High - Code exposure to reverse engineering

**Subtasks:**
1. Analyze current ProGuard rules (`-keep class com.** { *; }`)
2. Identify minimal keep requirements (MainActivity, Application, Serializable)
3. Refine rules to enable obfuscation
4. Add source file obfuscation
5. Test release build thoroughly
6. Verify crash reports show obfuscated traces
7. Document keep rule rationale

**Files:**
- `app/proguard-rules.pro` (refactor)
- `PROGUARD.md` (NEW - documentation)

**Before:**
```proguard
-keep class com.augmentalis.Avanues.web.** { *; }  # TOO BROAD
```

**After:**
```proguard
# Keep only entry points
-keep class com.augmentalis.Avanues.web.app.MainActivity { *; }
-keep class com.augmentalis.Avanues.web.app.WebAvanueApp { *; }

# Keep serializable models
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    <fields>;
}

# Obfuscate everything else
-repackageclasses ''
-allowaccessmodification
```

**Acceptance Criteria:**
- [ ] Only entry points unobfuscated
- [ ] APK size reduced 20-30%
- [ ] All features work in release build
- [ ] Crash reports obfuscated

---

### Task 1.4: Integrate Production Logging Framework (Napier)
**Priority:** P0 - CRITICAL
**Effort:** 1 day
**Risk:** Medium - Cannot diagnose issues

**Subtasks:**
1. Add Napier dependency (2.6.1)
2. Create Logger utility with PII filtering
3. Replace all `println()` statements (206 occurrences)
4. Configure log levels (DEBUG/INFO/WARN/ERROR)
5. Add contextual tags (class names)
6. Test logging in debug and release builds

**Files to Update:**
- `build.gradle.kts` (add dependency)
- `Logger.kt` (NEW - wrapper utility)
- `TabViewModel.kt` (50 println → Logger calls)
- `BrowserRepositoryImpl.kt` (30 println → Logger calls)
- `DownloadViewModel.kt` (20 println → Logger calls)
- All other ViewModels and platform files

**Example Replacement:**
```kotlin
// BEFORE
println("TabViewModel: ${error.technicalDetails}")

// AFTER
Logger.error("TabViewModel", error.technicalDetails, error)
```

**Acceptance Criteria:**
- [ ] Zero `println()` statements in codebase
- [ ] All logs use Napier with tags
- [ ] PII filtering for URLs/filenames
- [ ] Log levels properly configured

---

### Task 1.5: Integrate Crash Reporting (Sentry)
**Priority:** P0 - CRITICAL
**Effort:** 1 day
**Risk:** High - Cannot track production crashes

**Subtasks:**
1. Add Sentry KMP SDK (4.0.0)
2. Configure Sentry in Application.onCreate()
3. Implement breadcrumb tracking
4. Add custom error scopes (user context)
5. Test crash reporting
6. Configure error grouping rules
7. Document Sentry setup

**Files:**
- `build.gradle.kts` (add dependency)
- `WebAvanueApp.kt` (initialize Sentry)
- `SentryManager.kt` (NEW - breadcrumb helper)

**Implementation:**
```kotlin
Sentry.init { options ->
    options.dsn = BuildConfig.SENTRY_DSN
    options.environment = if (BuildConfig.DEBUG) "development" else "production"
    options.release = "${BuildConfig.VERSION_NAME}@${BuildConfig.VERSION_CODE}"
    options.tracesSampleRate = 1.0
}

// Breadcrumbs
Breadcrumb().apply {
    category = "navigation"
    message = "Tab switched to ${sanitizeUrl(tab.url)}"
    level = SentryLevel.INFO
}.let { Sentry.addBreadcrumb(it) }
```

**Acceptance Criteria:**
- [ ] Crashes reported to Sentry
- [ ] Breadcrumbs capture user actions
- [ ] User context included (device, OS, app version)
- [ ] No PII in crash reports

---

### Task 1.6: Implement Certificate Pinning
**Priority:** P0
**Effort:** 1 day
**Risk:** Medium - MITM attacks on critical APIs

**Subtasks:**
1. Identify critical API domains (if any)
2. Extract certificate fingerprints
3. Configure network_security_config.xml
4. Implement backup pins
5. Add pinning failure handling
6. Test with valid/invalid certificates
7. Document pin rotation process

**Files:**
- `res/xml/network_security_config.xml` (update)
- `CERTIFICATE_PINNING.md` (NEW - documentation)

**Acceptance Criteria:**
- [ ] Critical domains pinned
- [ ] Backup pins configured
- [ ] Pinning failure handled gracefully
- [ ] Pin rotation documented

---

### Task 1.7: Update Outdated Dependencies
**Priority:** P0
**Effort:** 1 day
**Risk:** Medium - Security vulnerabilities

**Dependencies to Update:**
- androidx.webkit: 1.9.0 → 1.12.0+ (security patches)
- androidx.activity-compose: 1.8.2 → 1.9.3
- androidx.navigation-compose: 2.7.6 → 2.8.5
- androidx.lifecycle: 2.6.2 → 2.8.7
- AGP: 8.2.2 → 8.7.3
- Compose Material: 1.5.4 → 1.7.5

**Subtasks:**
1. Create version catalog (`gradle/libs.versions.toml`)
2. Update dependency versions
3. Fix breaking changes (if any)
4. Update build scripts
5. Run full test suite
6. Document version policy

**Files:**
- `gradle/libs.versions.toml` (NEW)
- All `build.gradle.kts` files (migrate to version catalog)

**Acceptance Criteria:**
- [ ] All dependencies updated
- [ ] Version catalog in use
- [ ] No breaking changes
- [ ] App builds and runs successfully

---

## Phase 2: Performance Optimization (Week 2) 🟡 P1

**Goal:** Optimize performance bottlenecks
**Duration:** 5 days
**Tasks:** 6

### Task 2.1: Optimize Startup Performance
**Priority:** P1
**Effort:** 2 days
**Current:** 50-200ms first frame delay
**Target:** <100ms P95

**Subtasks:**
1. Implement lazy ViewModel initialization
2. Paginate repository data loading (load only active tab + 10 recent)
3. Defer history/download loading to background
4. Add startup performance tracing
5. Measure and verify improvements

**Files:**
- `BrowserRepositoryImpl.kt` (lazy loading)
- `BrowserScreen.kt` (lazy ViewModels)
- `MainActivity.kt` (performance tracing)

**Before:**
```kotlin
init {
    val tabs = queries.selectAllTabs().executeAsList()  // Load 100+ tabs
    val favorites = queries.selectAllFavorites().executeAsList()
    val history = queries.selectAllHistory(100, 0).executeAsList()
}
```

**After:**
```kotlin
init {
    val activeTab = queries.selectActiveTab().executeAsOneOrNull()
    val recentTabs = queries.selectRecentTabs(limit = 10).executeAsList()
    _tabs.value = listOfNotNull(activeTab) + recentTabs
}

suspend fun loadAllTabs() {
    if (!allTabsLoaded) {
        val allTabs = withContext(Dispatchers.IO) {
            queries.selectAllTabs().executeAsList()
        }
        _tabs.value = allTabs
        allTabsLoaded = true
    }
}
```

**Acceptance Criteria:**
- [ ] First frame <100ms (P95)
- [ ] Only active tab loaded initially
- [ ] Background data loads don't block UI
- [ ] Performance metrics tracked

---

### Task 2.2: Add Database Indexes
**Priority:** P1
**Effort:** 1 day
**Current:** Missing indexes on url, visited_at
**Target:** Optimized query performance

**Subtasks:**
1. Add index on `Favorite(url)` for duplicate checks
2. Add index on `HistoryEntry(visited_at DESC)` for date range queries
3. Add index on `HistoryEntry(url)` for URL lookups
4. Add index on `Download(status)` for filtering
5. Analyze query plans (EXPLAIN QUERY PLAN)
6. Measure performance improvements

**Files:**
- `BrowserDatabase.sq` (add indexes)

**Implementation:**
```sql
CREATE INDEX IF NOT EXISTS idx_favorite_url ON Favorite(url);
CREATE INDEX IF NOT EXISTS idx_history_visited_at ON HistoryEntry(visited_at DESC);
CREATE INDEX IF NOT EXISTS idx_history_url ON HistoryEntry(url);
CREATE INDEX IF NOT EXISTS idx_download_status ON Download(status);
```

**Acceptance Criteria:**
- [ ] All critical indexes created
- [ ] Query performance improved (measure)
- [ ] No negative impact on insert performance

---

### Task 2.3: Optimize UI Recomposition
**Priority:** P1
**Effort:** 2 days
**Current:** 4+ recompositions per tab switch
**Target:** 1 recomposition per tab switch

**Subtasks:**
1. Split BrowserScreen into smaller composables
2. Use `key()` strategically for tab-specific state
3. Implement `derivedStateOf` for computed values
4. Add recomposition tracking (Layout Inspector)
5. Optimize `remember` usage
6. Measure improvements

**Files:**
- `BrowserScreen.kt` (split composables)
- `AddressBar.kt` (optimize)
- `BottomCommandBar.kt` (optimize)

**Before:**
```kotlin
val activeTab by tabViewModel.activeTab.collectAsState()
val tabs by tabViewModel.tabs.collectAsState()
val settings by settingsViewModel.settings.collectAsState()
val favorites by favoriteViewModel.favorites.collectAsState()
// 4 StateFlow collectors = 4 recompositions per tab switch
```

**After:**
```kotlin
val browserState by remember {
    derivedStateOf {
        BrowserUiState(
            activeTab = tabViewModel.activeTab.value,
            tabCount = tabViewModel.tabs.value.size,
            isFavorite = favoriteViewModel.favorites.value.any {
                it.url == tabViewModel.activeTab.value?.tab?.url
            }
        )
    }
}
// 1 derived state = 1 recomposition per tab switch
```

**Acceptance Criteria:**
- [ ] Recompositions reduced to 1 per tab switch
- [ ] No performance regression
- [ ] Layout Inspector confirms optimization

---

### Task 2.4: Implement Database Vacuuming
**Priority:** P1
**Effort:** 0.5 days

**Subtasks:**
1. Add VACUUM operation to repository
2. Schedule automatic vacuuming (on app exit or background)
3. Add progress indicator
4. Test with large databases

**Files:**
- `BrowserRepositoryImpl.kt` (add vacuum method)

**Acceptance Criteria:**
- [ ] Database vacuumed automatically
- [ ] Database size reduced (measure)

---

### Task 2.5: Optimize WebView State Serialization
**Priority:** P1
**Effort:** 0.5 days
**Current:** Base64 encoding overhead

**Subtasks:**
1. Profile serialization performance
2. Consider compression (GZIP)
3. Implement if beneficial
4. Measure improvements

**Files:**
- `WebViewLifecycle.kt` (optimize serialization)

**Acceptance Criteria:**
- [ ] Serialization optimized (if needed)
- [ ] No regression in tab switching speed

---

### Task 2.6: Memory Profiling and Optimization
**Priority:** P1
**Effort:** 1 day

**Subtasks:**
1. Run memory profiler on app
2. Identify memory leaks (if any)
3. Optimize bitmap loading (favicons)
4. Verify WebView pooling prevents OOM
5. Test on low-memory devices (<4GB RAM)

**Acceptance Criteria:**
- [ ] No memory leaks detected
- [ ] App runs smoothly on 2GB RAM devices
- [ ] WebView pool stays within 500MB limit

---

## Phase 3: Documentation Excellence (Week 3-4) 🟡 P1

**Goal:** Comprehensive documentation at all levels
**Duration:** 10 days
**Tasks:** 10

### Task 3.1: Configure Dokka for API Documentation
**Priority:** P1
**Effort:** 1 day

**Subtasks:**
1. Add Dokka plugin (1.9.10)
2. Configure Dokka output (HTML)
3. Add source links to GitHub
4. Generate initial documentation
5. Review output quality
6. Set up automated generation

**Files:**
- `build.gradle.kts` (add Dokka plugin)
- `Module.md` (module-level docs)

**Acceptance Criteria:**
- [ ] Dokka configured
- [ ] HTML docs generated
- [ ] Source links working
- [ ] Ready for publishing

---

### Task 3.2: Document All ViewModels
**Priority:** P1
**Effort:** 2 days

**ViewModels to Document:**
1. TabViewModel (370 lines)
2. SettingsViewModel (352 lines)
3. HistoryViewModel (257 lines)
4. FavoriteViewModel (370 lines)
5. DownloadViewModel (289 lines)
6. SecurityViewModel (347 lines)

**For Each ViewModel:**
- Class-level KDoc (purpose, threading model, usage)
- @param for constructor parameters
- Public method documentation
- StateFlow documentation
- Usage examples

**Example:**
```kotlin
/**
 * Manages browser tabs with lifecycle-aware operations.
 *
 * ## Threading Model
 * - UI state updates on Main dispatcher
 * - Repository operations on IO dispatcher
 * - Coroutines scoped to ViewModel lifecycle
 *
 * ## Usage
 * ```kotlin
 * val tabViewModel = TabViewModel(repository)
 * tabViewModel.createTab(url = "https://example.com")
 * val activeTab by tabViewModel.activeTab.collectAsState()
 * ```
 *
 * @param repository Data access layer for tab operations
 */
class TabViewModel(private val repository: BrowserRepository) { ... }
```

**Acceptance Criteria:**
- [ ] All 6 ViewModels have comprehensive KDoc
- [ ] Usage examples included
- [ ] Threading model documented
- [ ] Dokka generates clean API docs

---

### Task 3.3: Document Repository Implementation
**Priority:** P1
**Effort:** 1 day

**Files:**
- `BrowserRepositoryImpl.kt`
- `BrowserRepository.kt` (interface already documented)

**Content:**
- Class-level documentation (architecture, threading)
- Database initialization flow
- State management explanation
- Error handling patterns
- Cleanup requirements

**Acceptance Criteria:**
- [ ] Implementation fully documented
- [ ] Threading model clear
- [ ] Resource management documented

---

### Task 3.4: Create DEVELOPMENT.md
**Priority:** P1
**Effort:** 1 day

**Contents:**
1. **Prerequisites**
   - JDK 17
   - Android SDK 35
   - IDE setup (Android Studio Hedgehog+)

2. **Clone and Build**
   - Git clone instructions
   - Gradle sync
   - Build commands

3. **Project Structure**
   - Module organization
   - Package naming conventions

4. **Debugging**
   - Run configurations
   - Remote debugging
   - WebView debugging

5. **Platform-Specific Notes**
   - Android development
   - iOS (Phase 2)
   - Desktop (Phase 2)

6. **Troubleshooting**
   - Common build errors
   - Gradle issues
   - WebView issues

**File:**
- `DEVELOPMENT.md` (NEW)

**Acceptance Criteria:**
- [ ] New developer can set up in <1 hour
- [ ] All common issues covered
- [ ] Clear step-by-step instructions

---

### Task 3.5: Create Architecture Decision Records (ADRs)
**Priority:** P1
**Effort:** 1 day

**ADRs to Create:**
1. ADR-001: Use SQLDelight for Database
2. ADR-002: Use Voyager for Navigation
3. ADR-003: KMP Architecture Decisions
4. ADR-004: Threading Model (Coroutines + Dispatchers)
5. ADR-005: WebView Pooling Strategy

**Template:**
```markdown
# ADR-XXX: Title

## Status
Accepted | Proposed | Deprecated

## Context
Background and problem statement

## Decision
What was decided

## Consequences
### Positive
- Benefits

### Negative
- Drawbacks

## Alternatives Considered
1. Alternative 1 - Why rejected
2. Alternative 2 - Why rejected
```

**Directory:**
- `docs/adr/` (NEW)

**Acceptance Criteria:**
- [ ] 5 key ADRs documented
- [ ] Template followed consistently
- [ ] Rationale clear

---

### Task 3.6: Create User Documentation
**Priority:** P1
**Effort:** 2 days

**Documents to Create:**

1. **VOICE_COMMANDS.md**
   - All voice command categories
   - Examples for each command
   - Tips and tricks

2. **SETTINGS.md**
   - All 70 settings explained
   - Default values
   - Recommended configurations

3. **FEATURES.md**
   - Browser features overview
   - How to use each feature
   - Screenshots/demos

4. **FAQ.md**
   - Common questions
   - Quick answers

5. **TROUBLESHOOTING.md**
   - Common problems
   - Solutions

**Directory:**
- `docs/user/` (NEW)

**Acceptance Criteria:**
- [ ] All 5 user guides created
- [ ] Clear, non-technical language
- [ ] Examples included

---

### Task 3.7: Document All Composable Functions
**Priority:** P1
**Effort:** 1 day

**Files to Document:**
- `BrowserScreen.kt`
- `AddressBar.kt`
- `BottomCommandBar.kt`
- `VoiceCommandsDialog.kt`
- All other UI files

**For Each @Composable:**
- Purpose and usage
- @param documentation
- Usage examples (if complex)

**Acceptance Criteria:**
- [ ] All public Composables documented
- [ ] KDoc coverage >80%

---

### Task 3.8: Create ARCHITECTURE.md
**Priority:** P1
**Effort:** 1 day

**Contents:**
1. System Overview
2. Layer Architecture (Presentation, Domain, Data)
3. Module Structure
4. Threading Model
5. State Management
6. Navigation Architecture
7. WebView Architecture
8. Component Diagrams (ASCII)
9. Sequence Diagrams (key flows)

**File:**
- `ARCHITECTURE.md` (NEW)

**Acceptance Criteria:**
- [ ] Complete system architecture documented
- [ ] Diagrams included
- [ ] Easy to understand

---

### Task 3.9: Create SECURITY.md
**Priority:** P1
**Effort:** 0.5 days

**Contents:**
1. Security Features Overview
2. Data Encryption
3. Credential Storage
4. Certificate Pinning
5. Permission Management
6. Privacy Controls
7. Reporting Vulnerabilities

**File:**
- `SECURITY.md` (NEW)

**Acceptance Criteria:**
- [ ] All security features documented
- [ ] Vulnerability reporting process clear

---

### Task 3.10: Generate and Publish Dokka Documentation
**Priority:** P1
**Effort:** 0.5 days

**Subtasks:**
1. Generate final Dokka HTML
2. Set up GitHub Pages
3. Publish documentation
4. Add link to README
5. Configure versioned docs

**Acceptance Criteria:**
- [ ] API docs published to GitHub Pages
- [ ] Link in README
- [ ] Versioning configured

---

## Phase 4: Advanced Features (Week 5-6) 🟢 P2

**Goal:** Complete advanced browser features
**Duration:** 10 days
**Tasks:** 8

### Task 4.1: Enable WebView Remote Debugging
**Priority:** P2
**Effort:** 0.5 days

**Implementation:**
```kotlin
// WebAvanueApp.kt or MainActivity.kt
if (BuildConfig.DEBUG) {
    WebView.setWebContentsDebuggingEnabled(true)
}
```

**Acceptance Criteria:**
- [ ] Remote debugging enabled in debug builds
- [ ] Chrome DevTools can connect
- [ ] Documented in DEVELOPMENT.md

---

### Task 4.2: Implement Console Message Capture
**Priority:** P2
**Effort:** 1 day

**Subtasks:**
1. Override onConsoleMessage in WebChromeClient
2. Create ConsoleViewModel for UI display
3. Add console UI panel (optional)
4. Log console messages with Napier
5. Test with various console.log/error/warn

**Files:**
- `WebViewConfigurator.kt` (capture messages)
- `ConsoleViewModel.kt` (NEW - optional)
- `Logger.kt` (log console messages)

**Implementation:**
```kotlin
override fun onConsoleMessage(msg: ConsoleMessage?): Boolean {
    msg?.let {
        val level = when (it.messageLevel()) {
            ERROR -> "ERROR"
            WARNING -> "WARN"
            LOG -> "INFO"
            DEBUG -> "DEBUG"
            else -> "LOG"
        }
        Logger.info("WebConsole", "[$level] ${it.message()} (${it.sourceId()}:${it.lineNumber()})")
    }
    return true
}
```

**Acceptance Criteria:**
- [ ] Console messages captured
- [ ] Logged with Napier
- [ ] Accessible in debug builds

---

### Task 4.3: Implement Fullscreen API
**Priority:** P2
**Effort:** 1 day

**Subtasks:**
1. Override onShowCustomView()
2. Override onHideCustomView()
3. Add fullscreen container to layout
4. Handle video fullscreen
5. Add exit fullscreen controls
6. Test with YouTube/video sites

**Files:**
- `WebViewConfigurator.kt` (fullscreen handlers)
- `BrowserScreen.kt` (fullscreen container)

**Implementation:**
```kotlin
override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
    if (customView != null) {
        callback?.onCustomViewHidden()
        return
    }

    customView = view
    customViewCallback = callback

    // Hide UI, show fullscreen view
    addressBar.visibility = View.GONE
    commandBar.visibility = View.GONE
    fullscreenContainer.addView(view)
    fullscreenContainer.visibility = View.VISIBLE
}

override fun onHideCustomView() {
    if (customView == null) return

    fullscreenContainer.removeView(customView)
    fullscreenContainer.visibility = View.GONE
    addressBar.visibility = View.VISIBLE
    commandBar.visibility = View.VISIBLE

    customView = null
    customViewCallback?.onCustomViewHidden()
}
```

**Acceptance Criteria:**
- [ ] Video fullscreen works
- [ ] Exit fullscreen functional
- [ ] No UI glitches

---

### Task 4.4: Implement Print Support
**Priority:** P2
**Effort:** 1 day

**Subtasks:**
1. Create print adapter from WebView
2. Add print menu item
3. Handle print completion
4. Test print output
5. Document print feature

**Files:**
- `BrowserScreen.kt` (print menu)
- `WebViewController.kt` (print method)

**Implementation:**
```kotlin
fun printPage(webView: WebView, context: Context) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
    val printAdapter = webView.createPrintDocumentAdapter("WebPage")

    printManager.print(
        "WebAvanue - ${webView.title}",
        printAdapter,
        PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
    )
}
```

**Acceptance Criteria:**
- [ ] Print dialog opens
- [ ] Page prints correctly
- [ ] PDF export works

---

### Task 4.5: Implement Settings UI Completion
**Priority:** P2
**Effort:** 2 days

**Missing Settings UI:**
1. Download Path picker
2. Cache Management UI
3. Custom Search Engine editor
4. User Agent editor

**Files:**
- `SettingsScreen.kt` (add UI)

**Implementation Examples:**
```kotlin
// Download Path Picker
SettingItem(
    title = "Download Location",
    subtitle = downloadPath.takeLastPathSegments(2)
) {
    FolderPickerDialog(
        currentPath = downloadPath,
        onPathSelected = { newPath ->
            viewModel.updateDownloadPath(newPath)
        }
    )
}

// Cache Management
SettingItem(
    title = "Clear Cache",
    subtitle = "Current size: ${cacheSize.formatBytes()}"
) {
    Button(onClick = { viewModel.clearCache() }) {
        Text("Clear All Data")
    }
}

// Custom Search Engine
SettingItem(
    title = "Search Engine",
    subtitle = searchEngine.name
) {
    SearchEngineDialog(
        currentEngine = searchEngine,
        onEngineSelected = { viewModel.updateSearchEngine(it) },
        onCustomEngineAdded = { viewModel.addCustomEngine(it) }
    )
}

// User Agent Editor
SettingItem(
    title = "User Agent",
    subtitle = userAgent.take(50) + "..."
) {
    UserAgentDialog(
        currentUserAgent = userAgent,
        onUserAgentChanged = { viewModel.updateUserAgent(it) }
    )
}
```

**Acceptance Criteria:**
- [ ] All 4 settings have UI
- [ ] Folder picker works
- [ ] Cache management functional
- [ ] Custom search engine can be added
- [ ] User agent can be customized

---

### Task 4.6: WCAG 2.1 AA Compliance Audit
**Priority:** P2
**Effort:** 2 days

**Subtasks:**
1. Run Android Accessibility Scanner
2. Add missing contentDescription
3. Implement semantic modifiers
4. Fix color contrast issues
5. Add keyboard navigation
6. Test with TalkBack

**Files:**
- All UI Composables

**Implementation:**
```kotlin
// Semantic modifiers
Icon(
    imageVector = Icons.Default.Bookmark,
    contentDescription = "Add to bookmarks",
    modifier = Modifier.semantics {
        role = Role.Button
        stateDescription = if (isFavorite) "Bookmarked" else "Not bookmarked"
    }
)

// Keyboard navigation
Box(
    modifier = Modifier
        .focusable()
        .onKeyEvent { event ->
            when (event.key) {
                Key.Enter -> { onSubmit(); true }
                Key.Escape -> { clearFocus(); true }
                else -> false
            }
        }
)
```

**Acceptance Criteria:**
- [ ] Accessibility Scanner passes
- [ ] All interactive elements have contentDescription
- [ ] Color contrast ratio >4.5:1
- [ ] Keyboard navigation works
- [ ] TalkBack functional

---

### Task 4.7: UI Polish and Animations
**Priority:** P2
**Effort:** 1.5 days

**Subtasks:**
1. Add command bar slide animations
2. Add button press animations
3. Add loading indicators
4. Add haptic feedback
5. Optimize for AR/XR

**Files:**
- `BottomCommandBar.kt` (animations)
- `AddressBar.kt` (animations)
- `BrowserScreen.kt` (loading states)

**Acceptance Criteria:**
- [ ] Smooth animations throughout
- [ ] Haptic feedback on button press
- [ ] Loading states for async operations

---

### Task 4.8: Advanced Web Standards Testing
**Priority:** P2
**Effort:** 1 day

**Subtasks:**
1. Create test HTML pages for:
   - IndexedDB
   - Service Workers
   - WebAssembly
   - WebRTC getUserMedia()
2. Test each feature in WebAvanue
3. Document supported features
4. Create web standards compatibility matrix

**Files:**
- `tests/webstandards/` (NEW - test pages)
- `WEB_STANDARDS.md` (NEW - compatibility doc)

**Acceptance Criteria:**
- [ ] All features tested
- [ ] Compatibility matrix created
- [ ] Known limitations documented

---

## Phase 5: Platform Expansion (Future) 🟢 P3

**Note:** iOS and Desktop implementation deferred to Phase 2 of project

**Placeholder Tasks:**
1. iOS WKWebView implementation (5 days)
2. Desktop JCEF implementation (5 days)
3. WebXR JavaScript bridge (3 days)

---

## Task Summary

| Phase | Tasks | Effort (days) | Priority |
|-------|-------|---------------|----------|
| **Phase 1: Security & Infrastructure** | 7 | 8 | P0 🔴 |
| **Phase 2: Performance** | 6 | 7 | P1 🟡 |
| **Phase 3: Documentation** | 10 | 10 | P1 🟡 |
| **Phase 4: Advanced Features** | 8 | 10 | P2 🟢 |
| **TOTAL** | **31** | **35 days** | - |

---

## Timeline

**With 2 Developers:**
- **Phase 1:** Week 1 (5 days)
- **Phase 2:** Week 2 (5 days)
- **Phase 3:** Weeks 3-4 (10 days)
- **Phase 4:** Weeks 5-6 (10 days)

**Total: 6 weeks**

**With 1 Developer:**
- Total: ~7-8 weeks

---

## Success Criteria

### Phase 1 Complete
- [✅] Database encrypted with SQLCipher
- [✅] Credentials stored securely (EncryptedSharedPreferences)
- [✅] ProGuard properly configured
- [✅] Napier logging replacing all println()
- [✅] Sentry crash reporting active
- [✅] Certificate pinning implemented
- [✅] All dependencies updated

### Phase 2 Complete
- [✅] Startup time <100ms (P95)
- [✅] Database indexes added
- [✅] UI recomposition optimized (1 per tab switch)
- [✅] Memory usage optimized

### Phase 3 Complete
- [✅] Dokka API docs published
- [✅] 80%+ KDoc coverage
- [✅] DEVELOPMENT.md complete
- [✅] 5 ADRs created
- [✅] User documentation complete

### Phase 4 Complete
- [✅] Remote debugging enabled
- [✅] Console messages captured
- [✅] Fullscreen API working
- [✅] Print support functional
- [✅] All 70 settings have UI
- [✅] WCAG 2.1 AA compliant
- [✅] Web standards tested and documented

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **Database migration issues** | MEDIUM | HIGH | Test migration thoroughly, backup data |
| **ProGuard breaks features** | MEDIUM | HIGH | Comprehensive release testing |
| **Performance regressions** | LOW | MEDIUM | Continuous benchmarking |
| **Dependency conflicts** | LOW | MEDIUM | Test updates incrementally |
| **Documentation scope creep** | MEDIUM | LOW | Stick to essential docs first |

---

## Next Steps

Tasks will be created in TodoWrite for tracking. Implementation will proceed in priority order (P0 → P1 → P2).

**Ready to begin implementation with task tracking.**
