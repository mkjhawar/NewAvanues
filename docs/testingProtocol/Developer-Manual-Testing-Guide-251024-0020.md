# VOS4 Developer Manual Testing Guide - Complete Function Verification

**Document ID:** Developer-Manual-Testing-Guide-251024-0020
**Created:** 2025-10-24 00:20:00 PDT
**Version:** 1.0.0
**Purpose:** Step-by-step guide to verify ALL VOS4 functions work correctly

---

## Prerequisites

**Before Starting:**
- [ ] Build succeeds with no errors
- [ ] APK installed on test device (Android 10+)
- [ ] USB debugging enabled
- [ ] ADB connected (`adb devices` shows your device)
- [ ] Android Studio Device File Explorer open (for database inspection)

**Required Tools:**
```bash
# Install ADB if not present
# Mac: brew install android-platform-tools
# Linux: apt-get install adb

# Verify ADB connection
adb devices

# Open Android Studio's Database Inspector
# View → Tool Windows → App Inspection → Database Inspector
```

---

## Part 1: Installation & Permissions (10 minutes)

### Step 1.1: Install Application

```bash
cd "/Volumes/M Drive/Coding/vos4"
./gradlew :app:installDebug

# Verify installation
adb shell pm list packages | grep augmentalis
```

**Expected Output:**
```
package:com.augmentalis.voiceos
```

**Status:** [ ] PASS [ ] FAIL

**If Fail:** Check logcat for install errors:
```bash
adb logcat | grep "Package Manager"
```

---

### Step 1.2: Enable Accessibility Service

**Manual Steps:**
1. On device: Settings → Accessibility
2. Scroll to "Downloaded services" section
3. Find "VoiceOS" or "Voice Accessibility Service"
4. Tap to open
5. Toggle switch to ON
6. Tap "Allow" on permission dialog

**Verify Enabled:**
```bash
adb shell settings get secure enabled_accessibility_services
```

**Expected Output Should Contain:**
```
com.augmentalis.voiceos/com.augmentalis.voiceoscore.accessibility.VoiceOSService
```

**Status:** [ ] PASS [ ] FAIL

---

### Step 1.3: Grant All Permissions

**Check Current Permissions:**
```bash
adb shell dumpsys package com.augmentalis.voiceos | grep permission
```

**Grant Required Permissions:**
```bash
# Microphone
adb shell pm grant com.augmentalis.voiceos android.permission.RECORD_AUDIO

# Overlay (Draw over other apps)
adb shell appops set com.augmentalis.voiceos SYSTEM_ALERT_WINDOW allow

# Notifications
adb shell pm grant com.augmentalis.voiceos android.permission.POST_NOTIFICATIONS
```

**Verify All Granted:**
```bash
adb shell dumpsys package com.augmentalis.voiceos | grep "granted=true"
```

**Status:** [ ] PASS [ ] FAIL

---

## Part 2: Service Lifecycle Testing (15 minutes)

### Step 2.1: Service Starts on Boot

**Test Procedure:**
1. Reboot device: `adb reboot`
2. Wait for device to boot (2-3 minutes)
3. Check if VoiceOSService is running:

```bash
adb shell ps -A | grep voiceos
```

**Expected Output:**
```
u0_a123   12345  456  1234567  123456 SyS_epoll_wait 0 S com.augmentalis.voiceos
```

**Status:** [ ] PASS [ ] FAIL

---

### Step 2.2: Service Responds to Commands

**Start Logcat Monitoring:**
```bash
adb logcat -c  # Clear logs
adb logcat | grep "VoiceOS"
```

**Test Voice Recognition Start:**
1. Open VoiceOS app on device
2. Tap "Start Listening" button
3. Watch logcat for:

**Expected Log Output:**
```
VoiceOSService: onServiceConnected called
VoiceRecognitionManager: Starting recognition
SpeechEngineManager: Engine initialized: [engine_name]
```

**Status:** [ ] PASS [ ] FAIL

**Database Check:**
```bash
# Open Android Studio Database Inspector
# Connect to: com.augmentalis.voiceos
# Database: voice_os_database or app_scraping_database
```

**Verify Tables Exist:**
- [ ] `scraped_apps`
- [ ] `scraped_elements`
- [ ] `generated_commands`
- [ ] `user_interactions`
- [ ] `element_state_history`
- [ ] `screen_contexts`
- [ ] `screen_transitions`
- [ ] `element_relationships`
- [ ] `scraped_hierarchies`

**Status:** [ ] PASS [ ] FAIL

---

## Part 3: Voice Recognition Engine Testing (30 minutes)

### Step 3.1: Test Vosk Engine (Offline)

