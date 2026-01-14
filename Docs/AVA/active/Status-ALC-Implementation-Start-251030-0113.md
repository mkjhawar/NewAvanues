# AVA AI: ALC (Adaptive LLM Coordinator) Implementation - STARTED

**Date**: 2025-10-30 01:13 PDT
**Status**: 🚀 IN PROGRESS
**Phase**: Week 9-10 - ALC Integration (Pulled Forward)
**Framework**: Android-only Kotlin (not KMP - see Build Fix 251030-0021)

---

## Summary

Started implementation of ALC (Adaptive LLM Coordinator) with MLC LLM integration. Cloned upstream MLC LLM repository, examined Android implementation (mlc4j), and created initial module structure.

---

## What Was Done

### 1. Research & Analysis ✅

**MLC LLM Android Implementation Examined:**
- Repository: `https://github.com/mlc-ai/mlc-llm`
- Android code location: `/android/mlc4j/`
- Total code size: ~508 lines (196 MLCEngine.kt + 225 OpenAIProtocol.kt + 87 JSONFFIEngine.java)
- **Code quality**: Clean, well-structured, mostly Kotlin already!

**Key Files Identified:**
1. **MLCEngine.kt** (196 lines) - Main inference API, streaming support
2. **OpenAIProtocol.kt** (225 lines) - Data classes for OpenAI API compatibility
3. **JSONFFIEngine.java** (87 lines) - JNI bridge to native TVM runtime

**Architecture Understanding:**
```
MLCEngine (Kotlin)
    ↓
JSONFFIEngine (Java/JNI)
    ↓
TVM Runtime (C++ via JNI)
    ↓
libmlc_llm.so (Native library)
    ↓
GPU Inference (OpenCL/Vulkan)
```

### 2. Repository Cloned ✅

```bash
git clone --depth 1 https://github.com/mlc-ai/mlc-llm.git /tmp/mlc-llm
```

**Upstream structure:**
```
/tmp/mlc-llm/android/
├── mlc4j/                    # ✅ Core library (what we'll adopt)
│   ├── src/main/java/ai/mlc/mlcllm/
│   │   ├── MLCEngine.kt
│   │   ├── OpenAIProtocol.kt
│   │   └── JSONFFIEngine.java
│   └── build.gradle
├── MLCChat/                  # 📖 Sample app (reference only)
└── MLCEngineExample/         # 📖 Example app (reference only)
```

### 3. Module Structure Created ✅

```bash
mkdir -p features/llm/src/main/java/com/augmentalis/ava/features/llm/{mlc,domain,provider}
```

**Created structure:**
```
AVA AI/
└── features/llm/
    └── src/main/java/com/augmentalis/ava/features/llm/
        ├── mlc/       # ✅ MLC Android code (adopted & customized)
        ├── domain/    # ✅ AVA interfaces & models
        └── provider/  # ✅ AVA-specific LLM providers
```

---

## Architecture Plan

### Layer 1: MLC Native Integration (mlc/)

**Adopt from upstream** (ai.mlc.mlcllm → com.augmentalis.ava.features.llm.mlc):

| File | Source | Size | Purpose | Status |
|------|--------|------|---------|--------|
| `MLCEngine.kt` | mlc4j/ | 196 lines | Main inference API | ⏳ To copy |
| `OpenAIProtocol.kt` | mlc4j/ | 225 lines | API data classes | ⏳ To copy |
| `JSONFFIEngine.kt` | mlc4j/ (convert Java→Kotlin) | 87 lines | JNI bridge | ⏳ To convert & copy |

**Changes required:**
- Change package: `ai.mlc.mlcllm` → `com.augmentalis.ava.features.llm.mlc`
- Convert JSONFFIEngine.java → JSONFFIEngine.kt
- Add AVA-specific error handling
- Add logging (AVA logger)

### Layer 2: AVA Domain Layer (domain/)

**100% Our Code** - Interfaces and models for AVA:

