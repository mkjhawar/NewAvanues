package com.augmentalis.ava.core.data.prefs

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Tests for [ChatPreferences] (Phase 5 - P5T03).
 *
 * Tests cover:
 * - Conversation mode (APPEND vs NEW)
 * - Last active conversation ID persistence
 * - Confidence threshold (0.0-1.0 with clamping)
 * - StateFlow reactivity
 * - Singleton pattern
 * - Clear all preferences
 *
 * Design:
 * - Uses AndroidJUnit4 for SharedPreferences access
 * - Clears preferences before/after each test
 * - Tests default values
 * - Tests edge cases (null values, out-of-range thresholds)
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class ChatPreferencesTest {

    private lateinit var context: Context
    private lateinit var chatPreferences: ChatPreferences

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        chatPreferences = ChatPreferences.getInstance(context)

        // Clear preferences before each test
        chatPreferences.clearAll()
    }

    @After
    fun tearDown() {
        // Clean up after each test
        chatPreferences.clearAll()
    }

    // ==================== Conversation Mode Tests ====================

    @Test
    fun conversationMode_defaultsToAppend() = runTest {
        // GIVEN: Fresh preferences (cleared in setup)

        // WHEN: Getting conversation mode
        val mode = chatPreferences.getConversationMode()

        // THEN: Should default to APPEND
        assertEquals(ConversationMode.APPEND, mode)
    }

    @Test
    fun setConversationMode_updatesStateFlow() = runTest {
        // GIVEN: ChatPreferences with default mode

        // WHEN: Setting conversation mode to NEW
        chatPreferences.setConversationMode(ConversationMode.NEW)

        // THEN: Both getter and StateFlow should return NEW
        assertEquals(ConversationMode.NEW, chatPreferences.getConversationMode())
        assertEquals(ConversationMode.NEW, chatPreferences.conversationMode.first())
    }

    @Test
    fun setConversationMode_persistsAcrossInstances() = runTest {
        // GIVEN: Set conversation mode to NEW
        chatPreferences.setConversationMode(ConversationMode.NEW)

        // WHEN: Getting new instance (simulates app restart)
        val newInstance = ChatPreferences.getInstance(context)

        // THEN: Should persist NEW mode
        assertEquals(ConversationMode.NEW, newInstance.getConversationMode())
    }

    @Test
    fun conversationMode_stateFlowReactive() = runTest {
        // GIVEN: Initial APPEND mode
        assertEquals(ConversationMode.APPEND, chatPreferences.conversationMode.first())

        // WHEN: Changing mode to NEW
        chatPreferences.setConversationMode(ConversationMode.NEW)

        // THEN: StateFlow should update immediately
        assertEquals(ConversationMode.NEW, chatPreferences.conversationMode.first())
    }

    // ==================== Last Active Conversation ID Tests ====================

    @Test
    fun lastActiveConversationId_defaultsToNull() {
        // GIVEN: Fresh preferences (cleared in setup)

        // WHEN: Getting last active conversation ID
        val conversationId = chatPreferences.getLastActiveConversationId()

        // THEN: Should default to null
        assertNull(conversationId)
    }

    @Test
    fun setLastActiveConversationId_persistsValue() {
        // GIVEN: ChatPreferences
        val testId = "conv-12345"

        // WHEN: Setting conversation ID
        chatPreferences.setLastActiveConversationId(testId)

        // THEN: Should persist value
        assertEquals(testId, chatPreferences.getLastActiveConversationId())
    }

    @Test
    fun setLastActiveConversationId_handlesNullClear() {
        // GIVEN: Conversation ID is set
        chatPreferences.setLastActiveConversationId("conv-12345")
        assertNotNull(chatPreferences.getLastActiveConversationId())

        // WHEN: Setting to null (clear)
        chatPreferences.setLastActiveConversationId(null)

        // THEN: Should clear value
        assertNull(chatPreferences.getLastActiveConversationId())
    }

    @Test
    fun lastActiveConversationId_persistsAcrossInstances() {
        // GIVEN: Set conversation ID
        val testId = "conv-67890"
        chatPreferences.setLastActiveConversationId(testId)

        // WHEN: Getting new instance (simulates app restart)
        val newInstance = ChatPreferences.getInstance(context)

        // THEN: Should persist conversation ID
        assertEquals(testId, newInstance.getLastActiveConversationId())
    }

    @Test
    fun lastActiveConversationId_overwritesPrevious() {
        // GIVEN: First conversation ID set
        chatPreferences.setLastActiveConversationId("conv-first")

        // WHEN: Setting second conversation ID
        chatPreferences.setLastActiveConversationId("conv-second")

        // THEN: Should overwrite with second ID
        assertEquals("conv-second", chatPreferences.getLastActiveConversationId())
    }

    // ==================== Confidence Threshold Tests ====================

    @Test
    fun confidenceThreshold_defaultsTo0_5() = runTest {
        // GIVEN: Fresh preferences (cleared in setup)

        // WHEN: Getting confidence threshold
        val threshold = chatPreferences.getConfidenceThreshold()

        // THEN: Should default to 0.5
        assertEquals(0.5f, threshold)
    }

    @Test
    fun setConfidenceThreshold_updatesStateFlow() = runTest {
        // GIVEN: ChatPreferences with default threshold

        // WHEN: Setting threshold to 0.7
        chatPreferences.setConfidenceThreshold(0.7f)

        // THEN: Both getter and StateFlow should return 0.7
        assertEquals(0.7f, chatPreferences.getConfidenceThreshold())
        assertEquals(0.7f, chatPreferences.confidenceThreshold.first())
    }

    @Test
    fun setConfidenceThreshold_clampsToRange() = runTest {
        // GIVEN: ChatPreferences

        // WHEN: Setting threshold above 1.0
        chatPreferences.setConfidenceThreshold(1.5f)

        // THEN: Should clamp to 1.0
        assertEquals(1.0f, chatPreferences.getConfidenceThreshold())

        // WHEN: Setting threshold below 0.0
        chatPreferences.setConfidenceThreshold(-0.3f)

        // THEN: Should clamp to 0.0
        assertEquals(0.0f, chatPreferences.getConfidenceThreshold())
    }

    @Test
    fun confidenceThreshold_acceptsEdgeCases() = runTest {
        // GIVEN: ChatPreferences

        // WHEN: Setting to 0.0 (minimum)
        chatPreferences.setConfidenceThreshold(0.0f)

        // THEN: Should accept 0.0
        assertEquals(0.0f, chatPreferences.getConfidenceThreshold())

        // WHEN: Setting to 1.0 (maximum)
        chatPreferences.setConfidenceThreshold(1.0f)

        // THEN: Should accept 1.0
        assertEquals(1.0f, chatPreferences.getConfidenceThreshold())
    }

    @Test
    fun confidenceThreshold_persistsAcrossInstances() = runTest {
        // GIVEN: Set threshold to 0.8
        chatPreferences.setConfidenceThreshold(0.8f)

        // WHEN: Getting new instance (simulates app restart)
        val newInstance = ChatPreferences.getInstance(context)

        // THEN: Should persist threshold
        assertEquals(0.8f, newInstance.getConfidenceThreshold())
    }

    @Test
    fun confidenceThreshold_stateFlowReactive() = runTest {
        // GIVEN: Initial 0.5 threshold
        assertEquals(0.5f, chatPreferences.confidenceThreshold.first())

        // WHEN: Changing threshold to 0.3
        chatPreferences.setConfidenceThreshold(0.3f)

        // THEN: StateFlow should update immediately
        assertEquals(0.3f, chatPreferences.confidenceThreshold.first())
    }

    // ==================== Clear All Tests ====================

    @Test
    fun clearAll_resetsToDefaults() = runTest {
        // GIVEN: Preferences with custom values
        chatPreferences.setConversationMode(ConversationMode.NEW)
        chatPreferences.setLastActiveConversationId("conv-12345")
        chatPreferences.setConfidenceThreshold(0.8f)

        // WHEN: Clearing all preferences
        chatPreferences.clearAll()

        // THEN: Should reset to defaults
        assertEquals(ConversationMode.APPEND, chatPreferences.getConversationMode())
        assertNull(chatPreferences.getLastActiveConversationId())
        assertEquals(0.5f, chatPreferences.getConfidenceThreshold())
    }

    @Test
    fun clearAll_updatesStateFlows() = runTest {
        // GIVEN: Preferences with custom values
        chatPreferences.setConversationMode(ConversationMode.NEW)
        chatPreferences.setConfidenceThreshold(0.8f)

        // WHEN: Clearing all preferences
        chatPreferences.clearAll()

        // THEN: StateFlows should reflect defaults
        assertEquals(ConversationMode.APPEND, chatPreferences.conversationMode.first())
        assertEquals(0.5f, chatPreferences.confidenceThreshold.first())
    }

    // ==================== Singleton Pattern Tests ====================

    @Test
    fun getInstance_returnsSameSingleton() {
        // GIVEN: Context

        // WHEN: Getting two instances
        val instance1 = ChatPreferences.getInstance(context)
        val instance2 = ChatPreferences.getInstance(context)

        // THEN: Should be same object
        assertEquals(instance1, instance2)
    }

    @Test
    fun getInstance_sharesSamePreferences() {
        // GIVEN: Two instances
        val instance1 = ChatPreferences.getInstance(context)
        val instance2 = ChatPreferences.getInstance(context)

        // WHEN: Setting value on instance1
        instance1.setConversationMode(ConversationMode.NEW)

        // THEN: instance2 should see the change
        assertEquals(ConversationMode.NEW, instance2.getConversationMode())
    }

    // ==================== Integration Tests ====================

    @Test
    fun multiplePreferences_independentPersistence() {
        // GIVEN: ChatPreferences

        // WHEN: Setting multiple preferences
        chatPreferences.setConversationMode(ConversationMode.NEW)
        chatPreferences.setLastActiveConversationId("conv-abc")
        chatPreferences.setConfidenceThreshold(0.65f)

        // THEN: All should persist independently
        assertEquals(ConversationMode.NEW, chatPreferences.getConversationMode())
        assertEquals("conv-abc", chatPreferences.getLastActiveConversationId())
        assertEquals(0.65f, chatPreferences.getConfidenceThreshold())

        // WHEN: Clearing only conversation ID
        chatPreferences.setLastActiveConversationId(null)

        // THEN: Other preferences should remain
        assertEquals(ConversationMode.NEW, chatPreferences.getConversationMode())
        assertNull(chatPreferences.getLastActiveConversationId())
        assertEquals(0.65f, chatPreferences.getConfidenceThreshold())
    }

    @Test
    fun simulateAppLifecycle_persistsAcrossRestarts() {
        // GIVEN: Initial preferences set
        chatPreferences.setConversationMode(ConversationMode.NEW)
        chatPreferences.setLastActiveConversationId("conv-lifecycle")
        chatPreferences.setConfidenceThreshold(0.75f)

        // WHEN: Simulating app restart (new instance)
        val afterRestart = ChatPreferences.getInstance(context)

        // THEN: All preferences should persist
        assertEquals(ConversationMode.NEW, afterRestart.getConversationMode())
        assertEquals("conv-lifecycle", afterRestart.getLastActiveConversationId())
        assertEquals(0.75f, afterRestart.getConfidenceThreshold())
    }
}