**Setup:**
1. Open VoiceOS app
2. Navigate to Settings
3. Select Speech Engine → Vosk
4. Ensure Vosk model downloaded

**Test Recognition:**
```bash
# Monitor Vosk logs
adb logcat | grep -i "vosk"
```

1. Say clearly: **"hello voiceos"**
2. Say: **"go home"**
3. Say: **"open settings"**

**Expected Logcat Output:**
```
VoskEngine: Recognition started
VoskEngine: Partial result: "hello"
VoskEngine: Final result: "hello voiceos"
VoskEngine: Confidence: 0.87
CommandProcessor: Matched command: GO_HOME
```

**Verify in Database:**
```sql
-- In Database Inspector, run query:
SELECT * FROM user_interactions
ORDER BY timestamp DESC
LIMIT 5;
```

**Expected Result:**
- New row with command text
- Recognition confidence score
- Timestamp within last minute
- Engine type = "vosk"

**Status:** [ ] PASS [ ] FAIL
**Recognition Accuracy:** ___/3 commands

---

### Step 3.2: Test Vivoka Engine

**Setup:**
1. Settings → Speech Engine → Vivoka
2. Configure Vivoka API key (if required)

**Test Recognition:**
```bash
adb logcat | grep -i "vivoka"
```

1. Say: **"voice assistant"**
2. Say: **"navigate back"**
3. Say: **"recent apps"**

**Expected Logcat:**
```
VivokaEngine: SDK initialized
VivokaEngine: Recognition result: "voice assistant"
VivokaEngine: Confidence: 0.92
```

**Database Check:**
```sql
SELECT command_text, recognition_confidence, engine_used
FROM user_interactions
WHERE engine_used = 'vivoka'
ORDER BY timestamp DESC
LIMIT 3;
```

**Verify:**
- [ ] 3 new rows inserted
- [ ] Confidence scores present
- [ ] Engine = "vivoka"
- [ ] Timestamps recent

**Status:** [ ] PASS [ ] FAIL

---

### Step 3.3: Test Android STT Engine

**Setup:**
1. Settings → Speech Engine → Android STT
2. Ensure internet connected

**Test Recognition:**
```bash
adb logcat | grep "AndroidSTT\|SpeechRecognizer"
```

1. Say: **"what time is it"**
2. Say: **"show notifications"**
3. Say: **"go to home screen"**

**Expected Logcat:**
```
AndroidSTTEngine: onReadyForSpeech
AndroidSTTEngine: onResults: [what time is it]
```

**Database Check:**
```sql
SELECT command_text, processing_time_ms
FROM user_interactions
WHERE engine_used = 'android_stt'
ORDER BY timestamp DESC
LIMIT 3;
```

**Verify:**
- [ ] 3 commands recorded
- [ ] Processing times < 1000ms
- [ ] All online recognition

**Status:** [ ] PASS [ ] FAIL

---

## Part 4: Command Execution Testing (45 minutes)

### Step 4.1: Navigation Commands

**Test: GO_HOME Command**

```bash
# Monitor command execution
adb logcat | grep "CommandProcessor\|ActionCoordinator"
```

**Procedure:**
1. Open any app (e.g., Settings)
2. Say: **"go home"**
3. Watch for home screen appearance

**Expected Logcat:**
```
CommandProcessor: Processing command: GO_HOME
ActionCoordinator: Executing global action: GLOBAL_ACTION_HOME
VoiceOSService: performGlobalAction(GLOBAL_ACTION_HOME) called
AccessibilityService: Action performed successfully
```

**Database Check:**
```sql
SELECT
    command_text,
    command_type,
    execution_status,
    execution_time_ms
FROM user_interactions
WHERE command_type = 'GO_HOME'
ORDER BY timestamp DESC
LIMIT 1;
```

**Verify:**
- [ ] Home screen appeared
- [ ] Command logged in database
- [ ] execution_status = "SUCCESS"
- [ ] execution_time_ms < 500

**Status:** [ ] PASS [ ] FAIL

---

**Test: GO_BACK Command**

**Procedure:**
1. Open Settings → About Phone (navigate deep)
2. Say: **"go back"**
3. Verify returns to previous screen

**Expected Logcat:**
```
CommandProcessor: Processing command: GO_BACK
ActionCoordinator: Executing global action: GLOBAL_ACTION_BACK
```

**Database Check:**
```sql
SELECT command_type, execution_status
FROM user_interactions
WHERE command_type = 'GO_BACK'
ORDER BY timestamp DESC
LIMIT 1;
```

**Status:** [ ] PASS [ ] FAIL

---

**Test: RECENT_APPS Command**

**Procedure:**
1. Say: **"recent apps"** or **"show recent"**
2. Verify recents screen appears

