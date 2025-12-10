/**
 * DeviceManager.kt
 * Path: /libraries/DeviceManager/src/main/java/com/augmentalis/devicemanager/DeviceManager.kt
 * 
 * Created: 2025-01-22
 * Last Modified: 2025-09-06
 * Author: Manoj Jhawar
 * Version: 2.1.0
 * 
 * Purpose: Unified Device Manager - Single module for all device operations
 * Module: DeviceManager
 * 
 * Changelog:
 * - v1.0.0 (2025-01-22): Initial creation with core managers
 * - v2.0.0 (2025-01-23): Fixed import paths for VideoManager, GlassesManager, XRManager
 * - v2.1.0 (2025-09-06): Added TTSManager and FeedbackManager, removed old AccessibilityManager
 */

package com.augmentalis.devicemanager

import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.augmentalis.devicemanager.audio.AudioService
import com.augmentalis.devicemanager.display.DisplayOverlayManager
import com.augmentalis.devicemanager.network.BluetoothManager
import com.augmentalis.devicemanager.network.WiFiManager
import com.augmentalis.devicemanager.network.UwbManager
import com.augmentalis.devicemanager.network.NfcManager
import com.augmentalis.devicemanager.network.CellularManager
import com.augmentalis.devicemanager.network.UsbNetworkManager
import com.augmentalis.devicemanager.sensors.LidarManager
import com.augmentalis.devicemanager.sensors.imu.IMUManager
import com.augmentalis.devicemanager.security.BiometricManager
import com.augmentalis.devicemanager.accessibility.TTSManager
import com.augmentalis.devicemanager.accessibility.FeedbackManager
import com.augmentalis.devicemanager.deviceinfo.detection.DeviceDetection
import com.augmentalis.devicemanager.video.VideoManager
import com.augmentalis.devicemanager.smartglasses.GlassesManager
import com.augmentalis.devicemanager.smartglasses.XRManager
import com.augmentalis.devicemanager.deviceinfo.detection.DeviceDetector
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Unified Device Manager - Single source for all device operations
 * Direct implementation, no interfaces
 * 
 * Components:
 * - DeviceInfo: Device information and metrics
 * - DisplayManager: Display and overlay management
 * - GlassesManager: Smart glasses support
 * - AudioDeviceManager: Single source for audio control
 * - BluetoothManager: Comprehensive Bluetooth support (Classic, LE, Mesh)
 * - WiFiManager: WiFi 6E/7, Direct, Aware, RTT support
 * - UwbManager: Ultra-Wideband ranging and positioning
 * - LidarManager: LiDAR/ToF depth sensing and 3D scanning
 * - BiometricManager: Multi-modal biometric authentication
 * - TTSManager: Text-to-Speech functionality
 * - FeedbackManager: Haptic, audio, and visual feedback systems
 */
class DeviceManager(private val context: Context) : DefaultLifecycleObserver {
    
    // Static instance for single source access
    companion object {
        private const val TAG = "DeviceManager"
        
        @Volatile
        private var instance: DeviceManager? = null
        
        @JvmStatic
        fun getInstance(context: Context): DeviceManager {
            return instance ?: synchronized(this) {
                instance ?: DeviceManager(context).also { instance = it }
            }
        }
    }
    
    // Device capabilities detection
    private val deviceCapabilities = DeviceDetector.getCapabilities(context)
    
    // Core components - always available
    val info: DeviceInfo by lazy { DeviceInfo(context) }
    val display: DisplayOverlayManager by lazy { DisplayOverlayManager(context) }
    val audio: AudioService by lazy { AudioService(context) }
    // Accessibility managers - conditionally loaded based on system capabilities
    val tts: TTSManager? by lazy {
        if (deviceCapabilities.audio.hasSpeaker && !deviceCapabilities.integration.requiresDisableSpeech) TTSManager(context, audio) else null.also {
            Log.d(TAG, "TTS not available (no speaker or speech disabled) - manager not loaded")
        }
    }
    
    val feedback: FeedbackManager? by lazy {
        // FeedbackManager handles haptic, audio, and visual feedback - load if device has basic capabilities
        if (deviceCapabilities.audio.hasSpeaker || !deviceCapabilities.hardware.isWatch) FeedbackManager(context) else null.also {
            Log.d(TAG, "Feedback capabilities not available - manager not loaded")
        }
    }
    val deviceDetection: DeviceDetection by lazy { DeviceDetection(context) }
    
    // Conditionally loaded managers based on capability detection
    // These are only initialized if the hardware/feature is available
    
    // Network managers - only load if capability exists
    val bluetooth: BluetoothManager? by lazy { 
        if (deviceCapabilities.network.hasBluetooth) BluetoothManager(context, deviceCapabilities) else null.also {
            Log.d(TAG, "Bluetooth not available - manager not loaded")
        }
    }
    
