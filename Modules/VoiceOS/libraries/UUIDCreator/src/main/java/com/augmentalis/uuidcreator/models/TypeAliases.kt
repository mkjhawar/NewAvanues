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
    replaceWith = ReplaceWith("VUIDElement", "com.augmentalis.uuidcreator.models.VUIDElement"),
    level = DeprecationLevel.WARNING
)
typealias UUIDElement = VUIDElement

/**
 * Deprecated: Use VUIDHierarchy instead
 */
@Deprecated(
    message = "Use VUIDHierarchy instead of UUIDHierarchy. UUID has been replaced with VUID (VoiceUniqueID).",
    replaceWith = ReplaceWith("VUIDHierarchy", "com.augmentalis.uuidcreator.models.VUIDHierarchy"),
    level = DeprecationLevel.WARNING
)
typealias UUIDHierarchy = VUIDHierarchy

/**
 * Deprecated: Use VUIDMetadata instead
 */
@Deprecated(
    message = "Use VUIDMetadata instead of UUIDMetadata. UUID has been replaced with VUID (VoiceUniqueID).",
    replaceWith = ReplaceWith("VUIDMetadata", "com.augmentalis.uuidcreator.models.VUIDMetadata"),
    level = DeprecationLevel.WARNING
)
typealias UUIDMetadata = VUIDMetadata

/**
 * Deprecated: Use VUIDPosition instead
 */
@Deprecated(
    message = "Use VUIDPosition instead of UUIDPosition. UUID has been replaced with VUID (VoiceUniqueID).",
    replaceWith = ReplaceWith("VUIDPosition", "com.augmentalis.uuidcreator.models.VUIDPosition"),
    level = DeprecationLevel.WARNING
)
typealias UUIDPosition = VUIDPosition

/**
 * Deprecated: Use VUIDCommandResult instead
 */
@Deprecated(
    message = "Use VUIDCommandResult instead of UUIDCommandResult. UUID has been replaced with VUID (VoiceUniqueID).",
    replaceWith = ReplaceWith("VUIDCommandResult", "com.augmentalis.uuidcreator.models.VUIDCommandResult"),
    level = DeprecationLevel.WARNING
)
typealias UUIDCommandResult = VUIDCommandResult

/**
 * Deprecated: Use VUIDAccessibility instead
 */
@Deprecated(
    message = "Use VUIDAccessibility instead of UUIDAccessibility. UUID has been replaced with VUID (VoiceUniqueID).",
    replaceWith = ReplaceWith("VUIDAccessibility", "com.augmentalis.uuidcreator.models.VUIDAccessibility"),
    level = DeprecationLevel.WARNING
)
typealias UUIDAccessibility = VUIDAccessibility

/**
 * Deprecated: Use VUIDBounds instead
 */
@Deprecated(
    message = "Use VUIDBounds instead of UUIDBounds. UUID has been replaced with VUID (VoiceUniqueID).",
    replaceWith = ReplaceWith("VUIDBounds", "com.augmentalis.uuidcreator.models.VUIDBounds"),
    level = DeprecationLevel.WARNING
)
typealias UUIDBounds = VUIDBounds

// ===== Core Class Aliases =====

/**
 * Deprecated: Use VUIDGenerator instead
 */
@Deprecated(
    message = "Use VUIDGenerator instead of UUIDGenerator. UUID has been replaced with VUID (VoiceUniqueID).",
    replaceWith = ReplaceWith("VUIDGenerator", "com.augmentalis.uuidcreator.core.VUIDGenerator"),
    level = DeprecationLevel.WARNING
)
typealias UUIDGenerator = com.augmentalis.uuidcreator.core.VUIDGenerator

/**
 * Deprecated: Use VUIDRegistry instead
 */
@Deprecated(
    message = "Use VUIDRegistry instead of UUIDRegistry. UUID has been replaced with VUID (VoiceUniqueID).",
    replaceWith = ReplaceWith("VUIDRegistry", "com.augmentalis.uuidcreator.core.VUIDRegistry"),
    level = DeprecationLevel.WARNING
)
typealias UUIDRegistry = com.augmentalis.uuidcreator.core.VUIDRegistry

/**
 * Deprecated: Use VUIDCreator instead
 */
