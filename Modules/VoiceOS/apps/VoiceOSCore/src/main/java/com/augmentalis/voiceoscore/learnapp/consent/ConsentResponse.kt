/**
 * ConsentResponse.kt - User consent response types
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Represents user responses to consent dialogs.
 */

package com.augmentalis.voiceoscore.learnapp.consent

/**
 * Consent Response
 *
 * Sealed class representing user response to consent dialog.
 */
sealed class ConsentResponse {
    /**
     * User accepted consent for full exploration
     */
    object Accept : ConsentResponse()

    /**
     * User chose to skip (JIT learning only)
     */
    object Skip : ConsentResponse()

    /**
     * User declined consent
     */
    object Decline : ConsentResponse()

    /**
     * User dismissed dialog without responding
     */
    object Dismissed : ConsentResponse()

    /**
     * Dialog timed out
     */
    object Timeout : ConsentResponse()
}

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
}
