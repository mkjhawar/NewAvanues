# Implementation Plan: SQLDelight Schema v3 Migration

**Date:** 2025-12-19
**Module:** VoiceOS Database (core/database)
**Severity:** CRITICAL
**Estimated Time:** 2-3 hours

---

## Overview

**Problem:** DTO and Repository code reference Schema v3 fields (added 2025-12-13) but SQL schema was never updated, causing compilation failures.

**Impact:**
- ❌ VoiceOSCore cannot compile
- ❌ LearnApp cannot compile
- ❌ LearnAppDev cannot compile
- ❌ Blocks version catalog migration validation

**Solution:** Update GeneratedCommand.sq schema to match DTO expectations, regenerate SQLDelight code.

---

## Root Cause Analysis

**Schema Drift:**
```
DTO (GeneratedCommandDTO.kt)     SQL Schema (GeneratedCommand.sq)
├─ 20 fields (Schema v3)         ├─ 15 fields (old schema)
├─ Updated: 2025-12-13           ├─ Last updated: 2025-12-05
└─ Expects version tracking      └─ Missing 5 columns
```

**Missing Columns:**
1. `appId` - Package name for pagination
2. `appVersion` - Version string (e.g., "8.2024.11.123")
3. `versionCode` - Version code for comparison
4. `lastVerified` - Last time element was verified
5. `isDeprecated` - Whether command is deprecated

**Missing Queries:**
1. `lastInsertRowId` - Get ID of last inserted row
2. `deleteByPackage` - Delete commands by package name

**Parameter Mismatch:**
- Current `insert` query: 9 parameters
- Repository expects: 14 parameters

---

## Implementation Phases

### Phase 1: Update SQL Schema (30 min)

**File:** `Modules/VoiceOS/core/database/src/commonMain/sqldelight/com/augmentalis/database/GeneratedCommand.sq`

#### Task 1.1: Add Missing Columns to CREATE TABLE
```sql
CREATE TABLE commands_generated (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    elementHash TEXT NOT NULL,
    commandText TEXT NOT NULL,
    actionType TEXT NOT NULL,
    confidence REAL NOT NULL,
    synonyms TEXT,
    isUserApproved INTEGER NOT NULL DEFAULT 0,
    usageCount INTEGER NOT NULL DEFAULT 0,
    lastUsed INTEGER,
    createdAt INTEGER NOT NULL,

    -- P2 Task 3: Package-based pagination
    appId TEXT NOT NULL DEFAULT '',

    -- Schema v3: Version tracking (2025-12-13)
    appVersion TEXT NOT NULL DEFAULT '',
    versionCode INTEGER NOT NULL DEFAULT 0,
    lastVerified INTEGER,
    isDeprecated INTEGER NOT NULL DEFAULT 0,

    -- ADR-014: Unified Learning columns
    synced_to_ava INTEGER NOT NULL DEFAULT 0,
    synced_at INTEGER,

    UNIQUE(elementHash, commandText)
);
```

#### Task 1.2: Create Indexes for New Columns
```sql
-- Existing indexes
CREATE INDEX idx_gc_element ON commands_generated(elementHash);
CREATE INDEX idx_gc_action ON commands_generated(actionType);
CREATE INDEX idx_gc_confidence ON commands_generated(confidence);

-- New indexes for Schema v3
CREATE INDEX idx_gc_appId ON commands_generated(appId);
CREATE INDEX idx_gc_versionCode ON commands_generated(versionCode);
CREATE INDEX idx_gc_deprecated ON commands_generated(isDeprecated);
```

#### Task 1.3: Update INSERT Query
```sql
insert:
INSERT OR REPLACE INTO commands_generated(
    elementHash, commandText, actionType, confidence, synonyms,
    isUserApproved, usageCount, lastUsed, createdAt,
    appId, appVersion, versionCode, lastVerified, isDeprecated
)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
```

