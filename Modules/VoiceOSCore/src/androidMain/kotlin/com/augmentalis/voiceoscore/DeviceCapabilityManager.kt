package com.augmentalis.voiceoscore

import android.os.Build
import android.app.ActivityManager
import android.content.Context

/**
 * Android implementation of DeviceCapabilityManager.
 *
 * Detects device capability using available RAM, CPU cores, and SDK version
 * as proxies for device performance.
 */
actual object DeviceCapabilityManager {

    actual enum class DeviceSpeed {
        FAST,
        MEDIUM,
        SLOW
    }

    private var cachedSpeed: DeviceSpeed? = null
    private var userOverrideDebounce: Long? = null
    private var contextRef: Context? = null

    /**
     * FIX: Increased debounce values to prevent continuous processing that starves voice recognition.
     *
     * The previous values (100/200/300ms) were too low for apps like Device Info, YouTube,
     * and My Files that generate rapid continuous accessibility events. This caused:
     * 1. Continuous command generation flooding Dispatchers.Default
     * 2. Continuous speech engine grammar updates blocking voice recognition
     * 3. Collector coroutine starvation despite dedicated dispatcher
     *
     * SPEECH_ENGINE_UPDATE is particularly critical - grammar compilation is expensive
     * and blocks the recognizer. We now use longer debounces and allow skipping.
     */
    private val fastTimings = mapOf(
        TimingOperation.CONTENT_CHANGE to TimingConfig(debounceMs = 250, minIntervalMs = 150, canSkip = true),
        TimingOperation.SCROLL to TimingConfig(debounceMs = 150, minIntervalMs = 100, canSkip = true),
        TimingOperation.FULL_SCRAPE to TimingConfig(debounceMs = 400, minIntervalMs = 250, canSkip = false),
        TimingOperation.INCREMENTAL_UPDATE to TimingConfig(debounceMs = 200, minIntervalMs = 125, canSkip = true),
        TimingOperation.OVERLAY_REFRESH to TimingConfig(debounceMs = 100, minIntervalMs = 50, canSkip = true),
        TimingOperation.SPEECH_ENGINE_UPDATE to TimingConfig(debounceMs = 500, minIntervalMs = 400, canSkip = true)
    )

    private val mediumTimings = mapOf(
        TimingOperation.CONTENT_CHANGE to TimingConfig(debounceMs = 500, minIntervalMs = 300, canSkip = true),
        TimingOperation.SCROLL to TimingConfig(debounceMs = 300, minIntervalMs = 200, canSkip = true),
        TimingOperation.FULL_SCRAPE to TimingConfig(debounceMs = 600, minIntervalMs = 400, canSkip = false),
        TimingOperation.INCREMENTAL_UPDATE to TimingConfig(debounceMs = 400, minIntervalMs = 250, canSkip = true),
        TimingOperation.OVERLAY_REFRESH to TimingConfig(debounceMs = 150, minIntervalMs = 75, canSkip = true),
        TimingOperation.SPEECH_ENGINE_UPDATE to TimingConfig(debounceMs = 1000, minIntervalMs = 800, canSkip = true)
    )

    private val slowTimings = mapOf(
        TimingOperation.CONTENT_CHANGE to TimingConfig(debounceMs = 800, minIntervalMs = 500, canSkip = true),
        TimingOperation.SCROLL to TimingConfig(debounceMs = 500, minIntervalMs = 300, canSkip = true),
        TimingOperation.FULL_SCRAPE to TimingConfig(debounceMs = 1000, minIntervalMs = 600, canSkip = false),
        TimingOperation.INCREMENTAL_UPDATE to TimingConfig(debounceMs = 600, minIntervalMs = 400, canSkip = true),
        TimingOperation.OVERLAY_REFRESH to TimingConfig(debounceMs = 200, minIntervalMs = 100, canSkip = true),
        TimingOperation.SPEECH_ENGINE_UPDATE to TimingConfig(debounceMs = 2000, minIntervalMs = 1500, canSkip = true)
    )

    /**
     * Initialize with Android context for accurate device detection.
     * Call this during app initialization.
     */
    fun init(context: Context) {
        contextRef = context.applicationContext
    }

    actual fun getContentDebounceMs(): Long {
        userOverrideDebounce?.let { return it }
        return getTimingConfig(TimingOperation.CONTENT_CHANGE).debounceMs
    }

    actual fun getScrollDebounceMs(): Long {
        // Scroll debounce is typically half of content debounce for responsiveness
        userOverrideDebounce?.let { return it / 2 }
        return getTimingConfig(TimingOperation.SCROLL).debounceMs
    }

    actual fun setUserDebounceMs(ms: Long?) {
        userOverrideDebounce = ms
    }

    actual fun getUserDebounceMs(): Long? {
        return userOverrideDebounce
    }

    actual fun getDeviceSpeed(): DeviceSpeed {
        cachedSpeed?.let { return it }

        val speed = detectDeviceSpeed()
        cachedSpeed = speed
        return speed
    }

    actual fun getMaxConcurrentOperations(): Int {
        return when (getDeviceSpeed()) {
            DeviceSpeed.FAST -> 4
            DeviceSpeed.MEDIUM -> 2
            DeviceSpeed.SLOW -> 1
        }
    }

    actual fun supportsAggressiveScanning(): Boolean {
        return getDeviceSpeed() == DeviceSpeed.FAST
    }

    actual fun resetCache() {
        cachedSpeed = null
    }

    actual fun getTimingConfig(operation: TimingOperation): TimingConfig {
        val timings = when (getDeviceSpeed()) {
            DeviceSpeed.FAST -> fastTimings
            DeviceSpeed.MEDIUM -> mediumTimings
            DeviceSpeed.SLOW -> slowTimings
        }
        return timings[operation] ?: TimingConfig(debounceMs = 200, minIntervalMs = 100)
    }

    /**
     * Detect device speed using multiple signals.
     */
    private fun detectDeviceSpeed(): DeviceSpeed {
        // Signal 1: Available RAM
        val memoryScore = getMemoryScore()

        // Signal 2: CPU cores
        val cpuScore = getCpuScore()

        // Signal 3: SDK version (newer = generally faster)
        val sdkScore = getSdkScore()

        // Weighted average (memory is most important for accessibility operations)
        val totalScore = (memoryScore * 0.5f) + (cpuScore * 0.3f) + (sdkScore * 0.2f)

        return when {
            totalScore >= 0.7f -> DeviceSpeed.FAST
            totalScore >= 0.4f -> DeviceSpeed.MEDIUM
            else -> DeviceSpeed.SLOW
        }
    }

    /**
     * Score based on available RAM.
     */
    private fun getMemoryScore(): Float {
        val context = contextRef
        if (context != null) {
            try {
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                if (activityManager != null) {
                    val memInfo = ActivityManager.MemoryInfo()
                    activityManager.getMemoryInfo(memInfo)
                    val totalRamGB = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)

                    return when {
                        totalRamGB >= 8.0 -> 1.0f   // 8GB+ = high-end
                        totalRamGB >= 6.0 -> 0.8f   // 6GB+ = good
                        totalRamGB >= 4.0 -> 0.6f   // 4GB+ = decent
                        totalRamGB >= 3.0 -> 0.4f   // 3GB+ = low-mid
                        totalRamGB >= 2.0 -> 0.2f   // 2GB+ = low-end
                        else -> 0.1f                 // <2GB = very low
                    }
                }
            } catch (e: Exception) {
                // Fall through to JVM-based detection
            }
        }

        // Fallback: JVM max memory (less accurate but works without context)
        val runtime = Runtime.getRuntime()
        val maxMemoryMB = runtime.maxMemory() / (1024 * 1024)

        return when {
            maxMemoryMB >= 512 -> 0.8f
            maxMemoryMB >= 256 -> 0.5f
            maxMemoryMB >= 128 -> 0.3f
            else -> 0.1f
        }
    }

    /**
     * Score based on CPU cores.
     */
    private fun getCpuScore(): Float {
        val cores = Runtime.getRuntime().availableProcessors()
        return when {
            cores >= 8 -> 1.0f
            cores >= 6 -> 0.8f
            cores >= 4 -> 0.6f
            cores >= 2 -> 0.4f
            else -> 0.2f
        }
    }

    /**
     * Score based on SDK version.
     */
    private fun getSdkScore(): Float {
        return when {
            Build.VERSION.SDK_INT >= 33 -> 1.0f   // Android 13+
            Build.VERSION.SDK_INT >= 31 -> 0.8f   // Android 12
            Build.VERSION.SDK_INT >= 29 -> 0.6f   // Android 10
            Build.VERSION.SDK_INT >= 26 -> 0.4f   // Android 8
            else -> 0.2f
        }
    }
}
