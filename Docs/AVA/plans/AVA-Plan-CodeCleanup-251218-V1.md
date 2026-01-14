# Implementation Plan: AVA Codebase Cleanup & Consolidation

**Created:** 2025-12-18
**Updated:** 2025-12-18 (corrected module structure analysis)
**Mode:** .yolo .swarm .tasks .implement .cot .tot .rot
**Estimated Tasks:** 15 | **Estimated Time:** Sequential: 8h | Parallel (Swarm): 3h
**Swarm Recommended:** YES (complex multi-module impact)

---

## Executive Summary

This plan addresses issues identified in the comprehensive code review:
- **LLM Module:** NOT KMP - remove unused commonMain/androidMain/iosMain source sets ✅ COMPLETED
- **NLU Module:** IS KMP - expect/actual pattern is CORRECT (no changes needed)
- **16+ TODOs/FIXMEs** requiring implementation
- **Namespace redundancies** in file naming (DEFERRED - risk outweighs benefit)
- **SOLID violations** in ChatViewModel (2,292 lines)

---

## CRITICAL DISCOVERY

### Module Structure Analysis

| Module | Build Type | Source Sets | Duplicates? |
|--------|-----------|-------------|-------------|
| **LLM** | Android Library | main/, test/, androidTest/ | commonMain was SPURIOUS - removed |
| **NLU** | KMP | commonMain/, androidMain/, iosMain/, desktopMain/ | CORRECT expect/actual |
| **Chat** | KMP | commonMain/, main/ | Intentional (interfaces vs impl) |

### LLM Module Correction
The LLM module's `build.gradle.kts` uses `android.library` plugin, NOT KMP.
- **Problem:** Someone added commonMain/androidMain/iosMain folders that weren't compiled
- **Solution:** Deleted unused KMP source sets, keeping only main/test/androidTest
- **Status:** ✅ COMPLETED - build passes

---

## REASONING (CoT/ToT/RoT)

### Chain of Thought (CoT)
1. First verify module type (KMP vs Android-only) before assuming duplicates
2. LLM is Android-only → commonMain was never compiled → delete it
3. NLU is KMP → expect/actual is correct pattern → no changes
4. Namespace redundancy renaming has high risk for low benefit → DEFER

### Tree of Thought (ToT) - Approaches Re-evaluated

| Approach | Pros | Cons | Selected |
|----------|------|------|----------|
| A: Delete commonMain from LLM | Removes confusion, build works | None | **YES** ✅ |
| B: Convert LLM to KMP | Clean architecture | Major refactor, not needed | No |
| C: Keep unused source sets | None | Confusion, maintenance burden | No |

### Reflection on Thought (RoT)
- **Learning:** Always verify build.gradle.kts before assuming KMP structure
- **Risk:** Unused source sets cause confusion and may diverge from main/
- **Validation:** Build passed after removing commonMain from LLM

---

## PHASE 1: LLM Cleanup ✅ COMPLETED

| Task | Status | Action |
|------|--------|--------|
| 1.1 | ✅ | Deleted Modules/AVA/LLM/src/commonMain/ (20 unused files) |
| 1.2 | ✅ | Deleted Modules/AVA/LLM/src/androidMain/ (1 unused file) |
| 1.3 | ✅ | Deleted Modules/AVA/LLM/src/iosMain/ (created by mistake) |
| 1.4 | ✅ | Deleted Modules/AVA/LLM/src/desktopMain/ (created by mistake) |
| 1.5 | ✅ | Verified LLM module compiles |

**Lines Removed:** ~3,500 (unused, never compiled)

---

## REMAINING PHASES

### Phase 2: TODO/FIXME Implementation (High Priority)

