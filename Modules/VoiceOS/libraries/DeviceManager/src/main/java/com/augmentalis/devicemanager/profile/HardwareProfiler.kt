/**
 * HardwareProfiler.kt
 * Path: /libraries/DeviceManager/src/main/java/com/augmentalis/devicemanager/profile/HardwareProfiler.kt
 *
 * Created: 2025-10-09
 * Last Modified: 2025-10-09
 * Author: Manoj Jhawar
 * Version: 1.0.0
 *
 * Purpose: Create comprehensive hardware profile for device fingerprinting and capability analysis
 * Module: DeviceManager
 *
 * Changelog:
 * - v1.0.0 (2025-10-09): Initial creation with performance classification and capability matrix
 */

package com.augmentalis.devicemanager.profile

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import com.augmentalis.devicemanager.deviceinfo.detection.DeviceDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile

/**
 * HardwareProfiler - Create hardware profile for device
 *
 * Features:
 * - Comprehensive device fingerprint (CPU, RAM, GPU, sensors, connectivity)
 * - Performance classification (low/mid/high end)
 * - Capability matrix (boolean flags for all features)
 * - JSON export (for caching/sharing)
 *
 * COT Analysis: Hardware Profiling Strategy
 *
 * Performance Classification Methodology:
 *
 * LOW-END:
 * - CPU: < 4 cores OR < 1.5 GHz
 * - RAM: < 3 GB
 * - GPU: Basic OpenGL ES 2.0/3.0
 * - Storage: < 32 GB internal
 * - Typical: Budget phones, older devices (2015-2018)
 * - Optimization: Reduce visual effects, lower texture quality
 *
 * MID-RANGE:
 * - CPU: 4-8 cores, 1.5-2.5 GHz
 * - RAM: 3-6 GB
 * - GPU: OpenGL ES 3.1/3.2, Vulkan 1.0/1.1
 * - Storage: 32-128 GB internal
 * - Typical: Mainstream phones (2019-2021)
 * - Optimization: Balanced quality settings
 *
 * HIGH-END:
 * - CPU: 8+ cores, > 2.5 GHz
 * - RAM: > 6 GB
 * - GPU: Vulkan 1.1+, advanced features
 * - Storage: 128+ GB internal
 * - Typical: Flagship phones (2022+), tablets, smart glasses
 * - Optimization: Maximum quality settings, advanced features
 *
 * ROT Analysis: Why This Matters
 * - Enables adaptive UX based on device capabilities
 * - Prevents performance issues on low-end devices
 * - Leverages advanced features on high-end devices
 * - Critical for VR/AR applications (frame rate requirements)
 *
 * TOT Analysis: Alternative Classification Methods
 * - Benchmark scores (Geekbench, AnTuTu): Requires external data
 * - GPU vendor detection: Less reliable across manufacturers
 * - Year-based classification: Ignores budget vs flagship
 * - Decision: Multi-factor analysis (CPU + RAM + GPU + sensors)
 */
class HardwareProfiler(private val context: Context) {

    companion object {
        private const val TAG = "HardwareProfiler"
        private const val VERSION = "1.0.0"

        // Performance thresholds
        private const val LOW_END_CPU_CORES = 4
        private const val LOW_END_CPU_FREQ_MHZ = 1500
        private const val LOW_END_RAM_MB = 3072

        private const val HIGH_END_CPU_CORES = 8
        private const val HIGH_END_CPU_FREQ_MHZ = 2500
        private const val HIGH_END_RAM_MB = 6144
    }

    // Cached profile
    private var cachedProfile: HardwareProfile? = null

