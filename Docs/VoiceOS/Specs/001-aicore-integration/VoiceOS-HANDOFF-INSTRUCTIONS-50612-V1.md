# Handoff Instructions: AI Core Integration for VOS4

**Created**: 2025-10-26 15:39:24 PDT
**For**: Implementation Agent (g3)
**Feature**: AI-Powered Voice Command Intelligence
**Spec Location**: `/Volumes/M Drive/Coding/vos4/specs/001-aicore-integration/spec.md`

---

## 🎯 Quick Start (What You Need to Do)

You are tasked with implementing AI/NLP capabilities into the VOS4 voice operating system. This document provides everything you need to execute the work using the IDEACODE methodology.

### Your Mission

Integrate hybrid AI/ML capabilities into VOS4 to improve voice command recognition from ~60% accuracy (current Levenshtein fuzzy matching) to ~95% accuracy using:
1. **ONNX Runtime** for sentence embeddings (semantic similarity)
2. **llama.cpp** for on-device LLM inference (complex intent recognition)
3. **Gemini Flash API** for cloud fallback (edge cases)

### Priority

**P1 - Critical Enhancement**: This dramatically improves the core user experience of VOS4's voice command system.

---

## 📋 Step-by-Step IDEACODE Workflow

### Step 1: Read the Feature Specification (MANDATORY)

**File**: `/Volumes/M Drive/Coding/vos4/specs/001-aicore-integration/spec.md`

