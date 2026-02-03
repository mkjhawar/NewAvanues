# AVU Format Specification V1.0

**Avanues Universal Format - Unified Line-Based Data Format**

| Attribute | Value |
|-----------|-------|
| Version | 1.0.0 |
| Status | Draft |
| Author | Augmentalis Engineering |
| Created | 2026-01-15 |
| Extension | `.avu` (universal) |

---

## 1. Overview

The **AVU (Avanues Universal)** format is a unified, line-based data format designed to replace multiple competing formats (IDC, VOS, AMF) with a single, extensible specification.

### 1.1 Design Goals

| Goal | Description |
|------|-------------|
| **Compact** | ~50% smaller than JSON/YAML equivalents |
| **Human-readable** | Easy to edit manually |
| **App Store friendly** | Small bundle sizes for mobile apps |
| **Fast parsing** | Line-based for O(n) parsing |
| **Self-describing** | Headers declare content type |
| **Backward compatible** | Legacy formats supported |

### 1.2 Format Consolidation

| Legacy Format | Extension | New AVU Type | Status |
|---------------|-----------|--------------|--------|
| IDEACODE Config | `.idc` | `AVU:CONFIG` | Supported |
| VoiceOS Commands | `.vos` | `AVU:VOICE` | Supported |
| AVAMagic Theme | `.amf` | `AVU:THEME` | Supported |
| AVAMagic Layout | `.amf` | `AVU:LAYOUT` | Supported |
| State Exchange | (new) | `AVU:STATE` | New |
| Data Exchange | (new) | `AVU:DATA` | New |

---

## 2. Format Structure

Every AVU file follows this structure:

```
# AVU Format v1.0
# Type: {TYPE}
---
schema: avu-{type}-1.0
version: {file_version}
{optional_metadata}
---
{ENTRIES}
---
{OPTIONAL_SYNONYMS}
```

### 2.1 Sections

| Section | Required | Description |
|---------|----------|-------------|
| Header | Yes | Format identifier and type declaration |
| Metadata | Yes | Schema, version, project info |
| Entries | Yes | Line-based data records |
| Synonyms | No | Optional synonym mappings |

### 2.2 Section Delimiter

Sections are separated by `---` on its own line.

---

## 3. Record Type Prefixes

All data entries use 3-character uppercase prefixes followed by colon.

### 3.1 Universal Prefixes (All Types)

| Prefix | Purpose | Format |
|--------|---------|--------|
| `VER:` | Version info | `VER:major:minor:patch` |
| `MET:` | Metadata | `MET:key:value` |
| `REF:` | Reference link | `REF:id:url_or_path` |
| `CMT:` | Comment (ignored) | `CMT:any text` |

### 3.2 Configuration Prefixes (AVU:CONFIG)

| Prefix | Purpose | Format | Example |
|--------|---------|--------|---------|
| `PRJ:` | Project | `PRJ:name:type:version` | `PRJ:nav:monorepo:12.0.0` |
| `CFG:` | Config entry | `CFG:key:value:type` | `CFG:debug:true:bool` |
| `PRF:` | Profile | `PRF:name:p1:p2:p3:p4` | `PRF:default:50:40:true:false` |
| `GAT:` | Quality gate | `GAT:name:threshold:enforce` | `GAT:coverage:90:true` |
| `THR:` | Threshold | `THR:name:value:action` | `THR:debt:70:warn` |
| `SWM:` | Swarm config | `SWM:type:count:ratio:p1:p2:p3` | `SWM:parallel:5:0.8:true:true:3` |
| `PTH:` | Path | `PTH:name:path` | `PTH:docs:docs/` |
| `REG:` | Registry | `REG:name:pattern:location` | `REG:specs:*-Spec-*.md:docs/` |
| `FNM:` | File naming | `FNM:id:filename:location` | `FNM:arch:Architecture.md:docs/` |
| `MOD:` | Module | `MOD:name:path:status` | `MOD:VoiceOS:Modules/VoiceOS:active` |

### 3.3 Voice Command Prefixes (AVU:VOICE)

