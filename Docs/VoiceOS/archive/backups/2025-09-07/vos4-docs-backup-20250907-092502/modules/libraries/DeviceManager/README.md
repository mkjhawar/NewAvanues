# DeviceManager Module

## Overview
DeviceManager is a comprehensive device management library for VOS4 that provides unified access to all device capabilities including sensors, network interfaces, display management, and hardware detection.

## Author
Manoj Jhawar

## Module Structure (Updated January 30, 2025)

### Directory Organization
```
/libraries/DeviceManager/src/main/java/com/augmentalis/devicemanager/
├── accessibility/         # TTS and Feedback management (split from AccessibilityManager)
├── audio/                # Audio capture, routing, spatial audio
├── compatibility/        # Android API compatibility layer
├── dashboardui/          # Dashboard UI components
├── deviceinfo/           # Device information and detection
│   ├── cache/           # Device info caching
│   ├── certification/   # Certification detection
│   ├── detection/       # Device and smartglass detection
│   └── manufacturer/    # Manufacturer-specific features
├── display/              # Display and overlay management
├── network/              # Network interface managers
├── security/             # Biometric and security features
├── sensors/              # Sensor management
│   ├── imu/             # IMU and motion tracking
│   └── LidarManager.kt  # LiDAR/ToF sensors
├── smartdevices/         # Foldable device support
├── smartglasses/         # AR glasses support
├── usb/                  # USB device monitoring
└── video/                # Camera and video management
```

## Architectural Design

### Core Principle: Separation of Concerns
```
DeviceDetector (Detection) → DeviceCapabilities → Managers (Implementation)
```

### ⚠️ Important: Monolithic DeviceDetector (Justified Exception)

DeviceDetector is intentionally a single 903-line file handling ALL detection. This violates SRP but is **architecturally justified**:

1. **Performance**: Single-pass detection is 75% faster than distributed
2. **Interdependencies**: Many capabilities depend on each other  
3. **Android API Reality**: Acts as Anti-Corruption Layer for messy Android APIs
4. **Atomic Caching**: Single cache point ensures consistency
5. **Maintenance**: One place for all Android version compatibility

See [DeviceDetector-SRP-Exception.md](../../docs/architecture/DeviceDetector-SRP-Exception.md) for detailed justification.

## Features

### 🔍 Core Detection Capabilities (Centralized in DeviceDetector)
- **Extended Device Detection**: Complete device profiling including hardware, sensors, display properties
- **AR/XR Glasses Detection**: Support for RealWear, Vuzix, Rokid, XREAL, and other enterprise AR devices
- **Foldable Device Management**: Real-time hinge angle tracking, posture detection, and crease management
- **Manufacturer Detection**: Identify manufacturer-specific features, SDKs, and customizations
- **Certification Detection**: Enterprise, security, and rugged device certifications

### ⚡ Performance Optimization (After Refactoring)
- **Single-Pass Detection**: All capabilities detected once at startup (500ms)
- **Centralized Caching**: One cache for all capabilities (5-10MB savings)
- **Conditional Manager Loading**: Managers only load when hardware is detected
- **Zero Detection Duplication**: Managers receive capabilities, never detect
- **75% Faster Startup**: Compared to distributed detection approach

## Components

### 1. Core Managers
**Package**: `com.augmentalis.devicemanager.detection`

Comprehensive device property detection including:
- AR glasses models and specifications
- Display panel types and touch technologies
- Extended sensor information
- Root detection with multiple heuristics
- Display metrics and cutout detection

#### Key Classes:
- `ExtendedDeviceDetection`: Main detection class
- `DeviceInfo`: Complete device information data class
- `ARGlassesInfo`: AR device specifications
- `DisplayInfo`: Display properties and metrics
- `SensorInfo`: Extended sensor capabilities

#### Usage:
```kotlin
val detector = ExtendedDeviceDetection(context)
val deviceInfo = detector.detectDevice()

// Check if AR glasses
if (deviceInfo.arGlassesInfo != null) {
    val arInfo = deviceInfo.arGlassesInfo
    println("AR Device: ${arInfo.manufacturer} ${arInfo.model}")
    println("Features: Voice=${arInfo.voiceControl}, 6DOF=${arInfo.sixDOF}")
}
```

### 2. FoldableDeviceManager
**Package**: `com.augmentalis.devicemanager.foldable`

Real-time foldable device state management:
- Hinge angle monitoring via sensors
- Device posture detection (Flat, Tent, Tabletop, Book)
- Crease position and avoidance zones
- Multi-display state tracking
- Orientation and rotation management

#### Key Classes:
- `FoldableDeviceManager`: Main foldable management class
- `FoldableState`: Current fold state and posture
- `OrientationState`: Device orientation information
- `MultiDisplayState`: Multi-screen configuration

