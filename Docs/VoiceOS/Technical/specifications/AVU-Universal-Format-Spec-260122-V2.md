# AVU Universal Format Specification

**Format**: AVU (Avanues Universal Format)
**Schema**: `avu-2.2`
**Version**: 2.2.0
**Date**: 2026-02-03

---

## Overview

The AVU (Avanues Universal Format) is a compact, line-based format used across all Avanues projects. This specification defines the universal format structure and all registered RPC (Remote Procedure Call) codes.

## Design Principles

1. **Compact**: 60-80% smaller than JSON
2. **Human-Readable**: Easy to inspect and debug
3. **Self-Documenting**: Optional codes legend in header (NEW in v2.2)
4. **Cross-Compatible**: Readable by all Avanues projects
5. **Extensible**: Easy to add new codes without breaking parsers
6. **Type-Safe**: 3-letter codes ensure unique identification

## Format Structure

```
# Avanues Universal Format v2.2
# Type: <TYPE>
---
schema: avu-2.2
version: <version>
project: <project>
metadata:
  file: <filename>
  category: <category>
  count: <total_items>
codes:                              # NEW in v2.2 - Self-documenting legend
  SCR: Sync Create (msgId:entityType:entityId:version:data)
  SUP: Sync Update (msgId:entityType:entityId:version:data)
---
<CODE>:<field1>:<field2>:...
<CODE>:<field1>:<field2>:...
...
```

## Escape Characters

Reserved characters must be escaped using percent-encoding:

| Character | Encoded | Description |
|-----------|---------|-------------|
| `%` | `%25` | Escape character (MUST escape first) |
| `:` | `%3A` | Field delimiter |
| `\n` | `%0A` | Line feed |
| `\r` | `%0D` | Carriage return |

**IMPORTANT**: Order matters for encoding/decoding:
- When escaping: `%` MUST be escaped FIRST (otherwise `%3A` becomes `%253A`)
- When unescaping: `%` MUST be unescaped LAST (to correctly decode `%25`)

### Canonical Implementation

Use `AvuEscape` from `avucodec.core` for all escape operations:

```kotlin
import com.augmentalis.avucodec.core.AvuEscape

val escaped = AvuEscape.escape("https://example.com:8080")
// Result: "https%3A//example.com%3A8080"

val original = AvuEscape.unescape(escaped)
// Result: "https://example.com:8080"
```

## RPC Code Categories

### 1. Core RPC Codes (Communication)

| Code | Purpose | Format | Example |
|------|---------|--------|---------|
| `VCM` | Voice Command | `VCM:id:action:params...` | `VCM:cmd123:SCROLL_TOP` |
| `ACC` | Accept | `ACC:requestId` | `ACC:req456` |
| `ACD` | Accept with Data | `ACD:requestId:data` | `ACD:req456:success` |
| `DEC` | Decline | `DEC:requestId` | `DEC:req456` |
| `DCR` | Decline with Reason | `DCR:requestId:reason` | `DCR:req456:busy` |
| `BSY` | Busy | `BSY:requestId` | `BSY:req456` |
| `BCF` | Busy Callback | `BCF:requestId:callbackUrl` | `BCF:req456:http://...` |
| `ERR` | Error | `ERR:requestId:code:message` | `ERR:req456:500:Internal error` |
| `CHT` | Chat | `CHT:msgId:senderId:text` | `CHT:msg1:user1:Hello` |
| `URL` | URL Share | `URL:sessionId:url` | `URL:sess1:https://...` |
| `NAV` | Navigate | `NAV:sessionId:url` | `NAV:sess1:https://...` |
| `AIQ` | AI Query | `AIQ:queryId:query:context?` | `AIQ:q1:What is...` |
| `AIR` | AI Response | `AIR:queryId:response:confidence?` | `AIR:q1:The answer...:0.95` |
| `JSN` | JSON Wrapper | `JSN:requestId:json` | `JSN:req1:{...}` |
| `STT` | Speech to Text | `STT:sessionId:transcript:confidence:isFinal` | `STT:s1:hello:0.9:true` |
| `CON` | Connected | `CON:sessionId` | `CON:sess1` |
| `DIS` | Disconnected | `DIS:sessionId` | `DIS:sess1` |
| `HND` | Handshake | `HND:sessionId:appId:version` | `HND:s1:com.app:1.0.0` |
| `PNG` | Ping | `PNG:sessionId:timestamp` | `PNG:s1:1706000000000` |
| `PON` | Pong | `PON:sessionId:timestamp` | `PON:s1:1706000000000` |
| `CAP` | Capability | `CAP:sessionId:cap1,cap2,...` | `CAP:s1:voice,gaze` |

