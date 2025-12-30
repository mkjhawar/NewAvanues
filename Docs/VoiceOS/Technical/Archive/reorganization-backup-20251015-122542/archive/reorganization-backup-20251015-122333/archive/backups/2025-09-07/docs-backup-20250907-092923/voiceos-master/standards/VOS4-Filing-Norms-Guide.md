# MANDATORY FILING NORMS FOR VOS4
**Version**: 2.0  
**Date**: January 30, 2025  
**Status**: MANDATORY - ALL DEVELOPERS MUST FOLLOW
**Changes**: DeviceManager structure updated with new categories

## ⚠️ CRITICAL: READ BEFORE ANY CODE CHANGES

This document defines the MANDATORY file organization structure for the VOS4 project. 
**FAILURE TO FOLLOW THESE NORMS WILL RESULT IN PR REJECTION.**

## 📁 Master File Structure Pattern

### RULE 1: Package Path Must Match File Path

The package declaration MUST exactly match the file system path:

```
File Location:
/ModuleName/src/main/java/com/augmentalis/modulename/type/FileName.kt

Package Declaration:
package com.augmentalis.modulename.type
```

### RULE 2: Standard Module Structure

Every module MUST follow this structure:

```
ModuleName/
├── src/
│   ├── main/
│   │   ├── java/com/augmentalis/modulename/
│   │   │   ├── Type1/           # Group by functionality type
│   │   │   │   ├── File1.kt
│   │   │   │   └── File2.kt
│   │   │   ├── Type2/
│   │   │   │   └── Files.kt
│   │   │   └── ModuleMain.kt    # Main facade class at root
│   │   ├── res/                 # Android resources
│   │   └── AndroidManifest.xml
│   └── test/                    # Unit tests
│       └── java/com/augmentalis/modulename/
└── build.gradle.kts
```

## 📋 DeviceManager Specific Structure

### CORRECT Structure (MANDATORY - Updated Jan 30, 2025):

```
DeviceManager/
└── src/main/java/com/augmentalis/devicemanager/
    ├── accessibility/            # Accessibility features
    │   └── AccessibilityManager.kt
    ├── audio/                    # Audio-related functionality
    │   ├── AudioRouting.kt
    │   ├── SpatialAudio.kt
    │   └── AudioService.kt
    ├── compatibility/           # API compatibility layers
    │   └── ApiCompatibility.kt
    ├── dashboardui/             # Dashboard UI components
    │   ├── DeviceViewModel.kt
    │   ├── DeviceInfoUI.kt
    │   └── DeviceManagerActivity.kt
    ├── deviceinfo/              # Device information
    │   ├── cache/              # Caching layer
    │   ├── certification/      # Certification detection
    │   ├── detection/          # Device detection
    │   └── manufacturer/       # Manufacturer features
    ├── display/                 # Display management
    │   └── DisplayOverlayManager.kt
    ├── network/                 # ALL network functionality
    │   ├── BluetoothManager.kt
    │   ├── WiFiManager.kt
    │   ├── UwbManager.kt
    │   ├── NfcManager.kt
    │   ├── CellularManager.kt
    │   └── UsbNetworkManager.kt
    ├── security/                # Security features
    │   └── BiometricManager.kt
    ├── sensors/                 # ALL sensor functionality
    │   ├── imu/                # IMU subsystem
    │   │   ├── IMUManager.kt
    │   │   ├── CursorAdapter.kt
    │   │   └── SensorFusion.kt
    │   └── LidarManager.kt
    ├── smartdevices/            # Smart device support
    │   └── FoldableDeviceManager.kt
    ├── smartglasses/            # AR glasses support
    │   └── GlassesManager.kt
    ├── usb/                     # USB monitoring
    │   └── USBDeviceMonitor.kt
    ├── video/                   # Video/camera management
    │   └── VideoManager.kt
    └── DeviceManager.kt         # Main facade class

```

### INCORRECT Structure (WILL BE REJECTED):

```
❌ DeviceManager/managers/network/    # Redundant "managers" folder
❌ DeviceManager/NetworkManager.kt    # Monolithic file at root
❌ DeviceManager/src/.../devicemanager/managers/  # Double "manager" naming
```

