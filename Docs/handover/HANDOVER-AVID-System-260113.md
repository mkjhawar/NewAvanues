# HANDOVER: AVID System Implementation

**Date:** 2026-01-13
**Session:** VUID/AVID Consolidation Design
**Branch:** `Refactor-AvaMagic` (current), `Refactor-VUID` (for implementation)
**Status:** PLANNING COMPLETE - Ready for Implementation

---

## 1. Executive Summary

Designed a unified **AVID (Avanues Voice ID)** system to replace multiple scattered VUID/UUID implementations. All specifications and documentation are complete. Implementation is ready to begin.

### Key Deliverables Completed

| Document | Path |
|----------|------|
| AVID Format Spec | `Modules/VUID/docs/AVID-Format-Specification-260113-V1.md` |
| AVTR Type Registry Spec | `Modules/VUID/docs/AVTR-Format-Specification-260113-V1.md` |
| Usage Guide | `Modules/VUID/docs/AVID-Usage-Guide-260113-V1.md` |
| System Architecture | `Modules/VUID/docs/VUID-VID-Specification-260113-V1.md` |
| Implementation TODO | `Modules/VUID/docs/TODO-AVID-Implementation-260113.md` |
| Example .avid file | `Modules/VUID/docs/examples/social-apps.avid` |
| Example .avtr file | `Modules/VUID/docs/examples/voiceos-standard.avtr` |
| Coordination file | `docs/coordination/ACTIVE-WORK.md` |

---

## 2. What is AVID?

**AVID = Avanues Voice ID** - A universal identifier system for tracking UI elements across the VoiceOS platform.

### AVID Identifier Format

```
AVID-{platform}-{sequence}
```

| Platform | Code | Example |
|----------|------|---------|
| Android | A | AVID-A-000001 |
| iOS | I | AVID-I-000001 |
| Web | W | AVID-W-000001 |
| macOS | M | AVID-M-000001 |
| Windows | X | AVID-X-000001 |
| Linux | L | AVID-L-000001 |

**Total length:** 13 characters

### File Formats

| Extension | Name | Purpose |
|-----------|------|---------|
| `.avid` | AVID Library | Element library (apps, elements, commands, synonyms) |
| `.avtr` | AVID Type Registry | Platform-specific UI class mappings |

---

## 3. Key Design Decisions Made

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Branding | AVID (Avanues Voice ID) | Aligns with Avanues ecosystem |
| AVID format | `AVID-{platform}-{sequence}` | Compact (13 chars), cloud-assigned |
| Platform in AVID | 1-char code (A, I, W, M, X, L) | Compact identifier |
| Platform in records | 3-char code (and, ios, web, mac, win, lnx) | Human readable |
| OS version | In APP record field, NOT in AVID | Keeps AVID compact |
| Type codes | Generic 3-char (BTN, INP, TXT) | Platform-agnostic |
| Type resolution | Via AVTR registry file | Viewer app resolves to platform-specific classes |
| Runtime IDs | Integer pairs (appId, elemId) | 8 bytes, fast lookups |
| Human readable | On-demand via database views | Not stored in IDs |
| Local unsynced | LAVID format (`LAVID-{device}-{id}`) | Works offline |

---

## 4. Current Implementations (To Be Consolidated)

### 4.1 VoiceOSCoreNG VUIDGenerator
**Path:** `Modules/VoiceOSCoreNG/src/commonMain/kotlin/com/augmentalis/voiceoscoreng/common/VUIDGenerator.kt`

**Features to preserve:**
- 16-char compact format: `a3f2e1-b917cc9dc`
- `VUIDTypeCode` enum (1-char codes)
- `TypePatternRegistry` for class→type mapping
- `parseVUID()` validation

**Action:** DELETE after merging features

### 4.2 Modules/VUID VUIDGenerator
**Path:** `Modules/VUID/src/commonMain/kotlin/com/augmentalis/vuid/core/VUIDGenerator.kt`

**Features to preserve:**
- DNS-style format: `android.instagram.com:12.0.0:btn:a7f3e2c1`
- `TypeAbbrev` object (3-char codes)
- Module constants (AVA, VOICEOS, etc.)
- Convenience methods
- Legacy format migration

