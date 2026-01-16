/**
 * JITLearnerTest.kt - Tests for Just-In-Time Learner (Phase 14)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * TDD tests for JITLearner class which handles on-demand learning
 * of UI elements for voice command generation.
 */
package com.augmentalis.voiceoscoreng.learnapp

import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Mock implementation of IConsentProvider for testing consent workflows
 */
class MockConsentProvider : IConsentProvider {
    private val consentedPackages = mutableSetOf<String>()
    var requestConsentResult: Boolean = true
    var requestConsentCallCount: Int = 0

    override fun requestConsent(element: ElementInfo): Boolean {
        requestConsentCallCount++
        return requestConsentResult
    }

    override fun hasConsent(packageName: String): Boolean {
        return packageName in consentedPackages
    }

    fun grantConsent(packageName: String) {
        consentedPackages.add(packageName)
    }

    fun revokeConsent(packageName: String) {
        consentedPackages.remove(packageName)
    }

    fun clearConsent() {
        consentedPackages.clear()
    }
}

class JITLearnerTest {

    // ==================== shouldLearn - Disabled State Tests ====================

    @Test
    fun `shouldLearn returns false when JITLearner is disabled`() {
        val learner = JITLearner()
        learner.setEnabled(false)

        val element = ElementInfo.button(
            text = "Submit",
            resourceId = "com.test:id/submit",
            packageName = "com.test.app"
        )

        assertFalse(learner.shouldLearn(element))
    }

    @Test
    fun `shouldLearn returns true when JITLearner is enabled with valid element`() {
        val learner = JITLearner()
        assertTrue(learner.isEnabled())

        val element = ElementInfo.button(
            text = "Submit",
            resourceId = "com.test:id/submit",
            packageName = "com.test.app"
        )

        assertTrue(learner.shouldLearn(element))
    }

    // ==================== shouldLearn - Already Learned Tests ====================

    @Test
    fun `shouldLearn returns false for already learned elements`() {
        val learner = JITLearner()

        val element = ElementInfo.button(
            text = "Submit",
            resourceId = "com.test:id/submit",
            packageName = "com.test.app"
        )

        // First request should create learning request
        val request = learner.requestLearning(element, "session1")
        assertNotNull(request)

        // Mark as learned via consent
        learner.onUserConsent(request)

        // Now shouldLearn should return false
        assertFalse(learner.shouldLearn(element))
    }

    // ==================== shouldLearn - Content Tests ====================

    @Test
    fun `shouldLearn returns false for elements without text or contentDescription`() {
        val learner = JITLearner()

        val element = ElementInfo(
            className = "Button",
            resourceId = "com.test:id/mystery_btn",
            text = "",
            contentDescription = "",
            isClickable = true,
            packageName = "com.test.app"
        )

        assertFalse(learner.shouldLearn(element))
    }

    @Test
    fun `shouldLearn returns false for elements with only whitespace text`() {
        val learner = JITLearner()

        val element = ElementInfo(
            className = "Button",
            text = "   ",
            contentDescription = "  \t\n  ",
            isClickable = true,
            packageName = "com.test.app"
        )

        assertFalse(learner.shouldLearn(element))
    }

    @Test
    fun `shouldLearn returns true for elements with contentDescription only`() {
        val learner = JITLearner()

        val element = ElementInfo(
            className = "ImageButton",
            text = "",
            contentDescription = "Menu button",
            isClickable = true,
            packageName = "com.test.app"
        )

        assertTrue(learner.shouldLearn(element))
    }

    // ==================== shouldLearn - Interactivity Tests ====================

    @Test
    fun `shouldLearn returns false for non-interactive elements`() {
        val learner = JITLearner()

        val element = ElementInfo(
            className = "TextView",
            text = "Hello World",
            isClickable = false,
            isScrollable = false,
            packageName = "com.test.app"
        )

        assertFalse(learner.shouldLearn(element))
    }

    @Test
    fun `shouldLearn returns true for scrollable elements with text`() {
        val learner = JITLearner()

        val element = ElementInfo(
            className = "RecyclerView",
            text = "",
            contentDescription = "Message list",
            isClickable = false,
            isScrollable = true,
            packageName = "com.test.app"
        )

        assertTrue(learner.shouldLearn(element))
    }

    @Test
    fun `shouldLearn returns true for clickable elements`() {
        val learner = JITLearner()

        val element = ElementInfo.button(
            text = "OK",
            packageName = "com.test.app"
        )

        assertTrue(learner.shouldLearn(element))
    }

