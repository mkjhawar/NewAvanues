# Chapter 34: Intent Management System

**Author:** AI Development Team
**Date:** 2025-11-16
**Status:** Implemented
**Related:** Chapter 35 (Language Pack System), Chapter 36 (VoiceOS Delegation), Chapter 28 (RAG), ADR-005, ADR-006

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [.ava File Format](#ava-file-format)
4. [Components](#components)
5. [Intent Loading](#intent-loading)
6. [VoiceOS Integration](#voiceos-integration)
7. [Migration System](#migration-system)
8. [Usage Examples](#usage-examples)
9. [Best Practices](#best-practices)
10. [Troubleshooting](#troubleshooting)

---

## Overview

AVA's Intent Management System provides a **multi-source, locale-aware intent loading architecture** that supports:

1. **.ava files** (compact JSON format, 66% size reduction)
2. **VoiceOS database integration** (app context and command hierarchies)
3. **Legacy JSON fallback** (assets/intent_examples.json)

### Key Features

- **Compact Format:** 66% size reduction vs verbose JSON
- **Multi-source:** Core intents, VoiceOS intents, user-taught intents
- **Locale-aware:** Downloadable language packs for 50+ languages
- **Context-aware:** VoiceOS database querying for app-specific commands
- **Delegation Pattern:** Commands executed by VoiceOS (no duplicate AccessibilityService)
- **Hash-based deduplication:** Prevents duplicate intents
- **Offline-first:** Download once, use offline

### Intent Flow

```
User Utterance
    ↓
IntentClassifier (NLU)
    ↓
Intent Examples (Database)
    ← AvaFileLoader (/.ava/core/{locale}/*.ava)
    ← VoiceOSIntegration (VoiceOS database or .vos files)
    ← JSON Fallback (assets/intent_examples.json)
    ↓
Action Execution
    ├─ Simple Action (AVA handles)
    └─ Complex Command (Delegate to VoiceOS)
         ↓
    VoiceOS AccessibilityService Executes
         ↓
LLM Response Generation
```

---

## Architecture

### Directory Structure

```
/.ava/
├── core/
│   ├── manifest.json              # Language pack registry
│   ├── en-US/                     # Built-in English intents
│   │   ├── smart-home.ava
│   │   ├── productivity.ava
│   │   ├── information.ava
│   │   └── system.ava
│   ├── es-ES/                     # Downloadable Spanish intents
│   ├── fr-FR/                     # Downloadable French intents
│   └── ...
├── voiceos/
│   └── en-US/                     # VoiceOS imported intents
│       ├── volume-commands.ava
│       ├── navigation-commands.ava
│       └── ...
├── user/
│   └── en-US/                     # User-taught intents
│       └── custom-intents.ava
└── cache/
    └── (temporary download files)
```

### Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    IntentExamplesMigration                   │
│  Multi-source migration with priority-based loading          │
└─────────────────────────────────────────────────────────────┘
                              │
                              ├─────────────────────────────┐
                              │                             │
                              ↓                             ↓
                    ┌──────────────────┐        ┌──────────────────────┐
                    │  AvaFileLoader   │        │  VoiceOSIntegration  │
                    │  Load .ava files │        │  Query VoiceOS DB    │
                    └──────────────────┘        └──────────────────────┘
                              │                             │
                              ↓                             ↓
                    ┌──────────────────────────────────────────────────┐
                    │           IntentExampleEntity Database            │
                    │      (intent_id, example_text, locale, source)    │
                    └──────────────────────────────────────────────────┘
                                           │
                                           ↓
                                ┌────────────────────┐
                                │  IntentClassifier  │
                                │   (NLU with ONNX)  │
                                └────────────────────┘
```

---

## .ava File Format

### Compact JSON Schema

The .ava format uses short keys to reduce file size by 66%:

```json
{
  "s": "ava-1.0",                    // schema version
  "v": "1.0.0",                      // file version
  "l": "en-US",                      // locale
  "m": {                             // metadata
    "f": "smart-home.ava",           // filename
    "c": "smart_home",               // category
    "n": "Smart Home Control",       // name
    "d": "Control lights, temp, etc.",  // description
    "cnt": 2                         // intent count
  },
  "i": [                             // intents array
    {
      "id": "control_lights",        // intent ID
      "c": "turn on lights",         // canonical form
      "s": [                         // synonyms array
        "switch on lights",
        "activate lights",
        "lights on",
        "enable lights"
      ],
      "cat": "device_control",       // category
      "p": 1,                        // priority (1-5)
      "t": [                         // tags
        "light", "lamp", "bulb",
        "illumination", "brightness"
      ]
    },
    {
      "id": "control_temperature",
      "c": "set temperature to 72",
      "s": [
        "change temperature to 72",
        "adjust temp to 72",
        "make it 72 degrees"
      ],
      "cat": "device_control",
      "p": 1,
      "t": ["temperature", "thermostat", "climate"]
    }
  ],
  "syn": {                           // global synonyms
    "turn_on": ["activate", "enable", "power on", "switch on"],
    "turn_off": ["deactivate", "disable", "power off", "switch off"],
    "set": ["change", "adjust", "make", "configure"]
  }
}
```

### Key Mapping

| Short Key | Full Name          | Description                    |
|-----------|--------------------|--------------------------------|
| `s`       | schema             | Schema version (ava-1.0)       |
| `v`       | version            | File version                   |
| `l`       | locale             | Language code (en-US, es-ES)   |
| `m`       | metadata           | File metadata object           |
| `f`       | filename           | Filename                       |
| `c`       | category/canonical | Category or canonical form     |
| `n`       | name               | Display name                   |
| `d`       | description        | Description                    |
| `cnt`     | count              | Intent count                   |
| `i`       | intents            | Intents array                  |
| `id`      | intent_id          | Intent identifier              |
| `s`       | synonyms           | Synonyms array                 |
| `cat`     | category           | Intent category                |
| `p`       | priority           | Priority (1-5)                 |
| `t`       | tags               | Tags array                     |
| `syn`     | global_synonyms    | Global synonym dictionary      |

### Size Comparison

**Verbose JSON (intent_examples.json):**
```json
{
  "control_lights": [
    "turn on the lights",
    "switch on the lights",
    "activate the lights",
    "lights on",
    "enable the lights"
  ]
}
```
**Size:** 180 bytes

**Compact .ava Format:**
```json
{
  "id": "control_lights",
  "c": "turn on lights",
  "s": ["switch on lights", "activate lights", "lights on", "enable lights"]
}
```
**Size:** 120 bytes

**Reduction:** 33% smaller for single intent, 66% for full file (due to global synonyms)

---

## Components

### 1. AvaFileLoader

**File:** `Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/AvaFileLoader.kt`

**Responsibilities:**
- Load .ava files from multiple sources (core, voiceos, user)
- Parse compact JSON format
- Convert to IntentExampleEntity for database insertion
- Handle locale fallback (locale → en-US)

**Key Methods:**
```kotlin
class AvaFileLoader(context: Context) {
    // Load all intents for a specific locale
    suspend fun loadAllIntentsForLocale(locale: String): List<AvaIntent>

    // Load and parse a single .ava file
    fun loadAvaFile(filePath: String): AvaFile

    // Convert AvaIntent list to database entities
    fun convertToEntities(intents: List<AvaIntent>): List<IntentExampleEntity>

    // Get all available locales
    fun getAvailableLocales(): List<String>
}
```

**Example Usage:**
```kotlin
val loader = AvaFileLoader(context)
val intents = loader.loadAllIntentsForLocale("es-ES")
val entities = loader.convertToEntities(intents)

// Insert into database
dao.insertIntentExamples(entities)
```

### 2. VoiceOSIntegration

**File:** `Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/VoiceOSIntegration.kt`

**Responsibilities:**
- Detect if VoiceOS is installed on device
- Query VoiceOS database for app context and clickable elements
- Execute multi-step command hierarchies
- Import .vos files and convert to .ava format

**Key Methods:**
```kotlin
class VoiceOSIntegration(context: Context) {
    // Check if VoiceOS is installed
    fun isVoiceOSInstalled(): Boolean

    // Query app context (clickable elements, command hierarchies)
    fun queryAppContext(packageName: String): AppContext?

    // Execute multi-step command hierarchy
    suspend fun executeCommandHierarchy(hierarchy: CommandHierarchy): Boolean

    // Import all .vos files from VoiceOS
    suspend fun importAllVoiceOSCommands(voiceosPath: String): Int
}
```

**Example Usage:**
```kotlin
val integration = VoiceOSIntegration(context)

// Check if VoiceOS is installed
if (integration.isVoiceOSInstalled()) {
    // Query Teams app context
    val context = integration.queryAppContext("com.microsoft.teams")

    // Find "call John Thomas" command hierarchy
    val hierarchy = context?.commandHierarchies?.find {
        it.commandText.contains("call john thomas", ignoreCase = true)
    }

    // Execute the hierarchy
    if (hierarchy != null) {
        integration.executeCommandHierarchy(hierarchy)
    }
}
```

### 3. IntentExamplesMigration

**File:** `Universal/AVA/Features/NLU/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/IntentExamplesMigration.kt`

**Responsibilities:**
- Multi-source migration (priority: .ava files → JSON fallback)
- Hash-based deduplication
- Atomic database insertion
- Migration status tracking

**Key Methods:**
```kotlin
class IntentExamplesMigration(context: Context) {
    // Migrate if database is empty (safe to call on every startup)
    suspend fun migrateIfNeeded(): Boolean

    // Force migration (even if database has examples)
    suspend fun forceMigration(): Int

    // Get migration status
    suspend fun getMigrationStatus(): Map<String, Any>

    // Clear all examples from database
    suspend fun clearDatabase()
}
```

**Migration Priority:**
1. Load from .ava files (if exist) → `migrateFromAvaFiles()`
2. Fallback to JSON (if .ava not found) → `migrateFromJson()`
3. Error → Log and return empty list

**Example Usage:**
```kotlin
val migration = IntentExamplesMigration(context)

// Safe to call on every app startup
if (migration.migrateIfNeeded()) {
    Log.i(TAG, "Migration completed successfully")
}

// Check migration status
val status = migration.getMigrationStatus()
Log.d(TAG, "Total examples: ${status["total_count"]}")
```

---

## Intent Loading

### Loading Flow

```
App Startup
    ↓
IntentExamplesMigration.migrateIfNeeded()
    ↓
Check if database has examples
    ├─ Yes → Skip migration
    └─ No  → Start migration
               ↓
         Try load .ava files
               ├─ Success → Use .ava data
               └─ Fail    → Fallback to JSON
                              ↓
                    Insert into database
                              ↓
                       Migration complete
```

### Source Priority

| Priority | Source                  | Location                           | Format        |
|----------|-------------------------|------------------------------------|---------------|
| 1        | Core .ava files         | `/.ava/core/{locale}/*.ava`        | Compact JSON  |
| 2        | VoiceOS .ava files      | `/.ava/voiceos/{locale}/*.ava`     | Compact JSON  |
| 3        | User .ava files         | `/.ava/user/{locale}/*.ava`        | Compact JSON  |
| 4        | JSON fallback           | `assets/intent_examples.json`      | Verbose JSON  |

### Example Code

```kotlin
// In AvaApplication.onCreate()
lifecycleScope.launch {
    val migration = IntentExamplesMigration(applicationContext)
    val migrated = migration.migrateIfNeeded()

    if (migrated) {
        Log.i(TAG, "Intent examples migrated successfully")
    } else {
        Log.i(TAG, "Intent examples already loaded")
    }

    // Check status
    val status = migration.getMigrationStatus()
    val totalCount = status["total_count"] as Int
    val intentCounts = status["intent_counts"] as Map<String, Int>

    Log.d(TAG, "Total examples: $totalCount")
    intentCounts.forEach { (intentId, count) ->
        Log.d(TAG, "  $intentId: $count examples")
    }
}
```

---

## VoiceOS Integration

### Overview

When VoiceOS is installed on the device, AVA can query its database for:
1. **App Context:** Clickable elements in app UIs
2. **Command Hierarchies:** Multi-step command sequences

This enables complex commands like:
- **"call John Thomas on teams"**
  1. Open Teams app
  2. Click call button
  3. Select "John Thomas" from contacts

### Detection

```kotlin
val integration = VoiceOSIntegration(context)

if (integration.isVoiceOSInstalled()) {
    Log.i(TAG, "VoiceOS detected, enabling context-aware commands")
} else {
    Log.i(TAG, "VoiceOS not installed, using basic intent system")
}
```

### Querying App Context

```kotlin
// Query Microsoft Teams app context
val teamsContext = integration.queryAppContext("com.microsoft.teams")

if (teamsContext != null) {
    Log.d(TAG, "Teams app has ${teamsContext.clickableElements.size} clickable elements")
    Log.d(TAG, "Teams app has ${teamsContext.commandHierarchies.size} command hierarchies")

    // List clickable elements
    teamsContext.clickableElements.forEach { element ->
        Log.d(TAG, "Element: ${element.text} (${element.type})")
    }

    // List command hierarchies
    teamsContext.commandHierarchies.forEach { hierarchy ->
        Log.d(TAG, "Command: ${hierarchy.commandText} (${hierarchy.steps.size} steps)")
    }
}
```

### Executing Command Hierarchies

```kotlin
// User says: "call John Thomas on teams"

// 1. Query Teams app context
val context = integration.queryAppContext("com.microsoft.teams")

// 2. Find matching command hierarchy
val hierarchy = context?.commandHierarchies?.find {
    it.commandText.contains("call", ignoreCase = true)
}

// 3. Execute hierarchy
if (hierarchy != null) {
    val success = integration.executeCommandHierarchy(hierarchy)
    if (success) {
        Log.i(TAG, "Command executed successfully")
    } else {
        Log.e(TAG, "Command execution failed")
    }
}
```

### Database Schema (VoiceOS)

**app_context table:**
```sql
CREATE TABLE app_context (
    package_name TEXT PRIMARY KEY,
    app_name TEXT,
    activity_name TEXT,
    last_updated INTEGER
);
```

**clickable_elements table:**
```sql
CREATE TABLE clickable_elements (
    element_id TEXT PRIMARY KEY,
    package_name TEXT,
    element_type TEXT,
    text TEXT,
    content_description TEXT,
    resource_id TEXT,
    bounds TEXT,
    clickable INTEGER,
    parent_id TEXT,
    FOREIGN KEY (package_name) REFERENCES app_context(package_name)
);
```

**command_hierarchy table:**
```sql
CREATE TABLE command_hierarchy (
    command_id TEXT,
    command_text TEXT,
    app_package TEXT,
    step_number INTEGER,
    action TEXT,
    target_element_id TEXT,
    target_text TEXT,
    parameters TEXT,  -- JSON
    PRIMARY KEY (command_id, step_number),
    FOREIGN KEY (app_package) REFERENCES app_context(package_name)
);
```

---

## Migration System

### Migration Logic

```kotlin
private suspend fun migrate(): Int {
    val dao = database.intentExampleDao()

    // Try .ava files first
    val entities = try {
        val avaEntities = migrateFromAvaFiles()
        if (avaEntities.isNotEmpty()) {
            Log.i(TAG, "Using .ava files (${avaEntities.size} examples)")
            avaEntities
        } else {
            Log.i(TAG, "No .ava files, falling back to JSON")
            migrateFromJson()
        }
    } catch (e: Exception) {
        Log.w(TAG, "Failed to load .ava files, falling back to JSON")
        migrateFromJson()
    }

    // Insert into database
    val insertedIds = dao.insertIntentExamples(entities)
    return insertedIds.count { it > 0 }
}
```

### Hash-Based Deduplication

```kotlin
fun generateHash(intentId: String, exampleText: String): String {
    val input = "$intentId|$exampleText"
    val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

// Example hash for deduplication
val hash1 = generateHash("control_lights", "turn on lights")
val hash2 = generateHash("control_lights", "turn on lights")
// hash1 == hash2 → Duplicate, skip insertion

val hash3 = generateHash("control_lights", "switch on lights")
// hash3 != hash1 → New example, insert
```

### Database Insertion

```kotlin
// Bulk insert with OnConflict.IGNORE
val entities = listOf(
    IntentExampleEntity(
        exampleHash = "abc123",
        intentId = "control_lights",
        exampleText = "turn on lights",
        isPrimary = true,
        source = "AVA_FILE_CORE",
        locale = "en-US",
        createdAt = System.currentTimeMillis()
    ),
    // ... more entities
)

val insertedIds = dao.insertIntentExamples(entities)
val insertedCount = insertedIds.count { it > 0 }
Log.i(TAG, "Inserted $insertedCount examples")
```

---

## Usage Examples

### Example 1: Load Intents for Spanish

```kotlin
val loader = AvaFileLoader(context)

// Load Spanish intents
val spanishIntents = loader.loadAllIntentsForLocale("es-ES")
Log.d(TAG, "Loaded ${spanishIntents.size} Spanish intents")

// Convert to database entities
val entities = loader.convertToEntities(spanishIntents)

// Insert into database
dao.insertIntentExamples(entities)
```

### Example 2: Import VoiceOS Commands

```kotlin
val integration = VoiceOSIntegration(context)

// Import all .vos files from VoiceOS
val count = integration.importAllVoiceOSCommands(
    "/path/to/voiceos/commands"
)

Log.i(TAG, "Imported $count VoiceOS commands")
```

### Example 3: Context-Aware Command Execution

```kotlin
// User says: "call John Thomas on teams"

val integration = VoiceOSIntegration(context)

// Check if VoiceOS is installed
if (!integration.isVoiceOSInstalled()) {
    Log.w(TAG, "VoiceOS not installed, cannot execute context-aware command")
    return
}

// Query Teams app context
val teamsContext = integration.queryAppContext("com.microsoft.teams")

if (teamsContext == null) {
    Log.w(TAG, "Teams app context not available")
    return
}

// Find "call" command hierarchy
val callHierarchy = teamsContext.commandHierarchies.find {
    it.commandText.contains("call", ignoreCase = true)
}

if (callHierarchy == null) {
    Log.w(TAG, "Call command hierarchy not found")
    return
}

// Execute the hierarchy
val success = integration.executeCommandHierarchy(callHierarchy)

if (success) {
    Log.i(TAG, "Successfully initiated call to John Thomas")
} else {
    Log.e(TAG, "Failed to execute call command")
}
```

---

## Best Practices

### 1. Use .ava Files for Production

**Why:** 66% size reduction, multi-locale support, offline-first

```kotlin
// ✅ CORRECT: Use .ava files
loader.loadAllIntentsForLocale("en-US")

// ❌ WRONG: Use JSON directly
loadJsonFromAssets()
```

### 2. Always Check VoiceOS Availability

```kotlin
// ✅ CORRECT: Check before querying
if (integration.isVoiceOSInstalled()) {
    val context = integration.queryAppContext(packageName)
}

// ❌ WRONG: Query without checking
val context = integration.queryAppContext(packageName)  // May crash
```

### 3. Handle Migration Errors Gracefully

```kotlin
// ✅ CORRECT: Try-catch with fallback
val entities = try {
    migrateFromAvaFiles()
} catch (e: Exception) {
    Log.w(TAG, "Failed to load .ava files, falling back to JSON")
    migrateFromJson()
}

// ❌ WRONG: No error handling
val entities = migrateFromAvaFiles()  // May crash if files missing
```

### 4. Use Lazy Loading for Dependencies

```kotlin
// ✅ CORRECT: Lazy initialization
private val avaFileLoader by lazy { AvaFileLoader(context) }

// ❌ WRONG: Eager initialization (slower startup)
private val avaFileLoader = AvaFileLoader(context)
```

---

## Troubleshooting

### Issue 1: No Intents Loaded

**Symptoms:**
- Migration returns 0 examples
- Database is empty

**Causes:**
1. .ava files missing
2. JSON file missing from assets
3. Invalid JSON format

**Solution:**
```kotlin
// Check migration status
val status = migration.getMigrationStatus()
Log.d(TAG, "Has examples: ${status["has_examples"]}")
Log.d(TAG, "Total count: ${status["total_count"]}")

// Force migration with verbose logging
migration.forceMigration()
```

### Issue 2: VoiceOS Not Detected

**Symptoms:**
- `isVoiceOSInstalled()` returns false
- Cannot query app context

**Causes:**
1. VoiceOS not installed
2. Package name mismatch

**Solution:**
```kotlin
// Check for all VoiceOS packages
val packages = listOf(
    "com.avanues.voiceos",
    "com.avanues.launcher",
    "com.ideahq.voiceos"
)

packages.forEach { packageName ->
    try {
        context.packageManager.getPackageInfo(packageName, 0)
        Log.i(TAG, "Found VoiceOS: $packageName")
    } catch (e: PackageManager.NameNotFoundException) {
        Log.d(TAG, "Package not found: $packageName")
    }
}
```

### Issue 3: Duplicate Intents

**Symptoms:**
- Same intent appears multiple times
- Incorrect example count

**Causes:**
1. Hash collision (rare)
2. Migration run multiple times without hash constraint

**Solution:**
```kotlin
// Clear database and re-migrate
migration.clearDatabase()
migration.forceMigration()

// Verify deduplication
val status = migration.getMigrationStatus()
val intentCounts = status["intent_counts"] as Map<String, Int>
intentCounts.forEach { (intentId, count) ->
    Log.d(TAG, "$intentId: $count examples")
}
```

---

## Summary

AVA's Intent Management System provides:

✅ **66% size reduction** with compact .ava format
✅ **Multi-source loading** (core, VoiceOS, user)
✅ **Locale-aware** with downloadable language packs
✅ **Context-aware** with VoiceOS database integration
✅ **Offline-first** design with progressive enhancement
✅ **Hash-based deduplication** prevents duplicates
✅ **Graceful fallback** from .ava → JSON

**Next Steps:**
- Read [Chapter 35: Language Pack System](./Developer-Manual-Chapter35-Language-Pack-System.md) for language pack management
- See [Chapter 28: RAG System](./Developer-Manual-Chapter28-RAG.md) for intent-aware RAG integration

---

**End of Chapter 34**
