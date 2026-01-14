/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * This file is part of AVA AI and is proprietary software.
 * See LICENSE file in the project root for license information.
 */

package com.augmentalis.ava.core.common.backend

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import timber.log.Timber
import java.io.File

/**
 * Hardware-Aware Inference Backend Selector
 *
 * Automatically detects device capabilities and selects the optimal
 * execution backend for ML inference on Android devices.
 *
 * Backend Priority Matrix:
 * | Device Type     | Priority 1 | Priority 2 | Priority 3 | Priority 4 |
 * |-----------------|------------|------------|------------|------------|
 * | Qualcomm        | QNN/HTP    | NNAPI      | Vulkan     | OpenCL     |
 * | Samsung         | NNAPI      | Vulkan     | OpenCL     | CPU        |
 * | MediaTek        | NNAPI      | Vulkan     | OpenCL     | CPU        |
 * | Google Tensor   | NNAPI      | Vulkan     | CPU        | -          |
 * | Unknown         | NNAPI      | OpenCL     | CPU        | -          |
 *
 * Performance comparison (MobileBERT, Snapdragon 8 Gen 2):
 * - QNN/HTP: 15ms
 * - NNAPI:   25ms
 * - Vulkan:  35ms
 * - OpenCL:  40ms
 * - CPU:     120ms
 *
 * @see ADR-008-Hardware-Aware-Inference-Backend.md
 */
object InferenceBackendSelector {

    /**
     * Available inference backends
     */
    enum class Backend(val displayName: String) {
        QNN_HTP("Qualcomm Hexagon DSP"),     // Best for Snapdragon (dedicated AI processor)
        NNAPI("Android Neural Networks"),    // Cross-platform (auto-delegates to best hardware)
        VULKAN("Vulkan Compute"),            // GPU compute (modern API)
        OPENCL("OpenCL GPU"),                // GPU compute (legacy, wide support)
        CPU("CPU (ARM NEON)")                // Fallback (always works)
    }

    /**
     * Chipset vendor information
     */
    data class ChipsetInfo(
        val isQualcomm: Boolean,
        val isSamsung: Boolean,
        val isMediaTek: Boolean,
        val isGoogleTensor: Boolean,
        val socName: String,
        val gpuName: String = ""
    )

    /**
     * Device hardware capabilities
     */
    data class DeviceCapabilities(
        val hasQNN: Boolean,
        val hasNNAPI: Boolean,
        val hasVulkan: Boolean,
        val vulkanVersion: String?,
        val hasOpenCL: Boolean,
        val openCLVersion: String?,
        val cpuCores: Int,
        val hasNEON: Boolean,
        val supportsFp16: Boolean = true
    )

    // Cache for expensive operations
    private var cachedChipset: ChipsetInfo? = null
    private var cachedCapabilities: DeviceCapabilities? = null
    private var cachedOptimalBackend: Backend? = null

    /**
     * Detect and return optimal backend for current device
     *
     * Uses cached result after first detection to avoid repeated checks.
     *
     * @param context Android context
     * @return Optimal backend for this device
     */
    fun selectOptimalBackend(context: Context): Backend {
        cachedOptimalBackend?.let { return it }

        val chipset = detectChipset()
        val capabilities = detectCapabilities(context)

        val backend = when {
            // Qualcomm with QNN support - use Hexagon DSP (best for Snapdragon)
            chipset.isQualcomm && capabilities.hasQNN -> Backend.QNN_HTP

            // NNAPI available (Android 8.1+) - best cross-platform option
            capabilities.hasNNAPI -> Backend.NNAPI

            // Vulkan 1.0+ available - modern GPU compute
            capabilities.hasVulkan -> Backend.VULKAN

            // OpenCL available - legacy GPU support
            capabilities.hasOpenCL -> Backend.OPENCL

            // CPU fallback - always works
            else -> Backend.CPU
        }

        Timber.i("Selected inference backend: ${backend.displayName}")
        Timber.d("Device: ${chipset.socName}, Capabilities: NNAPI=${capabilities.hasNNAPI}, Vulkan=${capabilities.hasVulkan}, OpenCL=${capabilities.hasOpenCL}, QNN=${capabilities.hasQNN}")

        cachedOptimalBackend = backend
        return backend
    }

    /**
     * Get optimal backend for LLM inference (TVM Runtime)
     *
     * LLM inference has different requirements than NLU:
     * - Larger memory footprint
     * - Streaming output
     * - GPU preferred for throughput
     *
     * @param context Android context
     * @return Backend string for TVM Runtime ("vulkan", "opencl", "cpu")
     */
    fun selectLLMBackend(context: Context): String {
        val chipset = detectChipset()
        val capabilities = detectCapabilities(context)

        return when {
            // Vulkan preferred for LLM on modern devices
            capabilities.hasVulkan && chipset.isQualcomm -> "vulkan"
            capabilities.hasVulkan -> "vulkan"

            // OpenCL fallback for older Snapdragon devices
            capabilities.hasOpenCL -> "opencl"

            // CPU as last resort
            else -> "cpu"
        }
    }