### 2. VoiceOS Codes (Learned Apps)

| Code | Purpose | Format | Example |
|------|---------|--------|---------|
| `APP` | App Metadata | `APP:package:name:timestamp` | `APP:com.app:App:1706000000` |
| `STA` | Statistics | `STA:screens:elements:commands:avgDepth:maxDepth:coverage` | `STA:5:23:8:2.5:4:75.5` |
| `SCR` | Screen | `SCR:hash:activity:timestamp:elementCount` | `SCR:abc123:MainActivity:1706000000:5` |
| `ELM` | Element | `ELM:avid:label:type:actions:bounds:category` | `ELM:btn1:Settings:BUTTON:click:0,0,100,50:action` |
| `CMD` | Command | `CMD:avid:phrase:actionType:targetAvid:confidence` | `CMD:cmd1:settings:CLICK:btn1:0.95` |

### 3. Plugin Manifest Codes (v2.0)

| Code | Purpose | Format | Example |
|------|---------|--------|---------|
| `PLG` | Plugin Header | `PLG:id:version:entrypoint:name` | `PLG:com.example.plugin:1.0.0:com.example.Plugin:My Plugin` |
| `DSC` | Description | `DSC:description text` | `DSC:Enhanced voice recognition` |
| `AUT` | Author | `AUT:name:email:url` | `AUT:Example Corp:dev@example.com:https://example.com` |
| `PCP` | Plugin Capabilities | `PCP:cap1\|cap2\|cap3` | `PCP:accessibility.voice\|ai.nlu` |
| `MOD` | Target Modules | `MOD:module1\|module2` | `MOD:VoiceOSCore\|AI` |
| `DEP` | Dependency | `DEP:pluginId:versionConstraint` | `DEP:com.augmentalis.core:^2.0.0` |
| `PRM` | Permission | `PRM:permission:rationale` | `PRM:MICROPHONE:Voice input` |
| `PLT` | Platform | `PLT:platform:minVersion` | `PLT:android:26` |
| `AST` | Asset | `AST:type:path` | `AST:model:models/vocab.onnx` |
| `CFG` | Config Block | `CFG:start` or `CFG:end` | `CFG:start` |
| `KEY` | Config Key | `KEY:name:type:default:description` | `KEY:sensitivity:float:0.8:Detection threshold` |
| `HKS` | Hook | `HKS:event:handler` | `HKS:on_voice_command:handleVoice` |

### 4. Handover Codes (v2.2)

Handover files (`.hov`) transfer project state between sessions or agents. Some codes overlap with other categories — the file `type` in the header determines interpretation.

