# ADR-001: Kotlin Multiplatform (KMP) Strategy

**Status**: Accepted
**Date**: 2025-10-29
**Authors**: Manoj Jhawar
**Deciders**: Architecture Team

---

## Context

AVA AI will migrate to VoiceAvenue ecosystem and become AIAvanue app supporting Android + iOS. We need a cross-platform strategy that:
1. Maximizes code sharing between platforms (target: 70-75%)
2. Maintains native performance for ML inference
3. Enables reuse of VoiceAvenue platform libraries
4. Supports future platform expansion (macOS, Windows)

---

## Decision

**We will use Kotlin Multiplatform (KMP) with a hybrid architecture:**

### 1. Shared in commonMain (70-80% of codebase)
- **Domain models**: 100% shared (Conversation, Message, TrainExample, etc.)
- **Repository interfaces**: 100% shared
- **Business logic**: 100% shared (use cases, ViewModels)
- **UI components**: 90% shared (via Compose Multiplatform)
- **Networking**: 80% shared (Ktor client)

### 2. Platform-specific (20-30% of codebase)
- **ML inference**: expect/actual pattern
  - Android: ONNX Runtime Mobile
  - iOS: Core ML
- **Database drivers**: expect/actual pattern
  - Android: Room + SQLDelight Android driver
  - iOS: SQLDelight native driver
- **Platform services**: expect/actual pattern
  - TTS, permissions, file system access

### 3. Module Structure
```
features/[name]/
├── src/
│   ├── commonMain/kotlin/       # 70-90% of code
│   │   ├── [Feature]ViewModel.kt
│   │   ├── [Feature]Screen.kt   # Compose MP
│   │   └── use-cases/
│   ├── androidMain/kotlin/      # Android-specific
│   │   └── Platform[Feature].kt # actual implementations
│   └── iosMain/kotlin/          # iOS-specific
│       └── Platform[Feature].kt # actual implementations
```

---

## Rationale

### Why KMP?

**Performance**: Kotlin/Native achieves 95-98% of pure Swift performance (proven in production apps like Cash App, Trello)

**Code Sharing**: Real-world KMP apps achieve:
- 70-80% code sharing (typical)
- 85-90% code sharing (well-designed architecture)
- AVA AI target: 75% (conservative, achievable)

**VoiceAvenue Alignment**: Parent app uses KMP for all core libraries (MagicUI, MagicCode, ThemeBridge, Database)

**Cost Efficiency**:
- Single codebase for business logic
- Reduced maintenance burden
- Faster feature parity across platforms

### Why Hybrid (not 100% shared)?

**Native Performance**: ML inference is CPU/GPU intensive. Using native frameworks (ONNX, Core ML) ensures optimal performance without JNI/FFI overhead.

**Platform Strengths**: Each platform has mature ML ecosystems:
- Android: ONNX Runtime Mobile (25MB, INT8 quantization)
- iOS: Core ML (built-in, GPU acceleration, ANE support)

**Proven Pattern**: Used successfully by:
- Cash App (networking + business logic shared, UI native)
- Trello (ViewModels + domain shared, UI Compose MP)
- Instabug (SDKs with 80% code sharing)

---

## Consequences

### Positive

✅ **75% code sharing** → Reduced development time for iOS (estimated 6 weeks vs 12 weeks native)

✅ **Single source of truth** → Business logic bugs fixed once, tests run on both platforms

✅ **Compose Multiplatform** → 90% UI sharing with platform-native feel

✅ **VoiceAvenue integration ready** → Already using KMP libraries from parent app

✅ **Future platforms** → macOS/Windows support easier (Desktop Compose + shared ViewModels)

### Negative

⚠️ **Learning curve** → Team needs to learn expect/actual pattern, KMP build configuration

⚠️ **Build complexity** → Gradle configuration more complex (iOS targets, framework generation)

⚠️ **Debugging** → Cross-platform debugging requires platform-specific tools (Xcode for iOS, Android Studio for Android)

⚠️ **Library limitations** → Some Android libraries don't support KMP (need alternatives or wrappers)

### Neutral

🔄 **Gradual migration** → Modules converted one-by-one (features/teach already done, features/chat next)

🔄 **Dual database** → Room (Android) + SQLDelight (iOS) during transition, full SQLDelight later

---

## Implementation Plan

### Phase 1: Core Modules (Complete)
- ✅ core/common → KMP
- ✅ core/domain → KMP
- ✅ core/data → KMP + SQLDelight

### Phase 2: Feature Modules (Weeks 6-8)
- [ ] features/teach → Already KMP! (90% shared)
- [ ] features/chat → Convert to KMP (80% shareable)
- [ ] features/nlu → Convert to KMP (50% shareable, expect/actual for ONNX/Core ML)

### Phase 3: New Features (Week 9+)
- [ ] features/alc-llm → Build as KMP from start (70% shareable)

### Phase 4: VoiceAvenue Integration (Future)
- [ ] Migrate to AIAvanue app structure
- [ ] Use VoiceAvenue platform libraries
- [ ] IPC communication with VoiceOS

---

## Alternatives Considered

### 1. Flutter
**Rejected**: Dart language not aligned with VoiceAvenue ecosystem (Kotlin-first). Flutter adds 4MB+ overhead. Poor integration with native ML frameworks.

### 2. React Native
**Rejected**: JavaScript not type-safe. Poor performance for ML workloads. Maintenance burden with native bridges. VoiceAvenue is Kotlin-based.

### 3. Native iOS (Swift)
**Rejected**: 0% code sharing. Doubles development time. Business logic duplicated. Bug fixes need coordination across teams.

### 4. 100% KMP (no expect/actual)
**Rejected**: ML inference performance critical. Using Kotlin wrappers around native frameworks adds overhead. Direct native calls (ONNX, Core ML) proven faster.

---

## Success Metrics

| Metric | Target | Current | Status |
|--------|--------|---------|--------|
| Code sharing | 75% | 60% (core only) | 🟡 In Progress |
| Core modules KMP | 100% | 100% | ✅ Complete |
| Feature modules KMP | 100% | 33% (1/3) | 🟡 In Progress |
| iOS build time | <5 min | N/A | ⏳ Pending |
| Android build time | <3 min | ~2 min | ✅ Good |
| Test coverage (shared) | 80% | 92% (core) | ✅ Exceeds |

---

## References

- [Kotlin Multiplatform Official Docs](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [SQLDelight Documentation](https://cashapp.github.io/sqldelight/)
- [KMP Case Studies](https://kotlinlang.org/lp/multiplatform/case-studies/)
- VoiceAvenue CLAUDE.md (KMP architecture patterns)
- IDEACODE v3.1 Protocol-File-Organization.md

---

## Changelog

**v1.0 (2025-10-29)**: Initial decision - KMP with hybrid architecture, 75% code sharing target

---

**Created by Manoj Jhawar, manoj@ideahq.net**
