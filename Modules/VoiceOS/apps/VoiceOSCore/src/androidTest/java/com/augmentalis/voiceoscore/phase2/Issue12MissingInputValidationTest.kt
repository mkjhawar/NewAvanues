/**
 * Issue12MissingInputValidationTest.kt - Tests for voice command input validation
 *
 * Phase 2 - High Priority Issue #12: Missing Input Validation on Voice Commands
 * File: DatabaseCommandHandler.kt:104-106
 *
 * Problem: Extracted appName not validated before database query
 * Solution: Add input validation (format, length, characters)
 *
 * Test Coverage:
 * - App name format validation
 * - Length limits enforcement
 * - Special character filtering
 * - SQL injection prevention
 * - Null/empty input handling
 * - Unicode character handling
 *
 * Run with: ./gradlew :modules:apps:VoiceOSCore:connectedDebugAndroidTest
 */
package com.augmentalis.voiceoscore.phase2

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test suite for voice command input validation
 *
 * Tests verify that all user input is properly validated before
 * being used in database queries or command execution.
 */
@RunWith(AndroidJUnit4::class)
class Issue12MissingInputValidationTest {

    private lateinit var context: Context
    private lateinit var validator: VoiceCommandInputValidator

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        validator = VoiceCommandInputValidator()
    }

    /**
     * TEST 1: Verify valid app name passes validation
     */
    @Test
    fun testValidAppNamePassesValidation() {
        val result = validator.validateAppName("Instagram")
        assertThat(result.isValid).isTrue()
        assertThat(result.sanitizedValue).isEqualTo("Instagram")
    }

    /**
     * TEST 2: Verify null app name fails validation
     */
    @Test
    fun testNullAppNameFailsValidation() {
        val result = validator.validateAppName(null)
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).isEqualTo("App name cannot be null or empty")
    }

    /**
     * TEST 3: Verify empty app name fails validation
     */
    @Test
    fun testEmptyAppNameFailsValidation() {
        val result = validator.validateAppName("")
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).contains("cannot be null or empty")
    }

    /**
     * TEST 4: Verify whitespace-only app name fails validation
     */
    @Test
    fun testWhitespaceOnlyAppNameFailsValidation() {
        val result = validator.validateAppName("   ")
        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).contains("cannot be null or empty")
    }

    /**
     * TEST 5: Verify app name length limits enforced
     */
    @Test
    fun testAppNameLengthLimitEnforced() {
        // Create app name longer than max allowed (100 characters)
        val tooLong = "a".repeat(101)
        val result = validator.validateAppName(tooLong)

        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).contains("too long")
        assertThat(result.errorMessage).contains("100")
    }

    /**
     * TEST 6: Verify maximum valid length accepted
     */
    @Test
    fun testMaximumValidLengthAccepted() {
        val maxLength = "a".repeat(100)
        val result = validator.validateAppName(maxLength)

        assertThat(result.isValid).isTrue()
        assertThat(result.sanitizedValue).hasLength(100)
    }

    /**
     * TEST 7: Verify SQL injection patterns blocked
     */
    @Test
    fun testSQLInjectionPatternsBlocked() {
        val maliciousInputs = listOf(
            "Instagram'; DROP TABLE apps;--",
            "Instagram' OR '1'='1",
            "Instagram'; DELETE FROM apps WHERE '1'='1",
            "Instagram' UNION SELECT * FROM apps--"
        )

        maliciousInputs.forEach { input ->
            val result = validator.validateAppName(input)
            assertThat(result.isValid).isFalse()
            assertThat(result.errorMessage).contains("invalid characters")
        }
    }

    /**
     * TEST 8: Verify special characters sanitized
     */
    @Test
    fun testSpecialCharactersSanitized() {
        val result = validator.validateAppName("Instagram@#$%")

        // Should be valid but sanitized
        assertThat(result.isValid).isTrue()
        assertThat(result.sanitizedValue).isEqualTo("Instagram")
        assertThat(result.sanitizedValue).doesNotContain("@")
        assertThat(result.sanitizedValue).doesNotContain("#")
    }

    /**
     * TEST 9: Verify alphanumeric with spaces allowed
     */
    @Test
    fun testAlphanumericWithSpacesAllowed() {
        val result = validator.validateAppName("My App 123")

        assertThat(result.isValid).isTrue()
        assertThat(result.sanitizedValue).isEqualTo("My App 123")
    }

    /**
     * TEST 10: Verify leading/trailing whitespace trimmed
     */
    @Test
    fun testLeadingTrailingWhitespaceTrimmed() {
        val result = validator.validateAppName("  Instagram  ")

        assertThat(result.isValid).isTrue()
        assertThat(result.sanitizedValue).isEqualTo("Instagram")
    }

    /**
     * TEST 11: Verify Unicode characters handled properly
     */
    @Test
    fun testUnicodeCharactersHandled() {
        val result = validator.validateAppName("微信 WeChat")

        assertThat(result.isValid).isTrue()
        assertThat(result.sanitizedValue).contains("WeChat")
    }

    /**
     * TEST 12: Verify common app name formats accepted
     */
    @Test
    fun testCommonAppNameFormatsAccepted() {
        val validNames = listOf(
            "Instagram",
            "Facebook Messenger",
            "WhatsApp Business",
            "Google Chrome",
            "Microsoft Teams"
        )

        validNames.forEach { name ->
            val result = validator.validateAppName(name)
            assertThat(result.isValid).isTrue()
        }
    }

    /**
     * TEST 13: Verify command text validation
     */
    @Test
    fun testCommandTextValidation() {
        val result = validator.validateCommandText("show app details for Instagram")

        assertThat(result.isValid).isTrue()
        assertThat(result.sanitizedValue).isEqualTo("show app details for Instagram")
    }

    /**
     * TEST 14: Verify excessively long command rejected
     */
    @Test
    fun testExcessivelyLongCommandRejected() {
        val tooLong = "show app details for " + "a".repeat(500)
        val result = validator.validateCommandText(tooLong)

        assertThat(result.isValid).isFalse()
        assertThat(result.errorMessage).contains("too long")
    }

    /**
     * TEST 15: Verify package name validation
     */
    @Test
    fun testPackageNameValidation() {
        val result = validator.validatePackageName("com.instagram.android")

        assertThat(result.isValid).isTrue()
        assertThat(result.sanitizedValue).isEqualTo("com.instagram.android")
    }

    /**
     * TEST 16: Verify invalid package name format rejected
     */
    @Test
    fun testInvalidPackageNameFormatRejected() {
        val invalidPackages = listOf(
            "not-a-package",
            "com..invalid",
            ".com.invalid",
            "com.invalid.",
            "com/invalid/path"
        )

        invalidPackages.forEach { pkg ->
            val result = validator.validatePackageName(pkg)
            assertThat(result.isValid).isFalse()
        }
    }

    /**
     * TEST 17: Verify SQL wildcard escaping
     */
    @Test
    fun testSQLWildcardEscaping() {
        val inputWithWildcards = "App%Name_Test"
        val result = validator.validateAppName(inputWithWildcards)

        // Should escape or remove wildcards
        assertThat(result.sanitizedValue).doesNotContain("%")
        assertThat(result.sanitizedValue).doesNotContain("_")
    }

    /**
     * TEST 18: Verify validation result contains original value
     */
    @Test
    fun testValidationResultContainsOriginalValue() {
        val input = "  Instagram@  "
        val result = validator.validateAppName(input)

        assertThat(result.originalValue).isEqualTo(input)
        assertThat(result.sanitizedValue).isEqualTo("Instagram")
    }
}

