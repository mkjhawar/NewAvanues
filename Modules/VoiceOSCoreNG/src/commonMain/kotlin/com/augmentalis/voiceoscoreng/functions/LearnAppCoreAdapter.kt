/**
 * LearnAppCoreAdapter.kt - Migration bridge from LearnAppCore to VoiceOSCoreNG
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * This adapter bridges the old LearnAppCore API to the new VoiceOSCoreNG API,
 * enabling gradual migration of existing code.
 *
 * ## Old API (LearnAppCore):
 * - Uses UUID (36 chars) for element identification
 * - Uses android.graphics.Rect for bounds
 * - ProcessingMode.IMMEDIATE / ProcessingMode.BATCH
 * - ElementInfo with AccessibilityNodeInfo reference
 *
 * ## New API (VoiceOSCoreNG):
 * - Uses VUID (16 chars) for element identification
 * - Uses Bounds (platform-independent)
 * - ProcessingMode.IMMEDIATE / ProcessingMode.BATCH (same concept)
 * - ElementInfo (platform-independent, no node reference)
 *
 * @see MigrationGuide for complete migration instructions
 */
package com.augmentalis.voiceoscoreng.functions

import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.ProcessingMode
import com.augmentalis.voiceoscoreng.common.VUIDGenerator
import com.augmentalis.voiceoscoreng.common.VUIDTypeCode

// ============================================================================
// Legacy Types (from old LearnAppCore)
// ============================================================================

/**
 * Legacy ElementInfo from LearnAppCore.
 *
 * This class mirrors the old ElementInfo structure for migration purposes.
 * In the old API, this included an AccessibilityNodeInfo reference which
 * is not available in KMP. The adapter converts this to the new ElementInfo.
 */
data class LegacyElementInfo(
    val className: String,
    val text: String = "",
    val contentDescription: String = "",
    val resourceId: String = "",
    val isClickable: Boolean = false,
    val isEnabled: Boolean = true,
    val isPassword: Boolean = false,
    val isScrollable: Boolean = false,
    val boundsLeft: Int = 0,
    val boundsTop: Int = 0,
    val boundsRight: Int = 0,
    val boundsBottom: Int = 0,
    val isLongClickable: Boolean = false,
    val isEditable: Boolean = false,
    val isCheckable: Boolean = false,
    val isChecked: Boolean = false,
    val isFocusable: Boolean = false,
    val screenWidth: Int = 0,
    val screenHeight: Int = 0,
    val index: Int = 0
) {
    /**
     * Legacy method to check if element is EditText.
     */
    fun isEditText(): Boolean {
        val inputFieldTypes = listOf(
            "EditText", "TextInputEditText", "AppCompatEditText",
            "AutoCompleteTextView", "MultiAutoCompleteTextView", "TextField"
        )
        return inputFieldTypes.any { className.contains(it, ignoreCase = true) }
    }

    /**
     * Legacy method to get display name.
     */
    fun getDisplayName(): String {
        return when {
            text.isNotBlank() -> text
            contentDescription.isNotBlank() -> contentDescription
            resourceId.isNotBlank() -> resourceId.substringAfterLast('/')
            else -> "Unknown"
        }
    }
}

/**
 * Legacy ProcessingMode enum from LearnAppCore.
 */
enum class LegacyProcessingMode {
    /** Insert to database immediately */
    IMMEDIATE,
    /** Queue for batch insert */
    BATCH
}

/**
 * Legacy ElementProcessingResult from LearnAppCore.
 */
data class LegacyElementProcessingResult(
    val uuid: String,
    val commandText: String?,
    val actionType: String?,
    val confidence: Double,
    val success: Boolean,
    val error: String? = null
)

// ============================================================================
// New Types (for VoiceOSCoreNG)
// ============================================================================

/**
 * New ElementProcessingResult for VoiceOSCoreNG.
 */
data class NewElementProcessingResult(
    val vuid: String?,
    val commandText: String?,
    val actionType: String?,
    val confidence: Float,
    val success: Boolean,
    val error: String? = null
)

// ============================================================================
// Adapter Implementation
// ============================================================================

