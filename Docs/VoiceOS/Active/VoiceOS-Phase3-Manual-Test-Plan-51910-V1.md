# Phase 3 Manual Test Plan - User Interaction Tracking

**Date:** 2025-10-19 00:40:53 PDT
**Author:** Manoj Jhawar
**Status:** ⏳ PENDING (Device Required)
**Build Status:** ✅ BUILD SUCCESSFUL
**APK Location:** `app/build/outputs/apk/debug/app-debug.apk`

---

## Executive Summary

This document provides a comprehensive manual test plan for Phase 3 User Interaction Tracking. The code has been successfully built and is ready for testing. Testing is currently **blocked pending Android device availability**.

**Prerequisites:**
- ✅ Code implementation complete
- ✅ Build successful
- ❌ Android device connected (BLOCKING)

---

## Test Environment Setup

### Required Hardware
- Android device running API 29+ (Android 10+)
- USB cable for ADB connection
- Device with Developer Mode enabled
- USB Debugging enabled

### Required Software
- VoiceOS debug APK (built successfully)
- ADB tools (available at `~/Library/Android/sdk/platform-tools/adb`)
- Android Studio (optional, for logcat monitoring)

### Installation Steps

```bash
# 1. Connect Android device via USB
# 2. Verify device is connected
~/Library/Android/sdk/platform-tools/adb devices

# 3. Install VoiceOS debug APK
cd "/Volumes/M Drive/Coding/vos4"
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk

# 4. Grant accessibility service permission
# Settings → Accessibility → VoiceOS → Enable

# 5. Start logcat monitoring (optional)
~/Library/Android/sdk/platform-tools/adb logcat -s VoiceOS:V AccessibilityScrapingIntegration:V CommandGenerator:V VoiceCommandProcessor:V
```

---

## Test Suite 1: Interaction Recording

**Objective:** Verify that user interactions are recorded correctly in the database with proper settings and battery controls.

### Test 1.1: Basic Interaction Recording (Settings Enabled)

**Prerequisites:**
- Interaction learning enabled (default)
- Battery level >20%

**Steps:**
1. Open any app (e.g., Settings)
2. Click on various UI elements (buttons, list items, switches)
3. Long press on elements
4. Scroll through lists
5. Focus on text fields
6. Swipe between screens

**Expected Results:**
- Each interaction creates a record in `UserInteractionEntity` table
- Record includes: `elementHash`, `screenHash`, `interactionType`, `timestamp`, `success=true`
- Visibility duration calculated (time from element visible to user click)
- No blocking of accessibility events (smooth UI)

**Verification:**
```bash
# Check database for interaction records
adb shell "run-as com.augmentalis.voiceos sqlite3 /data/data/com.augmentalis.voiceos/databases/app_scraping.db 'SELECT COUNT(*) FROM user_interactions;'"

# View recent interactions
adb shell "run-as com.augmentalis.voiceos sqlite3 /data/data/com.augmentalis.voiceos/databases/app_scraping.db 'SELECT * FROM user_interactions ORDER BY timestamp DESC LIMIT 10;'"
```

**Pass Criteria:**
- ✅ All interactions recorded
- ✅ Visibility duration calculated correctly
- ✅ No UI lag or frame drops
- ✅ Success rate = 100% for successful interactions

---

### Test 1.2: Settings Toggle - Disable Learning

**Prerequisites:**
- Interaction learning currently enabled

**Steps:**
1. Call `setInteractionLearningEnabled(false)` via test code or UI
2. Interact with various apps (click, long press, scroll)
3. Check database for new records

**Expected Results:**
- No new interaction records created after disabling
- Fast exit from `recordInteraction()` (<0.01ms overhead)
- `isInteractionLearningEnabled()` returns `false`
- `isInteractionLearningUserEnabled()` returns `false`

**Verification:**
```bash
# Count interactions before
BEFORE=$(adb shell "run-as com.augmentalis.voiceos sqlite3 /data/data/com.augmentalis.voiceos/databases/app_scraping.db 'SELECT COUNT(*) FROM user_interactions;'")

# Disable learning, interact with app
# (via test code or UI toggle)

# Count interactions after
AFTER=$(adb shell "run-as com.augmentalis.voiceos sqlite3 /data/data/com.augmentalis.voiceos/databases/app_scraping.db 'SELECT COUNT(*) FROM user_interactions;'")

# Verify BEFORE == AFTER
```

