# NLU - Module Instructions

Parent Repository: NewAvanues
Module: NLU

---

## SCOPE

Work within NLU module only.
For cross-module changes, check with user first.

---

## INHERITED RULES

1. Parent repo rules: `/Volumes/M-Drive/Coding/NewAvanues/.claude/CLAUDE.md`
2. Global rules: `/Volumes/M-Drive/Coding/.claude/CLAUDE.md`

---

## DOCUMENTATION LOCATIONS

**Living Docs:** `/Volumes/M-Drive/Coding/NewAvanues/Docs/NLU/LivingDocs/LD-*.md`
**Registries:** `Modules/NLU/.ideacode/registries/`
- FOLDER-REGISTRY.md - Folder structure for this module
- FILE-REGISTRY.md - File naming for this module
- COMPONENT-REGISTRY.md - Components in this module

**Check Registries FIRST** before creating files or folders.

---

## MODULE-SPECIFIC RULES

| Rule | Requirement |
|------|-------------|
| Primary Language | Python 3.9+ |
| ML Framework | TensorFlow or PyTorch |
| Kotlin Interface | Kotlin interface for Android/KMP integration |
| Models | Version all models (e.g., `intent_v1.pkl`) |
| Testing | 90%+ coverage for core NLU logic |

---

## KEY COMPONENTS

- **Intent Recognizer** - ML-based intent classification
- **Entity Extractor** - Named entity recognition
- **Context Manager** - Conversation context tracking
- **Model Trainer** - Training pipeline for ML models

---

## PERFORMANCE TARGETS

| Metric | Current | Target |
|--------|---------|--------|
| Intent Accuracy | 89% | 95%+ |
| Entity F1 Score | 85% | 90%+ |
| Response Time | <100ms | <50ms |

---

## DEPENDENCIES

**Internal:**
- `Common/Core` - Shared utilities

**External:**
- Python 3.9+
- TensorFlow/PyTorch
- scikit-learn
- NLTK

See: `/Volumes/M-Drive/Coding/NewAvanues/.ideacode/registries/CROSS-MODULE-DEPENDENCIES.md`

---

## FILE NAMING

| Type | Pattern | Example |
|------|---------|---------|
| Living Docs | `LD-NLU-{Desc}-V#.md` | `LD-NLU-Feature-V1.md` |
| Specs | `NLU-Spec-{Feature}-YDDMM-V#.md` | `NLU-Spec-Intent-51215-V1.md` |
| Python | `{script}.py` | `train_model.py` |
| Kotlin Interface | `{Component}.kt` | `IntentRecognizer.kt` |
| Models | `{model}_v{version}.{ext}` | `intent_classifier_v1.pkl` |

---

## BUILDING & TESTING

```bash
# Setup virtual environment
python3 -m venv venv
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt

# Train models
python training/train_model.py

# Run tests
pytest tests/

# Performance benchmark
python benchmark/benchmark_nlu.py
```

---

## KOTLIN INTEROP

```kotlin
// Kotlin interface for NLU service
interface NLUService {
    suspend fun processText(
        text: String,
        context: Map<String, Any>
    ): NLUResult
}
```

See: `/Volumes/M-Drive/Coding/NewAvanues/Docs/NLU/LivingDocs/LD-NLU-API-Contracts-V1.md`

---

Updated: 2025-12-15 | Version: 12.0.0
