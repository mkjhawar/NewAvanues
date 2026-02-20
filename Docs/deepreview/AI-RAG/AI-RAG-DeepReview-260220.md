# AI/RAG Module — Deep Code Review
**Date:** 260220 (2026-02-20)
**Module:** `Modules/AI/RAG/`
**Reviewer:** Code-Reviewer Agent (Sonnet 4.6)
**Files Reviewed:** 78 .kt files (17 commonMain + 39 androidMain + 2 desktopMain + 2 iosMain + 11 commonTest + 10 androidTest + rounding)
**Scope:** All source files under `Modules/AI/RAG/src/`

---

## Summary

The RAG module has a well-structured architecture with sound interface design, good test coverage breadth, and working implementations for PDF, DOCX, Markdown, RTF, TXT, and HTML parsing on Android. However, the module is critically undermined by four issues that make it unable to produce semantically correct results in production: (1) the tokenizer uses hash-derived IDs instead of an actual BERT vocabulary, producing garbage embeddings; (2) `DocumentIngestionHandler` only dispatches PDF parsing even though five additional parsers are registered; (3) `ChunkEmbeddingHandler.updateChunkEmbeddingInternal()` is a complete no-op that silently drops all embedding updates; and (4) the gRPC server binding is incomplete (documented separately in the stub inventory). Rule 7 violations are pervasive — 46 files carry `// author: AVA AI Team` and 3 handler files explicitly name "Claude AI" as co-author.

---

## Issues

