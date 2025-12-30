# AVA Technology Stack - Modified Components

**Purpose:** Definitive reference for AVA-specific modifications to upstream technologies
**Status:** AUTHORITATIVE - Reference this document for all technical discussions
**Classification:** Internal Development Reference
**Last Updated:** 2025-11-21

---

## ⚠️ CRITICAL: AVA Uses Modified Technologies

**AVA is NOT using vanilla/upstream versions of these technologies.**

All AI/ML components have been significantly modified, updated, and optimized for AVA. **They are NOT drop-in replacements** and cannot be substituted with upstream versions.

---

## 1. AON (ONNX Runtime) - AVA's Modified ONNX

### What It Is

**AON** = **A**VA **ON**NX Runtime (Modified)

### Modifications from Upstream ONNX

| Aspect | Upstream ONNX | AON (AVA Modified) |
|--------|---------------|-------------------|
| **Purpose** | General ONNX inference | Optimized for ALC-LLM integration |
| **TVM Version** | N/A or older | TVM 0.22 (latest) |
| **Integration** | Standalone | Tightly coupled with ALC-LLM |
| **Code Quality** | Legacy codebase | Updated, modernized codebase |
| **Drop-in Replace?** | N/A | ❌ NO - Not compatible with vanilla ONNX |

### Why NOT Vanilla ONNX?

1. **ALC-LLM Compatibility:** AON is modified to work seamlessly with ALC-LLM's TVM 0.22 backend
2. **Better Code:** Upstream ONNX has legacy code; AON has updated, cleaner implementation
3. **Performance:** Optimizations specific to AVA's use cases
4. **Integration:** Deep integration with AVA's mALBERT model format

### File Locations

```
Universal/AVA/Features/NLU/src/androidMain/kotlin/.../aon/
├── AONRuntime.kt          # Modified ONNX runtime
├── AONSession.kt          # Session management
└── AONTensorUtils.kt      # Tensor utilities
```

### Usage in Code

```kotlin
// ❌ WRONG: Do NOT reference "ONNX"
import ai.onnxruntime.*  // This is vanilla ONNX

// ✅ CORRECT: Use AON
import com.augmentalis.ava.features.nlu.aon.*
val runtime = AONRuntime()
```

### Documentation References

When discussing NLU inference:
- ✅ **Correct:** "AON (AVA's modified ONNX Runtime)"
- ✅ **Correct:** "AON with TVM 0.22 backend"
- ❌ **Wrong:** "ONNX Runtime"
- ❌ **Wrong:** "Vanilla ONNX"

---

## 2. ALC-LLM - AVA's Modified MLC-LLM

### What It Is

**ALC-LLM** = **A**VA **L**ocal **C**hat **LLM** (Modified MLC-LLM)

### Modifications from Upstream MLC-LLM

| Aspect | Upstream MLC-LLM | ALC-LLM (AVA Modified) |
|--------|------------------|----------------------|
| **TVM Version** | 0.15-0.18 (legacy) | **TVM 0.22** (latest) |
| **Code Quality** | Legacy codebase | **Updated, modernized** |
| **Model Support** | Standard Gemma 2 | **Gemma 3** (ava-GE3) |
| **Integration** | Standalone | **Deep AON integration** |
| **Native Libs** | Standard | **Custom compiled** |
| **Drop-in Replace?** | N/A | ❌ **NO - Incompatible** |

### Key Improvements Over MLC-LLM

1. **TVM 0.22:** Latest TVM version vs legacy MLC-LLM
2. **Better Code:** Cleaner, more maintainable implementation
3. **Gemma 3 Support:** Optimized for latest Gemma 3 models (ava-GE3)
4. **AON Integration:** Works seamlessly with AVA's modified ONNX runtime
5. **Performance:** AVA-specific optimizations

### Why NOT Vanilla MLC-LLM?

**MLC-LLM is legacy.** ALC-LLM has:
- ✅ Newer TVM backend (0.22 vs 0.15-0.18)
- ✅ Better code quality
- ✅ Gemma 3 support (not just Gemma 2)
- ✅ Tighter integration with AVA ecosystem
- ✅ Custom optimizations

**ALC-LLM is NOT a drop-in replacement for MLC-LLM.**

### File Locations

```
Universal/AVA/Features/LLM/src/main/java/.../alc/
├── ALCEngine.kt           # Main ALC-LLM engine
├── ALCChat.kt             # Chat interface
├── ALCTokenizer.kt        # TVM 0.22 tokenizer
└── provider/
    └── LocalLLMProvider.kt  # ALC-LLM provider
```

### Native Libraries

**Location:** `apps/ava-standalone/src/main/jniLibs/arm64-v8a/`

```
├── libtvm_runtime.so      # TVM 0.22 runtime
├── libtokenizers.so       # TVM tokenizer
└── libmlc_llm.so          # ALC-LLM core
```

**These are custom-compiled for TVM 0.22 - NOT from upstream MLC-LLM.**

### Usage in Code

