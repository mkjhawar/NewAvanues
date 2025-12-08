# Product Requirements Document - Data Module
**Author:** Manoj Jhawar  
**Code-Reviewed-By:** CCA

## Module Name: Data
**Version:** 1.0.0  
**Status:** IN_PROGRESS  
**Priority:** HIGH

## 1. Executive Summary
The Data module provides persistent storage using ObjectBox database for all VOS3 modules, ensuring user preferences, command history, custom commands, and device profiles are preserved across sessions while maintaining optimal performance and minimal memory footprint.

## 2. Objectives
- Provide high-performance local data persistence via ObjectBox
- Ensure data privacy and security with local-only storage
- Enable efficient querying for voice command scenarios
- Support data export/import for user control
- Maintain <5MB memory overhead

## 3. Scope
### In Scope
- ObjectBox database integration
- Repository pattern implementation
- Data entities for all modules
- Export/import functionality
- Data migration support

### Out of Scope
- Cloud synchronization (future feature)
- External database connections
- Real-time data streaming

## 4. Technical Architecture
### Components
- DataModule: Main module controller
- VOS3ObjectBox: Database singleton
- Repositories: Data access layer
- Entities: Data models
- DataExporter: Import/export functionality

### Dependencies
- Internal: Core module
- External: ObjectBox 3.6.0

## 5. Data Entities (Option A - Complete Implementation)

### Core Entities
```kotlin
@Entity data class UserPreference(
    @Id var id: Long = 0,
    val key: String,
    val value: String,
    val type: String, // "string", "boolean", "int", "float"
    val module: String // which module owns this preference
)

@Entity data class CommandHistoryEntry(
    @Id var id: Long = 0,
    val originalText: String,
    val processedCommand: String,
    val confidence: Float,
    val timestamp: Long,
    val language: String,
    val engineUsed: String,
    val success: Boolean,
    val executionTimeMs: Long
)

@Entity data class CustomCommand(
    @Id var id: Long = 0,
    val name: String,
    val phrases: List<String>,
    val action: String,
    val parameters: String, // JSON
    val language: String,
    val isActive: Boolean,
    val createdDate: Long,
    val usageCount: Int
)
```

### Touch Gesture Learning Entities
```kotlin
@Entity data class TouchGesture(
    @Id var id: Long = 0,
    val name: String,
    val gestureData: String, // JSON of touch points/timing
    val description: String,
    val createdDate: Long,
    val usageCount: Int,
    val isSystemGesture: Boolean, // false for user-created
    val associatedCommand: String? // optional command link
)

@Entity data class UserSequence(
    @Id var id: Long = 0,
    val name: String,
    val description: String,
    val steps: List<String>, // JSON array of commands/gestures
    val triggerPhrase: String,
    val language: String,
    val createdDate: Long,
    val lastUsed: Long,
    val usageCount: Int,
    val estimatedDurationMs: Long
)
```

### Extended Data Entities
```kotlin
@Entity data class DeviceProfile(
    @Id var id: Long = 0,
    val deviceType: String, // "smartglasses", "phone", "tablet"
    val deviceModel: String,
    val settings: String, // JSON configuration
    val calibrationData: String?, // JSON calibration
    val isActive: Boolean,
    val lastConnected: Long
)

@Entity data class UsageStatistic(
    @Id var id: Long = 0,
    val type: String, // "command", "gesture", "sequence"
    val identifier: String,
    val count: Int,
    val totalTimeMs: Long,
    val successRate: Float,
    val lastUsed: Long,
    val dateRecorded: Long // for daily/weekly stats
)

@Entity data class LanguageModel(
    @Id var id: Long = 0,
    val languageCode: String,
    val engine: String, // "vosk", "vivoka"
    val modelPath: String,
    val downloadStatus: String, // "not_downloaded", "downloading", "ready"
    val fileSize: Long,
    val downloadDate: Long?,
    val version: String
)
```

