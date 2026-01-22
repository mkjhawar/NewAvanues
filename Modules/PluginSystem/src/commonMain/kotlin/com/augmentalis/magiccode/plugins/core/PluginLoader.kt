package com.augmentalis.magiccode.plugins.core

import com.augmentalis.magiccode.plugins.platform.PluginClassLoader
import com.augmentalis.magiccode.plugins.platform.FileIO
import kotlinx.serialization.decodeFromString
import net.mamoe.yamlkt.Yaml

/**
 * Plugin loader for loading and validating plugins.
 *
 * The PluginLoader orchestrates the complete plugin loading lifecycle, including:
 * - Manifest discovery and parsing
 * - Schema and functional requirement validation
 * - Directory structure validation
 * - Plugin class loading
 * - Namespace creation and isolation
 * - Registry management
 *
 * ## Loading Lifecycle
 * The loader executes plugins through an 8-step loading process:
 * 1. Read and parse plugin.yaml manifest
 * 2. Validate manifest schema and functional requirements
 * 3. Check for ID conflicts in registry
 * 4. Create isolated plugin namespace
 * 5. Validate directory structure
 * 6. Load plugin class (platform-specific)
 * 7. Register plugin in registry
 * 8. Return success/failure result
 *
 * ## Usage
 * ```kotlin
 * val loader = PluginLoader(
 *     config = PluginConfig(),
 *     registry = PluginRegistry()
 * )
 *
 * val result = loader.loadPlugin(
 *     pluginId = "com.example.plugin",
 *     manifestPath = "/plugins/com.example.plugin/plugin.yaml",
 *     libraryPath = "/plugins/com.example.plugin/lib/plugin.jar",
 *     appDataDir = "/data"
 * )
 *
 * when (result) {
 *     is LoadResult.Success -> println("Loaded: ${result.pluginInfo.manifest.name}")
 *     is LoadResult.Failure -> println("Failed: ${result.error.message}")
 * }
 * ```
 *
 * @property config Plugin system configuration
 * @property registry Plugin registry for tracking loaded plugins
 * @since 1.0.0
 * @see PluginRegistry
 * @see PluginManifest
 * @see ManifestValidator
 */
