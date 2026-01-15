# TODO: AVID System Implementation

**Date:** 2026-01-13
**Status:** PLANNING COMPLETE - Ready for Implementation
**Priority:** P1 - Core Infrastructure
**Branch:** Refactor-VUID (from Refactor-AvaMagic)

---

## Overview

Consolidate all VUID/UUID implementations into a single unified AVID (Avanues Voice ID) system in `Modules/VUID`.

---

## Phase 1: Delete Duplicate Modules (READY)

### Files to Delete

| Path | Status | Notes |
|------|--------|-------|
| `Common/uuidcreator/` | PENDING | Duplicate, unused |
| `Common/Libraries/uuidcreator/` | PENDING | Duplicate, unused |
| `Modules/AVAMagic/Libraries/UUIDCreator/` | PENDING | Duplicate, unused |
| `Common/VUID/` | DONE | Already deleted |

**Action:** Delete these 3 directories (they are copies of the same code)

### Verification

```bash
# Confirm no dependencies on these paths
grep -r "Common:uuidcreator" --include="*.kts" .
grep -r "Common:Libraries:uuidcreator" --include="*.kts" .
grep -r "AVAMagic:Libraries:UUIDCreator" --include="*.kts" .
```

---

## Phase 2: Update Module Structure

### Current State

```
Modules/VUID/                    # EXISTS - Basic structure
├── build.gradle.kts             # EXISTS - Needs update
└── src/
    └── commonMain/kotlin/
        └── com/augmentalis/vuid/
            └── core/
                └── VUIDGenerator.kt  # EXISTS - Old DNS-style format
```

### Target State

```
Modules/VUID/
├── build.gradle.kts
├── src/
│   ├── commonMain/kotlin/com/augmentalis/vuid/
│   │   ├── AvidGenerator.kt           # NEW - Main generator
│   │   ├── AvidParser.kt              # NEW - .avid parsing
│   │   ├── AvtrParser.kt              # NEW - .avtr parsing
│   │   ├── TypeResolver.kt            # NEW - Type code resolution
│   │   ├── Fingerprint.kt             # NEW - Hash functions
│   │   └── models/
│   │       ├── AvidFile.kt            # NEW
│   │       ├── AvidApp.kt             # NEW
│   │       ├── AvidElement.kt         # NEW
│   │       ├── AvidCommand.kt         # NEW
│   │       └── TypeDefinition.kt      # NEW
│   ├── androidMain/kotlin/
│   │   └── AndroidAvidGenerator.kt    # NEW
│   └── iosMain/kotlin/
│       └── IosAvidGenerator.kt        # NEW
└── docs/
    ├── AVID-Format-Specification-260113-V1.md      # DONE
    ├── AVTR-Format-Specification-260113-V1.md      # DONE
    ├── AVID-Usage-Guide-260113-V1.md               # DONE
    └── examples/
        ├── social-apps.avid                        # DONE
        └── voiceos-standard.avtr                   # DONE
```

---

## Phase 3: Implement Core Classes

### 3.1 Data Models

```kotlin
// AvidFile.kt
data class AvidFile(
    val schema: String,
    val version: String,
    val source: String,
    val exported: String,
    val platform: String,
    val osVersion: String,
    val apps: List<AvidApp>,
    val elements: List<AvidElement>,
    val screens: List<AvidScreen>,
    val commands: List<AvidCommand>,
    val synonyms: List<AvidSynonym>
)

// AvidApp.kt
data class AvidApp(
    val avid: String,
    val platform: String,
    val osVersion: String,
    val packageName: String,
    val name: String,
    val appVersion: String,
    val fingerprint: String
)

// AvidElement.kt
data class AvidElement(
    val avid: String,
    val version: String,
    val elemId: Int,
    val type: String,
    val resourceId: String?,
    val name: String?,
    val contentDesc: String?,
    val bounds: String?
)
```

### 3.2 Parsers

- `AvidParser.kt` - Parse .avid files
- `AvtrParser.kt` - Parse .avtr files

### 3.3 Generator

- `AvidGenerator.kt` - Interface
- `AndroidAvidGenerator.kt` - Android implementation
- `IosAvidGenerator.kt` - iOS implementation

### 3.4 Type Resolution

- `TypeResolver.kt` - Map class names ↔ type codes

---

## Phase 4: Database Schema (SQLDelight)

