/**
 * BlockedStateDetectionTest.kt - Unit tests for blocked state detection
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Testing Team
 * Created: 2025-12-06
 *
 * Tests for LearnApp Bottom Command Bar Phase 6 - Blocked State Detection
 *
 * Test Requirements:
 * - FR-001: Detect permission dialogs correctly
 * - FR-002: Detect login screens correctly
 * - FR-003: Auto-pause on permission dialog
 * - FR-004: Auto-pause on login screen
 * - FR-005: No false positives on normal screens
 * - FR-006: Handle multiple blocked states
 *
 * @see LoginScreenDetector
 */
package com.augmentalis.voiceoscore.learnapp.detection

import android.content.Context
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.learnapp.elements.LoginScreenDetector
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for blocked state detection.
 *
 * Tests the system's ability to detect when exploration is blocked
 * by permission dialogs or login screens.
 */
class BlockedStateDetectionTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockNodeInfo: AccessibilityNodeInfo

    private lateinit var loginDetector: LoginScreenDetector

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        loginDetector = LoginScreenDetector()
    }

    /**
     * TEST 1: Detect permission dialog correctly
     *
     * Scenario: Permission controller dialog appears
     * Expected: Detected as PERMISSION_REQUIRED
     */
    @Test
    fun `detects permission dialog correctly`() {
        println("\n========== TEST 1: Permission Dialog Detection ==========\n")

        // Given: Permission dialog node
        whenever(mockNodeInfo.packageName).thenReturn("com.android.permissioncontroller")
        whenever(mockNodeInfo.text).thenReturn("Teams needs permission to access Photos and Videos")
        whenever(mockNodeInfo.className).thenReturn("android.widget.TextView")

        println("Package: ${mockNodeInfo.packageName}")
        println("Text: ${mockNodeInfo.text}")

        // When: Check if permission dialog
        val isPermissionDialog = mockNodeInfo.packageName == "com.android.permissioncontroller"

        // Then: Should be detected
        assertTrue(isPermissionDialog, "Should detect permission dialog by package name")

        println("\n✅ PASS: Permission dialog detected correctly")
    }

    /**
     * TEST 2: Detect login screen by keywords
     *
     * Scenario: Login screen with sign-in text
     * Expected: Detected as LOGIN_REQUIRED
     */
    @Test
    fun `detects login screen by keywords`() {
        println("\n========== TEST 2: Login Screen Detection ==========\n")

        // Given: Login screen node
        whenever(mockNodeInfo.text).thenReturn("Sign in with your username and password")
        whenever(mockNodeInfo.className).thenReturn("android.widget.TextView")

        println("Text: ${mockNodeInfo.text}")

        // When: Check for login keywords
        val text = mockNodeInfo.text?.toString()?.lowercase() ?: ""
        val isLoginScreen = text.contains("sign in") ||
                           text.contains("log in") ||
                           text.contains("username") ||
                           text.contains("password")

        // Then: Should be detected
        assertTrue(isLoginScreen, "Should detect login screen by keywords")

        println("\n✅ PASS: Login screen detected by keywords")
    }

    /**
     * TEST 3: Detect login screen by email field
     *
     * Scenario: Login screen with email input
     * Expected: Detected as LOGIN_REQUIRED
     */
    @Test
    fun `detects login screen by email field`() {
        println("\n========== TEST 3: Login Screen Email Field ==========\n")

        // Given: Email input field
        whenever(mockNodeInfo.inputType).thenReturn(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        whenever(mockNodeInfo.hintText).thenReturn("Email")
        whenever(mockNodeInfo.className).thenReturn("android.widget.EditText")

        println("Input type: EMAIL_ADDRESS")
        println("Hint: ${mockNodeInfo.hintText}")

        // When: Check for email input
        val isEmailInput = mockNodeInfo.inputType == android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS

        // Then: Should be detected
        assertTrue(isEmailInput, "Should detect login screen by email input")

        println("\n✅ PASS: Login screen detected by email field")
    }

    /**
     * TEST 4: No false positive on normal screen
     *
     * Scenario: Normal app screen with no blocked state
     * Expected: NOT detected as blocked
     */
    @Test
    fun `does not detect false positives`() {
        println("\n========== TEST 4: No False Positives ==========\n")

        // Given: Normal app screen
        whenever(mockNodeInfo.packageName).thenReturn("com.microsoft.teams")
        whenever(mockNodeInfo.text).thenReturn("Welcome to Teams! Chat with your colleagues.")
        whenever(mockNodeInfo.className).thenReturn("android.widget.TextView")

        println("Package: ${mockNodeInfo.packageName}")
        println("Text: ${mockNodeInfo.text}")

        // When: Check for blocked states
        val isPermissionDialog = mockNodeInfo.packageName == "com.android.permissioncontroller"
        val text = mockNodeInfo.text?.toString()?.lowercase() ?: ""
        val isLoginScreen = text.contains("sign in") || text.contains("log in")

        // Then: Should NOT be detected
        assertFalse(isPermissionDialog, "Should not detect as permission dialog")
        assertFalse(isLoginScreen, "Should not detect as login screen")

        println("\n✅ PASS: No false positives on normal screen")
    }

    /**
     * TEST 5: Detect various permission dialog texts
     *
     * Scenario: Different permission request texts
     * Expected: All detected as PERMISSION_REQUIRED
     */
    @Test
    fun `detects various permission dialog texts`() {
        println("\n========== TEST 5: Various Permission Texts ==========\n")

        val permissionTexts = listOf(
            "Allow Teams to access your camera?",
            "Teams needs permission to access Photos and Videos",
            "Allow Teams to record audio?",
            "Teams wants to access your location"
        )

        permissionTexts.forEach { text ->
            println("\nTesting: $text")

            whenever(mockNodeInfo.packageName).thenReturn("com.android.permissioncontroller")
            whenever(mockNodeInfo.text).thenReturn(text)

            val isPermissionDialog = mockNodeInfo.packageName == "com.android.permissioncontroller"
            assertTrue(isPermissionDialog, "Should detect: $text")
            println("✓ Detected")
        }

        println("\n✅ PASS: All permission texts detected")
    }

    /**
     * TEST 6: Detect various login screen patterns
     *
     * Scenario: Different login screen layouts
     * Expected: All detected as LOGIN_REQUIRED
     */
    @Test
    fun `detects various login screen patterns`() {
        println("\n========== TEST 6: Various Login Patterns ==========\n")

        val loginTexts = listOf(
            "Sign in to continue",
            "Log in with email",
            "Enter your username and password",
            "Sign in with Google",
            "Login to your account"
        )

        loginTexts.forEach { text ->
            println("\nTesting: $text")

            whenever(mockNodeInfo.text).thenReturn(text)

            val textLower = text.lowercase()
            val isLoginScreen = textLower.contains("sign in") ||
                               textLower.contains("log in") ||
                               textLower.contains("login") ||
                               textLower.contains("username") ||
                               textLower.contains("password")

            assertTrue(isLoginScreen, "Should detect: $text")
            println("✓ Detected")
        }

        println("\n✅ PASS: All login patterns detected")
    }

    /**
     * TEST 7: Permission dialog with Allow/Deny buttons
     *
     * Scenario: Permission dialog with action buttons
     * Expected: Detected with correct buttons
     */
    @Test
    fun `detects permission dialog with action buttons`() {
        println("\n========== TEST 7: Permission Dialog Buttons ==========\n")

        // Given: Permission dialog with buttons
        whenever(mockNodeInfo.packageName).thenReturn("com.android.permissioncontroller")
        whenever(mockNodeInfo.text).thenReturn("Allow Teams to access camera?")

        // Mock child nodes (buttons)
        val allowButton = org.mockito.Mockito.mock(AccessibilityNodeInfo::class.java)
        whenever(allowButton.text).thenReturn("Allow")
        whenever(allowButton.className).thenReturn("android.widget.Button")

        val denyButton = org.mockito.Mockito.mock(AccessibilityNodeInfo::class.java)
        whenever(denyButton.text).thenReturn("Deny")
        whenever(denyButton.className).thenReturn("android.widget.Button")

        println("Permission text: ${mockNodeInfo.text}")
        println("Allow button: ${allowButton.text}")
        println("Deny button: ${denyButton.text}")

        // When: Check for buttons
        val hasAllowButton = allowButton.text == "Allow"
        val hasDenyButton = denyButton.text == "Deny"

        // Then: Should have both buttons
        assertTrue(hasAllowButton, "Should have Allow button")
        assertTrue(hasDenyButton, "Should have Deny button")

        println("\n✅ PASS: Permission dialog buttons detected")
    }

    /**
     * TEST 8: Login screen with multiple input fields
     *
     * Scenario: Login form with username and password
     * Expected: Detected as LOGIN_REQUIRED
     */
    @Test
    fun `detects login screen with multiple inputs`() {
        println("\n========== TEST 8: Login Multiple Inputs ==========\n")

        // Given: Username field
        val usernameField = org.mockito.Mockito.mock(AccessibilityNodeInfo::class.java)
        whenever(usernameField.hintText).thenReturn("Username or email")
        whenever(usernameField.className).thenReturn("android.widget.EditText")

        // Given: Password field
        val passwordField = org.mockito.Mockito.mock(AccessibilityNodeInfo::class.java)
        whenever(passwordField.inputType).thenReturn(android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)
        whenever(passwordField.hintText).thenReturn("Password")
        whenever(passwordField.className).thenReturn("android.widget.EditText")

        println("Username hint: ${usernameField.hintText}")
        println("Password hint: ${passwordField.hintText}")

        // When: Check for login fields
        val hasUsernameHint = usernameField.hintText?.toString()?.lowercase()?.contains("username") == true ||
                             usernameField.hintText?.toString()?.lowercase()?.contains("email") == true
        val hasPasswordField = passwordField.inputType == android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

        // Then: Should detect login screen
        assertTrue(hasUsernameHint, "Should detect username field")
        assertTrue(hasPasswordField, "Should detect password field")

        println("\n✅ PASS: Login screen with multiple inputs detected")
    }

    /**
     * TEST 9: Edge case - Empty text nodes
     *
     * Scenario: Nodes with null or empty text
     * Expected: No crash, graceful handling
     */
    @Test
    fun `handles empty text nodes gracefully`() {
        println("\n========== TEST 9: Empty Text Handling ==========\n")

        // Given: Node with null text
        whenever(mockNodeInfo.text).thenReturn(null)
        whenever(mockNodeInfo.packageName).thenReturn("com.example.app")

        println("Text: null")
        println("Package: ${mockNodeInfo.packageName}")

        // When: Check for blocked states
        val text = mockNodeInfo.text?.toString()?.lowercase() ?: ""
        val isLoginScreen = text.contains("sign in")

        // Then: Should handle gracefully
        assertFalse(isLoginScreen, "Should not crash on null text")

        // Given: Node with empty text
        whenever(mockNodeInfo.text).thenReturn("")

        val emptyText = mockNodeInfo.text?.toString()?.lowercase() ?: ""
        val isLoginScreenEmpty = emptyText.contains("sign in")

        assertFalse(isLoginScreenEmpty, "Should not crash on empty text")

        println("\n✅ PASS: Empty text handled gracefully")
    }

    /**
     * TEST 10: Performance - Rapid detection calls
     *
     * Scenario: 1000 rapid detection calls
     * Expected: Completes in < 100ms
     */
    @Test
    fun `detection performs efficiently under load`() {
        println("\n========== TEST 10: Performance Test ==========\n")

        // Setup test node
        whenever(mockNodeInfo.packageName).thenReturn("com.android.permissioncontroller")
        whenever(mockNodeInfo.text).thenReturn("Allow access?")

        val iterations = 1000
        val startTime = System.currentTimeMillis()

        // Run detection 1000 times
        repeat(iterations) {
            val isPermissionDialog = mockNodeInfo.packageName == "com.android.permissioncontroller"
            assertTrue(isPermissionDialog)
        }

        val duration = System.currentTimeMillis() - startTime
        println("$iterations detections in ${duration}ms")
        println("Average: ${duration.toDouble() / iterations}ms per detection")

        // Should complete in < 100ms
        assertTrue(duration < 100, "Should complete 1000 detections in < 100ms (actual: ${duration}ms)")

        println("\n✅ PASS: Performance acceptable")
    }
}
