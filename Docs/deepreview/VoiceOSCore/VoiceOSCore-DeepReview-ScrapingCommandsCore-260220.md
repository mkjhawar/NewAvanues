# VoiceOSCore Deep Review — Part 2: Scraping, Commands, Accessibility & Core
**Date:** 2026-02-20
**Reviewer:** Code Reviewer Agent
**Scope:** `Modules/VoiceOSCore/src/` — scraping, command manager, accessibility service, VOS distribution, IMU/gaze, RPC, synonym parser, commonMain models, iOS/desktop actuals

---

## Summary

The VoiceOS accessibility service, screen extractor, VOS import/export pipeline, and commonMain interfaces are architecturally sound and production-quality. However, significant security vulnerabilities exist in the SFTP sync system (MITM-vulnerable default configuration), the plugin manager (trusted signature loading is a no-op), and the gRPC service binding is broken (empty `ServerServiceDefinition`). The command manager subsystem contains multiple silent-failure stubs and thread-safety issues that silently corrupt state at runtime. The SynonymParser previously noted as having a hardcoded timestamp has been **fixed** — it now correctly uses `kotlinx-datetime`.

---

## Issues

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| **Critical** | `commandmanager/plugins/PluginManager.kt:934` | `loadTrustedSignatures()` is a no-op — the body contains only a log statement. `trustedSignatures` set is always empty after `initialize()`. At line 295 the code correctly rejects plugins when `trustedSignatures.isEmpty()`, so the effective result is that **all APK plugins are always rejected** in production (cannot load anything). This is better than accepting all, but means the plugin system is non-functional. | Implement actual signature loading from `EncryptedSharedPreferences` (Jetpack Security) keyed by a hardware-backed key. This is the root cause the entire plugin-loading path is dead. |
| **Critical** | `vos/sync/VosSftpClient.kt:54` | Default parameter `hostKeyChecking = "no"` across all connect calls disables SSH host key verification, enabling MITM attacks. All four call sites in `VosSyncManager.kt` (lines 55, 90, 217, 359) also default to `"no"`. Any attacker on the same network can impersonate the server and receive uploaded VOS command files or serve malicious ones. | Change default to `"accept-new"` for testing and `"yes"` for production. Add a `SyncConfig` data class that exposes the policy, defaulting to `"yes"` when `BuildConfig.DEBUG` is false. Never allow `"no"` to reach production. |
| **Critical** | `rpc/VoiceOSRpcServer.kt:357-360` | `VoiceOSServiceGrpcKt.VoiceOSServiceCoroutineImplBase.bindService()` returns an empty `ServerServiceDefinition` — it does not register any service methods. This means gRPC clients cannot discover or invoke any methods on the server at runtime, even though `VoiceOSServiceImpl` has full implementations. | The service definition must be built by reflecting the interface and registering handlers. Use the standard gRPC-Kotlin `AbstractCoroutineServerImpl.bindService()` pattern, or at minimum manually register each method using `ServerServiceDefinition.builder("voiceos.VoiceOSService").addMethod(...).build()`. |
| **High** | `commandmanager/dynamic/CommandPersistence.kt:190-202` | `CommandStorage.deleteNamespace()` does not delete anything. The comment says "Current schema doesn't track namespace, so this deletes all custom commands" but the implementation only reads the count — it calls `dao.getCommandCount()` but never calls any delete method. It silently returns success with a count of zero deleted records. | Implement actual deletion: add a `deleteAllForLocale()` method to the DAO and call it here. If namespace-scoped deletion is needed, add a `namespace` column to the schema. |
| **High** | `commandmanager/cache/CommandCache.kt:134,375` | Two uses of `GlobalScope.launch()` (`loadGlobalCommandsFromDatabase()` and `warmCache()`). `GlobalScope` is detached from all lifecycle — if the service is destroyed before the DB query completes, the coroutine continues writing to `tier1Cache` which may have been garbage collected or replaced. This is a potential crash or stale-data source. | Replace with an injected or passed-in `CoroutineScope` (tied to the accessibility service lifecycle), or use a supervised `CoroutineScope` stored as a member of `CommandCache`. |
| **High** | `commandmanager/routing/IntentDispatcher.kt:213` | `handlerSupportsApp()` always returns `true`, giving every handler a +0.3 confidence bonus regardless of whether it actually supports the current app. This inflates confidence scores for handlers that should not be candidates, making the confidence-scoring algorithm inaccurate. The TODO comment confirms this is unimplemented. | Maintain a `Map<String, Set<String>>` of `handlerCategory → supportedPackages`. Handlers that are truly global can be represented by a sentinel value. Until that map exists, return `0.15f` (the no-context partial credit) rather than the full 0.3f context match bonus. |
| **High** | `commandmanager/routing/IntentDispatcher.kt:316,317` | Comments acknowledge feedback is never persisted to DB and never fed to ML training. The `feedbackDatabase` is an in-memory `mutableListOf<UserFeedback>()` that is capped at 500 entries and lost on every process restart. The `recordUserCorrection()` method has two TODO comments on lines 315-316 stating this explicitly. | Either persist feedback to the `CommandDatabase` (a `CommandUsageEntity` or new `RoutingFeedback` table) or remove the feedback infrastructure from production code until it can be wired up. In-memory-only feedback creates the illusion of a learning system without any actual learning. |
| **High** | `commandmanager/dynamic/CommandPersistence.kt:7-8` | File header comment says "Note: This is a stub implementation showing the structure." The file is not a stub — `CommandStorage`, `PersistentCommandRegistry`, and `CommandExporter` are all real, working implementations that interact with Room. The misleading header will confuse maintainers into thinking this code should be replaced. | Remove or correct the stub disclaimer in the file header and the class-level comment on line 87-88. |
| **High** | `rpc/VoiceOSRpcServer.kt (iOS):21-33` | iOS `VoiceOSRpcServer.start()` sets `running = true` and logs a message but does not start any actual RPC server. iOS clients calling `isRunning()` will receive `true` without any network listener being created. This is already tracked in MEMORY.md but deserves a `@Deprecated` annotation or explicit documentation so callers do not depend on a non-functional server. | Add `@Deprecated("iOS RPC not implemented", level = DeprecationLevel.WARNING)` to `start()`. Alternatively gate behind `if (BuildConfig.DEBUG)` if the stub is intentional for local development only. |
| **High** | `commandmanager/actions/ActionFactory.kt:880-934` (DynamicBrowserAction `forward`) | `DynamicBrowserAction.execute()` for `action = "forward"` calls `GLOBAL_ACTION_BACK` and then immediately returns `createErrorResult(..., NOT_SUPPORTED, ...)` — it performs the opposite navigation action (back) before reporting failure. This is a functional bug: the user says "browser forward" and the system goes back instead. | Remove the `performGlobalAction(accessibilityService, GLOBAL_ACTION_BACK)` call from the `"forward"` branch. Just return the `NOT_SUPPORTED` error without side effects. |
| **High** | `commandmanager/actions/ActionFactory.kt:853-855` | `DynamicEditingAction` "redo" branch returns `createSuccessResult()` immediately without performing any action — it is a silent no-op returning success. | Implement redo via `KEYCODE_Z` with `CTRL` modifier (similar to the undo path), or via `AccessibilityNodeInfo.ACTION_REDO` if available (`Build.VERSION.SDK_INT >= 26`). |
| **Medium** | `managers/hudmanager/spatial/GazeTracker.kt:67-68` | `_currentGaze` is a `mutableStateOf<GazeTarget?>()` (Compose state), but the `CoroutineScope` at line 74 uses `Dispatchers.Main` while `startTracking()` is a suspend function. When the stub is enabled and actual camera work runs, any state mutation from a non-Main dispatcher will crash with a `CalledFromWrongThreadException`. Note: this is a stub and GazeTracker is intentionally disabled (tracked in MEMORY.md). | Pre-fix before re-enabling: switch `_currentGaze` to `MutableStateFlow` for coroutine-safe cross-thread updates, or ensure all state writes are wrapped in `withContext(Dispatchers.Main)`. |
| **Medium** | `commandmanager/dynamic/CommandPersistence.kt:50-52` | `CommandEntity.fromVoiceCommandData()` serializes metadata using `entries.joinToString("|") { "${it.key}:${it.value}" }`. If any key or value contains `|` or `:`, the round-trip through `toVoiceCommandData()` (line 62-65) will incorrectly split on those characters, corrupting metadata. The `:` delimiter is particularly problematic since URL values commonly contain colons. | Use `JSONObject` or kotlinx.serialization for metadata serialization, matching the approach already used in `CommandStorage.saveCommand()` (line 113) which correctly uses `JSONArray`. |
| **Medium** | `vos/VosFileImporter.kt:215` | `detectFileId()` has a dead branch: the `else` arm returns `locale` unconditionally whether `parsedDomain == "web"` or not — both sides of the `if` expression return `locale`. The intent was to return `locale` for app domains and `parsedDomain` (the domain string) for web domains, but the bug means web file IDs are always set to locale instead of the domain name. | Change to: `return if (parsedDomain == "web") parsedDomain else locale` — but this is also wrong. The correct fix is `return if (parsedDomain == "web") locale else parsedDomain` (web files are identified by domain, app files by locale). Verify with integration test. |
| **Medium** | `commandmanager/actions/ActionFactory.kt:847-850` | `DynamicEditingAction` undo is implemented by dispatching `KEYCODE_Z` through `AudioManager.dispatchMediaKeyEvent()`. `AudioManager` is for media key events (play/pause/next), not keyboard shortcuts. Ctrl+Z will not work this way on any Android device. | Use `AccessibilityNodeInfo.ACTION_CUT`/`ACTION_PASTE` chain for undo simulation, or send `KeyEvent(KeyEvent.ACTION_DOWN, KEYCODE_Z)` through `accessibilityService.performGlobalAction()`. Better: look for an `ACTION_UNDO` accessibility action on the focused node. |
| **Medium** | `VoiceOSAccessibilityService.kt:504-514` | In `processVoiceCommand()`, when building the callback `QuantizedCommand`, `actionType = CommandActionType.EXECUTE` is hardcoded regardless of what the actual handler matched. The result passed to `onCommandExecuted()` does not reflect the real command type or target AVID, making the callback useless for telemetry. | Pass the `QuantizedCommand` that was actually matched (available from the `HandlerResult.Success` response) rather than constructing a synthetic one with `CommandActionType.EXECUTE`. |
| **Medium** | `commandmanager/dynamic/CommandPersistence.kt:62-65` | `CommandEntity.toVoiceCommandData()` uses `metadata.split("|").filter { it.isNotBlank() && it.contains(":") }.associate { val (key, value) = it.split(":", limit = 2) ... }`. The destructuring assignment `val (key, value) = it.split(":", limit = 2)` will throw `IndexOutOfBoundsException` if the split produces a list with fewer than 2 elements (e.g., a string like `"key:"` with empty value). While `limit = 2` guarantees at most 2 elements, the destructuring expects exactly 2, which will fail on any entry that contains only a key with no value. | Use `val parts = it.split(":", limit = 2); if (parts.size == 2) parts[0] to parts[1]` with explicit size check, or switch the entire metadata field to JSON. |
| **Medium** | `managers/localizationmanager/` (multiple files) | The localization manager UI files (`LocalizationManagerActivity.kt`, `AnimatedLanguageDisplay.kt`, `SettingsDialog.kt`) are not accessible in this review pass but the broader `managers/hudmanager/ui/` files use `MaterialTheme.colorScheme.*` (confirmed in MEMORY.md — 30 occurrences across DeviceManager module). | Replace all `MaterialTheme.colorScheme.*` usages with `AvanueTheme.colors.*` per CLAUDE.md MANDATORY RULE #3. |
| **Medium** | `commandmanager/cache/CommandCache.kt:134,148` | `tier1Cache` is a plain `mutableMapOf<String, Command>()`. It is written from the `GlobalScope.launch(Dispatchers.IO)` coroutine (line 148: `tier1Cache[key] = command`) and read from the main thread without any synchronization. This is an unsynchronized mutable shared state between threads — a classic data race. | Replace `tier1Cache` with `ConcurrentHashMap<String, Command>()`, which matches the documented intent and the Tier 2 LRU cache's thread-safety properties. |
| **Medium** | `VoiceOSAccessibilityService.kt:484-488` | `refreshScreen()` creates a new `AccessibilityEvent.obtain()` but this event is never recycled. On API < 34, all obtained `AccessibilityEvent` objects must be recycled via `event.recycle()`. The event here is passed to `handleScreenChange()` which passes its `packageName` but then exits — the event is never passed to `event.recycle()`. | Wrap in a try-finally: `val event = AccessibilityEvent.obtain()...; try { handleScreenChange(event) } finally { if (SDK < UPSIDE_DOWN_CAKE) event.recycle() }`. |
| **Medium** | `commandmanager/plugins/PluginManager.kt:935-938` | `loadTrustedSignatures()` uses invalid comment syntax on lines 935-936: `/ TODO:` and `/ SECURITY:` (single slash instead of `//`). These are not valid Kotlin comments — they are parsed as division operators, which means the file contains syntax that should fail to compile. If the file compiles, the Kotlin compiler is silently ignoring them as divide-by-TODO expressions. | Replace `/ TODO:` with `// TODO:` and `/ SECURITY:` with `// SECURITY:`. Verify with compilation. |
| **Medium** | `commandmanager/actions/ActionFactory.kt:887-890` | `DynamicBrowserAction.execute()` for `action = "refresh"` starts `Intent(Intent.ACTION_VIEW)` with no URI. Starting an `Intent.ACTION_VIEW` with no URI will throw `ActivityNotFoundException` on most devices, which is caught and converted to `NOT_SUPPORTED`. The try-catch swallows this predictable failure. | Either use a WebView-backed refresh (call `webView.reload()`) or send a keyboard shortcut (`KeyEvent.KEYCODE_F5`) via the accessibility service. The current implementation always fails silently. |
| **Low** | `synonym/SynonymParser.kt:1-9` | File header says "Author: VOS4 Development Team" — violates CLAUDE.md Rule 7 (no AI attribution). The comment was originally `Author: VOS4 Development Team` which is a team name, not AI attribution, but Rule 7 specifies author should be "Manoj Jhawar" or omitted. Note: `currentTimestamp()` previously was a hardcoded date per MEMORY.md — this has been **correctly fixed** using `kotlinx.datetime`. The timestamp bug is resolved. | Change `Author: VOS4 Development Team` to `Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC` to match the style of other files in the module. |
| **Low** | `managers/hudmanager/spatial/GazeTracker.kt:74` | `CoroutineScope(SupervisorJob() + Dispatchers.Main)` is created but never cancelled if `dispose()` is not called. The `GazeTracker` holds a dangling scope. | `dispose()` already calls `scope.cancel()` — this is fine as long as `dispose()` is always called. Add a comment making the lifecycle contract explicit, or implement `Closeable`. |
| **Low** | `VoiceOSAccessibilityService.kt:534` | The `else -> { Log.d(TAG, "Command result: $result") }` branch in `processVoiceCommand()` silently swallows unknown `HandlerResult` subtypes. While the current sealed class covers all cases, future additions will be silently ignored. | Replace `else` with an exhaustive `when` on the sealed `HandlerResult` class. If `else` is intentional as a catch-all, document which result types are expected here. |
| **Low** | `commandmanager/dynamic/CommandPersistence.kt (header comment)` | File header says `@author VOS4 Development Team` — same attribution concern as above. | Change to match project owner attribution. |
| **Low** | `vos/sync/VosSyncManager.kt:393-394` | In `syncAll()`, errors from both upload and download phases are accumulated, but the method always returns `SftpResult.Success(result)` even when errors is non-empty. The comment `// Still success, errors are in the result` documents the intent, but callers cannot distinguish a partial success from a full success without inspecting `result.hasErrors`. | Consider returning `SftpResult.Error` when `errors.isNotEmpty()` and full failure when `uploadedCount == 0 && downloadedCount == 0`. Or document the contract clearly with KDoc. |
| **Low** | `commandmanager/actions/ActionFactory.kt (general)` | `DynamicScrollAction.findScrollableNode()` traverses the entire accessibility tree recursively without a depth limit, and does not recycle intermediate child nodes. For deep hierarchies this leaks `AccessibilityNodeInfo` objects (pre-API 34). | Add a `maxDepth` guard and recycle non-scrollable children in a `finally` block, matching the pattern in `AndroidScreenExtractor`. |

