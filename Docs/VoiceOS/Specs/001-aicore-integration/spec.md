# Feature Specification: AI-Powered Voice Command Intelligence

**Feature Branch**: `001-aicore-integration`
**Created**: 2025-10-26 15:39:24 PDT
**Status**: Draft
**Priority**: P1 - Critical Enhancement
**Author**: Manoj Jhawar (manoj@ideahq.net)

## Executive Summary

Integrate hybrid AI/NLP capabilities into VOS4 to dramatically improve voice command matching accuracy through semantic understanding, moving beyond simple Levenshtein fuzzy matching to intelligent intent recognition using embeddings and large language models.

**Current Problem**: VOS4 uses basic string matching (Levenshtein distance) which fails on:
- Semantic variations ("check grocery" vs "buy groceries")
- Multi-step commands ("add grocery todo and set due date for saturday")
- Ambiguous references ("check the first one")
- Natural language dates ("next friday", "in 2 weeks")

**Proposed Solution**: Create `AICore` shared library with three-tier hybrid architecture:
1. **Tier 1 (Fast Path)**: Levenshtein + ONNX sentence embeddings (30-50ms)
2. **Tier 2 (Smart Path)**: On-device LLM (Gemma 2B) for complex intent (200-500ms)
3. **Tier 3 (Cloud Fallback)**: Gemini Flash API for edge cases (500-1000ms)

**Expected Impact**:
- +40% command recognition accuracy (from ~60% to ~98%)
- Handles 95% commands on-device (privacy preserved)
- <50ms latency for 80% of commands
- Supports natural language input patterns

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Semantic Command Matching (Priority: P1)

**User Journey**: A user says "check grocery" but their todo is titled "Buy groceries" - the system should match semantically similar items even when words don't exactly match.

**Why this priority**: This is the most common failure mode in current VOS4. Users expect the system to understand semantic meaning, not just exact word matches. This single improvement addresses 60%+ of current recognition failures.

**Independent Test**: Create 20 test commands with semantic variations (e.g., "open browser" vs "launch chrome", "check mail" vs "read email"). Measure match rate before (Levenshtein) vs after (ONNX embeddings).

**Acceptance Scenarios**:

1. **Given** user has todo "Buy groceries for the week"
   **When** user says "check grocery todo"
   **Then** system finds correct todo with >0.80 confidence using embedding similarity

2. **Given** user says "open my email app"
   **When** Gmail is installed on device
   **Then** system resolves to Gmail using semantic matching

3. **Given** user says "turn up volume"
   **When** system has commands for "volume_up", "increase_volume", "louder"
   **Then** system matches to volume_up with >0.85 confidence

4. **Given** two similar todos exist: "Grocery shopping" and "Buy groceries"
   **When** user says "check grocery"
   **Then** system shows disambiguation dialog with both options ranked by similarity

---

### User Story 2 - Natural Language Date Parsing (Priority: P2)

**User Journey**: User says "add todo for next friday" - system should understand relative dates and convert to absolute dates for todo due dates.

**Why this priority**: Natural language dates are a major UX improvement over forcing users to say "2025-10-31". This addresses 30% of current friction in todo/calendar commands.

**Independent Test**: Provide 15 relative date phrases ("tomorrow", "next week", "in 3 days", "friday"). Verify each converts to correct absolute date with current date context.

**Acceptance Scenarios**:

1. **Given** today is Monday, October 26, 2025
   **When** user says "add todo for next friday"
   **Then** system creates todo with due date of November 1, 2025

2. **Given** current time is 2:30 PM
   **When** user says "remind me in 2 hours"
   **Then** system sets reminder for 4:30 PM today

3. **Given** today is January 28, 2025
   **When** user says "add todo for end of month"
   **Then** system sets due date to January 31, 2025

---

### User Story 3 - Multi-Step Command Understanding (Priority: P2)

**User Journey**: User says "add grocery todo and set due date for saturday" - system should parse compound commands and execute multiple actions sequentially.

**Why this priority**: Multi-step commands represent 15-20% of power user workflows. Current system requires users to issue separate commands, reducing efficiency.

