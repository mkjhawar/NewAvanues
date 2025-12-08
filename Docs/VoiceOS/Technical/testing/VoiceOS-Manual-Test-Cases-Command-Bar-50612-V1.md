# Manual Test Cases - LearnApp Command Bar

**Document Version:** 1.0
**Created:** 2025-12-06
**Author:** VOS4 Testing Team
**Component:** LearnApp Bottom Command Bar (Phase 6)

---

## Overview

This document provides comprehensive manual test scenarios for the LearnApp Bottom Command Bar feature. These tests must be performed on a physical device or emulator with VoiceOS installed and accessibility service enabled.

---

## Prerequisites

### Required Setup
- [x] VoiceOS installed on device/emulator
- [x] Accessibility service enabled for VoiceOS
- [x] SYSTEM_ALERT_WINDOW permission granted
- [x] Test apps installed:
  - Microsoft Teams
  - Any app requiring login
  - Any app requesting permissions
- [x] Device has internet connection (for login tests)

### Test Environment
- **Device:** Android phone/tablet or emulator
- **Android Version:** 8.0+ (API 26+)
- **VoiceOS Version:** 4.x
- **Test Duration:** 2-3 hours for complete suite

---

## Test Scenarios

### Scenario 1: Permission Dialog Auto-Pause

**Objective:** Verify command bar auto-pauses when permission dialog appears

**Test Package:** Microsoft Teams (com.microsoft.teams)

**Steps:**
1. Clear Teams app data (Settings > Apps > Teams > Storage > Clear Data)
2. Launch VoiceOS
3. Tap "Learn New App" or say "Learn Microsoft Teams"
4. Select Microsoft Teams from app list
5. Wait for exploration to start
6. Observe when permission dialog appears (typically within 10 seconds)

**Expected Results:**
- [ ] Command bar appears at bottom of screen (48dp height)
- [ ] Progress percentage shows initial progress (e.g., "15%")
- [ ] When permission dialog appears:
  - [ ] Command bar displays "⚠️ Permission required" message
  - [ ] "Pause" button automatically changes to "Resume"
  - [ ] Exploration stops (no new clicks happening)
  - [ ] Command bar remains visible
- [ ] Underlying permission dialog is fully interactive (not blocked)

**Actions:**
1. Manually grant permission by tapping "Allow" on dialog
2. Wait 2 seconds
3. Tap "Resume" button on command bar

**Post-Resume Verification:**
- [ ] Exploration continues from same point
- [ ] Progress percentage increases
- [ ] Command bar shows "Running..." or similar message
- [ ] No duplicate screens explored

**Completion:**
1. Let exploration run to completion (or stop manually after 5 minutes)
2. Verify final completeness >= 90%

**Notes:**
```
Permission dialog text examples:
- "Teams needs permission to access Photos and Videos"
- "Allow Teams to record audio?"
- "Allow Teams to access your camera?"
```

**Pass Criteria:**
- Auto-pause triggers within 1 second of dialog appearance
- Resume continues without re-exploring completed screens
- Final completeness >= 90%

---

### Scenario 2: Login Screen Auto-Pause

**Objective:** Verify command bar auto-pauses on login screen detection

**Test Package:** Any app requiring login (e.g., banking app, social media)

**Steps:**
1. Ensure test app is logged out
2. Start LearnApp exploration of the app
3. Wait for login screen to appear

**Expected Results:**
- [ ] Command bar detects login screen (keywords: "sign in", "log in", "username", "password")
- [ ] Auto-pause triggered
- [ ] Message displays "Login required"
- [ ] "Pause" button changes to "Resume"

**Actions:**
1. Manually enter credentials
2. Tap login button
3. Wait for login to complete (app home screen loads)
4. Tap "Resume" button

**Post-Resume Verification:**
- [ ] Exploration continues on authenticated screens
- [ ] Login screen not re-explored
- [ ] Protected features now accessible

**Pass Criteria:**
- Login screen detected correctly
- Resume accesses authenticated content
- No infinite loop on login screen

---

### Scenario 3: Manual Pause/Resume

**Objective:** Verify user can manually pause and resume at any time

**Test Package:** Any app (e.g., Settings)

**Steps:**
1. Start exploration of Settings app
2. Wait for 10 screens to be explored (progress ~20-30%)
3. Tap "Pause" button on command bar

**Expected Results:**
- [ ] Exploration immediately stops (no new clicks)
- [ ] "Pause" button changes to "Resume"
- [ ] Progress percentage frozen
- [ ] Current screen remains visible

**Actions:**
1. Wait 30 seconds (verify no auto-resume)
2. Manually navigate to a different app screen
3. Return to VoiceOS
4. Tap "Resume" button

**Post-Resume Verification:**
- [ ] Exploration resumes from exact same progress
- [ ] No duplicate exploration of manual navigation
- [ ] Progress continues incrementing
- [ ] Completion reaches 90%+