| Prefix | Purpose | Format | Example |
|--------|---------|--------|---------|
| `CMD:` | Command | `CMD:action:phrase:syn1,syn2,...` | `CMD:SCROLL_UP:scroll up:page up,go up` |
| `CAT:` | Category | `CAT:id:name:description` | `CAT:nav:Navigation:Movement commands` |
| `LOC:` | Locale | `LOC:code:name` | `LOC:en-US:English (US)` |
| `PRI:` | Priority | `PRI:action:level` | `PRI:SCROLL_UP:50` |
| `FLG:` | Flag | `FLG:action:flag:value` | `FLG:SCROLL_UP:contextual:true` |

### 3.4 Theme Prefixes (AVU:THEME)

| Prefix | Purpose | Format | Example |
|--------|---------|--------|---------|
| `THM:` | Theme meta | `THM:name:version` | `THM:Dark Mode:1.0.0` |
| `PAL:` | Palette color | `PAL:key:#hexcolor` | `PAL:primary:#007AFF` |
| `TYP:` | Typography | `TYP:style:size:weight:family` | `TYP:h1:28:bold:system` |
| `SPC:` | Spacing | `SPC:xs:v:sm:v:md:v:lg:v:xl:v` | `SPC:xs:4:sm:8:md:16:lg:24:xl:32` |
| `EFX:` | Effects | `EFX:type:enabled:k1:v1:k2:v2` | `EFX:shadow:true:blur:8:elevation:4` |
| `CMP:` | Component style | `CMP:name:k1:v1:k2:v2` | `CMP:button:radius:8:padding:12` |
| `ANI:` | Animation | `ANI:name:duration:easing` | `ANI:fade:300:easeInOut` |

### 3.5 State/Data Prefixes (AVU:STATE, AVU:DATA)

| Prefix | Purpose | Format | Example |
|--------|---------|--------|---------|
| `APP:` | App info | `APP:id:name:version` | `APP:voiceos:VoiceOS:4.0.0` |
| `STA:` | State entry | `STA:key:value:type` | `STA:screen:home:string` |
| `SCR:` | Screen | `SCR:id:name:parent` | `SCR:settings:Settings:home` |
| `ELM:` | Element | `ELM:vuid:type:text:bounds` | `ELM:abc123:button:Submit:0,0,100,40` |
| `NAV:` | Navigation | `NAV:from:to:action` | `NAV:home:settings:tap` |
| `USR:` | User data | `USR:key:value` | `USR:name:John` |
| `CTX:` | Context | `CTX:key:value` | `CTX:mode:dark` |

### 3.6 IPC/Exchange Prefixes (AVU:IPC)

| Prefix | Purpose | Format | Example |
|--------|---------|--------|---------|
| `REQ:` | Request | `REQ:id:action:payload` | `REQ:r001:speak:Hello` |
| `RES:` | Response | `RES:id:status:payload` | `RES:r001:ok:done` |
| `EVT:` | Event | `EVT:type:source:data` | `EVT:click:btn1:{}` |
| `ERR:` | Error | `ERR:code:message` | `ERR:404:Not found` |
| `ACK:` | Acknowledgment | `ACK:id:timestamp` | `ACK:r001:1705312800000` |

### 3.7 Handover Prefixes (AVU:HANDOVER)

| Prefix | Purpose | Format |
|--------|---------|--------|
| `ARC:` | Architecture | `ARC:topic:description` |
| `WIP:` | Work in progress | `WIP:task:status:notes` |
| `BLK:` | Blocker | `BLK:issue:severity:notes` |
| `DEC:` | Decision | `DEC:topic:choice:rationale` |
| `LEA:` | Learning | `LEA:insight:context` |
| `TSK:` | Task | `TSK:id:description:status` |
| `BUG:` | Known bug | `BUG:id:description:workaround` |

### 3.8 WebSocket/Sync Prefixes (AVU:SYNC)