## 6. Data Retention Policy (Option C - Hybrid Approach)

### Automatic Cleanup (Non-Essential Data)
- **Command History**: Auto-cleanup after 30 days, retain top 50 most used (user configurable: 25-200)
- **Usage Statistics**: Keep 90 days, aggregate older data monthly
- **System Touch Gestures**: Clean unused after 90 days
- **Language Model Cache**: Auto-cleanup temporary files after 7 days

### User-Controlled Retention (Personal Data)
- **Custom Commands**: Never auto-delete (user-created content)
- **User Touch Gestures**: Never auto-delete (user-created content)
- **User Sequences**: Never auto-delete (user-created content)
- **Device Profiles**: Keep until user manually removes

### Intelligent Retention Settings
```kotlin
@Entity data class RetentionSettings(
    @Id var id: Long = 0,
    val commandHistoryRetainCount: Int = 50, // user configurable 25-200
    val commandHistoryMaxDays: Int = 30,
    val statisticsRetentionDays: Int = 90,
    val enableAutoCleanup: Boolean = true,
    val notifyBeforeCleanup: Boolean = true,
    val maxDatabaseSizeMB: Int = 100
)
```

### Cleanup Strategy
1. **Storage Warning**: Notify user at 80MB database size
2. **Intelligent Cleanup**: Retain most used items based on `usageCount` and `lastUsed`
3. **User Choice**: Option to disable auto-cleanup completely
4. **Backup Before Cleanup**: Create export before major cleanups

## 7. Export/Import Functionality

### Export Configuration
```kotlin
@Entity data class ExportConfiguration(
    @Id var id: Long = 0,
    val includePreferences: Boolean = true,
    val includeCommandHistory: Boolean = true,
    val includeCustomCommands: Boolean = true,
    val includeUserSequences: Boolean = true,
    val includeTouchGestures: Boolean = true,
    val includeDeviceProfiles: Boolean = true,
    val includeStatistics: Boolean = true,
    val encodeExport: Boolean = true // default: encoded
)
```

### Export Format (Encoded JSON)
```kotlin
data class VOS3ExportData(
    val version: String = "1.0.0",
    val exportDate: Long,
    val deviceId: String, // anonymized device identifier
    val dataChecksum: String, // integrity verification
    val encodedData: String // Base64 encoded + AES encryption
)

// Decrypted structure
data class VOS3Data(
    val userPreferences: List<UserPreference>? = null,
    val commandHistory: List<CommandHistoryEntry>? = null,
    val customCommands: List<CustomCommand>? = null,
    val userSequences: List<UserSequence>? = null,
    val touchGestures: List<TouchGesture>? = null,
    val deviceProfiles: List<DeviceProfile>? = null,
    val usageStatistics: List<UsageStatistic>? = null,
    val retentionSettings: RetentionSettings? = null
)
```

### Security Implementation
- **Encoding**: AES-256 encryption with app-specific key
- **Decoder Key**: Stored securely in app, separate developer key for troubleshooting
- **Integrity**: SHA-256 checksum verification
- **Privacy**: No personally identifiable information in exports

### Import Options
```kotlin
data class ImportOptions(
    val replaceExisting: Boolean = false, // false = merge, true = replace
    val importPreferences: Boolean = true,
    val importCommandHistory: Boolean = true,
    val importCustomCommands: Boolean = true,
    val importUserSequences: Boolean = true,
    val importTouchGestures: Boolean = true,
    val importDeviceProfiles: Boolean = true,
    val importStatistics: Boolean = false, // default: skip stats
    val verifyChecksum: Boolean = true
)
```

### Developer Decoder Tool
**Separate tool/app for developers to decode export files for troubleshooting:**
- Desktop Java/Kotlin application
- Requires developer key (not shipped with app)
- Logs all decode operations for audit
- Read-only analysis (no modification capability)

## 8. Data Migration Strategy

