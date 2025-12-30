# Implementation Plan: SOLID Violations Refactoring

**Document:** AVA-Plan-SOLID-Refactoring-251217-V1.md
**Created:** 2025-12-17
**Author:** Claude AI
**Status:** Draft
**Branch:** `feature/solid-refactoring`

---

## 🧠 Chain of Thought Analysis (CoT)

### Current State Assessment

1. **ChatViewModel (2,439 lines, 46 functions)**
   - **Already refactored partially**: Found 5 coordinators already exist:
     - `NLUCoordinator` - NLU state and classification
     - `RAGCoordinator` - RAG context retrieval
     - `ActionCoordinator` - Action execution
     - `ResponseCoordinator` - Response generation
     - `NLUDispatcher` - NLU dispatching
   - **Remaining issue**: ChatViewModel still has 46 functions and handles UI state
   - **Solution**: Extract UI state management and TTS handling

2. **IntentClassifier (1,132 lines)**
   - Single class handling: ONNX initialization, inference, embedding management
   - **Solution**: Extract embedding management to separate class

3. **SQLiteRAGRepository (1,119 lines)**
   - Single class handling: documents, chunks, search, clustering, caching
   - **Solution**: Decompose into focused repositories/handlers

---

## 🌳 Tree of Thought Analysis (ToT)

### Approach Options

```
                    SOLID Refactoring
                          │
        ┌─────────────────┼─────────────────┐
        ▼                 ▼                 ▼
   Option A:         Option B:         Option C:
   Full Extract      Incremental       Interface-First
   (Big Bang)        (Stepwise)        (Abstract First)
```

| Option | Pros | Cons | Risk |
|--------|------|------|------|
| A: Full Extract | Complete SOLID compliance | High regression risk, long merge | 🔴 High |
| B: Incremental | Lower risk, mergeable PRs | Longer timeline, partial state | 🟡 Medium |
| C: Interface-First | Clean architecture, testable | More files, indirection | 🟢 Low |

**Recommendation:** Option B (Incremental) + Option C (Interface-First) hybrid

---

## Overview

| Metric | Value |
|--------|-------|
| Platforms | Android (primary) |
| Total Phases | 4 |
| Total Tasks | 16 |
| Swarm Recommended | No (single platform) |
| Risk Level | Medium |

---

## Phase 1: ChatViewModel Final Extraction (High Priority)

**Goal:** Extract remaining UI state and TTS handling from ChatViewModel

### Existing Coordinators (No Change Needed)
- ✅ `NLUCoordinator` - NLU state, classification, caching
- ✅ `RAGCoordinator` - RAG retrieval, citations
- ✅ `ActionCoordinator` - Action execution
- ✅ `ResponseCoordinator` - Response generation
- ✅ `NLUDispatcher` - NLU dispatching

### New Extractions Required

#### Task 1.1: Extract ChatUIStateManager
**File:** `Chat/ui/state/ChatUIStateManager.kt`
**Extract From:** ChatViewModel lines 313-499

```kotlin
@Singleton
class ChatUIStateManager @Inject constructor() {
    // Message state
    val messages: StateFlow<List<Message>>
    val isLoading: StateFlow<Boolean>
    val errorMessage: StateFlow<String?>

    // Teach-AVA state
    val teachAvaModeMessageId: StateFlow<String?>
    val showTeachBottomSheet: StateFlow<Boolean>

    // History overlay state
    val showHistoryOverlay: StateFlow<Boolean>
    val conversations: StateFlow<List<Conversation>>

    // Pagination state
    val messageOffset: StateFlow<Int>
    val hasMoreMessages: StateFlow<Boolean>
    val totalMessageCount: StateFlow<Int>

    // Accessibility prompts
    val showAccessibilityPrompt: StateFlow<Boolean>
    val showAppPreferenceSheet: StateFlow<Boolean>
}
```

#### Task 1.2: Extract TTSCoordinator
**File:** `Chat/coordinator/TTSCoordinator.kt`
**Extract From:** ChatViewModel lines 465-486

