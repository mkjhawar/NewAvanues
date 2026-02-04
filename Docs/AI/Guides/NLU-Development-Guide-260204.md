# NLU Development Guide

**Date**: 2026-02-04
**Module**: Modules/AI/NLU
**Status**: Active

---

## Overview

This guide explains how to work on the Natural Language Understanding (NLU) module, including adding intents, creating language packs, and extending the system.

## Module Structure

```
Modules/AI/NLU/
├── src/
│   ├── commonMain/kotlin/com/augmentalis/nlu/
│   │   ├── model/                 # Data models (UnifiedIntent, etc.)
│   │   ├── parser/                # AVU intent parser
│   │   └── NLUEngine.kt           # Core engine interface
│   ├── androidMain/kotlin/com/augmentalis/nlu/
│   │   ├── ava/                   # AVA file handling
│   │   │   ├── converter/         # Entity converters
│   │   │   ├── io/                # File I/O
│   │   │   └── parser/            # File parsers
│   │   ├── aon/                   # Ontology loader
│   │   ├── learning/              # Intent learning manager
│   │   ├── migration/             # Intent source coordinator
│   │   ├── ModelManager.kt        # Model lifecycle
│   │   └── NLUEngineImpl.kt       # Android implementation
│   └── commonTest/                # Unit tests
└── build.gradle.kts
```

## Getting Started

### Prerequisites

1. **Development Environment**:
   - Android Studio or IntelliJ IDEA
   - Kotlin 1.9+
   - Gradle 8+

2. **Dependencies**:
   ```kotlin
   // build.gradle.kts
   implementation(project(":Modules:AI:NLU"))
   implementation(project(":Modules:AVA:core:Data"))  // SQLDelight
   implementation(project(":Modules:AVUCodec"))       // Format parsing
   ```

3. **Database Setup**:
   - SQLDelight generates database classes from `.sq` files
   - Run `./gradlew :Modules:AI:NLU:generateSqlDelightInterface`

### Key Files to Understand

| File | Purpose |
|------|---------|
| `NLUEngine.kt` | Core interface for intent recognition |
| `UnifiedIntent.kt` | Intent data model |
| `IntentSourceCoordinator.kt` | Loads intents from external files |
| `AvaFileParser.kt` | Parses .ava intent files |
| `AvuIntentParser.kt` | Parses/generates AVU 2.2 format |
| `IntentLearningManager.kt` | Learns new intents from usage |

## Working with Intents

### Intent File Format (AVU 2.2)

```
# Avanues Universal Format v2.2
# Type: AVA
# Extension: .aai
---
schema: avu-2.2
version: 2.2.0
locale: en-US
project: shared
metadata:
  file: navigation.aai
  category: nlu_intents
  count: 5
---
INT:nav_back:go back:navigation:10:GLOBAL_ACTION_BACK
PAT:nav_back:go back
PAT:nav_back:navigate back
PAT:nav_back:return
SYN:nav_back:previous
EMB:nav_back:mobilebert-384:384:base64encodedvector
```

### Line Codes

| Code | Format | Description |
|------|--------|-------------|
| `INT` | `INT:id:canonical:category:priority:action` | Intent definition |
| `PAT` | `PAT:intent_id:pattern` | Recognition pattern |
| `SYN` | `SYN:intent_id:synonym` | Synonym for pattern matching |
| `EMB` | `EMB:intent_id:model:dim:base64` | Embedding vector |
| `ACT` | `ACT:intent_id:action_type:params` | Action binding |

### Adding New Intents

1. **Create Intent File**:
   ```kotlin
   // Place in: assets/intents/<locale>/custom.aai
   ```

2. **Use AvuIntentParser**:
   ```kotlin
   val parser = AvuIntentParser()
   val result = parser.parse(fileContent)

   if (result.errors.isEmpty()) {
       val intents = result.intents
       // Process intents
   }
   ```

3. **Generate Intent File**:
   ```kotlin
   val intents = listOf(
       UnifiedIntent(
           id = "custom_action",
           canonicalPhrase = "do custom action",
           category = "custom",
           priority = 5,
           actionId = "CUSTOM_ACTION",
           patterns = listOf("do custom action", "perform custom"),
           synonyms = listOf("execute custom")
       )
   )

   val avuContent = parser.generate(intents, locale = "en-US")
   // Write to file
   ```

## External File Injection

### Storage Paths

```kotlin
// IntentSourceCoordinator.kt
private val storageBase = AssetExtractor.getStorageBasePath(context)
private val corePath = "$storageBase/core"      // Core system intents
private val voiceosPath = "$storageBase/voiceos" // VoiceOS intents
private val userPath = "$storageBase/user"       // User custom intents
```