    // ==================== requestLearning Tests ====================

    @Test
    fun `requestLearning creates LearningRequest for valid element`() {
        val learner = JITLearner()

        val element = ElementInfo.button(
            text = "Submit",
            resourceId = "com.test:id/submit",
            packageName = "com.test.app"
        )

        val request = learner.requestLearning(element, "session123")

        assertNotNull(request)
        assertEquals(element, request.element)
        assertEquals("session123", request.sessionId)
        assertTrue(request.timestamp > 0)
    }

    @Test
    fun `requestLearning returns null when shouldLearn is false`() {
        val learner = JITLearner()
        learner.setEnabled(false)

        val element = ElementInfo.button(
            text = "Submit",
            packageName = "com.test.app"
        )

        val request = learner.requestLearning(element, "session123")

        assertNull(request)
    }

    @Test
    fun `requestLearning adds request to pending list`() {
        val learner = JITLearner()
        assertEquals(0, learner.getPendingCount())

        val element = ElementInfo.button(
            text = "Submit",
            packageName = "com.test.app"
        )

        learner.requestLearning(element, "session1")
        assertEquals(1, learner.getPendingCount())

        val element2 = ElementInfo.button(
            text = "Cancel",
            packageName = "com.test.app"
        )
        learner.requestLearning(element2, "session1")
        assertEquals(2, learner.getPendingCount())
    }

    // ==================== onUserConsent - Command Generation Tests ====================

    @Test
    fun `onUserConsent generates commands from element text`() {
        val learner = JITLearner()

        val element = ElementInfo.button(
            text = "Submit Form",
            resourceId = "com.test:id/submit",
            packageName = "com.test.app"
        )

        val request = learner.requestLearning(element, "session1")
        assertNotNull(request)

        val result = learner.onUserConsent(request)

        assertTrue(result.success)
        assertTrue(result.generatedCommands.contains("tap submit form"))
    }

    @Test
    fun `onUserConsent generates commands from contentDescription`() {
        val learner = JITLearner()

        val element = ElementInfo(
            className = "ImageButton",
            text = "",
            contentDescription = "Open Menu",
            isClickable = true,
            packageName = "com.test.app"
        )

        val request = learner.requestLearning(element, "session1")
        assertNotNull(request)

        val result = learner.onUserConsent(request)

        assertTrue(result.success)
        assertTrue(result.generatedCommands.contains("tap open menu"))
    }

    @Test
    fun `onUserConsent generates commands from both text and contentDescription`() {
        val learner = JITLearner()

        val element = ElementInfo(
            className = "Button",
            text = "Send",
            contentDescription = "Send message",
            isClickable = true,
            packageName = "com.test.app"
        )

        val request = learner.requestLearning(element, "session1")
        assertNotNull(request)

        val result = learner.onUserConsent(request)

        assertTrue(result.success)
        assertTrue(result.generatedCommands.contains("tap send"))
        assertTrue(result.generatedCommands.contains("tap send message"))
    }

    @Test
    fun `onUserConsent marks element as learned`() {
        val learner = JITLearner()
        assertEquals(0, learner.getLearnedCount())

        val element = ElementInfo.button(
            text = "Submit",
            resourceId = "com.test:id/submit",
            packageName = "com.test.app"
        )

        val request = learner.requestLearning(element, "session1")
        assertNotNull(request)

        learner.onUserConsent(request)
        assertEquals(1, learner.getLearnedCount())

        // Verify shouldLearn returns false now
        assertFalse(learner.shouldLearn(element))
    }

    @Test
    fun `onUserConsent generates VUID for element`() {
        val learner = JITLearner()

        val element = ElementInfo.button(
            text = "Submit",
            resourceId = "com.test:id/submit",
            packageName = "com.test.app"
        )

        val request = learner.requestLearning(element, "session1")
        assertNotNull(request)

        val result = learner.onUserConsent(request)

        assertNotNull(result.vuid)
        assertTrue(result.vuid!!.startsWith("vuid_"))
        assertTrue(result.vuid!!.contains("com.test.app"))
    }

    @Test
    fun `onUserConsent removes request from pending`() {
        val learner = JITLearner()

        val element = ElementInfo.button(
            text = "Submit",
            packageName = "com.test.app"
        )

        val request = learner.requestLearning(element, "session1")
        assertNotNull(request)
        assertEquals(1, learner.getPendingCount())

        learner.onUserConsent(request)
        assertEquals(0, learner.getPendingCount())
    }

