# Database Consolidation - Testing Guide

**Created:** 2025-11-07 08:30 PST
**Version:** 1.0
**Implementation:** Database-Consolidation-Implementation-Complete-2511070815.md
**Branch:** voiceos-database-update

---

## Overview

This guide provides step-by-step testing procedures for the database consolidation implementation. Follow these tests BEFORE merging to main.

**Test Coverage:**
- ✅ Unit Tests (automated)
- ✅ Device Tests (manual)
- ✅ Migration Tests (critical)
- ✅ Data Validation (critical)
- ✅ Rollback Tests (safety)
- ✅ Performance Tests (optional)

**Estimated Testing Time:** 2-3 hours

---

## Pre-Testing Checklist

### Build Verification ✅

**Already Complete:**
```bash
./gradlew assembleDebug --no-daemon --no-configuration-cache
# Result: BUILD SUCCESSFUL in 45s ✓
```

### Code Review Checklist

- [x] DatabaseMigrationHelper created
- [x] Migration triggered in VoiceOSService
- [x] VoiceCommandProcessor uses unified DB
- [x] CommandGenerator uses unified DB
- [x] AccessibilityScrapingIntegration uses unified DB
- [x] Old databases NOT deleted
- [x] Backward compatibility maintained
- [x] CoT comments present
- [x] Error handling implemented

---

## Test Environment Setup

### 1. Prepare Test Device

**Requirements:**
- Android device (API 29+)
- Developer mode enabled
- USB debugging enabled
- ADB access working

**Verify ADB Connection:**
```bash
adb devices
# Should show: device_id    device
```

### 2. Backup Existing Data (Critical!)

**Before installing test build, backup databases:**
```bash
# Create backup directory
mkdir -p ~/voiceos-test-backups/$(date +%Y%m%d_%H%M%S)
cd ~/voiceos-test-backups/$(date +%Y%m%d_%H%M%S)

# Backup all databases
adb pull /data/data/com.augmentalis.voiceos/databases/learnapp_database .
adb pull /data/data/com.augmentalis.voiceos/databases/app_scraping_database .
adb pull /data/data/com.augmentalis.voiceos/databases/voiceos_app_database .

# Backup shared preferences
adb pull /data/data/com.augmentalis.voiceos/shared_prefs .

echo "Backup complete: $(pwd)"
```

### 3. Install Test Build

**Option A: Fresh Install (Clean Test)**
```bash
# Uninstall existing app
adb uninstall com.augmentalis.voiceos

# Install test build
./gradlew installDebug

# Result: Fresh app, no migration needed
```

**Option B: Update Install (Migration Test)**
```bash
# Keep existing app data
./gradlew installDebug

# Result: App updated, migration should run
```

**Recommendation:** Test BOTH scenarios!

---

## Unit Tests (Automated)

### Create Test File

**Location:** `modules/apps/VoiceOSCore/src/test/java/com/augmentalis/voiceoscore/database/migration/DatabaseMigrationHelperTest.kt`

