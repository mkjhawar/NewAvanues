package com.augmentalis.ava.platform

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings as AndroidSettings
import java.util.Locale

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