class PluginLoader(
    private val config: PluginConfig = PluginConfig(),
    private val registry: PluginRegistry = PluginRegistry()
) {
    private val validator = ManifestValidator(config)
    private val requirementValidator = PluginRequirementValidator()
    private val fileIO = FileIO()

    companion object {
        private const val TAG = "PluginLoader"
        private const val MANIFEST_FILENAME_YAML = "plugin.yaml"
        private const val MANIFEST_FILENAME_AVU = "plugin.avu"
    }

    /**
     * Result of a plugin load operation.
     *
     * Encapsulates the outcome of attempting to load a plugin, providing either
     * successful plugin information or detailed error information.
     *
     * @since 1.0.0
     * @see loadPlugin
     */
    sealed class LoadResult {
        /**
         * Plugin loaded successfully.
         *
         * @property pluginInfo Complete plugin information including manifest and state
         */
        data class Success(val pluginInfo: PluginRegistry.PluginInfo) : LoadResult()

        /**
         * Plugin loading failed.
         *
         * @property error Exception describing what went wrong during loading
         */
        data class Failure(val error: PluginException) : LoadResult()
    }

    /**
     * Load a plugin from the specified paths.
     *
     * Executes the complete plugin loading lifecycle as described in the class documentation.
     * This method performs comprehensive validation and error handling at each step.
     *
     * ## Error Handling
     * All errors are caught and converted to appropriate [PluginException] subclasses,
     * returned as [LoadResult.Failure]. Common failure reasons include:
     * - Invalid or missing manifest file
     * - Manifest validation errors
     * - Plugin ID conflicts
     * - Missing or invalid plugin library
     * - Class loading failures
     *
     * ## Example
     * ```kotlin
     * val result = loader.loadPlugin(
     *     pluginId = "com.example.image-filters",
     *     manifestPath = "/plugins/com.example.image-filters/plugin.yaml",
     *     libraryPath = "/plugins/com.example.image-filters/lib/plugin.jar",
     *     appDataDir = "/data/app"
     * )
     * ```
     *
     * @param pluginId Unique plugin identifier (must match manifest ID)
     * @param manifestPath Absolute path to plugin.yaml manifest file
     * @param libraryPath Absolute path to plugin library (JAR/APK)
     * @param appDataDir Application data directory for creating plugin namespace
     * @return [LoadResult.Success] with plugin info, or [LoadResult.Failure] with error details
     * @see LoadResult
     * @see uninstallPlugin
     */
    suspend fun loadPlugin(
        pluginId: String,
        manifestPath: String,
        libraryPath: String,
        appDataDir: String
    ): LoadResult {
        PluginLog.i(TAG, "Loading plugin: $pluginId from $manifestPath")

        try {
            // Step 1: Read and parse manifest
            val manifestContent = PluginErrorHandler.withErrorHandling(
                category = PluginErrorHandler.ErrorCategory.MANIFEST,
                pluginId = pluginId,
                operation = "Reading manifest"
            ) {
                readManifestFile(manifestPath)
            }

            val manifest = PluginErrorHandler.withErrorHandling(
                category = PluginErrorHandler.ErrorCategory.MANIFEST,
                pluginId = pluginId,
                operation = "Parsing manifest"
            ) {
                parseManifest(manifestContent)
            }

            // Verify plugin ID matches
            PluginErrorHandler.require(
                condition = manifest.id == pluginId,
                category = PluginErrorHandler.ErrorCategory.VALIDATION,
                pluginId = pluginId
            ) {
                "Plugin ID mismatch: expected $pluginId, got ${manifest.id}"
            }

            // Step 2: Validate manifest schema
            when (val validationResult = validator.validate(manifest)) {
                is ManifestValidator.ValidationResult.Valid -> {
                    PluginLog.d(TAG, "Manifest validation passed for $pluginId")
                }
                is ManifestValidator.ValidationResult.Invalid -> {
                    val errorMessage = validationResult.errors.joinToString("; ") {
                        "${it.field}: ${it.message}"
                    }
                    return LoadResult.Failure(
                        ManifestInvalidException("Manifest validation failed: $errorMessage")
                    )
                }
            }

            // Step 2b: Validate functional requirements (FR-001, FR-002, FR-018)
            val pluginRoot = fileIO.getParentDirectory(manifestPath)
            when (val reqValidation = requirementValidator.validateAll(pluginRoot, manifest)) {
                is PluginRequirementValidator.ValidationResult.Valid -> {
                    PluginLog.d(TAG, "Functional requirement validation passed for $pluginId")
                }
                is PluginRequirementValidator.ValidationResult.Invalid -> {
                    val violations = reqValidation.violations.joinToString("; ") {
                        "[${it.requirementId}] ${it.field}: ${it.message}"
                    }
                    return LoadResult.Failure(
                        ManifestInvalidException("Functional requirement validation failed: $violations")
                    )
                }
            }

            // Step 3: Check if plugin already registered
            PluginErrorHandler.require(
                condition = !registry.isRegistered(pluginId),
                category = PluginErrorHandler.ErrorCategory.NAMESPACE,
                pluginId = pluginId
            ) {
                "Plugin already registered: $pluginId"
            }

            // Step 4: Create namespace
            val namespace = PluginErrorHandler.withErrorHandling(
                category = PluginErrorHandler.ErrorCategory.NAMESPACE,
                pluginId = pluginId,
                operation = "Creating namespace"
            ) {
                PluginNamespace.create(pluginId, appDataDir)
            }

            // Step 5: Validate directory structure
            PluginErrorHandler.withErrorHandling(
                category = PluginErrorHandler.ErrorCategory.FILESYSTEM,
                pluginId = pluginId,
                operation = "Validating directory structure"
            ) {
                validateDirectoryStructure(manifestPath, libraryPath, manifest)
            }

            // Step 6: Load plugin class (platform-specific)
            val classLoader = PluginClassLoader()
            PluginErrorHandler.withErrorHandling(
                category = PluginErrorHandler.ErrorCategory.INSTALLATION,
                pluginId = pluginId,
                operation = "Loading plugin class ${manifest.entrypoint}"
            ) {
                classLoader.loadClass(manifest.entrypoint, libraryPath)
                PluginLog.i(TAG, "Successfully loaded plugin class: ${manifest.entrypoint}")
            }

            // Step 7: Register plugin
            val registered = PluginErrorHandler.withErrorHandling(
                category = PluginErrorHandler.ErrorCategory.NAMESPACE,
                pluginId = pluginId,
                operation = "Registering plugin"
            ) {
                registry.register(manifest, namespace)
            }

            PluginErrorHandler.require(
                condition = registered,
                category = PluginErrorHandler.ErrorCategory.NAMESPACE,
                pluginId = pluginId
            ) {
                "Failed to register plugin: $pluginId (namespace collision)"
            }

            // Step 8: Return success
            val pluginInfo = registry.getPlugin(pluginId)
                ?: throw PluginNotFoundException(pluginId)
            PluginLog.i(TAG, "Successfully loaded plugin: $pluginId v${manifest.version}")
            return LoadResult.Success(pluginInfo)

        } catch (e: PluginException) {
            PluginLog.e(TAG, "Failed to load plugin: $pluginId", e)
            return LoadResult.Failure(e)
        } catch (e: Exception) {
            PluginLog.e(TAG, "Unexpected error loading plugin: $pluginId", e)
            return LoadResult.Failure(
                InstallationFailedException(pluginId, "Unexpected error during load", e)
            )
        }
    }

    /**
     * Read manifest file content.
     *
     * @param manifestPath Path to manifest file
     * @return Manifest content as string
     * @throws ManifestNotFoundException if file doesn't exist
     */
    private fun readManifestFile(manifestPath: String): String {
        if (!fileIO.fileExists(manifestPath)) {
            throw ManifestNotFoundException(manifestPath)
        }

        return try {
            fileIO.readFileAsString(manifestPath)
        } catch (e: Exception) {
            throw ManifestInvalidException("Failed to read manifest file: $manifestPath", e)
        }
    }

    /**
     * Parse manifest content (supports both YAML and AVU formats).
     *
     * The parser automatically detects the format:
     * - AVU format: Lines starting with PLG: or containing "Avanues Universal Plugin Format"
     * - YAML format: Everything else
     *
     * @param content Manifest string content (YAML or AVU format)
     * @return Parsed PluginManifest
     * @throws ManifestInvalidException if parsing fails
     */
    private fun parseManifest(content: String): PluginManifest {
        return try {
            // Detect AVU format
            if (AvuManifestParser.isAvuManifest(content)) {
                PluginLog.d(TAG, "Detected AVU format manifest, using AvuManifestParser")
                AvuManifestParser.parse(content)
                    ?: throw ManifestInvalidException("Failed to parse AVU manifest: invalid format")
            } else {
                // Default to YAML format
                PluginLog.d(TAG, "Using YAML format manifest parser")
                Yaml.Default.decodeFromString(PluginManifest.serializer(), content)
            }
        } catch (e: ManifestInvalidException) {
            throw e
        } catch (e: Exception) {
            throw ManifestInvalidException("Failed to parse manifest", e)
        }
    }

    /**
     * Validate plugin directory structure.
     *
     * Ensures required directories and files exist.
     *
     * @param manifestPath Path to manifest
     * @param libraryPath Path to library
     * @param manifest Parsed manifest
     */
    private fun validateDirectoryStructure(
        manifestPath: String,
        libraryPath: String,
        manifest: PluginManifest
    ) {
        // Directory structure validation
        // Expected structure:
        // plugin-root/
        //   plugin.yaml
        //   lib/
        //     plugin.jar
        //   assets/
        //     images/
        //     fonts/
        //     icons/
        //   themes/

        PluginLog.d(TAG, "Validating directory structure for ${manifest.id}")

        // Get plugin root directory from manifest path
        val pluginRoot = fileIO.getParentDirectory(manifestPath)
        if (!fileIO.directoryExists(pluginRoot)) {
            throw InstallationFailedException(
                manifest.id,
                "Plugin root directory does not exist: $pluginRoot"
            )
        }

        // Validate library file exists
        if (!fileIO.fileExists(libraryPath)) {
            throw InstallationFailedException(
                manifest.id,
                "Plugin library file not found: $libraryPath"
            )
        }

        // Validate library is in lib/ subdirectory
        val libDir = "$pluginRoot/lib"
        if (!libraryPath.startsWith(libDir)) {
            PluginLog.w(TAG, "Library file not in standard lib/ directory: $libraryPath")
        }

        // Check for optional asset directories if manifest declares assets
        if (manifest.assets != null) {
            val assetsDir = "$pluginRoot/assets"
            if (!fileIO.directoryExists(assetsDir)) {
                PluginLog.w(TAG, "Manifest declares assets but assets/ directory not found: $assetsDir")
            } else {
                // Validate asset subdirectories based on manifest
                if (manifest.assets.images?.isNotEmpty() == true) {
                    validateAssetDirectory(pluginRoot, "assets/images", manifest.id)
                }
                if (manifest.assets.fonts?.isNotEmpty() == true) {
                    validateAssetDirectory(pluginRoot, "assets/fonts", manifest.id)
                }
                if (manifest.assets.icons?.isNotEmpty() == true) {
                    validateAssetDirectory(pluginRoot, "assets/icons", manifest.id)
                }
            }

            // Validate themes directory if manifest declares themes
            if (manifest.assets.themes?.isNotEmpty() == true) {
                validateAssetDirectory(pluginRoot, "themes", manifest.id)
            }
        }

        PluginLog.d(TAG, "Directory structure validation passed for ${manifest.id}")
    }

    /**
     * Validate an asset directory exists.
     *
     * @param pluginRoot Plugin root directory
     * @param relativePath Relative path from plugin root
     * @param pluginId Plugin identifier for logging
     */
    private fun validateAssetDirectory(
        pluginRoot: String,
        relativePath: String,
        pluginId: String
    ) {
        val fullPath = "$pluginRoot/$relativePath"
        if (!fileIO.directoryExists(fullPath)) {
            PluginLog.w(TAG, "Asset directory not found for $pluginId: $fullPath")
        }
    }

    /**
     * Uninstall a plugin.
     *
     * Removes the plugin from the registry and cleans up its isolated namespace,
     * including all plugin-specific data and resources. The plugin state is set to
     * [PluginState.UNINSTALLING] during the operation and removed completely on success.
     *
     * ## Cleanup Process
     * 1. Update state to UNINSTALLING
     * 2. Cleanup plugin namespace (files, preferences, etc.)
     * 3. Unregister from registry
     *
     * If cleanup fails, a warning is logged but uninstallation continues. If any
     * error occurs, the plugin state is set to [PluginState.FAILED].
     *
     * @param pluginId Plugin identifier to uninstall
     * @return true if uninstalled successfully, false if plugin not found or error occurred
     * @see loadPlugin
     */
    suspend fun uninstallPlugin(pluginId: String): Boolean {
        PluginLog.i(TAG, "Uninstalling plugin: $pluginId")

        val pluginInfo = registry.getPlugin(pluginId)
        if (pluginInfo == null) {
            PluginLog.w(TAG, "Plugin not found for uninstall: $pluginId")
            return false
        }

        // Update state to uninstalling
        registry.updateState(pluginId, PluginState.UNINSTALLING)

        try {
            // Cleanup namespace resources (FR-017)
            val namespace = pluginInfo.namespace
            val cleanedUp = namespace.cleanup(fileIO)
            if (!cleanedUp) {
                PluginLog.w(TAG, "Failed to cleanup namespace for plugin: $pluginId")
            }

            // Unregister from registry
            registry.unregister(pluginId)

            PluginLog.i(TAG, "Successfully uninstalled plugin: $pluginId")
            return true

        } catch (e: Exception) {
            PluginLog.e(TAG, "Failed to uninstall plugin: $pluginId", e)
            registry.updateState(pluginId, PluginState.FAILED)
            return false
        }
    }
}