### Priority Order

1. **User** (`/user/`) - Highest priority, user customizations
2. **VoiceOS** (`/voiceos/`) - App-specific intents
3. **Core** (`/core/`) - System defaults

### Injecting New Intent Files

```kotlin
// Copy intent file to external storage
val intentFile = File(context.getExternalFilesDir(null), "user/intents/custom.aai")
intentFile.parentFile?.mkdirs()
intentFile.writeText(avuContent)

// Trigger reload
val coordinator = IntentSourceCoordinator(context)
coordinator.reloadFromPath(userPath)
```

### Database Update Flow

```
External File (.aai)
       │
       ▼
┌──────────────────┐
│ AvaFileParser    │
│ - Parse AVU/JSON │
│ - Validate       │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ AvaToEntity      │
│ Converter        │
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│ SQLDelight       │
│ Insert/Update    │
└──────────────────┘
```

## Localization

### Adding Language Support

1. **Create Locale Folder**:
   ```
   assets/intents/
   ├── en-US/
   │   ├── navigation.aai
   │   └── system.aai
   ├── es-ES/
   │   ├── navigation.aai
   │   └── system.aai
   └── fr-FR/
       └── ...
   ```

2. **Update LanguagePackManager**:
   ```kotlin
   // Add new locale to supported list
   val supportedLocales = setOf(
       "en-US", "es-ES", "fr-FR", "de-DE", ...
   )
   ```

3. **Translate Intent Patterns**:
   ```
   # Spanish navigation.aai
   INT:nav_back:volver atrás:navigation:10:GLOBAL_ACTION_BACK
   PAT:nav_back:volver atrás
   PAT:nav_back:regresar
   SYN:nav_back:anterior
   ```

## Intent Learning

### IntentLearningManager

```kotlin
// Record successful intent usage
learningManager.recordSuccess(
    intentId = "nav_back",
    phrase = "go back please",  // Actual user phrase
    context = mapOf("app" to "com.example")
)

// The system learns variations over time
```

### Learning Database

```sql
-- intent_example table
id, intent_id, text, locale, source, format_version, created_at

-- Learned examples have source = "LEARNED"
```

## Testing

### Unit Tests

```kotlin
// Location: src/commonTest/kotlin/
class AvuIntentParserTest {
    @Test
    fun `parse valid AVU file`() {
        val content = """
            # Avanues Universal Format v2.2
            ---
            schema: avu-2.2
            ---
            INT:test:test phrase:test:1:TEST_ACTION
            PAT:test:test phrase
        """.trimIndent()

        val result = parser.parse(content)

        assertEquals(1, result.intents.size)
        assertEquals("test", result.intents[0].id)
    }
}
```

### Integration Tests

```kotlin
// Location: src/androidTest/kotlin/
class IntentSourceCoordinatorTest {
    @Test
    fun migrateFromExternalFiles() = runBlocking {
        // Place test files in test assets
        val coordinator = IntentSourceCoordinator(context)

        val success = coordinator.migrateIfNeeded()

        assertTrue(success)
        // Verify database populated
    }
}
```

## Common Tasks

### Task: Add New Intent Category

1. Define intents in `.aai` file
2. Add to assets folder
3. Test with `AvuIntentParser`
4. Deploy and trigger migration

### Task: Customize Patterns for Locale

1. Copy English `.aai` file
2. Translate patterns and synonyms
3. Keep intent IDs the same
4. Place in locale folder

### Task: Debug Intent Recognition

```kotlin
// Enable logging
Log.d("NLU", "Recognized: ${intent.id} from '${phrase}'")
Log.d("NLU", "Confidence: ${result.confidence}")
Log.d("NLU", "Patterns matched: ${result.matchedPatterns}")
```

## API Reference

### NLUEngine Interface

```kotlin
interface NLUEngine {
    suspend fun recognize(text: String, context: Map<String, Any>): RecognitionResult
    suspend fun loadIntents(locale: String): Int
    fun getAvailableIntents(): List<String>
}
```

### RecognitionResult

```kotlin
data class RecognitionResult(
    val intent: UnifiedIntent?,
    val confidence: Float,
    val matchedPatterns: List<String>,
    val alternatives: List<UnifiedIntent>
)
```

---

## Next Steps for NLU Work

1. **Review existing intents**: Check `assets/intents/` for current coverage
2. **Identify gaps**: What commands/intents are missing?
3. **Create intent files**: Use AVU 2.2 format
4. **Test parsing**: Use `AvuIntentParser` to validate
5. **Deploy**: Add to assets or external storage
6. **Monitor learning**: Check `IntentLearningManager` logs

---

*Last updated: 2026-02-04*
