# Developer Manual - Chapter 86: AVU Plugin System

**Date**: 2026-02-06
**Author**: Augmentalis Engineering
**Status**: Active
**Implementation**: Phase 4 Complete

---

## Overview

The AVU Plugin System enables distributable, sandboxed extensions using `.avp` text files. Unlike traditional plugins that use DexClassLoader or dynamic code loading, AVU plugins are purely declarative text files interpreted by the AVU runtime. This makes them App Store compliant, inspectable, and safe to distribute through a marketplace.

This chapter supersedes Chapter 67 (Avanues Plugin Development).

---

## Plugin vs Workflow

| Aspect | .vos Workflow | .avp Plugin |
|--------|--------------|-------------|
| Purpose | System automation | Distributable extension |
| Distribution | Local only | Marketplace / sideload |
| Header `type:` | `workflow` | `plugin` |
| Requires `metadata:` | No | Yes (name, plugin_id, min_vos_version) |
| Requires `permissions:` | No | Yes (sandbox enforcement) |
| Requires `triggers:` | No | Yes (registers in DynamicCommandRegistry) |
| Code namespacing | No | Yes (plugin_id prefix) |
| User approval | Automatic | Manual permission grant |
| Isolation | System context | Sandboxed context |

---

## Plugin Lifecycle

```
DISCOVERY -> VALIDATION -> PERMISSION GRANT -> REGISTRATION -> ACTIVATION -> DEACTIVATION
```

### 1. DISCOVERY

Plugins are found from:
- **Bundled:** Pre-installed in `assets/plugins/`
- **Marketplace:** Downloaded from VoiceOS Plugin Marketplace
- **Sideloaded:** Manually imported `.avp` files (developer mode)

### 2. VALIDATION

The system validates the plugin structure:
- Parse header via `AvuHeader.parse()`
- Verify `schema: avu-2.2`
- Verify `type: plugin`
- Check required metadata: `name`, `plugin_id`, `min_vos_version`
- Validate declared codes against `AvuCodeRegistry`
- Parse body for syntax errors

### 3. PERMISSION GRANT

User is prompted with the plugin's declared permissions before activation. The plugin cannot execute without explicit approval of all requested permissions.

### 4. REGISTRATION

After permission grant:
- Codes registered with namespace prefix (`plugin_id:CODE`)
- Triggers registered in `DynamicCommandRegistry` with source tracking
- Plugin manifest stored in `PluginRegistry`

### 5. ACTIVATION

The interpreter starts with sandboxed config:
- `@on` handlers become active event listeners
- Plugin variables initialized
- Initialization handlers executed

### 6. DEACTIVATION

Cleanup when plugin is disabled or uninstalled:
- Event handlers detached
- Namespaced codes unregistered from `AvuCodeRegistry`
- Triggers unregistered from `DynamicCommandRegistry`
- Variables cleared, resources released

---

## Permission System

Plugins declare required permissions in their header:

| Permission | Description | Risk Level |
|-----------|-------------|------------|
| `GESTURES` | Perform tap, swipe, scroll | Medium |
| `APPS` | Launch and control apps | Medium |
| `SCREEN_READ` | Read screen content | High |
| `NOTIFICATIONS` | Access notifications | High |
| `SYSTEM_SETTINGS` | Modify device settings | High |
| `NETWORK` | Make network requests | High |
| `FILE_ACCESS` | Read/write files | Critical |
| `LOCATION` | Access device location | High |
| `CAMERA` | Access camera | Critical |
| `MICROPHONE` | Access microphone | Critical |
| `CONTACTS` | Access contacts | High |
| `CALENDAR` | Access calendar | Medium |

### Enforcement

The interpreter checks permissions before every code dispatch:

```kotlin
// Permission check before dispatch
val requiredPermission = CodePermissionMap.requiredPermissions(code)
if (requiredPermission.any { it !in manifest.permissions }) {
    return ExecutionResult.PermissionDenied(code, requiredPermission)
}
```

