/**
 * TypeAliases.kt - Backwards compatibility layer for UUID → VUID migration
 *
 * This file provides deprecated type aliases to ensure existing code continues
 * to compile during the migration from UUID to VUID (VoiceUniqueID).
 *
 * All UUID-based types are deprecated with ReplaceWith suggestions pointing
 * to their VUID equivalents. This allows for a gradual migration without
 * breaking existing code.
 *
 * Migration Timeline:
 * - Phase 1: Introduce VUID types alongside UUID types (this file)
 * - Phase 2: Deprecate UUID types (warnings only)
 * - Phase 3: Mark as errors (6 months after Phase 1)
 * - Phase 4: Remove completely (12 months after Phase 1)
 *
 * Author: AVAMagic Ecosystem v2.0
 * Created: 2025-12-23
 * Migration: UUID → VUID (VoiceUniqueID)
 */

package com.augmentalis.uuidcreator.models

/**
 * Deprecated: Use VUIDElement instead
 *
 * This type alias provides backwards compatibility during the UUID → VUID migration.
 * All new code should use VUIDElement directly.
 */
@Deprecated(
    message = "Use VUIDElement instead of UUIDElement. UUID has been replaced with VUID (VoiceUniqueID) for universal identification.",
    replaceWith = ReplaceWith("VUIDElement", "com.augmentalis.vuidcreator.models.VUIDElement"),
    level = DeprecationLevel.WARNING
)
typealias UUIDElement = VUIDElement

/**
 * Deprecated: Use VUIDHierarchy instead
 */
@Deprecated(
    message = "Use VUIDHierarchy instead of UUIDHierarchy. UUID has been replaced with VUID (VoiceUniqueID).",
    replaceWith = ReplaceWith("VUIDHierarchy", "com.augmentalis.vuidcreator.models.VUIDHierarchy"),
    level = DeprecationLevel.WARNING
)
typealias UUIDHierarchy = VUIDHierarchy

/**
 * Deprecated: Use VUIDMetadata instead
 */
@Deprecated(
    message = "Use VUIDMetadata instead of UUIDMetadata. UUID has been replaced with VUID (VoiceUniqueID).",
    replaceWith = ReplaceWith("VUIDMetadata", "com.augmentalis.vuidcreator.models.VUIDMetadata"),
    level = DeprecationLevel.WARNING
)
typealias UUIDMetadata = VUIDMetadata

/**
 * Deprecated: Use VUIDPosition instead
 */
@Deprecated(
    message = "Use VUIDPosition instead of UUIDPosition. UUID has been replaced with VUID (VoiceUniqueID).",
    replaceWith = ReplaceWith("VUIDPosition", "com.augmentalis.vuidcreator.models.VUIDPosition"),
    level = DeprecationLevel.WARNING
)
typealias UUIDPosition = VUIDPosition

/**
 * Deprecated: Use VUIDCommandResult instead
 */
@Deprecated(
    message = "Use VUIDCommandResult instead of UUIDCommandResult. UUID has been replaced with VUID (VoiceUniqueID).",
    replaceWith = ReplaceWith("VUIDCommandResult", "com.augmentalis.vuidcreator.models.VUIDCommandResult"),
    level = DeprecationLevel.WARNING
)
typealias UUIDCommandResult = VUIDCommandResult

/**
 * Deprecated: Use VUIDAccessibility instead
 */
@Deprecated(
    message = "Use VUIDAccessibility instead of UUIDAccessibility. UUID has been replaced with VUID (VoiceUniqueID).",
    replaceWith = ReplaceWith("VUIDAccessibility", "com.augmentalis.vuidcreator.models.VUIDAccessibility"),
    level = DeprecationLevel.WARNING
)
typealias UUIDAccessibility = VUIDAccessibility

/**
 * Deprecated: Use VUIDBounds instead
 */
@Deprecated(
    message = "Use VUIDBounds instead of UUIDBounds. UUID has been replaced with VUID (VoiceUniqueID).",
    replaceWith = ReplaceWith("VUIDBounds", "com.augmentalis.vuidcreator.models.VUIDBounds"),
    level = DeprecationLevel.WARNING
)
typealias UUIDBounds = VUIDBounds
