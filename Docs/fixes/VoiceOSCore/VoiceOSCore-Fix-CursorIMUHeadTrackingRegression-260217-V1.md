# VoiceOSCore Fix: Cursor IMU Head-Tracking Regression

**Date:** 2026-02-17
**Module:** VoiceOSCore + DeviceManager
**Type:** Bug Fix (dual root cause)
**Branch:** IosVoiceOS-Development

## Problem

Cursor renders and centers on screen correctly but does not move with head motion.
The IMU data pipeline exists but was broken at two independent points.

## Root Cause Analysis

### Bug 1: IMUManager sensor gate mismatch (Primary)

**File:** `Modules/DeviceManager/src/androidMain/.../imu/IMUManager.kt`

The lazy `rotationSensor` property (line 105-112) resolves if the device has
EITHER a gyroscope OR magnetometer (uses `||`). However, `startSensors()` at
line 258 required BOTH sensors (used `&&`).

On devices/emulators without a magnetometer, the rotation sensor object was
lazily created but never registered with `SensorManager.registerListener()`,
so `_orientationFlow` never emitted data.

The gyroscope fallback (line 272) only feeds `_motionFlow` (angular velocity),
not `_orientationFlow` (euler angles), so the cursor pipeline received no data.

**Before:**
```kotlin
if (caps?.hasGyroscope == true && caps.hasMagnetometer) {
```

**After:**
```kotlin
if (caps?.hasGyroscope == true || caps?.hasMagnetometer == true) {
```

### Bug 2: Missing IMU wiring in AndroidCursorHandler (Secondary)

**File:** `Modules/VoiceOSCore/src/androidMain/.../AndroidCursorHandler.kt`

`wireServiceDependencies()` wired CursorActions (voice movement commands) and
ClickDispatcher (tap dispatch) but never called `svc.startIMUTracking(imuManager)`.

When the cursor was started via voice command ("show cursor"), the IMU-to-cursor
pipeline was never connected. Only the Settings toggle path wired IMU (via a
different code path in the app layer).

**Fix:** Added IMU wiring after CursorActions/ClickDispatcher setup:
```kotlin
val imuManager = IMUManager.getInstance(service.applicationContext)
val imuStarted = imuManager.startIMUTracking("cursor_voice")
if (imuStarted) {
    svc.startIMUTracking(imuManager)
}
```

Also added `stopIMUTracking("cursor_voice")` in `hideCursor()` to properly
release sensor resources when cursor is hidden via voice.

## Data Pipeline (Verified)

```
IMUManager.onSensorChanged()
  → processRotationVector()
    → _orientationFlow.emit(IMUData)
      → HeadTrackingBridge.toCursorInputFlow()
        → CursorInput.HeadMovement(pitch, yaw, roll)
          → CursorController.connectInputFlow()
            → update() → computeHeadMovementPosition()
              → _state (cursor position)
```

## Files Modified

| File | Change |
|------|--------|
| `Modules/DeviceManager/.../IMUManager.kt` | Line 258: `&&` → `||` to match lazy property |
| `Modules/VoiceOSCore/.../AndroidCursorHandler.kt` | Added IMU import, startIMUTracking in wireServiceDependencies, stopIMUTracking in hideCursor |

## Testing Notes

- On emulators: rotation sensor may not be available; verify with `adb shell dumpsys sensorservice`
- On RealWear: has accelerometer + gyroscope but may lack magnetometer — this fix enables that config
- Consumer ID `"cursor_voice"` allows independent lifecycle from Settings-based cursor start
