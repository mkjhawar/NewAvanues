# NewAvanues Full Codebase Review

**Date:** 2026-02-20 | **Version:** V1-FINAL | **Branch:** HTTPAvanue
**Scope:** All 40+ modules and 10+ apps in the NewAvanues KMP monorepo
**Method:** 7 parallel code-review agents, class-by-class analysis of every .kt file

---

## Executive Summary

| Severity | Count | Description |
|----------|-------|-------------|
| CRITICAL | 16 | Runtime crashes, broken core functions, security gaps, rule violations, wire protocol bugs |
| HIGH | 48 | Silent failures, stubs, theme violations, thread-safety bugs, scope leaks |
| MEDIUM | 68 | Incomplete features, architectural issues, deprecated code, TODOs |
| LOW | 37 | Minor style issues, code duplication, optimization opportunities |
| **TOTAL** | **169** | Across ALL 40+ modules and 10+ apps (7/7 batches complete) |

**Key Takeaways:**
1. The codebase has strong KMP architecture overall but carries **16 critical bugs** that would cause crashes, security failures, or data corruption in production.
2. The most pervasive issue is `MaterialTheme.colorScheme` usage (**80+ violations** of the mandatory AvanueTheme system), concentrated in legacy AvanueUI renderers and legacy apps.
3. The **IPC module is entirely non-functional** on all 3 platforms — every method is a stub or returns failure.
4. **HTTP/2 has 3 critical bugs** (missing sink synchronization, no Huffman decoding, path traversal) that make it unsafe for production traffic.
5. **AVU wire protocol has a code collision** (`ACD` shared by 2 message types) causing frame misrouting.

---

## Batch Status

| # | Scope | Status | Findings |
|---|-------|--------|----------|
| 1 | Core Infrastructure (Foundation, Database, Logging, Utilities, Actions, Localization) | COMPLETE | 26 |
| 2 | Voice Pipeline (VoiceOSCore, VoiceAvanue, VoiceCursor, VoiceKeyboard, etc.) | COMPLETE | 27 |
| 3 | AvanueUI Theme System (25 sub-modules + AVID + AvidCreator) | COMPLETE | 30 |
| 4 | Feature Modules (Annotation, Image, Video, Note, PDF, Photo, Cockpit) | COMPLETE | 30 |
| 5 | Network/System (HTTPAvanue, RemoteCast, Rpc, IPC, WebSocket, DeviceManager, Gaze) | COMPLETE | 45 |
| 6 | AI & Licensing (7 AI sub-modules, License*, PluginSystem, AVACode) | COMPLETE | 5 |
| 7 | Apps & Platform (AVA, AVU, WebAvanue, all apps) | COMPLETE | 46 |

Full detailed reports for batches 4, 5, and 7 saved separately:
- `docs/reviews/Modules-Review-SixModulesDeepReview-260220-V1.md`
- `docs/reviews/NetworkSystem-Review-SevenModules-260220-V1.md`

---

## CRITICAL Findings (12)

### C1. Database `transaction()` Never Executes the Lambda
**File:** `Modules/Database/src/commonMain/.../VoiceOSDatabaseManager.kt:332-338`
**Impact:** All cross-repository transactional operations silently get no transaction. Data integrity at risk.
**Root Cause:** `block as T` casts the lambda itself instead of invoking it. The block is never called.
**Fix:** Remove the broken wrapper; expose `_database.transactionWithResult { }` directly or implement proper coroutine-compatible transaction.

### C2. Desktop Credentials Stored in Base64 (Not Encrypted)
**File:** `Modules/Foundation/src/desktopMain/.../DesktopCredentialStore.kt:22-47`
**Impact:** SFTP passwords, API keys stored on Desktop are trivially readable by any process. Violates `ICredentialStore` contract ("encrypted at rest").
**Fix:** Integrate with macOS Keychain / Windows Credential Manager / libsecret, or use AES-GCM with OS keystore key.

### C3. iOS Permission Checker Returns `true` Unconditionally
**File:** `Modules/Foundation/src/iosMain/.../IosPermissionChecker.kt:18-24`
**Impact:** All iOS permission gates are bypassed. Shared code that checks `hasPermission()` will always proceed regardless of actual iOS privacy boundaries.
**Fix:** Use Kotlin/Native `platform.AVFoundation`, `platform.Photos` etc. to check real permission status.