```kotlin
// domain/LLMProvider.kt
interface LLMProvider {
    suspend fun initialize(modelPath: String): Result<Unit>
    suspend fun generateResponse(prompt: String): Flow<String>
    suspend fun chat(messages: List<ChatMessage>): Flow<String>
    suspend fun cleanup()
}

// domain/ChatMessage.kt
data class ChatMessage(
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageRole {
    SYSTEM, USER, ASSISTANT
}

// domain/LLMResponse.kt
sealed class LLMResponse {
    data class Streaming(val chunk: String) : LLMResponse()
    data class Complete(val fullText: String) : LLMResponse()
    data class Error(val message: String, val exception: Throwable? = null) : LLMResponse()
}
```

### Layer 3: AVA Providers (provider/)

**100% Our Code** - AVA-specific implementations:

```kotlin
// provider/LocalLLMProvider.kt
class LocalLLMProvider(
    private val context: Context
) : LLMProvider {
    private val mlcEngine = MLCEngine()

    override suspend fun initialize(modelPath: String): Result<Unit> {
        // Load model, configure for AVA
    }

    override suspend fun generateResponse(prompt: String): Flow<String> {
        // Use MLCEngine, add AVA-specific logic
    }
}

// provider/HybridLLMProvider.kt (Future)
class HybridLLMProvider(
    private val localProvider: LocalLLMProvider,
    private val cloudProvider: CloudLLMProvider
) : LLMProvider {
    // Intelligent routing (privacy, complexity, connectivity)
}

// provider/PromptTemplates.kt
object AVAPromptTemplates {
    fun formatSystemPrompt(): String { ... }
    fun formatUserQuery(query: String, history: List<ChatMessage>): String { ... }
}
```

---

## Next Steps (Immediate)

### Phase 1: Copy & Customize MLC Code (1-2 hours)

**Tasks:**
1. ✅ Copy MLCEngine.kt to `features/llm/src/main/java/com/augmentalis/ava/features/llm/mlc/`
2. ✅ Copy OpenAIProtocol.kt
3. ✅ Convert JSONFFIEngine.java → JSONFFIEngine.kt
4. ✅ Change package names throughout
5. ✅ Add AVA logging
6. ✅ Add AVA error handling

### Phase 2: Create AVA Domain Layer (2-3 hours)

**Tasks:**
1. ✅ Create LLMProvider.kt interface
2. ✅ Create ChatMessage.kt data class
3. ✅ Create LLMResponse.kt sealed class
4. ✅ Create ModelConfig.kt for AVA-specific configs

### Phase 3: Implement LocalLLMProvider (3-4 hours)

**Tasks:**
1. ✅ Create LocalLLMProvider.kt
2. ✅ Wrap MLCEngine with AVA interface
3. ✅ Add streaming support (Flow<String>)
4. ✅ Add error handling
5. ✅ Add conversation state management

### Phase 4: Build Configuration (1-2 hours)

**Tasks:**
1. ✅ Create `features/llm/build.gradle.kts`
2. ✅ Add TVM dependencies
3. ✅ Configure native library loading
4. ✅ Add to `settings.gradle.kts`
5. ✅ Test build

### Phase 5: Native Library Integration (2-3 hours)

**Tasks:**
1. ⏳ Download pre-built libmlc_llm.so (from MLC releases)
2. ⏳ Add to `features/llm/libs/arm64-v8a/`
3. ⏳ Download TVM runtime (libtvm4j_runtime_packed.so)
4. ⏳ Test native library loading
5. ⏳ Handle library loading errors

### Phase 6: Model Download & Setup (1-2 hours)

**Tasks:**
1. ⏳ Download Gemma 2B model (GGUF format)
2. ⏳ Convert to MLC format (if needed)
3. ⏳ Add model to assets or external storage
4. ⏳ Implement model loading logic
5. ⏳ Test model initialization

### Phase 7: Integration with Chat UI (2-3 hours)

