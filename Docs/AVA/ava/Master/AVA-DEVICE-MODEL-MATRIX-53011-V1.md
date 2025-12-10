# AVA Device-Model Optimization Matrix

**Last Updated:** 2025-12-01
**Version:** 1.1
**Author:** AVA Development Team
**Audience:** End Users, Administrators

---

## Executive Summary

This document provides optimal NLU + LLM configurations for every supported AVA device, with both **Base (English)** and **Multilingual** options. **Gemma 3n is now available** via LiteRT runtime and is the recommended choice for devices with 4GB+ RAM.

---

## Device Inventory

### RealWear Devices

| Device | RAM | GPU | Year | Target Use |
|--------|-----|-----|------|------------|
| HMT-1 | 2GB | Adreno 506 | 2017 | Legacy industrial |
| HMT-1Z1 | 3GB | Adreno 506 | 2019 | Hazardous zones |
| Navigator 500 | 4GB | Adreno 619 | 2022 | Modern industrial |
| Navigator 520 | 4GB | Adreno 619 | 2023 | Enhanced Nav 500 |
| Navigator Z1 | 4GB | Adreno 619 | 2023 | Hazardous zones |
| Arc 3 | 4GB | Adreno 619 | 2024 | Premium industrial |

### Vuzix Devices

| Device | RAM | GPU | Year | Target Use |
|--------|-----|-----|------|------------|
| M400 | 6GB | Snapdragon XR1 | 2020 | Enterprise AR |
| M400C | 6GB | Snapdragon XR1 | 2021 | Consumer variant |
| Z100 | 6GB | Snapdragon XR2 | 2023 | Premium AR |
| M4000 | 6GB | Snapdragon XR2 | 2024 | Latest enterprise |

### Rokid Devices

| Device | RAM | GPU | Year | Target Use |
|--------|-----|-----|------|------------|
| X-Craft | 4GB | Mali-G57 | 2023 | Industrial AR |
| Max Pro | 6GB | Mali-G78 | 2024 | Premium AR |

### Mobile Devices

| Device | RAM | GPU | Year | Target Use |
|--------|-----|-----|------|------------|
| Samsung S23 | 8GB | Adreno 740 | 2023 | Flagship phone |
| Samsung S24 | 8-12GB | Adreno 750 | 2024 | Latest flagship |
| iPhone 15 Pro | 8GB | Apple GPU | 2023 | iOS flagship |
| iPhone 16 Pro | 12GB | Apple GPU | 2024 | Latest iOS |

---

## Optimal Configurations by Device

### RealWear HMT-1 / HMT-1Z1 (2-3GB RAM)

**Constraint:** Most limited device, must prioritize efficiency

#### Base (English-Focused)
```json
{
  "profile": "hmt1-base",
  "nlu": {
    "model": "AVA-384-BASE",
    "runtime": "ONNX",
    "memory": "200 MB"
  },
  "llm": {
    "model": "AVA-LL32-1B16",
    "runtime": "MLC-LLM",
    "memory": "1.37 GB"
  },
  "total_memory": "1.57 GB",
  "free_headroom": "0.4-1.4 GB"
}
```

**Analysis:**
- AVA-384-BASE: Fast intent recognition (~50ms), English-optimized
- AVA-LL32-1B16: Best quality-per-GB for English, 128K context
- Total ~1.6GB leaves adequate headroom on 3GB variant
- On 2GB variant, may need to reduce context length

#### Multilingual
```json
{
  "profile": "hmt1-multilingual",
  "nlu": {
    "model": "AVA-384-BASE",
    "runtime": "ONNX",
    "memory": "200 MB",
    "note": "768-MULTI too large for this device"
  },
  "llm": {
    "model": "AVA-QW3-06B16",
    "runtime": "MLC-LLM",
    "memory": "0.8 GB"
  },
  "total_memory": "1.0 GB",
  "free_headroom": "1.0-2.0 GB"
}
```

**Analysis:**
- AVA-QW3-06B16: Only viable multilingual option at 0.6B params
- Quality is basic but covers 100+ languages
- Cannot fit AVA-768-MULTI NLU, use 384-BASE with multilingual LLM

