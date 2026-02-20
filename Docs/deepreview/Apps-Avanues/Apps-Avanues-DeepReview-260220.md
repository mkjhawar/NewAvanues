# Apps/Avanues — Deep Code Review
**Date:** 260220
**Scope:** `apps/` directory — 72 .kt files across three app sub-modules
- `apps/avanues/` — Consolidated Avanues app (primary)
- `apps/voiceavanue/` — Standalone VoiceAvanue target
- `apps/voiceavanue-legacy/` — Legacy app module
**Reviewer:** Code Reviewer Agent

---

## Summary

The primary `apps/avanues/` consolidated app is architecturally sound and largely theme-compliant.
The most severe issues are: `runBlocking` called inside VoiceControl callback lambdas (ANR/deadlock
risk on main thread), the `VoiceRecognitionService` sending an empty-string command to "wake" VoiceOSCore
(which is a silent no-op), and the `DeveloperConsoleScreen` opening SQLite databases synchronously on
the UI thread. The `apps/voiceavanue/` and `apps/voiceavanue-legacy/` modules both contain banned
`MaterialTheme(colorScheme = ...)` theme wrappers — MANDATORY RULE #3 violations. No Rule 7
(AI attribution) violations were found anywhere in `apps/`.

**Finding counts:** 0 Critical | 4 High | 8 Medium | 8 Low = **20 total**

---

