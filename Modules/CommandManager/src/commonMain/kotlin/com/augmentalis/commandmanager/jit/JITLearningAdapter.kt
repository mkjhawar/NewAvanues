/**
 * JITLearningAdapter.kt - Migration bridge from JITLearning/JitElementCapture to VoiceOSCore
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * This adapter bridges the old JITLearning and JitElementCapture APIs to the
 * new VoiceOSCore ExplorationBridge and ElementParser APIs.
 *
 * ## Old APIs:
 * - JitElementCapture: Captures elements from accessibility tree
 * - JITLearningService: Foreground service for passive learning
 * - JITLearnerProvider: Interface for learning control
 * - ExplorationEngine: DFS exploration of entire apps
 *
 * ## New APIs (VoiceOSCore):
 * - ElementParser: Element extraction (platform-independent)
 * - ExplorationBridge: Exploration coordination
 * - ElementFingerprint: AVID generation (replaces ThirdPartyUuidGenerator)
 *
 * @see MigrationGuide for complete migration instructions
 */
package com.augmentalis.commandmanager

// ============================================================================
// Legacy Types (from JITLearning / JitElementCapture)
// ============================================================================

/**
 * Legacy JitCapturedElement from JitElementCapture.
 *
 * Mirrors the old JitCapturedElement structure.
 */
