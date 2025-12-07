# VoiceOS Database Architecture Guide

**Version:** 2.0 (SQLDelight)
**Last Updated:** 2025-11-28
**Status:** Production Ready

---

## Overview

VoiceOS uses **SQLDelight** (Kotlin Multiplatform SQL) for all database operations, providing type-safe SQL queries with compile-time verification. The system consists of two separate SQLite databases with distinct responsibilities.

---

## Architecture

### Two-Database System

```
┌─────────────────────────────────────────────────────────────┐
│                        VoiceOS App                           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────────┐      ┌─────────────────────────┐  │
│  │  VoiceOSCore         │      │  CommandManager         │  │
│  │  LearnApp            │      │                         │  │
│  │  Scraping            │      │  Static Commands        │  │
│  └──────┬───────────────┘      └──────┬──────────────────┘  │
│         │                             │                     │
│         ▼                             ▼                     │
│  ┌──────────────────────┐      ┌─────────────────────────┐  │
│  │  voiceos.db          │      │  command_database       │  │
│  │  (Main Database)     │      │  (Commands Only)        │  │
│  │                      │      │                         │  │
│  │  • Scraped Data      │      │  • Voice Commands       │  │
│  │  • LearnApp Data     │      │  • Command Usage        │  │
│  │  • Analytics         │      │  • Context Prefs        │  │
│  │  • Preferences       │      │  • Command History      │  │
│  │  • UUID Management   │      └─────────────────────────┘  │
│  │  • Plugins           │                                   │
│  │  • Error Reports     │                                   │
│  └──────────────────────┘                                   │
└─────────────────────────────────────────────────────────────┘
```

---

## Database 1: voiceos.db (Main Database)

**Purpose:** All VoiceOS data except static commands

**Location:** `/data/data/com.augmentalis.voiceos/databases/voiceos.db`

### Table Categories

#### Scraping Data
- **scraped_app** - Metadata for scraped applications
- **scraped_element** - UI elements extracted from apps
- **scraped_hierarchy** - Parent-child relationships between elements
- **commands_generated** - Voice commands generated from UI elements
- **commands_scraped** - Commands scraped from third-party apps (LearnApp)
- **screen_context** - Screen-level context information
- **screen_transition** - Navigation between screens
- **user_interaction** - User interaction tracking
- **element_state_history** - Element state changes over time
- **element_relationship** - Semantic relationships between elements

#### LearnApp Data
- **learned_apps** - Apps that have been learned by LearnApp
- **exploration_sessions** - LearnApp exploration session data
- **navigation_edges** - Navigation paths discovered during exploration
- **screen_state** - Screen states captured during exploration

#### System Data
- **analytics_settings** - Analytics configuration
- **context_preference** - User context preferences
- **custom_command** - User-defined custom commands
- **device_profile** - Device-specific settings
- **error_report** - Application error reports

#### Plugin System
- **plugins** - Installed plugin metadata
- **plugin_dependencies** - Plugin dependency relationships
- **plugin_permissions** - Plugin permission grants

#### UUID Management
- **uuid_aliases** - UUID alias mappings
- **uuid_hierarchy** - UUID hierarchical relationships
- **uuid_analytics** - UUID-based analytics

#### Learning Systems
- **gesture_learning** - Gesture recognition learning data
- **language_model** - Language model training data
- **recognition_learning** - Speech recognition improvements
- **touch_gesture** - Touch gesture patterns

---

## Database 2: command_database (Commands)

**Purpose:** Static voice commands and usage tracking

**Location:** `/data/data/com.augmentalis.voiceos/databases/command_database.db`

### Tables

- **commands_static** - Static command definitions (from .vos files)
- **command_usage** - Command usage statistics
- **command_history_entry** - Command execution history
- **context_preference** - Context-aware command preferences

---

## SQLDelight Implementation

### Query Files (.sq)

All SQL queries are defined in `.sq` files:

```
libraries/core/database/src/commonMain/sqldelight/com/augmentalis/database/
├── ScrapedApp.sq
├── ScrapedElement.sq
├── ScreenContext.sq
├── GeneratedCommand.sq
├── ElementRelationship.sq
├── command/
│   ├── VoiceCommand.sq
│   ├── CommandUsage.sq
│   └── ContextPreference.sq
├── plugin/
│   ├── Plugin.sq
│   ├── PluginDependency.sq
│   └── PluginPermission.sq
└── uuid/
    ├── UUIDAlias.sq
    └── UUIDHierarchy.sq
```

### Upsert Pattern (INSERT OR REPLACE)

**All tables use INSERT OR REPLACE for conflict resolution:**

```sql
-- Example from ScrapedApp.sq
insert:
INSERT OR REPLACE INTO scraped_app VALUES (
    ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
);
```

