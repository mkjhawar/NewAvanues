package com.augmentalis.universal.thememanager

import com.augmentalis.avanues.avamagic.components.core.Theme
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

/**
 * Repository interface for theme persistence
 *
 * Supports multiple storage backends:
 * - LocalThemeRepository: File-based storage
 * - CloudThemeRepository: Cloud synchronization (future)
 * - InMemoryThemeRepository: Testing and development
 */
interface ThemeRepository {
    /**
     * Save the universal Avanues theme
     */
    suspend fun saveUniversalTheme(theme: Theme)

    /**
     * Load the universal Avanues theme
     * Returns null if no theme is saved
     */
    suspend fun loadUniversalTheme(): Theme?

    /**
     * Save a theme for a specific app
     */
    suspend fun saveAppTheme(appId: String, theme: Theme)

    /**
     * Load a theme for a specific app
     * Returns null if no theme is saved for this app
     */
    suspend fun loadAppTheme(appId: String): Theme?

    /**
     * Delete a theme for a specific app
     */
    suspend fun deleteAppTheme(appId: String)

    /**
     * Save an override configuration for a specific app
     */
    suspend fun saveAppOverride(appId: String, override: ThemeOverride)

    /**
     * Load an override configuration for a specific app
     * Returns null if no override is saved for this app
     */
    suspend fun loadAppOverride(appId: String): ThemeOverride?

    /**
     * Delete an override configuration for a specific app
     */
    suspend fun deleteAppOverride(appId: String)

    /**
     * Load all app themes
     * Returns a map of app ID to theme
     */
    suspend fun loadAllAppThemes(): Map<String, Theme>

    /**
     * Clear all themes (universal and app-specific)
     */
    suspend fun clearAll()
}

/**
 * Local file-based theme repository
 * Saves themes to Universal/Core/ThemeManager/themes/
 */
class LocalThemeRepository(
    private val themesDir: String = getDefaultThemesDirectory()
) : ThemeRepository {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    override suspend fun saveUniversalTheme(theme: Theme) {
        val file = getUniversalThemeFile()
        writeFile(file, json.encodeToString(theme))
    }

    override suspend fun loadUniversalTheme(): Theme? {
        val file = getUniversalThemeFile()
        return try {
            val content = readFile(file) ?: return null
            json.decodeFromString<Theme>(content)
        } catch (e: Exception) {
            println("Error loading universal theme: ${e.message}")
            null
        }
    }

    override suspend fun saveAppTheme(appId: String, theme: Theme) {
        val file = getAppThemeFile(appId)
        writeFile(file, json.encodeToString(theme))
    }

    override suspend fun loadAppTheme(appId: String): Theme? {
        val file = getAppThemeFile(appId)
        return try {
            val content = readFile(file) ?: return null
            json.decodeFromString<Theme>(content)
        } catch (e: Exception) {
            println("Error loading app theme for $appId: ${e.message}")
            null
        }
    }

    override suspend fun deleteAppTheme(appId: String) {
        val file = getAppThemeFile(appId)
        deleteFile(file)
    }

    override suspend fun saveAppOverride(appId: String, override: ThemeOverride) {
        val file = getAppOverrideFile(appId)
        writeFile(file, json.encodeToString(override))
    }

    override suspend fun loadAppOverride(appId: String): ThemeOverride? {
        val file = getAppOverrideFile(appId)
        return try {
            val content = readFile(file) ?: return null
            json.decodeFromString<ThemeOverride>(content)
        } catch (e: Exception) {
            println("Error loading app override for $appId: ${e.message}")
            null
        }
    }

    override suspend fun deleteAppOverride(appId: String) {
        val file = getAppOverrideFile(appId)
        deleteFile(file)
    }

    override suspend fun loadAllAppThemes(): Map<String, Theme> {
        val appThemesMap = mutableMapOf<String, Theme>()
        val appFiles = listAppThemeFiles()

        appFiles.forEach { fileName ->
            // Extract app ID from filename (e.g., "com.augmentalis.voiceos.json" -> "com.augmentalis.voiceos")
            val appId = fileName.removeSuffix(".json")
            loadAppTheme(appId)?.let { theme ->
                appThemesMap[appId] = theme
            }
        }

        return appThemesMap
    }

    override suspend fun clearAll() {
        deleteFile(getUniversalThemeFile())

        // Delete all app themes and overrides
        val appFiles = listAppThemeFiles()
        appFiles.forEach { fileName ->
            val appId = fileName.removeSuffix(".json")
            deleteAppTheme(appId)
            deleteAppOverride(appId)
        }
    }

    // ==================== File Path Helpers ====================

    private fun getUniversalThemeFile(): String {
        return "$themesDir/universal.json"
    }

    private fun getAppThemeFile(appId: String): String {
        // Sanitize app ID for filename (replace dots with underscores)
        val sanitizedId = appId.replace(".", "_")
        return "$themesDir/apps/$sanitizedId.json"
    }

    private fun getAppOverrideFile(appId: String): String {
        val sanitizedId = appId.replace(".", "_")
        return "$themesDir/apps/$sanitizedId.override.json"
    }

    // ==================== Platform-Specific File Operations ====================
    // These functions need to be implemented using expect/actual for each platform

    /**
     * Write content to file
     */
    private fun writeFile(path: String, content: String) {
        // Platform-specific implementation
        // For JVM/Android: Use java.io.File
        // For iOS: Use NSFileManager
        // For JS: Use Node.js fs module or browser storage

        // Simplified implementation for demonstration
        try {
            val file = java.io.File(path)
            file.parentFile?.mkdirs()
            file.writeText(content)
        } catch (e: Exception) {
            println("Error writing file $path: ${e.message}")
        }
    }

    /**
     * Read content from file
     * Returns null if file doesn't exist
     */
    private fun readFile(path: String): String? {
        return try {
            val file = java.io.File(path)
            if (file.exists()) file.readText() else null
        } catch (e: Exception) {
            println("Error reading file $path: ${e.message}")
            null
        }
    }

    /**
     * Delete file
     */
    private fun deleteFile(path: String) {
        try {
            val file = java.io.File(path)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            println("Error deleting file $path: ${e.message}")
        }
    }

    /**
     * List all app theme files
     * Returns list of filenames (not full paths)
     */
    private fun listAppThemeFiles(): List<String> {
        return try {
            val appsDir = java.io.File("$themesDir/apps")
            if (!appsDir.exists()) return emptyList()

            appsDir.listFiles()
                ?.filter { it.isFile && it.name.endsWith(".json") && !it.name.endsWith(".override.json") }
                ?.map { it.name }
                ?: emptyList()
        } catch (e: Exception) {
            println("Error listing app theme files: ${e.message}")
            emptyList()
        }
    }

    companion object {
        /**
         * Get the default themes directory based on platform
         */
        fun getDefaultThemesDirectory(): String {
            // This should be platform-specific
            // For development, we use a relative path
            return "Universal/Core/ThemeManager/themes"
        }
    }
}

