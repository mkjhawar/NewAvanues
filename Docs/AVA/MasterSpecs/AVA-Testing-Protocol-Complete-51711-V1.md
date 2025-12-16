# AVA Complete Testing Protocol

**Version**: 1.0
**Last Updated**: 2025-11-17
**Purpose**: Verify NLU, LLM, and Action Handler functionality

---

## Table of Contents

1. [Pre-Test Setup](#1-pre-test-setup)
2. [NLU Testing Protocol](#2-nlu-testing-protocol)
3. [LLM Testing Protocol](#3-llm-testing-protocol)
4. [Action Handler Testing Protocol](#4-action-handler-testing-protocol)
5. [VoiceOS Integration Testing](#5-voiceos-integration-testing)
6. [End-to-End Testing](#6-end-to-end-testing)
7. [Performance Benchmarks](#7-performance-benchmarks)
8. [Troubleshooting](#8-troubleshooting)

---

## 1. Pre-Test Setup

### 1.1 Build and Install

```bash
# Build debug APK
./gradlew :apps:ava-standalone:assembleDebug

# Install on device
adb install -r apps/ava-standalone/build/outputs/apk/debug/ava-standalone-debug.apk
```

### 1.2 Enable Logging

```bash
# Clear previous logs
adb logcat -c

# Start logging with filters
adb logcat -s "AVA:*" "NLU:*" "ChatViewModel:*" "ActionsInitializer:*" "VoiceOSConnection:*"
```

### 1.3 Verify Initialization

Look for these log messages on app start:

```
I/ActionsInitializer: Action handlers initialized in XXms
I/ActionsInitializer: Registered 121 handlers: [...]
I/IntentClassifier: NLU model loaded successfully
I/ALCEngine: LLM model loaded successfully
```

---

## 2. NLU Testing Protocol

### 2.1 Intent Classification Tests

Test each category with sample utterances:

#### Core Intents
| Utterance | Expected Intent | Expected Confidence |
|-----------|-----------------|---------------------|
| "What time is it?" | show_time | > 0.8 |
| "Set an alarm for 7am" | set_alarm | > 0.7 |
| "What's the weather?" | check_weather | > 0.8 |

#### Settings Intents
| Utterance | Expected Intent | Expected Confidence |
|-----------|-----------------|---------------------|
| "Open settings" | open_settings | > 0.8 |
| "Open Gmail" | open_app | > 0.7 |
| "Open security settings" | open_security | > 0.7 |
| "Show sound settings" | open_sound | > 0.7 |

#### System Control Intents
| Utterance | Expected Intent | Expected Confidence |
|-----------|-----------------|---------------------|
| "Turn on WiFi" | wifi_on | > 0.8 |
| "Turn off Bluetooth" | bluetooth_off | > 0.8 |
| "Volume up" | volume_up | > 0.8 |
| "Turn on flashlight" | flashlight_on | > 0.8 |
| "What's my battery level?" | battery_status | > 0.7 |

#### Media Control Intents
| Utterance | Expected Intent | Expected Confidence |
|-----------|-----------------|---------------------|
| "Play music" | play_music | > 0.8 |
| "Pause" | pause_music | > 0.7 |
| "Next song" | next_track | > 0.8 |
| "Previous track" | previous_track | > 0.7 |

#### Navigation Intents
| Utterance | Expected Intent | Expected Confidence |
|-----------|-----------------|---------------------|
| "Go home" | go_home | > 0.8 |
| "Go back" | go_back | > 0.8 |
| "Show recent apps" | recent_apps | > 0.7 |
| "Show notifications" | notifications | > 0.7 |

#### VoiceOS Intents
| Utterance | Expected Intent | Expected Confidence |
|-----------|-----------------|---------------------|
| "Scroll down" | scroll_down | > 0.8 |
| "Swipe left" | swipe_left | > 0.7 |
| "Click" | single_click | > 0.7 |
| "Double click" | double_click | > 0.7 |

### 2.2 Confidence Threshold Test

1. Set confidence threshold to 0.5 (default)
2. Test these edge cases:

| Utterance | Expected Behavior |
|-----------|-------------------|
| "Blah blah random" | unknown → Teach AVA button |
| "Turn on the light" | Low confidence → Teach AVA |
| "Show me the clock" | show_time with moderate confidence |

### 2.3 Verify via Logs

```bash
# Check classification results
adb logcat -s "IntentClassifier:*" | grep "Classified"
```

Expected log format:
```
D/IntentClassifier: Classified 'what time is it' as 'show_time' (confidence: 0.92)
```

---

## 3. LLM Testing Protocol

### 3.1 LLM Initialization

Verify LLM loads successfully:

```bash
adb logcat -s "ALCEngine:*" "TVMRuntime:*"
```

Expected logs:
```
I/ALCEngine: Loading language pack: en
I/TVMRuntime: Model loaded successfully
I/ALCEngine: LLM ready for inference
```

### 3.2 Response Generation Tests

Test LLM response quality:

| Query | Expected Response Type |
|-------|------------------------|
| "Tell me a joke" | Humorous response |
| "What is Python?" | Informational response |
| "Help me understand loops" | Educational response |

### 3.3 Hybrid Response Tests

Test the template + LLM hybrid system:

| Intent | Expected Response |
|--------|-------------------|
| show_time | Template: "It's X:XX AM/PM on Day, Month Date" |
| unknown + question | LLM-generated response |
| check_weather | Template with weather data |

### 3.4 Streaming Test

1. Ask a longer question
2. Verify response streams (not all at once)
3. Check for smooth token-by-token display

---

## 4. Action Handler Testing Protocol

### 4.1 Core Handlers (3)

| Test | Command | Expected Result |
|------|---------|-----------------|
| 4.1.1 | "What time is it?" | Shows current time |
| 4.1.2 | "Set alarm for 7am" | Opens Clock app |
| 4.1.3 | "What's the weather?" | Shows weather info |

### 4.2 Settings Handlers (8)

| Test | Command | Expected Result |
|------|---------|-----------------|
| 4.2.1 | "Open settings" | Opens Settings app |
| 4.2.2 | "Open Gmail" | Opens Gmail app |
| 4.2.3 | "Open security settings" | Opens Security settings |
| 4.2.4 | "Open WiFi settings" | Opens WiFi settings |
| 4.2.5 | "Open sound settings" | Opens Sound settings |
| 4.2.6 | "Open display settings" | Opens Display settings |
| 4.2.7 | "Show about device" | Opens About phone |
| 4.2.8 | "Quick settings" | Opens quick settings panel |

### 4.3 System Control Handlers (18)

| Test | Command | Expected Result |
|------|---------|-----------------|
| 4.3.1 | "Turn on WiFi" | Opens WiFi settings |
| 4.3.2 | "Turn off WiFi" | Opens WiFi settings |
| 4.3.3 | "Turn on Bluetooth" | Opens Bluetooth settings |
| 4.3.4 | "Turn off Bluetooth" | Opens Bluetooth settings |
| 4.3.5 | "Volume up" | Increases volume with UI |
| 4.3.6 | "Volume down" | Decreases volume with UI |
| 4.3.7 | "Mute" | Mutes volume |
| 4.3.8 | "Unmute" | Unmutes volume |
| 4.3.9 | "Battery status" | Shows "Battery is at X%" |
| 4.3.10 | "Turn on flashlight" | Turns on torch |
| 4.3.11 | "Turn off flashlight" | Turns off torch |
| 4.3.12 | "Airplane mode on" | Opens airplane settings |
| 4.3.13 | "Airplane mode off" | Opens airplane settings |
| 4.3.14 | "Brightness up" | Increases brightness |
| 4.3.15 | "Brightness down" | Decreases brightness |
| 4.3.16 | "Lock screen" | Locks device (or VoiceOS) |
| 4.3.17 | "Take screenshot" | Routes to VoiceOS |

### 4.4 Media Control Handlers (6)

| Test | Command | Expected Result |
|------|---------|-----------------|
| 4.4.1 | "Play music" | Sends play key event |
| 4.4.2 | "Pause" | Sends pause key event |
| 4.4.3 | "Next track" | Sends next key event |
| 4.4.4 | "Previous track" | Sends previous key event |
| 4.4.5 | "Shuffle on" | Info message |
| 4.4.6 | "Repeat" | Info message |

### 4.5 Navigation Handlers (12)

| Test | Command | Expected Result |
|------|---------|-----------------|
| 4.5.1 | "Go home" | Goes to home screen |
| 4.5.2 | "Go back" | Routes to VoiceOS |
| 4.5.3 | "Recent apps" | Routes to VoiceOS |
| 4.5.4 | "Show notifications" | Expands notification panel |
| 4.5.5 | "Hide notifications" | Collapses panel |
| 4.5.6 | "Open browser" | Opens default browser |
| 4.5.7 | "Menu" | Routes to VoiceOS |

---

## 5. VoiceOS Integration Testing

### 5.1 Prerequisites

- VoiceOS app installed
- Accessibility service enabled

### 5.2 VoiceOS Status Check

```bash
# Check if VoiceOS is detected
adb logcat -s "VoiceOSConnection:*"
```

Expected for VoiceOS installed:
```
D/VoiceOSConnection: VoiceOS found: com.avanues.voiceos
D/VoiceOSConnection: VoiceOS accessibility service is running
```

### 5.3 VoiceOS Routing Tests

| Test | Command | Expected Result |
|------|---------|-----------------|
| 5.3.1 | "Go back" | VoiceOS executes back |
| 5.3.2 | "Recent apps" | VoiceOS shows recents |
| 5.3.3 | "Scroll down" | VoiceOS scrolls |
| 5.3.4 | "Swipe left" | VoiceOS swipes |
| 5.3.5 | "Click" | VoiceOS clicks |
| 5.3.6 | "Double click" | VoiceOS double clicks |
| 5.3.7 | "Screenshot" | VoiceOS takes screenshot |

### 5.4 VoiceOS Not Available Test

1. Uninstall VoiceOS or disable accessibility service
2. Test VoiceOS-routing commands
3. Verify error messages:

| Command | Expected Error |
|---------|----------------|
| "Go back" | "VoiceOS accessibility service is not running..." |
| "Scroll down" | "VoiceOS is not installed..." |

---

## 6. End-to-End Testing

### 6.1 Complete Flow Test

1. **Voice Input → NLU → Action → Response**
   - Tap mic button
   - Say "What time is it?"
   - Verify: Intent classified, action executed, response shown

2. **Text Input → NLU → Action → Response**
   - Type "Open settings"
   - Verify: Settings app opens

3. **Low Confidence → Teach AVA**
   - Type something ambiguous
   - Verify: "Teach AVA" button appears

4. **Teach New Intent**
   - Tap "Teach AVA"
   - Add training example
   - Test same phrase again
   - Verify: Now recognized correctly

### 6.2 Conversation Flow Test

```
User: "What time is it?"
AVA: "It's 3:45 PM on Sunday, November 17"

User: "Open Gmail"
AVA: "Opening Gmail"
[Gmail app opens]

User: "Go back"
AVA: [Back navigation via VoiceOS]

User: "Volume up"
AVA: "Volume increased"
[Volume slider appears]
```

### 6.3 Error Handling Test

| Scenario | Expected Behavior |
|----------|-------------------|
| Network error | Graceful fallback |
| Model not loaded | Show loading indicator |
| Permission denied | Request permission |
| App not installed | "I couldn't find app X" |

---

## 7. Performance Benchmarks

### 7.1 Expected Performance

| Metric | Target | Acceptable |
|--------|--------|------------|
| NLU classification | < 50ms | < 100ms |
| Action execution | < 100ms | < 500ms |
| LLM first token | < 1s | < 2s |
| LLM total response | < 5s | < 10s |
| Handler registration | < 50ms | < 100ms |

### 7.2 Memory Usage

| Component | Expected |
|-----------|----------|
| NLU model | ~50MB |
| LLM model | ~500MB |
| Handler registry | < 1MB |
| Total app | < 600MB |

### 7.3 Measure Performance

```bash
# Check NLU performance
adb logcat -s "IntentClassifier:*" | grep "ms"

# Check action performance
adb logcat -s "ActionsManager:*" | grep "executed"

# Check memory
adb shell dumpsys meminfo com.augmentalis.ava
```

---

## 8. Troubleshooting

### 8.1 NLU Not Classifying Correctly

**Symptoms**: Wrong intent or low confidence

**Solutions**:
1. Check model is loaded: `adb logcat -s "IntentClassifier:*"`
2. Verify .ava files loaded: `adb logcat -s "IntentSourceCoordinator:*"`
3. Check candidate intents: `adb logcat -s "ChatViewModel:*" | grep "candidate"`

### 8.2 Actions Not Executing

**Symptoms**: Intent classified but no action

**Solutions**:
1. Check handler registered: Look for intent in ActionsInitializer logs
2. Verify handler execution: `adb logcat -s "*ActionHandler:*"`
3. Check permissions: Especially for settings, flashlight

### 8.3 VoiceOS Not Responding

**Symptoms**: VoiceOS commands fail

**Solutions**:
1. Verify VoiceOS installed: `adb shell pm list packages | grep voiceos`
2. Check accessibility: Settings > Accessibility > VoiceOS
3. Check logs: `adb logcat -s "VoiceOSConnection:*"`

### 8.4 LLM Not Responding

**Symptoms**: No LLM response or slow

**Solutions**:
1. Check model loaded: `adb logcat -s "ALCEngine:*"`
2. Verify memory: `adb shell dumpsys meminfo`
3. Check TVM runtime: `adb logcat -s "TVMRuntime:*"`

---

## Test Checklist

### Pre-Release Checklist

- [ ] All 121 handlers registered
- [ ] NLU classifies all 116 intents
- [ ] Core handlers (time, alarm, weather) work
- [ ] Settings handlers work
- [ ] System control handlers work
- [ ] Media control handlers work
- [ ] Navigation handlers work
- [ ] VoiceOS routing works (when available)
- [ ] VoiceOS error messages work (when unavailable)
- [ ] LLM responds correctly
- [ ] Teach AVA flow works
- [ ] Performance within targets
- [ ] Memory usage acceptable
- [ ] No crashes or ANRs

### Sign-Off

| Tester | Date | Result |
|--------|------|--------|
| | | |

---

**Document Version**: 1.0
**Last Updated**: 2025-11-17
