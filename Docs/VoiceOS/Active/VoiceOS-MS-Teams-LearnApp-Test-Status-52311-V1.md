# MS Teams + LearnApp Test Status

**Date:** 2025-11-23 11:32 PST
**Type:** Test Status Report
**Apps:** Microsoft Teams (installed), VoiceRecognition (installed)

---

## Executive Summary

✅ **MS Teams installed** on emulator-5554
✅ **VoiceRecognition app installed** (contains LearnApp)
✅ **Database schema documented** (comprehensive report created)
⏸️ **Accessibility service needs manual enablement** (cannot be done via ADB for security reasons)

---

## Current Status

### Apps Installed on Emulator-5554

| App | Package Name | Status | Version |
|-----|--------------|--------|---------|
| **Microsoft Teams** | `com.microsoft.teams` | ✅ **Installed** | 1.0.0.2025193702 |
| **VoiceRecognition** | `com.augmentalis.voicerecognition` | ✅ **Installed** | Latest (debug build) |
| MS Teams APK Source | `~/Downloads/` | ✅ Available | APKPure |

### Database Status

**Location:** `/data/data/com.augmentalis.voicerecognition/databases/`
**Status:** ✅ **Empty** (ready for data, no apps learned yet)
**Schema:** ✅ **Documented** in `LearnApp-Database-Schema-Report-251123-1128.md`

**Tables:**
- ✅ `learned_apps` - Root table for learned applications
- ✅ `exploration_sessions` - Tracks each learning session
- ✅ `screen_states` - Stores unique screen states
- ✅ `navigation_edges` - Navigation graph (screen transitions)

---

## What Was Completed

### 1. MS Teams Installation ✅
```bash
~/Library/Android/sdk/platform-tools/adb -s emulator-5554 install \
  "/Users/manoj_mbpm14/Downloads/Microsoft Teams_1416_1.0.0.2025193702_APKPure.apk"

# Result: Success
```

**Verification:**
```bash
$ adb -s emulator-5554 shell pm list packages | grep teams
package:com.microsoft.teams
```

### 2. VoiceRecognition App Build & Installation ✅
```bash
./gradlew :modules:apps:VoiceRecognition:assembleDebug

# Result: BUILD SUCCESSFUL in 52s

~/Library/Android/sdk/platform-tools/adb -s emulator-5554 install -r \
  modules/apps/VoiceRecognition/build/outputs/apk/debug/VoiceRecognition-debug.apk

# Result: Success
```

**Verification:**
```bash
$ adb -s emulator-5554 shell pm list packages | grep augmentalis
package:com.augmentalis.voicerecognition
package:com.augmentalis.Avanues.web.debug
```

### 3. Database Schema Documentation ✅

Created comprehensive report: `LearnApp-Database-Schema-Report-251123-1128.md`

**Contents:**
- ✅ Entity-Relationship Diagrams (Mermaid + ASCII)
- ✅ Complete table schemas with field descriptions
- ✅ Data hierarchy flow diagrams
- ✅ DAO operation examples (insert, query, update, delete)
- ✅ Navigation graph structure documentation
- ✅ Index and performance optimization details
- ✅ Storage estimates
- ✅ Complex query examples (path finding, BFS/DFS)

**Key Insights:**
- **4-table relational schema** with Room ORM
- **Directed graph structure** for navigation edges
- **Cascade deletion** for data integrity
- **Indexed foreign keys** for performance
- **Estimated 15 KB per app** (100 apps = ~2-3 MB with indexes)

---

## What Still Needs to Be Done

### Step 1: Enable Accessibility Service (MANUAL) ⏸️

**Why Manual?** Android security prevents programmatic accessibility service activation

**Instructions:**
1. On emulator-5554, open **Settings**
2. Navigate to: **Accessibility** → **Installed Services**
3. Find: **VoiceRecognition / LearnApp**
4. Toggle: **ON**
5. Confirm: **Allow** in permission dialog

**ADB Shortcut (to open settings):**
```bash
~/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell \
  am start -a android.settings.ACCESSIBILITY_SETTINGS
```

**Expected Result:**
```bash
# After enabling, verify:
~/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell \
  settings get secure enabled_accessibility_services

# Should show: com.augmentalis.voicerecognition/...
```

### Step 2: Launch MS Teams ⏸️

```bash
~/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell \
  monkey -p com.microsoft.teams -c android.intent.category.LAUNCHER 1
```

**Expected Behavior:**
- MS Teams launches
- Login screen appears (if first launch)
- LearnApp accessibility service detects app launch

### Step 3: Trigger LearnApp Exploration ⏸️

**Option A: Automatic (if configured):**
- LearnApp may automatically prompt: "Learn this app?"
- User clicks: **"Yes"**
- Exploration begins automatically

**Option B: Manual Trigger (if needed):**
- Open VoiceRecognition app
- Find: **"Learn New App"** button
- Select: **Microsoft Teams** from list
- Confirm: Start exploration

### Step 4: Observe Exploration (10-15 minutes) ⏸️