**Expected Logcat:**
```
CommandProcessor: Processing command: RECENT_APPS
ActionCoordinator: Executing global action: GLOBAL_ACTION_RECENTS
```

**Status:** [ ] PASS [ ] FAIL

---

### Step 4.2: App Launching Commands

**Test: Open Specific App**

```bash
adb logcat | grep "AppHandler\|InstalledAppsManager"
```

**Procedure:**
1. Say: **"open settings"**
2. Verify Settings app launches

**Expected Logcat:**
```
CommandProcessor: Processing command: OPEN_APP
AppHandler: Searching for app: settings
InstalledAppsManager: Found app: com.android.settings
AppHandler: Launching app: Settings
```

**Database Check:**
```sql
SELECT
    command_text,
    target_package,
    execution_status
FROM user_interactions
WHERE command_type = 'OPEN_APP'
ORDER BY timestamp DESC
LIMIT 1;
```

**Verify:**
- [ ] Settings app opened
- [ ] target_package = "com.android.settings"
- [ ] execution_status = "SUCCESS"

**Test Multiple Apps:**
- [ ] "open chrome" → Browser opens
- [ ] "open messages" → Messages app opens
- [ ] "open camera" → Camera app opens

**Status:** [ ] PASS ___/4 apps [ ] FAIL

---

### Step 4.3: Tap & Click Commands

**Test: TAP Command**

```bash
adb logcat | grep "SelectHandler\|NumberHandler"
```

**Procedure:**
1. Open Settings app
2. Say: **"show numbers"** (if numbered overlay feature available)
3. Say: **"tap three"** or **"click wifi"**

**Expected Logcat:**
```
UIHandler: Number overlay requested
NumberOverlayManager: Drawing numbers on screen
SelectHandler: Processing tap command
SelectHandler: Finding element: wifi
AccessibilityService: Element found at (x, y)
GestureHandler: Performing click at (x, y)
```

**Database Check:**
```sql
SELECT
    command_type,
    target_element_text,
    target_coordinates,
    execution_status
FROM user_interactions
WHERE command_type IN ('TAP', 'CLICK')
ORDER BY timestamp DESC
LIMIT 3;
```

**Verify:**
- [ ] Correct element tapped
- [ ] Coordinates recorded
- [ ] UI responded (navigated to WiFi settings)

**Status:** [ ] PASS [ ] FAIL

---

### Step 4.4: Scroll Commands

**Test: SCROLL_DOWN**

```bash
adb logcat | grep "GestureHandler\|ScrollHandler"
```

**Procedure:**
1. Open Settings (long scrollable list)
2. Note current position
3. Say: **"scroll down"**
4. Verify screen scrolled down

**Expected Logcat:**
```
CommandProcessor: Processing command: SCROLL_DOWN
GestureHandler: Creating scroll gesture
GestureHandler: Gesture path: (x1,y1) -> (x2,y2)
AccessibilityService: Gesture dispatched successfully
```

**Test All Scroll Types:**
- [ ] "scroll down" → Screen scrolls down
- [ ] "scroll up" → Screen scrolls up
- [ ] "scroll to top" → Jumps to top
- [ ] "scroll to bottom" → Jumps to bottom
- [ ] "page down" → Scrolls one page

**Database Check:**
```sql
SELECT command_type, gesture_duration_ms, execution_status
FROM user_interactions
WHERE command_type LIKE 'SCROLL%'
ORDER BY timestamp DESC
LIMIT 5;
```

**Status:** [ ] PASS ___/5 scroll types [ ] FAIL

---

### Step 4.5: Text Input Commands

**Test: TYPE Command**

```bash
adb logcat | grep "InputHandler\|VoiceKeyboard"
```

**Procedure:**
1. Open any app with text field (e.g., Messages)
2. Tap text field to focus
3. Say: **"type hello world"**
4. Verify text appears

**Expected Logcat:**
```
CommandProcessor: Processing command: TYPE
InputHandler: Text input requested: "hello world"
VoiceKeyboard: Inserting text: "hello world"
InputHandler: Text inserted successfully
```

**Test Text Commands:**
- [ ] "type hello" → "hello" appears
- [ ] "delete" → Last character deleted
- [ ] "delete word" → Last word deleted
- [ ] "clear text" → All text cleared
- [ ] "new line" → Line break inserted

**Database Check:**
```sql
SELECT
    command_type,
    input_text,
    target_field_hint,
    execution_status
FROM user_interactions
WHERE command_type IN ('TYPE', 'DELETE', 'CLEAR_TEXT')
ORDER BY timestamp DESC
LIMIT 5;
```

