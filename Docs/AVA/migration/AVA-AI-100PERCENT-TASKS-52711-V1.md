# AVA AI - Path to 100% Quality - Detailed Task Breakdown

**Created:** 2025-11-27
**Target:** 100% quality across all domains
**Total Tasks:** 73 tasks across 4 phases
**Estimated Effort:** 120 hours (6 weeks, 1 FTE)

---

## PHASE 1: CRITICAL BLOCKERS (P0) - 18 tasks, 36 hours, Week 1

### Domain Impact: Error Handling 95%→100%, LLM Functionality 0%→100%

---

### EPIC 1.1: Unblock LLM Response Generation [P0 - CRITICAL]
**Current State:** Completely non-functional (blocked by TVMTokenizer)
**Target State:** Full LLM response generation working
**Impact:** +5% overall grade

#### Task 1.1.1: Evaluate TVMTokenizer Alternatives
- **ID:** P0-LLM-001
- **Effort:** 2 hours
- **Owner:** Senior Backend Engineer
- **Deliverable:** Technical decision document
- **Acceptance Criteria:**
  - [ ] Research 3 options: (A) Implement TVMTokenizer, (B) HuggingFace tokenizers, (C) SimpleTokenizer fallback
  - [ ] Document pros/cons for each
  - [ ] Recommend solution with risk assessment
  - [ ] Get approval from tech lead

#### Task 1.1.2: Implement HuggingFace Tokenizer Wrapper (RECOMMENDED)
- **ID:** P0-LLM-002
- **Effort:** 6 hours
- **Dependencies:** P0-LLM-001
- **Files to Create:**
  - `Universal/AVA/Features/LLM/src/main/java/com/augmentalis/ava/features/llm/tokenizer/HuggingFaceTokenizerWrapper.kt`
- **Files to Modify:**
  - `build.gradle.kts` - Add `ai.djl:tokenizers:0.25.0` dependency
- **Implementation:**
```kotlin
class HuggingFaceTokenizerWrapper(
    private val modelId: String = "google/gemma-2b-it"
) {
    private val tokenizer: HuggingFaceTokenizer by lazy {
        HuggingFaceTokenizer.newInstance(modelId)
    }

    fun encode(text: String): EncodedInput {
        val encoding = tokenizer.encode(text)
        return EncodedInput(
            inputIds = encoding.ids,
            attentionMask = encoding.attentionMask,
            tokens = encoding.tokens
        )
    }

    fun decode(tokenIds: LongArray): String {
        return tokenizer.decode(tokenIds)
    }
}
```
- **Tests Required:**
  - [ ] Unit test: 100 text encodings
  - [ ] Edge cases: empty, very long (>2048 tokens), special characters
  - [ ] Performance: <100ms for 512 tokens
- **Acceptance Criteria:**
  - [ ] All tests pass
  - [ ] Tokenization produces correct outputs
  - [ ] Performance meets target
  - [ ] No memory leaks

#### Task 1.1.3: Replace TVMTokenizer in LLMResponseGenerator
- **ID:** P0-LLM-003
- **Effort:** 4 hours
- **Dependencies:** P0-LLM-002
- **Files to Modify:**
  - `LLMResponseGenerator.kt:114-223` - Uncomment and update
  - `TVMRuntime.kt:254,269,279` - Replace tokenizer calls
- **Changes:**
```kotlin
// BEFORE (commented out)
// val tokens = tvmTokenizer.encode(prompt)

// AFTER
val tokens = huggingFaceTokenizer.encode(prompt)
```
- **Acceptance Criteria:**
  - [ ] Code compiles without errors
  - [ ] All commented sections uncommented
  - [ ] Tokenizer calls replaced
  - [ ] No TODOs referencing P7

#### Task 1.1.4: Write Integration Tests for LLM Response Generation
- **ID:** P0-LLM-004
- **Effort:** 4 hours
- **Dependencies:** P0-LLM-003
- **Files to Create:**
  - `LLMResponseGeneratorIntegrationTest.kt`
- **Test Cases:**
  - [ ] Simple prompt → coherent response
  - [ ] Multi-turn conversation
  - [ ] System prompts + user prompts
  - [ ] Streaming responses
  - [ ] Error handling (empty prompt, too long)
- **Acceptance Criteria:**
  - [ ] 10+ test cases passing
  - [ ] >90% code coverage
  - [ ] No flaky tests

#### Task 1.1.5: Performance Optimization and Benchmarking
- **ID:** P0-LLM-005
- **Effort:** 2 hours
- **Dependencies:** P0-LLM-004
- **Benchmarks:**
  - [ ] Tokenization latency: <200ms for 512 tokens
  - [ ] End-to-end response: <3s for 100 token response
  - [ ] Memory usage: <500MB peak
- **Acceptance Criteria:**
  - [ ] All benchmarks meet targets
  - [ ] No performance regressions vs baseline

---