| Code | Purpose | Format | Example |
|------|---------|--------|---------|
| `ARC` | Architecture | `ARC:key:value\|qualifiers` | `ARC:pattern:KMP monorepo\|shared code` |
| `DEC` | Decision | `DEC:id:rationale\|qualifiers` | `DEC:d001:No typealiases\|user enforced` |
| `STA` | State | `STA:key:value\|qualifiers` | `STA:phase:5\|theme consolidation` |
| `WIP` | Work In Progress | `WIP:id:description:status` | `WIP:w001:Logger migration:70%` |
| `BLK` | Blocker | `BLK:id:description:impact` | `BLK:b001:Desktop build fails:blocks testing` |
| `TSK` | Task | `TSK:id:description:status` | `TSK:t001:Merge OceanTheme:pending` |
| `FIL` | Files | `FIL:path1,path2,...` | `FIL:AvanueUI/AvanueTheme.kt,AvanueUI/OceanTheme.kt` |
| `MOD` | Module | `MOD:name\|description` | `MOD:AvanueUI\|Unified theme+components` |
| `DEP` | Dependency | `DEP:name:version` | `DEP:Foundation:1.0.0` |
| `LEA` | Learning | `LEA:topic:insight` | `LEA:KMP iOS:Pure Kotlin SHA-256 needed` |
| `BUG` | Known Bug | `BUG:id:description:workaround` | `BUG:b001:Desktop unresolved refs:Android-only verify` |
| `REF` | Reference | `REF:uri:description` | `REF:Chapter88:Consolidated App guide` |
| `CFG` | Config | `CFG:key:value` | `CFG:minSdk:29` |
| `API` | API Change | `API:endpoint:old:new` | `API:import:com.ava.theme:com.avanueui` |
| `CTX` | Context | `CTX:key:value` | `CTX:branch:060226-1-consolidation-framework` |
| `PRI` | Priority | `PRI:level:description` | `PRI:1:Theme consolidation before app launch` |

### 5. WebSocket/Sync Codes (v2.1)

| Code | Purpose | Format | Example |
|------|---------|--------|---------|
| `PNG` | Ping (keep-alive) | `PNG:sessionId:timestamp` | `PNG:sess_001:1705312800000` |
| `PON` | Pong (response) | `PON:sessionId:timestamp` | `PON:sess_001:1705312800001` |
| `HND` | Handshake | `HND:sessionId:deviceId:appVersion:platform:userId?` | `HND:sess_001:dev_001:1.0.0:Android:user_001` |
| `CAP` | Capability | `CAP:sessionId:cap1,cap2,cap3` | `CAP:sess_001:tabs,favorites,settings` |
| `SCR` | Sync Create | `SCR:msgId:entityType:entityId:version:data` | `SCR:msg_001:TAB:tab_001:1:escaped_data` |
| `SUP` | Sync Update | `SUP:msgId:entityType:entityId:version:data` | `SUP:msg_002:FAV:fav_001:2:escaped_data` |
| `SDL` | Sync Delete | `SDL:msgId:entityType:entityId` | `SDL:msg_003:HST:hist_001` |
| `SFL` | Sync Full Request | `SFL:msgId:entityTypes:lastSyncTimestamp` | `SFL:msg_004:TAB,FAV,SET:1705312800000` |
| `SBT` | Sync Batch | `SBT:msgId:count:op1\|op2\|op3` | `SBT:msg_005:2:TAB,t1,CREATE,1,data` |
| `SRS` | Sync Response | `SRS:msgId:entityType:entityId:version:data` | `SRS:msg_006:TAB:tab_001:1:escaped_data` |
| `SCF` | Sync Conflict | `SCF:msgId:entityType:entityId:localVer:remoteVer:resolution` | `SCF:msg_007:TAB:tab_001:1:2:REMOTE` |
| `SST` | Sync Status | `SST:sessionId:state:pendingCount:lastSync` | `SST:sess_001:SYNCING:5:1705312800000` |
| `CON` | Connected | `CON:sessionId:serverVersion:timestamp` | `CON:sess_001:2.0.0:1705312800000` |
| `DIS` | Disconnected | `DIS:sessionId:reason:timestamp` | `DIS:sess_001:Server closed:1705312800000` |
| `RCN` | Reconnecting | `RCN:sessionId:attempt:maxAttempts:delayMs` | `RCN:sess_001:3:5:4000` |

#### Sync Entity Types

| ID | Entity | Description |
|----|--------|-------------|
| `TAB` | Tab | Browser tabs |
| `FAV` | Favorite | Bookmarks/favorites |
| `HST` | History | Browsing history |
| `DWN` | Download | Downloads |
| `SET` | Settings | User settings |
| `SES` | Session | Browser sessions |

#### Sync States

