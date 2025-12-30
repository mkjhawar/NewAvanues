package com.augmentalis.webavanue.feature.xr

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Android implementation of XR performance monitoring.
 *
 * Extends CommonPerformanceMonitor with Android-specific battery,
 * temperature, and thermal status reading.
 *
 * @param context Android Context for system services
 * @param coroutineScope Scope for monitoring coroutine
 */
class XRPerformanceMonitor(
    private val context: Context,
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : CommonPerformanceMonitor(coroutineScope) {

    override fun getBatteryLevel(): Int {
        val batteryStatus = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        return if (level >= 0 && scale > 0) {
            (level * 100 / scale)
        } else {
            100
        }
    }

    override fun getBatteryTemperature(): Float {
        val batteryStatus = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val temp = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1

        return if (temp >= 0) {
            temp / 10f // Temperature is in tenths of degree Celsius
        } else {
            0f
        }
    }

    override fun getThermalStatus(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            return powerManager?.currentThermalStatus ?: 0
        }
        return 0
    }

    override fun currentTimeMillis(): Long {
        return System.currentTimeMillis()
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        stopMonitoring()
        coroutineScope.cancel()
    }
}
