/**
 * CapabilityQuery.kt
 * Path: /libraries/DeviceManager/src/main/java/com/augmentalis/devicemanager/capabilities/CapabilityQuery.kt
 *
 * Created: 2025-10-09
 * Last Modified: 2025-10-09
 * Author: Manoj Jhawar
 * Version: 1.0.0
 *
 * Purpose: Unified capability snapshot and query system for all device features
 * Module: DeviceManager
 *
 * Changelog:
 * - v1.0.0 (2025-10-09): Initial creation with comprehensive capability querying
 */

package com.augmentalis.devicemanager.capabilities

import android.content.Context
import android.os.Build
import android.util.Log
import com.augmentalis.devicemanager.deviceinfo.detection.DeviceDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * CapabilityQuery - Query all device capabilities at once
 *
 * Provides:
 * - Unified capability snapshot (all hardware features)
 * - JSON export (for debugging/logging)
 * - Comparison tools (compare two devices)
 * - Feature flags (NFC, UWB, Bluetooth, WiFi, sensors, etc.)
 *
 * COT Analysis: Aggregation Strategy
 * - Aggregates data from DeviceDetector's comprehensive capability detection
 * - Provides high-level boolean flags for quick capability checks
 * - Supports JSON serialization for logging, debugging, and remote diagnostics
 * - Comparison functionality enables device compatibility analysis
 */
class CapabilityQuery(private val context: Context) {

    companion object {
        private const val TAG = "CapabilityQuery"
        private const val VERSION = "1.0.0"
    }

    // Cached capabilities to avoid repeated detection
    private var cachedSnapshot: CapabilitySnapshot? = null
    private var lastUpdateTime: Long = 0
    private val cacheValidityMs = 60_000L // 1 minute cache

