/**
 * ThirdPartyUuidGenerator.kt - Generate stable UUIDs for third-party apps
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/thirdparty/ThirdPartyUuidGenerator.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Generates deterministic UUIDs for third-party Android apps via accessibility scanning
 */

package com.augmentalis.uuidcreator.thirdparty

import android.content.Context
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.uuidcreator.core.UUIDGenerator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Third-Party UUID Generator
 *
 * Generates stable, deterministic UUIDs for third-party Android applications
 * without requiring developer integration. Uses accessibility service to scan
 * UI elements and generate UUIDs based on stable fingerprints.
 *
 * ## UUID Format
 *
 * ```
 * {packageName}.v{version}.{type}-{hash}
 * ```
 *
 * **Example:**
 * ```
 * com.instagram.android.v12.0.0.button-a7f3e2c1d4b5
 * ```
 *
 * ## Components
 *
 * - **Package Name**: App identifier (e.g., "com.instagram.android")
 * - **Version**: App version (e.g., "v12.0.0")
 * - **Type**: Element type (button, text, input, etc.)
 * - **Hash**: 12-character deterministic hash from fingerprint
 *
 * ## Key Features
 *
 * 1. **Deterministic**: Same UI element → same UUID (within version)
 * 2. **Version Isolation**: Different app versions → different UUIDs
 * 3. **Stable Across Sessions**: UUIDs persist across app restarts
 * 4. **No Developer Integration**: Works with any Android app
 * 5. **Voice Command Compatible**: Enables universal voice control
 *
 * ## Usage Example
 *
 * ```kotlin
 * val generator = ThirdPartyUuidGenerator(context)
 *
 * // Generate UUID for accessibility node
 * val uuid = generator.generateUuid(
 *     node = accessibilityNodeInfo,
 *     packageName = "com.instagram.android"
 * )
 *
 * // Register with UUIDCreator
 * val element = UUIDElement(
 *     uuid = uuid,
 *     name = node.text?.toString(),
 *     type = fingerprint.getElementType(),
 *     metadata = UUIDMetadata(
 *         thirdPartyApp = true,
 *         packageName = "com.instagram.android"
 *     )
 * )
 * ```
 *
 * ## Thread Safety
 *
 * All operations are thread-safe using mutex locks for cache access.
 *
 * @property context Application context for PackageManager access
 * @property cache In-memory cache for fast UUID lookups
 * @property versionResolver Resolves app version strings
 *
 * @since 1.0.0
 */
