# Data Module - Developer Documentation
**Author:** Manoj Jhawar  
**Code-Reviewed-By:** CCA

## Table of Contents
1. [Quick Start](#quick-start)
2. [Architecture Overview](#architecture-overview)
3. [Installation & Setup](#installation--setup)
4. [Core Concepts](#core-concepts)
5. [API Reference](#api-reference)
6. [Code Examples](#code-examples)
7. [Best Practices](#best-practices)
8. [Troubleshooting](#troubleshooting)
9. [Migration Guide](#migration-guide)
10. [Performance Optimization](#performance-optimization)

## Quick Start

### For Beginners
```kotlin
// 1. Initialize the Data module
val dataModule = DataModule(context)
dataModule.initialize()

// 2. Store a preference
dataModule.userPreferences.setPreference("theme", "dark")

// 3. Retrieve a preference
val theme = dataModule.userPreferences.getString("theme", "light")

// 4. Save command history
val command = CommandHistoryEntry(
    originalText = "open settings",
    processedCommand = "OPEN_SETTINGS",
    confidence = 0.95f,
    timestamp = System.currentTimeMillis(),
    language = "en-US",
    engineUsed = "vosk",
    success = true,
    executionTimeMs = 150L
)
dataModule.commandHistory.insert(command)
```

### For Advanced Users
```kotlin
// Complex query with retention management
class AdvancedDataUsage(private val dataModule: DataModule) {
    
    suspend fun performSmartCleanup() {
        // Get retention settings
        val settings = dataModule.retentionSettings.getSettings()
        
        // Cleanup with custom retention
        dataModule.commandHistory.cleanupOldEntries(
            retainCount = settings?.commandHistoryRetainCount ?: 50,
            maxDays = settings?.commandHistoryMaxDays ?: 30
        )
        
        // Export before cleanup
        val backup = dataModule.exportData(includeAll = true)
        saveBackupToFile(backup)
    }
    
    suspend fun analyzeUsagePatterns() {
        // Get most used commands
        val topCommands = dataModule.customCommands.getMostUsedCommands(10)
        
        // Analyze success rates
        val successRate = dataModule.commandHistory.getSuccessRate()
        
        // Update analytics
        if (successRate < 90f) {
            dataModule.analyticsSettings.setErrorThreshold(0.15f)
        }
    }
}
```

## Architecture Overview

### Module Structure
```
data/
├── src/main/java/com/augmentalis/voiceos/data/
│   ├── DataModule.kt              # Main module controller
│   ├── VOS3ObjectBox.kt          # ObjectBox singleton
│   ├── entities/                  # Data models
│   │   ├── UserPreference.kt
│   │   ├── CommandHistoryEntry.kt
│   │   └── ... (11 more entities)
│   ├── repository/                # Data access layer
│   │   ├── BaseRepository.kt
│   │   ├── UserPreferenceRepository.kt
│   │   └── ... (12 more repositories)
│   └── export/                    # Import/Export
│       ├── DataExporter.kt
│       └── DataImporter.kt
├── build.gradle.kts
└── PRD.md
```

### Design Patterns

#### Repository Pattern
Each entity has a dedicated repository providing:
- CRUD operations
- Custom queries
- Business logic
- Coroutine support

#### Singleton Pattern
VOS3ObjectBox manages single BoxStore instance:
- Thread-safe initialization
- Centralized database access
- Resource management

## Installation & Setup

### 1. Add Dependencies
```kotlin
// build.gradle.kts (module level)
plugins {
    id("io.objectbox") version "3.6.0"
}

dependencies {
    implementation(project(":modules:data"))
    implementation("io.objectbox:objectbox-android:3.6.0")
    implementation("io.objectbox:objectbox-kotlin:3.6.0")
}
```

### 2. Initialize in Application
```kotlin
class VOS3Application : Application() {
    lateinit var dataModule: DataModule
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Data module
        dataModule = DataModule(this)
        
        lifecycleScope.launch {
            if (dataModule.initialize()) {
                Log.i("VOS3", "Data module ready")
                dataModule.start()
            }
        }
    }
}
```

### 3. Add to Manifest
```xml
<application
    android:name=".VOS3Application"
    android:allowBackup="false"
    ... >
```

## Core Concepts

### Entities
13 ObjectBox entities representing different data types:

#### UserPreference
Stores key-value pairs for module settings
```kotlin
@Entity
data class UserPreference(
    @Id var id: Long = 0,
    val key: String,      // Unique identifier
    val value: String,    // Stored as string
    val type: String,     // "string", "boolean", "int", "float"
    val module: String    // Module owner
)
```

#### CommandHistoryEntry
Tracks voice command executions
```kotlin
@Entity
data class CommandHistoryEntry(
    @Id var id: Long = 0,
    val originalText: String,      // User's spoken text
    val processedCommand: String,  // Parsed command
    val confidence: Float,          // Recognition confidence
    val timestamp: Long,            // Execution time
    val language: String,           // Language code
    val engineUsed: String,         // "vosk" or "vivoka"
    val success: Boolean,           // Execution result
    val executionTimeMs: Long,      // Performance metric
    val usageCount: Int = 1        // Frequency tracking
)
```

### Repositories
Type-safe data access with coroutine support:

#### BaseRepository
Abstract base providing common operations:
```kotlin
abstract class BaseRepository<T> {
    abstract val box: Box<T>
    
    suspend fun insert(entity: T): Long
    suspend fun update(entity: T)
    suspend fun delete(entity: T)
    suspend fun getById(id: Long): T?
    suspend fun getAll(): List<T>
    suspend fun count(): Long
}
```

#### Specialized Repositories
Each repository extends BaseRepository with custom methods:
```kotlin
class CommandHistoryRepository : BaseRepository<CommandHistoryEntry>() {
    suspend fun getRecentCommands(limit: Int): List<CommandHistoryEntry>
    suspend fun getMostUsedCommands(limit: Int): List<CommandHistoryEntry>
    suspend fun cleanupOldEntries(retainCount: Int, maxDays: Int)
    suspend fun getSuccessRate(): Float
}
```

## API Reference

### DataModule

#### Properties
| Property | Type | Description |
|----------|------|-------------|
| status | StateFlow<ModuleStatus> | Current module state |
| userPreferences | UserPreferenceRepository | Preference storage |
| commandHistory | CommandHistoryRepository | Command tracking |
| customCommands | CustomCommandRepository | User commands |
| touchGestures | TouchGestureRepository | Gesture storage |
| userSequences | UserSequenceRepository | Command sequences |
| deviceProfiles | DeviceProfileRepository | Device configs |
| usageStatistics | UsageStatisticRepository | Usage metrics |
| languageModels | LanguageModelRepository | Model management |
| retentionSettings | RetentionSettingsRepository | Cleanup config |
| analyticsSettings | AnalyticsSettingsRepository | Analytics config |
| errorReports | ErrorReportRepository | Error tracking |
| gestureLearning | GestureLearningRepository | Gesture metrics |

#### Methods
```kotlin
suspend fun initialize(): Boolean
suspend fun start(): Boolean
suspend fun stop()
suspend fun exportData(includeAll: Boolean = true): String?
suspend fun importData(jsonData: String, replaceExisting: Boolean = false): Boolean
fun getDatabaseSizeMB(): Float
suspend fun clearAllData()
```

### Repository Methods

#### UserPreferenceRepository
```kotlin
// Store preferences
suspend fun setPreference(
    key: String, 
    value: String, 
    type: String = "string", 
    module: String = "core"
)

// Retrieve preferences
suspend fun getPreference(key: String, module: String? = null): UserPreference?
fun getString(key: String, defaultValue: String = ""): String
fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
fun getInt(key: String, defaultValue: Int = 0): Int
fun getFloat(key: String, defaultValue: Float = 0f): Float

// Module management
suspend fun getModulePreferences(module: String): List<UserPreference>
suspend fun deleteModulePreferences(module: String)
```

#### CommandHistoryRepository
```kotlin
// Query commands
suspend fun getRecentCommands(limit: Int = 50): List<CommandHistoryEntry>
suspend fun getMostUsedCommands(limit: Int = 50): List<CommandHistoryEntry>
suspend fun getCommandsByLanguage(language: String): List<CommandHistoryEntry>

// Metrics
suspend fun getSuccessRate(): Float

// Maintenance
suspend fun cleanupOldEntries(retainCount: Int, maxDays: Int)
```

#### CustomCommandRepository
```kotlin
// Command management
suspend fun getActiveCommands(): List<CustomCommand>
suspend fun findCommandByPhrase(phrase: String): CustomCommand?
suspend fun toggleCommandActive(commandId: Long)

// Usage tracking
suspend fun incrementUsageCount(commandId: Long)
suspend fun getMostUsedCommands(limit: Int = 10): List<CustomCommand>
```

## Code Examples

### Example 1: Basic CRUD Operations
```kotlin
class DataExample(private val dataModule: DataModule) {
    
    // CREATE
    suspend fun createCustomCommand() {
        val command = CustomCommand(
            name = "Open Browser",
            phrases = listOf("open browser", "launch browser", "browser"),
            action = "LAUNCH_APP",
            parameters = """{"package": "com.android.chrome"}""",
            language = "en-US",
            isActive = true,
            createdDate = System.currentTimeMillis()
        )
        
        val id = dataModule.customCommands.insert(command)
        Log.d("Data", "Created command with ID: $id")
    }
    
    // READ
    suspend fun readCommands() {
        val allCommands = dataModule.customCommands.getAll()
        val activeCommands = dataModule.customCommands.getActiveCommands()
        val topCommands = dataModule.customCommands.getMostUsedCommands(5)
        
        topCommands.forEach { command ->
            Log.d("Data", "${command.name}: ${command.usageCount} uses")
        }
    }
    
    // UPDATE
    suspend fun updateCommand(commandId: Long) {
        val command = dataModule.customCommands.getById(commandId)
        command?.let {
            val updated = it.copy(
                phrases = it.phrases + "start browser",
                usageCount = it.usageCount + 1
            )
            dataModule.customCommands.update(updated)
        }
    }
    
    // DELETE
    suspend fun deleteCommand(commandId: Long) {
        dataModule.customCommands.deleteById(commandId)
    }
}
```

### Example 2: Complex Queries
```kotlin
class AdvancedQueries(private val dataModule: DataModule) {
    
    suspend fun analyzeCommandPerformance() {
        // Get commands by success rate
        val history = dataModule.commandHistory.getAll()
        
        val performanceByEngine = history
            .groupBy { it.engineUsed }
            .mapValues { (_, commands) ->
                val successful = commands.count { it.success }
                val total = commands.size
                (successful * 100f) / total
            }
        
        performanceByEngine.forEach { (engine, rate) ->
            Log.d("Analytics", "$engine success rate: $rate%")
        }
        
        // Find slow commands
        val slowCommands = history
            .filter { it.executionTimeMs > 500 }
            .sortedByDescending { it.executionTimeMs }
            .take(10)
        
        slowCommands.forEach { command ->
            Log.w("Performance", "${command.processedCommand}: ${command.executionTimeMs}ms")
        }
    }
    
    suspend fun findRelatedGestures(commandName: String) {
        // Find gestures linked to command
        val gestures = dataModule.touchGestures.getAll()
            .filter { it.associatedCommand == commandName }
        
        // Get learning data for each gesture
        gestures.forEach { gesture ->
            val learningData = dataModule.gestureLearning
                .getDataForGesture(gesture.id)
            
            Log.d("Gesture", "${gesture.name}: ${learningData?.successRate}% success")
        }
    }
}
```

### Example 3: Export/Import with Validation
```kotlin
class DataPortability(private val dataModule: DataModule) {
    
    suspend fun exportUserData(): File? {
        try {
            // Export all data
            val jsonData = dataModule.exportData(includeAll = true)
                ?: return null
            
            // Save to file
            val exportDir = File(context.getExternalFilesDir(null), "backups")
            exportDir.mkdirs()
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(Date())
            val file = File(exportDir, "vos3_backup_$timestamp.json")
            
            file.writeText(jsonData)
            Log.i("Export", "Data exported to: ${file.absolutePath}")
            
            return file
        } catch (e: Exception) {
            Log.e("Export", "Export failed", e)
            return null
        }
    }
    
    suspend fun importUserData(file: File, merge: Boolean = false) {
        try {
            val jsonData = file.readText()
            
            // Validate before import
            if (!validateImportData(jsonData)) {
                Log.e("Import", "Invalid data format")
                return
            }
            
            // Import with options
            val success = dataModule.importData(
                jsonData = jsonData,
                replaceExisting = !merge
            )
            
            if (success) {
                Log.i("Import", "Data imported successfully")
            }
        } catch (e: Exception) {
            Log.e("Import", "Import failed", e)
        }
    }
    
    private fun validateImportData(jsonData: String): Boolean {
        return try {
            val json = JSONObject(jsonData)
            json.has("encodedData") || json.has("version")
        } catch (e: Exception) {
            false
        }
    }
}
```

### Example 4: Retention Management
```kotlin
class RetentionManager(private val dataModule: DataModule) {
    
    suspend fun configureRetention() {
        // Update retention settings
        val settings = RetentionSettings(
            commandHistoryRetainCount = 100,  // Keep top 100
            commandHistoryMaxDays = 60,       // 60 days history
            statisticsRetentionDays = 180,    // 6 months stats
            enableAutoCleanup = true,
            notifyBeforeCleanup = true,
            maxDatabaseSizeMB = 200           // 200MB limit
        )
        
        dataModule.retentionSettings.updateSettings(settings)
    }
    
    suspend fun performManualCleanup() {
        val sizeBefore = dataModule.getDatabaseSizeMB()
        
        // Cleanup different data types
        dataModule.commandHistory.cleanupOldEntries(50, 30)
        dataModule.usageStatistics.cleanupOldStatistics(90)
        dataModule.touchGestures.cleanupUnusedSystemGestures()
        dataModule.errorReports.cleanupSentReports()
        
        val sizeAfter = dataModule.getDatabaseSizeMB()
        val freed = sizeBefore - sizeAfter
        
        Log.i("Cleanup", "Freed ${freed}MB (${sizeBefore}MB -> ${sizeAfter}MB)")
    }
    
    suspend fun monitorDatabaseSize() {
        val currentSize = dataModule.getDatabaseSizeMB()
        val settings = dataModule.retentionSettings.getSettings()
        val maxSize = settings?.maxDatabaseSizeMB ?: 100
        
        when {
            currentSize > maxSize -> {
                Log.e("Storage", "Database exceeded limit: ${currentSize}MB / ${maxSize}MB")
                performManualCleanup()
            }
            currentSize > maxSize * 0.8f -> {
                Log.w("Storage", "Database approaching limit: ${currentSize}MB / ${maxSize}MB")
            }
            else -> {
                Log.i("Storage", "Database size healthy: ${currentSize}MB / ${maxSize}MB")
            }
        }
    }
}
```

### Example 5: Error Tracking
```kotlin
class ErrorTracking(private val dataModule: DataModule) {
    
    suspend fun logError(
        exception: Exception,
        context: String,
        module: String
    ) {
        dataModule.errorReports.logError(
            type = exception.javaClass.simpleName,
            message = exception.message ?: "Unknown error",
            module = module,
            context = context,
            commandText = null // No command context
        )
        
        // Check if error rate is high
        val errorRate = dataModule.errorReports.getErrorRate()
        if (errorRate > 10f) {
            enableDetailedAnalytics()
        }
    }
    
    suspend fun enableDetailedAnalytics() {
        val settings = dataModule.analyticsSettings.getSettings()
            ?: AnalyticsSettings()
        
        val updated = settings.copy(
            trackPerformance = true,
            autoEnableOnErrors = true
        )
        
        dataModule.analyticsSettings.updateSettings(updated)
    }
    
    suspend fun sendErrorReports() {
        val settings = dataModule.analyticsSettings.getSettings()
        
        if (settings?.userConsent == true) {
            val unsent = dataModule.errorReports.getUnsentReports()
            
            unsent.forEach { report ->
                // Send to developers (implement email/API)
                sendReportToDevs(report)
                dataModule.errorReports.markAsSent(report.id)
            }
        }
    }
}
```

## Best Practices

### 1. Initialization
Always initialize in Application class:
```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize once globally
        VOS3ObjectBox.init(this)
    }
}
```

### 2. Coroutine Usage
Use appropriate dispatchers:
```kotlin
// Heavy database operations
withContext(Dispatchers.IO) {
    dataModule.commandHistory.cleanupOldEntries(50, 30)
}

// UI updates
withContext(Dispatchers.Main) {
    updateUI(data)
}
```

### 3. Error Handling
Always wrap database operations:
```kotlin
try {
    val result = dataModule.customCommands.insert(command)
} catch (e: Exception) {
    Log.e("Database", "Insert failed", e)
    // Handle error appropriately
}
```

### 4. Memory Management
Clean up when done:
```kotlin
override fun onDestroy() {
    super.onDestroy()
    lifecycleScope.launch {
        dataModule.stop()
    }
}
```

### 5. Performance Tips
- Use batch operations for multiple inserts
- Query only needed fields
- Index frequently queried properties
- Use pagination for large datasets

### 6. Security
- Never store sensitive data unencrypted
- Use encrypted exports for backups
- Sanitize user input before storage
- Validate imports thoroughly

## Troubleshooting

### Common Issues

#### Issue: "ObjectBox not initialized"
**Solution:**
```kotlin
// Ensure initialization before use
if (!VOS3ObjectBox.isInitialized()) {
    VOS3ObjectBox.init(context)
}
```

#### Issue: "Database size growing too large"
**Solution:**
```kotlin
// Enable auto-cleanup
val settings = RetentionSettings(
    enableAutoCleanup = true,
    maxDatabaseSizeMB = 100
)
dataModule.retentionSettings.updateSettings(settings)
```

#### Issue: "Import fails with validation error"
**Solution:**
```kotlin
// Check data format
val isValid = try {
    val json = JSONObject(importData)
    json.has("version") && json.has("encodedData")
} catch (e: Exception) {
    false
}
```

#### Issue: "Slow queries"
**Solution:**
```kotlin
// Use indexes and limits
val recentCommands = dataModule.commandHistory
    .getRecentCommands(limit = 20) // Limit results
```

### Debug Tools

#### ObjectBox Browser
```kotlin
// Enable in debug builds
if (BuildConfig.DEBUG) {
    val started = AndroidObjectBrowser(store).start(context)
    Log.d("ObjectBox", "Browser: http://localhost:8090")
}
```

#### Database Statistics
```kotlin
fun logDatabaseStats() {
    Log.d("Stats", "DB Size: ${dataModule.getDatabaseSizeMB()}MB")
    Log.d("Stats", "Commands: ${dataModule.commandHistory.count()}")
    Log.d("Stats", "Gestures: ${dataModule.touchGestures.count()}")
}
```

## Migration Guide

### From SQLite/Room
```kotlin
// 1. Export old data
val oldData = exportFromSQLite()

// 2. Transform to VOS3 format
val vos3Data = transformData(oldData)

// 3. Import to ObjectBox
dataModule.importData(vos3Data, replaceExisting = true)
```

### Version Updates
```kotlin
// Handle schema changes
@Entity
data class CommandHistoryEntry(
    @Id var id: Long = 0,
    // ... existing fields ...
    val newField: String = "" // Add with default value
)
```

## Performance Optimization

### Query Optimization
```kotlin
// Bad: Loading all data
val all = dataModule.commandHistory.getAll()
val filtered = all.filter { it.success }

// Good: Query filtering
val successful = dataModule.commandHistory.query {
    box.query().equal(CommandHistoryEntry_.success, true).build()
}
```

### Batch Operations
```kotlin
// Bad: Individual inserts
commands.forEach { dataModule.customCommands.insert(it) }

// Good: Batch insert
dataModule.customCommands.insertAll(commands)
```

### Lazy Loading
```kotlin
// Use StateFlow for reactive updates
val commands = dataModule.customCommands.getAllAsFlow()
    .stateIn(scope, SharingStarted.Lazily, emptyList())
```

## Testing

### Unit Tests
```kotlin
@Test
fun testCommandInsertion() = runTest {
    val command = createTestCommand()
    val id = dataModule.commandHistory.insert(command)
    
    assertNotNull(id)
    assertTrue(id > 0)
    
    val retrieved = dataModule.commandHistory.getById(id)
    assertEquals(command.originalText, retrieved?.originalText)
}
```

### Integration Tests
```kotlin
@Test
fun testExportImport() = runTest {
    // Insert test data
    insertTestData()
    
    // Export
    val exported = dataModule.exportData(true)
    assertNotNull(exported)
    
    // Clear and reimport
    dataModule.clearAllData()
    val success = dataModule.importData(exported!!, false)
    assertTrue(success)
    
    // Verify data restored
    val count = dataModule.commandHistory.count()
    assertTrue(count > 0)
}
```

## Support & Resources

### Documentation
- [ObjectBox Documentation](https://docs.objectbox.io/)
- [VOS3 Architecture Guide](../docs/ARCHITECTURE.md)
- [Module Integration Guide](../docs/INTEGRATION.md)

### Contact
- **Module Owner:** Data Team
- **Support:** vos3-data@augmentalis.com
- **Issues:** github.com/augmentalis/vos3/issues

### Version History
- **1.0.0** - Initial release with ObjectBox integration
- **1.0.1** - Added performance metrics (planned)
- **1.1.0** - Cloud sync support (future)