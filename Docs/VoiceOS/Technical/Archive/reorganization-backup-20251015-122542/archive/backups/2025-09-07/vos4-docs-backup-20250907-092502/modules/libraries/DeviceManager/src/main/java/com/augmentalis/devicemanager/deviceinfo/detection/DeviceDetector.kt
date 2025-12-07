/**
 * DeviceDetector.kt
 * Path: /deviceinfo/detection/DeviceDetector.kt
 * 
 * Purpose: Centralized device detection - ALL detection logic in one place
 * Managers should ONLY implement features, NOT detect capabilities
 * 
 * Architecture:
 * - This file orchestrates ALL detection
 * - Individual managers receive capabilities via constructor
 * - Reduces code duplication and ensures consistency
 * - Enables conditional loading based on detected features
 */

package com.augmentalis.devicemanager.deviceinfo.detection

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.biometrics.BiometricManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.io.File

/**
 * Device Detector - Centralized detection for ALL device capabilities
 * 
 * Single source of truth for:
 * - Hardware capabilities
 * - Network features
 * - Sensor availability
 * - Audio/Video capabilities
 * - Biometric support
 * - Manufacturer features
 * 
 * Managers receive capabilities, NEVER detect
 */
object DeviceDetector {
    
    private const val TAG = "DeviceDetector"
    private const val CACHE_FILE = "device_capabilities.json"
    private const val CACHE_VERSION = 2
    
    // Cache for expensive detections
    private var cachedCapabilities: DeviceCapabilities? = null
    
    /**
     * Get complete device capabilities
     * This is the main entry point for ALL detection
     */
    fun getCapabilities(context: Context, forceRefresh: Boolean = false): DeviceCapabilities {
        if (!forceRefresh && cachedCapabilities != null) {
            return cachedCapabilities!!
        }
        
        val capabilities = DeviceCapabilities(
            // Basic device info
            deviceInfo = detectDeviceInfo(),
            
            // Hardware capabilities
            hardware = detectHardwareCapabilities(context),
            
            // Network capabilities
            network = detectNetworkCapabilities(context),
            
            // Bluetooth capabilities
            bluetooth = detectBluetoothCapabilities(context),
            
            // WiFi capabilities
            wifi = detectWiFiCapabilities(context),
            
            // Camera/Video capabilities
            camera = detectCameraCapabilities(context),
            
            // Audio capabilities
            audio = detectAudioCapabilities(context),
            
            // Sensor capabilities
            sensors = detectSensorCapabilities(context),
            
            // Biometric capabilities
            biometric = detectBiometricCapabilities(context),
            
            // Display capabilities
            display = detectDisplayCapabilities(context),
            
            // Behavioral capabilities
            behavioral = detectBehavioralCapabilities(context),
            
            // Integration requirements
            integration = detectIntegrationRequirements(context)
        )
        
        cachedCapabilities = capabilities
        return capabilities
    }
    
    /**
     * Clear cached capabilities
     */
    fun clearCache() {
        cachedCapabilities = null
    }
    
    // ===== DEVICE INFO =====
    