    /**
     * Get a complete snapshot of all device capabilities
     *
     * @param forceRefresh Force fresh detection, ignoring cache
     * @return Complete capability snapshot
     */
    suspend fun getSnapshot(forceRefresh: Boolean = false): CapabilitySnapshot = withContext(Dispatchers.IO) {
        val currentTime = System.currentTimeMillis()

        // Return cached snapshot if valid and not forced refresh
        val cached = cachedSnapshot
        if (!forceRefresh && cached != null && (currentTime - lastUpdateTime) < cacheValidityMs) {
            Log.d(TAG, "Returning cached capability snapshot")
            return@withContext cached
        }

        Log.d(TAG, "Generating fresh capability snapshot")

        // Get comprehensive capabilities from DeviceDetector
        val capabilities = DeviceDetector.getCapabilities(context, forceRefresh)

        // Build unified snapshot
        val snapshot = CapabilitySnapshot(
            timestamp = currentTime,
            deviceInfo = DeviceInfoSnapshot(
                manufacturer = capabilities.deviceInfo.manufacturer,
                model = capabilities.deviceInfo.model,
                device = capabilities.deviceInfo.device,
                brand = capabilities.deviceInfo.brand,
                androidVersion = capabilities.deviceInfo.androidVersion,
                androidVersionName = capabilities.deviceInfo.androidVersionName,
                buildId = capabilities.deviceInfo.buildId,
                fingerprint = capabilities.deviceInfo.buildFingerprint
            ),

            // Network features
            networkFeatures = NetworkFeatures(
                hasBluetooth = capabilities.network.hasBluetooth,
                hasBluetoothLE = capabilities.network.hasBluetoothLE,
                hasWiFi = capabilities.network.hasWiFi,
                hasWiFiDirect = capabilities.network.hasWiFiDirect,
                hasWiFiAware = capabilities.network.hasWiFiAware,
                hasWiFiRtt = capabilities.network.hasWiFiRtt,
                hasUwb = capabilities.network.hasUwb,
                hasNfc = capabilities.network.hasNfc,
                hasCellular = capabilities.network.hasCellular,
                has5G = capabilities.network.has5G,
                hasEthernet = capabilities.network.hasEthernet,
                bluetoothVersion = capabilities.bluetooth?.bluetoothVersion,
                wifiStandard = capabilities.wifi?.wifiStandard
            ),

            // Sensor features
            sensorFeatures = SensorFeatures(
                hasAccelerometer = capabilities.sensors.hasAccelerometer,
                hasGyroscope = capabilities.sensors.hasGyroscope,
                hasMagnetometer = capabilities.sensors.hasMagnetometer,
                hasBarometer = capabilities.sensors.hasBarometer,
                hasProximity = capabilities.sensors.hasProximity,
                hasLight = capabilities.sensors.hasLight,
                hasTemperature = capabilities.sensors.hasTemperature,
                hasHumidity = capabilities.sensors.hasHumidity,
                hasStepCounter = capabilities.sensors.hasStepCounter,
                hasHeartRate = capabilities.sensors.hasHeartRate,
                hasHingeAngle = capabilities.sensors.hasHingeAngle,
                totalSensorCount = capabilities.sensors.totalSensorCount,
                sensorList = capabilities.sensors.sensorList
            ),

            // Hardware features
            hardwareFeatures = HardwareFeatures(
                hasNfc = capabilities.hardware.hasNfc,
                hasCamera = capabilities.hardware.hasCamera,
                hasCameraFront = capabilities.hardware.hasCameraFront,
                hasCameraFlash = capabilities.hardware.hasCameraFlash,
                hasMicrophone = capabilities.hardware.hasMicrophone,
                hasTelephony = capabilities.hardware.hasTelephony,
                hasUsb = capabilities.hardware.hasUsb,
                hasUsbAccessory = capabilities.hardware.hasUsbAccessory,
                hasFingerprint = capabilities.hardware.hasFingerprint,
                hasFace = capabilities.hardware.hasFace,
                hasIris = capabilities.hardware.hasIris,
                isFoldable = capabilities.hardware.isFoldable,
                isTablet = capabilities.hardware.isTablet,
                isWatch = capabilities.hardware.isWatch,
                isAutomotive = capabilities.hardware.isAutomotive,
                isTelevision = capabilities.hardware.isTelevision
            ),

            // Audio/Video features
            mediaFeatures = MediaFeatures(
                hasMicrophone = capabilities.audio.hasMicrophone,
                hasSpeaker = capabilities.audio.hasSpeaker,
                hasLowLatencyAudio = capabilities.audio.hasLowLatencyAudio,
                hasProAudio = capabilities.audio.hasProAudio,
                cameraCount = capabilities.camera?.cameraCount ?: 0,
                hasFrontCamera = capabilities.camera?.hasFrontCamera ?: false,
                hasDepthCamera = capabilities.camera?.hasDepthCamera ?: false,
                hasMultipleRearCameras = capabilities.camera?.hasMultipleRearCameras ?: false
            ),

            // Biometric features
            biometricFeatures = BiometricFeatures(
                hasFingerprint = capabilities.biometric.hasFingerprint,
                hasFace = capabilities.biometric.hasFace,
                hasIris = capabilities.biometric.hasIris,
                hasBiometric = capabilities.biometric.hasBiometric,
                biometricLevel = capabilities.biometric.biometricLevel,
                canAuthenticate = capabilities.biometric.canAuthenticateWithBiometrics
            ),

            // Display features
            displayFeatures = DisplayFeatures(
                widthPixels = capabilities.display.widthPixels,
                heightPixels = capabilities.display.heightPixels,
                densityDpi = capabilities.display.densityDpi,
                density = capabilities.display.density,
                refreshRate = capabilities.display.refreshRate,
                isHdr = capabilities.display.isHdr,
                isWideColorGamut = capabilities.display.isWideColorGamut,
                isRound = capabilities.display.isRound,
                hasXrSupport = capabilities.display.hasXrSupport
            ),

            // Behavioral characteristics
            behavioralFeatures = BehavioralFeatures(
                isSmartGlass = capabilities.behavioral.isSmartGlass,
                isWearable = capabilities.behavioral.isWearable,
                isAutomotive = capabilities.behavioral.isAutomotive,
                isTelevision = capabilities.behavioral.isTelevision,
                isVoiceFirst = capabilities.behavioral.isVoiceFirst,
                requiresLargeTouchTargets = capabilities.behavioral.requiresLargeTouchTargets,
                needsBatteryOptimization = capabilities.behavioral.needsBatteryOptimization,
                cursorStabilizationDelay = capabilities.behavioral.cursorStabilizationDelay,
                preferredInputMethod = capabilities.behavioral.preferredInputMethod
            )
        )

        // Cache the snapshot
        cachedSnapshot = snapshot
        lastUpdateTime = currentTime

        Log.d(TAG, "Capability snapshot generated successfully")
        return@withContext snapshot
    }

