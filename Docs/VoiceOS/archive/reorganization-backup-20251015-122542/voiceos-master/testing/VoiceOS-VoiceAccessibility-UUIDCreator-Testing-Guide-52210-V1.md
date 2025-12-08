# VoiceAccessibility + UUIDCreator Integration Testing Guide

**Last Updated:** 2025-10-09 03:40:00 PDT
**Integration Version:** 1.0.0 (Production Ready)
**Testing Type:** Manual Device Testing (Runtime Verification)
**Prerequisites:** Android device (API 28+), ADB access, VoiceOS installed

---

## 📋 Table of Contents

1. [Pre-Test Setup](#pre-test-setup)
2. [Installation & Initialization Testing](#installation--initialization-testing)
3. [UUIDCreator Integration Testing](#uuidcreator-integration-testing)
4. [Voice Command Testing](#voice-command-testing)
5. [LearnApp Third-Party Testing](#learnapp-third-party-testing)
6. [Performance Testing](#performance-testing)
7. [Memory & Stability Testing](#memory--stability-testing)
8. [Error Handling Testing](#error-handling-testing)
9. [Troubleshooting](#troubleshooting)
10. [Test Results Template](#test-results-template)

---

## 🔧 Pre-Test Setup

### Required Equipment

- **Android Device:** Physical device (API 28+, Android 9+)
  - Recommended: RealWear HMT-1, Android phone/tablet, or Android XR device
- **Development Machine:** macOS/Linux/Windows with ADB installed
- **USB Cable:** For device connection
- **Microphone:** For voice command testing

### Development Environment Setup

```bash
# 1. Verify ADB connection
adb devices
# Expected: Device listed with "device" status

# 2. Build and install VoiceOS
cd "/Volumes/M Drive/Coding/Warp/vos4"
./gradlew :app:assembleDebug

# 3. Install APK
./gradlew :app:installDebug
# OR manually:
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 4. Verify installation
adb shell pm list packages | grep augmentalis
# Expected: package:com.augmentalis.voiceos
```

### Enable Accessibility Service

**On Device:**
1. Open **Settings** → **Accessibility**
2. Find **VoiceOS Service**
3. Enable the service
4. Grant all requested permissions:
   - Accessibility access
   - Overlay permission (draw over other apps)
   - Microphone access
   - Storage access (if prompted)

**Verify via ADB:**
```bash
# Check if service is enabled
adb shell settings get secure enabled_accessibility_services
# Expected: Contains "com.augmentalis.voiceos/.../VoiceOSService"

# Check if service is running
adb shell dumpsys accessibility | grep VoiceOS
# Expected: Service listed and "isEnabled: true"
```

### Setup Logging

```bash
# Terminal 1: Monitor VoiceAccessibility logs
adb logcat -s VoiceAccessibilityService:* VoiceOSService:* -v time

# Terminal 2: Monitor UUIDCreator logs
adb logcat -s UUIDCreator:* UUIDRegistry:* -v time

# Terminal 3: Monitor LearnApp logs
adb logcat -s VOS4LearnAppIntegration:* ExplorationEngine:* -v time

# Combined view (one terminal):
adb logcat -s VoiceAccessibilityService:* VoiceOSService:* UUIDCreator:* VOS4LearnAppIntegration:* -v time
```

---

## 📱 Installation & Initialization Testing

### Test 1.1: Service Startup

**Objective:** Verify VoiceAccessibility service starts correctly with UUIDCreator integration

**Steps:**
1. Clear logcat buffer: `adb logcat -c`
2. Start logging: `adb logcat -s VoiceOSService:* -v time`
3. Enable VoiceOS Service in device settings
4. Observe logs

**Expected Results:**
```
03:40:00.123 I VoiceOSService: VoiceOS Service connected
03:40:00.234 I VoiceOSService: === UUIDCreator Initialization Start ===
03:40:00.245 D VoiceOSService: Attempting to get existing UUIDCreator instance...
03:40:00.256 I VoiceOSService: ✓ UUIDCreator initialized from VoiceAccessibilityService
03:40:00.267 I VoiceOSService: === UUIDCreator Initialization Complete ===
03:40:00.345 I VoiceOSService: === LearnApp Integration Initialization Start ===
03:40:00.356 D VoiceOSService: Attempting to initialize VOS4LearnAppIntegration...
03:40:00.367 I VoiceOSService: ✓ LearnApp integration initialized successfully
03:40:00.378 I VoiceOSService: === LearnApp Integration Initialization Complete ===
03:40:00.456 I VoiceOSService: All components initialized with optimization
```

**Success Criteria:**
- ✅ Service starts without exceptions
- ✅ UUIDCreator initialization completes in <500ms (target: ~50ms)
- ✅ LearnApp integration initializes successfully
- ✅ No error messages (`✗` markers)

**Failure Indicators:**
- ❌ `✗ CRITICAL: Failed to initialize UUIDCreator`
- ❌ `✗ Failed to initialize LearnApp integration`
- ❌ Any exception stack traces

---

### Test 1.2: Hilt Dependency Injection

**Objective:** Verify Hilt properly injects dependencies

**Steps:**
1. Check logs for injection confirmations
2. Verify no null pointer exceptions

**Expected Results:**
```
03:40:00.123 D VoiceOSService: SpeechEngineManager injected: true
03:40:00.124 D VoiceOSService: InstalledAppsManager injected: true
03:40:00.125 D VoiceOSService: UIScrapingEngine initialized (lazy)
03:40:00.126 D VoiceOSService: ActionCoordinator initialized (lazy)
```

**Success Criteria:**
- ✅ All `@Inject` fields initialized
- ✅ Lazy dependencies initialized on first use
- ✅ No `lateinit property has not been initialized` errors

---

## 🆔 UUIDCreator Integration Testing

### Test 2.1: Accessibility Tree Traversal

**Objective:** Verify accessibility events trigger tree traversal and UUID registration

**Steps:**
1. Clear logcat: `adb logcat -c`
2. Start logging: `adb logcat -s VoiceOSService:* -v time`
3. Open any app (e.g., Settings, Chrome)
4. Observe logs for tree traversal

**Expected Results:**
```
03:40:10.123 D VoiceOSService: Processing accessibility tree for event: 32
03:40:10.234 D VoiceOSService:   Processing node at depth 0
03:40:10.235 D VoiceOSService:     Class: android.widget.FrameLayout
03:40:10.236 D VoiceOSService:     Visible: true
03:40:10.245 I VoiceOSService:   ✓ Registered element: UUID=abc-123, name=Settings
03:40:10.256 D VoiceOSService:     Processing node at depth 1
03:40:10.267 I VoiceOSService:     ✓ Registered element: UUID=def-456, name=Wi-Fi
03:40:10.345 I VoiceOSService: Total elements registered: 47
03:40:10.356 I VoiceOSService: Tree traversal completed in 112ms
```

**Success Criteria:**
- ✅ Tree traversal triggers on window state/content changes
- ✅ Multiple elements registered (>10 for typical app screen)
- ✅ Traversal completes in <200ms (target: 80-120ms)
- ✅ Depth-based indentation in logs
- ✅ Success markers (`✓`) for registered elements

**Test Cases:**
| App/Screen | Expected Elements | Notes |
|------------|-------------------|-------|
| Settings home | 15-30 | List items, headers |
| Chrome browser | 10-25 | URL bar, tabs, buttons |
| Calculator | 15-20 | Number buttons, operators |
| System UI | 5-15 | Status bar, navigation |

---

### Test 2.2: Element Type Detection

**Objective:** Verify correct classification of UI element types

**Steps:**
1. Open app with diverse UI elements
2. Monitor logs for element type detection
3. Verify 15 element types are detected

**Expected Element Types:**
- `button` - Clickable buttons
- `input` - Text input fields
- `text` - Text labels
- `image` - Images, icons
- `checkbox` - Checkbox controls
- `switch` - Toggle switches
- `radio` - Radio buttons
- `dropdown` - Spinners, dropdowns
- `slider` - Seek bars, sliders
- `progress` - Progress bars
- `list` - List views
- `scrollview` - Scrollable containers
- `container` - Layout containers
- `toolbar` - Toolbars, action bars
- `appbar` - App bars, title bars

**Expected Results:**
```
03:40:10.123 I VoiceOSService: ✓ Registered element: UUID=abc-123, type=button, name=Submit
03:40:10.124 I VoiceOSService: ✓ Registered element: UUID=def-456, type=input, name=Email
03:40:10.125 I VoiceOSService: ✓ Registered element: UUID=ghi-789, type=checkbox, name=Remember me
```

**Success Criteria:**
- ✅ All 15 element types detected correctly
- ✅ Type matches actual UI element behavior
- ✅ No `unknown` types for standard Android widgets

---

### Test 2.3: Position & Bounds Extraction

**Objective:** Verify accurate position data extraction

**Steps:**
1. Enable verbose logging if needed
2. Open app with visible elements
3. Check position data in logs

**Expected Results:**
```
03:40:10.123 D VoiceOSService:   Position: x=100, y=200, width=300, height=50
03:40:10.124 D VoiceOSService:   Bounds: Rect(100, 200, 400, 250)
03:40:10.125 D VoiceOSService:   Center: (250, 225)
```

**Success Criteria:**
- ✅ X, Y coordinates are positive and within screen bounds
- ✅ Width and height are positive
- ✅ Bounds rectangle is valid (left < right, top < bottom)
- ✅ Position data matches visual element location

**Validation Test:**
```bash
# Get screen dimensions
adb shell wm size
# Physical size: 1080x1920

# Verify all positions are within screen bounds
# Expected: 0 <= x < 1080, 0 <= y < 1920
```

---

## 🎤 Voice Command Testing

### Test 3.1: Voice Command Recognition Setup

**Objective:** Verify speech engine initialization and command registration

**Steps:**
1. Enable VoiceOS Service
2. Wait for voice initialization
3. Monitor logs for command registration

**Expected Results:**
```
03:40:15.123 D VoiceOSService: SPEECH_TEST: collect result SpeechState(isInitialized=true, isListening=false)
03:40:15.234 I VoiceOSService: Voice recognition initialized
03:40:15.345 D VoiceOSService: SPEECH_TEST: registerVoiceCmd commandsStr = [Settings, Wi-Fi, Bluetooth, Submit, Cancel, ...]
03:40:15.456 I SpeechEngineManager: Commands registered: 47 commands
```

**Success Criteria:**
- ✅ Speech engine initializes (isInitialized=true)
- ✅ Commands registered from current screen
- ✅ Command count matches registered elements

---

### Test 3.2: UUID-Based Voice Commands (Strategy 1)

**Objective:** Test direct UUID targeting via voice

**Test Cases:**

| Command | Expected Behavior | Verification |
|---------|-------------------|--------------|
| "Click button abc-123" | Clicks element with UUID abc-123 | Check click event in logs |
| "Select input def-456" | Focuses input field def-456 | Check focus event |
| "Tap image ghi-789" | Taps image element | Check action execution |

**Steps:**
1. Get UUID from logs (e.g., `UUID=abc-123`)
2. Speak: "Click button [UUID]"
3. Observe action execution

**Expected Results:**
```
03:40:20.123 D VoiceOSService: SPEECH_TEST: handleVoiceCommand confidence = 0.87 , command = click button abc-123
03:40:20.234 D VoiceOSService: UUID targeting: Found element abc-123
03:40:20.345 D VoiceOSService: SPEECH_TEST: performClick x = 250 , y = 225
03:40:20.456 I VoiceOSService: Click dispatched successfully
```

**Success Criteria:**
- ✅ Command recognized with confidence >0.5
- ✅ UUID resolved to correct element
- ✅ Click performed at correct coordinates
- ✅ Target app responds to click

---

### Test 3.3: Name-Based Voice Commands (Strategy 2)

**Objective:** Test targeting by element name/text

**Test Cases:**

| Command | Target Element | Expected Action |
|---------|----------------|-----------------|
| "Click Settings" | Settings button | Opens Settings |
| "Select Wi-Fi" | Wi-Fi list item | Opens Wi-Fi settings |
| "Tap Submit" | Submit button | Submits form |
| "Click login button" | Login button | Performs login click |

**Steps:**
1. Open app with named elements
2. Observe registered element names in logs
3. Speak element name with action verb
4. Verify action execution

**Expected Results:**
```
03:40:25.123 D VoiceOSService: SPEECH_TEST: handleVoiceCommand confidence = 0.92 , command = click settings
03:40:25.234 D VoiceOSService: Name-based match found: normalizedText=settings
03:40:25.345 D VoiceOSService: Matched element UUID: abc-123
03:40:25.456 D VoiceOSService: SPEECH_TEST: performClick x = 250 , y = 225
```

**Success Criteria:**
- ✅ Element matched by normalized text
- ✅ Case-insensitive matching works
- ✅ Click executed at correct element position
- ✅ Confidence score >0.5

---

### Test 3.4: Position-Based Commands (Strategy 3)

**Objective:** Test targeting by element position

**Test Cases:**

| Command | Expected Target | Notes |
|---------|----------------|-------|
| "Click first button" | Top-most button | Position index 0 |
| "Select third item" | 3rd list item | Position index 2 |
| "Tap last button" | Bottom-most button | Last in list |

**Steps:**
1. Open app with multiple similar elements
2. Speak position-based command
3. Verify correct element selected

**Success Criteria:**
- ✅ Position index correctly calculated
- ✅ Correct element targeted
- ✅ Works with ordinal numbers (first, second, third)

---

### Test 3.5: Type-Based Commands (Strategy 4)

**Objective:** Test targeting by element type

**Test Cases:**

| Command | Expected Behavior |
|---------|-------------------|
| "Focus text field" | Focuses first text input |
| "Select checkbox" | Selects first checkbox |
| "Click button" | Clicks first button on screen |

**Success Criteria:**
- ✅ Element type correctly identified
- ✅ First matching element selected
- ✅ Fallback to next element if first unavailable

---

### Test 3.6: Spatial Navigation (Strategy 5)

**Objective:** Test directional navigation commands

**Test Cases:**

| Command | Current Element | Expected Target |
|---------|----------------|-----------------|
| "Move left" | Button at (500,300) | Element to the left |
| "Go right" | List item | Next item to right |
| "Move up" | Bottom button | Button above |
| "Go down" | Top item | Item below |

**Steps:**
1. Establish current focus/position
2. Speak directional command
3. Verify focus moves in correct direction

**Expected Results:**
```
03:40:30.123 D VoiceOSService: Spatial navigation: direction=LEFT, from=(500,300)
03:40:30.234 D VoiceOSService: Found candidate: UUID=def-456, position=(200,300), distance=300
03:40:30.345 I VoiceOSService: Spatial navigation successful
```

**Success Criteria:**
- ✅ Direction correctly parsed
- ✅ Nearest element in direction found
- ✅ Distance calculation accurate (5-8ms target)

---

### Test 3.7: Recent Element Targeting (Strategy 6)

**Objective:** Test targeting recently used elements

**Test Cases:**

| Command | Expected Behavior |
|---------|-------------------|
| "Recent" | Returns last 10 accessed elements |
| "Recent button" | Returns recent buttons only |
| "Recent 5" | Returns last 5 accessed elements |
| "Recent 3 button" | Returns last 3 accessed buttons |

**Steps:**
1. Interact with several elements
2. Speak "recent" command
3. Verify correct elements returned

**Success Criteria:**
- ✅ Recent elements tracked in database
- ✅ Type filtering works
- ✅ Limit parameter respected
- ✅ Ordered by most recent first

---

### Test 3.8: Global Actions (Strategy 7)

**Objective:** Test global system actions

**Test Cases:**

| Command | Expected Action | Verification |
|---------|----------------|--------------|
| "Go back" | Back button | Navigates back |
| "Go home" | Home button | Returns to home screen |
| "Recent apps" | Recents | Shows app switcher |
| "Notifications" | Notification shade | Opens notifications |
| "Quick settings" | Quick settings | Opens quick settings |
| "Screenshot" | Take screenshot | Screenshot saved |

**Steps:**
1. Speak global command
2. Observe system response
3. Verify action executed

**Expected Results:**
```
03:40:35.123 D VoiceOSService: SPEECH_TEST: handleVoiceCommand confidence = 0.95 , command = go back
03:40:35.234 D VoiceOSService: Global action match: BACK
03:40:35.345 I VoiceOSService: Global action dispatched: BACK
```

**Success Criteria:**
- ✅ All 7 global actions work
- ✅ Commands recognized consistently
- ✅ System responds correctly

---

### Test 3.9: Voice Command Performance

**Objective:** Measure voice command latency

**Metrics to Collect:**
- Command recognition time
- UUID resolution time
- Click dispatch time
- End-to-end latency

**Expected Performance:**
```
Recognition: 40-60ms (target: <100ms)
UUID resolution: 1-2ms (target: <5ms)
Click dispatch: 5-10ms (target: <20ms)
End-to-end: 40-60ms (target: <100ms)
```

**Steps:**
1. Speak command
2. Note timestamp in logs
3. Note action execution timestamp
4. Calculate delta

**Success Criteria:**
- ✅ End-to-end latency <100ms
- ✅ 95th percentile <150ms
- ✅ No timeouts or failures

---

## 🔍 LearnApp Third-Party Testing

### Test 4.1: App Launch Detection

**Objective:** Verify LearnApp detects new third-party app launches

**Steps:**
1. Install a test app not previously seen
2. Launch the test app
3. Monitor logs for detection

**Expected Results:**
```
03:40:40.123 I VOS4LearnAppIntegration: New app detected: com.example.testapp
03:40:40.234 D VOS4LearnAppIntegration: App not in learned database
03:40:40.345 I VOS4LearnAppIntegration: Showing consent dialog for: TestApp
```

**Success Criteria:**
- ✅ App launch detected
- ✅ Package name identified
- ✅ Consent dialog displayed

---

### Test 4.2: User Consent Flow

**Objective:** Test consent dialog and user responses

**Test Cases:**

| User Action | Expected Behavior |
|-------------|-------------------|
| Tap "Allow" | Exploration starts |
| Tap "Decline" | Dialog dismissed, no exploration |
| Tap outside | Dialog dismissed (default: decline) |
| Check "Don't ask again" + Allow | Remembered for future |
| Check "Don't ask again" + Decline | Blacklisted |

**Steps:**
1. Trigger consent dialog
2. Interact with dialog options
3. Verify response handling

**Expected Results (Allow):**
```
03:40:45.123 I VOS4LearnAppIntegration: User approved exploration for: com.example.testapp
03:40:45.234 I VOS4LearnAppIntegration: Starting exploration engine
03:40:45.345 D ExplorationEngine: DFS traversal starting...
```

**Expected Results (Decline):**
```
03:40:45.123 I VOS4LearnAppIntegration: User declined exploration for: com.example.testapp
03:40:45.234 I VOS4LearnAppIntegration: App added to blacklist (user preference)
```

**Success Criteria:**
- ✅ Dialog appears within 2s of app launch
- ✅ "Allow" triggers exploration
- ✅ "Decline" dismisses without exploration
- ✅ Preferences saved to database

---

### Test 4.3: Automatic UI Exploration

**Objective:** Verify DFS-based UI exploration

**Steps:**
1. Allow exploration for test app
2. Observe exploration progress
3. Monitor logs for discovered elements

**Expected Results:**
```
03:40:50.123 I ExplorationEngine: Exploration started for: com.example.testapp
03:40:50.234 D ExplorationEngine: DFS depth=0, discovered 15 elements
03:40:50.345 D ExplorationEngine: Clicking element: Login Button (UUID: abc-123)
03:40:50.456 D ExplorationEngine: New screen detected, continuing exploration
03:40:50.567 D ExplorationEngine: DFS depth=1, discovered 8 elements
03:40:50.678 D ExplorationEngine: Back navigation, returning to depth=0
03:40:51.234 I ExplorationEngine: Exploration complete: 47 elements discovered
03:40:51.345 I VOS4LearnAppIntegration: Elements saved to database
```

**Success Criteria:**
- ✅ Exploration starts automatically after consent
- ✅ DFS traversal visits multiple screens
- ✅ Elements discovered and registered with UUIDs
- ✅ Back navigation works correctly
- ✅ Exploration completes (30-60s for typical app)
- ✅ No infinite loops or crashes

---

### Test 4.4: Progress Overlay

**Objective:** Test exploration progress UI

**Visual Checks:**
- ✅ Overlay appears during exploration
- ✅ Progress percentage updates
- ✅ "Pause" button functional
- ✅ "Resume" button functional (after pause)
- ✅ "Stop" button terminates exploration
- ✅ Overlay dismisses when complete

**Steps:**
1. Start exploration
2. Verify overlay appears
3. Test pause/resume functionality
4. Test stop functionality

**Expected Results:**
```
03:40:55.123 D ProgressOverlay: Showing exploration progress
03:40:55.234 D ProgressOverlay: Progress updated: 25% (12/47 elements)
03:40:55.345 D ProgressOverlay: User tapped: PAUSE
03:40:55.456 I ExplorationEngine: Exploration paused by user
03:40:56.123 D ProgressOverlay: User tapped: RESUME
03:40:56.234 I ExplorationEngine: Exploration resumed
```

**Success Criteria:**
- ✅ Overlay visible and non-intrusive
- ✅ Progress updates in real-time
- ✅ Pause/resume works without data loss
- ✅ Stop cleanly terminates exploration

---

### Test 4.5: Database Persistence

**Objective:** Verify learned elements persist across sessions

**Steps:**
1. Complete exploration for test app
2. Close app
3. Relaunch app
4. Verify no consent dialog (already learned)
5. Verify elements available for voice commands

**Database Verification:**
```bash
# Pull database from device
adb pull /data/data/com.augmentalis.voiceos/databases/uuid-database.db

# Query learned apps (use DB Browser or sqlite3)
sqlite3 uuid-database.db "SELECT * FROM learned_apps WHERE package_name='com.example.testapp';"
# Expected: Row with app data

# Query elements
sqlite3 uuid-database.db "SELECT COUNT(*) FROM uuid_elements WHERE app_package='com.example.testapp';"
# Expected: Element count from exploration
```

**Success Criteria:**
- ✅ App marked as learned in database
- ✅ Elements persisted with UUIDs
- ✅ Data survives app restart
- ✅ No consent dialog on subsequent launches

---

## ⚡ Performance Testing

### Test 5.1: Initialization Performance

**Objective:** Measure component initialization times

**Metrics:**

| Component | Target | Measurement |
|-----------|--------|-------------|
| UUIDCreator Init | <500ms | Actual: ____ms |
| LearnApp Init | <200ms | Actual: ____ms |
| Total Service Startup | <1000ms | Actual: ____ms |

**Steps:**
1. Clear logs
2. Enable service
3. Measure timestamps between log markers

**Expected Results:**
```
03:41:00.000 I VoiceOSService: VoiceOS Service connected
03:41:00.050 I VoiceOSService: === UUIDCreator Initialization Complete ===  // 50ms ✓
03:41:00.230 I VoiceOSService: === LearnApp Integration Initialization Complete ===  // 180ms ✓
03:41:00.800 I VoiceOSService: All components initialized with optimization  // 800ms total ✓
```

**Success Criteria:**
- ✅ UUIDCreator: 10x better than target (target 500ms, actual ~50ms)
- ✅ LearnApp: Under target (target 200ms)
- ✅ Total: Under 1 second

---

### Test 5.2: Tree Traversal Performance

**Objective:** Measure accessibility tree processing time

**Test Cases:**

| UI Complexity | Element Count | Target Time | Actual Time |
|---------------|---------------|-------------|-------------|
| Simple (Calculator) | 15-20 | <100ms | ____ms |
| Medium (Settings) | 30-50 | <150ms | ____ms |
| Complex (Browser) | 50-100 | <200ms | ____ms |
| Very Complex (Maps) | 100+ | <300ms | ____ms |

**Steps:**
1. Open test app
2. Note tree traversal log messages
3. Calculate time from start to completion

**Expected Log Format:**
```
03:41:05.000 D VoiceOSService: Processing accessibility tree for event: 32
03:41:05.112 I VoiceOSService: Tree traversal completed in 112ms  // ✓ Under 200ms target
03:41:05.113 I VoiceOSService: Total elements registered: 47
```

**Success Criteria:**
- ✅ 80-120ms for typical screens (target <200ms)
- ✅ Linear scaling with element count
- ✅ No timeouts or ANR (Application Not Responding)

---

### Test 5.3: Voice Command Latency

**Objective:** Measure end-to-end voice command execution time

**Test Procedure:**
1. Speak command
2. Note recognition timestamp
3. Note action execution timestamp
4. Calculate latency

**Target Breakdown:**
- Recognition: <50ms
- UUID Resolution: <2ms
- Click Dispatch: <10ms
- **Total: <100ms**

**Actual Measurements:**
```
Command: "Click Settings"
Recognition: 03:41:10.000 → 03:41:10.042  (42ms ✓)
Resolution:  03:41:10.042 → 03:41:10.044  (2ms ✓)
Dispatch:    03:41:10.044 → 03:41:10.052  (8ms ✓)
Total:       42ms + 2ms + 8ms = 52ms ✓ (Target: <100ms)
```

**Success Criteria:**
- ✅ 95th percentile <100ms
- ✅ 99th percentile <150ms
- ✅ No commands >200ms

---

### Test 5.4: Memory Usage

**Objective:** Verify memory footprint stays within target

**Monitoring Command:**
```bash
# Monitor VoiceOS memory usage
adb shell dumpsys meminfo com.augmentalis.voiceos

# Key metrics:
# - TOTAL PSS: Should be <100MB idle, <150MB active
# - Java Heap: Should be <50MB
# - Native Heap: Should be <30MB
```

**Memory Breakdown Target:**
- UUIDCreator Instance: ~2MB
- Registry Cache: ~5-10MB (1000-2000 elements)
- Database: ~1-5MB
- LearnApp Integration: ~3MB (during exploration)
- **Total Integration Overhead: <15MB ✓**

**Test Scenarios:**

| Scenario | Expected Total PSS | Notes |
|----------|-------------------|-------|
| Idle (no apps) | <100MB | Baseline |
| Active (1 app open) | <120MB | Normal usage |
| LearnApp Exploring | <140MB | Peak usage |
| After 1 hour | <120MB | No leaks |
| After 24 hours | <120MB | Stability |

**Success Criteria:**
- ✅ Idle memory <100MB
- ✅ Active memory <150MB
- ✅ No memory leaks over 24 hours
- ✅ Integration overhead <15MB

---

## 🔄 Memory & Stability Testing

### Test 6.1: Memory Leak Detection

**Objective:** Verify no memory leaks over extended use

**Test Duration:** 24 hours minimum

**Procedure:**
```bash
# Start monitoring script
cat > /tmp/memtest.sh << 'EOF'
#!/bin/bash
while true; do
    timestamp=$(date "+%Y-%m-%d %H:%M:%S")
    pss=$(adb shell dumpsys meminfo com.augmentalis.voiceos | grep "TOTAL PSS" | awk '{print $3}')
    echo "$timestamp, $pss KB" >> /tmp/voiceos-memory-log.csv
    sleep 300  # Log every 5 minutes
done
EOF

chmod +x /tmp/memtest.sh
/tmp/memtest.sh &

# Let run for 24 hours
# Then analyze: plot data or check for upward trend
```

**Analysis:**
```bash
# Check for memory growth
tail -n 100 /tmp/voiceos-memory-log.csv | awk -F, '{print $2}' | \
  gnuplot -e "set terminal dumb; plot '-' with lines"
```

**Success Criteria:**
- ✅ Memory stays relatively flat over 24 hours
- ✅ No continuous upward trend
- ✅ Occasional spikes acceptable (garbage collection)
- ✅ Returns to baseline after GC

**Failure Indicators:**
- ❌ Continuous upward trend >1MB/hour
- ❌ Memory not returning to baseline
- ❌ Out of memory crashes

---

### Test 6.2: Service Lifecycle Testing

**Objective:** Test service survival across system events

**Test Cases:**

| Event | Expected Behavior | Verification |
|-------|-------------------|--------------|
| App backgrounded | Service continues | Check logs |
| Device sleep | Service pauses | Check logs |
| Device wake | Service resumes | Check logs |
| Low memory | Service persists | Check `dumpsys` |
| System killed service | Service restarts | Check logs |
| User disables service | Clean shutdown | Check cleanup logs |

**Steps:**
1. Monitor logs continuously
2. Trigger each event
3. Verify service response

**Expected Results (Sleep/Wake):**
```
03:41:30.000 D VoiceOSService: App moved to background
03:41:30.001 D VoiceOSService: Evaluating foreground service need
03:41:35.000 D VoiceOSService: Device sleep detected
[... device asleep ...]
03:42:00.000 D VoiceOSService: Device wake detected
03:42:00.001 D VoiceOSService: App moved to foreground
03:42:00.002 I VoiceOSService: Service resumed successfully
```

**Success Criteria:**
- ✅ Service survives all lifecycle events
- ✅ Clean shutdown on service disable
- ✅ Proper restart after system kill
- ✅ No data loss during transitions

---

### Test 6.3: Stress Testing

**Objective:** Test system under heavy load

**Stress Test Scenarios:**

**Scenario 1: Rapid App Switching**
```bash
# Script to rapidly switch apps
for i in {1..100}; do
    adb shell am start -n com.android.settings/.Settings
    sleep 1
    adb shell am start -n com.google.android.calculator/.Calculator
    sleep 1
done
```
**Expected:** Service handles all switches without errors

**Scenario 2: Rapid Voice Commands**
- Speak 20 commands in 60 seconds
- Verify all processed correctly
- No command queue overflow

**Scenario 3: Large UI Trees**
- Open complex app (e.g., Google Maps with many UI elements)
- Verify traversal completes
- No timeouts or ANR

**Success Criteria:**
- ✅ Handles 100+ app switches without errors
- ✅ Processes rapid voice commands (20/minute)
- ✅ Handles UIs with 200+ elements
- ✅ No ANR (Application Not Responding) dialogs
- ✅ No crashes

---

## ❌ Error Handling Testing

### Test 7.1: Graceful Degradation

**Objective:** Verify system continues working when components fail

**Test Cases:**

| Failure Scenario | Expected Behavior |
|------------------|-------------------|
| UUIDCreator init fails | Service starts without UUID features, logs error |
| LearnApp init fails | Service continues, LearnApp unavailable, logs warning |
| Speech engine fails | Service works without voice, manual commands still work |
| Database write fails | In-memory cache continues, logs error |
| Network unavailable | Local features work normally |

**Steps:**
1. Simulate component failure (e.g., corrupt database)
2. Observe service behavior
3. Verify fallback mechanisms

**Expected Results (LearnApp Failure):**
```
03:41:45.123 I VoiceOSService: === LearnApp Integration Initialization Start ===
03:41:45.234 E VoiceOSService: ✗ Failed to initialize LearnApp integration
03:41:45.235 E VoiceOSService: Error type: NullPointerException
03:41:45.236 E VoiceOSService: Error message: Database not accessible
03:41:45.237 W VoiceOSService: Service will continue without LearnApp integration
03:41:45.345 I VoiceOSService: All components initialized with optimization
```

**Success Criteria:**
- ✅ Service continues running despite failures
- ✅ Errors logged clearly with context
- ✅ No cascading failures
- ✅ User informed of unavailable features

---

### Test 7.2: Invalid Input Handling

**Objective:** Test system behavior with invalid inputs

**Test Cases:**

| Invalid Input | Expected Behavior |
|--------------|-------------------|
| Null accessibility event | Event skipped, logged, no crash |
| Malformed voice command | Command ignored, logged |
| Invalid UUID | Error logged, fallback to name-based |
| Corrupted element data | Element skipped, tree traversal continues |

**Steps:**
1. Trigger invalid input scenario
2. Verify error handling
3. Check logs for appropriate error messages

**Success Criteria:**
- ✅ No crashes on invalid input
- ✅ Errors logged with details
- ✅ System recovers automatically

---

### Test 7.3: Edge Cases

**Test Edge Cases:**
- Empty screen (no UI elements)
- Screen with 500+ elements
- Duplicate element names
- Elements with no text/description
- Rapid screen changes (animations)
- System dialogs (permissions, etc.)

**Success Criteria:**
- ✅ Handles all edge cases gracefully
- ✅ No infinite loops
- ✅ No crashes
- ✅ Logs explain unusual situations

---

## 🔧 Troubleshooting

### Common Issues

#### Issue 1: Service Not Starting

**Symptoms:**
- No logs appearing
- Service not listed in accessibility settings

**Checks:**
```bash
# Check if app is installed
adb shell pm list packages | grep augmentalis

# Check if service is declared in manifest
adb shell dumpsys package com.augmentalis.voiceos | grep VoiceOSService

# Check for crash logs
adb logcat -s AndroidRuntime:E
```

**Solutions:**
- Reinstall app: `./gradlew :app:installDebug`
- Grant all permissions manually
- Reboot device

---

#### Issue 2: UUIDCreator Not Initializing

**Symptoms:**
```
✗ CRITICAL: Failed to initialize UUIDCreator
```

**Checks:**
```bash
# Check if UUIDCreator module compiled
adb shell pm list packages | grep augmentalis
adb logcat -s UUIDCreator:*

# Check database permissions
adb shell ls -l /data/data/com.augmentalis.voiceos/databases/
```

**Solutions:**
- Clear app data: `adb shell pm clear com.augmentalis.voiceos`
- Grant storage permissions
- Check for database corruption

---

#### Issue 3: Voice Commands Not Working

**Symptoms:**
- Commands spoken but no action
- Low confidence scores

**Checks:**
```bash
# Check microphone permissions
adb shell dumpsys package com.augmentalis.voiceos | grep RECORD_AUDIO

# Check speech engine status
adb logcat -s SpeechEngineManager:* VoiceOSService:*

# Verify commands registered
# Look for: "SPEECH_TEST: registerVoiceCmd commandsStr = [...]"
```

**Solutions:**
- Grant microphone permission
- Check speech engine initialization
- Verify element names in logs
- Test with simple commands first ("Go back")

---

#### Issue 4: LearnApp Not Detecting Apps

**Symptoms:**
- No consent dialog appears
- App launches not detected

**Checks:**
```bash
# Check LearnApp integration logs
adb logcat -s VOS4LearnAppIntegration:* ExplorationEngine:*

# Check accessibility events
adb logcat -s VoiceOSService:* | grep "TYPE_WINDOW_STATE_CHANGED"
```

**Solutions:**
- Verify accessibility service enabled
- Check LearnApp initialization succeeded
- Test with fresh app (uninstall/reinstall)

---

#### Issue 5: High Memory Usage

**Symptoms:**
- PSS >150MB during normal use
- OOM (Out of Memory) crashes

**Checks:**
```bash
# Detailed memory breakdown
adb shell dumpsys meminfo com.augmentalis.voiceos

# Check for memory leaks
adb shell dumpsys meminfo com.augmentalis.voiceos | grep "Activities\|Views"
```

**Solutions:**
- Clear cache: `adb shell pm clear com.augmentalis.voiceos`
- Check for element cache explosion (limit should be 1000-2000)
- Monitor logs for excessive tree traversals

---

### Debug Logging Commands

```bash
# Enable verbose logging (if implemented)
adb shell setprop log.tag.VoiceOSService VERBOSE
adb shell setprop log.tag.UUIDCreator VERBOSE

# Full system dump
adb shell dumpsys activity services VoiceOSService > /tmp/voiceos-dump.txt

# Accessibility service info
adb shell dumpsys accessibility | grep -A 50 VoiceOS

# Package info
adb shell dumpsys package com.augmentalis.voiceos > /tmp/package-info.txt
```

---

## 📊 Test Results Template

### Test Session Information

**Date:** _______________
**Tester:** _______________
**Device Model:** _______________
**Android Version:** _______________
**Build Version:** _______________

---

### Installation & Setup

| Test | Pass | Fail | Notes |
|------|------|------|-------|
| App installs successfully | ☐ | ☐ | |
| Service appears in accessibility settings | ☐ | ☐ | |
| Service enables successfully | ☐ | ☐ | |
| All permissions granted | ☐ | ☐ | |
| Logs show service connected | ☐ | ☐ | |

---

### UUIDCreator Integration

| Test | Pass | Fail | Notes |
|------|------|------|-------|
| UUIDCreator initializes | ☐ | ☐ | Time: ___ms |
| Hilt dependencies injected | ☐ | ☐ | |
| Tree traversal triggers | ☐ | ☐ | |
| Elements registered (>10) | ☐ | ☐ | Count: ___ |
| 15 element types detected | ☐ | ☐ | |
| Position data accurate | ☐ | ☐ | |
| Traversal time <200ms | ☐ | ☐ | Time: ___ms |

---

### Voice Commands

| Test | Pass | Fail | Notes |
|------|------|------|-------|
| Speech engine initializes | ☐ | ☐ | |
| Commands registered | ☐ | ☐ | Count: ___ |
| UUID-based targeting | ☐ | ☐ | |
| Name-based targeting | ☐ | ☐ | |
| Position-based targeting | ☐ | ☐ | |
| Type-based targeting | ☐ | ☐ | |
| Spatial navigation | ☐ | ☐ | |
| Recent element targeting | ☐ | ☐ | |
| Global actions | ☐ | ☐ | |
| Command latency <100ms | ☐ | ☐ | Avg: ___ms |

---

### LearnApp Integration

| Test | Pass | Fail | Notes |
|------|------|------|-------|
| LearnApp initializes | ☐ | ☐ | |
| App launch detected | ☐ | ☐ | |
| Consent dialog appears | ☐ | ☐ | |
| Consent "Allow" works | ☐ | ☐ | |
| Consent "Decline" works | ☐ | ☐ | |
| Exploration starts | ☐ | ☐ | |
| Progress overlay visible | ☐ | ☐ | |
| Pause/Resume works | ☐ | ☐ | |
| Stop works | ☐ | ☐ | |
| Elements saved to database | ☐ | ☐ | Count: ___ |
| No consent on relaunch | ☐ | ☐ | |

---

### Performance

| Metric | Target | Actual | Pass | Fail |
|--------|--------|--------|------|------|
| UUIDCreator init time | <500ms | ___ms | ☐ | ☐ |
| Tree traversal time | <200ms | ___ms | ☐ | ☐ |
| Voice command latency | <100ms | ___ms | ☐ | ☐ |
| Memory usage (idle) | <100MB | ___MB | ☐ | ☐ |
| Memory usage (active) | <150MB | ___MB | ☐ | ☐ |

---

### Stability

| Test | Pass | Fail | Notes |
|------|------|------|-------|
| No crashes during testing | ☐ | ☐ | Crash count: ___ |
| No ANR dialogs | ☐ | ☐ | |
| Survives app switching | ☐ | ☐ | |
| Survives sleep/wake | ☐ | ☐ | |
| Survives 24-hour test | ☐ | ☐ | |
| No memory leaks | ☐ | ☐ | |

---

### Error Handling

| Test | Pass | Fail | Notes |
|------|------|------|-------|
| Graceful degradation works | ☐ | ☐ | |
| Invalid inputs handled | ☐ | ☐ | |
| Errors logged clearly | ☐ | ☐ | |
| No cascading failures | ☐ | ☐ | |

---

### Overall Assessment

**Overall Test Result:** ☐ Pass  ☐ Fail

**Blocker Issues:**
1. _______________________________________________
2. _______________________________________________
3. _______________________________________________

**Minor Issues:**
1. _______________________________________________
2. _______________________________________________
3. _______________________________________________

**Recommendations:**
_______________________________________________
_______________________________________________
_______________________________________________

**Tester Signature:** _______________
**Date:** _______________

---

## 📝 Notes

- This guide covers manual testing. Automated test suite is located in `/modules/apps/VoiceAccessibility/src/test/`
- For performance benchmarking, consider using Android Profiler in Android Studio
- For memory leak detection, consider using LeakCanary library
- All tests should be performed on physical devices, not emulators (accessibility features work differently)
- Test on multiple Android versions (9, 10, 11, 12, 13, 14) if possible
- Test on different device types (phones, tablets, smart glasses) if available

---

**Document Version:** 1.0.0
**Last Updated:** 2025-10-09 03:40:00 PDT
**Status:** Production Ready
**Author:** VOS4 Development Team