## Issues

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| High | `apps/avanues/.../service/VoiceAvanueAccessibilityService.kt:275–317` | `runBlocking` called inside VoiceControlCallbacks lambdas (`onMuteVoice`, `onWakeVoice`, `onStartDictation`, `onStopDictation`). These execute on the callback's calling thread; if that thread is Main, `runBlocking` blocks the UI dispatcher and risks ANR. If called from within an existing coroutine, `runBlocking` deadlocks. | Replace with `serviceScope.launch { core?.stopListening() }`. Pass `serviceScope` to the callback wrapper. |
| High | `apps/avanues/.../service/VoiceRecognitionService.kt:59` | `processVoiceCommand("", 0f)` called to "wake up VoiceOSCore". An empty-string phrase with 0.0 confidence will always fail VoiceOSCore's confidence threshold check and is discarded as a no-op. VoiceOSCore is never actually activated this way. | Remove this call. Use the correct VoiceOSCore API (`core?.startListening()` or a dedicated wake signal) or remove the no-op entirely. |
| High | `apps/avanues/.../ui/developer/DeveloperConsoleScreen.kt:491,520` | `getTablesForDb()` and `getTableRows()` open `SQLiteDatabase.openDatabase()` directly and call cursor operations synchronously. Both are called from `remember {}` blocks inside Composable functions, which execute on the **main/UI thread**. This blocks the UI thread during database I/O. | Move database calls into `viewModelScope.launch(Dispatchers.IO) { }` and expose results via `StateFlow`. Remove blocking calls from `remember {}` blocks. |
| High | `apps/voiceavanue/.../ui/theme/Theme.kt` and `apps/voiceavanue-legacy/.../ui/theme/Theme.kt` | Both files use `MaterialTheme(colorScheme = colorScheme, ...)` as the root theme wrapper. This is the **banned** pattern per MANDATORY RULE #3. `apps/voiceavanue/Theme.kt` also calls `window.statusBarColor = colorScheme.primary.toArgb()` (deprecated API). | Replace with `AvanueThemeProvider(colors = palette.colors(isDark), glass = palette.glass(isDark), water = palette.water(isDark), materialMode = style, isDark = isDark) { ... }`. Remove the `window.statusBarColor` call — use `WindowCompat.getInsetsController()` instead. |
| Medium | `apps/avanues/.../data/AvanuesSettingsRepository.kt:125` | `KEY_VOS_SFTP_HOST_KEY_MODE` has default value `"no"`. This disables SSH host key verification and exposes all VOS SFTP sync operations to man-in-the-middle attacks. This is the same vulnerability tracked in the module-level `VosSftpClient`. | Default to `"yes"` (strict) or `"ask"`. Document the "no" option clearly as a security-reducing developer override. Update `VosSftpClient` and `VosSyncManager` to enforce this setting. |
| Medium | `apps/avanues/.../ui/home/DashboardViewModel.kt:289–295` | `refreshModuleStates()` hardcodes `"engine" to "Android STT"` and `"language" to "en-US"` as VoiceAvanue metadata regardless of actual `DeveloperSettings` values. This causes stale/incorrect status display in the UI. | Read the actual `developerPreferences.speechEngine` and `voiceLanguage` flows and use those values in the metadata map. |
| Medium | `apps/avanues/.../ui/sync/VosSyncViewModel.kt:148,185,285` | `downloadAll()`, `fullSync()`, and `exportSuggestions()` all write to `Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)` directly via `File.writeText`. This is a scoped storage violation on Android 10+ (API 29+). Access to this path requires `MANAGE_EXTERNAL_STORAGE` or use of MediaStore / Storage Access Framework. | Use `MediaStore.Downloads` API for Android 10+ or request `ACTION_OPEN_DOCUMENT_TREE` via SAF. Fall back to app-internal storage in `filesDir` as a simpler alternative. |
| Medium | `apps/avanues/.../ui/developer/DeveloperConsoleScreen.kt:521` | SQL string built via `"SELECT * FROM \"$tableName\" LIMIT $limit"` without sanitization. A table name containing `"` or `;` (e.g., from a malicious DB file) could alter the query. | Use a whitelist of allowed table names derived from the schema, or validate the table name against the list from `sqlite_master` before interpolating it. Even in a developer console this is a bad pattern to allow. |
| Medium | `apps/avanues/.../service/CommandOverlayService.kt:154` | `AlarmManager.ELAPSED_REALTIME` used for the overlay restart alarm in `onTaskRemoved()`. On devices in doze/sleep mode, this alarm type does not wake the device. The overlay will not restart after task removal on a sleeping device. | Change to `AlarmManager.ELAPSED_REALTIME_WAKEUP` so the alarm fires even when the device is in doze mode. |
| Medium | `apps/avanues/.../ui/settings/UnifiedSettingsScreen.kt:109` | `sortedProviders.forEach { provider -> if (provider is SystemSettingsProvider) { provider.onNavigateToVosSync = onNavigateToVosSync } }` runs inside a Composable, mutating a Hilt singleton's `var` property on every recomposition. This is a side-effect in composition that violates Compose's idempotency contract. | Move this wiring to `LaunchedEffect(Unit)` or inject the navigation callback via the DI graph at construction time. |
| Medium | `apps/avanues/.../ui/settings/UnifiedSettingsScreen.kt:~180` | Developer easter egg tap counter (`devTapCount`) is declared with `remember { mutableIntStateOf(0) }` inside a `LazyColumn item {}` lambda. `LazyColumn` recycles items when they scroll off-screen, resetting this state to 0. A user who scrolls away mid-tap-sequence will restart the count. | Move `devTapCount` and `firstTapTime` to the ViewModel, or hoist the state above the `LazyColumn` so it persists across scroll. |
| Medium | `apps/avanues/.../service/ElementExtractor.kt:66` | `isParentClickable` is accepted as a function parameter and propagated recursively to children, but the constructed `ElementInfo` at line 66 always sets `isParentClickable = false` (hardcoded literal). The parameter is never stored in the element for the current depth. Downstream consumers reading `element.isParentClickable` will always see `false` at the direct extraction level. | Pass `isParentClickable` (the parameter) into the `ElementInfo` constructor, not the literal `false`. |
| Low | `apps/voiceavanue/.../service/VoiceAvanueAccessibilityService.kt:82` | `debugMode = true` hardcoded in the `ServiceConfiguration` for the `voiceavanue` standalone app. Debug mode is always active in this build variant, regardless of build type (debug/release). | Read from `BuildConfig.DEBUG` or from `DeveloperPreferencesRepository` so release builds do not expose debug output. |
| Low | `apps/avanues/.../ui/hub/SpatialOrbitHub.kt:~all interactive nodes` | All `OrbitNode` clickable elements (Compose `clickable` modifier) and `DockItem` clickable elements lack AVID voice semantics (`contentDescription` on the semantics block). This violates the Voice-First AVID zero-tolerance rule. The orbit hub is a primary navigation surface. | Add `Modifier.semantics { contentDescription = "Voice: open ${node.label}" }` (or similar) to each clickable element in `SpatialOrbitHub`. |
| Low | `apps/avanues/.../ui/home/SystemHealthSection.kt` | `PermissionActionCard` (an `AvanueCard` with `onClick`) and `PermissionGrantedRow` both lack AVID voice semantics. Users cannot voice-activate permission grant flows. | Add `Modifier.semantics { contentDescription = "Voice: grant ${title} permission" }` to each `PermissionActionCard`. Add `role = Role.Button` to the card modifier. |
| Low | `apps/avanues/.../ui/home/CommandsSection.kt` | `AvanueChip(onClick = {})` used for command phrase and synonym display chips with empty lambdas. No `contentDescription` or AVID semantics provided. | If chips are purely informational (not interactive), replace with `Text` to avoid creating false affordances. If interactive, add AVID semantics and implement meaningful `onClick` actions. |
| Low | `apps/avanues/.../ui/hub/SpatialOrbitHub.kt:~devMode` | Developer mode activated via long-press tap-sequence navigates to `DEVELOPER_SETTINGS`, but the settings easter egg in `AboutScreen`/`HubDashboardScreen` opens `DEVELOPER_CONSOLE`. The two easter eggs lead to different destinations — inconsistent UX. | Align both easter eggs to navigate to the same developer entry point. Recommend `DEVELOPER_CONSOLE` as the primary developer surface since it has more functionality. |
| Low | `apps/avanues/.../ui/home/HomeScreen.kt:~formatTimeAgo` | `formatTimeAgo()` calls `System.currentTimeMillis()` once at call time with no `remember {}` invalidation. The displayed relative time ("5 mins ago") will not update while the composable is on-screen. | Either call inside a `LaunchedEffect` that re-triggers on a ticker, or use `remember(currentTimeMillis()) { }` with a periodic state update. |
| Low | `apps/avanues/.../data/SftpCredentialStore.kt` | Falls back to plain `SharedPreferences` silently if `EncryptedSharedPreferences` initialization fails. Only a `Log.w` is emitted; callers have no way to detect that credentials are being stored unencrypted. | Expose an `isEncrypted: Boolean` property or throw a checked exception / return a `Result.failure`. The fallback to plaintext should never be silent for security-sensitive credential storage. |
| Low | `apps/avanues/.../ui/developer/DeveloperConsoleScreen.kt:~getMemoryInfo` | `getMemoryInfo()` is called inside an `item { val mem = viewModel.getMemoryInfo() ... }` block without `remember {}`. It will be called on every recomposition of that item. | Wrap in `remember { viewModel.getMemoryInfo() }` or better yet, expose as a `StateFlow` in the ViewModel and collect it with `collectAsState()`. |

