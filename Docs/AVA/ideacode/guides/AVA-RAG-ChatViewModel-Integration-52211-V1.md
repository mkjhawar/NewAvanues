# RAG + ChatViewModel Integration Guide

**Date:** 2025-11-22
**Phase:** 2.0 - RAG Integration (Task 4/4)
**Author:** AVA AI Documentation Team
**Status:** Complete Integration Guide

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Integration Patterns](#integration-patterns)
4. [Dependency Injection Setup](#dependency-injection-setup)
5. [ChatViewModel Enhancement](#chatviewmodel-enhancement)
6. [RAG Pathway Decision Tree](#rag-pathway-decision-tree)
7. [Error Handling](#error-handling)
8. [Code Examples](#code-examples)
9. [Migration Path](#migration-path)
10. [Testing Strategy](#testing-strategy)

---

## Overview

This guide explains how to integrate the RAG (Retrieval-Augmented Generation) system into AVA's main ChatViewModel, enabling context-aware responses with document citations.

### Current State

**Standalone RAG UI:**
- `RAGChatViewModel` - Dedicated ViewModel for RAG-only chat
- `RAGChatScreen` - Standalone UI for RAG conversations
- `RAGSearchScreen` - Document search without LLM
- `DocumentManagementScreen` - CRUD for document library

**Main Chat UI:**
- `ChatViewModel` - Main conversation ViewModel
- `ChatScreen` - Primary chat interface
- **NO RAG INTEGRATION** - Standard NLU → LLM flow only

### Target State

**Integrated RAG in Main Chat:**
- `ChatViewModel` enhanced with RAG capabilities
- Settings toggle: "Enable RAG" checkbox
- Document selector: Choose which docs to search
- Automatic pathway: NLU → **RAG retrieval** → LLM with context
- Source citations in message bubbles
- Visual indicator when RAG is active

---

## Architecture

### System Flow Comparison

**Current (Standard LLM):**
```
User Query
    ↓
ChatViewModel.sendMessage()
    ↓
IntentClassifier (NLU)
    ↓
ResponseGenerator (LLM)
    ↓
Response (no sources)
```

**Target (RAG-Enhanced):**
```
User Query
    ↓
ChatViewModel.sendMessage()
    ↓
Check: RAG Enabled? ──┐
    ↓ YES            │ NO
RAGRepository.search │  │
    ↓                │  │
Top 5 Chunks        │  │
    ↓                │  │
Assemble Context    │  │
    ↓                │  │
    ├────────────────┘
    ↓
IntentClassifier (NLU) - optional
    ↓
ResponseGenerator (LLM + context)
    ↓
Response with Sources
```

### Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                       ChatViewModel                          │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ Chat State   │  │ RAG State    │  │ Settings     │      │
│  │              │  │              │  │              │      │
│  │ - messages   │  │ - ragEnabled │  │ - threshold  │      │
│  │ - isTyping   │  │ - selectedDocs│ │ - documentIds│      │
│  │ - error      │  │ - sources    │  │              │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              Message Send Pipeline                    │   │
│  │                                                        │   │
│  │  sendMessage(text)                                     │   │
│  │      ↓                                                 │   │
│  │  if (ragEnabled && hasSelectedDocs)                   │   │
│  │      retrieveRAGContext(text)                         │   │
│  │      ↓                                                 │   │
│  │  generateResponse(text, ragContext)                   │   │
│  │      ↓                                                 │   │
│  │  emit Message with Sources                            │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
         ↓                       ↓                    ↓
┌────────────────┐   ┌────────────────┐   ┌──────────────────┐
│ RAGRepository  │   │ResponseGenerator│  │ MessageRepository │
│                │   │                │   │                   │
│ - search()     │   │ - generate()   │   │ - insert()        │
│ - listDocs()   │   │ - stream()     │   │ - getAll()        │
└────────────────┘   └────────────────┘   └──────────────────┘
```

---

## Integration Patterns

### Pattern 1: Settings-Based Toggle

**Use Case:** User enables RAG via settings, applies to all conversations

**Implementation:**
```kotlin
// ChatPreferences.kt - Add RAG fields
data class ChatPreferences(
    // Existing fields...
    val conversationMode: ConversationMode = ConversationMode.VOICE_FIRST,
    val showTimestamps: Boolean = true,

    // NEW: RAG Settings
    val ragEnabled: Boolean = false,
    val ragSelectedDocumentIds: List<String> = emptyList(),
    val ragMinSimilarity: Float = 0.7f,
    val ragMaxChunks: Int = 5
)
```

**Pros:**
- ✅ Simple on/off control
- ✅ Persistent across sessions
- ✅ Easy to understand

**Cons:**
- ⚠️ All-or-nothing (no per-message RAG)
- ⚠️ Requires restart to change docs

### Pattern 2: Per-Message RAG Toggle

**Use Case:** User decides per message whether to use RAG

**Implementation:**
```kotlin
// ChatViewModel.kt
fun sendMessage(
    text: String,
    useRAG: Boolean = _ragEnabled.value
) {
    viewModelScope.launch {
        if (useRAG && hasRAGDocuments()) {
            sendMessageWithRAG(text)
        } else {
            sendMessageStandard(text)
        }
    }
}
```

**UI:**
```kotlin
// ChatScreen.kt - Input area
Row {
    OutlinedTextField(...)

    // RAG toggle button
    IconButton(
        onClick = { tempRAGEnabled = !tempRAGEnabled }
    ) {
        Icon(
            imageVector = if (tempRAGEnabled) Icons.Filled.Book else Icons.Outlined.Book,
            tint = if (tempRAGEnabled) Color.Blue else Color.Gray
        )
    }

    IconButton(onClick = { sendMessage(inputText, useRAG = tempRAGEnabled) }) {
        Icon(Icons.Default.Send)
    }
}
```

**Pros:**
- ✅ Flexible per-message control
- ✅ Clear visual feedback

**Cons:**
- ⚠️ Extra user action per message
- ⚠️ More UI complexity

### Pattern 3: Automatic RAG (Recommended)

**Use Case:** System decides when to use RAG based on query type

**Implementation:**
```kotlin
// ChatViewModel.kt
private suspend fun shouldUseRAG(query: String): Boolean {
    // 1. Check if RAG is available
    if (!ragRepository.hasDocuments()) return false

    // 2. Check if query is factual (vs conversational)
    val isFactual = query.contains("?") ||
                    query.startsWith("how", ignoreCase = true) ||
                    query.startsWith("what", ignoreCase = true) ||
                    query.startsWith("where", ignoreCase = true) ||
                    query.startsWith("when", ignoreCase = true)

    // 3. Check if RAG has relevant context
    if (isFactual) {
        val preview = ragRepository.search(
            SearchQuery(query = query, maxResults = 1, minSimilarity = 0.6f)
        ).getOrNull()
        return !preview.isNullOrEmpty()
    }

    return false
}

fun sendMessage(text: String) {
    viewModelScope.launch {
        val useRAG = shouldUseRAG(text)

        if (useRAG) {
            sendMessageWithRAG(text)
        } else {
            sendMessageStandard(text)
        }
    }
}
```

**Pros:**
- ✅ Zero user config
- ✅ Smart context detection
- ✅ Best UX

**Cons:**
- ⚠️ Requires heuristic tuning
- ⚠️ User might not know when RAG is active (add badge)

---

## Dependency Injection Setup

### Step 1: Provide RAGRepository

**File:** `Universal/AVA/Features/Chat/src/main/kotlin/com/augmentalis/ava/features/chat/di/ChatModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object ChatModule {

    // Existing providers...

    /**
     * Provide ONNX Embedding Provider
     *
     * Uses external model: /sdcard/Android/data/com.augmentalis.ava/files/models/
     */
    @Provides
    @Singleton
    fun provideEmbeddingProvider(
        @ApplicationContext context: Context
    ): EmbeddingProvider {
        return ONNXEmbeddingProvider(
            context = context,
            modelId = "AVA-ONX-384-BASE-INT8" // 22 MB bundled model
        )
    }

    /**
     * Provide RAG Repository with Clustering
     *
     * Configuration:
     * - Clustering: Enabled (256 clusters for 40x speedup)
     * - Top clusters: 3 (search ~2,340 chunks instead of all)
     * - Batch size: 32 (optimal for ONNX)
     */
    @Provides
    @Singleton
    fun provideRAGRepository(
        @ApplicationContext context: Context,
        embeddingProvider: EmbeddingProvider
    ): RAGRepository {
        return SQLiteRAGRepository(
            context = context,
            embeddingProvider = embeddingProvider,
            enableClustering = true,
            clusterCount = 256,
            topClusters = 3
        )
    }

    /**
     * Provide LLM Provider (for RAG chat)
     *
     * TODO: Replace stub with MLC-LLM implementation
     */
    @Provides
    @Singleton
    fun provideLLMProvider(
        @ApplicationContext context: Context
    ): LLMProvider {
        // Option 1: Use existing LocalLLMProvider (if compatible)
        // return LocalLLMProviderAdapter(context)

        // Option 2: Stub for now (Phase 2), implement in Phase 4
        return object : LLMProvider {
            override fun generateStream(prompt: String): Flow<String> = flow {
                // Stub: Return template response
                emit("I would answer based on the provided context...")
                emit(" (LLM integration coming in Phase 4)")
            }

            override suspend fun generate(prompt: String): String {
                return "LLM response placeholder"
            }
        }
    }
}
```

### Step 2: Inject into ChatViewModel

**File:** `Universal/AVA/Features/Chat/src/main/kotlin/com/augmentalis/ava/features/chat/ui/ChatViewModel.kt`

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    // Existing dependencies...
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val intentClassifier: IntentClassifier,
    private val responseGenerator: ResponseGenerator,
    private val chatPreferences: ChatPreferences,

    // NEW: RAG dependencies
    private val ragRepository: RAGRepository,
    private val llmProvider: LLMProvider,

    @ApplicationContext private val context: Context
) : ViewModel() {

    // Existing state...
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    // NEW: RAG state
    private val _ragEnabled = MutableStateFlow(false)
    val ragEnabled: StateFlow<Boolean> = _ragEnabled.asStateFlow()

    private val _selectedDocuments = MutableStateFlow<List<Document>>(emptyList())
    val selectedDocuments: StateFlow<List<Document>> = _selectedDocuments.asStateFlow()

    private val _ragThreshold = MutableStateFlow(0.7f)
    val ragThreshold: StateFlow<Float> = _ragThreshold.asStateFlow()

    init {
        loadRAGSettings()
        loadDocuments()
    }

    /**
     * Load RAG settings from ChatPreferences
     */
    private fun loadRAGSettings() {
        viewModelScope.launch {
            chatPreferences.preferences.collect { prefs ->
                _ragEnabled.value = prefs.ragEnabled
                _ragThreshold.value = prefs.ragMinSimilarity

                // Load selected documents
                if (prefs.ragSelectedDocumentIds.isNotEmpty()) {
                    loadSelectedDocuments(prefs.ragSelectedDocumentIds)
                }
            }
        }
    }

    /**
     * Load selected documents from RAG repository
     */
    private suspend fun loadSelectedDocuments(documentIds: List<String>) {
        ragRepository.listDocuments(status = DocumentStatus.INDEXED)
            .collect { document ->
                if (document.id in documentIds) {
                    _selectedDocuments.value = _selectedDocuments.value + document
                }
            }
    }
}
```

---

## ChatViewModel Enhancement

### Core Methods

#### 1. Send Message with RAG Decision

```kotlin
/**
 * Send message with automatic RAG pathway detection
 *
 * Flow:
 * 1. Add user message to UI
 * 2. Check if RAG should be used
 * 3. If RAG: Retrieve context, generate with context
 * 4. If no RAG: Standard NLU → LLM flow
 * 5. Add assistant response with sources (if RAG)
 */
fun sendMessage(text: String) {
    if (text.isBlank()) return

    viewModelScope.launch {
        try {
            // 1. Add user message
            val userMessage = createUserMessage(text)
            addMessage(userMessage)

            // 2. Check RAG pathway
            val useRAG = shouldUseRAG(text)

            // 3. Generate response
            if (useRAG) {
                sendMessageWithRAG(text, userMessage.id)
            } else {
                sendMessageStandard(text, userMessage.id)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            showError("Failed to send message: ${e.message}")
        }
    }
}
```

#### 2. RAG-Enhanced Message Flow

```kotlin
/**
 * Send message using RAG context
 *
 * @param text User query
 * @param userMessageId ID of user message (for threading)
 */
private suspend fun sendMessageWithRAG(text: String, userMessageId: String) {
    // Create assistant message placeholder
    val assistantMessage = createAssistantMessage(
        content = "",
        inReplyTo = userMessageId,
        isStreaming = true
    )
    addMessage(assistantMessage)

    // Retrieve RAG context
    val ragContext = retrieveRAGContext(text)

    if (ragContext == null || ragContext.results.isEmpty()) {
        // Fallback to standard flow if no context found
        updateMessage(
            assistantMessage.copy(
                content = "I don't have relevant information in my documents. Let me answer based on my general knowledge.",
                isStreaming = false
            )
        )
        sendMessageStandard(text, userMessageId)
        return
    }

    // Build context for LLM
    val contextText = buildRAGContext(ragContext.results)

    // Generate response with context
    val fullResponse = StringBuilder()
    val sources = ragContext.results.take(3).map { result ->
        MessageSource(
            documentId = result.document?.id ?: "",
            documentTitle = result.document?.title ?: "Unknown",
            chunkId = result.chunk.id,
            snippet = result.chunk.content.take(200),
            pageNumber = result.chunk.metadata.pageNumber,
            similarity = result.similarity
        )
    }

    // Stream response
    responseGenerator.generateWithContextStream(
        query = text,
        context = contextText,
        conversationHistory = getRecentMessages(10)
    ).collect { chunk ->
        fullResponse.append(chunk.text)

        updateMessage(
            assistantMessage.copy(
                content = fullResponse.toString(),
                isStreaming = chunk.isFinal.not()
            )
        )
    }

    // Add sources to final message
    updateMessage(
        assistantMessage.copy(
            content = fullResponse.toString(),
            sources = sources,
            isStreaming = false,
            metadata = mapOf("rag_enabled" to "true")
        )
    )
}
```

#### 3. RAG Context Retrieval

```kotlin
/**
 * Retrieve relevant document chunks for query
 *
 * @param query User question
 * @return Search response with relevant chunks
 */
private suspend fun retrieveRAGContext(query: String): SearchResponse? {
    return try {
        val result = ragRepository.search(
            SearchQuery(
                query = query,
                maxResults = _ragThreshold.value.toInt().coerceIn(3, 10),
                minSimilarity = _ragThreshold.value,
                documentIds = _selectedDocuments.value.map { it.id }.takeIf { it.isNotEmpty() }
            )
        )

        result.getOrNull()
    } catch (e: Exception) {
        Log.e(TAG, "RAG search failed", e)
        null
    }
}
```

#### 4. Context Assembly

```kotlin
/**
 * Build context text from search results
 *
 * Format:
 * ```
 * [Source: Manual.pdf, Page 42, Relevance: 95%]
 * Content chunk here...
 *
 * [Source: Guide.docx, Page 15, Relevance: 87%]
 * Another chunk here...
 * ```
 */
private fun buildRAGContext(results: List<SearchResult>): String {
    return results.joinToString("\n\n") { result ->
        val title = result.document?.title ?: "Unknown"
        val page = result.chunk.metadata.pageNumber?.toString() ?: "?"
        val relevance = (result.similarity * 100).toInt()

        """
        [Source: $title, Page $page, Relevance: $relevance%]
        ${result.chunk.content}
        """.trimIndent()
    }
}
```

---

## RAG Pathway Decision Tree

```
┌──────────────────┐
│  User Query      │
└────────┬─────────┘
         │
         ▼
    ┌─────────────────┐
    │ RAG Enabled?    │
    └────┬────────────┘
         │
    ┌────┴────┐
    │         │
   YES       NO ────────────────────────────┐
    │                                       │
    ▼                                       │
┌─────────────────┐                        │
│Has Documents?   │                        │
└────┬────────────┘                        │
     │                                      │
 ┌───┴───┐                                 │
YES     NO ─────────────────────────────┐  │
 │                                       │  │
 ▼                                       │  │
┌──────────────────┐                    │  │
│ Search Documents │                    │  │
│ (top 5 chunks)   │                    │  │
└────┬─────────────┘                    │  │
     │                                   │  │
     ▼                                   │  │
┌──────────────────┐                    │  │
│Results >= min    │                    │  │
│similarity?       │                    │  │
└────┬─────────────┘                    │  │
     │                                   │  │
 ┌───┴───┐                              │  │
YES     NO ──────────────────────────┐  │  │
 │                                    │  │  │
 ▼                                    │  │  │
┌─────────────────┐                  │  │  │
│ Assemble        │                  │  │  │
│ Context         │                  │  │  │
└────┬────────────┘                  │  │  │
     │                                │  │  │
     ▼                                │  │  │
┌─────────────────┐                  │  │  │
│ Generate LLM    │◄─────────────────┴──┴──┘
│ Response        │    (Standard Flow)
│ with Context    │
└────┬────────────┘
     │
     ▼
┌─────────────────┐
│ Display with    │
│ Sources         │
└─────────────────┘
```

**Decision Logic:**

```kotlin
private suspend fun shouldUseRAG(query: String): Boolean {
    // 1. RAG must be enabled
    if (!_ragEnabled.value) return false

    // 2. Must have documents
    if (_selectedDocuments.value.isEmpty()) {
        val hasAnyDocs = ragRepository.getStatistics()
            .getOrNull()?.totalDocuments ?: 0 > 0
        if (!hasAnyDocs) return false
    }

    // 3. Query must be factual (heuristic)
    val isFactual = isFactualQuery(query)
    if (!isFactual) return false

    // 4. Must have relevant context (preview search)
    val preview = ragRepository.search(
        SearchQuery(
            query = query,
            maxResults = 1,
            minSimilarity = _ragThreshold.value * 0.8f // Slightly lower for preview
        )
    ).getOrNull()

    return !preview?.results.isNullOrEmpty()
}

private fun isFactualQuery(query: String): Boolean {
    val factualKeywords = listOf(
        "how", "what", "where", "when", "why", "who",
        "explain", "describe", "define", "list", "show"
    )

    val lowerQuery = query.lowercase()
    return factualKeywords.any { lowerQuery.startsWith(it) } ||
           query.contains("?")
}
```

---

## Error Handling

### Error Scenarios

#### 1. RAG Search Fails

```kotlin
private suspend fun sendMessageWithRAG(text: String, userMessageId: String) {
    try {
        val ragContext = retrieveRAGContext(text)
        // ... process context
    } catch (e: Exception) {
        Log.e(TAG, "RAG retrieval failed", e)

        // Fallback: Show error message and use standard flow
        showTemporaryMessage(
            "Document search temporarily unavailable. Using standard response.",
            duration = 3000L
        )

        sendMessageStandard(text, userMessageId)
    }
}
```

#### 2. No Relevant Documents

```kotlin
if (ragContext == null || ragContext.results.isEmpty()) {
    // User-friendly fallback message
    val fallbackMessage = when {
        _selectedDocuments.value.isEmpty() ->
            "No documents selected. Please choose documents in Settings > RAG."
        else ->
            "I couldn't find relevant information in your documents. Let me answer from general knowledge."
    }

    updateMessage(
        assistantMessage.copy(
            content = fallbackMessage,
            metadata = mapOf("rag_status" to "no_context")
        )
    )

    // Continue with standard flow
    sendMessageStandard(text, userMessageId)
}
```

#### 3. LLM Generation Fails

```kotlin
responseGenerator.generateWithContextStream(...)
    .catch { e ->
        Log.e(TAG, "LLM generation failed", e)
        emit(ResponseChunk(
            text = "Error generating response: ${e.message}",
            isFinal = true,
            isError = true
        ))
    }
    .collect { chunk ->
        // ... update message
    }
```

#### 4. Document Processing Incomplete

```kotlin
private suspend fun hasRAGDocuments(): Boolean {
    val stats = ragRepository.getStatistics().getOrNull()

    if (stats == null || stats.indexedDocuments == 0) {
        showTemporaryMessage(
            "No indexed documents available. Please add documents in RAG > Documents.",
            duration = 5000L
        )
        return false
    }

    // Warn if documents are still processing
    if (stats.totalDocuments > stats.indexedDocuments) {
        val processing = stats.totalDocuments - stats.indexedDocuments
        showTemporaryMessage(
            "$processing documents still processing. Results may be incomplete.",
            duration = 3000L
        )
    }

    return true
}
```

---

## Code Examples

### Example 1: Complete Integration

**File:** `ChatViewModel.kt` (Full Enhanced Version)

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val intentClassifier: IntentClassifier,
    private val responseGenerator: ResponseGenerator,
    private val chatPreferences: ChatPreferences,
    private val ragRepository: RAGRepository,
    private val llmProvider: LLMProvider,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
        private const val RAG_MIN_SIMILARITY = 0.7f
        private const val RAG_MAX_CHUNKS = 5
    }

    // State
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _ragEnabled = MutableStateFlow(false)
    val ragEnabled: StateFlow<Boolean> = _ragEnabled.asStateFlow()

    private val _selectedDocuments = MutableStateFlow<List<Document>>(emptyList())
    val selectedDocuments: StateFlow<List<Document>> = _selectedDocuments.asStateFlow()

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping.asStateFlow()

    init {
        loadRAGSettings()
    }

    /**
     * Send message (main entry point)
     */
    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            _isTyping.value = true

            try {
                val userMessage = createUserMessage(text)
                addMessage(userMessage)

                // RAG pathway decision
                val useRAG = shouldUseRAG(text)

                if (useRAG) {
                    Log.d(TAG, "Using RAG pathway for: $text")
                    sendMessageWithRAG(text, userMessage.id)
                } else {
                    Log.d(TAG, "Using standard pathway for: $text")
                    sendMessageStandard(text, userMessage.id)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in sendMessage", e)
                showError("Failed to send message: ${e.message}")
            } finally {
                _isTyping.value = false
            }
        }
    }

    /**
     * RAG-enhanced message flow
     */
    private suspend fun sendMessageWithRAG(text: String, userMessageId: String) {
        // Create streaming placeholder
        val assistantMessage = createAssistantMessage("", userMessageId, isStreaming = true)
        addMessage(assistantMessage)

        // Retrieve context
        val ragContext = retrieveRAGContext(text)

        if (ragContext == null || ragContext.results.isEmpty()) {
            updateMessage(
                assistantMessage.copy(
                    content = "No relevant documents found. Answering from general knowledge.",
                    isStreaming = false
                )
            )
            sendMessageStandard(text, userMessageId)
            return
        }

        // Build context
        val contextText = buildRAGContext(ragContext.results)

        // Generate response
        val fullResponse = StringBuilder()
        responseGenerator.generateWithContextStream(
            query = text,
            context = contextText,
            conversationHistory = getRecentMessages(10)
        ).collect { chunk ->
            fullResponse.append(chunk.text)
            updateMessage(
                assistantMessage.copy(
                    content = fullResponse.toString(),
                    isStreaming = !chunk.isFinal
                )
            )
        }

        // Add sources
        val sources = ragContext.results.take(3).map { result ->
            MessageSource(
                documentId = result.document?.id ?: "",
                documentTitle = result.document?.title ?: "Unknown",
                chunkId = result.chunk.id,
                snippet = result.chunk.content.take(150),
                pageNumber = result.chunk.metadata.pageNumber,
                similarity = result.similarity
            )
        }

        updateMessage(
            assistantMessage.copy(
                content = fullResponse.toString(),
                sources = sources,
                isStreaming = false,
                metadata = mapOf("rag_enabled" to "true")
            )
        )
    }

    /**
     * Standard message flow (existing logic)
     */
    private suspend fun sendMessageStandard(text: String, userMessageId: String) {
        // ... existing ChatViewModel logic
        // NLU classification → Template response / LLM generation
    }

    /**
     * Helper: Should use RAG for this query?
     */
    private suspend fun shouldUseRAG(query: String): Boolean {
        if (!_ragEnabled.value) return false

        val hasDocuments = ragRepository.getStatistics()
            .getOrNull()?.indexedDocuments ?: 0 > 0
        if (!hasDocuments) return false

        // Heuristic: Factual questions benefit from RAG
        val isFactual = query.lowercase().let { q ->
            q.startsWith("how") || q.startsWith("what") ||
            q.startsWith("where") || q.startsWith("when") ||
            q.contains("?")
        }

        if (!isFactual) return false

        // Preview search to check relevance
        val preview = ragRepository.search(
            SearchQuery(query = query, maxResults = 1, minSimilarity = 0.6f)
        ).getOrNull()

        return !preview?.results.isNullOrEmpty()
    }

    /**
     * Helper: Retrieve RAG context
     */
    private suspend fun retrieveRAGContext(query: String): SearchResponse? {
        return ragRepository.search(
            SearchQuery(
                query = query,
                maxResults = RAG_MAX_CHUNKS,
                minSimilarity = RAG_MIN_SIMILARITY,
                documentIds = _selectedDocuments.value.map { it.id }
                    .takeIf { it.isNotEmpty() }
            )
        ).getOrNull()
    }

    /**
     * Helper: Build context string
     */
    private fun buildRAGContext(results: List<SearchResult>): String {
        return results.joinToString("\n\n") { result ->
            val title = result.document?.title ?: "Unknown"
            val page = result.chunk.metadata.pageNumber?.toString() ?: "?"
            val relevance = (result.similarity * 100).toInt()

            "[Source: $title, Page $page, Relevance: $relevance%]\n${result.chunk.content}"
        }
    }

    /**
     * Settings: Enable/Disable RAG
     */
    fun setRAGEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _ragEnabled.value = enabled
            chatPreferences.updateRAGEnabled(enabled)
        }
    }

    /**
     * Settings: Select documents for RAG
     */
    fun setSelectedDocuments(documents: List<Document>) {
        viewModelScope.launch {
            _selectedDocuments.value = documents
            chatPreferences.updateRAGDocuments(documents.map { it.id })
        }
    }

    // ... other existing methods
}
```

### Example 2: Message Model Extension

**File:** `Core/Domain/model/Message.kt`

```kotlin
data class Message(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val intent: String? = null,
    val confidence: Float? = null,

    // NEW: RAG support
    val sources: List<MessageSource>? = null,
    val metadata: Map<String, String> = emptyMap(),
    val isStreaming: Boolean = false
)

/**
 * Source citation for RAG responses
 */
data class MessageSource(
    val documentId: String,
    val documentTitle: String,
    val chunkId: String,
    val snippet: String,
    val pageNumber: Int?,
    val similarity: Float
)

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}
```

### Example 3: UI - Source Display

**File:** `ChatScreen.kt`

```kotlin
@Composable
fun MessageBubble(
    message: Message,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Message content
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (message.role == MessageRole.USER) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // RAG indicator badge
                if (message.metadata["rag_enabled"] == "true") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = "RAG enabled",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Document-based",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Message text
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Streaming indicator
                if (message.isStreaming) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }
        }

        // Sources (if any)
        message.sources?.let { sources ->
            if (sources.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                SourcesSection(sources = sources)
            }
        }
    }
}