---

## Recommendations

### Security (address immediately)

1. **SFTP host key verification**: Change the default `hostKeyChecking` parameter from `"no"` to `"yes"` in `VosSftpClient.connect()` and all `VosSyncManager` call sites. Create a `SyncSecurityConfig` that the caller must explicitly set to a less-strict mode for development, making the insecure choice opt-in rather than opt-out.

2. **Plugin signature loading**: Implement `loadTrustedSignatures()` using `EncryptedSharedPreferences`. Until this is done, the plugin system cannot load any plugins — a functionality that is currently advertised as working but is completely non-functional.

3. **gRPC service binding**: Fix `VoiceOSServiceGrpcKt.VoiceOSServiceCoroutineImplBase.bindService()` to actually register service methods. The current empty `ServerServiceDefinition` means no RPC call can ever reach its implementation, making the entire gRPC server inert.

### Correctness (fix before next release)

4. **`DynamicBrowserAction` forward bug**: Remove the `GLOBAL_ACTION_BACK` call from the `"forward"` action branch — it goes backward while appearing to attempt forward navigation.

5. **`deleteNamespace()` no-op**: The method silently claims to delete commands but does nothing. Either implement deletion or return `Result.failure(UnsupportedOperationException(...))` to force callers to handle the missing feature.

6. **`detectFileId()` logic error**: Fix the dead else-branch in `VosFileImporter.detectFileId()` so web files are identified by domain name, not locale.