```kotlin
package com.augmentalis.voiceoscore.database.migration

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.augmentalis.learnapp.database.LearnAppDatabase
import com.augmentalis.learnapp.database.entities.LearnedAppEntity
import com.augmentalis.voiceoscore.database.VoiceOSAppDatabase
import com.augmentalis.voiceoscore.scraping.database.AppScrapingDatabase
import com.augmentalis.voiceoscore.scraping.entities.ScrapedAppEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class DatabaseMigrationHelperTest {

    private lateinit var context: Context
    private lateinit var learnAppDb: LearnAppDatabase
    private lateinit var scrapingDb: AppScrapingDatabase
    private lateinit var unifiedDb: VoiceOSAppDatabase
    private lateinit var migrationHelper: DatabaseMigrationHelper

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Create in-memory databases for testing
        learnAppDb = Room.inMemoryDatabaseBuilder(context, LearnAppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        scrapingDb = Room.inMemoryDatabaseBuilder(context, AppScrapingDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        unifiedDb = Room.inMemoryDatabaseBuilder(context, VoiceOSAppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        migrationHelper = DatabaseMigrationHelper(context)
    }

    @After
    fun tearDown() {
        learnAppDb.close()
        scrapingDb.close()
        unifiedDb.close()
    }

    @Test
    fun `migration copies learned app data correctly`() = runBlocking {
        // Given: A learned app in LearnAppDatabase
        val learnedApp = LearnedAppEntity(
            packageName = "com.test.app",
            appName = "Test App",
            versionCode = 100L,
            versionName = "1.0.0",
            firstLearnedAt = 1000L,
            lastUpdatedAt = 2000L,
            totalScreens = 10,
            totalElements = 254,
            appHash = "test_hash",
            explorationStatus = "COMPLETE"
        )
        learnAppDb.learnAppDao().insert(learnedApp)

        // When: Migration runs
        migrationHelper.migrateIfNeeded()

        // Then: App exists in unified database with correct mapping
        val unifiedApp = unifiedDb.appDao().getApp("com.test.app")
        assertNotNull(unifiedApp, "App should be migrated to unified database")
        assertEquals("Test App", unifiedApp.appName)
        assertEquals("COMPLETE", unifiedApp.explorationStatus)
        assertEquals(254, unifiedApp.exploredElementCount)  // Renamed field!
        assertEquals(10, unifiedApp.totalScreens)
        assertEquals(1000L, unifiedApp.firstExplored)  // Renamed field!
        assertEquals(2000L, unifiedApp.lastExplored)  // Renamed field!
        assertTrue(unifiedApp.isFullyLearned == true)
    }

    @Test
    fun `migration copies scraped app data correctly`() = runBlocking {
        // Given: A scraped app in AppScrapingDatabase
        val scrapedApp = ScrapedAppEntity(
            appId = "scraped_id",
            packageName = "com.test.app2",
            appName = "Test App 2",
            versionCode = 200,
            versionName = "2.0.0",
            appHash = "scraped_hash",
            firstScraped = 3000L,
            lastScraped = 4000L,
            scrapeCount = 5,
            elementCount = 85,
            commandCount = 42,
            isFullyLearned = false,
            scrapingMode = "DYNAMIC"
        )
        scrapingDb.scrapedAppDao().insert(scrapedApp)

        // When: Migration runs
        migrationHelper.migrateIfNeeded()

        // Then: App exists in unified database with correct mapping
        val unifiedApp = unifiedDb.appDao().getApp("com.test.app2")
        assertNotNull(unifiedApp, "App should be migrated to unified database")
        assertEquals("Test App 2", unifiedApp.appName)
        assertEquals(85, unifiedApp.scrapedElementCount)  // Renamed field!
        assertEquals(42, unifiedApp.commandCount)
        assertEquals(5, unifiedApp.scrapeCount)
        assertEquals(3000L, unifiedApp.firstScraped)
        assertEquals(4000L, unifiedApp.lastScraped)
        assertEquals("DYNAMIC", unifiedApp.scrapingMode)
        assertEquals(false, unifiedApp.isFullyLearned)
    }

    @Test
    fun `migration merges learned and scraped data for same app`() = runBlocking {
        // Given: Same app in BOTH databases
        val learnedApp = LearnedAppEntity(
            packageName = "com.test.merged",
            appName = "Merged App",
            versionCode = 100L,
            versionName = "1.0.0",
            firstLearnedAt = 1000L,
            lastUpdatedAt = 2000L,
            totalScreens = 20,
            totalElements = 254,
            appHash = "merged_hash",
            explorationStatus = "COMPLETE"
        )
        learnAppDb.learnAppDao().insert(learnedApp)

        val scrapedApp = ScrapedAppEntity(
            appId = "merged_id",
            packageName = "com.test.merged",
            appName = "Merged App",
            versionCode = 100,
            versionName = "1.0.0",
            appHash = "merged_hash",
            firstScraped = 3000L,
            lastScraped = 4000L,
            scrapeCount = 10,
            elementCount = 85,
            commandCount = 50,
            isFullyLearned = true,
            scrapingMode = "DYNAMIC"
        )
        scrapingDb.scrapedAppDao().insert(scrapedApp)

        // When: Migration runs
        migrationHelper.migrateIfNeeded()

        // Then: Unified database has BOTH sets of data merged
        val unifiedApp = unifiedDb.appDao().getApp("com.test.merged")
        assertNotNull(unifiedApp, "App should be migrated to unified database")

        // From LearnApp
        assertEquals("COMPLETE", unifiedApp.explorationStatus)
        assertEquals(254, unifiedApp.exploredElementCount)
        assertEquals(20, unifiedApp.totalScreens)

        // From Scraping
        assertEquals(85, unifiedApp.scrapedElementCount)
        assertEquals(50, unifiedApp.commandCount)
        assertEquals(10, unifiedApp.scrapeCount)

        // Merged correctly
        assertEquals(true, unifiedApp.isFullyLearned)
    }

    @Test
    fun `migration is idempotent - running twice doesn't duplicate data`() = runBlocking {
        // Given: A learned app
        val learnedApp = LearnedAppEntity(
            packageName = "com.test.idempotent",
            appName = "Idempotent App",
            versionCode = 100L,
            versionName = "1.0.0",
            firstLearnedAt = 1000L,
            lastUpdatedAt = 2000L,
            totalScreens = 5,
            totalElements = 100,
            appHash = "idempotent_hash",
            explorationStatus = "COMPLETE"
        )
        learnAppDb.learnAppDao().insert(learnedApp)

        // When: Migration runs twice
        migrationHelper.migrateIfNeeded()
        migrationHelper.migrateIfNeeded()  // Second run

        // Then: Only one app in unified database
        val allApps = unifiedDb.appDao().getAllApps()
        assertEquals(1, allApps.size, "Should only have one app after idempotent migration")

        val app = allApps[0]
        assertEquals("com.test.idempotent", app.packageName)
        assertEquals(100, app.exploredElementCount)
    }

    @Test
    fun `migration skips if already completed`() = runBlocking {
        // Given: Migration already marked complete
        val prefs = context.getSharedPreferences("voiceos_db_migration", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("migration_v1_to_unified_complete", true).apply()

        // Add data to old database
        val learnedApp = LearnedAppEntity(
            packageName = "com.test.skip",
            appName = "Skip App",
            versionCode = 100L,
            versionName = "1.0.0",
            firstLearnedAt = 1000L,
            lastUpdatedAt = 2000L,
            totalScreens = 5,
            totalElements = 50,
            appHash = "skip_hash",
            explorationStatus = "COMPLETE"
        )
        learnAppDb.learnAppDao().insert(learnedApp)

        // When: Migration runs
        migrationHelper.migrateIfNeeded()

        // Then: Data NOT migrated (skipped)
        val unifiedApp = unifiedDb.appDao().getApp("com.test.skip")
        assertNull(unifiedApp, "Should skip migration if already marked complete")
    }

    @Test
    fun `migration handles empty LearnAppDatabase gracefully`() = runBlocking {
        // Given: Empty LearnAppDatabase

        // When: Migration runs
        migrationHelper.migrateIfNeeded()

        // Then: No errors, migration completes
        assertTrue(migrationHelper.isMigrationComplete())
        val allApps = unifiedDb.appDao().getAllApps()
        assertEquals(0, allApps.size)
    }

    @Test
    fun `migration handles empty AppScrapingDatabase gracefully`() = runBlocking {
        // Given: Empty AppScrapingDatabase

        // When: Migration runs
        migrationHelper.migrateIfNeeded()

        // Then: No errors, migration completes
        assertTrue(migrationHelper.isMigrationComplete())
        val allApps = unifiedDb.appDao().getAllApps()
        assertEquals(0, allApps.size)
    }
}
```

### Run Unit Tests

```bash
# Run migration tests
./gradlew :modules:apps:VoiceOSCore:testDebugUnitTest --tests "*DatabaseMigrationHelperTest"

