# AVA File Format Specification v2.0

**Version:** 2.0.0
**Date:** 2025-11-20
**Status:** Proposed
**Replaces:** Developer-Manual-Chapter37-AVA-File-Format.md (v1.0)

---

## Overview

AVA File Format v2.0 integrates **Universal IPC protocol codes** into the .ava storage format, creating a unified ecosystem where voice intents are IPC-ready.

**Key Changes from v1.0:**
- ✅ **Universal DSL Integration:** Intent examples now use IPC codes (VCM, AIQ, STT)
- ✅ **IPC-Ready:** Zero conversion when sending intents via IPC
- ✅ **Backward Compatible:** Old format still parseable
- ✅ **Ecosystem Consistency:** Same codes across AVA, VoiceOS, AvaConnect, BrowserAvanue

**References:**
- [Universal IPC Specification](UNIVERSAL-IPC-SPEC.md)
- [Universal DSL Specification](UNIVERSAL-DSL-SPEC.md)

---

## Table of Contents

1. [File Format v2.0](#file-format-v20)
2. [Universal DSL Integration](#universal-dsl-integration)
3. [Intent Categories & IPC Codes](#intent-categories--ipc-codes)
4. [Migration from v1.0](#migration-from-v10)
5. [IPC Workflow](#ipc-workflow)
6. [Examples](#examples)

---

## File Format v2.0

### Schema Version

```json
{
  "s": "ava-2.0",
  "v": "2.0.0",
  "l": "en-US",
  "m": { ... },
  "i": [ ... ],
  "syn": { ... }
}
```

**Schema identifier changed:** `"ava-1.0"` → `"ava-2.0"`

### Field Definitions

#### Root Level (UNCHANGED)
- `s` (schema): Schema version (`"ava-2.0"`)
- `v` (version): File format version (`"2.0.0"`)
- `l` (locale): Language locale (`"en-US"`, `"es-ES"`)
- `m` (metadata): File metadata object
- `i` (intents): Array of intent objects
- `syn` (synonyms): Optional global synonym mappings

#### Intent Object (NEW FIELDS)

```json
{
  "id": "open_gmail",
  "ipc": "VCM::open gmail",           // NEW: IPC-ready message
  "c": "open gmail",
  "s": [
    "VCM::launch gmail",               // NEW: IPC codes in synonyms
    "VCM::start gmail"
  ],
  "cat": "voice_command",              // CHANGED: Align with IPC categories
  "p": 1,
  "t": ["email", "navigation"],
  "l": "en-US",
  "src": "CORE"
}
```

**New Fields:**
- `ipc` (string): Pre-formatted Universal IPC message for canonical example
  * Format: `CODE::text` where CODE is VCM, AIQ, STT, etc.
  * Used for direct IPC transmission (zero conversion)
  * Optional for backward compatibility

**Changed Fields:**
- `c` (canonical): Now stores plain text (IPC code in `ipc` field)
- `s` (synonyms): Can include IPC codes (e.g., `"VCM::launch gmail"`)
- `cat` (category): Now uses IPC category names (see mapping below)

---

## Universal DSL Integration

### IPC Code Assignment

Each intent category maps to Universal IPC codes:

| AVA Category | IPC Code | Example |
|--------------|----------|---------|
| `voice_command` | `VCM` | `VCM::open gmail` |
| `ai_query` | `AIQ` | `AIQ::what's the weather` |
| `speech_text` | `STT` | `STT::hello world` |
| `navigation` | `URL` | `URL::https://gmail.com` |
| `device_control` | `VCM` | `VCM::turn on lights` |
| `media_control` | `VCM` | `VCM::play music` |
| `system_control` | `VCM` | `VCM::increase volume` |

### IPC Message Format

Format: `CODE::text` (double colon, no ID for intent examples)

**Why no request ID in .ava files?**
- Request IDs are generated at runtime when sending IPC
- .ava files store TEMPLATES, not actual messages
- At IPC send time: `VCM::open gmail` → `VCM:cmd123:open gmail`

**Examples:**
```json
"ipc": "VCM::open gmail"      // Voice command template
"ipc": "AIQ::weather today"   // AI query template
"ipc": "STT::hello world"     // Speech-to-text template
```

### Synonym Handling

**Option 1: IPC in synonyms (recommended)**
```json
{
  "id": "open_gmail",
  "ipc": "VCM::open gmail",
  "c": "open gmail",
  "s": [
    "VCM::launch gmail",
    "VCM::start gmail app",
    "VCM::open my email"
  ]
}
```

**Option 2: Plain text (backward compatible)**
```json
{
  "id": "open_gmail",
  "ipc": "VCM::open gmail",
  "c": "open gmail",
  "s": [
    "launch gmail",           // Parser adds VCM:: prefix from category
    "start gmail app",
    "open my email"
  ]
}
```

---

## Intent Categories & IPC Codes

### Category Mapping Table

| Category | IPC Code | Description | Example Intent |
|----------|----------|-------------|----------------|
| `voice_command` | `VCM` | General voice commands | "open gmail", "turn on lights" |
| `ai_query` | `AIQ` | Questions to AI | "what's the weather", "how do I..." |
| `speech_text` | `STT` | Dictation/text input | User speech → text |
| `media_control` | `VCM` | Media playback | "play music", "pause video" |
| `device_control` | `VCM` | Smart device control | "turn on lights", "set thermostat" |
| `navigation` | `URL` | App/web navigation | "open gmail", "go to settings" |
| `system_control` | `VCM` | System settings | "increase volume", "enable wifi" |
| `context_share` | `CTX` | Share context with apps | Location, activity, etc. |
| `suggestion` | `SUG` | Command suggestions | "Try saying X" |

### Multi-Category Intents

Some intents fit multiple categories:

```json
{
  "id": "open_gmail",
  "ipc": "VCM::open gmail",
  "c": "open gmail",
  "cat": "voice_command",
  "t": ["navigation", "email"],   // Tags for secondary categories
  "alt_ipc": {                    // Alternative IPC formats
    "navigation": "URL::https://gmail.com",
    "ai_query": "AIQ::open my email"
  }
}
```

---

## Migration from v1.0

### Automatic Migration

The parser automatically upgrades v1.0 files:

```kotlin
fun parse(jsonString: String): AvaFile {
    val root = JSONObject(jsonString)
    val schema = root.getString("s")

    return when (schema) {
        "ava-1.0" -> parseV1AndUpgrade(root)  // Auto-upgrade
        "ava-2.0" -> parseV2(root)            // Native v2.0
        else -> throw IllegalArgumentException("Unknown schema: $schema")
    }
}

private fun parseV1AndUpgrade(root: JSONObject): AvaFile {
    val intents = root.getJSONArray("i")
    val upgradedIntents = mutableListOf<AvaIntent>()

    for (i in 0 until intents.length()) {
        val intent = intents.getJSONObject(i)
        val category = intent.getString("cat")
        val canonical = intent.getString("c")

        // Generate IPC field based on category
        val ipcCode = mapCategoryToIPCCode(category)
        val ipcMessage = "$ipcCode::$canonical"

        upgradedIntents.add(AvaIntent(
            id = intent.getString("id"),
            ipc = ipcMessage,                    // NEW
            canonical = canonical,
            synonyms = parseSynonyms(intent),
            category = category,
            priority = intent.getInt("p"),
            tags = parseTags(intent),
            locale = root.getString("l"),
            source = intent.optString("src", "UNKNOWN")
        ))
    }

    return AvaFile(
        schema = "ava-2.0",  // Upgraded
        version = "2.0.0",
        locale = root.getString("l"),
        metadata = parseMetadata(root),
        intents = upgradedIntents,
        globalSynonyms = parseSynonyms(root)
    )
}
```

### Manual Conversion Tool

```bash
# Convert v1.0 → v2.0 (planned)
./gradlew convertAvaToV2 \
  --input /.ava/core/en-US/ \
  --output /.ava/core-v2/en-US/ \
  --validate
```

### Backward Compatibility

**v2.0 parser can read v1.0 files:**
- Auto-detects schema version
- Generates `ipc` field on-the-fly
- No data loss

**v1.0 parser cannot read v2.0 files:**
- Unknown `ipc` field ignored (JSON tolerant)
- Still functions but without IPC integration

**Migration Strategy:**
1. **Phase 1:** Deploy v2.0 parser (reads both formats)
2. **Phase 2:** Migrate core .ava files to v2.0
3. **Phase 3:** Remove v1.0 fallback (future)

---

## IPC Workflow

### Voice Command → IPC Message

**Step 1: User speaks**
```
User: "open gmail"
```

**Step 2: AVA matches intent**
```kotlin
val intent = intentMatcher.match("open gmail")
// Returns: AvaIntent(id="open_gmail", ipc="VCM::open gmail", ...)
```

**Step 3: Generate IPC message**
```kotlin
val ipcMessage = VoiceCommandMessage(
    commandId = UUID.randomUUID().toString(),  // Generate runtime ID
    command = intent.canonical                  // Use canonical text
)
// Serializes to: "VCM:cmd123:open gmail"
```

**Step 4: Send via Universal IPC**
```kotlin
ipcManager.send(
    target = "com.augmentalis.voiceos",
    message = ipcMessage
)
```

### AI Query → IPC Message

**Step 1: User asks question**
```
User: "what's the weather today"
```

**Step 2: AVA detects AI query category**
```kotlin
val intent = intentMatcher.match("what's the weather today")
// Returns: AvaIntent(id="weather_query", ipc="AIQ::what's the weather", ...)
```

**Step 3: Route to AI via IPC**
```kotlin
val aiQuery = AIQueryMessage(
    queryId = "q1",
    query = intent.canonical
)
// Serializes to: "AIQ:q1:what's the weather today"

ipcManager.send("com.augmentalis.voiceos", aiQuery)
```

**Step 4: Receive AI response**
```kotlin
ipcManager.subscribe<AIResponseMessage>().collect { response ->
    // Received: "AIR:q1:It's sunny and 72°F"
    speak(response.responseText)
}
```

---

## Examples

### Example 1: Voice Command Intent (v2.0)

```json
{
  "s": "ava-2.0",
  "v": "2.0.0",
  "l": "en-US",
  "m": {
    "f": "navigation.ava",
    "c": "navigation",
    "n": "Navigation Commands",
    "d": "App and web navigation intents",
    "cnt": 3
  },
  "i": [
    {
      "id": "open_gmail",
      "ipc": "VCM::open gmail",
      "c": "open gmail",
      "s": [
        "VCM::launch gmail",
        "VCM::start gmail app",
        "VCM::open my email"
      ],
      "cat": "voice_command",
      "p": 1,
      "t": ["email", "navigation"],
      "l": "en-US",
      "src": "CORE"
    },
    {
      "id": "open_settings",
      "ipc": "VCM::open settings",
      "c": "open settings",
      "s": [
        "VCM::go to settings",
        "VCM::show settings menu"
      ],
      "cat": "voice_command",
      "p": 2,
      "t": ["system", "navigation"],
      "l": "en-US",
      "src": "CORE"
    }
  ],
  "syn": {
    "open": ["launch", "start", "go to"],
    "settings": ["preferences", "options"]
  }
}
```

### Example 2: AI Query Intent (v2.0)

```json
{
  "s": "ava-2.0",
  "v": "2.0.0",
  "l": "en-US",
  "m": {
    "f": "ai-queries.ava",
    "c": "ai_query",
    "n": "AI Queries",
    "d": "Questions for AI assistant",
    "cnt": 4
  },
  "i": [
    {
      "id": "weather_query",
      "ipc": "AIQ::what's the weather",
      "c": "what's the weather",
      "s": [
        "AIQ::weather today",
        "AIQ::how's the weather",
        "AIQ::temperature outside"
      ],
      "cat": "ai_query",
      "p": 1,
      "t": ["weather", "information"]
    },
    {
      "id": "how_to_query",
      "ipc": "AIQ::how do I cook pasta",
      "c": "how do I cook pasta",
      "s": [
        "AIQ::teach me to cook pasta",
        "AIQ::pasta recipe",
        "AIQ::how to make pasta"
      ],
      "cat": "ai_query",
      "p": 1,
      "t": ["cooking", "tutorial"]
    }
  ]
}
```

### Example 3: Mixed Categories (v2.0)

```json
{
  "s": "ava-2.0",
  "v": "2.0.0",
  "l": "en-US",
  "m": {
    "f": "mixed-intents.ava",
    "c": "mixed",
    "n": "Mixed Intents",
    "d": "Intents spanning multiple categories",
    "cnt": 2
  },
  "i": [
    {
      "id": "smart_home_lights",
      "ipc": "VCM::turn on lights",
      "c": "turn on lights",
      "s": [
        "VCM::lights on",
        "VCM::enable lights",
        "VCM::activate lights"
      ],
      "cat": "voice_command",
      "p": 1,
      "t": ["smart_home", "device_control"]
    },
    {
      "id": "context_location",
      "ipc": "CTX::share location",
      "c": "share my location",
      "s": [
        "CTX::where am i",
        "CTX::current location"
      ],
      "cat": "context_share",
      "p": 2,
      "t": ["context", "location"]
    }
  ]
}
```

### Example 4: Backward Compatible (v1.0 file, v2.0 parser)

**Original v1.0 file:**
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

**Auto-upgraded to v2.0 in memory:**
```kotlin
AvaIntent(
    id = "open_gmail",
    ipc = "VCM::open gmail",      // Generated from category
    canonical = "open gmail",
    synonyms = listOf(
        "VCM::launch gmail",      // Prefix added
        "VCM::start gmail"
    ),
    category = "navigation",
    priority = 1
)
```

---

## Size Comparison

### v1.0 vs v2.0

**v1.0 Format:**
```json
{
  "id": "open_gmail",
  "c": "open gmail",
  "s": ["launch gmail"],
  "cat": "navigation",
  "p": 1
}
```
**Size:** ~85 bytes

**v2.0 Format (with IPC):**
```json
{
  "id": "open_gmail",
  "ipc": "VCM::open gmail",
  "c": "open gmail",
  "s": ["VCM::launch gmail"],
  "cat": "voice_command",
  "p": 1
}
```
**Size:** ~110 bytes (+29%)

**Trade-off:**
- ❌ **+29% file size** per intent
- ✅ **Zero conversion overhead** at runtime
- ✅ **IPC-ready** for instant transmission
- ✅ **Ecosystem consistency**

**Mitigation:**
- Compress .ava files (gzip: ~40% reduction)
- Store on device (not in APK)
- Load lazily (only needed intents)

---

## Parser Changes

### AvaFileParser v2.0

```kotlin
object AvaFileParser {

    fun parse(jsonString: String): AvaFile {
        val root = JSONObject(jsonString)
        val schema = root.getString("s")

        return when {
            schema.startsWith("ava-2.") -> parseV2(root)
            schema.startsWith("ava-1.") -> parseV1AndUpgrade(root)
            else -> throw IllegalArgumentException("Unsupported schema: $schema")
        }
    }

    private fun parseV2(root: JSONObject): AvaFile {
        val intentsArray = root.getJSONArray("i")
        val intents = mutableListOf<AvaIntent>()

        for (i in 0 until intentsArray.length()) {
            val intentJson = intentsArray.getJSONObject(i)

            intents.add(AvaIntent(
                id = intentJson.getString("id"),
                ipc = intentJson.optString("ipc", null),  // v2.0 field
                canonical = intentJson.getString("c"),
                synonyms = parseSynonyms(intentJson.optJSONArray("s")),
                category = intentJson.getString("cat"),
                priority = intentJson.getInt("p"),
                tags = parseTags(intentJson.optJSONArray("t")),
                locale = intentJson.optString("l", root.getString("l")),
                source = intentJson.optString("src", "UNKNOWN")
            ))
        }

        return AvaFile(
            schema = root.getString("s"),
            version = root.getString("v"),
            locale = root.getString("l"),
            metadata = parseMetadata(root.getJSONObject("m")),
            intents = intents,
            globalSynonyms = parseGlobalSynonyms(root.optJSONObject("syn"))
        )
    }

    private fun parseV1AndUpgrade(root: JSONObject): AvaFile {
        // Auto-upgrade v1.0 → v2.0
        val intentsArray = root.getJSONArray("i")
        val intents = mutableListOf<AvaIntent>()

        for (i in 0 until intentsArray.length()) {
            val intentJson = intentsArray.getJSONObject(i)
            val category = intentJson.getString("cat")
            val canonical = intentJson.getString("c")

            // Generate IPC code from category
            val ipcCode = mapCategoryToIPCCode(category)
            val ipcMessage = "$ipcCode::$canonical"

            intents.add(AvaIntent(
                id = intentJson.getString("id"),
                ipc = ipcMessage,  // GENERATED
                canonical = canonical,
                synonyms = parseSynonyms(intentJson.optJSONArray("s"))
                    .map { "$ipcCode::$it" },  // Add prefix
                category = category,
                priority = intentJson.getInt("p"),
                tags = parseTags(intentJson.optJSONArray("t")),
                locale = root.getString("l"),
                source = intentJson.optString("src", "UNKNOWN")
            ))
        }

        return AvaFile(
            schema = "ava-2.0",  // Upgraded
            version = "2.0.0",
            locale = root.getString("l"),
            metadata = parseMetadata(root.optJSONObject("m")),
            intents = intents,
            globalSynonyms = parseGlobalSynonyms(root.optJSONObject("syn"))
        )
    }

    private fun mapCategoryToIPCCode(category: String): String {
        return when (category) {
            "navigation", "voice_command", "device_control",
            "media_control", "system_control" -> "VCM"
            "ai_query" -> "AIQ"
            "speech_text" -> "STT"
            "context_share" -> "CTX"
            "suggestion" -> "SUG"
            else -> "VCM"  // Default
        }
    }
}
```

### Data Model Changes

```kotlin
data class AvaIntent(
    val id: String,
    val ipc: String?,              // NEW: IPC-ready message template
    val canonical: String,
    val synonyms: List<String>,
    val category: String,
    val priority: Int,
    val tags: List<String> = emptyList(),
    val locale: String,
    val source: String
) {
    /**
     * Convert to Universal IPC message at runtime
     */
    fun toIPCMessage(requestId: String = UUID.randomUUID().toString()): UniversalMessage {
        val ipcTemplate = ipc ?: "VCM::$canonical"
        val code = ipcTemplate.substringBefore("::")
        val text = ipcTemplate.substringAfter("::")

        return when (code) {
            "VCM" -> VoiceCommandMessage(requestId, text)
            "AIQ" -> AIQueryMessage(requestId, text)
            "STT" -> SpeechToTextMessage(requestId, text)
            "CTX" -> ContextShareMessage(requestId, text)
            "SUG" -> SuggestionMessage(requestId, text)
            else -> VoiceCommandMessage(requestId, text)
        }
    }
}
```

---

## References

- [Universal IPC Specification](UNIVERSAL-IPC-SPEC.md)
- [Universal DSL Specification](UNIVERSAL-DSL-SPEC.md)
- [IPC Research Summary](IPC-RESEARCH-SUMMARY.md)
- [AVA Developer Manual Chapter 37 (v1.0)](../AVA/docs/Developer-Manual-Chapter37-AVA-File-Format.md)

---

## Changelog

**v2.0.0 (2025-11-20):**
- Universal IPC integration
- New `ipc` field for intent objects
- Category mapping to IPC codes
- Backward compatible parser
- Auto-upgrade from v1.0

**v1.0.0 (2025-11-17):**
- Initial .ava format
- 66% size reduction vs verbose JSON
- Multi-source loading (core/voiceos/user)

---

## License

**Proprietary - Augmentalis ES**

All rights reserved.

---

## Author

**Manoj Jhawar**
Email: manoj@ideahq.net
IDEACODE Version: 8.4
