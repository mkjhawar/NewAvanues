# Universal Format v2.0 - Master Implementation Guide

**Last Updated:** 2025-11-20  
**Status:** Active  
**Ecosystem Version:** 1.0

---

## Overview

The **Avanues Universal Format v2.0** is a unified, human-readable file format with 3-letter IPC codes used across the entire Avanues ecosystem for intents, commands, protocols, UI components, and code templates.

### Ecosystem Adoption

| Project | Extension | Status | Files | Purpose |
|---------|-----------|--------|-------|---------|
| **AVA** | `.ava` | ✅ Complete | 4 | Voice intent examples (124 intents) |
| **VoiceOS** | `.vos` | ✅ Complete | 1 | System commands (94 commands) |
| **AvaConnect** | `.avc` | ✅ Complete | 1 | Protocol definitions (80 codes) |
| **BrowserAvanue** | `.awb` | ✅ Complete | 1 | Browser commands (20 commands) |
| **MagicUI** | `.ami` | ✅ Complete | 1 | UI DSL components (60 components) |
| **MagicCode** | `.amc` | ✅ Complete | 1 | Code generation (50 templates) |

**Progress:** 6/6 projects (100%)

---

## Benefits Across Ecosystem

### 1. Human-Readable
- No JSON parsing needed to review files
- Clear 3-letter mnemonic codes (VCM, AIQ, CHT, etc.)
- Self-documenting format

### 2. IPC-Ready
- Direct IPC message creation from file entries
- Zero-conversion overhead
- Wire format matches file format

### 3. Cross-Project Compatible
- All projects can read each other's files
- UniversalFileParser provides unified parsing
- Shared code definitions across ecosystem

### 4. Size Reduction
- 60-87% smaller than JSON for IPC messages
- 16% reduction for command files
- Compact yet human-readable

### 5. Consistent Structure
- Same format across all 6 file types
- Predictable parsing
- Easy to learn once, use everywhere

---

## Format Structure (Universal)

All formats (.ava, .vos, .avc, .awb, .ami, .amc) share this structure:

```
# Avanues Universal Format v1.0
# Type: [PROJECT] - [PURPOSE]
# Extension: .[ext]
# Project: [project_name]
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: [project_name]
metadata:
  file: example.[ext]
  category: [category]
  name: [Display Name]
  description: [Description]
  priority: 1
  count: [number]
---
CODE:item_id:description or example
CODE:item_id:alternative description
---
synonyms:
  word: [synonym1, synonym2]
```

### Sections

1. **Header:** Comment block with file metadata (optional)
2. **Metadata:** YAML-like structure with schema info
3. **Entries:** IPC code format entries
4. **Synonyms:** Global word expansions (optional)

---

## IPC Code Categories

### Communication Codes (Universal)
| Code | Meaning | Projects Using |
|------|---------|----------------|
| **VCM** | Voice Command | AVA, VoiceOS, AvaConnect |
| **AIQ** | AI Query | AVA, AvaConnect |
| **STT** | Speech to Text | AVA, AvaConnect |
| **TTS** | Text to Speech | AvaConnect |
| **CHT** | Chat/Text Message | AvaConnect |
| **VCA** | Video Call | AvaConnect |
| **ACA** | Audio Call | AvaConnect |
| **ACC** | Accept | AvaConnect |
| **DEC** | Decline | AvaConnect |
| **FTR** | File Transfer | AvaConnect |
| **JSN** | JSON/UI Component | AvaConnect, MagicUI |

### Project-Specific Codes
- **AVA:** VCM (primary), AIQ, STT, CTX, SUG
- **VoiceOS:** VCM (all commands)
- **AvaConnect:** 80 codes across 10 categories
- **BrowserAvanue:** URL, BCK, FWD, RLD, TAB, CLS, SWT, ZIN, ZOT, SRC, BMK, HST, DWN, CFG, INC, PRT, SHR, RDR, FSC (20 codes)
- **MagicUI:** COL, ROW, TXT, BTN, INP, IMG, etc. (60 components)
- **MagicCode:** ACT, FRG, VML, RCT, TST, etc. (50 templates)

---

## Implementation by Project

### AVA (.ava files)

**Location:** `apps/ava-standalone/src/main/assets/ava-examples/`

**Files:**
- `navigation.ava` (8 intents)
- `media-control.ava` (10 intents)
- `system-control.ava` (12 intents)
- `voiceos-commands.ava` (94 intents)

**Total:** 124 intents

**Parser:** `Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/ava/parser/AvaFileParser.kt`

**Database:** Schema v4 with `format_version` and `ipc_code` tracking

**Documentation:** `docs/Developer-Manual-Chapter37-Universal-Format-v2.0.md`

---

### VoiceOS (.vos files)

**Location:** `app/src/main/assets/vos-commands/`

**Files:**
- `voiceos-commands.vos` (94 commands, 32KB)

**Categories:**
- Connectivity (20)
- Display (15)
- Volume (20)
- System (25)
- Navigation (14)