**Tasks:**
1. ⏳ Wire LocalLLMProvider to ChatViewModel
2. ⏳ Replace placeholder responses with LLM
3. ⏳ Add streaming UI (typewriter effect)
4. ⏳ Handle errors in UI
5. ⏳ Test end-to-end flow

### Phase 8: Testing (2-3 hours)

**Tasks:**
1. ⏳ Unit tests for LocalLLMProvider
2. ⏳ Integration tests for streaming
3. ⏳ Performance tests (latency, memory)
4. ⏳ Device testing
5. ⏳ Stress testing (long conversations)

---

## Total Estimated Time

| Phase | Description | Time | Status |
|-------|-------------|------|--------|
| Phase 1 | Copy & customize MLC code | 1-2h | ⏳ Next |
| Phase 2 | Create AVA domain layer | 2-3h | ⏳ Pending |
| Phase 3 | Implement LocalLLMProvider | 3-4h | ⏳ Pending |
| Phase 4 | Build configuration | 1-2h | ⏳ Pending |
| Phase 5 | Native library integration | 2-3h | ⏳ Pending |
| Phase 6 | Model download & setup | 1-2h | ⏳ Pending |
| Phase 7 | Integration with Chat UI | 2-3h | ⏳ Pending |
| Phase 8 | Testing | 2-3h | ⏳ Pending |
| **TOTAL** | **Full ALC implementation** | **16-22h** | **~3-4 days** |

---

## Technical Decisions

### 1. Android-Only (Not KMP)

**Decision**: Build as Android library (not KMP)

**Reasoning**:
- Build system just fixed to Android-only (251030-0021)
- Phase 1 is Android-only per project scope
- iOS deferred to Phase 2+ (user confirmed)
- Avoid KMP complexity until build system stabilizes

**Future**: Can convert to KMP in Phase 2+ when iOS support is needed.

### 2. Adopt MLC Code (Not Wrapper)

**Decision**: Copy MLC Android code into AVA, customize

**Reasoning**:
- Full control over integration layer
- Can add AVA-specific features (prompts, caching, routing)
- Easy to debug (code is ours)
- Small codebase (~508 lines)
- Still use upstream native library (libmlc_llm.so)

**Trade-off**: Must track upstream changes manually, but gain control.

### 3. Use TVM Runtime (Not Direct JNI)

**Decision**: Keep TVM dependency (org.apache.tvm)

**Reasoning**:
- MLC LLM uses TVM as runtime
- TVM provides cross-platform abstraction
- Well-tested, stable runtime
- Alternative (direct JNI) would require rewriting ~1000s of lines

### 4. Streaming-First API

**Decision**: All inference is streaming (no blocking APIs)

**Reasoning**:
- Better UX (typewriter effect)
- Matches ChatGPT UX
- Prevents ANRs (Android Not Responding)
- MLC LLM supports streaming natively

---

## Dependencies Required

### Gradle Dependencies

```kotlin
dependencies {
    // TVM Runtime (for MLC LLM)
    implementation("org.apache.tvm:tvm4j-core:0.10.0")  // Check latest version

    // Kotlin Coroutines (already have)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Kotlin Serialization (already have)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // AVA modules
    implementation(project(":core:domain"))
    implementation(project(":core:common"))
}
```

### Native Libraries

```
features/llm/libs/
├── arm64-v8a/
│   ├── libmlc_llm.so          # MLC LLM runtime (~50MB)
│   └── libtvm4j_runtime_packed.so  # TVM runtime (~20MB)
└── armeabi-v7a/               # Optional (32-bit ARM)
    ├── libmlc_llm.so
    └── libtvm4j_runtime_packed.so
```

**Download from**:
- MLC LLM releases: https://github.com/mlc-ai/binary-mlc-llm-libs/releases
- TVM releases: https://github.com/apache/tvm/releases

---

## File Organization

### Final Structure

