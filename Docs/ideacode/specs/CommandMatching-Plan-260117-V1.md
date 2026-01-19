# Command Matching Service - Implementation Plan

**Date:** 2026-01-17 | **Version:** V1 | **Spec:** CommandMatching-Spec-260117-V1.md

---

## Implementation Phases

### Phase 1: Complete Core Service ✅ (Done)
Files created:
- `Modules/AI/NLU/src/commonMain/kotlin/com/augmentalis/nlu/matching/CommandMatchingService.kt`
- `Modules/AI/NLU/src/commonMain/kotlin/com/augmentalis/nlu/matching/MultilingualSupport.kt`
- `Modules/AI/NLU/src/androidMain/kotlin/com/augmentalis/nlu/matching/PlatformUtils.android.kt`
- `Modules/AI/NLU/src/desktopMain/kotlin/com/augmentalis/nlu/matching/PlatformUtils.desktop.kt`
- `Modules/AI/NLU/src/iosMain/kotlin/com/augmentalis/nlu/matching/PlatformUtils.ios.kt`
- `Modules/AI/NLU/src/jsMain/kotlin/com/augmentalis/nlu/matching/PlatformUtils.js.kt`

---

### Phase 2: SpeechRecognition Integration

#### Why Direct Integration (Not a Bridge)?

**A "bridge" pattern would be over-engineering because:**

1. **Same language**: Both NLU and SpeechRecognition are Kotlin
2. **KMP compatibility**: NLU has `androidMain` for Android-specific code
3. **Simple dependency**: SpeechRecognition can depend on NLU directly
4. **No translation needed**: MatchResult can be used directly or with simple extension

**What we actually need:**

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     BEFORE (Current Architecture)                        │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  SpeechRecognition Module                                                │
│  ├── ResultProcessor.kt ───────────▶ CommandCache.kt (EXACT ONLY)       │
│  │                                                                       │
│  └── SimilarityMatcher.kt ◀──────── NOT WIRED!                          │
│                                                                          │
│  NLU Module (separate, unused by speech)                                │
│  ├── PatternMatcher.kt                                                  │
│  ├── FuzzyMatcher.kt                                                    │
│  └── SemanticMatcher.kt                                                 │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                     AFTER (Integrated Architecture)                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  NLU Module (KMP - shared)                                              │
│  └── CommandMatchingService.kt ◀──── Single source of truth            │
│      ├── Uses PatternMatcher                                            │
│      ├── Uses FuzzyMatcher                                              │
│      ├── Uses SemanticMatcher                                           │
│      └── Uses MultilingualNormalizer                                    │
│                                                                          │
│  SpeechRecognition Module (Android)                                      │
│  └── ResultProcessor.kt ────────────▶ CommandMatchingService            │
│      └── Calls matcher.match() directly                                 │
│                                                                          │
│  VoiceOSCoreNG Module (KMP)                                             │
│  └── CommandProcessor.kt ───────────▶ CommandMatchingService            │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

#### Tasks

| # | Task | File | Effort |
|---|------|------|--------|
| 2.1 | Add NLU dependency to SpeechRecognition build.gradle.kts | `Modules/SpeechRecognition/build.gradle.kts` | 5 min |
| 2.2 | Update ResultProcessor constructor to accept CommandMatchingService | `ResultProcessor.kt` | 15 min |
| 2.3 | Add fuzzy fallback to processResult() | `ResultProcessor.kt` | 30 min |
| 2.4 | Sync CommandCache commands to CommandMatchingService | `ResultProcessor.kt` | 15 min |
| 2.5 | Add integration tests | `ResultProcessorIntegrationTest.kt` | 30 min |
| 2.6 | Deprecate standalone SimilarityMatcher (keep for backward compat) | `SimilarityMatcher.kt` | 5 min |

**Total Effort:** ~1.5 hours

#### Implementation Details

**2.2 - Update ResultProcessor Constructor:**

```kotlin
// ResultProcessor.kt - BEFORE
class ResultProcessor(
    private val commandCache: CommandCache = CommandCache()
) {

// ResultProcessor.kt - AFTER
class ResultProcessor(
    private val commandCache: CommandCache = CommandCache(),
    private val commandMatcher: CommandMatchingService? = null
) {
    init {
        // Sync existing commands to matcher
        commandMatcher?.let { matcher ->
            val allCommands = commandCache.getAllCommands()
            if (allCommands.isNotEmpty()) {
                matcher.registerCommands(allCommands)
            }
        }
    }
```

