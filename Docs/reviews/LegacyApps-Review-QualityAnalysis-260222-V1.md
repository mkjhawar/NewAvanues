# Legacy & Secondary Apps — Quality Analysis Review
**Date:** 260222
**Reviewer:** code-reviewer agent
**Branch:** VoiceOS-1M-SpeechEngine

---

## Scope

| # | App | Path | Files |
|---|-----|------|-------|
| 1 | VoiceAvanue (current) | `Apps/voiceavanue/` | 12 kt |
| 2 | VoiceAvanue Legacy | `Apps/voiceavanue-legacy/` | 12 kt |
| 3 | WebAvanue Standalone | `android/apps/webavanue/` | 10 kt |
| 4 | WebAvanue Legacy | `android/apps/webavanue-legacy/` | 10 kt |
| 5 | VoiceOS App | `android/apps/VoiceOS/` | 6 kt |

---

## Summary

| App | P0 | P1 | P2 | Total |
|-----|----|----|----|-------|
| VoiceAvanue (current) | 4 | 5 | 8 | 17 |
| VoiceAvanue Legacy | 1 | 1 | 6 | 8 |
| WebAvanue Standalone | 3 | 4 | 6 | 13 |
| WebAvanue Legacy | 1 | 0 | 1 | 2 |
| VoiceOS App | 0 | 4 | 7 | 11 |
| **Total** | **9** | **14** | **28** | **51** |

---

---

# App 1: VoiceAvanue (current) — `Apps/voiceavanue/`

## Migration Status

This app is the **partially-migrated successor** to `voiceavanue-legacy`. It shares the same package namespace (`com.augmentalis.voiceavanue`) as the legacy build but uses `applicationId = com.augmentalis.voiceavanue`. The consolidated `apps/avanues/` app is intended to supersede both.

| Component | Migrated to consolidated? | Notes |
|-----------|--------------------------|-------|
| Theme tokens | Partial | Current uses `AvanueTheme.colors.*` in screens but `VoiceAvanueTheme` wraps `MaterialTheme`, not `AvanueThemeProvider` |
| CursorOverlay | No | Stub only — no implementation |
| BootReceiver | No | Logic commented out entirely |
| BrowserScreen | No | Raw WebView, not the WebAvanue module composable |
| Settings persistence | No | All settings are local `remember` state — never saved |
| Voice quick action | No | `/* TODO */` stub |
| Cursor quick action | No | `/* TODO */` stub |
| Learn quick action | No | `/* TODO */` stub |
| Bookmarks quick action | No | `/* TODO */` stub |
| AVID voice identifiers | No | Zero AVID on any interactive element |

**Conclusion:** This app is a scaffold with theme improvements over the legacy version but is not functionally complete. It cannot serve as a production replacement.

---

## P0 Issues — VoiceAvanue (current)

### P0-1: `VoiceAvanueTheme` wraps `MaterialTheme`, not `AvanueThemeProvider`
**File:** `Apps/voiceavanue/src/main/kotlin/com/augmentalis/voiceavanue/ui/theme/Theme.kt` L29-60
**Issue:** `VoiceAvanueTheme` creates a Material3 `colorScheme` and calls `MaterialTheme(colorScheme = colorScheme, ...)`. This does NOT set up `AvanueTheme.colors` CompositionLocals. Any composable downstream that reads `AvanueTheme.colors.*` will resolve to uninitialized defaults.
**Effect:** All `AvanueTheme.colors.*` calls in `HomeScreen`, `SettingsScreen`, `BrowserScreen` resolve to zero-initialized color values at runtime, causing invisible text, wrong backgrounds, or crashes.
**Fix:** Replace the `MaterialTheme` wrapper with `AvanueThemeProvider(colors = palette.colors(isDark), glass = palette.glass(isDark), water = palette.water(isDark), materialMode = style, isDark = isDark)`.

### P0-2: `CursorOverlayService` is a registered foreground service with empty body
**File:** `Apps/voiceavanue/src/main/kotlin/com/augmentalis/voiceavanue/service/CursorOverlayService.kt` L25-60
**Issue:** `onStartCommand()` contains only `// TODO: Initialize cursor overlay` comments. `onDestroy()` contains only `// TODO: Stop cursor overlay`. The service is declared in `AndroidManifest.xml` and the "Enable Overlay" path tries to start it. Starting this service foregrounds the app with a notification, acquires a `WindowManager` lock, and does nothing — wasting resources and showing the user a spurious notification.
**Fix:** Either implement the cursor overlay or remove the service and its manifest entry. Per Rule 1, do not leave stubs in production code.

### P0-3: `BootReceiver` is a registered receiver that does nothing
**File:** `Apps/voiceavanue/src/main/kotlin/com/augmentalis/voiceavanue/service/BootReceiver.kt` L15-35
**Issue:** The auto-start logic is commented out with `// TODO: Start services on boot`. The receiver is declared in `AndroidManifest.xml` with `BOOT_COMPLETED` intent filter. It fires on every boot, does nothing, and wastes system resources.
**Fix:** Implement the boot start logic or remove the receiver registration from the manifest entirely.

