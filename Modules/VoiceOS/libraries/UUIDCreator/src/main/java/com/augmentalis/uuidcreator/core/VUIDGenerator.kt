/**
 * VUIDGenerator.kt - VoiceOS VUID Generation (Android Extension)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-23
 * Updated: 2025-12-30 - Migrated to shared KMP module with Android extensions
 *
 * This file provides Android-specific extensions to the shared VUIDGenerator.
 * Core VUID generation is in Common:VUID module.
 *
 * ## Usage
 * ```kotlin
 * // Use shared module directly for compact format:
 * import com.augmentalis.vuid.core.VUIDGenerator
 * val vuid = VUIDGenerator.generateCompact("com.instagram.android", "12.0.0", "button")
 *
 * // Use Android extensions for Flutter support:
 * import com.augmentalis.uuidcreator.core.VUIDGeneratorExt
 * val flutterVuid = VUIDGeneratorExt.generateFromFlutterIdentifier(flutterIdentifier)
 * ```
 */
package com.augmentalis.uuidcreator.core

import com.augmentalis.uuidcreator.flutter.FlutterIdentifier
import java.security.MessageDigest

// Re-export shared VUIDGenerator for convenience
// Files can import from either location
typealias VUIDGenerator = com.augmentalis.vuid.core.VUIDGenerator

/**
 * Android-specific extensions to VUIDGenerator
 *
 * Provides:
 * - Flutter 3.19+ identifier support
 * - Secure random hash generation using Android's SecureRandom
 */
object VUIDGeneratorExt {

    // ========================================================================
    // Flutter 3.19+ Identifier Support (Android-specific)
    // ========================================================================

    /**
     * Generate stable VUID from Flutter 3.19+ identifier
     *
     * Flutter 3.19 introduced SemanticsProperties.identifier which maps to
     * Android's resource-id. This provides stable identification across sessions.
     *
     * @param flutterIdentifier The Flutter identifier extracted from node
     * @return Stable VUID based on the identifier
     */
    fun generateFromFlutterIdentifier(flutterIdentifier: FlutterIdentifier): String {
        return if (flutterIdentifier.isStable) {
            // Use stable hash for Flutter 3.19+ identifiers
            "flutter-${flutterIdentifier.toStableHash()}"
        } else {
            // Fall back to content-based generation for legacy Flutter
            VUIDGenerator.generateFromContent(flutterIdentifier.identifierValue)
        }
    }

    /**
     * Generate stable VUID from raw Flutter resource ID
     *
     * Convenience method when you have the raw resource ID string.
     *
     * @param resourceId The viewIdResourceName from AccessibilityNodeInfo
     * @return Stable VUID if Flutter identifier detected, null otherwise
     */
    fun generateFromFlutterResourceId(resourceId: String): String? {
        // Quick check for Flutter patterns
        if (!resourceId.contains("flutter", ignoreCase = true)) {
            return null
        }

        // Check if this is a stable Flutter 3.19+ identifier
        val isStable = resourceId.contains("flutter_semantics_") ||
                resourceId.contains("flutter_id_")

        // Extract identifier value (remove package prefix if present)
        val identifierValue = if (resourceId.contains(":id/")) {
            resourceId.substringAfter(":id/")
        } else {
            resourceId
        }

        return if (isStable) {
            // Generate stable hash for Flutter 3.19+
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(identifierValue.toByteArray())
            val hash = hashBytes.take(16).joinToString("") { "%02x".format(it) }
            "flutter-$hash"
        } else {
            // Use content-based generation for legacy Flutter
            VUIDGenerator.generateFromContent(identifierValue)
        }
    }

    // ========================================================================
    // Secure Hash Generation (Android-specific)
    // ========================================================================

    /**
     * Generate cryptographically secure 8-character hash
     * Uses Android's SecureRandom for better entropy than kotlin.random
     */
    fun generateSecureHash8(): String {
        val secureRandom = java.security.SecureRandom()
        val bytes = ByteArray(4)
        secureRandom.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Generate compact VUID with secure hash (Android-optimized)
     *
     * Same as VUIDGenerator.generateCompact but uses SecureRandom
     * for the hash component.
     */
    fun generateCompactSecure(
        packageName: String,
        version: String,
        typeName: String
    ): String {
        val reversedPkg = com.augmentalis.vuid.core.VUIDGenerator.reversePackage(packageName)
        val typeAbbrev = com.augmentalis.vuid.core.VUIDGenerator.TypeAbbrev.fromTypeName(typeName)
        val hash8 = generateSecureHash8()
        return "$reversedPkg:$version:$typeAbbrev:$hash8"
    }
}
