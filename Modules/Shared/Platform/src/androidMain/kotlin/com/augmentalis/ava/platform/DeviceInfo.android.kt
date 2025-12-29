package com.augmentalis.ava.platform

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import android.util.DisplayMetrics
import android.view.WindowManager
import java.net.NetworkInterface
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

/**
 * Android implementation of DeviceInfo.
 */
actual class DeviceInfo actual constructor() {
    private lateinit var context: Context

    internal fun init(context: Context) {
        this.context = context.applicationContext
    }

    actual fun getPlatform(): PlatformType {
        return PlatformType.ANDROID
    }

    actual fun getManufacturer(): String {
        return Build.MANUFACTURER
    }

    actual fun getModel(): String {
        return Build.MODEL
    }

    actual fun getOsVersion(): String {
        return Build.VERSION.RELEASE
    }

    actual fun getSdkVersion(): Int {
        return Build.VERSION.SDK_INT
    }

    actual fun getMemoryInfo(): MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        return MemoryInfo(
            totalMemory = memInfo.totalMem,
            availableMemory = memInfo.availMem
        )
    }

    actual fun getBatteryInfo(): BatteryInfo {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val level = batteryIntent?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) {
                (level * 100 / scale)
            } else {
                -1
            }
        } ?: -1

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isPowerSave = powerManager.isPowerSaveMode

        return BatteryInfo(
            level = level,
            isCharging = isCharging,
            isPowerSaveMode = isPowerSave
        )
    }

    actual fun isLowMemory(): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.lowMemory
    }

    actual fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "unknown"
        } catch (e: PackageManager.NameNotFoundException) {
            "unknown"
        }
    }

    actual fun getAppVersionCode(): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            0L
        }
    }

    actual fun getDeviceId(): String {
        return AndroidSettings.Secure.getString(
            context.contentResolver,
            AndroidSettings.Secure.ANDROID_ID
        ) ?: "unknown"
    }

    actual fun getLocale(): String {
        return Locale.getDefault().toLanguageTag()
    }

    actual fun hasFeature(feature: String): Boolean {
        val pm = context.packageManager
        return when (feature) {
            DeviceFeatures.FEATURE_MICROPHONE -> pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
            DeviceFeatures.FEATURE_CAMERA -> pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
            DeviceFeatures.FEATURE_BLUETOOTH -> pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
            DeviceFeatures.FEATURE_WIFI -> pm.hasSystemFeature(PackageManager.FEATURE_WIFI)
            DeviceFeatures.FEATURE_NFC -> pm.hasSystemFeature(PackageManager.FEATURE_NFC)
            DeviceFeatures.FEATURE_BIOMETRICS -> pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
            DeviceFeatures.FEATURE_TELEPHONY -> pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
            else -> false
        }
    }

    // ==================== FINGERPRINTING ====================

    private val fingerprintVersion = 1
    private var cachedFingerprint: DeviceFingerprint? = null

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("laas_fingerprint", Context.MODE_PRIVATE)
    }

    private val windowManager: WindowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private val sensorManager: SensorManager? by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    }

    actual fun getFingerprint(): DeviceFingerprint {
        cachedFingerprint?.let { return it }

        val components = mutableListOf<String>()

        // 1. Android ID (primary - most stable)
        val androidId = getAndroidId()
        val androidIdPresent = androidId.isNotEmpty()
        if (androidIdPresent) {
            components.add("aid:${sha256(androidId)}")
        }

        // 2. Build fingerprint (very stable)
        val buildInfo = collectBuildInfo()
        val buildPresent = buildInfo.isNotEmpty()
        components.add("build:${sha256(buildInfo)}")

        // 3. MAC address (Android <29/Q only - blocked after)
        var macPresent = false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            getMacAddress()?.let { mac ->
                components.add("mac:${sha256(mac)}")
                macPresent = true
            }
        }

        // 4. Hardware characteristics
        val hardware = collectHardwareInfo()
        components.add("hw:${sha256(hardware)}")

        // 5. Display characteristics
        val display = collectDisplayInfo()
        components.add("disp:${sha256(display)}")

        // 6. Sensor configuration (entropy)
        val sensors = collectSensorInfo()
        if (sensors.isNotEmpty()) {
            components.add("sens:${sha256(sensors)}")
        }

        // 7. Installation ID (consistency fallback)
        val installId = getOrCreateInstallId()
        components.add("inst:${sha256(installId)}")

        // Combine all components
        val combined = "v$fingerprintVersion:" + components.joinToString("|")
        val fingerprint = sha256(combined)

        val result = DeviceFingerprint(
            fingerprint = fingerprint,
            fingerprintShort = fingerprint.take(16).uppercase(),
            version = fingerprintVersion,
            platform = PlatformType.ANDROID,
            deviceType = getDeviceType(),
            isStable = androidIdPresent && buildPresent,
            generatedAt = System.currentTimeMillis()
        )

        cachedFingerprint = result
        return result
    }

    actual fun getFingerprintString(): String = getFingerprint().fingerprint

    actual fun getFingerprintShort(): String = getFingerprint().fingerprintShort

    actual fun getFingerprintDebugInfo(): FingerprintDebugInfo {
        return FingerprintDebugInfo(
            primaryIdPresent = getAndroidId().isNotEmpty(),
            buildInfoPresent = Build.FINGERPRINT.isNotEmpty(),
            macAddressPresent = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && getMacAddress() != null,
            hardwareInfoPresent = true,
            displayInfoPresent = true,
            installIdPresent = getOrCreateInstallId().isNotEmpty()
        )
    }

    actual fun getDeviceType(): DeviceType {
        val pm = context.packageManager
        return when {
            pm.hasSystemFeature(PackageManager.FEATURE_WATCH) -> DeviceType.WATCH
            pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK) -> DeviceType.TV
            pm.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE) -> DeviceType.AUTOMOTIVE
            pm.hasSystemFeature("android.hardware.type.xr") ||
            pm.hasSystemFeature("android.software.xr") -> DeviceType.XR
            isTablet() -> DeviceType.TABLET
            else -> DeviceType.PHONE
        }
    }

    // ==================== FINGERPRINT HELPERS ====================

    @SuppressLint("HardwareIds")
    private fun getAndroidId(): String {
        return try {
            AndroidSettings.Secure.getString(
                context.contentResolver,
                AndroidSettings.Secure.ANDROID_ID
            ) ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun collectBuildInfo(): String {
        return buildString {
            append(Build.FINGERPRINT)
            append(Build.BOARD)
            append(Build.BOOTLOADER)
            append(Build.BRAND)
            append(Build.DEVICE)
            append(Build.DISPLAY)
            append(Build.HARDWARE)
            append(Build.HOST)
            append(Build.ID)
            append(Build.MANUFACTURER)
            append(Build.MODEL)
            append(Build.PRODUCT)
            append(Build.TAGS)
            append(Build.TYPE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                append(Build.SOC_MANUFACTURER)
                append(Build.SOC_MODEL)
            }
        }
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    private fun getMacAddress(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return null

        return try {
            // Try WifiManager
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as? WifiManager
            @Suppress("DEPRECATION")
            val mac = wifiManager?.connectionInfo?.macAddress

            if (mac != null && mac != "02:00:00:00:00:00") {
                return mac
            }

            // Fallback: NetworkInterface
            NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { ni ->
                val hwAddr = ni.hardwareAddress
                if (hwAddr != null && hwAddr.isNotEmpty()) {
                    val macStr = hwAddr.joinToString(":") { "%02X".format(it) }
                    if (macStr != "00:00:00:00:00:00" && macStr != "02:00:00:00:00:00") {
                        return macStr
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun collectHardwareInfo(): String {
        val pm = context.packageManager
        return buildString {
            append(Build.SUPPORTED_ABIS.joinToString(","))
            append(Runtime.getRuntime().availableProcessors())
            append(pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH))
            append(pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
            append(pm.hasSystemFeature(PackageManager.FEATURE_NFC))
            append(pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT))
            append(pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY))
            append(pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY))
            append(pm.hasSystemFeature(PackageManager.FEATURE_WIFI))
            append(pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER))
            append(pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE))
        }
    }

    private fun collectDisplayInfo(): String {
        val metrics = getDisplayMetrics()
        return buildString {
            append(metrics.widthPixels)
            append(metrics.heightPixels)
            append(metrics.densityDpi)
            append(metrics.density.toInt())
            append(metrics.xdpi.toInt())
            append(metrics.ydpi.toInt())
        }
    }

    private fun collectSensorInfo(): String {
        return buildString {
            sensorManager?.getSensorList(Sensor.TYPE_ALL)?.forEach { sensor ->
                append(sensor.type)
                append(sensor.vendor?.hashCode() ?: 0)
                append(sensor.version)
            }
        }
    }

    private fun getOrCreateInstallId(): String {
        val key = "install_id"
        var id = prefs.getString(key, null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(key, id).apply()
        }
        return id
    }

    private fun getDisplayMetrics(): DisplayMetrics {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            DisplayMetrics().apply {
                widthPixels = bounds.width()
                heightPixels = bounds.height()
                density = context.resources.displayMetrics.density
                densityDpi = context.resources.displayMetrics.densityDpi
                xdpi = context.resources.displayMetrics.xdpi
                ydpi = context.resources.displayMetrics.ydpi
            }
        } else {
            @Suppress("DEPRECATION")
            DisplayMetrics().also { windowManager.defaultDisplay.getRealMetrics(it) }
        }
    }

    private fun isTablet(): Boolean {
        val metrics = getDisplayMetrics()
        val widthInches = metrics.widthPixels / metrics.xdpi
        val heightInches = metrics.heightPixels / metrics.ydpi
        val diagonal = kotlin.math.sqrt((widthInches * widthInches + heightInches * heightInches).toDouble())
        return diagonal >= 7.0
    }

    private fun sha256(input: String): String {
        if (input.isEmpty()) return ""
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    companion object {
        private var appContext: Context? = null

        fun initialize(context: Context) {
            appContext = context.applicationContext
        }

        internal fun getContext(): Context {
            return appContext ?: throw IllegalStateException(
                "DeviceInfo not initialized. Call DeviceInfo.initialize(context) in Application.onCreate()"
            )
        }
    }
}

/**
 * Android factory for DeviceInfo.
 */
actual object DeviceInfoFactory {
    actual fun create(): DeviceInfo {
        return DeviceInfo().apply {
            init(DeviceInfo.getContext())
        }
    }
}
