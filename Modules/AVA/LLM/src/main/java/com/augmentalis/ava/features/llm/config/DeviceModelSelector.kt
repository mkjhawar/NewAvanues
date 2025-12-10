/**
 * Device-Based Model Selector
 *
 * Single Responsibility: Select optimal NLU + LLM configurations based on device capabilities
 *
 * Uses AVA-DEVICE-MODEL-MATRIX.md recommendations to:
 * 1. Auto-detect device type via Build.MODEL and RAM
 * 2. Recommend best ROI configurations for the device
 * 3. Allow user to choose from compatible alternatives
 * 4. Validate selected models are actually installed
 *
 * Created: 2025-11-30
 * Reference: docs/AVA-DEVICE-MODEL-MATRIX.md
 */

package com.augmentalis.ava.features.llm.config

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.augmentalis.ava.features.llm.alc.loader.ModelDiscovery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Selects optimal model configurations based on device capabilities
 */
class DeviceModelSelector(
    private val context: Context,
    private val modelDiscovery: ModelDiscovery
) {

    /**
     * Auto-detect current device and return matching profile
     *
     * @return Detected device profile, or GENERIC if unknown
     */
    fun detectDevice(): DeviceProfile {
        val model = Build.MODEL.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        val ramGB = getDeviceRamGB()

        Timber.i("Detecting device: model=$model, manufacturer=$manufacturer, RAM=${ramGB}GB")

        // Match device by model name patterns
        val profile = when {
            // RealWear devices
            model.contains("hmt-1z1") || model.contains("hmt1z1") -> DeviceProfile.HMT_1Z1
            model.contains("hmt-1") || model.contains("hmt1") -> DeviceProfile.HMT_1
            model.contains("navigator z1") || model.contains("navz1") -> DeviceProfile.NAVIGATOR_Z1
            model.contains("navigator 520") || model.contains("nav520") -> DeviceProfile.NAVIGATOR_520
            model.contains("navigator 500") || model.contains("nav500") -> DeviceProfile.NAVIGATOR_500
            model.contains("arc 3") || model.contains("arc3") -> DeviceProfile.ARC_3

            // Vuzix devices
            model.contains("m4000") -> DeviceProfile.VUZIX_M4000
            model.contains("z100") -> DeviceProfile.VUZIX_Z100
            model.contains("m400c") -> DeviceProfile.VUZIX_M400C
            model.contains("m400") -> DeviceProfile.VUZIX_M400

            // Rokid devices
            model.contains("x-craft") || model.contains("xcraft") -> DeviceProfile.ROKID_XCRAFT
            model.contains("max pro") || model.contains("maxpro") -> DeviceProfile.ROKID_MAX_PRO

            // Samsung devices
            model.contains("sm-s92") || model.contains("s24") -> DeviceProfile.SAMSUNG_S24
            model.contains("sm-s91") || model.contains("s23") -> DeviceProfile.SAMSUNG_S23

            // Generic by RAM if unknown device
            else -> {
                Timber.w("Unknown device model: $model, using RAM-based profile")
                getGenericProfileByRam(ramGB)
            }
        }

        Timber.i("Detected device profile: ${profile.displayName} (RAM: ${profile.ramGB}GB)")
        return profile
    }

    /**
     * Get device RAM in GB
     */
    private fun getDeviceRamGB(): Int {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return (memInfo.totalMem / (1024 * 1024 * 1024)).toInt().coerceAtLeast(1)
    }

    /**
     * Get generic profile based on RAM
     */
    private fun getGenericProfileByRam(ramGB: Int): DeviceProfile {
        return when {
            ramGB <= 3 -> DeviceProfile.GENERIC_2GB
            ramGB <= 5 -> DeviceProfile.GENERIC_4GB
            ramGB <= 7 -> DeviceProfile.GENERIC_6GB
            else -> DeviceProfile.GENERIC_8GB_PLUS
        }
    }

    /**
     * Get all configurations for a device, sorted by ROI score
     *
     * @param device Device profile
     * @return List of model configurations sorted by ROI (best first)
     */
    fun getConfigurationsForDevice(device: DeviceProfile): List<ModelConfiguration> {
        return DEVICE_CONFIGURATIONS[device]?.sortedByDescending { it.roiScore }
            ?: emptyList()
    }

    /**
     * Get recommended configuration for device
     *
     * @param device Device profile
     * @param preferMultilingual Prefer multilingual over English-focused
     * @return Best configuration, or null if none available
     */
    suspend fun getRecommendedConfiguration(
        device: DeviceProfile,
        preferMultilingual: Boolean = false
    ): ModelConfiguration? {
        val configs = getConfigurationsForDevice(device)
            .filter { it.runtimeAvailable } // Only current runtimes, not future

        // Filter by language preference
        val preferred = if (preferMultilingual) {
            configs.filter { it.type == ConfigurationType.MULTILINGUAL }
        } else {
            configs.filter { it.type == ConfigurationType.BASE }
        }

        // Get highest ROI that has models installed
        for (config in preferred.sortedByDescending { it.roiScore }) {
            if (isConfigurationAvailable(config)) {
                Timber.i("Recommended config: ${config.id} (ROI: ${config.roiScore})")
                return config
            }
        }

        // Fall back to any available config
        for (config in configs.sortedByDescending { it.roiScore }) {
            if (isConfigurationAvailable(config)) {
                Timber.w("Falling back to config: ${config.id}")
                return config
            }
        }

        Timber.e("No compatible configuration found for ${device.displayName}")
        return null
    }

    /**
     * Check if a configuration's LLM model is actually installed
     */
    suspend fun isConfigurationAvailable(config: ModelConfiguration): Boolean {
        return modelDiscovery.isModelInstalled(config.llmModel)
    }

    /**
     * Get all available configurations (models actually installed)
     */
    suspend fun getAvailableConfigurations(device: DeviceProfile): List<ModelConfiguration> =
        withContext(Dispatchers.IO) {
            getConfigurationsForDevice(device).filter { config ->
                isConfigurationAvailable(config)
            }
        }

    /**
     * Get ROI summary for all devices
     *
     * @return Map of device to best ROI configuration
     */
    fun getRoiSummary(): Map<DeviceProfile, ModelConfiguration?> {
        return DeviceProfile.values()
            .filter { it != DeviceProfile.GENERIC_2GB } // Exclude generic profiles from summary
            .filter { it != DeviceProfile.GENERIC_4GB }
            .filter { it != DeviceProfile.GENERIC_6GB }
            .filter { it != DeviceProfile.GENERIC_8GB_PLUS }
            .associateWith { device ->
                getConfigurationsForDevice(device)
                    .filter { it.runtimeAvailable }
                    .maxByOrNull { it.roiScore }
            }
    }

    companion object {
        /**
         * All device configurations from AVA-DEVICE-MODEL-MATRIX.md
         */
        val DEVICE_CONFIGURATIONS: Map<DeviceProfile, List<ModelConfiguration>> = mapOf(
            // ==================== RealWear HMT-1 (2GB RAM) ====================
            DeviceProfile.HMT_1 to listOf(
                ModelConfiguration(
                    id = "hmt1-base",
                    displayName = "HMT-1 English",
                    type = ConfigurationType.BASE,
                    nluModel = "AVA-384-BASE",
                    llmModel = "AVA-LL32-1B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 1.57f,
                    estimatedSpeed = "15 t/s",
                    roiScore = 7,
                    runtimeAvailable = true,
                    description = "Best English option for 2GB device. 128K context."
                ),
                ModelConfiguration(
                    id = "hmt1-multilingual",
                    displayName = "HMT-1 Multilingual",
                    type = ConfigurationType.MULTILINGUAL,
                    nluModel = "AVA-384-BASE",
                    llmModel = "AVA-QW3-06B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 1.0f,
                    estimatedSpeed = "18 t/s",
                    roiScore = 5,
                    runtimeAvailable = true,
                    description = "Basic multilingual for constrained device. 100+ languages."
                )
            ),

            // ==================== RealWear HMT-1Z1 (3GB RAM) ====================
            DeviceProfile.HMT_1Z1 to listOf(
                ModelConfiguration(
                    id = "hmt1z1-base",
                    displayName = "HMT-1Z1 English",
                    type = ConfigurationType.BASE,
                    nluModel = "AVA-384-BASE",
                    llmModel = "AVA-LL32-1B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 1.57f,
                    estimatedSpeed = "15 t/s",
                    roiScore = 7,
                    runtimeAvailable = true,
                    description = "Best English option for hazardous zone device."
                ),
                ModelConfiguration(
                    id = "hmt1z1-multilingual",
                    displayName = "HMT-1Z1 Multilingual",
                    type = ConfigurationType.MULTILINGUAL,
                    nluModel = "AVA-384-BASE",
                    llmModel = "AVA-QW3-06B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 1.0f,
                    estimatedSpeed = "18 t/s",
                    roiScore = 5,
                    runtimeAvailable = true,
                    description = "Basic multilingual. Extra headroom vs HMT-1."
                )
            ),

            // ==================== RealWear Navigator 500 (4GB RAM) ====================
            DeviceProfile.NAVIGATOR_500 to listOf(
                ModelConfiguration(
                    id = "navigator-base",
                    displayName = "Navigator Base English",
                    type = ConfigurationType.BASE,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-GE2-2B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 1.97f,
                    estimatedSpeed = "18 t/s",
                    roiScore = 9,
                    runtimeAvailable = true,
                    description = "Recommended. Best quality-per-GB for technical guidance."
                ),
                ModelConfiguration(
                    id = "navigator-multilingual",
                    displayName = "Navigator Multilingual",
                    type = ConfigurationType.MULTILINGUAL,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-QW3-17B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 2.25f,
                    estimatedSpeed = "22 t/s",
                    roiScore = 8,
                    runtimeAvailable = true,
                    description = "Excellent multilingual. 85%+ accuracy in 100 languages."
                ),
                ModelConfiguration(
                    id = "navigator-gemma3n",
                    displayName = "Navigator Gemma 3n",
                    type = ConfigurationType.GEMMA3N,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-GE3N-2B",
                    llmRuntime = "LiteRT",
                    totalMemoryGB = 2.45f,
                    estimatedSpeed = "20 t/s",
                    roiScore = 10,
                    runtimeAvailable = true,
                    description = "Recommended. Best quality with Gemma 3n E2B."
                )
            ),

            // ==================== RealWear Navigator 520 (4GB RAM) ====================
            DeviceProfile.NAVIGATOR_520 to listOf(
                ModelConfiguration(
                    id = "nav520-base",
                    displayName = "Navigator 520 Base English",
                    type = ConfigurationType.BASE,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-GE2-2B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 1.97f,
                    estimatedSpeed = "18 t/s",
                    roiScore = 9,
                    runtimeAvailable = true,
                    description = "Recommended. Enhanced Navigator platform."
                ),
                ModelConfiguration(
                    id = "nav520-multilingual",
                    displayName = "Navigator 520 Multilingual",
                    type = ConfigurationType.MULTILINGUAL,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-QW3-17B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 2.25f,
                    estimatedSpeed = "22 t/s",
                    roiScore = 8,
                    runtimeAvailable = true,
                    description = "Excellent multilingual for global deployments."
                ),
                ModelConfiguration(
                    id = "nav520-gemma3n",
                    displayName = "Navigator 520 Gemma 3n",
                    type = ConfigurationType.GEMMA3N,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-GE3N-2B",
                    llmRuntime = "LiteRT",
                    totalMemoryGB = 2.45f,
                    estimatedSpeed = "20 t/s",
                    roiScore = 10,
                    runtimeAvailable = true,
                    description = "Recommended. +15% quality with Gemma 3n."
                )
            ),

            // ==================== RealWear Navigator Z1 (4GB RAM) ====================
            DeviceProfile.NAVIGATOR_Z1 to listOf(
                ModelConfiguration(
                    id = "navz1-base",
                    displayName = "Navigator Z1 Base English",
                    type = ConfigurationType.BASE,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-GE2-2B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 1.97f,
                    estimatedSpeed = "18 t/s",
                    roiScore = 9,
                    runtimeAvailable = true,
                    description = "Recommended for hazardous zone deployments."
                ),
                ModelConfiguration(
                    id = "navz1-multilingual",
                    displayName = "Navigator Z1 Multilingual",
                    type = ConfigurationType.MULTILINGUAL,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-QW3-17B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 2.25f,
                    estimatedSpeed = "22 t/s",
                    roiScore = 8,
                    runtimeAvailable = true,
                    description = "Multilingual for global hazardous zones."
                )
            ),

            // ==================== RealWear Arc 3 (4GB RAM) ====================
            DeviceProfile.ARC_3 to listOf(
                ModelConfiguration(
                    id = "arc3-base",
                    displayName = "Arc 3 Base English",
                    type = ConfigurationType.BASE,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-GE2-2B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 1.97f,
                    estimatedSpeed = "18 t/s",
                    roiScore = 9,
                    runtimeAvailable = true,
                    description = "Recommended. Premium industrial platform."
                ),
                ModelConfiguration(
                    id = "arc3-multilingual",
                    displayName = "Arc 3 Multilingual",
                    type = ConfigurationType.MULTILINGUAL,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-QW3-17B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 2.25f,
                    estimatedSpeed = "22 t/s",
                    roiScore = 8,
                    runtimeAvailable = true,
                    description = "Excellent multilingual for global enterprise."
                ),
                ModelConfiguration(
                    id = "arc3-gemma3n",
                    displayName = "Arc 3 Gemma 3n (Recommended)",
                    type = ConfigurationType.GEMMA3N,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-GE3N-2B",
                    llmRuntime = "LiteRT",
                    totalMemoryGB = 2.45f,
                    estimatedSpeed = "20 t/s",
                    roiScore = 10,
                    runtimeAvailable = true,
                    description = "Strongly recommended. Best quality for premium industrial."
                )
            ),

            // ==================== Vuzix M400 (6GB RAM) ====================
            DeviceProfile.VUZIX_M400 to listOf(
                ModelConfiguration(
                    id = "m400-base",
                    displayName = "M400 Base English",
                    type = ConfigurationType.BASE,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-LL32-3B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 2.95f,
                    estimatedSpeed = "20 t/s",
                    roiScore = 9,
                    runtimeAvailable = true,
                    description = "Recommended. Highest quality English. 128K context."
                ),
                ModelConfiguration(
                    id = "m400-multilingual",
                    displayName = "M400 Multilingual",
                    type = ConfigurationType.MULTILINGUAL,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-GE2-2B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 1.97f,
                    estimatedSpeed = "18 t/s",
                    roiScore = 8,
                    runtimeAvailable = true,
                    description = "Good multilingual with generous RAM headroom."
                ),
                ModelConfiguration(
                    id = "m400-gemma3n",
                    displayName = "M400 Gemma 3n E4B (Optimal)",
                    type = ConfigurationType.GEMMA3N,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-GE3N-4B",
                    llmRuntime = "LiteRT",
                    totalMemoryGB = 3.45f,
                    estimatedSpeed = "22 t/s",
                    roiScore = 10,
                    runtimeAvailable = true,
                    description = "Optimal. E4B fits comfortably. +25% quality."
                )
            ),

            // ==================== Vuzix M400C (6GB RAM) ====================
            DeviceProfile.VUZIX_M400C to listOf(
                ModelConfiguration(
                    id = "m400c-base",
                    displayName = "M400C Base English",
                    type = ConfigurationType.BASE,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-LL32-3B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 2.95f,
                    estimatedSpeed = "20 t/s",
                    roiScore = 9,
                    runtimeAvailable = true,
                    description = "Recommended for consumer M400 variant."
                ),
                ModelConfiguration(
                    id = "m400c-multilingual",
                    displayName = "M400C Multilingual",
                    type = ConfigurationType.MULTILINGUAL,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-GE2-2B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 1.97f,
                    estimatedSpeed = "18 t/s",
                    roiScore = 8,
                    runtimeAvailable = true,
                    description = "Good multilingual with RAM headroom."
                )
            ),

            // ==================== Vuzix Z100 (6GB RAM, XR2) ====================
            DeviceProfile.VUZIX_Z100 to listOf(
                ModelConfiguration(
                    id = "z100-base",
                    displayName = "Z100 Base English",
                    type = ConfigurationType.BASE,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-LL32-3B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 2.95f,
                    estimatedSpeed = "25 t/s",
                    roiScore = 9,
                    runtimeAvailable = true,
                    description = "Recommended. Fast XR2 GPU performance."
                ),
                ModelConfiguration(
                    id = "z100-multilingual-premium",
                    displayName = "Z100 Premium Multilingual",
                    type = ConfigurationType.MULTILINGUAL,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-QW3-4B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 3.95f,
                    estimatedSpeed = "25 t/s",
                    roiScore = 10,
                    runtimeAvailable = true,
                    description = "Best overall quality. 78% reasoning, 90% multilingual."
                ),
                ModelConfiguration(
                    id = "z100-gemma3n-premium",
                    displayName = "Z100 Gemma 3n E4B (Optimal)",
                    type = ConfigurationType.GEMMA3N,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-GE3N-4B",
                    llmRuntime = "LiteRT",
                    totalMemoryGB = 3.45f,
                    estimatedSpeed = "25 t/s",
                    roiScore = 10,
                    runtimeAvailable = true,
                    description = "Recommended. Optimal choice for premium device."
                )
            ),

            // ==================== Vuzix M4000 (6GB RAM, XR2) ====================
            DeviceProfile.VUZIX_M4000 to listOf(
                ModelConfiguration(
                    id = "m4000-base",
                    displayName = "M4000 Base English",
                    type = ConfigurationType.BASE,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-LL32-3B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 2.95f,
                    estimatedSpeed = "25 t/s",
                    roiScore = 9,
                    runtimeAvailable = true,
                    description = "Recommended for latest Vuzix enterprise."
                ),
                ModelConfiguration(
                    id = "m4000-multilingual-premium",
                    displayName = "M4000 Premium Multilingual",
                    type = ConfigurationType.MULTILINGUAL,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-QW3-4B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 3.95f,
                    estimatedSpeed = "25 t/s",
                    roiScore = 10,
                    runtimeAvailable = true,
                    description = "Best quality for enterprise multilingual."
                ),
                ModelConfiguration(
                    id = "m4000-gemma3n",
                    displayName = "M4000 Gemma 3n E4B (Optimal)",
                    type = ConfigurationType.GEMMA3N,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-GE3N-4B",
                    llmRuntime = "LiteRT",
                    totalMemoryGB = 3.45f,
                    estimatedSpeed = "25 t/s",
                    roiScore = 10,
                    runtimeAvailable = true,
                    description = "Recommended. Optimal for latest enterprise device."
                )
            ),

            // ==================== Rokid X-Craft (4GB RAM, Mali) ====================
            DeviceProfile.ROKID_XCRAFT to listOf(
                ModelConfiguration(
                    id = "xcraft-base",
                    displayName = "X-Craft Base English (Mali)",
                    type = ConfigurationType.BASE,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-GE2-2B16-MALI",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 1.97f,
                    estimatedSpeed = "16 t/s",
                    roiScore = 8,
                    runtimeAvailable = true,
                    description = "Requires Mali-compiled model from llm-mali/"
                ),
                ModelConfiguration(
                    id = "xcraft-multilingual",
                    displayName = "X-Craft Multilingual (Mali)",
                    type = ConfigurationType.MULTILINGUAL,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-QW3-17B16-MALI",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 2.25f,
                    estimatedSpeed = "20 t/s",
                    roiScore = 7,
                    runtimeAvailable = true,
                    description = "Requires Mali-compiled model."
                ),
                ModelConfiguration(
                    id = "xcraft-gemma3n",
                    displayName = "X-Craft Gemma 3n (Recommended)",
                    type = ConfigurationType.GEMMA3N,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-GE3N-2B",
                    llmRuntime = "LiteRT",
                    totalMemoryGB = 2.45f,
                    estimatedSpeed = "18 t/s",
                    roiScore = 9,
                    runtimeAvailable = true,
                    description = "Recommended. LiteRT has native Mali support."
                )
            ),

            // ==================== Rokid Max Pro (6GB RAM, Mali-G78) ====================
            DeviceProfile.ROKID_MAX_PRO to listOf(
                ModelConfiguration(
                    id = "maxpro-base",
                    displayName = "Max Pro Base English (Mali)",
                    type = ConfigurationType.BASE,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-LL32-3B16-MALI",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 2.95f,
                    estimatedSpeed = "18 t/s",
                    roiScore = 8,
                    runtimeAvailable = true,
                    description = "Requires Mali-compiled model from llm-mali/"
                ),
                ModelConfiguration(
                    id = "maxpro-multilingual",
                    displayName = "Max Pro Premium Multilingual (Mali)",
                    type = ConfigurationType.MULTILINGUAL,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-QW3-4B16-MALI",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 3.95f,
                    estimatedSpeed = "20 t/s",
                    roiScore = 9,
                    runtimeAvailable = true,
                    description = "Requires Mali-compiled model."
                ),
                ModelConfiguration(
                    id = "maxpro-gemma3n",
                    displayName = "Max Pro Gemma 3n E4B (Optimal)",
                    type = ConfigurationType.GEMMA3N,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-GE3N-4B",
                    llmRuntime = "LiteRT",
                    totalMemoryGB = 3.45f,
                    estimatedSpeed = "20 t/s",
                    roiScore = 10,
                    runtimeAvailable = true,
                    description = "Recommended. Strongly recommended for premium Rokid."
                )
            ),

            // ==================== Samsung S23 (8GB RAM) ====================
            DeviceProfile.SAMSUNG_S23 to listOf(
                ModelConfiguration(
                    id = "s23-premium",
                    displayName = "S23 Premium",
                    type = ConfigurationType.BASE,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-QW3-4B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 3.95f,
                    estimatedSpeed = "30 t/s",
                    roiScore = 10,
                    runtimeAvailable = true,
                    description = "Recommended. Best quality for flagship phone."
                ),
                ModelConfiguration(
                    id = "s23-multilingual",
                    displayName = "S23 Multilingual",
                    type = ConfigurationType.MULTILINGUAL,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-QW3-4B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 3.95f,
                    estimatedSpeed = "30 t/s",
                    roiScore = 10,
                    runtimeAvailable = true,
                    description = "Same model excels at both English and multilingual."
                ),
                ModelConfiguration(
                    id = "s23-gemma3n",
                    displayName = "S23 Gemma 3n E4B (Optimal)",
                    type = ConfigurationType.GEMMA3N,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-GE3N-4B",
                    llmRuntime = "LiteRT",
                    totalMemoryGB = 3.45f,
                    estimatedSpeed = "35 t/s",
                    roiScore = 10,
                    runtimeAvailable = true,
                    description = "Recommended. +20% quality, +20% speed."
                )
            ),

            // ==================== Samsung S24 (8-12GB RAM) ====================
            DeviceProfile.SAMSUNG_S24 to listOf(
                ModelConfiguration(
                    id = "s24-premium",
                    displayName = "S24 Premium",
                    type = ConfigurationType.BASE,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-QW3-4B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 3.95f,
                    estimatedSpeed = "35 t/s",
                    roiScore = 10,
                    runtimeAvailable = true,
                    description = "Recommended. Best performance on latest flagship."
                ),
                ModelConfiguration(
                    id = "s24-multilingual",
                    displayName = "S24 Multilingual",
                    type = ConfigurationType.MULTILINGUAL,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-QW3-4B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 3.95f,
                    estimatedSpeed = "35 t/s",
                    roiScore = 10,
                    runtimeAvailable = true,
                    description = "Excellent multilingual on flagship."
                ),
                ModelConfiguration(
                    id = "s24-gemma3n",
                    displayName = "S24 Gemma 3n E4B (Optimal)",
                    type = ConfigurationType.GEMMA3N,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-GE3N-4B",
                    llmRuntime = "LiteRT",
                    totalMemoryGB = 3.45f,
                    estimatedSpeed = "40 t/s",
                    roiScore = 10,
                    runtimeAvailable = true,
                    description = "Recommended. Optimal flagship performance."
                )
            ),

            // ==================== Generic Profiles ====================
            DeviceProfile.GENERIC_2GB to listOf(
                ModelConfiguration(
                    id = "generic-2gb-base",
                    displayName = "Generic 2GB English",
                    type = ConfigurationType.BASE,
                    nluModel = "AVA-384-BASE",
                    llmModel = "AVA-LL32-1B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 1.57f,
                    estimatedSpeed = "12 t/s",
                    roiScore = 6,
                    runtimeAvailable = true,
                    description = "Safe configuration for low-memory devices."
                )
            ),

            DeviceProfile.GENERIC_4GB to listOf(
                ModelConfiguration(
                    id = "generic-4gb-base",
                    displayName = "Generic 4GB English",
                    type = ConfigurationType.BASE,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-GE2-2B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 1.97f,
                    estimatedSpeed = "15 t/s",
                    roiScore = 8,
                    runtimeAvailable = true,
                    description = "Safe configuration for 4GB devices."
                ),
                ModelConfiguration(
                    id = "generic-4gb-multi",
                    displayName = "Generic 4GB Multilingual",
                    type = ConfigurationType.MULTILINGUAL,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-QW3-17B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 2.25f,
                    estimatedSpeed = "18 t/s",
                    roiScore = 7,
                    runtimeAvailable = true,
                    description = "Multilingual for unknown 4GB devices."
                )
            ),

            DeviceProfile.GENERIC_6GB to listOf(
                ModelConfiguration(
                    id = "generic-6gb-base",
                    displayName = "Generic 6GB English",
                    type = ConfigurationType.BASE,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-LL32-3B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 2.95f,
                    estimatedSpeed = "18 t/s",
                    roiScore = 8,
                    runtimeAvailable = true,
                    description = "Good configuration for 6GB devices."
                ),
                ModelConfiguration(
                    id = "generic-6gb-multi",
                    displayName = "Generic 6GB Multilingual",
                    type = ConfigurationType.MULTILINGUAL,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-QW3-4B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 3.95f,
                    estimatedSpeed = "20 t/s",
                    roiScore = 9,
                    runtimeAvailable = true,
                    description = "Premium multilingual for 6GB devices."
                )
            ),

            DeviceProfile.GENERIC_8GB_PLUS to listOf(
                ModelConfiguration(
                    id = "generic-8gb-premium",
                    displayName = "Generic 8GB+ Premium",
                    type = ConfigurationType.BASE,
                    nluModel = "AVA-768-MULTI",
                    llmModel = "AVA-QW3-4B16",
                    llmRuntime = "MLC-LLM",
                    totalMemoryGB = 3.95f,
                    estimatedSpeed = "25 t/s",
                    roiScore = 10,
                    runtimeAvailable = true,
                    description = "Best quality for high-RAM devices."
                )
            )
        )
    }
}