| Prefix | Purpose | Format | Example |
|--------|---------|--------|---------|
| `PNG:` | Ping keep-alive | `PNG:sessionId:timestamp` | `PNG:sess_001:1705312800000` |
| `PON:` | Pong response | `PON:sessionId:timestamp` | `PON:sess_001:1705312800001` |
| `HND:` | Handshake | `HND:sessionId:deviceId:appVersion:platform:userId` | `HND:sess_001:dev_001:1.0.0:Android:user_001` |
| `CAP:` | Capability | `CAP:sessionId:cap1,cap2,cap3` | `CAP:sess_001:tabs,favorites,settings` |
| `SCR:` | Sync Create | `SCR:msgId:entityType:entityId:version:data` | `SCR:msg_001:TAB:tab_001:1:escaped_json` |
| `SUP:` | Sync Update | `SUP:msgId:entityType:entityId:version:data` | `SUP:msg_002:FAV:fav_001:2:escaped_json` |
| `SDL:` | Sync Delete | `SDL:msgId:entityType:entityId` | `SDL:msg_003:HST:hist_001` |
| `SFL:` | Sync Full Request | `SFL:msgId:entityTypes:lastSyncTimestamp` | `SFL:msg_004:TAB,FAV,SET:1705312800000` |
| `SBT:` | Sync Batch | `SBT:msgId:count:op1\|op2\|op3` | `SBT:msg_005:2:TAB,t1,CREATE,1,data\|FAV,f1,UPDATE,2,data` |
| `SRS:` | Sync Response | `SRS:msgId:entityType:entityId:version:data` | `SRS:msg_006:TAB:tab_001:1:escaped_json` |
| `SCF:` | Sync Conflict | `SCF:msgId:entityType:entityId:localVer:remoteVer:resolution` | `SCF:msg_007:TAB:tab_001:1:2:REMOTE` |
| `SST:` | Sync Status | `SST:sessionId:state:pendingCount:lastSync` | `SST:sess_001:SYNCING:5:1705312800000` |
| `CON:` | Connected | `CON:sessionId:serverVersion:timestamp` | `CON:sess_001:2.0.0:1705312800000` |
| `DIS:` | Disconnected | `DIS:sessionId:reason:timestamp` | `DIS:sess_001:Server closed:1705312800000` |
| `RCN:` | Reconnecting | `RCN:sessionId:attempt:maxAttempts:delayMs` | `RCN:sess_001:3:5:4000` |

#### Entity Type Identifiers

| ID | Entity Type | Description |
|----|-------------|-------------|
| `TAB` | Tab | Browser tabs |
| `FAV` | Favorite | Bookmarks/favorites |
| `HST` | History | Browsing history |
| `DWN` | Download | Downloads |
| `SET` | Settings | User settings |
| `SES` | Session | Browser sessions |

#### Sync State Values

| State | Description |
|-------|-------------|
| `IDLE` | No sync activity |
| `SYNCING` | Sync in progress |
| `CONNECTING` | Establishing connection |
| `CONNECTED` | Connection established |
| `DISCONNECTED` | Connection lost |
| `ERROR` | Sync error occurred |
| `OFFLINE` | Device offline |

#### Conflict Resolution Values

| Resolution | Description |
|------------|-------------|
| `LOCAL` | Keep local version |
| `REMOTE` | Accept remote version |
| `MERGE` | Merge both versions |
| `MANUAL` | Requires user decision |

---

## 4. Type Detection

### 4.1 Header-Based Detection

Primary detection via header:
```
# Type: CONFIG   → AVU:CONFIG
# Type: VOICE    → AVU:VOICE
# Type: THEME    → AVU:THEME
# Type: STATE    → AVU:STATE
# Type: DATA     → AVU:DATA
# Type: IPC      → AVU:IPC
# Type: HANDOVER → AVU:HANDOVER
# Type: SYNC     → AVU:SYNC
```

### 4.2 Prefix-Based Fallback

If header missing, detect by first data prefix:
```kotlin
fun detectType(firstPrefix: String): AvuType = when(firstPrefix) {
    "PRJ", "CFG", "PRF", "GAT" -> AvuType.CONFIG
    "CMD", "CAT", "LOC" -> AvuType.VOICE
    "THM", "PAL", "TYP" -> AvuType.THEME
    "APP", "STA", "ELM" -> AvuType.STATE
    "REQ", "RES", "EVT" -> AvuType.IPC
    "ARC", "WIP", "BLK", "DEC" -> AvuType.HANDOVER
    "PNG", "PON", "HND", "SCR", "SUP", "SDL", "SFL", "SBT", "SRS", "SCF", "SST", "CON", "DIS", "RCN" -> AvuType.SYNC
    else -> AvuType.DATA
}
```

