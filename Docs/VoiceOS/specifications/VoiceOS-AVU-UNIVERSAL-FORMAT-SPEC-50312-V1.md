# AVU - Universal Exchange Format

**Schema**: `avu-1.0`
**Version**: `1.0.0`
**Spec Location**: `docs/specifications/AVU-UNIVERSAL-FORMAT-SPEC.md`
**Last Updated**: 2025-12-03

---

## Overview

AVU (Avanues Universal Format) is a cross-project file format specification that enables all Avanues ecosystem projects to exchange data using a single parser. Different file extensions indicate project context, but the format structure remains identical.

---

## All Extensions (Same Format, Different Context)

| Extension | Project       | Purpose                        |
|-----------|---------------|--------------------------------|
| `.aai`    | AVA           | Voice intent examples          |
| `.vos`    | VoiceOS       | System commands & learned apps |
| `.avc`    | AvaConnect    | Device pairing & IPC           |
| `.awb`    | BrowserAvanue | Browser commands               |
| `.ami`    | MagicUI       | UI DSL components              |
| `.amc`    | MagicCode     | Code generation                |

---

## Universal Format Structure

```
# Avanues Universal Format v1.0
# Type: AVA | VOS | AVC | AWB | AMI | AMC
# Extension: .aai | .vos | .avc | .awb | .ami | .amc
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: ava | voiceos | avaconnect | browseravanue | magicui | magiccode
metadata:
  file: example.aai
  category: voice_command
  count: 5
---
IPC:field1:field2:field3
IPC:field1:field2:field3
---
synonyms:
  word: [syn1, syn2, syn3]
  another: [alt1, alt2]
```

---

## Format Sections

### 1. Header (Comments)
- Lines starting with `#` are informational comments
- Not parsed, but provide human-readable context
- Indicate format version, type, and extension

### 2. Schema Section
- Delimited by `---`
- Contains metadata in YAML-like format
- Required fields: `schema`, `version`, `locale`, `project`
- Metadata block with file info and item count

### 3. Data Section
- Delimited by `---`
- Contains IPC code entries (colon-delimited)
- Format: `CODE:field1:field2:field3:...`
- Each line is one data entry

### 4. Synonyms Section
- Delimited by `---`
- Contains word synonyms in YAML format
- Used for NLP and voice recognition
- Optional section

---

## IPC Codes by Project

### Shared Codes (All Projects)

| Code  | Meaning          | Format | Used In |
|-------|------------------|--------|---------|
| **VCM** | Voice Command    | `intent_id:example_text` | AVA, VOS, AWB |
| **AIQ** | AI Query         | `query_id:question_text` | AVA |
| **STT** | Speech to Text   | `audio_id:transcript` | AVA, VOS |
| **CTX** | Context Share    | `context_id:data` | AVA, VOS, AVC |

### VoiceOS LearnApp Codes

| Code  | Meaning          | Format | Purpose |
|-------|------------------|--------|---------|
| **APP** | App Metadata     | `package:name:learned_at` | App identification |
| **STA** | Statistics       | `screens:elements:commands:avg_depth:max_depth:coverage` | Learning stats |
| **SCR** | Screen Definition | `hash:activity:timestamp:element_count` | Screen identification |
| **ELM** | Element Definition | `uuid:label:type:actions:bounds:category` | UI element data |
| **NAV** | Navigation Path  | `from_hash:to_hash:trigger_uuid:trigger_label:timestamp` | Screen transitions |
| **DNC** | Do Not Click     | `element_id:label:type:reason` | Excluded elements (safety) |
| **DYN** | Dynamic Region   | `screen_hash:region_id:change_type` | Dynamic content markers |
| **MNU** | Menu Definition  | `menu_id:total_items:visible_items:menu_type` | Expandable menus |
| **CNT** | Contact          | `contact_id:name:email:phone:source_app` | Extracted contacts |
| **CMD** | Generated Command | `uuid:trigger:action:element_uuid:confidence` | Voice command |
| **SYN** | Synonym Set      | `word:syn1+syn2+syn3` | Command synonyms |

