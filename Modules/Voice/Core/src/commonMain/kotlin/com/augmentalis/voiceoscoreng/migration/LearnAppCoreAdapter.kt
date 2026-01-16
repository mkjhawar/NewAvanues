/**
 * LearnAppCoreAdapter.kt - Migration adapter for LearnAppCore
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VoiceOSCoreNG Development Team
 * Created: 2026-01-06
 *
 * Bridges legacy LearnAppCore API to new VoiceOSCoreNG API.
 */
package com.augmentalis.voiceoscoreng.migration

import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.ProcessingMode

/**
 * New element processing result format.
 */
data class ElementProcessingResult(
    val vuid: String?,
    val commandText: String?,
    val actionType: String?,
    val confidence: Float,
    val success: Boolean,
    val error: String? = null
)

/**
 * Adapter for migrating LearnAppCore code to VoiceOSCoreNG.
 */
class LearnAppCoreAdapter {
    private var batchQueueSize: Int = 0

    fun getBatchQueueSize(): Int = batchQueueSize

    fun canProcessBatch(elements: List<LegacyElementInfo>): Boolean {
        return elements.isNotEmpty()
    }

    @Deprecated("Use VUIDCreator.generateVuid()", ReplaceWith("VUIDCreator.generateVuid(element)"))
    fun generateUUID(element: LegacyElementInfo): String {
        // Generate a simple VUID-format string
        val pkgHash = element.className.hashCode().toString(16).take(6)
        val typeCode = if (element.isClickable) "b" else "t"
        val elemHash = (element.text + element.resourceId).hashCode().toString(16).take(8)
        return "$pkgHash-$typeCode$elemHash"
    }

    companion object {
        /**
         * Converts legacy ElementInfo to new ElementInfo.
         */
        fun convertElementInfo(legacy: LegacyElementInfo): ElementInfo {
            return ElementInfo(
                className = legacy.className,
                text = legacy.text,
                contentDescription = legacy.contentDescription,
                resourceId = legacy.resourceId,
                bounds = Bounds(
                    legacy.boundsLeft,
                    legacy.boundsTop,
                    legacy.boundsRight,
                    legacy.boundsBottom
                ),
                isClickable = legacy.isClickable,
                isEnabled = legacy.isEnabled,
                isScrollable = legacy.isScrollable
            )
        }

        /**
         * Converts new ElementInfo to legacy format.
         */
        fun toLegacyElementInfo(element: ElementInfo): LegacyElementInfo {
            return LegacyElementInfo(
                className = element.className,
                text = element.text,
                contentDescription = element.contentDescription,
                resourceId = element.resourceId,
                isClickable = element.isClickable,
                isEnabled = element.isEnabled,
                isScrollable = element.isScrollable,
                boundsLeft = element.bounds.left,
                boundsTop = element.bounds.top,
                boundsRight = element.bounds.right,
                boundsBottom = element.bounds.bottom
            )
        }

        /**
         * Converts legacy processing mode to new format.
         */
        fun convertProcessingMode(legacy: LegacyProcessingMode): ProcessingMode {
            return when (legacy) {
                LegacyProcessingMode.IMMEDIATE -> ProcessingMode.IMMEDIATE
                LegacyProcessingMode.BATCH -> ProcessingMode.BATCH
            }
        }

        /**
         * Converts new processing mode to legacy format.
         */
        fun toLegacyProcessingMode(mode: ProcessingMode): LegacyProcessingMode {
            return when (mode) {
                ProcessingMode.IMMEDIATE -> LegacyProcessingMode.IMMEDIATE
                ProcessingMode.BATCH -> LegacyProcessingMode.BATCH
            }
        }

        /**
         * Checks if a UUID is in legacy format.
         */
        fun isLegacyUuid(uuid: String): Boolean {
            // Legacy formats:
            // 1. VoiceOS format: "com.example.app.button-a7f3e2c1d4b5" (contains dots and long hash)
            // 2. UUID v4 format: "550e8400-e29b-41d4-a716-446655440000" (36 chars with dashes)
            return uuid.contains(".") || uuid.length == 36
        }

        /**
         * Migrates legacy UUID to new VUID format.
         */
        fun migrateUuidToVuid(legacyUuid: String): String {
            // Extract meaningful parts from legacy UUID
            val hash = when {
                legacyUuid.contains(".") -> {
                    // VoiceOS format: extract hash after last hyphen
                    val parts = legacyUuid.split("-")
                    parts.lastOrNull()?.take(8) ?: legacyUuid.hashCode().toString(16).take(8)
                }
                legacyUuid.length == 36 -> {
                    // UUID v4 format: use first segment
                    legacyUuid.take(8)
                }
                else -> {
                    legacyUuid.hashCode().toString(16).take(8)
                }
            }

            // Generate VUID format: {pkgHash6}-{typeCode}{hash8}
            val pkgHash = legacyUuid.hashCode().toString(16).take(6).padStart(6, '0')
            return "$pkgHash-b$hash"
        }

        /**
         * Converts legacy processing result to new format.
         */
        fun convertProcessingResult(legacy: LegacyElementProcessingResult): ElementProcessingResult {
            val vuid = if (legacy.uuid.isNotEmpty()) {
                migrateUuidToVuid(legacy.uuid)
            } else {
                null
            }

            return ElementProcessingResult(
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