### EPIC 1.2: Fix ONNX Tensor Resource Leak [P0 - CRITICAL]
**Current State:** Memory leak during classification errors
**Target State:** Zero memory leaks
**Impact:** +1% overall grade (Error Handling 95%→96%)

#### Task 1.2.1: Add try-finally for Tensor Cleanup
- **ID:** P0-LEAK-001
- **Effort:** 3 hours
- **Files to Modify:**
  - `IntentClassifier.kt:189-341`
- **Implementation:**
```kotlin
suspend fun classifyIntent(utterance: String): Result<Intent> {
    var inputIdsTensor: OnnxTensor? = null
    var attentionMaskTensor: OnnxTensor? = null
    var outputTensor: OnnxTensor? = null

    return try {
        // Tokenization
        val tokens = tokenizer.tokenize(utterance)

        // Create tensors
        inputIdsTensor = OnnxTensor.createTensor(env, tokens.inputIds, ...)
        attentionMaskTensor = OnnxTensor.createTensor(env, tokens.attentionMask, ...)

        // Inference
        val outputs = ortSession.run(mapOf(
            "input_ids" to inputIdsTensor,
            "attention_mask" to attentionMaskTensor
        ))

        // Extract output
        outputTensor = outputs[0].value as OnnxTensor

        // Process results
        val intent = processOutputs(outputTensor)
        Result.Success(intent)

    } catch (e: Exception) {
        Log.e(TAG, "Classification failed", e)
        Result.Error(exception = e, message = "Classification failed: ${e.message}")
    } finally {
        // CRITICAL: Always cleanup tensors
        inputIdsTensor?.close()
        attentionMaskTensor?.close()
        outputTensor?.close()
    }
}
```
- **Acceptance Criteria:**
  - [ ] try-finally wraps all tensor operations
  - [ ] All tensor variables nullable and closed in finally
  - [ ] Compiles without errors

#### Task 1.2.2: Create Memory Leak Test
- **ID:** P0-LEAK-002
- **Effort:** 3 hours
- **Dependencies:** P0-LEAK-001
- **Files to Create:**
  - `IntentClassifierMemoryLeakTest.kt`
- **Test Implementation:**
```kotlin
@Test
fun testNoMemoryLeakOnErrors() = runBlocking {
    val classifier = IntentClassifier.getInstance(context)

    // Force garbage collection baseline
    System.gc()
    Thread.sleep(100)
    val memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

    // Run 10,000 classifications with 50% induced errors
    repeat(10000) { i ->
        val result = if (i % 2 == 0) {
            // Normal case
            classifier.classifyIntent("test utterance $i")
        } else {
            // Induce error by corrupting input
            classifier.classifyIntent("")
        }
    }

    // Force garbage collection
    System.gc()
    Thread.sleep(100)
    val memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

    // Assert memory growth <10MB (allows for GC overhead)
    val memoryGrowth = (memoryAfter - memoryBefore) / 1024 / 1024
    assertThat(memoryGrowth).isLessThan(10)
}
```
- **Acceptance Criteria:**
  - [ ] Test runs 10,000+ classifications
  - [ ] Memory stable (growth <10MB)
  - [ ] Test passes consistently (3/3 runs)

#### Task 1.2.3: Profile with Android Profiler
- **ID:** P0-LEAK-003
- **Effort:** 2 hours
- **Dependencies:** P0-LEAK-002
- **Steps:**
  - [ ] Run app with Memory Profiler attached
  - [ ] Trigger 1,000 classifications
  - [ ] Capture heap dump before/after
  - [ ] Verify no OnnxTensor instances retained
- **Acceptance Criteria:**
  - [ ] Heap dump shows zero leaked OnnxTensor objects
  - [ ] Memory graph shows stable sawtooth pattern (GC working)

---

### EPIC 1.3: Enable and Fix RAG Disabled Tests [P0 - CRITICAL]
**Current State:** 11 tests disabled, unknown quality
**Target State:** All tests enabled and passing
**Impact:** +2% overall grade (RAG 90%→92%)

#### Task 1.3.1: Enable RAGChatViewModelTest
- **ID:** P0-TEST-001
- **Effort:** 3 hours
- **Files to Modify:**
  - `RAGChatViewModelTest.kt.disabled` → `RAGChatViewModelTest.kt`
- **Process:**
  1. Rename file (remove `.disabled`)
  2. Run tests → identify failures
  3. Fix test expectations if API changed
  4. Fix bugs in implementation if tests are correct
- **Acceptance Criteria:**
  - [ ] All test methods passing
  - [ ] No @Ignore annotations added

