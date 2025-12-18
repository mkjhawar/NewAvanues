# Developer Manual - Chapter 49: Advanced RAG Features

**Date**: 2025-11-22
**Status**: 📋 Phase 3.0 Planning Document
**Target Implementation**: Q1 2026

---

## Executive Summary

This chapter documents advanced RAG (Retrieval-Augmented Generation) features planned for Phase 3.0, including document preview, advanced filtering, bookmarks/favorites, and annotation system.

---

## 1. Document Preview System

### 1.1 Implementation Strategy

**File**: `DocumentPreviewManager.kt`

```kotlin
class DocumentPreviewManager {
    suspend fun loadPreview(documentId: String, context: String? = null): DocumentPreview {
        val document = documentRepository.getDocument(documentId)
        val chunk = context?.let { findChunkNearContext(document, it) }

        return DocumentPreview(
            documentId = documentId,
            title = document.title,
            previewText = chunk?.text ?: document.summary,
            startOffset = chunk?.startOffset ?: 0,
            imagePreviewUrl = document.imagePreviewUrl,
            metadata = document.metadata,
            sourceUrl = document.sourceUrl
        )
    }

    private fun findChunkNearContext(doc: Document, context: String): DocumentChunk? {
        // Find chunk semantically similar to context
        return doc.chunks.maxByOrNull { chunk ->
            calculateSimilarity(context, chunk.text)
        }
    }
}
```

### 1.2 UI Component

```kotlin
@Composable
fun DocumentPreviewPanel(documentId: String) {
    val viewModel: PreviewViewModel = viewModel()
    val preview by viewModel.preview.collectAsState()

    when (preview) {
        is PreviewState.Loading -> LoadingPreview()
        is PreviewState.Success -> {
            val doc = (preview as PreviewState.Success).document
            DocumentPreviewContent(doc)
        }
        is PreviewState.Error -> ErrorPreview()
    }
}

@Composable
fun DocumentPreviewContent(preview: DocumentPreview) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Title
        Text(preview.title, style = MaterialTheme.typography.titleLarge)

        // Image preview
        preview.imagePreviewUrl?.let { url ->
            AsyncImage(url, contentDescription = "Document preview")
        }

        // Text excerpt
        Text(preview.previewText, style = MaterialTheme.typography.bodyMedium)

        // Metadata
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(label = { Text(preview.metadata["type"] ?: "Document") })
            Chip(label = { Text(preview.metadata["date"] ?: "Unknown") })
        }

        // Source link
        preview.sourceUrl?.let { url ->
            Button(onClick = { openUrl(url) }) {
                Text("View Source")
            }
        }
    }
}
```

---

## 2. Advanced Filtering System

### 2.1 Filter Engine

**File**: `DocumentFilterEngine.kt`

```kotlin
data class FilterCriteria(
    val searchQuery: String? = null,
    val documentTypes: Set<String> = setOf(),
    val dateRange: DateRange? = null,
    val tags: Set<String> = setOf(),
    val minRelevance: Float = 0.0f,
    val sources: Set<String> = setOf(),
    val language: String? = null,
    val maxAge: Duration? = null
)

class DocumentFilterEngine(
    private val documentRepository: DocumentRepository,
    private val embeddingProvider: EmbeddingProvider
) {
    suspend fun search(criteria: FilterCriteria): List<SearchResult> {
        var documents = documentRepository.getAllDocuments()

        // Apply filters
        if (criteria.searchQuery != null) {
            val embedding = embeddingProvider.encode(criteria.searchQuery)
            documents = documents.map { doc ->
                doc to calculateSimilarity(embedding, doc.embedding)
            }.filter { (_, score) ->
                score >= criteria.minRelevance
            }.sortedByDescending { (_, score) -> score }
             .map { (doc, score) -> doc }
        }

        if (criteria.documentTypes.isNotEmpty()) {
            documents = documents.filter { doc ->
                criteria.documentTypes.contains(doc.type)
            }
        }

        if (criteria.dateRange != null) {
            documents = documents.filter { doc ->
                doc.createdDate in criteria.dateRange
            }
        }

        if (criteria.tags.isNotEmpty()) {
            documents = documents.filter { doc ->
                doc.tags.any { criteria.tags.contains(it) }
            }
        }

        if (criteria.sources.isNotEmpty()) {
            documents = documents.filter { doc ->
                criteria.sources.contains(doc.source)
            }
        }

        if (criteria.language != null) {
            documents = documents.filter { doc ->
                doc.language == criteria.language
            }
        }

        if (criteria.maxAge != null) {
            val cutoffDate = Clock.System.now() - criteria.maxAge
            documents = documents.filter { doc ->
                doc.updatedDate >= cutoffDate
            }
        }

        return documents.map { SearchResult(it) }
    }
}
```