/**
 * LearnAppCoreAdapter - Bridge between old LearnAppCore and new VoiceOSCoreNG.
 *
 * ## Usage:
 *
 * ### Converting ElementInfo:
 * ```kotlin
 * val legacyElement = LegacyElementInfo(...)
 * val newElement = LearnAppCoreAdapter.convertElementInfo(legacyElement)
 * ```
 *
 * ### Migrating UUIDs to VUIDs:
 * ```kotlin
 * val legacyUuid = "com.app.button-abc123"
 * val vuid = LearnAppCoreAdapter.migrateUuidToVuid(legacyUuid)
 * ```
 *
 * ### Using with existing LearnAppCore code:
 * ```kotlin
 * // Instead of:
 * val result = learnAppCore.processElement(element, packageName, ProcessingMode.IMMEDIATE)
 *
 * // Use:
 * val adapter = LearnAppCoreAdapter()
 * val newElement = LearnAppCoreAdapter.convertElementInfo(element)
 * // Process with VoiceOSCoreNG APIs...
 * ```
 */
class LearnAppCoreAdapter {

    private var _batchQueueSize: Int = 0
    private var _isPaused: Boolean = false

    /**
     * Get current batch queue size.
     */
    fun getBatchQueueSize(): Int = _batchQueueSize

    /**
     * Check if batch processing is available for given elements.
     */
    fun canProcessBatch(elements: List<LegacyElementInfo>): Boolean {
        return elements.isNotEmpty() && elements.size <= MAX_BATCH_SIZE
    }

    /**
     * Generate UUID for element using legacy algorithm.
     *
     * @deprecated Use VUIDGenerator.generate() instead
     */
    @Deprecated(
        message = "Use VUIDGenerator.generate() for new code",
        replaceWith = ReplaceWith(
            "VUIDGenerator.generate(packageName, typeCode, elementHash)",
            "com.augmentalis.voiceoscoreng.common.VUIDGenerator"
        )
    )
    fun generateUUID(element: LegacyElementInfo, packageName: String = ""): String {
        val elementHash = calculateElementHash(element)
        val typeCode = VUIDGenerator.getTypeCode(element.className)

        return if (packageName.isNotEmpty()) {
            VUIDGenerator.generate(packageName, typeCode, elementHash)
        } else {
            // Generate without package (uses generic hash)
            VUIDGenerator.generate("unknown", typeCode, elementHash)
        }
    }

    private fun calculateElementHash(element: LegacyElementInfo): String {
        val fingerprint = buildString {
            append(element.className)
            append("|")
            append(element.resourceId)
            append("|")
            append(element.text)
            append("|")
            append(element.contentDescription)
            append("|")
            append("${element.boundsLeft},${element.boundsTop},${element.boundsRight},${element.boundsBottom}")
        }

        // Simple hash for KMP compatibility
        var hash = 0L
        fingerprint.forEach { char ->
            hash = (hash * 31 + char.code) and 0xFFFFFFFFL
        }
        return hash.toString(16).padStart(12, '0').takeLast(12)
    }

