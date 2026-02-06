# Developer Manual - Chapter 82: AVU Wire Protocol

**Date**: 2026-02-06
**Author**: Augmentalis Engineering
**Status**: Active

---

## Overview

Layer 1 of the AVU three-layer architecture. The wire protocol encodes messages as `CODE:field1:field2:...` using 3-letter uppercase codes. All special characters are percent-encoded via `AvuEscape`. The `AvuCodeRegistry` provides runtime validation, documentation, and legend generation.

---

## Message Format

```
CODE:field1:field2:field3:...
```

- **CODE**: Exactly 3 uppercase letters (e.g., `VCM`, `AAC`, `SCR`)
- **Fields**: Colon-separated values, percent-encoded
- **No trailing delimiter**: Last field ends the message

### Examples

```
GST:req_001                                    # Get Status
SST:req_001:1:1:0:en-US:4.0.0:voice,a11y      # Status Response
VCM:cmd_01:scroll down:app=chrome              # Voice Command
AAC:act_01:CLICK:login_btn:                    # Accessibility Action
```

---

## Escape Sequences (AvuEscape)

| Character | Encoded | Description |
|-----------|---------|-------------|
| `%` | `%25` | Percent sign (escape FIRST) |
| `:` | `%3A` | Colon (field delimiter) |
| `\n` | `%0A` | Line feed |
| `\r` | `%0D` | Carriage return |

**Critical ordering**: Escape `%` first, unescape `%` last.

### API

```kotlin
import com.augmentalis.avucodec.core.AvuEscape

AvuEscape.escape("hello:world")     // "hello%3Aworld"
AvuEscape.unescape("hello%3Aworld") // "hello:world"
AvuEscape.needsEscaping("hello")    // false
AvuEscape.needsEscaping("he:lo")    // true
AvuEscape.escapeIfNeeded("hello")   // "hello" (no allocation)
AvuEscape.escapeIfNeeded("he:lo")   // "he%3Alo"
```

---

## Code System

### AvuCodeInfo

```kotlin
data class AvuCodeInfo(
    val code: String,         // 3 uppercase chars, e.g. "VCM"
    val name: String,         // "Voice Command"
    val category: AvuCodeCategory,
    val format: String,       // "requestId:commandText:context"
    val description: String = "",
    val fields: List<AvuFieldDef> = emptyList(),
    val example: String? = null,
    val since: String = "1.0"
)
```

### Categories

| Category | Description |
|----------|-------------|
| CORE | System-level operations |
| VOICE | Voice command processing |
| SYNC | Data synchronization |
| RPC | Remote procedure calls |
| MEDIA | Audio/video operations |
| PLUGIN | Plugin lifecycle |
| BROWSER | Web browser control |
| AI | AI/NLU operations |
| SYSTEM | Device system operations |
| CUSTOM | User-defined codes |

### Field Types (AvuFieldType)

`STRING`, `INT`, `LONG`, `FLOAT`, `BOOLEAN`, `TIMESTAMP`, `ENUM`, `JSON`, `DATA`

---

## Encoder/Decoder Pattern

### Encoding (from VoiceOSAvuEncoder)

```kotlin
fun encodeVoiceCommand(request: VoiceCommandRequest): String = buildString {
    append(VoiceOSAvuCodes.VOICE_COMMAND)
    append(":").append(AvuEscape.escape(request.requestId))
    append(":").append(AvuEscape.escape(request.commandText))
    append(":").append(encodeMap(request.context))
}

// Boolean: "1"/"0"
// Map: "key1=val1,key2=val2"
```

### Decoding (from VoiceOSAvuDecoder)

```kotlin
fun parse(raw: String): AvuMessage {
    val parts = raw.split(":")
    val code = parts.first()
    val fields = parts.drop(1).map { AvuEscape.unescape(it) }
    return AvuMessage(code, fields)
}
```

---

## AvuHeader

Self-documenting file headers using `---` delimiters. The `HeaderData` structure includes core fields (schema, version, type, metadata, codes) plus a generic `sections` map for any additional section.

```kotlin
data class HeaderData(
    val formatVersion: String = "2.2",
    val type: String = "",
    val schema: String = "avu-2.2",
    val version: String = "1.0.0",
    val project: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val codes: Map<String, String> = emptyMap(),
    val sections: Map<String, List<String>> = emptyMap()
)
```

---

## AvuCodeRegistry

### Registration

```kotlin
AvuCodeRegistry.register(AvuCodeInfo(
    code = "VCM", name = "Voice Command",
    category = AvuCodeCategory.VOICE,
    format = "requestId:commandText:context"
))
```

### Validation

```kotlin
when (val result = AvuCodeRegistry.validate("VCM:cmd_01:scroll down")) {
    is AvuCodeRegistry.ValidationResult.Valid -> { /* OK */ }
    is AvuCodeRegistry.ValidationResult.Invalid -> { /* format error */ }
    is AvuCodeRegistry.ValidationResult.UnknownCode -> { /* unregistered */ }
}
```

---

## Transport Mechanisms

| Transport | Use Case |
|-----------|----------|
| Raw TCP | High-performance local IPC |
| WebSocket | Browser-to-app communication |
| Android Intent | Inter-app messaging |
| iOS URL Scheme | Inter-app messaging |

---

## Best Practices

1. Always use `AvuEscape.escape()` for field values
2. Register codes at module initialization
3. Use `AvuCodeRegistry.validate()` for incoming messages
4. Prefer `escapeIfNeeded()` to avoid unnecessary allocations
5. Use compact legends (`generateCompactLegend()`) for bandwidth-sensitive contexts

---

## Related Documents

- [Ch37: Universal Format v2.0](Developer-Manual-Chapter37-Universal-Format-v2.0.md)
- [Ch80: AVU Codec v2.2](Developer-Manual-Chapter80-AVU-Codec-v2.2.md)
- [Ch81: AVU Protocol Overview](Developer-Manual-Chapter81-AVU-Protocol-Overview.md)

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-02-06 | Initial chapter |