**Gemma 3n Verdict:** NOT RECOMMENDED - Insufficient RAM

---

### RealWear Navigator 500/520/Z1 (4GB RAM)

**Constraint:** Good RAM but moderate GPU

#### Base (English-Focused)
```json
{
  "profile": "navigator-base",
  "nlu": {
    "model": "AVA-768-MULTI",
    "runtime": "ONNX",
    "memory": "450 MB"
  },
  "llm": {
    "model": "AVA-GE2-2B16",
    "runtime": "MLC-LLM",
    "memory": "1.52 GB"
  },
  "total_memory": "1.97 GB",
  "free_headroom": "~1.5 GB"
}
```

**Analysis:**
- AVA-768-MULTI: Better intent accuracy (97% vs 92%)
- AVA-GE2-2B16: Excellent reasoning, best for technical guidance
- Good balance of quality and speed (~18 t/s)

#### Multilingual
```json
{
  "profile": "navigator-multilingual",
  "nlu": {
    "model": "AVA-768-MULTI",
    "runtime": "ONNX",
    "memory": "450 MB"
  },
  "llm": {
    "model": "AVA-QW3-17B16",
    "runtime": "MLC-LLM",
    "memory": "1.8 GB"
  },
  "total_memory": "2.25 GB",
  "free_headroom": "~1.25 GB"
}
```

**Analysis:**
- AVA-QW3-17B16: Excellent multilingual (85%+ in 100 languages)
- Good reasoning capability (65%)
- Slightly faster than GE2-2B16 (~22 t/s)

#### With Gemma 3n (Recommended)
```json
{
  "profile": "navigator-gemma3n",
  "nlu": {
    "model": "AVA-768-MULTI",
    "runtime": "ONNX",
    "memory": "450 MB"
  },
  "llm": {
    "model": "AVA-GE3N-2B",
    "runtime": "LiteRT",
    "memory": "2.0 GB"
  },
  "total_memory": "2.45 GB",
  "free_headroom": "~1.0 GB"
}
```

**Gemma 3n Analysis:**
- E2B: GOOD FIT - 2GB model + 450MB NLU leaves ~1GB headroom
- E4B: RISKY - 3GB model would leave minimal headroom
- LiteRT runtime available
- Performance: ~18-20 t/s with GPU delegate

---

### RealWear Arc 3 (4GB RAM)

**Note:** Same specs as Navigator but marketed as premium

#### Base (English-Focused)
```json
{
  "profile": "arc3-base",
  "nlu": {
    "model": "AVA-768-MULTI",
    "runtime": "ONNX",
    "memory": "450 MB"
  },
  "llm": {
    "model": "AVA-GE2-2B16",
    "runtime": "MLC-LLM",
    "memory": "1.52 GB"
  },
  "total_memory": "1.97 GB",
  "performance": "18 t/s"
}
```

#### Multilingual
```json
{
  "profile": "arc3-multilingual",
  "nlu": {
    "model": "AVA-768-MULTI",
    "runtime": "ONNX",
    "memory": "450 MB"
  },
  "llm": {
    "model": "AVA-QW3-17B16",
    "runtime": "MLC-LLM",
    "memory": "1.8 GB"
  },
  "total_memory": "2.25 GB",
  "performance": "22 t/s"
}
```

#### With Gemma 3n (Recommended)
```json
{
  "profile": "arc3-gemma3n",
  "nlu": {
    "model": "AVA-768-MULTI",
    "runtime": "ONNX",
    "memory": "450 MB"
  },
  "llm": {
    "model": "AVA-GE3N-2B",
    "runtime": "LiteRT",
    "memory": "2.0 GB"
  },
  "total_memory": "2.45 GB",
  "performance": "~20 t/s"
}
```

**Gemma 3n Verdict:** RECOMMENDED for Arc 3 - LiteRT runtime now available

---

### Vuzix M400 / M400C (6GB RAM)

**Advantage:** Generous RAM allows larger models

