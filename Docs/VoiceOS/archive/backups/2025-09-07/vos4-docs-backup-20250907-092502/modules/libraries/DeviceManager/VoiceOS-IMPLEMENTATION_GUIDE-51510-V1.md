# DeviceManager Library - Complete Implementation Guide

## Version 2.1 - Consolidated Architecture
**Date**: December 2024  
**Author**: Manoj Jhawar  
**Android Support**: API 28 (Android 9) - API 34+ (Android 14+) & Android XR Ready

### Recent Consolidation (December 2024)
- ✅ **Merged all Extended classes** into their main counterparts
- ✅ **Removed duplicate classes** (DeviceInfoExtended, XRManagerExtended, core/DeviceInfo)
- ✅ **Single source of truth** for each component
- ✅ **Clean imports** throughout the codebase

## 📋 Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [Project Structure](#project-structure)
3. [Component Documentation](#component-documentation)
4. [API Usage Guide](#api-usage-guide)
5. [Android Version Compatibility](#android-version-compatibility)
6. [Permission Requirements](#permission-requirements)
7. [Best Practices](#best-practices)
8. [Migration Guide](#migration-guide)

## 🏗️ Architecture Overview

The DeviceManager library follows a **facade pattern** with modular managers organized by functionality. This provides:

- **Single point of access** via `DeviceManager` facade
- **Lazy initialization** for performance
- **Clean separation** of concerns
- **Voice-first design** throughout
- **SDK-ready** with public APIs

### Design Principles
- **SOLID Principles**: Single Responsibility, Open/Closed adherence
- **Clean Architecture**: Clear separation between layers
- **Reactive Programming**: Kotlin Flow for state management
- **Null Safety**: Comprehensive null checks
- **API Compatibility**: Proper version checks for all Android APIs

## 📁 Project Structure

```
libraries/DeviceManager/
├── build.gradle.kts                 # Build configuration
├── IMPLEMENTATION_GUIDE.md          # This document
├── REVIEW_FINDINGS.md              # Code review results
├── CHANGELOG.md                     # Version history
└── src/main/
    ├── java/com/augmentalis/devicemanager/
    │   ├── DeviceManager.kt        # Main facade/coordinator
    │   ├── DeviceInfo.kt           # CONSOLIDATED device information (includes Extended)
    │   ├── NetworkManager.kt       # Comprehensive connectivity
    │   ├── XRManager.kt            # CONSOLIDATED XR/AR (includes Extended)
    ├── managers/                   # Organized by function
    │   ├── display/
    │   │   └── DisplayOverlayManager.kt
    │   ├── network/
    │   │   ├── NetworkManager.kt       # To be split
    │   │   ├── BluetoothManager.kt     # TODO: Extract
    │   │   ├── WiFiManager.kt          # TODO: Extract
    │   │   ├── CellularManager.kt      # TODO: Extract
    │   │   └── NFCManager.kt           # TODO: Extract
    │   ├── media/
    │   │   ├── AudioManager.kt         # TODO: Consolidate
    │   │   ├── VideoManager.kt
    │   │   └── audioservices/          # Legacy location
    │   └── xr/
    │       ├── XRManager.kt            # TODO: Consolidate
    │       └── GlassesManager.kt
    └── models/                     # Shared data classes
        └── (various data classes)
```

## 📚 Component Documentation

### 1. DeviceManager (Facade)
**Location**: `DeviceManager.kt`  
**Purpose**: Central coordinator providing unified access to all device functionality

```kotlin
val deviceManager = DeviceManager.getInstance(context)

// Access sub-managers
deviceManager.info           // Device information
deviceManager.display        // Display & overlays
deviceManager.network        // All connectivity
deviceManager.video          // Camera management
deviceManager.audio          // Audio control
deviceManager.xr            // XR/AR features
deviceManager.glasses       // Smart glasses
```

### 2. Core Components

#### DeviceInfo
**Location**: `core/DeviceInfo.kt`  
**Purpose**: Comprehensive hardware detection and device profiling

**Key Features**:
- Device type detection (tablet, foldable, XR, TV, automotive)
- Hardware capabilities (sensors, 6DOF, cameras)
- USB-C/DisplayPort device detection
- External display enumeration
- Input device detection
- Samsung DeX and desktop mode detection

**API Example**:
```kotlin
val info = deviceManager.info
val profile = info.getDeviceProfile()
val cameras = info.getCameraInfo()
val usbDevices = info.getUSBDevices()
val has6DOF = info.has6DOFTracking()
val isDeX = info.isDeXMode()
```

### 3. Display Management

#### DisplayOverlayManager
**Location**: `managers/display/DisplayOverlayManager.kt`  
**Purpose**: System overlays and external display management

**Key Features**:
- System overlay creation (floating windows)
- External display detection and management
- Samsung DeX and Android Desktop mode support
- Smart glasses display configuration
- Multi-display UI scaling

**API Example**:
```kotlin
val display = deviceManager.display

// Create overlay
display.createOverlay(
    id = "voice_assistant",
    view = customView,
    position = OverlayPosition.TOP_RIGHT
)

// Check external displays
if (display.hasExternalDisplay()) {
    display.extendToExternalDisplay(displayId)
}

// Configure for smart glasses
display.configureForSmartGlasses(SmartGlassesType.WIRELESS)
```

### 4. Network Management

#### NetworkManager
**Location**: `managers/network/NetworkManager.kt`  
**Purpose**: Comprehensive connectivity management

**Supported Protocols**:
- Bluetooth Classic & LE
- WiFi (including Direct, Aware, RTT)
- Cellular (2G-5G)
- USB-C networking
- NFC
- VPN detection

**API Example**:
```kotlin
val network = deviceManager.network

// Bluetooth
network.startBluetoothDiscovery()
network.startBleScan()

// WiFi
network.scanWifiNetworks()
network.connectToWifi("NetworkName", "password")

// Check connectivity
val hasInternet = network.hasInternetConnection()
val quality = network.getNetworkQuality()
```

### 5. Media Management

#### VideoManager
**Location**: `managers/media/VideoManager.kt`  
**Purpose**: Camera and video recording management

**Key Features**:
- Multi-camera support (front, back, external)
- Video recording with quality profiles
- Photo capture
- Camera controls (zoom, flash, focus)
- Voice-first configuration

**API Example**:
```kotlin
val video = deviceManager.video

// Open camera
video.openCamera(cameraId, surfaceView)

// Start recording
video.startRecording(outputFile, VideoProfile.HD_1080P)

// Take photo
video.takePhoto { byteArray ->
    // Handle photo data
}
```

#### AudioManager
**Location**: `managers/media/AudioManager.kt` (TODO: Consolidate)  
**Purpose**: Audio device and effects management

**Key Features**:
- Spatial audio (3D audio)
- Echo cancellation
- Noise suppression
- Audio routing
- Equalizer and effects

### 6. XR Management

#### XRManager
**Location**: `managers/xr/XRManager.kt`  
**Purpose**: Extended reality features

**Key Features**:
- Spatial anchors
- 6DOF tracking
- Plane detection
- Mesh generation
- Occlusion handling
- Cloud anchors
- Hand/eye tracking

#### GlassesManager
**Location**: `managers/xr/GlassesManager.kt`  
**Purpose**: Smart glasses specific features

## 📱 Android Version Compatibility

### API Level Support Matrix

| Feature | Min API | Fallback Strategy |
|---------|---------|-------------------|
| Basic Device Info | 28 | ✅ Always available |
| Display Overlays | 26 | TYPE_SYSTEM_ALERT for older |
| Bluetooth LE | 21 | Check availability |
| WiFi Direct | 14 | Check availability |
| WiFi Aware | 26 | Not available |
| WiFi RTT | 28 | Not available |
| Spatial Audio | 32 | Virtualizer fallback |
| Camera2 API | 21 | ✅ Always available |
| 6DOF Tracking | Varies | Multiple detection methods |
| Samsung DeX | Varies | Reflection-based detection |

### Version-Specific Code Examples

```kotlin
// Display API changes
val refreshRate = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
        context.display?.refreshRate ?: 60f
    }
    else -> {
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.refreshRate
    }
}

// Spatial audio with fallback
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
    // Use native spatializer (API 32+)
    audioManager.spatializer?.let { /* ... */ }
} else {
    // Fall back to virtualizer
    useVirtualizer()
}
```

## 🔐 Permission Requirements

### Required Permissions by Component

#### Core Permissions
```xml
<!-- Always needed -->
<uses-permission android:name="android.permission.INTERNET" />
```

#### Display Permissions
```xml
<!-- For overlays -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

#### Camera/Video Permissions
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
    android:maxSdkVersion="28" />
```

#### Network Permissions
```xml
<!-- Bluetooth (Android 12+) -->
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />

<!-- WiFi -->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />

<!-- NFC -->
<uses-permission android:name="android.permission.NFC" />
```

### Runtime Permission Handling

```kotlin
// Check and request permissions
private fun checkCameraPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (checkSelfPermission(Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
        }
    }
}
```

## 💡 Best Practices

### 1. Initialization
```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize once at app startup
        val deviceManager = DeviceManager.getInstance(this)
        deviceManager.initialize()
    }
}
```

### 2. Resource Management
```kotlin
override fun onDestroy() {
    super.onDestroy()
    // Always release resources
    deviceManager.shutdown()
}
```

### 3. Voice-First Configuration
```kotlin
// Configure all components for voice interaction
deviceManager.audio.configureForVoiceFirst(audioSessionId)
deviceManager.video.configureForVoiceFirst()
```

### 4. Error Handling
```kotlin
try {
    deviceManager.network.startBluetoothDiscovery()
} catch (e: SecurityException) {
    // Handle missing permissions
    requestBluetoothPermissions()
}
```

### 5. Reactive State Management
```kotlin
// Observe state changes with Flow
lifecycleScope.launch {
    deviceManager.network.networkState.collect { state ->
        updateUI(state)
    }
}
```

## 🔄 Migration Guide

### From Version 1.0 to 2.0

#### 1. Update Imports
```kotlin
// Old
import com.augmentalis.devicemanager.VosDisplayManager
import com.augmentalis.devicemanager.DeviceInfoExtended

// New  
import com.augmentalis.devicemanager.managers.display.DisplayOverlayManager
import com.augmentalis.devicemanager.core.DeviceInfo
```

#### 2. Update Class References
```kotlin
// Old
val display: VosDisplayManager
val infoExtended: DeviceInfoExtended

// New
val display: DisplayOverlayManager
val info: DeviceInfo  // Now includes extended functionality
```

#### 3. Package Structure Changes
- All managers moved to `managers/` subfolder
- Audio services consolidated
- XR features consolidated

## 🧪 Testing Checklist

- [ ] Test on Android 9 (API 28)
- [ ] Test on Android 10 (API 29) - WiFi permission changes
- [ ] Test on Android 11 (API 30) - Display API changes
- [ ] Test on Android 12 (API 31) - Bluetooth permissions
- [ ] Test on Android 12L (API 32) - Spatial audio
- [ ] Test on Android 13 (API 33) - Notification permissions
- [ ] Test on Android 14 (API 34) - Latest stable
- [ ] Test Samsung DeX mode
- [ ] Test with external displays
- [ ] Test permission denials
- [ ] Test resource cleanup

## 📈 Performance Considerations

1. **Lazy Initialization**: All managers use lazy loading
2. **Coroutines**: Async operations don't block UI
3. **Flow**: Efficient state updates with StateFlow
4. **Resource Management**: Proper cleanup prevents leaks

## 🚀 Future Enhancements

### Planned for Version 2.1
- [ ] Split NetworkManager into focused components
- [ ] Add PermissionManager for centralized handling
- [ ] Add unit tests for all components
- [ ] Add performance monitoring
- [ ] Add crash reporting integration

### Android XR Support (Future)
- [ ] Spatial computing APIs
- [ ] Hand tracking improvements
- [ ] Eye tracking integration
- [ ] Advanced occlusion handling

## 📞 Support

For issues or questions:
- Review `REVIEW_FINDINGS.md` for known issues
- Check component-specific documentation
- Follow Android version compatibility matrix

## 📄 License

Copyright © 2024 Augmentalis  
Internal use only - Voice OS 4 (VOS4)

---

*This implementation guide represents the current state of the DeviceManager library after comprehensive review and reorganization using CoT, RoT, and ToT analysis methodologies.*
