# Deep Review: android/apps/ — Legacy Android App Targets
**Date:** 260220
**Scope:** ALL .kt files in `android/apps/` (VoiceOS, webavanue, VoiceRecognition, VoiceUI, cockpit-mvp, VoiceOSIPCTest, webavanue-legacy, webavanue-ipc-legacy)
**Reviewer:** Code Reviewer Agent
**Criteria:** Bugs / null-safety / thread-safety / coroutine misuse / deprecated theme / Rule 7 violations

---

## Summary

The `android/apps/` directory contains 6 active app targets plus 2 legacy duplicate copies of WebAvanue. The active apps range from a test harness (VoiceOS) to a full UI framework library (VoiceUI). The most severe issues are: a fake database encryption migration that silently marks itself complete without performing any cryptographic operation; multiple thread-safety violations in the VoiceOS overlay service; a `runBlocking` call on companion object methods that blocks the main thread; and pervasive use of banned `MaterialTheme.colorScheme.*` instead of `AvanueTheme.colors.*` throughout the VoiceOS app and parts of VoiceUI. Rule 7 violations ("Author: VOS4 Development Team") appear in 15+ files across VoiceRecognition and VoiceUI modules. The VoiceUI module is a standalone framework library with its own MagicEngine, theme system, and component DSL — it predates the AvanueTheme system and does not use it at all.

---