```kotlin
@Singleton
class TTSCoordinator @Inject constructor(
    private val ttsManager: TTSManager,
    private val ttsPreferences: TTSPreferences
) {
    val isTTSReady: StateFlow<Boolean>
    val isTTSSpeaking: StateFlow<Boolean>
    val speakingMessageId: StateFlow<String?>
    val ttsSettings: StateFlow<TTSSettings>

    suspend fun speak(text: String, messageId: String)
    fun stop()
}
```

#### Task 1.3: Extract StatusIndicatorState
**File:** `Chat/ui/state/StatusIndicatorState.kt`
**Extract From:** ChatViewModel lines 267-311

```kotlin
@Singleton
class StatusIndicatorState @Inject constructor() {
    val isNLULoaded: StateFlow<Boolean>
    val isLLMLoaded: StateFlow<Boolean>
    val lastResponder: StateFlow<String?>
    val lastResponderTimestamp: StateFlow<Long>
    val llmFallbackInvoked: StateFlow<Boolean>
    val isFlashModeEnabled: StateFlow<Boolean>

    fun setLastResponder(responder: String)
    fun setLLMFallbackInvoked(invoked: Boolean)
}
```

#### Task 1.4: Refactor ChatViewModel
**Target:** Reduce to ~800 lines (orchestration only)

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    // Coordinators
    private val nluCoordinator: NLUCoordinator,
    private val ragCoordinator: RAGCoordinator,
    private val actionCoordinator: ActionCoordinator,
    private val responseCoordinator: ResponseCoordinator,
    private val ttsCoordinator: TTSCoordinator,
    // State managers
    private val uiStateManager: ChatUIStateManager,
    private val statusState: StatusIndicatorState,
    // Repositories
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    // Preferences
    private val chatPreferences: ChatPreferences
) : ViewModel() {
    // Delegate state to managers
    val messages by uiStateManager::messages
    val isLoading by uiStateManager::isLoading
    // ... etc

    // Orchestration only
    fun sendMessage(text: String) { /* orchestrate */ }
    fun teachIntent(intent: String) { /* orchestrate */ }
}
```

---

## Phase 2: IntentClassifier Decomposition (Medium Priority)

**Goal:** Extract embedding management from IntentClassifier

### Task 2.1: Create IntentEmbeddingManager
**File:** `NLU/embeddings/IntentEmbeddingManager.kt`

```kotlin
class IntentEmbeddingManager(
    private val database: AVADatabase
) {
    private val intentEmbeddings = mutableMapOf<String, FloatArray>()
    private val _isPreComputationComplete = MutableStateFlow(false)
    val isPreComputationComplete: StateFlow<Boolean>

    suspend fun precomputeEmbeddings(classifier: OrtSession)
    suspend fun loadTrainedEmbeddings()
    fun getEmbedding(intent: String): FloatArray?
    fun addEmbedding(intent: String, embedding: FloatArray)
    fun computeSimilarity(queryEmbedding: FloatArray): List<Pair<String, Float>>
}
```

### Task 2.2: Create OnnxSessionManager
**File:** `NLU/inference/OnnxSessionManager.kt`

```kotlin
class OnnxSessionManager(
    private val context: Context
) {
    private lateinit var ortEnvironment: OrtEnvironment
    private lateinit var ortSession: OrtSession

    suspend fun initialize(modelPath: String): Result<Unit>
    fun runInference(inputs: Map<String, OnnxTensor>): OrtSession.Result
    fun close()
}
```

### Task 2.3: Refactor IntentClassifier
**Target:** Reduce to ~600 lines

```kotlin
actual class IntentClassifier private constructor(
    private val sessionManager: OnnxSessionManager,
    private val embeddingManager: IntentEmbeddingManager,
    private val tokenizer: BertTokenizer
) {
    actual suspend fun initialize(modelPath: String): Result<Unit>
    actual suspend fun classify(text: String, candidates: List<String>): IntentClassification
    // Delegate embedding to embeddingManager
    // Delegate inference to sessionManager
}
```

---

## Phase 3: SQLiteRAGRepository Decomposition (Medium Priority)

**Goal:** Split repository into focused components

### Task 3.1: Create DocumentIngestionHandler
**File:** `RAG/data/handlers/DocumentIngestionHandler.kt`

```kotlin
class DocumentIngestionHandler(
    private val documentQueries: RAGDocumentQueries,
    private val textChunker: TextChunker,
    private val embeddingProvider: EmbeddingProvider
) {
    suspend fun addDocument(request: AddDocumentRequest): Result<AddDocumentResult>
    suspend fun processDocument(documentId: String, type: DocumentType, path: String)
    suspend fun updateDocument(request: UpdateDocumentRequest): Result<Unit>
}
```

### Task 3.2: Create ChunkEmbeddingHandler
**File:** `RAG/data/handlers/ChunkEmbeddingHandler.kt`

```kotlin
class ChunkEmbeddingHandler(
    private val chunkQueries: RAGChunkQueries,
    private val embeddingProvider: EmbeddingProvider,
    private val batchSize: Int,
    private val maxConcurrentBatches: Int
) {
    suspend fun processChunks(documentId: String, chunks: List<TextChunk>): Flow<ProcessingProgress>
    suspend fun computeEmbedding(text: String): FloatArray
}
```

### Task 3.3: Create ClusteredSearchHandler
**File:** `RAG/data/handlers/ClusteredSearchHandler.kt`

```kotlin
class ClusteredSearchHandler(
    private val clusterQueries: RAGClusterQueries,
    private val chunkQueries: RAGChunkQueries,
    private val kMeans: KMeansClustering,
    private val bm25Scorer: BM25Scorer,
    private val rrfFusion: ReciprocalRankFusion
) {
    suspend fun search(query: SearchQuery): SearchResponse
    suspend fun rebuildClusters()
    fun findTopClusters(queryEmbedding: FloatArray, k: Int): List<String>
}
```

### Task 3.4: Refactor SQLiteRAGRepository
**Target:** Reduce to ~400 lines (facade pattern)

```kotlin
class SQLiteRAGRepository(
    private val ingestionHandler: DocumentIngestionHandler,
    private val embeddingHandler: ChunkEmbeddingHandler,
    private val searchHandler: ClusteredSearchHandler,
    private val queryCache: QueryCache?
) : RAGRepository {
    // Delegate to handlers
    override suspend fun addDocument(request: AddDocumentRequest) =
        ingestionHandler.addDocument(request)
    override suspend fun search(query: SearchQuery) =
        searchHandler.search(query)
}
```

---

## Phase 4: Domain UseCases Expansion (Low Priority)

**Goal:** Add missing UseCases for better SRP

### Task 4.1: Create SendMessageUseCase
**File:** `Domain/usecase/SendMessageUseCase.kt`

```kotlin
class SendMessageUseCase(
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        content: String,
        role: MessageRole
    ): Result<Message>
}
```

### Task 4.2: Create ClassifyIntentUseCase (Domain Layer)
**File:** `Domain/usecase/ClassifyIntentUseCase.kt`

```kotlin
// Interface in Domain
interface ClassifyIntentUseCase {
    suspend operator fun invoke(text: String): IntentClassificationResult
}

