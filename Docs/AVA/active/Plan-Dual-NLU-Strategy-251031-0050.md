# Dual NLU Strategy: MobileBERT + mALBERT

**Created**: 2025-10-31 00:50 PDT
**Status**: Planning
**Author**: AVA Team

---

## Strategy Overview

Support **both** MobileBERT (English-only) and mALBERT (multilingual) as configurable NLU options, allowing us to choose at release time based on:
- Target market (English-only vs multilingual)
- Storage constraints (25 MB vs 82 MB)
- App variant (Lite vs Full)

---

## Architecture: Dual NLU Support

### Configuration-Based Selection

```kotlin
// Configuration determines which NLU model to use
enum class NLUModelType {
    MOBILEBERT_ENGLISH,  // 25 MB, English-only, fastest
    MALBERT_MULTILINGUAL // 82 MB, 52 languages, slightly slower
}

// Selected at build time via BuildConfig or runtime via settings
object NLUConfig {
    val modelType: NLUModelType = when {
        BuildConfig.FLAVOR == "lite" -> NLUModelType.MOBILEBERT_ENGLISH
        BuildConfig.FLAVOR == "full" -> NLUModelType.MALBERT_MULTILINGUAL
        else -> NLUModelType.MOBILEBERT_ENGLISH // Default
    }
}
```

### NLU Factory Pattern

```kotlin
interface INLUModel {
    suspend fun classify(text: String, language: String = "en"): Intent
    fun getSupportedLanguages(): List<String>
    fun getModelSize(): Long
}

class NLUModelFactory {
    fun createModel(type: NLUModelType, context: Context): INLUModel {
        return when (type) {
            NLUModelType.MOBILEBERT_ENGLISH -> MobileBERTModel(context)
            NLUModelType.MALBERT_MULTILINGUAL -> mALBERTModel(context)
        }
    }
}
```

### Model Implementations

```kotlin
class MobileBERTModel(context: Context) : INLUModel {
    private val onnxSession = // Load mobilebert_int8.onnx

    override suspend fun classify(text: String, language: String): Intent {
        if (language != "en") {
            throw UnsupportedOperationException("MobileBERT only supports English")
        }
        // Existing MobileBERT inference logic
    }

    override fun getSupportedLanguages() = listOf("en")
    override fun getModelSize() = 25_500_000L // 25.5 MB
}

class mALBERTModel(context: Context) : INLUModel {
    private val onnxSession = // Load malbert_int8.onnx

    override suspend fun classify(text: String, language: String): Intent {
        // mALBERT inference (supports 52 languages)
    }

    override fun getSupportedLanguages() = listOf(
        "en", "es", "fr", "de", "ja", "zh", "pt", "it", "ko", "ar",
        // ... 42 more languages
    )
    override fun getModelSize() = 41_000_000L // ~41 MB (INT8)
}
```

---

## Release Variants

### Variant 1: AVA Lite (English-only)

**Target**: English-speaking markets, storage-constrained devices

**NLU Model**: MobileBERT (25 MB)
**LLM Models**: English only (Gemma 2B)
**Total Storage**: ~4.2 GB
**Languages**: 1 (English)

**Build Configuration**:
```gradle
productFlavors {
    lite {
        dimension "version"
        applicationIdSuffix ".lite"
        buildConfigField "String", "NLU_MODEL", '"MOBILEBERT"'
    }
}
```

**Benefits**:
- ✅ Smallest storage footprint
- ✅ Fastest NLU inference (<50ms)
- ✅ Lower price point potential
- ✅ Simpler first release

### Variant 2: AVA Full (Multilingual)

**Target**: Global markets, multilingual users

**NLU Model**: mALBERT (82 MB, or ~41 MB INT8)
**LLM Models**: English + optional language packs
**Total Storage**: ~4.26 GB (base) + language packs
**Languages**: 52 supported (download on demand)

**Build Configuration**:
```gradle
productFlavors {
    full {
        dimension "version"
        applicationIdSuffix ".full"
        buildConfigField "String", "NLU_MODEL", '"MALBERT"'
    }
}
```