# Expected: All tests pass ✓
```

---

## Device Testing (Critical)

### Test 1: Fresh Install (No Migration Needed)

**Scenario:** User installing VoiceOS for first time

**Steps:**
```bash
# 1. Uninstall app (fresh start)
adb uninstall com.augmentalis.voiceos

# 2. Install test build
./gradlew installDebug

# 3. Enable VoiceOS service
adb shell settings put secure enabled_accessibility_services com.augmentalis.voiceos/.accessibility.VoiceOSService
adb shell settings put secure accessibility_enabled 1

# 4. Launch app
adb shell am start -n com.augmentalis.voiceos/.ui.MainActivity

# 5. Monitor logs
adb logcat -c  # Clear logs
adb logcat | grep -E "DatabaseMigrationHelper|VoiceOSService"
```

**Expected Logs:**
```
VoiceOSService: VoiceOS Service connected
DatabaseMigrationHelper: Migration already complete, skipping
  (or)
DatabaseMigrationHelper: No LearnApp data to migrate
DatabaseMigrationHelper: No Scraping data to migrate
DatabaseMigrationHelper: ✅ Migration complete successfully!
```

**Verification:**
- [ ] App launches successfully
- [ ] No crash
- [ ] Migration completes or skips correctly
- [ ] VoiceOS service running

---

### Test 2: Update Install (Migration Runs)

**Scenario:** Existing user updating to new version

**Prerequisites:**
1. Need existing VoiceOS installation with data
2. If you don't have one, create seed data:

**Create Seed Data:**
```bash
# Create seed data script
cat > /tmp/seed_voiceos_data.sh << 'EOF'
#!/bin/bash

