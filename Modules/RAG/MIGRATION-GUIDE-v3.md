# RAG Database Migration Guide (v2 → v3)

**Version:** 2.0 → 3.0
**Date:** 2025-11-22
**Feature:** Phase 3.0 Advanced RAG Features

---

## Overview

Database version 3 adds support for:
- **Bookmarks** - Save favorite documents and chunks
- **Annotations** - Highlight and annotate document content
- **Filter Presets** - Save frequently used search filters

---

## Migration Process

### Automatic Migration

The migration runs **automatically** when the app starts with the new version.

```kotlin
// Migration is already configured in RAGDatabase.kt
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Creates 3 new tables
        // Creates 15 indices
        // All existing data preserved
    }
}
```

### Manual Testing

To test the migration manually:

```kotlin
// 1. Install old version (database v2)
// 2. Add some documents and chunks
// 3. Upgrade to new version (database v3)
// 4. Verify data integrity

@Test
fun testMigration_2_to_3() = runBlocking {
    // Create v2 database
    val dbV2 = Room.databaseBuilder(context, RAGDatabase::class.java, "test.db")
        .createFromAsset("database/v2.db")
        .build()

    // Add test data
    val document = DocumentEntity(...)
    dbV2.documentDao().insertDocument(document)

    // Close v2
    dbV2.close()

    // Open as v3 (migration runs)
    val dbV3 = Room.databaseBuilder(context, RAGDatabase::class.java, "test.db")
        .addMigrations(MIGRATION_2_3)
        .build()

    // Verify data preserved
    val docs = dbV3.documentDao().getAllDocumentsSync()
    assertEquals(1, docs.size)

    // Verify new tables exist
    val bookmarks = dbV3.bookmarkDao().getAllBookmarksSync()
    assertEquals(0, bookmarks.size) // Empty on first migration

    dbV3.close()
}
```

---

## Schema Changes

### New Tables

#### 1. bookmarks

```sql
CREATE TABLE bookmarks (
    id TEXT PRIMARY KEY NOT NULL,
    type TEXT NOT NULL,
    document_id TEXT NOT NULL,
    chunk_id TEXT,
    title TEXT NOT NULL,
    description TEXT,
    tags_json TEXT NOT NULL,
    created_timestamp TEXT NOT NULL,
    last_accessed_timestamp TEXT,
    access_count INTEGER NOT NULL DEFAULT 0,
    is_pinned INTEGER NOT NULL DEFAULT 0,
    color TEXT NOT NULL,
    FOREIGN KEY(document_id) REFERENCES documents(id) ON DELETE CASCADE
)
```

#### 2. annotations

```sql
CREATE TABLE annotations (
    id TEXT PRIMARY KEY NOT NULL,
    chunk_id TEXT NOT NULL,
    document_id TEXT NOT NULL,
    type TEXT NOT NULL,
    highlighted_text TEXT NOT NULL,
    start_offset INTEGER NOT NULL,
    end_offset INTEGER NOT NULL,
    note TEXT,
    context TEXT,
    tags_json TEXT NOT NULL,
    created_timestamp TEXT NOT NULL,
    modified_timestamp TEXT NOT NULL,
    color TEXT NOT NULL,
    FOREIGN KEY(document_id) REFERENCES documents(id) ON DELETE CASCADE,
    FOREIGN KEY(chunk_id) REFERENCES chunks(id) ON DELETE CASCADE
)
```

#### 3. filter_presets

```sql
CREATE TABLE filter_presets (
    id TEXT PRIMARY KEY NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    filters_json TEXT NOT NULL,
    created_timestamp TEXT NOT NULL,
    is_pinned INTEGER NOT NULL DEFAULT 0,
    use_count INTEGER NOT NULL DEFAULT 0
)
```

### New Indices

