# DeviceManager Deprecated Code Archive

**Archived:** 2026-01-29
**Original Location:** `Modules/DeviceManager/_deprecated/`
**Size:** 1.3 MB (64 Kotlin files, 21,139 LOC)

## Contents

Legacy Android-only implementations that were duplicated during KMP migration:
- IMU/sensor fusion managers
- Video manager
- Foldable device support
- XR compatibility layer
- Audio processing
- Network managers (Bluetooth, WiFi, UWB, NFC)
- Device detection and certification

## Reason for Archive

This code was the original Android-only structure located at:
```
_deprecated/main/java/com/augmentalis/devicemanager/
```

It was migrated to the KMP structure at:
```
src/androidMain/kotlin/com/augmentalis/devicemanager/
```

The files are **exact duplicates** with only package path changes. Analysis confirmed:
- DeviceInfo.kt: Identical (0 diff lines)
- BiometricManager.kt: Identical (0 diff lines)
- DeviceManager.kt: 2 import path changes only
- LidarManager.kt: 1 package declaration change only

## Recovery

If any code is needed, it can be referenced from this archive or from git history.
