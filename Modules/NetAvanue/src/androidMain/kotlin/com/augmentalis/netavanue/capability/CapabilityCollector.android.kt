package com.augmentalis.netavanue.capability

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import com.augmentalis.netavanue.signaling.NetworkType

/**
 * Android implementation of [CapabilityCollector].
 *
 * Gathers device hardware info from Android system services:
 * - CPU cores from Runtime.getRuntime().availableProcessors()
 * - RAM from ActivityManager.MemoryInfo
 * - Battery from BatteryManager sticky broadcast
 * - Network from ConnectivityManager + NetworkCapabilities
 * - Screen dimensions from WindowManager
 */
actual class CapabilityCollector {
    private var context: Context? = null

    /** Must be called with an application context before collect(). */
    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    actual suspend fun collect(): DeviceCapability {
        val ctx = context ?: return fallbackCapability()

        val cpuCores = Runtime.getRuntime().availableProcessors()
        val ramMb = getAvailableRam(ctx)
        val (batteryPercent, isCharging) = getBatteryInfo(ctx)
        val (networkType, bandwidthMbps) = getNetworkInfo(ctx)
        val (screenWidth, screenHeight) = getScreenSize(ctx)

        return DeviceCapability(
            cpuCores = cpuCores,
            ramMb = ramMb,
            batteryPercent = batteryPercent,
            isCharging = isCharging,
            networkType = networkType,
            bandwidthMbps = bandwidthMbps,
            deviceType = if (screenWidth >= 600) "TABLET" else "PHONE",
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            supportedCodecs = getSupportedCodecs(),
            installedModules = emptyList(), // Populated by app layer
        )
    }

    private fun getAvailableRam(ctx: Context): Int {
        val activityManager = ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memInfo)
        return (memInfo.totalMem / (1024 * 1024)).toInt()
    }

    private fun getBatteryInfo(ctx: Context): Pair<Int, Boolean> {
        val batteryIntent = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else 50
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        return percent to charging
    }

    private fun getNetworkInfo(ctx: Context): Pair<NetworkType, Int> {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = cm?.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }

        val type = when {
            caps == null -> NetworkType.UNKNOWN
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            else -> NetworkType.UNKNOWN
        }

        val bandwidth = caps?.linkDownstreamBandwidthKbps?.let { it / 1000 } ?: 0
        return type to bandwidth
    }

    private fun getScreenSize(ctx: Context): Pair<Int, Int> {
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm?.defaultDisplay?.getRealMetrics(metrics)
        return metrics.widthPixels to metrics.heightPixels
    }

    private fun getSupportedCodecs(): List<String> {
        val codecs = mutableListOf("VP8", "VP9", "H264", "OPUS")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) codecs.add("AV1")
        return codecs
    }

    private fun fallbackCapability() = DeviceCapability(
        cpuCores = Runtime.getRuntime().availableProcessors(),
        ramMb = 4096,
        batteryPercent = 50,
        isCharging = false,
        networkType = NetworkType.UNKNOWN,
        bandwidthMbps = 0,
        deviceType = "PHONE",
        screenWidth = 1080,
        screenHeight = 2400,
        supportedCodecs = listOf("VP8", "H264", "OPUS"),
        installedModules = emptyList(),
    )
}
