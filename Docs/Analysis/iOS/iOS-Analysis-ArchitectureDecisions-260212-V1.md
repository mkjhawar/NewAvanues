# iOS Architecture Decisions — SwiftUI vs Compose MP, KMP Bridge, CI/CD

**Document:** iOS-Analysis-ArchitectureDecisions-260212-V1.md
**Date:** 2026-02-12
**Version:** V1
**Status:** RESEARCH COMPLETE

---

## Executive Summary

Recommendation: **Hybrid approach — SwiftUI shell + KMP shared logic + selective Compose MP screens**. This maximizes code reuse (~70%) while keeping iOS-native feel for accessibility and Siri integration.

---

## 1. Compose Multiplatform for iOS — Current State

### Stability
- **STABLE** as of May 2025 (Compose Multiplatform 1.8.0)
- JetBrains officially supports for production use
- 96% of teams report no major performance concerns
- ~9 MB binary size overhead vs pure SwiftUI

### Performance vs SwiftUI
- **Scrolling**: On par, even on 120Hz ProMotion
- **Startup**: Comparable to native iOS apps
- **Memory**: +9 MB over equivalent SwiftUI
- **Known issue**: GitHub #4912 — high CPU/memory in some scenarios

### Navigation Libraries (All Production-Ready)

| Library | Best For |
|---------|----------|
| **Decompose** | Complex apps with lifecycle awareness (RECOMMENDED) |
| **Voyager** | Simple navigation, Koin integration |
| **Precompose** | Teams familiar with Jetpack Navigation |

### WKWebView in Compose MP
- `compose-webview-multiplatform` (KevinnZou) — production-ready
- Uses `UIKitView` to embed WKWebView
- Full WKWebSettings access for iOS configuration

### DI on iOS (No Hilt)

| Library | Recommendation |
|---------|---------------|
| **Koin 4.0+** | RECOMMENDED — pragmatic, lightweight, excellent KMP docs |
| kotlin-inject | Compile-time, zero runtime overhead |
| Kodein | Multiplatform from inception |

### Missing iOS Features in Compose MP
- Widgets (Today Extensions, Home Screen)
- App Extensions (Share, Keyboard)
- Live Activities (Dynamic Island)
- App Intents (Siri Shortcuts)
- **VoiceOver accessibility** — not fully mature

---

## 2. SwiftUI + KMP Shared Logic

### Kotlin-to-Swift Interop

| Approach | Status | Recommendation |
|----------|--------|---------------|
| **SKIE** (Touchlab) | Production stable | **USE THIS** — Flow->AsyncSequence, suspend->async/await, sealed->enum |
| Swift Export (JetBrains) | Experimental | Wait for stable (targeting 2026) |
| cinterop (legacy) | Deprecated | Avoid |

### SKIE Type Mapping

| Kotlin | Swift (with SKIE) |
|--------|-------------------|
| `Int?` | `Int?` (not KotlinInt) |
| `sealed class` | `@frozen enum` |
| `suspend fun` | `async throws` |
| `Flow<T>` | `AsyncSequence` |
| `List<T>` | `[T]` |

### SQLDelight iOS Driver
- Production-ready (2.0.1+)
- NativeSqliteDriver — Cash App has used since 2018
- Zero issues with iOS stability

### Pain Points
1. Type mapping complexity without SKIE (boxed types, no exhaustive switches)
2. Observability bridging (StateFlow -> AsyncSequence/Combine)
3. Build complexity (CocoaPods or SPM config)
4. Debugging across language boundaries

---

## 3. Hybrid Approach (RECOMMENDED)

### Can You Mix Compose MP + SwiftUI? YES

```swift
// SwiftUI hosting Compose screen
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return ComposeUIViewController() // Kotlin Compose screen
    }
}
```

### Recommended Architecture

```
iOS App (SwiftUI shell)
|-- Launch/Onboarding         -> SwiftUI (platform-native)
|-- Siri Integration           -> SwiftUI (required)
|-- Web Browser View           -> Compose MP (share logic with Android)
|-- Settings Screens           -> Compose MP (already exist in Android)
|-- Voice Command Palette      -> SwiftUI (native iOS feel)
|-- Safari Extension           -> Native Swift (required)
|
+-- Shared KMP Module
    |-- VoiceOSCore (275+ files commonMain)
    |-- NLU (intent classification)
    |-- Database (SQLDelight)
    |-- WebAvanue (150+ files commonMain)
    |-- Networking (Ktor)
    +-- ViewModels (StateFlow -> AsyncSequence via SKIE)
```