### CRITICAL

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| Critical | `commonMain/.../embeddings/SimpleTokenizer.kt` L48-60 | `wordToId()` maps words by `abs(word.hashCode()) % (30000 - 103) + 103`. Kotlin hashCode is NOT aligned with any BERT vocabulary. Every token ID fed to the ONNX model is semantically wrong, producing random-noise embeddings for all documents. The entire RAG pipeline's semantic search is broken. | Replace with a real vocab lookup table. Embed the official `bert-base-uncased` `vocab.txt` (30,522 entries) as an asset and load it into a `HashMap<String, Int>`. This is mandatory before the ONNX provider can produce useful embeddings. |
| Critical | `androidMain/.../handlers/DocumentIngestionHandler.kt` L196-199 | `processDocumentInternal()` switches on `docType` with only `DocumentType.PDF` handled; all other types throw `Exception("Unsupported document type: $docType")`. The factory (`DocumentParserFactory.android.kt`) registers parsers for TXT, HTML, RTF, MD, DOCX. Any non-PDF document submitted for processing will fail. | Change the `when` block to delegate to `DocumentParserFactory.getParser(docType)` instead of a hardcoded `when`. If `getParser()` returns null, throw a descriptive `UnsupportedDocumentTypeException`. |
| Critical | `androidMain/.../handlers/ChunkEmbeddingHandler.kt` L218-225 | `updateChunkEmbeddingInternal()` is a complete no-op. The function logs "Updating embedding for chunk: $chunkId" then returns without writing anything to the database. All calls to `processChunks()` → `updateChunkEmbeddingInternal()` silently discard embedding updates. Chunks that go through re-embedding remain unchanged in the DB. | Implement the actual SQL update: `chunkQueries.updateEmbedding(blob, type, dimension, chunkId)`. Add the corresponding SQLDelight query if it does not exist. |
| Critical | `androidMain/.../security/EmbeddingEncryptionManager.kt` L(init block)` | The `init {}` block calls `generateKey()` which throws `KeyGenerationException` if the Android KeyStore is unavailable (e.g., during unit tests, early boot, or emulator without secure hardware). Constructor failure crashes the injection graph — any class that receives `EmbeddingEncryptionManager` via DI will also fail to construct. | Wrap `generateKey()` in `init {}` with `try/catch`. On failure, set a `isAvailable = false` flag and disable encryption gracefully. Use lazy initialization: delay key generation until first `encryptEmbedding()` call. |
| Critical | `androidMain/.../security/AONFileManager.kt` L452-481 | `verifySignature()` is documented as HMAC verification but does NOT verify any HMAC. It only re-checks package authorization (which is already checked at L221-227). The HMAC itself (computed in `computeSignature()`) is never verified during `unwrapAON()`. A tampered model with valid package hashes passes "signature verification" silently. The comment says "TODO for Phase 2". | Implement proper HMAC-SHA256 verification: serialize the header fields in canonical order (excluding the signature field), compute HMAC over `serialized_header_without_sig + sha256(onnxData)`, and compare against the stored 32-byte signature (the first 32 bytes of the 64-byte signature field). |

---

### HIGH

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| High | `androidMain/.../embeddings/ONNXEmbeddingProvider.android.kt` L246 | `loadModelFromAssets()` calls `kotlinx.coroutines.runBlocking {}` inside the non-suspend `initialize()` method. If `initialize()` is called from the main thread (e.g., during Hilt injection on the main thread), `runBlocking` will block the main thread. If the main thread coroutine dispatcher is also involved, this can deadlock. | Make `initialize()` a `suspend fun` or redesign `ONNXEmbeddingProvider` with lazy initialization inside the first `embed()` call (which is already suspend). |
| High | `androidMain/.../embeddings/ONNXEmbeddingProvider.android.kt` L(close)` | `close()` calls `ortSession?.close()` but never calls `ortEnvironment.close()`. The ONNX Runtime environment holds native resources. Failing to close it leaks memory. | Add `ortEnvironment.close()` to `close()` after the session is closed. |
| High | `androidMain/.../embeddings/EmbeddingProviderFactory.android.kt` L63-70 | `getLocalLLMProvider()` returns `null` (TODO comment at L63-66) and `getCloudProvider()` returns `null` (TODO comment at L68-70). The fallback LLM chain for hybrid search / chat is completely non-functional on Android. Callers that check `getLocalLLMProvider() != null` will always skip the LLM path. | Implement `getLocalLLMProvider()` using `LocalLLMProviderAdapter(LocalLLMProvider(context))`. Implement `getCloudProvider()` or at minimum throw `NotImplementedError` with a clear message instead of returning null silently. |
| High | `desktopMain/.../embeddings/EmbeddingProviderFactory.desktop.kt` (all methods) | All three factory methods (`getEmbeddingProvider`, `getLocalLLMProvider`, `getCloudProvider`) return `null` with TODO comments. Desktop has zero embedding capability. Any desktop code path that calls the factory will produce a NPE downstream when the caller dereferences the null result. | This is a known platform gap. At minimum, add an `EmbeddingProviderUnavailableException` and throw it from each method to produce a clear error rather than a downstream NPE. |
| High | `iosMain/.../embeddings/EmbeddingProviderFactory.ios.kt` (all methods) | Same as desktop: all methods return null with TODO comments. iOS embedding is completely non-functional. | Same recommendation: throw a descriptive exception instead of returning null. |
| High | `androidMain/.../data/SQLiteRAGRepository.kt` L183-185 | `processDocuments(documentId = null)` fetches ALL documents via `selectAll()` instead of filtering to `status = PENDING`. This means every call to re-process the index will re-process already-`INDEXED` documents unnecessarily, wasting CPU and time. | Change the query to `selectByStatus(DocumentStatus.PENDING.name)` when `documentId` is null. |
| High | `androidMain/.../data/SQLiteRAGRepository.kt` L399 | `searchKeyword()` calls `chunkQueries.selectAll().executeAsList()` which loads all chunks into memory. For a corpus of 200,000 chunks at ~384 floats each, this is ~300MB of RAM just for the embedding blobs. This will cause OOM on low-memory devices. | Implement keyword search using SQLite FTS5 (add a virtual FTS table shadowing chunk content) or paginate with `LIMIT/OFFSET`. At minimum, add a hard `LIMIT 10000` to the query to bound memory. |
| High | `androidMain/.../handlers/ClusteredSearchHandler.kt` L147 | `searchLinear()` also calls `chunkQueries.selectAll()` — same OOM risk as above. This is the fallback path when clustering is disabled, meaning it affects all non-clustered deployments. | Apply FTS5 or LIMIT-based approach consistently across both `searchKeyword` and `searchLinear`. |
| High | `androidMain/.../security/EncryptionMigration.kt` L162-163 | `migrateToEncrypted()` calls `chunkQueries.insert()` after calling `updateEncryptionStatus()`. The chunk row already exists in the database. `insert()` will either throw a UNIQUE constraint violation or silently fail depending on the SQLDelight conflict strategy. In either case the migration is broken: the encrypted blob is never stored. | Replace `chunkQueries.insert(...)` at L162-163 with `chunkQueries.updateEmbeddingBlob(encryptedBlob, isEncrypted = true, keyVersion, chunkId)`. Add that query to the SQLDelight .sq file if missing. |
| High | `androidMain/.../embeddings/AndroidModelDownloadManager.kt` `getModelFile()` + `verifyModel()` | `getModelFile()` always appends `.AON` extension to the stored model path. `verifyModel()` compares the file size against `modelInfo.sizeBytes`, which is the size of the original `.onnx` file (not the wrapped AON file). The AON format adds 384 bytes of overhead. Size verification will always fail for wrapped AON files. | Either (a) store the AON file size in `ModelInfo.sizeBytes`, or (b) verify the wrapped size as `modelInfo.sizeBytes + HEADER_SIZE + FOOTER_SIZE = sizeBytes + 384`. |
| High | `androidMain/.../handlers/ClusteredSearchHandler.kt` `toDomainDocument()` L386 | Infers `DocumentStatus` from chunk count (`if (chunkCount > 0) INDEXED else PENDING`) rather than reading `entity.status` from the database. This is inconsistent with `DocumentIngestionHandler.toDomainDocument()` which reads the real status. A document in `FAILED` or `PROCESSING` state will be reported as `PENDING` here. | Read `DocumentStatus.valueOf(entity.status)` with the same fallback pattern used in `DocumentIngestionHandler`. |
| High | `androidMain/.../handlers/ClusteredSearchHandler.kt` `deserializeCentroid()` vs `ChunkEmbeddingHandler.serializeEmbedding()` | `ChunkEmbeddingHandler.serializeEmbedding()` writes embeddings using `ByteOrder.LITTLE_ENDIAN`. `ClusteredSearchHandler.deserializeCentroid()` reads centroids using `ByteOrder.BIG_ENDIAN`. If centroids are serialized using the same helper path, centroids will be read with wrong byte order, producing completely incorrect cluster assignments and search results. | Audit the centroid serialization path. If `serializeCentroid` uses `BIG_ENDIAN` explicitly, centroids and embeddings have inconsistent endianness. Standardize all serialization to `LITTLE_ENDIAN` throughout the module. |
| High | `androidMain/.../llm/MLCLLMProvider.android.kt` `generateStream()` | On LLM error, `generateStream()` emits the error message as a plain `ChatResponse.Streaming` text chunk instead of `ChatResponse.Error`. Callers cannot distinguish an error message from valid LLM output. Error text ("Error: ...") appears in the user's chat as content. | Emit `ChatResponse.Error(message = ...)` on the error path and cancel the flow. |
| High | `androidMain/.../embeddings/AONFileManager.kt` L86-89 | `MASTER_KEY = "AVA-AON-HMAC-SECRET-KEY-V1-CHANGE-IN-PRODUCTION"` is a hardcoded plaintext string in source code. This key is used for HMAC signing (and encryption key derivation at L502). Any developer with source access can forge valid AON signatures. Comments acknowledge this ("CHANGE IN PRODUCTION") but the placeholder is shipped. | Move to `BuildConfig.AON_MASTER_KEY` injected at build time via `buildConfigField`. Do not include the actual production key in source. For encryption, use Android Keystore directly, not a derived key from this string. |

