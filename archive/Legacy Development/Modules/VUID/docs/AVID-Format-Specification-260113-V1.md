# AVID Format Specification

**Format Name:** Avanues Voice ID (AVID)
**Version:** 1.0
**Date:** 2026-01-13
**Status:** DRAFT
**Extension:** `.avid`

---

## 1. Overview

AVID (Avanues Voice ID) is a compact, human-readable format for storing and exchanging VoiceOS element libraries. It provides universal identifiers for UI elements that work across devices and platforms within the Avanues ecosystem.

### Design Principles

| Principle | Implementation |
|-----------|----------------|
| Compact | ~73% smaller than JSON |
| Platform-agnostic | Generic type codes, platform in header |
| Human-readable | Plain text, no binary encoding |
| Brandable | AVID prefix for universal IDs |
| Extensible | New type codes via AVTR registry |

### Related Formats

| Format | Extension | Purpose |
|--------|-----------|---------|
| AVID | `.avid` | Element library |
| AVTR | `.avtr` | Type registry |
| IDC | `.idc` | IDEACODE configuration |

---

## 2. AVID Identifier Format

### 2.1 Universal AVID

```
AVID-{platform}-{sequence}
```

| Component | Size | Description |
|-----------|------|-------------|
| `AVID-` | 5 chars | Fixed prefix (Avanues Voice ID) |
| `platform` | 1 char | Platform code (A/I/W/D) |
| `-` | 1 char | Separator |
| `sequence` | 6 chars | Zero-padded sequential number |

**Total Length:** 13 characters

**Examples:**
- `AVID-A-000001` - Android app #1
- `AVID-I-000042` - iOS app #42
- `AVID-W-000007` - Web app #7
- `AVID-M-000003` - macOS app #3
- `AVID-X-000015` - Windows app #15
- `AVID-L-000008` - Linux app #8

### 2.2 Platform Codes

Two formats are used for platform identification:

**AVID Prefix (1 char):** Used in the AVID identifier for compactness

| Code | Platform | Full AVID Example |
|------|----------|-------------------|
| A | Android | AVID-A-000001 |
| I | iOS | AVID-I-000001 |
| W | Web | AVID-W-000001 |
| M | macOS | AVID-M-000001 |
| X | Windows | AVID-X-000001 |
| L | Linux | AVID-L-000001 |

**Record Field (3 char):** Used in APP records for readability

| Code | Platform | AVID Prefix |
|------|----------|-------------|
| and | Android | A |
| ios | iOS | I |
| web | Web | W |
| mac | macOS | M |
| win | Windows | X |
| lnx | Linux | L |

**Note:** Windows uses `X` in AVID (not `W`) to avoid conflict with Web.

### 2.3 Local AVID (LAVID)

For unsynced apps before cloud assignment:

```
LAVID-{device_hash}-{local_id}
```

**Example:** `LAVID-m14a-0047`

---

## 3. File Structure

```
# Avanues Voice ID Library v1.0
# Type: AVID
# Extension: .avid
---
schema: avid-1.0
version: {file_version}
source: {device_id}
exported: {ISO8601_timestamp}
platform: {platform_code}
type_registry: {registry_version}
metadata:
  apps: {count}
  elements: {count}
  screens: {count}
  commands: {count}
---
# Records
TYPE:field1:field2:field3:...
---
aliases:
  type: [alias1, alias2]
```

---

## 4. Record Types

### 4.1 APP - Application

**Format:** `APP:avid:platform:osVersion:package:name:appVersion:fingerprint`

| Field | Required | Description |
|-------|----------|-------------|
| avid | Yes | AVID or LAVID identifier |
| platform | Yes | Platform code (and/ios/web/mac/win/lnx) |
| osVersion | Yes | OS version (e.g., 14, 11, 22.04) |
| package | Yes | Package/bundle identifier |
| name | Yes | Display name |
| appVersion | Yes | Application version string |
| fingerprint | Yes | 12-char deterministic hash |

**Platform & OS Version Examples:**

| Platform | Code | OS Version Examples |
|----------|------|---------------------|
| Android | and | 14, 13, 12 |
| iOS | ios | 17.2, 16.5 |
| Web | web | - (use browser or "web") |
| macOS | mac | 14.2, 13.6 |
| Windows | win | 11, 10 |
| Linux | lnx | 22.04, fedora39 |

**Example:**
```
APP:AVID-A-000001:and:14:com.instagram.android:Instagram:350.0.0:a3f2e1c9b7d4
APP:AVID-M-000015:mac:14.2:com.spotify.client:Spotify:1.2.3:b7d4e2f1a9c3
APP:AVID-X-000042:win:11:com.discord:Discord:1.0.9:c9d3f1e2b7a4
```

### 4.2 ELM - Element

**Format:** `ELM:avid:version:elemId:type:resourceId:name:contentDesc:bounds`

