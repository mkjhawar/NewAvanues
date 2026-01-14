<!--
filename: README.md
created: 2025-01-23 22:00:00 PST
author: VOS4 Development Team
copyright: Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
purpose: Complete VosDataManager module documentation
last-modified: 2025-01-23 22:15:00 PST
version: 1.0.1
-->

# VosDataManager Module Documentation

## Overview
VosDataManager is the centralized data persistence layer for VOS4, providing robust database management using ObjectBox as the underlying storage engine.

## Module Information
- **Package**: `com.augmentalis.vosdatamanager`
- **Version**: 1.0.0
- **Database**: ObjectBox 4.0.3
- **Min SDK**: 28
- **Target SDK**: 33

## Architecture

### Core Components

#### 1. ObjectBox Singleton (`core/ObjectBox.kt`)
The main database access point with the following features:

##### **Initialization & Lifecycle**
- Thread-safe singleton initialization with double-checking
- Custom database directory: `vos4-database`
- Graceful shutdown with resource cleanup
- Context retention for runtime operations

##### **Key Features**
| Feature | Description | Method |
|---------|-------------|---------|
| **Initialization** | One-time setup with application context | `init(context: Context): Boolean` |
| **Size Monitoring** | Real-time database size using native API | `getDatabaseSizeMB(): Float` |
| **Data Clearing** | Factory reset capability | `clearAllData()` |
| **Statistics** | Runtime monitoring data | `getStatistics(): Map<String, Any>` |
| **Path Access** | Get database directory location | `getDatabasePath(): String?` |
| **Resource Cleanup** | Proper shutdown | `close()` |

##### **Logging Levels**
- `ERROR`: Initialization failures, critical errors
- `WARN`: Data clearing operations, uninitialized access
- `INFO`: Successful initialization/shutdown
- `DEBUG`: Configuration details, size calculations
- `VERBOSE`: Detailed size measurements

#### 2. DatabaseModule (`core/DatabaseModule.kt`)
Central manager coordinating all repository access and module lifecycle.

**Responsibilities:**
- Repository initialization and management
- Auto-cleanup scheduling
- Data export/import coordination
- Module state management

#### 3. Data Repositories (`data/`)
Direct implementation repositories for each entity type:

| Repository | Entity | Purpose |
|------------|--------|---------|
| `UserPreferenceRepo` | UserPreference | User settings and preferences |
| `CommandHistoryRepo` | CommandHistoryEntry | Voice command history |
| `CustomCommandRepo` | CustomCommand | User-defined commands |
| `TouchGestureRepo` | TouchGesture | Touch gesture configurations |
| `UserSequenceRepo` | UserSequence | Command sequences |
| `DeviceProfileRepo` | DeviceProfile | Device-specific profiles |
| `UsageStatisticRepo` | UsageStatistic | App usage analytics |
| `LanguageModelRepo` | LanguageModel | Language model data |
| `RetentionSettingsRepo` | RetentionSettings | Data retention policies |
| `AnalyticsSettingsRepo` | AnalyticsSettings | Analytics configuration |
| `ErrorReportRepo` | ErrorReport | Error tracking |
| `GestureLearningRepo` | GestureLearningData | ML gesture data |

#### 4. Import/Export (`io/`)
- `DataExporter.kt`: JSON export functionality
- `DataImporter.kt`: JSON import with validation

### Database Configuration

**ObjectBox Setup:**
```kotlin
// Database location
/data/data/com.augmentalis.voiceos/files/vos4-database/

// Initialization in Application class
class VoiceOS : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize database
        if (!ObjectBox.init(this)) {
            Log.e(TAG, "Failed to initialize database")
        }
    }
}
```

## Usage Examples

### Basic Operations
```kotlin
// Get database instance
val databaseModule = DatabaseModule(context)

// Check database size
val sizeInMB = ObjectBox.getDatabaseSizeMB()
Log.d(TAG, "Database size: $sizeInMB MB")

// Save user preference
databaseModule.userPreferences.insert(
    UserPreference(
        key = "theme",
        value = "dark",
        category = "ui"
    )
)

// Export data
val jsonData = databaseModule.exportData(includeAll = true)

// Clear all data (factory reset)
ObjectBox.clearAllData()
```

### Monitoring
```kotlin
// Get statistics
val stats = ObjectBox.getStatistics()
stats.forEach { (key, value) ->
    Log.d(TAG, "$key: $value")
}

// Check if initialized
if (ObjectBox.isInitialized()) {
    // Safe to use database
}
```

## Migration & Compatibility

### From DataManager/DataMGR
- Module renamed to `VosDataManager` to avoid Android namespace conflicts
- Package changed from `com.ai.datamanager` to `com.augmentalis.vosdatamanager`
- All existing data structures maintained for compatibility

## Performance Considerations

1. **Database Size**: Monitor with `getDatabaseSizeMB()` - typical size 5-50MB
2. **Auto-Cleanup**: Runs daily to maintain optimal performance
3. **Batch Operations**: Use `insertAll()` for multiple records
4. **Threading**: All operations use `Dispatchers.IO` for background execution

## Security

- **Encryption**: Available but currently uses placeholder key (TODO: Implement Android Keystore)
- **Data Isolation**: Per-app sandbox protection
- **Export Control**: Selective export with category filtering

## Known Issues & TODOs

1. ⚠️ **Encryption Key**: Currently hardcoded, needs Android Keystore implementation
2. ⚠️ **Memory Management**: All repositories initialized at startup (consider lazy loading)
3. ⚠️ **Indexes**: No explicit database indexes defined yet

## Testing

### Verification Checklist
- [ ] Database initializes on app start
- [ ] Size calculation returns accurate values
- [ ] Data persists across app restarts
- [ ] Export/Import maintains data integrity
- [ ] Clear all data removes all entities
- [ ] Logging appears in appropriate log levels

## Dependencies
```gradle
implementation("io.objectbox:objectbox-kotlin:4.0.3")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
implementation("com.google.code.gson:gson:2.13.1")
```

## Changelog

### 2025-01-23
- Merged ObjectBox and DatabaseObjectBox implementations
- Added comprehensive documentation
- Improved logging and error handling
- Implemented efficient sizeOnDisk() method
- Added statistics monitoring

### Previous
- Renamed from DataManager to VosDataManager
- Removed BaseRepository inheritance pattern
- Implemented direct repository pattern

---

**Maintainer**: Manoj Jhawar  
**Last Updated**: 2025-01-23