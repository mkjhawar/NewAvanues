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

import com.augmentalis.avid.TypeCode
import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.ElementFingerprint
import com.augmentalis.voiceoscoreng.common.ProcessingMode
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
// OLD UUID/VUID Types -> NEW AVID Types
// ============================================================================

/**
 * Old: java.util.UUID (standard UUID v4) / VUID string format
 * New: AVID element fingerprint format
 *
 * Element fingerprints use the AVID module's Fingerprint class.
 * Format: {TypeCode}:{hash8}
 * Example: BTN:a3f2e1c9
 *
 * This typealias represents the element fingerprint as a String.
 */
typealias ElementId = String

/**
 * Old: ThirdPartyUuidGenerator / VUIDGenerator
 * New: ElementFingerprint (wrapper around AVID module)
 *
 * @deprecated Use [ElementFingerprint] directly. This alias will be removed in v2.0.
 */
@Deprecated(
    message = "Use ElementFingerprint object directly",
    replaceWith = ReplaceWith(
        "ElementFingerprint",
        "com.augmentalis.voiceoscoreng.common.ElementFingerprint"
    ),
    level = DeprecationLevel.WARNING
)
typealias UuidCreator = ElementFingerprint

/**
 * Old: Element type classification (VUIDTypeCode enum)
 * New: TypeCode constants (3-char strings)
 *
 * TypeCode provides 3-character type codes like "BTN", "INP", "TXT".
 * Access via TypeCode.BUTTON, TypeCode.INPUT, etc.
 */
typealias ElementTypeCode = String

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
 * Extension to get element type code from class name.
 *
 * Usage:
 * ```kotlin
 * val typeCode = "android.widget.Button".toTypeCode()
 * // Returns "BTN"
 * ```
 */
fun String.toTypeCode(): String {
    return TypeCode.fromTypeName(this)
}