| Field | Required | Description |
|-------|----------|-------------|
| avid | Yes | Parent app AVID |
| version | Yes | App version |
| elemId | Yes | Element ID (integer) |
| type | Yes | Generic type code (BTN, INP, etc.) |
| resourceId | No | Platform resource ID |
| name | No | Display text |
| contentDesc | No | Accessibility description |
| bounds | No | Relative bounds (left,top,right,bottom) |

**Example:**
```
ELM:AVID-A-000001:350.0.0:1:BTN:like_button:Like:Like this post:0.10,0.80,0.15,0.85
```

### 4.3 SCR - Screen

**Format:** `SCR:avid:version:screenId:name:class:elementCount`

| Field | Required | Description |
|-------|----------|-------------|
| avid | Yes | Parent app AVID |
| version | Yes | App version |
| screenId | Yes | Screen ID (integer) |
| name | Yes | Screen display name |
| class | No | Activity/ViewController class |
| elementCount | No | Number of tracked elements |

**Example:**
```
SCR:AVID-A-000001:350.0.0:1:Feed:MainActivity:47
```

### 4.4 CMD - Voice Command

**Format:** `CMD:avid:version:elemId:phrase:action:priority`

| Field | Required | Description |
|-------|----------|-------------|
| avid | Yes | Parent app AVID |
| version | Yes | App version |
| elemId | Yes | Target element ID |
| phrase | Yes | Voice trigger phrase |
| action | Yes | Action type (click/focus/scroll/type) |
| priority | Yes | Priority 0-100 |

**Example:**
```
CMD:AVID-A-000001:350.0.0:1:like:click:100
```

### 4.5 SYN - Synonym

**Format:** `SYN:avid:version:elemId:synonym:canonical`

| Field | Required | Description |
|-------|----------|-------------|
| avid | Yes | Parent app AVID |
| version | Yes | App version |
| elemId | Yes | Target element ID |
| synonym | Yes | Alternative phrase |
| canonical | Yes | Primary phrase |

**Example:**
```
SYN:AVID-A-000001:350.0.0:1:love:like
```

### 4.6 MAP - Local Mapping

**Format:** `MAP:avid:version:localAppId:localElemId`

Used after import to map AVIDs to device-local integer IDs.

**Example:**
```
MAP:AVID-A-000001:350.0.0:1:1
```

---

## 5. Generic Type Codes

Type codes are platform-agnostic. See AVTR specification for platform mappings.

| Code | Name | Description |
|------|------|-------------|
| BTN | Button | Clickable actions |
| INP | Input | Text entry fields |
| TXT | Text | Static text/labels |
| IMG | Image | Pictures, icons |
| LST | List | Scrollable lists |
| ITM | Item | List items |
| SCR | Screen | Full screens |
| NAV | Navigation | Navigation elements |
| SWT | Switch | Toggle switches |
| SLR | Slider | Range selectors |
| SEL | Select | Dropdowns, pickers |
| DIA | Dialog | Modal dialogs |
| MNU | Menu | Context menus |
| CRD | Card | Card containers |
| TAB | Tab | Tab items |
| FAB | FAB | Floating action buttons |
| PRG | Progress | Progress indicators |
| WBV | WebView | Embedded web content |
| CHK | Checkbox | Checkboxes |
| RDO | Radio | Radio buttons |
| LNK | Link | Hyperlinks |
| HDR | Header | Section headers |
| FTR | Footer | Section footers |
| VID | Video | Video players |
| AUD | Audio | Audio players |
| MAP | Map | Map views |
| CAM | Camera | Camera views |
| CHT | Chart | Charts/graphs |

---

## 6. Action Types

| Action | Description |
|--------|-------------|
| click | Tap/click element |
| focus | Focus input field |
| scroll | Scroll direction |
| type | Enter text |
| long_press | Long press/hold |
| swipe | Swipe gesture |
| pinch | Pinch zoom |
| double_tap | Double tap |

---

## 7. Parsing Rules

### 7.1 Line Types

| Pattern | Type |
|---------|------|
| `#...` | Comment (skip) |
| `---` | Section delimiter |
| `KEY:value` | YAML header line |
| `TYPE:...` | Record line |
| (blank) | Skip |

### 7.2 Field Handling

- Colon (`:`) is field delimiter
- Empty fields: consecutive colons (`::`)
- No escaping mechanism - avoid colons in values
- UTF-8 encoding required
- LF line endings preferred

### 7.3 Validation

```kotlin
fun validateAvid(avid: String): Boolean {
    val pattern = Regex("^(AVID-[AIWD]-\\d{6}|LAVID-[a-z0-9]+-\\d+)$")
    return pattern.matches(avid)
}
```

---

## 8. Complete Example