/**
 * Known device profiles with specifications
 */
enum class DeviceProfile(
    val displayName: String,
    val manufacturer: String,
    val ramGB: Int,
    val gpuType: String,
    val year: Int
) {
    // RealWear
    HMT_1("RealWear HMT-1", "RealWear", 2, "Adreno 506", 2017),
    HMT_1Z1("RealWear HMT-1Z1", "RealWear", 3, "Adreno 506", 2019),
    NAVIGATOR_500("RealWear Navigator 500", "RealWear", 4, "Adreno 619", 2022),
    NAVIGATOR_520("RealWear Navigator 520", "RealWear", 4, "Adreno 619", 2023),
    NAVIGATOR_Z1("RealWear Navigator Z1", "RealWear", 4, "Adreno 619", 2023),
    ARC_3("RealWear Arc 3", "RealWear", 4, "Adreno 619", 2024),

    // Vuzix
    VUZIX_M400("Vuzix M400", "Vuzix", 6, "Snapdragon XR1", 2020),
    VUZIX_M400C("Vuzix M400C", "Vuzix", 6, "Snapdragon XR1", 2021),
    VUZIX_Z100("Vuzix Z100", "Vuzix", 6, "Snapdragon XR2", 2023),
    VUZIX_M4000("Vuzix M4000", "Vuzix", 6, "Snapdragon XR2", 2024),

    // Rokid
    ROKID_XCRAFT("Rokid X-Craft", "Rokid", 4, "Mali-G57", 2023),
    ROKID_MAX_PRO("Rokid Max Pro", "Rokid", 6, "Mali-G78", 2024),

    // Samsung
    SAMSUNG_S23("Samsung Galaxy S23", "Samsung", 8, "Adreno 740", 2023),
    SAMSUNG_S24("Samsung Galaxy S24", "Samsung", 8, "Adreno 750", 2024),

    // Generic profiles
    GENERIC_2GB("Generic 2GB Device", "Unknown", 2, "Unknown", 2020),
    GENERIC_4GB("Generic 4GB Device", "Unknown", 4, "Unknown", 2020),
    GENERIC_6GB("Generic 6GB Device", "Unknown", 6, "Unknown", 2020),
    GENERIC_8GB_PLUS("Generic 8GB+ Device", "Unknown", 8, "Unknown", 2020)
}

