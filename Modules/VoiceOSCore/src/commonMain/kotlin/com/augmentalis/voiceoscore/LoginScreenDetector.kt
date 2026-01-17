/*
 * Copyright (c) 2026 Augmentalis. All rights reserved.
 *
 * LoginScreenDetector.kt - Detects login/authentication screens
 *
 * Part of VoiceOSCoreNG Safety System.
 * Identifies login screens to:
 * 1. Prompt user for manual login (never capture credentials)
 * 2. Skip authentication-related elements
 * 3. Log LGN event for AVU export
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Migrated to KMP: 2026-01-16
 * Related: VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md Section 5.4
 *
 * @since 2.0.0 (LearnApp Dual-Edition)
 */

package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.ElementInfo

/**
 * Type of login screen detected.
 */
enum class LoginType(val ipcCode: String, val description: String) {
    /**
     * Standard username/password form.
     */
    STANDARD("STD", "Standard login form"),

    /**
     * OAuth/SSO provider selection (Google, Facebook, Apple).
     */
    OAUTH("OAU", "OAuth provider selection"),

    /**
     * Phone number + OTP verification.
     */
    PHONE_OTP("OTP", "Phone OTP verification"),

    /**
     * Email magic link.
     */
    MAGIC_LINK("MGL", "Magic link login"),

    /**
     * PIN entry screen.
     */
    PIN("PIN", "PIN entry"),

    /**
     * Biometric prompt.
     */
    BIOMETRIC("BIO", "Biometric authentication"),

    /**
     * Two-factor authentication.
     */
    TWO_FACTOR("2FA", "Two-factor authentication"),

    /**
     * Account creation/signup.
     */
    SIGNUP("SGN", "Account signup"),

    /**
     * Password reset.
     */
    PASSWORD_RESET("RST", "Password reset");

    companion object {
        fun fromIpcCode(code: String): LoginType? {
            return entries.find { it.ipcCode.equals(code, ignoreCase = true) }
        }
    }
}

/**
 * Result of login screen detection.
 *
 * @property isLoginScreen Whether screen is detected as login
 * @property loginType Type of login detected
 * @property confidence Detection confidence (0.0 - 1.0)
 * @property indicators Which indicators triggered detection
 * @property authElements Elements related to authentication
 */
data class LoginDetectionResult(
    val isLoginScreen: Boolean,
    val loginType: LoginType?,
    val confidence: Float,
    val indicators: List<String>,
    val authElements: List<ElementInfo>
) {
    /**
     * Generate LGN IPC line for AVU export.
     *
     * Format: LGN:package:screen_hash:login_type
     */
    fun toLgnLine(packageName: String, screenHash: String): String? {
        if (!isLoginScreen || loginType == null) return null
        return "LGN:$packageName:$screenHash:${loginType.name}"
    }

    companion object {
        fun notLogin() = LoginDetectionResult(
            isLoginScreen = false,
            loginType = null,
            confidence = 0f,
            indicators = emptyList(),
            authElements = emptyList()
        )
    }
}

/**
 * Login Screen Detector - Identifies authentication screens
 *
 * Uses multiple signals to detect login screens:
 * 1. Password input fields
 * 2. Login-related labels/text
 * 3. OAuth provider buttons
 * 4. Activity class name patterns
 */
object LoginScreenDetector {

    // ============================================================
    // Password field indicators
    // ============================================================
    val PASSWORD_HINTS = setOf(
        "password",
        "pass",
        "pwd",
        "passcode",
        "pin",
        "secret"
    )

    // ============================================================
    // Username/email field indicators
    // ============================================================
    val USERNAME_HINTS = setOf(
        "username",
        "user",
        "email",
        "e-mail",
        "phone",
        "mobile",
        "account"
    )

    // ============================================================
    // Login button indicators
    // ============================================================
    val LOGIN_BUTTON_LABELS = setOf(
        "log in",
        "login",
        "sign in",
        "signin",
        "continue",
        "submit",
        "enter",
        "unlock"
    )

    // ============================================================
    // OAuth provider indicators
    // ============================================================
    val OAUTH_PROVIDERS = setOf(
        "google",
        "facebook",
        "apple",
        "microsoft",
        "twitter",
        "github",
        "linkedin",
        "amazon"
    )