### Files to Create

| File | Purpose |
|------|---------|
| `src/commonMain/sqldelight/com/augmentalis/vuid/Apps.sq` | App table |
| `src/commonMain/sqldelight/com/augmentalis/vuid/Elements.sq` | Element table |
| `src/commonMain/sqldelight/com/augmentalis/vuid/Commands.sq` | Command table |
| `src/commonMain/sqldelight/com/augmentalis/vuid/Synonyms.sq` | Synonym table |
| `src/commonMain/sqldelight/com/augmentalis/vuid/Views.sq` | Readable views |

---

## Phase 5: Update Dependencies

### Files to Modify

| File | Change |
|------|--------|
| `Modules/VoiceOSCoreNG/build.gradle.kts` | Add `implementation(project(":Modules:VUID"))` |
| `Modules/AVA/core/Data/build.gradle.kts` | Already has `:Modules:VUID` |
| `Modules/VoiceOS/libraries/UUIDCreator/build.gradle.kts` | Already has `:Modules:VUID` |
| `Modules/VoiceOS/apps/VoiceOSCore/build.gradle.kts` | Check for old paths |

### Imports to Update (~82 files)

See: `VUID-Unified-System-Analysis-260113-V1.md` for full list

Key files:
- VoiceOSCoreNG internal VUIDGenerator → use Modules/VUID
- UUIDCreator references → use Modules/VUID

---

## Phase 6: Deprecate Old Implementations

### VoiceOSCoreNG Internal VUIDGenerator

| File | Action |
|------|--------|
| `Modules/VoiceOSCoreNG/src/commonMain/kotlin/.../common/VUIDGenerator.kt` | DELETE |
| `Modules/VoiceOSCoreNG/src/commonMain/kotlin/.../common/VUIDTypeCode.kt` | DELETE |

### UUIDCreator Library

| File | Action |
|------|--------|
| `Modules/VoiceOS/libraries/UUIDCreator/` | MIGRATE then DELETE |

---

## Phase 7: Testing

### Unit Tests

- [ ] AvidParser tests
- [ ] AvtrParser tests
- [ ] TypeResolver tests
- [ ] Fingerprint tests
- [ ] AvidGenerator tests

### Integration Tests

- [ ] Database operations
- [ ] Export/import round-trip
- [ ] Voice command lookup

---

## Implementation Order

1. **Phase 1** - Delete duplicates (5 min)
2. **Phase 3** - Implement core classes (2-3 hours)
3. **Phase 4** - Database schema (1 hour)
4. **Phase 5** - Update dependencies (30 min)
5. **Phase 6** - Deprecate old code (1 hour)
6. **Phase 7** - Testing (1-2 hours)

---

## Coordination Notes

**Branch:** `Refactor-VUID`
**Parent Branch:** `Refactor-AvaMagic`

### Files Reserved (Do Not Modify from Other Terminals)

```
Modules/VUID/**
Modules/VoiceOSCoreNG/src/commonMain/kotlin/.../common/VUIDGenerator.kt
Modules/VoiceOSCoreNG/src/commonMain/kotlin/.../common/VUIDTypeCode.kt
settings.gradle.kts (VUID entries only)
```

### Dependencies on Other Work

- None currently identified

---

## Specifications Created

| Document | Path |
|----------|------|
| AVID Format Spec | `Modules/VUID/docs/AVID-Format-Specification-260113-V1.md` |
| AVTR Format Spec | `Modules/VUID/docs/AVTR-Format-Specification-260113-V1.md` |
| Usage Guide | `Modules/VUID/docs/AVID-Usage-Guide-260113-V1.md` |
| System Architecture | `Modules/VUID/docs/VUID-VID-Specification-260113-V1.md` |
| Example .avid | `Modules/VUID/docs/examples/social-apps.avid` |
| Example .avtr | `Modules/VUID/docs/examples/voiceos-standard.avtr` |

---

## Key Decisions Made

1. **AVID branding** - Avanues Voice ID (not VID)
2. **Platform codes** - 1-char in AVID, 3-char in records
3. **OS version** - In APP record, not in AVID identifier
4. **Type codes** - Generic 3-char, resolved via AVTR registry
5. **Runtime IDs** - Integer pairs (appId, elemId) for performance
6. **Human readable** - On-demand via database views

---

**Ready for implementation approval.**
