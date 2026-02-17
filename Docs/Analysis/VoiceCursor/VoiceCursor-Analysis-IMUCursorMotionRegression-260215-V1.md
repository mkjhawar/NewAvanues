# VoiceCursor Analysis: IMU Cursor Motion Regression

**Module:** VoiceCursor
**Type:** Analysis / Investigation
**Date:** 2026-02-15
**Version:** V1
**Branch:** IosVoiceOS-Development
**Status:** Investigation complete, fix pending

---

## Symptom

The cursor overlay renders correctly and is centered on screen, but does **not move** in response to head motion (IMU sensor input). The cursor remains static at its initial position.

## Related Commit

- `4e2e486e` feat(voicecursor): Wire IMU head tracking to cursor overlay via KMP flow bridge
- This commit introduced the KMP flow bridge connecting IMU sensors to cursor position

---

## Data Flow Pipeline (As Designed)

```
IMUManager.onSensorChanged (SensorEventListener)
  ↓ Sensor.TYPE_ROTATION_VECTOR
IMUManager.processRotationVector()
  ↓ SensorManager.getRotationMatrixFromVector()
  ↓ SensorManager.remapCoordinateSystem()
  ↓ SensorManager.getOrientation() → orientationBuffer[alpha, beta, gamma]
  ↓ imuDataPool.acquire() → populate IMUData
  ↓ _orientationFlow.emit(cursorData)
HeadTrackingBridge.toCursorInputFlow()
  ↓ orientationFlow.map { IMUData → CursorInput.HeadMovement }
CursorController.connectInputFlow()
  ↓ scope.launch { flow.collect { input → update(input, now) } }
CursorController.update()
  ↓ computeHeadMovementPosition() → CursorPosition
  ↓ filter.filter(rawPosition) [if jitterFilterEnabled]
  ↓ _state.value = currentState.copy(position = clampedPosition, ...)
CursorOverlayService.observeCursorState()
  ↓ serviceScope.launch { cursorController?.state?.collect { state → ... } }
  ↓ repositionOverlay(state)
WindowManager.updateViewLayout(overlayView, params)
  ↓ Cursor moves on screen
```

---

## Root Cause Hypotheses (Ranked by Probability)

### 1. IMU Sensors Not Starting (60% probability)

The `startIMUTracking()` call may return `false` due to:
- `deviceCapabilities == null` (line 162 in IMUManager)
- `DeviceDetector.getCapabilities()` not returning valid IMU sensor info
- Rotation sensor lazy property returning null (no gyro/magnetometer on device)

**Files:** `IMUManager.kt:162-164`, `IMUManager.kt:244-319`

### 2. Flow Not Collecting (20% probability)

The coroutine scope may be cancelled or the flow collection job cancelled after connection.

**Files:** `CursorController.kt:268-283`, `CursorOverlayService.kt:193-200`

### 3. Sensor Accuracy Blocking (10% probability)

All sensor events blocked by `mLastAccuracy == SensorManager.SENSOR_STATUS_UNRELIABLE`.

**Files:** `IMUManager.kt:333-334`

### 4. Dead Zone Filtering (5% probability)

`DEAD_ZONE_RAD` (0.002) may be too large for actual head movements, filtering out all motion.

**Files:** `CursorController.kt:307-356`

### 5. Rate Limiting (5% probability)

`HEAD_MIN_INTERVAL_MS` (8ms) or `MIN_SENSOR_INTERVAL_NS` (8ms) blocking updates.

**Files:** `IMUManager.kt:369-380`

---

## Key Files and Lines

| File | Line(s) | What to Check |
|------|---------|---------------|
| `IMUManager.kt` | 162-164 | `deviceCapabilities` null check |
| `IMUManager.kt` | 244-319 | Sensor start — `sensorsStarted > 0`? |
| `IMUManager.kt` | 333-334 | `mLastAccuracy` blocking events |
| `IMUManager.kt` | 366-416 | `processRotationVector` + flow emit |
| `HeadTrackingBridge.kt` | 30-37 | map function called? |
| `CursorController.kt` | 268-283 | flow collection active? |
| `CursorController.kt` | 307-356 | `computeHeadMovementPosition` deltas |
| `CursorOverlayService.kt` | 193-200 | state collection + repositionOverlay |

---

## Recommended Fix Approach

1. **Add logging at each pipeline stage** to trace where data stops flowing
2. **Verify IMU initialization** — check for "IMU tracking started with N sensors" in logs
3. **Check sensor availability** — `DeviceDetector.getCapabilities()` must report IMU sensors
4. **Verify flow emissions** — log each stage: emit → map → collect → update → reposition
5. **Check for silent exceptions** in coroutine scopes (caught and swallowed)

## Debugging Commands

```bash
# Check if IMU sensors are registering
adb logcat | grep -i "IMU\|sensor\|cursor\|tracking"

# Check for the specific flow bridge
adb logcat | grep "HeadTrackingBridge\|CursorController\|CursorOverlay"
```

---

## Resolution Status

| Step | Status |
|------|--------|
| Identify symptom | DONE |
| Trace data pipeline | DONE |
| Rank hypotheses | DONE |
| Add diagnostic logging | PENDING |
| Verify on device | PENDING |
| Implement fix | PENDING |
