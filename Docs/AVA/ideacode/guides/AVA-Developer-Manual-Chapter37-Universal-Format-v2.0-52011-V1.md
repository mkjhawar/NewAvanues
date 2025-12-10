# Chapter 37: Universal Format v2.0 - AVA Intent Files

**Last Updated:** 2025-11-20  
**Status:** Active  
**Format Version:** 2.0 (Universal Format ONLY)

---

## Overview

AVA uses the **Avanues Universal Format v2.0** for all intent example files. This format replaced the legacy v1.0 JSON format in November 2025, providing a human-readable, IPC-ready structure with 3-letter mnemonic codes.

### Key Benefits

1. **Human-Readable:** No JSON parsing needed to review intents
2. **IPC Integration:** Direct IPC message creation from file entries
3. **Cross-Project Sharing:** Same format across all Avanues projects
4. **Smaller Size:** 60-87% reduction vs JSON for IPC messages
5. **Zero-Conversion:** File format matches IPC wire format

---

## Format Structure

All `.ava` files follow this structure:

```
# Avanues Universal Format v1.0
# Type: AVA - Voice Intent Examples
# Extension: .ava
# Project: AVA (AI Voice Assistant)
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: ava
metadata:
  file: example.ava
  category: voice_command
  name: Example Commands
  description: Description here
  priority: 1
  count: 10
---
VCM:intent_id:canonical example
VCM:intent_id:synonym 1
VCM:intent_id:synonym 2
AIQ:query_id:question example
---
synonyms:
  word: [synonym1, synonym2]
```

### Sections

1. **Header (Optional):** Comment block with metadata
2. **Metadata:** YAML-like structure with file information
3. **Entries:** IPC code format `CODE:intent_id:example_text`
4. **Synonyms:** Global word expansions

---

## IPC Codes in AVA

| Code | Meaning | Category | Usage |
|------|---------|----------|-------|
| **VCM** | Voice Command | voice_command, navigation, device_control, media_control, system_control | Action-oriented commands |
| **AIQ** | AI Query | ai_query | Questions, information requests |
| **STT** | Speech to Text | speech_text | Transcription, dictation |
| **CTX** | Context Share | context_share | Location, app state, user context |
| **SUG** | Suggestion | suggestion | Recommendations, next actions |

### Code Selection Guide

- **Navigation/App Control:** Use `VCM`
- **Information Queries:** Use `AIQ`
- **Transcription:** Use `STT`
- **Context Sharing:** Use `CTX`
- **Suggestions:** Use `SUG`

**Default:** When unsure, use `VCM` as the converter will map categories correctly.

---

## AVA Intent Files

### Location
```
apps/ava-standalone/src/main/assets/ava-examples/
├── en-US/
│   ├── navigation.ava          (8 intents)
│   ├── media-control.ava       (10 intents)
│   ├── system-control.ava      (12 intents)
│   └── voiceos-commands.ava    (94 intents)
└── README.md
```

### File Examples

#### navigation.ava (8 intents, VCM codes)
```
VCM:open_app:open gmail
VCM:open_app:launch gmail
VCM:open_settings:open settings
VCM:go_home:go home
VCM:go_back:go back
...
```

#### media-control.ava (10 intents, VCM codes)
```
VCM:play_music:play music
VCM:pause_music:pause music
VCM:next_track:next track
VCM:volume_up:volume up
...
```

#### system-control.ava (12 intents, VCM codes)
```
VCM:wifi_on:turn on wifi
VCM:bluetooth_on:turn on bluetooth
VCM:brightness_up:brightness up
VCM:flashlight_on:flashlight on
...
```

#### voiceos-commands.ava (94 intents, VCM codes)
This file references VoiceOS system commands that AVA can delegate via IPC.

---

## Database Storage

Intent examples are stored in the `intent_examples` table (schema v4):

```kotlin
@Entity(tableName = "intent_examples")
data class IntentExampleEntity(
    val exampleHash: String,           // MD5(intentId + exampleText)
    val intentId: String,              // e.g., "open_gmail"
    val exampleText: String,           // e.g., "open gmail"
    val isPrimary: Boolean,            // true for canonical
    val source: String,                // "AVA_FILE_UNIVERSAL_V2"
    val formatVersion: String,         // "v2.0"
    val ipcCode: String?,              // "VCM", "AIQ", etc.
    val locale: String,                // "en-US"
    val createdAt: Long,
    val usageCount: Int,
    val lastUsed: Long?
)
```

