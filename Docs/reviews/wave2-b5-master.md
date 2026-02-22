# Wave 2 Batch 5 — Master Analysis Entries
**Date:** 260222
**Modules:** PluginSystem, Cockpit
**Full review:** `docs/reviews/PluginSystem-Cockpit-Review-QualityAnalysis-260222-V1.md`

---

## PluginSystem

**Module path:** `Modules/PluginSystem/` | **Files:** 173 .kt | **Targets:** commonMain, androidMain, jvmMain, desktopMain, jsMain

### Architecture Overview
Plugin lifecycle manager for the VoiceOS/AVA plugin ecosystem. Implements an 8-step load lifecycle (manifest parse → validate → conflict check → namespace → dir structure → class load → register → return), dependency resolution with topological sort and DFS cycle detection, permission sandbox with audit logging, filesystem checkpoint/rollback transaction system, hot-reload state persistence, and composite parallel discovery from multiple sources.

### CRITICAL Stubs / Non-Functional

| Location | Issue |
|----------|-------|
| `core/PluginLoader.kt:229` + `platform/PluginClassLoader.kt:14-37` | `PluginClassLoader` uses Android `DexClassLoader` which requires compiled DEX bytecode. The module's stated plugin format is `.avp` text files. These are incompatible — `DexClassLoader` cannot load script/text files. This is a pre-production architectural blocker; the plugin loading subsystem cannot function until this mismatch is resolved (either by adding a `.avp`→DEX compile step or replacing DEX classloading with a script interpreter). |
| `security/PluginSandbox.kt:362` + `security/PermissionStorage.kt` | `DefaultPluginSandbox` holds all granted permissions in an in-memory `mutableMapOf`. The `PermissionStorage` expect/actual class (AES256-GCM encrypted persistence) is defined but never called from `DefaultPluginSandbox`. All plugin permissions are lost on every app restart — the permission system is effectively non-functional across sessions. |

### HIGH Bugs

| Location | Issue |
|----------|-------|
| `dependencies/DependencyResolver.kt:82-96` | `buildDependencyGraph()` throws `IllegalStateException` for version constraint violations instead of returning `ResolutionResult.Failure`. The exception propagates uncaught through `resolveDependencies()`, crashing the calling coroutine. The `ResolutionResult.Failure` type exists expressly to carry these errors. |
| `integration/PluginSystemSetup.android.kt:312` | `AndroidCommandDispatcher.dispatch()` creates `QuantizedCommand(actionType = CommandActionType.CLICK)` hardcoded for ALL commands. Non-click commands (scroll, navigation, text entry) are all dispatched as CLICK actions — silent misrouting for all non-click plugin interactions. |
| `core/PluginRegistry.kt:445,449` | `addToIndex()` calls `PluginSource.valueOf()` and `DeveloperVerificationLevel.valueOf()` on unvalidated string fields from the plugin manifest. Any plugin with an unrecognized `source` or `verificationLevel` value in its manifest throws `IllegalArgumentException` inside the mutex lock and crashes `register()`. |
| `androidMain/platform/PluginClassLoader.kt:14-37` | Optimized DEX output written to `pluginPath/../dex_opt/` — outside the plugin's sandbox directory. On Android 10+ this parent path may not be writable, and writing here bypasses sandbox isolation. Should use `context.codeCacheDir`. |

### MEDIUM Issues