/**
 * VoiceCommandInputValidator - Input validation for voice commands
 *
 * Validates and sanitizes all user input before database queries
 * or command execution to prevent SQL injection and handle edge cases.
 */
class VoiceCommandInputValidator {

    companion object {
        private const val MAX_APP_NAME_LENGTH = 100
        private const val MAX_COMMAND_LENGTH = 500
        private const val PACKAGE_NAME_PATTERN = "^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$"

        // SQL injection patterns to block
        private val SQL_INJECTION_PATTERNS = listOf(
            "'", "\"", ";", "--", "/*", "*/",
            "DROP", "DELETE", "UPDATE", "INSERT",
            "UNION", "SELECT", "ALTER", "CREATE"
        )
    }

    /**
     * Validate app name input
     */
    fun validateAppName(appName: String?): ValidationResult {
        // Check null/empty
        if (appName.isNullOrBlank()) {
            return ValidationResult(
                isValid = false,
                errorMessage = "App name cannot be null or empty",
                originalValue = appName,
                sanitizedValue = ""
            )
        }

        val trimmed = appName.trim()

        // Check length
        if (trimmed.length > MAX_APP_NAME_LENGTH) {
            return ValidationResult(
                isValid = false,
                errorMessage = "App name too long (max $MAX_APP_NAME_LENGTH characters)",
                originalValue = appName,
                sanitizedValue = trimmed.take(MAX_APP_NAME_LENGTH)
            )
        }

        // Check for SQL injection patterns
        val upperCased = trimmed.uppercase()
        val hasSQLInjection = SQL_INJECTION_PATTERNS.any { pattern ->
            upperCased.contains(pattern)
        }

        if (hasSQLInjection) {
            return ValidationResult(
                isValid = false,
                errorMessage = "App name contains invalid characters",
                originalValue = appName,
                sanitizedValue = ""
            )
        }

        // Sanitize: remove special characters except alphanumeric, spaces, and common punctuation
        val sanitized = trimmed.replace(Regex("[^a-zA-Z0-9\\s\\-._\\u4e00-\\u9fff]"), "")
            .trim()
            .replace(Regex("\\s+"), " ")  // Normalize whitespace

        // Escape SQL wildcards
        val escaped = sanitized
            .replace("%", "")
            .replace("_", "")

        return ValidationResult(
            isValid = true,
            originalValue = appName,
            sanitizedValue = escaped
        )
    }

