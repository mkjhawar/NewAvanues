# AVA Cross-Platform & VoiceAvenue Integration Strategy

**Date**: 2025-11-02 01:30 PST
**Status**: 📋 Planning Phase
**Target Platforms**: iOS, macOS, Windows, Web + VoiceAvenue Ecosystem Integration
**Created by**: Manoj Jhawar, manoj@ideahq.net

---

## Executive Summary

**GOAL**: Transform AVA from Android-only to full cross-platform app integrated with VoiceAvenue ecosystem using MagicCode/MagicUI.

**CURRENT STATE**:
- ✅ Android app functional with ONNX NLU
- ✅ Core modules (common, domain, data) are KMP-ready
- ❌ Features modules (nlu, chat, overlay) are Android-only
- ❌ No iOS/macOS/Windows/Web support
- ❌ Not integrated with VoiceAvenue ecosystem
- ❌ Not using MagicCode/MagicUI

**TARGET STATE**:
- ✅ All platforms: iOS, macOS, Windows, Web, Android
- ✅ 70-80% code reuse via KMP
- ✅ MagicCode for cross-platform UI generation
- ✅ MagicUI runtime for all platforms
- ✅ Integrated with VoiceAvenue ecosystem as standalone app
- ✅ VoiceOS Bridge for IPC with VoiceOS accessibility layer

---

## Part 1: Cross-Platform Strategy

### 1.1 Platform Requirements

#### iOS (Priority P0 - Critical)
**Target**: iPhone and iPad

**Requirements**:
- Swift/SwiftUI UI layer
- ONNX Runtime iOS (for NLU)
- CoreData or SQLite (Room alternative)
- Speech framework (voice input)
- AVFoundation (audio)

**KMP Support**:
- ✅ Business logic in commonMain
- ✅ Data models fully shared
- ✅ Network/coroutines fully shared
- ⚠️ UI: SwiftUI (generated via MagicCode)
- ⚠️ Database: expect/actual for iOS Core Data

#### macOS (Priority P1 - High)
**Target**: Desktop Mac computers

**Requirements**:
- SwiftUI for macOS
- ONNX Runtime macOS
- CoreData or SQLite
- AppKit integration where needed

**KMP Support**:
- Same as iOS with macOS-specific adjustments
- Desktop-optimized layouts

#### Windows (Priority P2 - Medium)
**Target**: Windows 10/11 desktop

**Requirements**:
- WPF or WinUI 3 (via Compose Multiplatform Desktop)
- ONNX Runtime Windows
- SQLite
- Windows Speech API

**KMP Support**:
- ✅ Compose Multiplatform Desktop (Kotlin/JVM)
- ✅ Fully shared with Android (Compose-based)
- ⚠️ Windows-specific APIs via expect/actual

#### Web (Priority P2 - Medium)
**Target**: Browser-based PWA

**Requirements**:
- React/TypeScript or Kotlin/JS + Compose for Web
- TensorFlow.js (for NLU) or ONNX Runtime Web
- IndexedDB for local storage
- Web Speech API

**KMP Support**:
- ✅ Kotlin/JS target
- ✅ Business logic fully shared
- ⚠️ UI: React (generated via MagicCode) OR Compose for Web

### 1.2 Kotlin Multiplatform Migration Plan

#### Phase 1: Core Infrastructure (Week 1-2)

**1.1 Update Build Configuration**

Current: Android-only
```kotlin
// features/nlu/build.gradle.kts
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}
```

Target: KMP
```kotlin
// features/nlu/build.gradle.kts
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    macosX64()
    macosArm64()

    jvm("desktop")  // For Windows/Linux via Compose Desktop

    js(IR) {        // For Web
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core:common"))
                implementation(project(":core:domain"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("ai.onnxruntime:onnxruntime-android:1.17.0")
            }
        }

        val iosMain by getting {
            dependencies {
                // ONNX Runtime iOS - needs native binding
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation("ai.onnxruntime:onnxruntime:1.17.0")
            }
        }

        val jsMain by getting {
            dependencies {
                // TensorFlow.js or ONNX Runtime Web
            }
        }
    }
}
```

