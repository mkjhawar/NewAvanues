# External File Injection Guide

**Date**: 2026-02-04
**Version**: 2.2.0
**Status**: Active

---

## Overview

The NewAvanues platform supports loading commands, intents, and configurations from external storage. This allows:

- **Localization**: Language-specific command packs
- **Customization**: User-defined commands and intents
- **Updates**: Hot-reload without app updates
- **OTA**: Over-the-air content delivery

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    External Storage                         │
│  (app-specific: getExternalFilesDir(null))                 │
├─────────────────────────────────────────────────────────────┤
│  /core/                 System defaults (read-only)         │
│  /voiceos/              VoiceOS-specific data               │
│  /user/                 User customizations (highest prio)  │
│  /downloaded/           Downloaded packs                    │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    File Loaders                             │
│  CommandLoader, IntentSourceCoordinator, SynonymLoader      │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    SQLDelight Database                      │
│  voice_command, intent_example, app_category_override       │
└─────────────────────────────────────────────────────────────┘
```

## Storage Structure

```
getExternalFilesDir(null)/
├── core/                           # Core system files
│   ├── intents/
│   │   └── en-US/
│   │       ├── navigation.aai      # Navigation intents
│   │       ├── system.aai          # System intents
│   │       └── media.aai           # Media intents
│   ├── commands/
│   │   └── en-US/
│   │       ├── navigation.vos
│   │       └── system.vos
│   └── categories/
│       └── known-apps.acd          # App category database
│
├── voiceos/                        # VoiceOS learned data
│   ├── learned/
│   │   └── <package_name>.vos      # Learned app data
│   └── screens/
│       └── <package_name>/
│           └── <screen_hash>.json
│
├── user/                           # User customizations
│   ├── intents/
│   │   └── custom.aai              # Custom intents
│   ├── commands/
│   │   └── custom.vos              # Custom commands
│   └── synonyms/
│       └── custom.syn              # Custom synonyms
│
└── downloaded/                     # Downloaded content
    ├── languages/
    │   ├── es-ES/                  # Spanish pack
    │   ├── fr-FR/                  # French pack
    │   └── de-DE/                  # German pack
    └── models/
        └── embeddings/
            └── mobilebert-384.bin
```

## File Formats

### Commands (.vos) - AVU 2.2

```
# Avanues Universal Format v2.2
# Type: VOS
---
schema: avu-2.2
version: 2.2.0
locale: en-US
project: voiceos
metadata:
  file: custom_commands.vos
  category: custom
  count: 3
---
CMD:custom_action_1:do my thing:CUSTOM_ACTION:null:0.95
CMD:custom_action_2:another action:CUSTOM_ACTION_2:null:0.90
```

### Intents (.aai) - AVU 2.2

```
# Avanues Universal Format v2.2
# Type: AVA
---
schema: avu-2.2
version: 2.2.0
locale: en-US
metadata:
  file: custom_intents.aai
  category: nlu_intents
---
INT:my_intent:do my thing:custom:10:CUSTOM_ACTION
PAT:my_intent:do my thing
PAT:my_intent:perform my action
SYN:my_intent:my thing
```

### App Categories (.acd) - AVU 2.2

```
# Avanues Universal Format v2.2
# Type: AppCategoryDatabase
---
schema: avu-2.2
version: 1.0.0
project: voiceos
metadata:
  file: custom_categories.acd
  category: app_category_database
---
ACD:1.0.0:1706300000000:user
APC:com.custom.app:PRODUCTIVITY:custom:0.99
APG:CUSTOM:custom|mycategory
```

### Synonyms (.syn)

Binary format or text:
```
# Synonym Pack v1.0
---
word:synonym1,synonym2,synonym3
click:tap,press,hit
scroll:swipe,slide,move
```

## Injection Methods

### Method 1: Direct File Copy

```kotlin
// Copy file to external storage
val externalDir = context.getExternalFilesDir(null)
val targetFile = File(externalDir, "user/intents/custom.aai")
targetFile.parentFile?.mkdirs()
targetFile.writeText(avuContent)

// Trigger reload
val coordinator = IntentSourceCoordinator(context)
coordinator.reloadFromPath("$externalDir/user")
```

### Method 2: ContentProvider

```kotlin
// Insert via ContentProvider
val uri = Uri.parse("content://com.augmentalis.voiceos.provider/intents")
val values = ContentValues().apply {
    put("locale", "en-US")
    put("content", avuContent)
    put("source", "external")
}
context.contentResolver.insert(uri, values)
```

### Method 3: Broadcast Intent

```kotlin
// Send broadcast to trigger reload
val intent = Intent("com.augmentalis.voiceos.RELOAD_DATA")
intent.putExtra("type", "intents")
intent.putExtra("path", "$externalDir/user/intents")
context.sendBroadcast(intent)
```

### Method 4: gRPC Service

```kotlin
// Use VoiceOSService.RegisterDynamicCommand
val request = DynamicCommandRequest.newBuilder()
    .setRequestId(UUID.randomUUID().toString())
    .setCommandText("my custom command")
    .setActionJson("""{"action": "CUSTOM", "params": {}}""")
    .build()

