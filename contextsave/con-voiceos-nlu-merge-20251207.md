# Context Report: VoiceOS-NLU Merge

**Date:** 2025-12-07
**Branch:** `VoiceOS-NLUMerge`
**Last Commit:** `25db3aa0` - fix: resolve all build warnings in VoiceOS

---

## Project Overview

NewAvanues monorepo containing:
- **VoiceOS** - Voice control accessibility service
- **AVA** - AI assistant actions
- **WebAvanue** - Web-based interface

---

## Current Work: NLU Unification

### Objective
Merge duplicate NLU (Natural Language Understanding) systems from VoiceOS and AVA into a unified KMP (Kotlin Multiplatform) module.

### Completed

| Task | Status | Commit |
|------|--------|--------|
| Create `Modules/Shared/NLU` KMP module | Done | `db7e251a` |
| Implement AVU format parser | Done | `db7e251a` |
| Create UnifiedIntent model | Done | `db7e251a` |
| Implement HybridIntentClassifier | Done | `db7e251a` |
| Fix ActionFactory placeholders | Done | `5f4ae050` |
| Fix NLU build.gradle.kts (version catalog issue) | Done | `5f4ae050` |
| Resolve all VoiceOS build warnings | Done | `25db3aa0` |
| Wire UnifiedNluService into VoiceOS CommandManager | Done | pending commit |
| Wire UnifiedNluService into AVA NLU module | Done | pending commit |

### Build Status
- **VoiceOS:** Builds successfully with zero warnings
- **AVA NLU:** Builds successfully with Unified NLU integration
- **APK:** `android/apps/VoiceOS/app/build/outputs/apk/debug/app-debug.apk`

---

## Key Files Created/Modified

### New NLU Module (`Modules/Shared/NLU/`)
```
src/commonMain/kotlin/com/augmentalis/shared/nlu/
├── classifier/HybridIntentClassifier.kt    # Pattern + semantic classification
├── matcher/PatternMatcher.kt               # Regex/pattern matching
├── matcher/FuzzyMatcher.kt                 # Fuzzy string matching
├── matcher/SemanticMatcher.kt              # Embedding-based matching
├── model/UnifiedIntent.kt                  # Cross-platform intent model
├── parser/AvuIntentParser.kt               # AVU format parser
├── repository/IntentRepository.kt          # Intent storage interface
├── service/UnifiedNluService.kt            # Main NLU service

src/androidMain/kotlin/com/augmentalis/shared/nlu/
├── embedding/OnnxEmbeddingProvider.kt      # ONNX embedding for Android
├── repository/AndroidIntentRepository.kt   # SQLDelight implementation
```

### VoiceOS Integration (NEW)
```
Modules/VoiceOS/managers/CommandManager/.../nlu/
├── NluIntegration.kt                       # Bridges UnifiedNluService with CommandManager
                                            # - Singleton pattern with lazy init
                                            # - Intent-to-Command conversion
                                            # - VoiceOS command loading into NLU

Modules/VoiceOS/managers/CommandManager/CommandManager.kt
  - Added NluIntegration instance
  - Updated executeCommandInternal() to try NLU first
  - Added initializeNlu() during initialization
  - Added NLU control API (setNluEnabled, isNluReady, nlu())
```

### AVA Integration (NEW)
```
Modules/AVA/NLU/.../migration/
├── UnifiedNluBridge.kt                     # Bridges AVA IntentClassifier with UnifiedNluService
                                            # - Hybrid classification (pattern + BERT)
                                            # - Fast path for high-confidence pattern matches
                                            # - Sync intents between AVA and shared database

Modules/AVA/NLU/build.gradle.kts
  - Added Shared:NLU dependency
```

### Modified Files
```
Modules/VoiceOS/managers/CommandManager/.../ActionFactory.kt
  - Fixed DynamicBrowserAction (findAccessibilityNodeInfosByText)
  - Fixed DynamicOverlayAction (intent property assignment)
  - Fixed DynamicPositionAction (ACTION_SHOW_ON_SCREEN.id)

Modules/VoiceOS/apps/VoiceOSCore/.../VoiceOSService.kt
  - Removed 6 unnecessary safe calls (speechEngineManager?. → speechEngineManager.)
  - Added @Suppress("DEPRECATION") for recycle() calls
  - Added @Suppress("UNCHECKED_CAST") for JSON parsing

android/apps/VoiceOS/gradlew
  - Added JAVA_HOME=jdk-17 at script start (fixes Java 22+ warnings)

android/apps/VoiceOS/gradle.properties
  - Added JVM args for module access

Modules/Shared/NLU/build.gradle.kts
  - Changed SQLDelight version from 2.0.2 to 2.0.1 (aligned with AVA)

Modules/Shared/NLU/.../UnifiedIntent.sq
  - Fixed SQL reserved keyword issue (`is` → `syn` alias)
```

