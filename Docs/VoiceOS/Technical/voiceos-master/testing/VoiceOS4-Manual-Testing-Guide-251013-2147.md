# VoiceOS 4 Manual Testing Guide - Complete

**Created:** 2025-10-13 21:47 PDT
**Version:** 3.0.0
**Status:** Production Testing
**Note:** VoiceOS 4 (shortform: vos4) is a voice-controlled Android accessibility system

---

## Table of Contents

1. [Testing Overview](#testing-overview)
2. [Pre-Test Setup](#pre-test-setup)
3. [Test Environment Requirements](#test-environment-requirements)
4. [Feature Testing](#feature-testing)
5. [Integration Testing](#integration-testing)
6. [Performance Testing](#performance-testing)
7. [Edge Case Testing](#edge-case-testing)
8. [Regression Testing](#regression-testing)
9. [Test Results Documentation](#test-results-documentation)
10. [Appendix](#appendix)

---

## Testing Overview

### Purpose

This manual testing guide provides **step-by-step procedures** to verify all VoiceOS 4 features work correctly in production. Each test includes:

- ✅ **Prerequisites**: What you need before testing
- ✅ **Test Steps**: Detailed step-by-step instructions
- ✅ **Expected Results**: What should happen
- ✅ **Actual Results**: What you observe (fill in during testing)
- ✅ **Pass/Fail Criteria**: How to determine success
- ✅ **Notes**: Additional observations

### Testing Scope

**Core Features to Test:**
1. Installation and Setup
2. Accessibility Service Integration
3. Speech Recognition (all engines)
4. System Commands (Tier 1)
5. App Commands (Tier 2)
6. Legacy Commands (Tier 3)
7. Web Command Coordination
8. UI Scraping and Learning
9. Database Persistence
10. Cross-Session Command Stability
11. Performance and Resource Usage
12. Edge Cases and Error Handling

### Test Devices

**Minimum Test Coverage:**
- ✅ 1 device with Android 10
- ✅ 1 device with Android 12+
- ✅ 1 phone (small screen)
- ✅ 1 tablet (large screen)

**Ideal Test Coverage:**
- ✅ Android 10, 11, 12, 13, 14 (one device each)
- ✅ Different manufacturers (Samsung, Google Pixel, OnePlus, etc.)
- ✅ Different screen sizes and resolutions

---

## Pre-Test Setup

### Test Device Preparation

**Step 1: Factory Reset (Optional but Recommended)**
```
Settings → System → Reset → Factory data reset
```
*Why:* Clean slate ensures no interference from previous installations

**Step 2: Enable Developer Options**
```
Settings → About phone → Tap "Build number" 7 times
```

**Step 3: Enable USB Debugging**
```
Settings → System → Developer options → USB debugging → ON
```

**Step 4: Keep Screen Awake**
```
Settings → System → Developer options → Stay awake → ON
```
*Why:* Prevents screen timeout during testing

**Step 5: Disable Battery Optimization for VoiceOS 4**
```
Settings → Apps → VoiceOS 4 → Battery → Unrestricted
```
*Why:* Prevents Android from killing the service

### ADB Setup (for logging and debugging)

**Connect device:**
```bash
# Connect device via USB
adb devices

# Should show:
# List of devices attached
# ABCD1234    device
```

**Start logging:**
```bash
# Start logcat with VoiceOS 4 filter
adb logcat | grep -i "VoiceOS\|CommandManager\|VoiceCommand\|WebCommand"

# Save to file for later analysis
adb logcat > voiceos_test_$(date +%Y%m%d_%H%M%S).log
```

### Test Data Preparation

**Install Required Apps:**
```
1. Chrome browser (for web testing)
2. Gmail (for app command testing)
3. Calculator (for system testing)
4. YouTube (for web command testing)
5. Any 2-3 additional apps of your choice
```

**Prepare Test Accounts:**
```
- Google account (for Gmail, YouTube)
- Test website URLs ready:
  - https://www.google.com
  - https://www.youtube.com
  - https://www.amazon.com (or any e-commerce site)
```

### Test Checklist Template

**Create a testing spreadsheet or document with columns:**
```
| Test ID | Feature | Test Case | Expected Result | Actual Result | Pass/Fail | Notes |
```

---

## Test Environment Requirements

### Hardware Requirements

**Minimum:**
- Android device with Android 10+
- Working microphone
- Speaker/headphones for audio feedback
- 2GB+ RAM
- 1GB free storage

**Recommended:**
- Bluetooth headset with microphone (for hands-free testing)
- Quiet testing environment
- Charger connected (long testing sessions)

### Software Requirements

**Installed:**
- VoiceOS 4 (latest version)
- ADB tools (for logging)
- Android Studio (optional, for database inspection)

**Version Numbers to Record:**
```
VoiceOS 4 Version: ___________
Android Version: ___________
Device Model: ___________
Build Number: ___________
```

---

## Feature Testing

### TEST SUITE 1: Installation and Setup

#### Test 1.1: APK Installation

**Test ID:** INST-001
**Feature:** Application Installation
**Priority:** Critical

**Prerequisites:**
- APK file available
- Device has "Install from unknown sources" enabled (if sideloading)

**Test Steps:**
```
1. Navigate to APK location or Google Play Store
2. Tap "Install"
3. Wait for installation to complete
4. Verify app appears in app drawer
5. Open VoiceOS 4 from app drawer
```

**Expected Results:**
- ✅ Installation completes without errors
- ✅ App icon visible in app drawer
- ✅ App opens successfully
- ✅ No crash on first launch

**Pass Criteria:**
- App installs successfully
- App opens without crashing
- No error messages displayed

**Test Template:**
```
Actual Results: _______________________
Pass/Fail: [ ] Pass [ ] Fail
Notes: _______________________
```

#### Test 1.2: Accessibility Service Enablement

**Test ID:** INST-002
**Feature:** Accessibility Permission
**Priority:** Critical

**Prerequisites:**
- VoiceOS 4 installed
- Device unlocked

**Test Steps:**
```
1. Open Android Settings
2. Navigate to Accessibility
3. Scroll to find "VoiceOS 4" in services list
4. Tap on "VoiceOS 4"
5. Toggle switch to ON
6. Read permission dialog carefully
7. Tap "Allow" or "OK"
8. Verify toggle stays ON
9. Return to VoiceOS 4 app
10. Verify app shows "Service Connected" or similar status
```

**Expected Results:**
- ✅ VoiceOS 4 appears in Accessibility services list
- ✅ Permission dialog displays explaining what service can do
- ✅ Toggle stays enabled after granting permission
- ✅ VoiceOS 4 app reflects service is connected
- ✅ No system crashes or errors

**Verification:**
```bash
# Via ADB - Check service is running
adb shell dumpsys accessibility | grep -i "VoiceOS"

# Should show:
# Service[...VoiceOSService...]: running
```

**Pass Criteria:**
- Accessibility service enables successfully
- App reflects connected status
- Service remains running after 5 minutes

**Common Issues to Check:**
- [ ] Toggle switches on but immediately turns off (permission denied)
- [ ] App doesn't detect service connection (restart app)
- [ ] System dialog doesn't appear (check Android version compatibility)

#### Test 1.3: Microphone Permission

**Test ID:** INST-003
**Feature:** Microphone Access
**Priority:** Critical

**Prerequisites:**
- VoiceOS 4 installed
- Accessibility service enabled

**Test Steps:**
```
1. Open VoiceOS 4 app
2. Tap "Start Voice Control" or similar button
3. System requests microphone permission
4. Tap "Allow" (not "Allow only while using the app")
5. Verify permission granted in app UI
6. Test by speaking: "Hello test"
7. Verify app shows it's listening (visual indicator)
8. Check app settings to confirm microphone permission
```

**Expected Results:**
- ✅ Permission dialog appears
- ✅ Both "Allow" and "While using" options visible
- ✅ Permission granted successfully
- ✅ App shows listening indicator when active
- ✅ Microphone captures audio

**Verification:**
```
Settings → Apps → VoiceOS 4 → Permissions → Microphone
Should show: "Allowed"
```

**Pass Criteria:**
- Microphone permission granted
- App can access microphone
- Audio input captured successfully

---

### TEST SUITE 2: Speech Recognition

#### Test 2.1: Google Speech Engine

**Test ID:** SPEECH-001
**Feature:** Google Speech Recognition
**Priority:** Critical

**Prerequisites:**
- Internet connection available
- VoiceOS 4 fully set up
- Microphone working

**Test Steps:**
```
1. Open VoiceOS 4 settings
2. Navigate to "Speech Engine" settings
3. Select "Google Speech"
4. Return to home screen
5. Wait 2 seconds for engine to initialize
6. Speak clearly: "Go home"
7. Observe device response
8. Wait for home screen to appear
```

**Expected Results:**
- ✅ Engine selection saves successfully
- ✅ No errors after selection
- ✅ Speech recognized within 1-2 seconds
- ✅ Command executed (home screen appears)
- ✅ Audio feedback (optional, if enabled)

**Test Multiple Phrases:**

**Test 2.1a: Clear Speech**
```
Phrase: "Go home"
Expected: Navigate to home screen
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 2.1b: Speech with Background Noise**
```
Environment: Play music at low volume in background
Phrase: "Volume up"
Expected: Volume increases
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 2.1c: Fast Speech**
```
Phrase: "Go home" (spoken quickly)
Expected: Still recognizes and executes
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 2.1d: Accented Speech**
```
Phrase: "Open calculator" (with accent)
Expected: Opens calculator app
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 2.1e: Low Confidence Rejection**
```
Phrase: "Mumble mumblemumble" (intentionally unclear)
Expected: Command rejected (confidence < 0.5)
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Verification via Logs:**
```bash
# Check recognition results
adb logcat | grep "onRecognitionResult"

# Should show:
# onRecognitionResult: text="go home", confidence=0.95
```

**Pass Criteria:**
- Recognition accuracy ≥ 90% in quiet environment
- Recognition accuracy ≥ 70% with background noise
- Response time < 2 seconds
- Low confidence commands properly rejected

#### Test 2.2: Vosk Speech Engine (Offline)

**Test ID:** SPEECH-002
**Feature:** Vosk Offline Recognition
**Priority:** High

**Prerequisites:**
- VoiceOS 4 fully set up
- Vosk models downloaded
- **NO internet connection** (disable WiFi and mobile data)

**Test Steps:**
```
1. Disable WiFi and mobile data
2. Open VoiceOS 4 settings
3. Select "Vosk" as speech engine
4. Wait 5 seconds for model loading
5. Return to home screen
6. Speak: "Go home"
7. Verify command executes WITHOUT internet
8. Re-enable internet
```

**Expected Results:**
- ✅ Vosk engine loads successfully
- ✅ No "No internet" error
- ✅ Speech recognized offline
- ✅ Command executed correctly
- ✅ Performance acceptable (< 3 seconds)

**Offline Testing Scenarios:**

**Test 2.2a: Airplane Mode**
```
Setup: Enable airplane mode
Phrase: "Volume up"
Expected: Volume increases (no internet needed)
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 2.2b: Multiple Commands Offline**
```
Test 5 different commands in airplane mode:
1. "Go home" → Pass/Fail: ___
2. "Go back" → Pass/Fail: ___
3. "Volume up" → Pass/Fail: ___
4. "Recent apps" → Pass/Fail: ___
5. "Open calculator" → Pass/Fail: ___
```

**Pass Criteria:**
- All commands work without internet
- No internet-related errors
- Recognition accuracy ≥ 80% offline

#### Test 2.3: Speech Engine Switching

**Test ID:** SPEECH-003
**Feature:** Engine Switching
**Priority:** Medium

**Prerequisites:**
- Multiple engines available (Google, Vosk)

**Test Steps:**
```
1. Start with Google engine active
2. Execute command: "Go home" → Record success/fail
3. Open settings, switch to Vosk
4. Wait 3 seconds for engine reload
5. Execute same command: "Go home" → Record success/fail
6. Switch back to Google
7. Execute command again → Record success/fail
8. Verify no crashes during switches
```

**Expected Results:**
- ✅ Switch happens without crash
- ✅ Commands work with both engines
- ✅ No lingering effects from previous engine
- ✅ Settings persist after restart

**Pass Criteria:**
- Can switch between engines successfully
- Commands work after each switch
- No memory leaks or crashes

---

### TEST SUITE 3: System Commands (Tier 1)

#### Test 3.1: Navigation Commands

**Test ID:** TIER1-001
**Feature:** System Navigation
**Priority:** Critical

**Prerequisites:**
- VoiceOS 4 active
- Device on any screen (not home)

**Test Cases:**

**Test 3.1a: Go Home**
```
Current Screen: Settings app (or any app)
Command: "Go home"
Expected Result: Device navigates to home screen
Execution Time: < 100ms

Test Steps:
1. Open Settings app
2. Verify you're in Settings (not home)
3. Say "Go home"
4. Start timer
5. Wait for home screen
6. Stop timer

Actual Result: _______________________
Execution Time: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 3.1b: Go Back**
```
Current Screen: Settings → Display → Brightness (deep in navigation)
Command: "Go back"
Expected Result: Returns to Display settings
Repeat: Say "Go back" again → Returns to Settings main

Test Steps:
1. Navigate: Settings → Display → Brightness
2. Say "Go back"
3. Verify returned to Display screen
4. Say "Go back" again
5. Verify returned to Settings main

Actual Result: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 3.1c: Recent Apps**
```
Current Screen: Home
Command: "Recent apps"
Expected Result: Recent apps view appears

Test Steps:
1. Ensure you have 2-3 apps recently opened
2. Go to home screen
3. Say "Recent apps"
4. Verify recent apps screen appears
5. Verify correct apps shown

Actual Result: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 3.1d: Open Notifications**
```
Current Screen: Home
Command: "Open notifications"
Expected Result: Notification shade pulls down

Test Steps:
1. Ensure you have at least 1 notification
2. Go to home screen
3. Say "Open notifications"
4. Verify notification shade opens
5. Check notifications are visible

Actual Result: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 3.1e: Quick Settings**
```
Current Screen: Home
Command: "Quick settings"
Expected Result: Quick settings panel appears

Test Steps:
1. Go to home screen
2. Say "Quick settings"
3. Verify quick settings panel opens
4. Verify toggles visible (WiFi, Bluetooth, etc.)

Actual Result: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Verification via Logs:**
```bash
# Check Tier 1 execution
adb logcat | grep "Tier 1 executed"

# Should show:
# ✓ Tier 1 executed: go home (time: 20ms)
```

**Performance Requirements:**
- Go Home: < 100ms
- Go Back: < 100ms
- Recent Apps: < 150ms
- Notifications: < 100ms
- Quick Settings: < 100ms

#### Test 3.2: Volume Control

**Test ID:** TIER1-002
**Feature:** Volume Commands
**Priority:** High

**Prerequisites:**
- Device volume not at maximum or minimum
- Media playing (optional, for media volume test)

**Test Cases:**

**Test 3.2a: Volume Up**
```
Setup:
1. Set system volume to 50%
2. Note current volume level
3. Say "Volume up"
4. Check new volume level

Expected: Volume increases by 1 step
Actual Volume Before: _______
Actual Volume After: _______
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 3.2b: Volume Down**
```
Setup:
1. Set system volume to 50%
2. Say "Volume down"
3. Check volume decreased

Expected: Volume decreases by 1 step
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 3.2c: Volume Up at Maximum**
```
Setup:
1. Set volume to 100% (maximum)
2. Say "Volume up"
3. Verify no crash, stays at max

Expected: Volume stays at maximum, no error
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 3.2d: Volume Down at Minimum**
```
Setup:
1. Set volume to 0% (minimum)
2. Say "Volume down"
3. Verify no crash, stays at min

Expected: Volume stays at minimum, no error
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 3.2e: Mute/Unmute**
```
Test Steps:
1. Set volume to 50%
2. Say "Mute"
3. Verify volume is muted (or reduced to 0)
4. Say "Unmute"
5. Verify volume restored

Mute Result: _______________________
Unmute Result: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 3.2f: Rapid Volume Commands**
```
Test rapid commands:
1. Say "Volume up" 5 times rapidly
2. Each command should execute

Expected: Volume increases 5 steps
Actual: Volume increased ___ steps
Pass/Fail: [ ] Pass [ ] Fail
```

#### Test 3.3: Media Control

**Test ID:** TIER1-003
**Feature:** Media Playback Control
**Priority:** High

**Prerequisites:**
- Music/media app installed (Spotify, YouTube Music, etc.)
- Media ready to play

**Test Cases:**

**Test 3.3a: Play Music**
```
Setup:
1. Open music app
2. Queue a song but don't play
3. Say "Play music"

Expected: Music starts playing
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 3.3b: Pause Music**
```
Setup:
1. Have music playing
2. Say "Pause music"

Expected: Music pauses
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 3.3c: Next Song**
```
Setup:
1. Have playlist with multiple songs
2. Play first song
3. Say "Next song"

Expected: Skips to next track
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 3.3d: Previous Song**
```
Setup:
1. Play second song in playlist
2. Say "Previous song"

Expected: Returns to first song
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 3.3e: Media Control in Background**
```
Setup:
1. Start music playing
2. Go to home screen (music in background)
3. Say "Pause music"

Expected: Music pauses even though app in background
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

#### Test 3.4: App Launching

**Test ID:** TIER1-004
**Feature:** Open Applications
**Priority:** Critical

**Prerequisites:**
- Test apps installed: Chrome, Gmail, Calculator, Settings

**Test Cases:**

**Test 3.4a: Open Chrome**
```
Current Screen: Home
Command: "Open Chrome"
Expected: Chrome browser launches

Test Steps:
1. Close Chrome if open
2. Go to home screen
3. Say "Open Chrome"
4. Measure time to app opened

Actual: _______________________
Launch Time: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 3.4b: Open Gmail**
```
Command: "Open Gmail"
Expected: Gmail app launches
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 3.4c: Open Calculator**
```
Command: "Open Calculator"
Expected: Calculator app launches
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 3.4d: Open Settings**
```
Command: "Open Settings"
Expected: System settings opens
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 3.4e: Open Non-Existent App**
```
Command: "Open XYZ123NonExistent"
Expected: Command fails gracefully, no crash
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 3.4f: Open App with Similar Name**
```
Setup: Have "Messenger" and "Messages" installed
Command: "Open Messenger"
Expected: Correct app opens (Messenger, not Messages)
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Performance Requirements:**
- App launch time: < 2 seconds from command to app visible

---

### TEST SUITE 4: App Commands (Tier 2)

#### Test 4.1: UI Element Learning

**Test ID:** TIER2-001
**Feature:** Automatic Command Learning
**Priority:** Critical

**Prerequisites:**
- Fresh VoiceOS 4 install (or clear app data)
- Calculator app installed

**Test Steps:**
```
1. Clear VoiceOS 4 app data:
   Settings → Apps → VoiceOS 4 → Storage → Clear data

2. Re-enable accessibility service

3. Open Calculator app

4. Wait 10 seconds (let VoiceOS 4 scrape the UI)

5. Close Calculator

6. Check database for learned commands:
   adb shell "run-as com.augmentalis.voiceaccessibility \
   sqlite3 databases/app_scraping_database \
   'SELECT commandPhrase FROM generated_commands;'"

7. Reopen Calculator

8. Try saying: "Click one"

9. Try saying: "Click plus"

10. Try saying: "Click equals"
```

**Expected Results:**
- ✅ Calculator UI scraped automatically
- ✅ Commands generated for buttons (0-9, +, -, *, /, =)
- ✅ Commands work when spoken
- ✅ No manual configuration required

**Verification Checklist:**
```
[ ] Commands generated for "one" button
[ ] Commands generated for "plus" button
[ ] Commands generated for "equals" button
[ ] "Click one" works when spoken
[ ] "Click plus" works when spoken
[ ] "Click equals" works when spoken
```

**Database Verification:**
```bash
# Check scraped elements count
adb shell "run-as com.augmentalis.voiceaccessibility \
sqlite3 databases/app_scraping_database \
'SELECT COUNT(*) FROM scraped_elements WHERE packageName=\"com.android.calculator2\";'"

# Should show: 15-20 elements (calculator buttons)
```

**Pass Criteria:**
- At least 10 commands generated for Calculator
- Commands execute successfully
- Learning happens automatically without user action

#### Test 4.2: App Command Execution

**Test ID:** TIER2-002
**Feature:** Execute Learned Commands
**Priority:** Critical

**Prerequisites:**
- Calculator app learned (from Test 4.1)
- VoiceOS 4 active

**Test Cases:**

**Test 4.2a: Simple Calculation**
```
Goal: Calculate 5 + 3

Test Steps:
1. Open Calculator
2. Say "Click five" → Verify 5 appears
3. Say "Click plus" → Verify + appears
4. Say "Click three" → Verify 3 appears
5. Say "Click equals" → Verify result = 8

Expected Result: 8 displayed
Actual Result: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 4.2b: Complex Calculation**
```
Goal: Calculate (12 + 5) * 3

Test Steps:
1. Open Calculator
2. Say "Click one"
3. Say "Click two" → Shows 12
4. Say "Click plus"
5. Say "Click five" → Shows 5
6. Say "Click equals" → Shows 17
7. Say "Click multiply"
8. Say "Click three"
9. Say "Click equals" → Should show 51

Expected Result: 51
Actual Result: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 4.2c: Clear Button**
```
Test Steps:
1. Enter some numbers
2. Say "Click clear" (or "Click C")
3. Verify calculator cleared

Expected: Display shows 0
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 4.2d: Command Variations**
```
Test different phrasings:
1. "Click plus" → Pass/Fail: ___
2. "Tap plus" → Pass/Fail: ___
3. "Press plus" → Pass/Fail: ___

All variations should work
```

**Performance:**
- Time from command to button click: < 300ms

#### Test 4.3: Gmail App Commands

**Test ID:** TIER2-003
**Feature:** Email App Control
**Priority:** High

**Prerequisites:**
- Gmail installed and logged in
- At least 1 email in inbox

**Test Steps:**
```
1. Clear VoiceOS 4 learning data (fresh start)
2. Open Gmail
3. Wait 15 seconds (learning)
4. Navigate around: Inbox, Sent, Menu
5. Close Gmail and reopen
6. Test commands
```

**Test Cases:**

**Test 4.3a: Compose Email**
```
Current Screen: Gmail inbox
Command: "Click compose"
Expected: New email screen opens
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 4.3b: Open Menu**
```
Current Screen: Gmail inbox
Command: "Click menu" (or "Click hamburger")
Expected: Side menu opens
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 4.3c: Search**
```
Command: "Click search"
Expected: Search bar focused/keyboard appears
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 4.3d: Navigate to Sent**
```
Setup: Open Gmail menu
Command: "Click sent"
Expected: Sent folder opens
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 4.3e: Select Email**
```
Setup: In inbox with multiple emails
Command: "Click first email" (if learned)
Expected: Email opens
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
Notes: May need to try "Click [email subject]" instead
```

#### Test 4.4: Cross-App Command Persistence

**Test ID:** TIER2-004
**Feature:** Commands Persist Across Sessions
**Priority:** Critical

**Prerequisites:**
- Calculator commands learned (Test 4.1 completed)

**Test Steps:**
```
1. Verify Calculator commands work
2. Force stop VoiceOS 4:
   Settings → Apps → VoiceOS 4 → Force Stop
3. Restart VoiceOS 4
4. Re-enable accessibility service
5. Open Calculator
6. Try same commands: "Click one", "Click plus"
7. Verify commands still work
```

**Expected Results:**
- ✅ Commands work after restart
- ✅ No re-learning required
- ✅ Same performance as before restart

**Test Cases:**

**Test 4.4a: After Service Restart**
```
Test: "Click five" in Calculator after restart
Expected: Still works
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 4.4b: After Device Reboot**
```
Test Steps:
1. Reboot device
2. Re-enable VoiceOS 4 accessibility
3. Open Calculator
4. Say "Click one"

Expected: Command still works (no re-learning)
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 4.4c: Database Integrity Check**
```bash
# Before restart - count commands
adb shell "run-as com.augmentalis.voiceaccessibility \
sqlite3 databases/app_scraping_database \
'SELECT COUNT(*) FROM generated_commands;'"
# Record count: _______

# After restart - verify same count
# Count after restart: _______
# Should be identical
```

**Pass Criteria:**
- Commands work after service restart
- Commands work after device reboot
- Database retains all learned commands

---

### TEST SUITE 5: Web Command Coordination

#### Test 5.1: Browser Detection

**Test ID:** WEB-001
**Feature:** Detect Browser Apps
**Priority:** High

**Prerequisites:**
- Chrome browser installed
- Internet connection

**Test Steps:**
```
1. Open Chrome browser
2. Navigate to google.com
3. Check VoiceOS 4 logs for browser detection:
   adb logcat | grep "isCurrentAppBrowser"
4. Verify browser detected
```

**Expected Results:**
- ✅ Chrome detected as browser
- ✅ Web command mode activated
- ✅ Logs show: "Browser detected: com.android.chrome"

**Test Cases:**

**Test 5.1a: Chrome Detection**
```
App: Chrome
Package: com.android.chrome
Expected: Detected as browser
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 5.1b: Firefox Detection**
```
App: Firefox (if installed)
Package: org.mozilla.firefox
Expected: Detected as browser
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 5.1c: Non-Browser App**
```
App: Gmail
Expected: NOT detected as browser
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

#### Test 5.2: URL Extraction

**Test ID:** WEB-002
**Feature:** Extract Current URL from Browser
**Priority:** Critical

**Prerequisites:**
- Chrome installed
- Internet connected

**Test Steps:**
```
1. Open Chrome
2. Navigate to https://www.google.com
3. Wait 2 seconds for page load
4. Check logs:
   adb logcat | grep "getCurrentURL"
5. Verify URL extracted
```

**Expected Results:**
- ✅ URL detected: "https://www.google.com"
- ✅ URL bar found via accessibility
- ✅ No errors in extraction

**Test Cases:**

**Test 5.2a: Simple URL**
```
Navigate to: https://www.google.com
Expected URL: "https://www.google.com" or "google.com"
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 5.2b: Complex URL**
```
Navigate to: https://www.amazon.com/s?k=headphones
Expected: URL extracted correctly
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 5.2c: HTTPS vs HTTP**
```
Test both:
- https://www.google.com
- http://example.com
Both should extract correctly
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 5.2d: URL with Subdomain**
```
Navigate to: https://mail.google.com
Expected: Full URL extracted
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

#### Test 5.3: Web Command Learning (LearnWeb)

**Test ID:** WEB-003
**Feature:** Learn Website Commands
**Priority:** Critical

**Prerequisites:**
- LearnWeb app installed (part of VoiceOS 4)
- Chrome installed
- Internet connection

**Test Steps:**
```
1. Open LearnWeb app

2. In URL bar, enter: https://www.google.com

3. Tap "Learn" or navigate to the site

4. Wait for page to load fully (5 seconds)

5. Click the following elements manually:
   - Google Search button
   - Images link
   - Gmail link (if visible)

6. Each click should be captured by LearnWeb

7. See message: "Learned 3 elements" or similar

8. Close LearnWeb

9. Open regular Chrome

10. Navigate to https://www.google.com

11. Try voice command: "Click images"

12. Verify Images page opens
```

**Expected Results:**
- ✅ LearnWeb captures manual clicks
- ✅ Elements saved to WebScrapingDatabase
- ✅ Commands generated automatically
- ✅ Commands work in regular browser

**Verification:**
```bash
# Check web commands learned
adb shell "run-as com.augmentalis.voiceaccessibility \
sqlite3 databases/web_scraping_database \
'SELECT commandPhrase FROM generated_web_commands;'"

# Should show commands like:
# "click images"
# "click gmail"
# "click search"
```

**Test Cases:**

**Test 5.3a: Learn Google.com**
```
Website: https://www.google.com
Elements to Learn:
1. Search button → "click search"
2. Images link → "click images"
3. Gmail link → "click gmail"

Commands Learned: ___/3
Commands Working: ___/3
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 5.3b: Learn YouTube.com**
```
Website: https://www.youtube.com
Elements to Learn:
1. Search button
2. Trending button
3. Subscriptions

Commands Learned: ___/3
Commands Working: ___/3
Pass/Fail: [ ] Pass [ ] Fail
```

#### Test 5.4: Web Command Execution

**Test ID:** WEB-004
**Feature:** Execute Web Commands
**Priority:** Critical

**Prerequisites:**
- Google.com commands learned (Test 5.3 completed)
- Chrome browser open

**Test Cases:**

**Test 5.4a: Click Link**
```
Setup: Navigate to https://www.google.com
Command: "Click images"
Expected: Google Images page opens
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 5.4b: Click Button**
```
Setup: On google.com with search query entered
Command: "Click search button"
Expected: Search executes
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 5.4c: Multiple Commands in Sequence**
```
Test Steps:
1. On google.com
2. Say "Click images"
3. Wait for Images page
4. Say "Go back" (browser back)
5. Should return to google.com

All steps work: Pass/Fail: [ ] Pass [ ] Fail
```

**Test 5.4d: Click on YouTube Video**
```
Setup: Learn YouTube.com, navigate to site
Command: "Click first video" (if learned)
Expected: Video starts playing
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 5.4e: Web Command with Fuzzy Matching**
```
Learned Command: "click search button"
Test Variations:
1. "Click search" → Pass/Fail: ___
2. "Tap search" → Pass/Fail: ___
3. "Press search button" → Pass/Fail: ___

At least 2/3 should work (fuzzy matching)
```

#### Test 5.5: URL Navigation

**Test ID:** WEB-005
**Feature:** Voice URL Navigation
**Priority:** Medium

**Prerequisites:**
- URLBarInteractionManager implemented
- Chrome open

**Test Steps:**
```
1. Open Chrome
2. Say "Click address bar" (or it focuses automatically)
3. Use keyboard to type URL (voice typing may work)
4. Say "Go" or press enter
5. Verify navigation occurs
```

**Test Cases:**

**Test 5.5a: Navigate to URL**
```
Test Steps:
1. Focus address bar
2. Type: youtube.com
3. Press enter
4. Verify YouTube opens

Expected: YouTube loads
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 5.5b: Navigate with Voice Typing**
```
Test Steps:
1. Click address bar
2. Use Google voice typing: "amazon.com"
3. Say "Go" or press enter

Expected: Amazon opens
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

---

### TEST SUITE 6: Database Persistence

#### Test 6.1: Command Database Persistence

**Test ID:** DB-001
**Feature:** Database Command Storage
**Priority:** Critical

**Prerequisites:**
- VoiceOS 4 installed
- Commands learned or added

**Test Steps:**
```
1. Use VoiceOS 4 normally (learn some commands)

2. Check database has entries:
   adb shell "run-as com.augmentalis.voiceaccessibility \
   sqlite3 databases/command_database \
   'SELECT COUNT(*) FROM vos_commands;'"

3. Record count: _______

4. Force stop VoiceOS 4

5. Restart VoiceOS 4

6. Check database again (same query)

7. Record count: _______

8. Verify counts match
```

**Expected Results:**
- ✅ Database persists after restart
- ✅ Same number of commands
- ✅ No data loss

**Test Cases:**

**Test 6.1a: After App Restart**
```
Commands Before Restart: _______
Commands After Restart: _______
Match: [ ] Yes [ ] No
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 6.1b: After Device Reboot**
```
Commands Before Reboot: _______
Commands After Reboot: _______
Match: [ ] Yes [ ] No
Pass/Fail: [ ] Pass [ ] Fail
```

#### Test 6.2: Hash Stability

**Test ID:** DB-002
**Feature:** Element Hash Persistence
**Priority:** Critical

**Purpose:** Verify hash-based IDs remain stable across sessions

**Test Steps:**
```
1. Open Calculator

2. Query element hash for "1" button:
   adb shell "run-as com.augmentalis.voiceaccessibility \
   sqlite3 databases/app_scraping_database \
   'SELECT elementHash FROM scraped_elements WHERE text=\"1\";'"

3. Record hash: _______________________

4. Close Calculator

5. Force stop VoiceOS 4

6. Reboot device

7. Re-enable VoiceOS 4

8. Open Calculator

9. Query same element hash again

10. Record hash: _______________________

11. Compare: Should be IDENTICAL
```

**Expected Results:**
- ✅ Hash is identical before and after restart
- ✅ Hash is identical after device reboot
- ✅ Commands still reference correct hash

**Pass Criteria:**
- Hash remains stable across restarts
- Hash remains stable across reboots
- Hash remains stable after app updates

**Test Cases:**

**Test 6.2a: Hash Stability After Restart**
```
Element: Calculator "1" button
Hash Before: _______________________
Hash After:  _______________________
Match: [ ] Yes [ ] No
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 6.2b: Hash Stability After Reboot**
```
Element: Calculator "plus" button
Hash Before: _______________________
Hash After:  _______________________
Match: [ ] Yes [ ] No
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 6.2c: Hash Stability After App Reinstall**
```
Test Steps:
1. Learn Calculator, record hash
2. Uninstall VoiceOS 4
3. Reinstall VoiceOS 4
4. Re-learn Calculator
5. Compare hash

Hash Before Reinstall: _______________________
Hash After Reinstall:  _______________________
Match: [ ] Yes [ ] No
Pass/Fail: [ ] Pass [ ] Fail
```

---

### TEST SUITE 7: Performance Testing

#### Test 7.1: Command Execution Speed

**Test ID:** PERF-001
**Feature:** Response Time
**Priority:** High

**Prerequisites:**
- Stopwatch or timer
- VoiceOS 4 active

**Test Steps:**
```
For each command:
1. Start timer
2. Speak command
3. Wait for action completion
4. Stop timer
5. Record time
```

**Test Cases:**

**Test 7.1a: Tier 1 Commands (System)**
```
Command: "Go home"
Expected: < 100ms
Actual: _______ms
Pass/Fail: [ ] Pass [ ] Fail

Command: "Volume up"
Expected: < 50ms
Actual: _______ms
Pass/Fail: [ ] Pass [ ] Fail

Command: "Go back"
Expected: < 100ms
Actual: _______ms
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 7.1b: Tier 2 Commands (App)**
```
Command: "Click one" (Calculator)
Expected: < 300ms
Actual: _______ms
Pass/Fail: [ ] Pass [ ] Fail

Command: "Click compose" (Gmail)
Expected: < 300ms
Actual: _______ms
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 7.1c: Web Commands**
```
Command: "Click images" (Google.com)
Expected: < 500ms
Actual: _______ms
Pass/Fail: [ ] Pass [ ] Fail
```

**Performance Benchmarks:**
| Command Type | Target Time | Maximum Acceptable |
|--------------|-------------|---------------------|
| Tier 1 (System) | < 50ms | < 150ms |
| Tier 2 (App) | < 250ms | < 500ms |
| Web Commands | < 400ms | < 800ms |
| Speech Recognition | < 2s | < 4s |

#### Test 7.2: Memory Usage

**Test ID:** PERF-002
**Feature:** Memory Consumption
**Priority:** Medium

**Test Steps:**
```
1. Restart device
2. Enable VoiceOS 4
3. Check initial memory usage:
   adb shell dumpsys meminfo com.augmentalis.voiceaccessibility

4. Record: _______MB

5. Use VoiceOS 4 for 30 minutes (various commands)

6. Check memory usage again

7. Record: _______MB

8. Calculate increase: _______MB

9. Verify no excessive growth
```

**Expected Results:**
- ✅ Initial memory: < 100MB
- ✅ After 30min use: < 150MB
- ✅ Memory increase: < 50MB
- ✅ No memory leaks

**Test Cases:**

**Test 7.2a: Idle Memory**
```
Setup: VoiceOS 4 enabled but no commands issued
Wait: 5 minutes
Memory Usage: _______MB
Expected: < 80MB
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 7.2b: Active Use Memory**
```
Setup: Issue 50 commands over 10 minutes
Memory Before: _______MB
Memory After:  _______MB
Increase:      _______MB
Expected: < 30MB increase
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 7.2c: Memory After Database Query**
```
Setup: Execute commands that query database
Memory Before: _______MB
Memory After:  _______MB
Increase:      _______MB
Expected: < 10MB increase
Pass/Fail: [ ] Pass [ ] Fail
```

#### Test 7.3: Battery Usage

**Test ID:** PERF-003
**Feature:** Battery Consumption
**Priority:** Medium

**Test Steps:**
```
1. Fully charge device to 100%

2. Enable VoiceOS 4

3. Record battery level: 100%

4. Use device normally for 2 hours with VoiceOS 4 active
   - Issue ~20 commands per hour
   - Normal app usage between commands

5. Record battery level: ________%

6. Calculate VoiceOS 4 battery usage:
   Settings → Battery → VoiceOS 4

7. Record: _______%

8. Verify acceptable usage
```

**Expected Results:**
- ✅ Battery usage: < 5% per hour of active use
- ✅ Idle battery usage: < 1% per hour
- ✅ No excessive drain

**Test Cases:**

**Test 7.3a: 2-Hour Active Use**
```
Battery Start: 100%
Battery End: _______% (after 2 hours)
VoiceOS 4 Usage: _______%
Expected: < 10% for VoiceOS 4
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 7.3b: Overnight Idle**
```
Battery Start: 100% (8pm)
Battery End: _______% (8am, 12 hours later)
VoiceOS 4 Usage: _______%
Expected: < 5% for VoiceOS 4 over 12 hours
Pass/Fail: [ ] Pass [ ] Fail
```

#### Test 7.4: Database Query Performance

**Test ID:** PERF-004
**Feature:** Database Speed
**Priority:** Medium

**Prerequisites:**
- Database populated with commands

**Test Steps:**
```
1. Measure time for common queries
2. Use timestamps to calculate duration
3. Verify performance acceptable
```

**Test Cases:**

**Test 7.4a: Get All Commands**
```
Query: SELECT * FROM generated_commands;
Time: _______ms
Expected: < 100ms
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 7.4b: Find Command by Phrase**
```
Query: SELECT * FROM generated_commands WHERE commandPhrase = 'click one';
Time: _______ms
Expected: < 10ms
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 7.4c: Get Element by Hash**
```
Query: SELECT * FROM scraped_elements WHERE elementHash = 'abc123...';
Time: _______ms
Expected: < 20ms
Pass/Fail: [ ] Pass [ ] Fail
```

**Verification via ADB:**
```bash
# Enable query logging
adb logcat | grep "Database query"

# Should show query times:
# Database query: getCommandByPhrase took 5ms
```

---

### TEST SUITE 8: Edge Cases

#### Test 8.1: Unclear Speech

**Test ID:** EDGE-001
**Feature:** Handle Low Confidence
**Priority:** High

**Test Cases:**

**Test 8.1a: Mumbled Command**
```
Test: Speak "mumble mumble" unclearly
Expected: Command rejected (confidence < 0.5)
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 8.1b: Background Noise**
```
Setup: Play loud music in background
Command: "Go home" (spoken normally)
Expected: Either recognizes correctly or rejects
Should NOT execute wrong command
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 8.1c: Similar Sounding Commands**
```
Command: "Volume up" vs "All him up" (misheard)
Expected: Only execute if confidence high enough
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

#### Test 8.2: Rapid Commands

**Test ID:** EDGE-002
**Feature:** Handle Rapid Input
**Priority:** Medium

**Test Cases:**

**Test 8.2a: Rapid Sequential Commands**
```
Test: Issue 5 commands in 5 seconds
1. "Volume up"
2. "Volume up"
3. "Volume up"
4. "Volume up"
5. "Volume up"

Expected: All 5 execute (or queue)
Actual: _____ commands executed
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 8.2b: Overlapping Commands**
```
Test: Start saying command #2 before #1 finishes
Expected: #2 queued or rejected gracefully
Should NOT crash
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

#### Test 8.3: Long Command Strings

**Test ID:** EDGE-003
**Feature:** Handle Long Phrases
**Priority:** Low

**Test Cases:**

**Test 8.3a: Very Long Command**
```
Command: "Please open the calculator application on my device right now"
Expected: Recognizes "open calculator" portion
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 8.3b: Command with Extra Words**
```
Command: "Can you go home please"
Expected: Recognizes "go home"
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

#### Test 8.4: Resource Constraints

**Test ID:** EDGE-004
**Feature:** Low Resources
**Priority:** Medium

**Test Cases:**

**Test 8.4a: Low Memory Scenario**
```
Setup:
1. Open 10+ apps (don't close any)
2. Reduce available memory
3. Try VoiceOS 4 commands

Expected: Still works, or fails gracefully
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 8.4b: Low Battery**
```
Setup: Let battery drop below 10%
Expected: System may limit background services
VoiceOS 4 should still work or show warning
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 8.4c: No Internet (Online Engine)**
```
Setup: Use Google speech engine, disable internet
Command: "Go home"
Expected: Error message or fallback
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

#### Test 8.5: App State Changes

**Test ID:** EDGE-005
**Feature:** Handle App Changes
**Priority:** High

**Test Cases:**

**Test 8.5a: App Updated**
```
Test Steps:
1. Learn commands for an app (e.g., Calculator)
2. Update the app from Play Store (if update available)
3. Test if commands still work

Expected: Commands still work (or re-learn automatically)
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 8.5b: App Uninstalled**
```
Test Steps:
1. Learn commands for Calculator
2. Uninstall Calculator
3. Try command "Click one"

Expected: Command fails gracefully, no crash
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Test 8.5c: App Reinstalled**
```
Test Steps:
1. Learn Calculator, then uninstall
2. Reinstall Calculator
3. Open Calculator

Expected: Re-learns commands automatically
Actual: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

---

### TEST SUITE 9: Regression Testing

#### Test 9.1: Post-Update Regression

**Test ID:** REG-001
**Feature:** Verify After Updates
**Priority:** Critical

**Run this test after each VoiceOS 4 update**

**Test Steps:**
```
1. Record current VoiceOS 4 version: _______

2. Update to new version

3. Re-run critical tests:
   - [ ] Test 1.2: Accessibility Service Enablement
   - [ ] Test 2.1: Google Speech Engine
   - [ ] Test 3.1: Navigation Commands
   - [ ] Test 3.4: App Launching
   - [ ] Test 4.2: App Command Execution
   - [ ] Test 5.4: Web Command Execution
   - [ ] Test 6.1: Command Database Persistence
   - [ ] Test 6.2: Hash Stability
   - [ ] Test 7.1: Command Execution Speed

4. Record results
```

**Pass Criteria:**
- All critical tests still pass
- No regression in functionality
- Performance not degraded

#### Test 9.2: Android Version Compatibility

**Test ID:** REG-002
**Feature:** Multi-Version Support
**Priority:** High

**Test on different Android versions:**

**Android 10 Test:**
```
Device: _______________________
Android Version: 10
Test Suite: Run Tests 1-7
Pass Rate: _____/_____ tests
Critical Issues: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

**Android 11 Test:**
```
Device: _______________________
Android Version: 11
Pass Rate: _____/_____ tests
Pass/Fail: [ ] Pass [ ] Fail
```

**Android 12+ Test:**
```
Device: _______________________
Android Version: 12/13/14
Pass Rate: _____/_____ tests
Pass/Fail: [ ] Pass [ ] Fail
```

---

## Integration Testing

### Integration Test 1: End-to-End User Scenario

**Test ID:** INT-001
**Feature:** Complete User Journey
**Priority:** Critical

**Scenario:** Morning routine

**Test Steps:**
```
1. Device starts locked
2. Unlock device
3. Say "Go home"
4. Say "Open weather" (weather app)
5. Wait 5 seconds (check weather)
6. Say "Go back"
7. Say "Open Gmail"
8. Say "Click compose"
9. Type email subject
10. Say "Go back" (cancel email)
11. Say "Open Chrome"
12. Navigate to google.com
13. Say "Click images"
14. Say "Go back"
15. Say "Volume up" (3 times)
16. Say "Open music app"
17. Say "Play music"
18. Say "Go home"
```

**Expected Results:**
- ✅ All commands execute correctly
- ✅ No crashes or freezes
- ✅ Smooth transitions between apps
- ✅ Total time < 2 minutes

**Actual Results:**
```
Total Time: _______________________
Commands Executed: _____/18
Commands Failed: _______________________
Pass/Fail: [ ] Pass [ ] Fail
```

### Integration Test 2: 3-Tier Fallback

**Test ID:** INT-002
**Feature:** Tier Fallback System
**Priority:** Critical

**Purpose:** Verify commands fall through tiers correctly

**Test Cases:**

**Test 2a: Tier 1 Success**
```
Command: "Go home"
Expected Flow:
- Tier 1 (CommandManager): Matches → Executes → DONE
- Tier 2: Not reached
- Tier 3: Not reached

Verify in logs:
adb logcat | grep "Tier"

Should show: "✓ Tier 1 executed: go home"

Pass/Fail: [ ] Pass [ ] Fail
```

**Test 2b: Tier 1 Fail, Tier 2 Success**
```
Command: "Click one" (in Calculator)
Expected Flow:
- Tier 1 (CommandManager): No match → Falls through
- Tier 2 (VoiceCommandProcessor): Matches → Executes → DONE
- Tier 3: Not reached

Verify in logs: "✓ Tier 2 executed: click one"

Pass/Fail: [ ] Pass [ ] Fail
```

**Test 2c: Tier 1 & 2 Fail, Tier 3 Success**
```
Setup: Use obscure legacy command
Expected Flow:
- Tier 1: No match
- Tier 2: No match
- Tier 3 (ActionCoordinator): Handler match → Executes

Verify in logs: "✓ Tier 3 executed"

Pass/Fail: [ ] Pass [ ] Fail
```

**Test 2d: All Tiers Fail**
```
Command: "Xyz unknown command 123"
Expected Flow:
- Tier 1: No match
- Tier 2: No match
- Tier 3: No match
- Command fails gracefully

Verify in logs: "Command failed: xyz unknown command 123"
Should NOT crash

Pass/Fail: [ ] Pass [ ] Fail
```

### Integration Test 3: Web to App Transition

**Test ID:** INT-003
**Feature:** Browser to App Switching
**Priority:** High

**Test Steps:**
```
1. Open Chrome
2. Navigate to google.com
3. Say "Click images" (web command)
4. Verify Google Images loads
5. Say "Go home" (system command)
6. Say "Open Calculator" (app launch)
7. Say "Click five" (app command)
8. Verify all commands execute across contexts
```

**Expected Results:**
- ✅ Web commands work in browser
- ✅ System commands work everywhere
- ✅ App commands work in apps
- ✅ Smooth context switching

**Pass/Fail:** [ ] Pass [ ] Fail

---

## Performance Testing

### Performance Test 1: Stress Test

**Test ID:** PERF-STRESS-001
**Feature:** Handle Heavy Load
**Priority:** Medium

**Test Steps:**
```
1. Issue 100 commands in 10 minutes
2. Mix of Tier 1, Tier 2, Web commands
3. Monitor for:
   - Memory leaks
   - Slowdowns
   - Crashes
   - UI freezes
```

**Command Pattern:**
```
Repeat 20 times:
- "Go home"
- "Open Calculator"
- "Click one"
- "Click plus"
- "Click two"
```

**Metrics to Track:**
```
Total Commands: 100
Successful: _______
Failed: _______
Average Response Time: _______ms
Memory at Start: _______MB
Memory at End: _______MB
Battery Used: _______%
Crashes: _______
```

**Pass Criteria:**
- Success rate ≥ 95%
- No crashes
- Memory increase < 50MB
- Battery use < 5%

### Performance Test 2: Continuous Use

**Test ID:** PERF-STRESS-002
**Feature:** Extended Use
**Priority:** Medium

**Test Steps:**
```
1. Use VoiceOS 4 continuously for 1 hour
2. Issue command every 30 seconds (120 commands total)
3. Monitor device temperature
4. Monitor battery drain
5. Monitor performance degradation
```

**Metrics:**
```
Duration: 60 minutes
Commands Issued: 120
Commands Successful: _______
Device Temperature Start: _______°C
Device Temperature End: _______°C
Battery Start: _______%
Battery End: _______%
Memory Leaks Detected: [ ] Yes [ ] No
Performance Degraded: [ ] Yes [ ] No
```

**Pass Criteria:**
- Device doesn't overheat (< 45°C)
- Battery drain < 10%
- No memory leaks
- Performance stays consistent

---

## Edge Case Testing

### Edge Case Test 1: Network Loss During Recognition

**Test ID:** EDGE-NET-001

**Test Steps:**
```
1. Use Google speech engine (requires internet)
2. Start speaking command
3. Disable WiFi/data DURING speech
4. Observe behavior
```

**Expected:** Graceful error or uses cached model
**Actual:** _______________________
**Pass/Fail:** [ ] Pass [ ] Fail

### Edge Case Test 2: Database Corruption

**Test ID:** EDGE-DB-001

**Test Steps:**
```
1. Stop VoiceOS 4
2. Corrupt database file:
   adb shell "run-as com.augmentalis.voiceaccessibility \
   rm databases/app_scraping_database"
3. Restart VoiceOS 4
4. Observe behavior
```

**Expected:** Recreates database, no crash
**Actual:** _______________________
**Pass/Fail:** [ ] Pass [ ] Fail

### Edge Case Test 3: Permission Revoked Mid-Use

**Test ID:** EDGE-PERM-001

**Test Steps:**
```
1. VoiceOS 4 active and working
2. Disable accessibility service:
   Settings → Accessibility → VoiceOS 4 → OFF
3. Try issuing command
4. Observe behavior
```

**Expected:** Error message, prompts to re-enable
**Actual:** _______________________
**Pass/Fail:** [ ] Pass [ ] Fail

---

## Test Results Documentation

### Test Summary Template

**Test Session Information:**
```
Date: _______________________
Tester: _______________________
VoiceOS 4 Version: _______________________
Device: _______________________
Android Version: _______________________
Test Duration: _______________________
```

**Results Summary:**
```
Total Tests Run: _______
Tests Passed: _______
Tests Failed: _______
Pass Rate: _______%
Critical Failures: _______
```

**Test Breakdown:**
```
Installation & Setup:        Pass: ___ / Fail: ___ / Total: ___
Speech Recognition:          Pass: ___ / Fail: ___ / Total: ___
System Commands (Tier 1):    Pass: ___ / Fail: ___ / Total: ___
App Commands (Tier 2):       Pass: ___ / Fail: ___ / Total: ___
Web Commands:                Pass: ___ / Fail: ___ / Total: ___
Database Persistence:        Pass: ___ / Fail: ___ / Total: ___
Performance:                 Pass: ___ / Fail: ___ / Total: ___
Edge Cases:                  Pass: ___ / Fail: ___ / Total: ___
Integration:                 Pass: ___ / Fail: ___ / Total: ___
```

### Bug Report Template

**Bug ID:** BUG-______
**Severity:** [ ] Critical [ ] High [ ] Medium [ ] Low
**Test ID:** _______________________

**Description:**
```
Brief description of the issue
```

**Steps to Reproduce:**
```
1.
2.
3.
```

**Expected Result:**
```
What should happen
```

**Actual Result:**
```
What actually happened
```

**Frequency:**
[ ] Always [ ] Sometimes [ ] Rare

**Screenshots/Logs:**
```
Attach relevant logs or screenshots
```

**Environment:**
```
VoiceOS 4 Version: _______
Android Version: _______
Device Model: _______
```

### Test Completion Checklist

**Before Release:**
- [ ] All critical tests passed
- [ ] All high-priority tests passed
- [ ] Performance tests passed
- [ ] Tested on ≥ 3 different devices
- [ ] Tested on ≥ 2 Android versions
- [ ] No critical bugs remaining
- [ ] Regression tests passed
- [ ] Documentation updated
- [ ] Test results documented
- [ ] Sign-off from QA team

---

## Appendix

### Appendix A: ADB Commands Reference

**Device Connection:**
```bash
adb devices                    # List connected devices
adb shell                      # Open device shell
```

**Logging:**
```bash
# Full logcat
adb logcat

# Filter VoiceOS logs
adb logcat | grep -i "VoiceOS"

# Save to file
adb logcat > test_$(date +%Y%m%d_%H%M%S).log

# Clear log buffer
adb logcat -c
```

**Database Access:**
```bash
# Open database shell
adb shell "run-as com.augmentalis.voiceaccessibility \
sqlite3 databases/app_scraping_database"

# Query commands
adb shell "run-as com.augmentalis.voiceaccessibility \
sqlite3 databases/app_scraping_database \
'SELECT * FROM generated_commands;'"

# Count entries
adb shell "run-as com.augmentalis.voiceaccessibility \
sqlite3 databases/command_database \
'SELECT COUNT(*) FROM vos_commands;'"
```

**App Management:**
```bash
# Install APK
adb install voiceos.apk

# Uninstall
adb uninstall com.augmentalis.voiceaccessibility

# Clear app data
adb shell pm clear com.augmentalis.voiceaccessibility

# Force stop
adb shell am force-stop com.augmentalis.voiceaccessibility
```

**System Info:**
```bash
# Device info
adb shell getprop ro.build.version.release    # Android version
adb shell getprop ro.product.model            # Device model

# Battery info
adb shell dumpsys battery

# Memory info
adb shell dumpsys meminfo com.augmentalis.voiceaccessibility
```

### Appendix B: Test Data Files

**Sample Commands to Test:**
```
System Commands:
- go home
- go back
- recent apps
- volume up
- volume down
- open notifications
- quick settings

App Commands (Calculator):
- click one
- click two
- click plus
- click minus
- click equals

Web Commands (Google):
- click search
- click images
- click gmail
```

### Appendix C: Performance Benchmarks

**Target Performance:**
| Metric | Target | Maximum |
|--------|--------|---------|
| Tier 1 Command | 50ms | 150ms |
| Tier 2 Command | 250ms | 500ms |
| Web Command | 400ms | 800ms |
| Speech Recognition | 1500ms | 3000ms |
| Database Query | 10ms | 50ms |
| Memory Usage (Idle) | 70MB | 100MB |
| Memory Usage (Active) | 120MB | 180MB |
| Battery (per hour) | 3% | 6% |

---

**Document Version:** 3.0.0
**Last Updated:** 2025-10-13 21:47 PDT
**Status:** Ready for Testing
**For Architecture Details:** See VoiceOS4-System-Architecture-Complete-251013-2144.md
**For Developer Info:** See VoiceOS4-Developer-Guide-Complete-251013-2144.md
**For User Guide:** See VoiceOS4-User-Manual-Complete-251013-2144.md