**Pass Criteria:**
- Manual pause responds instantly
- Resume preserves exact state
- User actions during pause don't interfere

---

### Scenario 4: Dismiss Command Bar

**Objective:** Verify command bar can be dismissed and restored

**Test Package:** Any app

**Steps:**
1. Start exploration with command bar visible
2. Tap "X" (close) button on command bar

**Expected Results:**
- [ ] Command bar slides down smoothly (200ms animation)
- [ ] Command bar disappears completely
- [ ] Background notification appears in status bar:
  - Text: "LearnApp running in background"
  - Icon: VoiceOS logo
- [ ] Exploration continues in background (verify by watching screen clicks)

**Actions:**
1. Wait 10 seconds (exploration continues)
2. Pull down notification shade
3. Tap VoiceOS LearnApp notification

**Post-Tap Verification:**
- [ ] Command bar re-appears at bottom
- [ ] Progress percentage updated (higher than before dismiss)
- [ ] All buttons functional
- [ ] No memory leak (check Logcat for "ProgressOverlay GC'd")

**Pass Criteria:**
- Smooth animations (no jank)
- Background exploration continues
- Notification restores command bar
- No memory leaks

---

### Scenario 5: Multiple Pause/Resume Cycles

**Objective:** Verify command bar handles repeated pause/resume correctly

**Test Package:** Any large app (e.g., Chrome)

**Steps:**
1. Start exploration
2. Perform the following cycle 5 times:
   - Wait for 5 new screens to be explored
   - Tap "Pause"
   - Wait 5 seconds
   - Tap "Resume"

**Expected Results per Cycle:**
- [ ] Pause response: Instant (< 100ms)
- [ ] Resume response: Instant (< 100ms)
- [ ] No state corruption (progress always increases)
- [ ] No duplicate screens explored
- [ ] Memory stable (check Android Profiler if available)

**Final Verification:**
- [ ] All 5 cycles complete successfully
- [ ] Final completeness >= 90%
- [ ] No crashes or ANRs
- [ ] Logcat shows no ERROR logs related to LearnApp

**Pass Criteria:**
- All cycles complete without errors
- State remains consistent
- Completion target achieved

---

### Scenario 6: App Restart During Pause

**Objective:** Verify pause state survives app restart

**Test Package:** Any app

**Steps:**
1. Start exploration
2. Wait for ~50% progress
3. Tap "Pause" button
4. Verify paused state (no clicks happening)
5. Force close VoiceOS app:
   - Settings > Apps > VoiceOS > Force Stop
6. Restart VoiceOS
7. Navigate to LearnApp section

**Expected Results:**
- [ ] Pause state restored (command bar shows "Resume" button)
- [ ] Progress percentage matches pre-restart value (±5%)
- [ ] App package name preserved
- [ ] Tap "Resume" continues from saved point

**Alternative (if state not restored):**
- [ ] Clear state indication shown
- [ ] User can restart exploration cleanly
- [ ] No corrupted data in database

**Pass Criteria:**
- State restoration works OR clean failure handling
- No database corruption
- Resume possible after restart

---

### Scenario 7: Command Bar Height & Position

**Objective:** Verify command bar visual specifications

**Test Package:** Any app

**Tools Needed:**
- Ruler or screen measurement app
- Layout Inspector (Android Studio) if available

**Steps:**
1. Start exploration with command bar visible
2. Measure command bar height
3. Verify position

**Expected Results:**
- [ ] Height: Exactly 48dp (density-independent pixels)
  - On 160dpi screen: 48 physical pixels
  - On 320dpi screen: 96 physical pixels
  - On 480dpi screen: 144 physical pixels
- [ ] Position: Bottom of screen (aligned to bottom edge)
- [ ] Width: Full screen width (edge-to-edge)
- [ ] Elevation: Above all other content (shadow visible)

**Visual Elements:**
- [ ] Progress bar: Visible, fills according to percentage
- [ ] Progress text: "{percentage}% complete"
- [ ] Pause/Resume button: Clearly labeled
- [ ] Close button: "X" icon visible

**Pass Criteria:**
- All measurements within 2dp tolerance
- Visual elements clear and legible

---

### Scenario 8: Underlying UI Fully Interactive

**Objective:** Verify command bar doesn't block underlying app interactions

**Test Package:** Calculator or any interactive app

**Steps:**
1. Start LearnApp exploration of Calculator
2. While exploration running and command bar visible:
   - Manually tap calculator buttons
   - Verify buttons respond to taps
   - Perform calculation (e.g., 2 + 2 =)

**Expected Results:**
- [ ] All taps register on calculator (not blocked by command bar)
- [ ] Calculator displays results correctly
- [ ] Command bar remains visible above calculator
- [ ] No interference between manual taps and exploration clicks

**Alternative Test:**
1. Start exploration of Chrome browser
2. While command bar visible:
   - Manually scroll web page
   - Tap links
   - Enter text in search box

**Verification:**
- [ ] All manual interactions work normally
- [ ] Exploration continues around manual actions
- [ ] No input blocking or interference

