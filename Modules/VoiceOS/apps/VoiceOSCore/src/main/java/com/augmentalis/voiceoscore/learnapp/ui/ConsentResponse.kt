/**
 * ConsentResponse.kt - UI Consent response types for exploration consent
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Sealed class hierarchy representing user's response to exploration consent dialog.
 * This is the UI-specific version used in LearnAppIntegration.
 */
package com.augmentalis.voiceoscore.learnapp.ui

/**
 * Consent Response
 *
 * Represents possible user responses to the exploration consent dialog.
 */
sealed class ConsentResponse {
    /**
     * User approved exploration
     */
    object Approved : ConsentResponse()

    /**
     * User declined exploration
     */
    object Declined : ConsentResponse()

    /**
     * User skipped (JIT learning mode)
     */
    object Skipped : ConsentResponse()

    /**
     * Dialog was dismissed without explicit action
     */
    object Dismissed : ConsentResponse()

    /**
     * Consent request timed out
     */
    object Timeout : ConsentResponse()
}
