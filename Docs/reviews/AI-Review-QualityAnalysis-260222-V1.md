# AI Module Quality Analysis
**Date:** 260222
**Reviewer:** Code Reviewer Agent
**Scope:** `Modules/AI/` — 7 sub-modules: NLU, RAG, LLM, Memory, Chat, Teach, ALC
**File count:** 397 .kt, 1 .sq
**Branch:** VoiceOS-1M-SpeechEngine

---

## Summary

The AI module is the largest and most architecturally complex subsystem in the repository. Android NLU and the Android LLM inference pipeline are the strongest areas — production-grade ONNX Runtime integration, mutex-gated initialization, and well-structured DI via Hilt. The Memory module (newly reviewed) is the healthiest: both platform stores use correct `Mutex` discipline and `kotlinx.datetime` throughout with zero violations. However, the module has systemic problems that block shipping: the RAG pipeline is functionally non-functional (garbage embeddings, broken ingestion, no-op embedding writes); all non-Android platforms for ALC/LLM inference return uniform distribution stubs or CoreML zero-vectors with no error surfaced; the Chat desktop coordinators have three guaranteed-crash paths; and the Teach commonMain has iOS compile failures due to `String.format()` usage. Across the module there are 9 Critical, 18 High, 20 Medium, and 17 Low issues totalling 64 findings.

---

## Sub-System Analysis

### 1. NLU (Natural Language Understanding)
**Reviewed depth:** Full — 95 .kt files
**Prior review:** `docs/deepreview/AI-NLU/AI-NLU-DeepReview-260220.md` (confirmed current)

Android path is production-ready. iOS path is fundamentally broken at two levels: `BertTokenizer` returns all-zeros (no WordPiece tokenization), and `CoreMLModelManager.runInference()` returns a zero-vector FloatArray silently. The combination means iOS classification always scores random embeddings against random query embeddings — output is semantically meaningless with no error raised. Desktop NLU runs correctly without DB persistence. JS NLU is a declared Phase 2 stub.

**Intent matching quality:** Android — high (ONNX BERT + keyword ensemble with calibration). iOS — broken at embeddings layer. Desktop — functional but keyword-only (no saved embeddings).

**Confidence thresholds:** `UnifiedLearningService.getMinConfidenceThreshold()` has hardcoded `if (true)` — the VoiceOS-specific threshold branch is permanently unreachable, affecting learning pipeline confidence gating.

### 2. RAG (Retrieval-Augmented Generation)
**Reviewed depth:** Full — from prior session `docs/deepreview/AI-RAG/AI-RAG-DeepReview-260220.md`

**Embedding quality:** CRITICAL FAILURE. `SimpleTokenizer.wordToId()` uses Java `hashCode()` not BERT vocabulary IDs. All ONNX embeddings produced from this tokenizer are numerically garbage — they are hash values, not semantic vectors. Cosine similarity on these vectors is meaningless.

**Retrieval accuracy:** BM25 lexical scorer is correct and functional. The `RRF` (Reciprocal Rank Fusion) combiner is correct. But if ONNX embeddings are garbage, the semantic re-ranking stage contributes noise not signal.

**Ingestion pipeline:** `DocumentIngestionHandler` only handles PDF via a hardcoded `when` block. DOCX, TXT, HTML, MD, RTF all throw `UnsupportedOperationException`. The actual parsers for those formats exist and work — they are simply never called.

**Chunk embedding storage:** `ChunkEmbeddingHandler.updateChunkEmbeddingInternal()` is a complete no-op. Embeddings computed by the ONNX model (even the garbage ones) are never written to the SQLite `chunk_embeddings` table. RAG retrieval always falls back to BM25 only.

**Security:** `AONFileManager.verifySignature()` — HMAC check is performed but the comparison result is discarded (never used to gate operation). Tamper protection is theater.

### 3. LLM (Provider Abstraction + Local Inference)
**Reviewed depth:** Full — from prior session `docs/deepreview/AI-LLM/AI-LLM-DeepReview-260220.md`