### P0-4: `Routes.PERMISSIONS` constant defined but route never registered
**File:** `Apps/voiceavanue/src/main/kotlin/com/augmentalis/voiceavanue/MainActivity.kt` L84
**Issue:** `object Routes` defines `const val PERMISSIONS = "permissions"` but the `NavHost` block has no `composable(Routes.PERMISSIONS) { ... }` entry. Any `navigate(Routes.PERMISSIONS)` call will crash with `IllegalArgumentException` (unknown route).
**Fix:** Either add the permissions screen composable or remove the dead constant.

---

## P1 Issues — VoiceAvanue (current)

### P1-1: `VoiceRecognitionService` passes empty string as wake command
**File:** `Apps/voiceavanue/src/main/kotlin/com/augmentalis/voiceavanue/service/VoiceRecognitionService.kt` L45
**Code:** `accessibilityService.processVoiceCommand("", 0f)`
**Issue:** Called to "wake up VoiceOSCore" but passes an empty string command with 0f confidence. `processVoiceCommand` will attempt to match an empty string through the full command pipeline. This either crashes (empty phrase matching NullPointerException) or produces a spurious command dispatch with 0% confidence.
**Fix:** Use a defined wake protocol (e.g., a dedicated `wakeUp()` method on the accessibility service interface) rather than a fake empty command.

### P1-2: `applicationScope` uses `Dispatchers.Main` for background operations
**File:** `Apps/voiceavanue/src/main/kotlin/com/augmentalis/voiceavanue/VoiceAvanueApplication.kt` L30
**Code:** `private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)`
**Issue:** `Dispatchers.Main` is the UI thread dispatcher. Using it for `applicationScope` means all coroutines launched from this scope (including RPC server startup) run on the main thread, blocking the UI. Should use `Dispatchers.Default` for CPU work or `Dispatchers.IO` for IO work.
**Fix:** Change to `CoroutineScope(SupervisorJob() + Dispatchers.Default)`.

### P1-3: All settings switches are volatile — settings lost on restart
**File:** `Apps/voiceavanue/src/main/kotlin/com/augmentalis/voiceavanue/ui/settings/SettingsScreen.kt` L35-39
**Code:**
```kotlin
var dwellClickEnabled by remember { mutableStateOf(true) }
var dwellClickDelay by remember { mutableStateOf(1500f) }
var cursorSmoothing by remember { mutableStateOf(true) }
var voiceFeedback by remember { mutableStateOf(true) }
var autoStartOnBoot by remember { mutableStateOf(false) }
```
**Issue:** All settings are local Compose `remember` state. They reset to defaults on every app restart. `SettingsSwitch` and `SettingsSlider` callbacks write back to local state only, never to DataStore or SharedPreferences.
**Fix:** Inject a settings ViewModel backed by DataStore (or `AvanuesSettings` from the Foundation module) and read/write through that.

### P1-4: `normalizeUrl()` in `BrowserScreen` duplicates WebAvanue module logic
**File:** `Apps/voiceavanue/src/main/kotlin/com/augmentalis/voiceavanue/ui/browser/BrowserScreen.kt` L80-100
**Issue:** `BrowserScreen.kt` implements its own URL normalization (`https://` prefix, no-dot search fallback) that duplicates logic already present in the `WebAvanue` module's `processUrl()`. Two diverging implementations will produce different URL behaviors between the standalone browser and the module browser.
**Fix:** Import and call `processUrl()` from the `WebAvanue` module dependency (already on the classpath). Delete the local copy.

### P1-5: Zero AVID voice identifiers on all interactive elements
**Files:** `HomeScreen.kt`, `SettingsScreen.kt`, `BrowserScreen.kt`
**Issue:** Per project mandatory rule (CLAUDE.md §7), ALL interactive elements must have AVID `Modifier.semantics { contentDescription = "Voice: ..." }` blocks. The following have none:
- `HomeScreen.kt` L234: `Button(onClick = onEnable)` in `StatusCard`
- `HomeScreen.kt` L257: `Card(modifier = modifier, onClick = onClick)` in `QuickActionCard`
- `SettingsScreen.kt` L261: `Modifier.clickable(onClick = onClick)` in `SettingsItem`
- `SettingsScreen.kt` L284: `Switch(checked = checked, ...)` in `SettingsSwitch`
- `SettingsScreen.kt` L318: `Slider(value = value, ...)` in `SettingsSlider`
- `BrowserScreen.kt` L55: Back navigation `IconButton`
- `BrowserScreen.kt` L60: Voice `IconButton`
- `BrowserScreen.kt` L65: Bookmarks `IconButton`
**Fix:** Add semantics blocks to all 8+ elements listed above.

---

## P2 Issues — VoiceAvanue (current)

### P2-1: `Theme.kt` uses deprecated `window.statusBarColor` API
**File:** `Apps/voiceavanue/src/main/kotlin/com/augmentalis/voiceavanue/ui/theme/Theme.kt` L62
**Issue:** `window.statusBarColor = Color.Transparent.toArgb()` is deprecated in API 35. Use `enableEdgeToEdge()` (already called in `MainActivity.kt`) instead. `enableEdgeToEdge()` handles status bar color automatically.