**Element Categories** (for ELM category field):
- `NAV` - Navigation (bottom nav, tabs, back buttons)
- `ACT` - Action (submit, save, confirm buttons)
- `INP` - Input (text fields, search boxes)
- `DSP` - Display (labels, images, status text)
- `CNT` - Contact (contact list items)
- `MNU` - Menu (overflow menus, dropdowns)
- `DNG` - Dangerous (excluded via Do Not Click)

**Dynamic Change Types** (for DYN change_type field):
- `INFINITE_SCROLL` - Feed-like content that keeps loading
- `CONTENT_REFRESH` - Same structure, different content
- `LAYOUT_CHANGE` - Structure changes (e.g., ads injected)

**Do Not Click Reasons** (for DNC reason field):
- `CALL_ACTION` - Could initiate/end calls
- `CONTENT_CREATION` - Could create posts/messages
- `EXIT_ACTION` - Could close app/session
- `AUTH_ACTION` - Could expose credentials
- `PAYMENT_ACTION` - Could trigger purchases

### BrowserAvanue Codes

| Code  | Meaning          | Format | Purpose |
|-------|------------------|--------|---------|
| **URL** | Web URL          | `url:title` | Web navigation |
| **NAV** | Navigation       | `action:target` | Browser navigation |
| **TAB** | Tab Control      | `action:tab_id` | Tab management |

### AvaConnect Codes

| Code  | Meaning          | Format | Purpose |
|-------|------------------|--------|---------|
| **VCA** | Video Call       | `call_id:participants` | Call initiation |
| **ACC** | Accept Call      | `call_id:action` | Call acceptance |

### MagicUI Codes

| Code  | Meaning          | Format | Purpose |
|-------|------------------|--------|---------|
| **JSN** | JSON UI          | `component_id:json_data` | UI component data |

### MagicCode Codes

| Code  | Meaning          | Format | Purpose |
|-------|------------------|--------|---------|
| **GEN** | Code Generator   | `template_id:params` | Code generation |

---

## Field Separators

| Separator | Usage | Example |
|-----------|-------|---------|
| `:` (colon) | Primary field separator | `APP:com.app:Name:123` |
| `+` (plus) | Multiple values in field | `click+longClick+edit` |
| `,` (comma) | Sub-field separator | `100,200,300,250` |
| `\|` (pipe) | Alternative separator | `option1\|option2` |

---

## Complete Example (VoiceOS LearnApp)

```
# Avanues Universal Format v1.0
# Type: VOS
# Extension: .vos
# Generated by: LearnApp v2.0
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: voiceos
metadata:
  file: com.microsoft.teams.vos
  category: learned_app
  count: 18
  exploration_mode: automated
  duration_s: 180
---
APP:com.microsoft.teams:Microsoft Teams:1733931600
STA:4:15:8:3.2:5:78.5
SCR:abc123:CallsActivity:1733931600:6
ELM:nav-001:Calls:BottomNavigationItemView:click:0,1800,270,1920:NAV
ELM:nav-002:Chat:BottomNavigationItemView:click:270,1800,540,1920:NAV
ELM:inp-003:Search:EditText:click+edit:100,100,980,180:INP
ELM:cnt-004:Mike Johnson:TextView:click:0,300,800,400:CNT
ELM:cnt-005:Sarah Smith:TextView:click:0,400,800,500:CNT
DNC:btn-end:End Call:Button:CALL_ACTION
DYN:abc123:feed_region:INFINITE_SCROLL
MNU:more_menu:12:4:OVERFLOW
SCR:def456:ChatActivity:1733931610:5
ELM:inp-006:Type a message:EditText:click+edit:0,1700,900,1800:INP
ELM:btn-007:Send:ImageButton:click:900,1700,1080,1800:ACT
DNC:btn-post:Post:Button:CONTENT_CREATION
SCR:ghi789:LoginActivity:1733931500:4
DNC:inp-email:Email:EditText:AUTH_ACTION
DNC:inp-pass:Password:EditText:AUTH_ACTION
DNC:btn-login:Sign In:Button:AUTH_ACTION
NAV:abc123:def456:nav-002:Chat:1733931605
NAV:def456:abc123:nav-001:Calls:1733931615
CMD:cmd-001:click calls:click:nav-001:0.95
CMD:cmd-002:search:click:inp-003:0.92
CMD:cmd-003:call mike:click:cnt-004:0.88
CNT:cnt-001:Mike Johnson:mike@company.com:+15551234567:com.microsoft.teams
CNT:cnt-002:Sarah Smith:sarah@company.com:+15559876543:com.microsoft.teams
---
synonyms:
  calls: [phone, dial, ring, contacts]
  chat: [message, text, dm, conversation]
  search: [find, look for, locate]
  mike: [michael, mike johnson]
```