    /**
     * Export capabilities to JSON string
     *
     * @param forceRefresh Force fresh detection
     * @return JSON string representation of all capabilities
     */
    suspend fun toJson(forceRefresh: Boolean = false): String = withContext(Dispatchers.IO) {
        val snapshot = getSnapshot(forceRefresh)

        val json = JSONObject().apply {
            put("version", VERSION)
            put("timestamp", snapshot.timestamp)
            put("deviceInfo", snapshot.deviceInfo.toJson())
            put("networkFeatures", snapshot.networkFeatures.toJson())
            put("sensorFeatures", snapshot.sensorFeatures.toJson())
            put("hardwareFeatures", snapshot.hardwareFeatures.toJson())
            put("mediaFeatures", snapshot.mediaFeatures.toJson())
            put("biometricFeatures", snapshot.biometricFeatures.toJson())
            put("displayFeatures", snapshot.displayFeatures.toJson())
            put("behavioralFeatures", snapshot.behavioralFeatures.toJson())
        }

        return@withContext json.toString(2) // Pretty print with 2-space indent
    }

    /**
     * Compare two device capability snapshots
     *
     * @param other Other device's capability snapshot
     * @return Comparison result showing differences
     */
    suspend fun compare(other: CapabilitySnapshot): CapabilityComparison {
        val current = getSnapshot()

        return CapabilityComparison(
            thisDevice = current.deviceInfo.model,
            otherDevice = other.deviceInfo.model,
            differences = buildList {
                // Network differences
                if (current.networkFeatures.hasBluetooth != other.networkFeatures.hasBluetooth) {
                    add("Bluetooth: ${current.networkFeatures.hasBluetooth} vs ${other.networkFeatures.hasBluetooth}")
                }
                if (current.networkFeatures.hasWiFi != other.networkFeatures.hasWiFi) {
                    add("WiFi: ${current.networkFeatures.hasWiFi} vs ${other.networkFeatures.hasWiFi}")
                }
                if (current.networkFeatures.hasUwb != other.networkFeatures.hasUwb) {
                    add("UWB: ${current.networkFeatures.hasUwb} vs ${other.networkFeatures.hasUwb}")
                }
                if (current.networkFeatures.hasNfc != other.networkFeatures.hasNfc) {
                    add("NFC: ${current.networkFeatures.hasNfc} vs ${other.networkFeatures.hasNfc}")
                }

                // Sensor differences
                if (current.sensorFeatures.hasGyroscope != other.sensorFeatures.hasGyroscope) {
                    add("Gyroscope: ${current.sensorFeatures.hasGyroscope} vs ${other.sensorFeatures.hasGyroscope}")
                }
                if (current.sensorFeatures.hasMagnetometer != other.sensorFeatures.hasMagnetometer) {
                    add("Magnetometer: ${current.sensorFeatures.hasMagnetometer} vs ${other.sensorFeatures.hasMagnetometer}")
                }

                // Hardware differences
                if (current.hardwareFeatures.isTablet != other.hardwareFeatures.isTablet) {
                    add("Tablet: ${current.hardwareFeatures.isTablet} vs ${other.hardwareFeatures.isTablet}")
                }
                if (current.hardwareFeatures.isWatch != other.hardwareFeatures.isWatch) {
                    add("Watch: ${current.hardwareFeatures.isWatch} vs ${other.hardwareFeatures.isWatch}")
                }
                if (current.hardwareFeatures.isFoldable != other.hardwareFeatures.isFoldable) {
                    add("Foldable: ${current.hardwareFeatures.isFoldable} vs ${other.hardwareFeatures.isFoldable}")
                }

                // Biometric differences
                if (current.biometricFeatures.hasBiometric != other.biometricFeatures.hasBiometric) {
                    add("Biometric: ${current.biometricFeatures.hasBiometric} vs ${other.biometricFeatures.hasBiometric}")
                }
            },
            compatible = buildList {
                // List features both devices have
                if (current.networkFeatures.hasBluetooth && other.networkFeatures.hasBluetooth) add("Bluetooth")
                if (current.networkFeatures.hasWiFi && other.networkFeatures.hasWiFi) add("WiFi")
                if (current.networkFeatures.hasUwb && other.networkFeatures.hasUwb) add("UWB")
                if (current.sensorFeatures.hasGyroscope && other.sensorFeatures.hasGyroscope) add("Gyroscope")
                if (current.biometricFeatures.hasBiometric && other.biometricFeatures.hasBiometric) add("Biometric")
            }
        )
    }

