# Chapter 76: RPC Module Architecture

**Date:** 2026-02-02
**Author:** VOS4 Development Team
**Status:** Active

---

## Overview

This chapter documents the RPC (Remote Procedure Call) module architecture following the rename from `UniversalRPC` to `Rpc` and the standardization of IPC to RPC naming across the codebase.

## Module Rename Summary

### What Changed

| Before | After |
|--------|-------|
| `Modules/UniversalRPC` | `Modules/Rpc` |
| `com.augmentalis.universalrpc` | `com.augmentalis.rpc` |
| `AppIPCRegistry` | `AppRpcRegistry` |
| `UniversalIPCEncoder` | `RpcEncoder` |
| `*.IPC.COMMAND` | `*.RPC.COMMAND` |

### Why RPC Instead of IPC

- **RPC** (Remote Procedure Call) better describes the request-response pattern
- Consistent naming with cross-platform RPC standards (gRPC, Wire)
- Future-proof for KMP (Kotlin Multiplatform) across Android, iOS, Desktop
- Clearer API semantics

---

## Architecture

### Module Structure

```
Modules/Rpc/
├── build.gradle.kts              # KMP build configuration
├── Common/proto/                 # Protocol buffer definitions
│   ├── ava.proto
│   ├── cockpit.proto
│   ├── cursor.proto
│   ├── exploration.proto
│   ├── nlu.proto
│   ├── plugin.proto
│   └── webavanue.proto
├── src/
│   ├── commonMain/               # Shared KMP code
│   │   └── kotlin/com/augmentalis/rpc/
│   │       ├── RpcEncoder.kt     # Protocol encoder
│   │       ├── IRpcService.kt    # Service interface
│   │       ├── ServiceRegistry.kt
│   │       ├── cursor/           # VoiceCursor service
│   │       ├── nlu/              # NLU service
│   │       ├── exploration/      # Exploration service
│   │       └── plugin/           # Plugin service
│   ├── androidMain/              # Android-specific
│   ├── iosMain/                  # iOS-specific
│   └── desktopMain/              # Desktop-specific
├── android/                      # Android gRPC implementations
├── desktop/                      # Desktop gRPC implementations
└── web/                          # Web client implementations
```

### RpcEncoder

The `RpcEncoder` class provides cross-platform message encoding:

```kotlin
import com.augmentalis.rpc.RpcEncoder

val encoder = RpcEncoder()

// Encode voice command
val message = encoder.encodeVoiceCommand(
    commandId = "cmd123",
    action = "SCROLL_TOP",
    params = mapOf("speed" to "fast")
)
// Result: "VCM:cmd123:SCROLL_TOP:speed=fast"
```

### AppRpcRegistry

Registry for app-specific RPC actions:

```kotlin
import com.augmentalis.voiceoscore.managers.commandmanager.routing.AppRpcRegistry

// Get RPC action for an app
val action = AppRpcRegistry.getRpcAction("com.augmentalis.webavanue")
// Result: "com.augmentalis.webavanue.RPC.COMMAND"

// Register a new app
AppRpcRegistry.register(
    packageName = "com.augmentalis.myapp",
    rpcAction = "com.augmentalis.myapp.RPC.COMMAND",
    appName = "MyApp",
    appType = AppType.UTILITY,
    description = "My custom app"
)
```

---

## RPC Action Strings

Each app in the Avanues ecosystem has a unique RPC action:

| App | Package | RPC Action |
|-----|---------|------------|
| Avanues | `com.augmentalis.avanues` | `com.augmentalis.avanues.RPC.COMMAND` |
| WebAvanue | `com.augmentalis.webavanue` | `com.augmentalis.webavanue.RPC.COMMAND` |
| AVA AI | `com.augmentalis.ava` | `com.augmentalis.ava.RPC.COMMAND` |
| AvaConnect | `com.augmentalis.avaconnect` | `com.augmentalis.avaconnect.RPC.COMMAND` |

