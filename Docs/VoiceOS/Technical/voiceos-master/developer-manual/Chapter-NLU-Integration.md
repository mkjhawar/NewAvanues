# Developer Manual: NLU Integration

**Version:** 1.0
**Date:** 2025-11-18
**Author:** VoiceOS/AVA Architecture Team

---

## Overview

Natural Language Understanding (NLU) provides intent classification and entity extraction for VoiceOS when deterministic command matching fails. The NLU module uses an on-device BERT-based model for privacy and low latency.

---

## Architecture

### Processing Pipeline

```
User Speech
    ↓
VoiceOS Command Matching (<50ms)
    ↓ No Match
NLU Intent Classification (50-200ms)
    ↓
Entity Extraction
    ↓
Intent Plugin Execution
```

### Model Details

| Component | AVA Filename | Size | Format |
|-----------|--------------|------|--------|
| **NLU Model** | `AVA-ONX-384-BASE-INT8.onnx` | 10-20 MB | ONNX INT8 |
| **Vocabulary** | `vocab.txt` | ~460 KB | Text |

**Model Architecture:** MobileBERT (INT8 quantized)
- Input: 384 tokens max
- Output: Intent classification + entities
- Latency: 50-200ms on-device

---

## Cloud Storage

### Download URLs

| Environment | Base URL |
|-------------|----------|
| **Release** | `https://www.augmentalis.com/avanuevoiceosava/ava/nlu/` |
| **Debug** | `http://fs.dilonline.in/avanue_files/ava/nlu/` |

### Server Structure

```
avanuevoiceosava/ava/nlu/
├── AVA-ONX-384-BASE-INT8.onnx   # NLU model (~15 MB)
├── vocab.txt                     # Vocabulary (~460 KB)
└── manifest.json                 # Version info
```

### Manifest Format

```json
{
    "version": "1.0.0",
    "models": {
        "default": {
            "model": "AVA-ONX-384-BASE-INT8.onnx",
            "vocab": "vocab.txt",
            "size": 15728640,
            "checksum": "sha256:abc123..."
        }
    },
    "minAppVersion": "1.0.0"
}
```

---

## Local Storage

### Device Paths

**Shared AVA folder (survives uninstall):**
```
/storage/emulated/0/.ava/nlu/
├── AVA-ONX-384-BASE-INT8.onnx
├── vocab.txt
└── manifest.json
```

**App-specific (cleared on uninstall):**
```
context.filesDir/models/nlu/
├── AVA-ONX-384-BASE-INT8.onnx
└── vocab.txt
```

### Discovery Priority

1. Check shared `.ava/nlu/` folder
2. Check app-specific `filesDir/models/nlu/`
3. Download from cloud if not found

---

## Download Manager

### NLUModelManager