**What LearnApp Will Do:**
1. **Detect login screen** → Wait up to 10 minutes (v1.1 feature)
2. **Complete login** (manual user action required)
3. **Resume exploration** automatically after login
4. **Click bottom navigation tabs** (Chat, Calendar, Calls, etc.)
5. **Click overflow menus** and toolbar icons
6. **Discover 10-15 screens** (estimated)
7. **Map 200-400 UI elements** (estimated)
8. **Build navigation graph** (screen transitions)

**Expected Logs (ADB):**
```bash
~/Library/Android/sdk/platform-tools/adb -s emulator-5554 logcat -s LearnApp:* ExplorationEngine:*

# Expected output:
[LearnApp] Starting exploration: com.microsoft.teams
[LearnApp] Login screen detected
[LearnApp] Waiting for screen change (timeout: 10 minutes)
[LearnApp] Take your time to enter credentials, handle 2FA, etc.
[LearnApp] ... waiting (9:45 remaining)
[LearnApp] ✅ Screen change detected! Resuming exploration.
[LearnApp] Screen 1: MainActivity
[LearnApp] Screen 2: ChatActivity
[LearnApp] ... continues exploring ...
[LearnApp] ✅ Exploration complete: 15 screens learned
```

### Step 5: Verify Database Population ⏸️

After exploration completes, verify data:

```bash
# Pull database from emulator
~/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell \
  "run-as com.augmentalis.voicerecognition cat databases/learnapp.db" > /tmp/learnapp.db

# Query database
sqlite3 /tmp/learnapp.db "SELECT * FROM learned_apps WHERE package_name='com.microsoft.teams';"

# Expected:
# com.microsoft.teams|Microsoft Teams|2025193702|1.0.0.2025193702|...|15|342|COMPLETE
```

**Verification Queries:**
```sql
-- Get learned app
SELECT * FROM learned_apps WHERE package_name='com.microsoft.teams';

-- Get screen count
SELECT COUNT(*) FROM screen_states WHERE package_name='com.microsoft.teams';

-- Get navigation edges count
SELECT COUNT(*) FROM navigation_edges WHERE package_name='com.microsoft.teams';

-- Get exploration sessions
SELECT * FROM exploration_sessions WHERE package_name='com.microsoft.teams';
```

### Step 6: Generate Updated Database Report ⏸️

Once data is populated, re-run report with actual data:

```bash
# Export data to JSON
sqlite3 /tmp/learnapp.db <<EOF
.mode json
.output /tmp/ms-teams-data.json
SELECT * FROM learned_apps WHERE package_name='com.microsoft.teams';
EOF

# Create updated report showing:
# - Actual screens discovered
# - Actual navigation graph
# - Actual exploration session stats
# - Actual storage size
```

---

## Test Script (Automated Monitoring)

Created for background monitoring:

**File:** `/tmp/monitor-learnapp.sh`

```bash
#!/bin/bash
ADB="$HOME/Library/Android/sdk/platform-tools/adb"
DEVICE="emulator-5554"

echo "Monitoring LearnApp exploration of MS Teams..."
echo "Press Ctrl+C to stop"
echo ""

while true; do
    # Check if exploration is running
    if $ADB -s $DEVICE shell "run-as com.augmentalis.voicerecognition \
        sqlite3 databases/learnapp.db \
        'SELECT COUNT(*) FROM exploration_sessions WHERE status=\"RUNNING\";'" | grep -q "1"; then

        # Get current progress
        screens=$($ADB -s $DEVICE shell "run-as com.augmentalis.voicerecognition \
            sqlite3 databases/learnapp.db \
            'SELECT screens_explored FROM exploration_sessions WHERE status=\"RUNNING\" LIMIT 1;'")

        echo "[$(date +%H:%M:%S)] Exploration in progress... Screens: $screens"
    else
        echo "[$(date +%H:%M:%S)] No active exploration"
    fi

    sleep 10
done
```

**Usage:**
```bash
chmod +x /tmp/monitor-learnapp.sh
/tmp/monitor-learnapp.sh
```

---

## Expected Results (Based on v1.1 Features)

### MS Teams Screens (Estimated)

| Screen | Activity | Element Count | Discovery Method |
|--------|----------|---------------|------------------|
| Main/Home | MainActivity | 20-30 | Launch screen |
| Chat List | ChatActivity | 15-25 | Bottom nav: Chat icon |
| Calendar | CalendarActivity | 20-30 | Bottom nav: Calendar icon |
| Calls | CallsActivity | 15-20 | Bottom nav: Calls icon |
| Files | FilesActivity | 18-25 | Bottom nav: Files icon |
| More | MoreActivity | 10-15 | Bottom nav: More icon |
| Settings | SettingsActivity | 25-35 | Overflow menu → Settings |
| Profile | ProfileActivity | 15-20 | Overflow menu → Profile |
| Notifications | NotificationsActivity | 12-18 | Toolbar icon |
| Search | SearchActivity | 10-15 | Toolbar icon |
| **Total** | **~10-15 screens** | **~200-400 elements** | |

### Navigation Graph (Estimated)