**Pass Criteria:**
- ✅ No interactions recorded when disabled
- ✅ Settings persist across app restarts
- ✅ <0.01ms overhead when disabled (check logcat timestamps)

---

### Test 1.3: Settings Toggle - Re-enable Learning

**Prerequisites:**
- Interaction learning currently disabled

**Steps:**
1. Call `setInteractionLearningEnabled(true)`
2. Interact with apps
3. Verify recording resumes

**Expected Results:**
- New interactions recorded after enabling
- All tracking methods resume operation
- SharedPreferences updated

**Pass Criteria:**
- ✅ Recording resumes immediately
- ✅ No lost interactions
- ✅ Settings persist

---

### Test 1.4: Battery Cutoff at 20%

**Prerequisites:**
- Interaction learning enabled
- Ability to simulate battery level (or drain battery to ≤20%)

**Steps:**
1. Verify current battery level >20%
2. Verify interactions are being recorded
3. Drain battery to 20% or below (or mock `getBatteryLevel()`)
4. Interact with apps
5. Check if learning auto-disables

**Expected Results:**
- `getBatteryLevel()` returns correct percentage
- `isInteractionLearningEnabled()` returns `false` when battery ≤20%
- No interactions recorded when battery low
- User toggle setting unchanged (still enabled, just not active)

**Verification:**
```bash
# Check battery level
adb shell dumpsys battery | grep level

# Monitor log for battery cutoff
adb logcat -s AccessibilityScrapingIntegration:V | grep "battery"
```

**Pass Criteria:**
- ✅ Learning stops when battery ≤20%
- ✅ Learning resumes when battery >20%
- ✅ User toggle setting preserved
- ✅ Log message indicates battery cutoff

---

### Test 1.5: Battery Resume at 25%

**Prerequisites:**
- Battery at ≤20%, learning auto-disabled

**Steps:**
1. Charge device to >20% (e.g., 25%)
2. Interact with apps
3. Verify learning resumes

**Expected Results:**
- `isInteractionLearningEnabled()` returns `true` when battery >20%
- Interactions resume recording
- Automatic recovery

**Pass Criteria:**
- ✅ Learning resumes automatically
- ✅ No manual intervention required

---

### Test 1.6: State Change Recording

**Objective:** Verify element state changes are recorded.

**Steps:**
1. Find app with checkbox (e.g., Settings)
2. Check checkbox (unchecked → checked)
3. Uncheck checkbox (checked → unchecked)
4. Expand a list item
5. Collapse a list item
6. Toggle a switch

**Expected Results:**
- Each state change creates `ElementStateHistoryEntity` record
- Record includes: `elementHash`, `stateType` (checked/selected/enabled/focused/visible/expanded), `oldValue`, `newValue`, `timestamp`
- `triggerSource` = "user_interaction"

**Verification:**
```bash
adb shell "run-as com.augmentalis.voiceos sqlite3 /data/data/com.augmentalis.voiceos/databases/app_scraping.db 'SELECT * FROM element_state_history ORDER BY timestamp DESC LIMIT 10;'"
```

**Pass Criteria:**
- ✅ All state changes recorded
- ✅ Old and new values correct
- ✅ State type correct (checked, expanded, etc.)

---

## Test Suite 2: State-Aware Command Generation

**Objective:** Verify commands are generated based on current element state.

### Test 2.1: Checkbox Commands - Unchecked State

**Prerequisites:**
- App scraped with checkbox in unchecked state
- Checkbox text = "Enable notifications"

**Steps:**
1. Scrape app with unchecked checkbox
2. Call `generateStateAwareCommands(checkboxElement)`
3. Verify generated commands

**Expected Results:**
- Command: "check enable notifications"
- Synonyms: "tick enable notifications", "select enable notifications"
- Confidence: base confidence (no boost yet)
- Action type: "click"

