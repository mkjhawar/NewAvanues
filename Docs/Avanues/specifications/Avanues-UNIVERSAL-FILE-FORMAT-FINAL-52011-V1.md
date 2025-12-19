# Universal File Format Specification - FINAL

**Version:** 1.0.0 FINAL
**Date:** 2025-11-20
**Status:** Active Standard
**Author:** Manoj Jhawar (manoj@ideahq.net)

---

## FINAL Extension Mapping

| Extension | Project/Module | Purpose | Meaning |
|-----------|----------------|---------|---------|
| `.ava` | AVA | Voice intent examples | **Ava** (AI Voice Assistant) |
| `.vos` | VoiceOS | System commands & plugins | **Voice OS** |
| `.avc` | AvaConnect | Device pairing & IPC | **Ava Connect** |
| `.awb` | MainAvanues/WebAvanue | Browser commands | **Ava Web Browser** |
| `.ami` | MainAvanues/MagicUI | UI DSL components | **Ava MagicUI** |
| `.amc` | MainAvanues/MagicCode | Code generation | **Ava MagicCode** |

**Repository Structure:**
- **AVA** (standalone) - `.ava` files
- **VoiceOS** (standalone) - `.vos` files
- **AvaConnect** (standalone) - `.avc` files
- **MainAvanues** (master repo) - `.awb`, `.ami`, `.amc` files
  - Contains: WebAvanue/BrowserAvanue, MagicUI, MagicCode

---

## File Locations

```
# AVA Project
/storage/.../com.augmentalis.ava/files/
├── .ava/
│   ├── core/en-US/*.ava
│   ├── voiceos/*.ava
│   └── user/*.ava

# VoiceOS Project
/storage/.../com.augmentalis.voiceos/files/
├── .vos/
│   ├── system/*.vos
│   ├── plugins/*.vos
│   └── user/*.vos

# AvaConnect Project
/storage/.../com.augmentalis.avaconnect/files/
├── .avc/
│   ├── devices/*.avc
│   ├── commands/*.avc
│   └── sessions/*.avc

# MainAvanues Project (Master Repo)
/storage/.../com.augmentalis.avanues/files/
├── .awb/           # WebAvanue/BrowserAvanue
│   ├── commands/*.awb
│   ├── bookmarks/*.awb
│   └── history/*.awb
├── .ami/           # MagicUI
│   ├── components/*.ami
│   ├── themes/*.ami
│   └── layouts/*.ami
└── .amc/           # MagicCode
    ├── generators/*.amc
    ├── templates/*.amc
    └── scripts/*.amc
```

---

## Extension Details

### 1. `.ava` - AVA (AI Voice Assistant)
**Project:** AVA (standalone)
**Location:** `/Volumes/M-Drive/Coding/AVA`

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
  count: 5
---
VCM:open_gmail:open gmail
AIQ:weather:what's the weather
STT:dictation:hello world
URL:google:https://google.com
CTX:location:share my location
---
```

### 2. `.vos` - VoiceOS (Voice Operating System)
**Project:** VoiceOS (standalone)
**Location:** `/Volumes/M-Drive/Coding/Avanues/android/apps/voiceos`

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
  count: 5
  requires_permission: [ACCESSIBILITY_SERVICE]
---
VCM:tap_button:tap on button login
VCM:scroll_down:scroll down
VCM:go_back:go back
VCM:go_home:go home
VCM:notifications:show notifications
---
```

### 3. `.avc` - AvaConnect (Device Communication)
**Project:** AvaConnect (standalone)
**Location:** `/Volumes/M-Drive/Coding/AvaConnect`

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
  feature: video-call
  count: 8
---
VCA:call1:Pixel7:Manoj
ACC:call1
DEC:call1
MIC:call1:1
CAM:call1:0
REC:call1:start:video.mp4
CHT:call1:Hello there
DIS:call1:User ended call
---
```

### 4. `.awb` - WebAvanue/BrowserAvanue (Ava Web Browser)
**Project:** MainAvanues (master repo)
**Location:** `/Volumes/M-Drive/Coding/Avanues/android/apps/browseravanue`

```
# Avanues Universal Format v1.0
# Type: AWB
# Extension: .awb
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: browseravanue
metadata:
  file: browser-commands.awb
  category: browser_control
  engine: chromium
  count: 10
---
URL:google:https://google.com
NAV:back:go back
NAV:forward:go forward
NAV:refresh:refresh page
TAB:new:new tab
TAB:close:close tab
PLD:loaded:https://google.com:Google
VCM:scroll_down:scroll down
VCM:bookmark:bookmark this page
CHT:send_url:Share this page
---
```

### 5. `.ami` - MagicUI (Ava MagicUI)
**Project:** MainAvanues (master repo)
**Location:** `/Volumes/M-Drive/Coding/Avanues/modules/AVAMagic/UI`

```
# Avanues Universal Format v1.0
# Type: AMI
# Extension: .ami
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: magicui
metadata:
  file: call-dialogs.ami
  category: ui_component
  component_type: dialog
  count: 3
---
JSN:incoming_call:Col#callPrompt{spacing:16;@pad:24;Text#title{text:"Incoming call from {caller}";size:20;weight:bold};Row#actions{spacing:12;Btn#accept{label:"Accept";color:green};Btn#decline{label:"Decline";color:red}}}
JSN:call_active:Col#activeCall{Text{text:"Call in progress"};Text#timer{text:"{duration}"};Row{Btn#mute{icon:mic_off};Btn#end{icon:call_end;color:red}}}
JSN:call_ended:Col#endedCall{Text{text:"Call ended"};Text{text:"Duration: {duration}"};Btn#close{label:"Close"}}
---
```

### 6. `.amc` - MagicCode (Ava MagicCode)
**Project:** MainAvanues (master repo)
**Location:** `/Volumes/M-Drive/Coding/Avanues/modules/AVAMagic/Code`

```
# Avanues Universal Format v1.0
# Type: AMC
# Extension: .amc
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: magiccode
metadata:
  file: kotlin-generators.amc
  category: code_generation
  language: kotlin
  count: 3
