# Chapter 8: DeviceManager Library

**VOS4 Developer Manual**
**Version:** 4.0.0
**Last Updated:** 2025-11-02
**Status:** Complete
**Module Location:** `/modules/libraries/DeviceManager`

---

## Table of Contents

- [8.1 Overview & Purpose](#81-overview--purpose)
- [8.2 Device Detection Architecture](#82-device-detection-architecture)
- [8.3 Capability Management](#83-capability-management)
- [8.4 Platform Abstraction](#84-platform-abstraction)
- [8.5 XR Device Support](#85-xr-device-support)
- [8.6 Accessibility Integration](#86-accessibility-integration)
- [8.7 Integration Guide](#87-integration-guide)

---

## 8.1 Overview & Purpose

### Purpose

The DeviceManager library provides device detection, capability management, and platform abstraction for VOS4. It enables the system to:

- **Detect device type**: Phone, Tablet, TV, Automotive, Wear, XR
- **Determine capabilities**: Screen size, input methods, sensors available
- **Platform abstraction**: Unified API across Android variants
- **Accessibility services**: Manage AccessibilityManager integration
- **XR support**: Detect and support AR/VR devices

### Architecture

```
DeviceManager
    ├── DeviceDetector (Device type identification)
    ├── CapabilityManager (Feature detection)
    ├── PlatformAdapter (Platform-specific code)
    ├── AccessibilityHelper (A11y service management)
    └── XRDeviceManager (AR/VR support)
```

**Build Configuration:**

```gradle
// File: modules/libraries/DeviceManager/build.gradle.kts
android {
    namespace = "com.augmentalis.devicemanager"
    compileSdk = 34

    defaultConfig {
        minSdk = 29
        multiDexEnabled = true
    }
}

dependencies {
    // Android Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Accessibility
    implementation("androidx.accessibility:accessibility:1.0.0")

    // XR Support (when available)
    // compileOnly("androidx.xr:xr-core:1.0.0-alpha01")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")
}
```

---

## 8.2 Device Detection Architecture

### DeviceType Enum

```kotlin
/**
 * Supported device types
 */
enum class DeviceType {
    PHONE,          // Standard smartphone
    TABLET,         // Tablet (sw600dp+)
    TV,             // Android TV / Google TV
    AUTOMOTIVE,     // Android Automotive OS
    WEAR,           // Wear OS
    XR_HEADSET,     // AR/VR headset
    FOLDABLE,       // Foldable phone
    CHROMEBOOK,     // Chrome OS device
    UNKNOWN         // Unable to determine
}

/**
 * Form factor classification
 */
enum class FormFactor {
    SMALL,      // Phone-sized
    MEDIUM,     // Phablet/small tablet
    LARGE,      // Large tablet/TV
    WEARABLE,   // Watch/band
    XR          // Headset/glasses
}
```

### DeviceDetector

```kotlin
class DeviceDetector(private val context: Context) {

    companion object {
        private const val TAG = "DeviceDetector"

        // Screen size thresholds (dp)
        private const val TABLET_MIN_DP = 600
        private const val LARGE_TABLET_MIN_DP = 720
    }

    /**
     * Detect current device type
     */
    fun detectDeviceType(): DeviceType {
        return when {
            isTV() -> DeviceType.TV
            isAutomotive() -> DeviceType.AUTOMOTIVE
            isWear() -> DeviceType.WEAR
            isXRHeadset() -> DeviceType.XR_HEADSET
            isFoldable() -> DeviceType.FOLDABLE
            isChromebook() -> DeviceType.CHROMEBOOK
            isTablet() -> DeviceType.TABLET
            isPhone() -> DeviceType.PHONE
            else -> DeviceType.UNKNOWN
        }
    }

    /**
     * Check if device is TV
     */
    private fun isTV(): Boolean {
        val uiMode = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiMode.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    /**
     * Check if device is Automotive
     */
    private fun isAutomotive(): Boolean {
        val pm = context.packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)
    }

    /**
     * Check if device is Wear OS
     */
    private fun isWear(): Boolean {
        val pm = context.packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_WATCH)
    }

    /**
     * Check if device is XR headset
     */
    private fun isXRHeadset(): Boolean {
        val pm = context.packageManager
        // Note: XR features not yet available in stable Android
        return try {
            pm.hasSystemFeature("android.hardware.xr") ||
            pm.hasSystemFeature("com.google.android.feature.AR")
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if device is foldable
     */
    private fun isFoldable(): Boolean {
        val pm = context.packageManager
        return try {
            pm.hasSystemFeature("android.hardware.foldable") ||
            Build.MODEL.contains("Fold", ignoreCase = true) ||
            Build.MODEL.contains("Flip", ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if device is Chromebook
     */
    private fun isChromebook(): Boolean {
        val pm = context.packageManager
        return pm.hasSystemFeature("org.chromium.arc") ||
               pm.hasSystemFeature("org.chromium.arc.device_management")
    }

    /**
     * Check if device is tablet
     */
    private fun isTablet(): Boolean {
        val configuration = context.resources.configuration
        val screenLayout = configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK

        // Check screen layout
        if (screenLayout >= Configuration.SCREENLAYOUT_SIZE_LARGE) {
            return true
        }

        // Check smallest width (sw600dp+)
        val smallestWidthDp = configuration.smallestScreenWidthDp
        return smallestWidthDp >= TABLET_MIN_DP
    }

    /**
     * Check if device is phone
     */
    private fun isPhone(): Boolean {
        // Phone is the default if no other type matches
        return !isTablet() && !isTV() && !isWear() &&
               !isAutomotive() && !isXRHeadset()
    }

    /**
     * Get form factor classification
     */
    fun getFormFactor(): FormFactor {
        return when (detectDeviceType()) {
            DeviceType.PHONE -> FormFactor.SMALL
            DeviceType.FOLDABLE -> {
                if (isUnfolded()) FormFactor.MEDIUM else FormFactor.SMALL
            }
            DeviceType.TABLET -> {
                val widthDp = context.resources.configuration.smallestScreenWidthDp
                if (widthDp >= LARGE_TABLET_MIN_DP) FormFactor.LARGE else FormFactor.MEDIUM
            }
            DeviceType.TV -> FormFactor.LARGE
            DeviceType.WEAR -> FormFactor.WEARABLE
            DeviceType.XR_HEADSET -> FormFactor.XR
            DeviceType.AUTOMOTIVE -> FormFactor.LARGE
            DeviceType.CHROMEBOOK -> FormFactor.LARGE
            DeviceType.UNKNOWN -> FormFactor.SMALL
        }
    }

    /**
     * Check if foldable is unfolded
     */
    private fun isUnfolded(): Boolean {
        // Use WindowManager to detect fold state
        // This is a simplified check
        val displayMetrics = context.resources.displayMetrics
        val widthDp = displayMetrics.widthPixels / displayMetrics.density
        return widthDp >= TABLET_MIN_DP
    }

    /**
     * Get screen size in DP
     */
    fun getScreenSizeDp(): Pair<Int, Int> {
        val displayMetrics = context.resources.displayMetrics
        val widthDp = (displayMetrics.widthPixels / displayMetrics.density).toInt()
        val heightDp = (displayMetrics.heightPixels / displayMetrics.density).toInt()
        return Pair(widthDp, heightDp)
    }

    /**
     * Get detailed device info
     */
    fun getDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            deviceType = detectDeviceType(),
            formFactor = getFormFactor(),
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            brand = Build.BRAND,
            androidVersion = Build.VERSION.SDK_INT,
            screenSizeDp = getScreenSizeDp(),
            density = context.resources.displayMetrics.density
        )
    }
}

/**
 * Complete device information
 */
data class DeviceInfo(
    val deviceType: DeviceType,
    val formFactor: FormFactor,
    val manufacturer: String,
    val model: String,
    val brand: String,
    val androidVersion: Int,
    val screenSizeDp: Pair<Int, Int>,
    val density: Float
) {
    fun getDisplayName(): String {
        return "$manufacturer $model"
    }

    fun getAndroidVersionName(): String {
        return when (androidVersion) {
            29 -> "Android 10"
            30 -> "Android 11"
            31 -> "Android 12"
            32 -> "Android 12L"
            33 -> "Android 13"
            34 -> "Android 14"
            35 -> "Android 15"
            else -> "Android $androidVersion"
        }
    }
}
```

---

## 8.3 Capability Management

### CapabilityManager

```kotlin
class CapabilityManager(private val context: Context) {

    companion object {
        private const val TAG = "CapabilityManager"
    }

    private val packageManager = context.packageManager

    /**
     * Device capabilities data class
     */
    data class DeviceCapabilities(
        // Display
        val hasTouch: Boolean,
        val hasTouchscreen: Boolean,
        val supportsMultitouch: Boolean,
        val screenSize: ScreenSize,
        val density: DisplayDensity,

        // Input
        val hasKeyboard: Boolean,
        val hasMicrophone: Boolean,
        val hasSpeaker: Boolean,
        val hasCamera: Boolean,
        val hasFrontCamera: Boolean,

        // Sensors
        val hasAccelerometer: Boolean,
        val hasGyroscope: Boolean,
        val hasProximitySensor: Boolean,
        val hasLightSensor: Boolean,
        val hasMagnetometer: Boolean,
        val hasGPS: Boolean,

        // Connectivity
        val hasWifi: Boolean,
        val hasBluetooth: Boolean,
        val hasNFC: Boolean,
        val hasCellular: Boolean,
        val hasUSB: Boolean,

        // Audio
        val hasAudioOutput: Boolean,
        val supportsLowLatencyAudio: Boolean,
        val supportsProAudio: Boolean,

        // Accessibility
        val supportsAccessibilityService: Boolean,
        val hasVibrator: Boolean,

        // Advanced
        val supportsBiometrics: Boolean,
        val hasFingerprint: Boolean,
        val hasFaceUnlock: Boolean,
        val supportsVR: Boolean,
        val supportsAR: Boolean
    )

    enum class ScreenSize {
        SMALL, NORMAL, LARGE, XLARGE
    }

    enum class DisplayDensity {
        LDPI, MDPI, HDPI, XHDPI, XXHDPI, XXXHDPI
    }

    /**
     * Detect all device capabilities
     */
    fun detectCapabilities(): DeviceCapabilities {
        return DeviceCapabilities(
            // Display
            hasTouch = hasFeature(PackageManager.FEATURE_TOUCHSCREEN),
            hasTouchscreen = hasFeature(PackageManager.FEATURE_TOUCHSCREEN),
            supportsMultitouch = hasFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_DISTINCT),
            screenSize = detectScreenSize(),
            density = detectDensity(),

            // Input
            hasKeyboard = hasFeature(PackageManager.FEATURE_BLUETOOTH) ||
                         context.resources.configuration.keyboard != Configuration.KEYBOARD_NOKEYS,
            hasMicrophone = hasFeature(PackageManager.FEATURE_MICROPHONE),
            hasSpeaker = hasAudioOutput(),
            hasCamera = hasFeature(PackageManager.FEATURE_CAMERA_ANY),
            hasFrontCamera = hasFeature(PackageManager.FEATURE_CAMERA_FRONT),

            // Sensors
            hasAccelerometer = hasSensor(Sensor.TYPE_ACCELEROMETER),
            hasGyroscope = hasSensor(Sensor.TYPE_GYROSCOPE),
            hasProximitySensor = hasSensor(Sensor.TYPE_PROXIMITY),
            hasLightSensor = hasSensor(Sensor.TYPE_LIGHT),
            hasMagnetometer = hasSensor(Sensor.TYPE_MAGNETIC_FIELD),
            hasGPS = hasFeature(PackageManager.FEATURE_LOCATION_GPS),

            // Connectivity
            hasWifi = hasFeature(PackageManager.FEATURE_WIFI),
            hasBluetooth = hasFeature(PackageManager.FEATURE_BLUETOOTH),
            hasNFC = hasFeature(PackageManager.FEATURE_NFC),
            hasCellular = hasFeature(PackageManager.FEATURE_TELEPHONY),
            hasUSB = hasFeature(PackageManager.FEATURE_USB_HOST) ||
                     hasFeature(PackageManager.FEATURE_USB_ACCESSORY),

            // Audio
            hasAudioOutput = hasAudioOutput(),
            supportsLowLatencyAudio = hasFeature(PackageManager.FEATURE_AUDIO_LOW_LATENCY),
            supportsProAudio = hasFeature(PackageManager.FEATURE_AUDIO_PRO),

            // Accessibility
            supportsAccessibilityService = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N,
            hasVibrator = hasVibrator(),

            // Advanced
            supportsBiometrics = supportsBiometrics(),
            hasFingerprint = hasFeature(PackageManager.FEATURE_FINGERPRINT),
            hasFaceUnlock = hasFeature(PackageManager.FEATURE_FACE),
            supportsVR = hasFeature(PackageManager.FEATURE_VR_MODE_HIGH_PERFORMANCE),
            supportsAR = hasFeature("com.google.android.feature.AR")
        )
    }

    private fun hasFeature(feature: String): Boolean {
        return try {
            packageManager.hasSystemFeature(feature)
        } catch (e: Exception) {
            false
        }
    }

    private fun hasSensor(sensorType: Int): Boolean {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        return sensorManager?.getDefaultSensor(sensorType) != null
    }

    private fun hasAudioOutput(): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        return audioManager != null
    }

    private fun hasVibrator(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator != null
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.hasVibrator() == true
        }
    }

    private fun supportsBiometrics(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hasFeature(PackageManager.FEATURE_FINGERPRINT) ||
            hasFeature(PackageManager.FEATURE_FACE) ||
            hasFeature(PackageManager.FEATURE_IRIS)
        } else {
            false
        }
    }

    private fun detectScreenSize(): ScreenSize {
        val screenLayout = context.resources.configuration.screenLayout and
                          Configuration.SCREENLAYOUT_SIZE_MASK
        return when (screenLayout) {
            Configuration.SCREENLAYOUT_SIZE_SMALL -> ScreenSize.SMALL
            Configuration.SCREENLAYOUT_SIZE_NORMAL -> ScreenSize.NORMAL
            Configuration.SCREENLAYOUT_SIZE_LARGE -> ScreenSize.LARGE
            Configuration.SCREENLAYOUT_SIZE_XLARGE -> ScreenSize.XLARGE
            else -> ScreenSize.NORMAL
        }
    }

    private fun detectDensity(): DisplayDensity {
        val densityDpi = context.resources.displayMetrics.densityDpi
        return when {
            densityDpi <= DisplayMetrics.DENSITY_LOW -> DisplayDensity.LDPI
            densityDpi <= DisplayMetrics.DENSITY_MEDIUM -> DisplayDensity.MDPI
            densityDpi <= DisplayMetrics.DENSITY_HIGH -> DisplayDensity.HDPI
            densityDpi <= DisplayMetrics.DENSITY_XHIGH -> DisplayDensity.XHDPI
            densityDpi <= DisplayMetrics.DENSITY_XXHIGH -> DisplayDensity.XXHDPI
            else -> DisplayDensity.XXXHDPI
        }
    }

    /**
     * Check specific capability
     */
    fun hasCapability(capability: String): Boolean {
        return hasFeature(capability)
    }

    /**
     * Get capability report
     */
    fun generateCapabilityReport(): String {
        val caps = detectCapabilities()

        return """
        Device Capabilities Report
        ===========================

        Display:
        - Touch: ${caps.hasTouch}
        - Multitouch: ${caps.supportsMultitouch}
        - Screen Size: ${caps.screenSize}
        - Density: ${caps.density}

        Input:
        - Keyboard: ${caps.hasKeyboard}
        - Microphone: ${caps.hasMicrophone}
        - Camera: ${caps.hasCamera}

        Sensors:
        - Accelerometer: ${caps.hasAccelerometer}
        - Gyroscope: ${caps.hasGyroscope}
        - GPS: ${caps.hasGPS}

        Connectivity:
        - WiFi: ${caps.hasWifi}
        - Bluetooth: ${caps.hasBluetooth}
        - NFC: ${caps.hasNFC}
        - Cellular: ${caps.hasCellular}

        Advanced:
        - Biometrics: ${caps.supportsBiometrics}
        - VR: ${caps.supportsVR}
        - AR: ${caps.supportsAR}
        """.trimIndent()
    }
}
```

---

## 8.4 Platform Abstraction

### PlatformAdapter

```kotlin
/**
 * Platform-specific code abstraction
 */
class PlatformAdapter(private val context: Context) {

    private val deviceDetector = DeviceDetector(context)

    /**
     * Get platform-specific UI scale factor
     */
    fun getUIScaleFactor(): Float {
        return when (deviceDetector.detectDeviceType()) {
            DeviceType.PHONE -> 1.0f
            DeviceType.TABLET -> 1.2f
            DeviceType.TV -> 2.0f
            DeviceType.WEAR -> 0.8f
            DeviceType.XR_HEADSET -> 1.5f
            else -> 1.0f
        }
    }

    /**
     * Get platform-specific font scaling
     */
    fun getFontScaleFactor(): Float {
        return when (deviceDetector.detectDeviceType()) {
            DeviceType.PHONE -> 1.0f
            DeviceType.TABLET -> 1.1f
            DeviceType.TV -> 1.5f
            DeviceType.WEAR -> 0.9f
            else -> 1.0f
        }
    }

    /**
     * Get platform-specific spacing (dp)
     */
    fun getSpacingDp(): Int {
        return when (deviceDetector.detectDeviceType()) {
            DeviceType.PHONE -> 16
            DeviceType.TABLET -> 24
            DeviceType.TV -> 32
            DeviceType.WEAR -> 8
            else -> 16
        }
    }

    /**
     * Check if platform supports feature
     */
    fun supportsFeature(feature: PlatformFeature): Boolean {
        return when (feature) {
            PlatformFeature.SPLIT_SCREEN -> supportsSplitScreen()
            PlatformFeature.PICTURE_IN_PICTURE -> supportsPIP()
            PlatformFeature.MULTI_WINDOW -> supportsMultiWindow()
            PlatformFeature.FREEFORM -> supportsFreeform()
        }
    }

    private fun supportsSplitScreen(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
               deviceDetector.detectDeviceType() in listOf(
                   DeviceType.PHONE, DeviceType.TABLET, DeviceType.FOLDABLE, DeviceType.CHROMEBOOK
               )
    }

    private fun supportsPIP(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
               context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    private fun supportsMultiWindow(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    }

    private fun supportsFreeform(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
               deviceDetector.detectDeviceType() in listOf(
                   DeviceType.TABLET, DeviceType.CHROMEBOOK, DeviceType.TV
               )
    }
}

enum class PlatformFeature {
    SPLIT_SCREEN,
    PICTURE_IN_PICTURE,
    MULTI_WINDOW,
    FREEFORM
}
```

---

## 8.5 XR Device Support

### XRDeviceManager

```kotlin
/**
 * AR/VR device support
 */
class XRDeviceManager(private val context: Context) {

    companion object {
        private const val TAG = "XRDeviceManager"
    }

    enum class XRMode {
        AR,          // Augmented Reality
        VR,          // Virtual Reality
        MR,          // Mixed Reality
        PASSTHROUGH, // VR with camera passthrough
        NONE
    }

    data class XRCapabilities(
        val supportsAR: Boolean,
        val supportsVR: Boolean,
        val supportsMR: Boolean,
        val supportsPassthrough: Boolean,
        val has6DOF: Boolean,          // 6 degrees of freedom tracking
        val hasHandTracking: Boolean,
        val hasEyeTracking: Boolean,
        val hasSpatialAudio: Boolean
    )

    /**
     * Detect XR capabilities
     */
    fun detectXRCapabilities(): XRCapabilities {
        val pm = context.packageManager

        return XRCapabilities(
            supportsAR = checkARSupport(),
            supportsVR = checkVRSupport(),
            supportsMR = checkMRSupport(),
            supportsPassthrough = checkPassthroughSupport(),
            has6DOF = check6DOFSupport(),
            hasHandTracking = pm.hasSystemFeature("android.hardware.xr.hand_tracking"),
            hasEyeTracking = pm.hasSystemFeature("android.hardware.xr.eye_tracking"),
            hasSpatialAudio = pm.hasSystemFeature("android.hardware.xr.spatial_audio")
        )
    }

    private fun checkARSupport(): Boolean {
        val pm = context.packageManager
        return pm.hasSystemFeature("com.google.android.feature.AR") ||
               pm.hasSystemFeature("android.hardware.camera.ar")
    }

    private fun checkVRSupport(): Boolean {
        val pm = context.packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_VR_MODE) ||
               pm.hasSystemFeature(PackageManager.FEATURE_VR_MODE_HIGH_PERFORMANCE)
    }

    private fun checkMRSupport(): Boolean {
        // Mixed reality requires both AR and VR capabilities
        return checkARSupport() && checkVRSupport()
    }

    private fun checkPassthroughSupport(): Boolean {
        val pm = context.packageManager
        return pm.hasSystemFeature("android.hardware.xr.passthrough") &&
               pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    private fun check6DOFSupport(): Boolean {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        return sensorManager?.let {
            it.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null &&
            it.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null &&
            it.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null
        } ?: false
    }

    /**
     * Get optimal XR mode for current device
     */
    fun getOptimalXRMode(): XRMode {
        val capabilities = detectXRCapabilities()

        return when {
            capabilities.supportsMR -> XRMode.MR
            capabilities.supportsVR && capabilities.supportsPassthrough -> XRMode.PASSTHROUGH
            capabilities.supportsVR -> XRMode.VR
            capabilities.supportsAR -> XRMode.AR
            else -> XRMode.NONE
        }
    }
}
```

---

## 8.6 Accessibility Integration

### AccessibilityHelper

```kotlin
class AccessibilityHelper(private val context: Context) {

    companion object {
        private const val TAG = "AccessibilityHelper"
    }

    /**
     * Check if accessibility service is enabled
     */
    fun isAccessibilityServiceEnabled(serviceClass: Class<out AccessibilityService>): Boolean {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
            as? AccessibilityManager ?: return false

        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)

        val serviceName = ComponentName(context, serviceClass).flattenToString()

        while (colonSplitter.hasNext()) {
            if (colonSplitter.next().equals(serviceName, ignoreCase = true)) {
                return true
            }
        }

        return false
    }

    /**
     * Request accessibility service permission
     */
    fun requestAccessibilityPermission(serviceClass: Class<out AccessibilityService>) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    /**
     * Check if specific accessibility features are enabled
     */
    fun getAccessibilityFeatures(): AccessibilityFeatures {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE)
            as? AccessibilityManager

        return AccessibilityFeatures(
            isEnabled = accessibilityManager?.isEnabled == true,
            isTouchExplorationEnabled = accessibilityManager?.isTouchExplorationEnabled == true,
            isHighTextContrastEnabled = isHighTextContrastEnabled(),
            fontScale = getFontScale(),
            isAnimationsDisabled = areAnimationsDisabled()
        )
    }

    private fun isHighTextContrastEnabled(): Boolean {
        return try {
            Settings.Secure.getInt(
                context.contentResolver,
                "high_text_contrast_enabled",
                0
            ) == 1
        } catch (e: Exception) {
            false
        }
    }

    private fun getFontScale(): Float {
        return context.resources.configuration.fontScale
    }

    private fun areAnimationsDisabled(): Boolean {
        return try {
            val animatorScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            animatorScale == 0f
        } catch (e: Exception) {
            false
        }
    }
}

data class AccessibilityFeatures(
    val isEnabled: Boolean,
    val isTouchExplorationEnabled: Boolean,
    val isHighTextContrastEnabled: Boolean,
    val fontScale: Float,
    val isAnimationsDisabled: Boolean
)
```

---

## 8.7 Integration Guide

### Basic Usage

```kotlin
// 1. Inject DeviceManager
@Inject lateinit var deviceManager: DeviceManager

// 2. Detect device type
val deviceType = deviceManager.getDeviceType()
val formFactor = deviceManager.getFormFactor()

when (deviceType) {
    DeviceType.PHONE -> setupPhoneUI()
    DeviceType.TABLET -> setupTabletUI()
    DeviceType.TV -> setupTVUI()
    DeviceType.WEAR -> setupWearUI()
    DeviceType.XR_HEADSET -> setupXRUI()
}

// 3. Check capabilities
val capabilities = deviceManager.getCapabilities()

if (capabilities.hasMicrophone) {
    enableVoiceInput()
}

if (capabilities.supportsVR) {
    showVROption()
}

// 4. Platform adaptation
val uiScale = deviceManager.getUIScaleFactor()
val spacing = deviceManager.getSpacingDp()
```

### Hilt Module

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DeviceManagerModule {

    @Provides
    @Singleton
    fun provideDeviceManager(
        @ApplicationContext context: Context
    ): DeviceManager {
        return DeviceManager(context)
    }

    @Provides
    @Singleton
    fun provideDeviceDetector(
        @ApplicationContext context: Context
    ): DeviceDetector {
        return DeviceDetector(context)
    }

    @Provides
    @Singleton
    fun provideCapabilityManager(
        @ApplicationContext context: Context
    ): CapabilityManager {
        return CapabilityManager(context)
    }
}
```

---

## Summary

The **DeviceManager Library** provides:

✅ **Device Detection** - Accurate identification of device types
✅ **Capability Management** - Comprehensive feature detection
✅ **Platform Abstraction** - Unified API across Android variants
✅ **XR Support** - AR/VR device detection and capabilities
✅ **Accessibility Integration** - Accessibility service management
✅ **Adaptive UI** - Platform-specific UI scaling and spacing

**Total Module Size:** ~15 source files, ~3,000 lines of Kotlin code

**Next Chapter:** [Chapter 9: VoiceKeyboard Library](09-VoiceKeyboard-Library.md)

---

**Document Status:** ✅ Complete (40 pages)
