/**
 * PluginManifestReader.kt - Reads plugin manifests from various formats
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Reads and parses plugin manifest files in AVU and JSON formats.
 * Also provides serialization methods for writing manifests.
 */
package com.augmentalis.magiccode.plugins.discovery

import com.augmentalis.magiccode.plugins.core.AvuManifestParser
import com.augmentalis.magiccode.plugins.core.PluginManifest
import com.augmentalis.magiccode.plugins.platform.FileIO
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Reads plugin manifests from various formats.
 *
 * Supports reading and writing plugin manifests in multiple formats:
 * - **AVU format** (Avanues Universal format) - Native, compact format
 * - **JSON format** - Standard JSON for compatibility and debugging
 *
 * ## Format Detection
 * The reader automatically detects the format based on:
 * 1. File extension (.avu or .json)
 * 2. Content inspection (AVU codes vs JSON structure)
 *
 * ## Usage Example
 * ```kotlin
 * val fileIO = FileIO()  // Platform-specific implementation
 * val reader = PluginManifestReader(fileIO)
 *
 * // Read from file (auto-detects format)
 * val descriptorResult = reader.readManifest("/plugins/my-plugin/plugin.avu")
 * descriptorResult.onSuccess { descriptor ->
 *     println("Loaded: ${descriptor.pluginId} v${descriptor.version}")
 * }
 *
 * // Read from AVU bytes
 * val avuBytes = fileIO.readFileAsBytes("/path/to/plugin.avu")
 * val fromAvu = reader.readFromAvu(avuBytes)
 *
 * // Read from JSON string
 * val jsonContent = """{"pluginId": "com.example.plugin", ...}"""
 * val fromJson = reader.readFromJson(jsonContent)
 *
 * // Write to AVU format
 * val avuOutput = reader.writeToAvu(descriptor)
 *
 * // Write to JSON format
 * val jsonOutput = reader.writeToJson(descriptor)
 * ```
 *
 * ## Thread Safety
 * All methods are thread-safe and can be called concurrently.
 *
 * @param fileIO Platform-specific file I/O operations
 * @since 1.0.0
 * @see PluginDescriptor
 * @see AvuManifestParser
 */