**Independent Test**: Provide 10 compound commands. Verify system correctly extracts multiple intents and parameters, then executes in logical order.

**Acceptance Scenarios**:

1. **Given** user says "add grocery todo and set due date for saturday"
   **When** command is processed
   **Then** system creates todo "grocery" with due date set to upcoming Saturday

2. **Given** user says "open chrome and go to gmail"
   **When** command is processed
   **Then** system launches Chrome, waits for it to open, then navigates to gmail.com

3. **Given** user says "turn on wifi and connect to home network"
   **When** command is processed
   **Then** system enables WiFi, scans for networks, selects "home" network, connects

---

### User Story 4 - Context-Aware Disambiguation (Priority: P3)

**User Journey**: User says "check the first one" - system should understand "first one" refers to first todo in currently visible list.

**Why this priority**: Contextual references improve natural conversation flow. This is a power user feature that enhances UX but isn't critical for basic functionality.

**Independent Test**: Set up screen with 5 todos visible. Issue commands like "check the second one", "mark first as done". Verify correct todo is targeted based on UI context.

**Acceptance Scenarios**:

1. **Given** todo list showing items 1-5 on screen
   **When** user says "check the first one"
   **Then** system identifies todo at position 1 and marks it complete

2. **Given** user viewing email app with 3 unread messages
   **When** user says "read the last message"
   **Then** system opens 3rd message from the top

3. **Given** ambiguous command "check it"
   **When** no clear context available
   **Then** system asks "Check what? I see 3 todos on screen." with numbered options

---

### User Story 5 - Offline-First with Cloud Fallback (Priority: P1)

**User Journey**: User issues voice command - system should attempt on-device processing first (Tier 1 + Tier 2), only falling back to cloud when confidence is low, preserving privacy.

**Why this priority**: Privacy is a core VOS4 value proposition. Users trust accessibility services with sensitive device access, so keeping AI processing on-device is critical for trust.

**Independent Test**: Disconnect device from internet. Issue 20 common commands. Verify >90% success rate using only Tier 1 (embeddings) and Tier 2 (on-device LLM).

**Acceptance Scenarios**:

1. **Given** device is offline
   **When** user says "check grocery todo"
   **Then** system successfully matches using on-device embeddings without error

2. **Given** device is online but command is unambiguous
   **When** user says "go back"
   **Then** system uses fast path (Tier 1) and never hits cloud API

3. **Given** device is online and command is highly ambiguous
   **When** user says "do the thing with the stuff"
   **Then** system falls back to Gemini Flash, gets clarification, caches learned pattern for future

4. **Given** user settings have "Cloud AI" disabled
   **When** command fails Tier 1 and Tier 2
   **Then** system shows "Command not understood. Cloud AI is disabled." with option to enable

---

### Edge Cases

1. **What happens when model files are not downloaded?**
   - System detects missing ONNX/LLM models on first use
   - Shows one-time setup dialog: "AI features require 1.6GB download. Download now?"
   - Falls back to basic Levenshtein matching if user declines
   - Provides setting to download later in VOS4 settings

2. **How does system handle low memory devices?**
   - Detect available RAM on startup
   - If <4GB total RAM: Only enable Tier 1 (ONNX embeddings, 22MB)
   - If 4-6GB RAM: Enable Tier 1 + Tier 2 with 4-bit quantized model (1.6GB)
   - If >6GB RAM: Full model support
   - Setting to force disable LLM on low-end devices

3. **What happens during model inference timeout?**
   - Tier 2 (LLM) has 2-second timeout
   - If timeout exceeded, log error and fall back to Tier 3 (cloud)
   - If Tier 3 also fails/times out, return "Command not understood"
   - User setting to adjust timeout (1-5 seconds)

4. **How does system handle cold start latency?**
   - Models lazy-load on first command (adds 500ms to first command)
   - Show toast: "AI features loading..." during first use
   - After initial load, models stay in memory until app backgrounded
   - Warm-up models proactively if app in foreground for >10 seconds

5. **What happens if user has limited storage?**
   - ONNX model (22MB) always required for AI features
   - LLM model (1.6GB) optional - can skip and use Tier 1 + Tier 3 only
   - Models stored in `/Android/data/com.augmentalis.vos4/files/models/`
   - Setting to clear models to reclaim space
   - Auto-download on WiFi only (user setting)