### Context Adapters (Migration Support)
```
Modules/VoiceOS/managers/CommandManager/.../context/CommandContextAdapter.kt
  - Converts legacy sealed class → unified data class
  - Bidirectional conversion support
```

---

## Architecture

### Before (Duplicate Systems)
```
VoiceOS/CommandManager/
├── CommandContext.kt (sealed class)        # VoiceOS-specific
├── NLP classes                              # VoiceOS NLP

AVA/NLU/
├── IntentClassifier.kt                      # AVA-specific
├── Various NLU classes                      # AVA NLP
```

### After (Unified)
```
Modules/Shared/NLU/                          # KMP Module (shared)
├── UnifiedIntent                            # Cross-platform intent model
├── HybridIntentClassifier                   # Shared classification
├── AvuIntentParser                          # AVU format support
├── UnifiedNluService                        # Main NLU service

Modules/VoiceOS/managers/CommandManager/
├── nlu/NluIntegration.kt                    # VoiceOS ↔ Shared NLU bridge
├── CommandManager.kt                        # Uses NLU for classification

Modules/AVA/NLU/
├── migration/UnifiedNluBridge.kt            # AVA ↔ Shared NLU bridge
├── IntentClassifier.kt                      # Uses hybrid (pattern + BERT)

Modules/VoiceOS/core/command-models/
├── CommandContext (data class)              # Unified context model
├── CommandModels.kt                         # Shared command types

Modules/VoiceOS/managers/CommandManager/
├── CommandContextAdapter.kt                 # Legacy ↔ unified conversion
```

---

## Remaining Work

### Phase 1: Integration ✅ COMPLETE
- [x] Wire UnifiedNluService into VoiceOS CommandManager
- [x] Wire UnifiedNluService into AVA NLU module
- [ ] Runtime testing on device

### Phase 2: Migration
- [ ] Migrate VoiceOS to use UnifiedIntent directly
- [ ] Migrate AVA to use UnifiedIntent directly
- [ ] Remove legacy NLU code from both modules

### Phase 3: Cleanup
- [ ] Remove CommandContextAdapter after full migration
- [ ] Remove deprecated CommandContext sealed class
- [ ] Update all imports

---

## Build Commands

```bash
# From NewAvanues root
cd /Volumes/M-Drive/Coding/NewAvanues

# Build VoiceOS
cd android/apps/VoiceOS && ./gradlew :app:assembleDebug

# Build AVA NLU module
cd android/apps/ava && JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home ./gradlew :NLU:compileDebugKotlinAndroid
```

---

## Branch Strategy

| Branch | Purpose |
|--------|---------|
| `VoiceOS-NLUMerge` | Current work - NLU unification |
| `VoiceOS-Development` | VoiceOS feature development |
| `AVA-Development` | AVA feature development |
| `WebAvanue-Development` | Web interface development |

---

## Important Notes

1. **Java Version:** Project uses Java 17. The `gradlew` script now sets `JAVA_HOME` automatically.

2. **No main branch:** All work happens on feature branches. Do NOT commit to main.

3. **KMP Structure:** The NLU module uses Kotlin Multiplatform with `commonMain` and `androidMain` source sets.

4. **SQLDelight:** Used for intent storage in the NLU module (`SharedNluDatabase`). Version 2.0.1.

5. **AVU Format:** Avanues Universal format for intent definitions (similar to IDC format in IDEACODE).

6. **Hybrid Classification:** VoiceOS uses pattern matching first; AVA uses pattern matching + BERT for complex queries.

---

## Resume Command

To continue this work:
```
Continue the VoiceOS-NLU merge. Current status:
- Phase 1 integration COMPLETE
- VoiceOS CommandManager uses NluIntegration
- AVA NLU has UnifiedNluBridge
- Next: Commit changes, then runtime testing on device
```