**Status:** [ ] PASS ___/5 input types [ ] FAIL

---

## Part 5: UI Scraping & Learning Testing (30 minutes)

### Step 5.1: Verify UI Scraping Active

```bash
adb logcat | grep "UIScrapingEngine\|AccessibilityScrapingIntegration"
```

**Procedure:**
1. Open a new app never used before (e.g., Calculator)
2. Wait 2-3 seconds
3. Check logcat

**Expected Logcat:**
```
UIScrapingEngine: onAccessibilityEvent: TYPE_WINDOW_STATE_CHANGED
AccessibilityScrapingIntegration: Scraping started for package: com.android.calculator2
UIScrapingEngine: Found 15 interactive elements
UIScrapingEngine: Scraping complete, saving to database
```

**Database Check - Scraped Apps:**
```sql
-- Check if app was scraped
SELECT
    package_name,
    app_name,
    scrape_count,
    last_scraped_timestamp
FROM scraped_apps
WHERE package_name = 'com.android.calculator2';
```

**Expected Result:**
- [ ] 1 row for calculator app
- [ ] scrape_count = 1
- [ ] last_scraped_timestamp = recent

**Status:** [ ] PASS [ ] FAIL

---

### Step 5.2: Verify Element Scraping

**Database Check - Scraped Elements:**
```sql
SELECT
    element_id,
    element_text,
    element_type,
    is_clickable,
    is_editable,
    bounds_in_screen
FROM scraped_elements
WHERE package_name = 'com.android.calculator2'
LIMIT 10;
```

**Verify:**
- [ ] Multiple elements found (10+)
- [ ] Element text captured ("1", "2", "+", "=", etc.)
- [ ] Element types present (Button, EditText, etc.)
- [ ] Clickable flags correct
- [ ] Bounds coordinates present

**Count Elements:**
```sql
SELECT COUNT(*) as element_count
FROM scraped_elements
WHERE package_name = 'com.android.calculator2';
```

**Expected:** 10-30 elements (depending on calculator UI)

**Actual Count:** _____ elements

**Status:** [ ] PASS [ ] FAIL

---

### Step 5.3: Verify Element Hierarchy

**Database Check:**
```sql
SELECT
    hierarchy_id,
    root_element_id,
    depth,
    child_count
FROM scraped_hierarchies
WHERE package_name = 'com.android.calculator2'
ORDER BY timestamp DESC
LIMIT 1;
```

**Verify:**
- [ ] Hierarchy recorded
- [ ] Root element identified
- [ ] Depth > 0 (shows nesting)
- [ ] Child count matches element count

**Status:** [ ] PASS [ ] FAIL

---

### Step 5.4: Verify Command Generation

**Database Check - Generated Commands:**
```sql
SELECT
    command_id,
    command_phrase,
    target_element_text,
    confidence_score,
    times_used
FROM generated_commands
WHERE package_name = 'com.android.calculator2'
ORDER BY confidence_score DESC
LIMIT 10;
```

**Expected Commands Generated:**
- [ ] "tap one" → button with text "1"
- [ ] "tap plus" → button with text "+"
- [ ] "tap equals" → button with text "="
- [ ] "press clear" → clear button

**Verify:**
- [ ] Commands generated automatically
- [ ] Confidence scores assigned
- [ ] Target elements correctly linked

**Status:** [ ] PASS [ ] FAIL

---

### Step 5.5: Test Learned Command Execution

**Procedure:**
1. Stay in Calculator app
2. Say: **"tap five"** (using learned command)
3. Verify "5" button pressed

```bash
adb logcat | grep "CommandGenerator\|CommandProcessor"
```

**Expected Logcat:**
```
CommandProcessor: Searching generated commands
CommandProcessor: Found generated command: "tap five"
CommandProcessor: Target element: Button[text="5"]
SelectHandler: Clicking element at position
```

**Database Check - Command Usage:**
```sql
SELECT
    command_phrase,
    times_used,
    last_used_timestamp,
    success_rate
FROM generated_commands
WHERE command_phrase = 'tap five'
ORDER BY last_used_timestamp DESC
LIMIT 1;
```

**Verify:**
- [ ] times_used incremented
- [ ] last_used_timestamp updated
- [ ] success_rate calculated

**Test More Learned Commands:**
- [ ] "tap plus" → + button
- [ ] "tap equals" → = button
- [ ] "press clear" → clear button

**Status:** [ ] PASS ___/4 commands [ ] FAIL

---

## Part 6: State Tracking & History Testing (20 minutes)

### Step 6.1: User Interaction Tracking

**Procedure:**
1. Perform 5 different actions in Calculator app
2. Check database captures all interactions