    /**
     * Generate comprehensive hardware profile
     *
     * @param forceRefresh Force new profile generation
     * @return Complete hardware profile
     */
    suspend fun generateProfile(forceRefresh: Boolean = false): HardwareProfile = withContext(Dispatchers.IO) {
        val cached = cachedProfile
        if (!forceRefresh && cached != null) {
            Log.d(TAG, "Returning cached hardware profile")
            return@withContext cached
        }

        Log.d(TAG, "Generating fresh hardware profile")

        val capabilities = DeviceDetector.getCapabilities(context, forceRefresh)

        // CPU information
        val cpuInfo = getCpuInfo()

        // Memory information
        val memoryInfo = getMemoryInfo()

        // GPU information
        val gpuInfo = getGpuInfo()

        // Storage information
        val storageInfo = getStorageInfo()

        // Performance classification
        val performanceClass = classifyPerformance(cpuInfo, memoryInfo, gpuInfo)

        // Build capability matrix
        val capabilityMatrix = buildCapabilityMatrix(capabilities)

        // Device fingerprint
        val fingerprint = generateFingerprint(capabilities, cpuInfo, memoryInfo, gpuInfo, storageInfo)

        val profile = HardwareProfile(
            timestamp = System.currentTimeMillis(),
            deviceFingerprint = fingerprint,
            performanceClass = performanceClass,
            cpuInfo = cpuInfo,
            memoryInfo = memoryInfo,
            gpuInfo = gpuInfo,
            storageInfo = storageInfo,
            capabilityMatrix = capabilityMatrix,
            deviceInfo = DeviceInfoProfile(
                manufacturer = capabilities.deviceInfo.manufacturer,
                model = capabilities.deviceInfo.model,
                device = capabilities.deviceInfo.device,
                brand = capabilities.deviceInfo.brand,
                androidVersion = capabilities.deviceInfo.androidVersion,
                androidVersionName = capabilities.deviceInfo.androidVersionName,
                buildId = capabilities.deviceInfo.buildId
            )
        )

        cachedProfile = profile
        Log.d(TAG, "Hardware profile generated: $performanceClass class")
        return@withContext profile
    }