### Database Migration

**Version 3 → 4 (MIGRATION_3_4):**
- Added `format_version` column (defaults to "v1.0" for legacy data)
- Added `ipc_code` column (VCM, AIQ, etc.)
- Existing data preserved, new data uses v2.0

---

## Parser Implementation

### AvaFileParser.kt

**Location:** `Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/ava/parser/AvaFileParser.kt`

```kotlin
object AvaFileParser {
    /**
     * Parse .ava file in Universal Format v2.0
     * v1.0 JSON format is NO LONGER SUPPORTED
     */
    fun parse(content: String): AvaFile {
        val trimmed = content.trim()
        require(trimmed.startsWith("#") || trimmed.startsWith("---")) {
            "Invalid .ava file: Must use Universal Format v2.0"
        }
        return parseUniversalFormat(trimmed)
    }
}
```

**Key Points:**
- Auto-detects v2.0 format by checking for `#` or `---` header
- Throws error if v1.0 JSON format detected
- Parses metadata, entries, and synonyms sections
- Groups entries by intent ID
- Extracts IPC codes from entry format

---

## Entity Conversion

### AvaToEntityConverter.kt

**Location:** `Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/ava/converter/AvaToEntityConverter.kt`

```kotlin
fun convertToEntity(
    intent: AvaIntent,
    exampleText: String,
    isPrimary: Boolean,
    timestamp: Long
): IntentExampleEntity {
    return IntentExampleEntity(
        exampleHash = generateHash(intent.id, exampleText),
        intentId = intent.id,
        exampleText = exampleText,
        isPrimary = isPrimary,
        source = "AVA_FILE_${intent.source}",
        formatVersion = "v2.0",          // All files now v2.0
        ipcCode = intent.getIPCCode(),   // VCM, AIQ, etc.
        locale = intent.locale,
        createdAt = timestamp,
        usageCount = 0,
        lastUsed = null
    )
}
```

---

## Creating New Intent Files

### Step-by-Step Guide

1. **Create file structure:**
```bash
touch apps/ava-standalone/src/main/assets/ava-examples/en-US/my-intents.ava
```

2. **Add header and metadata:**
```
# Avanues Universal Format v1.0
# Type: AVA - Voice Intent Examples
---
schema: avu-1.0
version: 1.0.0
locale: en-US
project: ava
metadata:
  file: my-intents.ava
  category: voice_command
  name: My Custom Commands
  description: Custom voice commands for my feature
  priority: 1
  count: 5
---
```

3. **Add intent entries:**
```
VCM:my_command:do something
VCM:my_command:perform action
VCM:my_command:execute task
```

4. **Add global synonyms (optional):**
```
---
synonyms:
  do: [perform, execute, run]
  something: [action, task, work]
```

5. **Rebuild and test:**
```bash
./gradlew :app:ava-standalone:assembleDebug
```

### Best Practices

- **Group synonyms:** Keep all synonyms for an intent together
- **Use VCM by default:** Unless the intent is clearly a query (AIQ)
- **Be specific:** Use clear, unambiguous example phrases
- **Test variations:** Include different ways users might phrase the command
- **Add context:** Use descriptive intent IDs (e.g., `open_gmail` not `cmd_001`)

---

## Cross-Project Compatibility

AVA can read intent files from other projects using `UniversalFileParser`:

| Extension | Project | AVA Can Read? | Purpose |
|-----------|---------|---------------|---------|
| `.ava` | AVA | ✅ Native | Voice intent examples |
| `.vos` | VoiceOS | ✅ Yes | System commands & plugins |
| `.avc` | AvaConnect | ✅ Yes | Device pairing & IPC definitions |
| `.awb` | BrowserAvanue | ✅ Yes | Browser commands |
| `.ami` | MagicUI | ✅ Yes | UI DSL components |
| `.amc` | MagicCode | ✅ Yes | Code generation templates |

### Using UniversalFileParser

```kotlin
import com.augmentalis.avamagic.ipc.UniversalFileParser
import com.augmentalis.avamagic.ipc.FileType

// Read VoiceOS commands from AVA
val vosContent = File("voiceos-commands.vos").readText()
val vosFile = UniversalFileParser.parse(vosContent, FileType.VOS)

// Read AvaConnect protocols from AVA
val avcContent = File("avaconnect-protocol.avc").readText()
val avcFile = UniversalFileParser.parse(avcContent, FileType.AVC)
```