    /**
     * Get optimal backend for NLU inference (ONNX Runtime)
     *
     * NLU models are smaller and benefit from NNAPI auto-delegation.
     *
     * @param context Android context
     * @return Backend for ONNX Runtime configuration
     */
    fun selectNLUBackend(context: Context): Backend {
        val chipset = detectChipset()
        val capabilities = detectCapabilities(context)

        return when {
            // Qualcomm QNN for best Snapdragon performance
            chipset.isQualcomm && capabilities.hasQNN -> Backend.QNN_HTP

            // NNAPI for cross-platform (auto-delegates to GPU/DSP/NPU)
            capabilities.hasNNAPI -> Backend.NNAPI

            // CPU fallback
            else -> Backend.CPU
        }
    }

    /**
     * Detect chipset vendor and SoC information
     */
    fun detectChipset(): ChipsetInfo {
        cachedChipset?.let { return it }

        val hardware = Build.HARDWARE.lowercase()
        val board = Build.BOARD.lowercase()
        val soc = getSystemProperty("ro.board.platform")
        val gpu = getSystemProperty("ro.hardware.egl") ?: ""

        val chipset = ChipsetInfo(
            isQualcomm = soc.startsWith("msm") || soc.startsWith("sm") ||
                    soc.startsWith("sdm") || hardware.contains("qcom") ||
                    board.contains("msm") || board.contains("sdm"),
            isSamsung = soc.startsWith("exynos") || hardware.contains("exynos") ||
                    soc.startsWith("s5") || board.contains("universal"),
            isMediaTek = soc.startsWith("mt") || hardware.contains("mt") ||
                    board.contains("mt"),
            isGoogleTensor = soc.startsWith("gs") || hardware.contains("tensor") ||
                    board.contains("slider") || board.contains("raven") ||
                    board.contains("oriole") || board.contains("bluejay"),
            socName = soc.ifEmpty { hardware },
            gpuName = gpu
        )

        Timber.d("Detected chipset: ${chipset.socName} (Qualcomm=${chipset.isQualcomm}, Samsung=${chipset.isSamsung}, MediaTek=${chipset.isMediaTek}, Tensor=${chipset.isGoogleTensor})")

        cachedChipset = chipset
        return chipset
    }

    /**
     * Detect device hardware capabilities
     */
    fun detectCapabilities(context: Context): DeviceCapabilities {
        cachedCapabilities?.let { return it }

        val pm = context.packageManager

        val capabilities = DeviceCapabilities(
            hasQNN = checkQNNAvailable(),
            hasNNAPI = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1, // Android 8.1+
            hasVulkan = checkVulkanSupport(pm),
            vulkanVersion = getVulkanVersion(pm),
            hasOpenCL = checkOpenCLSupport(),
            openCLVersion = getOpenCLVersion(),
            cpuCores = Runtime.getRuntime().availableProcessors(),
            hasNEON = checkNEONSupport(),
            supportsFp16 = checkFp16Support()
        )

        Timber.d("Device capabilities: NNAPI=${capabilities.hasNNAPI}, Vulkan=${capabilities.vulkanVersion}, OpenCL=${capabilities.openCLVersion}, QNN=${capabilities.hasQNN}, Cores=${capabilities.cpuCores}")

        cachedCapabilities = capabilities
        return capabilities
    }

    /**
     * Check if Qualcomm QNN SDK is available
     */
    private fun checkQNNAvailable(): Boolean {
        return try {
            // Check for QNN runtime library
            val qnnLibPath = "/vendor/lib64/libQnnHtp.so"
            val altQnnPath = "/system/vendor/lib64/libQnnHtp.so"

            File(qnnLibPath).exists() || File(altQnnPath).exists() ||
                    System.getProperty("qnn.available")?.toBoolean() == true
        } catch (e: Exception) {
            Timber.d("QNN check failed: ${e.message}")
            false
        }
    }

