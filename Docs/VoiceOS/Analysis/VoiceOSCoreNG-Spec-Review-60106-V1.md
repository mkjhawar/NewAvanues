# VoiceOSCoreNG Specification Review

**Date:** 2026-01-06
**Module:** Modules/VoiceOSCoreNG
**Branch:** VoiceOSCoreNG
**Status:** Implementation Gaps Identified

---

## Executive Summary

VoiceOSCoreNG is a Kotlin Multiplatform (KMP) voice accessibility framework. This review identifies **35 TODOs**, **6 major implementation gaps**, and **5 priority work items** for production readiness.

| Category | Count | Priority |
|----------|-------|----------|
| iOS Platform Stubs | 22 TODOs | P2 |
| Desktop Platform Stubs | 8 TODOs | P2 |
| Android Speech Engines | 3 TODOs | P1 |
| Core Speech System | 2 TODOs | P1 |
| UI/UX (Just Completed) | 0 | Done |

---

## Implementation Status

### Production Ready (commonMain)

| Component | File | Status |
|-----------|------|--------|
| CommandGenerator | `common/CommandGenerator.kt` | ✅ |
| CommandMatcher | `common/CommandMatcher.kt` | ✅ |
| CommandRegistry | `common/CommandRegistry.kt` | ✅ |
| StaticCommandRegistry | `common/StaticCommandRegistry.kt` | ✅ |
| VUIDGenerator | `common/VUIDGenerator.kt` | ✅ |
| AVUSerializer | `common/AVUSerializer.kt` | ✅ |
| OverlayCoordinator | `features/OverlayCoordinator.kt` | ✅ |
| NumberedSelectionOverlay | `features/NumberedSelectionOverlay.kt` | ✅ |
| ConfidenceOverlay | `features/ConfidenceOverlay.kt` | ✅ |
| CommandStatusOverlay | `features/CommandStatusOverlay.kt` | ✅ |
| ContextMenuOverlay | `features/ContextMenuOverlay.kt` | ✅ |
| OverlayTheme | `features/OverlayTheme.kt` | ✅ |
| ThemeProvider | `features/ThemeProvider.kt` | ✅ |
| SpeechEngine enum | `features/SpeechEngine.kt` | ✅ |
| SpeechMode enum | `features/SpeechMode.kt` | ✅ |
| SpeechConfig | `features/SpeechConfig.kt` | ✅ |
| ISpeechEngine | `features/ISpeechEngine.kt` | ✅ |
| ISpeechEngineFactory | `features/ISpeechEngineFactory.kt` | ✅ |
| IVivokaEngine | `features/IVivokaEngine.kt` | ✅ |
| IActionExecutor | `handlers/IActionExecutor.kt` | ✅ |
| ActionResult | `handlers/ActionResult.kt` | ✅ |
| Framework Handlers | `handlers/*Handler.kt` | ✅ |

### Stub Implementations (Need Real Code)

| Component | Platform | Location | Status |
|-----------|----------|----------|--------|
| IOSActionExecutor | iOS | `iosMain/handlers/` | ⚠️ 14 TODOs |
| StubExecutors (iOS) | iOS | `iosMain/handlers/` | ⚠️ 4 TODOs |
| IOSHandlerFactory | iOS | `iosMain/` | ⚠️ 2 TODOs |
| iOS SpeechEngine | iOS | `iosMain/features/` | ⚠️ 1 TODO |
| iOS PlatformExtractor | iOS | `iosMain/functions/` | ⚠️ Stub |
| DesktopActionExecutor | Desktop | `desktopMain/handlers/` | ⚠️ Stub |
| StubExecutors (Desktop) | Desktop | `desktopMain/handlers/` | ⚠️ 4 TODOs |
| DesktopHandlerFactory | Desktop | `desktopMain/` | ⚠️ 2 TODOs |
| Desktop SpeechEngine | Desktop | `desktopMain/features/` | ⚠️ 1 TODO |
| VoskEngine | Android | `androidMain/features/` | ⚠️ 1 TODO |
| GoogleEngine | Android | `androidMain/features/` | ⚠️ 1 TODO |
| AzureEngine | Android | `androidMain/features/` | ⚠️ 1 TODO |

---

## Detailed TODO Analysis

### iOS Platform (22 TODOs)

#### IOSActionExecutor.kt (14 TODOs)

| Line | Action | Required API |
|------|--------|--------------|
| 34 | Tap element | `UIAccessibilityElement.accessibilityActivate()` |
| 42 | Long press | `UIAccessibilityCustomAction` |
| 50 | Focus element | `UIAccessibility.post(notification:argument:)` |
| 58 | Enter text | `UITextInput` protocol |
| 80 | Scroll | `UIAccessibilityScrollDirection` |
| 128 | Open settings | `UIApplication.openSettingsURLString` |
| 152 | Screenshot | `UIWindow.layer.render(in:)` |
| 160 | Flashlight | `AVCaptureDevice.setTorchMode()` |
| 173 | Media play/pause | `MPRemoteCommandCenter` |
| 181 | Media next | `MPRemoteCommandCenter.nextTrackCommand` |
| 189 | Media previous | `MPRemoteCommandCenter.previousTrackCommand` |
| 197 | Volume control | `MPVolumeView` or `AVAudioSession` |
| 223 | Open app by URL | `UIApplication.open(URL)` |
| 302/307 | Element lookup | Swift accessibility tree bridging |

#### StubExecutors.kt - iOS (4 TODOs)

| Line | Executor | Required |
|------|----------|----------|
| 15 | NavigationExecutor | UIAccessibility navigation |
| 28 | UIExecutor | UIAccessibility actions |
| 46 | InputExecutor | UITextInput protocol |
| 63 | SystemExecutor | UIApplication APIs |