#### Supported Devices:
- Samsung Galaxy Fold/Flip series
- Google Pixel Fold
- Microsoft Surface Duo
- OPPO Find N series
- Xiaomi Mix Fold
- Motorola Razr
- And more...

#### Conditional Loading:
```kotlin
// DeviceManager automatically detects capabilities
val deviceManager = DeviceManager.getInstance(context)

// Managers are nullable - only loaded if hardware exists
deviceManager.nfc?.let { nfcManager ->
    // NFC is available, use it
    nfcManager.enableReaderMode()
}

// Check capabilities directly
val capabilities = CapabilityDetector.getCapabilities(context)
if (capabilities.hasUwb) {
    deviceManager.uwb?.startRanging()
}
```

#### Usage:
```kotlin
val foldableManager = FoldableDeviceManager(context)
foldableManager.initialize()

// Observe fold state
foldableManager.foldState.collect { state ->
    when (state.posture) {
        DevicePosture.TABLETOP -> adaptForTabletopMode()
        DevicePosture.TENT -> adaptForTentMode()
        DevicePosture.FLAT -> adaptForFlatMode()
    }
}

// Check crease info
val creaseInfo = foldableManager.getCreaseInfo()
if (creaseInfo?.shouldAvoid == true) {
    // Adjust UI to avoid crease area
    avoidRect(creaseInfo.bounds)
}
```

### 3. ManufacturerDetection
**Package**: `com.augmentalis.devicemanager.manufacturer`

Detect manufacturer-specific features and SDKs:
- Samsung: S Pen, DeX, Knox, Bixby, Edge Panel
- Google: Pixel exclusive features, Tensor chip
- Xiaomi: MIUI features and optimizations
- OnePlus/OPPO: ColorOS, Warp Charge, Alert Slider
- And 30+ other manufacturers

#### Key Classes:
- `ManufacturerDetection`: Main detection class
- `ManufacturerInfo`: Complete manufacturer information
- `ManufacturerFeatures`: Device-specific features
- `ManufacturerSDKs`: Available SDKs and services

#### Usage:
```kotlin
val manufacturerDetector = ManufacturerDetection(context)
val info = manufacturerDetector.detectManufacturer()

when (info.manufacturer) {
    DeviceManufacturer.SAMSUNG -> {
        if (info.features.samsungPen?.supported == true) {
            enableSPenFeatures()
        }
        if (info.features.samsungDex?.supported == true) {
            enableDeXMode()
        }
    }
    DeviceManufacturer.GOOGLE -> {
        info.features.pixelExclusive?.let { pixel ->
            if (pixel.callScreen) enableCallScreening()
            if (pixel.carCrashDetection) enableCrashDetection()
        }
    }
}
```

### 4. CertificationDetector
**Package**: `com.augmentalis.devicemanager.certification`

Comprehensive certification detection using 8 different methods:

#### Detection Methods:
1. **System Properties**: Direct Android system property checks
2. **File System**: Certification file presence verification
3. **Package Detection**: Installed certification packages
4. **Hardware Features**: Hardware-backed security features
5. **Build Configuration**: Build tags and configuration
6. **Model Heuristics**: Pattern matching on device models
7. **Manufacturer APIs**: Vendor-specific certification APIs
8. **Multi-level Verification**: Cross-validation with confidence scoring

#### Certification Types:
- **Enterprise**: Android Enterprise, Zero Touch, EMM/MDM
- **Security**: Knox, FIPS 140-2, Common Criteria, StrongBox
- **Rugged**: MIL-STD-810G/H, IP ratings (IP54-IP69K)
- **Hazardous**: ATEX, IECEx for explosive environments
- **Industry**: Healthcare (HIPAA), Public Safety (FirstNet)
- **Compliance**: GDPR, PCI-DSS, ISO 27001

#### Usage:
```kotlin
val certDetector = CertificationDetector(context)
val certifications = certDetector.detectCertifications()

// Check security level
when (certifications.securityLevel) {
    SecurityLevel.MAXIMUM -> println("Maximum security with FIPS/Knox")
    SecurityLevel.HIGH -> println("Enterprise security features")
    SecurityLevel.STANDARD -> println("Standard Android security")
}

// Check specific certifications
certifications.certifications.forEach { cert ->
    when (cert.type) {
        CertificationType.KNOX_CERTIFIED -> {
            println("Knox ${cert.version} - Confidence: ${cert.confidence}")
        }
        CertificationType.MIL_STD_810G -> {
            println("Military spec certified")
        }
        CertificationType.IP68 -> {
            println("Water resistant to 1.5m")
        }
    }
}

// Check rugged specifications
certifications.ruggedSpecs?.let { specs ->
    println("Drop test: ${specs.dropTestHeight}m")
    println("Operating temp: ${specs.operatingTempMin}°C to ${specs.operatingTempMax}°C")
    specs.ipRatings.forEach { rating ->
        println("${rating.rating}: ${rating.details}")
    }
}
```

