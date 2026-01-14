# AVA AI File Locations and Setup

**Version:** 1.0
**Date:** 2025-11-28
**Purpose:** Exact file locations in repo and download instructions

## Overview

This document clarifies which files exist in the repo and which need to be downloaded for testing.

---

## Files in Repository (Copy from Here)

### 1. NLU Models (Bundled in APK)

#### MobileBERT ONNX Model
```
Repo Location:
/Volumes/M-Drive/Coding/AVA/app/src/main/assets/models/mobilebert_int8.onnx
Size: 25 MB

Copy to Device:
/sdcard/ava-ai-models/embeddings/mobilebert-uncased-int8.onnx

Command:
adb push app/src/main/assets/models/mobilebert_int8.onnx /sdcard/ava-ai-models/embeddings/mobilebert-uncased-int8.onnx
```

#### Vocabulary File
```
Repo Location:
/Volumes/M-Drive/Coding/AVA/app/src/main/assets/models/vocab.txt
OR
/Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/assets/models/vocab.txt
Size: 226 KB

Copy to Device:
/sdcard/ava-ai-models/embeddings/vocab.txt

Command:
adb push apps/ava-app-android/src/main/assets/models/vocab.txt /sdcard/ava-ai-models/embeddings/vocab.txt
```

### 2. RAG Models (AON Format)

#### AVA-384-Base-INT8.AON
```
Repo Location:
/Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/assets/models/AVA-384-Base-INT8.AON
Size: 22 MB

Copy to Device:
/sdcard/ava-ai-models/rag/AVA-384-Base-INT8.AON

Command:
adb push apps/ava-app-android/src/main/assets/models/AVA-384-Base-INT8.AON /sdcard/ava-ai-models/rag/AVA-384-Base-INT8.AON
```

#### Vocabulary File (RAG)
```
Repo Location:
/Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/assets/models/vocab.txt
Size: 226 KB

Copy to Device:
/sdcard/ava-ai-models/rag/vocab.txt

Command:
adb push apps/ava-app-android/src/main/assets/models/vocab.txt /sdcard/ava-ai-models/rag/vocab.txt
```

### 3. AVA Intent Files

#### Information Intents
```
Repo Location:
/Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/assets/ava-examples/en-US/information.ava
Size: 1.1 KB

Copy to Device:
/sdcard/ava-ai-models/intents/information.ava

Command:
adb push apps/ava-app-android/src/main/assets/ava-examples/en-US/information.ava /sdcard/ava-ai-models/intents/
```

#### Productivity Intents
```
Repo Location:
/Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/assets/ava-examples/en-US/productivity.ava
Size: 1.0 KB

Copy to Device:
/sdcard/ava-ai-models/intents/productivity.ava

Command:
adb push apps/ava-app-android/src/main/assets/ava-examples/en-US/productivity.ava /sdcard/ava-ai-models/intents/
```

#### System Control Intents
```
Repo Location:
/Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/assets/ava-examples/en-US/system-control.ava
Size: 2.5 KB

Copy to Device:
/sdcard/ava-ai-models/intents/system-control.ava

Command:
adb push apps/ava-app-android/src/main/assets/ava-examples/en-US/system-control.ava /sdcard/ava-ai-models/intents/
```

#### Navigation Intents
```
Repo Location:
/Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/assets/ava-examples/en-US/navigation.ava
Size: 1.6 KB

Copy to Device:
/sdcard/ava-ai-models/intents/navigation.ava

Command:
adb push apps/ava-app-android/src/main/assets/ava-examples/en-US/navigation.ava /sdcard/ava-ai-models/intents/
```

#### Media Control Intents
```
Repo Location:
/Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/assets/ava-examples/en-US/media-control.ava
Size: 1.7 KB

Copy to Device:
/sdcard/ava-ai-models/intents/media-control.ava

Command:
adb push apps/ava-app-android/src/main/assets/ava-examples/en-US/media-control.ava /sdcard/ava-ai-models/intents/
```

#### VoiceOS Commands (Large)
```
Repo Location:
/Volumes/M-Drive/Coding/AVA/apps/ava-app-android/src/main/assets/ava-examples/en-US/voiceos-commands.ava
Size: 32 KB

Copy to Device:
/sdcard/ava-ai-models/intents/voiceos-commands.ava

Command:
adb push apps/ava-app-android/src/main/assets/ava-examples/en-US/voiceos-commands.ava /sdcard/ava-ai-models/intents/
```

### 4. Additional ONNX Models in Repo

These exist but may need processing:

#### all-MiniLM-L6-v2 (Raw ONNX)
```
Repo Location:
/Volumes/M-Drive/Coding/AVA/ai-models/all-MiniLM-L6-v2.onnx

Note: This needs to be wrapped in AON format before use
See: Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/embeddings/AONFileManager.kt
```

#### Cached Hugging Face Models
```
Repo Location:
/Volumes/M-Drive/Coding/AVA/models/cache/models--onnx-community--mobilebert-uncased-ONNX/snapshots/*/onnx/model_int8.onnx

Note: These are Hugging Face cache downloads
```

---

## Files NOT in Repository (Need to Download)

### 1. mALBERT Multilingual Model

**File:** `AVA-768-Multi-INT8.AON`
**Size:** ~90 MB (quantized for ARM64, wrapped in AON format)
**Not in repo because:** Too large for git

**Setup:**

Step 1: Download raw ONNX from Hugging Face
```bash
wget https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/onnx/model_qint8_arm64.onnx
```

Step 2: Wrap in AON format using AONFileManager
```kotlin
import com.augmentalis.ava.features.rag.embeddings.AONFileManager

val onnxFile = File("model_qint8_arm64.onnx")
val aonFile = File("AVA-768-Multi-INT8.AON")

AONFileManager.wrapONNX(
    onnxFile = onnxFile,
    outputFile = aonFile,
    modelId = "AVA-768-Multi-INT8",
    allowedPackages = listOf("com.augmentalis.ava")
)
```

Step 3: Deploy to device
```bash
adb push AVA-768-Multi-INT8.AON /sdcard/ava-ai-models/embeddings/
```

### 2. LLM Models

**File:** `phi-2-q4.gguf`
**Size:** ~1.6 GB
**Not in repo because:** Extremely large

**Download from Hugging Face:**
```bash
# Download URL (example - adjust for your preferred model)
wget https://huggingface.co/TheBloke/phi-2-GGUF/resolve/main/phi-2.Q4_K_M.gguf

# Rename
mv phi-2.Q4_K_M.gguf phi-2-q4.gguf

# Push to device
adb push phi-2-q4.gguf /sdcard/ava-ai-models/llm/
```

**File:** `tokenizer.json`
**Download from same model repo**

### 3. Additional Language Intent Files

**Files:** `es-es.ava`, `fr-fr.ava`, etc.
**Not in repo:** Need to be created

**Create from template:**
```bash
# Copy English template
cp apps/ava-app-android/src/main/assets/ava-examples/en-US/information.ava information-es.ava

# Translate intent examples to target language
# Then push to device
adb push information-es.ava /sdcard/ava-ai-models/intents/es-es.ava
```

---

## Test Documents (Create These)

### 1. test-article.txt

**Create file with sample content:**
```bash
cat > test-article.txt << 'EOF'
Artificial Intelligence and Machine Learning

Artificial intelligence (AI) is transforming how we interact with technology.
Machine learning, a subset of AI, enables computers to learn from data without
explicit programming.

Neural networks are inspired by biological neurons and consist of layers of
interconnected nodes. Deep learning uses multiple layers to extract high-level
features from raw input.

Natural language processing (NLP) allows computers to understand and generate
human language. Applications include chatbots, translation, and sentiment analysis.

Computer vision enables machines to interpret visual information from images
and videos. Applications include object detection, facial recognition, and
autonomous vehicles.
EOF

adb push test-article.txt /sdcard/ava-ai-models/test-data/
```

### 2. knowledge-base.md

**Create file with sample content:**
```bash
cat > knowledge-base.md << 'EOF'
# AVA AI Product Documentation

## Overview

AVA AI is an intelligent assistant that combines natural language understanding
with retrieval-augmented generation for accurate, context-aware responses.

## Features

### Intent Classification
- Dual model architecture (MobileBERT + mALBERT)
- 52+ language support
- 95%+ accuracy on built-in intents

### RAG Search
- Semantic search with vector embeddings
- Keyword search with BM25 algorithm
- Hybrid search with reciprocal rank fusion
- Query caching for 30-50% performance improvement

### Response Generation
- Template-based responses for speed
- LLM-based responses for quality
- Streaming support for real-time output
EOF

adb push knowledge-base.md /sdcard/ava-ai-models/test-data/
```

### 3. sample-document.pdf

**Use any PDF tool or existing document**

---

## Quick Setup Script (Using Repo Files)

