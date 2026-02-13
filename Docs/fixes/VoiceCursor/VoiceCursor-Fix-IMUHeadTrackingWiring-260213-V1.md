# VoiceCursor Fix: Wire IMU Head Tracking to VoiceCursor

**Date:** 2026-02-13
**Branch:** VoiceOSCore-KotlinUpdate
**Status:** Implemented

## Problem

Cursor overlay displays correctly but doesn't move with head motion. The old `VoiceCursorIMUIntegration.kt` was never ported during app consolidation. All IMU pipeline pieces exist (IMUManager, CursorAdapter, CursorController) but are disconnected.

## Root Cause

During the consolidation from the legacy VoiceCursor standalone app to the Avanues monorepo, the integration glue between DeviceManager's IMU system and VoiceCursor's KMP CursorController was lost. The old integration file lived in `Archives/deprecated/VoiceCursor-App-260204/` and was not ported.

## Solution: KMP Flow Bridge (Direct Wiring)

Instead of recreating the old integration file, we used a lighter approach:
- Extension function on IMUManager produces a `Flow<CursorInput.HeadMovement>`
- CursorController gained `connectInputFlow()`/`disconnectInputFlow()` for continuous input
- CursorOverlayService exposes `startIMUTracking()`/`stopIMUTracking()`
- VoiceAvanueAccessibilityService lazy-inits IMUManager and wires it when cursor is enabled

### Tangent-Based Displacement Algorithm

Ported the proven algorithm from `CursorAdapter.processOrientationForCursor()`:

```
displacement_x = tan(delta_roll) * screenWidth * 2.0
                + tan(delta_yaw) * screenWidth * 2.0 * (speed * 0.2)
displacement_y = tan(delta_pitch) * screenHeight * 3.0 * (speed * 0.2)
```

Safety features:
- Dead zone: 0.002 radians (~0.11 degrees) filters sensor noise
- Rate limiting: 8ms minimum interval between updates
- Angle clamping: rejects deltas > 1.4 rad (~80 degrees) to prevent tan() blowup
- Fine tuning: movements < 1% of screen dimension scaled by 0.4x for precision

## Files Modified

| File | Change |
|------|--------|
| `Modules/VoiceCursor/src/commonMain/.../CursorController.kt` | Added `connectInputFlow`/`disconnectInputFlow`, tangent-based HeadMovement computation, head tracking state management |
| `Modules/VoiceCursor/src/androidMain/.../input/HeadTrackingBridge.kt` | **NEW** — Extension function `IMUManager.toCursorInputFlow()` mapping IMUData to CursorInput.HeadMovement |
| `Modules/VoiceCursor/src/androidMain/.../overlay/CursorOverlayService.kt` | Added `startIMUTracking()`/`stopIMUTracking()`, cleanup in onDestroy |
| `apps/avanues/.../service/VoiceAvanueAccessibilityService.kt` | Lazy IMUManager init with DeviceDetector capabilities, start/stop IMU tracking with cursor lifecycle |
| `Modules/VoiceCursor/build.gradle.kts` | Added `implementation(project(":Modules:DeviceManager"))` to androidMain |

## Data Flow

```
IMUManager (sensors → rotation vector → orientation)
    ↓ orientationFlow: SharedFlow<IMUData>
HeadTrackingBridge.toCursorInputFlow()
    ↓ Flow<CursorInput.HeadMovement>
CursorController.connectInputFlow()
    ↓ computeHeadMovementPosition() → tangent displacement
    ↓ CompositeCursorFilter (jitter smoothing)
    ↓ screen bounds clamping
CursorController.state: StateFlow<CursorState>
    ↓ collected by CursorOverlayService
WindowManager.updateViewLayout() → cursor moves on screen
```

## IMU Axis Mapping

| IMUData field | Sensor Meaning | CursorInput.HeadMovement | Cursor Effect |
|---------------|---------------|--------------------------|---------------|
| alpha | Roll (X-axis rotation) | roll | Primary horizontal |
| beta | Pitch (Y-axis rotation) | pitch | Vertical |
| gamma | Yaw (Z-axis azimuth) | yaw | Secondary horizontal |

## Testing Notes

- Toggle cursor in Settings → VoiceCursor → Enable Cursor
- IMUManager requires device with accelerometer/gyroscope/magnetometer
- On emulators: cursor will appear but not move (no IMU sensors)
- DeviceDetector.getCapabilities() detects sensor availability; IMU gracefully no-ops without sensors
