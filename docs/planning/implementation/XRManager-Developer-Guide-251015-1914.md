# XRManager Developer Guide

## Overview

The `XRManager` class provides comprehensive Extended Reality (AR/VR/MR) management capabilities for VOS4 applications. It serves as the central interface for detecting XR hardware capabilities, managing XR sessions, and coordinating with various XR SDKs and frameworks.

## Table of Contents

1. [Architecture](#architecture)
2. [Getting Started](#getting-started)
3. [API Reference](#api-reference)
4. [XR Capabilities Detection](#xr-capabilities-detection)
5. [State Management](#state-management)
6. [Best Practices](#best-practices)
7. [Troubleshooting](#troubleshooting)
8. [Migration Guide](#migration-guide)

## Architecture

### Core Components

```
XRManager
├── XRState (StateFlow management)
├── Capability Detection
│   ├── AR Feature Detection
│   ├── VR Feature Detection
│   └── Hardware Assessment
├── Session Management
│   ├── AR Sessions
│   ├── VR Sessions
│   └── Mixed Reality Sessions
└── Integration Points
    ├── ARCore Integration (Future)
    ├── OpenXR Integration (Future)
    └── Device-specific SDKs (Future)
```

### Key Features

- **Hardware Capability Detection**: Automatically detects AR/VR support
- **Reactive State Management**: StateFlow-based reactive programming
- **Cross-platform Design**: Prepared for multiple XR frameworks
- **Resource Management**: Efficient initialization and cleanup
- **Error Handling**: Comprehensive error reporting and recovery

## Getting Started

### Basic Usage

```kotlin
// Initialize XRManager
val context = applicationContext
val xrManager = XRManager(context)

// Initialize the manager (lazy initialization)
xrManager.initialize()

// Check basic XR support
if (xrManager.isXRSupported()) {
    println("XR is supported on this device")
}

// Check specific capabilities
if (xrManager.isARSupported()) {
    println("AR is supported")
}

if (xrManager.isVRSupported()) {
    println("VR is supported")
}
```

### Reactive State Observation

```kotlin
// Observe XR state changes
xrManager.xrState.collect { state ->
    when {
        state.hasError -> {
            println("XR Error: ${state.errorMessage}")
        }
        state.isInitialized -> {
            println("XR initialized successfully")
            println("AR Support: ${state.isARSupported}")
            println("VR Support: ${state.isVRSupported}")
        }
        else -> {
            println("XR initializing...")
        }
    }
}
```

### Advanced Usage

```kotlin
// Get detailed XR capabilities
val capabilities = xrManager.getXRCapabilities()
capabilities?.let { caps ->
    println("Supported features: ${caps.supportedFeatures}")
    println("Has dedicated XR chip: ${caps.hasDedicatedXRChip}")
}

// Start XR session (when implemented)
// xrManager.startARSession { session ->
//     // Handle AR session
// }
```

## API Reference

### Core Methods

#### `initialize()`
Initializes the XR Manager and performs capability detection.

```kotlin
fun initialize()
```

**Behavior:**
- Lazy initialization - only runs once
- Performs hardware capability detection
- Updates XR state with detected capabilities
- Handles initialization errors gracefully

#### `isXRSupported(): Boolean`
Checks if any XR functionality is supported on the device.

```kotlin
fun isXRSupported(): Boolean
```

**Returns:** `true` if AR or VR is supported, `false` otherwise

#### `isARSupported(): Boolean`
Checks if Augmented Reality is supported.

```kotlin
fun isARSupported(): Boolean
```

**Returns:** `true` if AR capabilities are detected, `false` otherwise

#### `isVRSupported(): Boolean`
Checks if Virtual Reality is supported.

```kotlin
fun isVRSupported(): Boolean
```

**Returns:** `true` if VR capabilities are detected, `false` otherwise

### State Management

#### `xrState: StateFlow<XRState>`
Reactive state flow containing current XR status.

```kotlin
data class XRState(
    val isXRSupported: Boolean = false,
    val isARSupported: Boolean = false,
    val isVRSupported: Boolean = false,
    val hasDedicatedXRChip: Boolean = false,
    val supportedFeatures: List<String> = emptyList(),
    val isInitialized: Boolean = false,
    val hasError: Boolean = false,
    val errorMessage: String? = null
)
```

## XR Capabilities Detection

### Hardware Features Detected

The XRManager detects the following hardware features:

| Feature | Android Feature String | Description |
|---------|------------------------|-------------|
| AR Camera | `android.hardware.camera.ar` | AR-capable camera system |
| VR Headtracking | `android.hardware.vr.headtracking` | VR head tracking support |
| VR High Performance | `android.software.vr.mode` | High-performance VR mode |
| Vulkan Graphics | `android.hardware.vulkan.level` | Vulkan API support level |

### Detection Algorithm

```kotlin
private fun detectXRCapabilities(): XRCapabilities {
    val packageManager = context.packageManager
    
    val isARSupported = packageManager.hasSystemFeature(FEATURE_AR)
    val isVRSupported = packageManager.hasSystemFeature(FEATURE_VR_HEADTRACKING) ||
                       packageManager.hasSystemFeature(FEATURE_VR_HIGH_PERFORMANCE)
    
    val supportedFeatures = mutableListOf<String>()
    
    // Check each feature and add to supported list
    if (isARSupported) supportedFeatures.add("AR")
    if (isVRSupported) supportedFeatures.add("VR")
    
    return XRCapabilities(
        isARSupported = isARSupported,
        isVRSupported = isVRSupported,
        hasDedicatedXRChip = detectDedicatedXRChip(),
        supportedFeatures = supportedFeatures
    )
}
```

## State Management

### StateFlow Integration

The XRManager uses StateFlow for reactive state management:

```kotlin
// Subscribe to state changes
lifecycleScope.launch {
    xrManager.xrState
        .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
        .collect { state ->
            updateUI(state)
        }
}
```

### State Transitions

```
Initial State → Initializing → Initialized (Success)
                           → Error State (Failure)
```

## Best Practices

### 1. Lazy Initialization

Always initialize XRManager when needed, not at app startup:

```kotlin
// Good: Lazy initialization
private val xrManager by lazy { XRManager(context) }

// Avoid: Immediate initialization
// val xrManager = XRManager(context).apply { initialize() }
```

### 2. Error Handling

Always handle initialization errors:

```kotlin
xrManager.xrState.collect { state ->
    if (state.hasError) {
        Log.e("XR", "XR initialization failed: ${state.errorMessage}")
        showXRUnavailableUI()
    }
}
```

### 3. Resource Management

XRManager handles its own resources, but ensure proper lifecycle management:

```kotlin
class MyActivity : AppCompatActivity() {
    private lateinit var xrManager: XRManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        xrManager = XRManager(this)
    }
    
    // XRManager automatically cleans up resources
}
```

### 4. Feature Checking

Check for specific XR features before using them:

```kotlin
if (xrManager.isARSupported()) {
    // Safe to use AR features
    startARExperience()
} else {
    // Show fallback UI or disable AR features
    showARUnavailableMessage()
}
```

## Troubleshooting

### Common Issues

#### 1. XR Not Detected on Supported Device

**Problem:** XR capabilities not detected on a device that should support them.

**Solution:**
- Check Android API level (minimum API 24 for AR)
- Verify device has required sensors (accelerometer, gyroscope, magnetometer)
- Check ARCore availability on the device

```kotlin
// Debug XR capabilities
Log.d("XR", "Device model: ${Build.MODEL}")
Log.d("XR", "Android version: ${Build.VERSION.SDK_INT}")
Log.d("XR", "XR State: ${xrManager.xrState.value}")
```

#### 2. Initialization Failures

**Problem:** XRManager initialization fails with errors.

**Solution:**
- Check logcat for detailed error messages
- Verify proper context is provided
- Ensure required permissions are granted

```kotlin
// Check initialization status
if (!xrManager.xrState.value.isInitialized) {
    Log.w("XR", "XR not initialized, calling initialize()")
    xrManager.initialize()
}
```

#### 3. State Not Updating

**Problem:** XR state doesn't update in UI.

**Solution:**
- Ensure StateFlow collection is lifecycle-aware
- Check for proper coroutine scope usage

```kotlin
// Correct StateFlow collection
lifecycleScope.launch {
    xrManager.xrState
        .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
        .collect { state ->
            // Update UI here
        }
}
```

### Debug Logging

Enable verbose logging for troubleshooting:

```kotlin
// Add to your Application class or Activity
if (BuildConfig.DEBUG) {
    Log.d("XRManager", "Debug mode enabled")
}
```

## Migration Guide

### From Legacy XR Code

If migrating from older XR implementations:

#### Before (Legacy)
```kotlin
// Old approach
val hasAR = context.packageManager.hasSystemFeature("android.hardware.camera.ar")
```

#### After (XRManager)
```kotlin
// New approach
val xrManager = XRManager(context)
val hasAR = xrManager.isARSupported()
```

### Integration with DeviceManager

XRManager integrates with the broader DeviceManager system:

```kotlin
// Access via DeviceManager
val deviceManager = DeviceManager(context)
val xrSupported = deviceManager.xr?.isXRSupported() ?: false
```

## Future Roadmap

### Planned Features

1. **ARCore Integration**
   - Direct ARCore session management
   - ARCore availability checking
   - AR anchor management

2. **OpenXR Support**
   - Cross-platform XR runtime
   - Multi-vendor headset support
   - Standardized XR APIs

3. **Device-Specific SDKs**
   - Magic Leap integration
   - HoloLens support
   - Meta Quest integration

4. **Advanced Features**
   - Hand tracking detection
   - Eye tracking support
   - Spatial mapping capabilities

### API Evolution

Future versions will maintain backward compatibility while adding:

```kotlin
// Future API examples
suspend fun startARSession(): ARSession?
suspend fun startVRSession(): VRSession?
fun getHandTrackingSupport(): HandTrackingCapabilities
fun getEyeTrackingSupport(): EyeTrackingCapabilities
```

## Contributing

When contributing to XRManager:

1. Follow VOS4 coding standards
2. Add comprehensive tests for new features
3. Update this documentation for API changes
4. Ensure backward compatibility
5. Add proper error handling

### Testing

```kotlin
@Test
fun testXRCapabilityDetection() {
    val xrManager = XRManager(context)
    xrManager.initialize()
    
    val state = xrManager.xrState.value
    assertTrue(state.isInitialized)
}
```

---

*Last Updated: 2025-09-06*  
*Version: 1.4.0*  
*Author: VOS4 Development Team*