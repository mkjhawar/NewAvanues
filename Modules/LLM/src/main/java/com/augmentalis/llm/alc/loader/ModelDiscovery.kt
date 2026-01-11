/**
 * Dynamic Model Discovery
 *
 * Single Responsibility: Discover installed LLM models on device storage
 *
 * Scans standard locations for AVA-formatted LLM models and provides
 * information about what's available without hardcoding model names.
 *
 * Supports AVA 3-character extension scheme (v2.0):
 * - .amm = MLC-LLM models (TVM compiled)
 * - .amg = GGUF models (llama.cpp)
 * - .amr = LiteRT models (Google AI Edge)
 *
 * Created: 2025-11-24
 * Updated: 2025-12-01 (v2.0 extension scheme)
 * Updated: 2025-12-05 (checksum verification)
 */

package com.augmentalis.llm.alc.loader

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Discovers installed LLM models dynamically
 *
 * Scans storage locations for AVA-formatted model directories and archives.
 * Supports multiple runtime formats:
 * - MLC-LLM (.amm) - TVM compiled models
 * - GGUF (.amg) - llama.cpp models
 * - LiteRT (.amr) - Google AI Edge models
 *
 * Does NOT hardcode model names - discovers what's actually installed.
 */
class ModelDiscovery(
    private val context: Context
) {

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        // Standard locations to scan for models
        // Includes both release (com.augmentalis.ava) and debug (com.augmentalis.ava.debug) paths
        private val SCAN_PATHS = listOf(
            "/sdcard/ava-ai-models/llm",           // MLC models
            "/sdcard/ava-ai-models/llm-gguf",      // GGUF models
            "/sdcard/ava-ai-models/llm-litert",    // LiteRT models
            "ava-ai-models/llm",                    // Relative to external files
            "ava-ai-models/llm-gguf",
            "ava-ai-models/llm-litert",
            "models/llm",                           // Legacy location
            "models"                                // Fallback
        )

        // Additional absolute paths for debug/release package data directories
        private val PACKAGE_DATA_PATHS = listOf(
            "/sdcard/Android/data/com.augmentalis.ava/files/models/llm",
            "/sdcard/Android/data/com.augmentalis.ava.debug/files/models/llm",
            "/sdcard/Android/data/com.augmentalis.ava/files/models",
            "/sdcard/Android/data/com.augmentalis.ava.debug/files/models"
        )

        // MLC model requires these files (.adm = Ava Device MLC)
        private val MLC_REQUIRED_FILES = listOf(
            "AVALibrary.adm"                        // MLC runtime library
        )

        // MLC device code patterns - recognize all loadable library formats
        private val MLC_DEVICE_CODE_PATTERNS = listOf(
            ".adm",                                 // Ava Device MLC code
            ".ads",                                 // Ava Device Shared (TVM compat shim)
            ".so"                                   // Direct shared object (TVM native)
        )

        // GGUF model archive extension (.amg = Ava Model GGUF)
        private const val GGUF_ARCHIVE_EXT = ".amg"

        // LiteRT model archive extension (.amr = Ava Model liteRT)
        private const val LITERT_ARCHIVE_EXT = ".amr"

        // Tokenizer file patterns
        private val TOKENIZER_PATTERNS = listOf(
            "tokenizer.ats",                        // SentencePiece (.ats)
            "tokenizer.ath",                        // HuggingFace (.ath)
            "tokenizer.model",                      // Legacy SentencePiece
            "tokenizer.json"                        // Legacy HuggingFace
        )
    }

    /**
     * Discover all installed LLM models
     *
     * @return List of discovered models, empty if none found
     */
    suspend fun discoverInstalledModels(): List<DiscoveredModel> = withContext(Dispatchers.IO) {
        val discoveredModels = mutableListOf<DiscoveredModel>()

        for (scanPath in getScanLocations()) {
            if (!scanPath.exists() || !scanPath.isDirectory) {
                Timber.d("Scan location not found: ${scanPath.absolutePath}")
                continue
            }

            Timber.d("Scanning for models in: ${scanPath.absolutePath}")

            // Check for model archives (.amm, .amg, .amr)
            scanPath.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val modelFormat = when {
                        file.name.endsWith(".amm", ignoreCase = true) -> ModelFormat.MLC
                        file.name.endsWith(GGUF_ARCHIVE_EXT, ignoreCase = true) -> ModelFormat.GGUF
                        file.name.endsWith(LITERT_ARCHIVE_EXT, ignoreCase = true) -> ModelFormat.LITERT
                        file.name.endsWith(".ALM", ignoreCase = true) -> ModelFormat.MLC  // Legacy
                        else -> null
                    }

                    if (modelFormat != null) {
                        val modelName = file.nameWithoutExtension
                        Timber.d("Found ${modelFormat.name} archive: ${file.name}")

                        discoveredModels.add(DiscoveredModel(
                            id = modelName,
                            name = modelName,
                            path = File(scanPath, modelName).absolutePath,  // Extracted path
                            sizeBytes = file.length(),
                            type = ModelType.ARCHIVE,
                            format = modelFormat,
                            metadata = mapOf(
                                "archive_file" to file.absolutePath,
                                "runtime" to modelFormat.runtime
                            )
                        ))
                    }
                }
            }

            // Check for extracted model directories
            scanPath.listFiles()?.forEach { file ->
                if (file.isDirectory && file.name.startsWith("AVA-")) {
                    val modelFormat = detectModelFormat(file)
                    if (modelFormat != null) {
                        val metadata = loadModelMetadata(file)
                        val sizeBytes = calculateDirectorySize(file)

                        Timber.i("Found installed ${modelFormat.name} model: ${file.name} (${sizeBytes / 1_000_000}MB)")

                        discoveredModels.add(DiscoveredModel(
                            id = file.name,
                            name = metadata["model_name"] ?: file.name,
                            path = file.absolutePath,
                            sizeBytes = sizeBytes,
                            type = ModelType.EXTRACTED,
                            format = modelFormat,
                            metadata = metadata + mapOf("runtime" to modelFormat.runtime)
                        ))
                    } else {
                        Timber.w("Invalid model directory (unknown format): ${file.name}")
                    }
                }
            }
        }

        // Remove duplicates (prefer extracted over archives)
        val uniqueModels = discoveredModels
            .groupBy { it.id }
            .mapValues { (_, models) ->
                models.firstOrNull { it.type == ModelType.EXTRACTED }
                    ?: models.first()
            }
            .values
            .sortedBy { it.id }

        Timber.i("Discovered ${uniqueModels.size} installed model(s)")
        return@withContext uniqueModels.toList()
    }

    /**
     * Get the first available model (for automatic selection)
     *
     * Priority:
     * 1. Largest model (usually best quality)
     * 2. Extracted over archive
     * 3. Alphabetically first
     *
     * @return First available model, or null if none installed
     */
    suspend fun getFirstAvailableModel(): DiscoveredModel? {
        val models = discoverInstalledModels()

        // Priority: Prefer extracted, then by size (larger = better)
        return models
            .filter { it.type == ModelType.EXTRACTED }
            .maxByOrNull { it.sizeBytes }
            ?: models.maxByOrNull { it.sizeBytes }
    }

    /**
     * Check if a specific model is installed
     *
     * @param modelId Model ID to check (e.g., "AVA-GE3-4B16")
     * @return true if model is installed
     */
    suspend fun isModelInstalled(modelId: String): Boolean {
        val models = discoverInstalledModels()
        return models.any { it.id == modelId }
    }

    /**
     * Get model by ID
     *
     * @param modelId Model ID to find
     * @return DiscoveredModel if found, null otherwise
     */
    suspend fun getModelById(modelId: String): DiscoveredModel? {
        val models = discoverInstalledModels()
        return models.firstOrNull { it.id == modelId }
    }

    /**
     * Get scan locations (absolute paths)
     *
     * Scans in priority order:
     * 1. Package-specific external storage (handles debug suffix automatically)
     * 2. Fixed absolute paths (both release and debug package names)
     * 3. Standard relative paths from external files directory
     */
    private fun getScanLocations(): List<File> {
        val locations = mutableListOf<File>()

        // Priority 1: Package-specific external storage (auto-handles .debug suffix)
        val externalDir = context.getExternalFilesDir(null)
        if (externalDir != null) {
            // Direct models directory in app's external storage
            locations.add(File(externalDir, "models/llm"))
            locations.add(File(externalDir, "models"))
            Timber.d("Package-specific path: ${externalDir.absolutePath}/models")
        }

        // Priority 2: Fixed absolute paths for both release and debug packages
        PACKAGE_DATA_PATHS.forEach { locations.add(File(it)) }

        // Priority 3: Absolute paths from SCAN_PATHS
        SCAN_PATHS.filter { it.startsWith("/") }
            .forEach { locations.add(File(it)) }

        // Priority 4: Relative paths (to external files dir)
        val baseDir = externalDir ?: context.filesDir
        SCAN_PATHS.filter { !it.startsWith("/") }
            .forEach { locations.add(File(baseDir, it)) }

        // Log all scan locations for debugging
        Timber.d("Model scan locations (${locations.size} total):")
        locations.forEachIndexed { index, file ->
            val exists = file.exists()
            Timber.d("  [$index] ${file.absolutePath} (exists: $exists)")
        }

        return locations.distinctBy { it.absolutePath }
    }

    /**
     * Detect the model format from a directory
     *
     * @return ModelFormat if valid model directory, null otherwise
     */
    private fun detectModelFormat(dir: File): ModelFormat? {
        if (!dir.exists() || !dir.isDirectory) return null

        // Check for MLC format (.adm files)
        val hasMlcFiles = MLC_REQUIRED_FILES.any { requiredFile ->
            File(dir, requiredFile).exists()
        } || dir.listFiles()?.any { file ->
            MLC_DEVICE_CODE_PATTERNS.any { pattern ->
                file.name.endsWith(pattern, ignoreCase = true)
            }
        } ?: false

        // Check for legacy MLC format (.ADco files)
        val hasLegacyMlcFiles = File(dir, "AVALibrary.ADco").exists() ||
            dir.listFiles()?.any { file ->
                file.name.endsWith(".ADco", ignoreCase = true)
            } ?: false

        if (hasMlcFiles || hasLegacyMlcFiles) {
            // Verify has tokenizer
            val hasTokenizer = TOKENIZER_PATTERNS.any { File(dir, it).exists() }
            if (hasTokenizer) return ModelFormat.MLC
        }

        // Check for GGUF format (manifest.json with format=AMG or .gguf files)
        val manifestFile = File(dir, "manifest.json")
        if (manifestFile.exists()) {
            try {
                val manifestText = manifestFile.readText()
                val jsonElement = json.parseToJsonElement(manifestText).jsonObject
                val format = jsonElement["format"]?.jsonPrimitive?.content
                when (format) {
                    "AMG" -> return ModelFormat.GGUF
                    "AMR" -> return ModelFormat.LITERT
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to parse manifest.json")
            }
        }

        // Check for raw GGUF files
        val hasGgufFile = dir.listFiles()?.any { file ->
            file.name.endsWith(".gguf", ignoreCase = true)
        } ?: false
        if (hasGgufFile) return ModelFormat.GGUF

        // Check for LiteRT task files
        val hasTaskFile = dir.listFiles()?.any { file ->
            file.name.endsWith(".task", ignoreCase = true)
        } ?: false
        if (hasTaskFile) return ModelFormat.LITERT

        return null
    }

    /**
     * Load model metadata from ava-model-config.json
     */
    private fun loadModelMetadata(modelDir: File): Map<String, String> {
        return try {
            // Try ava-model-config.json first (new format)
            val configFile = File(modelDir, "ava-model-config.json")
            if (!configFile.exists()) {
                // Fallback to mlc-chat-config.json (legacy)
                val legacyConfigFile = File(modelDir, "mlc-chat-config.json")
                if (!legacyConfigFile.exists()) {
                    Timber.d("No config file found in ${modelDir.name}")
                    return mapOf("model_name" to modelDir.name)
                }
                return parseModelConfig(legacyConfigFile)
            }

            parseModelConfig(configFile)

        } catch (e: Exception) {
            Timber.e(e, "Failed to load model metadata for ${modelDir.name}")
            mapOf("model_name" to modelDir.name)
        }
    }

    /**
     * Parse model configuration JSON
     */
    private fun parseModelConfig(configFile: File): Map<String, String> {
        val configText = configFile.readText()
        val jsonElement = json.parseToJsonElement(configText).jsonObject

        return mapOf(
            "model_name" to (jsonElement["model"]?.jsonPrimitive?.content ?: configFile.parentFile?.name ?: "unknown"),
            "model_type" to (jsonElement["model_type"]?.jsonPrimitive?.content ?: "unknown"),
            "vocab_size" to (jsonElement["vocab_size"]?.jsonPrimitive?.content ?: "unknown"),
            "context_window" to (jsonElement["context_window_size"]?.jsonPrimitive?.content ?: "unknown"),
            "temperature" to (jsonElement["temperature"]?.jsonPrimitive?.content ?: "unknown")
        )
    }

    /**
     * Calculate total size of directory
     */
    private fun calculateDirectorySize(dir: File): Long {
        var size = 0L
        dir.walkTopDown().forEach { file ->
            if (file.isFile) {
                size += file.length()
            }
        }
        return size
    }

    /**
     * Calculate SHA-256 checksum for a model directory
     *
     * Computes checksum of critical model files (weights, config, tokenizer)
     * to verify model integrity before loading.
     *
     * @param modelDir The model directory
     * @return SHA-256 hex string, or null if calculation fails
     */
    private fun calculateModelChecksum(modelDir: File): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")

            // Critical files to checksum (in order for deterministic hash)
            val criticalPatterns = listOf(
                ".adm", ".ads", ".so",           // MLC model code
                ".gguf",                          // GGUF weights
                ".task", ".tflite",               // LiteRT
                "tokenizer.ats", "tokenizer.ath", // Tokenizers
                "tokenizer.model", "tokenizer.json",
                "ava-model-config.json",          // Config
                "mlc-chat-config.json"
            )

            val filesToHash = modelDir.listFiles()
                ?.filter { file ->
                    file.isFile && criticalPatterns.any { pattern ->
                        file.name.endsWith(pattern, ignoreCase = true)
                    }
                }
                ?.sortedBy { it.name } // Deterministic order
                ?: return null

            if (filesToHash.isEmpty()) {
                Timber.w("No critical files found for checksum in ${modelDir.name}")
                return null
            }

            for (file in filesToHash) {
                FileInputStream(file).use { fis ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (fis.read(buffer).also { bytesRead = it } != -1) {
                        digest.update(buffer, 0, bytesRead)
                    }
                }
            }

            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Timber.e(e, "Failed to calculate checksum for ${modelDir.name}")
            null
        }
    }

    /**
     * Verify model integrity by checking checksum
     *
     * Compares calculated checksum against stored checksum in manifest.
     *
     * @param model The discovered model to verify
     * @return VerificationResult with status and details
     */
    suspend fun verifyModelIntegrity(model: DiscoveredModel): VerificationResult = withContext(Dispatchers.IO) {
        val modelDir = File(model.path)

        if (!modelDir.exists() || !modelDir.isDirectory) {
            return@withContext VerificationResult(
                isValid = false,
                status = VerificationStatus.NOT_FOUND,
                message = "Model directory not found: ${model.path}"
            )
        }

        // Load expected checksum from manifest
        val manifestFile = File(modelDir, "manifest.json")
        val expectedChecksum: String? = if (manifestFile.exists()) {
            try {
                val manifestText = manifestFile.readText()
                val jsonElement = json.parseToJsonElement(manifestText).jsonObject
                jsonElement["checksum"]?.jsonPrimitive?.content
            } catch (e: Exception) {
                Timber.w(e, "Failed to read checksum from manifest")
                null
            }
        } else {
            null
        }

        if (expectedChecksum == null) {
            return@withContext VerificationResult(
                isValid = true, // No checksum to verify against
                status = VerificationStatus.NO_CHECKSUM,
                message = "Model has no stored checksum (verification skipped)"
            )
        }

        // Calculate current checksum
        val currentChecksum = calculateModelChecksum(modelDir)

        if (currentChecksum == null) {
            return@withContext VerificationResult(
                isValid = false,
                status = VerificationStatus.CALCULATION_FAILED,
                message = "Failed to calculate checksum"
            )
        }

        // Compare
        val isValid = currentChecksum.equals(expectedChecksum, ignoreCase = true)

        if (isValid) {
            Timber.i("Model ${model.id} integrity verified (SHA-256: ${currentChecksum.take(16)}...)")
            VerificationResult(
                isValid = true,
                status = VerificationStatus.VERIFIED,
                message = "Checksum verified",
                checksum = currentChecksum
            )
        } else {
            Timber.e("Model ${model.id} checksum MISMATCH! Expected: $expectedChecksum, Got: $currentChecksum")
            VerificationResult(
                isValid = false,
                status = VerificationStatus.CHECKSUM_MISMATCH,
                message = "Checksum mismatch - model may be corrupted",
                expectedChecksum = expectedChecksum,
                actualChecksum = currentChecksum
            )
        }
    }

    /**
     * Result of model integrity verification
     */
    data class VerificationResult(
        val isValid: Boolean,
        val status: VerificationStatus,
        val message: String,
        val checksum: String? = null,
        val expectedChecksum: String? = null,
        val actualChecksum: String? = null
    )

    /**
     * Status of verification check
     */
    enum class VerificationStatus {
        VERIFIED,            // Checksum matches
        NO_CHECKSUM,         // No checksum in manifest (can't verify)
        CHECKSUM_MISMATCH,   // Checksum doesn't match (corrupted)
        CALCULATION_FAILED,  // Failed to calculate checksum
        NOT_FOUND            // Model directory not found
    }

    /**
     * Generate checksum file for a model
     *
     * Creates a checksum.sha256 file in the model directory.
     * Used by model packaging tools.
     *
     * @param modelDir The model directory
     * @return true if checksum file was created successfully
     */
    suspend fun generateChecksumFile(modelDir: File): Boolean = withContext(Dispatchers.IO) {
        val checksum = calculateModelChecksum(modelDir) ?: return@withContext false

        try {
            val checksumFile = File(modelDir, "checksum.sha256")
            checksumFile.writeText("$checksum  ${modelDir.name}\n")
            Timber.i("Generated checksum file for ${modelDir.name}")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to write checksum file")
            false
        }
    }

    /**
     * Information about a discovered model
     */
    data class DiscoveredModel(
        val id: String,                    // Model ID (directory name)
        val name: String,                  // Display name from config
        val path: String,                  // Absolute path to model directory
        val sizeBytes: Long,               // Total size in bytes
        val type: ModelType,               // Archive or extracted
        val format: ModelFormat = ModelFormat.MLC, // Runtime format
        val metadata: Map<String, String>, // Additional metadata
        val checksum: String? = null,      // SHA-256 checksum of model files
        val isVerified: Boolean = false    // Whether checksum has been verified
    ) {
        val sizeMB: Float get() = sizeBytes / 1_000_000f
        val sizeGB: Float get() = sizeBytes / 1_000_000_000f

        fun getDisplaySize(): String {
            return when {
                sizeBytes < 1_000_000 -> "${sizeBytes / 1_000}KB"
                sizeBytes < 1_000_000_000 -> "${String.format("%.1f", sizeMB)}MB"
                else -> "${String.format("%.2f", sizeGB)}GB"
            }
        }

        /**
         * Get the runtime engine name for this model
         */
        fun getRuntimeName(): String = format.runtime
    }

    /**
     * Type of model installation
     */
    enum class ModelType {
        ARCHIVE,    // .amm/.amg/.amr file (needs extraction)
        EXTRACTED   // Extracted directory (ready to use)
    }

    /**
     * Model runtime format
     *
     * AVA 3-character extension scheme:
     * - .amm = Ava Model MLC (TVM compiled)
     * - .amg = Ava Model GGUF (llama.cpp)
     * - .amr = Ava Model liteRT (Google AI Edge)
     */
    enum class ModelFormat(val extension: String, val runtime: String) {
        MLC(".amm", "mlc-llm"),           // MLC-LLM / TVM
        GGUF(".amg", "llama.cpp"),        // llama.cpp GGUF
        LITERT(".amr", "litert")          // Google AI Edge LiteRT
    }

    /**
     * Detailed status report for model discovery
     */
    data class ModelStatusReport(
        val packageName: String,
        val isDebugBuild: Boolean,
        val scannedLocations: List<String>,
        val existingLocations: List<String>,
        val discoveredModels: List<DiscoveredModel>,
        val errors: List<String>
    ) {
        val hasModels: Boolean get() = discoveredModels.isNotEmpty()
        val llmModels: List<DiscoveredModel> get() = discoveredModels
        val primaryModel: DiscoveredModel? get() = discoveredModels.maxByOrNull { it.sizeBytes }

        fun toLogString(): String = buildString {
            appendLine("=== Model Status Report ===")
            appendLine("Package: $packageName (debug: $isDebugBuild)")
            appendLine("Scanned: ${scannedLocations.size} locations")
            appendLine("Found: ${existingLocations.size} valid paths")
            appendLine("Models: ${discoveredModels.size}")
            discoveredModels.forEach { model ->
                appendLine("  - ${model.id} (${model.getDisplaySize()}, ${model.format.runtime})")
            }
            if (errors.isNotEmpty()) {
                appendLine("Errors: ${errors.size}")
                errors.forEach { appendLine("  ! $it") }
            }
        }
    }

    /**
     * Get a detailed status report of model discovery
     */
    suspend fun getStatusReport(): ModelStatusReport = withContext(Dispatchers.IO) {
        val packageName = context.packageName
        val isDebug = packageName.endsWith(".debug")
        val errors = mutableListOf<String>()

        val scanLocations = getScanLocations()
        val existingLocations = scanLocations.filter { it.exists() }.map { it.absolutePath }

        val models = try {
            discoverInstalledModels()
        } catch (e: Exception) {
            errors.add("Discovery failed: ${e.message}")
            emptyList()
        }

        ModelStatusReport(
            packageName = packageName,
            isDebugBuild = isDebug,
            scannedLocations = scanLocations.map { it.absolutePath },
            existingLocations = existingLocations,
            discoveredModels = models,
            errors = errors
        ).also {
            Timber.i(it.toLogString())
        }
    }
}