voiceOSService.registerDynamicCommand(request)
```

## Loader APIs

### CommandLoader

```kotlin
class CommandLoader(context: Context) {
    // Load commands for locale with English fallback
    suspend fun loadCommands(locale: String): Int

    // Load from specific path
    suspend fun loadFromPath(path: String): Int

    // Check if locale is loaded
    fun isLocaleLoaded(locale: String): Boolean
}
```

### IntentSourceCoordinator

```kotlin
class IntentSourceCoordinator(context: Context) {
    // Migrate if first run or reload needed
    suspend fun migrateIfNeeded(): Boolean

    // Reload from specific path
    suspend fun reloadFromPath(path: String): Int

    // Clear and reload all
    suspend fun fullReload(): Int
}
```

### AppCategoryLoader

```kotlin
interface IAppCategoryRepository {
    suspend fun upsertCategory(packageName: String, category: String, ...)
    suspend fun upsertPatternGroup(category: String, patterns: List<String>)
    suspend fun getVersion(): Long
    suspend fun setVersion(version: Long)
}

class AppCategoryLoader(
    assetReader: IAssetReader,
    repository: IAppCategoryRepository
) {
    suspend fun loadIfNeeded(): Boolean
    suspend fun loadFromFile(filename: String): Int
}
```

## Priority Resolution

When multiple sources define the same command/intent:

| Priority | Source | Path | Description |
|----------|--------|------|-------------|
| 1 (highest) | User | `/user/` | User customizations |
| 2 | Downloaded | `/downloaded/` | Downloaded packs |
| 3 | VoiceOS | `/voiceos/` | Learned data |
| 4 | Core | `/core/` | System defaults |
| 5 (lowest) | Assets | `assets/` | Bundled fallback |

```kotlin
// Resolution logic
fun resolve(id: String): Command? {
    return userCommands[id]
        ?: downloadedCommands[id]
        ?: voiceosCommands[id]
        ?: coreCommands[id]
        ?: assetCommands[id]
}
```

## Validation

### Schema Validation

```kotlin
fun validateAvuFile(content: String): ValidationResult {
    // Check header
    if (!content.contains("# Avanues Universal Format")) {
        return ValidationResult.error("Missing AVU header")
    }

    // Check schema version
    val schemaMatch = "schema: avu-([0-9.]+)".toRegex().find(content)
    val version = schemaMatch?.groupValues?.get(1)
    if (version == null || version < "2.0") {
        return ValidationResult.error("Invalid or outdated schema")
    }

    // Validate data lines
    // ...

    return ValidationResult.success()
}
```

### Security Checks

1. **Path traversal**: Reject paths with `..`
2. **File size**: Limit to reasonable size (e.g., 10MB)
3. **Content validation**: Validate AVU format
4. **Permission check**: Verify app has storage permission

## Hot Reload

### File Watcher (Optional)

```kotlin
class FileWatcher(context: Context) {
    private val observer = object : FileObserver(watchPath) {
        override fun onEvent(event: Int, path: String?) {
            when (event) {
                CREATE, MODIFY -> {
                    if (path?.endsWith(".vos") == true ||
                        path?.endsWith(".aai") == true) {
                        triggerReload(path)
                    }
                }
            }
        }
    }

    fun startWatching() = observer.startWatching()
    fun stopWatching() = observer.stopWatching()
}
```

### Manual Reload

```kotlin
// In settings or debug menu
fun reloadAllData() {
    scope.launch {
        commandLoader.fullReload()
        intentCoordinator.fullReload()
        categoryLoader.loadIfNeeded()
    }
}
```

## Localization Workflow

### Adding a New Language

1. **Create language pack**:
   ```
   es-ES/
   ├── navigation.aai
   ├── system.aai
   └── commands.vos
   ```

2. **Deploy to external storage**:
   ```kotlin
   // Download and extract to /downloaded/languages/es-ES/
   ```

3. **Switch language**:
   ```kotlin
   Localizer.setLanguage("es-ES")
   commandLoader.loadCommands("es-ES")
   intentCoordinator.reloadForLocale("es-ES")
   ```

### Language Fallback

```kotlin
// Always loads English first, then target locale
suspend fun loadCommands(locale: String) {
    // 1. Load English as fallback (is_fallback = true)
    loadFromAssets("en-US", isFallback = true)

    // 2. Load target locale (overrides English)
    if (locale != "en-US") {
        loadFromExternal(locale, isFallback = false)
        loadFromAssets(locale, isFallback = false)
    }
}
```

---

## Summary

The external file injection system provides:

1. **Flexibility**: Load data from multiple sources
2. **Priority**: User > Downloaded > VoiceOS > Core > Assets
3. **Hot reload**: Update without app restart
4. **Localization**: Language-specific content
5. **Validation**: Schema and content validation
6. **Security**: Path traversal and permission checks

---

*Last updated: 2026-02-04*