**Database Check:**
```sql
SELECT
    interaction_id,
    command_text,
    command_type,
    timestamp,
    execution_status,
    execution_time_ms
FROM user_interactions
WHERE package_name = 'com.android.calculator2'
ORDER BY timestamp DESC
LIMIT 5;
```

**Verify:**
- [ ] All 5 interactions logged
- [ ] Timestamps in correct order
- [ ] Execution times reasonable (< 1000ms)
- [ ] execution_status shows SUCCESS/FAILURE

**Status:** [ ] PASS [ ] FAIL

---

### Step 6.2: Element State History

**Database Check:**
```sql
SELECT
    element_id,
    previous_state,
    new_state,
    state_change_timestamp,
    trigger_command
FROM element_state_history
WHERE package_name = 'com.android.calculator2'
ORDER BY state_change_timestamp DESC
LIMIT 10;
```

**Verify:**
- [ ] State changes recorded
- [ ] Previous and new states differ
- [ ] Trigger commands linked
- [ ] Timestamps sequential

**Status:** [ ] PASS [ ] FAIL

---

### Step 6.3: Screen Transition Tracking

**Procedure:**
1. From Calculator, press home
2. Open Calculator again
3. Check screen transitions recorded

**Database Check:**
```sql
SELECT
    from_screen_id,
    to_screen_id,
    transition_trigger,
    transition_timestamp
FROM screen_transitions
WHERE from_package = 'com.android.calculator2'
   OR to_package = 'com.android.calculator2'
ORDER BY transition_timestamp DESC
LIMIT 5;
```

**Verify:**
- [ ] Transitions recorded
- [ ] From/to screens identified
- [ ] Trigger commands logged
- [ ] Time between transitions reasonable

**Status:** [ ] PASS [ ] FAIL

---

### Step 6.4: Screen Context Recognition

**Database Check:**
```sql
SELECT
    screen_id,
    screen_title,
    screen_type,
    dominant_elements,
    context_confidence
FROM screen_contexts
WHERE package_name = 'com.android.calculator2'
ORDER BY last_seen_timestamp DESC
LIMIT 1;
```

**Verify:**
- [ ] Screen context identified
- [ ] Screen title captured
- [ ] Screen type classified
- [ ] Dominant elements listed
- [ ] Confidence score present

**Status:** [ ] PASS [ ] FAIL

---

## Part 7: Element Relationship Testing (15 minutes)

### Step 7.1: Parent-Child Relationships

**Database Check:**
```sql
SELECT
    parent_element_id,
    child_element_id,
    relationship_type,
    spatial_relationship
FROM element_relationships
WHERE package_name = 'com.android.calculator2'
LIMIT 20;
```

**Verify:**
- [ ] Parent-child relationships detected
- [ ] Relationship types assigned (CONTAINS, ABOVE, BELOW, etc.)
- [ ] Spatial relationships calculated

**Count Relationships:**
```sql
SELECT
    relationship_type,
    COUNT(*) as relationship_count
FROM element_relationships
WHERE package_name = 'com.android.calculator2'
GROUP BY relationship_type;
```

**Expected Relationship Types:**
- PARENT_OF
- CHILD_OF
- SIBLING_OF
- ABOVE
- BELOW
- LEFT_OF
- RIGHT_OF

**Status:** [ ] PASS [ ] FAIL

---

## Part 8: Voice Cursor Testing (20 minutes)

### Step 8.1: Cursor Activation

```bash
adb logcat | grep "VoiceCursor\|CursorManager"
```

**Procedure:**
1. Say: **"show cursor"** or **"enable cursor"**
2. Verify cursor appears on screen

**Expected Logcat:**
```
VoiceCursorManager: Cursor activation requested
CursorVisibilityManager: Showing cursor
CursorPositionTracker: Initial position: (x, y)
```

**Status:** [ ] PASS [ ] FAIL [ ] N/A (feature not available)

---

### Step 8.2: Cursor Movement

**Test All Directions:**
- [ ] "move up" → Cursor moves up
- [ ] "move down" → Cursor moves down
- [ ] "move left" → Cursor moves left
- [ ] "move right" → Cursor moves right
- [ ] "move faster" → Speed increases
- [ ] "move slower" → Speed decreases

**Expected Logcat:**
```
CursorPositionTracker: Moving cursor: direction=UP, speed=MEDIUM
CursorPositionTracker: New position: (x, y)
```

**Database Check:**
```sql
SELECT
    command_text,
    cursor_position_before,
    cursor_position_after,
    movement_distance
FROM user_interactions
WHERE command_type LIKE 'CURSOR_%'
ORDER BY timestamp DESC
LIMIT 10;
```