### P2-2: `checkPermissions()` in `MainActivity` has empty if-bodies
**File:** `Apps/voiceavanue/src/main/kotlin/com/augmentalis/voiceavanue/ui/MainActivity.kt` L64-73
**Issue:** `checkPermissions()` checks permission state but the `if (!enabled)` blocks contain only comments. The method does nothing. Called every `onResume()` but provides no value.
**Fix:** Either remove the empty method and `onResume()` override, or implement actual permission prompting logic.

### P2-3: `dynamicColor` flag used but Material You is not part of AvanueTheme v5.1
**File:** `Apps/voiceavanue/src/main/kotlin/com/augmentalis/voiceavanue/ui/theme/Theme.kt` L38-44
**Issue:** `Theme.kt` checks `dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S` and creates `dynamicDarkColorScheme(context)` / `dynamicLightColorScheme(context)`. This contradicts the AvanueTheme v5.1 system which uses fixed palettes (SOL/LUNA/TERRA/HYDRA). Material You dynamic colors will override AvanueTheme palette selections.
**Fix:** Remove the dynamic color path. Use `AvanueColorPalette.HYDRA` (default) or read palette from DataStore.

### P2-4: Raw `WebView` in `BrowserScreen` instead of WebAvanue module composable
**File:** `Apps/voiceavanue/src/main/kotlin/com/augmentalis/voiceavanue/ui/browser/BrowserScreen.kt` L35-75
**Issue:** `BrowserScreen.kt` uses `AndroidView { WebView(context) }` directly rather than the composable wrapper from the `WebAvanue` module. The `WebAvanue` module is already on the classpath. Features like voice command integration, tab management, and favorites in `WebAvanue` are inaccessible from this raw `WebView`.
**Fix:** Replace raw `WebView` with the `WebAvanueScreen` composable from the `WebAvanue` module.

### P2-5: `StatusCard` icon has `contentDescription = null`
**File:** `Apps/voiceavanue/src/main/kotlin/com/augmentalis/voiceavanue/ui/home/HomeScreen.kt` L206
**Issue:** The status icon has `contentDescription = null` but the card represents an actionable system status (Accessibility enabled/disabled). Screen readers get no description of the icon's meaning.
**Fix:** Set `contentDescription = "$title status icon"` or similar.

### P2-6: `ModuleInfoCard` status indicator has inadequate size for tap target
**File:** `Apps/voiceavanue/src/main/kotlin/com/augmentalis/voiceavanue/ui/home/HomeScreen.kt` L306-318
**Issue:** The activity indicator `Box` is `12.dp` with `padding(2.dp)` leaving a `10.dp` visible dot. Android minimum tap target is 48dp. While this is not clickable, it is too small to be visually useful and users with visual impairments will struggle to distinguish on/off states.
**Fix:** Increase to at least 16dp dot, or add a text label ("Active" / "Inactive").

### P2-7: `VoiceAvanueApplication.getInstance()` is a global singleton anti-pattern
**File:** `Apps/voiceavanue/src/main/kotlin/com/augmentalis/voiceavanue/VoiceAvanueApplication.kt` L20-25
**Issue:** `companion object { fun getInstance(): VoiceAvanueApplication }` exposes the Application as a global singleton. With Hilt already in the project, dependencies should be injected rather than fetched from the Application singleton.
**Fix:** Use `@HiltAndroidApp` entry points (`EntryPointAccessors`) or inject via constructor.

### P2-8: Multiple `/* TODO */` quick action handlers in `HomeScreen`
**File:** `Apps/voiceavanue/src/main/kotlin/com/augmentalis/voiceavanue/ui/home/HomeScreen.kt` L122, L136, L143
**Issue:** Voice, Cursor, and Learn quick action cards have `onClick = { /* TODO: ... */ }`. These are visible to the user as tappable buttons but do nothing. This degrades user trust.
**Fix:** Either implement the handlers or visually disable the buttons with `enabled = false` and explain why.

---

---

# App 2: VoiceAvanue Legacy — `Apps/voiceavanue-legacy/`

## Migration Status

This is the **direct predecessor** of `Apps/voiceavanue/`. The only structural differences are:
- `applicationId = com.augmentalis.voiceavanue.legacy`
- All theme colors use `MaterialTheme.colorScheme.*` instead of `AvanueTheme.colors.*`
- `SettingsScreen.kt` uses `Icons.AutoMirrored.Filled.List` (deprecated API corrected) vs current's `Icons.Default.List`

**Conclusion:** This app should be archived/removed once the consolidated `apps/avanues/` app reaches feature parity. It serves no function that the current `voiceavanue` does not, and the current app is itself incomplete.

---

## P0 Issues — VoiceAvanue Legacy