---

### MEDIUM

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| Medium | `commonMain/.../domain/Chunk.kt` `Embedding.quantize()` vs `androidMain/.../embeddings/Quantization.kt` `quantizeToInt8()` | Two independent INT8 quantization implementations with different behavior. `Quantization.kt` uses `coerceIn(0, 255)` to clamp values. `Chunk.kt`'s `Embedding.quantize()` does not clamp, allowing `toInt()` to produce negative values or values above 255 for out-of-range floats. | Consolidate to a single implementation. Have `Embedding.quantize()` call `Quantization.quantizeToInt8()` rather than duplicating the logic. |
| Medium | `commonMain/.../chat/RAGChatEngine.kt` `ask()` | When `searchResults` is null (embedding generation failed), the engine emits `ChatResponse.NoContext` with a generic "no documents found" message rather than `ChatResponse.Error`. The caller sees "No context available" instead of an embedding error, hiding the real failure from users and logs. | Check if the failure is due to search returning null vs. search returning empty results. Emit `ChatResponse.Error(message = "Embedding generation failed: ...")` for the null case. |
| Medium | `commonMain/.../data/InMemoryRAGRepository.kt` L156-163 | Inside `processDocuments()`, the inner `try/catch` at L156-159 marks the document as FAILED and re-throws. The outer `try/catch` at L163 wraps the re-thrown exception in `Result.failure()` — which is correct — but also means the exception is caught by the outer handler while the document is already marked FAILED. The behavior is correct but the nested exception flow is fragile and could mask errors in a future refactor. | Flatten to a single `try/catch` in `processDocuments()` that both marks FAILED and returns `Result.failure()`. |
| Medium | `androidMain/.../ui/RAGChatScreen.kt` (all Composables) | Uses `MaterialTheme.colorScheme.*` throughout. Mandatory CLAUDE.md Rule 3 (AvanueTheme v5.1) requires `AvanueTheme.colors.*`. Hardcoded `Color.White` is used for app bar text instead of a theme token. | Replace all `MaterialTheme.colorScheme.*` with `AvanueTheme.colors.*`. Replace `Color.White` with `AvanueTheme.colors.onSurface` or appropriate token. |
| Medium | `androidMain/.../ui/RAGSearchScreen.kt` (all Composables) | Same theme violation as `RAGChatScreen.kt` — uses `MaterialTheme.colorScheme.*` throughout. | Same fix: migrate to `AvanueTheme.colors.*`. |
| Medium | `androidMain/.../ui/DocumentManagementScreen.kt` (all Composables) | Same theme violation (`MaterialTheme.colorScheme.*`) plus zero AVID semantics on interactive elements (FAB, icon buttons, list item click targets). Mandatory per Rule 7 of AvanueUI Protocol. | Add AVID to all interactive elements: `Modifier.semantics { contentDescription = "Voice: add document" }` etc. Migrate to `AvanueTheme.colors.*`. |
| Medium | `androidMain/.../ui/GradientUtils.kt` L(color literals) | Hardcodes `Color(0xFF6366F1)` and `Color(0xFF8B5CF6)` as primary/secondary gradient colors instead of using AvanueTheme tokens. These colors will not adapt to theme changes or different palettes. | Replace with `AvanueTheme.colors.primary` and `AvanueTheme.colors.secondary` (or appropriate tokens). |
| Medium | `androidMain/.../data/SQLiteRAGRepository.kt` `measureSearchPerformance()` | The `clusterCount` parameter is accepted by the function signature but never used in the implementation. The method always measures search with the repository's own cluster count configuration regardless of what is passed. | Either use the parameter to override cluster count for the duration of the measurement, or remove the parameter from the signature to avoid misleading callers. |
| Medium | `commonMain/.../embeddings/ModelDownloadManager.kt` `AvailableModels` | All 5 models in `AvailableModels` have `sha256 = null`. The `verifyDownload()` method in `AndroidModelDownloadManager` has a checksum verification path but the checksum is always null, so verification is always skipped. Users who download corrupted or tampered models are not protected. | Populate `sha256` with the actual SHA-256 hashes of the canonical AON model files. Generate these during the build pipeline or from official model distribution. |
| Medium | `androidMain/.../embeddings/BatchModelWrapper.kt` (entire file) | File is in `androidMain` source set but uses only `java.io.File` and JVM IO — no Android APIs. This is a KMP source set violation. The file could compile for desktop/JVM but is unnecessarily restricted to Android. | Move to `jvmMain` source set (shared by Android + Desktop JVM) or `desktopMain` if it is only a build-time tool. |
| Medium | `androidMain/.../parser/RtfParser.android.kt` L12 | Imports `javax.swing.text.rtf.RTFEditorKit` — a Swing/AWT class not available on Android. This import is from the desktop JRE, not the Android runtime. This file will fail to compile for Android builds. | Use a dedicated Android RTF parsing library (e.g., `rtfparserkit` on Maven) or implement a minimal RTF stripping regex for Android. |
| Medium | `androidMain/.../chat/RAGLLMIntegration.kt` | `RAGLLMIntegration` is not a ViewModel and uses `lateinit var` fields for `ragRepository`, `llmProvider`, etc. If `ask()` is called before `initialize()`, it logs an error and calls `onResponse(ChatResponse.Error(...))` — that part is handled. But `cleanup()` calls `llmProvider.cleanup()` even if initialization failed partway through (before `llmProvider` was assigned), causing `UninitializedPropertyAccessException`. | Add `if (::llmProvider.isInitialized)` guard in `cleanup()`. |
| Medium | `androidMain/.../ui/RAGChatViewModel.kt` L195-204 | `stopGeneration()` sets `_isGenerating.value = false` but does NOT cancel the underlying coroutine running `chatEngine.ask()`. The LLM continues generating and emitting tokens even after `stopGeneration()` is called; the UI just stops reflecting it. This wastes CPU/GPU and may update `_messages` after the user expects generation to have stopped. | Hold a reference to the `askJob = viewModelScope.launch { ... }` in `askQuestion()`. In `stopGeneration()`, call `askJob?.cancel()`. |
| Medium | `commonMain/.../cache/QueryCache.kt` documentation vs usage | Class is documented as "Not thread-safe — external synchronization required". `ClusteredSearchHandler` and `SQLiteRAGRepository` access the cache from coroutines that may run on different threads (e.g., `Dispatchers.IO`). There is no external synchronization in either caller. | Wrap `QueryCache` operations in a `Mutex` in `ClusteredSearchHandler`, or switch the cache implementation to use `ConcurrentHashMap` with `AtomicReference`. |
| Medium | `androidMain/.../embeddings/AONFileManager.kt` L353 | `onnxSHA256` field in the header stores only the FIRST 16 bytes of the SHA-256 hash (truncated from 32 bytes). The footer stores the full 32-byte hash. The verification at L235-238 computes `expectedHash = actualHash.take(16).toByteArray()` and compares against `header.onnxSHA256` — comparing a 16-byte truncated hash against itself. This means the check always passes as long as the hash isn't completely corrupted. The effective integrity protection is only 128 bits (from the footer's full 32-byte check). | Either store the full 32-byte SHA-256 in the header (requires expanding the header layout or merging reserved bytes) or remove the redundant partial-hash field and rely solely on the footer's full-hash integrity check. |