    /**
     * Check if device has specific feature
     *
     * @param featureName Feature to check (e.g., "bluetooth", "nfc", "gyroscope")
     * @return True if device has the feature
     */
    suspend fun hasFeature(featureName: String): Boolean {
        val snapshot = getSnapshot()

        return when (featureName.lowercase()) {
            // Network features
            "bluetooth" -> snapshot.networkFeatures.hasBluetooth
            "bluetooth_le", "ble" -> snapshot.networkFeatures.hasBluetoothLE
            "wifi" -> snapshot.networkFeatures.hasWiFi
            "wifi_direct" -> snapshot.networkFeatures.hasWiFiDirect
            "wifi_aware" -> snapshot.networkFeatures.hasWiFiAware
            "wifi_rtt" -> snapshot.networkFeatures.hasWiFiRtt
            "uwb" -> snapshot.networkFeatures.hasUwb
            "nfc" -> snapshot.networkFeatures.hasNfc
            "cellular" -> snapshot.networkFeatures.hasCellular
            "5g" -> snapshot.networkFeatures.has5G

            // Sensor features
            "accelerometer" -> snapshot.sensorFeatures.hasAccelerometer
            "gyroscope" -> snapshot.sensorFeatures.hasGyroscope
            "magnetometer" -> snapshot.sensorFeatures.hasMagnetometer
            "barometer" -> snapshot.sensorFeatures.hasBarometer
            "proximity" -> snapshot.sensorFeatures.hasProximity
            "light" -> snapshot.sensorFeatures.hasLight
            "temperature" -> snapshot.sensorFeatures.hasTemperature
            "humidity" -> snapshot.sensorFeatures.hasHumidity
            "step_counter" -> snapshot.sensorFeatures.hasStepCounter
            "heart_rate" -> snapshot.sensorFeatures.hasHeartRate

            // Hardware features
            "camera" -> snapshot.hardwareFeatures.hasCamera
            "camera_front" -> snapshot.hardwareFeatures.hasCameraFront
            "microphone" -> snapshot.hardwareFeatures.hasMicrophone
            "fingerprint" -> snapshot.hardwareFeatures.hasFingerprint
            "face" -> snapshot.hardwareFeatures.hasFace
            "iris" -> snapshot.hardwareFeatures.hasIris

            // Device types
            "tablet" -> snapshot.hardwareFeatures.isTablet
            "watch" -> snapshot.hardwareFeatures.isWatch
            "foldable" -> snapshot.hardwareFeatures.isFoldable
            "smart_glass", "smartglass" -> snapshot.behavioralFeatures.isSmartGlass

            // GPU Acceleration (not in snapshot, from GPUCapabilities)
            "gpu", "gpu_acceleration", "rendereffect" -> GPUCapabilities.isGpuAccelerationAvailable
            "blur" -> GPUCapabilities.isBlurSupported
            "color_filter" -> GPUCapabilities.isColorFilterSupported

            else -> {
                Log.w(TAG, "Unknown feature: $featureName")
                false
            }
        }
    }

