# SQLDelight KMP Database Migration Plan

**Date:** 2025-11-18
**Status:** Planning
**Priority:** High

---

## Overview

Migrate VoiceOS database from Room (Android-only) to SQLDelight (KMP) for cross-platform support.

### Current State
- **23 entities** across 2 modules
- **200+ queries** in 22+ DAOs
- Room with @TypeConverter, @Index, @ForeignKey

### Target State
- SQLDelight with ColumnAdapters
- Shared database layer for Android/iOS
- Compile-time SQL validation

---

## Migration Phases

### Phase 1: Core Command System (Week 1)

**Entities:**
1. `CommandHistoryEntry` - Command execution history
2. `CustomCommand` - User-defined commands
3. `RecognitionLearning` - Speech learning data

**Why First:** Core functionality, heavily used, includes Flow queries.

```sql
-- command_history_entry.sq
CREATE TABLE command_history_entry (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    originalText TEXT NOT NULL,
    processedCommand TEXT,
    confidence REAL NOT NULL,
    timestamp INTEGER NOT NULL,
    language TEXT NOT NULL,
    engineUsed TEXT NOT NULL,
    success INTEGER NOT NULL,
    executionTimeMs INTEGER NOT NULL,
    usageCount INTEGER NOT NULL DEFAULT 1,
    source TEXT NOT NULL DEFAULT 'VOICE'
);

getByTimeRange:
SELECT * FROM command_history_entry
WHERE timestamp BETWEEN :start AND :end
ORDER BY timestamp DESC;

getSuccessRate:
SELECT CASE WHEN COUNT(*) = 0 THEN 0.0
ELSE CAST(SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) AS REAL) / COUNT(*)
END AS rate FROM command_history_entry;
```

**ColumnAdapter for List<String>:**
```kotlin
val stringListAdapter = object : ColumnAdapter<List<String>, String> {
    override fun decode(databaseValue: String) =
        Json.decodeFromString<List<String>>(databaseValue)
    override fun encode(value: List<String>) =
        Json.encodeToString(value)
}
```

---

### Phase 2: UI Scraping System (Week 2)

**Entities:**
1. `GeneratedCommandEntity` - Generated voice commands
2. `UserInteractionEntity` - User interaction tracking
3. `ElementStateHistoryEntity` - Element state changes
4. `ScrapedAppEntity` - App metadata

**Complexity:** Foreign keys, JOINs, cascading deletes.

```sql
-- generated_commands.sq
CREATE TABLE generated_commands (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    element_hash TEXT NOT NULL,
    commandText TEXT NOT NULL,
    actionType TEXT NOT NULL,
    confidence REAL NOT NULL,
    synonyms TEXT, -- JSON array
    isUserApproved INTEGER NOT NULL DEFAULT 0,
    usageCount INTEGER NOT NULL DEFAULT 0,
    lastUsed INTEGER,
    createdAt INTEGER NOT NULL,
    FOREIGN KEY (element_hash) REFERENCES scraped_elements(element_hash) ON DELETE CASCADE
);

CREATE INDEX idx_gc_element ON generated_commands(element_hash);
CREATE INDEX idx_gc_action ON generated_commands(actionType);

getByAppId:
SELECT gc.* FROM generated_commands gc
JOIN scraped_elements se ON gc.element_hash = se.element_hash
WHERE se.app_id = :appId;

fuzzySearch:
SELECT * FROM generated_commands
WHERE commandText LIKE '%' || :searchText || '%';
```

---

### Phase 3: Settings & Analytics (Week 3)

**Entities:**
1. `AnalyticsSettings` - Analytics config
2. `RetentionSettings` - Data retention policy
3. `DeviceProfile` - Device configuration
4. `TouchGesture` - Touch gestures
5. `UserPreference` - User preferences
6. `ErrorReport` - Error tracking

**Note:** Single-record tables (id=1) for settings.

---

### Phase 4: Remaining Entities (Week 4)

**Entities:**
1. `LanguageModel`
2. `GestureLearningData`
3. `UsageStatistic`
4. `ScrappedCommand`
5. Screen/Hierarchy entities

---

## Technical Implementation

### Directory Structure

