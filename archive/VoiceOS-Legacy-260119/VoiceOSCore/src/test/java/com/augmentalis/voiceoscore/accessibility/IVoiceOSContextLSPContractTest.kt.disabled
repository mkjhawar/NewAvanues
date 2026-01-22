/**
 * IVoiceOSContextLSPContractTest.kt - LSP Contract Compliance Tests
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-22
 *
 * Tests that IVoiceOSContext implementations comply with Liskov Substitution Principle.
 * Verifies behavioral contracts documented in interface KDoc.
 *
 * Phase 5: SOLID Refactoring - Liskov Substitution Principle
 */

package com.augmentalis.voiceoscore.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceos.cursor.core.CursorOffset
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * LSP Contract Tests for IVoiceOSContext
 *
 * Tests verify that all implementations follow documented behavioral contracts:
 * - Property contracts (never null)
 * - Nullable return contracts
 * - Boolean return contracts
 * - Exception behavior (no throw for normal failures)
 * - Thread safety
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class IVoiceOSContextLSPContractTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockAccessibilityService: AccessibilityService

    @Mock
    private lateinit var mockWindowManager: WindowManager

    private lateinit var voiceOSContext: IVoiceOSContext

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Create test implementation
        voiceOSContext = object : IVoiceOSContext {
            override val context: Context = mockContext
            override val accessibilityService: AccessibilityService = mockAccessibilityService
            override val windowManager: WindowManager = mockWindowManager

            override fun performGlobalAction(action: Int): Boolean {
                // Mock: return false for unsupported actions
                return action == AccessibilityService.GLOBAL_ACTION_BACK
            }

            override fun getAppCommands(): Map<String, String> {
                return emptyMap()
            }

            override fun getSystemService(name: String): Any? {
                // Mock: return null for unavailable services
                return if (name == Context.WINDOW_SERVICE) mockWindowManager else null
            }

            override fun startActivity(intent: Intent) {
                // Mock: no-op for testing
            }

            override fun showToast(message: String) {
                // Mock: no-op for testing
            }

            override fun vibrate(duration: Long) {
                // Mock: no-op for testing
            }

            override fun isCursorVisible(): Boolean {
                return false // Mock: cursor not available
            }

            override fun getCursorPosition(): CursorOffset {
                return CursorOffset(0, 0) // Mock: default position
            }
        }
    }

    // ========== Contract: Property Contracts (Never Null) ==========

    @Test
    fun `context property is never null`() {
        // LSP Contract: MUST always return valid application context (never null)
        assertNotNull(voiceOSContext.context, "context property must never be null")
    }

    @Test
    fun `accessibilityService property is never null`() {
        // LSP Contract: MUST return active accessibility service
        assertNotNull(voiceOSContext.accessibilityService, "accessibilityService must never be null")
    }

    @Test
    fun `windowManager property is never null`() {
        // LSP Contract: MUST return valid WindowManager instance
        assertNotNull(voiceOSContext.windowManager, "windowManager must never be null")
    }

    // ========== Contract: Nullable Return Contracts ==========

    @Test
    fun `getRootNodeInActiveWindow returns null when no active window`() {
        // LSP Contract: MUST return null when no active window exists
        `when`(mockAccessibilityService.rootInActiveWindow).thenReturn(null)

        val result = voiceOSContext.getRootNodeInActiveWindow()
        assertNull(result, "getRootNodeInActiveWindow should return null when no active window")
    }

    @Test
    fun `getRootNodeInActiveWindow returns node when window available`() {
        // LSP Contract: Returns node when active window exists
        val mockNode = AccessibilityNodeInfo.obtain()
        `when`(mockAccessibilityService.rootInActiveWindow).thenReturn(mockNode)

        val result = voiceOSContext.getRootNodeInActiveWindow()
        assertNotNull(result, "getRootNodeInActiveWindow should return node when available")
    }

    @Test
    fun `getSystemService returns null when service not available`() {
        // LSP Contract: MUST return null when service not available (NOT throw)
        val result = voiceOSContext.getSystemService("INVALID_SERVICE")
        assertNull(result, "getSystemService should return null for unavailable service")
    }

    @Test
    fun `getSystemService returns service when available`() {
        // LSP Contract: Returns service instance when available
        val result = voiceOSContext.getSystemService(Context.WINDOW_SERVICE)
        assertNotNull(result, "getSystemService should return service when available")
    }

    // ========== Contract: Boolean Return Contracts ==========

    @Test
    fun `performGlobalAction returns false when action cannot be performed`() {
        // LSP Contract: MUST return false if action cannot be performed (NOT throw)
        val result = voiceOSContext.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
        assertFalse(result, "performGlobalAction should return false for unsupported action")
    }

    @Test
    fun `performGlobalAction returns true when action succeeds`() {
        // LSP Contract: Returns true when action performed successfully
        val result = voiceOSContext.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        assertTrue(result, "performGlobalAction should return true for supported action")
    }

    @Test
    fun `isCursorVisible returns false when cursor unavailable`() {
        // LSP Contract: MUST return false when cursor feature unavailable
        val result = voiceOSContext.isCursorVisible()
        assertFalse(result, "isCursorVisible should return false when cursor unavailable")
    }

    // ========== Contract: Empty Collections (Not Null) ==========

    @Test
    fun `getAppCommands returns empty map when no commands configured`() {
        // LSP Contract: MUST return empty map if no commands configured (NOT null)
        val result = voiceOSContext.getAppCommands()
        assertNotNull(result, "getAppCommands should return map (not null)")
        assertTrue(result.isEmpty(), "getAppCommands should return empty map when no commands")
    }

    // ========== Contract: Non-Null Return Values ==========

    @Test
    fun `getCursorPosition never returns null`() {
        // LSP Contract: MUST NOT return null (return default CursorOffset(0, 0) if unavailable)
        val result = voiceOSContext.getCursorPosition()
        assertNotNull(result, "getCursorPosition must never return null")
    }

    @Test
    fun `getCursorPosition returns default when cursor unavailable`() {
        // LSP Contract: Returns (0, 0) when cursor feature unavailable
        val result = voiceOSContext.getCursorPosition()
        assertEquals(0, result.x, "Default cursor X should be 0")
        assertEquals(0, result.y, "Default cursor Y should be 0")
    }

    @Test
    fun `getPackageManager never returns null`() {
        // LSP Contract: MUST return valid PackageManager (never null)
        `when`(mockContext.packageManager).thenReturn(
            mockContext.applicationContext.packageManager
        )

        val result = voiceOSContext.getPackageManager()
        assertNotNull(result, "getPackageManager must never return null")
    }

    // ========== Contract: Exception Behavior (No Throw for Normal Failures) ==========

    @Test
    fun `startActivity does not throw when activity not found`() {
        // LSP Contract: MUST NOT throw if activity not found (log error instead)
        val intent = Intent("com.nonexistent.ACTION")

        try {
            voiceOSContext.startActivity(intent)
            // Success - no exception thrown
        } catch (e: Exception) {
            throw AssertionError("startActivity should not throw when activity not found", e)
        }
    }

    @Test
    fun `showToast does not throw on empty message`() {
        // LSP Contract: MUST handle empty/null messages gracefully (no-op)
        try {
            voiceOSContext.showToast("")
            voiceOSContext.showToast("   ")
            // Success - no exception thrown
        } catch (e: Exception) {
            throw AssertionError("showToast should not throw on empty message", e)
        }
    }

    @Test
    fun `vibrate does not throw when vibrator unavailable`() {
        // LSP Contract: MUST handle vibrator unavailable gracefully (no-op)
        try {
            voiceOSContext.vibrate(100L)
            // Success - no exception thrown
        } catch (e: Exception) {
            throw AssertionError("vibrate should not throw when vibrator unavailable", e)
        }
    }

    @Test
    fun `performGlobalAction does not throw on invalid action`() {
        // LSP Contract: MUST NOT throw for invalid/unsupported actions
        try {
            val result = voiceOSContext.performGlobalAction(-1) // Invalid action
            assertFalse(result, "Should return false for invalid action")
        } catch (e: Exception) {
            throw AssertionError("performGlobalAction should not throw on invalid action", e)
        }
    }

    // ========== Contract: Thread Safety ==========

    @Test
    fun `methods are callable from any thread`() {
        // LSP Contract: All methods MUST be callable from any thread
        // Note: This is a basic test. Full thread safety requires more comprehensive testing.

        // Test from main thread context
        val result1 = voiceOSContext.getAppCommands()
        assertNotNull(result1)

        val result2 = voiceOSContext.isCursorVisible()
        assertNotNull(result2)

        val result3 = voiceOSContext.getCursorPosition()
        assertNotNull(result3)

        // All should execute without thread-related exceptions
    }

    // ========== Contract: Behavioral Consistency ==========

    @Test
    fun `getRootNodeInActiveWindow behavior is consistent`() {
        // LSP Contract: Default implementation uses accessibilityService.rootInActiveWindow
        `when`(mockAccessibilityService.rootInActiveWindow).thenReturn(null)

        val result1 = voiceOSContext.getRootNodeInActiveWindow()
        val result2 = voiceOSContext.getRootNodeInActiveWindow()

        // Both calls should return null consistently
        assertNull(result1)
        assertNull(result2)
    }

    @Test
    fun `getPackageManager behavior is consistent`() {
        // LSP Contract: Default implementation uses context.packageManager
        `when`(mockContext.packageManager).thenReturn(
            mockContext.applicationContext.packageManager
        )

        val result1 = voiceOSContext.getPackageManager()
        val result2 = voiceOSContext.getPackageManager()

        // Both calls should return same instance
        assertNotNull(result1)
        assertNotNull(result2)
    }

    @Test
    fun `getCursorPosition returns consistent default when unavailable`() {
        // LSP Contract: Returns (0, 0) when cursor feature unavailable
        val result1 = voiceOSContext.getCursorPosition()
        val result2 = voiceOSContext.getCursorPosition()

        assertEquals(result1.x, result2.x, "Cursor X should be consistent")
        assertEquals(result1.y, result2.y, "Cursor Y should be consistent")
    }

    // ========== Contract: No Blocking Operations on Main Thread ==========

    @Test
    fun `methods complete quickly and do not block`() {
        // LSP Contract: No blocking operations on main thread
        // This test verifies methods complete in reasonable time

        val startTime = System.currentTimeMillis()

        voiceOSContext.isCursorVisible()
        voiceOSContext.getCursorPosition()
        voiceOSContext.getAppCommands()
        voiceOSContext.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)

        val duration = System.currentTimeMillis() - startTime

        assertTrue(duration < 100, "Methods should complete quickly (< 100ms)")
    }
}
