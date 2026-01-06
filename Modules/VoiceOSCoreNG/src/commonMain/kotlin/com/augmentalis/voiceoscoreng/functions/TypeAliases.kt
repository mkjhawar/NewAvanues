/**
 * TypeAliases.kt - Type aliases for gradual migration from old modules
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * This file provides type aliases that map old class names to new implementations
 * in VoiceOSCoreNG. This enables gradual migration by allowing existing code
 * to continue using familiar names while actually using new implementations.
 *
 * ## Migration Path:
 *
 * 1. Add this import to your file:
 *    `import com.augmentalis.voiceoscoreng.functions.*`
 *
 * 2. Old code will continue to work with minimal changes
 *
 * 3. Gradually update to use new types directly
 *
 * 4. Remove migration imports when fully migrated
 *
 * @see MigrationGuide for complete migration instructions
 */
@file:Suppress("unused", "DEPRECATION")

package com.augmentalis.voiceoscoreng.functions

import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.ProcessingMode
import com.augmentalis.voiceoscoreng.common.VUIDGenerator
import com.augmentalis.voiceoscoreng.common.VUIDTypeCode
import com.augmentalis.voiceoscoreng.functions.ElementParser

// ============================================================================
// OLD LearnAppCore Types -> NEW VoiceOSCoreNG Types
// ============================================================================

/**
 * Old: com.augmentalis.learnappcore.models.ElementInfo
 * New: com.augmentalis.voiceoscoreng.common.ElementInfo
 *
 * @deprecated Use [ElementInfo] directly. This alias will be removed in v2.0.
 */
@Deprecated(
    message = "Use com.augmentalis.voiceoscoreng.common.ElementInfo directly",
    replaceWith = ReplaceWith(
        "ElementInfo",
        "com.augmentalis.voiceoscoreng.common.ElementInfo"
    ),
    level = DeprecationLevel.WARNING
)
typealias LearnAppElementInfo = ElementInfo

/**
 * Old: com.augmentalis.learnappcore.core.ProcessingMode
 * New: com.augmentalis.voiceoscoreng.common.ProcessingMode
 *
 * @deprecated Use [ProcessingMode] directly. This alias will be removed in v2.0.
 */
@Deprecated(
    message = "Use com.augmentalis.voiceoscoreng.common.ProcessingMode directly",
    replaceWith = ReplaceWith(
        "ProcessingMode",
        "com.augmentalis.voiceoscoreng.common.ProcessingMode"
    ),
    level = DeprecationLevel.WARNING
)
typealias LearnAppProcessingMode = ProcessingMode

/**
 * Old: android.graphics.Rect (used for bounds)
 * New: com.augmentalis.voiceoscoreng.common.Bounds
 *
 * Note: This is a type alias to our KMP-compatible Bounds class.
 * For Android Rect conversion, use Bounds.fromRect() extension.
 *
 * @deprecated Use [Bounds] directly. This alias will be removed in v2.0.
 */
@Deprecated(
    message = "Use com.augmentalis.voiceoscoreng.common.Bounds directly",
    replaceWith = ReplaceWith(
        "Bounds",
        "com.augmentalis.voiceoscoreng.common.Bounds"
    ),
    level = DeprecationLevel.WARNING
)
typealias ElementBounds = Bounds

// ============================================================================
// OLD UUID Types -> NEW VUID Types
// ============================================================================

/**
 * Old: java.util.UUID (standard UUID v4)
 * New: VUID string format
 *
 * VUID is a compact 16-character identifier optimized for voice accessibility.
 * Format: {pkgHash6}-{typeCode}{hash8}
 * Example: a3f2e1-b917cc9dc
 *
 * This typealias represents the VUID as a String since VUIDs are compact strings.
 */
typealias VUID = String

/**
 * Old: ThirdPartyUuidGenerator
 * New: VUIDGenerator
 *
 * @deprecated Use [VUIDGenerator] directly. This alias will be removed in v2.0.
 */