#### IOSHandlerFactory.kt (2 TODOs)

| Line | Description |
|------|-------------|
| 9 | Implement using UIAccessibility APIs |
| 19/24 | Replace stub executors with real implementations |

#### SpeechEngineFactoryProvider.kt - iOS (1 TODO)

| Line | Description |
|------|-------------|
| 122 | Implement Apple Speech using `SFSpeechRecognizer` |

---

### Desktop Platform (8 TODOs)

#### StubExecutors.kt - Desktop (4 TODOs)

| Line | Executor | Required |
|------|----------|----------|
| 15 | NavigationExecutor | AWT Robot |
| 28 | UIExecutor | AWT Robot |
| 46 | InputExecutor | AWT Robot |
| 63 | SystemExecutor | Platform-specific APIs |

#### DesktopHandlerFactory.kt (2 TODOs)

| Line | Description |
|------|-------------|
| 9 | Implement using AWT Robot or platform-specific APIs |
| 19/24 | Replace stub executors with real implementations |

#### SpeechEngineFactoryProvider.kt - Desktop (1 TODO)

| Line | Description |
|------|-------------|
| 133 | Implement VOSK using vosk-api JNI bindings |

#### DesktopActionExecutor.kt (1 Stub)

Full implementation needed using AWT Robot for:
- Mouse clicks, keyboard input
- Window management
- System integration

---

### Android Platform (3 TODOs)

#### SpeechEngineFactoryProvider.kt - Android

| Line | Engine | Description |
|------|--------|-------------|
| 193 | VoskEngine | Implement using Vosk offline library |
| 211 | GoogleEngine | Implement using Google Cloud Speech API |
| 229 | AzureEngine | Implement using Azure Cognitive Services SDK |

**Note:** Android platform has working:
- UI extraction (AccessibilityNodeInfo)
- Action execution (AndroidActionExecutor)
- Native Android SpeechRecognizer
- Vivoka integration (when SDK available)

---

## Gap Analysis vs VoiceOSCore

### Migrated Successfully ✅

| Feature | VoiceOSCore | VoiceOSCoreNG |
|---------|-------------|---------------|
| UI Extraction | ✅ | ✅ |
| Command Generation | ✅ | ✅ |
| Command Matching | ✅ | ✅ |
| Overlay System | ✅ | ✅ (Enhanced) |
| Theme System | ✅ | ✅ (Enhanced) |
| Feature Tiers | ✅ | ✅ |
| Framework Detection | ✅ | ✅ |
| Static Commands | ✅ | ✅ |
| Speech Mode | ✅ | ✅ |
| Speech Config | ✅ | ✅ |

### Migration Gaps ❌

| Gap | Priority | Effort | Details |
|-----|----------|--------|---------|
| Database Integration | P1 | Medium | Connect to core/database SQLDelight repos |
| Speech Engine Manager | P1 | High | Coordinate multiple engines, lifecycle |
| iOS Swift Bridges | P2 | High | Kotlin/Native to Swift interop |
| Desktop Automation | P2 | Medium | AWT Robot + platform specifics |
| Real Speech Engines | P1 | High | Vosk, Google, Azure implementations |

---

## Test Coverage

| Category | Test Files | Coverage |
|----------|------------|----------|
| Command System | 3 | ✅ Good |
| Overlay System | 12 | ✅ Excellent |
| Theme System | 4 | ✅ Good |
| Handlers | 9 | ✅ Good |
| Cursor/Navigation | 5 | ✅ Good |
| LearnApp | 3 | ✅ Good |
| Integration | 5 | ✅ Good |
| **Total** | **41** | **✅** |

---

## Priority Workstreams

### P0 - Critical Path (Production Blocker)

1. **Database Integration**
   - Connect to `core/database` module
   - Use existing SQLDelight repositories
   - Enable command persistence

### P1 - High Priority (Required for Full Functionality)

2. **Android Speech Engine Implementations**
   - VoskEngine: Offline recognition
   - GoogleEngine: Online high-quality
   - AzureEngine: Enterprise option

3. **SpeechEngineManager**
   - Multi-engine coordination
   - State management (StateFlow)
   - Event emission (SharedFlow)

### P2 - Medium Priority (Platform Completion)

4. **iOS Platform Implementation**
   - Swift/Kotlin interop bridges
   - UIAccessibility action execution
   - Speech.framework integration

5. **Desktop Platform Implementation**
   - AWT Robot automation
   - Vosk JNI integration
   - Cross-platform abstraction

---

## File Counts

| Directory | Files | Purpose |
|-----------|-------|---------|
| `commonMain/` | 50+ | Shared KMP code |
| `androidMain/` | 12 | Android implementations |
| `iosMain/` | 9 | iOS stubs |
| `desktopMain/` | 8 | Desktop stubs |
| `commonTest/` | 41 | Unit tests |

---

## Acceptance Criteria for Production

- [ ] Database repositories integrated
- [ ] At least one speech engine working per platform
- [ ] SpeechEngineManager coordinating engines
- [ ] iOS basic actions working (tap, scroll, back)
- [ ] Desktop basic actions working
- [ ] All unit tests passing
- [ ] Demo app demonstrating full flow

---

## References

- Migration Gaps Doc: `Docs/VoiceOS/issues/VoiceOS-Issue-VoiceOSCoreNG-MigrationGaps-60105-V1.md`
- UI Demo: `Demo/VoiceOSCoreNG/V1/ui-recommendations.html`
- VoiceOSCore Source: `Modules/VoiceOS/apps/VoiceOSCore/`
- Database Module: `Modules/VoiceOS/core/database/`

---

**Generated:** 2026-01-06 by /i.spec .review
