// Author: Manoj Jhawar
// Purpose: Comprehensive device information component with USB, camera, and peripheral detection

package com.augmentalis.devicemanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.hardware.Camera
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.hardware.input.InputManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.InputDevice
import android.view.WindowManager
import com.augmentalis.devicemanager.deviceinfo.cache.DeviceInfoCache
import com.augmentalis.devicemanager.usb.USBDeviceMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import kotlin.collections.emptyList
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Comprehensive Device Information Component
 * Provides complete device metrics, capabilities, and hardware detection
 * Now with intelligent caching and USB device monitoring for optimized performance
 */
class DeviceInfo(private val context: Context) {
    
    companion object {
        private const val TAG = "DeviceInfo"
        private const val CACHE_ENABLED = true // Can be made configurable
    }
    
    // System services
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
    private val inputManager = context.getSystemService(Context.INPUT_SERVICE) as? InputManager
    
    // Cache and monitoring components
    private val cache = DeviceInfoCache(context)
    private val usbMonitor = USBDeviceMonitor(context)
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Cached data
    private var cachedDeviceProfile: DeviceProfile? = null
    private var cachedHardwareInfo: HardwareInfo? = null
    private var cachedScalingProfile: ScalingProfile? = null
    private var cachedCameraInfo: List<CameraInfo>? = null
    private var isCacheLoaded = false
    
    private var displayMetrics: DisplayMetrics? = null
    
    // State flows for reactive updates
    private val _connectedDevices = MutableStateFlow<List<ConnectedDevice>>(emptyList())
    val connectedDevices: StateFlow<List<ConnectedDevice>> = _connectedDevices
    
    private val _externalDisplays = MutableStateFlow<List<ExternalDisplay>>(emptyList())
    val externalDisplays: StateFlow<List<ExternalDisplay>> = _externalDisplays
    
    fun initialize() {
        // Initialize with cache support
        coroutineScope.launch {
            initializeWithCache()
        }
    }
    
    private suspend fun initializeWithCache() {
        // Try to load from cache first
        if (CACHE_ENABLED && cache.isCacheValid()) {
            loadFromCache()
        } else {
            // Perform full scan on first run or when cache is invalid
            performFullDeviceScan()
        }
        
        // Always scan dynamic components (USB, displays)
        scanDynamicDevices()
        
        // Start USB monitoring
        startUSBMonitoring()
        
        // Register receivers for dynamic updates
        registerReceivers()
    }
    