---

## Detailed Findings

### HIGH-1: `runBlocking` in VoiceControlCallbacks (ANR/Deadlock Risk)

**File:** `apps/avanues/src/main/kotlin/com/augmentalis/voiceavanue/service/VoiceAvanueAccessibilityService.kt`
**Lines:** 275–317

```kotlin
// Current (DANGEROUS):
onMuteVoice = {
    runBlocking { core?.stopListening() }  // blocks calling thread
},
onWakeVoice = {
    runBlocking { core?.startListening() } // blocks calling thread
},
```

`VoiceControlCallbacks` is invoked from within `VoiceControlHandler` which runs as part of the
VoiceOSCore command processing pipeline. Depending on the dispatch context, the calling thread may be:

1. **Main thread** — `runBlocking` here blocks the UI, risks ANR after 5 seconds.
2. **A coroutine dispatcher thread** — `runBlocking` blocks the thread and steals it from the
   dispatcher pool. If the calling coroutine holds a lock (e.g., a `Mutex` inside VoiceOSCore),
   the new blocking coroutine waiting for that same lock will deadlock.

The fix is straightforward — launch a fire-and-forget coroutine from `serviceScope`:

```kotlin
// Correct:
onMuteVoice = {
    serviceScope.launch { core?.stopListening() }
},
onWakeVoice = {
    serviceScope.launch { core?.startListening() }
},
```