---

## Plugin Sandbox

Each plugin runs in isolated sandbox with:
- **Isolated variable scope** - no cross-plugin variable leaks
- **Namespaced code registration** - prevents name conflicts
- **Permission-limited dispatching** - only granted codes execute
- **Resource limits** from `SandboxConfig` (steps, time, loops, nesting, variables)

---

## Plugin Manifest

Derived from the `.avp` file header:

```kotlin
data class PluginManifest(
    val pluginId: String,           // e.g., "com.augmentalis.smartlogin"
    val name: String,               // Display name
    val version: String,            // Semver
    val minVosVersion: Int,         // Minimum VoiceOS version code
    val author: String?,
    val permissions: List<String>,
    val triggers: List<String>,
    val codes: Map<String, String>
)
```

---

## PluginLoader (Implemented - Phase 4)

**Package:** `com.augmentalis.voiceoscore.dsl.plugin`

The `PluginLoader` validates and loads `.avp` files through a 6-step pipeline:

1. **Parse** — Lex + parse via `AvuDslLexer` / `AvuDslParser`
2. **Type check** — Verify `type: plugin`
3. **Manifest extraction** — Header metadata → `PluginManifest`
4. **Manifest validation** — Required fields, valid identifiers
5. **Permission check** — Declared codes covered by declared permissions (via `CodePermissionMap`)
6. **Sandbox assignment** — Trust level → `SandboxConfig`

```kotlin
object PluginLoader {
    fun load(avpContent: String, trustLevel: PluginTrustLevel? = null): PluginLoadResult
}

sealed class PluginLoadResult {
    data class Success(val plugin: LoadedPlugin) : PluginLoadResult()
    data class ParseError(val errors: List<String>) : PluginLoadResult()
    data class ValidationError(val errors: List<String>) : PluginLoadResult()
    data class PermissionError(val errors: List<String>) : PluginLoadResult()

    val isSuccess: Boolean
    fun pluginOrNull(): LoadedPlugin?
}
```

### PluginManifest

Extracted from `.avp` file headers via `PluginManifest.fromHeader(header)`:

```kotlin
data class PluginManifest(
    val pluginId: String,           // e.g., "com.augmentalis.smartlogin"
    val name: String,               // Display name
    val version: String,            // Semver from header
    val minVosVersion: Int?,        // Minimum VoiceOS version code
    val author: String?,
    val description: String?,
    val codes: Map<String, String>,
    val permissions: Set<PluginPermission>,
    val triggers: List<String>
) {
    fun validate(): ManifestValidation
}
```

### PluginPermission

Cross-platform permission enum (15 permissions):

```kotlin
enum class PluginPermission(val displayName: String, val description: String) {
    GESTURES, APPS, NOTIFICATIONS, NETWORK, STORAGE, LOCATION,
    SENSORS, CAMERA, MICROPHONE, CONTACTS, CALENDAR, SMS,
    PHONE, ACCESSIBILITY, SYSTEM
}
```

### PluginState

Lifecycle state machine:

```
DISCOVERED → VALIDATED → REGISTERED → ACTIVE ⇄ INACTIVE
                              ↓                   ↓
                            ERROR               ERROR
```

### LoadedPlugin

Immutable plugin container with state transitions:

```kotlin
data class LoadedPlugin(
    val manifest: PluginManifest,
    val ast: AvuDslFile,
    val sandboxConfig: SandboxConfig,
    val state: PluginState = PluginState.VALIDATED,
    val errorMessage: String? = null,
    val loadedAtMs: Long = 0
) {
    fun withState(newState: PluginState, error: String? = null): LoadedPlugin
}
```

### PluginSandbox & Trust Levels

Trust-based sandbox configuration:

| Trust Level | Plugin ID Prefix | Sandbox Profile |
|------------|-----------------|-----------------|
| `SYSTEM` | `com.augmentalis.*`, `com.realwear.*` | `SandboxConfig.SYSTEM` (60s, 10K steps) |
| `VERIFIED` | Verified author | `SandboxConfig.DEFAULT` (10s, 1K steps) |
| `USER` | Default | Custom (8s, 800 steps) |
| `UNTRUSTED` | Unknown | `SandboxConfig.STRICT` (5s, 500 steps) |

```kotlin
object PluginSandbox {
    fun configForTrustLevel(level: PluginTrustLevel): SandboxConfig
    fun determineTrustLevel(manifest: PluginManifest): PluginTrustLevel
    fun addVerifiedAuthor(author: String)
}
```

---

## PluginRegistry (Implemented - Phase 4)

**Package:** `com.augmentalis.voiceoscore.dsl.plugin`

Central registry managing the full plugin lifecycle with exclusive trigger ownership:

```kotlin
class PluginRegistry(private val dispatcher: IAvuDispatcher) {
    fun register(plugin: LoadedPlugin): PluginRegistrationResult
    fun activate(pluginId: String): Boolean
    fun deactivate(pluginId: String): Boolean
    fun unregister(pluginId: String): Boolean
    suspend fun handleTrigger(pattern: String, captures: Map<String, String>): PluginTriggerResult
    fun findPluginForTrigger(pattern: String): LoadedPlugin?
    fun getPlugin(pluginId: String): LoadedPlugin?
    fun getAllPlugins(): List<LoadedPlugin>
    fun getActivePlugins(): List<LoadedPlugin>
    fun getRegisteredTriggers(): Map<String, String>
    fun getStatistics(): Map<PluginState, Int>
    fun clear()
}
```

### Trigger Routing

When `handleTrigger()` is called:
1. Look up trigger pattern → owning plugin ID
2. Verify plugin is in `ACTIVE` state
3. Create `AvuInterpreter` with plugin's `SandboxConfig`
4. Execute via `interpreter.handleTrigger(ast, pattern, captures)`
5. On failure, transition plugin to `ERROR` state

### Result Types

```kotlin
sealed class PluginRegistrationResult {
    data class Success(val plugin: LoadedPlugin)
    data class Error(val message: String)
    data class Conflict(val conflicts: List<String>)  // trigger ownership conflicts
}

sealed class PluginTriggerResult {
    data class Success(val pluginId: String, val returnValue: Any?, val executionTimeMs: Long)
    data class Error(val message: String)
    data class NoHandler(val pattern: String)
}
```

### Full Loading Pipeline

```kotlin
// Load, register, and activate a plugin
val result = PluginLoader.load(avpFileContent)
if (result is PluginLoadResult.Success) {
    val regResult = registry.register(result.plugin)
    if (regResult is PluginRegistrationResult.Success) {
        registry.activate(regResult.plugin.pluginId)
    }
}

// Handle a voice trigger
val triggerResult = registry.handleTrigger("note {text}", mapOf("text" to "buy milk"))
```

### File Layout

```
dsl/plugin/
├── PluginPermission.kt    (35 lines)  - 15-value permission enum
├── PluginManifest.kt      (67 lines)  - Manifest extraction + validation
├── PluginState.kt         (22 lines)  - 6-state lifecycle enum
├── LoadedPlugin.kt        (25 lines)  - Immutable plugin container
├── PluginSandbox.kt       (67 lines)  - Trust-based sandbox config
├── PluginLoader.kt       (100 lines)  - 6-step load/validate pipeline
└── PluginRegistry.kt     (184 lines)  - Lifecycle management + trigger routing
```

Total: ~500 lines across 7 files.

---

## Distribution

### Marketplace

Central repository with:
- Version management and automatic updates
- User ratings and reviews
- Plugin signing and verification
- Category browsing and search

### Sideload

Direct `.avp` file import (developer mode required). For beta testing, internal corporate plugins, personal automation.

