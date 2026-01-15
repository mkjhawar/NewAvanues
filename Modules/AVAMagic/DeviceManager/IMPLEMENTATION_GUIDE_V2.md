# DeviceManager Library - Comprehensive Implementation Guide v2.0

## Version 2.0 - Complete Device Detection Architecture
**Date**: January 2025  
**Author**: Manoj Jhawar  
**Android Support**: API 28+ (Android 9+) with Android XR/AR Ready  
**Enterprise Support**: RealWear, Vuzix, Rokid, XREAL, Virture

## 🚀 What's New in v2.0

### Major Enhancements
- **Intelligent Caching**: 80% faster initialization with 7-day cache
- **Enterprise AR Support**: Complete detection for 15+ AR glass models
- **Foldable State Tracking**: Real-time fold angle and posture detection
- **200+ Properties**: Most comprehensive device detection available
- **USB Monitoring**: Real-time USB-C and peripheral detection
- **Voice-First Design**: Complete input method detection for VoiceUI

## 📋 Table of Contents
1. [Quick Start](#quick-start)
2. [Core Features](#core-features)
3. [Enterprise AR Detection](#enterprise-ar-detection)
4. [Foldable & Multi-Display](#foldable--multi-display)
5. [Performance & Caching](#performance--caching)
6. [Advanced Integration](#advanced-integration)
7. [Voice UI Adaptation](#voice-ui-adaptation)
8. [API Reference](#api-reference)

## 🚀 Quick Start

### Installation

```gradle
dependencies {
    implementation("com.augmentalis:devicemanager:2.0.0")
}
```

### Basic Setup

```kotlin
class MyApplication : Application() {
    lateinit var deviceManager: DeviceManager
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize with caching and monitoring
        deviceManager = DeviceManager(this).apply {
            initialize() // Async with intelligent caching
            
            // Enable auto-rescan on USB connection
            info.setAutoRescan(true)
        }
    }
}
```

### Simple Usage

```kotlin
// Get device information
val profile = deviceManager.info.getDeviceProfile()
val isTablet = deviceManager.info.isTablet()
val isFoldable = deviceManager.info.isFoldable()

// Check for enterprise AR device
when {
    deviceManager.isRealWearDevice() -> {
        // Voice-only interface
        disableAllTouchControls()
    }
    deviceManager.isVuzixDevice() -> {
        // Touchpad navigation
        enableTouchpadMode()
    }
}
```

## 🎯 Core Features

### Complete Device Profiling

```kotlin
// Extended device profile with 50+ properties
val extendedProfile = deviceManager.info.getExtendedDeviceProfile()

// Access detailed information
println("Manufacturer: ${extendedProfile.manufacturer}")
println("Kernel: ${extendedProfile.kernelVersion}")
println("Security Patch: ${extendedProfile.securityPatchLevel}")
println("Root Status: ${extendedProfile.rootStatus}")
println("Knox Version: ${extendedProfile.knoxVersion}") // Samsung only
```

### Display Detection & Metrics

```kotlin
// Comprehensive display information
val display = deviceManager.info.getExtendedDisplayProfile()

// Physical characteristics
println("Panel Type: ${display.panelType}") // OLED, AMOLED, LCD, E_INK
println("Max Brightness: ${display.maxBrightness} nits")
println("HDR Support: ${display.hdrCapabilities}")
println("Refresh Rate: ${display.variableRefreshRate}")

// Features
val hasNotch = display.hasNotch
val hasPunchHole = display.hasPunchHole
val hasUnderDisplayCamera = display.hasUnderDisplayCamera
val cutoutBounds = display.cutoutDetails?.bounds
```

### Sensor Details

```kotlin
// Get detailed sensor information
val sensors = deviceManager.info.getExtendedSensorList()

sensors.forEach { sensor ->
    println("Sensor: ${sensor.name}")
    println("  Vendor: ${sensor.vendor}")
    println("  Power: ${sensor.power} mA")
    println("  Max Range: ${sensor.maxRange}")
    println("  Resolution: ${sensor.resolution}")
    println("  Wake-up: ${sensor.isWakeUp}")
}
```

## 🥽 Enterprise AR Detection

### RealWear Devices (HMT-1, Navigator Series)

```kotlin
// Detect RealWear device
if (deviceManager.isRealWearDevice()) {
    val features = deviceManager.getRealWearFeatures()
    
    // Voice-only control (no touch interface)
    if (features.hasNoTouchInterface) {
        enableVoiceOnlyMode()
        
        // Configure voice commands
        val commands = features.voiceCommands
        registerVoiceCommands(commands)
        
        // Head gesture support
        if (features.hasHeadGestures) {
            enableHeadNodDetection()
        }
    }
    
    // Industrial features
    println("Noise Reduction: ${features.noiseReductionDB} dB")
    println("IP Rating: ${features.ipRating}")
    println("Drop Test: ${features.dropTestHeight} meters")
}
```

### Vuzix Devices (M400, M4000, Blade)

```kotlin
// Detect Vuzix device
if (deviceManager.isVuzixDevice()) {
    val features = deviceManager.getVuzixFeatures()
    
    // Critical touchpad configuration
    if (features.hasTouchpad) {
        configureTouchpad(
            buttons = features.touchpadButtons, // 3 buttons
            gestures = features.touchpadGestures // tap, hold, swipe
        )
    }
    
    // Display differences
    println("Display Type: ${features.displayType}") // OLED vs LCD
    println("Resolution: ${features.displayResolution}")
    println("FOV: ${features.fieldOfView}°")
    
    // Special features
    if (features.hasBarcodeScanEngine) {
        enableBarcodeScanning()
    }
}
```

### Rokid Glass

```kotlin
// Rokid detection
if (deviceManager.isRokidDevice()) {
    val features = deviceManager.getRokidFeatures()
    
    // Display configuration
    if (features.hasBinocularDisplay) {
        configureStereoscopicDisplay()
    }
    
    // Interaction methods
    if (features.hasTouchpad) {
        enableTempleTouchpad()
    }
    
    if (features.hasRokidRing) {
        enableRingController()
    }
    
    // Advanced features
    if (features.hasSLAM) {
        enable6DOFTracking()
    }
}
```

### XREAL (Nreal) Air Series

```kotlin
// XREAL detection
if (deviceManager.isXRealDevice()) {
    val features = deviceManager.getXRealFeatures()
    
    // Display capabilities
    println("Refresh Rate: ${features.refreshRate} Hz")
    
    if (features.hasElectrochromicDimming) {
        enableAutoDimming()
    }
    
    // Tracking capabilities
    when {
        features.has6DOF -> enable6DOFMode()
        features.has3DOF -> enable3DOFMode()
    }
    
    // Accessories
    if (features.hasBeamAccessory) {
        configureBeamCompute()
    }
}
```

## 📱 Foldable & Multi-Display

### Foldable State Tracking

```kotlin
// Monitor fold state changes
deviceManager.registerFoldStateListener { state ->
    when (state.foldState) {
        FoldState.CLOSED -> {
            // Use outer display
            switchToOuterDisplay()
        }
        FoldState.HALF_OPEN -> {
            // Flex mode (laptop-like)
            if (state.hingeAngle in 75f..115f) {
                enableFlexMode()
                // Bottom half: controls
                // Top half: content
            }
        }
        FoldState.OPEN -> {
            // Full tablet mode
            useFullScreen()
        }
        FoldState.TENT -> {
            // Standing mode
            enableTentMode()
        }
    }
    
    // Avoid the crease
    state.creaseInfo?.let { crease ->
        avoidArea(crease.bounds)
    }
}
```

### Multi-Display Management

```kotlin
// Handle multiple displays
val displayState = deviceManager.getMultiDisplayState()

when (displayState.displayArrangement) {
    DisplayArrangement.SINGLE -> {
        // Normal single screen
    }
    DisplayArrangement.EXTENDED -> {
        // Extended desktop
        val primary = displayState.primaryDisplayId
        val secondary = displayState.presentationDisplayId
        showContentOnDisplay(secondary)
    }
    DisplayArrangement.SPANNING -> {
        // App spans both screens (Surface Duo)
        val gap = displayState.hingeGap
        avoidHingeArea(gap)
    }
}
```

### Orientation & Rotation

```kotlin
// Advanced orientation handling
deviceManager.registerOrientationListener { state ->
    // Current orientation
    when (state.currentOrientation) {
        Orientation.PORTRAIT -> adjustForPortrait()
        Orientation.LANDSCAPE -> adjustForLandscape()
        Orientation.REVERSE_PORTRAIT -> handle180Rotation()
    }
    
    // Sensor data
    if (state.isTabletopStable) {
        // Device lying flat
        enableTableTopMode()
    }
    
    // Special cases
    if (state.isLockedToLandscape) {
        // Some XR devices are landscape-only
        disableRotation()
    }
}
```

## ⚡ Performance & Caching

### Intelligent Caching System

```kotlin
// Configure caching
val cache = deviceManager.info.cache

// Check cache status
if (cache.isCacheValid()) {
    println("Using cached data")
    println("Last scan: ${cache.getLastScanTime()}")
}

// Force rescan if needed
deviceManager.info.forceRescan()

// Configure auto-rescan
cache.setAutoRescanOnUSB(true) // Rescan when USB devices connect
```

### Performance Metrics

```kotlin
// Monitor performance
val perfState = deviceManager.getPerformanceState()

// Thermal throttling
if (perfState.thermalStatus == ThermalStatus.SEVERE) {
    // Reduce UI complexity
    reduceAnimations()
    lowerRefreshRate()
}

// Battery optimization
if (perfState.isPowerSaveMode) {
    disableNonEssentialFeatures()
}

// Adaptive refresh rate
when (perfState.refreshRateMode) {
    RefreshRateMode.ADAPTIVE -> { /* Let system decide */ }
    RefreshRateMode.HIGH_PERFORMANCE -> setRefreshRate(120)
    RefreshRateMode.POWER_SAVE -> setRefreshRate(60)
}
```

## 🔧 Advanced Integration

### Manufacturer-Specific Features

```kotlin
// Samsung-specific
if (deviceManager.isSamsungDevice()) {
    val features = deviceManager.getSamsungFeatures()
    
    if (features.hasDeX) {
        // Desktop mode active
        adaptForDesktopMode()
    }
    
    if (features.hasSPen) {
        val pressureLevels = features.sPenPressureLevels
        enablePressureSensitiveDrawing(pressureLevels)
    }
    
    if (features.hasPowerShare) {
        // Can charge other devices
        showPowerShareOption()
    }
}

// OnePlus-specific
if (deviceManager.isOnePlusDevice()) {
    val features = deviceManager.getOnePlusFeatures()
    
    if (features.hasAlertSlider) {
        monitorAlertSliderPosition()
    }
    
    if (features.hasWarpCharge) {
        showFastChargingStatus()
    }
}
```

### Dynamic UI Adaptation

```kotlin
// Calculate safe UI areas
val uiState = deviceManager.getUIAdaptationState()

// Avoid system UI
val safeArea = Rect(
    left = uiState.safeInsets.left,
    top = uiState.safeInsets.top + uiState.statusBarHeight,
    right = screenWidth - uiState.safeInsets.right,
    bottom = screenHeight - uiState.navigationBarHeight
)

// Handle cutouts
uiState.cutoutBounds.forEach { cutout ->
    avoidArea(cutout)
}

// Keyboard handling
if (uiState.isKeyboardVisible) {
    adjustForKeyboard(uiState.keyboardHeight)
    
    if (uiState.isFloatingKeyboard) {
        // Keyboard can be moved
        val keyboardBounds = uiState.keyboardPosition
        avoidArea(keyboardBounds)
    }
}

// Multi-window
when (uiState.windowingMode) {
    WindowingMode.SPLIT_SCREEN -> {
        // App is in split screen
        val bounds = uiState.windowBounds
        adaptToSmallerSpace(bounds)
    }
    WindowingMode.PICTURE_IN_PICTURE -> {
        // Minimal UI for PIP
        showMinimalControls()
    }
    WindowingMode.FREEFORM -> {
        // Desktop-like windowing
        enableResizableWindow()
    }
}
```

## 🎤 Voice UI Adaptation

### Input Method Detection

```kotlin
// Comprehensive input detection for Voice UI
val inputMethods = deviceManager.getInputMethodDetection()

// Voice capabilities
if (inputMethods.hasVoiceControl) {
    when (inputMethods.voiceActivationType) {
        VoiceActivationType.ALWAYS_ON -> {
            // Always listening (privacy consideration)
            setupAlwaysOnVoice()
        }
        VoiceActivationType.WAKE_WORD -> {
            // "Hey Device" style
            val wakeWords = inputMethods.wakeWords
            registerWakeWords(wakeWords)
        }
        VoiceActivationType.PUSH_TO_TALK -> {
            // Button activated
            mapPushToTalkButton()
        }
    }
}

// Physical controls
inputMethods.physicalButtons.forEach { button ->
    println("Button: ${button.label} at ${button.position}")
    if (button.isProgrammable) {
        programButton(button.id, customAction)
    }
}

// Touch surfaces
inputMethods.touchSurfaces.forEach { surface ->
    when (surface.type) {
        TouchType.TRACKPAD -> configureTrackpad(surface)
        TouchType.TOUCHBAR -> configureTouchbar(surface)
    }
}

// Accessibility requirements
if (inputMethods.requiresVoiceOnly) {
    // RealWear style - no touch at all
    disableAllVisualControls()
    enableCompleteVoiceControl()
}
```

### Context-Aware Voice Prompts

```kotlin
// Adapt voice prompts based on device
fun getVoicePrompt(action: String): String {
    return when {
        deviceManager.isRealWearDevice() -> {
            // RealWear uses specific commands
            "Say 'SELECT ITEM ${action.toUpperCase()}'"
        }
        deviceManager.isVuzixDevice() -> {
            // Vuzix has touchpad
            "Say '$action' or tap the touchpad"
        }
        deviceManager.isFoldable() -> {
            val state = deviceManager.getFoldState()
            if (state == FoldState.CLOSED) {
                "Device is closed. Say '$action' to continue"
            } else {
                "Say '$action' or tap the screen"
            }
        }
        else -> {
            "Say '$action'"
        }
    }
}
```

## 📚 API Reference

### Core Detection APIs

```kotlin
// Device type detection
deviceManager.info.isTablet(): Boolean
deviceManager.info.isFoldable(): Boolean
deviceManager.info.isWearable(): Boolean
deviceManager.info.isXR(): Boolean
deviceManager.info.isTV(): Boolean
deviceManager.info.isAutomotive(): Boolean

// Enterprise AR detection
deviceManager.isRealWearDevice(): Boolean
deviceManager.isVuzixDevice(): Boolean
deviceManager.isRokidDevice(): Boolean
deviceManager.isXRealDevice(): Boolean
deviceManager.isVirtureDevice(): Boolean

// Manufacturer detection
deviceManager.isSamsungDevice(): Boolean
deviceManager.isOnePlusDevice(): Boolean
deviceManager.isXiaomiDevice(): Boolean
deviceManager.isOppoDevice(): Boolean
deviceManager.isGooglePixel(): Boolean

// State monitoring
deviceManager.getFoldState(): FoldableState
deviceManager.getOrientationState(): OrientationState
deviceManager.getMultiDisplayState(): MultiDisplayState
deviceManager.getPerformanceState(): PerformanceState
deviceManager.getUIAdaptationState(): UIAdaptationState

// Feature detection
deviceManager.info.has6DOFTracking(): Boolean
deviceManager.info.hasWirelessDisplaySupport(): Boolean
deviceManager.info.isDeXMode(): Boolean
deviceManager.info.isDesktopMode(): Boolean
```

### Event Listeners

```kotlin
// Register for state changes
deviceManager.registerFoldStateListener { state -> }
deviceManager.registerOrientationListener { orientation -> }
deviceManager.registerDisplayListener { displays -> }
deviceManager.registerUSBListener { devices -> }
deviceManager.registerThermalListener { status -> }
deviceManager.registerConfigurationChangeListener { config -> }

// Unregister when done
deviceManager.unregisterAllListeners()
```

### Performance APIs

```kotlin
// Cache management
deviceManager.info.forceRescan()
deviceManager.info.setAutoRescan(enabled: Boolean)
deviceManager.info.getLastScanTime(): Long
deviceManager.info.isCacheValid(): Boolean

// Performance monitoring
deviceManager.getMemoryUsage(): MemoryInfo
deviceManager.getCPUUsage(): CPUInfo
deviceManager.getThermalStatus(): ThermalStatus
deviceManager.getBatteryInfo(): BatteryInfo
```

## 🔒 Permissions

The library requires minimal permissions:

```xml
<!-- Basic detection (no permissions needed) -->

<!-- Optional for enhanced features -->
<uses-permission android:name="android.permission.CAMERA" /> <!-- Camera details -->
<uses-permission android:name="android.permission.BLUETOOTH" /> <!-- Bluetooth detection -->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" /> <!-- WiFi info -->

<!-- For USB devices -->
<uses-feature android:name="android.hardware.usb.host" />
```

## 📊 Performance Metrics

- **Initialization**: ~50ms with cache, ~500ms without
- **Memory Usage**: <5MB total
- **CPU Usage**: <0.1% idle, <2% active scanning
- **Battery Impact**: Negligible with caching
- **Cache Size**: ~2MB maximum

## 🧪 Testing

```kotlin
// Unit test example
@Test
fun testDeviceDetection() {
    val mockContext = mockk<Context>()
    val deviceManager = DeviceManager(mockContext)
    
    // Test device type detection
    every { mockContext.resources.configuration } returns mockTabletConfig
    assertTrue(deviceManager.info.isTablet())
    
    // Test manufacturer detection
    every { Build.MANUFACTURER } returns "samsung"
    assertTrue(deviceManager.isSamsungDevice())
}

// Integration test
@Test
fun testRealWearIntegration() {
    val device = RealWearHMT1()
    val features = deviceManager.getRealWearFeatures()
    
    assertFalse(features.hasTouchInterface)
    assertTrue(features.hasVoiceControl)
    assertEquals(4, features.microphoneCount)
}
```

## 🚨 Troubleshooting

### Common Issues

1. **Cache not updating**: Call `forceRescan()` to refresh
2. **USB devices not detected**: Ensure USB host permission granted
3. **Fold state not working**: Check manufacturer-specific APIs
4. **Performance issues**: Enable caching, reduce scan frequency

### Debug Mode

```kotlin
// Enable debug logging
DeviceManager.enableDebugMode(true)

// Get diagnostic info
val diagnostics = deviceManager.getDiagnostics()
println(diagnostics.fullReport)
```

## 📝 License

This library is part of the Augmentalis VOS4 project.
Copyright © 2025 Augmentalis. All rights reserved.

## 🤝 Support

For enterprise support and custom implementations:
- Email: support@augmentalis.com
- Documentation: https://docs.augmentalis.com/devicemanager
- Issues: https://github.com/augmentalis/devicemanager/issues