    /**
     * Get CPU information
     */
    private fun getCpuInfo(): CpuInfo {
        val cpuCount = Runtime.getRuntime().availableProcessors()

        // Try to read CPU frequency from /sys/devices/system/cpu
        var maxFreqMhz = 0
        var minFreqMhz = Int.MAX_VALUE

        for (i in 0 until cpuCount) {
            try {
                val maxFreqFile = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
                if (maxFreqFile.exists()) {
                    val maxFreq = maxFreqFile.readText().trim().toIntOrNull() ?: 0
                    maxFreqMhz = maxOf(maxFreqMhz, maxFreq / 1000) // Convert kHz to MHz
                }

                val minFreqFile = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_min_freq")
                if (minFreqFile.exists()) {
                    val minFreq = minFreqFile.readText().trim().toIntOrNull() ?: Int.MAX_VALUE
                    minFreqMhz = minOf(minFreqMhz, minFreq / 1000)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not read CPU frequency for core $i", e)
            }
        }

        // Read CPU architecture
        val architecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"

        // Try to determine CPU vendor/model
        val cpuModel = try {
            File("/proc/cpuinfo").useLines { lines ->
                lines.firstOrNull { it.startsWith("Hardware") || it.startsWith("model name") }
                    ?.split(":")?.getOrNull(1)?.trim() ?: "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }

        return CpuInfo(
            coreCount = cpuCount,
            maxFrequencyMhz = if (maxFreqMhz > 0) maxFreqMhz else 0,
            minFrequencyMhz = if (minFreqMhz < Int.MAX_VALUE) minFreqMhz else 0,
            architecture = architecture,
            model = cpuModel,
            supportedAbis = Build.SUPPORTED_ABIS.toList()
        )
    }

    /**
     * Get memory information
     */
    private fun getMemoryInfo(): MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val totalRamMb = (memInfo.totalMem / (1024 * 1024)).toInt()
        val availableRamMb = (memInfo.availMem / (1024 * 1024)).toInt()
        val usedRamMb = totalRamMb - availableRamMb

        // Get memory class (max heap size for apps)
        val memoryClass = activityManager.memoryClass
        val largeMemoryClass = activityManager.largeMemoryClass

        return MemoryInfo(
            totalRamMb = totalRamMb,
            availableRamMb = availableRamMb,
            usedRamMb = usedRamMb,
            memoryClass = memoryClass,
            largeMemoryClass = largeMemoryClass,
            isLowRamDevice = activityManager.isLowRamDevice
        )
    }

    /**
     * Get GPU information
     */
    private fun getGpuInfo(): GpuInfo {
        // GPU info requires OpenGL context, which we don't have here
        // Instead, use known mappings from chipset to GPU

        val chipset = Build.HARDWARE.lowercase()
        val board = Build.BOARD.lowercase()

        val gpuVendor = when {
            chipset.contains("qcom") || chipset.contains("qualcomm") || board.contains("msm") || board.contains("sdm") -> "Qualcomm Adreno"
            chipset.contains("exynos") -> "ARM Mali"
            chipset.contains("kirin") -> "ARM Mali"
            chipset.contains("mediatek") || chipset.contains("mt") -> "ARM Mali"
            chipset.contains("tegra") -> "NVIDIA"
            else -> "Unknown"
        }

        // Estimate OpenGL ES version based on Android version
        val openGlVersion = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N -> "3.2"
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> "3.1"
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 -> "3.0"
            else -> "2.0"
        }

        // Check Vulkan support
        val hasVulkan = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

        return GpuInfo(
            vendor = gpuVendor,
            renderer = chipset,
            openGlVersion = openGlVersion,
            vulkanSupported = hasVulkan,
            vulkanVersion = if (hasVulkan) "1.0" else null
        )
    }

    /**
     * Get storage information
     */
    private fun getStorageInfo(): StorageInfo {
        val internalStats = StatFs(Environment.getDataDirectory().path)
        val internalTotalGb = (internalStats.totalBytes / (1024 * 1024 * 1024)).toInt()
        val internalAvailableGb = (internalStats.availableBytes / (1024 * 1024 * 1024)).toInt()

        // Check external storage
        val hasExternalStorage = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
        val externalTotalGb = if (hasExternalStorage) {
            try {
                val externalStats = StatFs(Environment.getExternalStorageDirectory().path)
                (externalStats.totalBytes / (1024 * 1024 * 1024)).toInt()
            } catch (e: Exception) {
                0
            }
        } else 0

        return StorageInfo(
            internalTotalGb = internalTotalGb,
            internalAvailableGb = internalAvailableGb,
            externalTotalGb = externalTotalGb,
            hasExternalStorage = hasExternalStorage
        )
    }

    /**
     * Classify device performance
     */
    private fun classifyPerformance(cpu: CpuInfo, memory: MemoryInfo, gpu: GpuInfo): PerformanceClass {
        var score = 0

        // CPU scoring
        if (cpu.coreCount >= HIGH_END_CPU_CORES) score += 2
        else if (cpu.coreCount >= LOW_END_CPU_CORES) score += 1

        if (cpu.maxFrequencyMhz >= HIGH_END_CPU_FREQ_MHZ) score += 2
        else if (cpu.maxFrequencyMhz >= LOW_END_CPU_FREQ_MHZ) score += 1

        // RAM scoring
        if (memory.totalRamMb >= HIGH_END_RAM_MB) score += 2
        else if (memory.totalRamMb >= LOW_END_RAM_MB) score += 1

        // GPU scoring
        if (gpu.vulkanSupported) score += 2
        if (gpu.openGlVersion >= "3.1") score += 1

        // Android version bonus
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) score += 1

        // Classify based on score
        return when {
            score >= 8 -> PerformanceClass.HIGH_END
            score >= 4 -> PerformanceClass.MID_RANGE
            else -> PerformanceClass.LOW_END
        }
    }

    /**
     * Build capability matrix
     */
    private fun buildCapabilityMatrix(capabilities: DeviceDetector.DeviceCapabilities): CapabilityMatrix {
        return CapabilityMatrix(
            // Connectivity
            bluetooth = capabilities.network.hasBluetooth,
            bluetoothLE = capabilities.network.hasBluetoothLE,
            wifi = capabilities.network.hasWiFi,
            wifiDirect = capabilities.network.hasWiFiDirect,
            wifiAware = capabilities.network.hasWiFiAware,
            uwb = capabilities.network.hasUwb,
            nfc = capabilities.network.hasNfc,
            cellular = capabilities.network.hasCellular,
            fiveG = capabilities.network.has5G,

            // Sensors
            accelerometer = capabilities.sensors.hasAccelerometer,
            gyroscope = capabilities.sensors.hasGyroscope,
            magnetometer = capabilities.sensors.hasMagnetometer,
            barometer = capabilities.sensors.hasBarometer,
            proximity = capabilities.sensors.hasProximity,
            light = capabilities.sensors.hasLight,
            stepCounter = capabilities.sensors.hasStepCounter,
            heartRate = capabilities.sensors.hasHeartRate,

            // Input/Output
            camera = capabilities.hardware.hasCamera,
            cameraFront = capabilities.hardware.hasCameraFront,
            microphone = capabilities.hardware.hasMicrophone,
            speaker = capabilities.audio.hasSpeaker,
            haptic = true, // Most devices have vibration

            // Biometric
            fingerprint = capabilities.hardware.hasFingerprint,
            face = capabilities.hardware.hasFace,
            iris = capabilities.hardware.hasIris,

            // Display
            hdr = capabilities.display.isHdr,
            wideColorGamut = capabilities.display.isWideColorGamut,
            xrSupport = capabilities.display.hasXrSupport,

            // Device type
            tablet = capabilities.hardware.isTablet,
            watch = capabilities.hardware.isWatch,
            foldable = capabilities.hardware.isFoldable,
            smartGlass = capabilities.behavioral.isSmartGlass
        )
    }

