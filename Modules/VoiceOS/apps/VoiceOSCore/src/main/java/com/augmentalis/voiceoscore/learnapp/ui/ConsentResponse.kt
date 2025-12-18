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
    /** Package name associated with the consent response */
    abstract val packageName: String

    /**
     * User approved exploration
     */
    data class Approved(override val packageName: String) : ConsentResponse()

    /**
     * User declined exploration
     */
    data class Declined(override val packageName: String) : ConsentResponse()

    /**
     * User skipped (JIT learning mode)
     */
    data class Skipped(override val packageName: String) : ConsentResponse()

    /**
     * Dialog was dismissed without explicit action
     */
    data class Dismissed(override val packageName: String) : ConsentResponse()

    /**
     * Consent request timed out
     */
    data class Timeout(override val packageName: String) : ConsentResponse()
}