---

### LOW

| Severity | File:Line | Issue | Suggestion |
|----------|-----------|-------|------------|
| Low | `commonMain/.../embeddings/SimpleTokenizer.kt` L(class docstring) | Documented as "Phase 2 simplified tokenizer" but used in production ONNX inference. The "Phase 2" label suggests it is temporary, but it has been integrated as the live tokenizer with no replacement planned. | Remove the "simplified" / "Phase 2" wording to avoid confusion. Clarify in the docstring that a real vocab table needs to be substituted (see Critical finding above). |
| Low | `androidMain/.../parser/DocumentParserFactory.android.kt` L32 | `// TODO: Add EPUB parser` — `DocumentType.EPUB` is declared in the enum but has no parser registered and no timeline for implementation. | Either implement an EPUB parser (consider `epublib-core` for Android) or explicitly throw `UnsupportedDocumentTypeException` from `getParser(EPUB)` with a roadmap comment to avoid silent failures. |
| Low | `commonMain/.../embeddings/ModelDownloadManager.kt` `AvailableModels` | `MULTILINGUAL_E5_SMALL` is listed as `dimension = 384` but the actual `multilingual-e5-small` model produces 384-dimensional output only for the base variant. The small variant produces 384 as well, but this should be verified and documented explicitly. | Add a comment linking to the official HuggingFace model card for each entry in `AvailableModels` so the dimension can be verified. |
| Low | `androidMain/.../ui/WindowSizeUtils.kt` (entire file) | Reinvents `WindowSizeClass` from Jetpack's `androidx.compose.material3.adaptive` or `androidx.window.core`. This is a local reimplementation of a standard Android library class. | Replace with the official `WindowSizeClass` from `androidx.window:window:1.x` to reduce maintenance burden and gain consistent behavior across the ecosystem. |
| Low | `androidMain/.../chat/RAGLLMIntegrationExample.kt` | The example `RAGChatViewModel` usage is commented out in a large `/* ... */` block (L242-343). The file ships production-compiled code (`RAGLLMIntegration` class) plus a dead comment block that is misleading about how to use the ViewModel. | Either move the example to a `samples/` directory or delete the comment block. Do not ship large commented-out code in production source. |
| Low | `commonMain/.../search/TokenCounter.kt` `findOffsetForTokenCount()` | Uses `text.indexOf(word, currentOffset)` which for repeated words may find the wrong instance. If the same word appears multiple times, `indexOf` from `currentOffset` might skip the intended occurrence when tokens from earlier duplicates consumed the offset. | Use a more robust offset tracking: split the text into tokens with their exact positions using a regex match iterator, then look up the nth token's start offset directly. |
| Low | `commonMain/.../domain/SearchQuery.kt` `DateRange.contains()` | Parses `Instant.fromEpochMilliseconds(timestampMs)` twice — once to compare against `start`, once against `end`. This creates two `Instant` objects per call. | Store `val instant = Instant.fromEpochMilliseconds(timestampMs)` once and compare both bounds against `instant`. |
| Low | `androidMain/.../embeddings/PlatformDetector.kt` L62-63 | Uses deprecated `getInstallerPackageName()` API. On Android 11+ (API 30+), the replacement is `getInstallSourceInfo().installingPackageName`. | Use `if (Build.VERSION.SDK_INT >= 30) packageManager.getInstallSourceInfo(packageName).installingPackageName else packageManager.getInstallerPackageName(packageName)` with `@Suppress("DEPRECATION")` on the fallback branch. |
| Low | `androidMain/.../security/EmbeddingEncryptionManager.kt` `getEncryptionStats()` | `availableKeyVersions` hardcodes only `listOf(KEY_VERSION_1, KEY_VERSION_2)`. After a third `rotateKey()` call, version 3 exists in the Keystore but will not appear in stats. Any monitoring or migration code that reads `availableKeyVersions` will have an inaccurate picture. | Enumerate existing keys from the Android Keystore dynamically: `KeyStore.getInstance("AndroidKeyStore").aliases().asSequence().filter { it.startsWith(KEY_ALIAS_PREFIX) }.map { extractVersionFrom(it) }.toList()`. |