#### Task 1.3.2: Enable Remaining 10 RAG Tests
- **ID:** P0-TEST-002
- **Effort:** 12 hours
- **Dependencies:** P0-TEST-001 (pattern established)
- **Files to Enable:**
  - `RAGChatEngineIntegrationTest.kt.disabled`
  - `DocumentParserIntegrationTest.kt.disabled`
  - `RAGPerformanceBenchmark.kt.disabled`
  - `Phase3OptimizationBenchmark.kt.disabled`
  - `RAGEndToEndTest.kt.disabled`
  - `BookmarkRepositoryTest.kt.disabled`
  - `SQLiteRAGRepositoryTest.kt.disabled`
  - `AnnotationRepositoryTest.kt.disabled`
  - `ApiKeyEncryptionTest.kt.disabled` (LLM)
  - `ModelManagerIntegrationExample.kt.disabled` (LLM)
- **Acceptance Criteria:**
  - [ ] All 11 tests enabled
  - [ ] All 11 tests passing
  - [ ] Test coverage report shows >90% for RAG module

---

## PHASE 2: HIGH PRIORITY GAPS (P1) - 28 tasks, 48 hours, Weeks 2-3

### Domain Impact: NLU 95%→100%, RAG 92%→98%, Tokenization 88%→95%

---

### EPIC 2.1: Multi-Locale Support [P1 - HIGH]
**Current State:** Hardcoded to "en-US"
**Target State:** 52 languages supported
**Impact:** +3% NLU grade

#### Task 2.1.1: Create LocaleManager
- **ID:** P1-LOCALE-001
- **Effort:** 3 hours
- **Files to Create:**
  - `Universal/AVA/Features/NLU/src/commonMain/kotlin/com/augmentalis/ava/features/nlu/locale/LocaleManager.kt`
- **Implementation:**
```kotlin
class LocaleManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("nlu_prefs", Context.MODE_PRIVATE)

    fun getCurrentLocale(): String {
        // Priority: User override > System default
        return prefs.getString("locale", null)
            ?: Locale.getDefault().toLanguageTag()
    }

    fun setLocale(locale: String) {
        prefs.edit().putString("locale", locale).apply()
    }

    fun getFallbackChain(locale: String): List<String> {
        // e.g., "fr-FR" → ["fr-FR", "fr", "en-US"]
        val parts = locale.split("-")
        return when (parts.size) {
            2 -> listOf(locale, parts[0], "en-US")
            1 -> listOf(locale, "en-US")
            else -> listOf("en-US")
        }
    }
}
```
- **Acceptance Criteria:**
  - [ ] getCurrentLocale() returns system locale by default
  - [ ] setLocale() persists selection
  - [ ] getFallbackChain() generates correct chain

#### Task 2.1.2: Remove Hardcoded "en-US"
- **ID:** P1-LOCALE-002
- **Effort:** 2 hours
- **Dependencies:** P1-LOCALE-001
- **Files to Modify:**
  - `IntentClassifier.kt:580`
- **Changes:**
```kotlin
// BEFORE
val cachedEmbeddings = embeddingDao.getAllEmbeddingsForLocale("en-US") // TODO

// AFTER
val locale = localeManager.getCurrentLocale()
val fallbackChain = localeManager.getFallbackChain(locale)

val cachedEmbeddings = fallbackChain.firstNotNullOfOrNull { localeCode ->
    embeddingDao.getAllEmbeddingsForLocale(localeCode)
        .takeIf { it.isNotEmpty() }
} ?: emptyList()
```
- **Acceptance Criteria:**
  - [ ] No hardcoded "en-US" strings
  - [ ] Falls back correctly (fr-FR → fr → en-US)
  - [ ] Tests pass for 5 locales

#### Task 2.1.3: Add Locale to Embedding Computation
- **ID:** P1-LOCALE-003
- **Effort:** 2 hours
- **Files to Modify:**
  - `AonEmbeddingComputer.kt`
- **Changes:**
  - Add `locale: String` parameter to `computeEmbedding()`
  - Store locale in `IntentEmbeddingEntity`
- **Acceptance Criteria:**
  - [ ] Embeddings stored per locale
  - [ ] Database schema supports multiple locales

#### Task 2.1.4: Test 5 Languages
- **ID:** P1-LOCALE-004
- **Effort:** 3 hours
- **Dependencies:** P1-LOCALE-002, P1-LOCALE-003
- **Test Languages:** en, es, fr, de, zh
- **Test Cases:**
```kotlin
@Test
fun testMultiLocaleClassification() {
    val testCases = mapOf(
        "en-US" to "turn on the lights",
        "es-ES" to "enciende las luces",
        "fr-FR" to "allume les lumières",
        "de-DE" to "schalte das Licht ein",
        "zh-CN" to "打开灯"
    )

    testCases.forEach { (locale, utterance) ->
        localeManager.setLocale(locale)
        val result = classifier.classifyIntent(utterance)
        assertThat(result).isSuccess()
        assertThat(result.data.id).isEqualTo("lights_on")
    }
}
```
- **Acceptance Criteria:**
  - [ ] All 5 languages classify correctly
  - [ ] Fallback works when locale not available