**Total:** 94 commands

**Documentation:** `docs/Universal-Format-Commands.md`

---

### AvaConnect (.avc files)

**Location:** `protocol-definitions/`

**Files:**
- `avaconnect-protocol.avc` (80 protocol codes, 3.3KB)

**Categories:**
- REQUESTS (13)
- RESPONSES (7)
- EVENTS (9)
- STATE (8)
- CONTENT (10)
- VOICE (8)
- BROWSER (6)
- AI (6)
- SYSTEM (7)
- SERVER (10)

**Total:** 80 protocol codes

**Implementation:** `demos/android/avaconnect-minimal-demo/src/main/kotlin/com/augmentalis/avaconnect/minimal/protocol/CompactProtocol.kt`

**Documentation:** `docs/Universal-Format-Protocol.md`

---

### BrowserAvanue (.awb files)

**Location:** `browser-commands/`

**Files:**
- `browseravanue-commands.awb` (20 commands)

**Categories:**
- Navigation: URL, BCK, FWD, RLD (4)
- Tabs: TAB, CLS, SWT (3)
- Zoom: ZIN, ZOT (2)
- Search: SRC (1)
- Bookmarks: BMK (1)
- History: HST (1)
- Downloads: DWN (1)
- Settings: CFG (1)
- Incognito: INC (1)
- Voice Search: VCM (1)
- Page Actions: PRT, SHR (2)
- Reader Mode: RDR (1)
- Fullscreen: FSC (1)

**Total:** 20 commands

**Documentation:** `docs/BrowserAvanue-Universal-Format.md`

---

### MagicUI (.ami files)

**Location:** `magicui-components/`

**Files:**
- `magicui-components.ami` (60 components)

**Categories:**
- Layout: COL, ROW, BOX, STK, GRD, LST, SPC, DVR (8)
- Text: TXT, HDR, LBL, CAP, TIT, SUB, PAR (7)
- Input: INP, TFA, PWD, NUM, EML, PHN, DAT, TIM, CHK, RDO, SWT, SLD (13)
- Button: BTN, ICB, FAB, TGB, CHI, SEG (6)
- Display: IMG, ICN, AVT, BDG, TAG, CRD, DLG, SHT, SNK, TLT, PRG, SPN (13)
- Navigation: TAB, NAV, DRW, MNU, BRD, PGN, STR (7)
- Media: VID, AUD, CAM, MIC, GAL, CRO (6)
- Data: TBL, CHT, GRA, TRE, TML, CAL (6)
- Feedback: ALR, TST, SKL, EMP, ERR (5)

**Total:** 60 components

**Documentation:** `docs/MagicUI-Universal-Format.md`

---

### MagicCode (.amc files)

**Location:** `magiccode-templates/`

**Files:**
- `magiccode-templates.amc` (50 templates)

**Categories:**
- Android Kotlin: ACT, FRG, VML, REP, DAO, ENT, SRV, RCV, WRK, CMP (10)
- iOS Swift: VWC, SWV, MOD, SVC, MGR, EXT, PRO (7)
- Web JS/TS: RCT, VUE, ANG, STO, ACN, RED, MID, HKS, CTX (9)
- KMP: KMP, EXP, ACT, SHR (4)
- Database: TBL, MIG, QRY, IDX, TRG (5)
- API: RTE, CTR, MDL, VAL, RES, REQ, DTR (7)
- Testing: TST, ITS, E2E, MOK, STB, SPY, FXT (7)
- Utility: UTL, HLP, EXT, DEC, FAC, SNG, OBS, ADT (8)

**Total:** 50 templates

**Documentation:** `docs/MagicCode-Universal-Format.md`

---

## UniversalFileParser (Shared Library)

**Location:** `modules/AVAMagic/IPC/UniversalIPC/src/commonMain/kotlin/com/augmentalis/avamagic/ipc/UniversalFileParser.kt`

### Features

- **Multi-format support:** Parses all 6 file types
- **Auto-detection:** Identifies format by extension and structure
- **KMP compatible:** Works on Android, iOS, Web
- **Type-safe:** Sealed class hierarchy for file types
- **Cross-project:** Any project can read any format

### Usage Example

```kotlin
import com.augmentalis.avamagic.ipc.UniversalFileParser
import com.augmentalis.avamagic.ipc.FileType

// Read AVA intents
val avaContent = File("navigation.ava").readText()
val avaFile = UniversalFileParser.parse(avaContent, FileType.AVA)

// Read VoiceOS commands
val vosContent = File("voiceos-commands.vos").readText()
val vosFile = UniversalFileParser.parse(vosContent, FileType.VOS)

// Read AvaConnect protocols
val avcContent = File("avaconnect-protocol.avc").readText()
val avcFile = UniversalFileParser.parse(avcContent, FileType.AVC)

// Read BrowserAvanue commands
val awbContent = File("browseravanue-commands.awb").readText()
val awbFile = UniversalFileParser.parse(awbContent, FileType.AWB)

// Read MagicUI components
val amiContent = File("magicui-components.ami").readText()
val amiFile = UniversalFileParser.parse(amiContent, FileType.AMI)

// Read MagicCode templates
val amcContent = File("magiccode-templates.amc").readText()
val amcFile = UniversalFileParser.parse(amcContent, FileType.AMC)
```

