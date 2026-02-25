package com.augmentalis.avanueui.argscanner

/**
 * ARG File Scanner
 *
 * Discovers and scans Avanue Registry (ARG) files from the filesystem.
 *
 * ## Discovery Locations
 * - Android: `/data/data/{package}/files/arg/`
 * - Android: `/sdcard/Avanue/registry/`
 * - iOS: `Documents/Avanue/registry/`
 * - macOS: `~/Library/Application Support/Avanue/registry/`
 * - Windows: `%APPDATA%/Avanue/registry/`
 *
 * ## Usage
 * ```kotlin
 * val scanner = ARGScanner(parser, registry)
 * val results = scanner.scanAll()
 * println("Discovered ${results.success.size} apps")
 * ```
 *
 * @since 1.0.0
 */
class ARGScanner(
    private val parser: ARGParser,
    private val registry: ARGRegistry
) {

    /**
     * Scan all discovery locations for ARG files
     *
     * @return Scan results with successful and failed loads
     */
    suspend fun scanAll(): ScanResults {
        val locations = getDiscoveryLocations()
        val results = ScanResults()

        locations.forEach { location ->
            try {
                val files = findARGFiles(location)
                files.forEach { file ->
                    try {
                        val content = readFile(file)
                        val argFile = parser.parse(content)

                        // Validate before registering
                        val errors = parser.validate(argFile)
                        if (errors.isEmpty()) {
                            registry.register(argFile)
                            results.success.add(ScanResult.Success(file, argFile))
                        } else {
                            results.failed.add(ScanResult.Failed(file, errors))
                        }
                    } catch (e: ARGParseException) {
                        results.failed.add(ScanResult.Failed(file, listOf(
                            ValidationError.InvalidFormat("file", e.message ?: "Parse error")
                        )))
                    }
                }
            } catch (e: Exception) {
                // Location doesn't exist or isn't accessible
                // This is normal, just skip
            }
        }

        return results
    }

    /**
     * Scan a specific file or directory
     *
     * @param path Path to ARG file or directory containing ARG files
     * @return Scan result
     */
    suspend fun scan(path: String): ScanResults {
        val results = ScanResults()

        try {
            if (isDirectory(path)) {
                val files = findARGFiles(path)
                files.forEach { file ->
                    try {
                        val content = readFile(file)
                        val argFile = parser.parse(content)

                        val errors = parser.validate(argFile)
                        if (errors.isEmpty()) {
                            registry.register(argFile)
                            results.success.add(ScanResult.Success(file, argFile))
                        } else {
                            results.failed.add(ScanResult.Failed(file, errors))
                        }
                    } catch (e: ARGParseException) {
                        results.failed.add(ScanResult.Failed(file, listOf(
                            ValidationError.InvalidFormat("file", e.message ?: "Parse error")
                        )))
                    }
                }
            } else {
                // Single file
                val content = readFile(path)
                val argFile = parser.parse(content)

                val errors = parser.validate(argFile)
                if (errors.isEmpty()) {
                    registry.register(argFile)
                    results.success.add(ScanResult.Success(path, argFile))
                } else {
                    results.failed.add(ScanResult.Failed(path, errors))
                }
            }
        } catch (e: Exception) {
            results.failed.add(ScanResult.Failed(path, listOf(
                ValidationError.InvalidFormat("file", e.message ?: "Scan error")
            )))
        }

        return results
    }

    /**
     * Watch a directory for new ARG files
     *
     * @param path Directory to watch
     * @param callback Called when new ARG file is detected
     */
    suspend fun watch(path: String, callback: (ARGFile) -> Unit) {
        // TODO: Implement file system watcher
        // Platform-specific implementation needed
    }

    /**
     * Get platform-specific discovery locations
     */
    private fun getDiscoveryLocations(): List<String> = buildList {
        // Add platform-specific paths
        // These will be implemented in platform-specific expect/actual
        add(getPrimaryRegistryPath())
        add(getSecondaryRegistryPath())
        add(getUserRegistryPath())
    }

    /**
     * Find all .arg files in a directory
     */
    private fun findARGFiles(directory: String): List<String> {
        // Platform-specific file discovery
        // Will be implemented as expect/actual
        return listFiles(directory).filter { it.endsWith(".arg") || it.endsWith(".arg.json") }
    }

    // Platform-specific functions (expect/actual)
    private fun getPrimaryRegistryPath(): String = getPlatformPrimaryPath()
    private fun getSecondaryRegistryPath(): String = getPlatformSecondaryPath()
    private fun getUserRegistryPath(): String = getPlatformUserPath()
    private fun readFile(path: String): String = platformReadFile(path)
    private fun isDirectory(path: String): Boolean = platformIsDirectory(path)
    private fun listFiles(directory: String): List<String> = platformListFiles(directory)
}

/**
 * Scan results container
 */
data class ScanResults(
    val success: MutableList<ScanResult.Success> = mutableListOf(),
    val failed: MutableList<ScanResult.Failed> = mutableListOf()
) {
    val totalScanned: Int get() = success.size + failed.size
    val successRate: Float get() = if (totalScanned > 0) success.size.toFloat() / totalScanned else 0f
}

/**
 * Individual scan result
 */
sealed class ScanResult {
    data class Success(val path: String, val argFile: ARGFile) : ScanResult()
    data class Failed(val path: String, val errors: List<ValidationError>) : ScanResult()
}

// Platform-specific expect functions
expect fun getPlatformPrimaryPath(): String
expect fun getPlatformSecondaryPath(): String
expect fun getPlatformUserPath(): String
expect fun platformReadFile(path: String): String
expect fun platformIsDirectory(path: String): Boolean
expect fun platformListFiles(directory: String): List<String>