---

### EPIC 2.2: iOS Platform Implementation [P1 - HIGH]
**Current State:** 9 NotImplementedError stubs
**Target State:** Full iOS NLU + RAG support
**Impact:** +3% NLU, +2% RAG

#### Task 2.2.1: Setup iOS Build Configuration
- **ID:** P1-IOS-001
- **Effort:** 2 hours
- **Files to Modify:**
  - `build.gradle.kts` - Add iOS targets
- **Acceptance Criteria:**
  - [ ] iOS target compiles
  - [ ] Xcode project generated

#### Task 2.2.2: Implement iOS BertTokenizer
- **ID:** P1-IOS-002
- **Effort:** 6 hours
- **Dependencies:** P1-IOS-001
- **Files to Create:**
  - `Universal/AVA/Features/NLU/src/iosMain/kotlin/com/augmentalis/ava/features/nlu/BertTokenizer.kt`
  - `ios/Sources/AVA/WordPieceTokenizer.swift`
- **Swift Implementation:**
```swift
import Foundation
import NaturalLanguage

@objc public class WordPieceTokenizer: NSObject {
    private let vocabulary: [String: Int]

    @objc public init(vocabPath: String) throws {
        // Load vocab.txt
        let contents = try String(contentsOfFile: vocabPath)
        var vocab = [String: Int]()
        contents.enumerateLines { line, _ in
            let index = vocab.count
            vocab[line] = index
        }
        self.vocabulary = vocab
    }

    @objc public func tokenize(_ text: String) -> TokenizationResult {
        // WordPiece tokenization logic
        var inputIds = [Int]()
        var attentionMask = [Int]()

        // Add [CLS]
        inputIds.append(101)
        attentionMask.append(1)

        // Tokenize words
        let words = text.lowercased().split(separator: " ")
        for word in words {
            let tokens = tokenizeWord(String(word))
            inputIds.append(contentsOf: tokens)
            attentionMask.append(contentsOf: Array(repeating: 1, count: tokens.count))
        }

        // Add [SEP]
        inputIds.append(102)
        attentionMask.append(1)

        // Pad to 128
        while inputIds.count < 128 {
            inputIds.append(0)
            attentionMask.append(0)
        }

        return TokenizationResult(inputIds: inputIds, attentionMask: attentionMask)
    }

    private func tokenizeWord(_ word: String) -> [Int] {
        // WordPiece subword tokenization
        // ...implementation...
    }
}
```
- **Kotlin Wrapper:**
```kotlin
actual class BertTokenizer actual constructor(private val context: Any) {
    private val tokenizer: WordPieceTokenizer

    init {
        val vocabPath = // path to vocab.txt
        tokenizer = WordPieceTokenizer(vocabPath)
    }

    actual fun tokenize(text: String): TokenizedInput {
        val result = tokenizer.tokenize(text)
        return TokenizedInput(
            inputIds = result.inputIds.toLongArray(),
            attentionMask = result.attentionMask.toLongArray()
        )
    }
}
```
- **Acceptance Criteria:**
  - [ ] iOS tokenizer produces same output as Android
  - [ ] Tests pass on iOS simulator
  - [ ] Performance: <10ms on iPhone 12

#### Task 2.2.3: Implement iOS ModelManager
- **ID:** P1-IOS-003
- **Effort:** 4 hours
- **Files to Create:**
  - `Universal/AVA/Features/NLU/src/iosMain/kotlin/com/augmentalis/ava/features/nlu/ModelManager.kt`
- **Implementation:**
```kotlin
actual class ModelManager actual constructor(private val context: Any) {
    private val documentsDir = NSFileManager.defaultManager
        .URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
        .first() as NSURL

    actual suspend fun downloadModelsIfNeeded(
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.Default) {
        // Use NSURLSession for download
        val config = NSURLSessionConfiguration.defaultSessionConfiguration()
        val session = NSURLSession.sessionWithConfiguration(config)

        val request = NSURLRequest(URL = NSURL(string = modelUrl))
        val task = session.downloadTaskWithRequest(request) { location, response, error ->
            // Handle download
        }
        task.resume()

        Result.Success(Unit)
    }
}
```
- **Acceptance Criteria:**
  - [ ] Model downloads on iOS
  - [ ] Progress tracking works
  - [ ] Models stored in Documents directory

#### Task 2.2.4: Implement iOS IntentClassifier with Core ML
- **ID:** P1-IOS-004
- **Effort:** 8 hours
- **Dependencies:** P1-IOS-002, P1-IOS-003
- **Files to Create:**
  - `Universal/AVA/Features/NLU/src/iosMain/kotlin/com/augmentalis/ava/features/nlu/IntentClassifier.kt`
  - `ios/Sources/AVA/CoreMLInference.swift`
- **Steps:**
  1. Convert ONNX model to Core ML (`.mlmodel`)
  2. Wrap Core ML model in Swift
  3. Call from Kotlin