---
GEN:data_class:generate Kotlin data class from schema
GEN:viewmodel:generate MVVM ViewModel boilerplate
GEN:repository:generate Repository pattern implementation
---
```

---

## Updated FileType Enum

```kotlin
enum class FileType {
    AVA,    // AVA voice intents
    VOS,    // VoiceOS system commands
    AVC,    // AvaConnect device communication
    AWB,    // WebAvanue browser commands
    AMI,    // MagicUI components
    AMC;    // MagicCode generators

    fun toExtension(): String = ".${name.lowercase()}"

    fun toProjectName(): String = when(this) {
        AVA -> "ava"
        VOS -> "voiceos"
        AVC -> "avaconnect"
        AWB -> "browseravanue"
        AMI -> "magicui"
        AMC -> "magiccode"
    }
}
```

---

## Cross-Project Usage Matrix

| From → To | AVA | VOS | AVC | AWB | AMI | AMC |
|-----------|-----|-----|-----|-----|-----|-----|
| **AVA** | ✅ | ✅ Delegate | ❌ | ✅ URLs | ✅ UI | ❌ |
| **VOS** | ✅ Intents | ✅ | ✅ IPC | ✅ URLs | ✅ UI | ❌ |
| **AVC** | ✅ Share | ✅ Share | ✅ | ✅ URLs | ✅ UI | ❌ |
| **AWB** | ✅ Voice | ✅ Voice | ✅ Share | ✅ | ✅ UI | ❌ |
| **AMI** | ✅ Render | ✅ Render | ✅ Render | ✅ Render | ✅ | ✅ Gen |
| **AMC** | ❌ | ❌ | ❌ | ❌ | ✅ Gen | ✅ |

**Legend:**
- ✅ = Can read/use files
- ❌ = No direct usage
- Delegate = AVA delegates complex commands to VoiceOS
- Share = Share data via IPC
- Voice = Voice command integration
- Render = Render UI components
- Gen = Code generation

---

## Project Summaries

### Standalone Projects
1. **AVA** - AI voice assistant (voice intents, learning, AI)
2. **VoiceOS** - System-wide voice control (accessibility, plugins)
3. **AvaConnect** - Device-to-device communication (WebRTC, IPC)

### MainAvanues (Master Repo)
Contains:
1. **BrowserAvanue/WebAvanue** - Voice-controlled browser
2. **MagicUI** - Universal UI DSL system
3. **MagicCode** - Code generation framework
4. **Universal IPC** - IPC library (shared by all)

---

## Migration Timeline

### Week 1: AVA (.ava)
- Convert .ava v1.0 → Universal Format
- Test with existing intents
- Deploy to production

### Week 2: VoiceOS (.vos)
- Convert .vos → Universal Format
- Plugin system integration
- Accessibility testing

### Week 3: AvaConnect (.avc)
- Extract protocol definitions → .avc files
- WebRTC integration testing
- Device pairing validation

### Week 4: BrowserAvanue (.awb)
- Extract IPCMessage → .awb files
- Browser command integration
- Voice navigation testing

### Week 5: MagicUI (.ami)
- Extract UI components → .ami files
- Component library creation
- Theme system integration

### Week 6: MagicCode (.amc)
- Code generator definitions → .amc files
- Template system setup
- Generator testing

---

## Developer Manual Chapters Needed

### AVA Project
- Chapter XX: Universal File Format (.ava)
- Chapter XX: Cross-Project IPC Integration
- Chapter XX: Migration Guide (v1.0 → v2.0)

### VoiceOS Project
- Chapter XX: Universal File Format (.vos)
- Chapter XX: Plugin System with .vos Files
- Chapter XX: AVA Integration

### AvaConnect Project
- Chapter XX: Universal File Format (.avc)
- Chapter XX: Protocol Definition Files
- Chapter XX: Feature-Specific Commands

### MainAvanues (Master Repo)
- Chapter XX: BrowserAvanue Commands (.awb)
- Chapter XX: MagicUI Components (.ami)
- Chapter XX: MagicCode Generators (.amc)
- Chapter XX: Universal IPC Library
- Chapter XX: Cross-Project Architecture

---

## User Manual Sections Needed

### For All Users
- Getting Started with Universal Format
- Understanding File Extensions (.ava, .vos, .avc, .awb, .ami, .amc)
- Cross-App Communication

### AVA Users
- Teaching New Voice Commands
- Understanding .ava Files
- VoiceOS Integration

### VoiceOS Users
- Creating Custom Commands (.vos files)
- Installing Plugins
- Accessibility Features

### AvaConnect Users
- Device Pairing
- Feature Commands (.avc files)
- WebRTC Connection Setup

### BrowserAvanue Users
- Voice Commands for Browser (.awb files)
- Custom Bookmarks
- Browser Automation

---

## References

- [Universal IPC Specification](UNIVERSAL-IPC-SPEC.md)
- [Universal DSL Specification](UNIVERSAL-DSL-SPEC.md)
- [IPC Research Summary](IPC-RESEARCH-SUMMARY.md)
- [Migration Guide](MIGRATION-GUIDE-UNIVERSAL-FORMAT.md)

---

## License

**Proprietary - Augmentalis ES**
All rights reserved.

---

## Author

**Manoj Jhawar**
Email: manoj@ideahq.net
IDEACODE Version: 8.4