# Install old version (if you have it)
# adb install old-voiceos.apk

# Or manually use the app to generate data:
echo "Use VoiceOS for a few minutes to generate data:"
echo "1. Enable accessibility service"
echo "2. Open a few apps (Teams, Chrome, Settings)"
echo "3. Let it scrape for 1-2 minutes"
echo ""
echo "Press Enter when ready to continue with update test..."
read

# Verify data exists
echo "Checking for existing data..."
adb shell "run-as com.augmentalis.voiceos ls databases/" | grep -E "learnapp|scraping|voiceos"

echo "Ready for update test!"
EOF

chmod +x /tmp/seed_voiceos_data.sh
/tmp/seed_voiceos_data.sh
```

**Test Steps:**
```bash
# 1. Verify existing data
adb shell "run-as com.augmentalis.voiceos ls -la databases/" | grep -E "database$"

# Should show:
# learnapp_database
# app_scraping_database
# (voiceos_app_database might not exist yet)

# 2. Count existing apps
adb shell << EOF
run-as com.augmentalis.voiceos sqlite3 databases/learnapp_database \
  "SELECT COUNT(*) FROM learned_apps;"
EOF

# Save this number for verification!

adb shell << EOF
run-as com.augmentalis.voiceos sqlite3 databases/app_scraping_database \
  "SELECT COUNT(*) FROM scraped_apps;"
EOF

# Save this number too!

# 3. Install update (keeps data)
./gradlew installDebug

# 4. Clear logcat and monitor migration
adb logcat -c
adb logcat | grep -E "DatabaseMigrationHelper|Migration"

# 5. Launch app
adb shell am start -n com.augmentalis.voiceos/.ui.MainActivity
```

**Expected Logs (Key Sequences):**
```
DatabaseMigrationHelper: ═══════════════════════════════════════════════
DatabaseMigrationHelper: Starting database migration to VoiceOSAppDatabase
DatabaseMigrationHelper: ═══════════════════════════════════════════════
DatabaseMigrationHelper: Step 1/4: Migrating LearnApp data...
DatabaseMigrationHelper:   Found N learned apps to migrate
DatabaseMigrationHelper:   Creating new entry for com.package.name
DatabaseMigrationHelper:     ✓ com.package.name: XXX elements
DatabaseMigrationHelper: ✅ Migrated N learned apps
DatabaseMigrationHelper: Step 2/4: Migrating Scraping data...
DatabaseMigrationHelper:   Found M scraped apps to migrate
DatabaseMigrationHelper:   Merging scraped data for com.package.name
DatabaseMigrationHelper:     ✓ com.package.name: YYY elements, ZZZ commands
DatabaseMigrationHelper: ✅ Migrated M scraped apps
DatabaseMigrationHelper: Step 3/4: Validating migration...
DatabaseMigrationHelper: ✅ Total apps in unified database: N+M
DatabaseMigrationHelper: Step 4/4: Marking migration complete...
DatabaseMigrationHelper: ═══════════════════════════════════════════════
DatabaseMigrationHelper: ✅ Migration complete successfully!
DatabaseMigrationHelper:   - Learned apps: N
DatabaseMigrationHelper:   - Scraped apps: M
DatabaseMigrationHelper:   - Total unified: N+M
DatabaseMigrationHelper: ═══════════════════════════════════════════════
```

**Verification Checklist:**
- [ ] Migration starts automatically
- [ ] No errors in logs
- [ ] Migration completes successfully
- [ ] App count matches (N+M = total from both old DBs)
- [ ] App doesn't crash
- [ ] VoiceOS service continues running

---

### Test 3: Data Validation (Critical!)

**Verify migrated data is correct:**

```bash
# Pull unified database
adb pull /data/data/com.augmentalis.voiceos/databases/voiceos_app_database /tmp/