### P0-1: Pervasive `MaterialTheme.colorScheme.*` usage (banned)
**Files:** `ui/home/HomeScreen.kt`, `ui/settings/SettingsScreen.kt`, `MainActivity.kt`
**Issue:** CLAUDE.md Mandatory Rule #3 bans all `MaterialTheme.colorScheme.*` usage. Legacy uses it in 15+ places:
- `MainActivity.kt` L51: `color = MaterialTheme.colorScheme.background`
- `HomeScreen.kt` L194: `containerColor = MaterialTheme.colorScheme.primaryContainer`
- `HomeScreen.kt` L197: `MaterialTheme.colorScheme.errorContainer`
- `HomeScreen.kt` L210: `MaterialTheme.colorScheme.primary`
- `HomeScreen.kt` L213: `MaterialTheme.colorScheme.error`
- `HomeScreen.kt` L226-229: `MaterialTheme.colorScheme.primary`, `MaterialTheme.colorScheme.error`
- `HomeScreen.kt` L269: `MaterialTheme.colorScheme.primary`
- `SettingsScreen.kt` L238: `MaterialTheme.colorScheme.primary`
- `SettingsScreen.kt` L258: `MaterialTheme.colorScheme.onSurfaceVariant`
- `SettingsScreen.kt` L280: `MaterialTheme.colorScheme.onSurfaceVariant`
- `SettingsScreen.kt` L306: `MaterialTheme.colorScheme.onSurfaceVariant`
**Fix:** Migrate all to `AvanueTheme.colors.*` equivalents. This is also blocked by P0-1 in the current app (the `VoiceAvanueTheme` wrapper must be `AvanueThemeProvider` for these to resolve correctly).

---

## P1 Issues — VoiceAvanue Legacy

### P1-1: Namespace conflict with current app
**File:** `Apps/voiceavanue-legacy/build.gradle.kts`
**Issue:** `namespace = "com.augmentalis.voiceavanue"` is identical to the current app's namespace. Both apps generate R classes in the same package. If both variants are built in the same Gradle invocation, their R classes will collide.
**Fix:** Change the legacy namespace to `com.augmentalis.voiceavanue.legacy` to match its `applicationId`.

---

## P2 Issues — VoiceAvanue Legacy

### P2-1 through P2-6: All P1-3 through P2-8 issues from current app also apply
The legacy app shares all the same structural issues (empty `checkPermissions()`, volatile settings state, no AVID, stub CursorOverlayService, dead BootReceiver, etc.) because the source files are nearly identical with only theme token differences.

---

---

# App 3: WebAvanue Standalone — `android/apps/webavanue/`

## Migration Status

This is the **standalone WebAvanue browser app** — a separate product from the voice-focused VoiceAvanue. It has substantially more complete UI code than the VoiceAvanue apps (tab management, favorites, voice command dialog, IPC receiver). However it has several critical data integrity and testing issues.

| Component | Status | Notes |
|-----------|--------|-------|
| Tab management | Implemented | `BrowserApp.kt` handles multi-tab via `TabManager` |
| Favorites | Implemented | `FavoritesDialog` with persistence via `FavoritesRepository` |
| Voice commands | Partial | IPC receiver wired; `handleVoiceCommand()` uses naive string matching |
| Database encryption | Broken stub | Comment says "simplified approach: just mark as encrypted" — data never encrypted |
| Crash reporting (Sentry) | Disabled | DSN is a placeholder string |
| E2E tests | Meaningless | All assertions are `assertTrue(true)` |
| Theme compliance | Violated | Multiple `MaterialTheme.colorScheme.*` usages |
| AVID identifiers | Missing | Zero AVID on any interactive element |

---

## P0 Issues — WebAvanue Standalone

### P0-1: SQLCipher encryption is a no-op stub — data never encrypted
**File:** `android/apps/webavanue/src/main/kotlin/com/augmentalis/webavanue/app/DatabaseMigrationHelper.kt` L100-125
**Code comment at L112:** `"For now, we'll use a simplified approach: just mark as encrypted and trust that the new driver will handle it"`
**Issue:** The encryption migration method calls `PRAGMA key` but then skips the actual `PRAGMA rekey` (in-place re-encryption) step. It sets a SharedPreference flag `"database_encrypted" = true` without ever encrypting the database file. Users who request encryption believe their browser history, passwords, and favorites are encrypted — they are not.
**Classification:** This is a security deception — the UI or documentation implies encryption is active when it is not. **Critical.**
**Fix:** Implement the full SQLCipher re-encryption migration: backup the unencrypted database, create a new encrypted database with `PRAGMA key`, copy all data, verify integrity, replace the original.

### P0-2: Inconsistent encryption default causes migration to never run
**Files:**
- `DatabaseMigrationHelper.kt` L55: `getBoolean("database_encryption", true)` — default `true`
- `WebAvanueApp.kt` L70: `getBoolean("database_encryption", false)` — default `false`
**Issue:** `needsMigration()` reads the preference with default `true` (migration needed). `WebAvanueApp` reads the same key with default `false` (no migration needed). The side effects:
1. On first install, `WebAvanueApp` initializes with `false` (unencrypted driver).
2. `DatabaseMigrationHelper.needsMigration()` reads `false` as well (key exists from step 1 — written as `false`).
3. Migration never runs even for users who later enable encryption.
**Fix:** Agree on a single canonical default. Since the feature is broken (P0-1), set both to `false` until encryption is actually implemented.