**Benefits**:
- ✅ Global market reach
- ✅ Single app for all languages
- ✅ Premium positioning
- ✅ Future-proof

### Variant 3: AVA Pro (Future)

**Target**: Power users, enterprise

**NLU Model**: mALBERT + fine-tuned custom models
**LLM Models**: Larger models (7B+), multiple simultaneous languages
**Total Storage**: Configurable
**Languages**: All 52 + custom

**Features**:
- Multiple language packs installed simultaneously
- Larger LLM models
- Custom domain-specific models
- Enterprise integrations

---

## Implementation Plan

### Phase 1: Prepare Dual Model Support (Week 7, Days 1-2)

**Tasks**:
1. ✅ Create `INLUModel` interface
2. ✅ Refactor existing `IntentClassifier` to `MobileBERTModel`
3. ✅ Create `NLUModelFactory`
4. ✅ Add model type configuration to `BuildConfig`
5. ✅ Update `ClassifyIntentUseCase` to use factory

**Files to Modify**:
- `features/nlu/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/`
  - Create: `INLUModel.kt`
  - Create: `NLUModelFactory.kt`
  - Rename: `IntentClassifier.kt` → `MobileBERTModel.kt`
  - Update: `ClassifyIntentUseCase.kt`

### Phase 2: Download & Convert mALBERT (Week 7, Days 3-4)

**Tasks**:
1. Download mALBERT from HuggingFace
2. Convert SafeTensors to ONNX
3. Quantize ONNX to INT8
4. Test inference speed on Android
5. Validate accuracy on test dataset

**Commands**:
```bash
# Download
huggingface-cli download cservan/malbert-base-cased-128k

# Convert to ONNX (Python script)
python convert_malbert_to_onnx.py

# Quantize to INT8
python quantize_malbert.py

# Result: malbert_int8.onnx (~41 MB)
```

**Target Metrics**:
- Model size: ~41 MB (INT8)
- Inference time: <100ms (Android device)
- Accuracy: >90% (on MMNLU benchmark)

### Phase 3: Implement mALBERTModel (Week 7, Days 5-6)

**Tasks**:
1. Create `mALBERTModel.kt` class
2. Load ONNX model in ONNX Runtime
3. Implement tokenization (SentencePiece)
4. Implement classification logic
5. Unit tests (mock ONNX session)
6. Integration tests (real model)

**Files to Create**:
- `features/nlu/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/mALBERTModel.kt`
- `features/nlu/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/SentencePieceTokenizer.kt`

### Phase 4: Update Language Filter (Week 7, Day 7)

**Tasks**:
1. Update `LocalizedLanguageFilter` to check NLU model type
2. Only show multilingual languages if mALBERT is active
3. Update language settings UI
4. Test language switching flow

**Logic**:
```kotlin
class LocalizedLanguageFilter(
    private val context: Context,
    private val nluModel: INLUModel
) {
    fun getAvailableLanguages(): Set<String> {
        val nluLanguages = nluModel.getSupportedLanguages()
        val appLocalizations = getAppLocalizations()

        // Only show languages supported by BOTH NLU and app UI
        return nluLanguages.intersect(appLocalizations)
    }
}
```

### Phase 5: Build Variants (Week 8, Day 1)

**Tasks**:
1. Configure Gradle product flavors
2. Add build type selection
3. Test both variants
4. Update CI/CD pipeline

**Gradle Configuration**:
```kotlin
android {
    flavorDimensions += "version"

    productFlavors {
        create("lite") {
            dimension = "version"
            applicationIdSuffix = ".lite"
            versionNameSuffix = "-lite"

            buildConfigField("String", "NLU_MODEL_TYPE", "\"MOBILEBERT\"")

            // Only include MobileBERT assets
            sourceSets {
                getByName("main") {
                    assets.srcDirs("src/main/assets/models/mobilebert")
                }
            }
        }

        create("full") {
            dimension = "version"
            applicationIdSuffix = ".full"
            versionNameSuffix = "-full"

            buildConfigField("String", "NLU_MODEL_TYPE", "\"MALBERT\"")

            // Include mALBERT assets
            sourceSets {
                getByName("main") {
                    assets.srcDirs("src/main/assets/models/malbert")
                }
            }
        }
    }
}
```

