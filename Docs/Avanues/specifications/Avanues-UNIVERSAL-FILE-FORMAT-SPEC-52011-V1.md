# Universal File Format Specification

**Version:** 1.0.0
**Date:** 2025-11-20
**Status:** Active Standard
**Author:** Manoj Jhawar (manoj@ideahq.net)

---

## Overview

Unified file format for the entire Avanues ecosystem using **3-letter code structure** with project-specific extensions.

**Core Principle:** Same structure, different extensions for clear ownership.

### File Extensions by Project

| Extension | Project | Purpose | Location |
|-----------|---------|---------|----------|
| `.ava` | AVA | Voice intent examples | `/.ava/core/`, `/.ava/user/` |
| `.vos` | VoiceOS | System commands & plugins | `/.vos/system/`, `/.vos/plugins/` |
| `.avc` | AvaConnect | Device pairing & IPC definitions | `/.avc/devices/`, `/.avc/commands/` |
| `.avw` | WebAvanue (BrowserAvanue) | Browser commands & navigation | `/.avw/commands/`, `/.avw/bookmarks/` |
| `.avn` | NewAvanue | Platform configuration | `/.avn/config/`, `/.avn/modules/` |
| `.avs` | Avanues (Master) | UI DSL components | `/.avs/components/`, `/.avs/themes/` |

**Benefits:**
- ✅ **Clear ownership** - Extension shows which app owns the file
- ✅ **Same structure** - All use 3-letter codes + metadata
- ✅ **Universal IPC** - All can send/receive via Universal IPC
- ✅ **Cross-compatible** - AVA can read .vos, VoiceOS can read .ava, etc.
- ✅ **Tooling-friendly** - File managers show file type clearly

---

## Table of Contents

