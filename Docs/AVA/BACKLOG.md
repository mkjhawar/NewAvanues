# AVA Feature Backlog

**Last Updated:** 2025-12-07
**Status:** Planning & Future Enhancements

---

## Recently Completed (2025-12-07)

### ✅ P1 Production Issues Resolved

All high-priority production issues from codebase analysis have been resolved:

| Issue | Description | Status | Commit |
|-------|-------------|--------|--------|
| H-07 | Latency tracking for analytics | ✅ Complete | 0a18decb |
| H-05 | Model checksum verification | ✅ Complete | f438144e |
| H-03 | Unknown intent handling | ✅ Complete | 6b17ea0f |
| H-04 | Background embedding computation | ✅ Complete | Already implemented |
| H-02 | Speech recognition | 🔄 Deferred | VoiceOS migration |

**Impact:** AVA is now production-ready with:
- Real-time performance monitoring
- Security-verified model loading
- Enhanced debugging for intent classification
- Battery-aware background learning

---

## Table of Contents

1. [Wake Word System Enhancements](#wake-word-system-enhancements)
2. [VoiceOS Integration](#voiceos-integration)
3. [Native Library Enhancements](#native-library-enhancements)
4. [P2 Issues from Codebase Analysis](#p2-issues-from-codebase-analysis)
5. [Platform Expansion](#platform-expansion)
6. [Performance Optimizations](#performance-optimizations)
7. [Developer Experience](#developer-experience)

---

## Wake Word System Enhancements

### Additional Engine Support

| Feature | Description | Priority | Estimated Effort |
|---------|-------------|----------|------------------|
| **Snowboy Integration** | Add Snowboy wake word engine support | P2 | 2-3 days |
| **PocketSphinx Support** | Lightweight alternative for low-end devices | P2 | 2-3 days |
| **Custom Engine API** | Plugin system for 3rd-party engines | P3 | 5-7 days |

**Snowboy Details:**
- Open source, MIT license
- Supports custom hotword training
- Works on Android, Linux, macOS
- Already has community-trained models for common wake words

**Files to Create:**
- `common/WakeWord/src/main/java/.../snowboy/SnowboyWakeWordDetector.kt`
- `common/WakeWord/build.gradle.kts` (add Snowboy dependency)

---

### Cloud Training Integration

| Feature | Description | Priority | Estimated Effort |
|---------|-------------|----------|------------------|
| **Porcupine Console Integration** | Upload custom wake words for cloud training | P2 | 3-4 days |
| **Training Progress UI** | Show training status in settings | P3 | 1-2 days |
| **Voice Sample Recorder** | Record multiple pronunciations for better accuracy | P2 | 2-3 days |

**Implementation Plan:**
1. Add Picovoice API integration (REST client)
2. Voice recording UI with 5-10 sample captures
3. Upload samples to Picovoice Console API
4. Poll for training completion
5. Download trained .ppn file automatically
6. Update WakeWordDetector with new model

**API Reference:** https://picovoice.ai/docs/api/

---

### Multi-Word Wake Phrases

| Feature | Description | Priority | Estimated Effort |
|---------|-------------|----------|------------------|
| **Phrase Support** | Allow wake phrases like "Hey AVA" or "Okay Computer" | P2 | 3-5 days |
| **Natural Language** | Detect variations: "AVA", "Hey AVA", "Okay AVA" | P3 | 5-7 days |

**Challenges:**
- Longer phrases = higher latency
- Need to detect word boundaries accurately
- False positive rate increases with longer phrases

**Solution Approach:**
- Use two-stage detection:
  1. Primary detector: Single word ("AVA")
  2. Secondary validator: Full phrase ("Hey AVA")
- Only trigger if both stages pass

---

### Engine Priority System

| Feature | Description | Priority | Estimated Effort |
|---------|-------------|----------|------------------|
| **Weighted Engines** | Prefer specific engine when multiple detect | P3 | 1-2 days |
| **Confidence Scores** | Use engine confidence to determine winner | P2 | 2-3 days |
| **User Preference** | Let user choose preferred engine | P3 | 1 day |

**Example:**
```
Both engines detect "AVA"
  - Porcupine: 95% confidence
  - Vivoka: 78% confidence
→ Use Porcupine result (higher confidence)
```

**Implementation:**
- Update `DetectionEvent` to include confidence score
- Modify `WakeWordMultiplexer` to compare scores
- Add "Preferred Engine" setting to UI

---

### Analytics & Accuracy Tracking

| Feature | Description | Priority | Estimated Effort |
|---------|-------------|----------|------------------|
| **Detection Analytics** | Track detection accuracy per engine | P2 | 2-3 days |
| **False Positive Logging** | Log when user dismisses false detection | P2 | 1-2 days |
| **A/B Testing** | Compare engine performance over time | P3 | 3-4 days |

**Metrics to Track:**
- Detection count per engine
- False positive rate
- Detection latency (keyword → activation time)
- Success rate (detected when spoken)
- Battery impact per engine

**Storage:**
- Local SQLite database
- Optional: Send anonymized metrics to analytics server

---

### Battery-Aware Optimization

| Feature | Description | Priority | Estimated Effort |
|---------|-------------|----------|------------------|
| **Adaptive Sensitivity** | Lower sensitivity when battery < 20% | P2 | 1-2 days |
| **Auto-Disable Secondary Engine** | Disable second engine on low battery | P2 | 1 day |
| **Sleep Schedule** | Disable wake word during sleep hours | P3 | 2-3 days |

**Implementation:**
```kotlin
class BatteryAwareWakeWordManager(
    private val multiplexer: WakeWordMultiplexer,
    private val batteryMonitor: BatteryMonitor
) {
    init {
        batteryMonitor.batteryLevel.collect { level ->
            when {
                level < 20 -> {
                    // Low battery: Disable secondary engine
                    multiplexer.setEngineEnabled(WakeWordEngine.VIVOKA, false)
                }
                level < 50 -> {
                    // Medium battery: Lower sensitivity
                    prefsRepository.setSensitivity(WakeWordEngine.PORCUPINE, 0.4f)
                }
                level > 80 -> {
                    // Good battery: Enable all
                    multiplexer.setEngineEnabled(WakeWordEngine.VIVOKA, true)
                    prefsRepository.setSensitivity(WakeWordEngine.PORCUPINE, 0.6f)
                }
            }
        }
    }
}
```

---

## VoiceOS Integration

### System Service Registration

| Feature | Description | Priority | Estimated Effort |
|---------|-------------|----------|------------------|
| **VoiceOSWakeWordAdapter** | Wrap AVA wake word as VoiceOS system service | P1 | 3-4 days |
| **Event Bus Integration** | Publish wake word events to VoiceOS event bus | P1 | 1-2 days |
| **Settings Integration** | Register wake word settings in VoiceOS settings | P1 | 2-3 days |

**Implementation:**
- Create `voiceos-adapters/` module
- Implement `VoiceOSSystemService` interface
- Register service in VoiceOS manifest

**Files to Create:**
- `voiceos-adapters/WakeWordAdapter.kt` (~200 lines)
- `voiceos-adapters/VoiceOSManifest.kt` (~100 lines)

---

### Cloud Sync Adapter

| Feature | Description | Priority | Estimated Effort |
|---------|-------------|----------|------------------|
| **Preferences Sync** | Sync wake word settings across VoiceOS devices | P2 | 2-3 days |
| **Model Sync** | Sync custom trained models | P3 | 3-4 days |

**Implementation:**
```kotlin
class WakeWordSyncAdapter(
    private val prefsRepository: WakeWordPreferencesRepository
) : VoiceOSSyncAdapter {
    override suspend fun exportData(): Map<String, Any> {
        return prefsRepository.exportPreferences()
    }

    override suspend fun importData(data: Map<String, Any>) {
        prefsRepository.importPreferences(data)
    }
}
```

---

### Multi-Platform KMP Library

| Feature | Description | Priority | Estimated Effort |
|---------|-------------|----------|------------------|
| **Extract Common Code** | Move KMP-compatible code to shared library | P1 | 5-7 days |
| **iOS Implementation** | Port wake word detectors to iOS | P2 | 10-14 days |
| **Web Implementation** | Web-based wake word detection (future) | P3 | 7-10 days |

**Module Structure:**
```
AVAWakeWord-KMP/
├── src/
│   ├── commonMain/
│   │   ├── WakeWordEngine.kt
│   │   ├── WakeWordDetector.kt
│   │   ├── WakeWordMultiplexer.kt
│   │   └── WakeWordPreferencesRepository.kt
│   ├── androidMain/
│   │   ├── vivoka/
│   │   └── porcupine/
│   ├── iosMain/
│   │   ├── vivoka/
│   │   └── porcupine/
│   └── webMain/
│       └── WebWakeWordDetector.kt
```

---

### VoiceOS Marketplace Integration

| Feature | Description | Priority | Estimated Effort |
|---------|-------------|----------|------------------|
| **Wake Word Plugin System** | Allow 3rd-party wake word engines | P3 | 7-10 days |
| **Community Models** | Share trained wake word models | P3 | 5-7 days |

---

### System-Wide Wake Word

| Feature | Description | Priority | Estimated Effort |
|---------|-------------|----------|------------------|
| **Global Detection** | Any VoiceOS app can use wake word service | P1 | 3-4 days |
| **Permission System** | Apps request wake word access | P2 | 2-3 days |

---

## Native Library Enhancements

### Multi-Architecture Support

| Feature | Description | Priority | Estimated Effort |
|---------|-------------|----------|------------------|
| **armeabi-v7a Support** | 32-bit ARM for older devices | P2 | 1-2 days |
| **x86_64 Support** | Emulator and x86 devices | P3 | 1-2 days |
| **arm64-v8a Optimization** | Further optimize for modern devices | P2 | 3-4 days |

**Current Status:**
- ✅ arm64-v8a (64-bit ARM) - Primary target
- ❌ armeabi-v7a (32-bit ARM) - Not implemented
- ❌ x86_64 (Intel/AMD) - Not implemented

**APK Size Impact:**
- Single ABI: ~15 MB (.so files)
- All ABIs: ~45 MB total
- Recommendation: Use App Bundle with ABI splits

---

### GPU Acceleration

| Feature | Description | Priority | Estimated Effort |
|---------|-------------|----------|------------------|
| **OpenCL Support** | GPU acceleration via OpenCL | P2 | 5-7 days |
| **Vulkan Support** | Modern GPU API for better performance | P2 | 7-10 days |
| **Auto-Detection** | Automatically use GPU if available | P1 | 2-3 days |

**Performance Gains:**
- CPU-only: ~10 tokens/sec
- OpenCL: ~30 tokens/sec (3x faster)
- Vulkan: ~50 tokens/sec (5x faster)

**Requirements:**
- Android 7.0+ (Vulkan)
- GPU with compute shader support

---

### Quantization-Aware Training

| Feature | Description | Priority | Estimated Effort |
|---------|-------------|----------|------------------|
| **Dynamic Quantization** | Quantize models at runtime for speed | P3 | 5-7 days |
| **Mixed Precision** | Use FP16 for some layers, INT8 for others | P2 | 7-10 days |

---

### Model Hot-Swapping

| Feature | Description | Priority | Estimated Effort |
|---------|-------------|----------|------------------|
| **Runtime Model Switching** | Switch models without app restart | P2 | 3-4 days |
| **Preloading** | Load next model in background | P3 | 2-3 days |

**Use Case:**
User starts conversation in English, switches to Spanish mid-conversation. AVA seamlessly switches from Gemma to Qwen model.

---

## P2 Issues from Codebase Analysis

### Document Parser Enhancements

| Issue | File | Description | Priority | Effort |
|-------|------|-------------|----------|--------|
| DOCX Support | DocumentParser.kt:45 | Support Word documents | P2 | 3-4 days |
| PDF Support | DocumentParser.kt:45 | Support PDF documents | P2 | 3-4 days |
| Excel Support | DocumentParser.kt:45 | Support .xlsx files | P3 | 3-4 days |

**Dependencies:**
- DOCX: Apache POI (~10 MB)
- PDF: Apache PDFBox (~8 MB)
- Excel: Apache POI (~10 MB)

---

### Platform Parity (iOS/Desktop)

| Issue | File | Description | Priority | Effort |
|-------|------|-------------|----------|--------|
| iOS IntentClassifier | IntentClassifier.ios.kt | Implement iOS NLU | P2 | 7-10 days |
| iOS Embedding | EmbeddingModel.ios.kt | Port embedding model to iOS | P2 | 5-7 days |
| Desktop IntentClassifier | IntentClassifier.desktop.kt | JVM-based NLU | P3 | 5-7 days |
| Web IntentClassifier | IntentClassifier.js.kt | WASM-based NLU | P3 | 10-14 days |

**Challenges:**
- iOS: No TensorFlow Lite, use Core ML
- Desktop: JVM doesn't support ONNX easily
- Web: WASM performance limitations

---

### macOS App Enhancements

| Issue | File | Description | Priority | Effort |
|-------|------|-------------|----------|--------|
| Verify Python Scripts | compile_aon.py, etc. | Ensure scripts exist and work | P2 | 2-3 days |
| FFI Type Definitions | NativeLibLoader.swift:200 | Complete FFI bindings | P2 | 3-4 days |
| Preferences UI | ContentView.swift:93 | Implement preferences | P3 | 2-3 days |

---

### Vector Store Optimization

| Issue | File | Description | Priority | Effort |
|-------|------|-------------|----------|--------|
| Index Optimization | VectorStore.kt:112 | HNSW or IVF indexing | P2 | 5-7 days |
| Compression | VectorStore.kt | Product quantization for vectors | P3 | 3-4 days |
| Sharding | VectorStore.kt | Split large vector stores | P3 | 5-7 days |

**Current:**
- Brute-force search (O(n))
- No indexing

**Optimized:**
- HNSW index (O(log n))
- 10-100x faster for large datasets

---

## Platform Expansion

### iOS App

| Feature | Priority | Effort |
|---------|----------|--------|
| Port AVA to iOS (KMP) | P1 | 30-40 days |
| SwiftUI Interface | P1 | 10-14 days |
| Siri Shortcuts Integration | P2 | 3-5 days |
| Apple Watch Companion | P3 | 7-10 days |

---

### Web App

| Feature | Priority | Effort |
|---------|----------|--------|
| React + Tailwind UI | P2 | 14-21 days |
| Web Speech API Integration | P2 | 3-5 days |
| PWA Support | P2 | 2-3 days |
| Offline Mode (Service Worker) | P3 | 5-7 days |

---

### Desktop App (Tauri)

| Feature | Priority | Effort |
|---------|----------|--------|
| Tauri Application Shell | P2 | 7-10 days |
| System Tray Integration | P2 | 2-3 days |
| Global Keyboard Shortcut | P2 | 1-2 days |
| Multi-Monitor Support | P3 | 3-4 days |

---

## Performance Optimizations

### Inference Speed

| Optimization | Current | Target | Effort |
|--------------|---------|--------|--------|
| First Token Latency | ~100ms | <50ms | 5-7 days |
| Tokens/Second | ~10 | >20 | 7-10 days |
| Model Load Time | ~2s | <1s | 3-4 days |

**Techniques:**
- KV cache optimization
- Speculative decoding
- Batch processing

---

### Memory Usage

| Optimization | Current | Target | Effort |
|--------------|---------|--------|--------|
| Model Size in RAM | ~2 GB | <1 GB | 5-7 days |
| KV Cache Size | ~500 MB | <250 MB | 3-4 days |

**Techniques:**
- Paged attention (vLLM-style)
- Quantization (INT4 instead of INT8)

---

### Battery Life

| Optimization | Current Drain | Target | Effort |
|--------------|---------------|--------|--------|
| Wake Word Detection | ~5% per day | <3% | 3-4 days |
| Background Sync | ~2% per day | <1% | 2-3 days |

---

## Developer Experience

### Testing Infrastructure

| Feature | Priority | Effort |
|---------|----------|--------|
| Automated UI Tests (Espresso) | P2 | 5-7 days |
| Performance Regression Tests | P2 | 3-4 days |
| Device Farm Integration | P3 | 2-3 days |

---

### Documentation

| Feature | Priority | Effort |
|---------|----------|--------|
| API Documentation (KDoc) | P2 | 7-10 days |
| Video Tutorials | P3 | 10-14 days |
| Interactive Demo App | P3 | 7-10 days |

---

### Build & CI/CD

| Feature | Priority | Effort |
|---------|----------|--------|
| Automated Builds (GitHub Actions) | P1 | 2-3 days |
| Automatic APK Signing | P1 | 1-2 days |
| Play Store Deployment Pipeline | P2 | 3-4 days |

---

## Notes

**Prioritization Guide:**
- **P1:** Critical for VoiceOS integration or production release
- **P2:** Important but not blocking
- **P3:** Nice-to-have, future enhancements

**Effort Estimates:**
- Based on 1 developer working full-time
- Includes implementation, testing, documentation
- Does not include code review or iteration time

**Dependencies:**
- Many wake word enhancements depend on Phase 3 completion
- VoiceOS integration requires KMP extraction
- Platform expansion requires core Android app stability

---

**Last Updated:** 2025-12-06