**Status:** [ ] PASS ___/6 movements [ ] FAIL [ ] N/A

---

### Step 8.3: Cursor Click

**Procedure:**
1. Move cursor over clickable element
2. Say: **"click"** or **"tap"**
3. Verify element clicked

**Expected Logcat:**
```
VoiceCursorEventHandler: Click command at cursor position
CursorPositionTracker: Current position: (x, y)
GestureHandler: Performing click at (x, y)
```

**Status:** [ ] PASS [ ] FAIL [ ] N/A

---

## Part 9: Database Integrity Testing (20 minutes)

### Step 9.1: Foreign Key Constraints

**Test: Insert Interaction Without Parent App**

```sql
-- This should FAIL due to FK constraint
INSERT INTO user_interactions (
    interaction_id,
    package_name,
    command_text,
    timestamp
) VALUES (
    'test-interaction-1',
    'com.nonexistent.app',
    'test command',
    datetime('now')
);
```

**Expected Result:**
```
Error: FOREIGN KEY constraint failed
```

**Verify:**
- [ ] Insert rejected
- [ ] Error message about FK constraint
- [ ] Database integrity maintained

**Status:** [ ] PASS [ ] FAIL

---

### Step 9.2: Cascade Deletion

**Test: Delete App Cascades to Elements**

```sql
-- Count elements before deletion
SELECT COUNT(*) FROM scraped_elements
WHERE package_name = 'com.android.calculator2';

-- Delete app
DELETE FROM scraped_apps
WHERE package_name = 'com.android.calculator2';

-- Count elements after deletion
SELECT COUNT(*) FROM scraped_elements
WHERE package_name = 'com.android.calculator2';
```

**Verify:**
- [ ] Elements count > 0 before deletion
- [ ] Elements count = 0 after deletion
- [ ] Cascade worked correctly

**Restore Test Data:**
```
-- Re-open Calculator app to re-scrape
```

**Status:** [ ] PASS [ ] FAIL

---

### Step 9.3: Database Migration

**Check Current Database Version:**
```sql
SELECT * FROM room_master_table;
```

**Expected:**
- Database version number
- Migration history (if available)

**Verify:**
- [ ] Database schema version correct
- [ ] All expected tables present
- [ ] No orphaned tables from old versions

**Status:** [ ] PASS [ ] FAIL

---

## Part 10: Performance & Memory Testing (25 minutes)

### Step 10.1: Memory Usage

**Check Current Memory:**
```bash
adb shell dumpsys meminfo com.augmentalis.voiceos
```

**Record Values:**
- Native Heap: _____ MB
- Dalvik Heap: _____ MB
- Total PSS: _____ MB

**Acceptable Limits:**
- Idle: < 100 MB
- Active Recognition: < 200 MB
- Heavy Scraping: < 300 MB

**Status:** [ ] PASS [ ] FAIL

---

### Step 10.2: Database Size

**Check Database File Size:**
```bash
adb shell run-as com.augmentalis.voiceos ls -lh /data/data/com.augmentalis.voiceos/databases/
```

**Record Sizes:**
- app_scraping_database: _____ KB/MB
- app_scraping_database-wal: _____ KB
- app_scraping_database-shm: _____ KB

**Expected:**
- Main DB: < 50 MB (after moderate use)
- WAL file: < 10 MB
- SHM file: < 1 MB

**Status:** [ ] PASS [ ] FAIL

---

### Step 10.3: Database Query Performance

**Test: Complex Query Time**

```sql
-- Enable query timing
.timer ON

-- Complex join query
SELECT
    sa.app_name,
    COUNT(DISTINCT se.element_id) as element_count,
    COUNT(DISTINCT gc.command_id) as command_count,
    COUNT(DISTINCT ui.interaction_id) as interaction_count
FROM scraped_apps sa
LEFT JOIN scraped_elements se ON sa.package_name = se.package_name
LEFT JOIN generated_commands gc ON sa.package_name = gc.package_name
LEFT JOIN user_interactions ui ON sa.package_name = ui.package_name
GROUP BY sa.package_name
ORDER BY interaction_count DESC
LIMIT 10;

-- Record execution time
```

**Expected Query Time:** < 500ms

**Actual Time:** _____ ms

**Status:** [ ] PASS [ ] FAIL

---

### Step 10.4: Recognition Latency

**Measure End-to-End Latency:**

```bash
# Start timestamp logging
adb logcat -c
adb logcat -v time | grep -E "Recognition started|Action performed"
```

**Procedure:**
1. Say command: **"go home"**
2. Note timestamps in logcat

**Expected Timing:**
- Speech start to recognition: < 500ms
- Recognition to command match: < 100ms
- Command to action execution: < 200ms
- **Total latency: < 800ms**