## Issues Table

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| **Critical** | `webavanue/.../DatabaseMigrationHelper.kt:111-117` | `PRAGMA rekey` never executed — fake migration marks DB as encrypted without performing encryption | Implement actual SQLCipher `PRAGMA rekey` via direct DB connection or use `SQLiteDatabase.changePassword()` |
| **Critical** | `webavanue/.../WebAvanueApp.kt:107` | Sentry DSN is literal placeholder `"https://YOUR_PUBLIC_KEY@sentry.io/YOUR_PROJECT_ID"` — ships to production | Replace with real DSN from environment variable or `BuildConfig` field; fail-fast with clear error if unconfigured |
| **Critical** | `VoiceOS/.../VoiceOSAccessibilityService.kt:350,358` | `runBlocking` in companion object methods `getCachedScreenCount()` and `getCachedScreenCountForCurrentApp()` — blocks calling thread | Replace companion object with a suspend fun on the service instance, or expose `StateFlow<Int>` that callers collect |
| **Critical** | `VoiceOS/.../VoiceOSAccessibilityService.kt:1058-1071` | `serviceScope.launch { voiceOSCore?.dispose() }` dispatched AFTER `serviceScope.cancel()` — cleanup coroutine is orphaned and `voiceOSCore` is never disposed | Move `serviceScope.cancel()` to AFTER the launched block completes: use `runBlocking` with a separate non-cancelled scope, or `withContext(NonCancellable)` |
| **Critical** | `VoiceOS/.../OverlayStateManager.kt:239-240,254-258` | `avidToNumber` (LinkedHashMap) and `maxAssignedNumber` are mutable fields on a `singleton object`, accessed concurrently from `serviceScope` coroutines (Dispatchers.Default) and potentially the main thread — no synchronization | Wrap `avidToNumber`/`maxAssignedNumber` mutations in a coroutine `Mutex`, or switch to `ConcurrentHashMap` + `AtomicInteger` |
| **High** | `VoiceOS/.../MainActivity.kt` (15+ occurrences) | Pervasive use of `MaterialTheme.colorScheme.*` — banned per CLAUDE.md Rule 3 (AvanueTheme v5.1) | Migrate to `AvanueTheme.colors.*` and wrap in `AvanueThemeProvider` |
| **High** | `VoiceOS/.../OverlayService.kt` (hardcoded colors throughout) | Hardcoded `Color(0xFF1E1E2E)`, `Color(0xEE000000)`, etc. in overlay composables — bypasses design token system | Use `AvanueTheme.colors.surface`, `AvanueTheme.colors.background`, etc. |
| **High** | `webavanue/.../DatabaseMigrationHelper.kt:117` | `Thread.sleep(500)` inside `suspend fun` on `Dispatchers.IO` — blocks an IO thread for 500ms during "simulated" encryption | Remove (the fake sleep serves no purpose once real encryption is implemented) |
| **High** | `VoiceRecognition/.../SpeechViewModel.kt` | Four nested Mutex objects (`engineSwitchingMutex`, `initializationMutex`, `engineCleanupMutex`, `engineMutex`) acquired in multiple orderings across coroutines — classic deadlock risk pattern | Consolidate to a single `Mutex` or enforce a consistent lock ordering documented in code |
| **High** | `VoiceRecognition/.../SpeechViewModel.kt:307` | `handleInitializationFailure()` calls `initializeEngine()` recursively — guarded by `initializationAttempts < 5L`, but `initializationAttempts` is never reset after a successful init, making the retry permanently unavailable after 5 total failures in a session | Reset `initializationAttempts` to 0 on each successful `initializeEngine()` call |
| **High** | `VoiceRecognition/.../SpeechViewModel.kt:75` | `engineInitializationHistory` is a plain `mutableMapOf` accessed from multiple coroutines — not thread-safe | Use `ConcurrentHashMap` |
| **High** | `VoiceRecognition/.../VoiceRecognitionService.kt:55-56` | `currentEngine: Any?` loses type safety; `isCurrentlyRecognizing: Boolean` is not `@Volatile` — accessed from AIDL binder thread AND `serviceScope` coroutine — data race on `isCurrentlyRecognizing` | Use `@Volatile` on `isCurrentlyRecognizing`; introduce a typed sealed class instead of `Any?` for engine |
| **High** | `VoiceRecognition/.../VoiceRecognitionService.kt:152-157` | `serviceScope.launch { stopCurrentRecognition() }` is dispatched in `onDestroy()`, then `callbacks.kill()` completes synchronously — stop coroutine is immediately orphaned | Use `runBlocking` to synchronously stop recognition before `callbacks.kill()`, or use `NonCancellable` job |
| **High** | `VoiceUI/.../MagicEngine.kt:36-43` | `stateScope` (object-level) is cancelled by `dispose()` and never re-created — any subsequent `initialize()` call silently fails to start new coroutines | Recreate `stateScope` on `initialize()` using `CoroutineScope(SupervisorJob() + Dispatchers.Default)` |
| **High** | `VoiceUI/.../MagicEngine.kt:43` | `contextStack` is a plain `mutableListOf` on a `@Stable object` — written from `pushContext()`/`popContext()` while stateScope coroutines read it — data race | Protect with `Mutex` or switch to thread-safe `CopyOnWriteArrayList` |
| **High** | `VoiceUI/.../MagicWindowSystem.kt:554-556` | `getScreenBounds()` always returns hardcoded `Size(1920f, 1080f)` — snap-to-edge never works correctly on real devices | Inject actual screen size via `LocalConfiguration` or a `WindowMetrics` callback |
| **High** | `webavanue/.../WebAvanueApp.kt:L63+225` | `applicationScope` uses `Dispatchers.Main`; IPC receiver executes `actionMapper.executeAction()` on Main dispatcher — can block UI thread if action is slow | Move action execution to `Dispatchers.Default`; use `withContext(Dispatchers.Main)` only for UI updates |
| **High** | `webavanue/.../WebAvanueApp.kt` | `onTerminate()` is the only place `ipcReceiver` is unregistered — `onTerminate()` is never called in production on Android, only in emulator — broadcast receiver permanently leaked | Unregister `ipcReceiver` in `onLowMemory()` or via a lifecycle observer attached to the process |
| **High** | `VoiceOS/.../VoiceOSAccessibilityService.kt` (companion) | Companion object `instance` var written on service main thread, read from any thread — not `@Volatile` — stale reads possible | Add `@Volatile` annotation to `instance` |
| **High** | `VoiceUI/.../HUDSystem.kt:20` | `currentFPS` hardcoded to `60.0f` (never updated); `scope` created but never used; `shutdown()` does not cancel scope | Implement real FPS calculation or remove the stub. Cancel `scope` in `shutdown()` |
| **High** | `VoiceUI/.../MagicComponents.kt:86-88,354,361` | `MaterialTheme.colorScheme.primary`, `.onPrimary`, `.onSurfaceVariant` used in MagicComponents form widgets — banned tokens | Migrate to `AvanueTheme.colors.*` |
| **High** | `VoiceUI/.../MagicThemeCustomizer.kt:74,300-301,991` | `MaterialTheme.colorScheme.surface`, `.onSurfaceVariant`, `.outline` used in MagicThemeCustomizer — banned tokens | Migrate to `AvanueTheme.colors.*` |
| **High** | `VoiceRecognition/.../MainActivity.kt:35` | `MaterialTheme {}` wraps `VoiceRecognitionApp` directly — bypasses AvanueTheme system entirely | Wrap with `AvanueThemeProvider` |
| **Medium** | `webavanue/.../BrowserTopBar.kt:49,89` | `MaterialTheme.colorScheme.surface`, `.primary` — banned | Migrate to `AvanueTheme.colors.*` |
| **Medium** | `webavanue/.../Dialogs.kt:106,227` | `MaterialTheme.colorScheme.onSurfaceVariant` + `MaterialTheme.shapes.large` — banned | Migrate to `AvanueTheme.colors.*`; use AvanueTheme shape tokens |
| **Medium** | `VoiceOS/.../ElementExtractor.kt:183-214` | Gmail-specific hardcoded string detection (`"Unread,"`, `"Starred,"`) for top-level list item heuristic — fragile and non-reusable | Extract into a configurable `AppListItemStrategy` interface; provide Gmail implementation |
| **Medium** | `VoiceOS/.../ElementExtractor.kt:117-119` | `indexOfFirst` inside per-element loop — O(n²) duplicate detection for large screens | Use `HashSet<String>` for seen-hashes; O(n) overall |
| **Medium** | `VoiceOS/.../OverlayService.kt:141` | `AlarmManager.set()` used in `onTaskRemoved()` for service restart — on API 23+ `set()` is inexact (OS may delay indefinitely for power saving) | Use `setExactAndAllowWhileIdle()` for guaranteed restart, or reconsider auto-restart architecture |
| **Medium** | `VoiceOS/.../OverlayService.kt:1208,1226` | `Divider` composable (deprecated since Compose M3 1.1) used twice | Replace with `HorizontalDivider` |
| **Medium** | `VoiceUI/.../CPUStateManager.kt:38-40` | `stateCache` (ConcurrentHashMap) and `accessOrder` (LinkedHashMap) are separate data structures — cacheState() checks `stateCache.size` and `stateCache.containsKey()` non-atomically before evictLRU(); concurrent writers can exceed `maxCacheSize` by racing on the size check | Wrap the size-check + evict + put block in `mutex.withLock` |
| **Medium** | `VoiceUI/.../GPUStateManager.kt:220-228` | `gpuBlur()` Modifier extension claims GPU blur but only sets `clip = true` — no blur is applied; the comment admits "actual blur can be applied via Modifier.blur()" — misleading API | Either implement blur via `Modifier.blur()` or rename to `gpuClip()` |
| **Medium** | `VoiceUI/.../AvaMagicAVIDIntegration.kt:28` | `CoroutineScope(Dispatchers.Default)` — no `SupervisorJob` — one failed child cancels the entire scope | Add `SupervisorJob()`: `CoroutineScope(Dispatchers.Default + SupervisorJob())` |
| **Medium** | `VoiceUI/.../MagicWindowSystem.kt:336-355` | Minimize button uses `Icons.Default.Close` (X icon) — semantically incorrect for minimize | Use `Icons.Default.Remove` or `Icons.Default.Minimize` |
| **Medium** | `VoiceUI/.../MagicWindowSystem.kt:351-354` | Maximize/restore button renders same `Icons.Default.Settings` icon in both states — both branches of `if (windowState.isMaximized.value)` are identical | Use `Icons.Default.Fullscreen` for maximize and `Icons.Default.FullscreenExit` for restore |
| **Medium** | `VoiceUI/.../MagicWindowSystem.kt:448-453` | `MagicWindowContainer` renders all windows via `remember { MagicWindowManager.getAllWindows() }` — `remember` with no key never re-runs, so new windows created after first composition are invisible | Remove `remember` wrapper; read live state directly from `MagicWindowManager.getAllWindows()` (but this requires converting to Compose `State`) |
| **Medium** | `VoiceUI/.../MigrationEngine.kt:19` | `migrationHistory = mutableListOf<MigrationRecord>()` on a `singleton object` — not thread-safe; `applyMigration()` and `rollback()` both add/remove on `Dispatchers.IO` concurrently | Use `CopyOnWriteArrayList` or protect with `Mutex` |
| **Medium** | `VoiceRecognition/.../SpeechViewModel.kt:481` | `updateConfiguration()` triggers full engine re-initialization on every call — including trivially calling it with the same config | Add a config equality check; skip reinit if config unchanged |
| **Medium** | `VoiceRecognition/.../VoiceRecognitionService.kt:118-126` | `getAvailableEngines()` advertises 5 engines (ANDROID_STT, VOSK, VIVOKA, GOOGLE_CLOUD, WHISPER) — only VIVOKA is actually implemented; all others are commented out | Return only `listOf("vivoka")` until engines are implemented, or document disabled engines clearly |
| **Medium** | `VoiceRecognition/.../ConfigurationScreen.kt:353` | `SpeechMode.values()` — deprecated; `SpeechMode.entries` should be used in Kotlin 1.9+ | Replace with `SpeechMode.entries` |
| **Medium** | `VoiceOS/.../VoiceOSAccessibilityService.kt:1075` | `(speechDispatcher as? Closeable)?.close()` — fragile unchecked cast used for cleanup | Define `speechDispatcher` as `Closeable` in its type declaration, removing the need for a runtime cast |
| **Medium** | All apps | Zero AVID voice semantics on all interactive elements across VoiceOS, webavanue, VoiceRecognition, cockpit-mvp overlays, VoiceUI widget composables — violates CLAUDE.md AvanueUI Protocol (Voice-First, MANDATORY) | Add `Modifier.semantics { contentDescription = "Voice: click ..." }` to all interactive elements |
| **Medium** | `webavanue/.../MainActivity.kt:119-123` | `onSaveInstanceState()` passes empty `Bundle()` — navigation state lost on process death — documented as TODO | Investigate Voyager serialization issue; either fix or use Compose navigation with `rememberSaveable` |
| **Medium** | `webavanue/.../MainActivity.kt:38-41` | `cameraPermissionLauncher` result handler is a no-op ("placeholder — actual integration would notify XRManager") | Implement actual camera permission result handling or remove launcher until implemented |
| **Low** | Rule 7 — 15 files | `Author: VOS4 Development Team` in file headers — violates CLAUDE.md Rule 7 (no AI/team attribution, use "Manoj Jhawar") | Change to `Author: Manoj Jhawar` or remove the author line entirely |
| **Low** | `VoiceRecognition/.../ThemeUtils.kt` | Explicitly `@deprecated STUB FILE` — `glassMorphism()` modifier is non-functional (ignores all parameters, applies only a background tint) | Implement using `Modifier.blur()` + border + background, or delete the file and remove callers |
| **Low** | `VoiceRecognition/.../SpeechRecognitionScreen.kt:57` | `currentConfig` hardcoded with `language = "en-US"` — ignores ViewModel's actual config | Read language from ViewModel state |
| **Low** | `VoiceOS/.../OverlayService.kt:284` | Emoji `🎤` in notification title string — violates project rule (no emojis) | Remove emoji from notification title |
| **Low** | `VoiceUI/.../MagicEngine.kt:118` | `autoState()` uses `currentCompositionLocalContext.hashCode()` as part of state key — `CompositionLocalContext` hashCode is not stable across recompositions | Use a stable, explicit key derived from the call-site or a provided string key |
| **Low** | `VoiceUI/.../MagicEngine.kt:311-318` | `persistState()` `LOCAL` and `CLOUD` branches are empty stubs (`// TODO`) | Implement or annotate with `@NotImplemented` and throw to prevent silent state loss |
| **Low** | `android/apps/webavanue-legacy/`, `android/apps/webavanue-ipc-legacy/` | Two full duplicate copies of `webavanue` source tree — DRY violation, maintenance burden | Delete both legacy directories; they appear to be dead code from pre-IPC era |
| **Low** | `VoiceUI/...GPUCapabilities.kt`, `GPUBenchmark.kt` | Empty `typealias` shim files marked `@Deprecated` — kept for "backwards compatibility during migration" | Delete both files once migration is complete (VOSFIX-044) |
| **Low** | `VoiceOSIPCTest/.../MainActivity.kt:483-486` | `delay()` wrapper uses `Thread.sleep()` on `Dispatchers.IO` — blocks IO thread unnecessarily | Replace with `kotlinx.coroutines.delay()` (proper suspend delay) |
| **Low** | `cockpit-mvp/.../MainActivity.kt` | Uses `CockpitThemeProvider(theme = AppTheme.OCEAN)` — custom theme wrapper, not AvanueTheme v5.1 | Migrate to `AvanueThemeProvider` with `AvanueColorPalette.HYDRA` + `MaterialMode.Water` |

