# Multilingual RAG Setup Guide

**Project:** ava
**Version:** 1.0
**Last Updated:** 2025-11-06

Complete guide to setting up AVA's RAG (Retrieval-Augmented Generation) system for multilingual document search and chat.

---

## Table of Contents

1. [Overview](#overview)
2. [Language Support](#language-support)
3. [Model Selection](#model-selection)
4. [Setup Instructions](#setup-instructions)
5. [Code Configuration](#code-configuration)
6. [Testing](#testing)
7. [Performance Considerations](#performance-considerations)
8. [Troubleshooting](#troubleshooting)

---

## Overview

AVA's RAG system supports multilingual document search and retrieval. You can:

- **Search documents in 50+ languages** using multilingual embedding models
- **Mix languages** - documents in different languages in the same collection
- **Cross-lingual search** - query in one language, find results in another
- **Use LLMs** that support multilingual responses (Gemma, Mistral, etc.)

---

## Language Support

### Fully Supported Languages (50+)

The multilingual models support all of these languages with high quality:

**European:**
- English, French, German, Spanish, Italian, Portuguese
- Dutch, Polish, Russian, Turkish, Finnish, Swedish
- Danish, Norwegian, Czech, Romanian, Greek, Bulgarian
- Croatian, Slovak, Slovenian, Ukrainian

**Asian:**
- Chinese (Simplified & Traditional)
- Japanese
- Korean
- Arabic
- Hebrew
- Thai
- Vietnamese
- Indonesian

**Other:**
- Hindi
- Persian (Farsi)
- Urdu
- Bengali
- And 20+ more...

---

## Model Selection

### Recommended: Multilingual MiniLM-L12 (470 MB)

**Best for most use cases**

- **Size:** 470 MB
- **Dimensions:** 384
- **Languages:** 50+
- **Speed:** Fast
- **Quality:** Very good

**Model ID:** `AVA-ONX-384-MULTI`

**When to use:**
- Mixed-language document collections
- Cross-lingual search requirements
- General multilingual applications
- Limited device storage

---

### High-Quality: Multilingual MPNet (1.1 GB)

**Best quality, larger size**

- **Size:** 1.1 GB
- **Dimensions:** 768
- **Languages:** 50+
- **Speed:** Moderate
- **Quality:** Excellent

**Model ID:** `AVA-ONX-768-MULTI`

**When to use:**
- Research applications
- High-accuracy requirements
- Devices with ample storage
- Quality over speed

---

### Compact: Distiluse Multilingual (540 MB)

**For 15 most common languages only**

- **Size:** 540 MB
- **Dimensions:** 512
- **Languages:** 15 (major languages only)
- **Speed:** Fast
- **Quality:** Good

**Model ID:** `AVA-ONX-512-MULTI`

**When to use:**
- Limited to major languages
- Moderate device resources
- Balance of size and quality

---

### Language-Specific Models

#### Chinese Only: DMetaSoul Chinese (400 MB)

**Optimized for Chinese text**

- **Size:** 400 MB
- **Dimensions:** 384
- **Languages:** Chinese only
- **Quality:** Excellent for Chinese

**Model ID:** `AVA-ONX-384-ZH`

**When to use:**
- Chinese-only document collections
- Maximum Chinese language quality

---

#### Japanese Only: Sentence-BERT Japanese (450 MB)

**Optimized for Japanese text**

- **Size:** 450 MB
- **Dimensions:** 768
- **Languages:** Japanese only
- **Quality:** Excellent for Japanese

**Model ID:** `AVA-ONX-768-JA`

**When to use:**
- Japanese-only document collections
- Maximum Japanese language quality

---

## Setup Instructions

### Step 1: Download Multilingual Embedding Model

**Recommended: Multilingual MiniLM-L12**

```bash
# Download the model
curl -L https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/onnx/model.onnx \
  -o AVA-ONX-384-MULTI.onnx

# Verify download
ls -lh AVA-ONX-384-MULTI.onnx
# Should be: 470 MB
```

**Alternative downloads:** See `docs/MODEL-DOWNLOAD-SOURCES.md`

---

### Step 2: Push to Android Device

```bash
# Create models directory
adb shell mkdir -p /sdcard/Android/data/com.augmentalis.ava/files/models/

# Push embedding model
adb push AVA-ONX-384-MULTI.onnx /sdcard/Android/data/com.augmentalis.ava/files/models/

# Verify
adb shell ls -lh /sdcard/Android/data/com.augmentalis.ava/files/models/
# Should show: AVA-ONX-384-MULTI.onnx  470M
```

---

### Step 3: Download LLM Model (Optional but Recommended)

LLMs like Gemma-2B already support multilingual text generation.

```bash
# Use existing Gemma-2B setup
# See docs/DEVICE-MODEL-SETUP-COMPLETE.md

# No additional LLM needed for multilingual support
# Gemma-2B already handles 50+ languages
```

---

## Code Configuration

### Update ONNXEmbeddingProvider

**For 384-dimension models (MiniLM-L12, Chinese):**

No code changes needed! Just specify the model ID when creating the provider:

```kotlin
// In your RAG initialization code
val embeddingProvider = ONNXEmbeddingProvider(
    context = context,
    modelId = "AVA-ONX-384-MULTI"  // Use multilingual model
)

val ragEngine = RAGEngine(
    embeddingProvider = embeddingProvider,
    vectorStore = vectorStore
)
```

---

### For 768-dimension models (MPNet, Japanese)

**Update the dimension parameter:**

```kotlin
// Create custom provider with different dimension
class CustomONNXProvider(
    context: Context,
    modelId: String = "AVA-ONX-768-MULTI"
) : ONNXEmbeddingProvider(context, modelId = modelId) {

    override val dimension = 768  // Override for 768-dim models
}

// Use it
val embeddingProvider = CustomONNXProvider(
    context = context,
    modelId = "AVA-ONX-768-MULTI"
)
```

---

### For 512-dimension models (Distiluse)

```kotlin
class CustomONNXProvider(
    context: Context,
    modelId: String = "AVA-ONX-512-MULTI"
) : ONNXEmbeddingProvider(context, modelId = modelId) {

    override val dimension = 512  // Override for 512-dim models
}

// Use it
val embeddingProvider = CustomONNXProvider(
    context = context,
    modelId = "AVA-ONX-512-MULTI"
)
```

---

### Runtime Model Switching

```kotlin
// Switch models at runtime
class AdaptiveEmbeddingProvider(
    private val context: Context
) {
    fun createProvider(language: String): ONNXEmbeddingProvider {
        val modelId = when (language) {
            "zh", "zh-CN", "zh-TW" -> "AVA-ONX-384-ZH"  // Chinese
            "ja" -> "AVA-ONX-768-JA"                     // Japanese
            else -> "AVA-ONX-384-MULTI"                  // Multilingual default
        }

        return ONNXEmbeddingProvider(
            context = context,
            modelId = modelId
        )
    }
}
```

---

## Testing

### Test 1: Multilingual Document Ingestion

```kotlin
// Add documents in different languages
val documents = listOf(
    Document(
        id = "doc1",
        content = "This is an English document about AI.",
        metadata = mapOf("language" to "en")
    ),
    Document(
        id = "doc2",
        content = "这是一个关于人工智能的中文文档。",
        metadata = mapOf("language" to "zh")
    ),
    Document(
        id = "doc3",
        content = "これは人工知能に関する日本語の文書です。",
        metadata = mapOf("language" to "ja")
    ),
    Document(
        id = "doc4",
        content = "Este es un documento en español sobre IA.",
        metadata = mapOf("language" to "es")
    )
)

// Ingest all documents
documents.forEach { doc ->
    ragEngine.addDocument(doc)
}

Timber.i("Ingested ${documents.size} multilingual documents")
```

---

### Test 2: Cross-Lingual Search

```kotlin
// Query in English, find results in all languages
val query = "artificial intelligence"

val results = ragEngine.search(
    query = query,
    topK = 5
)

results.forEach { result ->
    val language = result.metadata["language"]
    Timber.i("Found in $language: ${result.content.take(50)}...")
}

// Should find:
// - English: "This is an English document..."
// - Chinese: "这是一个关于人工智能..."
// - Japanese: "これは人工知能に関する..."
// - Spanish: "Este es un documento..."
```

---

### Test 3: Native Language Query

```kotlin
// Query in Chinese
val chineseQuery = "人工智能"

val results = ragEngine.search(
    query = chineseQuery,
    topK = 3
)

// Should rank Chinese document highest, but also find related content in other languages
```

---

### Test 4: RAG Chat with Multilingual Context

```kotlin
// Initialize RAG chat with multilingual model
val ragChat = RAGChatEngine(
    ragEngine = ragEngine,
    llmProvider = mlcLLMProvider
)

// Ask question in English about multilingual documents
val response = ragChat.chat(
    message = "What do these documents say about AI?",
    conversationId = "test-multilingual"
)

// LLM should synthesize information from documents in all languages
Timber.i("Response: $response")
```

---

## Performance Considerations

### Model Size vs Quality

| Model | Size | Quality | Speed | Languages |
|-------|------|---------|-------|-----------|
| AVA-ONX-384-MULTI | 470 MB | Very Good | Fast | 50+ |
| AVA-ONX-768-MULTI | 1.1 GB | Excellent | Moderate | 50+ |
| AVA-ONX-512-MULTI | 540 MB | Good | Fast | 15 |
| AVA-ONX-384-ZH | 400 MB | Excellent (ZH) | Fast | Chinese only |
| AVA-ONX-768-JA | 450 MB | Excellent (JA) | Moderate | Japanese only |

---

### Device Requirements

**Minimum (Multilingual MiniLM-L12):**
- **RAM:** 2 GB available
- **Storage:** 1 GB free (embedding model + overhead)
- **Recommendation:** Mid-range Android device

**Recommended (Multilingual MPNet):**
- **RAM:** 4 GB available
- **Storage:** 2 GB free
- **Recommendation:** High-end Android device

---

### Search Performance

**384-dimension models (MiniLM, Chinese):**
- ~100-200ms per search query
- Suitable for real-time applications

**768-dimension models (MPNet, Japanese):**
- ~200-400ms per search query
- Still acceptable for interactive use

**Optimization tips:**
- Use smaller `topK` values (3-5 instead of 10)
- Index fewer documents per collection
- Consider language-specific models if only 1-2 languages needed

---

## Troubleshooting

### Issue 1: Model not found

**Error:** "Embedding model not found: AVA-ONX-384-MULTI"

**Fix:**
```bash
# Check filename is exact
adb shell ls -l /sdcard/Android/data/com.augmentalis.ava/files/models/

# Must be: AVA-ONX-384-MULTI.onnx (case-sensitive)
# NOT: ava-onx-384-multi.onnx
```

---

### Issue 2: Wrong dimension error

**Error:** "Expected 384 dimensions, got 768"

**Fix:**
If using 768-dim model (MPNet, Japanese), override dimension:

```kotlin
class CustomProvider(context: Context) : ONNXEmbeddingProvider(
    context = context,
    modelId = "AVA-ONX-768-MULTI"
) {
    override val dimension = 768
}
```

---

### Issue 3: Poor search results for specific language

**Problem:** English queries work well, but Chinese queries don't

**Possible causes:**
1. Using English-only model instead of multilingual
2. Not enough documents in target language

**Fix:**
```kotlin
// Verify you're using multilingual model
val modelId = embeddingProvider.modelId
Timber.i("Current model: $modelId")  // Should be AVA-ONX-384-MULTI or similar

// Check document language distribution
val languages = documents.groupBy { it.metadata["language"] }
    .mapValues { it.value.size }
Timber.i("Documents by language: $languages")
```

---

### Issue 4: Cross-lingual search not working

**Problem:** Queries only find documents in same language

**Cause:** Using language-specific model instead of multilingual

**Fix:**
```kotlin
// Use multilingual model, not language-specific
val embeddingProvider = ONNXEmbeddingProvider(
    context = context,
    modelId = "AVA-ONX-384-MULTI"  // ✓ Multilingual
    // NOT: "AVA-ONX-384-ZH"        // ✗ Chinese-only
)
```

---

### Issue 5: Out of memory

**Error:** "OutOfMemoryError" when loading model

**Fix:**
Use smaller model or free up memory:

```kotlin
// Option 1: Use 384-dim instead of 768-dim
val modelId = "AVA-ONX-384-MULTI"  // Instead of AVA-ONX-768-MULTI

// Option 2: Free memory before loading
System.gc()
Thread.sleep(100)

val embeddingProvider = ONNXEmbeddingProvider(
    context = context,
    modelId = modelId
)
```

---

## Language-Specific Tips

### Chinese (中文)

**Best model:** `AVA-ONX-384-ZH` (Chinese-only) or `AVA-ONX-384-MULTI` (multilingual)

**Tips:**
- Handles both Simplified and Traditional Chinese
- Works well with mixed Chinese-English text
- Consider text segmentation for better results

---

### Japanese (日本語)

**Best model:** `AVA-ONX-768-JA` (Japanese-only) or `AVA-ONX-384-MULTI` (multilingual)

**Tips:**
- Handles hiragana, katakana, and kanji
- Works with mixed Japanese-English text
- Consider morphological analysis for better results

---

### Arabic (العربية)

**Best model:** `AVA-ONX-384-MULTI` (multilingual)

**Tips:**
- Handles right-to-left text automatically
- Works with diacritics
- Modern Standard Arabic and dialects supported

---

### Mixed Languages

**Best model:** `AVA-ONX-384-MULTI` or `AVA-ONX-768-MULTI`

**Example:**
```kotlin
val mixedDocument = Document(
    id = "mixed",
    content = """
        This document contains multiple languages.
        这包含中文文本。
        これは日本語を含んでいます。
        Ceci contient du français.
    """.trimIndent(),
    metadata = mapOf("language" to "mixed")
)

ragEngine.addDocument(mixedDocument)

// Search works in any language
val results = ragEngine.search("multiple languages")  // English
val results2 = ragEngine.search("多语言")               // Chinese
val results3 = ragEngine.search("複数の言語")           // Japanese
```

---

## Example: Complete Multilingual Setup

```kotlin
class MultilingualRAGSetup(private val context: Context) {

    private lateinit var ragEngine: RAGEngine
    private lateinit var ragChat: RAGChatEngine

    suspend fun initialize() {
        // 1. Create multilingual embedding provider
        val embeddingProvider = ONNXEmbeddingProvider(
            context = context,
            modelId = "AVA-ONX-384-MULTI"  // Supports 50+ languages
        )

        // 2. Initialize embedding provider
        embeddingProvider.initialize().getOrThrow()
        Timber.i("Multilingual embedding model loaded")

        // 3. Create vector store
        val vectorStore = InMemoryVectorStore()

        // 4. Create RAG engine
        ragEngine = RAGEngine(
            embeddingProvider = embeddingProvider,
            vectorStore = vectorStore,
            chunkSize = 500
        )

        // 5. Create LLM provider (supports multilingual too)
        val llmProvider = MLCLLMProvider(
            context = context,
            modelId = "AVA-GEM-2B-Q4"  // Gemma already multilingual
        )
        llmProvider.initialize().getOrThrow()

        // 6. Create RAG chat engine
        ragChat = RAGChatEngine(
            ragEngine = ragEngine,
            llmProvider = llmProvider
        )

        Timber.i("Multilingual RAG system ready!")
    }

    suspend fun testMultilingualSearch() {
        // Add documents in different languages
        val docs = listOf(
            "Artificial intelligence is transforming the world." to "en",
            "L'intelligence artificielle transforme le monde." to "fr",
            "人工智能正在改变世界。" to "zh",
            "人工知能が世界を変えています。" to "ja",
            "La inteligencia artificial está transformando el mundo." to "es"
        )

        docs.forEach { (content, lang) ->
            ragEngine.addDocument(Document(
                id = UUID.randomUUID().toString(),
                content = content,
                metadata = mapOf("language" to lang)
            ))
        }

        // Search in English - should find all relevant docs
        val results = ragEngine.search("AI transformation", topK = 5)

        Timber.i("Found ${results.size} results across languages:")
        results.forEach { result ->
            val lang = result.metadata["language"]
            Timber.i("  [$lang] ${result.content}")
        }
    }
}
```

---

## Next Steps

After setting up multilingual RAG:

1. **Test with your languages** - Add documents in your target languages
2. **Evaluate quality** - Compare different models for your use case
3. **Optimize performance** - Adjust model selection based on device constraints
4. **Monitor usage** - Track search quality across languages

---

## Additional Resources

### Model Documentation
- **Multilingual MiniLM:** https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2
- **Multilingual MPNet:** https://huggingface.co/sentence-transformers/paraphrase-multilingual-mpnet-base-v2
- **Chinese SBERT:** https://huggingface.co/DMetaSoul/sbert-chinese-general-v2
- **Japanese SBERT:** https://huggingface.co/sonoisa/sentence-bert-base-ja-mean-tokens-v2

### Related Guides
- `docs/MODEL-DOWNLOAD-SOURCES.md` - Download all models
- `docs/DEVICE-MODEL-SETUP-COMPLETE.md` - Device setup
- `docs/AVA-MODEL-NAMING-REGISTRY.md` - Model naming reference

### Community
- **Sentence Transformers:** https://www.sbert.net/
- **HuggingFace Forums:** https://discuss.huggingface.co/

---

**Document Version:** 1.0
**Last Updated:** 2025-11-06
**Maintained by:** AVA Development Team
