// filename: Universal/AVA/Core/Data/src/test/kotlin/com/augmentalis/ava/core/data/prefs/ChatPreferencesRAGTest.kt
// created: 2025-11-22
// updated: 2025-12-18 (converted from Mockito to MockK)
// author: RAG Settings Integration Specialist
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.core.data.prefs

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for ChatPreferences RAG settings (Phase 2 - Task 1)
 *
 * Tests:
 * - RAG enabled get/set
 * - Document IDs get/set with serialization
 * - RAG threshold get/set with clamping
 * - StateFlow reactivity
 * - Edge cases (empty lists, invalid values)
 *
 * Uses MockK + Robolectric for Android unit testing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ChatPreferencesRAGTest {

    private lateinit var context: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var chatPreferences: ChatPreferences

    @Before
    fun setup() {
        // Reset singleton to allow mocking for each test
        ChatPreferences.resetInstance()

        // Create mocks
        editor = mockk(relaxed = true) {
            every { putBoolean(any(), any()) } returns this
            every { putString(any(), any()) } returns this
            every { putFloat(any(), any()) } returns this
            every { apply() } returns Unit
        }

        sharedPreferences = mockk(relaxed = true) {
            every { edit() } returns editor
            every { getBoolean(eq("rag_enabled"), any()) } returns false
            every { getString(eq("rag_document_ids"), null) } returns null
            every { getFloat(eq("rag_threshold"), any()) } returns 0.7f
        }

        context = mockk(relaxed = true) {
            every { applicationContext } returns this
            every { getSharedPreferences(any(), any()) } returns sharedPreferences
        }

        chatPreferences = ChatPreferences.getInstance(context)
    }

    // ==================== RAG Enabled Tests ====================

    @Test
    fun `getRagEnabled returns default false when not set`() {
        val enabled = chatPreferences.getRagEnabled()
        assertFalse(enabled)
    }

    @Test
    fun `setRagEnabled persists value`() {
        chatPreferences.setRagEnabled(true)
        verify { editor.putBoolean("rag_enabled", true) }
        verify { editor.apply() }
    }

    @Test
    fun `ragEnabled StateFlow updates when changed`() = runTest {
        every { sharedPreferences.getBoolean(eq("rag_enabled"), any()) } returns true

        chatPreferences.setRagEnabled(true)

        val enabled = chatPreferences.ragEnabled.first()
        assertTrue(enabled)
    }

    // ==================== Document IDs Tests ====================

    @Test
    fun `getSelectedDocumentIds returns empty list when not set`() {
        val ids = chatPreferences.getSelectedDocumentIds()
        assertEquals(emptyList(), ids)
    }

    @Test
    fun `getSelectedDocumentIds parses comma-separated string`() {
        every { sharedPreferences.getString(eq("rag_document_ids"), null) } returns "doc1,doc2,doc3"

        val ids = chatPreferences.getSelectedDocumentIds()
        assertEquals(listOf("doc1", "doc2", "doc3"), ids)
    }

    @Test
    fun `setSelectedDocumentIds serializes list to comma-separated string`() {
        val documentIds = listOf("doc1", "doc2", "doc3")
        chatPreferences.setSelectedDocumentIds(documentIds)

        verify { editor.putString("rag_document_ids", "doc1,doc2,doc3") }
        verify { editor.apply() }
    }

    @Test
    fun `setSelectedDocumentIds handles empty list`() {
        chatPreferences.setSelectedDocumentIds(emptyList())

        verify { editor.putString("rag_document_ids", "") }
        verify { editor.apply() }
    }

    @Test
    fun `setSelectedDocumentIds handles single document`() {
        chatPreferences.setSelectedDocumentIds(listOf("doc1"))

        verify { editor.putString("rag_document_ids", "doc1") }
        verify { editor.apply() }
    }

    @Test
    fun `getSelectedDocumentIds filters blank entries`() {
        every { sharedPreferences.getString(eq("rag_document_ids"), null) } returns "doc1,,doc2,  ,doc3"

        val ids = chatPreferences.getSelectedDocumentIds()
        assertEquals(listOf("doc1", "doc2", "doc3"), ids)
    }

    @Test
    fun `selectedDocumentIds StateFlow updates when changed`() = runTest {
        val documentIds = listOf("doc1", "doc2")
        chatPreferences.setSelectedDocumentIds(documentIds)

        val ids = chatPreferences.selectedDocumentIds.first()
        assertEquals(documentIds, ids)
    }

    // ==================== RAG Threshold Tests ====================

    @Test
    fun `getRagThreshold returns default 0_7 when not set`() {
        val threshold = chatPreferences.getRagThreshold()
        assertEquals(0.7f, threshold)
    }

    @Test
    fun `setRagThreshold persists value`() {
        chatPreferences.setRagThreshold(0.8f)
        verify { editor.putFloat("rag_threshold", 0.8f) }
        verify { editor.apply() }
    }

    @Test
    fun `setRagThreshold clamps value to 0_0 - 1_0 range`() {
        // Test lower bound
        chatPreferences.setRagThreshold(-0.5f)
        verify { editor.putFloat("rag_threshold", 0.0f) }

        // Test upper bound
        chatPreferences.setRagThreshold(1.5f)
        verify { editor.putFloat("rag_threshold", 1.0f) }
    }

    @Test
    fun `setRagThreshold accepts valid values`() {
        // Test minimum
        chatPreferences.setRagThreshold(0.0f)
        verify { editor.putFloat("rag_threshold", 0.0f) }

        // Test mid-range
        chatPreferences.setRagThreshold(0.5f)
        verify { editor.putFloat("rag_threshold", 0.5f) }

        // Test maximum
        chatPreferences.setRagThreshold(1.0f)
        verify { editor.putFloat("rag_threshold", 1.0f) }
    }

    @Test
    fun `ragThreshold StateFlow updates when changed`() = runTest {
        chatPreferences.setRagThreshold(0.85f)

        val threshold = chatPreferences.ragThreshold.first()
        assertEquals(0.85f, threshold)
    }

    // ==================== Integration Tests ====================

    @Test
    fun `complete RAG settings workflow`() = runTest {
        // Enable RAG
        chatPreferences.setRagEnabled(true)
        assertTrue(chatPreferences.ragEnabled.first())

        // Select documents
        val documents = listOf("doc1", "doc2", "doc3")
        chatPreferences.setSelectedDocumentIds(documents)
        assertEquals(documents, chatPreferences.selectedDocumentIds.first())

        // Set threshold
        chatPreferences.setRagThreshold(0.75f)
        assertEquals(0.75f, chatPreferences.ragThreshold.first())

        // Verify all persisted
        verify { editor.putBoolean("rag_enabled", true) }
        verify { editor.putString("rag_document_ids", "doc1,doc2,doc3") }
        verify { editor.putFloat("rag_threshold", 0.75f) }
        verify(atLeast = 3) { editor.apply() }
    }
}
