/**
 * VivokaAssets.kt - Asset management and validation for Vivoka VSDK engine
 * 
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-28
 * 
 * Extracted from monolithic VivokaEngine.kt as part of SOLID refactoring
 * Handles asset extraction, validation, corruption detection, and recovery
 */
package com.augmentalis.voiceos.speech.engines.vivoka

import android.content.Context
import android.util.Log
import com.augmentalis.voiceos.speech.engines.common.SpeechError
import com.vivoka.vsdk.util.AssetsExtractor
import kotlinx.coroutines.*
import org.json.JSONObject
import org.json.JSONException
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages VSDK assets including extraction, validation, and integrity checking
 */
class VivokaAssets(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    
    companion object {
        private const val TAG = "VivokaAssets"
        private const val CHECKSUM_CACHE_FILE = "asset_checksums.json"
        private const val ASSET_MANIFEST_FILE = "asset_manifest.json"
        private const val CRITICAL_CONFIG_FILES = "vsdk.json,config.json"
        private const val SUPPORTED_VSDK_VERSION = "2.3.3"
        private const val CACHE_VALIDITY_HOURS = 24L
        private const val MAX_VALIDATION_RETRIES = 2
        private const val VSDK_CONFIG = "vsdk.json"
    }
    
    // Asset validation state
    private val checksumCache = ConcurrentHashMap<String, AssetChecksum>()
    private var lastAssetValidation = 0L
    private var assetValidationRetryCount = 0
    
    // Asset paths
    private var assetsPath: String = ""
    
    // Error handling
    private var assetErrorListener: ((String, Int) -> Unit)? = null
    
    data class AssetChecksum(
        val filePath: String,
        val sha256: String,
        val timestamp: Long,
        val fileSize: Long
    )
    
    data class AssetValidationResult(
        val isValid: Boolean,
        val reason: String,
        val needsReExtraction: Boolean = false,
        val validatedFiles: Int = 0,
        val totalFiles: Int = 0
    )
    
    /**
     * Initialize asset management with assets path
     */
    fun initialize(assetsPath: String) {
        this.assetsPath = assetsPath
        Log.d(TAG, "Asset management initialized for path: $assetsPath")
    }
    
    /**
     * Extract and validate VSDK assets with comprehensive checks
     */
    suspend fun extractAndValidateAssets(): AssetValidationResult {
        return retryAssetOperation(MAX_VALIDATION_RETRIES + 1) {
            try {
                Log.d(TAG, "Starting comprehensive asset extraction and validation")
                
                val assetsDir = File(assetsPath)
                var needsExtraction = !assetsDir.exists() || assetsDir.listFiles()?.isEmpty() == true
                
                // If assets exist, validate them first
                if (!needsExtraction) {
                    Log.d(TAG, "Assets exist, validating integrity...")
                    val validationResult = validateAssets()
                    
                    if (!validationResult.isValid) {
                        Log.w(TAG, "Asset validation failed: ${validationResult.reason}")
                        needsExtraction = validationResult.needsReExtraction
                        
                        if (needsExtraction) {
                            Log.i(TAG, "Corrupted assets detected, will re-extract")
                            if (assetsDir.exists()) {
                                assetsDir.deleteRecursively()
                            }
                        }
                    } else {
                        Log.i(TAG, "Asset validation successful: ${validationResult.reason}")
                        lastAssetValidation = System.currentTimeMillis()
                        return@retryAssetOperation validationResult
                    }
                }
                
                // Extract assets if needed
                if (needsExtraction) {
                    val extractionResult = extractAssets()
                    if (!extractionResult.isValid) {
                        throw Exception(extractionResult.reason)
                    }
                    
                    // Validate extracted assets
                    val postExtractionValidation = validateAssets()
                    if (!postExtractionValidation.isValid) {
                        throw Exception("Asset validation failed after extraction: ${postExtractionValidation.reason}")
                    }
                    
                    Log.i(TAG, "Asset extraction and validation completed successfully")
                    return@retryAssetOperation postExtractionValidation
                }
                
                // Should not reach here
                AssetValidationResult(false, "Unexpected asset validation state")
                
            } catch (e: Exception) {
                assetValidationRetryCount++
                Log.e(TAG, "Asset extraction/validation attempt $assetValidationRetryCount failed", e)
                throw e
            }
        }
    }
    
    /**
     * Extract VSDK assets from APK
     */
    private suspend fun extractAssets(): AssetValidationResult {
        return try {
            Log.i(TAG, "Extracting VSDK assets to $assetsPath")
            
            val assetsDir = File(assetsPath)
            
            // Ensure directory exists
            if (!assetsDir.exists()) {
                assetsDir.mkdirs()
            }
            
            // Extract from assets using VSDK AssetsExtractor
            withContext(Dispatchers.IO) {
                AssetsExtractor.extract(context, "vsdk", assetsPath)
            }
            
            // Count extracted files
            val extractedFiles = assetsDir.walkTopDown().filter { it.isFile }.count()
            
            Log.i(TAG, "Successfully extracted $extractedFiles asset files")
            
            AssetValidationResult(
                isValid = true,
                reason = "Assets extracted successfully",
                needsReExtraction = false,
                validatedFiles = extractedFiles,
                totalFiles = extractedFiles
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Asset extraction failed", e)
            AssetValidationResult(
                isValid = false,
                reason = "Asset extraction failed: ${e.message}",
                needsReExtraction = false
            )
        }
    }
    
    /**
     * Validate asset integrity with comprehensive checks
     */
    suspend fun validateAssets(): AssetValidationResult {
        return try {
            Log.d(TAG, "Starting comprehensive asset validation for: $assetsPath")
            
            val assetsDir = File(assetsPath)
            if (!assetsDir.exists()) {
                return AssetValidationResult(false, "Assets directory does not exist", true)
            }
            
            // Load cached checksums
            loadChecksumCache()
            
            var validatedFiles = 0
            var totalFiles = 0
            
            // Validate critical configuration files first
            val configValidation = validateConfigurationFiles()
            if (!configValidation.isValid) {
                return configValidation
            }
            validatedFiles += configValidation.validatedFiles
            totalFiles += configValidation.totalFiles
            
            // Validate model files
            val modelValidation = validateModelFiles()
            if (!modelValidation.isValid) {
                return modelValidation
            }
            validatedFiles += modelValidation.validatedFiles
            totalFiles += modelValidation.totalFiles
            
            // Validate asset manifest if exists
            val manifestValidation = validateAssetManifest()
            if (!manifestValidation.isValid) {
                return manifestValidation
            }
            validatedFiles += manifestValidation.validatedFiles
            totalFiles += manifestValidation.totalFiles
            
            // Check version compatibility
            val versionValidation = validateVersionCompatibility()
            if (!versionValidation.isValid) {
                return versionValidation
            }
            
            // Save updated checksum cache
            saveChecksumCache()
            
            Log.i(TAG, "Asset validation completed successfully - $validatedFiles/$totalFiles files validated")
            AssetValidationResult(
                isValid = true,
                reason = "All assets validated successfully",
                needsReExtraction = false,
                validatedFiles = validatedFiles,
                totalFiles = totalFiles
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Asset validation failed with exception", e)
            AssetValidationResult(false, "Validation error: ${e.message}", true)
        }
    }
    
    /**
     * Validate critical VSDK configuration files
     */
    private suspend fun validateConfigurationFiles(): AssetValidationResult {
        val criticalFiles = CRITICAL_CONFIG_FILES.split(",")
        var validatedCount = 0
        
        for (configFile in criticalFiles) {
            val file = File(assetsPath, configFile)
            if (!file.exists()) continue // Optional files
            
            Log.d(TAG, "Validating configuration file: $configFile")
            
            // Check file integrity
            if (!validateFileIntegrity(file)) {
                return AssetValidationResult(
                    false, 
                    "Configuration file corrupted: $configFile", 
                    true
                )
            }
            
            // Validate JSON structure for config files
            if (configFile.endsWith(".json")) {
                val jsonValidation = validateJSONStructure(file)
                if (!jsonValidation.isValid) {
                    return jsonValidation
                }
            }
            
            validatedCount++
        }
        
        return AssetValidationResult(
            true, 
            "Configuration files validated",
            validatedFiles = validatedCount,
            totalFiles = criticalFiles.size
        )
    }
    
    /**
     * Validate language model files
     */
    private suspend fun validateModelFiles(): AssetValidationResult {
        val assetsDir = File(assetsPath)
        val modelFiles = assetsDir.listFiles { file ->
            file.isFile && (file.name.contains("VoxMobileVoice") || 
                           file.name.endsWith(".bin") ||
                           file.name.endsWith(".model"))
        } ?: arrayOf()
        
        if (modelFiles.isEmpty()) {
            Log.w(TAG, "No model files found for validation")
            return AssetValidationResult(true, "No model files to validate")
        }
        
        var validatedCount = 0
        for (modelFile in modelFiles) {
            Log.d(TAG, "Validating model file: ${modelFile.name}")
            
            // Check file integrity
            if (!validateFileIntegrity(modelFile)) {
                return AssetValidationResult(
                    false,
                    "Model file corrupted: ${modelFile.name}",
                    true
                )
            }
            
            // Validate minimum file size (corrupted models are typically too small)
            if (modelFile.length() < 1024 * 100) { // Less than 100KB
                return AssetValidationResult(
                    false,
                    "Model file too small (likely corrupted): ${modelFile.name}",
                    true
                )
            }
            
            validatedCount++
        }
        
        return AssetValidationResult(
            true, 
            "Model files validated",
            validatedFiles = validatedCount,
            totalFiles = modelFiles.size
        )
    }
    
    /**
     * Validate asset manifest file if present
     */
    private suspend fun validateAssetManifest(): AssetValidationResult {
        val manifestFile = File(assetsPath, ASSET_MANIFEST_FILE)
        if (!manifestFile.exists()) {
            return AssetValidationResult(true, "No manifest file to validate")
        }
        
        return try {
            val manifestContent = manifestFile.readText()
            val manifest = JSONObject(manifestContent)
            
            // Validate required manifest fields
            val requiredFields = arrayOf("version", "files", "timestamp")
            for (field in requiredFields) {
                if (!manifest.has(field)) {
                    return AssetValidationResult(
                        false,
                        "Asset manifest missing required field: $field",
                        true
                    )
                }
            }
            
            // Validate files listed in manifest exist
            val files = manifest.getJSONArray("files")
            var validatedCount = 0
            
            for (i in 0 until files.length()) {
                val fileObj = files.getJSONObject(i)
                val fileName = fileObj.getString("name")
                val expectedChecksum = fileObj.optString("checksum", "")
                
                val file = File(assetsPath, fileName)
                if (!file.exists()) {
                    return AssetValidationResult(
                        false,
                        "Manifest file missing: $fileName",
                        true
                    )
                }
                
                // Validate checksum if provided
                if (expectedChecksum.isNotEmpty()) {
                    val actualChecksum = calculateSHA256(file)
                    if (actualChecksum != expectedChecksum) {
                        return AssetValidationResult(
                            false,
                            "Checksum mismatch for $fileName",
                            true
                        )
                    }
                }
                validatedCount++
            }
            
            AssetValidationResult(
                true, 
                "Asset manifest validated",
                validatedFiles = validatedCount,
                totalFiles = files.length()
            )
            
        } catch (e: JSONException) {
            AssetValidationResult(
                false,
                "Invalid asset manifest JSON: ${e.message}",
                true
            )
        }
    }
    
    /**
     * Validate VSDK version compatibility
     */
    private suspend fun validateVersionCompatibility(): AssetValidationResult {
        val configFile = File(assetsPath, VSDK_CONFIG)
        if (!configFile.exists()) {
            return AssetValidationResult(true, "No VSDK config for version check")
        }
        
        return try {
            val configContent = configFile.readText()
            val config = JSONObject(configContent)
            
            val version = config.optString("version", "")
            if (version.isEmpty()) {
                return AssetValidationResult(true, "No version specified in config")
            }
            
            // Check if version is compatible
            if (!isVersionCompatible(version, SUPPORTED_VSDK_VERSION)) {
                return AssetValidationResult(
                    false,
                    "Incompatible VSDK version: $version (expected: $SUPPORTED_VSDK_VERSION)",
                    true
                )
            }
            
            AssetValidationResult(true, "VSDK version compatible: $version")
            
        } catch (e: JSONException) {
            AssetValidationResult(
                false,
                "Invalid VSDK config JSON: ${e.message}",
                true
            )
        }
    }
    
    /**
     * Validate file integrity using cached checksums
     */
    private fun validateFileIntegrity(file: File): Boolean {
        return try {
            val filePath = file.absolutePath
            val cachedChecksum = checksumCache[filePath]
            
            // If we have a cached checksum and file hasn't changed, use cache
            if (cachedChecksum != null && 
                cachedChecksum.fileSize == file.length() &&
                System.currentTimeMillis() - cachedChecksum.timestamp < CACHE_VALIDITY_HOURS * 3600000) {
                return true
            }
            
            // Calculate new checksum
            val actualChecksum = calculateSHA256(file)
            
            // Update cache
            checksumCache[filePath] = AssetChecksum(
                filePath = filePath,
                sha256 = actualChecksum,
                timestamp = System.currentTimeMillis(),
                fileSize = file.length()
            )
            
            // If we had a cached checksum, compare it
            if (cachedChecksum != null) {
                return actualChecksum == cachedChecksum.sha256
            }
            
            // No previous checksum - assume valid for new files
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to validate file integrity: ${file.name}", e)
            false
        }
    }
    
    /**
     * Validate JSON file structure
     */
    private fun validateJSONStructure(file: File): AssetValidationResult {
        return try {
            val content = file.readText()
            JSONObject(content) // This will throw if invalid
            AssetValidationResult(true, "JSON structure valid")
        } catch (e: JSONException) {
            AssetValidationResult(
                false,
                "Invalid JSON in ${file.name}: ${e.message}",
                true
            )
        }
    }
    
    /**
     * Calculate SHA-256 checksum of file
     */
    private fun calculateSHA256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Check version compatibility
     */
    private fun isVersionCompatible(actual: String, expected: String): Boolean {
        return try {
            val actualParts = actual.split(".").map { it.toIntOrNull() ?: 0 }
            val expectedParts = expected.split(".").map { it.toIntOrNull() ?: 0 }
            
            // Major version must match
            if (actualParts.getOrNull(0) != expectedParts.getOrNull(0)) {
                return false
            }
            
            // Minor version should be >= expected
            val actualMinor = actualParts.getOrNull(1) ?: 0
            val expectedMinor = expectedParts.getOrNull(1) ?: 0
            
            actualMinor >= expectedMinor
        } catch (e: Exception) {
            Log.e(TAG, "Version compatibility check failed", e)
            false
        }
    }
    
    /**
     * Load checksum cache from storage
     */
    private fun loadChecksumCache() {
        try {
            val cacheFile = File(assetsPath, CHECKSUM_CACHE_FILE)
            if (!cacheFile.exists()) return
            
            val cacheContent = cacheFile.readText()
            val cacheJson = JSONObject(cacheContent)
            
            val filesArray = cacheJson.getJSONArray("files")
            for (i in 0 until filesArray.length()) {
                val fileObj = filesArray.getJSONObject(i)
                val checksum = AssetChecksum(
                    filePath = fileObj.getString("filePath"),
                    sha256 = fileObj.getString("sha256"),
                    timestamp = fileObj.getLong("timestamp"),
                    fileSize = fileObj.getLong("fileSize")
                )
                checksumCache[checksum.filePath] = checksum
            }
            
            Log.d(TAG, "Loaded ${checksumCache.size} cached checksums")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load checksum cache, starting fresh", e)
            checksumCache.clear()
        }
    }
    
    /**
     * Save checksum cache to storage
     */
    private fun saveChecksumCache() {
        try {
            val cacheFile = File(assetsPath, CHECKSUM_CACHE_FILE)
            val cacheJson = JSONObject()
            val filesArray = org.json.JSONArray()
            
            for (checksum in checksumCache.values) {
                val fileObj = JSONObject().apply {
                    put("filePath", checksum.filePath)
                    put("sha256", checksum.sha256)
                    put("timestamp", checksum.timestamp)
                    put("fileSize", checksum.fileSize)
                }
                filesArray.put(fileObj)
            }
            
            cacheJson.put("files", filesArray)
            cacheJson.put("lastUpdated", System.currentTimeMillis())
            
            cacheFile.writeText(cacheJson.toString(2))
            Log.d(TAG, "Saved ${checksumCache.size} checksums to cache")
            
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save checksum cache", e)
        }
    }
    
    /**
     * Perform periodic asset integrity check during runtime
     */
    suspend fun performPeriodicAssetCheck(): AssetValidationResult {
        return try {
            val currentTime = System.currentTimeMillis()
            
            // Only check every hour to avoid overhead
            if (currentTime - lastAssetValidation < 3600000) {
                return AssetValidationResult(true, "Recent validation still valid")
            }
            
            Log.d(TAG, "Performing periodic asset integrity check")
            
            val validationResult = validateAssets()
            if (!validationResult.isValid) {
                Log.w(TAG, "Periodic asset check failed: ${validationResult.reason}")
                
                if (validationResult.needsReExtraction) {
                    Log.e(TAG, "Critical asset corruption detected during runtime")
                    assetErrorListener?.invoke(
                        "Asset corruption detected: ${validationResult.reason}",
                        SpeechError.INITIALIZATION_ERROR
                    )
                }
            } else {
                lastAssetValidation = currentTime
                Log.d(TAG, "Periodic asset check passed")
            }
            
            validationResult
            
        } catch (e: Exception) {
            Log.e(TAG, "Periodic asset check failed with exception", e)
            AssetValidationResult(false, "Periodic check failed: ${e.message}")
        }
    }
    
    /**
     * Retry asset operation with exponential backoff
     */
    private suspend fun retryAssetOperation(maxRetries: Int, operation: suspend () -> AssetValidationResult): AssetValidationResult {
        repeat(maxRetries) { attempt ->
            try {
                Log.d(TAG, "Asset operation attempt ${attempt + 1}/$maxRetries")
                return operation()
            } catch (e: Exception) {
                if (attempt < maxRetries - 1) {
                    val delay = 1000L * (1L shl attempt) // Exponential backoff
                    Log.w(TAG, "Asset operation failed (attempt ${attempt + 1}), retrying in ${delay}ms", e)
                    delay(delay)
                } else {
                    Log.e(TAG, "Asset operation failed after $maxRetries attempts", e)
                    return AssetValidationResult(false, "Operation failed after $maxRetries attempts: ${e.message}", true)
                }
            }
        }
        return AssetValidationResult(false, "Unexpected retry loop exit")
    }
    
    /**
     * Clear validation cache
     */
    fun clearCache() {
        checksumCache.clear()
        lastAssetValidation = 0L
        assetValidationRetryCount = 0
        Log.i(TAG, "Asset validation cache cleared")
    }
    
    /**
     * Force asset re-validation
     */
    suspend fun forceAssetRevalidation(): AssetValidationResult {
        Log.i(TAG, "Forcing asset re-validation")
        clearCache()
        return extractAndValidateAssets()
    }
    
    /**
     * Get asset validation status
     */
    fun getAssetValidationStatus(): Map<String, Any> {
        return mapOf(
            "lastValidation" to lastAssetValidation,
            "retryCount" to assetValidationRetryCount,
            "timeSinceValidation" to (System.currentTimeMillis() - lastAssetValidation),
            "needsValidation" to (System.currentTimeMillis() - lastAssetValidation > 3600000),
            "cachedChecksums" to checksumCache.size,
            "assetsPath" to assetsPath
        )
    }
    
    /**
     * Set error listener for asset-related errors
     */
    fun setErrorListener(listener: (String, Int) -> Unit) {
        this.assetErrorListener = listener
    }
    
    /**
     * Reset asset management
     */
    fun reset() {
        Log.d(TAG, "Resetting asset management")
        clearCache()
        assetsPath = ""
    }
}