This is non-blocking and runs on `serviceScope`'s dispatcher (Main by default), which is safe for
Compose-based VoiceOSCore state mutations.

---

### HIGH-2: `processVoiceCommand("", 0f)` is a Silent No-Op

**File:** `apps/avanues/src/main/kotlin/com/augmentalis/voiceavanue/service/VoiceRecognitionService.kt`
**Line:** 59

```kotlin
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // "Wake up VoiceOSCore" — this does nothing
    accessibilityService?.processVoiceCommand("", 0f)
    ...
}
```

`processVoiceCommand(phrase, confidence)` routes a recognized speech phrase through the command
pipeline. An empty string with 0.0 confidence:
- Fails the minimum confidence threshold check (typically 0.5–0.7)
- Returns an empty phrase to all matchers
- Produces `HandlerResult.notHandled()` from every handler

VoiceOSCore is not "woken up" by this call. If the intent is to signal readiness or start listening,
the correct call is `core?.startListening()` on the `VoiceOSCore` instance held by
`VoiceAvanueAccessibilityService`. This entire method body needs to be corrected to use the
proper activation API.

---

### HIGH-3: SQLite Database I/O on Main Thread (DeveloperConsoleScreen)

**File:** `apps/avanues/src/main/kotlin/com/augmentalis/voiceavanue/ui/developer/DeveloperConsoleScreen.kt`
**Lines:** 491, 520

```kotlin
// Inside a @Composable item {} lambda — runs on the UI thread:
val tables = remember(selectedDatabase) { getTablesForDb(context, selectedDatabase) }
val rows   = remember(selectedDatabase, selectedTable, queryLimit) {
    getTableRows(context, selectedDatabase, selectedTable, queryLimit)
}
```

```kotlin
// getTablesForDb opens a real SQLiteDatabase:
private fun getTablesForDb(context: Context, dbName: String): List<String> {
    val db = SQLiteDatabase.openDatabase(...)  // blocking I/O
    val cursor = db.rawQuery("SELECT name FROM sqlite_master ...", null)
    ...
}
```

`remember {}` blocks execute synchronously during composition on the main thread. Calling blocking
SQLite I/O here will freeze the UI for the duration of the database open + query. On a device with
a large database or slow storage this can trigger ANR warnings.

The fix:
```kotlin
// In DeveloperConsoleViewModel:
private val _tables = MutableStateFlow<List<String>>(emptyList())
val tables: StateFlow<List<String>> = _tables.asStateFlow()

fun loadTables(dbName: String) {
    viewModelScope.launch(Dispatchers.IO) {
        _tables.value = getTablesForDb(context, dbName)
    }
}
```

---

### HIGH-4: Banned `MaterialTheme` Wrapper in voiceavanue + voiceavanue-legacy

**Files:**
- `apps/voiceavanue/src/main/kotlin/com/augmentalis/voiceavanue/ui/theme/Theme.kt`
- `apps/voiceavanue-legacy/src/main/kotlin/com/augmentalis/voiceavanue/ui/theme/Theme.kt`

