/**
 * DebugOverlayState.kt - Data models for LearnApp debug overlay
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-08
 *
 * Data models for the visual debugging overlay that shows element states,
 * VUID assignments, navigation links, and learning source during exploration.
 */
package com.augmentalis.voiceoscore.learnapp.debugging

import android.graphics.Rect

/**
 * Debug verbosity level
 */
enum class DebugVerbosity {
    /** Color borders only */
    MINIMAL,
    /** Color + truncated VUID */
    STANDARD,
    /** Color + VUID + links + source screen */
    VERBOSE
}

/**
 * Learning source for an element
 */
enum class LearningSource {
    /** Learned during full LearnApp exploration */
    LEARNAPP,
    /** Learned via Just-In-Time passive mode */
    JIT,
    /** Not yet learned */
    UNLEARNED,
    /** Currently being explored */
    EXPLORING
}

/**
 * Element state for debug overlay
 *
 * @property bounds Element bounds on screen
 * @property vuid VUID (Voice Unique ID) if assigned
 * @property displayName Human-readable name
 * @property classification Element classification (safe_clickable, dangerous, etc.)
 * @property learningSource How the element was learned
 * @property hashedScreen Whether element has been screen-hashed
 * @property linksToScreen Screen hash this element navigates TO (downstream)
 * @property linkedFromScreen Screen hash this element was reached FROM (upstream)
 * @property clickCount Number of times clicked during exploration
 * @property isDangerous Whether element is flagged as dangerous
 */
data class DebugElementState(
    val bounds: Rect,
    val vuid: String?,
    val displayName: String,
    val classification: String?,
    val learningSource: LearningSource,
    val hashedScreen: Boolean,
    val linksToScreen: String?,
    val linkedFromScreen: String?,
    val clickCount: Int = 0,
    val isDangerous: Boolean = false
)

/**
 * Screen state for debug overlay
 *
 * @property screenHash Unique screen identifier (MD5)
 * @property activityName Activity class name
 * @property packageName Package name of target app
 * @property elements Elements on this screen
 * @property totalElements Total element count
 * @property learnedElements Elements with VUID
 * @property exploredElements Elements that have been clicked
 * @property parentScreenHash Screen we navigated FROM to reach this
 */
data class DebugScreenState(
    val screenHash: String,
    val activityName: String,
    val packageName: String,
    val elements: List<DebugElementState>,
    val totalElements: Int,
    val learnedElements: Int,
    val exploredElements: Int,
    val parentScreenHash: String?
)

/**
 * Overall debug overlay state
 *
 * @property isEnabled Whether debug overlay is visible
 * @property verbosity Current verbosity level
 * @property currentScreen Current screen being displayed
 * @property totalScreensExplored Total screens explored so far
 * @property totalElementsLearned Total elements with VUID
 * @property explorationProgress Overall progress percentage
 */
data class DebugOverlayState(
    val isEnabled: Boolean = false,
    val verbosity: DebugVerbosity = DebugVerbosity.STANDARD,
    val currentScreen: DebugScreenState? = null,
    val totalScreensExplored: Int = 0,
    val totalElementsLearned: Int = 0,
    val explorationProgress: Int = 0
)

/**
 * Color scheme for debug overlay
 */
object DebugColors {
    /** LearnApp-learned element */
    const val LEARNAPP_GREEN = 0xFF4CAF50.toInt()

    /** JIT-learned element */
    const val JIT_BLUE = 0xFF2196F3.toInt()

    /** Has VUID but not linked */
    const val HAS_VUID_YELLOW = 0xFFFFEB3B.toInt()

    /** Currently being explored */
    const val EXPLORING_ORANGE = 0xFFFF9800.toInt()

    /** Not yet learned */
    const val UNLEARNED_GRAY = 0xFF9E9E9E.toInt()

    /** Dangerous element (skipped) */
    const val DANGEROUS_RED = 0xFFF44336.toInt()

    /** Links to another screen indicator */
    const val LINK_CYAN = 0xFF00BCD4.toInt()

    /** Background for legend */
    const val LEGEND_BG = 0xCC000000.toInt()

    /** Text color */
    const val TEXT_WHITE = 0xFFFFFFFF.toInt()

    /** Secondary text */
    const val TEXT_SECONDARY = 0xB0FFFFFF.toInt()
}
