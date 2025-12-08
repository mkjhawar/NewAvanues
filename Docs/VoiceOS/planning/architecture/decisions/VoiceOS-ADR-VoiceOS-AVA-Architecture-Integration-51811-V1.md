# Architecture Decision Record: VoiceOS + AVA Integration

**Date:** 2025-11-18
**Status:** PROPOSED
**Decision Makers:** Manoj Jhawar, Development Team
**Related Systems:** VoiceOS, AVA

---

## Executive Summary

This document analyzes the optimal architecture for integrating VoiceOS (accessibility voice commands) with AVA (AI assistant with NLU/LLM) and provides recommendations for plugin system placement, NLU location, and app structure.

---

## Context

### Current Systems

**VoiceOS:**
- Function: Deterministic voice command execution
- Processing: Pattern matching, command lookup
- Latency: <100ms (local)
- Resources: Lightweight when idle
- Plugin System: Command-level extensions

**AVA:**
- Function: Intent understanding, context reasoning
- Processing: NLU (on-device ONNX), LLM (on-device TVM)
- Latency: 50-200ms NLU, 500-2000ms LLM
- Resources: Moderate NLU, heavy LLM during inference

### Actual Model Sizes (Corrected)

| Component | AVA Filename | Size | Notes |
|-----------|--------------|------|-------|
| **NLU (MobileBERT INT8)** | `AVA-ONX-384-BASE-INT8.onnx` | 10-20 MB | Downloaded on demand |
| **NLU Vocabulary** | `vocab.txt` | ~460 KB | Downloaded with model |
| **LLM (Gemma 2B)** | `AVA-GEM-2B-Q4.tar` | ~2 GB | Default English |
| **LLM (Gemma 3 4B)** | `AVA-G3M-4B-Q4.tar` | ~2.5 GB | Higher quality |
| **Total NLU** | | **~15-20 MB** | Not 50-100 MB as initially estimated |

### Cloud Storage Structure

Models are hosted at: `https://www.augmentalis.com/avanuevoiceosava/`

```
avanuevoiceosava/
├── ava/
│   ├── nlu/
│   │   ├── AVA-ONX-384-BASE-INT8.onnx
│   │   └── vocab.txt
│   └── llm/
│       ├── en/
│       │   └── AVA-GEM-2B-Q4.tar
│       ├── es/
│       │   └── AVA-FLOR-1B-Q4.tar
│       └── manifest.json
├── English/
│   ├── english.json
│   └── en_voice_resource.zip
├── Spanish/
│   ├── spanish.json
│   └── es_voice_resource.zip
└── ...
```

---

## Decision Questions

1. **Plugin Location:** VoiceOS (command-level) or AVA (intent-level)?
2. **NLU Placement:** Keep in AVA or move to VoiceOS?
3. **App Architecture:** Separate apps, merged, or layered?

---

## Analysis

### Plugin System Location

#### Option A: Plugins in VoiceOS (Command-Level)

```
User: "Add milk to my Todoist"
       ↓
VoiceOS: Pattern match "add * to * Todoist"
       ↓
Todoist Plugin: Execute API call
```

**Pros:**
- Fast execution (no NLU overhead)
- Deterministic behavior
- Works offline
- Low battery usage
- Simple plugin development

**Cons:**
- Rigid commands (exact phrases only)
- No context awareness
- Every variation must be pre-defined

---

#### Option B: Plugins in AVA (Intent-Level)

```
User: "Put milk on my Todoist list"
       ↓
AVA NLU: Intent=ADD_ITEM, App=TODOIST, Item="milk"
       ↓
Todoist Plugin: Execute with entities
```

**Pros:**
- Natural language flexibility
- Context awareness
- Entity extraction
- Plugin defines intents, not phrases

**Cons:**
- Slower (NLU adds 50-200ms)
- Non-deterministic (NLU can misunderstand)
- More complex plugin development

---

#### RECOMMENDATION: Dual-Level Plugin System

**Both plugin types coexist:**

| Plugin Type | Location | Use Case | Latency |
|-------------|----------|----------|---------|
| Command Plugin | VoiceOS | Exact, fast, deterministic | <100ms |
| Intent Plugin | AVA | Natural, flexible, context-aware | 100-300ms |

**Flow:**
```
Speech → VoiceOS Command Match → [Match?] → Execute
                             ↓ No
                  AVA NLU → Intent Match → Execute
```

---

### NLU Placement Analysis

