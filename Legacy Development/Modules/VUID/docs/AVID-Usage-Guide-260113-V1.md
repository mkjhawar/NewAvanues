# AVID System Usage Guide

**Version:** 1.0
**Date:** 2026-01-13
**Status:** DRAFT

---

## Table of Contents

1. [Overview](#1-overview)
2. [Quick Start](#2-quick-start)
3. [AVID Format Reference](#3-avid-format-reference)
4. [AVTR Type Registry](#4-avtr-type-registry)
5. [Implementation Guide](#5-implementation-guide)
6. [Database Integration](#6-database-integration)
7. [Export/Import Workflow](#7-exportimport-workflow)
8. [Voice Command Integration](#8-voice-command-integration)
9. [API Reference](#9-api-reference)
10. [Troubleshooting](#10-troubleshooting)

---

## 1. Overview

### What is AVID?

**AVID (Avanues Voice ID)** is a universal identifier system for tracking UI elements across the VoiceOS platform. It enables:

- **Cross-device consistency** - Same app/element has same ID everywhere
- **Voice command mapping** - Link spoken phrases to UI actions
- **Element library sharing** - Export/import scanned app data
- **Platform abstraction** - Generic type codes work across all platforms

### System Components

| Component | Extension | Purpose |
|-----------|-----------|---------|
| AVID File | `.avid` | Element library (apps, elements, commands) |
| AVTR File | `.avtr` | Type registry (platform class mappings) |
| LAVID | - | Local AVID (unsynced, device-specific) |

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    AVID SYSTEM ARCHITECTURE                  │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐ │
│  │   Scanner    │────>│   AVIDGen    │────>│   Database   │ │
│  │ (Accessib.)  │     │  (Modules/   │     │  (SQLDelight)│ │
│  │              │     │    VUID)     │     │              │ │
│  └──────────────┘     └──────────────┘     └──────────────┘ │
│         │                    │                    │          │
│         │                    │                    │          │
│         ▼                    ▼                    ▼          │
│  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐ │
│  │  Type Codes  │     │  Voice Cmds  │     │   Export     │ │
│  │   (AVTR)     │     │  (CMD/SYN)   │     │   (.avid)    │ │
│  └──────────────┘     └──────────────┘     └──────────────┘ │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Quick Start

### 2.1 Reading an AVID File

```kotlin
// Load and parse AVID file
val avidContent = File("social-apps.avid").readText()
val avidFile = AvidParser.parse(avidContent)

// Access apps
avidFile.apps.forEach { app ->
    println("${app.avid}: ${app.name} v${app.appVersion}")
}

// Access elements
avidFile.elements.forEach { elem ->
    println("  ${elem.elemId}: ${elem.type} - ${elem.name}")
}
```

### 2.2 Creating AVID Records

```kotlin
// During accessibility scanning
val avid = avidGenerator.registerApp(
    platform = "and",
    osVersion = "14",
    packageName = "com.instagram.android",
    appName = "Instagram",
    appVersion = "350.0.0"
)
// Returns: AVID-A-000001 (or LAVID-xxx if offline)

val elemId = avidGenerator.registerElement(
    avid = avid,
    version = "350.0.0",
    type = "BTN",
    resourceId = "like_button",
    name = "Like"
)
// Returns: 1
```

### 2.3 Type Resolution

```kotlin
// Load type registry
val avtrContent = File("voiceos-standard.avtr").readText()
val registry = AvtrParser.parse(avtrContent)
val resolver = TypeResolver(registry)

// Scanning: class name → type code
val className = "android.widget.Button"
val typeCode = resolver.classToType(className, "and")
// Returns: "BTN"

// Display: type code → readable name
val display = resolver.resolve("BTN", "and")
// Returns: TypeDisplay(code="BTN", name="Button", classes=[...])
```

---

## 3. AVID Format Reference

### 3.1 AVID Identifier Format

```
AVID-{platform}-{sequence}
```

| Platform | Code | Example |
|----------|------|---------|
| Android | A | AVID-A-000001 |
| iOS | I | AVID-I-000001 |
| Web | W | AVID-W-000001 |
| macOS | M | AVID-M-000001 |
| Windows | X | AVID-X-000001 |
| Linux | L | AVID-L-000001 |

### 3.2 Local AVID (LAVID)

For unsynced apps before cloud assignment:

```
LAVID-{device_hash}-{local_id}
```

Example: `LAVID-m14a-0047`

### 3.3 Record Types

| Type | Format | Description |
|------|--------|-------------|
| APP | `APP:avid:platform:osVersion:package:name:appVersion:fingerprint` | Application |
| ELM | `ELM:avid:version:elemId:type:resourceId:name:contentDesc:bounds` | UI Element |
| SCR | `SCR:avid:version:screenId:name:class:elementCount` | Screen |
| CMD | `CMD:avid:version:elemId:phrase:action:priority` | Voice Command |
| SYN | `SYN:avid:version:elemId:synonym:canonical` | Synonym |

### 3.4 File Structure

```
# Comment
---
schema: avid-1.0
version: 1.0.0
source: device-id
exported: 2026-01-13T10:00:00Z
platform: and
os_version: 14
type_registry: voiceos-standard-1.0
metadata:
  apps: 3
  elements: 50
---
APP:AVID-A-000001:and:14:com.example.app:Example:1.0.0:a3f2e1c9b7d4
ELM:AVID-A-000001:1.0.0:1:BTN:button_id:Click Me::
CMD:AVID-A-000001:1.0.0:1:click me:click:100
---
aliases:
  app: [application]
```

---

## 4. AVTR Type Registry

### 4.1 Purpose

The AVTR file maps generic 3-character type codes to platform-specific UI classes.

### 4.2 Type Code Reference

| Code | Name | Android | iOS | Web |
|------|------|---------|-----|-----|
| BTN | Button | Button, MaterialButton | UIButton | button |
| INP | Input | EditText, TextField | UITextField | input |
| TXT | Text | TextView | UILabel | p, span |
| IMG | Image | ImageView | UIImageView | img |
| LST | List | RecyclerView | UITableView | ul, ol |
| FAB | FAB | FloatingActionButton | - | button.fab |
| NAV | Navigation | NavigationView | UINavigationBar | nav |
| TAB | Tab | TabLayout | UITabBarItem | .tab |
| SWT | Switch | Switch | UISwitch | input.switch |
| DIA | Dialog | AlertDialog | UIAlertController | dialog |
| MNU | Menu | PopupMenu | UIMenu | menu |

### 4.3 Scanning Workflow

```kotlin
// 1. Detect UI class
val className = node.className.toString()
// "androidx.appcompat.widget.AppCompatButton"

// 2. Resolve to generic type
val typeCode = typeResolver.classToType(className, "and")
// "BTN"

// 3. Store with AVID
val element = AvidElement(
    avid = currentAppAvid,
    version = currentVersion,
    elemId = nextId++,
    type = typeCode,  // Generic code
    resourceId = node.viewIdResourceName,
    name = node.text?.toString()
)
```

### 4.4 Display Workflow

```kotlin
// 1. Read element from storage
val element = database.getElement(appId, elemId)
// type = "BTN"

// 2. Resolve for display
val display = typeResolver.resolve(element.type, "and")
// name = "Button", classes = ["Button", "ImageButton", ...]

// 3. Show to user
println("${display.name} (${display.classes.first()})")
// "Button (Button)"
```

---

## 5. Implementation Guide

### 5.1 Module Structure

```
Modules/VUID/
├── build.gradle.kts
├── src/
│   ├── commonMain/kotlin/com/augmentalis/vuid/
│   │   ├── AvidGenerator.kt          # AVID creation
│   │   ├── AvidParser.kt             # .avid file parsing
│   │   ├── AvtrParser.kt             # .avtr file parsing
│   │   ├── TypeResolver.kt           # Type code resolution
│   │   ├── Fingerprint.kt            # Hash generation
│   │   └── models/
│   │       ├── AvidFile.kt
│   │       ├── AvidApp.kt
│   │       ├── AvidElement.kt
│   │       └── TypeDefinition.kt
│   ├── androidMain/kotlin/
│   │   └── AndroidAvidGenerator.kt   # Android-specific
│   └── iosMain/kotlin/
│       └── IosAvidGenerator.kt       # iOS-specific
└── docs/
    ├── AVID-Format-Specification-260113-V1.md
    ├── AVTR-Format-Specification-260113-V1.md
    └── examples/
        ├── social-apps.avid
        └── voiceos-standard.avtr
```

### 5.2 Core Interfaces

```kotlin
interface AvidGenerator {
    /**
     * Register or retrieve app AVID
     */
    suspend fun registerApp(
        platform: String,
        osVersion: String,
        packageName: String,
        appName: String?,
        appVersion: String
    ): String  // Returns AVID or LAVID

    /**
     * Register element and return ID
     */
    suspend fun registerElement(
        avid: String,
        version: String,
        type: String,
        resourceId: String?,
        name: String?,
        contentDesc: String?,
        bounds: String?
    ): Int  // Returns element ID

    /**
     * Get human-readable AVID string
     */
    suspend fun getReadableAvid(avid: String, version: String, elemId: Int): String
}

interface TypeResolver {
    /**
     * Map class name to type code
     */
    fun classToType(className: String, platform: String): String?

    /**
     * Resolve type code to display info
     */
    fun resolve(typeCode: String, platform: String): TypeDisplay
}
```

### 5.3 Fingerprint Generation

```kotlin
object Fingerprint {
    /**
     * Generate deterministic app fingerprint
     * Same input always produces same output
     */
    fun forApp(platform: String, packageName: String): String {
        val input = "$platform:$packageName".lowercase()
        return hash(input, 12)
    }

    /**
     * Generate element hash for matching
     */
    fun forElement(
        resourceId: String?,
        className: String,
        contentDesc: String?
    ): String {
        val input = listOfNotNull(resourceId, className, contentDesc)
            .joinToString("|")
        return hash(input, 8)
    }

    private fun hash(input: String, length: Int): String {
        var hash1 = 0L
        var hash2 = 0L
        input.forEachIndexed { i, c ->
            hash1 = (hash1 * 31 + c.code.toLong()) and 0xFFFFFFFFL
            hash2 = (hash2 * 37 + c.code.toLong() * (i + 1)) and 0xFFFFFFFFL
        }
        val combined = ((hash1 shl 16) xor hash2) and 0xFFFFFFFFFFFFL
        return combined.toString(16).padStart(12, '0').takeLast(length)
    }
}
```

---

## 6. Database Integration

### 6.1 SQLDelight Schema

```sql
-- apps.sq
CREATE TABLE apps (
    local_id INTEGER PRIMARY KEY AUTOINCREMENT,
    avid TEXT,
    platform TEXT NOT NULL,
    os_version TEXT NOT NULL,
    package_name TEXT NOT NULL,
    app_name TEXT,
    app_version TEXT NOT NULL,
    fingerprint TEXT NOT NULL,
    last_scanned INTEGER,
    UNIQUE(platform, package_name, app_version)
);

getByAvid:
SELECT * FROM apps WHERE avid = ?;

getByPackage:
SELECT * FROM apps WHERE platform = ? AND package_name = ? AND app_version = ?;

insert:
INSERT INTO apps (avid, platform, os_version, package_name, app_name, app_version, fingerprint, last_scanned)
VALUES (?, ?, ?, ?, ?, ?, ?, ?);
```

```sql
-- elements.sq
CREATE TABLE elements (
    local_app_id INTEGER NOT NULL,
    elem_id INTEGER NOT NULL,
    elem_hash TEXT NOT NULL,
    type TEXT NOT NULL,
    resource_id TEXT,
    name TEXT,
    content_desc TEXT,
    bounds TEXT,
    PRIMARY KEY (local_app_id, elem_id),
    FOREIGN KEY (local_app_id) REFERENCES apps(local_id)
);

getByIds:
SELECT * FROM elements WHERE local_app_id = ? AND elem_id = ?;

getByHash:
SELECT * FROM elements WHERE local_app_id = ? AND elem_hash = ?;

insert:
INSERT INTO elements (local_app_id, elem_id, elem_hash, type, resource_id, name, content_desc, bounds)
VALUES (?, ?, ?, ?, ?, ?, ?, ?);
```

```sql
-- commands.sq
CREATE TABLE commands (
    local_app_id INTEGER NOT NULL,
    elem_id INTEGER NOT NULL,
    phrase TEXT NOT NULL,
    action TEXT NOT NULL,
    priority INTEGER NOT NULL DEFAULT 100,
    PRIMARY KEY (local_app_id, elem_id, phrase),
    FOREIGN KEY (local_app_id, elem_id) REFERENCES elements(local_app_id, elem_id)
);
```

### 6.2 Views for Human-Readable Display

```sql
-- views.sq
CREATE VIEW readable_elements AS
SELECT
    a.local_id AS app_id,
    e.elem_id,
    COALESCE(a.avid, 'LAVID-' || a.local_id) || ':' ||
        a.app_version || ':' || e.type || ':' ||
        COALESCE(e.resource_id, e.name, 'elem' || e.elem_id) AS readable_avid,
    a.app_name,
    a.package_name,
    a.app_version,
    e.type,
    e.resource_id,
    e.name,
    e.content_desc
FROM elements e
JOIN apps a ON e.local_app_id = a.local_id;
```

---

## 7. Export/Import Workflow

### 7.1 Export from Device

```kotlin
class AvidExporter(
    private val database: AvidDatabase,
    private val deviceId: String
) {
    fun export(): String {
        val apps = database.appsQueries.getAll().executeAsList()
        val elements = database.elementsQueries.getAll().executeAsList()
        val commands = database.commandsQueries.getAll().executeAsList()

        val builder = StringBuilder()

        // Header
        builder.appendLine("# Avanues Voice ID Library v1.0")
        builder.appendLine("---")
        builder.appendLine("schema: avid-1.0")
        builder.appendLine("version: 1.0.0")
        builder.appendLine("source: $deviceId")
        builder.appendLine("exported: ${Instant.now()}")
        builder.appendLine("metadata:")
        builder.appendLine("  apps: ${apps.size}")
        builder.appendLine("  elements: ${elements.size}")
        builder.appendLine("---")

        // Apps
        apps.forEach { app ->
            builder.appendLine(
                "APP:${app.avid ?: "LAVID-${app.local_id}"}:" +
                "${app.platform}:${app.os_version}:${app.package_name}:" +
                "${app.app_name}:${app.app_version}:${app.fingerprint}"
            )
        }

        // Elements
        elements.forEach { elem ->
            val app = apps.find { it.local_id == elem.local_app_id }
            builder.appendLine(
                "ELM:${app?.avid ?: "LAVID-${elem.local_app_id}"}:" +
                "${app?.app_version}:${elem.elem_id}:${elem.type}:" +
                "${elem.resource_id ?: ""}:${elem.name ?: ""}:" +
                "${elem.content_desc ?: ""}:${elem.bounds ?: ""}"
            )
        }

        builder.appendLine("---")
        return builder.toString()
    }
}
```

### 7.2 Import to Device

```kotlin
class AvidImporter(
    private val database: AvidDatabase
) {
    fun import(content: String) {
        val avidFile = AvidParser.parse(content)

        avidFile.apps.forEach { app ->
            // Check if app exists
            val existing = database.appsQueries
                .getByPackage(app.platform, app.packageName, app.appVersion)
                .executeAsOneOrNull()

            if (existing == null) {
                database.appsQueries.insert(
                    avid = app.avid,
                    platform = app.platform,
                    os_version = app.osVersion,
                    package_name = app.packageName,
                    app_name = app.name,
                    app_version = app.appVersion,
                    fingerprint = app.fingerprint,
                    last_scanned = null
                )
            } else if (existing.avid == null && app.avid != null) {
                // Update with AVID from cloud
                database.appsQueries.updateAvid(app.avid, existing.local_id)
            }
        }

        // Import elements...
    }
}
```

---

## 8. Voice Command Integration

### 8.1 Command Registration

```kotlin
class VoiceCommandRegistry(
    private val database: AvidDatabase
) {
    fun registerCommand(
        avid: String,
        version: String,
        elemId: Int,
        phrase: String,
        action: String,
        priority: Int = 100
    ) {
        val app = database.appsQueries.getByAvid(avid).executeAsOneOrNull()
            ?: return

        database.commandsQueries.insert(
            local_app_id = app.local_id,
            elem_id = elemId,
            phrase = phrase.lowercase(),
            action = action,
            priority = priority
        )
    }

    fun findCommand(phrase: String, currentAppId: Int?): CommandMatch? {
        val normalized = phrase.lowercase().trim()

        // First check current app
        if (currentAppId != null) {
            val match = database.commandsQueries
                .findByPhrase(currentAppId, normalized)
                .executeAsOneOrNull()
            if (match != null) return match.toCommandMatch()
        }

        // Then check synonyms
        val synonym = database.synonymsQueries
            .findSynonym(currentAppId ?: -1, normalized)
            .executeAsOneOrNull()
        if (synonym != null) {
            return findCommand(synonym.canonical, currentAppId)
        }

        return null
    }
}
```

### 8.2 Action Execution

```kotlin
sealed class VoiceAction {
    data class Click(val elemId: Int) : VoiceAction()
    data class Focus(val elemId: Int) : VoiceAction()
    data class Type(val elemId: Int, val text: String) : VoiceAction()
    data class Scroll(val direction: String) : VoiceAction()
}

fun CommandMatch.toAction(): VoiceAction {
    return when (action) {
        "click" -> VoiceAction.Click(elemId)
        "focus" -> VoiceAction.Focus(elemId)
        "scroll" -> VoiceAction.Scroll(/* parse from phrase */)
        else -> VoiceAction.Click(elemId)
    }
}
```

---

## 9. API Reference

### 9.1 AvidParser

```kotlin
object AvidParser {
    fun parse(content: String): AvidFile
    fun parseApp(line: String): AvidApp
    fun parseElement(line: String): AvidElement
    fun parseCommand(line: String): AvidCommand
}
```

### 9.2 AvtrParser

```kotlin
object AvtrParser {
    fun parse(content: String): AvtrFile
    fun parseType(lines: List<String>): TypeDefinition
}
```

### 9.3 TypeResolver

```kotlin
class TypeResolver(registry: AvtrFile) {
    fun classToType(className: String, platform: String): String?
    fun resolve(typeCode: String, platform: String): TypeDisplay
    fun getActions(platform: String): List<ActionDefinition>
}
```

### 9.4 AvidGenerator

```kotlin
interface AvidGenerator {
    suspend fun registerApp(...): String
    suspend fun registerElement(...): Int
    suspend fun getReadableAvid(...): String
    suspend fun export(): String
    suspend fun import(content: String)
}
```

---

## 10. Troubleshooting

### 10.1 Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| Unknown type code | Class not in AVTR registry | Add mapping or use "UNK" |
| Duplicate AVID | Same app registered twice | Use fingerprint to dedupe |
| Parse error | Malformed AVID file | Check for missing colons |
| LAVID not synced | No cloud connection | Will sync on next export |

### 10.2 Validation

```kotlin
fun validateAvidFile(content: String): ValidationResult {
    val errors = mutableListOf<String>()

    val lines = content.lines()
    var lineNum = 0

    for (line in lines) {
        lineNum++
        if (line.isBlank() || line.startsWith("#") || line.startsWith("---")) {
            continue
        }

        val parts = line.split(":")
        if (parts.isEmpty()) {
            errors.add("Line $lineNum: Empty record")
            continue
        }

        when (parts[0].uppercase()) {
            "APP" -> if (parts.size < 8) errors.add("Line $lineNum: APP needs 8 fields")
            "ELM" -> if (parts.size < 5) errors.add("Line $lineNum: ELM needs at least 5 fields")
            "CMD" -> if (parts.size < 7) errors.add("Line $lineNum: CMD needs 7 fields")
        }
    }

    return ValidationResult(errors.isEmpty(), errors)
}
```

---

## Related Documents

- `AVID-Format-Specification-260113-V1.md` - Full format spec
- `AVTR-Format-Specification-260113-V1.md` - Type registry spec
- `VUID-VID-Specification-260113-V1.md` - System architecture
- `examples/social-apps.avid` - Example library file
- `examples/voiceos-standard.avtr` - Standard type registry

---

**End of Guide**