    private suspend fun loadFromCache() {
        try {
            val cachedData = cache.loadCache()
            if (cachedData != null) {
                Log.d(TAG, "Loading device info from cache")
                
                // Restore cached static data
                cachedDeviceProfile = DeviceProfile(
                    manufacturer = cachedData.deviceProfile.manufacturer,
                    model = cachedData.deviceProfile.model,
                    brand = cachedData.deviceProfile.brand,
                    device = cachedData.deviceProfile.device,
                    product = cachedData.deviceProfile.product,
                    androidVersion = cachedData.deviceProfile.androidVersion,
                    apiLevel = cachedData.deviceProfile.apiLevel,
                    buildId = cachedData.deviceProfile.buildId,
                    buildType = cachedData.deviceProfile.buildType,
                    fingerprint = cachedData.deviceProfile.fingerprint
                )
                
                cachedHardwareInfo = HardwareInfo(
                    board = cachedData.hardwareInfo.board,
                    hardware = cachedData.hardwareInfo.hardware,
                    supportedAbis = cachedData.hardwareInfo.supportedAbis,
                    supportedAbis32 = Build.SUPPORTED_32_BIT_ABIS.toList(),
                    supportedAbis64 = Build.SUPPORTED_64_BIT_ABIS.toList(),
                    hasBluetooth = cachedData.hardwareInfo.hasBluetooth,
                    hasNfc = cachedData.hardwareInfo.hasNfc,
                    hasFingerprint = cachedData.hardwareInfo.hasFingerprint,
                    hasTelephony = cachedData.hardwareInfo.hasTelephony,
                    sensors = cachedData.sensors
                )
                
                cachedScalingProfile = ScalingProfile(
                    scaleFactor = cachedData.scalingProfile.scaleFactor,
                    fontScale = cachedData.scalingProfile.fontScale,
                    displayScale = cachedData.scalingProfile.displayScale,
                    uiMode = cachedData.scalingProfile.uiMode
                )
                
                // Restore camera info
                cachedCameraInfo = cachedData.cameras.map { camera ->
                    CameraInfo(
                        id = camera.id,
                        facing = CameraFacing.valueOf(camera.facing),
                        megapixels = camera.megapixels,
                        sensorSizeMm = null, // Not cached for simplicity
                        focalLengths = emptyList(), // Not cached for simplicity
                        hasFlash = camera.hasFlash,
                        hasOIS = camera.hasOIS,
                        hasDepthSensor = camera.hasDepthSensor,
                        hasRawSupport = camera.hasRawSupport,
                        isExternal = camera.isExternal
                    )
                }
                
                isCacheLoaded = true
                Log.d(TAG, "Successfully loaded device info from cache")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading from cache, performing full scan", e)
            performFullDeviceScan()
        }
    }
    
    private suspend fun performFullDeviceScan() {
        Log.d(TAG, "Performing full device scan")
        
        // Cache display metrics - using modern API for Android 11+ (API 30+)
        displayMetrics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+) - Modern approach
            val windowMetrics = windowManager.currentWindowMetrics
            val bounds = windowMetrics.bounds
            DisplayMetrics().apply {
                widthPixels = bounds.width()
                heightPixels = bounds.height()
                density = context.resources.displayMetrics.density
                densityDpi = context.resources.displayMetrics.densityDpi
                xdpi = context.resources.displayMetrics.xdpi
                ydpi = context.resources.displayMetrics.ydpi
            }
        } else {
            // Android 10 and below (API 29-) - Legacy approach
            @Suppress("DEPRECATION")
            DisplayMetrics().apply {
                windowManager.defaultDisplay.getRealMetrics(this)
            }
        }
        
        // Clear cached data to force fresh scan
        cachedDeviceProfile = null
        cachedHardwareInfo = null
        cachedScalingProfile = null
        cachedCameraInfo = null
        isCacheLoaded = false
        