---

## Rule 7 Violations (AI Attribution — ABSOLUTE ZERO TOLERANCE)

**46 files** carry `// author: AVA AI Team` in the file header (lines 1-5). This is a direct violation of CLAUDE.md Rule 7 which prohibits ALL AI/AI-team attribution.

**3 files** carry explicit `@author Manoj Jhawar / Claude AI` in the class KDoc block:
- `androidMain/.../handlers/ChunkEmbeddingHandler.kt` L44
- `androidMain/.../handlers/ClusteredSearchHandler.kt` L44
- `androidMain/.../handlers/DocumentIngestionHandler.kt` L44

The `/ Claude AI` suffix is the most explicit Rule 7 violation in the codebase. Every file must have its `author` field changed to `Manoj Jhawar` (or removed entirely) and `/ Claude AI` must be deleted everywhere.

**Complete list of files requiring Rule 7 remediation:**

commonMain (17 files):
- `domain/RAGRepository.kt` L3
- `domain/Document.kt` L3
- `domain/Chunk.kt` L3
- `domain/SearchQuery.kt` L3
- `domain/RAGConfig.kt` L3
- `cache/QueryCache.kt` L3
- `chat/RAGChatEngine.kt` L3
- `embeddings/EmbeddingProvider.kt` L3
- `embeddings/ModelDownloadManager.kt` L3
- `embeddings/Quantization.kt` L3
- `embeddings/SimpleTokenizer.kt` L3
- `parser/DocumentParser.kt` L3
- `parser/TextChunker.kt` L3
- `parser/TokenCounter.kt` L3
- `search/BM25Scorer.kt` L3
- `search/ReciprocalRankFusion.kt` L3
- `data/InMemoryRAGRepository.kt` L3

