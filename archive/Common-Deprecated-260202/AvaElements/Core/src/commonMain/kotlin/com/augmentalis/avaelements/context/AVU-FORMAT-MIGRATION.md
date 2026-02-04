# AVU Format Migration - Screen Hierarchy System

**Date:** 2025-12-06
**Version:** 1.0.0
**Status:** Complete

---

## Overview

The Universal Screen Hierarchy System has been updated to use **AVU (Avanues Universal)** format instead of generic JSON terminology. This aligns with the Avanues ecosystem's standardized file format system.

---

## What Changed

### Terminology Updates

| Old Term | New Term | Description |
|----------|----------|-------------|
| JSON | AVU | File format name |
| .json files | .avu files | File extension |
| JSON serialization | AVU serialization | Process name |
| JSON export | AVU export | Export terminology |

### API Changes

All serialization methods have been renamed:

| Old Method | New Method | Status |
|------------|------------|--------|
| `toJson()` | `toAvu()` | Old method deprecated |
| `toQuantizedJson()` | `toQuantizedAvu()` | Old method deprecated |
| `toNLUJson()` | `toNLUAvu()` | Old method deprecated |
| `componentTreeToJson()` | `componentTreeToAvu()` | Old method deprecated |
| `exportToJSON()` | `exportToAVU()` | Old method deprecated |
| `exportQuantizedToJSON()` | `exportQuantizedToAVU()` | Old method deprecated |

---

## Migration Guide

### For Code Using Serialization

**Before:**
```kotlin
// Old JSON-based API
val json = ScreenHierarchySerializer.toJson(hierarchy)
saveToFile("screen.json", json)

val quantized = ScreenHierarchySerializer.toQuantizedJson(hierarchy)
val nluContext = ScreenHierarchySerializer.toNLUJson(context)
```

**After:**
```kotlin
// New AVU-based API
val avu = ScreenHierarchySerializer.toAvu(hierarchy)
saveToFile("screen.avu", avu)

val quantized = ScreenHierarchySerializer.toQuantizedAvu(hierarchy)
val nluContext = ScreenHierarchySerializer.toNLUAvu(context)
```

### For VoiceOS Adapter

**Before:**
```kotlin
val adapter = VoiceOSScreenHierarchyAdapter(...)
val json = adapter.exportToJSON()
val quantizedJson = adapter.exportQuantizedToJSON()
```

**After:**
```kotlin
val adapter = VoiceOSScreenHierarchyAdapter(...)
val avu = adapter.exportToAVU()
val quantizedAvu = adapter.exportQuantizedToAVU()
```

---

## Backward Compatibility

### Deprecated Methods

All old JSON-based methods are **deprecated but still functional**. They redirect to the new AVU methods:

```kotlin
@Deprecated("Use toAvu() instead", ReplaceWith("toAvu(hierarchy, pretty)"))
fun toJson(hierarchy: ScreenHierarchy, pretty: Boolean = true): String = toAvu(hierarchy, pretty)
```

**Migration Timeline:**
- **Now:** Old methods work but show deprecation warnings
- **Next Release:** Old methods will be removed
- **Action Required:** Update all code to use new AVU methods

### IDE Support

The deprecation annotations include `ReplaceWith` hints, so modern IDEs will offer automatic migration:

1. IDE shows deprecation warning
2. Quick-fix option available: "Replace with toAvu()"
3. One-click migration to new API

---

## What is AVU Format?

AVU (Avanues Universal) is the standard file format used across the Avanues ecosystem:

### Schema Identifier

```yaml
schema: avu-1.0  # Avanues Universal format version
```

### File Extensions by Project

| Extension | Project | Purpose |
|-----------|---------|---------|
| `.avu` | Universal | Screen hierarchy, shared data |
| `.ava` | AVA | Voice intent examples |
| `.vos` | VoiceOS | System commands & plugins |
| `.avc` | AvaConnect | Device pairing & IPC |
| `.avw` | WebAvanue | Browser commands |
| `.avn` | NewAvanue | Platform configuration |
| `.avs` | Avanues | UI DSL components |

### Format Structure

```
# Avanues Universal Format v1.0
# Type: AVU
# Extension: .avu
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: screen-hierarchy
metadata:
  file: login-screen.avu
  category: ui_context
  name: Login Screen Hierarchy
---
[Screen hierarchy data in JSON format]
```

---

## Files Updated

### Source Files

1. **ScreenHierarchySerialization.kt**
   - Updated all method names
   - Added deprecation annotations
   - Updated documentation

2. **VoiceOSScreenHierarchyAdapter.kt**
   - Renamed export methods
   - Added deprecation annotations
   - Updated comments

3. **README.md**
   - Updated all examples to use AVU terminology
   - Updated API reference
   - Updated architecture diagrams

### Test Files

4. **ScreenHierarchySerializationTest.kt**
   - Renamed all test methods
   - Updated variable names
   - Updated assertions and messages

---

## Benefits of AVU Format

1. **Ecosystem Alignment**: Consistent with all Avanues projects
2. **Clear Ownership**: `.avu` extension shows Universal system ownership
3. **Cross-Compatible**: All Avanues apps can read/write AVU files
4. **Tooling Support**: File managers show correct file type
5. **Universal IPC**: Integrates with Avanues Universal IPC system

---

## References

- **Universal Format Spec**: `/docs/specifications/UNIVERSAL-FILE-FORMAT-SPEC.md`
- **Migration Guide**: `/docs/specifications/MIGRATION-GUIDE-UNIVERSAL-FORMAT.md`
- **Implementation Guide**: `/docs/UNIVERSAL-FORMAT-IMPLEMENTATION-COMPLETE.md`

---

## Summary

✅ **All JSON references updated to AVU**
✅ **Backward compatibility maintained**
✅ **Deprecation warnings in place**
✅ **Documentation updated**
✅ **Tests updated**
✅ **Migration path clear**

The Universal Screen Hierarchy System now fully aligns with Avanues Universal Format standards.

---

**Last Updated:** 2025-12-06
**Author:** IDEACODE v10.3