## Integration

### Gradle Dependencies
```gradle
dependencies {
    implementation project(':libraries:DeviceManager')
    
    // Required dependencies
    implementation 'androidx.window:window:1.1.0'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
}
```

### Permissions Required
```xml
<!-- Basic detection -->
<uses-permission android:name="android.permission.READ_PHONE_STATE" />

<!-- Sensor access for foldables -->
<uses-feature android:name="android.hardware.sensor.hinge_angle" android:required="false" />

<!-- Enterprise features -->
<uses-feature android:name="android.software.device_admin" android:required="false" />
```

### Complete Example
```kotlin
class DeviceProfiler(private val context: Context) {
    
    private val extendedDetection = ExtendedDeviceDetection(context)
    private val foldableManager = FoldableDeviceManager(context)
    private val manufacturerDetection = ManufacturerDetection(context)
    private val certificationDetector = CertificationDetector(context)
    
    fun profileDevice(): CompleteDeviceProfile {
        // Basic device info
        val deviceInfo = extendedDetection.detectDevice()
        
        // Manufacturer features
        val manufacturerInfo = manufacturerDetection.detectManufacturer()
        
        // Certifications
        val certifications = certificationDetector.detectCertifications()
        
        // Initialize foldable monitoring if applicable
        if (deviceInfo.displayInfo.isFoldable) {
            foldableManager.initialize()
        }
        
        return CompleteDeviceProfile(
            device = deviceInfo,
            manufacturer = manufacturerInfo,
            certifications = certifications,
            isFoldable = foldableManager.isFoldable()
        )
    }
}
```

## Supported Devices

### AR/XR Glasses
- **RealWear**: Navigator 500/520, HMT-1/1Z1
- **Vuzix**: M400, M4000, Blade 2, Shield, Z100
- **Rokid**: Glass 2, Air, Max
- **XREAL/Nreal**: Light, Air, Air 2
- **Others**: Virture One, Even Realities G1, Almer Arc-Two

### Foldable Devices
- **Samsung**: Galaxy Fold series, Galaxy Z Flip series
- **Google**: Pixel Fold
- **Microsoft**: Surface Duo 1/2
- **OPPO**: Find N series
- **Xiaomi**: Mix Fold series
- **Motorola**: Razr series
- **Honor**: Magic V series
- **Vivo**: X Fold series

### Enterprise/Rugged Devices
- **Samsung**: XCover series, Galaxy Active series
- **Zebra**: TC series, MC series
- **Honeywell**: Dolphin, CK series
- **Panasonic**: Toughbook series
- **Getac**: Rugged tablets and handhelds
- **CAT**: CAT S series phones
- **Sonim**: XP series

## Performance Considerations

### Optimization Tips:
1. **Lazy Initialization**: Only initialize components you need
2. **Caching**: Detection results are cached where appropriate
3. **Coroutines**: Foldable monitoring uses coroutines for efficiency
4. **Debouncing**: State changes are debounced to prevent excessive updates

### Memory Usage:
- ExtendedDeviceDetection: ~2MB
- FoldableDeviceManager: ~1MB + sensor listeners
- ManufacturerDetection: ~1.5MB
- CertificationDetector: ~2.5MB

## Testing

### Unit Tests
```kotlin
@Test
fun testARGlassesDetection() {
    // Mock RealWear device
    every { Build.MANUFACTURER } returns "RealWear"
    every { Build.MODEL } returns "Navigator-500"
    
    val detector = ExtendedDeviceDetection(context)
    val info = detector.detectDevice()
    
    assertNotNull(info.arGlassesInfo)
    assertEquals("RealWear", info.arGlassesInfo?.manufacturer)
    assertTrue(info.arGlassesInfo?.voiceControl == true)
}
```

### Integration Tests
Test on real devices or emulators with specific configurations:
- Foldable emulators (Android Studio)
- Samsung Remote Test Lab
- Firebase Test Lab

## Troubleshooting

### Common Issues:

1. **Foldable detection not working**
   - Ensure WindowManager dependency is included
   - Check for hinge angle sensor availability
   - Verify AndroidX Window library version

2. **Certification detection confidence is LOW**
   - System properties may be restricted on some devices
   - Try running with elevated permissions
   - Some certifications require manufacturer APIs

3. **AR glasses not detected**
   - Check Build.MANUFACTURER and Build.MODEL values
   - Some devices may use custom identifiers
   - File an issue with device details for addition

## Contributing
Please submit issues and pull requests with:
- New device models for detection
- Additional certification types
- Manufacturer-specific features
- Bug fixes and optimizations

## License
[Your License Here]

## Version History