---

## Detailed Findings: Critical Issues

### CRIT-1: Fake Database Encryption Migration
**File:** `android/apps/webavanue/src/main/kotlin/com/augmentalis/webavanue/app/DatabaseMigrationHelper.kt:111-117`

```kotlin
// Step 5: SQLCipher in-place encryption
// Note: This would require direct SQL access to execute PRAGMA rekey
// For now, we'll use a simplified approach: just mark as encrypted
// and trust that the new driver will handle it
onProgress("Applying encryption...")
Thread.sleep(500) // Simulate encryption work
// markDatabaseAsEncrypted() is called BELOW — marks migration complete
```

The migration reports "encryption applied" to users and sets a persistent flag. The underlying database file is **never encrypted**. Any user who enables database encryption is falsely reassured while their data remains unprotected. Combined with `Thread.sleep(500)` inside a `suspend fun` (blocking the `Dispatchers.IO` thread), this is the most severe defect in the file.

**Fix:** Use SQLCipher's `SQLiteDatabase.changePassword(newKey)` API after opening the existing plain database with the SQLCipher driver in plain-text mode. Reference: SQLCipher docs "Encrypting an Existing Database".

---

### CRIT-2: Sentry DSN Placeholder Ships to Production
**File:** `android/apps/webavanue/src/main/kotlin/com/augmentalis/webavanue/app/WebAvanueApp.kt:107`