#### Base (English-Focused)
```json
{
  "profile": "m400-base",
  "nlu": {
    "model": "AVA-768-MULTI",
    "runtime": "ONNX",
    "memory": "450 MB"
  },
  "llm": {
    "model": "AVA-LL32-3B16",
    "runtime": "MLC-LLM",
    "memory": "2.5 GB"
  },
  "total_memory": "2.95 GB",
  "free_headroom": "~2.5 GB"
}
```

**Analysis:**
- AVA-LL32-3B16: Highest quality English responses
- Long 128K context for document processing
- Plenty of headroom for multitasking

#### Multilingual
```json
{
  "profile": "m400-multilingual",
  "nlu": {
    "model": "AVA-768-MULTI",
    "runtime": "ONNX",
    "memory": "450 MB"
  },
  "llm": {
    "model": "AVA-GE2-2B16",
    "runtime": "MLC-LLM",
    "memory": "1.52 GB"
  },
  "total_memory": "1.97 GB",
  "free_headroom": "~3.5 GB"
}
```

**Analysis:**
- GE2-2B16 offers better multilingual than LL32
- Plenty of RAM for concurrent operations
- Could upgrade to QW3-4B16 if quality needed

#### With Gemma 3n (Optimal - Available)
```json
{
  "profile": "m400-gemma3n",
  "nlu": {
    "model": "AVA-768-MULTI",
    "runtime": "ONNX",
    "memory": "450 MB"
  },
  "llm": {
    "model": "AVA-GE3N-4B",
    "runtime": "LiteRT",
    "memory": "3.0 GB"
  },
  "total_memory": "3.45 GB",
  "free_headroom": "~2.0 GB"
}
```

**Gemma 3n Verdict:** STRONGLY RECOMMENDED - Now available via LiteRT

---

### Vuzix Z100 / M4000 (6GB RAM, XR2)

**Advantage:** Latest Snapdragon XR2 with best GPU performance

#### Base (English-Focused)
```json
{
  "profile": "z100-base",
  "nlu": {
    "model": "AVA-768-MULTI",
    "runtime": "ONNX",
    "memory": "450 MB"
  },
  "llm": {
    "model": "AVA-LL32-3B16",
    "runtime": "MLC-LLM",
    "memory": "2.5 GB"
  },
  "total_memory": "2.95 GB",
  "performance": "25 t/s"
}
```

#### Multilingual (Premium)
```json
{
  "profile": "z100-multilingual-premium",
  "nlu": {
    "model": "AVA-768-MULTI",
    "runtime": "ONNX",
    "memory": "450 MB"
  },
  "llm": {
    "model": "AVA-QW3-4B16",
    "runtime": "MLC-LLM",
    "memory": "3.5 GB"
  },
  "total_memory": "3.95 GB",
  "free_headroom": "~1.5 GB"
}
```

**Analysis:**
- QW3-4B16: Best overall quality (78% reasoning, 90% multilingual)
- XR2 GPU provides fastest inference (~25 t/s)
- Premium choice for enterprise deployments

#### With Gemma 3n (Optimal - Available)
```json
{
  "profile": "z100-gemma3n-premium",
  "nlu": {
    "model": "AVA-768-MULTI",
    "runtime": "ONNX",
    "memory": "450 MB"
  },
  "llm": {
    "model": "AVA-GE3N-4B",
    "runtime": "LiteRT",
    "memory": "3.0 GB"
  },
  "total_memory": "3.45 GB",
  "performance": "~25 t/s"
}
```

**Gemma 3n Verdict:** OPTIMAL CHOICE - E4B now available via LiteRT

---

### Rokid X-Craft (4GB RAM, Mali GPU)

**Note:** Requires Mali OpenCL compiled models (llm-mali/)

#### Base (English-Focused)
```json
{
  "profile": "xcraft-base",
  "nlu": {
    "model": "AVA-768-MULTI",
    "runtime": "ONNX",
    "memory": "450 MB"
  },
  "llm": {
    "model": "AVA-GE2-2B16-MALI",
    "runtime": "MLC-LLM",
    "memory": "1.52 GB",
    "compiled_for": "Mali OpenCL"
  },
  "total_memory": "1.97 GB"
}
```