```sql
-- Bookmarks
CREATE INDEX index_bookmarks_document_id ON bookmarks(document_id)
CREATE INDEX index_bookmarks_chunk_id ON bookmarks(chunk_id)
CREATE INDEX index_bookmarks_type ON bookmarks(type)
CREATE INDEX index_bookmarks_is_pinned ON bookmarks(is_pinned)
CREATE INDEX index_bookmarks_created_timestamp ON bookmarks(created_timestamp)

-- Annotations
CREATE INDEX index_annotations_document_id ON annotations(document_id)
CREATE INDEX index_annotations_chunk_id ON annotations(chunk_id)
CREATE INDEX index_annotations_type ON annotations(type)
CREATE INDEX index_annotations_created_timestamp ON annotations(created_timestamp)

-- Filter Presets
CREATE INDEX index_filter_presets_is_pinned ON filter_presets(is_pinned)
CREATE INDEX index_filter_presets_created_timestamp ON filter_presets(created_timestamp)
```

---

## Code Usage Examples

### Using Bookmarks

```kotlin
// Initialize repository
val bookmarkRepo = RoomBookmarkRepository(database.bookmarkDao())

// Add a bookmark
val bookmark = Bookmark(
    id = UUID.randomUUID().toString(),
    type = BookmarkType.DOCUMENT,
    targetId = documentId,
    title = "Important Research Paper",
    description = "Key findings on AI",
    tags = listOf("AI", "research"),
    createdAt = Clock.System.now().toString(),
    color = BookmarkColor.BLUE,
    isPinned = true
)
bookmarkRepo.addBookmark(bookmark)

// Check if bookmarked
val isBookmarked = bookmarkRepo.isBookmarked(documentId).getOrNull() ?: false

// Get all bookmarks
val allBookmarks = bookmarkRepo.getAllBookmarks().getOrNull() ?: emptyList()

// Record access
bookmarkRepo.recordAccess(bookmark.id)
```

### Using Annotations

```kotlin
// Initialize repository
val annotationRepo = RoomAnnotationRepository(
    database.annotationDao(),
    database.chunkDao(),
    database.documentDao()
)

// Add an annotation
val annotation = Annotation(
    id = UUID.randomUUID().toString(),
    chunkId = chunkId,
    documentId = documentId,
    type = AnnotationType.HIGHLIGHT,
    content = AnnotationContent(
        text = "Important finding",
        startOffset = 100,
        endOffset = 117,
        note = "This supports my hypothesis"
    ),
    tags = listOf("key-finding"),
    createdAt = Clock.System.now().toString(),
    modifiedAt = Clock.System.now().toString(),
    color = AnnotationColor.YELLOW
)
annotationRepo.addAnnotation(annotation)

// Get annotations for chunk
val annotations = annotationRepo.getAnnotationsForChunk(chunkId).getOrNull() ?: emptyList()

// Search annotations
val searchResults = annotationRepo.searchAnnotations("hypothesis").getOrNull() ?: emptyList()

// Export annotations
val export = annotationRepo.exportAnnotations(documentId, ExportFormat.MARKDOWN)
```

### Using Advanced Filters

```kotlin
// Create advanced filter
val filters = SearchFilters(
    documentTypes = listOf(DocumentType.PDF, DocumentType.DOCX),
    authors = listOf("John Doe"),
    tags = listOf("important"),
    bookmarkedOnly = true,
    dateRange = DateRange(
        start = "2024-01-01",
        end = "2024-12-31"
    )
)

// Use in search
val query = SearchQuery(
    query = "machine learning",
    filters = filters
)
val results = ragRepository.search(query)
```

---

## Rollback Plan

**WARNING:** Migration is ONE-WAY. Rolling back requires data loss.

If you need to rollback:

1. **Export user data** (if possible):
   ```kotlin
   val bookmarks = bookmarkDao.getAllBookmarksSync()
   val annotations = annotationDao.getAllAnnotations()
   // Save to JSON file
   ```

2. **Uninstall app** (clears database)

3. **Install previous version**

4. **Restore documents** (bookmarks/annotations will be lost)

**Recommendation:** Don't rollback. New features are opt-in and don't affect existing functionality.