```kotlin
// ❌ WRONG: Do NOT reference "MLC-LLM"
import org.mlc.mlc_llm.*  // This is vanilla MLC-LLM

// ✅ CORRECT: Use ALC-LLM
import com.augmentalis.ava.features.llm.alc.*
val engine = ALCEngine(context)
```

### Documentation References

When discussing local LLM:
- ✅ **Correct:** "ALC-LLM (AVA's modified MLC-LLM)"
- ✅ **Correct:** "ALC-LLM with TVM 0.22"
- ❌ **Wrong:** "MLC-LLM"
- ❌ **Wrong:** "Vanilla MLC-LLM"

---

## 3. mALBERT - AVA's Modified ALBERT

### What It Is

**mALBERT** = **m**odified **ALBERT** (Lite BERT for AVA)

### Modifications from Upstream Models

| Aspect | MobileBERT | ALBERT | mALBERT (AVA) |
|--------|------------|--------|---------------|
| **Base** | Google MobileBERT | Google ALBERT | **Modified ALBERT** |
| **Size** | 25 MB | 12-60 MB | **Optimized for AVA** |
| **Quantization** | INT8 | FP32/FP16 | **INT8 (AVA-optimized)** |
| **Runtime** | ONNX | PyTorch/TF | **AON (modified ONNX)** |
| **Tokenizer** | WordPiece | SentencePiece | **WordPiece (modified)** |

### Why NOT MobileBERT?

**AVA uses mALBERT (modified ALBERT), NOT MobileBERT.**

Reasons:
1. **Better compression:** ALBERT's parameter sharing
2. **AVA-specific tuning:** Optimized for AVA's intent classification
3. **AON compatibility:** Modified to work with AON runtime
4. **Quantization:** Custom INT8 quantization for AVA

### Model Files

```
Universal/AVA/Features/NLU/src/commonMain/resources/models/
└── malbert-intent-classifier-int8.onnx  # mALBERT model
```

**This is NOT MobileBERT - it's mALBERT (modified ALBERT).**

### Usage in Code

```kotlin
// ❌ WRONG: Do NOT reference "MobileBERT"
val model = "mobile_bert_intent_classifier.onnx"

// ✅ CORRECT: Use mALBERT
val model = "malbert-intent-classifier-int8.onnx"
```

### Documentation References

When discussing NLU model:
- ✅ **Correct:** "mALBERT (AVA's modified ALBERT)"
- ✅ **Correct:** "mALBERT INT8 model"
- ❌ **Wrong:** "MobileBERT"
- ❌ **Wrong:** "ALBERT" (without the 'm' prefix)

---

## 4. Gemma 3 (ava-GE3) - AVA's LLM Model

### What It Is

**Gemma 3** = Google's Gemma 3 model series, compiled for ALC-LLM

**ava-GE3** = AVA's proprietary naming for Gemma 3 models

### Modifications from Upstream Gemma

| Aspect | Gemma 2 | Gemma 3 (ava-GE3) |
|--------|---------|-------------------|
| **Version** | Gemma 2 (older) | **Gemma 3** (latest) |
| **Compilation** | Standard MLC-LLM | **ALC-LLM (TVM 0.22)** |
| **Quantization** | Q4F16 | **Q4BF16** (better) |
| **Runtime** | MLC-LLM (TVM 0.15) | **ALC-LLM (TVM 0.22)** |
| **Format** | .tar (MLC) | **.tar (ALC-specific)** |

### Why NOT Gemma 2?

**AVA uses Gemma 3**, which has:
1. **Latest model:** Gemma 3 improvements over Gemma 2
2. **Better quantization:** Q4BF16 (bfloat16) vs Q4F16 (float16)
3. **ALC-LLM compilation:** Compiled with TVM 0.22, not legacy MLC
4. **AVA optimizations:** Custom compilation flags

### Model Files

**AVA Naming:**
```
AVA-GE3-2B-Q4.tar    # Gemma 3, 2 billion params, Q4 quant
AVA-GE3-7B-Q4.tar    # Gemma 3, 7 billion params, Q4 quant
```

**Upstream Source (before AVA compilation):**
```
gemma-3-2b-it        # Google's Gemma 3 2B instruct
gemma-3-7b-it        # Google's Gemma 3 7B instruct
```

**After ALC-LLM Compilation:**
```
ava-GE3-2b-q4bf16_1-android.tar   # Compiled for ALC-LLM
```

### File Locations

```
apps/ava-standalone/src/main/assets/models/llm/
└── ava-GE3-2b-q4bf16_1-android.tar  # Gemma 3 for ALC-LLM
```

### Usage in Code

```kotlin
// ❌ WRONG: Do NOT reference "Gemma 2"
val modelPath = "gemma-2b-it-q4f16_1-MLC"

// ✅ CORRECT: Use Gemma 3 (ava-GE3)
val modelPath = "ava-GE3-2b-q4bf16_1-android"
```

### Documentation References