**Verification:**
```bash
adb logcat -s CommandGenerator:V | grep "generateStateAwareCommands"
```

**Pass Criteria:**
- ✅ "check" command generated (not "uncheck")
- ✅ Synonyms included
- ✅ Confidence within [0.0, 1.0]

---

### Test 2.2: Checkbox Commands - Checked State

**Prerequisites:**
- Same checkbox now checked

**Steps:**
1. Check the checkbox
2. Re-scrape app
3. Call `generateStateAwareCommands(checkboxElement)`
4. Verify generated commands

**Expected Results:**
- Command: "uncheck enable notifications"
- Synonyms: "untick enable notifications", "deselect enable notifications"
- Confidence: base confidence
- Action type: "click"

**Pass Criteria:**
- ✅ "uncheck" command generated (not "check")
- ✅ State-aware command correct

---

### Test 2.3: Expandable Commands - Collapsed State

**Prerequisites:**
- App with expandable list item (e.g., Settings categories)

**Steps:**
1. Scrape app with collapsed expandable item
2. Call `generateStateAwareCommands(expandableElement)`
3. Verify generated commands

**Expected Results:**
- Command: "expand [item text]"
- Synonyms: "open [item text]", "show [item text]"
- Action type: "click"

**Pass Criteria:**
- ✅ "expand" command generated

---

### Test 2.4: Expandable Commands - Expanded State

**Prerequisites:**
- Same expandable item now expanded

**Steps:**
1. Expand the item
2. Re-scrape app
3. Call `generateStateAwareCommands(expandableElement)`
4. Verify generated commands

**Expected Results:**
- Command: "collapse [item text]"
- Synonyms: "close [item text]", "hide [item text]"

**Pass Criteria:**
- ✅ "collapse" command generated (not "expand")

---

### Test 2.5: Confidence Boost - Frequency (100+ Interactions)

**Objective:** Verify confidence increases with interaction count.

**Prerequisites:**
- Element with >100 recorded interactions

**Steps:**
1. Simulate 100+ interactions with element (or use existing data)
2. Call `generateInteractionWeightedCommands(element)`
3. Check confidence boost

**Expected Results:**
- Frequency boost = +0.15f (for >100 interactions)
- Total confidence = base + 0.15f (clamped to 1.0)
- Log shows interaction count

**Verification:**
```bash
# Insert test interactions
adb shell "run-as com.augmentalis.voiceos sqlite3 /data/data/com.augmentalis.voiceos/databases/app_scraping.db 'SELECT COUNT(*) FROM user_interactions WHERE element_hash = \"<hash>\";'"

adb logcat -s CommandGenerator:V | grep "Frequency boost"
```

**Pass Criteria:**
- ✅ Confidence boost applied correctly
- ✅ >100 interactions = +0.15f boost
- ✅ 50-100 interactions = +0.10f boost
- ✅ 20-50 interactions = +0.05f boost

---

### Test 2.6: Confidence Penalty - Low Success Rate (<50%)

**Objective:** Verify confidence decreases with low success rate.

**Prerequisites:**
- Element with <50% success rate (many failed interactions)

**Steps:**
1. Create interactions with success=false
2. Ensure success rate <50%
3. Call `generateInteractionWeightedCommands(element)`
4. Check confidence penalty

**Expected Results:**
- Success boost = -0.10f (for <50% success rate)
- Total confidence = base - 0.10f (clamped to 0.0)
- Log shows success rate calculation

**Pass Criteria:**
- ✅ Confidence penalty applied
- ✅ <50% success = -0.10f penalty
- ✅ 50-70% success = -0.05f penalty
- ✅ 70-90% success = 0.0f (neutral)
- ✅ >90% success = +0.05f boost

---

### Test 2.7: Confidence Clamping

**Objective:** Verify confidence stays within [0.0, 1.0].

**Steps:**
1. Create element with very high interaction count (>100) and high success rate (>90%)
2. Base confidence = 0.9f
3. Expected boost = +0.15f (frequency) + 0.05f (success) = +0.20f
4. Call `generateInteractionWeightedCommands(element)`

