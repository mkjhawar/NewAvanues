# VoiceOS 4 Developer Guide - Complete

**Created:** 2025-10-13 21:44 PDT
**Version:** 3.0.0
**Status:** Production
**Note:** VoiceOS 4 (shortform: vos4) is a voice-controlled Android accessibility system

---

## Table of Contents

1. [Getting Started](#getting-started)
2. [Development Environment Setup](#development-environment-setup)
3. [Project Structure](#project-structure)
4. [Adding New Commands](#adding-new-commands)
5. [Working with Databases](#working-with-databases)
6. [Creating New Modules](#creating-new-modules)
7. [Speech Engine Integration](#speech-engine-integration)
8. [Accessibility Service Development](#accessibility-service-development)
9. [Testing Strategies](#testing-strategies)
10. [Debugging Techniques](#debugging-techniques)
11. [Code Standards](#code-standards)
12. [Common Patterns](#common-patterns)
13. [Performance Optimization](#performance-optimization)
14. [Troubleshooting](#troubleshooting)

---

## Getting Started

### Prerequisites

**Required:**
- **Android Studio**: Arctic Fox or later
- **JDK**: 17 (Java 17)
- **Gradle**: 8.10.2 or later
- **Kotlin**: 1.9.25
- **Android SDK**: API 29+ (Android 10+)

**Recommended:**
- **Git**: For version control
- **Android Device**: Physical device with Android 10+ for testing
- **ADB**: Android Debug Bridge

### Clone and Build

```bash
# Clone repository
git clone https://gitlab.com/AugmentalisES/voiceos.git
cd voiceos

# Checkout vos4 branch
git checkout vos4-legacyintegration

# Build project
./gradlew assembleDebug

# Install on device
./gradlew installDebug
```

### Build Configuration

**Minimum Requirements:**
```kotlin
// build.gradle.kts (module level)
android {
    compileSdk = 34

    defaultConfig {
        minSdk = 29  // Android 10+
        targetSdk = 34

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
```

---

## Development Environment Setup

### Android Studio Configuration

**1. Import Project**
```
File → Open → Select vos4 directory
Wait for Gradle sync to complete
```

**2. SDK Configuration**
```
File → Project Structure → SDK Location
- Android SDK Location: /path/to/Android/sdk
- JDK Location: /path/to/jdk-17
```

**3. Enable Build Features**
```kotlin
// build.gradle.kts
buildFeatures {
    compose = true  // For Compose UI modules
}

composeOptions {
    kotlinCompilerExtensionVersion = "1.5.15"
}
```

### Code Style Settings

**Import VOS4 Code Style:**

1. Go to `Settings → Editor → Code Style → Kotlin`
2. Import scheme from `/docs/voiceos-master/standards/vos4-code-style.xml`

**Key Standards:**
- **Indent**: 4 spaces
- **Max line length**: 120 characters
- **Package naming**: `com.augmentalis.{module}`
- **No interfaces**: Direct implementation only

### Plugins to Install

**Recommended Plugins:**
- **Kotlin**: Already included
- **Android**: Already included
- **Database Inspector**: For Room database debugging
- **Git**: Version control integration

---

## Project Structure

### Directory Layout

```
vos4/
├── app/                              # Main app module (launcher)
│   └── src/main/
│       ├── java/com/augmentalis/voiceos/
│       ├── res/
│       └── AndroidManifest.xml
│
├── modules/
│   ├── apps/                         # Application modules
│   │   ├── LearnApp/                # Web learning interface
│   │   │   ├── src/main/java/
│   │   │   │   └── com/augmentalis/learnapp/
│   │   │   ├── build.gradle.kts
│   │   │   └── AndroidManifest.xml
│   │   │
│   │   ├── VoiceAccessibility/      # Main accessibility service
│   │   │   ├── src/main/java/
│   │   │   │   └── com/augmentalis/voiceaccessibility/
│   │   │   │       ├── VoiceOSService.kt
│   │   │   │       ├── VoiceCommandProcessor.kt
│   │   │   │       ├── ActionCoordinator.kt
│   │   │   │       ├── WebCommandCoordinator.kt
│   │   │   │       └── database/
│   │   │   ├── build.gradle.kts
│   │   │   └── AndroidManifest.xml
│   │   │
│   │   ├── VoiceCursor/             # Cursor control
│   │   ├── VoiceRecognition/        # Recognition UI
│   │   └── VoiceUI/                 # Voice feedback UI
│   │
│   ├── libraries/                    # Library modules
│   │   ├── DeviceManager/           # Device abstraction
│   │   ├── SpeechRecognition/       # Speech engines
│   │   │   ├── src/main/java/
│   │   │   │   └── com/augmentalis/speechrecognition/
│   │   │   │       ├── SpeechEngineManager.kt
│   │   │   │       ├── engines/
│   │   │   │       │   ├── GoogleSpeechEngine.kt
│   │   │   │       │   ├── VoskSpeechEngine.kt
│   │   │   │       │   └── VivokaSpeechEngine.kt
│   │   │   └── build.gradle.kts
│   │   │
│   │   ├── UUIDCreator/             # Hash generation
│   │   ├── VoiceKeyboard/           # Virtual keyboard
│   │   ├── VoiceOsLogger/           # Logging system
│   │   └── VoiceUIElements/         # UI components
│   │
│   └── managers/                     # Manager modules
│       ├── CommandManager/           # Command processing
│       │   ├── src/main/java/
│       │   │   └── com/augmentalis/commandmanager/
│       │   │       ├── CommandManager.kt
│       │   │       ├── models/
│       │   │       │   ├── Command.kt
│       │   │       │   ├── CommandContext.kt
│       │   │       │   ├── CommandAction.kt
│       │   │       │   └── CommandPriority.kt
│       │   │       └── database/
│       │   │           ├── CommandDatabase.kt
│       │   │           ├── VOSCommand.kt
│       │   │           ├── VOSCommandDao.kt
│       │   │           └── VOSCommandSynonym.kt
│       │   └── build.gradle.kts
│       │
│       ├── HUDManager/              # Heads-up display
│       ├── LicenseManager/          # Licensing
│       ├── LocalizationManager/     # i18n
│       └── VoiceDataManager/        # Data persistence
│
├── docs/                             # Documentation
│   ├── modules/                      # Module-specific docs
│   │   ├── command-manager/
│   │   ├── voice-accessibility/
│   │   └── speech-recognition/
│   ├── voiceos-master/              # System-wide docs
│   │   ├── architecture/
│   │   ├── guides/
│   │   ├── standards/
│   │   └── status/
│   └── scripts/                      # Automation scripts
│
├── coding/                           # Active development
│   ├── TODO/                        # Task lists
│   ├── STATUS/                      # Status reports
│   ├── ISSUES/                      # Issue tracking
│   └── DECISIONS/                   # Architecture decisions
│
├── tests/                            # Test code
│   └── (test rewrite in progress)
│
├── build.gradle.kts                 # Root build file
├── settings.gradle.kts              # Module configuration
├── gradle.properties                # Gradle settings
└── CLAUDE.md                        # AI agent instructions
```

### Module Organization

**Convention:**
- **Code modules**: `PascalCase` (e.g., `CommandManager/`)
- **Documentation folders**: `kebab-case` (e.g., `command-manager/`)
- **Package names**: `lowercase` (e.g., `com.augmentalis.commandmanager`)

---

## Adding New Commands

### Method 1: System Commands (JSON Ingestion)

**Best for:** System-wide commands, navigation, volume control

**Step 1: Create JSON file**

```json
// commands/system-commands.json
{
  "commands": [
    {
      "phrase": "open calculator",
      "action": "OPEN_APP",
      "category": "app_launch",
      "locale": "en_US",
      "synonyms": ["launch calculator", "start calculator"],
      "priority": 8,
      "confidence": 1.0,
      "metadata": {
        "packageName": "com.android.calculator2"
      }
    },
    {
      "phrase": "increase brightness",
      "action": "BRIGHTNESS_UP",
      "category": "system",
      "locale": "en_US",
      "synonyms": ["brighten screen", "brighter"],
      "priority": 7,
      "confidence": 0.9
    }
  ]
}
```

**Step 2: Create ingestion code**

```kotlin
// In CommandManager or initialization code

suspend fun ingestCommandsFromJSON(jsonString: String) {
    val jsonObject = JSONObject(jsonString)
    val commandsArray = jsonObject.getJSONArray("commands")

    for (i in 0 until commandsArray.length()) {
        val cmdJson = commandsArray.getJSONObject(i)

        // Insert main command
        val command = VOSCommand(
            commandPhrase = cmdJson.getString("phrase"),
            action = cmdJson.getString("action"),
            category = cmdJson.getString("category"),
            locale = cmdJson.getString("locale"),
            priority = cmdJson.getInt("priority"),
            confidence = cmdJson.getDouble("confidence").toFloat(),
            isEnabled = true
        )

        val commandId = database.vosCommandDao().insertCommand(command)

        // Insert synonyms
        if (cmdJson.has("synonyms")) {
            val synonymsArray = cmdJson.getJSONArray("synonyms")
            for (j in 0 until synonymsArray.length()) {
                val synonym = VOSCommandSynonym(
                    commandId = commandId,
                    synonym = synonymsArray.getString(j),
                    locale = command.locale,
                    confidence = command.confidence * 0.9f
                )
                database.vosCommandSynonymDao().insertSynonym(synonym)
            }
        }
    }
}
```

**Step 3: Load commands on initialization**

```kotlin
// In VoiceOSService.kt or initialization

private suspend fun loadSystemCommands() {
    val jsonString = context.assets
        .open("commands/system-commands.json")
        .bufferedReader()
        .use { it.readText() }

    commandManager.ingestCommandsFromJSON(jsonString)
}
```

### Method 2: Programmatic Registration

**Best for:** Dynamic commands, context-specific commands

```kotlin
// In CommandManager or service initialization

fun registerCustomCommand() {
    val command = Command(
        id = "custom_action_${UUID.randomUUID()}",
        phrase = "do custom action",
        action = CommandAction.CUSTOM,
        category = "custom",
        priority = CommandPriority.MEDIUM,
        confidence = 0.85f,
        handler = { context ->
            // Custom action logic
            Log.d(TAG, "Executing custom action")
            performCustomAction(context)
            true // Return true on success
        }
    )

    commandManager.registerCommand(command)
}

private fun performCustomAction(context: CommandContext): Boolean {
    // Your custom logic here
    return try {
        // Do something...
        true
    } catch (e: Exception) {
        Log.e(TAG, "Custom action failed", e)
        false
    }
}
```

### Method 3: Learned Commands (Automatic)

**Best for:** UI element commands, app-specific commands

Commands are automatically generated when UI elements are scraped:

```kotlin
// Automatic generation in AccessibilityScrapingIntegration

private fun generateCommandsForElement(element: ScrapedElementEntity): List<GeneratedCommandEntity> {
    val commands = mutableListOf<GeneratedCommandEntity>()

    // Generate from text
    if (!element.text.isNullOrBlank()) {
        commands.add(GeneratedCommandEntity(
            elementHash = element.elementHash,
            commandPhrase = "click ${element.text.lowercase()}",
            action = "CLICK",
            confidence = 0.9f
        ))

        commands.add(GeneratedCommandEntity(
            elementHash = element.elementHash,
            commandPhrase = "tap ${element.text.lowercase()}",
            action = "CLICK",
            confidence = 0.85f
        ))
    }

    // Generate from content description
    if (!element.contentDescription.isNullOrBlank()) {
        commands.add(GeneratedCommandEntity(
            elementHash = element.elementHash,
            commandPhrase = "click ${element.contentDescription.lowercase()}",
            action = "CLICK",
            confidence = 0.8f
        ))
    }

    // Generate from view ID
    if (!element.viewIdResourceName.isNullOrBlank()) {
        val readable = element.viewIdResourceName
            .split("/").last()
            .replace("_", " ")
            .replace(Regex("([a-z])([A-Z])"), "$1 $2")
            .lowercase()

        commands.add(GeneratedCommandEntity(
            elementHash = element.elementHash,
            commandPhrase = "click $readable",
            action = "CLICK",
            confidence = 0.75f
        ))
    }

    return commands
}
```

### Command Action Handlers

**Define action handlers in CommandManager:**

```kotlin
class CommandManager(
    private val context: Context,
    private val commandDatabase: CommandDatabase
) {

    private val actionHandlers = mapOf<String, (CommandContext) -> Boolean>(
        "NAVIGATE_HOME" to { navigateHome() },
        "NAVIGATE_BACK" to { navigateBack() },
        "OPEN_APP" to { ctx -> openApp(ctx.metadata["packageName"] as? String) },
        "VOLUME_UP" to { volumeUp() },
        "VOLUME_DOWN" to { volumeDown() },
        "BRIGHTNESS_UP" to { brightnessUp() },
        "BRIGHTNESS_DOWN" to { brightnessDown() },
        // Add more handlers...
    )

    suspend fun executeCommand(command: Command, context: CommandContext): Boolean {
        val handler = actionHandlers[command.action] ?: return false
        return try {
            handler(context)
        } catch (e: Exception) {
            Log.e(TAG, "Command execution failed: ${command.phrase}", e)
            false
        }
    }

    private fun navigateHome(): Boolean {
        return accessibilityService.performGlobalAction(
            AccessibilityService.GLOBAL_ACTION_HOME
        )
    }

    private fun navigateBack(): Boolean {
        return accessibilityService.performGlobalAction(
            AccessibilityService.GLOBAL_ACTION_BACK
        )
    }

    private fun openApp(packageName: String?): Boolean {
        if (packageName == null) return false

        return try {
            val intent = context.packageManager
                .getLaunchIntentForPackage(packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app: $packageName", e)
            false
        }
    }
}
```

### Registering Commands with Speech Engine

**Update vocabulary when commands change:**

```kotlin
private suspend fun updateSpeechVocabulary() {
    val allPhrases = mutableSetOf<String>()

    // Collect from CommandDatabase
    val vosCommands = commandDatabase.vosCommandDao()
        .getCommandsByLocale(Locale.getDefault().toString())
    allPhrases.addAll(vosCommands.map { it.commandPhrase })

    // Collect synonyms
    vosCommands.forEach { command ->
        val synonyms = commandDatabase.vosCommandSynonymDao()
            .getSynonyms(command.id)
        allPhrases.addAll(synonyms.map { it.synonym })
    }

    // Collect from AppScrapingDatabase
    val appCommands = appScrapingDatabase.generatedCommandDao()
        .getAllCommands()
    allPhrases.addAll(appCommands.map { it.commandPhrase })

    // Update speech engine
    speechEngineManager.updateVocabulary(allPhrases.toList())

    Log.d(TAG, "Updated speech vocabulary: ${allPhrases.size} phrases")
}
```

---

## Working with Databases

### Database Overview

VoiceOS 4 uses **three Room databases**:

1. **CommandDatabase** - System commands
2. **AppScrapingDatabase** - Learned app commands
3. **WebScrapingDatabase** - Learned web commands

### Setting Up a New Database

**Step 1: Define entities**

```kotlin
// models/MyEntity.kt
@Entity(
    tableName = "my_entities",
    indices = [Index(value = ["uniqueField"], unique = true)]
)
data class MyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,
    val value: Int,
    val uniqueField: String,
    val createdAt: Long = System.currentTimeMillis()
)
```

**Step 2: Create DAO**

```kotlin
// dao/MyEntityDao.kt
@Dao
interface MyEntityDao {

    @Query("SELECT * FROM my_entities")
    suspend fun getAll(): List<MyEntity>

    @Query("SELECT * FROM my_entities WHERE id = :id")
    suspend fun getById(id: Long): MyEntity?

    @Query("SELECT * FROM my_entities WHERE uniqueField = :field")
    suspend fun getByUniqueField(field: String): MyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MyEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<MyEntity>): List<Long>

    @Update
    suspend fun update(entity: MyEntity)

    @Delete
    suspend fun delete(entity: MyEntity)

    @Query("DELETE FROM my_entities")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM my_entities")
    suspend fun count(): Int
}
```

**Step 3: Create database class**

```kotlin
// database/MyDatabase.kt
@Database(
    entities = [MyEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MyDatabase : RoomDatabase() {

    abstract fun myEntityDao(): MyEntityDao

    companion object {
        @Volatile
        private var INSTANCE: MyDatabase? = null

        fun getDatabase(context: Context): MyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MyDatabase::class.java,
                    "my_database"
                )
                .fallbackToDestructiveMigration()
                .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
```

**Step 4: Use database**

```kotlin
// In your service or manager

class MyManager(private val context: Context) {

    private val database = MyDatabase.getDatabase(context)
    private val dao = database.myEntityDao()

    suspend fun saveEntity(name: String, value: Int, uniqueField: String) {
        val entity = MyEntity(
            name = name,
            value = value,
            uniqueField = uniqueField
        )
        dao.insert(entity)
    }

    suspend fun getAllEntities(): List<MyEntity> {
        return dao.getAll()
    }

    suspend fun getEntityByField(field: String): MyEntity? {
        return dao.getByUniqueField(field)
    }
}
```

### Database Migrations

**When schema changes:**

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add new column
        database.execSQL(
            "ALTER TABLE my_entities ADD COLUMN newField TEXT DEFAULT '' NOT NULL"
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create new table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS new_table (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                field1 TEXT NOT NULL,
                field2 INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

// Apply migrations
Room.databaseBuilder(context, MyDatabase::class.java, "my_database")
    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
    .build()
```

### Database Best Practices

**1. Always use suspend functions**
```kotlin
// ✅ CORRECT: Async with coroutines
suspend fun getCommands(): List<Command> {
    return withContext(Dispatchers.IO) {
        dao.getAllCommands()
    }
}

// ❌ WRONG: Blocking the main thread
fun getCommands(): List<Command> {
    return dao.getAllCommands() // Crashes on main thread!
}
```

**2. Use transactions for multiple operations**
```kotlin
suspend fun saveMultipleEntities(entities: List<MyEntity>) {
    database.withTransaction {
        entities.forEach { entity ->
            dao.insert(entity)
        }
    }
}
```

**3. Index frequently queried fields**
```kotlin
@Entity(
    tableName = "commands",
    indices = [
        Index(value = ["commandPhrase"]),  // For phrase lookups
        Index(value = ["locale"]),         // For locale filtering
        Index(value = ["elementHash"], unique = true)  // Unique constraint
    ]
)
data class Command(...)
```

**4. Recycle queries after use**
```kotlin
// DAOs handle this automatically, but for raw queries:
val cursor = database.query("SELECT * FROM my_table")
try {
    // Use cursor...
} finally {
    cursor.close()
}
```

---

## Creating New Modules

### Module Types

1. **App Module**: Standalone application with UI
2. **Library Module**: Reusable code library
3. **Manager Module**: System service or manager

### Creating a Library Module

**Step 1: Create module directory**

```bash
mkdir -p modules/libraries/MyLibrary/src/main/java/com/augmentalis/mylibrary
```

**Step 2: Create build.gradle.kts**

```kotlin
// modules/libraries/MyLibrary/build.gradle.kts

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.augmentalis.mylibrary"
    compileSdk = 34

    defaultConfig {
        minSdk = 29
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Android
    implementation("androidx.core:core-ktx:1.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Internal dependencies (if needed)
    // implementation(project(":modules:libraries:VoiceOsLogger"))

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.25")
}
```

**Step 3: Create AndroidManifest.xml**

```xml
<!-- modules/libraries/MyLibrary/src/main/AndroidManifest.xml -->
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Library manifest - typically minimal -->
</manifest>
```

**Step 4: Add to settings.gradle.kts**

```kotlin
// settings.gradle.kts (root)

include(":modules:libraries:MyLibrary")
```

**Step 5: Create module code**

```kotlin
// modules/libraries/MyLibrary/src/main/java/com/augmentalis/mylibrary/MyLibrary.kt

package com.augmentalis.mylibrary

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MyLibrary - Brief description
 *
 * Provides functionality for...
 */
class MyLibrary(private val context: Context) {

    /**
     * Initialize the library
     */
    fun initialize() {
        // Initialization code
    }

    /**
     * Main library method
     */
    suspend fun doSomething(): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Implementation
            Result.success("Success!")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "MyLibrary"
    }
}
```

**Step 6: Use the module**

```kotlin
// In dependent module's build.gradle.kts
dependencies {
    implementation(project(":modules:libraries:MyLibrary"))
}

// In code
import com.augmentalis.mylibrary.MyLibrary

class MyService {
    private val myLibrary = MyLibrary(context)

    fun useLibrary() {
        myLibrary.initialize()

        lifecycleScope.launch {
            val result = myLibrary.doSomething()
            result.onSuccess { value ->
                Log.d(TAG, "Success: $value")
            }.onFailure { error ->
                Log.e(TAG, "Error", error)
            }
        }
    }
}
```

---

## Speech Engine Integration

### Adding a New Speech Engine

**Step 1: Create engine class**

```kotlin
// modules/libraries/SpeechRecognition/src/main/java/com/augmentalis/speechrecognition/engines/MySpeechEngine.kt

package com.augmentalis.speechrecognition.engines

import android.content.Context
import com.augmentalis.speechrecognition.SpeechEngine
import com.augmentalis.speechrecognition.RecognitionListener

class MySpeechEngine(
    private val context: Context,
    private val listener: RecognitionListener
) : SpeechEngine {

    private var isListening = false

    override fun initialize() {
        // Initialize your engine
        // Load models, setup connections, etc.
    }

    override fun startListening() {
        if (isListening) return

        isListening = true

        // Start audio capture
        // Start recognition processing
        // Call listener.onRecognitionResult() when recognized
    }

    override fun stopListening() {
        if (!isListening) return

        isListening = false

        // Stop audio capture
        // Stop recognition processing
    }

    override fun shutdown() {
        stopListening()

        // Release resources
        // Cleanup engine
    }

    override fun setLanguage(language: String) {
        // Change recognition language
    }

    override fun registerVocabulary(words: List<String>) {
        // Optional: Update recognition vocabulary
        // Improves accuracy for specific words
    }

    private fun processAudio(audioData: ByteArray) {
        // Process audio and recognize speech

        // On success:
        listener.onRecognitionResult(
            text = "recognized text",
            confidence = 0.95f
        )

        // On error:
        listener.onRecognitionError(
            errorCode = 1,
            message = "Recognition failed"
        )
    }

    companion object {
        private const val TAG = "MySpeechEngine"
    }
}
```

**Step 2: Register engine in SpeechEngineManager**

```kotlin
// In SpeechEngineManager.kt

enum class EngineType {
    GOOGLE,
    VOSK,
    VIVOKA,
    MY_ENGINE  // Add your engine
}

class SpeechEngineManager(
    private val context: Context,
    private val listener: RecognitionListener
) {
    private var currentEngine: SpeechEngine? = null
    private var currentType: EngineType = EngineType.GOOGLE

    fun setEngine(type: EngineType) {
        currentEngine?.shutdown()

        currentEngine = when (type) {
            EngineType.GOOGLE -> GoogleSpeechEngine(context, listener)
            EngineType.VOSK -> VoskSpeechEngine(context, listener)
            EngineType.VIVOKA -> VivokaSpeechEngine(context, listener)
            EngineType.MY_ENGINE -> MySpeechEngine(context, listener)  // Add your engine
        }

        currentEngine?.initialize()
        currentType = type
    }
}
```

**Step 3: Add engine selection UI** (optional)

```kotlin
// In settings or preferences

fun onEngineSelectorClick() {
    val engines = listOf("Google", "Vosk", "Vivoka", "My Engine")

    AlertDialog.Builder(context)
        .setTitle("Select Speech Engine")
        .setItems(engines.toTypedArray()) { _, which ->
            val engineType = when (which) {
                0 -> EngineType.GOOGLE
                1 -> EngineType.VOSK
                2 -> EngineType.VIVOKA
                3 -> EngineType.MY_ENGINE
                else -> EngineType.GOOGLE
            }

            speechEngineManager.setEngine(engineType)
        }
        .show()
}
```

---

## Accessibility Service Development

### Handling Accessibility Events

**Event types to listen for:**

```kotlin
override fun onAccessibilityEvent(event: AccessibilityEvent) {
    when (event.eventType) {
        AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
            // New window or activity opened
            val packageName = event.packageName?.toString()
            val className = event.className?.toString()
            onWindowChanged(packageName, className)
        }

        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
            // Window content updated
            if (shouldScrapeEvent(event)) {
                scrapeCurrentWindow()
            }
        }

        AccessibilityEvent.TYPE_VIEW_CLICKED -> {
            // User clicked something
            onViewClicked(event)
        }

        AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
            // View gained focus
            onViewFocused(event)
        }

        AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
            // Text input changed
            onTextChanged(event)
        }
    }
}
```

### Traversing Accessibility Tree

**Recursive traversal:**

```kotlin
private fun traverseAccessibilityTree(
    node: AccessibilityNodeInfo?,
    depth: Int = 0,
    maxDepth: Int = 50
): List<AccessibilityNodeInfo> {
    if (node == null || depth > maxDepth) return emptyList()

    val results = mutableListOf<AccessibilityNodeInfo>()

    // Process current node
    if (isInterestingNode(node)) {
        results.add(node)
    }

    // Traverse children
    for (i in 0 until node.childCount) {
        val child = node.getChild(i)
        if (child != null) {
            results.addAll(traverseAccessibilityTree(child, depth + 1, maxDepth))
        }
    }

    return results
}

private fun isInterestingNode(node: AccessibilityNodeInfo): Boolean {
    return node.isClickable ||
           node.isLongClickable ||
           node.isFocusable ||
           node.isCheckable ||
           !node.text.isNullOrBlank() ||
           !node.contentDescription.isNullOrBlank()
}
```

### Finding Elements

**By text:**
```kotlin
fun findNodeByText(text: String): AccessibilityNodeInfo? {
    val rootNode = rootInActiveWindow ?: return null

    val nodes = rootNode.findAccessibilityNodeInfosByText(text)
    return if (nodes.isNotEmpty()) {
        nodes[0] // Return first match
    } else {
        null
    }
}
```

**By view ID:**
```kotlin
fun findNodeByViewId(packageName: String, viewId: String): AccessibilityNodeInfo? {
    val rootNode = rootInActiveWindow ?: return null

    val fullId = "$packageName:id/$viewId"
    val nodes = rootNode.findAccessibilityNodeInfosByViewId(fullId)

    return if (nodes.isNotEmpty()) {
        nodes[0]
    } else {
        null
    }
}
```

**By hierarchy path:**
```kotlin
fun findNodeByPath(path: String): AccessibilityNodeInfo? {
    val rootNode = rootInActiveWindow ?: return null

    val indices = path.trim('/').split('/').map { it.toInt() }
    var current: AccessibilityNodeInfo? = rootNode

    for (index in indices) {
        if (current == null) break

        current = if (index < current.childCount) {
            current.getChild(index)
        } else {
            null
        }
    }

    return current
}
```

### Performing Actions

**Standard actions:**
```kotlin
fun clickNode(node: AccessibilityNodeInfo): Boolean {
    return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
}

fun longClickNode(node: AccessibilityNodeInfo): Boolean {
    return node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
}

fun focusNode(node: AccessibilityNodeInfo): Boolean {
    return node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
}

fun scrollNode(direction: Int): Boolean {
    return node.performAction(
        if (direction > 0) {
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        } else {
            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        }
    )
}
```

**Text input:**
```kotlin
fun setNodeText(node: AccessibilityNodeInfo, text: String): Boolean {
    val arguments = Bundle()
    arguments.putCharSequence(
        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
        text
    )

    return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
}
```

### Global Actions

```kotlin
fun performGlobalHome(): Boolean {
    return performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
}

fun performGlobalBack(): Boolean {
    return performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
}

fun performGlobalRecents(): Boolean {
    return performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
}

fun performGlobalNotifications(): Boolean {
    return performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
}

fun performGlobalQuickSettings(): Boolean {
    return performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
}
```

### Memory Management

**ALWAYS recycle AccessibilityNodeInfo objects:**

```kotlin
// ✅ CORRECT: Recycle after use
fun processNode() {
    val rootNode = rootInActiveWindow
    try {
        // Use node...
    } finally {
        rootNode?.recycle()
    }
}

// ✅ CORRECT: Recycle list of nodes
fun processNodes() {
    val nodes = rootInActiveWindow?.findAccessibilityNodeInfosByText("button")
    try {
        nodes?.forEach { node ->
            // Use node...
        }
    } finally {
        nodes?.forEach { it.recycle() }
    }
}

// ❌ WRONG: Not recycling causes memory leaks!
fun leakMemory() {
    val node = rootInActiveWindow
    // Use node but never recycle - MEMORY LEAK!
}
```

---

## Testing Strategies

### Unit Testing

**Test command matching:**

```kotlin
// CommandManagerTest.kt

@Test
fun `test command matching exact phrase`() = runBlocking {
    val manager = CommandManager(context, database)

    manager.registerCommand(Command(
        id = "test",
        phrase = "go home",
        action = CommandAction.NAVIGATE_HOME,
        confidence = 1.0f
    ))

    val results = manager.findCommands("go home")

    assertEquals(1, results.size)
    assertEquals("go home", results[0].phrase)
}

@Test
fun `test command matching fuzzy`() = runBlocking {
    val manager = CommandManager(context, database)

    manager.registerCommand(Command(
        id = "test",
        phrase = "go home",
        action = CommandAction.NAVIGATE_HOME,
        confidence = 1.0f
    ))

    // Typo: "gohome" instead of "go home"
    val results = manager.findCommands("gohome")

    assertTrue(results.isNotEmpty())
    assertTrue(results[0].phrase == "go home")
}
```

**Test hash generation:**

```kotlin
@Test
fun `test hash stability`() {
    val fingerprint1 = AccessibilityFingerprint(
        packageName = "com.example",
        className = "Button",
        text = "Login",
        contentDescription = null,
        viewIdResourceName = "login_btn",
        hierarchyPath = "/0/1/3"
    )

    val fingerprint2 = AccessibilityFingerprint(
        packageName = "com.example",
        className = "Button",
        text = "Login",
        contentDescription = null,
        viewIdResourceName = "login_btn",
        hierarchyPath = "/0/1/3"
    )

    val hash1 = UUIDCreator.generateHashUUID(fingerprint1)
    val hash2 = UUIDCreator.generateHashUUID(fingerprint2)

    assertEquals(hash1, hash2, "Same properties should generate same hash")
}
```

### Integration Testing

**Test database operations:**

```kotlin
@Test
fun `test command database operations`() = runBlocking {
    val database = CommandDatabase.getDatabase(context)
    val dao = database.vosCommandDao()

    // Insert
    val command = VOSCommand(
        commandPhrase = "test command",
        action = "TEST_ACTION",
        locale = "en_US",
        category = "test",
        priority = 5,
        confidence = 0.9f
    )

    val id = dao.insertCommand(command)
    assertTrue(id > 0)

    // Read
    val retrieved = dao.getById(id)
    assertNotNull(retrieved)
    assertEquals("test command", retrieved?.commandPhrase)

    // Update
    val updated = retrieved!!.copy(priority = 10)
    dao.updateCommand(updated)

    val afterUpdate = dao.getById(id)
    assertEquals(10, afterUpdate?.priority)

    // Delete
    dao.deleteCommand(updated)

    val afterDelete = dao.getById(id)
    assertNull(afterDelete)
}
```

### UI Testing (Instrumented)

**Test accessibility service:**

```kotlin
@RunWith(AndroidJUnit4::class)
class VoiceOSServiceTest {

    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun testAccessibilityServiceConnected() {
        // Enable accessibility service
        val service = getAccessibilityService()

        assertNotNull(service)
        assertTrue(service.isServiceConnected())
    }

    @Test
    fun testCommandExecution() {
        val service = getAccessibilityService()

        // Execute command
        service.handleVoiceCommand("go home", 1.0f)

        // Verify home screen is shown
        Thread.sleep(1000)
        val currentPackage = getCurrentPackageName()
        assertTrue(currentPackage.contains("launcher"))
    }
}
```

### Manual Testing Checklist

**Before each release:**

- [ ] Test voice recognition accuracy
- [ ] Test command execution (all tiers)
- [ ] Test database persistence across restarts
- [ ] Test web command coordination in browsers
- [ ] Test UI scraping and learning
- [ ] Test memory usage (no leaks)
- [ ] Test on multiple Android versions
- [ ] Test with different screen sizes
- [ ] Test with different locales
- [ ] Test edge cases (empty commands, long phrases)

---

## Debugging Techniques

### Logging Best Practices

**Use VoiceOsLogger:**

```kotlin
import com.augmentalis.voiceoslogger.Logger

class MyClass {
    companion object {
        private const val TAG = "MyClass"
    }

    fun myMethod() {
        Logger.d(TAG, "Debug message")
        Logger.i(TAG, "Info message")
        Logger.w(TAG, "Warning message")
        Logger.e(TAG, "Error message", exception)

        // Verbose logging (only in debug builds)
        if (BuildConfig.DEBUG) {
            Logger.v(TAG, "Verbose details: $details")
        }
    }
}
```

### Database Inspection

**Android Studio Database Inspector:**

1. Run app on device/emulator
2. Go to `View → Tool Windows → Database Inspector`
3. Select running process
4. Browse databases: `CommandDatabase`, `AppScrapingDatabase`, `WebScrapingDatabase`
5. Execute SQL queries to inspect data

**Manual SQL queries:**

```kotlin
// In debug code only
private fun debugDatabaseContents() {
    lifecycleScope.launch(Dispatchers.IO) {
        val db = database.openHelper.writableDatabase

        // Query all commands
        val cursor = db.rawQuery("SELECT * FROM vos_commands", null)
        while (cursor.moveToNext()) {
            val phrase = cursor.getString(cursor.getColumnIndex("commandPhrase"))
            val action = cursor.getString(cursor.getColumnIndex("action"))
            Log.d(TAG, "Command: $phrase -> $action")
        }
        cursor.close()

        // Count elements
        val countCursor = db.rawQuery("SELECT COUNT(*) FROM scraped_elements", null)
        if (countCursor.moveToFirst()) {
            val count = countCursor.getInt(0)
            Log.d(TAG, "Scraped elements: $count")
        }
        countCursor.close()
    }
}
```

### Accessibility Debug

**Enable accessibility logging:**

```kotlin
// In VoiceOSService

private var debugAccessibility = BuildConfig.DEBUG

override fun onAccessibilityEvent(event: AccessibilityEvent) {
    if (debugAccessibility) {
        Log.d(TAG, "=== Accessibility Event ===")
        Log.d(TAG, "Type: ${event.eventType}")
        Log.d(TAG, "Package: ${event.packageName}")
        Log.d(TAG, "Class: ${event.className}")
        Log.d(TAG, "Text: ${event.text}")
        Log.d(TAG, "Time: ${event.eventTime}")
    }

    // Normal processing...
}
```

**Dump accessibility tree:**

```kotlin
private fun dumpAccessibilityTree() {
    val rootNode = rootInActiveWindow ?: return

    Log.d(TAG, "=== Accessibility Tree ===")
    dumpNode(rootNode, depth = 0)
    rootNode.recycle()
}

private fun dumpNode(node: AccessibilityNodeInfo, depth: Int) {
    val indent = "  ".repeat(depth)

    Log.d(TAG, "$indent└─ ${node.className}")
    Log.d(TAG, "$indent   Text: ${node.text}")
    Log.d(TAG, "$indent   ID: ${node.viewIdResourceName}")
    Log.d(TAG, "$indent   Clickable: ${node.isClickable}")
    Log.d(TAG, "$indent   Bounds: ${node.getBoundsInScreen()}")

    for (i in 0 until node.childCount) {
        val child = node.getChild(i)
        if (child != null) {
            dumpNode(child, depth + 1)
            child.recycle()
        }
    }
}
```

### Performance Profiling

**Measure command execution time:**

```kotlin
private suspend fun executeCommandWithProfiling(command: String) {
    val startTime = System.currentTimeMillis()

    val result = try {
        commandManager.executeCommand(command)
    } finally {
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        Log.d(TAG, "Command '$command' took ${duration}ms")

        if (duration > 500) {
            Log.w(TAG, "⚠️ Slow command execution: ${duration}ms")
        }
    }
}
```

**Memory profiling:**

```kotlin
private fun logMemoryUsage() {
    val runtime = Runtime.getRuntime()

    val used = runtime.totalMemory() - runtime.freeMemory()
    val max = runtime.maxMemory()

    val usedMB = used / 1024 / 1024
    val maxMB = max / 1024 / 1024
    val percentUsed = (used * 100) / max

    Log.d(TAG, "Memory: ${usedMB}MB / ${maxMB}MB ($percentUsed%)")

    if (percentUsed > 80) {
        Log.w(TAG, "⚠️ High memory usage!")
    }
}
```

---

## Code Standards

### Naming Conventions

**Classes (PascalCase):**
```kotlin
class CommandManager { }
class VoiceOSService { }
class AccessibilityScrapingIntegration { }
```

**Methods/Functions (camelCase):**
```kotlin
fun handleVoiceCommand() { }
fun findElementByHash() { }
fun registerDatabaseCommands() { }
```

**Variables (camelCase):**
```kotlin
val commandManager = CommandManager(context)
val currentPackage = event.packageName
var isListening = false
```

**Constants (SCREAMING_SNAKE_CASE):**
```kotlin
companion object {
    private const val TAG = "MyClass"
    private const val MAX_RETRIES = 3
    private const val TIMEOUT_MS = 5000L
}
```

**Packages (lowercase):**
```kotlin
package com.augmentalis.commandmanager
package com.augmentalis.voiceaccessibility.database
```

### File Organization

**Standard file structure:**

```kotlin
// filename: MyClass.kt
// created: 2025-10-13 21:00:00 PDT
// author: Your Name
// © Augmentalis Inc

package com.augmentalis.mylibrary

import android.content.Context
import androidx.room.Database
// ... other imports

/**
 * MyClass - Brief description
 *
 * Detailed description of what this class does,
 * its responsibilities, and how it fits into the system.
 *
 * @param context Android context
 * @param config Configuration object
 */
class MyClass(
    private val context: Context,
    private val config: Config
) {

    // Properties
    private var state: State = State.IDLE

    // Initialization block
    init {
        initialize()
    }

    // Public methods
    fun publicMethod() {
        // Implementation
    }

    // Private methods
    private fun privateMethod() {
        // Implementation
    }

    // Companion object
    companion object {
        private const val TAG = "MyClass"

        fun staticMethod() {
            // Implementation
        }
    }
}
```

### Documentation

**Class documentation:**
```kotlin
/**
 * CommandManager - Central command processing system
 *
 * Responsible for:
 * - Command registration and storage
 * - Command matching (exact and fuzzy)
 * - Command execution with confidence filtering
 * - Integration with 3 database sources
 *
 * This is Tier 1 of the 3-tier command architecture.
 *
 * @param context Android application context
 * @param database CommandDatabase instance for persistence
 *
 * @see VoiceCommandProcessor Tier 2 processor
 * @see ActionCoordinator Tier 3 coordinator
 */
class CommandManager(
    private val context: Context,
    private val database: CommandDatabase
) { ... }
```

**Method documentation:**
```kotlin
/**
 * Find commands matching the input phrase
 *
 * Searches for commands using:
 * 1. Exact phrase matching
 * 2. Fuzzy matching (Levenshtein distance)
 * 3. Synonym matching
 *
 * Results are filtered by confidence threshold (>= 0.5) and
 * sorted by priority (highest first).
 *
 * @param input The user's voice input (normalized)
 * @return List of matching commands, sorted by priority
 *
 * @sample
 * ```kotlin
 * val matches = commandManager.findCommands("go home")
 * matches.forEach { cmd ->
 *     println("Found: ${cmd.phrase} (priority: ${cmd.priority})")
 * }
 * ```
 */
suspend fun findCommands(input: String): List<Command> { ... }
```

---

## Common Patterns

### Singleton Pattern (for managers)

```kotlin
object MyManager {
    private lateinit var context: Context
    private val database by lazy { MyDatabase.getDatabase(context) }

    fun initialize(appContext: Context) {
        context = appContext.applicationContext
    }

    fun doSomething() {
        // Use context and database
    }
}

// Usage:
MyManager.initialize(applicationContext)
MyManager.doSomething()
```

### Coroutine Scope (for services)

```kotlin
class VoiceOSService : AccessibilityService() {

    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main
    )

    override fun onCreate() {
        super.onCreate()

        serviceScope.launch {
            // Async initialization
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun handleCommand(command: String) {
        serviceScope.launch {
            // Async command handling
        }
    }
}
```

### Result Pattern (for error handling)

```kotlin
suspend fun executeCommand(command: String): Result<CommandResult> {
    return try {
        val result = performCommandExecution(command)
        Result.success(result)
    } catch (e: Exception) {
        Log.e(TAG, "Command execution failed", e)
        Result.failure(e)
    }
}

// Usage:
val result = executeCommand("go home")
result.onSuccess { cmdResult ->
    Log.d(TAG, "Success: $cmdResult")
}.onFailure { error ->
    Log.e(TAG, "Failed: ${error.message}")
}
```

### Observer Pattern (for listeners)

```kotlin
interface CommandListener {
    fun onCommandExecuted(command: String, success: Boolean)
    fun onCommandFailed(command: String, error: String)
}

class CommandManager {
    private val listeners = mutableListOf<CommandListener>()

    fun addListener(listener: CommandListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: CommandListener) {
        listeners.remove(listener)
    }

    private fun notifyExecuted(command: String, success: Boolean) {
        listeners.forEach { it.onCommandExecuted(command, success) }
    }
}
```

---

## Performance Optimization

### Database Optimization

**1. Use indices:**
```kotlin
@Entity(
    tableName = "commands",
    indices = [
        Index(value = ["commandPhrase"]),  // Fast phrase lookup
        Index(value = ["locale", "category"])  // Composite index
    ]
)
```

**2. Batch operations:**
```kotlin
// ✅ GOOD: Single transaction
suspend fun insertMultiple(commands: List<Command>) {
    database.withTransaction {
        dao.insertAll(commands)
    }
}

// ❌ BAD: Multiple transactions
suspend fun insertMultipleSlow(commands: List<Command>) {
    commands.forEach { command ->
        dao.insert(command)  // Separate transaction each time!
    }
}
```

**3. Use suspend functions:**
```kotlin
// ✅ GOOD: Non-blocking
suspend fun getCommands(): List<Command> {
    return withContext(Dispatchers.IO) {
        dao.getAllCommands()
    }
}
```

### Memory Optimization

**1. Recycle AccessibilityNodeInfo:**
```kotlin
// Always recycle after use
val node = rootInActiveWindow
try {
    // Use node
} finally {
    node?.recycle()
}
```

**2. Limit recursion depth:**
```kotlin
private fun traverse(node: AccessibilityNodeInfo, depth: Int = 0) {
    if (depth > 50) return  // Prevent stack overflow

    // Process node...

    for (i in 0 until node.childCount) {
        val child = node.getChild(i)
        if (child != null) {
            traverse(child, depth + 1)
            child.recycle()
        }
    }
}
```

**3. Use object pools:**
```kotlin
private val nodePool = ArrayDeque<AccessibilityNodeInfo>()

private fun borrowNode(): AccessibilityNodeInfo? {
    return nodePool.removeFirstOrNull()
}

private fun returnNode(node: AccessibilityNodeInfo) {
    nodePool.addLast(node)
}
```

---

## Troubleshooting

### Common Issues

**1. AccessibilityService not connecting**

**Problem:** Service doesn't start after enabling in settings

**Solution:**
- Check AndroidManifest.xml has correct service declaration
- Verify accessibility configuration XML exists
- Check logcat for permission errors
- Restart device if necessary

**2. Commands not recognized**

**Problem:** Speech recognition returns text but commands don't execute

**Solution:**
```kotlin
// Debug command matching
private fun debugCommandMatching(input: String) {
    Log.d(TAG, "Input: '$input'")

    // Check Tier 1
    val tier1 = commandManager.findCommands(input)
    Log.d(TAG, "Tier 1 matches: ${tier1.size}")

    // Check Tier 2
    val tier2 = voiceCommandProcessor.findMatches(input)
    Log.d(TAG, "Tier 2 matches: ${tier2.size}")

    // Check vocabulary
    val registered = speechEngineManager.isInVocabulary(input)
    Log.d(TAG, "In vocabulary: $registered")
}
```

**3. Database queries slow**

**Problem:** Command lookups take > 100ms

**Solution:**
- Add indices to frequently queried columns
- Use EXPLAIN QUERY PLAN to analyze queries
- Consider caching frequently accessed commands
- Reduce query complexity

**4. Memory leaks**

**Problem:** App memory grows over time

**Solution:**
- Profile with Android Studio Memory Profiler
- Check for unreleased AccessibilityNodeInfo objects
- Verify coroutine scopes are cancelled
- Check for listeners not being unregistered

---

**Document Version:** 3.0.0
**Last Updated:** 2025-10-13 21:44 PDT
**Status:** Production Ready
**Next:** VoiceOS4-User-Manual-Complete-251013-2144.md