```
features/llm/
├── src/main/java/com/augmentalis/ava/features/llm/
│   ├── mlc/                           # Adopted MLC code
│   │   ├── MLCEngine.kt               # Main inference API
│   │   ├── OpenAIProtocol.kt          # API data classes
│   │   └── JSONFFIEngine.kt           # JNI bridge (converted from Java)
│   │
│   ├── domain/                        # AVA interfaces
│   │   ├── LLMProvider.kt             # Main interface
│   │   ├── ChatMessage.kt             # Message model
│   │   ├── LLMResponse.kt             # Response types
│   │   └── ModelConfig.kt             # Configuration
│   │
│   ├── provider/                      # AVA implementations
│   │   ├── LocalLLMProvider.kt        # Local inference (uses MLCEngine)
│   │   ├── CloudLLMProvider.kt        # Cloud fallback (future)
│   │   ├── HybridLLMProvider.kt       # Intelligent routing (future)
│   │   └── PromptTemplates.kt         # AVA prompts
│   │
│   └── util/                          # AVA utilities
│       ├── ModelDownloader.kt         # Model management
│       └── LLMCache.kt                # Response caching
│
├── libs/
│   └── arm64-v8a/
│       ├── libmlc_llm.so              # Native library
│       └── libtvm4j_runtime_packed.so # TVM runtime
│
├── build.gradle.kts                   # Module build config
└── README.md                          # Module documentation
```

---

## Current Status

### Completed ✅
- Research MLC LLM Android implementation
- Clone upstream repository
- Examine code structure (~508 lines total)
- Create module directory structure
- Understand architecture (MLCEngine → JSONFFIEngine → TVM → GPU)

### In Progress ⏳
- None (session ended, ready to continue)

### Next Session TODO
1. Copy MLCEngine.kt to AVA
2. Copy OpenAIProtocol.kt to AVA
3. Convert JSONFFIEngine.java → JSONFFIEngine.kt
4. Change package names
5. Create LLMProvider.kt interface

---

## Blockers & Risks

### Current Blockers

**None** - Ready to proceed with implementation.

### Risks

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| **Native library size** (~70MB total) | High | Medium | Use model compression, lazy loading |
| **TVM dependency not in Maven Central** | Medium | High | Check TVM releases, may need manual AAR |
| **Model size** (Gemma 2B ~5GB) | High | High | Use quantized models (INT4 ~1.5GB) |
| **First inference slow** (~30s) | High | Medium | Warm up model on app start, show progress |
| **Memory usage** (>2GB VRAM) | Medium | High | Use 4-bit quantization, profile on device |

---

## Performance Targets

| Metric | Target | Measured | Status |
|--------|--------|----------|--------|
| **First inference (cold start)** | <30s | ⏳ TBD | Not tested |
| **Subsequent inference** | <2s | ⏳ TBD | Not tested |
| **Tokens per second** | >10 tok/s | ⏳ TBD | Not tested |
| **Memory usage (peak)** | <2GB | ⏳ TBD | Not tested |
| **Model load time** | <10s | ⏳ TBD | Not tested |

---

## References

### Documentation
- **MLC LLM Docs**: https://llm.mlc.ai/docs/deploy/android.html
- **MLC LLM GitHub**: https://github.com/mlc-ai/mlc-llm
- **TVM Documentation**: https://tvm.apache.org/docs/
- **OpenAI API Reference**: https://platform.openai.com/docs/api-reference/chat

### Internal Docs
- **ALC Strategy**: `docs/planning/ALC-Cross-Platform-Strategy.md`
- **MLC Integration Plan**: `docs/planning/MLC-LLM-Android-Integration-Plan.md`
- **Build Fix**: `docs/active/Status-Gradle-Android-Only-251030-0021.md`

### Upstream Code
- **MLC mlc4j**: `/tmp/mlc-llm/android/mlc4j/`
- **MLC samples**: `/tmp/mlc-llm/android/MLCChat/`

---

**Created by**: Manoj Jhawar, manoj@ideahq.net
**Timestamp**: 2025-10-30 01:13 PDT
**Next Session**: Continue with Phase 1 (Copy & Customize MLC Code)