### 4.3 Extension Mapping (Legacy Support)

| Extension | Maps To | Notes |
|-----------|---------|-------|
| `.avu` | Auto-detect | Universal |
| `.idc` | AVU:CONFIG | Legacy IDEACODE |
| `.vos` | AVU:VOICE | Legacy VoiceOS |
| `.amf` | AVU:THEME | Legacy AVAMagic |
| `.ava` | AVU:VOICE | AVA intents |
| `.avc` | AVU:IPC | AvaConnect |
| `.awb` | AVU:VOICE | WebAvanue browser |
| `.ami` | AVU:THEME | MagicUI |
| `.amc` | AVU:DATA | MagicCode |
| `.hov` | AVU:HANDOVER | Handover |

---

## 5. Examples

### 5.1 Configuration File (.avu / .idc)

```
# AVU Format v1.0
# Type: CONFIG
---
schema: avu-cfg-1.0
version: 12.0.0
project: newavanues
metadata:
  file: config.avu
  category: configuration
---
PRJ:nav:NewAvanues:monorepo:12.0.0
CFG:framework:/Volumes/M-Drive/Coding/ideacode:12.0.0
CFG:voice_first:true:bool
CFG:auto_verify:true:bool
PRF:default:50:40:true:false
PRF:strict:30:30:true:true
GAT:test_coverage:90:true
GAT:ipc_coverage:100:true
THR:technical_debt:70:warn
THR:test_coverage:90:enforce
MOD:VoiceOS:Modules/VoiceOS:active
MOD:AVA:Modules/AVA:active
MOD:WebAvanue:Modules/WebAvanue:active
---
aliases:
  cfg: [config, configuration]
  prj: [project]
```

### 5.2 Voice Commands File (.avu / .vos)

```
# AVU Format v1.0
# Type: VOICE
---
schema: avu-vox-1.0
version: 1.0.0
locale: en-US
metadata:
  file: browser-commands.avu
  category: browser
  display_name: Browser Control
  command_count: 5
---
CAT:browser:Browser Control:Web browser voice commands
CMD:SCROLL_UP:scroll up:page up,go up,move up,scroll upward
CMD:SCROLL_DOWN:scroll down:page down,go down,move down,scroll downward
CMD:SCROLL_TOP:top of page:scroll to top,jump to top,go to top
CMD:SCROLL_BOTTOM:bottom of page:scroll to bottom,jump to bottom
CMD:NEW_TAB:new tab:open new tab,create tab,add tab
---
synonyms:
  scroll: [move, navigate, go]
  page: [screen, view, document]
```

### 5.3 Theme File (.avu / .amf)

```
# AVU Format v1.0
# Type: THEME
---
schema: avu-thm-1.0
version: 1.0.0
metadata:
  name: Dark Mode
  author: Augmentalis
---
THM:Dark Mode:1.0.0
PAL:primary:#007AFF
PAL:secondary:#5AC8FA
PAL:background:#000000
PAL:surface:#1C1C1E
PAL:error:#FF3B30
PAL:onPrimary:#FFFFFF
PAL:onSecondary:#FFFFFF
PAL:onBackground:#FFFFFF
PAL:onSurface:#FFFFFF
PAL:onError:#FFFFFF
TYP:h1:28:bold:system
TYP:h2:22:bold:system
TYP:body:16:regular:system
TYP:caption:12:regular:system
SPC:xs:4:sm:8:md:16:lg:24:xl:32
EFX:shadow:true:blur:8:elevation:4
CMP:button:radius:8:padding:12:minHeight:44
ANI:fade:300:easeInOut
```

### 5.4 Layout File (.avu / .amf)