#### Multilingual
```json
{
  "profile": "xcraft-multilingual",
  "nlu": {
    "model": "AVA-768-MULTI",
    "runtime": "ONNX",
    "memory": "450 MB"
  },
  "llm": {
    "model": "AVA-QW3-17B16-MALI",
    "runtime": "MLC-LLM",
    "memory": "1.8 GB",
    "compiled_for": "Mali OpenCL"
  },
  "total_memory": "2.25 GB"
}
```

#### With Gemma 3n (Recommended - Available)
```json
{
  "profile": "xcraft-gemma3n",
  "nlu": {
    "model": "AVA-768-MULTI",
    "runtime": "ONNX",
    "memory": "450 MB"
  },
  "llm": {
    "model": "AVA-GE3N-2B",
    "runtime": "LiteRT",
    "memory": "2.0 GB",
    "note": "LiteRT has native Mali support"
  },
  "total_memory": "2.45 GB"
}
```

**Gemma 3n Verdict:** RECOMMENDED - E2B available with native Mali support

---

### Rokid Max Pro (6GB RAM, Mali-G78)

#### Multilingual (Premium)
```json
{
  "profile": "maxpro-multilingual",
  "nlu": {
    "model": "AVA-768-MULTI",
    "runtime": "ONNX",
    "memory": "450 MB"
  },
  "llm": {
    "model": "AVA-QW3-4B16-MALI",
    "runtime": "MLC-LLM",
    "memory": "3.5 GB"
  },
  "total_memory": "3.95 GB"
}
```

#### With Gemma 3n (Optimal - Available)
```json
{
  "profile": "maxpro-gemma3n",
  "nlu": {
    "model": "AVA-768-MULTI",
    "runtime": "ONNX",
    "memory": "450 MB"
  },
  "llm": {
    "model": "AVA-GE3N-4B",
    "runtime": "LiteRT",
    "memory": "3.0 GB"
  },
  "total_memory": "3.45 GB"
}
```

**Gemma 3n Verdict:** STRONGLY RECOMMENDED - E4B now available via LiteRT

---

### Samsung Galaxy S23/S24 (8-12GB RAM)

#### Premium Configuration
```json
{
  "profile": "galaxy-premium",
  "nlu": {
    "model": "AVA-768-MULTI",
    "runtime": "ONNX",
    "memory": "450 MB"
  },
  "llm": {
    "model": "AVA-QW3-4B16",
    "runtime": "MLC-LLM",
    "memory": "3.5 GB"
  },
  "total_memory": "3.95 GB",
  "performance": "30+ t/s"
}
```

#### With Gemma 3n (Optimal - Available)
```json
{
  "profile": "galaxy-gemma3n",
  "nlu": {
    "model": "AVA-768-MULTI",
    "runtime": "ONNX",
    "memory": "450 MB"
  },
  "llm": {
    "model": "AVA-GE3N-4B",
    "runtime": "LiteRT",
    "memory": "3.0 GB"
  },
  "total_memory": "3.45 GB",
  "performance": "~35 t/s"
}
```

**Gemma 3n Verdict:** OPTIMAL - E4B available with flagship performance

---

### iPhone 15/16 Pro (8-12GB RAM)

#### Premium Configuration
```json
{
  "profile": "iphone-premium",
  "nlu": {
    "model": "AVA-768-MULTI",
    "runtime": "CoreML",
    "memory": "450 MB"
  },
  "llm": {
    "model": "AVA-QW3-4B16-IOS",
    "runtime": "MLC-LLM (Metal)",
    "memory": "3.5 GB"
  },
  "total_memory": "3.95 GB",
  "performance": "35+ t/s"
}
```

#### With Gemma 3n (Optimal - Available)
```json
{
  "profile": "iphone-gemma3n",
  "nlu": {
    "model": "AVA-768-MULTI",
    "runtime": "CoreML",
    "memory": "450 MB"
  },
  "llm": {
    "model": "AVA-GE3N-4B",
    "runtime": "LiteRT (CoreML)",
    "memory": "3.0 GB"
  },
  "total_memory": "3.45 GB",
  "performance": "~40 t/s"
}
```

**Gemma 3n Verdict:** OPTIMAL - E4B available with Apple Neural Engine

---

## Master Recommendation Matrix