### P0-3: All E2E tests are permanently passing no-ops
**File:** `android/apps/webavanue/src/androidTest/kotlin/com/augmentalis/Avanues/web/app/VoiceCommandIPCE2ETest.kt`
**Issue:** All 8 test methods have `assertTrue(true, "message")` as their only assertion. This tests nothing. Every CI run shows 8 green tests while zero real behavior is verified. Additionally, the IPC action used in tests (`com.augmentalis.avamagic.IPC.UNIVERSAL`) does not match the registered receiver action (`com.augmentalis.avanues.web.IPC.COMMAND`). Even if the assertions were real, they would never reach the receiver.
**Fix:** Rewrite all 8 test methods with actual assertions against real IPC behavior using the correct action string `com.augmentalis.avanues.web.IPC.COMMAND`.

---

## P1 Issues — WebAvanue Standalone

### P1-1: `Thread.sleep(500)` in a suspend function blocks IO dispatcher thread
**File:** `android/apps/webavanue/src/main/kotlin/com/augmentalis/webavanue/app/DatabaseMigrationHelper.kt` L88
**Code:** `Thread.sleep(500)` inside `suspend fun performMigration()`
**Issue:** `Thread.sleep()` blocks the underlying IO dispatcher thread, starving other coroutines waiting on that thread. At scale (many concurrent IO operations) this degrades performance.
**Fix:** Replace with `delay(500)` which suspends without blocking the thread.

### P1-2: Sentry DSN is a hardcoded placeholder — crash reporting disabled
**File:** `android/apps/webavanue/src/main/kotlin/com/augmentalis/webavanue/app/WebAvanueApp.kt` L55
**Code:** `"https://YOUR_PUBLIC_KEY@sentry.io/YOUR_PROJECT_ID"`
**Issue:** All production crashes are silently dropped. The Sentry SDK initializes successfully with a placeholder DSN but sends to a non-existent project. If this app reaches users, there is no visibility into crashes.
**Fix:** Replace with the real Sentry DSN for the WebAvanue project, or use a build-time config value from `BuildConfig.SENTRY_DSN`.

### P1-3: Multiple `MaterialTheme.colorScheme.*` usages (banned)
**Files:** `presentation/BrowserTopBar.kt`, `presentation/BrowserBottomBar.kt`, `presentation/Dialogs.kt`
**Specific violations:**
- `BrowserTopBar.kt` L35: `color = MaterialTheme.colorScheme.surface`
- `BrowserTopBar.kt` L52: `tint = MaterialTheme.colorScheme.primary`
- `Dialogs.kt` L38, L72, L110: `MaterialTheme.colorScheme.onSurfaceVariant`
**Fix:** Migrate to `AvanueTheme.colors.*` equivalents (`surface`, `primary`, `textSecondary`). Wrap the app in `AvanueThemeProvider`.

### P1-4: `handleVoiceCommand()` uses naive string contains matching
**File:** `android/apps/webavanue/src/main/kotlin/com/augmentalis/webavanue/BrowserApp.kt` L180-220
**Issue:** `handleVoiceCommand()` uses `lowerCommand.contains("search")`, `lowerCommand.contains("go to")`, etc. This is an ad-hoc NLP implementation that will miss many valid commands and false-positive on others (e.g., "search" in the middle of a search query). The VoiceOSCore `CommandRegistry` and `IntentClassifier` already exist in the module dependency.
**Fix:** Route voice commands through `VoiceOSCore`'s `CommandRegistry.match()` instead of implementing custom string matching.

---

## P2 Issues — WebAvanue Standalone

### P2-1: `isLoading = false` hardcoded — loading state never shown
**File:** `android/apps/webavanue/src/main/kotlin/com/augmentalis/webavanue/BrowserApp.kt` L95
**Code:** `isLoading = false`
**Issue:** The `isLoading` flag is always false. The browser shows no progress indicator while pages load. This is a poor UX regression versus native Chrome.
**Fix:** Wire `isLoading` to `WebViewClient.onPageStarted` / `onPageFinished` callbacks.

### P2-2: Zero AVID voice identifiers on all browser UI elements
**Files:** `BrowserTopBar.kt`, `BrowserBottomBar.kt`, `Dialogs.kt`, `BrowserApp.kt`
**Issue:** No interactive element in the browser has AVID semantics. A voice-controlled browser with no voice identifiers on its own UI controls is a fundamental gap.
**Fix:** Add `Modifier.semantics { contentDescription = "Voice: ..." }` to all interactive elements — back button, forward button, refresh, URL bar, tab button, bookmark button, dialog buttons.

### P2-3: Tab package name in test file uses old namespace
**File:** `android/apps/webavanue/src/androidTest/kotlin/com/augmentalis/Avanues/web/app/VoiceCommandIPCE2ETest.kt`
**Issue:** Test file is in package `com.augmentalis.Avanues.web.app` (capital A, old `avamagic` ancestor namespace). The app itself uses `com.augmentalis.webavanue`. This inconsistency suggests the test was ported from an older codebase and never updated.
**Fix:** Move test to `com.augmentalis.webavanue` package.