    /**
     * Get a list of all available features on this device
     *
     * @return List of feature names that are available
     */
    suspend fun getAvailableFeatures(): List<String> {
        val snapshot = getSnapshot()
        val features = mutableListOf<String>()

        // Network features
        if (snapshot.networkFeatures.hasBluetooth) features.add("Bluetooth")
        if (snapshot.networkFeatures.hasBluetoothLE) features.add("Bluetooth LE")
        if (snapshot.networkFeatures.hasWiFi) features.add("WiFi")
        if (snapshot.networkFeatures.hasWiFiDirect) features.add("WiFi Direct")
        if (snapshot.networkFeatures.hasWiFiAware) features.add("WiFi Aware")
        if (snapshot.networkFeatures.hasWiFiRtt) features.add("WiFi RTT")
        if (snapshot.networkFeatures.hasUwb) features.add("UWB")
        if (snapshot.networkFeatures.hasNfc) features.add("NFC")
        if (snapshot.networkFeatures.hasCellular) features.add("Cellular")
        if (snapshot.networkFeatures.has5G) features.add("5G")

        // Sensor features
        if (snapshot.sensorFeatures.hasAccelerometer) features.add("Accelerometer")
        if (snapshot.sensorFeatures.hasGyroscope) features.add("Gyroscope")
        if (snapshot.sensorFeatures.hasMagnetometer) features.add("Magnetometer")
        if (snapshot.sensorFeatures.hasBarometer) features.add("Barometer")
        if (snapshot.sensorFeatures.hasProximity) features.add("Proximity Sensor")
        if (snapshot.sensorFeatures.hasLight) features.add("Light Sensor")
        if (snapshot.sensorFeatures.hasTemperature) features.add("Temperature Sensor")
        if (snapshot.sensorFeatures.hasHumidity) features.add("Humidity Sensor")
        if (snapshot.sensorFeatures.hasStepCounter) features.add("Step Counter")
        if (snapshot.sensorFeatures.hasHeartRate) features.add("Heart Rate Monitor")

        // Hardware features
        if (snapshot.hardwareFeatures.hasCamera) features.add("Camera")
        if (snapshot.hardwareFeatures.hasCameraFront) features.add("Front Camera")
        if (snapshot.hardwareFeatures.hasMicrophone) features.add("Microphone")
        if (snapshot.hardwareFeatures.hasFingerprint) features.add("Fingerprint Scanner")
        if (snapshot.hardwareFeatures.hasFace) features.add("Face Recognition")
        if (snapshot.hardwareFeatures.hasIris) features.add("Iris Scanner")

        // Biometric
        if (snapshot.biometricFeatures.hasBiometric) features.add("Biometric Authentication")

        // Display
        if (snapshot.displayFeatures.isHdr) features.add("HDR Display")
        if (snapshot.displayFeatures.isWideColorGamut) features.add("Wide Color Gamut")
        if (snapshot.displayFeatures.hasXrSupport) features.add("XR Support")

        // GPU Acceleration
        if (GPUCapabilities.isGpuAccelerationAvailable) features.add("GPU Acceleration (RenderEffect)")

        return features.sorted()
    }

    /**
     * Clear the cached snapshot
     */
    fun clearCache() {
        cachedSnapshot = null
        lastUpdateTime = 0
        Log.d(TAG, "Cache cleared")
    }
}

// ===== DATA CLASSES =====

/**
 * Complete capability snapshot
 */
