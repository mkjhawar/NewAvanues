# Developer Manual - Chapter 84: AVU Code Registry

**Date**: 2026-02-06
**Author**: Augmentalis Engineering
**Status**: Active

---

## Overview

The `AvuCodeRegistry` is the central registry for all 3-letter AVU codes. It provides registration, lookup, validation, legend generation, and documentation. All modules register their codes at initialization time, enabling runtime validation and self-documenting file headers.

**Package:** `com.augmentalis.avucodec.core`

---

## AvuCodeInfo

The complete definition of an AVU code:

```kotlin
data class AvuCodeInfo(
    val code: String,                       // 3 uppercase chars (e.g., "VCM")
    val name: String,                       // Human-readable name (e.g., "Voice Command")
    val category: AvuCodeCategory,          // Category for organization
    val format: String,                     // Field format (e.g., "requestId:commandText:context")
    val description: String = "",           // Detailed description
    val fields: List<AvuFieldDef> = emptyList(),  // Field definitions
    val example: String? = null,            // Example message
    val since: String = "1.0"              // Version introduced
)
```

### Example

```kotlin
val voiceCommandInfo = AvuCodeInfo(
    code = "VCM",
    name = "Voice Command",
    category = AvuCodeCategory.VOICE,
    format = "requestId:commandText:context",
    description = "Represents a voice command from the user",
    fields = listOf(
        AvuFieldDef("requestId", AvuFieldType.STRING, required = true,
                    description = "Unique request identifier"),
        AvuFieldDef("commandText", AvuFieldType.STRING, required = true,
                    description = "The spoken command text"),
        AvuFieldDef("context", AvuFieldType.JSON, required = false,
                    description = "Optional context metadata")
    ),
    example = "VCM:req_001:scroll down:app=chrome",
    since = "1.0"
)
```

---

## AvuCodeCategory

Organizes codes into logical groups:

| Category | Display Name | Description |
|----------|-------------|-------------|
| `CORE` | Core Protocol | System-level operations (HBT, ACK, ERR) |
| `VOICE` | Voice & Audio | Voice commands and audio (VCM, VAR) |
| `SYNC` | Synchronization | Data sync operations (SCR, SUP, SDL) |
| `RPC` | Remote Procedure Call | RPC messages |
| `MEDIA` | Media Control | Audio/video playback |
| `PLUGIN` | Plugin Management | Plugin lifecycle |
| `BROWSER` | Browser Control | Web browser operations |
| `AI` | AI & ML | AI/NLU operations |
| `SYSTEM` | System Control | Device system operations |
| `CUSTOM` | Custom/Extension | User-defined codes |

---

## AvuFieldDef and AvuFieldType

Field definitions specify message structure:

```kotlin
data class AvuFieldDef(
    val name: String,
    val type: AvuFieldType,
    val required: Boolean = true,
    val description: String = ""
)

enum class AvuFieldType {
    STRING,      // Text data
    INT,         // 32-bit integer
    LONG,        // 64-bit integer
    FLOAT,       // Floating point
    BOOLEAN,     // true/false ("1"/"0" on wire)
    TIMESTAMP,   // ISO-8601 timestamp
    ENUM,        // Enumerated value
    JSON,        // JSON object/array
    DATA         // Binary/base64 data
}
```

---

## Registration

### Single Code

```kotlin
AvuCodeRegistry.register(AvuCodeInfo(
    code = "VCM",
    name = "Voice Command",
    category = AvuCodeCategory.VOICE,
    format = "requestId:commandText:context"
))
```

Throws `IllegalArgumentException` if the code is already registered with a different definition. Re-registering the same definition is safe (idempotent).

### Batch Registration

```kotlin
AvuCodeRegistry.registerAll(
    AvuCodeInfo(code = "HBT", name = "Heartbeat",
        category = AvuCodeCategory.CORE, format = "timestamp"),
    AvuCodeInfo(code = "ACK", name = "Acknowledgment",
        category = AvuCodeCategory.CORE, format = "messageId"),
    AvuCodeInfo(code = "ERR", name = "Error",
        category = AvuCodeCategory.CORE, format = "code:message:details")
)
```

### Module Initialization Pattern

```kotlin
object VoiceCoreModule {
    init {
        AvuCodeRegistry.registerAll(
            AvuCodeInfo(code = "VCM", name = "Voice Command",
                category = AvuCodeCategory.VOICE,
                format = "requestId:commandText:context"),
            AvuCodeInfo(code = "AAC", name = "Accessibility Action",
                category = AvuCodeCategory.CORE,
                format = "id:actionType:targetAvid:params")
        )
    }
}
```

---

## Lookup

```kotlin
// Get code info by code
val info: AvuCodeInfo? = AvuCodeRegistry.get("VCM")

// Get all codes in a category
val voiceCodes: List<AvuCodeInfo> = AvuCodeRegistry.getByCategory(AvuCodeCategory.VOICE)

// Get all registered codes
val allCodes: Map<String, AvuCodeInfo> = AvuCodeRegistry.getAll()

// Check if registered
val exists: Boolean = AvuCodeRegistry.isRegistered("VCM")

// Total count
val total: Int = AvuCodeRegistry.count()
```

---

## Validation

The registry validates incoming wire protocol messages:

```kotlin
sealed class ValidationResult {
    data class Valid(val info: AvuCodeInfo) : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
    data class UnknownCode(val code: String) : ValidationResult()
}
```

### Validation Checks

1. Message length (minimum 4 characters: `CODE`)
2. Code format (exactly 3 uppercase letters)
3. Code registered in the registry
4. Required field count satisfied

### Usage

