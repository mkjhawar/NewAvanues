/**
 * UUIDCreator.kt - Backwards compatibility layer for UUID → VUID migration
 *
 * This file provides a deprecated typealias to maintain backwards compatibility
 * with code that imports `com.augmentalis.uuidcreator.UUIDCreator`.
 *
 * The actual implementation is in VUIDCreator.kt (VoiceUniqueID Creator).
 *
 * Migration: Use VUIDCreator instead of UUIDCreator.
 *
 * Author: VoiceOS Development Team
 * Created: 2025-12-27
 */
@file:Suppress("DEPRECATION")

package com.augmentalis.uuidcreator

/**
 * Deprecated: Use VUIDCreator instead
 *
 * This typealias exists for backwards compatibility during the UUID → VUID migration.
 * All new code should import VUIDCreator directly.
 */
@Deprecated(
    message = "Use VUIDCreator instead of UUIDCreator. UUID has been replaced with VUID (VoiceUniqueID).",
    replaceWith = ReplaceWith("VUIDCreator", "com.augmentalis.uuidcreator.VUIDCreator"),
    level = DeprecationLevel.WARNING
)
typealias UUIDCreator = VUIDCreator