### 2.2 UI Component

```kotlin
@Composable
fun AdvancedFilterPanel(onApplyFilter: (FilterCriteria) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypes by remember { mutableStateOf(setOf<String>()) }
    var selectedSources by remember { mutableStateOf(setOf<String>()) }
    var dateRange by remember { mutableStateOf<DateRange?>(null) }
    var minRelevance by remember { mutableStateOf(0.5f) }

    Column(modifier = Modifier.padding(16.dp)) {
        // Search query
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search") }
        )

        // Document type filter
        DocumentTypeFilter(
            selected = selectedTypes,
            onSelectionChange = { selectedTypes = it }
        )

        // Source filter
        SourceFilter(
            selected = selectedSources,
            onSelectionChange = { selectedSources = it }
        )

        // Date range picker
        DateRangePickerPanel(
            range = dateRange,
            onRangeChange = { dateRange = it }
        )

        // Relevance slider
        Slider(
            value = minRelevance,
            onValueChange = { minRelevance = it },
            valueRange = 0f..1f,
            steps = 9
        )

        Button(
            onClick = {
                val criteria = FilterCriteria(
                    searchQuery = searchQuery.ifBlank { null },
                    documentTypes = selectedTypes,
                    dateRange = dateRange,
                    sources = selectedSources,
                    minRelevance = minRelevance
                )
                onApplyFilter(criteria)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Apply Filters")
        }
    }
}
```

---

## 3. Bookmarks & Favorites System

### 3.1 Data Model

```kotlin
@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val documentId: String,
    val userId: String,
    val content: String, // Bookmarked text/chunk
    val startOffset: Int,
    val endOffset: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList(),
    val color: String = "#FFEB3B", // Bookmark color
    val notes: String = ""
)

@Entity(tableName = "favorites")
data class Favorite(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val documentId: String,
    val userId: String,
    val addedAt: Long = System.currentTimeMillis(),
    val rating: Int = 0, // 0-5 stars
    val personalNotes: String = ""
)
```

### 3.2 Repository Implementation

```kotlin
class BookmarkRepository(
    private val bookmarkDao: BookmarkDao,
    private val favoriteDao: FavoriteDao
) {
    suspend fun addBookmark(bookmark: Bookmark) {
        bookmarkDao.insert(bookmark)
    }

    suspend fun removeBookmark(bookmarkId: String) {
        bookmarkDao.delete(bookmarkId)
    }

    suspend fun updateBookmark(bookmark: Bookmark) {
        bookmarkDao.update(bookmark)
    }

    suspend fun getBookmarks(documentId: String): List<Bookmark> {
        return bookmarkDao.getByDocument(documentId)
    }

    suspend fun getAllBookmarks(): List<Bookmark> {
        return bookmarkDao.getAll()
    }

    suspend fun addFavorite(documentId: String) {
        val favorite = Favorite(documentId = documentId)
        favoriteDao.insert(favorite)
    }

    suspend fun removeFavorite(documentId: String) {
        favoriteDao.delete(documentId)
    }

    suspend fun updateFavoriteRating(documentId: String, rating: Int) {
        favoriteDao.updateRating(documentId, rating)
    }

    suspend fun isFavorite(documentId: String): Boolean {
        return favoriteDao.isFavorite(documentId)
    }

    suspend fun getFavorites(): List<Favorite> {
        return favoriteDao.getAll()
    }
}
```