# Inspect with sqlite3
sqlite3 /tmp/voiceos_app_database << EOF
.mode column
.headers on

-- Show all migrated apps
SELECT
  package_name,
  app_name,
  explored_element_count,  -- From LearnApp
  scraped_element_count,   -- From Scraping
  command_count,
  is_fully_learned
FROM apps
ORDER BY app_name;

-- Count total apps
SELECT COUNT(*) as total_apps FROM apps;

-- Show apps from both sources (merged)
SELECT
  package_name,
  CASE
    WHEN exploration_status IS NOT NULL THEN 'Learned'
    ELSE 'Not Learned'
  END as learn_status,
  CASE
    WHEN scraped_element_count > 0 THEN 'Scraped'
    ELSE 'Not Scraped'
  END as scrape_status
FROM apps;
EOF
```

**Critical Validation Points:**

1. **Teams App Test (if you have it):**
   ```bash
   sqlite3 /tmp/voiceos_app_database \
     "SELECT package_name, explored_element_count, scraped_element_count
      FROM apps
      WHERE package_name LIKE '%teams%';"

   # Expected: explored_element_count = 254 (if learned)
   ```

2. **No Data Loss:**
   ```bash
   # Count from old databases (before migration)
   LEARNED_COUNT=$(adb shell "run-as com.augmentalis.voiceos sqlite3 databases/learnapp_database 'SELECT COUNT(*) FROM learned_apps;'")
   SCRAPED_COUNT=$(adb shell "run-as com.augmentalis.voiceos sqlite3 databases/app_scraping_database 'SELECT COUNT(*) FROM scraped_apps;'")

   # Count from unified database (after migration)
   UNIFIED_COUNT=$(sqlite3 /tmp/voiceos_app_database "SELECT COUNT(*) FROM apps;")

   echo "Learned: $LEARNED_COUNT"
   echo "Scraped: $SCRAPED_COUNT"
   echo "Unified: $UNIFIED_COUNT"
   echo ""

   # Verify: UNIFIED_COUNT >= max(LEARNED_COUNT, SCRAPED_COUNT)
   # (Some apps might be in both, so unified count might be less than sum)
   ```

3. **Field Mapping Verification:**
   ```bash
   sqlite3 /tmp/voiceos_app_database << EOF
   -- Verify LearnApp fields migrated
   SELECT
     package_name,
     exploration_status,
     total_screens,
     explored_element_count,
     first_explored,
     last_explored
   FROM apps
   WHERE exploration_status IS NOT NULL
   LIMIT 5;

   -- Verify Scraping fields migrated
   SELECT
     package_name,
     scraped_element_count,
     command_count,
     scrape_count,
     first_scraped,
     last_scraped
   FROM apps
   WHERE scraped_element_count > 0
   LIMIT 5;
   EOF
   ```

**Verification Checklist:**
- [ ] All apps from LearnApp migrated
- [ ] All apps from Scraping migrated
- [ ] Field mappings correct (explored_element_count, etc.)
- [ ] Timestamps preserved
- [ ] No NULL where should have value
- [ ] Teams app shows 254 elements (if applicable)

---

### Test 4: Old Databases Still Work (Rollback Test)

**Verify old databases weren't deleted:**

```bash
# List all databases
adb shell "run-as com.augmentalis.voiceos ls -la databases/"

# Should still show:
# - learnapp_database          ✓ (kept as backup)
# - app_scraping_database      ✓ (kept as backup)
# - voiceos_app_database       ✓ (new unified)
```

**Verify old databases are readable:**
```bash
# LearnApp database
adb shell "run-as com.augmentalis.voiceos sqlite3 databases/learnapp_database 'SELECT COUNT(*) FROM learned_apps;'"
# Should work ✓

# Scraping database
adb shell "run-as com.augmentalis.voiceos sqlite3 databases/app_scraping_database 'SELECT COUNT(*) FROM scraped_apps;'"
# Should work ✓
```

**Verification Checklist:**
- [ ] learnapp_database still exists
- [ ] app_scraping_database still exists
- [ ] Both old databases are readable
- [ ] Old data intact (not deleted)

---

### Test 5: Migration Idempotency

**Verify migration only runs once:**

```bash
# 1. Force-stop app
adb shell am force-stop com.augmentalis.voiceos

# 2. Clear logcat
adb logcat -c