#### Task 1.4: Update INSERT IF NOT EXISTS Query
```sql
insertIfNotExists:
INSERT OR IGNORE INTO commands_generated(
    elementHash, commandText, actionType, confidence, synonyms,
    isUserApproved, usageCount, lastUsed, createdAt,
    appId, appVersion, versionCode, lastVerified, isDeprecated
)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
```

#### Task 1.5: Update UPDATE Query
```sql
update:
UPDATE commands_generated
SET elementHash = ?,
    commandText = ?,
    actionType = ?,
    confidence = ?,
    synonyms = ?,
    isUserApproved = ?,
    usageCount = ?,
    lastUsed = ?,
    appId = ?,
    appVersion = ?,
    versionCode = ?,
    lastVerified = ?,
    isDeprecated = ?
WHERE id = ?;
```

#### Task 1.6: Add Missing Queries
```sql
-- Get last inserted row ID (required by repository)
lastInsertRowId:
SELECT last_insert_rowid();

-- Delete commands by package name
deleteByPackage:
DELETE FROM commands_generated WHERE appId = ?;

-- Get commands by app version
getByAppVersion:
SELECT * FROM commands_generated
WHERE appId = ? AND versionCode = ?
ORDER BY usageCount DESC;

-- Get deprecated commands
getDeprecated:
SELECT * FROM commands_generated
WHERE isDeprecated = 1
ORDER BY lastVerified DESC;

-- Mark commands as deprecated (for version cleanup)
markDeprecated:
UPDATE commands_generated
SET isDeprecated = 1, lastVerified = ?
WHERE appId = ? AND versionCode < ?;

-- Clean up old deprecated commands
deleteDeprecated:
DELETE FROM commands_generated
WHERE isDeprecated = 1 AND lastVerified < ?;
```

---

### Phase 2: Database Migration (45 min)

**File:** Create `Modules/VoiceOS/core/database/src/commonMain/sqldelight/migrations/3.sqm`

#### Task 2.1: Create Migration File
```sql
-- Migration from Schema v2 to Schema v3
-- Adds version tracking columns for app version management

-- Add new columns with default values
ALTER TABLE commands_generated ADD COLUMN appId TEXT NOT NULL DEFAULT '';
ALTER TABLE commands_generated ADD COLUMN appVersion TEXT NOT NULL DEFAULT '';
ALTER TABLE commands_generated ADD COLUMN versionCode INTEGER NOT NULL DEFAULT 0;
ALTER TABLE commands_generated ADD COLUMN lastVerified INTEGER;
ALTER TABLE commands_generated ADD COLUMN isDeprecated INTEGER NOT NULL DEFAULT 0;

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_gc_appId ON commands_generated(appId);
CREATE INDEX IF NOT EXISTS idx_gc_versionCode ON commands_generated(versionCode);
CREATE INDEX IF NOT EXISTS idx_gc_deprecated ON commands_generated(isDeprecated);
```

#### Task 2.2: Update SQLDelight Configuration
**File:** `Modules/VoiceOS/core/database/build.gradle.kts`

Update the `sqldelight` block:
```kotlin
sqldelight {
    databases {
        create("VoiceOSDatabase") {
            packageName.set("com.augmentalis.database")
            generateAsync.set(false)
            deriveSchemaFromMigrations.set(true)  // Changed from false
            verifyMigrations.set(true)  // Changed from false
        }
    }
}
```

#### Task 2.3: Test Migration Path
Create test to verify migration works:
```kotlin
// Test in androidInstrumentedTest
@Test
fun testMigrationFromV2ToV3() {
    // Insert data with old schema
    // Run migration
    // Verify new columns exist with defaults
}
```

---

### Phase 3: Regenerate SQLDelight Code (15 min)

#### Task 3.1: Clean Generated Code
```bash
./gradlew :Modules:VoiceOS:core:database:clean
rm -rf Modules/VoiceOS/core/database/build/generated/
```