### 3.3 UI Component

```kotlin
@Composable
fun BookmarksPanel(documentId: String) {
    val viewModel: BookmarkViewModel = viewModel()
    val bookmarks by viewModel.getBookmarks(documentId).collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Bookmarks", style = MaterialTheme.typography.titleMedium)

        LazyColumn {
            items(bookmarks) { bookmark ->
                BookmarkItem(
                    bookmark = bookmark,
                    onUpdate = { updated -> viewModel.updateBookmark(updated) },
                    onDelete = { viewModel.removeBookmark(bookmark.id) }
                )
            }
        }
    }
}

@Composable
fun BookmarkItem(
    bookmark: Bookmark,
    onUpdate: (Bookmark) -> Unit,
    onDelete: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(android.graphics.Color.parseColor(bookmark.color)).copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = bookmark.content, style = MaterialTheme.typography.bodySmall)

            if (bookmark.notes.isNotEmpty()) {
                Text(
                    text = "Notes: ${bookmark.notes}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = SimpleDateFormat("MMM d, yyyy").format(bookmark.createdAt),
                    style = MaterialTheme.typography.labelSmall
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        EditBookmarkDialog(
            bookmark = bookmark,
            onSave = { updated ->
                onUpdate(updated)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }
}

@Composable
fun FavoriteButton(documentId: String) {
    val viewModel: FavoriteViewModel = viewModel()
    val isFavorite by viewModel.isFavorite(documentId).collectAsState(initial = false)
    var showRatingDialog by remember { mutableStateOf(false) }

    IconButton(
        onClick = {
            if (isFavorite) {
                viewModel.removeFavorite(documentId)
            } else {
                showRatingDialog = true
            }
        }
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.Favorite,
            contentDescription = "Add to favorites"
        )
    }

    if (showRatingDialog) {
        RatingDialog(
            onRate = { rating ->
                viewModel.addFavorite(documentId, rating)
                showRatingDialog = false
            },
            onDismiss = { showRatingDialog = false }
        )
    }
}
```

---

## 4. Document Annotation System

### 4.1 Annotation Models

```kotlin
@Entity(tableName = "annotations")
data class Annotation(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val documentId: String,
    val startOffset: Int,
    val endOffset: Int,
    val text: String,
    val annotationType: AnnotationType,
    val content: String, // Annotation comment
    val color: String = "#FFEB3B",
    val createdAt: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList()
)

enum class AnnotationType {
    HIGHLIGHT,
    COMMENT,
    QUESTION,
    IMPORTANT,
    TODO
}
```

### 4.2 Annotation Manager

```kotlin
class AnnotationManager(
    private val annotationDao: AnnotationDao,
    private val documentRepository: DocumentRepository
) {
    suspend fun addAnnotation(annotation: Annotation) {
        annotationDao.insert(annotation)
    }

    suspend fun getAnnotations(documentId: String): List<Annotation> {
        return annotationDao.getByDocument(documentId)
    }

    suspend fun updateAnnotation(annotation: Annotation) {
        annotationDao.update(annotation)
    }

    suspend fun deleteAnnotation(annotationId: String) {
        annotationDao.delete(annotationId)
    }

    suspend fun exportAnnotations(documentId: String): String {
        val annotations = getAnnotations(documentId)
        val document = documentRepository.getDocument(documentId)

        return buildString {
            appendLine("# Annotations for: ${document.title}")
            appendLine()

            annotations.groupBy { it.annotationType }.forEach { (type, items) ->
                appendLine("## $type")
                items.forEach { annotation ->
                    appendLine("- **[${annotation.startOffset}-${annotation.endOffset}]** \"${annotation.text}\"")
                    appendLine("  ${annotation.content}")
                    appendLine()
                }
            }
        }
    }
}
```