- **Acceptance Criteria:**
  - [ ] Classification works on iOS
  - [ ] Accuracy matches Android (±1%)
  - [ ] Performance: <100ms on iPhone 12

#### Task 2.2.5-2.2.8: iOS RAG Implementation
- **ID:** P1-IOS-005 through P1-IOS-008
- **Effort:** 8 hours total
- **Components:**
  - DocumentParserFactory (PDF, TXT, DOCX)
  - EmbeddingProviderFactory (ONNX)
- **Acceptance Criteria:**
  - [ ] All RAG parsers work on iOS
  - [ ] Embeddings generated correctly

#### Task 2.2.9: iOS Integration Tests
- **ID:** P1-IOS-009
- **Effort:** 4 hours
- **Tests:**
  - [ ] End-to-end classification
  - [ ] RAG document processing
  - [ ] Model download
- **Acceptance Criteria:**
  - [ ] All tests pass on iOS simulator
  - [ ] Tests pass on iPhone 12+ device

---

### EPIC 2.3: Desktop Platform Implementation [P1 - HIGH]
**Current State:** 9 NotImplementedError stubs
**Target State:** Full Desktop NLU + RAG support
**Impact:** +2% NLU, +1% RAG

#### Task 2.3.1-2.3.9: Desktop Implementation (Similar to iOS)
- **ID:** P1-DESKTOP-001 through P1-DESKTOP-009
- **Effort:** 16 hours total
- **Approach:**
  - Use HuggingFace tokenizers (JVM)
  - ONNX Runtime Desktop
  - Standard Java File I/O
- **Acceptance Criteria:**
  - [ ] Works on Windows, macOS, Linux
  - [ ] Performance: <80ms inference on Intel i5

---

### EPIC 2.4: Hybrid Search for RAG [P1 - HIGH]
**Current State:** Semantic search only
**Target State:** BM25 + Semantic with RRF
**Impact:** +4% RAG (92%→96%)

#### Task 2.4.1: Add FTS4 Table to Database
- **ID:** P1-RAG-001
- **Effort:** 2 hours
- **Files to Modify:**
  - `RAGDatabase.kt`
- **Migration v3→v4:**
```kotlin
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create FTS4 virtual table
        database.execSQL("""
            CREATE VIRTUAL TABLE chunks_fts USING fts4(
                content='chunks',
                chunk_id,
                content
            )
        """)

        // Populate FTS table from existing chunks
        database.execSQL("""
            INSERT INTO chunks_fts(chunk_id, content)
            SELECT id, content FROM chunks
        """)

        // Create triggers for auto-sync
        database.execSQL("""
            CREATE TRIGGER chunks_fts_insert AFTER INSERT ON chunks BEGIN
                INSERT INTO chunks_fts(chunk_id, content)
                VALUES (new.id, new.content);
            END
        """)

        database.execSQL("""
            CREATE TRIGGER chunks_fts_update AFTER UPDATE ON chunks BEGIN
                UPDATE chunks_fts SET content = new.content
                WHERE chunk_id = old.id;
            END
        """)

        database.execSQL("""
            CREATE TRIGGER chunks_fts_delete AFTER DELETE ON chunks BEGIN
                DELETE FROM chunks_fts WHERE chunk_id = old.id;
            END
        """)
    }
}
```
- **Acceptance Criteria:**
  - [ ] Migration runs successfully
  - [ ] FTS table populated
  - [ ] Triggers working

#### Task 2.4.2: Implement BM25 Scorer
- **ID:** P1-RAG-002
- **Effort:** 4 hours
- **Files to Create:**
  - `Universal/AVA/Features/RAG/src/commonMain/kotlin/com/augmentalis/ava/features/rag/search/BM25Scorer.kt`
- **Implementation:**
```kotlin
class BM25Scorer(
    private val k1: Float = 1.5f,
    private val b: Float = 0.75f,
    private val avgDocLength: Float = 500f
) {
    fun score(
        query: String,
        document: String,
        totalDocs: Int,
        termDocFrequencies: Map<String, Int>
    ): Float {
        val queryTerms = query.lowercase().split(" ")
        val docTerms = document.lowercase().split(" ")
        val docLength = docTerms.size

        var score = 0f
        for (term in queryTerms) {
            val tf = docTerms.count { it == term }.toFloat()
            val df = termDocFrequencies[term] ?: 1
            val idf = ln((totalDocs - df + 0.5f) / (df + 0.5f))

            val numerator = tf * (k1 + 1f)
            val denominator = tf + k1 * (1f - b + b * (docLength / avgDocLength))

            score += idf * (numerator / denominator)
        }

        return score
    }
}
```
- **Acceptance Criteria:**
  - [ ] BM25 scores match reference implementation
  - [ ] Tests pass with known query-document pairs