    // ==================== Consent Provider Tests ====================

    @Test
    fun `onUserConsent fails when consent not granted via provider`() {
        val mockProvider = MockConsentProvider()
        val learner = JITLearner(consentProvider = mockProvider)

        val element = ElementInfo.button(
            text = "Submit",
            packageName = "com.test.app"
        )

        val request = learner.requestLearning(element, "session1")
        assertNotNull(request)

        // Consent not granted for this package
        val result = learner.onUserConsent(request)

        assertFalse(result.success)
        assertEquals("User consent not granted", result.error)
    }

    @Test
    fun `onUserConsent succeeds when consent granted via provider`() {
        val mockProvider = MockConsentProvider()
        mockProvider.grantConsent("com.test.app")
        val learner = JITLearner(consentProvider = mockProvider)

        val element = ElementInfo.button(
            text = "Submit",
            packageName = "com.test.app"
        )

        val request = learner.requestLearning(element, "session1")
        assertNotNull(request)

        val result = learner.onUserConsent(request)

        assertTrue(result.success)
        assertNull(result.error)
    }

    @Test
    fun `onUserConsent succeeds without provider - null provider`() {
        val learner = JITLearner(null)

        val element = ElementInfo.button(
            text = "Submit",
            packageName = "com.test.app"
        )

        val request = learner.requestLearning(element, "session1")
        assertNotNull(request)

        val result = learner.onUserConsent(request)

        assertTrue(result.success)
    }

    // ==================== cancelLearning Tests ====================

    @Test
    fun `cancelLearning removes request from pending`() {
        val learner = JITLearner()

        val element = ElementInfo.button(
            text = "Submit",
            packageName = "com.test.app"
        )

        val request = learner.requestLearning(element, "session1")
        assertNotNull(request)
        assertEquals(1, learner.getPendingCount())

        learner.cancelLearning(request)
        assertEquals(0, learner.getPendingCount())
    }

    @Test
    fun `cancelLearning does not mark element as learned`() {
        val learner = JITLearner()

        val element = ElementInfo.button(
            text = "Submit",
            resourceId = "com.test:id/submit",
            packageName = "com.test.app"
        )

        val request = learner.requestLearning(element, "session1")
        assertNotNull(request)

        learner.cancelLearning(request)

        // Element should still be learnable since we cancelled
        assertTrue(learner.shouldLearn(element))
        assertEquals(0, learner.getLearnedCount())
    }

    // ==================== Count Methods Tests ====================

    @Test
    fun `getPendingCount returns correct count`() {
        val learner = JITLearner()

        assertEquals(0, learner.getPendingCount())

        learner.requestLearning(
            ElementInfo.button(text = "One", packageName = "com.test"),
            "s1"
        )
        assertEquals(1, learner.getPendingCount())

        learner.requestLearning(
            ElementInfo.button(text = "Two", packageName = "com.test"),
            "s1"
        )
        assertEquals(2, learner.getPendingCount())

        learner.requestLearning(
            ElementInfo.button(text = "Three", packageName = "com.test"),
            "s1"
        )
        assertEquals(3, learner.getPendingCount())
    }

    @Test
    fun `getLearnedCount returns correct count`() {
        val learner = JITLearner()

        assertEquals(0, learner.getLearnedCount())

        val request1 = learner.requestLearning(
            ElementInfo.button(text = "One", resourceId = "id1", packageName = "com.test"),
            "s1"
        )
        learner.onUserConsent(request1!!)
        assertEquals(1, learner.getLearnedCount())

        val request2 = learner.requestLearning(
            ElementInfo.button(text = "Two", resourceId = "id2", packageName = "com.test"),
            "s1"
        )
        learner.onUserConsent(request2!!)
        assertEquals(2, learner.getLearnedCount())
    }

    // ==================== clearLearned Tests ====================

    @Test
    fun `clearLearned removes all learned elements`() {
        val learner = JITLearner()

        // Learn some elements
        val element1 = ElementInfo.button(text = "One", resourceId = "id1", packageName = "com.test")
        val element2 = ElementInfo.button(text = "Two", resourceId = "id2", packageName = "com.test")

        val request1 = learner.requestLearning(element1, "s1")
        val request2 = learner.requestLearning(element2, "s1")
        learner.onUserConsent(request1!!)
        learner.onUserConsent(request2!!)

        assertEquals(2, learner.getLearnedCount())

        learner.clearLearned()

        assertEquals(0, learner.getLearnedCount())

        // Elements should be learnable again
        assertTrue(learner.shouldLearn(element1))
        assertTrue(learner.shouldLearn(element2))
    }