```kotlin
class NLUModelManager(private val context: Context) {

    private val sharedPath = File(
        Environment.getExternalStorageDirectory(),
        ".ava/nlu"
    )

    private val appPath = File(context.filesDir, "models/nlu")

    private val modelFile = "AVA-ONX-384-BASE-INT8.onnx"
    private val vocabFile = "vocab.txt"

    /**
     * Check if NLU model is available locally
     */
    fun isModelAvailable(): Boolean {
        return (getModelFromShared() != null) || (getModelFromApp() != null)
    }

    /**
     * Get model path (shared takes priority)
     */
    fun getModelPath(): String? {
        return getModelFromShared()?.absolutePath
            ?: getModelFromApp()?.absolutePath
    }

    /**
     * Get vocabulary path
     */
    fun getVocabPath(): String? {
        val sharedVocab = File(sharedPath, vocabFile)
        val appVocab = File(appPath, vocabFile)

        return when {
            sharedVocab.exists() -> sharedVocab.absolutePath
            appVocab.exists() -> appVocab.absolutePath
            else -> null
        }
    }

    private fun getModelFromShared(): File? {
        val file = File(sharedPath, modelFile)
        return if (file.exists() && file.length() > MIN_MODEL_SIZE) file else null
    }

    private fun getModelFromApp(): File? {
        val file = File(appPath, modelFile)
        return if (file.exists() && file.length() > MIN_MODEL_SIZE) file else null
    }

    /**
     * Download NLU model if not available
     */
    suspend fun downloadIfNeeded(
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {

        if (isModelAvailable()) {
            return@withContext Result.Success(Unit)
        }

        try {
            // Ensure directory exists
            sharedPath.mkdirs()

            // Get URLs from Firebase Remote Config
            val configRepo = FirebaseRemoteConfigRepository(context)
            val baseUrl = if (BuildConfig.DEBUG) {
                "http://fs.dilonline.in/avanue_files/ava/nlu/"
            } else {
                "https://www.augmentalis.com/avanuevoiceosava/ava/nlu/"
            }

            // Download model (90% of progress)
            onProgress(0.1f)
            val modelResult = downloadFile(
                url = "$baseUrl$modelFile",
                destination = File(sharedPath, modelFile),
                onProgress = { onProgress(0.1f + it * 0.8f) }
            )
            if (modelResult is Result.Error) return@withContext modelResult

            // Download vocabulary (10% of progress)
            onProgress(0.9f)
            val vocabResult = downloadFile(
                url = "$baseUrl$vocabFile",
                destination = File(sharedPath, vocabFile),
                onProgress = { onProgress(0.9f + it * 0.1f) }
            )
            if (vocabResult is Result.Error) return@withContext vocabResult

            onProgress(1.0f)
            Result.Success(Unit)

        } catch (e: Exception) {
            Result.Error(
                message = "Failed to download NLU model: ${e.message}",
                exception = e
            )
        }
    }

    private suspend fun downloadFile(
        url: String,
        destination: File,
        onProgress: (Float) -> Unit
    ): Result<Unit> {
        // Implementation similar to ASR download
        // See Chapter-ASR-Engine-Architecture.md
    }

    companion object {
        const val MIN_MODEL_SIZE = 5 * 1024 * 1024L // 5 MB minimum
    }
}
```

---

## NLU Engine

### Initialization

```kotlin
class NLUEngine(private val context: Context) {

    private val modelManager = NLUModelManager(context)
    private var session: OrtSession? = null
    private var tokenizer: BertTokenizer? = null

    suspend fun initialize(): Result<Unit> {
        // Download model if needed
        if (!modelManager.isModelAvailable()) {
            val downloadResult = modelManager.downloadIfNeeded { progress ->
                Timber.d("NLU download: ${(progress * 100).toInt()}%")
            }
            if (downloadResult is Result.Error) {
                return downloadResult
            }
        }

        // Load ONNX model
        val modelPath = modelManager.getModelPath()
            ?: return Result.Error("Model not found")

        val env = OrtEnvironment.getEnvironment()
        session = env.createSession(modelPath)

        // Load tokenizer
        val vocabPath = modelManager.getVocabPath()
            ?: return Result.Error("Vocabulary not found")

        tokenizer = BertTokenizer(vocabPath)

        return Result.Success(Unit)
    }

    /**
     * Classify intent from text
     */
    suspend fun classifyIntent(text: String): IntentResult {
        val tokenized = tokenizer?.tokenize(text, maxLength = 128)
            ?: throw IllegalStateException("Tokenizer not initialized")

        val inputTensor = OnnxTensor.createTensor(
            OrtEnvironment.getEnvironment(),
            tokenized.inputIds
        )

        val results = session?.run(mapOf("input_ids" to inputTensor))

        // Parse results
        val intentLogits = results?.get("intent_logits")?.value as FloatArray
        val entityLogits = results?.get("entity_logits")?.value as FloatArray

        return IntentResult(
            intent = decodeIntent(intentLogits),
            entities = decodeEntities(entityLogits, tokenized.tokens),
            confidence = softmax(intentLogits).max()
        )
    }

    fun release() {
        session?.close()
        session = null
    }
}
```

---

## Intent Plugin Integration

### IntentPlugin Interface

```kotlin
interface IntentPlugin {
    val intents: List<IntentDefinition>
    suspend fun execute(intent: ParsedIntent): IntentResult
}

data class IntentDefinition(
    val name: String,
    val description: String,
    val requiredEntities: List<String>,
    val optionalEntities: List<String> = emptyList()
)

data class ParsedIntent(
    val name: String,
    val entities: Map<String, String>,
    val confidence: Float,
    val rawText: String
)
```

### Example: Calendar Plugin

