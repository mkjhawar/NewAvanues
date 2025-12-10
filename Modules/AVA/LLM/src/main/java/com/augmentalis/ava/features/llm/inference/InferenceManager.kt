package com.augmentalis.ava.features.llm.inference

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages inference backend selection based on device state.
 *
 * Monitors:
 * - Battery level and charging status
 * - Thermal status (Android 10+)
 * - Network connectivity
 * - Cloud LLM configuration
 *
 * Decision logic prioritizes:
 * 1. User experience (fast responses)
 * 2. Battery life (prefer cloud when low)
 * 3. Device health (prevent overheating)
 *
 * @see ADR-013: Self-Learning NLU with LLM-as-Teacher Architecture
 */
@Singleton
class InferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        /** Battery threshold below which to prefer cloud */
        const val BATTERY_LOW_THRESHOLD = 30

        /** Battery threshold below which cloud is required */
        const val BATTERY_CRITICAL_THRESHOLD = 15

        /** SharedPreferences name for settings */
        const val PREFS_NAME = "ava_settings"

        /** Key for cloud API key */
        const val KEY_CLOUD_API_KEY = "cloud_api_key"

        /** Key for cloud provider */
        const val KEY_CLOUD_PROVIDER = "cloud_provider"

        /** Key for cloud enabled flag */
        const val KEY_CLOUD_ENABLED = "cloud_enabled"
    }

    /**
     * Available inference backends.
     */
    enum class InferenceBackend {
        /** On-device LLM (Gemma 2B, etc.) */
        LOCAL_LLM,

        /** Cloud LLM (OpenAI, Groq, etc.) */
        CLOUD_LLM,

        /** Defer until conditions improve */
        QUEUED,

        /** Use NLU result even if low confidence (battery critical) */
        NLU_ONLY
    }

    /**
     * Current device state snapshot.
     */
    data class DeviceState(
        val batteryPercent: Int,
        val isCharging: Boolean,
        val thermalStatus: Int,
        val hasNetwork: Boolean,
        val hasCloudConfig: Boolean,
        val isLowPowerMode: Boolean
    ) {
        /** Battery below low threshold and not charging */
        val isBatteryLow: Boolean
            get() = batteryPercent < BATTERY_LOW_THRESHOLD && !isCharging

        /** Battery below critical threshold and not charging */
        val isBatteryCritical: Boolean
            get() = batteryPercent < BATTERY_CRITICAL_THRESHOLD && !isCharging

        /** Device is thermal throttling */
        val isThermalThrottling: Boolean
            get() = thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE

        /** Device thermal is critical - may shut down */
        val isThermalCritical: Boolean
            get() = thermalStatus >= PowerManager.THERMAL_STATUS_SEVERE

        /** Cloud is available and configured */
        val canUseCloud: Boolean
            get() = hasNetwork && hasCloudConfig
    }

    /**
     * Select optimal inference backend based on current device state.
     *
     * Priority:
     * 1. Thermal critical -> QUEUED (prevent damage)
     * 2. Thermal throttling + has cloud -> CLOUD_LLM
     * 3. Battery critical + has cloud -> CLOUD_LLM
     * 4. Battery low + has cloud -> CLOUD_LLM
     * 5. Normal conditions -> LOCAL_LLM
     * 6. No cloud + struggling -> LOCAL_LLM (best effort)
     *
     * @return Selected backend
     */
    fun selectBackend(): InferenceBackend {
        val state = getDeviceState()

        Timber.d(
            "Device state: battery=${state.batteryPercent}%%, " +
            "charging=${state.isCharging}, thermal=${state.thermalStatus}, " +
            "network=${state.hasNetwork}, cloud=${state.hasCloudConfig}"
        )

        return when {
            // Critical thermal - cannot safely run local inference
            state.isThermalCritical -> {
                Timber.w("Thermal critical - queuing request or using cloud")
                if (state.canUseCloud) {
                    InferenceBackend.CLOUD_LLM
                } else {
                    InferenceBackend.QUEUED
                }
            }

            // Thermal throttling - prefer cloud to let device cool
            state.isThermalThrottling && state.canUseCloud -> {
                Timber.i("Thermal throttling - using cloud LLM")
                InferenceBackend.CLOUD_LLM
            }

            // Battery critical - must conserve
            state.isBatteryCritical -> {
                if (state.canUseCloud) {
                    Timber.i("Battery critical - using cloud LLM")
                    InferenceBackend.CLOUD_LLM
                } else {
                    Timber.w("Battery critical, no cloud - NLU only mode")
                    InferenceBackend.NLU_ONLY
                }
            }

            // Battery low - prefer cloud if available
            state.isBatteryLow && state.canUseCloud -> {
                Timber.i("Battery low - using cloud LLM")
                InferenceBackend.CLOUD_LLM
            }

            // Low power mode enabled by user - respect their choice
            state.isLowPowerMode && state.canUseCloud -> {
                Timber.i("Low power mode - using cloud LLM")
                InferenceBackend.CLOUD_LLM
            }

            // Normal conditions - use local for privacy
            else -> {
                Timber.d("Normal conditions - using local LLM")
                InferenceBackend.LOCAL_LLM
            }
        }
    }

    /**
     * Get current device state snapshot.
     *
     * @return DeviceState with current battery, thermal, network info
     */
    fun getDeviceState(): DeviceState {
        // Battery info
        val batteryStatus = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, 50) ?: 50
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPercent = (level * 100) / scale

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                         status == BatteryManager.BATTERY_STATUS_FULL

        // Thermal status (Android 10+)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val thermalStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            powerManager.currentThermalStatus
        } else {
            PowerManager.THERMAL_STATUS_NONE
        }

        // Network connectivity
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
        val hasNetwork = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        // Cloud config
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasCloudConfig = prefs.getBoolean(KEY_CLOUD_ENABLED, false) &&
                             prefs.getString(KEY_CLOUD_API_KEY, "")?.isNotBlank() == true

        // Low power mode
        val isLowPowerMode = powerManager.isPowerSaveMode

        return DeviceState(
            batteryPercent = batteryPercent,
            isCharging = isCharging,
            thermalStatus = thermalStatus,
            hasNetwork = hasNetwork,
            hasCloudConfig = hasCloudConfig,
            isLowPowerMode = isLowPowerMode
        )
    }

    /**
     * Check if cloud LLM should be suggested to user.
     *
     * @return True if device is frequently struggling and cloud could help
     */
    fun shouldSuggestCloudSetup(): Boolean {
        val state = getDeviceState()
        return !state.hasCloudConfig &&
               (state.isBatteryLow || state.isThermalThrottling) &&
               state.hasNetwork
    }

    /**
     * Get user-friendly message about current inference mode.
     *
     * @return Status message or null if normal operation
     */
    fun getInferenceStatusMessage(): String? {
        val state = getDeviceState()
        return when {
            state.isThermalCritical && !state.canUseCloud ->
                "Device is too warm. Please let it cool down."

            state.isThermalThrottling && !state.canUseCloud ->
                "Device is warm. Consider adding Cloud AI for better performance."

            state.isBatteryCritical && !state.canUseCloud ->
                "Battery very low. Responses may be limited."

            state.isBatteryLow && !state.canUseCloud ->
                "Battery low. Add Cloud AI backup for faster responses."

            state.isThermalThrottling && state.canUseCloud ->
                "Using Cloud AI while device cools down."

            state.isBatteryLow && state.canUseCloud ->
                "Using Cloud AI to save battery."

            else -> null
        }
    }

    /**
     * Check if local LLM is currently usable.
     *
     * @return True if device can run local inference
     */
    fun isLocalLLMUsable(): Boolean {
        val state = getDeviceState()
        return !state.isThermalCritical &&
               !state.isBatteryCritical
    }

    /**
     * Get battery percentage for UI display.
     *
     * @return Battery percentage 0-100
     */
    fun getBatteryPercent(): Int {
        return getDeviceState().batteryPercent
    }

    /**
     * Check if device is currently charging.
     *
     * @return True if charging or full
     */
    fun isCharging(): Boolean {
        return getDeviceState().isCharging
    }
}