data class CapabilitySnapshot(
    val timestamp: Long,
    val deviceInfo: DeviceInfoSnapshot,
    val networkFeatures: NetworkFeatures,
    val sensorFeatures: SensorFeatures,
    val hardwareFeatures: HardwareFeatures,
    val mediaFeatures: MediaFeatures,
    val biometricFeatures: BiometricFeatures,
    val displayFeatures: DisplayFeatures,
    val behavioralFeatures: BehavioralFeatures
)

data class DeviceInfoSnapshot(
    val manufacturer: String,
    val model: String,
    val device: String,
    val brand: String,
    val androidVersion: Int,
    val androidVersionName: String,
    val buildId: String,
    val fingerprint: String
) {
    fun toJson() = JSONObject().apply {
        put("manufacturer", manufacturer)
        put("model", model)
        put("device", device)
        put("brand", brand)
        put("androidVersion", androidVersion)
        put("androidVersionName", androidVersionName)
        put("buildId", buildId)
        put("fingerprint", fingerprint)
    }
}

data class NetworkFeatures(
    val hasBluetooth: Boolean,
    val hasBluetoothLE: Boolean,
    val hasWiFi: Boolean,
    val hasWiFiDirect: Boolean,
    val hasWiFiAware: Boolean,
    val hasWiFiRtt: Boolean,
    val hasUwb: Boolean,
    val hasNfc: Boolean,
    val hasCellular: Boolean,
    val has5G: Boolean,
    val hasEthernet: Boolean,
    val bluetoothVersion: String?,
    val wifiStandard: String?
) {
    fun toJson() = JSONObject().apply {
        put("hasBluetooth", hasBluetooth)
        put("hasBluetoothLE", hasBluetoothLE)
        put("hasWiFi", hasWiFi)
        put("hasWiFiDirect", hasWiFiDirect)
        put("hasWiFiAware", hasWiFiAware)
        put("hasWiFiRtt", hasWiFiRtt)
        put("hasUwb", hasUwb)
        put("hasNfc", hasNfc)
        put("hasCellular", hasCellular)
        put("has5G", has5G)
        put("hasEthernet", hasEthernet)
        put("bluetoothVersion", bluetoothVersion ?: "N/A")
        put("wifiStandard", wifiStandard ?: "N/A")
    }
}

data class SensorFeatures(
    val hasAccelerometer: Boolean,
    val hasGyroscope: Boolean,
    val hasMagnetometer: Boolean,
    val hasBarometer: Boolean,
    val hasProximity: Boolean,
    val hasLight: Boolean,
    val hasTemperature: Boolean,
    val hasHumidity: Boolean,
    val hasStepCounter: Boolean,
    val hasHeartRate: Boolean,
    val hasHingeAngle: Boolean,
    val totalSensorCount: Int,
    val sensorList: List<String>
) {
    fun toJson() = JSONObject().apply {
        put("hasAccelerometer", hasAccelerometer)
        put("hasGyroscope", hasGyroscope)
        put("hasMagnetometer", hasMagnetometer)
        put("hasBarometer", hasBarometer)
        put("hasProximity", hasProximity)
        put("hasLight", hasLight)
        put("hasTemperature", hasTemperature)
        put("hasHumidity", hasHumidity)
        put("hasStepCounter", hasStepCounter)
        put("hasHeartRate", hasHeartRate)
        put("hasHingeAngle", hasHingeAngle)
        put("totalSensorCount", totalSensorCount)
        put("sensorList", JSONArray(sensorList))
    }
}