---

## Protocol Codes

The RPC protocol uses 3-letter codes:

### Voice Commands
- `VCM` - Voice Command
- `STT` - Speech to Text
- `TTS` - Text to Speech

### Responses
- `ACC` - Accept
- `DEC` - Decline
- `ERR` - Error

### Browser
- `URL` - URL share
- `NAV` - Navigate
- `TAB` - Tab operation

### AI
- `AIQ` - AI Query
- `AIR` - AI Response

---

## Integration Example

### Sending Commands Between Apps

```kotlin
// In VoiceOS CommandManager
suspend fun executeExternalCommand(
    command: Command,
    targetApp: String,
    params: Map<String, Any> = emptyMap()
) {
    // Get app-specific RPC action
    val rpcAction = AppRpcRegistry.getRpcActionWithFallback(targetApp)

    // Encode command
    val rpcMessage = rpcEncoder.encodeVoiceCommand(
        commandId = command.id,
        action = command.id.uppercase(),
        params = params
    )

    // Send via Intent broadcast
    val intent = Intent(rpcAction).apply {
        setPackage(targetApp)
        putExtra(RpcEncoder.EXTRA_SOURCE_APP, context.packageName)
        putExtra(RpcEncoder.EXTRA_MESSAGE, rpcMessage)
    }
    context.sendBroadcast(intent)
}
```

### Receiving Commands in WebAvanue

```kotlin
class WebAvanueRpcReceiver : BroadcastReceiver() {

    private val encoder = RpcEncoder()

    override fun onReceive(context: Context, intent: Intent) {
        val message = intent.getStringExtra(RpcEncoder.EXTRA_MESSAGE) ?: return
        val sourceApp = intent.getStringExtra(RpcEncoder.EXTRA_SOURCE_APP)

        // Validate and process
        if (encoder.isValidMessage(message)) {
            val code = encoder.extractCode(message)
            when (code) {
                "VCM" -> handleVoiceCommand(message)
                "NAV" -> handleNavigate(message)
                else -> Log.w(TAG, "Unknown code: $code")
            }
        }
    }
}
```

---

## Migration Guide

### From UniversalRPC to Rpc

1. **Update imports:**
```kotlin
// Before
import com.augmentalis.universalrpc.client.UniversalClient

// After
import com.augmentalis.rpc.client.UniversalClient
```

2. **Update dependencies:**
```kotlin
// build.gradle.kts
// Before
implementation(project(":Modules:UniversalRPC"))

// After
implementation(project(":Modules:Rpc"))
```

3. **Update encoder references:**
```kotlin
// Before
val encoder = UniversalIPCEncoder()

// After
val encoder = RpcEncoder()
```

### From IPC to RPC naming

1. **Update registry usage:**
```kotlin
// Before
AppIPCRegistry.getIPCAction(packageName)

// After
AppRpcRegistry.getRpcAction(packageName)
```

2. **Update action strings in manifests:**
```xml
<!-- Before -->
<action android:name="com.augmentalis.myapp.IPC.COMMAND"/>

<!-- After -->
<action android:name="com.augmentalis.myapp.RPC.COMMAND"/>
```

---

## Benefits

1. **Consistency** - Single naming convention across all modules
2. **Cross-Platform** - KMP support for Android, iOS, Desktop
3. **Type Safety** - Strongly typed protocol messages
4. **Efficiency** - ~60% smaller than JSON equivalents
5. **Extensibility** - Easy to add new protocol codes

---

## Related Documentation

- Chapter 37: Universal Format v2.0
- Chapter 67: Avanues Plugin Development
- Chapter 75: StateFlow Utilities

---

## Changelog

| Date | Change |
|------|--------|
| 2026-02-02 | Renamed UniversalRPC → Rpc, IPC → RPC |
| 2025-12-28 | Initial UniversalRPC module creation |