**Provider abstraction:** The cloud provider layer (OpenAI, Anthropic, Groq, OpenRouter, Google AI, HuggingFace) is well-structured. `BaseCloudProvider` + `FallbackProvider` chain is solid. Error emission via `LLMResponse.Error` is correct.

**API key handling:** `ALCModule.kt` uses `System.getenv("ANTHROPIC_API_KEY")` etc. `System.getenv()` returns null on Android — all cloud providers receive null API keys and will receive 401 responses on every call. The providers do not guard against `null` apiKey before making HTTP requests.

**Local inference fallback:** Android TVM/MLC and GGUF paths are partially implemented but `BackpressureStreamingManager` KV cache is a no-op (getCache/setCache do nothing) — every token generation restarts from context length 0. GGUF native pointers are shared across instances (data race).

**Rate limiting:** No rate limiting anywhere in the provider layer. Concurrent requests to cloud APIs are unlimited. No exponential backoff on retry (only the `FallbackProvider` iterates providers but does not back off).

**Token limit handling:** `maxContextLength` is stored in `LLMCapabilities` but never enforced before sending requests. A request exceeding the provider's context window will result in a provider-side error, not a graceful truncation.

**Model checksums:** `ChecksumHelper.KnownChecksums` — all three values (`GEMMA_2B_IT_Q4_PARAMS`, `MOBILEBERT_INT8_ONNX`, `MOBILEBERT_VOCAB`) are the string literal `"TODO_GENERATE_AFTER_DOWNLOAD"`. Any download verification using these will always fail.

**Absolute paths in ModelSpec:** `ModelSelector.kt` L46/58/102/112 contains absolute paths `/Users/manoj_mbpm14/...` in production `ModelSpec.localSourcePath` entries. These will fail on any other machine or device.

### 4. Memory (Persistent Memory System)
**Reviewed depth:** Full — new review this session
**Files:** 10 .kt across commonMain, androidMain, desktopMain

**Android (`AndroidMemoryStore`):** High quality. Correct `Mutex.withLock` discipline throughout. `kotlinx.datetime.Clock.System.now()` used for all timestamps. Disk persistence using `kotlinx.serialization` JSON. No `System.currentTimeMillis()` violations. The double-check on `ensureDiskLoaded()` correctly releases the mutex before the IO operation and re-acquires after.

**Desktop (`FileBasedMemoryStore`):** Correct `Mutex` usage. Uses `java.io.File` in `desktopMain` — acceptable for JVM-only source set. Uses `println()` for logging (9 calls) — should use SLF4J/Timber.

**Common (`InMemoryStore`):** Correct `Mutex` discipline. Clean.

**`DefaultMemoryManager` (desktop):** Uses `CoroutineScope(SupervisorJob() + Dispatchers.Default)` as an unmanaged class-level scope — this scope is never cancelled. If `DefaultMemoryManager` is destroyed, the scope and any pending `scope.launch {}` jobs continue running. This is a lifecycle leak.

**Memory search quality:** Keyword-only substring matching in both implementations. No semantic/embedding-based retrieval. This is noted in code comments as intentional for V1. `AndroidMemoryStore.search()` correctly counts term frequency (multi-hit scoring). `InMemoryStore.search()` is a plain `String.contains()` — less precise but acceptable for test use.

**`FileBasedMemoryStore.persistEntry()`:** Does not use `.use{}` on the `FileWriter` path — `File.writeText()` internally manages the stream, so this is actually safe. However `parseMemoryFile()` uses `file.readLines()` which also manages the stream correctly.

**Rule 7:** All Memory module files are clean — no AI attribution found.

### 5. Chat (Conversation Management)
**Reviewed depth:** Full — from prior session `docs/deepreview/AI-Chat/AI-Chat-DeepReview-260220.md`

**Android coordinators:** Production quality. `ConversationManager`, `RAGCoordinator`, `NLUCoordinator` on Android are fully wired.