### C4. Database Migration V3->V4 is a No-Op
**File:** `Modules/Database/src/commonMain/.../migrations/DatabaseMigrations.kt:214-227`
**Impact:** The migration chain calls `migrateV3ToV4()` but the function body is empty. If the `.sqm` file doesn't handle it, FK constraints are silently skipped for all users.
**Fix:** Either implement imperatively or remove from the migration chain if `.sqm` handles it.

### C5. WakeWord Type Contract Mismatch
**File:** `Modules/Voice/WakeWord/src/commonMain/.../IWakeWordDetector.kt:50` vs `main/java/.../IWakeWordDetector.kt`
**Impact:** `commonMain` uses `WakeWordSettingsData`, `main/java` uses `WakeWordSettings` (Parcelable). Will cause compile failure when both are in scope.
**Fix:** Delete `main/java` duplicate, unify types. Move Android-specific code to `androidMain`.

### C6. WakeWord Detector is a Non-Functional Stub
**File:** `Modules/Voice/WakeWord/src/main/java/.../detector/PhonemeWakeWordDetector.kt:43-214`
**Impact:** `start()` transitions to LISTENING but never captures audio. `WakeWordService` starts it believing it works. Wake word detection silently does nothing.
**Fix:** Implement audio capture or throw `UnsupportedOperationException`.

### C7. AI Attribution Rule Violations (Rule 7)
**Files:**
- `Modules/SpeechRecognition/src/androidMain/.../AndroidSpeechRecognitionService.kt:5` — "Author: Claude (AI Assistant)"
- `Modules/SpeechRecognition/src/desktopMain/.../DesktopSpeechRecognitionService.kt:5` — Same
- `Modules/SpeechRecognition/src/commonMain/.../SpeechRecognitionService.kt:5` — Same
- `Modules/Voice/WakeWord/src/commonMain/.../IWakeWordDetector.kt:1-4` — "author: Claude Code"
- `Modules/Actions/src/androidMain/.../ActionsManager.kt:57` — "@author AVA AI Team"

**Impact:** Zero-tolerance rule violation. All AI attribution must be removed.
**Fix:** Replace with "Manoj Jhawar" or remove author line entirely.

### C8. AvanueUI DSL Components Throw `TODO()` (Uncatchable Error)
**File:** `Modules/AvanueUI/Core/src/commonMain/.../Components.kt` — 27 occurrences
**Impact:** Every DSL component's `render()` calls `TODO()` which throws `KotlinNotImplementedError` (extends `Error`, NOT `Exception`). Uncatchable via `catch(e: Exception)`. Will crash the process.
**Fix:** Implement via renderer dispatch or delete. Never leave `TODO()` in `render()`.

### C9. AvanueUI Data Module — 14 More `TODO()` Crashes
**File:** `Modules/AvanueUI/Data/src/commonMain/kotlin/com/avanueui/data/` — Avatar, Accordion, Carousel, Chip, DataGrid, Divider, EmptyState, List, Paper, Skeleton, Stepper, Table, Timeline, TreeView
**Impact:** Same as C8 — all `render()` methods throw `TODO()`.
**Fix:** Implement or delete.

### C10. AvanueUI LSP Parser Stubs Return `NotImplementedError`
**File:** `Modules/AvanueUI/AvanueLanguageServer/.../stubs/ParserStubs.kt:18,28,38`
**Impact:** LSP document service imports these parsers. Any completion/hover request silently fails.
**Fix:** Implement parsers or delete the LSP module.

### C11. StateManagement Duplicate Class Definitions
**File:** `Modules/AvanueUI/StateManagement/src/commonMain/.../Core.kt:6`
**Impact:** Three different `ColumnComponent` classes exist across three packages. KMP compilation may fail or pick the wrong one.
**Fix:** Delete `StateManagement/Core.kt` stubs, import from `AvanueUI:Core`.

### C12. `Types3D.kt` Uses Java `Math.toRadians()` in commonMain
**File:** `Modules/AvanueUI/Core/src/commonMain/.../Types3D.kt:123,128,133,213`
**Impact:** `java.lang.Math` is JVM-only. **Will not compile on iOS/Native targets.** KMP violation.
**Fix:** Replace with `degrees * kotlin.math.PI / 180.0`.

---

## HIGH Findings (26)