1. [Common File Structure](#common-file-structure)
2. [3-Letter Code System](#3-letter-code-system)
3. [Project-Specific Formats](#project-specific-formats)
4. [Cross-Project Integration](#cross-project-integration)
5. [IPC Integration](#ipc-integration)
6. [Migration Guides](#migration-guides)
7. [Parser Implementation](#parser-implementation)

---

## Common File Structure

All file types (`.ava`, `.vos`, `.avc`, `.avw`, `.avn`, `.avs`) share this structure:

### Schema Format

```
HEADER
---
METADATA BLOCK
---
ENTRIES BLOCK (3-letter codes)
---
SYNONYMS BLOCK (optional)
```

### Example Structure

```
# Avanues Universal Format v1.0
# Type: AVA
# Extension: .ava
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: ava
metadata:
  file: navigation.ava
  category: voice_command
  name: Navigation Commands
  description: App navigation voice commands
  count: 5
---
# Format: CODE:id:param1:param2:...
VCM:open_gmail:open gmail
VCM:open_settings:open settings
AIQ:weather:what's the weather
STT:dictation:hello world
CTX:location:share my location
---
synonyms:
  open: [launch, start, go to]
  settings: [preferences, options, configuration]
```

### Header Section

**Line 1-3: File type identification**
```
# Avanues Universal Format v1.0
# Type: [AVA|VOS|AVC|AVW|AVN|AVS]
# Extension: .[ava|vos|avc|avw|avn|avs]
```

### Metadata Section

**YAML-style metadata (after first `---`)**
```yaml
schema: avu-1.0          # Avanues Universal format version
version: 1.0.0           # File format version
locale: en-US            # Language/region
project: ava             # Owning project
metadata:
  file: navigation.ava   # Filename
  category: voice_command # Category
  name: Navigation       # Human name
  description: ...       # Description
  count: 5               # Entry count
```

### Entries Section

**3-letter code entries (after second `---`)**
```
CODE:id:params...
```

Format: `CODE:identifier:data...`

**Examples:**
```
VCM:open_gmail:open gmail
AIQ:weather_query:what's the weather
FTR:photo_transfer:photo.jpg:2500000:1
URL:google:https://google.com
JSN:call_ui:Col{Text{text:"Incoming call"}}
```

### Synonyms Section

**Optional global synonyms (after third `---`)**
```yaml
synonyms:
  open: [launch, start]
  close: [exit, quit]
```

---

## 3-Letter Code System

All entries use Universal IPC 3-letter codes. See [UNIVERSAL-IPC-SPEC.md](UNIVERSAL-IPC-SPEC.md) for complete list.

### Core Categories

| Category | Codes | Usage |
|----------|-------|-------|
| **Voice** | VCM, STT, TTS, WWD | Voice commands, speech |
| **AI** | AIQ, AIR, CTX, SUG | AI queries, context |
| **Communication** | VCA, ACC, DEC, CHT | Calls, messages |
| **Media** | FTR, SSO, SSI, RCO | File transfer, screen share |
| **Browser** | URL, NAV, TAB, PLD | Web navigation |
| **System** | HND, PNG, CAP, PRO | System messages |
| **UI** | JSN | UI components |

### Common Codes Used Across All Projects

| Code | Full Name | AVA | VOS | AVC | AVW | AVS |
|------|-----------|-----|-----|-----|-----|-----|
| `VCM` | Voice Command | ✅ | ✅ | ✅ | ✅ | ✅ |
| `AIQ` | AI Query | ✅ | ✅ | ❌ | ✅ | ✅ |
| `URL` | URL Share | ✅ | ✅ | ✅ | ✅ | ❌ |
| `JSN` | UI Component | ✅ | ✅ | ✅ | ✅ | ✅ |
| `FTR` | File Transfer | ❌ | ✅ | ✅ | ✅ | ❌ |
| `VCA` | Video Call | ❌ | ✅ | ✅ | ❌ | ❌ |
| `NAV` | Navigate | ✅ | ✅ | ❌ | ✅ | ✅ |

---

## Project-Specific Formats

### 1. AVA Files (.ava)

**Purpose:** Voice intent examples for AVA assistant
**Location:** `/.ava/core/`, `/.ava/voiceos/`, `/.ava/user/`

**Example: `navigation.ava`**
```
# Avanues Universal Format v1.0
# Type: AVA
# Extension: .ava
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: ava
metadata:
  file: navigation.ava
  category: voice_command
  name: Navigation Commands
  description: Voice commands for app navigation
  count: 10
  priority: 1
  tags: [navigation, apps, voice]
---
VCM:open_gmail:open gmail
VCM:open_settings:open settings
VCM:open_calendar:open calendar
VCM:launch_maps:launch maps
AIQ:find_app:where is the calculator
AIQ:app_location:how do I open photos
URL:open_website:open google.com
URL:search_web:search for cats
CTX:share_location:share my location
SUG:voice_hint:try saying "open gmail"
---
synonyms:
  open: [launch, start, run, go to]
  settings: [preferences, options, config]
  calendar: [schedule, agenda, events]
```

**AVA-Specific Features:**
- Priority levels (1-10)
- Learning tags for user-taught intents
- AI query integration
- VoiceOS command delegation

---

### 2. VoiceOS Files (.vos)

**Purpose:** System-wide accessibility commands
**Location:** `/.vos/system/`, `/.vos/plugins/`, `/.vos/user/`

**Example: `accessibility.vos`**
```
# Avanues Universal Format v1.0
# Type: VOS
# Extension: .vos
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: voiceos
metadata:
  file: accessibility.vos
  category: accessibility
  name: Accessibility Commands
  description: System accessibility control commands
  count: 8
  requires_permission: [ACCESSIBILITY_SERVICE]
  min_api: 24
---
VCM:tap_element:tap on button login
VCM:scroll_down:scroll down
VCM:scroll_up:scroll up
VCM:go_back:go back
VCM:go_home:go home
VCM:recent_apps:show recent apps
VCM:notifications:show notifications
VCM:quick_settings:open quick settings
---
synonyms:
  tap: [click, press, touch]
  scroll: [swipe, slide]
  back: [return, previous]
```

**VoiceOS-Specific Features:**
- Permission requirements
- API level compatibility
- Accessibility node targeting
- Plugin system integration

---

### 3. AvaConnect Files (.avc)

**Purpose:** Device-to-device communication definitions
**Location:** `/.avc/devices/`, `/.avc/commands/`, `/.avc/sessions/`

**Example: `video-call.avc`**
```
# Avanues Universal Format v1.0
# Type: AVC
# Extension: .avc
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: avaconnect
metadata:
  file: video-call.avc
  category: communication
  name: Video Call Commands
  description: Video calling feature commands
  count: 12
  feature: video-call
  webrtc_enabled: true
---
VCA:incoming_call:call from Pixel7:Manoj
ACC:accept_call:accept
DEC:decline_call:decline
DCR:decline_reason:decline:User busy
MIC:toggle_mic:microphone toggle:1
CAM:toggle_cam:camera toggle:0
REC:start_recording:start recording:video_call.mp4
REC:stop_recording:stop recording
DIS:end_call:call ended:User hung up
CHT:send_message:Hi there
SSO:share_screen:share my screen:1920:1080:30
RCO:remote_control:control remote device
---
synonyms:
  accept: [answer, pick up, take call]
  decline: [reject, ignore, dismiss]
  microphone: [mic, audio]
  camera: [cam, video]
```

**AvaConnect-Specific Features:**
- WebRTC integration
- Session management
- Device pairing codes
- Feature flags

---

### 4. WebAvanue Files (.avw)

**Purpose:** Browser voice commands and navigation
**Location:** `/.avw/commands/`, `/.avw/bookmarks/`, `/.avw/history/`

**Example: `browser-commands.avw`**
```
# Avanues Universal Format v1.0
# Type: AVW
# Extension: .avw
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: webavanue
metadata:
  file: browser-commands.avw
  category: browser_control
  name: Browser Commands
  description: Voice commands for browser control
  count: 15
  engine: chromium
  version: 120.0
---
URL:open_url:open google.com
URL:search:search for cats
NAV:go_back:go back
NAV:go_forward:go forward
NAV:refresh:refresh page
TAB:new_tab:new tab
TAB:close_tab:close tab
TAB:next_tab:next tab
TAB:prev_tab:previous tab
PLD:page_loaded:https://google.com:Google
VCM:scroll_down:scroll down
VCM:scroll_up:scroll up
VCM:zoom_in:zoom in
VCM:zoom_out:zoom out
VCM:bookmark:bookmark this page
---
synonyms:
  open: [go to, navigate to, visit]
  search: [find, look for, google]
  tab: [window]
  bookmark: [save, favorite, mark]
```

**WebAvanue-Specific Features:**
- Browser engine compatibility
- Tab management
- Bookmark integration
- Page navigation

---

### 5. NewAvanue Files (.avn)

**Purpose:** Platform configuration and module definitions
**Location:** `/.avn/config/`, `/.avn/modules/`, `/.avn/integrations/`

**Example: `platform-config.avn`**
```
# Avanues Universal Format v1.0
# Type: AVN
# Extension: .avn
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: newavanue
metadata:
  file: platform-config.avn
  category: platform
  name: Platform Configuration
  description: Core platform module configuration
  count: 8
  platform_version: 2.0.0
---
HND:module_init:ava:2.0:device1
HND:module_init:voiceos:4.0:device1
CAP:module_caps:ava:video,screen,file
CAP:module_caps:voiceos:accessibility,voice,system
PRO:server_promote:device1:12345:1732012345000
ROL:role_change:device1:server
PNG:heartbeat:1732012345000
PON:heartbeat_ack:1732012345000
---
synonyms:
  module: [component, plugin, service]
  init: [initialize, startup, boot]
```

**NewAvanue-Specific Features:**
- Module orchestration
- Platform versioning
- Cross-app coordination
- Server promotion logic

---

### 6. Avanues UI Files (.avs)

**Purpose:** UI component definitions using Avanues DSL
**Location:** `/.avs/components/`, `/.avs/themes/`, `/.avs/layouts/`

**Example: `call-prompt.avs`**
```
# Avanues Universal Format v1.0
# Type: AVS
# Extension: .avs
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: avanues
metadata:
  file: call-prompt.avs
  category: ui_component
  name: Call Prompt UI
  description: Incoming call prompt dialog
  count: 3
  component_type: dialog
---
JSN:call_prompt:Col#callPrompt{spacing:16;@pad:24;Text#title{text:"Incoming call from Manoj";size:20;weight:bold};Text#device{text:"Device: Pixel 7";size:14;color:gray};Row#actions{spacing:12;Btn#accept{label:"Accept";color:green;@click:accept_call};Btn#decline{label:"Decline";color:red;@click:decline_call}}}
JSN:call_active:Col#activeCall{Text{text:"Call in progress"};Text{text:"00:05:23"};Row{Btn#mute{icon:mic_off};Btn#speaker{icon:volume_up};Btn#end{icon:call_end;color:red}}}
JSN:call_ended:Col#endedCall{Text{text:"Call ended"};Text{text:"Duration: 5:23"};Btn#close{label:"Close"}}
---
synonyms:
  call: [voice call, video call, connection]
  prompt: [dialog, popup, notification]
```

**Avanues-Specific Features:**
- UI DSL integration
- Component hierarchy
- Theme support
- Event binding

---

## Cross-Project Integration

### Reading Files from Other Projects

All parsers can read any format (`.ava`, `.vos`, `.avc`, `.avw`, `.avn`, `.avs`):

**AVA reading VoiceOS commands:**
```kotlin
val vosFile = UniversalFileParser.parse("/.vos/system/accessibility.vos")
val commands = vosFile.entries.filter { it.code == "VCM" }
// AVA can now delegate to VoiceOS
```

**VoiceOS reading AVA intents:**
```kotlin
val avaFile = UniversalFileParser.parse("/.ava/core/navigation.ava")
val intents = avaFile.entries.map { convertToVoiceOSCommand(it) }
```

**AvaConnect reading browser commands:**
```kotlin
val avwFile = UniversalFileParser.parse("/.avw/commands/browser-commands.avw")
val urlCommands = avwFile.entries.filter { it.code == "URL" }
```

### File Location Convention

```
/storage/emulated/0/Android/data/com.augmentalis.*/files/
├── .ava/           # AVA files
│   ├── core/
│   ├── voiceos/
│   └── user/
├── .vos/           # VoiceOS files
│   ├── system/
│   ├── plugins/
│   └── user/
├── .avc/           # AvaConnect files
│   ├── devices/
│   ├── commands/
│   └── sessions/
├── .avw/           # WebAvanue files
│   ├── commands/
│   ├── bookmarks/
│   └── history/
├── .avn/           # NewAvanue files
│   ├── config/
│   ├── modules/
│   └── integrations/
└── .avs/           # Avanues UI files
    ├── components/
    ├── themes/
    └── layouts/
```

---

## IPC Integration

### From File to IPC Message

**Step 1: Load file entry**
```kotlin
val entry = "VCM:open_gmail:open gmail"
```

**Step 2: Parse to UniversalMessage**
```kotlin
val message = UniversalDSL.parse(entry)
// Returns: VoiceCommandMessage(commandId="open_gmail", command="open gmail")
```

**Step 3: Send via IPC**
```kotlin
ipcManager.send("com.augmentalis.voiceos", message)
// Serializes to: "VCM:cmd123:open gmail"
```

### From IPC Message to File Storage

**Step 1: Receive IPC message**
```kotlin
ipcManager.subscribe<VoiceCommandMessage>().collect { msg ->
    // Received: "VCM:cmd123:turn on lights"
}
```

**Step 2: Convert to file entry**
```kotlin
val entry = "${msg.code}:${msg.commandId}:${msg.command}"
// Result: "VCM:turn_on_lights:turn on lights"
```

**Step 3: Save to user file**
```kotlin
appendToFile("/.ava/user/en-US/learned.ava", entry)
```

---

## Migration Guides

### AVA: .ava v1.0 → .ava v2.0 (Universal Format)

**Before (v1.0 - JSON):**
```json
{
  "s": "ava-1.0",
  "v": "1.0.0",
  "l": "en-US",
  "i": [
    {
      "id": "open_gmail",
      "c": "open gmail",
      "s": ["launch gmail", "start gmail"],
      "cat": "navigation",
      "p": 1
    }
  ]
}
```

**After (v2.0 - Universal):**
```
# Avanues Universal Format v1.0
# Type: AVA
# Extension: .ava
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: ava
metadata:
  file: navigation.ava
  category: voice_command
  count: 1
---
VCM:open_gmail:open gmail
VCM:launch_gmail:launch gmail
VCM:start_gmail:start gmail
---
synonyms:
  open: [launch, start]
```

**Migration Script:**
```bash
python3 migrate_ava_v1_to_v2.py \
  --input /.ava/core/en-US/ \
  --output /.ava/core/en-US-v2/ \
  --backup
```

---

### VoiceOS: .vos (old) → .vos v2.0 (Universal Format)

**Before (old format):**
```json
{
  "schema": "vos-1.0",
  "commands": [
    {
      "action": "open_gmail",
      "cmd": "open gmail",
      "synonyms": ["launch gmail"]
    }
  ]
}
```

**After (v2.0 - Universal):**
```
# Avanues Universal Format v1.0
# Type: VOS
# Extension: .vos
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: voiceos
metadata:
  file: apps.vos
  category: navigation
  count: 1
---
VCM:open_gmail:open gmail
VCM:launch_gmail:launch gmail
```

---

### WebAvanue: IPCMessage → .avw files

**Before (Kotlin sealed class):**
```kotlin
sealed class IPCMessage {
    data class OpenUrl(val url: String) : IPCMessage()
    data class Search(val query: String) : IPCMessage()
}
```

**After (.avw file):**
```
# Avanues Universal Format v1.0
# Type: AVW
# Extension: .avw
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: webavanue
metadata:
  file: browser-ipc.avw
  category: browser_control
  count: 2
---
URL:open_url:https://google.com
NAV:search:cats
```

---

### AvaConnect: Protocol → .avc files

**Before (CompactProtocol.kt):**
```kotlin
sealed class CompactMessage {
    data class VideoCallRequest(...) : CompactMessage()
    data class AcceptResponse(...) : CompactMessage()
}
```

**After (.avc file):**
```
# Avanues Universal Format v1.0
# Type: AVC
# Extension: .avc
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: avaconnect
metadata:
  file: video-call.avc
  category: communication
  count: 2
---
VCA:call1:Pixel7:Manoj
ACC:call1
```

---

## Parser Implementation

### Universal File Parser (KMP)

**Location:** `/Volumes/M-Drive/Coding/Avanues/modules/AVAMagic/IPC/UniversalIPC/src/commonMain/kotlin/com/augmentalis/avamagic/ipc/universal/UniversalFileParser.kt`

```kotlin
object UniversalFileParser {

    fun parse(filePath: String): UniversalFile {
        val content = readFile(filePath)
        val sections = content.split("---")

        require(sections.size >= 3) { "Invalid file format" }

        val header = parseHeader(sections[0])
        val metadata = parseMetadata(sections[1])
        val entries = parseEntries(sections[2])
        val synonyms = if (sections.size > 3) parseSynonyms(sections[3]) else emptyMap()

        return UniversalFile(
            type = header.type,
            extension = header.extension,
            schema = metadata.schema,
            version = metadata.version,
            locale = metadata.locale,
            project = metadata.project,
            metadata = metadata.metadata,
            entries = entries,
            synonyms = synonyms
        )
    }

    private fun parseEntries(section: String): List<UniversalEntry> {
        return section.lines()
            .filter { it.isNotBlank() && !it.trim().startsWith("#") }
            .map { line ->
                val parts = line.split(":", limit = 3)
                require(parts.size >= 2) { "Invalid entry: $line" }

                UniversalEntry(
                    code = parts[0],
                    id = parts[1],
                    data = if (parts.size > 2) parts[2] else ""
                )
            }
    }
}

data class UniversalFile(
    val type: FileType,         // AVA, VOS, AVC, AVW, AVN, AVS
    val extension: String,       // .ava, .vos, .avc, .avw, .avn, .avs
    val schema: String,
    val version: String,
    val locale: String,
    val project: String,
    val metadata: Map<String, Any>,
    val entries: List<UniversalEntry>,
    val synonyms: Map<String, List<String>>
)

data class UniversalEntry(
    val code: String,           // VCM, AIQ, URL, etc.
    val id: String,             // Unique identifier
    val data: String            // Rest of the data
) {
    fun toIPCMessage(requestId: String = UUID.randomUUID().toString()): UniversalMessage {
        return UniversalDSL.parse("$code:$requestId:$data")
            .let { (it as ParseResult.Protocol).message }
    }
}

enum class FileType {
    AVA, VOS, AVC, AVW, AVN, AVS
}
```

### Extension-Specific Readers

Each project has a convenience reader:

```kotlin
// AVA
class AvaFileReader {
    fun load(path: String): UniversalFile {
        val file = UniversalFileParser.parse(path)
        require(file.extension == ".ava") { "Not an AVA file" }
        return file
    }
}

// VoiceOS
class VosFileReader {
    fun load(path: String): UniversalFile {
        val file = UniversalFileParser.parse(path)
        require(file.extension == ".vos") { "Not a VOS file" }
        return file
    }
}

// Similar for .avc, .avw, .avn, .avs
```

---

## Benefits Summary

### ✅ Same Structure
- All projects use 3-letter codes
- Consistent metadata format
- Universal parser works for all

### ✅ Clear Ownership
- `.ava` = AVA files
- `.vos` = VoiceOS files
- `.avc` = AvaConnect files
- `.avw` = WebAvanue files
- `.avn` = NewAvanue files
- `.avs` = Avanues UI files

### ✅ Cross-Compatible
- AVA can read .vos files
- VoiceOS can read .ava files
- All projects share Universal IPC

### ✅ Migration Path
- Gradual migration (coexistence)
- Auto-conversion tools
- Backward compatible parsers

---

## Migration Timeline

### Phase 1: Parser Implementation (Week 1)
- ✅ Create UniversalFileParser (KMP)
- ✅ Extension-specific readers
- ✅ Unit tests for all formats

### Phase 2: AVA Migration (Week 2)
- Convert .ava v1.0 → v2.0
- Test with existing intents
- Deploy to AVA app

### Phase 3: VoiceOS Migration (Week 3)
- Convert .vos → v2.0
- Plugin system integration
- Test accessibility commands

### Phase 4: AvaConnect Migration (Week 4)
- Convert protocol definitions → .avc
- WebRTC integration
- Device pairing flow

### Phase 5: WebAvanue Migration (Week 5)
- Convert IPCMessage → .avw
- Browser command integration
- Voice navigation testing

### Phase 6: Avanues UI Migration (Week 6)
- Convert UI DSL → .avs files
- Component library
- Theme system

---

## Tooling

### Conversion Scripts

```bash
# Convert AVA v1.0 → v2.0
python3 tools/convert_ava.py --input /.ava/ --output /.ava-v2/

# Convert VoiceOS → v2.0
python3 tools/convert_vos.py --input /.vos/ --output /.vos-v2/

# Convert WebAvanue IPC → .avw
python3 tools/convert_webavanue.py --input IPCBridge.kt --output /.avw/

# Convert AvaConnect → .avc
python3 tools/convert_avaconnect.py --input CompactProtocol.kt --output /.avc/
```

### Validation

```bash
# Validate all files
python3 tools/validate_universal.py --scan /storage/emulated/0/Android/data/

# Check cross-compatibility
python3 tools/check_compat.py --ava /.ava/ --vos /.vos/
```

---

## References

- [Universal IPC Specification](UNIVERSAL-IPC-SPEC.md)
- [Universal DSL Specification](UNIVERSAL-DSL-SPEC.md)
- [IPC Research Summary](IPC-RESEARCH-SUMMARY.md)
- [AVA File Format v1.0](../AVA/docs/Developer-Manual-Chapter37-AVA-File-Format.md)

---

## License

**Proprietary - Augmentalis ES**
All rights reserved.

---

## Author

**Manoj Jhawar**
Email: manoj@ideahq.net
IDEACODE Version: 8.4