**Expected Results:**
- Calculated confidence = 0.9 + 0.20 = 1.10
- Clamped confidence = 1.0 (max)

**Pass Criteria:**
- ✅ Confidence never exceeds 1.0
- ✅ Confidence never goes below 0.0

---

## Test Suite 3: CommandManager Integration

**Objective:** Verify static command fallback works correctly.

### Test 3.1: Dynamic Command Found (No Fallback)

**Prerequisites:**
- App scraped with dynamic commands

**Steps:**
1. Say voice command that matches dynamic command (e.g., "click submit")
2. Verify dynamic command executes
3. Verify CommandManager NOT called

**Expected Results:**
- `VoiceCommandProcessor.findMatchingCommand()` returns command
- Command executes via dynamic path
- `tryStaticCommand()` NOT called
- Log: "✓ Dynamic command executed: click submit"

**Pass Criteria:**
- ✅ Dynamic command has priority
- ✅ No unnecessary CommandManager calls

---

### Test 3.2: Static Command Fallback - "go back"

**Prerequisites:**
- App NOT scraped (or dynamic command not found)

**Steps:**
1. Say "go back"
2. Verify fallback to CommandManager
3. Verify navigation occurs

**Expected Results:**
- `findMatchingCommand()` returns null
- `tryStaticCommand("go back", "go back")` called
- Command object created with `source = CommandSource.VOICE`
- `CommandManager.executeCommand()` called
- Device navigates back
- Log: "No dynamic command found for: 'go back', trying static commands"
- Log: "✓ Static command executed successfully: go back"

**Verification:**
```bash
adb logcat -s VoiceCommandProcessor:V CommandManager:V | grep "static"
```

**Pass Criteria:**
- ✅ Static command fallback triggered
- ✅ CommandManager called correctly
- ✅ Navigation occurs
- ✅ Correct log messages

---

### Test 3.3: Static Command Fallback - "volume up"

**Steps:**
1. Say "volume up"
2. Verify fallback to CommandManager
3. Verify volume increases

**Expected Results:**
- CommandManager executes volume up
- Device volume increases
- Log: "✓ Static command executed successfully: volume up"

**Pass Criteria:**
- ✅ Volume increases
- ✅ Static command works globally

---

### Test 3.4: Static Command Fallback - "go home"

**Steps:**
1. Say "go home"
2. Verify fallback to CommandManager
3. Verify navigation to home screen

**Expected Results:**
- Device navigates to home screen
- Static command works in any app

**Pass Criteria:**
- ✅ Home navigation works
- ✅ Global command (no app scraping needed)

---

### Test 3.5: Command Not Found (Neither Dynamic nor Static)

**Prerequisites:**
- Voice input has no matching dynamic or static command

**Steps:**
1. Say nonsense command: "banana fruitcake"
2. Verify both lookups fail
3. Verify error returned

**Expected Results:**
- `findMatchingCommand()` returns null
- `tryStaticCommand()` returns failure
- Final result: "Command not recognized: 'banana fruitcake'"
- Log: "✗ Static command execution failed" or "Command not recognized"

**Pass Criteria:**
- ✅ Graceful failure
- ✅ User-friendly error message
- ✅ No crash

---

### Test 3.6: Command Object Construction

**Objective:** Verify Command object created correctly for static commands.

**Steps:**
1. Trigger static command fallback
2. Check Command object parameters

**Expected Results:**
- `id = normalizedInput` (e.g., "go back")
- `text = normalizedInput`
- `source = CommandSource.VOICE`
- `confidence = 1.0f`
- `timestamp = current time`

**Pass Criteria:**
- ✅ All parameters correct
- ✅ No missing parameters (no compilation errors)

---

## Test Suite 4: Performance & Battery Impact

**Objective:** Validate performance claims.

### Test 4.1: CPU Overhead - Learning Disabled

**Steps:**
1. Disable interaction learning
2. Use Android Profiler to measure CPU usage during accessibility events
3. Measure time from event received to event processed

**Expected Results:**
- Overhead <0.01ms per event (guard clause fast exit)
- No database writes
- Minimal CPU usage

**Pass Criteria:**
- ✅ <0.01ms overhead
- ✅ No noticeable performance impact

