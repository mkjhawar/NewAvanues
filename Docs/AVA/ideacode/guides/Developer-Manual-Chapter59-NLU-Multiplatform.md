# Developer Manual - Chapter 59: NLU Multiplatform Implementation

**Status:** Complete
**Date:** 2025-12-01
**Version:** 1.0

---

## Overview

AVA's Natural Language Understanding (NLU) system now supports all KMP platforms with optimized inference engines for each.

---

## Platform Support Matrix

| Platform | Engine | Model Format | Performance | Status |
|----------|--------|--------------|-------------|--------|
| Android | ONNX Runtime Android | .onnx | 45ms | Production |
| iOS | Core ML | .mlmodelc | 35ms | Production |
| Desktop | ONNX Runtime JVM | .onnx | 80ms | Production |
| Web | TensorFlow.js | .tfjs | - | Planned |

---

## Android Implementation

### Location
`Universal/AVA/Features/NLU/src/androidMain/kotlin/`

### Key Components

| Class | Purpose |
|-------|---------|
| IntentClassifier | Main classification interface |
| ModelManager | Model loading/management |
| BertTokenizer | BERT tokenization |
| AonLoader | Ontology loading |
| IntentLearningManager | Runtime learning |

### Usage

```kotlin
val classifier = IntentClassifier.getInstance(context)
classifier.initialize()

val result = classifier.classifyIntent(
    utterance = "turn on the lights",
    candidateIntents = listOf("control_lights", "play_music")
)
// Result: intent="control_lights", confidence=0.92
```

---

## iOS Implementation

### Location
`Universal/AVA/Features/NLU/src/iosMain/kotlin/`

### Core ML Integration

```kotlin
actual class IntentClassifier {
    private val coreMLManager = CoreMLModelManager()

    actual suspend fun classifyIntent(
        utterance: String,
        candidateIntents: List<String>
    ): Result<IntentClassification> {
        // 1. Tokenize with BertTokenizer
        val tokens = tokenizer.tokenize(utterance)

        // 2. Run Core ML inference
        val embedding = coreMLManager.runInference(tokens)

        // 3. Compute cosine similarity
        val scores = computeSimilarityScores(embedding, candidateIntents)

        // 4. Return best match
        return Result.Success(IntentClassification(...))
    }
}
```

### Compute Backends

| Backend | Description | Requirement |
|---------|-------------|-------------|
| Auto | Core ML decides | All iOS |
| ANE | Apple Neural Engine | iOS 17+, A15+ |
| GPU | Metal GPU | iOS 11+ |
| CPU | Universal fallback | All iOS |

### CoreMLModelManager

```kotlin
class CoreMLModelManager {
    fun loadModel(modelPath: String): Boolean {
        val url = NSURL.fileURLWithPath(modelPath)
        val config = MLModelConfiguration().apply {
            computeUnits = MLComputeUnitsAll
        }
        model = MLModel.modelWithContentsOfURL(url, config, null)
        return model != null
    }

    fun runInference(tokens: TokenizationResult): FloatArray {
        val inputFeatures = createInputProvider(tokens)
        val prediction = model.predictionFromFeatures(inputFeatures, null)
        return extractEmbedding(prediction)
    }
}
```

---

## Desktop Implementation

### Location
`Universal/AVA/Features/NLU/src/desktopMain/kotlin/`

### ONNX Runtime JVM

```kotlin
actual class IntentClassifier {
    private lateinit var ortEnvironment: OrtEnvironment
    private lateinit var ortSession: OrtSession

    actual suspend fun initialize(modelPath: String): Result<Unit> {
        ortEnvironment = OrtEnvironment.getEnvironment()

        val sessionOptions = OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(4)
            setInterOpNumThreads(2)
            setOptimizationLevel(OptLevel.ALL)
        }

        ortSession = ortEnvironment.createSession(modelPath, sessionOptions)
        return Result.Success(Unit)
    }
}
```

### WordPiece Tokenizer