    private fun detectDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            device = Build.DEVICE,
            brand = Build.BRAND,
            androidVersion = Build.VERSION.SDK_INT,
            androidVersionName = Build.VERSION.RELEASE,
            buildId = Build.ID,
            buildFingerprint = Build.FINGERPRINT
        )
    }
    
    // ===== HARDWARE CAPABILITIES =====
    
    private fun detectHardwareCapabilities(context: Context): HardwareCapabilities {
        val pm = context.packageManager
        
        return HardwareCapabilities(
            hasNfc = detectNfc(context),
            hasCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY),
            hasCameraFront = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT),
            hasCameraFlash = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH),
            hasMicrophone = pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE),
            hasTelephony = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY),
            hasUsb = pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST),
            hasUsbAccessory = pm.hasSystemFeature(PackageManager.FEATURE_USB_ACCESSORY),
            hasFingerprint = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT),
            hasFace = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                pm.hasSystemFeature(PackageManager.FEATURE_FACE)
            } else false,
            hasIris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                pm.hasSystemFeature(PackageManager.FEATURE_IRIS)
            } else false,
            isFoldable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                pm.hasSystemFeature("android.hardware.type.foldable")
            } else false,
            isTablet = (context.resources.configuration.screenLayout and 
                       android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK) >= 
                       android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE,
            isWatch = pm.hasSystemFeature("android.hardware.type.watch"),
            isAutomotive = pm.hasSystemFeature("android.hardware.type.automotive"),
            isTelevision = pm.hasSystemFeature("android.hardware.type.television"),
            isEmbedded = pm.hasSystemFeature("android.hardware.type.embedded")
        )
    }
    
    // ===== NETWORK CAPABILITIES =====
    
    private fun detectNetworkCapabilities(context: Context): NetworkCapabilities {
        val pm = context.packageManager
        
        return NetworkCapabilities(
            hasBluetooth = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH),
            hasBluetoothLE = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE),
            hasWiFi = pm.hasSystemFeature(PackageManager.FEATURE_WIFI),
            hasWiFiDirect = pm.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT),
            hasWiFiAware = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                pm.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)
            } else false,
            hasWiFiRtt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.hasSystemFeature(PackageManager.FEATURE_WIFI_RTT)
            } else false,
            hasWiFiPasspoint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                pm.hasSystemFeature("android.software.wifi.passpoint")
            } else false,
            hasUwb = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                pm.hasSystemFeature(PackageManager.FEATURE_UWB)
            } else false,
            hasNfc = detectNfc(context),
            hasCellular = pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY),
            has5G = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                detectCellular5G(context)
            } else false,
            hasEthernet = pm.hasSystemFeature("android.hardware.ethernet")
        )
    }
    
    // ===== BLUETOOTH CAPABILITIES =====
    
    @SuppressLint("MissingPermission")
    private fun detectBluetoothCapabilities(context: Context): BluetoothCapabilities? {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            return null
        }
        
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bluetoothManager?.adapter ?: return null
        
        return BluetoothCapabilities(
            hasClassic = true,
            hasBLE = context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE),
            bluetoothVersion = detectBluetoothVersion(),
            supportedProfiles = detectBluetoothProfiles(),
            supportedCodecs = detectBluetoothCodecs(),
            dualMode = adapter.isEnabled && context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE),
            multipleAdvertisement = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                adapter.isMultipleAdvertisementSupported
            } else false,
            offloadedFiltering = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                adapter.isOffloadedFilteringSupported
            } else false,
            offloadedScanBatching = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                adapter.isOffloadedScanBatchingSupported
            } else false,
            extendedAdvertising = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                adapter.isLe2MPhySupported
            } else false,
            le2MPhy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                adapter.isLe2MPhySupported
            } else false,
            leCodedPhy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                adapter.isLeCodedPhySupported
            } else false,
            leExtendedAdvertising = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                adapter.isLeExtendedAdvertisingSupported
            } else false,
            lePeriodicAdvertising = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                adapter.isLePeriodicAdvertisingSupported
            } else false,
            leAudio = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        )
    }
    
    private fun detectBluetoothVersion(): String {
        return when {
            Build.VERSION.SDK_INT >= 33 -> "5.3"
            Build.VERSION.SDK_INT >= 31 -> "5.2"
            Build.VERSION.SDK_INT >= 29 -> "5.1"
            Build.VERSION.SDK_INT >= 26 -> "5.0"
            Build.VERSION.SDK_INT >= 23 -> "4.2"
            Build.VERSION.SDK_INT >= 21 -> "4.1"
            Build.VERSION.SDK_INT >= 18 -> "4.0"
            else -> "3.0"
        }
    }
    
    private fun detectBluetoothProfiles(): List<String> {
        val profiles = mutableListOf<String>()
        
        profiles.add("A2DP")
        profiles.add("HEADSET")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            profiles.add("GATT")
            profiles.add("GATT_SERVER")
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            profiles.add("HID")
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            profiles.add("SAP")
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            profiles.add("HEARING_AID")
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            profiles.add("LE_AUDIO")
            profiles.add("HAP")
        }
        
        return profiles
    }
    
    private fun detectBluetoothCodecs(): List<String> {
        val codecs = mutableListOf("SBC") // Mandatory codec
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            codecs.add("AAC")
        }
        
        // Manufacturer-specific codecs
        val manufacturer = Build.MANUFACTURER.lowercase()
        
        when {
            manufacturer.contains("sony") -> {
                codecs.add("LDAC")
            }
            manufacturer.contains("samsung") -> {
                codecs.add("Samsung HD")
                codecs.add("Scalable Codec")
            }
            manufacturer.contains("qualcomm") || 
            Build.BOARD.lowercase().contains("msm") || 
            Build.BOARD.lowercase().contains("sdm") -> {
                codecs.add("aptX")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    codecs.add("aptX HD")
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    codecs.add("aptX Adaptive")
                    codecs.add("aptX TWS+")
                }
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                codecs.add("HWA")
                codecs.add("LHDC")
            }
        }
        
        // LE Audio codec
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            codecs.add("LC3")
        }
        
        return codecs
    }
    
    // ===== WIFI CAPABILITIES =====
    
    private fun detectWiFiCapabilities(context: Context): WiFiCapabilities? {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)) {
            return null
        }
        
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return null
        
        return WiFiCapabilities(
            isEnabled = wifiManager.isWifiEnabled,
            is5GHzSupported = wifiManager.is5GHzBandSupported,
            is6GHzSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                wifiManager.is6GHzBandSupported
            } else false,
            is60GHzSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                wifiManager.is60GHzBandSupported
            } else false,
            isWiFi6Supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
            isWiFi6ESupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                wifiManager.is6GHzBandSupported
            } else false,
            isWiFi7Supported = Build.VERSION.SDK_INT >= 34, // Android 14+
            wifiStandard = detectWiFiStandard(context, wifiManager),
            isDualBandSupported = wifiManager.is5GHzBandSupported,
            isTdlsSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                wifiManager.isTdlsSupported
            } else false,
            isP2pSupported = wifiManager.isP2pSupported,
            isPreferredNetworkOffloadSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                wifiManager.isPreferredNetworkOffloadSupported
            } else false,
            isEasyConnectSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                wifiManager.isEasyConnectSupported
            } else false,
            isWpa3SaeSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                wifiManager.isWpa3SaeSupported
            } else false,
            isWpa3SuiteBSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                wifiManager.isWpa3SuiteBSupported
            } else false,
            isEnhancedOpenSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                wifiManager.isEnhancedOpenSupported
            } else false,
            maxNumberOfNetworkSuggestions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                wifiManager.maxNumberOfNetworkSuggestionsPerApp
            } else 0
        )
    }
    
    private fun detectWiFiStandard(context: Context, wifiManager: WifiManager): String {
        return when {
            Build.VERSION.SDK_INT >= 34 && 
            context.packageManager.hasSystemFeature("android.hardware.wifi.be") -> "WiFi 7 (802.11be)"
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && 
            wifiManager.is6GHzBandSupported -> "WiFi 6E (802.11ax)"
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> "WiFi 6 (802.11ax)"
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> "WiFi 5 (802.11ac)"
            wifiManager.is5GHzBandSupported -> "WiFi 5 (802.11ac)"
            else -> "WiFi 4 (802.11n)"
        }
    }
    
    // ===== CAMERA CAPABILITIES =====
    
    private fun detectCameraCapabilities(context: Context): CameraCapabilities? {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            return null
        }
        
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return null
        
        try {
            val cameraIds = cameraManager.cameraIdList
            val cameras = mutableListOf<CameraInfo>()
            
            for (cameraId in cameraIds) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                
                cameras.add(CameraInfo(
                    cameraId = cameraId,
                    facing = when (facing) {
                        CameraCharacteristics.LENS_FACING_BACK -> "BACK"
                        CameraCharacteristics.LENS_FACING_FRONT -> "FRONT"
                        CameraCharacteristics.LENS_FACING_EXTERNAL -> "EXTERNAL"
                        else -> "UNKNOWN"
                    },
                    hardwareLevel = when (characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)) {
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "LEGACY"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "LIMITED"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "FULL"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "LEVEL_3"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "EXTERNAL"
                        else -> "UNKNOWN"
                    },
                    hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false,
                    hasAutofocus = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)
                        ?.contains(CameraCharacteristics.CONTROL_AF_MODE_AUTO) ?: false
                ))
            }
            
            val frontCameras = cameras.filter { it.facing == "FRONT" }
            val rearCameras = cameras.filter { it.facing == "BACK" }
            
            return CameraCapabilities(
                cameraCount = cameraIds.size,
                cameras = cameras,
                hasDepthCamera = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cameras.any { camera ->
                        val chars = cameraManager.getCameraCharacteristics(camera.cameraId)
                        chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                            ?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT) ?: false
                    }
                } else false,
                hasLogicalMultiCamera = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    cameras.any { camera ->
                        val chars = cameraManager.getCameraCharacteristics(camera.cameraId)
                        chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                            ?.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA) ?: false
                    }
                } else false,
                hasFrontCamera = frontCameras.isNotEmpty(),
                hasMultipleRearCameras = rearCameras.size > 1
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting camera capabilities", e)
            return null
        }
    }
    
    // ===== AUDIO CAPABILITIES =====
    
    private fun detectAudioCapabilities(context: Context): AudioCapabilities {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val pm = context.packageManager
        
        return AudioCapabilities(
            hasMicrophone = pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE),
            hasSpeaker = true, // Almost all devices have speakers
            hasLowLatencyAudio = pm.hasSystemFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY),
            hasProAudio = pm.hasSystemFeature(PackageManager.FEATURE_AUDIO_PRO),
            hasAudioOutput = pm.hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT),
            hasMidi = pm.hasSystemFeature(PackageManager.FEATURE_MIDI),
            supportedOutputSampleRates = detectSupportedSampleRates(audioManager),
            supportedInputSampleRates = if (pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)) {
                detectSupportedSampleRates(audioManager)
            } else emptyList()
        )
    }
    
    private fun detectSupportedSampleRates(audioManager: AudioManager): List<Int> {
        val rates = mutableListOf<Int>()
        
        // Common sample rates
        val commonRates = listOf(8000, 11025, 16000, 22050, 44100, 48000, 88200, 96000, 176400, 192000)
        
        // Get native sample rate
        val nativeRate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE)?.toIntOrNull()
        if (nativeRate != null && !rates.contains(nativeRate)) {
            rates.add(nativeRate)
        }
        
        // Add common rates (most Android devices support these)
        rates.addAll(commonRates.filter { it <= (nativeRate ?: 48000) })
        
        return rates.sorted()
    }
    
    // ===== SENSOR CAPABILITIES =====
    
    private fun detectSensorCapabilities(context: Context): SensorCapabilities {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        
        if (sensorManager == null) {
            return SensorCapabilities()
        }
        
        val allSensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        
        return SensorCapabilities(
            hasAccelerometer = hasSensor(sensorManager, Sensor.TYPE_ACCELEROMETER),
            hasGyroscope = hasSensor(sensorManager, Sensor.TYPE_GYROSCOPE),
            hasMagnetometer = hasSensor(sensorManager, Sensor.TYPE_MAGNETIC_FIELD),
            hasBarometer = hasSensor(sensorManager, Sensor.TYPE_PRESSURE),
            hasProximity = hasSensor(sensorManager, Sensor.TYPE_PROXIMITY),
            hasLight = hasSensor(sensorManager, Sensor.TYPE_LIGHT),
            hasTemperature = hasSensor(sensorManager, Sensor.TYPE_AMBIENT_TEMPERATURE),
            hasHumidity = hasSensor(sensorManager, Sensor.TYPE_RELATIVE_HUMIDITY),
            hasStepCounter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                hasSensor(sensorManager, Sensor.TYPE_STEP_COUNTER)
            } else false,
            hasStepDetector = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                hasSensor(sensorManager, Sensor.TYPE_STEP_DETECTOR)
            } else false,
            hasHeartRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                hasSensor(sensorManager, Sensor.TYPE_HEART_RATE)
            } else false,
            hasHeartBeat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                hasSensor(sensorManager, Sensor.TYPE_HEART_BEAT)
            } else false,
            hasLowLatencyOffBodyDetect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                hasSensor(sensorManager, Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT)
            } else false,
            hasHingeAngle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                hasSensor(sensorManager, Sensor.TYPE_HINGE_ANGLE)
            } else false,
            totalSensorCount = allSensors.size,
            sensorList = allSensors.map { it.name }
        )
    }
    
    private fun hasSensor(sensorManager: SensorManager, sensorType: Int): Boolean {
        return sensorManager.getDefaultSensor(sensorType) != null
    }
    
    // ===== BIOMETRIC CAPABILITIES =====
    
    private fun detectBiometricCapabilities(context: Context): BiometricCapabilities {
        val pm = context.packageManager
        
        val hasFingerprint = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        val hasFace = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pm.hasSystemFeature(PackageManager.FEATURE_FACE)
        } else false
        val hasIris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            pm.hasSystemFeature(PackageManager.FEATURE_IRIS)
        } else false
        
        val biometricLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val biometricManager = context.getSystemService(Context.BIOMETRIC_SERVICE) as? BiometricManager
            when (biometricManager?.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
                BiometricManager.BIOMETRIC_SUCCESS -> "STRONG"
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "AVAILABLE_NOT_ENROLLED"
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "NO_HARDWARE"
                else -> "WEAK"
            }
        } else {
            if (hasFingerprint) "STRONG" else "NO_HARDWARE"
        }
        
        return BiometricCapabilities(
            hasFingerprint = hasFingerprint,
            hasFace = hasFace,
            hasIris = hasIris,
            hasBiometric = hasFingerprint || hasFace || hasIris,
            biometricLevel = biometricLevel,
            canAuthenticateWithBiometrics = biometricLevel == "STRONG" || biometricLevel == "WEAK",
            canAuthenticateWithDeviceCredential = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val biometricManager = context.getSystemService(Context.BIOMETRIC_SERVICE) as? BiometricManager
                biometricManager?.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) == 
                    BiometricManager.BIOMETRIC_SUCCESS
            } else true
        )
    }
    
    // ===== DISPLAY CAPABILITIES =====
    
    private fun detectDisplayCapabilities(context: Context): DisplayCapabilities {
        val display = context.resources.displayMetrics
        val configuration = context.resources.configuration
        val pm = context.packageManager
        
        // Detect XR support
        val hasXrSupport = pm.hasSystemFeature("android.hardware.camera.ar") || 
                          pm.hasSystemFeature("android.hardware.vr.headtracking") ||
                          pm.hasSystemFeature("android.software.vr.mode") ||
                          pm.hasSystemFeature("android.hardware.vulkan.level") ||
                          detectSmartGlass() // Smart glasses often have XR capabilities
        
        return DisplayCapabilities(
            widthPixels = display.widthPixels,
            heightPixels = display.heightPixels,
            densityDpi = display.densityDpi,
            density = display.density,
            scaledDensity = display.scaledDensity,
            xdpi = display.xdpi,
            ydpi = display.ydpi,
            refreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display?.refreshRate ?: 60f
            } else 60f,
            isHdr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                configuration.isScreenHdr
            } else false,
            isWideColorGamut = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                configuration.isScreenWideColorGamut
            } else false,
            isRound = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                configuration.isScreenRound
            } else false,
            supportedModes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.display?.supportedModes?.map { 
                    "${it.physicalWidth}x${it.physicalHeight}@${it.refreshRate}Hz"
                } ?: emptyList()
            } else emptyList(),
            hasXrSupport = hasXrSupport
        )
    }
    
    // ===== BEHAVIORAL CAPABILITIES =====
    
    private fun detectBehavioralCapabilities(context: Context): BehavioralCapabilities {
        val isSmartGlass = detectSmartGlass()
        val isWearable = context.packageManager.hasSystemFeature("android.hardware.type.watch")
        val isAutomotive = context.packageManager.hasSystemFeature("android.hardware.type.automotive")
        val isTelevision = context.packageManager.hasSystemFeature("android.hardware.type.television")
        
        return BehavioralCapabilities(
            isSmartGlass = isSmartGlass,
            isWearable = isWearable,
            isAutomotive = isAutomotive,
            isTelevision = isTelevision,
            isVoiceFirst = isSmartGlass || isAutomotive,
            requiresLargeTouchTargets = isSmartGlass || isAutomotive || isTelevision,
            needsBatteryOptimization = isSmartGlass || isWearable,
            cursorStabilizationDelay = if (isSmartGlass) 800L else 0L,
            preferredInputMethod = when {
                isSmartGlass -> "VOICE"
                isAutomotive -> "VOICE_AND_TOUCH"
                isTelevision -> "REMOTE"
                isWearable -> "TOUCH"
                else -> "TOUCH"
            }
        )
    }
    
    private fun detectSmartGlass(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.lowercase()
        val device = Build.DEVICE?.lowercase() ?: ""
        
        return manufacturer.contains("vuzix") ||
               manufacturer.contains("realwear") ||
               manufacturer.contains("google") && model.contains("glass") ||
               manufacturer.contains("epson") && model.contains("moverio") ||
               manufacturer.contains("rokid") ||
               device.contains("hmt") ||
               device.contains("blade") ||
               model.contains("glass")
    }
    
    // ===== INTEGRATION REQUIREMENTS =====
    
    private fun detectIntegrationRequirements(context: Context): IntegrationRequirements {
        // This would load from device_integration.json
        // For now, returning basic detection
        val manufacturer = Build.MANUFACTURER.lowercase()
        
        return IntegrationRequirements(
            speechSystem = when {
                manufacturer.contains("realwear") -> "WearHF"
                manufacturer.contains("vuzix") -> "VuzixSpeechSDK"
                manufacturer.contains("samsung") -> "Bixby"
                else -> "Android"
            },
            requiresDisableSpeech = manufacturer.contains("realwear"),
            displayName = Build.MANUFACTURER.capitalize(),
            logoPath = "logos/${manufacturer}.png"
        )
    }
    
    // ===== HELPER METHODS =====
    
    private fun detectNfc(context: Context): Boolean {
        return try {
            val nfcManager = context.getSystemService(Context.NFC_SERVICE) as? NfcManager
            nfcManager?.defaultAdapter != null
        } catch (e: Exception) {
            false
        }
    }
    
    private fun detectCellular5G(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                // Check for 5G network type
                val networkType = telephonyManager?.dataNetworkType
                networkType == 20 // TelephonyManager.NETWORK_TYPE_NR
            } else false
        } catch (e: Exception) {
            false
        }
    }
    
    // ===== DATA CLASSES =====
    
    /**
     * Complete device capabilities
     */
    data class DeviceCapabilities(
        val deviceInfo: DeviceInfo,
        val hardware: HardwareCapabilities,
        val network: NetworkCapabilities,
        val bluetooth: BluetoothCapabilities?,
        val wifi: WiFiCapabilities?,
        val camera: CameraCapabilities?,
        val audio: AudioCapabilities,
        val sensors: SensorCapabilities,
        val biometric: BiometricCapabilities,
        val display: DisplayCapabilities,
        val behavioral: BehavioralCapabilities,
        val integration: IntegrationRequirements
    )
    
    data class DeviceInfo(
        val manufacturer: String,
        val model: String,
        val device: String,
        val brand: String,
        val androidVersion: Int,
        val androidVersionName: String,
        val buildId: String,
        val buildFingerprint: String
    )
    
    data class HardwareCapabilities(
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
        val isTelevision: Boolean,
        val isEmbedded: Boolean
    )
    
    data class NetworkCapabilities(
        val hasBluetooth: Boolean,
        val hasBluetoothLE: Boolean,
        val hasWiFi: Boolean,
        val hasWiFiDirect: Boolean,
        val hasWiFiAware: Boolean,
        val hasWiFiRtt: Boolean,
        val hasWiFiPasspoint: Boolean,
        val hasUwb: Boolean,
        val hasNfc: Boolean,
        val hasCellular: Boolean,
        val has5G: Boolean,
        val hasEthernet: Boolean
    )
    
    data class BluetoothCapabilities(
        val hasClassic: Boolean,
        val hasBLE: Boolean,
        val bluetoothVersion: String,
        val supportedProfiles: List<String>,
        val supportedCodecs: List<String>,
        val dualMode: Boolean,
        val multipleAdvertisement: Boolean,
        val offloadedFiltering: Boolean,
        val offloadedScanBatching: Boolean,
        val extendedAdvertising: Boolean,
        val le2MPhy: Boolean,
        val leCodedPhy: Boolean,
        val leExtendedAdvertising: Boolean,
        val lePeriodicAdvertising: Boolean,
        val leAudio: Boolean
    )
    
    data class WiFiCapabilities(
        val isEnabled: Boolean,
        val is5GHzSupported: Boolean,
        val is6GHzSupported: Boolean,
        val is60GHzSupported: Boolean,
        val isWiFi6Supported: Boolean,
        val isWiFi6ESupported: Boolean,
        val isWiFi7Supported: Boolean,
        val wifiStandard: String,
        val isDualBandSupported: Boolean,
        val isTdlsSupported: Boolean,
        val isP2pSupported: Boolean,
        val isPreferredNetworkOffloadSupported: Boolean,
        val isEasyConnectSupported: Boolean,
        val isWpa3SaeSupported: Boolean,
        val isWpa3SuiteBSupported: Boolean,
        val isEnhancedOpenSupported: Boolean,
        val maxNumberOfNetworkSuggestions: Int
    )
    
    data class CameraCapabilities(
        val cameraCount: Int,
        val cameras: List<CameraInfo>,
        val hasDepthCamera: Boolean,
        val hasLogicalMultiCamera: Boolean,
        val hasFrontCamera: Boolean = false,
        val hasMultipleRearCameras: Boolean = false
    )
    
    data class CameraInfo(
        val cameraId: String,
        val facing: String,
        val hardwareLevel: String,
        val hasFlash: Boolean,
        val hasAutofocus: Boolean
    )
    
    data class AudioCapabilities(
        val hasMicrophone: Boolean,
        val hasSpeaker: Boolean,
        val hasLowLatencyAudio: Boolean,
        val hasProAudio: Boolean,
        val hasAudioOutput: Boolean,
        val hasMidi: Boolean,
        val supportedOutputSampleRates: List<Int>,
        val supportedInputSampleRates: List<Int>
    )
    
    data class SensorCapabilities(
        val hasAccelerometer: Boolean = false,
        val hasGyroscope: Boolean = false,
        val hasMagnetometer: Boolean = false,
        val hasBarometer: Boolean = false,
        val hasProximity: Boolean = false,
        val hasLight: Boolean = false,
        val hasTemperature: Boolean = false,
        val hasHumidity: Boolean = false,
        val hasStepCounter: Boolean = false,
        val hasStepDetector: Boolean = false,
        val hasHeartRate: Boolean = false,
        val hasHeartBeat: Boolean = false,
        val hasLowLatencyOffBodyDetect: Boolean = false,
        val hasHingeAngle: Boolean = false,
        val totalSensorCount: Int = 0,
        val sensorList: List<String> = emptyList()
    )
    
    data class BiometricCapabilities(
        val hasFingerprint: Boolean,
        val hasFace: Boolean,
        val hasIris: Boolean,
        val hasBiometric: Boolean,
        val biometricLevel: String,
        val canAuthenticateWithBiometrics: Boolean,
        val canAuthenticateWithDeviceCredential: Boolean
    )
    
    data class DisplayCapabilities(
        val widthPixels: Int,
        val heightPixels: Int,
        val densityDpi: Int,
        val density: Float,
        val scaledDensity: Float,
        val xdpi: Float,
        val ydpi: Float,
        val refreshRate: Float,
        val isHdr: Boolean,
        val isWideColorGamut: Boolean,
        val isRound: Boolean,
        val supportedModes: List<String>,
        val hasXrSupport: Boolean = false
    )
    
    data class BehavioralCapabilities(
        val isSmartGlass: Boolean,
        val isWearable: Boolean,
        val isAutomotive: Boolean,
        val isTelevision: Boolean,
        val isVoiceFirst: Boolean,
        val requiresLargeTouchTargets: Boolean,
        val needsBatteryOptimization: Boolean,
        val cursorStabilizationDelay: Long,
        val preferredInputMethod: String
    )
    
    data class IntegrationRequirements(
        val speechSystem: String,
        val requiresDisableSpeech: Boolean,
        val displayName: String,
        val logoPath: String
    )
}