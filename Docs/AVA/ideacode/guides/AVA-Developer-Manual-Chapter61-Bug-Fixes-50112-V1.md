# Developer Manual - Chapter 61: Bug Fixes (2025-12-01)

**Status:** Complete
**Date:** 2025-12-01
**Version:** 1.0

---

## Overview

This chapter documents bug fixes applied during the December 2025 maintenance cycle using swarm-based parallel fixing.

---

## Fixes Summary

| Bug | Module | Root Cause | Resolution |
|-----|--------|------------|------------|
| RAG Storage returns 0L | RAG | No query implementation | Added SQLDelight aggregate queries |
| PDF Section Detection empty | RAG | Not implemented | Font-based heading detection |
| Model Download URL placeholder | LLM | Unconfigured URL | HuggingFace URL configured |
| Gradle deprecations | Build | Outdated syntax | Updated to Gradle 9.0 compatible |
| LLM Metrics null | LLM | No inference tracking | Added metrics recording in chat() |
| Conversation Previews empty | Chat | No preview query | Added SQL with last message |
| RAG Date Range filters | RAG | Filter logic missing | Implemented in matchesFilters() |

---

## 1. RAG Storage Usage Fix

**File:** `SQLiteRAGRepository.kt`

**Problem:** `getStatistics()` returned `storageUsedBytes = 0L`

**Solution:** Added SQLDelight queries to calculate actual storage:

```sql
-- RAGDocument.sq
sumSizeBytes:
SELECT COALESCE(SUM(size_bytes), 0) FROM rag_document;

-- RAGChunk.sq
sumEmbeddingBytes:
SELECT COALESCE(SUM(LENGTH(embedding_blob)), 0) FROM rag_chunk;

sumContentBytes:
SELECT COALESCE(SUM(LENGTH(content)), 0) FROM rag_chunk;
```

**Usage:**
```kotlin
val totalStorage = documentSizeBytes + embeddingBytes + contentBytes
```

---

## 2. PDF Section Detection

**File:** `PdfParser.android.kt`

**Problem:** `sections = emptyList()` - no heading detection

**Solution:** Implemented `HeadingDetector` class using PDFBox font analysis:

| Detection Method | Pattern |
|-----------------|---------|
| Large font | fontSize > 12pt, length 5-200 chars |
| Bold font | fontName contains "Bold" |
| All caps | 10+ uppercase characters |
| Numbered | Regex: `^\d+(\.\d+)*\.\s+.+` |
| Short lines | < 100 chars with font > 10pt |

**Level Assignment:**
- Level 1: All caps, very large fonts, single-digit sections
- Level 2-3: Based on font size hierarchy or dot count

---

## 3. Model Download URL Configuration

**File:** `ModelDownloader.kt`

**Problem:** `BASE_DOWNLOAD_URL` contained "PLACEHOLDER"

**Solution:** Configured HuggingFace URL structure:

```kotlin
private const val BASE_DOWNLOAD_URL =
    "https://huggingface.co/augmentalis/ava-models/resolve/main/"
```

**Model URL Pattern:**
```
https://huggingface.co/augmentalis/ava-models/resolve/main/{MODEL_ID}.ALM
```

**Note:** Models must be uploaded to HuggingFace before downloads work.

---

## 4. Gradle Deprecation Fixes

| Deprecation | Fix |
|-------------|-----|
| `buildDir` property | → `layout.buildDirectory.asFile.get()` |
| `packagingOptions` | → `packaging` |
| Manual KMP hierarchy | → `kotlin.mpp.applyDefaultHierarchyTemplate=true` |

**Files Updated:**
- `gradle.properties`
- `build.gradle.kts` (root)
- `Universal/AVA/Features/Chat/build.gradle.kts`
- 4 KMP module build files

**Remaining:** Compose plugin warning (awaiting Jetbrains 1.7.0)

---

## 5. LLM Metrics Tracking

**File:** `LocalLLMProvider.kt`

**Problem:** `averageLatencyMs` and `errorRate` returned null

**Solution:** Added metrics recording in `chat()` method:

```kotlin
override suspend fun chat(...): Flow<LLMResponse> {
    val startTime = System.currentTimeMillis()
    var hasError = false

    return flow {
        try {
            engine.chat(messages, options).collect { response ->
                if (response is LLMResponse.Error) {
                    hasError = true
                    latencyMetrics.recordError(response.message)
                }
                emit(response)
            }
            if (!hasError) {
                latencyMetrics.recordInference(System.currentTimeMillis() - startTime)
            }
        } catch (e: Exception) {
            latencyMetrics.recordError(e.message ?: "Unknown")
            throw e
        }
    }
}
```

**Metrics Now Available:**
- `averageLatencyMs`: Rolling average of last 100 inferences
- `errorRate`: `totalErrors / totalOperations`
- `lastError`: Most recent error message

---

## 6. Conversation Previews

**Files:** `Conversation.sq`, `Conversation.kt`, `ConversationMapper.kt`, `ConversationRepositoryImpl.kt`

**Problem:** Conversation list showed empty previews

**Solution:** Added SQL query with correlated subquery:

```sql
selectAllWithPreview:
SELECT
    c.*,
    COALESCE(
        (SELECT content FROM message
         WHERE conversation_id = c.id
         ORDER BY timestamp DESC LIMIT 1),
        ''
    ) AS preview
FROM conversation c
ORDER BY c.updated_at DESC;
```

**Domain Model Update:**
```kotlin
data class Conversation(
    val id: String,
    val title: String,
    // ...
    val preview: String = ""  // NEW: Last message preview
)
```

---

## 7. RAG Date Range Filters

**File:** `InMemoryRAGRepository.kt`

**Problem:** Date range and metadata filters not working

**Solution:** Implemented filter logic in `matchesFilters()`:

```kotlin
// Date range filter
if (filters.dateRange != null) {
    val timestampMs = document.createdAt.toEpochMilliseconds()
    if (!filters.dateRange.contains(timestampMs)) {
        return false
    }
}

// Metadata filter - all entries must match
if (filters.metadata != null) {
    for ((key, value) in filters.metadata) {
        if (document.metadata[key] != value) {
            return false
        }
    }
}
```

---

## Testing Recommendations

| Fix | Test |
|-----|------|
| RAG Storage | Add documents, verify `storageUsedBytes > 0` |
| PDF Sections | Parse PDF with headings, verify `sections.isNotEmpty()` |
| Model Download | Configure real models, test download flow |
| LLM Metrics | Run inferences, check `checkHealth()` returns values |
| Previews | Create conversations, verify preview in list |
| RAG Filters | Search with date range, verify filtered results |

---

## Related Documentation

- [Chapter 58: Room to SQLDelight Migration](Developer-Manual-Chapter58-Room-SQLDelight-Completion.md)
- [Chapter 59: NLU Multiplatform](Developer-Manual-Chapter59-NLU-Multiplatform.md)
- [Chapter 60: Model Download System](Developer-Manual-Chapter60-Model-Download-System.md)

---

## Change Log

| Date | Version | Changes |
|------|---------|---------|
| 2025-12-01 | 1.0 | Initial bug fixes documentation |