    val wifi: WiFiManager? by lazy { 
        if (deviceCapabilities.network.hasWiFi) WiFiManager(context, deviceCapabilities) else null.also {
            Log.d(TAG, "WiFi not available - manager not loaded")
        }
    }
    
    val uwb: UwbManager? by lazy { 
        if (deviceCapabilities.network.hasUwb) UwbManager(context, deviceCapabilities) else null.also {
            Log.d(TAG, "UWB not available - manager not loaded")
        }
    }
    
    val nfc: NfcManager? by lazy { 
        if (deviceCapabilities.network.hasNfc) NfcManager(context, deviceCapabilities) else null.also {
            Log.d(TAG, "NFC not available - manager not loaded")
        }
    }
    
    val cellular: CellularManager? by lazy { 
        if (deviceCapabilities.network.hasCellular) CellularManager(context, deviceCapabilities) else null.also {
            Log.d(TAG, "Cellular not available - manager not loaded")
        }
    }
    
    val usbNetwork: UsbNetworkManager? by lazy { 
        if (deviceCapabilities.hardware.hasUsb) UsbNetworkManager(context) else null.also {
            Log.d(TAG, "USB host not available - manager not loaded")
        }
    }
    
    // Sensor managers - only load if hardware exists
    val lidar: LidarManager? by lazy { 
        if (deviceCapabilities.sensors.totalSensorCount > 0) LidarManager(context) else null.also { // LiDAR check simplified
            Log.d(TAG, "LiDAR not available - manager not loaded")
        }
    }
    
    // IMU manager for motion and orientation tracking
    val imu: IMUManager by lazy {
        IMUManager.getInstance(context).also { manager ->
            // Inject device capabilities when IMU manager is first accessed
            manager.injectCapabilities(deviceCapabilities)
            Log.d(TAG, "IMU manager initialized with device capabilities")
        }
    }
    
    // Security managers
    val biometric: BiometricManager? by lazy { 
        if (deviceCapabilities.biometric != null) BiometricManager(context, deviceCapabilities) else null.also {
            Log.d(TAG, "Biometric not available - manager not loaded")
        }
    }
    
    // Device-specific managers
    val video: VideoManager? by lazy { 
        if (deviceCapabilities.camera != null) VideoManager(context) else null.also {
            Log.d(TAG, "Camera not available - video manager not loaded")
        }
    }
    
    val glasses: GlassesManager by lazy { GlassesManager(context, deviceCapabilities) }
    
    val xr: XRManager? by lazy { 
        if (deviceCapabilities.display.hasXrSupport) XRManager(context) else null.also {
            Log.d(TAG, "XR not supported - manager not loaded")
        }
    }
    
    
    // Coroutine scope for async operations
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // State flows
    private val _deviceState = MutableStateFlow(DeviceState())
    val deviceState: StateFlow<DeviceState> = _deviceState.asStateFlow()
    
    private val isInitialized = AtomicBoolean(false)
    
    fun initialize() {
        if (isInitialized.get()) return
        
        try {
            // Initialize sub-components as needed
            info.initialize()
            // Display, glasses, XR, video initialized on-demand
            
            isInitialized.set(true)
        } catch (e: Exception) {
            throw RuntimeException("Failed to initialize DeviceManager", e)
        }
    }
    
    fun shutdown() {
        if (!isInitialized.get()) return
        
        // Cleanup sub-components
        display.release()
        glasses.release()
        xr?.exitXRMode()
        video?.release()
        audio.release()
        tts?.release()
        feedback?.cleanup()
        // Stop IMU tracking if active - use DeviceManager as consumer ID
        imu.stopIMUTracking("DeviceManager")
        
        isInitialized.set(false)
    }
    
    fun isReady(): Boolean = isInitialized.get()
    