class PluginManifestReader(
    private val fileIO: FileIO
) {
    /**
     * JSON serializer with lenient parsing for compatibility.
     */
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
        encodeDefaults = true
    }

    /**
     * Read a manifest from a file path.
     *
     * Automatically detects the format based on file extension and content.
     *
     * @param path Absolute path to the manifest file
     * @return Result containing PluginDescriptor or error
     */
    fun readManifest(path: String): Result<PluginDescriptor> {
        return try {
            if (!fileIO.fileExists(path)) {
                return Result.failure(ManifestNotFoundException("Manifest not found at: $path"))
            }

            val content = fileIO.readFileAsString(path)
            if (content.isBlank()) {
                return Result.failure(InvalidManifestException("Manifest file is empty: $path"))
            }

            // Detect format and parse
            val descriptor = when {
                path.endsWith(EXTENSION_AVU) || isAvuFormat(content) -> parseAvuContent(content)
                path.endsWith(EXTENSION_JSON) || isJsonFormat(content) -> parseJsonContent(content)
                else -> {
                    // Try AVU first, then JSON
                    tryParseAvu(content) ?: tryParseJson(content)
                    ?: throw InvalidManifestException("Unable to parse manifest format: $path")
                }
            }

            Result.success(descriptor)
        } catch (e: ManifestNotFoundException) {
            Result.failure(e)
        } catch (e: InvalidManifestException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(InvalidManifestException("Failed to read manifest: ${e.message}", e))
        }
    }

    /**
     * Read a manifest from AVU bytes.
     *
     * @param bytes AVU format content as byte array
     * @return Result containing PluginDescriptor or error
     */
    fun readFromAvu(bytes: ByteArray): Result<PluginDescriptor> {
        return try {
            val content = bytes.decodeToString()
            if (content.isBlank()) {
                return Result.failure(InvalidManifestException("AVU content is empty"))
            }

            val descriptor = parseAvuContent(content)
            Result.success(descriptor)
        } catch (e: Exception) {
            Result.failure(InvalidManifestException("Failed to parse AVU content: ${e.message}", e))
        }
    }

    /**
     * Read a manifest from JSON string.
     *
     * @param jsonString JSON format content
     * @return Result containing PluginDescriptor or error
     */
    fun readFromJson(jsonString: String): Result<PluginDescriptor> {
        return try {
            if (jsonString.isBlank()) {
                return Result.failure(InvalidManifestException("JSON content is empty"))
            }

            val descriptor = parseJsonContent(jsonString)
            Result.success(descriptor)
        } catch (e: Exception) {
            Result.failure(InvalidManifestException("Failed to parse JSON content: ${e.message}", e))
        }
    }

    /**
     * Write a manifest to AVU format.
     *
     * @param descriptor Plugin descriptor to write
     * @return AVU format content as byte array
     */
    fun writeToAvu(descriptor: PluginDescriptor): ByteArray {
        val manifest = descriptorToManifest(descriptor)
        val avuContent = AvuManifestParser.toAvu(manifest)
        return avuContent.encodeToByteArray()
    }

    /**
     * Write a manifest to AVU format string.
     *
     * @param descriptor Plugin descriptor to write
     * @return AVU format content as string
     */
    fun writeToAvuString(descriptor: PluginDescriptor): String {
        val manifest = descriptorToManifest(descriptor)
        return AvuManifestParser.toAvu(manifest)
    }

    /**
     * Write a manifest to JSON format.
     *
     * @param descriptor Plugin descriptor to write
     * @return JSON format content as string
     */
    fun writeToJson(descriptor: PluginDescriptor): String {
        val jsonDescriptor = JsonPluginDescriptor(
            pluginId = descriptor.pluginId,
            name = descriptor.name,
            version = descriptor.version,
            capabilities = descriptor.capabilities.toList(),
            description = descriptor.description,
            author = descriptor.author,
            entrypoint = descriptor.entrypoint,
            metadata = descriptor.metadata,
            dependencies = descriptor.dependencies.map { dep ->
                JsonPluginDependency(
                    pluginId = dep.pluginId,
                    versionConstraint = dep.versionConstraint,
                    optional = dep.optional
                )
            }
        )
        return json.encodeToString(jsonDescriptor)
    }

    /**
     * Write a manifest to JSON format as bytes.
     *
     * @param descriptor Plugin descriptor to write
     * @return JSON format content as byte array
     */
    fun writeToJsonBytes(descriptor: PluginDescriptor): ByteArray {
        return writeToJson(descriptor).encodeToByteArray()
    }

    /**
     * Validate a manifest file.
     *
     * Checks if the manifest is valid without fully parsing it.
     *
     * @param path Path to the manifest file
     * @return ValidationResult with any validation errors
     */
    fun validateManifest(path: String): ValidationResult {
        val errors = mutableListOf<String>()

        if (!fileIO.fileExists(path)) {
            return ValidationResult(false, listOf("Manifest file not found: $path"))
        }

        val content = try {
            fileIO.readFileAsString(path)
        } catch (e: Exception) {
            return ValidationResult(false, listOf("Cannot read manifest: ${e.message}"))
        }

        if (content.isBlank()) {
            return ValidationResult(false, listOf("Manifest file is empty"))
        }

        // Try to parse and collect errors
        val parseResult = readManifest(path)
        if (parseResult.isFailure) {
            errors.add("Parse error: ${parseResult.exceptionOrNull()?.message}")
            return ValidationResult(false, errors)
        }

        val descriptor = parseResult.getOrThrow()

        // Validate required fields
        if (descriptor.pluginId.isBlank()) {
            errors.add("Missing required field: pluginId")
        } else if (!PLUGIN_ID_PATTERN.matches(descriptor.pluginId)) {
            errors.add("Invalid pluginId format: ${descriptor.pluginId}")
        }

        if (descriptor.name.isBlank()) {
            errors.add("Missing required field: name")
        }

        if (descriptor.version.isBlank()) {
            errors.add("Missing required field: version")
        } else if (!isValidSemver(descriptor.version)) {
            errors.add("Invalid version format: ${descriptor.version}")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /**
     * Detect manifest format from content.
     *
     * @param content Manifest content
     * @return Detected format or UNKNOWN
     */
    fun detectFormat(content: String): ManifestFormat {
        return when {
            isAvuFormat(content) -> ManifestFormat.AVU
            isJsonFormat(content) -> ManifestFormat.JSON
            else -> ManifestFormat.UNKNOWN
        }
    }

    /**
     * Convert PluginManifest to PluginDescriptor.
     *
     * @param manifest Source manifest
     * @param source Plugin source (defaults to Builtin)
     * @return PluginDescriptor
     */
    fun manifestToDescriptor(
        manifest: PluginManifest,
        source: PluginSource = PluginSource.Builtin
    ): PluginDescriptor {
        return PluginDescriptor(
            pluginId = manifest.id,
            name = manifest.name,
            version = manifest.version,
            capabilities = manifest.capabilities.toSet(),
            source = source,
            metadata = buildMap {
                put("author", manifest.author)
                manifest.license?.let { put("license", it) }
                manifest.homepage?.let { put("homepage", it) }
                put("source", manifest.source)
                put("verificationLevel", manifest.verificationLevel)
            }.filterValues { it.isNotEmpty() },
            description = manifest.description,
            author = manifest.author,
            entrypoint = manifest.entrypoint,
            dependencies = manifest.dependencies.map { dep ->
                PluginDependencyDescriptor(
                    pluginId = dep.pluginId,
                    versionConstraint = dep.version,
                    optional = dep.optional
                )
            }
        )
    }

    /**
     * Convert PluginDescriptor to PluginManifest.
     *
     * @param descriptor Source descriptor
     * @return PluginManifest
     */
    fun descriptorToManifest(descriptor: PluginDescriptor): PluginManifest {
        return PluginManifest(
            id = descriptor.pluginId,
            name = descriptor.name,
            version = descriptor.version,
            author = descriptor.author ?: descriptor.metadata["author"] ?: "",
            description = descriptor.description,
            entrypoint = descriptor.entrypoint ?: "",
            capabilities = descriptor.capabilities.toList(),
            dependencies = descriptor.dependencies.map { dep ->
                com.augmentalis.magiccode.plugins.core.PluginDependency(
                    pluginId = dep.pluginId,
                    version = dep.versionConstraint,
                    optional = dep.optional
                )
            },
            source = descriptor.metadata["source"] ?: "THIRD_PARTY",
            verificationLevel = descriptor.metadata["verificationLevel"] ?: "UNVERIFIED",
            homepage = descriptor.metadata["homepage"],
            license = descriptor.metadata["license"]
        )
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    /**
     * Check if content is in AVU format.
     */
    private fun isAvuFormat(content: String): Boolean {
        return content.lines().any { line ->
            val trimmed = line.trim()
            trimmed.startsWith("PLG:") ||
                trimmed.contains("Avanues Universal Plugin Format") ||
                trimmed.startsWith("# AVU") ||
                AVU_CODES.any { code -> trimmed.startsWith("$code:") }
        }
    }

    /**
     * Check if content is in JSON format.
     */
    private fun isJsonFormat(content: String): Boolean {
        val trimmed = content.trim()
        return trimmed.startsWith("{") && trimmed.endsWith("}")
    }

    /**
     * Parse AVU format content.
     */
    private fun parseAvuContent(content: String): PluginDescriptor {
        val manifest = AvuManifestParser.parse(content)
            ?: throw InvalidManifestException("Failed to parse AVU manifest")
        return manifestToDescriptor(manifest)
    }

    /**
     * Parse JSON format content.
     */
    private fun parseJsonContent(content: String): PluginDescriptor {
        val jsonDescriptor = json.decodeFromString<JsonPluginDescriptor>(content)
        return PluginDescriptor(
            pluginId = jsonDescriptor.pluginId,
            name = jsonDescriptor.name,
            version = jsonDescriptor.version,
            capabilities = jsonDescriptor.capabilities.toSet(),
            source = PluginSource.Builtin,  // Will be updated by caller
            metadata = jsonDescriptor.metadata,
            description = jsonDescriptor.description,
            author = jsonDescriptor.author,
            entrypoint = jsonDescriptor.entrypoint,
            dependencies = jsonDescriptor.dependencies.map { dep ->
                PluginDependencyDescriptor(
                    pluginId = dep.pluginId,
                    versionConstraint = dep.versionConstraint,
                    optional = dep.optional
                )
            }
        )
    }

    /**
     * Try to parse as AVU, return null on failure.
     */
    private fun tryParseAvu(content: String): PluginDescriptor? {
        return try {
            if (AvuManifestParser.isAvuManifest(content)) {
                parseAvuContent(content)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Try to parse as JSON, return null on failure.
     */
    private fun tryParseJson(content: String): PluginDescriptor? {
        return try {
            parseJsonContent(content)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Check if version string is valid semver.
     */
    private fun isValidSemver(version: String): Boolean {
        // Basic semver pattern: MAJOR.MINOR.PATCH with optional pre-release
        return SEMVER_PATTERN.matches(version)
    }

    companion object {
        /** AVU file extension. */
        const val EXTENSION_AVU = ".avu"

        /** JSON file extension. */
        const val EXTENSION_JSON = ".json"

        /** AVU protocol codes. */
        private val AVU_CODES = listOf("PLG", "DSC", "AUT", "PCP", "MOD", "DEP", "PRM", "PLT", "AST", "CFG", "HKS")

        /** Plugin ID validation pattern. */
        private val PLUGIN_ID_PATTERN = Regex("^[a-z][a-z0-9-]*(\\.[a-z][a-z0-9-]*)+$")

        /** Basic semver validation pattern. */
        private val SEMVER_PATTERN = Regex("^\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9.]+)?(\\+[a-zA-Z0-9.]+)?$")

        /**
         * Create a reader with a new FileIO instance.
         *
         * @param fileIO FileIO instance
         * @return PluginManifestReader
         */
        fun create(fileIO: FileIO): PluginManifestReader {
            return PluginManifestReader(fileIO)
        }
    }
}

/**
 * Manifest format enumeration.
 */
enum class ManifestFormat {
    /** Avanues Universal format. */
    AVU,

    /** JSON format. */
    JSON,

    /** Unknown format. */
    UNKNOWN
}

/**
 * Result of manifest validation.
 *
 * @property valid Whether the manifest is valid
 * @property errors List of validation errors
 */
data class ValidationResult(
    val valid: Boolean,
    val errors: List<String> = emptyList()
) {
    /**
     * Check if validation passed.
     */
    val isValid: Boolean get() = valid && errors.isEmpty()
}

/**
 * Exception thrown when manifest file is not found.
 */
class ManifestNotFoundException(message: String) : Exception(message)

/**
 * Exception thrown when manifest content is invalid.
 */
class InvalidManifestException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * JSON representation of plugin descriptor for serialization.
 */
@Serializable
internal data class JsonPluginDescriptor(
    val pluginId: String,
    val name: String,
    val version: String,
    val capabilities: List<String> = emptyList(),
    val description: String? = null,
    val author: String? = null,
    val entrypoint: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val dependencies: List<JsonPluginDependency> = emptyList()
)

/**
 * JSON representation of plugin dependency for serialization.
 */
@Serializable
internal data class JsonPluginDependency(
    val pluginId: String,
    val versionConstraint: String = "*",
    val optional: Boolean = false
)