## 🚫 Prohibited Patterns

### 1. Redundant Naming
```kotlin
// ❌ WRONG
com.augmentalis.devicemanager.managers.network.NetworkManager

// ✅ CORRECT
com.augmentalis.devicemanager.network.WiFiManager
```

### 2. Monolithic Classes
```kotlin
// ❌ WRONG - Single class handling multiple concerns
class NetworkManager {
    fun handleWiFi() {}
    fun handleBluetooth() {}
    fun handleNFC() {}
    fun handleCellular() {}
}

// ✅ CORRECT - Separate managers for each concern
class WiFiManager { }
class BluetoothManager { }
class NfcManager { }
```

### 3. Inconsistent Placement
```
❌ WRONG:
devicemanager/
├── audio/AudioRouting.kt      # Some in subfolder
├── NetworkManager.kt           # Some at root
└── managers/display/           # Some in "managers"

✅ CORRECT:
devicemanager/
├── audio/AudioRouting.kt
├── network/NetworkManager.kt
└── display/DisplayManager.kt
```

## 📝 File Naming Conventions

### Managers and Services
- **Pattern**: `[Feature]Manager.kt` or `[Feature]Service.kt`
- **Example**: `WiFiManager.kt`, `AudioService.kt`

### Data Classes
- **Pattern**: `[Feature]Info.kt` or `[Feature]State.kt`
- **Example**: `DeviceInfo.kt`, `NetworkState.kt`

### Utilities
- **Pattern**: `[Feature]Utils.kt` or `[Feature]Helper.kt`
- **Example**: `AudioUtils.kt`, `NetworkHelper.kt`

## 🔄 Migration Checklist

When refactoring existing code:

- [ ] Check package declaration matches file path
- [ ] Verify no redundant "managers" folders
- [ ] Ensure single responsibility per class
- [ ] Group related functionality in type folders
- [ ] Update all imports in dependent files
- [ ] Run tests to verify no breaking changes
- [ ] Update module documentation

## 📊 Type Folder Categories

Standard categories for organizing files:

| Type Folder | Purpose | Examples |
|------------|---------|----------|
| `audio/` | Audio processing, routing | AudioRouting, SpatialAudio |
| `network/` | All network connectivity | WiFi, Bluetooth, NFC, Cellular |
| `sensors/` | Sensor management | IMU, Lidar, Accelerometer |
| `display/` | Display and UI management | Overlay, Screen, Brightness |
| `media/` | Media capture/playback | Video, Camera, Recorder |
| `security/` | Security features | Biometric, Encryption, Auth |
| `storage/` | Data persistence | Cache, Database, Files |
| `ui/` | UI components | ViewModels, Activities, Views |
| `compatibility/` | API compatibility | ApiCompat, VersionHelper |
| `utils/` | General utilities | DateUtils, StringUtils |

## ⚡ Quick Reference

### Creating a New Manager

```kotlin
// File: /DeviceManager/src/main/java/com/augmentalis/devicemanager/network/NfcManager.kt
package com.augmentalis.devicemanager.network  // Matches path

class NfcManager(private val context: Context) {
    // Implementation
}
```

### Adding to Main Facade

```kotlin
// File: /DeviceManager/src/main/java/com/augmentalis/devicemanager/DeviceManager.kt
class DeviceManager(context: Context) {
    // Lazy initialization of managers
    val nfc by lazy { network.NfcManager(context) }
    val wifi by lazy { network.WiFiManager(context) }
    val bluetooth by lazy { network.BluetoothManager(context) }
}
```

## 🔴 Enforcement

1. **Pre-commit Hook**: Validates file structure
2. **CI/CD Check**: Automated structure validation
3. **PR Review**: Manual verification required
4. **Quarterly Audit**: Full codebase structure review

## 📚 Related Documents

- [VOS4 Architecture](Architecture/VOS4-ARCHITECTURE-2025-01-30.md)
- [Module Guidelines](modules/MODULE-GUIDELINES.md)
- [Code Style Guide](STYLE-GUIDE.md)

---
**Remember**: Consistent structure = Maintainable code = Faster development

*Last Updated: January 30, 2025*