### Current (MLC-LLM Runtime)

| Device | RAM | Base NLU | Base LLM | Multi NLU | Multi LLM |
|--------|-----|----------|----------|-----------|-----------|
| **HMT-1** | 2-3GB | AVA-384-BASE | AVA-LL32-1B16 | AVA-384-BASE | AVA-QW3-06B16 |
| **Navigator 5xx** | 4GB | AVA-768-MULTI | AVA-GE2-2B16 | AVA-768-MULTI | AVA-QW3-17B16 |
| **Arc 3** | 4GB | AVA-768-MULTI | AVA-GE2-2B16 | AVA-768-MULTI | AVA-QW3-17B16 |
| **Vuzix M400** | 6GB | AVA-768-MULTI | AVA-LL32-3B16 | AVA-768-MULTI | AVA-GE2-2B16 |
| **Vuzix Z100** | 6GB | AVA-768-MULTI | AVA-LL32-3B16 | AVA-768-MULTI | AVA-QW3-4B16 |
| **Rokid X-Craft** | 4GB | AVA-768-MULTI | AVA-GE2-2B16* | AVA-768-MULTI | AVA-QW3-17B16* |
| **Rokid Max Pro** | 6GB | AVA-768-MULTI | AVA-LL32-3B16* | AVA-768-MULTI | AVA-QW3-4B16* |
| **Samsung S23/24** | 8-12GB | AVA-768-MULTI | AVA-QW3-4B16 | AVA-768-MULTI | AVA-QW3-4B16 |
| **iPhone 15/16** | 8-12GB | AVA-768-MULTI | AVA-QW3-4B16† | AVA-768-MULTI | AVA-QW3-4B16† |

*Requires Mali-compiled models
†Requires iOS-compiled models

### Gemma 3n (Now Available via LiteRT)

| Device | RAM | Gemma 3n | Model | Improvement vs Base |
|--------|-----|----------|-------|---------------------|
| **HMT-1** | 2-3GB | No | N/A | Stick with current |
| **Navigator 5xx** | 4GB | E2B | AVA-GE3N-2B | +15% quality, same speed |
| **Arc 3** | 4GB | E2B | AVA-GE3N-2B | +15% quality, same speed |
| **Vuzix M400** | 6GB | E4B | AVA-GE3N-4B | +25% quality, +10% speed |
| **Vuzix Z100** | 6GB | E4B | AVA-GE3N-4B | +25% quality, +15% speed |
| **Rokid X-Craft** | 4GB | E2B | AVA-GE3N-2B | +15% quality, native Mali |
| **Rokid Max Pro** | 6GB | E4B | AVA-GE3N-4B | +25% quality, native Mali |
| **Samsung S23/24** | 8-12GB | E4B | AVA-GE3N-4B | +20% quality, +20% speed |
| **iPhone 15/16** | 8-12GB | E4B | AVA-GE3N-4B | +20% quality, +25% speed |

---

## Gemma 3n Analysis

### Why Gemma 3n?

1. **Google's Edge Optimization:** Specifically designed for mobile/edge deployment
2. **Selective Layer Activation:** "E2B" means effective 2B (actual ~5B with smart activation)
3. **Multilingual Excellence:** 100+ languages out of the box
4. **Native LiteRT Support:** Fastest inference on Android/iOS
5. **Latest Knowledge:** June 2025 training data

### AVA-GE3N-2B vs Current Best

| Metric | AVA-GE2-2B16 | AVA-QW3-17B16 | AVA-GE3N-2B |
|--------|--------------|---------------|--------------|
| Parameters | 2.6B | 1.7B | ~5B (2B effective) |
| RAM | 1.5 GB | 1.8 GB | 2.0 GB |
| Reasoning | 70% | 65% | **75%** |
| Multilingual | 60% | 85% | **92%** |
| Speed (4GB) | 18 t/s | 22 t/s | **18-20 t/s** |
| Runtime | MLC-LLM | MLC-LLM | **LiteRT** |

### Implementation Priority