```kotlin
when (val result = AvuCodeRegistry.validate("VCM:cmd_01:scroll down")) {
    is ValidationResult.Valid -> {
        val info = result.info
        // Parse and process message
    }
    is ValidationResult.Invalid -> {
        println("Invalid: ${result.reason}")
    }
    is ValidationResult.UnknownCode -> {
        println("Unknown code: ${result.code}")
    }
}
```

### System Boundary Pattern

Always validate at entry points:

```kotlin
class AvuMessageReceiver {
    fun receive(rawMessage: String) {
        when (val result = AvuCodeRegistry.validate(rawMessage)) {
            is ValidationResult.Valid -> routeMessage(rawMessage, result.info)
            is ValidationResult.Invalid -> logger.warn("Rejected: ${result.reason}")
            is ValidationResult.UnknownCode -> logger.warn("Unknown: ${result.code}")
        }
    }
}
```

---

## Legend Generation

### Full Legend (grouped by category)

```kotlin
val legend = AvuCodeRegistry.generateLegend(
    filter = setOf("VCM", "AAC", "SCR"),
    includeDescriptions = true
)
```

Output:

```
codes:
  # Voice & Audio
  VCM: Voice Command (requestId:commandText:context)
  # Core Protocol
  AAC: Accessibility Action (id:actionType:targetAvid:params)
  SCR: Screen Read (id:targetAvid:readMode)
```

### Compact Legend

```kotlin
val compact = AvuCodeRegistry.generateCompactLegend(setOf("VCM", "AAC"))
```

Output:

```
codes:
  VCM: requestId:commandText:context
  AAC: id:actionType:targetAvid:params
```

### Full Documentation

```kotlin
val docs = AvuCodeRegistry.generateDocumentation()
// Produces markdown table with all codes grouped by category
```

---

## Namespace Support (Implemented)

**Package:** `com.augmentalis.voiceoscore.dsl.registry`

Plugin codes are namespaced to prevent conflicts between plugins. The `AvuCodeNamespace` class handles parsing and qualification:

### AvuCodeNamespace

```kotlin
object AvuCodeNamespace {
    fun parse(qualified: String): NamespacedCode   // "com.example:VCM" -> NamespacedCode
    fun qualify(namespace: String, code: String): String  // -> "com.example:VCM"
    fun isNamespaced(code: String): Boolean         // "com.example:VCM" -> true, "VCM" -> false
    fun extractCode(qualified: String): String      // "com.example:VCM" -> "VCM"
}

data class NamespacedCode(val namespace: String, val code: String) {
    val qualified: String  // "com.example:VCM"
    val isSystem: Boolean  // true if namespace == "system"
}
```

### Examples

```kotlin
// System code (bare)
val vcm = AvuCodeNamespace.parse("VCM")  // NamespacedCode("system", "VCM")

// Plugin code (namespaced)
val pluginVcm = AvuCodeNamespace.parse("com.example.plugin:VCM")
// -> NamespacedCode("com.example.plugin", "VCM")

// Same code, different namespace = no conflict
"com.augmentalis.smartlogin:VCM"  // OK
"com.example.other:VCM"           // OK - different namespace
```

### CodePermissionMap (Implemented)

Maps 30+ AVU codes to required `PluginPermission` sets:

```kotlin
object CodePermissionMap {
    fun permissionsForCode(code: String): Set<PluginPermission>
    fun validateCodePermissions(
        declaredCodes: Set<String>,
        grantedPermissions: Set<PluginPermission>
    ): CodePermissionValidation

    fun registerCustomMapping(code: String, permissions: Set<PluginPermission>)
}
```

Built-in mappings include:
- `AAC` -> `GESTURES`
- `SCR` -> `ACCESSIBILITY`
- `APP` -> `APPS`
- `CAM` -> `CAMERA`
- `MIC` -> `MICROPHONE`
- `NET` -> `NETWORK`
- `GPS` -> `LOCATION`
- `VCM`, `CHT`, `TTS`, `QRY` -> no permission required (core codes)

---

## Integration with DSL Parser

The `AvuDslParser` validates declared codes in `.vos`/`.avp` headers against the registry:

```kotlin
// In AvuDslParser.parseHeader()
val headerData = AvuHeader.parse(headerContent)
val declaredCodes = headerData.codes.keys

// Post-parse validation
declaredCodes.forEach { code ->
    if (!AvuCodeRegistry.isRegistered(code)) {
        warnings.add(ParseError("Unknown code: $code", line, col, ErrorSeverity.WARNING))
    }
}
```

---

## Best Practices

1. **Register at initialization** - Register codes in `init {}` blocks or DI setup, before any message processing
2. **Validate at boundaries** - Always validate incoming messages at system entry points
3. **Include field definitions** - Provide `AvuFieldDef` lists for documentation and validation
4. **Use categories** - Group related codes for organized legends and documentation
5. **Pre-register before headers** - Ensure all codes are registered before generating file headers with `generateLegend()`
6. **Use `clear()` in tests** - Reset registry between test cases to avoid cross-contamination

---

## Related Documents

- [Ch37: Universal Format v2.0](Developer-Manual-Chapter37-Universal-Format-v2.0.md)
- [Ch51: 3Letter JSON Schema](Developer-Manual-Chapter51-3Letter-JSON-Schema.md)
- [Ch80: AVU Codec v2.2](Developer-Manual-Chapter80-AVU-Codec-v2.2.md)
- [Ch81: AVU Protocol Overview](Developer-Manual-Chapter81-AVU-Protocol-Overview.md)
- [Ch82: AVU Wire Protocol](Developer-Manual-Chapter82-AVU-Wire-Protocol.md)
- [Ch83: AVU DSL Syntax](Developer-Manual-Chapter83-AVU-DSL-Syntax.md)

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-02-06 | Initial chapter |
| 2.0 | 2026-02-06 | Updated: namespace + permission mapping now implemented (Phase 3) |
