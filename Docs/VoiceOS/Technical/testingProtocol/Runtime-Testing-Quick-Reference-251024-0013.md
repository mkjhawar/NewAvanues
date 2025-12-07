# VOS4 Runtime Testing - Quick Reference Guide

**Document ID:** Runtime-Testing-Quick-Reference-251024-0013
**Created:** 2025-10-24 00:13:00 PDT
**Version:** 1.0.0
**Purpose:** Quick reference for manual runtime testing of VOS4

---

## Prerequisites for Runtime Testing

**Device Requirements:**
- Android 10+ (API 29+)
- Microphone enabled
- USB debugging enabled
- Accessibility services enabled

**Build Requirements:**
- APK compiled successfully
- All known build issues resolved

---

## Installation & Setup (10 minutes)

### Step 1: Install APK
```bash
./gradlew :app:installDebug
# OR
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Step 2: Enable Accessibility Service
1. Open Settings → Accessibility
2. Find "VoiceOS" service
3. Toggle ON
4. Grant all permissions

### Step 3: Grant Required Permissions
- Microphone access
- Overlay permission (draw over other apps)
- Accessibility service permission
- Notification access (if needed)

**Verification:**
- [ ] App appears in launcher
- [ ] Accessibility service enabled
- [ ] All permissions granted
- [ ] No crash on launch

---

## Core Functionality Tests (30 minutes)

### Test 1: Voice Recognition Start/Stop

**Procedure:**
1. Launch VoiceOS app
2. Tap "Start Listening" or say wake word
3. Speak a simple command: "tap"
4. Verify microphone indicator shows active
5. Stop listening

**Expected Result:**
- Microphone turns on
- Voice indicator appears
- Command recognized
- No crashes

**Status:** [ ] PASS [ ] FAIL

**Notes:** ___________________________________

---

### Test 2: Basic Navigation Commands

**Test Commands:**
1. "go home" - Should navigate to home screen
2. "go back" - Should press back button
3. "recent apps" - Should show recent apps
4. "open settings" - Should open Settings app

**Verification:**
- [ ] Home navigation works
- [ ] Back button works
- [ ] Recent apps opens
- [ ] App launching works

**Status:** [ ] PASS [ ] FAIL

---

### Test 3: Tap & Click Commands

**Procedure:**
1. Open any app with buttons
2. Say "show numbers" (if number overlay available)
3. Say "tap [number]" or "click [element]"
4. Verify correct element tapped

**Expected:**
- Numbers appear on clickable elements
- Tap executes on correct element
- UI responds appropriately

**Status:** [ ] PASS [ ] FAIL

---

### Test 4: Text Input Commands

**Procedure:**
1. Open app with text field
2. Tap text field to focus
3. Say "type hello world"
4. Verify text appears

**Commands to Test:**
- "type [text]" - Insert text
- "delete" - Delete character
- "clear text" - Clear field

**Status:** [ ] PASS [ ] FAIL

---

### Test 5: Scroll Commands

**Procedure:**
1. Open long scrollable content
2. Say "scroll down"
3. Say "scroll up"
4. Say "scroll to top"
5. Say "scroll to bottom"

**Verification:**
- [ ] Scroll down works
- [ ] Scroll up works
- [ ] Scroll to top works
- [ ] Scroll to bottom works

**Status:** [ ] PASS [ ] FAIL

---

### Test 6: Voice Cursor (if available)

**Procedure:**
1. Say "show cursor" or enable voice cursor
2. Say "move up", "move down", "move left", "move right"
3. Say "click" to tap at cursor position
4. Say "hide cursor"

**Verification:**
- [ ] Cursor appears
- [ ] Movement responds to commands
- [ ] Click works at cursor location
- [ ] Cursor hides on command

**Status:** [ ] PASS [ ] FAIL

---

## Speech Recognition Engine Tests (20 minutes)

### Test Each Engine

**Engines to Test:**
1. Vosk (offline)
2. Vivoka (if configured)
3. Android STT (online)
4. Whisper (if available)
5. Google Cloud (if configured)

**For Each Engine:**
1. Select engine in settings
2. Speak test command: "go home"
3. Verify recognition accuracy
4. Note response time

**Engine Performance Matrix:**

| Engine | Works | Speed | Accuracy | Notes |
|--------|-------|-------|----------|-------|
| Vosk | [ ] | ___ ms | ___% | _________ |
| Vivoka | [ ] | ___ ms | ___% | _________ |
| Android STT | [ ] | ___ ms | ___% | _________ |
| Whisper | [ ] | ___ ms | ___% | _________ |
| Google Cloud | [ ] | ___ ms | ___% | _________ |

---

## Database & Learning Tests (15 minutes)

### Test 1: Command Learning

**Procedure:**
1. Navigate to an app not previously used
2. Perform actions manually
3. Check if VoiceOS learns commands
4. Verify learned commands in database

**Verification:**
- [ ] UI scraping active
- [ ] Commands generated
- [ ] Database stores interactions

**Status:** [ ] PASS [ ] FAIL

---

### Test 2: Learned Command Execution

**Procedure:**
1. Use app with learned commands
2. Say learned command
3. Verify correct action executes

**Status:** [ ] PASS [ ] FAIL

---

## Performance Tests (20 minutes)

### Test 1: Memory Usage

**Check Memory:**
```bash
adb shell dumpsys meminfo com.augmentalis.voiceos
```

**Baseline Limits:**
- Idle: < 100MB
- Active: < 200MB
- Peak: < 300MB

**Status:** [ ] PASS [ ] FAIL
**Actual:** _____ MB

---

### Test 2: Battery Consumption

**Procedure:**
1. Note battery % at start
2. Use VoiceOS for 30 minutes
3. Note battery % at end
4. Calculate consumption rate

**Acceptable:** < 5% per hour during active use

**Status:** [ ] PASS [ ] FAIL
**Consumption:** _____% per hour

---

### Test 3: Response Time

**Measure Response Times:**
- Voice command to recognition: _____ ms (target: < 500ms)
- Recognition to action: _____ ms (target: < 200ms)
- Total latency: _____ ms (target: < 700ms)

**Status:** [ ] PASS [ ] FAIL

---

## Stress Tests (15 minutes)

### Test 1: Rapid Commands

**Procedure:**
1. Issue 10 commands rapidly (1 per second)
2. Verify all commands execute
3. Check for lag or crashes

**Status:** [ ] PASS [ ] FAIL

---

### Test 2: Long Session

**Procedure:**
1. Use VoiceOS continuously for 15 minutes
2. Monitor for memory leaks
3. Check for performance degradation

**Status:** [ ] PASS [ ] FAIL

---

### Test 3: Background Operation

**Procedure:**
1. Start VoiceOS
2. Use other apps
3. Switch back to VoiceOS commands
4. Verify service still responsive

**Status:** [ ] PASS [ ] FAIL

---

## Known Issues Verification (Current State)

### Expected Issues After Revert

**Issue 1: VoiceOSService SOLID References**
- [ ] Verified present (build fails)
- [ ] Fixed (build succeeds)

**Issue 2: App Theme Colors Missing**
- [ ] Verified present (build fails)
- [ ] Fixed (build succeeds)

**Issue 3: Vivoka AAR Dependency**
- [ ] Issue confirmed (can't build library with AARs)
- [ ] Issue not present
- [ ] Cannot test (earlier build failure)

---

## Critical Path Test (5 minutes)

**Quick smoke test for release validation:**

1. Install app
2. Enable accessibility
3. Say "go home"
4. Say "open settings"
5. Say "go back"

**All 5 steps must pass for release.**

**Status:** [ ] PASS [ ] FAIL

---

## Test Execution Summary

**Date:** [Fill in]
**Tester:** [Fill in]
**Device:** [Model/OS version]
**Build:** [Commit hash]

**Results:**
- Installation: [ ] PASS [ ] FAIL
- Voice Recognition: [ ] PASS [ ] FAIL
- Navigation: [ ] PASS [ ] FAIL
- Tap/Click: [ ] PASS [ ] FAIL
- Text Input: [ ] PASS [ ] FAIL
- Scroll: [ ] PASS [ ] FAIL
- Voice Cursor: [ ] PASS [ ] FAIL [ ] N/A
- Speech Engines: [ ] PASS [ ] FAIL
- Learning: [ ] PASS [ ] FAIL
- Performance: [ ] PASS [ ] FAIL
- Stress Tests: [ ] PASS [ ] FAIL
- Critical Path: [ ] PASS [ ] FAIL

**Overall Status:** [ ] READY FOR RELEASE [ ] NEEDS FIXES

**Critical Issues Found:**
```
[List any critical issues here]
```

---

## Next Steps

**If All Tests Pass:**
1. Update test log with PASS status
2. Tag commit as tested
3. Proceed to release process

**If Tests Fail:**
1. Document all failures
2. Create issues for each failure
3. Prioritize fixes
4. Re-test after fixes

---

**Document Status:** ✅ Active
**For Full Details:** See complete runtime testing protocol documentation
**Owner:** VOS4 QA Team
