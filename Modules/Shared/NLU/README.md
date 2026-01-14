# AVA NLU Module - Dual Model Strategy

**Natural Language Understanding** with dual model support (MobileBERT + mALBERT)

---

## 🎯 Overview

The NLU module provides intent classification using on-device ONNX models. It supports two build variants:

- **AVA Lite**: MobileBERT (25 MB, English-only, <50ms)
- **AVA Full**: mALBERT (41 MB, 52 languages, <80ms)

---

## 🏗️ Architecture

```
ClassifyIntentUseCase
    ↓
NLUModelFactory
    ↓
┌─────────────┬─────────────┐
│ MobileBERT  │  mALBERT    │
├─────────────┼─────────────┤
│ WordPiece   │ SentencePiece│
└─────────────┴─────────────┘
         ↓
   ONNX Runtime
```

### Key Components

1. **INLUModel** - Interface for all NLU models
2. **NLUModelFactory** - Creates models based on build config
3. **MobileBERTModel** - English-only implementation
4. **mALBERTModel** - Multilingual implementation
5. **ITokenizer** - Tokenization abstraction
6. **ClassifyIntentUseCase** - High-level API

---

## 🚀 Quick Start

### Using the NLU Module

```kotlin
// Get singleton instance
val useCase = ClassifyIntentUseCase.getInstance(context)

// Classify intent
val result = useCase.execute("turn on the lights", language = "en")

when (result) {
    is Result.Success -> {
        val intent = result.data
        println("Intent: ${intent.name}")
        println("Confidence: ${intent.confidence}")
    }
    is Result.Error -> {
        println("Error: ${result.message}")
    }
}

// Check language support
if (useCase.supportsLanguage("es")) {
    val result = useCase.execute("enciende las luces", "es")
}

// Cleanup when done
useCase.cleanup()
```

### Building Variants

```bash
# Build Lite variant (MobileBERT)
./gradlew assembleLiteDebug

# Build Full variant (mALBERT)
./gradlew assembleFullDebug
```

---

## 📦 Model Comparison

| Feature | MobileBERT (Lite) | mALBERT (Full) |
|---------|-------------------|----------------|
| **Size** | 25 MB | 41 MB |
| **Languages** | 1 (English) | 52 (Multilingual) |
| **Vocab** | 30,522 | 128,000 |
| **Tokenizer** | WordPiece | SentencePiece |
| **Inference** | <50ms | <80ms |
| **Memory** | <100 MB | <200 MB |

---

## 🌍 Supported Languages (Full Variant)

### European (23)
English, Spanish, French, German, Italian, Portuguese, Dutch, Polish, Romanian, Czech, Swedish, Hungarian, Greek, Danish, Finnish, Norwegian, Slovak, Bulgarian, Croatian, Lithuanian, Slovenian, Estonian, Latvian

### Asian (14)
Japanese, Chinese, Korean, Thai, Vietnamese, Indonesian, Malay, Hindi, Bengali, Tamil, Telugu, Urdu, Nepali, Sinhala

### Middle Eastern (4)
Arabic, Hebrew, Persian, Turkish

### Other (4)
Russian, Ukrainian, Catalan, Serbian

---

## 🔧 Model Conversion

To download and convert mALBERT for Android:

```bash
# Setup Python environment
python3 -m venv venv
source venv/bin/activate
pip install transformers optimum onnx onnxruntime torch sentencepiece

# Run conversion script
python scripts/convert_malbert_to_onnx.py
```

**Output**:
- `models/malbert/malbert_int8.onnx` (41 MB)
- `models/malbert/tokenizer/` (vocab files)

**Time**: ~10-15 minutes

---

## 🧪 Testing

```bash
# Unit tests
./gradlew :features:nlu:testDebugUnitTest

# Integration tests (on device)
./gradlew :features:nlu:connectedDebugAndroidTest

# Specific test
./gradlew :features:nlu:connectedDebugAndroidTest \
  --tests "*mALBERTModelTest*"
```

**Test Coverage**: 50 tests (28 unit + 22 integration)

---

## 📚 Documentation

- **Implementation Guide**: `docs/active/Guide-mALBERT-Implementation-251031-1145.md`
- **Week 7 Status**: `docs/active/Status-Week7-Complete-251031-1200.md`
- **Architecture**: `docs/active/Status-Dual-NLU-Architecture-251031-1130.md`
- **Dual NLU Plan**: `docs/active/Plan-Dual-NLU-Strategy-251031-0050.md`

---

**Created**: 2025-10-31
**Status**: ✅ Week 7 Complete
**Author**: AVA Team