**What's in it**:
- 5 prioritized user stories with acceptance criteria
- 30 functional requirements (FR-001 through FR-030)
- 8 edge cases you must handle
- 10 success criteria with measurable targets
- Technical constraints and dependencies
- Out of scope items (don't implement these)

**Action**: Read this file completely before proceeding. This is your WHAT document.

---

### Step 2: Run IDEACODE Commands (In Order)

#### 2a. Clarify Underspecified Areas (Optional but Recommended)

```bash
cd /Volumes/M Drive/Coding/vos4
/idea.clarify
```

**What this does**: Analyzes the spec for ambiguous requirements and asks targeted clarification questions.

**Output**: `specs/001-aicore-integration/clarifications.md` with Q&A

**Decision**: If spec is clear enough, you can skip this and go straight to `/idea.plan`. If you find areas unclear while implementing, come back and run this.

---

#### 2b. Generate Implementation Plan (MANDATORY)

```bash
cd /Volumes/M Drive/Coding/vos4
/idea.plan
```

**What this does**:
- Performs technical research (existing codebase analysis, dependency evaluation)
- Creates detailed HOW document with architecture decisions
- Generates data models and API contracts
- Creates quickstart guide for developers

**Expected Outputs**:
- `specs/001-aicore-integration/plan.md` - Main implementation plan
- `specs/001-aicore-integration/research.md` - Technical findings from codebase
- `specs/001-aicore-integration/data-model.md` - Entity definitions, data flow
- `specs/001-aicore-integration/quickstart.md` - How to use the new AICore library
- `specs/001-aicore-integration/contracts/` - API interfaces and schemas

**Time Estimate**: Plan generation takes 20-40 minutes

---

#### 2c. Generate Task Breakdown (MANDATORY)

```bash
cd /Volumes/M Drive/Coding/vos4
/idea.tasks
```

**What this does**:
- Breaks implementation plan into small, dependency-ordered tasks
- Creates actionable checklist in `tasks.md`
- Each task is sized to 30-90 minutes of work
- Dependencies are clearly marked

**Expected Output**: `specs/001-aicore-integration/tasks.md`

**Format**:
```markdown
## Phase 0: Setup
- [ ] Task 0.1: Create AICore module structure
- [ ] Task 0.2: Add ONNX Runtime dependency
- [ ] Task 0.3: Add llama.cpp as git submodule

## Phase 1: ONNX Embeddings (Tier 1)
- [ ] Task 1.1: Implement SentenceEmbedder.kt (depends: 0.2)
- [ ] Task 1.2: Write unit tests for SentenceEmbedder (depends: 1.1)
...
```

---

#### 2d. Execute Implementation (THE BIG ONE)

```bash
cd /Volumes/M Drive/Coding/vos4
/idea.implement
```

**What this does**:
- Executes ALL tasks from `tasks.md` using the **IDE Loop** methodology
- For EACH task:
  1. **IMPLEMENT**: Write production code
  2. **DEFEND**: Write comprehensive tests (80%+ coverage)
  3. **EVALUATE**: Verify acceptance criteria met
  4. **COMMIT**: Create atomic git commit with docs

**IMPORTANT**: This is NOT a one-shot command. The `/idea.implement` command will:
- Process tasks in dependency order
- Show you progress after each task
- Ask for approval if it encounters ambiguity
- Create commits automatically after each task completes
- Generate progress reports

**Time Estimate**: Full implementation is 2-4 weeks of work (depends on task count)

---

#### 2e. Verify Compliance (Post-Implementation)

```bash
cd /Volumes/M Drive/Coding/vos4
/idea.analyze
```

**What this does**:
- Verifies all requirements from spec are implemented
- Checks test coverage meets 80% threshold
- Validates documentation is complete
- Ensures no TODOs or FIXMEs left in code
- Cross-references tasks.md completion

**Expected Output**: `specs/001-aicore-integration/compliance-report.md`

---

#### 2f. Generate Checklist (Final Validation)

```bash
cd /Volumes/M Drive/Coding/vos4
/idea.checklist
```

**What this does**:
- Generates comprehensive validation checklist based on success criteria
- Includes manual testing steps
- Includes performance benchmarks to run
- Includes user acceptance testing guidance

**Expected Output**: `specs/001-aicore-integration/validation-checklist.md`

---

## 🏗️ Architecture Overview (TL;DR)

### What You're Building

```
┌─────────────────────────────────────────────────────────────┐
│   VOS4 (Voice Operating System)                              │
│                                                               │
│   ┌───────────────────────────────────────────────────────┐ │
│   │  VoiceCommandProcessor (existing)                      │ │
│   │  - Receives text from VoiceRecognition service        │ │
│   └──────────────┬────────────────────────────────────────┘ │
│                  │ calls                                     │
│                  ↓                                           │
│   ┌───────────────────────────────────────────────────────┐ │
│   │  HybridAIRouter (NEW in AICore library)               │ │
│   │                                                        │ │
│   │  ┌──────────┐  ┌──────────┐  ┌──────────────────┐    │ │
│   │  │ Tier 1   │→ │ Tier 2   │→ │ Tier 3           │    │ │
│   │  │ Fast     │  │ Smart    │  │ Cloud            │    │ │
│   │  │          │  │          │  │                  │    │ │
│   │  │Levenshtein│  │On-Device │  │Gemini Flash API │    │ │
│   │  │+ ONNX    │  │LLM       │  │                  │    │ │
│   │  │Embeddings│  │(Gemma 2B)│  │(Fallback)        │    │ │
│   │  │          │  │          │  │                  │    │ │
│   │  │30-50ms   │  │200-500ms │  │500-1000ms        │    │ │
│   │  └──────────┘  └──────────┘  └──────────────────┘    │ │
│   └───────────────────────────────────────────────────────┘ │
│                  ↓ returns                                  │
│   ┌───────────────────────────────────────────────────────┐ │
│   │  CommandResolution                                     │ │
│   │  - action: String                                      │ │
│   │  - confidence: Float                                   │ │
│   │  - tier: Int (1/2/3)                                   │ │
│   └───────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### New Module Structure

```
/Volumes/M Drive/Coding/vos4/modules/libraries/AICore/
├── build.gradle.kts
├── src/
│   ├── main/
│   │   ├── java/com/augmentalis/aicore/
│   │   │   ├── embeddings/
│   │   │   │   ├── SentenceEmbedder.kt
│   │   │   │   ├── ONNXEmbeddingEngine.kt
│   │   │   │   └── VectorSimilarity.kt
│   │   │   ├── llm/
│   │   │   │   ├── LlamaEngine.kt
│   │   │   │   ├── LlamaNative.kt
│   │   │   │   └── PromptTemplates.kt
│   │   │   ├── routing/
│   │   │   │   ├── HybridAIRouter.kt
│   │   │   │   ├── IntentClassifier.kt
│   │   │   │   └── ConfidenceScorer.kt
│   │   │   ├── cloud/
│   │   │   │   └── GeminiClient.kt
│   │   │   └── models/
│   │   │       └── ModelManager.kt
│   │   └── cpp/
│   │       ├── llama/          # git submodule
│   │       ├── jni/
│   │       │   └── llama_jni.cpp
│   │       └── CMakeLists.txt
│   └── test/
│       └── java/com/augmentalis/aicore/
│           ├── embeddings/
│           ├── llm/
│           └── routing/
└── README.md
```

---

## 🔑 Key Implementation Details

### Integration Point

**File to Modify**: `/vos4/modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/scraping/VoiceCommandProcessor.kt`

**Current Code**:
```kotlin
// Current simple fuzzy matching
val bestMatch = SimilarityMatcher.findBestMatch(spokenText, availableCommands)
if (bestMatch != null && bestMatch.score > 0.70) {
    executeAction(bestMatch.command)
}
```

**New Code (After Integration)**:
```kotlin
// New AI-powered matching
val resolution = hybridAIRouter.processCommand(
    spokenText = spokenText,
    context = AppContext(
        currentScreen = getCurrentScreen(),
        availableActions = getAvailableActions(),
        currentApp = getCurrentApp()
    )
)

when (resolution) {
    is CommandResolution.Success -> executeAction(resolution.action)
    is CommandResolution.Failure -> showError(resolution.reason)
}
```

### Dependencies to Add

**In `/vos4/modules/libraries/AICore/build.gradle.kts`**:
```kotlin
dependencies {
    // ONNX Runtime for embeddings
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.0")

    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")

    // Gemini API (optional, for cloud fallback)
    implementation("com.google.ai.client.generativeai:generativeai:0.3.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}
```

### Model Files

**ONNX Model**: all-MiniLM-L6-v2.onnx (22MB)
- Download from: `https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2`
- Convert to ONNX using: `optimum-cli export onnx --model sentence-transformers/all-MiniLM-L6-v2 onnx/`
- Store in: `/Android/data/com.augmentalis.vos4/files/models/`

**LLM Model**: gemma-2b-it-q4_k_m.gguf (1.6GB)
- Download from: `https://huggingface.co/lmstudio-ai/gemma-2b-it-GGUF`
- Store in: `/Android/data/com.augmentalis.vos4/files/models/`

---

## 📊 Success Metrics (How to Know You're Done)

### Code Metrics

- [ ] **Test Coverage**: ≥80% for all new code in AICore module
- [ ] **Unit Tests**: ≥90 tests across embeddings, LLM, routing modules
- [ ] **Integration Tests**: ≥20 end-to-end command routing tests
- [ ] **Performance**: Tier 1 <50ms (p80), Tier 2 <500ms (p80), Tier 3 <1000ms (p80)
- [ ] **Memory**: <100MB overhead when models loaded
- [ ] **Zero Memory Leaks**: Verified with LeakCanary after 1000 commands

### Functional Metrics

- [ ] **Command Accuracy**: ≥95% on 200-command test dataset (up from 60%)
- [ ] **Semantic Matching**: ≥85% accuracy on semantic variation tests
- [ ] **Natural Language Dates**: ≥90% parsing accuracy
- [ ] **Multi-Step Commands**: ≥80% success rate
- [ ] **Offline-First**: 95%+ commands processed on-device (Tier 1 + Tier 2)

### User Metrics (Beta Testing)

- [ ] **Crash Rate**: <0.1% (measured in beta)
- [ ] **User Satisfaction**: ≥80% report improvement over old system
- [ ] **Performance Satisfaction**: ≥70% report "no noticeable lag"
- [ ] **Privacy Comfort**: ≥85% comfortable with hybrid on-device/cloud approach

---

## ⚠️ Critical Gotchas & Warnings

### 1. Model File Size

**Problem**: LLM is 1.6GB - users might not want to download
**Solution**:
- Make LLM optional (Tier 2 can be skipped)
- Show clear value proposition: "Download AI model (1.6GB) for 40% better accuracy?"
- WiFi-only download by default
- Add setting to delete models and reclaim space

### 2. Cold Start Latency

**Problem**: First command after app start takes 500ms extra (model loading)
**Solution**:
- Lazy-load models on first command, not on app start
- Show toast: "AI features loading..." during first use
- Warm up models proactively if app in foreground >10 seconds
- Keep models in memory until app backgrounded

### 3. Low-End Device Support

**Problem**: Old phones can't run LLM efficiently
**Solution**:
- Detect device RAM on startup
- If <4GB RAM: Disable Tier 2, only use Tier 1 + Tier 3
- Add setting: "Force disable LLM" for manual control
- Show clear explanation: "Your device doesn't support on-device AI, using cloud fallback"

### 4. Privacy Concerns

**Problem**: Users don't want commands sent to cloud
**Solution**:
- Process 95% of commands on-device (Tier 1 + Tier 2)
- Add prominent setting: "Disable cloud fallback" (offline-only mode)
- Show clear explanation: "Only text (not audio) sent to cloud, and only when confidence is low"
- Provide telemetry opt-in (disabled by default)

### 5. Backward Compatibility

**Problem**: Can't break existing exact command matching
**Solution**:
- Run comprehensive regression test suite (200 existing commands)
- Exact matches should ALWAYS use fast path (Tier 1 Levenshtein component)
- A/B test in beta: 50% old system, 50% new system, compare metrics

### 6. llama.cpp Integration Complexity

**Problem**: llama.cpp is C++, requires JNI bridge and CMake build
**Solution**:
- Reuse existing Whisper integration as template (VOS4 already does this!)
- Copy CMakeLists.txt structure from `/modules/libraries/SpeechRecognition/src/main/cpp/`
- Use same ARM64/ARMv7 optimization flags
- Test thoroughly on multiple devices

---

## 🚨 Zero Tolerance Policies (MUST FOLLOW)

From Master CLAUDE.md:

1. **ALWAYS use local machine time** (`date "+%Y-%m-%d %H:%M:%S %Z"`) - NEVER cloud time
2. **NEVER delete files/folders without EXPLICIT written approval**
3. **ALL code changes MUST be 100% functionally equivalent** (unless told otherwise)
4. **ALL documentation MUST be updated BEFORE commits**
5. **Stage by category**: docs → code → tests (NEVER mix in same commit)
6. **NO AI/Claude/Anthropic references in commits** - Keep professional
7. **MANDATORY: Create/update documentation after EVERY run**
8. **MANDATORY: Deploy parallel specialized agents for 2+ independent tasks** (proven 60-80% time reduction)
9. **ONLY stage/commit/push files YOU created/modified** - NEVER stage unrelated files

---

## 📁 File Locations (Reference)

### VOS4 Project Root
`/Volumes/M Drive/Coding/vos4/`

### Existing Components (Study These)
- **VoiceCommandProcessor**: `modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/scraping/VoiceCommandProcessor.kt`
- **SimilarityMatcher** (current fuzzy logic): `modules/libraries/SpeechRecognition/src/main/java/com/augmentalis/voiceos/speech/utils/SimilarityMatcher.kt`
- **IntentDispatcher**: `modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/routing/IntentDispatcher.kt`
- **Whisper Integration** (CMake example): `modules/libraries/SpeechRecognition/src/main/cpp/`

### New Components (You'll Create These)
- **AICore Module**: `modules/libraries/AICore/`
- **Spec Files**: `specs/001-aicore-integration/`
- **Tests**: `modules/libraries/AICore/src/test/`
- **Documentation**: `docs/Modules/AICore/`

### Universal Instructions
- **Master CLAUDE.md**: `/Volumes/M Drive/Coding/ideacode/claude/CLAUDE.md`
- **Protocols**: `/Volumes/M Drive/Coding/ideacode/protocols/*.md`

---

## 🧪 Testing Strategy

### Phase 1: Unit Tests (During Implementation)

For EACH component you implement, write tests IMMEDIATELY (Defend phase):

```kotlin
// Example: SentenceEmbedderTest.kt
class SentenceEmbedderTest {
    private lateinit var embedder: SentenceEmbedder

    @Before
    fun setup() {
        embedder = SentenceEmbedder(context)
    }

    @Test
    fun `generateEmbedding returns 384-dimensional vector`() {
        val embedding = embedder.generateEmbedding("test text")
        assertEquals(384, embedding.size)
    }

    @Test
    fun `similar texts have high cosine similarity`() {
        val emb1 = embedder.generateEmbedding("buy groceries")
        val emb2 = embedder.generateEmbedding("grocery shopping")
        val similarity = VectorSimilarity.cosine(emb1, emb2)
        assertTrue(similarity > 0.70)
    }

    // ... 18 more test cases
}
```

### Phase 2: Integration Tests (After All Components Done)

```kotlin
@RunWith(AndroidJUnit4::class)
class HybridAIRouterIntegrationTest {
    @Test
    fun `end to end command routing uses correct tier`() {
        // Test that "go back" uses Tier 1 (fast path)
        // Test that "add todo for next friday" uses Tier 2 (LLM)
        // Test that "do the thing with the stuff" uses Tier 3 (cloud)
    }
}
```

### Phase 3: Manual Testing (Before Beta Release)

Follow checklist from `/idea.checklist` output.

---

## 📞 Getting Help

### If You Encounter Issues

1. **Check the spec**: `specs/001-aicore-integration/spec.md`
2. **Check the plan**: `specs/001-aicore-integration/plan.md` (after `/idea.plan`)
3. **Check existing code**: Study Whisper integration as template
4. **Check protocols**: `/Volumes/M Drive/Coding/ideacode/protocols/`
5. **Ask for clarification**: Run `/idea.clarify` to identify underspecified areas

### If You Find Ambiguities in the Spec

**DO NOT MAKE ASSUMPTIONS**. Instead:

1. Run `/idea.clarify` to generate clarification questions
2. Document your assumptions in `specs/001-aicore-integration/assumptions.md`
3. Proceed with reasonable defaults, but mark them clearly in code:
   ```kotlin
   // ASSUMPTION: Using 0.75 threshold for semantic matching
   // (Spec says ≥0.75 but doesn't specify if this is minimum or recommended)
   const val SEMANTIC_MATCH_THRESHOLD = 0.75f
   ```

### If Tests Are Failing

1. Check if regression tests pass (existing functionality should still work)
2. Check if new functionality meets acceptance criteria from spec
3. Use LeakCanary to detect memory leaks
4. Profile performance with Android Profiler
5. Document failures in `specs/001-aicore-integration/test-failures.md`

---

## ✅ Final Checklist (Before Marking Complete)

Before you consider this feature "done", verify ALL of these:

### Code Completeness
- [ ] All tasks in `tasks.md` marked as complete
- [ ] All functional requirements (FR-001 through FR-030) implemented
- [ ] All edge cases (8 scenarios) handled with tests
- [ ] Test coverage ≥80% (measured with JaCoCo)
- [ ] Zero TODOs or FIXMEs in production code
- [ ] All deprecation warnings resolved

### Documentation Completeness
- [ ] `README.md` in AICore module with usage examples
- [ ] KDoc comments on all public APIs
- [ ] Architecture diagram created (in `docs/Modules/AICore/Architecture/`)
- [ ] Quickstart guide tested by someone unfamiliar with the code
- [ ] Changelog updated in `docs/Active/Changelog-YYMMDD-HHMM.md`

### Testing Completeness
- [ ] All unit tests pass (≥90 tests)
- [ ] All integration tests pass (≥20 tests)
- [ ] Regression tests pass (200 existing commands still work)
- [ ] Manual testing checklist completed
- [ ] Beta testing conducted (50+ users)
- [ ] Performance benchmarks meet targets (latency, memory)

### Integration Completeness
- [ ] VoiceCommandProcessor successfully uses HybridAIRouter
- [ ] SimilarityMatcher replaced or extended (no regressions)
- [ ] Settings UI added for AI feature configuration
- [ ] Model download flow working on real device
- [ ] Graceful degradation when models missing

### Compliance
- [ ] `/idea.analyze` compliance report shows 100% implementation
- [ ] No zero-tolerance policy violations
- [ ] All commits follow naming convention
- [ ] All commits include "Created by Manoj Jhawar, manoj@ideahq.net"
- [ ] Git history is clean (no sensitive data, no large files)

---

## 🎓 Learning Resources

### ONNX Runtime
- Official docs: https://onnxruntime.ai/docs/
- Android integration: https://onnxruntime.ai/docs/tutorials/mobile/
- Model conversion: https://onnxruntime.ai/docs/tutorials/export-pytorch-model.html

### llama.cpp
- GitHub repo: https://github.com/ggerganov/llama.cpp
- Android example: https://github.com/ggerganov/llama.cpp/tree/master/examples/llama.android
- GGUF format: https://github.com/ggerganov/ggml/blob/master/docs/gguf.md

### Gemini API
- Quickstart: https://ai.google.dev/gemini-api/docs/quickstart
- Android guide: https://ai.google.dev/gemini-api/docs/android
- Rate limits: https://ai.google.dev/gemini-api/docs/rate-limits

### Sentence Transformers
- HuggingFace models: https://huggingface.co/sentence-transformers
- all-MiniLM-L6-v2: https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2
- ONNX export: https://huggingface.co/docs/optimum/exporters/onnx/usage_guides/export_a_model

---

## 🚀 Ready to Start?

**Your workflow**:

1. Read spec completely: `specs/001-aicore-integration/spec.md`
2. Run `/idea.clarify` (optional, if anything unclear)
3. Run `/idea.plan` (generates implementation plan)
4. Run `/idea.tasks` (generates task breakdown)
5. Run `/idea.implement` (execute with IDE Loop)
6. Run `/idea.analyze` (verify compliance)
7. Run `/idea.checklist` (final validation)

**Estimated Timeline**: 2-4 weeks for full implementation

**Questions?**: Review protocols in `/Volumes/M Drive/Coding/ideacode/protocols/`

---

**Created by Manoj Jhawar, manoj@ideahq.net**
**Handoff Date**: 2025-10-26 15:39:24 PDT
**IDEACODE Version**: 1.0.0

**Good luck! 🚀**