    /**
     * Validate command text input
     */
    fun validateCommandText(commandText: String?): ValidationResult {
        if (commandText.isNullOrBlank()) {
            return ValidationResult(
                isValid = false,
                errorMessage = "Command text cannot be null or empty",
                originalValue = commandText,
                sanitizedValue = ""
            )
        }

        val trimmed = commandText.trim()

        if (trimmed.length > MAX_COMMAND_LENGTH) {
            return ValidationResult(
                isValid = false,
                errorMessage = "Command text too long (max $MAX_COMMAND_LENGTH characters)",
                originalValue = commandText,
                sanitizedValue = trimmed.take(MAX_COMMAND_LENGTH)
            )
        }

        // Sanitize similar to app name
        val sanitized = trimmed.replace(Regex("[^a-zA-Z0-9\\s\\-._]"), "")
            .trim()
            .replace(Regex("\\s+"), " ")

        return ValidationResult(
            isValid = true,
            originalValue = commandText,
            sanitizedValue = sanitized
        )
    }

    /**
     * Validate package name format
     */
    fun validatePackageName(packageName: String?): ValidationResult {
        if (packageName.isNullOrBlank()) {
            return ValidationResult(
                isValid = false,
                errorMessage = "Package name cannot be null or empty",
                originalValue = packageName,
                sanitizedValue = ""
            )
        }

        val trimmed = packageName.trim()

        // Check package name format (e.g., com.example.app)
        val isValidFormat = trimmed.matches(Regex(PACKAGE_NAME_PATTERN))

        if (!isValidFormat) {
            return ValidationResult(
                isValid = false,
                errorMessage = "Invalid package name format",
                originalValue = packageName,
                sanitizedValue = trimmed
            )
        }

        return ValidationResult(
            isValid = true,
            originalValue = packageName,
            sanitizedValue = trimmed
        )
    }
}

/**
 * Validation result data class
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val originalValue: String?,
    val sanitizedValue: String
)