```
# AVU Format v1.0
# Type: LAYOUT
---
schema: avu-lyt-1.0
version: 1.0.0
metadata:
  name: Settings Screen
  platform: android
---
LYT:SettingsScreen:1.0.0
COL:root:1.0:start
  ROW:header:0:center
    IMG:logo:ic_settings:contain
    TXT:title:Settings:h1
  END:header
  SCR:content:vertical:start
    ROW:theme_row:0:spaceBetween
      TXT:theme_label:Theme:body
      BTN:theme_toggle:Dark:toggle_theme
    END:theme_row
    ROW:volume_row:0:spaceBetween
      TXT:volume_label:Volume:body
      TXT:volume_value:75%:body
    END:volume_row
    GRD:quick_settings:2:8
      BTN:wifi:WiFi:toggle_wifi
      BTN:bluetooth:Bluetooth:toggle_bt
      BTN:airplane:Airplane:toggle_airplane
      BTN:dnd:Do Not Disturb:toggle_dnd
    END:quick_settings
  END:content
END:root
```

### 5.5 State Exchange File (.avu)

```
# AVU Format v1.0
# Type: STATE
---
schema: avu-sta-1.0
version: 1.0.0
app: voiceos
---
APP:voiceos:VoiceOS:4.0.0
STA:screen:settings:string
STA:theme:dark:string
STA:volume:75:int
SCR:settings:Settings:home
SCR:general:General Settings:settings
ELM:vuid_001:button:Save:100,500,200,44
ELM:vuid_002:switch:Dark Mode:100,300,60,32
NAV:home:settings:tap
NAV:settings:general:tap
CTX:mode:edit
CTX:user:authenticated
```

### 5.6 Handover File (.avu / .hov)

```
# AVU Format v1.0
# Type: HANDOVER
---
schema: avu-hov-1.0
version: 1.0.0
session: claude-20260115-001
terminal: NewAvanues__main__t58642
---
ARC:format:Unified AVU format consolidates IDC, VOS, AMF into single spec
WIP:parser:Implementing UniversalAvuParser:80% complete
BLK:tests:Need test coverage for all format types:high
DEC:extension:Using .avu as universal extension:simplifies tooling
LEA:parsing:Line-based format 50% faster than JSON parsing
TSK:t001:Convert 100+ .vos files to .avu:pending
TSK:t002:Update all parsers:in_progress
BUG:b001:Legacy .vos files missing header:add fallback detection
```

---

## 6. Size Comparison

### 6.1 Voice Command (7 commands)

| Format | Size | Comparison |
|--------|------|------------|
| JSON (current .vos) | 1,847 bytes | Baseline |
| AVU (proposed) | 892 bytes | **52% smaller** |
| YAML | 1,234 bytes | 33% smaller |

### 6.2 Theme Definition

| Format | Size | Comparison |
|--------|------|------------|
| JSON | 2,156 bytes | Baseline |
| AVU | 987 bytes | **54% smaller** |
| YAML | 1,567 bytes | 27% smaller |

---

## 7. Parser Implementation

### 7.1 Unified Parser Interface

```kotlin
interface AvuParser {
    fun parse(content: String): AvuDocument
    fun serialize(document: AvuDocument): String
    fun detectType(content: String): AvuType
    fun validate(content: String): ValidationResult
}
```

### 7.2 Entry Parsing

```kotlin
fun parseEntry(line: String): AvuEntry {
    val parts = line.split(":", limit = 3)
    require(parts.size >= 2) { "Invalid entry: $line" }
    return AvuEntry(
        prefix = parts[0],
        id = parts[1],
        data = if (parts.size > 2) parts[2] else ""
    )
}
```

### 7.3 Synonym Parsing

```kotlin
fun parseSynonyms(data: String): List<String> {
    return data.split(",").map { it.trim() }
}
```

---

## 8. Migration Guide

### 8.1 JSON to AVU Conversion

**Before (JSON .vos):**
```json
{
  "action": "SCROLL_UP",
  "cmd": "scroll up",
  "syn": ["page up", "go up", "move up"]
}
```

**After (AVU):**
```
CMD:SCROLL_UP:scroll up:page up,go up,move up
```

### 8.2 Backward Compatibility

1. Parsers MUST detect format by extension first
2. If `.avu`, detect type by header
3. If header missing, detect by first prefix
4. Support legacy JSON .vos files with separate parser path

---

## 9. Files Requiring Updates

### 9.1 High Priority (Parser Core)