**Action:** KEEP as base, enhance with AVID features

### 4.3 UUIDCreator Library
**Path:** `Modules/VoiceOS/libraries/UUIDCreator/`

**Features to preserve:**
- Flutter 3.19+ identifier support
- SecureRandom hash generation
- `generateFromFlutterIdentifier()`

**Action:** MIGRATE Android-specific code to `Modules/VUID/androidMain/`, DELETE library

### 4.4 Duplicate Directories (DELETE)
```
Common/uuidcreator/                    - DELETE (unused duplicate)
Common/Libraries/uuidcreator/          - DELETE (unused duplicate)
Modules/AVAMagic/Libraries/UUIDCreator/ - DELETE (unused duplicate)
```

---

## 5. AVID File Format (.avid)

```
# Avanues Voice ID Library v1.0
---
schema: avid-1.0
version: 1.0.0
source: device-id
exported: 2026-01-13T10:00:00Z
platform: and
os_version: 14
type_registry: voiceos-standard-1.0
metadata:
  apps: 3
  elements: 50
---
# APP:avid:platform:osVersion:package:name:appVersion:fingerprint
APP:AVID-A-000001:and:14:com.instagram.android:Instagram:350.0.0:a3f2e1c9b7d4

# ELM:avid:version:elemId:type:resourceId:name:contentDesc:bounds
ELM:AVID-A-000001:350.0.0:1:BTN:like_button:Like:Like this post:0.10,0.80,0.15,0.85

# CMD:avid:version:elemId:phrase:action:priority
CMD:AVID-A-000001:350.0.0:1:like:click:100

# SYN:avid:version:elemId:synonym:canonical
SYN:AVID-A-000001:350.0.0:1:love:like
---
aliases:
  app: [application, package]
```

---

## 6. AVTR Type Registry Format (.avtr)

Maps generic type codes to platform-specific UI classes.

```
# AVID Type Registry v1.0
---
schema: avtr-1.0
version: 1.0.0
name: VoiceOS Standard Types
metadata:
  platforms: [and, ios, web, mac, win, lnx]
---
TYP:BTN:Button:Clickable action elements
AND:BTN:Button,ImageButton,MaterialButton,FloatingActionButton
IOS:BTN:UIButton,UIBarButtonItem,Button
WEB:BTN:button,input[type=submit],[role=button]
MAC:BTN:NSButton,Button
WIN:BTN:Button,AppBarButton
LNX:BTN:GtkButton,QPushButton

ACT:click:Click:Tap or click element:*
ACT:long_press:Long Press:Press and hold:and,ios,dsk
---
```

---

## 7. Implementation Phases

| Phase | Description | Status |
|-------|-------------|--------|
| 1 | Delete 3 duplicate directories | READY |
| 2 | Update Modules/VUID structure | READY |
| 3 | Implement core classes (AvidGenerator, parsers) | READY |
| 4 | Database schema (SQLDelight) | READY |
| 5 | Update dependencies across codebase | READY |
| 6 | Deprecate old implementations | READY |
| 7 | Testing | PENDING |

### Phase 1: Delete Duplicates
```bash
rm -rf Common/uuidcreator/
rm -rf Common/Libraries/uuidcreator/
rm -rf Modules/AVAMagic/Libraries/UUIDCreator/
```

### Phase 3: Core Classes to Create

```
Modules/VUID/src/commonMain/kotlin/com/augmentalis/vuid/
├── AvidGenerator.kt           # Main generator interface
├── AvidParser.kt              # Parse .avid files
├── AvtrParser.kt              # Parse .avtr files
├── TypeResolver.kt            # Type code ↔ class resolution
├── Fingerprint.kt             # Deterministic hash functions
└── models/
    ├── AvidFile.kt
    ├── AvidApp.kt
    ├── AvidElement.kt
    ├── AvidCommand.kt
    └── TypeDefinition.kt
```

---

## 8. Database Schema (SQLDelight)

