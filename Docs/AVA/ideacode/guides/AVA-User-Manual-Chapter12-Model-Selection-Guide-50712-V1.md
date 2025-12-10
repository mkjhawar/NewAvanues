# User Manual - Chapter 12: Model Selection Guide

**Last Updated:** 2025-11-30
**Author:** AVA Development Team
**Version:** 1.0

---

## Overview

This guide helps you select the optimal NLU (Natural Language Understanding) and LLM (Large Language Model) modules for your AVA deployment. The right combination depends on your device hardware, use case requirements, and language needs.

---

## Device Tier Overview (NEW - December 2025)

| Tier | Example Devices | RAM | Max Model | Languages |
|------|-----------------|-----|-----------|-----------|
| **High** | ARC 3, Pixel 8 Pro, Samsung S24 | 12GB+ | 4B | 100+ |
| **Mid** | Pixel 6, Samsung A54, Navigator 500 | 6-8GB | 2B | 100+ |
| **Low** | RealWear HMT-1, Budget phones | 2-4GB | 0.6B-1B | English |

### Recommended Configurations by Tier

| Priority | Device Tier | LLM | Embeddings | Total Size | Status |
|----------|-------------|-----|------------|------------|--------|
| 1 | High (ARC 3) | AVA-GE3-4B16 | AVA-768-Multi-INT8 | ~2.6GB | Ready |
| 2 | Mid-range | AVA-GE2-2B16 | AVA-384-Multi-INT8 | ~1.4GB | Ready |
| 3 | GGUF fallback | AVA-GE3N-E4B16 | AVA-384-Multi-INT8 | ~4.4GB | Ready |
| 4 | Low (HMT-1) | AVA-QW3-06B16 | AVA-384-Base-INT8 | ~450MB | Pending* |

*AVA-QW3-06B16 requires TVM compilation (missing .adm files)

---

## Quick Selection Matrix

### By Device RAM

| RAM | NLU Model | LLM Model | Performance |
|-----|-----------|-----------|-------------|
| **2GB** | AVA-384-BASE | AVA-QW3-06B16* | Basic |
| **3GB** | AVA-384-BASE | AVA-LL32-1B16 | Good |
| **4GB** | AVA-768-MULTI | AVA-QW3-17B16 | Very Good |
| **6GB** | AVA-768-MULTI | AVA-GE2-2B16 | Excellent |
| **8GB+** | AVA-768-MULTI | AVA-GE3-4B16 | Best |

*Requires TVM compilation

### By Device

| Device | Recommended NLU | Recommended LLM | Notes |
|--------|-----------------|-----------------|-------|
| RealWear HMT-1 | AVA-384-BASE | AVA-LL32-1B16 | 2-3GB RAM constraint |
| RealWear Arc 3 | AVA-768-MULTI | **AVA-GE3N-2B** | Gemma 3n recommended |
| RealWear Navigator 500/520 | AVA-768-MULTI | **AVA-GE3N-2B** | Gemma 3n recommended |
| Vuzix M400/Z100 | AVA-768-MULTI | **AVA-GE3N-4B** | Gemma 3n optimal |
| Vuzix M4000 | AVA-768-MULTI | **AVA-GE3N-4B** | Gemma 3n optimal |
| Rokid X-Craft | AVA-768-MULTI | **AVA-GE3N-2B** | Gemma 3n + Mali |
| Rokid Max Pro | AVA-768-MULTI | **AVA-GE3N-4B** | Gemma 3n optimal |
| Samsung Galaxy | AVA-768-MULTI | **AVA-GE3N-4B** | Gemma 3n optimal |
| iPhone 15/16 Pro | AVA-768-MULTI | **AVA-GE3N-4B** | Gemma 3n optimal |

---

## NLU Models (Intent Recognition)

NLU models understand what you're asking AVA to do. They classify your speech into intents like "open app", "send message", "navigate", etc.

### Available NLU Models

| Model | Dimensions | Languages | Size | Speed | Accuracy |
|-------|------------|-----------|------|-------|----------|
| **AVA-384-BASE** | 384 | English | 90 MB | Fast | Good |
| **AVA-768-MULTI** | 768 | 100+ | 380 MB | Medium | Excellent |

### When to Use Each

#### AVA-384-BASE (Recommended for constrained devices)

**Best for:**
- Devices with 2-3GB RAM
- English-only deployments
- Speed-critical applications
- RealWear HMT-1

**Performance:**
- Intent recognition: ~50ms
- Memory usage: ~200MB runtime
- Accuracy: 92% on AVA intents