| Task | File | TODO | Priority |
|------|-------------|-------------------|------------|--------|
| IntentTemplates.kt | 114 | 114 | YES | DELETE main/, keep commonMain/ |
| TemplateResponseGenerator.kt | 108 | 118 | 91% | MERGE to commonMain/ + expect/actual |
| ResponseGenerator.kt | 158 | 158 | YES | DELETE main/, keep commonMain/ |
| LLMContextBuilder.kt | 189 | 189 | YES | DELETE main/, keep commonMain/ |
| LanguageDetector.kt | 351 | 353 | 99% | MERGE to commonMain/ |
| LatencyMetrics.kt | 236 | 292 | 81% | MERGE best of both |
| ChatMessage.kt | 79 | 81 | 98% | DELETE main/, keep commonMain/ |
| LLMResponse.kt | 105 | 105 | YES | DELETE main/, keep commonMain/ |
| LLMProvider.kt | 371 | 372 | 99% | DELETE main/, keep commonMain/ |
| DownloadState.kt | 235 | 235 | YES | DELETE main/, keep commonMain/ |
| Models.kt | 224 | 224 | YES | DELETE main/, keep commonMain/ |
| TopPSampler.kt | 80 | 80 | YES | DELETE main/, keep commonMain/ |
| StopTokenDetector.kt | 283 | 285 | 99% | DELETE main/, keep commonMain/ |
| TokenSampler.kt | 289 | 291 | 99% | DELETE main/, keep commonMain/ |
| IInferenceStrategy.kt | 53 | 53 | YES | DELETE main/, keep commonMain/ |
| IMemoryManager.kt | 60 | 60 | YES | DELETE main/, keep commonMain/ |
| IModelLoader.kt | 52 | 52 | YES | DELETE main/, keep commonMain/ |
| ISamplerStrategy.kt | 47 | 47 | YES | DELETE main/, keep commonMain/ |
| IStreamingManager.kt | 47 | 47 | YES | DELETE main/, keep commonMain/ |
| ImprovedIntentTemplates.kt | 231 | 232 | 99% | DELETE main/, keep commonMain/ |

**Total Redundant Lines:** ~3,462

### NLU Module (4 duplicates - expect/actual pattern)

| File | androidMain/ Lines | commonMain/ Lines | Pattern | Action |
|------|-------------------|-------------------|---------|--------|
| BertTokenizer.kt | 334 | 64 | expect/actual | CORRECT - keep both |
| IntentClassifier.kt | 1086 | 72 | expect/actual | CORRECT - keep both |
| ModelManager.kt | 690 | 59 | expect/actual | CORRECT - keep both |
| LocaleManager.kt | 187 | 77 | expect/actual | CORRECT - keep both |

**Status:** NLU duplicates are CORRECT KMP expect/actual pattern ✓

---

## NAMESPACE REDUNDANCY REPORT

### Redundant File Names (name restates folder)

| Current Path | Issue | Rename To | Rationale |
|--------------|-------|-----------|-----------|
| `tts/TTSManager.kt` | "TTS" prefix in tts/ folder | `tts/Manager.kt` | Folder provides context |
| `tts/TTSPreferences.kt` | "TTS" prefix in tts/ folder | `tts/Preferences.kt` | Folder provides context |
| `tts/TTSViewModel.kt` | "TTS" prefix in tts/ folder | `tts/ViewModel.kt` | Folder provides context |
| `coordinator/TTSCoordinator.kt` | "Coordinator" suffix in coordinator/ | `coordinator/TTS.kt` | Folder provides context |
| `coordinator/NLUCoordinator.kt` | "Coordinator" suffix | `coordinator/NLU.kt` | Folder provides context |
| `coordinator/RAGCoordinator.kt` | "Coordinator" suffix | `coordinator/RAG.kt` | Folder provides context |
| `coordinator/ActionCoordinator.kt` | "Coordinator" suffix | `coordinator/Action.kt` | Folder provides context |
| `coordinator/ResponseCoordinator.kt` | "Coordinator" suffix | `coordinator/Response.kt` | Folder provides context |
| `download/ModelDownloadManager.kt` | "Download" in download/ folder | `download/Manager.kt` | Folder provides context |
| `download/ModelDownloadConfig.kt` | "Download" in download/ folder | `download/Config.kt` | Folder provides context |
| `download/LLMDownloadWorker.kt` | "Download" in download/ folder | `download/Worker.kt` | Folder provides context |
| `download/LLMModelDownloader.kt` | "Model" and "Downloader" redundant | `download/LLMDownloader.kt` | Remove "Model" |
| `learning/IntentLearningManager.kt` | "Learning" in learning/ folder | `learning/Manager.kt` | Folder provides context |
| `response/ResponseGenerator.kt` | "Response" in response/ folder | `response/Generator.kt` | Folder provides context |
| `response/LLMResponseGenerator.kt` | "Response" in response/ folder | `response/LLMGenerator.kt` | Keep LLM prefix for distinction |
| `response/HybridResponseGenerator.kt` | "Response" in response/ folder | `response/HybridGenerator.kt` | Folder provides context |
| `response/TemplateResponseGenerator.kt` | "Response" in response/ folder | `response/TemplateGenerator.kt` | Folder provides context |

