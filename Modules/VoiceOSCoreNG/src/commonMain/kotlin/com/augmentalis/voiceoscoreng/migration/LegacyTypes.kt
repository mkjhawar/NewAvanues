/**
 * LegacyTypes.kt - Legacy type definitions for migration adapters
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VoiceOSCoreNG Development Team
 * Created: 2026-01-06
 *
 * These types represent the old VoiceOS 3.x API structures that are being
 * migrated to the new VoiceOSCoreNG API. Used by migration adapters to
 * provide backwards compatibility.
 */
package com.augmentalis.voiceoscoreng.migration

/**
 * Legacy JIT captured element from JitElementCapture service.
 */
data class LegacyJitCapturedElement(
    val elementHash: String = "",
    val className: String = "",
    val viewIdResourceName: String = "",
    val text: String = "",
    val contentDescription: String = "",
    val boundsLeft: Int = 0,
    val boundsTop: Int = 0,
    val boundsRight: Int = 0,
    val boundsBottom: Int = 0,
    val isClickable: Boolean = false,
    val isLongClickable: Boolean = false,
    val isEditable: Boolean = false,
    val isScrollable: Boolean = false,
    val isCheckable: Boolean = false,
    val isFocusable: Boolean = false,
    val isEnabled: Boolean = true,
    val depth: Int = 0,
    val indexInParent: Int = 0,
    val uuid: String? = null
)

/**
 * Legacy JIT state from JITLearning service.
 */
data class LegacyJITState(
    val isActive: Boolean = false,
    val currentPackage: String = "",
    val screensLearned: Int = 0,
    val elementsDiscovered: Int = 0,
    val lastCaptureTime: Long = 0L
)

/**
 * Legacy exploration progress from ExplorationEngine.
 */
data class LegacyExplorationProgress(
    val state: String = "IDLE",
    val progress: Int = 0,
    val screensExplored: Int = 0,
    val elementsFound: Int = 0,
    val elementsClicked: Int = 0,
    val navigationEdges: Int = 0,
    val errorCount: Int = 0,
    val currentScreen: String = ""
)

/**
 * Legacy exploration states.
 */
enum class LegacyExplorationState {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED
}

/**
 * Legacy screen capture result.
 */
data class LegacyScreenCaptureResult(
    val packageName: String = "",
    val activityName: String = "",
    val screenHash: String = "",
    val elements: List<LegacyJitCapturedElement> = emptyList(),
    val captureTimeMs: Long = 0L
)

/**
 * Legacy element info from LearnAppCore.
 */
data class LegacyElementInfo(
    val className: String = "",
    val text: String = "",
    val contentDescription: String = "",
    val resourceId: String = "",
    val isClickable: Boolean = false,
    val isEnabled: Boolean = true,
    val isScrollable: Boolean = false,
    val boundsLeft: Int = 0,
    val boundsTop: Int = 0,
    val boundsRight: Int = 0,
    val boundsBottom: Int = 0
)

/**
 * Legacy processing modes.
 */
enum class LegacyProcessingMode {
    IMMEDIATE,
    BATCH
}

/**
 * Legacy element processing result.
 */
data class LegacyElementProcessingResult(
    val uuid: String = "",
    val commandText: String? = null,
    val actionType: String? = null,
    val confidence: Double = 0.0,
    val success: Boolean = false,
    val error: String? = null
)