class ThirdPartyUuidGenerator(
    private val context: Context
) {

    /**
     * In-memory cache for generated UUIDs
     *
     * Maps node fingerprint hash → generated UUID
     * Avoids regenerating UUIDs for same nodes
     */
    private val cache = ThirdPartyUuidCache()

    /**
     * Package version resolver
     */
    private val versionResolver = PackageVersionResolver(context)

    /**
     * Mutex for thread-safe cache access
     */
    private val cacheMutex = Mutex()

    /**
     * Generate UUID for accessibility node
     *
     * Creates deterministic UUID based on node fingerprint. The UUID will be
     * stable across app sessions (as long as app version unchanged).
     *
     * ## Process
     *
     * 1. Extract accessibility fingerprint from node
     * 2. Check cache for existing UUID
     * 3. If not cached, generate new UUID from fingerprint hash
     * 4. Cache and return UUID
     *
     * @param node AccessibilityNodeInfo to generate UUID for
     * @param packageName App package name (if null, extracted from node)
     * @return Generated UUID string
     */
    suspend fun generateUuid(
        node: AccessibilityNodeInfo,
        packageName: String? = null
    ): String {
        val pkg = packageName ?: node.packageName?.toString()
            ?: throw IllegalArgumentException("Cannot determine package name")

        // Get app version
        val version = versionResolver.getVersionString(pkg)

        // Extract fingerprint
        val fingerprint = AccessibilityFingerprint.fromNode(
            node = node,
            packageName = pkg,
            appVersion = version
        )

        // Check cache
        cacheMutex.withLock {
            cache.get(fingerprint)?.let { return it }
        }

        // Generate new UUID
        val uuid = formatUuid(fingerprint)

        // Cache result
        cacheMutex.withLock {
            cache.put(fingerprint, uuid)
        }

        return uuid
    }

    /**
     * Generate UUID from fingerprint (non-suspending)
     *
     * Synchronous version for contexts where coroutines not available.
     * Does not use cache - always generates fresh UUID.
     *
     * @param fingerprint Accessibility fingerprint
     * @return Generated UUID string
     */
    fun generateUuidFromFingerprint(fingerprint: AccessibilityFingerprint): String {
        return formatUuid(fingerprint)
    }

    /**
     * Batch generate UUIDs for node tree
     *
     * Recursively scans accessibility tree and generates UUIDs for all nodes.
     * Returns map of node → UUID.
     *
     * @param rootNode Root of accessibility tree
     * @param packageName App package name
     * @return Map of AccessibilityNodeInfo to UUID
     */
    suspend fun generateUuidsForTree(
        rootNode: AccessibilityNodeInfo,
        packageName: String? = null
    ): Map<AccessibilityNodeInfo, String> {
        val results = mutableMapOf<AccessibilityNodeInfo, String>()

        suspend fun processNode(node: AccessibilityNodeInfo) {
            try {
                val uuid = generateUuid(node, packageName)
                results[node] = uuid

                // Process children
                for (i in 0 until node.childCount) {
                    node.getChild(i)?.let { child ->
                        processNode(child)
                    }
                }
            } catch (e: Exception) {
                // Skip nodes that fail to generate UUID
            }
        }

        processNode(rootNode)
        return results
    }

    /**
     * Format UUID from fingerprint
     *
     * Creates formatted UUID string according to template:
     * `{packageName}.v{version}.{type}-{hash}`
     *
     * @param fingerprint Accessibility fingerprint
     * @return Formatted UUID string
     */
    private fun formatUuid(fingerprint: AccessibilityFingerprint): String {
        val hash = fingerprint.generateHash()
        val type = fingerprint.getElementType()

        return "${fingerprint.packageName}.v${fingerprint.appVersion}.$type-$hash"
    }

    /**
     * Check if UUID is for third-party app
     *
     * Determines if UUID was generated by this third-party generator
     * based on format pattern.
     *
     * @param uuid UUID string to check
     * @return true if third-party UUID format detected
     */
    fun isThirdPartyUuid(uuid: String): Boolean {
        // Pattern: package.v{version}.type-hash
        val pattern = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+\\.v[0-9]+\\.[0-9]+\\.[0-9]+\\.[a-z]+-[a-f0-9]{12}$")
        return pattern.matches(uuid)
    }

    /**
     * Parse third-party UUID
     *
     * Extracts components from third-party UUID format.
     *
     * @param uuid Third-party UUID string
     * @return Parsed components or null if invalid format
     */
    fun parseThirdPartyUuid(uuid: String): ThirdPartyUuidComponents? {
        if (!isThirdPartyUuid(uuid)) return null

        // Split: com.instagram.android.v12.0.0.button-a7f3e2c1d4b5
        val lastDotIndex = uuid.lastIndexOf('.')
        val dashIndex = uuid.indexOf('-', lastDotIndex)

        if (lastDotIndex < 0 || dashIndex < 0) return null

        val beforeType = uuid.substring(0, lastDotIndex)
        val type = uuid.substring(lastDotIndex + 1, dashIndex)
        val hash = uuid.substring(dashIndex + 1)

        // Extract package and version
        val versionIndex = beforeType.indexOf(".v")
        if (versionIndex < 0) return null

        val packageName = beforeType.substring(0, versionIndex)
        val version = beforeType.substring(versionIndex + 2) // Skip ".v"

        return ThirdPartyUuidComponents(
            packageName = packageName,
            version = version,
            elementType = type,
            hash = hash
        )
    }

    /**
     * Get cache statistics
     *
     * @return Current cache stats
     */
    suspend fun getCacheStats(): CacheStats {
        return cacheMutex.withLock {
            cache.getStats()
        }
    }

    /**
     * Clear cache
     *
     * Removes all cached UUIDs. Use when app updates detected.
     */
    suspend fun clearCache() {
        cacheMutex.withLock {
            cache.clear()
        }
    }

    /**
     * Clear cache for specific package
     *
     * @param packageName Package to clear from cache
     */
    suspend fun clearCacheForPackage(packageName: String) {
        cacheMutex.withLock {
            cache.clearPackage(packageName)
        }
    }
}

/**
 * Third-Party UUID Components
 *
 * Parsed components from third-party UUID format.
 *
 * @property packageName App package name
 * @property version App version string
 * @property elementType Element type (button, text, etc.)
 * @property hash Fingerprint hash
 */
data class ThirdPartyUuidComponents(
    val packageName: String,
    val version: String,
    val elementType: String,
    val hash: String
) {
    /**
     * Reconstruct original UUID
     */
    fun toUuid(): String {
        return "$packageName.v$version.$elementType-$hash"
    }
}

/**
 * Cache statistics
 *
 * @property size Number of cached UUIDs
 * @property hitCount Cache hit count
 * @property missCount Cache miss count
 */
data class CacheStats(
    val size: Int,
    val hitCount: Long,
    val missCount: Long
) {
    /**
     * Cache hit rate (0.0 - 1.0)
     */
    val hitRate: Float
        get() {
            val total = hitCount + missCount
            return if (total == 0L) 0f else hitCount.toFloat() / total
        }
}