// Implementation in NLU module
class ClassifyIntentUseCaseImpl(
    private val intentClassifier: IntentClassifier,
    private val candidateProvider: CandidateIntentProvider
) : ClassifyIntentUseCase
```

### Task 4.3: Create TeachIntentUseCase
**File:** `Domain/usecase/TeachIntentUseCase.kt`

```kotlin
class TeachIntentUseCase(
    private val trainExampleRepository: TrainExampleRepository,
    private val learningManager: IntentLearningManager
) {
    suspend operator fun invoke(
        utterance: String,
        intent: String
    ): Result<Unit>
}
```

### Task 4.4: Create SearchDocumentsUseCase
**File:** `Domain/usecase/SearchDocumentsUseCase.kt`

```kotlin
class SearchDocumentsUseCase(
    private val ragRepository: RAGRepository
) {
    suspend operator fun invoke(
        query: String,
        filters: SearchFilters
    ): Result<SearchResponse>
}
```

---

## File Summary

### New Files to Create

| Phase | File | Lines (Est) |
|-------|------|-------------|
| 1 | `Chat/ui/state/ChatUIStateManager.kt` | ~150 |
| 1 | `Chat/coordinator/TTSCoordinator.kt` | ~100 |
| 1 | `Chat/ui/state/StatusIndicatorState.kt` | ~80 |
| 2 | `NLU/embeddings/IntentEmbeddingManager.kt` | ~200 |
| 2 | `NLU/inference/OnnxSessionManager.kt` | ~150 |
| 3 | `RAG/data/handlers/DocumentIngestionHandler.kt` | ~250 |
| 3 | `RAG/data/handlers/ChunkEmbeddingHandler.kt` | ~200 |
| 3 | `RAG/data/handlers/ClusteredSearchHandler.kt` | ~300 |
| 4 | `Domain/usecase/SendMessageUseCase.kt` | ~50 |
| 4 | `Domain/usecase/ClassifyIntentUseCase.kt` | ~40 |
| 4 | `Domain/usecase/TeachIntentUseCase.kt` | ~50 |
| 4 | `Domain/usecase/SearchDocumentsUseCase.kt` | ~40 |

### Files to Refactor

| File | Current | Target | Reduction |
|------|---------|--------|-----------|
| `ChatViewModel.kt` | 2,439 | ~800 | -67% |
| `IntentClassifier.kt` | 1,132 | ~600 | -47% |
| `SQLiteRAGRepository.kt` | 1,119 | ~400 | -64% |

---

## Dependency Graph

```
ChatViewModel (orchestrator)
    ├── ChatUIStateManager
    ├── StatusIndicatorState
    ├── NLUCoordinator (existing)
    │   └── NLUDispatcher (existing)
    ├── RAGCoordinator (existing)
    ├── ActionCoordinator (existing)
    ├── ResponseCoordinator (existing)
    └── TTSCoordinator (new)

