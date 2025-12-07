# How to Relearn an App in LearnApp

**Created:** 2025-10-30 00:45 PDT
**Purpose:** Instructions for deleting/resetting learned app data to enable re-exploration

---

## Problem

When you want to test LearnApp fixes or re-explore an app, the app is marked as "COMPLETE" in the database and won't be re-learned unless you delete or reset its data.

---

## Solution 1: In-App Methods (RECOMMENDED)

### Method A: Using LearnAppIntegration API

**⭐ RECOMMENDED for Testing and Production**

LearnApp now provides public methods for resetting/deleting learned apps directly from within VoiceOS.

**Reset an app (clear data, keep metadata):**
```kotlin
val learnApp = LearnAppIntegration.getInstance()

learnApp.resetLearnedApp("com.realwear.testcomp") { success, message ->
    if (success) {
        Log.d("VoiceOS", "Reset successful: $message")
        // Toast automatically shown: "App Reset" - "App reset successfully..."
    } else {
        Log.e("VoiceOS", "Reset failed: $message")
        // Toast shown: "Reset Failed" - "Failed to reset app: ..."
    }
}
```

**Delete an app completely:**
```kotlin
learnApp.deleteLearnedApp("com.realwear.testcomp") { success, message ->
    Log.d("VoiceOS", "Delete: $message")
    // Toast automatically shown
}
```

**List all learned apps:**
```kotlin
learnApp.getLearnedApps { apps ->
    apps.forEach { packageName ->
        Log.d("VoiceOS", "Learned app: $packageName")
    }
}
```

**Check if app is learned:**
```kotlin
learnApp.isAppLearned("com.realwear.testcomp") { isLearned ->
    Log.d("VoiceOS", "Is learned: $isLearned")
}
```

**Benefits:**
- ✅ No ADB required
- ✅ Works from within VoiceOS app
- ✅ Toast notifications automatic
- ✅ Main thread callbacks (UI safe)
- ✅ Error handling included
- ✅ Ready for voice commands

**See:** `/docs/modules/LearnApp/USAGE-APP-MANAGEMENT.md` for complete usage guide with UI examples

---

## Solution 2: Using ADB and SQLite (Alternative Method)

### Method A: Direct Database Access (For Advanced Users)

**Step 1: Find the database file**
```bash
adb shell "run-as com.augmentalis.voiceos find /data/data/com.augmentalis.voiceos -name '*.db' 2>/dev/null"
```

Expected output:
```
/data/data/com.augmentalis.voiceos/databases/learnapp_database.db
```

**Step 2: Open database with sqlite3**
```bash
adb shell "run-as com.augmentalis.voiceos sqlite3 /data/data/com.augmentalis.voiceos/databases/learnapp_database.db"
```

**Step 3: Delete the app (Foreign key CASCADE will delete all related data)**
```sql
-- For RealWear TestComp:
DELETE FROM learned_apps WHERE package_name = 'com.realwear.testcomp';

-- Verify deletion:
SELECT COUNT(*) FROM learned_apps WHERE package_name = 'com.realwear.testcomp';
-- Should return: 0

-- Verify screen_states also deleted (CASCADE):
SELECT COUNT(*) FROM screen_states WHERE package_name = 'com.realwear.testcomp';
-- Should return: 0

-- Exit sqlite3:
.quit
```

**Step 4: Relaunch LearnApp and explore the app again**

---

### Method B: Using SQL Commands (Manual Database Editing)

Open sqlite3 as shown in Method A, then run these SQL commands:

**Delete the app:**
```sql
DELETE FROM learned_apps WHERE package_name = 'com.realwear.testcomp';
.quit
```

---

## Solution 3: Reset App Exploration Status (SQL)

If you want to keep basic app metadata but clear exploration data:

```sql
-- Reset exploration status to PARTIAL:
UPDATE learned_apps
SET exploration_status = 'PARTIAL',
    total_screens = 0,
    total_elements = 0,
    last_updated_at = strftime('%s', 'now') * 1000
WHERE package_name = 'com.realwear.testcomp';

-- Delete exploration data:
DELETE FROM screen_states WHERE package_name = 'com.realwear.testcomp';
DELETE FROM navigation_edges WHERE package_name = 'com.realwear.testcomp';
DELETE FROM exploration_sessions WHERE package_name = 'com.realwear.testcomp';

-- Verify:
SELECT exploration_status, total_screens, total_elements
FROM learned_apps
WHERE package_name = 'com.realwear.testcomp';
-- Should show: PARTIAL, 0, 0
```

---

## Solution 4: Clear All LearnApp Data (Nuclear Option)

**⚠️ WARNING:** This deletes ALL learned apps and exploration data.

```bash
# Stop VoiceOS
adb shell am force-stop com.augmentalis.voiceos

# Delete database
adb shell "run-as com.augmentalis.voiceos rm /data/data/com.augmentalis.voiceos/databases/learnapp_database.db"

# Restart VoiceOS - database will be recreated empty
adb shell am start -n com.augmentalis.voiceos/.MainActivity
```

---

## Quick Reference: SQL Commands