```kotlin
class CalendarIntentPlugin : IntentPlugin {

    override val intents = listOf(
        IntentDefinition(
            name = "ADD_CALENDAR_EVENT",
            description = "Add event to calendar",
            requiredEntities = listOf("event_title"),
            optionalEntities = listOf("date", "time", "location")
        ),
        IntentDefinition(
            name = "CHECK_CALENDAR",
            description = "Check calendar for date",
            requiredEntities = listOf("date")
        )
    )

    override suspend fun execute(intent: ParsedIntent): IntentResult {
        return when (intent.name) {
            "ADD_CALENDAR_EVENT" -> addEvent(intent)
            "CHECK_CALENDAR" -> checkCalendar(intent)
            else -> IntentResult.NotHandled
        }
    }

    private suspend fun addEvent(intent: ParsedIntent): IntentResult {
        val title = intent.entities["event_title"] ?: return IntentResult.Error("No title")
        val date = intent.entities["date"]
        val time = intent.entities["time"]

        // Add to calendar...
        return IntentResult.Success("Added '$title' to calendar")
    }
}
```

---

## Configuration

### Firebase Remote Config

```kotlin
// Remote config parameters for NLU
val nluModelUrl = remoteConfig.getString("nlu_model_url")
val nluVocabUrl = remoteConfig.getString("nlu_vocab_url")
val nluModelVersion = remoteConfig.getString("nlu_model_version")
```

### Local Config

In `.ava/ASR/config.json`:

```json
{
    "nlu": {
        "baseUrl": {
            "release": "https://www.augmentalis.com/avanuevoiceosava/ava/nlu/",
            "debug": "http://fs.dilonline.in/avanue_files/ava/nlu/"
        },
        "models": {
            "default": {
                "model": "AVA-ONX-384-BASE-INT8.onnx",
                "vocab": "vocab.txt"
            }
        }
    }
}
```

---

## Error Handling

### Common Errors

```kotlin
sealed class NLUError : Exception() {
    object ModelNotFound : NLUError()
    object VocabNotFound : NLUError()
    object DownloadFailed : NLUError()
    object InferenceFailed : NLUError()

    data class LowConfidence(
        val confidence: Float
    ) : NLUError()
}
```

### Fallback Strategy

When NLU fails or has low confidence:

```kotlin
suspend fun processWithFallback(text: String): Result<String> {
    // Try NLU
    val intentResult = nluEngine.classifyIntent(text)

    if (intentResult.confidence > CONFIDENCE_THRESHOLD) {
        // Execute intent plugin
        return executeIntent(intentResult)
    }

    // Low confidence - suggest similar commands
    val suggestions = commandMatcher.findSimilar(text)
    return Result.Suggestion(
        message = "Did you mean: ${suggestions.joinToString()}?",
        suggestions = suggestions
    )
}

const val CONFIDENCE_THRESHOLD = 0.7f
```

---

## Testing

### Unit Tests

```kotlin
@Test
fun `NLU classifies calendar intent correctly`() = runTest {
    val engine = NLUEngine(context)
    engine.initialize()

    val result = engine.classifyIntent("add meeting tomorrow at 3pm")

    assertEquals("ADD_CALENDAR_EVENT", result.intent)
    assertEquals("meeting", result.entities["event_title"])
    assertEquals("tomorrow", result.entities["date"])
    assertEquals("3pm", result.entities["time"])
    assertTrue(result.confidence > 0.8f)
}

@Test
fun `NLU downloads model when not available`() = runTest {
    // Clear local models
    modelManager.clearModels()

    // Should trigger download
    val result = modelManager.downloadIfNeeded { }

    assertTrue(result is Result.Success)
    assertTrue(modelManager.isModelAvailable())
}
```

---

## Performance

### Benchmarks

| Operation | Latency | Notes |
|-----------|---------|-------|
| Model load | 200-500ms | One-time at startup |
| Tokenization | 5-10ms | Per input |
| Inference | 50-150ms | Per classification |
| Total | 60-165ms | Per intent |

### Optimization Tips

1. **Preload model** - Initialize at app startup
2. **Cache tokenization** - For repeated queries
3. **Batch processing** - Multiple intents together
4. **Quantization** - INT8 reduces size 75%

---

## Related Documents

- ADR-VoiceOS-AVA-Architecture-Integration-251118.md
- Chapter-ASR-Engine-Architecture.md
- Chapter-LLM-Integration.md
- AVA-MODEL-NAMING-REGISTRY.md

---

**Document Status:** Draft
**Review Status:** Pending
**Next Steps:** Technical review, API stabilization