#### Current: NLU in AVA
```
VoiceOS (Commands) ←IPC→ AVA (NLU + LLM)
```

#### Proposed: Two-Tier NLU

**Tier 1 (VoiceOS):** Lightweight pattern/keyword matching
- Common intents (navigation, system, apps)
- <5 MB
- <50ms latency

**Tier 2 (AVA):** Full BERT-based NLU
- Complex intents (reasoning, context)
- ~15-20 MB (downloaded on demand)
- 50-200ms latency

**RECOMMENDATION:** Keep full NLU in AVA

**Reasoning:**
- NLU model is only 15-20 MB (manageable)
- Avoids duplicating model loading infrastructure
- VoiceOS remains lightweight for accessibility
- Clear separation: VoiceOS = deterministic, AVA = intelligent

---

### App Architecture Options

#### Option A: Separate Apps (Current)

```
┌─────────────┐     IPC      ┌─────────────┐
│  VoiceOS    │ ←──────────→ │    AVA      │
│ - Commands  │              │ - NLU       │
│ - Plugins   │              │ - LLM       │
│ - A11y      │              │ - Context   │
└─────────────┘              └─────────────┘
```

**Mobile Performance:**
- CPU: ⭐⭐⭐⭐ (separate processes)
- Battery: ⭐⭐⭐ (IPC overhead)
- Memory: ⭐⭐ (duplicate libraries)
- Latency: ⭐⭐ (IPC 10-50ms)

**Deployment Flexibility:** ✅ VoiceOS or AVA standalone

---

#### Option B: Single Merged App

```
┌─────────────────────────────┐
│        VoiceOS + AVA        │
│ (Everything in one app)     │
└─────────────────────────────┘
```