#### Task 2.4.3: Implement Reciprocal Rank Fusion
- **ID:** P1-RAG-003
- **Effort:** 3 hours
- **Files to Create:**
  - `Universal/AVA/Features/RAG/src/commonMain/kotlin/com/augmentalis/ava/features/rag/search/ReciprocalRankFusion.kt`
- **Implementation:**
```kotlin
object ReciprocalRankFusion {
    fun fuse(
        semanticResults: List<Pair<String, Float>>,  // chunkId to similarity
        keywordResults: List<Pair<String, Float>>,   // chunkId to BM25 score
        k: Int = 60
    ): List<Pair<String, Float>> {
        val scores = mutableMapOf<String, Float>()

        // Add semantic scores
        semanticResults.forEachIndexed { rank, (chunkId, _) ->
            scores[chunkId] = (scores[chunkId] ?: 0f) + (1f / (k + rank + 1))
        }

        // Add keyword scores
        keywordResults.forEachIndexed { rank, (chunkId, _) ->
            scores[chunkId] = (scores[chunkId] ?: 0f) + (1f / (k + rank + 1))
        }

        // Sort by fused score
        return scores.entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }
    }
}
```
- **Acceptance Criteria:**
  - [ ] RRF correctly combines rankings
  - [ ] Tests verify fusion logic

#### Task 2.4.4: Integrate Hybrid Search into Repository
- **ID:** P1-RAG-004
- **Effort:** 4 hours
- **Dependencies:** P1-RAG-001, P1-RAG-002, P1-RAG-003
- **Files to Modify:**
  - `SQLiteRAGRepository.kt:290-468`
- **Implementation:**
```kotlin
override suspend fun search(query: SearchQuery): Result<SearchResponse> {
    val startTime = System.currentTimeMillis()

    // 1. Generate query embedding (semantic)
    val queryEmbedding = embeddingProvider.embed(query.query).getOrThrow()

    // 2. Parallel search
    val semanticResults = async { searchSemantic(queryEmbedding.values, query) }
    val keywordResults = async { searchKeyword(query.query, query) }

    // 3. Fuse results
    val fusedResults = ReciprocalRankFusion.fuse(
        semanticResults.await().map { it.first.id to it.second },
        keywordResults.await().map { it.first.id to it.second }
    )

    // 4. Retrieve full chunks for top results
    val topChunks = fusedResults
        .take(query.maxResults)
        .mapNotNull { (chunkId, score) ->
            chunkDao.getChunkById(chunkId)?.let { chunk ->
                SearchResult(chunk, score, ...)
            }
        }

    return SearchResponse(
        query = query.query,
        results = topChunks,
        totalResults = fusedResults.size,
        searchTimeMs = System.currentTimeMillis() - startTime
    )
}
```
- **Acceptance Criteria:**
  - [ ] Hybrid search works correctly
  - [ ] Performance: <50ms for 200k chunks
  - [ ] Benchmark shows 15%+ accuracy improvement

#### Task 2.4.5: Benchmark Hybrid vs Semantic-Only
- **ID:** P1-RAG-005
- **Effort:** 2 hours
- **Dependencies:** P1-RAG-004
- **Test Queries:**
  - 10 keyword queries ("find document about API authentication")
  - 10 semantic queries ("how do I secure my endpoints?")
  - 10 mixed queries
- **Metrics:**
  - Precision@10
  - Recall@10
  - MRR (Mean Reciprocal Rank)
- **Acceptance Criteria:**
  - [ ] Hybrid ≥ semantic-only on all metrics
  - [ ] Keyword queries show biggest improvement

---

### EPIC 2.5: Model Versioning and Migration [P1 - HIGH]
**Current State:** No version tracking
**Target State:** Auto-migration on model updates
**Impact:** +3% Model Management (92%→95%)

#### Task 2.5.1: Add Database Schema for Versioning
- **ID:** P1-MODEL-001
- **Effort:** 2 hours
- **Files to Modify:**
  - `AVADatabase.kt` - Migration v6→v7
  - `IntentEmbeddingEntity.kt` - Add `schema_version` field
- **New Table:**
```kotlin
@Entity(tableName = "embedding_metadata")
data class EmbeddingMetadata(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "model_name") val modelName: String,
    @ColumnInfo(name = "model_version") val modelVersion: String,
    @ColumnInfo(name = "embedding_dimension") val embeddingDimension: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true
)
```
- **Acceptance Criteria:**
  - [ ] Migration runs successfully
  - [ ] New fields available

#### Task 2.5.2: Implement Version Checking Logic
- **ID:** P1-MODEL-002
- **Effort:** 3 hours
- **Dependencies:** P1-MODEL-001
- **Files to Modify:**
  - `ModelManager.kt`