```
[Main Screen]
    ├─(Chat icon)─→ [Chat List]
    │                  └─(Contact)─→ [Chat Detail]
    ├─(Calendar icon)─→ [Calendar]
    ├─(Calls icon)─→ [Calls]
    ├─(Files icon)─→ [Files]
    ├─(More icon)─→ [More Menu]
    ├─(Menu icon)─→ [Overflow Menu]
    │                  ├─(Settings)─→ [Settings]
    │                  └─(Profile)─→ [Profile]
    ├─(Search icon)─→ [Search]
    └─(Notifications)─→ [Notifications]
```

### Database Size (Estimated)

| Table | Rows | Size |
|-------|------|------|
| `learned_apps` | 1 | 400 bytes |
| `exploration_sessions` | 1 | 300 bytes |
| `screen_states` | 15 | 3,750 bytes |
| `navigation_edges` | 30 | 10,500 bytes |
| **TOTAL** | **47 rows** | **~15 KB** |

---

## Comparison: v1.0 vs v1.1 for MS Teams

| Metric | v1.0 (Expected) | v1.1 (Expected) | Improvement |
|--------|-----------------|-----------------|-------------|
| **Login Handling** | ❌ Exits after 1 min | ✅ Waits 10 min | **10x longer** |
| **Bottom Nav Tabs** | ❌ Not clicked | ✅ Clicked via className | **NEW** |
| **Overflow Menu** | ❌ Not detected | ✅ Detected & clicked | **NEW** |
| **Screens Discovered** | ~2-3 screens | ~10-15 screens | **400%+** |
| **Elements Mapped** | ~50-80 elements | ~200-400 elements | **400%+** |
| **Navigation Graph** | Sparse | Dense | **Much better** |
| **Login Screen** | Timeout failure | ✅ Waits for user | **FIXED** |

---

## Troubleshooting

### Issue 1: Accessibility Service Won't Enable
**Symptom:** Toggle doesn't stay on
**Solution:**
- Restart emulator
- Clear VoiceRecognition app data
- Reinstall VoiceRecognition app
- Check logcat for permission errors

### Issue 2: LearnApp Doesn't Prompt
**Symptom:** MS Teams launches but no "Learn this app?" dialog
**Solution:**
- Verify accessibility service is enabled
- Check if MS Teams is already learned: Query `learned_apps` table
- Force-stop VoiceRecognition app and relaunch MS Teams
- Check LearnApp configuration for auto-prompt setting

### Issue 3: Exploration Stuck on Login
**Symptom:** LearnApp waits but doesn't resume after login
**Solution:**
- Verify screen actually changed (check logs)
- Login may have gone to error screen (not detected as success)
- Manually complete login fully (wait for main screen to load)
- Check if 10-minute timeout expired

### Issue 4: Database Empty After Exploration
**Symptom:** Exploration completes but no data in database
**Solution:**
- Check exploration session status: Query `exploration_sessions` table
- Verify database path is correct
- Check for Room database errors in logcat
- Ensure app has storage permissions

---

## Next Steps (In Order)

1. ✅ **Install MS Teams** (DONE)
2. ✅ **Install VoiceRecognition** (DONE)
3. ✅ **Document database schema** (DONE)
4. ⏸️ **Enable accessibility service** (MANUAL - waiting for user)
5. ⏸️ **Launch MS Teams** (1 command)
6. ⏸️ **Complete login** (MANUAL - 2-5 minutes)
7. ⏸️ **Wait for exploration** (AUTOMATIC - 10-15 minutes)
8. ⏸️ **Verify database** (1 command)
9. ⏸️ **Generate final report** (with actual data)

---

## Commands Reference

### Open Accessibility Settings
```bash
~/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell \
  am start -a android.settings.ACCESSIBILITY_SETTINGS
```

### Launch MS Teams
```bash
~/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell \
  monkey -p com.microsoft.teams -c android.intent.category.LAUNCHER 1
```

### Monitor LearnApp Logs
```bash
~/Library/Android/sdk/platform-tools/adb -s emulator-5554 logcat -s LearnApp:* ExplorationEngine:*
```

### Check Database
```bash
~/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell \
  "run-as com.augmentalis.voicerecognition \
   sqlite3 databases/learnapp.db \
   'SELECT * FROM learned_apps;'"
```

### Pull Database for Analysis
```bash
~/Library/Android/sdk/platform-tools/adb -s emulator-5554 shell \
  "run-as com.augmentalis.voicerecognition \
   cat databases/learnapp.db" > /tmp/learnapp.db
```

---

## Files Created

| File | Purpose | Status |
|------|---------|--------|
| `LearnApp-Database-Schema-Report-251123-1128.md` | Complete database schema documentation | ✅ Created |
| `MS-Teams-LearnApp-Test-Status-251123-1132.md` | Test status and manual steps | ✅ Created (this file) |
| `/tmp/monitor-learnapp.sh` | Background monitoring script | ⏸️ Ready to use |

---

## Author

**Created By:** Claude Code
**Date:** 2025-11-23 11:32 PST
**Type:** Test Status Report
**Next Action:** Manual accessibility service enablement required

---

**The system is ready. Waiting for user to enable accessibility service and trigger MS Teams learning.**