    companion object {
        private const val MAX_BATCH_SIZE = 100

        // Legacy UUID patterns
        private val UUID_V4_PATTERN = Regex(
            "^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$",
            RegexOption.IGNORE_CASE
        )

        private val SIMPLE_LEGACY_PATTERN = Regex(
            "^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*\\.[a-z]+-[a-f0-9]{6,}$",
            RegexOption.IGNORE_CASE
        )

        // ==================== ElementInfo Conversion ====================

        /**
         * Convert legacy ElementInfo to new ElementInfo.
         *
         * @param legacy The legacy ElementInfo from old LearnAppCore
         * @return New VoiceOSCoreNG ElementInfo
         */
        fun convertElementInfo(legacy: LegacyElementInfo): ElementInfo {
            return ElementInfo(
                className = legacy.className,
                text = legacy.text,
                contentDescription = legacy.contentDescription,
                resourceId = legacy.resourceId,
                bounds = Bounds(
                    left = legacy.boundsLeft,
                    top = legacy.boundsTop,
                    right = legacy.boundsRight,
                    bottom = legacy.boundsBottom
                ),
                isClickable = legacy.isClickable,
                isScrollable = legacy.isScrollable,
                isEnabled = legacy.isEnabled,
                packageName = "" // Package name was not part of old ElementInfo
            )
        }

        /**
         * Convert new ElementInfo back to legacy format.
         *
         * Useful for interoperating with code that still uses old APIs.
         *
         * @param element The new VoiceOSCoreNG ElementInfo
         * @return Legacy ElementInfo structure
         */
        fun toLegacyElementInfo(element: ElementInfo): LegacyElementInfo {
            return LegacyElementInfo(
                className = element.className,
                text = element.text,
                contentDescription = element.contentDescription,
                resourceId = element.resourceId,
                boundsLeft = element.bounds.left,
                boundsTop = element.bounds.top,
                boundsRight = element.bounds.right,
                boundsBottom = element.bounds.bottom,
                isClickable = element.isClickable,
                isScrollable = element.isScrollable,
                isEnabled = element.isEnabled
            )
        }

        // ==================== ProcessingMode Conversion ====================

        /**
         * Convert legacy ProcessingMode to new ProcessingMode.
         */
        fun convertProcessingMode(legacy: LegacyProcessingMode): ProcessingMode {
            return when (legacy) {
                LegacyProcessingMode.IMMEDIATE -> ProcessingMode.IMMEDIATE
                LegacyProcessingMode.BATCH -> ProcessingMode.BATCH
            }
        }

        /**
         * Convert new ProcessingMode to legacy ProcessingMode.
         */
        fun toLegacyProcessingMode(mode: ProcessingMode): LegacyProcessingMode {
            return when (mode) {
                ProcessingMode.IMMEDIATE -> LegacyProcessingMode.IMMEDIATE
                ProcessingMode.BATCH -> LegacyProcessingMode.BATCH
            }
        }

        // ==================== UUID/VUID Migration ====================

        /**
         * Migrate a legacy UUID to new VUID format.
         *
         * @param legacyUuid The legacy UUID string
         * @return VUID string, or null if migration failed
         */
        fun migrateUuidToVuid(legacyUuid: String): String? {
            // Check if already a VUID
            if (VUIDGenerator.isValidVUID(legacyUuid)) {
                return legacyUuid
            }

            // Try simple legacy format (com.pkg.type-hash)
            if (SIMPLE_LEGACY_PATTERN.matches(legacyUuid)) {
                return migrateSimpleLegacy(legacyUuid)
            }

            // UUID v4 - extract components and generate VUID
            if (UUID_V4_PATTERN.matches(legacyUuid)) {
                val hash = legacyUuid.replace("-", "").takeLast(8)
                return VUIDGenerator.generate(
                    packageName = "migrated",
                    typeCode = VUIDTypeCode.ELEMENT,
                    elementHash = hash
                )
            }

            return null
        }

        private fun migrateSimpleLegacy(legacyUuid: String): String? {
            // Format: com.pkg.type-hash
            val dashIdx = legacyUuid.lastIndexOf('-')
            if (dashIdx < 0) return null

            val beforeDash = legacyUuid.substring(0, dashIdx)
            val hash = legacyUuid.substring(dashIdx + 1).take(8)

            // Extract type from last segment before dash
            val lastDotIdx = beforeDash.lastIndexOf('.')
            if (lastDotIdx < 0) return null

            val typeName = beforeDash.substring(lastDotIdx + 1)
            val packageName = beforeDash.substring(0, lastDotIdx)

            val typeCode = VUIDGenerator.getTypeCode(typeName)
            return VUIDGenerator.generate(packageName, typeCode, hash)
        }

        // ==================== Processing Result Conversion ====================

        /**
         * Convert legacy ElementProcessingResult to new format.
         */
        fun convertProcessingResult(legacy: LegacyElementProcessingResult): NewElementProcessingResult {
            val vuid = if (legacy.uuid.isNotEmpty()) {
                migrateUuidToVuid(legacy.uuid) ?: legacy.uuid
            } else null

            return NewElementProcessingResult(
                vuid = vuid,
                commandText = legacy.commandText,
                actionType = legacy.actionType,
                confidence = legacy.confidence.toFloat(),
                success = legacy.success,
                error = legacy.error
            )
        }
    }
}
