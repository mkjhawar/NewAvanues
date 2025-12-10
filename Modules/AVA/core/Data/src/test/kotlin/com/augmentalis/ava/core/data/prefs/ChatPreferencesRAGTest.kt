// filename: Universal/AVA/Core/Data/src/test/kotlin/com/augmentalis/ava/core/data/prefs/ChatPreferencesRAGTest.kt
// created: 2025-11-22
// author: RAG Settings Integration Specialist
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.core.data.prefs

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
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
 * Note: Using Silent mode because not all mocks are used in all tests.
 * This is acceptable because the shared setup provides common mocks
 * that different tests use selectively.
 */
@RunWith(MockitoJUnitRunner.Silent::class)
class ChatPreferencesRAGTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var sharedPreferences: SharedPreferences

    @Mock
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var chatPreferences: ChatPreferences

    @Before
    fun setup() {
        // Reset singleton to allow mocking for each test
        ChatPreferences.resetInstance()

        // Mock SharedPreferences behavior
        `when`(context.applicationContext).thenReturn(context)
        `when`(context.getSharedPreferences(anyString(), anyInt())).thenReturn(sharedPreferences)
        `when`(sharedPreferences.edit()).thenReturn(editor)
        `when`(editor.putBoolean(anyString(), anyBoolean())).thenReturn(editor)
        `when`(editor.putString(anyString(), anyString())).thenReturn(editor)
        `when`(editor.putFloat(anyString(), anyFloat())).thenReturn(editor)
        `when`(editor.apply()).then { }

        // Default return values
        `when`(sharedPreferences.getBoolean(eq("rag_enabled"), anyBoolean())).thenReturn(false)
        `when`(sharedPreferences.getString(eq("rag_document_ids"), isNull())).thenReturn(null)
        `when`(sharedPreferences.getFloat(eq("rag_threshold"), anyFloat())).thenReturn(0.7f)

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
        verify(editor).putBoolean("rag_enabled", true)
        verify(editor).apply()
    }

    @Test
    fun `ragEnabled StateFlow updates when changed`() = runTest {
        `when`(sharedPreferences.getBoolean(eq("rag_enabled"), anyBoolean())).thenReturn(true)

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
        `when`(sharedPreferences.getString(eq("rag_document_ids"), isNull()))
            .thenReturn("doc1,doc2,doc3")

        val ids = chatPreferences.getSelectedDocumentIds()
        assertEquals(listOf("doc1", "doc2", "doc3"), ids)
    }

    @Test
    fun `setSelectedDocumentIds serializes list to comma-separated string`() {
        val documentIds = listOf("doc1", "doc2", "doc3")
        chatPreferences.setSelectedDocumentIds(documentIds)

        verify(editor).putString("rag_document_ids", "doc1,doc2,doc3")
        verify(editor).apply()
    }

    @Test
    fun `setSelectedDocumentIds handles empty list`() {
        chatPreferences.setSelectedDocumentIds(emptyList())

        verify(editor).putString("rag_document_ids", "")
        verify(editor).apply()
    }

    @Test
    fun `setSelectedDocumentIds handles single document`() {
        chatPreferences.setSelectedDocumentIds(listOf("doc1"))

        verify(editor).putString("rag_document_ids", "doc1")
        verify(editor).apply()
    }

    @Test
    fun `getSelectedDocumentIds filters blank entries`() {
        `when`(sharedPreferences.getString(eq("rag_document_ids"), isNull()))
            .thenReturn("doc1,,doc2,  ,doc3")

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
        verify(editor).putFloat("rag_threshold", 0.8f)
        verify(editor).apply()
    }

    @Test
    fun `setRagThreshold clamps value to 0_0 - 1_0 range`() {
        // Test lower bound
        chatPreferences.setRagThreshold(-0.5f)
        verify(editor).putFloat("rag_threshold", 0.0f)

        // Test upper bound
        clearInvocations(editor)
        chatPreferences.setRagThreshold(1.5f)
        verify(editor).putFloat("rag_threshold", 1.0f)
    }

    @Test
    fun `setRagThreshold accepts valid values`() {
        // Test minimum
        chatPreferences.setRagThreshold(0.0f)
        verify(editor).putFloat("rag_threshold", 0.0f)

        // Test mid-range
        clearInvocations(editor)
        chatPreferences.setRagThreshold(0.5f)
        verify(editor).putFloat("rag_threshold", 0.5f)

        // Test maximum
        clearInvocations(editor)
        chatPreferences.setRagThreshold(1.0f)
        verify(editor).putFloat("rag_threshold", 1.0f)
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
        verify(editor).putBoolean("rag_enabled", true)
        verify(editor).putString("rag_document_ids", "doc1,doc2,doc3")
        verify(editor).putFloat("rag_threshold", 0.75f)
        verify(editor, times(3)).apply()
    }

    @Test
    fun `RAG settings are independent from other preferences`() {
        // Set RAG settings
        chatPreferences.setRagEnabled(true)
        chatPreferences.setSelectedDocumentIds(listOf("doc1"))
        chatPreferences.setRagThreshold(0.8f)

        // Verify RAG keys are distinct
        verify(editor, never()).putBoolean(eq("conversation_mode"), anyBoolean())
        verify(editor, never()).putFloat(eq("confidence_threshold"), anyFloat())
    }
}