### Decision: DEFER Namespace Renames

**Reasoning:** While redundant naming is suboptimal, renaming these files:
1. Breaks external imports and DI modules
2. Requires extensive refactoring across 50+ files
3. Risk outweighs benefit for stable code

**Recommendation:** Apply naming convention to NEW files only. Existing files remain unchanged.

---

## IMPLEMENTATION PHASES

### Phase 1: LLM Duplicate Consolidation (Critical)
**Agent:** llm-consolidation
**Effort:** 4 hours (parallel: 1.5h)

| Task | Files | Action |
|------|-------|--------|
| 1.1 | Delete 14 identical main/ files | Remove, update imports |
| 1.2 | Merge TemplateResponseGenerator.kt | Add expect/actual TimeProvider |
| 1.3 | Merge LatencyMetrics.kt | Take best logging from both |
| 1.4 | Update all imports in LLM module | Point to commonMain packages |
| 1.5 | Run tests, fix breakages | Verify build passes |

### Phase 2: TODO/FIXME Implementation (High Priority)
**Agent:** todo-implementation
**Effort:** 6 hours (parallel: 2h)

| Task | File | TODO | Action |
|------|------|------|--------|
| 2.1 | ChecksumHelper.kt | Generate checksums | Implement SHA256 verification |
| 2.2 | ChatViewModel.kt | Speech recognition trigger | Wire to VoiceOS |
| 2.3 | ChatViewModel.kt | Model download UI prompt | Add download flow |
| 2.4 | NLUCoordinator.kt | Load intents from config | Create built-in-intents.json |
| 2.5 | MLCInferenceStrategy.kt | Runtime availability check | Implement actual check |
| 2.6 | HybridResponseGenerator.kt | Cloud LLM provider | Implement CloudLLMProvider |
| 2.7 | LocalLLMProvider.kt | Hot-swapping support | Add model swap API |
| 2.8 | ModelDownloader.kt | Format parameter | Add GGUF/LiteRT format |
| 2.9 | LLMResponseGenerator.kt | Context truncation | Implement sliding window |
| 2.10 | LatencyMetrics.kt | Platform logging | Use expect/actual Logger |

### Phase 3: ChatViewModel Refactoring (SOLID Compliance)
**Agent:** viewmodel-refactor
**Effort:** 4 hours (parallel: 1.5h)

| Task | Action |
|------|--------|
| 3.1 | Extract 20 dependencies → 5 coordinator facades |
| 3.2 | Move NLU logic to NLUCoordinator |
| 3.3 | Move RAG logic to RAGCoordinator |
| 3.4 | Move Action logic to ActionCoordinator |
| 3.5 | Move Response logic to ResponseCoordinator |
| 3.6 | Move TTS logic to TTSCoordinator |
| 3.7 | Reduce ChatViewModel to <500 lines |

### Phase 4: Logging Standardization
**Agent:** logging-standardization
**Effort:** 2 hours (parallel: 0.5h)

| Task | Action |
|------|--------|
| 4.1 | Create expect/actual Platform.Logger |
| 4.2 | Replace Timber usages in LLM module |
| 4.3 | Replace android.util.Log in Chat module |
| 4.4 | Add log level configuration |

---

## TIME ESTIMATES

| Phase | Sequential | Parallel (Swarm) |
|-------|------------|------------------|
| Phase 1: LLM Consolidation | 4h | 1.5h |
| Phase 2: TODO Implementation | 6h | 2h |
| Phase 3: ViewModel Refactor | 4h | 1.5h |
| Phase 4: Logging | 2h | 0.5h |
| **TOTAL** | **16h** | **5.5h** |

**Savings with Swarm:** 10.5 hours (66%)

---

## SWARM CONFIGURATION

