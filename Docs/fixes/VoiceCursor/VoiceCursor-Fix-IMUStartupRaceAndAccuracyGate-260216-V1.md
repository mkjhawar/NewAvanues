# VoiceCursor Fix: IMU Startup Race Condition & Accuracy Gate

**Module:** VoiceCursor + DeviceManager
**Date:** 2026-02-16
**Branch:** IosVoiceOS-Development
**Priority:** HIGH
**Device:** RealWear Smart Glasses (head-tracking cursor)

## Symptoms

- Cursor overlay displays correctly and is centered on screen
- Cursor does NOT move with head motion (IMU sensor input)
- Dwell clicks fire continuously at center position (no movement to cancel dwell)
- No IMU/HeadTracking log entries in logcat

## Root Cause Analysis (CoT)

### Bug 1: Race Condition in Service Startup (PRIMARY)

**File:** `apps/avanues/.../VoiceAvanueAccessibilityService.kt` (lines 397-494)

The startup chain in the settings observation flow:
1. `collectLatest` receives settings with `cursorEnabled = true`
2. Line 400: `CursorOverlayService.getInstance() == null` → true
3. Line 403: `startForegroundService(intent)` — **async call**, returns immediately
4. Line 470: `CursorOverlayService.getInstance()?.let { }` — **still null** because
   `onStartCommand()` hasn't run yet to set `instance = this`
5. IMU wiring block is entirely SKIPPED
6. No more settings emissions → IMU tracking NEVER starts

**Why the cursor appears:** `CursorOverlayService.onStartCommand()` runs shortly after
and creates the overlay window, but the IMU flow is never connected.

### Bug 2: Sensor Accuracy Gate Blocks All Events (SECONDARY)

**File:** `Modules/DeviceManager/.../imu/IMUManager.kt` (line 65, 335-338)

```kotlin
private var mLastAccuracy = 0  // 0 == SensorManager.SENSOR_STATUS_UNRELIABLE

override fun onSensorChanged(event: SensorEvent) {
    if (mLastAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) return  // BLOCKS ALL EVENTS
}
```

`mLastAccuracy` is initialized to `0` (`SENSOR_STATUS_UNRELIABLE`). This means ALL sensor
events are silently dropped until `onAccuracyChanged()` fires with accuracy >= 1. On RealWear
smart glasses, this callback may not fire immediately (or at all for some sensor types),
causing additional blocking even after the IMU flow is connected.

## Fix Applied

### Fix 1: Polling Wait After Service Start

Added a polling loop (100ms intervals, 2s timeout) after `startForegroundService()` to wait
for `CursorOverlayService.getInstance()` to become non-null before proceeding to IMU wiring:

```kotlin
var waitMs = 0
while (CursorOverlayService.getInstance() == null && waitMs < 2000) {
    delay(100)
    waitMs += 100
}
```

This runs inside a coroutine (`collectLatest` lambda in `serviceScope`), so `delay()` is
non-blocking. If `collectLatest` cancels (new emission), the polling stops naturally.

### Fix 2: Initialize Accuracy to ACCURACY_LOW

Changed `mLastAccuracy` from `0` (`SENSOR_STATUS_UNRELIABLE`) to
`SensorManager.SENSOR_STATUS_ACCURACY_LOW` (1), so sensor events flow immediately after
`registerListener()`. If `onAccuracyChanged()` later reports `SENSOR_STATUS_UNRELIABLE`,
events will be properly blocked.

### Enhanced Diagnostics

Added logging for:
- DeviceDetector sensor capabilities (accel/gyro/mag)
- IMU tracking start result and pipeline connection
- CursorOverlayService readiness timing
- Null service fallback warning

## Files Modified

| File | Change |
|------|--------|
| `apps/avanues/.../VoiceAvanueAccessibilityService.kt` | Added `delay` import, polling wait after startForegroundService, enhanced IMU diagnostic logging |
| `Modules/DeviceManager/.../imu/IMUManager.kt` | Changed mLastAccuracy init from 0 to SENSOR_STATUS_ACCURACY_LOW |

## Expected Log Output (After Fix)

```
I/VoiceAvanueService: CursorOverlayService started via settings toggle
I/VoiceAvanueService: CursorOverlayService ready after 200ms
I/VoiceAvanueService: DeviceDetector sensors: accel=true, gyro=true, mag=true
I/VoiceAvanueService: IMUManager initialized with device capabilities
I/IMUManager: Starting IMU tracking for first consumer: VoiceCursor
I/IMUManager: Started rotation sensor: ...
I/IMUManager: IMU tracking started with 1 sensors
I/VoiceAvanueService: IMU head tracking connected to cursor — pipeline active
I/CursorOverlayService: IMU head tracking connected to cursor controller
```

## Testing

Deploy to RealWear smart glasses:
1. Enable cursor in Settings → VoiceCursor → Toggle ON
2. Verify logs show polling wait and successful IMU pipeline connection
3. Move head — cursor should follow head motion
4. Verify dwell click only fires when cursor is stationary over interactive element