Both files contain:
```kotlin
MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
)
```

This is explicitly banned by MANDATORY RULE #3. Any code downstream that calls
`MaterialTheme.colorScheme.*` (including Material3 components that access it implicitly) will
get the raw Material color scheme instead of AvanueTheme tokens.

Additionally, `voiceavanue/Theme.kt` calls:
```kotlin
@SuppressLint("NewApi")
window.statusBarColor = colorScheme.primary.toArgb()  // deprecated API
```

Correct replacement:
```kotlin
// In voiceavanue/Theme.kt — replace MaterialTheme wrapper with:
AvanueThemeProvider(
    colors = HydraColors.colors(isDark),
    glass = HydraColors.glass(isDark),
    water = HydraColors.water(isDark),
    materialMode = MaterialMode.Water,
    isDark = isDark
) {
    content()
}
```

Note: these are standalone app modules that may be slated for deprecation in favor of `apps/avanues/`.
If they are legacy-only, a migration note in code is sufficient; otherwise they must be fixed.

---

## Recommendations

1. **Fix `runBlocking` in VoiceControlCallbacks immediately** — this is the highest runtime risk.
   An ANR report from a user triggering mute/wake during a slow voice processing call would fail
   to reproduce in automated testing but will surface in production.

2. **Fix VoiceRecognitionService.onStartCommand** — remove the dead `processVoiceCommand("", 0f)`
   call and implement the correct VoiceOSCore activation flow.

3. **Migrate `apps/voiceavanue/Theme.kt` and `apps/voiceavanue-legacy/Theme.kt` to AvanueThemeProvider**
   if either module is actively used. If they are fully superseded by `apps/avanues/`, add a
   prominent deprecation comment and track removal.

4. **Fix scoped storage in VosSyncViewModel** — `Environment.getExternalStoragePublicDirectory()`
   is blocked on Android 10+ without `MANAGE_EXTERNAL_STORAGE`. The easiest migration is to
   use `context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)` which is app-scoped and
   requires no extra permissions on Android 10+.

5. **Add AVID semantics to SpatialOrbitHub, SystemHealthSection, and CommandsSection** — these are
   primary navigation and permission-grant surfaces. Voice-first is a zero-tolerance rule and
   all three screens are directly user-facing.

6. **Add an `isEncrypted` flag to SftpCredentialStore** — the silent fallback to plaintext
   SharedPreferences is a security gap. Callers must know when credentials are not encrypted.

7. **Align developer easter egg destinations** — both easter eggs should open the same developer
   screen. `DEVELOPER_CONSOLE` is recommended as it contains more diagnostic functionality than
   `DEVELOPER_SETTINGS`.

8. **Move `devTapCount` out of `LazyColumn item {}`** — the tap counter resets on scroll which
   makes the developer mode activation frustrating to trigger. Hoist to ViewModel state.

---

## Rule 7 Check

No Rule 7 violations found. No `Author: Claude`, `Author: AI`, `VOS4 Development Team`, or
similar AI attribution present in any of the 72 reviewed `.kt` files.

---

## Theme Compliance Summary

| Module | Status |
|--------|--------|
| `apps/avanues/` | COMPLIANT — uses `AvanueThemeProvider` + `AvanueTheme.colors.*` throughout |
| `apps/voiceavanue/Theme.kt` | VIOLATION — uses `MaterialTheme(colorScheme = ...)` |
| `apps/voiceavanue-legacy/Theme.kt` | VIOLATION — uses `MaterialTheme(colorScheme = ...)` |

`apps/avanues/` does import `MaterialTheme` in some files (`HomeScreen.kt`, `MainActivity.kt`)
but only for **typography tokens** (`MaterialTheme.typography.*`), not for color tokens. Typography
usage via `MaterialTheme.typography.*` is acceptable since AvanueTheme does not override the
Material typography composition local.

---

*Report generated: 260220 | Reviewed by: Code Reviewer Agent*