| File | Change Required |
|------|-----------------|
| `UniversalFileParser.kt` | Add AVU type detection, unified schema |
| `VOSFileParser.kt` | Add AVU format support alongside JSON |
| `AmfThemeParser.kt` | Align with AVU:THEME schema |
| `VosFile.kt` | Update FileType enum |

### 9.2 Medium Priority (Consumers)

| File | Change Required |
|------|-----------------|
| `VOSCommandIngestion.kt` | Recognize .avu extension |
| `ThemeIO.kt` | Add AVU export option |
| `AvaFileParser.kt` | Update schema detection |
| `AssetManager.kt` | Handle .avu asset files |

### 9.3 Asset Conversion (100+ files)

| Location | Files | Action |
|----------|-------|--------|
| `CommandManager/assets/commands/` | 96 .vos | Convert to .avu |
| `WebAvanue/docs/` | 4 .vos | Convert to .avu |
| `.claude/` | 3 .idc | Update schema version |

---

## 10. Appendix

### 10.1 Reserved Prefixes

These prefixes are reserved for future use:
- `EXT:` - Extension data
- `PLG:` - Plugin definition
- `SEC:` - Security/encryption
- `SIG:` - Digital signature
- `CRC:` - Checksum

### 10.2 Character Encoding

- Files MUST be UTF-8 encoded
- Lines MUST use LF (`\n`) line endings
- Colons in data values MUST be preserved (parser uses `limit` on split)

### 10.3 Maximum Limits

| Limit | Value |
|-------|-------|
| Max line length | 4096 characters |
| Max file size | 10 MB |
| Max entries per file | 100,000 |
| Max synonym count | 50 per command |

---

## 11. Extending the Format

### 11.1 Adding New Prefixes

To add a new 3-character prefix to the AVU format:

#### Step 1: Choose a Prefix

| Rule | Requirement | Example |
|------|-------------|---------|
| Length | Exactly 3 uppercase characters | `PLG`, `SEC`, `WGT` |
| Unique | Must not conflict with existing prefixes | Check Section 3 |
| Meaningful | Should be an abbreviation of the purpose | `PLG` = Plugin |
| Category | Assign to appropriate type (CONFIG, VOICE, etc.) | THEME type for UI |

#### Step 2: Define the Format

Document the prefix format:
```
| Prefix | Purpose | Format | Example |
|--------|---------|--------|---------|
| `NEW:` | Description | `NEW:param1:param2:param3` | `NEW:value1:value2:value3` |
```

#### Step 3: Update the Registry

Add to `PREFIX-REGISTRY.md`:
```markdown
### NEW: - New Feature Name
- **Category:** AVU:{TYPE}
- **Added:** YYYY-MM-DD
- **Version:** 1.x.0
- **Format:** `NEW:param1:param2:param3`
- **Parameters:**
  - `param1` (required): Description
  - `param2` (optional): Description
- **Example:** `NEW:example:data:here`
```

#### Step 4: Update Parser

Add to `UniversalAvuParser.kt`:
```kotlin
// In parseEntry() when clause:
"NEW" -> parseNewEntry(parts)

// New parsing function:
private fun parseNewEntry(parts: List<String>): NewEntry {
    require(parts.size >= 3) { "NEW requires at least 2 parameters" }
    return NewEntry(
        param1 = parts[1],
        param2 = parts.getOrNull(2) ?: "",
        param3 = parts.getOrNull(3) ?: ""
    )
}
```

#### Step 5: Add Tests

```kotlin
@Test
fun `parse NEW prefix`() {
    val line = "NEW:value1:value2:value3"
    val entry = parser.parseEntry(line)
    assertEquals("NEW", entry.prefix)
    assertEquals("value1", entry.param1)
}
```

### 11.2 Prefix Naming Conventions

| Category | Prefix Pattern | Examples |
|----------|----------------|----------|
| Configuration | 3 consonants | `CFG`, `PRJ`, `MOD` |
| Voice/Commands | Action-related | `CMD`, `SYN`, `CAT` |
| Theme/UI | Visual-related | `PAL`, `TYP`, `EFX` |
| State/Data | State-related | `STA`, `CTX`, `USR` |
| IPC/Exchange | Protocol-related | `REQ`, `RES`, `EVT` |

