/**
 * UUIDGenerator.kt - Backwards compatibility layer for UUID → VUID migration
 *
 * This file provides a deprecated typealias to maintain backwards compatibility
 * with code that imports `com.augmentalis.uuidcreator.core.UUIDGenerator`.
 *
 * The actual implementation is in VUIDGenerator.kt (VoiceUniqueID Generator).
 *
 * Migration: Use VUIDGenerator instead of UUIDGenerator.
 *
 * Author: VoiceOS Development Team
 * Created: 2025-12-27
 */
@file:Suppress("DEPRECATION")

package com.augmentalis.uuidcreator.core

/**
 * Deprecated: Use VUIDGenerator instead
 *
 * This typealias exists for backwards compatibility during the UUID → VUID migration.
 * All new code should import VUIDGenerator directly.
 */
@Deprecated(
    message = "Use VUIDGenerator instead of UUIDGenerator. UUID has been replaced with VUID (VoiceUniqueID).",
    replaceWith = ReplaceWith("VUIDGenerator", "com.augmentalis.uuidcreator.core.VUIDGenerator"),
    level = DeprecationLevel.WARNING
)
typealias UUIDGenerator = VUIDGenerator