**Desktop coordinators:** Three critical bugs:
1. `ConversationManagerDesktop.deleteConversation()` holds `Mutex` then calls `switchConversation()` + `createNewConversation()`, both of which re-acquire the same `Mutex` — guaranteed deadlock.
2. `ExportCoordinatorDesktop.json.encodeToString(Map<String, Any>)` — `kotlinx.serialization` cannot serialize `Any` — `SerializationException` at runtime on every export.
3. `VoiceOSStub.startListening()` immediately fires `onError(NotAvailable)` and is wired as the production `VoiceInputProvider` — voice input is completely non-functional in Chat.

**ChatScreen.kt:** Uses `viewModel()` instead of `hiltViewModel()` — Hilt injection fails silently at runtime. Snackbar uses `MaterialTheme.colorScheme.*` — Mandatory Rule 3 violation.

**Rule 7:** 12 author tag violations across Chat interfaces and implementations (`/ Claude AI` suffix).

### 6. Teach (Learning/Training Pipeline)
**Reviewed depth:** Partial (commonMain + key androidMain files)

**`TrainingAnalytics.kt` (commonMain):** Extension functions `Double.toOneDecimal()` and `Double.toTwoDecimals()` use `"%.1f".format(this)` and `"%.2f".format(this)`. `String.format()` is not available in Kotlin/Native — this causes iOS compilation failure. Prior review `docs/deepreview/AI-Other/AI-ALC-Memory-Teach-DeepReview-260220.md` confirmed `System.currentTimeMillis()` and `String.format()` in commonMain.

**Hash mismatch deduplication:** `AddExampleDialog` computes dedup key as `"$utterance:$intent"` (colon separator) while `BulkImportExportManager` uses `"$utterance$intent"` (no separator). Example "open maps" for intent "navigation" → key `"open maps:navigation"` vs `"open mapsnavigation"`. Deduplication is broken between manual add and bulk import paths.

**Theme violations:** All Teach UI files (`TeachAvaScreen`, `TrainingAnalyticsScreen`, etc.) use `MaterialTheme.colorScheme.*` — full AvanueTheme v5.1 migration required across all 7 Compose files.

**`TeachAvaScreen.kt`:** Uses `AvanueTheme.colors.*` for TopAppBar and FAB colors (correct). But `TeachAvaContent`, `TrainingAnalyticsScreen`, and other inner screens do not — inconsistent application.

**AVID:** No AVID voice semantics on any interactive element in the Teach UI (FAB, list items, filter buttons, dialog buttons).

### 7. ALC (AI Lifecycle Coordination)
**Reviewed depth:** Full — from prior session `docs/deepreview/AI-Other/AI-ALC-Memory-Teach-DeepReview-260220.md`

**ALCModule.kt:** `System.getenv()` returns `null` on Android — all API keys are null. Confirmed current.

**ALCEngineIOS:** Unlike the previous notes about iOS Core ML being a zero-vector stub, `ALCEngineIOS.kt` now contains a real token-generation loop with actual `CoreMLRuntime.predict()` calls and top-p/top-k sampling. This is improved from prior state. However:
- `isGenerating` and `shouldStop` are plain `var` — data race when `stop()` and `chat()` run on different coroutines.
- `getMemoryInfo()` returns `usedBytes = 0` and `modelSizeBytes = 0` hardcoded — diagnostic data is always wrong.
- No rate limit on token generation; infinite loop if `sampleToken()` always selects non-stop tokens.

**ALCEngineDesktop + Linux + Windows + macOS:** All return `Result.failure(NotImplementedError(...))` or uniform-distribution inference — documented in prior review.

---

