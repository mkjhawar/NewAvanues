# Avanues OSS License Audit Report

**Date:** 2026-02-09
**Branch:** 060226-1-consolidation-framework
**Scope:** All modules in NewAvanues monorepo

---

## Executive Summary

- **Total third-party dependencies:** 55 libraries
- **Previously listed in app:** 16 (29% coverage — incomplete)
- **Modified from original:** 1 (TVM Runtime)
- **Custom wrappers around OSS:** 1 (Whisper.cpp JNI)
- **Proprietary/commercial:** 1 (Vivoka VSDK)
- **Copyleft (GPL/LGPL):** 0 — no copyleft risk
- **License types:** Apache 2.0 (majority), MIT, BSD 3-Clause, BSD 2-Clause

---

## Modified Implementations

### TVM Runtime (Apache TVM) — MODIFIED
- **Files:** `Modules/AI/LLM/libs/tvm4j_core.jar`, `Modules/AI/ALC/libs/tvm4j_core.jar`
- **Patch:** `Modules/AI/LLM/tvm-patches/Module.java`
- **Change:** Fixed `ffi.ModuleLoadFromFile` signature (1 arg instead of 2) for TVM v0.22.0 FFI API change
- **License:** Apache License 2.0
- **Compliance:** Apache 2.0 §4 requires stating changes. Patch is documented in `tvm-patches/README.md`.
- **Attribution:** "Based on Apache TVM, modified by Augmentalis"

### Whisper.cpp — CUSTOM JNI WRAPPERS (not a fork)
- **Files:** `Modules/SpeechRecognition/src/main/cpp/whisper_jni.cpp`, `jni/whisper/jni.c`
- **Status:** JNI bridge code written by Augmentalis to interface with whisper.cpp
- **Whisper source:** NOT vendored (build target `whisper-source/` missing, not currently built)
- **License:** MIT (wrappers are original Augmentalis code)
- **Attribution:** "Uses whisper.cpp by Georgi Gerganov (MIT License)" if/when integrated

---

## Proprietary Dependencies

### Vivoka VSDK — PROPRIETARY
- **Files:** `/vivoka/vsdk-6.0.0.aar`, `vsdk-csdk-asr-2.0.0.aar`, `vsdk-csdk-core-1.0.1.aar`
- **Status:** Used as-is, not modified, `compileOnly` scope
- **License:** Commercial/proprietary — requires vendor agreement
- **Action needed:** Verify distribution rights with Vivoka

---

## Complete Dependency List

### Apache License 2.0

#### Google (16 libraries)
| Library | Version | Module(s) |
|---------|---------|-----------|
| Jetpack Compose | BOM 2024.06.00 | AvanueUI, apps/avanues |
| Compose Material 3 | via BOM | AvanueUI, apps/avanues |
| Compose Material 3 Adaptive | 1.0.0-beta01 | apps/avanues (Settings) |
| Material Icons Extended | via BOM | apps/avanues, AvanueUI |
| Hilt / Dagger | 2.51.1 | apps/avanues, VoiceOSCore |
| AndroidX Core KTX | 1.12.0 | apps/avanues, DeviceManager |
| AndroidX Activity Compose | 1.8.1 | apps/avanues |
| AndroidX Navigation Compose | 2.7.6 | apps/avanues |
| AndroidX Lifecycle | 2.6.2 / 2.7.0 | apps/avanues, multiple |
| AndroidX SplashScreen | 1.0.1 | apps/avanues |
| AndroidX AppCompat | 1.6.1 | DeviceManager |
| AndroidX Camera | 1.3.1 | DeviceManager |
| AndroidX WebKit | 1.8.0 | WebAvanue |
| AndroidX DataStore | 1.1.1 | apps/avanues |
| AndroidX Work | 2.9.0 | apps/avanues |
| Gson | 2.10.1 | SpeechRecognition |

#### Google — Play Services (4 libraries)
| Library | Version | Module(s) |
|---------|---------|-----------|
| Play Services Nearby | 19.1.0 | DeviceManager |
| Play Services Base | 18.3.0 | DeviceManager |
| Play Services Location | 21.0.1 | DeviceManager |
| Play Services Fitness | 21.1.0 | DeviceManager |

#### Google — Firebase (1 library)
| Library | Version | Module(s) |
|---------|---------|-----------|
| Firebase BOM (Config) | 34.3.0 | SpeechRecognition |

#### Google — TensorFlow (2 libraries)
| Library | Version | Module(s) |
|---------|---------|-----------|
| TensorFlow Lite | 2.14.0 | AI/LLM, AI/NLU |
| TensorFlow Lite Support | 0.4.4 | AI/LLM, AI/NLU |

#### Google — Protocol Buffers (2 libraries)
| Library | Version | Module(s) |
|---------|---------|-----------|
| protobuf-kotlin | 3.25.2 | RPC |
| protobuf-kotlin-lite | 3.25.2 | RPC |

#### JetBrains (7 libraries)
| Library | Version | Module(s) |
|---------|---------|-----------|
| Kotlin | 1.9.24 | All |
| Kotlin Coroutines | 1.8.1 | All |
| Kotlin Multiplatform | 1.9.24 | Shared modules |
| kotlinx-datetime | 0.5.0 | VoiceOSCore, WebAvanue |
| kotlinx-serialization | 1.6.0 | Multiple |
| kotlinx-atomicfu | 0.23.2 | VoiceOSCore |
| Ktor HTTP Client | 2.3.7 | AI/LLM (6 sub-modules) |