### VOS2 Migration (Option C - Clean Slate)
- **No automatic migration** from VOS2 (not in production)
- **Fresh start** for all VOS3 users
- **Simplified implementation** without legacy compatibility

### Future Migration Support
- **Version tracking** for future VOS3 updates
- **Schema migration** support in ObjectBox for database changes
- **Backward compatibility** for VOS3.x updates

```kotlin
@Entity data class DatabaseVersion(
    @Id var id: Long = 1, // single record
    val currentVersion: Int = 1,
    val lastMigration: Long,
    val migrationHistory: List<String> // JSON array of versions
)
```

## 9. Performance Requirements

### Performance Targets
| Metric | Target | Priority |
|--------|--------|----------|
| Query response time | <10ms single entity | HIGH |
| Bulk insert | >1000 records/second | HIGH |
| Database size | <100MB typical usage | MEDIUM |
| Memory overhead | <5MB for module | HIGH |
| Startup time | <100ms initialization | HIGH |
| Export time | <5s for 100MB data | MEDIUM |
| Import time | <10s for 100MB data | MEDIUM |

### Analytics & Reporting Configuration
```kotlin
@Entity data class AnalyticsSettings(
    @Id var id: Long = 0,
    val trackPerformance: Boolean = true, // ON by default for testing phase
    val autoEnableOnErrors: Boolean = true, // Enable if error rate >10%
    val errorThreshold: Float = 0.10f,
    val sendAnonymousReports: Boolean = false,
    val includeDeviceId: Boolean = false, // For support purposes
    val userConsent: Boolean = false,
    val consentDate: Long? = null,
    val detailedLogDays: Int = 7,
    val aggregateOlderData: Boolean = true
)

@Entity data class ErrorReport(
    @Id var id: Long = 0,
    val errorType: String,
    val errorMessage: String,
    val context: String, // Sanitized
    val timestamp: Long,
    val commandText: String?, // Anonymized
    val moduleAffected: String,
    val deviceId: String?, // Optional, only if user consents
    val sent: Boolean = false,
    val sentDate: Long? = null
)
```

### Success Criteria
- [x] All 8 entity types properly defined
- [ ] ObjectBox integration complete
- [ ] Repository pattern implemented for all entities
- [ ] Export/import with AES-256 encryption
- [ ] Retention policies enforced automatically
- [ ] Performance targets achieved
- [ ] Unit test coverage >80%
- [ ] No data loss on app crashes
- [ ] Developer decoder tool created

## 10. Testing Strategy

### Unit Tests Required
- Repository CRUD operations for all entities
- Retention policy enforcement
- Export/import with encryption
- Data cleanup algorithms
- Query performance benchmarks
- Concurrent access handling

### Integration Tests
- Module initialization with Core
- EventBus communication
- Cross-module data access
- Backup/restore workflows

## 11. ObjectBox Integration
**MANDATORY:** All local data storage uses ObjectBox as per VOS3 coding standards.

## 12. Gesture Support Configuration

### Supported Gesture Types
- **Multi-finger**: Up to 3 fingers
- **Directional**: 8 directions + custom angles
- **Rotation**: Degrees (0-360°) or percentage
- **Pressure**: Light/Medium/Hard with fallback
- **Velocity**: Slow/Normal/Fast tracking
- **Zones**: Custom screen areas for gestures
- **Patterns**: Tap patterns, shapes (circle, check, X, L)
- **Advanced**: Hold & drag, double tap & hold, flick, zigzag

### Gesture Learning Data
```kotlin
@Entity data class GestureLearningData(
    @Id var id: Long = 0,
    val gestureId: Long,
    val userId: String?, // Anonymous ID
    val successRate: Float,
    val averageVelocity: Float,
    val averagePressure: Float?,
    val commonMistakes: String, // JSON array
    val zonePreferences: String // JSON map
)
```

## 13. Implementation Priority
**Status:** IN_PROGRESS - High priority foundation module required by all other modules.