**Measured Times:**
- Speech → Recognition: _____ ms
- Recognition → Command: _____ ms
- Command → Action: _____ ms
- **Total: _____ ms**

**Status:** [ ] PASS [ ] FAIL

---

## Part 11: Error Handling & Edge Cases (20 minutes)

### Step 11.1: Unrecognized Command

**Procedure:**
1. Say nonsense: **"blah blah blah xyz"**
2. Check error handling

**Expected Logcat:**
```
CommandProcessor: No matching command found
CommandProcessor: Confidence too low: 0.23
VoiceOSService: Showing user feedback: Command not recognized
```

**Database Check:**
```sql
SELECT
    command_text,
    execution_status,
    error_message
FROM user_interactions
WHERE execution_status = 'FAILED'
ORDER BY timestamp DESC
LIMIT 1;
```

**Verify:**
- [ ] Failed interaction logged
- [ ] Error message present
- [ ] No crash occurred

**Status:** [ ] PASS [ ] FAIL

---

### Step 11.2: Service Restart

**Procedure:**
1. Force stop service: `adb shell am force-stop com.augmentalis.voiceos`
2. Open any app
3. Verify service restarts automatically

**Expected Logcat:**
```
VoiceOSService: onCreate called
VoiceOSService: Service restarted
VoiceOSService: onServiceConnected
```

**Status:** [ ] PASS [ ] FAIL

---

### Step 11.3: Database Corruption Recovery

**Test: Missing Database Recovery**

```bash
# Backup current database
adb shell run-as com.augmentalis.voiceos cp /data/data/com.augmentalis.voiceos/databases/app_scraping_database /sdcard/backup.db

# Delete database
adb shell run-as com.augmentalis.voiceos rm /data/data/com.augmentalis.voiceos/databases/app_scraping_database

# Restart app
adb shell am force-stop com.augmentalis.voiceos
adb shell am start -n com.augmentalis.voiceos/.ui.activities.MainActivity
```

**Expected Logcat:**
```
Room: Database not found, creating new database
AppScrapingDatabase: Running migrations
AppScrapingDatabase: Database ready
```

**Verify:**
- [ ] New database created
- [ ] All tables present
- [ ] No data loss for future operations

**Restore:**
```bash
adb shell run-as com.augmentalis.voiceos cp /sdcard/backup.db /data/data/com.augmentalis.voiceos/databases/app_scraping_database
```

**Status:** [ ] PASS [ ] FAIL

---

## Part 12: Complete Function Test Summary

### Database Population Verification

**Final Database State Check:**

```sql
-- Count all records
SELECT 'scraped_apps' as table_name, COUNT(*) as record_count FROM scraped_apps
UNION ALL
SELECT 'scraped_elements', COUNT(*) FROM scraped_elements
UNION ALL
SELECT 'generated_commands', COUNT(*) FROM generated_commands
UNION ALL
SELECT 'user_interactions', COUNT(*) FROM user_interactions
UNION ALL
SELECT 'element_state_history', COUNT(*) FROM element_state_history
UNION ALL
SELECT 'screen_contexts', COUNT(*) FROM screen_contexts
UNION ALL
SELECT 'screen_transitions', COUNT(*) FROM screen_transitions
UNION ALL
SELECT 'element_relationships', COUNT(*) FROM element_relationships
UNION ALL
SELECT 'scraped_hierarchies', COUNT(*) FROM scraped_hierarchies;
```

**Expected Minimum Counts After Full Testing:**
- scraped_apps: > 3 (Calculator, Settings, home screen, etc.)
- scraped_elements: > 50 (elements from all apps)
- generated_commands: > 20 (learned commands)
- user_interactions: > 30 (all test commands)
- element_state_history: > 10 (state changes)
- screen_contexts: > 5 (different screens)
- screen_transitions: > 10 (navigation events)
- element_relationships: > 30 (parent-child, siblings)
- scraped_hierarchies: > 5 (UI trees)

**Your Actual Counts:**

| Table | Expected | Actual | Status |
|-------|----------|--------|--------|
| scraped_apps | >3 | ___ | [ ] PASS [ ] FAIL |
| scraped_elements | >50 | ___ | [ ] PASS [ ] FAIL |
| generated_commands | >20 | ___ | [ ] PASS [ ] FAIL |
| user_interactions | >30 | ___ | [ ] PASS [ ] FAIL |
| element_state_history | >10 | ___ | [ ] PASS [ ] FAIL |
| screen_contexts | >5 | ___ | [ ] PASS [ ] FAIL |
| screen_transitions | >10 | ___ | [ ] PASS [ ] FAIL |
| element_relationships | >30 | ___ | [ ] PASS [ ] FAIL |
| scraped_hierarchies | >5 | ___ | [ ] PASS [ ] FAIL |