#### Task 3.2: Regenerate SQLDelight Interfaces
```bash
./gradlew :Modules:VoiceOS:core:database:generateCommonMainVoiceOSDatabaseInterface
```

**Expected Output:**
- `Commands_generated` interface updated with 5 new properties
- All query functions regenerated
- `GeneratedCommandQueries` interface updated

#### Task 3.3: Verify Generated Code
Check `build/generated/sqldelight/code/VoiceOSDatabase/commonMain/`:
- ✅ `Commands_generated.kt` has 20 properties
- ✅ `GeneratedCommandQueries.kt` has all queries
- ✅ `lastInsertRowId()` function exists

---

### Phase 4: Update Repository Implementation (30 min)

**File:** `Modules/VoiceOS/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/impl/SQLDelightGeneratedCommandRepository.kt`

#### Task 4.1: Add New Repository Methods
```kotlin
override suspend fun deleteByPackage(appId: String) = withContext(Dispatchers.Default) {
    queries.deleteByPackage(appId)
}

override suspend fun getByAppVersion(appId: String, versionCode: Long): List<GeneratedCommandDTO> =
    withContext(Dispatchers.Default) {
        queries.getByAppVersion(appId, versionCode)
            .executeAsList()
            .map { it.toGeneratedCommandDTO() }
    }

override suspend fun getDeprecated(): List<GeneratedCommandDTO> =
    withContext(Dispatchers.Default) {
        queries.getDeprecated()
            .executeAsList()
            .map { it.toGeneratedCommandDTO() }
    }

override suspend fun markDeprecated(appId: String, olderThanVersionCode: Long) =
    withContext(Dispatchers.Default) {
        queries.markDeprecated(
            lastVerified = System.currentTimeMillis(),
            appId = appId,
            versionCode = olderThanVersionCode
        )
    }

override suspend fun deleteDeprecated(olderThanTimestamp: Long) =
    withContext(Dispatchers.Default) {
        queries.deleteDeprecated(olderThanTimestamp)
    }
```

#### Task 4.2: Update Interface
**File:** `Modules/VoiceOS/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/IGeneratedCommandRepository.kt`

Add method signatures:
```kotlin
suspend fun deleteByPackage(appId: String)
suspend fun getByAppVersion(appId: String, versionCode: Long): List<GeneratedCommandDTO>
suspend fun getDeprecated(): List<GeneratedCommandDTO>
suspend fun markDeprecated(appId: String, olderThanVersionCode: Long)
suspend fun deleteDeprecated(olderThanTimestamp: Long)
```

---

### Phase 5: Build & Test (30 min)

#### Task 5.1: Build Database Module
```bash
./gradlew :Modules:VoiceOS:core:database:build
```

**Success Criteria:**
- ✅ No compilation errors
- ✅ All DTO fields map correctly
- ✅ Repository queries compile

#### Task 5.2: Run Database Unit Tests
```bash
./gradlew :Modules:VoiceOS:core:database:test
```

#### Task 5.3: Build Dependent Modules
```bash
./gradlew :Modules:VoiceOS:apps:LearnApp:assembleDebug
./gradlew :Modules:VoiceOS:apps:LearnAppDev:assembleDebug
./gradlew :Modules:VoiceOS:apps:VoiceOSCore:assembleDebug
```

**Success Criteria:**
- ✅ All three apps compile successfully
- ✅ No schema-related errors

#### Task 5.4: Run Integration Tests
```bash
./gradlew :Modules:VoiceOS:core:database:connectedAndroidTest
```

---

### Phase 6: Documentation (15 min)

#### Task 6.1: Update Schema Documentation
**File:** `Docs/VoiceOS/LivingDocs/LD-VOS-Database-Schema-V1.md`

