/**
 * ScreenExplorationResult.kt - Result of screen exploration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Sealed class representing result of screen exploration.
 */

package com.augmentalis.voiceoscore.learnapp.exploration

import com.augmentalis.voiceoscore.learnapp.models.ElementClassification
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import com.augmentalis.voiceoscore.learnapp.models.ScreenState

/**
 * Screen Exploration Result
 *
 * Sealed class representing result of screen exploration.
 */
sealed class ScreenExplorationResult {

    /**
     * Success - screen explored successfully
     *
     * @property screenState Screen state
     * @property allElements All elements on screen
     * @property safeClickableElements Safe clickable elements
     * @property dangerousElements Dangerous elements (element, reason)
     * @property elementClassifications All element classifications
     * @property scrollableContainerCount Number of scrollable containers found
     */
    data class Success(
        val screenState: ScreenState,
        val allElements: List<ElementInfo>,
        val safeClickableElements: List<ElementInfo>,
        val dangerousElements: List<Pair<ElementInfo, String>>,
        val elementClassifications: List<ElementClassification>,
        val scrollableContainerCount: Int = 0
    ) : ScreenExplorationResult()

    /**
     * Already visited - screen was previously explored
     *
     * @property screenState Screen state
     */
    data class AlreadyVisited(
        val screenState: ScreenState
    ) : ScreenExplorationResult()

    /**
     * Login screen - pause for user login
     *
     * @property screenState Screen state
     * @property allElements All elements on screen (for registration)
     * @property loginElements Login-related elements
     */
    data class LoginScreen(
        val screenState: ScreenState,
        val allElements: List<ElementInfo>,
        val loginElements: List<ElementInfo>
    ) : ScreenExplorationResult()

    /**
     * Error - exploration failed
     *
     * @property message Error message
     */
    data class Error(
        val message: String
    ) : ScreenExplorationResult()
}
