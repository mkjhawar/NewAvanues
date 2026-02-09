# Cockpit MVP - Complete Testing Guide

**Version:** 1.0
**Date:** 2025-12-10
**Platform:** Android (Jetpack Compose)
**Test Environment:** Android Emulator (Pixel 9, API 35)

---

## Table of Contents

1. [Setup Instructions](#setup-instructions)
2. [Phase 1: Window Controls Testing](#phase-1-window-controls-testing)
3. [Phase 2: Selection State Testing](#phase-2-selection-state-testing)
4. [Phase 3: State Persistence Testing](#phase-3-state-persistence-testing)
5. [Phase 4: WebView Enhancements Testing](#phase-4-webview-enhancements-testing)
6. [Integration Testing](#integration-testing)
7. [Performance Testing](#performance-testing)
8. [Bug Reporting Template](#bug-reporting-template)

---

## Setup Instructions

### Prerequisites
- Android Studio installed
- Android Emulator running (Pixel 9, API 35 recommended)
- ADB tools configured
- Project built successfully

### Initial Setup

1. **Build the APK**
   ```bash
   cd /Volumes/M-Drive/Coding/NewAvanues-Cockpit/android/apps/cockpit-mvp
   ./gradlew assembleDebug
   ```

2. **Start Emulator**
   - Open Android Studio → Device Manager
   - Launch Pixel 9 (API 35) emulator
   - Verify emulator is running:
     ```bash
     ~/Library/Android/sdk/platform-tools/adb devices
     ```
   - Should show: `emulator-5554   device`

3. **Deploy App**
   ```bash
   ./gradlew installDebug
   ~/Library/Android/sdk/platform-tools/adb shell am start -n com.augmentalis.cockpit.mvp/.MainActivity
   ```

4. **Verify Initial State**
   - ✅ App launches without crashes
   - ✅ 3 windows appear: WebAvanue, Google, Calculator
   - ✅ Ocean Theme glassmorphic UI visible
   - ✅ Top navigation bar showing "3 windows active"
   - ✅ Bottom control panel visible

---

## Phase 1: Window Controls Testing

### Test 1.1: Minimize Button Functionality

**Objective:** Verify minimize button collapses window to title bar only

**Steps:**
1. Locate the **WebAvanue** window (first window, teal color)
2. Identify the minimize button (dash icon, leftmost in control bar)
3. Click the minimize button
4. Observe the animation

**Expected Results:**
- ✅ Window smoothly animates to 48dp height (300ms duration)
- ✅ Window content disappears
- ✅ Only title bar remains visible
- ✅ Haptic feedback (medium tap) occurs on button click
- ✅ Window maintains its horizontal position

**Pass/Fail:** ________

**Notes:** ________________________________________

---

### Test 1.2: Restore from Minimized State

**Objective:** Verify minimized window can be restored by clicking title bar

**Steps:**
1. Ensure WebAvanue window is minimized (from Test 1.1)
2. Click anywhere on the minimized title bar
3. Observe the animation

**Expected Results:**
- ✅ Window smoothly animates back to 400dp height (300ms duration)
- ✅ Window content reappears
- ✅ Minimize button still visible
- ✅ No content state lost (scroll position preserved if any)

**Pass/Fail:** ________

**Notes:** ________________________________________

---

### Test 1.3: Maximize Button Functionality

**Objective:** Verify maximize button doubles window size

**Steps:**
1. Locate the **Google** window (second window, mint color)
2. Identify the maximize button (fullscreen icon, middle button in control bar)
3. Click the maximize button
4. Observe the size change and icon

**Expected Results:**
- ✅ Window smoothly animates from 300x400dp → 600x800dp (300ms)
- ✅ Icon changes from "Fullscreen" to "FullscreenExit" (restore icon)
- ✅ Haptic feedback (medium tap) occurs
- ✅ Window content scales appropriately
- ✅ Other windows remain at normal size

**Pass/Fail:** ________

**Notes:** ________________________________________

---

### Test 1.4: Restore from Maximized State

**Objective:** Verify maximized window can be restored to normal size

**Steps:**
1. Ensure Google window is maximized (from Test 1.3)
2. Click the restore button (FullscreenExit icon)
3. Observe the size change and icon

**Expected Results:**
- ✅ Window smoothly animates from 600x800dp → 300x400dp (300ms)
- ✅ Icon changes from "FullscreenExit" back to "Fullscreen"
- ✅ Haptic feedback (medium tap) occurs
- ✅ Window content scales back appropriately

**Pass/Fail:** ________

**Notes:** ________________________________________

---

### Test 1.5: Close Button Functionality

**Objective:** Verify close button removes window from workspace

**Steps:**
1. Locate the **Calculator** window (third window, pink color)
2. Identify the close button (X icon, rightmost in control bar)
3. Click the close button
4. Observe window removal

**Expected Results:**
- ✅ Window immediately disappears
- ✅ Haptic feedback (light tap) occurs
- ✅ Top navigation bar updates to "2 windows active"
- ✅ Remaining windows adjust layout if in landscape mode
- ✅ No crash or errors

**Pass/Fail:** ________

**Notes:** ________________________________________

---

### Test 1.6: Multiple Windows Minimize/Maximize

**Objective:** Verify multiple windows can have different states simultaneously

**Steps:**
1. Minimize the WebAvanue window
2. Maximize the Google window
3. Observe both windows

**Expected Results:**
- ✅ WebAvanue remains minimized (48dp height)
- ✅ Google remains maximized (600x800dp)
- ✅ States are independent
- ✅ No interference between window states

**Pass/Fail:** ________

**Notes:** ________________________________________

---

## Phase 2: Selection State Testing

### Test 2.1: Window Selection via Title Bar Click

**Objective:** Verify clicking window title bar selects the window

**Steps:**
1. Click on the title bar of the WebAvanue window
2. Observe the visual changes

**Expected Results:**
- ✅ Window border changes to **2dp blue** (OceanTheme.primary)
- ✅ Window elevation increases to **8dp** (more prominent shadow)
- ✅ Previously selected window (if any) returns to gray border
- ✅ Selection state is visually clear

**Pass/Fail:** ________

**Notes:** ________________________________________

---

### Test 2.2: Window Selection via Content Area Click

**Objective:** Verify clicking window content area also selects the window

**Steps:**
1. Click anywhere in the **content area** of the Google window (not title bar)
2. Observe the visual changes

**Expected Results:**
- ✅ Google window border changes to **2dp blue**
- ✅ Google window elevation increases to **8dp**
- ✅ WebAvanue window border returns to **1dp gray**
- ✅ WebAvanue window elevation returns to **4dp**

**Pass/Fail:** ________

**Notes:** ________________________________________

---

### Test 2.3: Single Selection Model

**Objective:** Verify only one window can be selected at a time

**Steps:**
1. Select WebAvanue window (should have blue border)
2. Select Google window (click anywhere on it)
3. Observe both windows

**Expected Results:**
- ✅ Only Google window has blue border
- ✅ WebAvanue window has gray border
- ✅ No two windows can be selected simultaneously
- ✅ Selection state transfers smoothly

**Pass/Fail:** ________

**Notes:** ________________________________________

---

### Test 2.4: Selection Persistence Across State Changes

**Objective:** Verify selection persists when window is minimized/maximized

**Steps:**
1. Select WebAvanue window (blue border)
2. Minimize the WebAvanue window
3. Observe the minimized window
4. Restore the WebAvanue window
5. Observe the restored window

**Expected Results:**
- ✅ Minimized window maintains blue border on title bar
- ✅ Restored window still has blue border
- ✅ Selection state is not lost during minimize/restore

**Pass/Fail:** ________

**Notes:** ________________________________________

---

## Phase 3: State Persistence Testing

### Test 3.1: WebView Scroll Position Save

**Objective:** Verify WebView scroll position is saved

**Steps:**
1. Select the **Google** window
2. Wait for Google homepage to fully load
3. Scroll down approximately 500-1000 pixels
4. Note the content visible on screen
5. Minimize the Google window
6. Wait 2 seconds
7. Restore the Google window

**Expected Results:**
- ✅ Scroll position is exactly where you left it
- ✅ Same content visible on screen
- ✅ No jump to top of page
- ✅ Smooth restore of scroll state

**Pass/Fail:** ________

**Actual Scroll Behavior:** ________________________________________

---

### Test 3.2: WebView Scroll Position Across Window Switches

**Objective:** Verify scroll position persists when switching between windows

**Steps:**
1. Scroll down in the Google window (approximately 500px)
2. Note the visible content
3. Click on the WebAvanue window to select it
4. Wait 2 seconds
5. Click back on the Google window
6. Observe the scroll position

**Expected Results:**
- ✅ Google window scroll position is preserved
- ✅ Same content visible as before
- ✅ No reset to top
- ✅ State persists across window switches

**Pass/Fail:** ________

**Notes:** ________________________________________

---

### Test 3.3: Multiple WebView Scroll States

**Objective:** Verify each window maintains independent scroll state

**Steps:**
1. Scroll Google window down 500px
2. Switch to WebAvanue window
3. Scroll WebAvanue window down 300px
4. Switch back to Google window
5. Observe Google scroll position
6. Switch back to WebAvanue window
7. Observe WebAvanue scroll position

**Expected Results:**
- ✅ Google window at 500px scroll position
- ✅ WebAvanue window at 300px scroll position
- ✅ Each window maintains independent state
- ✅ No cross-contamination of scroll positions

**Pass/Fail:** ________

**Notes:** ________________________________________

---

### Test 3.4: Scroll State After Maximize/Minimize

**Objective:** Verify scroll state persists through size changes

**Steps:**
1. Scroll Google window down 500px
2. Maximize the Google window
3. Observe scroll position (should be preserved)
4. Minimize the Google window
5. Restore the Google window
6. Observe scroll position

**Expected Results:**
- ✅ Scroll position preserved after maximize
- ✅ Scroll position preserved after minimize
- ✅ Scroll position preserved after restore
- ✅ No state loss during size transitions

**Pass/Fail:** ________

**Notes:** ________________________________________

---

## Phase 4: WebView Enhancements Testing

### Test 4.1: Desktop Mode (User Agent)

**Objective:** Verify WebView uses desktop user agent by default

**Steps:**
1. Open the Google window
2. Navigate to a site that detects user agent (e.g., https://www.whatismybrowser.com/detect/what-is-my-user-agent)
3. Observe the detected user agent string

**Expected Results:**
- ✅ User agent contains "Windows NT 10.0; Win64; x64"
- ✅ User agent identifies as Chrome 120
- ✅ Desktop user agent string detected
- ✅ Sites render desktop versions (not mobile)

**Detected User Agent:** ________________________________________

**Pass/Fail:** ________

---

### Test 4.2: Desktop Site Rendering

**Objective:** Verify websites render desktop versions

**Steps:**
1. In Google window, navigate to https://www.reddit.com
2. Observe the layout
3. Navigate to https://www.twitter.com (or X.com)
4. Observe the layout

**Expected Results:**
- ✅ Desktop layout with multiple columns
- ✅ Full navigation menus (not mobile hamburger menu)
- ✅ Desktop-optimized UI components
- ✅ Better rendering for AR glasses viewing

**Pass/Fail:** ________

**Notes:** ________________________________________

---

### Test 4.3: HTTP Basic Auth Dialog (Manual)

**Objective:** Verify HTTP Basic Auth dialog appears and functions

**Note:** This test requires access to a site with HTTP Basic Authentication. If unavailable, create a test server or skip.

**Steps:**
1. Navigate to a site requiring HTTP Basic Auth (e.g., https://httpbin.org/basic-auth/user/passwd)
2. Observe if auth dialog appears
3. Enter username: "user"
4. Enter password: "passwd"
5. Click "Sign In"

**Expected Results:**
- ✅ Dialog appears with "Authentication Required" title
- ✅ Shows site host and realm
- ✅ Username field accepts input
- ✅ Password field accepts input (masked)
- ✅ "Cancel" button dismisses dialog
- ✅ "Sign In" button enabled when both fields filled
- ✅ Credentials submitted correctly
- ✅ Protected content loads after successful auth

**Pass/Fail:** ________

**Notes:** ________________________________________

---

### Test 4.4: HTTP Basic Auth Cancel

**Objective:** Verify auth dialog can be cancelled

**Steps:**
1. Navigate to HTTP Basic Auth protected site
2. When dialog appears, click "Cancel"

**Expected Results:**
- ✅ Dialog dismisses
- ✅ Auth request is cancelled
- ✅ Page shows auth failure (expected behavior)
- ✅ No crash or error

**Pass/Fail:** ________

**Notes:** ________________________________________

---

### Test 4.5: Multi-Window Support (Infrastructure)

**Objective:** Verify window.open() support is enabled

**Steps:**
1. Open browser dev tools if available, or check WebView settings
2. Verify javaScriptCanOpenWindowsAutomatically = true
3. Verify setSupportMultipleWindows(true)

**Expected Results:**
- ✅ Settings enabled (code inspection confirms)
- ✅ No errors when sites attempt window.open()
- ✅ Infrastructure ready for future implementation

**Pass/Fail:** ________

**Notes:** ________________________________________

---

## Integration Testing

### Test INT-1: 2D Mode Full Workflow

**Objective:** Test complete workflow in 2D mode

**Steps:**
1. Ensure spatial mode is OFF (ViewStream icon in top bar)
2. Add a new window via bottom control panel
3. Minimize one window
4. Maximize another window
5. Select different windows
6. Scroll in a WebView window
7. Close a window

**Expected Results:**
- ✅ All features work in 2D mode
- ✅ Windows arrange in vertical (portrait) or horizontal (landscape) layout
- ✅ No spatial rendering artifacts
- ✅ Smooth transitions between all states

**Pass/Fail:** ________

**Notes:** ________________________________________

---

### Test INT-2: Spatial Mode Full Workflow

**Objective:** Test complete workflow in spatial mode

**Steps:**
1. Click spatial mode toggle (ViewInAr icon in top bar)
2. Verify spatial mode activates (icon turns blue)
3. Minimize one window
4. Maximize another window
5. Select different windows
6. Cycle through layout presets (if available)

**Expected Results:**
- ✅ All window controls work in spatial mode
- ✅ Windows render in curved/3D layout
- ✅ Selection state visible in spatial mode
- ✅ No regressions from 2D mode

**Pass/Fail:** ________

**Notes:** ________________________________________

---

### Test INT-3: Maximum Window Capacity

**Objective:** Verify system handles 6 windows (maximum capacity)

**Steps:**
1. Add windows until you reach 6 total
2. Test minimize/maximize on various windows
3. Test selection on different windows
4. Test scroll persistence on multiple windows

**Expected Results:**
- ✅ System handles 6 windows without performance issues
- ✅ All features work correctly with full capacity
- ✅ Layout adjusts appropriately
- ✅ No crashes or slowdowns

**Pass/Fail:** ________

**Performance Notes:** ________________________________________

---

### Test INT-4: Workspace Reset

**Objective:** Verify workspace reset functionality

**Steps:**
1. Modify window states (minimize some, maximize others)
2. Scroll in WebView windows
3. Click the Reset button in control panel
4. Observe workspace state

**Expected Results:**
- ✅ All windows reset to default state
- ✅ 3 windows appear (WebAvanue, Google, Calculator)
- ✅ All windows at normal size (300x400dp)
- ✅ No windows minimized
- ✅ Scroll positions reset

**Pass/Fail:** ________

**Notes:** ________________________________________

---

## Performance Testing

### Test PERF-1: Animation Frame Rate

**Objective:** Verify animations run at 60 FPS

**Tools Required:** Android Studio Profiler or GPU Profiling

**Steps:**
1. Enable GPU profiling: Developer Options → Profile GPU Rendering → On screen as bars
2. Maximize a window (watch animation)
3. Minimize a window (watch animation)
4. Observe the green bars (should stay below 16ms line)

**Expected Results:**
- ✅ Green bars stay below 16ms line (60 FPS)
- ✅ No dropped frames during transitions
- ✅ Smooth animations throughout

**Frame Rate Observed:** ________

**Pass/Fail:** ________

---

### Test PERF-2: Scroll Performance

**Objective:** Verify scrolling is smooth with multiple windows

**Steps:**
1. Open 6 windows
2. Scroll rapidly in a WebView window
3. Observe scroll smoothness
4. Check for any lag or stuttering

**Expected Results:**
- ✅ Scrolling is smooth and responsive
- ✅ No lag or stuttering
- ✅ 60 FPS maintained during scroll
- ✅ Scroll position saves without impacting performance

**Pass/Fail:** ________

**Notes:** ________________________________________

---

### Test PERF-3: Memory Usage

**Objective:** Monitor memory usage with state persistence

**Tools Required:** Android Studio Profiler

**Steps:**
1. Open Android Studio → Profiler
2. Attach to cockpit-mvp process
3. Open 6 windows
4. Scroll in all windows
5. Monitor memory usage over 5 minutes

**Expected Results:**
- ✅ Memory usage stable (no leaks)
- ✅ No continuous growth pattern
- ✅ Garbage collection occurs normally
- ✅ App remains under 200MB total memory

**Peak Memory:** ________

**Pass/Fail:** ________

---

## Regression Testing

### Test REG-1: Existing Features Still Work

**Objective:** Verify no regressions in existing functionality

**Features to Test:**
- ✅ Head cursor toggle (Sensors icon)
- ✅ Spatial mode toggle (ViewInAr/ViewStream icons)
- ✅ Add window functionality
- ✅ Window type selection (Web App, Android App, Widget)
- ✅ Ocean Theme glassmorphic UI
- ✅ Top navigation bar display
- ✅ Window count indicator

**Pass/Fail:** ________

**Regressions Found:** ________________________________________

---

## Bug Reporting Template

If you encounter any issues during testing, please document using this template:

```
BUG REPORT

Title: [Brief description of the issue]

Phase: [Phase 1, 2, 3, or 4]

Test ID: [e.g., Test 1.1]

Severity: [Critical / High / Medium / Low]

Steps to Reproduce:
1.
2.
3.

Expected Behavior:


Actual Behavior:


Screenshots: [Attach if applicable]

Device Info:
- Emulator: Pixel 9 (API 35)
- Android Version:
- App Version: cockpit-mvp-debug.apk

Additional Notes:


Reported By: ________
Date: ________
```

---

## Test Summary Report Template

After completing all tests, fill out this summary:

```
COCKPIT MVP - TEST SUMMARY

Test Date: ________
Tester Name: ________
Environment: Android Emulator (Pixel 9, API 35)

Phase 1: Window Controls
- Tests Passed: __ / 6
- Tests Failed: __ / 6
- Critical Issues: __
- Notes: ________

Phase 2: Selection State
- Tests Passed: __ / 4
- Tests Failed: __ / 4
- Critical Issues: __
- Notes: ________

Phase 3: State Persistence
- Tests Passed: __ / 4
- Tests Failed: __ / 4
- Critical Issues: __
- Notes: ________

Phase 4: WebView Enhancements
- Tests Passed: __ / 5
- Tests Failed: __ / 5
- Critical Issues: __
- Notes: ________

Integration Tests
- Tests Passed: __ / 4
- Tests Failed: __ / 4
- Critical Issues: __
- Notes: ________

Performance Tests
- Tests Passed: __ / 3
- Tests Failed: __ / 3
- Critical Issues: __
- Notes: ________

Regression Tests
- Tests Passed: __ / 1
- Tests Failed: __ / 1
- Critical Issues: __
- Notes: ________

OVERALL ASSESSMENT: [PASS / FAIL / PASS WITH ISSUES]

Total Tests: 27
Tests Passed: __
Tests Failed: __
Success Rate: __%

Recommendation: [APPROVE FOR DEPLOYMENT / NEEDS FIXES / MAJOR ISSUES]

Signed: ________
Date: ________
```

---

## Testing Checklist

Use this checklist to track your testing progress:

### Phase 1: Window Controls
- [ ] Test 1.1: Minimize button
- [ ] Test 1.2: Restore from minimized
- [ ] Test 1.3: Maximize button
- [ ] Test 1.4: Restore from maximized
- [ ] Test 1.5: Close button
- [ ] Test 1.6: Multiple windows states

### Phase 2: Selection State
- [ ] Test 2.1: Title bar selection
- [ ] Test 2.2: Content area selection
- [ ] Test 2.3: Single selection model
- [ ] Test 2.4: Selection persistence

### Phase 3: State Persistence
- [ ] Test 3.1: Scroll save
- [ ] Test 3.2: Scroll across switches
- [ ] Test 3.3: Multiple scroll states
- [ ] Test 3.4: Scroll after size changes

### Phase 4: WebView Enhancements
- [ ] Test 4.1: Desktop mode UA
- [ ] Test 4.2: Desktop rendering
- [ ] Test 4.3: HTTP Basic Auth
- [ ] Test 4.4: Auth cancel
- [ ] Test 4.5: Multi-window support

### Integration Tests
- [ ] Test INT-1: 2D mode workflow
- [ ] Test INT-2: Spatial mode workflow
- [ ] Test INT-3: Maximum capacity
- [ ] Test INT-4: Workspace reset

### Performance Tests
- [ ] Test PERF-1: Animation FPS
- [ ] Test PERF-2: Scroll performance
- [ ] Test PERF-3: Memory usage

### Regression Tests
- [ ] Test REG-1: Existing features

---

**Testing Guide Version:** 1.0
**Last Updated:** 2025-12-10
**Status:** Ready for Testing

---

**End of Testing Guide**