---

## 5. Full-Text Search Optimization

### 5.1 Full-Text Search Index

```kotlin
@Entity(
    tableName = "documents_fts",
    foreignKeys = [
        ForeignKey(
            entity = Document::class,
            parentColumns = ["id"],
            childColumns = ["docId"]
        )
    ]
)
data class DocumentFullTextSearch(
    @PrimaryKey
    val id: String,
    val docId: String,
    val title: String,
    val content: String,
    val metadata: String
)

@Dao
interface DocumentFtsDao {
    @Insert
    suspend fun insertFts(document: DocumentFullTextSearch)

    @Query("""
        SELECT * FROM documents_fts
        WHERE documents_fts MATCH :query
        ORDER BY rank
        LIMIT :limit
    """)
    suspend fun search(query: String, limit: Int = 20): List<DocumentFullTextSearch>
}
```

### 5.2 Hybrid Search (Semantic + Keyword)

```kotlin
class HybridSearchEngine(
    private val embeddingProvider: EmbeddingProvider,
    private val ftsDao: DocumentFtsDao,
    private val documentRepository: DocumentRepository
) {
    suspend fun search(query: String, limit: Int = 10): List<SearchResult> {
        // Parallel execution
        val semanticResults = async {
            semanticSearch(query, limit)
        }
        val keywordResults = async {
            keywordSearch(query, limit)
        }

        val semantic = semanticResults.await()
        val keyword = keywordResults.await()

        // Merge and rank results
        return mergeResults(semantic, keyword)
    }

    private suspend fun semanticSearch(
        query: String,
        limit: Int
    ): List<SearchResult> {
        val embedding = embeddingProvider.encode(query)
        return documentRepository.searchByEmbedding(embedding, limit)
    }

    private suspend fun keywordSearch(
        query: String,
        limit: Int
    ): List<SearchResult> {
        val ftsResults = ftsDao.search(query, limit)
        return ftsResults.map { SearchResult(it) }
    }

    private fun mergeResults(
        semantic: List<SearchResult>,
        keyword: List<SearchResult>
    ): List<SearchResult> {
        val combined = (semantic + keyword)
            .groupBy { it.documentId }
            .map { (_, results) ->
                results[0].copy(
                    score = (results.map { it.score }.average()).toFloat()
                )
            }
            .sortedByDescending { it.score }

        return combined
    }
}
```

---

## 6. Performance Metrics

**Target Metrics**:
- Document preview load: <200ms
- Filter execution: <500ms
- Search operation: <1000ms
- Bookmark operations: <100ms
- Annotation save: <50ms

---

## 7. Testing

```kotlin
class AdvancedRAGTests {
    @Test
    fun testDocumentPreview() = runTest {
        val manager = DocumentPreviewManager(mockRepository)
        val preview = manager.loadPreview("doc-123")

        assert(preview.title.isNotEmpty())
    }

    @Test
    fun testAdvancedFiltering() = runTest {
        val engine = DocumentFilterEngine(mockRepository, mockEmbedding)
        val criteria = FilterCriteria(
            searchQuery = "neural networks",
            documentTypes = setOf("PDF", "Article")
        )

        val results = engine.search(criteria)
        assert(results.all { it.type in setOf("PDF", "Article") })
    }

    @Test
    fun testBookmarkOperations() = runTest {
        val repo = BookmarkRepository(mockDao, mockFavDao)
        val bookmark = Bookmark(documentId = "doc-1", content = "Important text")

        repo.addBookmark(bookmark)
        val retrieved = repo.getBookmarks("doc-1")

        assert(retrieved.isNotEmpty())
    }
}
```

---

**Created by**: Agent 6 (AI Assistant)
**Framework**: IDEACODE v8.4
**Status**: 📋 Planning Document for Phase 3.0 Implementation
**Last Updated**: 2025-11-22