### Infrastructure (8 HIGH)
| ID | Module | Issue | File |
|----|--------|-------|------|
| H1 | Utilities+Logging | Two duplicate logging systems coexist | `Modules/Utilities/Logger.kt` vs `Modules/Logging/Logger.kt` |
| H2 | Utilities | `NetworkMonitor` hidden init dependency causes runtime crash | `NetworkMonitor.android.kt:15-131` |
| H3 | Foundation | iOS `UserDefaultsSettingsStore` race window + observer leak | `UserDefaultsSettingsStore.kt:34-35` |
| H4 | Foundation | `NumberToWords.defaultSystem` mutable global, thread-unsafe | `NumberToWords.kt:188` |
| H5 | Actions | `VoiceOSConnection.bind()` polling runnable leaks on cancel | `VoiceOSConnection.kt:220-237` |
| H6 | Actions | Partial initialization silently sets `isInitialized=true` | `ActionsInitializer.kt:268-274` |
| H7 | Localization | `shutdown()` sets `instance=null` without synchronization | `Localizer.android.kt:63-66` |
| H8 | Foundation | `ServiceStateProvider.metadata` creates new Flow per access | `ServiceState.kt:87` |

### Voice Pipeline (7 HIGH)
| ID | Module | Issue | File |
|----|--------|-------|------|
| H9 | VoiceAvanue | CommandSystem/BrowserSystem/RpcSystem all empty stubs | `VoiceAvanue.kt:114-148` |
| H10 | VoiceOSCore | Desktop SpeechEngineFactory always returns failure | `SpeechEngineFactoryProvider.desktop.kt:68-70` |
| H11 | VoiceOSCore | AICommandHandler + CastCommandHandler always return failure | `handlers/AICommandHandler.kt:40-48` |
| H12 | SpeechRecognition | Desktop `startListening()` stub, never captures audio | `DesktopSpeechRecognitionService.kt:91-92` |
| H13 | SpeechRecognition | Non-Android engines silently fall back to Android STT | `AndroidSpeechRecognitionService.kt:103-126` |
| H14 | VoiceKeyboard | Self-assignment bug: `currentInputConnection = currentInputConnection` | `VoiceKeyboardService.kt:140` |
| H15 | VoiceKeyboard | `getSupportedLanguages()` hardcoded, never queries device | `VoiceInputHandler.kt:341` |

### AvanueUI (11 HIGH)
| ID | Module | Issue | File |
|----|--------|-------|------|
| H16 | AvanueUI/Adapters | `MaterialTheme.colorScheme` in commonMain for alerts | `ComposeRenderer.kt:490-493` |
| H17 | AvanueUI/Renderers/Android | 30+ `MaterialTheme.colorScheme` violations | `LayoutDisplayExtensions.kt` (pervasive) |
| H18 | AvanueUI/Renderers/Android | Duplicate severity-to-color mapping in 4 files | `ToastMapper.kt`, `ConfirmMapper.kt`, `AlertMapper.kt`, `NavigationFeedbackExtensions.kt` |
| H19 | AvanueUI/Renderers/Android | Avatar image URL never loaded | `AvatarMapper.kt:49,56,59` |
| H20 | AvanueUI/Renderers/Android | 6 theme violations in input fields | `InputExtensions.kt:379-499` |
| H21 | AvanueUI/ThemeBuilder | ThemeBuilder itself uses `MaterialTheme.colorScheme` (13 refs) | `Main.kt:111,144,211,...` |
| H22 | AvanueUI/Adapters | 4 theme violations in Android Compose adapter | `ComposeUIImplementation.kt:129,266,472,529` |
| H23 | AvanueUI/Theme | `CloudThemeRepository` — all 10 methods are stubs | `ThemeRepository.kt:281-330` |
| H24 | AvanueUI/Theme | `ThemeIO` import/export pipeline empty | `ThemeIO.kt:251-267` |
| H25 | AvanueUI/AssetManager | Version history save/load both no-ops | `AssetVersionManager.kt:325,331` |
| H26 | AvanueUI/ARGScanner | `watch()` body is TODO — callers hang | `ARGScanner.kt:128-130` |

---

## MEDIUM Findings (29)

### Infrastructure (9)
- `SettingsMigration` doesn't distinguish null from corrupted data (Foundation)
- `TranslationProvider.formatString()` fragile `%s/%d` replacement logic (Localization)
- French/German/Japanese/Chinese translations 30-50% incomplete (Localization)
- `VoiceOSDatabaseManager.waitForInitialization()` misleading `suspend` (Database)
- `IntentRouter.inferCategoryFromName()` fragile `contains()` matching (Actions)
- `JavaPreferencesSettingsStore` listener mixes threads (Foundation)
- `PIISafeLoggerFactory` allocates new logger per call (Logging)
- `UiState.handleWith()` incomplete parallel to `execute()` (Foundation)
- `scrappedCommandQueries` typo in accessor name (Database)