    @Test
    fun `clearLearned does not affect pending requests`() {
        val learner = JITLearner()

        // Create pending request
        learner.requestLearning(
            ElementInfo.button(text = "Pending", packageName = "com.test"),
            "s1"
        )
        assertEquals(1, learner.getPendingCount())

        // Learn another element
        val request = learner.requestLearning(
            ElementInfo.button(text = "Learned", resourceId = "id1", packageName = "com.test"),
            "s1"
        )
        assertEquals(2, learner.getPendingCount()) // Two pending before consent

        learner.onUserConsent(request!!)

        assertEquals(1, learner.getPendingCount()) // One pending after consent (Learned was removed)
        assertEquals(1, learner.getLearnedCount())

        learner.clearLearned()

        assertEquals(0, learner.getLearnedCount())
        assertEquals(1, learner.getPendingCount()) // Original "Pending" request still there
    }

    // ==================== reset Tests ====================

    @Test
    fun `reset clears all state`() {
        val learner = JITLearner()

        // Add pending requests
        learner.requestLearning(
            ElementInfo.button(text = "Pending1", packageName = "com.test"),
            "s1"
        )
        learner.requestLearning(
            ElementInfo.button(text = "Pending2", packageName = "com.test"),
            "s1"
        )

        // Learn some elements
        val request = learner.requestLearning(
            ElementInfo.button(text = "Learned", resourceId = "id1", packageName = "com.test"),
            "s1"
        )
        learner.onUserConsent(request!!)

        assertTrue(learner.getPendingCount() > 0)
        assertEquals(1, learner.getLearnedCount())

        learner.reset()

        assertEquals(0, learner.getPendingCount())
        assertEquals(0, learner.getLearnedCount())
    }

    // ==================== Enable/Disable Tests ====================

    @Test
    fun `setEnabled changes enabled state`() {
        val learner = JITLearner()

        assertTrue(learner.isEnabled())

        learner.setEnabled(false)
        assertFalse(learner.isEnabled())

        learner.setEnabled(true)
        assertTrue(learner.isEnabled())
    }

    // ==================== Edge Cases ====================

    @Test
    fun `learning same element twice does not add duplicate`() {
        val learner = JITLearner()

        val element = ElementInfo.button(
            text = "Submit",
            resourceId = "com.test:id/submit",
            packageName = "com.test.app"
        )

        val request1 = learner.requestLearning(element, "session1")
        assertNotNull(request1)
        learner.onUserConsent(request1)

        assertEquals(1, learner.getLearnedCount())

        // Try to request learning again - should return null
        val request2 = learner.requestLearning(element, "session2")
        assertNull(request2)

        assertEquals(1, learner.getLearnedCount())
    }

    @Test
    fun `LearningResult contains correct element reference`() {
        val learner = JITLearner()

        val element = ElementInfo.button(
            text = "Submit",
            resourceId = "com.test:id/submit",
            packageName = "com.test.app"
        )

        val request = learner.requestLearning(element, "session1")
        assertNotNull(request)

        val result = learner.onUserConsent(request)

        assertEquals(element, result.element)
    }

    @Test
    fun `VUID generation uses resourceId when available`() {
        val learner = JITLearner()

        val element = ElementInfo.button(
            text = "Submit",
            resourceId = "com.test:id/submit_btn",
            packageName = "com.test.app"
        )

        val request = learner.requestLearning(element, "session1")
        val result = learner.onUserConsent(request!!)

        assertNotNull(result.vuid)
        assertTrue(result.vuid!!.contains("com.test:id/submit_btn"))
    }

    @Test
    fun `VUID generation uses text hash when resourceId not available`() {
        val learner = JITLearner()

        val element = ElementInfo(
            className = "Button",
            text = "Dynamic Button",
            isClickable = true,
            packageName = "com.test.app"
        )

        val request = learner.requestLearning(element, "session1")
        val result = learner.onUserConsent(request!!)

        assertNotNull(result.vuid)
        assertTrue(result.vuid!!.startsWith("vuid_com.test.app_"))
    }
}