```sql
-- apps.sq
CREATE TABLE apps (
    local_id INTEGER PRIMARY KEY AUTOINCREMENT,
    avid TEXT,                    -- AVID-A-000001 or NULL if unsynced
    platform TEXT NOT NULL,       -- and, ios, web, mac, win, lnx
    os_version TEXT NOT NULL,
    package_name TEXT NOT NULL,
    app_name TEXT,
    app_version TEXT NOT NULL,
    fingerprint TEXT NOT NULL,
    UNIQUE(platform, package_name, app_version)
);

-- elements.sq
CREATE TABLE elements (
    local_app_id INTEGER NOT NULL,
    elem_id INTEGER NOT NULL,
    type TEXT NOT NULL,           -- BTN, INP, TXT, etc.
    resource_id TEXT,
    name TEXT,
    content_desc TEXT,
    bounds TEXT,
    PRIMARY KEY (local_app_id, elem_id)
);

-- commands.sq
CREATE TABLE commands (
    local_app_id INTEGER NOT NULL,
    elem_id INTEGER NOT NULL,
    phrase TEXT NOT NULL,
    action TEXT NOT NULL,
    priority INTEGER DEFAULT 100,
    PRIMARY KEY (local_app_id, elem_id, phrase)
);
```

---

## 9. Files Reserved (Do Not Modify from Other Work)

```
Modules/VUID/**
Modules/VoiceOSCoreNG/src/commonMain/kotlin/.../common/VUIDGenerator.kt
Modules/VoiceOSCoreNG/src/commonMain/kotlin/.../common/VUIDTypeCode.kt
Common/uuidcreator/
Common/Libraries/uuidcreator/
Modules/AVAMagic/Libraries/UUIDCreator/
```

See `docs/coordination/ACTIVE-WORK.md` for full coordination details.

---

## 10. Migration Path for Consumers

### Before (VoiceOSCoreNG)
```kotlin
import com.augmentalis.voiceoscoreng.common.VUIDGenerator
import com.augmentalis.voiceoscoreng.common.VUIDTypeCode

val vuid = VUIDGenerator.generate(packageName, VUIDTypeCode.BUTTON, elementHash)
```

### After (Modules/VUID)
```kotlin
import com.augmentalis.vuid.core.AvidGenerator

val avid = avidGenerator.registerApp("and", "14", packageName, appName, version)
val elemId = avidGenerator.registerElement(avid, version, "BTN", resourceId, name)
```

---

## 11. Key Files to Read First

1. **Start here:** `Modules/VUID/docs/TODO-AVID-Implementation-260113.md`
2. **Format spec:** `Modules/VUID/docs/AVID-Format-Specification-260113-V1.md`
3. **Type registry:** `Modules/VUID/docs/AVTR-Format-Specification-260113-V1.md`
4. **Usage guide:** `Modules/VUID/docs/AVID-Usage-Guide-260113-V1.md`
5. **Coordination:** `docs/coordination/ACTIVE-WORK.md`

---

## 12. Commands to Resume Work

```bash
# Check current branch
git branch

# If not on Refactor-AvaMagic, switch
git checkout Refactor-AvaMagic

# Create implementation branch if needed
git checkout -b Refactor-VUID

# Start with Phase 1: Delete duplicates
rm -rf Common/uuidcreator/
rm -rf Common/Libraries/uuidcreator/
rm -rf Modules/AVAMagic/Libraries/UUIDCreator/
```

---

## 13. Open Questions (None)

All design decisions have been made and approved:
- AVID branding ✓
- Platform codes (1-char in AVID, 3-char in records) ✓
- OS version in APP record (not in AVID) ✓
- Type codes via AVTR registry ✓
- Integer runtime IDs ✓

---

## 14. Context for New Session

**Prompt to start new session:**

```
Read /Volumes/M-Drive/Coding/NewAvanues/docs/handover/HANDOVER-AVID-System-260113.md

I'm ready to implement the AVID system. The design and specifications are complete.
Start with Phase 1 (delete duplicates) and proceed through all phases.

Key docs:
- Modules/VUID/docs/TODO-AVID-Implementation-260113.md
- Modules/VUID/docs/AVID-Format-Specification-260113-V1.md
- docs/coordination/ACTIVE-WORK.md
```

---

**End of Handover**