---

### Overall Test Results

**Test Date:** _______________
**Tester Name:** _______________
**Device:** _______________
**OS Version:** _______________
**Build/Commit:** _______________

**Test Results Summary:**

| Category | Tests | Passed | Failed | N/A |
|----------|-------|--------|--------|-----|
| Installation & Permissions | 3 | ___ | ___ | ___ |
| Service Lifecycle | 2 | ___ | ___ | ___ |
| Voice Recognition Engines | 3 | ___ | ___ | ___ |
| Command Execution | 5 | ___ | ___ | ___ |
| UI Scraping & Learning | 5 | ___ | ___ | ___ |
| State Tracking | 4 | ___ | ___ | ___ |
| Element Relationships | 1 | ___ | ___ | ___ |
| Voice Cursor | 3 | ___ | ___ | ___ |
| Database Integrity | 3 | ___ | ___ | ___ |
| Performance & Memory | 4 | ___ | ___ | ___ |
| Error Handling | 3 | ___ | ___ | ___ |
| **TOTAL** | **36** | **___** | **___** | **___** |

**Pass Percentage:** _____ %

**Critical Issues Found:**
1. _______________________________________________
2. _______________________________________________
3. _______________________________________________

**Overall Assessment:**
- [ ] ✅ ALL FUNCTIONS WORKING - Ready for production
- [ ] ⚠️ MINOR ISSUES - Ready with known limitations
- [ ] ❌ MAJOR ISSUES - Not ready, requires fixes

---

## Appendix A: Database Schema Quick Reference

```sql
-- Quick view of all table structures
SELECT name, sql FROM sqlite_master WHERE type='table';

-- Common useful queries for testing

-- 1. Recent activity summary
SELECT
    command_text,
    command_type,
    execution_status,
    execution_time_ms,
    timestamp
FROM user_interactions
ORDER BY timestamp DESC
LIMIT 20;

-- 2. App learning progress
SELECT
    sa.app_name,
    COUNT(DISTINCT se.element_id) as elements_found,
    COUNT(DISTINCT gc.command_id) as commands_generated,
    sa.last_scraped_timestamp
FROM scraped_apps sa
LEFT JOIN scraped_elements se ON sa.package_name = se.package_name
LEFT JOIN generated_commands gc ON sa.package_name = gc.package_name
GROUP BY sa.package_name;

-- 3. Command success rates
SELECT
    command_type,
    COUNT(*) as total_attempts,
    SUM(CASE WHEN execution_status = 'SUCCESS' THEN 1 ELSE 0 END) as successes,
    ROUND(100.0 * SUM(CASE WHEN execution_status = 'SUCCESS' THEN 1 ELSE 0 END) / COUNT(*), 2) as success_rate
FROM user_interactions
GROUP BY command_type
HAVING total_attempts > 5
ORDER BY success_rate DESC;

-- 4. Performance metrics
SELECT
    command_type,
    AVG(execution_time_ms) as avg_time,
    MIN(execution_time_ms) as min_time,
    MAX(execution_time_ms) as max_time
FROM user_interactions
WHERE execution_status = 'SUCCESS'
GROUP BY command_type
ORDER BY avg_time DESC;
```

---

## Appendix B: Troubleshooting Guide

**Service Won't Start:**
```bash
# Check if service is enabled
adb shell settings get secure enabled_accessibility_services

# Force enable
adb shell settings put secure enabled_accessibility_services com.augmentalis.voiceos/com.augmentalis.voiceoscore.accessibility.VoiceOSService

# Restart accessibility
adb shell killall com.android.systemui
```

**Database Not Populating:**
```bash
# Check if database exists
adb shell run-as com.augmentalis.voiceos ls /data/data/com.augmentalis.voiceos/databases/

# Check database permissions
adb shell run-as com.augmentalis.voiceos ls -l /data/data/com.augmentalis.voiceos/databases/

# Check Room logs
adb logcat | grep "Room\|Database"
```

**Commands Not Recognized:**
```bash
# Check speech engine status
adb logcat | grep "SpeechEngine\|Recognition"

# Check if microphone permission granted
adb shell dumpsys package com.augmentalis.voiceos | grep RECORD_AUDIO

# Test microphone directly
adb shell cmd media_session volume --show --stream 3
```

---

**Document Status:** ✅ Active - Complete Manual Testing Guide
**Next Update:** After test execution with real results
**Owner:** VOS4 QA Team
**Last Updated:** 2025-10-24 00:20:00 PDT