```
Priority 1: Vuzix Z100/M4000 + Gemma 3n E4B
- Highest capability devices
- Most benefit from quality upgrade
- Premium enterprise market

Priority 2: Vuzix M400 + Gemma 3n E4B
- Large installed base
- 6GB RAM comfortable fit
- Good ROI

Priority 3: Arc 3/Navigator + Gemma 3n E2B
- Most common RealWear devices
- E2B fits well at 4GB
- Industrial market leader

Priority 4: Rokid devices + Gemma 3n
- Growing market
- Native Mali support in LiteRT
- E2B for X-Craft, E4B for Max Pro

Skip: HMT-1
- Legacy device
- Insufficient RAM
- Current models adequate
```

---

## Quick Reference Cards

### RealWear Quick Reference

```
┌─────────────────────────────────────────────────────┐
│ REALWEAR DEVICE CONFIGURATIONS                      │
├─────────────────────────────────────────────────────┤
│                                                     │
│ HMT-1 (2-3GB):                                     │
│   Base:  AVA-384-BASE + AVA-LL32-1B16              │
│   Multi: AVA-384-BASE + AVA-QW3-06B16              │
│   Gemma: NOT SUPPORTED                              │
│                                                     │
│ Navigator 500/520 (4GB):                           │
│   Base:  AVA-768-MULTI + AVA-GE2-2B16              │
│   Multi: AVA-768-MULTI + AVA-QW3-17B16             │
│   Gemma: AVA-768-MULTI + AVA-GE3N-2B (recommended) │
│                                                     │
│ Arc 3 (4GB):                                       │
│   Base:  AVA-768-MULTI + AVA-GE2-2B16              │
│   Multi: AVA-768-MULTI + AVA-QW3-17B16             │
│   Gemma: AVA-768-MULTI + AVA-GE3N-2B (recommended) │
│                                                     │
└─────────────────────────────────────────────────────┘
```

### Vuzix Quick Reference

```
┌─────────────────────────────────────────────────────┐
│ VUZIX DEVICE CONFIGURATIONS                         │
├─────────────────────────────────────────────────────┤
│                                                     │
│ M400/M400C (6GB):                                  │
│   Base:  AVA-768-MULTI + AVA-LL32-3B16             │
│   Multi: AVA-768-MULTI + AVA-GE2-2B16              │
│   Gemma: AVA-768-MULTI + AVA-GE3N-4B (optimal)     │
│                                                     │
│ Z100/M4000 (6GB, XR2):                             │
│   Base:  AVA-768-MULTI + AVA-LL32-3B16             │
│   Multi: AVA-768-MULTI + AVA-QW3-4B16              │
│   Gemma: AVA-768-MULTI + AVA-GE3N-4B (optimal)     │
│                                                     │
└─────────────────────────────────────────────────────┘
```

### Rokid Quick Reference

```
┌─────────────────────────────────────────────────────┐
│ ROKID DEVICE CONFIGURATIONS                         │
├─────────────────────────────────────────────────────┤
│                                                     │
│ X-Craft (4GB, Mali):                               │
│   Base:  AVA-768-MULTI + AVA-GE2-2B16-MALI         │
│   Multi: AVA-768-MULTI + AVA-QW3-17B16-MALI        │
│   Gemma: AVA-768-MULTI + AVA-GE3N-2B (recommended) │
│                                                     │
│ Max Pro (6GB, Mali):                               │
│   Base:  AVA-768-MULTI + AVA-LL32-3B16-MALI        │
│   Multi: AVA-768-MULTI + AVA-QW3-4B16-MALI         │
│   Gemma: AVA-768-MULTI + AVA-GE3N-4B (optimal)     │
│                                                     │
│ Note: All Rokid models require Mali-compiled       │
│ variants (llm-mali/ directory)                     │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.2 | 2025-11-30 | Gemma 3n marked as available via LiteRT runtime |
| 1.1 | 2025-12-01 | Standardized all model names to AVA naming convention |
| 1.0 | 2025-11-30 | Initial release with all device configurations |

---

## Related Documentation

- **Developer-DEVICE-MODEL-MATRIX.md** - Developer reference with original model cross-references
- **User-Manual-Chapter12-Model-Selection-Guide.md** - Quick selection guide

---

**Document Version:** 1.2
**Last Updated:** 2025-11-30
