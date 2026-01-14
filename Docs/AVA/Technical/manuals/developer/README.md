# AVA Developer Manual

**Version:** 1.0
**Updated:** 2025-12-07

---

## Overview

AVA (Avanues Voice Assistant) is an on-device AI assistant with NLU, LLM, and RAG capabilities. This manual covers development practices for the AVA codebase.

---

## Module Structure

| Module | Purpose |
|--------|---------|
| `app/` | Main Android application |
| `core/Utils` | Logging, utilities |
| `core/Domain` | Domain models, repositories |
| `core/Data` | SQLDelight database, DataStore |
| `core/Theme` | Ocean Glass design system |
| `Chat` | Chat UI, ViewModel, TTS |
| `NLU` | ONNX-based NLU (KMP) |
| `LLM` | On-device LLM (TVM, MLC-LLM) |
| `RAG` | Document ingestion, embeddings |
| `Actions` | Action execution, VoiceOS integration |
| `Teach` | Teaching mode UI |
| `Overlay` | Floating overlay UI |
| `WakeWord` | Porcupine wake word detection |

---

## Key Features

- [VoiceOS AIDL Integration](features/voiceos-aidl-integration-251207.md)

---

## Architecture

- [VoiceOS Connection Architecture](architecture/voiceos-connection-251207.md)

---

## Build Instructions

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
cd android/apps/ava
./gradlew assembleDebug
```

---

## Related Documentation

- [AVA Migration Complete](../../../Migration/AVA/MIGRATION-COMPLETE.md)
- [VoiceOS Developer Manual](../../VoiceOS/Technical/manuals/developer/README.md)
