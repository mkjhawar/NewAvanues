# DeviceManager API Reference
## Unified Device Management System for VOS4
**Version**: 1.0.0  
**Module**: `com.augmentalis.devicemanager`  
**Last Updated**: 2025-01-30

---

## Table of Contents

1. [Overview](#overview)
2. [Installation](#installation)
3. [Core API](#core-api)
4. [Network Managers](#network-managers)
5. [Sensor Managers](#sensor-managers)
6. [Audio Managers](#audio-managers)
7. [Display & XR](#display--xr)
8. [State Management](#state-management)
9. [Examples](#examples)
10. [Error Handling](#error-handling)

---

## Overview

DeviceManager provides a unified, zero-overhead interface to all device hardware capabilities. It follows VOS4's direct implementation pattern with lazy initialization for optimal performance.

### Key Features
- **Unified Access**: Single entry point for all hardware
- **Lazy Loading**: Managers initialized only when accessed
- **State Management**: Reactive state updates via Kotlin Flow
- **Zero Overhead**: Direct hardware access without abstraction layers

### Architecture
```kotlin
DeviceManager (Singleton)
├── Network
│   ├── BluetoothManager
│   ├── WiFiManager
│   └── UwbManager
├── Sensors
│   ├── LidarManager
│   └── BiometricManager
├── Audio
│   ├── AudioDeviceManager
│   ├── AudioSessionManager
│   └── AudioCapture
└── Display/XR
    ├── DisplayOverlayManager
    ├── GlassesManager
    └── XRManager
```

---

## Installation

### Gradle Dependency
```kotlin
dependencies {
    implementation(project(":libraries:DeviceManager"))
}
```

### Initialization
```kotlin
class MyApplication : Application() {
    lateinit var deviceManager: DeviceManager
    
    override fun onCreate() {
        super.onCreate()
        deviceManager = DeviceManager.getInstance(this)
        deviceManager.initialize()
    }
}
```

---

## Core API

### DeviceManager
```kotlin
class DeviceManager(context: Context) : DefaultLifecycleObserver {
    companion object {
        fun getInstance(context: Context): DeviceManager
    }
    
    // Initialization
    fun initialize()
    fun shutdown()
    fun isReady(): Boolean
    
    // Sub-managers (lazy loaded)
    val bluetooth: BluetoothManager
    val wifi: WiFiManager
    val uwb: UwbManager
    val lidar: LidarManager
    val biometric: BiometricManager
    val audio: AudioDeviceManager
    val display: DisplayOverlayManager
    val glasses: GlassesManager
    val xr: XRManager
    
    // Device information
    val info: DeviceInfo
    val deviceDetection: DeviceDetection
    
    // State
    val deviceState: StateFlow<DeviceState>
    
    // Comprehensive operations
    fun initializeAll()
    fun getComprehensiveDeviceInfo(): DeviceInfoSummary
}
```

### DeviceInfo
```kotlin
class DeviceInfo(context: Context) {
    fun getDeviceModel(): String
    fun getManufacturer(): String
    fun getAndroidVersion(): Int
    fun getRAM(): Long
    fun getCPUCores(): Int
    fun getScreenResolution(): Pair<Int, Int>
    fun getBatteryLevel(): Int
    fun isCharging(): Boolean
    fun getStorageInfo(): StorageInfo
}
```

---

## Network Managers

### BluetoothManager
```kotlin
class BluetoothManager(context: Context) {
    // State
    val bluetoothState: StateFlow<BluetoothState>
    val discoveredDevices: StateFlow<List<BluetoothDevice>>
    val connectedDevices: StateFlow<List<BluetoothDevice>>
    
    // Basic Operations
    fun enable(): Boolean
    fun disable(): Boolean
    fun isEnabled(): Boolean
    
    // Scanning
    fun startScanning()
    fun stopScanning()
    fun startLEScanning(
        filters: List<ScanFilter> = emptyList(),
        settings: ScanSettings? = null
    )
    
    // Connection Management
    fun connect(device: BluetoothDevice): Boolean
    fun disconnect(device: BluetoothDevice)
    fun pair(device: BluetoothDevice)
    fun unpair(device: BluetoothDevice)
    fun getBondedDevices(): Set<BluetoothDevice>
    
    // Bluetooth LE
    fun connectGatt(
        device: BluetoothDevice,
        autoConnect: Boolean = false
    ): BluetoothGatt?
    fun startAdvertising(data: AdvertiseData, settings: AdvertiseSettings)
    fun stopAdvertising()
    
    // Advanced Features
    fun createGattServer(): BluetoothGattServer?
    fun joinMesh(networkKey: String)
    fun sendMeshMessage(message: ByteArray)
    fun enableLeAudio()
    
    // Data Transfer
    fun sendData(device: BluetoothDevice, data: ByteArray): Boolean
    fun receiveData(): Flow<ByteArray>
}

data class BluetoothState(
    val isEnabled: Boolean = false,
    val isScanning: Boolean = false,
    val isAvailable: Boolean = true,
    val version: String? = null,
    val capabilities: BluetoothCapabilities? = null
)

data class BluetoothCapabilities(
    val hasLE: Boolean,
    val hasLEAudio: Boolean,
    val hasLECodedPHY: Boolean,
    val hasLE2MPhy: Boolean,
    val hasLeExtendedAdvertising: Boolean,
    val hasLePeriodicAdvertising: Boolean,
    val hasMesh: Boolean,
    val supportedCodecs: List<String>,
    val maxAdvertiseDataLength: Int
)
```

### WiFiManager
```kotlin
class WiFiManager(context: Context) {
    // State
    val wifiState: StateFlow<WifiState>
    val availableNetworks: StateFlow<List<WifiNetwork>>
    val currentNetwork: StateFlow<WifiNetwork?>
    
    // Basic Operations
    fun enable(): Boolean
    fun disable(): Boolean
    fun isEnabled(): Boolean
    
    // Scanning
    fun startScan()
    fun getScanResults(): List<ScanResult>
    
    // Connection
    fun connect(network: WifiNetwork, password: String? = null): Boolean
    fun disconnect()
    fun forget(network: WifiNetwork)
    fun getSavedNetworks(): List<WifiConfiguration>
    
    // WiFi 6E/7 Features
    fun enable6GHzBand(): Boolean
    fun getMloCapabilities(): MloCapabilities?
    fun enable320MHzChannels(): Boolean
    
    // WiFi Direct (P2P)
    fun startP2pDiscovery()
    fun stopP2pDiscovery()
    fun createGroup()
    fun removeGroup()
    fun connectP2p(device: WifiP2pDevice)
    
    // WiFi Aware (NAN)
    fun publishService(
        serviceName: String,
        serviceInfo: ByteArray? = null
    ): PublishDiscoverySession?
    fun subscribeToService(
        serviceName: String
    ): SubscribeDiscoverySession?
    
    // WiFi RTT (Round Trip Time)
    fun startRanging(
        accessPoints: List<ScanResult>
    ): Flow<RangingResult>
    fun isRttSupported(): Boolean
    
    // Hotspot
    fun startHotspot(config: HotspotConfig): Boolean
    fun stopHotspot()
    fun getConnectedClients(): List<HotspotClient>
}

data class WifiState(
    val isEnabled: Boolean = false,
    val isConnected: Boolean = false,
    val isScanning: Boolean = false,
    val currentNetwork: WifiNetwork? = null,
    val signalStrength: Int = 0,
    val linkSpeed: Int = 0,
    val frequency: Int = 0,
    val capabilities: WifiCapabilities? = null
)

data class WifiCapabilities(
    val supportsWiFi6: Boolean,
    val supportsWiFi6E: Boolean,
    val supportsWiFi7: Boolean,
    val supports5GHz: Boolean,
    val supports6GHz: Boolean,
    val supportsP2P: Boolean,
    val supportsAware: Boolean,
    val supportsRtt: Boolean,
    val supportsTdls: Boolean,
    val maxChannelBandwidth: Int
)
```

### UwbManager
```kotlin
class UwbManager(context: Context) {
    // State
    val uwbState: StateFlow<UwbState>
    val rangingSessions: StateFlow<List<RangingSession>>
    val discoveredDevices: StateFlow<List<UwbDevice>>
    
    // Availability
    fun isSupported(): Boolean
    fun isEnabled(): Boolean
    fun getCapabilities(): UwbCapabilities?
    
    // Discovery
    fun startDiscovery(params: DiscoveryParams? = null)
    fun stopDiscovery()
    
    // Ranging
    fun startRanging(params: RangingParams): RangingSession
    fun stopRanging(sessionId: String)
    fun updateRangingParams(sessionId: String, params: RangingParams)
    
    // Measurements
    fun getDistance(device: UwbDevice): Float
    fun getAngle(device: UwbDevice): AngleInfo
    fun getPosition(device: UwbDevice): Position3D
    fun track3DPosition(device: UwbDevice): Flow<Position3D>
    
    // Advanced Features
    fun createControleeSession(): ControleeSession
    fun createControllerSession(): ControllerSession
    fun sendData(device: UwbDevice, data: ByteArray)
}

data class UwbState(
    val isSupported: Boolean = false,
    val isEnabled: Boolean = false,
    val isRanging: Boolean = false,
    val activeSessionCount: Int = 0,
    val capabilities: UwbCapabilities? = null
)

data class UwbCapabilities(
    val maxRangingDistance: Float,
    val rangingAccuracy: Float,
    val supportsAoA: Boolean,
    val angleAccuracy: Float?,
    val supportedChannels: List<Int>,
    val maxDataRate: Int,
    val chipsetInfo: ChipsetInfo?
)

data class RangingSession(
    val sessionId: String,
    val state: SessionState,
    val devices: List<UwbDevice>,
    val measurements: Flow<RangingMeasurement>
)
```

---

## Sensor Managers

### LidarManager
```kotlin
class LidarManager(context: Context) {
    // State
    val lidarState: StateFlow<LidarState>
    val scanData: StateFlow<ScanData?>
    
    // Availability
    fun isAvailable(): Boolean
    fun getTechnology(): String? // "ToF", "LiDAR", "Structured Light"
    
    // Scanning Control
    fun startScanning(config: ScanConfig = ScanConfig())
    fun stopScanning()
    fun pauseScanning()
    fun resumeScanning()
    
    // Data Acquisition
    fun getDepthMap(): DepthMap
    fun getPointCloud(): PointCloud
    fun get3DMesh(): Mesh3D
    fun getConfidenceMap(): ConfidenceMap
    
    // Real-time Streams
    fun streamDepthData(): Flow<DepthFrame>
    fun streamPointCloud(): Flow<PointCloud>
    
    // Processing
    fun detectObjects(): List<DetectedObject>
    fun detectPlanes(): List<Plane>
    fun measureDistance(point: Point3D): Float
    fun measureArea(points: List<Point3D>): Float
    fun measureVolume(mesh: Mesh3D): Float
    
    // Room Scanning
    fun startRoomScan(): RoomScanSession
    fun exportScan(format: ExportFormat): ByteArray
    
    // Calibration
    fun calibrate()
    fun getCalibrationStatus(): CalibrationStatus
}

data class LidarState(
    val isAvailable: Boolean = false,
    val isActive: Boolean = false,
    val technology: String? = null,
    val capabilities: LidarCapabilities? = null,
    val currentFps: Float = 0f
)

data class LidarCapabilities(
    val maxRange: Float,
    val minRange: Float,
    val accuracy: Float,
    val resolution: Resolution,
    val frameRate: Range<Float>,
    val fieldOfView: FieldOfView,
    val supportsPointCloud: Boolean,
    val supportsMeshGeneration: Boolean,
    val supportsMotionTracking: Boolean,
    val supportsObjectDetection: Boolean
)
```

### BiometricManager
```kotlin
class BiometricManager(context: Context) {
    // State
    val biometricState: StateFlow<BiometricState>
    
    // Availability
    fun isHardwareAvailable(): Boolean
    fun isEnrolled(): Boolean
    fun canAuthenticate(level: Int = BIOMETRIC_STRONG): Int
    
    // Authentication
    fun authenticate(
        title: String,
        subtitle: String? = null,
        description: String? = null,
        negativeButtonText: String = "Cancel",
        allowedAuthenticators: Int = BIOMETRIC_STRONG,
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: String) -> Unit
    )
    
    // Enrollment
    fun launchEnrollment(biometricType: BiometricType)
    fun getEnrolledBiometrics(): List<BiometricType>
    
    // Advanced Features
    fun authenticateWithCrypto(
        cryptoObject: CryptoObject,
        onSuccess: (CryptoObject) -> Unit,
        onError: (errorCode: Int, errString: String) -> Unit
    )
    
    // Type-specific
    fun getFingerprintCount(): Int
    fun getFaceCount(): Int
    fun hasIrisRecognition(): Boolean
}

data class BiometricState(
    val isHardwareAvailable: Boolean = false,
    val isEnrolled: Boolean = false,
    val availableTypes: List<BiometricType> = emptyList(),
    val securityLevel: SecurityLevel = SecurityLevel.NONE,
    val capabilities: BiometricCapabilities? = null
)

data class BiometricType(
    val type: String, // "fingerprint", "face", "iris"
    val isEnrolled: Boolean,
    val securityLevel: SecurityLevel,
    val sensorId: String?
)

enum class SecurityLevel {
    NONE,
    CONVENIENCE,
    WEAK,
    STRONG
}
```

---

## Audio Managers

### AudioDeviceManager
```kotlin
class AudioDeviceManager(context: Context) {
    // Volume Control
    fun setVolume(stream: Int, level: Int)
    fun getVolume(stream: Int): Int
    fun getMaxVolume(stream: Int): Int
    fun mute(stream: Int = STREAM_MUSIC)
    fun unmute(stream: Int = STREAM_MUSIC)
    fun isMuted(stream: Int = STREAM_MUSIC): Boolean
    
    // Audio Routing
    fun setSpeakerphone(enabled: Boolean)
    fun isSpeakerphoneOn(): Boolean
    fun setBluetoothScoOn(enabled: Boolean)
    fun isBluetoothScoOn(): Boolean
    fun routeAudioTo(device: AudioDeviceInfo)
    
    // Device Management
    fun getActiveAudioDevice(): AudioDeviceInfo?
    fun getAvailableAudioDevices(type: Int): List<AudioDeviceInfo>
    fun isHeadsetConnected(): Boolean
    fun isBluetoothA2dpOn(): Boolean
    
    // Audio Focus
    fun requestAudioFocus(
        streamType: Int = STREAM_MUSIC,
        durationHint: Int = AUDIOFOCUS_GAIN
    ): Int
    fun abandonAudioFocus(): Int
    
    // Audio Effects
    fun enableNoiseSuppression(enabled: Boolean)
    fun enableEchoCancellation(enabled: Boolean)
    fun enableAutomaticGainControl(enabled: Boolean)
}
```

### AudioSessionManager
```kotlin
class AudioSessionManager(context: Context) {
    // Session Management
    fun createSession(config: AudioSessionConfig): AudioSession
    fun getSession(sessionId: Int): AudioSession?
    fun getActiveSessions(): List<AudioSession>
    fun releaseSession(sessionId: Int)
    fun releaseAllSessions()
    
    // Global Control
    fun pauseAll()
    fun resumeAll()
    fun stopAll()
    
    // Audio Processing
    fun applyEqualizer(preset: EqualizerPreset)
    fun applyReverb(preset: ReverbPreset)
    fun setBassBoost(strength: Int)
    fun setVirtualizer(strength: Int)
}

data class AudioSession(
    val sessionId: Int,
    val state: SessionState,
    val config: AudioSessionConfig,
    val audioTrack: AudioTrack?,
    val audioRecord: AudioRecord?
)
```

### AudioCapture
```kotlin
class AudioCapture(context: Context) {
    // Recording Control
    fun startRecording(config: AudioConfig = AudioConfig.forSpeechRecognition())
    fun stopRecording()
    fun pauseRecording()
    fun resumeRecording()
    fun isRecording(): Boolean
    
    // Data Access
    fun getAudioStream(): Flow<ByteArray>
    fun getAudioLevel(): Float
    fun getPeakAmplitude(): Int
    
    // Configuration
    fun setAudioConfig(config: AudioConfig)
    fun getAudioConfig(): AudioConfig
    
    // File Operations
    fun recordToFile(filePath: String, format: AudioFormat)
    fun saveRecording(): File?
}

data class AudioConfig(
    val sampleRate: Int = 16000,
    val channelConfig: Int = CHANNEL_IN_MONO,
    val audioFormat: Int = ENCODING_PCM_16BIT,
    val bufferSize: Int = 0, // 0 = auto
    val noiseSuppression: Boolean = true,
    val echoCancellation: Boolean = false,
    val automaticGainControl: Boolean = true
) {
    companion object {
        fun forSpeechRecognition(): AudioConfig
        fun forDictation(): AudioConfig
        fun forMusic(): AudioConfig
        fun forVoiceCall(): AudioConfig
    }
}
```

---

## Display & XR

### DisplayOverlayManager
```kotlin
class DisplayOverlayManager(context: Context) {
    // Overlay Control
    fun showOverlay(view: View, params: OverlayParams = OverlayParams())
    fun hideOverlay()
    fun updateOverlay(view: View)
    fun isOverlayShowing(): Boolean
    
    // Positioning
    fun setPosition(x: Int, y: Int)
    fun setSize(width: Int, height: Int)
    fun setGravity(gravity: Int)
    
    // Animation
    fun animateIn(animation: Animation)
    fun animateOut(animation: Animation)
    fun fadeIn(duration: Long = 300)
    fun fadeOut(duration: Long = 300)
    
    // Display Info
    fun getBrightness(): Int
    fun setBrightness(level: Int)
    fun getRotation(): Int
    fun keepScreenOn(enabled: Boolean)
    fun isScreenOn(): Boolean
    fun getRefreshRate(): Float
    fun getDisplayMetrics(): DisplayMetrics
}

data class OverlayParams(
    val width: Int = WRAP_CONTENT,
    val height: Int = WRAP_CONTENT,
    val x: Int = 0,
    val y: Int = 0,
    val gravity: Int = Gravity.TOP or Gravity.START,
    val flags: Int = FLAG_NOT_FOCUSABLE,
    val format: Int = PixelFormat.TRANSLUCENT,
    val alpha: Float = 1.0f
)
```

### GlassesManager
```kotlin
class GlassesManager(context: Context) {
    // Connection
    fun isConnected(): Boolean
    fun connect(): Boolean
    fun disconnect()
    fun getGlassesModel(): String?
    fun getGlassesInfo(): GlassesInfo?
    
    // Display
    fun sendNotification(text: String, duration: Long = 3000)
    fun displayOverlay(content: View)
    fun clearOverlay()
    fun setBrightness(level: Int)
    
    // Interaction
    fun vibrateGlasses(pattern: LongArray)
    fun playSound(soundId: Int)
    fun captureImage(): Bitmap?
    
    // Sensors
    fun getHeadTracking(): Flow<HeadTrackingData>
    fun getEyeTracking(): Flow<EyeTrackingData>
    fun getTouchpadEvents(): Flow<TouchEvent>
}

data class GlassesInfo(
    val model: String,
    val manufacturer: String,
    val firmwareVersion: String,
    val batteryLevel: Int,
    val displayResolution: Pair<Int, Int>,
    val hasCamera: Boolean,
    val hasTouchpad: Boolean,
    val hasEyeTracking: Boolean
)
```

### XRManager
```kotlin
class XRManager(context: Context) {
    // XR Support
    fun isXRSupported(): Boolean
    fun getXRCapabilities(): XRCapabilities?
    
    // Session Management
    fun enterXRMode(config: XRConfig = XRConfig())
    fun exitXRMode()
    fun isInXRMode(): Boolean
    
    // Tracking
    fun trackHeadPosition(): Flow<Position6DOF>
    fun trackHandPosition(hand: Hand): Flow<HandTracking>
    fun trackEyeGaze(): Flow<EyeGazeData>
    
    // Rendering
    fun renderSpatialUI(element: View, position: Position3D)
    fun renderSpatialAnchor(anchor: SpatialAnchor)
    fun render3DModel(model: Model3D, transform: Transform)
    
    // Environment Understanding
    fun detectPlanes(): Flow<List<Plane>>
    fun detectAnchors(): Flow<List<Anchor>>
    fun getMeshMap(): MeshMap
    
    // Interaction
    fun handleControllerInput(): Flow<ControllerInput>
    fun handleGestures(): Flow<Gesture>
}

data class XRCapabilities(
    val supports6DOF: Boolean,
    val supportsHandTracking: Boolean,
    val supportsEyeTracking: Boolean,
    val supportsPassthrough: Boolean,
    val supportsMeshMapping: Boolean,
    val maxRenderResolution: Pair<Int, Int>,
    val fieldOfView: Float,
    val refreshRate: Int
)
```

---

## State Management

### Device State
```kotlin
data class DeviceState(
    val isInitialized: Boolean = false,
    val capabilities: DeviceCapabilities? = null,
    val errors: List<String> = emptyList()
)

data class DeviceCapabilities(
    val hasWifi: Boolean,
    val hasBluetooth: Boolean,
    val hasUwb: Boolean,
    val hasLidar: Boolean,
    val hasBiometric: Boolean,
    val hasNfc: Boolean,
    val deviceInfo: DeviceInfoSummary
)
```

### Observing State Changes
```kotlin
// Observe device state
deviceManager.deviceState
    .onEach { state ->
        updateUI(state)
    }
    .launchIn(lifecycleScope)

// Observe specific manager state
deviceManager.bluetooth.bluetoothState
    .onEach { state ->
        updateBluetoothUI(state)
    }
    .launchIn(lifecycleScope)
```

---

## Examples

### Basic Initialization
```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var deviceManager: DeviceManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get instance
        deviceManager = DeviceManager.getInstance(this)
        
        // Initialize all managers
        lifecycleScope.launch {
            deviceManager.initializeAll()
        }
    }
}
```

### Bluetooth Scanning
```kotlin
// Start Bluetooth LE scanning
deviceManager.bluetooth.startLEScanning(
    filters = listOf(
        ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(MY_SERVICE_UUID))
            .build()
    )
)

// Observe discovered devices
deviceManager.bluetooth.discoveredDevices
    .onEach { devices ->
        updateDeviceList(devices)
    }
    .launchIn(lifecycleScope)
```

### WiFi Direct Connection
```kotlin
// Start WiFi Direct discovery
deviceManager.wifi.startP2pDiscovery()

// Create a group
deviceManager.wifi.createGroup()

// Connect to a peer
fun connectToPeer(device: WifiP2pDevice) {
    deviceManager.wifi.connectP2p(device)
}
```

### UWB Ranging
```kotlin
// Start UWB ranging session
val session = deviceManager.uwb.startRanging(
    RangingParams(
        updateRate = RangingParams.UpdateRate.FREQUENT,
        sessionType = RangingParams.SessionType.CONTROLLER
    )
)

// Observe measurements
session.measurements
    .onEach { measurement ->
        val distance = measurement.distance
        val angle = measurement.angleOfArrival
        updatePosition(distance, angle)
    }
    .launchIn(lifecycleScope)
```

### LiDAR Scanning
```kotlin
// Start LiDAR scanning
deviceManager.lidar.startScanning(
    ScanConfig(
        resolution = ScanConfig.Resolution.HIGH,
        range = ScanConfig.Range.MEDIUM,
        frameRate = 30
    )
)

// Get point cloud stream
deviceManager.lidar.streamPointCloud()
    .onEach { pointCloud ->
        render3DPoints(pointCloud)
    }
    .launchIn(lifecycleScope)
```

### Biometric Authentication
```kotlin
deviceManager.biometric.authenticate(
    title = "Authenticate",
    subtitle = "Use your fingerprint to continue",
    negativeButtonText = "Cancel",
    onSuccess = {
        // Authentication successful
        proceedWithAction()
    },
    onError = { errorCode, errorString ->
        // Handle error
        showError(errorString)
    }
)
```

### Audio Recording
```kotlin
// Start recording for speech
deviceManager.audioCapture.startRecording(
    AudioConfig.forSpeechRecognition()
)

// Get audio stream
deviceManager.audioCapture.getAudioStream()
    .onEach { audioData ->
        processAudioData(audioData)
    }
    .launchIn(lifecycleScope)

// Stop recording
deviceManager.audioCapture.stopRecording()
```

---

## Error Handling

### Error Types
```kotlin
sealed class DeviceError : Exception() {
    data class InitializationError(override val message: String) : DeviceError()
    data class PermissionError(val permission: String) : DeviceError()
    data class HardwareNotAvailable(val feature: String) : DeviceError()
    data class ConnectionError(override val message: String) : DeviceError()
    data class OperationFailed(override val message: String) : DeviceError()
}
```

### Error Handling Pattern
```kotlin
try {
    deviceManager.bluetooth.enable()
} catch (e: DeviceError) {
    when (e) {
        is DeviceError.PermissionError -> requestPermission(e.permission)
        is DeviceError.HardwareNotAvailable -> showFeatureNotAvailable(e.feature)
        is DeviceError.OperationFailed -> showError(e.message)
        else -> handleGenericError(e)
    }
}
```

### Permission Handling
```kotlin
// Check permissions before operations
if (!deviceManager.bluetooth.hasPermissions()) {
    requestBluetoothPermissions()
    return
}

// Permission request helper
fun requestBluetoothPermissions() {
    val permissions = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_ADVERTISE
    )
    requestPermissions(permissions, REQUEST_BLUETOOTH_PERMISSIONS)
}
```

---

## Performance Considerations

### Best Practices
1. **Lazy Initialization**: Managers are lazy-loaded, access only what you need
2. **Lifecycle Awareness**: Use lifecycle-aware components to manage resources
3. **Coroutine Scopes**: Use appropriate scopes for async operations
4. **State Observation**: Use StateFlow for efficient state updates
5. **Resource Cleanup**: Always release resources when done

### Memory Management
```kotlin
override fun onDestroy() {
    super.onDestroy()
    // Stop operations
    deviceManager.bluetooth.stopScanning()
    deviceManager.wifi.stopP2pDiscovery()
    deviceManager.lidar.stopScanning()
    
    // Release resources
    deviceManager.shutdown()
}
```

### Battery Optimization
```kotlin
// Use appropriate scan settings for battery efficiency
deviceManager.bluetooth.startLEScanning(
    settings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
        .build()
)

// Stop scanning when not needed
lifecycle.addObserver(object : DefaultLifecycleObserver {
    override fun onPause(owner: LifecycleOwner) {
        deviceManager.bluetooth.stopScanning()
    }
})
```

---

## Migration Guide

### From Legacy DeviceMGR
```kotlin
// Old way
val deviceMGR = DeviceMGR(context)
deviceMGR.audio.setVolume(50)

// New way
val deviceManager = DeviceManager.getInstance(context)
deviceManager.audio.setVolume(STREAM_MUSIC, 50)
```

### From Separate Managers
```kotlin
// Old way
val bluetoothManager = BluetoothManager(context)
val wifiManager = WiFiManager(context)

// New way - unified access
val deviceManager = DeviceManager.getInstance(context)
deviceManager.bluetooth.startScanning()
deviceManager.wifi.startScan()
```

---

## Version History

- **1.0.0** (2025-01-30): Initial release with comprehensive hardware support
- **0.9.0** (2025-01-15): Beta with core managers
- **0.5.0** (2024-12-01): Alpha release

---

## License

Copyright (C) 2025 Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC  
All rights reserved. Proprietary and confidential.