    /**
     * Generate unique device fingerprint
     */
    private fun generateFingerprint(
        capabilities: DeviceDetector.DeviceCapabilities,
        cpu: CpuInfo,
        memory: MemoryInfo,
        gpu: GpuInfo,
        storage: StorageInfo
    ): String {
        val fingerprintData = listOf(
            capabilities.deviceInfo.manufacturer,
            capabilities.deviceInfo.model,
            capabilities.deviceInfo.device,
            capabilities.deviceInfo.androidVersion.toString(),
            cpu.coreCount.toString(),
            cpu.architecture,
            memory.totalRamMb.toString(),
            gpu.vendor,
            storage.internalTotalGb.toString()
        )

        return fingerprintData.joinToString("-").hashCode().toString(16)
    }

    /**
     * Export profile to JSON
     */
    suspend fun toJson(forceRefresh: Boolean = false): String = withContext(Dispatchers.IO) {
        val profile = generateProfile(forceRefresh)

        val json = JSONObject().apply {
            put("version", VERSION)
            put("timestamp", profile.timestamp)
            put("deviceFingerprint", profile.deviceFingerprint)
            put("performanceClass", profile.performanceClass.name)

            // Device info
            put("deviceInfo", JSONObject().apply {
                put("manufacturer", profile.deviceInfo.manufacturer)
                put("model", profile.deviceInfo.model)
                put("device", profile.deviceInfo.device)
                put("brand", profile.deviceInfo.brand)
                put("androidVersion", profile.deviceInfo.androidVersion)
                put("androidVersionName", profile.deviceInfo.androidVersionName)
                put("buildId", profile.deviceInfo.buildId)
            })

            // CPU info
            put("cpu", JSONObject().apply {
                put("coreCount", profile.cpuInfo.coreCount)
                put("maxFrequencyMhz", profile.cpuInfo.maxFrequencyMhz)
                put("minFrequencyMhz", profile.cpuInfo.minFrequencyMhz)
                put("architecture", profile.cpuInfo.architecture)
                put("model", profile.cpuInfo.model)
                put("supportedAbis", JSONArray(profile.cpuInfo.supportedAbis))
            })

            // Memory info
            put("memory", JSONObject().apply {
                put("totalRamMb", profile.memoryInfo.totalRamMb)
                put("availableRamMb", profile.memoryInfo.availableRamMb)
                put("usedRamMb", profile.memoryInfo.usedRamMb)
                put("memoryClass", profile.memoryInfo.memoryClass)
                put("largeMemoryClass", profile.memoryInfo.largeMemoryClass)
                put("isLowRamDevice", profile.memoryInfo.isLowRamDevice)
            })

            // GPU info
            put("gpu", JSONObject().apply {
                put("vendor", profile.gpuInfo.vendor)
                put("renderer", profile.gpuInfo.renderer)
                put("openGlVersion", profile.gpuInfo.openGlVersion)
                put("vulkanSupported", profile.gpuInfo.vulkanSupported)
                put("vulkanVersion", profile.gpuInfo.vulkanVersion ?: "N/A")
            })

            // Storage info
            put("storage", JSONObject().apply {
                put("internalTotalGb", profile.storageInfo.internalTotalGb)
                put("internalAvailableGb", profile.storageInfo.internalAvailableGb)
                put("externalTotalGb", profile.storageInfo.externalTotalGb)
                put("hasExternalStorage", profile.storageInfo.hasExternalStorage)
            })

            // Capability matrix
            put("capabilities", profile.capabilityMatrix.toJson())
        }

        return@withContext json.toString(2)
    }

