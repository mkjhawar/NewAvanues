# AVA NLU Testing Protocol

**Version:** 1.0
**Date:** 2025-11-17
**Author:** AVA Team

## Overview

This document provides a comprehensive testing protocol for AVA's Natural Language Understanding (NLU) system. It covers the complete workflow from .ava file extraction to intent classification.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [.ava File Structure](#ava-file-structure)
3. [Current .ava Files](#current-ava-files)
4. [Testing Workflow](#testing-workflow)
5. [Expected Intent Classifications](#expected-intent-classifications)
6. [Troubleshooting](#troubleshooting)
7. [Performance Metrics](#performance-metrics)

---

## Prerequisites

### Required Files

1. **ONNX Model:**
   - Path: `/sdcard/Android/data/com.augmentalis.ava.debug/files/models/AVA-ONX-384-BASE-INT8.onnx`
   - Size: ~12-15 MB (full model with weights)
   - Purpose: MobileBERT embedding model for semantic similarity

2. **.ava Intent Files:**
   - Path: `/sdcard/Android/data/com.augmentalis.ava.debug/files/.ava/core/en-US/`
   - Files:
     - `media-control.ava` (~2.5 KB)
     - `navigation.ava` (~2.2 KB)
     - `system-control.ava` (~3.5 KB)

### Required Permissions

- Storage access (app-specific external storage)
- Internet (for future model downloads)

---

## .ava File Structure

### Format Specification

.ava files use JSON format with the following structure:

```json
{
  "s": "ava-1.0",              // Schema version
  "v": "1.0.0",                // File version
  "l": "en-US",                // Locale
  "m": {                       // Metadata
    "f": "system-control.ava", // Filename
    "c": "system_control",     // Category ID
    "n": "System Control",     // Category name
    "d": "System settings...", // Description
    "cnt": 12                  // Intent count
  },
  "i": [                       // Intents array
    {
      "id": "wifi_on",         // Intent ID
      "c": "turn on wifi",     // Primary command
      "s": [                   // Synonyms
        "enable wifi",
        "wifi on",
        "activate wifi",
        "connect wifi"
      ],
      "cat": "system_control", // Category
      "p": 1,                  // Priority (1=high)
      "t": ["wifi", "network"] // Tags
    }
  ]
}
```

---

## Current .ava Files

### 1. media-control.ava

**Intents (8):**
- `play_music` - "play music", "start music", "play song"
- `pause_music` - "pause", "stop music", "pause playback"
- `next_track` - "next song", "skip", "next track"
- `previous_track` - "previous song", "go back", "last track"
- `volume_up` - "increase volume", "louder", "turn up"
- `volume_down` - "decrease volume", "quieter", "turn down"
- `mute` - "mute", "silence", "turn off sound"
- `unmute` - "unmute", "sound on", "turn on sound"

**Test Commands:**
```
"play some music"      → play_music
"pause the music"      → pause_music
"next song please"     → next_track
"make it louder"       → volume_up
```

### 2. navigation.ava

**Intents (7):**
- `navigate_home` - "go home", "navigate home", "take me home"
- `navigate_to` - "navigate to", "directions to", "take me to"
- `find_nearby` - "find nearby", "what's near me", "search nearby"
- `show_traffic` - "show traffic", "traffic update", "how's traffic"
- `eta` - "eta", "how long", "time to arrive"
- `alternate_route` - "alternate route", "different route", "avoid traffic"
- `cancel_navigation` - "cancel navigation", "stop navigating", "end route"

**Test Commands:**
```
"navigate to the store"     → navigate_to
"find nearby restaurants"   → find_nearby
"show me traffic"           → show_traffic
"what's my eta"             → eta
```

### 3. system-control.ava

**Intents (12):**
- `wifi_on` - "turn on wifi", "enable wifi", "wifi on"
- `wifi_off` - "turn off wifi", "disable wifi", "wifi off"
- `bluetooth_on` - "turn on bluetooth", "enable bluetooth"
- `bluetooth_off` - "turn off bluetooth", "disable bluetooth"
- `airplane_mode_on` - "airplane mode on", "enable airplane mode"
- `airplane_mode_off` - "airplane mode off", "disable airplane mode"
- `screen_brightness_up` - "increase brightness", "brighter screen"
- `screen_brightness_down` - "decrease brightness", "dimmer screen"
- `battery_saver_on` - "battery saver on", "enable battery saver"
- `battery_saver_off` - "battery saver off", "disable battery saver"
- `do_not_disturb_on` - "do not disturb on", "dnd on", "silence notifications"
- `do_not_disturb_off` - "do not disturb off", "dnd off", "enable notifications"

**Test Commands:**
```
"turn on wifi"              → wifi_on
"enable bluetooth"          → bluetooth_on
"airplane mode on"          → airplane_mode_on
"increase brightness"       → screen_brightness_up
"battery saver on"          → battery_saver_on
```

**Total Intents:** 27 (8 + 7 + 12)

---

## Testing Workflow

### Phase 1: Initial Setup

1. **Install APK:**
   ```bash
   adb -s emulator-5554 install -r apps/ava-standalone/build/outputs/apk/debug/ava-standalone-debug.apk
   ```

2. **Verify .ava Files Extracted:**
   ```bash
   adb -s emulator-5554 shell ls -lh /sdcard/Android/data/com.augmentalis.ava.debug/files/.ava/core/en-US/
   ```

   Expected output:
   ```
   -rw-rw---- 1 media_rw media_rw 2.5K media-control.ava
   -rw-rw---- 1 media_rw media_rw 2.2K navigation.ava
   -rw-rw---- 1 media_rw media_rw 3.5K system-control.ava
   ```

3. **Verify ONNX Model:**
   ```bash
   adb -s emulator-5554 shell ls -lh /sdcard/Android/data/com.augmentalis.ava.debug/files/models/
   ```

   Expected output:
   ```
   -rw-rw---- 1 media_rw media_rw 14M AVA-ONX-384-BASE-INT8.onnx
   ```

### Phase 2: Database Reload

1. **Open AVA App**

2. **Tap Voice Command FAB (Microphone icon at bottom-right)**

3. **Navigate to Voice Commands:**
   - Tap "Voice" category
   - Tap "Reload Data" button (🔄)

4. **Verify Success Toast:**
   ```
   "✅ Loaded 27 NLU examples from .ava files"
   ```

   **Note:** If you see a different number (e.g., 6), the database is using built-in intents instead of .ava files.

5. **Check Logcat:**
   ```bash
   adb -s emulator-5554 logcat -s NLUDebugManager IntentSourceCoordinator IntentClassifier
   ```

   Expected output:
   ```
   I/NLUDebugManager: === Starting NLU Data Reload ===
   I/NLUDebugManager: Clearing existing database...
   I/IntentSourceCoordinator: Cleared 6 examples from database
   I/NLUDebugManager: Forcing migration from .ava sources...
   I/IntentSourceCoordinator: Loading from .ava files for locale: en-US
   I/IntentSourceCoordinator: Loaded 27 examples from 27 intents (.ava files)
   I/IntentClassifier: === NLU Initialization Complete ===
   I/IntentClassifier: Loaded 27 intent embeddings
   I/IntentClassifier:   ✓ wifi_on
   I/IntentClassifier:   ✓ wifi_off
   I/IntentClassifier:   ✓ bluetooth_on
   ...
   I/NLUDebugManager: === NLU Data Reload Complete ===
   ```

### Phase 3: Intent Classification Testing

1. **Test System Control Commands:**

   ```
   Input: "turn on wifi"
   Expected: wifi_on (confidence: 0.85+)

   Input: "enable bluetooth"
   Expected: bluetooth_on (confidence: 0.85+)

   Input: "airplane mode on"
   Expected: airplane_mode_on (confidence: 0.85+)

   Input: "increase brightness"
   Expected: screen_brightness_up (confidence: 0.80+)
   ```

2. **Test Media Control Commands:**

   ```
   Input: "play some music"
   Expected: play_music (confidence: 0.85+)

   Input: "pause the music"
   Expected: pause_music (confidence: 0.85+)

   Input: "next song please"
   Expected: next_track (confidence: 0.85+)

   Input: "make it louder"
   Expected: volume_up (confidence: 0.75+)
   ```

3. **Test Navigation Commands:**

   ```
   Input: "navigate to the store"
   Expected: navigate_to (confidence: 0.85+)

   Input: "find nearby restaurants"
   Expected: find_nearby (confidence: 0.85+)

   Input: "what's my eta"
   Expected: eta (confidence: 0.80+)
   ```

4. **Test Unknown Commands:**

   ```
   Input: "open calendar"
   Expected: unknown (confidence: <0.60)

   Input: "hello world"
   Expected: unknown (confidence: <0.60)
   ```

### Phase 4: Logcat Analysis

Monitor logcat during testing:

```bash
adb -s emulator-5554 logcat -s IntentClassifier | grep -A 5 "Classifying"
```

**Expected log output for "turn on wifi":**
```
I/IntentClassifier: === Classifying: "turn on wifi" ===
I/IntentClassifier: Candidate intents: wifi_on, wifi_off, bluetooth_on, ...
I/IntentClassifier: Using method: Semantic Similarity
I/IntentClassifier:   wifi_on: 0.92
I/IntentClassifier:   wifi_off: 0.35
I/IntentClassifier:   bluetooth_on: 0.41
I/IntentClassifier: Best match: wifi_on (confidence: 0.92)
I/IntentClassifier: Threshold: 0.6, Confidence: 0.92
I/IntentClassifier: FINAL DECISION: wifi_on
I/IntentClassifier: === Classification Complete (45ms) ===
```

**❌ BAD output (database not loaded):**
```
I/IntentClassifier: === Classifying: "turn on wifi" ===
I/IntentClassifier: Candidate intents: control_lights, control_temperature, ...
I/IntentClassifier:   control_lights: 0.0
I/IntentClassifier:   control_temperature: 0.0
I/IntentClassifier: Best match: control_lights (confidence: 0.0)
I/IntentClassifier: FINAL DECISION: unknown
```

---

## Expected Intent Classifications

### High Confidence (0.85+)

These commands should match with 85%+ confidence:

| Command | Expected Intent | Confidence |
|---------|----------------|------------|
| "turn on wifi" | wifi_on | 0.90+ |
| "enable bluetooth" | bluetooth_on | 0.88+ |
| "play music" | play_music | 0.92+ |
| "pause" | pause_music | 0.85+ |
| "navigate home" | navigate_home | 0.90+ |

### Medium Confidence (0.70-0.84)

These commands may have slight ambiguity:

| Command | Expected Intent | Confidence |
|---------|----------------|------------|
| "make it louder" | volume_up | 0.75+ |
| "brighter" | screen_brightness_up | 0.72+ |
| "silence notifications" | do_not_disturb_on | 0.78+ |

### Low Confidence / Unknown (<0.60)

These commands should return "unknown":

| Command | Expected Intent | Confidence |
|---------|----------------|------------|
| "open calendar" | unknown | <0.50 |
| "hello world" | unknown | <0.30 |
| "what's the weather" | unknown | <0.40 |

---

## Troubleshooting

### Issue 1: Toast shows "Loaded 6 examples" instead of 27

**Cause:** Database migration failed, using built-in intents

**Solution:**
1. Check .ava files exist:
   ```bash
   adb -s emulator-5554 shell ls /sdcard/Android/data/com.augmentalis.ava.debug/files/.ava/core/en-US/
   ```

2. Check logcat for migration errors:
   ```bash
   adb -s emulator-5554 logcat -s IntentSourceCoordinator | grep -i error
   ```

3. Manually clear app data and reinstall:
   ```bash
   adb -s emulator-5554 shell pm clear com.augmentalis.ava.debug
   adb -s emulator-5554 install -r apps/ava-standalone/build/outputs/apk/debug/ava-standalone-debug.apk
   ```

### Issue 2: All confidence scores are 0.0

**Cause:** Embeddings not computed, using keyword matching fallback

**Solution:**
1. Check ONNX model exists:
   ```bash
   adb -s emulator-5554 shell ls -lh /sdcard/Android/data/com.augmentalis.ava.debug/files/models/
   ```

2. Verify model size is ~14 MB (not 311 KB)

3. Check logcat for ONNX initialization errors:
   ```bash
   adb -s emulator-5554 logcat -s IntentClassifier | grep -i onnx
   ```

### Issue 3: "Reload Data" button does nothing

**Cause:** Coroutine exception or missing permissions

**Solution:**
1. Check logcat for exceptions:
   ```bash
   adb -s emulator-5554 logcat -s NLUDebugManager
   ```

2. Verify storage permissions granted

3. Restart app and try again

---

## Performance Metrics

### Target Performance

| Metric | Target | Acceptable | Unacceptable |
|--------|--------|------------|--------------|
| **Inference Time** | <50ms | <100ms | >100ms |
| **Accuracy (High-Priority)** | 90%+ | 85%+ | <85% |
| **Accuracy (Medium-Priority)** | 80%+ | 75%+ | <75% |
| **Memory Usage** | <100 MB | <150 MB | >150 MB |
| **Model Size** | ~14 MB | ~20 MB | >30 MB |

### Measurement Commands

**Inference Time:**
```bash
adb -s emulator-5554 logcat -s IntentClassifier | grep "Classification Complete"
```

**Memory Usage:**
```bash
adb -s emulator-5554 shell dumpsys meminfo com.augmentalis.ava.debug | grep TOTAL
```

---

## Test Results Template

Use this template to record test results:

```markdown
## Test Session: [Date]

**Environment:**
- Device: emulator-5554
- Android Version: 14
- AVA Version: 1.0.0-debug
- .ava Files Loaded: [27 / 6 / other]

**System Control (12 intents):**
- ✅ wifi_on: "turn on wifi" → 0.92 (45ms)
- ✅ bluetooth_on: "enable bluetooth" → 0.88 (42ms)
- ⚠️  screen_brightness_up: "brighter" → 0.68 (low confidence)
- ❌ airplane_mode_on: "airplane mode on" → unknown (failed)

**Media Control (8 intents):**
- ✅ play_music: "play music" → 0.94 (48ms)
- ✅ pause_music: "pause" → 0.87 (43ms)

**Navigation (7 intents):**
- ✅ navigate_home: "go home" → 0.91 (46ms)
- ✅ find_nearby: "find nearby" → 0.85 (50ms)

**Summary:**
- Total Tests: 20
- Passed (0.85+): 16 (80%)
- Medium (0.70-0.84): 3 (15%)
- Failed (<0.70): 1 (5%)
- Average Inference Time: 45ms
```

---

## Continuous Testing

For automated testing, consider implementing:

1. **Unit Tests:**
   - Test .ava file parsing
   - Test database migration
   - Test embedding computation

2. **Integration Tests:**
   - Test end-to-end classification
   - Test confidence threshold tuning
   - Test performance benchmarks

3. **UI Tests:**
   - Test "Reload Data" command
   - Test voice command overlay
   - Test error handling

---

## Next Steps

After successful testing:

1. **Tune confidence thresholds** based on real-world accuracy
2. **Add more .ava files** for additional domains (weather, calendar, etc.)
3. **Implement continuous learning** (track misclassifications, add examples)
4. **Performance optimization** (model quantization, caching)

---

## Support

For issues or questions:
- Check logcat first: `adb logcat -s IntentClassifier NLUDebugManager`
- Review this protocol: `/docs/NLU-Testing-Protocol.md`
- Contact: AVA Team

---

**Last Updated:** 2025-11-17
**Protocol Version:** 1.0