@Deprecated(
    message = "Use VUIDCreator instead of UUIDCreator. UUID has been replaced with VUID (VoiceUniqueID).",
    replaceWith = ReplaceWith("VUIDCreator", "com.augmentalis.uuidcreator.VUIDCreator"),
    level = DeprecationLevel.WARNING
)
typealias UUIDCreator = com.augmentalis.uuidcreator.VUIDCreator

// ===== Database Class Aliases =====

/**
 * Deprecated: Use SQLDelightVUIDRepositoryAdapter instead
 */
@Deprecated(
    message = "Use SQLDelightVUIDRepositoryAdapter instead. UUID has been replaced with VUID (VoiceUniqueID).",
    replaceWith = ReplaceWith("SQLDelightVUIDRepositoryAdapter", "com.augmentalis.uuidcreator.database.repository.SQLDelightVUIDRepositoryAdapter"),
    level = DeprecationLevel.WARNING
)
typealias SQLDelightUUIDRepositoryAdapter = com.augmentalis.uuidcreator.database.repository.SQLDelightVUIDRepositoryAdapter

// ===== IPC/Parcelable Class Aliases =====

/**
 * Deprecated: Use VUIDElementData instead
 */
@Deprecated(
    message = "Use VUIDElementData instead of UUIDElementData. UUID has been replaced with VUID (VoiceUniqueID).",
    replaceWith = ReplaceWith("VUIDElementData", "com.augmentalis.uuidcreator.VUIDElementData"),
    level = DeprecationLevel.WARNING
)
typealias UUIDElementData = com.augmentalis.uuidcreator.VUIDElementData

/**
 * Deprecated: Use VUIDCommandResultData instead
 */
@Deprecated(
    message = "Use VUIDCommandResultData instead of UUIDCommandResultData. UUID has been replaced with VUID (VoiceUniqueID).",
    replaceWith = ReplaceWith("VUIDCommandResultData", "com.augmentalis.uuidcreator.VUIDCommandResultData"),
    level = DeprecationLevel.WARNING
)
typealias UUIDCommandResultData = com.augmentalis.uuidcreator.VUIDCommandResultData

/**
 * Deprecated: Use VUIDCreatorServiceBinder instead
 */
@Deprecated(
    message = "Use VUIDCreatorServiceBinder instead of UUIDCreatorServiceBinder. UUID has been replaced with VUID (VoiceUniqueID).",
    replaceWith = ReplaceWith("VUIDCreatorServiceBinder", "com.augmentalis.uuidcreator.VUIDCreatorServiceBinder"),
    level = DeprecationLevel.WARNING
)
typealias UUIDCreatorServiceBinder = com.augmentalis.uuidcreator.VUIDCreatorServiceBinder

// ===== ViewModel Class Aliases =====

/**
 * Deprecated: Use VUIDViewModel instead
 */
@Deprecated(
    message = "Use VUIDViewModel instead of UUIDViewModel. UUID has been replaced with VUID (VoiceUniqueID).",
    replaceWith = ReplaceWith("VUIDViewModel", "com.augmentalis.uuidcreator.ui.VUIDViewModel"),
    level = DeprecationLevel.WARNING
)
typealias UUIDViewModel = com.augmentalis.uuidcreator.ui.VUIDViewModel

/**
 * Deprecated: Use VUIDUiState instead
 */
@Deprecated(
    message = "Use VUIDUiState instead of UUIDUiState. UUID has been replaced with VUID (VoiceUniqueID).",
    replaceWith = ReplaceWith("VUIDUiState", "com.augmentalis.uuidcreator.ui.VUIDUiState"),
    level = DeprecationLevel.WARNING
)
typealias UUIDUiState = com.augmentalis.uuidcreator.ui.VUIDUiState

/**
 * Deprecated: Use VUIDElementInfo instead
 */
@Deprecated(
    message = "Use VUIDElementInfo instead of UUIDElementInfo. UUID has been replaced with VUID (VoiceUniqueID).",
    replaceWith = ReplaceWith("VUIDElementInfo", "com.augmentalis.uuidcreator.ui.VUIDElementInfo"),
    level = DeprecationLevel.WARNING
)
typealias UUIDElementInfo = com.augmentalis.uuidcreator.ui.VUIDElementInfo