    val OAUTH_PATTERNS = listOf(
        Regex("continue with.*", RegexOption.IGNORE_CASE),
        Regex("sign in with.*", RegexOption.IGNORE_CASE),
        Regex("log in with.*", RegexOption.IGNORE_CASE)
    )

    // ============================================================
    // Activity class name patterns
    // ============================================================
    val LOGIN_ACTIVITY_PATTERNS = listOf(
        Regex(".*Login.*", RegexOption.IGNORE_CASE),
        Regex(".*SignIn.*", RegexOption.IGNORE_CASE),
        Regex(".*Auth.*", RegexOption.IGNORE_CASE),
        Regex(".*Account.*", RegexOption.IGNORE_CASE),
        Regex(".*Welcome.*", RegexOption.IGNORE_CASE),
        Regex(".*Onboard.*", RegexOption.IGNORE_CASE)
    )

    // ============================================================
    // Signup indicators
    // ============================================================
    val SIGNUP_LABELS = setOf(
        "sign up",
        "signup",
        "create account",
        "register",
        "new account",
        "get started",
        "join"
    )

    // ============================================================
    // OTP/verification indicators
    // ============================================================
    val OTP_LABELS = setOf(
        "otp",
        "verification code",
        "verify",
        "code sent",
        "enter code",
        "6-digit",
        "4-digit",
        "sms code"
    )

    // ============================================================
    // Password reset indicators
    // ============================================================
    val RESET_LABELS = setOf(
        "forgot password",
        "reset password",
        "recover",
        "forgot",
        "trouble signing in"
    )

    /**
     * Detect if screen is a login/authentication screen.
     *
     * @param elements All elements on screen
     * @param activityName Current activity class name (optional)
     * @return Detection result with type and confidence
     */
    fun detectLoginScreen(
        elements: List<ElementInfo>,
        activityName: String? = null
    ): LoginDetectionResult {
        val indicators = mutableListOf<String>()
        val authElements = mutableListOf<ElementInfo>()
        var confidence = 0f

        // Check activity name
        if (activityName != null) {
            val activityMatch = LOGIN_ACTIVITY_PATTERNS.any { it.matches(activityName) }
            if (activityMatch) {
                indicators.add("activity:$activityName")
                confidence += 0.2f
            }
        }

        // Analyze elements
        var hasPasswordField = false
        var hasUsernameField = false
        var hasLoginButton = false
        var hasOAuthButton = false
        var hasOtpField = false
        var hasSignupLink = false
        var hasResetLink = false

        for (element in elements) {
            val label = element.voiceLabel.lowercase()
            val resourceId = element.resourceId.lowercase()

            // Password field detection (check class name and resource ID)
            if (isPasswordField(element)) {
                hasPasswordField = true
                indicators.add("password_field:${element.stableId()}")
                authElements.add(element)
                confidence += 0.3f
            }

            // Check labels and resource IDs
            when {
                // Password hints (even if not marked as password)
                PASSWORD_HINTS.any { label.contains(it) || resourceId.contains(it) } -> {
                    if (isEditTextField(element)) {
                        hasPasswordField = true
                        indicators.add("password_hint:$label")
                        authElements.add(element)
                        confidence += 0.25f
                    }
                }

                // Username/email field
                USERNAME_HINTS.any { label.contains(it) || resourceId.contains(it) } -> {
                    if (isEditTextField(element)) {
                        hasUsernameField = true
                        indicators.add("username_field:$label")
                        authElements.add(element)
                        confidence += 0.15f
                    }
                }

                // Login button
                LOGIN_BUTTON_LABELS.any { label.contains(it) } -> {
                    if (isButton(element) || element.isClickable) {
                        hasLoginButton = true
                        indicators.add("login_button:$label")
                        authElements.add(element)
                        confidence += 0.2f
                    }
                }

                // OAuth providers
                OAUTH_PROVIDERS.any { label.contains(it) } ||
                OAUTH_PATTERNS.any { it.matches(label) } -> {
                    hasOAuthButton = true
                    indicators.add("oauth:$label")
                    authElements.add(element)
                    confidence += 0.25f
                }

                // OTP/verification
                OTP_LABELS.any { label.contains(it) } -> {
                    hasOtpField = true
                    indicators.add("otp:$label")
                    authElements.add(element)
                    confidence += 0.3f
                }

                // Signup link
                SIGNUP_LABELS.any { label.contains(it) } -> {
                    hasSignupLink = true
                    indicators.add("signup:$label")
                    authElements.add(element)
                    confidence += 0.1f
                }

                // Password reset
                RESET_LABELS.any { label.contains(it) } -> {
                    hasResetLink = true
                    indicators.add("reset:$label")
                    authElements.add(element)
                    confidence += 0.1f
                }
            }
        }

        // Cap confidence at 1.0
        confidence = confidence.coerceAtMost(1.0f)

        // Determine login type
        val loginType = when {
            hasOtpField -> LoginType.PHONE_OTP
            hasOAuthButton && !hasPasswordField -> LoginType.OAUTH
            hasPasswordField && hasUsernameField -> LoginType.STANDARD
            hasPasswordField -> LoginType.PIN
            hasSignupLink && !hasLoginButton -> LoginType.SIGNUP
            hasResetLink -> LoginType.PASSWORD_RESET
            hasOAuthButton -> LoginType.OAUTH
            confidence >= 0.3f -> LoginType.STANDARD
            else -> null
        }

        // Determine if this is a login screen
        val isLogin = confidence >= 0.4f || hasPasswordField || hasOAuthButton

        return LoginDetectionResult(
            isLoginScreen = isLogin,
            loginType = if (isLogin) loginType else null,
            confidence = confidence,
            indicators = indicators,
            authElements = authElements
        )
    }