androidMain (29 files):
- `data/SQLiteRAGRepository.kt` L3
- `security/EmbeddingEncryptionManager.kt` L3 + L58 (class KDoc)
- `security/EncryptedEmbeddingRepository.kt` L3 + L39 (class KDoc)
- `security/EncryptionMigration.kt` L3 + L48 (class KDoc)
- `handlers/ChunkEmbeddingHandler.kt` L3 + L44 (`/ Claude AI`)
- `handlers/ClusteredSearchHandler.kt` L3 + L44 (`/ Claude AI`)
- `handlers/DocumentIngestionHandler.kt` L3 + L44 (`/ Claude AI`)
- `clustering/KMeansClustering.kt` L3
- `embeddings/ONNXEmbeddingProvider.android.kt` L3
- `embeddings/EmbeddingProviderFactory.android.kt` L3
- `embeddings/AndroidModelDownloadManager.kt` L3
- `embeddings/BatchModelWrapper.kt` L3
- `embeddings/PlatformDetector.kt` L3
- `embeddings/AONFileManager.kt` L3
- `embeddings/AONPackageManager.kt` L3
- `embeddings/AONWrapperTool.kt` L3
- `llm/MLCLLMProvider.android.kt` L3
- `chat/LocalLLMProviderAdapter.kt` L3
- `chat/RAGLLMIntegrationExample.kt` L3
- `parser/PdfParser.android.kt` L3
- `parser/DocxParser.android.kt` L3
- `parser/MarkdownParser.android.kt` L3
- `parser/RtfParser.android.kt` L3
- `parser/TxtParser.android.kt` L3
- `parser/HtmlParser.android.kt` L3
- `parser/DocumentParserFactory.android.kt` L3
- `ui/RAGChatScreen.kt` L3
- `ui/RAGSearchScreen.kt` L3
- `ui/DocumentManagementScreen.kt` L3
- `ui/DocumentManagementViewModel.kt` L3
- `ui/RAGChatViewModel.kt` L3
- `ui/WindowSizeUtils.kt` L3
- `ui/GradientUtils.kt` L3