### Bundled

System plugins shipped with VoiceOS in `assets/plugins/`. Pre-validated and pre-approved.

---

## Complete Plugin Example

```
---
schema: avu-2.2
version: 1.0.0
type: plugin
metadata:
  name: Quick Notes
  plugin_id: com.augmentalis.quicknotes
  min_vos_version: 40100
  author: Augmentalis
  description: Voice-activated note taking with quick recall
codes:
  VCM: Voice Command (id:action:params)
  AAC: Accessibility Action (id:actionType:targetAvid:params)
  SCR: Screen Read (id:targetAvid:readMode)
permissions:
  APPS
  SCREEN_READ
  NOTIFICATIONS
triggers:
  note {text}
  read notes
  find note {query}
---

@define save_note(text)
  VCM(id: "open", action: "launch", target: "com.google.keep")
  @wait 1500
  AAC(action: "CLICK", target: "new_note_btn")
  @wait 500
  AAC(action: "SET_TEXT", target: "note_body", text: $text)
  AAC(action: "CLICK", target: "save_btn")
  @log "Note saved: " + $text

@define read_all_notes()
  VCM(id: "open", action: "launch", target: "com.google.keep")
  @wait 1500
  SCR(target: "note_list", mode: "FULL")
  @log "Reading notes from screen"

@define search_notes(query)
  VCM(id: "open", action: "launch", target: "com.google.keep")
  @wait 1000
  AAC(action: "CLICK", target: "search_icon")
  @wait 500
  AAC(action: "SET_TEXT", target: "search_field", text: $query)
  @wait 1000
  SCR(target: "search_results", mode: "FULL")

@on "note {text}"
  save_note(text: $text)

@on "read notes"
  read_all_notes()

@on "find note {query}"
  search_notes(query: $query)
```

---

## Migration from PluginManager.kt

| Aspect | Old (PluginManager.kt) | New (.avp plugins) |
|--------|----------------------|-------------------|
| Format | APK-based via DexClassLoader | Text-based .avp files |
| Platform | Android only | KMP (all platforms) |
| Security | Executable code | Declarative text (no code loading) |
| App Store | Potential rejection | Fully compliant |
| Inspection | Decompilation required | Plain text, human readable |

`PluginManager.kt` is **DEPRECATED**. New plugin development should use `.avp` format exclusively.

---

## Best Practices

1. **Declare minimum permissions** - Only request what you actually use
2. **Include descriptive triggers** - Natural language patterns for voice activation
3. **Fill out metadata** - Name, description, author for marketplace discovery
4. **Test within sandbox limits** - Verify plugin works with default `SandboxConfig`
5. **Version with semver** - `major.minor.patch` for clear change tracking
6. **Keep plugins focused** - One plugin = one capability
7. **Handle errors with @if** - Check conditions before risky operations
8. **Document with comments** - Use `#` comments for usage instructions

---

## Related Documents

- [Ch67: Avanues Plugin Development](Developer-Manual-Chapter67-Avanues-Plugin-Development.md) (SUPERSEDED)
- [Ch81: AVU Protocol Overview](Developer-Manual-Chapter81-AVU-Protocol-Overview.md)
- [Ch83: AVU DSL Syntax](Developer-Manual-Chapter83-AVU-DSL-Syntax.md)
- [Ch84: AVU Code Registry](Developer-Manual-Chapter84-AVU-Code-Registry.md)
- [Ch85: AVU Runtime Interpreter](Developer-Manual-Chapter85-AVU-Runtime-Interpreter.md)
- [Ch87: AVU Migration Guide](Developer-Manual-Chapter87-AVU-Migration-Guide.md)

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-02-06 | Initial chapter |
| 2.0 | 2026-02-06 | Updated: PluginLoader, PluginRegistry, PluginSandbox, LoadedPlugin, PluginState now implemented (Phase 4) |