```
Use Case: "Hey AVA, open camera"
→ Intent: OPEN_APP
→ Entity: camera
→ Processing: 50ms
```

#### AVA-768-MULTI (Recommended for multilingual)

**Best for:**
- Devices with 4GB+ RAM
- Multilingual deployments
- Higher accuracy requirements
- Enterprise deployments

**Performance:**
- Intent recognition: ~80ms
- Memory usage: ~450MB runtime
- Accuracy: 97% on AVA intents

**Supported Languages:**
English, Spanish, French, German, Italian, Portuguese, Chinese, Japanese, Korean, Arabic, Hindi, Russian, and 90+ more

```
Use Case: "Ouvrir la caméra" (French: Open the camera)
→ Intent: OPEN_APP
→ Entity: camera
→ Processing: 80ms
```

---

## LLM Models (Conversational AI)

LLM models power AVA's conversational abilities - answering questions, providing explanations, generating content, and handling complex requests.

**NEW:** You can now download LLM models directly from within AVA! See [Where to Get Models](#where-to-get-models) below.

### Available LLM Models

| Model | Parameters | Size | RAM Needed | Speed | Quality |
|-------|------------|------|------------|-------|---------|
| **AVA-QW3-06B16** | 0.6B | 0.33 GB | 1.5 GB | Fastest | Basic |
| **AVA-LL32-1B16** | 1.24B | 0.66 GB | 1.4 GB | Fast | Good |
| **AVA-QW3-17B16** | 1.7B | 0.92 GB | 1.8 GB | Medium | Very Good |
| **AVA-GE2-2B16** | 2.6B | 1.39 GB | 1.5 GB | Medium | Excellent |
| **AVA-GE3N-2B** | ~5B (2B eff) | 2.0 GB | 2.5 GB | Medium | Excellent+ |
| **AVA-LL32-3B16** | 3B | 1.69 GB | 2.5 GB | Slower | Very Good |
| **AVA-GE3N-4B** | ~8B (4B eff) | 3.0 GB | 3.5 GB | Medium | Best |
| **AVA-QW3-4B16** | 4B | 2.12 GB | 3.5 GB | Slowest | Best |

### Gemma 3n Models (NEW - Recommended)

Google's latest Gemma 3n models are now available via LiteRT runtime:

| Model | Effective Params | RAM | Best For |
|-------|-----------------|-----|----------|
| **AVA-GE3N-2B** | 2B | 2.5 GB | 4GB devices - Best quality at this tier |
| **AVA-GE3N-4B** | 4B | 3.5 GB | 6GB+ devices - Optimal quality |

**Why Gemma 3n?**
- +15-25% quality improvement over current models
- Selective layer activation (efficient inference)
- Excellent multilingual support (100+ languages)
- Native LiteRT support for fast inference

### Model Comparison by Category

#### Speed (Tokens/Second on Typical Device)

| Model | 2GB Device | 4GB Device | 8GB Device |
|-------|------------|------------|------------|
| AVA-QW3-06B16 | 25 t/s | 35 t/s | 40 t/s |
| AVA-LL32-1B16 | 20 t/s | 30 t/s | 35 t/s |
| AVA-QW3-17B16 | - | 22 t/s | 28 t/s |
| AVA-GE2-2B16 | - | 18 t/s | 25 t/s |
| AVA-QW3-4B16 | - | - | 15 t/s |

#### Quality (Benchmark Scores)

| Model | Reasoning | Instruction | Multilingual | Overall |
|-------|-----------|-------------|--------------|---------|
| AVA-QW3-06B16 | 45% | 50% | 70% | Basic |
| AVA-LL32-1B16 | 55% | 60% | 40% | Good |
| AVA-QW3-17B16 | 65% | 70% | 85% | Very Good |
| AVA-GE2-2B16 | 70% | 75% | 60% | Excellent |
| AVA-QW3-4B16 | 78% | 82% | 90% | Best |

### Detailed Model Profiles

#### AVA-QW3-06B16 (Ultra-Light)

**Specifications:**
- Parameters: 0.6 billion
- Quantization: 4-bit (q4f16_1)
- Context: 32K tokens
- Strengths: Multilingual (100+ languages)

**Best for:**
- Simple Q&A
- Quick commands
- Very constrained devices
- Battery-sensitive applications

**Example Performance:**
```
User: "What's the weather like?"
AVA: "I don't have real-time weather data, but you can
      check your weather app or ask me to open it."
Response time: ~0.8 seconds
```

#### AVA-LL32-1B16 (Balanced English)

**Specifications:**
- Parameters: 1.24 billion
- Quantization: 4-bit (q4f16_1)
- Context: 128K tokens
- Strengths: English-optimized, long context

**Best for:**
- English-focused deployments
- Long-context tasks
- Document summarization
- RealWear devices

**Strengths:**
- Excellent English comprehension
- Very long context window
- Well-suited for industrial use cases

**Example Performance:**
```
User: "Summarize the safety procedure I just read"
AVA: "The safety procedure covers: 1) PPE requirements
      including hard hat and safety glasses, 2) Lock-out
      tag-out steps before maintenance, 3) Emergency
      contact numbers and evacuation routes."
Response time: ~1.5 seconds
```

#### AVA-QW3-17B16 (Multilingual Balanced)

**Specifications:**
- Parameters: 1.7 billion
- Quantization: 4-bit (q4f16_1)
- Context: 32K tokens
- Strengths: Best multilingual at low resource

**Best for:**
- Multilingual deployments
- International teams
- Balanced speed/quality
- 4GB devices

**Supported Languages (Top Tier):**
English, Chinese, Spanish, French, German, Japanese, Korean, Arabic, Russian, Portuguese

**Example Performance:**
```
User: "解释这个错误代码" (Chinese: Explain this error code)
AVA: "这个错误代码表示连接超时。请检查网络连接或重试操作。"
     (Translation: This error code indicates connection timeout.
      Please check network connection or retry the operation.)
Response time: ~2 seconds
```

#### AVA-GE2-2B16 (High Quality)

**Specifications:**
- Parameters: 2.6 billion
- Quantization: 4-bit (q4f16_1)
- Context: 8K tokens
- Strengths: Best reasoning capability

**Best for:**
- Complex reasoning tasks
- Detailed explanations
- Technical documentation
- Quality-focused deployments

**Strengths:**
- Best reasoning among small models
- Excellent instruction following
- High-quality responses

**Example Performance:**
```
User: "Walk me through troubleshooting this hydraulic system"
AVA: "Let's systematically diagnose the hydraulic system:

      1. **Check fluid levels** - Low fluid is the most common
         cause. Look at the reservoir sight glass.

      2. **Inspect for leaks** - Check all connections, hoses,
         and cylinder seals for visible fluid.

      3. **Test pump pressure** - Connect a gauge to verify
         the pump is generating specified PSI.

      4. **Check relief valve** - A stuck-open relief valve
         will prevent pressure buildup.

      Which step would you like to start with?"
Response time: ~3 seconds
```

#### AVA-QW3-4B16 (Premium Quality)

**Specifications:**
- Parameters: 4 billion
- Quantization: 4-bit (q4f16_1)
- Context: 32K tokens
- Strengths: Highest quality multilingual

**Best for:**
- High-end devices (8GB+ RAM)
- Complex analysis
- Code generation
- Premium deployments

**Strengths:**
- Highest reasoning capability
- Best multilingual support
- Most coherent long responses
- Advanced code understanding

**Example Performance:**
```
User: "Analyze this wiring diagram and identify potential issues"
AVA: "Analyzing the wiring diagram, I've identified several
      potential issues:

      1. **Circuit 3 Overload Risk** - The 20A breaker feeds
         three 15A-rated outlets. Combined load could exceed
         breaker capacity during peak usage.

      2. **Missing Ground** - The junction box at location D4
         shows hot and neutral but no ground wire connection.

      3. **Wire Gauge Mismatch** - The run from panel to
         sub-panel uses 12 AWG but the sub-panel breaker is
         rated for 30A, requiring 10 AWG minimum.

      4. **Code Violation** - The outdoor receptacle lacks
         GFCI protection required by NEC 210.8.

      Priority: Address items 2 and 3 immediately for safety."
Response time: ~5 seconds
```

---

## Use Case Recommendations

### Industrial/Manufacturing

**Priority:** Reliability, speed, noise resistance

| Use Case | NLU | LLM | Reason |
|----------|-----|-----|--------|
| Hands-free operation | AVA-384-BASE | AVA-LL32-1B16 | Fast, reliable |
| Work instructions | AVA-768-MULTI | AVA-GE2-2B16 | Detailed guidance |
| Maintenance support | AVA-768-MULTI | AVA-QW3-17B16 | Multilingual crews |

### Healthcare

**Priority:** Accuracy, compliance, speed

| Use Case | NLU | LLM | Reason |
|----------|-----|-----|--------|
| Patient documentation | AVA-768-MULTI | AVA-GE2-2B16 | High accuracy |
| Quick lookups | AVA-384-BASE | AVA-LL32-1B16 | Fast response |
| Multilingual patients | AVA-768-MULTI | AVA-QW3-17B16 | Language support |

### Field Service

**Priority:** Offline capability, battery life, durability

| Use Case | NLU | LLM | Reason |
|----------|-----|-----|--------|
| Equipment diagnosis | AVA-768-MULTI | AVA-GE2-2B16 | Reasoning needed |
| Quick commands | AVA-384-BASE | AVA-QW3-06B16 | Battery efficient |
| Remote support | AVA-768-MULTI | AVA-QW3-4B16 | Detailed analysis |

### Warehouse/Logistics

**Priority:** Speed, accuracy, multilingual

| Use Case | NLU | LLM | Reason |
|----------|-----|-----|--------|
| Pick operations | AVA-384-BASE | AVA-QW3-06B16 | Fastest response |
| Inventory queries | AVA-768-MULTI | AVA-LL32-1B16 | Balanced |
| International teams | AVA-768-MULTI | AVA-QW3-17B16 | 100+ languages |

---

## Configuration Guide

### Setting Models in AVA

#### Android Configuration

```json
// ava-config.json
{
  "nlu": {
    "model": "AVA-768-MULTI",
    "path": "/sdcard/ava-ai-models/embeddings/AVA-768-MULTI-INT8.aon"
  },
  "llm": {
    "model": "AVA-GE2-2B16",
    "path": "/sdcard/ava-ai-models/llm/AVA-GE2-2B16/",
    "max_tokens": 512,
    "temperature": 0.7
  }
}
```

#### Device-Specific Profiles

**Profile: RealWear HMT-1 (Constrained)**
```json
{
  "profile": "constrained",
  "nlu": {"model": "AVA-384-BASE"},
  "llm": {"model": "AVA-LL32-1B16", "max_tokens": 256}
}
```

**Profile: Vuzix M400 (Balanced)**
```json
{
  "profile": "balanced",
  "nlu": {"model": "AVA-768-MULTI"},
  "llm": {"model": "AVA-GE2-2B16", "max_tokens": 512}
}
```

**Profile: Samsung Galaxy (Premium)**
```json
{
  "profile": "premium",
  "nlu": {"model": "AVA-768-MULTI"},
  "llm": {"model": "AVA-QW3-4B16", "max_tokens": 1024}
}
```

---

## Performance Optimization Tips

### 1. Match Model to Task Complexity

```
Simple command: "Open camera"
→ Use: AVA-QW3-06B16 (fastest)

Complex query: "Explain troubleshooting steps for error E-502"
→ Use: AVA-GE2-2B16 (better reasoning)
```

### 2. Reduce Max Tokens for Speed

```json
// Faster responses (shorter answers)
{"max_tokens": 256}

// More detailed responses (slower)
{"max_tokens": 1024}
```

### 3. Use Appropriate Context Length

| Model | Max Context | Recommended |
|-------|-------------|-------------|
| AVA-QW3-06B16 | 32K | 2K |
| AVA-LL32-1B16 | 128K | 4K |
| AVA-GE2-2B16 | 8K | 4K |
| AVA-QW3-4B16 | 32K | 8K |

### 4. Pre-warm Models at Startup

```kotlin
// Load models at app start, not first query
avaEngine.preloadModels()
```

### 5. Monitor Memory Usage

```bash
# Check model memory on device
adb shell dumpsys meminfo com.augmentalis.ava | grep "TOTAL"
```

---

## Troubleshooting

### "Model too slow"

**Solutions:**
1. Switch to smaller model (e.g., AVA-QW3-17B16 → AVA-LL32-1B16)
2. Reduce max_tokens
3. Close background apps
4. Check GPU is being utilized

### "Out of memory"

**Solutions:**
1. Use smaller model
2. Reduce context length
3. Enable model offloading
4. Restart device to clear RAM

### "Poor response quality"

**Solutions:**
1. Upgrade to larger model
2. Increase max_tokens
3. Adjust temperature (0.3-0.5 for factual, 0.7-0.9 for creative)
4. Check NLU is correctly classifying intent

### "Multilingual not working"

**Solutions:**
1. Switch to AVA-768-MULTI NLU
2. Use Qwen models (AVA-QW3-*) for better multilingual LLM
3. Verify language is in supported list

---

## Model Storage Locations

```
/sdcard/ava-ai-models/
├── embeddings/                    # NLU models
│   ├── AVA-384-BASE-INT8.aon     # 90 MB
│   └── AVA-768-MULTI-INT8.aon    # 380 MB
└── llm/                          # LLM models
    ├── AVA-QW3-06B16/            # 330 MB
    ├── AVA-LL32-1B16/            # 660 MB
    ├── AVA-QW3-17B16/            # 920 MB
    ├── AVA-GE2-2B16/             # 1.4 GB
    ├── AVA-LL32-3B16/            # 1.7 GB
    └── AVA-QW3-4B16/             # 2.1 GB
```

---

## Model Security & Integrity (NEW - Dec 2025)

AVA now includes **automatic model verification** to ensure your models haven't been corrupted or tampered with.

### How It Works

When you load a model, AVA:
1. **Checks for a checksum file** (e.g., `AVA-GE2-2B16.sha256`)
2. **Calculates the model's hash** using SHA-256
3. **Compares with the expected checksum**
4. **Blocks loading if mismatch** is detected

### What You'll See

**✅ Verified Model:**
```
Model integrity verified: AVA-GE2-2B16
Loading model...
```

**⚠️ No Checksum Found:**
```
Model has no checksum - skipping verification: AVA-GE2-2B16
Loading model anyway...
```

**❌ Checksum Mismatch:**
```
Model checksum mismatch! Model may be corrupted or tampered.
Model loading blocked for security.
```

### When This Happens

**Checksum mismatch usually means:**
- File was corrupted during download
- Storage corruption on your device
- Incomplete file transfer
- (Rare) Tampered model file

**What to do:**
1. Delete the model from `/sdcard/ava-ai-models/llm/`
2. Re-download from official source
3. Verify storage isn't failing (`Settings > Storage > Check`)

### For Developers

Models downloaded from the official AVA model server automatically include `.sha256` checksum files. If you're building custom models, generate checksums with:

```bash
sha256sum AVA-GE2-2B16/AVA-GE2-2B16.adm > AVA-GE2-2B16.sha256
```

---

## Where to Get Models

### Automatic Download (Recommended - NEW!)

**Best for:** Most users, easiest method

**Available Models for Download:**

| Model | Download Size | Description |
|-------|--------------|-------------|
| **QWEN2-1.5B** | 1GB | Fastest, multilingual, good for older devices |
| **AVA-GE2-2B16** | 1.5GB | Recommended for most users, balanced quality |
| **AVA-GE3-4B16** | 1.7GB | Better quality, needs 6GB+ RAM |
| **PHI3-MINI** | 2.3GB | Advanced features, power users, needs 8GB+ RAM |

**How to Download:**
1. Open AVA → Settings → Model Management
2. Switch to "Available" tab
3. Tap model name to see details
4. Tap "Download" button
5. Wait for download to complete
6. Model appears in "Downloaded" tab

**Features:**
- Progress tracking with speed display
- Automatic resume if interrupted
- Integrity verification (SHA-256)
- Concurrent downloads (up to 2)
- Works over WiFi or mobile data

See [User Manual Chapter 10: Model Installation](User-Manual-Chapter10-Model-Installation.md#method-3-automatic-download-new---available-now) for complete download instructions.

---

### Manual Installation

**Best for:** Advanced users, testing custom models

**Methods:**
- **USB Transfer:** Use ADB to copy models from computer
- **Cloud Storage:** Download from Google Drive/Dropbox
- **SD Card:** Copy models to external storage

See [User Manual Chapter 10: Model Installation](User-Manual-Chapter10-Model-Installation.md) for detailed manual installation steps.

---

## Summary: Quick Decision Tree

```
START
│
├─ Device RAM < 3GB?
│  ├─ YES → NLU: AVA-384-BASE, LLM: AVA-LL32-1B16
│  └─ NO ↓
│
├─ Need multilingual?
│  ├─ YES → NLU: AVA-768-MULTI
│  └─ NO → NLU: AVA-384-BASE
│
├─ Device RAM?
│  ├─ 3-4GB → LLM: AVA-QW3-17B16 (multilingual) or AVA-LL32-1B16 (English)
│  ├─ 4-6GB → LLM: AVA-GE2-2B16 (quality) or AVA-QW3-17B16 (multilingual)
│  └─ 8GB+ → LLM: AVA-QW3-4B16 (best quality)
│
└─ DONE
```

---

## Related Documentation

- **Chapter 11:** Model Installation Guide
- **Chapter 13:** Performance Tuning
- **Developer Manual Chapter 54:** Cross-GPU Model Compilation
- **FORMAT-REGISTRY.md:** Technical format specifications

---

**Document Version:** 1.4 (Added automatic download information)
**Last Updated:** 2025-12-06

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.4 | 2025-12-06 | Added "Where to Get Models" section with automatic download details and available models table |
| 1.3 | 2025-12-04 | Updated model recommendations |
| 1.0 | 2025-11-30 | Initial release |