### Why Hybrid?
1. **Share critical logic** — VoiceOSCore, NLU, database (70%+ reuse)
2. **Share complex screens** — Browser view, settings (avoid duplication)
3. **Keep iOS-native** — Siri, widgets, onboarding (best UX)
4. **Mitigate Compose MP risks** — If accessibility issues arise, most screens are SwiftUI
5. **Incremental migration** — Start SwiftUI + shared logic, add Compose screens over time

---

## 4. CI/CD for KMP iOS

### Recommended: GitHub Actions + Fastlane

| Tool | Role |
|------|------|
| GitHub Actions | CI runner (macos-14/15, Xcode 16+) |
| Fastlane | Build, sign, deploy to TestFlight |
| Fastlane Match | Code signing management |
| Gradle | Build KMP shared framework |

### Build Pipeline
1. Build KMP shared framework (`linkReleaseFrameworkIosArm64`)
2. Run Kotlin tests
3. Build iOS app with Fastlane
4. Upload to TestFlight

### Code Signing
- Same as native iOS — use Fastlane Match for CI
- Must sign KMP framework if distributed as XCFramework
- Apple mandates Xcode 16+ / iOS 18 SDK for uploads

---

## 5. Technology Stack Decision

| Decision | Choice | Rationale |
|----------|--------|-----------|
| UI Framework | **SwiftUI + selective Compose MP** | Native iOS feel + code reuse |
| Swift Interop | **SKIE** (Touchlab) | Mandatory for good DX |
| DI | **Koin 4.0+** | Works across KMP + iOS |
| Navigation (Compose) | **Decompose** | Lifecycle-aware, SwiftUI interop |
| Networking | **Ktor** (Darwin engine) | Cross-platform, uses NSURLSession |
| Database | **SQLDelight** (NativeSqliteDriver) | Production-ready since 2018 |
| Settings | **DataStore 1.1.0+** | Official Jetpack, iOS stable |
| CI/CD | **GitHub Actions + Fastlane** | Standard iOS tooling |
| Speech | **SFSpeechRecognizer** -> SpeechAnalyzer | Stable now, upgrade later |
| Web Engine | **WKWebView** | Only option on iOS |
| Credential Storage | **iOS Keychain** | Standard, AES-256 |

---

## 6. Real-World KMP iOS Production Apps

| Company | Use Case |
|---------|----------|
| Netflix | Studio production apps |
| Cash App | Payment processing (7+ years) |
| McDonald's | Mobile ordering (100M+ downloads) |
| Autodesk | Mobile CAD tools |
| 9GAG | Social media feed |
| Quizlet | Education app |

### Common Pitfalls & Solutions

| Pitfall | Solution |
|---------|----------|
| High memory/CPU | Avoid deeply nested Composables, profile with Instruments |
| Slow build times | Gradle build cache, modularize KMP, XCFramework distribution |
| Accessibility issues | Use native SwiftUI for critical accessibility flows |
| Third-party SDKs | Use expect/actual for platform-specific implementations |
| Swift interop pain | Use SKIE (transformative improvement) |

---

## 7. Risk Assessment

| Risk | Probability | Mitigation |
|------|-------------|------------|
| Compose MP accessibility (VoiceOver) | MEDIUM | Use SwiftUI for voice UI, Compose for browser |
| KMP build complexity | MEDIUM | SKIE + Gradle cache + modularization |
| App Store rejection | LOW | Clear accessibility purpose, no private APIs |
| Performance (#4912) | LOW | Profile early, native screens as fallback |
| SKIE stability | LOW | Production-proven, Touchlab support |

---

## Decision: HYBRID (SwiftUI + KMP + Selective Compose MP)

**This gives ~70% code reuse while keeping iOS polished and accessible.** Going all-Compose risks VoiceOver blockers. Going all-SwiftUI means duplicating VoiceOSCore + NLU + WebAvanue in Swift — massive waste.