---

### Test 4.2: CPU Overhead - Learning Enabled

**Steps:**
1. Enable interaction learning
2. Measure CPU usage during interaction recording
3. Measure time for `recordInteraction()` execution

**Expected Results:**
- ~2ms per interaction (hash calculation + async write)
- Non-blocking (coroutine execution)
- No UI lag

**Pass Criteria:**
- ✅ <5ms per interaction
- ✅ No frame drops
- ✅ Smooth UI

---

### Test 4.3: Battery Impact - 24 Hour Test

**Steps:**
1. Fully charge device to 100%
2. Enable interaction learning
3. Use device normally for 24 hours
4. Measure battery drain

**Expected Results:**
- <0.1% additional drain per day
- Total battery usage similar to baseline (without learning)

**Pass Criteria:**
- ✅ <0.1% additional battery drain
- ✅ No significant impact on battery life

---

### Test 4.4: Memory Overhead - Transient

**Steps:**
1. Use Android Profiler to measure memory usage
2. Track `elementVisibilityTracker` and `elementStateTracker` sizes

**Expected Results:**
- ~800 bytes for visibility tracker
- ~1KB for state tracker
- Total ~2KB transient memory
- Cleared on screen change

**Pass Criteria:**
- ✅ <5KB transient memory
- ✅ Memory released on screen change

---

### Test 4.5: Storage Overhead - 30 Days

**Steps:**
1. Estimate interactions per day (~500)
2. Calculate storage: 500 × 150 bytes × 30 days
3. Check actual database size after 30 days

**Expected Results:**
- ~2.25MB for 30 days
- Acceptable storage usage

**Pass Criteria:**
- ✅ <5MB per month
- ✅ Storage grows linearly

---

## Test Suite 5: Edge Cases & Error Handling

### Test 5.1: Null Source in Accessibility Event

**Steps:**
1. Trigger accessibility event with null source
2. Verify graceful handling

**Expected Results:**
- `val node = event.source ?: return` exits early
- No crash
- No database write
- Log: No error (silent return)

**Pass Criteria:**
- ✅ No crash
- ✅ Graceful handling

---

### Test 5.2: Database Write Failure

**Steps:**
1. Simulate database write failure (disk full, permission denied)
2. Trigger interaction recording

**Expected Results:**
- Exception caught in try-catch
- Log: "Error recording interaction"
- No crash
- User experience unaffected

**Pass Criteria:**
- ✅ No crash
- ✅ Error logged
- ✅ App continues functioning

---

### Test 5.3: Battery Intent Null

**Steps:**
1. Mock `registerReceiver()` to return null
2. Call `getBatteryLevel()`

**Expected Results:**
- Returns 100 (safe default - assume full battery)
- Learning continues
- Log: "Unable to get battery level"

**Pass Criteria:**
- ✅ Safe default
- ✅ No crash

---

### Test 5.4: Concurrent Screen Changes

**Steps:**
1. Rapidly switch between apps
2. Verify state trackers cleared correctly

**Expected Results:**
- `elementVisibilityTracker.clear()` called on screen change
- `elementStateTracker.clear()` called on screen change
- No stale data

**Pass Criteria:**
- ✅ Trackers cleared
- ✅ No memory leaks
- ✅ No stale data

---

## Test Suite 6: Integration Testing

### Test 6.1: End-to-End Flow - Learning from Interactions

**Scenario:** User interacts with app, VoiceOS learns and improves commands.

**Steps:**
1. Enable interaction learning
2. Scrape app (e.g., Gmail)
3. Say "click compose" → fails (not found)
4. Manually click "Compose" button 50 times
5. Re-scrape app
6. Say "click compose"

**Expected Results:**
- After 50 interactions, "compose" command has +0.10f confidence boost
- Command now more likely to match
- Learning improves user experience

**Pass Criteria:**
- ✅ Commands improve with usage
- ✅ Confidence scoring works end-to-end

---

### Test 6.2: End-to-End Flow - State-Aware Commands

**Scenario:** Checkbox state affects generated commands.

