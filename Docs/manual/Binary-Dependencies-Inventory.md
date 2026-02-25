# Binary Dependencies Inventory

**Policy**: Files under 100MB are tracked in git (GitLab blob limit). Files over 100MB are stored in GitLab Package Registry and downloaded via `scripts/setup-sdk.sh`.

**Checksums**: `scripts/checksums.sha256` (verify with `shasum -a 256 -c scripts/checksums.sha256`)

---

## Git-Tracked Binaries (~2.8GB total)

### Vivoka SDK (~2.4GB)

| File | Size | Purpose |
|------|------|---------|
| `vivoka/vsdk-6.0.0.aar` | 128K | Vivoka SDK core AAR |
| `vivoka/vsdk-csdk-asr-2.0.0.aar` | 36MB | Vivoka ASR engine AAR |
| `vivoka/vsdk-csdk-core-1.0.1.aar` | 33MB | Vivoka CSDK core AAR |
| `vivoka/Android/libs/vsdk-6.0.0.jar` | 98K | Vivoka SDK JAR (extracted from AAR) |
| `vivoka/Android/libs/vsdk-csdk-asr-2.0.0.jar` | 7K | Vivoka ASR JAR |
| `vivoka/Android/libs/vsdk-csdk-core-1.0.1.jar` | 1K | Vivoka CSDK core JAR |
| `vivoka/Android/src/main/jniLibs/arm64-v8a/*.so` | ~59MB | Native libs (ARM64) |
| `vivoka/Android/src/main/jniLibs/armeabi-v7a/*.so` | ~56MB | Native libs (ARMv7) |
| `vivoka/Android/src/main/jniLibs/x86_64/*.so` | ~66MB | Native libs (x86_64) |

### Sherpa-ONNX (~38MB)

| File | Size | Purpose |
|------|------|---------|
| `sherpa-onnx/sherpa-onnx.aar` | 38MB | Sherpa-ONNX Android AAR |
| `sherpa-onnx/sherpa-onnx-classes.jar` | 206K | Sherpa-ONNX Java classes |
| `sherpa-onnx/sherpa-onnx-desktop.jar` | 149K | Sherpa-ONNX desktop JAR |

### TVM4J Core JARs (~84K)

| File | Size | Purpose |
|------|------|---------|
| `Modules/AI/ALC/libs/tvm4j_core.jar` | 32K | TVM Java bindings (ALC module) |
| `Modules/AI/LLM/libs/tvm4j_core.jar` | 52K | TVM Java bindings (LLM module) |

### VSDK ASR Data (~164MB)

| File | Size | Purpose |
|------|------|---------|
| `.../vsdk/data/csdk/asr/acmod/am_enu_vocon_car_*.dat` | 9MB | Acoustic model (English-US) |
| `.../vsdk/data/csdk/asr/asr/vocon_asr2.dat` | ~1MB | ASR engine data |
| `.../vsdk/data/csdk/asr/clc/clc_enu_cfg3_*.dat` | ~4MB | CLC config |
| `.../vsdk/data/csdk/asr/clc/dict-asr_eng-USA_*.dcc` | ~2MB | Dictionary (English-US) |
| `.../vsdk/data/csdk/asr/ctx/asreng-US.fcf` | 1.3K | ASR context config |
| `.../vsdk/data/csdk/asr/ctx/ctx-primary_eng-USA_*.fcf` | 67MB | Primary context (English-US) |
| `.../vsdk/data/csdk/asr/lm/lm-assistantmessaging_*.dat` | 66MB | Language model (messaging) |
| `.../vsdk/data/csdk/asr/lm/lm-guarded_*.dat` | 13MB | Language model (guarded) |
| `.../vsdk/data/csdk/asr/lm/lm-shadowdomain_*.dat` | ~1MB | Language model (shadow domain) |
| `.../vsdk/data/csdk/asr/lm/lm-w3wM_*.dat` | ~1MB | Language model (W3W) |

Base path: `Modules/SpeechRecognition/src/main/assets/vsdk/data/`

### VLM Models — Tiny (~296MB)

| File | Size | Language | Tier |
|------|------|----------|------|
| `VLMFiles/EN/VoiceOS-Tin-EN.bin` | 74MB | English | Tiny (ONNX) |
| `VLMFiles/EN/VoiceOS-Tin-EN.vlm` | 74MB | English | Tiny (AON-encrypted) |
| `VLMFiles/MUL/VoiceOS-Tin-MUL.bin` | 74MB | Multilingual | Tiny (ONNX) |
| `VLMFiles/MUL/VoiceOS-Tin-MUL.vlm` | 74MB | Multilingual | Tiny (AON-encrypted) |

---

## GitLab Package Registry Only (>100MB per file)

Download via: `GITLAB_TOKEN=glpat-XXX GITLAB_PROJECT_ID=76928718 ./scripts/setup-sdk.sh --vlm-only`

### VLM Models — Base + Small + Medium (~8.4GB)

| File | Size | Language | Tier | Package |
|------|------|----------|------|---------|
| `VLMFiles/EN/VoiceOS-Bas-EN.bin` | 141MB | English | Base | `vlm-models-en/1.0.0` |
| `VLMFiles/EN/VoiceOS-Bas-EN.vlm` | 141MB | English | Base | `vlm-models-en/1.0.0` |
| `VLMFiles/EN/VoiceOS-Sml-EN.bin` | 465MB | English | Small | `vlm-models-en/1.0.0` |
| `VLMFiles/EN/VoiceOS-Sml-EN.vlm` | 465MB | English | Small | `vlm-models-en/1.0.0` |
| `VLMFiles/EN/VoiceOS-Med-EN.bin` | 1.4GB | English | Medium | `vlm-models-en/1.0.0` |
| `VLMFiles/EN/VoiceOS-Med-EN.vlm` | 1.4GB | English | Medium | `vlm-models-en/1.0.0` |
| `VLMFiles/MUL/VoiceOS-Bas-MUL.bin` | 141MB | Multilingual | Base | `vlm-models-mul/1.0.0` |
| `VLMFiles/MUL/VoiceOS-Bas-MUL.vlm` | 141MB | Multilingual | Base | `vlm-models-mul/1.0.0` |
| `VLMFiles/MUL/VoiceOS-Sml-MUL.bin` | 465MB | Multilingual | Small | `vlm-models-mul/1.0.0` |
| `VLMFiles/MUL/VoiceOS-Sml-MUL.vlm` | 465MB | Multilingual | Small | `vlm-models-mul/1.0.0` |
| `VLMFiles/MUL/VoiceOS-Med-MUL.bin` | 1.4GB | Multilingual | Medium | `vlm-models-mul/1.0.0` |
| `VLMFiles/MUL/VoiceOS-Med-MUL.vlm` | 1.4GB | Multilingual | Medium | `vlm-models-mul/1.0.0` |

---

## Scripts

| Script | Purpose |
|--------|---------|
| `scripts/setup-sdk.sh` | Download registry-only files after clone |
| `scripts/publish-packages.sh` | Upload binaries to GitLab Package Registry |
| `scripts/checksums.sha256` | SHA-256 verification checksums |

### Setup flags

```bash
./scripts/setup-sdk.sh              # Download everything (VLM Sml+Med only)
./scripts/setup-sdk.sh --vlm-only   # Only VLM Small + Medium models
```

---

## Quick Reference

After `git clone`, all binaries under 100MB are immediately available. For VLM Base/Small/Medium models:

```bash
export GITLAB_TOKEN=glpat-XXXXXXXXXXXXXXXXXXXX
export GITLAB_PROJECT_ID=76928718
./scripts/setup-sdk.sh --vlm-only
```