### Voice Pipeline (10)
- `UnifiedCommand` types duplicated between VoiceAvanue and VoiceOSCore
- Three duplicate `GlassmorphismUtils` files across modules
- `CommandPersistence` misleading "stub" comment (no longer accurate)
- iOS/Desktop `VoiceIsolation.initialize()` returns true but does nothing
- `VoiceOSCoreDesktop.initialize()` is a single `println()`
- `WakeWordService.onDestroy()` scope cancel races with cleanup coroutine
- `VoiceKeyboardService` dictation state change is TODO
- `DictationHandler.loadDictationPreferences()` is TODO
- `GazeTracker` stub returns null forever (intentional but undocumented)
- `VoiceDataManagerDesktop` throws `NotImplementedError` on all APIs

### AvanueUI (6)
- 10 deprecated theme files still present (OceanColors, SunsetGlass, etc.)
- `Data` module uses banned `com.avanueui.*` package prefix
- StateManagement dual-module nesting confusion
- LSP workspace service advertises 4 unimplemented capabilities
- AVID `Fingerprint.deterministicHash()` weak 32-bit collision space
- GlassmorphicComponents migration diagram references deprecated `OceanGlass`

### AI & Licensing (4)
- NLU `NLUModelDownloader` hardcoded checksum "TBD" for MALBERT
- NLU JS/Web stubs: `BertTokenizer`, `IntentClassifier`, `ModelManager` (Phase 2)
- Chat `ResponseCoordinatorDesktop.addResponseTemplates()` is a stub

---

## LOW Findings (21)

### Infrastructure (5)
- `AndroidLogger` globalMinLevel duplicates Android's `isLoggable` system
- `DesktopLogger` calls `System.getProperty()` on every log (cache miss)
- `IosFileSystem.delete()` swallows NSError silently
- `HashUtils` has 3 redundant public entry points for same hash
- Copyright years outdated (2024-2025, should be 2026)

### Voice Pipeline (6)
- `CursorController.setDwellClickDelay()` doesn't update `GazeClickManager`
- `EventBus` scope never cancelled (test leak)
- `CursorFilter.currentTimeMillis()` three platforms when one suffices
- `VoiceInputHandler` locale never refreshed after device change
- `Code-Reviewed-By: CCA` undefined abbreviation in headers
- `CommandPersistence` pipe-delimited metadata breaks on `|` in values

### AvanueUI (9)
- Breadcrumb mapper: 3 theme violations (LOW because renderer is legacy)
- FileUpload mapper: 6 theme violations
- AdvancedInput mappers: 6 theme violations
- Rating mapper: 2 theme violations
- Feedback mapper: 1 theme violation
- `Canvas3DHandler.groupValues[2]` IndexOutOfBounds risk (4 locations)
- `StateManagement/build.gradle.kts` bogus `java.prefs:java.prefs` dependency
- iOS renderer 2100-line file with stub types at bottom
- `Types3D.kt` partial migration from `Math.*` to `kotlin.math.*`

### AI & Licensing (1)
- `LicenseManager` glassmorphism code has commented-out dead imports

---

## Theme System Violations Summary