Document Schema v3 changes:
```markdown
## Schema v3 (2025-12-19)

### New Columns in commands_generated

| Column | Type | Description |
|--------|------|-------------|
| appId | TEXT | Package name for app-specific filtering |
| appVersion | TEXT | Version string (e.g., "8.2024.11.123") |
| versionCode | INTEGER | Numeric version for comparison |
| lastVerified | INTEGER | Timestamp when element was last seen |
| isDeprecated | INTEGER | 0=active, 1=deprecated (pending cleanup) |

### New Queries

- `deleteByPackage(appId)` - Delete all commands for an app
- `lastInsertRowId()` - Get ID of last inserted row
- `getByAppVersion(appId, versionCode)` - Get commands for specific app version
- `getDeprecated()` - Get all deprecated commands
- `markDeprecated(appId, versionCode)` - Mark old versions as deprecated
- `deleteDeprecated(timestamp)` - Clean up old deprecated commands

### Migration Path

Apps using Schema v2 will automatically migrate to v3 on first database access.
Default values are applied to existing rows.
```

#### Task 6.2: Update ADR Document
**File:** Create `Docs/VoiceOS/adrs/ADR-015-Schema-v3-Version-Tracking.md`

```markdown
# ADR-015: Schema v3 - Version Tracking for Generated Commands

## Status
Implemented - 2025-12-19

## Context
Generated commands need to track which app version they belong to, allowing:
- Cleanup of commands from uninstalled apps
- Version-specific command management
- Deprecation of commands from old app versions

## Decision
Add version tracking columns to `commands_generated` table:
- `appId` - Package name
- `appVersion` - Human-readable version string
- `versionCode` - Integer for efficient comparison
- `lastVerified` - Timestamp of last verification
- `isDeprecated` - Flag for pending deletion

## Consequences
**Positive:**
- Efficient command cleanup when apps are updated/uninstalled
- Better command quality (old versions marked deprecated)
- Supports pagination by package

**Negative:**
- Database migration required
- Slightly larger database size (~5 bytes per row)

## Implementation
- Migration: `3.sqm`
- Updated queries: insert, update, deleteByPackage
- New queries: getByAppVersion, markDeprecated, deleteDeprecated
```

---

## Critical Files

### To Modify:
1. `Modules/VoiceOS/core/database/src/commonMain/sqldelight/com/augmentalis/database/GeneratedCommand.sq` - **PRIMARY**
2. `Modules/VoiceOS/core/database/build.gradle.kts` - Enable migrations
3. `Modules/VoiceOS/core/database/src/commonMain/sqldelight/migrations/3.sqm` - **NEW**
4. `Modules/VoiceOS/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/IGeneratedCommandRepository.kt`
5. `Modules/VoiceOS/core/database/src/commonMain/kotlin/com/augmentalis/database/repositories/impl/SQLDelightGeneratedCommandRepository.kt`

### To Verify (No Changes):
- `GeneratedCommandDTO.kt` - Already correct
- Repository insert/update calls - Already correct

---

## Success Metrics

1. ✅ Database module compiles without errors
2. ✅ All DTO fields map to generated schema
3. ✅ LearnApp, LearnAppDev, VoiceOSCore compile successfully
4. ✅ Unit tests pass
5. ✅ Integration tests pass
6. ✅ Migration runs successfully on existing database

---

## Risk Mitigation

### Risk 1: Data Loss During Migration
**Mitigation:**
- Migration uses ALTER TABLE (preserves existing data)
- Default values ensure no NULL columns
- Test migration on sample database first

### Risk 2: Performance Impact
**Mitigation:**
- Indexes created for new columns
- Query performance tested
- Pagination by appId improves large dataset handling

### Risk 3: Breaking Changes
**Mitigation:**
- DTO defaults ensure backward compatibility
- Repository methods handle missing data gracefully
- Migration is one-way (no rollback needed if defaults work)

---

## Post-Implementation

1. Monitor app performance after schema update
2. Verify command cleanup works correctly
3. Test version-based deprecation flow
4. Update API documentation if exposed externally

---

**Estimated Total Time:** 2-3 hours
**Complexity:** Medium (schema change + migration)
**Risk Level:** Low (backward compatible with defaults)