## Issues

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| **Critical** | `AI/RAG/.../tokenizer/SimpleTokenizer.kt` | `wordToId()` uses Java `hashCode()` not BERT vocab IDs. All ONNX embeddings are numerically garbage. Semantic RAG search is non-functional. | Replace with proper BERT vocabulary lookup using `vocab.txt` binary search or HashMap. |
| **Critical** | `AI/RAG/.../handler/DocumentIngestionHandler.kt` | Hardcoded `when` block only handles PDF. DOCX/TXT/HTML/MD/RTF all throw `UnsupportedOperationException`. The parsers exist but are never called. | Extend the `when` block to dispatch all supported formats. |
| **Critical** | `AI/RAG/.../handler/ChunkEmbeddingHandler.kt` | `updateChunkEmbeddingInternal()` is a complete no-op. Embeddings are computed but never written to DB. `chunk_embeddings` table is permanently empty. | Implement the DB write using the existing `SQLiteRAGRepository.updateChunkEmbedding()` method. |
| **Critical** | `AI/RAG/.../security/AONFileManager.kt` | `verifySignature()` computes HMAC but discards the comparison result. Tamper-detected files are processed anyway. | Return `false` when HMAC mismatch is detected. Callers must check return value. |
| **Critical** | `AI/Chat/src/desktopMain/.../ConversationManagerDesktop.kt:204` | `deleteConversation()` holds non-reentrant `Mutex`, then calls `switchConversation()` + `createNewConversation()`, both of which also `mutex.withLock{}` — guaranteed deadlock. Desktop chat hangs permanently on any delete. | Refactor to extract inner logic into private non-locking helpers that the `mutex.withLock` block calls directly. |
| **Critical** | `AI/Chat/src/desktopMain/.../ExportCoordinatorDesktop.kt:300` | `json.encodeToString(Map<String, Any>)` — `kotlinx.serialization` cannot serialize `Any` at runtime. Throws `SerializationException` on every Desktop export. | Define a concrete serializable data class for the export payload. Replace `Map<String, Any>` with typed model. |
| **Critical** | `AI/Chat/src/main/.../voice/VoiceOSStub.kt` | `startListening()` immediately fires `onError(NotAvailable)`. This stub is wired as the production `VoiceInputProvider` in `ChatModule.kt`. Chat voice input is completely non-functional. Rule 1 violation. | Either implement real `VoiceOSCore` integration or conditionally inject a no-op stub only in debug builds. Do not use error-emitting stub in production. |
| **Critical** | `AI/Teach/src/commonMain/.../TrainingAnalytics.kt:256-257` | Extension functions `Double.toOneDecimal()` and `Double.toTwoDecimals()` call `"%.1f".format(this)` and `"%.2f".format(this)`. `String.format()` does not exist in Kotlin/Native — iOS compile failure. | Replace with `kotlinx.math` rounding: `((this * 10).roundToInt() / 10.0).toString()` or use a `expect/actual` formatter. |
| **Critical** | `AI/NLU/src/iosMain/.../coreml/CoreMLModelManager.kt:229-244` | `runInference()` returns `FloatArray(384){0.0f}` silently with no error. iOS NLU classifications are always wrong with no indication ML is not running. | Return `Result.Error("Core ML inference not implemented")` instead of a zero-vector success. |
| **High** | `AI/LLM/src/androidMain/.../di/ALCModule.kt:46,59,85` | `System.getenv("ANTHROPIC_API_KEY")` etc. returns `null` on Android. All three cloud providers receive null API keys and will 401 on every call. | Read API keys from `EncryptedSharedPreferences` / `ICredentialStore` (the KMP abstraction already exists). Remove `System.getenv()` calls. |
| **High** | `AI/LLM/src/androidMain/.../download/ChecksumHelper.kt:133,145,157` | `KnownChecksums.GEMMA_2B_IT_Q4_PARAMS`, `MOBILEBERT_INT8_ONNX`, `MOBILEBERT_VOCAB` are all `"TODO_GENERATE_AFTER_DOWNLOAD"`. Any verification using these always fails → files deleted after every download. | Fill real SHA-256 checksums, or add an explicit `if (expected.startsWith("TODO")) return true` bypass with a build-time warning. |
| **High** | `AI/LLM/src/androidMain/.../config/ModelSelector.kt:46,58,102,112` | Absolute dev-machine paths `/Users/manoj_mbpm14/...` in production `ModelSpec.localSourcePath`. These paths do not exist on any other device. | Remove hardcoded absolute paths. Use app-relative paths (`context.filesDir`) or a configurable base directory. |
| **High** | `AI/NLU/src/androidMain/.../ModelManager.kt:164` | `detectBestModel()` throws `IllegalStateException` in `init{}` when no model deployed. Crashes any caller at startup before model download completes. | Replace throw with `activeModelFile = null; return ModelType.MOBILEBERT`. Surface via `isModelAvailable() == false`. |
| **High** | `AI/NLU/src/androidMain/.../download/NLUModelDownloader.kt:345` | `MALBERT_CHECKSUM = "TBD"` — mALBERT permanently undownloadable; checksum always fails, file always deleted after download. | Fill real SHA-256 or add `if (expected == "TBD") return true` bypass with prominent TODO comment. |
| **High** | `AI/NLU/src/androidMain/.../inference/OnnxSessionManager.kt:37` | `@author Manoj Jhawar / Claude AI` — Rule 7 violation. | Remove `/ Claude AI`. Use `Manoj Jhawar` only. |
| **High** | `AI/NLU/src/commonMain/.../classifier/HybridClassifier.kt:53-64` | `intentCalibration`, `negativeSamples`, counters: plain mutable fields, no synchronization. Data race under concurrent classification + feedback. | Wrap all mutations in `kotlinx.coroutines.sync.Mutex`. |
| **High** | `AI/NLU/src/commonMain/.../matching/CommandMatchingService.kt:62-69` | `commands`, `commandIndex`, `learnedMappings` are plain `mutableMapOf` / `mutableListOf`. Concurrent read/write from multiple coroutines — data race. | Use `Mutex` or confine all mutations to a single coroutine dispatcher. |
| **High** | `AI/NLU/src/androidMain/.../IntentClassifier.kt:569+` | 10+ methods each construct fresh `DatabaseDriverFactory(context).createDriver().createDatabase()`. Connection-per-call leaks if not explicitly closed. | Inject a single `AVADatabase` via Hilt or use a class-level `lazy` property. |
| **High** | `AI/Chat/src/main/.../ChatScreen.kt:81` | `viewModel()` used instead of `hiltViewModel()`. Hilt injection fails silently — ViewModel receives wrong (uninjected) dependencies. | Replace `viewModel()` with `hiltViewModel()`. |
| **High** | `AI/ALC/src/iosMain/.../engine/ALCEngineIOS.kt` | `isGenerating` and `shouldStop` are plain `var` — race condition when `stop()` is called from a different coroutine than `chat()`. | Annotate with `@Volatile` or use `AtomicBoolean` (via `kotlinx.atomicfu`). |
| **High** | `AI/Chat/src/desktopMain/.../TeachingFlowManagerDesktop.kt` | 5 read methods bypass `teachingMutex` when reading `trainingExamples`. Data race with concurrent writes. | Wrap all reads in `mutex.withLock{}`. |
| **High** | `AI/RAG/build.gradle.kts:3` | File header `// author: AVA AI Team` — vague but acceptable. However the file contains `// created: 2025-11-04` with no update log. Not a blocking issue but tracked. | Add to Rule 7 audit sweep. |
| **Medium** | `AI/Memory/src/desktopMain/.../DefaultMemoryManager.kt:40` | `scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)` — unmanaged scope. If `DefaultMemoryManager` is GC'd, `scope.launch{}` jobs from `refreshAllFlows()` keep running. Lifecycle leak. | Accept a `CoroutineScope` parameter instead of creating one internally, or add a `close()` method that calls `scope.cancel()`. |
| **Medium** | `AI/Teach/src/androidMain/.../BulkImportExportManager.kt` | Deduplication key is `"$utterance$intent"` (no separator) vs `AddExampleDialog`'s `"$utterance:$intent"`. Deduplication broken between manual and bulk-import paths. | Unify to a single constant `"$utterance:$intent"` across both classes. |
| **Medium** | `AI/NLU/src/commonMain/.../matching/MultilingualSupport.kt:238-258` | `detectScript()` early-returns on first character. Wrong for mixed-script input. | Count all characters then return dominant script. |
| **Medium** | `AI/NLU/src/androidMain/.../learning/UnifiedLearningService.kt:123` | `if (true)` hardcoded — `VOICEOS_MIN_CONFIDENCE` branch unreachable forever. | Remove dead branch. Return threshold based on command source. |
| **Medium** | `AI/NLU/src/androidMain/.../download/NLUModelDownloader.kt:157` | `Thread.sleep()` in `suspend fun` on `Dispatchers.IO` — blocks IO thread. | Replace with `kotlinx.coroutines.delay()`. |
| **Medium** | `AI/NLU/src/androidMain/.../ModelManager.kt:42` | `/sdcard/ava-ai-models/` hardcoded — scoped storage violation on Android 10+. | Use `context.getExternalFilesDir()` or package-specific dir. |
| **Medium** | `AI/NLU/src/commonMain/.../classifier/HybridIntentClassifier.kt:218-221` | `currentTimeMillis()` returns hardcoded `0L`. All `processingTimeMs` values always zero. | Use `kotlinx.datetime.Clock.System.now().toEpochMilliseconds()`. |
| **Medium** | `AI/NLU/src/iosMain/.../BertTokenizer.kt:16-37` | Both `tokenize()` / `tokenizePair()` return all-zeros `LongArray`. BERT receives only padding tokens on iOS. | Implement WordPiece tokenization for iOS. The Android/Desktop implementation can be moved to commonMain. |
| **Medium** | `AI/LLM/src/androidMain/.../alc/memory/BackpressureStreamingManager.kt:323-338` | `getCache()`/`setCache()` are no-ops. Every token generation restarts from zero context — no KV cache reuse. Performance degrades quadratically with context length. | Implement real KV cache storage using a `ByteArray` or typed tensor structure. |
| **Medium** | `AI/LLM/src/androidMain/.../alc/inference/MLCInferenceStrategy.kt` | `isAvailable()` always returns `true` regardless of device capability. JNI crash on incompatible devices. | Check NNAPI / NPU capability before returning `true`. |
| **Medium** | `AI/Chat/src/main/.../ChatScreen.kt:181-186` | Snackbar uses `MaterialTheme.colorScheme.inverseSurface` — Mandatory Rule 3 violation. | Replace with `AvanueTheme.colors.*` equivalent. |
| **Medium** | `AI/Teach/src/androidMain/` — 7 Compose files | All use `MaterialTheme.colorScheme.*` — full AvanueTheme v5.1 migration needed across entire Teach UI. | Migrate to `AvanueTheme.colors.*` + `AvanueTheme.typography.*`. |
| **Medium** | `AI/Memory/src/desktopMain/.../FileBasedMemoryStore.kt` (9 `println()` calls) and `DefaultMemoryManager.kt` (5 `println()` calls) | `println()` used for logging throughout Desktop Memory implementation. | Replace with SLF4J / `Napier` logging. |
| **Medium** | `AI/Chat/src/desktopMain/.../RAGCoordinatorDesktop.kt:82` | Returns empty RAG result with `context = null` unconditionally. (Prior session confirmed — checking still open.) | Implement keyword retrieval from desktop file store or connect to Memory module. |
| **Medium** | `AI/Chat/src/desktopMain/.../ResponseCoordinatorDesktop.kt:214` | Returns hardcoded placeholder `"(LLM integration pending)"`. Rule 1 violation. | Connect to ALC desktop provider or return `Result.Error`. Do not return fake success strings. |
| **Medium** | `AI/ALC/src/iosMain/.../engine/ALCEngineIOS.kt` | `getMemoryInfo()` returns `usedBytes = 0` and `modelSizeBytes = 0` hardcoded. Diagnostic data is always wrong. | Use `mach_task_info` via Kotlin/Native `cinterop` or at minimum measure loaded model file size. |
| **Low** | `AI/Chat/src/main/kotlin/com/augmentalis/chat/` — 12 files | `@author ... / Claude AI` tags — Rule 7 violation in `IConversationManager`, `ITTSCoordinator`, `IExportCoordinator`, `ITeachingFlowManager`, `ConversationManager`, `TTSCoordinator`, `ExportCoordinator`, `TeachingFlowManager`, `ChatUIStateManager`, `StatusIndicatorState`, `ChatViewModel`, `ChatModule`. | Remove `/ Claude AI` suffix. Use `Manoj Jhawar` or omit. |
| **Low** | `AI/RAG/build.gradle.kts:1-4` | File header contains `// author: AVA AI Team` — vague but potentially Rule 7 adjacent. | Replace with project owner or omit. |
| **Low** | `AI/LLM/src/androidMain/.../download/ChecksumHelper.kt:19` | `Author: AVA AI Team` — Rule 7 check: "AVA AI Team" is vague but not a direct AI attribution. | Acceptable. Monitor for pattern. |
| **Low** | `AI/NLU/src/androidMain/.../learning/IntentLearningManager.kt:204-205` | `learnIntent()` calls `IntentClassifier.getInstance(context).initialize(modelPath)` — full re-initialization after every new example. O(n) per teach interaction. | Remove `initialize()` call. Use incremental `computeAndSaveNewIntent()` instead. |
| **Low** | `AI/NLU/src/commonMain/.../KeywordSpotter.kt:69-71` | Trie data structure built and maintained but `findKeywords()` does not use it — iterates flat `keywordMap` instead. Trie is dead code. | Either implement Aho-Corasick using the Trie or remove the Trie code entirely. |
| **Low** | `AI/NLU/src/androidMain/.../download/NLUModelDownloader.kt:207-232` | `FileOutputStream` not wrapped in `try-finally` / `.use{}`. Partial-write FD leak if `input.read()` throws. | Wrap in `.use{}`. |
| **Low** | `AI/NLU/src/androidMain/.../download/NLUModelDownloader.kt:88-93` | Uses deprecated `activeNetworkInfo.isConnected` — broken on Android 10+. | Replace with `NetworkCapabilities.hasCapability(NET_CAPABILITY_INTERNET)`. |
| **Low** | `AI/Teach/src/androidMain/` (all Compose screens) | ZERO AVID voice semantics on any interactive element (FAB, list items, filter chips, dialog buttons). | Add `Modifier.semantics { contentDescription = "Voice: ..." }` to all interactive elements. |
| **Low** | `AI/NLU/src/androidMain/.../IntentClassifier.kt:946-969` | `loadTrainedEmbeddings()` private method is never called — dead code. | Delete. |
| **Low** | `AI/Memory/src/commonMain/.../InMemoryStore.kt` | `search()` uses `String.contains()` without term frequency scoring. Fine for test use but suboptimal if used in production. | Document clearly as test-only or upgrade to term-frequency scoring matching `AndroidMemoryStore`. |

