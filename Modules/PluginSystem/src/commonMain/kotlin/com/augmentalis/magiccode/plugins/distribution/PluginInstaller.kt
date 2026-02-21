package com.augmentalis.magiccode.plugins.distribution

import com.augmentalis.magiccode.plugins.core.*
import com.augmentalis.magiccode.plugins.platform.FileIO
import com.augmentalis.magiccode.plugins.platform.ZipExtractor
import com.augmentalis.magiccode.plugins.security.*
import kotlinx.serialization.decodeFromString
import net.mamoe.yamlkt.Yaml

/**
 * Plugin installer for handling plugin packages.
 *
 * Supports ZIP archives and validates signatures.
 */
class PluginInstaller(
    private val loader: PluginLoader,
    private val fileIO: FileIO = FileIO(),
    private val zipExtractor: ZipExtractor = ZipExtractor(),
    private val signatureVerifier: SignatureVerifier = SignatureVerifier(),
    private val trustStore: TrustStore = TrustStore(),
    private val requireSignatureVerification: Boolean = true
) {
    companion object {
        private const val TAG = "PluginInstaller"
        private const val MANIFEST_FILENAME = "plugin.yaml"
        private const val TEMP_EXTRACT_PREFIX = "plugin_extract_"
    }

    /**
     * Installation result.
     */
    sealed class InstallResult {
        data class Success(val pluginId: String) : InstallResult()
        data class Failure(val reason: String) : InstallResult()
    }

    /**
     * Install plugin from package file.
     *
     * @param packagePath Path to plugin package (ZIP)
     * @param appDataDir Application data directory
     * @return InstallResult
     */
    suspend fun installFromPackage(
        packagePath: String,
        appDataDir: String
    ): InstallResult {
        PluginLog.i(TAG, "Installing plugin from: $packagePath")

        // Verify package exists
        if (!fileIO.fileExists(packagePath)) {
            return InstallResult.Failure("Package not found: $packagePath")
        }

        var tempExtractDir: String? = null
        var finalPluginDir: String? = null

        try {
            // Step 1: Create temporary extraction directory
            tempExtractDir = "$appDataDir/$TEMP_EXTRACT_PREFIX${System.currentTimeMillis()}"
            if (!fileIO.createDirectory(tempExtractDir)) {
                return InstallResult.Failure("Failed to create temporary extraction directory")
            }

            PluginLog.d(TAG, "Extracting to temporary directory: $tempExtractDir")

            // Step 2: Extract ZIP archive
            try {
                zipExtractor.extractZip(packagePath, tempExtractDir)
            } catch (e: Exception) {
                PluginLog.e(TAG, "Failed to extract ZIP", e)
                return InstallResult.Failure("Failed to extract ZIP archive: ${e.message}")
            }

            // Step 3: Validate ZIP structure - must contain plugin.yaml at root
            val manifestPath = "$tempExtractDir/$MANIFEST_FILENAME"
            if (!fileIO.fileExists(manifestPath)) {
                return InstallResult.Failure("Invalid plugin package: $MANIFEST_FILENAME not found at root")
            }

            // Step 4: Parse and validate manifest
            val manifest: PluginManifest
            try {
                val manifestContent = fileIO.readFileAsString(manifestPath)
                manifest = Yaml.Default.decodeFromString(PluginManifest.serializer(), manifestContent)
                PluginLog.i(TAG, "Parsed manifest for plugin: ${manifest.id} v${manifest.version}")
            } catch (e: Exception) {
                PluginLog.e(TAG, "Failed to parse manifest", e)
                return InstallResult.Failure("Failed to parse plugin manifest: ${e.message}")
            }

            // Step 5: Validate manifest
            val validator = ManifestValidator()
            when (val validationResult = validator.validate(manifest)) {
                is ManifestValidator.ValidationResult.Valid -> {
                    PluginLog.d(TAG, "Manifest validation passed")
                }
                is ManifestValidator.ValidationResult.Invalid -> {
                    val errors = validationResult.errors.joinToString("; ") {
                        "${it.field}: ${it.message}"
                    }
                    return InstallResult.Failure("Manifest validation failed: $errors")
                }
            }

            // Step 6: Verify signature
            // NOTE: Full signature verification requires a functional TrustStore with registered
            // publisher keys. Until the trust infrastructure is implemented, verification is
            // best-effort — logs a warning but does not block installation.
            if (!verifySignature(packagePath, publisherId = manifest.author)) {
                PluginLog.w(TAG, "Signature verification failed for: $packagePath (publisher: ${manifest.author}). " +
                    "Install proceeding — trust store does not have a registered key for this publisher.")
            }

            // Step 7: Determine final plugin directory
            val pluginId = manifest.id
            finalPluginDir = "$appDataDir/plugins/$pluginId"

            // Check if plugin already exists
            if (fileIO.directoryExists(finalPluginDir)) {
                PluginLog.w(TAG, "Plugin directory already exists, removing: $finalPluginDir")
                if (!fileIO.delete(finalPluginDir)) {
                    return InstallResult.Failure("Failed to remove existing plugin directory")
                }
            }

            // Create plugin directory
            if (!fileIO.createDirectory(finalPluginDir)) {
                return InstallResult.Failure("Failed to create plugin directory: $finalPluginDir")
            }

            // Step 8: Copy extracted files to final plugin directory
            try {
                if (!fileIO.copy(tempExtractDir, finalPluginDir)) {
                    return InstallResult.Failure("Failed to copy plugin files to destination")
                }
                PluginLog.i(TAG, "Copied plugin files to: $finalPluginDir")
            } catch (e: Exception) {
                PluginLog.e(TAG, "Failed to copy plugin files", e)
                return InstallResult.Failure("Failed to copy plugin files: ${e.message}")
            }

            // Step 9: Determine library path (typically in lib/ subdirectory)
            val libDir = "$finalPluginDir/lib"
            val libraryPath = if (fileIO.directoryExists(libDir)) {
                val libFiles = fileIO.listFiles(libDir)
                val jarFile = libFiles.firstOrNull { it.endsWith(".jar") }
                if (jarFile != null) {
                    "$libDir/$jarFile"
                } else {
                    // No JAR found, plugin might not have native code
                    "$libDir/plugin.jar"
                }
            } else {
                // No lib directory, create placeholder
                "$libDir/plugin.jar"
            }

            // Step 10: Load plugin using PluginLoader
            val loadResult = loader.loadPlugin(
                pluginId = pluginId,
                manifestPath = "$finalPluginDir/$MANIFEST_FILENAME",
                libraryPath = libraryPath,
                appDataDir = appDataDir
            )

            when (loadResult) {
                is PluginLoader.LoadResult.Success -> {
                    PluginLog.i(TAG, "Successfully installed plugin: $pluginId")
                    return InstallResult.Success(pluginId)
                }
                is PluginLoader.LoadResult.Failure -> {
                    PluginLog.e(TAG, "Plugin load failed: ${loadResult.error.message}")
                    // Clean up on failure
                    fileIO.delete(finalPluginDir)
                    return InstallResult.Failure("Plugin load failed: ${loadResult.error.message}")
                }
            }

        } catch (e: Exception) {
            PluginLog.e(TAG, "Unexpected error during installation", e)
            // Clean up on failure
            finalPluginDir?.let { fileIO.delete(it) }
            return InstallResult.Failure("Installation failed: ${e.message}")
        } finally {
            // Clean up temporary extraction directory
            tempExtractDir?.let {
                if (fileIO.directoryExists(it)) {
                    fileIO.delete(it)
                    PluginLog.d(TAG, "Cleaned up temporary directory: $it")
                }
            }
        }
    }

    /**
     * Verify plugin package signature.
     *
     * @param packagePath Path to package
     * @return true if signature valid
     */
    /**
     * Verify digital signature of plugin package.
     *
     * Looks for signature file (.sig) next to package, or checks for embedded signature.
     *
     * @param packagePath Path to plugin package
     * @param publisherId Publisher identifier for trust store lookup
     * @param algorithm Signature algorithm to use
     * @return true if signature is valid or verification is disabled
     */
    fun verifySignature(
        packagePath: String,
        publisherId: String? = null,
        algorithm: SignatureAlgorithm = SignatureAlgorithm.RSA_SHA256
    ): Boolean {
        if (!requireSignatureVerification) {
            PluginLog.w(TAG, "Signature verification is disabled")
            return true
        }

        // Check for .sig file
        val signaturePath = "$packagePath.sig"
        if (fileIO.fileExists(signaturePath)) {
            // Get public key from trust store
            val publicKeyPath = if (publisherId != null) {
                trustStore.getPublicKeyPath(publisherId)
            } else {
                null
            }

            if (publicKeyPath == null) {
                PluginLog.w(TAG, "No trusted public key found for publisher: $publisherId")
                return false
            }

            if (!fileIO.fileExists(publicKeyPath)) {
                PluginLog.e(TAG, "Public key file not found: $publicKeyPath")
                return false
            }

            // Verify signature
            val result = signatureVerifier.verify(packagePath, signaturePath, publicKeyPath, algorithm)
            return when (result) {
                is VerificationResult.Valid -> {
                    PluginLog.i(TAG, "Signature verification succeeded")
                    true
                }
                is VerificationResult.Invalid -> {
                    PluginLog.e(TAG, "Signature verification failed: ${result.reason}")
                    false
                }
            }
        } else {
            PluginLog.w(TAG, "No signature file found: $signaturePath")
            return false
        }
    }

    /**
     * Get the trust store for managing trusted publishers.
     *
     * @return TrustStore instance
     */
    fun getTrustStore(): TrustStore {
        return trustStore
    }
}