    fun hasNFC(): Boolean = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_NFC)
    
    // ========== DATA MODELS ==========
    
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
    
    data class DeviceInfoSummary(
        val manufacturer: String = android.os.Build.MANUFACTURER,
        val model: String = android.os.Build.MODEL,
        val androidVersion: Int = android.os.Build.VERSION.SDK_INT,
        val bluetoothInfo: BluetoothInfo? = null,
        val wifiInfo: WifiInfo? = null,
        val uwbInfo: UwbInfo? = null,
        val lidarInfo: LidarInfo? = null,
        val biometricInfo: BiometricInfo? = null
    )
    
    data class BluetoothInfo(
        val isEnabled: Boolean,
        val isScanning: Boolean,
        val version: String?,
        val connectedDevices: Int,
        val discoveredDevices: Int
    )
    
    data class WifiInfo(
        val isEnabled: Boolean,
        val isConnected: Boolean,
        val currentNetwork: String?,
        val wifiStandard: String
    )
    
    data class UwbInfo(
        val isSupported: Boolean,
        val isEnabled: Boolean,
        val isRanging: Boolean
    )
    
    data class LidarInfo(
        val isAvailable: Boolean,
        val isActive: Boolean,
        val technology: String?
    )
    
    data class BiometricInfo(
        val isHardwareAvailable: Boolean,
        val isEnrolled: Boolean,
        val availableTypes: List<String>,
        val securityLevel: String
    )
    
    // ========== COMPREHENSIVE METHODS ==========
    
    /**
     * Initialize all hardware managers
     */
    fun initializeAll() {
        scope.launch {
            try {
                Log.d(TAG, "Initializing all hardware managers...")
                
                // Initialize existing components
                initialize()
                
                // Update device capabilities
                updateDeviceCapabilities()
                
                Log.d(TAG, "All managers initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize managers", e)
                _deviceState.update { it.copy(errors = it.errors + e.message.orEmpty()) }
            }
        }
    }
    
    private fun updateDeviceCapabilities() {
        val bluetoothInfo = bluetooth?.let {
            BluetoothInfo(
                isEnabled = it.bluetoothState.value.isEnabled,
                isScanning = it.bluetoothState.value.isScanning,
                version = "Bluetooth 5.0", // Default version, adapter details not directly accessible
                connectedDevices = it.connectedDevices.value.size,
                discoveredDevices = it.discoveredDevices.value.size
            )
        }
        
        val wifiInfo = wifi?.let {
            WifiInfo(
                isEnabled = it.wifiState.value.isEnabled,
                isConnected = it.wifiState.value.isConnected,
                currentNetwork = it.wifiState.value.currentNetwork?.ssid,
                wifiStandard = it.wifiState.value.wifiStandard.toString()
            )
        }
        
        val uwbInfo = uwb?.let {
            UwbInfo(
                isSupported = it.uwbState.value.isSupported,
                isEnabled = it.uwbState.value.isEnabled,
                isRanging = it.uwbState.value.isRanging
            )
        }
        
        val lidarInfo = lidar?.let {
            LidarInfo(
                isAvailable = it.lidarState.value.isAvailable,
                isActive = it.lidarState.value.isActive,
                technology = it.lidarState.value.technology
            )
        }
        
        val biometricInfo = biometric?.let {
            BiometricInfo(
                isHardwareAvailable = it.biometricState.value.isHardwareAvailable,
                isEnrolled = it.biometricState.value.isEnrolled,
                availableTypes = it.biometricState.value.availableTypes.map { type -> type.type },
                securityLevel = it.biometricState.value.securityLevel.toString()
            )
        }
        
        val deviceInfo = DeviceInfoSummary(
            bluetoothInfo = bluetoothInfo,
            wifiInfo = wifiInfo,
            uwbInfo = uwbInfo,
            lidarInfo = lidarInfo,
            biometricInfo = biometricInfo
        )
        
        val capabilities = DeviceCapabilities(
            hasWifi = wifi?.wifiState?.value?.capabilities != null,
            hasBluetooth = bluetooth?.bluetoothState?.value?.isEnabled ?: false,
            hasUwb = uwb?.uwbState?.value?.isSupported ?: false,
            hasLidar = lidar?.lidarState?.value?.isAvailable ?: false,
            hasBiometric = biometric?.biometricState?.value?.isHardwareAvailable ?: false,
            hasNfc = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_NFC),
            deviceInfo = deviceInfo
        )
        
        _deviceState.update { it.copy(capabilities = capabilities) }
    }
    
    /**
     * Get comprehensive device information
     */
    fun getComprehensiveDeviceInfo(): DeviceInfoSummary {
        updateDeviceCapabilities()
        return _deviceState.value.capabilities?.deviceInfo ?: DeviceInfoSummary()
    }
    
    /**
     * Clean up all managers
     */
    fun cleanupAll() {
        shutdown()
        bluetooth?.cleanup()
        wifi?.cleanup()
        uwb?.cleanup()
        nfc?.cleanup()
        cellular?.cleanup()
        usbNetwork?.cleanup()
        lidar?.cleanup()
        biometric?.cleanup()
        tts?.release()
        feedback?.cleanup()
        scope.cancel()
    }
    
    // ========== LIFECYCLE ==========
    
    override fun onStart(owner: LifecycleOwner) {
        Log.d(TAG, "Lifecycle: onStart")
        if (!isInitialized.get()) {
            initializeAll()
        }
    }
    
    override fun onStop(owner: LifecycleOwner) {
        Log.d(TAG, "Lifecycle: onStop")
        // Stop active operations but don't cleanup
        // bluetooth?.stopScanning() // Method doesn't exist
        lidar?.stopScanning()
        uwb?.stopRanging("")
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        Log.d(TAG, "Lifecycle: onDestroy")
        cleanupAll()
    }
}
