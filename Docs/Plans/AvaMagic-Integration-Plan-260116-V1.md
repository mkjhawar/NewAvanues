# AvaMagic Integration Implementation Plan

**Date:** 2026-01-16 | **Version:** V1 | **Target:** Full Accessibility + NLU + LLM Integration

---

## Overview

This plan enables distribution across multiple AI terminals for parallel execution. Tasks are organized by stream (independent work) and phase (sequential within stream).

**Total Streams:** 6
**Estimated Tasks:** 24
**Dependencies:** Streams are independent; phases within streams are sequential

---

## Stream 1: NLU Integration (Terminal A)

### Phase 1.1: Create NLU Bridge Service

**Files to modify:**
- `android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/service/VoiceOSAccessibilityService.kt`
- Create: `android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/nlu/NluBridge.kt`

**Tasks:**
1. Create `NluBridge.kt` interface for NLU integration
2. Inject `EnhancedNluService` from `Modules/Shared/NLU`
3. Add NLU processing before command execution in `onAccessibilityEvent`
4. Handle confidence thresholds (high/medium/low)

**Acceptance Criteria:**
- [ ] NluBridge instantiated in AccessibilityService
- [ ] Commands pass through NLU before execution
- [ ] Confidence scores logged

### Phase 1.2: Wire NLU Coordinator

**Files to modify:**
- `Modules/AVA/Chat/src/main/kotlin/com/augmentalis/chat/coordinator/NLUCoordinator.kt`
- `android/apps/voiceoscoreng/build.gradle.kts` (add dependency)

**Tasks:**
1. Add `Modules/AVA/Chat` dependency to voiceoscoreng
2. Initialize NLUCoordinator in service onCreate
3. Route voice input through NLUDispatcher
4. Map NLU results to CommandRegistry commands

**Acceptance Criteria:**
- [ ] NLUCoordinator initialized
- [ ] Voice commands processed through NLU pipeline
- [ ] Intent classification working

---

## Stream 2: LLM Fallback (Terminal B)

### Phase 2.1: Create LLM Fallback Handler

**Files to modify:**
- Create: `android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/llm/LlmFallbackHandler.kt`
- `android/apps/voiceoscoreng/build.gradle.kts`

**Tasks:**
1. Add `Modules/LLM` dependency to voiceoscoreng
2. Create `LlmFallbackHandler` class
3. Inject `LocalLLMProvider` via Hilt
4. Inject `CloudLLMProvider` as secondary fallback
5. Implement confidence-based routing:
   - confidence >= 0.8: Direct execution
   - confidence >= 0.5: LLM clarification
   - confidence < 0.5: Full LLM processing

**Acceptance Criteria:**
- [ ] LlmFallbackHandler created
- [ ] LocalLLMProvider injected
- [ ] CloudLLMProvider as fallback

### Phase 2.2: Integrate with NLU Bridge

**Files to modify:**
- `android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/nlu/NluBridge.kt`
- `android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/llm/LlmFallbackHandler.kt`

**Tasks:**
1. Wire LlmFallbackHandler into NluBridge
2. Add fallback decision logic based on NLU confidence
3. Implement response generation for ambiguous commands
4. Add user confirmation flow for LLM-generated actions

**Acceptance Criteria:**
- [ ] Low-confidence commands routed to LLM
- [ ] LLM responses generate executable commands
- [ ] Fallback chain working: NLU -> LocalLLM -> CloudLLM

---

## Stream 3: IPC Implementation (Terminal C)

### Phase 3.1: Complete IPCManager Android

**Files to modify:**
- `Modules/AvaMagic/IPC/src/androidMain/kotlin/com/augmentalis/avamagic/ipc/IPCManager.android.kt`

**Tasks:**
1. Implement `sendMessage()` - AIDL or Messenger-based
2. Implement `receiveMessage()` - Handler for incoming
3. Implement `registerCallback()` - Callback registration
4. Implement `unregisterCallback()` - Cleanup
5. Implement `bindService()` - Service binding

**Acceptance Criteria:**
- [ ] All 5 stub methods implemented
- [ ] AIDL interface defined
- [ ] Service binding working

### Phase 3.2: Create VoiceUI Service Connection

**Files to modify:**
- Create: `android/apps/VoiceUI/src/main/java/com/augmentalis/voiceui/service/VoiceOSConnection.kt`
- `android/apps/VoiceUI/src/main/AndroidManifest.xml`

**Tasks:**
1. Create VoiceOSConnection service client
2. Bind to VoiceOSAccessibilityService from VoiceUI
3. Implement command relay (VoiceUI -> AccessibilityService)
4. Implement status updates (AccessibilityService -> VoiceUI)

**Acceptance Criteria:**
- [ ] VoiceUI can communicate with voiceoscoreng
- [ ] Commands flow from VoiceUI to AccessibilityService
- [ ] Status updates flow back to VoiceUI

---

## Stream 4: Intelligent Scanning (Terminal D)

### Phase 4.1: Optimize ScreenCacheManager

**Files to modify:**
- `android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/service/ScreenCacheManager.kt`
- Create: `android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/scanning/ScanOptimizer.kt`

**Tasks:**
1. Create ScanOptimizer class
2. Implement element-level change detection
3. Only re-scan changed subtrees (not full tree)
4. Add hash comparison at each tree level
5. Skip static elements (status bar, nav bar)

**Acceptance Criteria:**
- [ ] Partial tree scanning implemented
- [ ] Static elements excluded
- [ ] CPU usage reduced by >50% for unchanged screens

### Phase 4.2: Add Battery-Aware Throttling

**Files to modify:**
- Create: `android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/scanning/BatteryAwareScanner.kt`
- `android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/service/VoiceOSAccessibilityService.kt`

