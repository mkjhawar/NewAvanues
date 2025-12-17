/**
 * ElementClassification.kt - Classification of UI elements
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Sealed class representing classification of UI elements during exploration.
 */

package com.augmentalis.voiceoscore.learnapp.models

/**
 * Element Classification
 *
 * Classifies UI elements during exploration into categories
 * for safety and interaction decisions.
 */
sealed class ElementClassification {

    /**
     * Safe clickable element - can be clicked during exploration
     */
    data class SafeClickable(
        val element: ElementInfo
    ) : ElementClassification()

    /**
     * Dangerous clickable element - should not be clicked
     */
    data class DangerousClickable(
        val element: ElementInfo,
        val reason: String
    ) : ElementClassification()

    /**
     * Text input field - requires special handling
     */
    data class TextInput(
        val element: ElementInfo
    ) : ElementClassification()

    /**
     * Non-clickable element - not interactive
     */
    data class NonClickable(
        val element: ElementInfo
    ) : ElementClassification()

    /**
     * Login-related element - indicates login screen
     */
    data class LoginElement(
        val element: ElementInfo,
        val loginType: LoginElementType
    ) : ElementClassification()
}

/**
 * Type of login-related element
 */
enum class LoginElementType {
    USERNAME_FIELD,
    PASSWORD_FIELD,
    LOGIN_BUTTON,
    SIGNUP_BUTTON,
    FORGOT_PASSWORD,
    SOCIAL_LOGIN,
    OTHER
}