**Why OR REPLACE?**
- Matches original Room's `OnConflictStrategy.REPLACE` behavior
- Prevents SQLiteConstraintException on duplicate inserts
- Allows safe re-scraping and updates
- No application code changes needed

**See:** ADR-010 for complete technical rationale

---

## Repository Pattern

### VoiceOSCore Usage

```kotlin
// Get adapter instance (singleton)
val adapter = VoiceOSCoreDatabaseAdapter.getInstance(context)

// Direct SQLDelight access (PREFERRED)
val apps = adapter.databaseManager.scrapedApps.getAll()
adapter.databaseManager.scrapedElements.insert(elementDTO)

// Transaction support
adapter.databaseManager.transaction {
    scrapedApps.insert(appDTO)
    scrapedElements.insert(elementDTO)
    generatedCommands.insert(commandDTO)
}

// Helper methods (backward compatibility)
adapter.insertApp(appEntity) // Converts Entity → DTO
val app = adapter.getApp(packageName)
```

### LearnApp Usage

```kotlin
// Get adapter instance
val adapter = LearnAppDatabaseAdapter.getInstance(context)

// Room-compatible DAO API
val dao = adapter.learnAppDao()

// Use DAO methods (Room-compatible)
dao.insertLearnedApp(appEntity)
val app = dao.getLearnedApp(packageName)

// Transaction support
dao.transaction {
    insertLearnedApp(app)
    insertExplorationSession(session)
    insertNavigationEdges(edges)
}
```

### CommandManager Usage

```kotlin
// CommandManager uses its own database
val resolver = DatabaseCommandResolver(
    voiceCommandRepository,
    commandUsageRepository,
    contextPreferenceRepository
)

// Load commands
val commands = resolver.getCommandsByCategory("navigation")

// Record usage
commandUsageRepository.recordUsage(
    commandId = command.id,
    context = currentContext,
    success = true
)
```

---

## Entity ↔ DTO Conversion

### Why Two Types?

**DTOs (Data Transfer Objects):**
- Generated by SQLDelight
- Match database schema exactly
- Used for database operations
- Located in `com.augmentalis.database.dto`

**Entities:**
- VoiceOSCore domain objects
- May have additional fields
- May have different types
- Located in module packages

### Conversion Example

```kotlin
// ScrapedAppDTO → AppEntity
fun ScrapedAppDTO.toAppEntity(): AppEntity {
    return AppEntity(
        appId = this.appId,
        packageName = this.packageName,
        versionCode = this.versionCode,
        isFullyLearned = this.isFullyLearned == 1L,
        scrapedElementCount = this.elementCount.toInt(),
        lastScraped = this.lastScrapedAt,
        // ... additional fields with defaults
    )
}

// AppEntity → ScrapedAppDTO
fun AppEntity.toScrapedAppDTO(): ScrapedAppDTO {
    return ScrapedAppDTO(
        appId = this.packageName,
        packageName = this.packageName,
        versionCode = this.versionCode,
        isFullyLearned = if (this.isFullyLearned) 1L else 0L,
        elementCount = (this.scrapedElementCount ?: 0).toLong(),
        lastScrapedAt = this.lastScraped ?: System.currentTimeMillis(),
        // ... all required fields
    )
}
```

---

## Migration from Room

### What Changed

**Before (Room):**
```kotlin
@Entity(tableName = "scraped_app")
data class ScrapedAppEntity(...)

@Dao
interface ScrapedAppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: ScrapedAppEntity)
}

@Database(entities = [...], version = X)
abstract class VoiceOSAppDatabase : RoomDatabase() {
    abstract fun scrapedAppDao(): ScrapedAppDao
}
```

**After (SQLDelight):**
```sql
-- ScrapedApp.sq
CREATE TABLE scraped_app (...);

insert:
INSERT OR REPLACE INTO scraped_app VALUES (...);
```

```kotlin
// Repository interface (unchanged)
interface IScrapedAppRepository {
    suspend fun insert(app: ScrapedAppDTO)
}

// SQLDelight implementation
class SQLDelightScrapedAppRepository(
    private val queries: ScrapedAppQueries
) : IScrapedAppRepository {
    override suspend fun insert(app: ScrapedAppDTO) {
        queries.insert(...)
    }
}
```

### Migration Benefits

✅ **Type Safety:** Compile-time SQL validation
✅ **Multiplatform:** Works on Android, iOS, JVM, JS
✅ **Performance:** No annotation processing overhead
✅ **Flexibility:** Direct SQL control
✅ **Maintainability:** SQL and Kotlin in separate files

---

## Database Schema

### Key Constraints

All tables with UNIQUE constraints or PRIMARY KEYs use INSERT OR REPLACE:

```sql
-- Primary key on ID
CREATE TABLE scraped_app (
    appId TEXT PRIMARY KEY NOT NULL,
    ...
);

-- Unique constraint on hash
CREATE TABLE scraped_element (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    elementHash TEXT NOT NULL UNIQUE,
    ...
);

-- Composite unique constraint
CREATE TABLE element_relationship (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sourceElementHash TEXT NOT NULL,
    targetElementHash TEXT NOT NULL,
    relationshipType TEXT NOT NULL,
    UNIQUE(sourceElementHash, targetElementHash, relationshipType)
);
```

### Foreign Keys

```sql
-- Cascade delete
CREATE TABLE scraped_element (
    ...
    appId TEXT NOT NULL,
    FOREIGN KEY (appId) REFERENCES scraped_app(appId) ON DELETE CASCADE
);
```

### Indexes

```sql
-- ScrapedElement.sq
CREATE INDEX idx_se_app ON scraped_element(appId);
CREATE INDEX idx_se_hash ON scraped_element(elementHash);
CREATE INDEX idx_se_uuid ON scraped_element(uuid);
CREATE INDEX idx_se_view_id ON scraped_element(viewIdResourceName);
CREATE INDEX idx_se_class ON scraped_element(className);
```

---

## Common Operations

### Insert/Update (Upsert)

```kotlin
// Single insert
adapter.databaseManager.scrapedApps.insert(appDTO)

// Batch insert
adapter.databaseManager.transaction {
    apps.forEach { scrapedApps.insert(it) }
}
```

### Query

```kotlin
// Get all
val allApps = adapter.databaseManager.scrapedApps.getAll()

// Get by ID
val app = adapter.databaseManager.scrapedApps.getById("com.example.app")

// Get by package
val app = adapter.databaseManager.scrapedApps.getByPackage("com.example.app")

// Get fully learned
val learnedApps = adapter.databaseManager.scrapedApps.getFullyLearned()
```

### Delete

```kotlin
// Delete by ID
adapter.databaseManager.scrapedApps.deleteById("com.example.app")

// Delete all elements for an app (cascade)
adapter.databaseManager.scrapedElements.deleteByApp("com.example.app")

// Delete old records
adapter.databaseManager.scrapedElements.deleteOlderThan(cutoffTimestamp)
```

### Transactions

```kotlin
// Multi-table transaction
adapter.databaseManager.transaction {
    scrapedApps.insert(appDTO)
    scrapedElements.deleteByApp(appDTO.appId)
    scrapedElements.insertBatch(newElements)
    generatedCommands.insertBatch(newCommands)
}
```

---

## Performance Considerations

### Batch Operations

```kotlin
// ❌ BAD: Individual inserts in loop
elements.forEach { element ->
    adapter.databaseManager.scrapedElements.insert(element)
}

// ✅ GOOD: Batch in transaction
adapter.databaseManager.transaction {
    elements.forEach { element ->
        scrapedElements.insert(element)
    }
}
```

### Query Optimization

```kotlin
// ✅ Use indexes
val elements = queries.getByApp(appId) // Uses idx_se_app

// ✅ Use specific queries
val clickable = queries.getClickable(appId)

// ❌ Avoid loading all then filtering
val all = queries.getAll()
val clickable = all.filter { it.isClickable == 1L }
```

---

## Testing

### Unit Tests

```kotlin
@Test
fun testUpsertBehavior() = runTest {
    val app = ScrapedAppDTO(...)

    // First insert
    repository.insert(app)
    assertEquals(1, repository.count())

    // Second insert with same ID (should update, not crash)
    val updated = app.copy(elementCount = 10L)
    repository.insert(updated)
    assertEquals(1, repository.count()) // Still 1 record

    // Verify update
    val result = repository.getById(app.appId)
    assertEquals(10L, result?.elementCount)
}
```

---

## Troubleshooting

### Common Issues

**Issue: SQLiteConstraintException**
```
UNIQUE constraint failed: scraped_element.elementHash
```
**Solution:** Should not occur anymore - all tables use INSERT OR REPLACE

**Issue: Type mismatch**
```
Type mismatch: inferred type is Long but Int was expected
```
**Solution:** SQLDelight uses Long for INTEGER columns, convert with `.toInt()` or `.toLong()`

**Issue: Null values**
```
lateinit property has not been initialized
```
**Solution:** Check nullable fields in DTO, use `?:` for defaults

---

## References

### Documentation
- SQLDelight: https://cashapp.github.io/sqldelight/
- SQLite: https://www.sqlite.org/
- ADR-010: Room to SQLDelight Migration Completion

### Source Locations
- Database queries: `libraries/core/database/src/commonMain/sqldelight/`
- Repositories: `libraries/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/`
- DTOs: `libraries/core/database/src/commonMain/kotlin/com/augmentalis/database/dto/`
- Adapters: `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/database/`

---

**Document Version:** 2.1
**Last Updated:** 2025-12-05
**Status:** ✅ Production Ready
**Changelog:** v2.1 - Renamed command tables: voice_commands→commands_static, generated_command→commands_generated, scrapped_command→commands_scraped
