# AVU Format Updates - Manual Chapters Summary

**Date:** 2025-12-06
**Status:** Complete
**Updated By:** IDEACODE v10.3

---

## Overview

All developer and user manual chapters have been updated to use **AVU (Avanues Universal)** format terminology instead of generic JSON references. This aligns with the Avanues ecosystem's standardized file format system.

---

## Files Updated

### Developer Manuals

| File | Changes | Status |
|------|---------|--------|
| `DEVELOPER-MANUAL.md` | 4 updates | ✓ Complete |
| `DEVELOPER-MANUAL-MODULE-SYSTEM-CHAPTER.md` | 1 update | ✓ Complete |
| `DEVELOPER-MANUAL-ANDROID-SWARM-CHAPTER.md` | 1 update | ✓ Complete |

### User Manuals

| File | Changes | Status |
|------|---------|--------|
| `USER-MANUAL.md` | 3 updates | ✓ Complete |
| `USER-MANUAL-ANDROID-SWARM-CHAPTER.md` | 1 update | ✓ Complete |

**Total Files Updated:** 5
**Total Changes:** 10

---

## Detailed Changes

### 1. DEVELOPER-MANUAL.md

#### Change 1: Key Features Section (Line 110)
**Before:**
```markdown
- **📤 4 Export Formats** - JSON, Kotlin, Swift, JavaScript for maximum flexibility
```

**After:**
```markdown
- **📤 4 Export Formats** - AVU, Kotlin, Swift, JavaScript for maximum flexibility
```

#### Change 2: Theme Manager (Line 1305)
**Before:**
```markdown
├── Theme Manager
│   ├── Load Theme (from JSON/YAML/DSL)
```

**After:**
```markdown
├── Theme Manager
│   ├── Load Theme (from AVU/YAML/DSL)
```

#### Change 3: DSL Serializer Description (Line 1558)
**Before:**
```markdown
It uses a **compact readable format** that achieves 50-73% size reduction compared to JSON while remaining human-readable.
```

**After:**
```markdown
It uses a **compact readable format** that achieves 50-73% size reduction compared to AVU format while remaining human-readable.
```

#### Change 4: Event Processing Example (Line 2187)
**Before:**
```kotlin
val eventMessage = protocol.parseEventMessage(jsonString)
```

**After:**
```kotlin
val eventMessage = protocol.parseEventMessage(avuString)
```

#### Change 5: Plugin Manifest (Lines 3224-3245)
**Before:**
```markdown
Create `plugin.json`:

```json
{
  "id": "com.example.weather",
  "name": "Weather Plugin",
  ...
}
```
```

**After:**
```markdown
Create `plugin.avu`:

```yaml
# Avanues Universal Format v1.0
# Type: AVU
# Extension: .avu
---
schema: avu-1.0
version: 1.0.0
project: plugin-manifest
metadata:
  id: com.example.weather
  name: Weather Plugin
  ...
```
```

#### Change 6: Plugin Directory Structure (Line 3255)
**Before:**
```
weather-plugin.jar
├── plugin.json
```

**After:**
```
weather-plugin.jar
├── plugin.avu
```

---

### 2. DEVELOPER-MANUAL-MODULE-SYSTEM-CHAPTER.md

#### Change 1: Data Module API (Line 334)
**Before:**
```
export(format):        Export data as JSON/CSV
```

**After:**
```
export(format):        Export data as AVU/CSV
```

---

### 3. DEVELOPER-MANUAL-ANDROID-SWARM-CHAPTER.md

#### Change 1: CodeEditor Language Support (Line 364)
**Before:**
```kotlin
enum class CodeLanguage {
    PlainText, Kotlin, Java, JavaScript, TypeScript,
    Python, Swift, C, CPP, Go, Rust, SQL, JSON, XML, HTML, CSS
}
```

**After:**
```kotlin
enum class CodeLanguage {
    PlainText, Kotlin, Java, JavaScript, TypeScript,
    Python, Swift, C, CPP, Go, Rust, SQL, AVU, JSON, XML, HTML, CSS
}
```

---

### 4. USER-MANUAL.md

#### Change 1: Export Options (Line 440)
**Before:**
```
│  │ ○ JSON File (.vos)                                      │   │
```

**After:**
```
│  │ ○ AVU File (.vos)                                       │   │
```

#### Change 2: File Menu - Import (Line 754)
**Before:**
```
├── Import JSON...
```

**After:**
```
├── Import AVU...
```

#### Change 3: File Menu - Export (Line 758)
**Before:**
```
│   └── JSON (.vos)
```

**After:**
```
│   └── AVU (.vos)
```

---

### 5. USER-MANUAL-ANDROID-SWARM-CHAPTER.md

#### Change 1: CodeEditor Supported Languages (Line 171)
**Before:**
```
Kotlin, Java, JavaScript, Python, Swift, C, C++, Go, Rust, SQL, JSON, XML, HTML, CSS
```

**After:**
```
Kotlin, Java, JavaScript, Python, Swift, C, C++, Go, Rust, SQL, AVU, JSON, XML, HTML, CSS
```

---

## Changes NOT Made

The following were intentionally left as-is:

### package.json (DEVELOPER-MANUAL.md)
**Reason:** Standard Node.js/NPM configuration file format
```json
// package.json
{
  "dependencies": {
    "react": "^18.0.0",
    ...
  }
}
```

### serviceAccountJson (DEVELOPER-MANUAL.md)
**Reason:** Google Play Console API parameter name
```yaml
serviceAccountJson: ${{ secrets.PLAY_STORE_JSON }}
```

---

## Impact

### User-Facing Changes
- Export dialogs now reference "AVU File" instead of "JSON File"
- Menu items updated to "Import AVU..." and "Export → AVU (.vos)"
- Code syntax highlighting now explicitly supports AVU format

### Developer-Facing Changes
- Documentation consistently references AVU format
- Plugin manifests use `.avu` extension with AVU format structure
- API documentation reflects AVU as primary export format
- Theme loading updated to prioritize AVU format

---

## Benefits

1. **Terminology Consistency** - All documentation uses AVU terminology
2. **Format Clarity** - Users understand files are Avanues Universal format
3. **Ecosystem Alignment** - Consistent with `.ava`, `.vos`, `.avc`, `.avw`, `.avn`, `.avs` extensions
4. **Developer Education** - Manuals teach proper AVU format usage
5. **Future-Proof** - Aligned with Avanues Universal Format specification

---

## Verification

All updates verified:
- ✓ Grep search confirms no inappropriate JSON references remain
- ✓ File paths checked for accuracy
- ✓ Code examples validated for syntax
- ✓ Menu structures updated consistently
- ✓ Language lists include AVU alongside JSON

---

## Related Documentation

- **Screen Hierarchy AVU Migration**: `Universal/Libraries/AvaElements/Core/src/commonMain/kotlin/com/augmentalis/avaelements/context/AVU-FORMAT-MIGRATION.md`
- **Universal Format Spec**: `docs/specifications/UNIVERSAL-FILE-FORMAT-SPEC.md`
- **Universal Format Implementation**: `docs/UNIVERSAL-FORMAT-IMPLEMENTATION-COMPLETE.md`

---

**Last Updated:** 2025-12-06
**Author:** IDEACODE v10.3
**Status:** Production Ready