```
libraries/core/database/
├── build.gradle.kts (KMP with SQLDelight plugin)
├── src/
│   ├── commonMain/
│   │   ├── kotlin/
│   │   │   └── com/augmentalis/database/
│   │   │       ├── VoiceOSDatabase.kt
│   │   │       ├── adapters/
│   │   │       │   └── StringListAdapter.kt
│   │   │       └── DatabaseFactory.kt (expect)
│   │   └── sqldelight/
│   │       └── com/augmentalis/database/
│   │           ├── command_history.sq
│   │           ├── custom_command.sq
│   │           └── ... (all .sq files)
│   ├── androidMain/
│   │   └── kotlin/
│   │       └── DatabaseFactory.android.kt (actual)
│   └── iosMain/
│       └── kotlin/
│           └── DatabaseFactory.ios.kt (actual)
```

### build.gradle.kts

```kotlin
plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("app.cash.sqldelight") version "2.0.1"
}

sqldelight {
    databases {
        create("VoiceOSDatabase") {
            packageName.set("com.augmentalis.database")
            generateAsync.set(true)
        }
    }
}

kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation("app.cash.sqldelight:coroutines-extensions:2.0.1")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
        }
        androidMain.dependencies {
            implementation("app.cash.sqldelight:android-driver:2.0.1")
        }
        iosMain.dependencies {
            implementation("app.cash.sqldelight:native-driver:2.0.1")
        }
    }
}
```

### Platform Factory

```kotlin
// commonMain - expect
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

// androidMain - actual
actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            VoiceOSDatabase.Schema,
            context,
            "voiceos.db"
        )
    }
}

// iosMain - actual
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            VoiceOSDatabase.Schema,
            "voiceos.db"
        )
    }
}
```

### Flow Support

```kotlin
// Extension for Flow queries
fun <T : Any> Query<T>.asFlowList(): Flow<List<T>> {
    return this.asFlow().mapToList(Dispatchers.IO)
}

// Usage
val learningFlow: Flow<List<RecognitionLearning>> =
    database.recognitionLearningQueries
        .findByEngineAndType(engine, type)
        .asFlowList()
```

---

## Migration Checklist

### Per-Entity Checklist

- [ ] Create .sq file with CREATE TABLE
- [ ] Add indices (CREATE INDEX)
- [ ] Add foreign keys with CASCADE
- [ ] Migrate all DAO queries to .sq
- [ ] Create ColumnAdapters if needed
- [ ] Test queries compile
- [ ] Write unit tests
- [ ] Verify Flow queries work
- [ ] Test batch operations in transactions

### Integration Checklist

- [ ] Create DatabaseManager wrapper
- [ ] Implement Hilt module for DI
- [ ] Add migration from Room (if data exists)
- [ ] Update all module dependencies
- [ ] Run full integration tests
- [ ] Performance benchmarks vs Room

---

## Key Queries to Validate

### Complex JOINs
```sql
-- Must work across tables
getCommandsByApp:
SELECT gc.* FROM generated_commands gc
JOIN scraped_elements se ON gc.element_hash = se.element_hash
WHERE se.app_id = ?;
```

### Aggregations
```sql
-- Must return correct types
getSuccessRate:
SELECT CASE WHEN COUNT(*) = 0 THEN 0.0
ELSE CAST(SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) AS REAL) / COUNT(*)
END AS rate FROM command_history_entry;
```

### Batch Operations
```kotlin
database.transaction {
    items.forEach { item ->
        database.commandHistoryQueries.insert(item)
    }
}
```

---

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Data loss during migration | HIGH | Export Room data before switching |
| Query performance regression | MEDIUM | Benchmark critical queries |
| Missing Room features | MEDIUM | Validate all DAO methods covered |
| iOS testing complexity | LOW | Set up iOS simulator tests early |

---

## Success Criteria

1. All 23 entities migrated
2. All 200+ queries working
3. Flow observers functional
4. Foreign key cascades verified
5. Unit test coverage >90%
6. iOS builds and runs
7. No performance regression vs Room

---

## References

- SQLDelight Docs: https://cashapp.github.io/sqldelight/
- KMP Docs: https://kotlinlang.org/docs/multiplatform.html
- Current Room entities: `modules/managers/VoiceDataManager/src/main/java/com/augmentalis/datamanager/entities/`