data class LegacyJitCapturedElement(
    val elementHash: String,
    val className: String,
    val viewIdResourceName: String? = null,
    val text: String? = null,
    val contentDescription: String? = null,
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
 * Legacy JITState from JITLearningService.
 */
data class LegacyJITState(
    val isActive: Boolean,
    val currentPackage: String?,
    val screensLearned: Int,
    val elementsDiscovered: Int,
    val lastCaptureTime: Long
)

/**
 * Legacy ExplorationProgress from ExplorationEngine.
 */
data class LegacyExplorationProgress(
    val state: String,
    val progress: Int,
    val screensExplored: Int,
    val elementsFound: Int,
    val elementsClicked: Int = 0,
    val navigationEdges: Int = 0,
    val errorCount: Int = 0,
    val currentScreen: String? = null
)

/**
 * Legacy ExplorationState enum from ExplorationEngine.
 */
enum class LegacyExplorationState {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED
}

/**
 * Legacy ScreenCaptureResult.
 */
data class LegacyScreenCaptureResult(
    val packageName: String,
    val activityName: String,
    val screenHash: String,
    val elements: List<LegacyJitCapturedElement>,
    val captureTimeMs: Long
)

// ============================================================================
// New Types (for VoiceOSCoreNG)
// ============================================================================

/**
 * New JIT progress result for VoiceOSCoreNG.
 */
data class JITProgressResult(
    val isActive: Boolean,
    val packageName: String?,
    val screensExplored: Int,
    val elementsFound: Int,
    val lastUpdateTime: Long = 0L
)

/**
 * New exploration progress result.
 */
data class NewExplorationProgressResult(
    val state: String,
    val screensExplored: Int,
    val elementsFound: Int,
    val elementsClicked: Int = 0,
    val currentScreen: String? = null
)

/**
 * New screen capture result.
 */
data class NewScreenCaptureResult(
    val packageName: String,
    val activityName: String,
    val screenHash: String,
    val elements: List<ElementInfo>,
    val captureTimeMs: Long
)

// ============================================================================
// Adapter Implementation
// ============================================================================

/**
 * JITLearningAdapter - Bridge between old JITLearning and new VoiceOSCoreNG.
 *
 * ## Usage:
 *
 * ### Converting JitCapturedElement:
 * ```kotlin
 * val legacyElement = LegacyJitCapturedElement(...)
 * val elementInfo = JITLearningAdapter.convertToElementInfo(legacyElement)
 * ```
 *
 * ### Migration from JITLearnerProvider:
 * ```kotlin
 * // Old:
 * provider.pauseLearning()
 * val count = provider.getScreensLearnedCount()
 *
 * // New (through adapter):
 * val adapter = JITLearningAdapter()
 * adapter.pauseLearning()
 * val count = adapter.getScreensExploredCount() // New naming
 * ```
 *
 * ### Migration from ExplorationEngine:
 * ```kotlin
 * // Old:
 * engine.startExploration("com.app")
 * engine.explorationState.collect { state -> ... }
 *
 * // New:
 * val bridge = ExplorationBridge(...)
 * bridge.startExploration("com.app")
 * bridge.progress.collect { progress -> ... }
 * ```
 */
class JITLearningAdapter {

    private var _isLearningPaused: Boolean = false
    private var _elementsDiscovered: Int = 0
    private var _screensExplored: Int = 0

    /**
     * Check if adapter supports legacy JITLearnerProvider interface.
     */
    fun supportsLegacyProvider(): Boolean = true

    /**
     * Check if adapter supports ExplorationBridge compatibility.
     */
    fun supportsExplorationBridge(): Boolean = true

    // ==================== JITLearnerProvider Bridge ====================

    /**
     * Pause JIT learning.
     */
    fun pauseLearning() {
        _isLearningPaused = true
    }

    /**
     * Resume JIT learning.
     */
    fun resumeLearning() {
        _isLearningPaused = false
    }

    /**
     * Check if learning is paused.
     */
    fun isLearningPaused(): Boolean = _isLearningPaused

    /**
     * Get elements discovered count.
     *
     * @deprecated Use getElementsFoundCount() instead
     */
    @Deprecated(
        message = "Use getElementsFoundCount() instead",
        replaceWith = ReplaceWith("getElementsFoundCount()")
    )
    fun getElementsDiscoveredCount(): Int = _elementsDiscovered

    /**
     * Get elements found count (new naming).
     */
    fun getElementsFoundCount(): Int = _elementsDiscovered

    /**
     * Get screens explored count.
     */
    fun getScreensExploredCount(): Int = _screensExplored

    /**
     * Check if screen can be captured.
     *
     * @deprecated This method is deprecated. Use new exploration APIs.
     */
    @Deprecated(
        message = "Use ExplorationBridge.canExplore() instead",
        replaceWith = ReplaceWith("ExplorationBridge().canExplore(packageName)")
    )
    fun canCaptureScreen(packageName: String): Boolean {
        // Basic validation - non-empty package name
        return packageName.isNotEmpty() && packageName.contains(".")
    }

    companion object {

        // ==================== Element Conversion ====================

        /**
         * Convert LegacyJitCapturedElement to new ElementInfo.
         */
        fun convertToElementInfo(legacy: LegacyJitCapturedElement): ElementInfo {
            return ElementInfo(
                className = legacy.className,
                resourceId = legacy.viewIdResourceName ?: "",
                text = legacy.text ?: "",
                contentDescription = legacy.contentDescription ?: "",
                bounds = Bounds(
                    left = legacy.boundsLeft,
                    top = legacy.boundsTop,
                    right = legacy.boundsRight,
                    bottom = legacy.boundsBottom
                ),
                isClickable = legacy.isClickable,
                isScrollable = legacy.isScrollable,
                isEnabled = legacy.isEnabled,
                packageName = ""
            )
        }

        /**
         * Convert new ElementInfo to LegacyJitCapturedElement.
         */
        fun toLegacyJitCapturedElement(
            element: ElementInfo,
            elementHash: String,
            depth: Int = 0,
            indexInParent: Int = 0,
            uuid: String? = null
        ): LegacyJitCapturedElement {
            return LegacyJitCapturedElement(
                elementHash = elementHash,
                className = element.className,
                viewIdResourceName = element.resourceId.ifEmpty { null },
                text = element.text.ifEmpty { null },
                contentDescription = element.contentDescription.ifEmpty { null },
                boundsLeft = element.bounds.left,
                boundsTop = element.bounds.top,
                boundsRight = element.bounds.right,
                boundsBottom = element.bounds.bottom,
                isClickable = element.isClickable,
                isLongClickable = false, // Not tracked in new ElementInfo
                isEditable = element.className.contains("Edit", ignoreCase = true),
                isScrollable = element.isScrollable,
                isCheckable = false, // Not tracked in new ElementInfo
                isFocusable = true, // Assumed
                isEnabled = element.isEnabled,
                depth = depth,
                indexInParent = indexInParent,
                uuid = uuid
            )
        }

        // ==================== JITState Conversion ====================

        /**
         * Convert LegacyJITState to new progress format.
         */
        fun convertToProgress(legacy: LegacyJITState): JITProgressResult {
            return JITProgressResult(
                isActive = legacy.isActive,
                packageName = legacy.currentPackage,
                screensExplored = legacy.screensLearned,
                elementsFound = legacy.elementsDiscovered,
                lastUpdateTime = legacy.lastCaptureTime
            )
        }

        // ==================== ExplorationProgress Conversion ====================

        /**
         * Convert LegacyExplorationProgress to new format.
         */
        fun convertExplorationProgress(legacy: LegacyExplorationProgress): NewExplorationProgressResult {
            return NewExplorationProgressResult(
                state = legacy.state,
                screensExplored = legacy.screensExplored,
                elementsFound = legacy.elementsFound,
                elementsClicked = legacy.elementsClicked,
                currentScreen = legacy.currentScreen
            )
        }

        // ==================== Screen Capture Conversion ====================

        /**
         * Convert LegacyScreenCaptureResult to new format.
         */
        fun convertScreenCaptureResult(legacy: LegacyScreenCaptureResult): NewScreenCaptureResult {
            return NewScreenCaptureResult(
                packageName = legacy.packageName,
                activityName = legacy.activityName,
                screenHash = legacy.screenHash,
                elements = legacy.elements.map { convertToElementInfo(it) },
                captureTimeMs = legacy.captureTimeMs
            )
        }

        // ==================== Hash Extraction ====================

        /**
         * Extract element hash from legacy UUID.
         *
         * Legacy format: com.package.type-elementhash
         */
        fun extractElementHash(legacyUuid: String): String? {
            val dashIdx = legacyUuid.lastIndexOf('-')
            if (dashIdx < 0 || dashIdx >= legacyUuid.length - 1) {
                return null
            }
            return legacyUuid.substring(dashIdx + 1)
        }

        // ==================== State Mapping ====================

        /**
         * Map legacy exploration state to new state string.
         */
        fun mapExplorationState(legacy: LegacyExplorationState): String {
            return when (legacy) {
                LegacyExplorationState.IDLE -> "IDLE"
                LegacyExplorationState.RUNNING -> "RUNNING"
                LegacyExplorationState.PAUSED -> "PAUSED"
                LegacyExplorationState.COMPLETED -> "COMPLETED"
                LegacyExplorationState.FAILED -> "FAILED"
            }
        }

        // ==================== AVID Migration ====================

        /**
         * Migrate legacy UUID to AVID (Avanues ID) format.
         *
         * Uses LearnAppCoreAdapter for the actual migration logic.
         */
        fun migrateUuidToAvid(legacyUuid: String): String? {
            return LearnAppCoreAdapter.migrateUuidToAvid(legacyUuid)
        }

        /**
         * Generate AVID fingerprint for a captured element.
         *
         * @param element The captured element
         * @param packageName The app package name
         * @return Generated AVID (Avanues ID)
         */
        fun generateAvid(element: LegacyJitCapturedElement, packageName: String): String {
            return ElementFingerprint.generate(
                className = element.className,
                packageName = packageName,
                resourceId = element.viewIdResourceName ?: "",
                text = element.text ?: "",
                contentDesc = element.contentDescription ?: ""
            )
        }
    }
}