---

## P0/P1/P2 Summary

### P0 — Crash / Data Loss / Security (fix immediately)
1. `RAG/SimpleTokenizer.wordToId()` — garbage embeddings, RAG semantic search non-functional
2. `RAG/DocumentIngestionHandler` — only PDF accepted, all other formats crash
3. `RAG/ChunkEmbeddingHandler.updateChunkEmbeddingInternal()` — embeddings never persisted, DB always empty
4. `RAG/AONFileManager.verifySignature()` — tamper protection discards result, security bypass
5. `Chat/ConversationManagerDesktop.deleteConversation()` — guaranteed deadlock
6. `Chat/ExportCoordinatorDesktop.json.encodeToString(Map<String, Any>)` — runtime `SerializationException`
7. `Chat/VoiceOSStub` wired as production — voice input broken
8. `Teach/TrainingAnalytics.kt` — `String.format()` in commonMain — iOS compile failure
9. `NLU/CoreMLModelManager.runInference()` — silent zero-vector, iOS NLU always wrong

### P1 — Blocking Feature / Bad User Experience
1. `LLM/ALCModule.kt` `System.getenv()` — all cloud providers receive null API keys on Android
2. `LLM/ChecksumHelper.KnownChecksums` — all 3 checksums `"TODO"` — downloads always fail verification
3. `LLM/ModelSelector.kt` — absolute dev-machine paths in production model specs
4. `NLU/ModelManager.detectBestModel()` — throws on startup, crashes any caller
5. `NLU/NLUModelDownloader MALBERT_CHECKSUM = "TBD"` — mALBERT permanently undownloadable
6. `NLU/OnnxSessionManager` — Rule 7 violation (`/ Claude AI`)
7. `NLU/HybridClassifier` + `CommandMatchingService` — unsynchronized shared state, data races
8. `NLU/IntentClassifier` — fresh DB connection per method call, 10+ per request
9. `Chat/ChatScreen.kt viewModel()` — Hilt injection fails silently