/**
 * Cloud-based theme repository
 * Placeholder for future cloud synchronization feature
 *
 * Potential cloud providers:
 * - Firebase Firestore
 * - AWS DynamoDB
 * - Azure Cosmos DB
 * - Custom REST API
 */
class CloudThemeRepository(
    private val cloudProvider: CloudProvider
) : ThemeRepository {

    override suspend fun saveUniversalTheme(theme: Theme) {
        // TODO: Implement cloud save
        println("CloudThemeRepository: saveUniversalTheme not yet implemented")
    }

    override suspend fun loadUniversalTheme(): Theme? {
        // TODO: Implement cloud load
        println("CloudThemeRepository: loadUniversalTheme not yet implemented")
        return null
    }

    override suspend fun saveAppTheme(appId: String, theme: Theme) {
        // TODO: Implement cloud save
        println("CloudThemeRepository: saveAppTheme not yet implemented")
    }

    override suspend fun loadAppTheme(appId: String): Theme? {
        // TODO: Implement cloud load
        println("CloudThemeRepository: loadAppTheme not yet implemented")
        return null
    }

    override suspend fun deleteAppTheme(appId: String) {
        // TODO: Implement cloud delete
        println("CloudThemeRepository: deleteAppTheme not yet implemented")
    }

    override suspend fun saveAppOverride(appId: String, override: ThemeOverride) {
        // TODO: Implement cloud save
        println("CloudThemeRepository: saveAppOverride not yet implemented")
    }

    override suspend fun loadAppOverride(appId: String): ThemeOverride? {
        // TODO: Implement cloud load
        println("CloudThemeRepository: loadAppOverride not yet implemented")
        return null
    }

    override suspend fun deleteAppOverride(appId: String) {
        // TODO: Implement cloud delete
        println("CloudThemeRepository: deleteAppOverride not yet implemented")
    }

    override suspend fun loadAllAppThemes(): Map<String, Theme> {
        // TODO: Implement cloud load all
        println("CloudThemeRepository: loadAllAppThemes not yet implemented")
        return emptyMap()
    }

    override suspend fun clearAll() {
        // TODO: Implement cloud clear all
        println("CloudThemeRepository: clearAll not yet implemented")
    }
}

/**
 * Cloud provider interface
 * Define the contract for cloud storage providers
 */
interface CloudProvider {
    suspend fun save(key: String, data: String)
    suspend fun load(key: String): String?
    suspend fun delete(key: String)
    suspend fun list(prefix: String): List<String>
}

/**
 * In-memory theme repository for testing
 */
class InMemoryThemeRepository : ThemeRepository {
    private var universalTheme: Theme? = null
    private val appThemes = mutableMapOf<String, Theme>()
    private val appOverrides = mutableMapOf<String, ThemeOverride>()

    override suspend fun saveUniversalTheme(theme: Theme) {
        universalTheme = theme
    }

    override suspend fun loadUniversalTheme(): Theme? = universalTheme

    override suspend fun saveAppTheme(appId: String, theme: Theme) {
        appThemes[appId] = theme
    }

    override suspend fun loadAppTheme(appId: String): Theme? = appThemes[appId]

    override suspend fun deleteAppTheme(appId: String) {
        appThemes.remove(appId)
    }

    override suspend fun saveAppOverride(appId: String, override: ThemeOverride) {
        appOverrides[appId] = override
    }

    override suspend fun loadAppOverride(appId: String): ThemeOverride? = appOverrides[appId]

    override suspend fun deleteAppOverride(appId: String) {
        appOverrides.remove(appId)
    }

    override suspend fun loadAllAppThemes(): Map<String, Theme> = appThemes.toMap()

    override suspend fun clearAll() {
        universalTheme = null
        appThemes.clear()
        appOverrides.clear()
    }
}
