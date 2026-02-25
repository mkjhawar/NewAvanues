/**
 * AvidGeneratorExt.kt - VoiceOS AVID Generation (Android Extension)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-23
 * Updated: 2026-01-14 - Migrated from VUID to AVID
 *
 * This file provides Android-specific extensions to the shared AvidGenerator.
 * Core AVID generation is in Modules:AVID module.
 *
 * ## Usage
 * ```kotlin
 * // Use shared module directly for compact format:
 * import com.augmentalis.avid.core.AvidGenerator
 * val avid = AvidGenerator.generateCompact("com.instagram.android", "12.0.0", "button")
 *
 * // Use Android extensions for Flutter support:
 * import com.augmentalis.avidcreator.core.AvidGeneratorExt
 * val flutterAvid = AvidGeneratorExt.generateFromFlutterIdentifier(flutterIdentifier)
 * ```
 */
package com.augmentalis.avidcreator.core

import com.augmentalis.avid.core.AvidGenerator
import com.augmentalis.avidcreator.flutter.FlutterIdentifier
import java.security.MessageDigest

// Re-export shared AvidGenerator for convenience
// Files can import from either location
typealias AvidGenerator = AvidGenerator

/**
 * Android-specific extensions to AvidGenerator
 *
 * Provides:
 * - Flutter 3.19+ identifier support
 * - Secure random hash generation using Android's SecureRandom
 */
object AvidGeneratorExt {

    // ========================================================================
    // Flutter 3.19+ Identifier Support (Android-specific)
    // ========================================================================

    /**
     * Generate stable AVID from Flutter 3.19+ identifier
     *
     * Flutter 3.19 introduced SemanticsProperties.identifier which maps to
     * Android's resource-id. This provides stable identification across sessions.
     *
     * @param flutterIdentifier The Flutter identifier extracted from node
     * @return Stable AVID based on the identifier
     */
    fun generateFromFlutterIdentifier(flutterIdentifier: FlutterIdentifier): String {
        return if (flutterIdentifier.isStable) {
            // Use stable hash for Flutter 3.19+ identifiers
            "flutter-${flutterIdentifier.toStableHash()}"
        } else {
            // Fall back to content-based generation for legacy Flutter
            AvidGenerator.generateFromContent(flutterIdentifier.identifierValue)
        }
    }

    /**
     * Generate stable AVID from raw Flutter resource ID
     *
     * Convenience method when you have the raw resource ID string.
     *
     * @param resourceId The viewIdResourceName from AccessibilityNodeInfo
     * @return Stable AVID if Flutter identifier detected, null otherwise
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
            AvidGenerator.generateFromContent(identifierValue)
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
     * Generate compact AVID with secure hash (Android-optimized)
     *
     * Same as AvidGenerator.generateCompact but uses SecureRandom
     * for the hash component.
     */
    fun generateCompactSecure(
        packageName: String,
        version: String,
        typeName: String
    ): String {
        val reversedPkg = AvidGenerator.reversePackage(packageName)
        val typeAbbrev = AvidGenerator.TypeAbbrev.fromTypeName(typeName)
        val hash8 = generateSecureHash8()
        return "$reversedPkg:$version:$typeAbbrev:$hash8"
    }
}

