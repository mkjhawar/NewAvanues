# Batch 10 Hardware Stubs Fix — F4, F9, F10

**Module**: DeviceManager + VoiceOSCore
**Branch**: VoiceOS-1M-SpeechEngine
**Date**: 2026-02-22
**Version**: V1

---

## Summary

Replaced 3 remaining hardcoded stubs from Deep Review Batch 10 with real implementations that use the existing hardware manager APIs.

## Fixes

### F4: calibrateSpatialMapping() — HUDManager.kt

**Before:** Ignored `calibrationPoints` parameter entirely. Set `isCalibrated = true` and `trackingQuality = 0.95f` unconditionally.

**After:** Real calibration logic:
1. Validates minimum 3 verified calibration points with non-zero accuracy
2. Reads live IMU orientation via `IMUManager.getCurrentOrientation()`
3. Computes tracking quality as weighted combination: `meanAccuracy * 0.6 + countConfidence * 0.3 + imuBonus * 0.1`
4. Derives head position from calibration point centroid
5. Sets head rotation from real IMU data (pitch/yaw/roll in radians)
6. Returns false with descriptive error if validation fails

### F9: scanBluetoothDevices() — DeviceViewModel.kt

**Before:** `delay(5000)` + hardcoded list of 3 fake devices ("Headphones", "Smartwatch", "Speaker").

**After:** Wired to `BluetoothManager.startDiscovery()`:
1. Validates Bluetooth is supported and enabled
2. Calls `startDiscovery(duration = 10_000)` for classic + BLE scan
3. Waits for scan completion (duration + 1.5s margin)
4. Reads `discoveredDevices` StateFlow — maps `BluetoothDeviceInfo` to display strings with name, address, and RSSI
5. Catches `SecurityException` for missing BLUETOOTH_SCAN permission

### F10: scanWiFiNetworks() — DeviceViewModel.kt

**Before:** `delay(3000)` + hardcoded list of 4 fake networks ("HomeNetwork", "OfficeWiFi", etc.).

**After:** Wired to `WiFiManager.startScan()`:
1. Validates WiFi is enabled
2. Calls system `startScan()`
3. Waits for fresh BroadcastReceiver results via `scanResults.drop(1).first()` with 10s timeout
4. Falls back to current scan results on timeout
5. Maps `WiFiNetwork` to display strings with SSID, WiFi standard (4/5/6/6E/7), signal level, and security type
6. Catches `SecurityException` for missing location permission

## Files Modified

| File | Lines Changed | Purpose |
|------|--------------|---------|
| `Modules/DeviceManager/.../DeviceViewModel.kt` | +95/-30 | F9 + F10 real scanning |
| `Modules/VoiceOSCore/.../HUDManager.kt` | +48/-14 | F4 real calibration |
| `Docs/Plans/NewAvanues-Plan-DeepReviewFixPrioritization-260221-V1.md` | updated | Mark F4/F9/F10 FIXED, Batch 10 19/19 |

## Key Insight

The `BluetoothManager` and `WiFiManager` were already fully implemented with production-grade scanning APIs, rich data models, and StateFlows. The stubs in `DeviceViewModel` simply bypassed them. The fix was pure plumbing — no new manager code was needed.

## Batch 10 Status

All 19/19 items now resolved. Deep review fix plan is 100% complete across all 10 batches.