    /**
     * Check Vulkan support
     */
    private fun checkVulkanSupport(pm: PackageManager): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL, 0) ||
                    pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION, 0x400000) // Vulkan 1.0
        } else {
            false
        }
    }

    /**
     * Get Vulkan version string
     */
    private fun getVulkanVersion(pm: PackageManager): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return null

        return when {
            pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION, 0x403000) -> "1.3"
            pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION, 0x402000) -> "1.2"
            pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION, 0x401000) -> "1.1"
            pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION, 0x400000) -> "1.0"
            pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL, 0) -> "1.0"
            else -> null
        }
    }

    /**
     * Check OpenCL support by looking for OpenCL libraries
     */
    private fun checkOpenCLSupport(): Boolean {
        return try {
            val openclPaths = listOf(
                "/system/vendor/lib64/libOpenCL.so",
                "/vendor/lib64/libOpenCL.so",
                "/system/lib64/libOpenCL.so",
                "/system/vendor/lib/libOpenCL.so",
                "/vendor/lib/libOpenCL.so"
            )
            openclPaths.any { File(it).exists() }
        } catch (e: Exception) {
            Timber.d("OpenCL check failed: ${e.message}")
            false
        }
    }

    /**
     * Get OpenCL version (best effort)
     */
    private fun getOpenCLVersion(): String? {
        // OpenCL version detection requires loading the library
        // Return "2.0" for Adreno GPUs (standard for Snapdragon 625+)
        val chipset = cachedChipset ?: return null
        return when {
            chipset.isQualcomm -> "2.0"
            chipset.isSamsung -> "2.0"
            chipset.isMediaTek -> "1.2" // Conservative estimate
            else -> null
        }
    }

    /**
     * Check ARM NEON SIMD support
     */
    private fun checkNEONSupport(): Boolean {
        // All ARMv8 (64-bit) processors support NEON
        return Build.SUPPORTED_64_BIT_ABIS.isNotEmpty() ||
                Build.SUPPORTED_ABIS.any { it.contains("arm64") || it.contains("armeabi-v7a") }
    }

    /**
     * Check FP16 (half-precision) support
     */
    private fun checkFp16Support(): Boolean {
        // FP16 compute is supported on:
        // - Adreno 5xx+ GPUs (Snapdragon 820+)
        // - Mali G71+ GPUs
        // - Most modern mobile GPUs
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O // Android 8.0+ generally supports FP16
    }

    /**
     * Get system property safely
     */
    private fun getSystemProperty(key: String): String {
        return try {
            val process = Runtime.getRuntime().exec("getprop $key")
            process.inputStream.bufferedReader().readLine()?.trim() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Check if a specific backend is available on this device
     *
     * @param backend Backend to check
     * @param context Android context
     * @return true if backend is available
     */
    fun isBackendAvailable(backend: Backend, context: Context): Boolean {
        val capabilities = detectCapabilities(context)

        return when (backend) {
            Backend.QNN_HTP -> capabilities.hasQNN
            Backend.NNAPI -> capabilities.hasNNAPI
            Backend.VULKAN -> capabilities.hasVulkan
            Backend.OPENCL -> capabilities.hasOpenCL
            Backend.CPU -> true // Always available
        }
    }

    /**
     * Get all available backends on this device (ordered by preference)
     *
     * @param context Android context
     * @return List of available backends in priority order
     */
    fun getAvailableBackends(context: Context): List<Backend> {
        val chipset = detectChipset()
        val capabilities = detectCapabilities(context)

        return buildList {
            // Qualcomm-specific: QNN first
            if (chipset.isQualcomm && capabilities.hasQNN) add(Backend.QNN_HTP)

            // NNAPI is preferred for most use cases
            if (capabilities.hasNNAPI) add(Backend.NNAPI)

            // Vulkan for GPU compute
            if (capabilities.hasVulkan) add(Backend.VULKAN)

            // OpenCL for legacy GPU support
            if (capabilities.hasOpenCL) add(Backend.OPENCL)

            // CPU always available
            add(Backend.CPU)
        }
    }

    /**
     * Get device summary for logging/debugging
     *
     * @param context Android context
     * @return Human-readable device summary
     */
    fun getDeviceSummary(context: Context): String {
        val chipset = detectChipset()
        val capabilities = detectCapabilities(context)
        val optimal = selectOptimalBackend(context)

        return buildString {
            appendLine("=== AVA Inference Backend Summary ===")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("SoC: ${chipset.socName}")
            appendLine("Vendor: ${getVendorName(chipset)}")
            appendLine()
            appendLine("Capabilities:")
            appendLine("  - NNAPI: ${if (capabilities.hasNNAPI) "Yes (Android ${Build.VERSION.SDK_INT})" else "No"}")
            appendLine("  - Vulkan: ${capabilities.vulkanVersion ?: "Not available"}")
            appendLine("  - OpenCL: ${capabilities.openCLVersion ?: "Not available"}")
            appendLine("  - QNN/HTP: ${if (capabilities.hasQNN) "Yes" else "No"}")
            appendLine("  - CPU cores: ${capabilities.cpuCores}")
            appendLine("  - NEON SIMD: ${if (capabilities.hasNEON) "Yes" else "No"}")
            appendLine()
            appendLine("Selected Backend: ${optimal.displayName}")
            appendLine("=====================================")
        }
    }

    private fun getVendorName(chipset: ChipsetInfo): String {
        return when {
            chipset.isQualcomm -> "Qualcomm Snapdragon"
            chipset.isSamsung -> "Samsung Exynos"
            chipset.isMediaTek -> "MediaTek"
            chipset.isGoogleTensor -> "Google Tensor"
            else -> "Unknown"
        }
    }

    /**
     * Clear cached detection results (useful for testing)
     */
    fun clearCache() {
        cachedChipset = null
        cachedCapabilities = null
        cachedOptimalBackend = null
    }
}