**2.3 - Add Fuzzy Fallback:**

```kotlin
// In processResult(), after exact match fails:

// Process based on mode
val finalText = when (currentMode) {
    SpeechMode.STATIC_COMMAND,
    SpeechMode.DYNAMIC_COMMAND -> {
        // Try exact match first
        commandCache.findMatch(normalizedText)
            ?: commandMatcher?.let { matcher ->
                // Fallback to fuzzy/semantic matching
                when (val result = matcher.match(normalizedText)) {
                    is MatchResult.Exact -> result.command
                    is MatchResult.Fuzzy -> {
                        if (result.confidence >= confidenceThreshold) {
                            result.command
                        } else {
                            normalizedText  // Below threshold, return original
                        }
                    }
                    is MatchResult.Ambiguous -> {
                        // Could emit ambiguous event for UI handling
                        result.candidates.firstOrNull()?.command ?: normalizedText
                    }
                    is MatchResult.NoMatch -> normalizedText
                }
            }
            ?: normalizedText  // No matcher, return original
    }
    // ... rest of modes
}
```

---

### Phase 3: VoiceOSCoreNG Integration

#### Tasks

| # | Task | File | Effort |
|---|------|------|--------|
| 3.1 | Add NLU dependency to Voice/Core build.gradle.kts | `Modules/Voice/Core/build.gradle.kts` | 5 min |
| 3.2 | Refactor CommandMatcher to use CommandMatchingService | `CommandMatcher.kt` | 45 min |
| 3.3 | Migrate synonym handling to LocalizedSynonymProvider | `CommandMatcher.kt` | 30 min |
| 3.4 | Update CommandMatcher tests | `CommandMatcherTest.kt` | 30 min |

**Total Effort:** ~2 hours

#### Implementation Options

**Option A: Wrapper (recommended)**
Keep existing `CommandMatcher` API, internally use `CommandMatchingService`:

```kotlin
object CommandMatcher {
    private val service = CommandMatchingService()

    var synonymProvider: ISynonymProvider? = null
        set(value) {
            field = value
            // Sync synonyms to service
            value?.getAllSynonyms()?.let { service.setSynonyms(it) }
        }

    fun match(voiceInput: String, registry: CommandRegistry, threshold: Float = 0.7f): MatchResult {
        // Register commands from registry
        service.registerCommands(registry.all().map { it.phrase })

        // Use service for matching
        return when (val result = service.match(voiceInput)) {
            is com.augmentalis.nlu.matching.MatchResult.Exact ->
                MatchResult.Exact(registry.find(result.command)!!)
            is com.augmentalis.nlu.matching.MatchResult.Fuzzy ->
                MatchResult.Fuzzy(registry.find(result.command)!!, result.confidence)
            // ... etc
        }
    }
}
```

**Option B: Direct replacement**
Update all callers to use `CommandMatchingService` directly.

Recommendation: **Option A** - maintains backward compatibility.

---

### Phase 4: HybridClassifier Alignment

#### Tasks

| # | Task | File | Effort |
|---|------|------|--------|
| 4.1 | Assess HybridClassifier vs CommandMatchingService overlap | Analysis | 30 min |
| 4.2 | Consolidate or delegate text matching | `HybridClassifier.kt` | 1 hour |
| 4.3 | Keep HybridClassifier for intent-level classification | N/A | N/A |

**Decision:** HybridClassifier focuses on **intent classification** (higher level), CommandMatchingService focuses on **command matching** (lower level). They can coexist, with HybridClassifier optionally using CommandMatchingService for its text matching stage.

---

### Phase 5: Phoneme-Based Matching (Future)

#### Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    PHONEME MATCHING PIPELINE                             │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  Audio Input                                                             │
│       │                                                                  │
│       ▼                                                                  │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ Phoneme Extractor (Platform-specific)                           │    │
│  │ - Android: CMUSphinx / Vosk phoneme mode / TensorFlow Lite     │    │
│  │ - iOS: Speech framework phoneme output                          │    │
│  │ - Desktop: Vosk / DeepSpeech phoneme mode                      │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│       │                                                                  │
│       ▼                                                                  │
│  Phoneme Sequence: ["G", "OW", "B", "AE", "K"]                          │
│       │                                                                  │
│       ▼                                                                  │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ Phoneme Command Dictionary                                       │    │
│  │                                                                  │    │
│  │ "G OW B AE K"      → go_back                                    │    │
│  │ "S K R OW L AH P"  → scroll_up                                  │    │
│  │ "S K R OW L D AW N" → scroll_down                               │    │
│  │ "N EH K S T"       → next                                       │    │
│  │ "K AE N S AH L"    → cancel                                     │    │
│  │                                                                  │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│       │                                                                  │
│       ▼                                                                  │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ Phoneme Matcher                                                  │    │
│  │ - Weighted edit distance (similar phonemes cost less)           │    │
│  │ - Phoneme confusion matrix                                       │    │
│  │ - Handles: P/B, T/D, K/G, S/Z, F/V confusion                    │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│       │                                                                  │
│       ▼                                                                  │
│  PhonemeMatchResult: (command_id, confidence, phoneme_sequence)         │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

#### Phoneme Confusion Matrix

Similar-sounding phonemes should have lower substitution cost:

| Phoneme | Confused With | Cost |
|---------|---------------|------|
| P | B | 0.3 |
| T | D | 0.3 |
| K | G | 0.3 |
| S | Z | 0.3 |
| F | V | 0.3 |
| M | N | 0.4 |
| IY | IH | 0.4 |
| EY | EH | 0.4 |
| Different | Different | 1.0 |

#### Tasks

| # | Task | File | Effort |
|---|------|------|--------|
| 5.1 | Define PhonemeExtractor interface | `PhonemeMatching.kt` | 30 min |
| 5.2 | Create phoneme command dictionary structure | `PhonemeMatching.kt` | 1 hour |
| 5.3 | Implement weighted phoneme edit distance | `PhonemeMatching.kt` | 2 hours |
| 5.4 | Create phoneme confusion matrix | `PhonemeMatching.kt` | 1 hour |
| 5.5 | Android implementation (Vosk phoneme mode) | `PlatformUtils.android.kt` | 4 hours |
| 5.6 | iOS implementation | `PlatformUtils.ios.kt` | 4 hours |
| 5.7 | Integration with CommandMatchingService | `CommandMatchingService.kt` | 2 hours |
| 5.8 | Create IPA dictionaries for common commands | `resources/` | 4 hours |

**Total Effort:** ~18-20 hours

---

## Implementation Order

```
Phase 1 ──▶ Phase 2 ──▶ Phase 3 ──▶ Phase 4 ──▶ Phase 5
  ✅         Next        Later      Optional    Future
  Done
```

### Recommended Next Steps

1. **Complete Phase 2** (SpeechRecognition integration) - biggest impact, smallest effort
2. **Test in production** - validate fuzzy matching improves user experience
3. **Phase 3** if VoiceOSCoreNG needs unified matching
4. **Phase 5** for latency-critical fixed commands

---

## Dependency Graph

```
                    ┌─────────────────┐
                    │   NLU Module    │
                    │ (KMP - shared)  │
                    └────────┬────────┘
                             │
                    depends on NLU
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
         ▼                   ▼                   ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│ SpeechRecognition│ │  VoiceOSCoreNG  │ │  Other Modules  │
│   (Android)      │ │     (KMP)       │ │                 │
└─────────────────┘ └─────────────────┘ └─────────────────┘
```

---

## Gradle Dependencies

### NLU build.gradle.kts (already has matchers)
No changes needed.

### SpeechRecognition build.gradle.kts
```kotlin
dependencies {
    implementation(project(":Modules:AI:NLU"))
    // ... existing deps
}
```

### Voice/Core build.gradle.kts
```kotlin
kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":Modules:AI:NLU"))
                // ... existing deps
            }
        }
    }
}
```

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Breaking existing behavior | Medium | High | Keep CommandCache as primary, matcher as fallback |
| Performance regression | Low | Medium | Profile fuzzy matching, add timeout |
| Multilingual edge cases | Medium | Medium | Extensive testing per locale |
| Phoneme extraction quality | Medium | High | Start with limited command set, expand gradually |

---

## Success Metrics

| Metric | Current | Target |
|--------|---------|--------|
| Exact match rate | ~60% | ~40% (more handled by fuzzy) |
| Fuzzy match rate | 0% | ~40% |
| Command recognition accuracy | ~60% | ~90%+ |
| Average matching latency | <5ms | <50ms (with fuzzy) |
| User corrections needed | High | -50% reduction |

---

*Plan created: 2026-01-17 | Author: Claude*