---

## Cross-Project Compatibility Matrix

| Reader ↓ / File → | .ava | .vos | .avc | .awb | .ami | .amc |
|-------------------|------|------|------|------|------|------|
| **AVA** | ✅ Native | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **VoiceOS** | ✅ Yes | ✅ Native | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes |
| **AvaConnect** | ✅ Yes | ✅ Yes | ✅ Native | ✅ Yes | ✅ Yes | ✅ Yes |
| **BrowserAvanue** | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Native | ✅ Yes | ✅ Yes |
| **MagicUI** | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Native | ✅ Yes |
| **MagicCode** | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Yes | ✅ Native |

**All projects can read all formats via UniversalFileParser.**

---

## Size Comparison

### AVA voiceos-commands
- **Before (v1.0 JSON):** 1,943 lines, 38KB
- **After (v2.0 Universal):** 1,151 lines, 32KB
- **Reduction:** 16%

### AvaConnect Protocol Messages
| Message | JSON | Universal | Reduction |
|---------|------|-----------|-----------|
| Video call | 182 bytes | 24 bytes | **87%** |
| Chat | 160 bytes | 22 bytes | **86%** |
| File transfer | 245 bytes | 45 bytes | **82%** |
| Accept | 98 bytes | 9 bytes | **91%** |
| UI component | 395 bytes | 145 bytes | **63%** |

---

## Migration from v1.0 (Deprecated)

**⚠️ v1.0 JSON format is NO LONGER SUPPORTED as of November 2025.**

All projects have been fully migrated to v2.0 Universal Format with no backward compatibility.

### Migration Summary

| Project | Status | Commits |
|---------|--------|---------|
| AVA | ✅ Complete | 5 commits |
| VoiceOS | ✅ Complete | 2 commits |
| AvaConnect | ✅ Complete | 1 commit |
| BrowserAvanue | ✅ Complete | 1 commit |
| MagicUI | ✅ Complete | 1 commit |
| MagicCode | ✅ Complete | 1 commit |

---

## Creating New Files

### Template for Any Format

```
# Avanues Universal Format v1.0
# Type: [PROJECT] - [PURPOSE]
# Extension: .[ext]
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: [project_name]
metadata:
  file: example.[ext]
  category: [category]
  name: [Display Name]
  count: [number]
---
CODE:item_id:description
CODE:item_id:alternative
---
synonyms:
  key: [value1, value2]
```

### Best Practices

1. **Choose correct IPC code** for the entry type
2. **Group related entries** by item_id
3. **Add clear descriptions** for each entry
4. **Include synonyms** for common terms
5. **Test parsing** with UniversalFileParser

---

## References

### Specifications
- **UNIVERSAL-FILE-FORMAT-FINAL.md** - Complete format specification
- **UNIVERSAL-IPC-SPEC.md** - IPC protocol specification
- **MIGRATION-GUIDE-UNIVERSAL-FORMAT.md** - Migration guide (historical)

### Implementation
- **UniversalFileParser.kt** - `modules/AVAMagic/IPC/UniversalIPC/`
- **UniversalIPCManager.kt** - IPC manager implementation

### Project Documentation
- **AVA:** `docs/Developer-Manual-Chapter37-Universal-Format-v2.0.md`
- **VoiceOS:** `docs/Universal-Format-Commands.md`
- **AvaConnect:** `docs/Universal-Format-Protocol.md`
- **BrowserAvanue:** `docs/BrowserAvanue-Universal-Format.md`
- **MagicUI:** `docs/MagicUI-Universal-Format.md`
- **MagicCode:** `docs/MagicCode-Universal-Format.md`

---

## Roadmap

### Completed (6/6) ✅

All projects have been successfully migrated to Universal Format v2.0:

- ✅ AVA (.ava) - 124 intents
- ✅ VoiceOS (.vos) - 94 commands
- ✅ AvaConnect (.avc) - 80 protocols
- ✅ BrowserAvanue (.awb) - 20 browser commands
- ✅ MagicUI (.ami) - 60 UI components
- ✅ MagicCode (.amc) - 50 code templates

**Total:** 428 definitions across 6 projects

### Future Enhancements

Potential future additions:
- Additional language locales (es-ES, fr-FR, de-DE, etc.)
- Project-specific extension files
- Community-contributed templates and components

---

## Support

**Issues:** Report format or parsing issues in respective project repositories

**Questions:** Contact manoj@ideahq.net

---

**Ecosystem:** Avanues v1.0
**Format Version:** Universal v2.0
**Adoption:** 100% (6/6 projects) ✅
**Total Definitions:** 428 (124 intents + 94 commands + 80 protocols + 20 browser commands + 60 UI components + 50 code templates)