```sql
-- List all learned apps:
SELECT package_name, exploration_status, total_screens, total_elements FROM learned_apps;

-- Check screen_states for specific app:
SELECT screen_hash, package_name, element_count, datetime(discovered_at/1000, 'unixepoch')
FROM screen_states
WHERE package_name = 'com.realwear.testcomp';

-- Check exploration sessions:
SELECT session_id, status, datetime(started_at/1000, 'unixepoch') as started
FROM exploration_sessions
WHERE package_name = 'com.realwear.testcomp';

-- Check navigation edges count:
SELECT COUNT(*) FROM navigation_edges WHERE package_name = 'com.realwear.testcomp';

-- Delete specific app (CASCADE deletes all related data):
DELETE FROM learned_apps WHERE package_name = 'com.realwear.testcomp';
```

---

## Testing the Fixes

After deleting the app data:

### Test 1: Verify screen_states Persistence

**Expected Result:**
```sql
-- After re-exploring RealWear TestComp:
SELECT COUNT(*) FROM screen_states WHERE package_name = 'com.realwear.testcomp';
-- Should return: 1-2 (not 0)

SELECT screen_hash, package_name, element_count
FROM screen_states
WHERE package_name = 'com.realwear.testcomp';
-- Should show actual screen records with element counts
```

### Test 2: Verify NO Launcher Elements

**Expected Result:**
```sql
-- Count distinct app_ids (screens):
SELECT COUNT(DISTINCT app_id) FROM scraped_elements
WHERE element_hash LIKE '%com.realwear.testcomp%';
-- Should return: 1-2 (not 4)

-- Verify no launcher package:
SELECT DISTINCT package_name FROM scraped_elements
WHERE app_id IN (
    SELECT app_id FROM scraped_elements
    WHERE element_hash LIKE '%com.realwear.testcomp%'
);
-- Should ONLY show: com.realwear.testcomp (no launcher package)
```

### Check Logs:

**Expected Logs:**
```
D/LearnAppRepository: Saved ScreenStateEntity: hash=abc123..., package=com.realwear.testcomp, elements=7

W/ExplorationEngine: Navigation led to different package: com.google.android.launcher (expected: com.realwear.testcomp)
D/ExplorationEngine: Recording special navigation edge and attempting BACK to recover
D/ExplorationEngine: Successfully recovered to com.realwear.testcomp after 1 BACK attempts

D/ExplorationEngine: Package name validated: com.realwear.testcomp matches target com.realwear.testcomp
```

**Should NOT see:**
```
❌ W/ExplorationEngine: exploreScreenRecursive called with wrong package: com.google.android.launcher (expected: com.realwear.testcomp)
```
(This would indicate launcher elements being explored)

---

## Repository Methods Available

The following methods are now available in `LearnAppRepository`:

### 1. deleteAppCompletely(packageName: String)
- Deletes learned app AND all associated data
- Returns: `RepositoryResult.Success` or `RepositoryResult.Failure`
- Use: Complete removal (testing, cleanup)

### 2. resetAppForRelearning(packageName: String)
- Clears exploration data, keeps app metadata
- Sets status to PARTIAL
- Returns: `RepositoryResult.Success` or `RepositoryResult.Failure`
- Use: Re-explore app while keeping basic info

### 3. clearExplorationData(packageName: String)
- Clears sessions, edges, screen states
- Keeps app entry unchanged
- Returns: `RepositoryResult.Success` or `RepositoryResult.Failure`
- Use: Clean exploration data only

---

## Future Enhancement: UI for Relearning

**Recommended Implementation:**

Add a "Relearn App" option in LearnApp settings:

```kotlin
// In Settings UI:
Button(
    text = "Relearn ${app.appName}",
    onClick = {
        viewModel.relearnApp(app.packageName)
    }
)

// In ViewModel:
fun relearnApp(packageName: String) {
    viewModelScope.launch {
        when (val result = repository.resetAppForRelearning(packageName)) {
            is RepositoryResult.Success -> {
                // Show success message
                _uiState.value = UiState.Success("App reset. Relaunch to explore again.")
            }
            is RepositoryResult.Failure -> {
                // Show error
                _uiState.value = UiState.Error(result.reason)
            }
        }
    }
}
```

---

## Summary

**For Testing (Quick):**
```bash
# Delete app from database:
adb shell "run-as com.augmentalis.voiceos sqlite3 /data/data/com.augmentalis.voiceos/databases/learnapp_database.db \"DELETE FROM learned_apps WHERE package_name = 'com.realwear.testcomp';\""

# Verify:
adb shell "run-as com.augmentalis.voiceos sqlite3 /data/data/com.augmentalis.voiceos/databases/learnapp_database.db \"SELECT COUNT(*) FROM learned_apps WHERE package_name = 'com.realwear.testcomp';\""
```

**For Production (Future):**
- Implement UI with "Relearn App" button
- Use `repository.resetAppForRelearning(packageName)`
- Show confirmation dialog before reset

---

**Created:** 2025-10-30 00:45 PDT
**Related:** LearnApp-Package-Filtering-And-ScreenState-Persistence-251030-0019.md