6. **How does system handle multi-language support?**
   - Phase 1 (this spec): English-only models
   - Phase 2 (future): Multi-language ONNX embeddings (paraphrase-multilingual)
   - Phase 3 (future): Multi-language LLM (Gemma 7B or Aya)
   - Detect device language, show "AI features not available in [language] yet"

7. **What happens when cloud API quota is exceeded?**
   - Track Gemini API usage (free tier: 15 requests/minute)
   - If quota exceeded: Show "Cloud AI temporarily unavailable"
   - Fall back to Tier 1 + Tier 2 only
   - Reset quota after 1 minute cooldown
   - Enterprise tier setting: Use custom API key with higher limits

8. **How does system handle model updates?**
   - Check for model updates on app start (once per day)
   - Download new models in background if WiFi + charging
   - Show notification: "AI models updated" when complete
   - Atomic update: Keep old model until new one fully downloaded
   - Rollback if new model fails validation tests

---

## Requirements *(mandatory)*

### Functional Requirements

#### Core AI Capabilities

- **FR-001**: System MUST provide three-tier command routing: Fast Path (Levenshtein + embeddings), Smart Path (on-device LLM), Cloud Fallback (Gemini API)
- **FR-002**: System MUST use ONNX Runtime for sentence embeddings with all-MiniLM-L6-v2 model (22MB)
- **FR-003**: System MUST use llama.cpp for on-device LLM inference with Gemma 2B 4-bit quantized model (1.6GB)
- **FR-004**: System MUST calculate cosine similarity for semantic matching with threshold ≥0.75 for high confidence
- **FR-005**: System MUST support offline-first processing, only hitting cloud when Tier 1 + Tier 2 confidence <0.70

#### Voice Command Integration

- **FR-006**: System MUST integrate with existing VoiceCommandProcessor to replace SimilarityMatcher fuzzy logic
- **FR-007**: System MUST maintain backward compatibility with exact command matching (no regression)
- **FR-008**: System MUST track routing metrics: tier used, latency, confidence score, success/failure
- **FR-009**: System MUST provide confidence-based disambiguation UI when multiple matches >0.70 confidence
- **FR-010**: System MUST support natural language date extraction: "tomorrow", "next friday", "in 2 weeks"

#### Model Management

- **FR-011**: System MUST download models on first use with user consent (show size, purpose, privacy statement)
- **FR-012**: System MUST validate model integrity using SHA256 checksums before loading
- **FR-013**: System MUST lazy-load models on first command to minimize startup time
- **FR-014**: System MUST provide settings UI to: enable/disable AI features, manage model storage, set cloud fallback policy
- **FR-015**: System MUST automatically use 4-bit quantized models on devices with <6GB RAM

#### Performance Requirements

- **FR-016**: Tier 1 (embeddings) MUST complete in <50ms for 80th percentile
- **FR-017**: Tier 2 (LLM) MUST complete in <500ms for 80th percentile
- **FR-018**: Tier 3 (cloud) MUST complete in <1000ms for 80th percentile
- **FR-019**: System MUST maintain <100MB memory overhead when models loaded
- **FR-020**: System MUST handle 1000 consecutive commands without memory leaks

#### Privacy & Security

- **FR-021**: System MUST process 95%+ of commands on-device without cloud API calls
- **FR-022**: System MUST provide user setting to completely disable cloud fallback (offline-only mode)
- **FR-023**: System MUST NOT send voice audio to cloud - only processed text transcripts
- **FR-024**: System MUST NOT log user commands to external services (telemetry opt-in only)
- **FR-025**: System MUST store models in app-private storage with proper Android file permissions

#### MagicCode Integration (Future)

- **FR-026**: AICore library MUST be usable by MagicCode runtime for YAML app intent classification
- **FR-027**: AICore MUST expose public API for: embedding generation, intent classification, semantic search
- **FR-028**: AICore MUST support plugin architecture for custom prompt templates
- **FR-029**: AICore MUST be publishable as standalone AAR for external projects