@Composable
fun SourcesSection(sources: List<MessageSource>) {
    Column(modifier = Modifier.padding(start = 16.dp)) {
        Text(
            text = "Sources:",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )

        sources.forEach { source ->
            SourceChip(source = source)
        }
    }
}

@Composable
fun SourceChip(source: MessageSource) {
    Row(
        modifier = Modifier
            .padding(top = 4.dp)
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = buildString {
                append(source.documentTitle)
                source.pageNumber?.let { append(" (p.$it)") }
                append(" - ${(source.similarity * 100).toInt()}%")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}
```

---

## Migration Path

### Phase 1: Preparation (1 hour)

**Tasks:**
1. ✅ Read existing RAG documentation
2. ✅ Review ChatViewModel current implementation
3. ✅ Review RAGChatViewModel for patterns
4. ✅ Plan integration approach

### Phase 2: Domain Model Extension (30 minutes)

**Tasks:**
1. Add `sources` field to Message model
2. Create `MessageSource` data class
3. Add `metadata` map to Message
4. Add `isStreaming` boolean to Message
5. Update Room schema version (migration)

**Files:**
- `Core/Domain/model/Message.kt`
- `Core/Data/room/MessageEntity.kt`
- `Core/Data/room/AVADatabase.kt` (migration)

### Phase 3: Dependency Injection (30 minutes)

**Tasks:**
1. Add RAGRepository provider to ChatModule
2. Add EmbeddingProvider provider
3. Add LLMProvider provider (stub for now)
4. Inject into ChatViewModel

**Files:**
- `Features/Chat/di/ChatModule.kt`
- `Features/Chat/ui/ChatViewModel.kt`

### Phase 4: ChatViewModel RAG State (45 minutes)

**Tasks:**
1. Add `_ragEnabled` StateFlow
2. Add `_selectedDocuments` StateFlow
3. Add `_ragThreshold` StateFlow
4. Add `loadRAGSettings()` method
5. Add `setRAGEnabled()` method
6. Add `setSelectedDocuments()` method

**Files:**
- `Features/Chat/ui/ChatViewModel.kt`

### Phase 5: RAG Pathway Implementation (2 hours)

**Tasks:**
1. Add `shouldUseRAG()` decision logic
2. Add `sendMessageWithRAG()` method
3. Add `retrieveRAGContext()` helper
4. Add `buildRAGContext()` helper
5. Update `sendMessage()` to use decision tree
6. Add error handling for all paths

**Files:**
- `Features/Chat/ui/ChatViewModel.kt`

### Phase 6: UI Updates (1.5 hours)

**Tasks:**
1. Update MessageBubble to show sources
2. Add SourcesSection component
3. Add SourceChip component
4. Add RAG indicator badge
5. Add streaming progress indicator

**Files:**
- `Features/Chat/ui/ChatScreen.kt`
- `Features/Chat/ui/components/MessageBubble.kt`

### Phase 7: Settings Integration (1 hour)

**Tasks:**
1. Add RAG section to Settings screen
2. Add "Enable RAG" toggle
3. Add "Select Documents" button
4. Add DocumentSelectorDialog
5. Update ChatPreferences

**Files:**
- `Features/Chat/ui/settings/SettingsScreen.kt`
- `Features/Chat/ui/dialogs/DocumentSelectorDialog.kt`
- `Core/Data/prefs/ChatPreferences.kt`

### Phase 8: Testing (2 hours)

**Tasks:**
1. Unit tests for RAG decision logic
2. Unit tests for context building
3. Integration tests for full flow
4. UI tests for source display
5. Manual testing on device

**Files:**
- `Features/Chat/src/test/kotlin/ChatViewModelTest.kt`
- `Features/Chat/src/androidTest/kotlin/ChatScreenTest.kt`

**Total Estimated Time:** ~9 hours

---

## Testing Strategy

### Unit Tests

**File:** `Features/Chat/src/test/kotlin/ChatViewModelRAGTest.kt`

```kotlin
@RunWith(JUnit4::class)
class ChatViewModelRAGTest {

    private lateinit var viewModel: ChatViewModel
    private lateinit var mockRAGRepository: RAGRepository
    private lateinit var mockLLMProvider: LLMProvider

    @Before
    fun setup() {
        // Mock dependencies
        mockRAGRepository = mock()
        mockLLMProvider = mock()

        viewModel = ChatViewModel(
            // ... other mocks
            ragRepository = mockRAGRepository,
            llmProvider = mockLLMProvider
        )
    }

    @Test
    fun `when RAG disabled, should use standard flow`() = runTest {
        // Given
        viewModel.setRAGEnabled(false)

        // When
        viewModel.sendMessage("How do I reset?")

        // Then
        verify(mockRAGRepository, never()).search(any())
        // Verify standard flow was used
    }

    @Test
    fun `when RAG enabled but no documents, should fallback to standard`() = runTest {
        // Given
        viewModel.setRAGEnabled(true)
        whenever(mockRAGRepository.getStatistics()).thenReturn(
            Result.success(RAGStatistics(totalDocuments = 0))
        )

        // When
        viewModel.sendMessage("How do I reset?")

        // Then
        verify(mockRAGRepository).getStatistics()
        verify(mockRAGRepository, never()).search(any())
    }

    @Test
    fun `when RAG enabled and has documents, should search and include sources`() = runTest {
        // Given
        viewModel.setRAGEnabled(true)

        val mockDocument = Document(
            id = "doc1",
            title = "Manual.pdf",
            filePath = "/path/to/manual.pdf",
            documentType = DocumentType.PDF,
            status = DocumentStatus.INDEXED
        )

        val mockChunk = Chunk(
            id = "chunk1",
            documentId = "doc1",
            chunkIndex = 0,
            content = "To reset, press the button for 10 seconds.",
            metadata = ChunkMetadata(pageNumber = 42)
        )

        val mockSearchResult = SearchResult(
            chunk = mockChunk,
            similarity = 0.95f,
            document = mockDocument
        )

        whenever(mockRAGRepository.search(any())).thenReturn(
            Result.success(SearchResponse(results = listOf(mockSearchResult)))
        )

        // When
        viewModel.sendMessage("How do I reset?")
        advanceUntilIdle()

        // Then
        verify(mockRAGRepository).search(any())

        val messages = viewModel.messages.value
        val assistantMessage = messages.last()

        assertNotNull(assistantMessage.sources)
        assertEquals(1, assistantMessage.sources?.size)
        assertEquals("Manual.pdf", assistantMessage.sources?.first()?.documentTitle)
        assertEquals(42, assistantMessage.sources?.first()?.pageNumber)
    }

    @Test
    fun `should use lower threshold for preview search`() = runTest {
        // Given
        viewModel.setRAGEnabled(true)
        viewModel.setRAGThreshold(0.7f)

        // When
        viewModel.sendMessage("How do I reset?")

        // Then
        verify(mockRAGRepository).search(
            argThat { query ->
                query.minSimilarity == 0.7f * 0.8f // Preview uses 80% of threshold
            }
        )
    }
}
```

### Integration Tests

**File:** `Features/Chat/src/androidTest/kotlin/ChatRAGIntegrationTest.kt`

```kotlin
@RunWith(AndroidJUnit4::class)
class ChatRAGIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var ragRepository: RAGRepository

    @Inject
    lateinit var conversationRepository: ConversationRepository

    private lateinit var viewModel: ChatViewModel

    @Before
    fun setup() {
        hiltRule.inject()
        // viewModel initialization...
    }

    @Test
    fun endToEnd_RAGChat_withRealDocuments() = runTest {
        // 1. Add test document
        val documentPath = InstrumentationRegistry.getInstrumentation()
            .context.assets.open("test_manual.pdf")

        // Copy to temp location
        val testFile = File(context.filesDir, "test_manual.pdf")
        documentPath.copyTo(testFile.outputStream())

        // 2. Index document
        val addResult = ragRepository.addDocument(
            AddDocumentRequest(
                filePath = testFile.absolutePath,
                title = "Test Manual",
                processImmediately = true
            )
        )
        assertTrue(addResult.isSuccess)

        // 3. Enable RAG
        viewModel.setRAGEnabled(true)

        // 4. Send query
        viewModel.sendMessage("How do I reset the device?")

        // 5. Wait for response
        viewModel.messages
            .filter { it.isNotEmpty() }
            .first { messages ->
                messages.last().role == MessageRole.ASSISTANT &&
                !messages.last().isStreaming
            }

        // 6. Verify response has sources
        val response = viewModel.messages.value.last()
        assertNotNull(response.sources)
        assertTrue(response.sources!!.isNotEmpty())
        assertEquals("Test Manual", response.sources!!.first().documentTitle)
    }
}
```

---

## Summary

This guide provides a complete blueprint for integrating RAG into ChatViewModel:

**Completed:**
- ✅ Architecture diagram and flow
- ✅ Three integration patterns (Settings, Per-Message, Automatic)
- ✅ Dependency injection setup
- ✅ ChatViewModel enhancement code
- ✅ RAG pathway decision tree
- ✅ Error handling strategies
- ✅ Complete code examples
- ✅ 8-phase migration plan
- ✅ Testing strategy

**Key Files:**
- `ChatModule.kt` - DI providers
- `ChatViewModel.kt` - Enhanced with RAG
- `Message.kt` - Domain model extension
- `ChatScreen.kt` - UI with sources
- `ChatPreferences.kt` - Settings persistence

**Estimated Effort:** ~9 hours for full integration

**Next Steps:**
1. Review this guide with team
2. Decide on integration pattern (recommend Automatic)
3. Execute migration phases sequentially
4. Test thoroughly with real documents
5. Document any issues or improvements

---

**Author:** AVA AI Documentation Team
**Date:** 2025-11-22
**Phase:** 2.0 - Task 4/4
**Status:** ✅ Complete