    /**
     * Check if a single element is auth-related.
     *
     * @param element Element to check
     * @return true if element is authentication-related
     */
    fun isAuthElement(element: ElementInfo): Boolean {
        if (isPasswordField(element)) return true

        val label = element.voiceLabel.lowercase()
        val resourceId = element.resourceId.lowercase()

        return PASSWORD_HINTS.any { label.contains(it) || resourceId.contains(it) } ||
               USERNAME_HINTS.any { label.contains(it) || resourceId.contains(it) } ||
               LOGIN_BUTTON_LABELS.any { label.contains(it) } ||
               OTP_LABELS.any { label.contains(it) }
    }

    /**
     * Get user-facing message for login screen.
     *
     * @param loginType Type of login detected
     * @return Message to show user
     */
    fun getLoginPromptMessage(loginType: LoginType): String {
        return when (loginType) {
            LoginType.STANDARD -> "Login screen detected. Please sign in manually, then resume exploration."
            LoginType.OAUTH -> "Sign-in options detected. Please authenticate manually."
            LoginType.PHONE_OTP -> "Phone verification required. Please enter your code manually."
            LoginType.PIN -> "PIN entry required. Please enter your PIN manually."
            LoginType.BIOMETRIC -> "Biometric authentication required. Please authenticate manually."
            LoginType.TWO_FACTOR -> "Two-factor authentication required. Please complete verification."
            LoginType.SIGNUP -> "Account creation screen detected. Please sign up manually if needed."
            LoginType.PASSWORD_RESET -> "Password reset screen detected. Complete manually if needed."
            LoginType.MAGIC_LINK -> "Magic link login detected. Check your email and sign in manually."
        }
    }

    // ============================================================
    // KMP-compatible helper functions
    // ============================================================

    /**
     * Check if element is a password field.
     * Uses class name and resource ID heuristics.
     */
    private fun isPasswordField(element: ElementInfo): Boolean {
        val className = element.className.lowercase()
        val resourceId = element.resourceId.lowercase()

        return className.contains("password") ||
               resourceId.contains("password") ||
               resourceId.contains("pwd")
    }

    /**
     * Check if element is an EditText field.
     */
    private fun isEditTextField(element: ElementInfo): Boolean {
        val className = element.className.lowercase()
        return className.contains("edittext") ||
               className.contains("textinput") ||
               className.contains("textfield")
    }

    /**
     * Check if element is a Button.
     */
    private fun isButton(element: ElementInfo): Boolean {
        val className = element.className.lowercase()
        return className.contains("button")
    }
}