- **Implementation:**
```kotlin
data class ModelVersion(
    val name: String,
    val version: String,
    val dimension: Int,
    val checksum: String
)

suspend fun checkModelVersion(): Result<VersionStatus> {
    val currentVersion = ModelVersion(
        name = "mALBERT-base-v2",
        version = "tvm-0.23",
        dimension = 768,
        checksum = "sha256:..."
    )

    val storedMetadata = metadataDao.getActiveMetadata()

    return when {
        storedMetadata == null -> Result.Success(VersionStatus.NewInstall)
        storedMetadata.modelVersion != currentVersion.version -> {
            Result.Success(VersionStatus.NeedsMigration(
                from = storedMetadata.modelVersion,
                to = currentVersion.version
            ))
        }
        else -> Result.Success(VersionStatus.Current)
    }
}
```
- **Acceptance Criteria:**
  - [ ] Version check detects mismatches
  - [ ] Returns correct status

#### Task 2.5.3: Implement Automatic Migration
- **ID:** P1-MODEL-003
- **Effort:** 4 hours
- **Dependencies:** P1-MODEL-002
- **Implementation:**
```kotlin
suspend fun migrateEmbeddings(): Result<Unit> = withContext(Dispatchers.IO) {
    try {
        Log.i(TAG, "Starting embedding migration...")

        // 1. Mark old embeddings as inactive
        embeddingDao.updateAllInactive()

        // 2. Get all ontologies
        val ontologies = ontologyDao.getAllOntologies()

        // 3. Recompute embeddings with progress
        val total = ontologies.size
        ontologies.forEachIndexed { index, ontology ->
            val embedding = embeddingComputer.compute(ontology)
            embeddingDao.insert(embedding)

            if (index % 100 == 0) {
                val progress = (index.toFloat() / total * 100).toInt()
                Log.i(TAG, "Migration progress: $progress% ($index/$total)")
            }
        }

        // 4. Update metadata
        metadataDao.insert(newMetadata)

        // 5. Delete old inactive embeddings (optional - keep for rollback)
        // embeddingDao.deleteInactive()

        Log.i(TAG, "Migration complete! Recomputed $total embeddings")
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(exception = e, message = "Migration failed")
    }
}
```
- **Acceptance Criteria:**
  - [ ] Migration completes successfully
  - [ ] All embeddings recomputed
  - [ ] Metadata updated

#### Task 2.5.4: Integrate into App Startup
- **ID:** P1-MODEL-004
- **Effort:** 2 hours
- **Dependencies:** P1-MODEL-003
- **Files to Modify:**
  - `NLUInitializer.kt`
- **Changes:**
```kotlin
suspend fun initialize(): Result<Unit> {
    // Check model version
    when (val status = modelManager.checkModelVersion().getOrThrow()) {
        is VersionStatus.NeedsMigration -> {
            Log.i(TAG, "Model version changed: ${status.from} → ${status.to}")
            Log.i(TAG, "Starting automatic migration...")

            // Show migration UI (progress dialog)
            withContext(Dispatchers.Main) {
                showMigrationProgress()
            }

            // Run migration
            modelManager.migrateEmbeddings().getOrThrow()

            withContext(Dispatchers.Main) {
                hideMigrationProgress()
            }
        }
        is VersionStatus.NewInstall -> {
            Log.i(TAG, "First install - computing embeddings...")
        }
        is VersionStatus.Current -> {
            Log.i(TAG, "Model version current - no migration needed")
        }
    }

    // Continue with normal initialization...
}
```
- **Acceptance Criteria:**
  - [ ] Migration runs on app startup if needed
  - [ ] User sees progress (not frozen)
  - [ ] Tests pass for migration scenarios

---

## PHASE 3: MEDIUM PRIORITY IMPROVEMENTS (P2) - 15 tasks, 24 hours, Weeks 4-5

### Domain Impact: RAG 96%→100%, Tokenization 95%→100%, Database 98%→100%

---

### EPIC 3.1: INT8 Quantization for RAG [P2 - MEDIUM]
**Current State:** Float32 storage (1,536 bytes/384-dim)
**Target State:** INT8 storage (384 bytes) - 75% savings
**Impact:** +2% RAG (96%→98%)

#### Task 3.1.1-3.1.5: Quantization Implementation
- **ID:** P2-QUANT-001 through P2-QUANT-005
- **Effort:** 8 hours total
- **Key Implementation:**
```kotlin
fun quantizeToInt8(embedding: FloatArray): Triple<ByteArray, Float, Float> {
    val min = embedding.minOrNull() ?: 0f
    val max = embedding.maxOrNull() ?: 0f
    val scale = (max - min) / 255f
    val offset = min

    val quantized = ByteArray(embedding.size) { i ->
        ((embedding[i] - offset) / scale).toInt().coerceIn(0, 255).toByte()
    }

    return Triple(quantized, scale, offset)
}

fun dequantizeFromInt8(quantized: ByteArray, scale: Float, offset: Float): FloatArray {
    return FloatArray(quantized.size) { i ->
        (quantized[i].toInt() and 0xFF).toFloat() * scale + offset
    }
}
```
- **Acceptance Criteria:**
  - [ ] 75% space reduction
  - [ ] Accuracy loss <2%
  - [ ] Tests pass