### Key Entities

- **AICore Library**: **SEPARATE** Android library module containing ONNX Runtime, llama.cpp JNI, and HybridAIRouter
  - **Architecture Decision**: Standalone module for reusability across VOS4 and MagicCode projects
  - Location: `/vos4/modules/libraries/AICore/`
  - Dependencies: ONNX Runtime Android (1.17.0), Gson (2.10.1), Coroutines (1.7.3)
  - Native components: CMake build for llama.cpp with ARM64/ARMv7 support
  - **Rationale**:
    1. Enables reuse in MagicCode project for YAML app intent classification
    2. Independent testing and versioning
    3. Clear public API boundary (only AICore.kt exposed, all else internal)
    4. Follows existing VOS4 pattern (like SpeechRecognition library)
    5. Future-proof for publishing as standalone AAR if needed

- **HybridAIRouter**: Core routing logic that selects Tier 1/2/3 based on confidence thresholds
  - Input: spoken text (String), app context (current screen, available commands)
  - Output: CommandResolution (success with action + confidence, or failure with reason)
  - Tracks metrics: tier usage, latency, fallback rate

- **SentenceEmbedder**: ONNX wrapper for semantic similarity
  - Model: all-MiniLM-L6-v2 (22MB, 384-dimensional embeddings)
  - Input: text string (up to 256 tokens)
  - Output: FloatArray[384]
  - Performance: 15-30ms on mobile CPU

- **LlamaEngine**: JNI bridge to llama.cpp for on-device LLM
  - Model: Gemma 2B 4-bit quantized GGUF (1.6GB)
  - Input: prompt template + user text + context
  - Output: JSON with intent, confidence, parameters
  - Performance: 8-12 tokens/second on flagship devices

- **GeminiClient**: REST API client for cloud fallback
  - Model: Gemini 1.5 Flash (Google AI API)
  - Rate limit: 15 requests/minute (free tier), 1000/minute (paid)
  - Cost: $0.075 per 1M input tokens
  - Timeout: 2 seconds with exponential backoff retry

- **CommandResolution**: Result object from AI routing
  - Properties: success (Boolean), action (String), confidence (Float), tier (Int), latency (Long)
  - Serializable for logging and analytics

- **ModelManager**: Handles model download, validation, updates
  - Responsibilities: check for updates, download models, verify SHA256, atomic updates
  - Storage location: `/Android/data/com.augmentalis.vos4/files/models/`
  - Update frequency: once per day (user configurable)

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Command recognition accuracy increases from 60% (baseline Levenshtein) to 95%+ (AI-powered) on test dataset of 200 diverse commands
- **SC-002**: 95% of commands processed on-device (Tier 1 + Tier 2) without cloud API calls in normal usage
- **SC-003**: Average command latency remains <100ms for 80th percentile (fast path must dominate)
- **SC-004**: System handles semantic variations with ≥85% accuracy (e.g., "grocery" matches "Buy groceries")
- **SC-005**: Natural language date parsing achieves ≥90% accuracy on 50 test phrases
- **SC-006**: Multi-step commands successfully parsed and executed in ≥80% of test cases
- **SC-007**: Disambiguation UI shown only when 2+ matches have confidence >0.70 (precision: low false positives)
- **SC-008**: Zero memory leaks after 1000 consecutive commands (verified with LeakCanary)
- **SC-009**: Cold start adds <500ms to first command (model lazy-load), subsequent commands <50ms
- **SC-010**: User survey shows ≥80% satisfaction with AI-powered voice command improvements

### User Validation Tests

1. **Semantic Matching Test**:
   - Give 10 users list of 20 commands with semantic variations
   - Measure success rate before (Levenshtein) vs after (embeddings)
   - Target: ≥30 percentage point improvement

2. **Natural Language Test**:
   - Give 5 users tasks requiring relative dates ("add todo for next week")
   - Measure task completion success rate
   - Target: ≥90% complete successfully on first try

3. **Multi-Step Command Test**:
   - Give 5 users 5 compound commands to execute
   - Measure how many complete successfully without manual intervention
   - Target: ≥80% success rate