```kotlin
val sentryDsn = "https://YOUR_PUBLIC_KEY@sentry.io/YOUR_PROJECT_ID"
if (sentryDsn.contains("YOUR_")) {
    Logger.warn(TAG, "Sentry DSN not configured - crash reporting disabled")
}
```

The code silently disables crash reporting with a warning log only. If this app is installed by users, no crashes are ever reported. The check `sentryDsn.contains("YOUR_")` is a workaround that should not exist in a production app.

**Fix:** Move DSN to `local.properties` → `BuildConfig.SENTRY_DSN`. In CI, assert `BuildConfig.SENTRY_DSN.isNotEmpty()` at build time for release variants.

---

### CRIT-3: `runBlocking` in Companion Object / Accessibility Service Main Thread
**File:** `android/apps/VoiceOS/src/main/kotlin/com/augmentalis/voiceos/service/VoiceOSAccessibilityService.kt:350,358`

```kotlin
companion object {
    fun getCachedScreenCount(): Int {
        return runBlocking { instance?.screenHashRepository?.getScreenCount() ?: 0 }
    }
    fun getCachedScreenCountForCurrentApp(): Int {
        return runBlocking { instance?.screenHashRepository?.getScreenCountForCurrentApp() ?: 0 }
    }
}
```

`VoiceOSAccessibilityService.getCachedScreenCount()` is called from `MainActivity.kt` (the VoiceOS test UI) to display stats. The main thread is blocked while the underlying SQLDelight query executes on the repository's dispatcher. If the database is locked (e.g., another coroutine is writing), this will ANR.