When discussing LLM model:
- ✅ **Correct:** "Gemma 3 (ava-GE3)"
- ✅ **Correct:** "ava-GE3-2b model"
- ❌ **Wrong:** "Gemma 2"
- ❌ **Wrong:** "Gemma 2B" (without version 3 clarification)

---

## 5. TVM 0.22 - The Common Thread

### Why TVM 0.22 Matters

**All AVA modifications are built on TVM 0.22.**

| Component | TVM Version | Why It Matters |
|-----------|-------------|----------------|
| **AON** | 0.22 | Latest performance optimizations |
| **ALC-LLM** | 0.22 | Modern TVM APIs |
| **mALBERT** | 0.22 via AON | Consistent runtime |

### Upstream Versions (Outdated)

| Technology | TVM Version | Status |
|------------|-------------|--------|
| Vanilla ONNX Runtime | N/A or older | ❌ Legacy |
| MLC-LLM (upstream) | 0.15-0.18 | ❌ Legacy |
| AVA Stack (AON + ALC) | **0.22** | ✅ **Latest** |

**This is why AVA components are NOT drop-in replacements.**

---

## Quick Reference: Correct Terminology

### ✅ DO Say:

| Component | Correct Term |
|-----------|-------------|
| ONNX Runtime | **AON (AVA's modified ONNX)** |
| MLC-LLM | **ALC-LLM (AVA's modified MLC-LLM)** |
| Model (NLU) | **mALBERT (modified ALBERT)** |
| Model (LLM) | **Gemma 3 (ava-GE3)** |
| TVM Version | **TVM 0.22** |

### ❌ DON'T Say:

| Wrong Term | Why Wrong |
|------------|-----------|
| "ONNX Runtime" | Too generic, implies vanilla ONNX |
| "MLC-LLM" | Implies upstream, which is legacy |
| "MobileBERT" | AVA uses mALBERT, not MobileBERT |
| "Gemma 2" or "Gemma 2B" | AVA uses Gemma 3 |
| "TVM 0.15" | Outdated, AVA uses 0.22 |

---

## Code Import Rules

### ✅ Correct Imports

```kotlin
// AON (not vanilla ONNX)
import com.augmentalis.ava.features.nlu.aon.*

// ALC-LLM (not MLC-LLM)
import com.augmentalis.ava.features.llm.alc.*

// mALBERT model reference
const val MODEL_FILE = "malbert-intent-classifier-int8.onnx"

// Gemma 3 model reference
const val LLM_MODEL = "ava-GE3-2b-q4bf16_1-android"
```

### ❌ Incorrect Imports

```kotlin
// ❌ WRONG: Vanilla ONNX
import ai.onnxruntime.*

// ❌ WRONG: Upstream MLC-LLM
import org.mlc.mlc_llm.*

// ❌ WRONG: MobileBERT reference
const val MODEL = "mobile_bert.onnx"

// ❌ WRONG: Gemma 2 reference
const val MODEL = "gemma-2b-it"
```

---

## Documentation Update Checklist

When writing/updating docs, ensure:

- [ ] Replace "ONNX Runtime" with "AON (AVA's modified ONNX)"
- [ ] Replace "MLC-LLM" with "ALC-LLM (AVA's modified MLC-LLM)"
- [ ] Replace "MobileBERT" with "mALBERT (modified ALBERT)"
- [ ] Replace "Gemma 2" or "Gemma 2B" with "Gemma 3 (ava-GE3)"
- [ ] Mention "TVM 0.22" when discussing runtime
- [ ] Clarify "NOT drop-in replacements" when comparing to upstream
- [ ] Add "modified" or "AVA-specific" qualifiers

---

## Related Documentation

**Primary References:**
- `AVA-MODEL-NAMING-REGISTRY.md` - Model filename mappings
- `DEVELOPER-MANUAL.md` - Developer guide (needs update)
- `USER-MANUAL.md` - User guide (needs update)
- `REGISTRY.md` - Project registry (needs update)
- `FEATURE-PARITY-MATRIX.md` - Platform support (needs update)

**Technical Specs:**
- `Universal/AVA/Features/NLU/README.md` - AON + mALBERT details
- `Universal/AVA/Features/LLM/README.md` - ALC-LLM + Gemma 3 details

---

## Change Log

| Date | Change | Updated By |
|------|--------|------------|
| 2025-11-21 | Initial technology stack documentation created | AVA AI Team |
| 2025-11-21 | Clarified AON vs ONNX, ALC-LLM vs MLC-LLM | AVA AI Team |
| 2025-11-21 | Clarified mALBERT vs MobileBERT, Gemma 3 vs Gemma 2 | AVA AI Team |
| 2025-11-21 | Added TVM 0.22 as common thread | AVA AI Team |

---

**Classification:** INTERNAL DEVELOPMENT REFERENCE
**Distribution:** Development Team Only
**Authority:** DEFINITIVE - Override all conflicting documentation
**Last Updated:** 2025-11-21
**Version:** 1.0