| State | Description |
|-------|-------------|
| `IDLE` | No sync activity |
| `SYNCING` | Sync in progress |
| `CONNECTING` | Establishing connection |
| `CONNECTED` | Connection established |
| `DISCONNECTED` | Connection lost |
| `ERROR` | Sync error occurred |
| `OFFLINE` | Device offline |

#### Conflict Resolutions

| Resolution | Description |
|------------|-------------|
| `LOCAL` | Keep local version |
| `REMOTE` | Accept remote version |
| `MERGE` | Merge both versions |
| `MANUAL` | Requires user decision |

#### Batch Operation Format

Each operation in `SBT` batch uses comma-separated fields:
```
entityType,entityId,action,version,data
```

Actions: `CREATE`, `UPDATE`, `DELETE`

Example batch:
```
SBT:msg_005:3:TAB,tab1,CREATE,1,{...}|FAV,fav1,UPDATE,2,{...}|HST,hist1,DELETE,1,
```

---

## Self-Documenting Headers (NEW in v2.2)

AVU files can include a `codes:` section in the header that documents the codes used in the file. This makes files self-documenting for both humans and AI.

### Using AvuCodeRegistry

```kotlin
import com.augmentalis.avucodec.core.AvuCodeRegistry
import com.augmentalis.avucodec.core.AvuCodeInfo
import com.augmentalis.avucodec.core.AvuCodeCategory

// Register codes at module initialization
AvuCodeRegistry.register(AvuCodeInfo(
    code = "SCR",
    name = "Sync Create",
    category = AvuCodeCategory.SYNC,
    format = "msgId:entityType:entityId:version:data",
    description = "Create a new entity on remote"
))

// Generate legend for file header
val legend = AvuCodeRegistry.generateLegend(
    filter = setOf("SCR", "SUP", "SDL"),
    includeDescriptions = true
)
```

### Using AvuHeader

```kotlin
import com.augmentalis.avucodec.core.AvuHeader

// Generate a complete header
val header = AvuHeader.generate(
    type = "WebSocket Sync",
    version = "1.0.0",
    project = "webavanue",
    codes = setOf("SCR", "SUP", "SDL", "PNG", "PON")
)

// Parse a header
val (headerData, bodyStart) = AvuHeader.parse(fileContent)
println(headerData.codes)  // Map of code -> description
```

---

## Plugin Manifest Format

Plugin manifests use the `.avu` extension and follow this structure:

```
# Avanues Universal Plugin Format v1.0
# Type: Plugin Manifest
# Extension: .avu

PLG:com.example.voiceenhancer:1.2.0:com.example.VoiceEnhancerPlugin:Voice Enhancer
DSC:Enhanced voice recognition for accessibility users with speech difficulties
AUT:Example Corp:contact@example.com:https://example.com

PCP:accessibility.voice|ai.nlu|speech.recognition
MOD:VoiceOSCore|SpeechRecognition|AI

DEP:com.augmentalis.core:^2.0.0
DEP:com.augmentalis.pluginsystem:^1.0.0

PRM:ACCESSIBILITY:Access UI elements for command generation
PRM:MICROPHONE:Voice input processing

PLT:android:26
PLT:ios:14.0
PLT:desktop:any

AST:model:models/custom_vocab.onnx
AST:config:config/defaults.avu
AST:locale:locales/en-US.avu

CFG:start
KEY:sensitivity:float:0.8:Voice detection sensitivity (0.0-1.0)
KEY:timeout_ms:int:5000:Command timeout in milliseconds
KEY:language:string:en-US:Recognition language
KEY:offline_mode:bool:true:Enable offline recognition
CFG:end

HKS:on_voice_command:classifyIntent
HKS:on_screen_change:suggestCommands
HKS:on_app_foreground:handleAppForeground
```

### Plugin Capability Strings

Standard capability identifiers:

| Category | Capability | Description |
|----------|------------|-------------|
| **Accessibility** | `accessibility.voice` | Voice control |
| | `accessibility.gaze` | Gaze/eye tracking |
| | `accessibility.switch` | Switch access |
| **AI** | `ai.llm` | Large language model |
| | `ai.nlu` | Natural language understanding |
| | `ai.nlp` | Natural language processing |
| | `ai.embedding` | Text embeddings |
| | `ai.rag` | Retrieval augmented generation |
| **Speech** | `speech.recognition` | Speech-to-text |
| | `speech.tts` | Text-to-speech |
| | `speech.wakeword` | Wake word detection |
| **UI** | `ui.overlay` | Screen overlays |
| | `ui.theme` | Theme customization |
| | `ui.component` | UI components |
| **Handler** | `handler.navigation` | Navigation commands |
| | `handler.ui` | UI interaction commands |
| | `handler.system` | System commands |
| | `handler.custom` | Custom command handlers |
| **Data** | `data.sync` | Data synchronization |
| | `data.export` | Data export |
| | `data.import` | Data import |

### Config Key Types

| Type | Description | Example Default |
|------|-------------|-----------------|
| `string` | Text value | `en-US` |
| `int` | Integer | `5000` |
| `float` | Floating point | `0.8` |
| `bool` | Boolean | `true` or `false` |
| `list` | Comma-separated list | `en-US,es-ES,fr-FR` |

### Version Constraints

Semver-compatible constraints:

| Constraint | Meaning |
|------------|---------|
| `*` | Any version |
| `1.0.0` | Exact version |
| `^1.0.0` | Compatible (>=1.0.0 <2.0.0) |
| `~1.0.0` | Patch updates (>=1.0.0 <1.1.0) |
| `>=1.0.0` | Greater or equal |
| `<2.0.0` | Less than |

---

## VoiceOS Learned App Format

Learned app files use the `.vos` extension:

```
# Avanues Universal Format v2.2
# Type: VOS
---
schema: avu-2.2
version: 1.0.0
locale: en-US
project: voiceos
metadata:
  file: com.instagram.android.vos
  category: learned_app
  count: 32
codes:
  APP: App Metadata (package:name:timestamp)
  ELM: Element (avid:label:type:actions:bounds:category)
  CMD: Command (avid:phrase:actionType:targetAvid:confidence)
---
APP:com.instagram.android:Instagram:1706000000000
STA:5:23:8:4.6:3:75.5
SCR:abc123:MainActivity:1706000000000:5
ELM:btn-settings:Settings:BUTTON:click:100,200,300,250:action
ELM:btn-profile:Profile:IMAGE_BUTTON:click:350,200,450,250:action
NAV:abc123:def456:btn-settings:Settings:1706000000000
CMD:cmd1:open settings:CLICK:btn-settings:0.95
---
synonyms:
  settings: [preferences, options, config]
  back: [return, previous, go back]
```

---

## Handover Format

Handover files use the `.hov` extension for transferring project state:

```
# Avanues Universal Format v2.2
# Type: HOV (Handover)
---
schema: avu-2.2
version: 1.0.0
project: NewAvanues
module: architecture
metadata:
  file: architecture.hov
  category: handover
  created: 2026-02-07
  branch: 060226-1-consolidation-framework
  chunk: 1/4
codes:
  ARC: Architecture (key:value|qualifiers)
  DEC: Decision (id:rationale|qualifiers)
---
ARC:pattern:KMP monorepo|shared code Android+iOS+Desktop
ARC:modules:Foundation,Logging,AVU,AvanueUI,VoiceOSCore,WebAvanue
DEC:d001:No typealiases|user enforced|direct imports everywhere
DEC:d002:Flat modules over nested|Modules/AvanueUI not AVA/core/Theme
---
```

---

## Parsing Implementation

### Kotlin (AVUCodec Module)