**Fix:** Convert to `suspend fun` and call from a `LaunchedEffect` in the composable, or expose a `StateFlow<Int>` from the service.

---

### CRIT-4: Cleanup Coroutine Orphaned in `onDestroy()`
**File:** `android/apps/VoiceOS/src/main/kotlin/com/augmentalis/voiceos/service/VoiceOSAccessibilityService.kt:1058-1071`

```kotlin
override fun onDestroy() {
    super.onDestroy()
    // ...
    serviceScope.launch {           // L1058 — coroutine dispatched
        try {
            voiceOSCore?.dispose()
        } catch (e: Exception) { ... }
    }
    // ...
    voiceOSCore = null              // L1068 — instance nulled before dispose completes
    serviceScope.cancel()           // L1071 — scope cancelled; launch above is orphaned
}
```

The `serviceScope.launch` at L1058 is cancelled before it executes because `serviceScope.cancel()` at L1071 runs synchronously on the same thread. `voiceOSCore?.dispose()` is never called, leaking any resources held by `voiceOSCore`.

**Fix:**
```kotlin
override fun onDestroy() {
    super.onDestroy()
    runBlocking(NonCancellable) {
        try { voiceOSCore?.dispose() } catch (e: Exception) { Log.e(TAG, "dispose error", e) }
    }
    voiceOSCore = null
    serviceScope.cancel()
}
```