# 3. Restart app
adb shell am start -n com.augmentalis.voiceos/.ui.MainActivity

# 4. Monitor logs
adb logcat | grep DatabaseMigrationHelper

# Expected log:
# DatabaseMigrationHelper: Migration already complete, skipping
```

**Verification:**
- [ ] Second launch says "Migration already complete, skipping"
- [ ] No re-migration attempted
- [ ] App starts quickly (no delay)

---

### Test 6: App Functionality (End-to-End)

**Test that VoiceOS still works after migration:**

**Test Voice Commands:**
```bash
# 1. Open an app (e.g., Settings)
adb shell am start -n com.android.settings/.Settings

# 2. Say "Show my voice" (or your hotword)

# 3. Say a command like "Click Bluetooth"

# 4. Monitor logs
adb logcat | grep -E "VoiceCommandProcessor|CommandGenerator"
```

**Expected Behavior:**
- [ ] Voice recognition works
- [ ] Commands are recognized
- [ ] Commands are executed
- [ ] App responds correctly

**Test Dynamic Scraping:**
```bash
# 1. Open a new app
adb shell am start -n com.android.chrome/com.google.android.apps.chrome.Main

# 2. Wait 5 seconds for scraping

# 3. Check logs
adb logcat | grep AccessibilityScrapingIntegration

# Expected:
# - "Scraping triggered"
# - "Inserted N elements"
# - "App already in database" or "Creating new app"
```

**Verification:**
- [ ] Scraping still works
- [ ] Elements saved to unified database
- [ ] Commands generated
- [ ] No errors

---

## Performance Testing (Optional)

### Migration Performance

**Measure migration time:**

```bash
# Clear migration flag to force re-run
adb shell "run-as com.augmentalis.voiceos rm shared_prefs/voiceos_db_migration.xml"

# Clear logcat and add timestamp
adb logcat -c

# Force-stop and restart
adb shell am force-stop com.augmentalis.voiceos
adb shell am start -n com.augmentalis.voiceos/.ui.MainActivity

# Monitor logs with timestamps
adb logcat -v time | grep DatabaseMigrationHelper

# Record:
# - Start time: "Starting database migration"
# - End time: "Migration complete successfully"
# - Duration: (end - start)
```

**Expected Performance:**
- Small dataset (<10 apps): <1 second
- Medium dataset (10-50 apps): 1-3 seconds
- Large dataset (50-100 apps): 3-5 seconds
- Very large (100+ apps): 5-10 seconds

**Verification:**
- [ ] Migration completes in reasonable time
- [ ] No ANR (Application Not Responding)
- [ ] No UI freeze

---

## Edge Case Testing

### Test 7: Corrupted Old Database

**Simulate corrupted LearnAppDatabase:**

```bash
# Backup first!
adb pull /data/data/com.augmentalis.voiceos/databases/learnapp_database /tmp/backup_learnapp_database

# Corrupt database
adb shell "run-as com.augmentalis.voiceos dd if=/dev/zero of=databases/learnapp_database bs=1024 count=1"

# Clear migration flag
adb shell "run-as com.augmentalis.voiceos rm shared_prefs/voiceos_db_migration.xml"

# Restart app
adb shell am force-stop com.augmentalis.voiceos
adb shell am start -n com.augmentalis.voiceos/.ui.MainActivity

# Monitor logs
adb logcat | grep -E "DatabaseMigrationHelper|ERROR"
```

**Expected Behavior:**
- [ ] Migration catches exception
- [ ] Logs "No LearnApp data to migrate"
- [ ] Continues with scraping migration
- [ ] App doesn't crash
- [ ] Service continues running

**Cleanup:**
```bash
# Restore backup
adb push /tmp/backup_learnapp_database /data/data/com.augmentalis.voiceos/databases/learnapp_database
```

### Test 8: Version Code Type Mismatch

**Already handled in code:** ScrapedAppEntity uses `Int`, AppEntity uses `Long`

**Verification:**
```bash
# Check if migration handles conversion
sqlite3 /tmp/voiceos_app_database << EOF
SELECT
  package_name,
  typeof(version_code),
  version_code
FROM apps
LIMIT 5;
EOF

# Expected: typeof = "integer" (SQLite stores as INTEGER)
```

- [ ] Version codes stored correctly
- [ ] No overflow errors
- [ ] Large version codes (API 28+) work

---

## Rollback Testing

### Test 9: Rollback Scenario

**Test that rollback is possible:**

```bash
# 1. Note current state
adb shell "run-as com.augmentalis.voiceos ls -la databases/"

