# VID Format Specification

**Version:** 1.0
**Date:** 2026-01-13
**Status:** DRAFT
**Extension:** `.vid`

---

## 1. Overview

The VID format is a compact, human-readable text format for storing and exchanging VoiceOS element libraries. It follows the same design principles as the IDC (IDEACODE Configuration) format:

- Colon-delimited fields
- 3-character type codes
- YAML header for metadata
- Comment support with `#`
- Aliases section for type code expansion

### Design Goals

| Goal | Solution |
|------|----------|
| Compact | ~56% smaller than JSON equivalent |
| Human-readable | Plain text, no binary encoding |
| Parseable | Simple line-by-line parsing |
| Extensible | New type codes can be added |
| Portable | UTF-8 text, cross-platform |

---

## 2. File Structure

```
# Header comments
---
schema: vid-1.0
version: {file_version}
source: {device_id}
exported: {ISO8601_timestamp}
metadata:
  apps: {count}
  elements: {count}
  platform: {platform_code}
---
# Type records
TYPE:field1:field2:field3:...
TYPE:field1:field2:field3:...
---
aliases:
  type: [alias1, alias2]
```

### Sections

| Section | Required | Description |
|---------|----------|-------------|
| Header comments | No | Lines starting with `#` before first `---` |
| YAML header | Yes | Metadata block between first two `---` |
| Records | Yes | Type-coded data lines |
| Aliases | No | Type code aliases for tooling |

---

## 3. Type Codes

### 3.1 APP - Application Registration

**Format:** `APP:vid:platform:package:name:version:fingerprint`

| Field | Type | Description |
|-------|------|-------------|
| vid | string | VID (e.g., `VID-A-000001`) or `LOCAL-{n}` |
| platform | enum | `and`, `ios`, `web`, `dsk` |
| package | string | Package name (e.g., `com.instagram.android`) |
| name | string | Display name (e.g., `Instagram`) |
| version | string | Version string (e.g., `350.0.0`) |
| fingerprint | string | 12-char deterministic hash |

**Example:**
```
APP:VID-A-000001:and:com.instagram.android:Instagram:350.0.0:a3f2e1c9b7d4
```

### 3.2 ELM - UI Element

**Format:** `ELM:vid:version:elemId:type:resourceId:name:contentDesc:bounds`

| Field | Type | Description |
|-------|------|-------------|
| vid | string | Parent app VID |
| version | string | App version this element belongs to |
| elemId | int | Element ID (sequential per app) |
| type | enum | 3-char type code (btn, inp, txt, etc.) |
| resourceId | string | Android resource ID (optional) |
| name | string | Display text (optional) |
| contentDesc | string | Content description (optional) |
| bounds | string | Relative bounds `left,top,right,bottom` (optional) |

**Example:**
```
ELM:VID-A-000001:350.0.0:1:btn:like_button:Like:Like this post:0.10,0.80,0.15,0.85
```

### 3.3 SCR - Screen/Activity

**Format:** `SCR:vid:version:screenId:name:activityClass:elementCount`

| Field | Type | Description |
|-------|------|-------------|
| vid | string | Parent app VID |
| version | string | App version |
| screenId | int | Screen ID (sequential per app) |
| name | string | Screen display name |
| activityClass | string | Activity/Fragment class name |
| elementCount | int | Number of tracked elements on screen |

**Example:**
```
SCR:VID-A-000001:350.0.0:1:Feed:MainActivity:47
```

### 3.4 CMD - Voice Command

**Format:** `CMD:vid:version:elemId:phrase:action:priority`

| Field | Type | Description |
|-------|------|-------------|
| vid | string | Parent app VID |
| version | string | App version |
| elemId | int | Target element ID |
| phrase | string | Voice trigger phrase |
| action | enum | `click`, `focus`, `scroll`, `type`, etc. |
| priority | int | Command priority (0-100) |

**Example:**
```
CMD:VID-A-000001:350.0.0:1:like:click:100
```

### 3.5 SYN - Synonym Mapping

**Format:** `SYN:vid:version:elemId:synonym:canonical`

| Field | Type | Description |
|-------|------|-------------|
| vid | string | Parent app VID |
| version | string | App version |
| elemId | int | Target element ID |
| synonym | string | Alternative phrase |
| canonical | string | Primary phrase this maps to |

**Example:**
```
SYN:VID-A-000001:350.0.0:1:love:like
```

### 3.6 MAP - Local ID Mapping

**Format:** `MAP:vid:version:localAppId:localElemId`

Used to map VIDs to device-local integer IDs after import.

| Field | Type | Description |
|-------|------|-------------|
| vid | string | VID from library |
| version | string | App version |
| localAppId | int | Device-local app ID |
| localElemId | int | Device-local element ID |

**Example:**
```
MAP:VID-A-000001:350.0.0:1:1
MAP:VID-A-000001:350.0.0:1:2
```

---

## 4. Element Type Codes

| Code | Full Name | Description |
|------|-----------|-------------|
| btn | Button | Clickable buttons |
| inp | Input | Text input fields |
| txt | Text | Static text labels |
| img | Image | Images, icons |
| lst | List | Scrollable lists |
| itm | Item | List items |
| scr | Screen | Full screens |
| nav | Navigation | Navigation elements |
| swt | Switch | Toggle switches |
| slr | Slider | Range sliders |
| sel | Select | Dropdowns, pickers |
| dia | Dialog | Modal dialogs |
| mnu | Menu | Context menus |
| crd | Card | Card containers |
| tab | Tab | Tab items |
| fab | FAB | Floating action buttons |
| prg | Progress | Progress indicators |
| wbv | WebView | Web views |