### 11.3 Reserved Prefix Ranges

| Range | Reserved For | Notes |
|-------|--------------|-------|
| `A**` | App/AVA prefixes | `APP`, `AVA`, `ARC`, `ANI`, `ACK` |
| `B**` | Browser/Block | `BLK`, `BUG` |
| `C**` | Config/Command | `CFG`, `CMD`, `CAT`, `CMP`, `CTX`, `CMT` |
| `D**` | Data/Decision | `DEC`, `DAT` |
| `E**` | Element/Effect/Event | `ELM`, `EFX`, `EVT`, `ERR`, `EXT` |
| `F**` | File/Flag | `FNM`, `FLG` |
| `G**` | Gate/Global | `GAT` |
| `H**` | Handover | `HOV` |
| `L**` | Learning/Locale | `LEA`, `LOC` |
| `M**` | Module/Metadata | `MOD`, `MET` |
| `N**` | Navigation | `NAV` |
| `P**` | Project/Palette/Path/Priority | `PRJ`, `PAL`, `PTH`, `PRI`, `PRF`, `PLG` |
| `R**` | Registry/Request/Response/Reference | `REG`, `REQ`, `RES`, `REF` |
| `S**` | State/Screen/Spacing/Swarm/Synonym | `STA`, `SCR`, `SPC`, `SWM`, `SYN`, `SEC`, `SIG` |
| `T**` | Theme/Type/Threshold/Task | `THM`, `TYP`, `THR`, `TSK` |
| `U**` | User | `USR` |
| `V**` | Version | `VER` |
| `W**` | Work/Widget | `WIP`, `WGT` |
| `X**` | Exchange | `XCH` |
| `Z**` | Reserved for future | - |

### 11.4 Creating a New AVU Type

To create an entirely new AVU type (like AVU:PLUGIN):

#### Step 1: Define the Type

```kotlin
enum class AvuType {
    CONFIG,
    VOICE,
    THEME,
    STATE,
    DATA,
    IPC,
    HANDOVER,
    PLUGIN  // New type
}
```

#### Step 2: Create Schema

Define schema identifier: `avu-plg-1.0`

#### Step 3: Define Type-Specific Prefixes

```markdown
### Plugin Prefixes (AVU:PLUGIN)

| Prefix | Purpose | Format |
|--------|---------|--------|
| `PLG:` | Plugin meta | `PLG:id:name:version` |
| `DEP:` | Dependency | `DEP:plugin_id:version_range` |
| `PRM:` | Permission | `PRM:permission:required` |
| `HKS:` | Hooks | `HKS:event:handler` |
| `API:` | API endpoint | `API:method:path:handler` |
```

#### Step 4: Update Type Detection

```kotlin
fun detectType(firstPrefix: String): AvuType = when(firstPrefix) {
    // ... existing cases ...
    "PLG", "DEP", "PRM", "HKS", "API" -> AvuType.PLUGIN
    else -> AvuType.DATA
}
```

### 11.5 Deprecating Prefixes

When deprecating a prefix:

1. Mark as deprecated in registry with version number
2. Add warning to parser (don't fail)
3. Document migration path
4. Remove in next major version

```kotlin
// In parser:
"OLD" -> {
    log.warn("OLD prefix deprecated in v1.2.0, use NEW instead")
    parseOldEntry(parts) // Still parse for backward compatibility
}
```

### 11.6 Prefix Validation Rules

```kotlin
object PrefixValidator {
    private val VALID_PATTERN = Regex("^[A-Z]{3}$")

    fun validate(prefix: String): ValidationResult {
        return when {
            !prefix.matches(VALID_PATTERN) ->
                ValidationResult.Error("Prefix must be exactly 3 uppercase letters")
            prefix in RESERVED_PREFIXES ->
                ValidationResult.Error("Prefix '$prefix' is reserved")
            prefix in EXISTING_PREFIXES ->
                ValidationResult.Error("Prefix '$prefix' already exists")
            else ->
                ValidationResult.Ok
        }
    }
}
```

---

**Document Version:** 1.1.0
**Last Updated:** 2026-02-03
**Status:** Draft - Added WebSocket/Sync Prefixes (Section 3.8)
