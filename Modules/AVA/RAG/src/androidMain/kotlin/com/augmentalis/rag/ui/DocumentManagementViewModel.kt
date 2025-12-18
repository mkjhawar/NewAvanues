// filename: Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/ui/DocumentManagementViewModel.kt
// created: 2025-11-06
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.augmentalis.rag.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

/**
 * ViewModel for RAG document management
 *
 * Handles:
 * - Document list display
 * - Document addition (local files, URLs)
 * - Document deletion
 * - Processing status tracking
 * - Cluster management
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun DocumentScreen(viewModel: DocumentManagementViewModel) {
 *     val documents by viewModel.documents.collectAsState()
 *     val isProcessing by viewModel.isProcessing.collectAsState()
 *
 *     DocumentManagementScreen(
 *         documents = documents,
 *         onAddDocument = { file -> viewModel.addDocument(file) },
 *         onDeleteDocument = { doc -> viewModel.deleteDocument(doc) }
 *     )
 * }
 * ```
 */
class DocumentManagementViewModel(
    private val ragRepository: RAGRepository
) : ViewModel() {

    // UI State
    private val _documents = MutableStateFlow<List<Document>>(emptyList())
    val documents: StateFlow<List<Document>> = _documents.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _processingProgress = MutableStateFlow(0f)
    val processingProgress: StateFlow<Float> = _processingProgress.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _clusterStats = MutableStateFlow<ClusteringStats?>(null)
    val clusterStats: StateFlow<ClusteringStats?> = _clusterStats.asStateFlow()

    init {
        loadDocuments()
    }

    /**
     * Load all documents from repository
     */
    fun loadDocuments() {
        viewModelScope.launch {
            try {
                ragRepository.listDocuments().collect { document ->
                    _documents.value = _documents.value + document
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load documents")
                _error.value = "Failed to load documents: ${e.message}"
            }
        }
    }

    /**
     * Add document from local file
     *
     * @param filePath Path to local file
     * @param title Optional custom title
     */
    fun addDocument(filePath: String, title: String? = null) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processingProgress.value = 0f
            _statusMessage.value = "Adding document..."

            try {
                val file = File(filePath)
                if (!file.exists()) {
                    _error.value = "File not found: $filePath"
                    _isProcessing.value = false
                    return@launch
                }

                // Detect document type from extension
                val documentType = detectDocumentType(file.extension)
                if (documentType == null) {
                    _error.value = "Unsupported file type: ${file.extension}"
                    _isProcessing.value = false
                    return@launch
                }

                // Add document
                val request = AddDocumentRequest(
                    filePath = filePath,
                    title = title ?: file.nameWithoutExtension,
                    processImmediately = true
                )

                val result = ragRepository.addDocument(request)
                result.fold(
                    onSuccess = { addResult ->
                        _statusMessage.value = "Document added: ${addResult.documentId}"
                        _documents.value = emptyList()
                        loadDocuments()

                        // Auto-rebuild clusters if needed
                        checkAndRebuildClusters()
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to add document")
                        _error.value = "Failed to add document: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Document addition failed")
                _error.value = "Error: ${e.message}"
            } finally {
                _isProcessing.value = false
                _processingProgress.value = 1f
            }
        }
    }

    /**
     * Add document from URL
     *
     * @param url Web document URL
     * @param title Optional custom title
     */
    fun addDocumentFromUrl(url: String, title: String? = null) {
        viewModelScope.launch {
            _isProcessing.value = true
            _statusMessage.value = "Fetching web document..."

            try {
                val request = AddDocumentRequest(
                    filePath = url,
                    title = title ?: extractTitleFromUrl(url),
                    processImmediately = true
                )

                val result = ragRepository.addDocument(request)
                result.fold(
                    onSuccess = { addResult ->
                        _statusMessage.value = "Web document added: ${addResult.documentId}"
                        _documents.value = emptyList()
                        loadDocuments()
                        checkAndRebuildClusters()
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to add web document")
                        _error.value = "Failed to add web document: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Web document addition failed")
                _error.value = "Error: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Delete document and its chunks
     *
     * @param document Document to delete
     */
    fun deleteDocument(document: Document) {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Deleting document..."

                val result = ragRepository.deleteDocument(document.id)
                result.fold(
                    onSuccess = {
                        _statusMessage.value = "Document deleted: ${document.title}"
                        _documents.value = _documents.value.filter { it.id != document.id }

                        // Rebuild clusters after deletion
                        checkAndRebuildClusters()
                    },
                    onFailure = { error ->
                        Timber.e(error, "Failed to delete document")
                        _error.value = "Failed to delete: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Document deletion failed")
                _error.value = "Error: ${e.message}"
            }
        }
    }

    /**
     * Rebuild clusters (after bulk operations)
     */
    fun rebuildClusters() {
        viewModelScope.launch {
            _isProcessing.value = true
            _statusMessage.value = "Reprocessing documents..."

            try {
                val result = ragRepository.processDocuments()
                result.fold(
                    onSuccess = { processedCount ->
                        _statusMessage.value = "Processed $processedCount documents"
                        Timber.i("Document reprocessing completed: $processedCount documents")
                        // Reload documents to get updated stats
                        loadDocuments()
                    },
                    onFailure = { error ->
                        Timber.e(error, "Document reprocessing failed")
                        _error.value = "Failed to reprocess documents: ${error.message}"
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Document reprocessing error")
                _error.value = "Error: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Clear status message
     */
    fun clearStatus() {
        _statusMessage.value = null
    }

    /**
     * Auto-rebuild clusters if chunk count increased significantly
     */
    private suspend fun checkAndRebuildClusters() {
        val stats = _clusterStats.value
        val totalChunks = _documents.value.sumOf { it.chunkCount }

        // Rebuild if chunk count increased >20% or no clusters exist
        if (stats == null || (totalChunks > stats.chunkCount * 1.2)) {
            rebuildClusters()
        }
    }

    /**
     * Detect document type from file extension
     */
    private fun detectDocumentType(extension: String): DocumentType? {
        return when (extension.lowercase()) {
            "pdf" -> DocumentType.PDF
            "docx" -> DocumentType.DOCX
            "doc" -> DocumentType.DOCX
            "txt" -> DocumentType.TXT
            "html", "htm" -> DocumentType.HTML
            "md", "markdown" -> DocumentType.MD
            "rtf" -> DocumentType.RTF
            else -> null
        }
    }

    /**
     * Extract title from URL
     */
    private fun extractTitleFromUrl(url: String): String {
        return try {
            val uri = java.net.URI(url)
            val path = uri.path.split("/").lastOrNull { it.isNotBlank() }
            path ?: uri.host ?: "Web Document"
        } catch (e: Exception) {
            "Web Document"
        }
    }
}

/**
 * UI state for clustering stats
 */
data class ClusteringStats(
    val clusterCount: Int,
    val chunkCount: Int,
    val timeMs: Long,
    val avgClusterSize: Int = chunkCount / clusterCount.coerceAtLeast(1)
)
