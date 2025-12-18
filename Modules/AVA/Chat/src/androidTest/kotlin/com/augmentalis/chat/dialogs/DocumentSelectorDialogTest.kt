// filename: Universal/AVA/Features/Chat/src/androidTest/kotlin/com/augmentalis/ava/features/chat/ui/dialogs/DocumentSelectorDialogTest.kt
// created: 2025-11-22
// author: RAG Settings Integration Specialist
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.chat.dialogs

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.rag.domain.Document
import com.augmentalis.rag.domain.DocumentStatus
import com.augmentalis.rag.domain.DocumentType
import kotlinx.datetime.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for DocumentSelectorDialog composable (Phase 2 - Task 1)
 *
 * Tests:
 * - Dialog rendering
 * - Document list display
 * - Multi-select functionality
 * - Search/filter
 * - Empty states
 * - Confirm/Cancel actions
 */
@RunWith(AndroidJUnit4::class)
class DocumentSelectorDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createTestDocument(
        id: String,
        title: String,
        sizeBytes: Long = 1024,
        status: DocumentStatus = DocumentStatus.INDEXED
    ): Document {
        val now = Instant.parse("2025-11-22T12:00:00Z")
        return Document(
            id = id,
            title = title,
            filePath = "/path/to/$title",
            fileType = DocumentType.PDF,
            sizeBytes = sizeBytes,
            createdAt = now,
            modifiedAt = now,
            indexedAt = now,
            chunkCount = 10,
            status = status
        )
    }

    @Test
    fun dialog_displaysCorrectly() {
        val documents = listOf(
            createTestDocument("doc1", "Document 1"),
            createTestDocument("doc2", "Document 2")
        )

        composeTestRule.setContent {
            DocumentSelectorDialog(
                documents = documents,
                selectedDocumentIds = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        // Verify dialog title
        composeTestRule.onNodeWithText("Select Documents")
            .assertIsDisplayed()

        // Verify selection count
        composeTestRule.onNodeWithText("0 document(s) selected")
            .assertIsDisplayed()

        // Verify search field
        composeTestRule.onNodeWithText("Search documents...")
            .assertIsDisplayed()

        // Verify confirm/cancel buttons
        composeTestRule.onNodeWithText("Confirm")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel")
            .assertIsDisplayed()
    }

    @Test
    fun dialog_displaysDocuments() {
        val documents = listOf(
            createTestDocument("doc1", "Test Document 1"),
            createTestDocument("doc2", "Test Document 2"),
            createTestDocument("doc3", "Test Document 3")
        )

        composeTestRule.setContent {
            DocumentSelectorDialog(
                documents = documents,
                selectedDocumentIds = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        // Verify all documents are displayed
        composeTestRule.onNodeWithText("Test Document 1")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Document 2")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Document 3")
            .assertIsDisplayed()
    }

    @Test
    fun dialog_showsOnlyIndexedDocuments() {
        val documents = listOf(
            createTestDocument("doc1", "Indexed Doc", status = DocumentStatus.INDEXED),
            createTestDocument("doc2", "Pending Doc", status = DocumentStatus.PENDING),
            createTestDocument("doc3", "Failed Doc", status = DocumentStatus.FAILED)
        )

        composeTestRule.setContent {
            DocumentSelectorDialog(
                documents = documents,
                selectedDocumentIds = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        // Only indexed document should be displayed
        composeTestRule.onNodeWithText("Indexed Doc")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Pending Doc")
            .assertDoesNotExist()
        composeTestRule.onNodeWithText("Failed Doc")
            .assertDoesNotExist()
    }

    @Test
    fun documentSelection_updatesSelectionCount() {
        val documents = listOf(
            createTestDocument("doc1", "Document 1"),
            createTestDocument("doc2", "Document 2")
        )

        composeTestRule.setContent {
            DocumentSelectorDialog(
                documents = documents,
                selectedDocumentIds = listOf("doc1"),
                onDismiss = {},
                onConfirm = {}
            )
        }

        // Verify initial selection count
        composeTestRule.onNodeWithText("1 document(s) selected")
            .assertIsDisplayed()
    }

    @Test
    fun documentItem_displaysMetadata() {
        val document = createTestDocument(
            id = "doc1",
            title = "Test Document",
            sizeBytes = 1024 * 1024 // 1 MB
        )

        composeTestRule.setContent {
            DocumentSelectorDialog(
                documents = listOf(document),
                selectedDocumentIds = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        // Verify metadata is displayed
        composeTestRule.onNodeWithText("PDF")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("1 MB")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("10 chunks")
            .assertIsDisplayed()
    }

    @Test
    fun searchField_filtersDocuments() {
        val documents = listOf(
            createTestDocument("doc1", "Android Development"),
            createTestDocument("doc2", "iOS Development"),
            createTestDocument("doc3", "Android Testing")
        )

        composeTestRule.setContent {
            DocumentSelectorDialog(
                documents = documents,
                selectedDocumentIds = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        // Type search query
        composeTestRule.onNodeWithText("Search documents...")
            .performTextInput("Android")

        // Verify filtered results
        composeTestRule.onNodeWithText("Android Development")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Android Testing")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("iOS Development")
            .assertDoesNotExist()
    }

    @Test
    fun searchField_clearButton_clearsSearch() {
        val documents = listOf(
            createTestDocument("doc1", "Test Document")
        )

        composeTestRule.setContent {
            DocumentSelectorDialog(
                documents = documents,
                selectedDocumentIds = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        // Type search query
        composeTestRule.onNodeWithText("Search documents...")
            .performTextInput("query")

        // Click clear button
        composeTestRule.onNodeWithContentDescription("Clear search")
            .performClick()

        // Verify search is cleared
        composeTestRule.onNodeWithText("query")
            .assertDoesNotExist()
    }

    @Test
    fun emptyState_whenNoDocuments_isDisplayed() {
        composeTestRule.setContent {
            DocumentSelectorDialog(
                documents = emptyList(),
                selectedDocumentIds = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        // Verify empty state message
        composeTestRule.onNodeWithText("No indexed documents available")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Add and index documents first")
            .assertIsDisplayed()
    }

    @Test
    fun emptyState_whenSearchNoResults_isDisplayed() {
        val documents = listOf(
            createTestDocument("doc1", "Test Document")
        )

        composeTestRule.setContent {
            DocumentSelectorDialog(
                documents = documents,
                selectedDocumentIds = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        // Search for non-existent document
        composeTestRule.onNodeWithText("Search documents...")
            .performTextInput("NonExistent")

        // Verify search empty state
        composeTestRule.onNodeWithText("No documents match your search")
            .assertIsDisplayed()
    }

    @Test
    fun confirmButton_whenNoDocuments_isDisabled() {
        composeTestRule.setContent {
            DocumentSelectorDialog(
                documents = emptyList(),
                selectedDocumentIds = emptyList(),
                onDismiss = {},
                onConfirm = {}
            )
        }

        // Confirm button should be disabled
        composeTestRule.onNodeWithText("Confirm")
            .assertIsNotEnabled()
    }

    @Test
    fun confirmButton_whenClicked_callsCallback() {
        val documents = listOf(
            createTestDocument("doc1", "Document 1")
        )
        var confirmedIds: List<String>? = null

        composeTestRule.setContent {
            DocumentSelectorDialog(
                documents = documents,
                selectedDocumentIds = listOf("doc1"),
                onDismiss = {},
                onConfirm = { confirmedIds = it }
            )
        }

        // Click confirm
        composeTestRule.onNodeWithText("Confirm")
            .performClick()

        // Verify callback was called with correct IDs
        assert(confirmedIds == listOf("doc1")) {
            "Expected confirmed IDs to be [doc1], got $confirmedIds"
        }
    }

    @Test
    fun cancelButton_whenClicked_callsCallback() {
        val documents = listOf(
            createTestDocument("doc1", "Document 1")
        )
        var dismissCalled = false

        composeTestRule.setContent {
            DocumentSelectorDialog(
                documents = documents,
                selectedDocumentIds = emptyList(),
                onDismiss = { dismissCalled = true },
                onConfirm = {}
            )
        }

        // Click cancel
        composeTestRule.onNodeWithText("Cancel")
            .performClick()

        // Verify dismiss callback was called
        assert(dismissCalled) { "onDismiss callback was not called" }
    }
}
