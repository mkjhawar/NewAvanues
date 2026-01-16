/**
 * JITLearningAdapter.kt - Migration adapter for JITLearning and JitElementCapture
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VoiceOSCoreNG Development Team
 * Created: 2026-01-06
 *
 * Bridges legacy JITLearning/JitElementCapture APIs to new VoiceOSCoreNG APIs.
 */
package com.augmentalis.voiceoscoreng.migration

import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo

/**
 * New progress result format.
 */
data class JITProgressResult(
    val isActive: Boolean,
    val packageName: String,
    val screensExplored: Int,
    val elementsFound: Int
)

/**
 * New exploration progress result format.
 */
data class ExplorationProgressResult(
    val state: String,
    val screensExplored: Int,
    val elementsFound: Int
)

/**
 * New screen capture result format.
 */
data class ScreenCaptureResult(
    val packageName: String,
    val activityName: String,
    val screenHash: String,
    val elements: List<ElementInfo>,
    val captureTimeMs: Long
)

/**
 * Adapter for migrating JITLearning and JitElementCapture code to VoiceOSCoreNG.
 */
class JITLearningAdapter {
    private var isPaused: Boolean = false
    private var elementsDiscovered: Int = 0

    fun supportsLegacyProvider(): Boolean = true

    fun supportsExplorationBridge(): Boolean = true

    fun pauseLearning() {
        isPaused = true
    }

    fun resumeLearning() {
        isPaused = false
    }

    fun isLearningPaused(): Boolean = isPaused

    @Deprecated("Use new API", replaceWith = ReplaceWith("getDiscoveredElementCount()"))
    fun getElementsDiscoveredCount(): Int = elementsDiscovered

    @Deprecated("Use new API")
    fun canCaptureScreen(packageName: String): Boolean = true

    companion object {
        /**
         * Converts legacy JitCapturedElement to new ElementInfo.
         */
        fun convertToElementInfo(legacy: LegacyJitCapturedElement): ElementInfo {
            return ElementInfo(
                className = legacy.className,
                resourceId = legacy.viewIdResourceName,
                text = legacy.text,
                contentDescription = legacy.contentDescription,
                bounds = Bounds(
                    legacy.boundsLeft,
                    legacy.boundsTop,
                    legacy.boundsRight,
                    legacy.boundsBottom
                ),
                isClickable = legacy.isClickable,
                isScrollable = legacy.isScrollable,
                isEnabled = legacy.isEnabled
            )
        }

        /**
         * Converts new ElementInfo to legacy JitCapturedElement format.
         */
        fun toLegacyJitCapturedElement(
            element: ElementInfo,
            elementHash: String,
            depth: Int,
            indexInParent: Int,
            uuid: String?
        ): LegacyJitCapturedElement {
            return LegacyJitCapturedElement(
                elementHash = elementHash,
                className = element.className,
                viewIdResourceName = element.resourceId,
                text = element.text,
                contentDescription = element.contentDescription,
                boundsLeft = element.bounds.left,
                boundsTop = element.bounds.top,
                boundsRight = element.bounds.right,
                boundsBottom = element.bounds.bottom,
                isClickable = element.isClickable,
                isScrollable = element.isScrollable,
                isEnabled = element.isEnabled,
                depth = depth,
                indexInParent = indexInParent,
                uuid = uuid
            )
        }

        /**
         * Converts legacy JIT state to new progress format.
         */
        fun convertToProgress(legacy: LegacyJITState): JITProgressResult {
            return JITProgressResult(
                isActive = legacy.isActive,
                packageName = legacy.currentPackage,
                screensExplored = legacy.screensLearned,
                elementsFound = legacy.elementsDiscovered
            )
        }

        /**
         * Converts legacy exploration progress to new format.
         */
        fun convertExplorationProgress(legacy: LegacyExplorationProgress): ExplorationProgressResult {
            return ExplorationProgressResult(
                state = legacy.state,
                screensExplored = legacy.screensExplored,
                elementsFound = legacy.elementsFound
            )
        }

        /**
         * Extracts element hash from legacy UUID format.
         */
        fun extractElementHash(legacyUuid: String): String {
            val parts = legacyUuid.split("-")
            return parts.lastOrNull() ?: legacyUuid
        }

        /**
         * Converts legacy screen capture result to new format.
         */
        fun convertScreenCaptureResult(legacy: LegacyScreenCaptureResult): ScreenCaptureResult {
            return ScreenCaptureResult(
                packageName = legacy.packageName,
                activityName = legacy.activityName,
                screenHash = legacy.screenHash,
                elements = legacy.elements.map { convertToElementInfo(it) },
                captureTimeMs = legacy.captureTimeMs
            )
        }

        /**
         * Maps legacy exploration state to new state string.
         */
        fun mapExplorationState(state: LegacyExplorationState): String {
            return when (state) {
                LegacyExplorationState.IDLE -> "IDLE"
                LegacyExplorationState.RUNNING -> "RUNNING"
                LegacyExplorationState.PAUSED -> "PAUSED"
                LegacyExplorationState.COMPLETED -> "COMPLETED"
                LegacyExplorationState.FAILED -> "FAILED"
            }
        }
    }
}