---

### EPIC 3.2: Batch ONNX Inference [P2 - MEDIUM]
**Current State:** Sequential processing (10s for 1,000 chunks)
**Target State:** True batch processing (~500ms)
**Impact:** +1% Database (98%→99%)

#### Task 3.2.1-3.2.4: Batch Implementation
- **ID:** P2-BATCH-001 through P2-BATCH-004
- **Effort:** 6 hours total
- **Key Change:**
```kotlin
override suspend fun embedBatch(texts: List<String>): Result<List<Embedding.Float32>> {
    val batchSize = 32
    val batches = texts.chunked(batchSize)
    val allEmbeddings = mutableListOf<Embedding.Float32>()

    for (batch in batches) {
        val batchInputIds = stackTensors(batch.map { tokenize(it).inputIds })
        val batchAttentionMask = stackTensors(batch.map { tokenize(it).attentionMask })

        // Single ONNX call for entire batch
        val inputIdsTensor = OnnxTensor.createTensor(
            env, batchInputIds,
            longArrayOf(batch.size.toLong(), 128L)
        )
        val attentionMaskTensor = OnnxTensor.createTensor(
            env, batchAttentionMask,
            longArrayOf(batch.size.toLong(), 128L)
        )

        try {
            val outputs = session.run(mapOf(
                "input_ids" to inputIdsTensor,
                "attention_mask" to attentionMaskTensor
            ))

            allEmbeddings.addAll(extractBatchEmbeddings(outputs, batch.size))
        } finally {
            inputIdsTensor.close()
            attentionMaskTensor.close()
        }
    }

    return Result.success(allEmbeddings)
}
```
- **Acceptance Criteria:**
  - [ ] 20x speedup achieved
  - [ ] No OOM on 10k batch
  - [ ] Tests pass

---

### EPIC 3.3-3.6: Additional P2 Improvements
- **Cross-Encoder Reranking** (4 tasks, 5 hours) - +1% RAG
- **Query Caching** (2 tasks, 2 hours) - +0.5% overall
- **Metadata Filtering** (2 tasks, 2 hours) - +0.5% RAG
- **Model Checksums** (1 task, 2 hours) - +2% Model Management

---

## PHASE 4: POLISH & OPTIMIZATION (P3) - 12 tasks, 12 hours, Week 6

### Domain Impact: Tokenization 98%→100%, Final polish to 100%

---

### EPIC 4.1-4.6: P3 Features (Optional for 100%)
- **JS/Web Platform** (4 hours) - Complete KMP vision
- **Cloud/LLM Providers** (2 hours) - Provider flexibility
- **EPUB Parser** (1 hour) - Additional format
- **AON Encryption** (2 hours) - Security
- **Hot-Swapping** (1 hour) - UX improvement
- **Telemetry** (2 hours) - Observability

---

## TASK METRICS SUMMARY

### Total Effort by Phase

| Phase | Tasks | Hours | Grade Impact |
|-------|-------|-------|--------------|
| P0 (Week 1) | 18 | 36 | 90% → 95% |
| P1 (Weeks 2-3) | 28 | 48 | 95% → 98% |
| P2 (Weeks 4-5) | 15 | 24 | 98% → 100% |
| P3 (Week 6) | 12 | 12 | 100% (maintain) |
| **TOTAL** | **73** | **120** | **90% → 100%** |

### Grade Progression by Domain

| Domain | Current | After P0 | After P1 | After P2 | After P3 |
|--------|---------|----------|----------|----------|----------|
| NLU/NLM | 95% | 96% | 100% | 100% | 100% |
| RAG | 90% | 92% | 96% | 100% | 100% |
| Database | 98% | 98% | 98% | 100% | 100% |
| Model Mgmt | 92% | 92% | 95% | 100% | 100% |
| Error Handling | 95% | 100% | 100% | 100% | 100% |
| Tokenization | 88% | 88% | 95% | 98% | 100% |
| **OVERALL** | **90%** | **95%** | **98%** | **100%** | **100%** |

---

## SUCCESS CRITERIA FOR 100%

### Mandatory (Must Complete)
- [x] All 61 tasks in Phases 0-2 completed
- [x] All tests passing (>90% coverage)
- [x] No P0/P1 bugs remaining
- [x] All 6 domains at 100%
- [x] Production deployment successful

### Optional (Maintains 100%)
- [ ] 12 tasks in Phase 3 completed
- [ ] All platforms supported (Android, iOS, Desktop, Web)
- [ ] Full KMP vision realized

---

**Document Version:** 1.0
**Last Updated:** 2025-11-27
**Total Tasks:** 73
**Estimated Completion:** 2025-01-08 (6 weeks)
**Target Grade:** 100% across all domains