| Location | Issue |
|----------|-------|
| `discovery/CompositePluginDiscovery.kt:82,237,240` | `lastDiscoveryResult` is an unprotected `var` written after mutex release — data race with concurrent readers. `discoveryListeners` is a plain `mutableListOf` iterated at L240 without mutex — `ConcurrentModificationException` risk if listeners are added/removed concurrently. |
| `hotreload/PluginStateStorage.kt:397-406,429-431` | `buildMetadataJson()` embeds `metadata.pluginId` without JSON escaping. A plugin ID containing `"` or `\` produces malformed metadata JSON. `extractJsonString()` regex `[^"]+` also fails on escaped quotes. Both functions should be replaced with `kotlinx.serialization`. |
| `transactions/TransactionManager.kt:169-171` | `MAX_BACKUP_SIZE_MB = 500L` limit is advisory only — oversized backups proceed with a warning log. No enforcement; could fill device storage. |
| `sdk/BasePlugin.kt:162` | `activate()` state guard logic is inverted/incomplete — prevents activation from `STOPPED` and `ERROR` states (the two most common re-activation sources) while the intent appears to be the opposite. `FAILED` state handling also unclear. |
| `androidMain/android/AndroidPluginHost.kt:123` | Default `scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)` — `Dispatchers.Main` unavailable in JVM tests and non-Android environments. Should default to `Dispatchers.Default`. |
| `security/PermissionEscalationDetector.kt:213` | `manifestPermissions: MutableMap` constructor param — caller retains mutable reference, can bypass synchronization. Should be defensively copied. |

### LOW Issues

| Location | Issue |
|----------|-------|
| `core/PluginLoader.kt:41` | KDoc usage example references `plugin.jar` — contradicts `.avp` plugin format. |
| `core/PluginManifest.kt` | `source: String` and `verificationLevel: String` should be typed enum fields. Invalid values only caught (with crash) at index time. |
| `transactions/TransactionManager.kt` | `cleanupStaleCheckpoints()` only removes in-memory entries; orphaned backup directories on disk are never cleaned up after a crash. |

### Correctly Implemented

- DFS cycle detection in `DependencyResolver` is algorithmically correct (recursion stack + visited set separation)
- `GrpcPluginEventBus` and `PluginRegistry` both use `kotlinx.coroutines.sync.Mutex` — correct for coroutine contexts
- `CompositePluginDiscovery` parallel discovery via `coroutineScope { async{}.awaitAll() }` — correct structured concurrency
- `AndroidPluginHost` Activity lifecycle callbacks (pause/resume scoped to ACTIVE/PAUSED states) — correct
- `PluginHandlerBridge` plugin-first routing with configurable confidence threshold and legacy fallback — solid design

---

## Cockpit

**Module path:** `Modules/Cockpit/` | **Files:** 47 .kt | **Targets:** commonMain, androidMain, desktopMain

### Architecture Overview
Multi-window session management hub ("where avenues meet"). KMP ViewModel (`CockpitViewModel`) with `SupervisorJob + Dispatchers.Default` scope manages sessions, frames, layout modes, and auto-save (500ms debounce). 17 `FrameContent` subtypes (Web, Pdf, Image, Video, Note, Camera, VoiceNote, Form, Signature, Voice, Map, Whiteboard, Terminal, AiSummary, ScreenCast, Widget, ExternalApp). 13 layout modes (FREEFORM, GRID, SPLIT_LEFT, SPLIT_RIGHT, COCKPIT, T_PANEL, MOSAIC, FULLSCREEN, WORKFLOW, ROW, CAROUSEL, SPATIAL_DICE, GALLERY). SQLDelight-backed repositories for Android and Desktop. `FrameWindow` implements macOS-style traffic light controls with hover/press icon reveal.

### HIGH Bugs

| Location | Issue |
|----------|-------|
| `androidMain/repository/AndroidCockpitRepository.kt` + `desktopMain/repository/DesktopCockpitRepository.kt` | Both implementations are ~360 lines of near-identical code — massive DRY violation. All logic (SQLDelight queries, `deserializeContent()`, `SessionExport`, `importSession()`, `exportSession()`, enum parsers) is duplicated. Any bug fix must be manually mirrored to both files. Extract `BaseCockpitRepository` to eliminate duplication. |
| `commonMain/model/CockpitSession.kt` + both repositories | `CockpitSession.workflowSteps: List<WorkflowStep>` is never populated in `getSessions()` or `getSession()`. Every session load returns `workflowSteps = emptyList()`. All workflow step data is silently lost after every app restart. |
| `desktopMain/repository/DesktopCockpitRepository.kt:304-313` (same in Android) | `importSession()` generates frame IDs with `"${now.toEpochMilliseconds()}_${(0..99999).random()}"` in a loop. All frames share the same `now` timestamp; the random range has only 100,000 values. Real collision probability for sessions with many frames. Use full 64-bit random. |
| `commonMain/viewmodel/CockpitViewModel.kt:60-61,139,172` | `nextZOrder: Int` plain `var` mutated from concurrent `scope.launch` blocks on `Dispatchers.Default`. Two concurrent `addFrame()` or `selectFrame()` calls can read the same value, producing duplicate Z-orders. `_frames.value = currentFrames + frame` (L145) uses a captured snapshot — concurrent `addFrame()` calls silently lose frames. Replace with `AtomicInteger` and serialize frame mutations. |

### MEDIUM Issues

| Location | Issue |
|----------|-------|
| `androidMain/repository/AndroidCockpitRepository.kt:321-323` | `importSession()` catches all exceptions and returns `null` with no logging. "Malformed JSON", "DB write failed", and "schema mismatch" all produce identical silent failures. Return `Result<CockpitSession>` with typed errors. |
| `commonMain/viewmodel/CockpitViewModel.kt:317-329` | `deleteSession()` calls `loadSession()` which internally does `scope.launch`. Nested launches from within a launch create implicit ordering dependencies. Refactor `loadSession` to have a `suspend` internal variant called directly. |

### LOW Issues

| Location | Issue |
|----------|-------|
| `commonMain/ui/FrameWindow.kt:333` | `TrafficDot` outer clickable `Box` (24dp touch target) has no `semantics { contentDescription; role = Role.Button }`. The `contentDescription` is passed to the inner decorative `Icon` only. Screen reader accessibility incomplete. |
| `commonMain/ui/LayoutEngine.kt` | `FullscreenLayout` may show a maximized frame that is not the selected frame, leaving the user's selection hidden with no indication. Sync selection to the visible frame on FULLSCREEN layout activation. |

### Correctly Implemented

- `CockpitViewModel` uses `SupervisorJob + Dispatchers.Default` — correct KMP-safe scope (avoids unavailable `Dispatchers.Main` on Desktop)
- Auto-save debounce (`autoSaveJob?.cancel()` + `delay(500ms)`) is correct and resets properly on every change
- `LayoutEngine` exhaustively handles all 13 layout modes — no missing branches
- `contentTypeIcon()` exhaustively covers all 17 `FrameContent` subtypes — no missing branches
- `deserializeContent()` fallback handles all 17 content types on JSON parse failure — no crashes on schema migration
- `FrameWindow` title bar uses `AvanueTheme.colors.*` throughout — fully compliant with theme rules v5.1
- `FrameContent` sealed class subtypes all `@Serializable` with `@SerialName` — correct for JSON persistence across sessions

---

## Totals

| Module | Critical | High | Medium | Low | Total |
|--------|----------|------|--------|-----|-------|
| PluginSystem | 2 | 4 | 5 | 3 | 14 |
| Cockpit | 0 | 4 | 4 | 2 | 10 |
| Cross-module | 0 | 0 | 1 | 0 | 1 |
| **Wave 2 B5 Total** | **2** | **8** | **10** | **5** | **25** |