```yaml
swarm:
  agents: 4
  parallel_phases: [1, 2]
  sequential_phases: [3, 4]

agent_assignments:
  llm-consolidation:
    phase: 1
    files: Modules/AVA/LLM/src/**

  todo-implementation:
    phase: 2
    files: [ChecksumHelper.kt, ChatViewModel.kt, NLUCoordinator.kt, MLCInferenceStrategy.kt]

  viewmodel-refactor:
    phase: 3
    files: [ChatViewModel.kt, *Coordinator.kt]

  logging-standardization:
    phase: 4
    files: [**/*.kt with Timber/Log imports]
```

---

## SUCCESS CRITERIA

| Metric | Before | Target |
|--------|--------|--------|
| Duplicate files | 20 | 0 |
| Redundant lines | 3,462 | 0 |
| ChatViewModel lines | 2,292 | <500 |
| TODOs implemented | 0/16 | 10/16 |
| Build status | ✓ | ✓ |
| Test status | ✓ | ✓ |

---

## RISK MITIGATION

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Import breakage | High | Medium | Run full build after each deletion |
| Test failures | Medium | Medium | Run tests per-phase |
| DI module updates needed | High | Low | Update Hilt modules simultaneously |
| Merge conflicts | Low | High | Work on feature branch, rebase often |

---

## EXECUTION ORDER

1. **Create backup branch:** `git checkout -b backup/pre-cleanup`
2. **Execute Phase 1:** LLM consolidation (most impactful)
3. **Execute Phase 3:** ViewModel refactor (enables Phase 2)
4. **Execute Phase 2:** TODO implementation
5. **Execute Phase 4:** Logging standardization
6. **Final validation:** Full build + test suite
7. **Merge to feature branch**

---

## APPENDIX: Files to Delete (Phase 1)

```
Modules/AVA/LLM/src/main/java/com/augmentalis/llm/response/IntentTemplates.kt
Modules/AVA/LLM/src/main/java/com/augmentalis/llm/response/ResponseGenerator.kt
Modules/AVA/LLM/src/main/java/com/augmentalis/llm/response/LLMContextBuilder.kt
Modules/AVA/LLM/src/main/java/com/augmentalis/llm/domain/ChatMessage.kt
Modules/AVA/LLM/src/main/java/com/augmentalis/llm/domain/LLMResponse.kt
Modules/AVA/LLM/src/main/java/com/augmentalis/llm/domain/LLMProvider.kt
Modules/AVA/LLM/src/main/java/com/augmentalis/llm/download/DownloadState.kt
Modules/AVA/LLM/src/main/java/com/augmentalis/llm/alc/models/Models.kt
Modules/AVA/LLM/src/main/java/com/augmentalis/llm/alc/samplers/TopPSampler.kt
Modules/AVA/LLM/src/main/java/com/augmentalis/llm/alc/StopTokenDetector.kt
Modules/AVA/LLM/src/main/java/com/augmentalis/llm/alc/TokenSampler.kt
Modules/AVA/LLM/src/main/java/com/augmentalis/llm/alc/interfaces/IInferenceStrategy.kt
Modules/AVA/LLM/src/main/java/com/augmentalis/llm/alc/interfaces/IMemoryManager.kt
Modules/AVA/LLM/src/main/java/com/augmentalis/llm/alc/interfaces/IModelLoader.kt
Modules/AVA/LLM/src/main/java/com/augmentalis/llm/alc/interfaces/ISamplerStrategy.kt
Modules/AVA/LLM/src/main/java/com/augmentalis/llm/alc/interfaces/IStreamingManager.kt
Modules/AVA/LLM/src/main/java/com/augmentalis/llm/response/ImprovedIntentTemplates.kt
```

**Files to Merge (require expect/actual):**
```
Modules/AVA/LLM/src/main/java/com/augmentalis/llm/response/TemplateResponseGenerator.kt
Modules/AVA/LLM/src/main/java/com/augmentalis/llm/LanguageDetector.kt
Modules/AVA/LLM/src/main/java/com/augmentalis/llm/metrics/LatencyMetrics.kt
```

---

**Plan Version:** 1.0
**Author:** Code Review Agent
**Approved:** Pending user confirmation
