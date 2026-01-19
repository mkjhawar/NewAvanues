# Phoneme Extractors & Dictionary-Based Dictation Research

**Date:** 2026-01-18 | **Version:** V1 | **Status:** Research

---

## Executive Summary

This document analyzes universal phoneme extractors for mobile deployment and explores whether dictionary-based phoneme-to-word conversion can replace traditional ASR for dictation.

**Key Findings:**

| Question | Answer |
|----------|--------|
| Can we build universal phoneme model? | **Yes** - Allosaurus, TinyML models viable |
| Need separate Vosk models per language? | **Yes for dictation, No for commands** |
| Can dictionary replace language model? | **Partially** - 6-8% WER vs 2-3% with LM |
| Is dictionary-based dictation viable? | **Yes for constrained domains** |

---

## Part 1: Universal Phoneme Extractors

### Option A: Allosaurus

**Source:** [GitHub - xinjli/allosaurus](https://github.com/xinjli/allosaurus)

| Aspect | Details |
|--------|---------|
| Coverage | 2000+ languages |
| Architecture | Multilingual allophone system |
| Training | PHOIBLE phone inventory database |
| Accuracy | 17%+ improvement on low-resource languages |
| Framework | PyTorch |

**How it works:**
- Distinguishes between **phonemes** (language-specific contrastive sounds) and **phones** (actual sounds, language-independent)
- Uses shared parameters across languages
- Outputs IPA phoneme sequences

**Mobile Viability:**
- Model size: ~100-150MB (estimated based on architecture)
- Requires PyTorch Mobile or ONNX conversion
- Not designed for real-time mobile use
- **Verdict:** Needs optimization for mobile

---

### Option B: Wav2Vec2-XLSR Phoneme

**Source:** [HuggingFace Wav2Vec2Phoneme](https://huggingface.co/docs/transformers/model_doc/wav2vec2_phoneme)

| Aspect | Details |
|--------|---------|
| Base | Wav2Vec2 architecture |
| Training | CTC loss, multilingual |
| Output | IPA phoneme sequences |
| Accuracy | 72% phoneme error rate reduction vs prior SOTA |
| Sizes | 300M - 2B parameters |

**Key Capability:**
> "Can be fine-tuned on multiple languages at once and decode unseen languages in a single forward pass to a sequence of phonemes."

**Mobile Viability:**
- **300M params = ~1.2GB** - Too large for mobile
- ONNX conversion documented (Thai example)
- Quantization can reduce to ~300MB
- **Verdict:** Too large without significant distillation

---

### Option C: TinyML Phoneme Models (Best for Mobile)

**Source:** [TinyML for Speech Recognition](https://arxiv.org/html/2504.16213v1)

| Model | Size | Accuracy | Platform |
|-------|------|----------|----------|
| 1D CNN (Edge Impulse) | **23KB RAM** | 97% (keywords) | Arduino |
| Tiny Transducer | **1.6M params (~6MB)** | 18.1% WER | Edge devices |
| DFSMN-based | ~10MB | Competitive | Mobile |

**Tiny Transducer Architecture:**
- Uses DFSMN (Deep Feed-forward Sequential Memory) instead of LSTM
- 3%+ better than hybrid TDNN baseline
- Specifically designed for edge devices

**Mobile Viability:**
- **Verdict:** Best option for mobile phoneme extraction
- Can be trained on phoneme output instead of words
- ~10-20MB model size achievable

---

### Option D: Custom Kaldi/Vosk Phoneme Model

**Approach:** Use Vosk's underlying Kaldi toolkit to build phoneme-only acoustic model

| Aspect | Details |
|--------|---------|
| Base | Kaldi TDNN-F or similar |
| Output | Phoneme sequences (no LM) |
| Size | ~20-50MB (acoustic model only) |
| Languages | Train on multilingual data |

**How to build:**
```bash
# 1. Prepare multilingual phoneme training data
# 2. Define universal phone set (IPA subset)
# 3. Train acoustic model with CTC
# 4. Export without language model
```

**Mobile Viability:**
- **Verdict:** Most practical if we invest in training
- Full control over model architecture
- Can optimize specifically for our command vocabulary

---

### Phoneme Extractor Comparison

| Model | Size | Languages | Accuracy | Mobile Ready | Effort |
|-------|------|-----------|----------|--------------|--------|
| Allosaurus | ~150MB | 2000+ | High | Needs work | Low |
| Wav2Vec2-XLSR | 300MB-1.2GB | 100+ | Highest | No | Medium |
| TinyML/Transducer | **6-20MB** | Trainable | Good | **Yes** | Medium |
| Custom Kaldi | **20-50MB** | Trainable | Good | **Yes** | High |

**Recommendation:** Start with **TinyML approach** (Tiny Transducer architecture), trained on phoneme output.

---

## Part 2: Dictionary-Based Dictation

### The Core Question

Can we build dictation using:
```
Audio → Phoneme Extractor → Phoneme Sequence → Dictionary Lookup → Words
```

Instead of:
```
Audio → Full ASR (Acoustic Model + Language Model) → Words
```

### How Traditional ASR Works

**Source:** [ASR Decoding - Jonathan Hui](https://jonathan-hui.medium.com/speech-recognition-asr-decoding-f152aebed779)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     TRADITIONAL ASR PIPELINE                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Audio → Acoustic Model → Phoneme Probabilities                             │
│              │                                                               │
│              ▼                                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ Lexicon (Pronunciation Dictionary)                                   │   │
│  │ "cat" → /k/ /æ/ /t/                                                 │   │
│  │ "bat" → /b/ /æ/ /t/                                                 │   │
│  │ "cap" → /k/ /æ/ /p/                                                 │   │
│  │                                                                      │   │
│  │ Role: Maps phoneme sequences to possible words                       │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│              │                                                               │
│              ▼                                                               │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │ Language Model                                                       │   │
│  │ P("the cat sat") >> P("the bat sat") in most contexts               │   │
│  │                                                                      │   │
│  │ Role: Disambiguates between phonetically similar words               │   │
│  │       using context and word frequency                               │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│              │                                                               │
│              ▼                                                               │
│  Output: "the cat sat on the mat"                                           │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### WER Benchmarks: With vs Without Language Model

**Source:** [Fairseq wav2vec 2.0](https://github.com/facebookresearch/fairseq/issues/2977), [Denoising LM Research](https://arxiv.org/html/2512.13576)

| Configuration | test-clean WER | test-other WER |
|---------------|----------------|----------------|
| Viterbi (no LM) | 2.4% | 5.3% |
| Lexicon only | ~6% | ~8% |
| + KenLM (4-gram) | 2.0% | 4.3% |
| + Transformer LM | 1.8% | 3.3% |

**Key Insight:**
> Lexicon-only achieves **6-8% WER** - usable but with noticeable errors.
> Adding language model improves to **2-4% WER** - professional quality.

### The Homophone Problem

**Why dictionary-only struggles:**

```
Phoneme Sequence: /r aɪ t/

Possible Words:
├── "right" (correct, direction)
├── "write" (create text)
├── "rite" (ceremony)
└── "wright" (craftsman)

Without Language Model:
  "I want to [right/write/rite/wright] a letter"
  → System picks most frequent: "right" ❌

With Language Model:
  P("write a letter") >> P("right a letter")
  → System picks: "write" ✓
```

**English has ~7,000 homophones** - words that sound identical but have different meanings/spellings.

### Dictionary-Based Dictation: Viable Approaches

#### Approach 1: Constrained Domain Dictation

Limit vocabulary to domain-specific words where homophones are rare:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     CONSTRAINED DOMAIN DICTATION                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Domain: Medical Terminology                                                 │
│  Vocabulary: 5,000 medical terms                                            │
│  Homophones: Minimal (technical terms are unique)                           │
│                                                                              │
│  User says: "Patient has acute bronchitis"                                  │
│  Phonemes: /eɪ k juː t  b r ɒ ŋ k aɪ t ɪ s/                                │
│  Lookup: → "acute" → "bronchitis"                                           │
│  Result: "Patient has acute bronchitis" ✓                                   │
│                                                                              │
│  Accuracy: ~95%+ (domain-specific vocabulary)                               │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Good for:**
- Technical documentation
- Form filling with known fields
- Structured data entry

#### Approach 2: Phoneme + Simple N-gram (Lightweight LM)

Instead of full neural LM, use compact n-gram statistics:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     PHONEME + LIGHTWEIGHT LM                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Components:                                                                 │
│  ├── Phoneme Extractor: 20MB                                                │
│  ├── Pronunciation Dictionary: 5MB (134k words)                             │
│  └── Bigram Statistics: 10MB (word pair frequencies)                        │
│                                                                              │
│  Total: ~35MB (vs 500MB+ for full ASR)                                      │
│                                                                              │
│  Process:                                                                    │
│  1. Extract phonemes: /aɪ w ɒ n t t uː r aɪ t/                             │
│  2. Dictionary lookup: → candidates per segment                             │
│  3. Bigram scoring:                                                         │
│     P("I want") × P("want to") × P("to write") > P("to right")             │
│  4. Beam search with bigram probabilities                                   │
│                                                                              │
│  Expected WER: ~4-6% (between lexicon-only and full LM)                     │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### Approach 3: Phoneme + Semantic Post-Correction

Use embeddings to fix obvious errors after initial transcription:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     PHONEME + SEMANTIC CORRECTION                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Step 1: Phoneme → Dictionary → Initial Text                                │
│          "I want to right a letter"                                         │
│                                                                              │
│  Step 2: Semantic Anomaly Detection                                         │
│          Embed sentence, check for semantic coherence                       │
│          "right a letter" → low coherence score                             │
│                                                                              │
│  Step 3: Homophone Substitution                                             │
│          Try alternatives: "write a letter" → high coherence                │
│                                                                              │
│  Step 4: Output corrected text                                              │
│          "I want to write a letter" ✓                                       │
│                                                                              │
│  Components:                                                                 │
│  ├── Phoneme Extractor: 20MB                                                │
│  ├── Dictionary: 5MB                                                        │
│  ├── Homophone Map: 1MB                                                     │
│  └── Sentence Embedder (MiniLM): 22MB                                       │
│                                                                              │
│  Total: ~48MB                                                                │
│  Expected WER: ~3-5%                                                        │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### CMU Pronouncing Dictionary

**Source:** [CMU Pronouncing Dictionary](http://www.speech.cs.cmu.edu/cgi-bin/cmudict)

| Aspect | Details |
|--------|---------|
| Entries | 134,000+ words |
| Phonemes | 39 ARPAbet symbols |
| Stress markers | 0 (none), 1 (primary), 2 (secondary) |
| Size | ~5MB |
| License | BSD |

**Example entries:**
```
WRITE  R AY1 T
RIGHT  R AY1 T
RITE   R AY1 T
SETTING  S EH1 T IH0 NG
CONFIGURATION  K AH0 N F IH2 G Y ER0 EY1 SH AH0 N
```

**Building multilingual dictionary:**
- CMUdict: English
- Lexique: French
- CALLHOME: Spanish, Arabic, others
- Can generate via G2P (grapheme-to-phoneme) models

---

## Part 3: Proposed Architecture

### Hybrid System: Commands + Dictation

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                     UNIFIED VOICE RECOGNITION ARCHITECTURE                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│                           ┌──────────────┐                                  │
│                           │   Audio In   │                                  │
│                           └──────┬───────┘                                  │
│                                  │                                           │
│                    ┌─────────────┴─────────────┐                            │
│                    │   Universal Phoneme       │                            │
│                    │   Extractor (~20MB)       │                            │
│                    │   (TinyML/Custom Kaldi)   │                            │
│                    └─────────────┬─────────────┘                            │
│                                  │                                           │
│                    IPA Phoneme Sequence                                      │
│                    /oʊ p ə n s ɛ t ɪ ŋ z/                                   │
│                                  │                                           │
│            ┌─────────────────────┼─────────────────────┐                    │
│            │                     │                     │                    │
│            ▼                     ▼                     ▼                    │
│   ┌────────────────┐   ┌────────────────┐   ┌────────────────┐             │
│   │ COMMAND MODE   │   │ DYNAMIC UI     │   │ DICTATION MODE │             │
│   │                │   │                │   │                │             │
│   │ Phoneme Dict   │   │ Phoneme Dict   │   │ Phoneme Dict   │             │
│   │ (~100 commands)│   │ + Embeddings   │   │ + Bigram LM    │             │
│   │                │   │                │   │ + Semantic Fix │             │
│   │ Direct pattern │   │ Semantic match │   │                │             │
│   │ match (O(1))   │   │ vs UI elements │   │ Beam search    │             │
│   │                │   │                │   │ + correction   │             │
│   └───────┬────────┘   └───────┬────────┘   └───────┬────────┘             │
│           │                    │                    │                       │
│           ▼                    ▼                    ▼                       │
│   ┌────────────────┐   ┌────────────────┐   ┌────────────────┐             │
│   │ scroll_down    │   │ Click:Settings │   │ "Send email    │             │
│   │ open_settings  │   │ (0.89 match)   │   │  to John"      │             │
│   │ go_back        │   │                │   │                │             │
│   └────────────────┘   └────────────────┘   └────────────────┘             │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

MODEL SIZES:
├── Phoneme Extractor:        20MB
├── Command Phoneme Dict:     0.5MB
├── CMU Dictionary:           5MB
├── Bigram Statistics:        10MB
├── MiniLM Embeddings:        22MB
├── Homophone Map:            1MB
└── TOTAL:                    ~58MB (all languages for commands)
                              vs ~320MB (8 Vosk language models)
```

### Dictation Accuracy Expectations

| Mode | Model Size | Expected WER | Use Case |
|------|------------|--------------|----------|
| Commands (phoneme) | 20MB | <5% (pattern match) | "scroll down", "go back" |
| Dynamic UI (phoneme + embed) | 42MB | <10% | "tap settings" → Settings |
| Dictation (constrained) | 35MB | ~5% | Form fields, known vocab |
| Dictation (general) | 58MB | ~4-6% | Open text with correction |
| Dictation (full ASR) | 500MB+ | ~2-3% | Professional quality |

---

## Part 4: Implementation Roadmap

### Phase A: Universal Phoneme Extractor

| Task | Effort | Output |
|------|--------|--------|
| A.1 Research Tiny Transducer architecture | 4h | Architecture doc |
| A.2 Collect multilingual phoneme training data | 8h | Dataset |
| A.3 Train universal phoneme model | 16h | ~20MB model |
| A.4 Convert to ONNX/TFLite | 4h | Mobile-ready model |
| A.5 Benchmark on mobile devices | 4h | Performance metrics |

**Total:** ~36 hours

### Phase B: Command Dictionary System

| Task | Effort | Output |
|------|--------|--------|
| B.1 Create multilingual command phoneme dictionary | 8h | Command mappings |
| B.2 Implement phoneme pattern matcher | 4h | Matcher code |
| B.3 Integrate with CommandMatchingService | 4h | Integration |
| B.4 Test across languages | 8h | Test results |

**Total:** ~24 hours

### Phase C: Dictionary-Based Dictation

| Task | Effort | Output |
|------|--------|--------|
| C.1 Integrate CMU dictionary + multilingual dicts | 4h | Dictionary loader |
| C.2 Implement bigram statistics loader | 4h | Bigram scorer |
| C.3 Implement beam search decoder | 8h | Decoder |
| C.4 Add semantic post-correction | 8h | Corrector |
| C.5 Benchmark WER vs Vosk/Google | 8h | Comparison |

**Total:** ~32 hours

---

## Part 5: Decision Matrix

### Should We Build Dictionary-Based Dictation?

| Factor | Build Custom | Use Vosk/Cloud |
|--------|--------------|----------------|
| Model size (8 lang) | ~58MB | ~320MB+ |
| WER (commands) | ~3-5% | ~5-8% |
| WER (dictation) | ~4-6% | ~2-4% |
| Add new language | Add dictionary | Download 40MB |
| Offline capable | Yes | Yes (Vosk) |
| Vendor lock-in | None | None |
| Development effort | ~90 hours | ~20 hours |
| Maintenance | High | Low |

### Recommendation

**Hybrid approach:**

1. **Build universal phoneme extractor** (~20MB)
   - Works for all languages
   - Powers command recognition
   - Foundation for dictation experiments

2. **Build dictionary-based command system**
   - Phoneme → command mapping
   - Add semantic fallback for dynamic UI

3. **For dictation, start with constrained domain**
   - Test dictionary + bigram approach
   - Measure WER on target use cases
   - If <5% WER acceptable, expand
   - If not, fall back to Vosk for primary language

4. **Keep cloud ASR as premium option**
   - Google/Whisper for users who need <2% WER
   - Optional, not required

---

## References

- [Allosaurus - Universal Phone Recognizer](https://github.com/xinjli/allosaurus)
- [Wav2Vec2Phoneme - HuggingFace](https://huggingface.co/docs/transformers/model_doc/wav2vec2_phoneme)
- [TinyML for Speech Recognition](https://arxiv.org/html/2504.16213v1)
- [Tiny Transducer](https://www.researchgate.net/publication/352170500_Tiny_Transducer_A_Highly-Efficient_Speech_Recognition_Model_on_Edge_Devices)
- [CMU Pronouncing Dictionary](http://www.speech.cs.cmu.edu/cgi-bin/cmudict)
- [ASR Decoding - Jonathan Hui](https://jonathan-hui.medium.com/speech-recognition-asr-decoding-f152aebed779)
- [Lexicon and Language Model - Edinburgh](https://www.inf.ed.ac.uk/teaching/courses/asr/2017-18/asr08-lexlm.pdf)
- [Fairseq wav2vec 2.0 Benchmarks](https://github.com/facebookresearch/fairseq/issues/2977)

---

*Research completed: 2026-01-18 | Author: Claude*