4. **Privacy Perception Test**:
   - Survey 50 users after explaining on-device vs cloud processing
   - Measure willingness to use AI features
   - Target: ≥85% comfortable with hybrid approach, ≥60% prefer offline-only

5. **Performance Perception Test**:
   - Have 10 users issue 20 commands each
   - Survey: "Did you notice any lag compared to before?"
   - Target: ≥70% report "no noticeable difference" or "faster"

---

## Technical Constraints

### Performance Constraints

- **Maximum memory overhead**: 100MB when models loaded (excluding model file size)
- **Model file size limits**:
  - ONNX embeddings: ≤25MB
  - LLM quantized: ≤2GB (4-bit)
- **Inference latency targets**:
  - Tier 1: ≤50ms (p80)
  - Tier 2: ≤500ms (p80)
  - Tier 3: ≤1000ms (p80)
- **Cold start overhead**: ≤500ms for first command (lazy model load)
- **Battery impact**: ≤5% additional drain per hour of active use

### Platform Constraints

- **Minimum Android version**: API 28 (Android 9.0) - matches existing VOS4 target
- **Minimum RAM**: 3GB (with Tier 1 only), 4GB recommended (Tier 1 + Tier 2)
- **Storage required**: 22MB (embeddings) + 1.6GB (LLM, optional) + 50MB (cache/temp)
- **Architecture support**: ARM64 (primary), ARMv7 (fallback, Tier 1 only)
- **NDK version**: r25c (matches existing Whisper integration)

### Dependency Constraints

- **ONNX Runtime**: 1.17.0 (latest stable, Jan 2025)
- **llama.cpp**: Latest commit from main branch (no versioned releases)
- **Kotlin**: 1.9.0+ (matches VOS4 project)
- **Gradle**: 8.0+ (matches VOS4 project)
- **CMake**: 3.22.1+ (for native builds)

### API Constraints

- **Gemini API**:
  - Free tier: 15 requests/minute, 1500 requests/day
  - Input limit: 1M tokens per request (more than sufficient)
  - Requires API key (user-provided or embedded with rotation)
- **ONNX Model**:
  - Max input length: 256 tokens (all-MiniLM-L6-v2 limit)
  - Output dimensions: 384 (fixed)
- **LLM Model**:
  - Context window: 8192 tokens (Gemma 2B)
  - Max output: 128 tokens for command classification

---

## Out of Scope (Future Phases)

### Phase 2 Features (Not in This Spec)

- Multi-language model support (English-only for Phase 1)
- Voice activity detection improvements
- Speaker identification
- Command prediction before user finishes speaking
- Personalized model fine-tuning based on user patterns
- On-device model training

### Phase 3 Features (Future Roadmap)

- Integration with MagicCode YAML apps (separate spec)
- Visual context understanding (screenshot analysis for "click the blue button")
- Cross-app pattern learning (learn from user habits)
- Proactive suggestions ("It's 5 PM, should I start navigation home?")
- Custom wake word detection

---

## Dependencies & Risks

### External Dependencies

1. **ONNX Runtime Android** (com.microsoft.onnxruntime:onnxruntime-android:1.17.0)
   - Risk: Dependency update breaks API
   - Mitigation: Pin version, test before upgrading

2. **llama.cpp** (no versioned release, git submodule)
   - Risk: Breaking changes in main branch
   - Mitigation: Pin specific commit SHA, track upstream releases

3. **Gemini API** (Google AI)
   - Risk: API deprecation, quota changes, pricing changes
   - Mitigation: Abstract behind interface, support multiple LLM providers

4. **HuggingFace Model Hub** (model downloads)
   - Risk: Model files unavailable, CDN issues
   - Mitigation: Mirror models to S3/GCS, bundle tiny fallback model in APK

### Technical Risks

1. **Model file size** (1.6GB for LLM)
   - Risk: Users decline download due to size
   - Mitigation: Make optional, explain value proposition clearly, WiFi-only download

2. **Inference performance on low-end devices**
   - Risk: LLM too slow on old phones (<2GB RAM, older chips)
   - Mitigation: Device capability detection, disable Tier 2 on weak devices