#### Square (4 libraries)
| Library | Version | Module(s) |
|---------|---------|-----------|
| OkHttp | 4.12.0 | SpeechRecognition, AI/LLM |
| OkHttp Logging | 4.12.0 | SpeechRecognition |
| Wire Runtime | 5.4.0 | RPC |
| Wire gRPC Client | 5.4.0 | RPC |

#### CashApp / Square (1 library)
| Library | Version | Module(s) |
|---------|---------|-----------|
| SQLDelight | 2.0.1 | Database, WebAvanue (5 sub-modules) |

#### gRPC Authors (5 libraries)
| Library | Version | Module(s) |
|---------|---------|-----------|
| grpc-okhttp | 1.62.2 | RPC |
| grpc-netty-shaded | 1.62.2 | RPC |
| grpc-protobuf | 1.62.2 | RPC |
| grpc-protobuf-lite | 1.62.2 | RPC |
| grpc-stub | 1.62.2 | RPC |
| grpc-kotlin-stub | 1.4.1 | RPC |

#### Microsoft (1 library)
| Library | Version | Module(s) |
|---------|---------|-----------|
| ONNX Runtime | 1.17.0 | AI/NLU |

#### Apache Foundation (2 libraries)
| Library | Version | Module(s) |
|---------|---------|-----------|
| Commons Compress | 1.25.0 | AI/LLM |
| Apache TVM (modified) | 0.22.0 | AI/LLM, AI/ALC |

#### InsertKoin (2 libraries)
| Library | Version | Module(s) |
|---------|---------|-----------|
| Koin Core | 3.5.3 | WebAvanue |
| Koin Compose | 1.1.2 | WebAvanue |

#### Aakira (1 library)
| Library | Version | Module(s) |
|---------|---------|-----------|
| Napier (KMP logging) | 2.7.1 | WebAvanue |

#### AlphaCephei (1 library)
| Library | Version | Module(s) |
|---------|---------|-----------|
| Vosk (STT) | 0.3.47 | SpeechRecognition (compileOnly) |

#### MockK (testing only)
| Library | Version | Module(s) |
|---------|---------|-----------|
| MockK | 1.13.9 | Testing |
| MockK Android | 1.13.9 | Testing |

#### Square (testing only)
| Library | Version | Module(s) |
|---------|---------|-----------|
| LeakCanary | 2.12 | Testing/Debug |

### MIT License

| Library | Author | Version | Module(s) |
|---------|--------|---------|-----------|
| Voyager (Navigator, ScreenModel, Tabs, Transitions) | Adriel Cafe | 1.0.0 | WebAvanue |
| Whisper.cpp (JNI wrappers only) | Georgi Gerganov | - | SpeechRecognition (not built) |

### BSD 3-Clause License

| Library | Author | Version | Module(s) |
|---------|--------|---------|-----------|
| Protocol Buffers (core) | Google | 3.25.2 | RPC |
| SQLCipher | Zetetic LLC | 4.5.4 | Database, WebAvanue |

### BSD 2-Clause License

| Library | Author | Version | Module(s) |
|---------|--------|---------|-----------|
| Sentry Android | Sentry | 7.0.0 | WebAvanue, SpeechRecognition |

### Eclipse Public License 1.0

| Library | Author | Version | Module(s) |
|---------|--------|---------|-----------|
| JUnit | JUnit Team | 4.13.2 | Testing only |
| Logback | QOS.ch | 1.4.14 | AI/LLM (Desktop) |

### Dual License (MIT + Apache 2.0)

| Library | Author | Version | Module(s) |
|---------|--------|---------|-----------|
| SLF4J | QOS.ch | 2.0.9 | AI/LLM (Desktop) |

### Apache License 2.0 (testing only)

| Library | Author | Version | Module(s) |
|---------|--------|---------|-----------|
| Robolectric | Robolectric | 4.11.1 | Testing |

---

## License Compliance Checklist

| Requirement | Apache 2.0 | MIT | BSD 3 | BSD 2 | Status |
|-------------|-----------|-----|-------|-------|--------|
| Include license text | Yes | Yes | Yes | Yes | **TODO: Add to app** |
| Credit author | Yes | Yes | Yes | Yes | Partial (16/55) |
| State modifications | Yes | No | No | No | Done (TVM patch README) |
| Provide NOTICE file | If exists | No | No | No | TODO: Check each dep |
| Share source code | No | No | No | No | Not required |
| No endorsement | N/A | N/A | Yes | N/A | OK |

---

## Recommendations

1. **Immediate:** Update in-app license dialog to show all 55 dependencies
2. **Immediate:** Add full license text viewable per license type
3. **Immediate:** Group by license type → copyright holder (user's preferred layout)
4. **Short-term:** Add `aboutlibraries` Gradle plugin for auto-detection
5. **Short-term:** Verify Vivoka distribution rights
6. **Long-term:** Add CI check that flags new deps missing from license registry

---

## "Inspired By" vs "Uses" Classification

| Item | Classification | Rationale |
|------|---------------|-----------|
| All Gradle dependencies | **Uses** | Imported as-is from Maven Central |
| TVM Runtime | **Uses (modified)** | Source patched, must attribute + note changes |
| Whisper.cpp JNI | **Inspired by** | We wrote custom JNI wrappers; whisper source not vendored |
| Vivoka VSDK | **Uses** | Proprietary, used as-is |
| GlassmorphicComponents | **Original** | Our own code, inspired by Apple design language |
| AvanueUI Design System | **Original** | Our own design tokens and components |
| Ocean/Sunset/Liquid themes | **Original** | Our own color schemes |