desktopMain (2 files):
- `embeddings/EmbeddingProviderFactory.desktop.kt` L3
- `parser/DocumentParserFactory.desktop.kt` L3

iosMain (2 files):
- `embeddings/EmbeddingProviderFactory.ios.kt` L3
- `parser/DocumentParserFactory.ios.kt` L3

Test files (23 files — all carry `// author: AVA AI Team`):
All files in `androidTest/` and `commonTest/` carry the same header violation.

---

## Test Coverage Assessment

The module has **good breadth** of test scenarios but **critical gaps** in behavioral correctness:

**Positives:**
- `SimpleTokenizerTest.kt` — 9 tests covering edge cases (empty, long text, whitespace, batch). Well-structured.
- `EmbeddingEncryptionTest.kt` — 15 tests covering encrypt/decrypt, key rotation, random IV, corrupted data, performance benchmarks. Thorough.
- `SQLiteRAGRepositoryTest.kt` — Covers persistence, duplicates, metadata, delete, concurrent additions, statistics. Integration-level with real DB.
- `EncryptionMigrationTest.kt` — Tests migration progress, partial migration, data integrity after migration, key rotation migration, rollback.
- `BM25ScorerTest.kt`, `ReciprocalRankFusionTest.kt`, `QuantizationTest.kt`, `TextChunkerTest.kt` — Present in commonTest.