```bash
#!/bin/bash
# Setup AVA AI external storage using files from repo

REPO_PATH="/Volumes/M-Drive/Coding/AVA"
cd $REPO_PATH

# 1. Create directories
adb shell mkdir -p /sdcard/ava-ai-models/embeddings
adb shell mkdir -p /sdcard/ava-ai-models/rag
adb shell mkdir -p /sdcard/ava-ai-models/intents
adb shell mkdir -p /sdcard/ava-ai-models/test-data

# 2. Push NLU models (from repo)
adb push app/src/main/assets/models/mobilebert_int8.onnx /sdcard/ava-ai-models/embeddings/mobilebert-uncased-int8.onnx
adb push apps/ava-app-android/src/main/assets/models/vocab.txt /sdcard/ava-ai-models/embeddings/vocab.txt

# 3. Push RAG model (from repo - AON format)
adb push apps/ava-app-android/src/main/assets/models/AVA-384-Base-INT8.AON /sdcard/ava-ai-models/rag/AVA-384-Base-INT8.AON
adb push apps/ava-app-android/src/main/assets/models/vocab.txt /sdcard/ava-ai-models/rag/vocab.txt

# 4. Push all AVA intent files (from repo)
adb push apps/ava-app-android/src/main/assets/ava-examples/en-US/information.ava /sdcard/ava-ai-models/intents/
adb push apps/ava-app-android/src/main/assets/ava-examples/en-US/productivity.ava /sdcard/ava-ai-models/intents/
adb push apps/ava-app-android/src/main/assets/ava-examples/en-US/system-control.ava /sdcard/ava-ai-models/intents/
adb push apps/ava-app-android/src/main/assets/ava-examples/en-US/navigation.ava /sdcard/ava-ai-models/intents/
adb push apps/ava-app-android/src/main/assets/ava-examples/en-US/media-control.ava /sdcard/ava-ai-models/intents/
adb push apps/ava-app-android/src/main/assets/ava-examples/en-US/voiceos-commands.ava /sdcard/ava-ai-models/intents/

# 5. Create test documents
cat > /tmp/test-article.txt << 'EOF'
Artificial Intelligence and Machine Learning

Artificial intelligence (AI) is transforming how we interact with technology.
Machine learning, a subset of AI, enables computers to learn from data without
explicit programming.
EOF

cat > /tmp/knowledge-base.md << 'EOF'
# AVA AI Product Documentation

## Features
- Intent Classification with dual models
- RAG Search with semantic and keyword
- Response Generation with LLM
EOF

adb push /tmp/test-article.txt /sdcard/ava-ai-models/test-data/
adb push /tmp/knowledge-base.md /sdcard/ava-ai-models/test-data/

# 6. Verify
echo "Verifying file placement..."
adb shell ls -lhR /sdcard/ava-ai-models/

echo ""
echo "Setup complete!"
echo ""
echo "Files from repo: ✓"
echo "  - MobileBERT model"
echo "  - RAG embedding model (AON)"
echo "  - 6 AVA intent files"
echo "  - Vocabulary files"
echo "  - Test documents"
echo ""
echo "Optional downloads:"
echo "  - mALBERT multilingual model (120 MB)"
echo "  - LLM models (phi-2, etc.)"
echo ""
echo "Ready to test AVA AI!"
```

---

## Summary

### In Repository (Ready to Use)
- ✅ MobileBERT-384 model (25 MB) - **Bundled in APK**
- ✅ RAG embedding model `AVA-384-Base-INT8.AON` (22 MB) - **Bundled in APK**
- ✅ Vocabulary files (226 KB) - **Bundled in APK**
- ✅ 6 AVA intent files (en-US) - **Bundled in APK**
- ✅ Total: ~47 MB bundled in APK assets

### Need to Download (Optional)
- ⬇️ **mALBERT-768** (`AVA-768-Multi-INT8.AON`, ~90 MB) - Multilingual NLU, better quality
  - **NOT in repo** (too large for git)
  - Download from Hugging Face + wrap with AONFileManager
- ⬇️ **LLM models** (phi-2, etc., ~1.5GB+) - For local response generation
  - **NOT in repo** (extremely large)
- ⬇️ **Additional language intents** - For multilingual support
  - **NOT in repo** (need to be created)

### Need to Create (For Testing)
- 📝 Test documents (text, markdown, PDF)
- 📝 Sample content for RAG ingestion

### Minimum Required for Testing
Just use files from repo:
1. MobileBERT model ✓
2. RAG embedding model (AON) ✓
3. Vocabulary files ✓
4. Intent files (en-US) ✓
5. Simple test documents (create with script above)

**Total size from repo:** ~47 MB
**Ready to test immediately!**

---

**End of File Locations Guide**

**Version:** 1.0
**Last Updated:** 2025-11-28
**Status:** Complete - All files located
