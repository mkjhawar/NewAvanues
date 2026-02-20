# PluginSystem Deep Code Review
**Date:** 2026-02-20
**Reviewer:** code-reviewer agent
**Module:** `Modules/PluginSystem/`
**Files Reviewed:** ~95 .kt files across commonMain, androidMain, iosMain, commonTest, androidUnitTest
**Branch:** HTTPAvanue

---

## Summary

The PluginSystem module is a well-structured, ambitious plugin architecture for the Universal Plugin Architecture (UPA). The core design — KMP expect/actual isolation, coroutine-based lifecycle, confidence-based dispatch — is sound. However, the module contains **one compile-breaking KMP mismatch**, **one critical security bypass** (signature verification non-enforcing), and **one persistent stub** (Android persistence returns in-memory storage with a TODO). Additionally, there are systemic issues with `System.currentTimeMillis()` used directly in commonMain (KMP violations), thread-safety hazards mixing Java `synchronized` with coroutine Mutex, a non-functional confidence threshold in the handler bridge, and significant duplication of plugin registration logic across two separate registration paths.

---

## Issues

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| **CRITICAL** | `iosMain/.../security/PermissionStorage.kt` L1–80 | **KMP compile break**: iOS `actual class PermissionStorage` implements `IosPermissionStorage` with `save/load/delete/loadAll` API, while `expect class PermissionStorage` in commonMain declares `savePermission/hasPermission/getAllPermissions/revokePermission/clearAllPermissions/isEncrypted/getEncryptionStatus/migrateToEncrypted`. These are completely different interfaces — the `actual` declaration does not satisfy the `expect`. This will not compile for the iOS target. | Rewrite iOS `actual class PermissionStorage` to directly implement the same method signatures as the `expect class`. Use `NSUserDefaults` for the storage backend but mirror the Android API surface exactly. |
| **CRITICAL** | `commonMain/.../distribution/PluginInstaller.kt` L107–111 | **Security bypass**: Signature verification failure is treated as a non-blocking warning — execution continues after `PluginLog.w(...)`. Any unsigned or tampered plugin package will be silently installed. The comment `// For now, we log a warning and continue` makes this intentional but there is no runtime configuration flag to enforce it. | Make signature failure a hard error by default. Add an `enforceSignatureVerification: Boolean` flag in `PluginSystemConfig` (default `true`). When `true`, abort installation and return `InstallResult.Failure("Signature verification failed")`. |
| **CRITICAL** | `androidMain/.../persistence/PluginPersistence.kt` L21–27 | **Stub / non-persistent storage**: `createDefaultPluginPersistence()` returns `InMemoryPluginPersistence()` with a comment "SQLDelight migration pending." Plugin state is lost on every app restart — installed plugins are forgotten. This violates Rule 1 (No Stubs). | Implement file-based persistence using `FileIO` (already available as an expect/actual), serializing `PluginInfo` to JSON in the app data directory. SQLDelight can follow later, but file-JSON is fully working. |
| **High** | `commonMain/.../core/ManifestValidator.kt` L326 | **Version constraint validation is a no-op**: `validateVersionConstraint()` — the range operator branches (`>=`, `<=`, `>`, `<`) all `return true` without any actual constraint parsing. A manifest can declare `requires-host: >=999.0.0` and validation passes. Only the exact-version branch does real work. | Replace with calls to `SemverConstraintValidator.isValidConstraint()` (already exists in the module at `dependencies/SemverConstraintValidator.kt`) and then `satisfies()` with the actual host version. |
| **High** | `androidMain/.../android/AndroidPluginHost.kt` L~50 | **Main-thread I/O hazard**: `scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)`. All plugin lifecycle operations — including `plugin.initialize()` which may do file I/O, class loading, or network requests — run on the Android main thread. This will cause ANRs under load. | Change dispatcher to `Dispatchers.Default` or `Dispatchers.IO` for the plugin host scope. UI-facing state updates should be dispatched to `Dispatchers.Main` only at the call site when updating UI. |
| **High** | `androidMain/.../android/PluginHandlerBridge.kt` L57 + L105–140 | **Confidence threshold is unused in dispatch path**: `minPluginConfidence` is declared as a configurable field and checked in `canPluginHandle()`, but the main `dispatch()` method never calls `canPluginHandle()` before accepting a plugin result. Any plugin that returns `ActionResult.Success` is accepted regardless of its confidence score. The threshold has zero effect on production dispatch. | In `dispatch()`, after `tryPluginDispatch()` returns `ActionResult.Success`, check whether the selected plugin's confidence meets `minPluginConfidence`. If not, fall through to legacy or return error. |
| **High** | `androidMain/.../platform/PluginClassLoader.kt` L~30–60 | **No plugin class isolation**: `DexClassLoader` is created with parent `this::class.java.classLoader` (the host app's classloader). Plugins can access all internal host application classes. Additionally, `loadClass()` silently ignores subsequent calls with different `pluginPath` after the first class is loaded — a second plugin loading from a different path reuses the first plugin's classloader silently. | For class isolation: create each `DexClassLoader` with `ClassLoader.getSystemClassLoader()` as parent and only expose an explicit allowlist of host APIs via a bridge interface. For the multi-path bug: store classloaders per `pluginPath`, not a single nullable field. |
| **High** | `commonMain/.../security/PluginSandbox.kt` L~110–150 | **Sandbox does not persist permissions**: `DefaultPluginSandbox.grantPermission()` stores permissions in its internal `synchronized` map but the class has no `PermissionStorage` reference. The KDoc says "Granted permissions are persisted to PermissionStorage" — this is false. Revoked or granted permissions are lost on restart. | Inject `PermissionStorage` into `DefaultPluginSandbox` constructor and call `permissionStorage.savePermission(...)` in `grantPermission()` and `revokePermission()`. Load persisted permissions in `init {}`. |
| **High** | `commonMain/.../universal/PluginLifecycleManager.kt` L69–84 | **Coroutine Job leak in `manage()`**: A coroutine is launched to collect `plugin.stateFlow` but the returned `Job` is discarded — `scope.launch { plugin.stateFlow.collect { ... } }` result is never stored. When a plugin is later unmanaged or the lifecycle manager shuts down, these collection coroutines cannot be cancelled, leaking until the scope is cancelled. | Store the Job in `ManagedPlugin.observerJob: Job` and cancel it in an `unmanage(pluginId)` method. |
| **High** | `iosMain/.../platform/PluginClassLoader.kt` L~40–60 | **iOS static plugin registry is not thread-safe**: `pluginRegistry: MutableMap<String, Any>` is a companion object property mutated from multiple callers without synchronization. Concurrent plugin registrations from background threads can corrupt the map (ConcurrentModificationException or lost entries). | Protect with a `Mutex` (coroutine-aware) or `@Synchronized` on a lock object. |
| **High** | `commonMain/.../marketplace/MarketplaceDataProvider.kt` L368–399 | **Stub/fake progress in `startInstall()`**: Progress is simulated with a hardcoded `for (i in 1..9) { delay(200) }` loop. After the `api.download()` call succeeds, a `delay(500)` simulates installation — no actual installation occurs. The package bytes from `api.download()` are received but never passed to `PluginInstaller`. | Remove the fake progress loop. Use `api.download()` to retrieve the package path, then call `PluginInstaller.installFromPackage(path)` and track its progress via `PluginInstaller`'s own progress flow. |
| **High** | `commonMain/.../security/SecurityAuditLogger.kt` L~580–600 | **Deadlock risk in log handlers**: External handlers (registered via `addExternalHandler()`) are invoked while holding `synchronized(lock)`. If any handler calls back into `SecurityAuditLogger.log()` it will deadlock. | Copy handlers to a local list before calling them: `val handlers = synchronized(lock) { externalHandlers.toList() }`, then iterate outside the lock. |
| **High** | `commonMain/.../security/SecurityAuditLogger.kt` L~550 | **O(n) buffer trim**: `eventBuffer.removeAt(0)` on a `MutableList` is O(n). At high event volume this creates CPU pressure. | Replace `MutableList<AuditEvent>` with `ArrayDeque<AuditEvent>` and use `removeFirst()` which is O(1). |
| **Medium** | `commonMain/.../core/PluginRegistry.kt` L116 | **`System.currentTimeMillis()` in commonMain (KMP violation)**: Used directly for `registeredAt` timestamp in `register()`. Bypasses the project's expect/actual time pattern. | Use `currentTimeMillis()` from `com.augmentalis.magiccode.plugins.universal` (the expect/actual already defined in the module at `PlatformTime.kt`). |
| **Medium** | `commonMain/.../security/PermissionEscalationDetector.kt` L575 | **`System.currentTimeMillis()` in commonMain (KMP violation)**: `currentTimeMillis()` defined in the same file as a private method returns `System.currentTimeMillis()` — JVM-only. This file is in commonMain. | Use the module's expect `currentTimeMillis()` function. Remove the private internal method. |
| **Medium** | `commonMain/.../security/SecurityAuditLogger.kt` companion L~540 | **`System.currentTimeMillis()` in commonMain (KMP violation)**: `generateEventId()` uses `System.currentTimeMillis()` as part of ID generation. | Same fix: use the module's `currentTimeMillis()` expect function. |
| **Medium** | `commonMain/.../transactions/TransactionManager.kt` L113 | **`System.currentTimeMillis()` in commonMain (KMP violation)**: Checkpoint creation timestamp uses `System.currentTimeMillis()`. | Use the module's `currentTimeMillis()` expect function. |
| **Medium** | `commonMain/.../marketplace/UpdateNotifier.kt` L243, L342 | **`System.currentTimeMillis()` in commonMain (KMP violation)**: `checkNow()` and `performCheck()` both call `System.currentTimeMillis()` directly. | Use `currentTimeMillis()` expect function. |
| **Medium** | `commonMain/.../security/PluginSandbox.kt` L~80–160 | **Mixed synchronization paradigm**: `DefaultPluginSandbox` uses Java `synchronized(lock)` (a monitor lock) while the rest of the module uses coroutine `Mutex`. These are not interoperable: a coroutine suspended inside `synchronized` blocks the thread, not just the coroutine. | Convert `DefaultPluginSandbox` to use coroutine `Mutex` throughout. Since `grantPermission` and `revokePermission` must become `suspend fun`, update the `PluginSandbox` interface to match. |
| **Medium** | `commonMain/.../core/PluginRegistry.kt` L445–452 | **Unchecked `valueOf()` in `addToIndex()`**: `PluginSource.valueOf(info.manifest.source.uppercase())` and `DeveloperVerificationLevel.valueOf(...)` will throw `IllegalArgumentException` at runtime if the manifest contains an unrecognized source or verification level string. `ManifestValidator` validates these, but the manifest model uses raw `String` fields so the data path is not type-safe. | Wrap with a try-catch or use `enumValues<PluginSource>().find { it.name == source }` to return null-safe fallback. |
| **Medium** | `commonMain/.../core/PluginNamespace.kt` `isWithinNamespace()` | **Path traversal vulnerability**: `filePath.startsWith(baseDir)` without normalization. Path `plugins/com.example.plugin_evil/secret.txt` would match namespace `plugins/com.example.plugin` because the evil path starts with the base prefix. | Normalize both paths with `File(path).canonicalPath` (or `Path.resolve` on KMP) before calling `startsWith`. Append trailing `/` to `baseDir` before the check. |
| **Medium** | `commonMain/.../hotreload/PluginStateStorage.kt` L~310 | **Blocking I/O inside Mutex**: `cleanupStaleStates()` calls `getStateMetadataInternal()` while holding the Mutex, and `getStateMetadataInternal()` calls `fileIO.readFileAsString()`. If `fileIO` performs blocking I/O, this can stall all other coroutines waiting on the same Mutex. | Move I/O outside the Mutex critical section or ensure `fileIO` uses `withContext(Dispatchers.IO)`. |
| **Medium** | `commonMain/.../hotreload/PluginStateStorage.kt` `parseMetadataJson()` | **Fragile manual JSON parsing via regex**: Plugin IDs with special characters (`:`, `{`, `}`, `"`) would break parsing. Already uses `FileIO` for storage — should use proper serialization. | Replace manual regex parsing with `kotlinx.serialization` JSON parsing. The module already uses `kotlinx-serialization` elsewhere. |
| **Medium** | `commonMain/.../transactions/TransactionManager.kt` | **Checkpoints are in-memory only**: Checkpoints are stored in a `mutableMapOf` in-memory. An app crash during plugin install or update means rollback is impossible on restart — the transaction system provides no crash-safety guarantee. | Persist checkpoint data to the filesystem (the `FileIO` abstraction is available) so rollback can be attempted after restart. |
| **Medium** | `androidMain/.../security/PermissionStorage.kt` `savePermission()` | **Read-modify-write race**: `savePermission()` reads all existing permissions, then writes back the updated set. Under concurrent access, a second write can lose updates from the first. EncryptedSharedPreferences operations are individually atomic but the read-modify-write sequence is not. | Use `apply { ... }` inside a single `edit()` block to be atomic, or add a `ReentrantLock` around the full read-modify-write sequence. |
| **Medium** | `commonMain/.../universal/GrpcPluginEventBus.kt` `getSubscriptionCount()` | **Misleading subscription count**: Returns the count of named subscriptions registered via `subscribe(name, ...)`, not the actual number of active Flow collectors. A caller using this count for health monitoring will get wrong data if unnamed collectors exist. | Document clearly that this counts named registrations only, and rename to `getNamedSubscriptionCount()`. |
| **Medium** | `androidMain/.../integration/PluginSystemSetup.android.kt` L243–295 + `androidMain/.../android/BuiltinPluginRegistration.kt` L56–113 | **Duplicate plugin registration logic**: The exact same 7 plugin factory registrations appear verbatim in `AndroidPluginSystemSetup.registerBuiltinHandlerPlugins()` AND `BuiltinPluginRegistration.registerAll()`. Any change to one must be manually applied to the other. | Delete `registerBuiltinHandlerPlugins()` from `AndroidPluginSystemSetup` and delegate to `BuiltinPluginRegistration.registerAll(host)` instead. |
| **Medium** | `androidMain/.../integration/PluginSystemSetup.android.kt` L308–315 | **Hardcoded `CommandActionType.CLICK`** in `AndroidCommandDispatcher.dispatch()`: Any command dispatched through the generic `ICommandDispatcher` interface receives `CommandActionType.CLICK` as its action type regardless of the actual command. This ignores the semantic intent of the command for NLU/handler routing. | Remove the typed `ICommandDispatcher` wrapper or use NLU to resolve the action type from the command phrase before creating `QuantizedCommand`. |
| **Medium** | `commonMain/.../universal/PluginLifecycleManager.kt` `checkAllPluginsHealth()` | **ConcurrentModificationException risk**: Iterates over `_managedPlugins.value` with `forEach` while the same map can be modified by concurrent calls to `manage()` or `unmanage()`. `StateFlow.value` returns a snapshot, so this is actually safe for the map itself — but health check results reference plugin objects that may be concurrently shutting down. | Defensively copy the map: `val snapshot = _managedPlugins.value.toMap()` then iterate over the snapshot. |
| **Medium** | `commonMain/.../universal/PluginPerformanceMonitor.kt` L101–102 | **Non-thread-safe mutable state**: `latencySamples: MutableMap<String, MutableList<Long>>` and `pluginMetricsMap: MutableMap<String, PluginMetrics>` are accessed from multiple coroutines without synchronization. Concurrent `recordOperation()` and `recordPluginExecution()` calls can corrupt these structures. | Protect with a coroutine `Mutex` or use `ConcurrentHashMap` (androidMain) with an atomic list replacement strategy. |
| **Medium** | `commonMain/.../universal/PluginPerformanceMonitor.kt` companion | **Non-thread-safe singleton**: `_instance: PluginPerformanceMonitor?` is a mutable companion object var with no synchronization. Concurrent calls to `initialize()` from different threads can create multiple instances or observe a torn write. | Use `@Volatile` on `_instance` and a double-checked locking pattern, or use `AtomicReference`. |
| **Medium** | `iosMain/.../security/PermissionStorage.kt` L~13–17 | **Permissions stored unencrypted on iOS**: Even after the interface mismatch is fixed, the iOS implementation stores plugin permissions in `NSUserDefaults` — unencrypted and user-accessible. Android uses `EncryptedSharedPreferences` (AES256-GCM). | Use iOS Keychain for permission storage. The project already has `KeychainCredentialStore` in the Foundation module — use the same Keychain pattern here. |
| **Medium** | `commonMain/.../core/AvuManifestParser.kt` L198–201 | **Source classification is prefix-guessing**: Plugin source is classified as `PRE_BUNDLED`, `APPAVENUE_STORE`, or `THIRD_PARTY` based on whether the ID starts with `com.augmentalis.` or `com.appavenue.`. A malicious third-party plugin with ID `com.augmentalis.evilplugin` would be classified as `PRE_BUNDLED`, bypassing any policy checks. | Source should come from where the manifest was loaded (the `PluginDiscovery` source), not from the plugin ID prefix. Propagate `PluginSource` from the discovery layer rather than inferring from the ID. |
| **Medium** | `commonMain/.../discovery/FileSystemPluginDiscovery.kt` L317–327 | **`loadPluginDefault()` returns `UnsupportedOperationException`**: When no custom `PluginLoader` is provided, calling `loadPlugin(descriptor)` always fails with `UnsupportedOperationException("Default plugin loading not implemented...")`. This is a de-facto stub for the default code path. | This is the "no loader provided" case — the failure path is acceptable as a design choice (force callers to provide a loader). However, the KDoc should clearly state "no-op without a custom PluginLoader" and the exception message should say "Provide a custom PluginLoader via the constructor." |
| **Medium** | `androidMain/.../android/data/AndroidAccessibilityDataProvider.kt` | **`System.currentTimeMillis()` in androidMain**: This is acceptable for Android-specific code, but ensure the interface (`AccessibilityDataProvider`) does not expose this to commonMain. | No action needed — androidMain-only usage is acceptable. |
| **Low** | `commonMain/.../security/SignatureVerifier.kt` (common `TrustStore`) | **`TrustStore` is documented as NOT thread-safe**: The KDoc explicitly warns "This class is NOT thread-safe." But `TrustStore` is accessed from multiple plugins concurrently. | Add a `ReentrantReadWriteLock` or convert to coroutine `Mutex`. Given the KDoc acknowledges it, at minimum the production factory that creates `TrustStore` instances should wrap it in a thread-safe decorator. |
| **Low** | `androidMain/.../security/SignatureVerifier.kt` | **OOM risk for large plugins**: `File(packagePath).readBytes()` reads the entire plugin package into memory for signature verification. PluginConfig allows plugins up to the configured `maxPackageSize` — potentially 200MB+ — causing an OOM error on low-memory devices. | Use a streaming SHA digest (feed the `InputStream` chunk-by-chunk to `MessageDigest.update()`) to verify the signed hash without loading the full bytes into memory. |
| **Low** | `commonMain/.../universal/GrpcPluginEventBus.kt` `generateEventId()` | **Mutex acquired just to increment a counter**: A full coroutine `Mutex` is used to increment a `Long` counter for event ID generation. This adds coroutine suspension overhead for every event. | Use `AtomicLong` (available in Kotlin/JVM and via `kotlinx-atomicfu` for KMP) — zero lock overhead for simple counter increment. |
| **Low** | `commonMain/.../core/ManifestValidator.kt` `validatePermissions()` | **Unknown permissions allowed by default**: When `strictManifestValidation=false` (the default), unknown permissions produce a WARNING but validation still passes. A third-party plugin can declare arbitrary permission strings that have no enforcement, giving a false sense of permission management. | Log unknown permissions more prominently (as errors, not warnings) and consider making `strictManifestValidation=true` the default for production. |
| **Low** | `commonMain/.../universal/UniversalPlugin.kt` `saveState()` / `restoreState()` | **Default implementations are no-ops**: Both `saveState()` and `restoreState()` have default no-op implementations. Plugin authors who forget to override them will silently lose state across hot-reloads. | Add `@Deprecated("Override saveState for hot-reload support", level = DeprecationLevel.WARNING)` on the default — or at minimum add a KDoc warning that the default is a no-op and hot-reload will not work without overriding. |
| **Low** | `commonMain/.../core/PluginManifest.kt` | **`source` and `verificationLevel` are plain `String` fields**: All consumers must do unsafe `valueOf()` casts. Type errors can only be caught at runtime. | Change to typed fields: `source: PluginSource` and `verificationLevel: DeveloperVerificationLevel`. The `AvuManifestParser` and YAML parser already know the correct values — use the typed enums there. |
| **Low** | `androidMain/.../android/PluginCommandDispatcher.kt` L107 | **Debug logging at `Log.d` unconditionally**: `Log.d(tag, "Routing took ${routingTimeMs}ms, found ${candidates.size} candidates")` and several other `Log.d` calls fire on every dispatch regardless of the `debugMode` configuration. In production this adds string formatting overhead for every voice command. | Guard with `if (BuildConfig.DEBUG || debugLogging)` or use `PluginLog.d()` which already has a configurable threshold. |
| **Low** | `commonMain/.../marketplace/UpdateNotifier.kt` L64 | **Default `CoroutineScope` creates an unmanaged scope**: `scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())`. If the caller uses the default, this scope is never cancelled — it lives until the JVM exits. Background update-check coroutines will accumulate. | Remove the default. Require callers to provide the scope explicitly so its lifecycle is tied to the owning component (e.g., the application scope). |
| **Low** | `commonMain/.../security/PermissionManifestParser.kt` | **No test coverage for permission parsing edge cases**: Permission manifest parsing is security-sensitive but has no dedicated unit tests in commonTest or androidUnitTest. | Add tests for: empty rationale, duplicate permissions, malformed permission strings, and permissions that contain the delimiter character. |
| **Low** | `commonTest/.../integration/` | **Integration tests cover lifecycle and event bus but not security**: The 8 test files cover PluginLifecycle, EventBus, DataProvider, HandlerPlugin, Discovery, and Security integration. The `SecurityIntegrationTest` exists but should be checked to verify it covers: signature rejection on tampered packages, confidence threshold enforcement, and permission escalation detection. | Review `SecurityIntegrationTest.kt` and expand to cover the bypass cases identified in this review. |

---

## Recommendations

### Priority 1 — Fix Before Shipping (CRITICAL / HIGH)

1. **Fix the iOS `PermissionStorage` compile break immediately.** The iOS build cannot succeed in its current state. The `actual class PermissionStorage` must implement the same API as `expect class PermissionStorage`. File: `/Volumes/M-Drive/Coding/NewAvanues/Modules/PluginSystem/src/iosMain/kotlin/com/augmentalis/magiccode/plugins/security/PermissionStorage.kt`

2. **Make signature verification mandatory.** Change `PluginInstaller.installFromPackage()` to fail hard when signature verification fails. Expose a `PluginSystemConfig.enforceSignatureVerification: Boolean` flag defaulting to `true` so tests can bypass it, but production cannot. File: `/Volumes/M-Drive/Coding/NewAvanues/Modules/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/distribution/PluginInstaller.kt` L107–111

3. **Implement real Android persistence.** Replace `InMemoryPluginPersistence` in the `actual fun createDefaultPluginPersistence()` with a JSON file-based implementation using the existing `FileIO` abstraction. File: `/Volumes/M-Drive/Coding/NewAvanues/Modules/PluginSystem/src/androidMain/kotlin/com/augmentalis/magiccode/plugins/persistence/PluginPersistence.kt`

4. **Fix confidence threshold enforcement in `PluginHandlerBridge.dispatch()`.** The field `minPluginConfidence` is dead code relative to the actual dispatch path. File: `/Volumes/M-Drive/Coding/NewAvanues/Modules/PluginSystem/src/androidMain/kotlin/com/augmentalis/magiccode/plugins/android/PluginHandlerBridge.kt`

5. **Fix the `AndroidPluginHost` dispatcher from `Dispatchers.Main` to `Dispatchers.Default`.** ANR risk is real. File: `/Volumes/M-Drive/Coding/NewAvanues/Modules/PluginSystem/src/androidMain/kotlin/com/augmentalis/magiccode/plugins/android/AndroidPluginHost.kt`

6. **Connect `DefaultPluginSandbox` to `PermissionStorage`.** Currently granted permissions are ephemeral despite the KDoc claiming they are persisted. File: `/Volumes/M-Drive/Coding/NewAvanues/Modules/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/security/PluginSandbox.kt`

7. **Fix `MarketplaceDataProvider.startInstall()` to actually install packages.** After `api.download()` succeeds, call `PluginInstaller.installFromPackage()`. Remove the fake progress loop. File: `/Volumes/M-Drive/Coding/NewAvanues/Modules/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/marketplace/MarketplaceDataProvider.kt` L368–399

8. **Fix `PluginLifecycleManager.manage()` Job leak.** Store and cancel the state-collection Job when a plugin is unmanaged. File: `/Volumes/M-Drive/Coding/NewAvanues/Modules/PluginSystem/src/commonMain/kotlin/com/augmentalis/magiccode/plugins/universal/PluginLifecycleManager.kt` L69–84

### Priority 2 — Fix Before Plugin SDK Public Release (MEDIUM)

9. **Replace all `System.currentTimeMillis()` in commonMain** (5 occurrences across `PluginRegistry`, `PermissionEscalationDetector`, `SecurityAuditLogger`, `TransactionManager`, `UpdateNotifier`) with the module's own `currentTimeMillis()` expect function.

10. **Fix `ManifestValidator.validateVersionConstraint()`** to use `SemverConstraintValidator.satisfies()` instead of always returning `true`.

11. **Normalize paths in `PluginNamespace.isWithinNamespace()`** before calling `startsWith()` to prevent path traversal.

12. **Fix `SecurityAuditLogger` event buffer**: replace `MutableList.removeAt(0)` with `ArrayDeque.removeFirst()` for O(1) trimming. Fix handler invocation outside the lock to prevent deadlock.

13. **Fix the duplicate plugin registration code** in `AndroidPluginSystemSetup` — delegate to `BuiltinPluginRegistration.registerAll()` rather than duplicating 7 factories.

14. **Fix `AvuManifestParser` source classification** — do not infer `PluginSource` from the plugin ID prefix. Propagate source from the discovery layer.

15. **Fix `PermissionStorage` iOS storage security** — use Keychain rather than NSUserDefaults for permission grants.

16. **Thread-safety: convert `PluginPerformanceMonitor` internal maps** to use Mutex or concurrent data structures.

### Priority 3 — Quality Improvements (LOW)

17. Add `@Volatile` + double-checked locking to `PluginPerformanceMonitor._instance` singleton.
18. Use `AtomicLong` for `GrpcPluginEventBus.generateEventId()`.
19. Make `TrustStore` thread-safe.
20. Guard `AndroidPluginClassLoader` so multiple `loadClass()` calls with different paths create independent classloaders rather than reusing the first one.
21. Remove the default `CoroutineScope` from `UpdateNotifier` — require explicit scope.
22. Add streaming signature verification in `AndroidSignatureVerifier` to avoid OOM on large packages.
23. Expand `SecurityIntegrationTest` to cover the identified bypass cases.

---

## Architecture Observations

### Dual PluginState Enums
Two separate `PluginState` enums exist in the codebase:
- `com.augmentalis.magiccode.plugins.core.PluginState` — used by the persistence/registry layer, 8 values including `ENABLED/DISABLED`
- `com.augmentalis.magiccode.plugins.universal.PluginState` — used by `UniversalPlugin`/`BasePlugin`, different values (`ACTIVE`, `PAUSED`, `RESUMING`, `STOPPING`, etc.)

These are mapped between at the `AndroidPluginHost` boundary. This is a conceptual redundancy that could cause confusion. The `core.PluginState.ENABLED` does not appear in the `universal.PluginState` enum. Consider collapsing to one enum with a complete state machine.

### Positive Design Aspects
- **`SemverConstraintValidator`** is a clean, correct implementation of semantic version constraint matching including caret, tilde, range, and wildcard — well-tested conceptually.
- **`DependencyResolver`** topological sort with cycle detection is correct and handles optional dependencies properly.
- **`BasePlugin`** is an excellent SDK base class — the lifecycle state machine transitions are well-guarded, exception handling is comprehensive, and the hook pattern (`onInitialize`, `onShutdown`, etc.) is clean.
- **`ServiceRegistry`** provides both suspend and sync access paths with proper Mutex/synchronized split — this is a thoughtful design for the mixed-context Android environment.
- **`AndroidUIInteractionExecutor`** and **`AndroidNavigationExecutor`** have solid, production-quality implementations with proper `AccessibilityNodeInfo.recycle()` calls and `suspendCancellableCoroutine` for async gestures.
- **`SpeechEnginePluginInterface`** LSP contracts are the most thoroughly documented contracts in the module — exemplary interface design.
- **`FileSystemPluginDiscovery`** correctly uses the `currentTimeMillis()` expect function (unlike several other files in the same module).

---

## Test Coverage Assessment

**Existing tests (8 files):**
- `PluginLifecycleIntegrationTest` — lifecycle transitions
- `HandlerPluginIntegrationTest` — handler dispatch
- `EventBusIntegrationTest` — event routing
- `SecurityIntegrationTest` — security scenarios (needs expansion per above)
- `DiscoveryIntegrationTest` — plugin discovery
- `DataProviderIntegrationTest` — accessibility data
- `UniversalPluginIntegrationTest` — universal plugin lifecycle
- `TestUtils` — test helpers

**Coverage gaps:**
- No tests for `ManifestValidator.validateVersionConstraint()` (the broken code path)
- No tests for `PluginNamespace.isWithinNamespace()` path traversal
- No tests for `PluginHandlerBridge` confidence threshold behavior (because it doesn't work)
- No tests for `PluginInstaller` signature verification bypass
- No tests for `AvuManifestParser` source classification
- No tests for `PluginStateStorage` JSON parsing with special characters
- No tests for `PluginPerformanceMonitor` thread safety

---

## Finding Count Summary

| Severity | Count |
|----------|-------|
| Critical | 3 |
| High | 8 |
| Medium | 17 |
| Low | 10 |
| **Total** | **38** |