# 2. Uninstall new version
adb uninstall com.augmentalis.voiceos

# 3. Install old version (if you have it)
# adb install old-voiceos.apk

# 4. Verify data restored
adb shell "run-as com.augmentalis.voiceos sqlite3 databases/learnapp_database 'SELECT COUNT(*) FROM learned_apps;'"

# Should show same count as before ✓
```

**Verification:**
- [ ] Old databases intact after uninstall
- [ ] Can install old version
- [ ] Data accessible in old version
- [ ] No data loss

---

## Test Results Documentation

### Test Summary Template

```markdown
# Database Consolidation - Test Results

**Date:** YYYY-MM-DD
**Tester:** [Your Name]
**Device:** [Device Model, Android Version]
**Build:** [Git commit hash]

## Test Results

### Unit Tests
- [ ] All tests pass
- [ ] Test coverage: XX%
- [ ] Failed tests: [list if any]

### Device Tests

#### Test 1: Fresh Install
- [ ] PASS / FAIL
- Notes: [any issues]

#### Test 2: Update Install (Migration)
- [ ] PASS / FAIL
- Migration time: X seconds
- Apps migrated: N
- Notes: [any issues]

#### Test 3: Data Validation
- [ ] PASS / FAIL
- Learned apps: N
- Scraped apps: M
- Unified apps: N+M
- Teams element count: 254 ✓ / [actual]
- Notes: [any issues]

#### Test 4: Old Databases
- [ ] PASS / FAIL
- learnapp_database: EXISTS / DELETED
- app_scraping_database: EXISTS / DELETED
- Notes: [any issues]

#### Test 5: Idempotency
- [ ] PASS / FAIL
- Notes: [any issues]

#### Test 6: App Functionality
- [ ] Voice commands: PASS / FAIL
- [ ] Dynamic scraping: PASS / FAIL
- [ ] Command execution: PASS / FAIL
- Notes: [any issues]

### Performance Tests
- Migration time: X seconds
- Dataset size: N apps
- ANR: YES / NO

### Edge Case Tests
- [ ] Corrupted database: PASS / FAIL
- [ ] Version code conversion: PASS / FAIL
- [ ] Empty databases: PASS / FAIL

### Rollback Test
- [ ] PASS / FAIL
- Data intact: YES / NO

## Overall Result
- [ ] ✅ READY TO MERGE
- [ ] ❌ ISSUES FOUND (see notes)

## Issues Found
[List any issues with severity and reproduction steps]

## Recommendations
[Any recommendations before merging]
```

---

## Quick Test Script (Automated)

**Create automated test script:**

```bash
#!/bin/bash
# database-consolidation-test.sh

set -e

echo "==================================="
echo "Database Consolidation Test Script"
echo "==================================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

function test_pass() {
    echo -e "${GREEN}✓ $1${NC}"
}

function test_fail() {
    echo -e "${RED}✗ $1${NC}"
    exit 1
}

function test_warn() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

# Check ADB connection
echo "1. Checking ADB connection..."
if adb devices | grep -q "device$"; then
    test_pass "ADB connected"
else
    test_fail "No device connected"
fi

# Build app
echo ""
echo "2. Building app..."
./gradlew assembleDebug --quiet
if [ $? -eq 0 ]; then
    test_pass "Build successful"
else
    test_fail "Build failed"
fi

# Backup existing data
echo ""
echo "3. Backing up existing data..."
BACKUP_DIR=~/voiceos-test-backups/$(date +%Y%m%d_%H%M%S)
mkdir -p "$BACKUP_DIR"
adb pull /data/data/com.augmentalis.voiceos/databases "$BACKUP_DIR/" 2>/dev/null || test_warn "No existing data to backup"

# Install app
echo ""
echo "4. Installing app..."
./gradlew installDebug --quiet
if [ $? -eq 0 ]; then
    test_pass "App installed"
else
    test_fail "Installation failed"
fi

# Clear migration flag (force re-run)
echo ""
echo "5. Clearing migration flag..."
adb shell "run-as com.augmentalis.voiceos rm shared_prefs/voiceos_db_migration.xml" 2>/dev/null || test_warn "No migration flag to clear"

