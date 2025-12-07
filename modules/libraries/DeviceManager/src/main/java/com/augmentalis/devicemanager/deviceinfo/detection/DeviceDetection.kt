// Author: Manoj Jhawar
// Purpose: Comprehensive device detection with manufacturer SDKs and corrections

package com.augmentalis.devicemanager.deviceinfo.detection

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.WindowManager
import java.io.File
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Comprehensive device detection with 200+ properties
 * Includes enterprise AR glasses, foldables, and manufacturer-specific features
 */
class DeviceDetection(private val context: Context) {
    
    companion object {
        private const val TAG = "DeviceDetection"
        
        // AR Glasses Manufacturers
        private const val MANUFACTURER_REALWEAR = "realwear"
        private const val MANUFACTURER_VUZIX = "vuzix"
        private const val MANUFACTURER_ROKID = "rokid"
        private const val MANUFACTURER_XREAL = "xreal"
        private const val MANUFACTURER_NREAL = "nreal"
        private const val MANUFACTURER_VIRTURE = "virture"
        private const val MANUFACTURER_EVEN = "even"
        private const val MANUFACTURER_ALMER = "almer"
        
        fun getMarketName(): String {
            // Try to get marketing name from system properties
            return try {
                val clazz = Class.forName("android.os.SystemProperties")
                val method = clazz.getMethod("get", String::class.java)
                method.invoke(null, "ro.config.marketing_name") as? String ?: Build.MODEL
            } catch (e: Exception) {
                Build.MODEL
            }
        }
        
        fun getKernelVersion(): String? {
            return try {
                val reader = BufferedReader(InputStreamReader(Runtime.getRuntime().exec("uname -r").inputStream))
                reader.readLine()
            } catch (e: Exception) {
                null
            }
        }
        
        fun getSecurityPatchLevel(): String? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Build.VERSION.SECURITY_PATCH
            } else null
        }
        
        fun getBasebandVersion(): String? {
            return try {
                val clazz = Class.forName("android.os.SystemProperties")
                val method = clazz.getMethod("get", String::class.java)
                method.invoke(null, "gsm.version.baseband") as? String
            } catch (e: Exception) {
                Build.getRadioVersion()
            }
        }
        
        fun getSELinuxStatus(): String {
            return try {
                val process = Runtime.getRuntime().exec("getenforce")
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                reader.readLine() ?: "Unknown"
            } catch (e: Exception) {
                "Unknown"
            }
        }
        
        fun detectRootStatus(): RootStatus {
            val checkPaths = listOf(
                "/system/app/Superuser.apk",
                "/sbin/su",
                "/system/bin/su",
                "/system/xbin/su",
                "/data/local/xbin/su",
                "/data/local/bin/su",
                "/system/sd/xbin/su",
                "/system/bin/failsafe/su",
                "/data/local/su"
            )
            
            for (path in checkPaths) {
                if (File(path).exists()) {
                    return RootStatus.ROOTED
                }
            }
            
            return try {
                Runtime.getRuntime().exec("su")
                RootStatus.ROOTED
            } catch (e: Exception) {
                RootStatus.NOT_ROOTED
            }
        }
        
        fun detectPanelType(): PanelType {
            val model = Build.MODEL.lowercase()
            
            return when {
                model.contains("galaxy s2") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> PanelType.LTPO
                model.contains("iphone 13 pro") || model.contains("iphone 14 pro") -> PanelType.LTPO
                model.contains("oneplus") && model.contains("pro") -> PanelType.AMOLED
                else -> PanelType.LCD
            }
        }
        
        fun detectTouchType(): TouchType {
            val manufacturer = Build.MANUFACTURER.lowercase()
            
            return when {
                manufacturer == "apple" -> TouchType.PROJECTED_CAPACITIVE
                manufacturer == "samsung" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> TouchType.PROJECTED_CAPACITIVE
                else -> TouchType.CAPACITIVE
            }
        }
        
        fun getMultiTouchPoints(): Int {
            // Most modern devices support 10 points
            return 10
        }
    }
    
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
    private val packageManager = context.packageManager
    
    // ========== EXTENDED DEVICE PROFILE ==========
    
    data class ExtendedDeviceProfile(
        // Basic Info
        val manufacturer: String = Build.MANUFACTURER,
        val model: String = Build.MODEL,
        val brand: String = Build.BRAND,
        val device: String = Build.DEVICE,
        val product: String = Build.PRODUCT,
        val codename: String = Build.DEVICE, // Often same as device
        val marketName: String = getMarketName(),
        
        // System Info
        val androidVersion: String = Build.VERSION.RELEASE,
        val apiLevel: Int = Build.VERSION.SDK_INT,
        val kernelVersion: String? = getKernelVersion(),
        val securityPatchLevel: String? = getSecurityPatchLevel(),
        val bootloaderVersion: String? = Build.BOOTLOADER,
        val basebandVersion: String? = getBasebandVersion(),
        val selinuxStatus: String = getSELinuxStatus(),
        val rootStatus: RootStatus = detectRootStatus(),
        
        // Build Info
        val buildId: String = Build.ID,
        val buildType: String = Build.TYPE,
        val buildTags: String = Build.TAGS,
        val buildFingerprint: String = Build.FINGERPRINT,
        val buildUser: String? = Build.USER,
        val buildHost: String? = Build.HOST,
        val buildTime: Long = Build.TIME,
        val incrementalVersion: String = Build.VERSION.INCREMENTAL,
        val boardName: String? = Build.BOARD,
        val hardware: String? = Build.HARDWARE,
        
        // Regional Info
        val language: String,
        val country: String,
        val timezone: String
    )
    
    enum class RootStatus {
        ROOTED, NOT_ROOTED, POSSIBLY_ROOTED, UNKNOWN
    }
    
    // ========== EXTENDED DISPLAY PROFILE ==========
    
    data class ExtendedDisplayProfile(
        // Physical Characteristics
        val panelType: PanelType = detectPanelType(),
        val touchTechnology: TouchType = detectTouchType(),
        val multiTouchPoints: Int = getMultiTouchPoints(),
        
        // Metrics
        val widthPixels: Int,
        val heightPixels: Int,
        val physicalWidthMm: Float,
        val physicalHeightMm: Float,
        val pixelDensity: Float,
        val densityDpi: Int,
        val aspectRatio: AspectRatio,
        val diagonalInches: Float,
        
        // Capabilities
        val refreshRate: Float,
        val supportedRefreshRates: List<Float>,
        val colorDepth: Int = 8, // Default, actual detection complex
        val colorSpace: List<ColorSpace>,
        val hdrCapabilities: List<HDRType>,
        val variableRefreshRate: Boolean,
        
        // Features
        val hasAlwaysOnDisplay: Boolean,
        val hasBlueLightFilter: Boolean,
        val hasOutdoorMode: Boolean,
        val hasNotch: Boolean,
        val hasPunchHole: Boolean,
        val hasUnderDisplayCamera: Boolean,
        val hasUnderDisplayFingerprint: Boolean,
        val cutoutDetails: DisplayCutoutInfo?,
        val roundedCornerRadius: Float
    )
    
    enum class PanelType {
        OLED, AMOLED, SUPER_AMOLED, LCD, IPS, TFT, E_INK, MICRO_OLED, LTPO, UNKNOWN
    }
    
    enum class TouchType {
        CAPACITIVE, RESISTIVE, INFRARED, OPTICAL, PROJECTED_CAPACITIVE, NONE, UNKNOWN
    }
    
    enum class AspectRatio(val value: String) {
        RATIO_16_9("16:9"),
        RATIO_18_9("18:9"),
        RATIO_19_9("19:9"),
        RATIO_20_9("20:9"),
        RATIO_21_9("21:9"),
        RATIO_4_3("4:3"),
        RATIO_3_2("3:2"),
        CUSTOM("Custom")
    }
    
    enum class ColorSpace {
        SRGB, DCI_P3, ADOBE_RGB, DISPLAY_P3, BT2020, BT709
    }
    
    enum class HDRType {
        HDR10, HDR10_PLUS, DOLBY_VISION, HLG, HDR_VIVID
    }
    
    data class DisplayCutoutInfo(
        val bounds: List<Rect>,
        val safeInsets: Insets,
        val type: CutoutType
    )
    
    enum class CutoutType {
        NOTCH, PUNCH_HOLE, WATERDROP, DYNAMIC_ISLAND, NONE
    }
    
    data class Rect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        fun width(): Int = right - left
        fun height(): Int = bottom - top
    }
    data class Insets(val left: Int, val top: Int, val right: Int, val bottom: Int)
    
    // ========== EXTENDED SENSOR INFO ==========
    
    data class ExtendedSensorInfo(
        val name: String,
        val vendor: String,
        val version: Int,
        val type: Int,
        val typeString: String,
        val maxRange: Float,
        val resolution: Float,
        val power: Float, // mA
        val minDelay: Int, // microseconds
        val maxDelay: Int,
        val reportingMode: ReportingMode,
        val isWakeUp: Boolean,
        val isDynamic: Boolean,
        val additionalInfo: String?,
        val placement: SensorPlacement?
    )
    
    enum class ReportingMode {
        CONTINUOUS, ON_CHANGE, ONE_SHOT, SPECIAL_TRIGGER
    }
    
    enum class SensorPlacement {
        FRONT, BACK, TOP, BOTTOM, LEFT, RIGHT, INTERNAL, EXTERNAL, UNKNOWN
    }
    
    // ========== ENTERPRISE AR GLASSES DETECTION ==========
    
    data class ARGlassesInfo(
        val manufacturer: String,
        val model: String,
        val type: ARGlassesType,
        val features: ARGlassesFeatures,
        val displayInfo: ARDisplayInfo,
        val inputMethods: ARInputMethods,
        val connectivity: ARConnectivity,
        val certifications: ARCertifications
    )
    
    enum class ARGlassesType {
        // RealWear
        REALWEAR_HMT1,
        REALWEAR_HMT1Z1,
        REALWEAR_NAVIGATOR_500,
        REALWEAR_NAVIGATOR_520,
        
        // Vuzix
        VUZIX_M400,
        VUZIX_M400C, // With scanner
        VUZIX_M4000,
        VUZIX_BLADE_2,
        VUZIX_SHIELD,
        VUZIX_Z100,
        
        // Rokid
        ROKID_GLASS_2,
        ROKID_AIR,
        ROKID_MAX,
        ROKID_STATION,
        
        // XREAL (Nreal)
        XREAL_AIR,
        XREAL_AIR_2,
        XREAL_AIR_2_PRO,
        XREAL_AIR_2_ULTRA,
        XREAL_LIGHT,
        
        // Virture
        VIRTURE_ONE,
        
        // Even Realities
        EVEN_G1,
        
        // Almer
        ALMER_ARC_TWO,
        
        // Unknown
        UNKNOWN
    }
    
    data class ARGlassesFeatures(
        val hasVoiceControl: Boolean,
        val voiceActivationType: VoiceActivationType?,
        val hasHeadTracking: Boolean,
        val hasEyeTracking: Boolean,
        val hasHandTracking: Boolean,
        val hasGestureControl: Boolean,
        val has6DOF: Boolean,
        val has3DOF: Boolean,
        val hasSLAM: Boolean,
        val hasIMU: Boolean,
        val imuAxes: Int?,
        val hasBarcodeScan: Boolean,
        val hasImageRecognition: Boolean,
        val hasObjectRecognition: Boolean
    )
    
    enum class VoiceActivationType {
        ALWAYS_ON,
        WAKE_WORD,
        PUSH_TO_TALK,
        NONE
    }
    
    data class ARDisplayInfo(
        val displayType: String,
        val resolution: String,
        val fieldOfView: Int,
        val brightness: Int?, // nits
        val refreshRate: Int,
        val isMonocular: Boolean,
        val isBinocular: Boolean,
        val isStereoscopic: Boolean,
        val isSeeThrough: Boolean,
        val hasWaveguide: Boolean,
        val adjustableIPD: Boolean,
        val ipdRange: Pair<Float, Float>? // min, max in mm
    )
    
    data class ARInputMethods(
        val hasTouchpad: Boolean,
        val touchpadButtons: Int?,
        val touchpadGestures: List<String>,
        val hasPhysicalButtons: Boolean,
        val buttonCount: Int?,
        val buttonFunctions: List<String>,
        val hasScrollWheel: Boolean,
        val hasJoystick: Boolean,
        val hasDPad: Boolean,
        val hasController: Boolean,
        val controllerType: String?
    )
    
    data class ARConnectivity(
        val hasWifi: Boolean,
        val wifiStandards: List<String>,
        val hasBluetooth: Boolean,
        val bluetoothVersion: String?,
        val hasUSBC: Boolean,
        val hasDisplayPort: Boolean,
        val hasMiracast: Boolean,
        val hasProprietaryConnector: Boolean,
        val connectorType: String?
    )
    
    data class ARCertifications(
        val ipRating: String?,
        val milStdRating: String?,
        val dropTestHeight: Float?, // meters
        val operatingTempMin: Int?, // Celsius
        val operatingTempMax: Int?,
        val isIntrinsicallySafe: Boolean,
        val atexCertified: Boolean,
        val medicalGrade: Boolean,
        val foodSafe: Boolean
    )
    
    // ========== IMPLEMENTATION METHODS ==========
    
    fun getExtendedDeviceProfile(): ExtendedDeviceProfile {
        return ExtendedDeviceProfile(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            brand = Build.BRAND,
            device = Build.DEVICE,
            product = Build.PRODUCT,
            codename = Build.DEVICE,
            marketName = getMarketName(),
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            kernelVersion = getKernelVersion(),
            securityPatchLevel = getSecurityPatchLevel(),
            bootloaderVersion = Build.BOOTLOADER,
            basebandVersion = getBasebandVersion(),
            selinuxStatus = getSELinuxStatus(),
            rootStatus = detectRootStatus(),
            buildId = Build.ID,
            buildType = Build.TYPE,
            buildTags = Build.TAGS,
            buildFingerprint = Build.FINGERPRINT,
            buildTime = Build.TIME,
            buildUser = Build.USER,
            buildHost = Build.HOST,
            incrementalVersion = Build.VERSION.INCREMENTAL,
            boardName = Build.BOARD,
            hardware = Build.HARDWARE,
            language = context.resources.configuration.locales[0].language,
            country = context.resources.configuration.locales[0].country,
            timezone = java.util.TimeZone.getDefault().id
        )
    }
    
    fun getExtendedDisplayProfile(): ExtendedDisplayProfile {
        val metrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.getRealMetrics(metrics)
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getRealMetrics(metrics)
        }
        
        val refreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.refreshRate ?: 60f
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.refreshRate
        }
        
        val hdrTypes = mutableListOf<HDRType>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }
            
            if (display?.isHdr == true) {
                hdrTypes.add(HDRType.HDR10)
            }
        }
        
        return ExtendedDisplayProfile(
            panelType = detectPanelType(),
            touchTechnology = detectTouchType(),
            multiTouchPoints = getMultiTouchPoints(),
            widthPixels = metrics.widthPixels,
            heightPixels = metrics.heightPixels,
            physicalWidthMm = metrics.widthPixels / metrics.xdpi * 25.4f,
            physicalHeightMm = metrics.heightPixels / metrics.ydpi * 25.4f,
            pixelDensity = metrics.density,
            densityDpi = metrics.densityDpi,
            aspectRatio = calculateAspectRatio(metrics.widthPixels, metrics.heightPixels),
            diagonalInches = calculateDiagonalInches(metrics),
            refreshRate = refreshRate,
            supportedRefreshRates = getSupportedRefreshRates(),
            colorSpace = detectColorSpaces(),
            hdrCapabilities = hdrTypes,
            variableRefreshRate = hasVariableRefreshRate(),
            hasAlwaysOnDisplay = hasAlwaysOnDisplay(),
            hasBlueLightFilter = hasBlueLightFilter(),
            hasOutdoorMode = hasOutdoorMode(),
            hasNotch = hasNotch(),
            hasPunchHole = hasPunchHole(),
            hasUnderDisplayCamera = hasUnderDisplayCamera(),
            hasUnderDisplayFingerprint = hasUnderDisplayFingerprint(),
            cutoutDetails = getDisplayCutoutInfo(),
            roundedCornerRadius = getRoundedCornerRadius()
        )
    }
    
    fun getExtendedSensorList(): List<ExtendedSensorInfo> {
        val sensors = mutableListOf<ExtendedSensorInfo>()
        
        sensorManager?.getSensorList(Sensor.TYPE_ALL)?.forEach { sensor ->
            sensors.add(
                ExtendedSensorInfo(
                    name = sensor.name,
                    vendor = sensor.vendor,
                    version = sensor.version,
                    type = sensor.type,
                    typeString = getSensorTypeString(sensor.type),
                    maxRange = sensor.maximumRange,
                    resolution = sensor.resolution,
                    power = sensor.power,
                    minDelay = sensor.minDelay,
                    maxDelay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        sensor.maxDelay
                    } else 0,
                    reportingMode = getReportingMode(sensor),
                    isWakeUp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        sensor.isWakeUpSensor
                    } else false,
                    isDynamic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        sensor.isDynamicSensor
                    } else false,
                    additionalInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        sensor.stringType
                    } else null,
                    placement = detectSensorPlacement(sensor)
                )
            )
        }
        
        return sensors
    }
    
    fun detectARGlasses(): ARGlassesInfo? {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.lowercase()
        
        return when {
            manufacturer.contains(MANUFACTURER_REALWEAR) -> detectRealWearGlasses(model)
            manufacturer.contains(MANUFACTURER_VUZIX) -> detectVuzixGlasses(model)
            manufacturer.contains(MANUFACTURER_ROKID) -> detectRokidGlasses(model)
            manufacturer.contains(MANUFACTURER_XREAL) || manufacturer.contains(MANUFACTURER_NREAL) -> detectXRealGlasses(model)
            manufacturer.contains(MANUFACTURER_VIRTURE) -> detectVirtureGlasses(model)
            manufacturer.contains(MANUFACTURER_EVEN) -> detectEvenGlasses(model)
            manufacturer.contains(MANUFACTURER_ALMER) -> detectAlmerGlasses(model)
            else -> null
        }
    }
    
    // ========== PRIVATE DETECTION METHODS ==========
    
    private fun detectRealWearGlasses(model: String): ARGlassesInfo {
        val type = when {
            model.contains("hmt-1z1") -> ARGlassesType.REALWEAR_HMT1Z1
            model.contains("hmt-1") -> ARGlassesType.REALWEAR_HMT1
            model.contains("navigator 520") -> ARGlassesType.REALWEAR_NAVIGATOR_520
            model.contains("navigator 500") -> ARGlassesType.REALWEAR_NAVIGATOR_500
            else -> ARGlassesType.UNKNOWN
        }
        
        val is520 = type == ARGlassesType.REALWEAR_NAVIGATOR_520
        
        return ARGlassesInfo(
            manufacturer = "RealWear",
            model = Build.MODEL,
            type = type,
            features = ARGlassesFeatures(
                hasVoiceControl = true,
                voiceActivationType = VoiceActivationType.ALWAYS_ON,
                hasHeadTracking = false,
                hasEyeTracking = false,
                hasHandTracking = false,
                hasGestureControl = true, // Head gestures
                has6DOF = false,
                has3DOF = true,
                hasSLAM = false,
                hasIMU = true,
                imuAxes = 9,
                hasBarcodeScan = false,
                hasImageRecognition = false,
                hasObjectRecognition = false
            ),
            displayInfo = ARDisplayInfo(
                displayType = "Micro-display",
                resolution = if (is520) "1280x720" else "854x480",
                fieldOfView = 20,
                brightness = 2000,
                refreshRate = 60,
                isMonocular = true,
                isBinocular = false,
                isStereoscopic = false,
                isSeeThrough = false,
                hasWaveguide = false,
                adjustableIPD = false,
                ipdRange = null
            ),
            inputMethods = ARInputMethods(
                hasTouchpad = false,
                touchpadButtons = null,
                touchpadGestures = emptyList(),
                hasPhysicalButtons = true,
                buttonCount = 1, // Power button only
                buttonFunctions = listOf("Power"),
                hasScrollWheel = false,
                hasJoystick = false,
                hasDPad = false,
                hasController = false,
                controllerType = null
            ),
            connectivity = ARConnectivity(
                hasWifi = true,
                wifiStandards = listOf("802.11 a/b/g/n/ac"),
                hasBluetooth = true,
                bluetoothVersion = "5.0",
                hasUSBC = true,
                hasDisplayPort = false,
                hasMiracast = false,
                hasProprietaryConnector = false,
                connectorType = null
            ),
            certifications = ARCertifications(
                ipRating = "IP66",
                milStdRating = "MIL-STD-810G",
                dropTestHeight = 2.0f,
                operatingTempMin = -20,
                operatingTempMax = 50,
                isIntrinsicallySafe = type == ARGlassesType.REALWEAR_HMT1Z1,
                atexCertified = type == ARGlassesType.REALWEAR_HMT1Z1,
                medicalGrade = false,
                foodSafe = false
            )
        )
    }
    
    private fun detectVuzixGlasses(model: String): ARGlassesInfo {
        val type = when {
            model.contains("m400c") -> ARGlassesType.VUZIX_M400C
            model.contains("m4000") -> ARGlassesType.VUZIX_M4000
            model.contains("m400") -> ARGlassesType.VUZIX_M400
            model.contains("blade 2") -> ARGlassesType.VUZIX_BLADE_2
            model.contains("shield") -> ARGlassesType.VUZIX_SHIELD
            model.contains("z100") -> ARGlassesType.VUZIX_Z100
            else -> ARGlassesType.UNKNOWN
        }
        
        val isM4000 = type == ARGlassesType.VUZIX_M4000
        val hasScanner = type == ARGlassesType.VUZIX_M400C
        
        return ARGlassesInfo(
            manufacturer = "Vuzix",
            model = Build.MODEL,
            type = type,
            features = ARGlassesFeatures(
                hasVoiceControl = true,
                voiceActivationType = VoiceActivationType.PUSH_TO_TALK,
                hasHeadTracking = type == ARGlassesType.VUZIX_SHIELD,
                hasEyeTracking = false,
                hasHandTracking = type == ARGlassesType.VUZIX_SHIELD,
                hasGestureControl = true,
                has6DOF = type == ARGlassesType.VUZIX_SHIELD,
                has3DOF = type != ARGlassesType.VUZIX_SHIELD,
                hasSLAM = false,
                hasIMU = true,
                imuAxes = 9,
                hasBarcodeScan = hasScanner,
                hasImageRecognition = false,
                hasObjectRecognition = false
            ),
            displayInfo = ARDisplayInfo(
                displayType = if (isM4000) "OLED" else "LCD",
                resolution = if (isM4000) "854x480" else "640x360",
                fieldOfView = if (type == ARGlassesType.VUZIX_BLADE_2) 28 else 17,
                brightness = 2000,
                refreshRate = 60,
                isMonocular = true,
                isBinocular = false,
                isStereoscopic = false,
                isSeeThrough = type == ARGlassesType.VUZIX_BLADE_2,
                hasWaveguide = type == ARGlassesType.VUZIX_BLADE_2,
                adjustableIPD = false,
                ipdRange = null
            ),
            inputMethods = ARInputMethods(
                hasTouchpad = true,
                touchpadButtons = 3,
                touchpadGestures = listOf("tap", "hold", "swipe_forward", "swipe_back"),
                hasPhysicalButtons = true,
                buttonCount = 2,
                buttonFunctions = listOf("Power", "Function"),
                hasScrollWheel = false,
                hasJoystick = false,
                hasDPad = false,
                hasController = false,
                controllerType = null
            ),
            connectivity = ARConnectivity(
                hasWifi = true,
                wifiStandards = listOf("802.11 a/b/g/n/ac"),
                hasBluetooth = true,
                bluetoothVersion = "5.0",
                hasUSBC = true,
                hasDisplayPort = false,
                hasMiracast = true,
                hasProprietaryConnector = false,
                connectorType = null
            ),
            certifications = ARCertifications(
                ipRating = "IP67",
                milStdRating = null,
                dropTestHeight = 1.8f,
                operatingTempMin = -10,
                operatingTempMax = 40,
                isIntrinsicallySafe = false,
                atexCertified = false,
                medicalGrade = false,
                foodSafe = false
            )
        )
    }
    
    private fun detectRokidGlasses(model: String): ARGlassesInfo {
        val type = when {
            model.contains("glass 2") -> ARGlassesType.ROKID_GLASS_2
            model.contains("air") -> ARGlassesType.ROKID_AIR
            model.contains("max") -> ARGlassesType.ROKID_MAX
            model.contains("station") -> ARGlassesType.ROKID_STATION
            else -> ARGlassesType.UNKNOWN
        }
        
        return ARGlassesInfo(
            manufacturer = "Rokid",
            model = Build.MODEL,
            type = type,
            features = ARGlassesFeatures(
                hasVoiceControl = true,
                voiceActivationType = VoiceActivationType.WAKE_WORD,
                hasHeadTracking = true,
                hasEyeTracking = false,
                hasHandTracking = type == ARGlassesType.ROKID_STATION,
                hasGestureControl = true,
                has6DOF = type == ARGlassesType.ROKID_GLASS_2,
                has3DOF = type != ARGlassesType.ROKID_GLASS_2,
                hasSLAM = type == ARGlassesType.ROKID_GLASS_2,
                hasIMU = true,
                imuAxes = 9,
                hasBarcodeScan = false,
                hasImageRecognition = true,
                hasObjectRecognition = type == ARGlassesType.ROKID_GLASS_2
            ),
            displayInfo = ARDisplayInfo(
                displayType = "Micro-OLED",
                resolution = "1920x1080",
                fieldOfView = 40,
                brightness = 1800,
                refreshRate = if (type == ARGlassesType.ROKID_MAX) 120 else 60,
                isMonocular = false,
                isBinocular = true,
                isStereoscopic = true,
                isSeeThrough = false,
                hasWaveguide = false,
                adjustableIPD = true,
                ipdRange = Pair(58f, 72f)
            ),
            inputMethods = ARInputMethods(
                hasTouchpad = true,
                touchpadButtons = 1,
                touchpadGestures = listOf("tap", "swipe", "hold"),
                hasPhysicalButtons = true,
                buttonCount = 3,
                buttonFunctions = listOf("Power", "Volume+", "Volume-"),
                hasScrollWheel = false,
                hasJoystick = false,
                hasDPad = false,
                hasController = type == ARGlassesType.ROKID_STATION,
                controllerType = if (type == ARGlassesType.ROKID_STATION) "6DOF Controller" else null
            ),
            connectivity = ARConnectivity(
                hasWifi = true,
                wifiStandards = listOf("802.11 a/b/g/n/ac", "WiFi 6"),
                hasBluetooth = true,
                bluetoothVersion = "5.2",
                hasUSBC = true,
                hasDisplayPort = true,
                hasMiracast = false,
                hasProprietaryConnector = false,
                connectorType = null
            ),
            certifications = ARCertifications(
                ipRating = null,
                milStdRating = null,
                dropTestHeight = null,
                operatingTempMin = 0,
                operatingTempMax = 35,
                isIntrinsicallySafe = false,
                atexCertified = false,
                medicalGrade = false,
                foodSafe = false
            )
        )
    }
    
    private fun detectXRealGlasses(model: String): ARGlassesInfo {
        val type = when {
            model.contains("air 2 ultra") -> ARGlassesType.XREAL_AIR_2_ULTRA
            model.contains("air 2 pro") -> ARGlassesType.XREAL_AIR_2_PRO
            model.contains("air 2") -> ARGlassesType.XREAL_AIR_2
            model.contains("air") -> ARGlassesType.XREAL_AIR
            model.contains("light") -> ARGlassesType.XREAL_LIGHT
            else -> ARGlassesType.UNKNOWN
        }
        
        val hasElectrochromic = type == ARGlassesType.XREAL_AIR_2_PRO
        val has6DOF = type == ARGlassesType.XREAL_LIGHT || type == ARGlassesType.XREAL_AIR_2_ULTRA
        
        return ARGlassesInfo(
            manufacturer = "XREAL",
            model = Build.MODEL,
            type = type,
            features = ARGlassesFeatures(
                hasVoiceControl = false,
                voiceActivationType = VoiceActivationType.NONE,
                hasHeadTracking = true,
                hasEyeTracking = false,
                hasHandTracking = has6DOF,
                hasGestureControl = false,
                has6DOF = has6DOF,
                has3DOF = !has6DOF,
                hasSLAM = has6DOF,
                hasIMU = true,
                imuAxes = 9,
                hasBarcodeScan = false,
                hasImageRecognition = false,
                hasObjectRecognition = false
            ),
            displayInfo = ARDisplayInfo(
                displayType = "Micro-OLED",
                resolution = "1920x1080",
                fieldOfView = if (type == ARGlassesType.XREAL_AIR_2_ULTRA) 52 else 46,
                brightness = 400,
                refreshRate = if (type == ARGlassesType.XREAL_AIR_2_PRO) 120 else 90,
                isMonocular = false,
                isBinocular = true,
                isStereoscopic = true,
                isSeeThrough = false,
                hasWaveguide = false,
                adjustableIPD = false,
                ipdRange = null
            ),
            inputMethods = ARInputMethods(
                hasTouchpad = false,
                touchpadButtons = null,
                touchpadGestures = emptyList(),
                hasPhysicalButtons = hasElectrochromic,
                buttonCount = if (hasElectrochromic) 1 else 0,
                buttonFunctions = if (hasElectrochromic) listOf("Dimming") else emptyList(),
                hasScrollWheel = false,
                hasJoystick = false,
                hasDPad = false,
                hasController = false,
                controllerType = null
            ),
            connectivity = ARConnectivity(
                hasWifi = false,
                wifiStandards = emptyList(),
                hasBluetooth = false,
                bluetoothVersion = null,
                hasUSBC = true,
                hasDisplayPort = true,
                hasMiracast = false,
                hasProprietaryConnector = false,
                connectorType = null
            ),
            certifications = ARCertifications(
                ipRating = null,
                milStdRating = null,
                dropTestHeight = null,
                operatingTempMin = 0,
                operatingTempMax = 35,
                isIntrinsicallySafe = false,
                atexCertified = false,
                medicalGrade = false,
                foodSafe = false
            )
        )
    }
    
    private fun detectVirtureGlasses(model: String): ARGlassesInfo {
        return ARGlassesInfo(
            manufacturer = "Virture",
            model = Build.MODEL,
            type = ARGlassesType.VIRTURE_ONE,
            features = ARGlassesFeatures(
                hasVoiceControl = true,
                voiceActivationType = VoiceActivationType.PUSH_TO_TALK,
                hasHeadTracking = true,
                hasEyeTracking = false,
                hasHandTracking = false,
                hasGestureControl = false,
                has6DOF = false,
                has3DOF = true,
                hasSLAM = false,
                hasIMU = true,
                imuAxes = 6,
                hasBarcodeScan = false,
                hasImageRecognition = false,
                hasObjectRecognition = false
            ),
            displayInfo = ARDisplayInfo(
                displayType = "Binocular OLED",
                resolution = "1920x1080",
                fieldOfView = 38,
                brightness = 300,
                refreshRate = 60,
                isMonocular = false,
                isBinocular = true,
                isStereoscopic = false,
                isSeeThrough = false,
                hasWaveguide = false,
                adjustableIPD = false,
                ipdRange = null
            ),
            inputMethods = ARInputMethods(
                hasTouchpad = false,
                touchpadButtons = null,
                touchpadGestures = emptyList(),
                hasPhysicalButtons = true,
                buttonCount = 2,
                buttonFunctions = listOf("Scroll", "Multi-function"),
                hasScrollWheel = true,
                hasJoystick = false,
                hasDPad = false,
                hasController = false,
                controllerType = null
            ),
            connectivity = ARConnectivity(
                hasWifi = true,
                wifiStandards = listOf("WiFi 6"),
                hasBluetooth = true,
                bluetoothVersion = "5.0",
                hasUSBC = true,
                hasDisplayPort = false,
                hasMiracast = true,
                hasProprietaryConnector = false,
                connectorType = null
            ),
            certifications = ARCertifications(
                ipRating = null,
                milStdRating = null,
                dropTestHeight = null,
                operatingTempMin = 0,
                operatingTempMax = 35,
                isIntrinsicallySafe = false,
                atexCertified = false,
                medicalGrade = false,
                foodSafe = false
            )
        )
    }
    
    private fun detectEvenGlasses(model: String): ARGlassesInfo {
        return ARGlassesInfo(
            manufacturer = "Even Realities",
            model = Build.MODEL,
            type = ARGlassesType.EVEN_G1,
            features = ARGlassesFeatures(
                hasVoiceControl = false,
                voiceActivationType = VoiceActivationType.NONE,
                hasHeadTracking = false,
                hasEyeTracking = false,
                hasHandTracking = false,
                hasGestureControl = false,
                has6DOF = false,
                has3DOF = false,
                hasSLAM = false,
                hasIMU = false,
                imuAxes = null,
                hasBarcodeScan = false,
                hasImageRecognition = false,
                hasObjectRecognition = false
            ),
            displayInfo = ARDisplayInfo(
                displayType = "Micro-LED",
                resolution = "640x200",
                fieldOfView = 20,
                brightness = 200,
                refreshRate = 30,
                isMonocular = true,
                isBinocular = false,
                isStereoscopic = false,
                isSeeThrough = true,
                hasWaveguide = true,
                adjustableIPD = false,
                ipdRange = null
            ),
            inputMethods = ARInputMethods(
                hasTouchpad = false,
                touchpadButtons = null,
                touchpadGestures = emptyList(),
                hasPhysicalButtons = false,
                buttonCount = 0,
                buttonFunctions = emptyList(),
                hasScrollWheel = false,
                hasJoystick = false,
                hasDPad = false,
                hasController = false,
                controllerType = null
            ),
            connectivity = ARConnectivity(
                hasWifi = false,
                wifiStandards = emptyList(),
                hasBluetooth = true,
                bluetoothVersion = "5.0 LE",
                hasUSBC = false,
                hasDisplayPort = false,
                hasMiracast = false,
                hasProprietaryConnector = true,
                connectorType = "Magnetic charging"
            ),
            certifications = ARCertifications(
                ipRating = "IPX4",
                milStdRating = null,
                dropTestHeight = null,
                operatingTempMin = 0,
                operatingTempMax = 40,
                isIntrinsicallySafe = false,
                atexCertified = false,
                medicalGrade = false,
                foodSafe = false
            )
        )
    }
    
    private fun detectAlmerGlasses(model: String): ARGlassesInfo {
        return ARGlassesInfo(
            manufacturer = "Almer",
            model = Build.MODEL,
            type = ARGlassesType.ALMER_ARC_TWO,
            features = ARGlassesFeatures(
                hasVoiceControl = true,
                voiceActivationType = VoiceActivationType.WAKE_WORD,
                hasHeadTracking = true,
                hasEyeTracking = false,
                hasHandTracking = false,
                hasGestureControl = false,
                has6DOF = false,
                has3DOF = true,
                hasSLAM = false,
                hasIMU = true,
                imuAxes = 6,
                hasBarcodeScan = false,
                hasImageRecognition = false,
                hasObjectRecognition = false
            ),
            displayInfo = ARDisplayInfo(
                displayType = "OLED",
                resolution = "1920x1080",
                fieldOfView = 43,
                brightness = 400,
                refreshRate = 60,
                isMonocular = false,
                isBinocular = true,
                isStereoscopic = true,
                isSeeThrough = false,
                hasWaveguide = false,
                adjustableIPD = false,
                ipdRange = null
            ),
            inputMethods = ARInputMethods(
                hasTouchpad = true,
                touchpadButtons = 1,
                touchpadGestures = listOf("tap", "swipe"),
                hasPhysicalButtons = true,
                buttonCount = 2,
                buttonFunctions = listOf("Power", "Home"),
                hasScrollWheel = false,
                hasJoystick = false,
                hasDPad = false,
                hasController = false,
                controllerType = null
            ),
            connectivity = ARConnectivity(
                hasWifi = true,
                wifiStandards = listOf("802.11 a/b/g/n/ac"),
                hasBluetooth = true,
                bluetoothVersion = "5.0",
                hasUSBC = true,
                hasDisplayPort = false,
                hasMiracast = false,
                hasProprietaryConnector = false,
                connectorType = null
            ),
            certifications = ARCertifications(
                ipRating = null,
                milStdRating = null,
                dropTestHeight = null,
                operatingTempMin = 0,
                operatingTempMax = 35,
                isIntrinsicallySafe = false,
                atexCertified = false,
                medicalGrade = false,
                foodSafe = false
            )
        )
    }
    
    // ========== HELPER METHODS ==========
    
    private fun detectPanelType(): PanelType {
        val model = Build.MODEL.lowercase()
        val device = Build.DEVICE.lowercase()
        
        return when {
            // Known OLED devices
            model.contains("galaxy s") && Build.MANUFACTURER.equals("samsung", ignoreCase = true) -> PanelType.SUPER_AMOLED
            model.contains("pixel") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> PanelType.OLED
            model.contains("iphone") -> PanelType.OLED // Won't happen on Android but for completeness
            
            // AR glasses are typically OLED or Micro-OLED
            Build.MANUFACTURER.lowercase().let { 
                it.contains("vuzix") || it.contains("rokid") || it.contains("xreal") 
            } -> PanelType.MICRO_OLED
            
            // Check for always-on display (usually OLED)
            packageManager.hasSystemFeature("android.hardware.display.always_on") -> PanelType.OLED
            
            // E-ink devices
            model.contains("boox") || model.contains("onyx") -> PanelType.E_INK
            
            // Default to LCD for older or unknown devices
            else -> PanelType.LCD
        }
    }
    
    private fun detectTouchType(): TouchType {
        return when {
            // AR glasses often have no touch
            Build.MANUFACTURER.lowercase().contains("realwear") -> TouchType.NONE
            Build.MANUFACTURER.lowercase().contains("even") -> TouchType.NONE
            
            // Most modern devices use capacitive
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH -> TouchType.CAPACITIVE
            
            else -> TouchType.UNKNOWN
        }
    }
    
    private fun getMultiTouchPoints(): Int {
        return if (packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_JAZZHAND)) {
            10 // Supports 10+ fingers
        } else if (packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT)) {
            5 // Supports 5+ fingers
        } else if (packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH)) {
            2 // Basic multitouch
        } else {
            1 // Single touch or no touch
        }
    }
    
    private fun calculateAspectRatio(width: Int, height: Int): AspectRatio {
        val ratio = width.toFloat() / height.toFloat()
        return when {
            ratio in 1.76f..1.79f -> AspectRatio.RATIO_16_9
            ratio in 1.99f..2.01f -> AspectRatio.RATIO_18_9
            ratio in 2.10f..2.12f -> AspectRatio.RATIO_19_9
            ratio in 2.21f..2.23f -> AspectRatio.RATIO_20_9
            ratio in 2.32f..2.34f -> AspectRatio.RATIO_21_9
            ratio in 1.32f..1.34f -> AspectRatio.RATIO_4_3
            ratio in 1.49f..1.51f -> AspectRatio.RATIO_3_2
            else -> AspectRatio.CUSTOM
        }
    }
    
    private fun calculateDiagonalInches(metrics: DisplayMetrics): Float {
        val widthInches = metrics.widthPixels / metrics.xdpi
        val heightInches = metrics.heightPixels / metrics.ydpi
        return kotlin.math.sqrt(widthInches * widthInches + heightInches * heightInches)
    }
    
    private fun getSupportedRefreshRates(): List<Float> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.supportedModes?.map { it.refreshRate } ?: listOf(60f)
        } else {
            listOf(60f) // Default
        }
    }
    
    private fun detectColorSpaces(): List<ColorSpace> {
        val spaces = mutableListOf(ColorSpace.SRGB) // All devices support sRGB
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }
            
            if (display?.isWideColorGamut == true) {
                spaces.add(ColorSpace.DCI_P3)
                spaces.add(ColorSpace.DISPLAY_P3)
            }
        }
        
        return spaces
    }
    
    private fun hasVariableRefreshRate(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val modes = context.display?.supportedModes ?: emptyArray()
            modes.map { it.refreshRate }.distinct().size > 1
        } else {
            false
        }
    }
    
    private fun hasAlwaysOnDisplay(): Boolean {
        return packageManager.hasSystemFeature("android.hardware.display.always_on") ||
               Build.MODEL.lowercase().contains("galaxy") ||
               Build.MODEL.lowercase().contains("pixel")
    }
    
    private fun hasBlueLightFilter(): Boolean {
        // Most modern devices have this
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    }
    
    private fun hasOutdoorMode(): Boolean {
        // Manufacturer specific
        return Build.MANUFACTURER.equals("samsung", ignoreCase = true) ||
               Build.MANUFACTURER.equals("oneplus", ignoreCase = true)
    }
    
    private fun hasNotch(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val cutout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display?.cutout
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.cutout
            }
            cutout != null && cutout.boundingRects.isNotEmpty()
        } else {
            // Check for known notch devices
            Build.MODEL.lowercase().let {
                it.contains("iphone x") || // Won't happen but for reference
                it.contains("essential") ||
                it.contains("poco f1")
            }
        }
    }
    
    private fun hasPunchHole(): Boolean {
        // Check for known punch hole devices
        return Build.MODEL.lowercase().let {
            it.contains("galaxy s10") ||
            it.contains("galaxy note10") ||
            it.contains("galaxy a") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        }
    }
    
    private fun hasUnderDisplayCamera(): Boolean {
        // Very few devices have this
        return Build.MODEL.lowercase().let {
            it.contains("axon 20") ||
            it.contains("axon 30") ||
            it.contains("mix 4")
        }
    }
    
    private fun hasUnderDisplayFingerprint(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) &&
            Build.MODEL.lowercase().let {
                it.contains("galaxy s") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ||
                it.contains("oneplus") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
            }
        } else {
            false
        }
    }
    
    private fun getDisplayCutoutInfo(): DisplayCutoutInfo? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val cutout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display?.cutout
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.cutout
            }
            
            cutout?.let {
                val bounds = it.boundingRects.map { rect ->
                    Rect(rect.left, rect.top, rect.right, rect.bottom)
                }
                
                val safeInsets = Insets(
                    it.safeInsetLeft,
                    it.safeInsetTop,
                    it.safeInsetRight,
                    it.safeInsetBottom
                )
                
                val type = when {
                    bounds.size == 1 && bounds[0].width() > bounds[0].height() * 2 -> CutoutType.NOTCH
                    bounds.size == 1 && bounds[0].width() < 100 -> CutoutType.PUNCH_HOLE
                    Build.MODEL.contains("iPhone 14", ignoreCase = true) -> CutoutType.DYNAMIC_ISLAND
                    else -> CutoutType.NOTCH
                }
                
                DisplayCutoutInfo(bounds, safeInsets, type)
            }
        } else {
            null
        }
    }
    
    private fun getRoundedCornerRadius(): Float {
        // This is typically not exposed, using heuristics
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> 32f // Modern devices
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> 24f // Slightly older
            else -> 0f // No rounded corners
        }
    }
    
    private fun getSensorTypeString(type: Int): String {
        return when (type) {
            Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
            Sensor.TYPE_MAGNETIC_FIELD -> "Magnetometer"
            Sensor.TYPE_GYROSCOPE -> "Gyroscope"
            Sensor.TYPE_LIGHT -> "Light"
            Sensor.TYPE_PRESSURE -> "Pressure"
            Sensor.TYPE_PROXIMITY -> "Proximity"
            Sensor.TYPE_GRAVITY -> "Gravity"
            Sensor.TYPE_LINEAR_ACCELERATION -> "Linear Acceleration"
            Sensor.TYPE_ROTATION_VECTOR -> "Rotation Vector"
            Sensor.TYPE_RELATIVE_HUMIDITY -> "Humidity"
            Sensor.TYPE_AMBIENT_TEMPERATURE -> "Temperature"
            else -> "Unknown ($type)"
        }
    }
    
    private fun getReportingMode(sensor: Sensor): ReportingMode {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            when (sensor.reportingMode) {
                Sensor.REPORTING_MODE_CONTINUOUS -> ReportingMode.CONTINUOUS
                Sensor.REPORTING_MODE_ON_CHANGE -> ReportingMode.ON_CHANGE
                Sensor.REPORTING_MODE_ONE_SHOT -> ReportingMode.ONE_SHOT
                Sensor.REPORTING_MODE_SPECIAL_TRIGGER -> ReportingMode.SPECIAL_TRIGGER
                else -> ReportingMode.CONTINUOUS
            }
        } else {
            // Estimate based on sensor type
            when (sensor.type) {
                Sensor.TYPE_ACCELEROMETER,
                Sensor.TYPE_GYROSCOPE,
                Sensor.TYPE_MAGNETIC_FIELD -> ReportingMode.CONTINUOUS
                Sensor.TYPE_LIGHT,
                Sensor.TYPE_PROXIMITY,
                Sensor.TYPE_PRESSURE -> ReportingMode.ON_CHANGE
                else -> ReportingMode.CONTINUOUS
            }
        }
    }
    
    private fun detectSensorPlacement(sensor: Sensor): SensorPlacement {
        // This is mostly guesswork based on sensor type
        return when (sensor.type) {
            Sensor.TYPE_PROXIMITY -> SensorPlacement.FRONT
            Sensor.TYPE_LIGHT -> SensorPlacement.FRONT
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_MAGNETIC_FIELD -> SensorPlacement.INTERNAL
            else -> SensorPlacement.UNKNOWN
        }
    }
}
