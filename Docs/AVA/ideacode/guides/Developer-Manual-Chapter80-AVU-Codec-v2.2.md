# Developer Manual - Chapter 80: AVU Codec v2.2

**Date**: 2026-02-03
**Author**: Augmentalis Engineering
**Status**: Active

---

## Overview

AVU (Avanues Universal Format) v2.2 introduces unified escape utilities, self-documenting headers, and a centralized code registry. This chapter documents the new `avucodec.core` package and migration guidance.

## Key Changes in v2.2

| Change | Description |
|--------|-------------|
| **Unified Escape** | Single `AvuEscape` implementation for all modules |
| **Code Registry** | `AvuCodeRegistry` for runtime code registration and legend generation |
| **Self-Documenting Headers** | Optional `codes:` section in file headers |
| **RPC Terminology** | IPC renamed to RPC (Remote Procedure Call) throughout |

## Package Structure

```
com.augmentalis.avucodec/
  ├── AVUEncoder.kt          # Existing encoder (uses AvuEscape)
  ├── AVUDecoder.kt          # Existing decoder (uses AvuEscape)
  └── core/
      ├── AvuEscape.kt       # Canonical escape/unescape utilities
      ├── AvuCodeInfo.kt     # Code metadata definitions
      ├── AvuCodeRegistry.kt # Runtime code registry
      └── AvuHeader.kt       # Self-documenting header support
```

## AvuEscape - Unified Escape Utilities

### The Problem

Previously, escape/unescape was duplicated across 7+ modules with inconsistencies:
- Some used percent-encoding (`%3A`)
- Some used backslash-encoding (`\:`)

This caused parsing failures when messages crossed module boundaries.

### The Solution

`AvuEscape` is the single source of truth for AVU format escaping:

```kotlin
import com.augmentalis.avucodec.core.AvuEscape

// Escape a value containing reserved characters
val url = "https://example.com:8080/path?query=value"
val escaped = AvuEscape.escape(url)
// Result: "https%3A//example.com%3A8080/path?query=value"

// Unescape back to original
val original = AvuEscape.unescape(escaped)
// Result: "https://example.com:8080/path?query=value"

// Check if escaping is needed (optimization)
if (AvuEscape.needsEscaping(value)) {
    value = AvuEscape.escape(value)
}
```

### Escape Sequences

| Character | Encoded | Order |
|-----------|---------|-------|
| `%` | `%25` | Escape FIRST / Unescape LAST |
| `:` | `%3A` | Field delimiter |
| `\n` | `%0A` | Line feed |
| `\r` | `%0D` | Carriage return |

### Migration

Replace all local escape implementations:

```kotlin
// Before (in each module)
private fun escape(text: String): String = text
    .replace("\\", "\\\\")  // WRONG: backslash escaping
    .replace(":", "\\:")
    .replace("\n", "\\n")

// After (use shared utility)
import com.augmentalis.avucodec.core.AvuEscape

fun escape(text: String): String = AvuEscape.escape(text)
```

## AvuCodeRegistry - Code Registration

### Purpose

The registry enables:
- Runtime code lookup and validation
- Self-documenting file headers with code legends
- Documentation generation
- AI/LLM understanding of message formats

### Registering Codes

```kotlin
import com.augmentalis.avucodec.core.AvuCodeRegistry
import com.augmentalis.avucodec.core.AvuCodeInfo
import com.augmentalis.avucodec.core.AvuCodeCategory

// Register at module initialization
AvuCodeRegistry.register(AvuCodeInfo(
    code = "SCR",
    name = "Sync Create",
    category = AvuCodeCategory.SYNC,
    format = "msgId:entityType:entityId:version:data",
    description = "Create a new entity on remote server",
    example = "SCR:msg_001:TAB:tab_001:1:escaped_data"
))

// Register multiple codes
AvuCodeRegistry.registerAll(
    AvuCodeInfo(code = "SUP", name = "Sync Update", ...),
    AvuCodeInfo(code = "SDL", name = "Sync Delete", ...)
)
```

### Code Categories

```kotlin
enum class AvuCodeCategory {
    CORE,      // ACC, DEC, ERR, HND, PNG, PON
    VOICE,     // VCM, STT, ELM, CMD
    SYNC,      // SCR, SUP, SDL, SBT
    RPC,       // MTH, MRS, MRE, QRY
    MEDIA,     // VCA, FTR, SSO, WBS
    PLUGIN,    // PLG, DEP, PRM, HKS
    BROWSER,   // URL, NAV, TAB, PLD
    AI,        // AIQ, AIR
    SYSTEM,    // CON, DIS, CAP
    CUSTOM     // User-defined codes
}
```

### Generating Legends

```kotlin
// Generate legend for specific codes
val legend = AvuCodeRegistry.generateLegend(
    filter = setOf("SCR", "SUP", "SDL"),
    includeDescriptions = true
)
// Output:
// codes:
//   SCR: Sync Create (msgId:entityType:entityId:version:data) - Create a new entity on remote server
//   SDL: Sync Delete (msgId:entityType:entityId)
//   SUP: Sync Update (msgId:entityType:entityId:version:data)

// Generate documentation for all codes
val docs = AvuCodeRegistry.generateDocumentation()
```

### Validating Messages