---

## Storage Comparison

| Variant | NLU Model | NLU Size | Base LLM | Total | Languages |
|---------|-----------|----------|----------|-------|-----------|
| **Lite** | MobileBERT | 25 MB | 4.2 GB | **4.23 GB** | 1 (en) |
| **Full** | mALBERT | 82 MB | 4.2 GB | **4.28 GB** | 52 |
| **Full (INT8)** | mALBERT INT8 | ~41 MB | 4.2 GB | **4.24 GB** | 52 |

**Analysis**:
- Lite → Full: +60 MB (+1.4%)
- Lite → Full (INT8): +20 MB (+0.5%)

**Conclusion**: Negligible storage difference for 51 additional languages!

---

## Performance Comparison

| Variant | NLU Inference | Accuracy | Supported Languages |
|---------|---------------|----------|---------------------|
| **MobileBERT** | <50ms | 95%+ (English) | 1 |
| **mALBERT** | ~60-80ms (est) | 90%+ (multilingual) | 52 |

**Trade-off**: ~10-30ms slower for 51 additional languages (acceptable)

---

## Recommendation Matrix

### Use MobileBERT (Lite) When:
- ✅ Targeting English-only markets (US, UK, Australia)
- ✅ Storage is critical (<5 GB available)
- ✅ Fastest possible inference required
- ✅ Want lowest price point
- ✅ First release (MVP focus)

### Use mALBERT (Full) When:
- ✅ Targeting global markets
- ✅ Users speak multiple languages
- ✅ Premium positioning
- ✅ Storage not constrained
- ✅ Future-proofing important

### Release Strategy Recommendation:

**Phase 1 (Initial Release)**:
- Release **Lite** (MobileBERT) first
- English-only markets
- Lower storage footprint
- Faster to market

**Phase 2 (3-6 months later)**:
- Release **Full** (mALBERT) variant
- Global expansion
- Multilingual support
- Premium tier ($9.99 → $14.99?)

**Phase 3 (1 year later)**:
- Sunset Lite variant OR keep both
- Upgrade existing users to Full
- mALBERT becomes standard

---

## Migration Path (Lite → Full)

### Option A: In-App Upgrade
```kotlin
// Download mALBERT model via OTA update
LanguagePackManager.downloadModel(ModelType.MALBERT)

// Switch NLU model at runtime
NLUModelFactory.switchModel(NLUModelType.MALBERT_MULTILINGUAL)

// Show new language options
LanguageSettings.showMultilingual()
```

### Option B: Separate App (Recommended)
- AVA Lite (English)
- AVA Full (Multilingual)
- Users choose at install time
- Easier to manage in Play Store

---

## Technical Decisions

### Decision 1: Model Selection Timing

**Option A: Build-Time Selection** ✅ RECOMMENDED
- Gradle flavor determines which model is included
- Smaller APK size (only one model)
- Simpler user experience

**Option B: Runtime Selection**
- Include both models in APK
- User chooses in settings
- Larger APK (+107 MB)
- More flexible but confusing

**Recommendation**: Build-time selection (separate variants)

### Decision 2: Tokenizer Strategy

**MobileBERT**: WordPiece (vocab.txt, 226 KB)
**mALBERT**: SentencePiece (spiece.model, 2.41 MB)

**Approach**:
- Create `ITokenizer` interface
- `WordPieceTokenizer` for MobileBERT
- `SentencePieceTokenizer` for mALBERT
- Factory pattern selects correct tokenizer

### Decision 3: Language Filtering

**Requirement**: Only show languages where BOTH are true:
1. NLU model supports the language
2. App has complete strings.xml translation

**Implementation**:
```kotlin
fun getAvailableLanguages(): Set<String> {
    val nluSupported = nluModel.getSupportedLanguages()
    val appLocalized = LocalizedLanguageFilter.SUPPORTED_LANGUAGES
    return nluSupported.intersect(appLocalized)
}
```

