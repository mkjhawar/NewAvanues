# AVU Format Specification for Learned Apps

**Format**: AVU (Avanues Universal Format)
**Schema**: `avu-1.0`
**Extension**: `.vos`
**Project**: VoiceOS
**Category**: `learned_app`
**Date**: 2025-12-03

---

## Overview

This document defines the AVU format for storing learned app navigation data in VoiceOS. The format follows the universal AVU specification while using VoiceOS-specific IPC codes for learned app context.

## Format Structure

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
  file: <package>.vos
  category: learned_app
  count: <total_items>
---
<IPC_CODE>:<field1>:<field2>:...
...
---
synonyms:
  word: [syn1, syn2, ...]
```

## IPC Codes for Learned Apps

| Code | Purpose | Fields | Example |
|------|---------|--------|---------|
| **APP** | App metadata | package:name:learned_at | `APP:com.instagram.android:Instagram:1733234567890` |
| **STA** | Statistics | screens:elements:paths:avg_elements:max_depth:coverage | `STA:5:23:8:4.6:3:75.5` |
| **SCR** | Screen definition | hash:activity:discovered_at:element_count | `SCR:abc123:MainActivity:1733234567890:5` |
| **ELM** | Element definition | uuid:label:type:actions:location | `ELM:btn-xyz:Settings:android.widget.Button:click:100,200,300,250` |
| **NAV** | Navigation path | from_hash:to_hash:trigger_uuid:trigger_label:timestamp | `NAV:abc123:def456:btn-xyz:Settings:1733234567890` |

## Field Definitions

### APP (App Metadata)
- **package**: Package name (e.g., `com.instagram.android`)
- **name**: Human-readable app name (e.g., `Instagram`)
- **learned_at**: Unix timestamp when app was learned

### STA (Statistics)
- **screens**: Total number of screens discovered
- **elements**: Total number of actionable elements
- **paths**: Total number of navigation paths
- **avg_elements**: Average elements per screen (float)
- **max_depth**: Maximum navigation depth from root
- **coverage**: Estimated coverage percentage (0-100)

### SCR (Screen Definition)
- **hash**: SHA-256 screen hash (structure-based)
- **activity**: Android activity name or "Unknown"
- **discovered_at**: Unix timestamp when screen was discovered
- **element_count**: Number of elements on this screen

### ELM (Element Definition)
- **uuid**: Stable element UUID from ThirdPartyUuidGenerator
- **label**: User-visible label (text, contentDescription, or viewIdResourceName)
- **type**: Android class name (e.g., `android.widget.Button`)
- **actions**: Available actions separated by `+` (e.g., `click+longClick`)
- **location**: Screen bounds as `left,top,right,bottom` or empty string

### NAV (Navigation Path)
- **from_hash**: Source screen hash
- **to_hash**: Destination screen hash
- **trigger_uuid**: UUID of element that triggers navigation
- **trigger_label**: Label of trigger element
- **timestamp**: Unix timestamp when transition was discovered

## Complete Example

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
  file: com.instagram.android.vos
  category: learned_app
  count: 32
---
APP:com.instagram.android:Instagram:1733234567890
STA:5:23:8:4.6:3:75.5
SCR:abc123:MainActivity:1733234567890:5
ELM:btn-settings:Settings:android.widget.Button:click:100,200,300,250
ELM:btn-profile:Profile:android.widget.ImageButton:click:350,200,450,250
ELM:edit-search:Search:android.widget.EditText:click+edit:500,200,900,250
ELM:btn-camera:Camera:android.widget.ImageButton:click:950,200,1050,250
ELM:btn-messages:Messages:android.widget.ImageButton:click:1100,200,1200,250
SCR:def456:SettingsActivity:1733234567901:8
ELM:btn-account:Account:android.widget.TextView:click:0,300,1080,400
ELM:btn-privacy:Privacy:android.widget.TextView:click:0,400,1080,500
ELM:btn-security:Security:android.widget.TextView:click:0,500,1080,600
ELM:btn-notifications:Notifications:android.widget.TextView:click:0,600,1080,700
ELM:btn-help:Help:android.widget.TextView:click:0,700,1080,800
ELM:btn-about:About:android.widget.TextView:click:0,800,1080,900
ELM:toggle-dark-mode:Dark Mode:android.widget.Switch:click:0,900,1080,1000
ELM:btn-back:Back:android.widget.ImageButton:click:50,50,150,150
SCR:ghi789:ProfileActivity:1733234567912:6
ELM:img-avatar:Avatar:android.widget.ImageView:click:400,100,680,380
ELM:txt-username:johndoe:android.widget.TextView::0,400,1080,450
ELM:txt-bio:Bio text:android.widget.TextView::0,450,1080,520
ELM:btn-edit:Edit Profile:android.widget.Button:click:100,550,500,650
ELM:btn-share:Share:android.widget.Button:click:580,550,980,650
ELM:btn-back:Back:android.widget.ImageButton:click:50,50,150,150
NAV:abc123:def456:btn-settings:Settings:1733234567895
NAV:def456:abc123:btn-back:Back:1733234567910
NAV:abc123:ghi789:btn-profile:Profile:1733234567900
NAV:ghi789:abc123:btn-back:Back:1733234567920
NAV:ghi789:def456:btn-edit:Edit Profile:1733234567925
---
synonyms:
  settings: [preferences, options, config]
  back: [return, previous, go back]
  next: [continue, forward, proceed]
  profile: [account, user, me]
  edit: [modify, change, update]
```

## File Organization

**Location**: `/data/data/com.augmentalis.voiceos/files/learned_apps/`
**Naming**: `<package_name>.vos` (sanitized, e.g., `com.instagram.android.vos`)

## Parsing Notes

1. **Header Lines**: Start with `#` and are comments (informational only)
2. **Section Delimiters**: `---` separates sections
3. **Colon Separator**: Fields within IPC codes are separated by `:`
4. **Plus Separator**: Multiple actions in ELM use `+` (e.g., `click+longClick`)
5. **Comma Separator**: Location coordinates use `,` (e.g., `100,200,300,250`)
6. **Empty Fields**: Empty strings allowed (e.g., location may be empty)
7. **Order**: SCR must come before its ELM entries

## Cross-Project Compatibility

As per AVU spec, all Avanues projects can parse this format using the same universal parser:

```kotlin
// In any Avanues project
val parser = AvaFileParser()  // or VosFileParser, AvcFileParser, etc.
val data = parser.parse(vosContent)  // ✅ Works across all projects
```

## Benefits

1. **Compact**: 60-80% smaller than JSON
2. **Human-Readable**: Easy to inspect and debug
3. **Cross-Compatible**: Readable by all Avanues projects
4. **Structured**: Clear IPC codes for each data type
5. **Extensible**: Easy to add new codes without breaking parsers
6. **Standard**: Follows universal AVU specification

## Version History

- **1.0.0** (2025-12-03): Initial AVU format specification for learned apps

---

**Related Documents**:
- AVU Universal Spec: `../ava/docs/ideacode/specs/UNIVERSAL-FILE-FORMAT-FINAL.md`
- AI Context Generator: `jit-screen-hash-uuid-deduplication-spec.md`
- VoiceOS Command Format: `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/loader/VOSFileParser.kt`