**Pass Criteria:**
- 100% of manual taps register correctly
- No lag or delayed response
- Command bar purely informational (doesn't block)

---

### Scenario 9: Progress Percentage Accuracy

**Objective:** Verify progress percentage reflects actual exploration

**Test Package:** Settings app (known screen count)

**Steps:**
1. Start exploration of Settings
2. Monitor progress percentage and screen count
3. Record data every 10 screens:

| Screens Explored | Progress % | Expected % (approx) |
|------------------|------------|---------------------|
| 10               | ?          | 15-25%             |
| 20               | ?          | 35-45%             |
| 30               | ?          | 55-65%             |
| 40               | ?          | 75-85%             |
| 50               | ?          | 90-100%            |

**Expected Results:**
- [ ] Progress increases monotonically (never decreases)
- [ ] Percentage aligns with screens explored (±10%)
- [ ] Reaches 90%+ at completion
- [ ] Updates in real-time (max 1 second delay)

**Pass Criteria:**
- Progress correlates with screens
- Final completeness >= 90%
- Real-time updates

---

### Scenario 10: Edge Case - Rapid Permission Dialogs

**Objective:** Handle apps with multiple rapid permission requests

**Test Package:** Camera app or media-heavy app

**Steps:**
1. Clear app data for test app
2. Start LearnApp exploration
3. Observe multiple permission dialogs appearing in sequence:
   - Camera permission
   - Microphone permission
   - Storage permission

**Expected Results:**
- [ ] First permission triggers auto-pause
- [ ] Grant first permission
- [ ] Resume triggers automatically or via button
- [ ] Second permission triggers another auto-pause
- [ ] Cycle repeats for each permission

**Verification:**
- [ ] Each permission handled independently
- [ ] No skipped permissions
- [ ] No infinite pause loop
- [ ] Exploration completes after all permissions granted

**Pass Criteria:**
- All permissions processed
- No stuck states
- Completion achieved

---

## Acceptance Criteria Summary

### Must Pass (Critical)
- [ ] Scenario 1: Permission dialog auto-pause (100% pass)
- [ ] Scenario 3: Manual pause/resume (100% pass)
- [ ] Scenario 4: Dismiss and restore (100% pass)
- [ ] Scenario 7: Height & position specifications (100% pass)
- [ ] Scenario 8: Underlying UI interactive (100% pass)

### Should Pass (High Priority)
- [ ] Scenario 2: Login screen detection (80%+ pass)
- [ ] Scenario 5: Multiple cycles (80%+ pass)
- [ ] Scenario 9: Progress accuracy (±10% tolerance)

### Nice to Pass (Medium Priority)
- [ ] Scenario 6: State restoration (OR clean failure)
- [ ] Scenario 10: Rapid permissions (OR graceful degradation)

---

## Bug Reporting Template

If any test fails, report using this template:

```markdown
**Test Scenario:** [Scenario number and name]
**Steps to Reproduce:**
1. [Step 1]
2. [Step 2]
...

**Expected Result:**
[What should happen]

**Actual Result:**
[What actually happened]

**Screenshots:**
[Attach screenshots if relevant]

**Logcat Output:**
[Paste relevant ERROR/WARNING logs]

**Device Info:**
- Device: [Make/Model]
- Android Version: [Version]
- VoiceOS Version: [Version]

**Severity:**
- [ ] Critical (blocks feature)
- [ ] Major (significant impact)
- [ ] Minor (cosmetic/edge case)
```

---

## Performance Benchmarks

### Target Metrics
- **Auto-pause latency:** < 1 second from dialog appearance
- **Manual pause latency:** < 100ms from button tap
- **Resume latency:** < 500ms from button tap
- **Animation duration:** 200ms (slide up/down)
- **Memory overhead:** < 5MB for command bar overlay
- **CPU usage:** < 5% while command bar visible

### Measurement Tools
- Logcat timestamps for latency
- Android Profiler for memory/CPU
- High-speed camera for animation smoothness

---

## Test Execution Log

| Scenario | Tester | Date | Result | Notes |
|----------|--------|------|--------|-------|
| 1        |        |      |        |       |
| 2        |        |      |        |       |
| 3        |        |      |        |       |
| 4        |        |      |        |       |
| 5        |        |      |        |       |
| 6        |        |      |        |       |
| 7        |        |      |        |       |
| 8        |        |      |        |       |
| 9        |        |      |        |       |
| 10       |        |      |        |       |

**Legend:**
- ✅ PASS
- ❌ FAIL
- ⚠️ PARTIAL (specify in notes)
- ⏭️ SKIPPED (specify reason)

---

## Sign-Off

**QA Lead Approval:**

```
Signature: _____________________
Date: _____________________
Result: [ ] APPROVED [ ] NEEDS REVISION
```

**Notes:**
```
[Add any overall comments about test suite execution]
```

---

**End of Manual Test Cases**