---

## Cross-Project Compatibility

All Avanues projects can parse each other's files using the same universal parser:

```kotlin
// In AVA - read VoiceOS commands
val vosFile = AvaFileParser.parse(vosContent)  // ✅ Works

// In VoiceOS - read AVA intents
val aaiFile = VosFileParser.parse(aaiContent)  // ✅ Works

// In AvaConnect - read BrowserAvanue commands
val awbFile = AvcFileParser.parse(awbContent)  // ✅ Works
```

**Single format, multiple extensions = project context without conversion.**

---

## Parser Implementation Guidelines

### Required Features

1. **Header Parsing**: Ignore lines starting with `#`
2. **Section Detection**: Split by `---` delimiters
3. **Schema Validation**: Verify `avu-1.0` schema
4. **IPC Code Parsing**: Split by `:` delimiter
5. **Synonym Parsing**: Parse YAML-like syntax

### Optional Features

1. **Version Checking**: Warn on version mismatch
2. **Locale Support**: Handle different locales
3. **Code Validation**: Verify known IPC codes
4. **Field Validation**: Check field count per code

### Error Handling

1. **Malformed Lines**: Skip and log warning
2. **Unknown Codes**: Accept but flag as unknown
3. **Missing Sections**: Use defaults
4. **Invalid Fields**: Use empty/default values

---

## Versioning

### Schema Versions

- **avu-1.0**: Initial specification (current)
- **avu-1.1**: (Future) Extended IPC codes
- **avu-2.0**: (Future) Binary format support

### Backward Compatibility

- Parsers should accept older schema versions
- New codes are always backward compatible
- Section order is flexible
- Unknown codes should be ignored, not rejected

---

## Benefits

1. **Universal**: One parser for all Avanues projects
2. **Compact**: 60-80% smaller than JSON
3. **Human-Readable**: Easy to inspect and debug
4. **Extensible**: Add new codes without breaking parsers
5. **Structured**: Clear separation of data types
6. **Cross-Compatible**: No conversion needed between projects

---

## Implementation Status

| Project | Extension | Parser | Status |
|---------|-----------|--------|--------|
| AVA | `.aai` | `AvaFileParser` | ✅ Implemented |
| VoiceOS | `.vos` | `VosFileParser` | ✅ Implemented |
| VoiceOS LearnApp | `.vos` | `AIContextSerializer` | ✅ Implemented |
| AvaConnect | `.avc` | `AvcFileParser` | 🔄 Planned |
| BrowserAvanue | `.awb` | `AwbFileParser` | 🔄 Planned |
| MagicUI | `.ami` | `AmiFileParser` | 🔄 Planned |
| MagicCode | `.amc` | `AmcFileParser` | 🔄 Planned |

---

## Related Documentation

- **VoiceOS Commands**: `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/loader/VOSFileParser.kt`
- **LearnApp Format**: `docs/specifications/avu-learned-app-format-spec.md`
- **AVA Intents**: `../ava/docs/ideacode/specs/UNIVERSAL-FILE-FORMAT-FINAL.md`

---

## Contributing

To add new IPC codes:

1. Choose a 3-letter code (uppercase)
2. Define field structure clearly
3. Update this specification
4. Implement in relevant parsers
5. Add examples
6. Document in project-specific specs

---

**Version**: 1.0.0
**Last Updated**: 2025-12-03
**Maintained By**: Avanues Ecosystem Team
**License**: Proprietary - Avanues Universal Format