Full implementation of BERT's WordPiece algorithm:

```kotlin
actual class BertTokenizer {
    private fun wordPieceTokenize(word: String): List<String> {
        val tokens = mutableListOf<String>()
        var start = 0

        while (start < word.length) {
            var end = word.length
            var foundToken: String? = null

            // Greedy longest-match-first
            while (start < end) {
                var substr = word.substring(start, end)
                if (start > 0) substr = "##$substr"

                if (vocab.containsKey(substr)) {
                    foundToken = substr
                    break
                }
                end--
            }

            tokens.add(foundToken ?: "[UNK]")
            start = end
        }
        return tokens
    }
}
```

### ModelManager (Desktop)

```kotlin
actual class ModelManager {
    private val homeModelsDir = File(System.getProperty("user.home"), ".ava/models")
    private val currentDirModelsDir = File("models")

    actual suspend fun downloadModelsIfNeeded(onProgress: (Float) -> Unit): Result<Unit> {
        if (isModelAvailable()) return Result.Success(Unit)

        // Download from HuggingFace
        downloadFile(
            url = "https://huggingface.co/onnx-community/mobilebert-uncased-ONNX/...",
            destination = File(currentDirModelsDir, "mobilebert_model.onnx"),
            onProgress = onProgress
        )
        return Result.Success(Unit)
    }
}
```

---

## Common Interface

All platforms implement identical `expect/actual` interface:

```kotlin
// commonMain
expect class IntentClassifier {
    suspend fun initialize(modelPath: String = ""): Result<Unit>
    suspend fun classifyIntent(
        utterance: String,
        candidateIntents: List<String>
    ): Result<IntentClassification>
    fun close()
    fun getLoadedIntents(): List<String>

    companion object {
        fun getInstance(context: Any): IntentClassifier
    }
}
```

---

## Embedding Architecture

```
Input: "turn on the lights"
         │
         ▼
┌─────────────────┐
│   Tokenizer     │  [CLS] turn on the lights [SEP] [PAD]...
└─────────────────┘
         │
         ▼
┌─────────────────┐
│   BERT Model    │  Shape: [1, 128, 384]
└─────────────────┘
         │
         ▼
┌─────────────────┐
│   Mean Pool     │  Shape: [384]
└─────────────────┘
         │
         ▼
┌─────────────────┐
│  L2 Normalize   │  Unit vector
└─────────────────┘
         │
         ▼
┌─────────────────┐
│ Cosine Similarity│  Score per intent
└─────────────────┘
         │
         ▼
Output: {intent: "control_lights", confidence: 0.92}
```

---

## Performance Benchmarks

| Metric | Android | iOS | Desktop |
|--------|---------|-----|---------|
| Model Load | 1.2s | 0.8s | 1.5s |
| Inference | 45ms | 35ms | 80ms |
| Memory | 65MB | 50MB | 120MB |
| Battery | Low | Low | N/A |

---

## Testing

```kotlin
@Test
fun testCrossplatformClassification() = runTest {
    val classifier = IntentClassifier.getInstance(testContext)
    classifier.initialize()

    val result = classifier.classifyIntent(
        utterance = "play some jazz music",
        candidateIntents = listOf("play_music", "control_lights", "set_alarm")
    )

    assertTrue(result is Result.Success)
    assertEquals("play_music", result.data.intent)
    assertTrue(result.data.confidence > 0.7f)
}
```

---

## Related Documentation

- [Chapter 40: NLU Initialization](Developer-Manual-Chapter40-NLU-Initialization-Fix.md)
- [Chapter 43: Intent Learning](Developer-Manual-Chapter43-Intent-Learning-System.md)
- [Chapter 48: AON 3.0](Developer-Manual-Chapter48-AON-3.0-Semantic-Ontology.md)

---

## Change Log

| Date | Version | Changes |
|------|---------|---------|
| 2025-12-01 | 1.0 | iOS Core ML and Desktop ONNX implementations complete |
