# AVA Overlay System - Implementation Checklist

**Status**: Phase 1 - Module Structure Created
**Next**: Implement core service and UI components

---

## ✅ Completed

1. Design document created (`docs/design/AVA-Overlay-System-Design.md`)
2. Module structure created (`features/overlay/`)
3. build.gradle.kts configured with all dependencies
4. Module added to settings.gradle

---

## 📝 Implementation Files Needed

### Phase 1: Core Infrastructure

#### 1. Service Layer
- [ ] `service/OverlayService.kt` - Foreground service with TYPE_APPLICATION_OVERLAY
- [ ] `service/OverlayPermissionActivity.kt` - Permission request flow
- [ ] `service/OverlayNotificationManager.kt` - Foreground notification

#### 2. Controller Layer
- [ ] `controller/OverlayController.kt` - State management with StateFlow
- [ ] `controller/VoiceRecognizer.kt` - Android SpeechRecognizer wrapper
- [ ] `controller/GestureHandler.kt` - Drag and tap handling

#### 3. UI Layer (Basic)
- [ ] `ui/OverlayComposables.kt` - Root composable
- [ ] `ui/VoiceOrb.kt` - Draggable mic bubble (64dp)
- [ ] `ui/theme/OverlayTheme.kt` - Material3 theme configuration

#### 4. Data Layer
- [ ] `data/OverlayPreferences.kt` - DataStore for position/settings

---

### Phase 2: Glassmorphic UI

#### 1. Theme Components
- [ ] `theme/GlassEffects.kt` - Blur, shadow, border modifiers
- [ ] `theme/AnimationSpecs.kt` - Transition specs (220ms ease-out)

#### 2. UI Components
- [ ] `ui/GlassMorphicPanel.kt` - Expandable glass card
- [ ] `ui/SuggestionChips.kt` - Material3 AssistChip with custom styling

---

### Phase 3: AVA Integration

#### 1. Integration Bridge
- [ ] `integration/AvaIntegrationBridge.kt` - Central integration point
- [ ] `integration/NluConnector.kt` - Use features:nlu IntentClassifier
- [ ] `integration/ChatConnector.kt` - Use features:chat ChatViewModel

---

### Phase 4: Context Engine

#### 1. Context Detection
- [ ] `integration/ContextEngine.kt` - Detect active app
- [ ] `data/ContextData.kt` - Context models

---

## 🎯 Implementation Order

### Sprint 1 (Phase 1 - Week 1)

**Day 1-2**: Core Service
```kotlin
// 1. OverlayService.kt
// 2. OverlayNotificationManager.kt
// 3. OverlayPermissionActivity.kt
```

**Day 3-4**: Controllers
```kotlin
// 4. OverlayController.kt
// 5. VoiceRecognizer.kt
// 6. GestureHandler.kt
```

**Day 5**: Basic UI
```kotlin
// 7. OverlayComposables.kt
// 8. VoiceOrb.kt
```

**Day 6-7**: Testing & Polish
- Unit tests for controllers
- Manual QA on permissions
- Fix any crashes

---

### Sprint 2 (Phase 2 - Week 2)

**Day 1-3**: Glass Theme
```kotlin
// 1. GlassEffects.kt with blur/shadow
// 2. AnimationSpecs.kt
// 3. GlassMorphicPanel.kt
```

**Day 4-5**: UI Components
```kotlin
// 4. Suggestion Chips
// 5. Panel expansion animations
```

**Day 6-7**: Visual Polish
- Match AVA's existing chat UI aesthetic
- Accessibility testing
- Performance optimization

---

### Sprint 3 (Phase 3 - Week 3)

**Day 1-2**: Integration Layer
```kotlin
// 1. AvaIntegrationBridge.kt
// 2. NluConnector.kt
```

**Day 3-4**: Chat Integration
```kotlin
// 3. ChatConnector.kt
// 4. Wire voice → NLU → Chat → UI
```

**Day 5-7**: End-to-End Testing
- Test voice commands
- Test responses
- Error handling

---

### Sprint 4 (Phase 4 - Week 4)

**Day 1-3**: Context Engine
```kotlin
// 1. ContextEngine.kt
// 2. App detection logic
```

**Day 4-5**: Smart Suggestions
- Context-aware chip generation
- Template system

**Day 6-7**: Final QA
- Full system testing
- Performance profiling
- Documentation

---

## 📦 Code Template Structure

Each file should follow this template:

```kotlin
// filename: path/to/File.kt
// created: 2025-11-01 22:00:00 -0700
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// TCR: [Phase number and status]
// agent: Engineer | mode: ACT

package com.augmentalis.ava.features.overlay.[subpackage]

// imports...

/**
 * [Component description]
 *
 * [Purpose and usage]
 *
 * @param [parameters if applicable]
 */
class/fun ComponentName(...) {
    // implementation
}
```

---

## 🧪 Testing Checklist

### Unit Tests
- [ ] OverlayController state transitions
- [ ] VoiceRecognizer lifecycle
- [ ] GestureHandler touch events
- [ ] NluConnector integration
- [ ] ChatConnector integration

### Integration Tests
- [ ] Overlay service start/stop
- [ ] Permission flow (grant/deny)
- [ ] Voice recognition end-to-end
- [ ] NLU classification
- [ ] Chat response generation

### Manual QA
- [ ] Overlay appears over other apps
- [ ] Dragging works smoothly
- [ ] Voice recognition accurate
- [ ] Animations are 60fps
- [ ] No memory leaks
- [ ] Battery impact < 5%

---

## 📚 Reference Files to Study

Before implementing, study these existing AVA files:

1. **Chat UI Patterns**:
   - `features/chat/ui/ChatScreen.kt`
   - `features/chat/ui/components/MessageBubble.kt`

2. **NLU Integration**:
   - `features/nlu/src/main/java/com/augmentalis/ava/features/nlu/IntentClassifier.kt`

3. **ViewModel Patterns**:
   - `features/chat/ui/ChatViewModel.kt`

4. **Theme System**:
   - Check existing Material3 theme configuration in chat module

---

## 🚀 Quick Start After Review

When ready to implement:

```bash
cd /Users/manoj_mbpm14/Coding/ava/features/overlay

# Create first file
touch src/main/java/com/augmentalis/ava/features/overlay/service/OverlayService.kt

# Verify module builds
cd ../..
./gradlew :features:overlay:assembleDebug
```

---

## 📊 Success Metrics

Track these metrics during implementation:

- **Build Time**: Should stay < 30s for overlay module
- **APK Size**: Overlay should add < 500KB to final APK
- **Launch Time**: Overlay service < 200ms startup
- **Memory**: < 50MB footprint
- **Test Coverage**: > 80% for controller logic

---

**Next Action**: Begin implementing `OverlayService.kt` following the design document specifications.