---

### CRIT-5: Race Condition on `OverlayStateManager.avidToNumber`
**File:** `android/apps/VoiceOS/src/main/kotlin/com/augmentalis/voiceos/service/OverlayStateManager.kt:239-240`

```kotlin
private val avidToNumber = linkedMapOf<String, Int>()  // NOT thread-safe
private var maxAssignedNumber = 0                        // NOT volatile / NOT atomic
```

`updateNumberedOverlayItemsIncremental()` is called from the accessibility service's coroutine scope (which uses `Dispatchers.Default`). `clearOverlayItems()` is called from UI interactions (main thread). The `LinkedHashMap` and `maxAssignedNumber` are accessed concurrently with no synchronization, leading to potential `ConcurrentModificationException` and incorrect numbering.

**Fix:** Add a `private val mutex = Mutex()` to `OverlayStateManager` and wrap both `updateNumberedOverlayItemsIncremental` and `clearOverlayItems` in `mutex.withLock { }`. Change `maxAssignedNumber` to `AtomicInteger`.

---

## Detailed Findings: High Issues

### HIGH-1: SpeechViewModel Deadlock Risk (Four Nested Mutexes)
**File:** `android/apps/VoiceRecognition/src/main/java/com/augmentalis/voicerecognition/viewmodel/SpeechViewModel.kt`

The ViewModel declares four coroutine `Mutex` objects used in overlapping call chains:
- `engineSwitchingMutex` — outermost, held during `startRecognition()`
- `initializationMutex` — acquired inside `initializeEngine()` which is called while `engineSwitchingMutex` is held
- `engineCleanupMutex` — acquired in `cleanupPreviousEngine()` which is called while both above are held
- `engineMutex` — acquired in `stopListening()` which is called from `cleanupPreviousEngine()`

Lock ordering: `engineSwitching → initialization → engineCleanup → engine`

If any code path acquires these in a different order (e.g., `engine → engineSwitching`), deadlock will occur. Coroutine mutexes are not reentrant, so even a single recursive path deadlocks.

**Fix:** Consolidate to a single `Mutex` guarding all engine state. If concurrency is needed, use a `Channel`-based actor pattern instead.

---

### HIGH-2: MagicEngine `stateScope` Not Restartable After `dispose()`
**File:** `android/apps/VoiceUI/src/main/java/com/augmentalis/voiceui/core/MagicEngine.kt`

```kotlin
object MagicEngine {
    private val stateScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun dispose() {
        stateScope.cancel()   // scope is dead; no way to restart it on an object
    }

    fun initialize() {
        // stateScope.launch { ... } — silently fails after dispose()
    }
}
```

As a Kotlin `object`, `MagicEngine` has a single instance for the process lifetime. After calling `dispose()`, all subsequent `stateScope.launch` calls are no-ops (a cancelled scope ignores new coroutines). Any re-initialization of the engine after activity recreation will silently not work.

**Fix:** Replace `val stateScope` with `var stateScope` and recreate it in `initialize()`:
```kotlin
private var stateScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

fun initialize() {
    if (stateScope.coroutineContext[Job]?.isActive != true) {
        stateScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    }
    // ...
}
```

---

### HIGH-3: WebAvanue IPC Receiver Leak
**File:** `android/apps/webavanue/src/main/kotlin/com/augmentalis/webavanue/app/WebAvanueApp.kt`

