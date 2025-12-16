// filename: Universal/AVA/Features/RAG/src/commonMain/kotlin/com/augmentalis/ava/features/rag/domain/Document.kt
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.rag.domain

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Represents a document in the RAG system
 *
 * Documents are the top-level entities that get indexed and searched.
 * Each document is split into chunks for embedding and retrieval.
 */
@Serializable
data class Document(
    val id: String,
    val title: String,
    val filePath: String,
    val fileType: DocumentType,
    val sizeBytes: Long,
    val createdAt: Instant,
    val modifiedAt: Instant,
    val indexedAt: Instant? = null,
    val chunkCount: Int = 0,
    val metadata: Map<String, String> = emptyMap(),
    val status: DocumentStatus = DocumentStatus.PENDING
)

/**
 * Document types supported by the RAG system
 */
@Serializable
enum class DocumentType(val extension: String, val mimeType: String) {
    PDF("pdf", "application/pdf"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    TXT("txt", "text/plain"),
    MD("md", "text/markdown"),
    HTML("html", "text/html"),
    EPUB("epub", "application/epub+zip"),
    RTF("rtf", "application/rtf");

    companion object {
        fun fromExtension(extension: String): DocumentType? {
            return entries.find { it.extension.equals(extension, ignoreCase = true) }
        }

        fun fromMimeType(mimeType: String): DocumentType? {
            return entries.find { it.mimeType.equals(mimeType, ignoreCase = true) }
        }
    }
}

/**
 * Document processing status
 */
@Serializable
enum class DocumentStatus {
    /** Document added but not yet processed */
    PENDING,

    /** Document is currently being processed */
    PROCESSING,

    /** Document successfully indexed */
    INDEXED,

    /** Document failed to process */
    FAILED,

    /** Document has been updated and needs re-indexing */
    OUTDATED,

    /** Document has been deleted */
    DELETED
}

/**
 * Request to add a new document to the RAG system
 */
@Serializable
data class AddDocumentRequest(
    val filePath: String,
    val title: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val processImmediately: Boolean = false
)

/**
 * Result of adding a document
 */
@Serializable
data class AddDocumentResult(
    val documentId: String,
    val status: DocumentStatus,
    val message: String? = null
)