7. **Undo implementation**: Replace the `AudioManager.dispatchMediaKeyEvent(KEYCODE_Z)` approach with a valid undo mechanism.

### Thread safety

8. **`tier1Cache` race**: Replace `mutableMapOf()` with `ConcurrentHashMap()` in `CommandCache` to prevent data races between the DB-loading coroutine and cache readers.

9. **`GlobalScope` usage**: Replace both `GlobalScope.launch()` calls in `CommandCache` with a lifecycle-scoped coroutine scope passed in through the constructor.

### Code quality

10. **Fix single-slash comments** in `PluginManager.kt:935-936` — these are parse errors disguised as comments.

11. **`handlerSupportsApp()` always true**: This inflates every handler's confidence score by +0.3 regardless of app compatibility. Either implement the app-handler registry or revert the scoring to neutral (0.15f) until the registry exists.

12. **Feedback not persisted**: The `IntentDispatcher` feedback database is in-memory only. Either wire it to `CommandDatabase` or remove the infrastructure to avoid misleading maintainers about learning capabilities.

13. **`CommandEntity` pipe-delimited metadata**: Switch to JSON serialization to prevent corruption from special characters in metadata values.

---

## Known Stubs (Confirmed Intentional per MEMORY.md)

The following items were reviewed and confirmed as intentional/tracked:

- `GazeTracker` — ML Kit disabled to reduce APK size. Tracked in `PROJECT-TODO-BACKLOG.md`.
- iOS `VoiceOSRpcServer` — iOS RPC is TBD. Not a production path.
- `SynonymParser.currentTimestamp()` — Previously hardcoded, now **correctly fixed** with `kotlinx.datetime`.

---

## Files Reviewed

**androidMain:**
- `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/AndroidScreenExtractor.kt`
- `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/VoiceOSAccessibilityService.kt`
- `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/AccessibilityNodeAdapter.kt`
- `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/ScreenCacheManager.kt`
- `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/commandmanager/dynamic/CommandPersistence.kt`
- `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/commandmanager/dynamic/DynamicCommandRegistry.kt`
- `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/commandmanager/actions/ActionFactory.kt`
- `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/commandmanager/routing/IntentDispatcher.kt`
- `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/commandmanager/cache/CommandCache.kt`
- `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/commandmanager/plugins/PluginManager.kt`
- `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/vos/VosFileImporter.kt`
- `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/vos/VosFileExporter.kt`
- `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/vos/sync/VosSftpClient.kt`
- `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/vos/sync/VosSyncManager.kt`
- `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/vos/sync/SyncModels.kt`
- `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/rpc/VoiceOSRpcServer.kt`
- `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOSCore/src/androidMain/kotlin/com/augmentalis/voiceoscore/managers/hudmanager/spatial/GazeTracker.kt`

**commonMain:**
- `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/VoiceOSCore.kt`
- `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/element/ElementFingerprint.kt`
- `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/synonym/SynonymParser.kt`

**iosMain:**
- `/Volumes/M-Drive/Coding/NewAvanues/Modules/VoiceOSCore/src/iosMain/kotlin/com/augmentalis/voiceoscore/rpc/VoiceOSRpcServer.kt`