3. **Memory management**
   - Risk: Models consume too much RAM, cause app to be killed
   - Mitigation: Unload models when app backgrounded, use memory-mapped models

4. **Battery drain**
   - Risk: Continuous inference drains battery faster
   - Mitigation: Profile power usage, cache results aggressively, power saver mode

### Integration Risks

1. **Backward compatibility with existing commands**
   - Risk: New AI router breaks exact command matching
   - Mitigation: Comprehensive regression test suite, A/B testing rollout

2. **VoiceCommandProcessor integration complexity**
   - Risk: Tight coupling to existing code makes refactor difficult
   - Mitigation: Design AICore as drop-in replacement for SimilarityMatcher

3. **MagicCode future integration**
   - Risk: AICore API not flexible enough for YAML apps
   - Mitigation: Design generic intent classification API from start

---

## Acceptance Testing Strategy

### Automated Tests (95% coverage target)

1. **Unit Tests** (JUnit 4):
   - SentenceEmbedder: 20 test cases (similarity calculation, tokenization, error handling)
   - LlamaEngine: 15 test cases (prompt formatting, JSON parsing, timeout handling)
   - HybridAIRouter: 30 test cases (tier selection, confidence thresholds, fallback logic)
   - GeminiClient: 10 test cases (API calls, error handling, retry logic)
   - ModelManager: 15 test cases (download, validation, updates)

2. **Integration Tests** (Instrumented):
   - End-to-end command routing: 50 test commands across all tiers
   - Model loading: Verify models load correctly and inference works
   - Fallback behavior: Simulate tier failures, verify cascading fallback
   - Performance: Measure actual latency on test device

3. **Regression Tests**:
   - Run existing 200-command test suite, verify accuracy ≥current
   - Exact command matching: 50 commands that should use fast path only

### Manual Tests (Before Release)

1. **Usability Testing** (5 users, 1 hour each):
   - Issue 20 commands with semantic variations
   - Issue 10 multi-step commands
   - Issue 10 natural language date commands
   - Observe confusion points, measure task success rate

2. **Performance Testing** (1 test device):
   - Issue 100 commands consecutively, measure latency distribution
   - Monitor memory usage over 1000 commands
   - Profile battery drain over 1 hour of usage

3. **Edge Case Testing**:
   - Test all 8 edge cases from spec
   - Verify graceful degradation when models missing
   - Test on low-RAM device (3GB RAM or less)

### Beta Testing Strategy

1. **Internal Alpha** (Week 1): 5 team members
   - Focus: Catch crashes, validate core functionality
   - Metrics: Crash rate, command success rate

2. **Closed Beta** (Week 2-3): 50 power users from VOS4 community
   - Focus: Real-world usage patterns, find edge cases
   - Metrics: Command recognition accuracy, user satisfaction survey

3. **Open Beta** (Week 4): Release to 10% of users
   - Focus: Performance at scale, diverse device compatibility
   - Metrics: Tier usage distribution, latency percentiles, opt-in rate

---

## Privacy & Security Considerations

### Data Handling

- **Voice audio**: Never sent to cloud - only text transcripts after local STT
- **Command text**: Sent to Gemini API only if Tier 1/2 confidence <0.70
- **User context**: Device info (screen, app list) never leaves device
- **Model updates**: SHA256 validation before loading to prevent tampering

### User Controls

- **Settings → AI Features → Cloud Fallback**: Toggle on/off
- **Settings → AI Features → Model Management**: Download, update, delete models
- **Settings → AI Features → Telemetry**: Opt-in to share anonymized metrics
- **Settings → AI Features → Advanced → Confidence Threshold**: Adjust tier transition points

### Security Measures

- **Model integrity**: SHA256 checksums verified before loading
- **API key rotation**: Embedded Gemini key rotates every 30 days
- **Rate limiting**: Prevent API abuse, respect free tier limits
- **Sandboxing**: Models run in isolated process if device supports

---

**Created by Manoj Jhawar, manoj@ideahq.net**
**IDEACODE Spec Version: 1.0.0**
**Next Steps**: Run `/idea.clarify` to identify underspecified areas, then `/idea.plan` to generate implementation plan.