```kotlin
val result = AvuCodeRegistry.validate("SCR:msg_001:TAB:tab_001:1:data")
when (result) {
    is ValidationResult.Valid -> println("Code: ${result.info.name}")
    is ValidationResult.Invalid -> println("Error: ${result.reason}")
    is ValidationResult.UnknownCode -> println("Unknown: ${result.code}")
}
```

## AvuHeader - Self-Documenting Files

### Purpose

AVU files can include a `codes:` section that documents the codes used, making files self-documenting for humans and AI.

### Generating Headers

```kotlin
import com.augmentalis.avucodec.core.AvuHeader

// Generate complete header with codes legend
val header = AvuHeader.generate(
    type = "WebSocket Sync",
    version = "1.0.0",
    project = "webavanue",
    metadata = mapOf("file" to "sync.avu", "count" to "42"),
    codes = setOf("SCR", "SUP", "SDL", "PNG", "PON"),
    includeDescriptions = false
)

// Output:
// # Avanues Universal Format v2.2
// # Type: WebSocket Sync
// ---
// schema: avu-2.2
// version: 1.0.0
// project: webavanue
// metadata:
//   file: sync.avu
//   count: 42
// codes:
//   PNG: Ping (sessionId:timestamp)
//   PON: Pong (sessionId:timestamp)
//   SCR: Sync Create (msgId:entityType:entityId:version:data)
//   SDL: Sync Delete (msgId:entityType:entityId)
//   SUP: Sync Update (msgId:entityType:entityId:version:data)
// ---
```

### Parsing Headers

```kotlin
val fileContent = """
# Avanues Universal Format v2.2
# Type: Learned App
---
schema: avu-2.2
version: 1.0.0
codes:
  APP: App Metadata (package:name:timestamp)
  ELM: Element (avid:label:type:actions:bounds:category)
---
APP:com.example:Example:1706000000
ELM:btn1:Click Me:BUTTON:click:0,0,100,50:action
""".trimIndent()

val (header, bodyStart) = AvuHeader.parse(fileContent)

println(header.schema)      // avu-2.2
println(header.version)     // 1.0.0
println(header.type)        // Learned App
println(header.codes)       // {APP=App Metadata..., ELM=Element...}

// Extract just the body
val body = AvuHeader.extractBody(fileContent)
// APP:com.example:Example:1706000000
// ELM:btn1:Click Me:BUTTON:click:0,0,100,50:action
```

### Wire Protocol vs File Format

| Use Case | Include Header? | Reason |
|----------|-----------------|--------|
| **File formats** (.vos, .avu) | Yes | Self-documenting |
| **Wire protocol** (WebSocket, RPC) | No | Overhead |

For wire protocol, use `HND` (Handshake) to exchange schema version:
```
HND:sess_001:com.app:2.2.0:android
```

## Module Dependencies

Add AVUCodec as a dependency to use shared utilities:

```kotlin
// build.gradle.kts
dependencies {
    implementation(project(":Modules:AVUCodec"))
}
```

Modules should register their codes at initialization:

```kotlin
// In WebSocket module
object WebSocketCodes {
    fun register() {
        AvuCodeRegistry.registerAll(
            AvuCodeInfo(code = "SCR", name = "Sync Create", ...),
            AvuCodeInfo(code = "SUP", name = "Sync Update", ...),
            AvuCodeInfo(code = "SDL", name = "Sync Delete", ...)
        )
    }
}

// Call during module initialization
WebSocketCodes.register()
```

## Best Practices

### 1. Always Use AvuEscape

```kotlin
// Good
val message = "VCM:${AvuEscape.escape(commandId)}:${AvuEscape.escape(action)}"

// Bad - direct string concatenation with reserved chars
val message = "VCM:$commandId:$action"  // Will break if commandId contains ':'
```

### 2. Register Codes Early

```kotlin
// In module initialization or Application.onCreate()
fun initializeModule() {
    registerAvuCodes()
    // ... other init
}
```

### 3. Validate Unknown Messages

```kotlin
fun handleMessage(message: String) {
    when (val result = AvuCodeRegistry.validate(message)) {
        is ValidationResult.Valid -> process(message, result.info)
        is ValidationResult.UnknownCode -> {
            // Log but don't fail - forward compatibility
            logger.warn("Unknown AVU code: ${result.code}")
            processGeneric(message)
        }
        is ValidationResult.Invalid -> {
            logger.error("Invalid message: ${result.reason}")
        }
    }
}
```

### 4. Include Headers in Exported Files

```kotlin
fun exportData(data: List<Item>): String = buildString {
    append(AvuHeader.generate(
        type = "Data Export",
        version = "1.0.0",
        codes = setOf("ITM", "META")
    ))
    data.forEach { item ->
        appendLine("ITM:${AvuEscape.escape(item.id)}:${AvuEscape.escape(item.name)}")
    }
}
```

## Related Documents

- [AVU Universal Format Specification v2.2](../../VoiceOS/Technical/specifications/AVU-Universal-Format-Spec-260122-V2.md)
- [Chapter 37: Universal Format v2.0](Developer-Manual-Chapter37-Universal-Format-v2.0.md)
- [AVUCodec Module Source](../../../../Modules/AVUCodec/src/commonMain/kotlin/com/augmentalis/avucodec/)

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-02-03 | Initial chapter for AVU Codec v2.2 |