### P2-4: `FavoritesRepository` and `TabManager` inject via constructor but no Hilt module defined
**File:** `android/apps/webavanue/src/main/kotlin/com/augmentalis/webavanue/BrowserApp.kt` L30-35
**Issue:** `BrowserApp.kt` creates `TabManager()` and `FavoritesRepository()` with direct instantiation inside a `ViewModel` or Composable. These are not Hilt-provided. Lifecycle and singleton semantics are not guaranteed.
**Fix:** Provide via Hilt `@Module` + `@Provides` or annotate with `@Singleton` + `@Inject constructor`.

### P2-5: `BrowserTopBar.kt` uses `LocalContentColor` directly
**File:** `android/apps/webavanue/src/main/kotlin/com/augmentalis/webavanue/presentation/BrowserTopBar.kt` L58
**Issue:** `LocalContentColor.current` derives from `MaterialTheme`, not `AvanueTheme`. Once the `AvanueThemeProvider` fix (P1-3) is applied, `LocalContentColor` will still derive from Material defaults unless the `AvanueThemeProvider` explicitly bridges it.
**Fix:** Use `AvanueTheme.colors.textPrimary` directly.

### P2-6: `VoiceCommandDialog` does not filter by current app context
**File:** `android/apps/webavanue/src/main/kotlin/com/augmentalis/webavanue/BrowserApp.kt` L150-175
**Issue:** The voice command dialog shows a static list of commands unrelated to the current page context. It should show contextual commands based on the currently loaded URL and page content (consistent with how VoiceOSCore scraping works for other apps).

---

---

# App 4: WebAvanue Legacy — `android/apps/webavanue-legacy/`

## Migration Status

This app is an **exact copy** of `android/apps/webavanue/`. The Kotlin source files are byte-for-byte identical based on inspection. The only expected difference is the `applicationId` in the build file.

**Conclusion:** This should be removed from the repository. There is no value in maintaining an identical copy of a standalone app. If rollback is needed, git history provides it.

---

## P0 Issues — WebAvanue Legacy

### P0-1: Exact code duplication — all P0/P1/P2 issues from WebAvanue Standalone apply verbatim
The encryption stub, the meaningless tests, the theme violations, the zero AVID, and the Sentry placeholder are all present in identical form.

---

## P2 Issues — WebAvanue Legacy

### P2-1: Legacy app serves no purpose — remove from repository
**File:** `android/apps/webavanue-legacy/` (entire directory)
**Issue:** The directory is a full copy of `android/apps/webavanue/`. Maintaining two identical codebases multiplies fix burden — every bug must be fixed in two places. Neither is the consolidated successor app (`apps/avanues/`).
**Fix:** Delete `android/apps/webavanue-legacy/` from the repository. Archive to git history only.

---

---

# App 5: VoiceOS App — `android/apps/VoiceOS/`

## Migration Status

This is the most complete and production-quality of the five apps. It contains:
- `VoiceOSAccessibilityService.kt` — 1288 lines, full production service
- `OverlayService.kt` — 1331 lines, full production overlay with debug FAB, numbers badges, animation
- `OverlayStateManager.kt` — proper singleton state management
- `ElementExtractor.kt` — real accessibility tree extraction
- `ScreenCacheManager.kt` — real screen caching

| Component | Status | Notes |
|-----------|--------|-------|
| Accessibility service | Full implementation | Dedicated speech dispatcher, screen hash caching, SOLID refactoring |
| Overlay service | Full implementation | Numbers overlay, debug FAB, animated panel |
| State management | Production quality | `Collections.synchronizedMap` for thread safety |
| Screen caching | Production quality | Hash-based deduplication |
| Build configuration | Not found | No `build.gradle.kts` or `AndroidManifest.xml` at source level |
| AVID semantics | Missing | Zero AVID on MainActivity UI |
| Theme | Partial | Uses `AvanueTheme.colors.*` but no `AvanueThemeProvider` in `setContent` |

---

## P1 Issues — VoiceOS App

