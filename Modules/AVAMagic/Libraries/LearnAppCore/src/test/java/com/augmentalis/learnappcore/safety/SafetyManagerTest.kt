/**
 * SafetyManagerTest.kt - Unit tests for SafetyManager
 *
 * Tests safety checks, filtering, and integration with safety subsystems:
 * - Do Not Click filtering
 * - Login screen detection
 * - Password field detection
 * - Element filtering
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: Phase 1 Architecture Improvement Plan
 *
 * @since 2.0.0 (LearnApp Dual-Edition)
 */

package com.augmentalis.learnappcore.safety

import android.graphics.Rect
import com.augmentalis.learnappcore.models.ElementInfo
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for SafetyManager
 *
 * Tests:
 * - Do Not Click list checking
 * - Login screen detection
 * - Password field detection
 * - Element filtering by safety
 * - Safety callbacks
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SafetyManagerTest {

    private lateinit var mockCallback: SafetyCallback
    private lateinit var safetyManager: SafetyManager

    @Before
    fun setup() {
        mockCallback = mockk(relaxed = true)
        safetyManager = SafetyManager(mockCallback)
    }

    // ============================================================
    // Do Not Click Tests
    // ============================================================

    @Test
    fun checkElement_OnDNCList_ReturnsUnsafe() {
        // Given: Element with "Call" keyword (Do Not Click)
        val element = ElementInfo(
            className = "android.widget.Button",
            text = "Call Now",
            contentDescription = "",
            resourceId = "",
            isClickable = true,
            isEnabled = true,
            bounds = Rect(0, 0, 100, 100)
        )

        // When: Check element safety
        val result = safetyManager.checkElement(element)

        // Then: Element marked as unsafe
        assertFalse(result.isSafe)
        assertEquals(SafetyCategory.DO_NOT_CLICK, result.category)
        assertEquals(SafetyRecommendation.LOG_ONLY, result.recommendation)
        assertNotNull(result.reason)
        assertTrue(result.reason!!.contains("Do Not Click"))

        // Verify callback invoked
        verify { mockCallback.onDangerousElement(element, DoNotClickReason.CALL_ACTION) }
    }

    @Test
    fun checkElement_PostButton_ReturnsUnsafe() {
        // Given: Element with "Post" keyword (Do Not Click)
        val element = ElementInfo(
            className = "android.widget.Button",
            text = "Post",
            contentDescription = "",
            resourceId = "",
            isClickable = true,
            isEnabled = true,
            bounds = Rect(0, 0, 100, 100)
        )

        // When: Check element safety
        val result = safetyManager.checkElement(element)

        // Then: Element marked as unsafe
        assertFalse(result.isSafe)
        assertEquals(SafetyCategory.DO_NOT_CLICK, result.category)
        verify { mockCallback.onDangerousElement(element, DoNotClickReason.CONTENT_CREATION) }
    }

    @Test
    fun checkElement_SendButton_ReturnsUnsafe() {
        // Given: Element with "Send" keyword (Do Not Click)
        val element = ElementInfo(
            className = "android.widget.ImageButton",
            text = "",
            contentDescription = "Send message",
            resourceId = "com.example:id/btn_send",
            isClickable = true,
            isEnabled = true,
            bounds = Rect(0, 0, 100, 100)
        )

        // When: Check element safety
        val result = safetyManager.checkElement(element)

        // Then: Element marked as unsafe
        assertFalse(result.isSafe)
        assertEquals(SafetyCategory.DO_NOT_CLICK, result.category)
    }

    @Test
    fun checkElement_LogoutButton_ReturnsUnsafe() {
        // Given: Element with "Logout" keyword (Do Not Click)
        val element = ElementInfo(
            className = "android.widget.Button",
            text = "Sign Out",
            contentDescription = "",
            resourceId = "",
            isClickable = true,
            isEnabled = true,
            bounds = Rect(0, 0, 100, 100)
        )

        // When: Check element safety
        val result = safetyManager.checkElement(element)

        // Then: Element marked as unsafe
        assertFalse(result.isSafe)
        assertEquals(SafetyCategory.DO_NOT_CLICK, result.category)
        verify { mockCallback.onDangerousElement(element, DoNotClickReason.EXIT_ACTION) }
    }

    @Test
    fun checkElement_PaymentButton_ReturnsUnsafe() {
        // Given: Element with "Pay" keyword (Do Not Click)
        val element = ElementInfo(
            className = "android.widget.Button",
            text = "Pay Now",
            contentDescription = "",
            resourceId = "",
            isClickable = true,
            isEnabled = true,
            bounds = Rect(0, 0, 100, 100)
        )

        // When: Check element safety
        val result = safetyManager.checkElement(element)

        // Then: Element marked as unsafe
        assertFalse(result.isSafe)
        assertEquals(SafetyCategory.DO_NOT_CLICK, result.category)
        verify { mockCallback.onDangerousElement(element, DoNotClickReason.PAYMENT_ACTION) }
    }

    // ============================================================
    // Login Screen Tests
    // ============================================================

    @Test
    fun checkElement_OnLoginScreen_ReturnsUnsafe() {
        // Given: Login screen with auth elements
        val passwordField = ElementInfo(
            className = "android.widget.EditText",
            text = "",
            contentDescription = "Password",
            resourceId = "",
            isClickable = false,
            isEnabled = true,
            isPassword = true,
            bounds = Rect(0, 100, 500, 200)
        )

        val loginButton = ElementInfo(
            className = "android.widget.Button",
            text = "Login",
            contentDescription = "",
            resourceId = "",
            isClickable = true,
            isEnabled = true,
            bounds = Rect(0, 300, 500, 400)
        )

        // When: Update screen context with login elements
        safetyManager.updateScreenContext(
            packageName = "com.example.app",
            screenHash = "login_screen",
            activityName = "LoginActivity",
            elements = listOf(passwordField, loginButton)
        )

        // Then: Login screen detected
        val loginResult = safetyManager.isLoginScreen()
        assertTrue(loginResult.isLoginScreen)
        verify { mockCallback.onLoginDetected(any(), any()) }

        // When: Check auth element on login screen
        val checkResult = safetyManager.checkElement(loginButton)

        // Then: Auth element marked unsafe
        assertFalse(checkResult.isSafe)
        assertEquals(SafetyCategory.LOGIN_SCREEN, checkResult.category)
        assertEquals(SafetyRecommendation.PROMPT_USER, checkResult.recommendation)
    }

    // ============================================================
    // Password Field Tests
    // ============================================================

    @Test
    fun checkElement_PasswordField_ReturnsSkip() {
        // Given: Password field element
        val element = ElementInfo(
            className = "android.widget.EditText",
            text = "",
            contentDescription = "Password",
            resourceId = "",
            isClickable = false,
            isEnabled = true,
            isPassword = true,
            bounds = Rect(0, 0, 500, 100)
        )

        // When: Check element safety
        val result = safetyManager.checkElement(element)

        // Then: Element marked to skip
        assertFalse(result.isSafe)
        assertEquals(SafetyCategory.PASSWORD_FIELD, result.category)
        assertEquals(SafetyRecommendation.SKIP_ELEMENT, result.recommendation)
    }

    // ============================================================
    // Safe Element Tests
    // ============================================================

    @Test
    fun checkElement_Safe_ReturnsSafe() {
        // Given: Safe element (no dangerous keywords or patterns)
        val element = ElementInfo(
            className = "android.widget.Button",
            text = "View Profile",
            contentDescription = "",
            resourceId = "",
            isClickable = true,
            isEnabled = true,
            bounds = Rect(0, 0, 100, 100)
        )

        // When: Check element safety
        val result = safetyManager.checkElement(element)

        // Then: Element marked as safe
        assertTrue(result.isSafe)
    }

    @Test
    fun checkElement_SafeImageButton_ReturnsSafe() {
        // Given: Safe ImageButton
        val element = ElementInfo(
            className = "android.widget.ImageButton",
            text = "",
            contentDescription = "Settings",
            resourceId = "com.example:id/btn_settings",
            isClickable = true,
            isEnabled = true,
            bounds = Rect(0, 0, 100, 100)
        )

        // When: Check element safety
        val result = safetyManager.checkElement(element)

        // Then: Element marked as safe
        assertTrue(result.isSafe)
    }

    // ============================================================
    // Element Filtering Tests
    // ============================================================

    @Test
    fun filterElements_MixedSafety_SeparatesCorrectly() {
        // Given: Mix of safe and unsafe elements
        val safeButton1 = ElementInfo(
            className = "android.widget.Button",
            text = "View",
            isClickable = true,
            bounds = Rect(0, 0, 100, 100)
        )

        val callButton = ElementInfo(
            className = "android.widget.Button",
            text = "Call",
            isClickable = true,
            bounds = Rect(100, 0, 200, 100)
        )

        val safeButton2 = ElementInfo(
            className = "android.widget.Button",
            text = "Edit",
            isClickable = true,
            bounds = Rect(200, 0, 300, 100)
        )

        val postButton = ElementInfo(
            className = "android.widget.Button",
            text = "Post",
            isClickable = true,
            bounds = Rect(300, 0, 400, 100)
        )

        val passwordField = ElementInfo(
            className = "android.widget.EditText",
            text = "",
            contentDescription = "Password",
            isPassword = true,
            bounds = Rect(0, 100, 400, 200)
        )

        val elements = listOf(safeButton1, callButton, safeButton2, postButton, passwordField)

        // When: Filter elements
        val (safe, unsafe) = safetyManager.filterElements(elements)

        // Then: Elements separated correctly
        assertEquals(2, safe.size)  // View, Edit
        assertEquals(3, unsafe.size)  // Call, Post, Password

        // Verify safe elements
        assertTrue(safe.contains(safeButton1))
        assertTrue(safe.contains(safeButton2))

        // Verify unsafe elements
        val unsafeElements = unsafe.map { it.first }
        assertTrue(unsafeElements.contains(callButton))
        assertTrue(unsafeElements.contains(postButton))
        assertTrue(unsafeElements.contains(passwordField))

        // Verify unsafe reasons
        val callReason = unsafe.find { it.first == callButton }?.second
        assertEquals(SafetyCategory.DO_NOT_CLICK, callReason?.category)

        val postReason = unsafe.find { it.first == postButton }?.second
        assertEquals(SafetyCategory.DO_NOT_CLICK, postReason?.category)

        val passwordReason = unsafe.find { it.first == passwordField }?.second
        assertEquals(SafetyCategory.PASSWORD_FIELD, passwordReason?.category)
    }

    @Test
    fun filterElements_AllSafe_ReturnsAllSafe() {
        // Given: All safe elements
        val elements = listOf(
            ElementInfo(
                className = "android.widget.Button",
                text = "View",
                isClickable = true,
                bounds = Rect(0, 0, 100, 100)
            ),
            ElementInfo(
                className = "android.widget.Button",
                text = "Edit",
                isClickable = true,
                bounds = Rect(100, 0, 200, 100)
            ),
            ElementInfo(
                className = "android.widget.Button",
                text = "Settings",
                isClickable = true,
                bounds = Rect(200, 0, 300, 100)
            )
        )

        // When: Filter elements
        val (safe, unsafe) = safetyManager.filterElements(elements)

        // Then: All elements safe
        assertEquals(3, safe.size)
        assertEquals(0, unsafe.size)
    }

    @Test
    fun filterElements_AllUnsafe_ReturnsAllUnsafe() {
        // Given: All unsafe elements
        val elements = listOf(
            ElementInfo(
                className = "android.widget.Button",
                text = "Call",
                isClickable = true,
                bounds = Rect(0, 0, 100, 100)
            ),
            ElementInfo(
                className = "android.widget.Button",
                text = "Post",
                isClickable = true,
                bounds = Rect(100, 0, 200, 100)
            ),
            ElementInfo(
                className = "android.widget.EditText",
                text = "",
                isPassword = true,
                bounds = Rect(0, 100, 400, 200)
            )
        )

        // When: Filter elements
        val (safe, unsafe) = safetyManager.filterElements(elements)

        // Then: All elements unsafe
        assertEquals(0, safe.size)
        assertEquals(3, unsafe.size)
    }

    // ============================================================
    // Screen Visit Tracking Tests
    // ============================================================

    @Test
    fun updateScreenContext_TracksVisits() {
        // Given: Screen context
        val screenHash = "home_screen"
        val elements = emptyList<ElementInfo>()

        // When: Visit screen multiple times
        safetyManager.updateScreenContext("com.example.app", screenHash, null, elements)
        safetyManager.updateScreenContext("com.example.app", screenHash, null, elements)
        safetyManager.updateScreenContext("com.example.app", screenHash, null, elements)

        // Then: Visits tracked
        assertEquals(3, safetyManager.getScreenVisits(screenHash))
    }

    @Test
    fun isLoopDetected_TooManyVisits_ReturnsTrue() {
        // Given: Screen visited too many times
        val screenHash = "loop_screen"
        val elements = emptyList<ElementInfo>()

        // When: Visit screen 4+ times (threshold = 3)
        repeat(4) {
            safetyManager.updateScreenContext("com.example.app", screenHash, null, elements)
        }

        // Then: Loop detected
        assertTrue(safetyManager.isLoopDetected(screenHash))
        verify { mockCallback.onLoopDetected(screenHash, 4) }
    }

    @Test
    fun resetScreenVisits_ClearsVisitCount() {
        // Given: Screen with visits
        val screenHash = "test_screen"
        val elements = emptyList<ElementInfo>()

        safetyManager.updateScreenContext("com.example.app", screenHash, null, elements)
        safetyManager.updateScreenContext("com.example.app", screenHash, null, elements)
        assertEquals(2, safetyManager.getScreenVisits(screenHash))

        // When: Reset visits
        safetyManager.resetScreenVisits(screenHash)

        // Then: Visits cleared
        assertEquals(0, safetyManager.getScreenVisits(screenHash))
    }

    // ============================================================
    // Reset Tests
    // ============================================================

    @Test
    fun reset_ClearsAllState() {
        // Given: SafetyManager with tracked data
        val screenHash = "test_screen"
        val elements = listOf(
            ElementInfo(
                className = "android.widget.EditText",
                text = "",
                isPassword = true,
                bounds = Rect(0, 0, 100, 100)
            )
        )

        safetyManager.updateScreenContext("com.example.app", screenHash, null, elements)
        assertEquals(1, safetyManager.getScreenVisits(screenHash))

        // When: Reset
        safetyManager.reset()

        // Then: All state cleared
        assertEquals(0, safetyManager.getScreenVisits(screenHash))
    }

    @Test
    fun clearScreen_ClearsScreenState() {
        // Given: Multiple screens with data
        val screen1 = "screen1"
        val screen2 = "screen2"
        val elements = emptyList<ElementInfo>()

        safetyManager.updateScreenContext("com.example.app", screen1, null, elements)
        safetyManager.updateScreenContext("com.example.app", screen2, null, elements)

        assertEquals(1, safetyManager.getScreenVisits(screen1))
        assertEquals(1, safetyManager.getScreenVisits(screen2))

        // When: Clear screen1
        safetyManager.clearScreen(screen1)

        // Then: Only screen1 cleared
        assertEquals(0, safetyManager.getScreenVisits(screen1))
        assertEquals(1, safetyManager.getScreenVisits(screen2))
    }
}
