/**
 * LoginPromptAction.kt - User action when login screen detected
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * User action when login screen is detected during exploration.
 */

package com.augmentalis.voiceoscore.learnapp.overlays

/**
 * Login Prompt Action
 *
 * User action when login screen is detected during exploration.
 */
sealed class LoginPromptAction {
    /**
     * Continue exploration after login
     */
    object Continue : LoginPromptAction()

    /**
     * Skip the login and continue with current screen
     */
    object Skip : LoginPromptAction()

    /**
     * Pause exploration for manual login
     */
    object Pause : LoginPromptAction()

    /**
     * Stop exploration completely
     */
    object Stop : LoginPromptAction()

    /**
     * Dismiss the overlay without action
     */
    object Dismiss : LoginPromptAction()
}