---

## Troubleshooting

### Migration Fails

**Symptoms:**
- App crashes on startup
- Database error logs

**Solutions:**

1. **Check database integrity:**
   ```sql
   PRAGMA integrity_check;
   ```

2. **Verify foreign key constraints:**
   ```sql
   PRAGMA foreign_key_check;
   ```

3. **Enable migration logging:**
   ```kotlin
   Room.databaseBuilder(context, RAGDatabase::class.java, DATABASE_NAME)
       .addMigrations(MIGRATION_2_3)
       .setQueryCallback({ sqlQuery, bindArgs ->
           Log.d("RAGDatabase", "SQL: $sqlQuery")
       }, executor)
       .build()
   ```

4. **Last resort - destructive migration:**
   ```kotlin
   // WARNING: This will DELETE ALL DATA
   .fallbackToDestructiveMigration()
   ```

### Bookmark Foreign Key Errors

**Symptom:** Cannot add bookmark for non-existent document

**Solution:** Ensure document exists before bookmarking:
```kotlin
val doc = documentDao.getDocument(documentId)
if (doc != null) {
    bookmarkRepo.addBookmark(bookmark)
}
```

### Annotation Foreign Key Errors

**Symptom:** Cannot add annotation for non-existent chunk

**Solution:** Ensure chunk exists before annotating:
```kotlin
val chunk = chunkDao.getChunk(chunkId)
if (chunk != null) {
    annotationRepo.addAnnotation(annotation)
}
```

---

## Performance Considerations

### Index Usage

All queries should use indices:

```sql
-- Good: Uses index_bookmarks_type
SELECT * FROM bookmarks WHERE type = 'DOCUMENT'

-- Good: Uses index_bookmarks_is_pinned
SELECT * FROM bookmarks WHERE is_pinned = 1

-- Slower: No index on description
SELECT * FROM bookmarks WHERE description LIKE '%keyword%'
```

### Tag Searches

Tag searches use JSON pattern matching:

```kotlin
// Implemented as:
SELECT * FROM bookmarks WHERE tags_json LIKE '%"important"%'

// Fast enough for < 1000 bookmarks
// For larger datasets, consider FTS (Full-Text Search)
```

### Batch Operations

Use batch inserts for better performance:

```kotlin
// Instead of:
annotations.forEach { annotationDao.insertAnnotation(it) }

// Do this:
annotationDao.insertAnnotations(annotations)
```

---

## Data Validation

### Required Fields

```kotlin
// Bookmark validation
require(bookmark.title.isNotBlank()) { "Title required" }
require(bookmark.targetId.isNotBlank()) { "Target ID required" }

// Annotation validation
require(annotation.content.text.isNotBlank()) { "Text required" }
require(annotation.content.endOffset > annotation.content.startOffset) { "Invalid offsets" }
```

### Data Sanitization

```kotlin
// Sanitize JSON fields
val tags = bookmark.tags.filter { it.isNotBlank() }.distinct()
val tagsJson = Json.encodeToString(tags)
```

---

## Testing Checklist

- [ ] Migration runs without errors
- [ ] All existing documents preserved
- [ ] All existing chunks preserved
- [ ] Bookmarks table created
- [ ] Annotations table created
- [ ] Filter presets table created
- [ ] All indices created
- [ ] Foreign key constraints work
- [ ] Can add bookmark
- [ ] Can add annotation
- [ ] Can save filter preset
- [ ] Can delete bookmark (cascade works)
- [ ] Can delete annotation (cascade works)
- [ ] Performance is acceptable (< 100ms)

---

## Support

For issues or questions:
1. Check logs: `adb logcat | grep RAGDatabase`
2. Verify schema: `adb shell 'run-as com.augmentalis.ava sqlite3 /data/data/com.augmentalis.ava/databases/ava_rag.db .schema'`
3. Contact: AVA AI Team (Agent 3)

---

**Migration Guide v1.0 - 2025-11-22**