### 2.1.0 (September 6, 2025) - Current
- Split AccessibilityManager into TTSManager and FeedbackManager
- Added FeedbackUI for comprehensive feedback settings
- Enhanced Bluetooth audio routing with SCO/A2DP support
- Consolidated audio focus management in AudioService
- Added DeviceCapabilities to all network and sensor managers
- Removed all direct hardware detection from managers
- Deleted deprecated NetworkManager (991 lines removed)

### 1.0.0 (Initial Release)
- Initial release with full device detection suite
- Support for 30+ manufacturers
- 50+ certification types
- Real-time foldable monitoring
- Comprehensive AR glasses support

### Roadmap
- [ ] Additional AR glasses models
- [ ] More manufacturer SDK integrations
- [ ] Enhanced certification validation
- [ ] Performance optimizations
- [ ] Compose UI adaptation helpers

## Developer Resources

### Implementation Examples

#### Quick Start
```kotlin
// Basic usage - DeviceManager handles everything
val deviceManager = DeviceManager.getInstance(context)

// Check what's available
deviceManager.nfc?.let { 
    println("NFC is available") 
}
```

#### Example 1: Payment Processing with Fallbacks
```kotlin
class PaymentProcessor(private val deviceManager: DeviceManager) {
    
    fun processPayment(amount: Double) {
        // Try NFC first
        deviceManager.nfc?.let { nfc ->
            if (nfc.isNfcEnabled()) {
                initiateNFCPayment(amount)
                return
            }
        }
        
        // Fallback to camera/QR
        deviceManager.camera?.let { 
            initiateQRPayment(amount)
            return
        }
        
        // Final fallback
        showManualEntry(amount)
    }
}
```

#### Example 2: Adaptive UI Components
```kotlin
@Composable
fun AdaptiveSettingsScreen(deviceManager: DeviceManager) {
    LazyColumn {
        // Always show basic settings
        item { BasicDeviceSettings() }
        
        // Show NFC settings only if available
        deviceManager.nfc?.let {
            item { NFCSettings(it) }
        }
        
        // Show biometric settings only if available
        deviceManager.biometric?.let {
            item { BiometricSettings(it) }
        }
        
        // Show foldable settings only if available
        deviceManager.foldable?.let {
            item { FoldableDisplaySettings(it) }
        }
    }
}
```

#### Example 3: Feature Detection Service
```kotlin
class DeviceFeatureService(context: Context) {
    private val deviceManager = DeviceManager.getInstance(context)
    
    fun getAvailableFeatures(): Set<String> {
        return buildSet {
            if (deviceManager.nfc != null) add("NFC")
            if (deviceManager.bluetooth != null) add("Bluetooth")
            if (deviceManager.uwb != null) add("UWB")
            if (deviceManager.biometric != null) add("Biometric")
            if (deviceManager.foldable != null) add("Foldable")
        }
    }
    
    fun hasFeature(feature: String): Boolean {
        return when (feature.uppercase()) {
            "NFC" -> deviceManager.nfc != null
            "BLUETOOTH" -> deviceManager.bluetooth != null
            "UWB" -> deviceManager.uwb != null
            "BIOMETRIC" -> deviceManager.biometric != null
            "FOLDABLE" -> deviceManager.foldable != null
            else -> false
        }
    }
}
```

#### Example 4: Sensor Monitoring
```kotlin
class SensorMonitor(private val deviceManager: DeviceManager) {
    
    fun startMonitoring() {
        // Monitor IMU if available
        deviceManager.sensors?.imu?.let { imu ->
            imu.startTracking()
            imu.sensorData.collect { data ->
                processIMUData(data)
            }
        }
        
        // Monitor LiDAR if available
        deviceManager.sensors?.lidar?.let { lidar ->
            lidar.startScanning()
            lidar.depthData.collect { depth ->
                processDepthData(depth)
            }
        }
    }
}
```

#### Example 5: Testing with Mock Capabilities
```kotlin
@Test
fun testConditionalLoading() {
    // Create context with specific capabilities
    val context = createMockContext {
        hasNFC = true
        hasBluetooth = false
        hasUWB = false
    }
    
    val deviceManager = DeviceManager.getInstance(context)
    
    // Verify conditional loading
    assertNotNull(deviceManager.nfc)
    assertNull(deviceManager.bluetooth)
    assertNull(deviceManager.uwb)
}
```

### Documentation

- **[Developer Guide](../../docs/DEVELOPER-GUIDE-CONDITIONAL-LOADING.md)** - Complete implementation guide
- **[Architecture Specification](../../docs/VOS4-Architecture-Specification.md)** - System architecture
- **[Filing Norms](../../docs/MANDATORY-FILING-NORMS.md)** - Code organization standards

## Contact
Author: Manoj Jhawar
Repository: [Your Repository URL]
