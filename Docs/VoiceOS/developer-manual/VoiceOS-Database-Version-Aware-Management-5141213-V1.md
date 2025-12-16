# VoiceOS Developer Manual - Chapter 40
# Version-Aware Command Lifecycle Management

**Document:** VoiceOS-40-Version-Aware-Command-Management-51214-V1
**Version:** 1.0
**Date:** 2025-12-14
**Author:** VOS4 Development Team
**Code-Reviewed-By:** CCA

---

## Table of Contents

1. [Overview](#1-overview)
2. [Problem Statement](#2-problem-statement)
3. [Database Schema v3](#3-database-schema-v3)
4. [Repository Layer](#4-repository-layer)
5. [Version Detection Service](#5-version-detection-service)
6. [Migration Guide](#6-migration-guide)
7. [Usage Examples](#7-usage-examples)
8. [Best Practices](#8-best-practices)
9. [Testing](#9-testing)
10. [Performance Considerations](#10-performance-considerations)

---

## 1. Overview

### 1.1 Purpose

Version-Aware Command Lifecycle Management prevents command accumulation across app updates by tracking app versions, marking deprecated commands, and implementing intelligent cleanup strategies.

### 1.2 Key Features

- **Version Tracking**: Track app version for each command
- **Automatic Deprecation**: Mark old commands when app updates
- **Grace Period**: 30-day grace period before deletion
- **User Preservation**: Preserve user-approved commands
- **Smart Cleanup**: Background cleanup of outdated commands
- **API Compatibility**: Supports Android API 21-34

### 1.3 Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    App Update Detected                       │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│              AppVersionDetector (Phase 3)                    │
│  • Detect version change using PackageManager               │
│  • Compare with stored version in database                  │
│  • Return VersionChange (Updated/FirstInstall/etc.)         │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│            AppVersionManager (Phase 3)                       │
│  • handleVersionUpdate() - Mark old commands deprecated     │
│  • verifyCommand() - Update version if element exists       │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│          Repository Layer (Phase 2)                          │
│  • markVersionDeprecated() - Bulk deprecation               │
│  • updateCommandVersion() - Individual updates              │
│  • deleteDeprecatedCommands() - Cleanup old commands        │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│           Database Schema v3 (Phase 1)                       │
│  • appVersion (TEXT) - Version string                       │
│  • versionCode (INTEGER) - Version for comparison           │
│  • lastVerified (INTEGER) - Last verification timestamp     │
│  • isDeprecated (INTEGER) - Deprecation flag                │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Problem Statement

### 2.1 Command Accumulation Issue

**Before Version-Aware Management:**

```
Gmail v100 (150 commands) → v105 (170 commands) → v110 (190 commands)
                           ↓
Database: 150 + 170 + 190 = 510 commands (66% bloat!)
```

**Realistic Command Count:**
- Gmail: ~150 commands
- Chrome: ~120 commands
- Settings: ~80 commands

**Problem:** Apps accumulate commands across updates, creating:
- Database bloat (5000+ commands instead of 150)
- Slower queries
- Stale commands pointing to removed UI elements
- User confusion (outdated commands still available)

### 2.2 Solution

**After Version-Aware Management:**

```
Gmail v100 (150 commands) → UPDATE → v105
                           ↓
1. Mark v100 commands as deprecated
2. Scan new v105 UI (170 new elements, 140 unchanged)
3. Verify 140 unchanged → update to v105
4. 30 deprecated commands (removed from UI)
5. After 30 days → cleanup 30 deprecated
                           ↓
Database: 170 commands (accurate!)
```

---

## 3. Database Schema v3

### 3.1 Schema Changes

**Migration:** v2 → v3

```sql
-- Add version tracking columns
ALTER TABLE commands_generated ADD COLUMN appVersion TEXT NOT NULL DEFAULT '';
ALTER TABLE commands_generated ADD COLUMN versionCode INTEGER NOT NULL DEFAULT 0;
ALTER TABLE commands_generated ADD COLUMN lastVerified INTEGER;
ALTER TABLE commands_generated ADD COLUMN isDeprecated INTEGER NOT NULL DEFAULT 0;

-- Create indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_gc_app_version
ON commands_generated(appId, versionCode, isDeprecated);

CREATE INDEX IF NOT EXISTS idx_gc_last_verified
ON commands_generated(lastVerified, isDeprecated);
```

### 3.2 Column Descriptions

| Column | Type | Description | Example |
|--------|------|-------------|---------|
| `appVersion` | TEXT | Human-readable version | "8.2024.11.123" |
| `versionCode` | INTEGER | Integer for comparison | 82024 |
| `lastVerified` | INTEGER | Last seen timestamp (ms) | 1702502400000 |
| `isDeprecated` | INTEGER | 0=active, 1=deprecated | 0 |

### 3.3 Complete Schema

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
    appId TEXT NOT NULL DEFAULT '',

    -- Version tracking (Schema v3)
    appVersion TEXT NOT NULL DEFAULT '',
    versionCode INTEGER NOT NULL DEFAULT 0,
    lastVerified INTEGER,
    isDeprecated INTEGER NOT NULL DEFAULT 0,

    UNIQUE(elementHash, commandText)
);

-- Indexes
CREATE INDEX idx_gc_element ON commands_generated(elementHash);
CREATE INDEX idx_gc_action ON commands_generated(actionType);
CREATE INDEX idx_gc_confidence ON commands_generated(confidence);
CREATE INDEX idx_gc_app_id ON commands_generated(appId, id);
CREATE INDEX idx_gc_app_version ON commands_generated(appId, versionCode, isDeprecated);
CREATE INDEX idx_gc_last_verified ON commands_generated(lastVerified, isDeprecated);
```

### 3.4 New Queries

```sql
-- Mark all commands for a version as deprecated
markVersionDeprecated:
UPDATE commands_generated
SET isDeprecated = 1
WHERE appId = ? AND versionCode = ?;

-- Update command version after verification
updateCommandVersion:
UPDATE commands_generated
SET versionCode = ?, appVersion = ?, lastVerified = ?, isDeprecated = ?
WHERE id = ?;

-- Delete old deprecated commands
deleteDeprecatedCommands:
DELETE FROM commands_generated
WHERE isDeprecated = 1
  AND lastVerified < ?
  AND (? = 0 OR isUserApproved = 0);

-- Get active commands for current version
getActiveCommands:
SELECT * FROM commands_generated
WHERE appId = ? AND versionCode = ? AND isDeprecated = 0
ORDER BY usageCount DESC, id ASC
LIMIT ?;
```

---

## 4. Repository Layer

### 4.1 Interface Methods

**File:** `IGeneratedCommandRepository.kt`

```kotlin
interface IGeneratedCommandRepository {
    // ... existing methods ...

    // Version Management Methods (Schema v3)

    suspend fun markVersionDeprecated(packageName: String, versionCode: Long): Int

    suspend fun updateCommandVersion(
        id: Long,
        versionCode: Long,
        appVersion: String,
        lastVerified: Long,
        isDeprecated: Long
    )

    suspend fun updateCommandDeprecated(id: Long, isDeprecated: Long)

    suspend fun deleteDeprecatedCommands(
        olderThan: Long,
        keepUserApproved: Boolean
    ): Int

    suspend fun getDeprecatedCommands(packageName: String): List<GeneratedCommandDTO>

    suspend fun getActiveCommands(
        packageName: String,
        versionCode: Long,
        limit: Int
    ): List<GeneratedCommandDTO>
}
```

### 4.2 Implementation

**File:** `SQLDelightGeneratedCommandRepository.kt`

All methods use:
- `Dispatchers.Default` (KMP-compatible, not `Dispatchers.IO`)
- Input validation with clear error messages
- Proper transaction handling
- Logging for debugging

**Example:**

```kotlin
override suspend fun markVersionDeprecated(
    packageName: String,
    versionCode: Long
): Int = withContext(Dispatchers.Default) {
    require(packageName.isNotEmpty()) { "Package name cannot be empty" }
    require(versionCode >= 0) { "Version code must be non-negative" }

    var rowsAffected = 0
    database.transaction {
        queries.markVersionDeprecated(appId = packageName, versionCode = versionCode)
        rowsAffected = queries.getDeprecatedCommands(packageName).executeAsList().size
    }
    rowsAffected
}
```

---

## 5. Version Detection Service

### 5.1 AppVersion Data Class

**File:** `com.augmentalis.voiceoscore.version.AppVersion`

```kotlin
data class AppVersion(
    val versionName: String,  // "8.2024.11.123"
    val versionCode: Long     // 82024
) {
    override fun toString(): String = "$versionName ($versionCode)"

    fun isNewerThan(other: AppVersion): Boolean = versionCode > other.versionCode
    fun isOlderThan(other: AppVersion): Boolean = versionCode < other.versionCode
    fun isSameAs(other: AppVersion): Boolean = versionCode == other.versionCode
    fun isValid(): Boolean = this != NOT_INSTALLED && this != UNKNOWN

    companion object {
        val NOT_INSTALLED = AppVersion("NOT_INSTALLED", -1L)
        val UNKNOWN = AppVersion("UNKNOWN", 0L)
    }
}
```

### 5.2 VersionChange Sealed Class

**File:** `com.augmentalis.voiceoscore.version.VersionChange`

```kotlin
sealed class VersionChange {
    data class FirstInstall(val packageName: String, val current: AppVersion)
    data class Updated(val packageName: String, val previous: AppVersion, val current: AppVersion)
    data class Downgraded(val packageName: String, val previous: AppVersion, val current: AppVersion)
    data class NoChange(val packageName: String, val version: AppVersion)
    data class AppNotInstalled(val packageName: String)

    fun requiresVerification(): Boolean = this is Updated || this is Downgraded
    fun requiresCleanup(): Boolean = this is AppNotInstalled
}
```

### 5.3 AppVersionDetector

**File:** `com.augmentalis.voiceoscore.version.AppVersionDetector`

```kotlin
class AppVersionDetector(
    private val context: Context,
    private val repository: IGeneratedCommandRepository
) {
    fun getCurrentVersion(packageName: String): AppVersion
    suspend fun detectVersionChange(packageName: String): VersionChange
    fun isAppInstalled(packageName: String): Boolean
    fun getCurrentVersions(packageNames: List<String>): Map<String, AppVersion>
}
```

**API Level Compatibility:**

```kotlin
@Suppress("DEPRECATION")
private fun getVersionCode(packageInfo: PackageInfo): Long {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // API 28+ - Use getLongVersionCode()
        packageInfo.longVersionCode
    } else {
        // API < 28 - Use deprecated versionCode (Int) and convert to Long
        packageInfo.versionCode.toLong()
    }
}
```

**Supported API Levels:** Android API 21-34 (Android 5.0 - Android 14)

---

## 6. Migration Guide

### 6.1 Automatic Migration

Migration from Schema v2 to v3 is **automatic** on first app launch after update.

**Migration File:** `migrations/2.sqm`

**Process:**

1. **Database Version Check**: On app start, check current schema version
2. **Apply Migration**: If version < 3, apply migration SQL
3. **Verify Schema**: Confirm all columns and indexes created
4. **Preserve Data**: All existing commands remain intact with default values

### 6.2 Default Values for Existing Data

When migration runs, existing commands get these defaults:

```kotlin
appVersion = ""           // Empty string
versionCode = 0          // Zero
lastVerified = null      // NULL
isDeprecated = 0         // Active (not deprecated)
```

### 6.3 Migration Testing

**Test File:** `MigrationV2ToV3Test.kt` (10 tests)

```kotlin
// Test 1: Migration adds all 4 columns
@Test
fun testMigration_v2ToV3_addsAllColumns()

// Test 2: Migration creates indexes
@Test
fun testMigration_v2ToV3_createsAppVersionIndex()

// Test 3: Migration preserves existing data
@Test
fun testMigration_v2ToV3_preservesExistingData()

// Test 4: Migration applies correct defaults
@Test
fun testMigration_v2ToV3_appliesCorrectDefaults()

// Test 5: Migration is idempotent (can run multiple times)
@Test
fun testMigration_v2ToV3_isIdempotent()
```

---

## 7. Usage Examples

### 7.1 Detecting Version Changes

```kotlin
val detector = AppVersionDetector(context, repository)

when (val change = detector.detectVersionChange("com.google.android.gm")) {
    is VersionChange.FirstInstall -> {
        // App newly installed - ready for initial scraping
        logger.info("New app: ${change.current}")
        startInitialScraping(change.packageName, change.current)
    }

    is VersionChange.Updated -> {
        // App updated - mark old commands for verification
        logger.info("Updated: ${change.previous} → ${change.current}")

        // Mark all old version commands as deprecated
        val deprecatedCount = repository.markVersionDeprecated(
            change.packageName,
            change.previous.versionCode
        )
        logger.info("Marked $deprecatedCount commands as deprecated")

        // Start re-scraping with new version
        startVersionedScraping(change.packageName, change.current)
    }

    is VersionChange.NoChange -> {
        // Same version - no action needed
        logger.debug("No change: ${change.version}")
    }

    is VersionChange.Downgraded -> {
        // User downgraded (rare) - treat as update
        logger.warn("Downgrade: ${change.previous} → ${change.current}")
        handleDowngrade(change)
    }

    is VersionChange.AppNotInstalled -> {
        // App uninstalled - cleanup all commands
        logger.info("App not installed: ${change.packageName}")
        deleteAllCommandsForApp(change.packageName)
    }
}
```

### 7.2 Verifying Commands on Re-Scan

```kotlin
// During UI scraping, verify each element
suspend fun processScrapedElement(
    element: ScrapedElement,
    currentVersion: AppVersion
) {
    // Check if command exists for this element
    val existingCommands = repository.getByElement(element.hash)

    for (command in existingCommands) {
        if (command.isDeprecated == 1L) {
            // Element still exists - un-deprecate and update version
            repository.updateCommandVersion(
                id = command.id,
                versionCode = currentVersion.versionCode,
                appVersion = currentVersion.versionName,
                lastVerified = System.currentTimeMillis(),
                isDeprecated = 0L  // Mark as active
            )
            logger.debug("Verified: ${command.commandText} (v${currentVersion.versionCode})")
        } else if (command.versionCode < currentVersion.versionCode) {
            // Update version for active command
            repository.updateCommandVersion(
                id = command.id,
                versionCode = currentVersion.versionCode,
                appVersion = currentVersion.versionName,
                lastVerified = System.currentTimeMillis(),
                isDeprecated = 0L
            )
        }
    }
}
```

### 7.3 Cleanup Old Commands

```kotlin
// Run periodic cleanup (e.g., daily via WorkManager)
suspend fun cleanupDeprecatedCommands() {
    val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)

    // Delete deprecated commands older than 30 days, but keep user-approved
    val deletedCount = repository.deleteDeprecatedCommands(
        olderThan = thirtyDaysAgo,
        keepUserApproved = true  // Preserve user-approved commands
    )

    logger.info("Cleaned up $deletedCount deprecated commands (older than 30 days)")
}
```

### 7.4 Getting Active Commands for Execution

```kotlin
// When executing voice commands, only use active commands for current version
suspend fun executeVoiceCommand(
    commandText: String,
    packageName: String
) {
    // Get current app version
    val currentVersion = detector.getCurrentVersion(packageName)
    if (!currentVersion.isValid()) {
        logger.error("App not installed: $packageName")
        return
    }

    // Get active commands for current version only
    val activeCommands = repository.getActiveCommands(
        packageName = packageName,
        versionCode = currentVersion.versionCode,
        limit = 100
    )

    // Find matching command
    val command = activeCommands.find { it.commandText == commandText }
    if (command != null) {
        executeCommand(command)
    } else {
        logger.warn("Command not found or deprecated: $commandText")
    }
}
```

---

## 8. Best Practices

### 8.1 Version Detection

✅ **DO:**
- Detect version changes on app launch
- Check version before UI scraping
- Log all version changes for debugging
- Handle all VersionChange variants

❌ **DON'T:**
- Skip version detection to save time
- Assume version never decreases (downgrades happen)
- Ignore AppNotInstalled case

### 8.2 Command Verification

✅ **DO:**
- Update `lastVerified` timestamp on every scrape
- Un-deprecate commands when element still exists
- Update `versionCode` to current version
- Preserve `usageCount` and `isUserApproved`

❌ **DON'T:**
- Delete commands immediately on version change
- Ignore verification during re-scraping
- Reset usage statistics

### 8.3 Cleanup Strategy

✅ **DO:**
- Use 30-day grace period minimum
- Preserve user-approved commands
- Run cleanup in background (WorkManager)
- Log cleanup statistics for monitoring

❌ **DON'T:**
- Delete deprecated commands immediately
- Skip grace period
- Delete user-approved commands without warning
- Run cleanup on UI thread

### 8.4 Performance

✅ **DO:**
- Use `getActiveCommands()` with LIMIT for command execution
- Use indexes for version-based queries
- Batch verification updates in transactions
- Use `Dispatchers.Default` for database operations (KMP-compatible)

❌ **DON'T:**
- Load all commands when only need active ones
- Skip transaction batching for bulk updates
- Use `Dispatchers.IO` (not KMP-compatible)
- Query deprecated commands for execution

---

## 9. Testing

### 9.1 Test Coverage

**Total Tests:** 25

1. **Migration Tests** (10 tests) - `MigrationV2ToV3Test.kt`
   - Column addition
   - Index creation
   - Data preservation
   - Default values
   - Idempotency

2. **Repository Tests** (15 tests) - `VersionManagementRepositoryTest.kt`
   - `markVersionDeprecated()` (3 tests)
   - `updateCommandVersion()` (2 tests)
   - `updateCommandDeprecated()` (2 tests)
   - `deleteDeprecatedCommands()` (4 tests)
   - `getDeprecatedCommands()` (2 tests)
   - `getActiveCommands()` (2 tests)

### 9.2 Running Tests

**Prerequisites:**
- Android device or emulator connected
- ADB enabled

**Commands:**

```bash
# Run all database tests
./gradlew :Modules:VoiceOS:core:database:connectedAndroidTest

# Run only migration tests
./gradlew :Modules:VoiceOS:core:database:connectedAndroidTest \
  --tests "com.augmentalis.database.MigrationV2ToV3Test"

# Run only version management tests
./gradlew :Modules:VoiceOS:core:database:connectedAndroidTest \
  --tests "com.augmentalis.database.VersionManagementRepositoryTest"
```

### 9.3 Test Scenarios

**Scenario 1: Gmail Update v100 → v200**

```kotlin
@Test
fun testGmailUpdate_marksOldCommandsDeprecated() = runBlocking {
    // Insert 50 Gmail v100 commands
    repeat(50) { i ->
        repository.insert(createCommand("gmail_$i", versionCode = 100L))
    }

    // Simulate app update
    val deprecatedCount = repository.markVersionDeprecated("com.google.android.gm", 100L)

    // Verify
    assertEquals(50, deprecatedCount)
}
```

**Scenario 2: Grace Period Cleanup**

```kotlin
@Test
fun testCleanup_respectsGracePeriod() = runBlocking {
    val now = System.currentTimeMillis()
    val oldTimestamp = now - (45L * 24 * 60 * 60 * 1000)  // 45 days ago
    val recentTimestamp = now - (15L * 24 * 60 * 60 * 1000)  // 15 days ago

    // Insert old deprecated command
    repository.insert(createCommand("old", isDeprecated = 1L)
        .copy(lastVerified = oldTimestamp))

    // Insert recent deprecated command
    repository.insert(createCommand("recent", isDeprecated = 1L)
        .copy(lastVerified = recentTimestamp))

    // Cleanup (30-day threshold)
    val deletedCount = repository.deleteDeprecatedCommands(
        olderThan = now - (30L * 24 * 60 * 60 * 1000),
        keepUserApproved = false
    )

    // Verify: Only old command deleted
    assertEquals(1, deletedCount)
    assertEquals(1, repository.count())
}
```

---

## 10. Performance Considerations

### 10.1 Query Performance

**Index Usage:**

```sql
-- Fast: Uses idx_gc_app_version
SELECT * FROM commands_generated
WHERE appId = 'com.google.android.gm'
  AND versionCode = 200
  AND isDeprecated = 0
LIMIT 100;

-- Fast: Uses idx_gc_last_verified
DELETE FROM commands_generated
WHERE isDeprecated = 1
  AND lastVerified < 1702502400000;
```

### 10.2 Bulk Operations

**Use Transactions:**

```kotlin
// ✅ GOOD: Single transaction
database.transaction {
    commands.forEach { command ->
        repository.updateCommandVersion(command.id, newVersion, ...)
    }
}

// ❌ BAD: Individual transactions (slow)
commands.forEach { command ->
    repository.updateCommandVersion(command.id, newVersion, ...)
}
```

### 10.3 Memory Usage

**Pagination for Large Datasets:**

```kotlin
// ✅ GOOD: Limited query
val activeCommands = repository.getActiveCommands(
    packageName,
    versionCode,
    limit = 100  // Only get top 100
)

// ❌ BAD: Load all commands
val allCommands = repository.getAll()  // Could be 10,000+ commands!
```

### 10.4 Background Processing

**Use WorkManager for Cleanup:**

```kotlin
class CleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repository = getRepository()
        val deletedCount = repository.deleteDeprecatedCommands(
            olderThan = System.currentTimeMillis() - THIRTY_DAYS_MS,
            keepUserApproved = true
        )
        Log.i(TAG, "Cleanup: Deleted $deletedCount commands")
        return Result.success()
    }
}

// Schedule daily cleanup
val cleanupRequest = PeriodicWorkRequestBuilder<CleanupWorker>(1, TimeUnit.DAYS)
    .build()
WorkManager.getInstance(context).enqueue(cleanupRequest)
```

---

## Summary

Version-Aware Command Lifecycle Management provides:

✅ **Automatic command lifecycle tracking** across app updates
✅ **Intelligent cleanup** with grace period and user preservation
✅ **Zero manual intervention** - fully automatic
✅ **Database efficiency** - prevents command bloat
✅ **API compatibility** - Android API 21-34
✅ **KMP-ready** - Database code in `commonMain`
✅ **Comprehensive testing** - 25 tests covering all scenarios

**Key Metrics:**
- **Database Bloat Reduction**: 66% (from 510 to 170 commands for Gmail)
- **Grace Period**: 30 days minimum
- **User Command Preservation**: 100% (user-approved never deleted)
- **Test Coverage**: 25 tests (migration + repository)

---

**End of Chapter 40**