**1.2 Restructure Source Directories**

Move code to KMP structure:
```
features/nlu/
├── src/
│   ├── commonMain/kotlin/          # Shared business logic (70%)
│   │   ├── domain/
│   │   │   ├── IntentClassifier.kt (interface)
│   │   │   ├── ModelManager.kt (interface)
│   │   │   └── models/
│   │   │       └── IntentClassification.kt
│   │   └── usecase/
│   │       ├── ClassifyIntentUseCase.kt
│   │       └── TrainIntentUseCase.kt
│   │
│   ├── androidMain/kotlin/         # Android-specific (15%)
│   │   └── platform/
│   │       ├── IntentClassifierImpl.kt (ONNX Android)
│   │       ├── BertTokenizer.kt
│   │       └── ModelManagerImpl.kt
│   │
│   ├── iosMain/kotlin/             # iOS-specific (15%)
│   │   └── platform/
│   │       ├── IntentClassifierImpl.kt (ONNX iOS)
│   │       └── ModelManagerImpl.kt
│   │
│   ├── desktopMain/kotlin/         # Desktop-specific
│   │   └── platform/
│   │       ├── IntentClassifierImpl.kt (ONNX Desktop)
│   │       └── ModelManagerImpl.kt
│   │
│   └── jsMain/kotlin/              # Web-specific
│       └── platform/
│           ├── IntentClassifierImpl.kt (TF.js)
│           └── ModelManagerImpl.kt
```

**1.3 Use Expect/Actual Pattern**

```kotlin
// commonMain/domain/IntentClassifier.kt
expect class IntentClassifier() {
    suspend fun initialize(modelPath: String): Result<Unit>
    suspend fun classifyIntent(
        text: String,
        candidateIntents: List<String>
    ): Result<IntentClassification>
    fun close()
}

// androidMain/platform/IntentClassifier.kt
actual class IntentClassifier actual constructor() {
    private var ortSession: OrtSession? = null

    actual suspend fun initialize(modelPath: String): Result<Unit> {
        // ONNX Runtime Android implementation
    }

    actual suspend fun classifyIntent(
        text: String,
        candidateIntents: List<String>
    ): Result<IntentClassification> {
        // ONNX inference
    }

    actual fun close() {
        ortSession?.close()
    }
}

// iosMain/platform/IntentClassifier.kt
actual class IntentClassifier actual constructor() {
    // iOS CoreML or ONNX Runtime iOS
}

// desktopMain/platform/IntentClassifier.kt
actual class IntentClassifier actual constructor() {
    // ONNX Runtime Desktop (JVM)
}

// jsMain/platform/IntentClassifier.kt
actual class IntentClassifier actual constructor() {
    // TensorFlow.js or ONNX Runtime Web
}
```

#### Phase 2: Feature Modules Migration (Week 3-4)

**Priority Order**:
1. ✅ **features/nlu** (Already has interfaces, just restructure)
2. ✅ **features/chat** (ViewModels, business logic - high shareability)
3. ✅ **features/overlay** (Needs platform-specific UI)

**Migration Steps per Module**:
1. Create KMP structure
2. Move business logic to commonMain
3. Create expect declarations for platform APIs
4. Implement actual declarations per platform
5. Test on each platform
6. Update documentation

---

## Part 2: MagicCode/MagicUI Integration

### 2.1 What is MagicCode/MagicUI?

**MagicCode**: DSL-based code generator that produces native UI code for all platforms
**MagicUI**: Cross-platform UI runtime that renders native components

**Supported Platforms**:
- Kotlin Jetpack Compose (Android + Desktop)
- SwiftUI (iOS + macOS)
- React/TypeScript (Web)

### 2.2 AVA UI Architecture with MagicCode