### P1-1: `runBlocking` in companion object accessible from main thread
**File:** `android/apps/VoiceOS/src/main/kotlin/com/augmentalis/voiceos/service/VoiceOSAccessibilityService.kt` L1240-1255
**Code:**
```kotlin
fun getCachedScreenCount(): Int = runBlocking { screenCacheManager.getCachedScreenCount() }
fun getCachedScreenCountForCurrentApp(): Int = runBlocking { ... }
```
**Issue:** `runBlocking` on a `suspend` DB query from `companion object`. If called from the main thread (e.g., from an Activity's `onResume`), this blocks the UI thread for the duration of the database query, potentially causing ANR.
**Fix:** Declare as `suspend fun` or expose as `Flow<Int>` so callers must collect from a coroutine.

### P1-2: `voiceOSCore?.dispose()` may never execute after scope cancellation
**File:** `android/apps/VoiceOS/src/main/kotlin/com/augmentalis/voiceos/service/VoiceOSAccessibilityService.kt` L180-195
**Issue:** In `onDestroy()`, `serviceScope.cancel()` is called, then `serviceScope.launch { voiceOSCore?.dispose() }` is called. Launching a coroutine on a cancelled scope is a no-op — the `dispose()` call is silently dropped. VoiceOSCore resources may not be cleaned up on service destruction.
**Fix:** Call `voiceOSCore?.dispose()` directly (if synchronous) or use a separate, non-cancelled scope for the disposal coroutine.

### P1-3: Notification title contains emoji (banned by project rule)
**File:** `android/apps/VoiceOS/src/main/kotlin/com/augmentalis/voiceos/service/OverlayService.kt` L284
**Code:** `"🎤 VoiceOS Listening"`
**Issue:** CLAUDE.md explicitly prohibits emojis in all files. The notification title contains `🎤`.
**Fix:** Change to `"VoiceOS Listening"`.

### P1-4: `MainActivity` mixes legacy `AlertDialog.Builder` with Compose
**File:** `android/apps/VoiceOS/src/main/kotlin/com/augmentalis/voiceos/MainActivity.kt` L85-120
**Issue:** `MainActivity` uses `android.app.AlertDialog.Builder` (View-based API) to show permission dialogs while the rest of the UI is Compose. This creates an inconsistent UX (different dialog styling, no Compose state integration) and contradicts the Compose-first approach.
**Fix:** Replace with a Compose `AlertDialog` composable driven by Compose state.

---

## P2 Issues — VoiceOS App

### P2-1: `maxAssignedNumber` is an unsynchronized plain `var` field
**File:** `android/apps/VoiceOS/src/main/kotlin/com/augmentalis/voiceos/service/OverlayStateManager.kt` L243
**Code:** `private var maxAssignedNumber = 0`
**Issue:** `maxAssignedNumber` is read and written in `updateNumberedOverlayItemsIncremental()` which can be called from background coroutines. While `avidToNumber` is correctly protected by `Collections.synchronizedMap`, `maxAssignedNumber` is not. Concurrent increments can produce duplicate numbers.
**Fix:** Change to `@Volatile private var maxAssignedNumber = 0` and use `synchronized(avidToNumber)` block when incrementing, or use `AtomicInteger`.

### P2-2: `ElementExtractor` contains Gmail-specific hardcoded patterns
**File:** `android/apps/VoiceOS/src/main/kotlin/com/augmentalis/voiceos/service/ElementExtractor.kt` L220-240
**Issue:** `findTopLevelListItems()` checks for content descriptions containing `"Unread,"`, `"Starred,"`, `"Sent,"` — Gmail-specific text patterns. A general-purpose accessibility extractor should not have app-specific logic. This will produce false positives in other apps whose content descriptions happen to contain those strings.
**Fix:** Extract Gmail-specific detection to a named filter strategy or move to a `GmailElementStrategy` class. The general extractor should use structural patterns (role type, sibling count) not content text.

### P2-3: `AvanueTheme.colors.*` used without `AvanueThemeProvider` wrapper
**File:** `android/apps/VoiceOS/src/main/kotlin/com/augmentalis/voiceos/MainActivity.kt` L55-80
**Issue:** `MainScreen` reads `AvanueTheme.colors.primaryContainer` and `AvanueTheme.colors.background`, but `setContent { VoiceOSTheme { ... } }` uses `VoiceOSTheme` (likely a `MaterialTheme` wrapper), not `AvanueThemeProvider`. The `AvanueTheme.colors` CompositionLocals are uninitialized; all color reads return defaults (likely transparent/black).
**Fix:** Replace `VoiceOSTheme` with `AvanueThemeProvider` or wrap it inside one.

### P2-4: Zero AVID voice identifiers on MainActivity interactive elements
**File:** `android/apps/VoiceOS/src/main/kotlin/com/augmentalis/voiceos/MainActivity.kt`
**Issue:** All buttons in `MainScreen` (accessibility enable button, overlay enable button, permissions screen navigation) have no AVID semantics. A voice OS whose own home screen cannot be controlled by voice is a fundamental contradiction.
**Fix:** Add `Modifier.semantics { contentDescription = "Voice: ..." }` to all interactive elements.

### P2-5: Missing `build.gradle.kts` and `AndroidManifest.xml` at source level
**Path:** `android/apps/VoiceOS/`
**Issue:** No `build.gradle.kts` was found in the VoiceOS app source directory (only build intermediates in `build/`). This means the app's module configuration is unclear — it may be defined at the root `android/` project level or via a different mechanism. Without a discoverable build file, adding new dependencies or changing the app configuration is undocumented.
**Note:** The build intermediates at `android/apps/VoiceOS/build/` confirm the app compiles successfully, so the build file exists somewhere. May be at `android/build.gradle.kts` as a multi-module project.

### P2-6: `OverlayService.onTaskRemoved()` uses inexact alarm on Android 12+
**File:** `android/apps/VoiceOS/src/main/kotlin/com/augmentalis/voiceos/service/OverlayService.kt` L1200-1220
**Issue:** `AlarmManager.set(ELAPSED_REALTIME, ...)` schedules an inexact restart alarm. On Android 12+, inexact alarms can be deferred by the OS by several minutes. If the user removes the task, the overlay may not restart for a long time.
**Fix:** Use `AlarmManager.setExactAndAllowWhileIdle()` (requires `SCHEDULE_EXACT_ALARM` permission, which should be declared) or use a `JobScheduler` job with `setOverrideDeadline()`.

### P2-7: `ScreenCacheManager` delegates every call — potentially thin wrapper
**File:** `android/apps/VoiceOS/src/main/kotlin/com/augmentalis/voiceos/service/ScreenCacheManager.kt`
**Issue:** `ScreenCacheManager` is a thin delegation wrapper around `ScreenHashRepository`. Each method directly calls through with no added behavior. Per project Rule 2 (minimize indirection), if the wrapper adds no value, the `ScreenHashRepository` should be used directly.
**Caveat:** If `ScreenCacheManager` is intended as an abstraction boundary for testing or future cache logic, keep it. If it is genuinely a pass-through, remove it.

---

---

# Cross-Cutting Issues (All Apps)

## DRY Violations

### DRY-1: `normalizeUrl` / `processUrl` duplicated across apps
- `Apps/voiceavanue/src/main/kotlin/com/augmentalis/voiceavanue/ui/browser/BrowserScreen.kt` — local `normalizeUrl()`
- `android/apps/webavanue/src/main/kotlin/com/augmentalis/webavanue/presentation/BrowserTopBar.kt` — local URL processing logic
**Fix:** Extract to `WebAvanue` module's `UrlUtils.kt` and use from both apps.

### DRY-2: Settings switch/slider composables duplicated in voiceavanue and voiceavanue-legacy
Both apps define identical `SettingsSwitch`, `SettingsSlider`, `SettingsItem`, and `SettingsSectionHeader` composables. These should be in the `AvanueUI` module.

### DRY-3: Identical build.gradle.kts dependency blocks in voiceavanue and voiceavanue-legacy
All 20+ dependencies are identical. If the legacy app is not removed, at minimum it should be a build variant of the current app (different `productFlavor`), not a separate module.

## AVID Coverage Summary

No interactive element in any of the five apps has AVID voice identifiers. This is a systemic failure across all secondary/legacy apps.

| App | Interactive elements without AVID |
|-----|----------------------------------|
| voiceavanue | StatusCard Button, QuickActionCard, all settings controls, all browser buttons |
| voiceavanue-legacy | Same as above |
| webavanue | All browser navigation, tab button, favorites button, all dialogs |
| webavanue-legacy | Same as webavanue |
| VoiceOS | All MainActivity controls |

## Theme Violation Summary

| App | `MaterialTheme.colorScheme.*` occurrences |
|-----|------------------------------------------|
| voiceavanue | 1 (Theme.kt — indirect) |
| voiceavanue-legacy | 15+ (throughout HomeScreen, SettingsScreen, MainActivity) |
| webavanue | 8 (BrowserTopBar, Dialogs) |
| webavanue-legacy | 8 (same) |
| VoiceOS | 0 (uses `AvanueTheme.colors.*` but no AvanueThemeProvider) |

---

## Recommendations

### Immediate (before any release)

1. **Delete `Apps/voiceavanue-legacy/`** — it is superceded by `Apps/voiceavanue/` (same code, worse themes). Commit message: `chore: archive voiceavanue-legacy (superceded by consolidated app)`
2. **Delete `android/apps/webavanue-legacy/`** — exact duplicate, no value. Same rationale.
3. **Fix `VoiceAvanueTheme` to wrap `AvanueThemeProvider`** — all AvanueTheme.colors reads are broken without this.
4. **Fix `VoiceOSTheme` in VoiceOS app** — same as above.
5. **Fix `DatabaseMigrationHelper` encryption stub** — a feature that claims to encrypt but does not is a security deception.
6. **Remove `🎤` emoji from `OverlayService` notification title** — violates project rule.
7. **Fix `voiceOSCore?.dispose()` after scope cancellation** in VoiceOSAccessibilityService.

### Short-term (before public beta)

8. **Add AVID semantics to all interactive elements** across all 5 apps.
9. **Persist settings to DataStore** in VoiceAvanue `SettingsScreen`.
10. **Rewrite E2E tests** in WebAvanue with real assertions.
11. **Fix IPC action in E2E tests** from `avamagic.IPC.UNIVERSAL` to `avanues.web.IPC.COMMAND`.
12. **Replace `Thread.sleep(500)` with `delay(500)`** in `DatabaseMigrationHelper`.

### Architectural

13. **Extract `normalizeUrl`/`processUrl` to `WebAvanue` module** — eliminate duplication.
14. **Route `handleVoiceCommand()` through `VoiceOSCore` CommandRegistry** instead of naive string matching.
15. **Locate and document `VoiceOS` build.gradle.kts** — ensure it is discoverable for contributors.