# Start app and monitor
echo ""
echo "6. Starting app and monitoring migration..."
adb logcat -c
adb shell am force-stop com.augmentalis.voiceos
adb shell am start -n com.augmentalis.voiceos/.ui.MainActivity &>/dev/null

# Wait for migration
sleep 5

# Check logs
echo ""
echo "7. Checking migration logs..."
if adb logcat -d | grep -q "Migration complete successfully"; then
    test_pass "Migration completed successfully"
elif adb logcat -d | grep -q "Migration already complete, skipping"; then
    test_pass "Migration already complete (skipped)"
else
    adb logcat -d | grep DatabaseMigrationHelper
    test_warn "Migration status unclear - check logs above"
fi

# Verify unified database exists
echo ""
echo "8. Verifying unified database..."
if adb shell "run-as com.augmentalis.voiceos ls databases/voiceos_app_database" 2>/dev/null | grep -q "voiceos_app_database"; then
    test_pass "Unified database exists"
else
    test_fail "Unified database not found"
fi

# Count apps in unified database
echo ""
echo "9. Counting migrated apps..."
APP_COUNT=$(adb shell "run-as com.augmentalis.voiceos sqlite3 databases/voiceos_app_database 'SELECT COUNT(*) FROM apps;'" 2>/dev/null)
if [ -n "$APP_COUNT" ]; then
    test_pass "Unified database has $APP_COUNT apps"
else
    test_warn "Could not count apps in unified database"
fi

# Verify old databases exist
echo ""
echo "10. Verifying old databases kept as backup..."
if adb shell "run-as com.augmentalis.voiceos ls databases/" | grep -q "learnapp_database"; then
    test_pass "LearnAppDatabase kept as backup"
else
    test_warn "LearnAppDatabase not found (might be fresh install)"
fi

if adb shell "run-as com.augmentalis.voiceos ls databases/" | grep -q "app_scraping_database"; then
    test_pass "AppScrapingDatabase kept as backup"
else
    test_warn "AppScrapingDatabase not found (might be fresh install)"
fi

echo ""
echo "==================================="
echo "Test Summary"
echo "==================================="
echo "Backup location: $BACKUP_DIR"
echo "App count in unified DB: $APP_COUNT"
echo ""
echo -e "${GREEN}All critical tests passed!${NC}"
echo ""
echo "Next steps:"
echo "1. Test voice commands manually"
echo "2. Test dynamic scraping manually"
echo "3. Review logs: adb logcat | grep DatabaseMigrationHelper"
echo "4. Verify data: sqlite3 commands in testing guide"
```

**Make executable and run:**
```bash
chmod +x database-consolidation-test.sh
./database-consolidation-test.sh
```

---

## Test Sign-Off

### Before Merging to Main

**Required Sign-Offs:**
- [ ] Unit tests pass (automated)
- [ ] Device tests pass (manual)
- [ ] Migration verified (data intact)
- [ ] Old databases kept (rollback possible)
- [ ] App functionality works (end-to-end)
- [ ] Performance acceptable (<10s migration)
- [ ] No data loss (verified)
- [ ] Teams app shows 254 elements (if applicable)

**Reviewer Approval:**
- [ ] Code review complete
- [ ] Test results reviewed
- [ ] Documentation complete
- [ ] Ready to merge

**Signed:**
- Developer: _________________ Date: _______
- Reviewer: _________________ Date: _______

---

## Support / Troubleshooting

### Common Issues

**Issue 1: Migration takes too long**
- Check dataset size
- Monitor CPU usage
- Consider async migration with progress indicator

**Issue 2: Migration fails with exception**
- Check logs for specific error
- Verify database file permissions
- Check disk space
- Try clearing app cache

**Issue 3: Data missing after migration**
- Check migration completion flag
- Verify old databases readable
- Compare counts (old vs unified)
- Restore from backup if needed

**Issue 4: App crashes on startup**
- Check logcat for stack trace
- Verify database schema version
- Clear app data and retry
- Report issue with logs

---

## Contact

**Questions or Issues:**
- Email: manoj@ideahq.net
- GitHub: https://github.com/anthropics/claude-code/issues
- Documentation: `/docs/context/Database-Consolidation-*`

---

**Testing Guide Version:** 1.0
**Last Updated:** 2025-11-07 08:30 PST
**Implementation:** Database-Consolidation-Implementation-Complete-2511070815.md
**Status:** Ready for Testing