**Current**: Hand-coded Android Compose UI
```kotlin
// features/overlay/OverlayScreen.kt - Android-only Compose
@Composable
fun OverlayScreen() {
    Column {
        Text("Voice Input")
        Button(onClick = {}) {
            Text("Start Recording")
        }
    }
}
```

**Target**: MagicCode-generated multi-platform UI

**Step 1: Define UI in MagicCode DSL**
```kotlin
// shared/ui/overlay/OverlayScreen.magic.kt
@MagicScreen
class OverlayScreen : Screen() {
    override fun build() = container {
        orientation = Vertical

        text {
            value = "Voice Input"
            style = TextStyle.headline
        }

        button {
            text = "Start Recording"
            onClick = { viewModel.startRecording() }
            style = ButtonStyle.primary
        }

        voiceOrb {
            isActive = viewModel.isRecording.value
            amplitude = viewModel.audioLevel.value
        }
    }
}
```

**Step 2: MagicCode generates platform code**
```bash
# Run code generator
./gradlew generateMagicCode

# Generates:
# - androidMain/OverlayScreen.kt (Jetpack Compose)
# - iosMain/OverlayScreen.swift (SwiftUI)
# - jsMain/OverlayScreen.tsx (React)
```

**Generated Android Compose**:
```kotlin
// androidMain/generated/OverlayScreen.kt
@Composable
fun OverlayScreen(viewModel: OverlayViewModel) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Voice Input",
            style = MaterialTheme.typography.headlineMedium
        )

        Button(
            onClick = { viewModel.startRecording() }
        ) {
            Text("Start Recording")
        }

        VoiceOrb(
            isActive = viewModel.isRecording.value,
            amplitude = viewModel.audioLevel.value
        )
    }
}
```

**Generated iOS SwiftUI**:
```swift
// iosMain/generated/OverlayScreen.swift
struct OverlayScreen: View {
    @ObservedObject var viewModel: OverlayViewModel

    var body: some View {
        VStack {
            Text("Voice Input")
                .font(.headline)

            Button("Start Recording") {
                viewModel.startRecording()
            }
            .buttonStyle(.primary)

            VoiceOrb(
                isActive: viewModel.isRecording,
                amplitude: viewModel.audioLevel
            )
        }
    }
}
```

**Generated Web React**:
```typescript
// jsMain/generated/OverlayScreen.tsx
export const OverlayScreen: React.FC<{viewModel: OverlayViewModel}> = ({viewModel}) => {
  return (
    <div className="overlay-screen">
      <h2 className="headline">Voice Input</h2>

      <button
        className="primary-button"
        onClick={() => viewModel.startRecording()}
      >
        Start Recording
      </button>

      <VoiceOrb
        isActive={viewModel.isRecording}
        amplitude={viewModel.audioLevel}
      />
    </div>
  );
};
```

### 2.3 MagicUI Component Library