data class HardwareFeatures(
    val hasNfc: Boolean,
    val hasCamera: Boolean,
    val hasCameraFront: Boolean,
    val hasCameraFlash: Boolean,
    val hasMicrophone: Boolean,
    val hasTelephony: Boolean,
    val hasUsb: Boolean,
    val hasUsbAccessory: Boolean,
    val hasFingerprint: Boolean,
    val hasFace: Boolean,
    val hasIris: Boolean,
    val isFoldable: Boolean,
    val isTablet: Boolean,
    val isWatch: Boolean,
    val isAutomotive: Boolean,
    val isTelevision: Boolean
) {
    fun toJson() = JSONObject().apply {
        put("hasNfc", hasNfc)
        put("hasCamera", hasCamera)
        put("hasCameraFront", hasCameraFront)
        put("hasCameraFlash", hasCameraFlash)
        put("hasMicrophone", hasMicrophone)
        put("hasTelephony", hasTelephony)
        put("hasUsb", hasUsb)
        put("hasUsbAccessory", hasUsbAccessory)
        put("hasFingerprint", hasFingerprint)
        put("hasFace", hasFace)
        put("hasIris", hasIris)
        put("isFoldable", isFoldable)
        put("isTablet", isTablet)
        put("isWatch", isWatch)
        put("isAutomotive", isAutomotive)
        put("isTelevision", isTelevision)
    }
}

data class MediaFeatures(
    val hasMicrophone: Boolean,
    val hasSpeaker: Boolean,
    val hasLowLatencyAudio: Boolean,
    val hasProAudio: Boolean,
    val cameraCount: Int,
    val hasFrontCamera: Boolean,
    val hasDepthCamera: Boolean,
    val hasMultipleRearCameras: Boolean
) {
    fun toJson() = JSONObject().apply {
        put("hasMicrophone", hasMicrophone)
        put("hasSpeaker", hasSpeaker)
        put("hasLowLatencyAudio", hasLowLatencyAudio)
        put("hasProAudio", hasProAudio)
        put("cameraCount", cameraCount)
        put("hasFrontCamera", hasFrontCamera)
        put("hasDepthCamera", hasDepthCamera)
        put("hasMultipleRearCameras", hasMultipleRearCameras)
    }
}

data class BiometricFeatures(
    val hasFingerprint: Boolean,
    val hasFace: Boolean,
    val hasIris: Boolean,
    val hasBiometric: Boolean,
    val biometricLevel: String,
    val canAuthenticate: Boolean
) {
    fun toJson() = JSONObject().apply {
        put("hasFingerprint", hasFingerprint)
        put("hasFace", hasFace)
        put("hasIris", hasIris)
        put("hasBiometric", hasBiometric)
        put("biometricLevel", biometricLevel)
        put("canAuthenticate", canAuthenticate)
    }
}

data class DisplayFeatures(
    val widthPixels: Int,
    val heightPixels: Int,
    val densityDpi: Int,
    val density: Float,
    val refreshRate: Float,
    val isHdr: Boolean,
    val isWideColorGamut: Boolean,
    val isRound: Boolean,
    val hasXrSupport: Boolean
) {
    fun toJson() = JSONObject().apply {
        put("widthPixels", widthPixels)
        put("heightPixels", heightPixels)
        put("densityDpi", densityDpi)
        put("density", density)
        put("refreshRate", refreshRate)
        put("isHdr", isHdr)
        put("isWideColorGamut", isWideColorGamut)
        put("isRound", isRound)
        put("hasXrSupport", hasXrSupport)
    }
}

data class BehavioralFeatures(
    val isSmartGlass: Boolean,
    val isWearable: Boolean,
    val isAutomotive: Boolean,
    val isTelevision: Boolean,
    val isVoiceFirst: Boolean,
    val requiresLargeTouchTargets: Boolean,
    val needsBatteryOptimization: Boolean,
    val cursorStabilizationDelay: Long,
    val preferredInputMethod: String
) {
    fun toJson() = JSONObject().apply {
        put("isSmartGlass", isSmartGlass)
        put("isWearable", isWearable)
        put("isAutomotive", isAutomotive)
        put("isTelevision", isTelevision)
        put("isVoiceFirst", isVoiceFirst)
        put("requiresLargeTouchTargets", requiresLargeTouchTargets)
        put("needsBatteryOptimization", needsBatteryOptimization)
        put("cursorStabilizationDelay", cursorStabilizationDelay)
        put("preferredInputMethod", preferredInputMethod)
    }
}

data class CapabilityComparison(
    val thisDevice: String,
    val otherDevice: String,
    val differences: List<String>,
    val compatible: List<String>
)