```
# Avanues Voice ID Library v1.0
# Type: AVID
# Extension: .avid
#
# Exported from device: manoj-m14
# Contains: Social media apps collection
---
schema: avid-1.0
version: 1.0.0
source: manoj-m14
exported: 2026-01-13T10:30:00Z
platform: and
type_registry: avtr-1.0
metadata:
  apps: 3
  elements: 15
  screens: 6
  commands: 12
---
# Applications
APP:AVID-A-000001:and:com.instagram.android:Instagram:350.0.0:a3f2e1c9b7d4
APP:AVID-A-000002:and:com.whatsapp:WhatsApp:2.24.1:b7d4e2f1a9c3
APP:LAVID-m14a-0047:and:com.example.test:TestApp:1.0.0:c9d3f1e2b7a4

# Screens
SCR:AVID-A-000001:350.0.0:1:Feed:MainActivity:8
SCR:AVID-A-000001:350.0.0:2:Profile:ProfileActivity:5
SCR:AVID-A-000002:2.24.1:1:Chats:HomeActivity:7

# Elements - Instagram
ELM:AVID-A-000001:350.0.0:1:BTN:like_button:Like:Like this post:0.10,0.80,0.15,0.85
ELM:AVID-A-000001:350.0.0:2:BTN:comment_button:Comment:Add comment:0.20,0.80,0.25,0.85
ELM:AVID-A-000001:350.0.0:3:INP:search_edit_text:Search::0.05,0.05,0.95,0.10
ELM:AVID-A-000001:350.0.0:4:BTN:share_button:Share:Share post:0.30,0.80,0.35,0.85
ELM:AVID-A-000001:350.0.0:5:LST:feed_recycler:::0.00,0.15,1.00,0.75
ELM:AVID-A-000001:350.0.0:6:FAB:create_post:New Post::0.85,0.85,0.95,0.95
ELM:AVID-A-000001:350.0.0:7:NAV:bottom_nav:::0.00,0.90,1.00,1.00
ELM:AVID-A-000001:350.0.0:8:IMG:profile_image:::0.02,0.02,0.10,0.08

# Elements - WhatsApp
ELM:AVID-A-000002:2.24.1:1:FAB:fab:New Chat:Start new chat:0.85,0.85,0.95,0.95
ELM:AVID-A-000002:2.24.1:2:INP:search_src_text:Search::0.10,0.02,0.90,0.08
ELM:AVID-A-000002:2.24.1:3:LST:conversations_list:::0.00,0.10,1.00,0.90
ELM:AVID-A-000002:2.24.1:4:TAB:tab_chats:Chats::0.00,0.08,0.25,0.12
ELM:AVID-A-000002:2.24.1:5:TAB:tab_status:Status::0.25,0.08,0.50,0.12
ELM:AVID-A-000002:2.24.1:6:TAB:tab_calls:Calls::0.50,0.08,0.75,0.12
ELM:AVID-A-000002:2.24.1:7:MNU:overflow_menu:::0.90,0.02,0.98,0.08

# Voice Commands
CMD:AVID-A-000001:350.0.0:1:like:click:100
CMD:AVID-A-000001:350.0.0:1:heart:click:90
CMD:AVID-A-000001:350.0.0:2:comment:click:100
CMD:AVID-A-000001:350.0.0:3:search:focus:100
CMD:AVID-A-000001:350.0.0:4:share:click:100
CMD:AVID-A-000001:350.0.0:6:new post:click:100
CMD:AVID-A-000002:2.24.1:1:new chat:click:100
CMD:AVID-A-000002:2.24.1:1:compose:click:80
CMD:AVID-A-000002:2.24.1:2:search:focus:100
CMD:AVID-A-000002:2.24.1:4:chats:click:100
CMD:AVID-A-000002:2.24.1:5:status:click:100
CMD:AVID-A-000002:2.24.1:6:calls:click:100

# Synonyms
SYN:AVID-A-000001:350.0.0:1:love:like
SYN:AVID-A-000001:350.0.0:4:send:share
SYN:AVID-A-000002:2.24.1:1:start chat:new chat
SYN:AVID-A-000002:2.24.1:1:message:new chat
---
aliases:
  app: [application, package, apk]
  elm: [element, ui, widget, view, component]
  scr: [screen, activity, page, fragment, view]
  cmd: [command, phrase, voice, trigger, utterance]
  syn: [synonym, alias, alternative, aka]
```

---

## 9. Size Metrics

| Content | JSON (bytes) | AVID (bytes) | Savings |
|---------|--------------|--------------|---------|
| 1 app | ~150 | ~75 | 50% |
| 1 element | ~180 | ~70 | 61% |
| 1 command | ~80 | ~40 | 50% |
| Full library (50 apps, 2500 elements) | ~500KB | ~150KB | 70% |

---

## 10. Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-01-13 | Initial AVID specification |

---

## 11. Related Documents

- `AVTR-Format-Specification-260113-V1.md` - Type registry format
- `AVID-VID-Specification-260113-V1.md` - System architecture

---

**End of Specification**
