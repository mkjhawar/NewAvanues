// Author: Manoj Jhawar
// Purpose: Android implementation of DeviceCapabilityProvider

package com.augmentalis.devicemanager

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import com.augmentalis.devicemanager.deviceinfo.detection.DeviceDetector
import com.augmentalis.devicemanager.profile.HardwareProfiler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android implementation of DeviceCapabilityProvider.
 *
 * Bridges the KMP capability interface with existing Android-specific
 * detection components (DeviceInfo, DeviceDetector, HardwareProfiler).
 */
class AndroidDeviceCapabilityProvider(
    private val context: Context
) : DeviceCapabilityProvider {

    companion object {
        private const val TAG = "AndroidDeviceCaps"
    }

    // Existing Android components for capability detection
    private val deviceInfo: DeviceInfo by lazy { DeviceInfo(context) }
    private val deviceDetector: DeviceDetector.DeviceCapabilities by lazy {
        DeviceDetector.getCapabilities(context)
    }
    private val hardwareProfiler: HardwareProfiler by lazy { HardwareProfiler(context) }

    // Cached capabilities
    @Volatile
    private var cachedCapabilities: DeviceCapabilities? = null

    override fun getKmpDeviceInfo(): KmpDeviceInfo {
        return KmpDeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            brand = Build.BRAND,
            device = Build.DEVICE,
            osVersion = Build.VERSION.RELEASE,
            osVersionCode = Build.VERSION.SDK_INT,
            deviceType = detectDeviceType()
        )
    }

    override fun getHardwareProfile(): HardwareProfile {
        val cpuCount = Runtime.getRuntime().availableProcessors()
        val cpuArch = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
        val cpuMaxFreq = getCpuMaxFrequency()

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val totalRamMb = (memInfo.totalMem / (1024 * 1024)).toInt()
        val availableRamMb = (memInfo.availMem / (1024 * 1024)).toInt()

        val internalStorageGb = try {
            val statFs = StatFs(Environment.getDataDirectory().path)
            (statFs.totalBytes / (1024 * 1024 * 1024)).toInt()
        } catch (e: Exception) {
            0
        }

        val gpuVendor = getGpuVendor()

        return HardwareProfile(
            cpuCores = cpuCount,
            cpuArchitecture = cpuArch,
            cpuMaxFrequencyMhz = cpuMaxFreq,
            totalRamMb = totalRamMb,
            availableRamMb = availableRamMb,
            gpuVendor = gpuVendor,
            gpuRenderer = Build.HARDWARE,
            internalStorageGb = internalStorageGb
        )
    }

    override fun getNetworkCapabilities(): NetworkCapabilities {
        return NetworkCapabilities(
            hasBluetooth = deviceDetector.network.hasBluetooth,
            hasBluetoothLE = deviceDetector.network.hasBluetoothLE,
            hasWiFi = deviceDetector.network.hasWiFi,
            hasWiFiDirect = deviceDetector.network.hasWiFiDirect,
            hasWiFiAware = deviceDetector.network.hasWiFiAware,
            hasNfc = deviceDetector.network.hasNfc,
            hasUwb = deviceDetector.network.hasUwb,
            hasCellular = deviceDetector.network.hasCellular,
            has5G = deviceDetector.network.has5G
        )
    }

    override fun getSensorCapabilities(): SensorCapabilities {
        return SensorCapabilities(
            hasAccelerometer = deviceDetector.sensors.hasAccelerometer,
            hasGyroscope = deviceDetector.sensors.hasGyroscope,
            hasMagnetometer = deviceDetector.sensors.hasMagnetometer,
            hasBarometer = deviceDetector.sensors.hasBarometer,
            hasProximity = deviceDetector.sensors.hasProximity,
            hasLight = deviceDetector.sensors.hasLight,
            hasStepCounter = deviceDetector.sensors.hasStepCounter,
            hasHeartRate = deviceDetector.sensors.hasHeartRate,
            totalSensorCount = deviceDetector.sensors.totalSensorCount
        )
    }

    override fun getDisplayCapabilities(): DisplayCapabilities {
        val displayProfile = deviceInfo.getDisplayProfile()
        return DisplayCapabilities(
            widthPixels = displayProfile.widthPixels,
            heightPixels = displayProfile.heightPixels,
            densityDpi = displayProfile.densityDpi,
            refreshRate = displayProfile.refreshRate,
            isHdr = deviceDetector.display.isHdr,
            isWideColorGamut = deviceDetector.display.isWideColorGamut,
            hasXrSupport = deviceDetector.display.hasXrSupport
        )
    }

    override fun getBiometricCapabilities(): BiometricCapabilities? {
        val biometric = deviceDetector.biometric ?: return null

        return BiometricCapabilities(
            hasFingerprint = deviceDetector.hardware.hasFingerprint,
            hasFace = deviceDetector.hardware.hasFace,
            hasIris = deviceDetector.hardware.hasIris,
            biometricLevel = biometric.biometricLevel.toString()
        )
    }

    override fun getDeviceFingerprint(): DeviceFingerprint {
        // Generate fingerprint using FNV-1a hash of device components
        val components = listOf(
            Build.MANUFACTURER,
            Build.MODEL,
            Build.DEVICE,
            Build.BOARD,
            Build.HARDWARE,
            Build.PRODUCT,
            Build.VERSION.SDK_INT.toString(),
            Runtime.getRuntime().availableProcessors().toString()
        )

        val fingerprintValue = generateFnv1aHash(components.joinToString(":"))

        return DeviceFingerprint(
            value = fingerprintValue,
            type = "hardware",
            components = listOf("manufacturer", "model", "device", "board", "hardware", "product", "sdk", "cores"),
            timestamp = System.currentTimeMillis()
        )
    }

    override fun getPerformanceClass(): PerformanceClass {
        val cpuCores = Runtime.getRuntime().availableProcessors()
        val cpuMaxFreq = getCpuMaxFrequency()

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val totalRamMb = (memInfo.totalMem / (1024 * 1024)).toInt()

        // Scoring based on hardware
        var score = 0

        // CPU scoring
        if (cpuCores >= 8) score += 2
        else if (cpuCores >= 4) score += 1

        if (cpuMaxFreq >= 2500) score += 2
        else if (cpuMaxFreq >= 1500) score += 1

        // RAM scoring
        if (totalRamMb >= 6144) score += 2
        else if (totalRamMb >= 3072) score += 1

        // GPU/Vulkan bonus
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) score += 2

        // Android version bonus
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) score += 1

        return when {
            score >= 8 -> PerformanceClass.HIGH_END
            score >= 4 -> PerformanceClass.MID_RANGE
            else -> PerformanceClass.LOW_END
        }
    }

    override suspend fun refreshCapabilities(): DeviceCapabilities {
        return withContext(Dispatchers.IO) {
            cachedCapabilities = null
            getAllCapabilities().also { cachedCapabilities = it }
        }
    }

    // ===== Private Helpers =====

    private fun detectDeviceType(): KmpDeviceType {
        return when {
            deviceDetector.hardware.isWatch -> KmpDeviceType.WATCH
            deviceDetector.behavioral.isSmartGlass -> KmpDeviceType.SMART_GLASS
            deviceDetector.display.hasXrSupport -> KmpDeviceType.XR_HEADSET
            deviceDetector.hardware.isTablet -> KmpDeviceType.TABLET
            deviceInfo.isTV() -> KmpDeviceType.TV
            deviceInfo.isAutomotive() -> KmpDeviceType.AUTOMOTIVE
            else -> KmpDeviceType.PHONE
        }
    }

    private fun getCpuMaxFrequency(): Int {
        try {
            for (i in 0 until Runtime.getRuntime().availableProcessors()) {
                val maxFreqFile = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
                if (maxFreqFile.exists()) {
                    val maxFreq = maxFreqFile.readText().trim().toIntOrNull() ?: 0
                    if (maxFreq > 0) return maxFreq / 1000 // Convert kHz to MHz
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read CPU frequency", e)
        }
        return 0
    }

    private fun getGpuVendor(): String {
        val chipset = Build.HARDWARE.lowercase()
        val board = Build.BOARD.lowercase()

        return when {
            chipset.contains("qcom") || chipset.contains("qualcomm") ||
                    board.contains("msm") || board.contains("sdm") -> "Qualcomm Adreno"
            chipset.contains("exynos") -> "ARM Mali"
            chipset.contains("kirin") -> "ARM Mali"
            chipset.contains("mediatek") || chipset.contains("mt") -> "ARM Mali"
            chipset.contains("tegra") -> "NVIDIA"
            else -> "Unknown"
        }
    }

    /**
     * Generate FNV-1a hash for fingerprint
     */
    private fun generateFnv1aHash(input: String): String {
        val FNV_OFFSET_BASIS = 2166136261L
        val FNV_PRIME = 16777619L

        var hash = FNV_OFFSET_BASIS
        for (byte in input.toByteArray()) {
            hash = hash xor (byte.toLong() and 0xFF)
            hash = (hash * FNV_PRIME) and 0xFFFFFFFFL
        }

        return hash.toString(16).padStart(8, '0')
    }
}