---

## 5. Platform Codes

| Code | Platform |
|------|----------|
| and | Android |
| ios | iOS |
| web | Web |
| dsk | Desktop |

---

## 6. Parsing Rules

### 6.1 Line Processing

1. Skip blank lines
2. Skip comment lines (starting with `#`)
3. Detect section markers (`---`)
4. Parse YAML header between first two `---`
5. Parse type records until next `---` or EOF
6. Parse aliases section if present

### 6.2 Field Handling

- Empty fields are represented by consecutive colons: `field1::field3`
- Colons within field values are NOT supported (escape or omit)
- Whitespace is preserved within fields
- No quoting mechanism (fields are literal)

### 6.3 Character Encoding

- UTF-8 encoding required
- BOM (Byte Order Mark) optional but discouraged
- Line endings: LF (`\n`) preferred, CRLF (`\r\n`) accepted

---

## 7. Kotlin Parser

```kotlin
data class VidFile(
    val schema: String,
    val version: String,
    val source: String,
    val exported: String,
    val apps: List<VidApp>,
    val elements: List<VidElement>,
    val screens: List<VidScreen>,
    val commands: List<VidCommand>,
    val synonyms: List<VidSynonym>
)

data class VidApp(
    val vid: String,
    val platform: String,
    val packageName: String,
    val name: String,
    val version: String,
    val fingerprint: String
)

data class VidElement(
    val vid: String,
    val version: String,
    val elemId: Int,
    val type: String,
    val resourceId: String?,
    val name: String?,
    val contentDesc: String?,
    val bounds: String?
)

object VidParser {
    fun parse(content: String): VidFile {
        val lines = content.lines()
        var inYamlHeader = false
        var inRecords = false
        val yamlLines = mutableListOf<String>()
        val apps = mutableListOf<VidApp>()
        val elements = mutableListOf<VidElement>()
        val screens = mutableListOf<VidScreen>()
        val commands = mutableListOf<VidCommand>()
        val synonyms = mutableListOf<VidSynonym>()

        for (line in lines) {
            when {
                line.isBlank() || line.startsWith("#") -> continue
                line.trim() == "---" -> {
                    if (!inYamlHeader && !inRecords) {
                        inYamlHeader = true
                    } else if (inYamlHeader) {
                        inYamlHeader = false
                        inRecords = true
                    } else {
                        inRecords = false
                    }
                }
                inYamlHeader -> yamlLines.add(line)
                inRecords -> parseRecord(line, apps, elements, screens, commands, synonyms)
            }
        }

        val metadata = parseYamlHeader(yamlLines)
        return VidFile(
            schema = metadata["schema"] ?: "vid-1.0",
            version = metadata["version"] ?: "1.0.0",
            source = metadata["source"] ?: "",
            exported = metadata["exported"] ?: "",
            apps = apps,
            elements = elements,
            screens = screens,
            commands = commands,
            synonyms = synonyms
        )
    }

    private fun parseRecord(
        line: String,
        apps: MutableList<VidApp>,
        elements: MutableList<VidElement>,
        screens: MutableList<VidScreen>,
        commands: MutableList<VidCommand>,
        synonyms: MutableList<VidSynonym>
    ) {
        val parts = line.split(":")
        if (parts.isEmpty()) return

        when (parts[0].uppercase()) {
            "APP" -> apps.add(parseApp(parts))
            "ELM" -> elements.add(parseElement(parts))
            "SCR" -> screens.add(parseScreen(parts))
            "CMD" -> commands.add(parseCommand(parts))
            "SYN" -> synonyms.add(parseSynonym(parts))
        }
    }

    private fun parseApp(parts: List<String>): VidApp {
        return VidApp(
            vid = parts.getOrNull(1) ?: "",
            platform = parts.getOrNull(2) ?: "",
            packageName = parts.getOrNull(3) ?: "",
            name = parts.getOrNull(4) ?: "",
            version = parts.getOrNull(5) ?: "",
            fingerprint = parts.getOrNull(6) ?: ""
        )
    }

    private fun parseElement(parts: List<String>): VidElement {
        return VidElement(
            vid = parts.getOrNull(1) ?: "",
            version = parts.getOrNull(2) ?: "",
            elemId = parts.getOrNull(3)?.toIntOrNull() ?: 0,
            type = parts.getOrNull(4) ?: "",
            resourceId = parts.getOrNull(5)?.takeIf { it.isNotEmpty() },
            name = parts.getOrNull(6)?.takeIf { it.isNotEmpty() },
            contentDesc = parts.getOrNull(7)?.takeIf { it.isNotEmpty() },
            bounds = parts.getOrNull(8)?.takeIf { it.isNotEmpty() }
        )
    }
}
```

---

## 8. Size Comparison

### Test Dataset: Instagram App (47 elements)

| Format | Size | Compression |
|--------|------|-------------|
| JSON (pretty) | 12,847 bytes | baseline |
| JSON (minified) | 8,234 bytes | 36% smaller |
| VID | 3,456 bytes | **73% smaller** |

### Size Formula

Approximate bytes per record:
- APP: ~80 bytes
- ELM: ~70 bytes
- SCR: ~50 bytes
- CMD: ~40 bytes
- SYN: ~35 bytes

---

## 9. Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-01-13 | Initial specification |

---

## 10. Related Documents

- `VUID-VID-Specification-260113-V1.md` - Main VID system specification
- `VUID-Unified-System-Analysis-260113-V1.md` - Analysis document
- `sample-library.vid` - Example VID file

---

**End of Specification**