**Steps:**
1. Scrape Settings with unchecked "Wi-Fi" toggle
2. Say "check wi-fi" → command exists
3. Check Wi-Fi toggle manually
4. Re-scrape Settings
5. Say "uncheck wi-fi" → command exists

**Expected Results:**
- Commands change based on state
- User doesn't need to know current state
- VoiceOS adapts

**Pass Criteria:**
- ✅ State-aware commands work end-to-end

---

### Test 6.3: End-to-End Flow - Static Command Fallback

**Scenario:** System commands work without scraping.

**Steps:**
1. Install VoiceOS on fresh device (no apps scraped)
2. Say "go back" → works
3. Say "volume up" → works
4. Say "go home" → works

**Expected Results:**
- Static commands work immediately
- No scraping required
- Global functionality

**Pass Criteria:**
- ✅ Static commands work out-of-box

---

## Test Execution Checklist

**Before Testing:**
- [ ] Build successful
- [ ] Android device connected
- [ ] ADB working
- [ ] VoiceOS installed
- [ ] Accessibility service enabled
- [ ] Logcat monitoring started

**During Testing:**
- [ ] Execute all test suites
- [ ] Document failures
- [ ] Capture logcat for failures
- [ ] Take screenshots of issues

**After Testing:**
- [ ] Document test results
- [ ] File bugs for failures
- [ ] Update test plan with actual results
- [ ] Create bug fix tasks

---

## Known Limitations

1. **No Unit Tests for DAOs:** Constructor issues prevented DAO unit tests. Relying on manual testing instead.
2. **No Real-Time Command Updates:** Commands generated during scraping, not dynamically regenerated based on interactions.
3. **Limited State Types:** Only supports checked, selected, enabled, focused, visible, expanded states.

---

## Success Criteria

**Phase 3 is considered successfully tested if:**
- ✅ All Test Suite 1 tests pass (Interaction Recording)
- ✅ All Test Suite 2 tests pass (State-Aware Commands)
- ✅ All Test Suite 3 tests pass (CommandManager Integration)
- ✅ Performance metrics meet expectations (Suite 4)
- ✅ No critical bugs found (Suite 5)
- ✅ End-to-end flows work (Suite 6)

---

## Bug Reporting Template

```markdown
**Bug ID:** BUG-XXXX
**Severity:** Critical / High / Medium / Low
**Test:** [Test ID from this plan]
**Description:** [What happened]
**Expected:** [What should have happened]
**Actual:** [What actually happened]
**Steps to Reproduce:**
1. [Step 1]
2. [Step 2]
3. [Step 3]

**Logcat:**
[Paste relevant logcat output]

**Screenshots:**
[Attach screenshots if applicable]

**Device:** [Device model and Android version]
```

---

## Next Steps

1. **Connect Android Device:** Obtain Android device (API 29+)
2. **Execute Test Plan:** Run all test suites
3. **Document Results:** Fill in actual results for each test
4. **File Bugs:** Create issues for any failures
5. **Fix Bugs:** Address critical and high-priority bugs
6. **Re-test:** Verify fixes work
7. **Sign Off:** Mark Phase 3 as complete and production-ready

---

## Status

**Current Status:** ⏳ BLOCKED - No Android device connected
**Build Status:** ✅ BUILD SUCCESSFUL (app-debug.apk created)
**Ready for Testing:** ✅ YES (pending device)

**Blocking Issue:** No Android device available for manual testing
**Resolution:** Connect Android device or use emulator

---

## References

**IDEADEV Documents:**
- Spec: `ideadev/specs/0001-phase3-interaction-tracking.md`
- Plan: `ideadev/plans/0001-phase3-interaction-tracking.md`
- Review: `ideadev/reviews/0001-phase3-interaction-tracking.md`

**Implementation Documentation:**
- Integration: `docs/Active/Phase3-Integration-Complete-251019-0020.md`
- Changelog: `docs/modules/VoiceOSCore/changelog/changelog-2025-10-251019-0020.md`

**Code Files:**
- `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/accessibility/AccessibilityScrapingIntegration.kt`
- `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/scraping/CommandGenerator.kt`
- `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/voice/VoiceCommandProcessor.kt`

---

**End of Test Plan**