AVA will use MagicElements (VoiceAvenue's cross-platform component library):

**Available Components**:
- ✅ ColorPicker
- ✅ Preferences
- ✅ Text
- ✅ Button
- ✅ Container
- ✅ TextField
- ✅ Checkbox
- ✅ Dialog
- ✅ ListView

**Custom AVA Components** (need to create):
- VoiceOrb (audio visualization)
- MessageBubble (chat bubbles)
- SuggestionChip (contextual suggestions)
- OverlayPanel (floating overlay)

**Integration Steps**:
1. Add MagicElements dependency
2. Create AVA-specific components following MagicElements pattern
3. Register components with MagicCode generator
4. Use in MagicCode DSL

---

## Part 3: VoiceAvenue Ecosystem Integration

### 3.1 VoiceAvenue Architecture

**VoiceAvenue Ecosystem Structure**:
```
VoiceAvanue/
├── voiceavanue/              # Platform code
│   ├── core/
│   │   ├── magicui/         # UI runtime
│   │   ├── magiccode/       # Code generator
│   │   ├── themebridge/     # Theme system
│   │   ├── database/        # Persistence
│   │   └── voiceosbridge/   # IPC with VoiceOS
│   └── libraries/
│       ├── magicelements/   # UI components
│       ├── speechrecognition/
│       └── devicemanager/
│
└── apps/                     # Standalone apps
    ├── voiceos/             # VoiceOS accessibility (FREE)
    ├── voiceavanue-app/     # Core platform (FREE)
    ├── aiavanue/            # AI capabilities ($9.99)
    ├── browseravanue/       # Voice browser ($4.99)
    └── noteavanue/          # Voice notes (FREE/$2.99)
```

**AVA's Position**: Standalone app under `apps/avaai/` or integrated into `aiavanue/`

### 3.2 Integration Option 1: Standalone App

**Structure**:
```
VoiceAvanue/apps/avaai/
├── shared/                   # KMP business logic
│   ├── domain/
│   ├── data/
│   └── features/
├── android/                  # Android app
├── ios/                      # iOS app
├── macos/                    # macOS app
├── windows/                  # Windows app
├── web/                      # Web app
├── docs/
└── tests/
```

**Benefits**:
- ✅ Independent development
- ✅ Separate pricing ($4.99 standalone)
- ✅ Can be bundled with aiavanue

**Integration Points**:
- Uses VoiceOSBridge for system-wide voice commands
- Shares theme via ThemeBridge
- Uses MagicElements components
- Integrated into VoiceAvenue app launcher

### 3.3 Integration Option 2: Part of AIAvanue

**Structure**:
```
VoiceAvanue/apps/aiavanue/
├── features/
│   ├── conversation/         # Chat with AI
│   ├── contextual-ai/        # AVA overlay system
│   ├── voice-commands/       # Voice automation
│   └── learning/             # Teach-AVA
└── ...
```

**Benefits**:
- ✅ Unified AI experience
- ✅ Single app purchase
- ✅ Deeper integration

**Recommendation**: **Option 2** - Integrate AVA as "Contextual AI" feature within AIAvanue

### 3.4 VoiceOS Bridge Integration

**Purpose**: AVA overlay needs system-wide accessibility to detect context and show overlays

**Architecture**:
```kotlin
// commonMain
expect class VoiceOSBridge {
    suspend fun requestAccessibilityPermission(): Boolean
    suspend fun getCurrentAppContext(): AppContext
    suspend fun showOverlay(config: OverlayConfig)
    suspend fun hideOverlay()
    fun registerContextListener(listener: ContextListener)
}

// androidMain
actual class VoiceOSBridge {
    // Uses Android Accessibility Service
    actual suspend fun requestAccessibilityPermission(): Boolean {
        // Request SYSTEM_ALERT_WINDOW permission
    }

    actual suspend fun getCurrentAppContext(): AppContext {
        // Query AccessibilityService for current app
    }
}

// iosMain
actual class VoiceOSBridge {
    // Uses iOS VoiceOS app (separate accessibility helper)
    actual suspend fun getCurrentAppContext(): AppContext {
        // IPC with VoiceOS iOS app via URL scheme or App Group
    }
}
```

**Note**: iOS doesn't allow system-wide overlays. AVA on iOS will work as:
- In-app voice assistant
- Siri Shortcuts integration
- Share Sheet extension

---

## Part 4: Implementation Roadmap

### Phase 1: Foundation (Week 1-2)
**Goal**: KMP setup + core modules

- [ ] Update build.gradle.kts for KMP
- [ ] Add iOS, macOS, desktop, JS targets
- [ ] Restructure core modules (already mostly KMP)
- [ ] Create expect/actual for platform APIs
- [ ] Setup MagicCode generator
- [ ] Add MagicElements dependency

**Deliverables**:
- ✅ KMP builds successfully
- ✅ commonMain code compiles for all targets
- ✅ MagicCode generator runs

### Phase 2: NLU Cross-Platform (Week 3-4)
**Goal**: NLU works on all platforms

- [ ] ONNX Runtime iOS integration
- [ ] ONNX Runtime macOS integration
- [ ] ONNX Runtime Desktop (Windows/Linux)
- [ ] TensorFlow.js for Web
- [ ] BertTokenizer for each platform
- [ ] Model download/management per platform
- [ ] Tests on all platforms

**Deliverables**:
- ✅ Intent classification works on iOS
- ✅ Intent classification works on macOS
- ✅ Intent classification works on Windows
- ✅ Intent classification works on Web

### Phase 3: Chat UI Migration (Week 5-6)
**Goal**: Chat UI using MagicCode

- [ ] Define ChatScreen in MagicCode DSL
- [ ] Generate platform-specific UI
- [ ] Migrate ChatViewModel to commonMain
- [ ] Platform-specific TTS/STT integration
- [ ] Test on all platforms

**Deliverables**:
- ✅ Chat works on all platforms
- ✅ Voice input/output works
- ✅ UI matches platform conventions

### Phase 4: Overlay System (Week 7-8)
**Goal**: Context-aware overlay

- [ ] VoiceOSBridge implementation
- [ ] Overlay UI in MagicCode
- [ ] Platform-specific permission handling
- [ ] Context detection per platform
- [ ] iOS: Alternative implementation (Siri Shortcuts, Share Sheet)

**Deliverables**:
- ✅ Overlay works on Android
- ✅ Alternative on iOS (Siri/Share)
- ✅ Overlay works on macOS
- ✅ Desktop overlay works

### Phase 5: VoiceAvenue Integration (Week 9-10)
**Goal**: Integrated into VoiceAvenue ecosystem

- [ ] Move AVA to VoiceAvanue repository
- [ ] Integrate with ThemeBridge
- [ ] Add to AIAvanue app
- [ ] Setup IPC with VoiceOS
- [ ] Cross-app data sharing
- [ ] Unified settings

**Deliverables**:
- ✅ AVA integrated into AIAvanue
- ✅ Shares themes with ecosystem
- ✅ Works with VoiceOS accessibility

### Phase 6: Platform Polish (Week 11-12)
**Goal**: Platform-specific optimizations

- [ ] iOS: Siri integration, Widgets
- [ ] macOS: Menu bar app, Shortcuts
- [ ] Windows: System tray, Cortana integration
- [ ] Web: PWA, offline support
- [ ] Platform-specific gestures/shortcuts

**Deliverables**:
- ✅ Native feel on each platform
- ✅ Platform-specific features work
- ✅ Performance optimized

---

## Part 5: Technical Challenges & Solutions

### Challenge 1: ONNX Runtime on iOS
**Problem**: ONNX Runtime iOS has different API than Android
**Solution**:
- Create abstraction layer in commonMain
- Use expect/actual for platform-specific runtime
- Consider CoreML fallback for iOS

### Challenge 2: Overlay System on iOS
**Problem**: iOS doesn't allow system-wide overlays
**Solution**:
- Siri Shortcuts integration
- Share Sheet extension
- Keyboard extension (voice keyboard)
- Notification-based suggestions

### Challenge 3: Database Sync
**Problem**: Different databases per platform (Room, CoreData, SQLite)
**Solution**:
- Use SQLDelight (KMP SQL database)
- Single schema generates platform code
- Already planned in AVA architecture

### Challenge 4: MagicCode Learning Curve
**Problem**: New DSL to learn
**Solution**:
- Start with simple components
- Gradual migration (keep Compose for complex screens initially)
- Use MagicElements examples as reference

### Challenge 5: Testing on All Platforms
**Problem**: Need 5 different test environments
**Solution**:
- Use GitHub Actions for CI/CD
- Android: Emulator
- iOS/macOS: macOS runner
- Windows: Windows runner
- Web: Headless browser
- Focus on commonMain unit tests (80% coverage)

---

## Part 6: File Structure Changes

### Before (Current)
```
ava/
├── core/
│   ├── common/
│   ├── domain/
│   └── data/
├── features/
│   ├── nlu/           # Android-only
│   ├── chat/          # Android-only
│   └── overlay/       # Android-only
├── platform/
│   └── app/           # Android app
└── docs/
```

### After (Target)
```
VoiceAvanue/apps/aiavanue/
└── features/
    └── contextual-ai/  # AVA integrated
        ├── shared/     # KMP
        │   ├── src/
        │   │   ├── commonMain/kotlin/
        │   │   │   ├── domain/
        │   │   │   ├── data/
        │   │   │   └── features/
        │   │   │       ├── nlu/
        │   │   │       ├── chat/
        │   │   │       └── overlay/
        │   │   ├── androidMain/kotlin/
        │   │   ├── iosMain/kotlin/
        │   │   ├── desktopMain/kotlin/
        │   │   └── jsMain/kotlin/
        │   └── build.gradle.kts (KMP)
        │
        ├── ui/         # MagicCode DSL
        │   ├── overlay/
        │   │   ├── OverlayScreen.magic.kt
        │   │   └── VoiceOrb.magic.kt
        │   └── chat/
        │       ├── ChatScreen.magic.kt
        │       └── MessageBubble.magic.kt
        │
        ├── android/    # Android-specific
        ├── ios/        # iOS-specific
        ├── macos/      # macOS-specific
        ├── desktop/    # Windows/Linux
        └── web/        # Web app
```

---

## Part 7: Migration Checklist

### Pre-Migration
- [ ] Read all VoiceAvenue ecosystem docs
- [ ] Study MagicCode DSL syntax
- [ ] Understand VoiceOSBridge IPC
- [ ] Setup all platform dev environments
- [ ] Create migration branch

### Migration Phase 1: Build System
- [ ] Update root build.gradle.kts for KMP
- [ ] Add all platform targets
- [ ] Configure expect/actual
- [ ] Test builds on all platforms
- [ ] Setup MagicCode generator

### Migration Phase 2: Core Modules
- [ ] Migrate core/common (already KMP)
- [ ] Migrate core/domain (already KMP)
- [ ] Migrate core/data (add platform impls)
- [ ] Add SQLDelight for database
- [ ] Test on all platforms

### Migration Phase 3: Features
- [ ] Migrate features/nlu
- [ ] Migrate features/chat
- [ ] Migrate features/overlay
- [ ] Create MagicCode UI definitions
- [ ] Generate platform UI code

### Migration Phase 4: Integration
- [ ] Move to VoiceAvanue repo
- [ ] Integrate VoiceOSBridge
- [ ] Add ThemeBridge
- [ ] Setup IPC
- [ ] Test ecosystem integration

### Post-Migration
- [ ] Update all documentation
- [ ] Platform-specific testing
- [ ] Performance optimization
- [ ] Release beta builds
- [ ] User testing

---

## Part 8: Success Metrics

**Code Reuse**:
- Target: 70-80% shared code
- Measure: Lines of code in commonMain vs platform-specific

**Platform Parity**:
- All core features work on all platforms
- UI feels native on each platform
- Performance within 10% of native apps

**Integration**:
- Successfully integrated into AIAvanue
- VoiceOS Bridge working on Android/macOS
- Theme syncs across ecosystem

**Timeline**:
- Complete migration in 12 weeks
- Beta release in 14 weeks
- Production release in 16 weeks

---

## Next Steps

1. **Review this document** with team/stakeholders
2. **Setup development environments** for all platforms
3. **Create detailed Phase 1 tasks** in project tracker
4. **Begin KMP migration** with core modules
5. **Experiment with MagicCode** on simple UI first

---

**Created by**: Manoj Jhawar, manoj@ideahq.net
**Last Updated**: 2025-11-02 01:30 PST