    /**
     * Clear cached profile
     */
    fun clearCache() {
        cachedProfile = null
        Log.d(TAG, "Cache cleared")
    }
}

// ===== DATA CLASSES =====

/**
 * Complete hardware profile
 */
data class HardwareProfile(
    val timestamp: Long,
    val deviceFingerprint: String,
    val performanceClass: PerformanceClass,
    val cpuInfo: CpuInfo,
    val memoryInfo: MemoryInfo,
    val gpuInfo: GpuInfo,
    val storageInfo: StorageInfo,
    val capabilityMatrix: CapabilityMatrix,
    val deviceInfo: DeviceInfoProfile
)

data class DeviceInfoProfile(
    val manufacturer: String,
    val model: String,
    val device: String,
    val brand: String,
    val androidVersion: Int,
    val androidVersionName: String,
    val buildId: String
)

data class CpuInfo(
    val coreCount: Int,
    val maxFrequencyMhz: Int,
    val minFrequencyMhz: Int,
    val architecture: String,
    val model: String,
    val supportedAbis: List<String>
)

data class MemoryInfo(
    val totalRamMb: Int,
    val availableRamMb: Int,
    val usedRamMb: Int,
    val memoryClass: Int,
    val largeMemoryClass: Int,
    val isLowRamDevice: Boolean
)

data class GpuInfo(
    val vendor: String,
    val renderer: String,
    val openGlVersion: String,
    val vulkanSupported: Boolean,
    val vulkanVersion: String?
)

data class StorageInfo(
    val internalTotalGb: Int,
    val internalAvailableGb: Int,
    val externalTotalGb: Int,
    val hasExternalStorage: Boolean
)

data class CapabilityMatrix(
    // Connectivity
    val bluetooth: Boolean,
    val bluetoothLE: Boolean,
    val wifi: Boolean,
    val wifiDirect: Boolean,
    val wifiAware: Boolean,
    val uwb: Boolean,
    val nfc: Boolean,
    val cellular: Boolean,
    val fiveG: Boolean,

    // Sensors
    val accelerometer: Boolean,
    val gyroscope: Boolean,
    val magnetometer: Boolean,
    val barometer: Boolean,
    val proximity: Boolean,
    val light: Boolean,
    val stepCounter: Boolean,
    val heartRate: Boolean,

    // Input/Output
    val camera: Boolean,
    val cameraFront: Boolean,
    val microphone: Boolean,
    val speaker: Boolean,
    val haptic: Boolean,

    // Biometric
    val fingerprint: Boolean,
    val face: Boolean,
    val iris: Boolean,

    // Display
    val hdr: Boolean,
    val wideColorGamut: Boolean,
    val xrSupport: Boolean,

    // Device type
    val tablet: Boolean,
    val watch: Boolean,
    val foldable: Boolean,
    val smartGlass: Boolean
) {
    fun toJson() = JSONObject().apply {
        // Connectivity
        put("bluetooth", bluetooth)
        put("bluetoothLE", bluetoothLE)
        put("wifi", wifi)
        put("wifiDirect", wifiDirect)
        put("wifiAware", wifiAware)
        put("uwb", uwb)
        put("nfc", nfc)
        put("cellular", cellular)
        put("fiveG", fiveG)

        // Sensors
        put("accelerometer", accelerometer)
        put("gyroscope", gyroscope)
        put("magnetometer", magnetometer)
        put("barometer", barometer)
        put("proximity", proximity)
        put("light", light)
        put("stepCounter", stepCounter)
        put("heartRate", heartRate)

        // Input/Output
        put("camera", camera)
        put("cameraFront", cameraFront)
        put("microphone", microphone)
        put("speaker", speaker)
        put("haptic", haptic)

        // Biometric
        put("fingerprint", fingerprint)
        put("face", face)
        put("iris", iris)

        // Display
        put("hdr", hdr)
        put("wideColorGamut", wideColorGamut)
        put("xrSupport", xrSupport)

        // Device type
        put("tablet", tablet)
        put("watch", watch)
        put("foldable", foldable)
        put("smartGlass", smartGlass)
    }
}

/**
 * Performance classification
 */
enum class PerformanceClass {
    LOW_END,    // Budget devices, optimize for performance
    MID_RANGE,  // Mainstream devices, balanced quality
    HIGH_END    // Flagship devices, maximum quality
}