@Deprecated(
    message = "Use VUIDGenerator object directly",
    replaceWith = ReplaceWith(
        "VUIDGenerator",
        "com.augmentalis.voiceoscoreng.common.VUIDGenerator"
    ),
    level = DeprecationLevel.WARNING
)
typealias UuidCreator = VUIDGenerator

/**
 * Old: Element type classification (string-based)
 * New: VUIDTypeCode enum
 *
 * @deprecated Use [VUIDTypeCode] directly. This alias will be removed in v2.0.
 */
@Deprecated(
    message = "Use VUIDTypeCode enum directly",
    replaceWith = ReplaceWith(
        "VUIDTypeCode",
        "com.augmentalis.voiceoscoreng.common.VUIDTypeCode"
    ),
    level = DeprecationLevel.WARNING
)
typealias ElementTypeCode = VUIDTypeCode

// ============================================================================
// OLD JITLearning Types -> NEW VoiceOSCoreNG Types
// ============================================================================

/**
 * Old: com.augmentalis.voiceoscore.learnapp.jit.JitElementCapture
 * New: com.augmentalis.voiceoscoreng.extraction.ElementParser
 *
 * @deprecated Use [ElementParser] directly. This alias will be removed in v2.0.
 */
@Deprecated(
    message = "Use ElementParser object directly for element extraction",
    replaceWith = ReplaceWith(
        "ElementParser",
        "com.augmentalis.voiceoscoreng.extraction.ElementParser"
    ),
    level = DeprecationLevel.WARNING
)
typealias JITElementCapture = ElementParser

// NOTE: Exploration type aliases removed - ExplorationBridge moved to app integration layer

// ============================================================================
// Convenience Extensions for Migration
// ============================================================================

/**
 * Extension to convert old bounds format (left, top, right, bottom) to new Bounds.
 *
 * Usage:
 * ```kotlin
 * val bounds = boundsOf(10, 20, 110, 70)
 * ```
 */
fun boundsOf(left: Int, top: Int, right: Int, bottom: Int): Bounds {
    return Bounds(left, top, right, bottom)
}

/**
 * Extension to convert bounds string to Bounds object.
 *
 * Usage:
 * ```kotlin
 * val bounds = "10,20,110,70".toBounds()
 * ```
 */
fun String.toBounds(): Bounds? = Bounds.fromString(this)

/**
 * Extension to check if a string is a legacy UUID format.
 *
 * Usage:
 * ```kotlin
 * if (identifier.isLegacyUuid()) {
 *     val vuid = identifier.migrateToVuid()
 * }
 * ```
 */
fun String.isLegacyUuid(): Boolean {
    return VUIDGenerator.isLegacyUuid(this) || VUIDGenerator.isLegacyVoiceOS(this)
}

/**
 * Extension to migrate a legacy UUID to VUID format.
 *
 * Usage:
 * ```kotlin
 * val vuid = legacyUuid.migrateToVuid()
 * ```
 */
fun String.migrateToVuid(): String? {
    return when {
        VUIDGenerator.isLegacyVoiceOS(this) -> VUIDGenerator.migrateToCompact(this)
        VUIDGenerator.isLegacyUuid(this) -> {
            // For UUID v4, generate a new VUID based on the hash
            val hash = VUIDGenerator.extractHash(this) ?: return null
            VUIDGenerator.generate(
                packageName = "migrated",
                typeCode = VUIDTypeCode.ELEMENT,
                elementHash = hash
            )
        }
        VUIDGenerator.isValidVUID(this) -> this // Already a VUID
        else -> null
    }
}

/**
 * Extension to get element type code from class name.
 *
 * Usage:
 * ```kotlin
 * val typeCode = "android.widget.Button".toVUIDTypeCode()
 * ```
 */
fun String.toVUIDTypeCode(): VUIDTypeCode {
    return VUIDGenerator.getTypeCode(this)
}