**Mobile Performance:**
- CPU: ⭐⭐⭐ (can't partially kill)
- Battery: ⭐⭐⭐⭐ (no IPC)
- Memory: ⭐⭐⭐⭐ (shared libraries)
- Latency: ⭐⭐⭐⭐⭐ (direct calls)

**Deployment Flexibility:** ❌ All-or-nothing

---

#### Option C: Layered Architecture (RECOMMENDED)

```
┌───────────────────────────────────────┐
│            Application Layer           │
│  ┌─────────────┐   ┌─────────────┐    │
│  │  VoiceOS    │   │    AVA      │    │
│  │  (UI/App)   │   │  (UI/App)   │    │
│  └──────┬──────┘   └──────┬──────┘    │
│         │                  │           │
├─────────┼──────────────────┼───────────┤
│         ↓                  ↓           │
│    ┌────────────────────────────┐      │
│    │      Shared Core Service   │      │
│    │  - Speech Recognition      │      │
│    │  - Command Engine          │      │
│    │  - Plugin System           │      │
│    └────────────┬───────────────┘      │
│                 │                      │
│    ┌────────────┴───────────────┐      │
│    │   AVA Engine (Optional)    │      │
│    │   - NLU (15-20 MB)         │      │
│    │   - LLM (on-demand)        │      │
│    └────────────────────────────┘      │
└───────────────────────────────────────┘
```

**Mobile Performance:**
- CPU: ⭐⭐⭐⭐⭐ (load only what's needed)
- Battery: ⭐⭐⭐⭐⭐ (AVA on demand)
- Memory: ⭐⭐⭐⭐ (shared core)
- Latency: ⭐⭐⭐⭐⭐ (in-process)

**Deployment Flexibility:**
- VoiceOS standalone (accessibility only)
- AVA standalone (AI assistant only)
- Combined (full experience)

---

## Primary Recommendation

### Architecture: Layered (Option C)

**Why:**
1. Supports all deployment scenarios
2. Optimal mobile performance
3. User choice on what to install
4. Clear component boundaries
5. In-process communication (no IPC overhead)

### Plugin System: Dual-Level

**Command Plugins (VoiceOS Core):**
```kotlin
interface CommandPlugin {
    val phrases: List<String>  // Exact patterns
    suspend fun execute(command: VoiceCommand): CommandResult
}
```

**Intent Plugins (AVA Engine):**
```kotlin
interface IntentPlugin {
    val intents: List<IntentDefinition>  // Semantic intents
    suspend fun execute(intent: ParsedIntent): IntentResult
}
```

### NLU Location: AVA Engine

**Reasoning:**
- Model is only 15-20 MB (not heavy)
- VoiceOS stays lightweight (<100 MB base)
- Avoids infrastructure duplication
- Clear separation of concerns

---

## Deployment Configurations

### Config 1: VoiceOS Standalone

**Use Case:** Accessibility-only, no AI

**Components:**
- Speech recognition ✅
- Command matching ✅
- Command plugins ✅
- NLU ❌
- LLM ❌

**APK Size:** ~150 MB (or ~80 MB without Vivoka)

---

### Config 2: AVA Standalone

**Use Case:** AI assistant without accessibility

**Components:**
- Speech recognition ✅
- NLU ✅
- LLM ✅
- Intent plugins ✅
- Accessibility Service ❌

**APK Size:** ~100 MB base + models on demand

---

### Config 3: Full Integration

**Use Case:** Complete voice + AI experience

**Components:** All ✅

**APK Size:**
- Dynamic Feature approach: ~150 MB base + ~50 MB AVA module
- Models downloaded on demand (15-20 MB NLU, 1-7 GB LLM per language)

---

## Mobile Best Practices Applied

### CPU Optimization
- VoiceOS commands: Local, <100ms
- NLU: On-device ONNX, 50-200ms
- LLM: On-device TVM, only when needed

### Battery Optimization
- Foreground Service for voice listening
- AVA engine loaded on demand
- LLM models lazy-loaded per language
- Only one LLM model in memory at a time

### Memory Management
- VoiceOS core: ~50 MB runtime
- NLU model: ~15-20 MB when loaded
- LLM model: 1-7 GB when active, unloaded when not needed
- Automatic model switching with cleanup

---

## Speech Recognition Architecture

### ASR Engine Priority Order

VoiceOS uses a cascading priority system for speech recognition engines:

| Priority | Engine | Type | Size | Notes |
|----------|--------|------|------|-------|
| 1 | **Vivoka** | Commercial | ~70 MB | Best accuracy, lowest latency |
| 2 | **Android TTS** | System | 0 MB | Uses system engine if available |
| 3 | **Vosk** | Open source | ~50 MB | Good offline accuracy |
| 4 | **Whisper** | Open source | ~40 MB | Best multilingual support |
| 5 | **Google** | Cloud | 0 MB | Fallback, requires network |

### ASR Discovery System

#### Storage Structure

ASR models are stored in a hidden folder accessible to both VoiceOS and AVA:

```
/storage/emulated/0/.ava/
└── ASR/
    ├── config.json           # Master configuration
    ├── vivoka/
    │   ├── en_us/            # Language models
    │   ├── es_es/
    │   └── manifest.json
    ├── vosk/
    │   ├── en_us/
    │   └── manifest.json
    ├── whisper/
    │   ├── base/
    │   └── manifest.json
    └── google/
        └── manifest.json     # Config only, no models
```

#### Discovery Algorithm

```kotlin
class ASRDiscoveryManager(private val context: Context) {

    private val asrBasePath = File(
        Environment.getExternalStorageDirectory(),
        ".ava/ASR"
    )

    /**
     * Discover available ASR engine in priority order
     * Returns first available engine or prompts for download
     */
    suspend fun discoverASREngine(): ASREngine {
        // Priority order
        val engines = listOf(
            ASREngineType.VIVOKA,
            ASREngineType.ANDROID_TTS,
            ASREngineType.VOSK,
            ASREngineType.WHISPER,
            ASREngineType.GOOGLE
        )

        for (engineType in engines) {
            val engine = checkEngine(engineType)
            if (engine != null) {
                return engine
            }
        }

        // No engine found - prompt for download
        return promptForDownload()
    }

    private suspend fun checkEngine(type: ASREngineType): ASREngine? {
        return when (type) {
            ASREngineType.VIVOKA -> checkVivokaEngine()
            ASREngineType.ANDROID_TTS -> checkAndroidTTS()
            ASREngineType.VOSK -> checkVoskEngine()
            ASREngineType.WHISPER -> checkWhisperEngine()
            ASREngineType.GOOGLE -> checkGoogleEngine()
        }
    }

    private fun checkVivokaEngine(): ASREngine? {
        val vivokaDir = File(asrBasePath, "vivoka")
        val manifest = File(vivokaDir, "manifest.json")

        if (manifest.exists()) {
            val config = parseManifest(manifest)
            if (config.isValid && hasRequiredModels(vivokaDir, config)) {
                return VivokaEngine(vivokaDir)
            }
        }
        return null
    }

    private fun checkAndroidTTS(): ASREngine? {
        // Check if system has speech recognition
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        val activities = context.packageManager.queryIntentActivities(intent, 0)

        return if (activities.isNotEmpty()) {
            AndroidTTSEngine(context)
        } else null
    }

    // Similar checks for Vosk, Whisper, Google...
}
```

#### Download Flow

```kotlin
/**
 * Prompt user to download ASR engine
 * Default: Vivoka en_us (best quality)
 */
suspend fun promptForDownload(): ASREngine {
    // 1. Request storage permission
    val hasPermission = requestStoragePermission()
    if (!hasPermission) {
        throw ASRPermissionDeniedException()
    }

    // 2. Show engine selection dialog
    val selectedEngine = showEngineSelectionDialog(
        default = ASREngineType.VIVOKA,
        language = "en_us"
    )

    // 3. Download from configured URL
    val downloadUrl = getDownloadUrl(selectedEngine)
    val result = downloadEngine(downloadUrl)

    // 4. Extract and validate
    extractToASRFolder(result)

    // 5. Return initialized engine
    return initializeEngine(selectedEngine)
}

private fun getDownloadUrl(engine: ASREngineType): String {
    val config = File(asrBasePath, "config.json")
    val urls = parseConfig(config).downloadUrls

    return urls[engine] ?: throw ASRDownloadUrlNotConfiguredException(engine)
}
```

#### Master Configuration File

`/storage/emulated/0/.ava/ASR/config.json`:

```json
{
    "version": "1.0",
    "defaultEngine": "vivoka",
    "defaultLanguage": "en_us",
    "downloadUrls": {
        "vivoka": {
            "baseUrl": {
                "release": "https://www.augmentalis.com/avanuevoiceosava/",
                "debug": "http://fs.dilonline.in/avanue_files/"
            },
            "auth": {
                "username": "avanuevoiceos",
                "password": "!AvA$Avanue123#"
            },
            "languages": {
                "en_us": {
                    "config": "English/english.json",
                    "resources": "English/en_voice_resource.zip"
                },
                "es_es": {
                    "config": "Spanish/spanish.json",
                    "resources": "Spanish/es_voice_resource.zip"
                },
                "fr_fr": {
                    "config": "French/french.json",
                    "resources": "French/fr_voice_resource.zip"
                },
                "ja_jp": {
                    "config": "Japanese/japanese.json",
                    "resources": "Japanese/jp_voice_resource.zip"
                }
            }
        },
        "vosk": {
            "baseUrl": "https://alphacephei.com/vosk/models/",
            "languages": {
                "en_us": "vosk-model-small-en-us-0.15.zip",
                "es_es": "vosk-model-small-es-0.42.zip"
            }
        },
        "whisper": {
            "baseUrl": "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/",
            "models": {
                "base": "ggml-base.bin",
                "small": "ggml-small.bin"
            }
        }
    },
    "enginePriority": ["vivoka", "android_tts", "vosk", "whisper", "google"],
    "nlu": {
        "baseUrl": {
            "release": "https://www.augmentalis.com/avanuevoiceosava/ava/nlu/",
            "debug": "http://fs.dilonline.in/avanue_files/ava/nlu/"
        },
        "models": {
            "default": {
                "model": "AVA-ONX-384-BASE-INT8.onnx",
                "vocab": "vocab.txt"
            }
        }
    },
    "llm": {
        "baseUrl": {
            "release": "https://www.augmentalis.com/avanuevoiceosava/ava/llm/",
            "debug": "http://fs.dilonline.in/avanue_files/ava/llm/"
        },
        "models": {
            "en": {
                "default": "AVA-GEM-2B-Q4.tar",
                "quality": "AVA-G3M-4B-Q4.tar"
            },
            "es": {
                "default": "AVA-FLOR-1B-Q4.tar"
            },
            "fr": {
                "default": "AVA-CROI-1B-Q4.tar"
            },
            "ja": {
                "default": "AVA-RINN-3B-Q4.tar"
            },
            "zh": {
                "default": "AVA-QWEN-2B-Q4.tar"
            }
        }
    },
    "firebaseRemoteConfig": {
        "enabled": true,
        "parameterPattern": "{lang}_{type}",
        "debugSuffix": "_debug"
    }
}
```

#### Firebase Remote Config Parameters

URLs are managed via Firebase Remote Config for dynamic updates:

| Parameter | Description | Example Value |
|-----------|-------------|---------------|
| `es_json` | Spanish config JSON | `https://www.augmentalis.com/avanuevoiceosava/Spanish/spanish.json` |
| `es_voice_resource` | Spanish resources ZIP | `https://www.augmentalis.com/avanuevoiceosava/Spanish/es_voice_resource.zip` |
| `es_json_debug` | Debug config | `http://fs.dilonline.in/avanue_files/spanish.json` |
| `es_voice_resource_debug` | Debug resources | `http://fs.dilonline.in/avanue_files/es_voice_resource.zip` |

#### Vivoka Folder Structure

After download and extraction, Vivoka resources are organized as:

```
context.filesDir/vsdk/data/csdk/asr/
├── acmod/          # Acoustic models (both grammar & free speech)
├── clc/            # Language components (dynamic grammar)
├── ctx/            # Context files (both grammar & free speech)
└── lm/             # Language models (free speech only)
```

**Temporary extraction path:**
```
context.filesDir/voice_temp/{lang}_voice_resource/data/csdk/asr/
```

---

### APK Bundling Strategy

To minimize APK size, ASR models are NOT bundled:

| Strategy | APK Impact | User Experience |
|----------|------------|-----------------|
| **No bundling** | ~80 MB APK | First-run download required |
| **Vivoka only** | ~150 MB APK | Works immediately |
| **All engines** | ~250 MB APK | Wasteful, user picks one |

**Recommendation:** No bundling (download on first run)

- Smaller initial download
- User chooses preferred engine
- Shared models between VoiceOS and AVA
- Easy updates without app update

---

## Implementation Roadmap

### Sprint 1: Stabilize Core
1. Extract shared core from VoiceOS
2. Define Command Plugin interface
3. Create Dynamic Feature Module structure
4. Maintain current IPC for backward compatibility

### Sprint 2: Define Plugin SDK
1. Publish Command Plugin SDK
2. Publish Intent Plugin SDK
3. Create example plugins
4. Write developer documentation

### Sprint 3: ASR Discovery System
1. Implement ASR discovery manager
2. Create download flow with permissions
3. Set up model hosting infrastructure
4. Test all engine integrations

### Sprint 4: AVA as Optional Module
1. Package AVA engine as Dynamic Feature Module
2. Implement on-demand download
3. Create seamless integration with core
4. Test all deployment scenarios

### Sprint 5: Unified Experience
1. Shared speech recognition across apps
2. Command-to-Intent fallback chain
3. Plugin discovery UI
4. Performance optimization

---

## Developer Documentation Requirements

### Chapter: Plugin Architecture & System Integration

**Sections to Write:**

1. **Overview**
   - Why dual-level plugins?
   - When to use Command vs Intent plugins
   - Architecture diagram

2. **Command Plugin Development (VoiceOS)**
   - Interface specification
   - Phrase pattern syntax
   - Execution context
   - Example: Screenshot plugin

3. **Intent Plugin Development (AVA)**
   - Interface specification
   - Intent definition format
   - Entity extraction
   - Example: Smart home plugin

4. **Choosing Plugin Type**
   - Decision flowchart
   - Performance comparison
   - Use case examples

5. **Mobile Performance Guide**
   - Battery considerations
   - Memory management
   - Background restrictions

6. **Deployment Configurations**
   - Standalone VoiceOS
   - Standalone AVA
   - Integrated

7. **Integration Architecture**
   - Layered design
   - Service communication
   - Model loading

8. **API Reference**
   - Command Plugin API
   - Intent Plugin API
   - Shared utilities

---

## Summary

| Decision | Recommendation | Reasoning |
|----------|----------------|-----------|
| **Plugin Location** | Both (dual-level) | Commands for speed, intents for flexibility |
| **NLU Placement** | AVA Engine | Only 15-20 MB, keeps VoiceOS light |
| **Architecture** | Layered with Dynamic Features | All deployment scenarios, optimal performance |
| **Default Config** | VoiceOS core (~150 MB) + optional AVA module | User choice, smaller base |

---

## Appendix: Size Analysis (Corrected)

### NLU Model (AVA)
- MobileBERT INT8 ONNX: 10-20 MB
- Vocabulary: ~460 KB
- **Total: ~15-20 MB** (downloaded on demand)

### LLM Models (AVA)
- Downloaded per language
- Only one loaded at a time
- 1-7 GB depending on model
- Examples: Gemma 2B (English), FLOR 1.3B (Spanish)

### VoiceOS
- Current APK: 262 MB
- With optimizations: ~150-180 MB
- Without Vivoka: ~80 MB

---

**Document Author:** VoiceOS/AVA Architecture Team
**Review Status:** Pending
**Next Steps:** Team review, stakeholder approval

