# PluginSystem Fix: DexClassLoader → AVU Text Interpreter Dispatch

**Date:** 260222
**Module:** Modules/PluginSystem
**Severity:** Critical (architecture mismatch — DexClassLoader requires compiled DEX; .avp plugins are plain text)
**Branch:** VoiceOS-1M-SpeechEngine

---

## Problem

`PluginClassLoader` (Android actual) used `dalvik.system.DexClassLoader` to load plugins from APK/JAR files containing compiled DEX bytecode. The JVM actual used `java.net.URLClassLoader` for JARs. The iOS actual used a static Kotlin factory registry.

All three were incompatible with the stated plugin format: `.avp` text files interpreted by the AVU DSL runtime (`AvuInterpreter`).

### Root Cause

`PluginClassLoader` was written assuming a JVM-style bytecode plugin model. The `.avp` format decision was made later as part of the AVU DSL evolution, but `PluginClassLoader` was never updated to match.

---

## Fix Summary

### 1. `Modules/PluginSystem/build.gradle.kts`
Added `implementation(project(":Modules:AVU"))` to `commonMain` dependencies.
`PluginLoader` (AVU), `AvuDslLexer`, `AvuDslParser`, `LoadedPlugin` are now accessible across all platforms.

### 2. `commonMain/platform/PluginClassLoader.kt` (expect)
- Added `fun getLoadedPlugin(pluginId: String): LoadedPlugin?` to the expect class
- Updated KDoc to reflect .avp text loading model (removed all bytecode references)

### 3. `androidMain/platform/PluginClassLoader.kt` (actual)
Replaced `DexClassLoader` entirely:
- Reads `.avp` text via `java.io.File.readText()`
- Runs through `com.avanues.avu.dsl.plugin.PluginLoader.load(avpContent)`
- Verifies `plugin.manifest.pluginId == entrypoint` (manifest ID must match caller's expected ID)
- Stores `LoadedPlugin` in `loadedPlugins` map keyed by `entrypoint`
- Throws `ClassNotFoundException` for missing files; `IllegalArgumentException` for parse/validation/permission errors
- `getLoadedPlugin(pluginId)` retrieves stored `LoadedPlugin` for later execution by `AvuInterpreter`

### 4. `jvmMain/platform/PluginClassLoader.kt` (actual)
Same as Android but uses `java.io.File`. Removes `URLClassLoader` entirely.

### 5. `iosMain/platform/PluginClassLoader.kt` (actual)
Replaces the static Kotlin factory registry with real `.avp` text file loading:
- Reads `.avp` via `NSData.dataWithContentsOfFile` + `NSString` UTF-8 decode
- Runs through same `PluginLoader.load()` AVU pipeline
- iOS plugins can now be either bundled in the app bundle or placed in Documents/ at runtime
- `NSFileManager.defaultManager.fileExistsAtPath()` used for existence check

### 6. `commonMain/security/PluginSandbox.kt` — `DefaultPluginSandbox`
Wired `PermissionStorage` (encrypted AES256-GCM, expect/actual):
- New constructor: `DefaultPluginSandbox(storage: PermissionStorage? = null, auditLogger: ...)`
- **Write-through on grant:** `grantPermission()` writes `permission.name` to `storage.savePermission()`
- **Write-through on revoke:** `revokePermission()` calls `storage.revokePermission()`; `revokeAllPermissions()` calls `storage.clearAllPermissions()`
- **Lazy load per plugin:** `loadForPlugin(pluginId)` loads all persisted permissions from storage into in-memory map. Call this after each plugin is registered so permissions are hot before first `checkPermission()` call.
- Storage failures are caught and logged — they never cause a permission check to throw. The in-memory grant is still applied on storage write failure (fail-open for grants, fail-secure for checks).
- Existing no-arg `DefaultPluginSandbox()` usage in tests continues to compile unchanged (both params are optional).

### 7. `androidMain/integration/PluginSystemSetup.android.kt`
Added `val pluginSandbox: DefaultPluginSandbox` lazy property on `AndroidPluginSystemSetup`:
- Creates `PermissionStorage.create(context)` (hardware-backed AES256-GCM)
- Passes it to `DefaultPluginSandbox(storage = storage)`
- Callers access `setup.pluginSandbox` to get the persisted, encrypted sandbox instance

---

## Architecture After Fix

```
.avp file (text)
    │
    ▼
PluginClassLoader.loadClass(pluginId, avpFilePath)
    │
    ├─ File.readText() / NSData  (platform)
    │
    ▼
com.avanues.avu.dsl.plugin.PluginLoader.load(avpContent)
    │
    ├─ AvuDslLexer.tokenize()
    ├─ AvuDslParser.parse()
    ├─ PluginManifest.fromHeader() + validate()
    ├─ CodePermissionMap.validateCodePermissions()
    └─ PluginSandbox.configForTrustLevel()
    │
    ▼
LoadedPlugin (manifest + AST + SandboxConfig)
    │
    ├─ Stored in PluginClassLoader.loadedPlugins[pluginId]
    │
    ▼
Later: AvuInterpreter(dispatcher, loadedPlugin.sandboxConfig)
           .handleTrigger(loadedPlugin.ast, triggerPattern, captures)
```

---

## Files Changed

| File | Change |
|------|--------|
| `Modules/PluginSystem/build.gradle.kts` | Added `:Modules:AVU` dependency |
| `src/commonMain/.../platform/PluginClassLoader.kt` | Added `getLoadedPlugin()`; updated expect signature |
| `src/androidMain/.../platform/PluginClassLoader.kt` | Replaced DexClassLoader with AVU PluginLoader |
| `src/jvmMain/.../platform/PluginClassLoader.kt` | Replaced URLClassLoader with AVU PluginLoader |
| `src/iosMain/.../platform/PluginClassLoader.kt` | Replaced static registry with AVU PluginLoader + NSData file I/O |
| `src/commonMain/.../security/PluginSandbox.kt` | Added `PermissionStorage` write-through + `loadForPlugin()` |
| `src/androidMain/.../integration/PluginSystemSetup.android.kt` | Added `pluginSandbox` lazy property with encrypted storage |

---

## Backward Compatibility

- `DefaultPluginSandbox()` (no args) still compiles — tests unaffected
- `DefaultPluginSandbox(auditLogger = ...)` still compiles — named arg form unaffected
- `PluginLoader.loadPlugin()` call site unchanged — `classLoader.loadClass(manifest.entrypoint, libraryPath)` signature preserved; return value was already discarded
- `PluginClassLoader.unload()` still works (clears the in-memory map)
