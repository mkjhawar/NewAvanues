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
| **STA** | Statistics       | `screens:elements:paths:avg:depth:coverage` | Learning stats |
| **SCR** | Screen Definition | `hash:activity:timestamp:element_count` | Screen identification |
| **ELM** | Element Definition | `uuid:label:type:actions:location` | UI element data |
| **NAV** | Navigation Path  | `from_hash:to_hash:trigger_uuid:trigger_label:timestamp` | Screen transitions |

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
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: voiceos
metadata:
  file: com.instagram.android.vos
  category: learned_app
  count: 12
---
APP:com.instagram.android:Instagram:1733234567890
STA:3:10:5:3.3:2:80.5
SCR:abc123:MainActivity:1733234567890:4
ELM:btn-001:Settings:android.widget.Button:click:100,200,300,250
ELM:btn-002:Profile:android.widget.ImageButton:click:350,200,450,250
ELM:edit-003:Search:android.widget.EditText:click+edit:500,200,900,250
ELM:btn-004:Camera:android.widget.ImageButton:click:950,200,1050,250
SCR:def456:SettingsActivity:1733234567901:3
ELM:btn-005:Account:android.widget.TextView:click:0,300,1080,400
ELM:btn-006:Privacy:android.widget.TextView:click:0,400,1080,500
ELM:btn-007:Back:android.widget.ImageButton:click:50,50,150,150
NAV:abc123:def456:btn-001:Settings:1733234567895
NAV:def456:abc123:btn-007:Back:1733234567910
---
synonyms:
  settings: [preferences, options, config]
  back: [return, previous, go back]
  profile: [account, user, me]
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