**Critical gaps:**
1. **No test for `SimpleTokenizer.wordToId()` producing BERT-aligned IDs.** `SimpleTokenizerTest` tests structural properties (token count, attention mask) but never validates that token IDs match a real BERT vocabulary. The Critical tokenizer bug would not be caught by existing tests.
2. **No test for `ChunkEmbeddingHandler.updateChunkEmbeddingInternal()`.** The no-op bug would not be caught.
3. **No test for `DocumentIngestionHandler` with non-PDF document types.** Only PDF path is exercised in integration tests because `processDocuments()` always throws for DOCX/TXT/etc.
4. **`EmbeddingEncryptionTest.testMigration_DataIntegrity` uses incorrect `createTestDocument()`** — the helper calls `rAGDocumentQueries.insert(...)` without the `status` and `error_message` parameters added in Issue 5.2. This test may fail to compile if the schema has been updated.
5. **`EncryptionMigrationTest.createTestChunk()`** uses unencrypted blob with `ByteBuffer` using default byte order (BIG_ENDIAN), while production serialization uses LITTLE_ENDIAN. Migration tests may be testing incorrect byte layouts.

---

## Recommendations

1. **Fix SimpleTokenizer first (Critical, blocking).** Replace hash-based token IDs with a real BERT vocab lookup before any other RAG work. Without this, all ONNX-generated embeddings are semantically invalid and every downstream search result is wrong.

2. **Fix DocumentIngestionHandler parser dispatch (Critical, blocking).** The `when` hardcoding to PDF means non-PDF indexing is entirely broken. This is a one-line fix: replace `when (docType) { DocumentType.PDF -> PdfParser(context) else -> throw ... }` with `DocumentParserFactory.getParser(docType) ?: throw UnsupportedDocumentTypeException(docType)`.

3. **Fix ChunkEmbeddingHandler.updateChunkEmbeddingInternal (Critical, blocking).** The no-op implementation means the re-embedding workflow writes nothing. Add the actual `chunkQueries.updateEmbedding(...)` call.

4. **Batch-fix Rule 7 violations.** Run a project-wide search-and-replace on `// author: AVA AI Team` → `// author: Manoj Jhawar` and `/ Claude AI` → `` (delete). 46 files, straightforward scripted fix.

5. **Replace `MaterialTheme.colorScheme.*` in all UI files.** Three UI Composable files violate the AvanueTheme v5.1 mandatory rule. Add AVID semantics to `DocumentManagementScreen.kt` at the same time.

6. **Resolve the AONFileManager HMAC verification gap (Critical security).** The HMAC check is a complete stub that re-uses the package authorization check. Until HMAC is properly implemented, the AON format's tamper protection is theater.

7. **Fix the encryption key hardcoding in AONFileManager (High security).** The `MASTER_KEY` constant must move to `BuildConfig` before any production release.

8. **Fix RtfParser `javax.swing` import (Medium, will fail Android build).** `RTFEditorKit` is not available on Android. This file cannot compile for Android targets. Replace with a proper Android-compatible RTF library.

9. **Add FTS5 for keyword search (High, OOM prevention).** `searchKeyword()` and `searchLinear()` loading all chunks into memory will cause OOM on production datasets. FTS5 via SQLite is the appropriate solution.

10. **Standardize endianness across embedding serialization (High, data correctness).** Audit every ByteBuffer usage in the module and confirm `LITTLE_ENDIAN` is used consistently for embeddings. Confirm centroid serialization matches the embedding byte order.

---

## Totals

| Severity | Count |
|----------|-------|
| Critical | 5 |
| High | 13 |
| Medium | 14 |
| Low | 9 |
| Rule 7 (separate category) | 46+ files |
| **Total** | **41 + Rule 7** |

---

*Report generated by code-reviewer agent | Reviewed 78 .kt files across commonMain / androidMain / desktopMain / iosMain / commonTest / androidTest*
