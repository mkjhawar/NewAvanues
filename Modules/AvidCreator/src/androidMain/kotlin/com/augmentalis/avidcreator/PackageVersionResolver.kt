/**
 * PackageVersionResolver.kt - Resolve app version information
 * Path: libraries/AvidCreator/src/main/java/com/augmentalis/uuidcreator/thirdparty/PackageVersionResolver.kt
 *
 * Author: Manoj Jhawar
 * Created: 2025-10-08
 *
 * Resolves Android app version information for UUID namespace isolation
 */

package com.augmentalis.avidcreator.thirdparty

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Package Version Resolver
 *
 * Resolves version information for installed Android applications.
 * Used to create version-scoped UUIDs for third-party apps.
 *
 * ## Version Formats
 *
 * - **Version Name**: Human-readable (e.g., "12.0.0", "1.2.3-beta")
 * - **Version Code**: Numeric build number (e.g., 120000)
 * - **Normalized**: Standardized format for UUIDs (e.g., "12.0.0")
 *
 * ## UUID Version Scoping
 *
 * Different app versions get different UUID namespaces:
 * ```
 * v12.0.0 → com.app.v12.0.0.button-abc123
 * v12.0.1 → com.app.v12.0.1.button-xyz789  // Different UUID!
 * ```
 *
 * This ensures UUIDs remain stable within an app version, but change
 * when the app updates (preventing voice command mismatches).
 *
 * ## Usage Example
 *
 * ```kotlin
 * val resolver = PackageVersionResolver(context)
 *
 * // Get version string for UUID
 * val version = resolver.getVersionString("com.instagram.android")
 * // Returns: "12.0.0"
 *
 * // Get detailed info
 * val info = resolver.getVersionInfo("com.instagram.android")
 * // Returns: VersionInfo(name="12.0.0", code=120000, ...)
 * ```
 *
 * @property context Application context
 * @property packageManager PackageManager instance
 *
 * @since 1.0.0
 */
class PackageVersionResolver(
    private val context: Context
) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * In-memory cache for version info
     *
     * Avoids repeated PackageManager queries
     */
    private val versionCache = mutableMapOf<String, VersionInfo>()

    /**
     * Get version string for UUID generation
     *
     * Returns normalized version string suitable for UUID namespace.
     * Format: "{major}.{minor}.{patch}"
     *
     * ## Examples
     *
     * - "12.0.0" → "12.0.0"
     * - "1.2.3-beta" → "1.2.3"
     * - "v4.5.6" → "4.5.6"
     *
     * @param packageName Package name
     * @return Normalized version string
     * @throws PackageManager.NameNotFoundException if package not found
     */
    fun getVersionString(packageName: String): String {
        return getVersionInfo(packageName).normalizedVersion
    }

    /**
     * Get detailed version information
     *
     * Retrieves comprehensive version info from PackageManager.
     *
     * @param packageName Package name
     * @return VersionInfo with all version details
     * @throws PackageManager.NameNotFoundException if package not found
     */
    fun getVersionInfo(packageName: String): VersionInfo {
        // Check cache
        versionCache[packageName]?.let { return it }

        // Query PackageManager
        val info = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            throw PackageManager.NameNotFoundException("Package not found: $packageName")
        }

        // Extract version info
        val versionName = info.versionName ?: "unknown"
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }

        val versionInfo = VersionInfo(
            packageName = packageName,
            versionName = versionName,
            versionCode = versionCode,
            normalizedVersion = normalizeVersionString(versionName)
        )

        // Cache result
        versionCache[packageName] = versionInfo

        return versionInfo
    }

    /**
     * Normalize version string
     *
     * Converts various version formats to standardized format:
     * - Remove prefixes: "v1.2.3" → "1.2.3"
     * - Remove suffixes: "1.2.3-beta" → "1.2.3"
     * - Remove build metadata: "1.2.3+build" → "1.2.3"
     * - Ensure 3 parts: "1.2" → "1.2.0"
     *
     * @param versionName Raw version name
     * @return Normalized version string
     */
    private fun normalizeVersionString(versionName: String): String {
        var normalized = versionName

        // Remove "v" prefix
        normalized = normalized.removePrefix("v").removePrefix("V")

        // Remove suffixes (-, +, anything after space)
        normalized = normalized.split('-', '+', ' ')[0]

        // Split into parts
        val parts = normalized.split('.')
            .filter { it.isNotBlank() }
            .map { it.filter { char -> char.isDigit() } }
            .filter { it.isNotBlank() }

        // Ensure at least 3 parts (major.minor.patch)
        val major = parts.getOrNull(0) ?: "0"
        val minor = parts.getOrNull(1) ?: "0"
        val patch = parts.getOrNull(2) ?: "0"

        return "$major.$minor.$patch"
    }

    /**
     * Check if package is installed
     *
     * @param packageName Package name
     * @return true if package is installed
     */
    fun isPackageInstalled(packageName: String): Boolean {
        return try {
            getVersionInfo(packageName)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Get all installed packages
     *
     * Returns list of all installed package names (excluding system apps).
     *
     * @param includeSystemApps Include system apps (default: false)
     * @return List of package names
     */
    fun getInstalledPackages(includeSystemApps: Boolean = false): List<String> {
        val packages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(0)
        }

        return packages
            .filter { pkg ->
                includeSystemApps || ((pkg.applicationInfo?.flags ?: 0) and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0
            }
            .map { it.packageName }
    }

    /**
     * Compare versions
     *
     * @param version1 First version
     * @param version2 Second version
     * @return -1 if version1 < version2, 0 if equal, 1 if version1 > version2
     */
    fun compareVersions(version1: String, version2: String): Int {
        val v1 = normalizeVersionString(version1).split('.').map { it.toIntOrNull() ?: 0 }
        val v2 = normalizeVersionString(version2).split('.').map { it.toIntOrNull() ?: 0 }

        for (i in 0 until maxOf(v1.size, v2.size)) {
            val part1 = v1.getOrNull(i) ?: 0
            val part2 = v2.getOrNull(i) ?: 0

            when {
                part1 < part2 -> return -1
                part1 > part2 -> return 1
            }
        }

        return 0
    }

    /**
     * Clear version cache
     *
     * Use when app updates detected to refresh version info.
     */
    fun clearCache() {
        versionCache.clear()
    }

    /**
     * Clear version cache for specific package
     *
     * @param packageName Package to clear from cache
     */
    fun clearCacheForPackage(packageName: String) {
        versionCache.remove(packageName)
    }
}

/**
 * Version Information
 *
 * Comprehensive version data for installed package.
 *
 * @property packageName Package name
 * @property versionName Version name from manifest (e.g., "12.0.0")
 * @property versionCode Numeric version code
 * @property normalizedVersion Normalized version for UUIDs
 */
data class VersionInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val normalizedVersion: String
) {
    /**
     * Short version string
     *
     * Returns major.minor only (e.g., "12.0")
     */
    val shortVersion: String
        get() {
            val parts = normalizedVersion.split('.')
            return "${parts.getOrNull(0) ?: "0"}.${parts.getOrNull(1) ?: "0"}"
        }

    /**
     * Format for display
     *
     * Returns human-readable version string
     */
    override fun toString(): String {
        return "$versionName (code: $versionCode)"
    }
}