`ipcReceiver` (a `BroadcastReceiver`) is registered in `onCreate()` but only unregistered in `onTerminate()`. The Android documentation states: "This method is for use in emulated process environments. It will never be called on a production Android device." In production, the receiver is permanently registered for the lifetime of the app process, accumulating for every app restart if the process is not killed.

Additionally, the receiver's callback executes `actionMapper.executeAction()` on `Dispatchers.Main` — any slow or blocking action will produce visible UI jank or ANR.

**Fix:** Use `ProcessLifecycleOwner.get().lifecycle.addObserver(...)` to unregister on `ON_DESTROY`, or use a `LifecycleEventObserver`. Move `actionMapper.executeAction()` to `Dispatchers.Default`.

---

### HIGH-4: `MagicWindowContainer` New Windows Not Visible
**File:** `android/apps/VoiceUI/src/main/java/com/augmentalis/voiceui/windows/MagicWindowSystem.kt:448-453`

```kotlin
@Composable
fun MagicWindowContainer(
    ...
) {
    Box(modifier = modifier.fillMaxSize()) {
        content()

        val windows = remember { MagicWindowManager.getAllWindows() }  // BUG: never re-runs
        windows.forEach { windowState ->
            key(windowState.id) {
                // Windows rendered here — but the list is frozen at first composition
            }
        }
    }
}
```

`remember { MagicWindowManager.getAllWindows() }` without a key captures the window list exactly once at composition time. Any window created after the `MagicWindowContainer` is first composed will never appear in this iteration, making `MagicWindowManager.createWindow()` silently ineffective for callers using `MagicWindowContainer`.

**Fix:** Expose `windows` as `mutableStateMapOf<String, MagicWindowState>` inside `MagicWindowManager` and read it directly (Compose will track the state reads and recompose automatically).

---

## Rule 7 Violations — "Author: VOS4 Development Team"

The following files all contain `Author: VOS4 Development Team` in their file headers, violating CLAUDE.md Rule 7. Change to `Author: Manoj Jhawar` or omit the author line entirely.

**VoiceRecognition app (11 files):**
- `android/apps/VoiceRecognition/src/main/java/.../service/VoiceRecognitionService.kt` L5
- `android/apps/VoiceRecognition/src/main/java/.../service/ClientConnection.kt` L5
- `android/apps/VoiceRecognition/src/main/java/.../viewmodel/SpeechViewModel.kt` L5
- `android/apps/VoiceRecognition/src/main/java/.../ui/ThemeUtils.kt` L6
- `android/apps/VoiceRecognition/src/main/java/.../ui/SpeechRecognitionScreen.kt` L5
- `android/apps/VoiceRecognition/src/main/java/.../ui/ConfigurationScreen.kt` L5
- `android/apps/VoiceRecognition/src/main/java/.../MainActivity.kt` L5
- `android/apps/VoiceRecognition/src/test/.../service/VoiceRecognitionServiceTest.kt` L5
- `android/apps/VoiceRecognition/src/androidTest/.../service/ServiceBindingTest.kt` L5
- `android/apps/VoiceRecognition/src/androidTest/.../integration/AidlCommunicationTest.kt` L5
- `android/apps/VoiceRecognition/src/androidTest/.../mocks/MockRecognitionCallback.kt` L5

**VoiceUI module (2 files):**
- `android/apps/VoiceUI/src/main/java/.../core/GPUStateManager.kt` L5
- `android/apps/VoiceUI/src/main/java/.../core/CPUStateManager.kt` L5

**Documentation files (referenced, not .kt):**
- `android/apps/VoiceRecognition/README.md` L4
- `android/apps/VoiceRecognition/CHANGELOG.md` L191

---

## Deprecated Theme Usage — `MaterialTheme.colorScheme.*` (Banned per CLAUDE.md Rule 3)

The following files use `MaterialTheme.colorScheme.*` in non-theme-definition contexts. These are violations. Files that **define** a theme by wrapping `MaterialTheme(colorScheme = ...)` (e.g., `GreyARTheme.kt`, `MagicDreamTheme.kt`) are exempt as they are the theme definition layer.