        // Save to cache after full scan
        if (CACHE_ENABLED) {
            cache.saveCache(this)
        }
    }
    
    private fun scanDynamicDevices() {
        // Scan only dynamic components that change frequently
        scanConnectedDevices()
    }
    
    private fun startUSBMonitoring() {
        // Setup USB monitoring with auto-rescan
        usbMonitor.startMonitoring()
        
        // Register callback for USB events
        usbMonitor.registerCallback(object : USBDeviceMonitor.USBEventCallback {
            override fun onUSBDeviceAttached(device: USBDeviceMonitor.USBDeviceInfo) {
                Log.d(TAG, "USB device attached: ${device.productName}")
                if (cache.isAutoRescanOnUSBEnabled()) {
                    scanConnectedDevices()
                }
            }
            
            override fun onUSBDeviceDetached(device: USBDeviceMonitor.USBDeviceInfo) {
                Log.d(TAG, "USB device detached: ${device.productName}")
                scanConnectedDevices()
            }
            
            override fun onUSBPermissionGranted(device: USBDeviceMonitor.USBDeviceInfo) {
                Log.d(TAG, "USB permission granted: ${device.productName}")
            }
            
            override fun onUSBPermissionDenied(device: USBDeviceMonitor.USBDeviceInfo) {
                Log.d(TAG, "USB permission denied: ${device.productName}")
            }
            
            override fun onUSBScanCompleted(devices: List<USBDeviceMonitor.USBDeviceInfo>) {
                Log.d(TAG, "USB scan completed, found ${devices.size} devices")
            }
        })
    }
    
    // ========== BASIC DEVICE INFO ==========
    
    // Get device language
    fun getDeviceLanguage(): String {
        return Locale.getDefault().toLanguageTag() // Returns like "en-US"
    }
    
    // Get device locale
    fun getDeviceLocale(): Locale {
        return Locale.getDefault()
    }
    
    // Get all available locales
    fun getAvailableLocales(): List<String> {
        return Locale.getAvailableLocales().map { it.toLanguageTag() }
    }
    
    // Device Profile
    fun getDeviceProfile(): DeviceProfile {
        // Return cached data if available
        if (isCacheLoaded && cachedDeviceProfile != null) {
            return cachedDeviceProfile!!
        }
        
        // Generate fresh data and cache it
        val profile = DeviceProfile(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            brand = Build.BRAND,
            device = Build.DEVICE,
            product = Build.PRODUCT,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            buildId = Build.ID,
            buildType = Build.TYPE,
            fingerprint = Build.FINGERPRINT
        )
        
        cachedDeviceProfile = profile
        return profile
    }
    
    // Display Information
    fun getDisplayProfile(): DisplayProfile {
        val metrics = displayMetrics ?: getDisplayMetrics()
        
        val refreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ - Use display.refreshRate
            context.display?.refreshRate ?: 60f
        } else {
            // Android 10 and below
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.refreshRate
        }
        
        val rotation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ - Use display.rotation
            context.display?.rotation ?: 0
        } else {
            // Android 10 and below
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.rotation
        }
        
        return DisplayProfile(
            widthPixels = metrics.widthPixels,
            heightPixels = metrics.heightPixels,
            density = metrics.density,
            densityDpi = metrics.densityDpi,
            scaledDensity = context.resources.configuration.fontScale * metrics.density,
            xdpi = metrics.xdpi,
            ydpi = metrics.ydpi,
            refreshRate = refreshRate,
            rotation = rotation,
            diagonalInches = calculateDiagonalInches(metrics)
        )
    }
    
    // Hardware Capabilities
    fun getHardwareInfo(): HardwareInfo {
        // Return cached data if available
        if (isCacheLoaded && cachedHardwareInfo != null) {
            return cachedHardwareInfo!!
        }
        
        // Generate fresh data and cache it
        val hardware = HardwareInfo(
            board = Build.BOARD,
            hardware = Build.HARDWARE,
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            supportedAbis32 = Build.SUPPORTED_32_BIT_ABIS.toList(),
            supportedAbis64 = Build.SUPPORTED_64_BIT_ABIS.toList(),
            hasBluetooth = context.packageManager.hasSystemFeature("android.hardware.bluetooth"),
            hasNfc = context.packageManager.hasSystemFeature("android.hardware.nfc"),
            hasFingerprint = context.packageManager.hasSystemFeature("android.hardware.fingerprint"),
            hasTelephony = context.packageManager.hasSystemFeature("android.hardware.telephony"),
            sensors = getSensorList()
        )
        
        cachedHardwareInfo = hardware
        return hardware
    }
    
    // ========== DEVICE TYPE DETECTION ==========
    
    fun isTablet(): Boolean {
        val configuration = context.resources.configuration
        val screenLayout = configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        return screenLayout >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }
    
    fun isFoldable(): Boolean {
        // Check for foldable device characteristics
        return context.packageManager.hasSystemFeature("android.hardware.sensor.hinge_angle") ||
               Build.MANUFACTURER.equals("samsung", ignoreCase = true) && 
               (Build.MODEL.contains("fold", ignoreCase = true) || 
                Build.MODEL.contains("flip", ignoreCase = true))
    }
    
    fun isWearable(): Boolean {
        return context.packageManager.hasSystemFeature("android.hardware.type.watch") ||
               Build.DEVICE.contains("glass", ignoreCase = true) ||
               Build.MODEL.contains("glass", ignoreCase = true)
    }
    
    fun isXR(): Boolean {
        // Android XR detection (for future Android XR devices)
        return context.packageManager.hasSystemFeature("android.hardware.type.xr") ||
               context.packageManager.hasSystemFeature("android.software.xr") ||
               context.packageManager.hasSystemFeature("android.hardware.vr.high_performance") ||
               context.packageManager.hasSystemFeature("android.hardware.vr.headtracking") ||
               Build.DEVICE.contains("xr", ignoreCase = true) ||
               Build.MODEL.contains("quest", ignoreCase = true) || // Meta Quest
               Build.MODEL.contains("pico", ignoreCase = true) ||  // Pico XR
               Build.MODEL.contains("lynx", ignoreCase = true)     // Lynx R1
    }
    
    fun isTV(): Boolean {
        return context.packageManager.hasSystemFeature("android.software.leanback")
    }
    
    fun isAutomotive(): Boolean {
        return context.packageManager.hasSystemFeature("android.hardware.type.automotive")
    }
    
    // ========== EXTENDED DEVICE CAPABILITIES ==========
    
    /**
     * Get comprehensive camera information
     */
    fun getCameraInfo(): List<CameraInfo> {
        // Return cached data if available
        if (isCacheLoaded && cachedCameraInfo != null) {
            return cachedCameraInfo!!
        }
        
        val cameras = mutableListOf<CameraInfo>()
        
        try {
            cameraManager?.cameraIdList?.forEach { cameraId ->
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                val sensorSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                val megapixels = calculateMegapixels(characteristics)
                val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                val hasOIS = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)?.isNotEmpty() ?: false
                
                // Check for depth/ToF capabilities
                val hasDepth = capabilities?.contains(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT
                ) ?: false
                
                // Check for RAW support
                val hasRaw = capabilities?.contains(
                    CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW
                ) ?: false
                
                cameras.add(
                    CameraInfo(
                        id = cameraId,
                        facing = when(facing) {
                            CameraCharacteristics.LENS_FACING_FRONT -> CameraFacing.FRONT
                            CameraCharacteristics.LENS_FACING_BACK -> CameraFacing.BACK
                            CameraCharacteristics.LENS_FACING_EXTERNAL -> CameraFacing.EXTERNAL
                            else -> CameraFacing.UNKNOWN
                        },
                        megapixels = megapixels,
                        sensorSizeMm = sensorSize,
                        focalLengths = focalLengths?.toList() ?: emptyList(),
                        hasFlash = hasFlash,
                        hasOIS = hasOIS,
                        hasDepthSensor = hasDepth,
                        hasRawSupport = hasRaw,
                        isExternal = facing == CameraCharacteristics.LENS_FACING_EXTERNAL
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting camera info", e)
        }
        
        cachedCameraInfo = cameras
        return cameras
    }
    
    /**
     * Detect USB-C/DisplayPort connected devices
     */
    fun getUSBDevices(): List<USBDeviceInfo> {
        val devices = mutableListOf<USBDeviceInfo>()
        
        try {
            usbManager?.deviceList?.values?.forEach { device ->
                devices.add(
                    USBDeviceInfo(
                        deviceName = device.deviceName,
                        productName = device.productName ?: "Unknown",
                        manufacturerName = device.manufacturerName ?: "Unknown",
                        vendorId = device.vendorId,
                        productId = device.productId,
                        deviceClass = device.deviceClass,
                        deviceSubclass = device.deviceSubclass,
                        deviceProtocol = device.deviceProtocol,
                        interfaceCount = device.interfaceCount,
                        isDisplayLink = isDisplayLinkDevice(device),
                        isAudioDevice = isAudioDevice(device),
                        isVideoDevice = isVideoDevice(device),
                        isHID = isHIDDevice(device),
                        isMassStorage = isMassStorageDevice(device)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting USB devices", e)
        }
        
        return devices
    }
    
    /**
     * Get external display information (monitors, TVs, smart glasses)
     */
    fun getExternalDisplays(): List<ExternalDisplay> {
        val displays = mutableListOf<ExternalDisplay>()
        
        try {
            displayManager?.displays?.forEach { display ->
                if (display.displayId != Display.DEFAULT_DISPLAY) {
                    val metrics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        display.mode
                    } else {
                        null
                    }
                    
                    displays.add(
                        ExternalDisplay(
                            displayId = display.displayId,
                            name = display.name,
                            isValid = display.isValid,
                            width = metrics?.physicalWidth ?: 0,
                            height = metrics?.physicalHeight ?: 0,
                            refreshRate = metrics?.refreshRate ?: display.refreshRate,
                            flags = display.flags,
                            rotation = display.rotation,
                            isPresentation = display.flags and Display.FLAG_PRESENTATION != 0,
                            isPrivate = display.flags and Display.FLAG_PRIVATE != 0,
                            isSecure = display.flags and Display.FLAG_SECURE != 0,
                            isHDR = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) display.isHdr else false,
                            isWideColorGamut = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) display.isWideColorGamut else false,
                            connectionType = detectConnectionType(display)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting external displays", e)
        }
        
        return displays
    }
    
    /**
     * Get touchpad/input device information
     */
    fun getInputDevices(): List<InputDeviceInfo> {
        val devices = mutableListOf<InputDeviceInfo>()
        
        try {
            InputDevice.getDeviceIds().forEach { deviceId ->
                InputDevice.getDevice(deviceId)?.let { device ->
                    val sources = device.sources
                    
                    devices.add(
                        InputDeviceInfo(
                            id = device.id,
                            name = device.name,
                            descriptor = device.descriptor,
                            vendorId = device.vendorId,
                            productId = device.productId,
                            isVirtual = device.isVirtual,
                            hasMicrophone = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                device.hasMicrophone()
                            } else false,
                            hasVibrator = device.vibrator != null,
                            isTouchpad = sources and InputDevice.SOURCE_TOUCHPAD != 0,
                            isTouchscreen = sources and InputDevice.SOURCE_TOUCHSCREEN != 0,
                            isKeyboard = sources and InputDevice.SOURCE_KEYBOARD != 0,
                            isMouse = sources and InputDevice.SOURCE_MOUSE != 0,
                            isStylus = sources and InputDevice.SOURCE_STYLUS != 0,
                            isGamepad = sources and InputDevice.SOURCE_GAMEPAD != 0,
                            isJoystick = sources and InputDevice.SOURCE_JOYSTICK != 0,
                            keyboardType = device.keyboardType,
                            motionRanges = device.motionRanges.map { range ->
                                MotionRangeInfo(
                                    axis = range.axis,
                                    source = range.source,
                                    min = range.min,
                                    max = range.max,
                                    flat = range.flat,
                                    fuzz = range.fuzz,
                                    resolution = range.resolution
                                )
                            }
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting input devices", e)
        }
        
        return devices
    }
    
    /**
     * Check if device has 6DOF tracking capability
     */
    fun has6DOFTracking(): Boolean {
        // Check multiple indicators for 6DOF
        return context.packageManager.hasSystemFeature("android.hardware.vr.headtracking") ||
               context.packageManager.hasSystemFeature("android.hardware.sensor.accelerometer") &&
               context.packageManager.hasSystemFeature("android.hardware.sensor.gyroscope") &&
               context.packageManager.hasSystemFeature("android.hardware.sensor.compass") ||
               // Check for ARCore support
               context.packageManager.hasSystemFeature("android.hardware.camera.ar") ||
               // Check for XR-specific features
               context.packageManager.hasSystemFeature("android.hardware.type.xr") ||
               context.packageManager.hasSystemFeature("android.software.xr.spatial_tracking")
    }
    
    /**
     * Detect Samsung DeX mode
     */
    fun isDeXMode(): Boolean {
        return try {
            val config = context.resources.configuration
            val uiMode = config.uiMode and Configuration.UI_MODE_TYPE_MASK
            uiMode == Configuration.UI_MODE_TYPE_DESK ||
            // Check for Samsung-specific DeX indicator
            context.resources.configuration.semDesktopModeEnabled == 1
        } catch (e: NoSuchFieldError) {
            // Not a Samsung device or doesn't support DeX
            false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Detect desktop mode (generic Android desktop mode)
     */
    fun isDesktopMode(): Boolean {
        val config = context.resources.configuration
        val uiMode = config.uiMode and Configuration.UI_MODE_TYPE_MASK
        return uiMode == Configuration.UI_MODE_TYPE_DESK
    }
    
    /**
     * Check for wireless display support (Miracast, Chromecast)
     */
    fun hasWirelessDisplaySupport(): Boolean {
        return context.packageManager.hasSystemFeature("android.hardware.wifi.direct") ||
               context.packageManager.hasSystemFeature("android.software.leanback")
    }
    
    // Scaling Profile for UI
    fun getScalingProfile(): ScalingProfile {
        // Return cached data if available
        if (isCacheLoaded && cachedScalingProfile != null) {
            return cachedScalingProfile!!
        }
        
        val metrics = displayMetrics ?: getDisplayMetrics()
        
        val baselineDpi = 160f
        val scaleFactor = metrics.densityDpi / baselineDpi
        
        val displayScale = context.resources.configuration.fontScale
        
        val profile = ScalingProfile(
            scaleFactor = scaleFactor,
            fontScale = context.resources.configuration.fontScale,
            displayScale = displayScale,
            uiMode = when {
                isXR() -> "xr"
                isTV() -> "tv"
                isTablet() -> "tablet"
                isWearable() -> "wearable"
                else -> "phone"
            }
        )
        
        cachedScalingProfile = profile
        return profile
    }
    
    // ========== HELPER FUNCTIONS ==========
    
    private fun calculateDiagonalInches(metrics: DisplayMetrics): Float {
        val widthInches = metrics.widthPixels / metrics.xdpi
        val heightInches = metrics.heightPixels / metrics.ydpi
        return sqrt(widthInches.pow(2) + heightInches.pow(2))
    }
    
    private fun calculateMegapixels(characteristics: CameraCharacteristics): Float {
        val size = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        return if (size != null) {
            (size.width * size.height) / 1_000_000f
        } else {
            0f
        }
    }
    
    private fun getSensorList(): List<String> {
        return sensorManager?.getSensorList(Sensor.TYPE_ALL)?.map { 
            it.name 
        } ?: emptyList()
    }
    
    private fun getDisplayMetrics(): DisplayMetrics {
        return when {
            Build.VERSION.SDK_INT >= 35 -> {
                // Android 15+ (API 35+) - Future API support
                // Use the most modern approach, ready for stereoscopic displays
                val windowMetrics = windowManager.currentWindowMetrics
                val bounds = windowMetrics.bounds
                DisplayMetrics().apply {
                    widthPixels = bounds.width()
                    heightPixels = bounds.height()
                    density = context.resources.displayMetrics.density
                    densityDpi = context.resources.displayMetrics.densityDpi
                    xdpi = context.resources.displayMetrics.xdpi
                    ydpi = context.resources.displayMetrics.ydpi
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                // Android 11-14 (API 30-34)
                val windowMetrics = windowManager.currentWindowMetrics
                val bounds = windowMetrics.bounds
                DisplayMetrics().apply {
                    widthPixels = bounds.width()
                    heightPixels = bounds.height()
                    density = context.resources.displayMetrics.density
                    densityDpi = context.resources.displayMetrics.densityDpi
                    xdpi = context.resources.displayMetrics.xdpi
                    ydpi = context.resources.displayMetrics.ydpi
                }
            }
            else -> {
                // Android 10 and below (API 29-)
                @Suppress("DEPRECATION")
                DisplayMetrics().apply {
                    windowManager.defaultDisplay.getRealMetrics(this)
                }
            }
        }
    }
    
    private fun isDisplayLinkDevice(device: UsbDevice): Boolean {
        // DisplayLink vendor ID is 0x17e9
        return device.vendorId == 0x17e9 ||
               device.deviceClass == UsbConstants.USB_CLASS_VIDEO
    }
    
    private fun isAudioDevice(device: UsbDevice): Boolean {
        return device.deviceClass == UsbConstants.USB_CLASS_AUDIO
    }
    
    private fun isVideoDevice(device: UsbDevice): Boolean {
        return device.deviceClass == UsbConstants.USB_CLASS_VIDEO
    }
    
    private fun isHIDDevice(device: UsbDevice): Boolean {
        return device.deviceClass == UsbConstants.USB_CLASS_HID
    }
    
    private fun isMassStorageDevice(device: UsbDevice): Boolean {
        return device.deviceClass == UsbConstants.USB_CLASS_MASS_STORAGE
    }
    
    private fun detectConnectionType(display: Display): DisplayConnectionType {
        return when {
            display.name.contains("HDMI", ignoreCase = true) -> DisplayConnectionType.HDMI
            display.name.contains("DisplayPort", ignoreCase = true) -> DisplayConnectionType.DISPLAY_PORT
            display.name.contains("USB", ignoreCase = true) -> DisplayConnectionType.USB_C
            display.flags and Display.FLAG_PRESENTATION != 0 -> DisplayConnectionType.WIRELESS
            else -> DisplayConnectionType.UNKNOWN
        }
    }
    
    private fun scanConnectedDevices() {
        val devices = mutableListOf<ConnectedDevice>()
        
        // Scan USB devices
        getUSBDevices().forEach { usb ->
            devices.add(
                ConnectedDevice(
                    type = DeviceType.USB,
                    name = usb.productName,
                    manufacturer = usb.manufacturerName,
                    connectionType = ConnectionType.WIRED
                )
            )
        }
        
        // Scan external displays
        getExternalDisplays().forEach { display ->
            devices.add(
                ConnectedDevice(
                    type = DeviceType.DISPLAY,
                    name = display.name,
                    manufacturer = "Unknown",
                    connectionType = if (display.connectionType == DisplayConnectionType.WIRELESS) {
                        ConnectionType.WIRELESS
                    } else {
                        ConnectionType.WIRED
                    }
                )
            )
        }
        
        _connectedDevices.value = devices
    }
    
    private fun registerReceivers() {
        // USB device attachment/detachment
        val usbFilter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        context.registerReceiver(usbReceiver, usbFilter)
        
        // Display changes
        displayManager?.registerDisplayListener(displayListener, null)
    }
    
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            scanConnectedDevices()
        }
    }
    
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            scanConnectedDevices()
            _externalDisplays.value = getExternalDisplays()
        }
        
        override fun onDisplayRemoved(displayId: Int) {
            scanConnectedDevices()
            _externalDisplays.value = getExternalDisplays()
        }
        
        override fun onDisplayChanged(displayId: Int) {
            _externalDisplays.value = getExternalDisplays()
        }
    }
    
    /**
     * Force a full rescan of device information
     * This will clear the cache and perform a complete scan
     */
    fun forceRescan() {
        coroutineScope.launch {
            Log.d(TAG, "Forcing full device rescan")
            cache.clearCache()
            performFullDeviceScan()
            scanDynamicDevices()
        }
    }
    
    /**
     * Check if auto-rescan on USB is enabled
     */
    fun isAutoRescanEnabled(): Boolean {
        return cache.isAutoRescanOnUSBEnabled()
    }
    
    /**
     * Set auto-rescan on USB setting
     */
    fun setAutoRescan(enabled: Boolean) {
        cache.setAutoRescanOnUSB(enabled)
    }
    
    /**
     * Get the last scan timestamp
     */
    fun getLastScanTime(): Long {
        return cache.getLastScanTime()
    }
    
    fun release() {
        try {
            context.unregisterReceiver(usbReceiver)
            displayManager?.unregisterDisplayListener(displayListener)
            usbMonitor.stopMonitoring()
            coroutineScope.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing resources", e)
        }
    }
}

// ========== DATA CLASSES ==========

// Basic device info data classes
data class DeviceProfile(
    val manufacturer: String,
    val model: String,
    val brand: String,
    val device: String,
    val product: String,
    val androidVersion: String,
    val apiLevel: Int,
    val buildId: String,
    val buildType: String,
    val fingerprint: String,
    val language: String = Locale.getDefault().toLanguageTag(),
    val country: String = Locale.getDefault().country,
    val displayLanguage: String = Locale.getDefault().displayLanguage
)

data class DisplayProfile(
    val widthPixels: Int,
    val heightPixels: Int,
    val density: Float,
    val densityDpi: Int,
    val scaledDensity: Float,
    val xdpi: Float,
    val ydpi: Float,
    val refreshRate: Float,
    val rotation: Int,
    val diagonalInches: Float
)

data class HardwareInfo(
    val board: String,
    val hardware: String,
    val supportedAbis: List<String>,
    val supportedAbis32: List<String>,
    val supportedAbis64: List<String>,
    val hasBluetooth: Boolean,
    val hasNfc: Boolean,
    val hasFingerprint: Boolean,
    val hasTelephony: Boolean,
    val sensors: List<String>
)

data class ScalingProfile(
    val scaleFactor: Float,
    val fontScale: Float,
    val displayScale: Float,
    val uiMode: String
)

// Extended device info data classes
data class CameraInfo(
    val id: String,
    val facing: CameraFacing,
    val megapixels: Float,
    val sensorSizeMm: android.util.SizeF?,
    val focalLengths: List<Float>,
    val hasFlash: Boolean,
    val hasOIS: Boolean,
    val hasDepthSensor: Boolean,
    val hasRawSupport: Boolean,
    val isExternal: Boolean
)

enum class CameraFacing {
    FRONT, BACK, EXTERNAL, UNKNOWN
}

data class USBDeviceInfo(
    val deviceName: String,
    val productName: String,
    val manufacturerName: String,
    val vendorId: Int,
    val productId: Int,
    val deviceClass: Int,
    val deviceSubclass: Int,
    val deviceProtocol: Int,
    val interfaceCount: Int,
    val isDisplayLink: Boolean,
    val isAudioDevice: Boolean,
    val isVideoDevice: Boolean,
    val isHID: Boolean,
    val isMassStorage: Boolean
)

data class ExternalDisplay(
    val displayId: Int,
    val name: String,
    val isValid: Boolean,
    val width: Int,
    val height: Int,
    val refreshRate: Float,
    val flags: Int,
    val rotation: Int,
    val isPresentation: Boolean,
    val isPrivate: Boolean,
    val isSecure: Boolean,
    val isHDR: Boolean,
    val isWideColorGamut: Boolean,
    val connectionType: DisplayConnectionType
)

enum class DisplayConnectionType {
    HDMI, DISPLAY_PORT, USB_C, WIRELESS, UNKNOWN
}

data class InputDeviceInfo(
    val id: Int,
    val name: String,
    val descriptor: String,
    val vendorId: Int,
    val productId: Int,
    val isVirtual: Boolean,
    val hasMicrophone: Boolean,
    val hasVibrator: Boolean,
    val isTouchpad: Boolean,
    val isTouchscreen: Boolean,
    val isKeyboard: Boolean,
    val isMouse: Boolean,
    val isStylus: Boolean,
    val isGamepad: Boolean,
    val isJoystick: Boolean,
    val keyboardType: Int,
    val motionRanges: List<MotionRangeInfo>
)

data class MotionRangeInfo(
    val axis: Int,
    val source: Int,
    val min: Float,
    val max: Float,
    val flat: Float,
    val fuzz: Float,
    val resolution: Float
)

data class ConnectedDevice(
    val type: DeviceType,
    val name: String,
    val manufacturer: String,
    val connectionType: ConnectionType
)

enum class DeviceType {
    USB, DISPLAY, CAMERA, INPUT, AUDIO, UNKNOWN
}

enum class ConnectionType {
    WIRED, WIRELESS
}

// USB Class constants
object UsbConstants {
    const val USB_CLASS_AUDIO = 0x01
    const val USB_CLASS_HID = 0x03
    const val USB_CLASS_MASS_STORAGE = 0x08
    const val USB_CLASS_VIDEO = 0x0E
}

// Extension for Samsung DeX (requires reflection)
private val Configuration.semDesktopModeEnabled: Int
    get() = try {
        val field = this::class.java.getDeclaredField("semDesktopModeEnabled")
        field.getInt(this)
    } catch (e: Exception) {
        0
    }