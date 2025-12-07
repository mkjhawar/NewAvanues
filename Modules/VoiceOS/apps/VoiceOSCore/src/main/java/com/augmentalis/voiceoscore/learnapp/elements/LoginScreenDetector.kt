/**
 * LoginScreenDetector.kt - Detects login screens
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/elements/LoginScreenDetector.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Detects login screens to pause exploration
 */

package com.augmentalis.voiceoscore.learnapp.elements

import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import com.augmentalis.voiceoscore.learnapp.models.LoginFieldType

/**
 * Login Screen Detector
 *
 * Detects login screens by looking for password fields + email/username fields + login buttons.
 * When login screen detected, exploration pauses and prompts user to login manually.
 *
 * ## Detection Logic
 *
 * A screen is classified as login screen if it has:
 * - Password field (isPassword = true OR hint/text contains "password")
 * - AND (Email field OR Username field OR Login button)
 *
 * ## Usage Example
 *
 * ```kotlin
 * val detector = LoginScreenDetector()
 *
 * val elements = collectAllElements(rootNode)
 * val isLogin = detector.isLoginScreen(elements)
 *
 * if (isLogin) {
 *     pauseExploration()
 *     showLoginPrompt()
 * }
 * ```
 *
 * @since 1.0.0
 */
class LoginScreenDetector {

    /**
     * Check if screen is a login screen
     *
     * @param elements All elements on screen
     * @return true if login screen detected
     */
    fun isLoginScreen(elements: List<ElementInfo>): Boolean {
        val hasPasswordField = hasPasswordField(elements)
        val hasEmailOrUsernameField = hasEmailField(elements) || hasUsernameField(elements)
        val hasLoginButton = hasLoginButton(elements)

        // Login screen if: password field + (email/username field OR login button)
        return hasPasswordField && (hasEmailOrUsernameField || hasLoginButton)
    }

    /**
     * Classify login field type
     *
     * @param element Element to classify
     * @return LoginFieldType or null if not a login field
     */
    fun classifyLoginField(element: ElementInfo): LoginFieldType? {
        return when {
            isPasswordField(element) -> LoginFieldType.PASSWORD
            isEmailField(element) -> LoginFieldType.EMAIL
            isUsernameField(element) -> LoginFieldType.USERNAME
            isLoginButton(element) -> LoginFieldType.LOGIN_BUTTON
            isSignupButton(element) -> LoginFieldType.SIGNUP_BUTTON
            else -> null
        }
    }

    /**
     * Check if elements contain password field
     *
     * @param elements List of elements
     * @return true if password field found
     */
    private fun hasPasswordField(elements: List<ElementInfo>): Boolean {
        return elements.any { isPasswordField(it) }
    }

    /**
     * Check if elements contain email field
     *
     * @param elements List of elements
     * @return true if email field found
     */
    private fun hasEmailField(elements: List<ElementInfo>): Boolean {
        return elements.any { isEmailField(it) }
    }

    /**
     * Check if elements contain username field
     *
     * @param elements List of elements
     * @return true if username field found
     */
    private fun hasUsernameField(elements: List<ElementInfo>): Boolean {
        return elements.any { isUsernameField(it) }
    }

    /**
     * Check if elements contain login button
     *
     * @param elements List of elements
     * @return true if login button found
     */
    private fun hasLoginButton(elements: List<ElementInfo>): Boolean {
        return elements.any { isLoginButton(it) }
    }

    /**
     * Check if element is password field
     *
     * @param element Element to check
     * @return true if password field
     */
    private fun isPasswordField(element: ElementInfo): Boolean {
        // Check if marked as password
        if (element.isPassword) {
            return true
        }

        // Check EditText with "password" in text/hint
        if (element.isEditText()) {
            val lowerText = element.text.lowercase()
            val lowerDesc = element.contentDescription.lowercase()
            val lowerResourceId = element.resourceId.lowercase()

            return PASSWORD_PATTERNS.any { pattern ->
                pattern in lowerText || pattern in lowerDesc || pattern in lowerResourceId
            }
        }

        return false
    }

    /**
     * Check if element is email field
     *
     * @param element Element to check
     * @return true if email field
     */
    private fun isEmailField(element: ElementInfo): Boolean {
        if (!element.isEditText()) {
            return false
        }

        val lowerText = element.text.lowercase()
        val lowerDesc = element.contentDescription.lowercase()
        val lowerResourceId = element.resourceId.lowercase()

        return EMAIL_PATTERNS.any { pattern ->
            pattern in lowerText || pattern in lowerDesc || pattern in lowerResourceId
        }
    }

    /**
     * Check if element is username field
     *
     * @param element Element to check
     * @return true if username field
     */
    private fun isUsernameField(element: ElementInfo): Boolean {
        if (!element.isEditText()) {
            return false
        }

        val lowerText = element.text.lowercase()
        val lowerDesc = element.contentDescription.lowercase()
        val lowerResourceId = element.resourceId.lowercase()

        return USERNAME_PATTERNS.any { pattern ->
            pattern in lowerText || pattern in lowerDesc || pattern in lowerResourceId
        }
    }

    /**
     * Check if element is login button
     *
     * @param element Element to check
     * @return true if login button
     */
    private fun isLoginButton(element: ElementInfo): Boolean {
        if (!element.isClickable) {
            return false
        }

        val lowerText = element.text.lowercase()
        val lowerDesc = element.contentDescription.lowercase()
        val lowerResourceId = element.resourceId.lowercase()

        return LOGIN_BUTTON_PATTERNS.any { pattern ->
            pattern in lowerText || pattern in lowerDesc || pattern in lowerResourceId
        }
    }

    /**
     * Check if element is signup button
     *
     * @param element Element to check
     * @return true if signup button
     */
    private fun isSignupButton(element: ElementInfo): Boolean {
        if (!element.isClickable) {
            return false
        }

        val lowerText = element.text.lowercase()
        val lowerDesc = element.contentDescription.lowercase()

        return SIGNUP_BUTTON_PATTERNS.any { pattern ->
            pattern in lowerText || pattern in lowerDesc
        }
    }

    companion object {
        /**
         * Password field patterns
         */
        private val PASSWORD_PATTERNS = listOf(
            "password",
            "passcode",
            "pin"
        )

        /**
         * Email field patterns
         */
        private val EMAIL_PATTERNS = listOf(
            "email",
            "e-mail",
            "mail"
        )

        /**
         * Username field patterns
         */
        private val USERNAME_PATTERNS = listOf(
            "username",
            "user name",
            "user id",
            "userid"
        )

        /**
         * Login button patterns
         */
        private val LOGIN_BUTTON_PATTERNS = listOf(
            "login",
            "log in",
            "sign in",
            "signin"
        )

        /**
         * Signup button patterns
         */
        private val SIGNUP_BUTTON_PATTERNS = listOf(
            "sign up",
            "signup",
            "register",
            "create account"
        )
    }
}