**Result**:
- Lite variant: Only shows "English"
- Full variant: Shows "English" initially, more as we add translations

---

## File Structure

### Assets Directory

```
platform/app/src/main/assets/models/
├── mobilebert/                    # Lite variant
│   ├── mobilebert_int8.onnx      (25.5 MB)
│   └── vocab.txt                  (226 KB)
└── malbert/                       # Full variant
    ├── malbert_int8.onnx          (~41 MB target)
    ├── spiece.model               (2.41 MB)
    └── spiece.vocab               (2.28 MB)
```

### Source Structure

```
features/nlu/src/androidMain/kotlin/com/augmentalis/ava/features/nlu/
├── INLUModel.kt                   # Interface
├── NLUModelFactory.kt             # Factory pattern
├── models/
│   ├── MobileBERTModel.kt         # MobileBERT implementation
│   └── mALBERTModel.kt            # mALBERT implementation
└── tokenizers/
    ├── ITokenizer.kt              # Tokenizer interface
    ├── WordPieceTokenizer.kt      # For MobileBERT
    └── SentencePieceTokenizer.kt  # For mALBERT
```

---

## Next Steps (Week 7 Roadmap)

### Day 1-2: Architecture Refactoring ✅ PRIORITY
- [ ] Create `INLUModel` interface
- [ ] Create `NLUModelFactory`
- [ ] Refactor `IntentClassifier` → `MobileBERTModel`
- [ ] Add build configuration support
- [ ] Update `ClassifyIntentUseCase`

### Day 3-4: mALBERT Download & Conversion
- [ ] Download mALBERT from HuggingFace
- [ ] Convert SafeTensors → ONNX
- [ ] Quantize ONNX → INT8
- [ ] Benchmark on Android device
- [ ] Validate accuracy

### Day 5-6: mALBERT Implementation
- [ ] Create `mALBERTModel.kt`
- [ ] Implement SentencePiece tokenization
- [ ] Implement classification logic
- [ ] Unit tests
- [ ] Integration tests

### Day 7: Language Filtering & UI
- [ ] Update `LocalizedLanguageFilter`
- [ ] Test language availability logic
- [ ] Update documentation

### Week 8 Day 1: Build Variants
- [ ] Configure Gradle flavors
- [ ] Test Lite build
- [ ] Test Full build
- [ ] Update CI/CD

---

## Success Criteria

### Technical
- ✅ Both MobileBERT and mALBERT compile successfully
- ✅ Factory pattern selects correct model at runtime
- ✅ NLU inference <100ms for both models
- ✅ Accuracy >90% for both models
- ✅ Build variants generate correct APKs

### Product
- ✅ Clear differentiation between Lite and Full
- ✅ Storage footprint acceptable for both
- ✅ User experience smooth in both variants
- ✅ Migration path defined for upgrades

---

## Risk Mitigation

### Risk 1: mALBERT Accuracy Lower Than Expected
**Mitigation**: Keep MobileBERT as default, mALBERT as optional upgrade
**Fallback**: Use DistilBERT multilingual (~227 MB)

### Risk 2: mALBERT Inference Too Slow
**Mitigation**: Profile and optimize ONNX model
**Fallback**: Keep MobileBERT, add language packs with custom fine-tuned MobileBERTs per language

### Risk 3: SentencePiece Integration Issues
**Mitigation**: Test thoroughly with diverse inputs
**Fallback**: Use alternative tokenization library

### Risk 4: APK Size Too Large
**Mitigation**: Use Android App Bundles, dynamic feature modules
**Fallback**: Remove less common languages from mALBERT

---

## Conclusion

Dual NLU strategy provides maximum flexibility:
- ✅ **MobileBERT (Lite)**: Fast, small, English-only
- ✅ **mALBERT (Full)**: Multilingual, slightly larger
- ✅ **Build-time selection**: Clean architecture
- ✅ **Migration path**: Clear upgrade strategy

**Recommendation**: Start with MobileBERT (Lite) for initial release, add mALBERT (Full) variant 3-6 months later for global expansion.

---

**Created by**: Manoj Jhawar, manoj@ideahq.net