---

## Migration from v1.0 (Deprecated)

**⚠️ v1.0 JSON format is NO LONGER SUPPORTED as of November 2025.**

If you have legacy v1.0 files, convert them using this template:

### v1.0 (JSON) - DEPRECATED
```json
{
  "s": "ava-1.0",
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

### v2.0 (Universal) - CURRENT
```
---
schema: avu-1.0
version: 1.0.0
locale: en-US
metadata:
  category: navigation
---
VCM:open_gmail:open gmail
VCM:open_gmail:launch gmail
VCM:open_gmail:start gmail
```

---

## Testing

### Unit Tests

Test intent parsing:
```kotlin
@Test
fun `parse v2 universal format`() {
    val content = """
        # Avanues Universal Format v1.0
        ---
        schema: avu-1.0
        version: 1.0.0
        locale: en-US
        ---
        VCM:test:test command
    """.trimIndent()
    
    val avaFile = AvaFileParser.parse(content)
    assertEquals("avu-1.0", avaFile.schema)
    assertEquals(1, avaFile.intents.size)
    assertEquals("VCM", avaFile.intents[0].ipcCode)
}
```

### Integration Tests

Test database insertion:
```kotlin
@Test
fun `insert v2 intents into database`() {
    val intent = AvaIntent(
        id = "test",
        canonical = "test command",
        synonyms = listOf("test"),
        category = "voice_command",
        ipcCode = "VCM",
        source = "UNIVERSAL_V2"
    )
    
    val entities = AvaToEntityConverter.convertToEntities(listOf(intent))
    intentExampleDao.insertAll(entities)
    
    val result = intentExampleDao.getByIntentId("test")
    assertEquals("v2.0", result[0].formatVersion)
    assertEquals("VCM", result[0].ipcCode)
}
```

---

## References

### Documentation
- **Universal Format Spec:** `/Volumes/M-Drive/Coding/Avanues/docs/specifications/UNIVERSAL-FILE-FORMAT-FINAL.md`
- **Universal IPC Spec:** `/Volumes/M-Drive/Coding/Avanues/docs/specifications/UNIVERSAL-IPC-SPEC.md`
- **README:** `apps/ava-standalone/src/main/assets/ava-examples/README.md`

### Code
- **Parser:** `Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/ava/parser/AvaFileParser.kt`
- **Converter:** `Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/ava/converter/AvaToEntityConverter.kt`
- **Entity:** `Universal/AVA/Core/Data/src/main/java/com/augmentalis/ava/core/data/entity/IntentExampleEntity.kt`
- **Migration:** `Universal/AVA/Core/Data/src/main/java/com/augmentalis/ava/core/data/migration/DatabaseMigrations.kt`
- **UniversalFileParser:** `/Volumes/M-Drive/Coding/Avanues/modules/AVAMagic/IPC/UniversalIPC/`

---

## Troubleshooting

### Error: "Invalid .ava file: Must use Universal Format v2.0"

**Cause:** File is in legacy v1.0 JSON format  
**Solution:** Convert file to v2.0 Universal Format using the template above

### Error: "Invalid Universal Format: expected at least 3 sections"

**Cause:** Missing `---` section delimiters  
**Solution:** Ensure file has header, metadata, and entries sections separated by `---`

### Error: Database constraint violation

**Cause:** Duplicate exampleHash (same intent ID + example text)  
**Solution:** Remove duplicate entries from .ava file

### Intent not matching at runtime

**Cause:** Example text doesn't match user input closely enough  
**Solution:** Add more synonym variations to the intent

---

## Changelog

### v2.0 (November 2025) - Current
- ✅ Migrated all .ava files to Universal Format v2.0
- ✅ Removed v1.0 JSON parsing code
- ✅ Added IPC code support (VCM, AIQ, STT, CTX, SUG)
- ✅ Database schema v4 with format tracking
- ✅ Cross-project file reading via UniversalFileParser
- ✅ 124 total intents across 4 files

### v1.0 (2024) - Deprecated
- Legacy JSON format
- No longer supported as of November 2025

---

**Status:** ✅ Production Ready  
**Format:** Universal v2.0 ONLY  
**Total Intents:** 124 (8 navigation, 10 media, 12 system, 94 voiceos)