IntentClassifier
    ├── OnnxSessionManager (new)
    ├── IntentEmbeddingManager (new)
    └── BertTokenizer (existing)

SQLiteRAGRepository (facade)
    ├── DocumentIngestionHandler (new)
    ├── ChunkEmbeddingHandler (new)
    ├── ClusteredSearchHandler (new)
    └── QueryCache (existing)
```

---

## Testing Strategy

### Unit Tests Required

| Component | Test File | Priority |
|-----------|-----------|----------|
| ChatUIStateManager | `ChatUIStateManagerTest.kt` | High |
| TTSCoordinator | `TTSCoordinatorTest.kt` | Medium |
| IntentEmbeddingManager | `IntentEmbeddingManagerTest.kt` | High |
| OnnxSessionManager | `OnnxSessionManagerTest.kt` | High |
| DocumentIngestionHandler | `DocumentIngestionHandlerTest.kt` | Medium |
| SearchDocumentsUseCase | `SearchDocumentsUseCaseTest.kt` | Medium |

### Integration Tests Required

| Test | Scope |
|------|-------|
| `ChatViewModelIntegrationTest` | Verify coordinator orchestration |
| `IntentClassifierIntegrationTest` | Verify inference pipeline |
| `RAGRepositoryIntegrationTest` | Verify search pipeline |

---

## Risk Assessment

| Risk | Mitigation |
|------|------------|
| Regression in ChatViewModel | Extensive test coverage before refactoring |
| ONNX inference breaking | Keep OnnxSessionManager interface stable |
| Performance regression | Benchmark before/after each phase |
| Hilt injection issues | Verify DI graph after each extraction |

---

## Success Criteria

- [ ] ChatViewModel reduced to <1000 lines
- [ ] IntentClassifier reduced to <700 lines
- [ ] SQLiteRAGRepository reduced to <500 lines
- [ ] All existing tests pass
- [ ] New unit tests added (>80% coverage on new code)
- [ ] No performance regression (benchmark verified)
- [ ] SOLID principles fully applied

---

## Execution Order

```
Phase 1: ChatViewModel Final Extraction
    Task 1.1 → Task 1.2 → Task 1.3 → Task 1.4
    ↓
Phase 2: IntentClassifier Decomposition
    Task 2.1 → Task 2.2 → Task 2.3
    ↓
Phase 3: SQLiteRAGRepository Decomposition
    Task 3.1 → Task 3.2 → Task 3.3 → Task 3.4
    ↓
Phase 4: Domain UseCases Expansion
    Tasks 4.1-4.4 (parallel)
```

---

## Notes

- Coordinators already exist for NLU, RAG, Action, Response - good SOLID foundation
- Focus on extracting UI state and remaining cross-cutting concerns
- Use facade pattern for RAGRepository to maintain clean API
- Consider suspend functions for all async operations
