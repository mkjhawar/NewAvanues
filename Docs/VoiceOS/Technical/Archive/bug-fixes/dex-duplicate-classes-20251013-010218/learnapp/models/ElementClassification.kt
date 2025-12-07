/**
 * ElementClassification.kt - Element classification sealed class
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/models/ElementClassification.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Sealed class for element classification results
 */

package com.augmentalis.learnapp.models

/**
 * Element Classification
 *
 * Sealed class representing classification of UI elements.
 * Used to determine which elements are safe to explore.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val classification = classifier.classify(element)
 *
 * when (classification) {
 *     is ElementClassification.SafeClickable -> {
 *         // Safe to click and explore
 *         clickElement(element)
 *     }
 *     is ElementClassification.Dangerous -> {
 *         // Skip dangerous element
 *         println("Skipped: ${classification.reason}")
 *     }
 *     is ElementClassification.EditText -> {
 *         // Skip text input fields
 *     }
 *     is ElementClassification.NonClickable -> {
 *         // Not interactive
 *     }
 * }
 * ```
 *
 * @since 1.0.0
 */
sealed class ElementClassification {

    /**
     * Safe clickable element
     *
     * Element is safe to click and explore.
     *
     * @property element The element info
     */
    data class SafeClickable(
        val element: ElementInfo
    ) : ElementClassification()

    /**
     * Dangerous element
     *
     * Element should NOT be clicked (delete, logout, purchase, etc.)
     *
     * @property element The element info
     * @property reason Why element is dangerous
     */
    data class Dangerous(
        val element: ElementInfo,
        val reason: String
    ) : ElementClassification()

    /**
     * EditText field
     *
     * Text input field - skip because we can't auto-fill meaningfully.
     *
     * @property element The element info
     */
    data class EditText(
        val element: ElementInfo
    ) : ElementClassification()

    /**
     * Login screen element
     *
     * Part of login screen (password field, login button, etc.)
     *
     * @property element The element info
     * @property fieldType Type of login field (password, email, button)
     */
    data class LoginField(
        val element: ElementInfo,
        val fieldType: LoginFieldType
    ) : ElementClassification()

    /**
     * Non-clickable element
     *
     * Element is not interactive.
     *
     * @property element The element info
     */
    data class NonClickable(
        val element: ElementInfo
    ) : ElementClassification()

    /**
     * Disabled element
     *
     * Element is clickable but disabled.
     *
     * @property element The element info
     */
    data class Disabled(
        val element: ElementInfo
    ) : ElementClassification()
}

/**
 * Login Field Type
 *
 * Type of field on login screen.
 */
enum class LoginFieldType {
    PASSWORD,
    EMAIL,
    USERNAME,
    LOGIN_BUTTON,
    SIGNUP_BUTTON,
    OTHER
}