**Tasks:**
1. Create BatteryAwareScanner class
2. Register BroadcastReceiver for battery state
3. Implement throttling tiers:
   - Battery > 50%: Full scanning (300ms debounce)
   - Battery 20-50%: Reduced scanning (1000ms debounce)
   - Battery < 20%: Minimal scanning (3000ms debounce)
   - Charging: Full scanning
4. Add idle-time background scanning with WorkManager

**Acceptance Criteria:**
- [ ] Battery-based throttling working
- [ ] Idle-time scanning scheduled
- [ ] Battery impact measurably reduced

---

## Stream 5: VoiceIntegration Completion (Terminal E)

### Phase 5.1: Complete VoiceIntegration TODOs

**Files to modify:**
- `Modules/AvaMagic/VoiceIntegration/src/commonMain/kotlin/com/augmentalis/avamagic/voice/VoiceIntegration.kt`

**Tasks:**
1. Review 14 TODOs in file
2. Implement voice command registration
3. Implement command routing
4. Implement feedback system
5. Implement error handling

**Acceptance Criteria:**
- [ ] All 14 TODOs resolved
- [ ] Voice commands route correctly
- [ ] Error handling in place

### Phase 5.2: Wire VoiceIntegration to AccessibilityService

**Files to modify:**
- `android/apps/voiceoscoreng/src/main/kotlin/com/augmentalis/voiceoscoreng/service/VoiceOSAccessibilityService.kt`
- `android/apps/voiceoscoreng/build.gradle.kts`

**Tasks:**
1. Add VoiceIntegration module dependency
2. Initialize VoiceIntegration in service
3. Route voice events through VoiceIntegration
4. Connect feedback to overlay system

**Acceptance Criteria:**
- [ ] VoiceIntegration initialized in service
- [ ] Voice events routed correctly
- [ ] Feedback displayed in overlay

---

## Stream 6: Speech Engine Completion (Terminal F)

### Phase 6.1: Complete Google Speech Auth

**Files to modify:**
- `Modules/AvaMagic/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/google/GoogleAuth.kt`
- `Modules/AvaMagic/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/google/GoogleNetwork.kt`

**Tasks:**
1. Implement OAuth2 token refresh
2. Implement credential caching
3. Implement token expiry handling
4. Complete streaming audio upload
5. Implement error recovery

**Acceptance Criteria:**
- [ ] Google Cloud auth working
- [ ] Streaming recognition functional
- [ ] Token refresh automatic

### Phase 6.2: Complete Whisper Native Bindings

**Files to modify:**
- `Modules/AvaMagic/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/engines/whisper/WhisperNative.kt`
- Create: `Modules/AvaMagic/SpeechRecognition/src/main/cpp/whisper_jni.cpp` (if needed)

**Tasks:**
1. Complete JNI bindings for Whisper
2. Implement audio buffer processing
3. Implement model loading
4. Add streaming support
5. Optimize for mobile (quantized models)

**Acceptance Criteria:**
- [ ] Whisper native bindings complete
- [ ] On-device recognition working
- [ ] Model loading from assets

---

## Execution Instructions

### Terminal Setup

Each terminal should:
1. Clone/pull the `Refactor-TempAll` branch
2. Create a worktree: `/i.branch .createworktree {stream-name}`
3. Register with coordination API
4. Lock files before modification

### Running Tasks

```bash
# Terminal A (NLU Integration)
claude --task "Execute Stream 1 from AvaMagic-Integration-Plan-260116-V1.md"

# Terminal B (LLM Fallback)
claude --task "Execute Stream 2 from AvaMagic-Integration-Plan-260116-V1.md"

# Terminal C (IPC)
claude --task "Execute Stream 3 from AvaMagic-Integration-Plan-260116-V1.md"

# Terminal D (Intelligent Scanning)
claude --task "Execute Stream 4 from AvaMagic-Integration-Plan-260116-V1.md"

# Terminal E (VoiceIntegration)
claude --task "Execute Stream 5 from AvaMagic-Integration-Plan-260116-V1.md"

# Terminal F (Speech Engines)
claude --task "Execute Stream 6 from AvaMagic-Integration-Plan-260116-V1.md"
```

### Coordination

- Streams 1 & 2 can be merged first (NLU + LLM)
- Stream 3 blocks VoiceUI testing
- Stream 4 is independent
- Stream 5 depends on Stream 1 completion
- Stream 6 is independent

### Post-Execution

After all streams complete:
1. Merge all worktrees
2. Run integration tests
3. Build and test on device
4. Performance profiling
5. Battery impact testing

---

## Verification Checklist

### P0 (Must Have)
- [ ] AccessibilityService processes voice through NLU
- [ ] LLM fallback activates on low confidence
- [ ] IPC working between VoiceUI and VoiceOSCoreNG
- [ ] Intelligent scanning reduces CPU by >50%

### P1 (Should Have)
- [ ] VoiceIntegration module complete
- [ ] Battery-aware throttling working
- [ ] Google Cloud speech functional

### P2 (Nice to Have)
- [ ] Whisper native bindings complete
- [ ] Full speech engine coverage
- [ ] All 255 TODOs resolved

---

## Dependencies Graph

```
Stream 1 (NLU) ─────────────┬──> Stream 5 (VoiceIntegration)
                            │
Stream 2 (LLM) ─────────────┼──> Integration Testing
                            │
Stream 3 (IPC) ─────────────┘

Stream 4 (Scanning) ────────> Independent

Stream 6 (Speech) ──────────> Independent
```

---

**Plan Created:** 2026-01-16 | **Review Date:** After Stream 1-3 completion