```kotlin
import com.augmentalis.avucodec.AVUEncoder
import com.augmentalis.avucodec.AVUDecoder
import com.augmentalis.avucodec.core.AvuEscape
import com.augmentalis.avucodec.core.AvuHeader
import com.augmentalis.avucodec.core.AvuCodeRegistry

// Escape utilities (use for all field values)
val escaped = AvuEscape.escape("value:with:colons")
val original = AvuEscape.unescape(escaped)

// Encoding plugin manifest
val manifest = AVUEncoder.PluginManifestData(
    id = "com.example.plugin",
    version = "1.0.0",
    entrypoint = "com.example.MyPlugin",
    name = "My Plugin",
    capabilities = listOf("accessibility.voice", "ai.nlu"),
    permissions = listOf("MICROPHONE" to "Voice input")
)
val avuString = AVUEncoder.encodePluginManifest(manifest)

// Decoding
val parsed = AVUDecoder.parsePluginManifest(avuString)
println(parsed?.id)           // com.example.plugin
println(parsed?.capabilities) // [accessibility.voice, ai.nlu]
```

### File Locations

| Type | Extension | Location |
|------|-----------|----------|
| Learned App | `.vos` | `/data/data/com.augmentalis.voiceos/files/learned_apps/` |
| Plugin Manifest | `.avu` | `plugins/<plugin-id>/plugin.avu` |
| Config | `.avu` | `plugins/<plugin-id>/config/*.avu` |
| Handover | `.hov` | `.claude/handover/` or `Docs/handover/` |

---

## Migration Guide

### Changes in v2.2

1. **Terminology**: IPC renamed to RPC (Remote Procedure Call)
2. **Self-Documenting Headers**: Optional `codes:` section in header
3. **Unified Escape**: All modules use `AvuEscape` from `avucodec.core`
4. **Code Registry**: `AvuCodeRegistry` for runtime code registration
5. **Header Utilities**: `AvuHeader` for generating/parsing headers

### Changes in v2.1

1. **WebSocket/Sync Codes**: SCR, SUP, SDL, SFL, SBT, SRS, SCF, SST, RCN
2. **Entity Types**: TAB, FAV, HST, DWN, SET, SES
3. **Sync States**: IDLE, SYNCING, CONNECTING, etc.

### Changes in v2.0

1. **Plugin Manifest Codes**: PLG, DSC, AUT, PCP, MOD, DEP, PRM, PLT, AST, CFG, KEY, HKS
2. **Pipe Separator for Lists**: Capabilities and modules use `|` instead of `,`
3. **Schema Version**: Updated to `avu-2.0`

### Backward Compatibility

- v1.0 and v2.0 files remain parseable
- Unknown codes are ignored (forward compatible)
- `codes:` section is optional (backward compatible)
- Parsers should check schema version

---

## Related Documents

- [Universal Plugin Architecture Plan](../../../AI/Plans/UniversalPlugin-Architecture-Plan-260122.md)
- [AVUCodec Module](../../../../Modules/AVUCodec/src/commonMain/kotlin/com/augmentalis/avucodec/)
- [AVUCodec Core Utilities](../../../../Modules/AVUCodec/src/commonMain/kotlin/com/augmentalis/avucodec/core/)
- [VoiceOSCore AVUSerializer](../../../../Modules/VoiceOSCore/src/commonMain/kotlin/com/augmentalis/voiceoscore/AVUSerializer.kt)
- [WebSocket Module - AvuSyncMessage](../../../../Modules/WebSocket/src/commonMain/kotlin/com/augmentalis/websocket/AvuSyncMessage.kt)

---

## Version History

- **2.2.0** (2026-02-03): Self-documenting headers with codes legend, unified AvuEscape, AvuCodeRegistry, IPC renamed to RPC, Handover (.hov) file type with 16 codes (ARC, DEC, STA, WIP, BLK, TSK, FIL, MOD, DEP, LEA, BUG, REF, CFG, API, CTX, PRI)
- **2.1.0** (2026-02-03): Added WebSocket/Sync codes (PNG, PON, HND, CAP, SCR, SUP, SDL, SFL, SBT, SRS, SCF, SST, CON, DIS, RCN), entity types, sync states, conflict resolutions
- **2.0.0** (2026-01-22): Added Plugin Manifest format, new codes (PLG, DSC, AUT, PCP, MOD, DEP, PRM, PLT, AST, CFG, KEY, HKS)
- **1.0.0** (2025-12-03): Initial AVU format specification

---

**Author**: Augmentalis Engineering
**License**: Proprietary