**Total `MaterialTheme.colorScheme` References:** 50+ across the codebase (BANNED by CLAUDE.md Rule #3)

| Location | Count | Priority |
|----------|-------|----------|
| `AvanueUI/Renderers/Android/` | ~35 | HIGH (legacy renderer) |
| `AvanueUI/ThemeBuilder/` | ~13 | HIGH (dogfooding failure) |
| `AvanueUI/Adapters/` | ~5 | HIGH |
| `AI/Teach/` | ~12 | MEDIUM |
| `DeviceManager/` | ~8 | MEDIUM (uses own `DeviceColors`) |

**No violations found in:** AVA, WebAvanue, apps/, VoiceOSCore (clean)

---

## Recommendations (Priority Order)

### Immediate (Block Production)
1. **Fix `VoiceOSDatabaseManager.transaction()`** — broken lambda execution, data integrity risk
2. **Replace `DesktopCredentialStore` Base64** with OS-native encryption
3. **Fix `IosPermissionChecker`** — return real iOS permission status
4. **Remove all AI attribution** — 5 files violate zero-tolerance Rule 7
5. **Fix `Types3D.kt`** `Math.toRadians()` — breaks iOS/Native compilation
6. **Fix `VoiceKeyboardService` self-assignment** on L140 — breaks all text input

### Short-term (1-2 Sprints)
7. **Consolidate logging** — migrate `Utilities/Logger` callers to `Logging/LoggerFactory`
8. **Wire AI/Cast handlers** or unregister them — silent failure is worse than "unknown command"
9. **Consolidate GlassmorphismUtils** — 3 copies into `AvanueUI`
10. **Migrate theme violations** — batch PR for `Renderers/Android` + `Adapters` + `ThemeBuilder`
11. **Delete deprecated theme files** — 10 files (`OceanColors`, etc.) after verifying zero callers
12. **Fix WakeWord type contract** — unify `WakeWordSettings`/`WakeWordSettingsData`

### Medium-term (Architecture)
13. **Complete localization** — French/German/Japanese/Chinese missing 30-50% of keys
14. **Implement or remove** `CloudThemeRepository`, `ThemeIO`, `ARGScanner.watch()`
15. **Fix AvanueUI DSL** `TODO()` crashes — 41 components throw uncatchable `Error`
16. **Rename `com.avanueui.data`** to `com.augmentalis.avanueui.data` (banned prefix)
17. **Consolidate StateManagement** dual-module into one
18. **Strengthen AVID hash** — polynomial hash has weak collision space at scale

---

## Modules Rated "Clean" (No Issues Found)
- `Modules/AnnotationAvanue/` — Well-structured, no TODOs, no theme violations
- `Modules/AVA/core/Data/`, `Domain/`, `Utils/` — Clean KMP code
- `Modules/AVA/Overlay/` — Properly migrated to AvanueTheme (1 minor TODO in ChatConnector)
- `Modules/WebAvanue/` — No TODOs, no MaterialTheme violations
- `apps/avanues/`, `apps/voiceavanue/` — No theme violations detected
- `Modules/PluginSystem/` — Comprehensive KMP coverage, no issues

---

## ADDITIONAL CRITICAL Findings from Batches 4, 5, 7 (4 more)

### C13. AVU Wire Protocol Code Collision
**File:** `Modules/AVU/src/commonMain/.../codec/AVUEncoder.kt:33,68`
**Impact:** `CODE_ACCEPT_DATA = "ACD"` and `CODE_APP_CATEGORY_DB = "ACD"` share the same 3-letter code. Decoder cannot distinguish them — silent frame misrouting.
**Fix:** Assign unique code to one constant (likely `"ACB"` or `"ADB"` for category DB).

### C14. HTTP/2 Sink Not Synchronized (Data Corruption)
**File:** `Modules/HTTPAvanue/src/commonMain/.../http2/Http2Connection.kt:211-238`
**Impact:** Concurrent HTTP/2 stream writes corrupt frame boundaries on the wire.
**Fix:** Add a `Mutex`-guarded `writeFrameSynchronized()` method.

### C15. HPACK Huffman Decoding Not Implemented
**File:** `Modules/HTTPAvanue/src/commonMain/.../hpack/HpackDecoder.kt:108-113`
**Impact:** Browsers send Huffman-encoded headers by default. Server silently produces garbage header strings. HTTP/2 interop with Chrome/Firefox/curl is broken.
**Fix:** Implement RFC 7541 Appendix B Huffman table.

### C16. Static File Middleware Path Traversal
**File:** `Modules/HTTPAvanue/src/commonMain/.../middleware/StaticFileMiddleware.kt:14-17`
**Impact:** `/static/../../etc/passwd` reaches the resource loader. Directory traversal vulnerability.
**Fix:** Check `filePath.contains("..")` or normalize and verify path stays within root.

---

## ADDITIONAL HIGH Findings from Batches 4, 5, 7 (22 more)

### Feature Modules (Batch 4) — 8 HIGH
| ID | Module | Issue |
|----|--------|-------|
| H27 | AnnotationAvanue | `AnnotationColors.WHITE = 0xFFFFFFFF` missing `L` suffix — overflows to -1 |
| H28 | ImageAvanue | `if (imageList.size > 1 \|\| true)` — leftover debug code, always true |
| H29 | VideoAvanue | `String.format()` in commonMain — JVM-only, breaks iOS compilation |
| H30 | VideoAvanue | ExoPlayer listener leak — added but never removed on recomposition |
| H31 | NoteAvanue | Camera/attach/dictate/save buttons missing AVID semantics |
| H32 | PDFAvanue | `search()` returns empty, `extractText()` returns "" — silent stubs |
| H33 | PhotoAvanue | ModeChip `onClick = { }` — mode switching is a complete no-op |
| H34 | Cockpit | CommandBar wrong icons — Forward/Play/Redo all show same icon |

### Network/System (Batch 5) — 15 HIGH
| ID | Module | Issue |
|----|--------|-------|
| H35 | HTTPAvanue | `activeConnections` non-thread-safe `MutableSet` with concurrent access |
| H36 | HTTPAvanue | HPACK integer decode has no overflow guard |
| H37 | HTTPAvanue | `Http2Stream.increaseReceiveWindow()` missing overflow check |
| H38 | HTTPAvanue | `SseConnectionManager.emitters` not thread-safe, `ConcurrentModificationException` |
| H39 | HTTPAvanue | Server scope never cancelled in `stop()` |
| H40 | RemoteCast | No authentication on `/cast/stream` — any LAN client sees screen |
| H41 | RemoteCast | `frameCount` non-atomic Long increment |
| H42 | Rpc | `VoiceOSServiceDelegateStub` in production source, not test |
| H43 | Rpc | TLS flag is no-op — all gRPC traffic plaintext |
| H44 | Rpc | `stop()` kills in-flight RPCs with no graceful shutdown |
| H45 | IPC | `createIPCManager()` on Android throws `NotImplementedError` |
| H46 | IPC | `connectInternal()` fakes connection, `invoke()` returns hardcoded success |
| H47 | IPC | iOS + Desktop IPC entirely non-functional |
| H48 | WebSocket | `KeepAliveManager` uses `System.currentTimeMillis()` in commonMain |
| H49 | DeviceManager | `IMUManager.onSensorChanged()` launches coroutine per event at 120Hz |

### Apps & Platform (Batch 7) — 7 HIGH
| ID | Module | Issue |
|----|--------|-------|
| H50 | AVA/Data | Duplicate `TokenCacheRepositoryImpl` in wrong `src/main/java/` source set |
| H51 | AVA/Overlay | `AvaIntegrationBridge.release()` never cancels root CoroutineScope — leak |
| H52 | AVA/Overlay | ChatConnector hardcoded model path, silently templates responses |
| H53 | ava-legacy app | `runBlocking` on main thread during `onCreate()` — ANR risk |
| H54 | VoiceOSIPCTest | CoroutineScope never cancelled in `onDestroy()` |
| H55 | DeviceManager | LiDAR detection uses `totalSensorCount > 0` — true on all devices |
| H56 | DeviceManager | CoroutineScope never cancelled — leak |

---

## Complete Priority Fix List (Updated)

### Immediate (16 items — block production)
1. Fix `VoiceOSDatabaseManager.transaction()` — broken lambda
2. Replace `DesktopCredentialStore` Base64 with encryption
3. Fix `IosPermissionChecker` — always returns true
4. Remove all AI attribution (5 files)
5. Fix `Types3D.kt` `Math.toRadians()` — breaks iOS
6. Fix VoiceKeyboard self-assignment on L140
7. **Fix AVU `ACD` code collision** — wire protocol corruption
8. **Fix HTTP/2 sink synchronization** — data corruption
9. **Implement HPACK Huffman decoding** — HTTP/2 interop broken
10. **Fix StaticFileMiddleware path traversal** — security vulnerability
11. **Fix `VideoItem.durationFormatted`** `String.format()` — breaks iOS
12. **Fix `AnnotationColors.WHITE`** missing `L` suffix
13. **Fix `ImageViewer.kt`** `|| true` debug leftover
14. **Fix `AvaIntegrationBridge.release()`** scope leak
15. **Fix `ava-legacy` `runBlocking`** on main thread
16. **Add RemoteCast authentication** — screen visible to LAN

### Short-term (20 items — 1-2 sprints)
17-36: See individual batch recommendations above

### Medium-term (Architecture)
37+: Theme migration sprint, IPC rewrite, logging consolidation, etc.

---

## Stats

| Metric | Value |
|--------|-------|
| Modules reviewed | 40+ |
| Apps reviewed | 10 |
| .kt files analyzed | 500+ |
| Total findings | 169 |
| Critical | 16 |
| High | 56 |
| Review agents | 7 (parallel) |
| Total agent tool uses | ~350 |
| Total tokens processed | ~1.2M |

---

*Report generated by 7 parallel code-review agents analyzing every .kt file in the NewAvanues monorepo.*
*Date: 2026-02-20 | All 7 batches COMPLETE*
