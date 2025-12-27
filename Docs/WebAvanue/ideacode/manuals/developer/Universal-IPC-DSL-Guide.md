# Universal IPC/DSL Developer Guide

**Version:** 1.0.0
**Audience:** Human Developers
**Last Updated:** 2025-11-20

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Understanding the Universal Format](#understanding-the-universal-format)
3. [IPC Implementation](#ipc-implementation)
4. [DSL Usage](#dsl-usage)
5. [Common Patterns](#common-patterns)
6. [Troubleshooting](#troubleshooting)
7. [Migration Guide](#migration-guide)

---

## Quick Start

### 30-Second Tutorial

**Create a voice command file:**

```kotlin
// 1. Create .ava file
val content = """
# Avanues Universal Format v1.0
# Type: AVA
# Extension: .ava
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: ava
metadata:
  file: my_commands.ava
  category: voice_command
  count: 2
---
VCM:open_settings:open settings
VCM:close_app:close app
---
""".trimIndent()

// 2. Parse it
val parsed = UniversalFileParser.parse(content)

// 3. Use it
parsed.entries.forEach { entry ->
    when (entry.type) {
        "VCM" -> registerVoiceCommand(entry.id, entry.value)
    }
}
```

**That's it!** You've just created and parsed your first universal format file.

---

## Understanding the Universal Format

### The Four Sections

Every universal format file has this structure:

```
┌─────────────────────────────────────┐
│ SECTION 1: HEADER                   │  ← File type and extension
│ # Avanues Universal Format v1.0     │
│ # Type: AVA                          │
│ # Extension: .ava                    │
├─────────────────────────────────────┤
│ SECTION 2: METADATA                  │  ← Schema, version, locale, etc.
│ schema: avu-1.0                      │
│ version: 1.0.0                       │
│ locale: en-US                        │
│ project: ava                         │
│ metadata:                            │
│   file: commands.ava                 │
│   category: voice_command            │
├─────────────────────────────────────┤
│ SECTION 3: ENTRIES (Required)        │  ← Your actual data
│ VCM:open_gmail:open gmail            │
│ AIQ:weather:what's the weather       │
│ IPC:DeviceMgr:com.augmentalis.IDevice│
├─────────────────────────────────────┤
│ SECTION 4: SYNONYMS (Optional)       │  ← Alternate phrasings
│ open_gmail:                          │
│   - launch gmail                     │
│   - start gmail app                  │
└─────────────────────────────────────┘
```

**Key Point:** Sections are separated by `---` on its own line.

### File Extensions Explained

| Extension | Project | What It's For | Example Use Case |
|-----------|---------|---------------|------------------|
| `.ava` | AVA | Voice commands that AVA understands | "open gmail", "what's the weather" |
| `.vos` | VoiceOS | System-level commands and plugin configs | "install plugin", "enable accessibility" |
| `.avc` | AvaConnect | Device pairing, remote control, IPC | "pair device ABC123", IPC definitions |
| `.awb` | WebAvanue/BrowserAvanue | Browser automation commands | "navigate to google.com", "click button" |
| `.ami` | MagicUI | UI component definitions (DSL) | Button layouts, theme configs |
| `.amc` | MagicCode | Code generation templates | API client generators, boilerplate |

**Rule of Thumb:** If you're in the AVA project, use `.ava`. VoiceOS? Use `.vos`. And so on.

---

## IPC Implementation

### Step-by-Step: Adding a New IPC Service

**Scenario:** You want to add a DeviceManager service to AvaConnect.

#### Step 1: Create the IPC Definition File

**File:** `AvaConnect/.avc/ipc/device_manager.avc`

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
  file: device_manager.avc
  category: ipc_definition
  protocol: AIDL
  count: 3
---
IPC:DeviceManager:com.augmentalis.avaconnect.IDeviceManager
IPC:PairingService:com.augmentalis.avaconnect.IPairingService
IPC:ConnectionMonitor:com.augmentalis.avaconnect.IConnectionMonitor
---
```

#### Step 2: Create the AIDL Interface

**File:** `IDeviceManager.aidl`

```java
package com.augmentalis.avaconnect;

interface IDeviceManager {
    boolean pairDevice(String deviceId);
    boolean unpairDevice(String deviceId);
    List<String> getConnectedDevices();
}
```

#### Step 3: Implement the Service

```kotlin
class DeviceManagerService : Service() {
    private val binder = object : IDeviceManager.Stub() {
        override fun pairDevice(deviceId: String): Boolean {
            // Implementation
            return true
        }

        override fun unpairDevice(deviceId: String): Boolean {
            // Implementation
            return true
        }

        override fun getConnectedDevices(): List<String> {
            // Implementation
            return emptyList()
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder
}
```

#### Step 4: Register the Service Dynamically

```kotlin
// Load IPC definitions from .avc file
val file = File(filesDir, ".avc/ipc/device_manager.avc")
val parsed = UniversalFileParser.parse(file.readText())

// Find the DeviceManager entry
val deviceMgrEntry = parsed.entries.find {
    it.type == "IPC" && it.id == "DeviceManager"
}

// Bind to service
val intent = Intent().apply {
    component = ComponentName(
        "com.augmentalis.avaconnect",
        deviceMgrEntry!!.value // "com.augmentalis.avaconnect.IDeviceManager"
    )
}
bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
```

#### Step 5: Document in IPC-METHODS.md

**File:** `AvaConnect/docs/ideacode/specs/IPC-METHODS.md`

```markdown
## Service: DeviceManager

**IPC Type:** AIDL
**Package:** com.augmentalis.avaconnect
**Interface:** `IDeviceManager`
**Definition File:** `.avc/ipc/device_manager.avc`

### Methods:
- `pairDevice(deviceId: String): Boolean` - Pairs a new device
- `unpairDevice(deviceId: String): Boolean` - Unpairs a device
- `getConnectedDevices(): List<String>` - Returns list of connected device IDs

**Permissions Required:**
- `com.augmentalis.permission.DEVICE_MANAGEMENT`

**Cross-References:**
- Intent: `com.augmentalis.ACTION_DEVICE_PAIRED` (see INTENT-REGISTRY.md)
- Contract: `DeviceContract.kt`
```

---

## DSL Usage

### Example 1: Voice Commands (AVA)

**Use Case:** Add new voice commands to AVA.

**File:** `AVA/.ava/core/en-US/navigation.ava`

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
VCM:open_maps:open google maps
VCM:open_settings:open settings
AIQ:weather:what's the weather
URL:google:https://google.com
---
open_gmail:
  - launch gmail
  - start gmail app
open_maps:
  - launch maps
  - navigate to maps
---
```

**Kotlin Handler:**

```kotlin
val file = File(filesDir, ".ava/core/en-US/navigation.ava")
val parsed = UniversalFileParser.parse(file.readText())

parsed.entries.forEach { entry ->
    when (entry.type) {
        "VCM" -> {
            // Register voice command
            voiceEngine.registerCommand(entry.value) {
                handleVoiceCommand(entry.id)
            }

            // Register synonyms if available
            parsed.synonyms[entry.id]?.forEach { synonym ->
                voiceEngine.registerCommand(synonym) {
                    handleVoiceCommand(entry.id)
                }
            }
        }
        "AIQ" -> {
            // Register AI query
            aiEngine.registerQuery(entry.value) {
                handleAIQuery(entry.id)
            }
        }
        "URL" -> {
            // Register URL command
            urlRegistry.register(entry.id, entry.value)
        }
    }
}

fun handleVoiceCommand(commandId: String) {
    when (commandId) {
        "open_gmail" -> launchApp("com.google.android.gm")
        "open_maps" -> launchApp("com.google.android.apps.maps")
        "open_settings" -> launchApp("com.android.settings")
    }
}
```

### Example 2: UI Components (MagicUI)

**Use Case:** Define a button component in the MagicUI DSL.

**File:** `MainAvanues/MagicUI/.ami/components/buttons.ami`

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
  file: buttons.ami
  category: ui_component
  component_type: button
  count: 3
---
BTN:primary:background=#007AFF;color=#FFFFFF;radius=8
BTN:secondary:background=#E5E5EA;color=#000000;radius=8
BTN:danger:background=#FF3B30;color=#FFFFFF;radius=8
---
```

**Usage in Code:**

```kotlin
val file = File(filesDir, ".ami/components/buttons.ami")
val parsed = UniversalFileParser.parse(file.readText())

val primaryButton = parsed.entries.find { it.id == "primary" }
val styles = parseStyles(primaryButton!!.value)

Button(
    modifier = Modifier
        .background(Color(styles["background"]!!))
        .clip(RoundedCornerShape(styles["radius"]!!.toInt().dp)),
    colors = ButtonDefaults.buttonColors(
        containerColor = Color(styles["background"]!!),
        contentColor = Color(styles["color"]!!)
    )
) {
    Text("Primary Button")
}
```

---

## Common Patterns

### Pattern 1: Dynamic Service Discovery

```kotlin
// Instead of hardcoding services...
❌ val service = bindService("com.augmentalis.DeviceManager")

// ...load from IPC definition files
✅
fun discoverServices(projectDir: File): Map<String, String> {
    val ipcFiles = projectDir.resolve(".avc/ipc").listFiles()
        ?.filter { it.extension == "avc" } ?: emptyList()

    val services = mutableMapOf<String, String>()

    ipcFiles.forEach { file ->
        val parsed = UniversalFileParser.parse(file.readText())
        parsed.entries.filter { it.type == "IPC" }.forEach { entry ->
            services[entry.id] = entry.value
        }
    }

    return services
}

// Use it
val services = discoverServices(filesDir)
val deviceMgr = bindService(services["DeviceManager"]!!)
```

### Pattern 2: Locale-Specific Command Loading

```kotlin
fun loadVoiceCommands(locale: String): List<VoiceCommand> {
    val commandsDir = File(filesDir, ".ava/core/$locale")
    val avaFiles = commandsDir.listFiles()?.filter { it.extension == "ava" } ?: emptyList()

    val commands = mutableListOf<VoiceCommand>()

    avaFiles.forEach { file ->
        val parsed = UniversalFileParser.parse(file.readText())

        // Only load if locale matches
        if (parsed.locale == locale) {
            parsed.entries.forEach { entry ->
                commands.add(VoiceCommand(
                    id = entry.id,
                    phrase = entry.value,
                    synonyms = parsed.synonyms[entry.id] ?: emptyList()
                ))
            }
        }
    }

    return commands
}

// Load English commands
val enCommands = loadVoiceCommands("en-US")
```

### Pattern 3: Version Migration

```kotlin
fun parseWithMigration(content: String): UniversalFile {
    val parsed = UniversalFileParser.parse(content)

    // Check schema version
    when (parsed.schema) {
        "avu-1.0" -> return parsed
        "avu-0.9" -> return migrateFrom0_9(parsed)
        else -> throw IllegalArgumentException("Unsupported schema: ${parsed.schema}")
    }
}

fun migrateFrom0_9(old: UniversalFile): UniversalFile {
    // Migration logic...
    return old.copy(schema = "avu-1.0")
}
```

---

## Troubleshooting

### Error: "Invalid file format: expected at least 3 sections"

**Cause:** Missing section separators (`---`)

**Fix:**
```
❌ WRONG:
# Header
schema: avu-1.0
VCM:test:test

✅ CORRECT:
# Header
---
schema: avu-1.0
---
VCM:test:test
---
```

### Error: "Missing '# Type:' in header"

**Cause:** Header section incomplete

**Fix:**
```
❌ WRONG:
# Avanues Universal Format v1.0
---

✅ CORRECT:
# Avanues Universal Format v1.0
# Type: AVA
# Extension: .ava
---
```

### Error: "Invalid entry format"

**Cause:** Entry doesn't follow `TYPE:ID:VALUE` format

**Fix:**
```
❌ WRONG:
VCM-open_gmail-open gmail

✅ CORRECT:
VCM:open_gmail:open gmail
```

### Common Mistakes

1. **Wrong extension for project**
   - ❌ Using `.ava` in VoiceOS project
   - ✅ Use `.vos` in VoiceOS, `.ava` in AVA

2. **Manual parsing**
   - ❌ `line.split(":")`
   - ✅ `UniversalFileParser.parse(content)`

3. **Missing metadata**
   - ❌ Only entries, no metadata section
   - ✅ Include schema, version, locale, project

---

## Migration Guide

### Migrating from Legacy Formats

**Step 1: Identify File Type**

```kotlin
// Old format (JSON)
{
  "commands": [
    {"phrase": "open gmail", "action": "launch_gmail"}
  ]
}

// Determine: This is voice commands → Use .ava extension
```

**Step 2: Convert to Universal Format**

```kotlin
// New format (.ava)
# Avanues Universal Format v1.0
# Type: AVA
# Extension: .ava
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: ava
metadata:
  file: commands.ava
  category: voice_command
  count: 1
---
VCM:launch_gmail:open gmail
---
```

**Step 3: Update Code**

```kotlin
// Old code
❌ val json = JSONObject(file.readText())
   val commands = json.getJSONArray("commands")

// New code
✅ val parsed = UniversalFileParser.parse(file.readText())
   val commands = parsed.entries
```

**Step 4: Validate**

```kotlin
try {
    val parsed = UniversalFileParser.parse(newContent)
    println("✓ Migration successful!")
} catch (e: Exception) {
    println("✗ Migration failed: ${e.message}")
}
```

---

## References

- **Format Specification:** `/Volumes/M-Drive/Coding/Avanues/docs/specifications/UNIVERSAL-FILE-FORMAT-FINAL.md`
- **Programming Standard (AI):** `/Volumes/M-Drive/Coding/ideacode/programming-standards/universal-ipc-dsl.md`
- **Protocol (Rules):** `/Volumes/M-Drive/Coding/ideacode/docs/ideacode/protocols/Protocol-Universal-IPC-DSL.md`
- **Migration Guide:** `/Volumes/M-Drive/Coding/Avanues/docs/specifications/MIGRATION-GUIDE-UNIVERSAL-FORMAT.md`
- **Parser Source:** `com.augmentalis.avamagic.ipc.universal.UniversalFileParser`

---

## Need Help?

- **Questions?** Check the spec: `UNIVERSAL-FILE-FORMAT-FINAL.md`
- **Bug in Parser?** File issue with test case
- **Unsure about extension?** See table in "File Extensions Explained" section
- **Migration issues?** Follow the 6-week plan in `MIGRATION-GUIDE-UNIVERSAL-FORMAT.md`

**Author:** Manoj Jhawar (manoj@ideahq.net)
**License:** Proprietary