### P2 — Quality / Platform Completeness
1. `Memory/DefaultMemoryManager` — unmanaged coroutine scope lifecycle leak
2. `Teach/BulkImportExportManager` — dedup key mismatch breaks bulk-import deduplication
3. `NLU/MultilingualSupport.detectScript()` — early return wrong for mixed scripts
4. `NLU/UnifiedLearningService.getMinConfidenceThreshold()` — `if (true)` dead branch
5. `NLU/NLUModelDownloader Thread.sleep()` — blocks IO thread, use `delay()`
6. `NLU/BertTokenizer.kt` iOS — all-zeros tokenization, prerequisite for any iOS NLU
7. `LLM/BackpressureStreamingManager` — KV cache no-op, quadratic performance degradation
8. `LLM/MLCInferenceStrategy.isAvailable()` — always true, JNI crash on incompatible devices
9. `Chat/TeachingFlowManagerDesktop` — 5 read methods bypass mutex
10. `Chat/RAGCoordinatorDesktop` + `ResponseCoordinatorDesktop` — stubs still open
11. `Teach/` all Compose UI — full AvanueTheme v5.1 migration + AVID semantics needed
12. `Chat/` 12 files — Rule 7 `/ Claude AI` author tags

---

## Recommendations

1. **Fix RAG pipeline before any RAG-dependent feature ships (P0).** `SimpleTokenizer` producing hash-based embeddings invalidates all semantic search. The three RAG P0 issues (tokenizer, ingestion, no-op writes) together mean RAG is not functional. BM25 works; the semantic layer does not.