/**
 * Model configuration for a specific device profile
 */
data class ModelConfiguration(
    val id: String,
    val displayName: String,
    val type: ConfigurationType,
    val nluModel: String,
    val llmModel: String,
    val llmRuntime: String,
    val totalMemoryGB: Float,
    val estimatedSpeed: String,
    val roiScore: Int,  // 1-10, higher is better ROI
    val runtimeAvailable: Boolean,  // true if runtime is implemented
    val description: String
) {
    /**
     * Check if this is a Mali-specific configuration
     */
    val requiresMali: Boolean get() = llmModel.contains("-MALI")

    /**
     * Check if this is for iOS
     */
    val requiresIOS: Boolean get() = llmModel.contains("-IOS")

    /**
     * Get ROI display string
     */
    fun getRoiDisplay(): String = when (roiScore) {
        10 -> "★★★★★ Optimal"
        9 -> "★★★★☆ Excellent"
        8 -> "★★★★ Very Good"
        7 -> "★★★☆ Good"
        6 -> "★★★ Acceptable"
        else -> "★★ Basic"
    }
}

/**
 * Type of configuration
 */
enum class ConfigurationType(val displayName: String) {
    BASE("English-Focused"),
    MULTILINGUAL("Multilingual"),
    GEMMA3N("Gemma 3n")
}
