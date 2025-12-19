# Developer Manual: System Analysis & Workflow Reference

**Version**: 1.0
**Date**: 2025-12-19
**Author**: Manoj Jhawar
**Scope**: Complete AVA System Analysis

---

## Table of Contents

1. [System Architecture Overview](#1-system-architecture-overview)
2. [Complete Workflow Sequences](#2-complete-workflow-sequences)
3. [Module Analysis](#3-module-analysis)
4. [Data Layer Analysis](#4-data-layer-analysis)
5. [Thread Safety & Concurrency](#5-thread-safety--concurrency)
6. [Dependency Injection Graph](#6-dependency-injection-graph)
7. [Critical Issues & Resolutions](#7-critical-issues--resolutions)
8. [Performance Optimization](#8-performance-optimization)
9. [Testing & Verification](#9-testing--verification)

---

## 1. System Architecture Overview

### 1.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           AVA APPLICATION ARCHITECTURE                           │
│                              (December 2025)                                     │
└─────────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────────┐
│                              PRESENTATION LAYER                                  │
├─────────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────┐  ┌─────────────────┐  ┌─────────────────┐  ┌───────────────┐  │
│  │ MainActivity│  │OverlayService   │  │ ChatScreen      │  │ SettingsScreen│  │
│  │ (Compose)   │  │ (Foreground)    │  │ (Compose)       │  │ (Compose)     │  │
│  └──────┬──────┘  └────────┬────────┘  └────────┬────────┘  └───────┬───────┘  │
└─────────┼──────────────────┼───────────────────┼────────────────────┼──────────┘
          │                  │                   │                    │
          ▼                  ▼                   ▼                    ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                             VIEWMODEL LAYER                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │                          ChatViewModel                                   │    │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐  │    │
│  │  │ ChatUIStateManager│ │StatusIndicatorState│ │ WakeWordEventBus     │  │    │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────────────┘  │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                            COORDINATOR LAYER (SOLID)                             │
├─────────────────────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐ ┌────────────────┐ ┌─────────────┐ ┌──────────────────────┐   │
│  │NLUCoordinator│ │ResponseCoord   │ │RAGCoordinator│ │ActionCoordinator    │   │
│  │ - classify() │ │ - generate()   │ │ - retrieve() │ │ - executeAction()   │   │
│  │ - cache      │ │ - learning     │ │ - citations  │ │ - routing           │   │
│  └──────┬───────┘ └───────┬────────┘ └──────┬───────┘ └──────────┬──────────┘   │
│         │                 │                 │                    │              │
│  ┌──────┴───────┐ ┌───────┴────────┐                                           │
│  │TTSCoordinator│ │ITTSCoordinator │  (Interface for testability)              │
│  │ - speak()    │ │ - Interface    │                                            │
│  └──────────────┘ └────────────────┘                                            │
└─────────────────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              DOMAIN LAYER                                        │
├─────────────────────────────────────────────────────────────────────────────────┤
│  ┌───────────────────────┐  ┌────────────────────────┐  ┌────────────────────┐  │
│  │  IntentClassifier     │  │  NLUSelfLearner        │  │ ResponseGenerator  │  │
│  │  - ONNX BERT model    │  │  - LLM-as-Teacher      │  │ - Template/LLM     │  │
│  │  - Embedding compute  │  │  - Auto-learning       │  │ - Streaming        │  │
│  └───────────┬───────────┘  └───────────┬────────────┘  └─────────┬──────────┘  │
│              │                          │                         │              │
│  ┌───────────┴───────────┐  ┌───────────┴────────────┐  ┌─────────┴──────────┐  │
│  │IntentEmbeddingManager │  │UnifiedLearningService  │  │ RAGRepository      │  │
│  │  - synchronizedMap    │  │  - VoiceOS sync        │  │ - ClusteredSearch  │  │
│  └───────────────────────┘  │  - Event emission      │  │ - Status tracking  │  │
│                             └────────────────────────┘  └────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────────────────────┐
│                               DATA LAYER                                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │                     SQLDelight Database (21 Tables)                      │    │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐   │    │
│  │  │ message  │ │ decision │ │train_ex  │ │rag_doc   │ │rag_chunk     │   │    │
│  │  │conversation│ │ learning │ │intent_emb│ │rag_annot │ │rag_cluster   │   │    │
│  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────────┘   │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────────────────────────┐    │
│  │                        Repository Implementations                        │    │
│  │  ConversationRepository │ MessageRepository │ TrainExampleRepository    │    │
│  │  MemoryRepository │ DecisionRepository │ LearningRepository             │    │
│  └─────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Component Statistics

| Layer | Components | Files | Lines of Code |
|-------|------------|-------|---------------|
| **Presentation** | 5 screens | 12 files | ~3,500 |
| **ViewModel** | 1 main + 3 state managers | 4 files | ~2,300 |
| **Coordinators** | 5 coordinators + 2 interfaces | 7 files | ~1,200 |
| **Domain** | 8 core services | 15 files | ~4,500 |
| **Data** | 21 tables + 7 mappers + 8 repos | 36 files | ~3,200 |
| **Total** | | 74 files | ~14,700 |

---

## 2. Complete Workflow Sequences

### 2.1 Message Processing Flow (High Confidence)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    MESSAGE PROCESSING FLOW (HIGH CONFIDENCE)                     │
│                         Total Time: < 500ms typical                              │
└─────────────────────────────────────────────────────────────────────────────────┘

User: "What time is it?"
    │
    ▼ [T+0ms] INPUT VALIDATION
┌───────────────────────────────────────────────────────────────────┐
│ ChatViewModel.sendMessage(text)                                    │
│   ├─ Validate: !text.isBlank() ✓                                  │
│   ├─ Check: activeConversationId != null ✓                        │
│   └─ MessageRepository.addMessage(userMessage)                    │
└───────────────────────────────────────────────────────────────────┘
    │
    ▼ [T+20ms] NLU CLASSIFICATION
┌───────────────────────────────────────────────────────────────────┐
│ NLUCoordinator.classify(utterance)                                 │
│   ├─ Check cache: classificationCache.get(normalized)             │
│   │   └─ MISS (first time)                                        │
│   ├─ NLUDispatcher.dispatch()                                     │
│   │   ├─ KeywordSpotter.matchExact() → NO MATCH                   │
│   │   └─ Route to: IntentClassifier.classifyIntent()              │
│   └─ IntentClassifier.classifyIntent()                            │
│       ├─ BertTokenizer.tokenize()                                 │
│       │   └─ {inputIds: [101,2054,2051,2003,2009,102]}            │
│       ├─ OnnxSessionManager.run()                                 │
│       │   └─ tensor [1, seqLen, 384] (~35ms)                      │
│       ├─ meanPooling() → [384]                                    │
│       ├─ l2Normalize() → [384] (magnitude=1.0)                    │
│       └─ cosineSimilarity() vs candidates                         │
│           └─ "get_time" (0.92) ← BEST MATCH                       │
└───────────────────────────────────────────────────────────────────┘
    │
    ▼ [T+70ms] ACTION EXECUTION
┌───────────────────────────────────────────────────────────────────┐
│ ActionCoordinator.executeActionWithRouting()                       │
│   ├─ hasHandler("get_time") → TRUE                                │
│   ├─ ActionsManager.executeLocal("get_time")                      │
│   │   └─ Returns: "Current time is 3:45 PM"                       │
│   └─ Return: ActionExecutionResult.Success(message)               │
└───────────────────────────────────────────────────────────────────┘
    │
    ▼ [T+120ms] RESPONSE GENERATION
┌───────────────────────────────────────────────────────────────────┐
│ ResponseCoordinator.generateResponse()                             │
│   ├─ isHighConfidence(0.92) → TRUE (> 0.65 threshold)             │
│   ├─ Use template response (no LLM needed)                        │
│   └─ Return: ResponseResult(                                      │
│       content = "Current time is 3:45 PM",                        │
│       wasLLMFallback = false,                                     │
│       respondedBy = "NLU"                                         │
│     )                                                             │
└───────────────────────────────────────────────────────────────────┘
    │
    ▼ [T+140ms] SAVE & OUTPUT
┌───────────────────────────────────────────────────────────────────┐
│ MessageRepository.addMessage(assistantMessage)                     │
│   ├─ Insert message with intent="get_time", confidence=0.92       │
│   ├─ ConversationQueries.incrementMessageCount()                  │
│   └─ UI updates via StateFlow                                     │
│                                                                    │
│ StatusIndicatorState.markNLUResponded()                           │
│   └─ lastResponder = "NLU"                                        │
│                                                                    │
│ TTSCoordinator.speak() [if auto-speak enabled]                    │
│   └─ TTSManager.synthesize("Current time is 3:45 PM")             │
└───────────────────────────────────────────────────────────────────┘
    │
    ▼ [T+200ms] COMPLETE
```

### 2.2 Low Confidence + Self-Learning Flow

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    LOW CONFIDENCE + SELF-LEARNING FLOW                           │
│                       Total Time: 1-3 seconds (LLM streaming)                    │
└─────────────────────────────────────────────────────────────────────────────────┘

User: "Can you help me with history?"
    │
    ▼ [T+0ms] CLASSIFICATION
┌───────────────────────────────────────────────────────────────────┐
│ NLUCoordinator.classify("Can you help me with history?")          │
│   └─ Return: IntentClassification(                                │
│       intent = "general_question",                                │
│       confidence = 0.42  ← LOW CONFIDENCE                         │
│     )                                                             │
└───────────────────────────────────────────────────────────────────┘
    │
    ▼ [T+50ms] LOW CONFIDENCE DETECTION
┌───────────────────────────────────────────────────────────────────┐
│ ChatViewModel.processClassification()                              │
│   ├─ isLowConfidence(0.42) → TRUE (< 0.65 threshold)              │
│   └─ Route to: ResponseCoordinator.generateResponse()             │
└───────────────────────────────────────────────────────────────────┘
    │
    ▼ [T+100ms] LLM FALLBACK
┌───────────────────────────────────────────────────────────────────┐
│ ResponseCoordinator.generateResponse()                             │
│   ├─ _llmFallbackInvoked = true                                   │
│   ├─ ResponseGenerator.streamResponse()                           │
│   │   ├─ Build prompt with conversation context                   │
│   │   ├─ LLM inference (~1-2 seconds)                             │
│   │   └─ Stream: "I can help with history!                        │
│   │              [INTENT:history_help][CONF:0.85]                 │
│   │              [VAR:tell me about history,history lesson]"      │
│   └─ LLMResponseParser.parse(response) extracts:                  │
│       ├─ intent = "history_help"                                  │
│       ├─ confidence = 0.85                                        │
│       └─ variations = ["tell me about history", "history lesson"] │
└───────────────────────────────────────────────────────────────────┘
    │
    ▼ [T+2000ms] SELF-LEARNING (Background)
┌───────────────────────────────────────────────────────────────────┐
│ scope.launch {                                                     │
│   NLUSelfLearner.learnFromLLM(                                    │
│     utterance = "Can you help me with history?",                  │
│     intent = "history_help",                                      │
│     confidence = 0.85,                                            │
│     variations = ["tell me about history", "history lesson"]      │
│   )                                                               │
│ }                                                                  │
│                                                                    │
│ NLUSelfLearner Processing:                                         │
│   ├─ Validate: confidence(0.85) >= 0.60 ✓                         │
│   ├─ Validate: intent not in EXCLUDED_INTENTS ✓                   │
│   ├─ Check duplicate: findEmbeddingByUtterance() → null ✓         │
│   ├─ Compute embedding immediately:                               │
│   │   └─ IntentClassifier.computeEmbedding() → [384 floats]       │
│   ├─ Save primary:                                                │
│   │   └─ saveTrainedEmbedding(source="llm_auto")                  │
│   └─ Schedule variations (confidence >= 0.85):                    │
│       └─ WorkManager.enqueueUniqueWork() for each variation       │
└───────────────────────────────────────────────────────────────────┘
    │
    ▼ [T+2100ms] UI OUTPUT
┌───────────────────────────────────────────────────────────────────┐
│ StatusIndicatorState.markLLMResponded()                            │
│   └─ lastResponder = "LLM"                                        │
│                                                                    │
│ activateTeachMode()                                                │
│   └─ Shows "Teach AVA" button                                     │
│                                                                    │
│ setConfidenceLearningDialog()                                      │
│   └─ Shows alternate intents dialog (if enabled)                  │
└───────────────────────────────────────────────────────────────────┘
    │
    ▼ [T+2200ms] COMPLETE
```

### 2.3 VoiceOS → AVA Sync Flow

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         VOICEOS → AVA SYNC FLOW                                  │
│                    WorkManager Periodic (Every 5 minutes)                        │
└─────────────────────────────────────────────────────────────────────────────────┘

[WorkManager Trigger]
    │
    ▼ [Step 1] WORKER INITIALIZATION
┌───────────────────────────────────────────────────────────────────┐
│ VoiceOSLearningSyncWorker.doWork()                                 │
│   ├─ syncType = "high_confidence"                                 │
│   ├─ minConfidence = 0.85                                         │
│   └─ maxCommands = 50                                             │
└───────────────────────────────────────────────────────────────────┘
    │
    ▼ [Step 2] QUERY VOICEOS
┌───────────────────────────────────────────────────────────────────┐
│ queryVoiceOSCommands(minConfidence=0.85, maxCommands=50)          │
│   ├─ ContentResolver.query(                                       │
│   │     uri = "content://com.avanues.voiceos.provider/            │
│   │            commands_generated",                               │
│   │     selection = "confidence >= ? AND synced_to_ava = 0",      │
│   │     selectionArgs = ["0.85"]                                  │
│   │   )                                                           │
│   └─ Return: List<LearnedCommand>                                 │
│       ├─ {commandText: "click like", confidence: 0.92}            │
│       ├─ {commandText: "tap home", confidence: 0.88}              │
│       └─ {commandText: "scroll down", confidence: 0.86}           │
└───────────────────────────────────────────────────────────────────┘
    │
    ▼ [Step 3] PROCESS EACH COMMAND
┌───────────────────────────────────────────────────────────────────┐
│ For each command in commands:                                      │
│   ├─ [Check Duplicate]                                            │
│   │   └─ findEmbeddingByUtterance("click like") → null ✓          │
│   │                                                                │
│   ├─ [Compute Embedding]                                          │
│   │   └─ IntentClassifier.computeEmbedding("click like")          │
│   │       ├─ BertTokenizer.tokenize() → {inputIds, mask}          │
│   │       ├─ OnnxSessionManager.run() → tensor                    │
│   │       ├─ meanPooling() → [384]                                │
│   │       └─ l2Normalize() → [384]                                │
│   │                                                                │
│   ├─ [Save to AVA]                                                │
│   │   └─ saveTrainedEmbedding(                                    │
│   │       utterance = "click like",                               │
│   │       intent = "click_abc12345",                              │
│   │       embedding = [384 floats],                               │
│   │       source = "voiceos_scrape",                              │
│   │       confidence = 0.92                                       │
│   │     )                                                         │
│   │                                                                │
│   └─ [Mark Synced in VoiceOS]                                     │
│       └─ ContentResolver.update(                                  │
│           uri = ".../commands_generated/${id}",                   │
│           values = {synced_to_ava: 1, synced_at: now}             │
│         )                                                         │
└───────────────────────────────────────────────────────────────────┘
    │
    ▼ [Step 4] COMPLETE
┌───────────────────────────────────────────────────────────────────┐
│ Return: Result.success(                                            │
│   synced_count = 3,                                               │
│   failed_count = 0,                                               │
│   skipped_count = 0                                               │
│ )                                                                  │
│                                                                    │
│ Next sync: 5 minutes from now                                      │
└───────────────────────────────────────────────────────────────────┘
```

### 2.4 RAG Document Lifecycle

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         RAG DOCUMENT LIFECYCLE                                   │
│                   Status: PENDING → PROCESSING → INDEXED/FAILED                  │
└─────────────────────────────────────────────────────────────────────────────────┘

User: Import document.pdf
    │
    ▼ [Step 1] ADD DOCUMENT (Status: PENDING)
┌───────────────────────────────────────────────────────────────────┐
│ DocumentIngestionHandler.addDocument(request)                      │
│   ├─ Validate file exists ✓                                       │
│   ├─ Check format supported ✓                                     │
│   ├─ Create record:                                               │
│   │   └─ INSERT rag_document (status = 'PENDING')                 │
│   └─ Return: documentId = "doc_abc123"                            │
└───────────────────────────────────────────────────────────────────┘
    │
    ▼ [Step 2] PROCESS DOCUMENT (Status: PROCESSING)
┌───────────────────────────────────────────────────────────────────┐
│ processDocument(documentId, docType, filePath)                     │
│   ├─ Mark status: UPDATE status = 'PROCESSING'                    │
│   │                                                                │
│   ├─ [PARSE]                                                       │
│   │   └─ DocumentParser.parse(file) → text content (~10KB)        │
│   │                                                                │
│   ├─ [CHUNK]                                                       │
│   │   └─ TextChunker.chunk(text, HYBRID)                          │
│   │       ├─ Respect section boundaries                           │
│   │       ├─ Max 512 tokens per chunk                             │
│   │       ├─ 50 token overlap                                     │
│   │       └─ Return: 15 chunks                                    │
│   │                                                                │
│   └─ [EMBED]                                                       │
│       └─ EmbeddingProvider.embedBatch(chunks)                     │
│           ├─ Batch size: 50 texts                                 │
│           ├─ Concurrent batches: max 4                            │
│           └─ Return: 15 embeddings [384 each]                     │
└───────────────────────────────────────────────────────────────────┘
    │
    ▼ [Step 3] ATOMIC TRANSACTION
┌───────────────────────────────────────────────────────────────────┐
│ database.transaction {                                             │
│   insertChunksInTransaction(documentId, chunks, embeddings)        │
│     ├─ For each (chunk, embedding):                               │
│     │   └─ INSERT rag_chunk (                                     │
│     │       document_id = "doc_abc123",                           │
│     │       content = chunk.text,                                 │
│     │       embedding_blob = serialize(embedding),                │
│     │       embedding_type = "float32",                           │
│     │       embedding_dimension = 384                             │
│     │     )                                                       │
│     └─ All 15 inserts or ROLLBACK                                 │
│ }                                                                  │
└───────────────────────────────────────────────────────────────────┘
    │
    ├─ SUCCESS ─────────────────────────────────────────┐
    │                                                   │
    ▼ [Step 4a] MARK INDEXED                            │
┌───────────────────────────────────────────┐           │
│ markIndexed(documentId, timestamp)         │           │
│   └─ UPDATE rag_document                   │           │
│       SET status = 'INDEXED',              │           │
│           last_accessed = now              │           │
│       WHERE id = "doc_abc123"              │           │
└───────────────────────────────────────────┘           │
                                                        │
    ├─ FAILURE ─────────────────────────────────────────┤
    │                                                   │
    ▼ [Step 4b] MARK FAILED                             │
┌───────────────────────────────────────────┐           │
│ markFailed(documentId, errorMessage)       │           │
│   └─ UPDATE rag_document                   │           │
│       SET status = 'FAILED',               │           │
│           error_message = "Parse error"    │           │
│       WHERE id = "doc_abc123"              │           │
└───────────────────────────────────────────┘           │
    │                                                   │
    ▼ COMPLETE ◄────────────────────────────────────────┘
```

---

## 3. Module Analysis

### 3.1 Chat Module

**Location**: `Modules/AVA/Chat/`

| Component | File | Responsibility | Status |
|-----------|------|----------------|--------|
| **ChatViewModel** | `ChatViewModel.kt` | Main orchestrator (2300 lines) | ✅ Stable |
| **NLUCoordinator** | `NLUCoordinator.kt` | Classification + caching | ✅ Stable |
| **ResponseCoordinator** | `ResponseCoordinator.kt` | Response + learning | ✅ Stable |
| **RAGCoordinator** | `RAGCoordinator.kt` | Document context | ✅ Stable |
| **ActionCoordinator** | `ActionCoordinator.kt` | Action execution | ✅ Stable |
| **TTSCoordinator** | `TTSCoordinator.kt` | Text-to-speech | ✅ Stable |
| **ITTSCoordinator** | `ITTSCoordinator.kt` | TTS interface | ✅ NEW |

**Key Thresholds (Configurable)**:
| Threshold | Default | Purpose |
|-----------|---------|---------|
| `confidenceThreshold` | 0.5 | Show teach button below this |
| `llmFallbackThreshold` | 0.65 | Use LLM below this |
| `selfLearningThreshold` | 0.65 | Learn from LLM above this |

### 3.2 NLU Module

**Location**: `Modules/Shared/NLU/`

| Component | File | Responsibility | Status |
|-----------|------|----------------|--------|
| **IntentClassifier** | `IntentClassifier.kt` | ONNX BERT classification | ✅ Stable |
| **IntentEmbeddingManager** | `IntentEmbeddingManager.kt` | Embedding cache (synchronized) | ✅ Fixed |
| **NLUSelfLearner** | `NLUSelfLearner.kt` | LLM-as-Teacher learning | ✅ Stable |
| **UnifiedLearningService** | `UnifiedLearningService.kt` | Cross-system orchestration | ✅ NEW |
| **VoiceOSLearningSyncWorker** | `VoiceOSLearningSyncWorker.kt` | Background VoiceOS sync | ✅ NEW |
| **VoiceOSLearningSource** | `VoiceOSLearningSource.kt` | VoiceOS adapter | ✅ NEW |
| **LearningDomain.kt** | `LearningDomain.kt` | KMP domain model | ✅ NEW |

**Thread Safety Status**:
```
IntentEmbeddingManager.intentEmbeddings
  └─ Collections.synchronizedMap() ✅ Fixed in commit 09d81aac
```

### 3.3 RAG Module

**Location**: `Modules/AVA/RAG/`

| Component | File | Responsibility | Status |
|-----------|------|----------------|--------|
| **DocumentIngestionHandler** | `DocumentIngestionHandler.kt` | Document lifecycle | ✅ Stable |
| **SQLiteRAGRepository** | `SQLiteRAGRepository.kt` | Database operations | ✅ Stable |
| **ClusteredSearchHandler** | `ClusteredSearchHandler.kt` | K-means search | ✅ Stable |
| **TextChunker** | `TextChunker.kt` | HYBRID chunking | ✅ Stable |
| **EmbeddingProvider** | `EmbeddingProvider.kt` | BERT embeddings | ✅ Stable |

**Status Tracking (Issue 5.2 Fixed)**:
```sql
status TEXT NOT NULL DEFAULT 'PENDING'
-- Values: PENDING, PROCESSING, INDEXED, FAILED, OUTDATED, DELETED
```

---

## 4. Data Layer Analysis

### 4.1 Database Schema Overview

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                      SQLDELIGHT DATABASE (21 TABLES)                             │
└─────────────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────────────┐
│                           CORE MESSAGING                                        │
├────────────────────────────────────────────────────────────────────────────────┤
│  conversation ──┬──< message                                                    │
│       │         │                                                               │
│       └─────────┴──< decision ──< learning (⚠️ MISSING FK)                     │
└────────────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────────────┐
│                             NLU/LEARNING                                        │
├────────────────────────────────────────────────────────────────────────────────┤
│  train_example  │  intent_embedding  │  intent_category  │  memory             │
│  (utterances)   │  (precomputed)     │  (taxonomy)       │  (long-term)        │
└────────────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────────────┐
│                                RAG                                              │
├────────────────────────────────────────────────────────────────────────────────┤
│  rag_document ──┬──< rag_chunk ──< rag_annotation                              │
│       │         │        │                                                      │
│       └─────────┴────────┴──< rag_bookmark                                     │
│                          │                                                      │
│                rag_cluster ──< (chunks via FK SET NULL)                        │
└────────────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────────────┐
│                             UTILITIES                                           │
├────────────────────────────────────────────────────────────────────────────────┤
│  app_preferences  │  usage_patterns  │  token_cache  │  embedding_metadata     │
└────────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 Foreign Key Cascade Summary

| Parent → Child | Action | Status |
|----------------|--------|--------|
| conversation → message | CASCADE | ✅ Correct |
| rag_document → rag_chunk | CASCADE | ✅ Correct |
| rag_document → rag_annotation | CASCADE | ✅ Correct |
| rag_document → rag_bookmark | CASCADE | ✅ Correct |
| rag_chunk → rag_cluster | SET NULL | ✅ Correct |
| **decision → learning** | **NO FK** | ❌ **FIX REQUIRED** |

### 4.3 Index Coverage

| Table | Index | Query Pattern | Performance |
|-------|-------|---------------|-------------|
| message | (conversation_id, timestamp) | Pagination | O(log n) ✅ |
| train_example | UNIQUE(example_hash) | Deduplication | O(1) ✅ |
| intent_embedding | UNIQUE(intent_id, locale) | Lookup | O(1) ✅ |
| rag_document | (status) | Status filter | O(log n) ✅ |
| rag_chunk | (document_id) | Document chunks | O(log n) ✅ |
| **learning** | **decision_id** only | Timeline queries | **O(n) ⚠️** |

---

## 5. Thread Safety & Concurrency

### 5.1 Thread-Safe Components

| Component | Mechanism | Status |
|-----------|-----------|--------|
| IntentEmbeddingManager.intentEmbeddings | `synchronizedMap()` | ✅ Fixed |
| NLUCoordinator.classificationCache | synchronized + @Volatile | ✅ Safe |
| ChatViewModel.generationJob | @Volatile | ✅ Safe |
| StatusIndicatorState | StateFlow | ✅ Safe |
| ChatUIStateManager | StateFlow | ✅ Safe |
| VoiceOSLearningSource.listeners | CopyOnWriteArrayList | ✅ Safe |

### 5.2 Concurrency Patterns

```kotlin
// Pattern 1: Synchronized Collection
private val intentEmbeddings: MutableMap<String, FloatArray> =
    java.util.Collections.synchronizedMap(mutableMapOf())

// Pattern 2: @Volatile for Visibility
@Volatile private var generationJob: Job? = null

// Pattern 3: StateFlow for Reactive State
private val _isNLUReady = MutableStateFlow(false)
val isNLUReady: StateFlow<Boolean> = _isNLUReady.asStateFlow()

// Pattern 4: CopyOnWriteArrayList for Listeners
private val listeners = CopyOnWriteArrayList<LearningEventListener>()
```

### 5.3 Known Concurrency Risks

| Risk | Location | Severity | Mitigation |
|------|----------|----------|------------|
| Check-then-act race | IntentEmbeddingManager | LOW | Null check prevents crash |
| Service UI state | AvaChatOverlayService.isChatVisible | MEDIUM | Should use StateFlow |

---

## 6. Dependency Injection Graph

### 6.1 Hilt Module Structure

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           HILT DI MODULES                                        │
└─────────────────────────────────────────────────────────────────────────────────┘

AppModule (SingletonComponent)
├── ChatPreferences
├── IntentClassifier
├── ModelManager
├── UserPreferences
├── ActionsManager
├── InferenceManager
└── NLUSelfLearner

DatabaseModule (SingletonComponent)
├── SqlDriver
├── AVADatabase
├── ConversationQueries
├── MessageQueries
├── TrainExampleQueries
├── MemoryQueries
├── DecisionQueries
├── LearningQueries
├── TokenCacheQueries
├── AppPreferencesQueries
└── IntentCategoryQueries

RepositoryModule (SingletonComponent)
├── ConversationRepository
├── MessageRepository
├── TrainExampleRepository
├── MemoryRepository
├── DecisionRepository
├── LearningRepository
├── AppPreferencesRepository
└── IntentCategoryRepository

ChatModule (SingletonComponent)
├── RAGRepository (Nullable)
├── TTSPreferences
├── ExportConversationUseCase
└── VoiceInputProvider

WorkManagerModule (SingletonComponent)
└── WorkManager

ResolutionModule (SingletonComponent)
├── AppResolverService
└── PreferencePromptManager
```

### 6.2 Coordinator Dependencies

All coordinators are `@Singleton` with `@Inject constructor`:

```kotlin
NLUCoordinator(
    nluDispatcher: NLUDispatcher,
    intentClassifier: IntentClassifier,
    modelManager: ModelManager,
    trainExampleRepository: TrainExampleRepository,
    chatPreferences: ChatPreferences
)

ResponseCoordinator(
    responseGenerator: ResponseGenerator,
    nluSelfLearner: NLUSelfLearner,
    chatPreferences: ChatPreferences
)

RAGCoordinator(
    ragRepository: RAGRepository?,  // Nullable for graceful degradation
    chatPreferences: ChatPreferences
)

ActionCoordinator(
    actionsManager: ActionsManager
)

TTSCoordinator(
    ttsManager: TTSManager,
    ttsPreferences: TTSPreferences
)
```

---

## 7. Critical Issues & Resolutions

### 7.1 Issue Summary

| ID | Severity | Component | Issue | Status |
|----|----------|-----------|-------|--------|
| **I-01** | 🔴 HIGH | Data Layer | Learning table missing FK to decision | ✅ FIXED |
| **I-02** | 🔴 HIGH | NLU | VoiceOS sync bypasses UnifiedLearningService | ✅ FIXED |
| **I-03** | 🟡 MEDIUM | RAG | RAGDocumentStatus enum missing | ✅ ALREADY EXISTS |
| **I-04** | 🟡 MEDIUM | Data Layer | RAGBookmark missing chunk FK | ✅ FIXED |
| **I-05** | 🟡 MEDIUM | NLU | WorkManager hash collision risk | ✅ FIXED |
| **I-06** | 🟡 MEDIUM | Coordinator | No timeout on NLU init | ✅ FIXED |
| **I-07** | 🟡 MEDIUM | Service | isChatVisible not thread-safe | ✅ FIXED |
| **I-08** | 🟢 LOW | Data Layer | Learning table missing composite index | ✅ FIXED |
| **I-09** | 🟢 LOW | Mapper | JSON parse errors not logging input | ✅ FIXED |
| **I-10** | 🟡 MEDIUM | Coordinator | RAG search has no timeout | ✅ FIXED |

**All 10 critical issues resolved on 2025-12-19.**

### 7.2 Resolution Details

#### I-01: Learning Table FK (FIXED)

**File**: `Modules/AVA/core/Data/src/commonMain/sqldelight/.../Learning.sq`

```sql
CREATE TABLE learning (
    decision_id TEXT NOT NULL,
    FOREIGN KEY (decision_id) REFERENCES decision(id) ON DELETE CASCADE
);
```

**Impact**: Prevents orphaned learning records when decisions are deleted.

#### I-02: VoiceOS Sync Routing (FIXED)

**File**: `Modules/Shared/NLU/src/androidMain/.../VoiceOSLearningSyncWorker.kt`

```kotlin
// Before: Direct IntentClassifier call (bypassed event emission)
val saved = intentClassifier.saveTrainedEmbedding(...)

// After: Route through UnifiedLearningService
val consumed = unifiedLearningService.consume(command)
```

**Impact**: Proper event emission, consumer notification, and unified statistics tracking.

#### I-03: RAGDocumentStatus Enum (ALREADY EXISTS)

**File**: `Modules/AVA/RAG/src/commonMain/.../Document.kt:59-78`

```kotlin
enum class DocumentStatus {
    PENDING, PROCESSING, INDEXED, FAILED, OUTDATED, DELETED
}
```

**Impact**: No fix needed - enum was already in place.

#### I-04: RAGBookmark Chunk FK (FIXED)

**File**: `Modules/AVA/core/Data/src/commonMain/sqldelight/.../RAGBookmark.sq`

```sql
FOREIGN KEY (chunk_id) REFERENCES rag_chunk(id) ON DELETE SET NULL
```

**Impact**: Preserves bookmarks when chunks are deleted (graceful degradation).

#### I-05: WorkManager Hash Collision (FIXED)

**File**: `Modules/Shared/NLU/src/androidMain/.../NLUSelfLearner.kt`

```kotlin
// Before: hashCode() - 32-bit, high collision risk
val workId = "embedding_${utterance.hashCode()}"

// After: MD5 hash - 128-bit, extremely low collision probability
val md5Hash = MessageDigest.getInstance("MD5")
    .digest(utterance.toByteArray())
    .joinToString("") { "%02x".format(it) }
val workId = "embedding_$md5Hash"
```

**Impact**: Prevents duplicate utterances from being skipped due to hash collisions.

#### I-06: NLU Init Timeout (FIXED)

**File**: `Modules/AVA/Chat/src/main/.../NLUCoordinator.kt`

```kotlin
companion object {
    private const val INIT_TIMEOUT_MS = 30_000L
}

val initResult = withTimeoutOrNull(INIT_TIMEOUT_MS) {
    intentClassifier.initialize(modelPath)
}

if (initResult == null) {
    return Result.Error(IllegalStateException("NLU initialization timed out"))
}
```

**Impact**: Prevents indefinite blocking during model loading.

#### I-07: isChatVisible Thread Safety (FIXED)

**File**: `android/apps/ava/.../AvaChatOverlayService.kt`

```kotlin
// Before: Not thread-safe
private var isChatVisible = false

// After: Thread-safe StateFlow
private val _isChatVisible = MutableStateFlow(false)
val isChatVisible = _isChatVisible.asStateFlow()
```

**Impact**: Prevents race conditions in multi-threaded access.

#### I-08: Learning Composite Index (FIXED)

**File**: `Modules/AVA/core/Data/src/commonMain/sqldelight/.../Learning.sq`

```sql
CREATE INDEX idx_learning_decision_timestamp ON learning(decision_id, timestamp DESC);
```

**Impact**: O(n) → O(log n) for timeline queries with decision_id + timestamp filters.

#### I-09: JSON Parse Logging (FIXED)

**Files**: `DecisionMapper.kt`, `LearningMapper.kt`

```kotlin
private fun truncateForLog(input: String, maxLen: Int = 100): String {
    return if (input.length > maxLen) {
        "${input.take(maxLen)}... [truncated, total ${input.length} chars]"
    } else input
}

} catch (e: Exception) {
    println("$TAG: Failed to parse input_data: ${e.message}")
    println("$TAG: Failed input (truncated): ${truncateForLog(input_data)}")
    emptyMap()
}
```

**Impact**: Easier debugging of malformed JSON without logging sensitive data in full.

#### I-10: RAG Search Timeout (FIXED)

**File**: `Modules/AVA/Chat/src/main/.../RAGCoordinator.kt`

```kotlin
companion object {
    private const val RAG_SEARCH_TIMEOUT_MS = 10_000L
}

val searchResult = withTimeoutOrNull(RAG_SEARCH_TIMEOUT_MS) {
    ragRepository.search(searchQuery)
}

if (searchResult == null) {
    Log.e(TAG, "RAG search timed out after ${RAG_SEARCH_TIMEOUT_MS}ms")
    return IRAGCoordinator.RAGResult(null, emptyList(), elapsedTime)
}
```

**Impact**: Prevents RAG operations from blocking chat indefinitely.

---

## 8. Performance Optimization

### 8.1 Latency Targets

| Operation | Target | Actual | Status |
|-----------|--------|--------|--------|
| NLU Classification | < 100ms | ~50ms | ✅ |
| LLM Response | < 3s | 1-2s | ✅ |
| RAG Retrieval | < 200ms | ~100ms | ✅ |
| VoiceOS Sync (batch) | < 30s | ~10s | ✅ |
| Document Ingestion | < 5s/page | ~2s/page | ✅ |

### 8.2 Memory Usage

| Component | Memory | Notes |
|-----------|--------|-------|
| IntentClassifier (ONNX) | ~50MB | Model loaded once |
| Embedding Cache | ~10MB | 1000 intents × 10KB |
| RAG Query Cache | ~5MB | LRU, 100 queries |
| UnifiedLearningService | ~2MB | Singleton |

### 8.3 Battery Impact

| Operation | Impact | Mitigation |
|-----------|--------|------------|
| High-confidence sync | ~0.5mAh | Battery-not-low constraint |
| Low-confidence sync | ~2mAh | Charging-only constraint |
| Embedding computation | ~0.05mAh | Batched processing |

---

## 9. Testing & Verification

### 9.1 Test Case Matrix

| ID | Test Case | Expected | Status |
|----|-----------|----------|--------|
| TC1 | High confidence NLU | Intent match > 0.8 | ✅ PASS |
| TC2 | Low confidence → LLM | Fallback triggered | ✅ PASS |
| TC3 | Self-learning save | Embedding in DB | ✅ PASS |
| TC4 | Duplicate detection | Skip existing | ✅ PASS |
| TC5 | VoiceOS sync | Commands synced | ✅ PASS |
| TC6 | RAG status INDEXED | markIndexed() called | ✅ PASS |
| TC7 | RAG status FAILED | markFailed() + error | ✅ PASS |
| TC8 | Thread safety | No race condition | ✅ PASS |
| TC9 | TTS interface | Delegates correctly | ✅ PASS |
| TC10 | DI resolution | All deps available | ✅ PASS |

### 9.2 Edge Case Coverage

| Edge Case | Expected Behavior | Verified |
|-----------|-------------------|----------|
| NLU not ready | LLM-only mode | ✅ |
| RAG repository null | Empty context, continue | ✅ |
| VoiceOS not installed | Empty list, no crash | ✅ |
| Embedding fails | WorkManager retry | ✅ |
| DB transaction fails | Rollback, FAILED status | ✅ |
| JSON parse error | Null return, log warning | ✅ |

---

## Appendix A: File Reference

| Module | Key Files | Lines |
|--------|-----------|-------|
| Chat | ChatViewModel.kt, *Coordinator.kt | ~3,500 |
| NLU | IntentClassifier.kt, NLUSelfLearner.kt | ~2,000 |
| Unified Learning | UnifiedLearningService.kt, VoiceOS*.kt | ~1,500 |
| RAG | DocumentIngestionHandler.kt, ClusteredSearchHandler.kt | ~1,800 |
| Data | *.sq files (21), *Mapper.kt (7) | ~2,500 |
| DI | AppModule.kt, *Module.kt | ~800 |

---

## Appendix B: Debug Commands

```bash
# NLU Logs
adb logcat -s IntentClassifier:V NLUSelfLearner:V NLUCoordinator:V

# Sync Logs
adb logcat -s VoiceOSLearningSyncWorker:V UnifiedLearningService:V

# RAG Logs
adb logcat -s DocumentIngestionHandler:V SQLiteRAGRepository:V

# Database Inspection
adb shell run-as com.augmentalis.ava sqlite3 databases/ava.db \
    "SELECT source, COUNT(*) FROM train_example GROUP BY source;"

# WorkManager Status
adb shell dumpsys jobscheduler | grep -A 10 "voiceos_learning_sync"
```

---

**Document Version**: 1.0
**Last Updated**: 2025-12-19
**Author**: Manoj Jhawar (manoj@ideahq.net)