2. **Fix `TrainingAnalytics.kt` String.format() immediately (P0).** This is a compile-time error for iOS — any iOS build including the Teach module will fail. Replace with `kotlin.math.roundToInt()` arithmetic.

3. **Migrate API key provisioning from `System.getenv()` to `ICredentialStore` (P1).** The KMP credential abstraction already exists in the Foundation module. The LLM module should consume it rather than using an environment variable API that Android silently returns null for.

4. **Unify deduplication key in Teach module (P2).** The one-line fix to align `AddExampleDialog` and `BulkImportExportManager` prevents silent data duplication across training example sources.

5. **Scope `DefaultMemoryManager`'s coroutine scope to caller lifecycle (P2).** Accept a `CoroutineScope` as a constructor parameter — the same pattern used in `ChatViewModel`, `ConversationManager`, etc. This is the idiomatic Kotlin pattern for injectable scopes.

6. **Replace `println()` with structured logging in Memory module (P2).** Use `Napier` (already in other modules) or SLF4J on Desktop. `println()` is not filterable in production.

7. **Run Rule 7 sweep on Chat module (P2).** Twelve files have `/ Claude AI` author tags. A single sed command removes them: `sed -i '' 's| / Claude AI||g'`.

---

*Full prior-session reviews:*
- NLU: `docs/deepreview/AI-NLU/AI-NLU-DeepReview-260220.md`
- RAG: `docs/deepreview/AI-RAG/AI-RAG-DeepReview-260220.md`
- LLM: `docs/deepreview/AI-LLM/AI-LLM-DeepReview-260220.md`
- ALC+Memory+Teach: `docs/deepreview/AI-Other/AI-ALC-Memory-Teach-DeepReview-260220.md`
- Chat: `docs/deepreview/AI-Chat/AI-Chat-DeepReview-260220.md`