**AvanueTheme migration required:**
- `android/apps/VoiceOS/src/main/kotlin/.../MainActivity.kt` — 15+ occurrences
- `android/apps/webavanue/src/main/kotlin/.../presentation/BrowserTopBar.kt:49,89`
- `android/apps/webavanue/src/main/kotlin/.../presentation/Dialogs.kt:106,227`
- `android/apps/VoiceUI/src/main/java/.../api/MagicComponents.kt:65,86-88,354,361`
- `android/apps/VoiceUI/src/main/java/.../theme/MagicThemeCustomizer.kt:74,300,301,991`
- `android/apps/VoiceRecognition/src/main/java/.../MainActivity.kt:35` (`MaterialTheme {}` wrapper)

**Note on VoiceUI module:** VoiceUI has its own `MagicDreamTheme` / `GreyARTheme` / `MagicThemeData` system that predates AvanueTheme v5.1. The entire VoiceUI module needs a migration plan to use `AvanueTheme.colors.*` as its token source while maintaining backward compatibility with `MagicThemeData` where needed.

---

## Additional Observations

### Triplicated WebAvanue Source
`android/apps/webavanue/`, `android/apps/webavanue-legacy/`, and `android/apps/webavanue-ipc-legacy/` appear to be three versions of the same browser app. The `-legacy` and `-ipc-legacy` variants are dead code. They should be deleted to eliminate the confusion and maintenance burden.

### VoiceRecognition Module Architecture
`ClientConnection.kt` defines a data class that is entirely unused — the service uses `RemoteCallbackList<IRecognitionCallback>` (AIDL-native callback list) instead of manual connection tracking. `ClientConnection` can be deleted.

### VoiceOS MainActivity — Completeness
`VoiceOS/MainActivity.kt` contains a `TODO` comment at L178: `// TODO: Handle command tap action`. The command list in the debug UI is visually tappable but nothing happens on tap. This is acceptable for a test harness but should be tracked.

### AvaMagicAVIDIntegration — Missing SupervisorJob
`AvaMagicAVIDIntegration.kt` uses `CoroutineScope(Dispatchers.Default)` without `SupervisorJob`. Any exception in `processVoiceCommand` or `executeVoiceCommand` will cancel the entire scope, permanently disabling AVID command processing for the session.

### VoiceOSIPCTest — `delay()` Implementation
The test activity's `delay()` helper uses `Thread.sleep(millis)` wrapped in `withContext(Dispatchers.IO)`. This is correct in that it doesn't block the Main dispatcher, but it does block an IO thread for 300-2000ms during the test suite. Replace with `kotlinx.coroutines.delay(millis)` which yields the coroutine instead of blocking a thread.

---

## Recommendations

1. **Fix CRIT-1 immediately** (fake encryption): The `DatabaseMigrationHelper` silently tells users their data is encrypted when it is not. This is the highest-priority fix.

2. **Fix CRIT-2** (Sentry DSN): Move to `BuildConfig` injection via `local.properties` and assert non-empty in release builds.

3. **Fix CRIT-3** (runBlocking): Expose screen count as `StateFlow<Int>` from `VoiceOSAccessibilityService`, collected in `LaunchedEffect` in `MainActivity.kt`.

4. **Fix CRIT-4+5** (onDestroy + OverlayStateManager race): Add `NonCancellable` block for cleanup; add `Mutex` to `OverlayStateManager`.

5. **Rule 7**: One global search-and-replace across all 13 affected `.kt` files changes `Author: VOS4 Development Team` to `Author: Manoj Jhawar`. Takes under 10 minutes.

6. **Theme migration plan**: VoiceOS/MainActivity.kt (15+ violations) and VoiceUI module need a dedicated migration sprint to AvanueTheme v5.1. VoiceUI's `MagicThemeData` can be kept as an internal representation but colors must source from `AvanueTheme.colors.*`.

7. **Delete dead code**: Remove `webavanue-legacy/`, `webavanue-ipc-legacy/`, `ClientConnection.kt` (VoiceRecognition), `GPUCapabilities.kt` shim, `GPUBenchmark.kt` shim.

8. **SpeechViewModel mutex redesign**: Replace 4-mutex nesting with single-mutex + state machine to eliminate deadlock risk.

9. **AVID audit**: Zero interactive elements have voice semantics across all 6 apps. A systematic AVID pass is needed before any app ships.
