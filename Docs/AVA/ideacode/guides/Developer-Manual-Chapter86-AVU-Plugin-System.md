# Developer Manual - Chapter 86: AVU Plugin System

**Date**: 2026-02-06
**Author**: Augmentalis Engineering
**Status**: Active

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

## Future: PluginLoader (Phase 4)

```kotlin
class PluginLoader(
    private val registry: PluginRegistry,
    private val interpreter: AvuInterpreter
) {
    fun loadPlugin(source: String): PluginLoadResult
    fun activatePlugin(pluginId: String): Boolean
    fun deactivatePlugin(pluginId: String): Boolean
    fun uninstallPlugin(pluginId: String): Boolean
}

sealed class PluginLoadResult {
    data class Success(val manifest: PluginManifest) : PluginLoadResult()
    data class ValidationError(val errors: List<String>) : PluginLoadResult()
    data class PermissionDenied(val required: List<String>) : PluginLoadResult()
}
```

---

## Future: PluginRegistry (Phase 4)

```kotlin
class PluginRegistry {
    fun register(manifest: PluginManifest)
    fun unregister(pluginId: String)
    fun getPlugin(pluginId: String): PluginManifest?
    fun getActivePlugins(): List<PluginManifest>
    fun getPluginsByPermission(permission: String): List<PluginManifest>
    fun isActive(pluginId: String): Boolean
    fun setActive(pluginId: String, active: Boolean)
}
```

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
